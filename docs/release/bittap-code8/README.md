# BitTap 0.4.0 / 8 原型对齐候选包

2026-10-16。对照用户确认的顶栏语言入口方案和横排平衡卡片，逐项修正原生页面。完整范围见 [对齐明细](../../UI-PROTOTYPE-ALIGNMENT.md)。

候选包：`app/build/outputs/bittap/BitTap-0.4.0-code8-candidate.apk`。沿用原发行签名，应用 ID 仍为 `io.coingift.android`，可覆盖原同签名发行包；版本名 0.4.0，内部版本 8。

验证记录：

- 核心 62 + Android 14 = 76 项单元测试，零失败；逐项 XML 与 UNIT-RESULTS.json 留档。
- 最终交互结果见 DEVICE-SMOKE-RESULT.txt，包含原型语言入口与选择后保持弹层、关于、九组安全原理折叠与返回、备注计数、增强模式分组，以及原有文件备份、多卡、二维码解码与清理流程。
- 最终 Camera2 结果见 CAMERA-RESULT.txt：真实调用模拟器相机、匹配最终照片时间戳、显示内存照片、确认使用后回收照片并生成 CG3 钱包。相机摘要 / 混合算法不变。
- 曾在同一轮测试中遇到模拟器最终画面重复，生产校验拒绝继续，未生成钱包；见 CAMERA-REPEATED-IMAGE-RESULT.txt。测试允许有限次通过「重试」重新采集，不能跳过重复检查，也不会自动退回标准模式。最终通过不意味着已测得相机额外熵值。
- Release lint 无问题，生产 APK 不可调试，run-as 被拒绝。仅 NFC、CAMERA 权限，无 INTERNET / 存储权限；不含 instrumentation Runner、测试密码或公开标量夹具。
- 原发行证书 SHA-256：`58b6613e32239ddf229066fa6bdf1b77579ade691a5d29b5bf621341a3c2b85a`。APK 哈希见 SHA256SUMS.txt。
- 图片来自最终候选包的空钱包界面或公开测试夹具，不是重新绘制的效果图。不要给测试地址充值。

实际页面：

![实际页面预览](screenshots/preview.png)

[首页](screenshots/aligned-home.png) · [语言选择](screenshots/aligned-language.png) · [关于](screenshots/aligned-about.png) · [安全原理](screenshots/aligned-security.png) · [折叠比较](screenshots/aligned-security-comparison.png) · [标准制作](screenshots/aligned-setup-standard.png) · [增强制作](screenshots/aligned-setup-enhanced.png)

系统字体、状态栏和系统权限 / 文件选择器由 Android 决定；长页面允许滚动，不以隐藏内容适配高度。实体 NFC 锁卡、跨设备无法覆盖以及真实手机相机兼容性仍待实机验收；此记录不是独立资金安全审计。
