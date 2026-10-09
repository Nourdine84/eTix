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
| États de ligne | < 80 % : barre verte, pas de libellé ; 80–100 % : orange « Attention — xx% » ; ≥ 100 % : rouge « Dépassé — xx% » | Seuils et couleurs identiques ; **libellé à 100 % pile : « Budget atteint »** (écart volontaire, B7 ci-dessous) |
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
| Titre « Il te reste X » ; en dépassement « Budgets dépassés de X » (rouge) ; « X dépensés sur Y prévus » | Identique, sauf à 100 % pile : « Budget atteint » (écart B3) |
| Pourcentage `Int(ratio × 100)` (troncature : 99,9 % → 99%) | Identique |
| « N jours restants dans le mois » (du début d'aujourd'hui au 1er du mois suivant : dernier jour = 1) | Identique |
| 3 lignes max triées par ratio décroissant, « et N autres → » | Identique (égalité départagée par le nom, iOS indéterministe) |
| Nom de ligne = casse du ticket le plus récent du mois, sinon clé capitalisée | Identique |
| Fond teinté : orange 8 % (attention), rouge 8 % (critique / dépassé) | Couleurs **opaques pré-mélangées** sur le fond de l’Accueil (8 % en clair, 16 % en sombre) : une teinte transparente laissait voir l’ombre de la carte en gris (Android 14) |
| `budgetTense` = état global critique ou dépassé → phrase « Ton rythme de dépenses augmente » (priorité après la maturité des données) | Identique |

### Écarts et ambiguïtés (à valider)

| # | Sujet | Choix |
|---|---|---|
| B1 | iOS choisit UNE carte contextuelle : Budget si action nécessaire, sinon carte Magasin (StoreIntelligence), sinon Budget informatif ; carte masquée si elle répète l'insight « budget dépassé » | Android n'a ni insights ni carte Magasin : la carte Budget s'affiche dès qu'un budget existe (« Ce mois »). À revoir si ces cartes sont portées |
| B2 | Montants iOS arrondis à l'euro (« %.0f € ») | Montants exacts si décimales (« 12,50 € »), comme au lot 7 (A1) |
| B3 | À 100 % pile, iOS affiche « Budgets dépassés de 0 € » | **Écart volontaire (demande produit)** : « Budget atteint » quand les dépenses égalent le budget au centime près ; « Budgets dépassés de X » seulement si elles le dépassent réellement (≥ 1 centime). L'état n'est pas modifié : à 100 % la carte reste rouge et l'Accueil « tendu », comme sur iOS. Tests : `BudgetSummaryEngineTest.textes_de_la_carte` (100 %, 0,1 + 0,2 = 0,3, ±1 centime), `Lot8ScreenshotTest.budget_atteint_light` |
| B4 | L'état « tendu » dépend du total global : une catégorie à 125 % ne rend pas l'Accueil « tendu » si le global reste < 80 % | Reproduit (règle iOS) |
| B5 | « et N autres → » n'est pas cliquable sur iOS | Identique (non cliquable) |
| B6 | Animations d'entrée de l'Accueil iOS | Non portées |

## Catégories : « Budget atteint » et lisibilité (décisions du 09/10/2026)

| # | Sujet | iOS (code `988eaf4`) | Android |
|---|---|---|---|
| B7 | Libellé à 100 % pile dans Catégories | « Dépassé — 100% » dès que total / budget ≥ 1 | **Écart volontaire (décision de Nourdine)**, comme la carte de l'Accueil (B3) : « Budget atteint » quand les dépenses égalent exactement le budget ; « Dépassé — xx% » seulement s'il est réellement dépassé. Comparaison des **montants exacts au centime** (`BudgetLine.overCents`), pas du pourcentage arrondi : 59,99 € / 60 € → « Attention — 100% » ; 60 € / 60 € → « Budget atteint » ; 60,01 € / 60 € → « Dépassé — 100% » ; 0,1 + 0,2 € pour 0,30 € → « Budget atteint ». Couleur rouge et barre pleine inchangées à 100 %, seuil d'alerte de 80 % inchangé, budgets partagés comparés sur la consommation cumulée. Tests : `BudgetRulesTest.budget_atteint_compare_les_montants_exacts`, `budget_partage_atteint_et_depasse`, émulateur `E2eRevueBudgetsTest` (captures `12b` à `12d`) |
| B8 | Noms longs (lignes, légende de l'anneau, réglage) | Aucune limite de lignes : le nom passe à la ligne (déduit du code, aucune capture iOS) | Nom complet, à la ligne entre les mots, jamais tronqué ni coupé en milieu de mot ; montant ou pourcentage placé **sous** le nom quand un mot ne tiendrait pas à côté (`TrailingValueRow`) ; police jamais réduite |
| B9 | Confirmation de suppression (écart A9) | Aucune | Fenêtre à contenu propre (`DeleteBudgetDialog`) : titre et message défilants, « Annuler » et « Supprimer » toujours entiers (côte à côte, ou empilés comme dans Material s'ils ne tiennent pas). Avant : à 320 dp, police 2,0, « Annuler » visible à 47 % et message coupé |
| B10 | Saisie d'un budget | Feuille au-dessus des onglets, titre = catégorie (barre de navigation) | Barre d'onglets masquée pendant la saisie et rétablie à la sortie ; titre dans la barre haute seulement s'il tient entier sur une ligne, sinon nom complet en tête de la zone défilante (« Annuler » / « Appliquer » toujours entiers) ; plus de réduction automatique de la police du titre (10 dp constatés) ; à l'ouverture du clavier, le haut de la zone n'est plus rogné (en entier, ou entièrement sorti de la vue) |
| B11 | Sélecteur de période de Catégories | Sélecteur segmenté du système | Libellés toujours entiers, même taille pour les trois, police jamais réduite : côte à côte s'ils tiennent, sinon empilés (un par ligne). Avant : « Cette ann… » et trois tailles différentes à 320 dp, police 2,0 |

## Écarts restants

- Carte « Budget » de l'Accueil et « budget tendu » : portés au lot 8 (voir ci-dessus).
- Rapport mensuel iOS (budgets) : non porté.

## Vérifications

JVM `BudgetRulesTest` (clé, saisie FR, seuils, libellés, restant/dépassement, changement de mois) ; Robolectric
`BudgetStoreTest` (stockage, persistance, donnée illisible, isolement) et `Lot7ScreenshotTest` (aperçus clair/sombre,
320 dp police 1,3, police 2,0, saisie, suppression d'un budget, tickets intacts) ; émulateur : `E2eBudgetAvantMajTest`
→ mise à jour A→B → `E2eBudgetApresMajTest`, `E2eBudgetsTest`, `E2eFrancaisTest.f05`, `E2eClavierPetitEcranTest.k04`.
Résultats : `docs/VALIDATION_EMULATEUR.md`.
