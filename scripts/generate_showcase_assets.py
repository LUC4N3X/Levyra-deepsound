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

def draw_rounded_layer(canvas, size, radius, color, angle, position):
    layer = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    draw.rounded_rectangle((0, 0, size[0] - 1, size[1] - 1), radius=radius, fill=(*color, 255))
    rotated = layer.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
    canvas.paste(rotated, position, rotated)

def create_m3_pair_canvas(card_w, card_h, gutter, scheme, pair_index):
    duo_w = card_w * 2 + gutter
    arr = np.zeros((card_h, duo_w, 4), dtype=np.uint8)
    top = np.array(scheme["surface"], dtype=np.float32)
    bottom = np.array(scheme["surface_low"], dtype=np.float32)
    for y in range(card_h):
        progress = y / float(card_h)
        color = top * (1 - progress) + bottom * progress
        arr[y, :, :3] = color.astype(np.uint8)
    arr[:, :, 3] = 255
    canvas = Image.fromarray(arr, mode="RGBA")

    direction = 1 if pair_index % 2 == 0 else -1
    draw_rounded_layer(
        canvas, (duo_w + 360, 610), 305, scheme["primary_container"],
        direction * 8, (-180, 410 if direction > 0 else 570),
    )
    draw_rounded_layer(
        canvas, (760, 560), 220, scheme["secondary_container"],
        -direction * 13, (-260, -150 if direction > 0 else 1090),
    )
    draw_rounded_layer(
        canvas, (720, 520), 210, scheme["tertiary_container"],
        direction * 11, (duo_w - 500, 1110 if direction > 0 else -120),
    )
    return canvas

def draw_m3_label(draw, x, y, text, scheme):
    font = get_font(27, bold=True)
    bbox = draw.textbbox((0, 0), text, font=font)
    width = bbox[2] - bbox[0]
    height = 52
    draw.rounded_rectangle(
        (x, y, x + width + 80, y + height),
        radius=26,
        fill=(*scheme["primary"], 255),
    )
    on_primary = (*scheme["on_primary"], 255)
    draw_vector_sparkle(draw, x + 27, y + height // 2, 12, on_primary)
    draw.text((x + 50, y + 10 - bbox[1]), text, font=font, fill=on_primary)

def fit_title_font(draw, title, max_width, initial_size=68, minimum_size=50):
    for size in range(initial_size, minimum_size - 1, -2):
        font = get_font(size, bold=True)
        if draw.textbbox((0, 0), title, font=font)[2] <= max_width:
            return font
    return get_font(minimum_size, bold=True)

def draw_m3_copy(card, spec, placement, scheme, card_index):
    _, kicker, title, subtitle, _, _ = spec
    draw = ImageDraw.Draw(card)
    panel_x = 48
    panel_y = 48 if placement == "top" else 1128
    panel_w = 804
    panel_h = 420
    panel_fill = scheme["surface_lowest"] if card_index == 0 else scheme["secondary_container"]
    panel_text = scheme["on_surface"] if card_index == 0 else scheme["on_secondary_container"]
    panel_body = scheme["on_surface_variant"] if card_index == 0 else scheme["on_secondary_container"]
    draw.rounded_rectangle(
        (panel_x, panel_y, panel_x + panel_w, panel_y + panel_h),
        radius=76,
        fill=(*panel_fill, 255),
        outline=(*scheme["outline_variant"], 255),
        width=3,
    )

    x = panel_x + 42
    y = panel_y + 38
    draw_m3_label(draw, x, y, kicker, scheme)

    fab_size = 78
    fab_x = panel_x + panel_w - fab_size - 34
    fab_y = panel_y + 28
    draw.ellipse(
        (fab_x, fab_y, fab_x + fab_size, fab_y + fab_size),
        fill=(*scheme["tertiary_container"], 255),
    )
    draw_vector_sparkle(
        draw,
        fab_x + fab_size // 2,
        fab_y + fab_size // 2,
        20,
        (*scheme["on_tertiary_container"], 255),
    )

    title_y = y + 78
    title_font = fit_title_font(draw, title, 700, initial_size=70, minimum_size=48)
    draw.text((x, title_y), title, font=title_font, fill=(*panel_text, 255))
    draw.multiline_text(
        (x, title_y + 86),
        subtitle,
        font=get_font(31),
        fill=(*panel_body, 255),
        spacing=7,
    )

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

def create_s26_ultra_mockup(source, scheme, size=(780, 1320)):
    frame = 14
    outer_w, outer_h = size
    inner_w, inner_h = outer_w - frame * 2, outer_h - frame * 2
    phone = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(phone)
    draw.rounded_rectangle(
        (0, 0, outer_w - 1, outer_h - 1),
        radius=58,
        fill=(28, 29, 34, 255),
        outline=(151, 154, 164, 230),
        width=3,
    )
    screenshot = crop_to_fill(source, inner_w, inner_h)
    mask = Image.new("L", (inner_w, inner_h), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle((0, 0, inner_w - 1, inner_h - 1), radius=45, fill=255)
    phone.paste(screenshot, (frame, frame), mask)
    draw.rounded_rectangle(
        (frame, frame, outer_w - frame - 1, outer_h - frame - 1),
        radius=45,
        outline=(8, 9, 12, 245),
        width=4,
    )
    camera_radius = 10
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

def paste_screenshot_portal(canvas, spec, card_index, placement, gutter, scheme, pair_index):
    card_w = 900
    card_left = card_index * (card_w + gutter)
    screenshot_path = resolve_screenshot_path(spec[0])
    if not screenshot_path:
        raise FileNotFoundError(f"Missing showcase screenshot: {spec[0]}")
    with Image.open(screenshot_path) as source:
        phone = create_s26_ultra_mockup(source, scheme)

    direction = 1 if pair_index % 2 == 0 else -1
    if placement == "bottom":
        angle = -5 * direction
        x = card_left + (155 if card_index == 0 else -70)
        y = -165
    else:
        angle = 5 * direction
        x = card_left + (-40 if card_index == 0 else 90)
        y = 350

    rotated = phone.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
    shadow, pad = create_m3_shadow(rotated, blur=30, offset_y=16)
    canvas.paste(shadow, (x - pad, y - pad), shadow)
    canvas.paste(rotated, (x, y), rotated)

def render_m3_cut_pair(left_spec, right_spec, pair_index):
    card_w, card_h, gutter = 900, 1600, 48
    scheme = M3_EXPRESSIVE_SCHEMES[pair_index % len(M3_EXPRESSIVE_SCHEMES)]
    canvas = create_m3_pair_canvas(card_w, card_h, gutter, scheme, pair_index)

    left_copy = "bottom" if pair_index % 2 == 0 else "top"
    right_copy = "top" if left_copy == "bottom" else "bottom"
    paste_screenshot_portal(canvas, left_spec, 0, left_copy, gutter, scheme, pair_index)
    paste_screenshot_portal(canvas, right_spec, 1, right_copy, gutter, scheme, pair_index)

    right_left = card_w + gutter
    left_card = canvas.crop((0, 0, card_w, card_h))
    right_card = canvas.crop((right_left, 0, right_left + card_w, card_h))
    draw_m3_copy(left_card, left_spec, left_copy, scheme, 0)
    draw_m3_copy(right_card, right_spec, right_copy, scheme, 1)
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
    print("Generating Material 3 Expressive tonal-portal pairs...")
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
                print(f"Saved Material 3 Expressive tonal portal: {out_file}")

if __name__ == "__main__":
    generate_hero_panoramic_showcase()
    generate_feature_cards()
    print("All Material 3 Expressive tonal-portal assets generated successfully!")
