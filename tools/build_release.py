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
from release_provenance import validate_ci, validate_policy, select_artifact, unpack_verified

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
    attempt = ci.get('run_attempt')
    jobs = json.loads(capture(['gh', 'api', f'repos/{REPOSITORY}/actions/runs/{args.ci_run_id}/attempts/{attempt}/jobs?per_page=100']))['jobs']
    validate_ci(ci, jobs, REPOSITORY, source, args.ci_run_id)
    actions = json.loads(capture(['gh', 'api', f'repos/{REPOSITORY}/actions/permissions']))
    selected = json.loads(capture(['gh', 'api', f'repos/{REPOSITORY}/actions/permissions/selected-actions']))
    workflow = json.loads(capture(['gh', 'api', f'repos/{REPOSITORY}/actions/permissions/workflow']))
    validate_policy(actions, selected, workflow, json.loads((ROOT / 'tools/actions-pins.json').read_text()))
    artifacts = json.loads(capture(['gh', 'api', f'repos/{REPOSITORY}/actions/runs/{args.ci_run_id}/artifacts?per_page=100']))['artifacts']
    artifact = select_artifact(artifacts, source)
    output = (args.output or ROOT / 'release-artifacts' / source[:12]).resolve()
    output.mkdir(parents=True, exist_ok=False)
    run([sys.executable, 'tools/check_environment.py', '--output', output / 'build-environment.json'])
    gradle = ['./gradlew', '--no-daemon', '--dependency-verification', 'strict']
    if os.environ.get('BITTAP_GOOGLE_MIRROR') == '1':
        gradle += ['--init-script', 'tools/google-mirror.init.gradle']
    # Build tools receive no signing configuration. Compare the independent unsigned
    # payload first, then invoke apksigner directly with the existing local identity.
    unsigned_environment = {k: v for k, v in os.environ.items() if not k.startswith('BITNFC_RELEASE_')}
    run(gradle + [':core:test', ':app:testReleaseUnitTest', ':recovery:test',
                  ':app:assembleRelease', ':app:lintRelease', ':recovery:distZip',
                  '-PbittapUnsignedVerification=true'], env=unsigned_environment)
    app = module_build('app')
    recovery = module_build('recovery')
    isolated = os.environ.get('BITTAP_BUILD_ROOT')
    metadata = (Path(isolated) / '_root' if isolated else ROOT / 'build') / 'release-metadata'
    unsigned_apk = app / 'outputs/apk/release/app-release-unsigned.apk'
    apk = output / 'signed-candidate.apk'
    tools = json.loads((ROOT / 'tools/build-environment.json').read_text())
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not sdk:
        raise ValueError('Set ANDROID_HOME to the pinned Android SDK')
    signer = Path(sdk) / 'build-tools' / tools['androidBuildTools'] / 'apksigner'
    with tempfile.TemporaryDirectory(prefix='bittap-independent-ci-') as directory:
        archive = Path(directory) / 'github-artifact.zip'
        with archive.open('wb') as stream:
            run(['gh', 'api', f"repos/{REPOSITORY}/actions/artifacts/{artifact['id']}/zip"], stdout=stream)
        unpack_verified(archive, artifact['digest'], directory)
        candidates = list(Path(directory).rglob('app-release-unsigned.apk'))
        if len(candidates) != 1:
            raise ValueError('Expected exactly one independently built unsigned APK')
        run([sys.executable, 'tools/compare_apks.py', unsigned_apk, candidates[0]])
        archives = list(Path(directory).rglob('bittap-recover.zip'))
        if len(archives) != 1:
            raise ValueError('Expected exactly one independently built recovery distribution')
        local_recovery = recovery / 'distributions/bittap-recover.zip'
        if hashlib.sha256(local_recovery.read_bytes()).digest() != hashlib.sha256(archives[0].read_bytes()).digest():
            raise ValueError('Recovery distribution differs from independent CI build')
    run([signer, 'sign', '--ks', os.environ['BITNFC_RELEASE_STORE_FILE'],
         '--ks-key-alias', os.environ['BITNFC_RELEASE_KEY_ALIAS'],
         '--ks-pass', 'env:BITNFC_RELEASE_STORE_PASSWORD', '--key-pass', 'env:BITNFC_RELEASE_KEY_PASSWORD',
         '--out', apk, unsigned_apk])
    certificate = capture([signer, 'verify', '--verbose', '--print-certs', apk])
    if tools['apkCertificateSha256'] not in certificate:
        raise ValueError('APK signing certificate differs from the existing release identity')
    (output / 'APK-SIGNATURE.txt').write_text(certificate + '\n')
    version = json.loads((app / 'outputs/apk/release/output-metadata.json').read_text())['elements'][0]['versionName']
    apk = apk.rename(output / f'BitTap-{version}.apk')
    provenance = {'repository': REPOSITORY, 'sourceCommit': source, 'runId': args.ci_run_id,
                  'runAttempt': attempt, 'artifactId': artifact['id'], 'artifactDigest': artifact['digest'],
                  'verificationJobId': next(j['id'] for j in jobs if j['name'] == 'Verify source and unsigned release')}
    (output / 'independent-ci-provenance.json').write_text(json.dumps(provenance, indent=2) + '\n')
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
