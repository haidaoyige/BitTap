[简体中文](RELEASE-NOTES.md) · [English](RELEASE-NOTES.en.md) · [文档目录 / Documentation](../../README.md)

# BitTap v0.6.0

Historical Android 9+, 0.6.0/code 23, original signer, private repository then.

## Changes

- Home fit screen, NF Cripple icons, titles/address alignment.
- Creation/unlock passwords in keyboard-aware bottom sheets. Recovery only View key; WIF/QR/copy/image/transfer help inside.
- New password 10 minimum; weak consent; legacy short password recovery.
- Android 9 WIF clipboard disabled; reject obscured clicks; Android 12 hides overlays/protection notice.
- Pending address-image consent; continue allowed, imagestyle unchanged.
- Camera 2 new page: button gray/disabled until 32 frames; matched final photo cleared after digest, return setup before password; four languages.
- BTA06–11: runtime updates/strict verification/independent CI/signed provenance/CLI limits/full licenses SBOM/private Issue reporting.

## Provenance/validation

Final commit independent Actions CI; signing local only. Compare all nonsignature APK entries/recovery ZIP, original cert and signed release-manifest. Exact commit/run/hashes in signed attachment. Code 21 units, Android 9/15 password/weak regression, Android 15 full flow/four languages; code 22 Camera 2 button 16 assertions. Final 0.6.0 build/device regression record delivered separately, not replace with history.

## Limits then

120 total/60 idle lock not implemented; private QR 60 close is not session expiry. Independent verify-only entry/accessibility semantics not implemented; verification briefly decrypts in memory. Length valid weak password can be offline guessed; card copyable, public note/address. Personal private main protection not enabled; CI/signing not substitute. Emulator not physical NFC/camera/external spending acceptance; not independent funds audit. [Inventory](../../security/REMEDIATION-STATUS.en.md).

Install BitTap-0.6.0.apk over same signer; Java 17 offline ZIP. No instrumentation APK or fixture funding.
