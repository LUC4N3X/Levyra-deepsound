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
        os.path.join(r"C:\Users\Luca Drogo\Desktop\Nuova cartella", filename),
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
    "Screenshot_20261020_142741_LEVYRA.jpg": "Listening insights that stay on-device.",
    "Screenshot_20261024_210317_Levyra.jpg": "Listening insights that stay on-device.",
    "Screenshot_20261024_210532_Levyra.jpg": "Handpicked collections and gems revolving around what you love.",
    "Screenshot_20261010_144048_LEVYRA.jpg": "Essential sets for every artist.",
    "Screenshot_20261010_140700_LEVYRA.jpg": "Design, audio, gestures, and local backups.",
    "Screenshot_20261015_205614_LEVYRA.jpg": "Theme, home, player, and audio in one place.",
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
    frame = 9
    outer_w, outer_h = size
    inner_w, inner_h = outer_w - frame * 2, outer_h - frame * 2
    phone = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(phone)
    radius = 18
    screen_radius = 12
    draw.rounded_rectangle(
        (0, 0, outer_w - 1, outer_h - 1),
        radius=radius,
        fill=(25, 27, 33, 255),
        outline=(185, 190, 202, 255),
        width=2,
    )
    screenshot = crop_to_fill(source, inner_w, inner_h)
    mask = Image.new("L", (inner_w, inner_h), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle((0, 0, inner_w - 1, inner_h - 1), radius=screen_radius, fill=255)
    phone.paste(screenshot, (frame, frame), mask)
    draw.rounded_rectangle(
        (frame, frame, outer_w - frame - 1, outer_h - frame - 1),
        radius=screen_radius,
        outline=(10, 11, 14, 255),
        width=2,
    )
    camera_radius = 7
    camera_x = outer_w // 2
    camera_y = frame + 14
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
        (camera_x - 3, camera_y - 3, camera_x + 1, camera_y + 1),
        fill=(32, 54, 85, 220),
    )
    draw.rounded_rectangle(
        (outer_w // 2 - 28, 2, outer_w // 2 + 28, 5),
        radius=1,
        fill=(65, 68, 76, 255),
    )
    draw.line((outer_w - 2, 285, outer_w - 2, 395), fill=(115, 120, 130, 255), width=3)
    draw.line((outer_w - 2, 435, outer_w - 2, 525), fill=(115, 120, 130, 255), width=3)
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
    ("Screenshot_20261024_210532_Levyra.jpg", "CURATED PLAYLISTS", "Curated for you.",
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
    ("Screenshot_20261024_210317_Levyra.jpg", "ON-DEVICE STATS", "Keep it personal.",
     "Private listening stats and charts,\ncomputed strictly on your device.",
     (110, 55, 180), ["14_listening_pulse.webp"]),

    # Card 15: Artist Playlists
    ("Screenshot_20261010_144048_LEVYRA.jpg", "ESSENTIAL SETS", "Artist playlists.",
     "Curated sets, tours, and the\nessential catalog of every artist.",
     (100, 50, 190), ["15_artist_playlists.webp"]),

    # Card 16: Settings
    ("Screenshot_20261015_205614_LEVYRA.jpg", "SETTINGS STUDIO", "Make Levyra yours.",
     "Theme, home, player, and audio—\nall tuned from one place.",
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
CARD_W, CARD_H = 900, 1800
GAP = 32

README_CARD_SPECS = [
    # Row 1: Flagship Experience & Dual Theme Cut
    (
        "01_pure_listening",
        "Pure Listening",
        "Smart radio crafted around your sound.\nZero ads, zero interruptions.",
        "Screenshot_20261016_223653_LEVYRA.jpg",
        (240, 244, 255),
        (224, 231, 255),
        (99, 102, 241),
        (199, 210, 254),
        (30, 27, 75),
        (79, 70, 229),
    ),
    (
        "02_stay_with_the_song",
        "Stay with the Song",
        "Immersive fullscreen playback\nwith live synced lyrics.",
        "Screenshot_20261016_135636_LEVYRA.jpg",
        (255, 241, 242),
        (254, 226, 226),
        (244, 63, 94),
        (254, 205, 211),
        (76, 5, 25),
        (159, 18, 57),
    ),
    (
        "03_daylight_expressive",
        "Daylight Expressive",
        "Every curve and color tuned for bright daylight clarity",
        "Screenshot_20261024_204745_Levyra.jpg",
        (255, 251, 235),
        (254, 240, 138),
        (234, 179, 8),
        (253, 224, 71),
        (66, 32, 6),
        (161, 98, 7),
    ),
    (
        "04_midnight_obsidian",
        "Midnight Obsidian",
        "Deep obsidian surfaces with vibrant high-contrast accents",
        "Screenshot_20261024_205815_Levyra.jpg",
        (241, 245, 249),
        (203, 213, 225),
        (56, 128, 158),
        (186, 230, 253),
        (15, 23, 42),
        (12, 74, 96),
    ),

    # Row 2: Discovery & Exploration
    (
        "05_fresh_currents",
        "Instant Search",
        "Find any track, album or artist in real time",
        "Screenshot_20261024_144737_Levyra.jpg",
        (240, 244, 255),
        (224, 231, 255),
        (59, 130, 246),
        (191, 219, 254),
        (30, 58, 138),
        (37, 99, 235),
    ),
    (
        "06_built_for_your_taste",
        "Player Styles",
        "Choose from multiple player layouts, controls and stream modes",
        "Screenshot_20261024_203401_Levyra.jpg",
        (254, 250, 242),
        (250, 240, 222),
        (217, 119, 6),
        (245, 222, 179),
        (69, 45, 12),
        (180, 83, 9),
    ),
    (
        "07_every_genre",
        "Every Mood and Genre",
        "Browse by energy, place or atmosphere in one tap",
        "Screenshot_20261015_140910_LEVYRA.jpg",
        (240, 253, 250),
        (204, 251, 241),
        (20, 184, 166),
        (153, 246, 228),
        (19, 78, 74),
        (13, 148, 136),
    ),
    (
        "08_acoustic_precision",
        "Acoustic Precision",
        "High-fidelity 320 kbps streaming with lossless output",
        "Screenshot_20261024_150113_Levyra.jpg",
        (239, 246, 255),
        (219, 234, 254),
        (59, 130, 246),
        (147, 197, 253),
        (30, 58, 138),
        (37, 99, 235),
    ),

    # Row 3: Library & Insights
    (
        "09_keep_it_personal",
        "Unified Library",
        "Streamed tracks and local files together in one home",
        "Screenshot_20261020_142741_LEVYRA.jpg",
        (239, 246, 255),
        (219, 234, 254),
        (59, 130, 246),
        (191, 219, 254),
        (30, 58, 138),
        (29, 78, 216),
    ),
    (
        "10_curated_for_you",
        "Curated Search",
        "Artist shortcuts, instant play, and category filters",
        "Screenshot_20261024_193919_Levyra.jpg",
        (255, 247, 237),
        (254, 237, 213),
        (249, 115, 22),
        (254, 215, 170),
        (67, 20, 7),
        (194, 65, 12),
    ),
    (
        "11_your_rhythm",
        "Listening Insights",
        "Personal stats, listening habits and receipt computed on device",
        "Screenshot_20261024_210317_Levyra.jpg",
        (240, 249, 255),
        (224, 242, 254),
        (14, 165, 233),
        (186, 230, 253),
        (12, 74, 96),
        (2, 132, 199),
    ),
    (
        "12_in_your_orbit",
        "Levyra Collections",
        "Handpicked playlists and gems revolving around what you love",
        "Screenshot_20261024_210532_Levyra.jpg",
        (245, 243, 255),
        (237, 233, 254),
        (139, 92, 246),
        (221, 214, 254),
        (46, 16, 101),
        (109, 40, 217),
    ),

    # Row 4: Personalization & Artists
    (
        "13_tailor_every_detail",
        "Skin Studio",
        "Seven complete looks. Switch layout in one tap",
        "Screenshot_20261023_212906_LEVYRA.jpg",
        (245, 243, 255),
        (237, 233, 254),
        (139, 92, 246),
        (221, 214, 254),
        (46, 16, 101),
        (109, 40, 217),
    ),
    (
        "14_fresh_off_the_stage",
        "Live Radio",
        "Thousands of live stations from around the globe",
        "Screenshot_20261024_145819_Levyra.jpg",
        (254, 242, 242),
        (254, 226, 226),
        (239, 68, 68),
        (254, 202, 202),
        (69, 10, 10),
        (185, 28, 28),
    ),
    (
        "15_meet_the_artist",
        "Meet the Artist",
        "Artist biographies, monthly audience, and complete discography",
        "Screenshot_20261024_145036_Levyra.jpg",
        (245, 243, 255),
        (237, 233, 254),
        (139, 92, 246),
        (221, 214, 254),
        (46, 16, 101),
        (109, 40, 217),
    ),
    (
        "16_listen_together",
        "Levyra Jam",
        "Real-time collaborative listening queue with friends",
        "Screenshot_20261011_133434_LEVYRA.jpg",
        (236, 253, 245),
        (209, 250, 229),
        (16, 185, 129),
        (167, 243, 208),
        (6, 78, 59),
        (5, 150, 105),
    ),

    # Row 5: Interactive Discovery & Personal Highlights
    (
        "17_sing_along",
        "Synced Lyrics",
        "Real-time lyrics with tap-to-seek and translation",
        "screen-lyrics.jpg",
        (255, 251, 235),
        (254, 240, 138),
        (245, 158, 11),
        (253, 224, 71),
        (69, 26, 3),
        (180, 83, 9),
    ),
    (
        "18_open_the_album",
        "Offline Music",
        "Download full albums and singles directly to device storage",
        "Screenshot_20261024_193647_Levyra.jpg",
        (248, 250, 252),
        (226, 232, 240),
        (100, 116, 139),
        (203, 213, 225),
        (15, 23, 42),
        (71, 85, 105),
    ),
    (
        "19_discover_for_you",
        "Discover for You",
        "Swipe right to save, swipe left to discover next",
        "Screenshot_20261024_195654_Levyra.jpg",
        (254, 250, 242),
        (250, 240, 222),
        (180, 130, 45),
        (245, 222, 179),
        (69, 45, 12),
        (150, 95, 20),
    ),
    (
        "20_your_year_in_music",
        "Your Year in Music",
        "A personal recap of your year, computed right on device",
        "Screenshot_20261016_223757_LEVYRA.jpg",
        (253, 244, 255),
        (250, 232, 255),
        (192, 38, 211),
        (240, 171, 252),
        (74, 4, 78),
        (162, 28, 175),
    ),

    # Row 6: Global Streaming Skins (Dual Dynamic Diptych Cuts)
    (
        "21_spotify_style",
        "Spotify Style",
        "Familiar dark surfaces, green accents and artist glow",
        "Screenshot_20261024_201452_Levyra.jpg",
        (236, 253, 245),
        (209, 250, 229),
        (30, 215, 96),
        (167, 243, 208),
        (6, 78, 59),
        (22, 101, 52),
    ),
    (
        "22_apple_music_style",
        "Apple Music Style",
        "Editorial white surfaces, album shelves and bold type",
        "Screenshot_20261024_201549_Levyra.jpg",
        (255, 241, 242),
        (255, 228, 230),
        (250, 45, 72),
        (254, 205, 211),
        (76, 5, 25),
        (190, 18, 60),
    ),
    (
        "23_youtube_music_style",
        "YouTube Music Style",
        "Dynamic chip filters, charts and pure black backdrop",
        "Screenshot_20261024_201709_Levyra.jpg",
        (254, 242, 242),
        (254, 226, 226),
        (255, 0, 0),
        (254, 202, 202),
        (69, 10, 10),
        (185, 28, 28),
    ),
    (
        "24_signature_looks",
        "Signature Looks",
        "From retro hi-fi warmth to modern tactile glass",
        "Screenshot_20261024_201753_Levyra.jpg",
        (250, 245, 255),
        (243, 232, 255),
        (168, 85, 247),
        (233, 213, 255),
        (59, 7, 100),
        (126, 34, 206),
    ),]

def make_campaign_device(filename, width):
    screenshot_path = resolve_screenshot_path(filename)
    if not screenshot_path:
        raise FileNotFoundError(f"Missing campaign screenshot: {filename}")
    with Image.open(screenshot_path) as screenshot_source:
        screenshot = enhance_screenshot(screenshot_source).convert("RGBA")

    outer_width = width
    outer_height = round(width * 2.10)
    padding = max(16, round(width * 0.025))
    frame = max(8, round(width * 0.013))
    # S25 Ultra signature softened ergonomic corners
    radius = max(17, round(width * 0.027))
    screen_radius = max(11, round(width * 0.018))

    device = Image.new(
        "RGBA",
        (outer_width + padding * 2, outer_height + padding * 2),
        (0, 0, 0, 0),
    )
    draw = ImageDraw.Draw(device)
    body = (padding, padding, padding + outer_width - 1, padding + outer_height - 1)

    # 1. Titanium Frame Base (Sleek dark titanium)
    draw.rounded_rectangle(
        body,
        radius=radius,
        fill=(25, 27, 33, 255),
    )

    # 2. Outer Chamfer / Metallic Titanium Rim
    draw.rounded_rectangle(
        body,
        radius=radius,
        outline=(185, 190, 202, 255),
        width=max(2, round(width * 0.0035)),
    )

    # 3. Inner frame depth
    inner_edge = max(2, round(width * 0.003))
    inner_body = (
        body[0] + inner_edge,
        body[1] + inner_edge,
        body[2] - inner_edge,
        body[3] - inner_edge,
    )
    draw.rounded_rectangle(
        inner_body,
        radius=radius - inner_edge,
        outline=(48, 51, 60, 255),
        width=1,
    )

    # 4. Screen
    screen_width = outer_width - frame * 2
    screen_height = outer_height - frame * 2
    screen = crop_to_fill(screenshot, screen_width, screen_height)
    screen_mask = Image.new("L", (screen_width, screen_height), 0)
    ImageDraw.Draw(screen_mask).rounded_rectangle(
        (0, 0, screen_width - 1, screen_height - 1),
        radius=screen_radius,
        fill=255,
    )
    screen_x = padding + frame
    screen_y = padding + frame
    device.paste(screen, (screen_x, screen_y), screen_mask)

    # 5. Screen inner hairline border
    draw.rounded_rectangle(
        (screen_x, screen_y, screen_x + screen_width - 1, screen_y + screen_height - 1),
        radius=screen_radius,
        outline=(10, 11, 14, 255),
        width=max(2, round(width * 0.003)),
    )

    # 6. S25 Ultra Infinity-O punch-hole selfie camera
    camera_x = padding + outer_width // 2
    camera_y = screen_y + max(13, round(width * 0.022))
    camera_radius = max(6, round(width * 0.010))
    draw.ellipse(
        (
            camera_x - camera_radius,
            camera_y - camera_radius,
            camera_x + camera_radius,
            camera_y + camera_radius,
        ),
        fill=(4, 5, 8, 255),
    )
    glint = max(2, round(camera_radius * 0.38))
    draw.ellipse(
        (camera_x - glint, camera_y - glint, camera_x + 1, camera_y + 1),
        fill=(32, 54, 85, 220),
    )

    # 7. Micro-slit earpiece speaker at top rim
    speaker_half = max(26, round(width * 0.055))
    draw.rounded_rectangle(
        (camera_x - speaker_half, padding + 2, camera_x + speaker_half, padding + 5),
        radius=1,
        fill=(65, 68, 76, 255),
    )

    # 8. Titanium buttons (Right side: Volume rocker and Power key)
    button_width = max(3, round(width * 0.006))
    button_x = padding + outer_width - 1
    v_top = padding + round(outer_height * 0.22)
    v_bot = padding + round(outer_height * 0.33)
    draw.rounded_rectangle(
        (button_x, v_top, button_x + button_width, v_bot),
        radius=button_width // 2,
        fill=(115, 120, 130, 255),
    )
    p_top = padding + round(outer_height * 0.38)
    p_bot = padding + round(outer_height * 0.45)
    draw.rounded_rectangle(
        (button_x, p_top, button_x + button_width, p_bot),
        radius=button_width // 2,
        fill=(115, 120, 130, 255),
    )
    return device

def create_readme_card_background(w, h, top_rgb, bot_rgb, bloom_rgb, bloom_opacity=110):
    base = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(base)
    for y in range(h):
        ratio = y / h
        r = int(top_rgb[0] + (bot_rgb[0] - top_rgb[0]) * ratio)
        g = int(top_rgb[1] + (bot_rgb[1] - top_rgb[1]) * ratio)
        b = int(top_rgb[2] + (bot_rgb[2] - top_rgb[2]) * ratio)
        draw.line([(0, y), (w, y)], fill=(r, g, b, 255))

    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    cx, cy = w // 2, int(h * 0.60)
    rx, ry = int(w * 0.55), int(h * 0.40)
    glow_draw.ellipse(
        (cx - rx, cy - ry, cx + rx, cy + ry),
        fill=bloom_rgb + (bloom_opacity,),
    )
    glow = glow.filter(ImageFilter.GaussianBlur(150))
    base.alpha_composite(glow)
    return base

def draw_readme_card_frame(canvas, border_rgb, radius=44):
    w, h = canvas.size
    border = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(border)
    draw.rounded_rectangle(
        (0, 0, w - 1, h - 1),
        radius=radius,
        outline=border_rgb + (255,),
        width=2,
    )
    canvas.alpha_composite(border)

def render_readme_hero_diptych(left_spec, right_spec):
    pair_w = CARD_W * 2 + GAP
    canvas_0 = create_readme_card_background(CARD_W, CARD_H, left_spec[4], left_spec[5], left_spec[6], bloom_opacity=110)
    canvas_1 = create_readme_card_background(CARD_W, CARD_H, right_spec[4], right_spec[5], right_spec[6], bloom_opacity=110)

    pair_canvas = Image.new("RGBA", (pair_w, CARD_H), (0, 0, 0, 0))
    pair_canvas.paste(canvas_0, (0, 0))
    pair_canvas.paste(canvas_1, (CARD_W + GAP, 0))

    # Left phone: Radio screen, width=720, angle=13, top_left=(220, -150)
    left_dev = make_campaign_device(left_spec[3], width=720)
    rot_left = left_dev.rotate(13, resample=Image.Resampling.BICUBIC, expand=True)
    shadow_left = Image.new("RGBA", rot_left.size, (15, 23, 42, 0))
    shadow_left_alpha = rot_left.getchannel("A").filter(ImageFilter.GaussianBlur(48))
    shadow_left_alpha = ImageEnhance.Brightness(shadow_left_alpha).enhance(0.35)
    shadow_left.putalpha(shadow_left_alpha)
    pair_canvas.alpha_composite(shadow_left, (220 + 24, -150 + 42))
    pair_canvas.alpha_composite(rot_left, (220, -150))

    # Right phone: Now Playing screen, width=720, angle=-8.5, top_left=(CARD_W + GAP + 200, 370)
    right_dev = make_campaign_device(right_spec[3], width=720)
    rot_right = right_dev.rotate(-8.5, resample=Image.Resampling.BICUBIC, expand=True)
    shadow_right = Image.new("RGBA", rot_right.size, (15, 23, 42, 0))
    shadow_right_alpha = rot_right.getchannel("A").filter(ImageFilter.GaussianBlur(48))
    shadow_right_alpha = ImageEnhance.Brightness(shadow_right_alpha).enhance(0.35)
    shadow_right.putalpha(shadow_right_alpha)
    pair_canvas.alpha_composite(shadow_right, (CARD_W + GAP + 200 + 24, 370 + 42))
    pair_canvas.alpha_composite(rot_right, (CARD_W + GAP + 200, 370))

    left_card = pair_canvas.crop((0, 0, CARD_W, CARD_H))
    right_card = pair_canvas.crop((CARD_W + GAP, 0, pair_w, CARD_H))

    draw_left = ImageDraw.Draw(left_card)
    title_font = get_font(56, bold=True)
    sub_font = get_font(28, bold=False)

    # Left card text (bottom-left)
    draw_left.text((72, 1420), left_spec[1], font=title_font, fill=left_spec[8] + (255,))
    draw_left.text((72, 1420 + 75), left_spec[2], font=sub_font, fill=left_spec[9] + (255,), spacing=8)

    # Right card text (top-right)
    draw_right = ImageDraw.Draw(right_card)
    title_right = right_spec[1]
    sub_right = right_spec[2]
    t_bbox = draw_right.textbbox((0, 0), title_right, font=title_font)
    t_w = t_bbox[2] - t_bbox[0]
    draw_right.text((CARD_W - 72 - t_w, 100), title_right, font=title_font, fill=right_spec[8] + (255,))
    for i, line in enumerate(sub_right.split("\n")):
        s_bbox = draw_right.textbbox((0, 0), line, font=sub_font)
        s_w = s_bbox[2] - s_bbox[0]
        draw_right.text((CARD_W - 72 - s_w, 100 + 75 + i * 38), line, font=sub_font, fill=right_spec[9] + (255,))

    mask = Image.new("L", (CARD_W, CARD_H), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, CARD_W - 1, CARD_H - 1), radius=44, fill=255)

    draw_readme_card_frame(left_card, left_spec[7])
    draw_readme_card_frame(right_card, right_spec[7])

    left_card.putalpha(mask)
    right_card.putalpha(mask)
    return left_card, right_card

def render_readme_theme_diptych(left_spec, right_spec):
    pair_w = CARD_W * 2 + GAP
    canvas_0 = create_readme_card_background(CARD_W, CARD_H, left_spec[4], left_spec[5], left_spec[6], bloom_opacity=110)
    canvas_1 = create_readme_card_background(CARD_W, CARD_H, right_spec[4], right_spec[5], right_spec[6], bloom_opacity=110)

    pair_canvas = Image.new("RGBA", (pair_w, CARD_H), (0, 0, 0, 0))
    pair_canvas.paste(canvas_0, (0, 0))
    pair_canvas.paste(canvas_1, (CARD_W + GAP, 0))

    # Left phone: Light theme, width=720, angle=-10.5, pos=(120, 310)
    left_dev = make_campaign_device(left_spec[3], width=720)
    rot_left = left_dev.rotate(-10.5, resample=Image.Resampling.BICUBIC, expand=True)
    shadow_left = Image.new("RGBA", rot_left.size, (15, 23, 42, 0))
    shadow_left_alpha = rot_left.getchannel("A").filter(ImageFilter.GaussianBlur(48))
    shadow_left_alpha = ImageEnhance.Brightness(shadow_left_alpha).enhance(0.35)
    shadow_left.putalpha(shadow_left_alpha)
    pair_canvas.alpha_composite(shadow_left, (120 + 24, 310 + 42))
    pair_canvas.alpha_composite(rot_left, (120, 310))

    # Right phone: Dark theme, width=720, angle=11.0, pos=(CARD_W + GAP - 30, 390)
    right_dev = make_campaign_device(right_spec[3], width=720)
    rot_right = right_dev.rotate(11.0, resample=Image.Resampling.BICUBIC, expand=True)
    shadow_right = Image.new("RGBA", rot_right.size, (15, 23, 42, 0))
    shadow_right_alpha = rot_right.getchannel("A").filter(ImageFilter.GaussianBlur(48))
    shadow_right_alpha = ImageEnhance.Brightness(shadow_right_alpha).enhance(0.40)
    shadow_right.putalpha(shadow_right_alpha)
    pair_canvas.alpha_composite(shadow_right, (CARD_W + GAP - 30 + 24, 390 + 42))
    pair_canvas.alpha_composite(rot_right, (CARD_W + GAP - 30, 390))

    left_card = pair_canvas.crop((0, 0, CARD_W, CARD_H))
    right_card = pair_canvas.crop((CARD_W + GAP, 0, pair_w, CARD_H))

    title_font = get_font(56, bold=True)
    sub_font = get_font(28, bold=False)

    # Left card text (top-left)
    draw_left = ImageDraw.Draw(left_card)
    draw_left.text((72, 100), left_spec[1], font=title_font, fill=left_spec[8] + (255,))
    draw_left.text((72, 100 + 75), left_spec[2], font=sub_font, fill=left_spec[9] + (255,), spacing=8)

    # Right card text (top-right)
    draw_right = ImageDraw.Draw(right_card)
    title_right = right_spec[1]
    sub_right = right_spec[2]
    t_bbox = draw_right.textbbox((0, 0), title_right, font=title_font)
    t_w = t_bbox[2] - t_bbox[0]
    draw_right.text((CARD_W - 72 - t_w, 100), title_right, font=title_font, fill=right_spec[8] + (255,))
    for i, line in enumerate(sub_right.split("\n")):
        s_bbox = draw_right.textbbox((0, 0), line, font=sub_font)
        s_w = s_bbox[2] - s_bbox[0]
        draw_right.text((CARD_W - 72 - s_w, 100 + 75 + i * 38), line, font=sub_font, fill=right_spec[9] + (255,))

    mask = Image.new("L", (CARD_W, CARD_H), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, CARD_W - 1, CARD_H - 1), radius=44, fill=255)

    draw_readme_card_frame(left_card, left_spec[7])
    draw_readme_card_frame(right_card, right_spec[7])

    left_card.putalpha(mask)
    right_card.putalpha(mask)
    return left_card, right_card

def render_readme_centered_card(spec):
    card_id, title, subtitle, screenshot_name, top_rgb, bot_rgb, bloom_rgb, border_rgb, ink_rgb, sub_rgb = spec
    canvas = create_readme_card_background(CARD_W, CARD_H, top_rgb, bot_rgb, bloom_rgb, bloom_opacity=110)
    draw = ImageDraw.Draw(canvas)

    title_font = get_font(56, bold=True)
    sub_font = get_font(28, bold=False)

    title_bbox = draw.textbbox((0, 0), title, font=title_font)
    title_w = title_bbox[2] - title_bbox[0]
    title_x = (CARD_W - title_w) // 2
    title_y = 110
    draw.text((title_x, title_y), title, font=title_font, fill=ink_rgb + (255,))

    sub_bbox = draw.textbbox((0, 0), subtitle, font=sub_font)
    sub_w = sub_bbox[2] - sub_bbox[0]
    sub_x = (CARD_W - sub_w) // 2
    sub_y = title_y + (title_bbox[3] - title_bbox[0]) + 18
    draw.text((sub_x, sub_y), subtitle, font=sub_font, fill=sub_rgb + (255,))

    dev_w = 705
    device = make_campaign_device(screenshot_name, dev_w)

    shadow = Image.new("RGBA", device.size, (15, 23, 42, 0))
    shadow_alpha = device.getchannel("A").filter(ImageFilter.GaussianBlur(36))
    shadow_alpha = ImageEnhance.Brightness(shadow_alpha).enhance(0.28)
    shadow.putalpha(shadow_alpha)

    dev_x = (CARD_W - device.width) // 2
    dev_y = 350

    canvas.alpha_composite(shadow, (dev_x, dev_y + 36))
    canvas.alpha_composite(device, (dev_x, dev_y))

    mask = Image.new("L", (CARD_W, CARD_H), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, CARD_W - 1, CARD_H - 1), radius=44, fill=255)

    draw_readme_card_frame(canvas, border_rgb, radius=44)
    canvas.putalpha(mask)
    return canvas

