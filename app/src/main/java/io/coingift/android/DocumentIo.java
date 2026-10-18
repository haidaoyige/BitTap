package io.coingift.android;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import io.coingift.core.BackupFiles;
import java.io.*;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bounded provider workers, cancellable descriptors and weak, identity-checked UI delivery. */
final class DocumentIo {
    static final long TIMEOUT_MS=15_000;
    enum Kind { IMPORT,BACKUP,PNG }
    interface Listener { void documentFinished(Job job,boolean success,String imported); }
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static ThreadPoolExecutor pool(String name,int queue){
        return new ThreadPoolExecutor(0,2,30,TimeUnit.SECONDS,queue==0?new SynchronousQueue<>():new ArrayBlockingQueue<>(queue),r->{Thread t=new Thread(r,name);t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    }
    private static final ThreadPoolExecutor IO=pool("bittap-document",0),CLOSE=pool("bittap-document-close",2);
    private DocumentIo(){}
    static Job start(Context context,Uri uri,Kind kind,String encrypted,byte[] png,Listener listener,long maximumDeadline){
        Job job=new Job(context.getApplicationContext().getContentResolver(),uri,kind,encrypted,png,listener,maximumDeadline);
        try{job.future=IO.submit(job);}catch(RejectedExecutionException ex){job.erase();job.deliver(false,null);}
        return job;
    }
    static final class Job implements Runnable {
        final Kind kind;final long deadline;
        private final ContentResolver resolver;private final Uri uri;private final String encrypted;
        private final byte[] png;private final WeakReference<Listener> listener;
        private final CancellationSignal signal=new CancellationSignal();
        private final AtomicBoolean cancelled=new AtomicBoolean();
        private final Set<Closeable> resources=Collections.newSetFromMap(new IdentityHashMap<>());
        private volatile Future<?> future;
        Job(ContentResolver resolver,Uri uri,Kind kind,String encrypted,byte[] png,Listener listener,long maximumDeadline){this.resolver=resolver;this.uri=uri;this.kind=kind;this.encrypted=encrypted;this.png=png;this.listener=new WeakReference<>(listener);deadline=Math.min(maximumDeadline,SystemClock.elapsedRealtime()+TIMEOUT_MS);}
        boolean expired(){return SystemClock.elapsedRealtime()>=deadline;}
        void cancel(){
            if(!cancelled.compareAndSet(false,true))return;
            listener.clear();Future<?> f=future;if(f!=null)f.cancel(true);erase();
            // Provider cancellation and close can themselves block. Never run them on the UI thread.
            try{CLOSE.execute(()->{try{signal.cancel();}catch(RuntimeException ignored){}closeResources();});}catch(RejectedExecutionException ignored){/* Bounded cleanup pool; worker finally still closes its resources. */}
        }
        private void check()throws IOException{if(cancelled.get()||expired()||Thread.currentThread().isInterrupted())throw new IOException("Document operation cancelled or expired");}
        private <T extends Closeable>T own(T resource)throws IOException{
            boolean stop;synchronized(resources){stop=cancelled.get();if(!stop)resources.add(resource);}
            if(stop){resource.close();throw new IOException("Document cancelled");}return resource;
        }
        private InputStream input()throws IOException{
            check();AssetFileDescriptor descriptor=resolver.openAssetFileDescriptor(uri,"r",signal);
            if(descriptor==null)throw new IOException("No input descriptor");own(descriptor);
            InputStream raw=own(descriptor.createInputStream());
            return new FilterInputStream(raw){@Override public int read(byte[] b,int off,int length)throws IOException{check();int n=super.read(b,off,length);check();return n;}@Override public int read()throws IOException{check();int n=super.read();check();return n;}};
        }
        private OutputStream output()throws IOException{
            check();AssetFileDescriptor descriptor=resolver.openAssetFileDescriptor(uri,"wt",signal);
            if(descriptor==null)throw new IOException("No output descriptor");own(descriptor);
            OutputStream raw=own(descriptor.createOutputStream());
            return new FilterOutputStream(raw){@Override public void write(byte[] b,int off,int length)throws IOException{check();out.write(b,off,length);check();}@Override public void write(int value)throws IOException{check();out.write(value);check();}@Override public void flush()throws IOException{check();out.flush();check();}};
        }
        @Override public void run(){
            boolean success=false;String imported=null;
            try{
                check();
                if(kind==Kind.IMPORT){try(InputStream in=input()){imported=new String(BackupFiles.readBounded(in),StandardCharsets.UTF_8).trim();}}
                else if(kind==Kind.BACKUP)BackupFiles.writeAndVerify(encrypted,this::output,this::input);
                else QrImageFiles.writeAndVerify(png,this::output,this::input);
                check();success=true;
            }catch(Exception ignored){/* Report failure only, never provider messages containing user data. */}
            finally{erase();closeResources();}
            deliver(success,imported);
        }
        private void erase(){if(png!=null)Arrays.fill(png,(byte)0);}
        private void closeResources(){List<Closeable> list;synchronized(resources){list=new ArrayList<>(resources);resources.clear();}for(Closeable c:list)try{c.close();}catch(Exception ignored){}}
        private void deliver(boolean success,String imported){MAIN.post(()->{Listener target=listener.get();if(!cancelled.get()&&target!=null)target.documentFinished(this,success,imported);});}
    }
}
