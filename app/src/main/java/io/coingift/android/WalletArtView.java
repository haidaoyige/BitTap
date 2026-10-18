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
    private final Paint iconPaint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds=new RectF();
    private final Drawable logo,lock;
    private Bitmap logoImage,lockImage;
    private final Typeface font=Typeface.create("sans-serif",Typeface.NORMAL);
    public WalletArtView(Context context){this(context,false);}
    WalletArtView(Context context,boolean scanning){this(context,scanning,false);}
    WalletArtView(Context context,boolean scanning,boolean animated){super(context);this.scanning=scanning;this.animated=animated;this.logo=context.getDrawable(R.drawable.ic_brand_white).mutate();this.lock=context.getDrawable(R.drawable.ic_lock).mutate();this.lock.setTint(AppUi.ACCENT);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setLayerType(LAYER_TYPE_SOFTWARE,null);}
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
        release(logoImage);logoImage=null;release(lockImage);lockImage=null;
        super.onDetachedFromWindow();
    }
    /** Rasterize the unchanged vector without rotation, above its final on-screen resolution. */
    private Bitmap iconImage(Drawable drawable,Bitmap cached,float size,float screenScale){
        int pixels=Math.max(1,Math.min(768,(int)Math.ceil(size*screenScale*3)));
        if(cached!=null&&cached.getWidth()==pixels)return cached;
        Bitmap image=Bitmap.createBitmap(pixels,pixels,Bitmap.Config.ARGB_8888);
        try{drawable.setBounds(0,0,pixels,pixels);drawable.draw(new Canvas(image));release(cached);return image;}
        catch(RuntimeException|Error failure){release(image);throw failure;}
    }
    private static void release(Bitmap image){if(image!=null&&!image.isRecycled()){image.eraseColor(Color.TRANSPARENT);image.recycle();}}
    /** Equal-width axis-aligned strokes must use the same phase of the physical pixel grid. */
    private static float strokeCoordinate(float value,int width){float phase=(width&1)==0?0:.5f;return Math.round(value-phase)+phase;}
    private void spaced(Canvas canvas,String text,float x,float y,float space){for(int i=0;i<text.length();i++){String letter=text.substring(i,i+1);canvas.drawText(letter,x,y,paint);x+=paint.measureText(letter)+space;}}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);canvas.save();
        float scale=getResources().getDisplayMetrics().density;
        float available=getWidth()/scale-20;float fit=Math.min(1f,available/270f);
        if(!scanning)fit=Math.min(Math.min(fit,(getWidth()/scale)/340f),Math.max(0,(getHeight()/scale-8)/185f));
        canvas.translate(getWidth()/2f,getHeight()/2f);canvas.scale(scale,scale);
        if(scanning){
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);paint.setColor(AppUi.LINE);
            canvas.drawCircle(0,0,117.5f,paint);canvas.drawCircle(0,0,99,paint);
            if(animated){paint.setAlpha((int)(160*(1-pulse)));canvas.drawCircle(0,0,71+25*pulse,paint);paint.setAlpha(255);}
        }
        canvas.rotate(-9);float w=233,h=145;
        // Scale to the available illustration surface, preserving the approved card proportions.
        canvas.scale(fit,fit);
        paint.setStyle(Paint.Style.FILL);paint.setColor(AppUi.ORANGE);bounds.set(-w/2,-h/2,w/2,h/2);paint.setShadowLayer(18,0,18,0x14cc7313);canvas.drawRoundRect(bounds,17,17,paint);paint.clearShadowLayer();
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(.8f);paint.setColor(0x45c57216);bounds.set(-w/2+7,-h/2+4.4f,w/2-7,h/2-4.4f);canvas.drawRoundRect(bounds,9,9,paint);
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xff33210d);paint.setTypeface(font);paint.setTextSize(15);
        canvas.drawText("BitTap",-w/2+w*.08f,-h/2+h*.12f+14,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1);paint.setStrokeCap(Paint.Cap.ROUND);float cx=w/2-w*.08f-7,cy=-h/2+h*.12f+7;
        for(int r=3;r<=7;r+=2){bounds.set(cx-r,cy-r,cx+r,cy+r);canvas.drawArc(bounds,-45,90,false,paint);}
        float left=-w/2+w*.08f,bottom=h/2-h*.15f;int size=45;
        logoImage=iconImage(logo,logoImage,size,scale*fit);
        // Retain the previous integer bounds, card rotation, symbol size and spacing.
        bounds.set((int)left,(int)(bottom-size),(int)left+size,(int)bottom);canvas.drawBitmap(logoImage,null,bounds,iconPaint);
        float textX=left+55,mid=bottom-size/2f;paint.setStyle(Paint.Style.FILL);paint.setTextSize(9);paint.setColor(0xff33210d);
        spaced(canvas,"BITCOIN · OFFLINE",textX,mid-3,1);paint.setTextSize(7.2f);paint.setColor(0xff71471d);spaced(canvas,"YOUR KEYS. YOUR BITCOIN.",textX,mid+9,.15f);
        canvas.restore();
        if(!scanning){
            float screenScale=scale*fit,centerX=getWidth()/2f,centerY=getHeight()/2f;
            int stroke=Math.max(1,Math.round(screenScale));
            // Keep the existing tile fill and shadow in their original coordinate system.
            canvas.save();canvas.translate(centerX,centerY);canvas.scale(screenScale,screenScale);
            bounds.set(112,30,170,88);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setShadowLayer(14,0,8,0x08594024);canvas.drawRoundRect(bounds,18,18,paint);paint.clearShadowLayer();canvas.restore();
            bounds.set(strokeCoordinate(centerX+112*screenScale,stroke),strokeCoordinate(centerY+30*screenScale,stroke),strokeCoordinate(centerX+170*screenScale,stroke),strokeCoordinate(centerY+88*screenScale,stroke));
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(stroke);paint.setColor(AppUi.LINE);canvas.drawRoundRect(bounds,18*screenScale,18*screenScale,paint);
            int iconPixels=Math.max(1,Math.round(24*screenScale));
            float iconLeft=Math.round(bounds.centerX()-iconPixels/2f),iconTop=Math.round(bounds.centerY()-iconPixels/2f);
            lockImage=iconImage(lock,lockImage,24,iconPixels/24f);
            bounds.set(iconLeft,iconTop,iconLeft+iconPixels,iconTop+iconPixels);canvas.drawBitmap(lockImage,null,bounds,iconPaint);
        }
    }
}
