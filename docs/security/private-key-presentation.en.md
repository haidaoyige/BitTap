[简体中文](private-key-presentation.md) · [English](private-key-presentation.en.md) · [文档目录 / Documentation](../README.md)

# BTA-04: private-key presentation and accessibility

Historical code 28 record. Code 40 added session wrapping and an all-accessibility-services gate: see [current step](BTA-04-code40.en.md).

2026-10-18 local 0.6.0/code 28 addressed ordinary text extraction after viewing WIF, not compromised-system protection.

## Implementation

- Replace only key text with PrivateKeyView drawing StaticLayout; never assign real WIF to displayed TextView, descriptions, hints or accessibility nodes.
- An empty detached TextView containing no key obtains theme size/typeface/line spacing/paint to preserve pixels. Owned char[] supplies key characters.
- Accessibility gets only localized “sensitive private-key content.” Copy/save/transfer/cancel/close and ordinary instructions unchanged/readable.
- Disable view state/autofill; Android 11+ content-capture exclusion; Android 14+ sensitive-accessibility flag for drawn key/QR.
- Existing secure flag, obscured-touch rejection, idle 60/absolute 120 and lifecycle cleanup remain. Close/detach/session cleanup wipes chars/releases layout.

## Verification

Android 9/API28 and 15/API35: four languages, three widths/two font scales match old text pixels; full private sheet pixel-identical in four languages. Separate test accessibility service requests nonimportant views/window content and declares accessibility-tool status: reads normal UI but not fixture WIF/events. Positive control putting same public WIF back in ordinary TextView is readable, ruling out disconnected/invisible/secure-flag false negatives. Copy/save/reopen/background checked; API35=85, API28=72 assertions. Test service/provider/runner/public key fixtures excluded from delivered APK.

## Limits

important For Accessibility is not a security boundary; this does not hide ordinary WIF text merely by marking unimportant. Sensitive flags have OS/service-category limits; no-WIF drawn semantics provides the additional foundation. Authorized malicious accessibility may still operate buttons, and explicit copy/export exposes plaintext. This historical change did not ban all accessibility. Plaintext still enters process memory; wiping owned chars cannot erase all system/font/string/render copies. Root/injection/compromised OS not guaranteed. Local checks are not independent reaudit or acceptance for every ROM/service combination.
