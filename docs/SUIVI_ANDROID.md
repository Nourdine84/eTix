# Suivi eTix Android — parité iOS

Document de reprise entre sessions. Concis : état, décisions, prochain lot.

## Dépôts et branches

| Élément | Emplacement |
|---|---|
| Code Android (référence) | `Nourdine84/eTix`, branche `feature/android-v2` (dernier commit 16/01/2026) |
| Branches de travail | Lot 1 `fix/android-v2-navigation` ; Lot 2 `feature/android-lot2-magasins` (contient lot 1) ; Lot 3 `feature/android-lot3-accueil-theme` (contient lots 1-2) ; Lot 4 `feature/android-lot4-formulaire-historique` ; Lot 5 `feature/android-lot5-categories` (depuis le lot 4 @ `c77f0ba`) ; Lot 6 `feature/android-lot6-fiabilisation` (depuis le lot 5 @ `959f41e`) ; Lot 7 `feature/android-lot7-budgets` (depuis le lot 6 @ `8172881`). **Lots 1 à 7 fusionnés dans `feature/android-v2`** (PR #75, commit de fusion `8c1af76`, 30/09/2026). **Lot 8 `feature/android-lot8-accueil-budget`** (depuis `8c1af76`) **fusionné dans `feature/android-v2`** (PR #76, commit de fusion `ef40e1f`, 01/10/2026). **Lot 9 `feature/android-lot9-scanner`** fusionné (PR #78, `86d9a19`, 02/10/2026). **Lot 10 `feature/android-lot10-reglages`** fusionné (PR #80, `f102cfe`, 02/10/2026) : `feature/android-v2` contient les lots 1 à 10. CI : `chore/ci-apercus-a-la-demande` (depuis `ef40e1f`). Branche de démonstration `demo/ci-verdict` (non destinée à la fusion). Anciennes branches conservées. `main` et `dev` jamais modifiés. |
| Historique Git | `feature/android-v2` et `dev` : ancêtre commun `ab7d6f8`, `dev` a 3 commits propres. `main` : racine distincte, sans ancêtre commun. Détail : `docs/REVUE_LOTS_1_2.md`. Intégration `dev`/`main` = décision séparée. |
| `Nourdine84/etix-android` | Squelette Gradle sans module `app` — **pas** le dépôt de dev |
| Copie locale Mac `~/AndroidStudioProjects/eTix` | Sur `dev` (19/12/2025), n'a pas `feature/android-v2` |
| Référence iOS | `Nourdine84/etix-ios` — `feature/home-hero-v2` (19/08/2026) ; `main` diverge (21 commits propres) |
| Maquettes validées | `eTix_V2_Maquettes_Completes_Validees_FINAL` (citées dans `Theme.swift` iOS) — **introuvables dans les sources accessibles** (3 dépôts, projet, dossiers Mac connectés), ce qui ne signifie pas qu'elles n'ont jamais existé ni été validées. Référence provisoire : code iOS. |

## Stack

Kotlin 1.9.22, AGP 8.2.2, Gradle 8.5, JDK 17, vues XML + ViewBinding, ViewPager2 + BottomNavigationView,
Room 2.6.1 (KAPT), Coroutines/Flow, ML Kit Text Recognition + CameraX. minSdk 21 / target 34.
Pas de backend : auth et données 100 % locales (SharedPreferences + Room).

Build : `./gradlew assembleDebug` (package `com.etix`) ou `./gradlew assembleQa` (package `com.etix.qa`, nom « eTix QA ») — tests : `./gradlew testDebugUnitTest`.
CI : `.github/workflows/android-ci.yml` (push sur `dev`, `feature/**`, `fix/**`, `chore/**`) → artefacts `eTix-QA-<version>.apk`, `app-debug-apk` (contient `app-debug.apk`), `screenshots`.

## Décisions

- Pas de migration d'architecture : on garde ViewPager2 + BottomNav ; `nav_graph.xml` n'est **pas** utilisé par l'app (aucun NavHost).
- Écrans poussés (détail / édition ticket) : `overlayContainer` au-dessus du ViewPager, bottom nav visible (équivalent TabBar iOS).
- Catégories (lot 5) : écran V2 `CategoryFragmentV2` branché ; l'écran V1 `fragments/CategoryFragment` reste dans le code, non branché (aucune suppression).
- Onglets (lot 2) : Accueil, Ajouter, Historique, Catégories, Magasins. Réglages via l'icône en haut de l'Accueil (BottomNavigationView limitée à 5).
- Tests manuels : **uniquement** avec l'APK QA (`com.etix.qa`), installé à côté de l'app. Jamais de désinstallation ni de réinitialisation de `com.etix` sans accord explicite.
- Tokens couleur V2 provisoires (`colors_v2.xml`, clair/sombre) repris des équivalents UIKit utilisés par iOS — à valider contre les maquettes.
- Magasins : regroupement insensible à la casse/espaces (écart assumé vs iOS, regroupement exact).
- `org.gradle.java.home` (chemin macOS) conservé dans `gradle.properties` pour ne pas casser le build local ; surchargé en CI.

## Journal des lots

### Lot 1 — Navigation (fix/android-v2-navigation)
- CI réparée (ligne `clear` parasite → YAML invalide depuis janvier ; JDK macOS forcé).
- Onglets Catégories / Paramètres : affichaient l'Accueil → écrans réels.
- Historique → Détail → Édition accessibles ; Retour Android : dépile, puis onglet → Accueil, puis quitte.
- Changer d'onglet ferme le détail ouvert.
- Crashs corrigés : Détail→Modifier (`requireParentFragment`), NPE binding après édition (collecte hors cycle de vie de la vue).
- Montants : virgule décimale acceptée (avant : refus à l'ajout, **0 € enregistré** à l'édition).
- Test unitaire OCR recompilable (`guessCategory` renommé) ; test Robolectric de navigation ajouté.
- Version : `1.1.0-lot1` (versionCode 2).
- CI verte (run 36574333574, commit 8dedd75) : APK compilé, 4 tests Robolectric navigation OK. **Non testé sur appareil.**

### Lot 2 — Magasins + Réglages (feature/android-lot2-magasins)
- Build QA : `applicationIdSuffix = ".qa"`, nom « eTix QA », versionNameSuffix `-qa` → installable à côté de `com.etix`, données séparées.
- CI : contrôle package/version/empreinte certificat, APK nommé `eTix-QA-<version>.apk`. (Signature : remplacée ensuite par la clé QA durable, voir ci-dessous.)
- Onglet Magasins (iOS `StoreListView`) : période Aujourd'hui/Ce mois/Cette année (défaut Ce mois), cartes classées (N°1-3), total, nb tickets, €/visite, part %, dernier passage, état vide.
- Fiche magasin (iOS `StoreDetailView`) : total, stats (panier moyen, tickets, dernière visite, fréquence), comparaison mensuelle, top 3 catégories, tickets (5 + « voir les autres ») → détail ticket.
- Réglages : icône engrenage sur l'Accueil → écran Réglages existant (inchangé), Retour → Accueil.
- Tests : `StoreStatsTest` (JVM), navigation mise à jour, captures `Lot2ScreenshotTest` (aperçu, pas une validation).
- Procédure de test sans risque pour `com.etix` : `docs/PROCEDURE_TEST_QA.md` ; plan de test : `docs/PLAN_TEST_LOT2.md`.
- CI : `ANDROID_USER_HOME` fixé (AGP rangeait la clé dans `~/.config/.android`, le cache ne la voyait pas → signature différente à chaque run).
- Corrections issues des captures : libellés de période tronqués ; libellés de tous les onglets visibles (comme iOS) ; barre d'onglets sombre en thème sombre ; texte invisible de 3 boutons Réglages (texte bleu sur fond bleu).
- Version : `1.2.0-lot2` (versionCode 3), APK QA `1.2.0-lot2-qa`.

### Signature QA durable (sur lot 2, reprise par le lot 3)
- `app/build.gradle.kts` : `signingConfig` QA alimentée uniquement par variables d'environnement ; `ETIX_QA_SIGNING_REQUIRED=true` fait échouer le build si la clé manque.
- CI : secrets `QA_KEYSTORE_B64` + `QA_KEYSTORE_PASSWORD` ; keystore hors workspace, supprimé en fin de job ; variable `QA_CERT_SHA256` → toute empreinte différente bloque la publication. Cache de clé supprimé du workflow. Plus d'APK `com.etix` publié.
- `GITHUB_TOKEN` limité à `contents: read`.
- Procédure : `docs/SIGNATURE_QA.md` ; mises à jour QA par `adb install -r`, aucune désinstallation prévue.

### Lot 3 — Accueil + thème sombre (feature/android-lot3-accueil-theme)
- Accueil porté depuis iOS `HomeView` : badge « e », titre, salutation/souhait selon l'heure, compteur, montant de période, chip delta (rouge hausse / vert baisse / neutre), narration (`FinancialStateEngine` iOS, mêmes seuils), sélecteur de période, tendance 6 mois + panier moyen, actions. Engrenage Réglages et « Voir l'historique » conservés.
- Logique pure `features/home/HomeStats.kt` + `HomeStatsTest` (seuils, maturité, libellés, tendance).
- Thème sombre : bleu primaire constant `#007BFF` + texte blanc sur boutons (le texte était noir), couleurs `md_*` sombres, total Catégories lisible, « Supprimer » contrasté.
- Actions indisponibles désactivées et signalées : « Vider tous les tickets », « Scanner un ticket » (Accueil et Ajouter). Aucune suppression globale branchée (`ProtectedActionsTest`).
- OCR : `OCRFixtureObservationTest` (observation sans assertion) ; proposition `docs/OCR_CAS_DE_REFERENCE.md`.
- Conformité aux maquettes validées **non confirmée** ; aperçus clair/sombre `docs/preview/lot3/`.
- Version `1.3.0-lot3` (versionCode 4).

### Validation sur émulateur (lots 1-3) et corrections
- Tests de bout en bout Espresso sur **vrais émulateurs** en CI (`emulator-api34`, `emulator-api21`), données fictives, captures réelles, extraction des plantages (logcat), isolation QA mesurée.
- Bugs trouvés sur émulateur et corrigés (`dd2b4fa`) :
  - **virgule supprimée au clavier** (API 34, clavier en-US) : « 12,50 » saisi → « 1250 » → ticket enregistré à 1 250,00 € ; champs montant acceptant désormais `,` et `.` ;
  - engrenage Réglages / retour fiche magasin : marge négative, 25 % de la zone tactile de 48 dp rognée.
- Lots 1-3 sur API 34 (Android 14) : 8/8 tests, aucun plantage (run 36593272554, `df5066c`).
- API 21 : image 64 bits jamais démarrée en CI → image 32 bits. **Plantage à l'ouverture de l'Accueil** trouvé et corrigé (`05bd9f4`).
- Résultat de référence (lots 1-4, `b7fd557`) : API 34 **11/11**, API 21 **11/11**, aucun plantage, isolation QA vérifiée sur les deux. Détail : `docs/VALIDATION_EMULATEUR.md`. **Pas une validation sur téléphone.**

### Lot 4 — Formulaire et Historique (feature/android-lot4-formulaire-historique)
- Formulaire partagé Ajout / Édition (iOS `TicketForm`) : magasin, montant (virgule ou point), date (sélecteur, heure conservée), catégorie (sélecteur iOS : 12 catégories système + catégories utilisées + « Autre… » libre, « Effacer »), description.
- Validation iOS : magasin non vide, montant > 0 ; montant en rouge si invalide.
- Édition préremplie (date, catégorie) ; suppression d'un ticket désormais **confirmée** (iOS `ConfirmDeletePopup`).
- Détail : date d'achat affichée.
- Historique (iOS `TicketHistoryView`) : recherche magasin/catégorie, filtre début/fin facultatifs (fin ≥ début), sections Aujourd'hui / Hier / Cette semaine / Ce mois / Plus ancien, plus récent d'abord, états « Aucun ticket » / « Aucun résultat » + « Effacer les filtres ». iOS n'a pas de sélecteur de tri : aucun ajouté. Export CSV Android conservé.
- **Aucune migration** : l'entité `Ticket` avait déjà date, catégorie, description (diff `model/` et `schemas/` vide). Seul ajout DAO : lecture `SELECT DISTINCT category`.
- Écarts assumés : catégorie non choisie → « Autre » (iOS : vide) pour l'écran Catégories V1 ; recherche Android n'inclut plus la description (alignement iOS).
- Version `1.4.0-lot4` (versionCode 5).

### Lot 5 — Catégories + couverture émulateur (feature/android-lot5-categories)
- Onglet Catégories porté depuis iOS `CategoryView` : sélecteur Aujourd'hui / Ce mois / Cette année (défaut Ce mois), anneau de
  répartition (palette iOS, variantes sombres), « Total » au centre, légende en % (1 décimale), lignes : total, variation vs
  période précédente (hausse rouge / baisse verte / absente si rien avant, comme iOS), part en % ; état vide « Aucun ticket sur
  cette période » ; clair et sombre.
- Lecture seule, regroupement par nom **exact** (comme iOS) : aucune catégorie fusionnée ni renommée, aucun ticket modifié
  (vérifié en Robolectric et sur émulateur). Ticket sans catégorie affiché « Sans catégorie » (donnée inchangée).
- Logique pure `features/category/CategoryStats.kt` + `CategoryStatsTest` (bornes, variations, division par zéro).
- Couverture émulateur : **API 36 (Android 16)** ajoutée ; **émulateur en français** (saisie « 12,50 », dates, sélecteur, filtres
  inclusifs, limites de période) ; **mise à jour A → B par `adb install -r`** sans désinstallation, tickets conservés.
- OCR : `OCRFixturesTest` (un test par fichier et par champ, blocages documentés), tickets **synthétiques** restaurant et long.
  Moteur et catégories inchangés ; Q1–Q4 restent des propositions.
- Version `1.5.0-lot5` (versionCode 6 ; build B de test : 7, suffixe `-maj`, jamais publié).
- Résultats : `docs/VALIDATION_EMULATEUR.md` (commit `747c558`, run 36619891801 ; code app identique depuis `1a30eac`) ; aperçus `docs/preview/lot5*`.
- Conformité aux maquettes validées **non confirmée** ; parité iOS de l'écran **partielle** (voir écarts).

#### Traçabilité commits ↔ exécutions CI (lot 5)

| Commit | Nature | Run CI | Résultat |
|---|---|---|---|
| `2244887` | Code de test/CI : émulateurs fr-FR et API 36, mise à jour A→B, tests OCR séparés | (non poussé seul) | — |
| `1a30eac` | Code app : écran Catégories V2 | 36613893760 | build échoué : aperçu Robolectric (test) |
| `337edb2` | Test : correction de l'aperçu | 36614394342 | JVM OK ; API 34 et 21 : 12/14, API 36 : 9/14 (tests à corriger, pas l'app) ; fr non exécuté (installation interrompue) |
| `7c28211` | Test/CI : langue fr, ciblage des lignes, API 36 | 36615624815 | 14/14 × 3 ; fr 3/4 |
| `4308a16` | Test fr f03 (**contient aussi** la mise à jour de `docs/OCR_CAS_DE_REFERENCE.md`) | 36616597248 | 14/14 × 3, fr 4/4, aucun plantage |
| `f08ffd0` | CI : bilan chiffré des tests JVM | 36617478203 | JVM 63 réussis / 11 désactivés ; API 36 14/14, API 21 14/14, fr 4/4 ; **API 34 12/14 (échec intermittent, voir validation)** |
| `f9df7ea` | CI : préparation de l'émulateur avant tests | 36618970067 | API 34 14/14, API 21 14/14, fr 4/4 ; **API 36 13/14 (a06 intermittent)** |
| `747c558` | Test a06 : attente de l'ouverture du clavier | **36619891801** | **référence finale** : JVM 63/0/11, API 36/34/21 14/14, fr 4/4, aucun plantage |
| (suivants) | Documentation uniquement | — | — |

