[简体中文](README.md) · [English](README.en.md) · [文档目录 / Documentation](../../../README.md)

# BitTap v0.7.0 security reaudit — public reports

Updated 2026-10-23. Exact scope: **v0.7.0/code 50**, commit [53c281b5f3ba8fa47ac1c59e9166300b384aaf33](https://github.com/haidaoyige/BitTap/tree/53c281b5f3ba8fa47ac1c59e9166300b384aaf33). Later commits do not automatically inherit these conclusions.

Codex-assisted source review, host tests, distribution/provenance checks and Xiaomi Android 15 software checks. Not third-party agency certification or an absolute/funds-safety guarantee.

## Final status

- **Host/source and software-on-phone checks completed.**137 unit and 10 release-tool tests passed; actual camera generation, SAF backup, independent decryption/four-address comparison, reimport and verification-record persistence after process restart passed.
- Keystore sessions, accessibility gates, faulty providers and actual 60-second idle/120.089-second absolute deadlines passed within the specified device scope. Assertions overlap; do not sum them into an independent test total.
- All 90 unsigned-content APK entries match same-commit independent CI; release recovery ZIP matches CI byte-for-byte. Local recovery ZIP differs under a different JDK; no complete local byte-reproduction claim.
- **Overall physical-card acceptance incomplete; further testing stopped by user.** Ordinary write/readback and rewrite after the first failure passed. Deliberate removal correctly reported failure, but later reads identified a different object type; original card's final contents unverified. **Permanent locking was not performed and is not accepted.**
- Remaining boundaries: compliant weak passwords, old p=1 recovery incompatibility, repository-protection details and mailbox delivery. Released v0.6.1 p=1 backups cannot recover in v0.7.0; old tools still recover the public fixture. This does not mean old data is corrupt.

## Reports

1. [Main source/host/provenance and 14-item reaudit](BitTap-0.7.0-安全复审报告.en.md). Historical first-phase snapshot; software deferral superseded below.
2. [Software-on-phone supplement](BitTap-0.7.0-软件真机补充审计.en.md), distinguishing release APK observations from independent audit-package instrumentation.
3. [Physical-card stop record](BitTap-0.7.0-实体卡测试停止记录.en.md), latest card status, superseding planned work in the other reports.
4. [Machine-readable status](status.json).
5. [Original report SHA-256 list](SHA256SUMS.txt), integrity only, not a new release signature/certification. It authenticates the listed original files, not these added translations.

## Public scope

Reports/sanitized status only: no passwords, real/temporary private keys, card recovery material, device UID/serials, accessibility configuration, raw device logs, reproduction scripts or complete private audit ZIP. Public example passwords are not actual passwords of this round's random wallets. Raw evidence is privately archived; mentioned filenames are archive indexes.

The original audit-document publication did not alter app features/dependencies/version/Release/security policy. Privately report new vulnerabilities under [SECURITY](../../../../SECURITY.md).
