import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageEnhance

SCREENSHOT_DIR = r"C:\Users\Luca Drogo\Desktop\screenshots"
OUT_DIR = r"C:\Users\Luca Drogo\repo-work\Levyra-deepsound\docs\assets\showcase\editorial-cards"
EMBLEM_PATH = os.path.join(SCREENSHOT_DIR, "Emblema a nastro neon astratto.png")

os.makedirs(OUT_DIR, exist_ok=True)

CARD_W, CARD_H = 900, 1800
GAP = 32

FONT_BOLD = r"C:\Windows\Fonts\segoeuib.ttf"
FONT_SEMIBOLD = r"C:\Windows\Fonts\segoeuisl.ttf"
FONT_REGULAR = r"C:\Windows\Fonts\segoeui.ttf"

def get_font(path, size):
    if os.path.exists(path):
        return ImageFont.truetype(path, size)
    return ImageFont.load_default()

# Display typography for Row 1 diptychs (2-line sculptural headlines)
font_diptych_title = get_font(FONT_BOLD, 76)
font_diptych_sub = get_font(FONT_BOLD, 34)

# Display typography for Rows 2-6 upright cards (centered editorial header)
font_upright_title = get_font(FONT_BOLD, 64)
font_upright_sub = get_font(FONT_BOLD, 31)

# Load official Levyra ribbon emblem
emblem_img = None
if os.path.exists(EMBLEM_PATH):
    emblem_raw = Image.open(EMBLEM_PATH).convert("RGBA")
    alpha_bbox = emblem_raw.getchannel("A").getbbox()
    if alpha_bbox:
        emblem_raw = emblem_raw.crop(alpha_bbox)
    ew = 66
    eh = int(round(ew * emblem_raw.height / emblem_raw.width))
    emblem_img = emblem_raw.resize((ew, eh), Image.Resampling.LANCZOS)

def resolve_screenshot_path(filename):
    candidates = [
        os.path.join(SCREENSHOT_DIR, filename),
        os.path.join(r"C:\Users\Luca Drogo\Downloads", filename),
        os.path.join(r"C:\Users\Luca Drogo\Pictures\levyra\screenshots", filename),
        os.path.join("docs", "screenshots", filename),
    ]
    for c in candidates:
        if os.path.exists(c):
            return c
    raise FileNotFoundError(f"Missing screenshot: {filename}")

def crop_to_fill(source, width, height):
    scale = max(width / source.width, height / source.height)
    resized = source.resize(
        (int(round(source.width * scale)), int(round(source.height * scale))),
        Image.Resampling.LANCZOS,
    )
    left = (resized.width - width) // 2
    top = (resized.height - height) // 2
    return resized.crop((left, top, left + width, top + height))

def enhance_screenshot(img):
    img = img.convert("RGB")
    img = ImageEnhance.Contrast(img).enhance(1.03)
    img = ImageEnhance.Sharpness(img).enhance(1.05)
    return img

_PIN_CACHE = {}

