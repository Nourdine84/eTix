# Parcours de revue visuelle (proposé, non approuvé)

Parcours court pour la revue des écrans Android par Nourdine, à partir des aperçus existants. **Aucun de ces écrans
n'est approuvé** : ces aperçus servent de base à la revue, ils ne valident ni les choix visuels ni la parité avec
iOS (référence provisoire : code iOS `feature/home-hero-v2`, maquettes introuvables). Captures Robolectric
(rendu simulé) et captures d'émulateur (rendu réel) sont distinguées ; aucune capture de téléphone physique.

Pour chaque écran : comparer à iOS, puis noter **conforme / à corriger / décision à prendre**. Les points déjà
listés comme décisions ouvertes sont rappelés.

Aperçus les plus récents (état du code fusionné) : PR #78, run https://github.com/Nourdine84/eTix/actions/runs/36994446604
(jobs `preview*`, images en annotations).

## 1. Accueil
- Clair / sombre (lot 3, Robolectric) : [lot3-1](preview/lot3/lot3-1-accueil-clair-sombre.jpg),
  état vide [lot3-2](preview/lot3/lot3-2-accueil-vide-clair-sombre.jpg)
- Carte Budget (lot 8, Robolectric) : [confort](preview/lot8/l8_01_confort_light.jpg),
  [attention sombre](preview/lot8/l8_02_attention_dark.jpg), [dépassé](preview/lot8/l8_04_depasse_light.jpg),
  [atteint](preview/lot8/l8_09_atteint_light.jpg)
- Émulateur : [carte Budget API 34](preview/lot8-emulateur/api34-accueil-budget.jpg),
  [Tendance API 34](preview/lot8-emulateur/api34-64-accueil-tendance.jpg),
  [français et petit écran](preview/lot8-emulateur/fr-et-petit-ecran.jpg)
- Décisions ouvertes : « Budget atteint » en rouge, abréviations à 3 lettres de la Tendance, retrait de 4 dp des
  barres. Absents (voir `PARITE_IOS.md`) : insights, carte Magasin.

## 2. Historique
- Robolectric (lot 3) : [ajouter et historique, clair / sombre](preview/lot3/lot3-3-ajouter-historique-clair-sombre.jpg)
- Émulateur (lot 4) : [historique, filtres et recherche API 34](preview/lot4-emulateur/api34-6-lot4-historique.jpg),
  [API 21](preview/lot4-emulateur/api21-6-lot4-historique.jpg)
- Écart connu : pas de suppression par balayage ; export CSV placé dans l'Historique (iOS : Réglages).

## 3. Ajouter
- Émulateur (lot 4) : [formulaire API 34](preview/lot4-emulateur/api34-5-lot4-formulaire.jpg),
  [saisie à virgule et modification](preview/lot4-emulateur/api34-2-ajout-virgule-modification.jpg)
- Formulaire prérempli par un scan, 360 dp police 2,0 (émulateur, lot 9) :
  [k08 passe d](preview/lot9-revue/api36_k08_scan_formulaire_d.jpg)
- Décisions ouvertes : catégorie par défaut « Autre », saisie « ,20 ».

## 4. Scanner et formulaire OCR
- Intro, sélecteur réel, refus caméra, appareil photo réel (émulateur, lot 9) :
  [sélecteur de photos](preview/lot9-revue/api34_80_systeme_selecteur.jpg),
  [formulaire après le sélecteur](preview/lot9-revue/api34_81_systeme_selecteur_formulaire_prerempli.jpg),
  [retour après refus](preview/lot9-revue/api34_82_systeme_camera_refusee_retour_intro.jpg),
  [appareil photo de l'émulateur](preview/lot9-revue/api34_83_systeme_appareil_photo.jpg)
- Petit écran, police 2,0 (émulateur) : [intro compacte 320 dp](preview/lot9-revue/api36_k07_scan_intro_d.jpg),
  [360 dp](preview/lot9-revue/api36_k07_scan_intro_b.jpg) ; Robolectric :
  [intro compacte](preview/lot9-revue/l9_10_scan_intro_320dp_police_2_compact_light.jpg)
- Champs non lus (Robolectric, avant le texte « Saisir le montant » et la mention de date) :
  [l9_09](preview/lot9-revue/l9_09_champs_non_lus_light.jpg). Version finale (« Saisir le montant »,
  « Date non lue — aujourd'hui proposé ») : jobs d'aperçu
  https://github.com/Nourdine84/eTix/actions/runs/36994446604/job/110800694783 (émulateur API 34) et
  https://github.com/Nourdine84/eTix/actions/runs/36994446604/job/110799682051 (Robolectric).
- **Aperçus obsolètes à ne pas évaluer** : `preview/lot9/l9_02_scan_autorisation_light.jpg` (étape
  d'autorisation supprimée) et `preview/lot9/l9_04_*` (badge « Vérifié » remplacé).
- Décisions ouvertes : écran du scanner, badges, bandeau, barre basse ; écarts iOS voulus listés dans
  `docs/SCANNER.md`.

## 5. Catégories et budgets
- Catégories (lot 5, Robolectric) : [mois clair](preview/lot5/l5_01_categories_mois_light.jpg),
  [mois sombre](preview/lot5/l5_01_categories_mois_dark.jpg), [vide](preview/lot5/l5_04_categories_vide_light.jpg) ;
  émulateur : [API 34](preview/lot5-emulateur/api34-categories.jpg)
- Détail catégorie et ticket (lot 6) : [détail catégorie](preview/lot6/l6_01_categorie_detail_light.jpg),
  [détail ticket](preview/lot6/l6_03_ticket_detail_light.jpg) ; émulateur : [API 34](preview/lot6-emulateur/api34-details.jpg)
- Budgets (lot 7) : [catégories avec budgets](preview/lot7/l7_01_categories_budgets_light.jpg),
  [réglage des budgets](preview/lot7/l7_03_reglage_budgets_light.jpg),
  [saisie, police 2,0](preview/lot7/l7_09_saisie_budget_police_2_light.jpg) ; émulateur :
  [API 34](preview/lot7-emulateur/api34-budgets.jpg)

## 6. Magasins
- Robolectric (lot 2) : [liste des magasins](preview/lot2/apercu-lot2-magasins.jpg)
- Émulateur (lot 4) : [magasins et persistance API 34](preview/lot4-emulateur/api34-3-magasins-persistance.jpg)
- Écart connu : comparaison entre magasins et graphique « Historique des achats » absents.

## 7. Réglages
- Robolectric : [fiche magasin et réglages (lot 2)](preview/lot2/apercu-lot2-fiche-reglages.jpg),
  [édition et réglages, clair / sombre (lot 3)](preview/lot3/lot3-5-edition-reglages-clair-sombre.jpg)
- Émulateur : [thème sombre API 34](preview/lot4-emulateur/api34-4-theme-sombre.jpg)
- Écart connu : réglages V1 (pas d'Apparence système / clair / sombre, ni de période par défaut ; « Vider tous
  les tickets » désactivé).

## Après la revue
Consigner pour chaque écran la décision de Nourdine dans `docs/SUIVI_ANDROID.md` (ou une issue), puis choisir le
prochain lot dans `docs/PARITE_IOS.md`. Les essais sur téléphone physique restent à faire avec l'APK QA, une fois
la signature QA durable en place.
