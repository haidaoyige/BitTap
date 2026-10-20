"""Fail-closed checks of GitHub CI identity and artifact bytes before local signing."""
import hashlib
import re
import zipfile
from pathlib import Path


def validate_ci(run, jobs, repository, source, run_id):
    if (not re.fullmatch(r'[0-9a-f]{40}', source) or run.get('id') != run_id
            or run.get('head_sha') != source or run.get('status') != 'completed'
            or run.get('conclusion') != 'success'
            or run.get('path') != '.github/workflows/ci.yml'
            or run.get('event') not in {'push', 'workflow_dispatch'}
            or run.get('repository', {}).get('full_name') != repository
            or run.get('head_repository', {}).get('full_name') != repository):
        raise ValueError('Require successful same-repository CI for this exact source commit')
    attempt = run.get('run_attempt')
    if not isinstance(attempt, int) or attempt < 1:
        raise ValueError('Missing CI run attempt')
    verified = [j for j in jobs if j.get('name') == 'Verify source and unsigned release']
    if (len(verified) != 1 or verified[0].get('conclusion') != 'success'
            or verified[0].get('run_attempt') != attempt):
        raise ValueError('Require the actual successful verification job in this run attempt')


def validate_policy(actions, selected, workflow, pins):
    exact = {name + '@' + item['sha'] for name, item in pins.items()}
    if (not actions.get('enabled') or actions.get('allowed_actions') != 'selected'
            or not actions.get('sha_pinning_required')
            or selected.get('github_owned_allowed') or selected.get('verified_allowed')
            or set(selected.get('patterns_allowed', [])) != exact
            or workflow.get('default_workflow_permissions') != 'read'
            or workflow.get('can_approve_pull_request_reviews')):
        raise ValueError('Repository Actions must use the reviewed exact pins and read-only defaults')


def select_artifact(artifacts, source):
    matches = [a for a in artifacts if a.get('name') == 'unsigned-' + source]
    if len(matches) != 1:
        raise ValueError('Require exactly one unsigned comparison artifact')
    artifact = matches[0]
    if (artifact.get('expired') or not re.fullmatch(r'sha256:[0-9a-f]{64}', artifact.get('digest', ''))
            or artifact.get('workflow_run', {}).get('head_sha') != source
            or not 0 < artifact.get('size_in_bytes', 0) <= 100 * 1024 * 1024):
        raise ValueError('Missing, expired or invalid CI artifact provenance')
    return artifact


def unpack_verified(archive, digest, destination):
    if 'sha256:' + hashlib.sha256(Path(archive).read_bytes()).hexdigest() != digest:
        raise ValueError('Downloaded CI archive differs from GitHub artifact digest')
    expected = {'app-release-unsigned.apk', 'bittap-recover.zip'}
    with zipfile.ZipFile(archive) as z:
        members = [m for m in z.infolist() if not m.is_dir()]
        if (len(members) != 2 or {Path(m.filename).name for m in members} != expected
                or sum(m.file_size for m in members) > 100 * 1024 * 1024
                or any(m.flag_bits & 1 for m in members)):
            raise ValueError('Unexpected comparison archive members or size')
        # Write only fixed, approved basenames; never extract archive paths.
        for member in members:
            (Path(destination) / Path(member.filename).name).write_bytes(z.read(member))
