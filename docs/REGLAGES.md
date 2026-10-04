# Réglages (lot 10)

Périmètre validé par Nourdine le 02/10/2026 (`docs/PERIMETRE_REGLAGES.md` sur la PR documentaire). Référence :
iOS `SettingsView` / `SettingsViewModel` / `AppSettings`. Accès inchangé : engrenage de l'Accueil (pas de 6ᵉ onglet).

## Contenu de l'écran

| Section | Android (lot 10) | iOS | Écart |
|---|---|---|---|
| Apparence | Ligne « Thème » → liste Système / Clair / Sombre | Picker « Thème » (menu) | Liste à choix unique Android au lieu du menu iOS |
| Période par défaut | Ligne « Période » → Aujourd'hui / Ce mois / Cette année + note d'usage | Picker « Période » | Note « Appliquée à l'ouverture… » ajoutée |
| Données | Exporter tous les tickets (CSV) ; Supprimer tous les tickets **désactivé** (« Indisponible dans cette version ») ; compteur « N ticket(s) enregistré(s) » | Export ; suppression avec confirmation | Suppression globale volontairement désactivée ; compteur ajouté (demande de Nourdine) |
| Informations | Version, Build | Version, Build | — |
| Compte | « Connecté en tant que … », « Connexion locale : aucun mot de passe n'est vérifié. Elle ne protège pas tes données. », Se déconnecter | absent | Section Android conservée, en fin d'écran |
| Diagnostic | Collecte inactive (cas actuel) : « Journaux de plantage indisponibles : leur collecte n'est pas active dans cette version. Aucun journal n'est enregistré ni envoyé. », boutons afficher / supprimer désactivés tant qu'aucun journal n'existe. Collecte active : « conservés sur ce téléphone, jamais envoyés. » | absent | Section Android conservée, en fin d'écran |

Présentation : en-tête avec retour et cartes groupées, comme « Budgets mensuels » (lot 7). Une seule mise en page
pour les deux thèmes : l'ancienne variante `layout-night-v8/fragment_settings.xml` (copie sombre avec d'autres
identifiants, `btnDeleteDatabase`) a été retirée ; la nouvelle mise en page utilise les couleurs `v2_*` déclinées
en sombre. Les fonds sombres des autres écrans ne sont pas modifiés (choix visuel ouvert).

## Comportements

- **Thème** : stocké comme avant (`etix_session` / `theme_mode`, valeurs AppCompat). Aucune préférence → Système.
  Un choix existant de l'ancien bouton (clair ou sombre) est conservé et affiché tel quel ; toute autre valeur
  s'affiche « Système ». Appliqué immédiatement (écran recréé) et au démarrage (Splash, Login, Main inchangés).
- **Période par défaut** : `etix_settings` / `default_range` (absente → Ce mois, comme iOS). Lue à la création de
  l'Accueil, des Catégories et des Magasins, c'est-à-dire à la prochaine ouverture de l'app (les onglets restent
  créés pendant l'utilisation). **Un écran déjà ouvert garde sa sélection** : changer le réglage, changer d'onglet
  ou recréer l'écran (rotation, thème) ne la modifie pas (demande de Nourdine du 02/10/2026). L'Historique n'utilise
  pas ce réglage : ses filtres (recherche, dates) ne changent pas.
