# Parcours de revue visuelle

**Seul l'Accueil est validé par Nourdine** (04/10/2026, voir ci-dessous). Aucun autre écran n'est approuvé. Ces captures servent de base à la revue ; elles ne valident ni les choix
visuels ni la parité avec iOS (référence provisoire : code iOS `feature/home-hero-v2`, maquettes introuvables dans les sources
accessibles, ce qui ne signifie pas qu'elles n'ont jamais existé ni été validées).

## Validations de Nourdine

| Date | Écran | Rendu validé | Portée |
|---|---|---|---|
| 04/10/2026 | 1. Accueil, clair et sombre | 6 captures émulateur API 34 en français, commit `6dc0707` (branche `chore/revue-accueil-captures`, PR #81 non fusionnée), run https://github.com/Nourdine84/eTix/actions/runs/37222538785 : [haut clair](preview/revue-accueil-6dc0707/api34_revue_accueil_1_haut_clair.jpg), [haut sombre](preview/revue-accueil-6dc0707/api34_revue_accueil_1_haut_sombre.jpg), [carte Budget clair](preview/revue-accueil-6dc0707/api34_revue_accueil_2_budget_clair.jpg), [carte Budget sombre](preview/revue-accueil-6dc0707/api34_revue_accueil_2_budget_sombre.jpg), [bas clair](preview/revue-accueil-6dc0707/api34_revue_accueil_3_bas_clair.jpg), [bas sombre](preview/revue-accueil-6dc0707/api34_revue_accueil_3_bas_sombre.jpg) | Ce rendu seulement (code de l'Accueil identique à `feature/android-v2` @ `aa4011c`). Ne vaut ni pour les écrans non examinés ni pour des essais sur téléphone physique. |

Accueil (04/10/2026) : rendu jugé suffisamment proche d'iOS, écarts actuels acceptables. Présentation conservée :
pas de refonte de l'en-tête, de la carte Budget, du sélecteur de période ni de la navigation. Panier moyen, « Voir
l'historique » et l'engrenage des Réglages conservés. Animations, étoiles de l'en-tête et cartes d'analyse : lot
distinct. Restent à vérifier séparément (constats avant toute modification de règle) : cohérence des seuils et
couleurs de budget entre Accueil et Catégories ; explication de l'écart entre dépenses totales et dépenses des
catégories budgétées.

## Captures de référence actuelles

- **Code capturé** : identique à `feature/android-v2` au commit `f102cfe` (lots 1 à 10). Captures produites par le
  run https://github.com/Nourdine84/eTix/actions/runs/37020784489 (résultat de fusion de la PR #80 avant fusion,
  label `apercus`) ; son code applicatif et ses tests sont identiques à `f102cfe` (seuls `docs/SUIVI_ANDROID.md` et
  `docs/FIABILITE_CI.md` diffèrent).
- Dossier : [`preview/revue-f102cfe/`](preview/revue-f102cfe/). Tests existants réutilisés, aucun test ajouté pour
  ces captures. Les captures des jobs de mise à jour (`emulator-api34-maj-*`) ne sont **pas** reprises : leur
  première phase tourne sur l'APK du lot 8 ou du lot 9.
- Trois sources, à ne pas confondre :
  - **Robolectric** (`l3_*`, `l7_*`, `l9_*`, `l10_*`, `02_*`, `03_*`) : rendu simulé sur la JVM, données de
    démonstration ;
  - **Émulateur API 34** (`api34_*`) : rendu réel Android 14, données des tests de bout en bout ; certaines captures
    montrent un message temporaire (« Ticket enregistré »…) ou une page en cours de défilement ;
  - **Émulateur API 36 petit écran** (`api36_k09_*`) : 320 dp et police 2,0 (passe d), 360 dp police 1,3 (passe a) ;
    **API 21** (`api21_*`) : Android 5.
- Aucune capture sur téléphone physique. Dates en anglais (« Oct 2, 2026 ») : langue de l'émulateur / Robolectric
  (en-US) ; en français, voir le job `emulator-api34-fr`.

Pour chaque écran : comparer à iOS, puis noter **conforme / à corriger / décision à prendre**.

