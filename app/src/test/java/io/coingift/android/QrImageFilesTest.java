package io.coingift.android;

import org.junit.Test;
import java.io.*;
import static org.junit.Assert.*;

public final class QrImageFilesTest {
    private final byte[] image={1,2,3,4};
    @Test public void verifiesOnlyAfterOutputCloses()throws Exception{
        boolean[] closed={false};ByteArrayOutputStream saved=new ByteArrayOutputStream(){public void close(){closed[0]=true;}};
        QrImageFiles.writeAndVerify(image,()->saved,()->{assertTrue(closed[0]);return new ByteArrayInputStream(saved.toByteArray());});
        assertArrayEquals(image,saved.toByteArray());
    }
    @Test public void rejectsDeniedWrite(){assertThrows(SecurityException.class,()->QrImageFiles.writeAndVerify(image,()->{throw new SecurityException();},()->new ByteArrayInputStream(image)));}
    @Test public void rejectsCloseFailure(){assertThrows(IOException.class,()->QrImageFiles.writeAndVerify(image,()->new ByteArrayOutputStream(){public void close()throws IOException{throw new IOException();}},()->new ByteArrayInputStream(image)));}
    @Test public void rejectsDeniedRead(){assertThrows(SecurityException.class,()->QrImageFiles.writeAndVerify(image,ByteArrayOutputStream::new,()->{throw new SecurityException();}));}
    @Test public void rejectsMissingStreams(){
        assertThrows(IOException.class,()->QrImageFiles.writeAndVerify(image,()->null,()->new ByteArrayInputStream(image)));
        assertThrows(IOException.class,()->QrImageFiles.writeAndVerify(image,ByteArrayOutputStream::new,()->null));
    }
    @Test public void rejectsCorruptionTruncationAndExtraBytes(){
        for(byte[] actual:new byte[][]{{1,2,9,4},{1,2,3},{1,2,3,4,5}})
            assertThrows(IOException.class,()->QrImageFiles.writeAndVerify(image,ByteArrayOutputStream::new,()->new ByteArrayInputStream(actual)));
    }
}
