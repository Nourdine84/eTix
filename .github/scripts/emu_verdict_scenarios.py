"""Scénarios synthétiques pour éprouver le verdict des jobs émulateur (aucun émulateur, aucune donnée).

Usage : emu_verdict_scenarios.py gen <scénario> <dossier>   → écrit <dossier>/emu-out et <dossier>/src
        emu_verdict_scenarios.py list                       → « scénario code_attendu » par ligne
Les sorties reproduisent le format réel de `am instrument -w -r` et les fichiers écrits par emulator_e2e.sh.
"""
import os, sys

CLS = "com.etix.e2e.E2eDemoTest"
SRC = """package com.etix.e2e

@RunWith(AndroidJUnit4::class)
class E2eDemoTest {
    @Test fun t1() {}
    @Test fun t2() {}
}
"""

def status(test, code, stack=None):
    lines = [f"INSTRUMENTATION_STATUS: class={CLS}", "INSTRUMENTATION_STATUS: current=1",
             "INSTRUMENTATION_STATUS: id=AndroidJUnitRunner", "INSTRUMENTATION_STATUS: numtests=2",
             "INSTRUMENTATION_STATUS: stream=", f"INSTRUMENTATION_STATUS: test={test}", "INSTRUMENTATION_STATUS_CODE: 1",
             f"INSTRUMENTATION_STATUS: class={CLS}", "INSTRUMENTATION_STATUS: current=1",
             "INSTRUMENTATION_STATUS: id=AndroidJUnitRunner", "INSTRUMENTATION_STATUS: numtests=2"]
    if stack:
        lines += [f"INSTRUMENTATION_STATUS: stack={stack[0]}"] + stack[1:]
    lines += ["INSTRUMENTATION_STATUS: stream=", f"INSTRUMENTATION_STATUS: test={test}", f"INSTRUMENTATION_STATUS_CODE: {code}"]
    return lines

OK_END = ["INSTRUMENTATION_RESULT: stream=", "", "Time: 1.2", "", "OK (2 tests)", "", "", "INSTRUMENTATION_CODE: -1"]
FAIL_STACK = ["java.lang.AssertionError: attendu 2 mais 3", "\tat com.etix.e2e.E2eDemoTest.t2(E2eDemoTest.kt:6)"]

# scénario -> (code de sortie attendu du verdict, contenu)
SCENARIOS = {
    "ok":                     0,
    "test_en_echec":          1,
    "resultat_absent_vide":   1,
    "resultat_absent_fichier":1,
    "aucun_test_execute":     1,
    "test_manquant":          1,
    "plantage_java":          1,
    "plantage_natif":         1,
    "delai_depasse":          1,
    "rapport_absent":         1,
}

def gen(name, root):
    out = os.path.join(root, "emu-out"); src = os.path.join(root, "src", "com", "etix", "e2e")
    os.makedirs(os.path.join(out, "shots"), exist_ok=True); os.makedirs(src, exist_ok=True)
    open(os.path.join(src, "E2eDemoTest.kt"), "w").write(SRC)
    open(os.path.join(out, "device.txt"), "w").write(f"scénario synthétique {name}\n")
    open(os.path.join(out, "expected_runs.txt"), "w").write(CLS + " \n")
    instr = os.path.join(out, f"instr_{CLS.replace('.', '_')}.txt")
    ok = status("t1", 0) + status("t2", 0)
    if name in ("ok", "plantage_java", "delai_depasse", "rapport_absent"):
        body = ok + OK_END
    elif name == "test_en_echec":
        body = status("t1", 0) + status("t2", -2, FAIL_STACK) + ["INSTRUMENTATION_RESULT: stream=", "",
               "FAILURES!!!", "Tests run: 2,  Failures: 1", "", "INSTRUMENTATION_CODE: -1"]
    elif name == "resultat_absent_vide":
        body = []
    elif name == "resultat_absent_fichier":
        body = None
    elif name == "aucun_test_execute":
        body = ["INSTRUMENTATION_RESULT: stream=", "", "Time: 0", "", "OK (0 tests)", "", "INSTRUMENTATION_CODE: -1"]
    elif name == "test_manquant":
        body = status("t1", 0) + ["INSTRUMENTATION_RESULT: stream=", "", "OK (1 test)", "", "INSTRUMENTATION_CODE: -1"]
    elif name == "plantage_natif":
        body = status("t1", 0) + ["INSTRUMENTATION_RESULT: shortMsg=Native crash",
               "INSTRUMENTATION_RESULT: longMsg=Native crash", "INSTRUMENTATION_CODE: 0"]
        open(os.path.join(out, "native.txt"), "w").write(
            f"== {CLS}\nF/libc    ( 2692): Fatal signal 11 (SIGSEGV), code 1, fault addr 0x28 in tid 2692 (com.etix)\n")
    if body is not None:
        open(instr, "w").write("\n".join(body) + ("\n" if body else ""))
    if name == "plantage_java":
        open(os.path.join(out, "crashes.txt"), "w").write(
            "1234:E/AndroidRuntime( 2692): FATAL EXCEPTION: main\n1235:E/AndroidRuntime( 2692): java.lang.IllegalStateException\n")
    if name == "delai_depasse":
        open(os.path.join(out, "timeouts.txt"), "w").write("délai dépassé (720 s) : adb shell am instrument …\n")

if __name__ == "__main__":
    if sys.argv[1] == "list":
        for k, v in SCENARIOS.items(): print(k, v)
    elif sys.argv[1] == "gen":
        gen(sys.argv[2], sys.argv[3])
