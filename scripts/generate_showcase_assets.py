import os
import math
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageEnhance

SCREENSHOT_DIR = os.environ.get(
    "LEVYRA_SCREENSHOT_DIR",
    os.path.join(os.path.expanduser("~"), "Desktop", "screenshots")
)
DOWNLOADS_DIR = os.environ.get(
    "LEVYRA_DOWNLOADS_DIR",
    os.path.join(os.path.expanduser("~"), "Downloads")
)
OUT_SHOWCASE_DIR = r"docs\assets\showcase"
OUT_CARDS_DIR = os.path.join(OUT_SHOWCASE_DIR, "cards")

os.makedirs(OUT_SHOWCASE_DIR, exist_ok=True)
os.makedirs(OUT_CARDS_DIR, exist_ok=True)

def get_font(size, bold=False):
    """Loads a TrueType font across Windows/Linux/macOS or falls back safely to default font."""
    candidates = [
        r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf",
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/System/Library/Fonts/SFCompactText-Bold.ttf" if bold else "/System/Library/Fonts/SFCompactText.ttf"
    ]
    for font_path in candidates:
        if os.path.exists(font_path):
            try:
                return ImageFont.truetype(font_path, size)
            except OSError:
                continue
    try:
        return ImageFont.load_default(size)
    except TypeError:
        return ImageFont.load_default()

