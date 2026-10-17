# 待核验收款地址保存前确认

按用户最终选择，保留原有收款地址 PNG 样式，不在图片上新增标记或警告。风险提醒放在点击“保存图片”之后、生成 PNG 和打开系统文件选择器之前。

- 待核验地址：提示地址来自卡片或备份中的公开信息，尚未与私钥核对；篡改或密钥不匹配可能导致转入的 BTC 损失。说明保存图片本身不会核验地址。
- “我理解风险，继续保存”允许继续保存；取消、关闭弹窗均不生成 PNG、不打开文件选择器，原地址二维码仍可查看。每次新的待核验地址保存请求都需要确认。
- 已核验地址沿用直接保存。私钥二维码仍保留原有明文私钥保存提示与文案。
- 提示支持中文、英文、日文、韩文，沿用当前底部弹窗风格、安全窗口及遮挡触摸保护。
- 同意绑定当前二维码弹窗、二维码位图和会话。更换地址、关闭二维码、切后台或遮挡拦截后，旧同意按钮不能继续授权导出。连续点击不会叠加确认或重复打开选择器。
- 文件写入、关闭、逐字节读回校验及取消／超时清理沿用原有逻辑；没有新建文件缓存，也不改变地址核验状态。

## 验证

`SmokeRunner -e addressSaveOnly true` 覆盖四语言 × 四类地址，取消与同意、原图片像素及二维码一致性、实际文件写入与读回、已核验地址直接保存、旧确认回调、切后台及遮挡拒绝。只使用公开测试夹具，系统文件选择器启动被测试监视器拦截，再以真实测试文件 URI 验证结果处理路径。

```sh
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
adb -s <dedicated-emulator> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <dedicated-emulator> install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s <dedicated-emulator> shell am instrument -w -e addressSaveOnly true io.coingift.android.test/io.coingift.android.SmokeRunner
adb -s <dedicated-emulator> shell am instrument -w io.coingift.android.test/io.coingift.android.SmokeRunner
```

原始日志与结果见 [address-save-consent/validation.json](address-save-consent/validation.json)。提示不验证地址、不阻止用户明确同意后保存待核验地址，也不保证用户不会向未经核验的地址转账。本次不修改图片内容、二维码编码、密文格式或其他未批准的地址核验流程。
