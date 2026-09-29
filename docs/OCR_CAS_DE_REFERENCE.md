# OCR — cas de référence proposés (à valider)

Statut : **proposition**. `OCRValidationTest` reste désactivé (`@Ignore`). Aucune attente n'a été modifiée
pour faire passer un test. Les résultats « observés » viennent du moteur réel
(`OCRFixtureObservationTest`, run CI du lot 3, annotation « OCR observé »).

## Fixtures existantes (`app/src/test/resources/ocr/`)

| Fichier | Contenu (lignes) |
|---|---|
| `ticket_001.txt` | `LIDL` · `TOTAL 34,50` · `A BIENTOT` |
| `ticket_002.txt` | `CARREFOUR` · `TOTAL TTC 12,99` · `MERCI DE VOTRE VISITE` |
| `ticket_003.txt` | `TOTAL` · `TOTAL 58,20` |
| `ticket_004.txt` | `ESSO` · `12/01/2026` · `TOTAL TTC 23,45 €` · `CB VISA` · `MERCI DE VOTRE VISITE` |

Fichiers référencés par le test mais **absents** : `ticket_carrefour.txt`, `ticket_restaurant.txt`.

## Attentes proposées vs moteur actuel

| Fixture | Champ | Attente proposée | Observé (moteur Android) | Écart |
|---|---|---|---|---|
| 001 | Enseigne | `LIDL` | `LIDL` | — |
| 001 | Montant | `34.50` | `34.5` | — |
| 001 | Date | aucune (absente du texte) | aucune | — |
| 001 | Catégorie | `Alimentation` (iOS `StoreCategoryMapper`) | `Alimentation` (confiance 0,50) | — |
| 002 | Enseigne | `CARREFOUR` | `CARREFOUR` | — |
| 002 | Montant | `12.99` | `12.99` | — |
| 002 | Date | aucune | aucune | — |
| 002 | Catégorie | `Alimentation` | `Alimentation` (0,50) | — |
| 003 | Enseigne | **aucune** (le texte ne contient que le libellé « TOTAL ») | `TOTAL` | **Oui** : libellé pris pour une enseigne |
| 003 | Montant | `58.20` | `58.2` | — |
| 003 | Date | aucune | aucune | — |
| 003 | Catégorie | aucune | aucune | — |
| 004 | Enseigne | `ESSO` | `ESSO` | — |
| 004 | Montant | `23.45` | `23.45` | — |
| 004 | Date | `12/01/2026` lue jj/mm/aaaa (12 janvier) | `12/01/2026` | — |
| 004 | Catégorie | `Carburant` (iOS : `esso` → Carburant) | aucune | **Oui** : `esso` inconnu ; Android range le carburant dans `Transport` |

## Décisions à prendre

| # | Question | Proposition |
|---|---|---|
| Q1 | `ticket_003` : « TOTAL » seul est-il une enseigne (station TotalEnergies) ou le libellé du total ? | Libellé → enseigne attendue : aucune. Un vrai ticket TotalEnergies porterait « TOTALENERGIES » ou une adresse. |
| Q2 | Taxonomie : catégories cible Android = celles d'iOS (`Alimentation`, `Restaurant`, `Transport`, `Santé`, `Carburant`, …) ? | Oui, aligner sur iOS (`CategoryStyle` + `StoreCategoryMapper`) ; l'ancien test attendait `Supermarché`, absent des deux plateformes. |
| Q3 | Format de date ambigu `12/01/2026` | jj/mm/aaaa (usage FR), comme aujourd'hui. |
| Q4 | Faut-il ajouter la date aux attentes du test (`OCRExpected` n'a que enseigne/montant/catégorie) ? | Oui. |
| Q5 | Remplacer les datasets absents par les 4 fixtures existantes ? | Oui, plus 2 fixtures réelles à fournir (restaurant, ticket long multi-lignes avec sous-total/TVA — cas D1 iOS). |

## Conditions de réactivation de `OCRValidationTest`

1. Réponses à Q1–Q5.
2. Test réécrit sur les fixtures validées, attentes = tableau validé (pas les valeurs observées).
3. Réactivation dans le lot OCR **avec** les corrections moteur : avec les attentes proposées, 003 (enseigne)
   et 004 (catégorie) échouent aujourd'hui. Ces échecs sont le résultat attendu tant que le moteur n'est pas corrigé :
   on corrige le moteur, pas l'attente.
