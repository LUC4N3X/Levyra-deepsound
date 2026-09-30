import os
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
        r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf",
        r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf",
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf" if bold else "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
        "/System/Library/Fonts/SFCompactText-Bold.ttf" if bold else "/System/Library/Fonts/SFCompactText.ttf",
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

    alpha = phone.getchannel("A")
    shadow_mask = Image.new("L", (sw, sh), 0)
    shadow_mask.paste(alpha, (pad, pad + offset_y))
    shadow_mask = shadow_mask.filter(ImageFilter.GaussianBlur(blur))
    shadow_mask = shadow_mask.point(lambda value: value * opacity // 255)

    shadow = Image.new("RGBA", (sw, sh), (8, 12, 20, 0))
    shadow.putalpha(shadow_mask)
    return shadow, pad

def create_colored_glow(phone, color, blur=50, opacity=85, offset_y=18):
    pad = blur * 2 + abs(offset_y)
    gw = phone.width + pad * 2
    gh = phone.height + pad * 2

    alpha = phone.getchannel("A")
    glow_mask = Image.new("L", (gw, gh), 0)
    glow_mask.paste(alpha, (pad, pad + offset_y))
    glow_mask = glow_mask.filter(ImageFilter.GaussianBlur(blur))
    glow_mask = glow_mask.point(lambda value: value * opacity // 255)

    glow = Image.new("RGBA", (gw, gh), (*color, 0))
    glow.putalpha(glow_mask)
    return glow, pad

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
    Generates 00_levyra_hero_wall_player.webp with compact cinematic proportions (2400x880),
    minimal Spotify-grade cascading phone composition, deep carbon-graphite foundation
    (Charcoal Carbon to Obsidian, ZERO green, ZERO purple behind logo), dual-tier glowing laser rim,
    acoustic sound pressure rings, bold editorial typography (NO badges), and screen-adaptive ambilight.
    """
    canvas_w, canvas_h = 2400, 880
    # Clean obsidian dark foundation
    canvas = Image.new("RGBA", (canvas_w, canvas_h), (7, 8, 12, 255))

    # 1. Subtle, minimalist ambient stage blooms behind the phones (Deep Sapphire, Warm Amber, Cyan - ZERO green!)
    aurora = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    adraw = ImageDraw.Draw(aurora)
    adraw.ellipse((1680, -100, 2380, 420), fill=(45, 75, 160, 42))
    adraw.ellipse((1100, 110, 1780, 750), fill=(225, 110, 20, 50))
    adraw.ellipse((1720, 280, 2380, 840), fill=(0, 180, 220, 36))
    adraw.ellipse((720, 90, 1360, 720), fill=(35, 60, 130, 42))
    aurora = aurora.filter(ImageFilter.GaussianBlur(165))
    canvas = Image.alpha_composite(canvas, aurora)

    # 2. Left Wave: Pure Brushed Carbon & Titanium (Deep Charcoal to Obsidian - ZERO purple!)
    wave_canvas = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    steps = 140
    t = np.linspace(0, 1, steps)
    ys = np.linspace(0, canvas_h, steps)

    # Back Wave: Translucent carbon smoke
    x_start_back = 750
    x_end_back = 480
    c1_x_back = 900
    c2_x_back = 410
    xs_back = (1 - t)**3 * x_start_back + 3 * (1 - t)**2 * t * c1_x_back + 3 * (1 - t) * t**2 * c2_x_back + t**3 * x_end_back

    poly_back = [(0, 0)]
    for x, y in zip(xs_back, ys):
        poly_back.append((int(x), int(y)))
    poly_back.extend([(0, canvas_h)])

    back_mask = Image.new("L", (canvas_w, canvas_h), 0)
    bmdraw = ImageDraw.Draw(back_mask)
    bmdraw.polygon(poly_back, fill=255)

    back_grad = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    bg_arr = np.zeros((canvas_h, canvas_w, 4), dtype=np.uint8)
    for row in range(canvas_h):
        ratio = row / float(canvas_h)
        # Cool carbon graphite tint
        bg_arr[row, :, 0] = int(22 * (1 - ratio) + 12 * ratio)
        bg_arr[row, :, 1] = int(26 * (1 - ratio) + 16 * ratio)
        bg_arr[row, :, 2] = int(36 * (1 - ratio) + 24 * ratio)
        bg_arr[row, :, 3] = 160
    back_grad = Image.fromarray(bg_arr, mode="RGBA")
    wave_canvas.paste(back_grad, (0, 0), back_mask)

    # Front Wave: Sleek Carbon-Titanium Gradient (Charcoal-Carbon to Deep Obsidian)
    x_start = 680
    x_end = 430
    c1_x = 830
    c2_x = 360
    xs = (1 - t)**3 * x_start + 3 * (1 - t)**2 * t * c1_x + 3 * (1 - t) * t**2 * c2_x + t**3 * x_end

    poly_front = [(0, 0)]
    for x, y in zip(xs, ys):
        poly_front.append((int(x), int(y)))
    poly_front.extend([(0, canvas_h)])

    front_mask = Image.new("L", (canvas_w, canvas_h), 0)
    fmdraw = ImageDraw.Draw(front_mask)
    fmdraw.polygon(poly_front, fill=255)

    front_grad = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    fg_arr = np.zeros((canvas_h, canvas_w, 4), dtype=np.uint8)
    for row in range(canvas_h):
        ratio = row / float(canvas_h)
        # Top: Brushed Carbon (24, 28, 38) -> Bottom: Deep Obsidian Carbon (11, 13, 18)
        fg_arr[row, :, 0] = int(24 * (1 - ratio) + 11 * ratio)
        fg_arr[row, :, 1] = int(28 * (1 - ratio) + 14 * ratio)
        fg_arr[row, :, 2] = int(38 * (1 - ratio) + 20 * ratio)
        fg_arr[row, :, 3] = 252
    front_grad = Image.fromarray(fg_arr, mode="RGBA")
    wave_canvas.paste(front_grad, (0, 0), front_mask)

    # Glowing laser rim (Electric Cyan transitioning to Cool Titanium Silver - ZERO purple!)
    rim = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    rdraw = ImageDraw.Draw(rim)
    curve_points = [(int(x), int(y)) for x, y in zip(xs, ys)]

    for i in range(len(curve_points) - 1):
        ratio = i / float(len(curve_points) - 1)
        # Cyan: (56, 189, 248) -> Cool Titanium: (148, 163, 184)
        lr = int(56 * (1 - ratio) + 148 * ratio)
        lg = int(189 * (1 - ratio) + 163 * ratio)
        lb = int(248 * (1 - ratio) + 184 * ratio)
        p1 = curve_points[i]
        p2 = curve_points[i + 1]
        rdraw.line([p1, p2], fill=(lr, lg, lb, 85), width=10)
    rim = rim.filter(ImageFilter.GaussianBlur(10))

    rdraw2 = ImageDraw.Draw(rim)
    for i in range(len(curve_points) - 1):
        ratio = i / float(len(curve_points) - 1)
        lr = int(100 * (1 - ratio) + 185 * ratio)
        lg = int(220 * (1 - ratio) + 200 * ratio)
        lb = int(255 * (1 - ratio) + 220 * ratio)
        rdraw2.line([curve_points[i], curve_points[i + 1]], fill=(lr, lg, lb, 150), width=4)
    rim = rim.filter(ImageFilter.GaussianBlur(3))

    rdraw3 = ImageDraw.Draw(rim)
    for i in range(len(curve_points) - 1):
        ratio = i / float(len(curve_points) - 1)
        lr = int(220 * (1 - ratio) + 245 * ratio)
        lg = int(250 * (1 - ratio) + 250 * ratio)
        lb = int(255 * (1 - ratio) + 255 * ratio)
        rdraw3.line([curve_points[i], curve_points[i + 1]], fill=(lr, lg, lb, 220), width=2)

    wave_canvas = Image.alpha_composite(wave_canvas, rim)

    # Concentric acoustic sound pressure rings radiating from logo center in subtle cyan/silver
    ring_center = (260, 270)
    ring_layer = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    rndraw = ImageDraw.Draw(ring_layer)
    for radius, alpha in [(200, 22), (300, 16), (410, 12), (530, 8)]:
        rndraw.arc(
            (ring_center[0] - radius, ring_center[1] - radius, ring_center[0] + radius, ring_center[1] + radius),
            start=290, end=430,
            fill=(56, 189, 248, alpha),
            width=2
        )
    ring_layer = ring_layer.filter(ImageFilter.GaussianBlur(2))
    wave_canvas = Image.alpha_composite(wave_canvas, ring_layer)

    canvas = Image.alpha_composite(canvas, wave_canvas)

    # 3. Cascading Phones (-19° tilt, clean Spotify rhythm, 3 parallel columns + edge accent)
    rot_angle = -19
    phones_spec = [
        # --- COLUMN 1 (Left) ---
        ("Screenshot_20260927_140841_LEVYRA.jpg", 670, 860, -110, 1, (20, 130, 230)),
        ("Screenshot_20260926_171253_LEVYRA.jpg", 730, 950, 300, 3, (50, 85, 175)),

        # --- COLUMN 2 (Center Hero) ---
        ("Screenshot_20260926_194706_LEVYRA.jpg", 690, 1350, -160, 2, (210, 135, 35)),
        ("Screenshot_20260926_193948_LEVYRA.jpg", 780, 1440, 205, 5, (235, 105, 20)),

        # --- COLUMN 3 (Right) ---
        ("Screenshot_20260929_194827_LEVYRA.jpg", 680, 1840, -130, 2, (40, 80, 175)),
        ("Screenshot_20260926_194603_LEVYRA.jpg", 735, 1930, 260, 4, (0, 190, 215)),

        # --- COLUMN 4 (Edge Accent) ---
        ("Screenshot_20260929_201454_LEVYRA.jpg", 660, 2260, 160, 2, (45, 95, 175)),
    ]

    phones_spec.sort(key=lambda s: s[4])

    for filename, height, px, py, z, ambilight_col in phones_spec:
        s_path = resolve_screenshot_path(filename)
        if not s_path:
            continue
        with Image.open(s_path) as src:
            phone = create_clean_phone(src, target_height=height)
        rotated = phone.rotate(rot_angle, resample=Image.Resampling.BICUBIC, expand=True)

        glow, gpad = create_colored_glow(rotated, ambilight_col, blur=54, opacity=65, offset_y=18)
        canvas.paste(glow, (px - gpad, py - gpad), glow)

        shadow, pad = create_studio_shadow(rotated, blur=44, opacity=145, offset_y=26)
        canvas.paste(shadow, (px - pad, py - pad), shadow)

        canvas.paste(rotated, (px, py), rotated)

    # 4. Editorial Left Branding (Pure Carbon & Titanium - ZERO BADGES, ZERO PURPLE!)
    draw = ImageDraw.Draw(canvas)

    # 3D Logo Lockup
    logo_file = LOGO_PATH if os.path.exists(LOGO_PATH) else r"app\src\main\res\drawable\levyra_logo.png"
    with Image.open(logo_file) as l_src:
        logo = l_src.convert("RGBA")
        bbox = logo.getbbox()
        logo_crop = logo.crop(bbox)
        target_logo_w = 400
        ratio = target_logo_w / float(logo_crop.width)
        target_logo_h = int(logo_crop.height * ratio)
        logo_res = logo_crop.resize((target_logo_w, target_logo_h), Image.Resampling.LANCZOS)

    logo_x = 80
    logo_y = 110
    canvas.paste(logo_res, (logo_x, logo_y), logo_res)

    # Bold Editorial Headline in Pure White & Electric Cyan
    text_y = logo_y + target_logo_h + 24
    draw.text((logo_x + 4, text_y), "PURE SOUND.", font=get_font(40, bold=True), fill=(255, 255, 255, 255))
    text_y += 48
    draw.text((logo_x + 4, text_y), "ZERO NOISE.", font=get_font(40, bold=True), fill=(56, 189, 248, 255))

    # Editorial micro-manifesto
    text_y += 56
    draw.text((logo_x + 4, text_y), "Hi-Fi Streaming & Offline Vault", font=get_font(20, bold=True), fill=(210, 225, 245, 240))
    text_y += 30
    draw.text((logo_x + 4, text_y), "Zero ads. Zero tracking. 100% on your device.", font=get_font(18, bold=False), fill=(148, 163, 184, 215))

    # Sleek platform signature
    text_y += 38
    draw.text((logo_x + 4, text_y), "ANDROID • WINDOWS", font=get_font(15, bold=True), fill=(120, 140, 165, 190))

    out_path = os.path.join(OUT_SHOWCASE_DIR, "00_levyra_hero_wall_player.webp")
    canvas.convert("RGB").save(out_path, "WEBP", quality=95, method=6)
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

    home_file = resolve_screenshot_path("Screenshot_20260926_171253_LEVYRA.jpg")
    if not home_file:
        print("Skipping hero cards: home screenshot not found")
        return
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
        ("03_now_playing.webp", "screen-lyrics.jpg", "Follow every line", "Synced lyrics move\nwith the music.", C_CASHMERE),
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
        screen_file = resolve_screenshot_path(screenshot_name)
        if not screen_file:
            print(f"Skipping {filename}: {screenshot_name} not found")
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
