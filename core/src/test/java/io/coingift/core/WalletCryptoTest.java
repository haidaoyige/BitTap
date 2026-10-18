package io.coingift.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.ProviderException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bitcoinj.crypto.ECKey;
import org.bouncycastle.crypto.generators.SCrypt;

public class WalletCryptoTest {
    // Publicly known scalar 1. Only for published standard tests; never use it for funds.
    private static byte[] one(){byte[] k=new byte[32];k[31]=1;return k;}
    private static char[] password(){return "test fixture password only".toCharArray();}
    private static char[] newPassword(){return "Public-fixture-Only9!".toCharArray();}
    private static byte[] fromHex(String hex){byte[] b=new byte[hex.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(hex.substring(i*2,i*2+2),16);return b;}
    private static String mutate(String text,int position){byte[] b=Base64.getUrlDecoder().decode(text.substring(4));b[position]^=1;return text.substring(0,4)+Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    private static int rawLength(String text){return Base64.getUrlDecoder().decode(text.substring(4)).length;}
    @Test public void secp256k1AndBech32PublishedVector(){
        ECKey key=WalletCrypto.publicKeyOf(one());
        assertEquals("0279be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798",WalletCrypto.hex(key.getPubKey()));
        assertEquals("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",WalletCrypto.addressOf(key,WalletCrypto.Network.MAINNET));
        try(WalletCrypto.Unlocked u=new WalletCrypto.Unlocked(one(),WalletCrypto.Network.MAINNET)){
            assertEquals("KwDiBf89QgGbjEhKnhXJuH7LrciVrZi3qYjgd9M7rFU73sVHnoWn",u.wif());
        }
    }
    @Test public void scryptRfc7914Vector(){
        byte[] result=SCrypt.generate("password".getBytes(StandardCharsets.US_ASCII),"NaCl".getBytes(StandardCharsets.US_ASCII),1024,8,16,64);
        assertEquals("fdbabe1c9d3472007856e7190d01e9fe7c6ad7cbc8237830e77376634b3731622eaf30d92e22a3886ff109279d9830dac727afb94a83ee6d8360cbdfa2cc0640",WalletCrypto.hex(result));
    }
    @Test public void aes256GcmNistVector()throws Exception{
        Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(new byte[32],"AES"),new GCMParameterSpec(128,new byte[12]));
        assertEquals("cea7403d4d606b6e074ec5d3baf39d18d0d1c8a799996bf0265b98b5d48ab919",WalletCrypto.hex(c.doFinal(new byte[16])));
    }
    @Test public void mainnetRoundTrip()throws Exception{
        WalletCrypto crypto=new WalletCrypto();
        for(WalletCrypto.Network network:WalletCrypto.Network.values()){
            String encrypted=crypto.encrypt(one(),password(),network);
            assertTrue(encrypted.length()<400);
            assertNotNull(crypto.inspect(encrypted).addresses);
            assertEquals(network,crypto.inspect(encrypted).network);
            try(WalletCrypto.Unlocked u=crypto.decrypt(encrypted,password())){
                assertEquals(WalletCrypto.addressOf(WalletCrypto.publicKeyOf(one()),network),u.address);
                assertEquals(33*2,u.publicKey.length());
            }
        }
    }
    @Test public void sameKeyAndPasswordGetDifferentSaltAndNonce()throws Exception{
        WalletCrypto crypto=new WalletCrypto();String a=crypto.encrypt(one(),password(),WalletCrypto.Network.MAINNET),b=crypto.encrypt(one(),password(),WalletCrypto.Network.MAINNET);
        assertNotEquals(a,b);assertFalse(Arrays.equals(crypto.inspect(a).salt,crypto.inspect(b).salt));assertFalse(Arrays.equals(crypto.inspect(a).nonce,crypto.inspect(b).nonce));
    }
    @Test public void wrongPasswordAndTamperingNeverReturnPrivateKey()throws Exception{
        WalletCrypto crypto=new WalletCrypto();String encrypted=crypto.encrypt(one(),password(),WalletCrypto.Network.MAINNET);
        assertThrows(GeneralSecurityException.class,()->crypto.decrypt(encrypted,"wrong fixture password".toCharArray()));
        for(int index:new int[]{10,26,rawLength(encrypted)-48,rawLength(encrypted)-1})assertThrows(GeneralSecurityException.class,()->crypto.decrypt(mutate(encrypted,index),password()));
        // Replace address with another syntactically valid P2WPKH address: metadata is authenticated too.
        byte[] raw=Base64.getUrlDecoder().decode(encrypted.substring(4));
        String other=WalletCrypto.addressOf(ECKey.fromPrivate(BigInteger.valueOf(2),true),WalletCrypto.Network.MAINNET);
        System.arraycopy(other.getBytes(StandardCharsets.US_ASCII),0,raw,41,42);
        String modified="CG2:"+Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        assertThrows(GeneralSecurityException.class,()->crypto.decrypt(modified,password()));
    }
    @Test public void hostileKdfAndUnknownVersionsRejectedBeforeKdf()throws Exception{
        WalletCrypto crypto=new WalletCrypto();String encrypted=crypto.encrypt(one(),password(),WalletCrypto.Network.MAINNET);
        for(int i:new int[]{0,3,4,5,6,7,8,9,38,39,40})assertThrows(IllegalArgumentException.class,()->crypto.inspect(mutate(encrypted,i)));
        assertThrows(IllegalArgumentException.class,()->crypto.inspect(encrypted+"="));
        assertThrows(IllegalArgumentException.class,()->crypto.inspect("CG2:"+"A".repeat(1021)));
    }
    @Test public void unicodeNormalizedButWhitespaceAndCasePreserved()throws Exception{
        WalletCrypto crypto=new WalletCrypto();
        String encrypted=crypto.encrypt(one(),"  caf\u00e9 secret password  ".toCharArray(),WalletCrypto.Network.MAINNET);
        try(WalletCrypto.Unlocked u=crypto.decrypt(encrypted,"  cafe\u0301 secret password  ".toCharArray())){assertNotNull(u.address);}
        assertThrows(GeneralSecurityException.class,()->crypto.decrypt(encrypted,"caf\u00e9 secret password".toCharArray()));
        assertThrows(GeneralSecurityException.class,()->crypto.decrypt(encrypted,"  Caf\u00e9 secret password  ".toCharArray()));
    }
    @Test public void privateKeyRangeBoundaries(){
        assertFalse(WalletCrypto.isValidKey(new byte[32]));assertTrue(WalletCrypto.isValidKey(one()));
        byte[] n=fromHex("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141");
        assertFalse(WalletCrypto.isValidKey(n));n[31]--;assertTrue(WalletCrypto.isValidKey(n));
        assertFalse(WalletCrypto.isValidKey(new byte[31]));
    }
    @Test public void rejectionSamplingRetriesZeroAndCurveOrder()throws Exception{
        class Sequence extends SecureRandom{
            int calls;
            @Override public void nextBytes(byte[] bytes){Arrays.fill(bytes,(byte)0);calls++;
                if(calls==2)System.arraycopy(fromHex("fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364141"),0,bytes,0,32);
                if(calls==3)bytes[31]=1;
            }
        }
        Sequence rng=new Sequence();WalletCrypto crypto=new WalletCrypto(rng);
        WalletCrypto.Card card=crypto.generate(newPassword(),WalletCrypto.Network.MAINNET);
        assertEquals(5,rng.calls);assertEquals("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",card.address);
    }
    @Test public void failedRandomSourceDoesNotFallback(){
        SecureRandom broken=new SecureRandom(){@Override public void nextBytes(byte[] b){throw new ProviderException("test failure");}};
        assertThrows(ProviderException.class,()->new WalletCrypto(broken).generate(newPassword(),WalletCrypto.Network.MAINNET));
    }
    @Test public void stuckRandomSourceStopsInsteadOfLoopingForever(){
        SecureRandom broken=new SecureRandom(){@Override public void nextBytes(byte[] b){Arrays.fill(b,(byte)0);}};
        assertThrows(GeneralSecurityException.class,()->new WalletCrypto(broken).generate(newPassword(),WalletCrypto.Network.MAINNET));
    }
    @Test public void shortOrBlankNewPasswordsRejected(){
        assertThrows(IllegalArgumentException.class,()->WalletCrypto.requireNewPassword("12345".toCharArray()));
        assertThrows(IllegalArgumentException.class,()->WalletCrypto.requireNewPassword("              ".toCharArray()));
    }
    @Test public void newPasswordMinimumAppliesBeforeEitherGeneratorUsesRandomness()throws Exception{
        SecureRandom unused=new SecureRandom(){@Override public void nextBytes(byte[] b){throw new AssertionError("short password reached key generation");}};
        WalletCrypto crypto=new WalletCrypto(unused);
        for(int n=0;n<10;n++){
            char[] shortPassword=Arrays.copyOf("123456789".toCharArray(),n);
            assertThrows(IllegalArgumentException.class,()->crypto.generate(shortPassword,WalletCrypto.Network.MAINNET));
            assertThrows(IllegalArgumentException.class,()->crypto.generate(shortPassword,WalletCrypto.Network.MAINNET,"",null));
        }
        WalletCrypto.requireNewPassword("Ab9!xy7@pq".toCharArray());
        WalletCrypto.requireNewPassword("Ab9!xy7@pqr".toCharArray());
        char[] maximum=new char[1024];Arrays.fill(maximum,'a');maximum[0]='A';maximum[1]='1';maximum[2]='!';WalletCrypto.requireNewPassword(maximum);
        assertThrows(IllegalArgumentException.class,()->WalletCrypto.requireNewPassword(new char[1025]));
    }
    @Test public void closePreventsFurtherWifAccess(){
        WalletCrypto.Unlocked u=new WalletCrypto.Unlocked(one(),WalletCrypto.Network.MAINNET);u.close();assertThrows(IllegalStateException.class,u::wif);
    }
    @Test public void bip38CompressedOfficialVector()throws Exception{
        WalletCrypto crypto=new WalletCrypto();
        try(WalletCrypto.Unlocked u=crypto.importBip38("6PYNKZ1EAgYgmQfmNVamxyXVWHzK5s6DGhwP4J5o44cvXdoY7sRzhtpUeo","TestingOneTwoThree".toCharArray())){
            assertEquals("L44B5gGEpqEDRS9vVPz7QT35jcBG2r3CZwSwQ4fCewXAhAhqGVpP",u.wif());
        }
    }
    @Test public void bip38WrongPasswordRejected()throws Exception{
        WalletCrypto crypto=new WalletCrypto();
        assertThrows(Exception.class,()->crypto.importBip38("6PYNKZ1EAgYgmQfmNVamxyXVWHzK5s6DGhwP4J5o44cvXdoY7sRzhtpUeo","WrongPassword".toCharArray()));
    }
    @Test public void bip38UncompressedKeyNotSilentlyChangedToSegwit()throws Exception{
        WalletCrypto crypto=new WalletCrypto();
        assertThrows(IllegalArgumentException.class,()->crypto.importBip38("6PRVWUbkzzsbcVac2qwfssoUJAN1Xhrg6bNk8J7Nzm5H7kxEbn2Nh2ZoGg","TestingOneTwoThree".toCharArray()));
    }
    @Test public void exportPublicTestFixtureForIndependentRecovery()throws Exception{
        // Public scalar 1, public test password; never a real user key or backup.
        String fixture=new WalletCrypto().encrypt(one(),password(),WalletCrypto.Network.MAINNET);
        java.nio.file.Path out=java.nio.file.Path.of(System.getProperty("bittap.fixtureDir", "build"), "interop-fixture.txt");
        java.nio.file.Files.createDirectories(out.getParent());
        java.nio.file.Files.writeString(out,fixture,StandardCharsets.US_ASCII);
    }

    @Test public void productionGeneratorCreatesRecoverableMainnetCard()throws Exception{
        WalletCrypto crypto=new WalletCrypto();
        WalletCrypto.Card card=crypto.generate(newPassword(),WalletCrypto.Network.MAINNET);
        assertTrue(card.address.startsWith("bc1q"));
        assertEquals(42,card.address.length());
        assertTrue(card.encrypted.startsWith("CG2:"));
        assertTrue(card.encrypted.length()<=363);
        try(WalletCrypto.Unlocked restored=crypto.decrypt(card.encrypted,newPassword())){
            assertEquals(card.address,restored.address);assertEquals(card.publicKey,restored.publicKey);
        }
    }

    @Test public void tenCharacterPasswordCanCreateAndRecoverCard()throws Exception{
        WalletCrypto crypto=new WalletCrypto();char[] pass="Ab9!xy7@pq".toCharArray();
        WalletCrypto.Card card=crypto.generate(pass,WalletCrypto.Network.MAINNET);
        try(WalletCrypto.Unlocked u=crypto.decrypt(card.encrypted,pass)){assertEquals(card.address,u.address);}
    }
    @Test public void testnetBackupRejectedExplicitly()throws Exception{
        WalletCrypto crypto=new WalletCrypto();String card=crypto.encrypt(one(),password(),WalletCrypto.Network.MAINNET);
        assertThrows(IllegalArgumentException.class,()->crypto.inspect(mutate(card,4)));
    }

}
