#!/usr/bin/env bash
# Verdict d'un job émulateur : échoue (code 1) sauf si emu_report.py a écrit exactement « OK ».
# Usage : emu_verdict.sh <dossier de résultats> <libellé>
D="${1:-emu-out}"; L="${2:-?}"
V=$(cat "$D/verdict.txt" 2>/dev/null || echo "ÉCHEC : rapport absent (verdict.txt non produit)")
echo "verdict ($L) : $V"
if [ "$V" != "OK" ]; then
  echo "::error title=Verdict émulateur ($L)::$V"
  exit 1
fi
