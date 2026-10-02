# Proposition — sécuriser les futures évolutions de la base Room (non appliquée)

Statut : **proposition, en attente de décision de Nourdine**. Rien n'est implémenté. Diagnostic : `docs/STOCKAGE_ROOM.md`.
À traiter dans un lot dédié, **avant** le premier changement du schéma (entité `Ticket` ou nouvelle table), jamais
en même temps qu'une fonctionnalité.

## Objectif

Qu'une évolution du schéma ne puisse ni effacer les tickets en silence (risque 2), ni être livrée sans avoir été
exécutée sur des bases existantes.

## Étapes proposées

1. **Rendre le schéma traçable** : `exportSchema = true`, fichiers `app/schemas/.../N.json` versionnés et relus en
   revue (le fichier `1.json` existant correspond déjà au schéma actuel).
2. **Retirer le repli destructif** (`fallbackToDestructiveMigration()`) : une migration manquante devient une erreur
   visible en test au lieu d'un effacement chez l'utilisateur. Sans changement de version, ce retrait n'a aucun
   effet sur les bases actuelles — à confirmer par les tests de mise à jour.
3. **Chaque changement de version accompagné d'une `Migration(n, n + 1)` écrite** (pas de migration automatique
   destructive), testée :
   - en JVM / instrumenté avec `MigrationTestHelper` (base créée au schéma N, migrée, données relues) ;
   - sur émulateur, mise à jour sans désinstallation depuis la version intégrée précédente avec données comparées
     (modèle : job `emulator-api34-maj-lot8`, à généraliser à « version intégrée précédente → nouvelle version »).
4. **Bases historiques** : documenter les versions réellement installées chez les utilisateurs (inconnu aujourd'hui).
   Pour le schéma d'août 2025 à version identique (risque 1), choisir explicitement : le laisser non pris en charge
   (documenté), ou ajouter une détection d'empreinte avec message clair. Aucune suppression automatique.
5. **Sauvegarde avant migration** (option à décider) : copie de `etix.db` avant la première ouverture d'une nouvelle
   version de schéma, conservée localement, jamais envoyée.

## Hors périmètre

Aucune synchronisation, aucun compte, aucune permission réseau, aucune suppression de données existantes.

## Coût et risque de la proposition

Faible tant que le schéma ne change pas (étapes 1-2 et tests) ; chaque migration future ajoute un test dédié. Le
retrait du repli destructif peut faire échouer des tests qui reposeraient sur une base recréée : à vérifier en CI.
