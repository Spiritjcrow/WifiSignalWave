#!/usr/bin/env bash
# Offline source preflight: no network or Android SDK needed.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
paths=list(Path('app/src/main').rglob('*.xml'))
assert paths,'No Android XML found'
for p in paths: ET.parse(p)
print('PASS: parsed',len(paths),'Android XML files')
src=Path('app/src/main/java/com/wifisignal/wave/renderer/SignalRenderer.java').read_text()
assert 'GL_PROGRAM_POINT_SIZE' not in src
assert 'gl_PointSize' in src and 'GL_POINTS' in src
ui=Path('app/src/main/java/com/wifisignal/wave/MainActivity.java').read_text().replace(' ','')
assert 'postDelayed(this,33L)' in ui and 'queueEvent(' in ui
assert 'onRequestPermissionsResult' in ui
print('PASS: GLES3, animation, permission and GL-thread checks')
PY
if command -v javac >/dev/null; then
  temp="$(mktemp -d)"
  trap 'rm -rf "$temp"' EXIT
  javac -d "$temp" app/src/main/java/com/wifisignal/wave/sensor/SignalProcessor.java tools/SignalProcessorSmoke.java
  java -cp "$temp" SignalProcessorSmoke
else
  echo 'SKIP: Java compiler not present'
fi
