[简体中文](android12-minimum.md) · [English](android12-minimum.en.md) · [文档目录 / Documentation](../README.md)

# Android 12 minimum — code 41

Historical development 0.6.2/code 41 sets minSdk 31 (Android 12+); target/compile 35 unchanged. Android 9–11 cannot install. Older release attachments/evidence remain unchanged and do not acquire these fixes.

## Simplification

- Remove Android 9-only WIF clipboard prohibition, localized warnings and old-platform tests.
- Activity/all protected sheets uniformly use FLAG_SECURE and setHideOverlayWindows(true); no legacy fallback for full/partial obscuration.
- Modern system-bar/IME insets for pages/passwords; exclude private presentation from content capture; modern haptics.
- Remove obsolete low-API inline compatibility comments/lint suppressions.
- Retain Android 13 back-gesture and Android 14 sensitive-accessibility API checks.

No layout/copy/normal-flow, randomness, derivation, format, scrypt/AES-GCM, NFC or deadline changes. Export consent, clipboard verification/cleanup, accessibility protection and automatic locking remain.

## Boundaries/tests

A higher OS floor unifies capabilities; it does not close BTA-02, exported clipboard copies, weak-strength misclassification or receiving before backup, nor prove patch freshness/trusted hardware.

Only public vectors/unfunded temporary wallets. Automation keeps emulator awake to avoid screen-off clipboard permission changes; production does not. Idle waits use actual remaining deadline after QR preparation, without clock reset/acceleration. Clipboard assertions wait for focus and read full values; denial/null is not protection success.

## 2026-10-21 results

- Debug/unsigned Release/test APK builds; 102 unit tests passed. Release lint 0 error/fatal, 9 warnings.
- APK min 31/target 35; Android 9 actual install rejected INSTALL_FAILED_OLDER_SDK.
- Android 15: creation 673, recovery 296, copy 80, overlay 137, keyboard 88 assertions passed, not distinct vulnerability counts.
- Android 12 retest: creation 673, recovery 300, copy 80, overlay 137, keyboard 88, deadlines/files 64, actual accessibility/private-pixel equivalence 109 passed. Overlapping assertions cannot be summed as independent assurances.
- Single cold-started emulator, 3072MB/4 cores/current software renderer. One-minute launcher/settings focus observation before regression; System UI stable, crash buffer empty afterward, last anr no ANR since boot. Earlier failures retained.
- Actual 60-second idle wait passed; 120-second absolute tested at boundary, not claimed as full real-time wait. Fault-provider cancellation/timeout/late-result tests passed.
- Android 12 may deny full screen automated screenshots with password IME. App-view-render fallback records capture mode, excludes system keyboard and is not presented as full-screen capture. Actual WindowInsets, coordinates and visible-rectangle assertions verify input visibility.
- Retest changed only screenshot tooling/evidence, not production UI/security. 102 units are prior-round results; seven listed Android 12 device suites were rerun.
- Physical NFC, OEMs and phones still need acceptance. Emulator results are not funds-security certification.

No new signed release or GitHub publication in that round. Existing audit risks do not close merely from the higher OS minimum.
