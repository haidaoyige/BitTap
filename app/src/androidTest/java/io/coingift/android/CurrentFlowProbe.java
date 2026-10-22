package io.coingift.android;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.*;
import android.widget.*;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import io.coingift.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Current full workflow. Generated disposable test wallet, never funds; NFC transport is simulated. */
final class CurrentFlowProbe {
    private final Instrumentation host;
    private MainActivity activity;
    private int checks;
    private static final String PASSWORD="Zq7!rT9@pL4#vN6$";
    CurrentFlowProbe(Instrumentation host){this.host=host;}
    int run()throws Exception{
        Texts.select(host.getTargetContext(),"zh");
        VerificationStore records=new VerificationStore(host.getTargetContext());records.clear();
        Uri document=null;
        try{
            activity=(MainActivity)host.startActivitySync(new Intent(host.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));host.waitForIdleSync();
            until(()->Boolean.TRUE.equals(get("resumed")),5000);
            click(root(),new ApprovedCopy(activity).ui("make"));
            yes("home".equals(get("screen"))&&get("session")==null,"entry requires risk consent before generating");
            click(dialog("sheet"),new ApprovedCopy(activity).ui("agree"));
            EditText note=null;for(View v:views(root()))if(v instanceof EditText){note=(EditText)v;break;}
            yes(note!=null,"setup offers wallet note");EditText field=note;
            main(()->{field.setText("audit44");invoke("showCreationDialog",new Class<?>[]{EditText.class},field);});
            main(()->{for(View v:views(dialog("creationDialog")))if(v instanceof EditText)((EditText)v).setText(PASSWORD);});
            click(dialog("creationDialog"),new ApprovedCopy(activity).ui("creationConfirmGenerate"));
            until(()->"write".equals(get("screen")),25000);
            CreationSession owner=(CreationSession)get("session");String payload=owner.encrypted;
            yes(payload.startsWith("CG3:")&&owner.note.equals("audit44"),"real UI creation emits the authenticated noted format");
            yes(get("unlocked")==null&&get("creationDialog")==null,"creation keeps no unlocked session or password dialog");
            char[] password=PASSWORD.toCharArray();
            try(WalletCrypto.Unlocked restored=new WalletCrypto().decrypt(payload,password)){
                yes(restored.addresses.sameAs(owner.addresses)&&restored.note.equals(owner.note),"independent recovery of exact generated payload agrees on all addresses and note");
            }finally{WalletCrypto.wipe(password);}
            yes(NfcCards.parse(NfcCards.message(payload)).equals(payload),"NDEF serialization preserves exact generated ciphertext");
            main(()->{owner.markCardWritten(new byte[]{4,4,1,2});set("screen","done");invoke("render");});
            yes(owner.needsBackup()&&!owner.canFinish(),"simulated write/readback still requires actual file backup");
            main(()->invoke("showAddressQr",new Class<?>[]{String.class,String.class,boolean.class},"SegWit",owner.addresses.nativeSegwit,true));
            yes(get("qrDialog")==null&&get("sheet")!=null,"receiving QR unavailable before mandatory backup");main(()->invoke("closeSheet"));
            main(()->activity.onActivityResult(22,Activity.RESULT_CANCELED,null));yes(!owner.backupSaved(),"cancel cannot satisfy mandatory backup");
            ContentValues values=new ContentValues();values.put(MediaStore.MediaColumns.DISPLAY_NAME,"bittap-audit44-disposable-"+System.nanoTime()+".txt");values.put(MediaStore.MediaColumns.MIME_TYPE,"text/plain");
            document=activity.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);yes(document!=null,"public test destination available");Uri target=document;
            main(()->{set("exportData",payload);set("exportAction",null);activity.onActivityResult(22,Activity.RESULT_OK,new Intent().setData(target));});
            until(()->!(Boolean)get("fileBusy")&&owner.backupSaved(),20000);
            try(InputStream in=activity.getContentResolver().openInputStream(document)){yes(payload.equals(new String(BackupFiles.readBounded(in),StandardCharsets.US_ASCII)),"backup stream closes and reads back exactly");}
            until(()->{try{return records.records().size()==1;}catch(Exception ex){return false;}},10000);
            yes(owner.canFinish(),"verified backup permits completion");
            main(()->invoke("leaveCreation"));yes(get("session")==null&&get("incoming")==null&&get("unlocked")==null,"finish clears creation and secret session references");
            accept(payload,"04040102");yes(get("publicVerification")!=null&&get("unlocked")==null,"created wallet re-reads without password or private-key unlock");
            checkReceiving(owner.addresses);
            records.clear();accept(payload,"04040102");
            yes(get("publicVerification")==null&&!has(root(),owner.addresses.nativeSegwit),"unknown record does not disclose a complete receiving address");
            main(()->invoke("showAddressQr",new Class<?>[]{String.class,String.class,boolean.class},"SegWit",owner.addresses.nativeSegwit,false));
            yes(get("qrDialog")==null&&get("unlockDialog")!=null,"unverified QR routes to password verification, not export consent");main(()->invoke("closeUnlockDialog"));
            submit("wrong public audit password");until(()->!(Boolean)get("busy"),25000);
            yes(get("unlocked")==null&&get("publicVerification")==null&&records.records().isEmpty(),"wrong password establishes neither a key session nor receiving trust");
            submit(PASSWORD);until(()->"revealed".equals(get("screen")),25000);
            yes(get("unlocked")!=null&&get("publicVerification")!=null,"correct password derives and checks current addresses");
            checkReceiving(owner.addresses);
            main(()->invoke("showPrivateQr"));until(()->get("qrDialog")!=null,15000);
            PrivateKeyView shown=(PrivateKeyView)get("secretView");Bitmap qr=(Bitmap)get("qrBitmap");char[] wif=shown.copyCharacters();
            try{yes(new String(wif).equals(decode(qr)),"explicit private QR matches displayed WIF exactly");yes(!has(root(),new String(wif))&&!has(dialog("qrDialog"),new String(wif)),"WIF is absent from ordinary TextView nodes");}finally{WalletCrypto.wipe(wif);}
            PrivateSession session=(PrivateSession)get("unlocked");main(()->invoke("requestPrivateCopy"));yes(get("copyConfirm")!=null,"private copy requires a separate confirmation");
            main(()->invoke("closeCopyConfirm"));
            main(()->{invoke("navigate",new Class<?>[]{String.class},"home");});
            yes(shown.isCleared()&&qr.isRecycled()&&get("unlocked")==null&&get("incoming")==null&&get("pendingQrImage")==null,"home clears private presentation and transient read/export state");
            try{session.wif();throw new AssertionError("Closed private session usable");}catch(IllegalStateException expected){checks++;}
            until(()->{try{return !records.records().isEmpty();}catch(Exception ex){return false;}},10000);
            accept(payload,"08080707");yes(get("publicVerification")!=null&&get("unlocked")==null,"identical wallet on another NFC UID still uses public-only verification");
            records.clear();String altered=mutate(payload);accept(altered,"04040102");submit(PASSWORD);until(()->!(Boolean)get("busy"),25000);
            yes(get("unlocked")==null&&get("publicVerification")==null&&records.records().isEmpty(),"modified encrypted payload cannot create trust even with the correct password");
            return checks;
        }finally{
            if(document!=null)host.getTargetContext().getContentResolver().delete(document,null,null);
            if(activity!=null)main(activity::finish);records.clear();Texts.select(host.getTargetContext(),"zh");
        }
    }
    private void checkReceiving(BitcoinAddresses a)throws Exception{
        String[] types={"SegWit","Legacy","P2SH","Taproot"},values={a.nativeSegwit,a.legacy,a.nestedSegwit,a.taproot};
        for(int i=0;i<types.length;i++){String type=types[i],address=values[i];main(()->invoke("showAddressQr",new Class<?>[]{String.class,String.class,boolean.class},type,address,true));yes(get("qrBitmap")!=null&&address.equals(decode((Bitmap)get("qrBitmap"))),"current trusted "+type+" QR preserves exact address");main(()->invoke("dismissQr"));}
    }
    private void accept(String payload,String uid)throws Exception{main(()->invoke("acceptIncoming",new Class<?>[]{String.class,String.class},payload,uid));until(()->!(Boolean)get("recordLoading"),10000);host.waitForIdleSync();}
    private void submit(String password)throws Exception{main(()->{invoke("showUnlockDialog");for(View v:views(dialog("unlockDialog")))if(v instanceof EditText)((EditText)v).setText(password);button(dialog("unlockDialog"),activity.getString(R.string.wallet_unlock_action)).performClick();});}
    private static String mutate(String payload){byte[] raw=Base64.getUrlDecoder().decode(payload.substring(4));raw[raw.length-1]^=1;return payload.substring(0,4)+Base64.getUrlEncoder().withoutPadding().encodeToString(raw);}
    // Exact, undistorted monochrome pixels: validate QR data directly, not camera finder heuristics.
    private static String decode(Bitmap b)throws Exception{int[] pixels=new int[b.getWidth()*b.getHeight()];b.getPixels(pixels,0,b.getWidth(),0,0,b.getWidth(),b.getHeight());try{return new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(b.getWidth(),b.getHeight(),pixels))),Collections.singletonMap(DecodeHintType.PURE_BARCODE,Boolean.TRUE)).getText();}finally{Arrays.fill(pixels,0);}}
    private View root(){return ((AppUi)get("ui")).page;}
    private View dialog(String name){Dialog d=(Dialog)get(name);if(d==null)throw new AssertionError("Missing dialog: "+name);return d.getWindow().getDecorView();}
    private static List<View> views(View v){List<View> list=new ArrayList<>();list.add(v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)list.addAll(views(g.getChildAt(i)));}return list;}
    private static boolean has(View v,String text){for(View child:views(v))if(child instanceof TextView&&text.contentEquals(((TextView)child).getText()))return true;return false;}
    private static Button button(View v,String text){for(View child:views(v))if(child instanceof Button&&(text.equals(child.getContentDescription())||text.contentEquals(((Button)child).getText())))return (Button)child;throw new AssertionError("Missing button "+text);}
    private void click(View v,String text){main(()->button(v,text).performClick());host.waitForIdleSync();}
    private Object get(String name){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void set(String name,Object value){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void invoke(String name){invoke(name,new Class<?>[0]);}
    private void invoke(String name,Class<?>[] types,Object...args){try{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(activity,args);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void main(Runnable r){Throwable[] error={null};host.runOnMainSync(()->{try{r.run();}catch(Throwable ex){error[0]=ex;}});if(error[0]!=null)throw new AssertionError("UI operation failed",error[0]);}
    private void until(java.util.function.BooleanSupplier condition,long timeout)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;while(!condition.getAsBoolean()&&SystemClock.elapsedRealtime()<end){Thread.sleep(25);main(()->{});}yes(condition.getAsBoolean(),"async operation completes");host.waitForIdleSync();}
    private void yes(boolean condition,String message){if(!condition)throw new AssertionError(message);checks++;}
}
