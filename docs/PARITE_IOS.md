# Parité Android / iOS — bilan après le lot 10 (02/10/2026)

Objectif : choisir le prochain lot à partir des écarts restants. Aucun nouveau développement n'est lancé par ce
document.

## Références utilisées

- **Android** : `feature/android-v2` au commit `f102cfe` (fusion de la PR #80, lot 10 ; lots 1 à 10 intégrés). CI
  après fusion : https://github.com/Nourdine84/eTix/actions/runs/37027739385 (verte après une relance ciblée de
  `emulator-compat-dates (24)`, installation bloquée avant les tests, cause inconnue). Lot 9 : `86d9a19`, run
  https://github.com/Nourdine84/eTix/actions/runs/36999579628.
- **iOS** : `Nourdine84/etix-ios`, branche `feature/home-hero-v2`, code source lu le 02/10/2026 (`Views/`,
  `Intelligence/`, `Utils/`, `eTixWidget/`). Les maquettes « validées » sont introuvables : **le code iOS reste la
  référence provisoire** ; aucune conformité à un design validé n'est affirmée.
- Journal des lots et décisions : `docs/SUIVI_ANDROID.md` ; scanner : `docs/SCANNER.md` ; Réglages :
  `docs/REGLAGES.md` ; résultats émulateur : `docs/VALIDATION_EMULATEUR.md`.

## Légende

- **Présente** : fonctionnalité iOS portée. **Partielle** : portée en partie (écarts listés). **Absente** : non portée.
- Colonne « Émulateur » : couverte par les tests de bout en bout sur émulateur (API 21 / 34 / 36, et selon le cas
  fr-FR, petit écran, API 22-25). JVM / Robolectric ne sont pas repris ici.
- **Aucune ligne n'a de validation visuelle par Nourdine ni d'essai sur téléphone physique** : ces deux colonnes
  sont « à faire » partout. Une fusion ne vaut pas validation visuelle.

## Bilan par écran

| Écran / fonction | iOS (référence) | Android | Statut | Émulateur | Visuel (Nourdine) | Téléphone |
|---|---|---|---|---|---|---|
| Démarrage | `SplashView`, `OnboardingView` | Splash + onboarding V1 | Partielle (onboarding V2 non porté) | oui (`E2eParcoursTest`) | à faire | à faire |
| Connexion | aucune (splash → onboarding → app) | `LoginActivity` / `RegisterActivity` factices | Écart Android, décision reportée (29/09) | oui (parcours) | — | à faire |
| Accueil : hero, totaux, période | `HomeView` | `HomeFragmentV2` | Présente | oui | à faire | à faire |
| Accueil : carte Budget | `BudgetSummaryCardView` | lot 8 | Présente | oui (`E2eAccueilBudgetTest`) | à faire | à faire |
| Accueil : Tendance | `TrendView` | lot 8 | Présente (abréviations à 3 lettres : décision ouverte) | oui | à faire | à faire |
| Accueil : insights | `HomeInsightEngine`, `InsightCardView` | — | **Absente** | — | — | — |
| Accueil : carte Magasin | `StoreIntelligenceCardView` | — | **Absente** | — | — | — |
| Accueil : montants animés, étoiles du header sombre | `AnimatedAmountText` | — | **Absente** | — | — | — |
| Accueil → rapport mensuel | `MonthlyReportView`, `PDFExportService` | — | **Absente** | — | — | — |
| Ajouter : formulaire | `AddTicketView`, `TicketForm` | lots 3-4 (date, catégorie, description) | Présente (catégorie par défaut « Autre » : décision ouverte) | oui (virgule, clavier, petit écran) | à faire | à faire |
| Ajouter : scanner | `ScannerFlowView`, `OCRProcessingView` | lot 9 | Présente avec écarts voulus (appareil photo du système sans permission, galerie, marques « Détecté / À vérifier / Non lu ») ; inchangé au lot 10, toujours vert après fusion (API 21 / 34 / 36, fr, système, petit écran) | oui, ML Kit réel ; vrais sélecteur et appareil photo de l'émulateur (`E2eScanSystemeTest`) ; aucune vraie photo | à faire | **à faire (bloquant pour la qualité de lecture réelle)** |
| Catégorie proposée | `StoreCategoryMapper`, `CategoryPickerSheet` | lots 4 et 9 | Présente | oui | à faire | à faire |
| Historique : liste, sections, recherche, filtres | `TicketHistoryView`, `TicketFilterSheet` | lots 3-4 | Présente | oui (`E2eLot4Test`, fr) | à faire | à faire |
| Historique : suppression par balayage | `TicketHistoryView` | — | **Absente** (suppression depuis le détail seulement) | — | — | — |
| Export CSV | Réglages : tous les tickets (`SettingsViewModel.exportAllTickets`) | lot 10 : Réglages (tous les tickets) + Historique (tickets affichés, ajout Android) ; partage Android | Présente, même format qu'iOS ; écart voulu : champs texte protégés contre les formules | oui (contenu lu par l'URI partagée, vraie feuille de partage annulée) | à faire | **à faire (aucune application destinataire réelle, aucun import Excel essayé)** |
| Détail / édition ticket, suppression confirmée | `TicketDetailView`, `TicketEditView`, `ConfirmDeletePopup` | lot 6 | Présente (montant en bleu uni au lieu du dégradé, pas de retour haptique) | oui (`E2eDetailsTest`) | à faire | à faire |
| Catégories : liste, anneau, périodes | `CategoryView`, `CategoryDonutView` | lot 5 | Présente | oui (`E2eCategoriesTest`) | à faire | à faire |
| Catégories : graphique en barres | `CategoryBarChartView` | — | **Absente** (à confirmer à la revue) | — | — | — |
| Catégories : export | `CategoryExportButton` | — | **Absente** | — | — | — |
| Détail catégorie | `CategoryDetailView` | lot 6 | Partielle (« Voir par magasin » non porté) | oui | à faire | à faire |
| Budgets mensuels | `BudgetSettingsView` | lot 7 | Présente (« Budget atteint » en rouge : décision ouverte) | oui (`E2eBudgetsTest`, mise à jour) | à faire | à faire |
| Magasins : liste et fiche | `StoreListView`, `StoreDetailView` | lots 2 et 5 | Partielle (comparaison mensuelle présente ; graphique « Historique des achats » absent) | oui (parcours, fr) | à faire | à faire |
| Magasins : comparaison | `StoreComparisonView` (barres, anneau) | — | **Absente** | — | — | — |
| Réglages | `SettingsView` : Apparence (Thème), Période par défaut, Données (export CSV, suppression de tous les tickets avec confirmation), Informations | lot 10 : Thème Système / Clair / Sombre, Période par défaut (écrans ouverts inchangés), export CSV, compteur, Version, Build ; Compte et Diagnostic Android en fin d'écran | Présente avec écarts voulus : suppression globale désactivée, liste à choix au lieu du menu iOS (valeur sous le titre), journaux de plantage signalés indisponibles, accès par l'engrenage (pas de 6ᵉ onglet) | oui (`E2eReglagesTest`, redémarrage, petit écran `k09`, mise à jour lots 8 et 9) | à faire | à faire |
| Widget | `eTixWidget`, `WidgetSync` | — | **Absente** | — | — | — |
| Popups succès / erreur | `SuccessPopup`, `ErrorPopup` | messages courts (Toast) | Partielle | oui | à faire | à faire |

