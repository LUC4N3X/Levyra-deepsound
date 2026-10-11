import os
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageEnhance, ImageChops, ImageOps

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

def create_clean_phone(screen_img, target_height=1260):
    screen_img = enhance_screenshot(screen_img)
    ss = 2
    orig_w, orig_h = screen_img.size
    aspect = orig_w / float(orig_h)
    screen_h = int(target_height * ss)
    screen_w = int(screen_h * aspect)
    screen_res = screen_img.resize((screen_w, screen_h), Image.Resampling.LANCZOS)

    bezel = max(12, int(screen_w * 0.019))
    corner_radius = int(screen_w * 0.044)
    inner_radius = max(8, corner_radius - bezel)
    btn_protrusion = max(4, int(screen_w * 0.0055))

    body_w = screen_w + bezel * 2
    body_h = screen_h + bezel * 2
    phone_w = body_w + btn_protrusion
    phone_h = body_h

    phone = Image.new("RGBA", (phone_w, phone_h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(phone)

    vol_top = int(body_h * 0.19)
    vol_bot = int(body_h * 0.305)
    pwr_top = int(body_h * 0.36)
    pwr_bot = int(body_h * 0.425)
    btn_x0 = body_w - bezel // 2
    btn_x1 = phone_w - 1
    btn_r = max(3, btn_protrusion // 2)

    draw.rounded_rectangle(
        (btn_x0, vol_top, btn_x1, vol_bot),
        radius=btn_r,
        fill=(78, 86, 102, 255),
        outline=(172, 184, 204, 235),
        width=max(1, ss // 2),
    )
    draw.rounded_rectangle(
        (btn_x0, pwr_top, btn_x1, pwr_bot),
        radius=btn_r,
        fill=(88, 96, 112, 255),
        outline=(186, 198, 218, 245),
        width=max(1, ss // 2),
    )

    draw.rounded_rectangle(
        (0, 0, body_w - 1, body_h - 1),
        radius=corner_radius,
        fill=(38, 42, 52, 255),
        outline=(130, 142, 160, 255),
        width=max(2, int(1.8 * ss)),
    )
    rim_w = max(1, int(1.5 * ss))
    draw.rounded_rectangle(
        (ss, ss, body_w - 1 - ss, body_h - 1 - ss),
        radius=max(6, corner_radius - ss),
        outline=(232, 242, 254, 165),
        width=rim_w,
    )

    inner_pad = max(3, int(bezel * 0.34))
    draw.rounded_rectangle(
        (inner_pad, inner_pad, body_w - 1 - inner_pad, body_h - 1 - inner_pad),
        radius=max(6, corner_radius - inner_pad),
        fill=(8, 10, 14, 255),
        outline=(30, 36, 46, 255),
        width=max(1, ss // 2),
    )

    screen_mask = Image.new("L", (screen_w, screen_h), 0)
    sdraw = ImageDraw.Draw(screen_mask)
    sdraw.rounded_rectangle((0, 0, screen_w - 1, screen_h - 1), radius=inner_radius, fill=255)

    screen_layer = screen_res.convert("RGBA")
    xs = np.linspace(0.0, 1.0, screen_w, dtype=np.float32)[None, :]
    ys = np.linspace(0.0, 1.0, screen_h, dtype=np.float32)[:, None]
    proj = xs * 0.70 + (1.0 - ys) * 0.45
    sheen_alpha = np.clip((proj - 0.48) * 55.0, 0.0, 26.0).astype(np.uint8)
    sheen_arr = np.zeros((screen_h, screen_w, 4), dtype=np.uint8)
    sheen_arr[:, :, :3] = 255
    sheen_arr[:, :, 3] = sheen_alpha
    glass_sheen = Image.fromarray(sheen_arr, mode="RGBA")
    screen_layer = Image.alpha_composite(screen_layer, glass_sheen)

    phone.paste(screen_layer, (bezel, bezel), screen_mask)

    draw.rounded_rectangle(
        (bezel - 1, bezel - 1, bezel + screen_w, bezel + screen_h),
        radius=inner_radius + 1,
        outline=(18, 22, 28, 240),
        width=max(1, ss // 2),
    )

    cam_r = max(4, int(screen_w * 0.0135))
    cam_x = body_w // 2
    cam_y = bezel + int(screen_h * 0.0135)
    draw.ellipse(
        (cam_x - cam_r, cam_y - cam_r, cam_x + cam_r, cam_y + cam_r),
        fill=(6, 8, 12, 255),
        outline=(32, 38, 50, 210),
        width=max(1, ss // 2),
    )
    reflex_r = max(1, cam_r // 3)
    draw.ellipse(
        (cam_x - reflex_r - 1, cam_y - reflex_r - 1, cam_x + reflex_r - 1, cam_y + reflex_r - 1),
        fill=(75, 125, 205, 185),
    )

    spk_w = int(screen_w * 0.11)
    spk_h = max(2, int(bezel * 0.22))
    spk_x = (body_w - spk_w) // 2
    spk_y = max(2, (bezel - spk_h) // 2)
    draw.rounded_rectangle(
        (spk_x, spk_y, spk_x + spk_w, spk_y + spk_h),
        radius=max(1, spk_h // 2),
        fill=(52, 58, 70, 255),
    )

    return phone.resize((phone_w // ss, phone_h // ss), Image.Resampling.LANCZOS)


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
    top_arr = np.zeros((height, width, 4), dtype=np.uint8)
    y_ratios = np.linspace(0.0, 1.0, height)[:, None]
    for ch in range(3):
        start_v = min(255, int(top_tint[ch] + 5))
        end_v = max(0, int(top_tint[ch] - 10))
        top_arr[:, :, ch] = (start_v * (1.0 - y_ratios) + end_v * y_ratios).astype(np.uint8)
    top_arr[:, :, 3] = 255
    card = Image.fromarray(top_arr, mode="RGBA")

    w2, h2 = width * 2, height * 2
    steps = 140
    t = np.linspace(0.0, 1.0, steps)
    xs = np.linspace(0.0, w2, steps)

    y1_2 = y_start * 2
    y2_2 = y_end * 2
    span = y2_2 - y1_2
    c1_y = y1_2 + span * 0.18 - 130
    c2_y = y2_2 - span * 0.18 + 130
    ys = (1 - t)**3 * y1_2 + 3 * (1 - t)**2 * t * c1_y + 3 * (1 - t) * t**2 * c2_y + t**3 * y2_2

    secondary_layer = Image.new("RGBA", (w2, h2), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(secondary_layer)
    sec_ys = ys - 88 * np.sin(t * math.pi) - 36
    sec_poly = [(0, int(sec_ys[0]))] + [(int(x), int(y)) for x, y in zip(xs, sec_ys)] + [(w2, h2), (0, h2)]
    mid_tone = tuple(int(top_tint[i] * 0.56 + accent_tone[i] * 0.44) for i in range(3))
    sdraw.polygon(sec_poly, fill=(*mid_tone, 150))
    card = Image.alpha_composite(card, secondary_layer.resize((width, height), Image.Resampling.LANCZOS))

    wave_layer = Image.new("RGBA", (w2, h2), (0, 0, 0, 0))
    wdraw = ImageDraw.Draw(wave_layer)
    poly = [(0, int(ys[0]))] + [(int(x), int(y)) for x, y in zip(xs, ys)] + [(w2, h2), (0, h2)]
    wdraw.polygon(poly, fill=(*accent_tone, 255))
    crest_pts = [(int(x), int(y)) for x, y in zip(xs, ys)]
    wdraw.line(crest_pts, fill=(255, 255, 255, 175), width=4)

    wave_smooth = wave_layer.resize((width, height), Image.Resampling.LANCZOS)
    card = Image.alpha_composite(card, wave_smooth)

    if glow_tone:
        glow_canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        gdraw = ImageDraw.Draw(glow_canvas)
        cx = width // 2
        gdraw.ellipse((cx - 310, 480, cx + 310, 1260), fill=(*glow_tone, 62))
        glow_canvas = glow_canvas.filter(ImageFilter.GaussianBlur(95))
        card = Image.alpha_composite(card, glow_canvas)

    border_draw = ImageDraw.Draw(card)
    border_col = tuple(max(0, int(accent_tone[i] - 18)) for i in range(3))
    border_draw.rectangle((0, 0, width - 1, height - 1), outline=(*border_col, 90), width=2)

    return card

def generate_hero_panoramic_showcase():
    """
    Generates 00_levyra_hero_wall_player.webp with compact cinematic proportions (2400x880) matching main,
    an evocative Apple Music-inspired cinematic loft background with expressive motion blur choreography,
    masterfully graded in Cosmic Sapphire Blue (velvety midnight shadows, rich royal sapphire midtones, and
    ice-cyan specular highlights), a prominent 3D Levyra DeepSound logo, refined editorial typography, and an
    authentic -20° cascading phone flight in Spotify style with crystal-clear unclouded screens.
    """
    canvas_w, canvas_h = 2400, 880

    candidates = [
        r"C:\Users\Luca Drogo\Downloads\Danza e musica in luce neon.png",
        os.path.join(OUT_SHOWCASE_DIR, "levyra_hero_cinematic_chatgpt.png"),
        r"C:\Users\Luca Drogo\Downloads\Immagine ChatGPT 30 set 2026, 21_37_37.png",
        r"C:\Users\Luca Drogo\Downloads\Immagine ChatGPT 30 set 2026, 21_31_11.png",
        resolve_screenshot_path("levyra_hero_cinematic_chatgpt.png"),
    ]
    src_file = next((c for c in candidates if c and os.path.exists(c)), None)
    if src_file:
        with Image.open(src_file) as im:
            im = im.convert("RGB")
            out_path = os.path.join(OUT_SHOWCASE_DIR, "00_levyra_hero_wall_player.webp")
            im.save(out_path, "WEBP", quality=95, method=6)
            print(f"Generated Panoramic Hero Showcase at original dimensions {im.size}:", out_path)
            return

    # 1. Load and prepare cinematic motion photography background
    bg_file = os.path.join(OUT_SHOWCASE_DIR, "apple_music_loft_dancer_bg.jpg")
    if not os.path.exists(bg_file):
        bg_file = resolve_screenshot_path("apple_music_loft_dancer_bg.jpg")

    if bg_file and os.path.exists(bg_file):
        with Image.open(bg_file) as d_img:
            d_img = d_img.convert("RGB")
            d_img = ImageOps.mirror(d_img)
            # Scale width to 2600 to provide horizontal framing leeway
            scale = 2600 / float(d_img.width)
            new_h = int(d_img.height * scale)
            d_resized = d_img.resize((2600, new_h), Image.Resampling.LANCZOS)
            # Crop so dancer is centered at x ~ 720 in clear open air
            crop_x = 420
            crop_y = int((new_h - canvas_h) * 0.45)
            d_crop = d_resized.crop((crop_x, crop_y, crop_x + canvas_w, crop_y + canvas_h))

        # 3-Way Hollywood Split-Tone Grade for Cosmic Sapphire Blue:
        gray = np.array(d_crop.convert("L"), dtype=np.float32) / 255.0
        r_arr = np.clip((gray**1.28) * 235 + (1 - gray) * 6, 0, 255).astype(np.uint8)
        g_arr = np.clip((gray**1.08) * 250 + (1 - gray) * 12, 0, 255).astype(np.uint8)
        b_arr = np.clip((gray**0.80) * 255 + (1 - gray) * 36, 0, 255).astype(np.uint8)

        graded_arr = np.stack([r_arr, g_arr, b_arr, np.full_like(r_arr, 255)], axis=2)
        graded_bg = Image.fromarray(graded_arr, mode="RGBA")
        graded_bg = ImageEnhance.Contrast(graded_bg).enhance(1.22)
        graded_bg = ImageEnhance.Brightness(graded_bg).enhance(1.05)
        canvas = graded_bg
    else:
        canvas = Image.new("RGBA", (canvas_w, canvas_h), (7, 10, 18, 255))

    # 2. Studio Vignette (dark on left edge, open on dancer, smooth darkening under phones)
    vignette = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    vdraw = ImageDraw.Draw(vignette)
    for col in range(canvas_w):
        if col < 450:
            alpha = int(40 * (1 - col / 450.0) + 10)
        elif col < 1050:
            alpha = 8  # Dancer in clear light
        else:
            p = (col - 1050) / float(canvas_w - 1050)
            alpha = int(20 + (p**1.3) * 155)
        vdraw.line([(col, 0), (col, canvas_h)], fill=(5, 8, 16, alpha))

    canvas = Image.alpha_composite(canvas, vignette)

    # 3. Subtle Studio Volumetric Atmosphere / Light Pools
    vol_light = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    vldraw = ImageDraw.Draw(vol_light)
    vldraw.ellipse((600, 80, 1150, 720), fill=(28, 110, 245, 55))  # Dancer highlight aura
    vldraw.ellipse((80, 100, 500, 680), fill=(20, 80, 210, 75))   # Logo aura
    vol_light = vol_light.filter(ImageFilter.GaussianBlur(120))
    canvas = Image.alpha_composite(canvas, vol_light)

    # 4. Screen Ambilight Blooms (Behind Phones)
    shift_phone_x = 110
    r_glow = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    rgdraw = ImageDraw.Draw(r_glow)
    rgdraw.ellipse((1350 + shift_phone_x, 100, 2050 + shift_phone_x, 720), fill=(225, 110, 20, 48))
    rgdraw.ellipse((950 + shift_phone_x, 160, 1500 + shift_phone_x, 780), fill=(25, 75, 185, 45))
    rgdraw.ellipse((1800 + shift_phone_x, 120, 2380 + shift_phone_x, 720), fill=(0, 180, 220, 40))
    r_glow = r_glow.filter(ImageFilter.GaussianBlur(140))
    canvas = Image.alpha_composite(canvas, r_glow)

    # 5. Spotify Cascading Phone Flight (-20° tilt, shifted for clearance)
    rot_angle = -20
    phones_spec = [
        # Back / Upper Row
        ("Screenshot_20260927_140841_LEVYRA.jpg", 640, 880 + shift_phone_x, -110, 1),
        ("Screenshot_20260926_194706_LEVYRA.jpg", 660, 1360 + shift_phone_x, -160, 2),
        ("Screenshot_20260929_194827_LEVYRA.jpg", 640, 1840 + shift_phone_x, -120, 1),

        # Middle / Center Row
        ("Screenshot_20260926_171253_LEVYRA.jpg", 710, 960 + shift_phone_x, 300, 4),
        ("Screenshot_20260926_193948_LEVYRA.jpg", 760, 1450 + shift_phone_x, 210, 5),
        ("Screenshot_20260926_194603_LEVYRA.jpg", 710, 1950 + shift_phone_x, 250, 4),

        # Bottom / Accents
        ("Screenshot_20260929_195520_LEVYRA.jpg", 650, 1560 + shift_phone_x, 650, 3),
        ("Screenshot_20260929_201454_LEVYRA.jpg", 650, 2060 + shift_phone_x, 680, 3),
    ]
    phones_spec.sort(key=lambda s: s[4])

    for filename, height, px, py, z in phones_spec:
        s_path = resolve_screenshot_path(filename)
        if not s_path:
            continue
        with Image.open(s_path) as src:
            phone = create_clean_phone(src, target_height=height)

        rotated = phone.rotate(rot_angle, resample=Image.Resampling.BICUBIC, expand=True)
        contact_shadow, c_pad = create_studio_shadow(rotated, blur=18, opacity=175, offset_y=12)
        ambient_shadow, a_pad = create_studio_shadow(rotated, blur=48, opacity=135, offset_y=28)

        canvas.paste(ambient_shadow, (px - a_pad, py - a_pad), ambient_shadow)
        canvas.paste(contact_shadow, (px - c_pad, py - c_pad), contact_shadow)
        canvas.paste(rotated, (px, py), rotated)

    # 6. Left Branding: 3D Logo + Bold Apple Music Editorial Typography
    logo_file = LOGO_PATH if os.path.exists(LOGO_PATH) else r"app\src\main\res\drawable\levyra_logo.png"
    with Image.open(logo_file) as l_src:
        logo = l_src.convert("RGBA")
        bbox = logo.getbbox()
        logo_crop = logo.crop(bbox)
        target_logo_w = 440
        ratio = target_logo_w / float(logo_crop.width)
        target_logo_h = int(logo_crop.height * ratio)
        logo_res = logo_crop.resize((target_logo_w, target_logo_h), Image.Resampling.LANCZOS)

    logo_x = 90
    logo_y = (canvas_h - target_logo_h - 110) // 2

    # Subtle sapphire halo behind logo
    logo_halo = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    lh_draw = ImageDraw.Draw(logo_halo)
    lh_draw.ellipse((logo_x - 30, logo_y - 30, logo_x + target_logo_w + 30, logo_y + target_logo_h + 30), fill=(26, 95, 230, 85))
    logo_halo = logo_halo.filter(ImageFilter.GaussianBlur(85))
    canvas = Image.alpha_composite(canvas, logo_halo)

    canvas.paste(logo_res, (logo_x, logo_y), logo_res)

    draw = ImageDraw.Draw(canvas)
    text_y = logo_y + target_logo_h + 22

    # Tagline in Electric Cyan (size 30 bold)
    draw.text((logo_x + 10, text_y), "MUSIC, KEPT PERSONAL.", font=get_font(30, bold=True), fill=(56, 189, 248, 255))
    text_y += 46

    # Minimal accent divider line
    draw.line([(logo_x + 10, text_y), (logo_x + 390, text_y)], fill=(56, 189, 248, 100), width=1)
    text_y += 18

    # Bold Apple-style secondary text (clean, large, perfectly readable on GitHub!)
    draw.text((logo_x + 10, text_y), "ALL THE WAYS YOU LOVE MUSIC.", font=get_font(20, bold=True), fill=(245, 250, 255, 245))
    text_y += 30
    draw.text((logo_x + 10, text_y), "HIGH-FIDELITY  •  PRIVATE  •  ANDROID & WINDOWS", font=get_font(16, bold=True), fill=(147, 197, 253, 220))

    out_path = os.path.join(OUT_SHOWCASE_DIR, "00_levyra_hero_wall_player.webp")
    canvas.convert("RGB").save(out_path, "WEBP", quality=95, method=6)
    print("Generated Panoramic Hero Showcase:", out_path)

def draw_accent_pill(draw, x, y, accent_tone):
    pill_col = tuple(max(0, int(accent_tone[i] * 0.58)) for i in range(3))
    draw.rounded_rectangle((x, y, x + 46, y + 6), radius=3, fill=(*pill_col, 255))

def paste_phone_with_studio_depth(canvas, phone, px, py):
    ambient_shadow, a_pad = create_studio_shadow(phone, blur=52, opacity=105, offset_y=30)
    contact_shadow, c_pad = create_studio_shadow(phone, blur=18, opacity=135, offset_y=12)
    canvas.paste(ambient_shadow, (px - a_pad, py - a_pad), ambient_shadow)
    canvas.paste(contact_shadow, (px - c_pad, py - c_pad), contact_shadow)
    canvas.paste(phone, (px, py), phone)

def generate_explore_mix_card(card_w, card_h):
    s1_path = resolve_screenshot_path("Screenshot_20261010_132210_LEVYRA.jpg")
    s2_path = resolve_screenshot_path("Screenshot_20261010_132217_LEVYRA.jpg")
    if not s1_path or not s2_path:
        print("Skipping dual explore card: missing screenshots")
        return

    C_SEAFOAM = ((238, 246, 248), (166, 198, 202), (145, 188, 192))
    top_tint, accent_tone, glow_tone = C_SEAFOAM
    title_font = get_font(64, bold=True)
    sub_font = get_font(31, bold=False)

    bg = create_delicate_bg(card_w, card_h, top_tint, accent_tone, glow_tone, y_start=740, y_end=880)
    draw = ImageDraw.Draw(bg)
    draw_accent_pill(draw, 68, 56, accent_tone)
    draw.text((68, 78), "Explore and mix", font=title_font, fill=(12, 18, 28, 255))
    draw.multiline_text(
        (68, 166),
        "Live radio, fresh currents, and custom\nsliders between familiar and new.",
        font=sub_font,
        fill=(40, 54, 68, 255),
        spacing=10,
    )

    with Image.open(s1_path) as src1:
        p1 = create_clean_phone(src1, target_height=1060)
    with Image.open(s2_path) as src2:
        p2 = create_clean_phone(src2, target_height=1100)

    rot_angle = -7.0
    p1_rot = p1.rotate(rot_angle, resample=Image.Resampling.BICUBIC, expand=True)
    p2_rot = p2.rotate(rot_angle, resample=Image.Resampling.BICUBIC, expand=True)

    p1_x, p1_y = 25, 290
    glow1, gpad1 = create_colored_glow(p1_rot, (0, 205, 230), blur=55, opacity=80, offset_y=16)
    bg.paste(glow1, (p1_x - gpad1, p1_y - gpad1), glow1)
    paste_phone_with_studio_depth(bg, p1_rot, p1_x, p1_y)

    p2_x, p2_y = 310, 430
    glow2, gpad2 = create_colored_glow(p2_rot, (25, 125, 245), blur=60, opacity=85, offset_y=20)
    bg.paste(glow2, (p2_x - gpad2, p2_y - gpad2), glow2)
    paste_phone_with_studio_depth(bg, p2_rot, p2_x, p2_y)

    border_col = tuple(max(0, int(accent_tone[i] - 18)) for i in range(3))
    ImageDraw.Draw(bg).rectangle((0, 0, card_w - 1, card_h - 1), outline=(*border_col, 90), width=2)
    out_path = os.path.join(OUT_CARDS_DIR, "05_explore_mix.webp")
    bg.convert("RGB").save(out_path, "WEBP", quality=94, method=4)
    print(f"Generated Dual Explore & Mix Card: {out_path}")

def generate_explore_mix_crossover(card_w, card_h):
    s1_path = resolve_screenshot_path("Screenshot_20261010_132210_LEVYRA.jpg")
    s2_path = resolve_screenshot_path("Screenshot_20261010_132217_LEVYRA.jpg")
    if not s1_path or not s2_path:
        print("Skipping explore mix crossover: missing screenshots")
        return

    gutter = 20
    pano_w = card_w * 2 + gutter
    pano_h = card_h

    C_SEAFOAM = ((238, 246, 248), (166, 198, 202), (145, 188, 192))
    C_SAPPHIRE = ((238, 244, 252), (162, 188, 222), (140, 175, 215))

    pano_bg = create_delicate_bg(
        pano_w,
        pano_h,
        top_tint=(238, 245, 250),
        accent_tone=(164, 192, 212),
        glow_tone=(142, 180, 205),
        y_start=860,
        y_end=740,
    )

    # Glowing Vinyl Record centered at the seam (x = 910)
    vinyl_size = 660
    vinyl = Image.new("RGBA", (vinyl_size, vinyl_size), (0, 0, 0, 0))
    vdraw = ImageDraw.Draw(vinyl)
    vdraw.ellipse((0, 0, vinyl_size - 1, vinyl_size - 1), fill=(15, 18, 25, 255), outline=(40, 48, 65, 255), width=3)

    for r in range(45, vinyl_size // 2 - 12, 10):
        opacity = 180 if (r % 30 == 0) else 85
        width = 2 if (r % 30 == 0) else 1
        vdraw.ellipse((vinyl_size // 2 - r, vinyl_size // 2 - r, vinyl_size // 2 + r, vinyl_size // 2 + r), outline=(45, 54, 72, opacity), width=width)

    sheen = Image.new("RGBA", (vinyl_size, vinyl_size), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(sheen)
    sdraw.pieslice((0, 0, vinyl_size - 1, vinyl_size - 1), start=38, end=72, fill=(255, 255, 255, 30))
    sdraw.pieslice((0, 0, vinyl_size - 1, vinyl_size - 1), start=218, end=252, fill=(255, 255, 255, 30))
    sheen = sheen.filter(ImageFilter.GaussianBlur(14))
    vinyl = Image.alpha_composite(vinyl, sheen)

    vdraw = ImageDraw.Draw(vinyl)
    label_r = 110
    vdraw.ellipse((vinyl_size // 2 - label_r, vinyl_size // 2 - label_r, vinyl_size // 2 + label_r, vinyl_size // 2 + label_r), fill=(18, 120, 220, 255), outline=(0, 225, 245, 255), width=3)
    vdraw.ellipse((vinyl_size // 2 - label_r + 12, vinyl_size // 2 - label_r + 12, vinyl_size // 2 + label_r - 12, vinyl_size // 2 + label_r - 12), outline=(255, 255, 255, 120), width=1)
    vdraw.text((vinyl_size // 2 - 55, vinyl_size // 2 - 26), "LEVYRA", font=get_font(26, bold=True), fill=(255, 255, 255, 255))
    vdraw.text((vinyl_size // 2 - 64, vinyl_size // 2 + 8), "SOUND ENGINE", font=get_font(15, bold=True), fill=(200, 235, 255, 220))
    vdraw.ellipse((vinyl_size // 2 - 14, vinyl_size // 2 - 14, vinyl_size // 2 + 14, vinyl_size // 2 + 14), fill=(12, 14, 18, 255), outline=(200, 210, 225, 255), width=2)

    vx = pano_w // 2 - vinyl_size // 2
    vy = 800

    v_glow = Image.new("RGBA", (vinyl_size + 120, vinyl_size + 120), (0, 0, 0, 0))
    ImageDraw.Draw(v_glow).ellipse((60, 60, vinyl_size + 60, vinyl_size + 60), fill=(0, 200, 245, 110))
    v_glow = v_glow.filter(ImageFilter.GaussianBlur(55))
    pano_bg.paste(v_glow, (vx - 60, vy - 40), v_glow)
    pano_bg.paste(vinyl, (vx, vy), vinyl)

    eq_layer = Image.new("RGBA", (pano_w, pano_h), (0, 0, 0, 0))
    eq_draw = ImageDraw.Draw(eq_layer)

    xs = np.linspace(0, pano_w, 400)
    ys_center = 1350 + 55 * np.sin(xs * 0.007) + 22 * np.cos(xs * 0.018 + 0.8)
    pts = [(int(x), int(y)) for x, y in zip(xs, ys_center)]
    eq_draw.line(pts, fill=(0, 225, 245, 170), width=6)

    np.random.seed(55)
    for bx in range(600, 1220, 14):
        dist_from_seam = abs(bx - 910)
        max_h = max(20, int(120 - dist_from_seam * 0.28))
        bh = np.random.randint(16, max_h)
        by = int(1350 + 55 * np.sin(bx * 0.007) + 22 * np.cos(bx * 0.018 + 0.8))
        r = int(0 + 40 * (bx / pano_w))
        g = int(220 - 70 * (bx / pano_w))
        b = 250
        eq_draw.line([(bx, by - bh // 2), (bx, by + bh // 2)], fill=(r, g, b, 200), width=5)

    eq_glow = eq_layer.filter(ImageFilter.GaussianBlur(16))
    pano_bg = Image.alpha_composite(pano_bg, eq_glow)
    pano_bg = Image.alpha_composite(pano_bg, eq_layer)

    with Image.open(s1_path) as src1:
        phone_exp = create_clean_phone(src1, target_height=1230)
    with Image.open(s2_path) as src2:
        phone_mix = create_clean_phone(src2, target_height=1230)

    rot_exp = phone_exp.rotate(-16, resample=Image.Resampling.BICUBIC, expand=True)
    rot_mix = phone_mix.rotate(16, resample=Image.Resampling.BICUBIC, expand=True)

    exp_x = 190
    exp_y = 255
    mix_x = 740
    mix_y = 290

    g_exp, gp_exp = create_colored_glow(rot_exp, (0, 215, 235), blur=60, opacity=85, offset_y=18)
    g_mix, gp_mix = create_colored_glow(rot_mix, (25, 130, 250), blur=65, opacity=90, offset_y=20)

    pano_bg.paste(g_exp, (exp_x - gp_exp, exp_y - gp_exp), g_exp)
    paste_phone_with_studio_depth(pano_bg, rot_exp, exp_x, exp_y)

    pano_bg.paste(g_mix, (mix_x - gp_mix, mix_y - gp_mix), g_mix)
    paste_phone_with_studio_depth(pano_bg, rot_mix, mix_x, mix_y)

    pdraw = ImageDraw.Draw(pano_bg)

    c1_tx, c1_ty = 68, 56
    draw_accent_pill(pdraw, c1_tx, c1_ty, C_SEAFOAM[1])
    pdraw.text((c1_tx, c1_ty + 22), "Explore & discover", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text(
        (c1_tx, c1_ty + 110),
        "Live radio, fresh currents, and\nsounds crafted for every moment.",
        font=get_font(31),
        fill=(40, 54, 68, 255),
        spacing=10,
    )

    c2_left = card_w + gutter
    c2_tx, c2_ty = c2_left + 68, 56
    draw_accent_pill(pdraw, c2_tx, c2_ty, C_SAPPHIRE[1])
    pdraw.text((c2_tx, c2_ty + 22), "Levyra Mix & radio", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text(
        (c2_tx, c2_ty + 110),
        "Interactive slider between familiar and new.\nTuned for right now in one tap.",
        font=get_font(31),
        fill=(40, 54, 68, 255),
        spacing=10,
    )

    card_05 = pano_bg.crop((0, 0, card_w, card_h))
    card_06 = pano_bg.crop((c2_left, 0, pano_w, card_h))

    b_col5 = tuple(max(0, int(C_SEAFOAM[1][i] - 18)) for i in range(3))
    b_col6 = tuple(max(0, int(C_SAPPHIRE[1][i] - 18)) for i in range(3))
    ImageDraw.Draw(card_05).rectangle((0, 0, card_w - 1, card_h - 1), outline=(*b_col5, 90), width=2)
    ImageDraw.Draw(card_06).rectangle((0, 0, card_w - 1, card_h - 1), outline=(*b_col6, 90), width=2)

    card_05.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "05_explore.webp"), "WEBP", quality=94, method=4)
    card_06.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "06_levyra_mix.webp"), "WEBP", quality=94, method=4)
    card_05.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "05_explore_stage.webp"), "WEBP", quality=94, method=4)
    card_06.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "06_levyra_mix_stage.webp"), "WEBP", quality=94, method=4)
    print("Generated 3D Crossover Diptych: 05_explore.webp, 06_levyra_mix.webp (and _stage.webp)")

card_w = 900
card_h = 1600
gutter = 20

def create_apple_colored_gradient(width, height, top_tint, bot_tint, ambilight=None):
    top_arr = np.zeros((height, width, 4), dtype=np.uint8)
    y_ratios = np.linspace(0.0, 1.0, height)[:, None]
    for ch in range(3):
        start_v = top_tint[ch]
        end_v = bot_tint[ch]
        top_arr[:, :, ch] = (start_v * (1.0 - y_ratios) + end_v * y_ratios).astype(np.uint8)
    top_arr[:, :, 3] = 255
    canvas = Image.fromarray(top_arr, mode="RGBA")

    if ambilight:
        glow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        gdraw = ImageDraw.Draw(glow)
        cx = width // 2
        cy = int(height * 0.62)
        rx = int(width * 0.44)
        ry = int(height * 0.36)
        gdraw.ellipse((cx - rx, cy - ry, cx + rx, cy + ry), fill=(*ambilight, 56))
        glow = glow.filter(ImageFilter.GaussianBlur(110))
        canvas = Image.alpha_composite(canvas, glow)

    return canvas

def create_apple_studio_shadow(phone):
    blur_ambient = 56
    blur_contact = 18
    pad = blur_ambient * 2 + 50
    sw = phone.width + pad * 2
    sh = phone.height + pad * 2
    alpha = phone.getchannel("A")

    amb_mask = Image.new("L", (sw, sh), 0)
    amb_mask.paste(alpha, (pad, pad + 38))
    amb_mask = amb_mask.filter(ImageFilter.GaussianBlur(blur_ambient))
    amb_mask = amb_mask.point(lambda v: v * 62 // 255)
    amb_shadow = Image.new("RGBA", (sw, sh), (20, 24, 35, 0))
    amb_shadow.putalpha(amb_mask)

    con_mask = Image.new("L", (sw, sh), 0)
    con_mask.paste(alpha, (pad, pad + 14))
    con_mask = con_mask.filter(ImageFilter.GaussianBlur(blur_contact))
    con_mask = con_mask.point(lambda v: v * 85 // 255)
    con_shadow = Image.new("RGBA", (sw, sh), (10, 14, 24, 0))
    con_shadow.putalpha(con_mask)

    shadow = Image.alpha_composite(amb_shadow, con_shadow)
    return shadow, pad

def render_apple_single_card(screenshot_name, kicker, title, subtitle, top_tint, bot_tint, ambilight, border_col, target_height=1210):
    bg = create_apple_colored_gradient(card_w, card_h, top_tint, bot_tint, ambilight)
    draw = ImageDraw.Draw(bg)

    tx = 82
    kicker_y = 74
    title_y = 108
    subtitle_y = 176
    kicker_fill = tuple(max(0, int(border_col[i] * 0.58)) for i in range(3)) + (255,)
    draw.text((tx, kicker_y), kicker.upper(), font=get_font(23, bold=True), fill=kicker_fill)
    draw.text((tx, title_y), title, font=get_font(58, bold=True), fill=(29, 29, 31, 255))
    draw.multiline_text((tx, subtitle_y), subtitle, font=get_font(29), fill=(60, 74, 90, 255), spacing=9)

    s_path = resolve_screenshot_path(screenshot_name)
    if s_path and os.path.exists(s_path):
        with Image.open(s_path) as src:
            phone = create_clean_phone(src, target_height=target_height)
        shadow, pad = create_apple_studio_shadow(phone)
        px = (card_w - phone.width) // 2
        py = 295
        bg.paste(shadow, (px - pad, py - pad), shadow)
        bg.paste(phone, (px, py), phone)

    bdraw = ImageDraw.Draw(bg)
    bdraw.rectangle((0, 0, card_w - 1, card_h - 1), outline=border_col, width=2)
    return bg

def render_duo_cut_cards():
    duo_w = card_w * 2 + gutter
    top_tint = (234, 244, 255)
    bot_tint = (204, 226, 252)
    bg = create_apple_colored_gradient(duo_w, card_h, top_tint, bot_tint, ambilight=(0, 160, 240))

    s_home = resolve_screenshot_path("Screenshot_20260926_171253_LEVYRA.jpg")
    if not s_home or not os.path.exists(s_home):
        s_home = resolve_screenshot_path("01_home.png")

    s_player = resolve_screenshot_path("Screenshot_20260926_193948_LEVYRA.jpg")
    if not s_player or not os.path.exists(s_player):
        s_player = resolve_screenshot_path("20_soundstage.webp")

    c2_left = card_w + gutter
    angle = 15.0

    # 1. Phone 1 (Home) in Card 01 cutting into Card 02
    rot1 = None
    p1_x, p1_y = 220, 70
    if s_home and os.path.exists(s_home):
        with Image.open(s_home) as src1:
            p1 = create_clean_phone(src1, target_height=1220)
        rot1 = p1.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
        s1_shadow, pad1 = create_apple_studio_shadow(rot1)
        bg.paste(s1_shadow, (p1_x - pad1, p1_y - pad1), s1_shadow)
        bg.paste(rot1, (p1_x, p1_y), rot1)

    # 2. Phone 2 (Player) in Card 02 floating completely inside
    rot2 = None
    if s_player and os.path.exists(s_player):
        with Image.open(s_player) as src2:
            p2 = create_clean_phone(src2, target_height=1070)
        rot2 = p2.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
        p2_x = c2_left + (card_w - rot2.width) // 2 + 10
        p2_y = 320
        s2_shadow, pad2 = create_apple_studio_shadow(rot2)
        bg.paste(s2_shadow, (p2_x - pad2, p2_y - pad2), s2_shadow)
        bg.paste(rot2, (p2_x, p2_y), rot2)

    draw = ImageDraw.Draw(bg)

    # Card 01 typography at bottom-left
    tx1, ty1 = 76, 1260
    kicker_col = (75, 105, 140, 255)
    title_col = (29, 29, 31, 255)
    sub_col = (62, 76, 92, 255)

    draw.text((tx1, ty1), "ZERO ADS. NATIVE PLAY.", font=get_font(23, bold=True), fill=kicker_col)
    draw.text((tx1, ty1 + 38), "Pure listening.", font=get_font(60, bold=True), fill=title_col)
    draw.multiline_text(
        (tx1, ty1 + 115),
        "Zero ads, zero accounts.\nStream and store locally on your device.",
        font=get_font(30),
        fill=sub_col,
        spacing=10,
    )

    # Card 02 typography at top-left
    tx2, ty2 = c2_left + 82, 74
    draw.text((tx2, ty2), "OFFLINE STORAGE", font=get_font(23, bold=True), fill=kicker_col)
    draw.text((tx2, ty2 + 38), "Download & keep.", font=get_font(60, bold=True), fill=title_col)
    draw.multiline_text(
        (tx2, ty2 + 115),
        "Clean M4A files in device storage.\nFull metadata, artwork, and lyrics.",
        font=get_font(30),
        fill=sub_col,
        spacing=10,
    )

    border_col = (165, 198, 235, 200)

    # Slice into Card 01 (0..900) and Card 02 (920..1820)
    c1 = bg.crop((0, 0, card_w, card_h))
    bdraw1 = ImageDraw.Draw(c1)
    bdraw1.rectangle((0, 0, card_w - 1, card_h - 1), outline=border_col, width=2)

    c2 = bg.crop((c2_left, 0, c2_left + card_w, card_h))
    bdraw2 = ImageDraw.Draw(c2)
    bdraw2.rectangle((0, 0, card_w - 1, card_h - 1), outline=border_col, width=2)

    return c1, c2

REMAINING_SPECS = [
    # Row 1 cards 3 & 4
    ("screen-lyrics.jpg", "SYNCHRONIZED LYRICS", "Follow every line.", "Synced lyrics move with the music.\nAccurate timing and translations.", (248, 238, 252), (230, 212, 244), (185, 120, 230), (215, 180, 235, 200), ["03_now_playing.webp"]),
    ("Screenshot_20261010_190538_LEVYRA.jpg", "PLAYER DECKS", "Style your stage.", "Canvas, card deck, or classic vinyl.\nSwitch your aesthetic seamlessly.", (238, 242, 252), (218, 226, 245), (140, 160, 230), (185, 200, 230, 200), ["04_player_deck.webp"]),

    # Row 2: Discovery & Audio Engine
    ("Screenshot_20261010_132210_LEVYRA.jpg", "DISCOVERY", "Explore & discover.", "Fresh currents, live radio, and\nsounds crafted for every moment.", (236, 250, 244), (210, 242, 228), (0, 190, 150), (170, 225, 200, 200), ["05_explore_stage.webp", "05_explore.webp"]),
    ("Screenshot_20261010_132217_LEVYRA.jpg", "PERSONAL RADIO", "Levyra Mix.", "Interactive slider between familiar and new.\nTuned for right now in one tap.", (236, 244, 255), (212, 230, 252), (30, 125, 240), (175, 210, 250, 200), ["06_levyra_mix_stage.webp", "06_levyra_mix.webp"]),
    ("Screenshot_20261010_132249_LEVYRA.jpg", "MOODS & GENRES", "Pick a direction.", "Move effortlessly through vibes and\nglobal genres curated for you.", (252, 238, 244), (246, 212, 226), (235, 80, 125), (240, 180, 205, 200), ["07_genres.webp"]),
    ("Screenshot_20261010_190726_LEVYRA.jpg", "AUDIO ENGINE", "Shape the sound.", "Sleep timer, tempo tuning, loudness norm,\nand advanced audio processing.", (238, 244, 250), (218, 230, 242), (70, 135, 205), (180, 205, 230, 200), ["08_audio_tuning.webp"]),

    # Row 3: Discography & Rhythm
    ("Screenshot_20260926_194706_LEVYRA.jpg", "DISCOGRAPHY", "Open the album.", "High-resolution artwork, release info,\nand complete tracklists.", (252, 246, 236), (246, 228, 206), (215, 140, 50), (235, 205, 170, 200), ["09_album.webp"]),
    ("Screenshot_20261010_145010_LEVYRA.jpg", "INSTANT MATCH", "Find it instantly.", "Recent searches, suggestions, and\ninstant matching across your music.", (236, 246, 252), (212, 234, 248), (40, 150, 225), (175, 215, 245, 200), ["10_search.webp"]),
    ("Screenshot_20261010_132136_LEVYRA.jpg", "CURATED PLAYLISTS", "Curated for you.", "Handpicked collections and gems\nrevolving around what you love.", (252, 238, 246), (246, 212, 230), (215, 75, 140), (240, 180, 215, 200), ["11_collections.webp"]),
    ("Screenshot_20260927_131943_LEVYRA.jpg", "LISTENING CADENCE", "Your rhythm.", "Activity heatmaps, peak hours, and\nyour personal listening cadence.", (252, 248, 235), (246, 234, 204), (220, 165, 45), (235, 215, 165, 200), ["12_listening_rhythm.webp"]),

    # Row 4: Orbit & Private Vault
    ("Screenshot_20261010_132103_LEVYRA.jpg", "TOP ROTATION", "In your orbit.", "The songs and artists that always return\nto your daily rotation.", (244, 238, 252), (226, 216, 246), (140, 95, 230), (205, 190, 240, 200), ["13_your_orbit.webp"]),
    ("Screenshot_20261010_132050_LEVYRA.jpg", "ON-DEVICE STATS", "Keep it personal.", "Private listening stats and charts,\ncomputed strictly on your device.", (235, 250, 250), (210, 242, 242), (0, 180, 185), (170, 225, 225, 200), ["14_listening_pulse.webp"]),
    ("Screenshot_20261010_144048_LEVYRA.jpg", "ESSENTIAL SETS", "Artist playlists.", "Curated sets, tours, and the\nessential catalog of every artist.", (238, 242, 252), (216, 226, 248), (55, 115, 235), (180, 200, 245, 200), ["15_artist_playlists.webp"]),
    ("Screenshot_20261010_140700_LEVYRA.jpg", "PRIVACY & BACKUP", "Tailor every detail.", "Audio, design, gestures, and local\nsingle-file Vault backups.", (240, 246, 242), (220, 234, 225), (85, 155, 120), (185, 215, 195, 200), ["16_settings_vault.webp"]),

    # Row 5: Global Charts & Lossless Soundstage
    ("Screenshot_20261010_144144_LEVYRA.jpg", "NEW MUSIC", "Fresh off the stage.", "New singles and albums updated\nevery week directly from artists.", (250, 238, 250), (240, 214, 240), (195, 80, 195), (230, 185, 230, 200), ["17_new_releases.webp"]),
    ("Screenshot_20261010_143646_LEVYRA.jpg", "ARTIST PROFILE", "Meet the artist.", "Full discography, singles, biographies,\nand top tracks in one tap.", (238, 244, 252), (216, 230, 248), (40, 125, 230), (180, 205, 245, 200), ["18_artist_profile.webp", "18_fresh_currents.webp"]),
    ("Screenshot_20261010_132028_LEVYRA.jpg", "GLOBAL CHARTS", "Top 50 charts.", "Explore daily country charts,\nviral hits, and top tracks worldwide.", (252, 238, 236), (248, 212, 208), (235, 75, 65), (245, 180, 175, 200), ["19_top_50.webp"]),
    ("Screenshot_20261011_133434_LEVYRA.jpg", "LEVYRA JAM", "Listen together.", "Real-time collaborative sessions on local Wi-Fi.\nShare queues, add tracks, and host together.", (236, 246, 255), (214, 232, 252), (0, 140, 255), (175, 212, 250, 200), ["20_soundstage.webp", "20_levyra_jam.webp"]),
]

def generate_feature_cards():
    print("Generating Apple Showcase Cards with duo cut on cards 1 & 2 and colored palettes...")
    c1, c2 = render_duo_cut_cards()
    c1.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "01_home.webp"), "WEBP", quality=94, method=4)
    c2.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "02_stay_with_the_song.webp"), "WEBP", quality=94, method=4)
    print("Saved duo cut cards: 01_home.webp, 02_stay_with_the_song.webp")

    for s_name, kicker, title, sub, top_t, bot_t, amb, border_c, fnames in REMAINING_SPECS:
        card = render_apple_single_card(s_name, kicker, title, sub, top_t, bot_t, amb, border_c, target_height=1210)
        for fn in fnames:
            out_p = os.path.join(OUT_CARDS_DIR, fn)
            card.convert("RGB").save(out_p, "WEBP", quality=94, method=4)
            print(f"Saved: {out_p}")


def main():
    print("Generating Apple Levyra showcase assets...")
    try:
        generate_hero_panoramic_showcase()
    except (FileNotFoundError, OSError) as e:
        print(f"Skipping hero panoramic showcase ({e})")
    generate_feature_cards()
    print("Showcase generation completed successfully!")

if __name__ == "__main__":
    main()
