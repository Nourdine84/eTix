# Validation sur émulateur — lots 1 à 8

**Ce n'est pas une validation sur téléphone physique.** Émulateurs Android officiels (Google) dans GitHub Actions,
données fictives uniquement, build de test signé avec la clé de développement du runner
(jamais publié, **pas** l'APK QA durable).

## Environnements

| | Récent | Minimum pris en charge (`minSdk 21`) |
|---|---|---|
| Android | 14 (API 34) | 5.0.2 (API 21) |
| Image | `google_apis` x86_64, `sdk_gphone64_x86_64` | `default` x86 (32 bits), « Android SDK built for x86 » |
| Clavier | Gboard / LatinIME, langue en-US | LatinIME, langue en-US |

Note : l'image API 21 **64 bits** n'a jamais démarré en CI (> 35 min) ; l'image 32 bits a été retenue.

## Résultat de référence — commit `b7fd557` (branche `feature/android-lot4-formulaire-historique`)

Run : https://github.com/Nourdine84/eTix/actions/runs/36601028467

| Niveau | Contenu | Résultat |
|---|---|---|
| JVM + Robolectric (`testDebugUnitTest`) | Règles Magasins / Accueil / formulaire / Historique ; navigation, actions protégées, captures simulées | 40 réussis, 1 désactivé (`OCRValidationTest`) |
| **Émulateur API 34** | 11 tests de bout en bout | **11/11** |
| **Émulateur API 21** | 11 tests de bout en bout | **11/11** |
| Plantages (logcat `FATAL EXCEPTION`) | API 34 / API 21 | aucun / aucun |
| Isolation eTix QA à côté de `com.etix` | `lastUpdateTime` avant/après installation | identique sur les deux |

### Tests de bout en bout (Espresso, vraie interface, vrai clavier)

| Test | Couvre |
|---|---|
| a01 | Premier lancement : onboarding → connexion factice → Accueil vide |
| a02 | États vides (Accueil, Ajouter, Historique, Catégories, Magasins), Scanner et « Vider tous les tickets » désactivés et sans effet |
| a03 | Ajout avec **« 12,50 » tapé au clavier** → Accueil 12,50 € |
| a04 | Historique → détail → modification (15,75) → Retour, pile correcte |
| a05 | Magasins : Ce mois / Aujourd'hui / Cette année, fiche magasin, montants cohérents |
| a06 | Défilement de l'Accueil ; bouton Enregistrer atteignable clavier ouvert |
| a07 | Thème sombre puis clair (Réglages, Accueil, Historique, Magasins, Ajouter) |
| Persistance | **Après `am force-stop`** et installation d'eTix QA : session et ticket 15,75 € intacts |
| c01 | Lot 4 : ajout avec date (sélecteur), catégorie « Carburant », description |
| c02 | Lot 4 : sections, recherche « carbu », filtre date de début, réinitialisation |
| c03 | Lot 4 : édition préremplie, catégorie libre « Péage fictif », reprise dans le sélecteur |

## Lot 8 — carte Budget de l'Accueil (branche `feature/android-lot8-accueil-budget`)

Commit testé : **`b48f5d0`** — run https://github.com/Nourdine84/eTix/actions/runs/36703532983 (dernier commit de code).

| Niveau | Résultat |
|---|---|
| JVM + Robolectric | **146 réussis, 0 échec, 10 désactivés** (dont `BudgetSummaryEngineTest` : sans budget, 49,99 / 50 / 80 / 100 %, dépassement, budget partagé compté une fois, 31/08 → 01/09, jours restants, 3 lignes max, « budget tendu ») |
| Émulateur API 36 / 34 / 21 | **26/26** chacun |
| Émulateur API 34 fr-FR | **6/6** (carte Budget en français) |
| Émulateur API 36 petit écran | **12/12** (carte Budget à 360 dp police 1,3 et 2,0, 320 dp police 1,3) |
| Mise à jour A → B | API 21, 34, 36 : versionCode 9 → 10, tickets, catégories et budgets conservés |
| Plantages | aucun (après correction) |
| Lint NewApi (nouveau, non bloquant) | 8 signalements, tous dans `OCRDateExtractor` (java.time, API 26), code existant non atteignable aujourd'hui |

Défauts trouvés sur émulateur / aperçus et corrigés :
- **Plantage de l'Accueil sur Android 5** dès qu'un budget existe (`HashMap.putIfAbsent`, API 24), run 36700391736 → `da8aa93` ;
- fond teinté transparent laissant voir l'ombre en gris (Android 14) → couleurs opaques (`b48f5d0`) ;
- « 200 » et « € » séparés en fin de ligne à police 2,0 → espace insécable (`b12fa3a`).

Changement de mois : vérifié en JVM seulement (horloge de l'émulateur non modifiée).
Aperçus : `docs/preview/lot8/` (Robolectric) et `docs/preview/lot8-emulateur/`.

## Lot 9 — scanner (branche `feature/android-lot9-scanner`)

Commit de code testé : **`897e536`**, résultat de fusion proposé de la PR #78 (cible `457f49e`) — run
https://github.com/Nourdine84/eTix/actions/runs/36930688922 (label `apercus` : 95 jobs dont 84 d'aperçu, tous verts).

| Niveau | Attendu | Observé |
|---|---|---|
| JVM + Robolectric | 201 `@Test` déclarés | 201 découverts, 200 réussis, 0 échec, 1 ignoré (`OCRValidationTest`, obsolète) |
| Émulateur API 21 / 34 / 36 | 33 chacun (29 + `E2eScanTest` 4) | 33 chacun ; ML Kit réel sur l'image de test, formulaire prérempli (ESSO, 23,45, 12/01/2026, Carburant), enregistrement après correction, rien détecté |
| Émulateur API 34 fr-FR | 6 | 6 |
| Émulateur API 36 petit écran | 18 (6 × 3 passes, dont `k07` scanner) | 18 |
| Lecteur de dates API 22 à 25 | 2 chacun | 2 chacun |
| Mise à jour A → B | versionCode 11 → 12 sans désinstallation | conforme (API 21, 34, 36), aucun plantage |

Défauts trouvés en cours de lot (CI, aperçus) et corrigés :
- toute image jugée « introuvable » (lecture des dimensions mal testée) — tests Robolectric, `9ec6e46` ;
- boutons du scanner hors de l'écran sur petit écran / grande police → barre basse toujours visible, `0fb922b` ;
- bandeau de vérification écrasé lettre par lettre à 320 dp / police 2,0 → empilé, `80a055a` ;
- fonds du bandeau et des badges absents sur Android 5 (teinte ignorée) → fonds construits directement, `897e536`.

Aperçus : `docs/preview/lot9/` (Robolectric) et `docs/preview/lot9-emulateur/`.

## Lot 8 — revue avant fusion : fiabilité CI, résultat de fusion (01/10/2026)

Résultat de fusion proposé de la PR #76 (`refs/pull/76/merge`, source `1540145`, cible `8c1af76`) — run
https://github.com/Nourdine84/eTix/actions/runs/36887839853, avec le verdict corrigé (`docs/FIABILITE_CI.md`).

| Job (lien) | Attendu | Observé |
|---|---|---|
| [build](https://github.com/Nourdine84/eTix/actions/runs/36887839853/job/110455589790) — JVM + Robolectric | 170 `@Test` déclarés et découverts, 160 exécutés, 10 `@Ignore` | 160 réussis, 0 échec, 10 ignorés ; lint NewApi : aucune nouvelle incompatibilité ; version livrable 10 / `1.8.1-lot8` |
| [emulator-api21](https://github.com/Nourdine84/eTix/actions/runs/36887839853/job/110455590508) | 29 | 29 réussis, mise à jour 10 → 11 conforme, aucun plantage |
| [emulator-api34](https://github.com/Nourdine84/eTix/actions/runs/36887839853/job/110455590381) | 29 | 29 |
| [emulator-api36](https://github.com/Nourdine84/eTix/actions/runs/36887839853/job/110455590571) | 29 | 29 |
| [emulator-api34-fr](https://github.com/Nourdine84/eTix/actions/runs/36887839853/job/110455590244) | 6 | 6 |
| [emulator-api36-petit](https://github.com/Nourdine84/eTix/actions/runs/36887839853/job/110455589943) | 15 (5 × 3 passes) | 15 |
| emulator-compat-dates API [22](https://github.com/Nourdine84/eTix/actions/runs/36887839853/job/110455590437) / [23](https://github.com/Nourdine84/eTix/actions/runs/36887839853/job/110455590125) / [24](https://github.com/Nourdine84/eTix/actions/runs/36887839853/job/110461982250) / [25](https://github.com/Nourdine84/eTix/actions/runs/36887839853/job/110455590331) | 2 chacun | 2 chacun (API 24 : 1re tentative rouge, installation bloquée > 300 s, [relance](https://github.com/Nourdine84/eTix/actions/runs/36887839853/job/110461982250) verte) |
| [verdict-autotest](https://github.com/Nourdine84/eTix/actions/runs/36887839853/job/110455590168) | 10 scénarios conformes | 10 conformes |

Parcours standard (29 par API) par lot : lots 1-3 `E2eParcoursTest` (7) et `E2ePersistanceTest` (1) ;
lot 4 `E2eLot4Test` (3) ; lot 5 `E2eCategoriesTest` (3) ; lot 6 `E2eDetailsTest` (3) ; lot 7
`E2eBudgetAvantMajTest`, `E2eBudgetApresMajTest` (1 + 1), `E2eBudgetsTest` (4) ; lot 8 `E2eAccueilBudgetTest` (4),
`E2eCompatDatesOcrTest` (2). Français : `E2eFrancaisTest` (6, lots 4 à 8). Petit écran : `E2eClavierPetitEcranTest`
(5 × 3, lots 6 à 8).

Défaut de test trouvé le 01/10/2026 (premier jour du mois) : `Lot6ScreenshotTest.details_*` supposait le ticket
Lidl plus récent que les autres ; le 1er du mois, tous tombent le même jour → échec (run 36886132461). Données de
test corrigées et attente Robolectric désormais en échec explicite à l'expiration (`1540145`). Code de l'app inchangé.

## Lot 8 — durcissement (compatibilité, « Budget atteint », Tendance 6 mois)

Commit de code testé : **`3129344`** — run https://github.com/Nourdine84/eTix/actions/runs/36725776665.

| Niveau | Résultat |
|---|---|
| JVM + Robolectric | **160 réussis, 0 échec, 10 désactivés** (dont équivalence stricte du lecteur de dates avec java.time, « Budget atteint », libellés de la Tendance à 360 dp / 320 dp, police 1,0 / 1,3 / 2,0) |
| Lint NewApi (désormais **bloquant**) | aucune nouvelle incompatibilité ; 2 exceptions documentées (`docs/COMPATIBILITE_ANDROID.md`) |
| Émulateur API 36 / 34 / 21 | **29/29** chacun, dont lecteur de dates OCR sur l'appareil (11 cas + traitement complet) et Tendance 6 mois |
| Émulateur API 34 fr-FR | **6/6** |
| Émulateur API 36 petit écran | **15/15** (Tendance : libellés entiers à 360 dp police 1,3 et 2,0, 320 dp police 1,3) |
| Mise à jour A → B | API 21, 34, 36 : versionCode 10 → 11 sans désinstallation, données conservées |
| Plantages | aucun |

Compatibilité java.time, exécutions réelles :

| Commit | Configuration | API 21 | API 34 / 36 |
|---|---|---|---|
| `21adde7` (run 36722937007) | code d'origine, sans désucrage | **11/11 cas en `NoClassDefFoundError`** (DateTimeFormatter), traitement OCR complet idem | 11/11 conformes |
| `50e1558` / `b3a7fa7` (runs 36724140825, 36725178848) | désucrage `desugar_jdk_libs` 2.0.4 | dates conformes, mais **plantages natifs `SIGSEGV`** de l'app pendant 5 classes de test | conformes |
| `3129344` (run 36725776665) | lecteur sans java.time, désucrage retiré | **11/11 conformes, 29/29 tests, aucun plantage** | conformes |

Les jobs émulateur échouent désormais si un test échoue, si aucun résultat n'est produit ou en cas de plantage
(étape « Verdict tests émulateur ») ; auparavant ils restaient verts et seules les annotations signalaient les échecs.

Mesures Tendance 6 mois (largeur du libellé / largeur disponible, taille par rapport à la taille prévue) :
360 dp police 2,0 → libellés iOS entiers réduits à 94 % ; 320 dp police 1,3 → entiers à 100 %.
Robolectric 320 dp police 2,0 → repli sur 3 lettres (« JUN », « JUL », « AOÛ », « SEP »), non atteint sur émulateur.

Aperçus : `docs/preview/lot8/l8_09_atteint_*`, `l8_10` à `l8_12` (Robolectric), `docs/preview/lot8-emulateur/*tendance*`.

## Incident CI après fusion de la PR #75 (30/09/2026)

Run 36695648030 (commit de fusion `8c1af76` sur `feature/android-v2`), tentative 1 : le job `emulator-api34-fr`
a échoué à l'étape **« Build APKs (A, B, tests, QA de développement) »** (compilation Gradle), avant tout test ;
les 11 aperçus dépendants ont échoué faute d'artefact. Les autres jobs, compilant le même code, ont réussi.
Tentative 2 (relance des jobs en échec) : réussie (fr 5/5).
**Cause : inconnue.** Les journaux ne sont pas accessibles depuis cette session (téléchargement refusé, HTTP 403) et
l'annotation ne contient que « Process completed with exit code 1 ». La relance réussie ne démontre pas une panne
d'environnement. À revoir si le journal devient accessible (onglet Actions du run, job `emulator-api34-fr`,
tentative 1) ou si l'échec se reproduit.

## Lot 7 — budgets (branche `feature/android-lot7-budgets`)

Commit testé : **`aa8758a`** — run https://github.com/Nourdine84/eTix/actions/runs/36645961271 (dernier commit de code).

| Niveau | Environnement | Résultat |
|---|---|---|
| JVM + Robolectric | runner Ubuntu | **121 réussis, 0 échec, 10 désactivés** |
| Émulateur API 36 / 34 / 21 | en-US | **22/22** chacun |
| Émulateur API 34 fr-FR | clavier français | **5/5** (dont budget « 12,5 » → 12,50 € et « 1 200 » → 1 200 €) |
| Émulateur API 36 petit écran | 360 dp police 1,3 et 2,0 ; 320 dp police 1,3 | **9/9** |
| Mise à jour A → B | API 21, 34, 36 | budget « Autre » 20,50 € saisi avec le build A, conservé après `install -r` + processus tué ; tickets intacts |
| Plantages | 5 émulateurs | aucun |

Budgets vérifiés sur émulateur : dépassement (« Dépassé — 129% », 20 € / 15,50 €), attention (« Attention — 83% »),
catégorie sans budget (pas de barre), « Cette année » sans barre, champ prérempli « 15,50 », « Annuler » sans effet,
suppression d'UN budget (l'autre intact), thème sombre. « Appliquer » de la saisie mesuré au-dessus du clavier et
réellement touché dans les 3 passes petit écran (barre haute : y ≈ 56–179 px, clavier à partir de 774–810 px).

Défauts trouvés et corrigés pendant le lot (émulateur / aperçus) :
- fenêtre de dialogue de saisie recouverte par le clavier (320 dp / 1,3 ; police 2,0) → écran à barre haute ;
- barre Material 3 avec point final et espace (≠ capsule iOS) ;
- titre « Catégori/es » coupé et libellés de période tronqués à police 2,0 → taille automatique.

Changement de mois : vérifié en JVM (`BudgetRulesTest.changement_de_mois`, bornes 31/03 23:59 → 01/04 00:00), **pas**
sur émulateur (horloge système non modifiée).

## Lot 6 — fiabilisation, détails (branche `feature/android-lot6-fiabilisation`)

Commit testé : **`095727b`** — run https://github.com/Nourdine84/eTix/actions/runs/36635957810
(dernier commit de code ; les commits suivants ne sont que de la documentation). Données fictives, clé de test du runner.

| Niveau | Environnement | Résultat |
|---|---|---|
| JVM + Robolectric | runner Ubuntu | **93 réussis, 0 échec, 10 désactivés** (`docs/TESTS_DESACTIVES.md`) |
| Émulateur API 36 / 34 / 21 | en-US | **17/17** chacun (+ détails catégorie/ticket, suppression confirmée) |
| Émulateur API 34 fr-FR | français | **4/4** |
| Émulateur API 36 petit écran | 3 passes taille/police | **6/6** |
| Mise à jour A → B sans désinstallation | API 21, 34, 36 | versionCode 7 → 8, `firstInstallTime` inchangé, tickets conservés |
| Plantages | 5 émulateurs | aucun |

### Clavier ouvert, petit écran, grande police (API 36, Android 16)

Espresso `isDisplayed` ignore la fenêtre du clavier : le test **mesure** la position du bouton par rapport au haut du
clavier (insets IME), puis **touche** le bouton et vérifie l'enregistrement (base) ou le montant affiché (modification).

| Passe | Écran | Police | Écran | Bouton avant défilement | Après défilement | Haut du clavier | Accessible | Toucher |
|---|---|---|---|---|---|---|---|---|
| a | 360×616 dp | 1,3 | Ajouter | 1182–1286 px (sous le clavier) | 546–650 | 810 | oui | enregistré |
| a | 360×616 dp | 1,3 | Modifier | 1158–1262 | 546–650 | 810 | oui | 5,30 € affiché |
| b | 360×616 dp | 2,0 | Ajouter | 1359–1463 | 546–650 | 810 | oui | enregistré |
| b | 360×616 dp | 2,0 | Modifier | 1442–1546 | 546–650 | 810 | oui | 5,30 € affiché |
| c | 320×544 dp | 1,3 | Ajouter | 1202–1319 | 477–594 | 774 | oui | enregistré |
| c | 320×544 dp | 1,3 | Modifier | 1288–1405 | 477–594 | 774 | oui | 5,30 € affiché |

Conclusion : le bouton Enregistrer est **masqué** par le clavier à l'ouverture, **atteignable par défilement** dans
les 6 cas ; aucune correction d'interface nécessaire pour ce point. La barre d'onglets reste visible au-dessus du
clavier (réduit l'espace utile sur petit écran — à juger sur téléphone).

Constats en cours de route :
- 1er caractère perdu (« 4,20 » → « ,20 » → ticket à 0,20 €) : frappe **injectée par le test** avant la fin
  d'ouverture du clavier ; corrigé dans le test (frappe après stabilisation + vérification du champ). Un humain ne peut
  pas taper avant l'apparition du clavier : **non considéré comme un défaut de l'app**, mais le formulaire accepte
  « ,20 » = 0,20 € (règle de saisie iOS reprise) — à confirmer.
- **Défaut d'interface corrigé** : détail d'un ticket à police 2,0 (360 dp) et 1,3 (320 dp) — « DATE D'ACHAT »,
  « MAGASIN », « CATÉGORIE », « Mardi » coupés lettre par lettre. Cartes empilées quand largeur / police < 300
  (`e1feb6c`, `095727b`).
- **Défaut d'interface corrigé** : « Évolution journalière » — dates superposées et dernière date rognée (`e1feb6c`).

### Échecs intermittents (sans plantage)

« Fenêtre sans focus » au 1er écran après démarrage à froid : 4 fois sur 42 exécutions standard (14 runs × API 21/34/36), uniquement sur API 34 et 36. Diagnostic ajouté
(capture + `dumpsys window`) : au moment du diagnostic, l'app a bien le focus → lenteur de démarrage à froid après
mise à jour (TotalTime mesuré : API 21 432 ms, API 34 839 ms, API 36 1 262 ms dans un run réussi). Attente du
1er écran portée à 30 s (`a2e42a1`) ; non reproduit sur les runs suivants (échantillon faible).
**Lot 7** : le délai rallongé est remplacé par une attente fondée sur l'**état réel** de l'app (`E2e.waitForAppReady` :
activité eTix au premier plan, puis fenêtre avec le focus ; temps publiés dans l'annotation « Démarrages »). En cas
d'échec, le verdict distingue « APP » (aucune activité au premier plan) de « SYSTÈME » (au premier plan sans focus).
**Cause non démontrée** : ni défaut de test ni défaut d'application n'est retenu à ce stade ; diagnostics conservés.

## Lot 5 — résultat de référence (branche `feature/android-lot5-categories`)

Commit de code testé : **`747c558`** — run https://github.com/Nourdine84/eTix/actions/runs/36619891801
(code de l'application identique depuis `1a30eac` ; les commits suivants ne touchent que tests et CI. Premier run entièrement vert : `4308a16`, run 36616597248.)

| Niveau | Environnement | Résultat |
|---|---|---|
| JVM + Robolectric | runner Ubuntu | **63 réussis, 0 échec, 11 désactivés** (OCR bloqués, voir `OCR_CAS_DE_REFERENCE.md`) |
| Émulateur **API 36** (Android 16, nouveau) | `google_apis` x86_64, en-US | **14/14** |
| Émulateur API 34 (Android 14) | `google_apis` x86_64, en-US | **14/14** |
| Émulateur API 21 (Android 5.0.2) | `default` x86, en-US | **14/14** |
| Émulateur **API 34 en français** (nouveau) | `google_apis` x86_64, langue système `fr-FR` vérifiée (`Locale.getDefault() = fr`) | **4/4** |
| Plantages (logcat) | 4 émulateurs | aucun |
| **Mise à jour A → B sans désinstallation** | API 21, 34, 36 | versionCode 6 → 7, même certificat pour A et B, `firstInstallTime` inchangé, tickets fictifs et session conservés (`E2ePersistanceTest`) |
| Isolation eTix QA | API 21, 34, 36 | `lastUpdateTime` de `com.etix` identique avant/après (non exécutée dans le job français, « identique: False » y signifie « non mesuré ») |

Déroulé standard : parcours (app neuve, build A) → arrêt → **`adb install -r` du build B** (même code, versionCode + 1,
même clé de test) → installation d'eTix QA à côté → persistance → lot 4 → Catégories (lot 5). La clé est celle du
runner CI (tests), **pas** la clé QA durable ; sur téléphone, la même vérification reste à faire avec la clé QA.

### Tests ajoutés

| Test | Couvre |
|---|---|
| f01 (fr-FR) | « 12,50 » tapé au clavier français → champ « 12,50 », date du jour au format français (« 29 sept. 2026 »), Historique « 12,50 € » |
| f02 (fr-FR) | Sélecteur de date en saisie « 15/MM/aaaa » (jour > 12 : pas d'ambiguïté jour/mois) → « 15 sept. 2026 » à l'écran et dans l'Historique |
| f03 (fr-FR) | Filtre Historique du J-1 au J-1 : J-1 00:00:00.000 et J-1 23:59:59.999 **inclus**, J-2 23:59:59.999 et J 00:00:00.000 **exclus** |
| f04 (fr-FR) | Magasins : Aujourd'hui inclut J 00:00, exclut J-1 23:59:59.999 ; Ce mois inclut le 1er 00:00, exclut la veille 23:59:59.999 |
| d01 | Catégories, Ce mois : lignes par catégorie exacte, variation +100 % (hausse), −75 % (baisse), aucune (pas de période précédente) ; aucun ticket modifié |
| d02 | Cette année (ticket de janvier présent) puis Aujourd'hui (absent) |
| d03 | Catégories en thème sombre puis retour au clair |

### Défauts trouvés en cours de lot (tests, pas l'application)

| Constat | Cause | Correction |
|---|---|---|
| `-change-locale fr-FR` sans effet (langue restée en-US) et installation interrompue (« Broken pipe ») | redémarrage du framework pendant l'installation | langue posée par `persist.sys.locale` + redémarrage contrôlé avant installation (`7c28211`) |
| a04 échoue sur API 36 | bouton Enregistrer sous la ligne de flottaison, attendu « affiché » avant défilement | attente du champ montant (`7c28211`) |
| d02/d03 : correspondance multiple | la légende de l'anneau contient aussi les noms | cibler les lignes (`7c28211`) |
| f03 : ticket non trouvé | présence vérifiée dans la partie visible seulement | recherche dans toute la liste (`4308a16`) |
| **Échec intermittent** API 34, run 36617478203 (`f08ffd0`, code app = `4308a16`) : a01 « fenêtre sans focus » au 1er lancement, puis persistance en cascade (session non ouverte) ; 12/14, aucun plantage | environnement émulateur probable (dialogue système au démarrage) — **non prouvé** | réveil / déverrouillage / fermeture des dialogues avant les tests + focus consigné (`f9df7ea`) ; non reproduit au run suivant (36618970067 : API 34 14/14) |
| **Échec intermittent** API 36, run 36618970067 (`f9df7ea`) : a06, bouton Enregistrer non visible après défilement clavier ouvert (1 fois sur 4 runs API 36) | défilement lancé pendant l'animation d'ouverture du clavier (hypothèse) — **non prouvé** ; si réel, le bouton reste atteignable en défilant à nouveau | attente de la fin d'ouverture du clavier avant défilement (`747c558` : 14/14) ; **à vérifier sur téléphone Android 16** |

Aperçus : `docs/preview/lot5-emulateur/` (`api21/34/36-categories.jpg` : vide, mois, année, aujourd'hui, sombre ;
`api34-fr-FR.jpg`). Aperçus Robolectric clair/sombre : `docs/preview/lot5/`.

## Défauts trouvés par les émulateurs (invisibles en JVM/Robolectric) et corrigés

| # | Défaut | Où | Gravité | Correctif |
|---|---|---|---|---|
| E1 | **Virgule supprimée au clavier** : « 12,50 » → « 1250 » → ticket enregistré **1 250,00 €** | API 34 (lots 1-3) | Critique (données fausses) | `dd2b4fa` |
| E2 | **Plantage à l'ouverture de l'Accueil sur Android 5** (`RadialGradient radius must be > 0`) | API 21 (lot 3) | Critique | `05bd9f4` (lot 4), `a4e1347` (lot 3) |
| E3 | Engrenage Réglages / retour fiche magasin : 25 % de la zone tactile rognée | API 34 | Moyenne (accessibilité) | `dd2b4fa` |
| E4 | Sélecteur de catégorie : « Autre… » hors écran (3 boutons empilés) | API 34 (lot 4) | Moyenne (fonction inaccessible) | `eba128d` |
| E5 | Icônes de la barre d'état invisibles (blanc sur blanc) | API 21-22 | Moyenne | `d9f4e74` |
| E6 | Barre d'état bleu vif en thème sombre | toutes | Faible | `3ec9b5b` |
| E7 | Halo de l'Accueil à bords nets, libellés d'onglets tronqués (320 dp) | API 21 | Faible | `d9f4e74` |
| E8 | Texte de recherche tronqué | 360 dp | Faible | `eba128d` |

E5/E7 ne sont **pas** reportés sur la branche du lot 3 (dépendance à un fichier du lot 4 ; report annulé sans forcer).

## Aperçus (captures réelles d'émulateur)

`docs/preview/lot4-emulateur/` : `api34-*` et `api21-*` (démarrage/états vides, ajout à virgule/modification,
Magasins/persistance, thème sombre, formulaire lot 4, Historique lot 4). Captures pleine résolution : artefacts
`emulator-api34` / `emulator-api21` du run.

## Limites

- Émulateurs standard en anglais ; le français est couvert par le job `emulator-api34-fr` (Android 14 uniquement), pas encore sur API 21/36 ni sur téléphone.
- Clavier : saisie Espresso (événements clavier injectés), pas une frappe réelle sur le clavier AZERTY à l'écran.
- Pas de test sur tablette, grand écran, pliable, ni Android 6 à 13 intermédiaires.
- Pas de mesure de performance ni de batterie ; pas de TalkBack réel (libellés d'accessibilité posés, non écoutés).
- Suppression d'un ticket non couverte par ces parcours (hors périmètre demandé).

## Restant à faire sur téléphone physique

`PARCOURS_VALIDATION_RAPIDE.md` (installation côte à côte, ticket à virgule, navigation, modification, Magasins,
thème sombre, persistance), avec l'APK QA durable une fois la signature configurée.
