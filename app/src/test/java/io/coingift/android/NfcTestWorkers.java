package io.coingift.android;

import java.lang.reflect.Field;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.fail;

/** A synchronous completion callback runs before its worker has returned to the pool. */
final class NfcTestWorkers {
    static void awaitIdle(Class<?> owner,String name)throws Exception{
        Field field=owner.getDeclaredField(name);field.setAccessible(true);
        ThreadPoolExecutor pool=(ThreadPoolExecutor)field.get(null);
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(3),idleSince=0;
        // getActiveCount is approximate; require stable idleness without submitting
        // another probe that would itself race the following test's real work.
        while(System.nanoTime()<end){
            if(pool.getActiveCount()==0&&pool.getQueue().isEmpty()){
                if(idleSince==0)idleSince=System.nanoTime();
                if(System.nanoTime()-idleSince>=TimeUnit.MILLISECONDS.toNanos(20))return;
            }else idleSince=0;
            Thread.sleep(5);
        }
        fail("NFC test executor did not become idle within 3 seconds");
    }
}
