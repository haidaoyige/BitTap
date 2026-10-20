package io.coingift.android;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import io.coingift.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Real session timeout plus accelerated boundary tests and real faulty ContentProvider I/O. */
final class DeadlineFileProbe {
    private final Instrumentation host;private MainActivity activity;private int checks;private String backup;
    DeadlineFileProbe(Instrumentation host){this.host=host;}
    int run(boolean filesOnly)throws Exception{
        try(InputStream in=host.getContext().getAssets().open("cg2-scalar-one.txt")){backup=new String(BackupFiles.readBounded(in),StandardCharsets.US_ASCII).trim();}
        try{
            if(!filesOnly){launch("zh");realIdleLock();
            for(String language:new String[]{"zh","en","ja","ko"}){launch(language);absoluteAndStale();}}
            launch("zh");fileOperations();return checks;
        }finally{
            control("release");Texts.select(host.getTargetContext(),"zh");
            if(activity!=null)main(()->{invoke("clearWalletSession",new Class<?>[]{boolean.class},true);activity.finish();});
        }
    }
    private void launch(String language){if(activity!=null){main(activity::finish);host.waitForIdleSync();}Texts.select(host.getTargetContext(),language);activity=(MainActivity)host.startActivitySync(new Intent(host.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));host.waitForIdleSync();}
    private void unlock()throws Exception{
        main(()->{invoke("acceptIncoming",new Class<?>[]{String.class},backup);invoke("showUnlockDialog");Dialog d=(Dialog)get("unlockDialog");for(View v:views(d.getWindow().getDecorView()))if(v instanceof EditText)((EditText)v).setText("test fixture password only");button(d.getWindow().getDecorView(),activity.getString(R.string.wallet_unlock_action)).performClick();});
        until(()->get("unlocked")!=null&&"revealed".equals(get("screen")),20000);
        yes(((UnlockDeadline)get("unlockDeadline")).active(),"real decryption starts a deadline");
    }
    private void realIdleLock()throws Exception{
        unlock();PrivateSession holder=(PrivateSession)get("unlocked");byte[] bytes=owned(holder);
        main(()->invoke("showPrivateQr"));until(()->get("qrBitmap")!=null,10000);Bitmap bitmap=(Bitmap)get("qrBitmap");PrivateKeyView secret=(PrivateKeyView)get("secretView");
        if(Build.VERSION.SDK_INT>=29){boolean[] copied={false};main(()->copied[0]=(Boolean)invoke("copyPrivateKey"));yes(copied[0],"public fixture private copy succeeds before idle test");}long start=SystemClock.elapsedRealtime();
        until(()->get("unlocked")==null&&get("autoLockDialog")!=null,65_000);
        long waited=SystemClock.elapsedRealtime()-start;yes(waited>=59_000&&waited<65_000,"actual 60-second idle session expires without any tap or accelerated clock");
        yes("unlock".equals(get("screen"))&&get("qrDialog")==null&&get("qrBitmap")==null&&bitmap.isRecycled(),"timeout dismisses private sheet and destroys QR pixels");
        yes(Arrays.equals(bytes,new byte[bytes.length])&&owned(holder)==null,"timeout wipes the owned scalar buffer and releases it");
        try{holder.wif();throw new AssertionError("expired key accessible");}catch(IllegalStateException expected){checks++;}
        yes(secret.isCleared(),"detached private drawing is cleared");
        if(Build.VERSION.SDK_INT>=29){ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);yes(clipboard.getPrimaryClip()==null,"idle expiry clears owned private clipboard while foreground");}
        Dialog notice=(Dialog)get("autoLockDialog");yes(has(notice.getWindow().getDecorView(),activity.getString(R.string.auto_lock_idle_body)),"idle cause is explained");sheetStyle(notice);yes(has(notice.getWindow().getDecorView(),activity.getString(R.string.auto_lock_cleared)),"lock notice explains session cleanup separately");capture("idle-locked",notice.getWindow().getDecorView());
        Thread.sleep(5200);yes(notice.isShowing(),"lock notice does not auto-dismiss after five seconds");
        main(()->button(notice.getWindow().getDecorView(),activity.getString(R.string.auto_lock_dismiss)).performClick());host.waitForIdleSync();yes(get("autoLockDialog")==null&&get("unlocked")==null,"confirm dismisses notice without unlocking");
    }
    private void sheetStyle(Dialog dialog){
        yes(!(dialog instanceof AlertDialog)&&dialog.getWindow().getAttributes().gravity==Gravity.BOTTOM,"status uses the app bottom sheet instead of a native centered alert");
        yes((dialog.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"status sheet preserves secure window protection");
    }
    private void absoluteAndStale()throws Exception{
        unlock();UnlockDeadline deadline=(UnlockDeadline)get("unlockDeadline");
        main(()->{long now=SystemClock.elapsedRealtime();field(deadline,"started",now-119_000);field(deadline,"last",now);invoke("showPrivateQr");invoke("tap");});
        until(()->get("unlocked")==null&&get("autoLockDialog")!=null,4000);
        yes(has(((Dialog)get("autoLockDialog")).getWindow().getDecorView(),activity.getString(R.string.auto_lock_max_body)),"maximum deadline is localized and not extended by opening private sheet");
        Dialog maxNotice=(Dialog)get("autoLockDialog");sheetStyle(maxNotice);capture("maximum-locked-"+Texts.language(activity),maxNotice.getWindow().getDecorView());
        main(()->button(maxNotice.getWindow().getDecorView(),activity.getString(R.string.auto_lock_dismiss)).performClick());
        unlock();main(()->invoke("showPrivateQr"));until(()->get("qrBitmap")!=null,10000);main(()->invoke("requestPrivateCopy"));
        Dialog copy=(Dialog)get("copyConfirm");Button stale=copy==null?null:button(copy.getWindow().getDecorView(),activity.getString(R.string.copy_confirm_action));
        main(()->{
            UnlockDeadline d=(UnlockDeadline)get("unlockDeadline");long now=SystemClock.elapsedRealtime();field(d,"started",now-121_000);field(d,"last",now);
            ((Handler)get("handler")).removeCallbacks((Runnable)get("checkUnlockDeadline"));
            invoke("copyPrivateKey");
        });
        yes(get("unlocked")==null&&get("copyConfirm")==null&&get("autoLockDialog")!=null,"export checks deadline synchronously even if timer callback has not run");
        if(stale!=null){main(stale::performClick);yes(get("unlocked")==null&&get("copyConfirm")==null,"stale copy consent cannot restore or export expired key");}
        main(()->{Dialog notice=(Dialog)get("autoLockDialog");button(notice.getWindow().getDecorView(),activity.getString(R.string.auto_lock_dismiss)).performClick();});
        unlock();main(()->{host.callActivityOnPause(activity);host.callActivityOnStop(activity);});
        yes(get("unlocked")==null&&!((UnlockDeadline)get("unlockDeadline")).active(),"background clears key and deadline");main(()->host.callActivityOnResume(activity));
    }
    private void fileOperations()throws Exception{
        control("release");startFile("good",DocumentIo.Kind.IMPORT,null,null,null);
        until(()->!(Boolean)get("fileBusy")&&"unlock".equals(get("screen")),4000);yes(backup.equals(get("incoming")),"normal descriptor import succeeds");
        control("reset");startFile("block-read",DocumentIo.Kind.IMPORT,null,null,null);long start=SystemClock.elapsedRealtime();
        until(()->!(Boolean)get("fileBusy")&&get("fileErrorDialog")!=null,18_000);
        yes(SystemClock.elapsedRealtime()-start>=14_500,"actual 15-second provider read timeout releases the UI");dismissError();control("release");
        // An uncooperative provider open ignores interruption. Cancellation must still release the UI.
        control("reset");startFile("slow-open",DocumentIo.Kind.IMPORT,null,null,null);DocumentIo.Job abandoned=(DocumentIo.Job)get("fileJob");
        main(()->activity.onBackPressed());yes(!(Boolean)get("fileBusy")&&get("fileJob")==null,"back cancels blocked provider immediately");dismissError();
        control("release");Thread.sleep(500);String late;
        try(InputStream in=host.getContext().getAssets().open("cg3-scalar-one.txt")){late=new String(BackupFiles.readBounded(in),StandardCharsets.US_ASCII).trim();}
        final String lateValid=late;main(()->activity.documentFinished(abandoned,true,lateValid));yes(backup.equals(get("incoming")),"late valid backup completion cannot replace current wallet");
        CreationSession owner=new CreationSession(backup);main(()->{set("session",owner);set("screen","write");invoke("render");});
        startFile("fail",DocumentIo.Kind.BACKUP,backup,null,owner);until(()->!(Boolean)get("fileBusy")&&get("fileErrorDialog")!=null,4000);
        yes(!owner.backupSaved(),"failed write never marks backup saved");AlertDialog error=(AlertDialog)get("fileErrorDialog");Thread.sleep(5200);yes(error.isShowing(),"save failure stays visible until confirmed");capture("save-failed",error.getWindow().getDecorView());dismissError();
        startFile("mismatch",DocumentIo.Kind.BACKUP,backup,null,owner);until(()->!(Boolean)get("fileBusy")&&get("fileErrorDialog")!=null,4000);yes(!owner.backupSaved(),"read-back mismatch cannot mark backup saved");dismissError();
        control("reset");startFile("slow-open",DocumentIo.Kind.BACKUP,backup,null,owner);main(()->invoke("cancelDocumentOperation",new Class<?>[]{boolean.class},false));yes(!owner.backupSaved(),"cancelled write does not mark backup saved");dismissError();control("release");Thread.sleep(500);yes(!owner.backupSaved(),"late writer completion cannot mark backup saved");
        startFile("roundtrip",DocumentIo.Kind.BACKUP,backup,null,owner);until(()->!(Boolean)get("fileBusy")&&owner.backupSaved(),4000);yes(owner.backupSaved(),"completed close and exact read-back marks backup saved");
        byte[] privatePng="public disposable PNG test bytes".getBytes(StandardCharsets.US_ASCII);control("reset");startFile("slow-open",DocumentIo.Kind.PNG,null,privatePng,null);
        main(()->invoke("cancelDocumentOperation",new Class<?>[]{boolean.class},false));yes(Arrays.equals(privatePng,new byte[privatePng.length]),"cancel immediately wipes pending private image payload");dismissError();control("release");
        byte[] expiredPng="public expired PNG bytes".getBytes(StandardCharsets.US_ASCII);
        main(()->invoke("startDocumentOperation",new Class<?>[]{Uri.class,DocumentIo.Kind.class,String.class,byte[].class,CreationSession.class,String.class,boolean.class,long.class},Uri.parse("content://io.coingift.android.test.documents/roundtrip"),DocumentIo.Kind.PNG,null,expiredPng,null,null,true,SystemClock.elapsedRealtime()-1));
        until(()->!(Boolean)get("fileBusy")&&get("fileErrorDialog")!=null,4000);yes(Arrays.equals(expiredPng,new byte[expiredPng.length]),"private PNG cannot outlive the original unlock deadline");dismissError();
        startFile("good",DocumentIo.Kind.IMPORT,null,null,null);until(()->!(Boolean)get("fileBusy"),4000);yes(get("fileErrorDialog")==null,"new valid operation works after cancellation and timeout");
    }
    private void startFile(String path,DocumentIo.Kind kind,String encrypted,byte[] png,CreationSession owner){main(()->invoke("startDocumentOperation",new Class<?>[]{Uri.class,DocumentIo.Kind.class,String.class,byte[].class,CreationSession.class,String.class,boolean.class},Uri.parse("content://io.coingift.android.test.documents/"+path),kind,encrypted,png,owner,null,kind==DocumentIo.Kind.PNG));}
    private void dismissError(){main(()->{AlertDialog d=(AlertDialog)get("fileErrorDialog");if(d!=null)d.getButton(AlertDialog.BUTTON_POSITIVE).performClick();});host.waitForIdleSync();}
    private void control(String method){host.getTargetContext().getContentResolver().call(Uri.parse("content://io.coingift.android.test.documents"),method,null,null);}
    private byte[] owned(PrivateSession holder){try{Field f=PrivateSession.class.getDeclaredField("ciphertext");f.setAccessible(true);return (byte[])f.get(holder);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void field(Object object,String name,long value){try{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);f.setLong(object,value);}catch(Exception ex){throw new RuntimeException(ex);}}
    private Object get(String name){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void set(String name,Object value){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}catch(Exception ex){throw new RuntimeException(ex);}}
    private Object invoke(String name){return invoke(name,new Class<?>[0]);}
    private Object invoke(String name,Class<?>[] types,Object... values){try{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(activity,values);}catch(Exception ex){throw new RuntimeException(ex);}}
    private List<View> views(View root){List<View> list=new ArrayList<>();list.add(root);if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)list.addAll(views(((ViewGroup)root).getChildAt(i)));return list;}
    private Button button(View root,String label){for(View v:views(root))if(v instanceof Button&&(label.contentEquals(((Button)v).getText())||label.equals(v.getContentDescription())))return (Button)v;throw new AssertionError("Button missing: "+label);}
    private boolean has(View root,String text){for(View v:views(root))if(v instanceof TextView&&text.contentEquals(((TextView)v).getText()))return true;return false;}
    private void capture(String name,View root){main(()->{Bitmap image=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);try{root.draw(new Canvas(image));File dir=activity.getExternalFilesDir("deadline-file-qa");if(dir==null||(!dir.isDirectory()&&!dir.mkdirs()))throw new IOException();try(OutputStream out=new FileOutputStream(new File(dir,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}}catch(IOException ex){throw new RuntimeException(ex);}finally{image.eraseColor(Color.TRANSPARENT);image.recycle();}});}
    private void main(Runnable action){Throwable[] error={null};host.runOnMainSync(()->{try{action.run();}catch(Throwable ex){error[0]=ex;}});if(error[0]!=null)throw new AssertionError("UI operation",error[0]);}
    private void until(java.util.function.BooleanSupplier condition,long timeout)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){boolean[] done={false};main(()->done[0]=condition.getAsBoolean());if(done[0]){host.waitForIdleSync();return;}Thread.sleep(50);}throw new AssertionError("UI timeout");}
    private void yes(boolean value,String description){if(!value)throw new AssertionError(description);checks++;}
}
