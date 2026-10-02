"""Génère les images de test du scanner (app/src/androidTest/assets/scan) — données fictives.
- exif_orientation_6.jpg : ticket stocké tourné de 90°, orientation EXIF 6 (l'app doit le redresser) ;
- grande_6000x8000.jpg : 48 Mpx (réduction mémoire) ;
- ticket_long_1000x7000.jpg : ticket long (largeur à préserver).
Usage : python3 .github/scripts/generer_images_scan.py (Pillow, police DejaVu Sans Bold)."""
import os
from PIL import Image, ImageDraw, ImageFont

F = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
OUT = os.path.join(os.path.dirname(__file__), "../../app/src/androidTest/assets/scan")
LINES = ["ESSO", "12/01/2026", "TOTAL TTC 23,45 EUR", "CB VISA"]


def ticket(w, h, size, lines, x, y0, step):
    im = Image.new("RGB", (w, h), "white")
    d = ImageDraw.Draw(im)
    f = ImageFont.truetype(F, size)
    for i, l in enumerate(lines):
        d.text((x, y0 + i * step), l, fill="black", font=f)
        assert x + d.textlength(l, font=f) < w - 20, (l, w)
    return im


os.makedirs(OUT, exist_ok=True)
ex = Image.Exif()
ex[0x0112] = 6
ticket(1080, 1500, 72, LINES, 80, 180, 160).rotate(90, expand=True) \
    .save(f"{OUT}/exif_orientation_6.jpg", quality=85, exif=ex.tobytes())
ticket(6000, 8000, 360, LINES, 400, 1000, 800).save(f"{OUT}/grande_6000x8000.jpg", quality=80)
long_lines = ["ESSO", "12/01/2026"] + [f"ARTICLE {i:02d}        1,00" for i in range(1, 31)] + ["TOTAL TTC 23,45 EUR", "CB VISA"]
ticket(1000, 7000, 56, long_lines, 60, 150, 190).save(f"{OUT}/ticket_long_1000x7000.jpg", quality=85)
