"""Reject confused CI identities, permissive workflow policy and tampered downloads."""
import copy
import hashlib
import json
from pathlib import Path
import sys
import tempfile
import unittest
import zipfile

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools'))
from release_provenance import validate_ci, validate_policy, select_artifact, unpack_verified


class ProvenanceTest(unittest.TestCase):
    def setUp(self):
        self.source = 'a' * 40
        self.run = {'id': 17, 'run_attempt': 2, 'head_sha': self.source, 'status': 'completed',
                    'conclusion': 'success', 'path': '.github/workflows/ci.yml', 'event': 'push',
                    'repository': {'full_name': 'owner/BitTap'}, 'head_repository': {'full_name': 'owner/BitTap'}}
        self.jobs = [{'name': 'Verify source and unsigned release', 'conclusion': 'success', 'run_attempt': 2}]
        self.artifact = {'name': 'unsigned-' + self.source, 'expired': False, 'digest': 'sha256:' + 'b' * 64,
                         'workflow_run': {'head_sha': self.source}, 'size_in_bytes': 100}

    def test_exact_successful_ci_is_accepted(self):
        validate_ci(self.run, self.jobs, 'owner/BitTap', self.source, 17)

    def test_fork_wrong_source_skipped_job_and_wrong_attempt_are_rejected(self):
        for field, value in [('head_sha', 'b' * 40), ('status', 'in_progress'), ('event', 'pull_request'),
                             ('path', '.github/workflows/other.yml'), ('id', 18), ('run_attempt', 1),
                             ('head_repository', {'full_name': 'attacker/BitTap'})]:
            changed = dict(self.run, **{field: value})
            with self.assertRaises(ValueError):
                validate_ci(changed, self.jobs, 'owner/BitTap', self.source, 17)
        with self.assertRaises(ValueError):
            validate_ci(self.run, [dict(self.jobs[0], conclusion='skipped')], 'owner/BitTap', self.source, 17)

    def test_artifacts_need_matching_source_valid_digest_and_unique_name(self):
        self.assertEqual(self.artifact, select_artifact([self.artifact], self.source))
        for field, value in [('expired', True), ('digest', ''), ('size_in_bytes', 101 * 1024 * 1024),
                             ('workflow_run', {'head_sha': 'b' * 40})]:
            with self.assertRaises(ValueError):select_artifact([dict(self.artifact, **{field: value})], self.source)
        with self.assertRaises(ValueError):select_artifact([self.artifact, self.artifact], self.source)

    def test_only_exact_reviewed_actions_and_readonly_defaults_are_accepted(self):
        pins = json.loads((ROOT / 'tools/actions-pins.json').read_text())
        actions = {'enabled': True, 'allowed_actions': 'selected', 'sha_pinning_required': True}
        selected = {'github_owned_allowed': False, 'verified_allowed': False,
                    'patterns_allowed': [k + '@' + v['sha'] for k, v in pins.items()]}
        workflow = {'default_workflow_permissions': 'read', 'can_approve_pull_request_reviews': False}
        validate_policy(actions, selected, workflow, pins)
        for changed in [dict(selected, patterns_allowed=['actions/checkout@*']), dict(selected, verified_allowed=True)]:
            with self.assertRaises(ValueError):validate_policy(actions, changed, workflow, pins)
        with self.assertRaises(ValueError):validate_policy(actions, selected, dict(workflow, default_workflow_permissions='write'), pins)

    def test_archive_digest_and_members_are_checked_before_extracting(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder);archive = root / 'artifact.zip'
            with zipfile.ZipFile(archive, 'w') as z:
                z.writestr('app/app-release-unsigned.apk', b'public APK fixture')
                z.writestr('recovery/bittap-recover.zip', b'public recovery fixture')
            digest = 'sha256:' + hashlib.sha256(archive.read_bytes()).hexdigest()
            unpack_verified(archive, digest, root)
            self.assertEqual(b'public APK fixture', (root / 'app-release-unsigned.apk').read_bytes())
            with self.assertRaises(ValueError):unpack_verified(archive, 'sha256:' + '0' * 64, root)
            with zipfile.ZipFile(archive, 'a') as z:z.writestr('../unexpected', b'unexpected')
            changed_digest = 'sha256:' + hashlib.sha256(archive.read_bytes()).hexdigest()
            with self.assertRaises(ValueError):unpack_verified(archive, changed_digest, root)


if __name__ == '__main__':unittest.main()
