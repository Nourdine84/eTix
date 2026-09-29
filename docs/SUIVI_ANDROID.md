# Suivi eTix Android — parité iOS

Document de reprise entre sessions. Concis : état, décisions, prochain lot.

## Dépôts et branches

| Élément | Emplacement |
|---|---|
| Code Android (référence) | `Nourdine84/eTix`, branche `feature/android-v2` (dernier commit 16/01/2026) |
| Branches de travail | Lot 1 `fix/android-v2-navigation` (depuis `feature/android-v2`) ; Lot 2 `feature/android-lot2-magasins` (depuis lot 1). **Rien fusionné** — fusion uniquement sur accord explicite. |
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
- Catégories : version V1 fonctionnelle branchée tant que la V2 reste un placeholder vide.
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
- CI : clé de signature stable (secret `QA_KEYSTORE_B64` si présent, sinon cache Actions), contrôle package/version/empreinte certificat, APK nommé `eTix-QA-<version>.apk`.
- Onglet Magasins (iOS `StoreListView`) : période Aujourd'hui/Ce mois/Cette année (défaut Ce mois), cartes classées (N°1-3), total, nb tickets, €/visite, part %, dernier passage, état vide.
- Fiche magasin (iOS `StoreDetailView`) : total, stats (panier moyen, tickets, dernière visite, fréquence), comparaison mensuelle, top 3 catégories, tickets (5 + « voir les autres ») → détail ticket.
- Réglages : icône engrenage sur l'Accueil → écran Réglages existant (inchangé), Retour → Accueil.
- Tests : `StoreStatsTest` (JVM), navigation mise à jour, captures `Lot2ScreenshotTest` (aperçu, pas une validation).
- Procédure de test sans risque pour `com.etix` : `docs/PROCEDURE_TEST_QA.md` ; plan de test : `docs/PLAN_TEST_LOT2.md`.
- CI : `ANDROID_USER_HOME` fixé (AGP rangeait la clé dans `~/.config/.android`, le cache ne la voyait pas → signature différente à chaque run).
- Corrections issues des captures : libellés de période tronqués ; libellés de tous les onglets visibles (comme iOS) ; barre d'onglets sombre en thème sombre ; texte invisible de 3 boutons Réglages (texte bleu sur fond bleu).
- Version : `1.2.0-lot2` (versionCode 3), APK QA `1.2.0-lot2-qa`.

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

Recommandation : A (ou B) pour la parité, puis C si la confidentialité des tickets est un besoin produit. Décision produit requise ; code conservé en attendant.

## Points ouverts (non traités)

| # | Sujet | Priorité |
|---|---|---|
| 1 | Magasins : bouton « Comparaison » (iOS `StoreComparisonView`) et graphique « Historique des achats » non portés | P2 |
| 2 | Bouton « Scanner » de l'ajout sans action ; flux OCR conçu pour NavController (crasherait) | P3 |
| 3 | Ajout : catégorie forcée à « Autre », pas de date ni de description | P3 |
| 4 | Accueil / Catégories / Réglages loin d'iOS (insights, budget, donut, apparence, export PDF…) | P2-P3 |
| 5 | Login / Register Android sans équivalent iOS (iOS : splash → onboarding → app) | À décider |
| 6 | `ETixApp` non déclarée dans le manifeste → journal de crash jamais alimenté | P4 |
| 7 | Manifeste : `.TicketEditActivity` déclarée mais inexistante | P4 |
| 8 | Room `fallbackToDestructiveMigration()` → perte de données à tout changement de schéma | P4 |
| 9 | Login simulé — voir section dédiée | Lié au #5 |
| 10 | Test `OCRValidationTest` désactivé — voir section dédiée | P3 |
| 11 | Accueil V2 : fond blanc codé en dur (`#FFFFFF`) → illisible/incohérent en thème sombre | P2 |
| 13 | Réglages : bouton « Vider tous les tickets » affiché mais **sans action** (non câblé) — trompeur ; iOS a « Supprimer tous les tickets » avec confirmation. Non modifié (action destructive → décision requise) | P2 |
| 14 | Thème sombre global : `colorSurface` du thème figé en clair ; Accueil/Réglages V1 codés en dur | P2 |
| 12 | Cache Actions de la clé QA : stable seulement par branche ; s'il est créé sur `dev`/`main`, des PR de forks (dépôt public) peuvent le restaurer. Préférer le secret `QA_KEYSTORE_B64` | P3 |