def draw_modern_pin(card, cx, cy, radius=17):
    """Draws a realistic 3D CNC-machined titanium studio pin/stud ('chiodo moderno') with cast shadow."""
    if radius not in _PIN_CACHE:
        scale = 4
        R = radius * scale
        pad = 18 * scale
        size = (R + pad) * 2
        pin_canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        c = size // 2

        # 1. Directional cast shadow (bottom-right)
        shadow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        sdraw = ImageDraw.Draw(shadow)
        sx, sy = c + 5 * scale, c + 8 * scale
        sdraw.ellipse((sx - R, sy - R, sx + R, sy + R), fill=(15, 23, 42, 130))
        shadow = shadow.filter(ImageFilter.GaussianBlur(6 * scale))
        pin_canvas.alpha_composite(shadow)

        # 2. Tight contact occlusion shadow
        contact = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        cdraw = ImageDraw.Draw(contact)
        cx_occ, cy_occ = c + 1 * scale, c + 2 * scale
        cdraw.ellipse((cx_occ - R, cy_occ - R, cx_occ + R, cy_occ + R), fill=(10, 14, 24, 165))
        contact = contact.filter(ImageFilter.GaussianBlur(2 * scale))
        pin_canvas.alpha_composite(contact)

        # 3. Outer machined titanium flange with diagonal directional lighting
        head = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        for y in range(c - R, c + R + 1):
            for x in range(c - R, c + R + 1):
                dx, dy = x - c, y - c
                dist = math.hypot(dx, dy)
                if dist <= R:
                    t = ((dx + dy) / (2.0 * R) + 1.0) * 0.5
                    t = max(0.0, min(1.0, t))
                    if dist > R - 2.4 * scale:
                        r = int(240 * (1 - t) + 38 * t)
                        g = int(244 * (1 - t) + 44 * t)
                        b = int(252 * (1 - t) + 56 * t)
                    elif dist > R * 0.62:
                        r = int(125 * (1 - t) + 24 * t)
                        g = int(132 * (1 - t) + 28 * t)
                        b = int(148 * (1 - t) + 38 * t)
                    elif dist > R * 0.50:
                        r = int(18 * (1 - t) + 85 * t)
                        g = int(22 * (1 - t) + 92 * t)
                        b = int(30 * (1 - t) + 108 * t)
                    else:
                        inner_t = ((dx / (R * 0.50) + dy / (R * 0.50)) * 0.5 + 1.0) * 0.5
                        inner_t = max(0.0, min(1.0, inner_t))
                        r = int(180 * (1 - inner_t) + 30 * inner_t)
                        g = int(188 * (1 - inner_t) + 35 * inner_t)
                        b = int(205 * (1 - inner_t) + 46 * inner_t)
                    head.putpixel((x, y), (r, g, b, 255))

        mask = Image.new("L", (size, size), 0)
        ImageDraw.Draw(mask).ellipse((c - R, c - R, c + R, c + R), fill=255)
        head.putalpha(mask)

        # 4. Crisp specular hotspot on top-left of dome
        glint = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        gdraw = ImageDraw.Draw(glint)
        gx, gy = c - int(R * 0.22), c - int(R * 0.22)
        gr = int(R * 0.17)
        gdraw.ellipse((gx - gr, gy - gr, gx + gr, gy + gr), fill=(255, 255, 255, 230))
        glint = glint.filter(ImageFilter.GaussianBlur(int(scale * 1.1)))
        head.alpha_composite(glint)

        pin_canvas.alpha_composite(head)
        out_size = size // scale
        _PIN_CACHE[radius] = pin_canvas.resize((out_size, out_size), Image.Resampling.LANCZOS)

    pin_final = _PIN_CACHE[radius]
    out_size = pin_final.width
    card.alpha_composite(pin_final, (cx - out_size // 2, cy - out_size // 2))

def draw_diptych_editorial_block(card, title_lines, subtitle, x, y, align="left", title_fill=(15, 23, 42, 255), sub_fill=(71, 85, 105, 255), show_pin=True):
    """Draws a bold 2-line display headline + concise 1-line subhead anchored by a modern 3D pin."""
    draw = ImageDraw.Draw(card)
    pin_r = 17

    if align == "left":
        if show_pin:
            draw_modern_pin(card, x + pin_r, y, radius=pin_r)
            curr_y = y + pin_r + 18
        else:
            curr_y = y
        for line in title_lines:
            draw.text((x, curr_y), line, font=font_diptych_title, fill=title_fill)
            curr_y += 82
        curr_y += 14
        draw.text((x, curr_y), subtitle, font=font_diptych_sub, fill=sub_fill)
    else:
        if show_pin:
            draw_modern_pin(card, x - pin_r, y, radius=pin_r)
            curr_y = y + pin_r + 18
        else:
            curr_y = y
        for line in title_lines:
            bbox = draw.textbbox((0, 0), line, font=font_diptych_title)
            lw = bbox[2] - bbox[0]
            draw.text((x - lw, curr_y), line, font=font_diptych_title, fill=title_fill)
            curr_y += 82
        curr_y += 14
        sbbox = draw.textbbox((0, 0), subtitle, font=font_diptych_sub)
        sw = sbbox[2] - sbbox[0]
        draw.text((x - sw, curr_y), subtitle, font=font_diptych_sub, fill=sub_fill)

def create_phone_mockup(screenshot_filename, outer_w=710):
    screenshot_path = resolve_screenshot_path(screenshot_filename)
    with Image.open(screenshot_path) as src:
        screenshot = enhance_screenshot(src).convert("RGBA")

    bezel = 12
    radius = 34
    screen_radius = 24

    screen_w = outer_w - bezel * 2
    screen_h = int(round(screen_w * 2993 / 1440))
    outer_h = screen_h + bezel * 2

    pad = 36
    cw, ch = outer_w + pad * 2, outer_h + pad * 2
    device = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    draw = ImageDraw.Draw(device)

    body = (pad, pad, pad + outer_w - 1, pad + outer_h - 1)

    # 1. Brushed Dark Titanium Chassis
    draw.rounded_rectangle(body, radius=radius, fill=(24, 26, 32, 255))

    # 2. Specular Metallic Rim
    draw.rounded_rectangle(body, radius=radius, outline=(192, 198, 210, 255), width=2)

    # 3. Inner Bezel Step
    inner_body = (body[0] + 2, body[1] + 2, body[2] - 2, body[3] - 2)
    draw.rounded_rectangle(inner_body, radius=radius - 2, outline=(46, 50, 60, 255), width=1)

    # 4. Pixel-perfect Screenshot
    resized_screen = crop_to_fill(screenshot, screen_w, screen_h)
    screen_mask = Image.new("L", (screen_w, screen_h), 0)
    ImageDraw.Draw(screen_mask).rounded_rectangle(
        (0, 0, screen_w - 1, screen_h - 1), radius=screen_radius, fill=255
    )

    screen_x = pad + bezel
    screen_y = pad + bezel
    device.paste(resized_screen, (screen_x, screen_y), screen_mask)

    # 5. Crisp inner glass edge
    draw.rounded_rectangle(
        (screen_x, screen_y, screen_x + screen_w - 1, screen_y + screen_h - 1),
        radius=screen_radius,
        outline=(10, 12, 16, 255),
        width=1,
    )

    # 6. Galaxy S25 Ultra Infinity-O camera punch-hole
    cam_x = pad + outer_w // 2
    cam_y = screen_y + 16
    cam_r = 7
    draw.ellipse((cam_x - cam_r, cam_y - cam_r, cam_x + cam_r, cam_y + cam_r), fill=(6, 7, 10, 255))
    draw.ellipse((cam_x - 2, cam_y - 2, cam_x + 1, cam_y + 1), fill=(38, 64, 104, 215))

    # 7. Earpiece micro-slit
    draw.rounded_rectangle((cam_x - 28, pad + 2, cam_x + 28, pad + 5), radius=1, fill=(70, 74, 84, 255))

    # 8. Side Titanium Hardware Keys
    btn_w = 3
    btn_x = pad + outer_w - 1
    v_top = pad + int(outer_h * 0.21)
    v_bot = pad + int(outer_h * 0.32)
    draw.rounded_rectangle((btn_x, v_top, btn_x + btn_w, v_bot), radius=1, fill=(145, 152, 165, 255))
    p_top = pad + int(outer_h * 0.37)
    p_bot = pad + int(outer_h * 0.44)
    draw.rounded_rectangle((btn_x, p_top, btn_x + btn_w, p_bot), radius=1, fill=(145, 152, 165, 255))

    return device, outer_w, outer_h, pad

def create_editorial_surface(w, h, top_rgb, bot_rgb, wash_rgb, wash_pos=(0.5, 0.62)):
    """Creates a tactile, non-black architectural card surface with curated tonal depth."""
    card = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(card)

    for y in range(h):
        mix = y / (h - 1)
        r = int(top_rgb[0] * (1 - mix) + bot_rgb[0] * mix)
        g = int(top_rgb[1] * (1 - mix) + bot_rgb[1] * mix)
        b = int(top_rgb[2] * (1 - mix) + bot_rgb[2] * mix)
        draw.line([(0, y), (w, y)], fill=(r, g, b, 255))

    wash = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    wdraw = ImageDraw.Draw(wash)
    cx, cy = int(w * wash_pos[0]), int(h * wash_pos[1])
    rx, ry = int(w * 0.58), int(h * 0.38)
    wdraw.ellipse((cx - rx, cy - ry, cx + rx, cy + ry), fill=wash_rgb + (105,))
    wash = wash.filter(ImageFilter.GaussianBlur(145))
    card.alpha_composite(wash)
    return card

def rotate_device(device, angle):
    if abs(angle) > 0.01:
        return device.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
    return device

def place_rotated_device(canvas, rot, pos, shadow_offset=(20, 38)):
    """Composites a device with a natural 3-layer studio shadow."""
    alpha = rot.getchannel("A")
    cw, ch = rot.size
    ox, oy = shadow_offset

    spad = 110
    scw, sch = cw + spad * 2, ch + spad * 2
    shadow_canvas = Image.new("RGBA", (scw, sch), (0, 0, 0, 0))

    padded_alpha = Image.new("L", (scw, sch), 0)
    padded_alpha.paste(alpha, (spad, spad))

    # Layer 1: Soft wide ambient shadow
    s3_a = padded_alpha.filter(ImageFilter.GaussianBlur(54))
    s3_a = ImageEnhance.Brightness(s3_a).enhance(0.18)
    s3 = Image.new("RGBA", (scw, sch), (15, 23, 42, 255))
    s3.putalpha(s3_a)
    shadow_canvas.alpha_composite(s3, (ox, oy))

    # Layer 2: Mid studio depth shadow
    s2_a = padded_alpha.filter(ImageFilter.GaussianBlur(26))
    s2_a = ImageEnhance.Brightness(s2_a).enhance(0.22)
    s2 = Image.new("RGBA", (scw, sch), (15, 23, 42, 255))
    s2.putalpha(s2_a)
    shadow_canvas.alpha_composite(s2, (int(ox * 0.55), int(oy * 0.55)))

    # Layer 3: Crisp contact shadow
    s1_a = padded_alpha.filter(ImageFilter.GaussianBlur(10))
    s1_a = ImageEnhance.Brightness(s1_a).enhance(0.25)
    s1 = Image.new("RGBA", (scw, sch), (10, 15, 28, 255))
    s1.putalpha(s1_a)
    shadow_canvas.alpha_composite(s1, (int(ox * 0.25), int(oy * 0.25)))

    px, py = pos
    canvas.alpha_composite(shadow_canvas, (px - spad, py - spad))
    canvas.alpha_composite(rot, (px, py))

def finalize_card(card, border_rgb):
    w, h = card.size
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, w - 1, h - 1), radius=42, fill=255)

    frame = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    fdraw = ImageDraw.Draw(frame)
    fdraw.rounded_rectangle((1, 1, w - 2, h - 2), radius=41, outline=(255, 255, 255, 150), width=2)
    fdraw.rounded_rectangle((0, 0, w - 1, h - 1), radius=42, outline=border_rgb + (255,), width=2)
    card.alpha_composite(frame)
    card.putalpha(mask)
    return card

