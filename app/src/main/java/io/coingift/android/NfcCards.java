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
    static final class Result {
        final String text,cardId;final byte[] uid;private Candidate candidate;
        Result(String text,String cardId,byte[] uid,Candidate candidate){this.text=text;this.cardId=cardId;this.uid=uid;this.candidate=candidate;}
        Candidate takeCandidate(){Candidate value=candidate;candidate=null;return value;}
        void clear(){WalletCrypto.wipe(uid);if(candidate!=null){WalletCrypto.wipe(candidate.uid);WalletCrypto.wipe(candidate.previous);candidate=null;}}
    }
    /** Immutable transport inputs only: a blocked tag cannot retain an Activity or private session. */
    static NfcIo.Work task(Tag tag,String operation,String encrypted,Candidate candidate,boolean protect,boolean duplicate){
        Candidate snapshot=candidate==null?null:new Candidate(candidate.uid,candidate.previous.clone(),candidate.capacity);
        return new NfcIo.Work(){
            public Object run(NfcIo.Job job)throws Exception{
                job.check();byte[] uid=tag.getId();if(uid==null||uid.length==0)throw new IllegalArgumentException("无法识别卡片，请换一张卡片");
                if(operation.equals("READ"))return new Result(read(tag,job),WalletCrypto.hex(uid),null,null);
                if(encrypted==null)throw new IllegalStateException("请先生成冷钱包");
                if(operation.equals("CHECK")){
                    if(duplicate)throw new IllegalArgumentException("这张卡片已保存此钱包，请换另一张 NFC 卡");
                    return new Result(null,null,null,check(tag,encrypted,protect,job));
                }
                if(snapshot==null)throw new IllegalStateException("请先检查卡片");
                if(operation.equals("WRITE"))writeAndVerify(tag,snapshot,encrypted,job);
                else if(operation.equals("LOCK"))makeReadOnly(tag,snapshot,encrypted,job);
                else if(operation.equals("VERIFY_LOCK"))verifyReadOnly(tag,snapshot,encrypted,job);
                else throw new IllegalArgumentException("Invalid NFC operation");
                return new Result(null,null,uid.clone(),null);
            }
            public void discard(Object value){if(value instanceof Result)((Result)value).clear();}
            public void close(){if(snapshot!=null){WalletCrypto.wipe(snapshot.uid);WalletCrypto.wipe(snapshot.previous);}}
        };
    }
    static NdefMessage message(String encrypted) {
        return new NdefMessage(NdefRecord.createMime(MIME, encrypted.getBytes(StandardCharsets.US_ASCII)));
    }
    static final class ReadOnlyCard extends IllegalArgumentException {
        ReadOnlyCard(){super("这张卡片已设为只读，不能重新写入。请换一张可写卡片。");}
    }
    /** Cached metadata can reject a tag before RF I/O. Success still requires a live readback. */
    interface Card extends java.io.Closeable {
        boolean writable();int capacity();void connect()throws Exception;
        byte[] read()throws Exception;void write(byte[] message)throws Exception;
    }
    private static Card card(Tag tag){
        Ndef n=Ndef.get(tag);
        if(n==null)throw new IllegalArgumentException("请使用已格式化为 NDEF 的卡片；本版不会自动格式化其他卡片");
        return new Card(){
            public boolean writable(){return n.isWritable();}public int capacity(){return n.getMaxSize();}
            public void connect()throws Exception{n.connect();}
            public byte[] read()throws Exception{NdefMessage value=n.getNdefMessage();return value==null?new byte[0]:value.toByteArray();}
            public void write(byte[] value)throws Exception{n.writeNdefMessage(new NdefMessage(value));}
            public void close()throws java.io.IOException{n.close();}
        };
    }
    private static void writable(Card card,int required){
        if(!card.writable())throw new ReadOnlyCard();
        if(required>card.capacity())throw new IllegalArgumentException("卡片容量不足：需要 "+required+" 字节，可用 "+card.capacity()+" 字节");
    }
    static Candidate check(Tag tag,String encrypted,NfcIo.Job job)throws Exception{
        return check(tag.getId(),card(tag),message(encrypted).toByteArray().length,job);
    }
    static Candidate check(byte[] uid,Card card,int required,NfcIo.Job job)throws Exception{
        job.check();if(uid==null||uid.length==0)throw new IllegalArgumentException("无法识别卡片，请换一张卡片");
        writable(card,required); // No connect, read, close or lock command for an already read-only tag.
        job.own(card);
        try{
            job.check();card.connect();job.check();writable(card,required);
            byte[] old=card.read();job.check();return new Candidate(uid,old,card.capacity());
        }finally{job.release(card);}
    }
    static Candidate check(Tag tag,String encrypted,boolean protect,NfcIo.Job job) throws Exception {
        Candidate result=check(tag,encrypted,job);
        try {
            if(protect)NtagProtection.inspect(tag,job);
            return result;
        } catch(Exception ex){WalletCrypto.wipe(result.uid);WalletCrypto.wipe(result.previous);throw ex;}
    }
    static void makeReadOnly(Tag tag,Candidate candidate,String encrypted,NfcIo.Job job) throws Exception {
        job.phase(NfcIo.Phase.MATCH_WRITTEN);requireSameWrittenCard(tag,candidate,encrypted,job);
        // Only explicit NTAG215/216 maps are accepted. Avoid the platform generic lock routine.
        job.phase(NfcIo.Phase.HARDWARE_LOCK);NtagProtection.lock(tag,job);
    }
    static void verifyReadOnly(Tag tag,Candidate candidate,String encrypted,NfcIo.Job job) throws Exception {
        job.phase(NfcIo.Phase.VERIFY_WRITTEN);requireSameWrittenCard(tag,candidate,encrypted,job);
        job.phase(NfcIo.Phase.VERIFY_BITS);NtagProtection.verify(tag,job);
        job.phase(NfcIo.Phase.VERIFY_READONLY);Ndef n=open(tag,job);try{if(n.isWritable())throw new IllegalStateException("卡片仍然可写，锁定未确认");}finally{job.release(n);}
    }
    private static void requireSameWrittenCard(Tag tag,Candidate candidate,String encrypted,NfcIo.Job job) throws Exception {
        if(candidate==null||!Arrays.equals(tag.getId(),candidate.uid))throw new IllegalArgumentException("请贴回刚才写入的同一张卡片");
        Ndef n=open(tag,job);try{job.check();NdefMessage actual=n.getNdefMessage();job.check();if(actual==null||!MessageDigest.isEqual(actual.toByteArray(),message(encrypted).toByteArray()))throw new IllegalStateException("卡片内容不一致，已停止锁定");}finally{job.release(n);}
    }
    static void writeAndVerify(Tag tag,Candidate candidate,String encrypted,NfcIo.Job job)throws Exception{
        writeAndVerify(tag.getId(),card(tag),candidate,message(encrypted).toByteArray(),job);
    }
    static void writeAndVerify(byte[] uid,Card card,Candidate candidate,byte[] desired,NfcIo.Job job)throws Exception{
        job.check();
        if(candidate==null||!Arrays.equals(uid,candidate.uid))throw new IllegalArgumentException("请贴回刚才检查过的同一张卡片");
        writable(card,desired.length);
        job.own(card);
        try{
            job.check();card.connect();job.check();writable(card,desired.length);
            byte[] previous=card.read();job.check();
            if(!MessageDigest.isEqual(previous,candidate.previous))throw new IllegalArgumentException("卡片内容发生变化，请重新检查");
            writable(card,desired.length);job.check();card.write(desired);job.check();
            // Must read over NFC again, never compare against discovery-time cached data.
            byte[] actual=card.read();job.check();
            if(!MessageDigest.isEqual(actual,desired))throw new IllegalStateException("写入后读回核验失败，请勿充值，重新读卡检查");
        }finally{job.release(card);}
    }
    static String read(Tag tag,NfcIo.Job job) throws Exception {
        Ndef ndef = open(tag,job);
        try {
            job.check();NdefMessage value = ndef.getNdefMessage();job.check();
            return parse(value);
        } finally { job.release(ndef); }
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
    private static Ndef open(Tag tag,NfcIo.Job job) throws Exception {
        Ndef ndef = Ndef.get(tag);
        if (ndef == null) throw new IllegalArgumentException("请使用已格式化为 NDEF 的卡片；本版不会自动格式化其他卡片");
        job.own(ndef);try{job.check();ndef.connect();job.check();return ndef;}catch(Exception ex){job.release(ndef);throw ex;}
    }
}
