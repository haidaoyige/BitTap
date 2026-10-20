package io.coingift.android;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import io.coingift.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** On-device regression with public scalar-one fixtures; never use these addresses for funds. */
final class SensitiveActionsProbe {
    private final Instrumentation host;
    private MainActivity activity;
    private int checks;
    private String backup;
    private static final String WIF="KwDiBf89QgGbjEhKnhXJuH7LrciVrZi3qYjgd9M7rFU73sVHnoWn";
    SensitiveActionsProbe(Instrumentation host){this.host=host;}
    int run() throws Exception {
        try(InputStream in=host.getContext().getAssets().open("cg2-scalar-one.txt")){backup=new String(BackupFiles.readBounded(in),StandardCharsets.US_ASCII).trim();}
        try {
            launch("zh");gestureRegression();
            for(String locale:new String[]{"zh","en","ja","ko"}){launch(locale);privateExportRegression(locale);}
            launch("zh");imageConsentRegression();publicAddressRegression();cardConsentRegression();cameraReviewRegression();
            return checks;
        } finally {
            Texts.select(host.getTargetContext(),"zh");
            if(activity!=null)main(()->{invoke("clearWalletSession",new Class<?>[]{boolean.class},true);activity.finish();});
        }
    }
    private void launch(String language){
        if(activity!=null){main(activity::finish);host.waitForIdleSync();}
        Texts.select(host.getTargetContext(),language);
        activity=(MainActivity)host.startActivitySync(new Intent(host.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));host.waitForIdleSync();
        yes((activity.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"FLAG_SECURE retained");
        if(Build.VERSION.SDK_INT>=31){
            yes(activity.checkSelfPermission("android.permission.HIDE_OVERLAY_WINDOWS")==android.content.pm.PackageManager.PERMISSION_GRANTED,"hide-overlay permission granted");
            yes(activity.getWindow().getAttributes().toString().contains("HIDE_NON_SYSTEM_OVERLAY_WINDOWS"),"activity requests hiding overlays");
        }
    }
    private void gestureRegression()throws Exception{
        final AppUi[] testUi={null};final Button[] button={null};final int[] clicks={0},rejects={0};
        main(()->{testUi[0]=new AppUi(activity,()->false,()->{});testUi[0].obscuredTouch=()->rejects[0]++;testUi[0].install();button[0]=testUi[0].action(testUi[0].page,"probe",true,0,()->clicks[0]++);});host.waitForIdleSync();
        ProtectedScrollView root=(ProtectedScrollView)testUi[0].scroll;
        int full=MotionEvent.FLAG_WINDOW_IS_OBSCURED;
        int partial=Build.VERSION.SDK_INT>=29?MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED:full;
        gesture(root,button[0],new int[]{0,0},new int[]{0,1});yes(clicks[0]==1&&rejects[0]==0,"clean gesture works");
        int[][] flags={{full,0},{partial,0},{0,full},{0,partial},{0,full,0},{0,partial,0}};
        for(int[] f:flags){int before=rejects[0];gesture(root,button[0],f,f.length==2?new int[]{0,1}:new int[]{0,2,1});yes(clicks[0]==1,"obscured gesture never clicks");yes(rejects[0]==before+1,"single rejection notification per gesture");}
        gesture(root,button[0],new int[]{0,0},new int[]{0,1});yes(clicks[0]==2,"clean retry works after cancelled gesture");
        main(()->invoke("render"));host.waitForIdleSync();
    }
    private void unlock()throws Exception{
        main(()->{set("incoming",backup);set("screen","unlock");invoke("render");invoke("showUnlockDialog");for(View v:views(((Dialog)get("unlockDialog")).getWindow().getDecorView()))if(v instanceof EditText)((EditText)v).setText("test fixture password only");});
        main(()->button(((Dialog)get("unlockDialog")).getWindow().getDecorView(),activity.getString(R.string.wallet_unlock_action)).performClick());
        until(()->"revealed".equals(get("screen")),20000);
    }
    private void privateExportRegression(String locale)throws Exception{
        unlock();PrivateSession holder=(PrivateSession)get("unlocked");Field field=PrivateSession.class.getDeclaredField("ciphertext");field.setAccessible(true);byte[] owned=(byte[])field.get(holder);
        main(()->button(activity.getWindow().getDecorView(),activity.getString(R.string.wallet_view_private)).performClick());host.waitForIdleSync();until(()->get("qrDialog")!=null,10000);
        Dialog qr=(Dialog)get("qrDialog");yes(findProtected(qr.getWindow().getDecorView())!=null,"private QR uses protected touch root");windowProtected(qr);
        yes(WIF.equals(decode((Bitmap)get("qrBitmap"))),"private QR remains exact WIF");
        final ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
        main(()->clipboard.setPrimaryClip(ClipData.newPlainText("security-probe","public sentinel")));
        Button export;
        if(Build.VERSION.SDK_INT<=28){
            Button disabled=button(qr.getWindow().getDecorView(),activity.getString(R.string.private_copy_unavailable));yes(!disabled.isEnabled(),"Android 9 copy visibly disabled");
            yes(has(qr.getWindow().getDecorView(),activity.getString(R.string.private_copy_legacy_notice)),"Android 9 warning localized");
            main(()->{invoke("requestPrivateCopy");disabled.performClick();});yes(get("copyConfirm")==null,"disabled and direct request cannot open copy consent");
            final boolean[] allowed={true};main(()->allowed[0]=(Boolean)invoke("copyPrivateKey"));yes(!allowed[0]&&sentinel(clipboard),"direct copy gate leaves clipboard unchanged");
            capture(locale+"-android9-private-qr",qr.getWindow().getDecorView());
            main(()->button(qr.getWindow().getDecorView(),Texts.tr(activity,"保存图片")).performClick());host.waitForIdleSync();
            export=button(((Dialog)get("qrSaveConfirm")).getWindow().getDecorView(),Texts.tr(activity,"确认保存"));
        }else{
            main(()->button(qr.getWindow().getDecorView(),new ApprovedCopy(activity).ui("copyWif")).performClick());host.waitForIdleSync();
            Dialog confirm=(Dialog)get("copyConfirm");windowProtected(confirm);
            yes(has(confirm.getWindow().getDecorView(),activity.getString(R.string.copy_confirm_description)),"existing copy wording preserved");
            export=button(confirm.getWindow().getDecorView(),activity.getString(R.string.copy_confirm_action));
        }
        Dialog owner=(Dialog)get(Build.VERSION.SDK_INT<=28?"qrSaveConfirm":"copyConfirm");
        Bitmap oldQr=(Bitmap)get("qrBitmap");
        gesture(findProtected(owner.getWindow().getDecorView()),export,new int[]{0,Build.VERSION.SDK_INT>=29?MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED:MotionEvent.FLAG_WINDOW_IS_OBSCURED},new int[]{0,1});
        yes("protection".equals(get("screen")),"obscured export opens protection screen");
        yes(sentinel(clipboard),"obscured export never copies private key");
        yes(get("pendingQrImage")==null,"obscured export creates no PNG export request");
        yes(get("unlocked")==null&&Arrays.equals(owned,new byte[owned.length])&&field.get(holder)==null,"protection clears owned private key buffer");
        yes(get("copyConfirm")==null&&get("qrSaveConfirm")==null&&get("qrDialog")==null&&oldQr.isRecycled(),"secret dialogs and QR cleared");
        yes(has(activity.getWindow().getDecorView(),activity.getString(R.string.obscured_title)),"protection title localized");
        capture(locale+"-protection",activity.getWindow().getDecorView());
        main(export::performClick);yes(sentinel(clipboard)&&get("pendingQrImage")==null,"stale private confirmation cannot export");
        if(locale.equals("zh")){
            Instrumentation.ActivityMonitor monitor=host.addMonitor(MainActivity.class.getName(),null,false);
            main(activity::recreate);Activity replacement=host.waitForMonitorWithTimeout(monitor,10000);host.removeMonitor(monitor);
            yes(replacement instanceof MainActivity,"protection screen recreates");activity=(MainActivity)replacement;host.waitForIdleSync();
            yes("protection".equals(get("screen"))&&get("unlocked")==null,"rotation does not restore private key");
        }
        main(()->button(activity.getWindow().getDecorView(),activity.getString(R.string.obscured_return)).performClick());host.waitForIdleSync();
        yes("unlock".equals(get("screen"))&&get("unlocked")==null,"return to password page without restoring private key");
        for(View v:views(activity.getWindow().getDecorView()))if(v instanceof EditText)yes(((EditText)v).length()==0,"return password field empty");
        if(Build.VERSION.SDK_INT>=29){
            unlock();main(()->button(activity.getWindow().getDecorView(),activity.getString(R.string.wallet_view_private)).performClick());host.waitForIdleSync();until(()->get("qrDialog")!=null,10000);
            main(()->button(((Dialog)get("qrDialog")).getWindow().getDecorView(),new ApprovedCopy(activity).ui("copyWif")).performClick());host.waitForIdleSync();
            Dialog confirm=(Dialog)get("copyConfirm");
            gesture(findProtected(confirm.getWindow().getDecorView()),button(confirm.getWindow().getDecorView(),activity.getString(R.string.copy_confirm_action)),new int[]{0,0},new int[]{0,1});
            ClipData copied=clipboard.getPrimaryClip();yes(copied!=null&&WIF.contentEquals(copied.getItemAt(0).getText()),"supported Android still copies exact WIF after consent");
            yes(copied.getDescription().getExtras().getBoolean("android.content.extra.IS_SENSITIVE"),"private clipboard sensitive flag retained");
        }
    }
    private void imageConsentRegression()throws Exception{
        unlock();main(()->button(activity.getWindow().getDecorView(),activity.getString(R.string.wallet_view_private)).performClick());host.waitForIdleSync();until(()->get("qrDialog")!=null,10000);
        main(()->button(((Dialog)get("qrDialog")).getWindow().getDecorView(),Texts.tr(activity,"保存图片")).performClick());host.waitForIdleSync();
        Dialog confirm=(Dialog)get("qrSaveConfirm");windowProtected(confirm);
        Instrumentation.ActivityMonitor monitor=host.addMonitor(new IntentFilter(Intent.ACTION_CREATE_DOCUMENT),new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null),true);
        try{
            gesture(findProtected(confirm.getWindow().getDecorView()),button(confirm.getWindow().getDecorView(),Texts.tr(activity,"确认保存")),new int[]{0,MotionEvent.FLAG_WINDOW_IS_OBSCURED},new int[]{0,1});
            yes(monitor.getHits()==0,"obscured PNG consent does not launch a document provider");
            yes("protection".equals(get("screen"))&&get("pendingQrImage")==null&&get("unlocked")==null,"obscured PNG consent leaves no key or export");
        }finally{host.removeMonitor(monitor);}
    }
    private void publicAddressRegression()throws Exception{
        main(()->{set("incoming",backup);set("screen","unlock");invoke("render");button(activity.getWindow().getDecorView(),Texts.tr(activity,"展示地址")).performClick();});host.waitForIdleSync();
        Dialog d=(Dialog)get("qrDialog");Button copy=button(d.getWindow().getDecorView(),Texts.tr(activity,"复制地址"));
        yes(copy.isEnabled(),"public address copy available on all supported Android versions");
        String address=new WalletCrypto().inspect(backup).address;yes(address.equals(decode((Bitmap)get("qrBitmap"))),"public QR contains exact address");
        gesture(findProtected(d.getWindow().getDecorView()),copy,new int[]{0,0},new int[]{0,1});
        ClipboardManager cb=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);ClipData clip=cb.getPrimaryClip();yes(clip!=null&&address.contentEquals(clip.getItemAt(0).getText()),"normal public copy succeeds");
        yes(button(d.getWindow().getDecorView(),Texts.tr(activity,"保存图片")).isEnabled(),"public PNG save remains enabled");
        main(()->((Dialog)get("qrDialog")).dismiss());host.waitForIdleSync();
    }
    private void cardConsentRegression()throws Exception{
        CreationSession session=new CreationSession(backup);NfcCards.Candidate candidate=new NfcCards.Candidate(new byte[]{1,2,3},new byte[]{4,5},512);
        main(()->{set("session",session);set("screen","write");invoke("render");set("candidate",candidate);invoke("confirmCard",new Class<?>[]{NfcCards.Candidate.class},candidate);});host.waitForIdleSync();
        Dialog overwrite=(Dialog)get("sheet");gesture(findProtected(overwrite.getWindow().getDecorView()),button(overwrite.getWindow().getDecorView(),"确认覆盖并写入"),new int[]{MotionEvent.FLAG_WINDOW_IS_OBSCURED,0},new int[]{0,1});
        yes("protection".equals(get("screen"))&&"NONE".equals(get("mode").toString())&&session.cardCount()==0,"obscured overwrite never arms NFC write");
        main(()->invoke("returnFromProtection"));host.waitForIdleSync();
        yes("write".equals(get("screen"))&&get("session")==session,"return preserves encrypted creation flow");
        main(()->invoke("confirmReadOnly"));host.waitForIdleSync();
        Dialog lock=(Dialog)get("sheet");String label=new ApprovedCopy(activity).section("lockCopy").optString("confirm");
        gesture(findProtected(lock.getWindow().getDecorView()),button(lock.getWindow().getDecorView(),label),new int[]{0,Build.VERSION.SDK_INT>=29?MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED:MotionEvent.FLAG_WINDOW_IS_OBSCURED,0},new int[]{0,2,1});
        yes("protection".equals(get("screen"))&&"NONE".equals(get("mode").toString()),"obscured permanent-lock consent never arms NFC");
    }
    private void cameraReviewRegression()throws Exception{
        CameraEntropy entropy=new CameraEntropy();java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-256");
        for(int i=0;i<32;i++)entropy.addFrame(hash.digest(("public-probe-frame-"+i).getBytes(StandardCharsets.US_ASCII)));
        entropy.finish(hash.digest("public-probe-photo".getBytes(StandardCharsets.US_ASCII)));
        Bitmap photo=Bitmap.createBitmap(32,32,Bitmap.Config.ARGB_8888);
        main(()->{set("cameraEntropy",entropy);set("cameraPhoto",photo);set("enhanced",true);set("screen","review");invoke("render");});host.waitForIdleSync();
        gesture(findProtected(activity.getWindow().getDecorView()),button(activity.getWindow().getDecorView(),new ApprovedCopy(activity).ui("useCapture")),new int[]{MotionEvent.FLAG_WINDOW_IS_OBSCURED,0},new int[]{0,1});
        yes("protection".equals(get("screen"))&&photo.isRecycled()&&!entropy.ready(),"obscured camera consent clears capture and photo");
        yes(get("camera")==null&&get("cameraEntropy")==null&&get("cameraPhoto")==null,"no camera sampling retained behind protection page");
        main(()->invoke("returnFromProtection"));host.waitForIdleSync();yes("setup".equals(get("screen")),"interrupted camera returns to settings for fresh capture");
    }
    private void gesture(ProtectedScrollView root,Button target,int[] flags,int[] actions){
        if(root==null)throw new AssertionError("protected root missing");
        main(()->{
            Rect visible=new Rect();target.getDrawingRect(visible);target.requestRectangleOnScreen(visible,true);
        });host.waitForIdleSync();
        main(()->{
            int[] base=new int[2],position=new int[2];root.getLocationOnScreen(base);target.getLocationOnScreen(position);
            float x=position[0]-base[0]+target.getWidth()/2f,y=position[1]-base[1]+target.getHeight()/2f;
            yes(target.getWidth()>0&&target.getHeight()>0,"touch target laid out");
            long down=SystemClock.uptimeMillis();
            MotionEvent.PointerProperties pointer=new MotionEvent.PointerProperties();pointer.id=0;pointer.toolType=MotionEvent.TOOL_TYPE_FINGER;
            MotionEvent.PointerCoords coords=new MotionEvent.PointerCoords();coords.x=x;coords.y=y;coords.pressure=1;coords.size=1;
            for(int i=0;i<actions.length;i++){
                MotionEvent event=MotionEvent.obtain(down,down+i*12,actions[i],1,new MotionEvent.PointerProperties[]{pointer},new MotionEvent.PointerCoords[]{coords},0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,flags[i]);
                try{root.dispatchTouchEvent(event);}finally{event.recycle();}
            }
        });host.waitForIdleSync();
    }
    private void windowProtected(Dialog d){yes((d.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"dialog secure");if(Build.VERSION.SDK_INT>=31)yes(d.getWindow().getAttributes().toString().contains("HIDE_NON_SYSTEM_OVERLAY_WINDOWS"),"dialog hides non-system overlays");}
    private ProtectedScrollView findProtected(View root){for(View v:views(root))if(v instanceof ProtectedScrollView)return (ProtectedScrollView)v;return null;}
    private boolean sentinel(ClipboardManager clipboard){ClipData c=clipboard.getPrimaryClip();return c!=null&&"public sentinel".contentEquals(c.getItemAt(0).getText());}
    private String decode(Bitmap bitmap)throws Exception{int[] pixels=new int[bitmap.getWidth()*bitmap.getHeight()];bitmap.getPixels(pixels,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());try{return new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(bitmap.getWidth(),bitmap.getHeight(),pixels)))).getText();}finally{Arrays.fill(pixels,0);}}
    private void capture(String name,View root){main(()->{Bitmap image=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);try{root.draw(new Canvas(image));File dir=activity.getExternalFilesDir("sensitive-actions-qa");if(dir==null||(!dir.isDirectory()&&!dir.mkdirs()))throw new IOException();try(OutputStream out=new FileOutputStream(new File(dir,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}}catch(IOException ex){throw new RuntimeException(ex);}finally{image.eraseColor(Color.WHITE);image.recycle();}});}
    private Button button(View root,String label){for(View v:views(root))if(v instanceof Button&&(label.contentEquals(((Button)v).getText())||label.equals(v.getContentDescription())))return (Button)v;throw new AssertionError("button missing: "+label);}
    private boolean has(View root,String value){for(View v:views(root))if(v instanceof TextView&&value.contentEquals(((TextView)v).getText()))return true;return false;}
    private List<View> views(View root){List<View> out=new ArrayList<>();out.add(root);if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)out.addAll(views(((ViewGroup)root).getChildAt(i)));return out;}
    private Object get(String name){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void set(String name,Object v){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,v);}catch(Exception ex){throw new RuntimeException(ex);}}
    private Object invoke(String name){return invoke(name,new Class<?>[0]);}
    private Object invoke(String name,Class<?>[] types,Object... args){try{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(activity,args);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void main(Runnable run){Throwable[] error={null};host.runOnMainSync(()->{try{run.run();}catch(Throwable ex){error[0]=ex;}});if(error[0]!=null)throw new AssertionError("UI regression",error[0]);}
    private void until(java.util.function.BooleanSupplier c,long timeout)throws Exception{long until=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<until){boolean[] yes={false};main(()->yes[0]=c.getAsBoolean());if(yes[0]){host.waitForIdleSync();return;}Thread.sleep(50);}throw new AssertionError("UI timeout");}
    private void yes(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
}
