[简体中文](CREATION-PASSWORD-DIALOG.md) · [English](CREATION-PASSWORD-DIALOG.en.md) · [文档目录 / Documentation](../README.md)

# Creation password dialog and revised camera page v0.6.0

This change follows the approved warm-white/BTC-orange prototype. Home restores larger heading/card proportions; artwork takes remaining height and actions/local-processing notice stay below. The reading entry uses the same ic_nfc as Read NFC card.

Setup keeps only the public note and camera option. Standard mode opens the password dialog after Generate cold wallet. Enhanced mode first captures32 frames and a matched final photo, returns to setup, then explicitly opens the password dialog. Recapture is available. Failure, cancellation or backgrounding invalidates unfinished capture without silently falling back.

The revised camera page has a two-line title, live Camera2 preview/count, capture status and separate privacy explanation. No simulated prototype scene is rendered. The final photo is cleared/recycled immediately on result delivery; only mixing digests remain. Manual photo confirmation was removed.

At this stage the password dialog required at least10 characters, matching entries and a strength hint. A weak password could proceed after separate consent. Both fields disable saved Android state/autofill and request no personalized IME learning.

Cancellation, close, back, rotation or background clears/releases field and button references; stale buttons cannot queue generation. Submission rechecks page, camera, both passwords and note. Weak-password consent binds the input revision and capture object. This historical implementation cleared and closed the password dialog before generation, retaining tracked erasable password arrays in the worker.

Dialogs retain secure windows, obscured-touch protection and keyboard-adjusted scrolling. Passwords are processed in memory; clearing Java text/arrays cannot prove erasure of system, IME or historical memory copies.

This modifies UI/interaction, not mixing, address derivation, CG3 cryptography, NFC format or backup verification. All four languages were updated. Evidence was kept in workspace `BitTap-security-validation/device-test-code21/`; included in v0.6.0. Build22 additionally disabled the capture button until32 frames were ready.
