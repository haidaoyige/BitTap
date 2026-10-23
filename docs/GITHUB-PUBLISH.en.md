[简体中文](GITHUB-PUBLISH.md) · [English](GITHUB-PUBLISH.en.md) · [文档目录 / Documentation](README.md)

# Current release 0.7.0

`haidaoyige/BitTap` is public; Apache-2.0 is unchanged. The main APK and recovery ZIP were verified against independent CI for the same commit and signed locally with the original certificate before upload. The release key stays local. [Release and verification](release/README.en.md) defines the current process; the following sections are historical.

# GitHub publication contents

Source includes the Android app, cryptographic core, offline recovery tool, Gradle Wrapper and dependency-verification files. Documentation includes usage, the CG1/NFC specification, changelog, test reports, third-party licenses and independent verification scripts.

The repository excludes real private keys, user passwords, real card backups, signing private keys, local SDK paths, caches and build directories. Source fixtures use public standard test keys/addresses: never fund them.

V0.5.0 was published to the separate private `haidaoyige/BitTap` repository without copying the old repository history. Release assets were `BitTap-v0.5.0.apk`, `BitTap-v0.5.0-offline-recovery.zip` and `SHA256SUMS.txt`; the matching tag supplied source. The APK retained the existing signing identity; the maintainer kept the key separately offline. Machine paths, caches, local-dev and release credentials were excluded.

For the first build install JDK17, Android SDK35/build-tools35.0.0 and configure local `local.properties`. See the root README.
