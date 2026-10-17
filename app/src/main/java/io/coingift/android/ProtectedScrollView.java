package io.coingift.android;

import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import android.widget.ScrollView;

/** Reject the entire gesture, including an obscured UP after a clean DOWN. */
@android.annotation.SuppressLint("ViewConstructor") // Constructed by AppUi, never inflated from XML.
class ProtectedScrollView extends ScrollView {
    private final Runnable onRejected;
    private boolean rejectedGesture;

    ProtectedScrollView(Context context, Runnable onRejected) {
        super(context);
        this.onRejected = onRejected;
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) rejectedGesture = false;
        int obscured = MotionEvent.FLAG_WINDOW_IS_OBSCURED;
        // Android 9 does not report partial occlusion; do not imply it can detect it.
        if (Build.VERSION.SDK_INT >= 29) obscured |= MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED;
        if ((event.getFlags() & obscured) != 0 && !rejectedGesture) {
            rejectedGesture = true;
            MotionEvent cancel = MotionEvent.obtain(event);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            try { super.dispatchTouchEvent(cancel); }
            finally { cancel.recycle(); }
            cancelPendingInputEvents();
            // Do not replace the active view hierarchy in the middle of touch dispatch.
            post(onRejected);
        }
        if (rejectedGesture) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) rejectedGesture = false;
            return true;
        }
        return super.dispatchTouchEvent(event);
    }
}
