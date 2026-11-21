#!/usr/bin/env bash
# JDK 17, Android SDK 34, curl, unzip and Python 3 required.
set -Eeuo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
GRADLE_VERSION=8.2.1
GRADLE_SHA256=03ec176d388f2aa99defcadc3ac6adf8dd2bce5145a129659537c0874dea5ad1
CACHE="$HOME/.cache/wifisignalwave"
DIST="$CACHE/gradle-$GRADLE_VERSION"
if [[ ! -x "$DIST/bin/gradle" ]]; then
  for executable in curl unzip python3; do
    command -v "$executable" >/dev/null || { echo "ERROR: $executable unavailable" >&2; exit 2; }
  done
  mkdir -p "$CACHE"
  archive="$CACHE/gradle-$GRADLE_VERSION-bin.zip"
  if [[ ! -e "$archive" ]]; then
    curl --fail --location --retry 3 --connect-timeout 15 \
      --output "$archive.part" \
      "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    mv "$archive.part" "$archive"
  fi
  python3 - "$archive" "$GRADLE_SHA256" <<'PY'
import hashlib,sys
archive,expected=sys.argv[1:]
digest=hashlib.sha256()
with open(archive,'rb') as f:
    for block in iter(lambda: f.read(1024*1024),b''):
        digest.update(block)
actual=digest.hexdigest()
if actual!=expected:
    raise SystemExit('ERROR: Gradle SHA-256 mismatch '+actual)
print('PASS: pinned Gradle SHA-256 verified')
PY
  unzip -q -o "$archive" -d "$CACHE"
fi
command -v java >/dev/null || { echo 'ERROR: JDK required' >&2; exit 2; }
[[ -n "$(printenv ANDROID_SDK_ROOT || true)" || -n "$(printenv ANDROID_HOME || true)" ]] || {
  echo 'ERROR: Android SDK environment not configured' >&2; exit 2;
}
"$DIST/bin/gradle" --no-daemon --stacktrace testDebugUnitTest assembleDebug
APK="$ROOT/app/build/outputs/apk/debug/app-debug.apk"
[[ -s "$APK" ]] || { echo 'ERROR: no APK built' >&2; exit 3; }
python3 - "$APK" <<'PY'
import hashlib,sys
from pathlib import Path
apk=Path(sys.argv[1])
digest=hashlib.sha256(apk.read_bytes()).hexdigest()
(apk.parent/(apk.name+'.sha256')).write_text(digest+'  '+apk.name+'\n',encoding='ascii')
print('PASS: debug APK exists:',apk)
print('APK SHA-256:',digest)
PY
