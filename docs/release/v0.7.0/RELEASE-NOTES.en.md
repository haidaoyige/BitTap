[简体中文](RELEASE-NOTES.md) · [English](RELEASE-NOTES.en.md) · [文档目录 / Documentation](../../README.md)

# BitTap v0.7.0

Android 12 and later; version 0.7.0 / internal build 50. The application ID `io.coingift.android` and original release certificate remain unchanged, allowing updates over same-signed installations. The repository is now public and retains Apache License 2.0.

## Changes

- Incorporates changes since 0.6.2: Home card/lock/button proportions, compact supported-card hint, removal of random-password generation. Manual password entry/confirmation and strict ASCII rules remain.
- Minimum Android 12; removes Android 9–11 paths while retaining necessary checks for newer Android APIs.
- Local public-address verification records: established after full-password decryption and address derivation, or completed creation/write-readback/mandatory backup. An identical complete payload can later match without a password. Unknown or changed data cannot inherit trust. Records can be managed/deleted and store no password, private key or complete backup.
- Recovered keys are wrapped in temporary Android Keystore sessions and briefly unwrapped for viewing. Idle60-second, absolute120-second, background and Home cleanup remain. Accessibility-service gating also restricts legitimate services.
- Project-format creation/recovery uses scrypt N=32768/r=8/p=3. Earlier p=1 internal-test files/cards are explicitly rejected without a compatibility branch. BIP38 boundary scalars are fixed; that external format follows its own standard.
- NFC I/O and reader-service control use bounded background workers. Already-read-only cards are refused before connecting; timeout/cancellation/late results cannot report success. Fixed diagnostic codes contain no raw exception or wallet content.
- NTAG215/216 permanent locking uses chip-specific bits instead of generic Ndef.makeReadOnly. Reserved bytes on the dynamic-lock page are corrected and each step read back. Final confirmation still requires removal/re-tap, full contents, actual hardware bits and NDEF read-only state. Enabling the switch alone does not mean a card is locked.
- README, reporting channels and release notes updated; four-language/offline flows retained.

## Validation and provenance

The code49 functional baseline passed137 unit tests. Same-signed packages on Android12 and15 emulators each passed192 lock-diagnostic,62 NFC-isolation/deadline,46 complete-wallet and41 read-only/blocking/cancellation assertions. The final0.7.0 version bump requires a fresh build/regression; Release VALIDATION.json is authoritative. Historical APK results are not new-APK tests.

A clean final source commit must pass independent CI, followed by local rebuild and comparison of nonsignature APK contents and recovery ZIP, then provenance signed with the original certificate. Release artifacts exclude signing keys, passwords and instrumentation APKs. See signed release-manifest.json for commit, run and hashes.

## Important limits

- Xiaomi15/Android16 previously reported NFC-LOCK-L3-TIME followed by NFC-D. The targeted lock fix still needs post-fix acceptance on the affected phone/cards. Emulators and page-permission models are not RF tests. Start with unfunded test wallets and disposable cards.
- Code47 recorded unstable Android15 emulator timing for visible cancellation within one second. A single code49 passing round does not establish permanent resolution.
- Locking is irreversible and failure may leave partial locks. It does not prevent copying, card replacement, rollback or counterfeit chips. Only complete final verification reports permanent read-only success.
- Password-free records reduce repeated decryption; they do not establish card identity or balance. First verification and explicit exports still briefly handle plaintext. Root, injection, compromised systems and external plaintext copies remain risks.
- Camera capture is supplemental mixing input, without measured entropy or hardware-randomness certification. No independent funds-security certification or complete external-wallet spending acceptance is claimed.
- Old p=1 internal-test backups are incompatible. A successful app update does not establish their recoverability.

## Downloads

`BitTap-0.7.0.apk` is the main application; `BitTap-0.7.0-offline-recovery.zip` requires Java17. Attachments include certificate, signed manifest, SHA256, SBOM, dependency-audit and validation records. The later [October23 audit](../../security/audits/v0.7.0-2026-10-23/README.en.md) records subsequent verification scope.
