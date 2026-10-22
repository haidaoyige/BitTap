package io.coingift.android;

import android.app.*;
import android.content.*;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import io.coingift.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Controlled transport failures only. Does not claim physical NFC or permanent-lock validation. */
final class NfcSafetyProbe {
    private final Instrumentation host;private MainActivity activity;private String payload;private int checks;
    NfcSafetyProbe(Instrumentation host){this.host=host;}
    int run()throws Exception{
        try(InputStream in=host.getContext().getAssets().open("cg3-scalar-one.txt")){payload=new String(BackupFiles.readBounded(in),StandardCharsets.US_ASCII).trim();}
        Texts.select(host.getTargetContext(),"zh");
        try{
            launch();readTimeout();cancelHungWrite();unknownLock("LOCK");unknownLock("VERIFY_LOCK");background();
            for(String language:new String[]{"zh","en","ja","ko"}){
                Texts.select(host.getTargetContext(),language);launch();
                for(int id:new int[]{R.string.nfc_operation_timeout,R.string.nfc_operation_busy,R.string.nfc_write_unconfirmed}){
                    String text=activity.getString(id);main(()->invoke("showNfcError",new Class<?>[]{String.class},text));Dialog sheet=(Dialog)get("sheet");
                    yes(sheet!=null&&sheet.getWindow().getAttributes().gravity==Gravity.BOTTOM,"NFC errors retain app sheet style in "+language);
                    yes(contains(sheet.getWindow().getDecorView(),text),"localized NFC error visible");main(()->invoke("closeSheet"));
                }
            }
            return checks;
        }finally{if(activity!=null)main(activity::finish);Texts.select(host.getTargetContext(),"zh");}
    }
    private void launch(){if(activity!=null){main(activity::finish);host.waitForIdleSync();}activity=(MainActivity)host.startActivitySync(new Intent(host.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));host.waitForIdleSync();}
    private static final class Transport implements NfcIo.Work {
        final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),done=new CountDownLatch(1),closed=new CountDownLatch(1);final AtomicInteger discarded=new AtomicInteger();
        final boolean ignoreClose;final NfcCards.Result value;
        Transport(boolean ignoreClose,NfcCards.Result value){this.ignoreClose=ignoreClose;this.value=value;}
        public Object run(NfcIo.Job job)throws Exception{job.own(()->{closed.countDown();if(!ignoreClose)release.countDown();});entered.countDown();while(release.getCount()>0)try{release.await();}catch(InterruptedException ignored){}return value;}
        public void close(){done.countDown();}public void discard(Object result){if(result instanceof NfcCards.Result){discarded.incrementAndGet();((NfcCards.Result)result).clear();}}
    }
    private NfcIo.Job attach(Transport transport,String mode,long timeout)throws Exception{
        final NfcIo.Job[] job={null};
        main(()->{
            Class<?> type=type();Object operation=Enum.valueOf((Class)type,mode);set("mode",operation);set("nfcOperation",operation);set("nfcOwner",get("session"));set("nfcProtect",true);set("nfcToken",get("epoch"));set("nfcBusy",true);
            job[0]=NfcIo.start(transport,activity,command->((android.os.Handler)get("handler")).post(command),timeout);set("nfcJob",job[0]);
            ((android.os.Handler)get("handler")).postDelayed((Runnable)get("expireNfcTask"),job[0].remainingMillis());
        });yes(transport.entered.await(5,TimeUnit.SECONDS),"controlled transport starts");return job[0];
    }
    private void readTimeout()throws Exception{
        NfcCards.Result late=new NfcCards.Result(payload,"04010203",null,null);Transport t=new Transport(false,late);long began=SystemClock.elapsedRealtime();
        attach(t,"READ",NfcIo.TIMEOUT_MS);
        // Real public-fixture crypto on the Activity's production executor must finish while NFC waits.
        Future<Boolean> decrypted=((ExecutorService)get("worker")).submit(()->{char[] p="test fixture password only".toCharArray();try(WalletCrypto.Unlocked u=new WalletCrypto().decrypt(payload,p)){return u.addresses!=null;}finally{WalletCrypto.wipe(p);}});
        yes(decrypted.get(8,TimeUnit.SECONDS),"actual decrypt worker remains usable during blocked NFC");yes(get("nfcJob")!=null,"NFC is still pending during independent decryption");
        until(()->get("nfcJob")==null,18000);long elapsed=SystemClock.elapsedRealtime()-began;
        yes(elapsed>=14500&&elapsed<19000,"actual NFC total deadline is fifteen seconds");
        yes(t.closed.await(3,TimeUnit.SECONDS),"timeout attempts to close active connection");yes(t.done.await(3,TimeUnit.SECONDS),"closing connection releases cooperative blocked transport");
        yes(get("incoming")==null&&get("unlocked")==null,"late read neither imports payload nor unlocks");
        yes(get("sheet")!=null,"timeout produces explicit app error");main(()->invoke("closeSheet"));drain();
    }
    private void cancelHungWrite()throws Exception{
        CreationSession owner=new CreationSession(payload);NfcCards.Candidate candidate=new NfcCards.Candidate(new byte[]{4,1},new byte[]{7,8},512);
        main(()->{set("session",owner);set("candidate",candidate);set("screen","write");invoke("render");});
        byte[] uid={4,1};Transport t=new Transport(true,new NfcCards.Result(null,null,uid,null));NfcIo.Job old=attach(t,"WRITE",NfcIo.TIMEOUT_MS);
        try{
            long start=SystemClock.elapsedRealtime();main(()->invoke("cancelNfc"));yes(SystemClock.elapsedRealtime()-start<1000,"cancel returns without waiting for uncooperative transport");
            yes(get("nfcJob")==null&&!((Boolean)get("nfcBusy"))&&get("candidate")==null,"cancel clears busy state and forces a new pre-write check");
            yes(owner.cardCount()==0&&!owner.canFinish()&&!owner.backupSaved(),"uncertain write cannot establish success or bypass backup");
            CountDownLatch rejected=new CountDownLatch(1);AtomicBoolean unavailable=new AtomicBoolean();NfcIo.Listener observer=(j,r,e)->{unavailable.set(e instanceof NfcIo.Unavailable);rejected.countDown();};
            Transport retry=new Transport(false,null);NfcIo.start(retry,observer,Runnable::run);yes(rejected.await(3,TimeUnit.SECONDS)&&unavailable.get(),"no NFC tasks queue behind an uncooperative operation");
            Future<String> other=((ExecutorService)get("worker")).submit(()->"ready");yes("ready".equals(other.get(1,TimeUnit.SECONDS)),"cancelled NFC cannot occupy crypto executor");
        }finally{t.release.countDown();yes(t.done.await(3,TimeUnit.SECONDS),"controlled hung write released");drain();}
        main(()->activity.nfcFinished(old,new NfcCards.Result(null,null,new byte[]{4,1},null),null));
        yes(owner.cardCount()==0&&"write".equals(get("screen")),"old successful write callback cannot alter wallet state");
    }
    private void unknownLock(String operation)throws Exception{
        CreationSession owner=new CreationSession(payload);owner.markCardWritten(new byte[]{4,1});
        main(()->{set("session",owner);set("candidate",new NfcCards.Candidate(new byte[]{4,1},new byte[0],512));set("screen","done");invoke("render");});
        Transport t=new Transport(false,new NfcCards.Result(null,null,new byte[]{4,1},null));NfcIo.Job old=attach(t,operation,100);
        until(()->get("nfcJob")==null,3000);yes("unknown".equals(get("lockStatus")),operation+" timeout preserves unknown lock state");
        yes(!owner.backupSaved()&&!owner.canFinish(),"unknown lock cannot satisfy backup");yes(t.done.await(3,TimeUnit.SECONDS),"lock task cancelled");drain();
        main(()->{invoke("closeSheet");activity.nfcFinished(old,new NfcCards.Result(null,null,new byte[]{4,1},null),null);});
        yes("unknown".equals(get("lockStatus")),"late lock success never claims permanent protection");
    }
    private void background()throws Exception{
        Transport t=new Transport(false,new NfcCards.Result(payload,"04010203",null,null));attach(t,"READ",NfcIo.TIMEOUT_MS);
        main(()->{host.callActivityOnPause(activity);host.callActivityOnStop(activity);});yes(get("nfcJob")==null&&get("unlocked")==null,"background cancels NFC and secret session");
        yes(t.done.await(3,TimeUnit.SECONDS),"background closes registered transport");drain();main(()->host.callActivityOnResume(activity));yes(get("incoming")==null,"background late read is ignored");
    }
    private void drain()throws Exception{ThreadPoolExecutor pool;Field f=NfcIo.class.getDeclaredField("IO");f.setAccessible(true);pool=(ThreadPoolExecutor)f.get(null);until(()->pool.getActiveCount()==0,3000);Thread.sleep(30);}
    private Class<?> type(){try{return Class.forName("io.coingift.android.MainActivity$Mode");}catch(Exception ex){throw new RuntimeException(ex);}}
    private Object get(String name){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void set(String name,Object value){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void invoke(String name,Class<?>[] types,Object...args){try{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(activity,args);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void invoke(String name){invoke(name,new Class<?>[0]);}
    private static boolean contains(View root,String text){if(root instanceof TextView&&text.contentEquals(((TextView)root).getText()))return true;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++)if(contains(g.getChildAt(i),text))return true;}return false;}
    private void main(Runnable action){Throwable[] error={null};host.runOnMainSync(()->{try{action.run();}catch(Throwable ex){error[0]=ex;}});if(error[0]!=null)throw new AssertionError("NFC UI operation",error[0]);}
    private void until(java.util.function.BooleanSupplier predicate,long timeout)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;while(!predicate.getAsBoolean()&&SystemClock.elapsedRealtime()<end){Thread.sleep(25);main(()->{});}yes(predicate.getAsBoolean(),"async completes");host.waitForIdleSync();}
    private void yes(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
}
