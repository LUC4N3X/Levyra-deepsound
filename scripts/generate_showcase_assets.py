import os
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageEnhance

SCREENSHOT_DIR = os.environ.get(
    "LEVYRA_SCREENSHOT_DIR",
    r"C:\Users\Luca Drogo\Desktop\screenshots" if os.path.exists(r"C:\Users\Luca Drogo\Desktop\screenshots")
    else os.path.join("docs", "screenshots")
)
OUT_SHOWCASE_DIR = r"docs\assets\showcase"
OUT_CARDS_DIR = os.path.join(OUT_SHOWCASE_DIR, "cards")

def resolve_screenshot_path(filename):
    if not filename:
        return None
    if os.path.isabs(filename) and os.path.exists(filename):
        return filename
    candidates = [
        os.path.join(r"C:\Users\Luca Drogo\Downloads", filename),
        os.path.join(r"C:\Users\Luca Drogo\Desktop\screenshots", filename),
        os.path.join(SCREENSHOT_DIR, filename),
        os.path.join(r"C:\Users\Luca Drogo\Pictures\levyra\screenshots", filename),
        os.path.join(r"C:\Users\Luca Drogo\Downloads\Nuova cartella", filename),
        os.path.join("docs", "screenshots", filename),
    ]
    for cand in candidates:
        if os.path.exists(cand):
            return cand
    return None

os.makedirs(OUT_SHOWCASE_DIR, exist_ok=True)
os.makedirs(OUT_CARDS_DIR, exist_ok=True)

def get_font(size, bold=False):
    candidates = [
        r"C:\Windows\Fonts\Rubik-Bold.ttf" if bold else r"C:\Windows\Fonts\Rubik-Regular.ttf",
        r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf",
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    ]
    for font_path in candidates:
        if os.path.exists(font_path):
            try:
                return ImageFont.truetype(font_path, size)
            except OSError:
                continue
    return ImageFont.load_default()

def enhance_screenshot(img):
    img = img.convert("RGB")
    enhancer = ImageEnhance.Contrast(img)
    img = enhancer.enhance(1.05)
    enhancer = ImageEnhance.Color(img)
    img = enhancer.enhance(1.06)
    enhancer = ImageEnhance.Sharpness(img)
    img = enhancer.enhance(1.10)
    return img

