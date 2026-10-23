[简体中文](PASSWORD-MINIMUM-10.md) · [English](PASSWORD-MINIMUM-10.en.md) · [文档目录 / Documentation](../README.md)

# New-wallet passwords: at least 10 characters

Historical change from six to ten characters, allowing longer values, retaining the then-existing 1024 UTF-16-unit maximum and all-whitespace rejection. Creation hints/errors updated in Chinese, English, Japanese and Korean.

- Core `WalletCrypto.requireNewPassword` runs in both generation entry points before randomness/key generation. UI submission and weak-password continuation both call it; consent cannot bypass fewer than ten characters.
- Guessable but length-valid passwords retain strength assessment/risk confirmation. Ten characters are not a security guarantee.
- Decryption/BIP38 kept existing password handling and could recover short-password older wallets. Unlock copy no longer reused creation minimum hints.
- No KDF, normalization, ciphertext or stored-card/backup changes in this step.

Validation: both generation paths reject lengths 0–9 without invoking RNG; actual generation/recovery at ten; longer values and 1024/1025 boundaries; four-language hints/nine-character refusal; complete ten-character weak-password consent; actual Android recovery of a six-character legacy fixture.

Use the [weak-password](WEAK-PASSWORD-CONFIRMATION.en.md) `weakPasswordOnly` test entry. Build/unit/device/lint evidence: [validation.json](password-minimum-10/validation.json). Earlier warning/overlay reports remain historical rather than being relabeled as these results. Later strict ASCII/profile rules supersede this historical policy; see [current policy](password-policy.en.md).
