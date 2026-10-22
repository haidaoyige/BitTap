package io.coingift.android;

import android.view.Window;
import android.view.WindowManager;

final class WindowProtection {
    private WindowProtection() {}
    static void secure(Window window) {
        if (window == null) return;
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        window.setHideOverlayWindows(true);
    }
}
