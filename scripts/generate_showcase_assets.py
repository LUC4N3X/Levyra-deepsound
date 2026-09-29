import os
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageEnhance

SCREENSHOT_DIR = r"C:\Users\Luca Drogo\Desktop\screenshots"
OUT_SHOWCASE_DIR = r"docs\assets\showcase"
OUT_CARDS_DIR = os.path.join(OUT_SHOWCASE_DIR, "cards")
LOGO_PATH = r"C:\Users\Luca Drogo\Downloads\ChatGPT Image 5 set 2026, 19_28_06.png"

os.makedirs(OUT_SHOWCASE_DIR, exist_ok=True)
os.makedirs(OUT_CARDS_DIR, exist_ok=True)

def get_font(size, bold=False):
    font_path = r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf"
    if not os.path.exists(font_path):
        font_path = r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf"
    return ImageFont.truetype(font_path, size)

def enhance_screenshot(img):
    img = img.convert("RGB")
    enhancer = ImageEnhance.Contrast(img)
    img = enhancer.enhance(1.05)
    enhancer = ImageEnhance.Color(img)
    img = enhancer.enhance(1.06)
    enhancer = ImageEnhance.Sharpness(img)
    img = enhancer.enhance(1.10)
    return img

def create_clean_phone(screen_img, target_height=1060):
    screen_img = enhance_screenshot(screen_img)
    orig_w, orig_h = screen_img.size
    aspect = orig_w / orig_h
    screen_h = int(target_height)
    screen_w = int(screen_h * aspect)
    screen_res = screen_img.resize((screen_w, screen_h), Image.Resampling.LANCZOS)

    bezel = max(7, int(screen_w * 0.030))
    corner_radius = int(screen_w * 0.125)
    phone_w = screen_w + bezel * 2
    phone_h = screen_h + bezel * 2

    phone = Image.new("RGBA", (phone_w, phone_h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(phone)

    # Dark titanium body
    draw.rounded_rectangle((0, 0, phone_w - 1, phone_h - 1), radius=corner_radius, fill=(18, 20, 26, 255))
    draw.rounded_rectangle((0, 0, phone_w - 1, phone_h - 1), radius=corner_radius, outline=(60, 68, 82, 255), width=2)

    # Screen mask
    screen_mask = Image.new("L", (screen_w, screen_h), 0)
    sdraw = ImageDraw.Draw(screen_mask)
    inner_radius = max(8, corner_radius - bezel)
    sdraw.rounded_rectangle((0, 0, screen_w - 1, screen_h - 1), radius=inner_radius, fill=255)

    phone.paste(screen_res, (bezel, bezel), screen_mask)

    # Camera punch hole
    cam_r = max(3, int(screen_w * 0.022))
    cam_x = phone_w // 2
    cam_y = bezel + int(screen_h * 0.026)
    draw.ellipse((cam_x - cam_r, cam_y - cam_r, cam_x + cam_r, cam_y + cam_r), fill=(5, 5, 8, 255))

    # Speaker slit
    spk_w = int(screen_w * 0.16)
    spk_h = max(2, int(bezel * 0.35))
    spk_x = (phone_w - spk_w) // 2
    spk_y = max(1, bezel // 3)
    draw.rounded_rectangle((spk_x, spk_y, spk_x + spk_w, spk_y + spk_h), radius=spk_h // 2, fill=(40, 44, 52, 255))

    return phone

def create_studio_shadow(phone, blur=45, opacity=110, offset_y=26):
    pad = blur * 2 + abs(offset_y)
    sw = phone.width + pad * 2
    sh = phone.height + pad * 2
    shadow = Image.new("RGBA", (sw, sh), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow)

    sdraw.rounded_rectangle(
        (pad + 8, pad + 10, pad + phone.width - 8, pad + phone.height + 6),
        radius=int(phone.width * 0.12),
        fill=(8, 12, 20, opacity)
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(blur))
    return shadow, pad

def create_rich_wave_bg(width, height, top_col, accent_start, accent_end, y_start=960, y_end=680):
    """
    Renders an organic, supersampled S-curve separating a bright tinted canvas
    and a vibrant, rich color gradient.
    """
    w2, h2 = width * 2, height * 2
    img2 = Image.new("RGBA", (w2, h2), (*top_col, 255))

    steps = 120
    xs = np.linspace(0, w2, steps)
    t = np.linspace(0, 1, steps)
    y1_2 = y_start * 2
    y2_2 = y_end * 2
    c1_y = y1_2 - 180
    c2_y = y2_2 + 180
    ys = (1 - t)**3 * y1_2 + 3 * (1 - t)**2 * t * c1_y + 3 * (1 - t) * t**2 * c2_y + t**3 * y2_2

    poly = [(0, y1_2)]
    for x, y in zip(xs, ys):
        poly.append((int(x), int(y)))
    poly.extend([(w2, y2_2), (w2, h2), (0, h2)])

    mask2 = Image.new("L", (w2, h2), 0)
    mdraw = ImageDraw.Draw(mask2)
    mdraw.polygon(poly, fill=255)

    g_arr = np.zeros((h2, w2, 4), dtype=np.uint8)
    for y in range(h2):
        ratio = y / h2
        r = int(accent_start[0] * (1 - ratio) + accent_end[0] * ratio)
        g = int(accent_start[1] * (1 - ratio) + accent_end[1] * ratio)
        b = int(accent_start[2] * (1 - ratio) + accent_end[2] * ratio)
        g_arr[y, :, 0] = r
        g_arr[y, :, 1] = g
        g_arr[y, :, 2] = b
        g_arr[y, :, 3] = 255

    gradient2 = Image.fromarray(g_arr)
    img2.paste(gradient2, (0, 0), mask2)
    return img2.resize((width, height), Image.Resampling.LANCZOS)

def generate_hero_panoramic_showcase():
    """
    Generates 00_levyra_hero_wall_player.webp with compact cinematic proportions,
    the user-provided 3D Levyra DeepSound logo, and dynamic fanned screenshot wall.
    """
    canvas_w, canvas_h = 2400, 880
    canvas = Image.new("RGBA", (canvas_w, canvas_h), (7, 8, 14, 255))
    draw = ImageDraw.Draw(canvas)

    # Ambient geometric lighting accents
    draw.polygon([(680, 0), (2400, 0), (2400, 320), (950, 520)], fill=(20, 36, 75, 255))
    draw.polygon([(1050, 880), (2400, 620), (2400, 880)], fill=(95, 22, 115, 200))
    draw.polygon([(1650, 0), (1920, 0), (1350, 880), (1100, 880)], fill=(180, 25, 140, 60))

    # Soft radial glow behind center hero phone
    glow_layer = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow_layer)
    gdraw.ellipse((1200, 100, 1800, 700), fill=(40, 80, 180, 70))
    glow_layer = glow_layer.filter(ImageFilter.GaussianBlur(80))
    canvas.paste(glow_layer, (0, 0), glow_layer)

    # Paste Levyra DeepSound 3D Logo from user path
    target_logo_w = 340
    logo_file = LOGO_PATH if os.path.exists(LOGO_PATH) else r"app\src\main\res\drawable\levyra_logo.png"
    with Image.open(logo_file) as l_src:
        logo = l_src.convert("RGBA")
        bbox = logo.getbbox()
        logo_crop = logo.crop(bbox)
        target_logo_h = int(logo_crop.height * (target_logo_w / logo_crop.width))
        logo_res = logo_crop.resize((target_logo_w, target_logo_h), Image.Resampling.LANCZOS)
    canvas.paste(logo_res, (110, 65), logo_res)

    # Typography on the left
    text_y = 65 + target_logo_h + 30
    draw.text((118, text_y), "MUSIC, KEPT PERSONAL.", font=get_font(28, bold=True), fill=(56, 189, 248, 255))
    text_y += 50
    draw.multiline_text(
        (118, text_y),
        "Native playback on Android and Windows.\nHigh-res streaming, synced lyrics & offline library.\nNo ads. Zero telemetry. 100% open source.",
        font=get_font(22),
        fill=(205, 215, 230, 255),
        spacing=10
    )
    text_y += 120
    draw.line((118, text_y, 480, text_y), fill=(255, 255, 255, 60), width=2)
    text_y += 24
    draw.text((118, text_y), "PLAY  /  EXPLORE  /  KEEP", font=get_font(18, bold=True), fill=(255, 255, 255, 180))
    text_y += 35
    draw.text((118, text_y), "ANDROID • WINDOWS", font=get_font(17, bold=True), fill=(56, 189, 248, 220))

    # Two-tier fanned phone screenshot wall
    back_row = [
        ("Screenshot_20260926_194736_LEVYRA.jpg", 460, 640, 80, -7),
        ("Screenshot_20260926_194706_LEVYRA.jpg", 480, 980, 50, -3),
        ("Screenshot_20260927_132248_LEVYRA.jpg", 480, 1680, 50, 4),
        ("Screenshot_20260929_194827_LEVYRA.jpg", 460, 1960, 90, 7),
    ]
    front_row = [
        ("Screenshot_20260926_171253_LEVYRA.jpg", 640, 860, 230, -4),
        ("Screenshot_20260926_193948_LEVYRA.jpg", 770, 1260, 95, 0),
        ("Screenshot_20260926_194603_LEVYRA.jpg", 640, 1720, 230, 4),
    ]

    for filename, height, px, py, angle in back_row + front_row:
        s_path = os.path.join(SCREENSHOT_DIR, filename)
        if not os.path.exists(s_path):
            continue
        with Image.open(s_path) as src:
            phone = create_clean_phone(src, target_height=height)
        if angle:
            phone = phone.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
        shadow, pad = create_studio_shadow(phone, blur=36, opacity=160, offset_y=22)
        canvas.paste(shadow, (px - pad, py - pad), shadow)
        canvas.paste(phone, (px, py), phone)

    out_path = os.path.join(OUT_SHOWCASE_DIR, "00_levyra_hero_wall_player.webp")
    canvas.convert("RGB").save(out_path, "WEBP", quality=94, method=6)
    print("Generated Panoramic Hero Showcase:", out_path)

def generate_feature_cards():
    """
    Generates all 20 feature cards with rich, vibrant, premium colors (ZERO grey, ZERO pink).
    """
    card_w, card_h = 900, 1600
    title_font = get_font(58, bold=True)
    sub_font = get_font(28, bold=False)

    # 1. CONNECTED HERO CARDS (01 & 02) - Nordic Ocean Sapphire
    pano_w = card_w * 2
    pano_h = card_h
    pano_bg = create_rich_wave_bg(
        pano_w, pano_h,
        top_col=(240, 247, 255),
        accent_start=(24, 100, 218),
        accent_end=(14, 68, 175),
        y_start=1150,
        y_end=620
    )

    home_file = os.path.join(SCREENSHOT_DIR, "Screenshot_20260926_171253_LEVYRA.jpg")
    with Image.open(home_file) as source:
        phone_hero = create_clean_phone(source, target_height=1280)

    rotated_hero = phone_hero.rotate(25, resample=Image.Resampling.BICUBIC, expand=True)
    rot_shadow, rpad = create_studio_shadow(rotated_hero, blur=48, opacity=110, offset_y=28)

    hero_x = 900 - rotated_hero.width // 2 - 35
    hero_y = 170
    pano_bg.paste(rot_shadow, (hero_x - rpad, hero_y - rpad), rot_shadow)
    pano_bg.paste(rotated_hero, (hero_x, hero_y), rotated_hero)

    pdraw = ImageDraw.Draw(pano_bg)

    # Card 01 Text (Bottom Left)
    pdraw.text((75, 1190), "Pure listening", font=get_font(60, bold=True), fill=(15, 23, 42, 255))
    pdraw.multiline_text(
        (75, 1275),
        "Zero ads, zero accounts, zero tracking.\nPure high-fidelity YouTube Music,\nplayed natively on your device.",
        font=sub_font,
        fill=(25, 40, 60, 255),
        spacing=10
    )

    # Card 02 Text (Top Right)
    c2_left = 900
    text_x = c2_left + 330
    pdraw.text((text_x, 95), "Download & keep", font=title_font, fill=(20, 23, 29, 255))
    pdraw.multiline_text(
        (text_x, 175),
        "Clean M4A files in device storage.\nFull metadata, artwork, and lyrics.",
        font=sub_font,
        fill=(71, 85, 105, 255),
        spacing=10
    )

    card_01 = pano_bg.crop((0, 0, card_w, card_h))
    card_02 = pano_bg.crop((card_w, 0, pano_w, card_h))
    card_01.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "01_home.webp"), "WEBP", quality=92, method=6)
    card_02.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "02_stay_with_the_song.webp"), "WEBP", quality=92, method=6)
    print("Generated Hero Cards: 01_home.webp, 02_stay_with_the_song.webp")

    # 2. CARDS 03 TO 20 (Rich, Vibrant, Premium Palettes - ZERO Grey, ZERO Pink)
    single_specs = [
        # (filename, screenshot_file, title, subtitle, top_color, accent_start, accent_end)
        ("03_now_playing.webp", "Screenshot_20260926_193948_LEVYRA.jpg", "Stay with the song", "One focused player for audio and video.\nSynced lyrics move with every beat.", (255, 250, 242), (214, 108, 28), (170, 72, 16)),
        ("04_player_deck.webp", "Screenshot_20260929_195520_LEVYRA.jpg", "Style your player", "Canvas, card deck, or classic vinyl.\nSwitch your stage seamlessly.", (244, 246, 255), (79, 70, 229), (55, 45, 185)),
        ("05_explore_mix.webp", "Screenshot_20260927_140841_LEVYRA.jpg", "Explore and mix", "Live radio, fresh currents, and custom\nsliders between familiar and new.", (236, 250, 255), (14, 136, 225), (8, 98, 180)),
        ("06_artist_profile.webp", "Screenshot_20260926_194603_LEVYRA.jpg", "Meet the artist", "Full discography, singles, biographies,\nand top tracks in one tap.", (248, 243, 255), (124, 58, 237), (92, 38, 195)),
        ("07_genres.webp", "Screenshot_20260929_194827_LEVYRA.jpg", "Pick a direction", "Move through moods, vibes, and genres\ncrafted for every moment.", (238, 253, 246), (16, 155, 102), (10, 118, 75)),
        ("08_audio_tuning.webp", "Screenshot_20260926_194845_LEVYRA.jpg", "Shape the playback", "Sleep timer, tempo tuning, loudness norm,\nand advanced audio engine.", (238, 252, 252), (13, 148, 136), (10, 112, 104)),
        ("09_album.webp", "Screenshot_20260926_194706_LEVYRA.jpg", "Open the album", "High-resolution artwork, release info,\nand complete tracklists.", (255, 251, 240), (205, 135, 25), (165, 98, 14)),
        ("10_search.webp", "Screenshot_20260926_194736_LEVYRA.jpg", "Find it instantly", "Recent searches, suggestions, and\ninstant matching across your music.", (238, 251, 255), (6, 145, 195), (4, 108, 150)),
        ("11_collections.webp", "Screenshot_20260927_132323_LEVYRA.jpg", "Curated for you", "Handpicked playlists and gems\nrevolving around what you love.", (244, 245, 255), (99, 102, 241), (72, 75, 210)),
        ("12_listening_rhythm.webp", "Screenshot_20260927_131943_LEVYRA.jpg", "Your listening rhythm", "Activity heatmaps, peak hours, and\nyour personal listening cadence.", (238, 253, 246), (16, 160, 110), (10, 122, 82)),
        ("13_your_orbit.webp", "Screenshot_20260929_195235_LEVYRA.jpg", "In your orbit", "The songs and artists that always return\nto your rotation.", (247, 242, 255), (109, 40, 217), (78, 24, 172)),
        ("14_listening_pulse.webp", "Screenshot_20260926_193717_LEVYRA.jpg", "Keep it personal", "Private listening stats and charts,\ncomputed strictly on your device.", (240, 247, 255), (30, 80, 220), (18, 58, 175)),
        ("15_artist_playlists.webp", "Screenshot_20260929_201454_LEVYRA.jpg", "Artist playlists", "Curated sets, tours, and the\nessential catalog of every artist.", (242, 248, 255), (25, 100, 220), (16, 70, 172)),
        ("16_settings_vault.webp", "Screenshot_20260929_194944_LEVYRA.jpg", "Tailor every detail", "Audio, design, gestures, and local\nsingle-file Vault backups.", (240, 252, 246), (22, 130, 88), (14, 95, 62)),
        ("17_new_releases.webp", "Screenshot_20260927_132248_LEVYRA.jpg", "Fresh off the stage", "New singles and albums updated\nevery week directly from artists.", (255, 248, 244), (216, 85, 38), (172, 60, 22)),
        ("18_fresh_currents.webp", "Screenshot_20260929_194853_LEVYRA.jpg", "Discovery stream", "Explore live stations, genre charts,\nand community soundscapes.", (236, 254, 255), (8, 155, 185), (5, 118, 145)),
        ("19_featured_artists.webp", "Screenshot_20260905_135618_LEVYRA.jpg", "Featured artists", "Discover local and global artists,\ncurated collections, and albums.", (248, 242, 255), (135, 52, 225), (98, 32, 180)),
        ("20_soundstage.webp", "Screenshot_20260926_193948_LEVYRA.jpg", "Pure soundstage", "Experience lossless decoding and\nuncompromised audio fidelity.", (240, 247, 255), (20, 85, 215), (12, 58, 162)),
    ]

    for filename, screenshot_name, title, subtitle, top_col, a_start, a_end in single_specs:
        screen_file = os.path.join(SCREENSHOT_DIR, screenshot_name)
        if not os.path.exists(screen_file):
            print(f"Skipping {filename}: {screen_file} not found")
            continue

        with Image.open(screen_file) as source:
            phone = create_clean_phone(source, target_height=1060)

        bg = create_rich_wave_bg(card_w, card_h, top_col, a_start, a_end, y_start=960, y_end=680)
        draw = ImageDraw.Draw(bg)

        draw.text((75, 95), title, font=title_font, fill=(15, 23, 42, 255))
        draw.multiline_text((75, 175), subtitle, font=sub_font, fill=(71, 85, 105, 255), spacing=10)

        shadow, pad = create_studio_shadow(phone, blur=45, opacity=110, offset_y=26)
        px = (card_w - phone.width) // 2
        py = 390
        bg.paste(shadow, (px - pad, py - pad), shadow)
        bg.paste(phone, (px, py), phone)

        out_path = os.path.join(OUT_CARDS_DIR, filename)
        bg.convert("RGB").save(out_path, "WEBP", quality=92, method=6)
        print(f"Generated Feature Card: {out_path}")

def main():
    print("Generating rich, refined Levyra showcase assets...")
    try:
        generate_hero_panoramic_showcase()
    except (FileNotFoundError, OSError) as e:
        print(f"Skipping hero panoramic showcase ({e})")
    generate_feature_cards()
    print("Showcase generation completed successfully!")

if __name__ == "__main__":
    main()
