package io.coingift.android;

import android.app.*;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import io.coingift.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;

/** Audit-only runner. Observations may demonstrate weaknesses; PASS means reproduced, not secure. */
public final class SecurityReviewRunner extends Instrumentation {
    private MainActivity activity;private int checks;private final StringBuilder observations=new StringBuilder();
    private static final String WIF="KwDiBf89QgGbjEhKnhXJuH7LrciVrZi3qYjgd9M7rFU73sVHnoWn";
    @Override public void onCreate(Bundle args){super.onCreate(args);start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            String backup;try(InputStream in=getContext().getAssets().open("cg2-scalar-one.txt")){backup=new String(BackupFiles.readBounded(in),StandardCharsets.US_ASCII).trim();}
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));waitForIdleSync();
            observe("debuggable",(getTargetContext().getApplicationInfo().flags&ApplicationInfo.FLAG_DEBUGGABLE)!=0);
            Field rng=WalletCrypto.class.getDeclaredField("SYSTEM_RANDOM");rng.setAccessible(true);SecureRandom random=(SecureRandom)rng.get(null);
            observations.append("RNG provider=").append(random.getProvider().getName()).append(", algorithm=").append(random.getAlgorithm()).append('\n');
            yes((activity.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"secure window");
            CreationSession owner=new CreationSession(backup);owner.markCardWritten(new byte[]{1,2,3,4});
            main(()->{set("session",owner);set("screen","done");invoke("render");set("exportData",backup);});
            ContentValues values=new ContentValues();values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,"bitnfc-public-audit-"+System.nanoTime()+".txt");values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"text/plain");
            android.net.Uri uri=getTargetContext().getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);if(uri==null)throw new IOException();
            try{main(()->activity.onActivityResult(22,Activity.RESULT_OK,new Intent().setData(uri)));until(()->owner.backupSaved()&&!(Boolean)get("fileBusy"),15000);}
            finally{getTargetContext().getContentResolver().delete(uri,null,null);}
            yes(get("session")==owner,"backup alone retains creation session");observe("ciphertext retained after verified backup",true);
            Bundle oldState=new Bundle();main(()->activity.onSaveInstanceState(oldState));yes(oldState.isEmpty(),"ciphertext excluded from instance state");observe("ciphertext present in saved instance state",false);
            main(()->invoke("leaveCreation"));yes(get("session")==null,"finish clears active session reference");observe("previously saved Bundle still contains ciphertext",backup.equals(oldState.getString("session")));
            ClipboardManager clipboard=(ClipboardManager)getTargetContext().getSystemService(Context.CLIPBOARD_SERVICE);
            main(()->clipboard.setPrimaryClip(ClipData.newPlainText("BitNFC-audit","public audit sentinel")));
            main(()->{set("incoming",backup);set("screen","unlock");invoke("render");});
            main(()->{for(View view:views(activity.getWindow().getDecorView()))if(view instanceof EditText){((EditText)view).setText("test fixture password only");return;}throw new AssertionError("password missing");});
            click("解密冷钱包");until(()->"revealed".equals(get("screen")),15000);
            WalletCrypto.Unlocked unlocked=(WalletCrypto.Unlocked)get("unlocked");Field rawField=WalletCrypto.Unlocked.class.getDeclaredField("privateKey");rawField.setAccessible(true);byte[] raw=(byte[])rawField.get(unlocked);
            yes(raw!=null&&raw.length==32&&raw[31]==1,"decrypted scalar live before export");observe("plaintext scalar exists before copy/display/QR",true);
            yes(clipboard.getPrimaryClip()!=null&&"public audit sentinel".contentEquals(clipboard.getPrimaryClip().getItemAt(0).getText()),"no automatic private clipboard export");
            yes(!hasWif(activity.getWindow().getDecorView()),"WIF absent from default view text");
            observe("default masked UI contains WIF",false);
            click("显示");yes(hasWif(activity.getWindow().getDecorView()),"WIF display requested");
            boolean semantic=false;long deadline=SystemClock.elapsedRealtime()+3000;
            while(SystemClock.elapsedRealtime()<deadline){AccessibilityNodeInfo node=getUiAutomation().getRootInActiveWindow();if(node!=null&&nodeHasWif(node)){semantic=true;break;}Thread.sleep(80);}
            observe("accessibility/UiAutomation reads displayed WIF while FLAG_SECURE remains set",semantic);
            Bundle unlockedState=new Bundle();main(()->activity.onSaveInstanceState(unlockedState));
            Parcel parcel=Parcel.obtain();byte[] encoded;try{unlockedState.writeToParcel(parcel,0);encoded=parcel.marshall();}finally{parcel.recycle();}
            yes(!contains(encoded,WIF.getBytes(StandardCharsets.UTF_16LE)),"saved state excludes displayed WIF string");
            yes(!contains(encoded,"test fixture password only".getBytes(StandardCharsets.UTF_16LE)),"saved state excludes password string");
            main(()->((Runnable)get("hideSecret")).run());yes(rawField.get(unlocked)!=null,"masking does not lock plaintext key");observe("30-second mask callback retains plaintext scalar",true);
            click("私钥二维码");clickDialog("qrDialog",new ApprovedCopy(activity).ui("copyWif"));clickDialog("copyConfirm",activity.getString(R.string.copy_confirm_action));
            ClipData clip=clipboard.getPrimaryClip();yes(clip!=null&&WIF.contentEquals(clip.getItemAt(0).getText()),"copy exports exact WIF on request");yes(clip.getDescription().getExtras().getBoolean("android.content.extra.IS_SENSITIVE"),"private clipboard preview marked sensitive");
            main(()->{callActivityOnPause(activity);callActivityOnStop(activity);});yes(get("unlocked")==null,"pause drops unlocked holder");yes(Arrays.equals(raw,new byte[32]),"pause zeroes owned scalar buffer");
            main(()->{callActivityOnResume(activity);set("incoming",null);set("screen","home");invoke("render");clipboard.clearPrimaryClip();});
            observations.append("AUDIT REPRODUCTION PASS: ").append(checks).append(" assertions. Observations do not constitute a complete release audit.\n");result.putString("stream",observations.toString());finish(Activity.RESULT_OK,result);
        }catch(Throwable ex){result.putString("stream",observations+"AUDIT FAILED after "+checks+": "+ex+"\n"+android.util.Log.getStackTraceString(ex));finish(Activity.RESULT_CANCELED,result);}
    }
    private void observe(String name,boolean value){observations.append(name).append('=').append(value).append('\n');}
    private Object get(String name){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void set(String name,Object value){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void invoke(String name){try{Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void main(Runnable r){Throwable[] error={null};runOnMainSync(()->{try{r.run();}catch(Throwable ex){error[0]=ex;}});if(error[0]!=null)throw new AssertionError("UI operation",error[0]);}
    private interface Condition{boolean ok();}
    private void until(Condition c,long timeout)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){boolean[] ok={false};main(()->ok[0]=c.ok());if(ok[0]){waitForIdleSync();return;}Thread.sleep(50);}throw new AssertionError("timeout");}
    private List<View> views(View v){List<View> all=new ArrayList<>();all.add(v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)all.addAll(views(g.getChildAt(i)));}return all;}
    private void click(String label){main(()->{for(View v:views(activity.getWindow().getDecorView()))if(v instanceof Button&&(label.contentEquals(((Button)v).getText())||label.equals(v.getContentDescription()))){v.performClick();return;}throw new AssertionError("button missing");});waitForIdleSync();}
    private void clickDialog(String field,String label){main(()->{View root=((android.app.Dialog)get(field)).getWindow().getDecorView();for(View v:views(root))if(v instanceof Button&&(label.contentEquals(((Button)v).getText())||label.equals(v.getContentDescription()))){v.performClick();return;}throw new AssertionError("dialog button missing");});waitForIdleSync();}
    private boolean hasWif(View root){for(View v:views(root))if(v instanceof TextView&&WIF.contentEquals(((TextView)v).getText()))return true;return false;}
    private boolean nodeHasWif(AccessibilityNodeInfo node){if(node.getText()!=null&&WIF.contentEquals(node.getText()))return true;for(int i=0;i<node.getChildCount();i++){AccessibilityNodeInfo c=node.getChild(i);if(c!=null&&nodeHasWif(c))return true;}return false;}
    private boolean contains(byte[] all,byte[] needle){outer:for(int i=0;i<=all.length-needle.length;i++){for(int j=0;j<needle.length;j++)if(all[i+j]!=needle[j])continue outer;return true;}return false;}
    private void yes(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;}
}
