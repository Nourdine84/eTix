# Scanner de tickets (lot 9)

Référence iOS : `ScannerFlowView`, `CameraPrimingView`, `OCRProcessingView`, `TextRecognizer`, `ReceiptParser`,
`StoreCategoryMapper`, `AddTicketViewModel.handleOCRResult`, `TicketForm` (badges de confiance).

## Parcours Android

| Étape | Android | iOS |
|---|---|---|
| Entrée | « Scanner un ticket » sur l'Accueil (ouvre Ajouter + scanner) ou sur Ajouter | Identique |
| Intro | Cadre illustré, « Prendre une photo », « Choisir une image », « Annuler » | « Scanner », « Annuler » (pas de galerie) |
| Présentation | Plein écran, barre d'onglets masquée (depuis le 02/10/2026) | fullScreenCover |
| Autorisation | **Aucune** (02/10/2026) : eTix ne déclare pas la permission CAMERA. La photo est prise par l'application appareil photo du système, qui gère elle-même son accès à la caméra. Contrôlé en CI (permissions de l'APK) | CameraPrimingView puis demande système (iOS capture lui-même) |
| Capture | Application appareil photo du système (photo enregistrée dans le cache de l'app, supprimée après lecture) | VNDocumentCameraViewController (recadrage automatique) |
| Image choisie | Sélecteur système, image jamais modifiée ni copiée | Absent |
| Lecture | ML Kit Text Recognition (modèle latin embarqué, sur l'appareil, aucune image envoyée) ; image redressée (EXIF) et réduite à 4 Mpx au plus ; étapes « Analyse de l'image / Reconnaissance du texte / Extraction des informations » suivant l'avancement réel | Vision, étapes indicatives minutées |
| Rien détecté | « Aucune information détectée », « Réessayer », « Saisir manuellement » | Identique |
| Erreur | « Lecture impossible » (image illisible / reconnaissance en échec) ou « Appareil photo indisponible », mêmes boutons | Absent (retour intro) |
| Résultat | Formulaire « Ajouter » prérempli : magasin, montant, date remplacés s'ils sont lus ; catégorie seulement si aucune n'est choisie | Identique |
| Marques | Voir « Informations extraites » ci-dessous : « Détecté », « À vérifier », « Non lu » ; **jamais « Vérifié »** ; catégorie « Suggéré par l'OCR » | « Vérifié » / « À vérifier » (écart voulu, décision du 02/10/2026) |
| Catégorie | Historique du magasin (catégorie la plus fréquente, ≥ 3 tickets = sans badge), sinon dictionnaire iOS, sinon mots d'activité de l'en-tête ; « Autre » = sans catégorie | Historique, puis dictionnaire |
| Validation | **Seul « Enregistrer » crée le ticket**, une seule fois (double appui ignoré) ; bandeau « Informations détectées sur le ticket, non vérifiées. Contrôle chaque champ avant d'enregistrer. » + « Annuler le scan » (vide le formulaire) | « Enregistrer » ; pas de bandeau |
| Retour système | Ferme le scanner depuis n'importe quelle étape (lecture annulée), aucun ticket | Pas de retour (modale) |

Aucun ticket existant n'est lu pour être modifié : l'historique sert uniquement à proposer une catégorie.

## Informations extraites : ce que signifient les marques (02/10/2026)

Avant le 02/10/2026, « Vérifié » s'affichait **sans aucune action de l'utilisateur** : pour le montant dès qu'il
venait d'une ligne de total (NET À PAYER, TOTAL TTC, TOTAL), pour la date dès qu'une date était lue. C'était une
confiance de l'OCR, pas une vérification. Désormais :

| Marque | Quand | Couleur |
|---|---|---|
| **Détecté** / **Détectée** | Montant lu sur une seule ligne de total (ou plusieurs lignes de même montant) ; date lue, unique sur le ticket, ni future (au-delà de demain), ni antérieure de plus de 2 ans | bleu (primaire) |
| **À vérifier** | Enseigne (toujours, comme iOS) ; montant hors ligne de total (paiement CB, repli) ou lignes de total de montants différents ; plusieurs dates différentes, date future ou très ancienne | orange |
| **Non lu** / **Non lue** | Champ absent du ticket : le contenu affiché (vide, saisie précédente, **date du jour**) n'a pas été lu | orange |
| aucune | Champ modifié par l'utilisateur après le scan (la valeur est la sienne) ; la marque revient s'il rétablit la valeur lue | — |

Les valeurs retenues ne changent pas (règles Q1 à Q4 et lot 6) : seule la marque reflète l'ambiguïté. Aucune valeur
par défaut n'est produite par la lecture : montant absent = champ inchangé (vide en général), date absente = date
du formulaire inchangée (date du jour par défaut) marquée « Non lue », catégorie absente = « Choisir une catégorie »
(enregistrée « Autre », décision produit ouverte). La validation reste explicite : seul « Enregistrer » crée le
ticket.

## Robustesse (02/10/2026)

- Double appui sur « Enregistrer » : un seul enregistrement à la fois (ViewModel), bouton désactivé pendant
  l'insertion ; l'insertion ne dépend plus de l'écran (rotation pendant l'insertion : un seul ticket, formulaire
  vidé dans le nouvel écran).
- Annulation (retour) pendant la lecture : lecture annulée, aucun formulaire rempli plus tard, aucun ticket.
- Rotation / recréation : lecture poursuivie ; corrections, date, catégorie, marques, bandeau et résultat de scan
  en attente conservés.
- Images : orientation EXIF appliquée ; réduction à 4 Mpx au plus (décodage sous-échantillonné puis mise à
  l'échelle exacte) : une photo 12 Mpx n'est plus gardée en pleine résolution, un ticket long garde sa largeur.

## ML Kit : configuration

- Dépendance `com.google.mlkit:text-recognition:16.0.0` = modèle **embarqué dans l'APK** (variante « bundled ») :
  aucun téléchargement de modèle, ni au premier lancement ni ensuite. La variante qui télécharge le modèle par
  les services Google Play (`play-services-mlkit-text-recognition`) n'est pas utilisée.
- Reconnaissance **sur l'appareil** : l'image (Bitmap) est passée à ML Kit en mémoire ; eTix n'envoie aucune
  image. Vérifié sur émulateur sans réseau au premier lancement (`E2eScanHorsLigneTest`, mode avion).
- Point à connaître : d'après la documentation de Google, les API ML Kit peuvent envoyer à Google des
  **statistiques d'utilisation et de performance** (pas les images ni le texte lu) quand le réseau est disponible.
  Permissions réseau de l'APK : voir l'annotation « Permissions de l'APK » du job `build`. Les couper (par exemple
  en retirant la permission INTERNET si l'app n'en a pas besoin) est une décision produit, non prise.

## Composants réels et simulés

| Composant | Robolectric | Émulateur `E2eScanTest` / `E2eScanFrTest` / petit écran | Émulateur `E2eScanSystemeTest` | Téléphone |
|---|---|---|---|---|
| Reconnaissance ML Kit | simulée (texte fourni) | **réelle** | **réelle** | à faire |
| Application appareil photo | non lancée (intention vérifiée) | simulée (Espresso-Intents écrit l'image) | **réelle** (appareil photo de l'émulateur, caméra « emulated », image de synthèse) | à faire |
| Refus d'accès caméra | — | — | **réel**, dans l'application appareil photo (eTix ne demande rien) | à faire |
| Sélecteur d'image | non lancé | simulé (Espresso-Intents) | **réel** (sélecteur du système) | à faire |
| Images | générées | générées + EXIF / 48 Mpx / ticket long (fichiers fournis) | générée | vraies photos à faire |
| Réseau | — | présent | **coupé** (mode avion) pour la 1re lecture | à faire |

## Tests

Revue du 02/10/2026 : `docs/VALIDATION_EMULATEUR.md` (résultats, constats, composants réels / simulés ci-dessus).
Robolectric ajoute : marques « Détecté » / « À vérifier » / « Non lu », valeurs ambiguës, marque retirée à la
modification, double appui, annulation pendant la lecture, recréation pendant la lecture / après correction /
pendant l'insertion, appareil photo sans autorisation, barre compacte à 320 dp police 2,0. Émulateur ajoute :
`E2eScanTest` s04 à s09, `E2eScanFrTest`, `E2eScanSystemeTest`, `E2eScanHorsLigneTest`, `k07` / `k08` (4 passes),
`E2eMajInstantaneAvant/ApresTest`.

Version initiale :

- JVM : `ReceiptScanParserTest` (confiances, rien détecté, historique / dictionnaire), `OCRScanRulesTest`
  (Q1 à Q4), `OCRFixturesTest` (9 tests réactivés).
- Robolectric (`Lot9ScanTest`, lecteur simulé) : intro, autorisation puis refus, lecture en cours, formulaire
  prérempli (badges, catégorie), « Annuler le scan », enregistrement après correction, historique prioritaire sans
  reclasser, rien détecté / réessayer, erreur / saisie manuelle, image illisible, retour système, Accueil → scanner,
  320 dp police 2,0. Tickets existants comparés avant / après dans chaque test.
- Émulateur (`E2eScanTest`, ML Kit réel sur une image de ticket générée ; appareil photo et sélecteur simulés par
  Espresso-Intents) : image choisie → formulaire prérempli → « Annuler le scan » ; photo depuis l'Accueil →
  correction → enregistrement (vérifie magasin, montant, catégorie, date du 12/01/2026) ; image blanche → rien
  détecté → réessayer → retour. Petit écran : boutons de l'intro atteignables (`k07`).

## Limites

- Pas de téléphone physique : vraies photos (flou, pli, lumière, ticket long réel), application appareil photo
  d'un constructeur, sélecteur de photos d'un vrai appareil restent à tester.
- Sur émulateur, les images de ticket sont générées (texte net) : elles valident la chaîne complète, pas la
  qualité de lecture ; la vraie application appareil photo de l'émulateur photographie une scène de synthèse.
