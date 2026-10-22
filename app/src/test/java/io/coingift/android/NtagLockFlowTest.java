package io.coingift.android;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Independent page-permission model from the public NXP data sheet; never physical-card acceptance. */
public class NtagLockFlowTest {
    private static class Chip implements NtagProtection.Link {
        final int model,last,dynamic,config;final byte[][] pages;final List<Integer> writes=new ArrayList<>();
        int connects,closes,timeout,failPage=-1,dropPage=-1,shortPage=-1;boolean frozen;byte[] version;
        Chip(int model){
            this.model=model;last=model==215?129:225;dynamic=last+1;config=dynamic+1;pages=new byte[model==215?135:231][4];
            for(int p=0;p<pages.length;p++)for(int i=0;i<4;i++)pages[p][i]=(byte)(p*17+i*3);
            pages[2]=new byte[]{(byte)0xa3,0x48,0,0};pages[3]=new byte[]{(byte)0xe1,0x10,(byte)(model==215?0x3e:0x6d),0};
            pages[dynamic]=new byte[]{0,0,0,(byte)0xbd};pages[config]=new byte[]{4,0,0,(byte)255};pages[config+1]=new byte[]{0x1a,0,0,0};
            version=new byte[]{0,4,4,2,1,0,(byte)(model==215?0x11:0x13),3};
        }
        public void connect(){connects++;}public void timeout(int ms){timeout=ms;}public byte[] version(){return version.clone();}public void close(){closes++;}
        public byte[] readPages(int first){byte[] bytes=new byte[first==shortPage?8:16];for(int i=0;i<bytes.length;i++)bytes[i]=pages[(first+i/4)%pages.length][i%4];return bytes;}
        boolean userLocked(int page){
            if(page>=3&&page<=7)return (pages[2][2]&(1<<page))!=0;
            if(page>=8&&page<=15)return (pages[2][3]&(1<<(page-8)))!=0;
            if(page>=16&&page<=last){int bit=(page-16)/16;return (pages[dynamic][bit/8]&(1<<(bit%8)))!=0;}
            return false;
        }
        public void writePage(int page,byte[] value)throws IOException{
            if(page==failPage)throw new IOException("controlled write failure");
            if(userLocked(page)||(frozen&&(page==config||page==config+1)))throw new IOException("hardware write protected");
            if(page==dynamic){
                // These are the actual legal WRITE masks, independently enforced by the model.
                int byte1=model==215?0:0x3f,byte2=model==215?15:0x7f;
                if((value[1]&~byte1)!=0||(value[2]&~byte2)!=0||value[3]!=0)throw new IOException("RFUI must be zero");
            }
            writes.add(page);if(page==dropPage)return;
            if(page==2){pages[2][2]|=value[2];pages[2][3]|=value[3];}
            else if(page==3){for(int i=0;i<4;i++)pages[3][i]|=value[i];}
            else if(page==dynamic){for(int i=0;i<3;i++)pages[page][i]|=value[i];}
            else pages[page]=value.clone();
        }
        void powerCycle(){frozen=(pages[config+1][0]&0x40)!=0;}
        byte[] userBytes(){ByteArrayOutputStream b=new ByteArrayOutputStream();for(int p=4;p<=last;p++)b.write(pages[p],0,4);return b.toByteArray();}
    }
    private interface Action{void run(NfcIo.Job job)throws Exception;}
    private static class Outcome{Exception error;NfcIo.Phase phase;}
    private Outcome run(Action action)throws Exception{
        java.lang.reflect.Field f=NfcIo.class.getDeclaredField("IO");f.setAccessible(true);ThreadPoolExecutor pool=(ThreadPoolExecutor)f.get(null);
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(pool.getActiveCount()!=0&&System.nanoTime()<end)Thread.sleep(5);Thread.sleep(10);
        Outcome result=new Outcome();CountDownLatch done=new CountDownLatch(1);NfcIo.Listener listener=(j,v,e)->{result.error=e;result.phase=j.phase();done.countDown();};
        NfcIo.start(new NfcIo.Work(){public Object run(NfcIo.Job job)throws Exception{action.run(job);return null;}public void discard(Object ignored){}public void close(){}},listener,Runnable::run);
        assertTrue(done.await(3,TimeUnit.SECONDS));return result;
    }
    @Test public void bothModelsLockEveryUserPageAndConfigAfterPowerCycleWithoutChangingWallet()throws Exception{
        for(int model:new int[]{215,216}){
            Chip c=new Chip(model);byte[] user=c.userBytes(),uid=Arrays.copyOf(c.pages[2],2),cfg=c.pages[c.config].clone();
            Outcome locked=run(j->NtagProtection.lock(c,j));assertNull(locked.error);assertEquals(3000,c.timeout);assertEquals(1,c.closes);
            assertArrayEquals(user,c.userBytes());assertArrayEquals(uid,Arrays.copyOf(c.pages[2],2));assertArrayEquals(cfg,c.pages[c.config]);assertEquals(0x5a,c.pages[c.config+1][0]&255);
            assertEquals(Arrays.asList(3,c.dynamic,c.config+1,2),c.writes);
            for(int p=3;p<=c.last;p++){assertTrue("unlocked page "+p+" on model "+model,c.userLocked(p));final int page=p;assertThrows(IOException.class,()->c.writePage(page,new byte[]{1,2,3,4}));}
            c.powerCycle();assertThrows(IOException.class,()->c.writePage(c.config,new byte[]{0,0,0,0}));assertThrows(IOException.class,()->c.writePage(c.config+1,new byte[]{0,0,0,0}));
            assertNull(run(j->NtagProtection.verify(c,j)).error);
        }
    }
    @Test public void reservedBdReadbackIsNeverWrittenBackAndLockIsIdempotent()throws Exception{
        for(int model:new int[]{215,216}){Chip c=new Chip(model);assertNull(run(j->NtagProtection.lock(c,j)).error);assertEquals(0xbd,c.readPages(c.dynamic)[3]&255);int count=c.writes.size();c.powerCycle();assertNull(run(j->NtagProtection.lock(c,j)).error);assertEquals(count,c.writes.size());}
    }
    @Test public void unsupportedVersionAndMalformedCcAreRejectedBeforeAnyWrite()throws Exception{
        Chip wrong=new Chip(215);wrong.version[6]=0x0f;assertTrue(run(j->NtagProtection.lock(wrong,j)).error instanceof IllegalArgumentException);assertTrue(wrong.writes.isEmpty());
        for(int index:new int[]{0,1,2,3}){Chip c=new Chip(216);c.pages[3][index]=index==2?0:(byte)0xfe;assertTrue(run(j->NtagProtection.lock(c,j)).error instanceof IllegalArgumentException);assertTrue(c.writes.isEmpty());}
    }
    @Test public void existingMirrorOrAccessProtectionIsNeverModified()throws Exception{
        Chip mirror=new Chip(215);mirror.pages[mirror.config][0]=(byte)0xc4;assertTrue(run(j->NtagProtection.lock(mirror,j)).error instanceof IllegalArgumentException);assertTrue(mirror.writes.isEmpty());
        Chip auth=new Chip(216);auth.pages[auth.config][3]=4;assertTrue(run(j->NtagProtection.lock(auth,j)).error instanceof IllegalArgumentException);assertTrue(auth.writes.isEmpty());
    }
    @Test public void frozenPartialLockConfigurationsAreRejectedBeforeCcChange()throws Exception{
        Chip stat=new Chip(215);stat.pages[2][2]=2;assertTrue(run(j->NtagProtection.lock(stat,j)).error instanceof IllegalArgumentException);assertTrue(stat.writes.isEmpty());
        for(int model:new int[]{215,216}){Chip dynamic=new Chip(model);dynamic.pages[dynamic.dynamic][2]=1;assertTrue(run(j->NtagProtection.lock(dynamic,j)).error instanceof IllegalArgumentException);assertTrue(dynamic.writes.isEmpty());}
    }
    @Test public void ackWithoutProgrammingBitsCannotContinueToOtherIrreversibleSteps()throws Exception{
        Chip c=new Chip(216);c.dropPage=c.dynamic;Outcome result=run(j->NtagProtection.lock(c,j));assertTrue(result.error instanceof IllegalStateException);assertEquals(NfcIo.Phase.DYNAMIC_LOCK,result.phase);assertEquals(Arrays.asList(3,c.dynamic),c.writes);assertEquals(0,c.pages[2][2]);assertEquals(0,c.pages[c.config+1][0]&0x40);assertEquals(1,c.closes);
    }
    @Test public void communicationFailureStopsAndPreservesLastStage()throws Exception{
        Chip c=new Chip(215);c.failPage=c.config+1;Outcome result=run(j->NtagProtection.lock(c,j));assertTrue(result.error instanceof IOException);assertEquals(NfcIo.Phase.CONFIG_LOCK,result.phase);assertEquals(Arrays.asList(3,c.dynamic),c.writes);assertEquals(0,c.pages[2][2]);assertEquals(1,c.closes);
    }
    @Test public void truncatedReadsAreRejectedBeforeAnyIrreversibleWrite()throws Exception{
        Chip c=new Chip(216);c.shortPage=c.dynamic;assertTrue(run(j->NtagProtection.lock(c,j)).error instanceof IllegalStateException);assertTrue(c.writes.isEmpty());assertEquals(1,c.closes);
    }
    @Test public void ccOnlyOrIncompleteHardwareProtectionNeverVerifies()throws Exception{
        Chip cc=new Chip(215);cc.pages[3][3]=15;assertTrue(run(j->NtagProtection.verify(cc,j)).error instanceof IllegalStateException);
        Chip partial=new Chip(216);assertNull(run(j->NtagProtection.lock(partial,j)).error);partial.pages[partial.dynamic][1]&=0x1f;assertTrue(run(j->NtagProtection.verify(partial,j)).error instanceof IllegalStateException);
    }
}
