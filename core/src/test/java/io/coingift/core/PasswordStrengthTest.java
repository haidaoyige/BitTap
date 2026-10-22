package io.coingift.core;

import org.junit.Test;
import static org.junit.Assert.*;
import static io.coingift.core.PasswordStrength.Level.*;

public class PasswordStrengthTest {
    @Test public void emptyIsNeutral(){assertEquals(EMPTY,PasswordStrength.assess(""));assertEquals(EMPTY,PasswordStrength.assess(null));}
    @Test public void commonPasswordsAndShortPinsAreWeak(){for(String p:new String[]{"123456","PASSWORD123","qwerty","390715","correct horse battery staple"})assertEquals(WEAK,PasswordStrength.assess(p));}
    @Test public void repeatedAndSequentialPatternsStayWeakDespiteLength(){for(String p:new String[]{"abcdefghijklmnop","12345678901234567890","QwertyuiopQwertyuiop","aB4!aB4!aB4!aB4!","!!!!!!!!!!!!!!!!!!!!"})assertEquals(WEAK,PasswordStrength.assess(p));}
    @Test public void lengthAndVarietyProduceGraduatedAdvice(){assertEquals(MEDIUM,PasswordStrength.assess("Mx7!pQ2@tL8#"));assertEquals(STRONG,PasswordStrength.assess("Mx7!pQ2@tL8#vR5$zN"));}
    @Test public void longPassphrasesCanReceiveStrongerAdvice(){assertEquals(STRONG,PasswordStrength.assess("river quartz lantern meadow orbit falcon"));}
    @Test public void formatChecksDoNotClaimPredictablePasswordIsStrong(){char[] weak="aB4!aB4!aB4!".toCharArray();assertEquals(WEAK,PasswordStrength.assess(new String(weak)));WalletCrypto.requireNewPassword(weak);WalletCrypto.wipe(weak);}
    @Test public void strengthUsesCanonicalCodePointsRatherThanInputCodeUnits(){
        assertEquals(WEAK,PasswordStrength.assess("Ab1!abcde\u0301"));
        assertEquals(WEAK,PasswordStrength.assess("Ab1!abcd\u00e9"));
        assertEquals(MEDIUM,PasswordStrength.assess("Ab1!abcde\u0301f"));
        assertEquals(PasswordStrength.assess("Ab1!abcd\u00e9f"),PasswordStrength.assess("Ab1!abcde\u0301f"));
        assertEquals(WEAK,PasswordStrength.assess("Ax1\ud83d\ude00mZrpt"));
        assertEquals(MEDIUM,PasswordStrength.assess("Ax1\ud83d\ude00mZrptu"));
        // Eight supplementary symbols must not count as sixteen for strong advice.
        assertEquals(MEDIUM,PasswordStrength.assess("Ab1!\ud83d\ude00\ud83d\ude01\ud83d\ude02\ud83d\ude03\ud83d\ude04\ud83d\ude05\ud83d\ude06\ud83d\ude07"));
    }
    @Test public void commonDecoratedTemplatesRemainWeakEvenWhenFormatIsValid(){
        for(String p:new String[]{"Password123!","P@ssw0rd123!","Password123!Aa","123Welcome!Aa","Bitcoin2026!","Qwerty123!Aa"}){
            char[] chars=p.toCharArray();try{NewPasswordPolicy.require(chars);assertEquals(p,WEAK,PasswordStrength.assess(java.nio.CharBuffer.wrap(chars)));}finally{WalletCrypto.wipe(chars);}
        }
    }
    @Test public void decoratedRepetitionAndDominantKeyboardRunsRemainWeak(){
        for(String p:new String[]{"A1!AbAbAbAbAb","Ab1234567890!","Qwertyuiop1!","Ab123412341234!"}){
            NewPasswordPolicy.require(p.toCharArray());assertEquals(p,WEAK,PasswordStrength.assess(p));
        }
    }
    @Test public void heuristicDoesNotDowngradeUnrelatedVariedPasswords(){
        for(String p:new String[]{"Zq7!rT9@pL4#vN6$","Mx7!pQ2@tL8#vR5$zN","jK9@vM2!xQ7#bR6%"})assertEquals(STRONG,PasswordStrength.assess(p));
    }
    @Test public void strengthAssessmentDoesNotModifyMutableInput(){
        char[] chars="P@ssw0rd123!".toCharArray(),before=chars.clone();
        try{assertEquals(WEAK,PasswordStrength.assess(java.nio.CharBuffer.wrap(chars)));assertArrayEquals(before,chars);}finally{WalletCrypto.wipe(chars);WalletCrypto.wipe(before);}
    }
}
