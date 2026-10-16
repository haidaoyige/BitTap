package io.coingift.core;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Optional camera supplement. The secret system input is fixed BEFORE image capture. */
public final class CameraEntropy implements AutoCloseable {
    public static final int FRAME_COUNT=32;
    private byte[] systemInput,finalDigest;
    private final List<byte[]> frames=new ArrayList<>();
    public CameraEntropy(){this(WalletCrypto.systemRandom());}
    CameraEntropy(SecureRandom random){systemInput=new byte[32];try{random.nextBytes(systemInput);}catch(RuntimeException ex){close();throw ex;}}
    public synchronized int count(){return frames.size();}
    public synchronized void addFrame(byte[] digest){
        requireOpen();requireDigest(digest);if(frames.size()>=FRAME_COUNT)return;
        for(byte[] prior:frames)if(MessageDigest.isEqual(prior,digest))return;
        frames.add(digest.clone());
    }
    public synchronized void finish(byte[] digest){
        requireOpen();requireDigest(digest);
        if(frames.size()!=FRAME_COUNT)throw new IllegalStateException("有效预览帧不足，请重新采集");
        for(byte[] prior:frames)if(MessageDigest.isEqual(prior,digest))throw new IllegalArgumentException("最终照片与预览帧重复，请重新采集");
        WalletCrypto.wipe(finalDigest);finalDigest=digest.clone();
    }
    public synchronized boolean ready(){return systemInput!=null&&finalDigest!=null&&frames.size()==FRAME_COUNT;}
    synchronized byte[] derive() throws GeneralSecurityException {
        if(!ready())throw new GeneralSecurityException("相机增强采集尚未完成");
        byte[] camera=null,candidate=null;
        try {
            MessageDigest hash=MessageDigest.getInstance("SHA-256");
            hash.update("BitTap/camera/v1".getBytes(StandardCharsets.US_ASCII));
            hash.update((byte)FRAME_COUNT);
            for(int i=0;i<frames.size();i++){hash.update((byte)0);hash.update((byte)i);hash.update((byte)32);hash.update(frames.get(i));}
            hash.update((byte)1);hash.update((byte)32);hash.update(finalDigest);camera=hash.digest();
            Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(systemInput,"HmacSHA256"));
            for(int i=0;i<128;i++){
                mac.update("BitTap/private-key/v1".getBytes(StandardCharsets.US_ASCII));mac.update((byte)32);mac.update(camera);
                candidate=mac.doFinal(new byte[]{0,0,0,(byte)i});
                if(WalletCrypto.isValidKey(candidate)){byte[] result=candidate;candidate=null;return result;}
                WalletCrypto.wipe(candidate);
            }
            throw new GeneralSecurityException("随机源异常，已停止生成");
        } finally {WalletCrypto.wipe(camera);WalletCrypto.wipe(candidate);close();}
    }
    private void requireOpen(){if(systemInput==null)throw new IllegalStateException("采集数据已清除，请重新采集");}
    private static void requireDigest(byte[] digest){if(digest==null||digest.length!=32)throw new IllegalArgumentException("图像摘要无效");}
    @Override public synchronized void close(){WalletCrypto.wipe(systemInput);systemInput=null;WalletCrypto.wipe(finalDigest);finalDigest=null;for(byte[] b:frames)WalletCrypto.wipe(b);frames.clear();}
}
