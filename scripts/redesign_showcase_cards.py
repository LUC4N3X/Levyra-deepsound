import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageEnhance

SCREENSHOT_DIR = r"C:\Users\Luca Drogo\Desktop\screenshots"
OUT_DIR = r"C:\Users\Luca Drogo\repo-work\Levyra-deepsound\docs\assets\showcase\editorial-cards"
EMBLEM_PATH = os.path.join(SCREENSHOT_DIR, "Emblema a nastro neon astratto.png")

os.makedirs(OUT_DIR, exist_ok=True)

CARD_W, CARD_H = 900, 1800
GAP = 32

# Typography — bold geometric/humanist display scale readable at GitHub README width
FONT_BOLD = r"C:\Windows\Fonts\segoeuib.ttf"
FONT_SEMIBOLD = r"C:\Windows\Fonts\segoeuisl.ttf"
FONT_REGULAR = r"C:\Windows\Fonts\segoeui.ttf"

def get_font(path, size):
    if os.path.exists(path):
        return ImageFont.truetype(path, size)
    return ImageFont.load_default()

font_display = get_font(FONT_BOLD, 76)
font_subhead = get_font(FONT_BOLD, 34)

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

def draw_ribbon_pill(card, x, y, width=60, height=7):
    """Draws a sleek editorial accent bar using the exact Levyra ribbon colors."""
    pill = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    pdraw = ImageDraw.Draw(pill)
    c1 = (79, 117, 255)   # Cobalt
    c2 = (139, 92, 246)   # Violet
    c3 = (236, 72, 153)   # Magenta
    for px in range(width):
        t = px / max(1, width - 1)
        if t < 0.5:
            lt = t * 2.0
            r = int(c1[0] * (1 - lt) + c2[0] * lt)
            g = int(c1[1] * (1 - lt) + c2[1] * lt)
            b = int(c1[2] * (1 - lt) + c2[2] * lt)
        else:
            lt = (t - 0.5) * 2.0
            r = int(c2[0] * (1 - lt) + c3[0] * lt)
            g = int(c2[1] * (1 - lt) + c3[1] * lt)
            b = int(c2[2] * (1 - lt) + c3[2] * lt)
        pdraw.line([(px, 0), (px, height)], fill=(r, g, b, 255))

    mask = Image.new("L", (width, height), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, width - 1, height - 1), radius=height // 2, fill=255)
    card.paste(pill, (x, y), mask)

def draw_editorial_block(card, title_lines, subtitle, x, y, align="left", title_fill=(15, 23, 42, 255), sub_fill=(71, 85, 105, 255)):
    """Draws a bold 2-line display headline + concise 1-line subhead."""
    draw = ImageDraw.Draw(card)
    pill_w, pill_h = 60, 7

    if align == "left":
        draw_ribbon_pill(card, x, y, pill_w, pill_h)
        curr_y = y + pill_h + 20
        for line in title_lines:
            draw.text((x, curr_y), line, font=font_display, fill=title_fill)
            curr_y += 82
        curr_y += 14
        draw.text((x, curr_y), subtitle, font=font_subhead, fill=sub_fill)
    else:
        draw_ribbon_pill(card, x - pill_w, y, pill_w, pill_h)
        curr_y = y + pill_h + 20
        for line in title_lines:
            bbox = draw.textbbox((0, 0), line, font=font_display)
            lw = bbox[2] - bbox[0]
            draw.text((x - lw, curr_y), line, font=font_display, fill=title_fill)
            curr_y += 82
        curr_y += 14
        sbbox = draw.textbbox((0, 0), subtitle, font=font_subhead)
        sw = sbbox[2] - sbbox[0]
        draw.text((x - sw, curr_y), subtitle, font=font_subhead, fill=sub_fill)

def enhance_screenshot(img):
    img = img.convert("RGB")
    img = ImageEnhance.Contrast(img).enhance(1.03)
    img = ImageEnhance.Sharpness(img).enhance(1.05)
    return img

