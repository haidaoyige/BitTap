package io.coingift.android;
import org.junit.Test;
import static org.junit.Assert.*;
public class NtagProtectionTest {
    @Test public void onlyExplicit215And216VersionLayoutsAccepted(){assertEquals(215,NtagProtection.model(new byte[]{0,4,4,2,1,0,0x11,3}));assertEquals(216,NtagProtection.model(new byte[]{0,4,4,2,1,0,0x13,3}));assertEquals(0,NtagProtection.model(new byte[]{0,4,4,2,1,0,0x0f,3}));assertEquals(0,NtagProtection.model(new byte[8]));assertEquals(0,NtagProtection.model(null));}
    @Test public void softwareCcFlagAloneDoesNotProveHardwareLock(){byte[] stat=new byte[16],dyn=new byte[16],cfg=new byte[16];stat[7]=15;assertFalse(NtagProtection.locked(215,stat,dyn,cfg));}
    @Test public void allNdefDataAndConfigLocksRequired(){byte[] stat=new byte[16],dyn=new byte[16],cfg=new byte[16];stat[2]=(byte)0xf8;stat[3]=(byte)255;stat[7]=15;dyn[0]=(byte)255;dyn[1]=0x3f;cfg[4]=0x40;assertTrue(NtagProtection.locked(215,stat,dyn,cfg));assertTrue(NtagProtection.locked(216,stat,dyn,cfg));dyn[1]=0;assertFalse(NtagProtection.locked(216,stat,dyn,cfg));cfg[4]=0;assertFalse(NtagProtection.locked(215,stat,dyn,cfg));}
    @Test public void badOrTruncatedLockReadsNeverConfirm(){assertFalse(NtagProtection.locked(216,new byte[7],new byte[4],new byte[8]));assertFalse(NtagProtection.locked(0,new byte[16],new byte[16],new byte[16]));assertThrows(IllegalArgumentException.class,()->NtagProtection.dynamicPage(213));}
}
