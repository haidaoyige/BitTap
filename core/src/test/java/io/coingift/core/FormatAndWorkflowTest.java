package io.coingift.core;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.crypto.generators.SCrypt;

public class FormatAndWorkflowTest {
    private static String backup;
    private static char[] password(){return "test fixture password only".toCharArray();}
    private static byte[] scalar(int i){byte[] key=new byte[32];key[31]=(byte)i;return key;}
    @BeforeClass public static void fixture()throws Exception{backup=new WalletCrypto().encrypt(scalar(1),password(),WalletCrypto.Network.MAINNET);}
    private static String encoded(byte[] raw){return "CG2:"+Base64.getUrlEncoder().withoutPadding().encodeToString(raw);}
    @Test public void fourPublicAddressesAvailableWithoutPassword(){
        WalletCrypto.Metadata meta=new WalletCrypto().inspect(backup);
        assertEquals(2,meta.version);assertNotNull(meta.addresses);
        assertEquals("1BgGZ9tcN4rm9KBzDn7KprQz87SZ26SAMH",meta.addresses.legacy);
        assertEquals("3JvL6Ymt8MVWiCNHC7oWU6nLeHNJKLZGLN",meta.addresses.nestedSegwit);
        assertTrue(meta.addresses.taproot.startsWith("bc1p"));
    }
    @Test public void existingCg1FixtureStillDecrypts()throws Exception{
        String text;
        try(InputStream in=getClass().getResourceAsStream("/cg1-scalar-one.txt")){
            assertNotNull(in);text=new String(BackupFiles.readBounded(in),StandardCharsets.US_ASCII).trim();
        }
        WalletCrypto crypto=new WalletCrypto();WalletCrypto.Metadata meta=crypto.inspect(text);
        assertEquals(1,meta.version);assertNull(meta.addresses);
        try(WalletCrypto.Unlocked key=crypto.decrypt(text,password())){
            assertEquals("KwDiBf89QgGbjEhKnhXJuH7LrciVrZi3qYjgd9M7rFU73sVHnoWn",key.wif());
            assertEquals(meta.address,key.address);
        }
    }
    @Test public void eachAddressIsCoveredByGcmAuthentication()throws Exception{
        WalletCrypto crypto=new WalletCrypto();
        BitcoinAddresses other=BitcoinAddresses.fromKey(WalletCrypto.publicKeyOf(scalar(3)));
        String[] values={other.nativeSegwit,other.legacy,other.nestedSegwit,other.taproot};
        byte[] original=Base64.getUrlDecoder().decode(backup.substring(4));int position=39;
        for(String value:values){
            int length=Byte.toUnsignedInt(original[position+1]);assertEquals(length,value.length());
            byte[] modified=original.clone();System.arraycopy(value.getBytes(StandardCharsets.US_ASCII),0,modified,position+2,length);
            assertNotNull(crypto.inspect(encoded(modified)).addresses);
            assertThrows(GeneralSecurityException.class,()->crypto.decrypt(encoded(modified),password()));
            position+=2+length;
        }
    }
    @Test public void authenticatedButUnrelatedAddressRejected()throws Exception{
        byte[] raw=Base64.getUrlDecoder().decode(backup.substring(4));
        BitcoinAddresses other=BitcoinAddresses.fromKey(WalletCrypto.publicKeyOf(scalar(3)));
        int legacyStart=39+2+42+2;
        System.arraycopy(other.legacy.getBytes(StandardCharsets.US_ASCII),0,raw,legacyStart,other.legacy.length());
        int headerSize=raw.length-48;
        byte[] derived=SCrypt.generate("test fixture password only".getBytes(StandardCharsets.UTF_8),Arrays.copyOfRange(raw,10,26),32768,8,1,32);
        try{
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(derived,"AES"),new GCMParameterSpec(128,Arrays.copyOfRange(raw,26,38)));
            cipher.updateAAD(Arrays.copyOf(raw,headerSize));byte[] encrypted=cipher.doFinal(scalar(1));
            System.arraycopy(encrypted,0,raw,headerSize,encrypted.length);
            assertThrows(GeneralSecurityException.class,()->new WalletCrypto().decrypt(encoded(raw),password()));
        }finally{WalletCrypto.wipe(derived);}
    }
    @Test public void swappedTypesTruncatedDataAndPrefixVersionMismatchRejected(){
        WalletCrypto crypto=new WalletCrypto();byte[] raw=Base64.getUrlDecoder().decode(backup.substring(4));
        raw[39]=2;assertThrows(IllegalArgumentException.class,()->crypto.inspect(encoded(raw)));
        assertThrows(IllegalArgumentException.class,()->crypto.inspect("CG1:"+backup.substring(4)));
        assertThrows(IllegalArgumentException.class,()->crypto.inspect(backup.substring(0,backup.length()-8)));
    }
    @Test public void completionRequiresVerifiedWriteAndSavedBackup(){
        CreationSession session=new CreationSession(backup);
        assertFalse(session.canFinish());session.markCardWritten(new byte[]{1});
        assertTrue(session.needsBackup());assertFalse(session.canFinish());
        assertThrows(IllegalArgumentException.class,()->session.markBackupSaved(backup+"wrong"));
        assertFalse(session.backupSaved());session.markBackupSaved(backup);assertTrue(session.canFinish());
    }
    @Test public void multipleCardsUseExactSameBackupAndCountUniqueTags(){
        CreationSession session=new CreationSession(backup);session.markBackupSaved(backup);
        session.markCardWritten(new byte[]{1,2});session.markCardWritten(new byte[]{1,2});session.markCardWritten(new byte[]{3,4});
        assertEquals(2,session.cardCount());assertEquals(backup,session.encrypted);
        assertTrue(session.containsCard(new byte[]{1,2}));assertFalse(session.containsCard(new byte[]{5}));
        CreationSession restored=new CreationSession(backup);restored.restoreWrittenCardIds(session.writtenCardIds());
        assertEquals(2,restored.cardCount());
    }
    @Test public void backupIsVerifiedAfterClose()throws Exception{
        CreationSession session=new CreationSession(backup);session.markCardWritten(new byte[]{1});
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();boolean[] closed={false};
        BackupFiles.writeAndVerify(backup,()->new FilterOutputStream(bytes){@Override public void close()throws IOException{super.close();closed[0]=true;}},
            ()->{assertTrue(closed[0]);return new ByteArrayInputStream(bytes.toByteArray());});
        session.markBackupSaved(backup);assertTrue(session.canFinish());
    }
    @Test public void failedCloseOrReadbackNeverCompletesBackup(){
        CreationSession session=new CreationSession(backup);session.markCardWritten(new byte[]{1});
        assertThrows(IOException.class,()->{
            BackupFiles.writeAndVerify(backup,()->new ByteArrayOutputStream(){@Override public void close()throws IOException{throw new IOException("fixture close failure");}},
                ()->{fail("must not read after failing close");return null;});session.markBackupSaved(backup);
        });
        assertFalse(session.canFinish());
        assertThrows(IOException.class,()->{
            BackupFiles.writeAndVerify(backup,ByteArrayOutputStream::new,()->new ByteArrayInputStream(new byte[]{1,2}));session.markBackupSaved(backup);
        });
        assertFalse(session.canFinish());
    }
}
