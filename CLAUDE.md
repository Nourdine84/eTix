# eTix Android — contexte pour Claude Code

Document de reprise. À lire en entier avant toute action. Détail : `docs/SUIVI_ANDROID.md` (journal des lots),
`docs/VALIDATION_EMULATEUR.md`, `docs/FIABILITE_CI.md`, `docs/SCANNER.md`, `docs/OCR_CAS_DE_REFERENCE.md`,
`docs/BUDGETS.md`, `docs/COMPATIBILITE_ANDROID.md`, `docs/TESTS_DESACTIVES.md`, `docs/SIGNATURE_QA.md`.

## Projet et rôles

- Objectif : amener eTix Android (ce dépôt, `Nourdine84/eTix`) au niveau de la version iOS
  (`Nourdine84/etix-ios`, branche de référence `feature/home-hero-v2`) : fonctionnalités, navigation, design.
- Nourdine (propriétaire, ingénieur QA senior) décide du produit et valide le visuel. Claude implémente, teste et
  rend compte. ChatGPT aide Nourdine à suivre l'avancement.
- Maquettes « validées » introuvables : le code iOS est la référence provisoire. Ne jamais affirmer une conformité
  au design validé ; signaler chaque écart avec iOS.
- Répondre en français, directement, niveau technique senior ; signaler risques et meilleures approches.

## Règles impératives (validées par Nourdine)

- **Moindre privilège.** Aucune suppression critique (données, fonctionnalités, fichiers importants, historique,
  branches, artefacts) sans accord explicite. L'absence de réponse ne vaut jamais accord.
- **Jamais** désinstaller l'app, réinitialiser son stockage (`pm clear`), ni toucher aux vrais tickets
  (`com.etix`). Tests manuels et sur téléphone : **uniquement l'APK QA** `com.etix.qa`, installé à côté.
- **Git** : identité locale `Nourdine84 <msanourdine@hotmail.com>` (`git config --local`, ne pas modifier la
  configuration globale). Messages de commit, titres et descriptions de PR sobres, en français, **sans mention de
  Claude, sans ligne Co-Authored-By ni signature automatique**. Pas d'amend/rebase sur des commits poussés, pas
  de push forcé, pas de réécriture d'historique.
- **Fusions** : jamais vers `main` ni `dev`. Vers `feature/android-v2` uniquement avec l'accord explicite de
  Nourdine, **une autorisation = une seule fusion**, par commit de fusion (pas de squash). Avant : vérifier que
  source et cible n'ont pas bougé et que le run CI du résultat de fusion proposé est vert ; s'arrêter si un
  conflit ou un changement fonctionnel demande une décision.
- **Aucune publication** d'APK ou de version sans accord. Ne jamais afficher ni écrire la clé de signature QA
  ou ses mots de passe (dépôt, journaux, conversation).
- Ne pas désactiver ni supprimer les suites de contrôles « Vercel » et « claude » (en attente, 0 contrôle).
- Garder le code de connexion ; « Vider tous les tickets » reste désactivé.
- Ne pas inventer de fonctionnalité ni de direction graphique. Toute modification d'interface : déclencher les
  aperçus (label `apercus` sur la PR) et **les examiner** avant de déclarer la revue visuelle faite. Une fusion
  ne valide ni les choix visuels ni les décisions produit ouvertes.
- Ne jamais présenter une fonctionnalité comme validée parce que le code est écrit : distinguer implémenté,
  compilé, testé (JVM / Robolectric / émulateur / téléphone) et restant à valider.
- Ne jamais modifier une attente de test correcte pour obtenir un test vert ; corriger le code.

## Décisions produit

- **Validées** (01/10/2026) : OCR Q1 à Q4 — « TOTAL » seul = libellé, pas une enseigne (TotalEnergies reste une
  enseigne) ; catégories de référence iOS (ESSO → Carburant), **sans reclasser aucun ticket existant** ; dates
  numériques jour/mois/année ; dates vérifiées dans les tests.
- Budgets : suppression d'un budget avec confirmation ; budgets de catégories ne différant que par la casse =
  un seul budget, consommation cumulée, compté une fois dans les totaux. « Budget atteint » à 100 % pile.
- **Ouvertes** : catégorie par défaut « Autre » (iOS : vide) ; saisie « ,20 » ; points visuels à valider
  (« Budget atteint » en rouge, abréviations à 3 lettres de la Tendance, retrait de 4 dp des barres, écran du
  scanner, badges, bandeau, barre basse).

## Stack et commandes

- Kotlin 1.9.22, AGP 8.2.2, Gradle 8.5, JDK 17, minSdk 21 / targetSdk 34, vues XML + ViewBinding,
  ViewPager2 + BottomNavigationView (5 onglets), écrans poussés dans `overlayContainer` de `MainActivityV2`,
  Room 2.6.1 (**aucun changement de schéma sans accord**), budgets en SharedPreferences `etix_budgets`,
  ML Kit Text Recognition (modèle embarqué), CameraX présent mais non utilisé par le scanner.
