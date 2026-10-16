# Independent verification

After running `./gradlew :core:test`, install Python `cryptography` in a local virtual environment and run:

```sh
python tools/check_interop.py
python tools/check_addresses.py
```

Both scripts read only published, known-scalar test fixtures under `core/build/` and the legacy CG1 fixture in `core/src/test/resources/`. They never read NFC cards, user wallets or user backups. Never fund the fixture addresses. Results are written to `docs/`.