| Étape | Clair (Robolectric) | Sombre (Robolectric) | Clair (émulateur) | Sombre (émulateur) |
|---|---|---|---|---|
| 1. Accueil (validé, voir plus haut) | [l3_01 clair](preview/revue-f102cfe/l3_01_accueil_light.jpg) | [l3_01 sombre](preview/revue-f102cfe/l3_01_accueil_dark.jpg) | [accueil, 1 ticket](preview/revue-f102cfe/api34_10_accueil_un_ticket.jpg) | [accueil sombre](preview/revue-f102cfe/api34_22_accueil_sombre.jpg) |
| 2. Historique | [l3_04 clair](preview/revue-f102cfe/l3_04_historique_light.jpg), [filtre + export](preview/revue-f102cfe/l10_08_historique_export_filtre_light.jpg) | [l3_04 sombre](preview/revue-f102cfe/l3_04_historique_dark.jpg) | [sections](preview/revue-f102cfe/api34_33_historique_sections.jpg), [recherche + export](preview/revue-f102cfe/api34_r04_historique_export_filtre.jpg) | [historique sombre](preview/revue-f102cfe/api34_23_historique_sombre.jpg) |
| 3. Ajouter | [l3_03 clair](preview/revue-f102cfe/l3_03_ajouter_light.jpg) | [l3_03 sombre](preview/revue-f102cfe/l3_03_ajouter_dark.jpg) | [formulaire complet](preview/revue-f102cfe/api34_32_ajout_formulaire_complet.jpg) | [ajouter sombre](preview/revue-f102cfe/api34_25_ajouter_sombre.jpg) |
| 4a. Scanner | [intro clair](preview/revue-f102cfe/l9_01_scan_intro_light.jpg) | [intro sombre](preview/revue-f102cfe/l9_01_scan_intro_dark.jpg) | [intro](preview/revue-f102cfe/api34_70_scan_intro.jpg) | — (non capturé sur émulateur) |
| 4b. Formulaire OCR | [prérempli clair](preview/revue-f102cfe/l9_04_formulaire_prerempli_light.jpg) | [prérempli sombre](preview/revue-f102cfe/l9_04_formulaire_prerempli_dark.jpg) | [prérempli](preview/revue-f102cfe/api34_71_scan_formulaire_prerempli.jpg), [champs non lus](preview/revue-f102cfe/api34_76_scan_champs_non_lus.jpg) | — (non capturé sur émulateur) |
| 5a. Catégories et budgets | [l7_01 clair](preview/revue-f102cfe/l7_01_categories_budgets_light.jpg) | [l7_01 sombre](preview/revue-f102cfe/l7_01_categories_budgets_dark.jpg) | [catégories mois](preview/revue-f102cfe/api34_40_categories_mois.jpg) | [catégories sombre](preview/revue-f102cfe/api34_43_categories_sombre.jpg), [budgets sombre](preview/revue-f102cfe/api34_57_budgets_sombre.jpg) |
| 5b. Réglage des budgets | [l7_03 clair](preview/revue-f102cfe/l7_03_reglage_budgets_light.jpg) | [l7_03 sombre](preview/revue-f102cfe/l7_03_reglage_budgets_dark.jpg) | — | — |
| 6. Magasins | [mois clair](preview/revue-f102cfe/02_magasins_mois_light.jpg) | [mois sombre](preview/revue-f102cfe/03_magasins_mois_dark.jpg) | [magasins mois](preview/revue-f102cfe/api34_16_magasins_mois.jpg) | [magasins sombre](preview/revue-f102cfe/api34_24_magasins_sombre.jpg) |
| 7a. Réglages (lot 10) | [haut](preview/revue-f102cfe/l10_01_reglages_haut_light.jpg), [bas](preview/revue-f102cfe/l10_02_reglages_bas_light.jpg), [l3_08](preview/revue-f102cfe/l3_08_reglages_light.jpg) | [haut](preview/revue-f102cfe/l10_03_reglages_haut_dark.jpg), [bas](preview/revue-f102cfe/l10_04_reglages_bas_dark.jpg), [l3_08](preview/revue-f102cfe/l3_08_reglages_dark.jpg) | [réglages (Système, clair)](preview/revue-f102cfe/api34_26_reglages_clair.jpg) | [sombre](preview/revue-f102cfe/api34_21_reglages_sombre.jpg), [haut](preview/revue-f102cfe/api34_r01_reglages_sombre_haut.jpg), [bas](preview/revue-f102cfe/api34_r01_reglages_sombre_bas.jpg) |

### Étape 7, compléments (Réglages)

