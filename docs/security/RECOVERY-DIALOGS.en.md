[简体中文](RECOVERY-DIALOGS.md) · [English](RECOVERY-DIALOGS.en.md) · [文档目录 / Documentation](../README.md)

# Build 18 read/recovery pages

Historical implementation of the approved compact paired layout and two bottom sheets; no cryptographic format, derivation, password-strength or export-consent wording changes.

- No password field on read page; Unlock cold wallet opens it. Change card/backup is a secondary NFC/file-source button; cancellation retains loaded data.
- Recovery has View private key and Hide key/return Home, without inline WIF, visibility switch or transfer-help entry.
- View key shows a genuine scannable QR with full WIF beneath. Copy retains consent, write/readback and OS limits; save retains plaintext-image consent. Transfer help is inside this sheet.
- Note is public only. Main/other addresses individually show pending/verified. Verified means this decryption's derived address matches stored data, not balance or source identity.
- Closing password sheet clears input and removes corresponding view/click/edit references from AppUi lists. Empty passwords do not decrypt; wrong password reopens an empty retry. Old detached buttons cannot decrypt.
- Legacy short passwords could recover then; creation's ten-character minimum was not applied to recovery.
- Closing key sheet or opening transfer help overwrites WIF TextView, clears active references and erases/recycles QR. Background/Home retain key-buffer/session cleanup.
- Secure windows and obscured-touch protection remain. Private text excluded from view saving/autofill; password input uses secure/no-learning flags and IME avoidance.

This reduces view exposure/lifetime. Plaintext WIF still exists in-process while viewing; privileged malware/compromised OS may read it. No claim of complete accessibility protection or JVM forensic erasure. At this historical step, foreground 120-second/idle 60 locking remained a separate unimplemented proposal; old QR60-second close stayed.

Public scalar-one fixture checks cover four languages, wrong/empty passwords, keyboard, cancel/reopen, QR/WIF equality, help navigation, close/background/Home. Release-signed device evidence is outside the repository at `BitTap-security-validation/device-test-code18/`. Emulator checks are not physical NFC or vulnerability-free proof.
