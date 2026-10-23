[简体中文](FORMAT.zh-CN.md) · [English](FORMAT.md) · [文档目录 / Documentation](README.md)

# BitTap 加密备份：CG3、CG2 与旧 CG1

CG3 是项目自有格式。内部版本 code39 起仅接受 scrypt `(N=32768,r=8,p=3)`；早期 p=1 测试卡/文件明确拒绝，不提供迁移。CG2/CG1 布局也只接受当前参数。它**不是 BIP38 或通用钱包导入标准**。请随备份保留规范和离线恢复工具。NFC 是可读取、复制的 NDEF 存储，不是安全元件。

文本为 `CG3:`（旧版 `CG2:` / `CG1:`）加规范、无填充的 Base64URL。前缀/版本不符拒绝，不获取外部 URL、密钥或参数。

## 公共固定头

| 偏移 | 字节数 | 字段 |
| --- | --- | --- |
| 0 | 3 | ASCII `CGF` |
| 3 | 1 | CG3=`0x03`，CG2=`0x02`，CG1=`0x01` |
| 4 | 1 | 网络 `0x00`，仅比特币主网 |
| 5 | 1 | 密钥配置 `0x01`：原始标量、压缩 secp256k1 公钥，默认 P2WPKH |
| 6 | 1 | KDF/加密配置 `0x01`：scrypt + AES-256-GCM |
| 7 | 1 | log2(N)=`15`，N=32768 |
| 8 | 1 | r=`8` |
| 9 | 1 | p=`3` |
| 10 | 16 | 新随机盐 |
| 26 | 12 | 新随机 GCM nonce |

KDF 前严格校验白名单。未知版本/网络/类型、重复或乱序条目、多余字节、非规范编码、地址校验和或长度错误均拒绝。

## CG2 / CG3 公开地址区

偏移38为地址数量，固定 `0x04`。从39开始，每条为一字节类型、一字节 ASCII 长度及地址；顺序严格如下：

| 类型 | 地址 | 长度 |
| --- | --- | --- |
| `0x01` | 原生 SegWit / P2WPKH，小写 Bech32 v0、20字节程序 | 42 |
| `0x02` | Legacy / P2PKH，主网 `1…` Base58Check | 26–35 |
| `0x03` | P2SH-P2WPKH，主网 `3…` Base58Check | 26–35 |
| `0x04` | BIP341 key-only P2TR，小写 Bech32m v1、32字节程序 | 62 |

CG3 再加一字节 UTF-8 备注长度（0–64）及内容。备注 Unicode NFC 规范化，最多16码点/64字节；允许文字、数字、组合标记和白名单普通标点，拒绝控制符、双向格式符、换行、emoji。备注公开，解密前未核验，不参与密码、密钥派生或文件名。

末尾固定48字节：32字节加密原始标量和16字节 GCM 标签。**全部头部，包括完整地址区和 CG3 备注长度/内容，均为 AAD。** 无密码可读，但认证前标为“待解密核验”。

GCM 成功后校验标量，独立推导**四个**地址并逐个比较。任何不符即拒绝并关闭临时明文持有对象；认证或地址校验失败绝不释放 WIF。

CG2 文本最多363 ASCII字节，通常360。CG3 加备注长度及最多64字节，最多334原始字节/450文本字节。当前27字节 MIME 加封装后最多483 NDEF字节。每次按**实际编码消息**检查可用 NDEF容量；优先 NTAG216，NTAG215 必须实测容量。NTAG213 放不下。芯片用户内存不等于可用NDEF容量。

## 旧 CG1

公共头版本1；偏移38为42，39–80存原生 P2WPKH，81–128为密文/标签，AAD=0–80。文本固定176 ASCII字节，不再新建。CG1 测试夹具采用当前p=3，不证明兼容早期p=1备份。

解密前仅提供原生 SegWit 声明；恢复后可推导四地址。独立 BIP38 字符串不含公开四地址元数据。

## 加密、密码与 WIF

密码 Unicode NFC规范化、UTF-8编码，保留大小写和空白。新建要求10–1024个可见ASCII字符，包含大小写、数字、标点；拒绝中文、emoji、空白和非ASCII。导入/解密允许1–1024字符及标准BIP38口令，但不恢复废弃KDF参数。10字符不证明熵足够；易猜密码仍明确警告。