- **Export CSV** :
  - Réglages : tous les tickets (`eTix_tous_les_tickets_<horodatage>.csv`).
  - Historique : bouton « Exporter les N tickets affichés (CSV) » / « Exporter le ticket affiché (CSV) », limité
    aux tickets visibles (recherche et filtre de dates actifs), désactivé si la liste est vide
    (`eTix_tickets_affiches_<horodatage>.csv`).
  - Partage Android (`ACTION_SEND`, `text/csv`, sélecteur du système), fichier du cache exposé par le FileProvider
    existant, lecture accordée au seul destinataire. Aucune permission ajoutée, aucun réseau utilisé par eTix.
    Annuler le partage ne modifie rien.
  - Format (aligné sur iOS `exportAllTickets`) : en-tête `Date,Magasin,Montant (€),Catégorie,Description`,
    séparateur virgule, date `jj/mm/aaaa` dans le fuseau du téléphone, montant à 2 décimales avec un **point**
    (`1234.56`), UTF-8 sans BOM, fin de ligne `\n`, tickets du plus récent au plus ancien. Champ contenant virgule,
    guillemet ou retour à la ligne : entre guillemets, guillemets doublés.
  - **Protection contre les formules** (demande de Nourdine, écart iOS voulu) : un champ texte (magasin,
    catégorie, description) commençant par `=`, `+`, `-`, `@`, une tabulation ou un retour chariot est précédé
    d'une apostrophe **dans le fichier seulement** (`=SOMME(A1:A2)` → `'=SOMME(A1:A2)`). Les tickets enregistrés ne
    sont pas modifiés ; la date et le montant ne sont pas concernés.
  - Avant le lot 10, l'export de l'Historique écrivait seulement un fichier dans le cache privé (inaccessible).

## Points ouverts (décisions de Nourdine)

1. **Format CSV** : virgule + point décimal (iOS). **Accepté pour le lot 10 par Nourdine (02/10/2026)**, avec la
   procédure d'import ci-dessous. L'ouverture dans une vraie application destinataire reste une validation à faire.
2. Thème et période : ligne « titre / valeur choisie » ouvrant une liste à choix unique (Android) au lieu du menu
   déroulant iOS avec la valeur à droite — choix dicté par la lisibilité à 320 dp police 2,0, à valider visuellement.
3. Journaux de plantage : collecte inactive (`ETixApp` non déclarée dans le manifeste, point ouvert n° 6 du suivi).
   L'activer est une décision distincte : **non activée** (décision de Nourdine du 02/10/2026) ; l'écran l'indique.
4. iOS : `SettingsView` enregistre le thème sous la clé `appearance` alors que l'app lit `app.appearance` (choix du
   thème probablement sans effet sur iOS) — écart signalé, non reproduit.

## Import du fichier CSV dans Excel (français)

### Format réel du fichier (vérifié par les tests)

Vérifié par `CsvExporterTest` (JVM), `Lot10ScreenshotTest` (fichier écrit, Robolectric) et `E2eReglagesTest` r03 / r04
(contenu lu par l'URI réellement partagée, émulateur) :

- encodage UTF-8 **sans BOM**, fins de ligne `\n` (LF) ;
- séparateur de colonnes : **virgule** ; première ligne : `Date,Magasin,Montant (€),Catégorie,Description` ;
- date `jj/mm/aaaa` (fuseau du téléphone), montant avec **point décimal** et 2 décimales (`1234.56`, `0.05`), sans
  symbole € ni séparateur de milliers ;
- champ contenant une virgule, un guillemet ou un retour à la ligne : entre guillemets, guillemets internes doublés
  (exemples des tests : `"Durand, fils"`, `"Vis ""inox"""`, description sur deux lignes entre guillemets) ;
- **protection contre les formules** : magasin, catégorie ou description commençant par `=`, `+`, `-`, `@`, une
  tabulation ou un retour chariot reçoit une apostrophe en tête **dans le fichier seulement** : `=SOMME(A1:A2)` y
  devient `'=SOMME(A1:A2)` ; le ticket enregistré dans l'app n'est pas modifié. Date et montant ne sont pas concernés.

### Procédure documentée (non essayée)

**Aucun de ces pas n'a été essayé** : pas d'Excel ni de LibreOffice dans l'environnement de travail, et aucune
application destinataire réelle n'a ouvert le fichier. Étapes tirées du fonctionnement documenté d'Excel 365
(Windows, interface française) ; les libellés peuvent varier selon la version (Mac, 2016-2019).

1. Récupérer le fichier : depuis le partage Android, l'envoyer par courriel ou l'enregistrer dans Drive, puis le
   télécharger sur l'ordinateur.
2. **Ne pas l'ouvrir par double-clic** : avec les paramètres régionaux français, Excel attend le point-virgule ; le
   fichier s'afficherait vraisemblablement sur une seule colonne et les montants ne seraient pas lus comme des nombres.
3. Dans un classeur vide : **Données → À partir d'un fichier texte/CSV**, choisir le fichier.
4. Dans l'aperçu : **Origine du fichier** = « 65001 : Unicode (UTF-8) » (sinon les accents sont altérés) ;
   **Délimiteur** = « Virgule ».
5. **Transformer les données** (Power Query) :
   - colonne « Montant (€) » : Modifier le type → **Utilisation des paramètres régionaux** → Type « Nombre décimal »,
     Paramètres régionaux « Anglais (États-Unis) » (point décimal) ;
   - colonne « Date » : Modifier le type → **Utilisation des paramètres régionaux** → Type « Date », Paramètres
     régionaux « Français (France) » ;
   - laisser Magasin, Catégorie et Description en « Texte ».
6. **Fermer et charger**.

