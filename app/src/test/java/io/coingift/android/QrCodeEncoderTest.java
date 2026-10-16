package io.coingift.android;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.Test;
import static org.junit.Assert.*;

public class QrCodeEncoderTest {
    // Public BIP38 standard fixture, never a real funded key.
    private static final String WIF = "L44B5gGEpqEDRS9vVPz7QT35jcBG2r3CZwSwQ4fCewXAhAhqGVpP";
    @Test public void displayedPixelsScanToExactlyTheWif() throws Exception {
        for(int size:new int[]{192,280,480}){
            BitMatrix matrix=QrCodeEncoder.wif(WIF,size);
            int[] pixels=new int[size*size];
            for(int y=0;y<size;y++)for(int x=0;x<size;x++)pixels[y*size+x]=matrix.get(x,y)?0xff000000:0xffffffff;
            BinaryBitmap image=new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(size,size,pixels)));
            assertEquals(WIF,new QRCodeReader().decode(image).getText());
            assertFalse(matrix.get(0,0));assertFalse(matrix.get(size-1,size-1));
        }
    }
    @Test public void neverEncodesAnAddressOrEncryptedBackupAsPrivateKey(){
        assertThrows(IllegalArgumentException.class,()->QrCodeEncoder.wif("CG1:abc",280));
        assertThrows(IllegalArgumentException.class,()->QrCodeEncoder.wif("bc1qexample",280));
    }
    @Test public void allFourAddressTypesScanToExactlyThePublicAddress() throws Exception {
        String[] addresses={
            "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",
            "1BgGZ9tcN4rm9KBzDn7KprQz87SZ26SAMH",
            "3JvL6Ymt8MVWiCNHC7oWU6nLeHNJKLZGLN",
            "bc1pmfr3p9j00pfxjh0zmgp99y8zftmd3s5pmedqhyptwy6lm87hf5sspknck9"
        };
        for(String address:addresses)for(int size:new int[]{160,280,480}){
            BitMatrix matrix=QrCodeEncoder.address(address,size);
            int[] pixels=new int[size*size];
            for(int y=0;y<size;y++)for(int x=0;x<size;x++)pixels[y*size+x]=matrix.get(x,y)?0xff000000:0xffffffff;
            BinaryBitmap image=new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(size,size,pixels)));
            assertEquals(address,new QRCodeReader().decode(image).getText());
            assertFalse(matrix.get(0,0));assertFalse(matrix.get(size-1,size-1));
        }
    }
    @Test public void addressQrRejectsPrivateKeysBackupsAndInvalidOrWrongNetworkAddresses(){
        for(String value:new String[]{null,WIF,"CG2:abc","6PRVWUbkzz","bitcoin:1BgGZ9tcN4rm9KBzDn7KprQz87SZ26SAMH",
            "1BgGZ9tcN4rm9KBzDn7KprQz87SZ26SAMJ","bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t5",
            "bc1pmfr3p9j00pfxjh0zmgp99y8zftmd3s5pmedqhyptwy6lm87hf5sspknck8","mipcBbFg9gMiCh81Kj8tqqdgoZub1ZJRfn"})
            assertThrows(IllegalArgumentException.class,()->QrCodeEncoder.address(value,280));
        assertThrows(IllegalArgumentException.class,()->QrCodeEncoder.address("1BgGZ9tcN4rm9KBzDn7KprQz87SZ26SAMH",159));
    }
}
