#!/usr/bin/env python3
"""Compare all APK payload entries, excluding only JAR signatures (v2 is outside ZIP)."""
import argparse
import hashlib
import re
import sys
import zipfile


def payload(path):
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        if not {'AndroidManifest.xml', 'classes.dex', 'resources.arsc'}.issubset(names):
            raise ValueError('Not an Android application payload')
        if len(names) != len(set(names)):
            raise ValueError('Duplicate APK ZIP entries')
        return {name: hashlib.sha256(archive.read(name)).hexdigest() for name in names
                if not name.endswith('/') and not re.fullmatch(
                    r'META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))', name, re.I)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('signed_apk')
    parser.add_argument('independent_unsigned_apk')
    args = parser.parse_args()
    signed, rebuilt = payload(args.signed_apk), payload(args.independent_unsigned_apk)
    changed = sorted(n for n in signed.keys() | rebuilt.keys() if signed.get(n) != rebuilt.get(n))
    if changed:
        print('APK payload differs: ' + ', '.join(changed), file=sys.stderr)
        return 1
    print(f'APK payload match: {len(signed)} entries (signature verification is separate)')
    return 0


if __name__ == '__main__':
    sys.exit(main())
