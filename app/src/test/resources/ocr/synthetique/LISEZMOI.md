# Tickets OCR SYNTHÉTIQUES

Textes **rédigés à la main** (29/09/2026) pour compléter les tests : ils imitent la sortie texte d'un OCR
mais ne proviennent d'aucun ticket réel ni d'aucune photo. Ils **ne remplacent pas** une validation avec de vrais
tickets (bruit OCR, colonnes décalées, caractères mal reconnus absents ici).

| Fichier | Imite | Ce qu'il teste |
|---|---|---|
| `restaurant_synthetique.txt` | Addition de restaurant (plat, café, TVA 10 %) | Enseigne en 1re ligne, total TTC, mot-clé « cafe » |
| `ticket_long_synthetique.txt` | Ticket de supermarché long (15 articles, sous-total, remise, TVA, HT) | Choix du TOTAL TTC face à SOUS-TOTAL / TOTAL HT / TVA (cas D1 iOS), date + heure |
