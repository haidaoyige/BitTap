# Security policy / 安全报告

BitTap is an experimental offline tool with known open security findings;
functional tests and an APK signature are not a funds-safety audit.
The active 0.6.x development line accepts security reports. Reports concerning
older backups are also welcome, especially any recovery compatibility issue.

## Report a vulnerability

Email **[ianzhangone@outlook.com](mailto:ianzhangone@outlook.com)** with a subject
beginning `[BitTap Security]`. Repository access is not required to use this
reporting address. Do not publish an undisclosed vulnerability in public Issues.

Include the affected version and source commit, Android version if relevant,
expected/observed behavior, attack prerequisites, and a minimal reproduction
using public test vectors or a disposable wallet that has never held funds.
Do not attach real private keys, passwords, seed phrases, wallet exports, private
QR images, signing credentials, or logs/screenshots containing those secrets.

The maintainer acknowledges reports by email and records severity, remediation
commits and verification, coordinating disclosure with the reporter. There is no
guaranteed response deadline or bounty program. If you have not received
acknowledgement, follow up in the same email thread; do not expose details publicly
to attract attention.

Authorized reviewers may also use an
[issue in this private repository](https://github.com/haidaoyige/BitTap/issues/new)
with a title beginning `[Security]` and mention `@haidaoyige`. This alternative is
visible to **every account with repository access**, not only the owner. Use it
only if that audience is acceptable; email is the primary reporting channel.

## Before making this repository public

The owner should verify that the reporting mailbox receives messages and that
replies can be delivered before making this repository public. The address is
configured in this policy; delivery and reply have not been tested by this change.
**Do not use public Issues for undisclosed vulnerabilities.** GitHub private
vulnerability reporting has not been enabled as part of this change.

## 当前中文说明

安全漏洞请发送至 **[ianzhangone@outlook.com](mailto:ianzhangone@outlook.com)**，
邮件主题以 `[BitTap Security]` 开头；无需获得仓库访问权限。
请说明受影响版本、源提交、复现步骤、攻击前提及预期／实际结果。
只使用公开测试向量或从未存入资金的测试钱包，切勿发送真实私钥、密码、
助记词、钱包备份、私钥二维码或含有这些秘密的日志与截图。

维护者通过邮件确认接收并协调修复与披露；未收到回复时请在原邮件中跟进。
私有仓库 Issue 仍可作为获授权审计人员的备选渠道，但所有仓库授权账号均可见。
本次仅配置报告邮箱，未发送测试邮件或验证收发；仓库公开前应实际核对邮箱可达。
