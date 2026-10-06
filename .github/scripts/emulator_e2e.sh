#!/usr/bin/env bash
# Tests de bout en bout sur émulateur (données fictives). Exécuté par android-emulator-runner.
# Ne touche à aucun appareil réel. Signature : clé de développement du runner (build réservé aux tests).
# MODE=standard (défaut) : parcours complet + mise à jour A→B + isolation QA + persistance + lot 4 + Catégories
# MODE=fr : émulateur en français, tests de locale (saisie, dates, filtres inclusifs, limites de période)
# MODE=petit : petit écran / grande police ; MODE=compat : lecteur de dates OCR seul (API 22 à 25)
# MODE=revue : émulateur en français, captures de revue visuelle (Accueil, Historique, Ajouter ; clair / sombre ; Ajouter aussi en
#   320 dp police 2,0 ; sélecteur de date autour du seuil de bascule : 320 dp, police 2,0, 1,8, 1,5 ; mêmes données fictives)
# MODE=systeme (lot 9) : ML Kit sans réseau au 1er lancement, vrais sélecteur d'image et appareil photo
# MODE=maj (lot 9, étendu au lot 10) : mise à jour depuis une version fusionnée (BASE_LABEL : lot 8 ou lot 9, APK
#   construits par build_maj_base.sh) vers cette version, données et thème comparés
set -u
MODE="${MODE:-standard}"
[ "$MODE" = "maj8" ] && MODE=maj
BASE_LABEL="${BASE_LABEL:-base}"

