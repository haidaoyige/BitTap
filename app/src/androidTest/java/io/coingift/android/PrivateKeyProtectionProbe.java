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
        try(InputStream in=host.getContext().getAssets().open("cg3-scalar-one.txt")){backup=new String(BackupFiles.readBounded(in),StandardCharsets.US_ASCII).trim();}
        host.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        until(()->snapshot().getBoolean("connected"),10000);
        try{
            for(String language:new String[]{"zh","en","ja","ko"}){
                services(true);Texts.select(host.getTargetContext(),language);
                activity=(MainActivity)host.startActivitySync(new Intent(host.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));host.waitForIdleSync();
                pixelMetrics(language);
                yes(AccessibilityGate.blocked(activity),"actual enabled service blocks sensitive operations");
                main(()->{invoke("acceptIncoming",new Class<?>[]{String.class},backup);invoke("showUnlockDialog");});
                yes(get("unlockDialog")==null&&get("unlocked")==null&&get("accessNotice")!=null,"password entry blocked with localized app notice");
                Dialog notice=(Dialog)get("accessNotice");yes(!(notice instanceof AlertDialog)&&notice.getWindow().getAttributes().gravity==Gravity.BOTTOM,"notice follows app bottom sheet style");
                yes((notice.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"access notice retains secure flag");
                main(()->{invoke("closeAccessNotice");invoke("showAddressQr",new Class<?>[]{String.class,String.class,boolean.class},"SegWit","bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",false);});
                yes(get("qrBitmap")!=null,"public address viewing remains available");main(()->invoke("dismissQr"));
                main(()->{set("screen","setup");invoke("render");EditText note=null;for(View v:views(activity.getWindow().getDecorView()))if(v instanceof EditText){note=(EditText)v;break;}invoke("showCreationDialog",new Class<?>[]{EditText.class},note);});
                yes(get("creationDialog")==null&&get("accessNotice")!=null,"new password entry blocked as well");
                main(()->{invoke("closeAccessNotice");invoke("acceptIncoming",new Class<?>[]{String.class},backup);});
                // Positive control proves the separate-process service can actually read plaintext widgets.
                final Dialog[] control={null};main(()->{control[0]=new Dialog(activity);control[0].setContentView(legacy(activity));control[0].getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);control[0].show();});
                until(()->snapshot().getBoolean("nodeLeak"),5000);yes(snapshot().getBoolean("nodeLeak"),"enabled service reads positive-control WIF under FLAG_SECURE");
                main(()->control[0].dismiss());resetProbe();services(false);
                main(()->invoke("closeAccessNotice"));yes(get("unlocked")==null,"disabling service never automatically unlocks");
                unlock();PrivateSession holder=(PrivateSession)get("unlocked");byte[] sealed=sealed(holder);
                yes(sealed.length==48,"recovered activity retains authenticated ciphertext instead of scalar");
                main(()->invoke("showPrivateQr"));until(()->get("qrBitmap")!=null,10000);
                PrivateKeyView secret=(PrivateKeyView)get("secretView");char[] raw=owned(secret);Bitmap qr=(Bitmap)get("qrBitmap");
                yes(secret!=null&&!secret.isCleared(),"explicit view still works without service");
                Button stale=button((Dialog)get("qrDialog"),Build.VERSION.SDK_INT<=28?activity.getString(R.string.private_copy_unavailable):new ApprovedCopy(activity).ui("copyWif"));
                services(true);until(()->get("unlocked")==null&&get("accessNotice")!=null,10000);
                yes(secret.isCleared()&&Arrays.equals(raw,new char[raw.length])&&qr.isRecycled(),"enabling service clears WIF and QR immediately");
                yes(Arrays.equals(sealed,new byte[sealed.length])&&sealed(holder)==null,"enabling service closes sealed session");
                main(stale::performClick);yes(get("copyConfirm")==null&&get("pendingQrImage")==null,"stale button cannot export after service is enabled");
                assertNoLeak("service-enabled lock");services(false);main(()->invoke("closeAccessNotice"));yes(get("unlocked")==null,"service shutdown leaves session locked");
                if("zh".equals(language)){checks+=new PrivateSessionProbe(host).run();passwordAndFileRace();}
                main(activity::finish);host.waitForIdleSync();activity=null;
            }
        }finally{services(false);Texts.select(host.getTargetContext(),"zh");if(activity!=null)main(activity::finish);}
        return checks;
    }
    private void passwordAndFileRace()throws Exception{
        final EditText[] password={null};final Button[] stale={null};
        main(()->{invoke("showUnlockDialog");Dialog dialog=(Dialog)get("unlockDialog");for(View v:views(dialog.getWindow().getDecorView()))if(v instanceof EditText){password[0]=(EditText)v;password[0].setText("Public-fixture-Only9!");break;}stale[0]=button(dialog,activity.getString(R.string.wallet_unlock_action));});
        services(true);until(()->get("unlockDialog")==null,10000);yes(password[0].length()==0&&get("unlocked")==null,"service activation clears already-entered password and closes input");
        main(stale[0]::performClick);yes(get("unlocked")==null&&!(Boolean)get("busy"),"old password submit cannot start decrypt after service activation");
        services(false);main(()->invoke("closeAccessNotice"));
        Uri provider=Uri.parse("content://io.coingift.android.test.documents");host.getTargetContext().getContentResolver().call(provider,"reset",null,null);
        byte[] png="public fixture private-export bytes".getBytes(StandardCharsets.US_ASCII);
        try{
            main(()->invoke("startDocumentOperation",new Class<?>[]{Uri.class,DocumentIo.Kind.class,String.class,byte[].class,io.coingift.core.CreationSession.class,String.class,boolean.class},Uri.withAppendedPath(provider,"slow-open"),DocumentIo.Kind.PNG,null,png,null,null,true));
            DocumentIo.Job abandoned=(DocumentIo.Job)get("fileJob");services(true);
            until(()->get("fileJob")==null,10000);yes(Arrays.equals(png,new byte[png.length])&&!(Boolean)get("fileBusy"),"service activation cancels pending provider and wipes private image bytes");
            main(()->activity.documentFinished(abandoned,true,null));yes(get("fileJob")==null&&get("unlocked")==null,"late provider completion cannot revive private export or unlock");
        }finally{host.getTargetContext().getContentResolver().call(provider,"release",null,null);services(false);main(()->invoke("closeAccessNotice"));}
    }
    private byte[] sealed(PrivateSession value){try{Field f=PrivateSession.class.getDeclaredField("ciphertext");f.setAccessible(true);return (byte[])f.get(value);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void services(boolean enabled)throws Exception{
        String services=enabled?"io.coingift.android.test/io.coingift.android.PrivateAccessService":"null";
        shell(enabled?"settings put secure enabled_accessibility_services "+services:"settings delete secure enabled_accessibility_services");shell("settings put secure accessibility_enabled "+(enabled?1:0));
        until(()->AccessibilityGate.blocked(host.getTargetContext())==enabled,10000);
        if(enabled)until(()->snapshot().getBoolean("connected"),10000);
    }
    private void shell(String command)throws Exception{try(ParcelFileDescriptor fd=host.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES).executeShellCommand(command);InputStream in=new ParcelFileDescriptor.AutoCloseInputStream(fd)){while(in.read()!=-1){}}}
    private void set(String name,Object value){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}catch(Exception ex){throw new RuntimeException(ex);}}
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
                    PrivateKeyView actual=new PrivateKeyView(context,WIF.toCharArray());TextView expected=legacy(context);
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
        // Compare settled content, not the cancel button's transient ripple animation.
        SystemClock.sleep(400);host.waitForIdleSync();main(root::jumpDrawablesToCurrentState);
        // Widget replacement reawakens a fading scrollbar. Its transient 4dp edge
        // is not part of the WIF/layout equivalence check; suppress it in BOTH captures.
        main(()->{for(View view:views(root))if(view instanceof ScrollView)view.setVerticalScrollBarEnabled(false);});
        int[] originalBounds=new int[4];char[] old=owned(value);
        main(()->{
            before[0]=draw(root);originalBounds[0]=value.getLeft();originalBounds[1]=value.getTop();originalBounds[2]=value.getWidth();originalBounds[3]=value.getHeight();
            LinearLayout parent=(LinearLayout)value.getParent();int index=parent.indexOfChild(value);ViewGroup.LayoutParams parameters=value.getLayoutParams();
            parent.removeView(value);TextView previous=legacy(activity);previous.setBackground(((AppUi)get("ui")).shape(0xfff5efe6,11,false));parent.addView(previous,index,parameters);
        });host.waitForIdleSync();
        main(()->{
            after[0]=draw(root);
            yes(value.isCleared()&&Arrays.equals(old,new char[old.length]),"detachment immediately clears owned private characters");
        });
        File directory=activity.getExternalFilesDir("private-access-qa");if(directory==null||(!directory.isDirectory()&&!directory.mkdirs()))throw new IOException();
        try{save(before[0],new File(directory,"private-sheet-"+language+"-protected.png"));save(after[0],new File(directory,"private-sheet-"+language+"-previous.png"));yes(before[0].sameAs(after[0]),"entire private sheet is pixel-identical to previous UI in "+language);}
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
    private void until(BooleanSupplier condition,long timeout)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){if(condition.getAsBoolean()){host.waitForIdleSync();return;}Thread.sleep(50);}throw new AssertionError("Private protection condition timed out");}
    private void yes(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}
