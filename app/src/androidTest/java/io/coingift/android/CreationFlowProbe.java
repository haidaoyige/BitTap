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
                EditText note=inputs(root()).get(0);main(()->note.setText("test-01"));capture("creation-"+language+"-setup",root());
                main(()->button(root(),copy.ui("generate")).performClick());host.waitForIdleSync();
                Dialog owner=(Dialog)get("creationDialog");yes(owner!=null&&owner.isShowing(),"generate opens a password dialog before generating");
                yes(get("session")==null&&!(Boolean)get("busy")&&pending()==0,"opening dialog does not generate or enqueue a password");
                List<EditText> fields=inputs(dialog("creationDialog"));yes(fields.size()==2,"two password inputs are inside the dialog");
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
                randomPasswordFlow();
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
    private void randomPasswordFlow()throws Exception{
        main(()->{set("screen","setup");invoke("render");});host.waitForIdleSync();
        EditText note=inputs(root()).get(0);open(note);List<EditText> fields=inputs(dialog("creationDialog"));
        Dialog owner=(Dialog)get("creationDialog");ApprovedCopy copy=new ApprovedCopy(activity);
        Button random=button(dialog("creationDialog"),activity.getString(R.string.random_password_generate));
        main(random::performClick);host.waitForIdleSync();
        final EditText first=fields.get(0),second=fields.get(1);char[] password=new char[first.length()];
        first.getText().getChars(0,first.length(),password,0);
        try{
            yes(password.length==32&&NewPasswordPolicy.inspect(java.nio.CharBuffer.wrap(password)).satisfied(),"generated password has 32 allowed characters and all four classes");
            yes((Boolean)get("creationPasswordGenerated")&&first.getTransformationMethod()==null&&second.length()==0,"generated password is visible and confirmation starts empty");
            yes(first.getMinLines()>=2&&first.getEllipsize()==null,"generated password wraps instead of ellipsizing");
            for(float size:new float[]{16,24}){
                main(()->first.setTextSize(size));host.waitForIdleSync();
                yes(first.getLayout()!=null&&first.getLayout().getLineCount()>=2&&first.getLayout().getLineEnd(first.getLayout().getLineCount()-1)==32,"all generated characters remain in the wrapped layout at font size "+size);
            }
            main(()->first.setTextSize(16));host.waitForIdleSync();
            CheckBox saved=(CheckBox)get("creationRandomSaved");
            yes(saved!=null&&!saved.isChecked()&&saved.getVisibility()==View.VISIBLE,"save acknowledgement is shown and unchecked");
            yes(hasText(dialog("creationDialog"),activity.getString(R.string.random_password_warning_body)),"generated password loss warning is localized");
            Button copyButton=button(dialog("creationDialog"),activity.getString(R.string.random_password_copy));main(copyButton::performClick);
            android.content.ClipboardManager clipboard=(android.content.ClipboardManager)activity.getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            if(Build.VERSION.SDK_INT>=29){
                android.content.ClipData clip=clipboard.getPrimaryClip();
                yes(clip!=null&&java.nio.CharBuffer.wrap(password).toString().contentEquals(clip.getItemAt(0).getText()),"copied password is read back exactly");
                yes(clip.getDescription().getExtras()!=null&&clip.getDescription().getExtras().getBoolean("android.content.extra.IS_SENSITIVE"),"copied password is marked sensitive");
                main(()->{second.requestFocus();second.onTextContextMenuItem(android.R.id.paste);});host.waitForIdleSync();
                yes(android.text.TextUtils.equals(first.getText(),second.getText()),"actual paste into confirmation preserves all characters");
            }else{
                yes(clipboard.getPrimaryClip()==null,"Android 9 never copies generated passwords");
                main(()->second.getText().replace(0,second.length(),java.nio.CharBuffer.wrap(password)));
            }
            Button generate=button(dialog("creationDialog"),copy.ui("creationConfirmGenerate"));
            yes(!generate.isEnabled(),"matching confirmation cannot bypass unsaved random password");
            confirm(copy);yes(get("session")==null&&pending()==0,"submission guard rejects missing save acknowledgement");
            main(()->saved.setChecked(true));yes(generate.isEnabled(),"saved and matching generated password enables wallet creation");
            main(random::performClick);host.waitForIdleSync();
            yes(get("randomPasswordReplace")!=null&&matches(password,first),"regenerate asks before replacing the current password");
            Dialog replace=(Dialog)get("randomPasswordReplace");
            final Dialog keepReplace=replace;main(()->button(keepReplace.getWindow().getDecorView(),activity.getString(R.string.random_password_keep)).performClick());host.waitForIdleSync();
            yes(get("randomPasswordReplace")==null&&matches(password,first)&&saved.isChecked(),"keeping password preserves input and save acknowledgement");
            main(random::performClick);host.waitForIdleSync();replace=(Dialog)get("randomPasswordReplace");final Dialog nextReplace=replace;
            main(()->button(nextReplace.getWindow().getDecorView(),activity.getString(R.string.random_password_replace_action)).performClick());host.waitForIdleSync();
            yes(!matches(password,first)&&first.length()==32&&second.length()==0&&!saved.isChecked()&&!generate.isEnabled(),"regenerate clears old confirmation and save acknowledgement");
            yes(clipboard.getPrimaryClip()==null,"regenerate clears its own copied old password");
            main(()->first.getText().append("A"));host.waitForIdleSync();
            yes(!(Boolean)get("creationPasswordGenerated")&&saved.getVisibility()==View.GONE,"manual editing leaves random mode and resets acknowledgement");
            main(random::performClick);host.waitForIdleSync();replace=(Dialog)get("randomPasswordReplace");
            Button stale=button(replace.getWindow().getDecorView(),activity.getString(R.string.random_password_replace_action));
            main(owner::cancel);host.waitForIdleSync();main(stale::performClick);
            yes(get("creationDialog")==null&&get("randomPasswordReplace")==null&&get("creationRandomInput")==null&&get("creationRandomSaved")==null,"dismiss wipes and releases generated password controls and nested dialog");
            yes(first.length()==0&&second.length()==0&&pending()==0&&get("session")==null,"stale regeneration action cannot restore cancelled inputs or create a wallet");
            open(note);fields=inputs(dialog("creationDialog"));final EditText newFirst=fields.get(0),newSecond=fields.get(1);
            main(()->button(dialog("creationDialog"),activity.getString(R.string.random_password_generate)).performClick());host.waitForIdleSync();
            char[] fresh=characters(newFirst);
            try{
                main(()->{newSecond.getText().replace(0,newSecond.length(),java.nio.CharBuffer.wrap(fresh));((CheckBox)get("creationRandomSaved")).setChecked(true);});confirm(copy);
                until(()->"write".equals(get("screen"))&&get("session")!=null&&pending()==0,30000);
                try(WalletCrypto.Unlocked restored=new WalletCrypto().decrypt(((CreationSession)get("session")).encrypted,fresh)){yes(restored.addresses.sameAs(((CreationSession)get("session")).addresses),"random password actually encrypts and recovers the created wallet");}
                yes(newFirst.length()==0&&newSecond.length()==0&&get("creationRandomInput")==null,"successful wallet creation wipes and releases generated input");
            }finally{WalletCrypto.wipe(fresh);}
            main(()->invoke("clearWalletSession",new Class<?>[]{boolean.class},true));
        }finally{WalletCrypto.wipe(password);}
    }
    private boolean hasText(View root,String text){for(View v:views(root))if(v instanceof TextView&&text.contentEquals(((TextView)v).getText()))return true;return false;}
    private boolean matches(char[] value,EditText input){return android.text.TextUtils.equals(java.nio.CharBuffer.wrap(value),input.getText());}
    private char[] characters(EditText input){char[] value=new char[input.length()];input.getText().getChars(0,input.length(),value,0);return value;}
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
