#!/usr/bin/env python3
"""Local-only signing after a successful independent CI build of the exact clean commit."""
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REPOSITORY = 'haidaoyige/BitTap'


def run(arguments, **options):
    return subprocess.run([str(a) for a in arguments], cwd=ROOT, check=True, **options)


def capture(arguments):
    return subprocess.check_output([str(a) for a in arguments], cwd=ROOT, text=True).strip()


def module_build(name):
    isolated = os.environ.get('BITTAP_BUILD_ROOT')
    return Path(isolated) / ('_' + name) if isolated else ROOT / name / 'build'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--ci-run-id', required=True, type=int)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    if os.environ.get('GITHUB_ACTIONS') == 'true':
        raise ValueError('Release signing is deliberately excluded from GitHub Actions')
    if capture(['git', 'status', '--porcelain']):
        raise ValueError('Commit or remove all source changes before signing')
    for name in ['BITNFC_RELEASE_STORE_FILE', 'BITNFC_RELEASE_STORE_PASSWORD',
                 'BITNFC_RELEASE_KEY_ALIAS', 'BITNFC_RELEASE_KEY_PASSWORD']:
        if not os.environ.get(name):
            raise ValueError('Missing existing release identity configuration: ' + name)
    source = capture(['git', 'rev-parse', 'HEAD'])
    ci = json.loads(capture(['gh', 'api', f'repos/{REPOSITORY}/actions/runs/{args.ci_run_id}']))
    if (ci['head_sha'] != source or ci['conclusion'] != 'success'
            or ci['path'] != '.github/workflows/ci.yml'
            or ci['event'] not in {'push', 'workflow_dispatch'}):
        raise ValueError('Require successful BitTap CI for this exact source commit')
    output = (args.output or ROOT / 'release-artifacts' / source[:12]).resolve()
    output.mkdir(parents=True, exist_ok=False)
    run([sys.executable, 'tools/check_environment.py', '--output', output / 'build-environment.json'])
    gradle = ['./gradlew', '--no-daemon', '--dependency-verification', 'strict']
    if os.environ.get('BITTAP_GOOGLE_MIRROR') == '1':
        gradle += ['--init-script', 'tools/google-mirror.init.gradle']
    run(gradle + [':core:test', ':app:testReleaseUnitTest', ':recovery:test',
                  ':app:assembleRelease', ':app:lintRelease', ':recovery:distZip'])
    app = module_build('app')
    recovery = module_build('recovery')
    isolated = os.environ.get('BITTAP_BUILD_ROOT')
    metadata = (Path(isolated) / '_root' if isolated else ROOT / 'build') / 'release-metadata'
    apk = app / 'outputs/apk/release/app-release.apk'
    tools = json.loads((ROOT / 'tools/build-environment.json').read_text())
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not sdk:
        raise ValueError('Set ANDROID_HOME to the pinned Android SDK')
    signer = Path(sdk) / 'build-tools' / tools['androidBuildTools'] / 'apksigner'
    certificate = capture([signer, 'verify', '--verbose', '--print-certs', apk])
    if tools['apkCertificateSha256'] not in certificate:
        raise ValueError('APK signing certificate differs from the existing release identity')
    (output / 'APK-SIGNATURE.txt').write_text(certificate + '\n')
    with tempfile.TemporaryDirectory(prefix='bittap-independent-ci-') as directory:
        run(['gh', 'run', 'download', args.ci_run_id, '--repo', REPOSITORY,
             '--name', 'unsigned-' + source, '--dir', directory])
        candidates = list(Path(directory).rglob('app-release-unsigned.apk'))
        if len(candidates) != 1:
            raise ValueError('Expected exactly one independently built unsigned APK')
        run([sys.executable, 'tools/compare_apks.py', apk, candidates[0]])
        archives = list(Path(directory).rglob('bittap-recover.zip'))
        if len(archives) != 1:
            raise ValueError('Expected exactly one independently built recovery distribution')
        local_recovery = recovery / 'distributions/bittap-recover.zip'
        if hashlib.sha256(local_recovery.read_bytes()).digest() != hashlib.sha256(archives[0].read_bytes()).digest():
            raise ValueError('Recovery distribution differs from independent CI build')
    version = json.loads((app / 'outputs/apk/release/output-metadata.json').read_text())['elements'][0]['versionName']
    shutil.copy2(apk, output / f'BitTap-{version}.apk')
    shutil.copy2(recovery / 'distributions/bittap-recover.zip', output / f'BitTap-{version}-offline-recovery.zip')
    shutil.copy2(metadata / 'sbom.cdx.json', output / 'sbom.cdx.json')
    shutil.copy2(metadata / 'runtime-inventory.json', output / 'runtime-inventory.json')
    run([sys.executable, 'tools/verify_release_contents.py', '--apk', apk,
         '--recovery', recovery / 'distributions/bittap-recover.zip'])
    files = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(output.iterdir()) if p.is_file()}
    manifest = {'repository': REPOSITORY, 'sourceCommit': source, 'version': version,
                'independentBuildRun': ci['html_url'], 'apkCertificateSha256': tools['apkCertificateSha256'],
                'files': files}
    (output / 'release-manifest.json').write_text(json.dumps(manifest, indent=2) + '\n')
    java = Path(os.environ['JAVA_HOME']) / 'bin/java'
    run([java, 'tools/ReleaseManifest.java', 'sign', output / 'release-manifest.json', output])
    run([java, 'tools/ReleaseManifest.java', 'verify', output / 'release-manifest.json',
         output / 'release-certificate.der', output / 'release-manifest.json.sig', tools['apkCertificateSha256']])
    (output / 'SHA256SUMS.txt').write_text(''.join(hashlib.sha256(p.read_bytes()).hexdigest() + '  ' + p.name + '\n'
                                                for p in sorted(output.iterdir()) if p.is_file()))
    print('Signed candidate prepared; independent APK/recovery comparison and signed manifest: ' + str(output))


if __name__ == '__main__':
    main()
