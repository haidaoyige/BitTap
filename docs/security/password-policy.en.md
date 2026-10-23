[简体中文](password-policy.md) · [English](password-policy.en.md) · [文档目录 / Documentation](../README.md)

# Code 39: encryption cost and internal-test compatibility

Creation/decryption now use scrypt N32768/r 8/p=3. By user decision earlier p=1 internal-test cards/backups are explicitly rejected. BIP38 uses its own standard parameters. Older dated sections below are historical.

# New-wallet password validation

Validation/strength and KDF share Unicode NFC normalization. The original normalization change did not alter format/KDF or add old-card branches.

- Visible ASCII only: A–Z, a–z, 0–9 and nonalphanumeric punctuation within U+0021–U+007E. 10–1024 characters with all four categories.
- Reject Chinese/Japanese/Korean, accented/full width letters, emoji, whitespace, newlines/invisibles. Check raw range before normalization/length/format/strength so non-ASCII characters such as Kelvin sign cannot normalize into accepted ASCII. Allowed ASCII is NFC-stable.
- Five rules update live; unsupported characters get explicit errors. Enable generation only when all pass and confirmation matches. Typing/pasting never silently strips/replaces. Keyboard submit/core generation enforce validation.
- Four languages. Format validity does not prove strength; obvious weak/repeated patterns retain warning, which cannot bypass format.
- Validator retains no input; already-NFC input is checked directly to avoid immutable strings on every edit. Java normalization may create non-wipe able strings; no claim all JVM copies erased. Existing exit/background/rotation/generation cleanup remains.
- Same core normalization serves format, strength and KDF. Recovery does not impose new-creation rules; no compatibility branch added for this change.

Later development accepts only manually entered/confirmed passwords, without random generation/copy/save check box. Character/strength rules unchanged.

## 2026-10-19 strict-range validation

96 core/app/recovery unit tests, no failures/errors/skips; Debug/instrumentation builds and lint pass (0 errors, 9 existing warnings). Exhaustive U+0000–U+10FFFF range: only U+0021–U+007E combined with an otherwise valid password passes. Check explicit error, unchanged input, ordinary/camera entry refusal before RNG.

API35 four-language creation 297 assertions: Chinese/emoji/full width/space/zero-width/newline/NUL refusal, disabled button, keyboard-submit rejection, real clipboard paste, localized errors, actual compliant ASCII generate/decrypt. Public passwords/unfunded one-use wallets only. External evidence `BitTap-security-validation/password-characters/`.

## 2026-10-19 normalization consistency before strict ASCII

94 unit tests, no failures/errors/skips, Debug/lint pass. Cases: raw 10→normalized 9 refused; canonically equivalent 10-code-point passwords; composition changing category; supplementary symbols counted once; both generation paths reject before RNG; equivalent-password recovery. No layout/consent change or new device UI run.

## Initial historical rules

87 units (core 67/app 14/recovery 6), Debug/test builds, lint 0 errors with existing UnusedResources/SetTextI18n warnings. API35 independent emulator creation 157 + format/weak-consent/old-recovery 109 =266 assertions. Each missing category, mismatch/live buttons, valid 10 generation, old 6 recovery, cancel/background/rotation/overlay/stale-callback cleanup. Public passwords/unfunded wallets only. External evidence `BitTap-security-validation/password-policy/`.
