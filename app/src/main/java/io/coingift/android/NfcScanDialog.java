package io.coingift.android;

import android.app.Dialog;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The prototype's NFC bottom sheet. Hardware callbacks, never a timer, advance the flow. */
final class NfcScanDialog extends Dialog {
    private final TextView heading;
    private TextView status;
    private Button cancel;

    NfcScanDialog(AppUi ui,String title,String description,String waiting,Runnable onCancel){
        super(ui.activity);
        heading=ui.showSheet(this,title,p->{
            ui.note(p,description);
            p.addView(new WalletArtView(ui.activity,true,true),new LinearLayout.LayoutParams(-1,ui.dp(236)));
            status=ui.text(p,getContext().getString(R.string.nfc_scan_status,Texts.tr(ui.activity,waiting)),12,AppUi.ACCENT,false);
            status.setGravity(Gravity.CENTER);status.setPadding(0,ui.dp(12),0,ui.dp(12));
            status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
            ui.space(p,12);cancel=ui.link(p,"取消",0,onCancel);
        },()->{if(cancel!=null&&cancel.isEnabled())onCancel.run();});
        setCanceledOnTouchOutside(false);setOnCancelListener(d->onCancel.run());
        if(getWindow()!=null)getWindow().setDimAmount(.31f);
    }

    void processing(String title,String message){
        heading.setText(Texts.tr(getContext(),title));status.setText(getContext().getString(R.string.nfc_scan_status,Texts.tr(getContext(),message)));
        // Cancellation abandons this attempt; it never implies a write or lock was rolled back.
        // Keep an escape route even while a device's NFC service is not responding.
        setCancelable(true);cancel.setEnabled(true);
    }
}
