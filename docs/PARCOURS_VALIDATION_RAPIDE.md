# Parcours de validation rapide — eTix QA (≈ 10 min)

Périmètre : installation côte à côte, un ticket fictif, navigation, modification, Magasins, thème sombre.
**Aucune suppression de données dans ce parcours.** App testée : eTix QA (`com.etix.qa`) uniquement.

Prérequis : APK QA publié par la CI avec empreinte conforme (`SIGNATURE_QA.md`) ; `adb` du SDK Android Studio :
`ADB=~/Library/Android/sdk/platform-tools/adb`.

| # | Étape | Commande / action | Attendu |
|---|---|---|---|
| 1 | État initial (lecture seule) | `$ADB shell pm list packages com.etix` puis `$ADB shell dumpsys package com.etix \| grep lastUpdateTime` | Noter la présence de `com.etix` / `com.etix.qa` et `lastUpdateTime` de `com.etix` |
| 2 | QA déjà présente ? | Si `package:com.etix.qa` existe : **arrêter**, suivre `SIGNATURE_QA.md` § « Avant la première installation » | Aucune désinstallation |
| 3 | Vérifier le fichier | `shasum -a 256 eTix-QA-<version>.apk` | Identique à « fichier SHA-256 » de l'annotation CI |
| 4 | Installer | `$ADB install eTix-QA-<version>.apk` | `Success` ; icône « eTix QA » à côté de « eTix » |
| 5 | Isolation | Refaire l'étape 1 | `lastUpdateTime` de `com.etix` inchangé ; « eTix » affiche toujours vos tickets |
| 6 | Premier lancement QA | Ouvrir eTix QA → Commencer → identifiant `Testeur`, mot de passe `fictif` | Accueil « Aucun ticket enregistré » (connexion factice, ce n'est pas une protection) |
| 7 | Ticket fictif à virgule | Ajouter : magasin `Boulangerie Test`, montant `12,50` (taper la virgule au clavier), catégorie `Alimentation`, Enregistrer | Message « Ticket enregistré » ; Accueil : 12,50 €, « 1 ticket enregistré » |
| 8 | Navigation | Toucher les 5 onglets puis Retour depuis Magasins | Chaque onglet a son écran ; Retour → Accueil, puis sortie |
| 9 | Modification | Historique → ticket → Modifier → montant `15,75`, date = hier → Enregistrer → Retour | Détail puis Historique à 15,75 €, rangé sous « Hier » |
| 10 | Magasins | Onglet Magasins, périodes Aujourd'hui / Ce mois / Cette année, ouvrir la fiche | Montants cohérents avec l'étape 9 (« Aujourd'hui » vide si la date est hier) |
| 11 | Thème sombre | Engrenage → Changer de thème → parcourir Accueil, Historique, Magasins, Ajouter → re-basculer | Textes lisibles, boutons bleus à texte blanc, barre d'onglets sombre |
| 12 | Persistance | Fermer eTix QA (balayer dans les apps récentes), rouvrir | Session et ticket 15,75 € conservés |

Remonter pour chaque étape : OK / KO + capture ; en cas de KO, la commande `$ADB logcat -d > logcat.txt`.
