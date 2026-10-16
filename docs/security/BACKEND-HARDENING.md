# BTA-06～11 后台修订记录（未发布）

基线为 `b43a54df3254446e58ab2327f5b8f4b78911a884`（0.5.0 / 15）。
本轮不修改 Android 业务页面、文案或交互；不覆盖历史 tag/Release。
这是修复和验证记录，不是独立复审结论或资金安全保证。

| 项目 | 实施内容 | 验证及边界 |
| --- | --- | --- |
| BTA-06 运行时依赖 | bitcoinj 0.17.1、BC 1.86、Guava 33.7.2、protobuf 4.36.2、SLF4J 2.0.17；移除旧 OkHttp/Okio 的传递运行时依赖；适配 bitcoinj 包名与公钥 API | 实际运行时 15 个 Maven 坐标经 OSV 查询为 0 命中；不能推论不存在未知漏洞。CG1/2/3、压缩 BIP38、四类地址与 Taproot 向量回归；独立 Python 校验另见报告 |
| BTA-07 干净构建 | 补齐两份 JUnit BOM module 和升级依赖的 SHA-256；保持 strict，不增加信任豁免 | 34 个新增校验条目逐一对照官方 Maven Central 的 checksum 或文件；空依赖缓存完整构建成功，无外部 JUnit POM-only 绕过 |
| BTA-08 发布来源 | 无发布密钥的 CI、固定 JDK/SDK/Gradle/action commit、独立 APK 内容和恢复 ZIP 比较、本机原证书签名及签名 manifest、CODEOWNERS | 工具与流程已加入；远端 CI 和完整候选签发需按最终提交执行后记录。**主分支强制保护未启用**：当前个人私有仓库 API 返回 403，要求 GitHub Pro 或公开仓库。未更改可见性、协作者权限或购买套餐 |
| BTA-09 CLI 读取 | 仅允许不跟随符号链接的普通文件；实际读取沿用 1026 字节上限；打开及读取有 10 秒期限，阻塞线程为 daemon | 6 项单元测试；额外以 32 MiB JVM 子进程检查 `/dev/zero`、FIFO、1 MiB 文件，均拒绝且未内存溢出。异常文件系统仍可能消耗期限以内的时间 |
| BTA-10 许可证 | APK assets 与恢复 ZIP 附完整 LICENSE/NOTICE、逐依赖文本、原 JAR 告知、CycloneDX SBOM、文件摘要和作用域 | 实际 APK/ZIP 检查许可内容；13 个外部恢复 JAR 与清单 SHA-256 一致；新组件无已审查许可登记则构建失败。历史 docs 告知不冒充本轮二进制清单 |
| BTA-11 报告渠道 | SECURITY.md、私有 Issue 入口、报告内容/处理/公开前准备规则 | 按用户选择使用私有仓库 Issue；所有授权账号均可查看。未冒充仅维护者可见的私密报告，也未宣称实际通知投递已测试 |

## 本机证据

- 空依赖缓存 strict 构建：100 个任务执行成功；62 core + 14 app + 6 recovery 单元测试通过，lintDebug 无问题。
- 发布工具对抗检查：5 项通过，覆盖 APK 内容替换、空归档、manifest 篡改及错误证书。
- [运行时公告查询](backend-hardening/dependency-audit.json) 记录查询时刻与所用 SBOM 的摘要。
- [新增校验值复核](backend-hardening/new-checksum-verification.json) 记录官方来源和实际结果。
- [本机构建环境](backend-hardening/environment.json) 中 sourceCommit 是测试开始时的基线 HEAD；最终源提交以 CI 和候选 manifest 为准。
- 历史发布的 0.5.0 APK 不因提交这些修复而自动更新。

## 尚未处理的 BTA-01～05

弱密码创建策略、未认证地址的收款出口、Android 9 私钥剪贴板、遮挡/辅助功能防护、前台解锁期限仍需另行批准和实现。原型设计仅用于讨论，不应误认为这些代码已修好。Java/密码库的历史内存副本不能承诺取证级擦除。

## 核验来源

- [bitcoinj release notes](https://bitcoinj.org/release-notes)
- [Bouncy Castle news](https://www.bouncycastle.org/news/)
- [Gradle dependency verification](https://docs.gradle.org/current/userguide/dependency_verification.html)
- [GitHub protected branches availability](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)
- [GitHub secure Actions use](https://docs.github.com/en/actions/reference/security/secure-use)

分支保护的剩余处理：由所有者决定是否升级支持私有分支保护的套餐；启用后要求维护者审查、CI 成功、禁止 force-push/删除并限制绕过。在此之前，本地签名、PR 和 CODEOWNERS 不能等同于服务端强制保护。
