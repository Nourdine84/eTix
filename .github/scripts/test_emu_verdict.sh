#!/usr/bin/env bash
# Auto-test du verdict émulateur : chaque scénario synthétique passe par la vraie chaîne
# emu_report.py → emu_verdict.sh ; le code de sortie doit être celui attendu (0 = job vert, 1 = job rouge).
# Ce script réussit quand le mécanisme se comporte correctement : il ne laisse aucun job en échec volontaire.
set -u
HERE="$(cd "$(dirname "$0")" && pwd)"
WORK="$(mktemp -d)"; ERR=0; TABLE=""
while read -r NAME WANT; do
  D="$WORK/$NAME"; python3 "$HERE/emu_verdict_scenarios.py" gen "$NAME" "$D"
  if [ "$NAME" != "rapport_absent" ]; then
    ANDROID_TEST_SRC="$D/src" python3 "$HERE/emu_report.py" "$D/emu-out" "$NAME" > "$D/report.log"
  fi
  bash "$HERE/emu_verdict.sh" "$D/emu-out" "$NAME" > "$D/verdict.log" 2>&1; GOT=$?
  V=$(head -1 "$D/verdict.log" | sed 's/^verdict ([^)]*) : //')
  if [ "$GOT" = "$WANT" ]; then R="conforme"; else R="NON CONFORME"; ERR=1; fi
  TABLE="$TABLE$NAME : sortie $GOT (attendu $WANT) — $R — $V%0A"
done < <(python3 "$HERE/emu_verdict_scenarios.py" list)
echo "::$([ $ERR = 0 ] && echo notice || echo error) title=Auto-test du verdict émulateur::$TABLE"
printf '%b' "${TABLE//%0A/\\n}"
exit $ERR
