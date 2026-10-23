[简体中文](README.md) · [English](README.en.md) · [文档目录 / Documentation](../README.md)

# BitTap v0.7.0 release and validation

Current version **0.7.0 / code 50**, minimum **Android 12/API31**. Same application ID and original release certificate. Public repository, Apache-2.0. [Scope and hardware limits](v0.7.0/RELEASE-NOTES.en.md).

A clean final source commit must pass independent GitHub Actions CI before local `tools/build_release.py --ci-run-id <successful-run> --output <new-directory>` rebuild comparison and signing. Distribute main APK, Java 17 offline recovery ZIP, SBOM, runtime inventory, environment/dependency evidence, signed provenance and SHA256. No instrumentation APK, signing key or local credentials. Signed release manifest is authoritative for commit, CI and attachment hashes.

Older0.6.x and earlier records below are historical: their OS/private-repository/password/compatibility claims do not describe 0.7.0. See the candidate workflow below for release procedure.

## Historical local candidates

Code 27 replaces separate crypto progress sheets with a disabled gray loading button in the original password sheet; code 26 automatic-lock style retained. Original signer permits updates. Code 26 had unified App sheets for generation/encryption/decryption and both lock causes, kept deadlines/cancelled-result rejection/secure windows, no recovery countdown; file waiting/error unchanged.

Code 25 added BTA05 idle 60/absolute 120 lock and BTA14 cancellable 15-second file tasks with stale-result refusal. Save failure uses manually dismissed native confirmation. Code 24 added 10+ and uppercase/lowercase/digit/special, four-language live rules/button state and legacy recovery. Local candidates were not GitHub Releases or independently CI-compared final commits. [Password policy](../security/password-policy.en.md).

