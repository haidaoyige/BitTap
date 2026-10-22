package io.coingift.android;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import com.google.zxing.common.BitMatrix;
import io.coingift.core.BitcoinAddresses;

/** Public receiving-address artwork with export-time verification state, rendered offline at 3x. */
final class AddressImageRenderer {
    private static final int SCALE=3,WIDTH=408;
    private static final int BG=0xfffffcf7,INK=0xff28221c,MUTED=0xff8a7a69,LINE=0xffeee3d5;
    private AddressImageRenderer(){}

    static String addressType(String address){
        BitcoinAddresses.requireMainnetAddress(address);
        if(address.startsWith("bc1q"))return "SegWit";
        if(address.startsWith("bc1p"))return "Taproot";
        return address.startsWith("3")?"P2SH":"Legacy";
    }
    static Bitmap render(Context context,String address,boolean verified)throws Exception{
        String type=addressType(address);
        TextPaint addressPaint=font(15,INK,false);addressPaint.setTypeface(Typeface.MONOSPACE);
        StaticLayout addressLines=StaticLayout.Builder.obtain(address,0,address.length(),addressPaint,322)
                .setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false)
                .setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_SIMPLE).setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                .setLineSpacing(7,1).build();
        StaticLayout warning=null;
        if(!verified){
            String text=context.getString(R.string.address_image_unverified);
            warning=StaticLayout.Builder.obtain(text,0,text.length(),font(12,0xff8c4b13,false),299)
                    .setIncludePad(false).setBreakStrategy(android.graphics.text.LineBreaker.BREAK_STRATEGY_SIMPLE)
                    .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE).setLineSpacing(5,1).build();
        }
        float warningTop=193,warningHeight=warning==null?0:26+Math.max(16,warning.getHeight());
        float qrTop=warning==null?200:warningTop+warningHeight+22;
        float addressTop=qrTop+296+22,addressHeight=14+14+9+addressLines.getHeight()+16;
        float footerY=addressTop+addressHeight+24;
        int height=(int)Math.ceil(footerY+56);
        Bitmap output=Bitmap.createBitmap(WIDTH*SCALE,height*SCALE,Bitmap.Config.ARGB_8888);
        try{
            output.eraseColor(Color.TRANSPARENT);
            Canvas c=new Canvas(output);c.scale(SCALE,SCALE);
            RectF bounds=new RectF(.5f,.5f,WIDTH-.5f,height-.5f);
            Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(BG);c.drawRoundRect(bounds,25,25,p);
            p.setColor(LINE);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);
            c.drawRoundRect(bounds,25,25,p);
            Drawable logo=context.getDrawable(R.drawable.ic_launcher);
            logo.setBounds(28,30,67,69);logo.draw(c);
            c.drawText("BitTap",77,58,font(23,INK,true));
            TextPaint network=font(11,0xff8e7560,false);network.setTextAlign(Paint.Align.RIGHT);
            c.drawText(context.getString(R.string.btc_mainnet),380,54,network);
            c.drawText(context.getString(R.string.address_image_title),28,132,font(27,INK,true));
            TextPaint typePaint=font(12,0xffa95000,false);
            p.setStyle(Paint.Style.FILL);p.setColor(0xfffff0db);
            c.drawRoundRect(new RectF(28,150,28+29+typePaint.measureText(type),176),8,8,p);
            p.setColor(AppUi.ORANGE);c.drawCircle(41.5f,163,2.5f,p);c.drawText(type,51,167.5f,typePaint);

            if(warning!=null){
                p.setColor(0xfffff0db);c.drawRoundRect(new RectF(28,warningTop,380,warningTop+warningHeight),12,12,p);
                Drawable alert=context.getDrawable(R.drawable.ic_warning).mutate();alert.setTint(0xff8c4b13);
                c.save();c.translate(42,warningTop+16);alert.setBounds(0,0,16,16);alert.draw(c);c.restore();
                c.save();c.translate(67,warningTop+13);warning.draw(c);c.restore();
            }
            RectF qrBounds=new RectF(56,qrTop,352,qrTop+296);
            p.setColor(Color.WHITE);c.drawRoundRect(qrBounds,18,18,p);
            // Re-encode at export resolution; retain square black modules and a four-module quiet zone.
            Bitmap qr=qr(address,296*SCALE);
            try{
                Paint sharp=new Paint();sharp.setFilterBitmap(false);
                Path clip=new Path();clip.addRoundRect(qrBounds,18,18,Path.Direction.CW);
                c.save();c.clipPath(clip);c.drawBitmap(qr,null,qrBounds,sharp);c.restore();
            }
            finally{qr.eraseColor(Color.WHITE);qr.recycle();}
            p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(0xfff0e7dc);
            c.drawRoundRect(qrBounds,18,18,p);

            p.setStyle(Paint.Style.FILL);p.setColor(0xfff6f1e9);
            c.drawRoundRect(new RectF(28,addressTop,380,addressTop+addressHeight),12,12,p);
            TextPaint label=font(11,MUTED,false);label.setTextAlign(Paint.Align.CENTER);
            c.drawText(context.getString(R.string.address_image_label),204,addressTop+25,label);
            c.save();c.translate(43,addressTop+37);addressLines.draw(c);c.restore();

            p.setColor(LINE);p.setStrokeWidth(1);c.drawLine(28,footerY,380,footerY,p);
            p.setColor(AppUi.ORANGE);c.drawRoundRect(new RectF(28,footerY+23,53,footerY+26),2,2,p);
            TextPaint tagline=font(9,0xff978674,false);tagline.setLetterSpacing(.05f);tagline.setTextAlign(Paint.Align.RIGHT);
            c.drawText(context.getString(R.string.address_image_tagline),380,footerY+28,tagline);
            return output;
        }catch(Exception|Error failure){output.eraseColor(Color.WHITE);output.recycle();throw failure;}
    }
    private static TextPaint font(float size,int color,boolean bold){
        TextPaint p=new TextPaint(Paint.ANTI_ALIAS_FLAG);p.setTextSize(size);p.setColor(color);
        p.setTypeface(bold?Typeface.create(Typeface.DEFAULT,600,false):Typeface.create("sans-serif",Typeface.NORMAL));return p;
    }
    private static Bitmap qr(String address,int size)throws Exception{
        BitMatrix matrix=QrCodeEncoder.address(address,size);int[] pixels=new int[size*size];
        try{
            for(int y=0;y<size;y++)for(int x=0;x<size;x++)pixels[y*size+x]=matrix.get(x,y)?Color.BLACK:Color.WHITE;
            Bitmap bitmap=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels,0,size,0,0,size,size);return bitmap;
        }finally{java.util.Arrays.fill(pixels,0);matrix.clear();}
    }
}
