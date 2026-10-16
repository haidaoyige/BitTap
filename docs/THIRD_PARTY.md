# Third-party dependencies

Application source: Apache License 2.0 (see LICENSE). No OneKey code or assets used.

Pinned direct runtime dependencies:

- bitcoinj 0.16.5 — Apache-2.0, https://github.com/bitcoinj/bitcoinj
- Bouncy Castle bcprov-jdk15to18 1.83 — Bouncy Castle License (MIT-style), https://www.bouncycastle.org/licence.html
- ZXing core 3.5.3 — Apache-2.0, https://github.com/zxing/zxing
- SLF4J 1.7.36 — MIT, https://www.slf4j.org/license.html

bitcoinj transitive runtime dependencies include Guava, protobuf-javalite, OkHttp, Okio and associated annotation libraries. Their own license texts remain in the recovery distribution's dependency JARs. A resolved runtime dependency report and extracted license notices accompany the delivered build. Android package resource duplicate exclusion does not waive their license obligations.

Build/test tooling: Gradle 8.11.1, Android Gradle Plugin 8.9.2, JDK 17, Android SDK 35, JUnit 4.13.2. Build tools are separate from the app and do not run on the recipient's phone. Official download checksums are checked for the toolchain archives. Building does not constitute a security audit of these dependencies.

## BitNFC mark (0.4.0)

Bitcoin glyph path reused without coordinate changes from [Bitcoin Design's Bitcoin symbol SVG](https://bitcoin.design/assets/images/guide/getting-started/visual-language/bitcoin-symbol.svg). Original source, SHA-256 and combination SVGs are in `docs/design/logo/`. Only uniform scaling/translation and added NFC arcs are used. This derivative BitNFC mark does not imply endorsement by Bitcoin Design or Bitcoin Core.

## BitTap UI icons (internal build 8)

The 17 additional interface icons reuse Lucide 1.8.0 paths from the bundled Lucide package, converted to local Android vectors. No runtime icon download is used. Full Lucide / Feather license notices are retained in `docs/design/licenses/LUCIDE-LICENSE.txt` and the APK asset `lucide-license.txt`. Original Bitcoin glyph paths are unchanged.
