package io.coingift.core;

import java.security.SecureRandom;
import java.util.Arrays;

/** Local-only CSPRNG passwords. Caller owns and must clear the returned buffer. */
public final class RandomPassword {
    public static final int LENGTH=32;
    private RandomPassword(){}
    public static char[] generate(){return generate(WalletCrypto.systemRandom());}
    static char[] generate(SecureRandom random){
        char[] value=new char[LENGTH];boolean returned=false;
        try{
            // Uniformly sample the 94 allowed characters; reject the whole sample
            // if a required class is absent, rather than inserting predictable slots.
            for(int attempt=0;attempt<128;attempt++){
                for(int i=0;i<value.length;i++)value[i]=(char)(0x21+random.nextInt(94));
                if(NewPasswordPolicy.inspect(java.nio.CharBuffer.wrap(value)).satisfied()){
                    returned=true;return value;
                }
            }
            throw new IllegalStateException("Secure random password generation failed");
        }finally{if(!returned)Arrays.fill(value,'\0');}
    }
}
