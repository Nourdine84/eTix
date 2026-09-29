# Procédure de test Android — sans toucher à l'app installée

Principe : on ne teste **jamais** sur `com.etix` (l'app qui contient vos tickets).
Les lots se testent avec **eTix QA** (`com.etix.qa`), une application distincte installée à côté :
données, signature et désinstallation séparées. Aucune commande ci-dessous ne modifie `com.etix`.

## 0. Pré-requis

- `adb` : `~/Library/Android/sdk/platform-tools/adb` (Android Studio).
- `apksigner` (facultatif, pour les empreintes) : `~/Library/Android/sdk/build-tools/<version>/apksigner`.
- Téléphone en débogage USB, `adb devices` le liste.

## 1. État initial (lecture seule)

```bash
adb shell pm list packages | grep -i etix
# attendu : package:com.etix   (pas de com.etix.qa)

adb shell dumpsys package com.etix | grep -E "versionName|versionCode|lastUpdateTime"
# noter versionName et lastUpdateTime : ils doivent être IDENTIQUES à la fin du test
```

Notez aussi, dans l'app eTix, le nombre de tickets de l'Historique (capture d'écran).

### Signature de l'app installée (facultatif, lecture seule)

```bash
adb shell pm path com.etix              # → package:/data/app/.../base.apk
adb pull <chemin>/base.apk etix-installee.apk
apksigner verify --print-certs etix-installee.apk | grep SHA-256
```

Comparez avec l'empreinte publiée par la CI (annotation « APK debug » du run).
Elles seront **différentes** (clé CI ≠ clé de votre Mac) : c'est pourquoi l'artefact `app-debug-apk`
(`app-debug.apk`, package `com.etix`) ne doit **pas** être utilisé — `adb install -r` échouerait avec
`INSTALL_FAILED_UPDATE_INCOMPATIBLE`, et le seul contournement serait de désinstaller `com.etix`
(perte des tickets). **Ne pas le faire.**

## 2. Installer eTix QA

1. GitHub → dépôt `Nourdine84/eTix` → Actions → run de la branche du lot → artefact `eTix-QA-<version>.apk`.
2. L'artefact est un zip ; il contient le fichier `eTix-QA-<version>.apk`.
3. Installation :

```bash
unzip eTix-QA-1.2.0-lot2-qa.apk.zip
adb install eTix-QA-1.2.0-lot2-qa.apk
```

(ou copier l'APK sur le téléphone et l'ouvrir ; autoriser les sources inconnues pour le gestionnaire de fichiers).

## 3. Vérifier l'isolement

```bash
adb shell pm list packages | grep -i etix
# attendu : package:com.etix  ET  package:com.etix.qa
adb shell dumpsys package com.etix | grep -E "versionName|lastUpdateTime"
# attendu : strictement identique à l'étape 1
```

Sur le téléphone : deux icônes, « eTix » et « eTix QA ». Ouvrir « eTix » : le nombre de tickets est inchangé.

## 4. Mises à jour de eTix QA

- Lot suivant : `adb install -r eTix-QA-<nouvelle-version>.apk` (met à jour **uniquement** `com.etix.qa`).
- Si `INSTALL_FAILED_UPDATE_INCOMPATIBLE` : la clé CI a changé (nouvelle branche sans secret `QA_KEYSTORE_B64`).
  Seule option : `adb uninstall com.etix.qa` — **vérifier le `.qa`** — cela efface uniquement les données de test.
- Pour une clé définitivement stable : voir « Clé de signature QA » ci-dessous.

## 5. Nettoyage (quand vous le décidez)

```bash
adb uninstall com.etix.qa     # jamais com.etix
```

## Clé de signature QA (recommandé, action de votre part)

Sans secret, la CI garde la clé en cache **par branche** : chaque nouvelle branche de lot produit une
nouvelle signature. Pour une signature stable sur tous les lots :

```bash
keytool -genkeypair -v -keystore etix-qa.keystore -alias androiddebugkey \
  -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=eTix QA"
base64 -i etix-qa.keystore | gh secret set QA_KEYSTORE_B64 -R Nourdine84/eTix
```

Clé de test uniquement (jamais pour une publication Play Store). Conservez `etix-qa.keystore` hors du dépôt.
