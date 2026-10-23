[简体中文](README.zh-CN.md) · [English](README.md) · [文档目录 / Documentation](../docs/README.md)

# 独立验证

运行 `./gradlew :core:test` 后，在本地虚拟环境安装 Python `cryptography`，执行：

```sh
python tools/check_interop.py
python tools/check_addresses.py
python tools/check_bip38.py
```

这些脚本只读取 `core/build/` 下公开的已知标量夹具和 `core/src/test/resources/` 的旧CG1夹具，不读取NFC卡、用户钱包或用户备份。不要向夹具地址入金。结果写入 `docs/`。

## 发布来源门禁

`build_release.py` 要求工作区干净、精确提交，以及同仓库中实际验证job通过的CI运行。Actions须使用四个已审查的精确SHA、SHA强制策略和只读默认权限。工具验证GitHub artifact ZIP的SHA256及固定成员，比较独立unsigned APK和恢复ZIP，之后才用既有身份本地签名。Gradle不接收发行签名环境变量。工具不会自动发布或上传GitHub Release。

原英文文件保留了当时的私有仓库套餐限制说明：主分支保护不可用；来源门禁不能替代强制分支保护和可信源码审查。按所有者决定，审计协作者既有write权限暂留，待收到审计专用只读SSH公钥后替换。此处翻译历史说明，不把旧私有套餐限制视为当前公开仓库状态。
