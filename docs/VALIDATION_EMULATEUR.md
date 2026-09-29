# Validation sur émulateur — lots 1 à 4

**Ce n'est pas une validation sur téléphone physique.** Émulateurs Android officiels (Google) dans GitHub Actions,
données fictives uniquement, build de test signé avec la clé de développement du runner
(jamais publié, **pas** l'APK QA durable).

## Environnements

| | Récent | Minimum pris en charge (`minSdk 21`) |
|---|---|---|
| Android | 14 (API 34) | 5.0.2 (API 21) |
| Image | `google_apis` x86_64, `sdk_gphone64_x86_64` | `default` x86 (32 bits), « Android SDK built for x86 » |
| Clavier | Gboard / LatinIME, langue en-US | LatinIME, langue en-US |

Note : l'image API 21 **64 bits** n'a jamais démarré en CI (> 35 min) ; l'image 32 bits a été retenue.

## Résultat de référence — commit `b7fd557` (branche `feature/android-lot4-formulaire-historique`)

Run : https://github.com/Nourdine84/eTix/actions/runs/36601028467

| Niveau | Contenu | Résultat |
|---|---|---|
| JVM + Robolectric (`testDebugUnitTest`) | Règles Magasins / Accueil / formulaire / Historique ; navigation, actions protégées, captures simulées | 40 réussis, 1 désactivé (`OCRValidationTest`) |
| **Émulateur API 34** | 11 tests de bout en bout | **11/11** |
| **Émulateur API 21** | 11 tests de bout en bout | **11/11** |
| Plantages (logcat `FATAL EXCEPTION`) | API 34 / API 21 | aucun / aucun |
| Isolation eTix QA à côté de `com.etix` | `lastUpdateTime` avant/après installation | identique sur les deux |

### Tests de bout en bout (Espresso, vraie interface, vrai clavier)

| Test | Couvre |
|---|---|
| a01 | Premier lancement : onboarding → connexion factice → Accueil vide |
| a02 | États vides (Accueil, Ajouter, Historique, Catégories, Magasins), Scanner et « Vider tous les tickets » désactivés et sans effet |
| a03 | Ajout avec **« 12,50 » tapé au clavier** → Accueil 12,50 € |
| a04 | Historique → détail → modification (15,75) → Retour, pile correcte |
| a05 | Magasins : Ce mois / Aujourd'hui / Cette année, fiche magasin, montants cohérents |
| a06 | Défilement de l'Accueil ; bouton Enregistrer atteignable clavier ouvert |
| a07 | Thème sombre puis clair (Réglages, Accueil, Historique, Magasins, Ajouter) |
| Persistance | **Après `am force-stop`** et installation d'eTix QA : session et ticket 15,75 € intacts |
| c01 | Lot 4 : ajout avec date (sélecteur), catégorie « Carburant », description |
| c02 | Lot 4 : sections, recherche « carbu », filtre date de début, réinitialisation |
| c03 | Lot 4 : édition préremplie, catégorie libre « Péage fictif », reprise dans le sélecteur |

## Défauts trouvés par les émulateurs (invisibles en JVM/Robolectric) et corrigés

| # | Défaut | Où | Gravité | Correctif |
|---|---|---|---|---|
| E1 | **Virgule supprimée au clavier** : « 12,50 » → « 1250 » → ticket enregistré **1 250,00 €** | API 34 (lots 1-3) | Critique (données fausses) | `dd2b4fa` |
| E2 | **Plantage à l'ouverture de l'Accueil sur Android 5** (`RadialGradient radius must be > 0`) | API 21 (lot 3) | Critique | `05bd9f4` (lot 4), `a4e1347` (lot 3) |
| E3 | Engrenage Réglages / retour fiche magasin : 25 % de la zone tactile rognée | API 34 | Moyenne (accessibilité) | `dd2b4fa` |
| E4 | Sélecteur de catégorie : « Autre… » hors écran (3 boutons empilés) | API 34 (lot 4) | Moyenne (fonction inaccessible) | `eba128d` |
| E5 | Icônes de la barre d'état invisibles (blanc sur blanc) | API 21-22 | Moyenne | `d9f4e74` |
| E6 | Barre d'état bleu vif en thème sombre | toutes | Faible | `3ec9b5b` |
| E7 | Halo de l'Accueil à bords nets, libellés d'onglets tronqués (320 dp) | API 21 | Faible | `d9f4e74` |
| E8 | Texte de recherche tronqué | 360 dp | Faible | `eba128d` |

E5/E7 ne sont **pas** reportés sur la branche du lot 3 (dépendance à un fichier du lot 4 ; report annulé sans forcer).

## Aperçus (captures réelles d'émulateur)

`docs/preview/lot4-emulateur/` : `api34-*` et `api21-*` (démarrage/états vides, ajout à virgule/modification,
Magasins/persistance, thème sombre, formulaire lot 4, Historique lot 4). Captures pleine résolution : artefacts
`emulator-api34` / `emulator-api21` du run.

## Limites

- Émulateurs en anglais (dates « Sep 29, 2026 ») : sur un téléphone en français, format français attendu — **à vérifier sur appareil**.
- Pas de test sur tablette, grand écran, pliable, ni Android 6 à 13 intermédiaires.
- Pas de mesure de performance ni de batterie ; pas de TalkBack réel (libellés d'accessibilité posés, non écoutés).
- Suppression d'un ticket non couverte par ces parcours (hors périmètre demandé).

## Restant à faire sur téléphone physique

`PARCOURS_VALIDATION_RAPIDE.md` (installation côte à côte, ticket à virgule, navigation, modification, Magasins,
thème sombre, persistance), avec l'APK QA durable une fois la signature configurée.
