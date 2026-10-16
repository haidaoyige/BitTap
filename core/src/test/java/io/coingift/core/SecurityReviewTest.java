package io.coingift.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.generators.SCrypt;

/** Adversarial checks of the reviewed behavior, including intentionally documented weaknesses. */
public class SecurityReviewTest {
    private static byte[] scalar(int value){byte[] raw=new byte[32];raw[31]=(byte)value;return raw;}
    private static char[] password(){return "test fixture password only".toCharArray();}
    private static byte[] raw(String text){return Base64.getUrlDecoder().decode(text.substring(4));}
    private static String text(byte[] raw){return "CG2:"+Base64.getUrlEncoder().withoutPadding().encodeToString(raw);}
    @Test public void freshCreationsUseDifferentKeysSaltsAndNonces()throws Exception{
        WalletCrypto crypto=new WalletCrypto();Set<String> addresses=new HashSet<>(),salts=new HashSet<>(),nonces=new HashSet<>();
        for(int i=0;i<12;i++){
            WalletCrypto.Card card=crypto.generate(password(),WalletCrypto.Network.MAINNET);WalletCrypto.Metadata meta=crypto.inspect(card.encrypted);
            assertTrue(addresses.add(card.address));assertTrue(salts.add(WalletCrypto.hex(meta.salt)));assertTrue(nonces.add(WalletCrypto.hex(meta.nonce)));
        }
        // A non-duplication sanity check is not a statistical proof of RNG entropy.
    }
    @Test public void decryptedScalarExistsBeforeAnyWifExportAndCloseWipesOwnedArray()throws Exception{
        WalletCrypto crypto=new WalletCrypto();String backup=crypto.encrypt(scalar(1),password(),WalletCrypto.Network.MAINNET);
        WalletCrypto.Unlocked key=crypto.decrypt(backup,password());
        Field field=WalletCrypto.Unlocked.class.getDeclaredField("privateKey");field.setAccessible(true);byte[] owned=(byte[])field.get(key);
        assertArrayEquals(scalar(1),owned); // No wif(), display or clipboard call has occurred.
        key.close();assertArrayEquals(new byte[32],owned);assertNull(field.get(key));assertThrows(IllegalStateException.class,key::wif);
    }
    @Test public void currentSixDigitPolicyAllowsOfflineDictionaryRecovery()throws Exception{
        char[] weak="123456".toCharArray();WalletCrypto.requireNewPassword(weak);
        WalletCrypto crypto=new WalletCrypto();String backup=crypto.encrypt(scalar(1),weak,WalletCrypto.Network.MAINNET);
        assertThrows(GeneralSecurityException.class,()->crypto.decrypt(backup,"000000".toCharArray()));
        try(WalletCrypto.Unlocked found=crypto.decrypt(backup,weak)){assertEquals(BitcoinAddresses.fromKey(WalletCrypto.publicKeyOf(scalar(1))).nativeSegwit,found.address);}
    }
    @Test public void publicClaimsCanBeReplacedBeforeAuthenticationEvenThoughDecryptionRejects()throws Exception{
        WalletCrypto crypto=new WalletCrypto();byte[] bytes=raw(crypto.encrypt(scalar(1),password(),WalletCrypto.Network.MAINNET));
        BitcoinAddresses attacker=BitcoinAddresses.fromKey(WalletCrypto.publicKeyOf(scalar(3)));
        String[] values={attacker.nativeSegwit,attacker.legacy,attacker.nestedSegwit,attacker.taproot};int offset=39;
        for(String value:values){int n=Byte.toUnsignedInt(bytes[offset+1]);assertEquals(n,value.length());System.arraycopy(value.getBytes(StandardCharsets.US_ASCII),0,bytes,offset+2,n);offset+=2+n;}
        String replaced=text(bytes);
        assertTrue(crypto.inspect(replaced).addresses.sameAs(attacker));
        assertThrows(GeneralSecurityException.class,()->crypto.decrypt(replaced,password()));
    }
    @Test public void authenticatedOutOfRangeScalarsNeverReturnAnUnlockedKey()throws Exception{
        WalletCrypto crypto=new WalletCrypto();String backup=crypto.encrypt(scalar(1),password(),WalletCrypto.Network.MAINNET);
        byte[] order=new java.math.BigInteger("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141",16).toByteArray();
        byte[] n=Arrays.copyOfRange(order,order.length-32,order.length);
        for(byte[] bad:new byte[][]{new byte[32],n}){
            byte[] bytes=raw(backup);int header=bytes.length-48;
            byte[] derived=SCrypt.generate("test fixture password only".getBytes(StandardCharsets.UTF_8),Arrays.copyOfRange(bytes,10,26),32768,8,1,32);
            try{
                Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(derived,"AES"),new GCMParameterSpec(128,Arrays.copyOfRange(bytes,26,38)));
                cipher.updateAAD(Arrays.copyOf(bytes,header));System.arraycopy(cipher.doFinal(bad),0,bytes,header,48);
                assertThrows(IllegalArgumentException.class,()->crypto.decrypt(text(bytes),password()));
            }finally{WalletCrypto.wipe(derived);}
        }
    }
    @Test public void parserHandlesEveryTruncationAndMutatedUntrustedPayloads()throws Exception{
        WalletCrypto crypto=new WalletCrypto();byte[] fixture=raw(crypto.encrypt(scalar(1),password(),WalletCrypto.Network.MAINNET));
        for(int end=0;end<fixture.length;end++){
            byte[] cut=Arrays.copyOf(fixture,end);assertThrows(IllegalArgumentException.class,()->crypto.inspect(text(cut)));
        }
        // Deterministic fuzzing is confined to tests, never used to generate production keys.
        Random fuzz=new Random(0x4249544e46434cL);
        for(int i=0;i<4096;i++){
            byte[] bytes=fixture.clone();for(int j=0;j<1+fuzz.nextInt(6);j++)bytes[fuzz.nextInt(bytes.length)]^=(byte)(1+fuzz.nextInt(255));
            try{crypto.inspect(text(bytes));}catch(IllegalArgumentException expected){/* Rejected before KDF. */}
        }
    }
}
