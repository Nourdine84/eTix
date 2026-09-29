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

## Tests séparés par fichier (lot 5) — `OCRFixturesTest`

Un test par fichier et par champ. Seules les attentes **établies par le texte** sont actives ; les autres sont
`@Ignore` avec leur motif exact. Aucune attente ajustée sur le moteur ; moteur et catégories **inchangés**.
`OCRValidationTest` (ancien, jeux de données absents) reste désactivé tel quel. Q1–Q4 restent des **propositions**.

| Fichier | Actifs (passent) | Bloqués (`@Ignore`) | Motif du blocage |
|---|---|---|---|
| `ticket_001.txt` | enseigne `LIDL`, montant 34,50, pas de date | catégorie | Q2 (taxonomie) |
| `ticket_002.txt` | enseigne `CARREFOUR`, montant 12,99, pas de date | catégorie | Q2 |
| `ticket_003.txt` | montant 58,20, pas de date | enseigne, catégorie | Q1 (« TOTAL » = libellé ?) ; moteur renvoie `TOTAL` |
| `ticket_004.txt` | enseigne `ESSO`, montant 23,45 | date, catégorie | Q3/Q4 (lecture jj/mm, date dans les attentes) ; Q2 (`Carburant` iOS, inconnu du moteur Android) |
| `synthetique/restaurant_synthetique.txt` | enseigne `LE PETIT BISTROT`, montant 16,70 | catégorie | Q2 (proposition : Restaurant) |
| `synthetique/ticket_long_synthetique.txt` | enseigne `CARREFOUR MARKET`, **montant 40,80 (lot 6)** | date, catégorie | Q4 ; Q2 (le défaut D1 `SOUS-TOTAL 42,80` est corrigé au lot 6) |

Résultat CI (commit `747c558`, run 36619891801) : **13 actifs réussis, 11 désactivés** (bilan JVM global : 63 réussis, 0 échec, 11 désactivés) (10 `OCRFixturesTest` + `OCRValidationTest`).

### Tickets synthétiques

`app/src/test/resources/ocr/synthetique/` (voir `LISEZMOI.md`) : textes **rédigés à la main**, jamais issus d'un
scan. Ils couvrent des structures absentes des 4 fichiers (addition de restaurant avec TVA 10 %, ticket long avec
15 articles, sous-total, remise, TVA). Ils **ne remplacent pas** une validation sur de vrais tickets photographiés
(bruit OCR, colonnes, polices, caractères mal lus) : celle-ci reste à faire au lot OCR, avec vos tickets réels.

## Lot 6 — correction du montant (cas D1)

`OCRAmountExtractor` (utilisé par `OCRProcessor` et `OCRTicketDraft`) : lignes jamais retenues comme total
(sous-total, HT, TVA, remise/réduction/bon, rendu, avoir) ; priorité NET À PAYER > TOTAL TTC > TOTAL, dernière
occurrence, montant après le mot-clé, ligne suivante si colonnes séparées ; sans total : paiement carte, puis
espèces − rendu. Indépendant des catégories et de l'enseigne. 18 cas de régression synthétiques
(`OCRAmountRegressionTest`). Inventaire des tests désactivés : `docs/TESTS_DESACTIVES.md`.

**Portée de la correction (reformulée au lot 7)** : les 18 cas de régression `OCRAmountRegressionTest` passent ;
cela ne constitue **pas** une garantie générale de sélection du bon montant. Les limites ci-dessous restent ouvertes,
et aucun ticket réel n'a encore été testé.

## Limites connues — montants ambigus (tests de caractérisation `OCRAmountAmbiguityTest`)

Ces tests **figent le comportement actuel** (parfois faux) pour qu'aucun changement de la règle ne passe inaperçu :
ils ne valident pas ce comportement. Toute évolution de la règle devra modifier ces tests explicitement.

| # | Texte (synthétique) | Lu aujourd'hui | Lecture probable / question |
|---|---|---|---|
| L1 | `TOTAL 2 125,00` / `2 125,00` | 2 125,00 | Milliers ou quantité 2 × 125,00 ? Ambigu sans contexte |
| L2 | `CAFE 2,50` + `SANDWICH 6,00` (aucun mot-clé) | 6,00 (plus grand) | Somme 8,50 ? Ticket incomplet |
| L3 | `TOTAL 1,234.56` (format anglo-saxon) | **234,56** | 1 234,56 — défaut probable (tickets étrangers) |
| L4 | `TOTAL -5,00` (remboursement) | aucun | Un ticket négatif n'existe pas dans l'app : à décider |
| L5 | `ESPECES 50,00` sans rendu ni total | 50,00 | Montant remis, pas forcément dû |
| L6 | deux `TOTAL TTC` (10,00 puis 12,00) | 12,00 (dernier) | Ticket corrigé/dupliqué : dernier plausible |
| L7 | `TOTAL 12,500` (3 décimales) + `CB 12,50` | 12,50 (repli carte) | Correct par chance ; 3 décimales non reconnues |
| L8 | `TOTAL 12.345,67` | 12 345,67 | Correct |
