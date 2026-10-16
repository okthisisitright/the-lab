#!/usr/bin/env bash
# Development-signed sideload package. Never commit the keystore.
set -euo pipefail
widgetlab_root="$(cd "$(dirname "$0")/.." && pwd)"
widgetlab_sdk="${ANDROID_HOME:?Set ANDROID_HOME to the Android SDK}"
widgetlab_tools="$widgetlab_sdk/build-tools/35.0.0"
widgetlab_version="${1:-0.3.0}"
[[ "$widgetlab_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]] || exit 2
widgetlab_apk="$widgetlab_root/prebuilt/WidgetLab-$widgetlab_version.apk"
"$widgetlab_tools/apksigner" sign --ks "${WIDGETLAB_DEV_KEYSTORE:-$HOME/.android/debug.keystore}" --ks-key-alias androiddebugkey --ks-pass pass:android --key-pass pass:android --min-sdk-version 29 --v4-signing-enabled false --out "$widgetlab_apk" "$widgetlab_root/app/build/outputs/apk/release/app-release-unsigned.apk"
"$widgetlab_tools/apksigner" verify --verbose --print-certs "$widgetlab_apk"
"$widgetlab_tools/zipalign" -c -P 16 4 "$widgetlab_apk"
python3 - "$widgetlab_apk" <<'PY'
from pathlib import Path
import hashlib,sys,zipfile
p=Path(sys.argv[1]);assert p.stat().st_size>0
with zipfile.ZipFile(p) as z:
    assert z.testzip() is None
    assert 'AndroidManifest.xml' in z.namelist() and 'classes.dex' in z.namelist()
digest=hashlib.sha256(p.read_bytes()).hexdigest()
p.with_name(p.name+'.sha256').write_text(digest+'  '+p.name+'\n')
print('Validated signed APK:',p.stat().st_size,'bytes; SHA-256',digest)
PY
