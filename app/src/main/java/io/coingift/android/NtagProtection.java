package io.coingift.android;

import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import java.io.Closeable;
import java.util.Arrays;

/** Explicit NTAG215/216 map only. NXP NTAG213_215_216 Rev 3.2, sections 8.5 and 8.8. */
final class NtagProtection {
    interface Link extends Closeable {
        void connect()throws Exception;void timeout(int millis)throws Exception;
        byte[] version()throws Exception;byte[] readPages(int page)throws Exception;void writePage(int page,byte[] value)throws Exception;
    }
    private static Link link(Tag tag){
        MifareUltralight m=MifareUltralight.get(tag);
        if(m==null)throw new IllegalArgumentException("不支持此卡片的永久只读锁定");
        return new Link(){
            public void connect()throws Exception{m.connect();}public void timeout(int millis){m.setTimeout(millis);}
            public byte[] version()throws Exception{return m.transceive(new byte[]{0x60});}
            public byte[] readPages(int page)throws Exception{return m.readPages(page);}
            public void writePage(int page,byte[] value)throws Exception{m.writePage(page,value);}
            public void close()throws java.io.IOException{m.close();}
        };
    }
    static int model(byte[] v){
        if(v==null||v.length!=8||v[0]!=0||v[1]!=4||v[2]!=4||v[3]!=2||v[4]!=1||v[5]!=0||v[7]!=3)return 0;
        return v[6]==0x11?215:v[6]==0x13?216:0;
    }
    static int dynamicPage(int model){if(model!=215&&model!=216)throw new IllegalArgumentException("不支持此卡片的永久只读锁定");return model==215?130:226;}
    static boolean locked(int model,byte[] stat,byte[] dynamic,byte[] config){
        if(stat==null||stat.length<8||dynamic==null||dynamic.length<4||config==null||config.length<8)return false;
        return (stat[2]&0xf8)==0xf8&&(stat[3]&255)==255&&(stat[7]&255)==15
            &&(dynamic[0]&255)==255&&(model==215||(model==216&&(dynamic[1]&0x3f)==0x3f))&&(config[4]&0x40)!=0;
    }
    static void inspect(Tag tag,NfcIo.Job job)throws Exception{inspect(link(tag),job);}
    static void lock(Tag tag,NfcIo.Job job)throws Exception{lock(link(tag),job);}
    static void verify(Tag tag,NfcIo.Job job)throws Exception{verify(link(tag),job);}
    static void inspect(Link m,NfcIo.Job job)throws Exception{
        job.own(m);
        try{connect(m,job);int model=model(version(m,job));int page=dynamicPage(model);preflight(model,read(m,2,job),read(m,page,job),read(m,page+1,job));}
        finally{job.release(m);}
    }
    static void lock(Link m,NfcIo.Job job)throws Exception{
        job.phase(NfcIo.Phase.CHIP_CONNECT);job.own(m);
        try{
            connect(m,job);job.phase(NfcIo.Phase.CHIP_VERSION);int model=model(version(m,job));int page=dynamicPage(model);
            job.phase(NfcIo.Phase.CHIP_CONFIG);byte[] stat=read(m,2,job),dynamic=read(m,page,job),config=read(m,page+1,job);
            preflight(model,stat,dynamic,config); // Validate all irreversible targets before the first write.
            job.phase(NfcIo.Phase.CC_LOCK);
            byte[] cc=Arrays.copyOfRange(stat,4,8);cc[3]|=15;
            writeIfChanged(m,3,Arrays.copyOfRange(stat,4,8),cc,job);
            same(cc,Arrays.copyOf(read(m,3,job),4));
            job.phase(NfcIo.Phase.DYNAMIC_LOCK);
            byte[] old=dynamicValue(model,dynamic),desired=old.clone();desired[0]=(byte)255;if(model==216)desired[1]=0x3f;
            // RFUI must be zero on WRITE, even though byte 3 reads back BDh.
            writeIfChanged(m,page,old,desired,job);same(desired,dynamicValue(model,read(m,page,job)));
            job.phase(NfcIo.Phase.CONFIG_LOCK);
            old=Arrays.copyOfRange(config,4,8);desired=old.clone();desired[0]|=0x40;
            writeIfChanged(m,page+2,old,desired,job);same(desired,Arrays.copyOfRange(read(m,page+1,job),4,8));
            job.phase(NfcIo.Phase.STATIC_LOCK);
            old=Arrays.copyOf(stat,4);desired=old.clone();desired[2]|=(byte)0xf8;desired[3]=(byte)255;
            writeIfChanged(m,2,old,desired,job);same(desired,Arrays.copyOf(read(m,2,job),4));
            // This readback confirms programmed bits only; the separate re-tap verifies the final state.
            requireLocked(model,read(m,2,job),read(m,page,job),read(m,page+1,job));
        }finally{job.release(m);}
    }
    static void verify(Link m,NfcIo.Job job)throws Exception{
        job.own(m);
        try{connect(m,job);int model=model(version(m,job));int page=dynamicPage(model);byte[] stat=read(m,2,job),dynamic=read(m,page,job),config=read(m,page+1,job);
            preflight(model,stat,dynamic,config);requireLocked(model,stat,dynamic,config);
        }finally{job.release(m);}
    }
    private static byte[] dynamicValue(int model,byte[] value){
        return new byte[]{value[0],model==216?(byte)(value[1]&0x3f):0,(byte)(value[2]&(model==215?15:0x7f)),0};
    }
    private static void preflight(int model,byte[] stat,byte[] dynamic,byte[] config){
        int size=stat[6]&255,access=stat[7]&255;
        if((stat[4]&255)!=0xe1||(stat[5]&255)!=0x10||size==0||size>(model==215?63:111)||(access!=0&&access!=15))
            throw new IllegalArgumentException("卡片 NDEF 配置不受支持，已停止锁定");
        if((config[0]&0xc0)!=0||(config[3]&255)!=255)throw new IllegalArgumentException("卡片已有镜像或访问保护配置，请换一张卡片");
        int s0=stat[2]&255,s1=stat[3]&255;
        if(((s0&1)!=0&&(s0&8)==0)||((s0&2)!=0&&((s0&0xf0)!=0xf0||(s1&3)!=3))||((s0&4)!=0&&(s1&0xfc)!=0xfc))
            throw new IllegalArgumentException("卡片锁位已被部分冻结，无法确认完整只读");
        int flags=(dynamic[0]&255)|(model==216?(dynamic[1]&0x3f)<<8:0),blocks=dynamic[2]&(model==215?15:0x7f);
        for(int i=0;i<(model==215?4:7);i++)if((blocks&(1<<i))!=0&&((flags>>(i*2))&3)!=3)
            throw new IllegalArgumentException("卡片锁位已被部分冻结，无法确认完整只读");
    }
    private static void requireLocked(int model,byte[] stat,byte[] dynamic,byte[] config){
        if(!locked(model,stat,dynamic,config))throw new IllegalStateException("硬件锁定状态未确认，请保存备份后重新读卡检查");
    }
    private static void same(byte[] expected,byte[] actual){if(!Arrays.equals(expected,actual))throw new IllegalStateException("锁定位写入后读回不一致，锁定未确认");}
    private static void writeIfChanged(Link m,int page,byte[] old,byte[] value,NfcIo.Job job)throws Exception{if(!Arrays.equals(old,value)){job.check();m.writePage(page,value);job.check();}}
    private static void connect(Link m,NfcIo.Job job)throws Exception{job.check();m.connect();job.check();m.timeout(3000);job.check();}
    private static byte[] version(Link m,NfcIo.Job job)throws Exception{job.check();byte[] value=m.version();job.check();return value;}
    private static byte[] read(Link m,int page,NfcIo.Job job)throws Exception{job.check();byte[] value=m.readPages(page);job.check();if(value==null||value.length!=16)throw new IllegalStateException("卡片硬件数据不完整，已停止锁定");return value;}
}
