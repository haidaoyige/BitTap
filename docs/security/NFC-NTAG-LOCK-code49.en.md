[简体中文](NFC-NTAG-LOCK-code49.md) · [English](NFC-NTAG-LOCK-code49.en.md) · [文档目录 / Documentation](../README.md)

# Code 49: NTAG215/216-specific permanent read-only

2026-10-22, 0.6.2/code 49 local original-signer candidate, not published then.

## Feedback/boundary

Xiaomi 15/Android 16 code 48 Confirm permanent lock while attached: NFC-LOCK-L3-TIME, then NFC-D. L3 covers connect/makeReadOnly/close, D reader-service stop. Narrows the path but without stacks cannot identify the exact Binder call or prove firmware root cause. Code 49 replaces it, no platform Ndef.makeReadOnly/can Make Read Only. Actual phone/cards still require retest, not substituted by emulator/page model.

## Changes

- Only exact GET_VERSION NTAG215/216; unknown models never locked. Response is not chip authenticity proof.
- Before irreversible writing, compare same-card full NDEF; precheck CC/mirroring/access/already-frozen partial bits for all targets.
- Set CC read-only, dynamic locks, CFGLCK, static locks in order, fresh readback each step. Wallet user area untouched, valid existing configuration preserved.
- Fix dynamic-page RFUI: final read byte BD cannot be written unchanged; write zero and model-specific reserved-bit masks.
- Physically remove/retap to power-cycle CFGLCK; final complete wallet, hardware bits and NDEF discovery read-only all required. No destructive trial write.
- New H0 for CC write/readback, H1–H6 retained. L3 enum historical only, production no longer enters it. Stages include close, not one exact SDK call.
-15-second I/O, 3-second chip communication, cancel, single worker, late-result rejection unchanged. NFC-D still means stop call late; app cannot force system recovery.
- Write/lock distinct; cancellation/timeout/mismatch/unknown never locked. Mandatory backup/key generation/format unchanged.

## Validation

Nine new units with independent chip-page permission model: all user pages protected on both chips, data unchanged, reserved bytes, power-cycle config freeze, repeat, unknown/abnormal rejection, partial freeze precheck, ACK-without-programming, communication/truncated readback. Model is not real-chip proof.

NFC test cleanup previously used two near-simultaneous executor snapshots, causing a test race; now require 20 ms stable idle. 3-second bound/main responsiveness not relaxed; product scheduling unchanged. 137 units/device details/lint/permissions/signature/bytecode in external `BitTap-security-validation/code49-ntag-lock/validation.json`. Bytecode confirms no generic NDEF lock calls/no test probes. Keep code 47 visible-cancel timing follow-up; one pass not permanent resolution.

## Device steps

Reboot first to clear old stuck service. Use unfunded new wallet/writable NTAG215/216 and save backup. Confirm lock, actually remove and retap. Only final explicit “permanently read-only, content matches” is app confirmation. On failure report full fixed code only. Older cards may be partly locked; do not overwrite existing wallets for diagnosis.

Basis: [NXP Rev 3.2](https://www.nxp.com/docs/en/data-sheet/NTAG213_215_216.pdf),§8.5/8.8. No unpublished AN11456 or claimed NFC compatibility certification.
