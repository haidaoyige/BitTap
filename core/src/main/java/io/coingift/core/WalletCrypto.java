package io.coingift.core;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.base.Base58;
import org.bitcoinj.base.LegacyAddress;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.base.SegwitAddress;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.params.MainNetParams;
import org.bouncycastle.crypto.generators.SCrypt;

/** All methods run offline. Never log inputs, generated keys, or passwords. */
public final class WalletCrypto {
    public static final String PREFIX = "CG2:";
    public static final String NOTE_PREFIX = "CG3:";
    public static final String LEGACY_PREFIX = "CG1:";
    public static final int MAX_TEXT_LENGTH = 1024;
    private static final int N = 32768, R = 8, P = 1;
    private static final byte[] MAGIC = {'C', 'G', 'F'};
    // Capture the platform RNG before bitcoinj initializes any legacy Android provider.
    private static final SecureRandom SYSTEM_RANDOM = new SecureRandom();
    private final SecureRandom random;

    public enum Network {
        MAINNET(0);
        final int id;
        Network(int id) { this.id = id; }
        NetworkParameters params() { return MainNetParams.get(); }
    }
    public static final class Card {
        public final String encrypted, address, publicKey;
        public final BitcoinAddresses addresses;
        public final Network network;
        Card(String encrypted, String address, String publicKey, Network network, BitcoinAddresses addresses) {
            this.encrypted = encrypted; this.address = address; this.publicKey = publicKey; this.network = network;
            this.addresses = addresses;
        }
    }
    public static final class Metadata {
        public final String address, note;
        /** Full public address claims for CG2/CG3; null for a legacy CG1 header. Not authenticated yet. */
        public final BitcoinAddresses addresses;
        public final int version;
        public final Network network;
        final byte[] salt, nonce, aad, ciphertext;
        Metadata(String address, BitcoinAddresses addresses, int version, Network network, byte[] salt, byte[] nonce, byte[] aad, byte[] ciphertext, String note) {
            this.address = address; this.network = network; this.salt = salt; this.nonce = nonce;
            this.addresses=addresses; this.version=version;
            this.aad = aad; this.ciphertext = ciphertext; this.note=note;
        }
    }
    /** Best-effort zeroization. JVM/library objects may retain copies; no hardware isolation is claimed. */
    public static final class Unlocked implements AutoCloseable {
        private byte[] privateKey;
        public final String address, publicKey, note;
        public final Network network;
        public final String legacyAddress;
        public final BitcoinAddresses addresses;
        Unlocked(byte[] privateKey, Network network) {this(privateKey,network,"");}
        Unlocked(byte[] privateKey, Network network, String note) {
            this.note=note;
            requireValidKey(privateKey);
            ECKey key = publicKeyOf(privateKey);
            this.addresses = BitcoinAddresses.fromKey(key);
            this.privateKey = privateKey.clone(); this.network = network;
            this.address = addressOf(key, network);
            this.publicKey = hex(key.getPubKey());
            this.legacyAddress = LegacyAddress.fromKey(network.params(), key).toString();
        }
        public String wif() {
            if (privateKey == null) throw new IllegalStateException("私钥已清除，请重新解密");
            byte[] payload = Arrays.copyOf(privateKey, 33);
            payload[32] = 1;
            try { return Base58.encodeChecked(network.params().getDumpedPrivateKeyHeader(), payload); }
            finally { wipe(payload); }
        }
        @Override public void close() { wipe(privateKey); privateKey = null; }
    }

    static SecureRandom systemRandom(){return SYSTEM_RANDOM;}
    public WalletCrypto() { this(SYSTEM_RANDOM); }
    // Injection is package-private and only used by deterministic tests; production never seeds its RNG.
    WalletCrypto(SecureRandom random) { this.random = random; }

