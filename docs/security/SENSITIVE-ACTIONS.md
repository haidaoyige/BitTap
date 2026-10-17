# Android 9 私钥复制限制与遮挡保护

本轮仅实施用户确认的两项：Android 9 WIF 剪贴板限制、敏感操作被遮挡时的保护与提示。版本号仍为 0.5.0；未引入密码策略、地址核验流程或前台解锁期限的其他原型调整。

## 行为

### Android 9 / API 28

- 私钥二维码弹层显示四语言说明，WIF 复制按钮以锁图标和“不支持复制”呈禁用状态。
- 请求复制确认与实际写剪贴板的两个入口各自检查系统版本，先拒绝，再返回；不会先构造 WIF 或 ClipData 后才拒绝。
- 普通收款地址复制保持可用。私钥二维码、经确认的私钥图片保存保持原有功能。
- 私钥二维码生成失败时，不向 Android 9 用户建议使用已经禁用的复制功能。
- Android 10+ 保留已有 WIF 复制确认文案、准确读回验证、敏感标记及短时清理。不把 Android 10+ 描述为绝对安全的剪贴板。

### 遮挡保护

- 主页面和 AppUi 创建的所有底部弹窗共享 ProtectedScrollView。覆盖私钥显示、二维码、复制确认、图片保存确认、密码操作、覆盖写卡、永久只读确认等触摸入口。
- 检测到系统遮挡标记时，向原触摸目标发送 CANCEL，取消待处理输入并吞掉余下手势。即使 DOWN 正常、MOVE 或 UP 才出现遮挡，也不会触发该次点击。
- 下一次干净的 DOWN 可以重新操作，不把已经取消的手势恢复为点击。
- 主窗口、底部弹窗、处理提示窗均保留 FLAG_SECURE；Android 12+ 同时通过 HIDE_OVERLAY_WINDOWS 和 setHideOverlayWindows(true) 请求隐藏第三方非系统覆盖窗口。
- 拦截后沿用批准的橙色／暖白提示页，说明本次点击未执行。关闭秘密弹窗、清理本 App 持有的私钥缓冲区、临时二维码和待导出图片；从私钥页返回时进入密码页，不恢复明文或密码。
- 正在处理 NFC 或文件 I/O 时，拒绝新的被遮挡触摸并显示重试提示，不因提示而强行打断已在进行的外部写入。
- 相机流程被中断时停止采集、销毁临时照片与熵数据，并返回设置页重新采集，不在安全提示页背后继续采集。
- 界面提示支持中文、英文、日文、韩文；屏幕重建保持正确的安全返回目标，不序列化钱包或秘密到 Bundle。

## 验证

使用独立 Android 9 / API 28 与 Android 15 / API 35 ARM64 模拟器，不覆盖此前模拟器的正式签名 App，不使用真实资金。测试数据为公开标量 1 固定样本，不可用于存币。

具体构建、单元测试、lint 与设备断言结果见 [validation.json](sensitive-actions/validation.json) 及同目录原始日志。

- 对已安装 App 的真实 Android SDK 版本判断进行测试，未通过篡改 Build.VERSION 或生产后门伪造旧系统。
- Android 9：禁用按钮、程序化点击禁用按钮、直接请求确认、直接复制入口都不能写出 WIF；普通地址可复制，私钥二维码能解码为固定测试 WIF。
- Android 15：原有确认及真实剪贴板写入保留，敏感标记正确，活动及弹窗的隐藏覆盖窗口属性已设置。
- 对实际触摸分发合成完整／部分遮挡标记，验证整段手势被取消；分别覆盖 DOWN、MOVE、UP 与后续干净重试。此项验证应用对系统标记的响应，不冒充真实恶意覆盖应用渗透测试。
- 覆盖复制、私钥图片保存、覆盖写卡、永久锁卡确认，检查剪贴板不变、不启动文档导出、不产生待导出 PNG、不启用 NFC 写入／锁定状态。
- 四语言页面、返回密码页、旧复制／保存确认回调、旋转及私钥缓冲区清理均做设备断言。
- 既有 SmokeRunner 的永久动画空闲等待已改为有界等待并保留功能断言，避免扫描动画导致测试无限等待。
- 既有完整 SmokeRunner 覆盖地址／私钥二维码、四语言图片、文件读写、密码输入、备份与 NFC 提示的兼容性回归；模拟器 NFC 测试仅验证 UI 与状态，不声称完成实体卡射频操作。

复现命令（使用仓库约定工具链）：

```sh
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
adb -s <dedicated-emulator> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <dedicated-emulator> install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s <dedicated-emulator> shell am instrument -w -e protectionOnly true io.coingift.android.test/io.coingift.android.SmokeRunner
```

## 边界与未实施项

- Android 9 不提供“部分遮挡”标记；该系统只能响应完整遮挡标记。Android 10+ 响应完整及部分遮挡；Android 12+ 另加系统隐藏覆盖层能力。
- 不能阻止操作系统没有报告的遮挡，也不能保证防住 Root、进程注入、恶意输入法、所有高权限辅助功能或全部系统可信覆盖层。没有全面禁用无障碍，秘密语义读取防护仍需独立评审。
- 已复制／导出到外部的数据无法撤回，JVM 历史副本也不能承诺取证级擦除。
- 本轮不更改现有复制确认文案，不实施两分钟／空闲 60 秒锁定，不实施地址专属核验标签或“仅核验地址后立即清理私钥”流程。弱密码二次确认后来获用户批准，单独实施与验证见 [WEAK-PASSWORD-CONFIRMATION.md](WEAK-PASSWORD-CONFIRMATION.md)；本目录原始测试记录仍对应当时的两项修订。
- 此处只关闭 BTA-03 的 Android 9 WIF 剪贴板路径；BTA-04 本轮完成遮挡触摸与覆盖窗口防护，辅助功能语义部分仍未完成。

参考：[Android Tapjacking](https://developer.android.com/privacy-and-security/risks/tapjacking)、[Secure sensitive activities](https://developer.android.com/security/fraud-prevention/activities)。
