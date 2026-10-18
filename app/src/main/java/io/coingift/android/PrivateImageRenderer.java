package io.coingift.android;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import com.google.zxing.common.BitMatrix;

/** Explicit plaintext WIF export only. Caller must erase and recycle the returned bitmap. */
final class PrivateImageRenderer {
    private static final int SCALE=3,WIDTH=408,LEFT=27,RIGHT=381,CONTENT=354;
    private static final int BG=0xfffffcf7,INK=0xff28221c,MUTED=0xff8a7a69,LINE=0xffeee3d5;
    private PrivateImageRenderer(){}

    static Bitmap render(Context context,String wif)throws Exception{
        BitMatrix matrix=QrCodeEncoder.wif(wif,284*SCALE);
        try{return artwork(context,wif,matrix);}finally{matrix.clear();}
    }
    private static Bitmap artwork(Context context,String wif,BitMatrix matrix){
        Block title=block(context.getString(R.string.private_image_title),27,INK,true,CONTENT,false);
        Block subtitle=block(context.getString(R.string.private_image_subtitle),12,MUTED,false,CONTENT,false);
        Block alertTitle=block(context.getString(R.string.private_image_risk_title),14,0xff99412c,true,CONTENT-54,false);
        Block alertBody=block(context.getString(R.string.private_image_risk_body),13,0xff9c5a43,false,CONTENT-30,false);
        Block keyLabel=block(context.getString(R.string.private_image_key_label),11,MUTED,false,CONTENT-30,true);
        TextPaint keyPaint=font(13,INK,false);keyPaint.setTypeface(Typeface.MONOSPACE);
        Block key=new Block(wif,keyPaint,CONTENT-30,true,5);
        Block moveTitle=block(context.getString(R.string.private_image_move_title),14,INK,true,CONTENT-35,false);
        Block moveBody=block(context.getString(R.string.private_image_move_body),13,0xff7c6e5e,false,CONTENT-35,false);
        Block stopTitle=block(context.getString(R.string.private_image_stop_title),14,INK,true,CONTENT-35,false);
        Block stopBody=block(context.getString(R.string.private_image_stop_body),13,0xff7c6e5e,false,CONTENT-35,false);
        Block notice=block(context.getString(R.string.private_image_notice),11,0xffa05a42,false,CONTENT,true);
        float titleTop=92,subtitleTop=titleTop+title.height()+8;
        float alertTop=subtitleTop+subtitle.height()+20;
        float alertHeight=14+Math.max(17,alertTitle.height())+7+alertBody.height()+14;
        float qrTop=alertTop+alertHeight+20,keyTop=qrTop+284+18;
        float keyHeight=13+keyLabel.height()+9+key.height()+15;
        float moveTop=keyTop+keyHeight+23,stopTop=moveTop+moveTitle.height()+4+moveBody.height()+18;
        float noticeTop=stopTop+stopTitle.height()+4+stopBody.height()+19;
        float footerY=noticeTop+notice.height()+22;
        int height=(int)Math.ceil(footerY+56);
        Bitmap output=Bitmap.createBitmap(WIDTH*SCALE,height*SCALE,Bitmap.Config.ARGB_8888);
        try{
            output.eraseColor(Color.TRANSPARENT);
            Canvas c=new Canvas(output);c.scale(SCALE,SCALE);
            RectF bounds=new RectF(.5f,.5f,WIDTH-.5f,height-.5f);
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(BG);c.drawRoundRect(bounds,25,25,p);
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(LINE);
            c.drawRoundRect(bounds,25,25,p);
            icon(context,c,R.drawable.ic_launcher,LEFT,28,39,null);
            c.drawText("BitTap",LEFT+49,56,font(23,INK,true));
            TextPaint network=font(11,0xff8e7560,false);network.setTextAlign(Paint.Align.RIGHT);
            c.drawText(context.getString(R.string.btc_mainnet),RIGHT,52,network);
            title.draw(c,LEFT,titleTop);subtitle.draw(c,LEFT,subtitleTop);
            RectF alert=new RectF(LEFT,alertTop,RIGHT,alertTop+alertHeight);
            p.setStyle(Paint.Style.FILL);p.setColor(0xfffff0e9);c.drawRoundRect(alert,13,13,p);
            p.setStyle(Paint.Style.STROKE);p.setColor(0xfff2d2c2);c.drawRoundRect(alert,13,13,p);
            warning(c,LEFT+15,alertTop+14,0xff99412c);
            alertTitle.draw(c,LEFT+39,alertTop+14);alertBody.draw(c,LEFT+15,alertTop+14+Math.max(17,alertTitle.height())+7);

            RectF qrBounds=new RectF(62,qrTop,346,qrTop+284);
            Bitmap qr=qr(matrix);
            try{
                Path clip=new Path();clip.addRoundRect(qrBounds,17,17,Path.Direction.CW);
                Paint sharp=new Paint();sharp.setFilterBitmap(false);
                c.save();c.clipPath(clip);c.drawBitmap(qr,null,qrBounds,sharp);c.restore();
            }finally{qr.eraseColor(Color.WHITE);qr.recycle();}
            p.setStyle(Paint.Style.STROKE);p.setColor(LINE);p.setStrokeWidth(1);c.drawRoundRect(qrBounds,17,17,p);
            p.setStyle(Paint.Style.FILL);p.setColor(0xfff6f1e9);
            c.drawRoundRect(new RectF(LEFT,keyTop,RIGHT,keyTop+keyHeight),12,12,p);
            keyLabel.draw(c,LEFT+15,keyTop+13);key.draw(c,LEFT+15,keyTop+13+keyLabel.height()+9);
            stepIcon(context,c,moveTop,false);moveTitle.draw(c,LEFT+35,moveTop);moveBody.draw(c,LEFT+35,moveTop+moveTitle.height()+4);
            stepIcon(context,c,stopTop,true);stopTitle.draw(c,LEFT+35,stopTop);stopBody.draw(c,LEFT+35,stopTop+stopTitle.height()+4);
            notice.draw(c,LEFT,noticeTop);
            p.setColor(LINE);c.drawRect(LEFT,footerY,RIGHT,footerY+1,p);
            p.setColor(AppUi.ORANGE);c.drawRoundRect(new RectF(LEFT,footerY+23,LEFT+25,footerY+26),2,2,p);
            TextPaint footer=font(9,0xff978674,false);footer.setLetterSpacing(.05f);footer.setTextAlign(Paint.Align.RIGHT);
            c.drawText(context.getString(R.string.address_image_tagline),RIGHT,footerY+28,footer);
            return output;
        }catch(Exception|Error failure){output.eraseColor(Color.WHITE);output.recycle();throw failure;}
    }
    private static void stepIcon(Context context,Canvas c,float y,boolean stop){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(0xfffff0db);
        c.drawRoundRect(new RectF(LEFT,y+1,LEFT+25,y+26),8,8,p);
        if(!stop){icon(context,c,R.drawable.ic_arrow_up_right,LEFT+5,y+6,15,AppUi.ACCENT);return;}
        p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f);p.setColor(AppUi.ACCENT);
        c.drawCircle(LEFT+12.5f,y+13.5f,6,p);c.drawLine(LEFT+8.2f,y+9.2f,LEFT+16.8f,y+17.8f,p);
    }
    private static void warning(Canvas c,float x,float y,int color){
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.2f);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeCap(Paint.Cap.ROUND);
        Path triangle=new Path();triangle.moveTo(x+8.5f,y+1);triangle.lineTo(x+16,y+15);triangle.lineTo(x+1,y+15);triangle.close();c.drawPath(triangle,p);
        c.drawLine(x+8.5f,y+6,x+8.5f,y+9,p);c.drawPoint(x+8.5f,y+12,p);
    }
    private static void icon(Context context,Canvas c,int id,float x,float y,int size,Integer color){
        Drawable d=context.getDrawable(id).mutate();if(color!=null)d.setTint(color);
        c.save();c.translate(x,y);d.setBounds(0,0,size,size);d.draw(c);c.restore();
    }
    private static TextPaint font(float size,int color,boolean bold){
        TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);p.setTextSize(size);p.setColor(color);
        p.setTypeface(bold?Typeface.create(Typeface.DEFAULT,600,false):Typeface.DEFAULT);return p;
    }
    private static Block block(String text,int size,int color,boolean bold,int width,boolean center){return new Block(text,font(size,color,bold),width,center,size>=20?4:5);}
    private static final class Block {
        private final StaticLayout layout;
        // SIMPLE inlines the API 23-compatible value 0; no API 29 class is accessed at runtime.
        @android.annotation.SuppressLint("InlinedApi")
        Block(String text,TextPaint paint,int width,boolean center,int gap){
            layout=StaticLayout.Builder.obtain(text,0,text.length(),paint,width).setIncludePad(false)
                    .setAlignment(center?Layout.Alignment.ALIGN_CENTER:Layout.Alignment.ALIGN_NORMAL)
                    .setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_SIMPLE)
                    .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE).setLineSpacing(gap,1).build();
        }
        int height(){return layout.getHeight();}
        void draw(Canvas c,float x,float y){c.save();c.translate(x,y);layout.draw(c);c.restore();}
    }
    private static Bitmap qr(BitMatrix matrix){
        int size=matrix.getWidth();int[] pixels=new int[size*size];
        try{
            for(int y=0;y<size;y++)for(int x=0;x<size;x++)pixels[y*size+x]=matrix.get(x,y)?Color.BLACK:Color.WHITE;
            Bitmap bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);bitmap.setPixels(pixels,0,size,0,0,size,size);return bitmap;
        }finally{java.util.Arrays.fill(pixels,0);}
    }
}
