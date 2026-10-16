"""Adversarial checks for release comparison and metadata signatures; no wallet secrets."""
import hashlib
import importlib.util
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[2]


class ApkComparisonTest(unittest.TestCase):
    def test_signatures_are_excluded_but_code_and_assets_are_compared(self):
        with tempfile.TemporaryDirectory() as directory:
            first, second = (Path(directory) / name for name in ['signed.apk', 'unsigned.apk'])
            for path in [first, second]:
                with zipfile.ZipFile(path, 'w') as z:
                    for name in ['AndroidManifest.xml', 'classes.dex', 'resources.arsc', 'assets/licenses/LICENSE']:
                        z.writestr(name, 'public build fixture only')
                    if path == first:
                        z.writestr('META-INF/TEST.RSA', 'public signature fixture')
            command = [os.sys.executable, str(ROOT / 'tools/compare_apks.py'), str(first), str(second)]
            self.assertEqual(0, subprocess.run(command, capture_output=True).returncode)
            with zipfile.ZipFile(second, 'a') as z:
                z.writestr('assets/unexpected-code-input', 'tampered')
            self.assertNotEqual(0, subprocess.run(command, capture_output=True).returncode)

    def test_empty_archives_are_not_accepted_as_matching_apks(self):
        with tempfile.TemporaryDirectory() as directory:
            files = [Path(directory) / name for name in ['one.apk', 'two.apk']]
            for path in files:
                with zipfile.ZipFile(path, 'w'):
                    pass
            command = [os.sys.executable, str(ROOT / 'tools/compare_apks.py'), *map(str, files)]
            self.assertNotEqual(0, subprocess.run(command, capture_output=True).returncode)


class SignedManifestTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.directory = tempfile.TemporaryDirectory(prefix='bittap-public-signature-test-')
        cls.path = Path(cls.directory.name)
        cls.java = Path(os.environ['JAVA_HOME']) / 'bin/java'
        cls.environment = dict(os.environ, BITTAP_PUBLIC_FIXTURE_PASSWORD='public-fixture-only',
                               BITNFC_RELEASE_STORE_FILE=str(cls.path / 'fixture.p12'),
                               BITNFC_RELEASE_STORE_PASSWORD='public-fixture-only',
                               BITNFC_RELEASE_KEY_PASSWORD='public-fixture-only', BITNFC_RELEASE_KEY_ALIAS='fixture')
        subprocess.run([str(cls.java.with_name('keytool')), '-genkeypair', '-storetype', 'PKCS12',
                        '-keystore', cls.environment['BITNFC_RELEASE_STORE_FILE'], '-alias', 'fixture',
                        '-storepass:env', 'BITTAP_PUBLIC_FIXTURE_PASSWORD', '-keypass:env', 'BITTAP_PUBLIC_FIXTURE_PASSWORD',
                        '-keyalg', 'RSA', '-keysize', '2048', '-validity', '1', '-dname', 'CN=Public test fixture only'],
                       env=cls.environment, check=True, capture_output=True)
        cls.manifest = cls.path / 'release-manifest.json'
        cls.manifest.write_text('{"publicTestFixture":true}\n')
        subprocess.run([str(cls.java), str(ROOT / 'tools/ReleaseManifest.java'), 'sign', str(cls.manifest), str(cls.path)],
                       env=cls.environment, check=True, capture_output=True)
        cls.fingerprint = hashlib.sha256((cls.path / 'release-certificate.der').read_bytes()).hexdigest()

    @classmethod
    def tearDownClass(cls):
        cls.directory.cleanup()

    def verify(self, manifest, fingerprint):
        return subprocess.run([str(self.java), str(ROOT / 'tools/ReleaseManifest.java'), 'verify', str(manifest),
                               str(self.path / 'release-certificate.der'), str(self.path / 'release-manifest.json.sig'),
                               fingerprint], capture_output=True).returncode

    def test_signature_accepts_exact_manifest_and_trusted_certificate(self):
        self.assertEqual(0, self.verify(self.manifest, self.fingerprint))

    def test_signature_rejects_tampered_manifest(self):
        tampered = self.path / 'tampered.json'
        tampered.write_text('{"publicTestFixture":false}\n')
        self.assertNotEqual(0, self.verify(tampered, self.fingerprint))

    def test_signature_rejects_untrusted_certificate(self):
        self.assertNotEqual(0, self.verify(self.manifest, '0' * 64))


if __name__ == '__main__':
    unittest.main()
