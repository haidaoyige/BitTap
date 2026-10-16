#!/usr/bin/env python3
"""Check the pinned wrapper/JDK and record the actual build host without credentials."""
import argparse
import hashlib
import json
import os
import platform
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    pinned = json.loads((ROOT / 'tools/build-environment.json').read_text())
    actual_hash = hashlib.sha256((ROOT / 'gradle/wrapper/gradle-wrapper.jar').read_bytes()).hexdigest()
    if actual_hash != pinned['wrapperJarSha256']:
        raise ValueError('Gradle wrapper JAR differs from reviewed toolchain')
    properties = (ROOT / 'gradle/wrapper/gradle-wrapper.properties').read_text()
    if 'distributionSha256Sum=' + pinned['gradleZipSha256'] not in properties:
        raise ValueError('Gradle distribution checksum differs from reviewed toolchain')
    java = str(Path(os.environ['JAVA_HOME']) / 'bin/java') if os.environ.get('JAVA_HOME') else 'java'
    output = subprocess.check_output([java, '-XshowSettings:properties', '-version'], stderr=subprocess.STDOUT, text=True)
    version = re.search(r'java.runtime.version = ([^\n]+)', output)
    if not version or version.group(1).strip() != pinned['jdkRuntime']:
        raise ValueError('Use pinned Temurin JDK ' + pinned['jdkRuntime'])
    if 'Temurin' not in output:
        raise ValueError('Use the pinned Temurin JDK distribution')
    record = {'pinned': pinned, 'java': output, 'platform': platform.platform(),
              'runnerImage': {k: os.environ.get(k) for k in ['ImageOS', 'ImageVersion']},
              'sourceCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(record, indent=2) + '\n')
    print('Pinned wrapper and Temurin JDK verified; build environment recorded')


if __name__ == '__main__':
    main()
