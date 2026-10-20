# Independent verification

After running `./gradlew :core:test`, install Python `cryptography` in a local virtual environment and run:

```sh
python tools/check_interop.py
python tools/check_addresses.py
python tools/check_bip38.py
```

Both scripts read only published, known-scalar test fixtures under `core/build/` and the legacy CG1 fixture in `core/src/test/resources/`. They never read NFC cards, user wallets or user backups. Never fund the fixture addresses. Results are written to `docs/`.

## Release provenance gate

`build_release.py` requires a clean exact commit and a successful same-repository
CI run with the actual verification job. Actions must use the four reviewed
exact SHA pins, SHA enforcement and read-only defaults. It verifies GitHub's
artifact ZIP SHA-256 and fixed members, compares the independent unsigned APK
and recovery ZIP, then signs locally with the existing identity. Gradle receives
no release-signing environment. No automatic GitHub release or upload occurs.

Personal private-repository branch protection remains unavailable on the current
plan; these gates do not replace enforced branch protection or trusted source
review. The audit collaborator's existing write access is retained per owner
instruction until an audit-specific read-only SSH public key is supplied.
