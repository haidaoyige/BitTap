[简体中文](BitTap-0.7.0-安全复审报告.md) · [English](BitTap-0.7.0-安全复审报告.en.md) · [文档目录 / Documentation](../../../README.md)

> Public translation: technical conclusions/limitations retained; local evidence paths only index private archives. [Latest status](README.en.md) governs.
>
> **2026-10-23: user stopped physical-card testing. Ordinary write/first-failure recovery passed, original post-interruption card contents unverified, permanent locking not performed. No overall card acceptance. [Stop record](BitTap-0.7.0-实体卡测试停止记录.en.md).**
>
> **Software update:** [phone supplement](BitTap-0.7.0-软件真机补充审计.en.md) supersedes software deferral in the first-phase snapshot below; physical NFC remains unaccepted.

# BitTap v0.7.0 security reaudit

Audit 2026-10-22, v0.7.0/code 50, commit `53c281b5f3ba8fa47ac1c59e9166300b384aaf33`. Baseline v0.6.1/code 29,`2894a1d0b7e030e72251617e48061bc09594ad35`. Codex-assisted source/independent host/distribution-CI review, not agency certification.

## Conclusion/release impact

**Host/source phase completed. No new confirmed key leak, address-verification bypass or cryptographic implementation flaw; one upgrade recovery incompatibility must be clearly communicated to older users. At this phase device/NFC acceptance was separately deferred by user, not end-to-end approval.**

Substantive improvements: unknown receiving payloads gated on verification, full-payload local records, temporary Keystore sessions, accessibility gating, BIP38 scalar fixes. Bounded NFC/cancellation/deadlines/specific locks show no false-success route within source/host models.

Independent 137 units/10 release-tool tests; CG1/2/3, four-addresses/BIP38 pass. Release APK90 nonsignature ZIP entries match same-commit CI/local audit reconstruction. Release recovery ZIP byte-identical to CI. No absolute guarantee/device substitution.

**This audit stage changed no production code/features/requirements/access/Release; no commit/push/mail/card write/permanent lock.**

## Scope/evidence

102 changed files versus baseline, 434 tracked files. Review entry/authentication, format, KDF, RNG, derivation, secrets, persistence, file/NFC state machines/supply chain.

| Module | Independently completed in phase 1 | Not established then |
| --- | --- | --- |
| Generation/camera | RNG/rejection/32 frames/final digest/HMAC/cleanup source and host tests; camera core byte-unchanged from baseline | Current device camera/minimum physical entropy/certification |
| CG1/2/3 | p=3 positive/negative, AAD, tamper, Python, cross-release official JAR | p=1 deliberately unsupported; real funded-wallet recovery guarantee |
| Addresses/Taproot | Four scalars/four-addresses/TapTweak | All external import/spending |
| BIP38 | Release JAR 6 valid/3 reject, Python 9, official JUnit/bad password/flags/checksum | All tools, uncompressed unsupported |
| Local records | Every-byte full-digest mutation, source paths, release-library tamper | Device persistence/delete/restart/upgrade |
| Sessions/accessibility | Wrapping/lifecycle/gates/exits source, raw-key holder-close host tests | Current device backend/races/all JVM-GPU erase |
| Auto lock | Unchanged deadline algorithm/host regression, new cleanup call paths | Current real 60/120 waits/lifecycle; old results not transferred |
| Files/exports | Provider workers/cancel/gate/late/readback source, helpers/CLI tests | Current actual SAF/faults/PNG/clipboard |
| NFC/locks | Bounded workers, UID+content, lock model/fault tests, NXP comparison | RF/power loss/removal/read-only/OEMANR/lock |
| Distribution/deps |20 attachment hashes, 16 manifest members, cert/CI/APK/recovery/licenses/SBOM/OSV | Uncompromised signer, unknown flaws, long-term permissions |
| Reporting |0.7 policy/public repo disclosure copy | Actual email/replies/private-report switch |

Release Android 12/15 emulator evidence downloaded and signed-digest-bound is **publisher evidence**, not instrumentation run by this audit or physical-card evidence.

## R070-01: older released backups incompatible with new recovery

