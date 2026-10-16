package io.coingift.android;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import io.coingift.core.BitcoinAddresses;
import java.util.EnumMap;
import java.util.Map;

/** Separate entry points for raw public addresses and raw compressed-mainnet WIF. */
final class QrCodeEncoder {
    static BitMatrix wif(String wif, int size) throws WriterException {
        if (wif == null || !wif.matches("[KL][1-9A-HJ-NP-Za-km-z]{51}") || size < 160)
            throw new IllegalArgumentException("需要有效的主网压缩 WIF 私钥");
        return encode(wif,size);
    }
    static BitMatrix address(String address, int size) throws WriterException {
        BitcoinAddresses.requireMainnetAddress(address);
        if(size < 160)throw new IllegalArgumentException("二维码尺寸过小");
        return encode(address,size);
    }
    private static BitMatrix encode(String payload,int size) throws WriterException {
        Map<EncodeHintType,Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 4);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        return new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints);
    }
}