# ==============================================================================
# ROW 1 — DYNAMIC EDITORIAL DIPTYCH CUTS (Cards 01-04 ONLY)
# ==============================================================================
def build_row1_cards():
    pair_w = CARD_W * 2 + GAP
    pair1 = Image.new("RGBA", (pair_w, CARD_H), (0, 0, 0, 0))

    # Card 01 Surface: Periwinkle Cobalt Mist
    bg1 = create_editorial_surface(
        CARD_W, CARD_H,
        top_rgb=(238, 243, 254),
        bot_rgb=(214, 225, 248),
        wash_rgb=(174, 195, 245),
        wash_pos=(0.55, 0.45),
    )
    # Card 02 Surface: Warm Rose-Orchid Blush
    bg2 = create_editorial_surface(
        CARD_W, CARD_H,
        top_rgb=(252, 241, 246),
        bot_rgb=(238, 218, 230),
        wash_rgb=(226, 185, 210),
        wash_pos=(0.50, 0.65),
    )
    pair1.paste(bg1, (0, 0))
    pair1.paste(bg2, (CARD_W + GAP, 0))

    # Phone 1: Pure Listening descending from top-right at +12.5 deg
    dev1, _, _, _ = create_phone_mockup("Screenshot_20261016_223653_LEVYRA.jpg", outer_w=725)
    rot1 = rotate_device(dev1, 12.5)
    place_rotated_device(pair1, rot1, pos=(215, -185), shadow_offset=(22, 40))

    # Phone 2: Stay with the Song rising from bottom-right at -8.5 deg
    dev2, _, _, _ = create_phone_mockup("Screenshot_20261016_135636_LEVYRA.jpg", outer_w=725)
    rot2 = rotate_device(dev2, -8.5)
    place_rotated_device(pair1, rot2, pos=(CARD_W + GAP + 195, 415), shadow_offset=(22, 40))

    c1 = pair1.crop((0, 0, CARD_W, CARD_H))
    c2 = pair1.crop((CARD_W + GAP, 0, pair_w, CARD_H))

    # Top-left 3D pin on Card 01 aligned at y=76 with Cards 02-04
    draw_modern_pin(c1, 68 + 17, 76, radius=17)
    draw_diptych_editorial_block(
        c1,
        title_lines=["Pure", "Listening."],
        subtitle="Your music, zero ads.",
        x=68,
        y=1455,
        align="left",
        title_fill=(17, 24, 39, 255),
        sub_fill=(55, 71, 107, 255),
        show_pin=False,
    )

    draw_diptych_editorial_block(
        c2,
        title_lines=["Stay with", "the Song."],
        subtitle="Live synced lyrics.",
        x=CARD_W - 68,
        y=76,
        align="right",
        title_fill=(36, 16, 30, 255),
        sub_fill=(108, 54, 88, 255),
    )

    c1 = finalize_card(c1, border_rgb=(195, 208, 238))
    c2 = finalize_card(c2, border_rgb=(224, 196, 213))

    # Diptych 2: Card 03 (Daylight Expressive) & Card 04 (Midnight Obsidian)
    c3 = create_editorial_surface(
        CARD_W, CARD_H,
        top_rgb=(252, 248, 240),
        bot_rgb=(238, 227, 208),
        wash_rgb=(226, 208, 178),
        wash_pos=(0.50, 0.65),
    )
    c4 = create_editorial_surface(
        CARD_W, CARD_H,
        top_rgb=(228, 237, 247),
        bot_rgb=(199, 214, 232),
        wash_rgb=(162, 188, 218),
        wash_pos=(0.50, 0.65),
    )

    dev3, _, _, _ = create_phone_mockup("Screenshot_20261024_204745_Levyra.jpg", outer_w=720)
    rot3 = rotate_device(dev3, -8.0)
    x3 = 105
    place_rotated_device(c3, rot3, pos=(x3, 395), shadow_offset=(20, 38))

    dev4, _, _, _ = create_phone_mockup("Screenshot_20261024_205815_Levyra.jpg", outer_w=720)
    rot4 = rotate_device(dev4, 8.0)
    x4 = CARD_W - x3 - rot4.width + 45
    place_rotated_device(c4, rot4, pos=(x4, 395), shadow_offset=(-20, 38))

    draw_diptych_editorial_block(
        c3,
        title_lines=["Daylight", "Expressive."],
        subtitle="Tuned for bright clarity.",
        x=68,
        y=76,
        align="left",
        title_fill=(28, 23, 18, 255),
        sub_fill=(104, 85, 62, 255),
    )

    draw_diptych_editorial_block(
        c4,
        title_lines=["Midnight", "Obsidian."],
        subtitle="Deep OLED contrast.",
        x=CARD_W - 68,
        y=76,
        align="right",
        title_fill=(15, 23, 42, 255),
        sub_fill=(48, 64, 87, 255),
    )

    c3 = finalize_card(c3, border_rgb=(220, 206, 184))
    c4 = finalize_card(c4, border_rgb=(178, 196, 218))

    return [c1, c2, c3, c4]