Effet attendu de la protection contre les formules (non vérifié dans Excel) : à l'import par Power Query, le champ
reste du texte ; l'apostrophe ajoutée par eTix devrait rester visible dans la cellule (`'=SOMME(A1:A2)`) et aucune
formule n'est exécutée. Conséquence : une description commençant volontairement par « - » ou « + » (ex. « -5 % »)
apparaîtra précédée d'une apostrophe dans le tableur.

Équivalent LibreOffice Calc (non essayé) : ouvrir le fichier ; dans la boîte d'import, Jeu de caractères
« Unicode (UTF-8) », Séparé par « Virgule », Langue « Anglais (USA) » pour le point décimal, type de la première
colonne « Date (JMA) ».

### Essais réellement effectués

| Élément | Fait | Où |
|---|---|---|
| Contenu exact du fichier (en-tête, séparateurs, guillemets, retours à la ligne, centimes, dates, formules) | oui | JVM, Robolectric, émulateur (lecture par l'URI partagée) |
| Fichier transmis par la feuille de partage ; annulation sans effet sur les données | oui | émulateur API 21 / 34 / 36 (`r05`) |
| Ouverture dans une application destinataire réelle (messagerie, Drive) | **non** | à faire sur téléphone |
| Import dans Excel ou LibreOffice | **non** | à faire (procédure ci-dessus) |

## Répartition de la couverture (tests)

Principe : la logique pure en JVM ; l'écran, les états et le contenu exporté en Robolectric ; ce qui dépend du vrai
système (thème réellement appliqué, feuille de partage, redémarrage du processus, mise à jour, petits écrans
mesurés) sur émulateur. Pas de doublon volontaire, sauf le contenu exporté, vérifié aux deux niveaux (fichier
écrit en Robolectric, flux réellement lu par l'URI partagée sur émulateur).

| Exigence | JVM | Robolectric (`Lot10ScreenshotTest`, `AppPreferencesTest`) | Émulateur |
|---|---|---|---|
| Thème : Système par défaut, choix existant conservé | `AppPreferencesTest` | affichage « Clair » conservé, liste, « Annuler » sans effet | mise à jour lot 8 / lot 9 : thème enregistré comparé et affiché (`E2eMajReglagesApresTest`) |
| Thème : choix enregistré et transmis à AppCompat | — | Sombre puis Système (préférence, mode AppCompat, relu à la réouverture) | — |
| Thème : apparence réellement appliquée (3 états), persistance après arrêt | — | — (recréation d'activité non fiable sous Robolectric) | `r01`, `E2eReglagesAvant/ApresRedemarrageTest` |
| Période : défaut, valeur illisible, choix conservé à la recréation | `AppPreferencesTest` | `periode_conservee_apres_recreation` | — |
| Période : écrans ouverts inchangés, onglets, réouverture | — | `periode_par_defaut` | `r02` (vrai ViewPager, relance de l'activité), redémarrage complet |
| Historique non affecté par la période | — | — | `r02` |
| CSV : format, échappement, fuseau, formules | `CsvExporterTest` | fichier écrit = `toCsv` (Réglages, Historique filtré) ; tickets stockés inchangés | flux lu par l'URI partagée, données délicates, formule (`r03`, `r04`) |
| Partage : intention, URI, droit de lecture | — | chooser, `ACTION_SEND`, URI du FileProvider | idem + vraie feuille du système annulée (`r05`) |
| Compteur, version, build, suppression désactivée | — | oui | `r01` (valeurs de l'APK installé) |
| Journaux indisponibles | — | note et boutons désactivés | — |
| Petit écran / police 2,0 | — | 320 dp police 2,0, clair et sombre (textes entiers) | `k09_reglages` passes a à d (mesures réelles) |
| Tickets et budgets inchangés | — | chaque test | chaque test `r0x` |

## Tests

- JVM : `CsvExporterTest` (en-tête, accents, virgule, guillemets, retours à la ligne, centimes, fuseau, langue).
- Robolectric : `AppPreferencesTest` (défauts, choix existant conservé, valeurs illisibles, période initiale) ;
  `Lot10ScreenshotTest` (aperçus clair / sombre / 320 dp police 2,0, listes de choix, thème choisi, export Réglages et
  Historique avec fichier relu, période sur les trois écrans, recréation, tickets et budgets inchangés).
- Émulateur (`E2eReglagesTest`, mode standard API 21 / 34 / 36) : thème trois états réellement appliqué, compteur,
  version, build, suppression désactivée ; période lue à l'ouverture, écrans ouverts inchangés, sans
  effet sur l'Historique ; exports relus par l'URI partagée (contenu exact, données délicates) ; vraie feuille de
  partage annulée ; tickets et budgets inchangés. Persistance après arrêt complet de l'app
  (`E2eReglagesAvantRedemarrageTest` → `am force-stop` → `E2eReglagesApresRedemarrageTest`).
- Petit écran (`k09_reglages`, passes a à d, sombre en passe d) ; mise à jour depuis le lot 8 **et** le lot 9
  fusionnés (`emulator-api34-maj-lot8`, `emulator-api34-maj-lot9`) avec thème enregistré comparé et affiché.
- Non fait : téléphone physique, vraie application destinataire (messagerie, Drive, tableur) ouvrant le fichier.
