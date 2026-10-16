package io.coingift.android;

import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import java.util.Arrays;

/** Restricted NTAG215/216 layout, per NXP NTAG213_215_216 Rev 3.2. No generic-tag lock guesses. */
final class NtagProtection {
    static int model(byte[] v){
        if(v==null||v.length!=8||v[0]!=0||v[1]!=4||v[2]!=4||v[3]!=2||v[4]!=1||v[5]!=0||v[7]!=3)return 0;
        return v[6]==0x11?215:v[6]==0x13?216:0;
    }
    static int dynamicPage(int model){if(model!=215&&model!=216)throw new IllegalArgumentException("不支持此卡片的永久只读锁定");return model==215?130:226;}
    static boolean locked(int model,byte[] staticPages,byte[] dynamic,byte[] config){
        if(staticPages==null||staticPages.length<8||dynamic==null||dynamic.length<4||config==null||config.length<8)return false;
        return (staticPages[2]&0xf8)==0xf8&&(staticPages[3]&255)==255&&(staticPages[7]&15)==15
            &&(dynamic[0]&255)==255&&(model==215||(model==216&&(dynamic[1]&0x3f)==0x3f))&&(config[4]&0x40)!=0;
    }
    static void inspect(Tag tag) throws Exception {withTag(tag,false);}
    static void lock(Tag tag) throws Exception {withTag(tag,true);}
    private static void withTag(Tag tag,boolean write) throws Exception {
        MifareUltralight m=MifareUltralight.get(tag);if(m==null)throw new IllegalArgumentException("不支持此卡片的永久只读锁定");
        try{m.connect();int model=model(m.transceive(new byte[]{0x60}));int page=dynamicPage(model);
            byte[] cfg=m.readPages(page+1);
            if(cfg.length<8||(cfg[0]&0xc0)!=0||(cfg[3]&255)!=255)throw new IllegalArgumentException("卡片已有镜像或访问保护配置，请换一张卡片");
            if(write){
                // One-way bits only, preserve manufacturer/config data and reserved bits.
                byte[] dynamic=Arrays.copyOf(m.readPages(page),4);dynamic[0]=(byte)255;if(model==216)dynamic[1]|=0x3f;
                if(!Arrays.equals(dynamic,Arrays.copyOf(m.readPages(page),4)))m.writePage(page,dynamic);
                byte[] access=Arrays.copyOfRange(cfg,4,8);access[0]|=0x40;if((cfg[4]&0x40)==0)m.writePage(page+2,access);
                byte[] stat=Arrays.copyOf(m.readPages(2),4);stat[2]|=(byte)0xf8;stat[3]=(byte)255;if(!Arrays.equals(stat,Arrays.copyOf(m.readPages(2),4)))m.writePage(2,stat);
            }
        }finally{m.close();}
    }
    static void verify(Tag tag) throws Exception {
        MifareUltralight m=MifareUltralight.get(tag);if(m==null)throw new IllegalStateException("无法核验硬件只读状态");
        try{m.connect();int model=model(m.transceive(new byte[]{0x60}));int page=dynamicPage(model);
            if(!locked(model,m.readPages(2),m.readPages(page),m.readPages(page+1)))throw new IllegalStateException("硬件锁定状态未确认，请保存备份后重新读卡检查");
        }finally{m.close();}
    }
}