# ==============================================================================
# ROWS 2 TO 6 — UPRIGHT FLAGSHIP STAGE (Cards 05-24)
# ==============================================================================
UPRIGHT_CARD_SPECS = [
    # Row 2: Discovery & Audio Precision (05-08)
    (
        "05_fresh_currents",
        "Instant Search",
        "Tracks, albums & artists in real time.",
        "Screenshot_20261024_144737_Levyra.jpg",
        (241, 246, 254), (218, 230, 248), (184, 206, 242), (194, 210, 236),
        (15, 23, 42), (48, 69, 104),
    ),
    (
        "06_built_for_your_taste",
        "Player Styles",
        "Layouts crafted for your taste.",
        "Screenshot_20261024_203401_Levyra.jpg",
        (250, 244, 236), (234, 220, 204), (218, 195, 170), (216, 199, 180),
        (28, 20, 16), (102, 74, 56),
    ),
    (
        "07_every_genre",
        "Every Mood & Genre",
        "Explore by energy and atmosphere.",
        "Screenshot_20261015_140910_LEVYRA.jpg",
        (239, 250, 246), (212, 239, 230), (174, 224, 208), (182, 222, 209),
        (15, 34, 29), (38, 92, 78),
    ),
    (
        "08_acoustic_precision",
        "Acoustic Precision",
        "Pure 320 kbps & lossless output.",
        "Screenshot_20261024_150113_Levyra.jpg",
        (237, 243, 250), (212, 225, 239), (178, 200, 226), (188, 205, 224),
        (15, 23, 42), (48, 67, 98),
    ),

    # Row 3: Library & Insights (09-12)
    (
        "09_keep_it_personal",
        "Unified Library",
        "Streamed and local tracks in one home.",
        "Screenshot_20261020_142741_LEVYRA.jpg",
        (240, 244, 253), (220, 229, 247), (186, 203, 238), (196, 210, 238),
        (17, 24, 39), (55, 71, 107),
    ),
    (
        "10_curated_for_you",
        "Curated Search",
        "Artist shortcuts and instant filters.",
        "Screenshot_20261024_193919_Levyra.jpg",
        (253, 245, 238), (242, 223, 206), (228, 198, 172), (226, 202, 182),
        (34, 20, 14), (112, 68, 46),
    ),
    (
        "11_your_rhythm",
        "Listening Insights",
        "On-device stats and personal habits.",
        "Screenshot_20261024_210317_Levyra.jpg",
        (238, 247, 252), (211, 232, 246), (175, 212, 238), (184, 214, 234),
        (14, 28, 42), (42, 82, 112),
    ),
    (
        "12_in_your_orbit",
        "Levyra Collections",
        "Handpicked playlists around your taste.",
        "Screenshot_20261024_210532_Levyra.jpg",
        (245, 240, 252), (226, 216, 243), (204, 188, 234), (204, 190, 228),
        (24, 17, 38), (82, 62, 118),
    ),

    # Row 4: Personalization & Artists (13-16)
    (
        "13_tailor_every_detail",
        "Skin Studio",
        "Seven complete looks in one tap.",
        "Screenshot_20261023_212906_LEVYRA.jpg",
        (246, 241, 253), (228, 217, 246), (206, 188, 238), (208, 194, 232),
        (26, 17, 42), (86, 60, 126),
    ),
    (
        "14_fresh_off_the_stage",
        "Live Radio",
        "Thousands of global stations live.",
        "Screenshot_20261024_145819_Levyra.jpg",
        (253, 241, 243), (242, 216, 221), (228, 186, 195), (226, 196, 203),
        (38, 16, 22), (116, 52, 66),
    ),
    (
        "15_meet_the_artist",
        "Meet the Artist",
        "Biographies, audience and discography.",
        "Screenshot_20261024_145036_Levyra.jpg",
        (251, 247, 240), (237, 226, 208), (222, 205, 178), (218, 204, 184),
        (28, 23, 18), (98, 80, 58),
    ),
    (
        "16_listen_together",
        "Levyra Jam",
        "Real-time shared queue with friends.",
        "Screenshot_20261011_133434_LEVYRA.jpg",
        (239, 249, 243), (213, 237, 224), (176, 220, 198), (186, 220, 202),
        (14, 34, 24), (38, 94, 68),
    ),

    # Row 5: Lyrics, Offline & Highlights (17-20)
    (
        "17_sing_along",
        "Synced Lyrics",
        "Tap-to-seek timing and translations.",
        "screen-lyrics.jpg",
        (253, 248, 236), (240, 226, 196), (228, 206, 162), (224, 206, 172),
        (32, 24, 12), (108, 78, 36),
    ),
    (
        "18_open_the_album",
        "Offline Music",
        "Full albums saved directly on device.",
        "Screenshot_20261024_193647_Levyra.jpg",
        (241, 245, 249), (218, 226, 236), (186, 200, 218), (194, 206, 220),
        (15, 23, 42), (51, 65, 85),
    ),
    (
        "19_discover_for_you",
        "Discover for You",
        "Swipe to save or explore next.",
        "Screenshot_20261024_195654_Levyra.jpg",
        (250, 243, 235), (233, 218, 200), (218, 194, 168), (214, 196, 176),
        (28, 21, 16), (102, 74, 54),
    ),
    (
        "20_your_year_in_music",
        "Your Year in Music",
        "Personal annual recap on device.",
        "Screenshot_20261016_223757_LEVYRA.jpg",
        (251, 240, 250), (238, 214, 236), (224, 184, 222), (222, 192, 220),
        (36, 14, 36), (108, 48, 106),
    ),

    # Row 6: Global Streaming Skins (21-24)
    (
        "21_spotify_style",
        "Spotify Style",
        "Familiar dark surfaces & green accents.",
        "Screenshot_20261026_204307_Levyra.jpg",
        (238, 249, 242), (211, 236, 221), (172, 220, 192), (184, 218, 198),
        (12, 36, 22), (34, 94, 58),
    ),
    (
        "22_apple_music_style",
        "Apple Music Style",
        "Editorial white shelves & bold type.",
        "Screenshot_20261024_201549_Levyra.jpg",
        (248, 250, 253), (226, 233, 243), (198, 210, 228), (198, 208, 222),
        (15, 23, 42), (190, 34, 68),
    ),
    (
        "23_youtube_music_style",
        "YouTube Music Style",
        "Dynamic mood chips & daily charts.",
        "Screenshot_20261024_201709_Levyra.jpg",
        (243, 241, 253), (222, 216, 245), (196, 186, 236), (200, 192, 230),
        (22, 16, 38), (76, 52, 128),
    ),
    (
        "24_qobuz_style",
        "Qobuz Style",
        "Magazine layout & warm hi-fi curation.",
        "Screenshot_20261030_123252_Levyra.jpg",
        (238, 246, 252), (212, 230, 245), (176, 208, 236), (186, 210, 232),
        (14, 28, 44), (40, 78, 114),
    ),
]

