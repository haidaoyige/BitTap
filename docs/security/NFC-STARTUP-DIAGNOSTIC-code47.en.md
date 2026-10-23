[简体中文](NFC-STARTUP-DIAGNOSTIC-code47.md) · [English](NFC-STARTUP-DIAGNOSTIC-code47.en.md) · [文档目录 / Documentation](../README.md)

# Code 47: NFC startup diagnosis and read-only status

2026-10-22. Xiaomi 15/Android 16 code 46, new card, read-only off: 5 seconds after Start write, still waiting discovery, NFC timeout. Reboot recovers, BitTap hangs NFC again. No USB available; user requested diagnostic APK.

## Boundary/codes

5 seconds is reader-start deadline, not 15-second post discovery I/O. No phone/service stacks; cannot identify SDK/OEM blocking point or claim emulator proves phone fixed. Fixed stage codes only; no timeout extension/KDF/format/hardware-lock change.

| Code | Meaning |
| --- | --- |
|NFC-Q| Queued / not yet in system call |
|NFC-A| Get adapter / initialize framework service |
|NFC-B| Query NFC enabled |
|NFC-C| enableReaderMode |
|NFC-D| disableReaderMode |
|NFC-R / NFC-I| Returned: reader active / stopped or idle |
|NFC-S| Controlled-test default, not normal physical backend |

Fixed in-memory strings; reading stage makes no NFC API call. Snapshot before cancellation, show existing localized error. No log files/network/stacks/UID/address/note/ciphertext/password. Late results invalid. Backend calls stop only after an actual start attempt, avoiding framework activity-state registration when never enabled. Reduces useless calls, not proven OEM root-cause fix.

## Read-only wording

Initial user clarification: option on without confirmed final read-only, later over writable. Existing separate irreversible consent/retap means no proof a successfully locked chip was bypassed. If selected but unfinished/unknown, completion title says wallet written/card not locked; only successful VERIFY_LOCK removes it. Keep confirm, skip-to-backup, retry. Never automatically execute unconfirmed lock or block saving backup. Authenticity/write protection still physical tests. Later code 48 clarification supersedes interpreting that particular report as only option selection.

## Test/device steps

External `BitTap-security-validation/code47-nfc-startup-investigation/validation.json`, original 15-second/cancel/stale/read-only-prerejection retained. Substitutes not Xiaomi RF acceptance. Install, reboot, start without card. If 5 sec timeout, send fixed code only, not password/key/QR/file. If start works, tap unfunded test card and observe checking. If system NFC unresponsive, stop repeating; device logs needed. [AOSP source](https://android.googlesource.com/platform/packages/modules/Nfc/+/refs/heads/main/framework/java/android/nfc/NfcActivityManager.java) is not proof Xiaomi internals identical.
