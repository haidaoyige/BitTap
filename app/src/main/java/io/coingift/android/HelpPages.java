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
    Dialog disclaimer(Runnable agree,Runnable full,Runnable close){
        Dialog owner=new Dialog(ui.activity);
        ui.riskSheet(owner,copy.ui("disclaimer"),p->{
            TextView loss=ui.text(p,copy.ui("riskNoticeLoss"),13,AppUi.ACCENT,true);
            loss.setPadding(ui.dp(14),ui.dp(13),ui.dp(14),ui.dp(13));
            loss.setBackground(ui.shape(AppUi.SOFT,12,false));ui.line(loss,1.5f);
            int[] items={1,2,4},icons={R.drawable.ic_wallet,R.drawable.ic_key,R.drawable.ic_warning};
            for(int i=0;i<items.length;i++){
                LinearLayout content=ui.risk(p,icons[i],copy.ui("risk"+items[i]),copy.ui("risk"+items[i]+"body"),i<items.length-1);
                ((View)content.getParent()).setTag("risk-notice-row");
                ((View)content.getParent()).setPadding(0,ui.dp(17),0,ui.dp(17));
                TextView title=(TextView)content.getChildAt(0);title.setTextSize(15);ui.line(title,1.5f);
                content.getChildAt(1).getLayoutParams().height=ui.dp(6);
                TextView body=(TextView)content.getChildAt(content.getChildCount()-1);body.setTextSize(14);ui.line(body,1.75f);
            }
            TextView liability=ui.text(p,copy.ui("riskNoticeLiability"),12,AppUi.SUB,false);
            liability.setPadding(0,ui.dp(16),0,ui.dp(6));ui.line(liability,1.75f);
            LinearLayout termsRow=ui.horizontal();termsRow.setGravity(android.view.Gravity.CENTER);p.addView(termsRow,new LinearLayout.LayoutParams(-1,-2));
            Button terms=ui.textAction(termsRow,copy.ui("readFullDisclaimer"),0,R.drawable.ic_chevron_right,full);terms.setTextSize(12);
            for(Button action:new Button[]{ui.action(p,copy.ui("agree"),true,R.drawable.ic_check_mark,agree),ui.action(p,copy.ui("disagree"),false,0,close)}){
                action.setTextSize(15);action.setMinHeight(ui.dp(50));action.setMinimumHeight(ui.dp(50));
                action.setPadding(ui.dp(12),ui.dp(12),ui.dp(12),ui.dp(12));ui.line(action,1.4f);
                ((LinearLayout.LayoutParams)action.getLayoutParams()).topMargin=ui.dp(8);
            }
            TextView session=ui.text(p,copy.ui("riskNoticeSession"),11,AppUi.SUB,false);
            session.setGravity(android.view.Gravity.CENTER);session.setPadding(0,ui.dp(8),0,0);ui.line(session,1.5f);
        });return owner;
    }
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
    Dialog about(Runnable close,Runnable security,Runnable language,Runnable verification){return ui.sheet(copy.ui("about"),p->{
        ui.text(p,copy.ui("aboutBody"),13,AppUi.SUB,false);ui.space(p,2);ui.callout(p,R.drawable.ic_shield,copy.ui("publicInfo"));ui.space(p,8);
        ui.action(p,copy.ui("securityEntry"),false,R.drawable.ic_shield,security);
        ui.action(p,ui.activity.getString(R.string.verification_entry),false,R.drawable.ic_record_check,verification);
        if(language!=null)ui.link(p,copy.ui("language"),R.drawable.ic_globe,language);
        ui.action(p,copy.ui("understand"),true,0,close);
    });}
    void verification(){
        ui.heading(null,ui.activity.getString(R.string.verification_entry),ui.activity.getString(R.string.verification_intro));
        int[] titles={R.string.verification_when_title,R.string.verification_private_title,R.string.verification_storage_title,R.string.verification_reset_title};
        int[] bodies={R.string.verification_when_body,R.string.verification_private_body,R.string.verification_storage_body,R.string.verification_reset_body};
        int[] icons={R.drawable.ic_record_check,R.drawable.ic_lock,R.drawable.ic_shield,R.drawable.ic_shuffle};
        for(int i=0;i<titles.length;i++)ui.risk(ui.page,icons[i],ui.activity.getString(titles[i]),ui.activity.getString(bodies[i]),i<titles.length-1);
    }
    Dialog principle(Runnable close){return ui.sheet(copy.ui("principleTitle"),p->{
        int[] icons={R.drawable.ic_shield,R.drawable.ic_camera,R.drawable.ic_shuffle};
        for(int i=1;i<=3;i++){LinearLayout row=ui.horizontal();row.setPadding(0,ui.dp(6),0,ui.dp(6));p.addView(row);ImageView image=new ImageView(ui.activity);image.setImageDrawable(ui.drawable(icons[i-1],AppUi.ACCENT,17));LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(ui.dp(17),ui.dp(20));ip.rightMargin=ui.dp(8);row.addView(image,ip);TextView text=ui.text(row,copy.ui("principle"+i),12,AppUi.SUB,false);text.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));row.setGravity(android.view.Gravity.TOP);text.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->ui.alignFirstLine(image,text));}
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
        rich(ui.page,s.optString("intro"),13,AppUi.SUB);
        LinearLayout cleanup=ui.callout(ui.page,R.drawable.ic_shield,null);raw(cleanup,s.optString("cleanup"),12,AppUi.SUB,false);ui.space(ui.page,12);
        JSONArray sections=s.optJSONArray("sections");
        for(int i=0;i<sections.length();i++){
            final int index=i;JSONObject section=sections.optJSONObject(i);LinearLayout body=ui.accordion(ui.page,section.optString("title"),expanded.contains(i),on->{if(on)expanded.add(index);else expanded.remove(index);});
            JSONArray paragraphs=section.optJSONArray("paragraphs");for(int j=0;j<paragraphs.length();j++){rich(body,paragraphs.optString(j),12,AppUi.SUB);ui.space(body,12);}
        }
        LinearLayout relatedBody=ui.accordion(ui.page,s.optString("relatedTitle"),expanded.contains(8),on->{if(on)expanded.add(8);else expanded.remove(8);});
        JSONArray related=s.optJSONArray("related");for(int i=0;i<related.length();i++){JSONObject item=related.optJSONObject(i);ui.space(relatedBody,16);raw(relatedBody,item.optString("title"),14,AppUi.FG,true);ui.space(relatedBody,7);rich(relatedBody,item.optString("text"),12,AppUi.SUB);}
        ui.space(relatedBody,12);raw(relatedBody,s.optString("caution"),12,AppUi.FG,true);ui.space(relatedBody,12);rich(relatedBody,s.optString("compatibility"),12,AppUi.SUB);
    }
    private void raw(LinearLayout p,String text,int size,int color,boolean bold){TextView t=ui.text(p,"",size,color,bold);t.setText(text);ui.line(t,1.9f);}
    private void rich(LinearLayout p,String text,int size,int color){
        TextView t=ui.text(p,"",size,color,false);android.text.SpannableStringBuilder out=new android.text.SpannableStringBuilder();String[] pieces=text.split("\\*\\*",-1);
        for(int i=0;i<pieces.length;i++){int start=out.length();out.append(pieces[i]);if((i&1)!=0){out.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),start,out.length(),0);out.setSpan(new android.text.style.ForegroundColorSpan(AppUi.FG),start,out.length(),0);}}t.setText(out);ui.line(t,1.9f);
    }
}