### Lot 6 — Fiabilisation + détails (feature/android-lot6-fiabilisation)
- **OCR, défaut D1 corrigé** : le montant n'est plus jamais pris sur sous-total / HT / TVA / remise / rendu
  (`OCRAmountExtractor`, utilisé par `OCRProcessor` et `OCRTicketDraft`). Attente 40,80 du ticket long conservée et
  réactivée ; 18 cas de régression synthétiques. Catégories et enseigne non touchées (Q1–Q4 ouvertes).
- **Tests désactivés réconciliés** : 11 au lot 5 (10 + `OCRValidationTest` omis du compte rendu) → 10 au lot 6.
  Inventaire, type (décision produit / défaut moteur / obsolète) et conditions : `docs/TESTS_DESACTIVES.md`.
  Observation CI : 7 passeraient dès validation, 2 échoueraient (défauts moteur liés à Q1 et Q2), 1 obsolète.
- **Clavier / petit écran / grande police** (Android 16) : bouton Enregistrer mesuré sous le clavier puis atteint par
  défilement et réellement touché — 6/6 (Ajouter, Modifier ; 360 dp police 1,3 et 2,0 ; 320 dp police 1,3).
- **Détail d'un ticket** (iOS `TicketDetailView`) : montant, carte date, Magasin / Catégorie, note, Modifier,
  « Supprimer ce ticket » avec la même confirmation que l'édition (logique partagée), Retour.
  Adaptation Android : cartes empilées si écran étroit / grande police.
