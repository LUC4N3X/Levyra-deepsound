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

def create_m3_shadow(element, blur=48, offset_y=28):
    w, h = element.size
    pad = 90
    cw, ch = w + pad * 2, h + pad * 2 + offset_y
    alpha = element.split()[-1]
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

M3_EXPRESSIVE_SCHEMES = [
    {
        "surface": (255, 247, 255), "surface_low": (247, 242, 250),
        "surface_high": (236, 230, 240), "surface_lowest": (255, 255, 255),
        "on_surface": (30, 27, 32), "on_surface_variant": (73, 69, 79),
        "primary": (103, 80, 164), "on_primary": (255, 255, 255),
        "primary_container": (234, 221, 255), "on_primary_container": (33, 0, 93),
        "secondary_container": (232, 222, 248), "on_secondary_container": (29, 25, 43),
        "tertiary_container": (255, 216, 228), "on_tertiary_container": (49, 17, 29),
        "outline_variant": (202, 196, 208),
    },
    {
        "surface": (255, 248, 250), "surface_low": (255, 241, 245),
        "surface_high": (247, 226, 233), "surface_lowest": (255, 255, 255),
        "on_surface": (35, 25, 29), "on_surface_variant": (82, 67, 73),
        "primary": (152, 64, 83), "on_primary": (255, 255, 255),
        "primary_container": (255, 216, 228), "on_primary_container": (62, 0, 20),
        "secondary_container": (244, 221, 227), "on_secondary_container": (43, 25, 31),
        "tertiary_container": (255, 221, 184), "on_tertiary_container": (45, 23, 0),
        "outline_variant": (216, 193, 200),
    },
    {
        "surface": (247, 249, 255), "surface_low": (239, 243, 252),
        "surface_high": (222, 230, 244), "surface_lowest": (255, 255, 255),
        "on_surface": (25, 28, 34), "on_surface_variant": (67, 71, 80),
        "primary": (65, 95, 145), "on_primary": (255, 255, 255),
        "primary_container": (216, 226, 255), "on_primary_container": (0, 27, 62),
        "secondary_container": (220, 226, 244), "on_secondary_container": (24, 28, 42),
        "tertiary_container": (246, 216, 255), "on_tertiary_container": (52, 19, 65),
        "outline_variant": (195, 199, 210),
    },
    {
        "surface": (247, 251, 244), "surface_low": (239, 247, 234),
        "surface_high": (222, 237, 214), "surface_lowest": (255, 255, 255),
        "on_surface": (26, 30, 24), "on_surface_variant": (67, 73, 63),
        "primary": (56, 106, 32), "on_primary": (255, 255, 255),
        "primary_container": (199, 239, 186), "on_primary_container": (10, 34, 0),
        "secondary_container": (218, 232, 208), "on_secondary_container": (25, 32, 21),
        "tertiary_container": (188, 235, 234), "on_tertiary_container": (0, 32, 32),
        "outline_variant": (194, 202, 188),
    },
    {
        "surface": (255, 248, 243), "surface_low": (252, 242, 234),
        "surface_high": (243, 226, 213), "surface_lowest": (255, 255, 255),
        "on_surface": (33, 27, 23), "on_surface_variant": (81, 68, 59),
        "primary": (139, 80, 0), "on_primary": (255, 255, 255),
        "primary_container": (255, 220, 194), "on_primary_container": (45, 23, 0),
        "secondary_container": (241, 223, 208), "on_secondary_container": (42, 29, 21),
        "tertiary_container": (218, 231, 190), "on_tertiary_container": (27, 33, 8),
        "outline_variant": (214, 195, 182),
    },
]

M3_HERO_SPECS = [
    ("Screenshot_20260926_171253_LEVYRA.jpg", "ZERO ADS • NATIVE PLAY", "Pure listening.",
     "Zero ads, zero accounts.\nStream and store locally on your device.",
     (95, 60, 165), ["01_home.webp"]),
    ("Screenshot_20260926_193948_LEVYRA.jpg", "OFFLINE STORAGE", "Download & keep.",
     "Clean M4A files in device storage.\nFull metadata, artwork, and lyrics.",
     (130, 60, 180), ["02_stay_with_the_song.webp"]),
]

