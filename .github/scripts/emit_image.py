"""Publie une capture en annotations GitHub (JPEG base64 découpé), lisible via l'API checks
sans télécharger d'artefact. Usage : emit_image.py <png>"""
import base64, io, sys
from PIL import Image

path = sys.argv[1]
name = path.rsplit("/", 1)[-1].rsplit(".", 1)[0]
img = Image.open(path).convert("RGB")
buf = io.BytesIO()
img.save(buf, "JPEG", quality=72, optimize=True)
data = base64.b64encode(buf.getvalue()).decode()
size = 30000
parts = [data[i:i + size] for i in range(0, len(data), size)]
if len(parts) > 9:
    img = img.resize((img.width * 2 // 3, img.height * 2 // 3))
    buf = io.BytesIO(); img.save(buf, "JPEG", quality=65, optimize=True)
    data = base64.b64encode(buf.getvalue()).decode()
    parts = [data[i:i + size] for i in range(0, len(data), size)]
for i, p in enumerate(parts[:9], 1):
    print(f"::notice title=shot {name} {i}/{len(parts)}::{p}")
