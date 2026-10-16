package io.coingift.android;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import io.coingift.core.WalletCrypto;

/** Native NFC I/O; invoke only on a worker thread. Never treats a tag UID as a security credential. */
final class NfcCards {
    private static final String MIME = "application/vnd.bittap.card";
    private static final String PREVIOUS_MIME = "application/vnd.bitnfc.card";
    private static final String LEGACY_MIME = "application/vnd.coingift.card";
    static final class Candidate {
        final byte[] uid, previous;
        final int capacity;
        final boolean hasData;
        Candidate(Tag tag, byte[] previous, int capacity) { this(tag.getId(),previous,capacity); }
        Candidate(byte[] uid, byte[] previous, int capacity) {
            this.uid = uid.clone(); this.previous = previous; this.capacity = capacity;
            this.hasData = previous.length > 0;
        }
    }
    static NdefMessage message(String encrypted) {
        return new NdefMessage(NdefRecord.createMime(MIME, encrypted.getBytes(StandardCharsets.US_ASCII)));
    }
    static Candidate check(Tag tag, String encrypted) throws Exception {
        if (tag.getId() == null || tag.getId().length == 0) throw new IllegalArgumentException("无法识别卡片，请换一张卡片");
        Ndef ndef = open(tag);
        try {
            if (!ndef.isWritable()) throw new IllegalArgumentException("这张卡片是只读的，请换一张卡片");
            int required = message(encrypted).toByteArray().length;
            if (required > ndef.getMaxSize()) throw new IllegalArgumentException("卡片容量不足：需要 " + required + " 字节，可用 " + ndef.getMaxSize() + " 字节");
            NdefMessage old = ndef.getNdefMessage();
            return new Candidate(tag, old == null ? new byte[0] : old.toByteArray(), ndef.getMaxSize());
        } finally { ndef.close(); }
    }
    static Candidate check(Tag tag,String encrypted,boolean protect) throws Exception {
        Candidate result=check(tag,encrypted);
        try {
            if(protect){Ndef n=Ndef.get(tag);if(n==null||!n.canMakeReadOnly())throw new IllegalArgumentException("这张卡片不支持永久只读，请换卡或返回关闭该选项");NtagProtection.inspect(tag);}
            return result;
        } catch(Exception ex){WalletCrypto.wipe(result.uid);WalletCrypto.wipe(result.previous);throw ex;}
    }
    static void makeReadOnly(Tag tag,Candidate candidate,String encrypted) throws Exception {
        requireSameWrittenCard(tag,candidate,encrypted);
        NtagProtection.inspect(tag);
        Ndef n=open(tag);
        try{if(!n.makeReadOnly())throw new IllegalStateException("永久只读操作未确认，请重新读卡检查");}finally{n.close();}
        NtagProtection.lock(tag);
    }
    static void verifyReadOnly(Tag tag,Candidate candidate,String encrypted) throws Exception {
        requireSameWrittenCard(tag,candidate,encrypted);NtagProtection.verify(tag);
        Ndef n=open(tag);try{if(n.isWritable())throw new IllegalStateException("卡片仍然可写，锁定未确认");}finally{n.close();}
    }
    private static void requireSameWrittenCard(Tag tag,Candidate candidate,String encrypted) throws Exception {
        if(candidate==null||!Arrays.equals(tag.getId(),candidate.uid))throw new IllegalArgumentException("请贴回刚才写入的同一张卡片");
        Ndef n=open(tag);try{NdefMessage actual=n.getNdefMessage();if(actual==null||!MessageDigest.isEqual(actual.toByteArray(),message(encrypted).toByteArray()))throw new IllegalStateException("卡片内容不一致，已停止锁定");}finally{n.close();}
    }
    static void writeAndVerify(Tag tag, Candidate candidate, String encrypted) throws Exception {
        if (!Arrays.equals(tag.getId(), candidate.uid)) throw new IllegalArgumentException("请贴回刚才检查过的同一张卡片");
        Ndef ndef = open(tag);
        try {
            NdefMessage old = ndef.getNdefMessage();
            byte[] previous = old == null ? new byte[0] : old.toByteArray();
            if (!MessageDigest.isEqual(previous, candidate.previous)) throw new IllegalArgumentException("卡片内容发生变化，请重新检查");
            NdefMessage desired = message(encrypted);
            if (!ndef.isWritable() || ndef.getMaxSize() < desired.toByteArray().length)
                throw new IllegalArgumentException("卡片不可写或容量不足");
            ndef.writeNdefMessage(desired);
            // Must read over NFC again, never compare against the discovery-time cached message.
            NdefMessage actual = ndef.getNdefMessage();
            if (actual == null || !MessageDigest.isEqual(actual.toByteArray(), desired.toByteArray()))
                throw new IllegalStateException("写入后读回核验失败，请勿充值，重新读卡检查");
        } finally { ndef.close(); }
    }
    static String read(Tag tag) throws Exception {
        Ndef ndef = open(tag);
        try {
            NdefMessage value = ndef.getNdefMessage();
            return parse(value);
        } finally { ndef.close(); }
    }
    static String parse(NdefMessage value) {
            if (value == null) throw new IllegalArgumentException("这是一张空卡片");
            String match = null;
            for (NdefRecord record : value.getRecords()) {
                String decoded = null;
                if (record.getTnf() == NdefRecord.TNF_MIME_MEDIA &&
                    (Arrays.equals(record.getType(), MIME.getBytes(StandardCharsets.US_ASCII)) || Arrays.equals(record.getType(), PREVIOUS_MIME.getBytes(StandardCharsets.US_ASCII)) ||
                     Arrays.equals(record.getType(), LEGACY_MIME.getBytes(StandardCharsets.US_ASCII)))) {
                    decoded = decodeAscii(record.getPayload(), 0);
                } else if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
                    byte[] payload = record.getPayload();
                    if (payload.length < 1 || (payload[0] & 0x80) != 0) continue;
                    int start = 1 + (payload[0] & 0x3f);
                    if (start > payload.length) throw new IllegalArgumentException("卡片文本格式不正确");
                    String text = decodeAscii(payload, start);
                    if (WalletCrypto.isBackup(text) || text.startsWith("6P")) decoded = text;
                }
                if (decoded != null) {
                    if (match != null) throw new IllegalArgumentException("卡片包含多份密钥，请使用单密钥卡片");
                    match = decoded;
                }
            }
            if (match == null) throw new IllegalArgumentException("卡片中没有可识别的 BitTap 或 BIP38 数据");
            return match;
    }
    private static String decodeAscii(byte[] bytes, int start) {
        if (bytes.length - start > WalletCrypto.MAX_TEXT_LENGTH) throw new IllegalArgumentException("卡片数据过长");
        for (int i = start; i < bytes.length; i++) if (bytes[i] < 32 || bytes[i] > 126)
            throw new IllegalArgumentException("卡片包含非规范字符");
        return new String(bytes, start, bytes.length - start, StandardCharsets.US_ASCII);
    }
    private static Ndef open(Tag tag) throws Exception {
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) throw new IllegalArgumentException("请使用已格式化为 NDEF 的卡片；本版不会自动格式化其他卡片");
        ndef.connect(); return ndef;
    }
}
