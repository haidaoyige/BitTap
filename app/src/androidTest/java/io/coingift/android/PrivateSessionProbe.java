package io.coingift.android;

import android.app.Instrumentation;
import io.coingift.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.security.KeyStore;
import java.util.Arrays;

/** Actual Android Keystore tests; all keys are public scalar-one fixtures without funds. */
final class PrivateSessionProbe {
    private final Instrumentation host;private int checks;
    PrivateSessionProbe(Instrumentation host){this.host=host;}
    int run()throws Exception{
        String backup;try(InputStream in=host.getContext().getAssets().open("cg3-scalar-one.txt")){backup=new String(BackupFiles.readBounded(in),java.nio.charset.StandardCharsets.US_ASCII).trim();}
        WalletCrypto crypto=new WalletCrypto();WalletCrypto.Unlocked raw=crypto.decrypt(backup,"test fixture password only".toCharArray());
        Field scalar=WalletCrypto.Unlocked.class.getDeclaredField("privateKey");scalar.setAccessible(true);byte[] previous=(byte[])scalar.get(raw);
        PrivateSession sealed=PrivateSession.seal(raw);String alias=(String)field(sealed,"alias");byte[] ciphertext=(byte[])field(sealed,"ciphertext");byte[] nonce=(byte[])field(sealed,"nonce");
        try{
            yes(scalar.get(raw)==null&&Arrays.equals(previous,new byte[32]),"sealing closes and zeroes core scalar holder");
            for(Field f:PrivateSession.class.getDeclaredFields())yes(f.getType()!=WalletCrypto.Unlocked.class&&f.getType()!=char[].class,"session has no plaintext key/password holder");
            yes(ciphertext.length==48&&nonce.length==12,"AES-GCM session contains scalar ciphertext and nonce only");
            KeyStore ks=store();yes(ks.getKey(alias,null).getEncoded()==null,"Android Keystore wrapping key is non-exportable");
            yes("KwDiBf89QgGbjEhKnhXJuH7LrciVrZi3qYjgd9M7rFU73sVHnoWn".equals(sealed.wif()),"explicit unwrap preserves exact compressed WIF");
            yes(crypto.inspect(backup).addresses.sameAs(sealed.addresses),"public address metadata survives sealing unchanged");
            ciphertext[0]^=1;try{sealed.wif();throw new AssertionError("tamper accepted");}catch(IllegalStateException expected){checks++;}
            yes(field(sealed,"ciphertext")==null&&Arrays.equals(ciphertext,new byte[48])&&Arrays.equals(nonce,new byte[12]),"tampered session fails closed and erases owned buffers");
            long end=android.os.SystemClock.elapsedRealtime()+5000;while(store().containsAlias(alias)&&android.os.SystemClock.elapsedRealtime()<end)Thread.sleep(20);
            yes(!store().containsAlias(alias),"closing removes ephemeral wrapping alias");
            try{sealed.wif();throw new AssertionError("closed key restored");}catch(IllegalStateException expected){checks++;}
        }finally{sealed.close();}
        return checks;
    }
    private static KeyStore store()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);return ks;}
    private static Object field(Object owner,String name)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
    private void yes(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
}
