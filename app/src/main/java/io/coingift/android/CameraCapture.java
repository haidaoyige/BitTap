package io.coingift.android;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import io.coingift.core.CameraEntropy;
import io.coingift.core.WalletCrypto;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;

/** In-app Camera2 capture; hashes valid YUV pixels, never saves pictures/files or EXIF. */
final class CameraCapture implements AutoCloseable {
    interface Listener {void progress(int frames);void complete(CameraEntropy result,android.graphics.Bitmap photo);void failed();}
    private final Activity activity;private final TextureView view;private final Listener listener;
    private final HandlerThread thread=new HandlerThread("BitTapCapture");private final Handler handler;
    private final SecureRandom schedule=new SecureRandom();
    private CameraEntropy entropy;private CameraDevice device;private CameraCaptureSession session;private ImageReader reader;private Surface preview;
    private volatile boolean closed,finalRequested;private long finalTimestamp=-1,nextSample;
    // Non-sensitive failure category, used by regression checks; never stores image/key data.
    private String failureStage;
    private final Map<Long,byte[]> finalCandidates=new LinkedHashMap<>();
    private final Map<Long,android.graphics.Bitmap> photos=new LinkedHashMap<>();
    CameraCapture(Activity activity,TextureView view,Listener listener){
        this.activity=activity;this.view=view;this.listener=listener;entropy=new CameraEntropy();thread.start();handler=new Handler(thread.getLooper());
        handler.postDelayed(()->{if(!closed)fail("timeout");},90000);
        if(view.isAvailable())open();else view.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){
            public void onSurfaceTextureAvailable(SurfaceTexture t,int w,int h){open();}
            public void onSurfaceTextureSizeChanged(SurfaceTexture t,int w,int h){}
            public boolean onSurfaceTextureDestroyed(SurfaceTexture t){close();return true;}
            public void onSurfaceTextureUpdated(SurfaceTexture t){}
        });
    }
    @android.annotation.SuppressLint("MissingPermission") private void open(){
        if(closed)return;
        try{
            CameraManager manager=(CameraManager)activity.getSystemService(Activity.CAMERA_SERVICE);String selected=null;Size chosen=null;
            for(String id:manager.getCameraIdList()){
                CameraCharacteristics c=manager.getCameraCharacteristics(id);
                Integer facing=c.get(CameraCharacteristics.LENS_FACING);if(facing==null||facing!=CameraCharacteristics.LENS_FACING_BACK)continue;
                android.hardware.camera2.params.StreamConfigurationMap map=c.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);if(map==null)continue;
                Size[] sizes=map.getOutputSizes(ImageFormat.YUV_420_888);if(sizes==null)continue;
                for(Size size:sizes)if(size.getWidth()<=640&&size.getHeight()<=480&&(chosen==null||size.getWidth()*size.getHeight()>chosen.getWidth()*chosen.getHeight())){selected=id;chosen=size;}
                if(selected!=null)break;
            }
            if(selected==null||chosen==null)throw new IllegalStateException();
            SurfaceTexture texture=view.getSurfaceTexture();if(texture==null)throw new IllegalStateException();
            texture.setDefaultBufferSize(chosen.getWidth(),chosen.getHeight());preview=new Surface(texture);
            reader=ImageReader.newInstance(chosen.getWidth(),chosen.getHeight(),ImageFormat.YUV_420_888,3);
            reader.setOnImageAvailableListener(this::image,handler);
            manager.openCamera(selected,new CameraDevice.StateCallback(){
                public void onOpened(CameraDevice camera){if(closed){camera.close();return;}device=camera;configure();}
                public void onDisconnected(CameraDevice camera){camera.close();fail();}
                public void onError(CameraDevice camera,int error){camera.close();fail();}
            },handler);
        }catch(Exception ex){fail("open");}
    }
    @SuppressWarnings("deprecation") private void configure(){
        try{device.createCaptureSession(Arrays.asList(preview,reader.getSurface()),new CameraCaptureSession.StateCallback(){
            public void onConfigured(CameraCaptureSession value){if(closed){value.close();return;}session=value;try{CaptureRequest.Builder b=device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);b.addTarget(preview);b.addTarget(reader.getSurface());session.setRepeatingRequest(b.build(),null,handler);}catch(Exception ex){fail();}}
            public void onConfigureFailed(CameraCaptureSession value){fail();}
        },handler);}catch(Exception ex){fail();}
    }
    private void image(ImageReader source){
        try(Image image=source.acquireLatestImage()){
            if(image==null||closed)return;
            long now=SystemClock.elapsedRealtime();if(!finalRequested&&now<nextSample)return;
            byte[] digest=digest(image);
            try{
                if(finalRequested){
                    finalCandidates.put(image.getTimestamp(),digest.clone());photos.put(image.getTimestamp(),photo(image));
                    while(finalCandidates.size()>4){Long first=finalCandidates.keySet().iterator().next();WalletCrypto.wipe(finalCandidates.remove(first));clearPhoto(photos.remove(first));}
                    finishIfReady();
                }else{entropy.addFrame(digest);nextSample=now+100+schedule.nextInt(151);int count=entropy.count();activity.runOnUiThread(()->{if(!closed)listener.progress(count);});}
            }finally{WalletCrypto.wipe(digest);}
        }catch(Exception ex){fail("image");}
    }
    static byte[] digest(Image image) throws Exception {
        if(image.getFormat()!=ImageFormat.YUV_420_888||image.getWidth()<=0||image.getHeight()<=0||image.getWidth()>640||image.getHeight()>480)throw new IllegalArgumentException();
        MessageDigest d=MessageDigest.getInstance("SHA-256");
        d.update("BitTap/YUV/v1".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        d.update(ByteBuffer.allocate(12).putInt(image.getFormat()).putInt(image.getWidth()).putInt(image.getHeight()).array());
        Image.Plane[] planes=image.getPlanes();if(planes.length!=3)throw new IllegalArgumentException();
        for(int i=0;i<3;i++){
            int width=i==0?image.getWidth():(image.getWidth()+1)/2,height=i==0?image.getHeight():(image.getHeight()+1)/2;
            Image.Plane p=planes[i];ByteBuffer b=p.getBuffer().duplicate();int base=b.position(),row=p.getRowStride(),pixel=p.getPixelStride();
            if(row<=0||pixel<=0||(long)base+(long)(height-1)*row+(long)(width-1)*pixel>=b.limit())throw new IllegalArgumentException();
            d.update((byte)i);d.update(ByteBuffer.allocate(4).putInt(width*height).array());
            for(int y=0;y<height;y++)for(int x=0;x<width;x++)d.update(b.get(base+y*row+x*pixel));
        }
        return d.digest();
    }
    private static android.graphics.Bitmap photo(Image image){
        int w=image.getWidth(),h=image.getHeight();Image.Plane[] planes=image.getPlanes();ByteBuffer[] buffers={planes[0].getBuffer().duplicate(),planes[1].getBuffer().duplicate(),planes[2].getBuffer().duplicate()};int[] pixels=new int[w*h];
        try{for(int y=0;y<h;y++)for(int x=0;x<w;x++){
            int yy=(buffers[0].get(buffers[0].position()+y*planes[0].getRowStride()+x*planes[0].getPixelStride())&255)-16;
            int u=(buffers[1].get(buffers[1].position()+(y/2)*planes[1].getRowStride()+(x/2)*planes[1].getPixelStride())&255)-128;
            int v=(buffers[2].get(buffers[2].position()+(y/2)*planes[2].getRowStride()+(x/2)*planes[2].getPixelStride())&255)-128;
            int c=Math.max(0,yy)*298;int r=clamp((c+409*v+128)>>8),g=clamp((c-100*u-208*v+128)>>8),b=clamp((c+516*u+128)>>8);pixels[y*w+x]=0xff000000|(r<<16)|(g<<8)|b;
        }android.graphics.Bitmap result=android.graphics.Bitmap.createBitmap(w,h,android.graphics.Bitmap.Config.ARGB_8888);result.setPixels(pixels,0,w,0,0,w,h);return result;}finally{Arrays.fill(pixels,0);}
    }
    private static int clamp(int value){return Math.max(0,Math.min(255,value));}
    private static void clearPhoto(android.graphics.Bitmap photo){if(photo!=null&&!photo.isRecycled()){photo.eraseColor(android.graphics.Color.TRANSPARENT);photo.recycle();}}
    void capture(){handler.post(()->{try{
        if(closed||finalRequested||entropy.count()!=CameraEntropy.FRAME_COUNT||session==null)throw new IllegalStateException();
        finalRequested=true;session.stopRepeating();CaptureRequest.Builder b=device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);b.addTarget(reader.getSurface());
        session.capture(b.build(),new CameraCaptureSession.CaptureCallback(){
            @Override public void onCaptureStarted(CameraCaptureSession s,CaptureRequest r,long timestamp,long frame){finalTimestamp=timestamp;finishIfReady();}
            @Override public void onCaptureFailed(CameraCaptureSession s,CaptureRequest r,CaptureFailure f){fail();}
        },handler);
    }catch(Exception ex){fail();}});}
    private void finishIfReady(){
        if(closed||finalTimestamp<0)return;byte[] result=finalCandidates.get(finalTimestamp);if(result==null)return;
        try{entropy.finish(result);CameraEntropy owned=entropy;android.graphics.Bitmap ownedPhoto=photos.remove(finalTimestamp);if(ownedPhoto==null)throw new IllegalStateException();entropy=null;close();activity.runOnUiThread(()->listener.complete(owned,ownedPhoto));}catch(Exception ex){fail(ex instanceof IllegalArgumentException?"repeated-final-image":"final-image");}
    }
    private void fail(){fail("camera-request");}
    private void fail(String stage){if(closed)return;failureStage=stage;close();activity.runOnUiThread(listener::failed);}
    @Override public void close(){
        if(closed)return;closed=true;
        handler.post(()->{try{if(session!=null)session.close();if(device!=null)device.close();if(reader!=null)reader.close();if(preview!=null)preview.release();}finally{if(entropy!=null){entropy.close();entropy=null;}for(byte[] b:finalCandidates.values())WalletCrypto.wipe(b);finalCandidates.clear();for(android.graphics.Bitmap photo:photos.values())clearPhoto(photo);photos.clear();thread.quitSafely();}});
    }
}
