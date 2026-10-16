# 设计参考

`bittap-next-prototype.html` 为本轮确认的 BitTap 原型，含四语言、相机增强、公开钱包备注、免责声明、支持卡型与永久只读交互。内部版本 7 的 Android 实现及验证边界见 [本轮实现说明](../BITTAP-IMPLEMENTATION.md)。原型使用演示数据；其中“尚未实现”等设计阶段文字不代表当前构建状态，以实现说明与验证报告为准。

`bitnfc-v040-prototype.html` 为用户确认的 0.4.0 交互原型，包含展示用占位数据；不会生成或恢复真实私钥。实际系统逻辑以根目录 README 和 docs/FORMAT.md 为准。

`logo/` 保留原始 Bitcoin SVG、组合标识和路径来源哈希。Android 的 ic_launcher / ic_brand_white 逐字复用原 SVG 的 Bitcoin path，仅作等比缩放和平移，然后添加 NFC 弧线。

`original-prototype.html` 仅保留早期历史页面，不代表当前行为。

二维码弹窗与入口的新方案已确认并纳入内部版本 6，实施边界和验证见 [qr-dialog-proposal.md](qr-dialog-proposal.md)。
