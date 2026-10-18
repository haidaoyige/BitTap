package io.coingift.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bitcoinj.base.Bech32;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.base.SegwitAddress;
import org.bitcoinj.params.MainNetParams;

public class BitcoinAddressesTest {
    private static byte[] hex(String s){byte[] b=new byte[s.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(s.substring(i*2,i*2+2),16);return b;}
    private static byte[] scalar(BigInteger k){byte[] raw=k.toByteArray(),out=new byte[32];int count=Math.min(32,raw.length);System.arraycopy(raw,raw.length-count,out,32-count,count);return out;}
    @Test public void knownScalarOneLegacyNestedAndNative(){
        BitcoinAddresses a=BitcoinAddresses.fromKey(WalletCrypto.publicKeyOf(scalar(BigInteger.ONE)));
        assertEquals("1BgGZ9tcN4rm9KBzDn7KprQz87SZ26SAMH",a.legacy);
        assertEquals("3JvL6Ymt8MVWiCNHC7oWU6nLeHNJKLZGLN",a.nestedSegwit);
        assertEquals("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",a.nativeSegwit);
    }
    @Test public void bip341KeyOnlyOfficialVector(){
        ECKey key=ECKey.fromPublicOnly(hex("02d6889cb081036e0faefa3a35157ad71086b123b2b144b649798b494c300a961d"));
        BitcoinAddresses a=BitcoinAddresses.fromKey(key);
        assertEquals("bc1p2wsldez5mud2yam29q22wgfh9439spgduvct83k3pm50fcxa5dps59h4z5",a.taproot);
        SegwitAddress parsed=SegwitAddress.fromBech32(MainNetParams.get(),a.taproot);
        assertEquals(1,parsed.getWitnessVersion());
        assertEquals("53a1f6e454df1aa2776a2814a721372d6258050de330b3c6d10ee8f4e0dda343",WalletCrypto.hex(parsed.getWitnessProgram()));
    }
    @Test public void bip86FirstReceivingOutputVector(){
        ECKey key=ECKey.fromPublicOnly(hex("02cc8a4bc64d897bddc5fbc2f670f7a8ba0b386779106cf1223c6fc5d7cd6fc115"));
        assertEquals("bc1p5cyxnuxmeuwuvkwfem96lqzszd02n6xdcjrs20cac6yqjjwudpxqkedrcr",BitcoinAddresses.fromKey(key).taproot);
    }
    @Test public void bip86SecondReceivingOutputVector(){
        ECKey key=ECKey.fromPublicOnly(hex("0283dfe85a3151d2517290da461fe2815591ef69f2b18a2ce63f01697a8b313145"));
        assertEquals("bc1p4qhjn9zdvkux4e44uhx8tc55attvtyu358kutcqkudyccelu0was9fqzwh",BitcoinAddresses.fromKey(key).taproot);
    }
    @Test public void oddAndEvenYUseSameXOnlyTaprootInternalKey(){
        BitcoinAddresses one=BitcoinAddresses.fromKey(WalletCrypto.publicKeyOf(scalar(BigInteger.ONE)));
        BitcoinAddresses opposite=BitcoinAddresses.fromKey(WalletCrypto.publicKeyOf(scalar(CurveParameters.SECP256K1.getN().subtract(BigInteger.ONE))));
        assertEquals(one.taproot,opposite.taproot);
        assertNotEquals(one.nativeSegwit,opposite.nativeSegwit);
        assertNotEquals(one.legacy,opposite.legacy);
    }
    @Test public void taprootUsesBech32mAndNativeUsesBech32(){
        BitcoinAddresses a=BitcoinAddresses.fromKey(WalletCrypto.publicKeyOf(scalar(BigInteger.ONE)));
        assertEquals(Bech32.Encoding.BECH32M,Bech32.decode(a.taproot).encoding);
        assertEquals(Bech32.Encoding.BECH32,Bech32.decode(a.nativeSegwit).encoding);
    }
    @Test public void rejectUncompressedPubkey(){
        ECKey key=ECKey.fromPublicOnly(ECKey.publicPointFromPrivate(BigInteger.ONE),false);
        assertThrows(IllegalArgumentException.class,()->BitcoinAddresses.fromKey(key));
    }
    @Test public void cardRoundTripAuthenticatesAllFourAddressesInCg2() throws Exception {
        WalletCrypto crypto=new WalletCrypto();char[] pass="Public-fixture-Only9!".toCharArray();
        WalletCrypto.Card card=crypto.generate(pass,WalletCrypto.Network.MAINNET);
        assertTrue(card.encrypted.startsWith("CG2:"));assertTrue(crypto.inspect(card.encrypted).addresses.sameAs(card.addresses));assertEquals(card.address,card.addresses.nativeSegwit);
        try(WalletCrypto.Unlocked u=crypto.decrypt(card.encrypted,pass)){
            assertEquals(card.addresses.legacy,u.addresses.legacy);
            assertEquals(card.addresses.nestedSegwit,u.addresses.nestedSegwit);
            assertEquals(card.addresses.nativeSegwit,u.addresses.nativeSegwit);
            assertEquals(card.addresses.taproot,u.addresses.taproot);
        }
    }
    @Test public void exportKnownScalarsForIndependentAddressAndSpendKeyVerification() throws Exception {
        // Public known scalars only; no production keys are exported or logged.
        StringBuilder rows=new StringBuilder();
        for(BigInteger k:new BigInteger[]{BigInteger.ONE,BigInteger.TWO,BigInteger.valueOf(3),CurveParameters.SECP256K1.getN().subtract(BigInteger.ONE)}){
            byte[] raw=scalar(k);BitcoinAddresses a=BitcoinAddresses.fromKey(WalletCrypto.publicKeyOf(raw));
            rows.append(WalletCrypto.hex(raw)).append('\t').append(a.legacy).append('\t').append(a.nestedSegwit).append('\t').append(a.nativeSegwit).append('\t').append(a.taproot).append('\n');
        }
        Path output=Path.of(System.getProperty("bittap.fixtureDir", "build"), "address-fixtures.tsv");
        Files.createDirectories(output.getParent());Files.writeString(output,rows,StandardCharsets.US_ASCII);
    }
}
