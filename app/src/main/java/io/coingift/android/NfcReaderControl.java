package io.coingift.android;

import java.lang.ref.WeakReference;
import java.util.concurrent.*;

/** Bounded, coalesced NFC service control. No Binder call runs on the UI or crypto threads. */
final class NfcReaderControl implements AutoCloseable {
    enum State { READY,DISABLED,UNSUPPORTED,UNAVAILABLE }
    interface Service {State enable()throws Exception;void disable()throws Exception;default String diagnostic(){return "NFC-S";}}
    interface Listener {void readerReady(long token,State state);}
    private static final ThreadPoolExecutor IO=new ThreadPoolExecutor(0,1,30,TimeUnit.SECONDS,
        new ArrayBlockingQueue<>(1),task->{Thread t=new Thread(task,"bittap-nfc-control");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());
    private final Service service;private final WeakReference<Listener> listener;private final Executor callback;
    private boolean enabled,closed,running;private volatile boolean callingService;private long revision,token;
    NfcReaderControl(Service service,Listener listener,Executor callback){this.service=service;this.listener=new WeakReference<>(listener);this.callback=callback;}
    void request(boolean enable,long token){
        long attempt;boolean rejected=false;
        synchronized(this){
            if(closed&&enable)return;enabled=enable;this.token=token;attempt=++revision;if(running)return;running=true;
            try{IO.execute(this::drain);}catch(RejectedExecutionException ex){running=false;rejected=true;}
        }
        if(rejected&&enable)deliver(attempt,token,State.UNAVAILABLE);
    }
    // A fixed, non-sensitive stage code only; implementations must never call the system here.
    String diagnostic(){return callingService?service.diagnostic():"NFC-Q";}
    private void drain(){
        while(true){
            long attempt,tag;boolean wanted;
            synchronized(this){attempt=revision;tag=token;wanted=enabled&&!closed;}
            State state=State.UNAVAILABLE;
            try{callingService=true;if(wanted)state=service.enable();else service.disable();}catch(Exception ignored){/* Never surface service data or exceptions to the UI. */}finally{callingService=false;}
            synchronized(this){if(attempt!=revision)continue;running=false;}
            if(wanted)deliver(attempt,tag,state);return;
        }
    }
    private void deliver(long attempt,long tag,State state){
        callback.execute(()->{
            synchronized(this){if(closed||!enabled||revision!=attempt)return;}
            Listener target=listener.get();if(target!=null)target.readerReady(tag,state);
        });
    }
    @Override public void close(){synchronized(this){closed=true;listener.clear();}request(false,0);}
}
