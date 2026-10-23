[简体中文](README.md) · [English](README.en.md) · [文档目录 / Documentation](docs/README.md)

# BitTap｜Keep your BTC with an NFC card

[简体中文](README.md) · **English**

BitTap is an open-source Android NFC cold-wallet tool. It generates a BTC private key offline on your phone, encrypts it with your password, and saves it to an NFC card and a separate encrypted backup file.

For everyday receiving, tap the card to view a receiving address. To move BTC, enter your password to recover the private key and import it into a compatible wallet to send the transaction. No account registration or handover of your private key to a third party is required.

## Why store a wallet on NFC?

**Low cost and easy copies**

Use inexpensive NTAG215/216 NFC cards to store the same wallet on multiple cards. Keep them separately to reduce reliance on a single storage medium.

**No charging and tap-to-read access**

Passive cards have no battery, offer a service life of up to 10 years, and resist moisture and insect damage. Read them with a compatible NFC-enabled Android phone, without a USB connector or dedicated card reader.

**Ordinary appearance for discreet storage**

Customize the card with stickers or printing to resemble an ordinary business card. Store it alongside everyday cards to make it less conspicuous.

**Keep the card and password separately**

The card stores an encrypted private key; the password is not written to it. Compared with a directly recorded plaintext key, someone obtaining the card must still crack the password to recover the key, adding security redundancy.

**Let the card handle storage only**

The phone generates and encrypts the key. The NFC card neither generates wallet private keys nor signs transactions, reducing reliance on complex wallet logic executed on the card.

The value of NFC as cold-wallet storage lies in its ordinary appearance, support for encrypted data, and low-cost backups. It cannot replace the disaster resistance of steel, nor does it mean NFC is safer than other storage media. It simply gives you another choice for BTC self-custody.

## What can the current version do?

- Generate BTC private keys offline, optionally mixing camera data as supplementary randomness input.
- Encrypt wallets with a password, read cards back after writing, and require an encrypted file backup.
- Derive SegWit, Legacy, P2SH and Taproot receiving addresses from one private key.
- Establish local verification records so the same complete data can later verify public addresses without a password or another private-key decryption.
- Display receiving QR codes, copy addresses and save receiving images; recover WIF private keys and their QR codes when needed.
- Save one wallet to multiple cards and optionally make compatible cards permanently read-only.
- Support Chinese, English, Japanese, Korean and the system language.

Current release: **0.7.0 / build 50**, for **Android 12 or later and Bitcoin mainnet**. BitTap does not hold funds, sign or broadcast transactions, or provide an internal balance query.

Lost passwords cannot be reset. Keep passwords separately from cards and backups, and test creation and recovery with an unfunded wallet first. This remains an experimental release. Open source, offline operation and testing do not guarantee funds safety.

## Visual usage guides

Follow the numbered screens and arrows: create and back up → read and receive → recover a private key.

[中文长图](docs/user-guide/BitTap-Long-Guide-ZH.png) · [English visual guide](docs/user-guide/BitTap-Long-Guide-EN.png) · [View both guides](docs/user-guide/README.md)

## Downloads and documentation

- [APK and offline recovery tool](https://github.com/haidaoyige/BitTap/releases/tag/v0.7.0)
- [Complete Chinese and English documentation index](docs/README.md)
- [Encrypted format](docs/FORMAT.md) · [Build and release](docs/release/README.en.md)
- [Local address verification](docs/security/local-address-verification.en.md) · [Security reporting](SECURITY.md)
- [October 23 2026 review and physical-card test status](docs/security/audits/v0.7.0-2026-10-23/README.en.md)

This release accepts only the scrypt N=32768/r=8/p=3 project profile. Earlier internal p=1 backups are incompatible; BIP38 uses its standard parameters. Overall physical-card acceptance remains incomplete and permanent-lock acceptance was not performed. Consult the latest audit records for details.

The project uses [Apache License 2.0](LICENSE); third-party components retain their own licenses. Report vulnerabilities privately. Never put real wallet secrets or undisclosed vulnerabilities in public Issues.
