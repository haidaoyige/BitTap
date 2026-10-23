[简体中文](address-image-export.md) · [English](address-image-export.en.md) · [文档目录 / Documentation](../README.md)

# Verification warning and transparent rounded corners in exported images

2026-10-18: included in local 0.6.0/code 28 APK, no new GitHub Release then.

- Pending-address save still requires consent. The image then says the address was not checked against the private key at export; decrypt/verify in BitTap before funding. No duplicate status badge.
- Four app languages; `YOUR KEYS. YOUR BITCOIN.` stays English.
- Export uses the current QR's address/status, never wallet/note status. Existing QR owner/session/pixel-object checks prevent stale consent exporting another QR.
- Verified images retain content. Pending images move QR/full address/footer down according to actual wrapped warning height.
- Address and WIF images are RGBA PNG: transparent outside rounded corners, warm-white inside, black/white QR modules and four-module quiet zone. Extension/MIME/readback unchanged.
- Transparency applies to corners, not the entire image. Galleries/chat apps can add backgrounds or flatten transparency on re-encoding.
- Status describes export time only; images can be edited. Warning neither prevents tampering nor authenticates address/key.

Tests: `SmokeRunner -e imageOnly true` for image/PNG/alpha/light-dark scan/four languages/types; `-e addressSaveOnly true` for actual entry/status/cancel/stale consent. Public scalar fixtures only.

Local results: API35 image 419 and save 276 assertions, 91 unit tests passed; release lint no errors, existing warnings retained. Actual PNG width 1224, 8-bit RGBA, corners alpha 0 retained after encoding/readback. Limited to that local build, not independent reaudit or actual gallery/chat reprocessing acceptance.
