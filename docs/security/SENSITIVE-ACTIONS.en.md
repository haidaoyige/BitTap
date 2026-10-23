[简体中文](SENSITIVE-ACTIONS.md) · [English](SENSITIVE-ACTIONS.en.md) · [文档目录 / Documentation](../README.md)

# Android 9 private-key copy restriction and obscured-touch protection

Historical 0.5.0 step implemented only these two approved changes, not other password/address/foreground-session proposals.

## Behavior

### Android 9/API28

Four-language private-QR notice; WIF copy disabled with lock/unsupported. Both request-consent and actual-copy entry reject by OS version before constructing WIF/ClipData. Address copy/private QR/consented private-image save remain. QR failure never suggests disabled copy. Android 10+ retains consent/exact clipboard readback/sensitive mark/short cleanup, without claiming an absolutely secure clipboard.

### Overlay protection

Main and all AppUi bottom sheets share ProtectedScrollView, including key/QR/copy/save/password/overwrite/permanent-lock. On OS obscuration flag send CANCEL to original target, revoke pending input and swallow remaining gesture. Even clean DOWN then obscured MOVE/UP cannot click. Next clean DOWN may retry; cancelled gesture never resurrects.

All windows keep FLAG_SECURE; Android 12+ HIDE_OVERLAY_WINDOWS/setHideOverlayWindows(true) asks OS to hide third-party non system overlays. Approved orange/warm-white retry page says action not executed, closes secret sheets and clears owned key/QR/export buffers. Returning from secret goes to password page without restoring secret/password. During NFC/file I/O reject new obscured touches/show retry, without forcibly undoing external writes already underway. Camera interruption stops/clears photo+entropy and returns settings; no capture behind warning. Four languages, correct return target after recreation, no wallet/secrets in Bundle.

## Validation

Dedicated Android 9/API28 and 15/API35 ARM64 emulators; don't overwrite previous release-signed installation. Public scalar-one, no funds. [Evidence](sensitive-actions/validation.json) and raw logs.

- Actual installed OS check, no Build. VERSION spoof/backdoor.
- API28 disabled/programmatic/direct consent/direct copy cannot write WIF; address copy and decodable private QR work.
- API35 original consent/actual clipboard/sensitive mark/window overlay attributes checked.
- Synthesized full/partial flags through actual touch dispatch at DOWN/MOVE/UP, entire gesture cancellation and clean retry. Tests OS-flag response, not real malicious-overlay penetration.
- Copy, private-image, overwrite, permanent-lock: clipboard unchanged, no document export/pending PNG/NFC write-lock state.
- Four languages, password return, stale copy/save, rotation/key cleanup.
- Existing SmokeRunner animation waits made bounded while keeping assertions.
- Full SmokeRunner QR/languages/images/files/password/backup/NFC-sheet regression. Emulator NFC checks UI/state, not physical RF.

```sh
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
adb -s <dedicated-emulator> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <dedicated-emulator> install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s <dedicated-emulator> shell am instrument -w -e protectionOnly true io.coingift.android.test/io.coingift.android.SmokeRunner
```

## Limits / not implemented then

Android 9 exposes only full-obscuration, 10+ full/partial, 12+ overlay hiding. Cannot stop unreported overlays or guarantee against root/injection/keyboards/all privileged accessibility/system-trusted overlays. Accessibility not globally disabled at this stage; semantics need separate review. External exports cannot be recalled/JVM history not forensic-erased.

Existing copy copy-text unchanged; no 120/60 lock, per-address tags or verify-and-immediately-clear flow in this step. Later [weak-password warning](WEAK-PASSWORD-CONFIRMATION.en.md) separately approved/tested; original evidence remains for original two fixes. Closes only BTA-03 Android 9 WIF path; BTA-04 overlay portion implemented here, semantics not yet complete then.

References: [Tap jacking](https://developer.android.com/privacy-and-security/risks/tapjacking), [Sensitive activities](https://developer.android.com/security/fraud-prevention/activities). Later Android 12 floor removes obsolete Android 9 paths; this remains historical.
