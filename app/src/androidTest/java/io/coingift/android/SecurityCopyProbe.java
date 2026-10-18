package io.coingift.android;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import org.json.*;

/** Checks real native rendering of every localized security paragraph and disclosure control. */
final class SecurityCopyProbe {
    private final SmokeRunner host;
    private MainActivity activity;
    private int checks;
    SecurityCopyProbe(SmokeRunner host){this.host=host;}
    int run()throws Exception{
        String[] languages={"zh","en","ja","ko"};
        String[] titles={"BitTap 如何保护你的私钥？","How does BitTap protect your private key?","BitTap は秘密鍵をどのように保護する？","BitTap은 개인 키를 어떻게 보호하나요?"};
        String chineseFirst=null;
        try{
            for(int l=0;l<languages.length;l++){
                String language=languages[l];Texts.select(host.getTargetContext(),language);
                activity=(MainActivity)host.startActivitySync(new Intent(host.getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));host.waitForIdleSync();
                ApprovedCopy copy=new ApprovedCopy(activity);JSONObject security=copy.security();
                yes(titles[l].equals(security.getString("title")),"security content resolves the selected language: "+language);
                if(l==0)chineseFirst=security.getJSONArray("sections").getJSONObject(0).getJSONArray("paragraphs").getString(0);
                else yes(!chineseFirst.equals(security.getJSONArray("sections").getJSONObject(0).getJSONArray("paragraphs").getString(0)),"non-Chinese locale does not fall back to Chinese body");
                main(()->invoke("help"));host.waitForIdleSync();
                click(((Dialog)get("sheet")).getWindow().getDecorView(),copy.ui("securityEntry"));
                yes("security".equals(get("screen")),"About entry opens the existing security page");
                View root=activity.getWindow().getDecorView();
                yes(has(root,security.getString("title"))&&has(root,security.getString("intro"))&&has(root,security.getString("cleanup")),"heading, introduction and cleanup callout are localized");
                List<View> headers=new ArrayList<>();for(View view:views(root))if("accordion-header".equals(view.getTag()))headers.add(view);
                yes(headers.size()==9,"all nine existing disclosure groups remain");
                JSONArray sections=security.getJSONArray("sections");
                for(int i=0;i<8;i++){
                    JSONObject section=sections.getJSONObject(i);View header=headers.get(i),body=((ViewGroup)header.getParent()).getChildAt(1);
                    yes(section.getString("title").contentEquals(header.getContentDescription()),"localized disclosure heading "+i);
                    yes(body.getVisibility()==(i==0?View.VISIBLE:View.GONE),"default first-open disclosure behavior remains");
                    if(i>0)main(header::performClick);host.waitForIdleSync();
                    yes(body.getVisibility()==View.VISIBLE,"disclosure still opens");
                    JSONArray paragraphs=section.getJSONArray("paragraphs");
                    for(int j=0;j<paragraphs.length();j++)yes(has(body,paragraphs.getString(j).replace("**","")),"localized paragraph renders fully: "+language+" "+i+"/"+j);
                    main(header::performClick);host.waitForIdleSync();yes(body.getVisibility()==View.GONE,"disclosure still closes");
                }
                View related=headers.get(8),body=((ViewGroup)related.getParent()).getChildAt(1);
                yes(security.getString("relatedTitle").contentEquals(related.getContentDescription()),"comparison disclosure heading is localized");
                main(related::performClick);host.waitForIdleSync();
                JSONArray items=security.getJSONArray("related");
                for(int i=0;i<items.length();i++){JSONObject item=items.getJSONObject(i);yes(has(body,item.getString("title"))&&has(body,item.getString("text")),"all comparison text is localized");}
                yes(has(body,security.getString("caution"))&&has(body,security.getString("compatibility")),"security limits and recovery-format explanation are localized");
                main(()->{related.performClick();headers.get(0).performClick();});host.waitForIdleSync();capture(language);
                click(root,copy.ui("backSecurity"));yes("home".equals(get("screen"))&&((Dialog)get("sheet")).isShowing(),"return still opens the About sheet");
                main(activity::finish);host.waitForIdleSync();
            }
            return checks;
        }finally{Texts.select(host.getTargetContext(),"zh");if(activity!=null)main(activity::finish);}
    }
    private void capture(String language){main(()->{
        View page=((AppUi)get("ui")).page;Bitmap image=Bitmap.createBitmap(page.getWidth(),page.getHeight(),Bitmap.Config.ARGB_8888);
        try{image.eraseColor(AppUi.BG);page.draw(new Canvas(image));File directory=activity.getExternalFilesDir("security-copy-qa");if(directory==null||(!directory.isDirectory()&&!directory.mkdirs()))throw new IOException();try(OutputStream out=new FileOutputStream(new File(directory,"security-"+language+".png"))){image.compress(Bitmap.CompressFormat.PNG,100,out);}}
        catch(IOException ex){throw new RuntimeException(ex);}finally{image.eraseColor(Color.TRANSPARENT);image.recycle();}
    });}
    private void click(View root,String text){main(()->{for(View view:views(root))if(view instanceof Button&&(text.contentEquals(((Button)view).getText())||text.contentEquals(view.getContentDescription()==null?"":view.getContentDescription()))){view.performClick();return;}throw new AssertionError("Missing action: "+text);});host.waitForIdleSync();}
    private boolean has(View root,String text){for(View view:views(root))if(view instanceof TextView&&text.contentEquals(((TextView)view).getText()))return true;return false;}
    private List<View> views(View root){List<View> result=new ArrayList<>();result.add(root);if(root instanceof ViewGroup)for(int i=0;i<((ViewGroup)root).getChildCount();i++)result.addAll(views(((ViewGroup)root).getChildAt(i)));return result;}
    private Object get(String name){try{Field field=MainActivity.class.getDeclaredField(name);field.setAccessible(true);return field.get(activity);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void invoke(String name){try{java.lang.reflect.Method method=MainActivity.class.getDeclaredMethod(name);method.setAccessible(true);method.invoke(activity);}catch(Exception ex){throw new RuntimeException(ex);}}
    private void main(Runnable action){Throwable[] failure={null};host.runOnMainSync(()->{try{action.run();}catch(Throwable ex){failure[0]=ex;}});if(failure[0]!=null)throw new AssertionError("Security copy UI check",failure[0]);}
    private void yes(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
}
