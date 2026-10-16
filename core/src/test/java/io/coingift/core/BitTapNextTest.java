package io.coingift.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.security.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Public fixtures only: never fund the addresses produced by these tests. */
public class BitTapNextTest {
    private static char[] password(){return "test fixture password only".toCharArray();}
    private static byte[] one(){byte[] key=new byte[32];key[31]=1;return key;}
    private static String cg3(String note)throws Exception{return new WalletCrypto().encrypt(one(),password(),WalletCrypto.Network.MAINNET,note,true);}
    @Test public void publicNoteAuthenticatesAndRecoveryMatches()throws Exception{
        String text=cg3("03 - 長期储蓄");WalletCrypto crypto=new WalletCrypto();WalletCrypto.Metadata m=crypto.inspect(text);
        assertEquals(3,m.version);assertEquals("03 - 長期储蓄",m.note);assertNotNull(m.addresses);
        try(WalletCrypto.Unlocked key=crypto.decrypt(text,password())){assertEquals(m.note,key.note);assertTrue(key.addresses.sameAs(m.addresses));assertEquals("KwDiBf89QgGbjEhKnhXJuH7LrciVrZi3qYjgd9M7rFU73sVHnoWn",key.wif());}
        Files.writeString(Path.of("build/cg3-scalar-one.txt"),text,StandardCharsets.US_ASCII);
    }
    @Test public void noteTamperingFailsAuthentication()throws Exception{
        String text=cg3("one");byte[] raw=Base64.getUrlDecoder().decode(text.substring(4));raw[raw.length-48-1]='x';String changed="CG3:"+Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        assertEquals("onx",new WalletCrypto().inspect(changed).note);assertThrows(GeneralSecurityException.class,()->new WalletCrypto().decrypt(changed,password()));
    }
    @Test public void supplementaryLettersFitExact64ByteBoundary()throws Exception{
        String note="𠀀".repeat(16);String text=cg3(note);assertTrue(text.length()<=450);
        try(WalletCrypto.Unlocked key=new WalletCrypto().decrypt(text,password())){assertEquals(note,key.note);}
        assertThrows(IllegalArgumentException.class,()->WalletNote.normalize(note+"x"));
    }
    @Test public void emptyNoteAndLegacyMetadataRemainReadable()throws Exception{
        assertEquals("",new WalletCrypto().inspect(cg3("")).note);
        String old=new WalletCrypto().encrypt(one(),password(),WalletCrypto.Network.MAINNET);assertTrue(old.startsWith("CG2:"));assertEquals("",new WalletCrypto().inspect(old).note);
    }
    @Test public void malformedNoteUtf8AndPrefixMismatchRejected()throws Exception{
        String text=cg3("one");byte[] raw=Base64.getUrlDecoder().decode(text.substring(4));raw[raw.length-48-1]=(byte)0xff;
        assertThrows(IllegalArgumentException.class,()->new WalletCrypto().inspect("CG3:"+Base64.getUrlEncoder().withoutPadding().encodeToString(raw)));
        assertThrows(IllegalArgumentException.class,()->new WalletCrypto().inspect("CG2:"+text.substring(4)));
    }
    @Test public void notesRejectControlBidiEmojiAndNewlines(){for(String text:new String[]{"\n","\u202e","😀","\u200b","\u0000"})assertThrows(IllegalArgumentException.class,()->WalletNote.normalize(text));assertEquals("é",WalletNote.normalize("e\u0301"));}
    private static CameraEntropy fixture()throws Exception{
        CameraEntropy c=new CameraEntropy(new SecureRandom(){@Override public void nextBytes(byte[] bytes){for(int i=0;i<bytes.length;i++)bytes[i]=(byte)i;}});
        MessageDigest h=MessageDigest.getInstance("SHA-256");for(int i=0;i<32;i++)c.addFrame(h.digest(("frame-"+i).getBytes(StandardCharsets.US_ASCII)));c.finish(h.digest("final-photo".getBytes(StandardCharsets.US_ASCII)));return c;
    }
    @Test public void mixtureMatchesIndependentPythonGoldenAndIsSingleUse()throws Exception{
        CameraEntropy c=fixture();assertTrue(c.ready());assertEquals("c22eeab355316875ad75c1cd39b63544dbd1ea3a362e447c852cf22aec695b41",WalletCrypto.hex(c.derive()));assertFalse(c.ready());assertEquals(0,c.count());assertThrows(GeneralSecurityException.class,c::derive);
    }
    @Test public void enhancedCreationRecoversNoteAndAllAddresses()throws Exception{
        CameraEntropy c=fixture();WalletCrypto.Card card=new WalletCrypto().generate(password(),WalletCrypto.Network.MAINNET,"カメラ1",c);
        assertTrue(card.encrypted.startsWith("CG3:"));assertFalse(c.ready());try(WalletCrypto.Unlocked key=new WalletCrypto().decrypt(card.encrypted,password())){assertTrue(key.addresses.sameAs(card.addresses));assertEquals("カメラ1",key.note);}
    }
    @Test public void repeatedFramesCannotCompleteCapture()throws Exception{
        try(CameraEntropy c=new CameraEntropy()){byte[] digest=MessageDigest.getInstance("SHA-256").digest(new byte[]{0});for(int i=0;i<100;i++)c.addFrame(digest);assertEquals(1,c.count());assertThrows(IllegalStateException.class,()->c.finish(digest));assertFalse(c.ready());}
    }
    @Test public void systemRandomFailureNeverFallsBackToCamera(){assertThrows(IllegalStateException.class,()->new CameraEntropy(new SecureRandom(){@Override public void nextBytes(byte[] b){throw new IllegalStateException("test");}}));}
    @Test public void closedCaptureRejectsMoreData()throws Exception{CameraEntropy c=fixture();c.close();assertFalse(c.ready());assertThrows(IllegalStateException.class,()->c.addFrame(new byte[32]));}
}
