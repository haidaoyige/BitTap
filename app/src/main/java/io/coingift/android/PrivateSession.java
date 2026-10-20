package io.coingift.android;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import io.coingift.core.BitcoinAddresses;
import io.coingift.core.WalletCrypto;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Enumeration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Public metadata and ephemeral ciphertext only. Never serialized or written to app storage. */
final class PrivateSession implements AutoCloseable {
    static final String ALIAS_PREFIX="bittap-session-v1-";
    private static boolean cleaned;
    private static final ExecutorService DELETE=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"bittap-vault-delete");t.setDaemon(true);return t;});
    private final String alias;
    private byte[] ciphertext,nonce;
    final String address,publicKey,note,legacyAddress;
    final BitcoinAddresses addresses;
    final WalletCrypto.Network network;

    private PrivateSession(String alias,byte[] ciphertext,byte[] nonce,WalletCrypto.Unlocked source){
        this.alias=alias;this.ciphertext=ciphertext;this.nonce=nonce;
        address=source.address;publicKey=source.publicKey;note=source.note;legacyAddress=source.legacyAddress;addresses=source.addresses;network=source.network;
    }
    private static KeyStore store()throws GeneralSecurityException{
        try{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);return ks;}
        catch(java.io.IOException ex){throw new GeneralSecurityException("Session keystore unavailable",ex);}
    }
    /** Once per process, before any new alias exists. A crash leaves no on-disk session ciphertext. */
    static synchronized void cleanupStaleKeys()throws GeneralSecurityException{
        if(cleaned)return;KeyStore ks=store();Enumeration<String> aliases=ks.aliases();
        while(aliases.hasMoreElements()){String alias=aliases.nextElement();if(alias.startsWith(ALIAS_PREFIX))ks.deleteEntry(alias);}
        cleaned=true;
    }
    static PrivateSession seal(WalletCrypto.Unlocked source)throws GeneralSecurityException{
        String alias=null;byte[] encrypted=null,iv=null;
        try{
            cleanupStaleKeys();alias=ALIAS_PREFIX+UUID.randomUUID();
            KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
            generator.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).setUserAuthenticationRequired(false).build());
            SecretKey key=generator.generateKey();byte[] exported=key.getEncoded();if(exported!=null){WalletCrypto.wipe(exported);throw new GeneralSecurityException("Session key must not be exportable");}
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key);
            cipher.updateAAD(aad(source.address,source.note,source.addresses));encrypted=source.sealForSession(cipher);iv=cipher.getIV();
            if(iv==null||iv.length!=12||encrypted.length!=48)throw new GeneralSecurityException("Invalid session cipher");
            PrivateSession result=new PrivateSession(alias,encrypted,iv,source);alias=null;encrypted=null;iv=null;return result;
        }finally{
            source.close();WalletCrypto.wipe(encrypted);WalletCrypto.wipe(iv);if(alias!=null)deleteLater(alias);
        }
    }
    private static byte[] aad(String address,String note,BitcoinAddresses addresses){
        // Newline is forbidden in addresses and notes; these are authenticated, public fields.
        return ("BitTap/session/v1\n"+address+"\n"+note+"\n"+addresses.legacy+"\n"+addresses.nestedSegwit+"\n"+addresses.taproot).getBytes(StandardCharsets.UTF_8);
    }
    /** Call on the worker only. Returned WIF exists only for an explicit sensitive action. */
    String wif(){
        byte[] sealed,iv,raw=null;
        synchronized(this){if(ciphertext==null)throw new IllegalStateException("私钥已清除，请重新解密");sealed=ciphertext.clone();iv=nonce.clone();}
        try{
            SecretKey key=(SecretKey)store().getKey(alias,null);if(key==null)throw new GeneralSecurityException("Session key missing");
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,iv));
            cipher.updateAAD(aad(address,note,addresses));raw=cipher.doFinal(sealed);
            synchronized(this){if(ciphertext==null)throw new IllegalStateException("私钥已清除，请重新解密");return WalletCrypto.compressedMainnetWif(raw);}
        }catch(GeneralSecurityException ex){close();throw new IllegalStateException("私钥会话不可用，请重新解密",ex);}
        finally{WalletCrypto.wipe(raw);WalletCrypto.wipe(sealed);WalletCrypto.wipe(iv);}
    }
    private static void deleteLater(String alias){
        DELETE.execute(()->{try{store().deleteEntry(alias);}catch(GeneralSecurityException ignored){/* Orphan cleanup retries next process start; no ciphertext is persisted. */}});
    }
    @Override public synchronized void close(){
        if(ciphertext==null)return;WalletCrypto.wipe(ciphertext);WalletCrypto.wipe(nonce);ciphertext=null;nonce=null;deleteLater(alias);
    }
}
