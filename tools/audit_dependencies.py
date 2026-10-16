#!/usr/bin/env python3
"""Query OSV using only public Maven coordinates from the resolved runtime SBOM."""
import argparse
import datetime
import hashlib
import json
import sys
import urllib.request
from pathlib import Path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--sbom', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    data = args.sbom.read_bytes()
    sbom = json.loads(data)
    coordinates = sorted({(c['group'] + ':' + c['name'], c['version'])
                          for c in sbom['components'] if c['type'] == 'library'})
    if not coordinates:
        raise ValueError('Empty runtime inventory must not pass a dependency audit')
    query = {'queries': [{'package': {'ecosystem': 'Maven', 'name': name}, 'version': version}
                         for name, version in coordinates]}
    request = urllib.request.Request('https://api.osv.dev/v1/querybatch',
                                     data=json.dumps(query).encode(),
                                     headers={'Content-Type': 'application/json'}, method='POST')
    with urllib.request.urlopen(request, timeout=60) as response:
        result = json.load(response)
    if len(result.get('results', [])) != len(coordinates):
        raise ValueError('Incomplete OSV response')
    findings = [{'coordinate': name + ':' + version,
                 'advisories': sorted(v['id'] for v in record.get('vulns', []))}
                for (name, version), record in zip(coordinates, result['results'])]
    report = {'source': 'https://api.osv.dev/v1/querybatch',
              'queriedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(),
              'sbomSha256': hashlib.sha256(data).hexdigest(),
              'scope': 'resolved runtime components only; no private source uploaded',
              'results': findings}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + '\n')
    affected = [f for f in findings if f['advisories']]
    print(f'OSV: {len(coordinates)} runtime coordinates; {len(affected)} affected')
    for finding in affected:
        print(finding['coordinate'] + ': ' + ', '.join(finding['advisories']))
    return 1 if affected else 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except (OSError, ValueError, KeyError) as error:
        print('Dependency audit failed: ' + str(error), file=sys.stderr)
        sys.exit(2)
