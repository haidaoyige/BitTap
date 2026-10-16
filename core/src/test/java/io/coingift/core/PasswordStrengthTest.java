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
    @Test public void strengthDoesNotBlockAnAcceptedWeakPassword(){char[] weak="123456".toCharArray();assertEquals(WEAK,PasswordStrength.assess(new String(weak)));WalletCrypto.requireNewPassword(weak);WalletCrypto.wipe(weak);}
}
