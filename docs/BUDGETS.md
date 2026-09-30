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
| Saisie | Feuille avec barre haute « Annuler » / « Appliquer », « Ex : 300 », virgule acceptée, > 0 ; « Appliquer » inactif sinon ; « Supprimer le budget » si un budget existe (**sans confirmation**) | Écran poussé avec la même barre haute (boutons jamais sous le clavier) ; message « Montant invalide » sous le champ. Une fenêtre de dialogue a d'abord été essayée : sur 320 dp / police 1,3 et à police 2,0 le clavier recouvrait ses boutons (émulateur, run 36642473284) → abandonnée |

## Ambiguïtés et choix faits (à valider)

| # | Ambiguïté | Choix Android | Raison |
|---|---|---|---|
| A1 | iOS affiche et **préremplit** le budget arrondi à l'euro (« %.0f ») : 12,50 € s'affiche « 13 € » et « Appliquer » sans retouche enregistre **13** | **Écart volontaire avec iOS, demandé** : montant exact — « 12,50 € », champ « 12,50 » ; entier sans décimales (« 300 € »). Valider sans retoucher conserve 12,50 € | Éviter une modification silencieuse de la donnée |
| A2 | « Courses » et « courses » (deux catégories distinctes des tickets) partagent la **même** clé de budget | **Budget partagé, consommation cumulée** (décision du 30/09) : chaque ligne garde son total, mais le pourcentage, la barre et l'alerte utilisent la somme des dépenses de toutes les catégories de même clé ; mention « Budget partagé avec « … » · consommation cumulée ». Aucune fusion, aucun renommage | Écart avec iOS (qui compare chaque ligne séparément) |
| A3 | Saisie iOS sans retrait des espaces (« 1 200 » refusé) | Règle de saisie Android des tickets : espaces retirés (« 1 200 » = 1 200 €) | Cohérence avec la saisie des montants de tickets |
| A4 | « ,20 » accepté (0,20 €) | Conservé, **décision en attente** (même question que pour les tickets) | Non tranché |
| A5 | Budget sur une catégorie **sans dépense ce mois** : aucune ligne dans Catégories, donc budget invisible hors réglage | Conservé | Règle iOS |
| A6 | Budget d'une catégorie qui n'a plus aucun ticket : invisible même dans le réglage, mais conservé | Conservé (aucun nettoyage automatique) | Pas de suppression de données |
| A7 | Libellé « Sans catégorie » (ticket à catégorie vide) | Exclu du réglage (iOS filtre les vides) | Règle iOS |
| A8 | Seuils différents Catégories (80 / 100 %) et Accueil (50 / 80 / 100 %) | Seuls ceux de Catégories sont utilisés | Accueil non porté |
| A9 | iOS supprime le budget **sans confirmation** | **Confirmation explicite** (« Supprimer le budget ? », montant et catégorie rappelés, « Annuler » sans effet) — écart volontaire avec iOS, demandé | Protection contre une suppression involontaire |

## Casse : deux catégories, un budget (A2) — comportement retenu

Budget « Courses » = 40 € ; tickets du mois « Courses » 30 € et « courses » 20 €
(tests `BudgetRulesTest.budget_partage_par_la_casse_consommation_cumulee`, `Lot7ScreenshotTest.budget_partage_casse_*`,
émulateur `E2eBudgetsTest.g025_budget_partage_casse`).

| Où | Résultat |
|---|---|
| Ligne « Courses » | total 30,00 € ; budget **50 € / 40 €**, **Dépassé — 125%**, « Budget partagé avec « courses » · consommation cumulée » |
| Ligne « courses » | total 20,00 € ; mêmes barre, pourcentage et alerte ; « Budget partagé avec « Courses » … » |
| Réglage | deux lignes, même budget 40 € (une seule clé) |
| Agrégation des budgets (`BudgetRules.totals`, future carte Accueil) | chaque clé comptée **une fois** : total budgets 40 €, dépenses 50 € |
| Tickets / catégories | inchangés |

