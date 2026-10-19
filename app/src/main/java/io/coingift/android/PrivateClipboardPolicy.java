package io.coingift.android;

/** Android 9 permits background clipboard reads; never export a WIF or generated password to it. */
final class PrivateClipboardPolicy {
    private PrivateClipboardPolicy() {}
    static boolean allowsCopy(int sdk) { return sdk >= 29; }
}
