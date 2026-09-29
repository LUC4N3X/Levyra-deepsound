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
    img = enhancer.enhance(1.06)
    enhancer = ImageEnhance.Color(img)
    img = enhancer.enhance(1.08)
    enhancer = ImageEnhance.Sharpness(img)
    img = enhancer.enhance(1.12)
    return img

def create_clean_phone(screen_img, target_height=1180):
    screen_img = enhance_screenshot(screen_img)
    orig_w, orig_h = screen_img.size
    aspect = orig_w / orig_h
    screen_h = int(target_height)
    screen_w = int(screen_h * aspect)
    screen_res = screen_img.resize((screen_w, screen_h), Image.Resampling.LANCZOS)

    bezel = max(7, int(screen_w * 0.028))
    corner_radius = int(screen_w * 0.125)
    phone_w = screen_w + bezel * 2
    phone_h = screen_h + bezel * 2

    phone = Image.new("RGBA", (phone_w, phone_h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(phone)

    # Dark titanium body
    draw.rounded_rectangle((0, 0, phone_w - 1, phone_h - 1), radius=corner_radius, fill=(16, 18, 24, 255))
    draw.rounded_rectangle((0, 0, phone_w - 1, phone_h - 1), radius=corner_radius, outline=(65, 75, 90, 255), width=2)

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

def create_studio_shadow(phone, blur=48, opacity=115, offset_y=28):
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

def create_delicate_bg(width, height, top_tint, accent_tone, glow_tone=None, y_start=920, y_end=660):
    """
    Renders an organic, supersampled soft S-curve separating a delicate tinted canvas
    and a soft, refined accent tone, with an ethereal ambient glow behind the phone.
    """
    card = Image.new("RGBA", (width, height), (*top_tint, 255))
    w2, h2 = width * 2, height * 2
    wave_layer = Image.new("RGBA", (w2, h2), (0, 0, 0, 0))
    wdraw = ImageDraw.Draw(wave_layer)

    steps = 100
    t = np.linspace(0, 1, steps)
    y1_2 = y_start * 2
    y2_2 = y_end * 2
    c1_y = y1_2 - 140
    c2_y = y2_2 + 140
    ys = (1 - t)**3 * y1_2 + 3 * (1 - t)**2 * t * c1_y + 3 * (1 - t) * t**2 * c2_y + t**3 * y2_2
    xs = np.linspace(0, w2, steps)

    poly = [(0, y1_2)]
    for x, y in zip(xs, ys):
        poly.append((int(x), int(y)))
    poly.extend([(w2, y2_2), (w2, h2), (0, h2)])
    wdraw.polygon(poly, fill=(*accent_tone, 255))

    wave_smooth = wave_layer.resize((width, height), Image.Resampling.LANCZOS)
    card = Image.alpha_composite(card, wave_smooth)

    if glow_tone:
        glow_canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        gdraw = ImageDraw.Draw(glow_canvas)
        gdraw.ellipse((180, 600, 720, 1180), fill=(*glow_tone, 50))
        glow_canvas = glow_canvas.filter(ImageFilter.GaussianBlur(90))
        card = Image.alpha_composite(card, glow_canvas)

    return card

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
    Generates all 20 feature cards with delicate, elegant, harmonious palettes
    and high-visibility typography and larger phone mockups.
    """
    card_w, card_h = 900, 1600
    title_font = get_font(68, bold=True)
    sub_font = get_font(33, bold=False)

    # 1. CONNECTED HERO CARDS (01 & 02) - Soft Nordic Ice Blue
    pano_w = card_w * 2
    pano_h = card_h
    pano_bg = create_delicate_bg(
        pano_w, pano_h,
        top_tint=(238, 245, 250),
        accent_tone=(170, 196, 218),
        glow_tone=(148, 185, 210),
        y_start=1150,
        y_end=620
    )

    home_file = os.path.join(SCREENSHOT_DIR, "Screenshot_20260926_171253_LEVYRA.jpg")
    with Image.open(home_file) as source:
        phone_hero = create_clean_phone(source, target_height=1400)

    rotated_hero = phone_hero.rotate(25, resample=Image.Resampling.BICUBIC, expand=True)
    rot_shadow, rpad = create_studio_shadow(rotated_hero, blur=52, opacity=115, offset_y=30)

    hero_x = 900 - rotated_hero.width // 2 - 35
    hero_y = 150
    pano_bg.paste(rot_shadow, (hero_x - rpad, hero_y - rpad), rot_shadow)
    pano_bg.paste(rotated_hero, (hero_x, hero_y), rotated_hero)

    pdraw = ImageDraw.Draw(pano_bg)

    # Card 01 Text (Bottom Left)
    pdraw.text((75, 1170), "Pure listening", font=get_font(72, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text(
        (75, 1265),
        "Zero ads, zero accounts, zero tracking.\nPure high-fidelity YouTube Music,\nplayed natively on your device.",
        font=get_font(34),
        fill=(35, 50, 68, 255),
        spacing=12
    )

    # Card 02 Text (Top Right)
    c2_left = 900
    text_x = c2_left + 300
    pdraw.text((text_x, 80), "Download & keep", font=get_font(70, bold=True), fill=(15, 22, 32, 255))
    pdraw.multiline_text(
        (text_x, 175),
        "Clean M4A files in device storage.\nFull metadata, artwork, and lyrics.",
        font=get_font(33),
        fill=(45, 60, 75, 255),
        spacing=12
    )

    card_01 = pano_bg.crop((0, 0, card_w, card_h))
    card_02 = pano_bg.crop((card_w, 0, pano_w, card_h))
    card_01.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "01_home.webp"), "WEBP", quality=92, method=6)
    card_02.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "02_stay_with_the_song.webp"), "WEBP", quality=92, method=6)
    print("Generated Hero Cards: 01_home.webp, 02_stay_with_the_song.webp")

    # 2. CARDS 03 TO 20 (Cohesive, Delicate Palettes - ZERO Carnival, ZERO Pink, ZERO Grey)
    # Five subtle tone-on-tone themes:
    # C_CELESTE   = (top, accent, glow)
    # C_SAGE      = (top, accent, glow)
    # C_CASHMERE  = (top, accent, glow)
    # C_TWILIGHT  = (top, accent, glow)
    # C_SEAFOAM   = (top, accent, glow)
    C_CELESTE = ((238, 245, 250), (170, 196, 218), (148, 185, 210))
    C_SAGE = ((238, 246, 242), (168, 198, 188), (145, 185, 172))
    C_CASHMERE = ((248, 245, 240), (212, 194, 170), (195, 175, 150))
    C_TWILIGHT = ((244, 244, 250), (182, 184, 212), (162, 165, 200))
    C_SEAFOAM = ((238, 246, 248), (166, 198, 202), (145, 188, 192))

    single_specs = [
        # (filename, screenshot_file, title, subtitle, palette)
        ("03_now_playing.webp", "Screenshot_20260926_193948_LEVYRA.jpg", "Stay with the song", "One focused player for audio and video.\nSynced lyrics move with every beat.", C_CASHMERE),
        ("04_player_deck.webp", "Screenshot_20260929_195520_LEVYRA.jpg", "Style your player", "Canvas, card deck, or classic vinyl.\nSwitch your stage seamlessly.", C_TWILIGHT),
        ("05_explore_mix.webp", "Screenshot_20260927_140841_LEVYRA.jpg", "Explore and mix", "Live radio, fresh currents, and custom\nsliders between familiar and new.", C_SEAFOAM),
        ("06_artist_profile.webp", "Screenshot_20260926_194603_LEVYRA.jpg", "Meet the artist", "Full discography, singles, biographies,\nand top tracks in one tap.", C_TWILIGHT),
        ("07_genres.webp", "Screenshot_20260929_194827_LEVYRA.jpg", "Pick a direction", "Move through moods, vibes, and genres\ncrafted for every moment.", C_SAGE),
        ("08_audio_tuning.webp", "Screenshot_20260926_194845_LEVYRA.jpg", "Shape the playback", "Sleep timer, tempo tuning, loudness norm,\nand advanced audio engine.", C_CELESTE),
        ("09_album.webp", "Screenshot_20260926_194706_LEVYRA.jpg", "Open the album", "High-resolution artwork, release info,\nand complete tracklists.", C_CASHMERE),
        ("10_search.webp", "Screenshot_20260926_194736_LEVYRA.jpg", "Find it instantly", "Recent searches, suggestions, and\ninstant matching across your music.", C_CELESTE),
        ("11_collections.webp", "Screenshot_20260927_132323_LEVYRA.jpg", "Curated for you", "Handpicked playlists and gems\nrevolving around what you love.", C_TWILIGHT),
        ("12_listening_rhythm.webp", "Screenshot_20260927_131943_LEVYRA.jpg", "Your listening rhythm", "Activity heatmaps, peak hours, and\nyour personal listening cadence.", C_SAGE),
        ("13_your_orbit.webp", "Screenshot_20260929_195235_LEVYRA.jpg", "In your orbit", "The songs and artists that always return\nto your rotation.", C_TWILIGHT),
        ("14_listening_pulse.webp", "Screenshot_20260926_193717_LEVYRA.jpg", "Keep it personal", "Private listening stats and charts,\ncomputed strictly on your device.", C_CELESTE),
        ("15_artist_playlists.webp", "Screenshot_20260929_201454_LEVYRA.jpg", "Artist playlists", "Curated sets, tours, and the\nessential catalog of every artist.", C_CELESTE),
        ("16_settings_vault.webp", "Screenshot_20260929_194944_LEVYRA.jpg", "Tailor every detail", "Audio, design, gestures, and local\nsingle-file Vault backups.", C_SAGE),
        ("17_new_releases.webp", "Screenshot_20260927_132248_LEVYRA.jpg", "Fresh off the stage", "New singles and albums updated\nevery week directly from artists.", C_CASHMERE),
        ("18_fresh_currents.webp", "Screenshot_20260929_194853_LEVYRA.jpg", "Discovery stream", "Explore live stations, genre charts,\nand community soundscapes.", C_SEAFOAM),
        ("19_featured_artists.webp", "Screenshot_20260905_135618_LEVYRA.jpg", "Featured artists", "Discover local and global artists,\ncurated collections, and albums.", C_TWILIGHT),
        ("20_soundstage.webp", "Screenshot_20260926_193948_LEVYRA.jpg", "Pure soundstage", "Experience lossless decoding and\nuncompromised audio fidelity.", C_CELESTE),
    ]

    for filename, screenshot_name, title, subtitle, palette in single_specs:
        screen_file = os.path.join(SCREENSHOT_DIR, screenshot_name)
        if not os.path.exists(screen_file):
            print(f"Skipping {filename}: {screen_file} not found")
            continue

        with Image.open(screen_file) as source:
            phone = create_clean_phone(source, target_height=1180)

        top_tint, accent_tone, glow_tone = palette
        bg = create_delicate_bg(card_w, card_h, top_tint, accent_tone, glow_tone, y_start=920, y_end=660)
        draw = ImageDraw.Draw(bg)

        draw.text((75, 80), title, font=title_font, fill=(12, 18, 28, 255))
        draw.multiline_text((75, 175), subtitle, font=sub_font, fill=(45, 60, 72, 255), spacing=10)

        shadow, pad = create_studio_shadow(phone, blur=48, opacity=115, offset_y=28)
        px = (card_w - phone.width) // 2
        py = 330
        bg.paste(shadow, (px - pad, py - pad), shadow)
        bg.paste(phone, (px, py), phone)

        out_path = os.path.join(OUT_CARDS_DIR, filename)
        bg.convert("RGB").save(out_path, "WEBP", quality=92, method=6)
        print(f"Generated Feature Card: {out_path}")

def main():
    print("Generating delicate, refined Levyra showcase assets...")
    try:
        generate_hero_panoramic_showcase()
    except (FileNotFoundError, OSError) as e:
        print(f"Skipping hero panoramic showcase ({e})")
    generate_feature_cards()
    print("Showcase generation completed successfully!")

if __name__ == "__main__":
    main()
