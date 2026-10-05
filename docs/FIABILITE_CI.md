# Fiabilité de la CI Android

Workflow unique : `.github/workflows/android-ci.yml` (« Android CI (PR APK) »), déclenché sur `push`
(`dev`, `feature/**`, `chore/**`, `fix/**`) et `pull_request` vers `dev`, `main`, `feature/android-v2`.
Workflow de démonstration isolé : `verdict-demo.yml`, **uniquement** sur la branche `demo/ci-verdict`
(jamais proposée à la fusion).

## 1. Défaut corrigé : jobs émulateur verts malgré des tests en échec

### Cause

Depuis l'ajout des tests sur émulateur (lot 3, 29/09/2026, commit `edce6b7`), `emulator_e2e.sh` exécutait
chaque classe de test sans en propager le résultat et se terminait sans erreur. L'étape « Report » ne faisait
qu'écrire des annotations. Le **statut vert d'un job émulateur ne prouvait donc rien** jusqu'au commit
`837f4e1` (30/09/2026, lot 8) ; seules les annotations « Tests émulateur » (lignes PASS / FAIL) et
« Plantages (logcat) » donnaient le résultat réel.

Non concernés : le job `build` (Gradle échoue si un test JVM / Robolectric échoue), les jobs `preview`
(publication d'images, sans test).

### Anciens résultats concernés (audit du 01/10/2026)

Audit par l'API GitHub de **tous les runs du workflow existants** (128 runs, 55 avec jobs émulateur,
239 jobs émulateur) : rapprochement du statut de chaque job avec ses annotations.

