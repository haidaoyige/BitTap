#!/usr/bin/env python3
"""Check shipped license completeness and each recovery runtime JAR against the SBOM inventory."""
import argparse
import hashlib
import json
import zipfile
from pathlib import Path


def require(condition, message):
    if not condition:
        raise ValueError(message)


def check_notices(archive, prefix):
    names = archive.namelist()
    require(len(names) == len(set(names)), 'Duplicate distribution entries')
    base = prefix + 'licenses/'
    require(len(archive.read(base + 'LICENSE')) > 10_000, 'Project license missing or truncated')
    require(archive.read(base + 'NOTICE'), 'Project notice missing')
    inventory = json.loads(archive.read(base + 'runtime-inventory.json'))
    require(inventory, 'Empty inventory')
    for component in inventory:
        require(archive.read(base + component['licenseText']), component['coordinate'])
        for notice in component['embeddedNotices']:
            require(archive.read(base + notice), notice)
    sbom = json.loads(archive.read(prefix + 'sbom.cdx.json'))
    require(sbom['bomFormat'] == 'CycloneDX', 'Unexpected SBOM format')
    require(len(sbom['components']) == len(inventory), 'SBOM inventory mismatch')
    return inventory


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--apk', type=Path, required=True)
    parser.add_argument('--recovery', type=Path, required=True)
    args = parser.parse_args()
    with zipfile.ZipFile(args.apk) as apk:
        android_inventory = check_notices(apk, 'assets/')
        require('assets/lucide-license.txt' in apk.namelist(), 'Existing icon attribution lost')
    with zipfile.ZipFile(args.recovery) as recovery:
        prefix = 'bittap-recover/'
        inventory = check_notices(recovery, prefix)
        require(len(recovery.read(prefix + 'LICENSE')) > 10_000, 'Recovery root license missing')
        expected = {c['filename']: c['sha256'] for c in inventory if 'recovery' in c['scopes']}
        actual = {}
        for name in recovery.namelist():
            if name.startswith(prefix + 'lib/') and name.endswith('.jar'):
                filename = name.rsplit('/', 1)[-1]
                # recovery/core are this project's own code, not external runtime coordinates.
                if filename in {'recovery.jar', 'core.jar'}:
                    continue
                actual[filename] = hashlib.sha256(recovery.read(name)).hexdigest()
        require(actual == expected, 'Recovery runtime JARs do not match reviewed inventory')
    require(android_inventory == inventory, 'APK/recovery notices describe different dependency sets')
    print(f'License bundle complete; {len(actual)} recovery dependency JARs match SHA-256 inventory')


if __name__ == '__main__':
    main()
