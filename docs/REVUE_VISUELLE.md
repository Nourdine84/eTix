# Parcours de revue visuelle (proposé, non approuvé)

**Aucun écran n'est approuvé par Nourdine.** Ces captures servent de base à la revue ; elles ne valident ni les choix
visuels ni la parité avec iOS (référence provisoire : code iOS `feature/home-hero-v2`, maquettes introuvables).

## Captures de référence actuelles

- **Code capturé** : `feature/android-v2` au commit `86d9a19` (lot 9 fusionné). Captures produites par le run
  https://github.com/Nourdine84/eTix/actions/runs/37011782681 (label `apercus` sur la PR #79, 113 jobs verts) sur
  le commit `2eef68a`, dont le code applicatif et la CI sont identiques à `86d9a19` (seuls des documents diffèrent).
- Dossier : [`preview/revue-86d9a19/`](preview/revue-86d9a19/). Tests existants réutilisés, aucun test ajouté.
- Deux sources, à ne pas confondre :
  - **Robolectric** (`l3_*`, `l7_*`, `l9_*`, `02_*`, `03_*`) : rendu simulé sur la JVM, données de démonstration ;
  - **Émulateur API 34** (`api34_*`) : rendu réel Android 14, données des tests de bout en bout ; certaines captures
    montrent un message temporaire (« Ticket enregistré », « Thème appliqué ») laissé par l'étape précédente du test.
- Aucune capture sur téléphone physique.

Pour chaque écran : comparer à iOS, puis noter **conforme / à corriger / décision à prendre**.

| Étape | Clair (Robolectric) | Sombre (Robolectric) | Clair (émulateur) | Sombre (émulateur) |
|---|---|---|---|---|
| 1. Accueil | [l3_01 clair](preview/revue-86d9a19/l3_01_accueil_light.jpg) | [l3_01 sombre](preview/revue-86d9a19/l3_01_accueil_dark.jpg) | [accueil, 1 ticket](preview/revue-86d9a19/api34_10_accueil_un_ticket.jpg) | [accueil sombre](preview/revue-86d9a19/api34_22_accueil_sombre.jpg) |
| 2. Historique | [l3_04 clair](preview/revue-86d9a19/l3_04_historique_light.jpg) | [l3_04 sombre](preview/revue-86d9a19/l3_04_historique_dark.jpg) | [sections](preview/revue-86d9a19/api34_33_historique_sections.jpg) | [historique sombre](preview/revue-86d9a19/api34_23_historique_sombre.jpg) |
| 3. Ajouter | [l3_03 clair](preview/revue-86d9a19/l3_03_ajouter_light.jpg) | [l3_03 sombre](preview/revue-86d9a19/l3_03_ajouter_dark.jpg) | [formulaire complet](preview/revue-86d9a19/api34_32_ajout_formulaire_complet.jpg) | [ajouter sombre](preview/revue-86d9a19/api34_25_ajouter_sombre.jpg) |
| 4a. Scanner | [intro clair](preview/revue-86d9a19/l9_01_scan_intro_light.jpg) | [intro sombre](preview/revue-86d9a19/l9_01_scan_intro_dark.jpg) | [intro](preview/revue-86d9a19/api34_70_scan_intro.jpg) | — (non capturé sur émulateur) |
| 4b. Formulaire OCR | [prérempli clair](preview/revue-86d9a19/l9_04_formulaire_prerempli_light.jpg) | [prérempli sombre](preview/revue-86d9a19/l9_04_formulaire_prerempli_dark.jpg) | [prérempli](preview/revue-86d9a19/api34_71_scan_formulaire_prerempli.jpg), [champs non lus](preview/revue-86d9a19/api34_76_scan_champs_non_lus.jpg) | — (non capturé sur émulateur) |
| 5a. Catégories et budgets | [l7_01 clair](preview/revue-86d9a19/l7_01_categories_budgets_light.jpg) | [l7_01 sombre](preview/revue-86d9a19/l7_01_categories_budgets_dark.jpg) | [catégories mois](preview/revue-86d9a19/api34_40_categories_mois.jpg) | [catégories sombre](preview/revue-86d9a19/api34_43_categories_sombre.jpg), [budgets sombre](preview/revue-86d9a19/api34_57_budgets_sombre.jpg) |
| 5b. Réglage des budgets | [l7_03 clair](preview/revue-86d9a19/l7_03_reglage_budgets_light.jpg) | [l7_03 sombre](preview/revue-86d9a19/l7_03_reglage_budgets_dark.jpg) | — | — |
| 6. Magasins | [mois clair](preview/revue-86d9a19/02_magasins_mois_light.jpg) | [mois sombre](preview/revue-86d9a19/03_magasins_mois_dark.jpg) | [magasins mois](preview/revue-86d9a19/api34_16_magasins_mois.jpg) | [magasins sombre](preview/revue-86d9a19/api34_24_magasins_sombre.jpg) |
| 7. Réglages | [l3_08 clair](preview/revue-86d9a19/l3_08_reglages_light.jpg) | [l3_08 sombre](preview/revue-86d9a19/l3_08_reglages_dark.jpg) | [réglages clair](preview/revue-86d9a19/api34_26_reglages_clair.jpg) | [réglages sombre](preview/revue-86d9a19/api34_21_reglages_sombre.jpg) |

## Points relevés en préparant les captures (constats, pas des décisions)

- Thème sombre : deux fonds coexistent — bleu nuit (Accueil, Ajouter, formulaire OCR, intro du scanner) et noir
  pur (Historique, Catégories, Magasins, Réglage des budgets, Réglages). À trancher à la revue.
- Réglages : écran V1 (titre « Paramètres » centré, boutons pleine largeur, « crash log » en anglais, « Connecté en
  tant que »), très éloigné d'iOS (liste par sections Apparence / Affichage / Données / Informations). Prochain lot
  retenu : `docs/PERIMETRE_REGLAGES.md`.
- Historique : bouton « Exporter CSV » sous la recherche ; l'export actuel n'est pas récupérable par l'utilisateur
  (voir `PERIMETRE_REGLAGES.md`).
- Dates affichées en anglais (« Oct 2, 2026 ») : langue de l'émulateur / de Robolectric (en-US) ; en français, voir
  le job `emulator-api34-fr`.
- Décisions déjà ouvertes : « Budget atteint » en rouge, abréviations de la Tendance, retrait des barres, écran du
  scanner, badges, bandeau, barre basse, catégorie par défaut « Autre », saisie « ,20 ».

## Historique (ne pas utiliser comme référence actuelle)

Les dossiers `preview/lot2/` à `preview/lot9-revue/` sont conservés comme trace des lots. Ils peuvent ne plus
correspondre au code actuel ; en particulier `preview/lot9/l9_02_scan_autorisation_light.jpg` (étape supprimée) et
`preview/lot9/l9_04_*` (badge « Vérifié » remplacé) sont obsolètes.

## Après la revue

Consigner pour chaque écran la décision de Nourdine dans `docs/SUIVI_ANDROID.md` (ou une issue). Les essais sur
téléphone physique restent à faire avec l'APK QA, une fois la signature QA durable en place.
