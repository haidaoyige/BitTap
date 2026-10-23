[简体中文](LOCAL-REVIEW-code38-2026-10-20.md) · [English](LOCAL-REVIEW-code38-2026-10-20.en.md) · [文档目录 / Documentation](../README.md)

# BitTap 0.6.2/code 38: tests and local security review

2026-10-20, working source/original-signer release candidate. Implementer review/automation, **not original auditor's independent reaudit or absolute funds guarantee**.

## Changes/results

Compact creation hint: NFC icon, wrapping copy, right supported-card link; help/content/click unchanged, four languages. No crypto/password/NFC-format/backup change. Compared with code 37 production changes only layout, four hints and version Code. Test-only fixes addressed Android 9 download API, obsolete camera password/copy assertions and animation interference; no relaxed product rules.

Strict Release build; 96 units (72 core/18 Android/6 recovery), no failures/errors/skips. Lint 0 errors/9 warnings (unused resources, typography, text concatenation), not zero warnings. Dedicated API35/API28 unfunded emulators at 360 dp; new 320 dp creation test. Final passing assertions overlap and cannot be summed as independent security cases.

| Suite | API35 | API28 |
| --- | ---: | ---: |
| Four-language creation/password/help/cancel/lifecycle |673|673|
| Risk/per-entry consent |328|—|
| Four-language security help |271|—|
| Recovery/lifecycle |296|288|
| Disabled/loading password action |112|112|
| Weak-pattern consent |109|—|
| Obscured/sensitive actions |137|112|
| Copy consent / old-OS refusal |80|24|
| Address/key image/error |419|—|
| Pending-address save consent |276|—|
| Session/fault provider |63|57|
| Keyboard visibility |88|—|
| Four-language Home/entries |32|—|
| Camera2 through encryption |16|—|
| Actual accessibility/pixel comparison |85|72|
| Full UI/NDEF/backup/multi card |259|—|
|320 dp four-language creation |673|—|

Actual about 60-second idle lock/cleanup/nonautomatic notice dismissal; 120-second accelerated boundary/units, not full real wait. Accessibility positive control reads old TextView WIF, then no WIF in new drawing nodes/events; not all active accessibility attacks.

Independent Python CG1/2/3 recovery/AAD/address-note tamper refusal; four public scalars' Legacy/P2SH/SegWit/Taproot and spending scalar checked. Five provenance tamper tests; 13 recovery JAR/license hashes match. 15 final runtime coordinates OSV zero known matches at query time, not unknown-flaw proof. Original cert, not debuggable, no Internet, backup disabled, no test providers/services/scalar resources.

## Source review

Scope: core RNG/key/address/format/password/camera; Android creation/recovery/background/lock/clipboard/accessibility/overlay/QR/files/NFC; CLI/material/signing/read-only repo checks. No new critical/high finding in this layout change.

Implemented: system 32-byte rejection sampling, no time/fixed seed; camera pre obtained secret with 32 distinct previews/final digest/HMAC; no gallery. At code 38 historical CG used scrypt 32768/8/**p=1**, random 16 salt/12 nonce, AES 256 GCM 128 tag, public header AAD, allowlisted/bounded parsing. Authenticated recovery rederives every address; password protects random key, does not generate it; WIF and four-addresses same scalar. Strict 10+ ASCII/four categories/consistent normalization; recovery separate. Generation wipes owned plaintext after recovery self-check; creation retains only ciphertext/public workflow. Background/Home/deadlines invalidate late jobs. 60 idle/120 total synchronous exits; dedicated non semantic drawing/secure window/overlay hiding. API 28 no WIF copy, newer consent/sensitive/readback/ownership cleanup. File close/exact readback required, 15 sec/cancel/stale fails, bounded workers/PNG wipe. NFC same card/old contents/fresh readback, separate permanent consent/retap; physical acceptance pending.

## Still open at that time

| Item | Finding / next action |
| --- | --- |
|01| Offline guessing/guessable compliant passwords. Evaluate p=1→p=3 against OWASP same-memory reference after real-device benchmarks/allowlist/migration decision; guide not wallet certification or compromise proof. Cost alone cannot fix predictable passwords |
|02| Unauthenticated public address/note can be substituted; pending labels/consent/image warning reduce misuse, not authenticate |
|04| Drawing not hardware isolation; authorized malicious accessibility/keyboard/root/injection/OS may access secrets. String/BigInteger/provider copies not reliably wipe able; no root forensics/full injection exercise |
|08|2026-10-20 read-only check: private, main unprotected, auditor 0xDenji-Ai write retained pending SSH key. Uncommitted code 38 no final independent clean CI; signer not source proof |
|11| Mail configured, no delivery test |
|12| Official compressed vectors pass but valid scalar 1 rejected by bitcoinj; compatibility flaw, not ordinary key leak; not fixed then |

BTA13 no physical-camera minimum entropy; 32 distinct hashes prove no bit gain. BTA14 bounded workers cannot force provider return/recall bytes. NFC driver fault acceptance pending. Authorized WIF/PNG exports cannot be recalled from recipient/cloud/provider/history. Ordinary NFC not authentic/non cl on able; read-only does not stop copying.

Not covered: physical 215/216 capacity/write/interruption/power loss/damage/lock/multi card; all OEM/keyboard/camera/service variations; external import/spending/Taproot matrix; erasure/entropy certification/final source independent CI/external agency. Latest user desktop audit unavailable then; BTA IDs from repository history/current code, not original-report sign-off.

External evidence `BitTap-security-validation/compact-support-code38/`: final logs, 96XML, validation-summary, OSV, BIP38 reproduction, signature/manifest, production inputs/screenshots. Delivery sanitized, no signing keys/passwords/wallets. References: [OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#scrypt) queried 2026-10-20, [clipboard](https://developer.android.com/privacy-and-security/risks/secure-clipboard-handling), [sensitive activities](https://developer.android.com/security/fraud-prevention/activities).
