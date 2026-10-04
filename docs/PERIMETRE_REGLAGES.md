# Lot 10 — Réglages : périmètre (validé par Nourdine le 02/10/2026)

> **Réalisé et fusionné** dans `feature/android-v2` (PR #80, commit de fusion `f102cfe`, 02/10/2026). Ce document
> garde le périmètre tel que validé. Implémentation, écarts par rapport à ce périmètre (liste à choix au lieu de
> boutons, période sans effet sur les écrans déjà ouverts, protection du CSV contre les formules, journaux signalés
> indisponibles), tests et procédure d'import du CSV : `docs/REGLAGES.md`. Aucun écran n'est validé visuellement.

Prochain lot retenu par Nourdine : Réglages. Périmètre validé le 02/10/2026 avec les décisions ci-dessous
(« Décisions de Nourdine »), qui priment sur la proposition. Références : iOS `feature/home-hero-v2` (`Views/Main/SettingsView.swift`,
`Settings/AppSettings.swift`, `Settings/SettingsViewModel.swift`, `AppAppearance.swift`, `eTixApp.swift`) ;
Android `feature/android-v2` `86d9a19` (`fragments/SettingsFragment.kt`, `res/layout/fragment_settings.xml`,
`utils/CsvExporter.kt`, `ui/history/TicketHistoryFragmentV2.kt`).

## Ce qui existe réellement côté iOS

| Section iOS | Contenu | Constat dans le code iOS |
|---|---|---|
| Apparence | « Thème » : Système / Clair / Sombre (défaut Système) | Le choix est enregistré sous la clé `appearance`, mais la racine de l'app lit `app.appearance` : **d'après le code, le choix n'est pas appliqué** (à confirmer sur un iPhone). |
| Affichage | « Période par défaut » : Aujourd'hui / Ce mois / Cette année (défaut Ce mois) | Lue à l'ouverture de l'Accueil, des Catégories et des Magasins (pas de l'Historique). |
| Données | « Exporter en CSV » : tous les tickets, désactivé s'il n'y en a aucun, feuille de partage du système | Colonnes `Date,Magasin,Montant (€),Catégorie,Description`, date courte française, montant à 2 décimales, guillemets échappés. |
| Données | « Supprimer tous les tickets » avec confirmation | **Reste désactivé sur Android** (règle validée). |
| Données (pied) | « N ticket(s) enregistré(s) » | — |
| Informations | Version, Build | — |
| Accès | 6ᵉ onglet « Réglages » | Android : 5 onglets au plus ; Réglages ouverts par l'engrenage de l'Accueil (écart conservé). |

## Ce qui existe côté Android

- Écran V1 « Paramètres » : « Connecté en tant que … », version, « Changer de thème » (bascule clair ↔ sombre ;
  impossible de revenir à « Système » une fois changé), « Vider tous les tickets — indisponible » (désactivé),
  « Afficher crash log », « Supprimer logs de crash », « Se déconnecter ».
- Thème stocké dans les préférences de session (`theme_mode`, défaut : système). Période initiale des écrans :
  constante `TimeRange.DEFAULT = MONTH` (Accueil, Catégories, Magasins, Détail catégorie).
- **Export CSV existant (Historique, bouton « Exporter CSV »)** : exporte la liste **filtrée** de l'Historique dans
  le cache privé de l'app et affiche seulement le nom du fichier. **Le fichier n'est pas récupérable par
  l'utilisateur** (aucun partage) : l'export ne remplit pas sa fonction aujourd'hui. Échappement incomplet
  (guillemets internes non doublés), montant non formaté, date `aaaa-mm-jj`.

## Périmètre proposé

1. **Apparence** : choix Système / Clair / Sombre (défaut Système), appliqué immédiatement et conservé
   (réutilise `theme_mode`, aucune nouvelle donnée). Corrige l'impossibilité actuelle de revenir à « Système ».
   Contrairement au code iOS lu, le choix sera réellement appliqué.
