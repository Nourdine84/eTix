# Signature QA durable — eTix QA (`com.etix.qa`)

But : toutes les versions d'eTix QA signées par **la même clé**, installées en **mise à jour** (`adb install -r`),
sans jamais désinstaller. La clé et son mot de passe ne passent **jamais** par le dépôt, les journaux ou la conversation.

## Règles appliquées par la CI

| Situation | Build QA | Publication de l'APK QA |
|---|---|---|
| Secrets absents | Non construit | Non |
| Secrets présents, variable `QA_CERT_SHA256` absente | Construit et inspecté | **Non** (avertissement avec l'empreinte obtenue) |
| Empreinte ≠ `QA_CERT_SHA256` | Construit | **Non** (échec du job) |
| Empreinte = `QA_CERT_SHA256` | Construit | Oui, annotation « APK QA publié » : commit, applicationId, version, empreinte, SHA-256 du fichier |

La réussite des tests est rapportée à part (annotation « Tests ») : un APK peut être publié alors qu'un test échoue,
et inversement. Les trois statuts — tests, production, publication — figurent séparément dans chaque compte rendu.

## Permissions minimales

- Dépôt personnel `Nourdine84/eTix` : seul le **propriétaire** (vous) peut créer des secrets et variables Actions ;
  un collaborateur, même en écriture, ne le peut pas. Aucune permission n'est à accorder à qui que ce soit.
- Méthode recommandée : **interface web GitHub** avec votre session → aucun jeton supplémentaire à créer.
- Je n'ai besoin d'aucun accès : je ne lis ni la clé ni le mot de passe ; je lis seulement l'empreinte publique
  affichée par la CI.

## Étapes (Mac, Terminal zsh) — environ 10 minutes

### 1. Créer la clé hors du dépôt et hors des dossiers synchronisés

```zsh
KEYTOOL="/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool"
mkdir -p ~/.etix-cles && chmod 700 ~/.etix-cles && cd ~/.etix-cles
"$KEYTOOL" -genkeypair -keystore etix-qa.p12 -storetype PKCS12 \
  -alias etix-qa -keyalg RSA -keysize 4096 -validity 10950 \
  -dname "CN=eTix QA, O=eTix"
chmod 600 etix-qa.p12
```

- `keytool` demande le mot de passe (saisie masquée, 2 fois) : ne le mettez jamais dans la commande.
- `~/.etix-cles` n'est ni dans `~/Documents` (souvent synchronisé iCloud) ni dans `~/AndroidStudioProjects`.

### 2. Calculer l'empreinte attendue (depuis la clé)

```zsh
"$KEYTOOL" -list -v -keystore ~/.etix-cles/etix-qa.p12 -storetype PKCS12 -alias etix-qa | grep "SHA256:"
```

Résultat : `SHA256: AB:CD:…` (32 paires). C'est une donnée **publique** : vous pouvez me la transmettre.

### 3. Sauvegarder

Gestionnaire de mots de passe : fichier `etix-qa.p12` en pièce jointe + mot de passe. Plus une copie hors ligne.

### 4. Déclarer secrets et variable (interface web)

GitHub → `Nourdine84/eTix` → **Settings → Secrets and variables → Actions**.

1. Onglet **Secrets** → *New repository secret* → nom `QA_KEYSTORE_B64`. Dans le Terminal :
   ```zsh
   base64 -i ~/.etix-cles/etix-qa.p12 | pbcopy
   ```
   puis coller dans le champ *Secret* (rien ne s'affiche dans le Terminal).
2. *New repository secret* → `QA_KEYSTORE_PASSWORD` → saisir le mot de passe.
3. Onglet **Variables** → *New repository variable* → `QA_CERT_SHA256` → coller l'empreinte de l'étape 2
   (avec ou sans `:`, majuscules acceptées).
4. Vider le presse-papiers :
   ```zsh
   pbcopy < /dev/null
   ```

### 5. Me prévenir

Je relance la CI. L'APK n'est publié que si l'empreinte du certificat signé = `QA_CERT_SHA256`.

## Avant la première installation sur un appareil

```zsh
adb shell pm list packages com.etix.qa
```

- Aucune ligne → première installation : `adb install eTix-QA-<version>.apk`.
- `package:com.etix.qa` présent (APK QA du lot 2, clés temporaires) → **arrêter**. Comparer :
  ```zsh
  adb shell pm path com.etix.qa                    # → package:/data/app/…/base.apk
  adb pull <chemin>/base.apk qa-installee.apk
  "/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" -printcert -jarfile qa-installee.apk | grep SHA256
  ```
  Si l'empreinte diffère de `QA_CERT_SHA256`, la mise à jour sera refusée par Android.
  Solution **sans perte ni désinstallation** : publier le build durable sous un identifiant QA nouveau
  (`com.etix.qa2`, « eTix QA 2 »), installé à côté ; l'ancienne eTix QA et ses données restent intactes
  jusqu'à votre décision. (Aucune désinstallation n'est proposée ni automatisée.)

## Perte de la clé

Les copies de l'étape 3 l'évitent. Sans clé, aucune mise à jour d'eTix QA ne serait possible :
il faudrait alors, là aussi, un nouvel identifiant QA.