- **Détail d'une catégorie** (iOS `CategoryDetailView`) : depuis une ligne de l'onglet Catégories ; période, total,
  nombre de tickets, « Évolution journalière », tickets par jour → détail du ticket, état vide ; Retour.
- Aucune migration, aucune modification des catégories existantes ; « Autre » reste provisoire.
- Version `1.6.0-lot6` (versionCode 7 ; build B de test : 8).
- Résultats : `docs/VALIDATION_EMULATEUR.md` ; aperçus `docs/preview/lot6*`.

#### Traçabilité commits ↔ exécutions CI (lot 6)

| Commit | Nature | Run CI | Résultat |
|---|---|---|---|
| `ed702d0` | Code OCR (D1) + tests | 36627737568 | JVM 82/0/10 ; émulateurs 14/14 ×3 ; fr 4/4 |
| `2f39beb`, `ffa04cd`, `bf3a2df` | Test/CI clavier petit écran | 36627961963, 36629217490, 36630124659 | mise au point du test (erreurs de test, puis 1er caractère perdu) ; 1 échec intermittent focus |
| `9c926e0` | Code : détails ticket et catégorie | 36631237084 (avec `5741104`) | a04/c03 : « Modifier » sous la ligne de flottaison (tests à adapter) |
| `2087d49` | Test : observation des cas OCR désactivés | 36631292574 | observation publiée |
| `a2e42a1` | Tests adaptés | 36632203892 | 17/17 ×3, fr 4/4 ; petit écran : relancé (démarrage émulateur), 5/6 → **défaut police 2,0** |
| `e1feb6c`, `390b43b` | Code : grande police, graphique ; tests | 36634872566 | tout vert ; **défaut restant 320 dp / 1,3** vu sur capture |
| `095727b` | Code : seuil d'empilement | **36635957810** | **référence** : JVM 93/0/10, 17/17 ×3, fr 4/4, petit écran 6/6, aucun plantage |
| (suivants) | Documentation uniquement | — | — |

