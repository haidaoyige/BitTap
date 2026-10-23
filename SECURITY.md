# Security policy / 安全报告

BitTap is an experimental offline tool. Functional tests, source publication and
an APK signature are not a funds-safety audit. The active 0.7.x line accepts
security reports. Older versions and recovery problems may also be reported;
obsolete internal p=1 test backups are deliberately unsupported by 0.7.0.

## Report a vulnerability privately

Email **[ianzhangone@outlook.com](mailto:ianzhangone@outlook.com)** with a subject
beginning `[BitTap Security]`. Repository access is not required.
If GitHub shows **Security → Report a vulnerability**, that private reporting
channel is also available. **Public Issues and pull requests are not channels
for undisclosed vulnerabilities.**

Include the version/source commit, device and Android version if relevant,
expected and observed behavior, attack prerequisites, and a minimal reproduction
using public test vectors or a disposable wallet that has never held funds.
Do not attach real private keys, passwords, seed phrases, wallet exports, private
QR images, signing credentials, or logs/screenshots containing those secrets.

The maintainer acknowledges reports by email and coordinates remediation and
disclosure with the reporter. No response deadline or bounty is guaranteed.
If there is no acknowledgement, follow up privately rather than publishing the
vulnerability. This repository does not independently verify mailbox delivery.

## 中文说明

BitTap 是实验性的离线工具。功能测试、源码公开和 APK 签名不等于资金安全审计。
当前 0.7.x 接受安全报告；旧版本和恢复问题也可报告。0.7.0 明确不支持废弃的 p=1 内测备份。

安全漏洞请发送至 **[ianzhangone@outlook.com](mailto:ianzhangone@outlook.com)**，
主题以 `[BitTap Security]` 开头，无需仓库权限；也可使用 GitHub 实际显示的
**Security → Report a vulnerability** 私密报告入口。
**公开 Issues 和 pull requests 不用于披露尚未公开或修复的漏洞。**

请说明版本、源提交、相关设备及 Android 版本、攻击前提、复现步骤和预期／实际结果。
仅使用公开测试向量或从未存入资金的一次性测试钱包。切勿发送真实私钥、密码、助记词、
钱包导出文件、私钥二维码、发行凭据或含这些秘密的日志与截图。

维护者通过邮件确认报告，并与报告者协调修复和披露；不保证回复时限或漏洞奖金。
未收到确认时请私下跟进，不要直接公开漏洞。本仓库未独立验证邮箱的实际投递能力。
