# BitNFC 0.4.0 安全评审

本报告记录修订前测试包的审查证据；下述原始发现不代表后续源码已重新完成全面审计。

日期：2026-10-15。结论：**不能判定用户提出的四项全部通过；当前测试包不应作为正式资金保管版本。** 上一轮通过的是功能与兼容性测试，不能替代资金安全审查。本轮未修改生产 Java 逻辑或重新发布 APK；新增对抗测试、仅测试包使用的 Runner 选择参数和本报告。以下风险仍待修复。

受检 APK：`BitNFC-0.4.0-debug.apk`，SHA-256 `2f242ab373f454b8950729147aa5e8ab2dbece508d84c1b1e5ebd0ed6aaf4355`。源码为本机 `codex/bitnfc-v040` 的未提交工作区，不能仅靠原 HEAD 复原；测试与源码应一并保留。

## 用户四项要求

| 要求 | 结论 | 证据及限制 |
| --- | --- | --- |
| 每次新建私钥随机生成 | 实现与现有验证通过 | `WalletCrypto.java:33,94` 使用系统 SecureRandom，32 字节拒绝采样至 `[1,n-1]`，没有时间/密码种子、取模或弱随机回退；失败及卡死随机源会停止。Android 15 实测 provider=AndroidOpenSSL，algorithm=SHA1PRNG。新增 12 次新建的地址、salt、nonce 无重复，仅作抽样检查，不能证明所有设备的熵健康。“写入另一张卡”按设计复制同一钱包，不是新建。 |
| 完成最后一步备份后，软件内删除私钥和加密文件 | 按字面要求未通过 | 生成阶段拥有的明文数组在加密及恢复校验结束后已尽力清零；制作会话仅保留密文/公开地址。但文件保存成功后仍保留密文，供继续写卡，并写入 savedInstanceState。只有“完成，返回首页”清除活动会话引用。清引用不等于擦除所有 JVM/库/系统保存状态副本。用户选择导出的备份文件应保留，不能把它也删除。 |
| 私钥与对应地址推导一致 | 现有验证通过 | 生成后解密比对原始标量；CG2 解密认证后重新推导四地址逐项比较。已过标准向量、奇偶 Y、独立 Python/OpenSSL 四地址和 TapTweak 花费标量检查；WIF 已知向量一致。未完成外部钱包实际花费，不能保证任意钱包可导入单私钥 Taproot。 |
| 解密后，复制前没有系统暴露 | 仅能部分满足 | 默认不显示 WIF，不自动写入剪贴板、日志或文件；但解密即在 App 堆中保留原始标量。点击显示/二维码也会在复制前输出明文。模拟器保持 FLAG_SECURE 时，UiAutomation 仍能从辅助功能语义树读到已显示的 WIF；这是有特权的测试能力，不等于任意普通 App 无权限即可读取。已 root/可调试/恶意辅助功能或输入法环境无法作绝对保密保证。 |

