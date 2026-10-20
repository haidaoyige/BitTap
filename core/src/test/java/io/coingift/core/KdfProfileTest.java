package io.coingift.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Base64;

/** Reject unapproved work factors before any password derivation. */
public class KdfProfileTest {
    private static byte[] one(){byte[] raw=new byte[32];raw[31]=1;return raw;}
    private static char[] password(){return "Public-fixture-Only9!".toCharArray();}
    @Test public void newCg2AndCg3UseApprovedHigherCost()throws Exception{
        WalletCrypto crypto=new WalletCrypto();
        for(boolean note:new boolean[]{false,true}){
            String text=crypto.encrypt(one(),password(),WalletCrypto.Network.MAINNET,"",note);
            byte[] raw=Base64.getUrlDecoder().decode(text.substring(4));
            assertEquals(15,raw[7]);assertEquals(8,raw[8]);assertEquals(3,raw[9]);
            try(WalletCrypto.Unlocked key=crypto.decrypt(text,password())){assertEquals("KwDiBf89QgGbjEhKnhXJuH7LrciVrZi3qYjgd9M7rFU73sVHnoWn",key.wif());}
        }
    }
    @Test public void obsoleteAndArbitraryKdfProfilesAreRejectedBeforeDecryption()throws Exception{
        WalletCrypto crypto=new WalletCrypto();String text=crypto.encrypt(one(),password(),WalletCrypto.Network.MAINNET);
        byte[] original=Base64.getUrlDecoder().decode(text.substring(4));
        for(int offset:new int[]{7,8,9})for(int value=0;value<256;value++){
            if(value==Byte.toUnsignedInt(original[offset]))continue;
            byte[] changed=original.clone();changed[offset]=(byte)value;
            String invalid="CG2:"+Base64.getUrlEncoder().withoutPadding().encodeToString(changed);
            assertThrows(IllegalArgumentException.class,()->crypto.inspect(invalid));
        }
    }
}
