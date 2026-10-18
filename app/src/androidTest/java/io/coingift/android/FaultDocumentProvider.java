package io.coingift.android;

import android.content.*;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import java.io.*;
import java.util.*;

/** Test APK only. Public encrypted scalar-one fixture and deliberately faulty document descriptors. */
public final class FaultDocumentProvider extends ContentProvider {
    private final Object lock=new Object();private boolean release;
    private final List<ParcelFileDescriptor> writers=new ArrayList<>();
    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri uri){return "application/octet-stream";}
    @Override public Cursor query(Uri u,String[] p,String s,String[] a,String order){return null;}
    @Override public Uri insert(Uri u,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
    @Override public Bundle call(String method,String arg,Bundle extras){
        synchronized(lock){
            if("reset".equals(method))release=false;
            if("release".equals(method)){release=true;lock.notifyAll();for(ParcelFileDescriptor writer:writers)try{writer.close();}catch(IOException ignored){}writers.clear();}
        }return Bundle.EMPTY;
    }
    @Override public AssetFileDescriptor openAssetFile(Uri uri,String mode)throws FileNotFoundException{return openAssetFile(uri,mode,null);}
    @Override public AssetFileDescriptor openAssetFile(Uri uri,String mode,CancellationSignal cancellation)throws FileNotFoundException{
        String path=uri.getLastPathSegment();
        try{
            if("fail".equals(path))throw new IOException("Public provider failure");
            if("slow-open".equals(path))synchronized(lock){while(!release){try{lock.wait(100);}catch(InterruptedException ignored){}}}
            if("block-read".equals(path)&&mode.equals("r")){
                ParcelFileDescriptor[] pipe=ParcelFileDescriptor.createPipe();synchronized(lock){writers.add(pipe[1]);}
                return new AssetFileDescriptor(pipe[0],0,AssetFileDescriptor.UNKNOWN_LENGTH);
            }
            File file=new File(getContext().getCacheDir(),"public-test-"+path);
            if(mode.equals("r")&&("good".equals(path)||"slow-open".equals(path)))try(InputStream in=getContext().getAssets().open("cg2-scalar-one.txt");OutputStream out=new FileOutputStream(file)){byte[] b=new byte[512];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
            if(mode.equals("r")&&"mismatch".equals(path))try(OutputStream out=new FileOutputStream(file)){out.write("mismatch".getBytes(java.nio.charset.StandardCharsets.US_ASCII));}
            return new AssetFileDescriptor(ParcelFileDescriptor.open(file,ParcelFileDescriptor.parseMode(mode)),0,AssetFileDescriptor.UNKNOWN_LENGTH);
        }catch(IOException ex){throw new FileNotFoundException("Public test provider failure");}
    }
}
