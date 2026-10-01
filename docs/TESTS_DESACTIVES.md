# Tests désactivés — inventaire et conditions de réactivation

**Une CI verte ne signifie pas que ces cas fonctionnent** : un test `@Ignore` n'est pas exécuté, donc ni réussi ni échoué.
Le résultat « moteur aujourd'hui » vient de `OCRFixtureObservationTest` (annotation « OCR observé », run 36631292574,
commit `2087d49`), qui exécute l'attente proposée **sans assertion**.

## Réconciliation des chiffres

| Rapport | Désactivés | Détail |
|---|---|---|
| Lot 5 (`747c558`) | **11** | 10 dans `OCRFixturesTest` + 1 `OCRValidationTest`. Le compte rendu du lot 5 ne détaillait que les 10 d'`OCRFixturesTest` (9 décisions Q1–Q4 + 1 défaut moteur) et omettait `OCRValidationTest` : c'est l'écart signalé. |
| Lot 6 | **10** | `s_long_montant_total_ttc` réactivé (défaut D1 corrigé, attente 40,80 inchangée) → 9 dans `OCRFixturesTest` + 1 `OCRValidationTest`. |
| Lot 9 | **1** | Décisions Q1–Q4 validées : les 9 tests d'`OCRFixturesTest` réactivés, attentes inchangées (7 passaient déjà, 2 ont demandé une correction du moteur : `t003_enseigne`, `t004_categorie`). Reste `OCRValidationTest` (obsolète, conservé). |

Aucun autre test désactivé dans le dépôt (JVM, Robolectric, émulateur).

## Inventaire (lot 6) — lignes 1 à 9 réactivées au lot 9

Types : **D** = décision produit en attente, l'attente proposée passe déjà ; **D+M** = décision produit en attente
**et** défaut moteur (l'attente proposée échouerait aujourd'hui) ; **O** = test obsolète (données absentes).

| # | Test | Type | Motif | Moteur aujourd'hui | Condition de réactivation |
|---|---|---|---|---|---|
| 1 | `OCRFixturesTest.t001_categorie` | D | Q2 : taxonomie cible | `Alimentation` = attendu | Valider Q2 (catégories iOS) → retirer `@Ignore` |
| 2 | `OCRFixturesTest.t002_categorie` | D | Q2 | `Alimentation` = attendu | Valider Q2 |
| 3 | `OCRFixturesTest.t003_enseigne` | **D+M** | Q1 : « TOTAL » seul = libellé ou enseigne ? | `TOTAL` ≠ aucune | Valider Q1 ; si « libellé » : **corriger l'extraction d'enseigne** puis réactiver |
| 4 | `OCRFixturesTest.t003_categorie` | D | Q1/Q2 | aucune = attendu | Valider Q1 et Q2 |
| 5 | `OCRFixturesTest.t004_date` | D | Q3 (jj/mm) et Q4 (date dans les attentes) | 12/01/2026 = attendu | Valider Q3 et Q4 |
| 6 | `OCRFixturesTest.t004_categorie` | **D+M** | Q2 (iOS : `esso` → Carburant) | aucune ≠ `Carburant` | Valider Q2 ; **ajouter la catégorie/mots-clés au moteur Android** (modifie les catégories : décision explicite requise) |
| 7 | `OCRFixturesTest.s_restaurant_categorie` | D | Q2 | `Restaurant` = attendu | Valider Q2 |
| 8 | `OCRFixturesTest.s_long_date` | D | Q4 | 15/09/2026 = attendu | Valider Q4 |
| 9 | `OCRFixturesTest.s_long_categorie` | D | Q2 | `Alimentation` = attendu | Valider Q2 |
| 10 | `OCRValidationTest.validate_ocr_results` | O | Fichiers `ticket_carrefour.txt` / `ticket_restaurant.txt` absents ; attente `Supermarché` hors taxonomie | non exécutable | Q5 : le **supprimer** au profit d'`OCRFixturesTest` (suppression = votre accord) ou fournir les 2 fichiers réels et réécrire ses attentes |

Bilan : 7 cas passeraient dès validation des propositions ; **2 échoueraient** (défauts moteur conditionnels à Q1 et Q2) ;
1 test obsolète. Les tickets synthétiques ne remplacent pas des tickets réels.

## Réactivé au lot 6

| Test | Avant | Après |
|---|---|---|
| `OCRFixturesTest.s_long_montant_total_ttc` | `@Ignore` (défaut D1 : `SOUS-TOTAL 42,80` retenu) | actif, réussi — `OCRAmountExtractor` ; + 18 cas de régression `OCRAmountRegressionTest` |

## Réactivés au lot 9 (décisions Q1 à Q4 validées le 01/10/2026)

Lignes 1 à 9 de l'inventaire : actives, attentes d'origine. Corrections moteur : enseigne « TOTAL » seule ignorée
(Q1, `t003_enseigne`) ; catégories iOS par enseigne (Q2, `t004_categorie` : ESSO → Carburant). Seul
`OCRValidationTest.validate_ocr_results` reste désactivé (obsolète, données absentes, conservé et documenté).
