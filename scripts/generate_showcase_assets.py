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
        p1 = create_clean_phone(src1, target_height=1090)
    with Image.open(s2_path) as src2:
        p2 = create_clean_phone(src2, target_height=1140)

    p1_rot = p1.rotate(-3.5, resample=Image.Resampling.BICUBIC, expand=True)
    p2_rot = p2.rotate(2.5, resample=Image.Resampling.BICUBIC, expand=True)

    glow1, gpad1 = create_colored_glow(p1_rot, (0, 190, 220), blur=55, opacity=85, offset_y=16)
    bg.paste(glow1, (30 - gpad1, 325 - gpad1), glow1)
    paste_phone_with_studio_depth(bg, p1_rot, 30, 325)

    glow2, gpad2 = create_colored_glow(p2_rot, (20, 130, 245), blur=50, opacity=80, offset_y=22)
    bg.paste(glow2, (308 - gpad2, 420 - gpad2), glow2)
    paste_phone_with_studio_depth(bg, p2_rot, 308, 420)

    border_col = tuple(max(0, int(accent_tone[i] - 18)) for i in range(3))
    ImageDraw.Draw(bg).rectangle((0, 0, card_w - 1, card_h - 1), outline=(*border_col, 90), width=2)
    out_path = os.path.join(OUT_CARDS_DIR, "05_explore_mix.webp")
    bg.convert("RGB").save(out_path, "WEBP", quality=94, method=4)
    print(f"Generated Dual Explore & Mix Card: {out_path}")

