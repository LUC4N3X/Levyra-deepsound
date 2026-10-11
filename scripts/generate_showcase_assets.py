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
LOGO_PATH = os.environ.get(
    "LEVYRA_LOGO_PATH",
    r"C:\Users\Luca Drogo\Downloads\ChatGPT Image 5 set 2026, 19_28_06.png" if os.path.exists(r"C:\Users\Luca Drogo\Downloads\ChatGPT Image 5 set 2026, 19_28_06.png")
    else os.path.join("app", "src", "main", "res", "drawable", "levyra_logo.png")
)

def resolve_screenshot_path(filename):
    if not filename:
        return None
    if os.path.isabs(filename) and os.path.exists(filename):
        return filename
    candidates = [
        os.path.join(SCREENSHOT_DIR, filename),
        os.path.join(r"C:\Users\Luca Drogo\Downloads\Nuova cartella", filename),
        os.path.join(r"C:\Users\Luca Drogo\Desktop\screenshots", filename),
        os.path.join(r"C:\Users\Luca Drogo\Downloads", filename),
        os.path.join(r"C:\Users\Luca Drogo\Pictures\levyra\screenshots", filename),
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
    img = enhancer.enhance(1.06)
    enhancer = ImageEnhance.Color(img)
    img = enhancer.enhance(1.08)
    enhancer = ImageEnhance.Sharpness(img)
    img = enhancer.enhance(1.12)
    return img

def create_s26_ultra_phone(screen_img, target_height=1210):
    screen_img = enhance_screenshot(screen_img)
    ss = 2
    orig_w, orig_h = screen_img.size
    aspect = orig_w / float(orig_h)
    screen_h = int(target_height * ss)
    screen_w = int(screen_h * aspect)
    screen_res = screen_img.resize((screen_w, screen_h), Image.Resampling.LANCZOS)

    bezel = max(10, int(screen_w * 0.014))
    corner_r = int(screen_w * 0.046)
    inner_r = max(6, corner_r - bezel)

    body_w = screen_w + bezel * 2
    body_h = screen_h + bezel * 2

    phone = Image.new("RGBA", (body_w, body_h), (0, 0, 0, 0))
    pdraw = ImageDraw.Draw(phone)

    # Titanium chassis rim
    pdraw.rounded_rectangle((0, 0, body_w - 1, body_h - 1), radius=corner_r, fill=(28, 29, 33, 255))
    pdraw.rounded_rectangle((2, 2, body_w - 3, body_h - 3), radius=corner_r - 2, fill=(16, 17, 20, 255))

    # Mask screen
    screen_mask = Image.new("L", (screen_w, screen_h), 0)
    smdraw = ImageDraw.Draw(screen_mask)
    smdraw.rounded_rectangle((0, 0, screen_w - 1, screen_h - 1), radius=inner_r, fill=255)
    phone.paste(screen_res, (bezel, bezel), screen_mask)

    # Inner bezel border
    pdraw.rounded_rectangle(
        (bezel, bezel, bezel + screen_w - 1, bezel + screen_h - 1),
        radius=inner_r,
        outline=(10, 10, 12, 220),
        width=2
    )

    # Center punch-hole camera
    cam_diam = max(14, int(screen_w * 0.032))
    cam_x = body_w // 2 - cam_diam // 2
    cam_y = bezel + int(screen_h * 0.012)
    pdraw.ellipse((cam_x - 1, cam_y - 1, cam_x + cam_diam + 1, cam_y + cam_diam + 1), fill=(10, 10, 12, 240))
    pdraw.ellipse((cam_x, cam_y, cam_x + cam_diam, cam_y + cam_diam), fill=(6, 6, 8, 255))
    lens_r = cam_diam // 3
    pdraw.ellipse(
        (cam_x + lens_r, cam_y + lens_r, cam_x + cam_diam - lens_r, cam_y + cam_diam - lens_r),
        fill=(14, 24, 46, 255)
    )
    refl_r = max(2, cam_diam // 5)
    pdraw.ellipse(
        (cam_x + cam_diam // 2 - refl_r // 2, cam_y + cam_diam // 3, cam_x + cam_diam // 2 + refl_r // 2, cam_y + cam_diam // 3 + refl_r),
        fill=(130, 175, 235, 180)
    )

    # Micro speaker slit
    spk_w = max(28, int(screen_w * 0.08))
    spk_h = 3
    spk_x = body_w // 2 - spk_w // 2
    pdraw.rounded_rectangle((spk_x, 3, spk_x + spk_w, 3 + spk_h), radius=1, fill=(40, 42, 46, 255))

    # Outer chamfer highlight
    pdraw.rounded_rectangle((0, 0, body_w - 1, body_h - 1), radius=corner_r, outline=(140, 145, 155, 130), width=2)

    return phone.resize((body_w // ss, body_h // ss), Image.Resampling.LANCZOS)

def create_m3_shadow(img, blur1=22, blur2=52, offset_y=28):
    w, h = img.size
    pad = 90
    canvas_w = w + pad * 2
    canvas_h = h + pad * 2 + offset_y
    alpha = img.split()[-1]

    s1 = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    s1_alpha = Image.new("L", (canvas_w, canvas_h), 0)
    s1_alpha.paste(alpha, (pad, pad + 10))
    s1_alpha = s1_alpha.filter(ImageFilter.GaussianBlur(blur1))
    s1_alpha = ImageEnhance.Brightness(s1_alpha).enhance(0.24)
    s1.paste((18, 16, 28, 255), (0, 0), s1_alpha)

    s2 = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    s2_alpha = Image.new("L", (canvas_w, canvas_h), 0)
    s2_alpha.paste(alpha, (pad, pad + offset_y))
    s2_alpha = s2_alpha.filter(ImageFilter.GaussianBlur(blur2))
    s2_alpha = ImageEnhance.Brightness(s2_alpha).enhance(0.18)
    s2.paste((24, 20, 36, 255), (0, 0), s2_alpha)

    shadow = Image.alpha_composite(s2, s1)
    return shadow, pad

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
    tw = bbox[2] - bbox[0]
    return sparkle_size + sparkle_gap + tw, th

def render_m3_duo_cut_cards():
    card_w, card_h = 900, 1600
    gutter = 48
    duo_w = card_w * 2 + gutter

    # 1. Continuous Vibrant Material 3 Expressive Tonal Canvas (No boxes, no square containers)
    bg_arr = np.zeros((card_h, duo_w, 4), dtype=np.uint8)

    tl = np.array([218, 208, 255], dtype=np.float32)  # M3 Primary Lavender Container
    bl = np.array([198, 220, 255], dtype=np.float32)  # M3 Soft Iris
    tr = np.array([255, 228, 208], dtype=np.float32)  # M3 Sunset Peach Container
    br = np.array([255, 210, 182], dtype=np.float32)  # M3 Warm Amber Container

    y_ratio = np.linspace(0, 1, card_h).reshape(card_h, 1, 1)
    x_ratio = np.linspace(0, 1, duo_w).reshape(1, duo_w, 1)

    left_col = tl * (1 - y_ratio) + bl * y_ratio
    right_col = tr * (1 - y_ratio) + br * y_ratio
    full_col = left_col * (1 - x_ratio) + right_col * x_ratio

    bg_arr[:, :, :3] = np.clip(full_col, 0, 255).astype(np.uint8)
    bg_arr[:, :, 3] = 255
    bg = Image.fromarray(bg_arr, mode="RGBA")

    # Volumetric ambilight blooms
    glow_layer = Image.new("RGBA", (duo_w, card_h), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow_layer)
    gdraw.ellipse((140, 80, 880, 940), fill=(160, 110, 255, 170))
    gdraw.ellipse((duo_w - 860, 380, duo_w - 90, 1220), fill=(255, 150, 40, 160))
    glow_layer = glow_layer.filter(ImageFilter.GaussianBlur(100))
    bg = Image.alpha_composite(bg, glow_layer)

    # Load screenshots
    s_home = resolve_screenshot_path("Screenshot_20260926_171253_LEVYRA.jpg")
    s_player = resolve_screenshot_path("Screenshot_20260926_193948_LEVYRA.jpg")

    angle = 15.0
    c2_left = card_w + gutter

    # 1. Phone 1 (Taylor Swift - Home)
    # Tilted 15°, placed so its right edge cuts across into Card 02!
    if s_home and os.path.exists(s_home):
        with Image.open(s_home) as src1:
            p1 = create_s26_ultra_phone(src1, target_height=1210)
        rot1 = p1.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
        s1_shadow, pad1 = create_m3_shadow(rot1, blur1=24, blur2=52, offset_y=28)

        p1_x = 250
        p1_y = 50
        bg.paste(s1_shadow, (p1_x - pad1, p1_y - pad1), s1_shadow)
        bg.paste(rot1, (p1_x, p1_y), rot1)

    # 2. Phone 2 (The Weeknd - Now Playing)
    # Floating in Card 02 alongside the cut edge of Phone 1, with comfortable top breathing room
    if s_player and os.path.exists(s_player):
        with Image.open(s_player) as src2:
            p2 = create_s26_ultra_phone(src2, target_height=1070)
        rot2 = p2.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
        s2_shadow, pad2 = create_m3_shadow(rot2, blur1=20, blur2=46, offset_y=24)

        p2_x = c2_left + 225
        p2_y = 350
        bg.paste(s2_shadow, (p2_x - pad2, p2_y - pad2), s2_shadow)
        bg.paste(rot2, (p2_x, p2_y), rot2)

    # Draw typography directly on the surface - clean, box-free, badge-free!
    draw = ImageDraw.Draw(bg)

    # --- CARD 01 (Bottom Left Typography) ---
    tx1, ty1 = 76, 1260
    draw_clean_m3_kicker(draw, tx1, ty1, "ZERO ADS • NATIVE PLAY", color=(85, 55, 155, 255), font_size=27)
    draw.text((tx1, ty1 + 46), "Pure listening.", font=get_font(66, bold=True), fill=(29, 27, 32, 255))
    draw.multiline_text(
        (tx1, ty1 + 130),
        "Zero ads, zero accounts.\nStream and store locally on your device.",
        font=get_font(31),
        fill=(70, 65, 80, 255),
        spacing=8
    )

    # --- CARD 02 (Top Left Typography) ---
    tx2, ty2 = c2_left + 82, 75
    draw_clean_m3_kicker(draw, tx2, ty2, "OFFLINE STORAGE", color=(145, 85, 0, 255), font_size=27)
    draw.text((tx2, ty2 + 46), "Download & keep.", font=get_font(66, bold=True), fill=(29, 27, 32, 255))
    draw.multiline_text(
        (tx2, ty2 + 130),
        "Clean M4A files in device storage.\nFull metadata, artwork, and lyrics.",
        font=get_font(31),
        fill=(75, 65, 60, 255),
        spacing=8
    )

    # Slice into Card 01 (0..900) and Card 02 (948..1848)
    c1 = bg.crop((0, 0, card_w, card_h))
    c2 = bg.crop((c2_left, 0, c2_left + card_w, card_h))

    # M3 outline-variant borders
    bdraw1 = ImageDraw.Draw(c1)
    bdraw1.rounded_rectangle((0, 0, card_w - 1, card_h - 1), radius=36, outline=(175, 165, 215, 190), width=2)

    bdraw2 = ImageDraw.Draw(c2)
    bdraw2.rounded_rectangle((0, 0, card_w - 1, card_h - 1), radius=36, outline=(230, 200, 170, 190), width=2)

    return c1, c2

def render_m3_single_card(screenshot_name, kicker, title, subtitle, top_tint, bot_tint, kicker_color, ambilight, border_color, target_height=1210):
    card_w, card_h = 900, 1600

    # 1. Tonal Surface Gradient
    arr = np.zeros((card_h, card_w, 4), dtype=np.uint8)
    for y in range(card_h):
        r = y / float(card_h)
        col = [int(top_tint[i] * (1 - r) + bot_tint[i] * r) for i in range(3)]
        arr[y, :, :3] = col
    arr[:, :, 3] = 255
    card = Image.fromarray(arr, mode="RGBA")

    # 2. Volumetric ambilight glow behind phone
    glow = Image.new("RGBA", (card_w, card_h), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse((100, 380, 800, 1300), fill=(*ambilight, 150))
    glow = glow.filter(ImageFilter.GaussianBlur(95))
    card = Image.alpha_composite(card, glow)

    # 3. Clean M3 Typography - directly on surface, NO boxes or bulky badges!
    draw = ImageDraw.Draw(card)
    tx, ty = 76, 75
    draw_clean_m3_kicker(draw, tx, ty, kicker, kicker_color, font_size=27)

    draw.text((tx, ty + 46), title, font=get_font(66, bold=True), fill=(29, 27, 32, 255))
    draw.multiline_text(
        (tx, ty + 130),
        subtitle,
        font=get_font(31),
        fill=(75, 65, 75, 255),
        spacing=8
    )

    # 4. Samsung Galaxy S26 Ultra mockup
    s_path = resolve_screenshot_path(screenshot_name)
    if s_path and os.path.exists(s_path):
        with Image.open(s_path) as src:
            phone = create_s26_ultra_phone(src, target_height=target_height)
        shadow, pad = create_m3_shadow(phone, blur1=22, blur2=48, offset_y=26)
        px = (card_w - phone.width) // 2
        py = 390
        card.paste(shadow, (px - pad, py - pad), shadow)
        card.paste(phone, (px, py), phone)

    # 5. M3 outline-variant border
    bdraw = ImageDraw.Draw(card)
    bdraw.rounded_rectangle((0, 0, card_w - 1, card_h - 1), radius=36, outline=border_color, width=2)
    return card

# Complete suite of Material 3 Expressive specifications with 100% ACCURATE SCREENSHOT MAPPINGS:
M3_REMAINING_SPECS = [
    # Card 03: Lyrics
    ("screen-lyrics.jpg", "SYNCED LYRICS", "Sing along.", "Real-time synchronized lyrics, Romanized lines,\nand effortless translation.",
     (255, 222, 238), (255, 198, 224), (194, 24, 91), (233, 30, 99), (235, 185, 215, 200), ["03_now_playing.webp"]),

    # Card 04: Player Decks (Choose your player style - Immersive, Canvas Card, etc.)
    ("Screenshot_20261010_190538_LEVYRA.jpg", "PLAYER DECKS", "Your listening deck.", "Fluid bottom sheets, gesture controls,\nand adaptive cover-art styling.",
     (216, 240, 255), (195, 228, 252), (0, 115, 195), (0, 170, 255), (170, 215, 248, 200), ["04_player_deck.webp"]),

    # Card 05: Explore & Fresh Currents (Discover new music, trends and videos)
    ("Screenshot_20261010_132210_LEVYRA.jpg", "DYNAMIC DISCOVERY", "Fresh currents.", "Handpicked artist spotlights, community mixes,\nand global trending charts.",
     (220, 248, 230), (198, 240, 214), (20, 135, 75), (35, 195, 105), (175, 228, 195, 200), ["05_explore_stage.webp", "05_explore.webp"]),

    # Card 06: Levyra Mix (Algorithmic discovery stations)
    ("Screenshot_20261010_132217_LEVYRA.jpg", "LEVYRA MIX", "Built for your taste.", "Algorithmic discovery stations powered by\nyour local listening history.",
     (234, 218, 255), (214, 192, 252), (115, 45, 195), (155, 75, 245), (205, 175, 245, 200), ["06_levyra_mix_stage.webp", "06_levyra_mix.webp"]),

    # Card 07: Genres & Moods (Romance, Sad, Sleep, Workout, Genres)
    ("Screenshot_20261010_132249_LEVYRA.jpg", "EXPLORE MOODS", "Every genre.", "Dive into curated categories, moods,\nand regional musical traditions.",
     (255, 226, 212), (255, 204, 185), (215, 75, 25), (255, 110, 45), (245, 185, 160, 200), ["07_genres.webp"]),

    # Card 08: Audio Architecture & Tuning (Sleep timer, tempo, audio quality, ambient)
    ("Screenshot_20261010_190726_LEVYRA.jpg", "AUDIO ARCHITECTURE", "Acoustic precision.", "10-band parametric equalizer, bass boost,\nand bit-perfect output.",
     (224, 245, 228), (200, 235, 208), (25, 130, 65), (45, 190, 95), (180, 225, 190, 200), ["08_audio_tuning.webp"]),

    # Card 09: Album Details (Hurry Up Tomorrow - The Weeknd)
    ("Screenshot_20260926_194706_LEVYRA.jpg", "DISCOGRAPHY", "Open the album.", "High-resolution artwork, release info,\nand complete tracklists.",
     (255, 232, 214), (255, 212, 188), (185, 85, 15), (240, 125, 35), (240, 195, 165, 200), ["09_album.webp"]),

    # Card 10: Instant Search (Recent searches, suggestions, artists)
    ("Screenshot_20261010_145010_LEVYRA.jpg", "INSTANT MATCH", "Find it instantly.", "Recent searches, suggestions, and\ninstant matching across your music.",
     (218, 238, 255), (196, 224, 252), (0, 115, 195), (25, 155, 235), (175, 215, 248, 200), ["10_search.webp"]),

    # Card 11: Collections (Levyra Collections, Quick picks)
    ("Screenshot_20261010_132136_LEVYRA.jpg", "CURATED PLAYLISTS", "Curated for you.", "Handpicked collections and gems\nrevolving around what you love.",
     (255, 220, 242), (255, 195, 230), (175, 35, 125), (225, 60, 155), (240, 180, 220, 200), ["11_collections.webp"]),

    # Card 12: Listening Rhythm & Heatmaps (Attività di ascolto, Il tuo ritmo)
    ("Screenshot_20260927_131943_LEVYRA.jpg", "LISTENING CADENCE", "Your rhythm.", "Activity heatmaps, peak hours, and\nyour personal listening cadence.",
     (255, 238, 204), (255, 220, 172), (165, 105, 0), (240, 165, 30), (240, 205, 155, 200), ["12_listening_rhythm.webp"]),

    # Card 13: Your Orbit (Top rotation, Songs that return)
    ("Screenshot_20261010_132103_LEVYRA.jpg", "TOP ROTATION", "In your orbit.", "The songs and artists that always return\nto your daily rotation.",
     (230, 220, 255), (208, 194, 252), (105, 45, 195), (140, 75, 240), (205, 178, 245, 200), ["13_your_orbit.webp"]),

    # Card 14: Private Listening Stats (Library, Listening Pulse, 0 min)
    ("Screenshot_20261010_132050_LEVYRA.jpg", "ON-DEVICE STATS", "Keep it personal.", "Private listening stats and charts,\ncomputed strictly on your device.",
     (216, 248, 248), (192, 238, 238), (0, 135, 145), (0, 195, 205), (170, 225, 225, 200), ["14_listening_pulse.webp"]),

    # Card 15: Artist Playlists (Essential sets, The Weeknd)
    ("Screenshot_20261010_144048_LEVYRA.jpg", "ESSENTIAL SETS", "Artist playlists.", "Curated sets, tours, and the\nessential catalog of every artist.",
     (222, 232, 255), (198, 215, 252), (35, 85, 205), (50, 115, 245), (180, 205, 248, 200), ["15_artist_playlists.webp"]),

    # Card 16: Vault & Settings (Customize LEVYRA, single-file backup)
    ("Screenshot_20261010_140700_LEVYRA.jpg", "PRIVACY & BACKUP", "Tailor every detail.", "Audio, design, gestures, and local\nsingle-file Vault backups.",
     (225, 246, 232), (202, 236, 212), (35, 130, 70), (60, 185, 105), (180, 225, 195, 200), ["16_settings_vault.webp"]),

    # Card 17: New Releases (Don't Chart, Wish Upon You)
    ("Screenshot_20261010_144144_LEVYRA.jpg", "NEW MUSIC", "Fresh off the stage.", "New singles and albums updated\nevery week directly from artists.",
     (255, 222, 248), (255, 198, 240), (175, 35, 145), (225, 55, 190), (240, 180, 230, 200), ["17_new_releases.webp"]),

    # Card 18: Artist Profile & Discography (Billie Eilish, biography, top tracks)
    ("Screenshot_20261010_143646_LEVYRA.jpg", "ARTIST PROFILE", "Meet the artist.", "Full discography, singles, biographies,\nand top tracks in one tap.",
     (224, 235, 255), (200, 220, 252), (25, 95, 210), (40, 130, 245), (180, 210, 248, 200), ["18_artist_profile.webp", "18_fresh_currents.webp"]),

    # Card 19: Top 50 Global Charts (Top 50 United States, daily charts)
    ("Screenshot_20261010_132028_LEVYRA.jpg", "GLOBAL CHARTS", "Top 50 charts.", "Explore daily country charts,\nviral hits, and top tracks worldwide.",
     (255, 224, 220), (255, 200, 194), (205, 45, 35), (250, 65, 50), (245, 180, 175, 200), ["19_top_50.webp"]),

    # Card 20: Levyra Jam (Collaborative listening on Wi-Fi)
    ("Screenshot_20261011_133434_LEVYRA.jpg", "LEVYRA JAM", "Listen together.", "Real-time collaborative sessions on local Wi-Fi.\nShare queues, add tracks, and host together.",
     (222, 236, 255), (198, 222, 255), (0, 105, 225), (0, 150, 255), (175, 215, 250, 200), ["20_soundstage.webp", "20_levyra_jam.webp"]),
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
    print("Generating Clean Material 3 Expressive Showcase Cards with accurate screen mappings...")
    c1, c2 = render_m3_duo_cut_cards()
    c1.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "01_home.webp"), "WEBP", quality=95, method=6)
    c2.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "02_stay_with_the_song.webp"), "WEBP", quality=95, method=6)
    print("Saved Material 3 Expressive Keynote Diptych: 01_home.webp, 02_stay_with_the_song.webp")

    for s_name, kicker, title, sub, top_t, bot_t, kicker_c, amb, border_c, fnames in M3_REMAINING_SPECS:
        card = render_m3_single_card(s_name, kicker, title, sub, top_t, bot_t, kicker_c, amb, border_c, target_height=1210)
        for fn in fnames:
            out_p = os.path.join(OUT_CARDS_DIR, fn)
            card.convert("RGB").save(out_p, "WEBP", quality=95, method=6)
            print(f"Saved Material 3 Expressive Card: {out_p}")

def main():
    print("Starting Levyra Clean Material 3 Expressive Showcase Generation...")
    try:
        generate_hero_panoramic_showcase()
    except (FileNotFoundError, OSError) as e:
        print(f"Skipping hero panoramic showcase ({e})")
    generate_feature_cards()
    print("All Clean Material 3 Expressive assets generated successfully!")

if __name__ == "__main__":
    main()
