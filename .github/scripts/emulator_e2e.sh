#!/usr/bin/env bash
# Tests de bout en bout sur émulateur (données fictives). Exécuté par android-emulator-runner.
# Ne touche à aucun appareil réel. Signature : clé de développement du runner (build réservé aux tests).
set -u
API=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
REL=$(adb shell getprop ro.build.version.release | tr -d '\r')
OUT="emu-out"; mkdir -p "$OUT/shots"
echo "api=$API release=$REL" > "$OUT/device.txt"
adb shell getprop ro.product.model | tr -d '\r' >> "$OUT/device.txt"
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb logcat -c || true

adb install -r app/build/outputs/apk/debug/app-debug.apk > "$OUT/install.txt" 2>&1
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk >> "$OUT/install.txt" 2>&1

run() { # $1 = classe de test
  adb shell am instrument -w -r -e class "$1" com.etix.test/androidx.test.runner.AndroidJUnitRunner > "$OUT/instr_$(basename "${1//./_}").txt" 2>&1
}

# Phase A : parcours complet sur app neuve
run com.etix.e2e.E2eParcoursTest

# Isolation QA (sur émulateur) : état de com.etix avant installation d'eTix QA
adb shell am force-stop com.etix
adb shell dumpsys package com.etix | grep -E "lastUpdateTime|versionName" | tr -d '\r' > "$OUT/etix_avant_qa.txt"
adb install app/build/outputs/apk/qa/app-qa.apk > "$OUT/install_qa.txt" 2>&1
adb shell pm list packages | grep -i etix | tr -d '\r' > "$OUT/packages.txt"
adb shell dumpsys package com.etix | grep -E "lastUpdateTime|versionName" | tr -d '\r' > "$OUT/etix_apres_qa.txt"
adb shell monkey -p com.etix.qa -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1
sleep 6
adb exec-out screencap -p > "$OUT/shots/api${API}_28_eTixQA_premier_lancement.png"
adb shell am force-stop com.etix.qa

# Phase B : processus tué, QA installée à côté → données com.etix intactes
run com.etix.e2e.E2ePersistanceTest

# Captures prises par les tests (run-as : app debuggable)
for f in $(adb shell run-as com.etix ls files/shots 2>/dev/null | tr -d '\r'); do
  adb exec-out run-as com.etix cat "files/shots/$f" > "$OUT/shots/$f"
done

# Plantages éventuels
adb logcat -d > "$OUT/logcat.txt" 2>&1 || true
grep -n -A25 "FATAL EXCEPTION" "$OUT/logcat.txt" > "$OUT/crashes.txt" || true
exit 0