M3_MARKETING_COPY = {
    "Screenshot_20260926_171253_LEVYRA.jpg": "Ad-free music, stored locally.",
    "Screenshot_20260926_193948_LEVYRA.jpg": "Offline files with artwork and lyrics.",
    "screen-lyrics.jpg": "Synced lyrics, Romanization, translation.",
    "Screenshot_20261012_193118_LEVYRA.jpg": "Four player styles. One fluid experience.",
    "Screenshot_20261010_132210_LEVYRA.jpg": "Fresh releases, mixes, and global charts.",
    "Screenshot_20261010_132217_LEVYRA.jpg": "Stations shaped by your listening.",
    "Screenshot_20261010_132249_LEVYRA.jpg": "Moods, genres, and music worldwide.",
    "Screenshot_20261010_190726_LEVYRA.jpg": "Parametric EQ and bit-perfect output.",
    "Screenshot_20260926_194706_LEVYRA.jpg": "Artwork, credits, and complete tracklists.",
    "Screenshot_20261013_111815_LEVYRA.jpg": "Search, suggestions, and instant matches.",
    "Screenshot_20261010_132136_LEVYRA.jpg": "Collections shaped around your taste.",
    "Screenshot_20260927_131943_LEVYRA.jpg": "See when and how you listen.",
    "Screenshot_20261012_193022_LEVYRA.jpg": "Your most-played music, always close.",
    "Screenshot_20261010_132050_LEVYRA.jpg": "Listening insights that stay on-device.",
    "Screenshot_20261010_144048_LEVYRA.jpg": "Essential sets for every artist.",
    "Screenshot_20261010_140700_LEVYRA.jpg": "Design, audio, gestures, and local backups.",
    "Screenshot_20261010_144144_LEVYRA.jpg": "New singles and albums, every week.",
    "Screenshot_20261010_143646_LEVYRA.jpg": "Discography, biography, and top tracks.",
    "Screenshot_20261011_202032_LEVYRA.jpg": "Daily charts from every country.",
    "Screenshot_20261011_133434_LEVYRA.jpg": "Share the queue over local Wi-Fi.",
}

M3_PAIR_PLACEMENTS = [
    ("bottom", "top"),
    ("bottom", "top"),
    ("top", "bottom"),
    ("top", "bottom"),
    ("bottom", "bottom"),
    ("top", "top"),
    ("top", "bottom"),
    ("bottom", "top"),
    ("bottom", "top"),
    ("top", "bottom"),
]

SOFT_M3_PALETTES = [
    ((250, 247, 252), (94, 68, 145), (232, 222, 248)),
    ((247, 250, 255), (54, 88, 139), (219, 232, 251)),
    ((255, 248, 249), (145, 66, 91), (248, 222, 231)),
    ((248, 252, 247), (58, 105, 61), (222, 239, 218)),
    ((255, 250, 245), (139, 86, 37), (249, 230, 207)),
    ((250, 248, 255), (105, 75, 151), (232, 224, 250)),
    ((247, 252, 252), (37, 105, 103), (215, 239, 236)),
    ((255, 248, 246), (151, 73, 55), (250, 224, 215)),
    ((248, 249, 255), (67, 84, 145), (222, 228, 249)),
    ((252, 252, 246), (101, 105, 42), (237, 239, 211)),
]

def draw_rounded_layer(canvas, size, radius, color, angle, position):
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    draw.rounded_rectangle((0, 0, size[0] - 1, size[1] - 1), radius=radius, fill=(*color, 255))
    rotated = layer.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
    canvas.paste(rotated, position, rotated)

