[简体中文](UNVERIFIED-ADDRESS-IMAGE-SAVE.md) · [English](UNVERIFIED-ADDRESS-IMAGE-SAVE.en.md) · [文档目录 / Documentation](../README.md)

# Consent before saving an unverified receiving address

Historical approved choice: retain original address PNG without new marks; warn after Save image, before PNG generation/system picker. A later image-export revision is recorded separately.

- Pending addresses are public claims from a card/backup, not yet checked against the key. Tampering/mismatch can lose incoming BTC; saving does not verify the address.
- Understand risk/continue permits save. Cancel/close generates no PNG or picker, leaving QR visible. Each new pending-address request requires consent.
- Verified addresses save directly. Private QR retains existing plaintext-export consent/copy.
- Four languages, existing bottom-sheet style, secure window and obscured-touch protection.
- Consent binds current QR sheet, bitmap and session. Changing address, closing QR, backgrounding or obscured-touch rejection invalidates old buttons. Repeat taps cannot stack prompts/pickers.
- Existing write/close/exact-readback/cancel/timeout cleanup unchanged; no file cache or verification-state change.

## Validation

`SmokeRunner -e addressSaveOnly true`: four languages × four types; cancel/consent, original pixels/QR, actual write/readback, verified direct save, stale callbacks, background/overlay refusal. Public fixtures only. Test monitor intercepts picker launch; real test-file URIs exercise result processing.

```sh
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
adb -s <dedicated-emulator> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <dedicated-emulator> install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s <dedicated-emulator> shell am instrument -w -e addressSaveOnly true io.coingift.android.test/io.coingift.android.SmokeRunner
adb -s <dedicated-emulator> shell am instrument -w io.coingift.android.test/io.coingift.android.SmokeRunner
```

[Logs/results](address-save-consent/validation.json). The warning does not verify, prevent a consenting save or guarantee users will not fund an unchecked address. This step did not change image content, QR encoding, ciphertext or other unapproved verification flows.
