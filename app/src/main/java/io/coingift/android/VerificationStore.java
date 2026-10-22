package io.coingift.android;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.AtomicFile;
import io.coingift.core.BitcoinAddresses;
import io.coingift.core.WalletCrypto;
import io.coingift.core.WalletFingerprint;
import io.coingift.core.WalletNote;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.*;

/** Authenticated, device-local PUBLIC verification history; never stores wallet decryption material. */
final class VerificationStore {
    private static final Object LOCK=new Object();
    private static final int MAX_FILE=262144,MAX_RECORDS=256;
    private static final byte[] AAD="BitTap/LocalVerificationStore/v1".getBytes(StandardCharsets.US_ASCII);
    private final AtomicFile file;
    private final File backingFile;
    private final String alias;
    VerificationStore(Context context){this(context,"wallet");}
    VerificationStore(Context context,String namespace){
        if(!namespace.matches("[a-z0-9-]{1,50}"))throw new IllegalArgumentException("Invalid namespace");
        backingFile=new File(context.getNoBackupFilesDir(),"address-records-v1-"+namespace+".bin");file=new AtomicFile(backingFile);
        alias="bittap-address-records-v1-"+namespace;
    }
    static final class Record {
        private final byte[] digest;
        final String id,note;
        final BitcoinAddresses addresses;
        final long created;
        final Set<String> cardIds;
        private Record(byte[] digest,BitcoinAddresses addresses,String note,long created,Collection<String> cardIds){
            if(digest.length!=32||created<0||addresses==null||!WalletNote.normalize(note).equals(note)||cardIds.size()>32)throw new IllegalArgumentException("Invalid record");
            for(String id:cardIds)requireCardId(id);
            this.digest=digest.clone();id=WalletCrypto.hex(digest);this.addresses=addresses;this.note=note;this.created=created;
            this.cardIds=Collections.unmodifiableSet(new LinkedHashSet<>(cardIds));
        }
        static Record verified(String payload,BitcoinAddresses addresses,String note,Collection<String> cardIds){
            byte[] hash=WalletFingerprint.of(payload);
            try{
                if(WalletCrypto.isBackup(payload)){
                    WalletCrypto.Metadata meta=new WalletCrypto().inspect(payload);
                    if(!meta.address.equals(addresses.nativeSegwit)||!meta.note.equals(note)||(meta.addresses!=null&&!addresses.sameAs(meta.addresses)))throw new IllegalArgumentException("Verified metadata mismatch");
                }else if(!note.isEmpty())throw new IllegalArgumentException("Invalid legacy note");
                return new Record(hash,addresses,note,System.currentTimeMillis(),cardIds);
            }finally{WalletCrypto.wipe(hash);}
        }
        boolean matches(String payload){return WalletFingerprint.matches(digest,payload);}
        boolean containsAddress(String value){return addresses.nativeSegwit.equals(value)||addresses.legacy.equals(value)||addresses.nestedSegwit.equals(value)||addresses.taproot.equals(value);}
    }
    static final class Lookup {
        final Record match,previous;
        Lookup(Record match,Record previous){this.match=match;this.previous=previous;}
    }
    Lookup lookup(String payload,String cardId)throws Exception{
        byte[] hash=WalletFingerprint.of(payload);
        try{synchronized(LOCK){Record previous=null;for(Record r:read()){
            if(MessageDigest.isEqual(r.digest,hash))return new Lookup(r,null);
            if(cardId!=null&&r.cardIds.contains(cardId)&&(previous==null||r.created>previous.created))previous=r;
        }return new Lookup(null,previous);}}finally{WalletCrypto.wipe(hash);}
    }
    List<Record> records()throws Exception{synchronized(LOCK){return new ArrayList<>(read());}}
    void save(Record record)throws Exception{synchronized(LOCK){
        List<Record> rows=read();Record previous=null;
        for(Record r:rows)if(r.id.equals(record.id)){previous=r;break;}
        if(previous!=null){
            if(!previous.note.equals(record.note)||!previous.addresses.sameAs(record.addresses))throw new GeneralSecurityException("Record metadata mismatch");
            LinkedHashSet<String> ids=new LinkedHashSet<>(previous.cardIds);ids.addAll(record.cardIds);
            if(ids.size()>32)throw new IOException("Too many card associations");
            rows.remove(previous);record=new Record(record.digest,record.addresses,record.note,previous.created,ids);
        }else if(rows.size()>=MAX_RECORDS)throw new IOException("Local verification record limit reached");
        rows.add(record);write(rows);
    }}
    void remove(String id)throws Exception{synchronized(LOCK){List<Record> rows=read();rows.removeIf(r->r.id.equals(id));if(rows.isEmpty())clear();else write(rows);}}
    void clear()throws Exception{synchronized(LOCK){
        file.delete();if(exists())throw new IOException("Cannot clear local records");
        KeyStore ks=store();if(ks.containsAlias(alias))ks.deleteEntry(alias);
    }}
    private static void requireCardId(String id){if(id==null||!id.matches("[0-9a-f]{2,64}")||(id.length()&1)!=0)throw new IllegalArgumentException("Invalid card hint");}
    private static KeyStore store()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);return ks;}
    private SecretKey key(boolean create)throws Exception{
        KeyStore ks=store();java.security.Key present=ks.getKey(alias,null);
        if(present instanceof SecretKey)return (SecretKey)present;
        if(present!=null||!create)throw new GeneralSecurityException("Record protection key unavailable");
        KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true).setUserAuthenticationRequired(false).build());
        SecretKey result=gen.generateKey();byte[] exported=result.getEncoded();
        if(exported!=null){WalletCrypto.wipe(exported);ks.deleteEntry(alias);throw new GeneralSecurityException("Record key must not be exportable");}return result;
    }
    private List<Record> read()throws Exception{
        if(!exists())return new ArrayList<>();
        byte[] sealed=null,plain=null;
        try(InputStream in=file.openRead()){
            ByteArrayOutputStream bounded=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(bounded.size()+n>MAX_FILE)throw new IOException("Record file too large");bounded.write(buffer,0,n);}sealed=bounded.toByteArray();if(sealed.length<29||sealed.length>MAX_FILE||sealed[0]!=1)throw new IOException("Invalid local record file");
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(false),new GCMParameterSpec(128,sealed,1,12));cipher.updateAAD(AAD);
            plain=cipher.doFinal(sealed,13,sealed.length-13);
            JSONObject root=new JSONObject(new String(plain,StandardCharsets.UTF_8));if(root.getInt("version")!=1)throw new IOException("Unknown local record schema");
            JSONArray entries=root.getJSONArray("records");if(entries.length()>MAX_RECORDS)throw new IOException("Record limit exceeded");
            List<Record> result=new ArrayList<>();Set<String> unique=new HashSet<>();
            for(int i=0;i<entries.length();i++){
                JSONObject r=entries.getJSONObject(i);if(!"mainnet".equals(r.getString("network")))throw new IOException("Wrong record network");
                String id=r.getString("digest");if(!id.matches("[0-9a-f]{64}")||!unique.add(id))throw new IOException("Invalid digest");
                byte[] hash=new byte[32];for(int j=0;j<32;j++)hash[j]=(byte)Integer.parseInt(id.substring(j*2,j*2+2),16);
                try{
                    JSONArray a=r.getJSONArray("addresses");if(a.length()!=4)throw new IOException("Invalid address set");
                    BitcoinAddresses addresses=BitcoinAddresses.fromPublicAddresses(a.getString(1),a.getString(2),a.getString(0),a.getString(3));
                    JSONArray hints=r.getJSONArray("cards");if(hints.length()>32)throw new IOException("Too many hints");List<String> ids=new ArrayList<>();for(int j=0;j<hints.length();j++)ids.add(hints.getString(j));
                    result.add(new Record(hash,addresses,r.getString("note"),r.getLong("created"),ids));
                }finally{WalletCrypto.wipe(hash);}
            }return result;
        }finally{WalletCrypto.wipe(sealed);WalletCrypto.wipe(plain);}
    }
    private void write(List<Record> rows)throws Exception{
        JSONArray entries=new JSONArray();
        for(Record r:rows){JSONObject value=new JSONObject();value.put("digest",r.id);value.put("network","mainnet");value.put("note",r.note);value.put("created",r.created);
            value.put("addresses",new JSONArray(Arrays.asList(r.addresses.nativeSegwit,r.addresses.legacy,r.addresses.nestedSegwit,r.addresses.taproot)));
            value.put("cards",new JSONArray(r.cardIds));entries.put(value);}
        JSONObject root=new JSONObject();root.put("version",1);root.put("records",entries);
        byte[] plain=root.toString().getBytes(StandardCharsets.UTF_8),sealed=null;FileOutputStream out=null;
        try{
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key(true));cipher.updateAAD(AAD);sealed=cipher.doFinal(plain);byte[] iv=cipher.getIV();
            if(iv.length!=12||sealed.length+13>MAX_FILE)throw new IOException("Invalid local record size");
            out=file.startWrite();out.write(1);out.write(iv);out.write(sealed);file.finishWrite(out);out=null;
        }finally{if(out!=null)file.failWrite(out);WalletCrypto.wipe(plain);WalletCrypto.wipe(sealed);}
    }
    private boolean exists(){return backingFile.exists()||new File(backingFile.getPath()+".bak").exists()||new File(backingFile.getPath()+".new").exists();}
}
