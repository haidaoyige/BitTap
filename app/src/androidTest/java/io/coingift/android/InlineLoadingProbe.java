package io.coingift.android;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import io.coingift.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Tests original password-dialog ownership while a real crypto request is queued. Public fixtures only. */
final class InlineLoadingProbe {
    private final Instrumentation host;private MainActivity activity;private int checks;private String backup;
    private CountDownLatch gate;
    InlineLoadingProbe(Instrumentation host){this.host=host;}
    int run()throws Exception{
        try(InputStream in=host.getContext().getAssets().open("cg2-scalar-one.txt")){backup=new String(BackupFiles.readBounded(in),StandardCharsets.US_ASCII).trim();}
        try{
            for(String language:new String[]{"zh","en","ja","ko"}){
                launch(language);creationCancel();unlockFailureAndSuccess();unlockCancel();
            }
            launch("zh");backgroundCancel();return checks;
        }finally{
            release();Texts.select(host.getTargetContext(),"zh");
            if(activity!=null)main(()->{invoke("clearWalletSession",new Class<?>[]{boolean.class},true);activity.finish();});
        }
    }
    private void launch(String language){if(activity!=null){main(activity::finish);host.waitForIdleSync();}Texts.select(host.getTargetContext(),language);activity=(MainActivity)host.startActivitySync(new Intent(host.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));host.waitForIdleSync();}
    private void creationCancel()throws Exception{
        main(()->{set("screen","setup");invoke("render");EditText note=fields(activity.getWindow().getDecorView()).get(0);invoke("showCreationDialog",new Class<?>[]{EditText.class},note);});host.waitForIdleSync();
        Dialog owner=(Dialog)get("creationDialog");List<EditText> inputs=fields(owner.getWindow().getDecorView());
        main(()->{for(EditText input:inputs)input.setText("Mx7!pQ2@tL8#vR5$zN");});
        Button submit=button(owner,new ApprovedCopy(activity).ui("creationConfirmGenerate"));block();main(submit::performClick);host.waitForIdleSync();loading(owner,submit,inputs,"creation-loading-");
        char[] pending=pending();int token=(Integer)get("epoch");main(submit::performClick);yes((Integer)get("epoch")==token&&get("session")==null,"repeat submission cannot start another wallet");
        main(()->button(owner,Texts.tr(activity,"取消")).performClick());host.waitForIdleSync();
        yes(get("creationDialog")==null&&get("workDialog")==null&&!(Boolean)get("busy")&&!owner.isShowing(),"cancel closes the original creation dialog and releases busy");
        yes(Arrays.equals(pending,new char[pending.length])&&(Integer)get("epoch")==token+1,"cancel wipes queued password and invalidates result token");
        release();drain();yes(get("session")==null,"late cancelled generation never creates a session");
    }
    private Dialog openUnlock(String password){
        main(()->{invoke("acceptIncoming",new Class<?>[]{String.class},backup);invoke("showUnlockDialog");});host.waitForIdleSync();Dialog owner=(Dialog)get("unlockDialog");main(()->fields(owner.getWindow().getDecorView()).get(0).setText(password));return owner;
    }
    private void unlockFailureAndSuccess()throws Exception{
        Dialog owner=openUnlock("public wrong password");List<EditText> inputs=fields(owner.getWindow().getDecorView());Button submit=button(owner,activity.getString(R.string.wallet_unlock_action));
        block();main(submit::performClick);host.waitForIdleSync();loading(owner,submit,inputs,"unlock-loading-");release();
        until(()->!(Boolean)get("busy")&&get("unlockFeedback")!=null&&((TextView)get("unlockFeedback")).getVisibility()==View.VISIBLE,20000);
        yes(get("unlockDialog")==owner&&owner.isShowing()&&submit.isEnabled(),"wrong password restores the original dialog and button for retry");
        yes(activity.getString(R.string.wallet_unlock_action).equals(submit.getContentDescription())&&inputs.get(0).isEnabled()&&inputs.get(0).length()==0,"retry restores original label, input enablement, and an empty password");
        main(()->{inputs.get(0).setText("test fixture password only");submit.performClick();});
        until(()->"revealed".equals(get("screen"))&&get("unlocked")!=null,20000);
        yes(get("unlockDialog")==null&&get("workDialog")==null&&!owner.isShowing(),"real successful decryption closes the password dialog automatically");
        yes(((WalletCrypto.Unlocked)get("unlocked")).addresses.sameAs(new WalletCrypto().inspect(backup).addresses),"loading flow preserves decrypted address derivation");
        yes(((Map<?,?>)get("workHints")).isEmpty()&&get("workSubmit")==null,"completion releases spinner and input references");
    }
    private void unlockCancel()throws Exception{
        Dialog owner=openUnlock("test fixture password only");Button submit=button(owner,activity.getString(R.string.wallet_unlock_action));block();main(submit::performClick);host.waitForIdleSync();char[] pending=pending();
        View close=views(owner.getWindow().getDecorView()).stream().filter(v->"sheet-close".equals(v.getTag())).findFirst().orElseThrow();
        yes(close.isEnabled(),"close icon remains available during decrypt");main(close::performClick);host.waitForIdleSync();
        yes(get("unlockDialog")==null&&get("workDialog")==null&&!(Boolean)get("busy")&&Arrays.equals(pending,new char[pending.length]),"close cancels decryption and wipes queued password");
        release();drain();yes(get("unlocked")==null&&"unlock".equals(get("screen")),"cancelled decrypt never reveals a late private key");
    }
    private void backgroundCancel()throws Exception{
        Dialog owner=openUnlock("test fixture password only");block();main(()->button(owner,activity.getString(R.string.wallet_unlock_action)).performClick());host.waitForIdleSync();char[] pending=pending();
        main(()->{host.callActivityOnPause(activity);host.callActivityOnStop(activity);});host.waitForIdleSync();
        yes(!owner.isShowing()&&get("unlockDialog")==null&&get("workDialog")==null&&get("workSubmit")==null&&((Map<?,?>)get("workHints")).isEmpty(),"background dismisses password dialog and releases loading references");
        yes(Arrays.equals(pending,new char[pending.length])&&get("unlocked")==null,"background wipes the queued password before completion");
        release();drain();main(()->host.callActivityOnResume(activity));yes(get("unlocked")==null&&"unlock".equals(get("screen")),"returning foreground cannot accept the old unlock result");
    }
    private void loading(Dialog owner,Button submit,List<EditText> inputs,String captureName){
        yes(get("workDialog")==owner&&owner.isShowing()&&!(owner instanceof AlertDialog),"original password dialog owns loading, without a separate processing dialog");
        yes((Boolean)get("busy")&&!submit.isEnabled()&&activity.getString(R.string.crypto_processing).equals(submit.getContentDescription()),"same submit button is disabled with localized loading text");
        yes(submit.getCurrentTextColor()==AppUi.SUB&&submit.getText() instanceof android.text.Spanned&&((android.text.Spanned)submit.getText()).getSpans(0,submit.length(),android.text.style.ReplacementSpan.class).length==1,"grey button embeds a centered spinner icon");
        yes(inputs.stream().allMatch(input->!input.isEnabled()&&input.length()==0&&"••••••••••••".contentEquals(input.getHint())),"real input text is wiped while masked placeholders remain visible");
        yes(button(owner,Texts.tr(activity,"取消")).isEnabled(),"cancel remains usable while busy");
        yes((owner.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"password dialog retains secure window protection");
        capture(captureName+Texts.language(activity),owner.getWindow().getDecorView());
    }
    private ExecutorService worker(){return (ExecutorService)get("worker");}
    private void block()throws Exception{gate=new CountDownLatch(1);CountDownLatch entered=new CountDownLatch(1);CountDownLatch current=gate;worker().execute(()->{entered.countDown();try{current.await();}catch(InterruptedException ex){Thread.currentThread().interrupt();}});yes(entered.await(5,TimeUnit.SECONDS),"worker gate installed before real request");}
    private void release(){if(gate!=null){gate.countDown();gate=null;}}
    private void drain()throws Exception{worker().submit(()->{}).get(20,TimeUnit.SECONDS);host.waitForIdleSync();}
    private char[] pending(){return (char[])((Set<?>)get("pendingPasswords")).iterator().next();}
    private List<EditText> fields(View root){List<EditText> fields=new ArrayList<>();for(View view:views(root))if(view instanceof EditText)fields.add((EditText)view);return fields;}
    private List<View> views(View root){List<View> list=new ArrayList<>();list.add(root);if(root instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)root;for(int i=0;i<group.getChildCount();i++)list.addAll(views(group.getChildAt(i)));}return list;}
    private Button button(Dialog dialog,String label){for(View view:views(dialog.getWindow().getDecorView()))if(view instanceof Button&&(label.contentEquals(((Button)view).getText())||label.equals(view.getContentDescription())))return (Button)view;throw new AssertionError("Button missing: "+label);}
    private Object get(String name){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void set(String name,Object value){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}catch(Exception ex){throw new RuntimeException(ex);}}
    private Object invoke(String name){return invoke(name,new Class<?>[0]);}
    private Object invoke(String name,Class<?>[] types,Object...args){try{Method method=MainActivity.class.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(activity,args);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void main(Runnable action){Throwable[] error={null};host.runOnMainSync(()->{try{action.run();}catch(Throwable ex){error[0]=ex;}});if(error[0]!=null)throw new AssertionError("UI operation",error[0]);}
    private void until(java.util.function.BooleanSupplier condition,long timeout)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){boolean[] done={false};main(()->done[0]=condition.getAsBoolean());if(done[0]){host.waitForIdleSync();return;}Thread.sleep(50);}throw new AssertionError("UI timeout");}
    private void capture(String name,View root){main(()->{Bitmap image=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);try{root.draw(new Canvas(image));File dir=activity.getExternalFilesDir("inline-loading-qa");if(dir==null||(!dir.isDirectory()&&!dir.mkdirs()))throw new IOException();try(OutputStream out=new FileOutputStream(new File(dir,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}}catch(IOException ex){throw new RuntimeException(ex);}finally{image.eraseColor(Color.TRANSPARENT);image.recycle();}});}
    private void yes(boolean value,String description){if(!value)throw new AssertionError(description);checks++;}
}
