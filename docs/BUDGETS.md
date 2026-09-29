# Budgets mensuels — règles iOS reproduites et ambiguïtés (lot 7)

Référence : `Nourdine84/etix-ios` branche `feature/home-hero-v2` (`988eaf4`) — `BudgetStore.swift`,
`BudgetSettingsView.swift` (+ `BudgetEditSheet`), `CategoryView.swift`, `CategoryRowView.swift`,
`BudgetSummaryEngine.swift`, `HomeView.swift`. Les maquettes validées restent introuvables : rendu **à valider**.

## Règles établies (reproduites)

| Sujet | iOS | Android lot 7 |
|---|---|---|
| Nature | Budget **par catégorie**, **mensuel**, reconduit chaque mois (pas de budget propre à un mois) | Identique |
| Budget global | **Aucune saisie globale.** Seul l'Accueil iOS agrège : total budgets = somme des budgets ; dépenses = uniquement catégories budgétées ; restant = total − dépenses ; état global 50 / 80 / 100 % (`BudgetSummaryEngine`) | Non porté (Accueil hors périmètre du lot) — voir « Écarts » |
| Stockage | `UserDefaults` « categoryBudgets », JSON { nom en minuscules : limite } | Préférences dédiées `etix_budgets`, même clé et même format. **Additif** : Room (tickets) non modifié, aucune migration |
| Clé | `category.lowercased()` | `lowercase(Locale.ROOT)` ; aucun autre traitement (espaces conservés, comme iOS) |
| Enregistrer | limite > 0 enregistrée ; nil ou ≤ 0 → budget retiré | Identique |
| Période | Barre affichée seulement sur « Ce mois » (`range == .month`) ; dépenses = total de la catégorie sur le mois courant [1er 00:00, 1er du mois suivant[ | Identique (mêmes bornes que l'écran Catégories) |
| Dépenses | Total de la ligne (catégorie **exacte**) | Identique |
| États de ligne | < 80 % : barre verte, pas de libellé ; 80–100 % : orange « Attention — xx% » ; ≥ 100 % : rouge « Dépassé — xx% » | Identique (100 % pile = dépassé) |
| Barre | Capsule 4 pt, remplissage borné à 100 % ; « dépensé / budget » à droite | Identique |
| Restant / dépassement | Non affiché sur la ligne (seulement dans la carte Accueil) | Non affiché visuellement ; **lu par TalkBack** (« reste 60 € » / « dépassé de 4,50 € ») |
| Invitation | « Définir des budgets mensuels » en fin de liste tant qu'**aucun** budget n'existe | Identique |
| Accès au réglage | Bouton de la barre de navigation (curseurs) + invitation | Bouton en haut à droite de « Catégories » + invitation |
| Liste du réglage | Catégories **distinctes des tickets**, non vides, triées ; budget ou « — » ; pied « Les budgets s'appliquent à la vue mensuelle. » | Identique ; si aucune catégorie : « Aucune catégorie : ajoutez d'abord un ticket. » |
| Saisie | « Ex : 300 », virgule acceptée, > 0 ; « Appliquer » inactif sinon ; « Supprimer le budget » si un budget existe (sans confirmation) | Identique ; message « Montant invalide » sous le champ |

## Ambiguïtés et choix faits (à valider)

| # | Ambiguïté | Choix Android | Raison |
|---|---|---|---|
| A1 | iOS affiche et **préremplit** le budget arrondi à l'euro (« %.0f ») : 12,50 € s'affiche « 13 € » et « Appliquer » sans retouche enregistre **13** | Montant exact : « 12,50 € », champ « 12,50 » ; entier sans décimales (« 300 € ») | Éviter une modification silencieuse de la donnée |
| A2 | « Courses » et « courses » (deux catégories distinctes des tickets) partagent la **même** clé de budget | Conservé (règle iOS) ; les deux lignes du réglage affichent le même budget | Pas de fusion ni de renommage des catégories existantes |
| A3 | Saisie iOS sans retrait des espaces (« 1 200 » refusé) | Règle de saisie Android des tickets : espaces retirés (« 1 200 » = 1 200 €) | Cohérence avec la saisie des montants de tickets |
| A4 | « ,20 » accepté (0,20 €) | Conservé, **décision en attente** (même question que pour les tickets) | Non tranché |
| A5 | Budget sur une catégorie **sans dépense ce mois** : aucune ligne dans Catégories, donc budget invisible hors réglage | Conservé | Règle iOS |
| A6 | Budget d'une catégorie qui n'a plus aucun ticket : invisible même dans le réglage, mais conservé | Conservé (aucun nettoyage automatique) | Pas de suppression de données |
| A7 | Libellé « Sans catégorie » (ticket à catégorie vide) | Exclu du réglage (iOS filtre les vides) | Règle iOS |
| A8 | Seuils différents Catégories (80 / 100 %) et Accueil (50 / 80 / 100 %) | Seuls ceux de Catégories sont utilisés | Accueil non porté |
| A9 | « Supprimer le budget » sans confirmation | Conservé (retire la limite de cette catégorie, aucun ticket touché) | Règle iOS ; à confirmer |

## Écarts restants

- Carte « Budget » de l'Accueil et effet « budget tendu » sur la phrase de l'Accueil (iOS `budgetTense`) : non portés.
- Rapport mensuel iOS (budgets) : non porté.

## Vérifications

JVM `BudgetRulesTest` (clé, saisie FR, seuils, libellés, restant/dépassement, changement de mois) ; Robolectric
`BudgetStoreTest` (stockage, persistance, donnée illisible, isolement) et `Lot7ScreenshotTest` (aperçus clair/sombre,
320 dp police 1,3, police 2,0, saisie, suppression d'un budget, tickets intacts) ; émulateur : `E2eBudgetAvantMajTest`
→ mise à jour A→B → `E2eBudgetApresMajTest`, `E2eBudgetsTest`, `E2eFrancaisTest.f05`, `E2eClavierPetitEcranTest.k04`.
Résultats : `docs/VALIDATION_EMULATEUR.md`.
