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
        os.path.join(SCREENSHOT_DIR, filename),
        os.path.join(r"C:\Users\Luca Drogo\Desktop\screenshots", filename),
        os.path.join(r"C:\Users\Luca Drogo\Downloads", filename),
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

M3_PAIR_PALETTES = [
    ((250, 247, 255), (234, 221, 255), (103, 80, 164), (232, 222, 248)),
    ((255, 248, 250), (255, 216, 228), (152, 64, 83), (244, 192, 210)),
    ((247, 249, 255), (216, 226, 255), (65, 95, 145), (196, 211, 255)),
    ((247, 251, 244), (199, 239, 186), (56, 106, 32), (174, 222, 156)),
    ((255, 248, 243), (255, 220, 194), (139, 80, 0), (246, 195, 145)),
]

M3_PHONE_OVERRIDES = {
    "Screenshot_20261012_193022_LEVYRA.jpg": {
        "target_w": 720,
        "x": -35,
        "y": -100,
        "angle": 8,
    },
}

M3_HERO_SPECS = [
    ("Screenshot_20260926_171253_LEVYRA.jpg", "ZERO ADS • NATIVE PLAY", "Pure listening.",
     "Zero ads, zero accounts.\nStream and store locally on your device.",
     (95, 60, 165), ["01_home.webp"]),
    ("Screenshot_20260926_193948_LEVYRA.jpg", "OFFLINE STORAGE", "Download & keep.",
     "Clean M4A files in device storage.\nFull metadata, artwork, and lyrics.",
     (130, 60, 180), ["02_stay_with_the_song.webp"]),
]