def render_upright_card(spec):
    (
        card_id,
        title,
        subtitle,
        screenshot_file,
        top_rgb,
        bot_rgb,
        wash_rgb,
        border_rgb,
        title_rgb,
        sub_rgb,
    ) = spec

    card = create_editorial_surface(CARD_W, CARD_H, top_rgb, bot_rgb, wash_rgb, wash_pos=(0.50, 0.60))
    draw = ImageDraw.Draw(card)

    # 1. Centered 3D Machined Titanium Pin ('Chiodo Moderno')
    draw_modern_pin(card, CARD_W // 2, 54, radius=17)

    # 2. Large Centered Display Title
    tbbox = draw.textbbox((0, 0), title, font=font_upright_title)
    tw = tbbox[2] - tbbox[0]
    tx = (CARD_W - tw) // 2
    ty = 88
    draw.text((tx, ty), title, font=font_upright_title, fill=title_rgb + (255,))

    # 3. Concise Centered 1-Line Subtitle
    sbbox = draw.textbbox((0, 0), subtitle, font=font_upright_sub)
    sw = sbbox[2] - sbbox[0]
    sx = (CARD_W - sw) // 2
    sy = 174
    draw.text((sx, sy), subtitle, font=font_upright_sub, fill=sub_rgb + (255,))

    # 4. Upright Galaxy S25 Ultra Smartphone (100% uncut, large & centered)
    dev_w = 708
    device, outer_w, outer_h, pad = create_phone_mockup(screenshot_file, outer_w=dev_w)
    phone_x = (CARD_W - outer_w) // 2 - pad
    phone_y = 264 - pad

    place_rotated_device(card, device, pos=(phone_x, phone_y), shadow_offset=(0, 34))

    return finalize_card(card, border_rgb=border_rgb)

def save_row_variants(row_cards, row_index):
    spaced_row = Image.new("RGBA", (3696, 1800), (0, 0, 0, 0))
    dense_row = Image.new("RGB", (3600, 1800), (255, 255, 255))

    for col, c in enumerate(row_cards):
        spaced_row.paste(c, (col * (CARD_W + GAP), 0), c)
        dense_row.paste(c.convert("RGB"), (col * CARD_W, 0))

    if row_index == 5:
        spaced_path = os.path.join(OUT_DIR, "editorial-grid-row-06-skins-spaced.webp")
        dense_path = os.path.join(OUT_DIR, "editorial-grid-row-06-skins.webp")
        uni_path = os.path.join(OUT_DIR, "editorial-grid-row-06-skins-uniform-centered.webp")
    else:
        idx_str = f"{row_index + 1:02d}"
        spaced_path = os.path.join(OUT_DIR, f"editorial-grid-row-{idx_str}-spaced.webp")
        dense_path = os.path.join(OUT_DIR, f"editorial-grid-row-{idx_str}.webp")
        uni_name = (
            "editorial-grid-row-01-orbit-reference.webp"
            if row_index == 0
            else f"editorial-grid-row-{idx_str}-uniform-centered.webp"
        )
        uni_path = os.path.join(OUT_DIR, uni_name)

    spaced_row.save(spaced_path, "WEBP", quality=95, method=6)
    dense_row.save(dense_path, "WEBP", quality=95, method=6)
    dense_row.save(uni_path, "WEBP", quality=95, method=6)
    print(f"Saved Row {row_index + 1}: {spaced_path}")
    return spaced_row

def main():
    print("Rendering Row 1 (Cards 01-04 with dynamic diptych cuts)...")
    row1_cards = build_row1_cards()
    row1_names = [
        "01_pure_listening",
        "02_stay_with_the_song",
        "03_daylight_expressive",
        "04_midnight_obsidian",
    ]
    for fname, c in zip(row1_names, row1_cards):
        c.save(os.path.join(OUT_DIR, fname + ".webp"), "WEBP", quality=95, method=6)

    all_cards = list(row1_cards)

    print("Rendering Rows 2-6 (Cards 05-24 as upright flagship cards)...")
    for idx, spec in enumerate(UPRIGHT_CARD_SPECS):
        fname = spec[0]
        print(f"  Card {idx + 5:02d}/24: {spec[1]} ({fname})...")
        c = render_upright_card(spec)
        c.save(os.path.join(OUT_DIR, fname + ".webp"), "WEBP", quality=95, method=6)
        all_cards.append(c)

    # Also keep legacy alias filenames in sync if present in repo
    alias_map = {
        "19_discover_for_you.webp": "19_top_charts.webp",
        "21_spotify_style.webp": "22_spotify_style.webp",
        "22_apple_music_style.webp": "23_apple_music_style.webp",
        "23_youtube_music_style.webp": "24_youtube_music_style.webp",
        "24_qobuz_style.webp": "24_signature_looks.webp",
    }
    for src_name, dst_name in alias_map.items():
        src_p = os.path.join(OUT_DIR, src_name)
        dst_p = os.path.join(OUT_DIR, dst_name)
        if os.path.exists(src_p) and os.path.exists(dst_p):
            with Image.open(src_p) as im:
                im.save(dst_p, "WEBP", quality=95, method=6)

    for row_idx in range(6):
        row_slice = all_cards[row_idx * 4 : (row_idx + 1) * 4]
        save_row_variants(row_slice, row_idx)

    print("Successfully generated all 24 showcase cards and 6 grid rows!")

if __name__ == "__main__":
    main()
