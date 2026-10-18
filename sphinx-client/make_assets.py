"""Generates Sphinx Client placeholder/derived assets. Re-run any time; replace outputs with final art if desired."""
import math, random
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageFilter

OUT = "src/main/resources/assets/sphinxclient/textures/gui/"
random.seed(7); np.random.seed(7)

# ---------- LOGO (from supplied white-on-black image: luminance -> alpha) ----------
src = Image.open("/mnt/user-data/uploads/1792299633034_image.png").convert("L")
bbox = src.point(lambda v: 255 if v > 40 else 0).getbbox()
x0, y0, x1, y1 = bbox
side = max(x1 - x0, y1 - y0) + 60
cx, cy = (x0 + x1) // 2, (y0 + y1) // 2
crop = src.crop((cx - side // 2, cy - side // 2, cx + side // 2, cy + side // 2)).resize((256, 256), Image.LANCZOS)
logo = Image.new("RGBA", (256, 256), (255, 255, 255, 0))
logo.putalpha(crop.point(lambda v: min(255, int(v * 1.15))))
logo.save(OUT + "logo.png")
ic = Image.new("RGBA", (128, 128), (0, 0, 0, 255))
ic.alpha_composite(logo.resize((112, 112), Image.LANCZOS), (8, 8))
ic.save("src/main/resources/assets/sphinxclient/icon.png")

# ---------- WORDMARK (1024x256): tracked "SPHINX" + thin "CLIENT" ----------
def tracked(draw, text, font, cx, y, tracking, fill):
    ws = [draw.textlength(c, font=font) for c in text]
    total = sum(ws) + tracking * (len(text) - 1)
    x = cx - total / 2
    for c, w in zip(text, ws):
        draw.text((x, y), c, font=font, fill=fill)
        x += w + tracking
wm = Image.new("RGBA", (1024, 256), (255, 255, 255, 0))
d = ImageDraw.Draw(wm)
big = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 120)
small = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf", 52)
tracked(d, "SPHINX", big, 512, 20, 46, (255, 255, 255, 255))
tracked(d, "CLIENT", small, 512, 175, 70, (200, 200, 205, 255))
wm.save(OUT + "wordmark.png")

# ---------- BACKGROUND: procedural pixel-art night scene (320x180 -> 1280x720) ----------
W, H, HZ = 320, 180, 112
img = np.zeros((H, W, 3), dtype=np.float32)
for y in range(HZ):
    t = y / HZ
    img[y, :] = np.array([4, 6, 13]) * (1 - t) + np.array([20, 28, 48]) * t
# moon glow
MX, MY = 246, 34
yy, xx = np.mgrid[0:HZ, 0:W]
dist = np.sqrt((xx - MX) ** 2 + (yy - MY) ** 2)
glow = np.clip(1 - dist / 62, 0, 1) ** 2.2
img[:HZ] += glow[..., None] * np.array([45, 58, 85])
# stars
for _ in range(110):
    x, y = random.randrange(W), random.randrange(0, 70)
    b = random.randint(110, 230)
    img[y, x] = (b, b, min(255, b + 15))
# blocky clouds
for _ in range(34):
    x, y = random.randrange(-10, W), random.randrange(8, 72)
    for r in range(random.randint(2, 4)):
        xo = random.randint(-3, 3)
        l = random.randint(8, 30)
        lum = 1 + 0.9 * glow[min(HZ - 1, max(0, y))][min(W - 1, max(0, x))]
        c = np.array([30, 38, 58]) * lum
        img[y + r * 2: y + r * 2 + 2, max(0, x + xo): max(0, x + xo + l)] = c
# moon
img[MY - 7:MY + 7, MX - 7:MX + 7] = (232, 238, 250)
for _ in range(14):
    px, py = MX - 6 + random.randrange(11), MY - 6 + random.randrange(11)
    img[py:py + 2, px:px + 2] = (196, 206, 224)
img[MY - 7:MY + 7, MX - 7] = (190, 200, 220); img[MY - 7, MX - 7:MX + 7] = (190, 200, 220)
# distant mountains (stepped)
h = 20; col_h = []
for x in range(0, W, 4):
    h = int(np.clip(h + random.choice([-3, -2, -1, 0, 1, 2, 3]), 8, 40)); col_h.append(h)
for i, hh in enumerate(col_h):
    img[HZ - hh:HZ, i * 4:i * 4 + 4] = (11, 15, 25)
# right cliffs
h = 30
for x in range(236, W, 4):
    h = int(np.clip(h + random.choice([-2, 0, 2, 4]) , 18, 62))
    shade = random.randint(-2, 3)
    img[HZ - h:HZ, x:x + 4] = (14 + shade, 16 + shade, 20 + shade)
# left ruined stepped temple
torches = []
base_w, tier = 110, 0
y = HZ
while base_w > 18 and tier < 11:
    th = 8
    x0 = 8 + (110 - base_w) // 2 - (tier % 2)
    for yy_ in range(y - th, y):
        for xx_ in range(x0, x0 + base_w):
            v = random.randint(-3, 3)
            img[yy_, xx_] = (24 + v, 17 + v, 15 + v)
    if tier in (1, 3, 5, 7):
        torches += [(x0 + 3, y - 5), (x0 + base_w - 4, y - 5)]
    if tier in (2, 4):  # arched doorway glow
        dx = x0 + base_w // 2
        img[y - 6:y, dx - 2:dx + 2] = (150, 90, 35)
        torches.append((dx, y - 4))
    y -= th; base_w -= 10; tier += 1
# left foreground bank
for x in range(0, 70):
    top = HZ - 6 - int(8 * max(0, 1 - x / 70)) - random.randint(0, 2)
    img[top:HZ, x] = (10, 14, 12)
for _ in range(8):
    torches.append((random.randint(6, 66), HZ - random.randint(6, 12)))
for (tx, ty) in torches:
    tx = min(W - 1, max(0, tx)); ty = min(HZ - 1, max(0, ty))
    for oy in range(-9, 10):
        for ox in range(-9, 10):
            px, py = tx + ox, ty + oy
            if 0 <= px < W and 0 <= py < HZ:
                f = max(0, 1 - math.hypot(ox, oy) / 9) ** 2
                img[py, px] += f * np.array([120, 62, 14])
    img[ty:ty + 2, tx] = (255, 190, 90)
# right distant ruin
img[HZ - 34:HZ, 200:206] = (13, 16, 22); img[HZ - 38:HZ - 34, 202:204] = (13, 16, 22)
img = np.clip(img, 0, 255)

# water + reflection
out = img.copy()
for y in range(HZ, H):
    d_ = y - HZ
    sy = HZ - 1 - d_ * 1.0
    if sy < 0: sy = 0
    wob = int(round(math.sin(d_ * 0.9) * (1 + d_ / 25)))
    row = np.roll(img[int(sy)], wob, axis=0)
    fade = max(0.22, 0.68 - d_ / 130)
    base = np.array([5, 8, 16]) * (1 - d_ / 110)
    out[y] = row * fade * (0.75 + 0.25 * ((y % 2) == 0)) + base
    if random.random() < 0.18:
        s = random.randrange(W - 40); out[y, s:s + random.randint(8, 40)] *= 1.25
# moon reflection streak
for y in range(HZ + 2, H):
    if (y // 2) % 2 == 0 or random.random() < 0.3:
        wdt = random.randint(2, 6) + (y - HZ) // 14
        c = MX + random.randint(-3, 3)
        out[y, c - wdt:c + wdt] = np.clip(out[y, c - wdt:c + wdt] * 0.4 + np.array([150, 170, 205]) * 0.7, 0, 255)
# vignette
yy, xx = np.mgrid[0:H, 0:W]
vig = 1 - 0.55 * np.clip(np.sqrt(((xx - W / 2) / (W / 2)) ** 2 + ((yy - H / 2) / (H / 2)) ** 2) - 0.45, 0, 1)
out *= vig[..., None]
bg = Image.fromarray(np.clip(out, 0, 255).astype(np.uint8)).resize((1280, 720), Image.NEAREST)
bg.save(OUT + "background.png")

# ---------- ICONS (48x48, white, 4x supersampled) ----------
S = 192
def new(): 
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0)); return im, ImageDraw.Draw(im)
def save(im, name): im.resize((48, 48), Image.LANCZOS).save(OUT + "icons/" + name + ".png")
WH = (255, 255, 255, 255)

