package io.coingift.android;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import io.coingift.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Uses production metadata rejection and Activity callbacks with controlled service stalls; no physical NFC claim. */
final class ReadOnlyAnrProbe {
    private final Instrumentation host;private MainActivity activity;private String payload;private int checks;
    ReadOnlyAnrProbe(Instrumentation host){this.host=host;}
    int run()throws Exception{
        try(InputStream in=host.getContext().getAssets().open("cg3-scalar-one.txt")){payload=new String(BackupFiles.readBounded(in),StandardCharsets.US_ASCII).trim();}
        Texts.select(host.getTargetContext(),"zh");
        try{
            launch();
            for(boolean protect:new boolean[]{true,false})readonly(protect);
            blockedDisable();startTimeout();processingCancel();lockCancel();pendingLockHeading();
            for(String language:new String[]{"zh","en","ja","ko"}){
                Texts.select(host.getTargetContext(),language);launch();
                main(()->invoke("showNfcError",new Class<?>[]{String.class},activity.getString(R.string.nfc_card_read_only)));
                yes(find(dialog("sheet"),activity.getString(R.string.nfc_card_read_only))!=null,"localized read-only explanation in "+language);
                yes(dialog("sheet").getWindow().getAttributes().gravity==Gravity.BOTTOM,"read-only error uses app bottom sheet");main(()->invoke("closeSheet"));
            }
            return checks;
        }finally{if(activity!=null)main(activity::finish);Texts.select(host.getTargetContext(),"zh");}
    }
    private void launch(){if(activity!=null){main(activity::finish);host.waitForIdleSync();}activity=(MainActivity)host.startActivitySync(new Intent(host.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));host.waitForIdleSync();}
    private void replace(NfcReaderControl.Service service){
        main(()->{((NfcReaderControl)get("nfcReader")).close();set("nfcReader",new NfcReaderControl(service,activity,command->((Handler)get("handler")).post(command)));});
    }
    private static NfcReaderControl.Service ready(){return new NfcReaderControl.Service(){public NfcReaderControl.State enable(){return NfcReaderControl.State.READY;}public void disable(){}};}
    private CreationSession prepare(){
        CreationSession session=new CreationSession(payload);main(()->{set("session",session);set("screen","write");set("candidate",new NfcCards.Candidate(new byte[]{4,1},new byte[]{8},512));invoke("render");});return session;
    }
    private static final class ReadOnly implements NfcCards.Card {
        final AtomicInteger commands=new AtomicInteger();public boolean writable(){return false;}public int capacity(){return 888;}
        public void connect(){commands.incrementAndGet();throw new AssertionError("Read-only tag must not connect");}public byte[] read(){commands.incrementAndGet();return new byte[]{1};}public void write(byte[] v){commands.incrementAndGet();}public void close(){commands.incrementAndGet();}
    }
    private NfcIo.Job attach(NfcIo.Work work,String operation,boolean protect)throws Exception{
        NfcIo.Job[] value={null};main(()->{
            Object op=Enum.valueOf((Class)modeType(),operation);set("mode",op);set("nfcOperation",op);set("nfcOwner",get("session"));set("nfcProtect",protect);set("nfcToken",get("epoch"));set("nfcBusy",true);
            value[0]=NfcIo.start(work,activity,command->((Handler)get("handler")).post(command));set("nfcJob",value[0]);
            ((Handler)get("handler")).postDelayed((Runnable)get("expireNfcTask"),value[0].remainingMillis());
        });return value[0];
    }
    private NfcIo.Work readonlyWork(ReadOnly card){return new NfcIo.Work(){public Object run(NfcIo.Job job)throws Exception{return new NfcCards.Result(null,null,null,NfcCards.check(new byte[]{4,1},card,400,job));}public void discard(Object value){if(value instanceof NfcCards.Result)((NfcCards.Result)value).clear();}public void close(){}};}
    private void readonly(boolean protect)throws Exception{
        replace(ready());CreationSession owner=prepare();ReadOnly card=new ReadOnly();
        main(()->set("lockAfterWrite",protect));long start=SystemClock.elapsedRealtime();attach(readonlyWork(card),"CHECK",protect);until(()->get("nfcJob")==null,3000);
        yes(SystemClock.elapsedRealtime()-start<3000,"already read-only card fails promptly with protection "+protect);
        yes(card.commands.get()==0,"no connect/read/write/lock commands on read-only rejection");
        yes(get("candidate")==null&&!((Boolean)get("nfcBusy"))&&"NONE".equals(get("mode").toString()),"reject resets candidate, mode and busy state");
        yes(owner.cardCount()==0&&!owner.backupSaved()&&!owner.canFinish(),"readonly rejection never confirms a write or bypasses mandatory backup");
        yes(find(dialog("sheet"),activity.getString(R.string.nfc_card_read_only))!=null,"read-only rejection shows actionable error, not overwrite confirmation");
        main(()->invoke("closeSheet"));yes(get("sheet")==null&&get("session")==owner,"error dismisses and preserves encrypted wallet for another card");drain();
    }
    private void blockedDisable()throws Exception{
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        replace(new NfcReaderControl.Service(){public NfcReaderControl.State enable(){return NfcReaderControl.State.READY;}public void disable()throws Exception{entered.countDown();release.await();}});
        CreationSession owner=prepare();ReadOnly card=new ReadOnly();
        try{
            attach(readonlyWork(card),"CHECK",true);until(()->get("sheet")!=null&&get("nfcJob")==null,3000);yes(entered.await(3,TimeUnit.SECONDS),"NFC disable intentionally remains blocked");
            long start=SystemClock.elapsedRealtime();main(()->invoke("closeSheet"));yes(SystemClock.elapsedRealtime()-start<1000,"main thread can dismiss read-only error while service disable is blocked");
            Future<String> crypto=((ExecutorService)get("worker")).submit(()->"crypto-ready");yes("crypto-ready".equals(crypto.get(1,TimeUnit.SECONDS)),"NFC control does not occupy crypto worker");
            yes(get("session")==owner&&get("candidate")==null,"blocked service does not change wallet outcome");
        }finally{release.countDown();drainControl();drain();replace(ready());}
    }
    private void startTimeout()throws Exception{
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),disabled=new CountDownLatch(1);
        replace(new NfcReaderControl.Service(){public NfcReaderControl.State enable()throws Exception{entered.countDown();release.await();return NfcReaderControl.State.READY;}public void disable(){disabled.countDown();}});
        drainControl();
        try{
            long start=SystemClock.elapsedRealtime();main(()->invoke("showNfcPrompt",new Class<?>[]{modeType()},Enum.valueOf((Class)modeType(),"READ")));
            yes(entered.await(3,TimeUnit.SECONDS),"NFC startup intentionally remains blocked");
            until(()->get("nfcDialog")==null&&get("sheet")!=null,7500);
            long elapsed=SystemClock.elapsedRealtime()-start;yes(elapsed>=4800&&elapsed<7500,"reader startup deadline returns UI in five seconds");
            yes(find(dialog("sheet"),activity.getString(R.string.nfc_startup_timeout,"NFC-S"))!=null,"startup error identifies a blocked service rather than claiming a card operation timed out");
            main(()->invoke("closeSheet"));yes("NONE".equals(get("mode").toString()),"startup timeout abandons mode");
        }finally{release.countDown();yes(disabled.await(3,TimeUnit.SECONDS),"late enable followed by disable");drainControl();}
        main(()->{});yes(get("nfcDialog")==null&&get("incoming")==null,"late service-ready cannot revive reader or import data");replace(ready());
    }
    private void processingCancel()throws Exception{
        CreationSession owner=prepare();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),closed=new CountDownLatch(1);AtomicInteger discards=new AtomicInteger();
        main(()->invoke("showNfcPrompt",new Class<?>[]{modeType()},Enum.valueOf((Class)modeType(),"WRITE")));
        NfcIo.Work work=new NfcIo.Work(){public Object run(NfcIo.Job job)throws Exception{entered.countDown();while(release.getCount()>0)try{release.await();}catch(InterruptedException ignored){}return new NfcCards.Result(null,null,new byte[]{4,1},null);}public void discard(Object value){if(value instanceof NfcCards.Result){discards.incrementAndGet();((NfcCards.Result)value).clear();}}public void close(){closed.countDown();}};
        attach(work,"WRITE",true);yes(entered.await(3,TimeUnit.SECONDS),"controlled write blocks");
        try{
            main(()->((NfcScanDialog)get("nfcDialog")).processing("正在写入并核对","正在读取，请保持卡片贴合…"));
            Button cancel=(Button)find(dialog("nfcDialog"),Texts.tr(activity,"取消"));yes(cancel!=null&&cancel.isEnabled(),"cancel remains enabled while processing");
            long start=SystemClock.elapsedRealtime();long[] actionMillis={0};main(()->{long began=SystemClock.elapsedRealtime();cancel.performClick();actionMillis[0]=SystemClock.elapsedRealtime()-began;});long totalMillis=SystemClock.elapsedRealtime()-start;yes(totalMillis<1000,"visible cancel returns promptly even if write ignores interruption: total="+totalMillis+" ms, main action="+actionMillis[0]+" ms");
            yes(get("nfcDialog")==null&&get("nfcJob")==null&&get("candidate")==null,"cancel clears visible sheet, task and candidate");
            yes(owner.cardCount()==0&&!owner.canFinish(),"cancel never confirms write");
        }finally{release.countDown();yes(closed.await(3,TimeUnit.SECONDS),"test write released");drain();}
        yes(discards.get()==1&&owner.cardCount()==0,"late write result discarded");
    }
    private void lockCancel(){
        main(()->{set("mode",Enum.valueOf((Class)modeType(),"LOCK"));set("screen","done");invoke("cancelNfc");});
        yes("unknown".equals(get("lockStatus")),"cancelling lock preserves unknown outcome, never locked");
    }
    private void pendingLockHeading(){
        // This page is reachable only after a confirmed write, not the preceding cancelled-write fixture.
        CreationSession written=prepare();written.markCardWritten(new byte[]{4,1});
        main(()->{set("lockAfterWrite",true);set("screen","done");set("lockStatus","unprotected");invoke("render");});
        yes(find(activity.getWindow().getDecorView(),activity.getString(R.string.nfc_written_not_locked))!=null,"selected read-only without completed lock is not labelled complete");
        main(()->{set("lockStatus","unknown");invoke("render");});
        yes(find(activity.getWindow().getDecorView(),activity.getString(R.string.nfc_written_not_locked))!=null,"unknown hardware lock remains explicitly incomplete");
        main(()->{set("lockStatus","locked");invoke("render");});
        yes(find(activity.getWindow().getDecorView(),activity.getString(R.string.nfc_written_not_locked))==null,"verified hardware lock clears pending heading");
        main(()->{set("lockAfterWrite",false);invoke("render");});
    }
    private void drain()throws Exception{drainPool(NfcIo.class,"IO");}
    private void drainControl()throws Exception{drainPool(NfcReaderControl.class,"IO");}
    private void drainPool(Class<?> type,String field)throws Exception{Field f=type.getDeclaredField(field);f.setAccessible(true);ThreadPoolExecutor pool=(ThreadPoolExecutor)f.get(null);until(()->pool.getActiveCount()==0&&pool.getQueue().isEmpty(),3000);}
    private Dialog dialog(String field){return (Dialog)get(field);}
    private View find(Dialog d,String text){return d==null?null:find(d.getWindow().getDecorView(),text);}
    private View find(View root,String text){if(root instanceof TextView&&text.contentEquals(((TextView)root).getText()))return root;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){View v=find(g.getChildAt(i),text);if(v!=null)return v;}}return null;}
    private Class<?> modeType(){try{return Class.forName("io.coingift.android.MainActivity$Mode");}catch(Exception e){throw new RuntimeException(e);}}
    private Object get(String name){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception e){throw new RuntimeException(e);}}
    private void set(String name,Object v){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,v);}catch(Exception e){throw new RuntimeException(e);}}
    private void invoke(String name){invoke(name,new Class<?>[0]);}
    private void invoke(String name,Class<?>[] args,Object...v){try{Method m=MainActivity.class.getDeclaredMethod(name,args);m.setAccessible(true);m.invoke(activity,v);}catch(Exception e){throw new RuntimeException(e);}}
    private void main(Runnable r){Throwable[] error={null};host.runOnMainSync(()->{try{r.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError("readonly UI",error[0]);}
    private void until(java.util.function.BooleanSupplier predicate,long timeout)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){boolean[] ok={false};main(()->ok[0]=predicate.getAsBoolean());if(ok[0])return;Thread.sleep(20);}throw new AssertionError("readonly probe async timeout");}
    private void yes(boolean value,String why){if(!value)throw new AssertionError(why);checks++;}
}
