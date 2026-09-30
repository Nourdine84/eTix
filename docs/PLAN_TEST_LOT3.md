# Plan de test — Lot 3 (Accueil, thème sombre, actions indisponibles)

App : **eTix QA** `1.3.0-lot3-qa` uniquement (`PROCEDURE_TEST_QA.md`). Mise à jour par `adb install -r`.
Conformité aux maquettes validées : **non confirmée** (maquettes introuvables) — référence = iOS `HomeView`.

## Données

| # | Magasin | Montant | Quand |
|---|---|---|---|
| D1 | `Lidl` | `20` | aujourd'hui |
| D2 | `Esso` | `30` | aujourd'hui |

Pour les cas avec comparaison, il faut ≥ 3 tickets dont au moins un le mois précédent : l'ajout Android force la date
du jour, donc créer un ticket puis modifier sa date est impossible aujourd'hui (limite connue) ; ces cas se vérifient
avec les captures CI ou un jeu de données QA existant.

## Cas

| ID | Action | Attendu |
|---|---|---|
| H01 | Premier lancement QA, aucun ticket | « Aucun ticket enregistré », 0,00 €, chip « — Pas de comparaison », « Ajoute ton premier ticket », pas de carte Tendance |
| H02 | Ajouter D1 | Compteur « 1 ticket enregistré », 20,00 €, « Ton suivi prend forme », carte Tendance visible, mois courant en bleu |
| H03 | Ajouter D2 | 50,00 €, panier moyen 25,00 € |
| H04 | Période Aujourd'hui / Cette année | Libellé « Dépenses du jour » / « Dépenses de l'année », montants cohérents |
| H05 | Heure < 18 h puis ≥ 18 h | « Bonjour / Bonne journée » puis « Bonsoir / Bonne soirée » |
| H06 | Toucher « Scanner un ticket » | Rien ne se passe ; bouton grisé ; mention « Scanner indisponible sur Android pour le moment » ; TalkBack annonce « désactivé » |
| H07 | « Ajout manuel » | Onglet Ajouter |
| H08 | « Voir l'historique » | Onglet Historique |
| H09 | Engrenage | Réglages ; Retour → Accueil |
| H10 | Rotation sur l'Accueil, période « Cette année » | Période conservée |
| S01 | Réglages → « Vider tous les tickets — indisponible » | Grisé, mention sous le bouton, **aucun ticket supprimé** (compter avant/après) |
| S02 | Ajouter → « Scanner un ticket — indisponible » | Grisé, aucun effet |
| T01 | Thème sombre (système) : parcourir les 5 onglets, détail, édition, Réglages, fiche magasin | Fonds sombres, textes lisibles, boutons bleus #007BFF à texte blanc, barre d'onglets sombre |
| T02 | Catégories en sombre | Total lisible en bas ; plus de ligne « 0,00 € » figée en haut |
| T03 | Édition d'un ticket | « Supprimer » rouge sur fond rouge pâle (clair et sombre) ; suppression unitaire inchangée |
| T04 | Réglages → Changer de thème (clair ↔ sombre) | Bascule immédiate, retour sur Réglages |
| I01 | `adb shell dumpsys package com.etix \| grep lastUpdateTime` | Identique à l'état initial |

## Hors périmètre (ne pas signaler comme bug)

Insights, carte Budget / Magasin de l'Accueil, étoiles du header sombre, animations d'entrée, rapport mensuel,
scanner, Catégories V1 (donut iOS), Réglages V1 (sections iOS).
