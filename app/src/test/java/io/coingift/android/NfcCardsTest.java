package io.coingift.android;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class NfcCardsTest {
    private static final byte[] UID={4,1,2};
    private static class Card implements NfcCards.Card {
        boolean writable=true,badReadback;int capacity=512,connects,reads,writes,closes;byte[] data=new byte[]{1,2};
        public boolean writable(){return writable;}public int capacity(){return capacity;}
        public void connect(){connects++;}public byte[] read(){reads++;return badReadback&&writes>0?new byte[]{0}:data.clone();}
        public void write(byte[] value){writes++;data=value.clone();}public void close(){closes++;}
    }
    private interface Action{Object run(NfcIo.Job job)throws Exception;}
    private static class Outcome {Object value;Exception failure;}
    private Outcome run(Action action)throws Exception{
        // Drain the shared worker from previous test callbacks before submitting.
        NfcTestWorkers.awaitIdle(NfcIo.class,"IO");
        Outcome outcome=new Outcome();CountDownLatch done=new CountDownLatch(1);NfcIo.Listener listener=(j,v,e)->{outcome.value=v;outcome.failure=e;done.countDown();};
        NfcIo.start(new NfcIo.Work(){public Object run(NfcIo.Job job)throws Exception{return action.run(job);}public void discard(Object v){}public void close(){}},listener,Runnable::run);
        assertTrue(done.await(3,TimeUnit.SECONDS));return outcome;
    }
    @Test public void permanentlyReadOnlyTagIsRejectedWithoutAnyConnectionOrCommands()throws Exception{
        Card card=new Card(){public void connect(){throw new AssertionError("must not connect to a discovered read-only tag");}};card.writable=false;
        Outcome r=run(job->NfcCards.check(UID,card,200,job));assertTrue(r.failure instanceof NfcCards.ReadOnlyCard);assertNull(r.value);assertEquals(0,card.connects+card.reads+card.writes+card.closes);
    }
    @Test public void sameTagBecomingReadOnlyBetweenCheckAndWriteIsRejectedBeforeConnection()throws Exception{
        Card card=new Card();Outcome checked=run(job->NfcCards.check(UID,card,200,job));assertNull(checked.failure);card.writable=false;int connections=card.connects;
        Outcome write=run(job->{NfcCards.writeAndVerify(UID,card,(NfcCards.Candidate)checked.value,new byte[]{9},job);return null;});
        assertTrue(write.failure instanceof NfcCards.ReadOnlyCard);assertEquals(connections,card.connects);assertEquals(0,card.writes);
    }
    @Test public void insufficientCapacityDoesNotConnect()throws Exception{
        Card card=new Card();card.capacity=100;Outcome r=run(job->NfcCards.check(UID,card,200,job));assertTrue(r.failure instanceof IllegalArgumentException);assertEquals(0,card.connects+card.reads+card.closes);
    }
    @Test public void writableCardUsesLiveReadAndSeparatePostWriteReadback()throws Exception{
        Card card=new Card();Outcome checked=run(job->NfcCards.check(UID,card,200,job));assertNull(checked.failure);
        Outcome written=run(job->{NfcCards.writeAndVerify(UID,card,(NfcCards.Candidate)checked.value,new byte[]{8,9},job);return null;});
        assertNull(written.failure);assertEquals(2,card.connects);assertEquals(3,card.reads);assertEquals(1,card.writes);assertEquals(2,card.closes);
    }
    @Test public void changedContentsAndFailedReadbackNeverReportWriteSuccess()throws Exception{
        Card card=new Card();NfcCards.Candidate candidate=new NfcCards.Candidate(UID,new byte[]{5},512);
        Outcome changed=run(job->{NfcCards.writeAndVerify(UID,card,candidate,new byte[]{8},job);return null;});assertNotNull(changed.failure);assertEquals(0,card.writes);
        NfcCards.Candidate correct=new NfcCards.Candidate(UID,card.data.clone(),512);card.badReadback=true;
        Outcome mismatch=run(job->{NfcCards.writeAndVerify(UID,card,correct,new byte[]{8},job);return null;});assertTrue(mismatch.failure instanceof IllegalStateException);assertEquals(1,card.writes);assertEquals(2,card.closes);
    }
}
