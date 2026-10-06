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
per_file = {}  # nom de fichier -> {"PASS": n, "FAIL": n, "ERROR": n, "SKIP": n}
for f in sorted(glob.glob(f"{out}/instr_*.txt")):
    txt = read(f)
    counts = per_file.setdefault(os.path.basename(f), {"PASS": 0, "FAIL": 0, "ERROR": 0, "SKIP": 0})
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
                counts[state] += 1
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
def field(t, k):
    m = re.search(k + r"=([^\s]+(?: [0-9:]+)?)", t)
    return m.group(1) if m else "?"
same_first = field(ua, "firstInstallTime") == field(ub, "firstInstallTime") != "?"
if ua or ub:
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
# Sélecteur de date autour du seuil (mode revue) : annotation à part, découpée (3 800 caractères au plus chacune)
seuil = [l for l in mes.splitlines() if l.startswith("seuil ")]
mes = "\n".join(l for l in mes.splitlines() if not l.startswith("seuil "))
if mes:
    notice("Clavier petit écran (mesures)", mes)
part, parts = "", []
for l in seuil:
    if len(esc(part + l + "\n")) > 3800:
        parts.append(part); part = ""
    part += l + "\n"
if part:
    parts.append(part)
for i, p in enumerate(parts[:3], 1):
    notice(f"Sélecteur de date au seuil (mesures {i} sur {min(len(parts), 3)})", p.strip())

# Lot 9 : mesures et constats du scanner, regroupés en une annotation (limite de 10 notices par étape)
scan_parts = []
for fname, title in (("mesures_scan.txt", "Petit écran (mesures)"), ("scan_images.txt", "Images"),
                     ("scan_systeme.txt", "Applications du système"), ("scan_hors_ligne.txt", "Hors ligne"),
                     ("maj_donnees.txt", "Mise à jour : données"), ("apk_permissions.txt", "Permissions de l'app installée"),
                     ("scan_reseau.txt", "Réseau")):
    t = read(f"{out}/shots/{fname}").strip()
    if t:
        scan_parts.append(f"== {title}\n{t}")
        print(f"--- {fname}\n{t}")          # journal complet (l'annotation est tronquée à 3800 caractères)
if scan_parts:
    notice("Scanner et mise à jour (constats)", "\n".join(scan_parts))

dem = read(f"{out}/shots/demarrage.txt").strip()
if dem:
    notice("Démarrages (état réel de l'app)", dem[:3500])

cd = read(f"{out}/shots/compat_dates.txt").strip()
if cd:
    notice("Compatibilité dates OCR", cd[:3500], "warning" if ("ERREUR" in cd or "ÉCART" in cd) else "notice")

nat = read(f"{out}/native.txt").strip()
if nat:
    for i in range(0, min(len(nat), 10500), 3500):
        notice(f"Plantages natifs {i//3500+1}", nat[i:i+3500], "error")

ech = read(f"{out}/shots/echec.txt").strip()
if ech:
    notice("Diagnostic des échecs (focus fenêtre)", ech[:3500], "warning")

to = read(f"{out}/timeouts.txt").strip()
if to:
    notice("Délais dépassés", to[:3500], "error")

crash = read(f"{out}/crashes.txt").strip()
notice("Plantages (logcat)", crash[:3500] if crash else "aucun FATAL EXCEPTION", "error" if crash else "notice")

# ---------------------------------------------------------------------------
# Attendus / observés : chaque classe lancée par emulator_e2e.sh (expected_runs.txt) doit avoir produit
# un résultat, avec autant de tests réussis que de @Test déclarés dans sa source (moins les @Ignore).
# ---------------------------------------------------------------------------
SRC = os.environ.get("ANDROID_TEST_SRC", "app/src/androidTest/java")

def declared(cls, src=None):
    """(@Test, @Ignore) déclarés dans le corps de la classe Kotlin `cls`, ou None si introuvable.
    `src` : sources d'une autre version (mode maj : tests de la version de base), sinon SRC."""
    for path in glob.glob(f"{src or SRC}/**/*.kt", recursive=True):
        txt = read(path)
        m = re.search(r"^(?:@\S+\s+)*class\s+" + re.escape(cls) + r"\b", txt, re.M)
        if not m:
            continue
        body = txt[m.end():]
        nxt = re.search(r"^(?:@RunWith|@FixMethodOrder|class |object |internal |private )", body, re.M)
        body = body[:nxt.start()] if nxt else body
        return len(re.findall(r"@Test\b", body)), len(re.findall(r"@Ignore\b", body))
    return None

expected_runs = [l.split() for l in read(f"{out}/expected_runs.txt").splitlines() if l.strip()]
table, exp_total, obs_total, mismatch = [], 0, 0, []
for parts in expected_runs:
    srcs = [p[4:] for p in parts[1:] if p.startswith("src=")]
    rest = [p for p in parts[1:] if not p.startswith("src=")]
    cls = parts[0].split(".")[-1]; suf = f"_{rest[0]}" if rest else ""
    fname = f"instr_{parts[0].replace('.', '_')}{suf}.txt"
    d = declared(cls, srcs[0] if srcs else None)
    c = per_file.get(fname)
    if d is None:
        mismatch.append(f"{cls}{suf} : classe introuvable dans les sources"); continue
    want = d[0] - d[1]; exp_total += want
    got = c["PASS"] if c else 0; obs_total += got
    state = "OK" if (c and got == want and c["FAIL"] == 0 and c["ERROR"] == 0) else "ÉCART"
    if c is None:
        mismatch.append(f"{cls}{suf} : résultat absent")
    elif state != "OK":
        mismatch.append(f"{cls}{suf} : attendu {want} réussis, observé {got} (échecs {c['FAIL'] + c['ERROR']})")
    table.append(f"{state} {cls}{suf} : attendu {want}, réussis {got}" + (f", échecs {c['FAIL'] + c['ERROR']}, ignorés {c['SKIP']}" if c else ", aucun résultat"))
notice("Attendus / observés", f"total attendu {exp_total}, réussis {obs_total}\n" + "\n".join(table) if table else "aucune exécution prévue enregistrée",
       "notice" if (table and not mismatch) else "error")

# Mise à jour A→B : versionCode croissant et pas de désinstallation (mode standard uniquement)
upd_bad = False
if ua or ub:
    upd_bad = not (same_first and field(ub, "versionCode") != "?" and field(ua, "versionCode") != "?"
                   and int(field(ub, "versionCode")) > int(field(ua, "versionCode")))

# Verdict lu par .github/scripts/emu_verdict.sh (le job échoue sinon)
reasons = []
if not expected_runs:
    reasons.append("aucune exécution prévue enregistrée")
if not rows:
    reasons.append("aucun résultat de test")
nfail = sum(1 for r in rows if r.startswith(("FAIL", "ERROR")))
if nfail or fails:
    reasons.append(f"{max(nfail, len(fails))} échec(s)")
if mismatch:
    reasons.append("attendus/observés : " + " ; ".join(mismatch))
if crash or nat:
    reasons.append("plantage (logcat)")
if to:
    reasons.append("délai dépassé")
if upd_bad:
    reasons.append("mise à jour A→B non conforme")
os.makedirs(out, exist_ok=True)
with open(f"{out}/verdict.txt", "w") as fh:
    fh.write("OK" if not reasons else "ÉCHEC : " + ", ".join(reasons))
