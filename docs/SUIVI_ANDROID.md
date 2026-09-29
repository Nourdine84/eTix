# Suivi eTix Android — parité iOS

Document de reprise entre sessions. Concis : état, décisions, prochain lot.

## Dépôts et branches

| Élément | Emplacement |
|---|---|
| Code Android (référence) | `Nourdine84/eTix`, branche `feature/android-v2` (dernier commit 16/01/2026) |
| Branche de travail | `fix/android-v2-navigation` (depuis `feature/android-v2`) |
| `Nourdine84/etix-android` | Squelette Gradle sans module `app` — **pas** le dépôt de dev |
| Copie locale Mac `~/AndroidStudioProjects/eTix` | Sur `dev` (19/12/2025), n'a pas `feature/android-v2` |
| Référence iOS | `Nourdine84/etix-ios` — `feature/home-hero-v2` (19/08/2026) ; `main` diverge (21 commits propres) |
| Maquettes validées | `eTix_V2_Maquettes_Completes_Validees_FINAL` (citées dans `Theme.swift` iOS) — **non accessibles** |

## Stack

Kotlin 1.9.22, AGP 8.2.2, Gradle 8.5, JDK 17, vues XML + ViewBinding, ViewPager2 + BottomNavigationView,
Room 2.6.1 (KAPT), Coroutines/Flow, ML Kit Text Recognition + CameraX. minSdk 21 / target 34.
Pas de backend : auth et données 100 % locales (SharedPreferences + Room).

Build : `./gradlew assembleDebug` — tests : `./gradlew testDebugUnitTest`.
CI : `.github/workflows/android-ci.yml` (push sur `dev`, `feature/**`, `fix/**`, `chore/**`) → artefact `app-debug-apk`.

## Décisions

- Pas de migration d'architecture : on garde ViewPager2 + BottomNav ; `nav_graph.xml` n'est **pas** utilisé par l'app (aucun NavHost).
- Écrans poussés (détail / édition ticket) : `overlayContainer` au-dessus du ViewPager, bottom nav visible (équivalent TabBar iOS).
- Catégories / Paramètres : versions V1 fonctionnelles branchées tant que les V2 restent des placeholders vides.
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

## Points ouverts (non traités)

| # | Sujet | Priorité |
|---|---|---|
| 1 | Onglet **Magasins** (iOS : 6 onglets) — BottomNavigationView limitée à 5 → décision produit | P1 |
| 2 | Bouton « Scanner » de l'ajout sans action ; flux OCR conçu pour NavController (crasherait) | P3 |
| 3 | Ajout : catégorie forcée à « Autre », pas de date ni de description | P3 |
| 4 | Accueil / Catégories / Réglages loin d'iOS (insights, budget, donut, apparence, export PDF…) | P2-P3 |
| 5 | Login / Register Android sans équivalent iOS (iOS : splash → onboarding → app) | À décider |
| 6 | `ETixApp` non déclarée dans le manifeste → journal de crash jamais alimenté | P4 |
| 7 | Manifeste : `.TicketEditActivity` déclarée mais inexistante | P4 |
| 8 | Room `fallbackToDestructiveMigration()` → perte de données à tout changement de schéma | P4 |
| 10 | Test `OCRValidationTest` désactivé : datasets vers fichiers absents, attentes ≠ fixtures | P3 |
| 9 | Login simulé : tout identifiant/mot de passe non vide est accepté, le mot de passe n'est ni stocké ni vérifié | Lié au #5 |
