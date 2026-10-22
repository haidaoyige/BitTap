package io.coingift.android;

import android.app.Activity;
import android.content.Context;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import java.lang.ref.WeakReference;

/** Only called by the bounded NFC control worker. No wallet state or strong Activity reference. */
final class NfcReaderService implements NfcReaderControl.Service,NfcAdapter.ReaderCallback {
    private final Context context;private final WeakReference<Activity> owner;private NfcAdapter adapter;private boolean readerRequested;private volatile String phase="NFC-Q";
    NfcReaderService(Activity activity){context=activity.getApplicationContext();owner=new WeakReference<>(activity);}
    @Override public NfcReaderControl.State enable(){
        requireWorker();
        Activity activity=owner.get();if(activity==null||activity.isFinishing()||activity.isDestroyed())return NfcReaderControl.State.UNAVAILABLE;
        if(adapter==null){phase="NFC-A";adapter=NfcAdapter.getDefaultAdapter(context);}
        if(adapter==null)return NfcReaderControl.State.UNSUPPORTED;
        phase="NFC-B";if(!adapter.isEnabled())return NfcReaderControl.State.DISABLED;
        phase="NFC-C";readerRequested=true;adapter.enableReaderMode(activity,this,NfcAdapter.FLAG_READER_NFC_A|NfcAdapter.FLAG_READER_NFC_B|NfcAdapter.FLAG_READER_NFC_F|NfcAdapter.FLAG_READER_NFC_V|NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,null);
        phase="NFC-R";return NfcReaderControl.State.READY;
    }
    @Override public void disable(){
        requireWorker();Activity activity=owner.get();
        // Do not create/register a framework reader state just to disable a reader never requested.
        if(adapter!=null&&readerRequested&&activity!=null&&!activity.isDestroyed()){
            phase="NFC-D";adapter.disableReaderMode(activity);readerRequested=false;
        }
        phase="NFC-I";
    }
    @Override public String diagnostic(){return phase;}
    private static void requireWorker(){if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())throw new IllegalStateException("NFC service control requires a worker thread");}
    @Override public void onTagDiscovered(Tag tag){Activity activity=owner.get();if(activity instanceof NfcAdapter.ReaderCallback)((NfcAdapter.ReaderCallback)activity).onTagDiscovered(tag);}
}
