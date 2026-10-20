package io.coingift.android;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;

/** Any configured/enabled service blocks secrets; no package-name allowlist or test bypass. */
final class AccessibilityGate implements AutoCloseable {
    private final Context context;private final AccessibilityManager manager;
    private final ContentObserver observer;private final AccessibilityManager.AccessibilityStateChangeListener state;
    AccessibilityGate(Context context,Handler handler,Runnable changed){
        this.context=context;manager=(AccessibilityManager)context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        observer=new ContentObserver(handler){@Override public void onChange(boolean self){changed.run();}};
        context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),false,observer);
        context.getContentResolver().registerContentObserver(Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_ENABLED),false,observer);
        state=enabled->changed.run();if(manager!=null)manager.addAccessibilityStateChangeListener(state,handler);
    }
    boolean blocked(){return blocked(context);}
    static boolean blocked(Context context){
        try{
            AccessibilityManager manager=(AccessibilityManager)context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            String configured=Settings.Secure.getString(context.getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return configured!=null&&!configured.trim().isEmpty()||manager==null||!manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).isEmpty();
        }catch(RuntimeException ex){return true;}
    }
    @Override public void close(){context.getContentResolver().unregisterContentObserver(observer);if(manager!=null)manager.removeAccessibilityStateChangeListener(state);}
}