    public Card generate(char[] password, Network network) throws GeneralSecurityException {
        requireNewPassword(password);
        byte[] privateKey = new byte[32];
        try {
            // Rejection sampling, never modulo reduction. RNG failure propagates with no fallback.
            for (int attempts = 0; ; attempts++) {
                if (attempts >= 128) throw new GeneralSecurityException("随机源异常，已停止生成");
                random.nextBytes(privateKey);
                if (isValidKey(privateKey)) break;
            }
            String encrypted = encrypt(privateKey, password, network);
            // Verify the exact payload before discarding the generated private key.
            try (Unlocked restored = decrypt(encrypted, password)) {
                if (!MessageDigest.isEqual(privateKey, restored.privateKey))
                    throw new GeneralSecurityException("生成后的恢复校验失败");
                return new Card(encrypted, restored.address, restored.publicKey, network, restored.addresses);
            }
        } finally { wipe(privateKey); }
    }

    public Card generate(char[] password, Network network, String note, CameraEntropy camera) throws GeneralSecurityException {
        byte[] privateKey=null;
        try {
            requireNewPassword(password);note=WalletNote.normalize(note);
            if(camera!=null)privateKey=camera.derive();
            else {privateKey=new byte[32];for(int i=0;;i++){if(i>=128)throw new GeneralSecurityException("随机源异常，已停止生成");random.nextBytes(privateKey);if(isValidKey(privateKey))break;}}
            String encrypted=encrypt(privateKey,password,network,note,true);
            try(Unlocked restored=decrypt(encrypted,password)){
                if(!MessageDigest.isEqual(privateKey,restored.privateKey)||!note.equals(restored.note))throw new GeneralSecurityException("生成后的恢复校验失败");
                return new Card(encrypted,restored.address,restored.publicKey,network,restored.addresses);
            }
        } finally {wipe(privateKey);if(camera!=null)camera.close();}
    }

