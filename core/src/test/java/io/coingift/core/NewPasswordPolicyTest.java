package io.coingift.core;

import org.junit.Test;
import static org.junit.Assert.*;
import java.security.SecureRandom;
import java.util.Arrays;

public class NewPasswordPolicyTest {
    @Test public void rejectsEachMissingClassAndWhitespaceIsNotASymbol(){
        for(String value:new String[]{"1234567890","abcdef123!","ABCDEF123!","Abcdefghi!","Abcdef1234","Abcdef123 ","Abcdef123\n"}){
            assertFalse(value,NewPasswordPolicy.inspect(value).satisfied());
            assertThrows(IllegalArgumentException.class,()->WalletCrypto.requireNewPassword(value.toCharArray()));
        }
        assertThrows(IllegalArgumentException.class,()->WalletCrypto.requireNewPassword(null));
    }
    @Test public void tenIsAMinimumAndAllFourClassesAreRequired(){
        assertFalse(NewPasswordPolicy.inspect("Ab1!abcde").satisfied());
        for(String value:new String[]{"Ab1!abcdef","Ab1!abcdef-more","Ab1！abcdef","Ab1😀abcdef"}){
            assertTrue(value,NewPasswordPolicy.inspect(value).satisfied());
            WalletCrypto.requireNewPassword(value.toCharArray());
        }
        // A supplementary symbol is one character, not two UTF-16 code units.
        assertFalse(NewPasswordPolicy.inspect("Ab1😀abcde").satisfied());
        char[] maximum=new char[1024];Arrays.fill(maximum,'a');maximum[0]='A';maximum[1]='1';maximum[2]='!';
        WalletCrypto.requireNewPassword(maximum);
        assertThrows(IllegalArgumentException.class,()->WalletCrypto.requireNewPassword(Arrays.copyOf(maximum,1025)));
    }
    @Test public void bothCreationPathsRejectInvalidFormatBeforeRandomness()throws Exception{
        SecureRandom untouched=new SecureRandom(){@Override public void nextBytes(byte[] value){throw new AssertionError("invalid password reached random generation");}};
        WalletCrypto crypto=new WalletCrypto(untouched);
        for(String value:new String[]{"1234567890","abcdef123!","ABCDEF123!","Abcdefghi!","Abcdef1234"}){
            assertThrows(IllegalArgumentException.class,()->crypto.generate(value.toCharArray(),WalletCrypto.Network.MAINNET));
            assertThrows(IllegalArgumentException.class,()->crypto.generate(value.toCharArray(),WalletCrypto.Network.MAINNET,"",null));
        }
    }
    @Test public void existingNoncompliantPasswordsStillDecrypt()throws Exception{
        byte[] publicScalar=new byte[32];publicScalar[31]=1;WalletCrypto crypto=new WalletCrypto();
        for(String value:new String[]{"123456","1234567890","a long lowercase password"}){
            char[] password=value.toCharArray();
            assertThrows(IllegalArgumentException.class,()->WalletCrypto.requireNewPassword(password));
            try{
                String old=crypto.encrypt(publicScalar,password,WalletCrypto.Network.MAINNET);
                try(WalletCrypto.Unlocked restored=crypto.decrypt(old,password)){assertEquals("bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4",restored.address);}
            }finally{WalletCrypto.wipe(password);}
        }
    }
}