def create_m3_pair_canvas(card_w, card_h, gutter, palette, pair_index):
    surface, container, primary, secondary = palette
    duo_w = card_w * 2 + gutter
    arr = np.zeros((card_h, duo_w, 4), dtype=np.uint8)
    top = np.array(surface, dtype=np.float32)
    bottom = np.array(container, dtype=np.float32)
    for y in range(card_h):
        progress = y / float(card_h)
        color = top * (1 - progress * 0.55) + bottom * (progress * 0.55)
        arr[y, :, :3] = color.astype(np.uint8)
    arr[:, :, 3] = 255
    canvas = Image.fromarray(arr, mode="RGBA")

    shapes = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(shapes)
    direction = -1 if pair_index % 2 else 1
    draw.ellipse(
        (duo_w // 2 - 520, -330 if direction > 0 else 1030, duo_w // 2 + 560, 720 if direction > 0 else 2050),
        fill=(*secondary, 220),
    )
    draw.ellipse((-320, 180, 540, 1040), fill=(*primary, 34))
    draw.ellipse((duo_w - 610, 760, duo_w + 250, 1620), fill=(*primary, 30))
    return Image.alpha_composite(canvas, shapes)

def draw_m3_label(draw, x, y, text, primary, on_primary=(255, 255, 255, 255)):
    font = get_font(27, bold=True)
    bbox = draw.textbbox((0, 0), text, font=font)
    width = bbox[2] - bbox[0]
    height = 52
    draw.rounded_rectangle((x, y, x + width + 80, y + height), radius=26, fill=(*primary, 255))
    draw_vector_sparkle(draw, x + 27, y + height // 2, 12, on_primary)
    draw.text((x + 50, y + 10 - bbox[1]), text, font=font, fill=on_primary)

def fit_title_font(draw, title, max_width, initial_size=68, minimum_size=50):
    for size in range(initial_size, minimum_size - 1, -2):
        font = get_font(size, bold=True)
        if draw.textbbox((0, 0), title, font=font)[2] <= max_width:
            return font
    return get_font(minimum_size, bold=True)

def draw_m3_copy(card, spec, placement, primary, surface):
    _, kicker, title, subtitle, _, _ = spec
    draw = ImageDraw.Draw(card)
    x = 66
    if placement == "bottom":
        draw.rounded_rectangle((-70, 1200, 970, 1660), radius=82, fill=(*surface, 255))
    y = 48 if placement == "top" else 1240
    draw_m3_label(draw, x, y, kicker, primary)
    title_y = y + 72
    title_font = fit_title_font(draw, title, 768)
    draw.text((x, title_y), title, font=title_font, fill=(32, 29, 36, 255))
    draw.multiline_text(
        (x, title_y + 86),
        subtitle,
        font=get_font(31),
        fill=(73, 69, 79, 255),
        spacing=7,
    )

def paste_cut_phone(canvas, spec, card_index, placement, gutter, angle):
    card_w = 900
    card_left = card_index * (card_w + gutter)
    phone_override = M3_PHONE_OVERRIDES.get(spec[0], {})
    screenshot_path = resolve_screenshot_path(spec[0])
    if not screenshot_path:
        raise FileNotFoundError(f"Missing showcase screenshot: {spec[0]}")
    with Image.open(screenshot_path) as source:
        phone = create_s26_ultra_mockup(source, target_w=phone_override.get("target_w", 700))
    angle = phone_override.get("angle", angle)
    rotated = phone.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
    shadow, pad = create_m3_shadow(rotated, blur=42, offset_y=24)

    if placement == "bottom":
        x = card_left + (230 if card_index == 0 else 120)
        y = -110
    else:
        x = card_left + (-40 if card_index == 0 else 160)
        y = 315
    x = card_left + phone_override.get("x", x - card_left)
    y = phone_override.get("y", y)
    canvas.paste(shadow, (x - pad, y - pad), shadow)
    canvas.paste(rotated, (x, y), rotated)

def render_m3_cut_pair(left_spec, right_spec, pair_index):
    card_w, card_h, gutter = 900, 1600, 48
    palette = M3_PAIR_PALETTES[pair_index % len(M3_PAIR_PALETTES)]
    canvas = create_m3_pair_canvas(card_w, card_h, gutter, palette, pair_index)

    left_copy = "bottom" if pair_index % 2 == 0 else "top"
    right_copy = "top" if left_copy == "bottom" else "bottom"
    paste_cut_phone(canvas, left_spec, 0, left_copy, gutter, 12 if pair_index % 2 == 0 else -10)
    paste_cut_phone(canvas, right_spec, 1, right_copy, gutter, -9 if pair_index % 2 == 0 else 11)

    right_left = card_w + gutter
    left_card = canvas.crop((0, 0, card_w, card_h))
    right_card = canvas.crop((right_left, 0, right_left + card_w, card_h))
    draw_m3_copy(left_card, left_spec, left_copy, palette[2], palette[0])
    draw_m3_copy(right_card, right_spec, right_copy, palette[2], palette[0])
    return left_card, right_card

# Material 3 Expressive content specifications
M3_SHOWCASE_SPECS = [
    # Card 03: Synced Lyrics
    ("screen-lyrics.jpg", "SYNCED LYRICS", "Sing along.",
     "Real-time synchronized lyrics, Romanized lines,\nand effortless translation.",
     (160, 45, 140), ["03_now_playing.webp"]),

    # Card 04: Player Decks
    ("Screenshot_20261012_193118_LEVYRA.jpg", "PLAYER DECKS", "Your listening deck.",
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
    ("Screenshot_20261012_193022_LEVYRA.jpg", "TOP ROTATION", "In your orbit.",
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
    print("Generating Material 3 Expressive cut-card pairs...")
    specs = M3_HERO_SPECS + M3_SHOWCASE_SPECS
    for pair_index in range(0, len(specs), 2):
        left_spec = specs[pair_index]
        right_spec = specs[pair_index + 1]
        left_card, right_card = render_m3_cut_pair(left_spec, right_spec, pair_index // 2)
        for card, spec in ((left_card, left_spec), (right_card, right_spec)):
            card_rgb = card.convert("RGB")
            for filename in spec[5]:
                out_file = os.path.join(OUT_CARDS_DIR, filename)
                card_rgb.save(out_file, "WEBP", quality=95, method=6)
                print(f"Saved Material 3 Expressive cut card: {out_file}")

if __name__ == "__main__":
    generate_hero_panoramic_showcase()
    generate_feature_cards()
    print("All Refined Material 3 Expressive assets generated successfully!")