Recovery/upgrade availability risk,**high impact if users still hold p=1 wallets**. Explicit source policy, not unauthorized change/key-loss vulnerability. Locations WalletCrypto.java: 30, 192; code 39 doc; release/SECURITY internal-test wording.

Released 0.6.1 core emits p=1; 0.7 only p=3 and rejects header before KDF. Old library rejects new p=3; Android/CLI share core. Public scalar-one/public-password fixtures, never fund:

| Official library |0.6.1-generated p=1|0.7-generated p=3|
| --- | --- | --- |
|0.6.1| Recovers | Unsupported profile |
|0.7.0| Unsupported profile | Recovers |

Private evidence cross-version.log/CrossVersionProbe.java/public fixtures. Proves new-tool incompatibility,**not corrupt old data/permanent loss**; old tool+password recover fixture, no evidence real users lost funds. Keeping p=3 policy: identify affected released versions(at least 0.6.1), link fixed-hash old offline tools/check recovery before upgrade. Do not call every p=1 source merely internal. If confirmed never funded, record accepted compatibility boundary. Never edit authenticated p byte as migration; KDF differs. Audit reported only, no policy/card/real-wallet migration.

## Original 14-item reaudit

| ID | Phase 1 result | Evidence/remaining scope |
| --- | --- | --- |
|01| Hardened, residual |10+ASCII/four categories, p=3 CPU cost; `Password1!` changes MEDIUM→WEAK, `Ab1!Ab1!Ab1!` WEAK but permitted with consent. Not entropy/offline immunity or duplicate new flaw |
|02| Source/core negatives pass, device pending | receivingAllowed gates copy/display/export/explorer; unknown decrypt first, recorded full SHA256; creation write/readback+backup; UID alone never trust |
|03| API 28 scenario excluded | APK min 31 excludes 9–11, not acceptance on old OS |
|04| Source passes, dynamic deferred | Any configured/enabled service blocks inputs/decrypt completion/view/copy/private file; session ciphertext/nonce not disk, nonexportable key, explicit WIF briefly plaintext. Root/injection/OS/exports not covered, legitimate services restricted |
|05| Algorithm pass, new integration device pending |60 idle/120 total/background/navigation/synchronous exits; late unwrap epoch/owner/deadline |
|06| Known inventory pass |15 OSV coordinates 0 matches, not unknown zero |
|07| Strict/same commit CI pass |137 units, no errors/fails/skips, lint 0 errors 12 warnings; local cache vs new CI Gradle home distinct |
|08| Provenance verified, permission detail not closed |20 files/16 members/cert/CI/90 entries/recovery match; main.protected true but detail 404/rules empty/Actions 403. Neither old unprotected nor complete enforcement inferred |
|09| Original scope pass | CLI/BackupInput unchanged, ordinary NOFOLLOW/bounds/deadline; not hardware filesystem proof |
|10| Distribution pass | Licenses/NOTICE/SBOM/13 recovery JAR hashes match |
|11| Policy updated, delivery untested | No public unfixed disclosure, email/conditional private channel; no mail sent |
|12| Original defect closed within tested vectors |1, 2, n−1, NFC, two EC modes; bad scalar/network refused; official JAR/Python crosscheck |
|13| Bounded information observation | Secret before capture, images supplemental, source unchanged; no extra bits claim |
|14| Bounded design/gate, device pending | Bounded work/close pools, 15 sec, cancel/identity; provider bytes not recallable/unresponsive call not forcibly stoppable |

## New mechanisms

WalletFingerprint hashes **entire original canonical payload** with domain separation/length prefix, not just address/UID. Every-byte mutation mismatch/parser refusal; actual release ciphertext tamper GCM refusal. VerificationStore only digest/public addresses/note/time/card hint, independent Keystore AES-GCM, noBackupFilesDir, atomic, 256 records/256 KiB, strict parsing, missing key/corruption fail closed. Save/clear serialized under lock. No physical identity/balance/freshness/rollback guarantee. Device persistence/delete/restart/upgrade needed then.

PrivateSession.seal finally closes original holder. Session ciphertext/random nonce/public AAD only; exit wipe/asynchronous alias delete, next process stale alias cleanup. setUserAuthenticationRequired(false) means not biometric authorization; nonexportability not proof hardware TEE/StrongBox. WIF strings/chars/QR pixels/PNG/clipboard still plaintext windows; ownership/deadlines not forensic wipe. Service-change callbacks/output checks reduce race, not substitute real service-switch tests.

