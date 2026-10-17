package io.coingift.android;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Small native UI toolkit for the approved BitTap flow. No WebView or network assets. */
final class AppUi {
    static final int BG=Color.rgb(255,252,247), FG=Color.rgb(40,34,28), SUB=Color.rgb(120,107,93);
    static final int ORANGE=Color.rgb(247,147,26), INK_ON_ORANGE=Color.rgb(44,25,7);
    static final int SOFT=Color.rgb(255,240,219), LINE=Color.rgb(236,227,215), ACCENT=Color.rgb(163,75,0);
    static final int ERROR=Color.rgb(162,64,53);
    final Activity activity;
    final List<EditText> passwords=new ArrayList<>();
    final List<EditText> inputs=new ArrayList<>();
    final List<View> buttons=new ArrayList<>();
    final BooleanSupplier blocked;
    final Runnable tap;
    LinearLayout page;
    ScrollView scroll;
    Runnable language=()->{};
    Runnable obscuredTouch=()->{};

    AppUi(Activity activity,BooleanSupplier blocked,Runnable tap){this.activity=activity;this.blocked=blocked;this.tap=tap;}
    int dp(int v){return Math.round(v*activity.getResources().getDisplayMetrics().density);}
    LinearLayout vertical(){LinearLayout p=new LinearLayout(activity);p.setOrientation(LinearLayout.VERTICAL);return p;}
    LinearLayout horizontal(){LinearLayout p=new LinearLayout(activity);p.setOrientation(LinearLayout.HORIZONTAL);p.setGravity(Gravity.CENTER_VERTICAL);return p;}
    void install(){
        clearInputs();passwords.clear();inputs.clear();buttons.clear();
        scroll=new ProtectedScrollView(activity,()->obscuredTouch.run());scroll.setFillViewport(true);scroll.setBackgroundColor(BG);
        page=vertical();page.setClipToPadding(false);page.setClipChildren(false);page.setPadding(dp(24),dp(12),dp(24),dp(24));scroll.addView(page);
        activity.setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            if(android.os.Build.VERSION.SDK_INT>=30){
                android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars()|android.view.WindowInsets.Type.displayCutout());
                android.graphics.Insets keyboard=insets.getInsets(android.view.WindowInsets.Type.ime());
                v.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,keyboard.bottom));
            }else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            v.post(this::revealFocusedInput);return insets;
        });
        scroll.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{if(b-t!=ob-ot)v.post(this::revealFocusedInput);});
        scroll.requestApplyInsets();
    }
    private void revealFocusedInput(){
        if(scroll==null)return;View input=scroll.findFocus();
        if(!(input instanceof EditText)||!input.isShown())return;
        View field=(View)input.getParent();if(field.getHeight()==0)return;
        int[] viewport=new int[2],position=new int[2];scroll.getLocationInWindow(viewport);field.getLocationInWindow(position);
        int top=viewport[1]+scroll.getPaddingTop()+dp(12),bottom=viewport[1]+scroll.getHeight()-scroll.getPaddingBottom()-dp(12);
        int delta=position[1]<top?position[1]-top:position[1]+field.getHeight()>bottom?position[1]+field.getHeight()-bottom:0;
        if(delta!=0)scroll.scrollBy(0,delta);
    }
    void clearPasswords(){for(EditText input:passwords)input.getText().clear();}
    void clearInputs(){for(EditText input:inputs)input.getText().clear();}
    GradientDrawable shape(int color,int radius,boolean border){
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));
        if(border)d.setStroke(dp(1),LINE);return d;
    }
    TextView text(LinearLayout parent,String value,int size,int color,boolean bold){
        TextView t=new TextView(activity);t.setText(Texts.tr(activity,value));t.setTextSize(size);t.setTextColor(color);
        t.setTypeface(bold?Typeface.create(Typeface.DEFAULT,600,false):Typeface.DEFAULT);t.setIncludeFontPadding(false);line(t,1.75f);
        parent.addView(t,new LinearLayout.LayoutParams(-1,-2));return t;
    }
    void space(LinearLayout p,int size){p.addView(new View(activity),new LinearLayout.LayoutParams(1,dp(size)));}
    void stretch(){page.addView(new View(activity),new LinearLayout.LayoutParams(1,dp(22),1));}
    boolean compactHome(){return activity.getResources().getDisplayMetrics().heightPixels/activity.getResources().getDisplayMetrics().density<700;}
    /** Keep actions at the bottom and spend spare height around the full card illustration. */
    void fitHomeArtwork(View artwork){fitArtwork(artwork,compactHome()?160:176);}
    void fitArtwork(View artwork,int minimumDp){
        LinearLayout homePage=page;ScrollView homeScroll=scroll;
        Runnable fit=()->{
            if(artwork.getParent()!=homePage||homeScroll.getHeight()==0||artwork.getHeight()==0)return;
            int available=homeScroll.getHeight()-homeScroll.getPaddingTop()-homeScroll.getPaddingBottom();
            int fixed=homePage.getPaddingTop()+homePage.getPaddingBottom();
            for(int i=0;i<homePage.getChildCount();i++){
                View child=homePage.getChildAt(i);if(child.getVisibility()==View.GONE)continue;
                LinearLayout.LayoutParams params=(LinearLayout.LayoutParams)child.getLayoutParams();
                fixed+=params.topMargin+params.bottomMargin;if(child!=artwork)fixed+=child.getHeight();
            }
            int height=Math.max(dp(minimumDp),available-fixed);
            if(artwork.getLayoutParams().height!=height){artwork.getLayoutParams().height=height;artwork.requestLayout();}
        };
        homePage.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->v.post(fit));
        homeScroll.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->v.post(fit));
        homeScroll.post(fit);
    }
    void note(LinearLayout p,String value){TextView t=text(p,value,12,SUB,false);t.setPadding(0,dp(8),0,dp(8));}
    void foot(String value){TextView t=text(page,value,11,SUB,false);t.setGravity(Gravity.CENTER);t.setPadding(0,dp(12),0,0);}
    void line(TextView t,float factor){t.setLineHeight(Math.round(t.getTextSize()*factor));}
    void heading(String kicker,String title,String desc){
        if(kicker!=null){TextView k=text(page,kicker,11,SUB,false);line(k,1.5f);}
        space(page,7);TextView h=text(page,title,27,FG,true);line(h,1.4f);h.setLetterSpacing(-.022f);space(page,10);
        if(desc!=null){TextView t=text(page,desc,13,SUB,false);line(t,1.85f);space(page,17);}
    }
    void progress(int step){
        LinearLayout row=horizontal();
        for(int i=1;i<=3;i++){
            View v=new View(activity);v.setBackground(shape(i<=step?ORANGE:LINE,2,false));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(3),1);if(i>1)p.leftMargin=dp(5);row.addView(v,p);
        }
        page.addView(row);space(page,24);
    }
    void toolbar(boolean home,String title,Runnable back,Runnable help){
        LinearLayout row=horizontal();LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,dp(64));rp.leftMargin=dp(-6);rp.rightMargin=dp(-6);page.addView(row,rp);
        if(home){
            ImageView logo=new ImageView(activity);logo.setImageResource(R.drawable.ic_launcher);logo.setContentDescription("BitTap");row.addView(logo,new LinearLayout.LayoutParams(dp(34),dp(34)));
            TextView name=new TextView(activity);name.setText(R.string.app_name);name.setTextColor(FG);name.setTextSize(21);name.setTypeface(Typeface.create(Typeface.DEFAULT,600,false));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);p.leftMargin=dp(9);row.addView(name,p);
            String code=Texts.language(activity),shortName=code.equals("zh")?"中文":code.equals("en")?"EN":code.equals("ja")?"日本語":"한국어";
            Button chip=new Button(activity);chip.setTag("language-chip");chip.setText(shortName);chip.setAllCaps(false);chip.setTextSize(12);chip.setTextColor(FG);chip.setMinWidth(0);chip.setMinimumWidth(0);chip.setMinHeight(dp(44));chip.setPadding(dp(10),dp(7),dp(10),dp(7));chip.setBackground(shape(Color.TRANSPARENT,20,true));chip.setCompoundDrawablesRelative(drawable(R.drawable.ic_globe,FG,15),null,drawable(R.drawable.ic_chevron_down,FG,15),null);chip.setCompoundDrawablePadding(dp(5));chip.setContentDescription(Texts.tr(activity,"语言 / Language"));wire(chip,language);row.addView(chip);
        }else{
            row.addView(icon(R.drawable.ic_back,"返回",back),new LinearLayout.LayoutParams(dp(44),dp(44)));
            TextView label=new TextView(activity);label.setTag("toolbar-title");label.setText(Texts.tr(activity,title));label.setGravity(Gravity.CENTER);label.setTextSize(20);label.setTextColor(SUB);
            label.setIncludeFontPadding(false);label.setTypeface(Typeface.create(Typeface.DEFAULT,500,false));line(label,1.15f);label.setMaxLines(2);label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.addView(label,new LinearLayout.LayoutParams(0,-1,1));
        }
        if(help!=null)row.addView(icon(R.drawable.ic_help,"使用说明",help),new LinearLayout.LayoutParams(dp(44),dp(44)));
        else row.addView(new View(activity),new LinearLayout.LayoutParams(dp(44),dp(44)));
        space(page,15);
    }
    Drawable drawable(int resource,int color,int size){
        Drawable d=activity.getDrawable(resource).mutate();d.setTint(color);d.setBounds(0,0,dp(size),dp(size));return d;
    }
    ImageButton icon(int resource,String label,Runnable action){
        ImageButton b=new ImageButton(activity);b.setImageDrawable(drawable(resource,SUB,20));b.setContentDescription(Texts.tr(activity,label));
        b.setPadding(dp(10),dp(10),dp(10),dp(10));b.setBackgroundColor(Color.TRANSPARENT);
        wire(b,action);return b;
    }
    private void centeredIcon(Button button,String label,int resource,int color,int size){
        android.text.SpannableString text=new android.text.SpannableString("\ufffc  "+Texts.tr(activity,label));
        text.setSpan(new CenteredIconSpan(drawable(resource,color,size)),0,1,android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        button.setText(text);button.setContentDescription(Texts.tr(activity,label));
    }
    /** Keep the icon centered on the text's font box rather than sitting on its baseline. */
    private static final class CenteredIconSpan extends android.text.style.ReplacementSpan {
        private final Drawable icon;
        CenteredIconSpan(Drawable icon){this.icon=icon;}
        @Override public int getSize(android.graphics.Paint paint,CharSequence text,int start,int end,android.graphics.Paint.FontMetricsInt fm){
            if(fm!=null){
                paint.getFontMetricsInt(fm);
                int center=(fm.ascent+fm.descent)/2,top=center-icon.getBounds().height()/2;
                fm.ascent=Math.min(fm.ascent,top);fm.top=Math.min(fm.top,fm.ascent);
                fm.descent=Math.max(fm.descent,top+icon.getBounds().height());fm.bottom=Math.max(fm.bottom,fm.descent);
            }
            return icon.getBounds().width();
        }
        @Override public void draw(android.graphics.Canvas canvas,CharSequence text,int start,int end,float x,int top,int y,int bottom,android.graphics.Paint paint){
            android.graphics.Paint.FontMetricsInt fm=paint.getFontMetricsInt();
            float iconTop=y+(fm.ascent+fm.descent-icon.getBounds().height())/2f;
            canvas.save();canvas.translate(x,iconTop);icon.draw(canvas);canvas.restore();
        }
    }
    void wire(View v,Runnable action){v.setOnClickListener(w->{if(!blocked.getAsBoolean()){tap.run();action.run();}});buttons.add(v);}
    Button action(LinearLayout parent,String label,boolean primary,int icon,Runnable run){
        Button b=new Button(activity);b.setText(Texts.tr(activity,label));b.setTextSize(14);b.setAllCaps(false);b.setMinHeight(dp(52));
        b.setTextColor(primary?INK_ON_ORANGE:FG);b.setBackground(shape(primary?ORANGE:Color.WHITE,15,!primary));
        b.setPadding(dp(16),dp(12),dp(16),dp(12));b.setGravity(Gravity.CENTER);b.setStateListAnimator(null);b.setTypeface(Typeface.create(Typeface.DEFAULT,550,false));b.setIncludeFontPadding(false);line(b,1.5f);
        if(icon!=0)centeredIcon(b,label,icon,primary?INK_ON_ORANGE:FG,18);
        wire(b,run);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(10);parent.addView(b,p);return b;
    }
    /** Keep the label and embedded icon consistent with the primary action's enabled state. */
    void primaryActionEnabled(Button button,String label,int icon,boolean enabled){
        int foreground=enabled?INK_ON_ORANGE:SUB;
        button.setEnabled(enabled);
        button.setBackground(shape(enabled?ORANGE:0xffe5dfd6,15,false));
        button.setTextColor(foreground);
        if(icon!=0)centeredIcon(button,label,icon,foreground,18);
    }
    Button link(LinearLayout parent,String label,int icon,Runnable run){
        Button b=new Button(activity);b.setText(Texts.tr(activity,label));b.setTextSize(12);b.setAllCaps(false);b.setTextColor(SUB);
        b.setMinHeight(dp(44));b.setPadding(dp(6),dp(8),dp(6),dp(8));b.setBackgroundColor(Color.TRANSPARENT);
        if(icon!=0)centeredIcon(b,label,icon,SUB,15);
        wire(b,run);parent.addView(b,new LinearLayout.LayoutParams(-1,-2));return b;
    }
    Button inline(String label,int resource,Runnable run){
        Button b=new Button(activity);b.setText(Texts.tr(activity,label));b.setTextSize(12);b.setAllCaps(false);b.setTextColor(ACCENT);
        b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setMinHeight(dp(44));b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(0,dp(4),0,dp(4));b.setBackgroundColor(Color.TRANSPARENT);
        if(resource!=0){b.setCompoundDrawablesRelative(drawable(resource,ACCENT,14),null,null,null);b.setCompoundDrawablePadding(dp(5));}
        wire(b,run);return b;
    }
    LinearLayout card(LinearLayout parent){
        LinearLayout panel=vertical();panel.setPadding(dp(16),dp(15),dp(16),dp(10));panel.setBackground(shape(Color.WHITE,18,true));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(10);p.bottomMargin=dp(8);parent.addView(panel,p);return panel;
    }
    void check(String value,boolean checked){
        LinearLayout row=horizontal();row.setPadding(0,dp(6),0,dp(6));page.addView(row);
        ImageView icon=new ImageView(activity);icon.setImageDrawable(drawable(checked?R.drawable.ic_check:R.drawable.ic_circle,ACCENT,17));
        row.addView(icon,new LinearLayout.LayoutParams(dp(18),dp(18)));
        TextView t=new TextView(activity);t.setText(Texts.tr(activity,value));t.setTextColor(SUB);t.setTextSize(12);t.setPadding(dp(9),0,0,0);row.addView(t);
    }
    void successIcon(boolean locked){statusIcon(locked?R.drawable.ic_lock:R.drawable.ic_check);}
    void statusIcon(int resource){
        LinearLayout box=horizontal();box.setGravity(Gravity.CENTER);box.setBackground(shape(SOFT,20,false));
        ImageView icon=new ImageView(activity);icon.setImageDrawable(drawable(resource,ACCENT,24));
        box.addView(icon,new LinearLayout.LayoutParams(dp(28),dp(28)));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(58),dp(58));p.topMargin=dp(8);p.bottomMargin=dp(20);page.addView(box,p);
    }
    EditText input(String label,boolean password){return input(label,password,null);}
    EditText input(String label,boolean password,TextView suffix){return input(page,label,password,suffix);}
    EditText input(LinearLayout parent,String label,boolean password,TextView suffix){
        space(parent,12);LinearLayout head=horizontal();parent.addView(head);TextView title=text(head,label,12,FG,true);title.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));if(suffix!=null)head.addView(suffix);space(parent,8);
        LinearLayout wrap=horizontal();wrap.setBackground(shape(Color.WHITE,13,true));parent.addView(wrap,new LinearLayout.LayoutParams(-1,-2));
        EditText e=new EditText(activity);e.setTextSize(16);e.setTextColor(FG);e.setPadding(dp(14),dp(14),dp(8),dp(14));
        e.setContentDescription(Texts.tr(activity,label));e.setBackgroundColor(Color.TRANSPARENT);e.setSingleLine(password);
        e.setInputType(password?InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD:InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        e.setImeOptions(android.view.inputmethod.EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING);
        e.setSaveEnabled(false);e.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(1024)});
        inputs.add(e);wrap.addView(e,new LinearLayout.LayoutParams(0,-2,1));
        e.setOnFocusChangeListener((v,focused)->{if(focused)v.post(this::revealFocusedInput);});
        if(password){
            e.setMinHeight(dp(52));passwords.add(e);
            ImageButton toggle=icon(R.drawable.ic_eye_closed,"显示"+label,()->{});wrap.addView(toggle,new LinearLayout.LayoutParams(dp(48),dp(48)));
            toggle.setOnClickListener(v->{
                if(blocked.getAsBoolean())return;tap.run();
                boolean hidden=e.getTransformationMethod()!=null;int start=e.getSelectionStart(),end=e.getSelectionEnd();
                e.setTransformationMethod(hidden?null:android.text.method.PasswordTransformationMethod.getInstance());
                e.setSelection(Math.max(0,start),Math.max(0,end));
                toggle.setImageDrawable(drawable(hidden?R.drawable.ic_eye_open:R.drawable.ic_eye_closed,SUB,20));
                toggle.setContentDescription(hidden?Texts.tr(activity,"隐藏")+Texts.tr(activity,label):Texts.tr(activity,"显示")+Texts.tr(activity,label));
            });
        }else{e.setMinLines(4);e.setGravity(Gravity.TOP);}
        return e;
    }
    /** Release detached password fields and listeners when a password sheet is dismissed. */
    void releaseInputs(View view){
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)releaseInputs(group.getChildAt(i));}
        if(view instanceof EditText){EditText input=(EditText)view;input.getText().clear();passwords.remove(input);inputs.remove(input);input.setOnFocusChangeListener(null);input.setOnEditorActionListener(null);}
        if(buttons.remove(view))view.setOnClickListener(null);
    }
    void passwordStrength(EditText input){passwordStrength(page,input);}
    void passwordStrength(LinearLayout parent,EditText input){
        TextView label=text(parent,"",11,SUB,false);label.setPadding(0,dp(6),0,0);label.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        Runnable update=()->{
            io.coingift.core.PasswordStrength.Level level=io.coingift.core.PasswordStrength.assess(input.getText());
            int color=level==io.coingift.core.PasswordStrength.Level.WEAK?ERROR:level==io.coingift.core.PasswordStrength.Level.STRONG?0xff2f6b43:ACCENT;
            label.setText(Texts.tr(activity,switch(level){
                case EMPTY -> "密码强度提示 · 仅供参考";
                case WEAK -> "密码强度：弱 · 可以继续，建议使用更长的独立密码";
                case MEDIUM -> "密码强度：中 · 可以继续，建议再增加长度";
                case STRONG -> "密码强度：较强 · 仍请独立使用并妥善保管";
            }));label.setTextColor(level==io.coingift.core.PasswordStrength.Level.EMPTY?SUB:color);
        };
        input.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence text,int start,int count,int after){}
            public void onTextChanged(CharSequence text,int start,int before,int count){update.run();}
            public void afterTextChanged(android.text.Editable text){}
        });update.run();
    }
    void helper(LinearLayout parent,String value){TextView t=text(parent,value,11,SUB,false);line(t,1.7f);t.setPadding(0,dp(6),0,0);}
    LinearLayout callout(LinearLayout parent,int icon,String value){
        LinearLayout box=horizontal();box.setGravity(Gravity.TOP);box.setPadding(dp(12),dp(12),dp(12),dp(12));box.setBackground(shape(SOFT,13,false));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.topMargin=dp(15);parent.addView(box,bp);
        ImageView image=new ImageView(activity);image.setImageDrawable(drawable(icon,ACCENT,17));LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(17),dp(21));ip.rightMargin=dp(9);box.addView(image,ip);
        LinearLayout body=vertical();box.addView(body,new LinearLayout.LayoutParams(0,-2,1));if(value!=null){TextView t=text(body,value,12,SUB,false);line(t,1.75f);}return body;
    }
    LinearLayout mode(LinearLayout parent){LinearLayout box=vertical();box.setPadding(dp(15),dp(10),dp(15),dp(15));box.setBackground(shape(Color.WHITE,16,true));LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,-2);bp.topMargin=dp(18);parent.addView(box,bp);return box;}
    Switch toggle(LinearLayout parent,String label,int icon,boolean checked,java.util.function.Consumer<Boolean> changed){
        Switch sw=new PrototypeSwitch(activity);sw.setText(label);sw.setTextSize(13);sw.setTextColor(FG);sw.setTypeface(Typeface.create(Typeface.DEFAULT,550,false));sw.setMinHeight(dp(44));sw.setSwitchMinWidth(dp(40));sw.setSwitchPadding(dp(12));
        sw.setCompoundDrawablesRelative(drawable(icon,ACCENT,17),null,null,null);sw.setCompoundDrawablePadding(dp(7));sw.setChecked(checked);parent.addView(sw,new LinearLayout.LayoutParams(-1,-2));buttons.add(sw);
        sw.setOnCheckedChangeListener((v,on)->{if(!blocked.getAsBoolean()){tap.run();changed.accept(on);}});return sw;
    }
    Button textAction(LinearLayout parent,String label,int before,int after,Runnable action){Button b=inline(label,before,action);b.setTextSize(11);if(after!=0){b.setCompoundDrawablesRelative(before==0?null:drawable(before,ACCENT,15),null,drawable(after,ACCENT,15),null);b.setCompoundDrawablePadding(dp(5));}parent.addView(b,new LinearLayout.LayoutParams(-2,-2));return b;}
    void divider(LinearLayout parent){View v=new View(activity);v.setBackgroundColor(LINE);parent.addView(v,new LinearLayout.LayoutParams(-1,dp(1)));}
    LinearLayout risk(LinearLayout parent,int icon,String title,String body,boolean border){
        LinearLayout row=horizontal();row.setGravity(Gravity.TOP);row.setPadding(0,dp(13),0,dp(13));parent.addView(row);ImageView image=new ImageView(activity);image.setImageDrawable(drawable(icon,ACCENT,19));LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(19),dp(23));ip.rightMargin=dp(12);row.addView(image,ip);LinearLayout content=vertical();row.addView(content,new LinearLayout.LayoutParams(0,-2,1));TextView h=text(content,title,13,FG,true);line(h,1.6f);space(content,3);if(body!=null){TextView t=text(content,body,12,SUB,false);line(t,1.8f);}if(border)divider(parent);return content;
    }
    Button option(LinearLayout parent,String title,String subtitle,boolean selected,int icon,Runnable action){
        Button b=action(parent,title,false,0,action);b.setTag(selected?"option-selected":"option");b.setTypeface(Typeface.DEFAULT);b.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);b.setTextSize(14);b.setPadding(dp(13),dp(12),dp(13),dp(12));b.setBackground(shape(selected?SOFT:Color.WHITE,13,true));
        if(selected){GradientDrawable bg=shape(SOFT,13,true);bg.setStroke(dp(1),0xfff5bf7c);b.setBackground(bg);}
        if(subtitle!=null){android.text.SpannableString value=new android.text.SpannableString(title+"\n"+subtitle);value.setSpan(new android.text.style.AbsoluteSizeSpan(11,true),title.length()+1,value.length(),0);value.setSpan(new android.text.style.ForegroundColorSpan(SUB),title.length()+1,value.length(),0);b.setText(value);}
        b.setContentDescription(title);if(icon!=0)b.setCompoundDrawablesRelative(null,null,drawable(icon,ACCENT,18),null);return b;
    }
    LinearLayout accordion(LinearLayout parent,String title,boolean open,java.util.function.Consumer<Boolean> changed){
        LinearLayout section=vertical();parent.addView(section);Button header=link(section,title,R.drawable.ic_chevron_right,()->{});header.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);header.setTextSize(13);header.setTypeface(Typeface.create(Typeface.DEFAULT,550,false));header.setMinHeight(dp(52));header.setCompoundDrawablesRelative(null,null,drawable(open?R.drawable.ic_chevron_down:R.drawable.ic_chevron_right,ACCENT,17),null);header.setText(title);header.setContentDescription(title);header.setTag("accordion-header");
        LinearLayout body=vertical();section.addView(body);body.setVisibility(open?View.VISIBLE:View.GONE);wire(header,()->{boolean expanded=body.getVisibility()!=View.VISIBLE;body.setVisibility(expanded?View.VISIBLE:View.GONE);header.setCompoundDrawablesRelative(null,null,drawable(expanded?R.drawable.ic_chevron_down:R.drawable.ic_chevron_right,ACCENT,17),null);changed.accept(expanded);});divider(section);return body;
    }
    Dialog sheet(String title,Consumer<LinearLayout> builder){
        Dialog d=new Dialog(activity);showSheet(d,title,builder,()->d.dismiss());return d;
    }
    Dialog consentSheet(String title,Consumer<LinearLayout> builder){Dialog d=new Dialog(activity);showSheet(d,title,builder,null);return d;}
    TextView showSheet(Dialog d,String title,Consumer<LinearLayout> builder){
        return showSheet(d,title,builder,null);
    }
    TextView showSheet(Dialog d,String title,Consumer<LinearLayout> builder,Runnable close){
        return showSheet(d,title,builder,close,0);
    }
    TextView showSheet(Dialog d,String title,Consumer<LinearLayout> builder,Runnable close,int leadingIcon){
        return showSheet(d,title,builder,close,leadingIcon,false);
    }
    TextView showSheet(Dialog d,String title,Consumer<LinearLayout> builder,Runnable close,int leadingIcon,boolean expanded){
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout panel=vertical();panel.setPadding(dp(24),dp(13),dp(24),dp(25));panel.setBackground(shape(Color.WHITE,25,false));
        View grab=new View(activity);grab.setBackground(shape(LINE,3,false));LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(dp(32),dp(4));gp.gravity=Gravity.CENTER_HORIZONTAL;gp.bottomMargin=dp(23);panel.addView(grab,gp);
        TextView heading;
        if(leadingIcon!=0){
            LinearLayout row=horizontal();panel.addView(row);
            android.widget.FrameLayout badge=new android.widget.FrameLayout(activity);badge.setBackground(shape(SOFT,13,false));row.addView(badge,new LinearLayout.LayoutParams(dp(44),dp(44)));
            ImageView symbol=new ImageView(activity);symbol.setImageDrawable(drawable(leadingIcon,ACCENT,22));badge.addView(symbol,new android.widget.FrameLayout.LayoutParams(dp(22),dp(22),Gravity.CENTER));
            row.addView(new View(activity),new LinearLayout.LayoutParams(0,1,1));
            if(close!=null)row.addView(icon(R.drawable.ic_close,"关闭",close),new LinearLayout.LayoutParams(dp(44),dp(44)));
            space(panel,17);heading=text(panel,title,24,FG,true);
        }else if(close==null)heading=text(panel,title,22,FG,true);
        else{
            LinearLayout row=horizontal();panel.addView(row);
            heading=text(row,title,22,FG,true);heading.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
            row.addView(icon(R.drawable.ic_close,"关闭",close),new LinearLayout.LayoutParams(dp(44),dp(44)));
        }
        line(heading,1.4f);space(panel,10);builder.accept(panel);
        ScrollView scroll=new ProtectedScrollView(activity,()->obscuredTouch.run()){
            @Override protected void onMeasure(int width,int height){
                int max=(int)(getResources().getDisplayMetrics().heightPixels*(expanded?.95:.85));
                super.onMeasure(width,MeasureSpec.makeMeasureSpec(max,MeasureSpec.AT_MOST));
            }
        };
        scroll.addView(panel);d.setContentView(scroll);d.setCanceledOnTouchOutside(true);
        Window window=d.getWindow();
        if(window!=null){window.setBackgroundDrawableResource(android.R.color.transparent);WindowProtection.secure(window);window.setGravity(Gravity.BOTTOM);window.setDimAmount(.31f);window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);}
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets bars=insets.getInsets(android.view.WindowInsets.Type.systemBars());android.graphics.Insets ime=insets.getInsets(android.view.WindowInsets.Type.ime());v.setPadding(0,0,0,Math.max(bars.bottom,ime.bottom));}
            v.post(()->{View focus=v.findFocus();if(focus instanceof EditText)focus.requestRectangleOnScreen(new android.graphics.Rect(0,0,focus.getWidth(),focus.getHeight()),true);});return insets;
        });
        d.show();if(window!=null)window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);return heading;
    }
}
