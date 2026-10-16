package io.coingift.core;

import java.util.HashSet;
import java.util.Set;

/** Creation workflow holds ciphertext/public metadata only, never passwords or plaintext keys. */
public final class CreationSession {
    public final String encrypted, note;
    public final BitcoinAddresses addresses;
    private boolean backupSaved;
    private final Set<String> writtenCards=new HashSet<>();

    public CreationSession(String encrypted) {
        WalletCrypto.Metadata meta=new WalletCrypto().inspect(encrypted);
        if(meta.addresses==null)throw new IllegalArgumentException("新制卡流程需要四地址备份");
        this.encrypted=encrypted;this.addresses=meta.addresses;this.note=meta.note;
    }
    public boolean backupSaved(){return backupSaved;}
    public int cardCount(){return writtenCards.size();}
    public boolean needsBackup(){return cardCount()>0&&!backupSaved;}
    public boolean canFinish(){return cardCount()>0&&backupSaved;}
    public void markBackupSaved(String savedPayload) {
        if(!encrypted.equals(savedPayload))throw new IllegalArgumentException("导出的备份不属于当前钱包");
        backupSaved=true;
    }
    public void markCardWritten(byte[] uid) {
        if(uid==null||uid.length==0)throw new IllegalArgumentException("无法识别卡片");
        writtenCards.add(WalletCrypto.hex(uid));
    }
    public boolean containsCard(byte[] uid){return uid!=null&&writtenCards.contains(WalletCrypto.hex(uid));}
    public String[] writtenCardIds(){return writtenCards.toArray(new String[0]);}
    public void restoreWrittenCardIds(String[] ids) {
        if(ids==null)return;
        for(String id:ids)if(id!=null&&id.matches("[0-9a-f]{2,64}")&&id.length()%2==0)writtenCards.add(id);
    }
}
