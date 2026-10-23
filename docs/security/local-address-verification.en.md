[简体中文](local-address-verification.md) · [English](local-address-verification.en.md) · [文档目录 / Documentation](../README.md)

# Local public-address verification records

## Establishment and later reads

- Creation establishes a record only after generated-key recovery/derivation checks, NFC write/readback and mandatory encrypted-file backup succeed.
- First recovery requires the full password and independently rederives/checks all four-addresses.
- Later reads strictly parse the complete payload, compute version-domain-separated SHA-256 and compare all 32 bytes to an authenticated record. Changes to randomness, addresses, note or ciphertext cannot inherit verification. Matching performs no wallet password derivation or private-key decryption.
- Status applies only to the corresponding receiving address, not physical card identity, balance or chain state.

## Storage/limits

Records contain only payload digest, four public addresses, public note, creation time and optional NFC UID hint: no passwords/password verifiers/private keys/wallet decryption keys/full encrypted backups. An independent nonexportable Android Keystore AES-256-GCM key authenticates/encrypts the atomic noBackupFilesDir file. Cloud backup/device migration disabled.

UID only detects changed data on an associated card; it is not a trust anchor. The same complete payload on another card can match. Unknown payload/UID on a new phone stays pending. Changed known-UID data requires password re-verification and explicit before/after public-data confirmation before adding a new record; the old record is not overwritten. An old complete valid payload can still match history; no rollback/clone protection.

Corruption/unavailable key/read failure cannot grant password-free verification. Fresh password verification remains usable in-session; save failure warns password will be needed next time. Reinstall/data clearing requires new records. Deleting records leaves cards/user backups intact. Home clears session but retains saved public records.

## Private-key boundary

Initial password verification briefly handles plaintext, then wraps it under an independent Keystore session. Only explicit view/copy/save briefly unwraps it again; no second password within the valid session. Idle 60 seconds or total 120 seconds, whichever earlier, and background/Home clear private session/presentation. Public address verification may remain after automatic lock.

Reduces repeated password entry/decryption; cannot guarantee safety against root, privileged malware or injection, or irrecoverable erasure of Java/system/user-exported plaintext copies.
