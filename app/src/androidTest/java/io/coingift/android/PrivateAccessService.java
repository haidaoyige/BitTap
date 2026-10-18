package io.coingift.android;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

/** Test APK only. Deliberately includes non-important views and declares itself an accessibility tool. */
public final class PrivateAccessService extends AccessibilityService {
    private static final String FIXTURE="KwDiBf89QgGbjEhKnhXJuH7LrciVrZi3qYjgd9M7rFU73sVHnoWn";
    private static volatile PrivateAccessService active;
    private static volatile boolean eventLeak;
    private static volatile int events;
    @Override protected void onServiceConnected(){active=this;}
    @Override public void onDestroy(){if(active==this)active=null;super.onDestroy();}
    @Override public void onInterrupt(){}
    @Override public void onAccessibilityEvent(AccessibilityEvent event){
        if(!"io.coingift.android".contentEquals(event.getPackageName()==null?"":event.getPackageName()))return;
        events++;
        for(CharSequence text:event.getText())if(leaks(text))eventLeak=true;
        if(leaks(event.getContentDescription())||leaks(event.getBeforeText()))eventLeak=true;
    }
    private static boolean leaks(CharSequence value){return value!=null&&value.toString().contains(FIXTURE);}
    static void reset(){eventLeak=false;events=0;}
    static Bundle snapshot(){
        Bundle result=new Bundle();PrivateAccessService service=active;
        result.putBoolean("connected",service!=null);
        if(service!=null){
            if(Build.VERSION.SDK_INT>=31)result.putBoolean("tool",service.getServiceInfo().isAccessibilityTool());
            int[] nodes={0};boolean found=false;
            for(AccessibilityWindowInfo window:service.getWindows()){
                AccessibilityNodeInfo root=window.getRoot();
                if(root!=null)try{found|=inspect(root,nodes);}finally{root.recycle();}
            }
            result.putInt("nodes",nodes[0]);result.putBoolean("nodeLeak",found);
        }
        result.putBoolean("eventLeak",eventLeak);result.putInt("events",events);return result;
    }
    private static boolean inspect(AccessibilityNodeInfo node,int[] count){
        if("io.coingift.android".contentEquals(node.getPackageName()==null?"":node.getPackageName())){
            count[0]++;
            if(leaks(node.getText())||leaks(node.getContentDescription())||leaks(node.getHintText())||leaks(node.getTooltipText())
                    ||leaks(node.getPaneTitle())||leaks(node.getExtras().toString())
                    ||(Build.VERSION.SDK_INT>=30&&leaks(node.getStateDescription())))return true;
        }
        boolean found=false;
        for(int i=0;i<node.getChildCount();i++){
            AccessibilityNodeInfo child=node.getChild(i);
            if(child!=null)try{found|=inspect(child,count);}finally{child.recycle();}
        }
        return found;
    }
}
