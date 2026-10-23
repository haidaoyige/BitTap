[简体中文](README.md) · [English](README.en.md) · [文档目录 / Documentation](../../README.md)

# BitTap 0.4.0/code 7 candidate validation

2026-10-16; JDK17, SDK35, Android 15 ARM64 emulator. APK `app/build/outputs/bittap/BitTap-0.4.0-code7-candidate.apk`.

- Build/sign/lint passed; original certificate retained (old certificate name for updates).
-62 core+14 Android=76 units, no failures, individual XML/UNIT-RESULTS.json archived.
- Final APK210 device assertions: DEVICE-SMOKE-RESULT.txt.
- Same APK7 Camera2 end-to-end assertions: CAMERA-RESULT.txt. Actual emulator camera, image hashing, final-photo timestamp matching, system randomness mixing and wallet creation; not injected digests.
- Initial camera test without extra wait failed/stopped at final photo; cause was not classified then. Longer capture spacing passed. Claims cover this success/fail-stop only, not real-camera entropy/all-device compatibility.
- Independent Python recovery/public-field authentication passed. Java CLI hidden-input CG3 recovery showed authenticated public note/addresses only, not WIF.
- APK-MANIFEST.txt records name/four-language resources/permissions: NFC, CAMERA, no INTERNET/storage, android Test/test passwords/scalar fixtures absent; run-as refused.
- Screenshots/recovery data use public scalar or unfunded temporary wallets; never fund fixtures.

Camera enhancement is not quantified extra security. Read-only units/UI do not establish physical NTAG215/216 write/lock/cross-phone overwrite refusal/interruption acceptance. Not full independent audit. Historical newer app reads CG2/CG1/compressed BIP38, older app cannot read CG3; retain new CLI/spec with CG3.


## Original artifact identifiers

`app/build/outputs/bittap/BitTap-0.4.0-code7-candidate.apk`

`58b6613e32239ddf229066fa6bdf1b77579ade691a5d29b5bf621341a3c2b85a`
