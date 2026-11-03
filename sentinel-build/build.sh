#!/usr/bin/env bash
# Builds the release APK and signs it with the Vayora key.
set -euo pipefail
KS_PASS="bb23496eab3f8c179bbd7564"

[ -f gradlew ] || { echo "ERROR: gradlew not found in repo root"; exit 1; }
chmod +x gradlew

# Remove the DUMP permission (normal apps can't hold it; it only adds a Play Protect flag)
python3 - <<'PY'
import re
m = "app/src/main/AndroidManifest.xml"
t = open(m, encoding="utf-8").read()
t2 = re.sub(r'[ \t]*<uses-permission[^>]*android\.permission\.DUMP[^>]*/>[ \t]*\n?', '', t)
open(m, "w", encoding="utf-8").write(t2)
print("DUMP permission removed" if t != t2 else "DUMP permission not found (nothing to remove)")
PY

./gradlew assembleRelease

BT=$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)
APK=$(find app/build/outputs/apk/release -name "*.apk" | head -1)
[ -n "$APK" ] || { echo "ERROR: no release APK produced"; exit 1; }

"$BT/zipalign" -f -p 4 "$APK" aligned.apk
"$BT/apksigner" sign --ks keystore/sentinel.jks --ks-key-alias sentinel \
  --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
  --out Sentinel-release.apk aligned.apk
"$BT/apksigner" verify --print-certs Sentinel-release.apk
