[简体中文](BTA-04-code40.md) · [English](BTA-04-code40.en.md) · [文档目录 / Documentation](../README.md)

# BTA-04: wrapped recovery sessions and sensitive-action gating — code 40

2026-10-20, based on code 39. No card/backup format, derivation, password KDF or normal layout change. Code 28 drawing protection remains, but its permissive accessibility policy is superseded.

## Session key handling

Password decryption and independent address derivation/comparison run in the background. Immediately wrap the 32-byte key with a nonexportable Android Keystore AES-256-GCM key, then close/wipe the original WalletCrypto. Unlocked scalar.

Activity retains only public addresses/note, 12-byte nonce, 48-byte authenticated ciphertext and random alias, not the original decrypted object/password/long-lived WIF. AAD binds addresses/note; tampered wrapping invalidates the session.

Only View private key briefly unwraps in the background, clears the owned raw scalar and supplies short-lived characters to protected drawing. Closing clears text/QR/layout. Copy/image export uses the current view, without another retained unwrapped session.

Home/background/idle 60/total 120 closes the session, wipes owned ciphertext/nonce and asynchronously deletes the alias without blocking UI. Stale aliases from abnormal exit/delete failure are cleaned before the first new session next process. Ciphertext never enters disk, Bundle, preferences, logs or cache. A leftover alias alone cannot recover lost session ciphertext.

Keystore unavailable/authentication failure stops; no fallback to an app-owned plaintext/software wrapping key. Hardware support varies; TEE/StrongBox not promised on every phone.

## Accessibility-service gate

Any configured/enabled accessibility service blocks creation password, recovery password, key view/copy/private-image export. No package allowlist or production test bypass, including legitimate TalkBack: an explicitly accepted accessibility tradeoff. Public address reading/display remains. Disabling a service does not replay cancelled actions/unlock automatically; user must click/enter again.

Observe settings and service state; recheck each sensitive entry and a sync commit. Enabling during processing invalidates callbacks/consent, clears passwords/key/QR/pending private image and closes session. File tasks recheck service state/original deadline before/after provider I/O; gate change cancels owned data/task but cannot recall provider bytes or force uncooperative calls to terminate. Existing BitTap sheet, four languages; normal flow adds no repeated password step.

## Validation

Adds core wrapping/close tests, actual Android Keystore nonexportability/unwrap/tamper/cleanup, and a separate-process accessibility positive control/gate. Existing password/address/copy/image/deadline/fault-provider/four-language regression retained; final delivery records authoritative. Public unfunded scalar-one vectors only; test services/providers/keys excluded from main APK. Emulators do not establish all OEM/physical NFC/external spending acceptance or original auditor sign-off.

## Remaining boundaries

Decryption, derivation and explicit view/export still briefly process plaintext. Java String/BigInteger/providers/keyboards/rendering may make un wipe able copies. Root/injection/compromised OS/privileged components may intercept or invoke Keystore through a controlled app. No “never in memory” or complete physical-erasure claim. Explicit WIF/image exports remain plaintext outside app control; auto-lock cannot delete recipient/provider/clipboard-history/synced copies.

BTA-02 unchanged in this step. Historical BTA-08 private-plan protection limits/retained auditor write access and BTA-11 mailbox delivery cannot be closed by it.

References: [Keystore](https://developer.android.com/privacy-and-security/keystore), [AccessibilityManager](https://developer.android.com/reference/android/view/accessibility/AccessibilityManager).