def create_m3_pair_canvas(card_w, card_h, gutter, scheme, pair_index):
    duo_w = card_w * 2 + gutter
    canvas = Image.new("RGBA", (duo_w, card_h), (250, 249, 252, 255))
    for card_index in range(2):
        card_left = card_index * (card_w + gutter)
        bg, accent, wash = SOFT_M3_PALETTES[(pair_index * 2 + card_index) % len(SOFT_M3_PALETTES)]
        card = Image.new("RGBA", (card_w, card_h), (*bg, 255))
        draw = ImageDraw.Draw(card)
        for y in range(card_h):
            mix = y / card_h
            color = tuple(int(bg[i] * (1 - mix * 0.16) + wash[i] * mix * 0.16) for i in range(3))
            draw.line((0, y, card_w, y), fill=(*color, 255))
        direction = 1 if (pair_index + card_index) % 2 == 0 else -1
        draw_rounded_layer(
            card,
            (790, 880),
            360,
            wash,
            7 * direction,
            (-430 if direction > 0 else 545, 380 if pair_index % 2 else 710),
        )
        draw.ellipse(
            (680 if direction > 0 else -180, 90, 1060 if direction > 0 else 200, 470),
            fill=(*wash, 255),
        )
        dot_x = 824 if direction > 0 else 54
        draw.ellipse((dot_x, 104, dot_x + 22, 126), fill=(*accent, 255))
        canvas.paste(card, (card_left, 0))

    return canvas

def draw_m3_label(draw, x, y, text, scheme):
    font = get_font(27, bold=True)
    bbox = draw.textbbox((0, 0), text, font=font)
    width = bbox[2] - bbox[0]
    accent = scheme["accent"]
    draw.rounded_rectangle((x, y + 9, x + 38, y + 17), radius=4, fill=(*accent, 255))
    draw.text((x + 55, y - bbox[1]), text, font=font, fill=(*accent, 255))

def fit_title_font(draw, title, max_width, initial_size=68, minimum_size=50):
    for size in range(initial_size, minimum_size - 1, -2):
        font = get_font(size, bold=True)
        if draw.textbbox((0, 0), title, font=font)[2] <= max_width:
            return font
    return get_font(minimum_size, bold=True)

def fit_body_font(draw, text, max_width, initial_size=36, minimum_size=28):
    for size in range(initial_size, minimum_size - 1, -1):
        font = get_font(size)
        if draw.textbbox((0, 0), text, font=font)[2] <= max_width:
            return font
    return get_font(minimum_size)

def draw_m3_copy(card, spec, placement, scheme, card_index, pair_index):
    screenshot_name, kicker, title, _, _, _ = spec
    subtitle = M3_MARKETING_COPY[screenshot_name]
    draw = ImageDraw.Draw(card)
    x = 58
    y = 58 if placement == "top" else 1238
    draw_m3_label(draw, x, y, kicker, scheme)

    title_y = y + 60
    title_font = fit_title_font(draw, title, 790, initial_size=86, minimum_size=58)
    draw.text((x, title_y), title, font=title_font, fill=(35, 33, 39, 255))
    body_font = fit_body_font(draw, subtitle, 760, initial_size=31, minimum_size=25)
    draw.text((x, title_y + 102), subtitle, font=body_font, fill=(91, 87, 98, 255))

