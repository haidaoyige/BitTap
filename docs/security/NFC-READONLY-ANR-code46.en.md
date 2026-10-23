[简体中文](NFC-READONLY-ANR-code46.md) · [English](NFC-READONLY-ANR-code46.en.md) · [文档目录 / Documentation](../README.md)

# Code 46: unresponsive UI when rewriting a read-only card

2026-10-22: Xiaomi 15/Android 16 user reports ANR scanning an already-read-only card with permanent-read-only option enabled. No device ANR stack; these confirmed code gaps are fixed without claiming reproduced/excluded OEM root cause.

## Fixes

1. Check discovery-cached writable/capacity before connect; refuse already-read-only regardless of option. No connect/write/overwrite/lock on refusal. Existing four-language error sheet.
2. Adapter acquisition/state/reader start-stop previously on main, so 15-second data-I/O deadline could not rescue blocked UI. New independent bounded control executor, max one worker/one queued latest intent, separate from crypto/card I/O. No explicit app adapter calls on main.
3. Reader start 5-second expiry abandons scan/shows timeout; stuck stop cannot block notice. Late start rechecks controller version/epoch/scanning; cancel/timeout arranges stop, never reenters.
4. Processing now retains visible cancel/close/back. Cancelled write reports unconfirmed; cancelled lock unknown. Neither proves rollback.
5. Failed/cancelled CHECK/WRITE discard candidate; late identity/epoch/session/deadline checks prevent card-success/backup credit.

Writable cards still recheck after connection, compare actual old contents before write and fresh full NDEF afterward. Cached writable only refuses, never proves success. Second tap checks writable again; reading existing read-only wallets remains allowed. No crypto/KDF/key/derivation/lock algorithm/mandatory backup changes. 0.6.2/code 46 original signer, not published then.

## Validation/limits

Units: read-only refusal without connect/read/write/close, becoming read-only between phases, capacity, fresh readback, changed original/mismatch, blocked start-stop, latest intent/stale/destruction/bounded queue. Device probes execute production card checks/completion with controlled Card/NFC service substitutes: option on/off, no overwrite after failure, candidate/busy cleanup, independent crypto, dismiss error while control blocked, actual 5-second timeout, processing cancel/stale write. No emulator RF.

External `BitTap-security-validation/code46-readonly-anr/validation.json` has final results/same APK hash. Android 16 Google image required separate license acceptance, not accepted/installed; 12/15 emulators are not 16/HyperOS acceptance.

Retest affected phone with unfunded wallet/same read-only card: option on→refuse, off→refuse, dismiss→file/new card works, existing wallet readable. Writable 215/216 write/read/lock needs physical acceptance. App cannot promise OS/OEM/framework lifecycle never blocks. Further ANR needs device stack; no unlimited replacement workers, new NFC fails fast until system/process recovery.

Basis: [AOSP Ndef](https://android.googlesource.com/platform/frameworks/base/+/28319c0/core/java/android/nfc/tech/Ndef.java) isWritable cached/noRF, getNdefMessage blocking; [Nfc Activity Manager](https://android.googlesource.com/platform/packages/modules/Nfc/+/refs/heads/main/framework/java/android/nfc/NfcActivityManager.java) reader/lifecycle service calls.
