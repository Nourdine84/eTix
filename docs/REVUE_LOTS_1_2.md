# Revue — lots 1 et 2 (avant fusion)

**Statut : en attente de validation sur téléphone. Aucune fusion effectuée.**

| Élément | Valeur |
|---|---|
| Branche à fusionner | `feature/android-lot2-magasins` (contient le lot 1 : historique linéaire) |
| Tête revue | `b4bf75e` |
| Base | `feature/android-v2` (`6095a7b`, 16/01/2026) |
| Comparaison | https://github.com/Nourdine84/eTix/compare/feature/android-v2...feature/android-lot2-magasins |
| Volume | 15 commits · 46 fichiers · +2 373 / −63 · **0 fichier supprimé** |
| CI (tête) | run 36586335822 : build OK, 21 tests OK, 1 désactivé (OCR), aucun APK QA (secrets absents) |

## Contenu

### Lot 1 — navigation
- `FragmentAdapter` : onglets 4 et 5 affichaient l'Accueil.
- `MainActivityV2` : conteneur d'écrans poussés (détail, édition), Retour Android (dépile → Accueil → quitte), changement d'onglet = fermeture du détail.
- Détail : crash « Modifier » (`requireParentFragment`) ; collecte liée au cycle de vie de la vue (NPE).
- Édition : chargement unique, validation du montant, suppression d'un ticket ferme détail + édition.
- Montants : virgule acceptée (avant : refus à l'ajout, **0 € enregistré** à l'édition).
- Historique : tap → détail.

### Lot 2 — Magasins, Réglages, QA
- Onglet Magasins + fiche magasin (parité iOS `StoreListView` / `StoreDetailView`, sans comparaison ni graphique).
- Réglages : depuis l'engrenage de l'Accueil (limite de 5 onglets).
- Build type `qa` : `com.etix.qa`, « eTix QA », installable à côté de `com.etix`.
- Signature QA durable par secrets GitHub, empreinte épinglable, aucune clé de secours.
- CI : YAML réparé, JDK macOS contourné, erreurs et tests remontés en annotations, captures publiées.

## Fichiers modifiés (hors ajouts)

`.github/workflows/android-ci.yml`, `app/build.gradle.kts`, `FragmentAdapter.kt`, `AddTicketFragmentV2.kt`,
`TicketDetailFragmentV2.kt`, `TicketEditFragmentV2.kt`, `TicketHistoryAdapter.kt`, `TicketHistoryFragmentV2.kt`,
`HomeFragmentV2.kt`, `MainActivityV2.kt`, `activity_main_v2.xml`, `fragment_home_v2.xml`, `fragment_settings.xml`
(+ variante `night-v8`), `item_ticket_history.xml`, `menu_bottom_nav.xml`, `OCRValidationTest.kt` (`@Ignore`).

## Points d'attention pour la revue

| # | Point | Risque | Mitigation |
|---|---|---|---|
| R1 | `feature/android-v2` et `dev`/`main` **n'ont aucun ancêtre commun** | Fusion vers `dev`/`main` impossible sans `--allow-unrelated-histories` | Fusionner uniquement dans `feature/android-v2` ; l'intégration dans `dev`/`main` est une décision séparée |
| R2 | Réglages retirés des onglets | Habitude utilisateur | Engrenage en haut de l'Accueil ; test R01/R02 |
| R3 | Regroupement des magasins insensible à la casse (≠ iOS) | Écart de chiffres avec iOS sur des noms mal saisis | Assumé et documenté ; à confirmer produit |
| R4 | `gradle.properties` impose un JDK macOS | Build CLI hors Mac | Laissé tel quel (build local), surchargé en CI |
| R5 | Room `fallbackToDestructiveMigration()` (existant) | Perte de données à un changement de schéma | Aucun changement de schéma dans ces lots ; à traiter avant toute évolution du modèle |
| R6 | Connexion factice conservée | Illusion de protection | Documenté, non présenté comme sécurité ; décision reportée |

## Vérifications

| Type | Statut |
|---|---|
| Compilation CI | Fait |
| Tests unitaires JVM (statistiques magasins) | Fait, 7/7 |
| Tests Robolectric navigation (onglets, Retour, détail/édition, Réglages, fiche magasin) | Fait, 6/6 |
| Captures Robolectric (aperçu) | Fait, pas une validation visuelle |
| **Téléphone réel** | **À faire** : `PLAN_TEST_LOT2.md` + cas lot 1 ci-dessous |

### Cas lot 1 à exécuter sur téléphone (eTix QA)

| ID | Action | Attendu |
|---|---|---|
| L1-01 | Toucher chaque onglet | Écran propre à chaque onglet |
| L1-02 | Historique → ticket → Modifier → Retour → Retour | Détail, puis Historique ; l'app ne se ferme pas |
| L1-03 | Ajouter `12,50` | Ticket enregistré à 12,50 € |
| L1-04 | Éditer un montant en `8,40` | Enregistré à 8,40 € (plus jamais 0 €) |
| L1-05 | Détail ouvert → autre onglet | Détail fermé |
| L1-06 | Onglet Historique → Retour | Accueil ; second Retour → sortie |

## Proposition de fusion (après votre validation)

1. Pull request `feature/android-lot2-magasins` → `feature/android-v2`, en brouillon jusqu'à validation.
2. Fusion par *merge commit* (traçabilité des lots), pas de *squash*.
3. Le lot 3 (`feature/android-lot3-accueil-theme`, basé sur le lot 2) sera revu ensuite contre `feature/android-v2`.
4. Aucune suppression de branche après fusion sans votre accord.
