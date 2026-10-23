[简体中文](NFC-AND-PASSWORD-code45.md) · [English](NFC-AND-PASSWORD-code45.en.md) · [文档目录 / Documentation](../README.md)

# Code 45: NFC isolation and weak-password patterns

2026-10-22, following code 44 local review; version 0.6.2/code 45, no GitHub release/permission change then.

## NFC boundaries

Dedicated process-shared executor: at most one worker, no queue; no competition with crypto/local records. A monotonic 15-second total begins on tag discovery/actual operation, not while waiting for a tap; covers connect/check/write/readback/close. UI releases waiting on timer; result receipt rechecks deadline.

Register NDEF/Ultra light connections before connect. Cancel/background/Home/navigation/destruction invalidates task; independent bounded cleanup attempts close/interruption without blocking main. Ultra light also 3-second per-communication timeout. Workers hold tag/public UID/encrypted payload/candidate snapshot, not Activity/password/private session. Weak UI reference plus task identity/epoch/creation session/deadline authorize results.

Late success cannot import, count a card, establish record, confirm lock or satisfy backup. Failed writes discard candidates; retry starts with a new check. Interrupted lock/verification remains unknown. Existing pages/buttons/confirmations/backup retained; localized four-language errors in existing sheets.

A completely unresponsive driver may occupy the sole worker. New NFC jobs fail fast, never spawn unlimited replacements/queue, while crypto works. Removing card/restarting process may be needed. Cancellation cannot undo already transmitted writes/lock bits; released UI is not hardware rollback.

## Password heuristics

Still 10+ visible ASCII with all four categories; normalization/KDF/format unchanged. Expand common-word-plus-digits/punctuation, substitutions, repeated cores and dominant keyboard/numeric sequences. Temporary CharSequence views avoid an extra full plaintext String. Existing weak-password consent still permits continuation. Heuristics can miss/misclassify, do not measure entropy, store/query passwords online or give fixed cracking times. Offline copies bypass app retry limits.

## Validation/acceptance

NfcIo fault units and Android 12/15 probes: blocked connect, actual 15-second expiry, close-on-cancel, refuse new job under stuck driver, independent crypto, stale write rejection, no false lock success, background cancellation. Controlled transport substitutes, not RF acceptance. Four-language create/recover/weak-consent/key gate/auto lock/address/full flow regression. 116 units(85 core/25 Android/6 recovery), 62NFC assertions on each OS. Final APK NFC/password/full/Home re tests and stage-specific hashes in external `BitTap-security-validation/code45-nfc-password-hardening/validation.json`.

Physical acceptance with unfunded wallets/disposable cards must cover: both models normal check/write/fresh readback/other-device recovery, insufficient capacity/existing-content consent; removal/NFC disable during connect/write/readback without success and subsequent actual-content inspection; interrupted lock unknown then power-cycle/full NDEF/hardware/other-device overwrite check; files/crypto still work after NFC faults; camera denial/repeated frames/cancel/OEM differences without entropy claim; external four-address matching, real spending separately authorized.

Unclosed by this change: BTA-01 guessable valid passwords; BTA-04 brief plaintext/root/injection/external exports; BTA-07/08 final independent CI/protection/auditor SSH-access changes; BTA-11 mailbox receipt pending user/authorized external mail test; BTA-13 unmeasured camera entropy and physical NFC/OEM/external-spend acceptance.
