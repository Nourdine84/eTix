# Compatibilité Android (minSdk 21)

Application : minSdk 21 (Android 5.0), targetSdk 34, Java/Kotlin 17.

## 1. java.time dans le lecteur de dates OCR (lot 8, durcissement)

### Constat

- `OCRDateExtractor` (appelé par `OCRProcessor.process`) utilise `java.time` : `LocalDate`,
  `DateTimeFormatter`, `ZoneId` — disponibles sur Android seulement à partir de l'API 26 (8.0).
- Configuration effective avant correction : aucun désucrage (`isCoreLibraryDesugaringEnabled`
  absent de `app/build.gradle.kts`), minSdk 21. Lint NewApi : 8 alertes, toutes dans ce fichier.
- Exécution réelle sur émulateur **avant correction** (commit `21adde7`, run 36722937007,
  test `E2eCompatDatesOcrTest`) :
  - API 21 (Android 5.0.2) : **11 cas sur 11 en erreur**
    `java.lang.NoClassDefFoundError: Failed resolution of: Ljava/time/format/DateTimeFormatter;`
    (y compris le traitement OCR complet `OCRProcessor.process`). L'erreur n'est pas rattrapée par
    le `catch (Exception)` du lecteur : dans l'app, elle ferait planter l'écran appelant.
  - API 34 et API 36 : 11 cas sur 11 conformes, traitement complet conforme.
- API 22 à 25 : non exécutées (pas d'émulateur dans la CI) ; `java.time` y est également absent
  (API 26 minimum), même défaut attendu, corrigé de la même façon.

### Portée pour l'utilisateur

Le chemin n'est **pas atteignable dans l'app actuelle** : le scanner OCR est désactivé et
`OCRPreviewFragment` n'est déclaré que dans `nav_graph.xml`, utilisé par des activités absentes du
manifeste. Le défaut était latent ; il se serait déclenché sur Android 5 à 7.1 dès l'activation
du scanner. Le scanner reste désactivé ; les décisions Q1 à Q4 restent ouvertes.

### Options essayées

| Option | Résultat | Décision |
|---|---|---|
| Désucrage de la bibliothèque Java (`coreLibraryDesugaring`, `desugar_jdk_libs` 2.0.4) — commits `50e1558`, `b3a7fa7` | Lecteur de dates correct sur API 21 (11/11), **mais plantages natifs de l'app sur Android 5.0.2** : `SIGSEGV` (`fault addr 0x28`) dans le code compilé de l'app (`base.apk@classes.dex`, même adresse `pc 00645c34` à chaque fois), pendant 5 classes de test (parcours, budget avant mise à jour, persistance, historique, catégories) — runs 36724140825 et 36725178848. Aucun plantage de ce type avant ce changement (run 36722937007 : 26 réussis, seuls les 2 tests de dates en échec). Cause exacte dans ART 5.0 non établie | **Écartée** (retirée au commit `3129344`) |
| Réécriture sans `java.time`, règles reproduites à l'identique | Voir ci-dessous | **Retenue** |

### Correction retenue : lecteur de dates sans java.time (commit `3129344`)

`OCRDateExtractor` n'utilise plus que `String`, `Regex` et `java.util.TimeZone`. Les règles
d'origine (celles de `LocalDate.parse` + `DateTimeFormatter.ofPattern` en mode par défaut) sont
reproduites à l'identique : mêmes motifs et même ordre, jour et mois sur 2 chiffres, « yyyy » avec
4 chiffres (plus de 4 seulement précédés de « + »), « yy » = 2000 à 2099, jours hors mois ramenés au
dernier jour (31/04 → 30/04, 29/02/2025 → 28/02), même repli par expression régulière, première
ligne datée retenue, début du jour dans le fuseau de l'appareil.

Équivalence vérifiée sur la JVM contre l'implémentation d'origine, conservée en test
(`OCRDateExtractorJavaTimeReference`) — `OCRDateExtractorEquivalenceTest` :

- lecture : ~40 000 entrées construites (formats, séparateurs, bornes, signes, préfixes, suffixes)
  et 40 000 entrées aléatoires : résultats identiques ;
- début du jour : chaque date du 01/01/1901 au 31/12/2086 dans 11 fuseaux, dont des fuseaux qui
  changent d'heure à minuit (Sao Paulo, Santiago, La Havane, Beyrouth) et Apia (jour sauté en
  2011) : résultats identiques.

Différences connues (hors usage réel d'un ticket) :

- année au-delà de ~292 millions précédée de « + » (« 12/01/+300000000 ») : java.time levait
  `ArithmeticException` (plantage) ; la ligne est désormais ignorée ;
- dates avant 1901 ou après 2086 : le jour lu est identique, mais l'instant de début du jour peut
  différer (heure locale historique, règles futures) car la table des fuseaux de
  `java.util.TimeZone` n'est pas celle de `java.time`.

## 2. Contrôle lint NewApi bloquant

Étape CI « Lint NewApi (bloquant) » (job `build`) :

- échoue si `lintDebug` signale une alerte `NewApi` ou `InlinedApi` absente de la baseline ;
- échoue si la baseline contient une entrée `NewApi` / `InlinedApi` autre que les exceptions
  ci-dessous (pas de mise en baseline silencieuse) ;
- échoue si le rapport lint est absent (compatibilité non vérifiée) ;
- les autres catégories lint restent en rapport, comme avant (aucun masquage global).

### Exceptions existantes (baseline, inchangées)

| Règle | Fichier | Attribut | Effet sur API < niveau requis | Justification |
|---|---|---|---|---|
| NewApi | `src/main/res/values/themes.xml` | `android:windowLightStatusBar` (API 23) | Attribut de thème ignoré par Android 5.x : icônes de la barre d'état non assombries | Pas de plantage (app lancée et testée sur API 21) ; rendu de la barre d'état sur Android 5.x à vérifier visuellement |
| NewApi | `src/main/res/values/themes.xml` | `android:windowLightNavigationBar` (API 27) | Ignoré avant Android 8.1 : icônes de navigation non assombries | Idem |

Une correction propre (`values-v23` / `values-v27`) est possible mais modifie le thème : non faite
sans validation.
