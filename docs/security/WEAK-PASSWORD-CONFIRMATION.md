# 生成钱包前的弱密码提醒

按用户确认的原型增加风险告知，版本号仍为 0.5.0。仅在创建钱包时增加一次当次输入的明确确认，不更改原有 WIF 复制确认。

## 行为

- 点击“生成冷钱包”后，先校验密码最低长度、两次输入一致、钱包备注及相机增强采集状态。无效输入沿用原有错误提示，不先弹出弱密码确认。
- 沿用 `PasswordStrength` 的现有判定；弱密码显示橙色／暖白底部弹窗，中等、较强密码继续原有生成流程。强度分级是启发式建议，不代表密码熵或不可破解承诺。
- 弹窗明确说明：卡片或备份副本可被拿到其他设备上离线猜测密码，App 内的次数限制不能阻止；相机随机性增强不能弥补弱密码。标准模式和相机增强模式都显示这一说明。
- 主按钮“返回，设置更强的密码”关闭弹窗并聚焦密码输入；次按钮“我理解风险，仍使用此密码”才开始生成、加密及恢复自检。取消、系统返回或点击外侧均不生成钱包。
- 没有“以后不再提醒”选项，也不持久化同意。每次新的弱密码生成请求重新确认。
- 弹窗支持中文、英文、日文、韩文，跟随 App 语言；复用现有安全窗口、遮挡触摸保护与可滚动底部弹窗。

## 数据与生命周期

- 提醒出现前不调用私钥生成，不将密码交给后台工作线程。用于校验与强度判定的临时 `char[]` 在显示弹窗前清零；弹窗不复制密码到文案、日志或持久存储。
- 同意只绑定当前弹窗、页面、会话以及输入修订号。密码、确认密码、备注或增强来源改变时，旧同意失效，需重新提交。
- 继续时重新读取并校验输入。生成后沿用现有输入清理、后台密码数组清理及相机熵消费／销毁流程；取消提醒不提前消费已采集的相机熵。
- 切后台、页面重建、返回首页及遮挡拦截关闭提醒，旧按钮回调不能重新授权生成。密码输入生命周期清理沿用原有实现。

## 验证

测试入口为 `WeakPasswordProbe`，通过真实 Android Activity、弹窗和生成／解密链路验证，不只测试文字是否存在。使用专用模拟器及公开测试密码，无真实 BTC。

```sh
./gradlew :core:test :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest :app:lintDebug
adb -s <dedicated-emulator> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <dedicated-emulator> install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s <dedicated-emulator> shell am instrument -w -e weakPasswordOnly true io.coingift.android.test/io.coingift.android.SmokeRunner
```

本次验证结果与原始日志记录在 [weak-password/validation.json](weak-password/validation.json)。设备截图仅由测试包在专用模拟器中绘制当前风险提醒视图，生产安全窗口标记保持开启，不捕获真实私钥。

## 边界

仍允许弱密码，提醒不会提高弱密码本身的抗猜测能力，也不关闭审计项 BTA-01 的剩余风险。最初加入提醒时未修改密码最低长度；用户随后要求新建密码至少 10 位，现已在核心生成入口强制执行，后续验证另见 [PASSWORD-MINIMUM-10.md](PASSWORD-MINIMUM-10.md)。本目录原始验证记录保留为当时版本的证据。KDF、加密格式、安卓随机源或相机混熵算法未改变。本次不实施解锁自动超时、地址核验新流程或随机密码生成器。
