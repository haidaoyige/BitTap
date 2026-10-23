[简体中文](UI-PROTOTYPE-ALIGNMENT.md) · [English](UI-PROTOTYPE-ALIGNMENT.en.md) · [文档目录 / Documentation](README.md)

# BitTap prototype alignment — build 8

Historical baseline: approved `docs/design/bittap-next-prototype.html`, top-bar language entry and balanced horizontal card. Build 7 retained functionality but replaced several visual groups/navigation with generic text/controls. Build 8 restored the prototype item by item.

| Page / interaction | Difference | Correction |
| --- | --- | --- |
| Home header | Orange text only; wrong brand sizing/spacing | Bordered language capsule, globe, selected language and chevron; consistent brand; fix negative-margin clipping |
| Home | Missing offline/floating-lock marks; different whitespace | Restore marks, card shadow, floating lock and title/illustration/button rhythm |
| Language sheet | Stacked generic buttons, text check, auto-dismiss | Current-language explanation, system subtitle, pale-orange selection, separate check, close/done; keep open after selection |
| About | Missing public-information card, wrong icons, extra technical footer | Restore shield card, prototype buttons, language/close; remove format footer |
| Key generation/encryption help | Links instead of accordions; comparison expanded; wrong ending/back path | Dedicated page, nine groups, first expanded, chevrons/dividers/emphasis/cleanup/comparison; both back controls return to About |
| Prepare a card | Plain paragraph and separate link | NFC icon and pale-orange card with inline link/arrow |
| Note | Missing optional marker/live counter | Right-aligned count, example and public-information hint; Unicode counting |
| Camera enhancement | Bare switch and scattered copy | White bordered mode card, camera icon, native switch, two explanatory levels and inline principles link |
| Passwords | Wrong strength order, extra bar, missing hint card | Strength after both inputs, remove extra bar, key hint card and approved generate-button order |
| Enhancement principles | Three plain paragraphs | Three icon steps, pale-orange caution and consistent confirm |
| Capture | Plain viewport, external count | Rounded viewport/frame, internal counter, privacy and capture/cancel order |
| Photo confirmation | Small sheet without photo/statistics | Dedicated matching-photo page, 32-frame/1-photo counts, use/retake; clear image on confirm/exit/background |
| Write/completion | Read-only, note status and progress grouped incorrectly | Mode card, irreversible warning, left/right note/status, three-step progress and action hierarchy |
| Read/decrypt | Old headings/copy | New wording/order, retaining real legacy/BIP38 compatibility notices |
| Other addresses / QR | Old copy/text arrow; wrong QR/risk styles | Four address rows, real chevron, full addresses, prototype QR sizing, risk card, copy/save/explorer |
| Explorer picker | Generic buttons and wrong return | Address/privacy cards, site rows/domains/external-link icons; return to original QR |
| NFC/help sheets | Inconsistent closes, icons, ring ratios/dividers | Consistent titles, close/icons/hierarchy; processing NFC cannot be interrupted by close/back at this historical stage |
| Read-only errors | Text only | Unsupported returns to write settings; uncertain lock explicitly reported and proceeds to backup while preserving unknown status |

Prototype simulated camera/scans/writes/success never enter production. Actual camera, permissions, I/O and file results determine the flow. Future-proposal copy follows actual implementation while retaining the information structure.

No change to scalar generation, scrypt, AES-GCM, CG3 bytes, address derivation or permanent-lock algorithm. Camera adds only in-memory photo preview; hashes/mixing still use the matched final frame. No photo files or Android page serialization; clear on use/lifecycle exit.

Native Android system bars, permission/file pickers, font rendering/scaling can differ from a web prototype. Overflow scrolls without hiding actions. Evidence: `docs/release/bittap-code8/`. Physical NFC locking/real-camera compatibility still require acceptance; visual alignment is not hardware or independent security approval.