def create_clean_phone(source_img, target_height=1060):
    """Renders a realistic dark titanium phone frame with punch-hole camera and subtle glass reflection."""
    src_w, src_h = source_img.size
    screen_h = target_height
    screen_w = int(screen_h * src_w / src_h)

    bezel = 14
    phone_w = screen_w + bezel * 2
    phone_h = screen_h + bezel * 2
    outer_r = 46
    inner_r = 36

    screen_resized = source_img.resize((screen_w, screen_h), Image.Resampling.LANCZOS).convert("RGBA")
    screen_resized = ImageEnhance.Contrast(screen_resized).enhance(1.05)
    screen_resized = ImageEnhance.Sharpness(screen_resized).enhance(1.10)

    # Subtle glass sheen
    sheen = Image.new("RGBA", (screen_w, screen_h), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(sheen)
    sdraw.polygon([(0, 0), (screen_w, 0), (0, int(screen_h * 0.40))], fill=(255, 255, 255, 12))
    screen_resized = Image.alpha_composite(screen_resized, sheen)

    screen_mask = Image.new("L", (screen_w, screen_h), 0)
    ImageDraw.Draw(screen_mask).rounded_rectangle((0, 0, screen_w, screen_h), radius=inner_r, fill=255)

    phone = Image.new("RGBA", (phone_w, phone_h), (0, 0, 0, 0))
    pdraw = ImageDraw.Draw(phone)

    # Frame
    pdraw.rounded_rectangle((0, 0, phone_w, phone_h), radius=outer_r, fill=(20, 23, 30, 255))
    pdraw.rounded_rectangle((0, 0, phone_w, phone_h), radius=outer_r, outline=(255, 255, 255, 50), width=1)

    phone.paste(screen_resized, (bezel, bezel), screen_mask)

    # Speaker slit
    speaker_w = 44
    speaker_h = 3
    speaker_x = (phone_w - speaker_w) // 2
    pdraw.rounded_rectangle((speaker_x, 5, speaker_x + speaker_w, 8), radius=2, fill=(10, 10, 12, 220))

    # Camera punch hole
    cam_r = 9
    cam_x = phone_w // 2
    cam_y = bezel + 22
    pdraw.ellipse((cam_x - cam_r, cam_y - cam_r, cam_x + cam_r, cam_y + cam_r), fill=(8, 10, 14, 255))

    return phone

def create_studio_shadow(phone, blur=45, opacity=100, offset_y=26):
    """Generates a natural two-stage studio drop shadow."""
    pad = blur * 2 + abs(offset_y)
    sw = phone.width + pad * 2
    sh = phone.height + pad * 2
    shadow = Image.new("RGBA", (sw, sh), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow)

    sdraw.rounded_rectangle(
        (pad + 8, pad + 10, pad + phone.width - 8, pad + phone.height + 6),
        radius=46,
        fill=(10, 15, 26, int(opacity * 0.55))
    )
    sdraw.rounded_rectangle(
        (pad, pad + offset_y, pad + phone.width, pad + phone.height + offset_y),
        radius=46,
        fill=(10, 15, 26, opacity)
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(blur))
    return shadow, pad

def create_minimal_wave_bg(w, h, top_color, accent_color, y_start, y_end):
    """Renders a clean, minimal dual-tone canvas with a smooth organic curve. Zero white lines or busy decorations."""
    canvas = Image.new("RGBA", (w, h), (*top_color, 255))
    draw = ImageDraw.Draw(canvas)

    curve_pts = []
    for x in range(0, w + 1, 4):
        t = x / w
        t_curve = t * t * (3.0 - 2.0 * t)
        y = y_start + (y_end - y_start) * t_curve
        curve_pts.append((x, y))

    poly = [(0, h)] + [(0, curve_pts[0][1])] + curve_pts + [(w, h)]
    draw.polygon(poly, fill=(*accent_color, 255))
    return canvas

def generate_hero_panoramic_showcase():
    """Generates 00_levyra_hero_wall_player.webp wide panoramic hero banner."""
    home_file = os.path.join(SCREENSHOT_DIR, "Screenshot_20260926_171253_LEVYRA.jpg")
    np_file = os.path.join(SCREENSHOT_DIR, "Screenshot_20260926_193948_LEVYRA.jpg")
    explore_file = os.path.join(SCREENSHOT_DIR, "Screenshot_20260927_140841_LEVYRA.jpg")

    if not all(os.path.exists(f) for f in [home_file, np_file, explore_file]):
        raise FileNotFoundError("Missing screenshots for hero banner")

    wall_w, wall_h = 2400, 1200
    canvas = Image.new("RGBA", (wall_w, wall_h), (15, 23, 42, 255))

    # Ambient studio lighting
    aura = Image.new("RGBA", (wall_w, wall_h), (0, 0, 0, 0))
    adraw = ImageDraw.Draw(aura)
    adraw.ellipse((1400, 200, 2200, 1000), fill=(105, 140, 165, 75))
    adraw.ellipse((1000, 300, 1800, 1100), fill=(185, 145, 110, 65))
    aura = aura.filter(ImageFilter.GaussianBlur(140))
    canvas = Image.alpha_composite(canvas, aura)

    draw = ImageDraw.Draw(canvas)
    draw.text((120, 480), "LEVYRA", font=get_font(96, bold=True), fill=(255, 255, 255, 255))
    draw.text((120, 600), "MUSIC, KEPT PERSONAL.", font=get_font(34, bold=True), fill=(147, 197, 253, 255))
    draw.multiline_text(
        (120, 670),
        "Native playback on Android and Windows.\nZero ads. Zero telemetry. Your library stays yours.",
        font=get_font(26),
        fill=(203, 213, 225, 255),
        spacing=12
    )

    with Image.open(explore_file) as s1:
        p1 = create_clean_phone(s1, target_height=780).rotate(10, resample=Image.Resampling.BICUBIC, expand=True)
    with Image.open(np_file) as s2:
        p2 = create_clean_phone(s2, target_height=920)
    with Image.open(home_file) as s3:
        p3 = create_clean_phone(s3, target_height=820).rotate(-8, resample=Image.Resampling.BICUBIC, expand=True)

    sh1, pad1 = create_studio_shadow(p1, blur=40, opacity=110, offset_y=24)
    sh2, pad2 = create_studio_shadow(p2, blur=45, opacity=130, offset_y=28)
    sh3, pad3 = create_studio_shadow(p3, blur=40, opacity=110, offset_y=24)

    canvas.paste(sh1, (1080 - pad1, 260 - pad1), sh1)
    canvas.paste(p1, (1080, 260), p1)

    canvas.paste(sh3, (1780 - pad3, 240 - pad3), sh3)
    canvas.paste(p3, (1780, 240), p3)

    canvas.paste(sh2, (1400 - pad2, 160 - pad2), sh2)
    canvas.paste(p2, (1400, 160), p2)

    out_path = os.path.join(OUT_SHOWCASE_DIR, "00_levyra_hero_wall_player.webp")
    canvas.convert("RGB").save(out_path, "WEBP", quality=92, method=6)
    print("Generated Panoramic Hero Showcase:", out_path)

def generate_feature_cards():
    """Generates all 20 feature cards with delicate, muted dual-tone minimal design (ZERO pink)."""
    card_w, card_h = 900, 1600
    title_font = get_font(58, bold=True)
    sub_font = get_font(28, bold=False)

    # 1. CONNECTED HERO CARDS (01 & 02)
    pano_w = card_w * 2
    pano_h = card_h
    # Delicate Nordic air blue
    pano_bg = create_minimal_wave_bg(pano_w, pano_h, (238, 243, 248), (130, 165, 195), y_start=1150, y_end=620)

    home_file = os.path.join(SCREENSHOT_DIR, "Screenshot_20260926_171253_LEVYRA.jpg")
    with Image.open(home_file) as source:
        phone_hero = create_clean_phone(source, target_height=1280)

    rotated_hero = phone_hero.rotate(25, resample=Image.Resampling.BICUBIC, expand=True)
    rot_shadow, rpad = create_studio_shadow(rotated_hero, blur=48, opacity=100, offset_y=28)

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
        fill=(30, 45, 65, 255),
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
        fill=(75, 82, 92, 255),
        spacing=10
    )

    card_01 = pano_bg.crop((0, 0, card_w, card_h))
    card_02 = pano_bg.crop((card_w, 0, pano_w, card_h))
    card_01.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "01_home.webp"), "WEBP", quality=92, method=6)
    card_02.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "02_stay_with_the_song.webp"), "WEBP", quality=92, method=6)
    print("Generated Hero Cards: 01_home.webp, 02_stay_with_the_song.webp")

    # 2. CARDS 03 TO 20 (Clean Single Cards with Delicate, Non-Aggressive, Zero-Pink Tones)
    single_specs = [
        # (filename, screenshot_file, title, subtitle, top_color, accent_color)
        ("03_now_playing.webp", "Screenshot_20260926_193948_LEVYRA.jpg", "Stay with the song", "One focused player for audio and video.\nSynced lyrics move with every beat.", (248, 240, 234), (195, 155, 130)),
        ("04_player_deck.webp", "Screenshot_20260929_195520_LEVYRA.jpg", "Style your player", "Canvas, card deck, or classic vinyl.\nSwitch your stage seamlessly.", (238, 242, 246), (115, 135, 155)),
        ("05_explore_mix.webp", "Screenshot_20260927_140841_LEVYRA.jpg", "Explore and mix", "Live radio, fresh currents, and custom\nsliders between familiar and new.", (236, 244, 248), (120, 160, 185)),
        ("06_artist_profile.webp", "Screenshot_20260926_194603_LEVYRA.jpg", "Meet the artist", "Full discography, singles, biographies,\nand top tracks in one tap.", (242, 240, 246), (135, 125, 160)),
        ("07_genres.webp", "Screenshot_20260929_194827_LEVYRA.jpg", "Pick a direction", "Move through moods, vibes, and genres\ncrafted for every moment.", (238, 245, 240), (130, 168, 148)),
        ("08_audio_tuning.webp", "Screenshot_20260926_194845_LEVYRA.jpg", "Shape the playback", "Sleep timer, tempo tuning, loudness norm,\nand advanced audio engine.", (240, 242, 246), (110, 125, 150)),
        ("09_album.webp", "Screenshot_20260926_194706_LEVYRA.jpg", "Open the album", "High-resolution artwork, release info,\nand complete tracklists.", (248, 242, 235), (185, 150, 115)),
        ("10_search.webp", "Screenshot_20260926_194736_LEVYRA.jpg", "Find it instantly", "Recent searches, suggestions, and\ninstant matching across your music.", (236, 244, 248), (110, 155, 175)),
        ("11_collections.webp", "Screenshot_20260927_132323_LEVYRA.jpg", "Curated for you", "Handpicked playlists and gems\nrevolving around what you love.", (244, 240, 248), (145, 130, 165)),
        ("12_listening_rhythm.webp", "Screenshot_20260927_131943_LEVYRA.jpg", "Your listening rhythm", "Activity heatmaps, peak hours, and\nyour personal listening cadence.", (236, 246, 244), (115, 160, 150)),
        ("13_your_orbit.webp", "Screenshot_20260929_195235_LEVYRA.jpg", "In your orbit", "The songs and artists that always return\nto your rotation.", (240, 240, 248), (125, 130, 170)),
        ("14_listening_pulse.webp", "Screenshot_20260926_193717_LEVYRA.jpg", "Keep it personal", "Private listening stats and charts,\ncomputed strictly on your device.", (238, 242, 248), (115, 145, 180)),
        ("15_artist_playlists.webp", "Screenshot_20260929_201454_LEVYRA.jpg", "Artist playlists", "Curated sets, tours, and the\nessential catalog of every artist.", (238, 242, 246), (110, 135, 165)),
        ("16_settings_vault.webp", "Screenshot_20260929_194944_LEVYRA.jpg", "Tailor every detail", "Audio, design, gestures, and local\nsingle-file Vault backups.", (240, 242, 245), (120, 130, 142)),
        ("17_new_releases.webp", "Screenshot_20260927_132248_LEVYRA.jpg", "Fresh off the stage", "New singles and albums updated\nevery week directly from artists.", (248, 242, 238), (185, 135, 115)),
        ("18_fresh_currents.webp", "Screenshot_20260929_194853_LEVYRA.jpg", "Discovery stream", "Explore live stations, genre charts,\nand community soundscapes.", (235, 245, 248), (105, 155, 170)),
        ("19_featured_artists.webp", "Screenshot_20260905_135618_LEVYRA.jpg", "Featured artists", "Discover local and global artists,\ncurated collections, and albums.", (244, 242, 248), (130, 125, 155)),
        ("20_soundstage.webp", "Screenshot_20260926_193948_LEVYRA.jpg", "Pure soundstage", "Experience lossless decoding and\nuncompromised audio fidelity.", (238, 244, 248), (105, 140, 165)),
    ]

    for filename, screenshot_name, title, subtitle, top_col, accent_col in single_specs:
        screen_file = os.path.join(SCREENSHOT_DIR, screenshot_name)
        if not os.path.exists(screen_file):
            print(f"Skipping {filename}: {screen_file} not found")
            continue

        with Image.open(screen_file) as source:
            phone = create_clean_phone(source, target_height=1060)

        bg = create_minimal_wave_bg(card_w, card_h, top_col, accent_col, y_start=960, y_end=680)
        draw = ImageDraw.Draw(bg)

        draw.text((75, 95), title, font=title_font, fill=(20, 23, 29, 255))
        draw.multiline_text((75, 175), subtitle, font=sub_font, fill=(75, 82, 92, 255), spacing=10)

        shadow, pad = create_studio_shadow(phone, blur=45, opacity=100, offset_y=26)
        px = (card_w - phone.width) // 2
        py = 390
        bg.paste(shadow, (px - pad, py - pad), shadow)
        bg.paste(phone, (px, py), phone)

        out_path = os.path.join(OUT_CARDS_DIR, filename)
        bg.convert("RGB").save(out_path, "WEBP", quality=92, method=6)
        print(f"Generated Feature Card: {out_path}")

def main():
    print("Generating minimal, refined Levyra showcase assets...")
    try:
        generate_hero_panoramic_showcase()
    except (FileNotFoundError, OSError) as e:
        print(f"Skipping hero panoramic showcase ({e})")
    generate_feature_cards()
    print("Showcase generation completed successfully!")

if __name__ == "__main__":
    main()
