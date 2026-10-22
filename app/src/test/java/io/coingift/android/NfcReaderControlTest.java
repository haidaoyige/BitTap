package io.coingift.android;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class NfcReaderControlTest {
    private static void await(CountDownLatch v)throws Exception{assertTrue(v.await(3,TimeUnit.SECONDS));}
    private static void drain()throws Exception{
        NfcTestWorkers.awaitIdle(NfcReaderControl.class,"IO");
    }
    @Test public void blockedEnableNeverBlocksCallerAndCancelCoalescesAllPendingRequests()throws Exception{
        drain();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),disabled=new CountDownLatch(1);AtomicInteger enables=new AtomicInteger(),callbacks=new AtomicInteger();Thread ui=Thread.currentThread();
        NfcReaderControl.Listener listener=(token,state)->callbacks.incrementAndGet();
        NfcReaderControl control=new NfcReaderControl(new NfcReaderControl.Service(){
            public NfcReaderControl.State enable()throws Exception{assertNotSame(ui,Thread.currentThread());enables.incrementAndGet();entered.countDown();release.await();return NfcReaderControl.State.READY;}
            public void disable(){disabled.countDown();}
        },listener,Runnable::run);
        try{
            long start=System.nanoTime();control.request(true,1);assertTrue(System.nanoTime()-start<TimeUnit.MILLISECONDS.toNanos(300));await(entered);
            for(int i=0;i<200;i++)control.request(true,i+2);control.request(false,999);
            ExecutorService crypto=Executors.newSingleThreadExecutor();try{assertEquals("ready",crypto.submit(()->"ready").get(1,TimeUnit.SECONDS));}finally{crypto.shutdownNow();}
        }finally{release.countDown();await(disabled);drain();control.close();drain();}
        assertEquals(1,enables.get());assertEquals(0,callbacks.get());
    }
    @Test public void blockedDisableNeverBlocksCallerAndLaterEnableUsesLatestToken()throws Exception{
        drain();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),ready=new CountDownLatch(1);AtomicLong received=new AtomicLong(-1);
        NfcReaderControl.Listener listener=(token,state)->{assertEquals(NfcReaderControl.State.READY,state);received.set(token);ready.countDown();};
        NfcReaderControl control=new NfcReaderControl(new NfcReaderControl.Service(){
            public NfcReaderControl.State enable(){return NfcReaderControl.State.READY;}
            public void disable()throws Exception{entered.countDown();release.await();}
        },listener,Runnable::run);
        try{control.request(false,1);await(entered);long start=System.nanoTime();control.request(true,2);control.request(true,3);assertTrue(System.nanoTime()-start<TimeUnit.MILLISECONDS.toNanos(300));}
        finally{release.countDown();}await(ready);assertEquals(3,received.get());control.close();drain();
    }
    @Test public void delayedReadyCannotReviveClosedActivity()throws Exception{
        drain();BlockingQueue<Runnable> callbacks=new ArrayBlockingQueue<>(2);AtomicInteger delivered=new AtomicInteger();CountDownLatch disabled=new CountDownLatch(1);
        NfcReaderControl.Listener listener=(t,s)->delivered.incrementAndGet();
        NfcReaderControl control=new NfcReaderControl(new NfcReaderControl.Service(){public NfcReaderControl.State enable(){return NfcReaderControl.State.READY;}public void disable(){disabled.countDown();}},listener,callbacks::add);
        control.request(true,5);Runnable late=callbacks.poll(3,TimeUnit.SECONDS);assertNotNull(late);control.close();late.run();await(disabled);drain();assertEquals(0,delivered.get());
    }
    @Test public void hungServiceHasBoundedQueueAcrossActivityRecreation()throws Exception{
        drain();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),unavailable=new CountDownLatch(1);AtomicInteger touched=new AtomicInteger();
        NfcReaderControl.Listener listener=(t,s)->{if(s==NfcReaderControl.State.UNAVAILABLE)unavailable.countDown();};
        NfcReaderControl first=new NfcReaderControl(new NfcReaderControl.Service(){public NfcReaderControl.State enable()throws Exception{entered.countDown();release.await();return NfcReaderControl.State.READY;}public void disable(){}},listener,Runnable::run);
        NfcReaderControl.Service queued=new NfcReaderControl.Service(){public NfcReaderControl.State enable(){touched.incrementAndGet();return NfcReaderControl.State.READY;}public void disable(){}};
        NfcReaderControl second=new NfcReaderControl(queued,listener,Runnable::run),third=new NfcReaderControl(queued,listener,Runnable::run);
        try{first.request(true,1);await(entered);second.request(true,2);third.request(true,3);await(unavailable);assertEquals(0,touched.get());first.close();second.close();third.close();}
        finally{release.countDown();drain();}assertEquals(0,touched.get());
    }
    @Test public void diagnosticReportsBlockedServiceStageWithoutCallingNfcAgain()throws Exception{
        drain();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger calls=new AtomicInteger();
        NfcReaderControl.Listener listener=(t,state)->{};
        NfcReaderControl control=new NfcReaderControl(new NfcReaderControl.Service(){
            public NfcReaderControl.State enable()throws Exception{calls.incrementAndGet();entered.countDown();release.await();return NfcReaderControl.State.READY;}
            public void disable(){}public String diagnostic(){return "NFC-C";}
        },listener,Runnable::run);
        try{control.request(true,1);await(entered);long start=System.nanoTime();assertEquals("NFC-C",control.diagnostic());assertEquals(1,calls.get());assertTrue(System.nanoTime()-start<TimeUnit.MILLISECONDS.toNanos(100));}
        finally{control.close();release.countDown();drain();}
    }

}
