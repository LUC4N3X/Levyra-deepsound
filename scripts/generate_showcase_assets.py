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

def create_clean_phone(screen_img, target_height=1235):
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
        fill=(42, 47, 58, 255),
        outline=(142, 154, 174, 255),
        width=max(2, int(1.8 * ss)),
    )
    draw.rounded_rectangle(
        (ss, ss, body_w - 1 - ss, body_h - 1 - ss),
        radius=max(6, corner_radius - ss),
        outline=(214, 224, 240, 140),
        width=max(1, ss),
    )

    inner_pad = max(3, int(bezel * 0.34))
    draw.rounded_rectangle(
        (inner_pad, inner_pad, body_w - 1 - inner_pad, body_h - 1 - inner_pad),
        radius=max(6, corner_radius - inner_pad),
        fill=(8, 10, 14, 255),
        outline=(36, 42, 52, 255),
        width=max(1, ss // 2),
    )

    screen_mask = Image.new("L", (screen_w, screen_h), 0)
    sdraw = ImageDraw.Draw(screen_mask)
    sdraw.rounded_rectangle((0, 0, screen_w - 1, screen_h - 1), radius=inner_radius, fill=255)

    screen_layer = Image.new("RGBA", (screen_w, screen_h), (0, 0, 0, 0))
    screen_layer.paste(screen_res.convert("RGBA"), (0, 0), screen_mask)
    phone.paste(screen_layer, (bezel, bezel), screen_mask)

    draw.rounded_rectangle(
        (bezel - 1, bezel - 1, bezel + screen_w, bezel + screen_h),
        radius=inner_radius + 1,
        outline=(20, 24, 32, 240),
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
        fill=(78, 128, 198, 185),
    )

    spk_w = int(screen_w * 0.11)
    spk_h = max(2, int(bezel * 0.22))
    spk_x = (body_w - spk_w) // 2
    spk_y = max(2, (bezel - spk_h) // 2)
    draw.rounded_rectangle(
        (spk_x, spk_y, spk_x + spk_w, spk_y + spk_h),
        radius=max(1, spk_h // 2),
        fill=(54, 60, 74, 255),
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
row_w = card_w * 4 + gutter * 3  # 3660
row_h = card_h                   # 1600

def create_panoramic_4card_bg(width, height, top_tint, accent_tone, glow_tone=None, y_wave_start=900, y_wave_end=700, wave_wave_freq=3.5):
    top_arr = np.zeros((height, width, 4), dtype=np.uint8)
    y_ratios = np.linspace(0.0, 1.0, height)[:, None]
    for ch in range(3):
        start_v = min(255, int(top_tint[ch] + 5))
        end_v = max(0, int(top_tint[ch] - 10))
        top_arr[:, :, ch] = (start_v * (1.0 - y_ratios) + end_v * y_ratios).astype(np.uint8)
    top_arr[:, :, 3] = 255
    canvas = Image.fromarray(top_arr, mode="RGBA")

    w2, h2 = width * 2, height * 2
    steps = 400
    t = np.linspace(0.0, 1.0, steps)
    xs = np.linspace(0.0, w2, steps)

    y1 = y_wave_start * 2
    y2 = y_wave_end * 2
    base_y = y1 * (1 - t) + y2 * t
    wave_offset = 120 * np.sin(t * wave_wave_freq * math.pi) + 40 * np.cos(t * (wave_wave_freq * 2) * math.pi)
    ys = base_y + wave_offset

    sec_layer = Image.new("RGBA", (w2, h2), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(sec_layer)
    sec_ys = ys - 90 * np.sin(t * 2 * math.pi) - 40
    sec_poly = [(0, int(sec_ys[0]))] + [(int(x), int(y)) for x, y in zip(xs, sec_ys)] + [(w2, h2), (0, h2)]
    mid_tone = tuple(int(top_tint[i] * 0.55 + accent_tone[i] * 0.45) for i in range(3))
    sdraw.polygon(sec_poly, fill=(*mid_tone, 150))
    canvas = Image.alpha_composite(canvas, sec_layer.resize((width, height), Image.Resampling.LANCZOS))

    wave_layer = Image.new("RGBA", (w2, h2), (0, 0, 0, 0))
    wdraw = ImageDraw.Draw(wave_layer)
    poly = [(0, int(ys[0]))] + [(int(x), int(y)) for x, y in zip(xs, ys)] + [(w2, h2), (0, h2)]
    wdraw.polygon(poly, fill=(*accent_tone, 255))
    crest_pts = [(int(x), int(y)) for x, y in zip(xs, ys)]
    wdraw.line(crest_pts, fill=(255, 255, 255, 175), width=5)
    canvas = Image.alpha_composite(canvas, wave_layer.resize((width, height), Image.Resampling.LANCZOS))

    if glow_tone:
        glow_canvas = Image.new("RGBA", (width, height), (0, 0, 0, 0))
        gdraw = ImageDraw.Draw(glow_canvas)
        for c_idx in range(4):
            cx = c_idx * (card_w + gutter) + card_w // 2
            gdraw.ellipse((cx - 320, 500, cx + 320, 1280), fill=(*glow_tone, 55))
        glow_canvas = glow_canvas.filter(ImageFilter.GaussianBlur(95))
        canvas = Image.alpha_composite(canvas, glow_canvas)

    return canvas

def slice_and_save_cards(bg, filenames_and_titles, accent_tone, preview_name=None):
    cards = []
    border_col = tuple(max(0, int(accent_tone[i] - 18)) for i in range(3))
    for i in range(4):
        x = i * (card_w + gutter)
        c = bg.crop((x, 0, x + card_w, card_h))
        ImageDraw.Draw(c).rectangle((0, 0, card_w - 1, card_h - 1), outline=(*border_col, 90), width=2)
        cards.append(c)
        for fn in filenames_and_titles[i]:
            out_p = os.path.join(OUT_CARDS_DIR, fn)
            c.convert("RGB").save(out_p, "WEBP", quality=94, method=4)
            print(f"Saved: {out_p}")

    if preview_name:
        preview = Image.new("RGB", (row_w, row_h), (250, 250, 252))
        for i, c in enumerate(cards):
            preview.paste(c, (i * (card_w + gutter), 0))
        thumb = preview.resize((row_w // 2, row_h // 2), Image.Resampling.LANCZOS)
        thumb.save(preview_name, "PNG")

    return cards

def generate_row_1():
    print("--- GENERATING ROW 1: Stage & Playback ---")
    top_tint = (238, 245, 250)
    accent_tone = (166, 194, 218)
    glow_tone = (142, 180, 210)
    bg = create_panoramic_4card_bg(row_w, row_h, top_tint, accent_tone, glow_tone, y_wave_start=950, y_wave_end=750)

    s1 = resolve_screenshot_path("Screenshot_20260926_171253_LEVYRA.jpg")  # Home
    s2 = resolve_screenshot_path("Screenshot_20260926_193948_LEVYRA.jpg")  # Player
    s3 = resolve_screenshot_path("screen-lyrics.jpg")                      # Lyrics
    s4 = resolve_screenshot_path("Screenshot_20261010_190538_LEVYRA.jpg")  # Player deck

    with Image.open(s1) as src1:
        p1 = create_clean_phone(src1, target_height=1310)
    rot1 = p1.rotate(21, resample=Image.Resampling.BICUBIC, expand=True)

    with Image.open(s2) as src2:
        p2 = create_clean_phone(src2, target_height=1150)
    rot2 = p2.rotate(21, resample=Image.Resampling.BICUBIC, expand=True)

    c1_left = card_w + gutter
    p2_x = c1_left + 255
    p2_y = 435
    g2, gp2 = create_colored_glow(rot2, (25, 125, 245), blur=55, opacity=80, offset_y=18)
    bg.paste(g2, (p2_x - gp2, p2_y - gp2), g2)
    paste_phone_with_studio_depth(bg, rot2, p2_x, p2_y)

    p1_x = 240
    p1_y = 22
    g1, gp1 = create_colored_glow(rot1, (0, 205, 230), blur=55, opacity=75, offset_y=16)
    bg.paste(g1, (p1_x - gp1, p1_y - gp1), g1)
    paste_phone_with_studio_depth(bg, rot1, p1_x, p1_y)

    with Image.open(s3) as src3:
        p3 = create_clean_phone(src3, target_height=1220)
    rot3 = p3.rotate(-8, resample=Image.Resampling.BICUBIC, expand=True)

    with Image.open(s4) as src4:
        p4 = create_clean_phone(src4, target_height=1220)
    rot4 = p4.rotate(8, resample=Image.Resampling.BICUBIC, expand=True)

    c2_left = (card_w + gutter) * 2
    c3_left = (card_w + gutter) * 3

    p3_x = c2_left + 150
    p3_y = 280
    g3, gp3 = create_colored_glow(rot3, (212, 194, 170), blur=55, opacity=70, offset_y=16)
    bg.paste(g3, (p3_x - gp3, p3_y - gp3), g3)
    paste_phone_with_studio_depth(bg, rot3, p3_x, p3_y)

    p4_x = c3_left + 70
    p4_y = 280
    g4, gp4 = create_colored_glow(rot4, (182, 184, 212), blur=55, opacity=75, offset_y=16)
    bg.paste(g4, (p4_x - gp4, p4_y - gp4), g4)
    paste_phone_with_studio_depth(bg, rot4, p4_x, p4_y)

    eq_layer = Image.new("RGBA", (row_w, row_h), (0, 0, 0, 0))
    eq_draw = ImageDraw.Draw(eq_layer)
    xs = np.linspace(0, row_w, 600)
    ys = 1380 + 40 * np.sin(xs * 0.005) + 20 * np.cos(xs * 0.012 + 0.5)
    pts = [(int(x), int(y)) for x, y in zip(xs, ys)]
    eq_draw.line(pts, fill=(0, 220, 245, 120), width=4)
    eq_glow = eq_layer.filter(ImageFilter.GaussianBlur(12))
    bg = Image.alpha_composite(bg, eq_glow)
    bg = Image.alpha_composite(bg, eq_layer)

    pdraw = ImageDraw.Draw(bg)
    # Card 0
    c0_tx, c0_ty = 58, 1246
    draw_accent_pill(pdraw, c0_tx, c0_ty - 22, accent_tone)
    pdraw.text((c0_tx, c0_ty), "Pure listening", font=get_font(62, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((c0_tx, c0_ty + 84), "Zero ads, zero accounts.\nZero tracking or logins.\nPlayed natively on your device.", font=get_font(30), fill=(32, 46, 64, 255), spacing=10)

    # Card 1
    c1_tx, c1_ty = c1_left + 215, 78
    draw_accent_pill(pdraw, c1_tx, c1_ty - 22, accent_tone)
    pdraw.text((c1_tx, c1_ty), "Download & keep", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((c1_tx, c1_ty + 86), "Clean M4A files in device storage.\nFull metadata, artwork, and lyrics.", font=get_font(31), fill=(38, 52, 68, 255), spacing=10)

    # Card 2
    c2_tx, c2_ty = c2_left + 68, 56
    draw_accent_pill(pdraw, c2_tx, c2_ty, (212, 194, 170))
    pdraw.text((c2_tx, c2_ty + 22), "Follow every line", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((c2_tx, c2_ty + 110), "Synced lyrics move\nwith the music.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    # Card 3
    c3_tx, c3_ty = c3_left + 68, 56
    draw_accent_pill(pdraw, c3_tx, c3_ty, (182, 184, 212))
    pdraw.text((c3_tx, c3_ty + 22), "Style your player", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((c3_tx, c3_ty + 110), "Canvas, card deck, or classic vinyl.\nSwitch your stage seamlessly.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    filenames = [
        ["01_home.webp"],
        ["02_stay_with_the_song.webp"],
        ["03_now_playing.webp"],
        ["04_player_deck.webp"],
    ]
    slice_and_save_cards(bg, filenames, accent_tone)

def generate_row_2():
    print("--- GENERATING ROW 2: Discovery & Sound Engine ---")
    top_tint = (238, 246, 248)
    accent_tone = (166, 198, 202)
    glow_tone = (145, 188, 192)
    bg = create_panoramic_4card_bg(row_w, row_h, top_tint, accent_tone, glow_tone, y_wave_start=860, y_wave_end=740)

    s1 = resolve_screenshot_path("Screenshot_20261010_132210_LEVYRA.jpg")  # Explore
    s2 = resolve_screenshot_path("Screenshot_20261010_132217_LEVYRA.jpg")  # Levyra Mix
    s3 = resolve_screenshot_path("Screenshot_20261010_132249_LEVYRA.jpg")  # Genres
    s4 = resolve_screenshot_path("Screenshot_20261010_190726_LEVYRA.jpg")  # Audio Tuning

    # Sliced Vinyl record portal centered at the seam between Card 0 and Card 1 (x = 910)
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

    vx = 910 - vinyl_size // 2
    vy = 800
    v_glow = Image.new("RGBA", (vinyl_size + 120, vinyl_size + 120), (0, 0, 0, 0))
    ImageDraw.Draw(v_glow).ellipse((60, 60, vinyl_size + 60, vinyl_size + 60), fill=(0, 200, 245, 110))
    v_glow = v_glow.filter(ImageFilter.GaussianBlur(55))
    bg.paste(v_glow, (vx - 60, vy - 40), v_glow)
    bg.paste(vinyl, (vx, vy), vinyl)

    # Connecting Equalizer & Sound Wave extending across Cards 0, 1, 2, and 3
    eq_layer = Image.new("RGBA", (row_w, row_h), (0, 0, 0, 0))
    eq_draw = ImageDraw.Draw(eq_layer)
    xs = np.linspace(0, row_w, 800)
    ys_center = 1350 + 50 * np.sin(xs * 0.005) + 20 * np.cos(xs * 0.012 + 0.8)
    pts = [(int(x), int(y)) for x, y in zip(xs, ys_center)]
    eq_draw.line(pts, fill=(0, 225, 245, 160), width=5)

    np.random.seed(42)
    for bx in range(400, 3200, 22):
        dist_from_vinyl = abs(bx - 910)
        max_h = max(18, int(130 - min(80, dist_from_vinyl * 0.05)))
        bh = np.random.randint(14, max_h)
        by = int(1350 + 50 * np.sin(bx * 0.005) + 20 * np.cos(bx * 0.012 + 0.8))
        r = int(0 + 50 * (bx / row_w))
        g = int(220 - 50 * (bx / row_w))
        b = 250
        eq_draw.line([(bx, by - bh // 2), (bx, by + bh // 2)], fill=(r, g, b, 175), width=5)

    eq_glow = eq_layer.filter(ImageFilter.GaussianBlur(16))
    bg = Image.alpha_composite(bg, eq_glow)
    bg = Image.alpha_composite(bg, eq_layer)

    # Phone 0 (Explore, -16°) & Phone 1 (Levyra Mix, +16°) Crossover
    with Image.open(s1) as src1:
        p1 = create_clean_phone(src1, target_height=1230)
    with Image.open(s2) as src2:
        p2 = create_clean_phone(src2, target_height=1230)
    rot1 = p1.rotate(-16, resample=Image.Resampling.BICUBIC, expand=True)
    rot2 = p2.rotate(16, resample=Image.Resampling.BICUBIC, expand=True)

    exp_x = 190
    exp_y = 255
    mix_x = 740
    mix_y = 290
    g1, gp1 = create_colored_glow(rot1, (0, 215, 235), blur=60, opacity=85, offset_y=18)
    g2, gp2 = create_colored_glow(rot2, (25, 130, 250), blur=65, opacity=90, offset_y=20)
    bg.paste(g1, (exp_x - gp1, exp_y - gp1), g1)
    paste_phone_with_studio_depth(bg, rot1, exp_x, exp_y)
    bg.paste(g2, (mix_x - gp2, mix_y - gp2), g2)
    paste_phone_with_studio_depth(bg, rot2, mix_x, mix_y)

    # Phone 2 (Genres, -8°) & Phone 3 (Audio Tuning, +8°)
    with Image.open(s3) as src3:
        p3 = create_clean_phone(src3, target_height=1230)
    with Image.open(s4) as src4:
        p4 = create_clean_phone(src4, target_height=1230)
    rot3 = p3.rotate(-8, resample=Image.Resampling.BICUBIC, expand=True)
    rot4 = p4.rotate(8, resample=Image.Resampling.BICUBIC, expand=True)

    c2_left = (card_w + gutter) * 2
    c3_left = (card_w + gutter) * 3
    p3_x = c2_left + 140
    p3_y = 275
    p4_x = c3_left + 80
    p4_y = 275

    g3, gp3 = create_colored_glow(rot3, (168, 198, 188), blur=55, opacity=75, offset_y=16)
    g4, gp4 = create_colored_glow(rot4, (140, 180, 220), blur=55, opacity=75, offset_y=16)
    bg.paste(g3, (p3_x - gp3, p3_y - gp3), g3)
    paste_phone_with_studio_depth(bg, rot3, p3_x, p3_y)
    bg.paste(g4, (p4_x - gp4, p4_y - gp4), g4)
    paste_phone_with_studio_depth(bg, rot4, p4_x, p4_y)

    # Texts
    pdraw = ImageDraw.Draw(bg)
    draw_accent_pill(pdraw, 68, 56, accent_tone)
    pdraw.text((68, 78), "Explore & discover", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((68, 166), "Live radio, fresh currents, and\nsounds crafted for every moment.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    c1_left = card_w + gutter
    draw_accent_pill(pdraw, c1_left + 68, 56, (162, 188, 222))
    pdraw.text((c1_left + 68, 78), "Levyra Mix & radio", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((c1_left + 68, 166), "Interactive slider between familiar and new.\nTuned for right now in one tap.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    draw_accent_pill(pdraw, c2_left + 68, 56, (168, 198, 188))
    pdraw.text((c2_left + 68, 78), "Pick a direction", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((c2_left + 68, 166), "Move through moods, vibes, and genres\ncrafted for every moment.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    draw_accent_pill(pdraw, c3_left + 68, 56, (170, 196, 218))
    pdraw.text((c3_left + 68, 78), "Shape the playback", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((c3_left + 68, 166), "Sleep timer, tempo tuning, loudness norm,\nand advanced audio engine.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    filenames = [
        ["05_explore_stage.webp", "05_explore.webp"],
        ["06_levyra_mix_stage.webp", "06_levyra_mix.webp"],
        ["07_genres.webp"],
        ["08_audio_tuning.webp"],
    ]
    slice_and_save_cards(bg, filenames, accent_tone)

def generate_row_3():
    print("--- GENERATING ROW 3: Discography & Collections ---")
    top_tint = (246, 244, 240)
    accent_tone = (198, 188, 172)
    glow_tone = (195, 175, 150)
    bg = create_panoramic_4card_bg(row_w, row_h, top_tint, accent_tone, glow_tone, y_wave_start=910, y_wave_end=760, wave_wave_freq=3.2)

    s1 = resolve_screenshot_path("Screenshot_20260926_194706_LEVYRA.jpg")  # Album
    s2 = resolve_screenshot_path("Screenshot_20261010_145010_LEVYRA.jpg")  # Search
    s3 = resolve_screenshot_path("Screenshot_20261010_132136_LEVYRA.jpg")  # Collections
    s4 = resolve_screenshot_path("Screenshot_20260927_131943_LEVYRA.jpg")  # Rhythm

    c1_left = card_w + gutter
    c2_left = (card_w + gutter) * 2
    c3_left = (card_w + gutter) * 3

    # Album Vinyl Sleeve peeking out behind Phone 0 and crossing across the seam into Card 1
    sleeve_size = 580
    sleeve = Image.new("RGBA", (sleeve_size, sleeve_size), (0, 0, 0, 0))
    s_draw = ImageDraw.Draw(sleeve)
    s_draw.rounded_rectangle((0, 0, sleeve_size - 1, sleeve_size - 1), radius=28, fill=(35, 30, 28, 255), outline=(180, 160, 140, 180), width=4)
    s_draw.ellipse((80, 80, sleeve_size - 80, sleeve_size - 80), outline=(220, 195, 160, 120), width=3)
    s_draw.ellipse((160, 160, sleeve_size - 160, sleeve_size - 160), fill=(20, 18, 16, 255), outline=(230, 205, 170, 200), width=2)
    s_draw.text((sleeve_size // 2 - 58, sleeve_size // 2 - 16), "DISCOGRAPHY", font=get_font(18, bold=True), fill=(240, 220, 190, 240))
    sleeve_rot = sleeve.rotate(-18, resample=Image.Resampling.BICUBIC, expand=True)

    sl_x = 420
    sl_y = 650
    sl_glow, sl_pad = create_colored_glow(sleeve_rot, (220, 185, 140), blur=50, opacity=80, offset_y=16)
    bg.paste(sl_glow, (sl_x - sl_pad, sl_y - sl_pad), sl_glow)
    paste_phone_with_studio_depth(bg, sleeve_rot, sl_x, sl_y)

    # Continuous Golden Heatmap Ribbon across row 3
    pulse_layer = Image.new("RGBA", (row_w, row_h), (0, 0, 0, 0))
    pdraw_layer = ImageDraw.Draw(pulse_layer)
    xs = np.linspace(0, row_w, 600)
    ys = 1380 + 45 * np.sin(xs * 0.006) + 25 * np.cos(xs * 0.014 + 1.2)
    pts = [(int(x), int(y)) for x, y in zip(xs, ys)]
    pdraw_layer.line(pts, fill=(235, 195, 135, 175), width=5)

    np.random.seed(88)
    for px in range(250, 3400, 28):
        py = int(1380 + 45 * np.sin(px * 0.006) + 25 * np.cos(px * 0.014 + 1.2))
        bar_h = np.random.randint(12, 65)
        pdraw_layer.line([(px, py - bar_h // 2), (px, py + bar_h // 2)], fill=(225, 180, 120, 160), width=4)

    pulse_glow = pulse_layer.filter(ImageFilter.GaussianBlur(14))
    bg = Image.alpha_composite(bg, pulse_glow)
    bg = Image.alpha_composite(bg, pulse_layer)

    # Phones: 0 (-14°), 1 (+12°), 2 (-10°), 3 (+10°)
    with Image.open(s1) as src1:
        p1 = create_clean_phone(src1, target_height=1220)
    with Image.open(s2) as src2:
        p2 = create_clean_phone(src2, target_height=1210)
    with Image.open(s3) as src3:
        p3 = create_clean_phone(src3, target_height=1220)
    with Image.open(s4) as src4:
        p4 = create_clean_phone(src4, target_height=1220)

    rot1 = p1.rotate(-14, resample=Image.Resampling.BICUBIC, expand=True)
    rot2 = p2.rotate(12, resample=Image.Resampling.BICUBIC, expand=True)
    rot3 = p3.rotate(-10, resample=Image.Resampling.BICUBIC, expand=True)
    rot4 = p4.rotate(10, resample=Image.Resampling.BICUBIC, expand=True)

    p1_x, p1_y = 150, 290
    p2_x, p2_y = c1_left + 110, 310
    p3_x, p3_y = c2_left + 130, 295
    p4_x, p4_y = c3_left + 80, 295

    g1, gp1 = create_colored_glow(rot1, (212, 194, 170), blur=55, opacity=75, offset_y=16)
    g2, gp2 = create_colored_glow(rot2, (170, 196, 218), blur=55, opacity=75, offset_y=16)
    g3, gp3 = create_colored_glow(rot3, (182, 184, 212), blur=55, opacity=75, offset_y=16)
    g4, gp4 = create_colored_glow(rot4, (168, 198, 188), blur=55, opacity=75, offset_y=16)

    bg.paste(g1, (p1_x - gp1, p1_y - gp1), g1)
    paste_phone_with_studio_depth(bg, rot1, p1_x, p1_y)

    bg.paste(g2, (p2_x - gp2, p2_y - gp2), g2)
    paste_phone_with_studio_depth(bg, rot2, p2_x, p2_y)

    bg.paste(g3, (p3_x - gp3, p3_y - gp3), g3)
    paste_phone_with_studio_depth(bg, rot3, p3_x, p3_y)

    bg.paste(g4, (p4_x - gp4, p4_y - gp4), g4)
    paste_phone_with_studio_depth(bg, rot4, p4_x, p4_y)

    # Texts
    pdraw = ImageDraw.Draw(bg)
    draw_accent_pill(pdraw, 68, 56, accent_tone)
    pdraw.text((68, 78), "Open the album", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((68, 166), "High-resolution artwork, release info,\nand complete tracklists.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    draw_accent_pill(pdraw, c1_left + 68, 56, (170, 196, 218))
    pdraw.text((c1_left + 68, 78), "Find it instantly", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((c1_left + 68, 166), "Recent searches, suggestions, and\ninstant matching across your music.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    draw_accent_pill(pdraw, c2_left + 68, 56, (182, 184, 212))
    pdraw.text((c2_left + 68, 78), "Curated for you", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((c2_left + 68, 166), "Handpicked playlists and gems\nrevolving around what you love.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    draw_accent_pill(pdraw, c3_left + 68, 56, (168, 198, 188))
    pdraw.text((c3_left + 68, 78), "Your listening rhythm", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((c3_left + 68, 166), "Activity heatmaps, peak hours, and\nyour personal listening cadence.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    filenames = [
        ["09_album.webp"],
        ["10_search.webp"],
        ["11_collections.webp"],
        ["12_listening_rhythm.webp"],
    ]
    slice_and_save_cards(bg, filenames, accent_tone)

def generate_row_4():
    print("--- GENERATING ROW 4: Personal Orbit & Vault ---")
    top_tint = (244, 244, 250)
    accent_tone = (182, 184, 212)
    glow_tone = (162, 165, 200)
    bg = create_panoramic_4card_bg(row_w, row_h, top_tint, accent_tone, glow_tone, y_wave_start=940, y_wave_end=730, wave_wave_freq=3.0)

    s1 = resolve_screenshot_path("Screenshot_20261010_132103_LEVYRA.jpg")  # Orbit
    s2 = resolve_screenshot_path("Screenshot_20261010_132050_LEVYRA.jpg")  # Pulse
    s3 = resolve_screenshot_path("Screenshot_20261010_144048_LEVYRA.jpg")  # Playlists
    s4 = resolve_screenshot_path("Screenshot_20261010_140700_LEVYRA.jpg")  # Vault

    c1_left = card_w + gutter
    c2_left = (card_w + gutter) * 2
    c3_left = (card_w + gutter) * 3

    # Cosmic Planetary Orbit Path across all 4 cards
    orbit_layer = Image.new("RGBA", (row_w, row_h), (0, 0, 0, 0))
    odraw = ImageDraw.Draw(orbit_layer)

    for ellipse_idx, (y_center, w_rad, h_rad, opac) in enumerate([
        (1100, 1900, 360, 110),
        (1150, 1750, 310, 85),
        (1050, 1850, 420, 65),
    ]):
        odraw.ellipse((row_w // 2 - w_rad, y_center - h_rad, row_w // 2 + w_rad, y_center + h_rad), outline=(140, 150, 245, opac), width=3)

    for node_x, node_y, r_node in [
        (450, 1380, 10),
        (1350, 1430, 14),
        (2250, 1370, 12),
        (3150, 1420, 16),
    ]:
        odraw.ellipse((node_x - r_node, node_y - r_node, node_x + r_node, node_y + r_node), fill=(180, 195, 255, 230), outline=(255, 255, 255, 255), width=2)
        odraw.ellipse((node_x - r_node * 2, node_y - r_node * 2, node_x + r_node * 2, node_y + r_node * 2), outline=(130, 140, 240, 120), width=1)

    orbit_glow = orbit_layer.filter(ImageFilter.GaussianBlur(16))
    bg = Image.alpha_composite(bg, orbit_glow)
    bg = Image.alpha_composite(bg, orbit_layer)

    # Phones: 0 (-15°), 1 (+13°), 2 (-12°), 3 (+12°)
    with Image.open(s1) as src1:
        p1 = create_clean_phone(src1, target_height=1210)
    with Image.open(s2) as src2:
        p2 = create_clean_phone(src2, target_height=1200)
    with Image.open(s3) as src3:
        p3 = create_clean_phone(src3, target_height=1210)
    with Image.open(s4) as src4:
        p4 = create_clean_phone(src4, target_height=1210)

    rot1 = p1.rotate(-15, resample=Image.Resampling.BICUBIC, expand=True)
    rot2 = p2.rotate(13, resample=Image.Resampling.BICUBIC, expand=True)
    rot3 = p3.rotate(-12, resample=Image.Resampling.BICUBIC, expand=True)
    rot4 = p4.rotate(12, resample=Image.Resampling.BICUBIC, expand=True)

    p1_x, p1_y = 150, 295
    p2_x, p2_y = c1_left + 110, 305
    p3_x, p3_y = c2_left + 130, 295
    p4_x, p4_y = c3_left + 80, 295

    g1, gp1 = create_colored_glow(rot1, (182, 184, 212), blur=55, opacity=75, offset_y=16)
    g2, gp2 = create_colored_glow(rot2, (170, 196, 218), blur=55, opacity=75, offset_y=16)
    g3, gp3 = create_colored_glow(rot3, (170, 196, 218), blur=55, opacity=75, offset_y=16)
    g4, gp4 = create_colored_glow(rot4, (168, 198, 188), blur=55, opacity=75, offset_y=16)

    bg.paste(g1, (p1_x - gp1, p1_y - gp1), g1)
    paste_phone_with_studio_depth(bg, rot1, p1_x, p1_y)

    bg.paste(g2, (p2_x - gp2, p2_y - gp2), g2)
    paste_phone_with_studio_depth(bg, rot2, p2_x, p2_y)

    bg.paste(g3, (p3_x - gp3, p3_y - gp3), g3)
    paste_phone_with_studio_depth(bg, rot3, p3_x, p3_y)

    bg.paste(g4, (p4_x - gp4, p4_y - gp4), g4)
    paste_phone_with_studio_depth(bg, rot4, p4_x, p4_y)

    # Texts
    pdraw = ImageDraw.Draw(bg)
    draw_accent_pill(pdraw, 68, 56, accent_tone)
    pdraw.text((68, 78), "In your orbit", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((68, 166), "The songs and artists that always return\nto your rotation.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    draw_accent_pill(pdraw, c1_left + 68, 56, (170, 196, 218))
    pdraw.text((c1_left + 68, 78), "Keep it personal", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((c1_left + 68, 166), "Private listening stats and charts,\ncomputed strictly on your device.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    draw_accent_pill(pdraw, c2_left + 68, 56, (170, 196, 218))
    pdraw.text((c2_left + 68, 78), "Artist playlists", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((c2_left + 68, 166), "Curated sets, tours, and the\nessential catalog of every artist.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    draw_accent_pill(pdraw, c3_left + 68, 56, (168, 198, 188))
    pdraw.text((c3_left + 68, 78), "Tailor every detail", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((c3_left + 68, 166), "Audio, design, gestures, and local\nsingle-file Vault backups.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    filenames = [
        ["13_your_orbit.webp"],
        ["14_listening_pulse.webp"],
        ["15_artist_playlists.webp"],
        ["16_settings_vault.webp"],
    ]
    slice_and_save_cards(bg, filenames, accent_tone)

def generate_row_5():
    print("--- GENERATING ROW 5: Global Charts & Lossless Soundstage ---")
    top_tint = (245, 242, 246)
    accent_tone = (192, 180, 210)
    glow_tone = (170, 160, 200)
    bg = create_panoramic_4card_bg(row_w, row_h, top_tint, accent_tone, glow_tone, y_wave_start=930, y_wave_end=720, wave_wave_freq=3.4)

    s1 = resolve_screenshot_path("Screenshot_20261010_144144_LEVYRA.jpg")  # New Releases
    s2 = resolve_screenshot_path("Screenshot_20261010_143646_LEVYRA.jpg")  # Artist Profile
    s3 = resolve_screenshot_path("Screenshot_20261010_132028_LEVYRA.jpg")  # Top 50
    s4 = resolve_screenshot_path("Screenshot_20260926_193948_LEVYRA.jpg")  # Soundstage

    c1_left = card_w + gutter
    c2_left = (card_w + gutter) * 2
    c3_left = (card_w + gutter) * 3

    # Master Soundstage Harmonic Acoustic Wave Ribbon across Row 5
    eq_layer = Image.new("RGBA", (row_w, row_h), (0, 0, 0, 0))
    eq_draw = ImageDraw.Draw(eq_layer)
    xs = np.linspace(0, row_w, 700)
    ys = 1360 + 48 * np.sin(xs * 0.0055) + 24 * np.cos(xs * 0.013 + 0.9)
    pts = [(int(x), int(y)) for x, y in zip(xs, ys)]
    eq_draw.line(pts, fill=(215, 185, 120, 165), width=5)

    ys2 = ys - 35 * np.cos(xs * 0.009)
    pts2 = [(int(x), int(y)) for x, y in zip(xs, ys2)]
    eq_draw.line(pts2, fill=(130, 185, 245, 120), width=3)

    eq_glow = eq_layer.filter(ImageFilter.GaussianBlur(16))
    bg = Image.alpha_composite(bg, eq_glow)
    bg = Image.alpha_composite(bg, eq_layer)

    # Phones: 0 (+14°), 1 (-14°), 2 (+10°), 3 (-12°)
    with Image.open(s1) as src1:
        p1 = create_clean_phone(src1, target_height=1210)
    with Image.open(s2) as src2:
        p2 = create_clean_phone(src2, target_height=1220)
    with Image.open(s3) as src3:
        p3 = create_clean_phone(src3, target_height=1200)
    with Image.open(s4) as src4:
        p4 = create_clean_phone(src4, target_height=1210)

    rot1 = p1.rotate(14, resample=Image.Resampling.BICUBIC, expand=True)
    rot2 = p2.rotate(-14, resample=Image.Resampling.BICUBIC, expand=True)
    rot3 = p3.rotate(10, resample=Image.Resampling.BICUBIC, expand=True)
    rot4 = p4.rotate(-12, resample=Image.Resampling.BICUBIC, expand=True)

    p1_x, p1_y = 100, 295
    p2_x, p2_y = c1_left + 150, 285
    p3_x, p3_y = c2_left + 90, 315
    p4_x, p4_y = c3_left + 130, 295

    g1, gp1 = create_colored_glow(rot1, (212, 194, 170), blur=55, opacity=75, offset_y=16)
    g2, gp2 = create_colored_glow(rot2, (182, 184, 212), blur=55, opacity=75, offset_y=16)
    g3, gp3 = create_colored_glow(rot3, (182, 184, 212), blur=55, opacity=75, offset_y=16)
    g4, gp4 = create_colored_glow(rot4, (215, 185, 120), blur=55, opacity=75, offset_y=16)

    bg.paste(g1, (p1_x - gp1, p1_y - gp1), g1)
    paste_phone_with_studio_depth(bg, rot1, p1_x, p1_y)

    bg.paste(g2, (p2_x - gp2, p2_y - gp2), g2)
    paste_phone_with_studio_depth(bg, rot2, p2_x, p2_y)

    bg.paste(g3, (p3_x - gp3, p3_y - gp3), g3)
    paste_phone_with_studio_depth(bg, rot3, p3_x, p3_y)

    bg.paste(g4, (p4_x - gp4, p4_y - gp4), g4)
    paste_phone_with_studio_depth(bg, rot4, p4_x, p4_y)

    # Master Lossless Soundstage Floating Emblem Badge on Card 3
    badge_w, badge_h = 360, 92
    badge = Image.new("RGBA", (badge_w, badge_h), (0, 0, 0, 0))
    bdraw = ImageDraw.Draw(badge)
    bdraw.rounded_rectangle((0, 0, badge_w - 1, badge_h - 1), radius=24, fill=(18, 22, 34, 245), outline=(225, 195, 130, 255), width=2)
    bdraw.text((36, 18), "HI-RES LOSSLESS", font=get_font(22, bold=True), fill=(255, 218, 145, 255))
    bdraw.text((36, 52), "24-BIT / 192 kHz AUDIO ENGINE", font=get_font(15, bold=True), fill=(210, 230, 255, 230))
    bx, by = c3_left + (card_w - badge_w) // 2, 1420
    b_glow, b_pad = create_colored_glow(badge, (220, 190, 130), blur=35, opacity=85, offset_y=10)
    bg.paste(b_glow, (bx - b_pad, by - b_pad), b_glow)
    bg.paste(badge, (bx, by), badge)

    # Texts
    pdraw = ImageDraw.Draw(bg)
    draw_accent_pill(pdraw, 68, 56, (212, 194, 170))
    pdraw.text((68, 78), "Fresh off the stage", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((68, 166), "New singles and albums updated\nevery week directly from artists.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    draw_accent_pill(pdraw, c1_left + 68, 56, (182, 184, 212))
    pdraw.text((c1_left + 68, 78), "Meet the artist", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((c1_left + 68, 166), "Full discography, singles, biographies,\nand top tracks in one tap.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    draw_accent_pill(pdraw, c2_left + 68, 56, (182, 184, 212))
    pdraw.text((c2_left + 68, 78), "Top 50 charts", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((c2_left + 68, 166), "Explore daily country charts,\nviral hits, and top tracks worldwide.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    draw_accent_pill(pdraw, c3_left + 68, 56, (215, 185, 120))
    pdraw.text((c3_left + 68, 78), "Pure soundstage", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text((c3_left + 68, 166), "Experience lossless decoding and\nuncompromised audio fidelity.", font=get_font(31), fill=(40, 54, 68, 255), spacing=10)

    filenames = [
        ["17_new_releases.webp"],
        ["18_artist_profile.webp", "18_fresh_currents.webp"],
        ["19_top_50.webp"],
        ["20_soundstage.webp"],
    ]
    slice_and_save_cards(bg, filenames, accent_tone)

def generate_feature_cards():
    generate_row_1()
    generate_row_2()
    generate_row_3()
    generate_row_4()
    generate_row_5()

def main():
    print("Generating refined Levyra 5-row continuous panoramic showcase assets...")
    try:
        generate_hero_panoramic_showcase()
    except (FileNotFoundError, OSError) as e:
        print(f"Skipping hero panoramic showcase ({e})")
    generate_feature_cards()
    print("Showcase generation completed successfully!")

if __name__ == "__main__":
    main()