boot_wait() { # attend la fin du démarrage (max ~4 min)
  timeout 60 adb wait-for-device
  for _ in $(seq 1 120); do
    [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && \
      adb shell cmd package list packages >/dev/null 2>&1 && return 0
    sleep 2
  done
  return 1
}

if [ "$MODE" = "fr" ] || [ "$MODE" = "revue" ]; then
  # Langue système fr-FR : propriété persistante (image google_apis, adb root) + redémarrage du framework.
  # (-change-locale redémarrait le framework pendant l'installation : « Broken pipe », langue restée en-US.)
  timeout 60 adb root >/dev/null 2>&1 || true
  boot_wait
  adb shell setprop persist.sys.locale fr-FR
  adb shell setprop ctl.restart zygote
  sleep 10
  boot_wait
  sleep 15
fi
API=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
REL=$(adb shell getprop ro.build.version.release | tr -d '\r')
LOC=$(adb shell getprop persist.sys.locale | tr -d '\r'); [ -z "$LOC" ] && LOC=$(adb shell getprop ro.product.locale | tr -d '\r')
OUT="emu-out"; mkdir -p "$OUT/shots"
{ echo "api=$API release=$REL mode=$MODE locale=$LOC"; adb shell getprop ro.product.model | tr -d '\r'; } > "$OUT/device.txt"
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb logcat -c || true
# Écran allumé, déverrouillé, sans boîte de dialogue système (ex. « System UI ne répond pas » au démarrage) :
# une telle fenêtre retire le focus à l'app et fait échouer le 1er test sans rapport avec l'app.
adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
sleep 5
adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
{ echo "focus avant tests:"; timeout 30 adb shell dumpsys window 2>/dev/null | grep -E "mCurrentFocus|mFocusedApp" | tr -d '\r' | sed 's/^ *//'; } >> "$OUT/device.txt"

T() { # T <secondes> <commande…> : limite de durée ; en cas de dépassement, diagnostic
  local secs=$1; shift
  if ! timeout "$secs" "$@"; then
    local tag="timeout_$(date +%H%M%S)"
    echo "TIMEOUT/ÉCHEC (${secs}s): $*" >> "$OUT/timeouts.txt"
    timeout 30 adb exec-out screencap -p > "$OUT/shots/api${API}_zz_${tag}.png" 2>/dev/null || true
    timeout 30 adb shell dumpsys activity top 2>/dev/null | grep -E "ACTIVITY|mResumed" | head -5 >> "$OUT/timeouts.txt" || true
    timeout 30 adb logcat -d -t 80 >> "$OUT/timeouts.txt" 2>&1 || true
  fi
}

pkginfo() { adb shell dumpsys package com.etix | grep -E "versionCode|versionName|lastUpdateTime|firstInstallTime" | tr -d '\r' | sed 's/^ *//'; }

pkgperms() { adb shell dumpsys package com.etix | tr -d '\r' | sed -n '/requested permissions:/,/install permissions:/p' | sed 's/^ *//'; }

if [ "$MODE" = "maj" ]; then
  # Version de base fusionnée installée neuve, données créées par SES propres tests (sources de la base)
  T 300 adb install -r dist/base-app.apk > "$OUT/install.txt" 2>&1
  T 300 adb install -r dist/base-androidTest.apk >> "$OUT/install.txt" 2>&1
fi
if [ "$MODE" != "maj" ]; then
# Build A (versionCode N) — même code que B, voir workflow
T 300 adb install -r dist/app-A.apk > "$OUT/install.txt" 2>&1
T 300 adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk >> "$OUT/install.txt" 2>&1
fi

run() { # $1 = classe de test, $2 = suffixe/passe facultatif (limite 12 min par classe)
  local extra="" suf=""
  # Avant chaque classe : écran allumé, déverrouillé, sans dialogue système (perte de focus intermittente observée)
  timeout 20 adb shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
  timeout 20 adb shell wm dismiss-keyguard >/dev/null 2>&1 || true
  timeout 20 adb shell am broadcast -a android.intent.action.CLOSE_SYSTEM_DIALOGS >/dev/null 2>&1 || true
  [ -n "${2:-}" ] && { extra="-e passe $2"; suf="_$2"; }
  # attendu, comparé aux résultats par emu_report.py ; RUN_SRC = sources d'une autre version (base fusionnée, mode maj)
  echo "$1 ${2:-}${RUN_SRC:+ src=$RUN_SRC}" >> "$OUT/expected_runs.txt"
  T 720 adb shell am instrument -w -r $extra -e class "$1" com.etix.test/androidx.test.runner.AndroidJUnitRunner > "$OUT/instr_$(basename "${1//./_}")$suf.txt" 2>&1
  timeout 20 adb shell am force-stop com.etix.test >/dev/null 2>&1 || true
  # Plantage natif (signal) : lignes fatales du journal relevées tout de suite (tampon limité sur API 21)
  if grep -q "Native crash\|Process crashed" "$OUT/instr_$(basename "${1//./_}")$suf.txt" 2>/dev/null; then
    { echo "== $1$suf"; timeout 30 adb logcat -d -v brief '*:F' 'DEBUG:*' 'art:E' 'AndroidRuntime:E' 2>/dev/null | tail -60; } >> "$OUT/native.txt"
  fi
}

collect() {
  for f in $(timeout 60 adb shell run-as com.etix ls files/shots 2>/dev/null | tr -d '\r'); do
    timeout 60 adb exec-out run-as com.etix cat "files/shots/$f" > "$OUT/shots/$f"
  done
  timeout 120 adb logcat -d > "$OUT/logcat.txt" 2>&1 || true
  grep -n -A25 "FATAL EXCEPTION" "$OUT/logcat.txt" > "$OUT/crashes.txt" || true
}

if [ "$MODE" = "compat" ]; then
  # Compatibilité ciblée (API 22 à 25) : lecteur de dates OCR seulement, logique pure, aucune donnée touchée
  run com.etix.e2e.E2eCompatDatesOcrTest
  collect
  exit 0
fi

if [ "$MODE" = "petit" ]; then
  # Petit écran + grande police, clavier ouvert. Passes : a = 360x640 dp police 1,3 ; b = 360x640 dp police 2,0 ;
  # c = 320x569 dp (densité 360) police 1,3 ; d = 320x569 dp police 2,0 (cas le plus contraint, lot 9).
  # Réglages de l'émulateur uniquement (jetable).
  for cfg in "a 720x1280 320 1.3" "b 720x1280 320 2.0" "c 720x1280 360 1.3" "d 720x1280 360 2.0"; do
    set -- $cfg
    adb shell wm size "$2"; adb shell wm density "$3"; adb shell settings put system font_scale "$4"
    echo "passe $1 : wm size $2, densité $3, police $4" >> "$OUT/device.txt"
    sleep 4
    run com.etix.e2e.E2eClavierPetitEcranTest "$1"
  done
  collect
  exit 0
fi

if [ "$MODE" = "fr" ]; then
  run com.etix.e2e.E2eFrancaisTest
  run com.etix.e2e.E2eScanFrTest
  collect
  exit 0
fi

if [ "$MODE" = "revue" ]; then
  # Revue visuelle : app neuve, données fictives injectées par le test, captures seulement
  run com.etix.e2e.E2eRevueAccueilTest
  run com.etix.e2e.E2eRevueHistoriqueTest   # mêmes données (créées par la classe précédente)
  run com.etix.e2e.E2eRevueAjouterTest      # écran Ajouter, mêmes données, formulaire non enregistré
  # Ajouter sur petit écran et grande police, clavier ouvert : 320 dp (720x1280, densité 360), police 2,0
  adb shell wm size 720x1280; adb shell wm density 360; adb shell settings put system font_scale 2.0
  echo "passe petit : wm size 720x1280, densité 360, police 2.0" >> "$OUT/device.txt"
  sleep 4
  run com.etix.e2e.E2eRevueAjouterTest petit
  # Sélecteur de date autour du seuil de bascule calendrier / saisie, 320 dp : police 2,0 (déjà réglée), 1,8, 1,5
  run com.etix.e2e.E2eRevueDateSeuilTest seuil20
  for cfg in "seuil18 1.8" "seuil15 1.5"; do
    set -- $cfg
    adb shell settings put system font_scale "$2"
    echo "passe $1 : wm size 720x1280, densité 360, police $2" >> "$OUT/device.txt"
    sleep 4
    run com.etix.e2e.E2eRevueDateSeuilTest "$1"
  done
  adb shell wm size reset; adb shell wm density reset; adb shell settings put system font_scale 1.0
  collect
  exit 0
fi

if [ "$MODE" = "systeme" ]; then
  pkgperms > "$OUT/shots/apk_permissions.txt"
  # 1) App neuve, jamais lancée, SANS réseau : 1re reconnaissance ML Kit (modèle embarqué attendu)
  timeout 30 adb shell cmd connectivity airplane-mode enable >/dev/null 2>&1 || true
  timeout 30 adb shell svc wifi disable >/dev/null 2>&1 || true
  timeout 30 adb shell svc data disable >/dev/null 2>&1 || true
  sleep 5
  { echo "réseau coupé (mode avion, Wi-Fi, données) :"; timeout 30 adb shell dumpsys connectivity 2>/dev/null | grep -m3 -E "Active default network|NetworkAgentInfo|Default network" | tr -d '\r'; } >> "$OUT/device.txt"
  run com.etix.e2e.E2eScanHorsLigneTest
  timeout 30 adb shell cmd connectivity airplane-mode disable >/dev/null 2>&1 || true
  timeout 30 adb shell svc wifi enable >/dev/null 2>&1 || true
  timeout 30 adb shell svc data enable >/dev/null 2>&1 || true
  sleep 5
  # 2) Vrais sélecteur d'image et application appareil photo de l'émulateur (caméra arrière « emulated »)
  adb shell am force-stop com.etix
  run com.etix.e2e.E2eScanSystemeTest
  # 3) Réseau (lot 9) : permissions accordées, groupes du processus (sans INTERNET : pas de groupe inet 3003, toute
  #    ouverture de connexion est refusée par le noyau), tâches planifiées par les bibliothèques (statistiques ML Kit)
  #    exécutées de force, puis recherche de plantage. Réseau de l'émulateur actif pendant cette étape.
  R="$OUT/shots/scan_reseau.txt"
  { echo "permissions installées de com.etix :"; adb shell dumpsys package com.etix | tr -d '\r' | sed -n '/install permissions:/,/User 0:/p' | grep -i "permission" | sed 's/^ *//'; } > "$R"
  adb shell monkey -p com.etix -c android.intent.category.LAUNCHER 1 >/dev/null 2>&1; sleep 6
  PID=$(adb shell pidof com.etix | tr -d '\r')
  echo "processus com.etix $PID : $(adb shell run-as com.etix cat /proc/$PID/status 2>/dev/null | tr -d '\r' | grep -E '^Groups')" >> "$R"
  JOBS=$(adb shell dumpsys jobscheduler | tr -d '\r' | grep -oE "JOB #u0a[0-9]+/[0-9]+: [0-9a-f]+ com\.etix/[A-Za-z0-9_.$]+" | sort -u)
  echo "tâches planifiées de com.etix : ${JOBS:-aucune}" >> "$R"
  for J in $(echo "$JOBS" | sed -E 's#JOB \#u0a[0-9]+/([0-9]+):.*#\1#'); do
    echo "exécution forcée de la tâche $J : $(timeout 30 adb shell cmd jobscheduler run -f com.etix "$J" 2>&1 | tr -d '\r')" >> "$R"
  done
  sleep 25
  echo "processus com.etix après les tâches : $(adb shell pidof com.etix | tr -d '\r')" >> "$R"
  { echo "journal datatransport / réseau (extraits) :"; timeout 30 adb logcat -d 2>/dev/null | grep -iE "TRuntime|CctTransportBackend|datatransport|EACCES|Permission denied.*socket|SecurityException" | grep -v "E2e" | tail -15; } >> "$R"
  adb shell am force-stop com.etix
  collect
  exit 0
fi

if [ "$MODE" = "maj" ]; then
  # Phase A (base) : parcours et budget avec les tests de la base elle-même (thème laissé tel que ses tests le
  # laissent : clair pour le lot 8, préférence enregistrée pour le lot 9)
  RUN_SRC=base/app/src/androidTest/java run com.etix.e2e.E2eParcoursTest
  RUN_SRC=base/app/src/androidTest/java run com.etix.e2e.E2eBudgetAvantMajTest
  adb shell am force-stop com.etix
  # Instantané des données de la base (tests de cette version, lecture seule, avant la mise à jour)
  T 300 adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk >> "$OUT/install.txt" 2>&1
  run com.etix.e2e.E2eMajInstantaneAvantTest
  adb shell am force-stop com.etix
  # Mise à jour base → cette version sans désinstallation
  echo "base : $BASE_LABEL" >> "$OUT/device.txt"
  pkginfo > "$OUT/update_avant.txt"
  T 300 adb install -r dist/app-A.apk > "$OUT/update_install.txt" 2>&1
  pkginfo > "$OUT/update_apres.txt"
  adb shell am force-stop com.etix
  # Phase B (cette version) : mêmes tickets, budgets et thème, session conservée ; Réglages affichant le thème conservé
  run com.etix.e2e.E2eMajInstantaneApresTest
  run com.etix.e2e.E2ePersistanceTest
  run com.etix.e2e.E2eBudgetApresMajTest
  run com.etix.e2e.E2eMajReglagesApresTest
  collect
  exit 0
fi

# Phase A : parcours complet sur app neuve
run com.etix.e2e.E2eParcoursTest
# Lot 7 : budget saisi AVANT la mise à jour (stockage additif à conserver)
run com.etix.e2e.E2eBudgetAvantMajTest
adb shell am force-stop com.etix

# Mise à jour A → B sans désinstallation (même clé de signature, versionCode + 1)
pkginfo > "$OUT/update_avant.txt"
T 300 adb install -r dist/app-B.apk > "$OUT/update_install.txt" 2>&1
pkginfo > "$OUT/update_apres.txt"

# Isolation QA (sur émulateur) : état de com.etix avant/après installation d'eTix QA
adb shell dumpsys package com.etix | grep -E "lastUpdateTime|versionName" | tr -d '\r' > "$OUT/etix_avant_qa.txt"
T 300 adb install app/build/outputs/apk/qa/app-qa.apk > "$OUT/install_qa.txt" 2>&1
adb shell pm list packages | grep -i etix | tr -d '\r' > "$OUT/packages.txt"
adb shell dumpsys package com.etix | grep -E "lastUpdateTime|versionName" | tr -d '\r' > "$OUT/etix_apres_qa.txt"
adb shell monkey -p com.etix.qa -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1
sleep 6
adb exec-out screencap -p > "$OUT/shots/api${API}_28_eTixQA_premier_lancement.png"
adb shell am force-stop com.etix.qa

# Temps de démarrage à froid après mise à jour (mesure seule ; l'app est ensuite de nouveau arrêtée)
{ echo "démarrage à froid après mise à jour :"; timeout 60 adb shell am start -W -n com.etix/.SplashActivity 2>&1 | grep -E "TotalTime|WaitTime|Status" | tr -d '\r'; } >> "$OUT/device.txt"
sleep 3; adb shell am force-stop com.etix

# Phase B : après mise à jour A→B + processus tué + QA installée à côté → données com.etix intactes
run com.etix.e2e.E2ePersistanceTest
run com.etix.e2e.E2eBudgetApresMajTest

# Phase C : lot 4 (date, catégorie, filtres et recherche de l'Historique) — sans suppression
run com.etix.e2e.E2eLot4Test

# Phase D : lot 5 (écran Catégories)
run com.etix.e2e.E2eCategoriesTest

# Phase E : lot 6 (détail catégorie / ticket, suppression confirmée d'un ticket fictif créé par le test)
run com.etix.e2e.E2eDetailsTest

# Phase F : lot 7 (budgets mensuels)
run com.etix.e2e.E2eBudgetsTest

# Phase G : lot 8 (carte Budget de l'Accueil)
run com.etix.e2e.E2eAccueilBudgetTest

# Compatibilité : lecteur de dates OCR exécuté sur l'appareil (logique pure, aucune donnée touchée)
run com.etix.e2e.E2eCompatDatesOcrTest

# Phase H : lot 9 (parcours de scan, ML Kit réel ; appareil photo et sélecteur d'image simulés)
run com.etix.e2e.E2eScanTest

# Phase I : lot 10 (Réglages : thème, période par défaut, exports CSV, partage réel annulé, données conservées)
run com.etix.e2e.E2eReglagesTest
# Persistance des Réglages après arrêt complet de l'app (processus tué entre les deux classes)
run com.etix.e2e.E2eReglagesAvantRedemarrageTest
adb shell am force-stop com.etix
run com.etix.e2e.E2eReglagesApresRedemarrageTest

collect
exit 0
