[简体中文](random-password.md) · [English](random-password.en.md) · [文档目录 / Documentation](../README.md)

# Historical feature: locally generated strong passwords

**Removed in development code 32 on 2026-10-20** after device feedback: generation, copy, regenerate and saved confirmation removed. Users manually enter/confirm. Below records released v0.6.2/code 31, not current behavior.

Opt-in 32-character passwords sampled uniformly via SecureRandom.nextInt(94) over U+0021–U+007E. Reject/resample whole string unless uppercase/lowercase/digit/punctuation all occur, at most 128 tries. RNG failures stop; no time/device/pseudorandom fallback or fixed-position patching.

Caller-owned char[] wiped immediately after filling EditText. No preferences/log/card/ordinary-file/cloud storage or recovery. Explicit copy exposes plaintext to system clipboard; strings/system copies cannot all be promised erased.

## Interaction

First input shows full monospaced wrapped value, no horizontal scroll/ellipsis; visual wrapping adds no actual newline. Confirmation empty. Generated state shows copy, loss warning and safely-saved check box; matching confirmation and check box required, rechecked at actual generation. Editing first field leaves generated state/revokes check box. Regeneration asks before replacement, clears confirmation/check box and tries clearing owned old clipboard. Cancel/background/generate use existing cleanup, persist neither password nor check box. Switching apps to store password closes sheet; reenter stored password on return. All four languages wrap labels/copy/buttons/check box. Unlock/backup format unchanged.

## Copy

Check current sheet/input/foreground/not busy; sensitive clipboard mark and exact label/content readback, otherwise explicit error. Historical Android 9 refused copy/manual recording; 10+ allowed. Try clearing owned clipboard at 120 seconds and on cancel/regenerate/complete; if background access denied, retry in foreground. Never clear newer other-app content. No erasure guarantee for external apps/keyboards/system history.

## Validation

Core covers range, full-string retry, RNG error/stall fail-stop, production sampler/cleanup. Device covers four languages/scaled wraps, readback/sensitive marking, paste/confirmation/save gate/regenerate/keep/revoke/stale clicks, real generated-password encryption/recovery of an unfunded temporary wallet. No generated passwords/test random keys saved.

2026-10-19: 99 units no fail/error/skip, Debug/instrumentation builds, lint 0 errors/9 existing warnings. API35=385, API28=377 creation assertions; latter actual copy prohibition/manual confirmation. Both four languages, 16/24 sp full 32 characters/no truncation. English native screenshots public fixed passwords only. Evidence outside repo `BitTap-security-validation/random-password/`. Not entropy certification/funds guarantee.