def create_phone_mockup(screenshot_filename, outer_w=730):
    screenshot_path = os.path.join(SCREENSHOT_DIR, screenshot_filename)
    if not os.path.exists(screenshot_path):
        alt_path = os.path.join(r"C:\Users\Luca Drogo\Downloads", screenshot_filename)
        if os.path.exists(alt_path):
            screenshot_path = alt_path
        else:
            raise FileNotFoundError(f"Missing screenshot: {screenshot_filename}")

    with Image.open(screenshot_path) as src:
        screenshot = enhance_screenshot(src).convert("RGBA")

    bezel = 12
    radius = 34
    screen_radius = 24

    screen_w = outer_w - bezel * 2
    screen_h = int(round(screen_w * src.height / src.width))
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
    resized_screen = screenshot.resize((screen_w, screen_h), Image.Resampling.LANCZOS)
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

    return device

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
    """Composites an already-rotated device with a natural 3-layer studio shadow."""
    alpha = rot.getchannel("A")
    cw, ch = rot.size
    ox, oy = shadow_offset

    spad = 110
    scw, sch = cw + spad * 2, ch + spad * 2
    shadow_canvas = Image.new("RGBA", (scw, sch), (0, 0, 0, 0))

    padded_alpha = Image.new("L", (scw, sch), 0)
    padded_alpha.paste(alpha, (spad, spad))

    # Layer 1: Soft wide ambient shadow
    s3_a = padded_alpha.filter(ImageFilter.GaussianBlur(56))
    s3_a = ImageEnhance.Brightness(s3_a).enhance(0.18)
    s3 = Image.new("RGBA", (scw, sch), (15, 23, 42, 255))
    s3.putalpha(s3_a)
    shadow_canvas.alpha_composite(s3, (ox, oy))

    # Layer 2: Mid studio depth shadow
    s2_a = padded_alpha.filter(ImageFilter.GaussianBlur(28))
    s2_a = ImageEnhance.Brightness(s2_a).enhance(0.22)
    s2 = Image.new("RGBA", (scw, sch), (15, 23, 42, 255))
    s2.putalpha(s2_a)
    shadow_canvas.alpha_composite(s2, (int(ox * 0.55), int(oy * 0.55)))

    # Layer 3: Crisp contact shadow
    s1_a = padded_alpha.filter(ImageFilter.GaussianBlur(11))
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
# ROW 1 — DYNAMIC EDITORIAL DIPTYCHS (Cards 01-04)
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
    dev1 = create_phone_mockup("Screenshot_20261016_223653_LEVYRA.jpg", outer_w=725)
    rot1 = rotate_device(dev1, 12.5)
    place_rotated_device(pair1, rot1, pos=(215, -185), shadow_offset=(22, 40))

    # Phone 2: Stay with the Song rising from bottom-right at -8.5 deg
    dev2 = create_phone_mockup("Screenshot_20261016_135636_LEVYRA.jpg", outer_w=725)
    rot2 = rotate_device(dev2, -8.5)
    place_rotated_device(pair1, rot2, pos=(CARD_W + GAP + 195, 415), shadow_offset=(22, 40))

    c1 = pair1.crop((0, 0, CARD_W, CARD_H))
    c2 = pair1.crop((CARD_W + GAP, 0, pair_w, CARD_H))

    if emblem_img:
        c1.paste(emblem_img, (68, 68), emblem_img)
    draw_editorial_block(
        c1,
        title_lines=["Pure", "Listening."],
        subtitle="Your music, zero ads.",
        x=68,
        y=1420,
        align="left",
        title_fill=(17, 24, 39, 255),
        sub_fill=(55, 71, 107, 255),
    )

    draw_editorial_block(
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
    # Mathematically exact symmetrical mirror cuts across the seam
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

    dev3 = create_phone_mockup("Screenshot_20261024_204745_Levyra.jpg", outer_w=720)
    rot3 = rotate_device(dev3, -8.0)
    x3 = 105
    place_rotated_device(c3, rot3, pos=(x3, 395), shadow_offset=(20, 38))

    dev4 = create_phone_mockup("Screenshot_20261024_205815_Levyra.jpg", outer_w=720)
    rot4 = rotate_device(dev4, 8.0)
    x4 = CARD_W - x3 - rot4.width + 45
    place_rotated_device(c4, rot4, pos=(x4, 395), shadow_offset=(-20, 38))

    draw_editorial_block(
        c3,
        title_lines=["Daylight", "Expressive."],
        subtitle="Tuned for bright clarity.",
        x=68,
        y=76,
        align="left",
        title_fill=(28, 23, 18, 255),
        sub_fill=(104, 85, 62, 255),
    )

    draw_editorial_block(
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
# ROW 2 — BESPOKE EDITORIAL COMPOSITIONS (Cards 05-08)
# ==============================================================================
def build_row2_cards():
    # Card 05: Instant Search — Glacier Sapphire Porcelain, angled left-to-right showcase (-5.5 deg)
    c5 = create_editorial_surface(
        CARD_W, CARD_H,
        top_rgb=(241, 246, 254),
        bot_rgb=(218, 230, 248),
        wash_rgb=(184, 206, 242),
        wash_pos=(0.55, 0.62),
    )
    dev5 = create_phone_mockup("Screenshot_20261024_144737_Levyra.jpg", outer_w=720)
    rot5 = rotate_device(dev5, -4.2)
    place_rotated_device(c5, rot5, pos=(48, 395), shadow_offset=(20, 38))
    draw_editorial_block(
        c5,
        title_lines=["Instant", "Search."],
        subtitle="Tracks, albums & artists.",
        x=68,
        y=76,
        align="left",
        title_fill=(15, 23, 42, 255),
        sub_fill=(48, 69, 104, 255),
    )
    c5 = finalize_card(c5, border_rgb=(194, 210, 236))

    # Card 06: Player Styles — Warm Sandstone Cream, top-descending stage (+5.5 deg) + Bottom Headline
    # With +5.5 deg, pos_x=42 centers the lower half of the phone ("Choosin' Texas" + controls) inside the card!
    c6 = create_editorial_surface(
        CARD_W, CARD_H,
        top_rgb=(250, 244, 236),
        bot_rgb=(234, 220, 204),
        wash_rgb=(218, 195, 170),
        wash_pos=(0.48, 0.45),
    )
    dev6 = create_phone_mockup("Screenshot_20261024_203401_Levyra.jpg", outer_w=730)
    rot6 = rotate_device(dev6, 5.5)
    place_rotated_device(c6, rot6, pos=(42, -185), shadow_offset=(20, 38))
    draw_editorial_block(
        c6,
        title_lines=["Player", "Styles."],
        subtitle="Crafted for your taste.",
        x=68,
        y=1420,
        align="left",
        title_fill=(28, 20, 16, 255),
        sub_fill=(102, 74, 56, 255),
    )
    c6 = finalize_card(c6, border_rgb=(216, 199, 180))

    # Card 07 & Card 08: Symmetrical Converging Pair (Lavender-Quartz & Nordic Ice-Platinum)
    c7 = create_editorial_surface(
        CARD_W, CARD_H,
        top_rgb=(245, 240, 252),
        bot_rgb=(226, 216, 243),
        wash_rgb=(204, 188, 234),
        wash_pos=(0.48, 0.65),
    )
    dev7 = create_phone_mockup("Screenshot_20261015_140910_LEVYRA.jpg", outer_w=720)
    rot7 = rotate_device(dev7, -4.2)
    x7 = 48
    place_rotated_device(c7, rot7, pos=(x7, 395), shadow_offset=(20, 38))
    draw_editorial_block(
        c7,
        title_lines=["Every Mood", "& Genre."],
        subtitle="Explore by energy.",
        x=68,
        y=76,
        align="left",
        title_fill=(24, 17, 38, 255),
        sub_fill=(82, 62, 118, 255),
    )
    c7 = finalize_card(c7, border_rgb=(204, 190, 228))

    # Card 08: Acoustic Precision — Converging +4.2 deg with full left-bezel clearance so "Audio output" is 100% uncut!
    c8 = create_editorial_surface(
        CARD_W, CARD_H,
        top_rgb=(237, 243, 250),
        bot_rgb=(212, 225, 239),
        wash_rgb=(178, 200, 226),
        wash_pos=(0.52, 0.65),
    )
    dev8 = create_phone_mockup("Screenshot_20261024_150113_Levyra.jpg", outer_w=720)
    rot8 = rotate_device(dev8, 4.2)
    x8 = CARD_W - x7 - rot8.width + 25
    place_rotated_device(c8, rot8, pos=(x8, 395), shadow_offset=(-20, 38))
    draw_editorial_block(
        c8,
        title_lines=["Acoustic", "Precision."],
        subtitle="Pure 320 kbps audio.",
        x=CARD_W - 68,
        y=76,
        align="right",
        title_fill=(15, 23, 42, 255),
        sub_fill=(48, 67, 98, 255),
    )
    c8 = finalize_card(c8, border_rgb=(188, 205, 224))

    return [c5, c6, c7, c8]

CARD_FILENAMES = [
    "01_pure_listening",
    "02_stay_with_the_song",
    "03_daylight_expressive",
    "04_midnight_obsidian",
    "05_fresh_currents",
    "06_built_for_your_taste",
    "07_every_genre",
    "08_acoustic_precision",
]

def main():
    print("Building Row 1 (Cards 01-04)...")
    row1_cards = build_row1_cards()
    print("Building Row 2 (Cards 05-08)...")
    row2_cards = build_row2_cards()

    all_cards = row1_cards + row2_cards

    for fname, card_img in zip(CARD_FILENAMES, all_cards):
        out_webp = os.path.join(OUT_DIR, fname + ".webp")
        card_img.save(out_webp, "WEBP", quality=95, method=6)
        print(f"Saved: {out_webp}")

    # Assemble Row 1
    row1_spaced = Image.new("RGBA", (3696, 1800), (0, 0, 0, 0))
    row1_dense = Image.new("RGB", (3600, 1800), (255, 255, 255))
    for col, c in enumerate(row1_cards):
        row1_spaced.paste(c, (col * (CARD_W + GAP), 0), c)
        row1_dense.paste(c.convert("RGB"), (col * CARD_W, 0))

    row1_spaced.save(os.path.join(OUT_DIR, "editorial-grid-row-01-spaced.webp"), "WEBP", quality=95, method=6)
    row1_dense.save(os.path.join(OUT_DIR, "editorial-grid-row-01.webp"), "WEBP", quality=95, method=6)
    row1_dense.save(os.path.join(OUT_DIR, "editorial-grid-row-01-orbit-reference.webp"), "WEBP", quality=95, method=6)

    # Assemble Row 2
    row2_spaced = Image.new("RGBA", (3696, 1800), (0, 0, 0, 0))
    row2_dense = Image.new("RGB", (3600, 1800), (255, 255, 255))
    for col, c in enumerate(row2_cards):
        row2_spaced.paste(c, (col * (CARD_W + GAP), 0), c)
        row2_dense.paste(c.convert("RGB"), (col * CARD_W, 0))

    row2_spaced.save(os.path.join(OUT_DIR, "editorial-grid-row-02-spaced.webp"), "WEBP", quality=95, method=6)
    row2_dense.save(os.path.join(OUT_DIR, "editorial-grid-row-02.webp"), "WEBP", quality=95, method=6)
    row2_dense.save(os.path.join(OUT_DIR, "editorial-grid-row-02-uniform-centered.webp"), "WEBP", quality=95, method=6)

    print("Done generating all 8 cards and rows!")

if __name__ == "__main__":
    main()