def render_readme_skins_fanout_stage():
    w, h = 3696, 1800
    base = Image.new("RGBA", (w, h), (10, 13, 20, 255))

    # Ambient Blooms
    blooms = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    bdraw = ImageDraw.Draw(blooms)
    # Left: Spotify emerald
    bdraw.ellipse((948 - 650, 1050 - 500, 948 + 650, 1050 + 500), fill=(30, 215, 96, 75))
    # Center: Apple Music soft white/rose
    bdraw.ellipse((1848 - 600, 950 - 550, 1848 + 600, 950 + 550), fill=(250, 45, 72, 65))
    # Right: YouTube Music ruby
    bdraw.ellipse((2748 - 650, 1050 - 500, 2748 + 650, 1050 + 500), fill=(255, 20, 20, 75))
    blooms = blooms.filter(ImageFilter.GaussianBlur(170))
    base.alpha_composite(blooms)

    # Devices: width 620
    dev_w = 620
    dev_spot = make_campaign_device("Screenshot_20261023_212929_LEVYRA.jpg", dev_w)
    dev_apple = make_campaign_device("Screenshot_20261023_212956_LEVYRA.jpg", dev_w)
    dev_yt = make_campaign_device("Screenshot_20261023_213023_LEVYRA.jpg", dev_w)

    cx = (w - dev_w) // 2
    cy = 430

    angle_spot = -10.5
    spot_rot = dev_spot.rotate(angle_spot, resample=Image.Resampling.BICUBIC, expand=True)
    spot_shadow = Image.new("RGBA", spot_rot.size, (4, 8, 15, 0))
    s_alpha = spot_rot.getchannel("A").filter(ImageFilter.GaussianBlur(36))
    s_alpha = ImageEnhance.Brightness(s_alpha).enhance(0.40)
    spot_shadow.putalpha(s_alpha)
    sx = cx - 740
    sy = cy + 45

    angle_yt = 10.5
    yt_rot = dev_yt.rotate(angle_yt, resample=Image.Resampling.BICUBIC, expand=True)
    yt_shadow = Image.new("RGBA", yt_rot.size, (4, 8, 15, 0))
    y_alpha = yt_rot.getchannel("A").filter(ImageFilter.GaussianBlur(36))
    y_alpha = ImageEnhance.Brightness(y_alpha).enhance(0.40)
    yt_shadow.putalpha(y_alpha)
    yx = cx + 580
    yy = cy + 45

    apple_shadow = Image.new("RGBA", dev_apple.size, (2, 5, 10, 0))
    a_alpha = dev_apple.getchannel("A").filter(ImageFilter.GaussianBlur(48))
    a_alpha = ImageEnhance.Brightness(a_alpha).enhance(0.65)
    apple_shadow.putalpha(a_alpha)

    base.alpha_composite(spot_shadow, (sx + 20, sy + 35))
    base.alpha_composite(spot_rot, (sx, sy))

    base.alpha_composite(yt_shadow, (yx - 20, yy + 35))
    base.alpha_composite(yt_rot, (yx, yy))

    base.alpha_composite(apple_shadow, (cx, cy + 35))
    base.alpha_composite(dev_apple, (cx, cy))

    draw = ImageDraw.Draw(base)
    title_font = get_font(54, bold=True)
    sub_font = get_font(26, bold=False)

    spot_center_x = sx + spot_rot.width // 2 - 20
    t1 = "Spotify Style"
    s1 = "Familiar green accents, shortcuts & artist glow"
    tb1 = draw.textbbox((0, 0), t1, font=title_font)
    sb1 = draw.textbbox((0, 0), s1, font=sub_font)
    draw.text((spot_center_x - (tb1[2]-tb1[0])//2, 120), t1, font=title_font, fill=(30, 215, 96, 255))
    draw.text((spot_center_x - (sb1[2]-sb1[0])//2, 195), s1, font=sub_font, fill=(167, 243, 208, 255))

    apple_center_x = cx + dev_apple.width // 2
    t2 = "Apple Music Style"
    s2 = "Editorial white surfaces, album shelves & bold type"
    tb2 = draw.textbbox((0, 0), t2, font=title_font)
    sb2 = draw.textbbox((0, 0), s2, font=sub_font)
    draw.text((apple_center_x - (tb2[2]-tb2[0])//2, 110), t2, font=title_font, fill=(255, 241, 242, 255))
    draw.text((apple_center_x - (sb2[2]-sb2[0])//2, 185), s2, font=sub_font, fill=(254, 205, 211, 255))

    yt_center_x = yx + yt_rot.width // 2 + 20
    t3 = "YouTube Music Style"
    s3 = "Familiar mood chips, clean cards & daily charts"
    tb3 = draw.textbbox((0, 0), t3, font=title_font)
    sb3 = draw.textbbox((0, 0), s3, font=sub_font)
    draw.text((yt_center_x - (tb3[2]-tb3[0])//2, 120), t3, font=title_font, fill=(255, 60, 60, 255))
    draw.text((yt_center_x - (sb3[2]-sb3[0])//2, 195), s3, font=sub_font, fill=(254, 202, 202, 255))

    draw_readme_card_frame(base, (51, 65, 85), radius=44)
    return base

def generate_readme_cards():
    os.makedirs(README_CARD_DIR, exist_ok=True)
    cards = []

    # 1. Render Hero diptych (Cards 01 and 02) with dynamic original cut
    left_card, right_card = render_readme_hero_diptych(
        README_CARD_SPECS[0],
        README_CARD_SPECS[1],
    )
    for artwork, spec in ((left_card, README_CARD_SPECS[0]), (right_card, README_CARD_SPECS[1])):
        cards.append(artwork)
        out_path = os.path.join(README_CARD_DIR, spec[0] + ".webp")
        artwork.save(out_path, "WEBP", quality=95, method=6)
        print(f"Saved README editorial card at {artwork.size}: {out_path}")

    # 2. Render Theme diptych (Cards 03 and 04) with dynamic light/dark interlocking cut
    theme_left, theme_right = render_readme_theme_diptych(
        README_CARD_SPECS[2],
        README_CARD_SPECS[3],
    )
    for artwork, spec in ((theme_left, README_CARD_SPECS[2]), (theme_right, README_CARD_SPECS[3])):
        cards.append(artwork)
        out_path = os.path.join(README_CARD_DIR, spec[0] + ".webp")
        artwork.save(out_path, "WEBP", quality=95, method=6)
        print(f"Saved README editorial card at {artwork.size}: {out_path}")

    # 3. Render Cards 05 through 20 (Rows 2, 3, 4, and 5 as clean centered cards!)
    for index in range(4, 20):
        spec = README_CARD_SPECS[index]
        artwork = render_readme_centered_card(spec)
        cards.append(artwork)
        out_path = os.path.join(README_CARD_DIR, spec[0] + ".webp")
        artwork.save(out_path, "WEBP", quality=95, method=6)
        print(f"Saved README editorial card at {artwork.size}: {out_path}")

    # 4. Render Row 6 Skins diptychs (Cards 21 & 22 Spotify/Apple and 23 & 24 YTM/Signature)
    skins_left, skins_right = render_readme_hero_diptych(
        README_CARD_SPECS[20],
        README_CARD_SPECS[21],
    )
    for artwork, spec in ((skins_left, README_CARD_SPECS[20]), (skins_right, README_CARD_SPECS[21])):
        cards.append(artwork)
        out_path = os.path.join(README_CARD_DIR, spec[0] + ".webp")
        artwork.save(out_path, "WEBP", quality=95, method=6)
        print(f"Saved README editorial card at {artwork.size}: {out_path}")

    sig_left, sig_right = render_readme_theme_diptych(
        README_CARD_SPECS[22],
        README_CARD_SPECS[23],
    )
    for artwork, spec in ((sig_left, README_CARD_SPECS[22]), (sig_right, README_CARD_SPECS[23])):
        cards.append(artwork)
        out_path = os.path.join(README_CARD_DIR, spec[0] + ".webp")
        artwork.save(out_path, "WEBP", quality=95, method=6)
        print(f"Saved README editorial card at {artwork.size}: {out_path}")

    # 4. Assemble Rows (01 to 06)
    row_count = len(README_CARD_SPECS) // 4
    for row_index in range(row_count):
        row = Image.new("RGB", (3600, 1800))
        spaced_row = Image.new("RGBA", (3696, 1800), (0, 0, 0, 0))
        for col in range(4):
            card = cards[row_index * 4 + col]
            row.paste(card.convert("RGB"), (col * 900, 0))
            spaced_row.paste(card, (col * (900 + 32), 0), card)

        if row_index == 5:
            spaced_path = os.path.join(README_CARD_DIR, "editorial-grid-row-06-skins-spaced.webp")
            row_path = os.path.join(README_CARD_DIR, "editorial-grid-row-06-skins.webp")
            uniform_path = os.path.join(README_CARD_DIR, "editorial-grid-row-06-skins-uniform-centered.webp")
        else:
            idx_str = f"{row_index + 1:02d}"
            spaced_path = os.path.join(README_CARD_DIR, f"editorial-grid-row-{idx_str}-spaced.webp")
            row_path = os.path.join(README_CARD_DIR, f"editorial-grid-row-{idx_str}.webp")
            uniform_name = (
                "editorial-grid-row-01-orbit-reference.webp"
                if row_index == 0
                else f"editorial-grid-row-{idx_str}-uniform-centered.webp"
            )
            uniform_path = os.path.join(README_CARD_DIR, uniform_name)

        spaced_row.save(spaced_path, "WEBP", quality=95, method=6)
        row.save(row_path, "WEBP", quality=95, method=6)
        row.save(uniform_path, "WEBP", quality=95, method=6)
        print(f"Saved row {row_index + 1}: {spaced_path}")

if __name__ == "__main__":
    generate_hero_panoramic_showcase()
    generate_feature_cards()
    generate_readme_cards()
    print("All Material 3 Expressive editorial-stage assets generated successfully!")
