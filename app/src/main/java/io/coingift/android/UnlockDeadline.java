package io.coingift.android;

/** Monotonic, memory-only session limits. Activity cannot extend the absolute deadline. */
final class UnlockDeadline {
    static final long IDLE_MS=60_000,MAX_MS=120_000;
    enum Cause { NONE,IDLE,MAXIMUM }
    private long started=-1,last=-1;
    void start(long now){started=last=now;}
    void clear(){started=last=-1;}
    boolean active(){return started>=0;}
    Cause cause(long now){
        if(!active())return Cause.NONE;
        long idle=last+IDLE_MS,maximum=started+MAX_MS;
        if(now<Math.min(idle,maximum))return Cause.NONE;
        return idle<=maximum?Cause.IDLE:Cause.MAXIMUM;
    }
    boolean interact(long now){if(!active()||cause(now)!=Cause.NONE)return false;last=now;return true;}
    long remaining(long now){return active()?Math.max(0,Math.min(last+IDLE_MS,started+MAX_MS)-now):0;}
}
