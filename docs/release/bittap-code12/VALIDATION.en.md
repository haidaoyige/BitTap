[简体中文](VALIDATION.md) · [English](VALIDATION.en.md) · [文档目录 / Documentation](../../README.md)

# BitTap 0.4.0/code 12

Approved risk-first WIFPNG: logo, mainnet, plaintext exposure, QR, full WIF, move all BTC/stop old addresses/don't share-upload. Remove “not an encrypted backup.” Four languages; fixed English footer. Measure/wrap each language; full WIF centered, no ellipsis; QR encodes WIF only.

Generate only after consent, in memory; wipe/recycle bitmap after encoding. Existing cancel/fail/timeout/write-readback/byte cleanup, no temporary image cache. Picker/background clears unlocked key/page QR. Explicitly saved images persist. Owned-buffer cleanup does not promise physical erasure of all JVM/OS copies.

Release/lint and 14 app units pass; API35 image 143 assertions; 2 public WIF fixtures×4 languages decode at original/408 px. Actual risk/full WIF/move funds/stop old addresses un truncated. No encoding before consent, no cancelled PNG, pending data wipe, background key cleanup, explicit readback checked. 16 address images regression pass. No real keys, NFC, funds; never fund fixtures.

Unreadable old build cache: isolated-build.init.gradle moved outputs/Gradle state to/private/tmp, source/old APK unchanged. BUILD.txt/IMAGE-EXPORT.txt.


## Original artifact identifiers

`BitTap-0.4.0-code12-candidate.apk`

`928594acae45e225a5acae6b3615459d1ff064543c0687c7040628224cb472cc`

`58b6613e32239ddf229066fa6bdf1b77579ade691a5d29b5bf621341a3c2b85a`
