[简体中文](VALIDATION.md) · [English](VALIDATION.en.md) · [文档目录 / Documentation](README.md)

# 0.4.0 validation history

[Security review](SECURITY-REVIEW-0.4.0.en.md) found outstanding issues. These are functional records, not funds-safety approval. Initial date 2026-10-15, Asia/Shanghai, JDK17/SDK35/Android 15 ARM64 emulator.

## Code 8 candidate

2026-10-16 [prototype alignment](UI-PROTOTYPE-ALIGNMENT.en.md). Final signed candidate 76 units, 228 interaction assertions, 9 camera end-to-end, release lint clean; [actual screenshots/results/hash](release/bittap-code8/README.en.md). Language capsule/selection stays open, About/nine accordions/return, note/mode/photo confirmation, write/read/QR/help restored; header/switch clipping fixed. Memory-only photo cleared on use; repeated frame refuses, then recapture passes, not measured entropy. Crypto/derivation/lock algorithm unchanged, not debuggable/no Internet/test entry, same signer. Physical NFC/camera and independent audit not replaced.

## Code 7 historical candidate

[Implementation](BITTAP-IMPLEMENTATION.en.md), [evidence](release/bittap-code7/README.en.md). Original signer 0.4.0/code 7, run-as refused, NFC/CAMERA only, four languages bundled, no tests/fixtures. 76 units(62 core/14 Android), lint clean; targeted language-lint explanation reflects bundled resources/no language splits/offline switching, not online download.

210 UI/lifecycle/file/QR/NDEF assertions: four-language consent/refusal/every entry/full terms, public note/authentication, read-only default off/irreversible consent/unknown, multi card/Home. Actual Camera2 seven assertions: 32 distinct digests, timestamp-matched final photo, CG3 generation/cleanup. First no-extra-wait failure stopped at photo, unclassified; later spaced capture passed, not real entropy/all phones.

Independent Python CG3/2/1/public-field byte mutation/four scalars/four-addresses/Taproot spending; Java CLI inspect/hidden-password CG3 shows authenticated public note/addresses, no WIF. Real NFC write/lock/cross device/interruption/partial lock and real camera permission/lens/OEM not performed. Implementer regression, not independent full audit.

## Code 6 historical release

2026-10-16 original signer updates 4/5, v2 verified, not debuggable, no test/network/storage, run-as refused. 51 core+4QR+6 image I/O units, lint clean. 162 interaction+15 security-reproduction assertions,[review](release/SECURITY-REVIEW.en.md), release/DEVICE-SMOKE-RESULT.txt and DEVICE-SECURITY-RESULT.txt. Unified address/labels/full wrap/buttons/private consent/exact PNG/cancel-fail-timeout/lifecycle/files/readback/backup/multi card.

Real file result/I/O, not full picker UI automation. NFC sheets tested via production presenter, no RF claim. No crypto/RNG/dependency change; prior independent results retained. Test APKs/screenshots removed, main left Home;[physical order](release/README.en.md).

## Code 4 historical regression

55 units(51 core/4QR), six strength-hint tests, weak passwords still allowed then. Four mainnet types exact scan 160/280/480 pixels; refuse keys/encrypted backup/URI/wrong network/checksum. 88 emulator assertions DEVICE-TEST-RESULT.txt: strength updates, no strength-disabled generation, Home clears inputs, mask old view/wipe scalar.

QR across write/done/CG2 pre-post/CG1; all four list types exact decode, full display, mutually exclusive pending/verified labels, close QR retains list, accurate copy/bitmap cleanup/FLAG_SECURE. Public QR needs no key. Empty savedInstanceState including removed old keys; memory configuration continuation preserves wallet/backup/multi card. Home clears read/export/deferred/candidate UID/original buffers, owned private/encrypted backup clipboard only; target wallet short paste retained. Emulator cache empty, no wallet files/preferences/databases; public screenshots removed, but not JVM/keyboard/system forensic proof. Compile/sign/lint pass; comprehensive audit deferred until release; password free address copy verification undecided.

## Initial 0.4.0 functional validation

41 units(39 core/2WIFQR): RNG error/stall fails closed, rejection/range, scrypt/GCM vectors, case/space/Unicode, BIP38/four-addresses/Taproot. CG2 public four-address parse, tampered address GCM refusal, reauthenticated but wrong key address refusal, type/order/version/truncation, CG1 fixture. Creation requires NFC verification+file backup; identical ciphertext/different UID counts, close/readback failure cannot pass.

35 emulator assertions: new/old MIME, Text, URL not key, multi key refusal, predecrypt four-addresses, bad password no key, exact compressed mainnet WIF, default mask, three help items, explicit QR close/background, CG1 before/after. Mandatory backup/second card button/back cannot bypass, cancel not backup, configuration retains ciphertext/unbacked state. Media Store URI actual write/close/readback then second card identical payload, cancel returns done; not full picker UIautomation.

Independent Python new CG2/old CG1 recover public scalar 1, every address byte mutation GCM refused; scalars 1/2/3/n−1 four-addresses/Tap Tweak spending pass(two INTEROP files). Actual CLI CG2/CG1 inspect/hidden-password CG2 recovery, no WIF print. APK0.4.0/code 4 display BitNFC, same ID/local debug signer update. Lint 0 errors/0 warnings; only annotated original Bitcoin SVG precision hint suppression, not global lowering. NFC permission only, no test fixtures/entries, secure main/private windows. SVG glyph strings identical, only uniform scale/translate/NFC Arcs; design/logo source/hashes.

Found immutable QR bitmap eraseColor crash; changed mutable and verified cleanup/close.

## Limits

All fixtures public, never fund. Completion/NFC states test-injected ciphertext/public UID, not actual card write. Test APK alone temporarily removes secure flag for public screenshots/restores; production no switch. Real NFC off/blank/overwrite/capacity/read-only/wrong card/changed content/removal/write/fresh readback/second card and external import/Taproot spend still pending. Preserved 30 sec mask, 60 QR close, sensitive clipboard/120 best-effort cleanup; tests default mask/manual close/background, not every timeout/all historical copies. Force-stop/uninstall/task removal cannot preserve unfinished session.
