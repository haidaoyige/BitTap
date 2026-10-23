[简体中文](qr-dialog-proposal.md) · [English](qr-dialog-proposal.en.md) · [文档目录 / Documentation](../README.md)

# QR interaction changes approved and implemented

Design approved October15 2026; final regression October16. The user selected the bottom-sheet layout and a centered full address below the QR. Native/Nested SegWit display labels became SegWit/P2SH. Address construction, CG2 fields and existing-backup compatibility were unchanged. Included in versionCode6.

## Source changes

- Main addresses wrap instead of being restricted to one line or automatically shrunk.
- Reading offers only NFC and encrypted-file import. Encrypted-text copy/paste UI and routes were removed; NDEF Text record compatibility remains.
- Address and WIF copy use a unique label and read back the exact text. Write/read exceptions, empty results, stale contents or multiple items never report success; the user is asked to check clipboard permissions and retry.
- Unconfirmed private-key copy attempts still retain app ownership labels and cleanup deadlines. Returning home clears only this app’s clipboard content, not another app’s content.

## Approved design

The preview offered bottom sheet and centered dialog. The bottom sheet continues the NFC sheet style.

- Pages and other-address lists use Show address with a scan-frame icon; duplicate receiving-QR entries were removed.
- Receiving sheets show type, BTC mainnet, QR, full wrapped address and verification state, with Copy address and Save image. Only successful re-derivation displays verification against the private key.
- Image export covers receiving and private-key QR. Exporting a private-key image first explains that it contains plaintext key material and can authorize spending.
- Public scalar-one addresses/WIF are used in prototypes and must never receive funds. Copy/save previews only show feedback, not actual clipboard/file changes.

The entries, dialogs, file writes and result feedback are implemented. ACTION_CREATE_DOCUMENT uses image/png without a new storage permission. Files are closed, reopened and compared byte-for-byte; no automatic gallery/cache write occurs. A dedicated in-memory private PNG is generated only after consent and cleared on cancellation, failure, completion, timeout, Activity destruction or home.

At this historical stage the PNG could wait up to three minutes while the screen QR and decrypted key were cleared for the picker. It was neither savedInstanceState nor RetainedFlow data. Rotation/process reclaim required a new export. The user retains exported images; the app cannot erase provider/cloud copies.

## Validation at this stage

- Release build and lint passed; lint reported No issues found.
- API35 ARM64 SmokeRunner passed162 assertions using public fixtures: wrapping, import, removed text entries, denied/stale clipboard, decryption/QR, backup, lifecycle, multiple cards, PNG save/cancel/failure/timeout, private-export consent and exact image decoding.
- Denied clipboard behavior was injected through read/write ports, including silent denial and SecurityException; this did not verify an OEM permission system.
- Import created a real file and fed its result through the ordinary path. Image tests intercepted picker launch, supplied a real file URI, and simulated Activity leave/return; they did not automate the system picker UI.
- Physical NFC and OEM permissions still needed real-device checks. Functional tests do not replace an audit.
- Both prototype flows and private-save consent were clicked; long Taproot addresses did not overflow narrow sheets.

Build5 artifacts were archived locally. Build6 received a separate release and source snapshot.
