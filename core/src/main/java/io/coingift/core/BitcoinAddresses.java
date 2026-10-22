package io.coingift.core;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.base.LegacyAddress;
import org.bitcoinj.base.SegwitAddress;
import org.bitcoinj.base.Sha256Hash;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.script.ScriptBuilder;
import org.bouncycastle.math.ec.ECPoint;

/** Four mainnet destinations for one compressed key; no HD derivation is performed. */
public final class BitcoinAddresses {
    public final String legacy, nestedSegwit, nativeSegwit, taproot;

    private BitcoinAddresses(String legacy, String nestedSegwit, String nativeSegwit, String taproot) {
        this.legacy=legacy;this.nestedSegwit=nestedSegwit;this.nativeSegwit=nativeSegwit;this.taproot=taproot;
    }

    /** Parse public, unauthenticated address claims without deriving or accepting a private key. */
    static BitcoinAddresses parse(String legacy, String nested, String nativeAddress, String taproot) {
        MainNetParams net=MainNetParams.get();
        LegacyAddress l=LegacyAddress.fromBase58(net,legacy), s=LegacyAddress.fromBase58(net,nested);
        if(!legacy.startsWith("1") || !nested.startsWith("3") || !l.toString().equals(legacy) || !s.toString().equals(nested))
            throw new IllegalArgumentException("传统或嵌套 SegWit 地址不正确");
        requireWitness(nativeAddress,0,20); requireWitness(taproot,1,32);
        return new BitcoinAddresses(legacy,nested,nativeAddress,taproot);
    }

    /** Validated public addresses, e.g. restored from an authenticated local record. No private key. */
    public static BitcoinAddresses fromPublicAddresses(String legacy,String nested,String nativeAddress,String taproot) {
        return parse(legacy,nested,nativeAddress,taproot);
    }

    static void requireWitness(String address,int version,int bytes) {
        SegwitAddress parsed=SegwitAddress.fromBech32(MainNetParams.get(),address);
        if(parsed.getWitnessVersion()!=version || parsed.getWitnessProgram().length!=bytes || !parsed.toString().equals(address))
            throw new IllegalArgumentException("地址类型或编码不正确");
    }

    /** Validate one of the four supported mainnet address encodings, including its checksum. */
    public static void requireMainnetAddress(String address) {
        if(address==null)throw new IllegalArgumentException("需要 BTC 主网收款地址");
        if(address.startsWith("1")||address.startsWith("3")) {
            if(!LegacyAddress.fromBase58(MainNetParams.get(),address).toString().equals(address))
                throw new IllegalArgumentException("收款地址编码不正确");
        } else if(address.startsWith("bc1q"))requireWitness(address,0,20);
        else if(address.startsWith("bc1p"))requireWitness(address,1,32);
        else throw new IllegalArgumentException("需要 BTC 主网收款地址");
    }

    public boolean sameAs(BitcoinAddresses other) {
        return other!=null && legacy.equals(other.legacy) && nestedSegwit.equals(other.nestedSegwit)
            && nativeSegwit.equals(other.nativeSegwit) && taproot.equals(other.taproot);
    }

    private static byte[] hash160(byte[] input) {
        byte[] sha=Sha256Hash.hash(input), result=new byte[20];
        RIPEMD160Digest digest=new RIPEMD160Digest();
        digest.update(sha,0,sha.length);digest.doFinal(result,0);
        return result;
    }

    static BitcoinAddresses fromKey(ECKey key) {
        if (!key.isCompressed()) throw new IllegalArgumentException("地址需要压缩公钥");
        MainNetParams net=MainNetParams.get();
        String legacy=LegacyAddress.fromKey(net,key).toString();
        byte[] redeemScript=ScriptBuilder.createP2WPKHOutputScript(key).getProgram();
        String nested=LegacyAddress.fromScriptHash(net,hash160(redeemScript)).toString();
        String nativeSegwit=SegwitAddress.fromKey(net,key).toString();

        // BIP341 key-only output: lift_x chooses the even-Y internal point.
        ECPoint point=key.getPubKeyPoint().normalize();
        if (point.getAffineYCoord().toBigInteger().testBit(0)) point=point.negate().normalize();
        byte[] x=Arrays.copyOfRange(point.getEncoded(true),1,33);
        byte[] tag=Sha256Hash.hash("TapTweak".getBytes(StandardCharsets.US_ASCII));
        byte[] preimage=new byte[96];
        System.arraycopy(tag,0,preimage,0,32);System.arraycopy(tag,0,preimage,32,32);System.arraycopy(x,0,preimage,64,32);
        BigInteger tweak=new BigInteger(1,Sha256Hash.hash(preimage));
        if (tweak.compareTo(CurveParameters.SECP256K1.getN())>=0) throw new IllegalArgumentException("Taproot tweak 超出有效范围");
        ECPoint output=point.add(CurveParameters.SECP256K1.getG().multiply(tweak)).normalize();
        if (output.isInfinity()) throw new IllegalArgumentException("Taproot 输出公钥无效");
        byte[] program=Arrays.copyOfRange(output.getEncoded(true),1,33);
        // bitcoinj uses Bech32m for version 1, as required by BIP350.
        String taproot=SegwitAddress.fromProgram(net,1,program).toString();
        return new BitcoinAddresses(legacy,nested,nativeSegwit,taproot);
    }
}
