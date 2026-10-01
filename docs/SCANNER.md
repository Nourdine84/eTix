# Scanner de tickets (lot 9)

Référence iOS : `ScannerFlowView`, `CameraPrimingView`, `OCRProcessingView`, `TextRecognizer`, `ReceiptParser`,
`StoreCategoryMapper`, `AddTicketViewModel.handleOCRResult`, `TicketForm` (badges de confiance).

## Parcours Android

| Étape | Android | iOS |
|---|---|---|
| Entrée | « Scanner un ticket » sur l'Accueil (ouvre Ajouter + scanner) ou sur Ajouter | Identique |
| Intro | Cadre illustré, « Prendre une photo », « Choisir une image », « Annuler » | « Scanner », « Annuler » (pas de galerie) |
| Autorisation | Écran d'explication avant la demande système ; refus → retour intro + fenêtre « Accès caméra requis » (Paramètres / Annuler) ; refus définitif → fenêtre directement | Identique (Réglages) |
| Capture | Application appareil photo du système (photo enregistrée dans le cache de l'app, supprimée après lecture) | VNDocumentCameraViewController (recadrage automatique) |
| Image choisie | Sélecteur système, image jamais modifiée ni copiée | Absent |
| Lecture | ML Kit Text Recognition (modèle latin embarqué, sur l'appareil, aucun envoi) ; image réduite à 2048 px et redressée (EXIF) ; étapes « Analyse de l'image / Reconnaissance du texte / Extraction des informations » suivant l'avancement réel | Vision, étapes indicatives minutées |
| Rien détecté | « Aucune information détectée », « Réessayer », « Saisir manuellement » | Identique |
| Erreur | « Lecture impossible » (image illisible / reconnaissance en échec) ou « Appareil photo indisponible », mêmes boutons | Absent (retour intro) |
| Résultat | Formulaire « Ajouter » prérempli : magasin, montant, date remplacés s'ils sont lus ; catégorie seulement si aucune n'est choisie | Identique |
| Badges | Magasin « À vérifier » (confiance moyenne), montant « Vérifié » sur une ligne de total sinon « À vérifier », date « Vérifié » ; catégorie « Suggéré par l'OCR » | Identique |
| Catégorie | Historique du magasin (catégorie la plus fréquente, ≥ 3 tickets = sans badge), sinon dictionnaire iOS, sinon mots d'activité de l'en-tête ; « Autre » = sans catégorie | Historique, puis dictionnaire |
| Validation | **Seul « Enregistrer » crée le ticket** ; bandeau « Informations lues sur le ticket. Vérifie-les avant d'enregistrer. » + « Annuler le scan » (vide le formulaire) | « Enregistrer » ; pas de bandeau |
| Retour système | Ferme le scanner depuis n'importe quelle étape (lecture annulée), aucun ticket | Pas de retour (modale) |

Aucun ticket existant n'est lu pour être modifié : l'historique sert uniquement à proposer une catégorie.

## Tests

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

- L'autorisation caméra refusée puis rétablie, la vraie application appareil photo et les vraies photos (flou,
  pli, lumière) ne sont testées que sur téléphone physique (à faire).
- Sur émulateur, l'image est générée (texte net) : elle valide la chaîne complète, pas la qualité de lecture.
