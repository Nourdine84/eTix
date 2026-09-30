"""Publie une capture en annotations GitHub (JPEG base64 découpé en morceaux < 4096 car.,
limite de l'API). Lisible via l'API checks sans télécharger d'artefact.
Contraintes GitHub : 10 notices max par step, 50 annotations max par job → 1 image par job.
Usage : emit_image.py <png>"""
import base64, io, sys
from PIL import Image

MAX_PARTS, SIZE = 10, 3900
path = sys.argv[1]
name = path.rsplit("/", 1)[-1].rsplit(".", 1)[0]
src = Image.open(path).convert("RGB")

for width, quality in [(480, 70), (420, 62), (400, 55), (360, 50), (320, 45)]:
    img = src.resize((width, round(src.height * width / src.width)), Image.LANCZOS)
    buf = io.BytesIO()
    img.save(buf, "JPEG", quality=quality, optimize=True, progressive=True)
    data = base64.b64encode(buf.getvalue()).decode()
    parts = [data[i:i + SIZE] for i in range(0, len(data), SIZE)]
    if len(parts) <= MAX_PARTS:
        break

for i, p in enumerate(parts[:MAX_PARTS], 1):
    print(f"::notice title=shot {name} {i}/{len(parts)}::{p}")
