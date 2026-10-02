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
| Diagnostic | « Journaux de plantage : conservés sur ce téléphone, jamais envoyés. », afficher / supprimer | absent | Section Android conservée, en fin d'écran |

Présentation : en-tête avec retour et cartes groupées, comme « Budgets mensuels » (lot 7). Une seule mise en page
pour les deux thèmes : l'ancienne variante `layout-night-v8/fragment_settings.xml` (copie sombre avec d'autres
identifiants, `btnDeleteDatabase`) a été retirée ; la nouvelle mise en page utilise les couleurs `v2_*` déclinées
en sombre. Les fonds sombres des autres écrans ne sont pas modifiés (choix visuel ouvert).

## Comportements

- **Thème** : stocké comme avant (`etix_session` / `theme_mode`, valeurs AppCompat). Aucune préférence → Système.
  Un choix existant de l'ancien bouton (clair ou sombre) est conservé et affiché tel quel ; toute autre valeur
  s'affiche « Système ». Appliqué immédiatement (écran recréé) et au démarrage (Splash, Login, Main inchangés).
- **Période par défaut** : `etix_settings` / `default_range` (absente → Ce mois, comme iOS). Appliquée à la création
  de l'Accueil, des Catégories et des Magasins ; quand le réglage change, les trois écrans déjà ouverts passent à
  la nouvelle période (l'Accueil reste actif sous les Réglages). Un choix fait sur un écran est conservé en changeant
  d'onglet et lors d'une recréation (rotation, thème). L'Historique n'utilise pas ce réglage : ses filtres
  (recherche, dates) ne changent pas.
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
  - Avant le lot 10, l'export de l'Historique écrivait seulement un fichier dans le cache privé (inaccessible).

## Points ouverts (décisions de Nourdine)

1. **Format CSV** : virgule + point décimal (iOS). Excel en français ouvre ce fichier sur une seule colonne et lit
   `1234.56` comme du texte ; l'alternative « Excel FR » (point-virgule, virgule décimale, BOM UTF-8) s'écarterait
   d'iOS. Non tranché : format iOS conservé.
2. **Champs commençant par `=`, `+`, `-`, `@`** : non neutralisés (comme iOS). Un tableur peut les interpréter comme
   formules (injection CSV) ; risque faible (données saisies par l'utilisateur lui-même), à décider.
3. Thème et période : liste à choix unique (Android) au lieu du menu déroulant iOS — à valider visuellement.
4. Période : appliquée immédiatement aux écrans déjà ouverts quand le réglage change (interprétation de
   « appliquée à l'ouverture » ; iOS non vérifié sur ce point).
5. Journaux de plantage : `ETixApp` n'est toujours pas déclarée dans le manifeste (point ouvert n° 6 du suivi), donc
   aucun journal n'est écrit aujourd'hui ; la section reste affichée telle quelle.
6. iOS : `SettingsView` enregistre le thème sous la clé `appearance` alors que l'app lit `app.appearance` (choix du
   thème probablement sans effet sur iOS) — écart signalé, non reproduit.

## Tests

- JVM : `CsvExporterTest` (en-tête, accents, virgule, guillemets, retours à la ligne, centimes, fuseau, langue).
- Robolectric : `AppPreferencesTest` (défauts, choix existant conservé, valeurs illisibles, période initiale,
  écoute) ; `Lot10ScreenshotTest` (aperçus clair / sombre / 320 dp police 2,0, listes de choix, export Réglages et
  Historique avec fichier relu, période sur les trois écrans, recréation, tickets et budgets inchangés).
- Émulateur (`E2eReglagesTest`, mode standard API 21 / 34 / 36) : thème trois états réellement appliqué, compteur,
  version, build, suppression désactivée ; période sur Accueil / Catégories / Magasins sans réinitialisation ni
  effet sur l'Historique ; exports relus par l'URI partagée (contenu exact, données délicates) ; vraie feuille de
  partage annulée ; tickets et budgets inchangés. Persistance après arrêt complet de l'app
  (`E2eReglagesAvantRedemarrageTest` → `am force-stop` → `E2eReglagesApresRedemarrageTest`).
- Petit écran (`k09_reglages`, passes a à d, sombre en passe d) ; mise à jour depuis le lot 8 **et** le lot 9
  fusionnés (`emulator-api34-maj-lot8`, `emulator-api34-maj-lot9`) avec thème enregistré comparé et affiché.
- Non fait : téléphone physique, vraie application destinataire (messagerie, Drive, tableur) ouvrant le fichier.
