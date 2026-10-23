[简体中文](RELEASE-NOTES.md) · [English](RELEASE-NOTES.en.md) · [文档目录 / Documentation](../../README.md)

# BitTap v0.6.2

Historical Android 9+, 0.6.2/code 31, same ID/cert/in-place updates, private repo, Apache 2.

## Changes

- New password visible ASCII U+0021–007E, 10–1024, uppercase/lowercase/digit/punctuation all required. Reject Chinese/emoji/full width/whitespace/invisibles; type/paste/submit consistent, no silent changes.
- Validation/strength use KDF NFC normalization; no format/parameter change/new legacy branch; recovery not creation gate.
-32 character system SecureRandom password, uniform 94 characters, whole-string retry if category missing, RNG fails top; full visual wrap, no inserted newline.
- Generated password confirmation empty, matching input+saved check box required. Regenerate asks, clears old confirmation/saved; manual edit exits generated mode. Four language loss/separate-storage warning.
- Sensitive copy/exact readback; Android 9 copy disabled. 120 sec owned clipboard cleanup with foreground retry/ownership/cancel/regenerate/complete; no input/check box persistence.
- Risk notice 3 items, remove entire card-copyable item; shield beside title, larger copy/spacing, height adaptive. Four languages, default tested view port no scroll for two actions; ask on each entry, full terms remain.

## Validation/provenance

99 units; lint 0 errors/9 existing warnings. API 35 release-signed creation 385 assertions; API 28 same source Debug 377 including copy ban/manual confirmation. Four language risk/per entry 328 at 1080×1920/420 dpi/default font. Preserve historical versus final candidate evidence. Final commit independent CI, local same signer, rebuild compare APK nonsignature/recovery ZIP, signed manifest authoritative for commit/run/actual validation/hashes. No credentials/test APK upload.

## Limits

Format not un guess ability; random password loss un recoverable. Clipboard create sun erasable external/system copies; app does not store/recover password. Very large font/tiny window protected scroll fallback prevents clipping; full terms unchanged. Retain 0.6.1 boundaries: verify-only not implemented, plaintext during decryption, root/privileged malware not covered, BIP38 special scalar issue, private plan main protection limits. No new physical NFC, cameraentropy certification, independent funds auditor real spending validation. Pass not funds guarantee.

InstallBitTap-0.6.2.apk; Java 17 offline-recovery ZIP; SHA/cert/SBOM/runtime/signed manifest attached. Random password was later removed; see [historical feature](../../security/random-password.en.md).
