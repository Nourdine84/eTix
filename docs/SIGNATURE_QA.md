# Signature QA durable — eTix QA (`com.etix.qa`)

Objectif : toutes les versions d'eTix QA sont signées par **la même clé**, pour que chaque nouvel APK
s'installe en **mise à jour** (`adb install -r`), sans jamais désinstaller.

## Principe

| Élément | Où | Visibilité |
|---|---|---|
| Keystore QA (PKCS12) | Chez vous : gestionnaire de mots de passe + 1 sauvegarde hors ligne | Privé |
| `QA_KEYSTORE_B64` | Secret GitHub Actions du dépôt `Nourdine84/eTix` | Chiffré, jamais affiché |
| `QA_KEYSTORE_PASSWORD` | Secret GitHub Actions | Chiffré, jamais affiché |
| `QA_CERT_SHA256` | **Variable** GitHub Actions (non secrète) | Empreinte publique du certificat |

- Rien de tout cela n'est dans le dépôt. `app/build.gradle.kts` lit uniquement des variables
  d'environnement (`ETIX_QA_KEYSTORE_FILE`, `ETIX_QA_KEYSTORE_PASSWORD`, alias `etix-qa`).
- En CI, le keystore est décodé dans le dossier temporaire du runner (droits 600), jamais dans le
  workspace ni l'artefact, et supprimé en fin de job. GitHub masque les secrets dans les journaux.
- **Sans secrets, aucun APK QA n'est publié** : pas de clé de secours, pas de cache.
- Avec `QA_CERT_SHA256` défini, un APK dont l'empreinte diffère **n'est pas publié** (échec CI) :
  impossible de recevoir un APK QA qui ne s'installerait pas en mise à jour.
- Les secrets ne sont pas transmis aux workflows déclenchés par des PR de forks (règle GitHub).

## Actions de votre part (une seule fois)

Permissions minimales : être **administrateur du dépôt** `Nourdine84/eTix` (vous l'êtes, propriétaire)
pour créer secrets et variables. Si vous passez par `gh` avec un jeton à portée fine :
dépôt `Nourdine84/eTix` uniquement, permissions **Secrets : lecture/écriture**, **Variables : lecture/écriture**
(et Metadata : lecture, imposée par GitHub). Aucune autre permission.

### 1. Créer la clé (sur votre Mac, hors du dépôt)

```bash
mkdir -p ~/Documents/etix-cles && cd ~/Documents/etix-cles
keytool -genkeypair -keystore etix-qa.p12 -storetype PKCS12 \
  -alias etix-qa -keyalg RSA -keysize 4096 -validity 10950 \
  -dname "CN=eTix QA, O=eTix"
```

`keytool` demande le mot de passe de façon masquée : ne le passez **pas** en argument
(il resterait dans l'historique du terminal). Ne créez pas ce dossier dans `~/AndroidStudioProjects/eTix`.

### 2. Conserver durablement

- Gestionnaire de mots de passe : fichier `etix-qa.p12` en pièce jointe + mot de passe.
- Une sauvegarde hors ligne (clé USB chiffrée, par exemple).
- Perdre la clé imposerait un jour de réinstaller eTix QA : c'est ce que ces deux copies évitent.

### 3. Déclarer les secrets

Interface : GitHub → `Nourdine84/eTix` → **Settings → Secrets and variables → Actions → Secrets → New repository secret**.

| Nom | Valeur |
|---|---|
| `QA_KEYSTORE_B64` | Contenu base64 du fichier : `base64 -i etix-qa.p12 \| pbcopy` puis coller (rien ne s'affiche dans le terminal) |
| `QA_KEYSTORE_PASSWORD` | Le mot de passe choisi à l'étape 1 |

Ou avec `gh` (saisie masquée, rien dans l'historique) :

```bash
base64 -i etix-qa.p12 | gh secret set QA_KEYSTORE_B64 -R Nourdine84/eTix
gh secret set QA_KEYSTORE_PASSWORD -R Nourdine84/eTix
```

Videz ensuite le presse-papiers (copier un autre texte).

### 4. Me prévenir

Je relance la CI. L'annotation « APK QA » du run donne l'empreinte SHA-256 complète (publique).

### 5. Épingler l'empreinte

GitHub → **Settings → Secrets and variables → Actions → Variables → New repository variable** :
`QA_CERT_SHA256` = l'empreinte donnée à l'étape 4 (64 caractères hexadécimaux, avec ou sans `:`).

À partir de là, un APK QA signé avec une autre clé ne peut plus être publié.

## Option de renforcement (non activée)

Un *Environment* GitHub `qa-signing` restreint aux branches `feature/*` et `fix/*` limiterait l'usage des
secrets à ces branches. Utile si d'autres personnes obtiennent un accès en écriture au dépôt ;
superflu tant que vous êtes seul contributeur.

## Si un APK QA a déjà été installé avec l'ancienne clé

Les APK QA des runs du 29/09 avant cette configuration (empreintes `85382a…`, `29aa43…`, `00076c…`,
`d67444…`) sont signés avec des clés temporaires de CI. **Ne les installez pas.** Si l'un d'eux est déjà
sur le téléphone, le passage à la clé durable ne pourra pas se faire en mise à jour : signalez-le avant
toute action, la décision vous revient.
