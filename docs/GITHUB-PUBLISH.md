# 当前发布：0.7.0

仓库 `haidaoyige/BitTap` 改为公开，Apache-2.0不变。主APK与恢复ZIP经同提交独立CI和本机原证书签发后上传Release；发行密钥仅本机保存。当前流程以 [发布与验证](release/README.md) 为准，下文为早期历史记录。

# GitHub 上传内容

源码包括 Android App、密码学核心、离线恢复工具、Gradle Wrapper 和依赖校验文件；资料包括使用说明、CG1/NFC 格式规范、更新记录、测试报告、第三方许可及独立验证脚本。

仓库不包含真实私钥、用户密码、真实卡片备份、签名私钥、本机 SDK 路径、缓存或构建目录。源码测试中的私钥/地址均为公开标准测试向量，请勿充值。

V0.5.0 发布到独立私有仓库 `haidaoyige/BitTap`，不复制原仓库的 Git 历史。Release 附件包含 `BitTap-v0.5.0.apk`、`BitTap-v0.5.0-offline-recovery.zip` 和 `SHA256SUMS.txt`；源码由相同版本标签提供。APK 沿用现有发行签名，签名私钥由维护者离线单独保管。本机路径、构建缓存、local-dev 和发行凭据不进入新仓库。

初次构建请安装 JDK 17、Android SDK 35 / build-tools 35.0.0，并设置本机 `local.properties`。参见根目录 README。