| Catégorie | Jobs |
|---|---|
| Sans étape de verdict, verts, annotations sans échec | 130 |
| Sans étape de verdict, **verts malgré des tests en échec, un plantage, un délai dépassé ou aucun résultat** | **62** (28 runs) |
| Sans étape de verdict, rouges (9) ou annulés (6) — le problème était déjà visible | 15 |
| Sans étape de verdict, verts sans annotation (copies de jobs lors d'une relance partielle) | 7 |
| Avec étape de verdict (runs antérieurs à l'audit) : verts et propres / rouges sur problème réel | 23 / 2 |

Les 62 jobs « verts trompeurs », tous sur des commits intermédiaires de développement :

| Run | Branche | Commit | Date (UTC) | Jobs verts malgré un problème |
|---|---|---|---|---|
| [36590067495](https://github.com/Nourdine84/eTix/actions/runs/36590067495) | `android-lot3-accueil-theme` | `edce6b7` | 2026-09-29 15:25 | emulator (34, google_apis) : 2 réussis / 6 en échec |
| [36592316145](https://github.com/Nourdine84/eTix/actions/runs/36592316145) | `android-lot3-accueil-theme` | `dd2b4fa` | 2026-09-29 15:43 | emulator (34, google_apis) : 7 réussis / 1 en échec |
| [36594484314](https://github.com/Nourdine84/eTix/actions/runs/36594484314) | `android-lot4-formulaire-historique` | `706ab08` | 2026-09-29 16:00 | emulator (34, google_apis) : 3 réussis / 8 en échec |
| [36594536862](https://github.com/Nourdine84/eTix/actions/runs/36594536862) | `android-lot4-formulaire-historique` | `3cae2f5` | 2026-09-29 16:00 | emulator (34, google_apis, x86_64) : 3 réussis / 8 en échec |
| [36595889130](https://github.com/Nourdine84/eTix/actions/runs/36595889130) | `android-lot4-formulaire-historique` | `b2a8648` | 2026-09-29 16:11 | emulator-api34 : 7 réussis / 4 en échec |
| [36597706287](https://github.com/Nourdine84/eTix/actions/runs/36597706287) | `android-lot4-formulaire-historique` | `eba128d` | 2026-09-29 16:26 | emulator-api21 : 0 réussis / 3 en échec (plantage, délai) |
| [36599665207](https://github.com/Nourdine84/eTix/actions/runs/36599665207) | `android-lot3-accueil-theme` | `a4e1347` | 2026-09-29 16:42 | emulator (34, google_apis) : 5 réussis / 3 en échec |
| [36599669943](https://github.com/Nourdine84/eTix/actions/runs/36599669943) | `android-lot4-formulaire-historique` | `05bd9f4` | 2026-09-29 16:42 | emulator-api21 : 3 réussis / 8 en échec |
| [36613893760](https://github.com/Nourdine84/eTix/actions/runs/36613893760) | `android-lot5-categories` | `1a30eac` | 2026-09-29 18:41 | emulator-api21 : 12 réussis / 2 en échec ; emulator-api34 : 12 réussis / 2 en échec ; emulator-api34-fr : 0 réussis / 1 en échec (délai) ; emulator-api36 : 9 réussis / 5 en échec |
| [36614394342](https://github.com/Nourdine84/eTix/actions/runs/36614394342) | `android-lot5-categories` | `337edb2` | 2026-09-29 18:45 | emulator-api21 : 12 réussis / 2 en échec ; emulator-api34 : 12 réussis / 2 en échec ; emulator-api34-fr : 0 réussis / 0 en échec (délai, aucun résultat) ; emulator-api36 : 9 réussis / 5 en échec |
| [36615624815](https://github.com/Nourdine84/eTix/actions/runs/36615624815) | `android-lot5-categories` | `7c28211` | 2026-09-29 18:56 | emulator-api34-fr : 3 réussis / 1 en échec |
| [36617478203](https://github.com/Nourdine84/eTix/actions/runs/36617478203) | `android-lot5-categories` | `f08ffd0` | 2026-09-29 19:11 | emulator-api34 : 12 réussis / 2 en échec |
| [36618970067](https://github.com/Nourdine84/eTix/actions/runs/36618970067) | `android-lot5-categories` | `f9df7ea` | 2026-09-29 19:24 | emulator-api36 : 13 réussis / 1 en échec |
| [36621008595](https://github.com/Nourdine84/eTix/actions/runs/36621008595) | `android-lot5-categories` | `959f41e` | 2026-09-29 19:41 | emulator-api36 : 13 réussis / 1 en échec |
| [36627961963](https://github.com/Nourdine84/eTix/actions/runs/36627961963) | `android-lot6-fiabilisation` | `2f39beb` | 2026-09-29 20:40 | emulator-api34 : 13 réussis / 1 en échec ; emulator-api36-petit : 0 réussis / 6 en échec |
| [36629217490](https://github.com/Nourdine84/eTix/actions/runs/36629217490) | `android-lot6-fiabilisation` | `ffa04cd` | 2026-09-29 20:50 | emulator-api36-petit : 2 réussis / 4 en échec |
| [36630124659](https://github.com/Nourdine84/eTix/actions/runs/36630124659) | `android-lot6-fiabilisation` | `bf3a2df` | 2026-09-29 20:58 | emulator-api36 : 12 réussis / 2 en échec ; emulator-api36-petit : 2 réussis / 4 en échec |
| [36631237084](https://github.com/Nourdine84/eTix/actions/runs/36631237084) | `android-lot6-fiabilisation` | `5741104` | 2026-09-29 21:08 | emulator-api21 : 13 réussis / 4 en échec ; emulator-api34 : 13 réussis / 4 en échec ; emulator-api36 : 13 réussis / 4 en échec ; emulator-api36-petit : 2 réussis / 4 en échec |
| [36631292574](https://github.com/Nourdine84/eTix/actions/runs/36631292574) | `android-lot6-fiabilisation` | `2087d49` | 2026-09-29 21:08 | emulator-api21 : 13 réussis / 4 en échec ; emulator-api34 : 13 réussis / 4 en échec ; emulator-api36 : 13 réussis / 4 en échec ; emulator-api36-petit : 2 réussis / 4 en échec |
| [36632203892](https://github.com/Nourdine84/eTix/actions/runs/36632203892) | `android-lot6-fiabilisation` | `a2e42a1` | 2026-09-29 21:16 | emulator-api36-petit : 5 réussis / 1 en échec |
| [36640759058](https://github.com/Nourdine84/eTix/actions/runs/36640759058) | `android-lot7-budgets` | `152d65d` | 2026-09-29 22:38 | emulator-api21 : 21 réussis / 1 en échec ; emulator-api34 : 21 réussis / 1 en échec ; emulator-api36 : 21 réussis / 1 en échec |
| [36641665027](https://github.com/Nourdine84/eTix/actions/runs/36641665027) | `android-lot7-budgets` | `74dd4c9` | 2026-09-29 22:47 | emulator-api21 : 21 réussis / 1 en échec ; emulator-api34 : 21 réussis / 1 en échec ; emulator-api36 : 21 réussis / 1 en échec ; emulator-api36-petit : 8 réussis / 1 en échec |
| [36642465095](https://github.com/Nourdine84/eTix/actions/runs/36642465095) | `android-lot7-budgets` | `aad19fa` | 2026-09-29 22:56 | emulator-api21 : 21 réussis / 1 en échec ; emulator-api34 : 21 réussis / 1 en échec ; emulator-api36 : 21 réussis / 1 en échec ; emulator-api36-petit : 8 réussis / 1 en échec |
| [36642473284](https://github.com/Nourdine84/eTix/actions/runs/36642473284) | `android-lot7-budgets` | `7ba0f85` | 2026-09-29 22:56 | emulator-api21 : 21 réussis / 1 en échec ; emulator-api34 : 21 réussis / 1 en échec ; emulator-api36 : 21 réussis / 1 en échec ; emulator-api36-petit : 8 réussis / 1 en échec |
| [36643424775](https://github.com/Nourdine84/eTix/actions/runs/36643424775) | `android-lot7-budgets` | `b0954a5` | 2026-09-29 23:06 | emulator-api21 : 21 réussis / 1 en échec ; emulator-api34 : 21 réussis / 1 en échec ; emulator-api36 : 21 réussis / 1 en échec ; emulator-api36-petit : 8 réussis / 1 en échec |
| [36700391736](https://github.com/Nourdine84/eTix/actions/runs/36700391736) | `android-lot8-accueil-budget` | `d91801f` | 2026-09-30 10:06 | emulator-api21 : 7 réussis / 8 en échec (plantage) ; emulator-api34 : 24 réussis / 2 en échec ; emulator-api36 : 24 réussis / 2 en échec ; emulator-api36-petit : 9 réussis / 3 en échec |
| [36701339818](https://github.com/Nourdine84/eTix/actions/runs/36701339818) | `android-lot8-accueil-budget` | `b12fa3a` | 2026-09-30 10:15 | emulator-api21 : 7 réussis / 8 en échec (plantage) ; emulator-api34 : 24 réussis / 2 en échec ; emulator-api36 : 24 réussis / 2 en échec ; emulator-api36-petit : 10 réussis / 2 en échec |
| [36722937007](https://github.com/Nourdine84/eTix/actions/runs/36722937007) | `android-lot8-accueil-budget` | `21adde7` | 2026-09-30 13:37 | emulator-api21 : 26 réussis / 2 en échec |

**Résultats de référence publiés dans les comptes rendus** : tous lus dans les annotations (tests réellement
réussis), et l'audit confirme 0 échec, 0 plantage pour chacun :

| Lot | Run de référence | Commit | Tests réussis (annotations) |
|---|---|---|---|
| 4 | [36601028467](https://github.com/Nourdine84/eTix/actions/runs/36601028467) | `b7fd557` | API 34 11/11, API 21 11/11 |
| 5 | [36619891801](https://github.com/Nourdine84/eTix/actions/runs/36619891801) | `747c558` | API 21/34/36 14/14, fr 4/4 |
| 6 | [36635957810](https://github.com/Nourdine84/eTix/actions/runs/36635957810) | `095727b` | API 21/34/36 17/17, fr 4/4, petit écran 6/6 |
| 7 | [36645961271](https://github.com/Nourdine84/eTix/actions/runs/36645961271) | `aa8758a` | API 21/34/36 22/22, fr 5/5, petit écran 9/9 |
| Fusion #75 | [36695648030](https://github.com/Nourdine84/eTix/actions/runs/36695648030) | `8c1af76` | API 21/34/36 23/23, fr 5/5 (tentative 2), petit écran 9/9 |
| 8 | [36703532983](https://github.com/Nourdine84/eTix/actions/runs/36703532983) | `b48f5d0` | API 21/34/36 26/26, fr 6/6, petit écran 12/12 |

Ces nombres restent valables ; ce qui était faux, c'est le seul statut vert des runs intermédiaires.

## 2. Mécanisme corrigé

1. `emulator_e2e.sh` consigne chaque classe lancée (`expected_runs.txt`).
2. `emu_report.py` compare, pour chaque classe, les tests réussis aux `@Test` déclarés dans sa source
   (moins les `@Ignore`), relève les échecs, les résultats absents ou vides, l'arrêt de l'instrumentation
   (« Native crash », « Process crashed »), les plantages Java (`FATAL EXCEPTION`) et natifs (`Fatal signal`),
   les délais dépassés et, en mode standard, la mise à jour A→B (versionCode croissant, pas de
   désinstallation). Annotation « Attendus / observés » et fichier `verdict.txt`.
3. Étape finale « Verdict tests émulateur » (`emu_verdict.sh`) dans chaque job émulateur : échoue sauf si
   le verdict vaut exactement « OK » (rapport absent = échec).

### Démonstrations

- **Auto-test permanent** (job `verdict-autotest`, à chaque run) : 10 scénarios synthétiques au format réel
  de `am instrument -r` passent par la vraie chaîne `emu_report.py` → `emu_verdict.sh` ; le job réussit si
  chaque code de sortie est celui attendu (0 pour « ok », 1 pour : test en échec, résultat vide, fichier
  absent, aucun test exécuté, test manquant, plantage Java, plantage natif, délai dépassé, rapport absent).
  Aucun échec volontaire n'est laissé dans la branche proposée.
- **Jobs réellement rouges, contexte isolé** : branche `demo/ci-verdict` (commit `0ee7868`), run
  [36886144608](https://github.com/Nourdine84/eTix/actions/runs/36886144608) : mêmes étapes Report puis
  Verdict, sans inversion. Résultat : job `ok` vert ; jobs `test_en_echec`, `resultat_absent_fichier`,
  `resultat_absent_vide`, `aucun_test_execute`, `plantage_java`, `plantage_natif` **rouges**, à l'étape
  Verdict uniquement.
- **Cas réels** : run 36724140825 (API 21, plantages natifs `SIGSEGV`) et run 36887839853 tentative 1
  (API 24, installation de l'APK bloquée > 300 s) : jobs rouges.

## 3. Autres contrôles du run

- **Tests JVM / Robolectric** (annotation « Bilan tests JVM ») : `@Test` déclarés dans `app/src/test`,
  tests découverts par Gradle, exécutés (réussis / échecs), ignorés (`@Ignore`, listés). Le job échoue si un
  test déclaré n'est pas découvert.
- **Version livrable** : l'APK construit par le job `build` (sans propriété de test) doit porter les valeurs
  de `app/build.gradle.kts`, sinon échec.
- **Lint NewApi** : bloquant (voir `COMPATIBILITE_ANDROID.md`).

## 4. Builds A et B (test de mise à jour)

| Build | Où | Commande | versionCode / versionName |
|---|---|---|---|
| Livrable (et « A ») | job `build`, jobs émulateur | `assembleDebug` / `assembleQa`, sans propriété | **10 / `1.8.1-lot8`** (`-qa` pour le QA) |
| « B » (test seulement) | jobs émulateur standard, `dist/app-B.apk` | `assembleDebug -PetixVersionCodeOffset=1 -PetixVersionNameSuffix=-maj` | 11 / `1.8.1-lot8-maj` |

Le build B n'est jamais publié ni téléversé comme artefact ; les propriétés valent 0 / vide par défaut. Le
contrôle « Version livrable » vérifie à chaque run que l'APK livrable porte 10 / `1.8.1-lot8`.

## 5. Ce que compte un run

Exemple : run 36887839853 (tentative 1) = **315 jobs**, dont **11 jobs de test** (`build`, 5 jobs émulateur,
4 jobs `emulator-compat-dates`, `verdict-autotest`) et **304 jobs `preview`** qui publient chacun une capture
en annotation (85 Robolectric, 219 émulateur). Le nombre de jobs n'est pas un nombre de tests.

## 6. Aperçus à la demande (branche `chore/ci-apercus-a-la-demande`, PR #77)

- Les captures complètes restent publiées à chaque run en artefacts, avant le verdict et même en cas d'échec :
  `screenshots` (Robolectric) et `emulator-*` (émulateur, avec les résultats bruts des tests), conservés 30 jours.
  Une annotation par job indique le nombre de captures et l'artefact.
- Les aperçus en annotations (lisibles via l'API, 4 captures par job) ne sont produits que si la PR porte le
  label **`apercus`**. Poser le label après l'ouverture relance le workflow (événement `labeled`), sans commit ;
  tant que le label reste posé, chaque nouveau push de la PR produit aussi les aperçus. Les autres labels ne
  lancent aucun job. Les runs de `push` n'en produisent jamais.
- Captures d'échec (`zz_echec`) : toujours publiées par le job de test lui-même, avec ou sans label.
- Inchangés : tests, verdict, auto-tests, permissions (`contents: read`).
- **Revue visuelle** : pour toute modification d'interface, poser `apercus` et examiner les aperçus concernés avant
  de déclarer la revue visuelle effectuée.
- Relance d'un seul job : les aperçus restent consultables dans la tentative d'origine (les jobs recopiés dans la
  nouvelle tentative n'ont pas d'annotations).

Mesures (commit `ebe9cd7`) :

| Parcours | Run | Jobs exécutés | Captures publiées en annotations |
|---|---|---|---|
| Avant (PR #76, `9926185`) | 36890321511 | 315 (11 de test + 304 d'aperçu) | 304 |
| PR ouverte, sans label | [36895506486](https://github.com/Nourdine84/eTix/actions/runs/36895506486) | **11** (+ 6 aperçus ignorés) | 0 (captures en artefacts) |
| Push de la branche | [36895485490](https://github.com/Nourdine84/eTix/actions/runs/36895485490) | **11** (+ 6 ignorés) | 0 |
| Label « QA » ajouté | [36895560872](https://github.com/Nourdine84/eTix/actions/runs/36895560872) | **0** (14 ignorés) | 0 |
| Label `apercus` ajouté après l'ouverture | [36895642561](https://github.com/Nourdine84/eTix/actions/runs/36895642561) | **89** (11 de test + 78 d'aperçu) | 304, au plus 42 annotations par job |

Preuves en cas d'échec : run de démonstration [36895728587](https://github.com/Nourdine84/eTix/actions/runs/36895728587)
(branche `demo/ci-verdict`, `e0aa782`) — les 6 jobs rouges ont leurs annotations et leur artefact (30 jours) ;
cas réel : `emulator-compat-dates (25)` du run 36895642561, rouge, avec annotations et artefact.

## 7. Suites « Vercel » et « claude » en attente (constat en lecture seule)

Sur chaque commit poussé depuis le premier run (`8dedd75`), GitHub crée une suite de contrôles pour chaque
application GitHub installée ayant la permission « checks » : **Vercel** (application de l'éditeur Vercel) et
**Claude** (application GitHub d'Anthropic). Aucune des deux n'a jamais publié de contrôle sur ce dépôt (0 contrôle,
statut « queued »), aucun déploiement ni statut n'est enregistré. Elles ne bloquent pas les fusions (PR #75 et #76
« clean »). Origine probable : applications installées sur le compte sans projet Vercel ni workflow Claude
configuré pour ce dépôt — non vérifiable d'ici (réglages d'installation et protections de branche non lisibles).
Rien n'a été désactivé ni supprimé.

## Limites

- Les journaux complets et les artefacts ne sont pas téléchargeables depuis cette session (HTTP 403) ; l'audit
  repose sur les annotations, conservées par GitHub.
- Échecs d'environnement possibles (focus perdu au démarrage ; installation de l'APK bloquée plus de 300 s sur
  API 24 une fois et API 25 deux fois le 01/10/2026, cause non établie) : désormais rouges, à relancer et
  consigner, jamais à ignorer.
- Même blocage le 02/10/2026 : API 24 (fusion du lot 9, run 36994446604) et API 25 (PR #80, run 37020784489,
  1ʳᵉ tentative : « Performing Streamed Install » sans réponse pendant 300 s, émulateur déclaré démarré 9 s après
  son lancement ; relance ciblée verte). Cause toujours inconnue : un lien avec l'APK n'est pas exclu ; un émulateur
  pas encore réellement prêt reste une hypothèse.
- Après la fusion du lot 10 (02/10/2026, `feature/android-v2` `f102cfe`, run 37027739385, 1ʳᵉ tentative) : API 24,
  `app-A.apk` installé (« Success »), puis installation de l'APK de tests `app-debug-androidTest.apk` sans réponse
  pendant 300 s ; émulateur déclaré démarré environ 12 s après son lancement, juste après un « device offline » ;
  journal système relevé s'arrêtant sur des événements Wi-Fi du démarrage ; aucun test exécuté. Relance ciblée
  unique : verte (2 / 2). **Cause inconnue**, mêmes réserves : lien avec l'APK non exclu, émulateur pas encore prêt =
  hypothèse non démontrée. Total au 02/10/2026 : API 24 trois fois, API 25 trois fois. Proposition si cela se
  reproduit, avant tout correctif : consigner l'état du gestionnaire de paquets (`dumpsys package`, `pm list
  packages`) et l'horodatage de `sys.boot_completed` au moment du blocage, pour départager les deux pistes.

## 04/10/2026 — `emulator-api36-petit`, « Clavier non affiché » (PR #81, `42eaeec`)

- Run 37229126796, 1ʳᵉ tentative : un seul échec, `E2eClavierPetitEcranTest.k01` (passe a, premier test de la passe
  sur émulateur neuf) : clavier non affiché ; 31 / 32 réussis. Commit ne modifiant que `docs/REVUE_VISUELLE.md` (code
  de l'app et des tests identique à `8895dc8`, vert ; même test vert sur `df0740a`).
- Une seule relance ciblée (2ᵉ tentative) : 32 / 32. **Cause inconnue** ; clavier pas encore prêt sur l'émulateur =
  hypothèse non démontrée.
