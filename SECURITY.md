# Security policy / 安全报告

BitTap is an experimental offline tool. Version 0.5.0 has known open security
findings; functional tests and an APK signature are not a funds-safety audit.
The active 0.5.x development line accepts security reports. Reports concerning
older backups are also welcome, especially any recovery compatibility issue.

## Current private reporting channel

This repository is **private**. Authorized reviewers should open an
[issue in this private repository](https://github.com/haidaoyige/BitTap/issues/new)
with a title beginning `[Security]`, and mention `@haidaoyige` to request triage.
The report is visible to **every account with repository access**, not only the
owner. Use this channel only if that audience is acceptable. If you do not have
access, request a private reporting channel from the maintainer who supplied
your copy; do not publish the vulnerability in an unrelated public issue.

Include the affected version and source commit, Android version if relevant,
expected/observed behavior, attack prerequisites, and a minimal reproduction
using public test vectors or a disposable wallet that has never held funds.
Do not attach real private keys, passwords, seed phrases, wallet exports, private
QR images, signing credentials, or logs/screenshots containing those secrets.

The maintainer records acknowledgement, severity, remediation commits and
verification in the private issue, and coordinates disclosure with the reporter.
There is no guaranteed response deadline or bounty program. If you have not
received acknowledgement, follow up in the same private issue with the owner
mention; do not expose details publicly to attract attention.

## Before making this repository public

The owner must first configure and verify GitHub private vulnerability reporting
or publish a working dedicated security contact, then update this policy and its
links. **Do not use public Issues for undisclosed vulnerabilities.** GitHub's
public-repository private-reporting feature is not currently available here.

## 当前中文说明

现阶段由已获授权人员在本私有仓库建立标题含 `[Security]` 的 Issue，
并 `@haidaoyige` 请求处理。所有具有仓库访问权限的人都能看到报告。
请只使用公开测试向量或从未存入资金的合成钱包，不上传真实秘密。
维护者在同一 Issue 记录接收、分级、修复和复验，协调后再公开细节。
仓库公开前必须先建立并实际验证私密报告入口，再更新本政策。
