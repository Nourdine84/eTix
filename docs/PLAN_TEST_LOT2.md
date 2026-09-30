# Plan de test — Lot 2 (Magasins, Réglages depuis l'Accueil)

App : **eTix QA** `1.2.0-lot2-qa` uniquement (voir `PROCEDURE_TEST_QA.md`). Jeu de données créé dans l'app QA.
Au premier lancement : onboarding, puis connexion factice (identifiant quelconque).

## Jeu de données (onglet Ajouter, dans cet ordre)

| # | Magasin | Montant saisi |
|---|---|---|
| D1 | `Lidl` | `12,50` |
| D2 | `LIDL ` (espace final) | `7,30` |
| D3 | `Carrefour` | `30` |

Limite connue : l'ajout force la catégorie « Autre » et la date du jour.

## Cas

| ID | Action | Résultat attendu |
|---|---|---|
| M01 | Avant D1-D3 : onglet Magasins | Icône + « Aucun magasin sur cette période », « Ce mois » sélectionné |
| M02 | Après D1-D3 : Magasins, Ce mois | 2 cartes : N°1 Carrefour 30,00 € (1 ticket · 30,00 €/visite · 60 %) ; N°2 « LIDL » (nom du ticket le plus récent, espaces retirés) 19,80 € (2 tickets · 9,90 €/visite · 40 %) ; « Dernier passage aujourd'hui » |
| M03 | Aujourd'hui puis Cette année | Mêmes 2 cartes (tickets du jour) ; libellés des 3 boutons lisibles en entier |
| M04 | Tap sur la carte LIDL | Fiche : 19,80 €, « 2 tickets enregistrés », Panier moyen 9,90 €, Tickets 2, Dernière visite = aujourd'hui, Fréquence « tous les 1 j » |
| M05 | Fiche Lidl : comparaison | CE MOIS 19,80 € / MOIS DERNIER 0,00 €, aucun pourcentage affiché |
| M06 | Fiche Lidl : top catégories | « Autre » 100 % 20 € |
| M07 | Fiche → tap le ticket 12,50 € → Modifier → montant `15,00` → Enregistrer | Retour au détail ; Retour → fiche : total 22,30 € |
| M08 | Fiche → ticket → Modifier → vider le montant (puis essai avec `0`) → Enregistrer | Toast « Magasin et montant valides requis », rien enregistré, on reste sur l'édition |
| M09 | Fiche Carrefour → ticket → Modifier → Supprimer | Détail et édition fermés ; la fiche Carrefour se ferme seule (plus aucun ticket) ; Carrefour absent de la liste |
| M10 | Ajouter 5 tickets `Esso` puis ouvrir sa fiche | 5 lignes, pas de lien ; au 6ᵉ ticket : « Voir les 1 autres tickets → », tap → 6 lignes |
| M11 | Fiche ouverte, rotation de l'écran | Fiche conservée, liste dépliée conservée |
| R01 | Accueil → icône engrenage | Écran Paramètres, les 5 boutons ont un texte lisible (dont « Se déconnecter »), barre d'onglets visible |
| R02 | Paramètres → Retour Android | Retour Accueil, l'app ne se ferme pas |
| R03 | Paramètres → Changer de thème | Thème appliqué ; vérifier qu'on reste sur Paramètres puis Retour → Accueil |
| R04 | « Vider tous les tickets » | **Ne rien attendre** : bouton non câblé (point ouvert #13), aucune donnée ne doit disparaître |
| N01 | Onglets | 5 onglets libellés : Accueil, Ajouter, Historique, Catégories, Magasins |
| N02 | Magasins → Retour Android | Accueil ; second Retour → sortie de l'app |
| N03 | Fiche magasin ouverte → tap onglet Historique | Fiche fermée, Historique affiché |
| N04 | Fiche magasin → tap onglet Magasins (re-tap) | Fiche fermée, liste affichée |
| T01 | Thème sombre (système ou Paramètres) | Magasins et fiche : fond noir, cartes gris foncé, texte blanc, barre d'onglets sombre |
| I01 | Après la session : `adb shell dumpsys package com.etix \| grep lastUpdateTime` | Identique à l'état initial ; app « eTix » : même nombre de tickets |

## Hors périmètre du lot 2 (ne pas signaler comme bug)

Comparaison entre magasins (icône graphique iOS), graphique « Historique des achats », design de l'Accueil,
écran Catégories V1, scanner OCR.