2. **Période par défaut** : Aujourd'hui / Ce mois / Cette année (défaut Ce mois), appliquée à l'ouverture de
   l'Accueil, des Catégories et des Magasins, comme iOS. Préférence locale (SharedPreferences), pas de Room.
3. **Export CSV dans Réglages** : tous les tickets, désactivé s'il n'y en a aucun, feuille de partage Android
   (`ACTION_SEND` via le FileProvider existant, aucune permission). Format aligné sur iOS (colonnes, date courte
   française, montant à 2 décimales, échappement correct).
4. **Accès existant à l'export dans l'Historique : conservé, non déplacé.** Proposition à valider : le brancher sur
   le même partage (il exporterait alors la liste filtrée affichée, ce qui le distingue de l'export complet des
   Réglages). Sans accord, il reste tel quel et l'écart est documenté.
5. **Données** : compteur « N ticket(s) enregistré(s) » ; « Supprimer tous les tickets » visible mais **désactivé**
   (règle inchangée).
6. **Informations** : version et build.
7. **Présentation** : liste par sections comme iOS (Apparence, Affichage, Données, Informations). Les éléments
   Android sans équivalent iOS ne sont pas retirés sans accord : « Connecté en tant que » / « Se déconnecter »
   (code de connexion conservé) et journaux de plantage, regroupés en fin d'écran (sections « Compte » et
   « Diagnostic »), à confirmer.

Hors périmètre : synchronisation, compte, permission réseau, suppression globale, widget, nouvelles données Room.

## Vérifications prévues

- JVM / Robolectric : trois états du thème (persistés, appliqués, retour à Système) ; période par défaut appliquée
  aux trois écrans ; contenu CSV (échappement, colonnes, accents) ; intention de partage ; export désactivé sans
  ticket ; suppression globale toujours désactivée ; aperçus clair / sombre, 320 dp police 2,0.
- Émulateur : changement de thème réel, feuille de partage du système ouverte avec le fichier, fr-FR, petit écran,
  préférences conservées lors d'une mise à jour sans désinstallation.
- Aperçus (label `apercus`) examinés avant toute demande de revue.

## Décisions de Nourdine (02/10/2026)

1. Thème Système / Clair / Sombre ; Système par défaut **en l'absence de préférence existante** ; le choix déjà
   enregistré d'un utilisateur est conservé ; persistance vérifiée après redémarrage.
2. Période par défaut appliquée à l'ouverture de l'Accueil, des Catégories et des Magasins ; filtres de
   l'Historique inchangés ; un choix fait sur un écran n'est pas réinitialisé lors des allers-retours entre écrans.
3. Export CSV : Réglages = tous les tickets ; Historique = uniquement les tickets correspondant aux filtres actifs,
   libellé clair ; les deux par le partage Android, sans permission ni réseau ; contenu du fichier transmis vérifié
   (accents, décimales, séparateurs, guillemets, retours à la ligne, dates) ; annuler le partage ne modifie aucune
   donnée.
4. Connexion et journaux de plantage conservés en fin d'écran ; la connexion factice n'est pas présentée comme une
   protection ; aucun journal transmis automatiquement.
5. Accès par l'engrenage de l'Accueil, sans sixième onglet.
6. Suppression globale désactivée ; compteur de tickets, version et build affichés.
7. Fonds sombres non uniformisés dans ce lot (choix visuel ouvert). Aucun changement de schéma Room.

## Questions (posées avant accord, désormais tranchées ci-dessus)

1. Thème à trois états, défaut Système : d'accord ?
2. Période par défaut appliquée à l'Accueil, aux Catégories et aux Magasins (pas à l'Historique), comme iOS ?
3. Export de l'Historique : le brancher sur le partage (liste filtrée) ou le laisser tel quel ?
4. « Connecté en tant que / Se déconnecter » et journaux de plantage : conservés en fin d'écran ?
5. Accès par l'engrenage de l'Accueil (pas de 6ᵉ onglet) : écart iOS conservé ?
