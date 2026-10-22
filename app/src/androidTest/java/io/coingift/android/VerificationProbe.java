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
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Disposable public scalar-one fixtures. No live card, wallet, clipboard data or funds. */
final class VerificationProbe {
    private final Instrumentation host;
    private MainActivity activity;
    private int checks;
    private VerificationStore store;
    private String namespace;
    private static final String PASSWORD="test fixture password only",UID="01020304";
    VerificationProbe(Instrumentation host){this.host=host;}
    int run()throws Exception{
        String payload;try(InputStream in=host.getContext().getAssets().open("cg3-scalar-one.txt")){payload=new String(BackupFiles.readBounded(in),StandardCharsets.US_ASCII).trim();}
        namespace="test-"+Long.toHexString(System.nanoTime());store=new VerificationStore(host.getTargetContext(),namespace);
        try{
            storage(payload);store.clear();launch("zh");accept(payload,UID);
            yes(get("publicVerification")==null&&get("unlocked")==null,"first read has no trusted record or secret session");
            yes(!has(root(),new WalletCrypto().inspect(payload).address),"unverified full address is withheld");
            main(()->invoke("showAddressQr",new Class<?>[]{String.class,String.class,boolean.class},"SegWit",new WalletCrypto().inspect(payload).address,false));
            yes(get("qrDialog")==null&&get("unlockDialog")!=null,"unverified QR routes to password verification");main(()->invoke("closeUnlockDialog"));
            unlock(false);yes(get("publicVerification")!=null&&get("unlocked")!=null,"password unlock binds public snapshot and sealed session");
            until(()->{try{return store.records().size()==1;}catch(Exception ex){return false;}},5000);
            PrivateSession owner=(PrivateSession)get("unlocked");main(()->invoke("showPrivateQr"));until(()->get("qrDialog")!=null,10000);
            yes(get("unlockDialog")==null&&get("unlocked")==owner,"viewing private key reuses the same valid session without password");
            main(()->{UnlockDeadline d=(UnlockDeadline)get("unlockDeadline");d.start(SystemClock.elapsedRealtime()-60_001);invoke("tickUnlockedSession");});
            yes(get("unlocked")==null&&get("qrDialog")==null&&get("publicVerification")!=null,"lock erases secret presentation but retains public verification");
            main(()->invoke("dismissAutoLockDialog"));
            String address=new WalletCrypto().inspect(payload).address;
            main(()->invoke("showAddressQr",new Class<?>[]{String.class,String.class,boolean.class},"SegWit",address,true));yes(get("qrDialog")!=null&&get("unlockDialog")==null,"locked public snapshot still displays receiving QR");main(()->invoke("dismissQr"));
            main(()->invoke("navigate",new Class<?>[]{String.class},"read"));accept(payload,UID);
            yes("local".equals(get("verificationSource"))&&get("unlocked")==null&&get("unlockDialog")==null,"matching read verifies with no password or private session");
            yes(has(root(),activity.getString(R.string.verification_local)),"address carries matching-record status");
            main(()->invoke("showUnlockDialog",new Class<?>[]{boolean.class},true));yes(get("unlockDialog")!=null,"explicit private request after a local match requires password");main(()->invoke("closeUnlockDialog"));
            String damaged=mutate(payload);accept(damaged,UID);yes(get("publicVerification")==null&&get("previousVerification")!=null,"same card hint plus changed payload never inherits trust");
            main(()->invoke("showUnlockDialog"));submit("wrong public fixture password");until(()->!(Boolean)get("busy"),20000);yes(get("unlocked")==null&&get("publicVerification")==null,"wrong password does not create trust or a key session");main(()->invoke("closeUnlockDialog"));
            byte[] scalar=new byte[32];scalar[31]=1;String rewritten;try{rewritten=rewriteFixture(scalar);}finally{WalletCrypto.wipe(scalar);}
            accept(rewritten,UID);unlock(true);yes(get("pendingVerification")!=null&&get("publicVerification")==null,"changed valid data waits for explicit confirmation");
            main(()->invoke("showPrivateQr"));yes(get("qrDialog")==null,"pending review cannot export a private key");
            main(()->invoke("closeSheet"));yes(get("unlocked")==null&&get("pendingVerification")==null,"dismissing changed-data review closes its sealed session");
            yes(store.records().size()==1,"dismissing review never persists the changed payload");
            unlock(true);Button expiredAccept=button(dialog("sheet"),activity.getString(R.string.verification_accept));
            main(()->{UnlockDeadline d=(UnlockDeadline)get("unlockDeadline");d.start(SystemClock.elapsedRealtime()-121_000);invoke("tickUnlockedSession");});
            yes(get("unlocked")==null&&get("pendingVerification")==null&&get("publicVerification")==null,"absolute expiry during review closes the unconfirmed session without public trust");
            main(expiredAccept::performClick);yes(store.records().size()==1&&get("unlocked")==null,"expired review confirmation cannot persist or revive the session");
            main(()->invoke("dismissAutoLockDialog"));unlock(true);PrivateSession pending=(PrivateSession)get("unlocked");
            main(()->button(dialog("sheet"),activity.getString(R.string.verification_accept)).performClick());
            yes(get("pendingVerification")==null&&get("publicVerification")!=null&&get("unlocked")==pending,"review acceptance reuses the valid session, no second password");
            until(()->{try{return store.records().size()==2;}catch(Exception ex){return false;}},5000);
            accept(rewritten,"05060708");yes(get("publicVerification")!=null,"same complete payload matches on another card UID");
            main(()->{invoke("help");button(dialog("sheet"),activity.getString(R.string.verification_entry)).performClick();});
            yes("verification".equals(get("screen")),"about opens the password-free explanation first");
            main(()->button(root(),activity.getString(R.string.verification_manage)).performClick());until(()->((List<?>)get("recordRows")).size()==2,5000);
            main(()->{invoke("confirmClearRecord",new Class<?>[]{String.class},new Object[]{null});button(dialog("sheet"),activity.getString(R.string.verification_clear_action)).performClick();});
            until(()->!(Boolean)get("busy")&&((List<?>)get("recordRows")).isEmpty(),5000);
            yes(store.records().isEmpty()&&get("publicVerification")==null&&get("unlocked")==null,"clear removes records and invalidates active trust/session");
            main(()->invoke("back"));yes("verification".equals(get("screen")),"record manager returns to explanation");
            creationGate(payload);
            for(String language:new String[]{"zh","en","ja","ko"})alignment(language,payload);
            return checks;
        }finally{
            if(activity!=null)main(()->activity.finish());store.clear();Texts.select(host.getTargetContext(),"zh");
        }
    }
    private static String rewriteFixture(byte[] scalar)throws Exception{
        Method m=WalletCrypto.class.getDeclaredMethod("encrypt",byte[].class,char[].class,WalletCrypto.Network.class,String.class,boolean.class);m.setAccessible(true);
        char[] password=PASSWORD.toCharArray();try{return (String)m.invoke(new WalletCrypto(),scalar,password,WalletCrypto.Network.MAINNET,"changed",true);}finally{java.util.Arrays.fill(password,'\0');}
    }
    private void storage(String payload)throws Exception{
        WalletCrypto.Metadata meta=new WalletCrypto().inspect(payload);
        VerificationStore.Record r=VerificationStore.Record.verified(payload,meta.addresses,meta.note,Collections.singleton(UID));store.save(r);
        yes(store.records().size()==1&&store.lookup(payload,UID).match!=null,"authenticated record survives a disk round trip");
        yes(store.lookup(payload,"05060708").match!=null,"UID is not the trust anchor");
        VerificationStore.Lookup changed=store.lookup(mutate(payload),UID);yes(changed.match==null&&changed.previous!=null,"complete ciphertext change invalidates fast match");
        yes(store.lookup(mutate(payload),"05060708").previous==null,"unknown card cannot be called a known modified card");
        KeyStore keys=KeyStore.getInstance("AndroidKeyStore");keys.load(null);String alias="bittap-address-records-v1-"+namespace;
        yes(keys.getKey(alias,null).getEncoded()==null,"record protection key is non-exportable");
        File f=new File(host.getTargetContext().getNoBackupFilesDir(),"address-records-v1-"+namespace+".bin");byte[] saved=read(f);
        String disk=new String(saved,StandardCharsets.ISO_8859_1);yes(!disk.contains(PASSWORD)&&!disk.contains(payload)&&!disk.contains(meta.address)&&!disk.contains("KwDiBf89"),"disk record is authenticated ciphertext, no wallet backup/password/plaintext key");
        byte[] damaged=saved.clone();damaged[damaged.length-1]^=1;write(f,damaged);rejectRead("tampered record fails closed");write(f,saved);
        keys.deleteEntry(alias);rejectRead("missing protection key fails closed");
        try{store.save(r);throw new AssertionError("unreadable history silently overwritten");}catch(Exception expected){checks++;}
        store.clear();yes(store.records().isEmpty(),"clearing unavailable records restores an empty usable store");
    }
    private void creationGate(String payload)throws Exception{
        store.clear();main(()->invoke("clearWalletSession",new Class<?>[]{boolean.class},true));CreationSession creation=new CreationSession(payload);creation.markCardWritten(new byte[]{9,8,7,6});
        main(()->{set("session",creation);set("screen","done");invoke("render");});host.waitForIdleSync();yes(store.records().isEmpty(),"no persistent record before required backup");
        main(()->invoke("showAddressQr",new Class<?>[]{String.class,String.class,boolean.class},"SegWit",creation.addresses.nativeSegwit,true));yes(get("qrDialog")==null,"receiving QR stays unavailable before backup");main(()->invoke("closeSheet"));
        main(()->{creation.markBackupSaved(payload);invoke("render");});until(()->{try{return store.records().size()==1;}catch(Exception ex){return false;}},5000);
        yes(store.lookup(payload,"09080706").match!=null,"write/readback plus backup establishes local record");
        main(()->{creation.markCardWritten(new byte[]{1,1,2,2});invoke("render");});until(()->{try{return store.lookup(mutate(payload),"01010202").previous!=null;}catch(Exception ex){return false;}},5000);
        yes(store.records().size()==1,"multiple cards merge hints into the same complete-payload record");
    }
    private void alignment(String language,String payload)throws Exception{
        launch(language);
        for(String page:new String[]{"home","setup","read","verification","records","protection","security"}){
            main(()->{set("screen",page);invoke("render");});host.waitForIdleSync();checkRows(root());capture(language+"-"+page,root());
        }
        accept(payload,UID);checkRows(root());capture(language+"-matched",root());
        unlock(false);checkRows(root());capture(language+"-unlocked",root());
        main(()->invoke("showPrivateQr"));until(()->get("qrDialog")!=null,10000);checkRows(dialog("qrDialog"));capture(language+"-private",dialog("qrDialog"));main(()->invoke("dismissQr"));
        main(()->{invoke("closeUnlocked");set("screen","unlock");invoke("render");});
        main(()->{invoke("help");});host.waitForIdleSync();checkRows(dialog("sheet"));capture(language+"-about",dialog("sheet"));main(()->invoke("closeSheet"));
        main(()->invoke("showUnlockDialog",new Class<?>[]{boolean.class},true));host.waitForIdleSync();checkRows(dialog("unlockDialog"));capture(language+"-password",dialog("unlockDialog"));main(()->invoke("closeUnlockDialog"));
        main(()->invoke("showAddressQr",new Class<?>[]{String.class,String.class,boolean.class},"SegWit",new WalletCrypto().inspect(payload).address,true));host.waitForIdleSync();checkRows(dialog("qrDialog"));capture(language+"-receive",dialog("qrDialog"));main(()->invoke("dismissQr"));
        CreationSession creation=new CreationSession(payload);creation.markCardWritten(new byte[]{1,2,3,4});creation.markBackupSaved(payload);
        main(()->{set("session",creation);set("screen","done");invoke("render");});host.waitForIdleSync();checkRows(root());capture(language+"-done",root());
    }
    private void checkRows(View view){
        for(View v:views(view))if(v instanceof LinearLayout){LinearLayout row=(LinearLayout)v;if(row.getOrientation()!=LinearLayout.HORIZONTAL||row.getHeight()==0)continue;
            ImageView image=null;TextView text=null;
            for(int i=0;i<row.getChildCount();i++){View child=row.getChildAt(i);if(child instanceof ImageView&&!(child instanceof ImageButton))image=(ImageView)child;else if(child instanceof TextView&&!(child instanceof Button))text=(TextView)child;}
            if(image!=null&&text!=null&&text.getLineCount()==1&&text.getHeight()>0){float a=image.getTop()+image.getTranslationY()+image.getHeight()/2f,b=text.getTop()+text.getHeight()/2f;yes(Math.abs(a-b)<=activity.getResources().getDisplayMetrics().density*2,"single-line image and text centers align: "+text.getText());}
            yes(!row.isBaselineAligned(),"horizontal rows use vertical gravity, no baseline override");
        }
    }
    private void capture(String name,View view){main(()->{if(view.getWidth()==0||view.getHeight()==0)return;Bitmap b=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);try{Canvas canvas=new Canvas(b);canvas.drawColor(AppUi.BG);view.draw(canvas);File dir=new File(activity.getExternalFilesDir("qa"),"verification");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}}catch(IOException ex){throw new RuntimeException(ex);}finally{b.recycle();}});}
    private void launch(String language){if(activity!=null)main(()->activity.finish());Texts.select(host.getTargetContext(),language);activity=(MainActivity)host.startActivitySync(new Intent(host.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));host.waitForIdleSync();main(()->set("verificationStore",store));}
    private void accept(String payload,String uid)throws Exception{main(()->invoke("acceptIncoming",new Class<?>[]{String.class,String.class},payload,uid));until(()->!(Boolean)get("recordLoading"),5000);host.waitForIdleSync();}
    private void unlock(boolean reviewing)throws Exception{main(()->invoke("showUnlockDialog"));submit(PASSWORD);until(()->reviewing?get("pendingVerification")!=null:"revealed".equals(get("screen")),20000);host.waitForIdleSync();}
    private void submit(String password){main(()->{View d=dialog("unlockDialog");for(View v:views(d))if(v instanceof EditText)((EditText)v).setText(password);button(d,activity.getString(R.string.wallet_unlock_action)).performClick();});}
    private void rejectRead(String message)throws Exception{try{store.records();throw new AssertionError(message);}catch(Exception expected){checks++;}}
    private static String mutate(String p){byte[] b=Base64.getUrlDecoder().decode(p.substring(4));b[b.length-1]^=1;return p.substring(0,4)+Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    private static byte[] read(File f)throws Exception{try(InputStream in=new FileInputStream(f)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
    private static void write(File f,byte[] b)throws Exception{try(OutputStream out=new FileOutputStream(f)){out.write(b);}}
    private View root(){return ((AppUi)get("ui")).page;}
    private View dialog(String field){Dialog d=(Dialog)get(field);if(d==null)throw new AssertionError("Missing dialog "+field);return d.getWindow().getDecorView();}
    private static List<View> views(View v){List<View> out=new ArrayList<>();out.add(v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)out.addAll(views(g.getChildAt(i)));}return out;}
    private static boolean has(View v,String value){for(View x:views(v))if(x instanceof TextView&&value.contentEquals(((TextView)x).getText()))return true;return false;}
    private static Button button(View v,String label){for(View x:views(v))if(x instanceof Button){Button b=(Button)x;if(label.equals(b.getContentDescription())||label.contentEquals(b.getText()))return b;}throw new AssertionError("Missing button "+label);}
    private Object get(String name){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void set(String name,Object value){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void invoke(String name){invoke(name,new Class<?>[0]);}
    private void invoke(String name,Class<?>[] types,Object... args){try{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(activity,args);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void main(Runnable r){host.runOnMainSync(r);}
    private void until(BooleanSupplier ready,long ms)throws Exception{long end=SystemClock.elapsedRealtime()+ms;while(!ready.getAsBoolean()&&SystemClock.elapsedRealtime()<end){Thread.sleep(30);main(()->{});}yes(ready.getAsBoolean(),"asynchronous operation completes");}
    private void yes(boolean value,String message){if(!value)throw new AssertionError(message);checks++;}
}
