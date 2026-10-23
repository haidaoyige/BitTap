[简体中文](BACKEND-HARDENING.md) · [English](BACKEND-HARDENING.en.md) · [文档目录 / Documentation](../README.md)

# BTA-06–11 backend hardening, included in v0.6.0

Baseline `b43a54df3254446e58ab2327f5b8f4b78911a884` (0.5.0/code 15). No Android page/copy/interaction changes or replacement of historical tags/releases. Implementation evidence, not independent reaudit or funds guarantee.

| Item | Implementation | Evidence / boundary |
| --- | --- | --- |
|06 dependencies| bitcoinj 0.17.1, BC1.86, Guava 33.7.2, protobuf 4.36.2, SLF4J2.0.17; remove transitive OkHttp/Okio, adapt packages/public-key API |15 actual runtime coordinates, OSV zero matches, not zero unknown flaws. CG1/2/3, compressed BIP38, four-addresses/Taproot regression; independent Python separately recorded |
|07 clean build| Add two JUnit BOM module checksums and upgraded artifacts; keep strict, no exemptions |34 new Central entries individually checked, one Linux aapt2 via official Google mirror. Empty dependency cache full build, no external POM-only workaround |
|08 provenance| Key-free CI, pinned tool chain/actions, independent APK/recovery comparison, local original signer/signed manifest, CODEOWNERS | Final source CI/candidate must actually run. At this historical stage personal private-plan protection API403 required Pro/public. No visibility/access/plan changes here |
|09 CLI input| Ordinary files, no sym link following, 1026-byte actual read bound, 10-second open/read daemon deadline |6 units plus 32 MiB JVM sub process /dev/zero, FIFO, 1 MiB file refusal without OOM; abnormal filesystems may consume deadline |
|10 licenses| Full LICENSE/NOTICE, dependency texts/JAR notices, Cyclone DX SBOM, hashes/scopes in APK/recovery | Actual package contents and 13 external recovery JAR hashes checked; unreviewed new license fails build. Old docs not current binary inventory |
|11 reporting| SECURITY, email, historical private-Issue alternative, content/handling rules |2026-10-18 owner-selected ian zhang one@outlook.com primary, no repository access needed. Then-private Issues visible to all collaborators. No mail sent/delivery test. Public Issues no longer private after visibility change |

## Local evidence

Empty-cache strict build 100 tasks; 62 core+14 app+6 recovery units, lint Debug clean. Five release-tool tamper tests: substituted APK, empty archive, changed manifest/wrong certificate. [OSV](backend-hardening/dependency-audit.json) binds timestamp/SBOM; [Linux aapt2](backend-hardening/linux-aapt2-verification.json), [new checksums](backend-hardening/new-checksum-verification.json), [environment](backend-hardening/environment.json). Environment source Commit is baseline at test start; final source is CI/manifest. Historical 0.5.0 APK does not auto-update.

## Then-outstanding BTA-01–05

Later approved [sensitive actions](SENSITIVE-ACTIONS.en.md), [weak consent](WEAK-PASSWORD-CONFIRMATION.en.md), [minimum 10](PASSWORD-MINIMUM-10.en.md), [pending-image consent](UNVERIFIED-ADDRESS-IMAGE-SAVE.en.md). Valid weak passwords/pending downloads still allowed with consent; residual 01/02 not eliminated. Code 18 adds per-address status; at that time verify-only, secret semantics and foreground deadline unimplemented. [Status history](REMEDIATION-STATUS.en.md). No JVM/library forensic-erasure promise.

Sources: [bitcoinj](https://bitcoinj.org/release-notes), [BC](https://www.bouncycastle.org/news/), [Gradle](https://docs.gradle.org/current/userguide/dependency_verification.html), [protected branches](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches), [Actions security](https://docs.github.com/en/actions/reference/security/secure-use).

Historical next step: owner decides plan supporting private protection, then require maintainer review/CI, block force-push/delete, restrict bypass. Until actual server enforcement, signing/PR/CODEOWNERS are not equivalent protection.
