package io.coingift.android;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.widget.Switch;

/** Native Switch semantics/touch handling, with the prototype's exact 40 x 24 track. */
final class PrototypeSwitch extends Switch {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF track=new RectF();
    PrototypeSwitch(Context context){super(context);setBackgroundColor(Color.TRANSPARENT);setSaveEnabled(false);}
    private float px(float dp){return dp*getResources().getDisplayMetrics().density;}
    private StaticLayout label(int width){TextPaint textPaint=new TextPaint(getPaint());textPaint.setColor(getCurrentTextColor());return StaticLayout.Builder.obtain(getText(),0,getText().length(),textPaint,Math.max(1,width))
        .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).setLineSpacing(0,1.35f).build();}
    @Override public void onMeasure(int widthSpec,int heightSpec){
        super.onMeasure(widthSpec,heightSpec);int labelWidth=(int)(getMeasuredWidth()-px(40+12+24));
        int required=Math.max((int)px(44),label(labelWidth).getHeight());setMeasuredDimension(getMeasuredWidth(),resolveSize(Math.max(getMeasuredHeight(),required),heightSpec));
    }
    @Override protected void onDraw(Canvas canvas){
        float right=getWidth()-getPaddingRight(),left=right-px(40),center=getHeight()/2f;
        paint.setColor(isChecked()?AppUi.ORANGE:AppUi.LINE);track.set(left,center-px(12),right,center+px(12));canvas.drawRoundRect(track,px(12),px(12),paint);
        paint.setColor(Color.WHITE);canvas.drawCircle(left+px(isChecked()?28:12),center,px(9),paint);
        Drawable icon=getCompoundDrawablesRelative()[0];float textLeft=getPaddingLeft();
        if(icon!=null){canvas.save();canvas.translate(textLeft,center-px(8.5f));icon.setBounds(0,0,(int)px(17),(int)px(17));icon.draw(canvas);canvas.restore();textLeft+=px(24);}
        StaticLayout text=label((int)(left-px(12)-textLeft));canvas.save();canvas.translate(textLeft,center-text.getHeight()/2f);text.draw(canvas);canvas.restore();
    }
}
