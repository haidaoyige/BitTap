[简体中文](NFC-LOCK-DIAGNOSTIC-code48.md) · [English](NFC-LOCK-DIAGNOSTIC-code48.en.md) · [文档目录 / Documentation](../README.md)

# Code 48: permanent-lock failure diagnostics

2026-10-22. User clarified code 47 writes, then Confirm permanent lock while continuously attached reports “written, lock unconfirmed.” Equating this solely to toggling the option was incorrect: LOCK/VERIFY_LOCK exceptions/timeouts/cancel also show it, so it cannot prove hardware status.

No USB/phone thread or NFC-service stack was available. Adds diagnostics, no confirmed root cause/Xiaomi 15 Android 16 physical-lock acceptance.

## Gap and codes

Previously all LOCK/VERIFY_LOCK errors became one sheet. Retain wording/backup entry, add fixed localized stage/reason: `NFC-LOCK-stage-reason` or `NFC-VERIFY-stage-reason`.

| Stage | Step, including connect/close |
| --- | --- |
|0| Not yet in a specific step / controlled test |
|L1| Compare complete NDEF on just-written same card |
|L2| Check model, mirroring/access configuration |
|L3| NDEF permanent-read-only operation |
|L4| Hardware-lock helper |
|H1| Connect page-reading interface |
|H2| GET_VERSION / supported model |
|H3| Configuration/mirroring/access protection |
|H4| Read/set/write dynamic locks |
|H5| CFGLCK configuration lock |
|H6| Read/write static locks |
|V1| Final complete wallet NDEF comparison |
|V2| Fresh static/dynamic/CC/configuration locks |
|V3| NDEF discovery read-only state |

Reasons TIME timeout, LOST disconnected, BUSY worker unavailable, CANCEL, IO, CARD conditions, STATE lock/verify failed, OTHER. CARD/STATE/OTHER stay conservative and do not identify firmware faults. Enum stages/type-mapped reasons never display raw exception, UID/address/note/ciphertext/password/key. Memory only, no new files/network.

## Preserved decisions

Write and lock success separate; failed LOCK/VERIFY remains unknown. No lock masks/order/makeReadOnly/card matching/fresh comparison/final criteria changed in this step. No destructive trial write. NDEF read-only check not ignored to force success; cached discovery might affect it, without physical evidence it caused this failure. No automatic/unconfirmed repeat irreversible lock. Cancel/timeout/background/stale rules and backup remain; no crypto/KDF/generation/verification change.

## Validation/next steps

Units preserve stages/reject post cancel updates. Device substitutes inject each fault through production completion handler, check four languages/fixed codes/no raw exceptions/unknown/written count preserved/mandatory backup/style/FLAG_SECURE. External `BitTap-security-validation/code48-lock-diagnostic/validation.json`. No real RF; not Xiaomi/NTAG acceptance. Code 47 Android 15 visible-cancel timing instability remains open.

At this historical point no new stage had yet been reported. Report only fixed codes, never wallet contents; keep backup, never overwrite an existing wallet to diagnose. Stop repeated taps if system NFC hangs; need system recovery/device logs.
