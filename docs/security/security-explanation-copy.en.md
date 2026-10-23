[简体中文](security-explanation-copy.md) · [English](security-explanation-copy.en.md) · [文档目录 / Documentation](../README.md)

# Review/translation of “How keys are generated and encrypted”

2026-10-18 source change after delivery of code 28; delivered code 28 APK did not contain this copy update.

## Findings/revisions

Retain accurate explanations of Android secure randomness, camera supplemental input, scrypt, AES-256-GCM, recovery self-check and NFC readback. Clarify:

-32 valid preview digests/final-image digest mixed by HMAC-SHA256 with system secret obtained before capture. Photos neither replace system randomness nor prove a fixed number of extra security bits.
- New passwords 10+ with uppercase/lowercase/digit/special; valid format can still be guessable.
- Address/note public and unauthenticated before successful decryption; GCM integrity is not confidentiality for these fields.
- Pending-address image warning does not itself verify.
- Session maximum 120/idle 60 seconds, earlier wins; background/Home clear.
- Dedicated key drawing does not expose WIF as ordinary accessibility text; explicit copy/export still produces plaintext.
- Plaintext cleared after generation before writing; creation state cleared after backup/Home. Owned-buffer/reference cleanup is not proof all system copies erased.
- Replace obsolete “real-camera acceptance pending in this version” with hardware/OS dependency, without equating workflow checks to entropy certification.

## Languages/UI

approved-copy.json securityContent provides zh/en/ja/ko: full titles/introduction/cleanup, 8 groups with 25 paragraphs, open-source-wallet comparison and compatibility/security limits. ApprovedCopy.security follows resolved app language; HelpPages.security uses same source for cleanup/comparison titles and removes Chinese-only notice. Nine accordions remain, first expanded, toggle and About return unchanged. No crypto/key/NFC-format/other-page-copy changes.

## Local implementation checked

WalletCrypto: RNG/rejection sampling, scrypt 32768/8/3, 32-byte derived key, 16-byte salt, 12-byte nonce, 128-bit tag, public-header AAD, rederived addresses/cleanup. CameraCapture/Entropy: valid image digests, 32 distinct previews/final photo, pre obtained secret HMAC, failure not enhancement success. NewPasswordPolicy/MainActivity/UnlockDeadline/PrivateKeyView: policy/lifecycles/60–120 deadlines/dedicated drawing/exit cleanup.

## Comparison sources

Reviewed public source for conceptual comparison, not identical code/security/formats or endorsement:

- [Bitcoin Core crypter](https://github.com/bitcoin/bitcoin/blob/master/src/wallet/crypter.cpp), [key](https://github.com/bitcoin/bitcoin/blob/master/src/key.cpp): key generation/AES-256-CBC, different from BitTap.
- [Electrum crypto](https://github.com/spesmilo/electrum/blob/master/electrum/crypto.py), [storage](https://github.com/spesmilo/electrum/blob/master/electrum/storage.py): AES-CBC/32-byte password derivation and ECIES wallet-file layer, not CG3.
- [SeedSigner tools](https://github.com/SeedSigner/seedsigner/blob/dev/src/seedsigner/views/tools_views.py): previews/final photo entropy flow differs from Android-system-secret HMAC mixing.

Licenses/disclaimers/other translations unchanged. API35 actual-page 271 assertions: four-language bodies, every paragraph/title/hint, accordions/About navigation; other approved copy matches code 28.91 units/release lint no errors, existing warnings. Foreign-language screenshots checked for wrap/layout; no APK redelivery in that step.
