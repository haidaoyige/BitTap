[简体中文](SECURITY-REVIEW-0.4.0.md) · [English](SECURITY-REVIEW-0.4.0.en.md) · [文档目录 / Documentation](README.md)

# BitNFC 0.4.0 security review

Historical pre-fix evidence; findings do not mean later source received a fresh comprehensive audit. 2026-10-15: **the user's four requirements cannot all be marked passed; this test APKshould not be a production funds-custody release.** Functional compatibility does not replace security review. This step added adversarial tests/test-runner selection/report, not production Java changes/new APK.

Debug APK SHA256 `2f242ab373f454b8950729147aa5e8ab2dbece508d84c1b1e5ebd0ed6aaf4355`. Source uncommitted codex/bitnfc-v040 workspace; HEAD alone not reproducible, retain source/tests together.

## Four user requirements

| Requirement | Finding / evidence |
| --- | --- |
| Every new key random | Implementation/current tests pass: WalletCrypto: 33, 94 platform SecureRandom 32-byte rejection `[1,n-1]`, no time/password seed/modulo/weak fallback; RNG error/stall stops. Android 15 provider Android Open SSL/algorithm SHA 1 PRNG. 12 creations no duplicate address/salt/nonce, only sample/not all-device entropy. Second card intentionally same wallet |
| Delete key/encrypted file after final backup | Literal requirement not met. Owned generation plaintext best-effort wiped after encrypt/recovery, self session ciphertext/public only. After file save ciphertext remains for more cards and saved Instance State; Home clears active refs, not all JVM/library/OS saved copies. User exported backup must remain |
| Key/address correspondence | Existing vectors/self-recovery/four authenticated addresses pass, independent Python/OpenSSL/parity/TapTweak/WIF checked. No actual external spending/all-wallet Taproot guarantee |
| No exposure before Copy | Partial: default WIF hidden, no auto clipboard/log/file, but plaintext scalar retained in heap, View/QR expose before Copy. Privileged UiAutomation reads shown WIF despite FLAG_SECURE; not permission less other app access. Root/debug/malicious service/keyboard environment not absolutely secret |

