[简体中文](WEAK-PASSWORD-CONFIRMATION.md) · [English](WEAK-PASSWORD-CONFIRMATION.en.md) · [文档目录 / Documentation](../README.md)

# Weak-password warning before wallet generation

Historical approved risk disclosure under version 0.5.0. One explicit confirmation for the current creation input; existing WIF copy consent unchanged.

## Behavior

- Validate minimum, matching confirmation, note and camera state before warning. Invalid input gets existing errors first.
- Existing `PasswordStrength` weak classification opens an orange/warm-white sheet; medium/strong proceeds. Heuristics are not entropy measurements or unbreakability guarantees.
- Explain offline guessing on copied cards/backups, which in-app retry limits cannot stop, and that camera mixing cannot repair a weak password. Both generation modes show this.
- Primary Return/set stronger closes and focuses password. Secondary Understand/use anyway begins generation, encryption and recovery self-check. Cancel/back/outside creates nothing.
- No don't-ask-again/persisted consent; each new weak request confirms again. Four app languages, secure window, overlay protection and scrollable sheet.

## Data/lifecycle

No key generation or password handoff to worker before warning. Temporary validation/strength `char[]` is wiped first; no password in text/logs/storage. Consent binds sheet/page/session/input revision; password, confirmation, note or enhancement-source changes invalidate it. Continue rereads/revalidates. Existing post-generation/background input cleanup and entropy consumption/destruction remain; cancelling warning does not consume collected entropy. Background, recreation, Home or overlay interception closes it; stale buttons cannot authorize generation.

## Validation

`WeakPasswordProbe` uses the actual Activity/dialog/generate/decrypt flow, dedicated emulator and public passwords, no real BTC.

```sh
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
adb -s <dedicated-emulator> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <dedicated-emulator> install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s <dedicated-emulator> shell am instrument -w -e weakPasswordOnly true io.coingift.android.test/io.coingift.android.SmokeRunner
```

[Evidence](weak-password/validation.json). Screenshots render only the risk view from the test APK on the dedicated emulator; production secure-window flags stay enabled, no real keys captured.

## Limits

Weak passwords remain allowed: disclosure does not improve them or close residual BTA-01. The first warning change left minimum unchanged; later a ten-character core minimum was enforced, with [separate evidence](PASSWORD-MINIMUM-10.en.md). Original logs preserve their historical build. No KDF/format/RNG/camera-mixing changes, automatic timeout, new address workflow or password generator in this step.
