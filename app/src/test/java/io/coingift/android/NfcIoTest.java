package io.coingift.android;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class NfcIoTest {
    private static void await(CountDownLatch value)throws Exception{assertTrue(value.await(3,TimeUnit.SECONDS));}
    private static class Work implements NfcIo.Work {
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),closed=new CountDownLatch(1);
        final AtomicInteger discards=new AtomicInteger(),resourceCloses=new AtomicInteger();
        volatile boolean blocked,ignoreClose;
        Work(boolean blocked){this.blocked=blocked;}
        public Object run(NfcIo.Job job)throws Exception{
            job.own(()->{resourceCloses.incrementAndGet();if(!ignoreClose)release.countDown();});entered.countDown();
            if(blocked)while(release.getCount()>0)try{release.await();}catch(InterruptedException ignored){}
            return new Object();
        }
        public void close(){closed.countDown();}
        public void discard(Object value){if(value!=null)discards.incrementAndGet();}
    }
    private static class Listener implements NfcIo.Listener {
        final CountDownLatch delivered=new CountDownLatch(1);volatile Object result;volatile Exception failure;
        public void nfcFinished(NfcIo.Job job,Object result,Exception failure){this.result=result;this.failure=failure;delivered.countDown();}
    }
    private static void drain()throws Exception{
        NfcTestWorkers.awaitIdle(NfcIo.class,"IO");
    }
    @Test public void ownedConnectionIsClosedAndResultDelivered()throws Exception{
        drain();Work work=new Work(false);Listener listener=new Listener();NfcIo.start(work,listener,Runnable::run);await(listener.delivered);
        assertNull(listener.failure);assertNotNull(listener.result);assertEquals(1,work.resourceCloses.get());
    }
    @Test public void cancellationClosesConnectionWithoutWaitingAndSuppressesLateSuccess()throws Exception{
        drain();Work work=new Work(true);Listener listener=new Listener();NfcIo.Job job=NfcIo.start(work,listener,Runnable::run);await(work.entered);
        long started=System.nanoTime();job.cancel();assertTrue(System.nanoTime()-started<TimeUnit.MILLISECONDS.toNanos(300));await(work.closed);drain();
        assertEquals(1,work.resourceCloses.get());assertEquals(1,work.discards.get());assertEquals(1,listener.delivered.getCount());
    }
    @Test public void hungNfcCannotQueueUnlimitedWorkAndDoesNotOccupyAnotherExecutor()throws Exception{
        drain();Work stuck=new Work(true);stuck.ignoreClose=true;Listener first=new Listener();NfcIo.Job job=NfcIo.start(stuck,first,Runnable::run);await(stuck.entered);
        ExecutorService unrelated=Executors.newSingleThreadExecutor();
        try{
            job.cancel();assertEquals("crypto-ready",unrelated.submit(()->"crypto-ready").get(1,TimeUnit.SECONDS));
            for(int i=0;i<8;i++){Work next=new Work(false);Listener listener=new Listener();NfcIo.start(next,listener,Runnable::run);await(listener.delivered);assertTrue(listener.failure instanceof NfcIo.Unavailable);assertEquals(1,next.entered.getCount());await(next.closed);}
        }finally{stuck.release.countDown();await(stuck.closed);unrelated.shutdownNow();drain();}
        assertEquals(1,first.delivered.getCount());
    }
    @Test public void deadlineAndCallbackDelayNeverCommitSuccess()throws Exception{
        drain();Work work=new Work(false);Listener listener=new Listener();BlockingQueue<Runnable> callbacks=new ArrayBlockingQueue<>(1);
        NfcIo.Job job=NfcIo.start(work,listener,callbacks::add,30);Runnable callback=callbacks.poll(3,TimeUnit.SECONDS);assertNotNull(callback);
        while(!job.expired())Thread.sleep(5);callback.run();assertTrue(listener.failure instanceof NfcIo.Expired);assertNull(listener.result);assertEquals(1,work.discards.get());drain();
    }
    @Test public void cancellationAfterCompletionBeforeUiDeliveryDiscardsResult()throws Exception{
        drain();Work work=new Work(false);Listener listener=new Listener();BlockingQueue<Runnable> callbacks=new ArrayBlockingQueue<>(1);
        NfcIo.Job job=NfcIo.start(work,listener,callbacks::add);Runnable callback=callbacks.poll(3,TimeUnit.SECONDS);assertNotNull(callback);job.cancel();callback.run();
        assertEquals(1,work.discards.get());assertEquals(1,listener.delivered.getCount());drain();
    }
    @Test public void resourcesRegisteredAfterCancellationCannotBeUsed()throws Exception{
        drain();Work work=new Work(false);Listener listener=new Listener();BlockingQueue<Runnable> callbacks=new ArrayBlockingQueue<>(1);NfcIo.Job job=NfcIo.start(work,listener,callbacks::add);
        assertNotNull(callbacks.poll(3,TimeUnit.SECONDS));job.cancel();CountDownLatch closed=new CountDownLatch(1);
        try{job.own(closed::countDown);fail("Late resource accepted");}catch(IOException expected){}await(closed);drain();
    }
    @Test public void cleanupThatIgnoresCloseCannotBlockCancelCaller()throws Exception{
        drain();CountDownLatch opened=new CountDownLatch(1),closing=new CountDownLatch(1),finishClose=new CountDownLatch(1),finishWork=new CountDownLatch(1),workDone=new CountDownLatch(1);
        Listener listener=new Listener();NfcIo.Work work=new NfcIo.Work(){
            public Object run(NfcIo.Job job)throws Exception{job.own(()->{closing.countDown();while(finishClose.getCount()>0)try{finishClose.await();}catch(InterruptedException ignored){}});opened.countDown();while(finishWork.getCount()>0)try{finishWork.await();}catch(InterruptedException ignored){}return null;}
            public void discard(Object ignored){}public void close(){workDone.countDown();}
        };
        NfcIo.Job job=NfcIo.start(work,listener,Runnable::run);await(opened);
        try{long before=System.nanoTime();job.cancel();assertTrue(System.nanoTime()-before<TimeUnit.MILLISECONDS.toNanos(300));await(closing);}
        finally{finishClose.countDown();finishWork.countDown();await(workDone);drain();}
    }
    @Test public void fixedPhaseSurvivesFailureWithoutRetainingExceptionText()throws Exception{
        drain();Listener listener=new Listener();NfcIo.Job job=NfcIo.start(new NfcIo.Work(){
            public Object run(NfcIo.Job job)throws Exception{job.phase(NfcIo.Phase.NDEF_LOCK);throw new IOException("test-only exception payload");}
            public void discard(Object ignored){}public void close(){}
        },listener,Runnable::run);await(listener.delivered);assertTrue(listener.failure instanceof IOException);assertEquals("L3",job.phase().code);drain();
    }
    @Test public void cancelledWorkCannotReplaceItsLastDiagnosticPhase()throws Exception{
        drain();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),done=new CountDownLatch(1);AtomicBoolean rejected=new AtomicBoolean();
        NfcIo.Listener listener=(j,r,e)->fail("Cancelled work must not deliver");
        NfcIo.Job job=NfcIo.start(new NfcIo.Work(){
            public Object run(NfcIo.Job job)throws Exception{job.phase(NfcIo.Phase.CONFIG_LOCK);entered.countDown();while(release.getCount()>0)try{release.await();}catch(InterruptedException ignored){}try{job.phase(NfcIo.Phase.VERIFY_BITS);}catch(InterruptedIOException expected){rejected.set(true);}return null;}
            public void discard(Object ignored){}public void close(){done.countDown();}
        },listener,Runnable::run);await(entered);job.cancel();release.countDown();await(done);drain();assertTrue(rejected.get());assertEquals("H5",job.phase().code);
    }

}
