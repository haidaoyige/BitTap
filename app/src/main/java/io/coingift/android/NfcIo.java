package io.coingift.android;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** NFC never occupies the crypto worker. One process-wide worker; no backlog or unbounded replacements. */
final class NfcIo {
    static final long TIMEOUT_MS=15_000;
    // Fixed stage identifiers only. Never accept card data or exception messages as stages.
    enum Phase {
        START("0"),MATCH_WRITTEN("L1"),INSPECT("L2"),NDEF_LOCK("L3"),HARDWARE_LOCK("L4"),
        CC_LOCK("H0"),CHIP_CONNECT("H1"),CHIP_VERSION("H2"),CHIP_CONFIG("H3"),DYNAMIC_LOCK("H4"),CONFIG_LOCK("H5"),STATIC_LOCK("H6"),
        VERIFY_WRITTEN("V1"),VERIFY_BITS("V2"),VERIFY_READONLY("V3");
        final String code;Phase(String code){this.code=code;}
    }
    interface Work extends Closeable {
        Object run(Job job)throws Exception;
        void discard(Object result);
    }
    interface Listener {void nfcFinished(Job job,Object result,Exception error);}
    static final class Unavailable extends IOException {}
    static final class Expired extends InterruptedIOException {}
    private static ThreadPoolExecutor pool(String name,int maximum,int queue){
        return new ThreadPoolExecutor(0,maximum,30,TimeUnit.SECONDS,
            queue==0?new SynchronousQueue<>():new ArrayBlockingQueue<>(queue),
            task->{Thread t=new Thread(task,name);t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    }
    private static final ThreadPoolExecutor IO=pool("bittap-nfc",1,0),CLOSE=pool("bittap-nfc-close",2,2);
    private NfcIo(){}
    static Job start(Work work,Listener listener,Executor callback){return start(work,listener,callback,TIMEOUT_MS);}
    // Short deadlines are injectable only by tests, never selected from card data.
    static Job start(Work work,Listener listener,Executor callback,long timeoutMillis){
        Job job=new Job(work,listener,callback,timeoutMillis);
        try{job.future=IO.submit(job);}catch(RejectedExecutionException ex){job.closeWork();job.deliver(null,new Unavailable());}
        return job;
    }
    private static final class Resource implements Closeable {
        final Closeable value;final AtomicBoolean closed=new AtomicBoolean();
        Resource(Closeable value){this.value=value;}
        @Override public void close()throws IOException{if(closed.compareAndSet(false,true))value.close();}
    }
    static final class Job implements Runnable {
        private final Work work;private final WeakReference<Listener> listener;private final Executor callback;
        private final long deadline;private final AtomicBoolean cancelled=new AtomicBoolean();
        private final Map<Closeable,Resource> resources=new IdentityHashMap<>();
        private volatile Future<?> future;private volatile Phase phase=Phase.START;
        private Job(Work work,Listener listener,Executor callback,long timeoutMillis){
            if(timeoutMillis<=0||timeoutMillis>TIMEOUT_MS)throw new IllegalArgumentException("Invalid NFC deadline");
            this.work=work;this.listener=new WeakReference<>(listener);this.callback=callback;
            deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        }
        void phase(Phase value)throws IOException{check();phase=Objects.requireNonNull(value);}
        Phase phase(){return phase;}
        boolean expired(){return deadline-System.nanoTime()<=0;}
        long remainingMillis(){return Math.max(1,TimeUnit.NANOSECONDS.toMillis(Math.max(0,deadline-System.nanoTime())));}
        void check()throws IOException{if(cancelled.get()||Thread.currentThread().isInterrupted())throw new InterruptedIOException();if(expired())throw new Expired();}
        <T extends Closeable>T own(T value)throws IOException{
            Resource resource=new Resource(value);
            synchronized(resources){if(!cancelled.get()&&!expired()){resources.put(value,resource);return value;}}
            closeAsync(Collections.singletonList(resource));check();throw new InterruptedIOException();
        }
        void release(Closeable value)throws IOException{
            Resource resource;synchronized(resources){resource=resources.remove(value);}
            if(resource!=null)resource.close();
        }
        void cancel(){
            listener.clear();if(!cancelled.compareAndSet(false,true))return;
            Future<?> f=future;if(f!=null)f.cancel(true);
            List<Resource> pending;synchronized(resources){pending=new ArrayList<>(resources.values());}
            closeAsync(pending);
        }
        private void closeResources(){
            List<Resource> pending;synchronized(resources){pending=new ArrayList<>(resources.values());resources.clear();}
            for(Resource resource:pending)try{resource.close();}catch(Exception ignored){}
        }
        private void closeWork(){try{work.close();}catch(Exception ignored){}}
        @Override public void run(){
            Object result=null;Exception failure=null;
            try{check();result=work.run(this);check();}catch(Exception ex){failure=ex;}
            finally{closeWork();closeResources();}
            if(failure!=null){work.discard(result);result=null;}deliver(result,failure);
        }
        private void deliver(Object result,Exception failure){
            if(cancelled.get()){work.discard(result);return;}
            try{callback.execute(()->{
                Listener target=listener.get();listener.clear();
                if(cancelled.get()||target==null){work.discard(result);return;}
                if(expired()){work.discard(result);target.nfcFinished(this,null,new Expired());}
                else target.nfcFinished(this,result,failure);
            });}catch(RejectedExecutionException ex){work.discard(result);listener.clear();}
        }
    }
    private static void closeAsync(List<Resource> resources){
        if(resources.isEmpty())return;
        try{CLOSE.execute(()->{for(Resource resource:resources)try{resource.close();}catch(Exception ignored){}});}
        catch(RejectedExecutionException ignored){/* Bounded cleanup; worker finally retries still-owned resources. Never block UI. */}
    }
}