AndroidOpenSSL 的 `SHA1PRNG` 是系统算法名称；对应 Android 系统实现调用 BoringSSL `RAND_bytes`，不意味着应用拿时间或密码做 SHA-1。没有发现随机生成代码的弱种子或可预测回退。参见 [Android SecureRandom](https://developer.android.com/reference/java/security/SecureRandom)、[Android 系统实现](https://android.googlesource.com/platform/prebuilts/fullsdk/sources/+/refs/heads/androidx-constraintlayout-release/android-35/com/android/org/conscrypt/OpenSSLRandom.java)。

## 优先处理的发现

### S01 / 高：短密码允许离线猜测

位置：`core/.../WalletCrypto.java:234`，scrypt 参数定义在第 30 行。

当前仅限制 6 字符和非全空白，`123456` 可以创建卡片。攻击者得到卡片或备份后能在自己电脑上猜测；App 内增加失败次数限制不能约束这种离线攻击。新增测试创建公开样本的弱密码备份，脱离 UI 以候选密码成功恢复；这证明弱密码路径成立，不是攻破 AES 或猜出高熵密码，也没有对真实钱包穷举。

建议拒绝常见密码、短 PIN、连续/重复模式，鼓励安全随机生成的长口令并给出明确强度反馈；不把“至少六位”作为资金安全标准。同时评估提高 KDF 成本并进行版本兼容。当前 `N=32768,r=8,p=1` 低于 [OWASP scrypt 参数参考](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#scrypt)中同内存档的 `p=3`；该参考不是钱包认证标准，也不能替代实际设备性能测量或口令熵。旧备份必须继续按原参数解密，不能直接改全局常量导致旧卡失效。

### S02 / 高：解密前的公开地址可被替换，收款可能转错

位置：`MainActivity.java:218,255`；`WalletCrypto.java:144` 的 inspect 仅做语法检查，认证在第 203 行的 decrypt。

攻击者可以覆盖可写卡片的四个地址为自己控制的合法地址，保留原密文。程序会显示和复制这些“待解密核验”的地址；之后输入正确密码会被 GCM 拒绝，保护原私钥。但如果用户先用显示的地址充值，资金已经转向攻击者。

新增测试逐项替换全部四地址，确认 inspect 返回替换后的地址，decrypt 拒绝。没有发送交易或使用真实资金。这是未认证数据使用边界风险，**不是 GCM 校验被绕过**。现有“待解密核验”标签有帮助，但直接复制用于收款仍缺少明确操作门槛。

建议在未核验地址的复制/收款路径明确提示，建立一次密码验证或独立可信指纹比对后再将其标为可信。无密码查看可以保留；不能同时声称在没有任何外部信任锚的情况下可验证整卡未被替换。把公钥/自签名一起写在同一张可替换卡里也不能自动解决整卡替换。物理锁卡能减少改写，仍无法防止克隆或换卡；备份用于防止卡片损坏/覆写造成不可恢复。

### S03 / 高（有条件）：当前交付的是可调试测试包

受检 APK 的合并 Manifest 为 `android:debuggable=true`，模拟器 `adb shell run-as io.coingift.android id` 成功。这需要已授权的 ADB/调试环境，并非普通第三方 App 自动获得调试权限；但显著放宽了进程调试和应用数据访问边界。

正式版必须不可调试、由维护者控制的发行密钥签名，并发布可核对的源码版本、依赖锁定和 APK 摘要。调试混淆或关闭 UI 截图不能替代这些要求。参见 [Android 官方 debuggable 风险说明](https://developer.android.com/privacy-and-security/risks/android-debuggable)。

### S04 / 高（有条件）：显示 WIF 后，辅助功能可读取；缺少覆盖层防护

位置：`MainActivity.java:240`，`AppUi.java` 的按钮/输入控件，Manifest。

实测：未点击复制，主动显示 WIF，FLAG_SECURE 仍开启，UiAutomation 能读取对应 TextView 的完整文字。未在真机安装恶意辅助功能服务；测试证明语义树暴露，不证明无权限提权。普通未授权 App 不能因此直接读到其他进程内存。

当前没有对敏感控件设置专用辅助功能敏感属性或隐藏明文节点，也没有过滤覆盖层/部分遮挡触摸。建议对私钥文字实施适合系统版本的语义树保护，给显示、二维码、复制、写卡确认加入遮挡触摸防护，在支持的系统上隐藏第三方覆盖层，并说明与合法读屏工具的取舍。仍不能保证抵抗已 root、恶意系统或被滥用的高权限服务。参见 [Android 防护边界](https://developer.android.com/security/fraud-prevention/activities)、[Tapjacking](https://developer.android.com/privacy-and-security/risks/tapjacking)。

### S05 / 中：自动隐藏不是自动锁定，明文会话没有前台超时

位置：`MainActivity.java:62,229`；`WalletCrypto.java:65`。

30 秒回调只把 TextView 改为圆点。实测执行该回调后，Unlocked 仍持有 32 字节私钥。只要前台停在该页，明文 holder 可继续存在；切后台或导航会 close 并清零其拥有的数组，这一部分验证通过。

建议增加真正的解锁会话期限、空闲/失焦锁定和尽量短的明文存活时间。清零持有数组是尽力措施；BigInteger、字符串、密码规范化、加密 provider、ZXing 中间数据和运行时可能存在不可控副本。普通 Java Android App 不能作“内存中绝不残留”的承诺；对这类强保证需另行设计硬件隔离，而不是改几处置 null。

### S06 / 中：备份成功不清理密文，与用户要求存在差距

位置：`MainActivity.java:87,126,337`。

实际文件提供程序写入、关闭和读回完成后，session 仍存在；随后保存 Bundle 内仍有同一密文。点击完成会清除活动 session 引用，但先前保存的 Bundle 副本不会因置 null 自动被擦除。测试没有证明 Android 在完成后一定还能恢复旧钱包，也没有声称已有明文私钥写入系统保存状态。

建议明确“保存备份”“继续写更多卡”“结束并清理会话”的边界，结束时统一清理 session、incoming、exportData、候选卡原数据、待处理回调和 UI 引用，检查序列化路径；保留用户导出的备份文件。若要求密文也绝不离开内存，应移除 savedInstanceState 密文保存，并重新解决进程回收时未备份钱包丢失问题。须清楚区分可靠防丢和尽力清理，不能同时作无法兑现的绝对保证。

### S07 / 中：Android 9 剪贴板和不可信输入法/文件提供程序

当前 minSdk=28，允许 Android 9。其后台其他应用可读剪贴板；敏感标记只保护支持系统的预览，不把剪贴板变为保险箱。120 秒清理在进程被终止或后台限制时不保证执行；已读取的副本无法追回。建议提高最低系统版本或限制老系统的明文复制，评估更短的有效时间及明确的清理入口。参见 [Android 剪贴板风险](https://developer.android.com/privacy-and-security/risks/secure-clipboard-handling)。

密码框禁止自动填充和个性化学习能减少常规副本，但挡不住恶意输入法。App 没有 INTERNET 权限不代表输入法、辅助功能、系统或用户选择的云文件提供程序不能联网。备份写出的是密文；仍应使用可信设备、输入法和保存位置。

### S08 / 发布前处理：运行依赖命中已知安全公告

从恢复分发包及锁定的校验元数据识别 16 个外部运行依赖，再加入 Android ZXing，共查询 **17 个 Maven 坐标**。OSV 返回 **12 条唯一公告**，原始查询/结果/详情保留在 `security/`。这不是构建工具、Android 系统、所有设备固件或库完整代码的审计。

| 组件 | 命中问题 | 当前路径评估 |
| --- | --- | --- |
| bitcoinj 0.16.5 | CVE-2026-44714，ScriptExecution P2PKH/P2WPKH 验证绕过 | 当前工具做密钥/地址/导入，不调用该交易脚本验证入口；未发现此漏洞直接破坏地址推导的路径。仍应升级至修复版本并完整复验。 |
| bcprov-jdk15to18 1.83 | GOST CTR、证书 Name Constraints、LDAP、Lazy ASN.1，共 4 条 | 当前用 scrypt、secp256k1 数学，未发现对应 GOST/证书/LDAP/不可信 ASN.1 入口。不能把公告数量直接换算成“可盗私钥”。 |
| Guava 31.0.1-android | 临时目录/信息泄露，2 条 | 当前源码未使用相关临时目录 API。 |
| protobuf-javalite 3.18.0 | 恶意消息导致资源耗尽，3 条 | 当前备份解析为固定二进制格式，不解析 protobuf 消息。 |
| OkHttp 3.14.9 / Okio 1.17.2 | 错误证书接受 / 有符号转换，共 2 条 | 当前没有联网权限和网络调用；未发现对应远程输入入口。 |

这些是基于现有源代码调用路径的判断，不是自动化完整可达性证明。正式发布前应升级/移除不用的传递模块，更新可信依赖校验数据并重跑随机源、BIP38、四地址、GCM、新旧格式和 Android 测试，不能为方便升级而关闭校验。

来源：[bitcoinj 维护者公告](https://github.com/bitcoinj/bitcoinj/security/advisories/GHSA-hfcf-v2f8-x9pc)、[Bouncy Castle 安全公告入口](https://www.bouncycastle.org/vulnerability-advisory.html)、各公告原始引用见 `security/dependency-advisories.json`。

## 已有且验证有效的防护

- CSPRNG 拒绝采样、无弱随机降级；私钥范围校验；每次新建不同盐和 nonce。
- AES-256-GCM 认证覆盖版本、网络、KDF 参数及全部四地址；错误密码、地址或密文篡改不释放私钥。
- KDF 参数固定白名单，输入长度/编码/地址类型有边界；截断及 4096 份确定性变异未出现预期外解析异常。
- 生成后恢复校验；CG2 解密后四地址重算；旧 CG1 兼容及 BIP38 标准向量保留。
- NFC 检查可写状态/实际容量；覆盖确认；同 UID 和原内容复查；写后实际读回完整 NDEF。UID 不作为防克隆认证。
- 文件输出成功关闭后重开读回；失败不算备份。该步骤证明提供程序返回字节一致，不证明云端/硬件永不丢失。
- 没有私钥明文文件、日志或自动剪贴板写出代码；Android 系统备份和迁移已排除；无联网权限。
- 默认 WIF 遮蔽、窗口防截图；切后台关闭明文 holder 并实际清零其拥有的私钥数组。

## 本轮证据与范围

- 核心 **45 项 JUnit** 全部通过（原 39 + 本轮 6），详见新增 `SecurityReviewTest.java` 和 `security/core-audit-tests.xml`。
- Android 15 ARM64 上 **15 项安全复现断言**完成，`security/device-audit.txt` 明确标注“观察到的弱点仍未修复”。测试通过只说明现象已复现，不等于安全通过。
- 独立 Python 新旧备份恢复、四个公开标量的四地址及 TapTweak 花费标量复验通过。
- 全程只使用公开样本及无资金测试创建，不读取真实用户钱包；没有资金转移、真实私钥穷举或恶意 App 投放。
- 未做实体 NFC 物理攻击、真实恶意输入法/辅助功能服务、侧信道、全堆取证、所有 Android 版本和 ROM、发行签名构建复现或外部钱包实际花费。
- 本次由实现者自行复核，不能替代独立密码学/移动端安全审计。没有发现可仅凭高熵密码加密后的正常卡片、在可信更新系统上直接恢复私钥的漏洞；这也不构成“不存在漏洞”的证明。

复现命令（使用本机 Android SDK / JDK 环境）：

```sh
./gradlew :core:test
./gradlew -PbitnfcAudit :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w io.coingift.android.test/io.coingift.android.SecurityReviewRunner
```

正式使用前建议按 S01/S02/S03/S04 优先加固，再落实 S05/S06/S07 的会话和平台策略、依赖升级及真机恢复验收。此前仅用无资金测试钱包验证，不把功能测试完成视为可正式保管资金。

## 用户确认后的调整（同日）

- S01：按用户选择增加本地密码强弱提示，不阻止原本允许的弱密码；保留最少 6 字符和确认一致规则。弱口令离线猜测风险仍然存在，不能标为已消除。
- S02：提出“保留免密查看，复制前提示，可选择密码核验后复制或明确继续”的方案，尚未改动地址复制规则，等待后续交互决定。
- S05/S06：以返回首页为统一清理边界，不新增前台解锁超时。停止将密文、输入和钱包状态写入 savedInstanceState，仅在内存中承接配置变化；返回首页关闭并清零当前持有的私钥数组，清除输入、二维码、密文引用、候选卡缓冲、待处理状态和仍属于本 App 的剪贴板。导出的备份文件保留。系统/JVM 历史副本无法保证取证级擦除，强制终止会丢失尚未备份的制作进度。
- S04：保留现有系统防护，不承诺抵抗受控系统或高权限辅助功能。
- 用户要求正式版生成后再做全面审计。本轮只做这些变更对应的功能与清理回归，不把旧报告或测试通过当作新版正式安全结论。