im, d = new(); d.polygon([(50, 28), (50, 164), (160, 96)], fill=WH); save(im, "play")
im, d = new()
d.ellipse([22, 22, 170, 170], outline=WH, width=12)
d.ellipse([62, 22, 130, 170], outline=WH, width=10)
d.line([(22, 96), (170, 96)], fill=WH, width=10); d.line([(36, 58), (156, 58)], fill=WH, width=8); d.line([(36, 134), (156, 134)], fill=WH, width=8)
save(im, "globe")
im, d = new()
d.polygon([(70, 28), (30, 52), (14, 92), (44, 106), (54, 90), (54, 168), (138, 168), (138, 90), (148, 106), (178, 92), (162, 52), (122, 28), (108, 44), (84, 44)], fill=WH)
d.polygon([(84, 44), (108, 44), (96, 60)], fill=(0, 0, 0, 0))
d.rectangle([80, 98, 112, 126], fill=(0, 0, 0, 0)); d.rectangle([84, 102, 108, 122], outline=WH, width=5)
save(im, "shirt")
im, d = new()
cx_, cy_ = 96, 96
pts = []
for i in range(8):
    a = i * math.pi / 4
    for da, r in [(-0.2, 62), (-0.14, 86), (0.14, 86), (0.2, 62)]:
        pts.append((cx_ + r * math.cos(a + da), cy_ + r * math.sin(a + da)))
d.polygon(pts, fill=WH); d.ellipse([cx_ - 56, cy_ - 56, cx_ + 56, cy_ + 56], fill=WH)
d.ellipse([cx_ - 26, cy_ - 26, cx_ + 26, cy_ + 26], fill=(0, 0, 0, 0))
save(im, "gear")
im, d = new()
d.rounded_rectangle([14, 52, 178, 158], radius=10, outline=WH, width=12)
d.polygon([(14, 62), (14, 38), (36, 30), (76, 30), (92, 52), (14, 52)], fill=WH)
save(im, "folder")
im, d = new()
d.ellipse([60, 22, 132, 94], fill=WH); d.pieslice([26, 100, 166, 240], 180, 360, fill=WH); d.rectangle([26, 170, 166, 176], fill=(0, 0, 0, 0))
save(im, "user")
im, d = new()
d.polygon([(40, 14), (116, 14), (152, 50), (152, 178), (40, 178)], outline=WH, width=11)
d.polygon([(116, 14), (116, 50), (152, 50)], fill=WH)
for yv in (86, 114, 142): d.line([(66, yv), (126, yv)], fill=WH, width=9)
save(im, "document")
im, d = new()
d.line([(86, 30), (30, 30), (30, 162), (86, 162)], fill=WH, width=12)
d.line([(70, 96), (160, 96)], fill=WH, width=12); d.polygon([(168, 96), (130, 62), (130, 130)], fill=WH)
save(im, "exit")
print("done")
