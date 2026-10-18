# “私钥如何生成与加密”内容核对与翻译

2026-10-18，code28 APK 交付后的源码修改；已交付的 code28 APK 不包含本次文案更新。

## 核对结果与内容修订

原有 Android 安全随机源、相机补充输入、scrypt、AES-256-GCM、生成后恢复核对及写卡读回的核心说明仍符合当前实现，保留这些解释。补充或修订：

- 相机增强收集 32 个有效预览帧与最终图像摘要，系统随机输入在采集前取得，再用 HMAC-SHA256 混合；不声称照片替代系统随机源或已证明增加固定的安全位数。
- 新建密码至少 10 个字符，并同时包含英文大写、小写、数字及特殊字符；格式合格不等于难猜。
- 地址与钱包备注不加密，成功解密前尚未认证；AES-GCM 的完整性保护不使公开字段保密。
- 未核验地址的导出图保留风险提示，但提示本身不执行地址核验。
- 解锁会话最长 2 分钟、60 秒空闲锁定，以较早者为准；切后台或回首页也清理解锁会话。
- 新私钥展示使用专用绘制，不把 WIF 作为普通辅助功能文本提供；明确复制或导出仍会产生明文材料。
- 保留生成后、进入写卡前清理明文，以及备份完成返回首页后清理制作会话的时点。清理本应用持有的缓冲或引用不等于证明所有系统副本彻底擦除。
- 将过时的“本版本真实手机相机尚待验收”改为明确相机输入依赖硬件与系统，不将实机流程验证说成熵认证。

## 翻译与界面接入

`approved-copy.json` 的 `securityContent` 现在分别提供 zh、en、ja、ko。每种语言包含完整标题、简介、清理提示、8 个说明分组的 25 段正文、开源钱包对比分组及兼容性／安全边界说明。

`ApprovedCopy.security()` 按 App 已解析的语言读取对应正文。`HelpPages.security()` 的清理提示与对比分组标题也使用同一内容源，移除过时的“正文仅中文”提示。保留原有 9 个折叠分组、默认只展开第一个、展开／收起和返回“关于”的交互。不修改加解密、私钥处理、NFC 格式或其他页面文案。

## 核对的本地实现

- `WalletCrypto`：安全随机源、私钥范围拒绝采样、scrypt N=32768/r=8/p=1、32 字节派生密钥、16 字节盐、12 字节 GCM nonce、128 位认证标签、公开头部 AAD、解密后地址核对及缓冲清理。
- `CameraCapture` / `CameraEntropy`：有效图像数据摘要、32 个不同帧摘要、最终图像摘要、预先取得系统输入的 HMAC-SHA256 混合、失败不冒充增强成功。
- `NewPasswordPolicy`、`MainActivity`、`UnlockDeadline`、`PrivateKeyView`：密码要求、制作／恢复生命周期、60/120 秒期限、专用绘制与退出清理。

## 开源项目对比的来源

以下公开源码于本次核对时查阅，对比只说明思路，不主张代码、安全性或文件格式相同，也不代表这些项目背书。

- [Bitcoin Core：wallet/crypter.cpp](https://github.com/bitcoin/bitcoin/blob/master/src/wallet/crypter.cpp)、[key.cpp](https://github.com/bitcoin/bitcoin/blob/master/src/key.cpp)：可见密钥生成及钱包 AES-256-CBC 加解密实现，与 BitTap 的实现不同。
- [Electrum：crypto.py](https://github.com/spesmilo/electrum/blob/master/electrum/crypto.py)、[storage.py](https://github.com/spesmilo/electrum/blob/master/electrum/storage.py)：密码保护的材料使用 AES-CBC、32 字节密码派生结果，钱包文件还有 ECIES 层，不能把其钱包文件格式说成 BitTap CG3。
- [SeedSigner：tools_views.py](https://github.com/SeedSigner/seedsigner/blob/dev/src/seedsigner/views/tools_views.py)：相机熵流程使用预览帧与最终照片。其处理流程不同于 BitTap 保留 Android 系统随机源的 HMAC 混合。

核对不改动既有开源许可、免责声明正文或其他页面的多语言文案。

## 验证

API35 模拟器实际页面专项 271 项通过，覆盖四语言正文、提示、各分组标题、每一段内容、展开／收起与返回“关于”的导航；同时确认其他已批准文案与 code28 APK 中保持一致。91 项单元测试通过，release lint 无错误（保留既有警告）。已核对外语页面截图的换行与排版，本次未重新交付 APK。
