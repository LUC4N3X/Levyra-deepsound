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

def create_s26_ultra_mockup(screen_img, target_w=580):
    screen_img = enhance_screenshot(screen_img)
    orig_w, orig_h = screen_img.size
    aspect = orig_h / float(orig_w)
    target_h = int(target_w * aspect)
    screen_res = screen_img.resize((target_w, target_h), Image.Resampling.LANCZOS)

    bezel = 8
    corner_r = 24
    inner_r = max(6, corner_r - bezel)
    body_w = target_w + bezel * 2
    body_h = target_h + bezel * 2

    phone = Image.new("RGBA", (body_w, body_h), (0, 0, 0, 0))
    pdraw = ImageDraw.Draw(phone)

    # Samsung Galaxy S26 Ultra Titanium chassis
    pdraw.rounded_rectangle((0, 0, body_w - 1, body_h - 1), radius=corner_r, fill=(28, 29, 34, 255))
    pdraw.rounded_rectangle((1, 1, body_w - 2, body_h - 2), radius=corner_r - 1, fill=(14, 15, 18, 255))

    # Mask screen (uncut, complete)
    screen_mask = Image.new("L", (target_w, target_h), 0)
    smdraw = ImageDraw.Draw(screen_mask)
    smdraw.rounded_rectangle((0, 0, target_w - 1, target_h - 1), radius=inner_r, fill=255)
    phone.paste(screen_res, (bezel, bezel), screen_mask)

    # Inner bezel border
    pdraw.rounded_rectangle((bezel, bezel, bezel + target_w - 1, bezel + target_h - 1), radius=inner_r, outline=(8, 8, 10, 230), width=1)

    # Infinity-O Punch-hole Camera
    cam_diam = 14
    cam_x = body_w // 2 - cam_diam // 2
    cam_y = bezel + 10
    pdraw.ellipse((cam_x, cam_y, cam_x + cam_diam, cam_y + cam_diam), fill=(6, 6, 8, 255))
    pdraw.ellipse((cam_x + 3, cam_y + 3, cam_x + cam_diam - 3, cam_y + cam_diam - 3), fill=(14, 24, 46, 255))
    pdraw.ellipse((cam_x + 5, cam_y + 4, cam_x + 8, cam_y + 7), fill=(130, 175, 235, 180))

    # Micro speaker slit
    pdraw.rounded_rectangle((body_w // 2 - 20, 2, body_w // 2 + 20, 4), radius=1, fill=(40, 42, 48, 255))

    # Diamond-cut outer chamfer
    pdraw.rounded_rectangle((0, 0, body_w - 1, body_h - 1), radius=corner_r, outline=(145, 150, 162, 120), width=1)

    # Side buttons
    btn_x = body_w - 1
    btn_col = (45, 47, 52, 255)
    pdraw.line([(btn_x, 240), (btn_x, 310)], fill=btn_col, width=3)
    pdraw.line([(btn_x, 350), (btn_x, 460)], fill=btn_col, width=3)

    return phone

def create_m3_shadow(phone, blur=44, offset_y=24):
    w, h = phone.size
    pad = 80
    cw, ch = w + pad * 2, h + pad * 2 + offset_y
    alpha = phone.split()[-1]
    s = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    s_alpha = Image.new("L", (cw, ch), 0)
    s_alpha.paste(alpha, (pad, pad + offset_y))
    s_alpha = s_alpha.filter(ImageFilter.GaussianBlur(blur))
    s_alpha = ImageEnhance.Brightness(s_alpha).enhance(0.28)
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

def draw_m3_header(draw, card_w, kicker, title, subtitle, kicker_color):
    k_font = get_font(25, bold=True)
    k_bbox = k_font.getbbox(kicker)
    k_w = k_bbox[2] - k_bbox[0]
    sparkle_sz = 22
    gap = 10
    total_k_w = sparkle_sz + gap + k_w
    kx = (card_w - total_k_w) // 2
    ky = 52
    draw_vector_sparkle(draw, kx + sparkle_sz // 2, ky + 12, sparkle_sz // 2, kicker_color)
    draw.text((kx + sparkle_sz + gap, ky - k_bbox[1]), kicker, font=k_font, fill=kicker_color)

    t_font = get_font(56, bold=True)
    t_bbox = t_font.getbbox(title)
    t_w = t_bbox[2] - t_bbox[0]
    tx = (card_w - t_w) // 2
    ty = 96
    draw.text((tx, ty - t_bbox[1]), title, font=t_font, fill=(25, 28, 33, 255))

    s_font = get_font(27)
    lines = subtitle.split("\n")
    sy = 172
    for line in lines:
        s_bbox = s_font.getbbox(line)
        sw = s_bbox[2] - s_bbox[0]
        sx = (card_w - sw) // 2
        draw.text((sx, sy - s_bbox[1]), line, font=s_font, fill=(75, 78, 86, 255))
        sy += 36

def render_m3_card(screenshot_name, kicker, title, subtitle, kicker_color, bloom_color, target_w=580):
    card_w, card_h = 900, 1600

    # 1. Unified Google Material 3 Surface Container (Consistent, clean, zero-carnival)
    arr = np.zeros((card_h, card_w, 4), dtype=np.uint8)
    top_t = np.array([250, 251, 254], dtype=np.float32)
    bot_t = np.array([238, 242, 249], dtype=np.float32)
    for y in range(card_h):
        r = y / float(card_h)
        col = top_t * (1 - r) + bot_t * r
        arr[y, :, :3] = col.astype(np.uint8)
    arr[:, :, 3] = 255
    card = Image.fromarray(arr, mode="RGBA")

    # 2. Refined volumetric ambient bloom behind phone (Soft Material 3 light diffusion)
    glow = Image.new("RGBA", (card_w, card_h), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse((100, 320, 800, 1420), fill=(*bloom_color, 85))
    glow = glow.filter(ImageFilter.GaussianBlur(130))
    card = Image.alpha_composite(card, glow)

    # 3. Centered Google Material 3 Typography
    draw = ImageDraw.Draw(card)
    draw_m3_header(draw, card_w, kicker, title, subtitle, (*kicker_color, 255))

    # 4. Samsung Galaxy S26 Ultra mockup (Fully intact, uncut, complete screen visibility)
    s_path = resolve_screenshot_path(screenshot_name)
    if s_path and os.path.exists(s_path):
        with Image.open(s_path) as src:
            phone = create_s26_ultra_mockup(src, target_w=target_w)
        shadow, pad = create_m3_shadow(phone, blur=44, offset_y=24)
        px = (card_w - phone.width) // 2
        py = 285
        card.paste(shadow, (px - pad, py - pad), shadow)
        card.paste(phone, (px, py), phone)

    return card

# Authentic Material 3 Expressive Showcase Specifications (20 Uncut, Harmonious Cards)
M3_SHOWCASE_SPECS = [
    # Card 01: Home Screen
    ("Screenshot_20260926_171253_LEVYRA.jpg", "ZERO ADS • NATIVE PLAY", "Pure listening.",
     "Zero ads, zero accounts.\nStream and store locally on your device.",
     (95, 60, 165), (140, 120, 220), ["01_home.webp"]),

    # Card 02: Offline Storage
    ("Screenshot_20260926_193948_LEVYRA.jpg", "OFFLINE STORAGE", "Download & keep.",
     "Clean M4A files in device storage.\nFull metadata, artwork, and lyrics.",
     (0, 105, 195), (80, 150, 230), ["02_stay_with_the_song.webp"]),

    # Card 03: Synced Lyrics
    ("screen-lyrics.jpg", "SYNCED LYRICS", "Sing along.",
     "Real-time synchronized lyrics, Romanized lines,\nand effortless translation.",
     (175, 45, 125), (200, 100, 170), ["03_now_playing.webp"]),

    # Card 04: Player Decks
    ("Screenshot_20261010_190538_LEVYRA.jpg", "PLAYER DECKS", "Your listening deck.",
     "Fluid bottom sheets, gesture controls,\nand adaptive cover-art styling.",
     (0, 115, 195), (80, 160, 230), ["04_player_deck.webp"]),

    # Card 05: Explore & Fresh Currents
    ("Screenshot_20261010_132210_LEVYRA.jpg", "DYNAMIC DISCOVERY", "Fresh currents.",
     "Handpicked artist spotlights, community mixes,\nand global trending charts.",
     (15, 130, 85), (80, 190, 140), ["05_explore_stage.webp", "05_explore.webp"]),

    # Card 06: Levyra Mix
    ("Screenshot_20261010_132217_LEVYRA.jpg", "LEVYRA MIX", "Built for your taste.",
     "Algorithmic discovery stations powered by\nyour local listening history.",
     (105, 55, 190), (150, 105, 230), ["06_levyra_mix_stage.webp", "06_levyra_mix.webp"]),

    # Card 07: Moods & Genres
    ("Screenshot_20261010_132249_LEVYRA.jpg", "EXPLORE MOODS", "Every genre.",
     "Dive into curated categories, moods,\nand regional musical traditions.",
     (185, 75, 25), (220, 130, 80), ["07_genres.webp"]),

    # Card 08: Audio Tuning & Equalizer
    ("Screenshot_20261010_190726_LEVYRA.jpg", "AUDIO ARCHITECTURE", "Acoustic precision.",
     "10-band parametric equalizer, bass boost,\nand bit-perfect output.",
     (20, 130, 75), (80, 185, 130), ["08_audio_tuning.webp"]),

    # Card 09: Album Details
    ("Screenshot_20260926_194706_LEVYRA.jpg", "DISCOGRAPHY", "Open the album.",
     "High-resolution artwork, release info,\nand complete tracklists.",
     (165, 80, 20), (210, 135, 70), ["09_album.webp"]),

    # Card 10: Instant Search
    ("Screenshot_20261010_145010_LEVYRA.jpg", "INSTANT MATCH", "Find it instantly.",
     "Recent searches, suggestions, and\ninstant matching across your music.",
     (0, 110, 195), (75, 155, 230), ["10_search.webp"]),

    # Card 11: Curated Collections
    ("Screenshot_20261010_132136_LEVYRA.jpg", "CURATED PLAYLISTS", "Curated for you.",
     "Handpicked collections and gems\nrevolving around what you love.",
     (165, 35, 125), (205, 90, 165), ["11_collections.webp"]),

    # Card 12: Listening Rhythm Heatmap
    ("Screenshot_20260927_131943_LEVYRA.jpg", "LISTENING CADENCE", "Your rhythm.",
     "Activity heatmaps, peak hours, and\nyour personal listening cadence.",
     (155, 100, 10), (200, 150, 60), ["12_listening_rhythm.webp"]),

    # Card 13: Your Orbit
    ("Screenshot_20261010_132103_LEVYRA.jpg", "TOP ROTATION", "In your orbit.",
     "The songs and artists that always return\nto your daily rotation.",
     (105, 50, 185), (150, 100, 225), ["13_your_orbit.webp"]),

    # Card 14: Private Listening Stats
    ("Screenshot_20261010_132050_LEVYRA.jpg", "ON-DEVICE STATS", "Keep it personal.",
     "Private listening stats and charts,\ncomputed strictly on your device.",
     (0, 125, 140), (70, 175, 190), ["14_listening_pulse.webp"]),

    # Card 15: Artist Playlists
    ("Screenshot_20261010_144048_LEVYRA.jpg", "ESSENTIAL SETS", "Artist playlists.",
     "Curated sets, tours, and the\nessential catalog of every artist.",
     (25, 85, 205), (90, 140, 240), ["15_artist_playlists.webp"]),

    # Card 16: Vault & Settings
    ("Screenshot_20261010_140700_LEVYRA.jpg", "PRIVACY & BACKUP", "Tailor every detail.",
     "Audio, design, gestures, and local\nsingle-file Vault backups.",
     (25, 125, 65), (85, 180, 120), ["16_settings_vault.webp"]),

    # Card 17: New Releases
    ("Screenshot_20261010_144144_LEVYRA.jpg", "NEW MUSIC", "Fresh off the stage.",
     "New singles and albums updated\nevery week directly from artists.",
     (165, 40, 140), (200, 95, 180), ["17_new_releases.webp"]),

    # Card 18: Artist Profile & Discography
    ("Screenshot_20261010_143646_LEVYRA.jpg", "ARTIST PROFILE", "Meet the artist.",
     "Full discography, singles, biographies,\nand top tracks in one tap.",
     (20, 95, 205), (80, 145, 235), ["18_artist_profile.webp", "18_fresh_currents.webp"]),

    # Card 19: Top 50 Global Charts
    ("Screenshot_20261011_202032_LEVYRA.jpg", "GLOBAL CHARTS", "Top 50 charts.",
     "Explore daily country charts, viral hits,\nand top tracks worldwide.",
     (0, 105, 205), (90, 150, 240), ["19_top_50.webp"]),

    # Card 20: Levyra Jam Collaborative Session
    ("Screenshot_20261011_133434_LEVYRA.jpg", "LEVYRA JAM", "Listen together.",
     "Real-time collaborative sessions on local Wi-Fi.\nShare queues, add tracks, and host together.",
     (0, 110, 215), (75, 150, 240), ["20_soundstage.webp", "20_levyra_jam.webp"]),
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
    print("Generating Unified Material 3 Expressive Uncut Showcase Cards...")
    for s_name, kicker, title, sub, kicker_c, bloom_c, fnames in M3_SHOWCASE_SPECS:
        card = render_m3_card(s_name, kicker, title, sub, kicker_c, bloom_c, target_w=580)
        card_rgb = card.convert("RGB")
        for fname in fnames:
            out_file = os.path.join(OUT_CARDS_DIR, fname)
            card_rgb.save(out_file, "WEBP", quality=95, method=6)
            print(f"Saved Uncut M3 Card: {out_file}")

if __name__ == "__main__":
    generate_hero_panoramic_showcase()
    generate_feature_cards()
    print("All Unified Material 3 Expressive showcase cards generated successfully!")
