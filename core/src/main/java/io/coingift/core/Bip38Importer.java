/*
 * Copyright 2014 Andreas Schildbach
 * Modified for BitTap: raw-scalar import, address-hash verification and buffer cleanup.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.coingift.core;

import java.math.BigInteger;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.bitcoinj.base.LegacyAddress;
import org.bitcoinj.base.Sha256Hash;
import org.bitcoinj.crypto.BIP38PrivateKey;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.params.MainNetParams;
import org.bouncycastle.crypto.generators.SCrypt;

/**
 * Compressed, mainnet BIP38 imports only. Adapted from bitcoinj 0.17.1
 * BIP38PrivateKey (Apache-2.0); parsing/checksum/flags remain in that library.
 * Its private ECKey wrapper excludes scalar 1; this path validates raw scalars
 * and uses public-only wrappers instead. BIP38's P2PKH address hash MUST match
 * before any private key is returned. AES/scrypt operations follow BIP38 unchanged.
 */
final class Bip38Importer {
    private Bip38Importer(){}
    private static byte[] keep(List<byte[]> buffers,byte[] value){buffers.add(value);return value;}
    static byte[] decrypt(BIP38PrivateKey encoded,char[] password)throws GeneralSecurityException{
        if(!encoded.compressed)throw new IllegalArgumentException("需要压缩公钥 BIP38");
        List<byte[]> buffers=new ArrayList<>();byte[] raw=null;
        try{
            byte[] pass=keep(buffers,PasswordNormalization.nfc(CharBuffer.wrap(password)).toString().getBytes(StandardCharsets.UTF_8));
            if(!encoded.ecMultiply){
                byte[] derived=keep(buffers,SCrypt.generate(pass,encoded.addressHash,16384,8,8,64));
                byte[] aes=keep(buffers,Arrays.copyOfRange(derived,32,64));
                raw=decryptBlock(aes,encoded.content);
                for(int i=0;i<32;i++)raw[i]^=derived[i];
            }else{
                byte[] entropy=Arrays.copyOfRange(encoded.content,0,8);
                byte[] salt=encoded.hasLotAndSequence?Arrays.copyOf(entropy,4):entropy;
                byte[] prefactor=keep(buffers,SCrypt.generate(pass,salt,16384,8,8,32));
                byte[] factor=encoded.hasLotAndSequence?keep(buffers,Sha256Hash.hashTwice(keep(buffers,join(prefactor,entropy)))):prefactor;
                BigInteger passFactor=new BigInteger(1,factor);
                if(passFactor.mod(CurveParameters.SECP256K1.getN()).signum()==0)throw new GeneralSecurityException("BIP38 密码或数据不正确");
                byte[] passPoint=keep(buffers,ECKey.publicKeyFromPrivate(passFactor,true));
                byte[] derived=keep(buffers,SCrypt.generate(passPoint,join(encoded.addressHash,entropy),1024,1,1,64));
                byte[] aes=keep(buffers,Arrays.copyOfRange(derived,32,64));
                byte[] second=keep(buffers,decryptBlock(aes,Arrays.copyOfRange(encoded.content,16,32)));
                for(int i=0;i<16;i++)second[i]^=derived[i+16];
                byte[] first=keep(buffers,decryptBlock(aes,keep(buffers,join(Arrays.copyOfRange(encoded.content,8,16),keep(buffers,Arrays.copyOf(second,8))))));
                for(int i=0;i<16;i++)first[i]^=derived[i];
                byte[] seed=keep(buffers,join(first,keep(buffers,Arrays.copyOfRange(second,8,16))));
                byte[] hash=keep(buffers,Sha256Hash.hashTwice(seed));
                BigInteger scalar=passFactor.multiply(new BigInteger(1,hash)).mod(CurveParameters.SECP256K1.getN());
                byte[] signed=keep(buffers,scalar.toByteArray());raw=new byte[32];
                int count=Math.min(32,signed.length);System.arraycopy(signed,signed.length-count,raw,32-count,count);
            }
            WalletCrypto.requireValidKey(raw);
            String address=LegacyAddress.fromKey(MainNetParams.get(),WalletCrypto.publicKeyOf(raw)).toString();
            byte[] actual=keep(buffers,Sha256Hash.hashTwice(address.getBytes(StandardCharsets.US_ASCII)));
            if(!MessageDigest.isEqual(Arrays.copyOf(actual,4),encoded.addressHash))throw new GeneralSecurityException("BIP38 密码或数据不正确");
            byte[] result=raw;raw=null;return result;
        }finally{WalletCrypto.wipe(raw);for(byte[] buffer:buffers)WalletCrypto.wipe(buffer);}
    }
    private static byte[] decryptBlock(byte[] aes,byte[] ciphertext)throws GeneralSecurityException{
        Cipher cipher=Cipher.getInstance("AES/ECB/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(aes,"AES"));return cipher.doFinal(ciphertext);
    }
    private static byte[] join(byte[] left,byte[] right){byte[] out=Arrays.copyOf(left,left.length+right.length);System.arraycopy(right,0,out,left.length,right.length);return out;}
}