    // Kept package-private so the UI cannot accidentally encrypt externally supplied malformed keys.
    String encrypt(byte[] privateKey, char[] password, Network network) throws GeneralSecurityException {
        return encrypt(privateKey,password,network,"",false);
    }
    String encrypt(byte[] privateKey, char[] password, Network network, String note, boolean withNote) throws GeneralSecurityException {
        requireValidKey(privateKey);note=WalletNote.normalize(note);
        BitcoinAddresses addresses = BitcoinAddresses.fromKey(publicKeyOf(privateKey));
        byte[] salt = new byte[16], nonce = new byte[12], pass = null, derived = null;
        random.nextBytes(salt); random.nextBytes(nonce);
        try {
            pass = passwordBytes(password);
            derived = SCrypt.generate(pass, salt, N, R, P, 32);
            ByteArrayOutputStream header = new ByteArrayOutputStream();
            header.write(MAGIC, 0, MAGIC.length);
            byte[] fixed = {(byte)(withNote?3:2), (byte) network.id, 1, 1, 15, 8, 1};
            header.write(fixed, 0, fixed.length);
            header.write(salt, 0, salt.length); header.write(nonce, 0, nonce.length);
            header.write(4);
            String[] values={addresses.nativeSegwit,addresses.legacy,addresses.nestedSegwit,addresses.taproot};
            for(int i=0;i<values.length;i++) {
                byte[] bytes=values[i].getBytes(StandardCharsets.US_ASCII);
                header.write(i+1); header.write(bytes.length); header.write(bytes,0,bytes.length);
            }
            if(withNote){byte[] bytes=note.getBytes(StandardCharsets.UTF_8);header.write(bytes.length);header.write(bytes,0,bytes.length);}
            byte[] aad = header.toByteArray();
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(derived, "AES"), new GCMParameterSpec(128, nonce));
            cipher.updateAAD(aad);
            byte[] ciphertext = cipher.doFinal(privateKey);
            header.write(ciphertext, 0, ciphertext.length);
            return (withNote?NOTE_PREFIX:PREFIX) + Base64.getUrlEncoder().withoutPadding().encodeToString(header.toByteArray());
        } finally { wipe(pass); wipe(derived); }
    }

    public static boolean isBackup(String text) {
        return text!=null && (text.startsWith(PREFIX)||text.startsWith(NOTE_PREFIX)||text.startsWith(LEGACY_PREFIX));
    }

    public Metadata inspect(String text) {
        if (text == null || text.length() > MAX_TEXT_LENGTH || !isBackup(text))
            throw new IllegalArgumentException("不是受支持的 BitTap 备份");
        String encoded = text.substring(4);
        if (!encoded.matches("[A-Za-z0-9_-]+")) throw new IllegalArgumentException("备份编码不正确");
        byte[] data;
        try { data = Base64.getUrlDecoder().decode(encoded); }
        catch (IllegalArgumentException ex) { throw new IllegalArgumentException("备份编码不正确"); }
        if (!Base64.getUrlEncoder().withoutPadding().encodeToString(data).equals(encoded))
            throw new IllegalArgumentException("备份编码不规范");
        if (data.length < 129 || data.length > 334)
            throw new IllegalArgumentException("备份长度不正确");
        ByteBuffer b = ByteBuffer.wrap(data);
        for (byte magic : MAGIC) if (b.get() != magic) throw new IllegalArgumentException("备份标识不正确");
        int version=Byte.toUnsignedInt(b.get());
        if ((version<1 || version>3) || !(version==1?text.startsWith(LEGACY_PREFIX):version==2?text.startsWith(PREFIX):text.startsWith(NOTE_PREFIX)))
            throw new IllegalArgumentException("不支持此备份版本");
        int id = Byte.toUnsignedInt(b.get());
        if (id != 0) throw new IllegalArgumentException("本版仅支持 BTC 主网卡片");
        Network network = Network.MAINNET;
        // Fixed allowlist prevents a malicious tag from requesting excessive KDF memory/work.
        if (b.get() != 1 || b.get() != 1 || b.get() != 15 || b.get() != 8 || b.get() != 1)
            throw new IllegalArgumentException("不支持此地址或加密参数");
        byte[] salt = new byte[16], nonce = new byte[12]; b.get(salt); b.get(nonce);
        String address; BitcoinAddresses addresses=null;
        if(version==1) {
            address=readAddress(b,42,42);
            BitcoinAddresses.requireWitness(address,0,20);
        } else {
            if(Byte.toUnsignedInt(b.get())!=4)throw new IllegalArgumentException("地址数量不正确");
            String[] values=new String[4];
            for(int i=0;i<4;i++) {
                if(b.remaining()<2 || Byte.toUnsignedInt(b.get())!=i+1)
                    throw new IllegalArgumentException("地址类型或顺序不正确");
                values[i]=readAddress(b,i==0?42:i==3?62:26,i==0?42:i==3?62:35);
            }
            addresses=BitcoinAddresses.parse(values[1],values[2],values[0],values[3]);
            address=addresses.nativeSegwit;
        }
        String note="";
        if(version==3){
            if(b.remaining()<49)throw new IllegalArgumentException("钱包备注数据不完整");
            int length=Byte.toUnsignedInt(b.get());if(length>64||b.remaining()!=length+48)throw new IllegalArgumentException("钱包备注长度不正确");
            byte[] bytes=new byte[length];b.get(bytes);
            try {note=StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}
            catch(java.nio.charset.CharacterCodingException ex){throw new IllegalArgumentException("钱包备注编码不正确");}
            if(!note.equals(WalletNote.normalize(note)))throw new IllegalArgumentException("钱包备注不规范");
        }
        if(b.remaining()!=48)throw new IllegalArgumentException("地址或密文长度不正确");
        byte[] aad = Arrays.copyOf(data, b.position());
        byte[] ciphertext = new byte[48]; b.get(ciphertext);
        return new Metadata(address, addresses, version, network, salt, nonce, aad, ciphertext, note);
    }

    private static String readAddress(ByteBuffer b,int min,int max) {
        if(b.remaining()<1)throw new IllegalArgumentException("地址数据不完整");
        int length=Byte.toUnsignedInt(b.get());
        if(length<min || length>max || b.remaining()<length+48)
            throw new IllegalArgumentException("地址或密文长度不正确");
        byte[] bytes=new byte[length];b.get(bytes);
        for(byte ch:bytes)if(ch<33||ch>126)throw new IllegalArgumentException("地址字符不正确");
        return new String(bytes,StandardCharsets.US_ASCII);
    }

    public Unlocked decrypt(String encrypted, char[] password) throws GeneralSecurityException {
        Metadata meta = inspect(encrypted);
        byte[] pass = null, derived = null, privateKey = null;
        try {
            pass = passwordBytes(password);
            derived = SCrypt.generate(pass, meta.salt, N, R, P, 32);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(derived, "AES"), new GCMParameterSpec(128, meta.nonce));
            cipher.updateAAD(meta.aad);
            privateKey = cipher.doFinal(meta.ciphertext);
            requireValidKey(privateKey);
            Unlocked result = new Unlocked(privateKey, meta.network, meta.note);
            if (!result.address.equals(meta.address) || (meta.addresses!=null && !result.addresses.sameAs(meta.addresses))) {
                result.close(); throw new GeneralSecurityException("恢复地址不一致");
            }
            return result;
        } finally { wipe(pass); wipe(derived); wipe(privateKey); }
    }

    /** BIP38 is an import-only legacy format; its checksum uses P2PKH, not the SegWit address. */
    public Unlocked importBip38(String encrypted, char[] password) throws Exception {
        if (encrypted == null || encrypted.length() != 58 || !encrypted.startsWith("6P"))
            throw new IllegalArgumentException("BIP38 密文应为 58 字符且以 6P 开头");
        byte[] passCheck = passwordBytes(password); wipe(passCheck);
        ECKey key = BIP38PrivateKey.fromBase58(MainNetParams.get(), encrypted).decrypt(new String(password));
        if (!key.isCompressed()) throw new IllegalArgumentException("此 BIP38 使用未压缩公钥；本版仅支持压缩公钥卡片");
        byte[] raw = key.getPrivKeyBytes();
        try { return new Unlocked(raw, Network.MAINNET); }
        finally { wipe(raw); }
    }

    public static void requireNewPassword(char[] password) {
        if (password == null || password.length < 10 || password.length > 1024)
            throw new IllegalArgumentException("请使用至少 10 个字符的独立长密码（最多 1024 字符）");
        if (new String(password).trim().isEmpty()) throw new IllegalArgumentException("密码不能全部为空白");
    }
    private static byte[] passwordBytes(char[] password) {
        if (password == null || password.length == 0 || password.length > 1024)
            throw new IllegalArgumentException("请输入解密密码（最多 1024 字符）");
        // Normalization never trims whitespace or changes case.
        return Normalizer.normalize(new String(password), Normalizer.Form.NFC).getBytes(StandardCharsets.UTF_8);
    }
    static boolean isValidKey(byte[] key) {
        if (key == null || key.length != 32) return false;
        BigInteger value = new BigInteger(1, key);
        return value.signum() > 0 && value.compareTo(CurveParameters.SECP256K1.getN()) < 0;
    }
    static void requireValidKey(byte[] key) { if (!isValidKey(key)) throw new IllegalArgumentException("无效的 BTC 私钥"); }
    static ECKey publicKeyOf(byte[] privateKey) {
        requireValidKey(privateKey);
        // Public-only wrapper also supports legal scalar 1, rejected by bitcoinj's private-key wrapper.
        return ECKey.fromPublicOnly(ECKey.publicPointFromPrivate(new BigInteger(1, privateKey)), true);
    }
    static String addressOf(ECKey key, Network network) { return SegwitAddress.fromKey(network.params(), key).toString(); }
    public static String hex(byte[] bytes) {
        StringBuilder s = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) s.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
        return s.toString();
    }
    public static void wipe(byte[] bytes) { if (bytes != null) Arrays.fill(bytes, (byte) 0); }
    public static void wipe(char[] chars) { if (chars != null) Arrays.fill(chars, '\0'); }
}