def create_s26_ultra_mockup(screen_img, target_w=670):
    screen_img = enhance_screenshot(screen_img)
    orig_w, orig_h = screen_img.size
    aspect = orig_h / float(orig_w)
    target_h = int(target_w * aspect)
    screen_res = screen_img.resize((target_w, target_h), Image.Resampling.LANCZOS)

    bezel = 9
    corner_r = 26
    inner_r = max(6, corner_r - bezel)
    body_w = target_w + bezel * 2
    body_h = target_h + bezel * 2

    phone = Image.new("RGBA", (body_w, body_h), (0, 0, 0, 0))
    pdraw = ImageDraw.Draw(phone)

    # Samsung Galaxy S26 Ultra Titanium frame
    pdraw.rounded_rectangle((0, 0, body_w - 1, body_h - 1), radius=corner_r, fill=(28, 29, 34, 255))
    pdraw.rounded_rectangle((1, 1, body_w - 2, body_h - 2), radius=corner_r - 1, fill=(14, 15, 18, 255))

    # Mask screen
    screen_mask = Image.new("L", (target_w, target_h), 0)
    smdraw = ImageDraw.Draw(screen_mask)
    smdraw.rounded_rectangle((0, 0, target_w - 1, target_h - 1), radius=inner_r, fill=255)
    phone.paste(screen_res, (bezel, bezel), screen_mask)

    # Inner bezel border
    pdraw.rounded_rectangle((bezel, bezel, bezel + target_w - 1, bezel + target_h - 1), radius=inner_r, outline=(8, 8, 10, 230), width=1)

    # Infinity-O Punch-hole Camera
    cam_diam = 16
    cam_x = body_w // 2 - cam_diam // 2
    cam_y = bezel + 12
    pdraw.ellipse((cam_x, cam_y, cam_x + cam_diam, cam_y + cam_diam), fill=(6, 6, 8, 255))
    pdraw.ellipse((cam_x + 3, cam_y + 3, cam_x + cam_diam - 3, cam_y + cam_diam - 3), fill=(14, 24, 46, 255))
    pdraw.ellipse((cam_x + 6, cam_y + 5, cam_x + 9, cam_y + 8), fill=(130, 175, 235, 180))

    # Micro speaker slit
    pdraw.rounded_rectangle((body_w // 2 - 22, 2, body_w // 2 + 22, 5), radius=1, fill=(40, 42, 48, 255))

    # Diamond-cut outer chamfer
    pdraw.rounded_rectangle((0, 0, body_w - 1, body_h - 1), radius=corner_r, outline=(145, 150, 162, 120), width=1)

    # Side buttons: power & volume on right side
    btn_x = body_w - 1
    btn_col = (45, 47, 52, 255)
    pdraw.line([(btn_x, 260), (btn_x, 340)], fill=btn_col, width=3)
    pdraw.line([(btn_x, 380), (btn_x, 500)], fill=btn_col, width=3)

    return phone

def create_m3_shadow(phone, blur=48, offset_y=28):
    w, h = phone.size
    pad = 90
    cw, ch = w + pad * 2, h + pad * 2 + offset_y
    alpha = phone.split()[-1]
    s = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    s_alpha = Image.new("L", (cw, ch), 0)
    s_alpha.paste(alpha, (pad, pad + offset_y))
    s_alpha = s_alpha.filter(ImageFilter.GaussianBlur(blur))
    s_alpha = ImageEnhance.Brightness(s_alpha).enhance(0.32)
    s.paste((20, 22, 30, 255), (0, 0), s_alpha)
    return s, pad

def draw_vector_sparkle(draw, center_x, center_y, radius, fill_color):
    points = []
    inner_radius = radius * 0.28
    for i in range(8):
        angle = i * (math.pi / 4.0)
        r = radius if (i % 2 == 0) else inner_radius
        x = center_x + r * math.cos(angle - math.pi / 2.0)
        y = center_y + r * math.sin(angle - math.pi / 2.0)
        points.append((x, y))
    draw.polygon(points, fill=fill_color)

def draw_clean_m3_kicker(draw, x, y, text, color, font_size=27):
    font = get_font(font_size, bold=True)
    sparkle_size = int(font_size * 0.95)
    sparkle_gap = 12

    bbox = font.getbbox(text)
    th = bbox[3] - bbox[1]

    sparkle_cx = x + sparkle_size // 2
    sparkle_cy = y + th // 2
    draw_vector_sparkle(draw, sparkle_cx, sparkle_cy, sparkle_size // 2, color)

    tx = x + sparkle_size + sparkle_gap
    ty = y - bbox[1]
    draw.text((tx, ty), text, font=font, fill=color)

# Unified Material 3 Expressive Tonal Background (Subtle, identical across all cards, not light-blue)
TOP_TINT = np.array([248, 245, 253], dtype=np.float32)  # M3 Expressive Iris/Lavender surface
BOT_TINT = np.array([238, 230, 248], dtype=np.float32)  # M3 Expressive Iris/Lavender tonal base
AMBILIGHT_BLOOM = (195, 165, 245)                        # Soft Iris volumetric bloom

def render_m3_duo_cut_cards():
    card_w, card_h = 900, 1600
    gutter = 48
    duo_w = card_w * 2 + gutter

    # 1. Unified Material 3 Expressive Canvas across Cards 01 & 02
    arr = np.zeros((card_h, duo_w, 4), dtype=np.uint8)
    for y in range(card_h):
        r = y / float(card_h)
        col = TOP_TINT * (1 - r) + BOT_TINT * r
        arr[y, :, :3] = col.astype(np.uint8)
    arr[:, :, 3] = 255
    bg = Image.fromarray(arr, mode="RGBA")

    # Volumetric ambilight blooms
    glow_layer = Image.new("RGBA", (duo_w, card_h), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow_layer)
    gdraw.ellipse((140, 80, 880, 940), fill=(*AMBILIGHT_BLOOM, 120))
    gdraw.ellipse((duo_w - 860, 380, duo_w - 90, 1220), fill=(*AMBILIGHT_BLOOM, 120))
    glow_layer = glow_layer.filter(ImageFilter.GaussianBlur(110))
    bg = Image.alpha_composite(bg, glow_layer)

    s_home = resolve_screenshot_path("Screenshot_20260926_171253_LEVYRA.jpg")
    s_player = resolve_screenshot_path("Screenshot_20260926_193948_LEVYRA.jpg")

    angle = 15.0
    c2_left = card_w + gutter

    # 1. Phone 1 (Home) cutting 15° across into Card 02
    if s_home and os.path.exists(s_home):
        with Image.open(s_home) as src1:
            p1 = create_s26_ultra_mockup(src1, target_w=640)
        rot1 = p1.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
        s1_shadow, pad1 = create_m3_shadow(rot1, blur=50, offset_y=30)
        p1_x = 280
        p1_y = 40
        bg.paste(s1_shadow, (p1_x - pad1, p1_y - pad1), s1_shadow)
        bg.paste(rot1, (p1_x, p1_y), rot1)

    # 2. Phone 2 (Now Playing) in foreground
    if s_player and os.path.exists(s_player):
        with Image.open(s_player) as src2:
            p2 = create_s26_ultra_mockup(src2, target_w=610)
        rot2 = p2.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
        s2_shadow, pad2 = create_m3_shadow(rot2, blur=46, offset_y=28)
        p2_x = c2_left + 175
        p2_y = 310
        bg.paste(s2_shadow, (p2_x - pad2, p2_y - pad2), s2_shadow)
        bg.paste(rot2, (p2_x, p2_y), rot2)

    # Slice into Card 01 and Card 02
    c1 = bg.crop((0, 0, card_w, card_h))
    c2 = bg.crop((c2_left, 0, c2_left + card_w, card_h))

    # Clean M3 typography - borderless surface
    d1 = ImageDraw.Draw(c1)
    tx1, ty1 = 72, 1260
    draw_clean_m3_kicker(d1, tx1, ty1, "ZERO ADS • NATIVE PLAY", color=(95, 60, 165, 255), font_size=27)
    d1.text((tx1, ty1 + 44), "Pure listening.", font=get_font(64, bold=True), fill=(29, 27, 32, 255))
    d1.multiline_text(
        (tx1, ty1 + 124),
        "Zero ads, zero accounts.\nStream and store locally on your device.",
        font=get_font(30),
        fill=(70, 65, 80, 255),
        spacing=6
    )

    d2 = ImageDraw.Draw(c2)
    tx2, ty2 = 72, 54
    draw_clean_m3_kicker(d2, tx2, ty2, "OFFLINE STORAGE", color=(130, 60, 180, 255), font_size=27)
    d2.text((tx2, ty2 + 44), "Download & keep.", font=get_font(64, bold=True), fill=(29, 27, 32, 255))
    d2.multiline_text(
        (tx2, ty2 + 124),
        "Clean M4A files in device storage.\nFull metadata, artwork, and lyrics.",
        font=get_font(30),
        fill=(75, 65, 80, 255),
        spacing=6
    )

    return c1, c2

def render_m3_single_card(screenshot_name, kicker, title, subtitle, kicker_color, target_w=670):
    card_w, card_h = 900, 1600

    # 1. Unified Tonal Surface Gradient (Always the same across all cards)
    arr = np.zeros((card_h, card_w, 4), dtype=np.uint8)
    for y in range(card_h):
        r = y / float(card_h)
        col = TOP_TINT * (1 - r) + BOT_TINT * r
        arr[y, :, :3] = col.astype(np.uint8)
    arr[:, :, 3] = 255
    card = Image.fromarray(arr, mode="RGBA")

    # 2. Volumetric ambilight glow behind phone
    glow = Image.new("RGBA", (card_w, card_h), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse((80, 280, 820, 1380), fill=(*AMBILIGHT_BLOOM, 110))
    glow = glow.filter(ImageFilter.GaussianBlur(100))
    card = Image.alpha_composite(card, glow)

    # 3. Clean M3 Typography - left aligned, directly on surface
    draw = ImageDraw.Draw(card)
    tx, ty = 72, 54
    draw_clean_m3_kicker(draw, tx, ty, kicker, color=(*kicker_color, 255), font_size=27)
    draw.text((tx, ty + 44), title, font=get_font(64, bold=True), fill=(29, 27, 32, 255))
    draw.multiline_text(
        (tx, ty + 124),
        subtitle,
        font=get_font(30),
        fill=(75, 65, 75, 255),
        spacing=6
    )

    # 4. Samsung Galaxy S26 Ultra mockup (Prominent hero size: 670px width)
    s_path = resolve_screenshot_path(screenshot_name)
    if s_path and os.path.exists(s_path):
        with Image.open(s_path) as src:
            phone = create_s26_ultra_mockup(src, target_w=target_w)
        shadow, pad = create_m3_shadow(phone, blur=48, offset_y=28)
        px = (card_w - phone.width) // 2
        py = 270
        card.paste(shadow, (px - pad, py - pad), shadow)
        card.paste(phone, (px, py), phone)

    return card

# Unified Material 3 Expressive specifications (All cards share identical subtle Iris background)
M3_SHOWCASE_SPECS = [
    # Card 03: Synced Lyrics
    ("screen-lyrics.jpg", "SYNCED LYRICS", "Sing along.",
     "Real-time synchronized lyrics, Romanized lines,\nand effortless translation.",
     (160, 45, 140), ["03_now_playing.webp"]),

    # Card 04: Player Decks
    ("Screenshot_20261010_190538_LEVYRA.jpg", "PLAYER DECKS", "Your listening deck.",
     "Fluid bottom sheets, gesture controls,\nand adaptive cover-art styling.",
     (110, 50, 185), ["04_player_deck.webp"]),

    # Card 05: Explore & Fresh Currents
    ("Screenshot_20261010_132210_LEVYRA.jpg", "DYNAMIC DISCOVERY", "Fresh currents.",
     "Handpicked artist spotlights, community mixes,\nand global trending charts.",
     (120, 50, 180), ["05_explore_stage.webp", "05_explore.webp"]),

    # Card 06: Levyra Mix
    ("Screenshot_20261010_132217_LEVYRA.jpg", "LEVYRA MIX", "Built for your taste.",
     "Algorithmic discovery stations powered by\nyour local listening history.",
     (115, 45, 195), ["06_levyra_mix_stage.webp", "06_levyra_mix.webp"]),

    # Card 07: Genres & Moods
    ("Screenshot_20261010_132249_LEVYRA.jpg", "EXPLORE MOODS", "Every genre.",
     "Dive into curated categories, moods,\nand regional musical traditions.",
     (140, 60, 175), ["07_genres.webp"]),

    # Card 08: Audio Architecture & Equalizer
    ("Screenshot_20261010_190726_LEVYRA.jpg", "AUDIO ARCHITECTURE", "Acoustic precision.",
     "10-band parametric equalizer, bass boost,\nand bit-perfect output.",
     (110, 50, 180), ["08_audio_tuning.webp"]),

    # Card 09: Album Details
    ("Screenshot_20260926_194706_LEVYRA.jpg", "DISCOGRAPHY", "Open the album.",
     "High-resolution artwork, release info,\nand complete tracklists.",
     (130, 60, 170), ["09_album.webp"]),

    # Card 10: Instant Search
    ("Screenshot_20261010_145010_LEVYRA.jpg", "INSTANT MATCH", "Find it instantly.",
     "Recent searches, suggestions, and\ninstant matching across your music.",
     (110, 50, 185), ["10_search.webp"]),

    # Card 11: Collections
    ("Screenshot_20261010_132136_LEVYRA.jpg", "CURATED PLAYLISTS", "Curated for you.",
     "Handpicked collections and gems\nrevolving around what you love.",
     (145, 45, 160), ["11_collections.webp"]),

    # Card 12: Listening Rhythm & Heatmaps
    ("Screenshot_20260927_131943_LEVYRA.jpg", "LISTENING CADENCE", "Your rhythm.",
     "Activity heatmaps, peak hours, and\nyour personal listening cadence.",
     (130, 65, 170), ["12_listening_rhythm.webp"]),

    # Card 13: Your Orbit
    ("Screenshot_20261010_132103_LEVYRA.jpg", "TOP ROTATION", "In your orbit.",
     "The songs and artists that always return\nto your daily rotation.",
     (105, 45, 195), ["13_your_orbit.webp"]),

    # Card 14: Private Listening Stats
    ("Screenshot_20261010_132050_LEVYRA.jpg", "ON-DEVICE STATS", "Keep it personal.",
     "Private listening stats and charts,\ncomputed strictly on your device.",
     (110, 55, 180), ["14_listening_pulse.webp"]),

    # Card 15: Artist Playlists
    ("Screenshot_20261010_144048_LEVYRA.jpg", "ESSENTIAL SETS", "Artist playlists.",
     "Curated sets, tours, and the\nessential catalog of every artist.",
     (100, 50, 190), ["15_artist_playlists.webp"]),

    # Card 16: Vault & Settings
    ("Screenshot_20261010_140700_LEVYRA.jpg", "PRIVACY & BACKUP", "Tailor every detail.",
     "Audio, design, gestures, and local\nsingle-file Vault backups.",
     (115, 55, 180), ["16_settings_vault.webp"]),

    # Card 17: New Releases
    ("Screenshot_20261010_144144_LEVYRA.jpg", "NEW MUSIC", "Fresh off the stage.",
     "New singles and albums updated\nevery week directly from artists.",
     (140, 50, 165), ["17_new_releases.webp"]),

    # Card 18: Artist Profile & Discography
    ("Screenshot_20261010_143646_LEVYRA.jpg", "ARTIST PROFILE", "Meet the artist.",
     "Full discography, singles, biographies,\nand top tracks in one tap.",
     (110, 50, 185), ["18_artist_profile.webp", "18_fresh_currents.webp"]),

    # Card 19: Top 50 Global Charts
    ("Screenshot_20261011_202032_LEVYRA.jpg", "GLOBAL CHARTS", "Top 50 charts.",
     "Explore daily country charts,\nviral hits, and top tracks worldwide.",
     (120, 50, 180), ["19_top_50.webp"]),

    # Card 20: Levyra Jam Collaborative Session
    ("Screenshot_20261011_133434_LEVYRA.jpg", "LEVYRA JAM", "Listen together.",
     "Real-time collaborative sessions on local Wi-Fi.\nShare queues, add tracks, and host together.",
     (105, 50, 190), ["20_soundstage.webp", "20_levyra_jam.webp"]),
]

def generate_hero_panoramic_showcase():
    hero_path = resolve_screenshot_path("levyra-banner-notturno-samsung.webp")
    if not hero_path or not os.path.exists(hero_path):
        return
    with Image.open(hero_path) as src:
        orig_w, orig_h = src.size
        hero = src.copy().convert("RGBA")
        out_path = os.path.join(OUT_SHOWCASE_DIR, "00_levyra_hero_wall_player.webp")
        hero.convert("RGB").save(out_path, "WEBP", quality=95, method=6)
        print(f"Generated Panoramic Hero Showcase at ({orig_w}, {orig_h}): {out_path}")

def generate_feature_cards():
    print("Generating Refined Material 3 Expressive Showcase Cards (Hero Scale, Continuous Diptych)...")
    c1, c2 = render_m3_duo_cut_cards()
    c1.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "01_home.webp"), "WEBP", quality=95, method=6)
    c2.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "02_stay_with_the_song.webp"), "WEBP", quality=95, method=6)
    print("Saved Material 3 Expressive Keynote Diptych: 01_home.webp, 02_stay_with_the_song.webp")

    for s_name, kicker, title, sub, kicker_c, fnames in M3_SHOWCASE_SPECS:
        card = render_m3_single_card(s_name, kicker, title, sub, kicker_c, target_w=670)
        card_rgb = card.convert("RGB")
        for fname in fnames:
            out_file = os.path.join(OUT_CARDS_DIR, fname)
            card_rgb.save(out_file, "WEBP", quality=95, method=6)
            print(f"Saved Refined Material 3 Expressive Card: {out_file}")

if __name__ == "__main__":
    generate_hero_panoramic_showcase()
    generate_feature_cards()
    print("All Refined Material 3 Expressive assets generated successfully!")
