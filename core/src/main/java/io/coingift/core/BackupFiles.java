package io.coingift.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Save encrypted backups, then verify the closed document by reading it back. */
public final class BackupFiles {
    private BackupFiles(){}
    public interface Open<T>{T open() throws IOException;}
    public static void writeAndVerify(String encrypted,Open<OutputStream> output,Open<InputStream> input) throws IOException {
        if(!WalletCrypto.isBackup(encrypted))throw new IllegalArgumentException("只能导出加密备份");
        new WalletCrypto().inspect(encrypted);
        byte[] bytes=encrypted.getBytes(StandardCharsets.US_ASCII);
        try(OutputStream out=output.open()){
            if(out==null)throw new IOException("无法打开备份文件");
            out.write(bytes);out.flush();
        }
        // A failing close must never be reported as a completed backup.
        try(InputStream in=input.open()){
            if(in==null)throw new IOException("无法核对备份文件");
            if(!MessageDigest.isEqual(bytes,readBounded(in)))throw new IOException("备份文件核对不一致");
        }
    }
    public static byte[] readBounded(InputStream input) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[256];int count;
        while((count=input.read(buffer))!=-1){
            out.write(buffer,0,count);
            if(out.size()>WalletCrypto.MAX_TEXT_LENGTH+2)throw new IOException("备份文件过大");
        }
        return out.toByteArray();
    }
}
