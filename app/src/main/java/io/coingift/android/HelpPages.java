package io.coingift.android;

import android.app.Dialog;
import android.view.View;
import android.widget.*;
import org.json.*;

/** Native rendering of the approved prototype, including its groups and navigation. */
final class HelpPages {
    private final AppUi ui;private final ApprovedCopy copy;
    private final java.util.Set<Integer> expanded=new java.util.HashSet<>(java.util.Arrays.asList(0));
    HelpPages(AppUi ui,ApprovedCopy copy){this.ui=ui;this.copy=copy;}
    Dialog disclaimer(Runnable agree,Runnable full,Runnable close){return ui.consentSheet(copy.ui("disclaimer"),p->{
        ui.note(p,copy.ui("disclaimerIntro"));int[] icons={R.drawable.ic_code,R.drawable.ic_warning,R.drawable.ic_scale};
        for(int i=1;i<=3;i++)ui.risk(p,icons[i-1],copy.ui("risk"+i),copy.ui("risk"+i+"body"),i<3);
        ui.link(p,copy.ui("readFullDisclaimer"),R.drawable.ic_file,full);
        ui.action(p,copy.ui("agree"),true,R.drawable.ic_check_mark,agree);ui.action(p,copy.ui("disagree"),false,0,close);
    });}
    Dialog cardHelp(Runnable close){JSONObject c=copy.section("cardHelpCopy");return ui.sheet(c.optString("title"),p->{
        JSONArray rows=c.optJSONArray("rows");int[] icons={R.drawable.ic_nfc,R.drawable.ic_display,R.drawable.ic_shuffle,R.drawable.ic_help};
        for(int i=0;i<rows.length();i++){JSONArray row=rows.optJSONArray(i);ui.risk(p,icons[i],row.optString(1),row.optString(2),i<rows.length()-1);}
        ui.helper(p,c.optString("phone"));ui.action(p,copy.ui("understand"),true,0,close);
    });}
    Dialog transfer(Runnable close){JSONObject c=copy.section("transferCopy");return ui.sheet(copy.ui("transfer"),p->{
        ui.risk(p,R.drawable.ic_key,c.optString("format"),c.optString("formatBody"),true);
        LinearLayout wallets=ui.risk(p,R.drawable.ic_wallet,c.optString("wallets"),null,true);
        wallet(wallets,"OKX Web3 Wallet · web3.okx.com",c.optString("okx"));wallet(wallets,"Electrum · electrum.org",c.optString("electrum"));wallet(wallets,"BlueWallet · bluewallet.io",c.optString("blue"));
        ui.risk(p,R.drawable.ic_display,c.optString("compatibility"),c.optString("compatibilityBody"),false);
        LinearLayout safety=ui.callout(p,R.drawable.ic_warning,null);ui.text(safety,c.optString("safety"),12,AppUi.SUB,true);ui.text(safety,c.optString("safetyBody"),12,AppUi.SUB,false);
        ui.helper(p,c.optString("source"));ui.action(p,copy.ui("understand"),true,0,close);
    });}
    private void wallet(LinearLayout p,String title,String body){ui.space(p,8);ui.text(p,title,12,AppUi.SUB,true);ui.text(p,body,12,AppUi.SUB,false);}
    Dialog about(Runnable close,Runnable security,Runnable language){return ui.sheet(copy.ui("about"),p->{
        ui.text(p,copy.ui("aboutBody"),13,AppUi.SUB,false);ui.space(p,2);ui.callout(p,R.drawable.ic_shield,copy.ui("publicInfo"));ui.space(p,8);
        ui.action(p,copy.ui("securityEntry"),false,R.drawable.ic_shield,security);
        if(language!=null)ui.link(p,copy.ui("language"),R.drawable.ic_globe,language);
        ui.action(p,copy.ui("understand"),true,0,close);
    });}
    Dialog principle(Runnable close){return ui.sheet(copy.ui("principleTitle"),p->{
        int[] icons={R.drawable.ic_shield,R.drawable.ic_camera,R.drawable.ic_shuffle};
        for(int i=1;i<=3;i++){LinearLayout row=ui.horizontal();row.setPadding(0,ui.dp(6),0,ui.dp(6));p.addView(row);ImageView image=new ImageView(ui.activity);image.setImageDrawable(ui.drawable(icons[i-1],AppUi.ACCENT,17));LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(ui.dp(17),ui.dp(20));ip.rightMargin=ui.dp(8);row.addView(image,ip);ui.text(row,copy.ui("principle"+i),12,AppUi.SUB,false).setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));}
        ui.callout(p,R.drawable.ic_info,copy.ui("principleNote"));ui.action(p,copy.ui("understand"),true,0,close);
    });}
    void terms(){
        ui.heading(copy.ui("legalDraft"),copy.ui("legalFull"),copy.ui("legalLanguage"));JSONArray rows=copy.terms();
        for(int i=0;i<rows.length();i++){JSONObject row=rows.optJSONObject(i);if(row.optString("type").equals("list")){JSONArray items=row.optJSONArray("items");for(int j=0;j<items.length();j++)legal(ui.page,"• "+items.optString(j));}
            else if(row.optString("type").equals("heading")){ui.space(ui.page,23);ui.text(ui.page,row.optString("text"),15,AppUi.FG,true);ui.space(ui.page,10);}
            else legal(ui.page,row.optString("text"));}
    }
    private void legal(LinearLayout p,String text){rich(p,text,12,AppUi.SUB);ui.space(p,13);}
    void security(){
        JSONObject s=copy.security();ui.heading(copy.ui("securityNav"),s.optString("title"),null);
        if(!Texts.language(ui.activity).equals("zh"))ui.helper(ui.page,copy.ui("securityLanguage"));
        rich(ui.page,s.optString("intro"),13,AppUi.SUB);
        LinearLayout cleanup=ui.callout(ui.page,R.drawable.ic_shield,null);raw(cleanup,"明文私钥在写卡前清理；密文在完成备份并返回首页后清理。相机增强为可选模式。",12,AppUi.SUB,false);ui.space(ui.page,12);
        JSONArray sections=s.optJSONArray("sections");
        for(int i=0;i<sections.length();i++){
            final int index=i;JSONObject section=sections.optJSONObject(i);LinearLayout body=ui.accordion(ui.page,section.optString("title"),expanded.contains(i),on->{if(on)expanded.add(index);else expanded.remove(index);});
            JSONArray paragraphs=section.optJSONArray("paragraphs");for(int j=0;j<paragraphs.length();j++){rich(body,paragraphs.optString(j),12,AppUi.SUB);ui.space(body,12);}
        }
        LinearLayout relatedBody=ui.accordion(ui.page,"与哪些开源钱包有相近思路？",expanded.contains(8),on->{if(on)expanded.add(8);else expanded.remove(8);});
        JSONArray related=s.optJSONArray("related");for(int i=0;i<related.length();i++){JSONObject item=related.optJSONObject(i);ui.space(relatedBody,16);raw(relatedBody,item.optString("title"),14,AppUi.FG,true);ui.space(relatedBody,7);rich(relatedBody,item.optString("text"),12,AppUi.SUB);}
        ui.space(relatedBody,12);raw(relatedBody,s.optString("caution"),12,AppUi.FG,true);ui.space(relatedBody,12);rich(relatedBody,s.optString("compatibility"),12,AppUi.SUB);
    }
    private void raw(LinearLayout p,String text,int size,int color,boolean bold){TextView t=ui.text(p,"",size,color,bold);t.setText(text);ui.line(t,1.9f);}
    private void rich(LinearLayout p,String text,int size,int color){
        TextView t=ui.text(p,"",size,color,false);android.text.SpannableStringBuilder out=new android.text.SpannableStringBuilder();String[] pieces=text.split("\\*\\*",-1);
        for(int i=0;i<pieces.length;i++){int start=out.length();out.append(pieces[i]);if((i&1)!=0){out.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),start,out.length(),0);out.setSpan(new android.text.style.ForegroundColorSpan(AppUi.FG),start,out.length(),0);}}t.setText(out);ui.line(t,1.9f);
    }
}
