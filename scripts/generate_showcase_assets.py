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
        os.path.join(r"C:\Users\Luca Drogo\Downloads\Nuova cartella", filename),
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
    img = enhancer.enhance(1.05)
    enhancer = ImageEnhance.Color(img)
    img = enhancer.enhance(1.06)
    enhancer = ImageEnhance.Sharpness(img)
    img = enhancer.enhance(1.10)
    return img

def create_simpmusic_bg(w=607, h=1080, reverse=False):
    arr = np.zeros((h, w, 3), dtype=np.uint8)
    c_left = np.array([255, 255, 255], dtype=np.float32)
    c_right = np.array([141, 204, 240], dtype=np.float32)
    if reverse:
        c_left, c_right = c_right, c_left
    x_ratio = np.linspace(0, 1, w).reshape(1, w, 1)
    col = c_left * (1 - x_ratio) + c_right * x_ratio
    arr[:, :] = np.clip(col, 0, 255).astype(np.uint8)
    return Image.fromarray(arr, mode="RGB").convert("RGBA")

def create_s26_ultra_mockup(screen_img, screen_w=430):
    screen_img = enhance_screenshot(screen_img)
    orig_w, orig_h = screen_img.size
    aspect = orig_h / float(orig_w)
    screen_h = int(screen_w * aspect)
    screen_res = screen_img.resize((screen_w, screen_h), Image.Resampling.LANCZOS)

    bezel = 6
    corner_r = 18
    inner_r = max(4, corner_r - bezel)
    body_w = screen_w + bezel * 2
    body_h = screen_h + bezel * 2

    phone = Image.new("RGBA", (body_w, body_h), (0, 0, 0, 0))
    pdraw = ImageDraw.Draw(phone)

    # Samsung Galaxy S26 Ultra Titanium frame
    pdraw.rounded_rectangle((0, 0, body_w - 1, body_h - 1), radius=corner_r, fill=(28, 29, 34, 255))
    pdraw.rounded_rectangle((1, 1, body_w - 2, body_h - 2), radius=corner_r - 1, fill=(14, 15, 18, 255))

    # Mask screen
    screen_mask = Image.new("L", (screen_w, screen_h), 0)
    smdraw = ImageDraw.Draw(screen_mask)
    smdraw.rounded_rectangle((0, 0, screen_w - 1, screen_h - 1), radius=inner_r, fill=255)
    phone.paste(screen_res, (bezel, bezel), screen_mask)

    # Inner bezel border
    pdraw.rounded_rectangle((bezel, bezel, bezel + screen_w - 1, bezel + screen_h - 1), radius=inner_r, outline=(8, 8, 10, 230), width=1)

    # Infinity-O Punch-hole Camera
    cam_diam = 11
    cam_x = body_w // 2 - cam_diam // 2
    cam_y = bezel + 9
    pdraw.ellipse((cam_x, cam_y, cam_x + cam_diam, cam_y + cam_diam), fill=(6, 6, 8, 255))
    pdraw.ellipse((cam_x + 2, cam_y + 2, cam_x + cam_diam - 2, cam_y + cam_diam - 2), fill=(14, 24, 46, 255))
    pdraw.ellipse((cam_x + 4, cam_y + 3, cam_x + 6, cam_y + 5), fill=(130, 175, 235, 180))

    # Micro speaker slit
    pdraw.rounded_rectangle((body_w // 2 - 16, 1, body_w // 2 + 16, 3), radius=1, fill=(40, 42, 48, 255))

    # Diamond-cut outer chamfer
    pdraw.rounded_rectangle((0, 0, body_w - 1, body_h - 1), radius=corner_r, outline=(145, 150, 162, 130), width=1)

    # Side buttons: power & volume on right side
    btn_x = body_w - 1
    btn_col = (45, 47, 52, 255)
    pdraw.line([(btn_x, 180), (btn_x, 230)], fill=btn_col, width=2)
    pdraw.line([(btn_x, 260), (btn_x, 340)], fill=btn_col, width=2)

    return phone

def create_phone_shadow(phone, blur=34, offset_y=18):
    w, h = phone.size
    pad = 70
    cw, ch = w + pad * 2, h + pad * 2 + offset_y
    alpha = phone.split()[-1]
    s = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    s_alpha = Image.new("L", (cw, ch), 0)
    s_alpha.paste(alpha, (pad, pad + offset_y))
    s_alpha = s_alpha.filter(ImageFilter.GaussianBlur(blur))
    s_alpha = ImageEnhance.Brightness(s_alpha).enhance(0.35)
    s.paste((15, 30, 45, 255), (0, 0), s_alpha)
    return s, pad

def render_simpmusic_duo_cut_cards():
    w, h = 607, 1080
    canvas_w = w * 2

    # Canvas background: Left card (0..607) goes white -> cyan, Right card (607..1214) goes cyan -> white
    arr = np.zeros((h, canvas_w, 3), dtype=np.uint8)
    c_white = np.array([255, 255, 255], dtype=np.float32)
    c_cyan = np.array([141, 204, 240], dtype=np.float32)
    r1 = np.linspace(0, 1, w).reshape(1, w, 1)
    r2 = np.linspace(1, 0, w).reshape(1, w, 1)
    col1 = c_white * (1 - r1) + c_cyan * r1
    col2 = c_white * (1 - r2) + c_cyan * r2
    arr[:, :w] = np.clip(col1, 0, 255).astype(np.uint8)
    arr[:, w:] = np.clip(col2, 0, 255).astype(np.uint8)
    canvas = Image.fromarray(arr, mode="RGB").convert("RGBA")

    s_home = resolve_screenshot_path("Screenshot_20260926_171253_LEVYRA.jpg")
    s_player = resolve_screenshot_path("Screenshot_20260926_193948_LEVYRA.jpg")

    angle = 18.0
    # Phone 1 (Taylor Swift - Home), tilted 18 degrees, cuts from card 01 into card 02
    if s_home and os.path.exists(s_home):
        with Image.open(s_home) as src1:
            p1 = create_s26_ultra_mockup(src1, screen_w=430)
        rot1 = p1.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
        s1_shadow, pad1 = create_phone_shadow(rot1, blur=36, offset_y=22)
        p1_x = 180
        p1_y = -100
        canvas.paste(s1_shadow, (p1_x - pad1, p1_y - pad1), s1_shadow)
        canvas.paste(rot1, (p1_x, p1_y), rot1)

    # Phone 2 (The Weeknd - Player) in Card 02, tilted 18 degrees
    if s_player and os.path.exists(s_player):
        with Image.open(s_player) as src2:
            p2 = create_s26_ultra_mockup(src2, screen_w=400)
        rot2 = p2.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
        s2_shadow, pad2 = create_phone_shadow(rot2, blur=32, offset_y=18)
        p2_x = w + 110
        p2_y = 170
        canvas.paste(s2_shadow, (p2_x - pad2, p2_y - pad2), s2_shadow)
        canvas.paste(rot2, (p2_x, p2_y), rot2)

    c1 = canvas.crop((0, 0, w, h))
    c2 = canvas.crop((w, 0, canvas_w, h))

    t_font = get_font(38, bold=True)
    s_font = get_font(20, bold=False)

    # Card 01 typography at bottom
    d1 = ImageDraw.Draw(c1)
    t1 = "Pure listening."
    s1 = "Zero ads, zero accounts. Stream and store locally"
    tw1 = t_font.getbbox(t1)[2] - t_font.getbbox(t1)[0]
    sw1 = s_font.getbbox(s1)[2] - s_font.getbbox(s1)[0]
    d1.text(((w - tw1) // 2, 875), t1, font=t_font, fill=(15, 85, 90, 255))
    d1.text(((w - sw1) // 2, 925), s1, font=s_font, fill=(42, 105, 115, 255))

    # Card 02 typography at top
    d2 = ImageDraw.Draw(c2)
    t2 = "Download & keep."
    s2 = "Clean M4A files in device storage with lyrics"
    tw2 = t_font.getbbox(t2)[2] - t_font.getbbox(t2)[0]
    sw2 = s_font.getbbox(s2)[2] - s_font.getbbox(s2)[0]
    d2.text(((w - tw2) // 2, 58), t2, font=t_font, fill=(15, 85, 90, 255))
    d2.text(((w - sw2) // 2, 110), s2, font=s_font, fill=(42, 105, 115, 255))

    return c1, c2

def render_simpmusic_single_card(screenshot_name, title, subtitle):
    w, h = 607, 1080
    card = create_simpmusic_bg(w, h)
    draw = ImageDraw.Draw(card)

    # Centered Title
    t_font = get_font(38, bold=True)
    t_bbox = t_font.getbbox(title)
    tw = t_bbox[2] - t_bbox[0]
    draw.text(((w - tw) // 2, 58), title, font=t_font, fill=(15, 85, 90, 255))

    # Centered Subtitle
    s_font = get_font(20, bold=False)
    s_bbox = s_font.getbbox(subtitle)
    sw = s_bbox[2] - s_bbox[0]
    draw.text(((w - sw) // 2, 110), subtitle, font=s_font, fill=(42, 105, 115, 255))

    # Phone Mockup
    s_path = resolve_screenshot_path(screenshot_name)
    if s_path and os.path.exists(s_path):
        with Image.open(s_path) as src:
            phone = create_s26_ultra_mockup(src, screen_w=430)
        shadow, pad = create_phone_shadow(phone)
        px = (w - phone.width) // 2
        py = 154
        card.paste(shadow, (px - pad, py - pad), shadow)
        card.paste(phone, (px, py), phone)

    return card.crop((0, 0, w, h))

# The full suite of 18 remaining feature cards, matching SimpMusic clean design
SIMPMUSIC_SPECS = [
    # Card 03: Synced lyrics
    ("screen-lyrics.jpg", "Synced lyrics", "Real-time synchronized lyrics and romanization",
     ["03_now_playing.webp"]),

    # Card 04: Player decks
    ("Screenshot_20261010_190538_LEVYRA.jpg", "Player decks", "Immersive, Canvas Card, Artwork and Editorial",
     ["04_player_deck.webp"]),

    # Card 05: Explore
    ("Screenshot_20261010_132210_LEVYRA.jpg", "Fresh currents", "Discover trending music, new releases and videos",
     ["05_explore_stage.webp", "05_explore.webp"]),

    # Card 06: Levyra Mix
    ("Screenshot_20261010_132217_LEVYRA.jpg", "Levyra Mix", "Algorithmic discovery stations built for your taste",
     ["06_levyra_mix_stage.webp", "06_levyra_mix.webp"]),

    # Card 07: Moods & Genres
    ("Screenshot_20261010_132249_LEVYRA.jpg", "Moods & genres", "Curated categories, moods and musical traditions",
     ["07_genres.webp"]),

    # Card 08: Audio Architecture & Equalizer
    ("Screenshot_20261010_190726_LEVYRA.jpg", "Audio architecture", "10-band equalizer, tempo and bit-perfect output",
     ["08_audio_tuning.webp"]),

    # Card 09: Album Details
    ("Screenshot_20260926_194706_LEVYRA.jpg", "Open the album", "High-resolution artwork, release info and tracklist",
     ["09_album.webp"]),

    # Card 10: Instant Search
    ("Screenshot_20261010_145010_LEVYRA.jpg", "Instant search", "Recent searches, suggestions and instant match",
     ["10_search.webp"]),

    # Card 11: Collections
    ("Screenshot_20261010_132136_LEVYRA.jpg", "Collections", "Handpicked playlists revolving around what you love",
     ["11_collections.webp"]),

    # Card 12: Listening Rhythm & Cadence
    ("Screenshot_20260927_131943_LEVYRA.jpg", "Listening rhythm", "Activity heatmaps, peak hours and listening cadence",
     ["12_listening_rhythm.webp"]),

    # Card 13: Your Orbit
    ("Screenshot_20261010_132103_LEVYRA.jpg", "In your orbit", "Top rotation tracks returning to your daily listening",
     ["13_your_orbit.webp"]),

    # Card 14: Private Listening Pulse
    ("Screenshot_20261010_132050_LEVYRA.jpg", "Listening pulse", "Private listening stats computed strictly on device",
     ["14_listening_pulse.webp"]),

    # Card 15: Artist Playlists
    ("Screenshot_20261010_144048_LEVYRA.jpg", "Artist playlists", "Curated essential sets, tours and discography",
     ["15_artist_playlists.webp"]),

    # Card 16: Settings & Vault
    ("Screenshot_20261010_140700_LEVYRA.jpg", "Vault & backup", "Customize Levyra with single-file local vault backups",
     ["16_settings_vault.webp"]),

    # Card 17: New Releases
    ("Screenshot_20261010_144144_LEVYRA.jpg", "New releases", "Fresh singles and albums updated weekly from artists",
     ["17_new_releases.webp"]),

    # Card 18: Artist Profile
    ("Screenshot_20261010_143646_LEVYRA.jpg", "Artist profile", "Full discography, singles, biography and top tracks",
     ["18_artist_profile.webp", "18_fresh_currents.webp"]),

    # Card 19: Top 50 Global Charts
    ("Screenshot_20261011_202032_LEVYRA.jpg", "Top 50 charts", "Explore daily country charts and viral hits worldwide",
     ["19_top_50.webp"]),

    # Card 20: Levyra Jam
    ("Screenshot_20261011_133434_LEVYRA.jpg", "Levyra Jam", "Real-time collaborative sessions on local Wi-Fi",
     ["20_soundstage.webp", "20_levyra_jam.webp"]),
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
    print("Generating SimpMusic-style Showcase Cards...")
    c1, c2 = render_simpmusic_duo_cut_cards()
    c1.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "01_home.webp"), "WEBP", quality=95, method=6)
    c2.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "02_stay_with_the_song.webp"), "WEBP", quality=95, method=6)
    print("Saved SimpMusic-style Keynote Diptych: 01_home.webp, 02_stay_with_the_song.webp")

    for s_name, title, sub, fnames in SIMPMUSIC_SPECS:
        card = render_simpmusic_single_card(s_name, title, sub)
        card_rgb = card.convert("RGB")
        for fname in fnames:
            out_file = os.path.join(OUT_CARDS_DIR, fname)
            card_rgb.save(out_file, "WEBP", quality=95, method=6)
            print(f"Saved SimpMusic-style Card: {out_file}")

if __name__ == "__main__":
    generate_hero_panoramic_showcase()
    generate_feature_cards()
    print("All SimpMusic-style assets generated successfully!")