- `gradle.properties` pointe `org.gradle.java.home` vers le JDK d'Android Studio (macOS).
- Build : `./gradlew assembleDebug` (`com.etix`) ; `./gradlew assembleQa` (`com.etix.qa`, clé QA durable
  seulement via variables d'environnement, sinon clé debug locale — voir `docs/SIGNATURE_QA.md`).
- Tests JVM + Robolectric : `./gradlew testDebugUnitTest` (aperçus Robolectric dans `app/build/screenshots/`).
- Lint : `./gradlew lintDebug` (NewApi bloquant en CI ; 2 exceptions documentées dans
  `docs/COMPATIBILITE_ANDROID.md`).
- Tests instrumentés : `app/src/androidTest/java/com/etix/e2e/` (Espresso, données fictives). Ordre et modes
  (standard, fr, petit, compat) : `.github/scripts/emulator_e2e.sh`.

## CI (`.github/workflows/android-ci.yml`)

- Déclencheurs : push sur `dev`, `feature/**`, `chore/**`, `fix/**` ; PR vers `dev`, `main`, `feature/android-v2`
  (ouverture, push, label `apercus`).
- Jobs de test : `build` (JVM + Robolectric, lint NewApi bloquant, contrôle de la version livrable),
  `emulator-api21/34/36` (parcours complet + mise à jour A→B sans désinstallation), `emulator-api34-fr`,
  `emulator-api36-petit` (3 passes taille / police), `emulator-compat-dates` (API 22 à 25), `verdict-autotest`.
- **Verdict émulateur** : le job échoue si les tests réussis ≠ `@Test` déclarés, résultat absent, plantage,
  délai dépassé ou mise à jour non conforme. Avant le 30/09 les jobs émulateur restaient verts malgré des
  échecs : ne jamais se fier au seul statut vert d'un ancien run.
- Captures : artefacts `screenshots` et `emulator-*` (30 jours) ; aperçus en annotations seulement avec le label
  `apercus`. Annotations lisibles via l'API checks (`gh api`).
- Incidents connus, cause inconnue : installation de l'APK bloquée > 300 s sur émulateur API 24 / 25 (1 + 2 fois).
  Pas de relance automatique ; s'ils se reproduisent, recueillir les diagnostics avant de proposer un correctif.

## État au 02/10/2026

- `feature/android-v2` = intégration : lots 1 à 9 + CI (PR #75, #76, #77, #78 fusionnées), tête `86d9a19` au 02/10.
- **Lot 9 fusionné** (PR #78, 02/10/2026) : `feature/android-v2` = commit de fusion **`86d9a19`** (scanner + Q1–Q4,
  version `1.9.0-lot9`, versionCode 11). CI après fusion verte : run 36999579628 (JVM 217 réussis / 1 ignoré,
  émulateurs 40/40 par API, système, mise à jour lot 8 → lot 9 avec données identiques). Branche
  `feature/android-lot9-scanner` conservée. Fusion ≠ validation visuelle : aucune revue visuelle ni essai
  physique faits (`docs/REVUE_VISUELLE.md`).
- Bilan de parité iOS et pistes de prochain lot : `docs/PARITE_IOS.md` (choix du lot par Nourdine).
- Permissions de l'APK : aucune permission système (CAMERA, INTERNET, ACCESS_NETWORK_STATE retirées, contrôle CI
  bloquant). Une future fonctionnalité réseau devra les redéclarer explicitement (et ajuster ce contrôle).
- Branche `demo/ci-verdict` : démonstration du verdict, jamais proposée à la fusion, à conserver.
- Copie locale Mac `~/AndroidStudioProjects/eTix` : était sur `dev` (12/2025) ; récupérer les branches
  (`git fetch`) sans rien écraser avant de travailler.

## Prochaine étape

Revue visuelle par Nourdine (`docs/REVUE_VISUELLE.md`) et choix du prochain lot (`docs/PARITE_IOS.md`) ;
signature QA durable à préparer séparément. Tests du scanner sur **téléphone physique** avec l'APK QA (jamais l'app `com.etix`) quand un téléphone sera
disponible : application appareil photo du constructeur, vraies photos (flou, pli, lumière, ticket long), refus
puis rétablissement de l'accès caméra dans l'application appareil photo (eTix ne demande plus rien), sélecteur du
téléphone, retour arrière à chaque étape, aucun ticket créé sans « Enregistrer ». Leur absence est une limite
documentée ; le lot 9 a été fusionné sans eux (décision de Nourdine du 02/10/2026).

## Pièges déjà rencontrés

- Android 5/6 : `HashMap.putIfAbsent` (API 24) et `java.time` (API 26) plantent ; le désucrage
  (`coreLibraryDesugaring`) a provoqué des plantages natifs sur Android 5.0 → écarté. Le lot 9 garde le lecteur
  de dates sans java.time (équivalence vérifiée par `OCRDateExtractorEquivalenceTest`).
- Android 5 : `backgroundTint` (XML ou `setBackgroundTintList`) ignoré → construire le fond directement.
- Robolectric : `Screens.waitFor` échoue à l'expiration ; tests sensibles au 1er du mois (dates « ce mois ») ;
  attendre la fin des coroutines d'écran avant de vérifier l'interface.
- Espresso : deux pages du ViewPager portent les mêmes identifiants (`btnScanTicket`…) → préciser le parent ;
  les vues hors écran ne passent pas `isDisplayed`.
- Petits écrans / police 2,0 : vérifier que les actions principales restent visibles (barres basses).
- Chaque compte rendu de lot : commit testé, liens CI, résultats (réussis / échecs / ignorés séparés), limites,
  et mise à jour de `docs/SUIVI_ANDROID.md`.
