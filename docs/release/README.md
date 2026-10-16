# BitTap V0.5.0 发布与实机测试

当前版本：**0.5.0 / versionCode 15**，应用 ID `io.coingift.android`，最低 Android 9 / API 28。独立私有仓库为 [haidaoyige/BitTap](https://github.com/haidaoyige/BitTap)。

[V0.5.0 Release](https://github.com/haidaoyige/BitTap/releases/tag/v0.5.0) 提供 `BitTap-v0.5.0.apk`、离线恢复工具和 `SHA256SUMS.txt`。APK 沿用原发行签名，可覆盖同签名的旧发行包。签名证书与本次验证见 [v0.5.0](v0.5.0/VALIDATION.md)。不分发 instrumentation 测试 APK 或发行签名私钥。

下文为历史候选包记录及仍适用的真机检查步骤；历史版本号和文件摘要不用于核验本次下载。

## 后台修订的发布候选流程（不覆盖历史 v0.5.0）

1. 使用 `tools/build-environment.json` 中固定的 Temurin 17.0.18+8、Gradle 8.11.1、SDK/build-tools，运行 `python3 tools/check_environment.py`。签名凭据保留在本机仓库外，不配置到 GitHub Actions。
2. 提交源代码并推送，让 `.github/workflows/ci.yml` 对同一提交执行无缓存、严格依赖校验的测试、lint、未签名 APK 与恢复 ZIP 构建、独立密码学/地址复核、许可证检查和实时 OSV 查询。网络失败也会失败，不记为扫描通过。
3. 工作区必须干净；在本机配置 README 中四个 `BITNFC_RELEASE_*` 签名环境变量（沿用原证书），然后运行：

   ```sh
   python3 tools/build_release.py --ci-run-id <成功的 push 或 workflow_dispatch 运行编号> --output /本机/新的候选输出目录
   ```

   可选 `BITTAP_BUILD_ROOT=/本机/临时构建目录` 将所有模块输出移出源码目录；受限网络可设置 `BITTAP_GOOGLE_MIRROR=1` 使用 Google 官方镜像路由，仍启用严格校验。不应使用未知镜像或跳过依赖校验。
4. 工具核对 CI 提交、事件与结论，重建并验证 APK 证书，比较 CI 与本机 APK 的全部 ZIP 内容（仅排除标准签名条目），并逐字节摘要比较恢复 ZIP。检查完整许可证与运行时 JAR 摘要后才生成候选附件。
5. 候选包含 APK、恢复 ZIP、SBOM、运行时清单、环境记录、APK 签名证据、SHA256SUMS；`release-manifest.json` 将源提交、CI 链接与附件摘要绑定，由原 APK 身份单独签名。独立验证时须从已信任的历史 APK/证书得到指纹，而不能信任下载包自带的指纹：

   ```sh
   java tools/ReleaseManifest.java verify release-manifest.json release-certificate.der release-manifest.json.sig <已独立确认的证书SHA256>
   ```

   签名验证通过后还须逐项核对 manifest 中的文件 SHA-256；`SHA256SUMS.txt` 本身不能替代来源签名。工具只准备本机候选，不自动建 tag、上传 Release 或覆盖历史附件。

此流程增加来源可复核性；不能证明代码没有漏洞。当前个人私有仓库套餐不能强制主分支审批/状态检查，CODEOWNERS 只路由审查请求，不能阻止有写权限的协作者绕过流程。保持私有需要仓库所有者升级到支持该能力的套餐并配置保护后，才能关闭这一部分风险。详见 [修订记录](../security/BACKEND-HARDENING.md)。

## 0.4.0 历史候选包

当前候选为内部版本 8：`app/build/outputs/bittap/BitTap-0.4.0-code8-candidate.apk`，沿用原签名，可覆盖内部版本 7。页面对齐及证据见 [本轮报告](bittap-code8/README.md)。

内部版本 7 的新增功能、验证边界见 [本轮实现说明](../BITTAP-IMPLEMENTATION.md)。候选包路径：`app/build/outputs/bittap/BitTap-0.4.0-code7-candidate.apk`；版本名 0.4.0、内部版本 7，沿用同一发行签名。新增 CAMERA 权限仅供用户主动开启相机增强，仍无 INTERNET 权限。四语言和本地完整条款包含在 APK 中。

新增实机检查：四语言及跟随系统、每次入口同意/不同意；备注在解密前后、文件与多卡中一致，非法备注拒绝；真实相机权限、采样、最终照片和后台中断；NTAG215/216 分别验证默认不锁、选择锁定后的二次确认、完整读回与硬件只读、其他手机不能覆盖、中断后的未知状态。永久锁定不可撤销，使用专门的空测试卡。浏览器查询只携带选中公开地址，外部跳转前提示隐私。

以下为内部版本 6 的历史交付说明；保留原文件以便核对旧版。

安装包：`app/build/outputs/bitnfc/BitNFC-0.4.0-release.apk`。应用 ID `io.coingift.android`，版本 0.4.0 / 6，最低 Android 9 / API 28。本次统一“展示地址”入口与二维码底部卡片、地址居中，界面名称改为 SegWit、P2SH，并支持地址和私钥 PNG 图片导出；沿用发行签名，可覆盖内部版本 4、5 的正式测试包。使用独立发行签名，关闭调试；没有测试入口、测试样本或联网权限。APK 摘要与签名证书分别见 `SHA256SUMS.txt`、`APK-SIGNATURE.txt`。

首次从调试包或另一台电脑签名的版本切换时，可能无法覆盖安装。请先保存原钱包加密备份和密码，再卸载旧版。今后的发行版本应沿用同一发行私钥；丢失私钥将无法为现有安装签发更新。

## 真机测试顺序

请使用无资金的新钱包，公开测试夹具的地址也不要充值。

1. 离线制作，检查密码强弱提示及确认密码，记录四种地址。
2. 用可写且容量足够的 NDEF 卡完成贴卡检查、确认写入与读回核对。测试容量不足、只读卡、覆盖旧内容、换卡和中途移开。
3. 保存加密备份文件，完成返回首页；重启 App 后分别从原卡和备份文件读取；加密文本复制、粘贴入口已取消，解密核对四地址与 WIF。
4. 从主地址栏与其他地址列表点击“展示地址”。检查完整地址居中换行；类型名为 SegWit、Legacy、P2SH、Taproot。解密前检查“待解密核验”，解密后检查“已与私钥核对，地址正确”。弹层内复制及保存收款地址图片，用其他设备扫码比对完整地址。
5. 测试错误密码、隐藏与显示私钥、私钥二维码、切到后台后重新解密，以及返回首页后清空会话。
6. 测试私钥二维码保存前的确认与取消；保存时通过系统文件选择器选择本机位置。图片为明文私钥，不会自动存入相册，也不属于加密备份。进入选择器会锁定私钥；旋转或等待超过 3 分钟后需重新保存。
7. 拒绝剪贴板权限或文件保存，确认不提示成功。保存成功需要关闭文件并读回核对，文件提供程序不允许读回时应提示未确认。
8. 将同一钱包写入第二张卡，分别读取两张卡与备份，确认地址和密码一致。

模拟器的 NFC 成功状态来自公开夹具注入，没有模拟成实体 NFC 写入通过。目标钱包单私钥导入和 Taproot 恢复兼容仍需另行验证。发行构建通过不等于资金保管安全通过；本轮自查记录见 `SECURITY-REVIEW.md`。

## 重建与验证

历史本机快捷脚本位于仓库外 `local-dev/`，不再作为可复核发布的必要条件。新的维护者按上文配置工具链并通过成功 CI 准备候选；不要请求或上传本机发行私钥。

针对发行包的设备测试使用 `-PbitnfcReleaseTest :app:assembleReleaseAndroidTest`。独立测试 APK 也使用同一发行签名，才可对不可调试的主 APK 进行 instrumentation 测试；不要分发测试 APK。审查 Runner 可再增加 `-PbitnfcAudit`。该参数只改变测试 APK 的入口。
