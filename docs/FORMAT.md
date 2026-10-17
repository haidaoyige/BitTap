# BitTap encrypted backups: CG3, CG2 and legacy CG1

CG3 is the project-specific format written by BitTap 0.4.0 internal build 7. CG2 and CG1 remain readable. It is **not BIP38 or a wallet import standard**. Keep this specification and the offline recovery distribution with backups. NFC is readable/copyable NDEF storage, not a secure element.

The text is `CG3:` (legacy `CG2:` / `CG1:`) followed by canonical, unpadded Base64URL. A prefix/version mismatch is rejected. No external URLs, keys or parameters are fetched.

## Common fixed header

| Byte offset | Length | Field |
| --- | --- | --- |
| 0 | 3 | ASCII `CGF` |
| 3 | 1 | Version: `0x03` for CG3; `0x02` for CG2; `0x01` for CG1 |
| 4 | 1 | Network: `0x00`, Bitcoin mainnet only |
| 5 | 1 | Key profile `0x01`: original scalar, compressed secp256k1 public key; default P2WPKH |
| 6 | 1 | KDF/cipher profile `0x01`: scrypt + AES-256-GCM |
| 7 | 1 | log2(N) = `15` (N = 32768) |
| 8 | 1 | scrypt r = `8` |
| 9 | 1 | scrypt p = `1` |
| 10 | 16 | Fresh random salt |
| 26 | 12 | Fresh random GCM nonce |

Profiles and parameters are strictly allowlisted before KDF work. Unknown versions, networks, address types, duplicate/out-of-order entries, extra bytes, noncanonical encodings, invalid address checksums and lengths are rejected.

## CG2 / CG3 public address section

Byte 38 is the address count, exactly `0x04`. Starting at byte 39, each entry is a one-byte type, one-byte ASCII byte length, then that many bytes of a canonical mainnet address. The exact order is:

| Type | Destination | Allowed length |
| --- | --- | --- |
| `0x01` | Native SegWit / P2WPKH, lowercase Bech32 v0, 20-byte program | 42 |
| `0x02` | Legacy / P2PKH, Base58Check, mainnet `1…` | 26–35 |
| `0x03` | Nested SegWit / P2SH-P2WPKH, Base58Check, mainnet `3…` | 26–35 |
| `0x04` | BIP341 key-only P2TR, lowercase Bech32m v1, 32-byte program | 62 |

CG3 appends one unsigned byte for the UTF-8 note length (0–64), then that many bytes. The note is Unicode NFC-normalized, at most 16 code points and 64 UTF-8 bytes, with letters, digits, combining marks and allowlisted ordinary punctuation. Controls, bidi formatting, newlines and emoji are rejected. The note is public and unverified before decryption; it is not a password, key derivation input or filename.

Exactly 48 bytes follow: the 32-byte encrypted original scalar and the 16-byte GCM authentication tag. **All header bytes, including the complete address section and CG3 note length/content, are AAD.** Reading these claims needs no password, but they are labelled “待解密核验” until authenticated.

After successful GCM decryption, validate the scalar and independently derive **all four** destinations, then compare every address with its stored counterpart. Any mismatch rejects recovery and closes the temporary plaintext holder. Never release WIF on authentication or address failure.

CG2 text is at most 363 ASCII bytes, normally 360. CG3 adds a note-length byte and up to 64 note bytes: at most 334 raw bytes / 450 text bytes. The current 27-byte MIME record adds framing: at most 483 NDEF bytes. Check the **actual encoded message** against the tag's usable NDEF capacity every time; prefer NTAG216, with NTAG215 subject to the actual check. NTAG213 cannot hold this payload. Chip user memory and usable NDEF capacity differ.

## Legacy CG1

CG1 uses the common header with version 1. Byte 38 is a length of 42, bytes 39–80 contain a native P2WPKH address, and bytes 81–128 are ciphertext/tag. AAD is bytes 0–80. The text is exactly 176 ASCII bytes. New creation never emits CG1; existing mainnet CG1 cards and files remain readable and decryptable.

CG1 provides only its original native SegWit claim before decryption. All four destinations can be derived after recovery. Independent BIP38 strings contain no public four-address metadata.

## Encryption, passwords and WIF

Passwords are Unicode NFC-normalized and UTF-8 encoded, with whitespace and case preserved. New creation requires 10–1024 UTF-16 code units and rejects all-whitespace input; recovery still accepts 1–1024 so older short-password backups remain recoverable. A ten-character minimum does not establish password entropy; guessable passwords still receive an explicit warning.

Derive 32 bytes with `scrypt(passwordUtf8, salt, N=32768, r=8, p=1, dkLen=32)`, nominally about 32 MiB plus implementation overhead. Encrypt the raw 32-byte scalar using AES-256-GCM with a 128-bit tag. Use fresh random salt and nonce for each encryption. The platform `SecureRandom` supplies generation randomness; rejection sampling accepts scalars in `[1,n-1]` without modulo bias and fails closed on RNG errors.