Release 0.6.0 integrated approved code 16–22; code 23 only version/docs. Final commit CI, local original signer and exact APK/recovery comparison before manifest/upload. [Release](https://github.com/haidaoyige/BitTap/releases/tag/v0.6.0) includes main APK, recovery, SBOM, runtime/environment/certificate evidence, signed manifest/hashes; no test APK/keys.

## Historical 0.5.0 candidates

- Code 22 disables capture until 32 frames; Android 15 Camera2 suite 16 assertions with actual capture/generation. Code 21 creation-password sheet/new camera/Home/four languages: [interaction](../ui/CREATION-PASSWORD-DIALOG.en.md). External evidence device-test-code 21/22 under BitTap-security-validation; historical, not 0.6 provenance.
- Code 20 removes excessive top/title/button gaps and two minimum-height spacers. Illustration/card/lock scale to safe remaining height. Full copy/languages/two 52 dp buttons retained. Default normal view port targets one screen; tiny windows/large fonts scroll. External device-test-code 20.
- Code 19 internal header 12→20 sp to match 20 dp back icon; full toolbar height/no font padding/equal side widths center it. Restored page header localized Private key restored, read page Read cold wallet. External device-test-code 19.
- Code 18 compact paired read/recovery layout, matching lock/title/note/address/two actions. Password in unlock sheet; Change card/backup secondary. View key contains WIF/QR/copy/image/transfer help. Four languages. External device-test-code 18; local candidate, not independent-CI release.
- Code 17 other-address chevron wraps to text width, outer container takes remaining gap; all shared address cards. Supported-card/principles links share indentation; NFC icon/explanation in same text control. External device-test-code 17; local only.
- Code 16 on 2026-10-17 includes implemented local fixes, original signer, does not replace historical Release or establish clean independent CI. External device-test-code 16; delivered in BitTap-deliverables. Dual entry/auto lock were still prototypes then.

[0.5.0 Release](https://github.com/haidaoyige/BitTap/releases/tag/v0.5.0): BitTap-v0.5.0.apk, recovery, SHA256SUMS. Original signer, [validation](v0.5.0/VALIDATION.en.md). No instrumentation/private signing key. Old hashes do not validate a newer download.

## Reproducible candidate workflow

1. Use pinned Temurin 17.0.18+8, Gradle 8.11.1, SDK/build-tools in tools/build-environment.json; run `python3 tools/check_environment.py`. Signing credentials stay local/outside repo, not Actions.
2. Commit/push; `.github/workflows/ci.yml` uses empty cache/strict verification for units, lint, unsigned APK, recovery, independent crypto/address, licenses/live OSV. Network failure fails the scan, not a clean result.
3. Clean workspace. Set four BITNFC_RELEASE_* variables for original identity, then:

```sh
python3 tools/build_release.py --ci-run-id <successful-push-or-workflow_dispatch-run> --output /local/new-candidate-directory
```

Optional BITTAP_BUILD_ROOT moves outputs outside source. BITTAP_GOOGLE_MIRROR=1 uses official Google mirror with strict verification; no unknown mirrors/bypass.

4. Verify CI commit, event, conclusion; rebuild/certificate check, compare every APK ZIP member except standard signature entries and exact recovery ZIP hashes. Check full licenses/runtime JAR hashes before preparing artifacts.
5. APK, recovery, SBOM, runtime/environment, signature evidence, SHA256SUMS and release-manifest bind source/CI/output hashes. Manifest independently signed with original APK identity. Verify against a certificate fingerprint obtained from a previously trusted APK/certificate, not merely included download:

```sh
java tools/ReleaseManifest.java verify release-manifest.json release-certificate.der release-manifest.json.sig <independently-trusted-certificate-SHA256>
```

After signature verification check every listed file hash. SHA256SUMS alone is not provenance authentication. Tool prepares local candidate only; never automatically tags/uploads/replaces attachments.

Provenance improves verifiability, not proof no bugs. Historical personal-private plan limited protection; public 0.7 obeys actual remote settings. CODEOWNERS routes review, does not itself enforce protection. [Hardening](../security/BACKEND-HARDENING.en.md).

## Historical 0.4.0 candidates

Code 8 `app/build/outputs/bittap/BitTap-0.4.0-code8-candidate.apk`, original signer, updates code 7. [Alignment/evidence](bittap-code8/README.en.md). Code 7 [scope](../BITTAP-IMPLEMENTATION.en.md), same path with code 7, 0.4.0, optional camera permission, no Internet, four languages/full offline terms.

Added device checks: four languages/system, per-entry consent/refusal; note before/after/file/multi card/invalid; camera permission/sampling/final/background; both 215/216 default unlocked, explicit irreversible consent, full readback/hardware status, other-device overwrite refusal, unknown interruption. Disposable blank test cards only. Explorer sends selected public address only after privacy disclosure.

Code 6 historical `app/build/outputs/bitnfc/BitNFC-0.4.0-release.apk`, io.coingift.android, 0.4.0/6, minimum 9/API28. Unified Show address/bottom sheet, centered address, SegWit/P2SH, PNG exports. Original independent signer updates code 4/5, not debuggable/no test/network. SHA256SUMS.txt/APK-SIGNATURE.txt are its identifiers.

A debug/other-machine signature may prevent update. Save encrypted backup/password before uninstall. Future releases must keep signer; losing key prevents signing updates.

## Phone test order

Unfunded new wallets only; never fund public fixtures.

1. Offline creation, password strength/confirmation, record four-addresses.
2. Writable sufficient NDEF card: check/write/readback; capacity/read-only/overwrite/wrong card/removal cases.
3. Mandatory file backup, Home, restart, recover card/file. No encrypted-text copy/paste. Decrypt/check all addresses/WIF.
4. Main/list Show address: full centered wrap, type names, pre/post verification labels, copy/save, independent device scan comparison.
5. Unlock sheet: cancel/wrong password/retry/keyboard; View key WIF/QR; close/background/Home clears sensitive display, background requires new decryption.
6. Private-image consent/cancel, local picker destination. Image is plaintext, not automatic gallery/encrypted backup. Picker locks key; rotation/original session expiry requires new unlock/save.
7. Deny clipboard/file permission: never success. Close/exact readback required; provider refusing readback means unconfirmed.
8. Second card same wallet; both cards/file same addresses/password.

Emulator NFC success uses public injected fixtures, not real-card proof. External import/Taproot spending acceptance separate. Release build is not funds-safety approval; [review](SECURITY-REVIEW.en.md).

## Rebuilding/device instrumentation

Old local-dev scripts outside repo are not required; new maintainers follow pinned tools/successful CI, never request/upload signing private key. Release instrumentation: `-PbitnfcReleaseTest :app:assembleReleaseAndroidTest`; same-signed separate test APK required for non-debuggable main, never distribute. Optional `-PbitnfcAudit` changes test runner only.
