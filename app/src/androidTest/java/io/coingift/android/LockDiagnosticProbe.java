package io.coingift.android;

import android.app.*;
import android.content.*;
import android.nfc.TagLostException;
import android.os.*;
import android.view.*;
import android.widget.*;
import io.coingift.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Controlled failures use the production completion handler. No permanent lock commands are sent. */
final class LockDiagnosticProbe {
    private final Instrumentation host;private MainActivity activity;private String payload;private int checks;
    private static final String SECRET="DO_NOT_RENDER_TEST_EXCEPTION_PAYLOAD";
    LockDiagnosticProbe(Instrumentation host){this.host=host;}
    int run()throws Exception{
        try(InputStream in=host.getContext().getAssets().open("cg3-scalar-one.txt")){payload=new String(BackupFiles.readBounded(in),StandardCharsets.US_ASCII).trim();}
        try{
            for(String language:new String[]{"zh","en","ja","ko"}){
                Texts.select(host.getTargetContext(),language);launch();
                failure("LOCK",NfcIo.Phase.MATCH_WRITTEN,new TagLostException(SECRET),"LOST",R.string.nfc_lock_reason_lost);
                failure("LOCK",NfcIo.Phase.NDEF_LOCK,new IllegalStateException(SECRET),"STATE",R.string.nfc_lock_reason_check);
                failure("LOCK",NfcIo.Phase.DYNAMIC_LOCK,new IOException(SECRET),"IO",R.string.nfc_lock_reason_io);
                failure("LOCK",NfcIo.Phase.CONFIG_LOCK,new NfcIo.Expired(),"TIME",R.string.nfc_lock_reason_timeout);
                failure("LOCK",NfcIo.Phase.CHIP_VERSION,new IllegalArgumentException(SECRET),"CARD",R.string.nfc_lock_reason_check);
                failure("VERIFY_LOCK",NfcIo.Phase.VERIFY_BITS,new IllegalStateException(SECRET),"STATE",R.string.nfc_lock_reason_check);
                failure("VERIFY_LOCK",NfcIo.Phase.VERIFY_READONLY,new NfcIo.Unavailable(),"BUSY",R.string.nfc_lock_reason_busy);
                failure("VERIFY_LOCK",NfcIo.Phase.VERIFY_WRITTEN,new Exception(SECRET),"OTHER",R.string.nfc_lock_reason_check);
            }
            return checks;
        }finally{if(activity!=null)main(activity::finish);Texts.select(host.getTargetContext(),"zh");}
    }
    private void launch(){if(activity!=null){main(activity::finish);host.waitForIdleSync();}activity=(MainActivity)host.startActivitySync(new Intent(host.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));host.waitForIdleSync();}
    private void failure(String operation,NfcIo.Phase phase,Exception error,String kind,int reason)throws Exception{
        drain();CreationSession owner=new CreationSession(payload);owner.markCardWritten(new byte[]{4,1});
        main(()->{
            set("session",owner);set("candidate",new NfcCards.Candidate(new byte[]{4,1},new byte[0],512));set("screen","done");set("lockAfterWrite",true);set("lockStatus","unknown");invoke("render");
            Object mode=Enum.valueOf((Class)modeType(),operation);set("mode",mode);set("nfcOperation",mode);set("nfcOwner",owner);set("nfcToken",get("epoch"));set("nfcProtect",true);set("nfcBusy",true);
            NfcIo.Work work=new NfcIo.Work(){public Object run(NfcIo.Job job)throws Exception{job.phase(phase);throw error;}public void discard(Object ignored){}public void close(){}};
            set("nfcJob",NfcIo.start(work,activity,command->((Handler)get("handler")).post(command)));
        });
        until(()->get("nfcJob")==null&&get("sheet")!=null,5000);
        Dialog sheet=(Dialog)get("sheet");String code="NFC-"+(operation.equals("LOCK")?"LOCK":"VERIFY")+"-"+phase.code+"-"+kind;
        String expected=activity.getString(R.string.nfc_lock_diagnostic,code,activity.getString(reason));
        yes(has(sheet.getWindow().getDecorView(),expected),"localized fixed stage and reason visible: "+code);
        yes(!allText(sheet.getWindow().getDecorView()).contains(SECRET),"raw exception text never rendered");
        yes("unknown".equals(get("lockStatus")),"failure never claims permanent read-only");
        yes(owner.cardCount()==1&&!owner.backupSaved()&&!owner.canFinish(),"lock failure preserves confirmed write but never bypasses backup");
        yes(sheet.getWindow().getAttributes().gravity==Gravity.BOTTOM,"error retains app bottom-sheet presentation");
        yes((sheet.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"diagnostic sheet remains secure");
        main(()->invoke("closeSheet"));
    }
    private void drain()throws Exception{Field f=NfcIo.class.getDeclaredField("IO");f.setAccessible(true);ThreadPoolExecutor pool=(ThreadPoolExecutor)f.get(null);until(()->pool.getActiveCount()==0,3000);Thread.sleep(15);}
    private static boolean has(View v,String s){if(v instanceof TextView&&s.contentEquals(((TextView)v).getText()))return true;if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)if(has(g.getChildAt(i),s))return true;}return false;}
    private static String allText(View v){StringBuilder s=new StringBuilder();if(v instanceof TextView)s.append(((TextView)v).getText());if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)s.append(allText(g.getChildAt(i)));}return s.toString();}
    private Class<?> modeType(){try{return Class.forName("io.coingift.android.MainActivity$Mode");}catch(Exception ex){throw new RuntimeException(ex);}}
    private Object get(String n){try{Field f=MainActivity.class.getDeclaredField(n);f.setAccessible(true);return f.get(activity);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void set(String n,Object v){try{Field f=MainActivity.class.getDeclaredField(n);f.setAccessible(true);f.set(activity,v);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void invoke(String n){try{Method m=MainActivity.class.getDeclaredMethod(n);m.setAccessible(true);m.invoke(activity);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void main(Runnable r){Throwable[] failure={null};host.runOnMainSync(()->{try{r.run();}catch(Throwable ex){failure[0]=ex;}});if(failure[0]!=null)throw new AssertionError("Lock diagnostic UI",failure[0]);}
    private void until(java.util.function.BooleanSupplier condition,long timeout)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){boolean[] done={false};main(()->done[0]=condition.getAsBoolean());if(done[0])return;Thread.sleep(20);}throw new AssertionError("Lock diagnostic timeout");}
    private void yes(boolean v,String reason){if(!v)throw new AssertionError(reason);checks++;}
}