## Qualité et robustesse (hors parité stricte)

- Testé sur émulateur : mise à jour sans désinstallation (A → B ; lot 8 → lot 10 et lot 9 → lot 10 avec tickets,
  budgets et thème identiques), français, petit écran / police 2,0 (4 passes), Android 5 à 16 (API 21, 22-25 pour
  les dates, 34, 36). Ces tests partent de bases créées par les tests, pas de bases réelles d'utilisateurs
  (`docs/STOCKAGE_ROOM.md`).
- Incident CI récurrent, cause inconnue : installation d'APK bloquée sur émulateur API 24 / 25 (3 + 3 fois),
  `docs/FIABILITE_CI.md`.
- APK sans permission système (CAMERA, INTERNET, ACCESS_NETWORK_STATE absentes, contrôle CI bloquant).
- Points techniques ouverts (`docs/SUIVI_ANDROID.md`, « Points ouverts ») : `fallbackToDestructiveMigration`
  (perte de données à un futur changement de schéma), `ETixApp` non déclarée, activité inexistante déclarée,
  `OCRValidationTest` désactivé, connexion factice.
- Signature QA durable : à préparer séparément (`docs/SIGNATURE_QA.md`) ; sans elle, aucun APK QA installable à
  côté de l'app n'est publié, ce qui bloque aussi les essais sur téléphone.

## Validations encore manquantes (tous écrans)

1. **Revue visuelle par Nourdine** : aucun écran n'est approuvé. Parcours proposé : `docs/REVUE_VISUELLE.md`
   (captures des Réglages actualisées après le lot 10).
2. **Essais sur téléphone physique** (APK QA uniquement) : aucun. Prioritaires pour le scanner (vraies photos,
   application appareil photo du constructeur), l'export CSV ouvert dans une vraie application puis importé dans
   Excel (`docs/REGLAGES.md`), le thème « Système », le clavier réel et le thème sombre.
3. Décisions produit ouvertes : catégorie par défaut « Autre », saisie « ,20 », points visuels (Budget atteint en
   rouge, Tendance, barres, scanner, badges, bandeau, barre basse, présentation des choix des Réglages, fonds
   sombres hétérogènes), connexion, suppression de tous les tickets, activation des journaux de plantage.

## Pistes pour le prochain lot (à choisir par Nourdine)

Par écart restant, sans estimation engageante :

| Piste | Contenu | Prérequis / risque |
|---|---|---|
| ~~A. Réglages iOS~~ | **Fait au lot 10** (`f102cfe`), sauf la suppression de tous les tickets (désactivée par règle) | Validation visuelle et essais sur téléphone à faire |
| B. Magasins | Comparaison entre magasins, graphique « Historique des achats » | Aucun ; calculs déjà présents en partie |
| C. Accueil « intelligent » | Insights, carte Magasin, montants animés | Moteur d'insights iOS à porter, textes à valider |
| D. Historique | Suppression par balayage | Confirmation et annulation à définir |
| E. Rapport mensuel / export PDF | `MonthlyReportView`, `PDFExportService` | Plus large ; partage de fichiers |
| F. Fiabilité des données | Migrations Room explicites au lieu de la suppression destructive | Aucun changement de schéma sans accord ; risque futur détaillé dans `docs/STOCKAGE_ROOM.md` |
| G. Widget | `eTixWidget` | Plus tard |

Piste A (Réglages) choisie le 02/10/2026 et fusionnée au lot 10. **Prochain lot : non choisi** (décision de
Nourdine). Pistes restantes : B à G.

Hors lot fonctionnel : signature QA durable (préalable aux essais sur téléphone) et revue visuelle.
