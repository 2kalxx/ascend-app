#!/bin/bash
set -e
cd "$(dirname "$0")"
export ANDROID_HOME=~/android-sdk; BT=$ANDROID_HOME/build-tools/35.0.0; PJ=$ANDROID_HOME/platforms/android-34/android.jar
mkdir -p assets && cp ../web/index.html assets/index.html
rm -rf out; mkdir -p out/classes out/dex
$BT/aapt2 compile --dir res -o out/res.zip
$BT/aapt2 link -o out/base.apk -I $PJ --manifest AndroidManifest.xml -A assets out/res.zip --java out/gen --auto-add-overlay
javac -Xlint:-options --release 8 -classpath $PJ -d out/classes $(find src out/gen -name '*.java') 2>&1 | grep -v -E "Picked up" || true
$BT/d8 --lib $PJ --min-api 26 --output out/dex $(find out/classes -name '*.class') 2>&1 | grep -v "Picked up" || true
test -f out/dex/classes.dex
cp out/base.apk out/unsigned.apk; (cd out/dex && zip -q ../unsigned.apk classes.dex)
$BT/zipalign -f -p 4 out/unsigned.apk out/aligned.apk
$BT/apksigner sign --ks "${KEYSTORE:-$HOME/ascend.keystore}" --ks-pass pass:"${KS_PASS:?set KS_PASS}" --out ../apk/Ascend.apk out/aligned.apk 2>&1 | grep -v Picked || true
$BT/apksigner verify ../apk/Ascend.apk 2>&1 | grep -v Picked && echo SIGNED_OK
