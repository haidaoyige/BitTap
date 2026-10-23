[简体中文](SECURITY-REVIEW.md) · [English](SECURITY-REVIEW.en.md) · [文档目录 / Documentation](../README.md)

# 0.4.0/code 6 release review

2026-10-16 Asia/Shanghai, implementer review of selected changes, not independent audit/funds approval. [Earlier findings](../SECURITY-REVIEW-0.4.0.en.md).

APK `BitNFC-0.4.0-release.apk`, SHA256 `351fee333e2f1795dcfe7c18fb143e79edc9100f5f23518884c7dc47307d1b26`. Certificate `58b6613e32239ddf229066fa6bdf1b77579ade691a5d29b5bf621341a3c2b85a`, same code 4/5. Code 5 locally archived, not committed/pushed.

## Changes/checks

Unified Show address, centered full wrapping QR address, SegWit/P2SH labels, unchanged construction/backup fields; no single-line shrinking. Remove encrypted-text copy/paste/routes, retain NFC/file/NDEF Text compatibility. Address/WIF copy unique label/exact text readback: denial/unavailable/stale/different/extra item never success; uncertain WIF still owned-cleanup tracked.

Explicit PNG via ACTION_CREATE_DOCUMENT, private plaintext consent, no storage permission/gallery/app file/cache. Temporary encoder buffers wiped. Pending bytes only on request, cleared cancel/complete/failure/historical 3 min timeout/destroy/Home; no savedInstanceState/RetainedFlow. Picker closes QR/locks key; worker finally wipes active bytes. Close/exact readback required, image not encrypted backup/does not satisfy backup. Failed provider may leave partial file; warn unconfirmed, never delete chosen file without authorization.

No crypto/RNG/dependency/CG2 change. 51 core+4QR+6 image I/O units; close failure/denied read-write/null/mismatch/truncated/extra tests. Final APK162API35 assertions including exact PNG decode, consent, cancel, timeout/background, center/buttons, existing wallet/NFC/backup/multi card. Picker launch intercepted, real URI supplied to normal handler with activity departure/return; not full system-picker automation.

15 security reproduction assertions explicitly show plaintext in-process after decryption, 30 sec masking not lock, viewed WIF accessible. Background closes/wipes owned scalar; default UI/saved state no WIF, never auto copy. V2 signature, not debuggable/testOnly, NFC only, backup/cleartext networking disabled, run-as refused, no test Runner/password/scalar markers. Test APKs/screenshots removed. Release lint/diff check clean. Source snapshot included uncommitted work; HEAD alone cannot reproduce; no signer/credentials in source.

## Limits

Weak passwords advisory/offline guessable; pending addresses not authenticated; cards copyable. Explicit WIFPNG plaintext can control BTC, external providers/cloud/gallery/OS outside control; exported images not deleted on Home. JVM/library/keyboard/clipboard history not forensic-erased. Rotation/process death during picker cancels pending image, re decrypt required.

Dependencies use prior review, no new advisory query/upgrade. No physical NFC interruption/cloning, OEM permissions, malicious keyboard/accessibility, full heap forensics or external spending; Taproot single-key import separate. Emulator not physical acceptance.
