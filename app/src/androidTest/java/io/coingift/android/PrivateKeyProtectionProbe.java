package io.coingift.android;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import io.coingift.core.BackupFiles;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.BooleanSupplier;

/** Public fixtures only. Pixel equivalence plus a separate-process, enabled accessibility service. */
final class PrivateKeyProtectionProbe {
    private static final String WIF="KwDiBf89QgGbjEhKnhXJuH7LrciVrZi3qYjgd9M7rFU73sVHnoWn";
    private static final Uri BRIDGE=Uri.parse("content://io.coingift.android.test.private-access");
    private final SmokeRunner host;
    private MainActivity activity;
    private int checks;
    private String backup;
    PrivateKeyProtectionProbe(SmokeRunner host){this.host=host;}
    int run()throws Exception{
        try(InputStream in=host.getContext().getAssets().open("cg3-scalar-one.txt")){
            backup=new String(BackupFiles.readBounded(in),StandardCharsets.US_ASCII).trim();
        }
        until(()->snapshot().getBoolean("connected"),10000);
        if(Build.VERSION.SDK_INT>=31)yes(snapshot().getBoolean("tool"),"probe uses a declared accessibility tool, including sensitive nodes");
        try{
            for(String language:new String[]{"zh","en","ja","ko"}){
                Texts.select(host.getTargetContext(),language);
                activity=(MainActivity)host.startActivitySync(new Intent(host.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));host.waitForIdleSync();
                unlock();pixelMetrics(language);resetProbe();
                main(()->invoke("showPrivateQr"));host.waitForIdleSync();
                PrivateKeyView value=(PrivateKeyView)get("secretView");
                yes(value!=null&&!value.isCleared(),"private key remains visibly drawn on explicit request");
                main(()->{
                    AccessibilityNodeInfo node=value.createAccessibilityNodeInfo();
                    try{yes(node.getText()==null&&!node.getContentDescription().toString().contains(WIF),"private node contains only the fixed localized description");}
                    finally{node.recycle();}
                    yes(value.getImportantForAutofill()==View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS&&!value.isSaveEnabled(),"private view excludes autofill and saved state");
                    if(Build.VERSION.SDK_INT>=30)yes(value.getImportantForContentCapture()==View.IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS,"private view excludes content capture");
                    if(Build.VERSION.SDK_INT>=34)yes(value.isAccessibilityDataSensitive()&&((ImageView)get("qrImage")).isAccessibilityDataSensitive(),"text drawing and private QR have sensitive accessibility flags");
                });
                assertNoLeak("private sheet");
                yes((((Dialog)get("qrDialog")).getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"private sheet retains screenshot protection");
                if(Build.VERSION.SDK_INT>=29){
                    click("qrDialog",new ApprovedCopy(activity).ui("copyWif"));assertNoLeak("copy confirmation");
                    click("copyConfirm",activity.getString(R.string.copy_confirm_action));
                    main(()->{
                        ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
                        yes(WIF.contentEquals(clipboard.getPrimaryClip().getItemAt(0).getText()),"existing explicit copy still exports the exact WIF");
                    });
                }else yes(!button((Dialog)get("qrDialog"),activity.getString(R.string.private_copy_unavailable)).isEnabled(),"Android 9 copy remains unavailable");
                click("qrDialog",Texts.tr(activity,"保存图片"));assertNoLeak("private image confirmation");
                click("qrSaveConfirm",Texts.tr(activity,"取消"));
                sameSheetAsPreviousTextView(language,value);
                // Positive control: the same enabled service must read the old TextView under FLAG_SECURE.
                until(()->snapshot().getBoolean("nodeLeak"),5000);
                yes(snapshot().getBoolean("nodeLeak"),"service detects the legacy plaintext TextView under FLAG_SECURE");
                main(()->invoke("dismissQr"));host.waitForIdleSync();resetProbe();
                main(()->invoke("showPrivateQr"));host.waitForIdleSync();assertNoLeak("reopened private sheet");
                PrivateKeyView detached=(PrivateKeyView)get("secretView");char[] owned=owned(detached);Bitmap qr=(Bitmap)get("qrBitmap");
                main(()->{host.callActivityOnPause(activity);host.callActivityOnStop(activity);});
                yes(detached.isCleared()&&Arrays.equals(owned,new char[owned.length])&&qr.isRecycled()&&get("unlocked")==null,"background wipes private characters, QR, and session");
                main(()->host.callActivityOnResume(activity));host.waitForIdleSync();main(activity::finish);
            }
        }finally{Texts.select(host.getTargetContext(),"zh");if(activity!=null)main(activity::finish);}
        return checks;
    }
    private void unlock()throws Exception{
        main(()->{
            invoke("acceptIncoming",new Class<?>[]{String.class},backup);invoke("showUnlockDialog");
            Dialog dialog=(Dialog)get("unlockDialog");
            for(View view:views(dialog.getWindow().getDecorView()))if(view instanceof EditText){((EditText)view).setText("test fixture password only");break;}
            button(dialog,activity.getString(R.string.wallet_unlock_action)).performClick();
        });until(()->get("unlocked")!=null&&"revealed".equals(get("screen")),20000);
    }
    private void pixelMetrics(String language){
        main(()->{
            for(float scale:new float[]{1f,1.3f}){
                Configuration configuration=new Configuration(activity.getResources().getConfiguration());configuration.fontScale=scale;
                Context context=activity.createConfigurationContext(configuration);
                for(int dp:new int[]{180,260,320}){
                    int width=Math.round(dp*context.getResources().getDisplayMetrics().density);
                    PrivateKeyView actual=new PrivateKeyView(context,WIF);TextView expected=legacy(context);
                    Bitmap a=drawMeasured(actual,width),b=drawMeasured(expected,width);
                    try{
                        if(!a.sameAs(b)){
                            try{File dir=activity.getExternalFilesDir("private-access-qa");dir.mkdirs();save(a,new File(dir,"metric-protected.png"));save(b,new File(dir,"metric-previous.png"));}catch(IOException ex){throw new RuntimeException(ex);}
                        }
                        yes(a.sameAs(b),"private text pixels match the previous widget: "+language+" "+dp+"dp fontScale="+scale+" actual="+a.getWidth()+"x"+a.getHeight()+" previous="+b.getWidth()+"x"+b.getHeight()+" "+describe(actual,expected));
                    }
                    finally{actual.clear();expected.setText("");a.eraseColor(Color.TRANSPARENT);b.eraseColor(Color.TRANSPARENT);a.recycle();b.recycle();}
                }
            }
        });
    }
    private TextView legacy(Context context){
        TextView value=new TextView(context);value.setLayoutParams(new LinearLayout.LayoutParams(-1,-2));value.setText("");value.setTextSize(13);value.setTextColor(AppUi.FG);value.setTypeface(Typeface.DEFAULT);
        value.setIncludeFontPadding(false);value.setLineHeight(Math.round(value.getTextSize()*1.75f));
        value.setText(WIF);value.setTypeface(Typeface.MONOSPACE);value.setGravity(Gravity.CENTER);
        value.setSingleLine(false);value.setHorizontallyScrolling(false);value.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_NONE);
        int padding=Math.round(12*context.getResources().getDisplayMetrics().density);value.setPadding(padding,padding,padding,padding);return value;
    }
    private String describe(PrivateKeyView actual,TextView expected){
        try{
            Field field=PrivateKeyView.class.getDeclaredField("layout");field.setAccessible(true);
            android.text.Layout a=(android.text.Layout)field.get(actual),b=expected.getLayout();
            StringBuilder result=new StringBuilder();
            for(android.text.Layout layout:new android.text.Layout[]{a,b}){
                result.append("layout:");
                for(int i=0;i<layout.getLineCount();i++)result.append(layout.getLineStart(i)).append('/').append(layout.getLineEnd(i)).append('@').append(layout.getLineBaseline(i)).append(',').append(layout.getLineLeft(i)).append(';');
                result.append(" flags=").append(layout.getPaint().getFlags()).append(" color=").append(layout.getPaint().getColor());
            }
            return result.toString();
        }catch(Exception ex){throw new RuntimeException(ex);}
    }
    private Bitmap drawMeasured(View view,int width){
        view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        view.layout(0,0,view.getMeasuredWidth(),view.getMeasuredHeight());
        Bitmap image=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);view.draw(new Canvas(image));return image;
    }
    private void sameSheetAsPreviousTextView(String language,PrivateKeyView value)throws Exception{
        Dialog owner=(Dialog)get("qrDialog");View root=owner.getWindow().getDecorView();Bitmap[] before={null},after={null};
        int[] originalBounds=new int[4];char[] old=owned(value);
        main(()->{
            before[0]=draw(root);originalBounds[0]=value.getLeft();originalBounds[1]=value.getTop();originalBounds[2]=value.getWidth();originalBounds[3]=value.getHeight();
            LinearLayout parent=(LinearLayout)value.getParent();int index=parent.indexOfChild(value);ViewGroup.LayoutParams parameters=value.getLayoutParams();
            parent.removeView(value);TextView previous=legacy(activity);previous.setBackground(((AppUi)get("ui")).shape(0xfff5efe6,11,false));parent.addView(previous,index,parameters);
        });host.waitForIdleSync();
        main(()->{
            after[0]=draw(root);
            yes(value.isCleared()&&Arrays.equals(old,new char[old.length]),"detachment immediately clears owned private characters");
            yes(before[0].sameAs(after[0]),"entire private sheet is pixel-identical to previous UI in "+language);
        });
        File directory=activity.getExternalFilesDir("private-access-qa");if(directory==null||(!directory.isDirectory()&&!directory.mkdirs()))throw new IOException();
        try{save(before[0],new File(directory,"private-sheet-"+language+"-protected.png"));save(after[0],new File(directory,"private-sheet-"+language+"-previous.png"));}
        finally{before[0].eraseColor(Color.TRANSPARENT);after[0].eraseColor(Color.TRANSPARENT);before[0].recycle();after[0].recycle();}
    }
    private static Bitmap draw(View view){Bitmap image=Bitmap.createBitmap(view.getWidth(),view.getHeight(),Bitmap.Config.ARGB_8888);view.draw(new Canvas(image));return image;}
    private static void save(Bitmap image,File file)throws IOException{try(OutputStream out=new FileOutputStream(file)){image.compress(Bitmap.CompressFormat.PNG,100,out);}}
    private char[] owned(PrivateKeyView value)throws Exception{Field f=PrivateKeyView.class.getDeclaredField("characters");f.setAccessible(true);return (char[])f.get(value);}
    private Bundle snapshot(){return host.getTargetContext().getContentResolver().call(BRIDGE,"snapshot",null,null);}
    private void resetProbe(){host.getTargetContext().getContentResolver().call(BRIDGE,"reset",null,null);}
    private void assertNoLeak(String stage)throws Exception{
        until(()->snapshot().getInt("nodes")>5,5000);Thread.sleep(150);
        Bundle result=snapshot();yes(!result.getBoolean("nodeLeak")&&!result.getBoolean("eventLeak"),"enabled accessibility service sees normal UI but no WIF in nodes/events: "+stage);
    }
    private void click(String field,String text){main(()->button((Dialog)get(field),text).performClick());host.waitForIdleSync();}
    private Button button(Dialog dialog,String text){for(View view:views(dialog.getWindow().getDecorView()))if(view instanceof Button&&(text.contentEquals(((Button)view).getText())||text.equals(view.getContentDescription())))return (Button)view;throw new AssertionError("Button missing: "+text);}
    private java.util.List<View> views(View view){java.util.List<View> result=new java.util.ArrayList<>();result.add(view);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)result.addAll(views(((ViewGroup)view).getChildAt(i)));return result;}
    private Object get(String name){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void invoke(String name){invoke(name,new Class<?>[0]);}
    private void invoke(String name,Class<?>[] types,Object...args){try{java.lang.reflect.Method method=MainActivity.class.getDeclaredMethod(name,types);method.setAccessible(true);method.invoke(activity,args);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void main(Runnable action){Throwable[] error={null};host.runOnMainSync(()->{try{action.run();}catch(Throwable ex){error[0]=ex;}});if(error[0]!=null)throw new AssertionError("Private protection UI check",error[0]);}
    private void until(BooleanSupplier condition,long timeout)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){if(condition.getAsBoolean())return;Thread.sleep(50);}throw new AssertionError("Private protection condition timed out");}
    private void yes(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}
