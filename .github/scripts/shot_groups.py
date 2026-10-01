"""Liste les captures d'un dossier en groupes de 4 (JSON), pour les jobs d'aperçu à la demande.
Un job d'aperçu publie au plus 4 captures : 4 × 10 annotations + avertissements < 50 par job (limite GitHub).
Les captures d'échec (zz_echec*) sont déjà publiées par le job de test lui-même et ne sont pas reprises.
Usage : shot_groups.py <dossier> [taille]"""
import glob, json, os, sys

folder = sys.argv[1]
size = int(sys.argv[2]) if len(sys.argv) > 2 else 4
names = sorted(os.path.basename(p)[:-4] for p in glob.glob(os.path.join(folder, "*.png")))
names = [n for n in names if "zz_echec" not in n]
print(json.dumps([names[i:i + size] for i in range(0, len(names), size)], separators=(",", ":")))
