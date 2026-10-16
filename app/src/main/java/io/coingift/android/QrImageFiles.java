package io.coingift.android;

import android.graphics.Bitmap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/** Explicit PNG export only. No temporary file, gallery insertion or persistent app cache. */
final class QrImageFiles {
    private QrImageFiles(){}
    interface Open<T>{T open() throws IOException;}
    private static final class WipingBuffer extends ByteArrayOutputStream {
        @Override public void close(){Arrays.fill(buf,(byte)0);reset();}
    }
    static byte[] encode(Bitmap bitmap) throws IOException {
        try(WipingBuffer out=new WipingBuffer()){
            if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out))throw new IOException("PNG encoding failed");
            return out.toByteArray();
        }
    }
    static void writeAndVerify(byte[] png,Open<OutputStream> output,Open<InputStream> input) throws IOException {
        if(png==null||png.length==0)throw new IOException("No image to save");
        try(OutputStream out=output.open()){
            if(out==null)throw new IOException("Cannot open image");
            out.write(png);out.flush();
        }
        byte[] buffer=new byte[4096];int offset=0;
        try(InputStream in=input.open()){
            if(in==null)throw new IOException("Cannot verify image");
            int count;
            while((count=in.read(buffer))!=-1){
                if(count>png.length-offset)throw new IOException("Image length mismatch");
                for(int i=0;i<count;i++)if(buffer[i]!=png[offset+i])throw new IOException("Image content mismatch");
                offset+=count;
            }
            if(offset!=png.length)throw new IOException("Image truncated");
        }finally{Arrays.fill(buffer,(byte)0);}
    }
}