def generate_feature_cards():
    card_w, card_h = 900, 1600
    gutter = 20
    title_font = get_font(64, bold=True)
    sub_font = get_font(31, bold=False)

    pano_w = card_w * 2 + gutter
    pano_h = card_h
    hero_top = (238, 245, 250)
    hero_accent = (166, 194, 218)
    hero_glow = (142, 180, 210)
    pano_bg = create_delicate_bg(
        pano_w,
        pano_h,
        top_tint=hero_top,
        accent_tone=hero_accent,
        glow_tone=hero_glow,
        y_start=1110,
        y_end=580,
    )

    home_file = resolve_screenshot_path("Screenshot_20260926_171253_LEVYRA.jpg")
    player_file = resolve_screenshot_path("Screenshot_20260926_193948_LEVYRA.jpg")
    if not home_file:
        print("Skipping hero cards: home screenshot not found")
        return

    if player_file:
        with Image.open(player_file) as p_source:
            phone_companion = create_clean_phone(p_source, target_height=1150)
        rot_companion = phone_companion.rotate(21, resample=Image.Resampling.BICUBIC, expand=True)
        comp_x = card_w + gutter + 255
        comp_y = 435
        paste_phone_with_studio_depth(pano_bg, rot_companion, comp_x, comp_y)

    with Image.open(home_file) as source:
        phone_hero = create_clean_phone(source, target_height=1310)

    rotated_hero = phone_hero.rotate(21, resample=Image.Resampling.BICUBIC, expand=True)
    hero_x = 240
    hero_y = 22
    paste_phone_with_studio_depth(pano_bg, rotated_hero, hero_x, hero_y)

    pdraw = ImageDraw.Draw(pano_bg)

    c1_tx, c1_ty = 58, 1246
    draw_accent_pill(pdraw, c1_tx, c1_ty - 22, hero_accent)
    pdraw.text((c1_tx, c1_ty), "Pure listening", font=get_font(62, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text(
        (c1_tx, c1_ty + 84),
        "Zero ads, zero accounts.\nZero tracking or logins.\nPlayed natively on your device.",
        font=get_font(30),
        fill=(32, 46, 64, 255),
        spacing=10,
    )

    c2_left = card_w + gutter
    text_x = c2_left + 215
    text_y = 78
    draw_accent_pill(pdraw, text_x, text_y - 22, hero_accent)
    pdraw.text((text_x, text_y), "Download & keep", font=get_font(64, bold=True), fill=(12, 18, 28, 255))
    pdraw.multiline_text(
        (text_x, text_y + 86),
        "Clean M4A files in device storage.\nFull metadata, artwork, and lyrics.",
        font=get_font(31),
        fill=(38, 52, 68, 255),
        spacing=10,
    )

    card_01 = pano_bg.crop((0, 0, card_w, card_h))
    card_02 = pano_bg.crop((c2_left, 0, pano_w, card_h))
    border_col = tuple(max(0, int(hero_accent[i] - 18)) for i in range(3))
    ImageDraw.Draw(card_01).rectangle((0, 0, card_w - 1, card_h - 1), outline=(*border_col, 90), width=2)
    ImageDraw.Draw(card_02).rectangle((0, 0, card_w - 1, card_h - 1), outline=(*border_col, 90), width=2)

    card_01.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "01_home.webp"), "WEBP", quality=92, method=4)
    card_02.convert("RGB").save(os.path.join(OUT_CARDS_DIR, "02_stay_with_the_song.webp"), "WEBP", quality=92, method=4)
    print("Generated Hero Cards: 01_home.webp, 02_stay_with_the_song.webp")

    generate_explore_mix_card(card_w, card_h)

    C_CELESTE = ((238, 245, 250), (170, 196, 218), (148, 185, 210))
    C_SAGE = ((238, 246, 242), (168, 198, 188), (145, 185, 172))
    C_CASHMERE = ((248, 245, 240), (212, 194, 170), (195, 175, 150))
    C_TWILIGHT = ((244, 244, 250), (182, 184, 212), (162, 165, 200))
    C_SEAFOAM = ((238, 246, 248), (166, 198, 202), (145, 188, 192))

    single_specs = [
        (0, "03_now_playing.webp", "screen-lyrics.jpg", "Follow every line", "Synced lyrics move\nwith the music.", C_CASHMERE),
        (1, "04_player_deck.webp", "Screenshot_20260929_195520_LEVYRA.jpg", "Style your player", "Canvas, card deck, or classic vinyl.\nSwitch your stage seamlessly.", C_TWILIGHT),
        (3, "06_artist_profile.webp", "Screenshot_20260926_194603_LEVYRA.jpg", "Meet the artist", "Full discography, singles, biographies,\nand top tracks in one tap.", C_TWILIGHT),
        (4, "07_genres.webp", "Screenshot_20261010_132249_LEVYRA.jpg", "Pick a direction", "Move through moods, vibes, and genres\ncrafted for every moment.", C_SAGE),
        (5, "08_audio_tuning.webp", "Screenshot_20260926_194845_LEVYRA.jpg", "Shape the playback", "Sleep timer, tempo tuning, loudness norm,\nand advanced audio engine.", C_CELESTE),
        (6, "09_album.webp", "Screenshot_20260926_194706_LEVYRA.jpg", "Open the album", "High-resolution artwork, release info,\nand complete tracklists.", C_CASHMERE),
        (7, "10_search.webp", "Screenshot_20260926_194736_LEVYRA.jpg", "Find it instantly", "Recent searches, suggestions, and\ninstant matching across your music.", C_CELESTE),
        (8, "11_collections.webp", "Screenshot_20261010_132136_LEVYRA.jpg", "Curated for you", "Handpicked playlists and gems\nrevolving around what you love.", C_TWILIGHT),
        (9, "12_listening_rhythm.webp", "Screenshot_20260927_131943_LEVYRA.jpg", "Your listening rhythm", "Activity heatmaps, peak hours, and\nyour personal listening cadence.", C_SAGE),
        (10, "13_your_orbit.webp", "Screenshot_20261010_132103_LEVYRA.jpg", "In your orbit", "The songs and artists that always return\nto your rotation.", C_TWILIGHT),
        (11, "14_listening_pulse.webp", "Screenshot_20261010_132050_LEVYRA.jpg", "Keep it personal", "Private listening stats and charts,\ncomputed strictly on your device.", C_CELESTE),
        (12, "15_artist_playlists.webp", "Screenshot_20260929_201454_LEVYRA.jpg", "Artist playlists", "Curated sets, tours, and the\nessential catalog of every artist.", C_CELESTE),
        (13, "16_settings_vault.webp", "Screenshot_20260929_194944_LEVYRA.jpg", "Tailor every detail", "Audio, design, gestures, and local\nsingle-file Vault backups.", C_SAGE),
        (14, "17_new_releases.webp", "Screenshot_20260927_132248_LEVYRA.jpg", "Fresh off the stage", "New singles and albums updated\nevery week directly from artists.", C_CASHMERE),
        (15, "18_fresh_currents.webp", "Screenshot_20260929_212908_LEVYRA.jpg", "Discovery stream", "Artist mixes, deep catalog filters,\nand instant radio stations.", C_SEAFOAM),
        (16, "19_top_50.webp", "Screenshot_20261010_132028_LEVYRA.jpg", "Top 50 charts", "Explore daily country charts,\nviral hits, and top tracks worldwide.", C_TWILIGHT),
        (17, "20_soundstage.webp", "Screenshot_20260926_193948_LEVYRA.jpg", "Pure soundstage", "Experience lossless decoding and\nuncompromised audio fidelity.", C_CELESTE),
    ]

    wave_endpoints = [
        (930, 740),
        (740, 880),
        (880, 690),
        (690, 910),
    ]

    for wave_idx, filename, screenshot_name, title, subtitle, palette in single_specs:
        screen_file = resolve_screenshot_path(screenshot_name)
        if not screen_file:
            print(f"Skipping {filename}: {screenshot_name} not found")
            continue

        with Image.open(screen_file) as source:
            phone = create_clean_phone(source, target_height=1235)

        top_tint, accent_tone, glow_tone = palette
        y_start, y_end = wave_endpoints[(wave_idx + 2) % 4]
        bg = create_delicate_bg(card_w, card_h, top_tint, accent_tone, glow_tone, y_start=y_start, y_end=y_end)
        draw = ImageDraw.Draw(bg)

        draw_accent_pill(draw, 68, 56, accent_tone)
        draw.text((68, 78), title, font=title_font, fill=(12, 18, 28, 255))
        draw.multiline_text((68, 166), subtitle, font=sub_font, fill=(40, 54, 68, 255), spacing=10)

        px = (card_w - phone.width) // 2
        py = 302
        paste_phone_with_studio_depth(bg, phone, px, py)

        out_path = os.path.join(OUT_CARDS_DIR, filename)
        bg.convert("RGB").save(out_path, "WEBP", quality=92, method=4)
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
