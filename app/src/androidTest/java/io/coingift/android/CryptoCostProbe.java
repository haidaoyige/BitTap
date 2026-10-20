package io.coingift.android;

import android.app.Instrumentation;
import android.os.SystemClock;
import io.coingift.core.WalletCrypto;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.json.JSONObject;

/** PUBLIC inputs only. Device measurements are observations, not physical-phone benchmarks. */
final class CryptoCostProbe {
    private final Instrumentation host;
    CryptoCostProbe(Instrumentation host){this.host=host;}
    String run()throws Exception{
        JSONObject metrics=new JSONObject();metrics.put("api",android.os.Build.VERSION.SDK_INT);
        java.lang.reflect.Method derive=Class.forName("org.bouncycastle.crypto.generators.SCrypt").getMethod("generate",byte[].class,byte[].class,int.class,int.class,int.class,int.class);
        byte[] password="Public fixture benchmark 9!".getBytes(StandardCharsets.US_ASCII),salt=new byte[16];
        try{
            for(int p:new int[]{1,3}){
                long best=Long.MAX_VALUE;
                for(int i=0;i<3;i++){long start=SystemClock.elapsedRealtime();byte[] derived=(byte[])derive.invoke(null,password,salt,32768,8,p,32);best=Math.min(best,SystemClock.elapsedRealtime()-start);WalletCrypto.wipe(derived);}
                metrics.put("scrypt_p"+p+"_best_ms",best);
            }
        }finally{WalletCrypto.wipe(password);}
        WalletCrypto crypto=new WalletCrypto();char[] pass="Public-fixture-Only9!".toCharArray();
        try{
            long start=SystemClock.elapsedRealtime();WalletCrypto.Card card=crypto.generate(pass,WalletCrypto.Network.MAINNET,"benchmark",null);metrics.put("create_with_self_verification_ms",SystemClock.elapsedRealtime()-start);
            byte[] raw=Base64.getUrlDecoder().decode(card.encrypted.substring(4));if(raw[9]!=3)throw new AssertionError("New card uses obsolete cost");
            start=SystemClock.elapsedRealtime();try(WalletCrypto.Unlocked key=crypto.decrypt(card.encrypted,pass)){if(!key.addresses.sameAs(card.addresses))throw new AssertionError("Address mismatch");}metrics.put("unlock_ms",SystemClock.elapsedRealtime()-start);
            raw[9]=1;String obsolete="CG3:"+Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
            try{crypto.inspect(obsolete);throw new AssertionError("Obsolete cost accepted");}catch(IllegalArgumentException expected){}
            try(WalletCrypto.Unlocked key=crypto.importBip38("6PYW57v4Fk5tnPAPQcXt578N4kxYVrz9FurvD63dSpUo3usVfchv4YVgCx","public boundary fixture".toCharArray())){
                if(!"KwDiBf89QgGbjEhKnhXJuH7LrciVrZi3qYjgd9M7rFU73sVHnoWn".equals(key.wif()))throw new AssertionError("BIP38 scalar-one mismatch");
            }
        }finally{WalletCrypto.wipe(pass);}
        java.io.File folder=host.getTargetContext().getExternalFilesDir("crypto-cost-qa");if(folder==null)throw new java.io.IOException();folder.mkdirs();
        try(java.io.OutputStream out=new java.io.FileOutputStream(new java.io.File(folder,"measurements.json"))){out.write(metrics.toString(2).getBytes(StandardCharsets.UTF_8));}
        return "PASS: device scrypt/new-card/obsolete-profile/BIP38 checks; "+metrics.toString()+"\n";
    }
}
