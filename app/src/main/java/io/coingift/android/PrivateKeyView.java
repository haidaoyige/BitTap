package io.coingift.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import java.nio.CharBuffer;
import java.util.Arrays;

/** Draw-only WIF presentation. The key is never assigned to a TextView or accessibility text. */
@android.annotation.SuppressLint("ViewConstructor") // Programmatic only; never inflated from XML.
final class PrivateKeyView extends View {
    private static final String MASK="••••  ••••  ••••  ••••";
    private final TextPaint paint;
    private final float spacingExtra,spacingMultiplier;
    private final int breakStrategy;
    private char[] characters;
    private StaticLayout layout;
    private int layoutWidth=-1;

    PrivateKeyView(Context context,char[] wif){
        super(context);
        // Match the previous TextView's metrics, including line height set before MONOSPACE.
        // This unattached, empty widget is used only for theme metrics; it never receives a key.
        TextView metrics=new TextView(context);
        metrics.setTextSize(13);metrics.setTextColor(AppUi.FG);metrics.setTypeface(Typeface.DEFAULT);
        metrics.setIncludeFontPadding(false);metrics.setLineHeight(Math.round(metrics.getTextSize()*1.75f));
        metrics.setTypeface(Typeface.MONOSPACE);metrics.setGravity(Gravity.CENTER);
        metrics.setSingleLine(false);metrics.setHorizontallyScrolling(false);
        metrics.setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE);
        paint=new TextPaint(metrics.getPaint());paint.setColor(metrics.getCurrentTextColor());
        spacingExtra=metrics.getLineSpacingExtra();spacingMultiplier=metrics.getLineSpacingMultiplier();
        breakStrategy=metrics.getBreakStrategy();
        int padding=Math.round(12*getResources().getDisplayMetrics().density);
        setPadding(padding,padding,padding,padding);
        characters=wif.clone();
        setSaveEnabled(false);setSaveFromParentEnabled(false);
        setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        if(Build.VERSION.SDK_INT>=30)setImportantForContentCapture(IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS);
        setContentDescription(context.getString(R.string.private_sensitive_content));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        if(Build.VERSION.SDK_INT>=34)setAccessibilityDataSensitive(ACCESSIBILITY_DATA_SENSITIVE_YES);
    }

    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getSize(widthSpec);
        if(MeasureSpec.getMode(widthSpec)==MeasureSpec.UNSPECIFIED)
            width=(int)Math.ceil(paint.measureText(characters==null?MASK:CharBuffer.wrap(characters),0,characters==null?MASK.length():characters.length))+getPaddingLeft()+getPaddingRight();
        ensureLayout(Math.max(1,width-getPaddingLeft()-getPaddingRight()));
        setMeasuredDimension(resolveSize(width,widthSpec),resolveSize(layout.getHeight()+getPaddingTop()+getPaddingBottom(),heightSpec));
    }
    private void ensureLayout(int width){
        if(layout!=null&&layoutWidth==width)return;
        CharSequence text=characters==null?MASK:CharBuffer.wrap(characters);
        layout=StaticLayout.Builder.obtain(text,0,text.length(),paint,width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .setIncludePad(false).setLineSpacing(spacingExtra,spacingMultiplier)
                .setBreakStrategy(breakStrategy).setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE).build();
        layoutWidth=width;
    }
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);ensureLayout(Math.max(1,getWidth()-getPaddingLeft()-getPaddingRight()));
        canvas.save();canvas.translate(getPaddingLeft(),getPaddingTop()+Math.max(0,(getHeight()-getPaddingTop()-getPaddingBottom()-layout.getHeight())/2));
        layout.draw(canvas);canvas.restore();
    }
    @Override public int getBaseline(){
        return layout==null?super.getBaseline():getPaddingTop()+Math.max(0,(getMeasuredHeight()-getPaddingTop()-getPaddingBottom()-layout.getHeight())/2)+layout.getLineBaseline(0);
    }
    void clear(){
        if(characters!=null){Arrays.fill(characters,'\0');characters=null;}
        layout=null;layoutWidth=-1;requestLayout();invalidate();
    }
    boolean isCleared(){return characters==null;}
    char[] copyCharacters(){if(characters==null)throw new IllegalStateException("Private display is closed");return characters.clone();}
    @Override protected void onDetachedFromWindow(){clear();super.onDetachedFromWindow();}
}
