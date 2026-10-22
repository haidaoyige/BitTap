package io.coingift.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

public class WalletFingerprintTest {
    private static String fixture()throws Exception {
        byte[] key=new byte[32];key[31]=1;
        try{return new WalletCrypto().encrypt(key,"Public-Only-Password9!".toCharArray(),WalletCrypto.Network.MAINNET,"test-01",true);}
        finally{WalletCrypto.wipe(key);}
    }
    @Test public void everyChangedByteFailsOrIsRejected()throws Exception {
        String original=fixture();byte[] expected=WalletFingerprint.of(original);
        assertEquals(32,expected.length);assertTrue(WalletFingerprint.matches(expected,original));
        byte[] bytes=Base64.getUrlDecoder().decode(original.substring(4));
        for(int i=0;i<bytes.length;i++){
            byte[] changed=bytes.clone();changed[i]^=1;
            String mutation="CG3:"+Base64.getUrlEncoder().withoutPadding().encodeToString(changed);
            try{assertFalse("byte "+i,WalletFingerprint.matches(expected,mutation));}catch(IllegalArgumentException acceptable){}
        }
    }
    @Test public void fullDigestAndCanonicalPayloadRequired()throws Exception {
        String value=fixture();byte[] digest=WalletFingerprint.of(value);
        assertFalse(WalletFingerprint.matches(Arrays.copyOf(digest,4),value));
        byte[] wrong=digest.clone();wrong[31]^=1;assertFalse(WalletFingerprint.matches(wrong,value));
        assertThrows(IllegalArgumentException.class,()->WalletFingerprint.of(value+"="));
        assertThrows(IllegalArgumentException.class,()->WalletFingerprint.of(" "+value));
        assertThrows(IllegalArgumentException.class,()->WalletFingerprint.of("CG2:"+value.substring(4)));
        assertThrows(IllegalArgumentException.class,()->WalletFingerprint.of("6P"+"1".repeat(56)));
        assertFalse(MessageDigest.isEqual(digest,MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII))));
    }
    @Test public void reencryptingSameKeyStillNeedsFreshVerification()throws Exception {
        String a=fixture(),b=fixture();assertFalse(WalletFingerprint.matches(WalletFingerprint.of(a),b));
        WalletCrypto crypto=new WalletCrypto();assertTrue(crypto.inspect(a).addresses.sameAs(crypto.inspect(b).addresses));
    }
}