Private export is **Bitcoin mainnet compressed WIF**: Base58Check of `0x80 || originalScalar32 || 0x01`. WIF carries no address/script type. CG1/CG2/CG3 must first be decrypted by BitTap or the recovery CLI before import into another wallet.

## Address derivation

For compressed public key K: P2PKH uses HASH160(K); P2SH-P2WPKH hashes the v0 P2WPKH redeem script; native P2WPKH encodes HASH160(K) as Bech32 v0. P2TR follows BIP341 key-only output construction without a script tree: normalize internal point P to even Y; calculate `t = int(taggedHash("TapTweak", x(P)))`, rejecting t >= n; compute `Q = P + tG`, rejecting infinity; encode x(Q) as a v1 32-byte witness program with Bech32m. This is a single independent key, not an HD/BIP86 path-derived wallet.

The original scalar k recovers the Taproot spending scalar as `q = ((k if kG has even Y else n-k) + t) mod n`, with standard BIP340 parity handling for signing. The backup encrypts k, not q. Plain WIF import does not guarantee the matching Taproot construction; verify the destination wallet can recover the actual funded address. BitTap does not sign or send transactions or query balances.

References: [BIP341](https://github.com/bitcoin/bips/blob/master/bip-0341.mediawiki), [BIP350](https://github.com/bitcoin/bips/blob/master/bip-0350.mediawiki), [BIP341 test vectors](https://github.com/bitcoin/bips/blob/master/bip-0341/wallet-test-vectors.json), [BIP86 address examples](https://github.com/bitcoin/bips/blob/master/bip-0086.mediawiki).

## NFC, backups and compatibility

New writes contain one MIME record, `application/vnd.bittap.card`, carrying the complete ASCII CG3 string. Reading accepts this MIME, `application/vnd.bitnfc.card`, and the legacy `application/vnd.coingift.card`, plus UTF-8 NDEF Text records carrying CG3, CG2, CG1 or BIP38. Multiple recognized key records are rejected. Arbitrary URI records never trigger navigation.

Before writing, inspect writable status, usable capacity, UID and existing NDEF contents. Existing data requires explicit overwrite confirmation. The second tap must match the checked UID and unchanged original contents; this is an operational safeguard, not UID authentication. Write, then read over NFC again and compare the entire encoded NDEF message. No automatic formatting. Optional permanent read-only defaults off and supports only recognized NTAG215/216 layouts. After a full write/read comparison and separate irreversible confirmation, it locks user memory, CC and configuration. A subsequent tap must verify the same complete NDEF and hardware lock bits before success. Interrupted or partial locking reports unknown. This cannot prevent copying, emulation or physical replacement. Real-tag validation remains required. Interrupted writes remain unconfirmed.

A second card receives the **exact same ciphertext**, addresses and password; a new key is not generated. Distinct observed UIDs are counted within the creation session, not used as proof against cloned hardware.

To complete creation, save a backup file through Android's document provider. Close the output successfully, reopen and compare exact bytes before marking the backup saved. Cancellation, failed close, failed readback and merely copying text do not satisfy this step. Only ciphertext/public workflow state is retained in memory across configuration changes; no wallet state is serialized into savedInstanceState. Passwords and plaintext keys are cleared on pause. Returning home clears owned session data and the current app-owned clipboard; user-exported backup files remain. Process death, force-stop, uninstall or removal from Recents discards in-progress memory-only state. Save a backup first; Java/runtime/OS copies cannot be promised forensic erasure.

BIP38 is import-only for mainnet compressed keys, retaining standard P2PKH verification. Uncompressed BIP38 records are rejected. Four destinations are derived only after successful recovery.

## Optional camera mixing (v1)

Capture supplements the original platform SecureRandom. Obtain secret 32-byte R before capture. Camera2 hashes 32 distinct YUV frame payloads and one final still capture matched by sensor timestamp. No external camera app or image file is used. Cancellation, backgrounding, failed capture or failed random source stops/clears the operation; standard mode requires an explicit choice.

Let f[i] and photo be SHA-256 digests. C = SHA256(ASCII("BitTap/camera/v1") || 0x20 || concat(i=0..31, 0x00 || uint8(i) || 0x20 || f[i]) || 0x01 || 0x20 || photo). For i=0..127, candidate = HMAC-SHA256(key=R, message=ASCII("BitTap/private-key/v1") || 0x20 || C || uint32be(i)). Accept the first candidate in [1,n-1], with no modulo reduction; otherwise fail. Hashing does not create entropy; no independent camera entropy estimate is claimed. A compromised phone can steal inputs or outputs.

Owned R, digests, candidate and plaintext buffers are cleared on use/cancel/background. The JVM, crypto provider, camera service and OS can retain copies outside application control; this is not forensic erasure. Ciphertext stays in the active memory-only session until mandatory file backup and return home, permitting same-wallet multi-card writes. User-exported files/images remain.
