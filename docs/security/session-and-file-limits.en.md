[简体中文](session-and-file-limits.md) · [English](session-and-file-limits.en.md) · [文档目录 / Documentation](../README.md)

# BTA-05 / BTA-14 local revisions

## BTA-05

Start monotonic elapsedRealtime when successful decryption reaches foreground session. Expire at 60 seconds without valid touch/key/button input or 120 seconds total, whichever earlier. Drawing/countdown/programmatic queries/prompts never extend absolute deadline. Track input consistently across page/private/copy/save protected sheets; rejected obscured gestures do not count. Synchronously recheck before view/copy/private-image export, so delayed main-thread callbacks cannot bypass.

Expiry closes secret/consent sheets, wipes held scalar arrays/text/QR/pending image and tries clearing owned private clipboard (retry on foreground if denied). Retain loaded ciphertext for password retry. At this historical step addresses revert pending and note has no verification tag. No countdown/banner on restored-key page. Code 26 orange/warm-white automatic-lock sheet distinguishes idle 60/total 120, dismissed only by Got it. Background/Home/rotation cleanup retained; secrets/timers never serialized.

Cleanup releases references/wipes owned mutable buffers, not forensic erasure of all JVM/services/recipient history. Saved images cannot be recalled. Pending private PNG/tasks share original session deadline; system picker does not grant a fresh three minutes.

## BTA-14

Byte I/O already background; gap was indefinitely blocked provider holding fileBusy.

- Separate crypto queue from file pool: max 2 daemon workers, no waiting queue. Fault work cannot grow indefinitely/occupy crypto queue.
- Open AssetFileDescriptor with CancellationSignal, register descriptor/stream, signal/attempt close on cancel.
- Signal/close can themselves block; bounded cleanup pool keeps main thread from waiting.
-15 seconds total for import, backup/image save and readback. Cancel/back/background/destruction immediately release fileBusy.
- Accept only current result and recheck monotonic deadline. Cancel/timeout/stale callbacks cannot mutate wallet/mark backup saved.
- Backup requires complete write, successful close/exact readback. Failure/cancel not success. Native Save failed stays until confirmation.
- Pending private PNG wiped immediately on cancel and again at end. Worker holds app Context/weak UI reference, not long-lived Activity.

Unresponsive providers may still occupy bounded workers. Other app work continues; both blocked means new file jobs fail fast, requiring healthy provider/app restart. Cancel cannot undo bytes already written; error asks user to check destination.

## Code 27 inline password-button loading

Reverses code 26 separate crypto-progress sheets. Original password sheet remains, submit disabled/gray with spinner and localized “Processing, please wait...”. Inputs/eye disabled; real Editable cleared immediately after copy to controlled pending buffer, fixed-length dots only. Keeping sheet does not prolong input retention.

Cancel/close/back revoke result authority, clean password and close. Failure restores action/empty inputs; success closes/navigates. Background/rotation cleanup; animation stops requesting frames on restore/removal. Auto-lock/address/files behavior/styles unchanged.

91 units, API35 four-language loading 112 and full creation 157 assertions passed; suites overlap. Code 27 separately signed original signer, code 26 not overwritten. Final code 27 reran 112, installed/delivered SHA identical. Evidence external `BitTap-security-validation/device-test-code27/`, earlier source `device-test-inline-loading/`.

## Code 26 UI (historical, superseded above)

App bottom sheets used native indeterminate progress for generate/encrypt and decrypt/verify, no fake percentage. Busy cancellation kept epoch invalidation/pending-password cleanup; existing callbacks alone commit. Automatic lock cleans then same-style nondismiss-on-timer sheet. Four languages; file failure native confirmation.

## Code 25 baseline

0.6.0/code 25 original signer, not debuggable, no INTERNET. Strict dependency verification: 67 core+18 app+6 recovery=91 units. Lint no errors, existing UnusedResources/SetTextI18n only.

| Scope | Passed | Boundary |
| --- | --- | --- |
| Deadlines/fault provider |52| Actual 60 sec/15 sec waits; 120 sec units/device boundary acceleration, not full real wait |
| Address-image consent |156| Four languages/types, save/cancel/expiry |
| Sensitive overlays |137| Touch refusal, export consent, background/stale cleanup |

Overlapping, not summed independent cases. Final/delivered/actually tested main APK exact SHA. External `BitTap-security-validation/device-test-code25/`. Public passwords/scalar-one/unfunded temporary wallets only.

## Code 26 validation

Original-signer 0.6.0/code 26: 91 units, lint 0 errors/2 existing warnings, API35 deadlines/new dialogs/fault provider 127 and sensitive 137. Four-language generation/decryption/auto lock screenshots. Actual 60 sec idle; 120 units/accelerated boundary. Busy cancel, system cancel, password zeroing, completion dismiss, expiry private-view/buffer clearing, manual dismiss checked. Delivered/tested bytes identical; cert/not debuggable/no Internet verified; no runner/fault provider/fixtures in main APK. External `device-test-code26/`. Suites overlap; local candidate, not GitHub Release then.