`scrypt(passwordUtf8, salt, N=32768, r=8, p=3, dkLen=32)` 派生32字节，名义内存约32MiB另加实现开销。AES-256-GCM 加密32字节标量，128位标签；每次新随机盐/nonce。平台 SecureRandom 提供随机性，拒绝采样仅接收 `[1,n-1]`，无取模偏差，随机源错误即停止。

私钥导出是**主网压缩 WIF**：`0x80 || originalScalar32 || 0x01` 的 Base58Check。WIF 不含地址/脚本类型；CG1/2/3须先由 BitTap 或 CLI 解密后导入其他钱包。

## 地址推导

压缩公钥K：P2PKH=HASH160(K)；P2SH-P2WPKH对v0 P2WPKH赎回脚本哈希；原生P2WPKH将HASH160(K)编码成Bech32 v0。P2TR按BIP341无脚本树构造：内部点P归一化为偶Y；`t=int(taggedHash("TapTweak", x(P)))`，t>=n拒绝；`Q=P+tG`，无穷点拒绝；x(Q)编码为v1 32字节Bech32m。这是独立单私钥，不是HD/BIP86路径钱包。

原标量k恢复Taproot花费标量：`q=((k if kG has even Y else n-k)+t) mod n`，签名遵循BIP340奇偶处理。备份加密k而非q。普通WIF导入不保证相同Taproot构造，务必核对目标钱包能恢复实际存币地址。BitTap不签名、转账或查余额。

参考：[BIP341](https://github.com/bitcoin/bips/blob/master/bip-0341.mediawiki)、[BIP350](https://github.com/bitcoin/bips/blob/master/bip-0350.mediawiki)、[BIP341向量](https://github.com/bitcoin/bips/blob/master/bip-0341/wallet-test-vectors.json)、[BIP86示例](https://github.com/bitcoin/bips/blob/master/bip-0086.mediawiki)。

## NFC、备份与兼容

新写单条 `application/vnd.bittap.card` MIME，载荷为完整ASCII CG3。读入还接受 `application/vnd.bitnfc.card`、`application/vnd.coingift.card` 及承载CG3/2/1/BIP38的UTF-8 NDEF Text。多条已识别密钥记录拒绝；任意URI不触发跳转。

写前检查可写、实际容量、UID、旧NDEF。已有数据须确认覆盖；第二次贴卡UID和原内容均不变才写。UID仅操作保护，不是身份认证。写后重新NFC读取并比较完整编码NDEF，不自动格式化。永久只读默认关，仅识别的NTAG215/216布局支持。完整写读核对、单独不可逆确认后锁用户区/CC/配置；后续贴卡核对同一完整NDEF及硬件锁位才成功。中断/部分锁定报未知，不防复制、模拟或物理替换；实体卡仍须验证。中断写入不算已确认。

另一张卡获得**完全相同的密文**、地址和密码，不生成新私钥。会话内按观察到的不同UID计数，不证明硬件不可克隆。

制作完成须经系统文档提供程序保存备份、成功关闭、重新打开逐字节核对。取消、关闭失败、读回失败或仅复制文本均不算保存。配置变化仅内存保留密文/公开状态，不写savedInstanceState。暂停清密码/明文，首页清自有会话/剪贴板；导出文件保留。进程死亡、强停、卸载、最近任务移除丢弃未完成内存状态；先备份。无法承诺Java/系统副本取证级擦除。

BIP38仅导入主网压缩密钥，保留标准P2PKH核验；未压缩记录拒绝，成功恢复后推导四地址。

## 可选相机混合 v1

相机补充原平台SecureRandom。采集前取32字节秘密R；Camera2哈希32个不同YUV帧及一张按传感器时间戳匹配的最终照片。不调用外部相机/写文件。取消、后台、采集或随机失败均停止清理；回普通模式须明确选择。

设 f[i] 与 photo 为SHA-256摘要：

```text
C = SHA256(ASCII("BitTap/camera/v1") || 0x20 || concat(i=0..31, 0x00 || uint8(i) || 0x20 || f[i]) || 0x01 || 0x20 || photo)
for i=0..127:
  candidate = HMAC-SHA256(key=R, message=ASCII("BitTap/private-key/v1") || 0x20 || C || uint32be(i))
```

首个 `[1,n-1]` 候选通过，不取模，否则失败。哈希不创造熵，不声称独立量化了相机熵。被入侵手机可窃取输入或输出。

自有R、摘要、候选、明文在使用/取消/后台清理；JVM、加密提供者、相机服务、系统可能保留不可控副本，不是取证级擦除。密文在活跃内存保留到强制备份并回首页，以支持多卡；用户导出文件/图片保留。