### Lot 7 — Budgets mensuels (feature/android-lot7-budgets)
- Règles iOS reproduites et **ambiguïtés A1–A9** : `docs/BUDGETS.md` (budget par catégorie, mensuel, reconduit ;
  pas de budget global saisi — l'agrégat n'existe que sur l'Accueil iOS, non porté).
- Stockage **additif** (préférences `etix_budgets`, clé = catégorie en minuscules) : aucune migration Room ; tickets et
  catégories jamais modifiés. Mise à jour A → B avec budget et tickets fictifs déjà présents : conservés.
- Catégories « Ce mois » : barre verte / orange « Attention — xx% » / rouge « Dépassé — xx% », « dépensé / budget » ;
  restant et dépassement lus par TalkBack ; invitation tant qu'aucun budget ; bouton de réglage.
- Réglage « Budgets mensuels » et saisie dans un écran à barre haute (comme la feuille iOS). Une fenêtre de dialogue
  essayée d'abord a été abandonnée : recouverte par le clavier à 320 dp / police 1,3 et à police 2,0 (émulateur).
- Grande police : titre « Catégories » et boutons de période en taille automatique (coupure « Catégori/es » et
  « Aujou… » constatées à police 2,0).
- Suites du lot 6 : garantie OCR reformulée + 8 cas de caractérisation des montants ambigus (L1–L8, règle inchangée) ;
  attente du démarrage fondée sur l'état réel de l'app (verdict APP / SYSTÈME) ; ancien test OCR conservé désactivé.
- Tests Robolectric : tas 2 Go et nouvelle JVM toutes les 8 classes (OOM à 130 tests).
- Version `1.7.0-lot7` (versionCode 8 ; build B de test : 9).

#### Traçabilité commits ↔ exécutions CI (lot 7)

| Commit | Nature | Run CI | Résultat |
|---|---|---|---|
| `7258c3f` | Tests : OCR ambigus, attente état réel | (poussé avec la suite) | — |
| `defa346` + `9810a84` | Code budgets + tests | 36639939729 | build échoué (attribut `suffixText`) |
| `152d65d` | Correctif ressource | 36640759058 | JVM : 1 échec (mémoire saturée) |
| `74dd4c9`, `aad19fa`, `7ba0f85` | CI / configuration des tests | 36641665027 … 36642473284 | JVM 120/0/10 ; émulateurs : g02 (bouton sous le clavier, test) et **k04 : fenêtre de saisie recouverte par le clavier (défaut d'interface)** |
| `b0954a5` | Code : barre, grande police | (enchaîné) | — |
| `e5ded74` | Code : saisie en écran à barre haute | 36644079365 | JVM 121/0/10 ; 22/22 ×3 ; fr 5/5 ; petit écran 9/9 |
| `aa8758a` | Code : titre de la saisie (taille auto) | **36645961271** | **référence** : JVM 121/0/10 ; 22/22 ×3 ; fr 5/5 ; petit écran 9/9 ; aucun plantage |
| (suivants) | Documentation uniquement | — | — |

### Lot 8 — Carte Budget de l'Accueil (feature/android-lot8-accueil-budget)
- Carte « Budgets du mois » (iOS `BudgetSummaryCardView`) sur « Ce mois » dès qu'un budget existe : « Il te reste » /
  « Budgets dépassés de », « X dépensés sur Y prévus », barre et pourcentage globaux (50 / 80 / 100 %), jours restants,
  3 lignes max + « et N autres ». Chaque budget compté une fois, dépenses cumulées par clé (casse ignorée).
- « Budget tendu » (global critique ou dépassé) → phrase « Ton rythme de dépenses augmente » (règle iOS).
- **Plantage Android 5 et 6 trouvé sur émulateur et corrigé** (`HashMap.putIfAbsent`, API 24). CI : rapport lint NewApi
  non bloquant ajouté ; il signalait aussi `OCRDateExtractor` (java.time, API 26) — corrigé au durcissement ci-dessous.
- Écarts / ambiguïtés B1–B6 : `docs/BUDGETS.md` (pas de carte Magasin ni d'insights, montants exacts, « dépassés de 0 € »
  au seuil 100 %, état tendu global, animations).
- Lecture seule ; tickets, catégories et budgets inchangés (vérifié). Version `1.8.0-lot8` (versionCode 9).
- **Durcissement** (version `1.8.1-lot8`, versionCode 10) :
  - java.time : plantage **confirmé sur émulateur API 21** (`NoClassDefFoundError`) ; le désucrage a provoqué des
    plantages natifs sur Android 5.0 → écarté ; lecteur réécrit sans java.time, équivalence stricte vérifiée contre
    l'implémentation d'origine (JVM) et exécution sur API 21 / 34 / 36. Scanner toujours désactivé, Q1–Q4 ouvertes.
    Détail : `docs/COMPATIBILITE_ANDROID.md`.
  - Lint NewApi **bloquant** pour toute nouvelle incompatibilité ; 2 exceptions existantes documentées.
  - Jobs émulateur en échec si un test échoue (avant : verts malgré les échecs).
  - « Budget atteint » à 100 % pile (écart iOS B3 documenté) ; « Budgets dépassés de X » seulement en dépassement réel.
  - Tendance 6 mois : libellés entiers sur petit écran et grande police (réduction ≤ 20 %, sinon 3 lettres),
    zone du graphique agrandie au lieu de couper, libellé accessible complet (« septembre 2026 : 55,00 € »).
  - PR #76 vers `feature/android-v2` : fusionnée (voir ci-dessous).
  - **Fiabilité CI** (`docs/FIABILITE_CI.md`) : jobs émulateur verts malgré des échecs jusqu'au 30/09 (62 jobs,
    28 runs intermédiaires ; résultats de référence des lots, lus dans les annotations, confirmés sans échec).
    Verdict par attendus/observés, auto-test permanent, démonstration rouge sur `demo/ci-verdict` (branche de
    démonstration, non destinée à la fusion). Lecteur de dates exécuté sur API 22 à 25. Version livrable contrôlée.

### Fusion du lot 8 dans `feature/android-v2` (PR #76, 01/10/2026)
- PR https://github.com/Nourdine84/eTix/pull/76, commit de fusion **`ef40e1f10f13d8f7ad31a5b3f525225f63226291`**
  (parents `8c1af76` et `9926185`, merge commit sans squash), contenu identique au résultat de fusion testé `e1e714d`.
- Avant fusion : source `9926185`, cible `8c1af76` vérifiées ; runs 36890321511 (PR) et 36890313218 (push) verts.
- **CI après fusion** : run https://github.com/Nourdine84/eTix/actions/runs/36892509302 — 315 jobs verts, 1 tentative.
  JVM + Robolectric : 170 `@Test` déclarés et découverts, 160 exécutés et réussis, 0 échec, 10 ignorés (`@Ignore` :
  9 `OCRFixturesTest` bloqués Q1–Q4, 1 `OCRValidationTest` obsolète). Émulateurs API 21 / 34 / 36 : 29/29 chacun ;
  fr 6/6 ; petit écran 15/15 ; lecteur de dates API 22 à 25 : 2/2 chacun ; auto-test du verdict 10/10.
  Mise à jour 10 → 11 sans désinstallation, données conservées (API 21 / 34 / 36). Aucun plantage, aucune relance.
- Non validés par cette fusion : choix visuels (« Budget atteint » rouge, abréviations, retrait des barres),
  Q1–Q4, « Autre », « ,20 ». Signature QA durable et tests sur téléphone physique : à faire séparément.
- Suites « Vercel » et « claude » : en attente, 0 contrôle (voir `docs/FIABILITE_CI.md`).

### CI : aperçus à la demande (`chore/ci-apercus-a-la-demande`)
- Captures complètes toujours en artefacts (`screenshots`, `emulator-*`, 30 jours) ; aperçus en annotations
  seulement si la PR porte le label **`apercus`** (posé à tout moment, sans commit), 4 captures par job.
- Tests, verdict et auto-tests inchangés ; permissions inchangées (`contents: read`).
- **Revue visuelle** : pour toute modification d'interface, poser `apercus` et examiner les aperçus concernés avant
  de déclarer la revue visuelle effectuée.

### Lot 9 — Scanner (`feature/android-lot9-scanner`, depuis `457f49e`)
- **Décisions OCR Q1 à Q4 validées** (01/10/2026) et appliquées : « TOTAL » seul n'est pas une enseigne
  (TotalEnergies reste une enseigne) ; catégories de référence iOS (ESSO → Carburant), sans reclasser aucun ticket ;
  dates jj/mm/aaaa vérifiées. 9 tests réactivés, attentes inchangées (7 passaient déjà, 2 ont demandé une
  correction du moteur). Correction en plus : « 03.10.26 18:42 » lu comme l'année 2618. `OCRValidationTest`
  (obsolète) reste désactivé. Détail : `docs/OCR_CAS_DE_REFERENCE.md`.
- **Parcours de scan** (iOS ScannerFlowView) : Accueil ou Ajouter → intro → photo (appareil photo du système,
  autorisation expliquée avant la demande, refus → Paramètres) ou image choisie → lecture ML Kit sur l'appareil
  (étapes affichées) → formulaire « Ajouter » prérempli, badges (« Détecté » / « À vérifier » / « Non lu » depuis le 02/10), catégorie
  « Suggéré par l'OCR » → **enregistrement uniquement par « Enregistrer »**. « Rien détecté » / erreur :
  « Réessayer » ou « Saisir manuellement ». Annuler, Retour, « Annuler le scan » : aucun ticket.
  Ancien écran de scan simulé (`OCRScannerFragment`, valeurs fictives) : jamais branché, conservé. Détail et
  écarts iOS : `docs/SCANNER.md`.
- Version `1.9.0-lot9` (versionCode 11). PR brouillon #78 (non fusionnée) ; résultats : `docs/VALIDATION_EMULATEUR.md`.
  - Finalisation (02/10/2026) : INTERNET et ACCESS_NETWORK_STATE retirées (apportées uniquement par les statistiques
    de ML Kit, aucune fonctionnalité réseau) ; montant vide : « Saisir le montant », enregistrement refusé avec un
    message précis ; date absente : « Date non lue — aujourd'hui proposé ». Description de la PR #78 mise à jour.
- **Revue avant décision de fusion (02/10/2026)**, demandée par Nourdine, sans téléphone physique :
  - Marques : « Vérifié » s'affichait sans action de l'utilisateur (montant sur une ligne de total, toute date
    lue). Remplacé par « Détecté » / « À vérifier » / « Non lu » ; marque retirée dès que l'utilisateur modifie
    le champ ; montant ou date ambigus (deux totaux différents, plusieurs dates, date future ou de plus de 2 ans)
    « À vérifier » ; champ absent « Non lu » (dont la date du jour par défaut). Écart iOS voulu.
  - Robustesse : double appui sur « Enregistrer » créait deux tickets → un seul (ViewModel) ; rotation pendant
    l'insertion sans doublon ; marques et résultat de scan en attente conservés à la recréation.
  - Permission CAMERA retirée (inutile pour l'application appareil photo du système) et étape d'autorisation
    supprimée ; contrôle CI des permissions de l'APK (restent INTERNET et ACCESS_NETWORK_STATE, apportées par
    ML Kit : l'app elle-même ne fait aucun appel réseau).
  - Petit écran : barre d'onglets masquée pendant le scan (iOS fullScreenCover) ; barre basse limitée à 40 %
    (mesurée sur émulateur à 320 dp / police 2,0 : 20 %, contre ≈ 60 % avant).
  - Images : budget de 4 Mpx (une photo 12 Mpx était gardée en pleine résolution ; un ticket long aurait été
    réduit à 256 px de large).
  - Nouveaux contrôles émulateur : vrai sélecteur de photos et vraie application appareil photo (refus puis
    accès rétabli), ML Kit hors ligne au premier lancement, EXIF / 48 Mpx / ticket long, rotation, double
    appui, annulation pendant la lecture, scan en français, passe 320 dp police 2,0, mise à jour depuis le lot 8
    fusionné (versionCode 10 → 11) avec tickets et budgets comparés. Résultats : `docs/VALIDATION_EMULATEUR.md`.

### Fusion du lot 9 dans `feature/android-v2` (PR #78, 02/10/2026)
- Fusion autorisée par Nourdine, commit de fusion **`86d9a19`** (parents `457f49e` et `8e6fa6f`, sans squash),
  arbre identique au résultat de fusion testé `bbb38df` (run https://github.com/Nourdine84/eTix/actions/runs/36994446604,
  une relance du job `emulator-compat-dates (24)` : installation de l'APK bloquée > 300 s, aucun test démarré,
  cause inconnue, diagnostics dans le run).
- Refus des montants nuls ou négatifs : déjà présent avant le lot 9 (`TicketFormRules.parseAmount`, champ sans
  signe moins) ; le lot 9 n'a changé que le message et le texte indicatif.
- **CI après fusion** : https://github.com/Nourdine84/eTix/actions/runs/36999579628 — 21 jobs verts, sans relance.
  JVM 218 déclarés, 217 réussis, 1 ignoré (`OCRValidationTest`), 0 échec ; émulateurs API 21 / 34 / 36 : 40 / 40
  chacun ; système 3 / 3 ; mise à jour lot 8 → lot 9 12 / 12 (tickets et budgets identiques) ; fr 7 / 7 ; petit
  écran 28 / 28 ; dates API 22-25. APK : seule `com.etix.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (CAMERA, INTERNET,
  ACCESS_NETWORK_STATE absentes).
- Limites restantes : aucune revue visuelle par Nourdine (parcours : `docs/REVUE_VISUELLE.md`), aucun essai sur
  téléphone physique (vraies photos, appareil photo et sélecteur du constructeur), signature QA durable à
  préparer. La fusion ne valide ni les choix visuels ni les décisions produit ouvertes.
- Bilan de parité et pistes pour le prochain lot : `docs/PARITE_IOS.md`.

### Lot 10 — Réglages (`feature/android-lot10-reglages`, depuis `86d9a19`)
- Périmètre validé par Nourdine le 02/10/2026 ; détail, écarts iOS, points ouverts, répartition de la couverture et
  procédure d'import du CSV : `docs/REGLAGES.md`. Version `1.10.0-lot10`, versionCode 12. Aucun changement de schéma
  Room, aucune migration, aucune permission (contrôle CI : seule `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`).
- Demandes du 02/10 après la première revue : période sans effet sur les écrans déjà ouverts ; champs texte du CSV
  neutralisés contre les formules (fichier seulement) ; collecte des journaux de plantage signalée inactive ;
  assertions Robolectric de thème restaurées.
- **Code testé avant fusion : `88175bb`**, tête de la PR à ce moment. Le commit suivant, `f09a7e6`, n'a modifié que
  deux documents (`docs/SUIVI_ANDROID.md` et `docs/FIABILITE_CI.md`) et a lui-même été testé. Résultat de fusion de
  `88175bb` avec `feature/android-v2` `86d9a19` : run https://github.com/Nourdine84/eTix/actions/runs/37020784489 —
  vert après une relance ciblée de `emulator-compat-dates (25)`. Run du commit seul :
  https://github.com/Nourdine84/eTix/actions/runs/37020777107 (vert).
  - JVM + Robolectric : 240 `@Test` déclarés, 239 réussis, 0 échec, 1 ignoré (`OCRValidationTest`) ; lint NewApi sans
    nouvelle incompatibilité.
  - Émulateurs (réussis / attendus, 0 échec) : API 21, 34, 36 : 47 / 47 chacun (dont `E2eReglagesTest` 5 / 5 et
    persistance après redémarrage 2 / 2) ; petit écran API 36 : 32 / 32 (4 passes, `k09_reglages` inclus) ;
    français 7 / 7 ; système 3 / 3 ; mise à jour lot 8 → lot 10 et lot 9 → lot 10 : 13 / 13 chacune (tickets, budgets
    et thème « Clair » identiques, Réglages affichant « Clair » et build 12) ; dates API 22, 23, 24 : 2 / 2 ; API 25 :
    2 / 2 à la relance.
- Incident API 25 (1ʳᵉ tentative, résultat de fusion de `88175bb`) : installation de `app-A.apk` bloquée 300 s sur
  « Performing Streamed Install », émulateur déclaré démarré 9 s après son lancement, aucun test exécuté. **Cause
  inconnue** : le blocage avant les tests n'exclut pas un lien avec l'APK ; un émulateur pas encore réellement prêt
  reste une hypothèse. Autres incidents d'infrastructure du 02/10, sur des commits remplacés : téléchargement de
  l'émulateur (« Error on ZipFile »), dépendances Maven introuvables, certificat lors du checkout.
- Aperçus examinés (run de fusion) : Réglages clair / sombre, haut et bas, 360 dp et 320 dp police 2,0 (Robolectric et
  émulateur API 36, passes a à d, sombre en d), listes de choix, Historique filtré, Accueil avant / après réouverture,
  feuille de partage API 34 et API 21, Réglages après redémarrage et après mise à jour. Captures reprises dans
  `docs/preview/revue-f102cfe/` ; revue visuelle par Nourdine à faire (`docs/REVUE_VISUELLE.md`).

### Fusion du lot 10 dans `feature/android-v2` (PR #80, 02/10/2026)
- Fusion autorisée par Nourdine, commit de fusion **`f102cfe`** (parents `86d9a19` et `f09a7e6`, sans squash). Avant
  l'opération : source et cible inchangées, résultat de fusion testé `3cff285` (run
  https://github.com/Nourdine84/eTix/actions/runs/37025069026, vert sans relance) ; arbre de `f102cfe` identique à
  celui de `3cff285`.
- **CI après fusion** : https://github.com/Nourdine84/eTix/actions/runs/37027739385 — verte à la 2ᵉ tentative.
  JVM + Robolectric 240 déclarés, 239 réussis, 1 ignoré (`OCRValidationTest`), 0 échec ; émulateurs API 21 / 34 / 36 :
  47 / 47 chacun ; petit écran 32 / 32 ; fr 7 / 7 ; système 3 / 3 ; mise à jour lot 8 → lot 10 (versionCode 10 → 12)
  et lot 9 → lot 10 (11 → 12) : 13 / 13 chacune, tickets, budgets et thème identiques avant / après, Réglages
  affichant le thème conservé, période « Ce mois » (préférence absente des anciennes versions) ; mises à jour A → B
  (12 → 13) sans désinstallation sur API 21 / 34 / 36 ; dates API 22, 23, 25 : 2 / 2 ; API 24 : 2 / 2 à la relance.
- **Incident API 24 (1ʳᵉ tentative après fusion)** : `app-A.apk` installé (« Success »), puis installation de l'APK
  de tests (`app-debug-androidTest.apk`) bloquée 300 s ; émulateur déclaré démarré environ 12 s après son lancement,
  juste après un « device offline » ; journal système relevé s'arrêtant sur des événements Wi-Fi du démarrage
  (15:37:42) ; aucun test exécuté. Une seule relance ciblée, verte. **Cause inconnue** : un lien avec l'APK n'est
  pas exclu ; l'hypothèse d'un émulateur pas encore prêt n'est pas démontrée. Consigné dans `docs/FIABILITE_CI.md`.
- Décisions de Nourdine (02/10/2026) : format CSV actuel accepté pour ce lot avec procédure d'import documentée
  (`docs/REGLAGES.md`) ; ouverture dans une vraie application destinataire = validation à faire ; journaux de
  plantage non activés.
- Limites : aucune revue visuelle par Nourdine, aucun essai sur téléphone physique, aucune application destinataire
  réelle n'a ouvert le fichier (sur l'émulateur API 21 de la CI, aucune application ne reçoit `text/csv`). La fusion
  ne valide ni les choix visuels ni les décisions produit ouvertes.

### Revue visuelle — Accueil validé (04/10/2026)

- Captures dédiées (aucun changement de l'app) : test `E2eRevueAccueilTest`, job `emulator-api34-revue` (émulateur
  API 34 en français, app neuve, 11 tickets et 3 budgets fictifs injectés en base, période « Ce mois », Clair puis
  Sombre, haut / carte Budget / bas). Commit `6dc0707`, PR #81 (brouillon, non fusionnée), run 37222538785 :
  141 jobs verts, verdict revue 2 / 2.
- **Validation de Nourdine** : Accueil clair et sombre tel que montré par les 6 captures
  (`docs/preview/revue-accueil-6dc0707/`, détail dans `docs/REVUE_VISUELLE.md`). Portée : ce rendu seulement, ni les
  écrans non examinés, ni les essais sur téléphone physique.
- À vérifier séparément : seuils et couleurs de budget Accueil / Catégories ; écart dépenses totales / dépenses des
  catégories budgétées.

## Écarts restants avec iOS (référence `feature/home-hero-v2`)

Bilan détaillé à jour (présent / partiel / absent, émulateur, validations manquantes) : `docs/PARITE_IOS.md`.

| Écran | Écart | Lot envisagé |
|---|---|---|
| Accueil | Insights (≤ 2), carte Budget / Magasin, étoiles du header sombre, animations d'entrée, lien Tendance → rapport mensuel | 4 |
| Accueil | Scanner : porté au lot 9, fusionné (`86d9a19`) ; écarts voulus dans `docs/SCANNER.md` | fait |
| Ajouter | Scanner et suggestion de catégorie (historique puis OCR) : lot 9, fusionné | fait |
| Historique | Suppression par balayage (iOS) non portée | 5 |
| Détail ticket | Aligné au lot 6 ; dégradé du montant iOS rendu en bleu uni ; pas de retour haptique | — |
| Catégories | Budgets portés (lot 7) ; export non porté | à planifier |
| Accueil | Carte Budget portée (lot 8) ; insights, carte Magasin, animations d'entrée non portés | à planifier |
| Détail catégorie | « Voir par magasin » (liste des magasins filtrée) et export non portés | à planifier |
| Catégories | Choix provisoire « Autre » quand aucune catégorie n'est choisie à l'ajout (iOS : vide) → ces tickets apparaissent sous « Autre » | décision produit |
| Magasins | Comparaison entre magasins, graphique « Historique des achats » | 5 |
| Réglages | Portés au lot 10, fusionné (`f102cfe`) : thème, période par défaut, export CSV, compteur, version, build ; écarts voulus (suppression globale désactivée, liste à choix au lieu du menu iOS, Compte et Diagnostic Android, protection contre les formules) dans `docs/REGLAGES.md` | fait |
| Global | Widget iOS, rapport mensuel / export PDF, onboarding V2 | à décider |
| Global | Connexion Android sans équivalent iOS (décision reportée) | à décider |

## Test OCR désactivé — conditions de réactivation

`OCRValidationTest` (`@Ignore`) : limite explicite, pas un test « vert ».

| Constat | Détail |
|---|---|
| Fichiers attendus absents | `app/src/test/resources/ocr/ticket_carrefour.txt`, `.../ticket_restaurant.txt` |
| Fichiers présents non utilisés | `ticket_001.txt` (LIDL 34,50), `ticket_002.txt` (CARREFOUR 12,99), `ticket_003.txt` (sans enseigne, 58,20), `ticket_004.txt` (ESSO 23,45 €, date) |
| Taxonomie incohérente | Le test attend `Supermarché`, l'analyseur (`OCRSmartAnalyzer.CATEGORY_KEYWORDS`) produit `Alimentation` |
| API | `guessCategory` renommé `guessCategoryWithConfidence` (corrigé au lot 1) |

Conditions : (1) fournir des textes OCR réels (ou valider les 4 fixtures existantes comme référence) ;
(2) fixer par écrit la liste des catégories cible, alignée iOS (`StoreCategoryMapper`) ;
(3) écrire les attentes par fixture (enseigne, montant, catégorie, date) et le comportement attendu quand l'enseigne est absente (`ticket_003`) ;
(4) retirer `@Ignore` au lot OCR, test exécuté en CI.

## Connexion factice — traitement proposé (non implémenté)

Constat : `LoginActivity` / `RegisterActivity` acceptent tout identifiant/mot de passe non vide ; le mot de passe n'est ni stocké ni vérifié ; `SessionManager.login()` pose seulement `logged_in=true`. **Ce n'est pas une protection** : les données Room restent lisibles par quiconque a le téléphone déverrouillé.

| Option | Effet | Coût |
|---|---|---|
| A. Retirer l'écran (parité iOS : splash → onboarding → app) | Plus d'illusion de sécurité | Faible |
| B. Garder, renommer en « Profil » (prénom local, sans mot de passe) | Personnalisation honnête | Faible |
| C. Verrou local réel (BiometricPrompt / code appareil au lancement) | Protection effective contre un tiers | Moyen ; à aligner iOS (Face ID) |
| D. Vrai compte (backend, sync) | Multi-appareils | Élevé, nouvelle fonctionnalité |

Décision du 29/09 : **reportée**. Code de connexion conservé tel quel, aucune suppression. Le verrou (option C) est une fonctionnalité distincte, à décider séparément ; non incluse dans les lots en cours.
Recommandation technique inchangée : A (ou B) pour la parité iOS.

## Points ouverts (non traités)

| # | Sujet | Priorité |
|---|---|---|
| 1 | Magasins : bouton « Comparaison » (iOS `StoreComparisonView`) et graphique « Historique des achats » non portés | P2 |
| 2 | ~~Bouton « Scanner » de l'ajout sans action~~ : résolu au lot 9 (parcours de scan, fusionné en `86d9a19`) ; l'ancien flux NavController reste non branché | fait |
| 3 | Ajout : catégorie non choisie → « Autre » (choix provisoire, écart iOS : vide) — date et description ajoutées au lot 4 | décision produit |
| 4 | Accueil loin d'iOS (insights, carte Magasin, export PDF…) ; Catégories : export et « Voir par magasin » manquants. Réglages : portés au lot 10 | P2-P3 |
| 5 | Login / Register Android sans équivalent iOS (iOS : splash → onboarding → app) | À décider |
| 6 | `ETixApp` non déclarée dans le manifeste → journal de crash jamais alimenté ; depuis le lot 10 les Réglages l'indiquent (« Journaux de plantage indisponibles »). Activation = décision distincte, non prise (02/10/2026) | P4 |
| 7 | Manifeste : `.TicketEditActivity` déclarée mais inexistante | P4 |
| 8 | Room `fallbackToDestructiveMigration()` → perte de données à tout changement de schéma | P4 |
| 9 | Login simulé — voir section dédiée | Lié au #5 |
| 10 | Test `OCRValidationTest` désactivé — voir section dédiée | P3 |
| 11 | Accueil V2 : fond blanc codé en dur (`#FFFFFF`) → illisible/incohérent en thème sombre | P2 |
| 13 | Réglages : « Supprimer tous les tickets » désactivé et signalé indisponible (lots 3 et 10). Implémentation (avec confirmation, parité iOS) = décision produit | P3 |
| 14 | Thème sombre : corrigé au lot 3 sur les écrans principaux ; écrans V1 restants (fiches, popups) à vérifier sur téléphone | P3 |
| 12 | Signature QA : clé durable via secrets — **en attente de votre action** (`docs/SIGNATURE_QA.md`) ; tant qu'elle manque, aucun APK QA n'est publié | P1 |