def crop_to_fill(source, width, height):
    source = enhance_screenshot(source)
    scale = max(width / source.width, height / source.height)
    resized = source.resize(
        (int(source.width * scale), int(source.height * scale)),
        Image.Resampling.LANCZOS,
    )
    left = max(0, (resized.width - width) // 2)
    top = max(0, int((resized.height - height) * 0.42))
    return resized.crop((left, top, left + width, top + height))

def create_s26_ultra_mockup(source, size=(720, 1500)):
    frame = 13
    outer_w, outer_h = size
    inner_w, inner_h = outer_w - frame * 2, outer_h - frame * 2
    phone = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(phone)
    draw.rounded_rectangle(
        (0, 0, outer_w - 1, outer_h - 1),
        radius=54,
        fill=(28, 29, 34, 255),
        outline=(120, 121, 128, 220),
        width=3,
    )
    screenshot = crop_to_fill(source, inner_w, inner_h)
    mask = Image.new("L", (inner_w, inner_h), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle((0, 0, inner_w - 1, inner_h - 1), radius=42, fill=255)
    phone.paste(screenshot, (frame, frame), mask)
    draw.rounded_rectangle(
        (frame, frame, outer_w - frame - 1, outer_h - frame - 1),
        radius=42,
        outline=(8, 9, 12, 245),
        width=4,
    )
    camera_radius = 9
    camera_x = outer_w // 2
    camera_y = frame + 22
    draw.ellipse(
        (
            camera_x - camera_radius,
            camera_y - camera_radius,
            camera_x + camera_radius,
            camera_y + camera_radius,
        ),
        fill=(4, 5, 8, 255),
    )
    draw.ellipse(
        (camera_x - 4, camera_y - 4, camera_x + 3, camera_y + 3),
        fill=(35, 58, 88, 230),
    )
    draw.rounded_rectangle(
        (outer_w // 2 - 28, 3, outer_w // 2 + 28, 7),
        radius=2,
        fill=(54, 55, 61, 255),
    )
    draw.line((outer_w - 2, 285, outer_w - 2, 395), fill=(75, 77, 85, 255), width=4)
    draw.line((outer_w - 2, 435, outer_w - 2, 545), fill=(75, 77, 85, 255), width=4)
    return phone

def paste_editorial_phone(canvas, spec, card_index, placement, gutter, pair_index):
    card_w = 900
    card_left = card_index * (card_w + gutter)
    screenshot_path = resolve_screenshot_path(spec[0])
    if not screenshot_path:
        raise FileNotFoundError(f"Missing showcase screenshot: {spec[0]}")
    with Image.open(screenshot_path) as source:
        if spec[0] == "screen-lyrics.jpg":
            source = ImageEnhance.Brightness(source).enhance(1.16)
            source = ImageEnhance.Contrast(source).enhance(0.94)
        phone = create_s26_ultra_mockup(source)

    direction = 1 if pair_index % 2 == 0 else -1
    if placement == "bottom":
        angle = -2.5 * direction
        x = card_left + (145 if card_index == 0 else 15)
        y = -285
    else:
        angle = 2.5 * direction
        x = card_left + (15 if card_index == 0 else 145)
        y = 285

    rotated = phone.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
    shadow, pad = create_m3_shadow(rotated, blur=24, offset_y=12)
    canvas.paste(shadow, (x - pad, y - pad), shadow)
    canvas.paste(rotated, (x, y), rotated)

def render_m3_editorial_pair(left_spec, right_spec, pair_index):
    card_w, card_h, gutter = 900, 1600, 48
    scheme = M3_EXPRESSIVE_SCHEMES[pair_index % len(M3_EXPRESSIVE_SCHEMES)]
    canvas = create_m3_pair_canvas(card_w, card_h, gutter, scheme, pair_index)

    left_copy, right_copy = M3_PAIR_PLACEMENTS[pair_index]
    paste_editorial_phone(canvas, left_spec, 0, left_copy, gutter, pair_index)
    paste_editorial_phone(canvas, right_spec, 1, right_copy, gutter, pair_index)

    right_left = card_w + gutter
    left_card = canvas.crop((0, 0, card_w, card_h))
    right_card = canvas.crop((right_left, 0, right_left + card_w, card_h))
    left_palette = SOFT_M3_PALETTES[(pair_index * 2) % len(SOFT_M3_PALETTES)]
    right_palette = SOFT_M3_PALETTES[(pair_index * 2 + 1) % len(SOFT_M3_PALETTES)]
    draw_m3_copy(left_card, left_spec, left_copy, {"accent": left_palette[1]}, 0, pair_index)
    draw_m3_copy(right_card, right_spec, right_copy, {"accent": right_palette[1]}, 1, pair_index)
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
    ("Screenshot_20261013_111815_LEVYRA.jpg", "INSTANT MATCH", "Find it instantly.",
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
    hero_w, hero_h = 2139, 850
    background_path = os.path.join(OUT_SHOWCASE_DIR, "levyra_dance_monochrome_bg.webp")
    if not os.path.exists(background_path):
        raise FileNotFoundError("Missing monochrome Levyra dance background")
    with Image.open(background_path) as background_source:
        background = background_source.convert("RGBA")
        scaled_h = round(background.height * hero_w / background.width)
        background = background.resize((hero_w, scaled_h), Image.Resampling.LANCZOS)
        background = ImageEnhance.Brightness(background.convert("RGB")).enhance(1.11)
        background = ImageEnhance.Contrast(background).enhance(0.96).convert("RGBA")
    hero = Image.new("RGBA", (hero_w, hero_h), (8, 8, 10, 255))
    hero.paste(background, (0, 0))
    if scaled_h < hero_h:
        extension_h = hero_h - scaled_h
        floor_source_h = min(scaled_h, max(180, extension_h * 2))
        floor_extension = background.crop((0, scaled_h - floor_source_h, hero_w, scaled_h))
        floor_extension = floor_extension.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
        floor_extension = floor_extension.resize((hero_w, extension_h), Image.Resampling.LANCZOS)
        floor_extension = ImageEnhance.Brightness(floor_extension).enhance(0.82)
        floor_extension = floor_extension.filter(ImageFilter.GaussianBlur(1.2))
        hero.paste(floor_extension, (0, scaled_h))
    overlay = Image.new("RGBA", (hero_w, hero_h), (0, 0, 0, 8))
    hero.alpha_composite(overlay)

    lockup_backdrop = Image.new("RGBA", (hero_w, hero_h), (0, 0, 0, 0))
    backdrop_draw = ImageDraw.Draw(lockup_backdrop)
    backdrop_draw.ellipse((350, 35, 1110, 680), fill=(0, 0, 0, 25))
    lockup_backdrop = lockup_backdrop.filter(ImageFilter.GaussianBlur(115))
    hero.alpha_composite(lockup_backdrop)

    tonal_glow = Image.new("RGBA", (hero_w, hero_h), (0, 0, 0, 0))
    tonal_draw = ImageDraw.Draw(tonal_glow)
    tonal_draw.ellipse((390, 25, 1080, 625), fill=(103, 80, 164, 28))
    tonal_draw.ellipse((445, 445, 1050, 860), fill=(52, 183, 224, 20))
    tonal_draw.ellipse((530, 460, 1145, 880), fill=(226, 42, 154, 18))
    tonal_glow = tonal_glow.filter(ImageFilter.GaussianBlur(105))
    hero.alpha_composite(tonal_glow)
    draw = ImageDraw.Draw(hero)

    logo_path = resolve_screenshot_path("Logo LEVYRA DeepSound in Neon Ribbon.png")
    if not logo_path:
        raise FileNotFoundError("Missing Levyra logo asset")
    with Image.open(logo_path) as logo_source:
        logo = logo_source.convert("RGBA")
        alpha_bbox = logo.getchannel("A").getbbox()
        if alpha_bbox:
            logo = logo.crop(alpha_bbox)
        logo.thumbnail((490, 400), Image.Resampling.LANCZOS)

    lockup_center_x = 735
    logo_x = int(lockup_center_x - logo.width / 2)
    hero.paste(logo, (logo_x, 85), logo)

    tagline_font = get_font(44)
    tagline_lines = ("All the ways you love music.", "All in one place.")
    for line_index, line in enumerate(tagline_lines):
        line_width = draw.textlength(line, font=tagline_font)
        draw.text(
            (lockup_center_x - line_width / 2, 500 + line_index * 42),
            line,
            font=tagline_font,
            fill=(240, 240, 242, 242),
        )

    out_path = os.path.join(OUT_SHOWCASE_DIR, "00_levyra_hero_dance_studio.webp")
    hero.convert("RGB").save(out_path, "WEBP", quality=95, method=6)
    print(f"Generated Monochrome Editorial Hero at ({hero_w}, {hero_h}): {out_path}")

def generate_feature_cards():
    print("Generating Material 3 Expressive editorial-stage pairs...")
    specs = M3_HERO_SPECS + M3_SHOWCASE_SPECS
    for pair_index in range(0, len(specs), 2):
        left_spec = specs[pair_index]
        right_spec = specs[pair_index + 1]
        left_card, right_card = render_m3_editorial_pair(left_spec, right_spec, pair_index // 2)
        for card, spec in ((left_card, left_spec), (right_card, right_spec)):
            card_rgb = card.convert("RGB")
            for filename in spec[5]:
                out_file = os.path.join(OUT_CARDS_DIR, filename)
                card_rgb.save(out_file, "WEBP", quality=95, method=6)
                print(f"Saved Material 3 Expressive editorial card: {out_file}")

README_CARD_DIR = os.path.join(OUT_SHOWCASE_DIR, "editorial-cards")

README_CARD_THEMES = [
    {
        "background": (28, 20, 32),
        "ink": (255, 247, 252),
        "muted": (214, 196, 210),
        "accent": (255, 142, 133),
        "shape": (79, 54, 83),
    },
    {
        "background": (238, 248, 242),
        "ink": (27, 35, 31),
        "muted": (77, 88, 82),
        "accent": (17, 102, 92),
        "shape": (190, 229, 213),
    },
    {
        "background": (255, 244, 235),
        "ink": (46, 31, 25),
        "muted": (98, 76, 67),
        "accent": (176, 66, 74),
        "shape": (246, 207, 190),
    },
    {
        "background": (238, 240, 255),
        "ink": (31, 32, 52),
        "muted": (76, 77, 101),
        "accent": (75, 65, 161),
        "shape": (205, 210, 248),
    },
]

README_CARD_SPECS = [
    ("01_pure_listening", "RADIO", "Pure\nlistening.", "Radio that starts with your sound.", "Screenshot_20260926_171253_LEVYRA.jpg"),
    ("02_stay_with_the_song", "NOW PLAYING", "Stay with\nthe song.", "Immersive playback, no visual noise.", "Screenshot_20260926_193948_LEVYRA.jpg"),
    ("03_sing_along", "SYNCED LYRICS", "Sing\nalong.", "Translation and timing in one flow.", "screen-lyrics.jpg"),
    ("04_open_the_album", "OFFLINE", "Open the\nwhole album.", "Keep the music you come back to.", "Screenshot_20260926_194706_LEVYRA.jpg"),
    ("05_fresh_currents", "DISCOVERY", "Fresh\ncurrents.", "New releases without the scroll fatigue.", "Screenshot_20261010_132210_LEVYRA.jpg"),
    ("06_built_for_your_taste", "LEVYRA MIX", "Built for\nyour taste.", "A mix shaped around the moment.", "Screenshot_20261015_144159_LEVYRA.jpg"),
    ("07_every_genre", "MOODS", "Every\ngenre.", "Move by mood, place or energy.", "Screenshot_20261015_140910_LEVYRA.jpg"),
    ("08_acoustic_precision", "AUDIO", "Acoustic\nprecision.", "Tune the sound, keep the soul.", "Screenshot_20261010_190726_LEVYRA.jpg"),
    ("09_keep_it_personal", "LIBRARY", "Keep it\npersonal.", "Your collection stays yours.", "Screenshot_20261010_132050_LEVYRA.jpg"),
    ("10_curated_for_you", "COLLECTIONS", "Curated\nfor you.", "Quick picks, ready when you are.", "Screenshot_20261010_132136_LEVYRA.jpg"),
    ("11_your_rhythm", "INSIGHTS", "Your\nrhythm.", "See the pattern behind the plays.", "Screenshot_20260927_131943_LEVYRA.jpg"),
    ("12_in_your_orbit", "HOME", "In your\norbit.", "Artists and albums close at hand.", "Screenshot_20261010_132103_LEVYRA.jpg"),
    ("13_tailor_every_detail", "SETTINGS", "Tailor every\ndetail.", "Private by design, flexible by choice.", "Screenshot_20261010_140700_LEVYRA.jpg"),
    ("14_fresh_off_the_stage", "NEW RELEASES", "Fresh off\nthe stage.", "The newest music, without the clutter.", "Screenshot_20261010_144144_LEVYRA.jpg"),
    ("15_meet_the_artist", "ARTISTS", "Meet the\nartist.", "From the profile straight into the music.", "Screenshot_20261010_143646_LEVYRA.jpg"),
    ("16_listen_together", "LEVYRA JAM", "Listen\ntogether.", "One room. One queue. Everyone in.", "Screenshot_20261011_133434_LEVYRA.jpg"),
]

def make_campaign_device(filename, width, border_color):
    screenshot_path = resolve_screenshot_path(filename)
    if not screenshot_path:
        raise FileNotFoundError(f"Missing campaign screenshot: {filename}")
    with Image.open(screenshot_path) as screenshot_source:
        screenshot = enhance_screenshot(screenshot_source).convert("RGBA")

    outer_w = width
    outer_h = round(width * 2.13)
    frame = max(10, round(width * 0.019))
    pad = max(12, round(width * 0.028))
    radius = max(38, round(width * 0.086))
    device = Image.new("RGBA", (outer_w + pad * 2, outer_h + pad * 2), (0, 0, 0, 0))
    draw = ImageDraw.Draw(device)
    body = (pad, pad, pad + outer_w - 1, pad + outer_h - 1)

    draw.rounded_rectangle(
        body,
        radius=radius,
        fill=(20, 21, 25, 255),
        outline=(142, 143, 150, 255),
        width=max(3, round(width * 0.007)),
    )
    inset = max(3, round(width * 0.006))
    draw.rounded_rectangle(
        (body[0] + inset, body[1] + inset, body[2] - inset, body[3] - inset),
        radius=radius - inset,
        outline=(57, 59, 66, 255),
        width=max(2, round(width * 0.004)),
    )

    screen_w = outer_w - frame * 2
    screen_h = outer_h - frame * 2
    screen = crop_to_fill(screenshot, screen_w, screen_h)
    screen_mask = Image.new("L", (screen_w, screen_h), 0)
    screen_radius = max(30, radius - frame)
    ImageDraw.Draw(screen_mask).rounded_rectangle(
        (0, 0, screen_w - 1, screen_h - 1),
        radius=screen_radius,
        fill=255,
    )
    screen_x = pad + frame
    screen_y = pad + frame
    device.paste(screen, (screen_x, screen_y), screen_mask)
    draw.rounded_rectangle(
        (screen_x, screen_y, screen_x + screen_w - 1, screen_y + screen_h - 1),
        radius=screen_radius,
        outline=(5, 6, 9, 255),
        width=max(3, round(width * 0.006)),
    )

    camera_x = pad + outer_w // 2
    camera_y = screen_y + max(14, round(width * 0.035))
    camera_radius = max(7, round(width * 0.014))
    draw.ellipse(
        (camera_x - camera_radius, camera_y - camera_radius,
         camera_x + camera_radius, camera_y + camera_radius),
        fill=(3, 4, 7, 255),
    )
    draw.ellipse(
        (camera_x - camera_radius // 2, camera_y - camera_radius // 2,
         camera_x + camera_radius // 2, camera_y + camera_radius // 2),
        fill=(24, 43, 65, 230),
    )
    speaker_half = max(18, round(width * 0.042))
    draw.rounded_rectangle(
        (camera_x - speaker_half, pad + 4, camera_x + speaker_half, pad + 7),
        radius=2,
        fill=(68, 69, 75, 255),
    )

    button_w = max(4, round(width * 0.008))
    button_x = pad + outer_w - 1
    draw.rounded_rectangle(
        (button_x, pad + round(outer_h * 0.20), button_x + button_w, pad + round(outer_h * 0.31)),
        radius=button_w // 2,
        fill=(91, 93, 101, 255),
    )
    draw.rounded_rectangle(
        (button_x, pad + round(outer_h * 0.35), button_x + button_w, pad + round(outer_h * 0.44)),
        radius=button_w // 2,
        fill=(91, 93, 101, 255),
    )
    return device

def paste_campaign_device(canvas, filename, width, horizontal_center, top, angle, border_color):
    device = make_campaign_device(filename, width, border_color)
    device = device.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
    shadow_alpha = device.getchannel("A").filter(ImageFilter.GaussianBlur(max(18, width // 18)))
    shadow_alpha = ImageEnhance.Brightness(shadow_alpha).enhance(0.42)
    shadow = Image.new("RGBA", device.size, (18, 12, 22, 0))
    shadow.putalpha(shadow_alpha)
    x = round(horizontal_center - device.width / 2)
    y = round(top)
    canvas.alpha_composite(shadow, (x + 20, y + 34))
    canvas.alpha_composite(device, (x, y))

def paste_campaign_ribbon(canvas, width, center, opacity, angle=0):
    ribbon_path = os.path.join(OUT_SHOWCASE_DIR, "levyra_editorial_ribbon.png")
    if not os.path.exists(ribbon_path):
        raise FileNotFoundError(f"Missing editorial ribbon: {ribbon_path}")
    with Image.open(ribbon_path) as ribbon_source:
        ribbon = ribbon_source.convert("RGBA")
    height = round(ribbon.height * width / ribbon.width)
    ribbon = ribbon.resize((width, height), Image.Resampling.LANCZOS)
    alpha = ribbon.getchannel("A").point(lambda value: round(value * opacity))
    ribbon.putalpha(alpha)
    if angle:
        ribbon = ribbon.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
    x = round(center[0] - ribbon.width / 2)
    y = round(center[1] - ribbon.height / 2)
    canvas.alpha_composite(ribbon, (x, y))

def draw_readme_card_copy(draw, spec, theme, index, origin):
    _, kicker, title, body, _ = spec
    x, y = origin
    kicker_font = get_font(25, bold=True)
    number_font = get_font(25, bold=True)
    title_font = get_font(76, bold=True)
    body_font = get_font(27)
    draw.text((x, y), kicker, font=kicker_font, fill=theme["accent"])
    number = f"{index + 1:02d}"
    number_width = draw.textlength(number, font=number_font)
    draw.text((820 - number_width, y), number, font=number_font, fill=theme["muted"])
    title_y = y + 55
    draw.multiline_text((x, title_y), title, font=title_font, fill=theme["ink"], spacing=-8)
    title_box = draw.multiline_textbbox((x, title_y), title, font=title_font, spacing=-8)
    body_y = title_box[3] + 24
    draw.text((x, body_y), body, font=body_font, fill=theme["muted"])
    return draw.textbbox((x, body_y), body, font=body_font)[3]

def draw_readme_card_shapes(draw, theme, variant):
    if variant == 0:
        draw.ellipse((-250, 900, 650, 1780), fill=theme["shape"] + (255,))
        draw.rounded_rectangle((610, -150, 1030, 310), radius=150, fill=theme["shape"] + (210,))
    elif variant == 1:
        draw.ellipse((480, 870, 1130, 1540), fill=theme["shape"] + (255,))
        draw.rounded_rectangle((-180, -120, 390, 250), radius=140, fill=theme["shape"] + (220,))
    elif variant == 2:
        draw.rounded_rectangle((-220, 1040, 1080, 1780), radius=330, fill=theme["shape"] + (255,))
        draw.ellipse((650, -180, 1060, 230), fill=theme["shape"] + (210,))
    else:
        draw.ellipse((-240, 900, 510, 1760), fill=theme["shape"] + (255,))
        draw.ellipse((580, -190, 1100, 330), fill=theme["shape"] + (190,))

def render_readme_card(spec, index):
    theme_order = [0, 1, 2, 3, 3, 0, 1, 2, 1, 2, 3, 0, 2, 3, 0, 1]
    variant_order = [0, 1, 2, 3, 2, 3, 0, 1, 1, 0, 3, 2, 3, 2, 1, 0]
    theme = README_CARD_THEMES[theme_order[index]]
    variant = variant_order[index]
    canvas = Image.new("RGBA", (900, 1800), theme["background"] + (255,))
    draw = ImageDraw.Draw(canvas)
    draw_readme_card_shapes(draw, theme, variant)
    ribbon_centers = [(705, 920), (190, 930), (700, 1090), (170, 1030)]
    ribbon_angles = [-12, 13, -5, 18]
    paste_campaign_ribbon(canvas, 780, ribbon_centers[variant], 0.11, ribbon_angles[variant])
    copy_bottom = draw_readme_card_copy(draw, spec, theme, index, (72, 70))

    _, _, _, _, screenshot = spec
    device_centers = [585, 445, 585, 445]
    angles = [-2.4, 2.0, -1.7, 2.4]
    device_top = copy_bottom + 48
    available_height = 1750 - device_top
    device_width = min(640, max(560, int(available_height / 2.24)))
    paste_campaign_device(
        canvas,
        screenshot,
        device_width,
        device_centers[variant],
        device_top,
        angles[variant],
        (22, 20, 24),
    )
    return canvas.convert("RGB")

def generate_readme_cards():
    os.makedirs(README_CARD_DIR, exist_ok=True)
    for index, spec in enumerate(README_CARD_SPECS):
        artwork = render_readme_card(spec, index)
        out_path = os.path.join(README_CARD_DIR, spec[0] + ".webp")
        artwork.save(out_path, "WEBP", quality=94, method=6)
        print(f"Saved README editorial card at {artwork.size}: {out_path}")

if __name__ == "__main__":
    generate_hero_panoramic_showcase()
    generate_feature_cards()
    generate_readme_cards()
    print("All Material 3 Expressive editorial-stage assets generated successfully!")