AndroidOpenSSL SHA1PRNG is platform algorithm name; Android implementation calls BoringSSL RAND_bytes, not app time/password SHA 1. No weak seed/fallback found. [SecureRandom](https://developer.android.com/reference/java/security/SecureRandom), [system implementation](https://android.googlesource.com/platform/prebuilts/fullsdk/sources/+/refs/heads/androidx-constraintlayout-release/android-35/com/android/org/conscrypt/OpenSSLRandom.java).

## Priority findings

### S01 high: short passwords enable offline guessing

WalletCrypto: 234, KDF: 30. Then 6 characters/non blank, 123456 allowed. Attacker guesses copied card/file outside app, retry limits irrelevant. Public weak fixture candidate recovered without UI, not AES break/high entropy crack/real wallet brute force. Recommend common/short PIN/sequence/repetition rejection, long random password/strength feedback, not 6-character funds standard. Evaluate KDF 32768/8/p=1→p=3 against[OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#scrypt), not wallet certification/device benchmark/entropy substitute. Historical recommendation retain old parameter recovery, not globally break old cards; later owner policy changed explicitly.

### S02 high: public-address replacement before decryption

MainActivity: 218, 255; inspect WalletCrypto: 144 syntax only, decrypt: 203 au th. Writable card addresses replaced with attacker-valid four-addresses while ciphertext retained; inspect displays/copies pending claims, correct password later GCM fails but prior incoming funds elsewhere. Four-address replacement test confirms exposure/decrypt refusal, no transactions. **Not GCM bypass.** Labels help but receiving needs operational gate. Recommend password verification or independent trusted fingerprint before trust; allow view without claim whole-card identity without external anchor. Same-card pub key/self signature cannot prevent whole card replacement. Read-only reduces rewrite, not clone/replacement; backup protects loss.

### S03 conditional high: debuggable delivery

Manifest debuggable true/run-as succeeds with authorized ADB, not ordinary app automatic debug authority. Production must not debuggable, maintainer-held release key, verifiable source/dep locks/APK hash. Obfuscation/screenshot ban not substitute. [Android](https://developer.android.com/privacy-and-security/risks/android-debuggable).

### S04 conditional high: accessibility WIF and absent overlay protection

MainActivity: 240/App Ui controls/Manifest. Explicit view, without copy, UiAutomation read full Text View despite secure flag. No malicious service on real phone; semantic exposure proof, not privilege escalation/ordinary memory access. Recommend system appropriate secret semantic protection, obscured touch filter for view/QR/copy/write confirm, hide overlays where available, explain legitimate-screen reader tradeoff. Not root/malicious OS/privileged abuse guarantee. [Activities](https://developer.android.com/security/fraud-prevention/activities), [tap jacking](https://developer.android.com/privacy-and-security/risks/tapjacking).

### S05 medium: hiding is not locking

MainActivity: 62, 229/WalletCrypto: 65.30-second callback only dots; Unlocked still 32 bytes, foreground holder indefinite. Background/navigation closes/wipes owned array, tested. Recommend actual session/idle/focus deadline/minimum plaintext lifetime. BigInteger/strings/normalization/provider/ZXing/runtime copies not fully controllable; null/array wipe not hardware isolation or“never residual.”

### S06 medium: backup retains ciphertext

MainActivity: 87, 126, 337. Actual close/readback leaves session and Bundle copy. Home clears active references not earlier Bundle; test not claim Android necessarily restores old wallet or plain keys in saved state. Clarify save/more cards/end-clean; clear all owned state/candidate/callback/UI, check serialization, retain exported file. Remove saved-state ciphertext if memory-only required, while addressing process death loss of unbacked wallet. Reliability/cleanup cannot both be absolute promises.

### S07 medium: Android 9 clipboard and untrusted IME/provider

Min 28: background other apps can read clipboard. Sensitive mark not vault; 120 cleanup may not run after kill/background, external copies irretrievable. Raise OS floor/limit old plaintext copy/shorten window/explicit clear. [Clipboard](https://developer.android.com/privacy-and-security/risks/secure-clipboard-handling). No autofill/learning reduces normal copies, not malicious IME. App no Internet does not stop IME/services/OS/cloud provider networking. Backups are ciphertext; trust device/keyboard/destination.

### S08 before release: known runtime advisories

16 recovery dependencies+ZXing=17 Maven coordinates queried OSV, 12 unique advisories, raw security/ records. Not build tools/OS/firmware/full library audit.

| Component | Hits | Current reach ability assessment |
| --- | --- | --- |
|bitcoinj 0.16.5|CVE-2026-44714 P2PKH/P2WPKH Script Execution bypass| Tool uses keys/addresses/import, not transaction-script validation; no direct derivation path found, still upgrade/retest |
|BC1.83 jdk 15 to 18|4: GOSTCTR, Name Constraints, LDAP, lazy ASN 1| Uses scrypt/secp256k1, no corresponding inputs; counts not stolen keys |
|Guava 31.0.1-android|2 temporary dir/disclosure| Relevant API unused |
|protobuf-java lite 3.18.0|3 resource exhaustion| Fixed binary backup, not protobuf parse |
|OkHttp 3.14.9/Okio 1.17.2|2 certificate/signed conversion| No network permission/calls, input path not found |

Source-path assessment not complete automated reach ability proof. Upgrade/remove unused transitives, update trusted checksums, rerun RNG/BIP38/addresses/GCM/legacy/Android, never skip verification. [bitcoinj advisory](https://github.com/bitcoinj/bitcoinj/security/advisories/GHSA-hfcf-v2f8-x9pc), [BC](https://www.bouncycastle.org/vulnerability-advisory.html), security/dependency-advisories.json.

## Validated protections

CSPRNG/rejection/no fallback/scalar range/fresh salt-nonce; GCM au th versions/network/KDF/all addresses, bad password/tamper no key; strict bounded types/encoding/truncation/4096 deterministic mutations no unexpected parse exceptions; self-recovery/four-address re compute/CG1/BIP38 vectors; NFC writable/capacity/overwrite/same UID+content/fresh NDEF(UID not anti clone); file close/reopen/exact readback(not cloud/hardware durability proof); no automatic plaintext file/log/clipboard, system backup/migration excluded, no Internet; default mask/secure window/background owned scalar wipe.

## Evidence/scope

45 core JUnit(original 39+6), SecurityReviewTest.java/security/core-audit-tests.xml. 15 Android 15 security reproduction assertions, device-audit explicitly observed weaknesses unfixed: pass means reproduced, not secure. Independent Python old/new/four scalars/four-addresses/Tap Tweak pass. Public/unfunded only, no real wallet reads/funds/brute force/malware deployment. No physical NFC attacks, real malicious IME/services, sidechannels, heap forensics, all ROMs, release-reproduction/external spend. Implementer self review, not independent crypto/mobile audit. No found direct high entropy normal-card recovery on trusted updated system, not proof none exists.

```sh
./gradlew :core:test
./gradlew -PbitnfcAudit :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w io.coingift.android.test/io.coingift.android.SecurityReviewRunner
```

Prioritize S01/02/03/04 then 05/06/07, dependencies and real recovery. Functional test completion not funds approval.

## Same-day user-approved adjustments

S01 advisory strength only, no weak block, then 6 minimum/confirmation; offline guessing remains. S02 proposed view/copy warning/optional verify-or-explicit continue, not implemented pending interaction. S05/06 Home cleanup without foreground timeout then: stop wallet serialization, memory-only configuration; Home close/wipe/input/QR/ciphertext/candidate/callback/owned clipboard; exported files remain, system/JVM history not forensic, force kill loses unbacked progress. S04 retains platform protections, no claim controlled system/privileged resistance. Comprehensive audit after release requested; only targeted regression then, no inheritance of old report as new security approval.
