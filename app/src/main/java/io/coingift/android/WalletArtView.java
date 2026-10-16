package io.coingift.android;

import android.content.Context;
import android.animation.ValueAnimator;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.view.View;

/** A local vector illustration; the Bitcoin glyph comes from the original SVG path. */
final class WalletArtView extends View {
    private final boolean scanning;
    private final boolean animated;
    private ValueAnimator animator;
    private float pulse;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds=new RectF();
    private final Drawable logo,lock;
    private final Typeface font=Typeface.create("sans-serif",Typeface.NORMAL);
    public WalletArtView(Context context){this(context,false);}
    WalletArtView(Context context,boolean scanning){this(context,scanning,false);}
    WalletArtView(Context context,boolean scanning,boolean animated){super(context);this.scanning=scanning;this.animated=animated;this.logo=context.getDrawable(R.drawable.ic_brand_white);this.lock=context.getDrawable(R.drawable.ic_lock).mutate();this.lock.setTint(AppUi.ACCENT);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setLayerType(LAYER_TYPE_SOFTWARE,null);}
    @Override protected void onAttachedToWindow(){
        super.onAttachedToWindow();
        if(scanning&&animated&&ValueAnimator.areAnimatorsEnabled()){
            animator=ValueAnimator.ofFloat(0,1);animator.setDuration(1800);animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new android.view.animation.LinearInterpolator());
            animator.addUpdateListener(a->{pulse=(float)a.getAnimatedValue();invalidate();});animator.start();
        }
    }
    @Override protected void onDetachedFromWindow(){
        if(animator!=null){animator.cancel();animator.removeAllUpdateListeners();animator=null;}
        super.onDetachedFromWindow();
    }
    private void spaced(Canvas canvas,String text,float x,float y,float space){for(int i=0;i<text.length();i++){String letter=text.substring(i,i+1);canvas.drawText(letter,x,y,paint);x+=paint.measureText(letter)+space;}}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.save();
        float scale=getResources().getDisplayMetrics().density;
        canvas.translate(getWidth()/2f,getHeight()/2f);canvas.scale(scale,scale);
        if(scanning){
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);paint.setColor(AppUi.LINE);
            canvas.drawCircle(0,0,117.5f,paint);canvas.drawCircle(0,0,99,paint);
            if(animated){paint.setAlpha((int)(160*(1-pulse)));canvas.drawCircle(0,0,71+25*pulse,paint);paint.setAlpha(255);}
        }
        canvas.rotate(-9);float w=233,h=145;
        // Scale to the available illustration surface, preserving the approved card proportions.
        float available=getWidth()/scale-20;float fit=Math.min(1f,available/270f);canvas.scale(fit,fit);
        paint.setStyle(Paint.Style.FILL);paint.setColor(AppUi.ORANGE);bounds.set(-w/2,-h/2,w/2,h/2);paint.setShadowLayer(18,0,18,0x14cc7313);canvas.drawRoundRect(bounds,17,17,paint);paint.clearShadowLayer();
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.8f);paint.setColor(0x45c57216);bounds.set(-w/2+7,-h/2+4.4f,w/2-7,h/2-4.4f);canvas.drawRoundRect(bounds,9,9,paint);
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xff33210d);paint.setTypeface(font);paint.setTextSize(15);
        canvas.drawText("BitTap",-w/2+w*.08f,-h/2+h*.12f+14,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);paint.setStrokeCap(Paint.Cap.ROUND);float cx=w/2-w*.08f-7,cy=-h/2+h*.12f+7;
        for(int r=3;r<=7;r+=2){bounds.set(cx-r,cy-r,cx+r,cy+r);canvas.drawArc(bounds,-45,90,false,paint);}
        float left=-w/2+w*.08f,bottom=h/2-h*.15f;int size=45;logo.setBounds((int)left,(int)(bottom-size),(int)left+size,(int)bottom);logo.draw(canvas);
        float textX=left+55,mid=bottom-size/2f;paint.setStyle(Paint.Style.FILL);paint.setTextSize(9);paint.setColor(0xff33210d);
        spaced(canvas,"BITCOIN · OFFLINE",textX,mid-3,1);paint.setTextSize(7.2f);paint.setColor(0xff71471d);spaced(canvas,"YOUR KEYS. YOUR BITCOIN.",textX,mid+9,.15f);
        canvas.restore();
        if(!scanning){canvas.save();canvas.scale(scale,scale);float right=getWidth()/scale-12,badgeBottom=getHeight()/scale-22;bounds.set(right-58,badgeBottom-58,right,badgeBottom);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setShadowLayer(14,0,8,0x08594024);canvas.drawRoundRect(bounds,18,18,paint);paint.clearShadowLayer();paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);paint.setColor(AppUi.LINE);canvas.drawRoundRect(bounds,18,18,paint);lock.setBounds((int)right-41,(int)badgeBottom-41,(int)right-17,(int)badgeBottom-17);lock.draw(canvas);canvas.restore();}
    }
}
