# Suivi eTix Android — parité iOS

Document de reprise entre sessions. Concis : état, décisions, prochain lot.

## Dépôts et branches

| Élément | Emplacement |
|---|---|
| Code Android (référence) | `Nourdine84/eTix`, branche `feature/android-v2` (dernier commit 16/01/2026) |
| Branches de travail | Lot 1 `fix/android-v2-navigation` ; Lot 2 `feature/android-lot2-magasins` (contient lot 1) ; Lot 3 `feature/android-lot3-accueil-theme` (contient lots 1-2) ; Lot 4 `feature/android-lot4-formulaire-historique` ; Lot 5 `feature/android-lot5-categories` (depuis le lot 4 @ `c77f0ba`) ; Lot 6 `feature/android-lot6-fiabilisation` (depuis le lot 5 @ `959f41e`) ; Lot 7 `feature/android-lot7-budgets` (depuis le lot 6 @ `8172881`). **Lots 1 à 7 fusionnés dans `feature/android-v2`** (PR #75, commit de fusion `8c1af76`, 30/09/2026). **Lot 8 `feature/android-lot8-accueil-budget`** (depuis `8c1af76`). Anciennes branches conservées. `main` et `dev` jamais modifiés. |
| Historique Git | `feature/android-v2` et `dev` : ancêtre commun `ab7d6f8`, `dev` a 3 commits propres. `main` : racine distincte, sans ancêtre commun. Détail : `docs/REVUE_LOTS_1_2.md`. Intégration `dev`/`main` = décision séparée. |
| `Nourdine84/etix-android` | Squelette Gradle sans module `app` — **pas** le dépôt de dev |
| Copie locale Mac `~/AndroidStudioProjects/eTix` | Sur `dev` (19/12/2025), n'a pas `feature/android-v2` |
| Référence iOS | `Nourdine84/etix-ios` — `feature/home-hero-v2` (19/08/2026) ; `main` diverge (21 commits propres) |
| Maquettes validées | `eTix_V2_Maquettes_Completes_Validees_FINAL` (citées dans `Theme.swift` iOS) — **introuvables** (3 dépôts, projet, dossiers Mac connectés). Référence provisoire : code iOS. |

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
  - PR brouillon vers `feature/android-v2` (non fusionnée).

## Écarts restants avec iOS (référence `feature/home-hero-v2`)

| Écran | Écart | Lot envisagé |
|---|---|---|
| Accueil | Insights (≤ 2), carte Budget / Magasin, étoiles du header sombre, animations d'entrée, lien Tendance → rapport mensuel | 4 |
| Accueil | Scanner indisponible (flux OCR non branché) | OCR |
| Ajouter | Scanner indisponible ; pas de suggestion de catégorie (OCR / historique) | OCR |
| Historique | Suppression par balayage (iOS) non portée | 5 |
| Détail ticket | Aligné au lot 6 ; dégradé du montant iOS rendu en bleu uni ; pas de retour haptique | — |
| Catégories | Budgets portés (lot 7) ; export non porté | à planifier |
| Accueil | Carte Budget portée (lot 8) ; insights, carte Magasin, animations d'entrée non portés | à planifier |
| Détail catégorie | « Voir par magasin » (liste des magasins filtrée) et export non portés | à planifier |
| Catégories | Choix provisoire « Autre » quand aucune catégorie n'est choisie à l'ajout (iOS : vide) → ces tickets apparaissent sous « Autre » | décision produit |
| Magasins | Comparaison entre magasins, graphique « Historique des achats » | 5 |
| Réglages | V1 ; iOS : Apparence (système/clair/sombre), période par défaut, budgets, export CSV, suppression avec confirmation | 5 |
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
| 2 | Bouton « Scanner » de l'ajout sans action ; flux OCR conçu pour NavController (crasherait) | P3 |
| 3 | Ajout : catégorie non choisie → « Autre » (choix provisoire, écart iOS : vide) — date et description ajoutées au lot 4 | décision produit |
| 4 | Accueil / Réglages loin d'iOS (insights, budget, apparence, export PDF…) ; Catégories : budgets/détail/export manquants | P2-P3 |
| 5 | Login / Register Android sans équivalent iOS (iOS : splash → onboarding → app) | À décider |
| 6 | `ETixApp` non déclarée dans le manifeste → journal de crash jamais alimenté | P4 |
| 7 | Manifeste : `.TicketEditActivity` déclarée mais inexistante | P4 |
| 8 | Room `fallbackToDestructiveMigration()` → perte de données à tout changement de schéma | P4 |
| 9 | Login simulé — voir section dédiée | Lié au #5 |
| 10 | Test `OCRValidationTest` désactivé — voir section dédiée | P3 |
| 11 | Accueil V2 : fond blanc codé en dur (`#FFFFFF`) → illisible/incohérent en thème sombre | P2 |
| 13 | Réglages : « Vider tous les tickets » désactivé et signalé indisponible (lot 3). Implémentation (avec confirmation, parité iOS) = décision produit | P3 |
| 14 | Thème sombre : corrigé au lot 3 sur les écrans principaux ; écrans V1 restants (fiches, popups) à vérifier sur téléphone | P3 |
| 12 | Signature QA : clé durable via secrets — **en attente de votre action** (`docs/SIGNATURE_QA.md`) ; tant qu'elle manque, aucun APK QA n'est publié | P1 |