NFC data/control/close separated, bounded. Writable/same UID/previous contents before write, fresh readback after. Timeout/cancel/late cannot reach success; already issued write cannot rollback, retain unconfirmed. 215/216 model/mirror/access checks, CC→dynamic→configuration→static/readback, RFUI zero. Final payload/bits/NDEF read-only. CFGLCK effective after power cycle, remove/retap/RF cannot be replaced by a model. [NXP Rev 3.2](https://www.nxp.com/docs/en/data-sheet/NTAG213_215_216.pdf). Previously reported Xiaomi/Android16 NFC-LOCK-L3-TIME/D not proven fixed; permanent lock/removal/power loss unperformed then, require explicit card/irreversibility authorization.

## Automation/supply chain

137 units(85 core/46 app/6 recovery), 0 fail/error/skip; lint 12 warnings(UnusedResources, TypographyEllipsis, SetTextI18n), 0 errors. 10 release-tool tests CI identity/fixed entries/digest/signature tamper. Initial missing JAVA_HOME tool runfixed and rerecorded, not product defect.

Python CG1/2/3 p=3, four scalars/four-addresses/Taproot, 9 BIP38 pass. Host hashlib.scrypt unavailable, independent cryptography Scrypt same parameters used, no production changes. Semgrep 60 Java rules/63 files: two AES-ECB findings at Bip38Importer.java: 84, required by[BIP38](https://github.com/bitcoin/bips/blob/master/bip-0038.mediawiki), with scalar/address hash validation not new flaw; CBC replacement breaks standard.

Gitleaks 22 commits/18 hits=14 public cert digests+4 historical source hashes. 434 files/23 valid WIF literals all public vectors/boundary scalars. No real secrets in examined scope, not exhaustive encoding proof. OSV2026-10-22: 15 runtime coordinates 0 matches, only public coordinates sent, no source uploaded.

Release APK SHA256 `a22be1a37c915385aa809cc804f1a11265539d8754b3bff9239ac2465024c5dc`; v3 sign verified, cert `58b6613e32239ddf229066fa6bdf1b77579ade691a5d29b5bf621341a3c2b85a`; min 31/target 35, no Internet, backup disabled, not debuggable. Recovery ZIP `51109a528b19e5360b2e33ae0fdcd64fffd8ec2500ddb90a1da3df0d9be91171`.

[Same commit CI 39743646530](https://github.com/haidaoyige/BitTap/actions/runs/39743646530) success; direct comparison/verification artifact downloads/GitHub SHA checked, XML independent count 137. Initial local APK missing Git metadata only provenance differed; with matching metadata all 90 entries match. Release recovery matches CI bytes; local RecoveryCli.class differs with JDK 17.0.6 vs 17.0.18, string-concat bytecode. **No claim local recovery ZIP byte-reproducible**; class source unchanged, official provenance established by CI exact match.

## Deferred device plan at phase 1

Verify actual APK hash/version/cert/update preserving user data; Keystore seal/unwrap/close/failure/process death/real 60 and 120; save original accessibility config and only authorized test service/races input-unwrap-copy-PNG/restore; record save/read/change UID payload/corrupt/missing key/delete/restart/cancel/four receiving exits; actual camera/SAF/independent recovery/reimport/fault provider; disposable card write/read/decrypt/read-only/interruption, permanent lock only separately authorized with retap/full bits/read state, focus affected OEM. Deferral was not authorization to install/write. No real funds/external spending.

## Delivery/reuse

Private complete package: report, machine status, evidence, public probes/fixtures, exact source ZIP, prior baseline, SHA256SUMS. Readable locally without external cache; external disk only rebuildable intermediates. No real passwords/keys/phone screenshot collected in phase 1; no signing key/current device data. Exact source archive has original public test images, no APK/recovery binaries(their links/cert/hashes retained). Never fund vectors. Public copy does not distribute private complete evidence. Applies only specified commit/attachments; later feature/dependency/signature/artifact changes require impact review.
