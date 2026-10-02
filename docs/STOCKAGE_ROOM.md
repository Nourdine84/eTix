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

| Depuis | Vers | Comportement attendu | Vérifié |
|---|---|---|---|
| lot 8 (versionCode 10) | lot 9 (11) | même schéma, même version : base ouverte telle quelle, aucune migration | **oui**, émulateur : tickets et budgets identiques avant / après (job `emulator-api34-maj-lot8`) |
| lot 9 (11) | 12 (build de test) | idem | oui, émulateur API 21 / 34 / 36 (mise à jour A → B) |
| toute version construite depuis le 25/10/2025 (`main`, `dev`, lots) | 1.9.0-lot9 | même schéma : sans risque | par construction (schéma identique), pas testé version par version |
| version d'août 2025 (`410e8d7`) | toute version récente | schéma différent à version égale : Room refuse d'ouvrir la base (IllegalStateException « Room cannot verify the data integrity »). Le repli destructif **ne s'applique pas** à ce cas : plantage au démarrage, données non effacées mais inaccessibles | non testé |
| future version avec schéma modifié **et** version portée à 2 sans migration | — | `fallbackToDestructiveMigration()` **supprime et recrée la table : tous les tickets sont effacés, sans message** | — |

## Risque

- **Risque actuel concret** : seulement si un téléphone a encore installée une version construite avant le
  25/10/2025 (schéma d'août 2025). Il n'y a pas de perte silencieuse dans ce cas, mais un plantage au lancement après
  mise à jour. **À confirmer : quelle version de `com.etix` est installée sur le téléphone qui contient les vrais
  tickets ?** Si elle date d'après le 25/10/2025, aucun risque actuel.
- **Risque éventuel (futur)** : le premier changement de l'entité `Ticket` (ou l'ajout d'une table) obligera à
  passer en version 2. Avec le repli destructif et sans migration, la mise à jour **effacerait tous les tickets
  de l'utilisateur**. Si au contraire la version n'est pas incrémentée, l'app plante au lancement.
- Filets existants : le job CI `emulator-api34-maj-lot8` (mise à jour depuis une version installée, tickets et
  budgets comparés) détecterait l'effacement ou le plantage avant fusion. La règle « aucun changement de schéma
  sans accord » reste la protection principale.

## Proposition (non appliquée, à décider)

Avant tout futur changement de schéma, dans un lot dédié et avec accord :
1. retirer `fallbackToDestructiveMigration()` (une migration manquante provoquera alors une erreur visible en test
   plutôt qu'un effacement silencieux chez l'utilisateur) ;
2. activer `exportSchema = true` et conserver les fichiers de schéma versionnés ;
3. écrire chaque `Migration(n, n + 1)` et la tester (`MigrationTestHelper`) ainsi qu'en CI émulateur (mise à jour
   avec données réelles comparées, comme pour le lot 8 → lot 9).

Aucun de ces points n'est nécessaire tant que le schéma ne change pas ; ils réduisent le risque futur, ils ne
corrigent pas un défaut actuel.
