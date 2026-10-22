package io.coingift.core;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.params.MainNetParams;

/** Identity of the entire canonical wallet payload. Never a password or private-key hash. */
public final class WalletFingerprint {
    private WalletFingerprint(){}
    public static byte[] of(String payload) {
        if(payload==null||payload.length()>WalletCrypto.MAX_TEXT_LENGTH)throw new IllegalArgumentException("Invalid wallet payload");
        if(WalletCrypto.isBackup(payload))new WalletCrypto().inspect(payload);
        else {
            if(payload.length()!=58||!payload.startsWith("6P"))throw new IllegalArgumentException("Invalid wallet payload");
            if(!BIP38PrivateKey.fromBase58(MainNetParams.get(),payload).compressed)throw new IllegalArgumentException("Unsupported BIP38 key");
        }
        byte[] bytes=payload.getBytes(StandardCharsets.US_ASCII);
        try {
            MessageDigest hash=MessageDigest.getInstance("SHA-256");
            hash.update("BitTap.LocalVerification.v1\0".getBytes(StandardCharsets.US_ASCII));
            hash.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
            return hash.digest(bytes);
        }catch(NoSuchAlgorithmException ex){throw new IllegalStateException(ex);}
        finally{WalletCrypto.wipe(bytes);}
    }
    public static boolean matches(byte[] expected,String payload){
        if(expected==null||expected.length!=32)return false;
        byte[] actual=of(payload);try{return MessageDigest.isEqual(expected,actual);}finally{WalletCrypto.wipe(actual);}
    }
}
