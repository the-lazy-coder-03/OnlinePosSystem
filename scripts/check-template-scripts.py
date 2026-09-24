#!/usr/bin/env python3
"""Parse every embedded JavaScript block without executing application code."""
from html.parser import HTMLParser
from pathlib import Path
import subprocess
import tempfile


class Scripts(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=False)
        self.active = False
        self.blocks = []

    def handle_starttag(self, tag, attrs):
        if tag == 'script':
            attributes = dict(attrs)
            self.active = 'src' not in attributes and attributes.get('type', '') in (
                '', 'text/javascript', 'application/javascript', 'module')
            if self.active:
                self.blocks.append('')

    def handle_endtag(self, tag):
        if tag == 'script':
            self.active = False

    def handle_data(self, data):
        if self.active:
            self.blocks[-1] += data


root = Path(__file__).resolve().parent.parent
count = 0
for template in sorted((root / 'src/main/resources/templates').glob('*.html')):
    scripts = Scripts()
    scripts.feed(template.read_text())
    for index, block in enumerate(scripts.blocks):
        if not block.strip():
            continue
        with tempfile.NamedTemporaryFile(mode='w', suffix='.mjs') as script:
            script.write(block)
            script.flush()
            result = subprocess.run(['node', '--check', script.name], capture_output=True, text=True)
        if result.returncode:
            raise SystemExit(f'{template.name}, script {index + 1}:\n{result.stderr}')
        count += 1
print(f'Checked {count} embedded JavaScript blocks.')
