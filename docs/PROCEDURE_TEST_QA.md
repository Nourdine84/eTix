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

Cette empreinte est celle de votre clé locale (Android Studio). Elle ne sera jamais celle de la CI :
un APK `com.etix` produit par la CI ne pourrait pas remplacer votre app sans la désinstaller (perte des
tickets). C'est pourquoi la CI ne publie plus d'APK `com.etix` : seul eTix QA (`com.etix.qa`) est livré.

## 2. Installer eTix QA

1. Prérequis : signature QA durable configurée (`SIGNATURE_QA.md`). Sans elle, la CI ne publie aucun APK QA.
2. GitHub → dépôt `Nourdine84/eTix` → Actions → run indiqué dans le compte rendu → artefact `eTix-QA-<version>.apk`.
3. L'artefact est un zip ; il contient le fichier `eTix-QA-<version>.apk`.
4. Première installation :

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

```bash
adb install -r eTix-QA-<nouvelle-version>.apk     # met à jour uniquement com.etix.qa, données QA conservées
```

- Tous les APK QA publiés sont signés par la clé QA durable (voir `SIGNATURE_QA.md`) ; la CI refuse de
  publier un APK dont l'empreinte diffère de `QA_CERT_SHA256`.
- Aucune désinstallation n'est prévue dans le processus de mise à jour.
- Installer les lots dans l'ordre : un APK plus ancien (versionCode inférieur) est refusé par Android
  (`INSTALL_FAILED_VERSION_DOWNGRADE`) ; ce n'est pas une panne, prendre le dernier APK publié.
- Si `adb` répond `INSTALL_FAILED_UPDATE_INCOMPATIBLE` : **arrêter**, ne rien désinstaller, me transmettre
  le message et l'empreinte affichée dans le run. C'est une anomalie à analyser, pas une étape normale.

## 5. Fin de campagne (décision de votre part uniquement)

Retirer eTix QA du téléphone n'est jamais nécessaire au processus ; si vous le décidez un jour,
la commande ne concerne que le package `.qa` : `adb uninstall com.etix.qa`.
