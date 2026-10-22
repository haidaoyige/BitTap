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

安全漏洞请发送至 **[ianzhangone@outlook.com](mailto:ianzhangone@outlook.com)**，
主题以 `[BitTap Security]` 开头；也可使用仓库 Security 中可见的私密漏洞报告入口。
**仓库及 Issues 已公开，请勿在那里披露尚未修复的漏洞。**

请说明受影响版本、源提交、复现步骤、攻击前提及预期／实际结果。
仅使用公开测试向量或从未存入资金的测试钱包。切勿发送真实私钥、密码、助记词、
备份、私钥二维码、发行凭据或含有这些秘密的日志与截图。

维护者协调修复和披露；未收到回复时请私下跟进，不保证回复时限或漏洞奖金。
