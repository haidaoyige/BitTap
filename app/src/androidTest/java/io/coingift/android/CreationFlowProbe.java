package io.coingift.android;

import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import io.coingift.core.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Public test passwords and disposable generated wallets only; never fund these test wallets. */
final class CreationFlowProbe {
    private final Instrumentation host;
    private MainActivity activity;
    private int checks;
    private static final String PASSWORD="Mx7!pQ2@tL8#vR5$zN";
    CreationFlowProbe(Instrumentation host){this.host=host;}
    int run()throws Exception{
        try{
            for(String language:new String[]{"zh","en","ja","ko"}){
                launch(language);ApprovedCopy copy=new ApprovedCopy(activity);
                main(()->button(root(),copy.ui("make")).performClick());
                yes("home".equals(get("screen"))&&get("sheet")!=null,"entry still requires consent");
                main(()->button(dialog("sheet"),copy.ui("agree")).performClick());host.waitForIdleSync();
                yes(inputs(root()).size()==1,"only the wallet note is inline");
                EditText note=inputs(root()).get(0);main(()->note.setText("test-01"));
                Button supported=button(root(),copy.section("cardHelpCopy").optString("view"));
                View prep=(View)supported.getParent();TextView preparation=null;
                for(View item:views(prep))if(item instanceof TextView&&copy.section("cardHelpCopy").optString("prep").contentEquals(((TextView)item).getText()))preparation=(TextView)item;
                yes(preparation!=null,"localized NFC preparation remains visible");
                until(()->supported.getWidth()>0&&prep.getWidth()>0,5000);
                Rect prepBounds=new Rect(),entryBounds=new Rect(),descriptionBounds=new Rect();
                prep.getGlobalVisibleRect(prepBounds);supported.getGlobalVisibleRect(entryBounds);preparation.getGlobalVisibleRect(descriptionBounds);
                yes(entryBounds.height()==supported.getHeight()&&supported.getHeight()>=Math.round(44*activity.getResources().getDisplayMetrics().density),"supported-card entry retains a full visible touch target");
                yes(prepBounds.contains(entryBounds)&&descriptionBounds.right<=entryBounds.left,"compact preparation and entry do not overlap or clip");
                main(supported::performClick);host.waitForIdleSync();
                yes(get("sheet")!=null&&views(dialog("sheet")).stream().anyMatch(v->v instanceof TextView&&((TextView)v).getText().toString().contains("NTAG216")),"supported-card sheet is reachable from the compact entry");
                main(()->button(dialog("sheet"),copy.ui("understand")).performClick());host.waitForIdleSync();
                yes("setup".equals(get("screen"))&&"test-01".contentEquals(note.getText()),"returning from card help preserves the wallet note and setup");
                yes(note.getHint()==null||note.getHint().length()==0,"wallet note has no example placeholder");
                capture("creation-"+language+"-setup",root());
                main(()->button(root(),copy.ui("generate")).performClick());host.waitForIdleSync();
                Dialog owner=(Dialog)get("creationDialog");yes(owner!=null&&owner.isShowing(),"generate opens a password dialog before generating");
                yes(get("session")==null&&!(Boolean)get("busy")&&pending()==0,"opening dialog does not generate or enqueue a password");
                List<EditText> fields=inputs(dialog("creationDialog"));yes(fields.size()==2,"two password inputs are inside the dialog");
                for(View v:views(dialog("creationDialog"))){
                    yes(!(v instanceof CheckBox),"manual password creation has no generated-password save gate");
                    Object tag=v.getTag();yes(tag==null||!tag.toString().startsWith("random-password-"),"no random-password or copy-password controls remain");
                }
                yes(fields.get(0).getTransformationMethod()!=null&&fields.get(1).getTransformationMethod()!=null,"both manual password fields start masked");
                yes(fields.get(0).getHint()==null||fields.get(0).getHint().length()==0,"password input has no explanatory placeholder");
                AppUi ui=(AppUi)get("ui");
                for(EditText field:fields){yes(!field.isSaveEnabled()&&field.getImportantForAutofill()==View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS,"password is excluded from state saving and autofill");yes((field.getImeOptions()&android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)!=0,"password disables personalized keyboard learning");}
                yes((owner.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_SECURE)!=0,"creation password dialog retains secure window protection");
                yes(protectedScroll(dialog("creationDialog"))!=null,"creation password dialog rejects obscured touches");
                Button generation=button(dialog("creationDialog"),copy.ui("creationConfirmGenerate"));yes(!generation.isEnabled(),"empty passwords disable generation");
                for(String invalid:new String[]{"Ab1!abcde","abcdef123!","ABCDEF123!","Abcdefghi!","Abcdef1234","Abcdef123 "}){
                    fill(fields,invalid,invalid);yes(!generation.isEnabled(),"missing format requirement disables button in every language");
                }
                for(String invalid:new String[]{PASSWORD+"中文",PASSWORD+"😀",PASSWORD+"Ａ",PASSWORD+"！",PASSWORD+" ",PASSWORD+"\u200b",PASSWORD+"\n",PASSWORD+"\u0000"}){
                    fill(fields,invalid,invalid);
                    yes(!generation.isEnabled(),"non-ASCII or whitespace input cannot enable generation");
                    yes(invalid.contentEquals(fields.get(0).getText())&&invalid.contentEquals(fields.get(1).getText()),"invalid input is retained for correction, never silently rewritten");
                    yes(activity.getString(R.string.new_password_characters).contentEquals(((TextView)get("creationFeedback")).getText()),"unsupported character feedback is localized");
                    final EditText invalidSecond=fields.get(1);main(()->invalidSecond.onEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_DONE));
                    yes(get("session")==null&&pending()==0&&!(Boolean)get("busy"),"keyboard submission cannot bypass character restrictions");
                }
                fill(fields,PASSWORD,PASSWORD);
                final EditText pasteTarget=fields.get(0);String pasted=PASSWORD+"中文😀";
                main(()->{
                    android.content.ClipboardManager clipboard=(android.content.ClipboardManager)activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(android.content.ClipData.newPlainText("public-invalid-password-test",pasted));
                    pasteTarget.requestFocus();pasteTarget.selectAll();pasteTarget.onTextContextMenuItem(android.R.id.paste);clipboard.clearPrimaryClip();
                });host.waitForIdleSync();
                yes(pasted.contentEquals(pasteTarget.getText())&&!generation.isEnabled(),"actual clipboard paste is rejected without removing characters");
                yes(activity.getString(R.string.new_password_characters).contentEquals(((TextView)get("creationFeedback")).getText()),"paste displays the same localized restriction");
                yes(activity.getString(R.string.new_password_characters).equals(Texts.tr(activity,NewPasswordPolicy.CHARACTERS_MESSAGE)),"core rejection maps to the same localized message");
                fill(fields,PASSWORD,PASSWORD);yes(generation.isEnabled(),"matching compliant password enables generation");
                for(int id:new int[]{R.string.new_password_length,R.string.new_password_upper,R.string.new_password_lower,R.string.new_password_digit,R.string.new_password_special}){
                    String description=activity.getString(id)+activity.getString(R.string.new_password_met);boolean found=false;
                    for(View v:views(dialog("creationDialog")))if(description.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))found=true;
                    yes(found,"all five live requirements are marked met in current language");
                }
                fill(fields,PASSWORD,PASSWORD+"different");yes(!generation.isEnabled(),"mismatch immediately disables generation");
                fill(fields,"123456789","123456789");confirm(copy);yes(get("weakPasswordConfirm")==null&&get("session")==null&&pending()==0&&((TextView)get("creationFeedback")).getVisibility()==View.VISIBLE,"nine-character password fails before generation");
                fill(fields,PASSWORD,PASSWORD+"different");confirm(copy);yes(get("session")==null&&get("weakPasswordConfirm")==null,"mismatch fails before generation");
                capture("creation-"+language+"-password",dialog("creationDialog"));
                Button stale=button(dialog("creationDialog"),copy.ui("creationConfirmGenerate"));main(owner::cancel);host.waitForIdleSync();
                yes(get("creationDialog")==null&&get("session")==null,"cancel returns without generating");
                for(EditText field:fields)yes(field.length()==0&&!ui.inputs.contains(field)&&!ui.passwords.contains(field),"cancel wipes and releases detached password inputs");
                main(stale::performClick);yes(get("session")==null&&!(Boolean)get("busy"),"stale dismissed button cannot generate");
                open(note);fields=inputs(dialog("creationDialog"));yes(fields.stream().allMatch(f->f.length()==0),"reopen starts with both passwords empty");
                fill(fields,PASSWORD,PASSWORD);Bundle state=new Bundle();main(()->activity.onSaveInstanceState(state));yes(state.isEmpty(),"password dialog is never serialized");
                EditText paused=fields.get(0);main(()->{host.callActivityOnPause(activity);host.callActivityOnStop(activity);});
                yes(get("creationDialog")==null&&paused.length()==0&&pending()==0,"background closes and wipes creation dialog");main(()->host.callActivityOnResume(activity));host.waitForIdleSync();
                EditText resumedNote=inputs(root()).get(0);open(resumedNote);fields=inputs(dialog("creationDialog"));fill(fields,PASSWORD,PASSWORD);
                confirm(copy);until(()->"write".equals(get("screen"))&&get("session")!=null&&pending()==0,30000);
                CreationSession wallet=(CreationSession)get("session");char[] password=PASSWORD.toCharArray();
                try(WalletCrypto.Unlocked restored=new WalletCrypto().decrypt(wallet.encrypted,password)){yes(restored.addresses.sameAs(wallet.addresses)&&"test-01".equals(restored.note),"real creation encrypts matching addresses and note with confirmed password");}finally{WalletCrypto.wipe(password);}
                yes(get("creationDialog")==null&&fields.stream().allMatch(f->f.length()==0),"successful creation wipes dialog fields before writing page");
                main(()->invoke("clearWalletSession",new Class<?>[]{boolean.class},true));
            }
            launch("zh");enhancedGate();rotationAndObscuring();
            return checks;
        }finally{
            Texts.select(host.getTargetContext(),"zh");
            if(activity!=null)main(()->{invoke("clearWalletSession",new Class<?>[]{boolean.class},true);activity.finish();});
        }
    }
    private void enhancedGate()throws Exception{
        main(()->{set("screen","setup");set("enhanced",true);invoke("render");});host.waitForIdleSync();ApprovedCopy copy=new ApprovedCopy(activity);
        yes(get("creationDialog")==null&&inputs(root()).size()==1,"enabled camera mode contains no password fields before capture");
        yes(button(root(),copy.ui("photoGenerate"))!=null,"uncaptured mode offers start capture");
        EditText note=inputs(root()).get(0);open(note);yes(get("creationDialog")==null&&get("session")==null,"password dialog cannot bypass an incomplete camera capture");
        CameraEntropy entropy=new CameraEntropy();java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-256");
        for(int i=0;i<32;i++)entropy.addFrame(hash.digest(("public-creation-frame-"+i).getBytes(StandardCharsets.US_ASCII)));
        entropy.finish(hash.digest("public-creation-final-photo".getBytes(StandardCharsets.US_ASCII)));
        main(()->{set("cameraEntropy",entropy);invoke("render");});host.waitForIdleSync();
        yes(get("creationDialog")==null&&button(root(),copy.ui("generateWithPassword"))!=null,"ready capture returns to setup and requires a second explicit action");
        capture("creation-enhanced-ready",root());main(()->button(root(),copy.ui("generateWithPassword")).performClick());host.waitForIdleSync();
        yes(get("creationDialog")!=null&&entropy.ready(),"opening password dialog does not consume captured entropy");
        Dialog owner=(Dialog)get("creationDialog");main(owner::cancel);yes(entropy.ready(),"password cancel keeps captured entropy available for retry");
        main(()->{host.callActivityOnPause(activity);host.callActivityOnStop(activity);});yes(!entropy.ready()&&get("cameraEntropy")==null&&get("creationDialog")==null,"background clears captured entropy and all passwords");main(()->host.callActivityOnResume(activity));host.waitForIdleSync();
        yes(button(root(),copy.ui("photoGenerate"))!=null,"resuming after cleanup asks for fresh capture");
        main(()->invoke("clearWalletSession",new Class<?>[]{boolean.class},true));
    }
    private void rotationAndObscuring()throws Exception{
        main(()->{set("screen","setup");invoke("render");});host.waitForIdleSync();EditText note=inputs(root()).get(0);open(note);List<EditText> fields=inputs(dialog("creationDialog"));fill(fields,PASSWORD,PASSWORD);
        Button old=button(dialog("creationDialog"),new ApprovedCopy(activity).ui("creationConfirmGenerate"));
        Instrumentation.ActivityMonitor monitor=host.addMonitor(MainActivity.class.getName(),null,false);main(activity::recreate);Activity next=host.waitForMonitorWithTimeout(monitor,10000);host.removeMonitor(monitor);yes(next instanceof MainActivity,"rotation recreates activity");activity=(MainActivity)next;host.waitForIdleSync();
        main(old::performClick);yes(get("creationDialog")==null&&get("session")==null&&fields.stream().allMatch(f->f.length()==0),"rotation clears passwords and rejects the old action");
        note=inputs(root()).get(0);open(note);fields=inputs(dialog("creationDialog"));fill(fields,PASSWORD,PASSWORD);
        View dialog=dialog("creationDialog");Button confirm=button(dialog,new ApprovedCopy(activity).ui("creationConfirmGenerate"));ProtectedScrollView scroll=protectedScroll(dialog);
        main(()->{Rect r=new Rect();confirm.getDrawingRect(r);confirm.requestRectangleOnScreen(r,true);});host.waitForIdleSync();
        main(()->{
            int[] origin=new int[2],target=new int[2];scroll.getLocationOnScreen(origin);confirm.getLocationOnScreen(target);
            MotionEvent.PointerProperties pointer=new MotionEvent.PointerProperties();pointer.id=0;pointer.toolType=MotionEvent.TOOL_TYPE_FINGER;MotionEvent.PointerCoords point=new MotionEvent.PointerCoords();point.x=target[0]-origin[0]+confirm.getWidth()/2f;point.y=target[1]-origin[1]+confirm.getHeight()/2f;point.pressure=1;point.size=1;
            long now=SystemClock.uptimeMillis();for(int i=0;i<2;i++){MotionEvent event=MotionEvent.obtain(now,now+i*10,i==0?MotionEvent.ACTION_DOWN:MotionEvent.ACTION_UP,1,new MotionEvent.PointerProperties[]{pointer},new MotionEvent.PointerCoords[]{point},0,0,1,1,0,0,InputDevice.SOURCE_TOUCHSCREEN,MotionEvent.FLAG_WINDOW_IS_OBSCURED);try{scroll.dispatchTouchEvent(event);}finally{event.recycle();}}
        });host.waitForIdleSync();
        yes("protection".equals(get("screen"))&&get("creationDialog")==null&&get("session")==null&&pending()==0,"obscured generation is blocked and password sheet is cleared");
        yes(fields.stream().allMatch(f->f.length()==0),"obscured touch leaves no input text");main(confirm::performClick);yes(get("session")==null&&!(Boolean)get("busy"),"blocked stale confirmation cannot generate");
    }
    private void launch(String locale){if(activity!=null){main(activity::finish);host.waitForIdleSync();}Texts.select(host.getTargetContext(),locale);activity=(MainActivity)host.startActivitySync(new Intent(host.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));host.waitForIdleSync();}
    private void open(EditText note){main(()->invoke("showCreationDialog",new Class<?>[]{EditText.class},note));host.waitForIdleSync();}
    private void fill(List<EditText> fields,String first,String second){main(()->{fields.get(0).setText(first);fields.get(1).setText(second);});}
    private void confirm(ApprovedCopy copy){main(()->button(dialog("creationDialog"),copy.ui("creationConfirmGenerate")).performClick());host.waitForIdleSync();}
    private int pending(){synchronized(activity){return ((Set<?>)get("pendingPasswords")).size();}}
    private View root(){return activity.getWindow().getDecorView();}
    private View dialog(String name){return ((Dialog)get(name)).getWindow().getDecorView();}
    private List<EditText> inputs(View root){List<EditText> list=new ArrayList<>();for(View v:views(root))if(v instanceof EditText)list.add((EditText)v);return list;}
    private List<View> views(View root){List<View> list=new ArrayList<>();list.add(root);if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)list.addAll(views(((ViewGroup)root).getChildAt(i)));return list;}
    private Button button(View root,String label){for(View v:views(root))if(v instanceof Button&&(label.contentEquals(((Button)v).getText())||label.equals(v.getContentDescription())))return (Button)v;throw new AssertionError("Button missing: "+label);}
    private ProtectedScrollView protectedScroll(View root){for(View v:views(root))if(v instanceof ProtectedScrollView)return (ProtectedScrollView)v;throw new AssertionError("Protected scroll missing");}
    private Object get(String name){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(activity);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void set(String name,Object value){try{Field f=MainActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(activity,value);}catch(Exception ex){throw new RuntimeException(ex);}}
    private Object invoke(String name){return invoke(name,new Class<?>[0]);}
    private Object invoke(String name,Class<?>[] types,Object... values){try{Method m=MainActivity.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(activity,values);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void capture(String name,View root){main(()->{Bitmap image=Bitmap.createBitmap(root.getWidth(),root.getHeight(),Bitmap.Config.ARGB_8888);try{root.draw(new Canvas(image));File dir=activity.getExternalFilesDir("creation-qa");if(dir==null||(!dir.isDirectory()&&!dir.mkdirs()))throw new IOException();try(OutputStream out=new FileOutputStream(new File(dir,name+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}}catch(IOException ex){throw new RuntimeException(ex);}finally{image.eraseColor(Color.TRANSPARENT);image.recycle();}});}
    private void main(Runnable action){Throwable[] error={null};host.runOnMainSync(()->{try{action.run();}catch(Throwable ex){error[0]=ex;}});if(error[0]!=null)throw new AssertionError("UI operation",error[0]);}
    private void until(java.util.function.BooleanSupplier condition,long timeout)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){boolean[] done={false};main(()->done[0]=condition.getAsBoolean());if(done[0]){host.waitForIdleSync();return;}Thread.sleep(50);}throw new AssertionError("UI timeout");}
    private void yes(boolean value,String description){if(!value)throw new AssertionError(description);checks++;}
}
