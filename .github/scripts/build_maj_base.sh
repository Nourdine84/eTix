#!/usr/bin/env bash
# Construction pour le test de mise à jour (jobs emulator-api34-maj-*) : version de base fusionnée (BASE_SHA,
# versionCode attendu BASE_CODE) puis cette version, avec la même clé de développement du runner (mise à jour réelle).
# Sorties : dist/base-app.apk, dist/base-androidTest.apk (tests de la base), dist/app-A.apk, emu-out/update_certs.txt.
set -euo pipefail
: "${BASE_SHA:?}" "${BASE_LABEL:?}" "${BASE_CODE:?}"
chmod +x ./gradlew
G="--console=plain -Dorg.gradle.java.home=$JAVA_HOME"
mkdir -p dist emu-out
git fetch --no-tags --depth=1 origin "$BASE_SHA"
git worktree add base "$BASE_SHA"
(cd base && chmod +x ./gradlew && ./gradlew assembleDebug assembleDebugAndroidTest $G)
cp base/app/build/outputs/apk/debug/app-debug.apk dist/base-app.apk
cp base/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk dist/base-androidTest.apk
./gradlew assembleDebug assembleDebugAndroidTest $G
cp app/build/outputs/apk/debug/app-debug.apk dist/app-A.apk
BT=$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)
for x in base-app app-A; do
  echo "$x $("$BT/aapt2" dump badging dist/$x.apk | grep -oP "versionCode='\K[^']+") $("$BT/apksigner" verify --print-certs dist/$x.apk | grep -oP 'SHA-256 digest: \K.*' | head -1)"
done > emu-out/update_certs.txt
echo "base $BASE_LABEL $BASE_SHA" >> emu-out/update_certs.txt
cat emu-out/update_certs.txt
CB=$(awk '/^base-app/{print $2}' emu-out/update_certs.txt); CA=$(awk '/^app-A/{print $2}' emu-out/update_certs.txt)
echo "::notice title=Mise à jour $BASE_LABEL → cette version::$BASE_LABEL $BASE_SHA versionCode $CB → versionCode $CA"
[ "$CB" = "$BASE_CODE" ] || { echo "::error title=$BASE_LABEL::versionCode $CB au lieu de $BASE_CODE"; exit 1; }
[ "$CA" -gt "$CB" ] || { echo "::error title=$BASE_LABEL::versionCode $CA de cette version non supérieur à $CB"; exit 1; }
