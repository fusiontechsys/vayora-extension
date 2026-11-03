#!/usr/bin/env bash
# Builds the release APK and signs it with the Vayora key.
set -euo pipefail
KS_PASS="bb23496eab3f8c179bbd7564"

[ -f gradlew ] || { echo "ERROR: gradlew not found in repo root"; exit 1; }
chmod +x gradlew
./gradlew assembleRelease

BT=$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)
APK=$(find app/build/outputs/apk/release -name "*.apk" | head -1)
[ -n "$APK" ] || { echo "ERROR: no release APK produced"; exit 1; }

"$BT/zipalign" -f -p 4 "$APK" aligned.apk
"$BT/apksigner" sign --ks keystore/sentinel.jks --ks-key-alias sentinel \
  --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
  --out Sentinel-release.apk aligned.apk
"$BT/apksigner" verify --print-certs Sentinel-release.apk
