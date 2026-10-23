[简体中文](CHANGELOG.md) · [English](CHANGELOG.en.md) · [文档目录 / Documentation](README.md)

# 0.7.0

Code 50; Android 12 minimum; repository public, Apache-2.0 retained. Integrates local password-free address-verification records, Keystore private-key sessions, accessibility gating, scrypt p=3 / BIP38 boundary fixes, bounded background NFC processing and NTAG215/216 lock-bit fixes. Removes random-password generation and adjusts Home/creation layouts. Old p=1 test backups are incompatible; physical NFC acceptance remains outstanding. [Release notes](release/v0.7.0/RELEASE-NOTES.en.md).

# 0.5.0

V0.5.0, code 15; standalone private BitTap repository at that historical time, original app ID/signature retained for updates.

- Approved branding/prototype, four languages, risk consent on every entry, notes, camera mixing and optional permanent NFC read-only.
- Full addresses, address switching, multilingual address/private-key QR image export with fixed English footer.
- Private-key copy confirmation and clipboard verification; recovery actions/alignment and keyboard avoidance.
- Write/readback, mandatory encrypted backup, same-wallet additional cards and session cleanup retained.

A release number is not independent security or physical NFC approval; see validation/history.

# 0.4.0

**Code 14:** Private QR reuses address-display icon; BTC transfer help aligns right with address action; orange hide-key/return-home button. Android 11+ system-bar/IME insets keep focused inputs visible after focus, keyboard/window changes. Older Android uses adjustResize.

**Code 13:** Remove inline Copy WIF from recovery; retain copy inside private QR. Four-language consent precedes copying; cancellation/close writes nothing. Background/Home/QR close revoke consent. Report success only for this consent and matching clipboard readback; failure keeps dialog/error.

**Code 12:** Approved risk-first WIF PNG: logo, plaintext exposure notice, WIF QR/text, move all BTC and stop receiving at old addresses. Four app languages; fixed `YOUR KEYS. YOUR BITCOIN.` footer. Explicit consent, system file picker and in-memory export cleanup retained. Remove “this is not an encrypted backup” copy.

**Code 11:** Address-image footer fixed English; title, network and address label still localized.

**Code 10:** Warm-white address PNG with logo, QR, full wrapped address/type. Follows Chinese/English/Japanese/Korean/system language. Explicit system-picker save only; QR encodes only public address.

**Code 9:** Home button icons/text vertically centered; both English card lines one size larger.

**Code 8:** Restore approved language capsule/selection, About, nine security accordions and About return path, creation groups/note counter, dedicated photo confirmation, writing/decryption and help sheets. Fix header and switch-thumb clipping. Key generation, format, derivation and mandatory backup unchanged. Photo preview memory-only, cleared after use/exit.

**Code 7:** Unified BitTap horizontal-card branding; four languages/system; per-entry disclaimer/full offline terms; public authenticated CG3 note; optional Camera2 mixing; card help; default-off NTAG215/216 permanent read-only with second consent/separate outcomes; updated WIF/security help; external public-address explorer after privacy consent. Recovery CLI/Python gain CG3 while then retaining older formats. Real camera/NFC lock acceptance required.

**Code 6:** Unified Show address; NFC-style QR bottom sheet, centered/wrapped full address, names SegWit/P2SH. Address/private QR PNG via system picker; private-image plaintext consent; successful close and exact readback required for save success. Temporary PNG memory-only, cleared on cancel/timeout/lifecycle. Remove encrypted-text copy/paste, retain NFC/file import. Clipboard unique label plus readback; denial/unverifiable results no longer report success.

Renamed BitNFC, approved orange step-based create/read flow, original Bitcoin SVG/NFC arcs. Other addresses unfold inside address card; remove compressed-public-key/ciphertext details; recovery help becomes three WIF transfer points.

**Code 5:** Restore NFC bottom sheet for checking/reading/writing: card illustration/rings and real callback status. Waiting cancellable, processing not cancellable, background closes. Capacity, same-card, overwrite consent/readback and original signature retained.

Added receiving QR beside Copy address across creation, completion, pre/post decrypt and four-address lists. Full mainnet address/type; “checked against private key” only after verification, otherwise pending decryption. Validate network/checksum; clear bitmap on close/background.

CG2 publicly stores/authenticates four-addresses, each independently rederived after recovery. Then retains CG1/BIP38 and updates CLI. NFC checks capacity/writable/original contents/same UID and fresh readback.

Mandatory file backup completes only after close and exact readback. Cancel/failure/text copy cannot skip it. Additional cards receive identical ciphertext, addresses/password. Strength was advisory at this historical stage. Ciphertext/public workflow state survives configuration only in memory, never page-saved state. Background clears passwords/plaintext; Home clears session, inputs, QR, candidate card and still-app-owned clipboard.

# 0.3.0

Six requested changes: four mainnet addresses per independent key with explanation; password eye toggles; move preparation heading; per-address copy/status; encrypted-backup help; repeat-creation save confirmation/full reset.

CG1/NFC unchanged; recovery tools output four-addresses. Taproot uses BIP341 key-only tweak/Bech32m with explicit recovery caveat; no generic WIF/Taproot guarantee. No networking or sending.

# 0.2.0

Eight requests: mainnet-only, system haptics for all buttons, primary-success dialogs, key copy/QR, six-character historical minimum, write-action priority and tap-card sheet.

Additional improvements: password visibility, cancellable crypto progress, NFC settings link, repeat-click protection, overwrite confirmation, cancellation protection after write discovery, hide key after 30 seconds, close QR after 60 seconds, sensitive clipboard marking and best-effort cleanup.
