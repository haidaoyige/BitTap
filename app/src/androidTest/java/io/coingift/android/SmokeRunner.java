package io.coingift.android;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.nfc.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import io.coingift.core.*;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Dependency-free device smoke tests. Fixtures are public scalar 1, never use for funds. */
public final class SmokeRunner extends Instrumentation {
    private MainActivity activity;
    private int checks;
    private static final String WIF="KwDiBf89QgGbjEhKnhXJuH7LrciVrZi3qYjgd9M7rFU73sVHnoWn";
    private boolean cameraOnly;
    private boolean homeOnly;
    private boolean imageOnly;
    private boolean copyOnly;
    private boolean copyVisualOnly;
    private boolean inputUiOnly;
    @Override public void onCreate(Bundle args){super.onCreate(args);cameraOnly=args!=null&&"true".equals(args.getString("cameraOnly"));homeOnly=args!=null&&"true".equals(args.getString("homeOnly"));imageOnly=args!=null&&"true".equals(args.getString("imageOnly"));copyVisualOnly=args!=null&&"true".equals(args.getString("copyVisualOnly"));copyOnly=copyVisualOnly||args!=null&&"true".equals(args.getString("copyOnly"));inputUiOnly=args!=null&&"true".equals(args.getString("inputUiOnly"));start();}
    @Override public void onStart(){
        Bundle result=new Bundle();
        try{
            if(cameraOnly){cameraProbe();result.putString("stream","PASS: "+checks+" Camera2 emulator assertions; no physical-camera entropy claim.\n");finish(Activity.RESULT_OK,result);return;}
            if(inputUiOnly){if(android.os.Build.VERSION.SDK_INT<30)throw new IllegalStateException("IME probe requires API 30+");inputUiProbe();result.putString("stream","PASS: "+checks+" input visibility and recovery UI assertions.\n");finish(Activity.RESULT_OK,result);return;}
            if(copyOnly){copyConfirmationProbe();result.putString("stream","PASS: "+checks+" private-copy confirmation assertions.\n");finish(Activity.RESULT_OK,result);return;}
            if(imageOnly){privateImageArtworkProbe();addressImageProbe();result.putString("stream","PASS: "+checks+" receiving/private image export assertions.\n");finish(Activity.RESULT_OK,result);return;}
            Texts.select(getTargetContext(),"zh");
            if(homeOnly){
                Intent launch=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
                activity=(MainActivity)startActivitySync(launch);waitForIdleSync();
                yes("home".equals(get("screen")),"home opens");capture("01-home");
                result.putString("stream","PASS: home visual capture.\n");finish(Activity.RESULT_OK,result);return;
            }
            String cg2=asset("cg2-scalar-one.txt"),cg1=asset("cg1-scalar-one.txt");
            ndef(cg2,cg1);
            clipboardDenials();
            Intent launch=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity=(MainActivity)startActivitySync(launch);waitForIdleSync();
            yes("home".equals(get("screen")),"home opens");capture("01-home");uiAlignment();nextUi(asset("cg3-scalar-one.txt"));
            click("制作冷钱包");yes("home".equals(get("screen")),"creation requires entry consent");clickDialog("同意并继续");input("123456");
            yes(hasPrefix(root(),"密码强度：弱"),"weak password receives advice");yes(button("生成冷钱包").isEnabled(),"strength advice does not disable creation");
            EditText oldPassword=firstInput();input("Mx7!pQ2@tL8#vR5$zN");yes(hasPrefix(root(),"密码强度：较强"),"stronger advice updates while typing");
            input("123456");capture("06-password-strength");main(()->activity.onBackPressed());yes(oldPassword.length()==0,"returning home clears old password input");
            Bundle initialState=new Bundle();main(()->activity.onSaveInstanceState(initialState));yes(initialState.isEmpty(),"home has no serialized wallet or UI state");
            click("读取冷钱包");clickDialog("同意并继续");yes(hasAction(root(),"导入加密备份文件")&&!hasAction(root(),"粘贴加密备份"),"reading offers NFC and files only");importBackup(cg2);
            yes("unlock".equals(get("screen")),"CG2 opens without password");yes(get("unlocked")==null,"public metadata does not unlock key");
            BitcoinAddresses publicAddresses=new WalletCrypto().inspect(cg2).addresses;
            addressWrap(publicAddresses.nativeSegwit);
            yes(hasAction(root(),"展示地址")&&!hasAction(root(),"复制地址")&&!hasAction(root(),"查看地址二维码"),"address card has one display entry and no duplicate QR/copy actions");
            click("展示地址");addressQr(publicAddresses.nativeSegwit,false);capture("07-address-qr-unverified");
            yes(get("unlocked")==null,"address QR requires no decrypted private key");
            publicImageExports(publicAddresses.nativeSegwit);
            Thread.sleep(12000); // Let expected export cancellation/failure/success toasts drain before public captures.
            click("展示地址");
            Bitmap closedQr=(Bitmap)get("qrBitmap");dismiss("qrDialog");yes(closedQr.isRecycled()&&get("qrBitmap")==null,"public QR pixels cleared on dismissal");
            click("其他收款地址");
            View addresses=dialogRoot("sheet");yes(has(addresses,"Legacy"),"legacy address without password");yes(has(addresses,"P2SH"),"nested address without password");yes(has(addresses,"Taproot"),"taproot address without password");capture("02-public-addresses");
            String[] types={"SegWit","Legacy","P2SH","Taproot"};
            String[] destinations={publicAddresses.nativeSegwit,publicAddresses.legacy,publicAddresses.nestedSegwit,publicAddresses.taproot};
            for(int i=0;i<types.length;i++){
                clickDialog("展示 "+types[i]+" 地址");addressQr(destinations[i],false);dismiss("qrDialog");
                yes(((Dialog)get("sheet")).isShowing(),"closing QR preserves other-address list");
            }
            dismiss("sheet");
            input("wrong fixture password");click("解密冷钱包");until(()->!(Boolean)get("busy"),20000);
            yes("unlock".equals(get("screen"))&&get("unlocked")==null,"wrong password releases no key");
            Thread.sleep(3500); // Let the expected wrong-password toast expire before capturing the next page.
            input("test fixture password only");click("解密冷钱包");until(()->"revealed".equals(get("screen")),20000);
            yes((((Activity)activity).getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"private screen secure");
            yes(((TextView)get("secretView")).getText().toString().startsWith("•"),"WIF masked by default");capture("03-recovered-masked");
            click("展示地址");addressQr(publicAddresses.nativeSegwit,true);capture("08-address-qr-verified");
            Bitmap backgroundQr=(Bitmap)get("qrBitmap");main(()->{callActivityOnPause(activity);callActivityOnStop(activity);});
            yes(backgroundQr.isRecycled()&&get("qrDialog")==null,"background clears address QR");main(()->callActivityOnResume(activity));
            input("test fixture password only");click("解密冷钱包");until(()->"revealed".equals(get("screen")),20000);
            click("显示");yes(WIF.contentEquals(((TextView)get("secretView")).getText()),"exact compressed mainnet WIF");
            click("如何转出 BTC");yes(has(dialogRoot("sheet"),"哪些钱包可以导入？")&&has(dialogRoot("sheet"),"OKX Web3 Wallet · web3.okx.com"),"three approved recovery instructions");dismiss("sheet");
            click("私钥二维码");yes(get("qrBitmap")!=null,"private QR generated on explicit request");yes(((Bitmap)get("qrBitmap")).isMutable(),"QR pixel storage can be erased safely");dismiss("qrDialog");yes(get("qrBitmap")==null,"QR bitmap erased on dismissal");
            click("私钥二维码");privateImageExport();
            input("test fixture password only");click("解密冷钱包");until(()->"revealed".equals(get("screen")),20000);
            WalletCrypto.Unlocked previous=(WalletCrypto.Unlocked)get("unlocked");
            yes(!hasAction(root(),"复制 WIF 私钥"),"recovery page no longer has a private-copy button");
            click("私钥二维码");clickDialog("qrDialog",new ApprovedCopy(activity).ui("copyWif"));clickDialog("copyConfirm",activity.getString(R.string.copy_confirm_action));
            ClipboardManager clipboard=(ClipboardManager)getTargetContext().getSystemService(Context.CLIPBOARD_SERVICE);
            yes(clipboard.getPrimaryClip()!=null&&WIF.contentEquals(clipboard.getPrimaryClip().getItemAt(0).getText()),"explicit private copy works");
            main(()->{callActivityOnPause(activity);callActivityOnStop(activity);});
            yes(get("unlocked")==null&&"unlock".equals(get("screen")),"background clears decrypted key");
            yes(clipboard.getPrimaryClip()!=null&&WIF.contentEquals(clipboard.getPrimaryClip().getItemAt(0).getText()),"switching apps preserves short paste window");
            try{previous.wif();throw new AssertionError("closed WIF still accessible");}catch(IllegalStateException expected){checks++;}
            main(()->callActivityOnResume(activity));
            main(()->activity.onBackPressed());importBackup(cg1);
            yes(!has(root(),"其他收款地址"),"CG1 retains one public address");
            click("展示地址");addressQr(publicAddresses.nativeSegwit,false);dismiss("qrDialog");
            input("test fixture password only");click("解密冷钱包");until(()->"revealed".equals(get("screen")),20000);
            yes(has(root(),"其他收款地址"),"CG1 shows four derived addresses after unlock");
            click("显示");TextView oldSecret=(TextView)get("secretView");WalletCrypto.Unlocked homeKey=(WalletCrypto.Unlocked)get("unlocked");
            Field privateField=WalletCrypto.Unlocked.class.getDeclaredField("privateKey");privateField.setAccessible(true);byte[] homeBytes=(byte[])privateField.get(homeKey);
            click("隐藏私钥，返回首页");yes(clipboard.getPrimaryClip()==null,"home removes owned private clipboard");
            yes(java.util.Arrays.equals(homeBytes,new byte[32])&&privateField.get(homeKey)==null,"home wipes live private-key storage");
            yes(oldSecret.getText().toString().startsWith("•"),"home clears the detached private text view");
            yes(get("incoming")==null&&get("unlocked")==null&&get("exportData")==null,"home removes read and export state");
            nfcSheets(cg2);
            CreationSession session=new CreationSession(cg2);session.markCardWritten(new byte[]{1,2,3,4});
            main(()->{set("session",session);set("screen","done");invoke("render");});
            yes(!session.canFinish(),"completion awaits file backup");capture("04-backup-required");
            click("展示地址");addressQr(publicAddresses.nativeSegwit,true);
            clickDialog("qrDialog","复制地址");yes(((Dialog)get("qrDialog")).isShowing(),"copy keeps the new QR sheet open");dismiss("qrDialog");
            yes(clipboard.getPrimaryClip()!=null&&publicAddresses.nativeSegwit.contentEquals(clipboard.getPrimaryClip().getItemAt(0).getText()),"address QR copy uses the exact public address");
            click("写入另一张 NFC 卡");yes(has(dialogRoot("sheet"),"先保存备份，再继续写卡"),"copy button active and explains prerequisite");dismiss("sheet");
            until(()->activity.hasWindowFocus(),5000);sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);until(()->get("sheet")!=null&&((Dialog)get("sheet")).isShowing()&&has(dialogRoot("sheet"),"返回前，请先保存备份"),5000);yes(has(dialogRoot("sheet"),"返回前，请先保存备份"),"back cannot bypass mandatory backup");dismiss("sheet");
            main(()->activity.onActivityResult(22,Activity.RESULT_CANCELED,null));yes(!session.backupSaved(),"cancelled save does not satisfy backup");
            Bundle beforeRotation=new Bundle();main(()->activity.onSaveInstanceState(beforeRotation));yes(beforeRotation.isEmpty(),"unfinished wallet is not serialized");
            ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);
            main(()->activity.recreate());
            Activity replacement=waitForMonitorWithTimeout(monitor,10000);removeMonitor(monitor);
            if(replacement==null)throw new AssertionError("Activity recreation timeout");activity=(MainActivity)replacement;waitForIdleSync();
            CreationSession restored=(CreationSession)get("session");
            yes("done".equals(get("screen"))&&restored.encrypted.equals(cg2)&&!restored.backupSaved()&&restored.cardCount()==1,"memory-only recreation retains ciphertext and backup requirement");
            android.content.ContentValues values=new android.content.ContentValues();
            values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,"bitnfc-public-fixture-"+System.nanoTime()+".txt");
            values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"text/plain");
            android.net.Uri document=getTargetContext().getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
            if(document==null)throw new AssertionError("Document provider unavailable");
            try{
                main(()->{set("exportData",cg2);set("exportAction","copy");activity.onActivityResult(22,Activity.RESULT_OK,new Intent().setData(document));});
                until(()->"write".equals(get("screen")),15000);
                yes(restored.backupSaved(),"actual document closed and read back before backup is marked saved");
                try(InputStream in=getTargetContext().getContentResolver().openInputStream(document)){
                    yes(cg2.equals(new String(BackupFiles.readBounded(in),StandardCharsets.US_ASCII)),"document contains exact encrypted payload");
                }
            }finally{getTargetContext().getContentResolver().delete(document,null,null);}
            yes("write".equals(get("screen"))&&(Boolean)get("copyWriting"),"second card enters write flow");yes(((CreationSession)get("session")).encrypted.equals(cg2),"second card uses exact same encrypted wallet");capture("05-another-card");
            click("展示地址");addressQr(publicAddresses.nativeSegwit,true);dismiss("qrDialog");
            until(()->activity.hasWindowFocus(),5000);sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);until(()->"done".equals(get("screen")),5000);yes("done".equals(get("screen")),"cancel second card returns to completion");
            main(()->invoke("backupSheet",new Class<?>[]{String.class},new Object[]{null}));
            yes(!hasAction(dialogRoot("sheet"),"复制加密备份文本")&&hasAction(dialogRoot("sheet"),new ApprovedCopy(activity).ui("saveFile")),"backup offers verified file saving without encrypted-text copying");dismiss("sheet");
            click("展示地址");clickDialog("qrDialog","复制地址");dismiss("qrDialog");yes(clipboard.getPrimaryClip()!=null&&publicAddresses.nativeSegwit.contentEquals(clipboard.getPrimaryClip().getItemAt(0).getText()),"address copy is confirmed by read-back");
            main(()->{set("candidate",new NfcCards.Candidate(new byte[]{9,8},new byte[]{7,6,5},512));set("exportData",cg2);set("exportAction","copy");set("afterResume",(Runnable)()->{throw new AssertionError("old callback survived");});});
            NfcCards.Candidate oldCandidate=(NfcCards.Candidate)get("candidate");
            click("完成，返回首页");yes(get("session")==null&&"home".equals(get("screen")),"saved completion returns home");
            yes(get("candidate")==null&&java.util.Arrays.equals(oldCandidate.previous,new byte[3])&&java.util.Arrays.equals(oldCandidate.uid,new byte[2]),"home wipes checked card buffers");
            yes(get("incoming")==null&&get("exportData")==null&&get("exportAction")==null&&get("afterResume")==null,"home removes all flow references and deferred actions");
            yes(clipboard.getPrimaryClip()==null,"home removes owned address clipboard");
            Bundle finalState=new Bundle();finalState.putString("session",cg2);main(()->activity.onSaveInstanceState(finalState));yes(finalState.isEmpty(),"state serialization clears legacy keys too");
            main(()->clipboard.setPrimaryClip(ClipData.newPlainText(null,"unrelated public sentinel")));main(()->invoke("render"));
            yes(clipboard.getPrimaryClip()!=null&&"unrelated public sentinel".contentEquals(clipboard.getPrimaryClip().getItemAt(0).getText()),"cleanup preserves an unrelated clipboard with null label");main(clipboard::clearPrimaryClip);
            result.putString("stream","PASS: "+checks+" device assertions; public-fixture UI/NDEF/lifecycle/backup/multi-card flows.\n");finish(Activity.RESULT_OK,result);
        }catch(Throwable ex){result.putString("stream","FAIL after "+checks+" assertions: "+ex+"\n"+android.util.Log.getStackTraceString(ex));finish(Activity.RESULT_CANCELED,result);}
    }
    private void uiAlignment()throws Exception{
        View chip=null;for(View v:views(root()))if("language-chip".equals(v.getTag()))chip=v;
        yes(chip instanceof Button,"home uses the language capsule");android.graphics.drawable.Drawable[] icons=((Button)chip).getCompoundDrawablesRelative();yes(icons[0]!=null&&icons[2]!=null,"language capsule has globe and chevron");capture("aligned-home");
        click("语言 / Language");yes(has(dialogRoot("sheet"),"当前显示：中文")&&hasAction(dialogRoot("sheet"),"完成")&&hasAction(dialogRoot("sheet"),"关闭"),"language picker has current-language text, completion and close");
        int selected=0;for(View v:views(dialogRoot("sheet")))if("option-selected".equals(v.getTag()))selected++;yes(selected==1,"exactly one selected language row");capture("aligned-language");
        ActivityMonitor enMonitor=addMonitor(MainActivity.class.getName(),null,false);clickDialog("English");Activity en=waitForMonitorWithTimeout(enMonitor,10000);removeMonitor(enMonitor);yes(en instanceof MainActivity,"language option recreates localized activity");activity=(MainActivity)en;waitForIdleSync();yes(((Dialog)get("sheet")).isShowing()&&hasAction(root(),new ApprovedCopy(activity).ui("make")),"language picker remains open with translated page");
        ActivityMonitor zhMonitor=addMonitor(MainActivity.class.getName(),null,false);clickDialog("中文");Activity zh=waitForMonitorWithTimeout(zhMonitor,10000);removeMonitor(zhMonitor);yes(zh instanceof MainActivity,"Chinese selection applies immediately");activity=(MainActivity)zh;waitForIdleSync();clickDialog("完成");
        click("使用说明");ApprovedCopy c=new ApprovedCopy(activity);yes(has(dialogRoot("sheet"),c.ui("publicInfo"))&&hasAction(dialogRoot("sheet"),c.ui("securityEntry"))&&!hasPrefix(dialogRoot("sheet"),"BitTap · BTC mainnet"),"About matches approved grouping without technical footer");capture("aligned-about");clickDialog(c.ui("securityEntry"));
        yes("security".equals(get("screen"))&&hasAction(root(),c.ui("backSecurity"))&&!hasAction(root(),"使用说明"),"security page uses dedicated navigation");java.util.List<View> heads=new ArrayList<>();for(View v:views(root()))if("accordion-header".equals(v.getTag()))heads.add(v);yes(heads.size()==9,"security has nine disclosure groups");
        yes(((ViewGroup)heads.get(0).getParent()).getChildAt(1).getVisibility()==View.VISIBLE&&((ViewGroup)heads.get(8).getParent()).getChildAt(1).getVisibility()==View.GONE,"only first disclosure initially open");capture("aligned-security");
        main(()->heads.get(8).performClick());yes(((ViewGroup)heads.get(8).getParent()).getChildAt(1).getVisibility()==View.VISIBLE,"wallet comparison opens as disclosure");main(()->((AppUi)get("ui")).scroll.fullScroll(View.FOCUS_DOWN));waitForIdleSync();capture("aligned-security-comparison");click(c.ui("backSecurity"));yes("home".equals(get("screen"))&&((Dialog)get("sheet")).isShowing()&&has(dialogRoot("sheet"),c.ui("about")),"security returns to About sheet");dismiss("sheet");
        click("制作冷钱包");clickDialog("同意并继续");View cameraCard=null;TextView count=null;for(View v:views(root())){if("camera-mode-card".equals(v.getTag()))cameraCard=v;if("note-count".equals(v.getTag()))count=(TextView)v;}
        yes(cameraCard!=null&&cameraCard.getBackground()!=null&&count!=null&&count.getText().toString().endsWith("0/16"),"setup has framed camera mode and note counter");capture("aligned-setup-standard");
        final TextView counter=count;main(()->{for(View v:views(root()))if(v instanceof EditText&&c.ui("remark").contentEquals(v.getContentDescription()))((EditText)v).setText("第一张");});yes(counter.getText().toString().endsWith("3/16"),"note count follows Unicode input");
        main(()->{for(View v:views(root()))if(v instanceof Switch){((Switch)v).setChecked(true);return;}});yes(has(root(),c.ui("captureBeforePassword"))&&has(root(),c.ui("passwordNote")),"enhanced setup retains both designed notice cards");yes(!hasAction(root(),"生成冷钱包")&&hasAction(root(),c.ui("photoGenerate")),"enhanced setup requests capture before passwords");capture("aligned-setup-enhanced");
        click(c.ui("principle"));yes(has(dialogRoot("sheet"),c.ui("principle1"))&&has(dialogRoot("sheet"),c.ui("principleNote")),"camera principle uses approved three steps and caution");capture("aligned-camera-principle");dismiss("sheet");main(()->activity.onBackPressed());
    }
    private void cameraProbe()throws Exception{
        Texts.select(getTargetContext(),"zh");activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));waitForIdleSync();
        main(()->{set("screen","setup");set("enhanced",true);set("setupNote","相机测试1");invoke("render");});
        if(activity.checkSelfPermission(android.Manifest.permission.CAMERA)!=android.content.pm.PackageManager.PERMISSION_GRANTED){
            main(()->invoke("requestCamera"));yes(has(dialogRoot("sheet"),new ApprovedCopy(activity).ui("cameraPermission")),"camera permission explanation precedes Android permission");dismiss("sheet");
            try(android.os.ParcelFileDescriptor fd=getUiAutomation().executeShellCommand("pm grant io.coingift.android android.permission.CAMERA");InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}
        }
        main(()->invoke("startCamera"));CameraEntropy captured=null;
        for(int attempt=0;attempt<4;attempt++){
            until(()->get("camera")!=null||get("sheet")!=null,10000);yes(get("camera")!=null,"Camera2 opens in app");
            until(()->{CameraCapture c=(CameraCapture)get("camera");if(c==null)return true;try{Field f=CameraCapture.class.getDeclaredField("entropy");f.setAccessible(true);CameraEntropy e=(CameraEntropy)f.get(c);return e!=null&&e.count()==32;}catch(Exception ex){throw new AssertionError(ex);}},60000);
            CameraCapture capture=(CameraCapture)get("camera");yes(capture!=null,"32 distinct emulated frames collected without capture error");Thread.sleep(1100+attempt*370);capture.capture();until(()->get("cameraEntropy")!=null||get("camera")==null,15000);
            captured=(CameraEntropy)get("cameraEntropy");if(captured!=null)break;
            Field stage=CameraCapture.class.getDeclaredField("failureStage");stage.setAccessible(true);yes("repeated-final-image".equals(stage.get(capture))&&get("session")==null&&get("cameraPhoto")==null,"synthetic repeated image fails closed without wallet or photo retention");
            if(attempt<3)clickDialog(new ApprovedCopy(activity).ui("retry"));
        }
        yes(captured!=null&&captured.ready()&&"review".equals(get("screen")),"final still timestamp matches and capture is ready after bounded explicit retries");Bitmap collectedPhoto=(Bitmap)get("cameraPhoto");yes(collectedPhoto!=null&&!collectedPhoto.isRecycled(),"review shows the matched final photo from memory");capture("next-camera-ready");click(new ApprovedCopy(activity).ui("useCapture"));yes(get("cameraPhoto")==null&&collectedPhoto.isRecycled(),"accepting capture clears displayed photo before password entry");
        main(()->{for(View v:views(root()))if(v instanceof EditText&&(((EditText)v).getInputType()&0x80)!=0)((EditText)v).setText("test fixture password only");});click("生成冷钱包");until(()->"write".equals(get("screen"))||!(Boolean)get("busy"),20000);
        yes("write".equals(get("screen")),"captured Camera2 input completes real encrypted creation");CreationSession made=(CreationSession)get("session");yes(new WalletCrypto().inspect(made.encrypted).version==3&&made.note.equals("相机测试1"),"camera-created wallet contains authenticated CG3 note");yes(!captured.ready(),"camera secret consumed and closed");
        main(()->{set("screen","home");invoke("render");});yes(get("session")==null&&get("cameraEntropy")==null,"camera test leaves no active wallet");
    }
    private void nextUi(String cg3)throws Exception{
        yes(NfcCards.parse(NfcCards.message(cg3)).equals(cg3),"CG3 NDEF roundtrip");
        yes(NfcCards.message(cg3).toByteArray().length<=504,"CG3 sample capacity includes MIME framing");
        for(String lang:new String[]{"en","ja","ko","zh"}){
            ActivityMonitor monitor=addMonitor(MainActivity.class.getName(),null,false);main(()->{Texts.select(activity,lang);activity.recreate();});Activity replacement=waitForMonitorWithTimeout(monitor,10000);removeMonitor(monitor);if(replacement==null)throw new AssertionError("Language recreation timeout");activity=(MainActivity)replacement;waitForIdleSync();
            ApprovedCopy approved=new ApprovedCopy(activity);yes(hasAction(root(),approved.ui("make")),"home language "+lang);
            click(approved.ui("make"));yes("home".equals(get("screen")),"entry does not proceed without consent "+lang);yes(has(dialogRoot("sheet"),approved.ui("disclaimer")),"translated disclaimer "+lang);
            clickDialog(approved.ui("readFullDisclaimer"));yes("legal".equals(get("screen")),"offline full terms "+lang);main(()->activity.onBackPressed());yes("home".equals(get("screen"))&&((Dialog)get("sheet")).isShowing(),"terms return still requires consent "+lang);
            clickDialog(approved.ui("disagree"));yes("home".equals(get("screen")),"disagree stays home "+lang);
            click(approved.ui("read"));yes(((Dialog)get("sheet")).isShowing(),"each read entry asks again "+lang);clickDialog(approved.ui("disagree"));
            capture("next-language-"+lang);
        }
        click("制作冷钱包");clickDialog("同意并继续");yes(hasAction(root(),"查看支持卡片"),"card requirements before generation");click("查看支持卡片");yes(has(dialogRoot("sheet"),"优先选择 NTAG216"),"NFC help shows recommended chip");dismiss("sheet");
        EditText remark=null;for(View v:views(root()))if(v instanceof EditText&&"钱包备注".contentEquals(v.getContentDescription()))remark=(EditText)v;
        if(remark==null)throw new AssertionError("Note input missing");final EditText note=remark;main(()->note.setText("テスト1"));yes("テスト1".equals(get("setupNote")),"note kept in active memory only");
        CameraEntropy entropy=new CameraEntropy();java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-256");for(int i=0;i<32;i++)entropy.addFrame(hash.digest(("public-frame-"+i).getBytes(StandardCharsets.US_ASCII)));entropy.finish(hash.digest("public-photo".getBytes(StandardCharsets.US_ASCII)));
        main(()->{set("cameraEntropy",entropy);set("enhanced",true);callActivityOnPause(activity);callActivityOnStop(activity);});yes(!entropy.ready()&&get("cameraEntropy")==null,"background clears capture secret and digests");main(()->callActivityOnResume(activity));
        main(()->{set("enhanced",false);set("screen","home");invoke("render");});
        main(()->invoke("acceptIncoming",new Class<?>[]{String.class},new Object[]{cg3}));yes(has(root(),"03 - 長期储蓄"),"CG3 note readable before password");yes(get("unlocked")==null,"public note never unlocks key");
        input("test fixture password only");click("解密冷钱包");until(()->"revealed".equals(get("screen")),20000);yes(((WalletCrypto.Unlocked)get("unlocked")).note.equals("03 - 長期储蓄"),"decryption authenticates note");
        click("如何转出 BTC");yes(has(dialogRoot("sheet"),"OKX Web3 Wallet · web3.okx.com"),"current WIF help includes reported OKX compatibility");dismiss("sheet");
        click("展示地址");yes(hasAction(dialogRoot("qrDialog"),"查看链上余额"),"explorer entry belongs to address QR");dismiss("qrDialog");click("私钥二维码");yes(!hasAction(dialogRoot("qrDialog"),"查看链上余额"),"private QR has no explorer entry");dismiss("qrDialog");click("隐藏私钥，返回首页");
        CreationSession owner=new CreationSession(cg3);main(()->{set("session",owner);set("screen","write");invoke("render");});yes(!(Boolean)get("lockAfterWrite"),"permanent locking defaults off");
        main(()->{for(View v:views(root()))if(v instanceof Switch&&"写入后永久只读".contentEquals(((Switch)v).getText())){((Switch)v).setChecked(true);return;}throw new AssertionError("Lock switch absent");});yes((Boolean)get("lockAfterWrite"),"lock option explicit opt-in");capture("next-write-readonly");
        main(()->{owner.markCardWritten(new byte[]{9,7});set("lockStatus","unprotected");set("screen","done");invoke("render");invoke("confirmReadOnly");});yes(has(dialogRoot("sheet"),"永久锁定这张卡片？"),"irreversible confirmation separate from wallet write");capture("next-lock-confirmation");clickDialog("暂不锁定，继续备份");yes("unprotected".equals(get("lockStatus")),"skipping does not claim read-only");dismiss("sheet");
        main(()->{set("lockStatus","unknown");invoke("render");});yes(has(root(),new ApprovedCopy(activity).section("lockCopy").optString("unknown")),"uncertain lock visibly distinct from success");
        owner.markBackupSaved(cg3);main(()->invoke("anotherCard"));yes(!(Boolean)get("lockAfterWrite")&&"".equals(get("lockStatus")),"another card resets lock choice and result");
        main(()->{set("screen","home");invoke("render");});yes(get("session")==null&&get("cameraEntropy")==null&&"".equals(get("setupNote")),"home clears wallet, capture and note");
    }
    private void ndef(String cg2,String cg1)throws Exception{
        yes(NfcCards.parse(NfcCards.message(cg2)).equals(cg2),"new MIME CG2 roundtrip");
        yes(NfcCards.message(cg2).toByteArray().length<=504,"public fixture fits 504-byte tag capacity");
        NdefRecord legacy=NdefRecord.createMime("application/vnd.coingift.card",cg1.getBytes(StandardCharsets.US_ASCII));
        yes(NfcCards.parse(new NdefMessage(legacy)).equals(cg1),"legacy MIME accepted");
        yes(NfcCards.parse(new NdefMessage(NdefRecord.createTextRecord("en",cg2))).equals(cg2),"text CG2 accepted");
        try{NfcCards.parse(new NdefMessage(new NdefRecord[]{legacy,NdefRecord.createTextRecord("en",cg2)}));throw new AssertionError("multiple keys accepted");}catch(IllegalArgumentException expected){checks++;}
        try{NfcCards.parse(new NdefMessage(NdefRecord.createUri("https://example.com")));throw new AssertionError("URL accepted as key");}catch(IllegalArgumentException expected){checks++;}
    }
    private void importBackup(String data)throws Exception{
        android.content.ContentValues values=new android.content.ContentValues();
        values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,"bitnfc-import-public-fixture-"+System.nanoTime()+".txt");
        values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"text/plain");
        android.net.Uri file=getTargetContext().getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
        if(file==null)throw new IOException("Fixture document unavailable");
        try{
            try(OutputStream out=getTargetContext().getContentResolver().openOutputStream(file,"wt")){if(out==null)throw new IOException();out.write(data.getBytes(StandardCharsets.US_ASCII));}
            main(()->activity.onActivityResult(21,Activity.RESULT_OK,new Intent().setData(file)));
            until(()->"unlock".equals(get("screen")),10000);
        }finally{getTargetContext().getContentResolver().delete(file,null,null);}
    }
    private void addressWrap(String address){
        final TextView[] value={null};main(()->{for(View v:views(root()))if(v instanceof TextView&&address.contentEquals(((TextView)v).getText())){value[0]=(TextView)v;return;}throw new AssertionError("Address missing");});
        ViewGroup.LayoutParams original=value[0].getLayoutParams();int width=Math.round(220*activity.getResources().getDisplayMetrics().density);
        main(()->value[0].setLayoutParams(new LinearLayout.LayoutParams(width,ViewGroup.LayoutParams.WRAP_CONTENT)));waitForIdleSync();
        android.text.Layout layout=value[0].getLayout();
        yes(layout!=null&&layout.getLineCount()>1,"narrow address cards wrap instead of shrinking to a single line");
        yes(layout.getLineEnd(layout.getLineCount()-1)==address.length()&&layout.getEllipsisCount(layout.getLineCount()-1)==0,"wrapped address preserves every character");
        main(()->value[0].setLayoutParams(original));waitForIdleSync();
    }
    private byte[] stageImageExport(boolean privateKey){
        IntentFilter filter=new IntentFilter(Intent.ACTION_CREATE_DOCUMENT);
        filter.addCategory(Intent.CATEGORY_OPENABLE);
        try{filter.addDataType("image/png");}catch(IntentFilter.MalformedMimeTypeException impossible){throw new AssertionError(impossible);}
        ActivityMonitor picker=addMonitor(filter,null,true);
        try{
            clickDialog("qrDialog","保存图片");
            if(privateKey){
                yes(get("qrSaveConfirm")!=null&&get("pendingQrImage")==null,"private image requires confirmation before encoding/export");
                yes(hasPrefix(dialogRoot("qrSaveConfirm"),"图片包含明文私钥"),"private export explicitly warns about plaintext");
                clickDialog("qrSaveConfirm","取消");yes(get("pendingQrImage")==null&&((Dialog)get("qrDialog")).isShowing(),"cancel private confirmation creates no export and preserves QR");
                clickDialog("qrDialog","保存图片");clickDialog("qrSaveConfirm","确认保存");
            }
            yes(picker.getHits()==1,"save requests the system document picker only after consent; intercepted="+picker.getHits()+", pending="+(get("pendingQrImage")!=null));
            byte[] png=(byte[])get("pendingQrImage");yes(png!=null&&png.length>8&&png[0]==(byte)0x89&&png[1]=='P',"pending export is an in-memory PNG");return png;
        }finally{removeMonitor(picker);}
    }
    private void publicImageExports(String address)throws Exception{
        yes((((Dialog)get("qrDialog")).getWindow().getAttributes().gravity&Gravity.BOTTOM)==Gravity.BOTTOM,"QR uses the approved bottom card layout");
        byte[] cancelled=stageImageExport(false);
        main(()->activity.onActivityResult(23,Activity.RESULT_CANCELED,null));
        yes(get("pendingQrImage")==null&&allZero(cancelled),"cancelled image export wipes pending bytes");
        byte[] failed=stageImageExport(false);
        main(()->activity.onActivityResult(23,Activity.RESULT_OK,new Intent().setData(android.net.Uri.parse("content://invalid.bitnfc.test/missing"))));
        until(()->!(Boolean)get("fileBusy"),5000);
        yes(allZero(failed)&&has(root(),"图片未确认保存成功，请重新保存并检查所选位置。"),"denied image destination gives failure and wipes bytes");
        byte[] expired=stageImageExport(false);main(()->((Runnable)get("expireQrExport")).run());
        yes(get("pendingQrImage")==null&&allZero(expired),"abandoned image save expires and clears bytes");
        byte[] png=stageImageExport(false);saveImageAndDecode(png,address);
        yes(get("unlocked")==null,"public PNG export does not unlock a private key");
    }
    private void addressImageProbe()throws Exception{
        BitcoinAddresses addresses=new WalletCrypto().inspect(asset("cg3-scalar-one.txt")).addresses;
        String[] values={addresses.nativeSegwit,addresses.legacy,addresses.nestedSegwit,addresses.taproot};
        String[] types={"SegWit","Legacy","P2SH","Taproot"};
        String[] languages={"zh","en","ja","ko"};
        String[] titles={"收款地址","Receiving address","受取アドレス","받는 주소"};
        String[] labels={"比特币地址","Bitcoin address","ビットコインアドレス","비트코인 주소"};
        File dir=getTargetContext().getExternalFilesDir("qa");if(dir==null)throw new IOException();dir.mkdirs();
        try{
            for(int l=0;l<languages.length;l++){
                Texts.select(getTargetContext(),languages[l]);Context local=Texts.wrap(getTargetContext());
                yes(local.getString(R.string.address_image_title).equals(titles[l])&&local.getString(R.string.address_image_label).equals(labels[l]),"export title and label follow selected app language "+languages[l]);
                yes(local.getString(R.string.address_image_tagline).equals("YOUR KEYS. YOUR BITCOIN."),"export tagline remains English in "+languages[l]);
                for(int i=0;i<values.length;i++){
                    yes(types[i].equals(AddressImageRenderer.addressType(values[i])),"export type matches validated address "+types[i]);
                    Bitmap image=AddressImageRenderer.render(local,values[i]);
                    try{
                        yes(values[i].equals(decode(image)),"full export scans exactly: "+languages[l]+" "+types[i]);
                        Bitmap reduced=Bitmap.createScaledBitmap(image,408,image.getHeight()/3,false);
                        try{yes(values[i].equals(decode(reduced)),"phone-size export scans exactly: "+languages[l]+" "+types[i]);}finally{reduced.recycle();}
                        try(OutputStream out=new FileOutputStream(new File(dir,"receive-"+languages[l]+"-"+types[i]+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}
                    }finally{image.eraseColor(android.graphics.Color.WHITE);image.recycle();}
                }
            }
            for(String invalid:new String[]{WIF,"CG3:abc","mipcBbFg9gMiCh81Kj8tqqdgoZub1ZJRfn"}){
                boolean rejected=false;try{AddressImageRenderer.render(Texts.wrap(getTargetContext()),invalid);}catch(IllegalArgumentException expected){rejected=true;}
                yes(rejected,"address artwork rejects non-mainnet-address payloads");
            }
            Texts.select(getTargetContext(),"system");
            yes(Texts.wrap(getTargetContext()).getResources().getConfiguration().getLocales().get(0).getLanguage().equals(Texts.language(getTargetContext())),"system selection uses the resolved app language");
            Texts.select(getTargetContext(),"zh");
            activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));waitForIdleSync();
            click("读取冷钱包");clickDialog("同意并继续");importBackup(asset("cg3-scalar-one.txt"));click("展示地址");
            publicImageExports(addresses.nativeSegwit);
            input("test fixture password only");click("解密冷钱包");until(()->"revealed".equals(get("screen")),20000);
            click("私钥二维码");privateImageExport();
            main(()->invoke("navigate",new Class<?>[]{String.class},new Object[]{"home"}));
        }finally{Texts.select(getTargetContext(),"zh");}
    }
    private void privateImageArtworkProbe()throws Exception{
        String[] languages={"zh","en","ja","ko"};
        String[] titles={"WIF 私钥","WIF private key","WIF 秘密鍵","WIF 개인 키"};
        String[] fixtures={WIF,"L44B5gGEpqEDRS9vVPz7QT35jcBG2r3CZwSwQ4fCewXAhAhqGVpP"};
        File dir=getTargetContext().getExternalFilesDir("qa");if(dir==null)throw new IOException();dir.mkdirs();
        try{
            for(int l=0;l<languages.length;l++){
                Texts.select(getTargetContext(),languages[l]);Context local=Texts.wrap(getTargetContext());
                yes(titles[l].equals(local.getString(R.string.private_image_title)),"private image follows app language "+languages[l]);
                yes("YOUR KEYS. YOUR BITCOIN.".equals(local.getString(R.string.address_image_tagline)),"private footer stays English in "+languages[l]);
                for(int f=0;f<fixtures.length;f++){
                    Bitmap image=PrivateImageRenderer.render(local,fixtures[f]);
                    try{
                        yes(image.getWidth()==1224&&image.getHeight()>image.getWidth(),"private export uses portrait artwork, not a raw QR");
                        yes(fixtures[f].equals(decode(image)),"private artwork decodes to exact WIF: "+languages[l]);
                        Bitmap reduced=Bitmap.createScaledBitmap(image,408,image.getHeight()/3,false);
                        try{yes(fixtures[f].equals(decode(reduced)),"phone-size private artwork decodes: "+languages[l]);}finally{reduced.recycle();}
                        if(f==0)try(OutputStream out=new FileOutputStream(new File(dir,"private-public-fixture-"+languages[l]+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}
                    }finally{image.eraseColor(android.graphics.Color.WHITE);image.recycle();}
                }
            }
            for(String invalid:new String[]{null,"CG3:abc","bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"}){
                boolean rejected=false;try{PrivateImageRenderer.render(Texts.wrap(getTargetContext()),invalid);}catch(IllegalArgumentException expected){rejected=true;}
                yes(rejected,"private artwork rejects non-WIF payloads");
            }
        }finally{Texts.select(getTargetContext(),"zh");}
    }
    private void privateImageExport()throws Exception{
        byte[] cancelled=stageImageExport(true);
        main(()->activity.onActivityResult(23,Activity.RESULT_CANCELED,null));
        yes(allZero(cancelled)&&get("pendingQrImage")==null&&!(Boolean)get("pendingQrPrivate"),"cancelled private artwork wipes pending bytes and sensitivity state");
        byte[] failed=stageImageExport(true);
        main(()->activity.onActivityResult(23,Activity.RESULT_OK,new Intent().setData(android.net.Uri.parse("content://invalid.bitnfc.test/missing"))));
        until(()->!(Boolean)get("fileBusy"),5000);
        yes(allZero(failed)&&get("pendingQrImage")==null,"failed private artwork save wipes bytes");
        byte[] expired=stageImageExport(true);main(()->((Runnable)get("expireQrExport")).run());
        yes(allZero(expired)&&get("pendingQrImage")==null&&!(Boolean)get("pendingQrPrivate"),"expired private artwork wipes bytes and sensitivity state");
        byte[] png=stageImageExport(true);
        Bitmap image=android.graphics.BitmapFactory.decodeByteArray(png,0,png.length);
        try{yes(image!=null&&image.getWidth()==1224&&image.getHeight()>image.getWidth(),"explicit private save uses the designed artwork");}finally{if(image!=null)image.recycle();}
        saveImageAndDecode(png,WIF);
        yes(get("unlocked")==null&&"unlock".equals(get("screen")),"private export picker transition clears decrypted key and returns locked");
    }
    private void saveImageAndDecode(byte[] png,String expected)throws Exception{
        Bitmap oldBitmap=(Bitmap)get("qrBitmap");
        main(()->{callActivityOnPause(activity);callActivityOnStop(activity);});
        yes(oldBitmap.isRecycled()&&get("qrBitmap")==null,"picker transition erases displayed QR pixels");
        yes(!allZero(png),"only the explicitly requested image survives the picker transition");
        Bundle state=new Bundle();main(()->activity.onSaveInstanceState(state));yes(state.isEmpty(),"pending PNG is never serialized into activity state");
        android.content.ContentValues values=new android.content.ContentValues();values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,"bitnfc-public-fixture-"+System.nanoTime()+".png");values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"image/png");
        android.net.Uri file=getTargetContext().getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);if(file==null)throw new IOException();
        try{
            main(()->activity.onActivityResult(23,Activity.RESULT_OK,new Intent().setData(file)));until(()->!(Boolean)get("fileBusy"),10000);
            yes(get("pendingQrImage")==null&&allZero(png),"saved image bytes are wiped after close and read-back");
            Bitmap image;try(InputStream in=getTargetContext().getContentResolver().openInputStream(file)){image=android.graphics.BitmapFactory.decodeStream(in);}
            if(image==null)throw new AssertionError("Saved PNG invalid");
            try{yes(expected.equals(decode(image)),"saved PNG decodes to the exact original address or WIF");}finally{image.recycle();}
        }finally{getTargetContext().getContentResolver().delete(file,null,null);}
        main(()->callActivityOnResume(activity));waitForIdleSync();
    }
    private boolean allZero(byte[] bytes){for(byte b:bytes)if(b!=0)return false;return true;}
    private void inputUiProbe()throws Exception{
        String[] languages={"zh","en","ja","ko"};
        try{
            for(String language:languages){
                Texts.select(getTargetContext(),language);
                activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));waitForIdleSync();
                click(new ApprovedCopy(activity).ui("make"));clickDialog(new ApprovedCopy(activity).ui("agree"));
                List<EditText> fields=new ArrayList<>();for(View v:views(root()))if(v instanceof EditText)fields.add((EditText)v);
                yes(fields.size()==3,"setup has note and two password inputs");
                String[] values={"keyboard-note-01","keyboard-test-only","keyboard-test-only"};
                String[] names={"note","password","confirm"};
                for(int i=0;i<fields.size();i++)keyboardInput(fields.get(i),values[i],"keyboard-"+language+"-"+names[i]);
                main(()->activity.onBackPressed());yes("home".equals(get("screen")),"setup returns home after keyboard closes");
                for(EditText field:fields)yes(field.length()==0,"leaving setup clears detached input");
                click(new ApprovedCopy(activity).ui("read"));clickDialog(new ApprovedCopy(activity).ui("agree"));importBackup(asset("cg3-scalar-one.txt"));
                EditText password=firstInput();keyboardInput(password,"test fixture password only","keyboard-"+language+"-unlock");
                click(Texts.tr(activity,"解密冷钱包"));until(()->"revealed".equals(get("screen")),20000);
                Button display=button(Texts.tr(activity,"展示地址")),help=button(Texts.tr(activity,"如何转出 BTC"));
                int[] a=new int[2],b=new int[2];main(()->{display.getLocationOnScreen(a);help.getLocationOnScreen(b);});
                yes(Math.abs(a[0]+display.getWidth()-b[0]-help.getWidth())<=2,"transfer help and address action share right edge in "+language);
                yes(!hasAction(root(),Texts.tr(activity,"复制 WIF 私钥")),"recovery page keeps direct copy removed");
                AppUi appUi=(AppUi)get("ui");main(()->appUi.scroll.fullScroll(View.FOCUS_DOWN));waitForIdleSync();capture("recovery-actions-"+language);
                click(Texts.tr(activity,"隐藏私钥，返回首页"));
                yes("home".equals(get("screen"))&&get("unlocked")==null&&get("copyConfirm")==null,"home button clears decrypted session");
                main(activity::finish);waitForIdleSync();
            }
        }finally{Texts.select(getTargetContext(),"zh");}
    }
    private void keyboardInput(EditText input,String value,String screenshot)throws Exception{
        android.view.inputmethod.InputMethodManager ime=(android.view.inputmethod.InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        main(()->{input.requestFocus();input.setText("");ime.showSoftInput(input,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);});
        until(()->root().getRootWindowInsets()!=null&&root().getRootWindowInsets().isVisible(WindowInsets.Type.ime()),10000);
        sendStringSync(value);waitForIdleSync();
        until(()->{
            View field=(View)input.getParent();int[] location=new int[2];field.getLocationOnScreen(location);
            int[] origin=new int[2];root().getLocationOnScreen(origin);
            int keyboardTop=origin[1]+root().getHeight()-root().getRootWindowInsets().getInsets(WindowInsets.Type.ime()).bottom;
            return location[1]>=origin[1]&&location[1]+field.getHeight()<=keyboardTop-8;
        },5000);
        yes(value.contentEquals(input.getText())&&input.getSelectionEnd()==input.length(),"typed text and caret preserved with IME open");
        View field=(View)input.getParent();android.graphics.Rect visible=new android.graphics.Rect();
        yes(field.getGlobalVisibleRect(visible)&&visible.height()==field.getHeight(),"entire focused input is visible above keyboard");
        capture(screenshot);
        main(()->ime.hideSoftInputFromWindow(input.getWindowToken(),0));
        until(()->!root().getRootWindowInsets().isVisible(WindowInsets.Type.ime()),10000);
        yes(value.contentEquals(input.getText()),"keyboard dismissal preserves input");
    }
    private void copyConfirmationProbe()throws Exception{
        String[] languages={"zh","en","ja","ko"};
        String[] titles={"复制 WIF 私钥？","Copy WIF private key?","WIF 秘密鍵をコピーしますか？","WIF 개인 키를 복사할까요?"};
        ClipboardManager clipboard=(ClipboardManager)getTargetContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardDenials();
        try{
            for(int l=0;l<languages.length;l++){
                Texts.select(getTargetContext(),languages[l]);
                activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));waitForIdleSync();
                click(new ApprovedCopy(activity).ui("read"));clickDialog(new ApprovedCopy(activity).ui("agree"));
                importBackup(asset("cg3-scalar-one.txt"));input("test fixture password only");click(Texts.tr(activity,"解密冷钱包"));until(()->"revealed".equals(get("screen")),20000);
                String copyLabel=new ApprovedCopy(activity).ui("copyWif");
                yes(!hasAction(root(),copyLabel)&&!hasAction(root(),Texts.tr(activity,"复制 WIF 私钥")),"removed recovery copy button in "+languages[l]);
                if(l==0)capture("recovery-without-copy");
                main(()->clipboard.setPrimaryClip(ClipData.newPlainText("public sentinel","public sentinel")));
                click(Texts.tr(activity,"私钥二维码"));clickDialog("qrDialog",copyLabel);
                yes(has(dialogRoot("copyConfirm"),titles[l]),"localized confirmation title "+languages[l]);
                yes("public sentinel".contentEquals(clipboard.getPrimaryClip().getItemAt(0).getText())&&get("clipboardMarker")==null,"opening confirmation exports nothing");
                yes(!hasPrefix(dialogRoot("copyConfirm"),WIF),"confirmation does not display private key");
                yes((((Dialog)get("copyConfirm")).getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"confirmation remains secure");
                if(copyVisualOnly)Thread.sleep(7000); // Let the OS clipboard preview disappear before visual capture.
                capture("copy-confirm-"+languages[l]);
                if(copyVisualOnly){
                    dismiss("copyConfirm");dismiss("qrDialog");click(Texts.tr(activity,"隐藏私钥，返回首页"));
                    main(activity::finish);waitForIdleSync();continue;
                }
                Button stale=dialogButton("copyConfirm",activity.getString(R.string.copy_confirm_action));
                clickDialog("copyConfirm",Texts.tr(activity,"取消"));
                yes(get("copyConfirm")==null&&((Dialog)get("qrDialog")).isShowing(),"cancel preserves private QR and closes only confirmation");
                main(stale::performClick);
                yes("public sentinel".contentEquals(clipboard.getPrimaryClip().getItemAt(0).getText()),"cancelled confirmation cannot copy via a stale click");
                clickDialog("qrDialog",copyLabel);
                until(()->((Dialog)get("copyConfirm")).getWindow().getDecorView().hasWindowFocus(),5000);sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);waitForIdleSync();
                yes(get("copyConfirm")==null&&"public sentinel".contentEquals(clipboard.getPrimaryClip().getItemAt(0).getText()),"system back dismisses without copying");
                clickDialog("qrDialog",copyLabel);
                clipboardWriteMode("ignore");
                try{
                    clickDialog("copyConfirm",activity.getString(R.string.copy_confirm_action));
                    yes(get("copyConfirm")!=null&&((Dialog)get("copyConfirm")).isShowing(),"denied write keeps confirmation open");
                    TextView message=(TextView)get("copyConfirmFeedback");
                    yes(message.getVisibility()==View.VISIBLE&&message.getText().toString().equals(Texts.tr(activity,"未能确认复制成功，请检查系统剪贴板权限后重试。")),"denied write shows localized failure");
                    yes("public sentinel".contentEquals(clipboard.getPrimaryClip().getItemAt(0).getText()),"denied write does not claim a successful private copy");
                    if(l==0)capture("copy-confirm-denied");
                }finally{clipboardWriteMode("allow");}
                Button beforePause=dialogButton("copyConfirm",activity.getString(R.string.copy_confirm_action));
                WalletCrypto.Unlocked oldKey=(WalletCrypto.Unlocked)get("unlocked");
                main(()->{callActivityOnPause(activity);callActivityOnStop(activity);});
                yes(get("copyConfirm")==null&&get("qrDialog")==null&&get("unlocked")==null,"background clears confirmation, QR and decrypted key");
                main(beforePause::performClick);
                yes("public sentinel".contentEquals(clipboard.getPrimaryClip().getItemAt(0).getText()),"backgrounded confirmation cannot export");
                try{oldKey.wif();throw new AssertionError("key survived pause");}catch(IllegalStateException expected){checks++;}
                main(()->callActivityOnResume(activity));input("test fixture password only");click(Texts.tr(activity,"解密冷钱包"));until(()->"revealed".equals(get("screen")),20000);
                click(Texts.tr(activity,"私钥二维码"));clickDialog("qrDialog",copyLabel);
                main(beforePause::performClick);
                yes("public sentinel".contentEquals(clipboard.getPrimaryClip().getItemAt(0).getText()),"old consent cannot authorize newly decrypted wallet");
                clickDialog("copyConfirm",activity.getString(R.string.copy_confirm_action));
                ClipData copied=clipboard.getPrimaryClip();
                yes(copied!=null&&WIF.contentEquals(copied.getItemAt(0).getText())&&copied.getDescription().getExtras().getBoolean("android.content.extra.IS_SENSITIVE"),"confirmed copy is exact and sensitive");
                yes(get("copyConfirm")==null&&((Dialog)get("qrDialog")).isShowing(),"confirmed success returns to QR");
                dismiss("qrDialog");click(Texts.tr(activity,"隐藏私钥，返回首页"));
                yes(get("unlocked")==null&&get("copyConfirm")==null&&clipboard.getPrimaryClip()==null,"home clears private session and owned clipboard");
                main(activity::finish);waitForIdleSync();
            }
        }finally{clipboardWriteMode("allow");Texts.select(getTargetContext(),"zh");}
    }
    private void clipboardWriteMode(String mode)throws Exception{
        try(android.os.ParcelFileDescriptor fd=getUiAutomation().executeShellCommand("cmd appops set io.coingift.android WRITE_CLIPBOARD "+mode);
            InputStream in=new android.os.ParcelFileDescriptor.AutoCloseInputStream(fd)){
            byte[] output=new byte[512];while(in.read(output)!=-1){}java.util.Arrays.fill(output,(byte)0);
        }
    }
    private String decode(Bitmap bitmap)throws Exception{
        int[] pixels=new int[bitmap.getWidth()*bitmap.getHeight()];bitmap.getPixels(pixels,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
        try{return new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(bitmap.getWidth(),bitmap.getHeight(),pixels)))).getText();}
        finally{java.util.Arrays.fill(pixels,0);}
    }
    private void clipboardDenials(){
        ClipData expected=ClipData.newPlainText("unique-copy-marker","public fixture"),stale=ClipData.newPlainText("old-marker","old content");
        yes(VerifiedClipboard.writeAndVerify(expected,c->{},()->expected),"clipboard success requires exact read-back");
        yes(!VerifiedClipboard.writeAndVerify(expected,c->{},()->stale),"silent write denial is not success");
        yes(!VerifiedClipboard.writeAndVerify(expected,c->{},()->null),"unavailable read-back is not success");
        yes(!VerifiedClipboard.writeAndVerify(expected,c->{throw new SecurityException();},()->expected),"write permission exception is handled");
        yes(!VerifiedClipboard.writeAndVerify(expected,c->{},()->{throw new SecurityException();}),"read permission exception is handled");
        yes(!VerifiedClipboard.writeAndVerify(expected,c->{},()->ClipData.newPlainText("unique-copy-marker","different content")),"same marker with wrong content is not success");
        yes(!VerifiedClipboard.writeAndVerify(expected,c->{},()->ClipData.newPlainText("different-marker","public fixture")),"same content from an earlier copy is not success");
        ClipData multiple=ClipData.newPlainText("unique-copy-marker","public fixture");multiple.addItem(new ClipData.Item("extra"));
        yes(!VerifiedClipboard.writeAndVerify(expected,c->{},()->multiple),"unexpected multiple clipboard items are not success");
    }
    private void nfcSheets(String backup)throws Exception{
        // Exercise production presentation/lifecycle without claiming emulator NFC hardware I/O.
        Thread.sleep(3500); // Let the prior sensitive clipboard preview disappear before public UI captures.
        CreationSession owner=new CreationSession(backup);
        main(()->{set("session",owner);set("screen","write");invoke("render");});
        openNfcPrompt("CHECK");
        yes(has(dialogRoot("nfcDialog"),new ApprovedCopy(activity).ui("scanWrite"))&&has(dialogRoot("nfcDialog"),"●  等待识别卡片…"),"check-card sheet matches prototype waiting state");
        yes((((Dialog)get("nfcDialog")).getWindow().getAttributes().gravity&Gravity.BOTTOM)==Gravity.BOTTOM,"NFC prompt is a bottom sheet");
        yes(views(dialogRoot("nfcDialog")).stream().anyMatch(v->v instanceof WalletArtView),"NFC prompt includes the card illustration");
        yes((((Dialog)get("nfcDialog")).getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"NFC sheet remains secure");capture("09-check-card-sheet");
        clickDialog("nfcDialog","取消");until(()->get("nfcDialog")==null,5000);
        yes("NONE".equals(get("mode").toString())&&get("session")==owner&&"write".equals(get("screen")),"cancel stops scanning and preserves the current wallet");
        openNfcPrompt("CHECK");main(()->((NfcScanDialog)get("nfcDialog")).processing("正在检查卡片","正在读取，请保持卡片贴合…"));
        yes(has(dialogRoot("nfcDialog"),"正在检查卡片"),"check status updates on processing");main(()->invoke("cancelNfc"));
        main(()->{set("screen","read");invoke("render");});openNfcPrompt("READ");
        yes(has(dialogRoot("nfcDialog"),new ApprovedCopy(activity).ui("scanRead")),"reading uses the same visual sheet");capture("10-read-card-sheet");
        until(()->((Dialog)get("nfcDialog")).getWindow().getDecorView().hasWindowFocus(),5000);sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);
        until(()->get("nfcDialog")==null,5000);yes("NONE".equals(get("mode").toString())&&"read".equals(get("screen")),"system back cancels the waiting sheet without leaving the page");
        main(()->{set("screen","write");invoke("render");});openNfcPrompt("WRITE");
        yes(has(dialogRoot("nfcDialog"),new ApprovedCopy(activity).ui("writeArmed"))&&has(dialogRoot("nfcDialog"),"●  等待贴回同一张卡片…"),"write prompt keeps same-card guidance");
        main(()->((NfcScanDialog)get("nfcDialog")).processing("正在写入并核对","写入 → 读回 → 核对内容，请保持贴合…"));
        yes(has(dialogRoot("nfcDialog"),"正在写入并核对")&&!dialogButton("nfcDialog","取消").isEnabled(),"writing state disables cancellation");capture("11-writing-card-sheet");
        until(()->((Dialog)get("nfcDialog")).getWindow().getDecorView().hasWindowFocus(),5000);sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);waitForIdleSync();
        yes(get("nfcDialog")!=null&&((Dialog)get("nfcDialog")).isShowing(),"system back cannot dismiss active processing");
        main(()->{callActivityOnPause(activity);callActivityOnStop(activity);});
        yes(get("nfcDialog")==null&&"NONE".equals(get("mode").toString()),"background dismisses the NFC sheet and stops reader mode");
        yes(get("session")==owner&&owner.encrypted.equals(backup),"NFC prompt lifecycle does not alter encrypted wallet data");
        main(()->{callActivityOnResume(activity);set("session",null);set("screen","home");invoke("render");});
    }
    private void openNfcPrompt(String mode)throws Exception{
        Class<?> type=Class.forName("io.coingift.android.MainActivity$Mode");
        Object desired=java.util.Arrays.stream(type.getEnumConstants()).filter(v->mode.equals(v.toString())).findFirst().orElseThrow();
        main(()->invoke("showNfcPrompt",new Class<?>[]{type},new Object[]{desired}));waitForIdleSync();
    }
    private String asset(String name)throws Exception{try(InputStream in=getContext().getAssets().open(name)){return new String(BackupFiles.readBounded(in),StandardCharsets.US_ASCII).trim();}}
    private View root(){return activity.getWindow().getDecorView();}
    private View dialogRoot(String name){return ((Dialog)get(name)).getWindow().getDecorView();}
    private Object get(String name){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void set(String name,Object value){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void invoke(String name,Class<?>[] types,Object[] args){try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);m.invoke(activity,args);}catch(Exception ex){throw new RuntimeException(ex);}}
    private boolean hasPrefix(View root,String text){for(View v:views(root))if(v instanceof TextView&&((TextView)v).getText().toString().startsWith(text))return true;return false;}
    private Button button(String text){for(View v:views(root()))if(v instanceof Button&&(text.contentEquals(((Button)v).getText())||text.equals(v.getContentDescription())))return (Button)v;throw new AssertionError("Button missing");}
    private EditText firstInput(){for(View v:views(root()))if(v instanceof EditText&&(((EditText)v).getInputType()&android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD)!=0)return (EditText)v;throw new AssertionError("Input missing");}
    private Button dialogButton(String field,String text){for(View v:views(dialogRoot(field)))if(v instanceof Button&&(text.contentEquals(((Button)v).getText())||text.equals(v.getContentDescription())))return (Button)v;throw new AssertionError("Dialog button missing");}
    private void clickDialog(String text){clickDialog("sheet",text);}
    private void clickDialog(String field,String text){main(()->dialogButton(field,text).performClick());waitForIdleSync();}
    private void invoke(String name){try{java.lang.reflect.Method m=MainActivity.class.getDeclaredMethod(name);m.setAccessible(true);m.invoke(activity);}catch(Exception ex){throw new RuntimeException(ex);}}
    private List<View> views(View v){List<View> all=new ArrayList<>();all.add(v);if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)all.addAll(views(g.getChildAt(i)));}return all;}
    private boolean has(View v,String text){for(View item:views(v))if(item instanceof TextView&&text.contentEquals(((TextView)item).getText()))return true;return false;}
    private boolean hasAction(View root,String label){for(View v:views(root))if(v.isClickable()&&(label.equals(v.getContentDescription())||(v instanceof TextView&&label.contentEquals(((TextView)v).getText()))))return true;return false;}
    private void click(String text){main(()->{for(View v:views(root()))if(v.isClickable()&&((v instanceof Button&&text.contentEquals(((Button)v).getText()))||text.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))){v.performClick();return;}throw new AssertionError("Button missing: "+text);});waitForIdleSync();}
    private void addressQr(String address,boolean verified)throws Exception{
        Bitmap bitmap=(Bitmap)get("qrBitmap");int[] pixels=new int[bitmap.getWidth()*bitmap.getHeight()];bitmap.getPixels(pixels,0,bitmap.getWidth(),0,0,bitmap.getWidth(),bitmap.getHeight());
        String decoded=new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(bitmap.getWidth(),bitmap.getHeight(),pixels)))).getText();
        yes(address.equals(decoded),"displayed address QR contains only the exact public address");
        yes(has(dialogRoot("qrDialog"),address)&&has(dialogRoot("qrDialog"),new ApprovedCopy(activity).ui("qrUnverified"))!=verified
            &&has(dialogRoot("qrDialog"),"已与私钥核对，地址正确")==verified,"address QR shows full address and appropriate verification state");
        boolean centered=false;for(View v:views(dialogRoot("qrDialog")))if(v instanceof TextView&&address.contentEquals(((TextView)v).getText()))centered=(((TextView)v).getGravity()&Gravity.HORIZONTAL_GRAVITY_MASK)==Gravity.CENTER_HORIZONTAL;
        yes(centered&&hasAction(dialogRoot("qrDialog"),"复制地址")&&hasAction(dialogRoot("qrDialog"),"保存图片"),"QR address is centered and both actions are present");
        Button save=dialogButton("qrDialog","保存图片"),copy=dialogButton("qrDialog","复制地址");ViewGroup actions=(ViewGroup)save.getParent();
        yes(save.getTop()>=0&&copy.getTop()>=0&&save.getBottom()<=actions.getHeight()&&copy.getBottom()<=actions.getHeight(),"QR action buttons fit fully inside their row without clipped corners");
        yes((((Dialog)get("qrDialog")).getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"QR dialog retains secure-window protection");
    }
    private void input(String value){main(()->firstInput().setText(value));}
    private void dismiss(String name){main(()->((Dialog)get(name)).dismiss());waitForIdleSync();}
    private interface Condition{boolean ok();}
    private void until(Condition c,long timeout)throws Exception{long end=android.os.SystemClock.elapsedRealtime()+timeout;while(android.os.SystemClock.elapsedRealtime()<end){final boolean[] ok={false};main(()->ok[0]=c.ok());if(ok[0]){waitForIdleSync();return;}Thread.sleep(50);}throw new AssertionError("UI timeout");}
    private void main(Runnable action){
        final Throwable[] error={null};super.runOnMainSync(()->{try{action.run();}catch(Throwable ex){error[0]=ex;}});
        if(error[0]!=null)throw new AssertionError("UI operation failed",error[0]);
    }
    private void yes(boolean result,String why){if(!result)throw new AssertionError(why);checks++;}
    private void capture(String name)throws Exception{
        // Test-only: capture public fixture pages. Production always retains FLAG_SECURE.
        main(()->{
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
            for(String field:new String[]{"sheet","qrDialog","nfcDialog","copyConfirm"}){Dialog visible=(Dialog)get(field);if(visible!=null&&visible.isShowing())visible.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);}
        });waitForIdleSync();Thread.sleep(600);
        try{
            Bitmap image=getUiAutomation().takeScreenshot();if(image==null)throw new AssertionError("Screenshot unavailable");
            File dir=getTargetContext().getExternalFilesDir("qa");if(dir==null)throw new IOException();dir.mkdirs();
            try(OutputStream out=new FileOutputStream(new File(dir,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}finally{image.recycle();}
        }finally{main(()->{
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            for(String field:new String[]{"sheet","qrDialog","nfcDialog","copyConfirm"}){Dialog visible=(Dialog)get(field);if(visible!=null&&visible.isShowing())visible.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);}
        });}
    }
}
