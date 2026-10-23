[简体中文](REMEDIATION-STATUS.md) · [English](REMEDIATION-STATUS.en.md) · [文档目录 / Documentation](../README.md)

# 0.7.0 release overview — 2026-10-22

Code 50 uses code 49 functionality. Public source/version/provenance changes are not new hardware acceptance. [Release scope](../release/v0.7.0/RELEASE-NOTES.en.md). Issues are now public and unsuitable for undisclosed flaws; use SECURITY email or available private report channel. Later [Oct 23 audit](audits/v0.7.0-2026-10-23/README.en.md) supersedes historical acceptance status below.

## Code 49 targeted revision

Xiaomi 15/Android 16 NFC-LOCK-L3-TIME then NFC-D led to explicit NTAG215/216 page locking, removal of generic Ndef.makeReadOnly/can Make Read Only, corrected reserved-byte writes and stepwise readback, retaining all final criteria. No affected-phone stack/RF acceptance: cannot claim proven OEM-service fix. [Details](NFC-NTAG-LOCK-code49.en.md). Older paths below are history.

## Code 48 diagnostics

After actual Confirm permanent lock, “written/lock unconfirmed” does not simply mean option never enabled. Fixed LOCK/VERIFY stages/reasons retain unknown and all conditions, not lock algorithm change. No device logs/physical bits, root cause pending. [Details](NFC-LOCK-DIAGNOSTIC-code48.en.md).

## Code 47 diagnostics

Affected phone still timed out about 5 seconds during startup and lost NFC response; code 46 not accepted there. Fixed startup stages/“written, not locked” title for no USB diagnosis, not root-cause/hardware proof. [Details](NFC-STARTUP-DIAGNOSTIC-code47.en.md).

## Code 46 revision

Already-read-only prerejection; explicit adapter control off main, 5-second startup bound, cancellable processing, uncertain write/lock not success. Fresh writable readback and 15-second I/O retained. No phone ANR stack/RF acceptance, not fully closed OEM ANR. [Details](NFC-READONLY-ANR-code46.en.md).

## Code 45 implementation overview — 2026-10-22

Separate bounded NFC worker, 15-second deadline, connection cancellation, expanded weak-pattern detection. [Scope/tests](NFC-AND-PASSWORD-code45.en.md). Device results follow final evidence, never predeclare card acceptance.

| ID | State / remaining boundary at code 45 |
| --- | --- |
|01|10+visible ASCII/four categories, consistent normalization, p=3, expanded weak warning but consent permits continuation; guessing remains |
|02|[Full-payload local records](local-address-verification.en.md); unknown receiving exits require verification, matching records password-free; not clone/rollback/balance proof |
|03| Minimum Android 12/API31; old paths removed, not old-OS acceptance |
|04|[Keystore/gates](BTA-04-code40.en.md); brief plaintext/root/injection/external copies remain |
|05|60 idle/120 total/background/Home; code 44 actual idle on two emulators, code 45 regression |
|06| Code 44 final 15 coordinates OSV zero on 2026-10-22; code 45 dependencies unchanged, final packaging rechecks |
|07| Strict/local rebuild maintained; uncommitted workspace not final independent clean CI |
|08| Exact Actions SHA/read-only default/sign-before-provenance gate; protection/auditor write pending then, final release independent CI required |
|09| Ordinary NOFOLLOW/bounded CLI/deadline; abnormal filesystem acceptance separate |
|10| Materials/licenses/JAR hashes continuously checked; old attachment hashes not new evidence |
|11| ian zhang one@outlook.com configured, delivery untested, no mail sent |
|12| Code 39 scalar 1 fix, code 44 independent 9 vectors pass; older “unfixed” is history |
|13| Camera supplemental, no quantified bits/physical entropy certification |
|14| Bounded file workers, 15 sec, cancel/stale rejection; cannot recall external bytes |
|NFC| Bounded dedicated worker/close/late refusal; stuck driver can limit NFC, not forcibly terminate every call; physical lock/interruption pending |

## Earlier revision chronology

Code 39 on 2026-10-20: 01 uniform p=3 with explicit owner-approved p=1 incompatibility; 12 valid scalar 1 BIP38 recovery with standard address verification; 08 exact Actions SHA/independent artifact signing gate. [Details](BTA-01-08-12-code39.md). Predictable passwords/private-plan limits/retained auditor write not fully closed.

