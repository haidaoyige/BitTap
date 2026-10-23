#!/usr/bin/env python3
"""Check bilingual coverage and local links / 检查双语覆盖及本地链接。"""
from pathlib import Path
import hashlib
import json
import re
import sys
from urllib.parse import unquote
ROOT = Path(__file__).resolve().parents[1]
def links(text):
    text = re.sub(r'```[\s\S]*?```', '', text)
    return re.findall(r'\]\(([^\s)]+)(?:\s+"[^"]*")?\)', text)
def resolve(path, target):
    target = unquote(target.strip('<>')).split('#', 1)[0].split('?', 1)[0]
    if not target or re.match(r'^[a-zA-Z][a-zA-Z0-9+.-]*:', target):
        return None
    return (path.parent / target).resolve()
def main():
    manifest = json.loads((ROOT / 'docs/documentation-languages.json').read_text())
    covered = {'docs/README.md'}
    errors = []
    for item in manifest['documents']:
        for key in ('source', 'zh', 'en'):
            name = item[key]
            covered.add(name)
            if not (ROOT / name).is_file():
                errors.append(f'Missing {key}: {name}')
        source = ROOT / item['source']
        if source.is_file() and hashlib.sha256(source.read_bytes()).hexdigest() != item['source_sha256']:
            errors.append(f'Review both languages and refresh inventory: {item["source"]}')
    files = {p.relative_to(ROOT).as_posix() for p in ROOT.rglob('*.md')
             if not any(x in p.relative_to(ROOT).parts for x in ('.git', 'build', '.gradle'))}
    for name in sorted(files - covered):
        errors.append(f'Missing language coverage: {name}')
    # Historical reports can link unpublished evidence. Do not rewrite originals.
    legacy = set()
    for item in manifest['documents']:
        original = ROOT / item['source']
        if original.exists():
            for target in links(original.read_text()):
                dest = resolve(original, target)
                if dest is not None and not dest.exists():
                    legacy.add(dest)
    for name in sorted(files):
        path = ROOT / name
        text = path.read_text()
        if text.count('```') % 2:
            errors.append(f'Unclosed fenced block: {name}')
        for target in links(text):
            dest = resolve(path, target)
            if dest is not None and not dest.exists() and dest not in legacy:
                errors.append(f'Broken link: {name} -> {target}')
    if errors:
        print('\n'.join(errors))
        return 1
    print(f'PASS: {len(manifest["documents"])} source documents covered; {len(files)} Markdown files checked.')
    print(f'Unpublished historical targets retained: {len(legacy)}')
    return 0
if __name__ == '__main__':
    sys.exit(main())
