# Stockage des tickets (Room) — diagnostic du risque de perte de données (02/10/2026)

Analyse en lecture seule de `feature/android-v2` au commit `86d9a19`. Aucun schéma, aucune migration et aucune
donnée n'ont été modifiés.

## Configuration actuelle

- `app/src/main/java/com/etix/data/AppDatabase.kt` : une seule base `etix.db`, une seule table `tickets`
  (entité `Ticket` : `id` INTEGER clé auto, `store` TEXT, `amount` REAL, `category` TEXT, `description` TEXT
  nullable, `dateMillis` INTEGER).
- `@Database(version = 1, exportSchema = false)`.
- **Repli destructif présent** : `Room.databaseBuilder(...).fallbackToDestructiveMigration()`.
- Aucune `Migration` déclarée. Le fichier `app/schemas/com.etix.data.AppDatabase/1.json` existe (ajouté en
  décembre 2025) et correspond au schéma actuel, mais il n'est plus régénéré (`exportSchema = false`).
- Les budgets ne sont pas dans Room (SharedPreferences `etix_budgets`) : non concernés.

## Historique du schéma (git)

| Période | Commit | Version Room | Entité `Ticket` |
|---|---|---|---|
| 08/2025 | `410e8d7` (premier commit) | 1 | `id` Int, `date` String, `description` non nullable, pas de `dateMillis` |
| depuis le 25/10/2025 | `ab09894` → `86d9a19` (`main`, `dev`, lots 1 à 9) | 1 | schéma actuel (identique jusqu'à `86d9a19`) |

Le schéma a changé une fois **sans changer de version** (août → octobre 2025). Depuis le 25/10/2025, il est stable.

## Chemins de mise à jour

| Depuis | Vers | Ce qui est vérifié | Ce qui ne l'est pas |
|---|---|---|---|
| lot 8 fusionné (`457f49e`, versionCode 10), base créée par les tests du lot 8 | lot 9 (11) | Émulateur API 34 : base ouverte sans migration, 1 ticket et 1 budget identiques avant / après (job `emulator-api34-maj-lot8`) | Bases contenant d'autres données (volumes, caractères, anciennes versions de l'app) |
| lot 9 (11) | build de test (12) | Émulateur API 21 / 34 / 36, mise à jour A → B : tickets et budgets créés par les tests conservés | Idem |
| versions construites depuis le 25/10/2025 (`main`, `dev`, lots 1 à 8) | lot 9 | Le code source de l'entité `Ticket` et `version = 1` sont identiques (lecture du code). L'empreinte de schéma calculée par Room n'a pas été comparée version par version, et aucune mise à jour n'a été exécutée depuis ces versions | Tout le reste : **non testé** |
| version d'août 2025 (`410e8d7`) | toute version récente | Rien | Voir risque 1 |

Le test lot 8 → version actuelle **ne couvre pas toutes les bases historiques** : il part d'une base neuve créée par le
lot 8 sur émulateur, pas d'une base produite par une version plus ancienne ni d'une base réelle d'utilisateur.

## Risque 1 — ancienne base à version identique (actuel, non confirmé)

- Situation : une base créée par la version d'août 2025 (`410e8d7`), dont le schéma diffère (`id` Int, `date`
  String, `description` non nullable), porte le même numéro de version 1 que le schéma actuel.
- Comportement attendu de Room (documentation, non reproduit ici) : à l'ouverture, l'empreinte enregistrée ne
  correspond pas, Room lève une `IllegalStateException` (« Room cannot verify the data integrity »). Le repli
  destructif ne s'applique pas à ce cas (il ne concerne que les changements de numéro de version) : **plantage au
  lancement**, données non effacées mais inaccessibles.
- Exposition : inconnue. La version de l'application installée sur le téléphone de Nourdine n'est pas connue (pas de
  téléphone disponible). Aucune installation, désinstallation ni manipulation de base n'est préparée pour le
  vérifier.

## Risque 2 — repli destructif lors d'un futur changement de version (éventuel)

- Situation : `fallbackToDestructiveMigration()` est actif et aucune migration n'existe.
- Au premier changement de l'entité `Ticket` (ou ajout de table), il faudra passer en version 2. Sans migration
  écrite, Room **supprime et recrée la table : tous les tickets sont effacés, sans message**. Si la version n'est
  pas incrémentée, on retombe dans le risque 1 (plantage).
- Non présent aujourd'hui : aucun changement de schéma n'est prévu dans le lot Réglages.
- Filet existant : le job `emulator-api34-maj-lot8` détecterait un effacement ou un plantage sur le chemin
  lot 8 → nouvelle version avant fusion ; il ne protège pas les autres bases historiques.

## Proposition

Proposition distincte, non appliquée : `docs/PROPOSITION_MIGRATIONS_ROOM.md`. Aucun changement de schéma ni de
migration dans le lot Réglages.
