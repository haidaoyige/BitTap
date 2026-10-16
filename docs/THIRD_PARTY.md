# Third-party dependencies

Application source: Apache License 2.0 (see LICENSE). No OneKey code or assets used.

## Current runtime baseline (unreleased security hardening)

- bitcoinj 0.17.1 — Apache-2.0, https://github.com/bitcoinj/bitcoinj
- Bouncy Castle bcprov-jdk15to18 1.86 — Bouncy Castle License, https://www.bouncycastle.org/licence.html
- Guava 33.7.2 (Android/JRE variants) — Apache-2.0, https://github.com/google/guava
- protobuf-javalite 4.36.2 — BSD-3-Clause, https://github.com/protocolbuffers/protobuf
- ZXing core 3.5.3 — Apache-2.0, https://github.com/zxing/zxing
- SLF4J 2.0.17 — MIT, https://www.slf4j.org/license.html

The resolved runtime inventory, rather than this summary or historical Gradle
verification entries, determines shipped dependencies. The APK embeds
`assets/sbom.cdx.json` and `assets/licenses/`; the offline recovery distribution
contains the same SBOM/license bundle plus top-level LICENSE and NOTICE. Both
include full reviewed license texts and available dependency-JAR notices.
JCIP annotations are CC-BY-2.5 (attribution retained), not public domain.
The reviewed source registry is `licenses/dependencies.json`; provenance for
vendored fallback texts is in `licenses/SOURCES.json`. A build fails for a new
runtime component without a reviewed license entry.

`generateReleaseMetadata` records actual JAR filenames, SHA-256 and distribution
scopes, including Guava's separately selected JVM binary. Release checks compare
all external recovery JAR hashes with the packaged inventory. Old OkHttp/Okio
versions are no longer part of the resolved runtime; historical notices under
`docs/third-party-notices/` are retained solely for prior releases.

Build/test tooling: Gradle 8.11.1, Android Gradle Plugin 8.9.2, Temurin
17.0.18+8, Android SDK 35 / build-tools 35.0.0, JUnit 4.13.2. Build tools do not
run on the recipient's phone. Wrapper/toolchain hashes, pinned CI action commits
and hashed independent verification dependencies are in `tools/`. Building and
an advisory scan do not constitute a security audit of these dependencies.

## BitNFC mark (0.4.0)

Bitcoin glyph path reused without coordinate changes from [Bitcoin Design's Bitcoin symbol SVG](https://bitcoin.design/assets/images/guide/getting-started/visual-language/bitcoin-symbol.svg). Original source, SHA-256 and combination SVGs are in `docs/design/logo/`. Only uniform scaling/translation and added NFC arcs are used. This derivative BitNFC mark does not imply endorsement by Bitcoin Design or Bitcoin Core.

## BitTap UI icons (internal build 8)

The 17 additional interface icons reuse Lucide 1.8.0 paths from the bundled Lucide package, converted to local Android vectors. No runtime icon download is used. Full Lucide / Feather license notices are retained in `docs/design/licenses/LUCIDE-LICENSE.txt` and the APK asset `lucide-license.txt`. Original Bitcoin glyph paths are unchanged.
