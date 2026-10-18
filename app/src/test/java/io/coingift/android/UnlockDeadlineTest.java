package io.coingift.android;

import org.junit.Test;
import static org.junit.Assert.*;

public class UnlockDeadlineTest {
    @Test public void locksAtSixtySecondsWithoutActivity(){
        UnlockDeadline d=new UnlockDeadline();d.start(1000);
        assertEquals(UnlockDeadline.Cause.NONE,d.cause(60_999));assertEquals(UnlockDeadline.Cause.IDLE,d.cause(61_000));
        assertFalse(d.interact(61_001));assertEquals(0,d.remaining(61_001));
    }
    @Test public void activityNeverExtendsTwoMinuteMaximum(){
        UnlockDeadline d=new UnlockDeadline();d.start(1000);
        assertTrue(d.interact(50_000));assertTrue(d.interact(100_000));assertTrue(d.interact(120_999));
        assertEquals(1,d.remaining(120_999));assertEquals(UnlockDeadline.Cause.MAXIMUM,d.cause(121_000));
        assertFalse(d.interact(121_000));
    }
    @Test public void lateInteractionCannotReviveAnExpiredSession(){
        UnlockDeadline d=new UnlockDeadline();d.start(500);
        assertFalse(d.interact(70_000));assertEquals(UnlockDeadline.Cause.IDLE,d.cause(70_000));
    }
    @Test public void freshUnlockStartsFreshDeadlineAndClearStopsIt(){
        UnlockDeadline d=new UnlockDeadline();d.start(0);d.clear();assertFalse(d.active());
        assertEquals(UnlockDeadline.Cause.NONE,d.cause(999_999));d.start(90_000);assertEquals(60_000,d.remaining(90_000));
    }
}