Code 38 same date: compact supported-card row, 96 units/API28/35 suites, reviewed public-address/plaintext/repository/mail/BIP38 and suggested KDF cost. [Local review](LOCAL-REVIEW-code38-2026-10-20.en.md). Included uncommitted work/no final independent CI; NFC/lock pending.

Code 32: remove random generation/copy/saved consent after actual feedback; manual strict rules/normalization/weak warning/recovery retained. v0.6.2 older sections not current behavior.

2026-10-19 v0.6.2 added ASCII/normalization and optional 32 random password/saved consent; not proof arbitrary manual password safe. [Policy](password-policy.en.md), [historical generator](random-password.en.md), [release](../release/v0.6.2/RELEASE-NOTES.en.md).

## Historical v0.6.1/code 29 reaudit inventory — 2026-10-18

Compared owner's 2026-10-17/code 23 report and 14 items with current source/code 28 delivery/later checks; not new independent audit/original auditor closure. **Six not fully closed, seven revised/closed within original scope, one information observation.**

| ID | Historical result | Residual / evidence |
| --- | --- | --- |
|01| Hardened, residual |10 Unicode codepoints/all categories; pure numeric example refused, patterned valid allowed after consent, offline guessing; old-password compatibility then required |
|02| Partial | Pending display/copy/QR/explorer permitted, save consent/code 28 multilingual export warning; substitution could still receive; warning not authentication |
|03| Revised/tested | API 28 button/consent/copy version checks, emulator not external physical sign-off |
|04| Dedicated drawing added | No WIF semantic nodes, state/autofill/capture/Android 14 sensitivity; actual API 28/35 service/pixels pass. Malicious authorized service can operate, root/injection not covered, original audit closure pending |
|05| Implemented |60 idle/120 total, exit checks; code 25/26 actual 60, 120 units/accelerated boundary; code 27 same |
|06| Closed in original inventory | Re query final release OSV, not future unknown zero |
|07| Original scope closed | Strict local; every final release independent CI, not cache build substitute |
|08| Partial |2026-10-18 main protected false/rules 403 plan. Final 0.6.1CI/payload/recovery/sign manifest not code 23 evidence; server enforcement absent then |
|09| Original scope closed | Ordinary NOFOLLOW/bounded read/10 sec daemon; fault filesystem may consume deadline |
|10| Original distribution closed | New release all attachments verify current inventory |
|11| Local configured, publish/delivery pending |0.6 policy/email updated, no delivery test |
|12| Unfixed then | bitcoinj 0.17.1 rejects valid scalar 1; no extended retest then, compatibility not random key leak; owner then inclined retain |
|13| Information | Conditional camera observation, not minimum physical entropy proof or new medium flaw |
|14| Implemented | Bounded workers/15 sec/cancel/stale/readback; code 25/26 fault provider regression, UI releases but underlying calls not forcibly terminated |

### Follow-up boundaries at that historical stage

02/04 affect receiving/plaintext; prototype approval before exit/accessibility changes. 01 do not equate format with entropy or forbid already-approved weak-pattern continuation. 08 server protection/final source CI, not Android UI. 11 policy/delivery/no external test mail without authorization. 12 “not planned”never “fixed.” Verify-only was a proposal, not duplicate finding; random-password button not yet approved then.

### Historical evidence/delivery

[Policy](password-policy.en.md), [deadlines/files](session-and-file-limits.en.md), [sensitive actions](SENSITIVE-ACTIONS.en.md), [private drawing](private-key-presentation.en.md), [images](address-image-export.en.md).

Code 28: 91 units, API 35 key/pixels 85/API28=72, image 419/save 276/session-files 63 including actual 60 idle. Overlap, not distinct totals; delivered/installed hash match, uncommitted local candidate/no CI comparison. Code 27: 91 units, final APK loading 112, same production full generation 157, overlap. Provenance `includesUncommittedChanges=true`, `independentCiComparison=false`. Local signer/tests not independent security/CI.

Physical permanent lock/deliberate interruption/all ROM-keyboard-service attacks/external spending/sensor entropy not newly accepted by this historical inventory.