| Sujet | Captures |
|---|---|
| Listes de choix | [thème](preview/revue-f102cfe/l10_09_choix_theme_light.jpg), [période](preview/revue-f102cfe/l10_10_choix_periode_light.jpg) (Robolectric) ; [période, 320 dp police 2,0](preview/revue-f102cfe/api36_k09_reglages_choix_periode_d.jpg) (émulateur API 36) |
| Petit écran, 320 dp police 2,0 | émulateur API 36 : [haut](preview/revue-f102cfe/api36_k09_reglages_haut_d.jpg), [bas](preview/revue-f102cfe/api36_k09_reglages_bas_d.jpg), [sombre haut](preview/revue-f102cfe/api36_k09_reglages_sombre_haut_d.jpg), [sombre bas](preview/revue-f102cfe/api36_k09_reglages_sombre_bas_d.jpg) ; 360 dp police 1,3 : [haut](preview/revue-f102cfe/api36_k09_reglages_haut_a.jpg) ; Robolectric : [haut](preview/revue-f102cfe/l10_05_reglages_320dp_police_2_haut_light.jpg), [bas](preview/revue-f102cfe/l10_06_reglages_320dp_police_2_bas_light.jpg), [sombre](preview/revue-f102cfe/l10_07_reglages_320dp_police_2_haut_dark.jpg), [liste période](preview/revue-f102cfe/l10_11_choix_periode_320dp_police_2_light.jpg) |
| Période par défaut | [réglée sur « Cette année »](preview/revue-f102cfe/api34_r02_reglages_periode_annee.jpg) ; [Accueil déjà ouvert : sélection conservée](preview/revue-f102cfe/api34_r02_accueil_choix_conserve.jpg) ; [réouverture : « Cette année »](preview/revue-f102cfe/api34_r02_accueil_reouverture_annee.jpg) |
| Thème « Système » | [émulateur en clair](preview/revue-f102cfe/api34_r01_reglages_systeme.jpg) (page défilée) |
| Partage du CSV | [feuille de partage API 34](preview/revue-f102cfe/api34_r05_feuille_de_partage.jpg) ; [API 21](preview/revue-f102cfe/api21_r05_feuille_de_partage.jpg) (« No apps can perform this action » : aucune application de l'émulateur n'accepte un CSV) |
| Après redémarrage (Sombre, Cette année) | [Réglages](preview/revue-f102cfe/api34_r06_reglages_apres_redemarrage.jpg) |
| Version affichée | [bas des Réglages, émulateur](preview/revue-f102cfe/api34_r01_reglages_sombre_bas.jpg) : « 1.10.0-lot10-maj », build 13 = build de test de mise à jour A → B de la CI (la version livrée est 1.10.0-lot10, build 12 : [Robolectric](preview/revue-f102cfe/l10_02_reglages_bas_light.jpg)) |

## Points relevés en préparant les captures (constats, pas des décisions)

- Réglages (lot 10) : thème et période en ligne « titre / valeur choisie » ouvrant une liste à choix unique (iOS :
  menu, valeur à droite) ; « Supprimer tous les tickets » grisé ; Compte et Diagnostic en fin d'écran ; boutons des
  journaux grisés (collecte inactive). À comparer à iOS `SettingsView`.
- Barre d'onglets visible sous les Réglages (comme les autres écrans superposés) ; elle réduit la place utile sur
  petit écran.
- Historique : bouton « Exporter les N tickets affichés (CSV) » sur deux lignes ; présentation à valider.
- Sur l'aperçu Robolectric de la liste des périodes à 320 dp police 2,0, la liste s'ouvre défilée (« Aujourd'hui »
  à moitié masqué) ; sur l'émulateur, les trois choix sont entiers.
- Thème sombre : deux fonds coexistent — bleu nuit (Accueil, Ajouter, formulaire OCR, intro du scanner) et noir
  pur (Historique, Catégories, Magasins, Réglage des budgets, Réglages). Choix visuel ouvert, non traité au lot 10.
- Décisions déjà ouvertes : « Budget atteint » en rouge, abréviations de la Tendance, retrait des barres, écran du
  scanner, badges, bandeau, barre basse, catégorie par défaut « Autre », saisie « ,20 ».

## Captures devenues obsolètes (ne pas utiliser comme référence actuelle)

- **`preview/revue-86d9a19/`** (lot 9, 02/10/2026) : conservé comme trace.
  - **Obsolètes** : étape 7 (Réglages V1 « Paramètres » : `l3_08_*`, `api34_21_reglages_sombre`,
    `api34_26_reglages_clair`) et étape 2 (bouton « Exporter CSV », remplacé par « Exporter les N tickets affichés
    (CSV) » : `l3_04_*`, `api34_23_historique_sombre`, `api34_33_historique_sections`).
  - Autres captures : écrans inchangés au lot 10, mais remplacés par leurs équivalents récents dans
    `preview/revue-f102cfe/`.
- `preview/lot2/` à `preview/lot9-revue/` : trace des lots, peuvent ne plus correspondre au code ; en particulier
  `preview/lot9/l9_02_scan_autorisation_light.jpg` (étape supprimée), `preview/lot9/l9_04_*` (badge « Vérifié »
  remplacé) et toute capture des Réglages V1.

## Après la revue

Consigner pour chaque écran la décision de Nourdine dans `docs/SUIVI_ANDROID.md` (ou une issue). Les essais sur
téléphone physique restent à faire avec l'APK QA, une fois la signature QA durable en place.
