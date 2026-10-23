[简体中文](THIRD_PARTY.zh-CN.md) · [English](THIRD_PARTY.md) · [文档目录 / Documentation](README.md)

# 第三方依赖

应用源码采用Apache License2.0，见LICENSE；没有使用OneKey代码或素材。

`core/.../Bip38Importer.java` 改编bitcoinj0.17.1的BIP38PrivateKey解密逻辑，版权2014 Andreas Schildbach，Apache-2.0。保留上游解析、AES/scrypt原语及P2PKH核验，以校验后的原始标量导入替换私钥包装器来支持标量1。源文件和分发NOTICE保留改编归属。

## 原文件记录的运行时安全加固基线

- bitcoinj0.17.1：Apache-2.0，https://github.com/bitcoinj/bitcoinj
- Bouncy Castle bcprov-jdk15to18 1.86：Bouncy Castle License，https://www.bouncycastle.org/licence.html
- Guava33.7.2，Android/JRE变体：Apache-2.0，https://github.com/google/guava
- protobuf-javalite4.36.2：BSD-3-Clause，https://github.com/protocolbuffers/protobuf
- ZXing core3.5.3：Apache-2.0，https://github.com/zxing/zxing
- SLF4J2.0.17：MIT，https://www.slf4j.org/license.html

原英文标题的“未发布安全加固”是保留的历史措辞；实际已分发依赖由解析库存决定，不能仅看摘要或历史Gradle校验条目。APK内含 `assets/sbom.cdx.json` 与 `assets/licenses/`；恢复分发包含同样物料和顶层LICENSE/NOTICE。两者包含完整审查过的许可证与可取得的JAR声明。JCIP annotations为CC-BY-2.5，保留署名，不是公有领域。

已审查登记在 `licenses/dependencies.json`，备用文本来源在 `licenses/SOURCES.json`。新增运行时组件没有审查条目会使构建失败。

`generateReleaseMetadata` 记录实际JAR文件名、SHA256、分发范围，包括Guava单独选择的JVM二进制。发布检查核对所有外部恢复JAR摘要与打包库存。旧OkHttp/Okio已退出当前解析库存，`docs/third-party-notices/` 仅保留历史版本声明。

构建/测试工具：Gradle8.11.1、AGP8.9.2、Temurin17.0.18+8、SDK35/build-tools35.0.0、JUnit4.13.2；它们不在用户手机运行。Wrapper/工具链摘要、固定CI Action提交和独立验证工具哈希在tools。构建与公告扫描不等于对依赖的安全审计。

## BitNFC标识 0.4.0

Bitcoin字形路径不改坐标，复用自 [Bitcoin Design Bitcoin symbol SVG](https://bitcoin.design/assets/images/guide/getting-started/visual-language/bitcoin-symbol.svg)。源文件、SHA256和组合SVG在 `docs/design/logo/`；只做等比缩放/平移并添加NFC弧线。衍生标识不代表Bitcoin Design或Bitcoin Core背书。

## BitTap界面图标 内部版本8

新增17个图标复用本地Lucide1.8.0路径并转成本地Android向量，不在运行时下载图标。完整Lucide/Feather许可保留在 `docs/design/licenses/LUCIDE-LICENSE.txt` 及APK的 `lucide-license.txt`。原Bitcoin字形不变。