## Accueil : carte Budget et « budget tendu » (lot 8)

Référence iOS : `BudgetSummaryEngine.swift`, `BudgetSummaryCardView.swift`, `HomeView.swift` (`resolveContextCard`,
`budgetTense`), `HomeSnapshot.swift`, `FinancialStateEngine.swift`.

| Règle iOS | Android lot 8 |
|---|---|
| Carte seulement sur « Ce mois » et s'il existe au moins un budget > 0 | Identique |
| Toujours calculée sur le mois courant [1er 00:00, 1er du mois suivant[ | Identique (`BudgetSummaryEngine.compute`) |
| Total des budgets = somme des budgets, **chaque clé une fois** ; dépenses = catégories budgétées seulement, cumulées par clé (casse ignorée), catégories vides ignorées | Identique (réutilise `BudgetRules.totals`) |
| États globaux et par ligne : < 50 % confortable (bleu), 50–80 % attention (orange), 80–100 % critique (rouge), ≥ 100 % dépassé (rouge) | Identique |
| Titre « Il te reste X » ; en dépassement « Budgets dépassés de X » (rouge) ; « X dépensés sur Y prévus » | Identique |
| Pourcentage `Int(ratio × 100)` (troncature : 99,9 % → 99%) | Identique |
| « N jours restants dans le mois » (du début d'aujourd'hui au 1er du mois suivant : dernier jour = 1) | Identique |
| 3 lignes max triées par ratio décroissant, « et N autres → » | Identique (égalité départagée par le nom, iOS indéterministe) |
| Nom de ligne = casse du ticket le plus récent du mois, sinon clé capitalisée | Identique |
| Fond teinté : orange 8 % (attention), rouge 8 % (critique / dépassé) | Identique en clair ; **16 % en sombre** (8 % invisible sur fond noir) |
| `budgetTense` = état global critique ou dépassé → phrase « Ton rythme de dépenses augmente » (priorité après la maturité des données) | Identique |

### Écarts et ambiguïtés (à valider)

| # | Sujet | Choix |
|---|---|---|
| B1 | iOS choisit UNE carte contextuelle : Budget si action nécessaire, sinon carte Magasin (StoreIntelligence), sinon Budget informatif ; carte masquée si elle répète l'insight « budget dépassé » | Android n'a ni insights ni carte Magasin : la carte Budget s'affiche dès qu'un budget existe (« Ce mois »). À revoir si ces cartes sont portées |
| B2 | Montants iOS arrondis à l'euro (« %.0f € ») | Montants exacts si décimales (« 12,50 € »), comme au lot 7 (A1) |
| B3 | À 100 % pile, iOS affiche « Budgets dépassés de 0 € » | Reproduit (test figé) ; libellé à décider |
| B4 | L'état « tendu » dépend du total global : une catégorie à 125 % ne rend pas l'Accueil « tendu » si le global reste < 80 % | Reproduit (règle iOS) |
| B5 | « et N autres → » n'est pas cliquable sur iOS | Identique (non cliquable) |
| B6 | Animations d'entrée de l'Accueil iOS | Non portées |

## Écarts restants

- Carte « Budget » de l'Accueil et « budget tendu » : portés au lot 8 (voir ci-dessus).
- Rapport mensuel iOS (budgets) : non porté.

## Vérifications

JVM `BudgetRulesTest` (clé, saisie FR, seuils, libellés, restant/dépassement, changement de mois) ; Robolectric
`BudgetStoreTest` (stockage, persistance, donnée illisible, isolement) et `Lot7ScreenshotTest` (aperçus clair/sombre,
320 dp police 1,3, police 2,0, saisie, suppression d'un budget, tickets intacts) ; émulateur : `E2eBudgetAvantMajTest`
→ mise à jour A→B → `E2eBudgetApresMajTest`, `E2eBudgetsTest`, `E2eFrancaisTest.f05`, `E2eClavierPetitEcranTest.k04`.
Résultats : `docs/VALIDATION_EMULATEUR.md`.
