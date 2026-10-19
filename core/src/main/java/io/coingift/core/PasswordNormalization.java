package io.coingift.core;

import java.text.Normalizer;

/** One canonical representation for password assessment and KDF input. */
final class PasswordNormalization {
    private PasswordNormalization(){}
    static CharSequence nfc(CharSequence value){
        // Preserve whitespace and case; only canonical Unicode composition changes.
        // Do not create an extra immutable password copy when already canonical.
        return Normalizer.isNormalized(value,Normalizer.Form.NFC)?value:Normalizer.normalize(value,Normalizer.Form.NFC);
    }
}
