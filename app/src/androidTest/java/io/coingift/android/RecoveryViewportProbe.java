package io.coingift.android;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import io.coingift.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Real viewport checks with maximum-length CJK notes and public scalar-one data only. */
final class RecoveryViewportProbe {
    private final Instrumentation host;
    private MainActivity activity;
    private int checks;
    private VerificationStore store;
    private static final String PASSWORD="test fixture password only";
    RecoveryViewportProbe(Instrumentation host){this.host=host;}
    int run()throws Exception{
        byte[] scalar=new byte[32];scalar[31]=1;char[] password=PASSWORD.toCharArray();String payload;
        try{
            Method encrypt=WalletCrypto.class.getDeclaredMethod("encrypt",byte[].class,char[].class,WalletCrypto.Network.class,String.class,boolean.class);encrypt.setAccessible(true);
            payload=(String)encrypt.invoke(new WalletCrypto(),scalar,password,WalletCrypto.Network.MAINNET,"钱包备注钱包备注钱包备注钱包备注",true);
        }finally{WalletCrypto.wipe(scalar);Arrays.fill(password,'\0');}
        WalletCrypto.Metadata meta=new WalletCrypto().inspect(payload);
        store=new VerificationStore(host.getTargetContext(),"viewport-"+Long.toHexString(System.nanoTime()));
        try{
            for(String language:new String[]{"zh","en","ja","ko"}){
                if(activity!=null)main(activity::finish);
                Texts.select(host.getTargetContext(),language);store.clear();
                activity=(MainActivity)host.startActivitySync(new Intent(host.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));host.waitForIdleSync();main(()->set("verificationStore",store));
                accept(payload);viewport(language+"-pending");
                store.save(VerificationStore.Record.verified(payload,meta.addresses,meta.note,Collections.emptyList()));
                accept(payload);yes(get("unlocked")==null,"local matching remains private-key locked");viewport(language+"-matched");
                main(()->{invoke("showUnlockDialog");Dialog d=(Dialog)get("unlockDialog");for(View v:views(d.getWindow().getDecorView()))if(v instanceof EditText)((EditText)v).setText(PASSWORD);button(d.getWindow().getDecorView(),activity.getString(R.string.wallet_unlock_action)).performClick();});
                waitFor(()->"revealed".equals(get("screen")),20000);viewport(language+"-unlocked");
                main(()->invoke("navigate",new Class<?>[]{String.class},"home"));yes(get("unlocked")==null,"home still clears the private session");
            }
            return checks;
        }finally{if(activity!=null)main(activity::finish);store.clear();Texts.select(host.getTargetContext(),"zh");}
    }
    private void accept(String payload)throws Exception{main(()->invoke("acceptIncoming",new Class<?>[]{String.class},payload));waitFor(()->!(Boolean)get("recordLoading"),5000);host.waitForIdleSync();}
    private void viewport(String name)throws Exception{
        host.waitForIdleSync();Thread.sleep(120);host.waitForIdleSync();
        main(()->{
            AppUi ui=(AppUi)get("ui");int available=ui.scroll.getHeight()-ui.scroll.getPaddingTop()-ui.scroll.getPaddingBottom();
            capture(name,ui.scroll);
            yes(ui.page.getHeight()<=available+1&&!ui.scroll.canScrollVertically(1),name+" fits: page="+ui.page.getHeight()+" viewport="+available);
            int actions=0;
            for(View v:views(ui.page)){
                if(v instanceof Button&&v.getParent()==ui.page){Rect visible=new Rect();yes(v.getGlobalVisibleRect(visible)&&visible.height()==v.getHeight(),name+" action fully visible");yes(v.getHeight()>=ui.dp(48),name+" action retains touch target");actions++;}
                if(v instanceof TextView){android.text.Layout layout=((TextView)v).getLayout();if(layout!=null)for(int i=0;i<layout.getLineCount();i++)yes(layout.getEllipsisCount(i)==0,name+" text is not truncated");}
            }
            yes(actions==2,name+" keeps both primary and secondary actions");
            View footer=ui.page.getChildAt(ui.page.getChildCount()-1);Rect visible=new Rect();yes(footer.getGlobalVisibleRect(visible)&&visible.height()==footer.getHeight(),name+" footer fully visible");
        });
    }
    private void capture(String name,View v){
        Bitmap b=Bitmap.createBitmap(v.getWidth(),v.getHeight(),Bitmap.Config.ARGB_8888);
        try{v.draw(new Canvas(b));File dir=new File(activity.getExternalFilesDir("qa"),"recovery-fit");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}}catch(IOException ex){throw new RuntimeException(ex);}finally{b.recycle();}
    }
    private static List<View> views(View v){List<View> all=new ArrayList<>();all.add(v);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)all.addAll(views(((ViewGroup)v).getChildAt(i)));return all;}
    private static Button button(View v,String label){for(View x:views(v))if(x instanceof Button&&(label.equals(x.getContentDescription())||label.contentEquals(((Button)x).getText())))return (Button)x;throw new AssertionError("Missing action "+label);}
    private Object get(String name){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void set(String name,Object value){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void invoke(String name){invoke(name,new Class<?>[0]);}
    private void invoke(String name,Class<?>[] types,Object... args){try{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(activity,args);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void main(Runnable r){Throwable[] error={null};host.runOnMainSync(()->{try{r.run();}catch(Throwable ex){error[0]=ex;}});if(error[0]!=null)throw new AssertionError("UI operation failed",error[0]);}
    private void waitFor(java.util.function.BooleanSupplier ready,long timeout)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;while(!ready.getAsBoolean()&&SystemClock.elapsedRealtime()<end){Thread.sleep(25);main(()->{});}yes(ready.getAsBoolean(),"asynchronous operation completes");host.waitForIdleSync();}
    private void yes(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;}
}
