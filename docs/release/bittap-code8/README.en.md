[简体中文](README.md) · [English](README.en.md) · [文档目录 / Documentation](../../README.md)

# BitTap 0.4.0/code 8 prototype-alignment candidate

2026-10-16. Approved header language entry/horizontal card; [full alignment](../../UI-PROTOTYPE-ALIGNMENT.en.md). Candidate `app/build/outputs/bittap/BitTap-0.4.0-code8-candidate.apk`, original signer, io.coingift.android, version 0.4.0/code 8, in-place update.

-76 units(62 core+14 Android), no failures, XML/UNIT-RESULTS.json.
- DEVICE-SMOKE-RESULT.txt covers language sheet staying open, About, nine security accordions/return, note counter, camera grouping, backup/multiple cards/QR decode/cleanup.
- CAMERA-RESULT.txt uses actual emulator camera, matched final timestamp, in-memory preview, photo disposal after confirm, CG3 generation; hashing/mixing unchanged.
- Repeated final image occurred in same round; production validation refused/no wallet. CAMERA-REPEATED-IMAGE-RESULT.txt. Bounded retry recollects without bypass or silent standard fallback. Final pass is not extra-entropy measurement.
- Release lint clean, not debuggable, run-as refused, NFC/CAMERA only, no Internet/storage, test Runner/password/scalar fixtures.
- Original certificate; APK hash SHA256SUMS.txt.
- Actual candidate screenshots with empty/public fixture wallets, not redraws; never fund.

![Actual pages](screenshots/preview.png)

[Home](screenshots/aligned-home.png) · [Languages](screenshots/aligned-language.png) · [About](screenshots/aligned-about.png) · [Security](screenshots/aligned-security.png) · [Comparison](screenshots/aligned-security-comparison.png) · [Standard creation](screenshots/aligned-setup-standard.png) · [Enhanced creation](screenshots/aligned-setup-enhanced.png)

Android controls system fonts/bars/permission/file pickers. Long pages scroll, never hide content. Physical NFC/cross-device refusal/real camera remain unaccepted; not independent funds audit.


## Original artifact identifiers

`app/build/outputs/bittap/BitTap-0.4.0-code8-candidate.apk`

`58b6613e32239ddf229066fa6bdf1b77579ade691a5d29b5bf621341a3c2b85a`
