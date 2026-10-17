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

/** Device regression for optional weak-password consent; no real funds or real user passwords. */
final class WeakPasswordProbe {
    private final Instrumentation host;private MainActivity activity;private int checks;
    private EditText first,second,note;
    WeakPasswordProbe(Instrumentation host){this.host=host;}
    int run()throws Exception{
        try{
            for(String locale:new String[]{"zh","en","ja","ko"}){
                launch(locale);setup(null);
                yes(has(((Dialog)get("creationDialog")).getWindow().getDecorView(),new ApprovedCopy(activity).ui("passHint"))&&new ApprovedCopy(activity).ui("passHint").contains("10"),"localized creation hint states minimum ten characters");
                fill("123456789","123456789");generate();yes(get("weakPasswordConfirm")==null&&get("session")==null,"nine characters rejected in each locale");
                fill("1234567890","1234567890");generate();
                Dialog consent=consent();Button previous=button(consent.getWindow().getDecorView(),activity.getString(R.string.weak_password_continue));
                yes(get("session")==null&&!(Boolean)get("busy")&&pendingCount()==0,"warning precedes all wallet generation and worker password ownership");
                yes(has(consent.getWindow().getDecorView(),activity.getString(R.string.weak_password_title)),"localized title");
                yes(has(consent.getWindow().getDecorView(),activity.getString(R.string.weak_password_camera)),"camera does not compensate weak password message");
                yes(!contains(consent.getWindow().getDecorView(),"1234567890"),"dialog never repeats entered password");
                yes((consent.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"warning remains secure");
                yes(findProtected(consent.getWindow().getDecorView())!=null,"warning inherits obscured-touch guard");
                capture(locale+"-weak-password",consent.getWindow().getDecorView());
                main(()->button(consent.getWindow().getDecorView(),activity.getString(R.string.weak_password_edit)).performClick());host.waitForIdleSync();
                yes(get("weakPasswordConfirm")==null&&first.hasFocus()&&first.length()==10,"edit returns to current password without generating");
                main(previous::performClick);yes(get("session")==null&&!(Boolean)get("busy"),"dismissed old continue cannot generate");
                main(()->invoke("hideKeyboard"));generate();yes(consent()!=consent,"every new weak submission asks again");
                main(()->button(consent().getWindow().getDecorView(),activity.getString(R.string.weak_password_continue)).performClick());
                awaitWallet("1234567890",null);yes(get("weakPasswordConfirm")==null,"consent closes on explicit continuation");
            }
            launch("zh");invalidAndStale();enhanced();validPasswordsSkip();legacyShortPassword();obscuredConsent();
            return checks;
        }finally{
            Texts.select(host.getTargetContext(),"zh");
            if(activity!=null)main(()->{invoke("clearWalletSession",new Class<?>[]{boolean.class},true);activity.finish();});
        }
    }
    private void launch(String locale){
        if(activity!=null){main(activity::finish);host.waitForIdleSync();}
        Texts.select(host.getTargetContext(),locale);
        activity=(MainActivity)host.startActivitySync(new Intent(host.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));host.waitForIdleSync();
    }
    private void setup(CameraEntropy entropy){
        main(()->{
            invoke("clearWalletSession",new Class<?>[]{boolean.class},true);set("screen","setup");set("enhanced",entropy!=null);set("cameraEntropy",entropy);invoke("render");
            note=null;for(View view:views(activity.getWindow().getDecorView()))if(view instanceof EditText){note=(EditText)view;break;}
            if(note==null)throw new AssertionError("wallet note missing");note.setText("test-01");invoke("showCreationDialog",new Class<?>[]{EditText.class},note);
            List<EditText> passwords=new ArrayList<>();for(View view:views(((Dialog)get("creationDialog")).getWindow().getDecorView()))if(view instanceof EditText)passwords.add((EditText)view);
            if(passwords.size()!=2)throw new AssertionError("password dialog inputs missing");first=passwords.get(0);second=passwords.get(1);
        });host.waitForIdleSync();
    }
    private void fill(String a,String b){main(()->{first.setText(a);second.setText(b);});}
    private void generate(){main(()->button(((Dialog)get("creationDialog")).getWindow().getDecorView(),new ApprovedCopy(activity).ui("creationConfirmGenerate")).performClick());host.waitForIdleSync();}
    private Dialog consent(){Dialog d=(Dialog)get("weakPasswordConfirm");if(d==null||!d.isShowing())throw new AssertionError("weak consent missing");return d;}
    private void awaitWallet(String password,CameraEntropy entropy)throws Exception{
        until(()->"write".equals(get("screen"))&&get("session")!=null&&pendingCount()==0,30000);
        CreationSession session=(CreationSession)get("session");char[] pass=password.toCharArray();
        try(WalletCrypto.Unlocked restored=new WalletCrypto().decrypt(session.encrypted,pass)){
            yes("test-01".equals(restored.note)&&restored.addresses.sameAs(session.addresses),"real wallet decrypts with the exact approved password and matching addresses");
        }finally{WalletCrypto.wipe(pass);}
        yes(first.length()==0&&second.length()==0&&get("weakPasswordConfirm")==null,"generation clears password inputs and reminder");
        if(entropy!=null)yes(!entropy.ready()&&get("cameraEntropy")==null,"enhanced entropy consumed only after explicit consent");
    }
    private void invalidAndStale()throws Exception{
        setup(null);for(int n=0;n<10;n++){String shortPassword="123456789".substring(0,n);fill(shortPassword,shortPassword);generate();yes(get("weakPasswordConfirm")==null&&get("session")==null&&pendingCount()==0,"0 through 9 characters rejected before consent and generation");}
        fill("1234567890","0987654321");generate();yes(get("weakPasswordConfirm")==null&&get("session")==null,"mismatch fails before consent");
        for(String weak:new String[]{"1234567890","PASSWORD123","abcdefghijklmnop","aB4!aB4!aB4!aB4!"}){
            fill(weak,weak);generate();Dialog d=consent();main(d::cancel);host.waitForIdleSync();yes(get("session")==null&&get("weakPasswordConfirm")==null,"cancel does not generate and does not persist consent");
        }
        fill("1234567890","1234567890");generate();Dialog original=consent();Button old=button(original.getWindow().getDecorView(),activity.getString(R.string.weak_password_continue));
        fill("0987654321","0987654321");main(old::performClick);host.waitForIdleSync();
        yes(get("weakPasswordConfirm")==null&&get("session")==null&&!(Boolean)get("busy"),"changed password requires new consent");
        generate();Dialog changed=consent();main(()->note.setText("test-02"));main(()->button(changed.getWindow().getDecorView(),activity.getString(R.string.weak_password_continue)).performClick());
        yes(get("session")==null&&get("weakPasswordConfirm")==null,"changed note cannot reuse prior confirmation");
        main(()->note.setText("test-01"));generate();Dialog pauseOwner=consent();Button beforePause=button(pauseOwner.getWindow().getDecorView(),activity.getString(R.string.weak_password_continue));
        main(()->{host.callActivityOnPause(activity);host.callActivityOnStop(activity);});main(beforePause::performClick);
        yes(get("weakPasswordConfirm")==null&&first.length()==0&&second.length()==0&&get("session")==null,"background closes consent, clears inputs and rejects stale callback");
        main(()->host.callActivityOnResume(activity));
        setup(null);fill("1234567890","1234567890");generate();Dialog rotateOwner=consent();Button beforeRotate=button(rotateOwner.getWindow().getDecorView(),activity.getString(R.string.weak_password_continue));
        Instrumentation.ActivityMonitor monitor=host.addMonitor(MainActivity.class.getName(),null,false);main(activity::recreate);
        Activity next=host.waitForMonitorWithTimeout(monitor,10000);host.removeMonitor(monitor);yes(next instanceof MainActivity,"activity recreated");activity=(MainActivity)next;host.waitForIdleSync();
        main(beforeRotate::performClick);yes(get("weakPasswordConfirm")==null&&get("session")==null&&!(Boolean)get("busy"),"rotation does not restore consent or let old button generate");
        for(View v:views(activity.getWindow().getDecorView()))if(v instanceof EditText&&(((EditText)v).getInputType()&android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)!=0)yes(((EditText)v).length()==0,"rotation password fields empty");
        setup(null);fill("1234567890","1234567890");generate();Dialog navigationOwner=consent();Button beforeNavigation=button(navigationOwner.getWindow().getDecorView(),activity.getString(R.string.weak_password_continue));
        main(()->invoke("navigate",new Class<?>[]{String.class},"home"));main(beforeNavigation::performClick);
        yes("home".equals(get("screen"))&&get("session")==null,"navigation cancels consent");
    }
    private void enhanced()throws Exception{
        CameraEntropy entropy=new CameraEntropy();java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-256");
        for(int i=0;i<32;i++)entropy.addFrame(hash.digest(("public-warning-frame-"+i).getBytes(StandardCharsets.US_ASCII)));
        entropy.finish(hash.digest("public-warning-photo".getBytes(StandardCharsets.US_ASCII)));setup(entropy);fill("1234567890","1234567890");generate();Dialog d=consent();
        yes(entropy.ready()&&get("cameraEntropy")==entropy&&get("session")==null,"enhanced mode waits for password consent without consuming captured entropy");
        main(d::cancel);yes(entropy.ready(),"cancel allows editing without using up camera entropy");
        generate();main(()->button(consent().getWindow().getDecorView(),activity.getString(R.string.weak_password_continue)).performClick());awaitWallet("1234567890",entropy);
    }
    private void validPasswordsSkip()throws Exception{
        for(String pass:new String[]{"Mx7!pQ2@tL8#","Mx7!pQ2@tL8#vR5$zN"}){
            setup(null);fill(pass,pass);generate();yes(get("weakPasswordConfirm")==null,"non-weak password has no extra consent");awaitWallet(pass,null);
        }
    }
    private void legacyShortPassword()throws Exception{
        byte[] scalar=new byte[32];scalar[31]=1;char[] oldPassword="123456".toCharArray();String encrypted;
        // Test-only fixture construction; package-private encryption is never exposed by production UI.
        Method encrypt=WalletCrypto.class.getDeclaredMethod("encrypt",byte[].class,char[].class,WalletCrypto.Network.class);encrypt.setAccessible(true);
        try{encrypted=(String)encrypt.invoke(new WalletCrypto(),scalar,oldPassword,WalletCrypto.Network.MAINNET);}
        finally{WalletCrypto.wipe(scalar);WalletCrypto.wipe(oldPassword);}
        main(()->{invoke("clearWalletSession",new Class<?>[]{boolean.class},true);invoke("acceptIncoming",new Class<?>[]{String.class},encrypted);});host.waitForIdleSync();
        yes("unlock".equals(get("screen"))&&!has(activity.getWindow().getDecorView(),new ApprovedCopy(activity).ui("passHint")),"recovery does not show new-wallet minimum length hint");
        main(()->{invoke("showUnlockDialog");Dialog d=(Dialog)get("unlockDialog");for(View view:views(d.getWindow().getDecorView()))if(view instanceof EditText){((EditText)view).setText("123456");break;}button(d.getWindow().getDecorView(),activity.getString(R.string.wallet_unlock_action)).performClick();});
        until(()->"revealed".equals(get("screen"))&&get("unlocked")!=null&&pendingCount()==0,30000);
        yes("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4".equals(((WalletCrypto.Unlocked)get("unlocked")).address),"six-character legacy password still decrypts through actual recovery UI");
    }
    private void obscuredConsent(){
        setup(null);fill("1234567890","1234567890");generate();Dialog d=consent();Button action=button(d.getWindow().getDecorView(),activity.getString(R.string.weak_password_continue));ProtectedScrollView root=findProtected(d.getWindow().getDecorView());
        main(()->{Rect rect=new Rect();action.getDrawingRect(rect);action.requestRectangleOnScreen(rect,true);});host.waitForIdleSync();
        main(()->{
            int[] from=new int[2],to=new int[2];root.getLocationOnScreen(from);action.getLocationOnScreen(to);float x=to[0]-from[0]+action.getWidth()/2f,y=to[1]-from[1]+action.getHeight()/2f;
            MotionEvent.PointerProperties pointer=new MotionEvent.PointerProperties();pointer.id=0;pointer.toolType=MotionEvent.TOOL_TYPE_FINGER;
            MotionEvent.PointerCoords coordinates=new MotionEvent.PointerCoords();coordinates.x=x;coordinates.y=y;coordinates.pressure=1;coordinates.size=1;
            long now=SystemClock.uptimeMillis();for(int i=0;i<2;i++){
                MotionEvent e=MotionEvent.obtain(now,now+10*i,i==0?MotionEvent.ACTION_DOWN:MotionEvent.ACTION_UP,1,new MotionEvent.PointerProperties[]{pointer},new MotionEvent.PointerCoords[]{coordinates},0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,i==0?0:MotionEvent.FLAG_WINDOW_IS_OBSCURED);
                try{root.dispatchTouchEvent(e);}finally{e.recycle();}
            }
        });host.waitForIdleSync();
        yes("protection".equals(get("screen"))&&get("weakPasswordConfirm")==null&&get("session")==null&&pendingCount()==0,"obscured weak-password approval never starts generation");
        main(action::performClick);yes(get("session")==null&&!(Boolean)get("busy"),"obscured old consent remains invalid");
    }
    private int pendingCount(){synchronized(activity){return ((Set<?>)get("pendingPasswords")).size();}}
    private void capture(String name,View root){main(()->{Bitmap image=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);try{root.draw(new Canvas(image));File dir=activity.getExternalFilesDir("weak-password-qa");if(dir==null||(!dir.isDirectory()&&!dir.mkdirs()))throw new IOException();try(OutputStream out=new FileOutputStream(new File(dir,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}}catch(IOException e){throw new RuntimeException(e);}finally{image.eraseColor(Color.WHITE);image.recycle();}});}
    private ProtectedScrollView findProtected(View root){for(View v:views(root))if(v instanceof ProtectedScrollView)return (ProtectedScrollView)v;return null;}
    private Button button(View root,String label){for(View v:views(root))if(v instanceof Button&&(label.contentEquals(((Button)v).getText())||label.equals(v.getContentDescription())))return (Button)v;throw new AssertionError("Button missing: "+label);}
    private boolean has(View root,String text){for(View v:views(root))if(v instanceof TextView&&text.contentEquals(((TextView)v).getText()))return true;return false;}
    private boolean contains(View root,String text){for(View v:views(root))if(v instanceof TextView&&((TextView)v).getText().toString().contains(text)||v.getContentDescription()!=null&&v.getContentDescription().toString().contains(text))return true;return false;}
    private List<View> views(View root){List<View> list=new ArrayList<>();list.add(root);if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)list.addAll(views(((ViewGroup)root).getChildAt(i)));return list;}
    private Object get(String name){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception e){throw new RuntimeException(e);}}
    private void set(String name,Object value){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}catch(Exception e){throw new RuntimeException(e);}}
    private Object invoke(String name){return invoke(name,new Class<?>[0]);}
    private Object invoke(String name,Class<?>[] types,Object... args){try{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(activity,args);}catch(Exception e){throw new RuntimeException(e);}}
    private void main(Runnable r){Throwable[] error={null};host.runOnMainSync(()->{try{r.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new AssertionError("UI operation",error[0]);}
    private void until(java.util.function.BooleanSupplier condition,long timeout)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){boolean[] done={false};main(()->done[0]=condition.getAsBoolean());if(done[0]){host.waitForIdleSync();return;}Thread.sleep(50);}throw new AssertionError("UI timeout");}
    private void yes(boolean value,String description){if(!value)throw new AssertionError(description);checks++;}
}
