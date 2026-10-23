[简体中文](BitTap-0.7.0-软件真机补充审计.md) · [English](BitTap-0.7.0-软件真机补充审计.en.md) · [文档目录 / Documentation](../../../README.md)

> Public translation: technical findings/limits retained; local evidence names are private archive indexes. [Latest overall status](README.en.md).

# BitTap v0.7.0 software-on-phone audit supplement

2026-10-22. Commit `53c281b5f3ba8fa47ac1c59e9166300b384aaf33`, code 50.

## Conclusion

**Software checks passed on Xiaomi 2201122C / Android 15, API35, 2026-05-01 security patch, with no newly confirmed security defect. Physical NFC writing/interruption/permanent locking were not performed in this software stage.** Updates the main report's software deferral; host/source findings stand. Not all ROMs, Android 12/16, malicious services or absolute funds safety.

Installed release base.apk read back matches release SHA256 `a22be1a37c915385aa809cc804f1a11265539d8754b3bff9239ac2465024c5dc`. Deeper faults use separate `io.coingift.android.audit` / `.audit.test`: 42 production Java files byte-identical. Only package IDs/provider authority/test-service scope/screenshot prohibition/test Activity keep-awake/probes changed. Debug instrumentation is not direct instrumentation of the release APK.

## Device checks

| Suite | Result | Actual scope |
| --- | --- | --- |
| Private key/accessibility |109 assertions| Positive-control ordinary TextView readable; gates/WIF cleanup/service enabled during password/late private-file callback/nonexportable Keystore/tamper-close/alias deletion |
| Local records |324| Real storage roundtrip, GCM tamper, missing key, changed payload/same UID refusal, same payload/different UID, cancel change consent, four languages; test records only |
| Full flow |46| Generation, real MediaStore backup/readback, four-addresses, bad password, changed ciphertext cannot gain trust, exit cleanup. NFC simulated; Java recovery shares core, not independent cryptography |
| Session/provider |64| Actual 60-second idle, real faulty provider/15-second deadline, cancel/late callback/PNG wipe; other boundaries include acceleration |
| Independent 120-second limit |120089 ms passed| Five batches of valid injected touches every 20 seconds, clock unmodified; session closes/48-byte ciphertext zeroed. Not manual touches or proof all copies erased |
| NFC isolation model |62| Simulated blocked work/deadlines/late results on phone, no RF write |
| Read-only/ANR model |41| Simulated prerejection/control-service blocking/UI response/cancel, not Xiaomi 15/Android 16 root-cause acceptance |
| Lock diagnostics |192| Simulated stage failures/fixed codes/four languages/unknown, not actual permanent bits |

Suites overlap; no summed independent total. Raw logs/runner arguments privately archived, not distributed here.

## Actual release-APK flow

Separate external UI observation/operation, no modifications or target instrumentation:

1. Actual camera preview observed 2, 6, 11, 17, 24, 30, 32 frames; final capture/enhanced generation completed. No camera screenshot; counts do not quantify entropy.
2. Full receiving address hidden during creation. No card write, creation not marked completed.
3. Actual Android picker saved 375-byte CG3; app readback showed Save again. Backup SHA256 `b8d1e715fb6acfd798107c75d5b3b156947d9214e330519fd7d5c80242b7f22c`.
4. Independent Python cryptography Scrypt 32768/8/3/AES-GCM authenticated, validated scalar, re computed P2PKH/P2SH-P2WPKH/P2WPKH/Taproot; all match, note AUDIT-V070.
5. Exit/reimport through system picker: full address initially hidden, correct password restores matching derived-address digest. No request to display/copy this random wallet's private key.
6. Force-stop/restart/reimport: persisted record permits password-free public-address matching/verified QR; View key remains separately locked.

Unfunded one-use wallets; password/backup not delivered. No real incoming funds/external spending.

## Exceptions and handling

MIUI USB install countdown rejected automatically; authorized Continue only within that installation sheet, without disabling global protection/unknown-source policy. First 120-second probe failed during preparation because old test did not await a sync records/used old button. Test wait/locator fixed, actual 120089 ms passed; both logs retained, not a product timeout defect. Release UI script similarly updated visible label to Unlock cold wallet and verify addresses/waited transition; no product change.

After restoring service list, system asynchronously set master switch 1; once stable restored original 0, uninstalled tests and read back exact original list/switch.

## Cleanup/remaining scope

Exact accessibility restore verified; test service removed. Phone initially had no BitTap; release/two test packages uninstalled, no remaining packages. Only this test's hash-matched backups deleted; temporary passwords/backups/local original-service-config files/device temporary UIXML deleted. No other user data/global screen setting/card modified in cleanup.

Closes software-device-pending for this specific scope only. Physical 215/216 write/power loss/removal/lock/affected OEM retest remained pending here, not accepted. Old p=1 incompatibility, compliant weak passwords, repo-protection/mailbox boundaries remain. All OS/ROM/keyboards/services, root/injection, hardware entropy, external spending not exhaustive. No production/requirements/remote changes; public supplement excludes raw device logs/recovery materials. Later physical stop record governs the card stage.
