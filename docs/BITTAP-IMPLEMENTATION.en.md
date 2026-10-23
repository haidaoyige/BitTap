[简体中文](BITTAP-IMPLEMENTATION.md) · [English](BITTAP-IMPLEMENTATION.en.md) · [文档目录 / Documentation](README.md)

# BitTap prototype implementation and subsequent alignment

Build 8 corrected build 7 layout/navigation differences; see the [alignment report](UI-PROTOTYPE-ALIGNMENT.en.md). The following preserves build 7 scope and historical validation. It describes a local candidate, not the current release: version 0.4.0, code 6 → 7, unchanged application ID and release signature, not yet published at that time.

## Changes

1. **Branding:** BitTap, Bitcoin orange, balanced horizontal card; larger original Bitcoin glyph and NFC mark, retaining `BITCOIN · OFFLINE` and adding `YOUR KEYS. YOUR BITCOIN.` beneath it. Rings sit behind the card.
2. **Languages:** Chinese, English, Japanese, Korean and system language on Home. Only language preference persists, never wallet state. Complete disclaimers are offline in all four languages; at this historical stage the approved long security article remained Chinese, with an explicit notice in other languages.
3. **Risk consent every time:** Both Home entry buttons reopen the disclaimer. Declining stays on Home; returning from full terms still requires consent.
4. **Wallet note:** Entered during creation and publicly stored in NFC/file backups; up to 16 Unicode code points / 64 bytes, letters, numbers and allowed ordinary punctuation. Shown unverified before decryption and authenticated by GCM afterward. It is neither a password, key-generation source nor filename. Older formats show no note.
5. **Optional camera mixing:** Camera2 collects 32 distinct preview-frame digests and the final photograph digest locally. HMAC-SHA256 mixes these with system secure randomness obtained before capture; rejection sampling produces a valid scalar. System randomness remains the foundation. No photographs are saved and no external camera is used. Permission denial, failure, cancellation or backgrounding clears the operation, with no silent fallback to standard mode.
6. **Help and balance links:** Supported-card explanation, full security principles, WIF and OKX / Electrum / BlueWallet import help. Address QR offers external explorer links after privacy consent explaining disclosure of the address/network information. Private-key QR has no explorer link.
7. **Permanent read-only:** Off by default. Opt-in checks NTAG215/216 support. After matching write/readback, a separate irreversible-lock confirmation and another tap verify the full NDEF and hardware lock bits. Only then report success; unlocked and unknown have separate states. This is neither copy protection nor anti-theft hardware.
8. **Compatibility and cleanup:** New CG3 authenticates the note and four-addresses. At this stage CG2/CG1/compressed BIP38 remained compatible; Python and Java offline recovery gained CG3. Mandatory file save/readback, same-wallet multiple cards, plaintext cleanup in background and full session cleanup on Home remained.

## What deletion means

After generation, encryption and recovery verification, close the plaintext holder and clear owned byte buffers. Ciphertext and public addresses stay in the active memory session for writing, mandatory backup and additional cards; ciphertext is not deleted immediately after the first card. Completing backup and returning Home clears the session, inputs, QR, candidate-card data and clipboard still owned by this app. Exported backups and plaintext private-key images remain under the user's control.

Java, cryptographic providers, camera services, keyboards or the OS may retain copies outside application control. Forensic erasure and safety on a compromised phone cannot be promised.

## Historical verification and outstanding acceptance

Candidate: `app/build/outputs/bittap/BitTap-0.4.0-code7-candidate.apk`. Passed 76 unit tests, 210 emulator interaction assertions and 7 Camera2 emulator assertions; release lint clean. Same release signer supports in-place updates; NFC/CAMERA only, no INTERNET. Hashes/results: [build 7 record](release/bittap-code7/README.en.md).

Checks cover CG3 note authentication, legacy recovery, address derivation, fixed camera-mixing vectors, RNG fail-closed behavior, lock-bit interpretation, four languages, entry disclaimers, file backup, QR and lifecycle cleanup. Emulator test APKs and temporary screenshots were removed; candidate left on Home.

**Physical NFC and actual-phone camera acceptance remained required:** for NTAG215 and 216, capacity, overwrite, readback, default unlocked, explicit permanent locking with cross-device overwrite refusal and unknown state on interruption; for camera, denied permissions, valid frames/final-photo matching, background interruption and recapture. Simulated card states/digests cannot replace hardware tests.

This was implementation/regression work, not an independent funds-security audit. Earlier review risks remain open unless independently resolved.
