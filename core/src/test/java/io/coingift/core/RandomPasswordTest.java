package io.coingift.core;

import org.junit.Test;
import java.security.SecureRandom;
import java.util.Arrays;
import static org.junit.Assert.*;

public class RandomPasswordTest {
    @Test public void samplesUniformAlphabetAndRetriesMissingClasses(){
        SecureRandom source=new SecureRandom(){int calls;
            @Override public int nextInt(int bound){assertEquals(94,bound);int n=calls++;return n<32?0:"Ab1!".charAt((n-32)%4)-0x21;}
        };
        char[] value=RandomPassword.generate(source);
        try{assertEquals(32,value.length);assertTrue(NewPasswordPolicy.inspect(java.nio.CharBuffer.wrap(value)).satisfied());assertEquals('A',value[0]);}
        finally{WalletCrypto.wipe(value);}
    }
    @Test public void randomFailureDoesNotFallBack(){
        SecureRandom failed=new SecureRandom(){@Override public int nextInt(int bound){throw new IllegalStateException("test random failure");}};
        assertThrows(IllegalStateException.class,()->RandomPassword.generate(failed));
        SecureRandom stuck=new SecureRandom(){@Override public int nextInt(int bound){return 0;}};
        assertThrows(IllegalStateException.class,()->RandomPassword.generate(stuck));
    }
    @Test public void productionSamplesHaveAllowedLengthClassesAndCanBeWiped(){
        char[] previous=null;
        try{for(int i=0;i<64;i++){
            char[] value=RandomPassword.generate();
            assertEquals(32,value.length);assertTrue(NewPasswordPolicy.inspect(java.nio.CharBuffer.wrap(value)).satisfied());
            if(previous!=null){assertFalse(Arrays.equals(previous,value));WalletCrypto.wipe(previous);}
            previous=value;
        }}finally{WalletCrypto.wipe(previous);}
    }
}
