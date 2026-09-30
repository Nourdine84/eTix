"""Résumé des tests émulateur en annotations GitHub (lisibles via l'API checks)."""
import glob, os, re, sys

out = sys.argv[1] if len(sys.argv) > 1 else "emu-out"
label = sys.argv[2] if len(sys.argv) > 2 else "?"

def read(p):
    return open(p, errors="replace").read() if os.path.exists(p) else ""

def esc(s):
    return s.replace("%", "%25").replace("\r", "").replace("\n", "%0A")

def notice(title, body, level="notice"):
    body = esc(body)[:3800]
    print(f"::{level} title={title} ({label})::{body}")

notice("Émulateur", read(f"{out}/device.txt") + "\n" + read(f"{out}/install.txt")[-600:])

rows, fails = [], []
for f in sorted(glob.glob(f"{out}/instr_*.txt")):
    txt = read(f)
    cur = {}
    key = None
    for line in txt.splitlines():
        m = re.match(r"INSTRUMENTATION_STATUS: (\w+)=(.*)", line)
        if m:
            key = m.group(1); cur[key] = m.group(2); continue
        m = re.match(r"INSTRUMENTATION_STATUS_CODE: (-?\d+)", line)
        if m:
            code = int(m.group(1))
            name = f"{cur.get('class','?').split('.')[-1]}.{cur.get('test','?')}"
            if code in (0, -1, -2, -3):
                state = {0: "PASS", -2: "FAIL", -1: "ERROR", -3: "SKIP"}[code]
                rows.append(f"{state} {name}")
                if code in (-1, -2):
                    stack = cur.get("stack", "").strip().splitlines()
                    causes = [l.strip() for l in stack if l.strip().startswith("Caused by")]
                    fails.append(f"{name}: " + " | ".join(stack[:2] + causes[:3]))
            cur, key = {}, None
            continue
        if key == "stack" and not line.startswith("INSTRUMENTATION"):
            cur["stack"] = cur.get("stack", "") + "\n" + line
    if "INSTRUMENTATION_RESULT: shortMsg" in txt or "Process crashed" in txt:
        fails.append(f"{os.path.basename(f)}: " + " ".join(l for l in txt.splitlines() if "shortMsg" in l or "crashed" in l)[:500])
    if not txt.strip():
        fails.append(f"{os.path.basename(f)}: sortie vide")

notice("Tests émulateur", "\n".join(rows) or "Aucun résultat")
if fails:
    for i in range(0, len(fails), 3):
        notice(f"Échecs émulateur {i//3+1}", "\n".join(fails[i:i+3]), "error")

ua, ub = read(f"{out}/update_avant.txt").strip(), read(f"{out}/update_apres.txt").strip()
if ua or ub:
    import re as _re
    def field(t, k):
        m = _re.search(k + r"=([^\s]+(?: [0-9:]+)?)", t)
        return m.group(1) if m else "?"
    same_first = field(ua, "firstInstallTime") == field(ub, "firstInstallTime") != "?"
    notice("Mise à jour A→B sans désinstallation",
           f"certificats (A / B) :\n{read(f'{out}/update_certs.txt').strip()}\n"
           f"install -r : {read(f'{out}/update_install.txt').strip()[-160:]}\n"
           f"avant : {ua}\naprès : {ub}\n"
           f"versionCode {field(ua,'versionCode')} → {field(ub,'versionCode')} ; "
           f"firstInstallTime inchangé (pas de désinstallation) : {same_first}")

avant, apres = read(f"{out}/etix_avant_qa.txt").strip(), read(f"{out}/etix_apres_qa.txt").strip()
if avant or apres:
  notice("Isolation QA (émulateur)",
         f"paquets installés:\n{read(f'{out}/packages.txt').strip()}\ninstall QA: {read(f'{out}/install_qa.txt').strip()[-200:]}\n"
         f"com.etix avant: {avant}\ncom.etix après: {apres}\nidentique: {avant == apres and bool(avant)}")

mes = read(f"{out}/shots/mesures_clavier.txt").strip()
if mes:
    notice("Clavier petit écran (mesures)", mes)

dem = read(f"{out}/shots/demarrage.txt").strip()
if dem:
    notice("Démarrages (état réel de l'app)", dem[:3500])

cd = read(f"{out}/shots/compat_dates.txt").strip()
if cd:
    notice("Compatibilité dates OCR", cd[:3500], "warning" if ("ERREUR" in cd or "ÉCART" in cd) else "notice")

ech = read(f"{out}/shots/echec.txt").strip()
if ech:
    notice("Diagnostic des échecs (focus fenêtre)", ech[:3500], "warning")

to = read(f"{out}/timeouts.txt").strip()
if to:
    notice("Délais dépassés", to[:3500], "error")

crash = read(f"{out}/crashes.txt").strip()
notice("Plantages (logcat)", crash[:3500] if crash else "aucun FATAL EXCEPTION", "error" if crash else "notice")

# Verdict lu par l'étape « Verdict tests émulateur » (le job échoue sinon)
reasons = []
if not rows:
    reasons.append("aucun résultat de test")
nfail = sum(1 for r in rows if r.startswith(("FAIL", "ERROR")))
if nfail or fails:
    reasons.append(f"{max(nfail, len(fails))} échec(s)")
if crash:
    reasons.append("plantage (logcat)")
if to:
    reasons.append("délai dépassé")
os.makedirs(out, exist_ok=True)
with open(f"{out}/verdict.txt", "w") as fh:
    fh.write("OK" if not reasons else "ÉCHEC : " + ", ".join(reasons))
