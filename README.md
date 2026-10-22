# BitTap · Android NFC 冷钱包工具

**在手机上离线生成 BTC 私钥，用密码加密后保存到 NFC 卡。**

BitTap 支持制作钱包、读取 NFC 卡与导入加密备份，并可导出 WIF 私钥供其他钱包使用。它不托管资金，不签名或广播交易，也不查询余额。App 没有联网权限、账号系统、遥测或私钥云同步。

当前版本 **0.7.0 / 内部版本 50**，支持 **Android 12（API 31）及以上**，仅支持 **BTC 主网**。源码公开，采用 [Apache License 2.0](LICENSE)。

[下载 APK 与离线恢复工具](https://github.com/haidaoyige/BitTap/releases/tag/v0.7.0) · [本版说明](docs/release/v0.7.0/RELEASE-NOTES.md) · [安全报告](SECURITY.md) · [审计修订状态](docs/security/REMEDIATION-STATUS.md)

> 当前仍为实验版本。测试与签名不能证明资金安全；实体 NFC、不同手机和外部钱包仍需实测。尤其是小米15／Android16的 NFC 锁定阻塞，本版已实施针对性修复，但尚未取得修复后的实机验收结果。请先使用无资金测试钱包。
>
> **本版只接受 scrypt N=32768/r=8/p=3 的项目备份。** 此前 p=1 的内部测试卡片与文件不兼容，没有迁移分支；BIP38 按其标准参数导入。

## 主要功能

- 中文、英文、日文、韩文及跟随系统语言；每次制作或读取前确认风险提示。
- 系统安全随机源生成独立私钥；可选相机采集作为额外混合输入，图像仅在本机内存中处理。相机模式不承诺可量化的额外熵，也不能弥补弱密码。
- 可选公开钱包备注；同一私钥提供 SegWit、Legacy、P2SH 与 Taproot 四种收款地址。
- 写卡前检查容量、可写状态及原内容；写入后实时读回，必须完成加密文件备份。可以将同一钱包保存到多张 NFC 卡。
- 已在本机建立核验记录的钱包，再次读取完整相同数据时可免密码核对公开地址，无需再次解密私钥。未知数据仍需完整密码解密和地址推导核对。
- 查看 WIF 与私钥二维码前明确操作；收款地址与私钥图片支持四语言透明圆角 PNG，底部英文标语保持固定。
- 解锁会话使用独立 Android Keystore 密钥封装；连续60秒无操作、解锁满120秒、切后台或返回首页时结束私钥会话。Android及密码学库的历史内存副本无法保证物理擦除。

## 制作与读取

1. 点击“制作冷钱包”，填写可选备注；如开启相机增强，先完成采集。
2. 在密码弹窗输入并确认独立密码：10–1024位可见 ASCII，须同时包含大写字母、小写字母、数字和半角标点；不接受中文、emoji、空格或全角字符。弱模式会额外提示，但格式合格并不等于难以猜测。没有随机密码生成功能。
3. 使用已格式化为 NDEF、容量足够的可写卡片完成检查、确认和写后读回。建议 NTAG215/216，App 不自动格式化卡片。
4. 保存加密备份文件。只有文件写入、关闭和完整读回核对都成功才算完成；取消、超时或保存失败不会绕过备份要求。密码无法重置，应与卡片和备份分开妥善保管。
5. 在“读取冷钱包”贴卡或导入备份。无本机核验记录时先输入完整密码；解密认证并重算地址后才建立记录。记录管理位于“关于 BitTap → 如何免密核验地址 → 管理本机核验记录”。

公开核验记录只保存负载摘要、公开地址和备注等，不保存密码、私钥或完整备份；由 Keystore 认证加密存储，卸载或清除数据后需要重新建立。它不证明卡片身份、余额、防克隆或防回滚，详见[本机核验说明](docs/security/local-address-verification.md)。

制作进度只在会话内保留，不写入系统页面状态或应用缓存。返回首页清理当前钱包会话，已保存的核验记录及用户主动导出的文件保留。未备份时不要强制终止进程。

## NFC 永久只读

默认关闭，只适用于识别到的 NTAG215/216，需再次确认且不可撤销。卡片仍可被读取、复制，只读不等于硬件密钥隔离。

本版使用专用芯片锁位流程，逐步读回；动态锁页的保留字节按芯片要求写零。锁定后须按提示真正移开卡片，再贴回核验。只有钱包内容、硬件锁位和 NDEF 只读状态全部符合才显示已永久只读；仅开启开关或写入成功都不代表锁定成功。失败或超时可能留下部分锁定状态，请保留备份，不要覆盖已有钱包进行诊断。

实现与硬件验收边界见 [NFC 锁定修订](docs/security/NFC-NTAG-LOCK-code49.md)。模拟器测试不能代替真实射频、卡片或小米15／Android16验收。

## 地址与私钥转出

| 地址类型 | 构造 | 前缀 |
| --- | --- | --- |
| SegWit | P2WPKH / Bech32 | bc1q… |
| Legacy | P2PKH，压缩公钥 | 1… |
| P2SH | P2SH-P2WPKH | 3… |
| Taproot | BIP341 key-only / Bech32m | bc1p… |

BitTap 是单私钥钱包，没有助记词或 HD 派生路径。四种地址余额分别记录。导出的是 **BTC 主网压缩 WIF**，不携带地址类型；目标钱包须支持实际存币地址的单私钥导入，支持 HD Taproot 不等于支持本工具的 Taproot 恢复。请核对完整收款地址，再转入已备份的新钱包；明文私钥一旦复制或导出，不能保证外部副本被清除。

地址二维码仅包含公开地址。经用户隐私确认，可由外部浏览器打开区块链浏览器；网站会知道所查地址和网络信息。不要向网站、AI或他人提供私钥、密码及私钥二维码。

## 加密与设备边界

系统 `SecureRandom` 提供随机数据，以拒绝采样生成 `[1,n-1]` 内的私钥；随机源失败即停止。加密使用 scrypt `N=32768,r=8,p=3,dkLen=32` 与 AES-256-GCM，128位认证标签，每次新建16字节 salt 和12字节 nonce。密码采用 Unicode NFC 规范化；新建时严格执行上述 ASCII 规则。

**CG3 / CG2 / CG1 是项目格式，不是 BIP38。** 读取项目格式时只接受本版的成本参数，格式和认证范围见 [FORMAT.md](docs/FORMAT.md)。低熵密码仍可被离线猜测，应用内重试限制不能阻止持有卡片副本者离线尝试。

新建、解密、查看及导出私钥时仍会短暂处理明文。Keystore 会话封装、FLAG_SECURE、遮挡检测和辅助功能服务门禁只能缩小风险，不能抵御已被攻破的系统、Root或进程注入。敏感操作会拦截已启用的辅助功能服务，包括合法服务；关闭后需主动重新操作。已导出的文件、剪贴板历史和输入法副本不受自动锁定控制。[BTA-04说明](docs/security/BTA-04-code40.md)

## 编译、测试与发布验证

固定工具链：Temurin 17.0.18+8、Gradle 8.11.1、Android SDK 35 / build-tools 35.0.0。配置 `local.properties` 的 `sdk.dir` 后：

```sh
./gradlew --dependency-verification strict :core:test :recovery:test :app:testDebugUnitTest :app:assembleDebug :app:lintDebug :recovery:distZip
```

设备测试与产品源码分开，不向生产 APK 加入测试入口：

```sh
./gradlew :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w io.coingift.android.test/io.coingift.android.SmokeRunner
```

应用 ID 保持 `io.coingift.android`，发行包沿用原证书，可覆盖安装同签名旧版。调试签名不同，切换前请先保存自己的加密备份和密码。

发布要求最终干净提交通过独立 GitHub Actions CI，再在本机重建并比较 APK 全部非签名内容及恢复 ZIP，通过后用原证书签发来源清单。发行私钥不上传 GitHub 或配置到 Actions。详细步骤及签名清单验证见 [发布流程](docs/release/README.md)。

## 独立离线恢复

下载 Release 的恢复 ZIP，解压后使用 Java17：

```sh
bittap-recover/bin/bittap-recover --inspect /本地/加密备份.txt
bittap-recover/bin/bittap-recover --decrypt /本地/加密备份.txt
```

CLI支持本版成本参数的 CG3/CG2/CG1 和压缩 BIP38，只有输入 `SHOW` 才打印 WIF；终端历史缓冲仍可能保留明文。请同时保留格式说明及恢复工具。

## 文档与许可

- [0.7.0发布说明](docs/release/v0.7.0/RELEASE-NOTES.md)、[历史版本](https://github.com/haidaoyige/BitTap/releases)、[更新记录](docs/CHANGELOG.md)
- [当前审计修订与剩余边界](docs/security/REMEDIATION-STATUS.md)、[第三方组件与标识来源](docs/THIRD_PARTY.md)
- 漏洞请按 [SECURITY.md](SECURITY.md) 私下报告，勿把真实钱包秘密或未披露漏洞发到公开 Issues。

本项目采用 Apache License 2.0；第三方组件遵循各自许可证。开源、签名和测试不构成资金安全保证。
