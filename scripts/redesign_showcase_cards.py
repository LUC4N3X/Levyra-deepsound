import os
import math
from PIL import Image, ImageDraw, ImageFont, ImageFilter, ImageEnhance

SCREENSHOT_DIR = r"C:\Users\Luca Drogo\Desktop\screenshots"
OUT_DIR = r"C:\Users\Luca Drogo\repo-work\Levyra-deepsound\docs\assets\showcase\editorial-cards"
EMBLEM_PATH = os.path.join(SCREENSHOT_DIR, "Emblema a nastro neon astratto.png")

os.makedirs(OUT_DIR, exist_ok=True)

CARD_W, CARD_H = 900, 1800
GAP = 32

# Typography
FONT_BOLD = r"C:\Windows\Fonts\segoeuib.ttf"
FONT_SEMIBOLD = r"C:\Windows\Fonts\segoeuiz.ttf"
FONT_REGULAR = r"C:\Windows\Fonts\segoeui.ttf"

def get_font(path, size):
    if os.path.exists(path):
        return ImageFont.truetype(path, size)
    return ImageFont.load_default()

font_eyebrow = get_font(FONT_BOLD, 20)
font_title = get_font(FONT_BOLD, 52)
font_sub = get_font(FONT_REGULAR, 26)

# Load emblem
emblem_img = None
if os.path.exists(EMBLEM_PATH):
    emblem_raw = Image.open(EMBLEM_PATH).convert("RGBA")
    ew = 44
    eh = int(round(ew * emblem_raw.height / emblem_raw.width))
    emblem_img = emblem_raw.resize((ew, eh), Image.Resampling.LANCZOS)

def draw_spaced_text(draw, pos, text, font, fill, letter_spacing=3):
    x, y = pos
    for char in text:
        draw.text((x, y), char, font=font, fill=fill)
        bbox = draw.textbbox((0, 0), char, font=font)
        w = bbox[2] - bbox[0]
        x += w + letter_spacing

def get_spaced_text_width(draw, text, font, letter_spacing=3):
    total = 0
    for char in text:
        bbox = draw.textbbox((0, 0), char, font=font)
        w = bbox[2] - bbox[0]
        total += w + letter_spacing
    if len(text) > 0:
        total -= letter_spacing
    return total

def enhance_screenshot(img):
    img = img.convert("RGB")
    enhancer = ImageEnhance.Contrast(img)
    img = enhancer.enhance(1.04)
    enhancer = ImageEnhance.Sharpness(img)
    img = enhancer.enhance(1.06)
    return img

def create_phone_mockup(screenshot_filename, outer_w=720):
    screenshot_path = os.path.join(SCREENSHOT_DIR, screenshot_filename)
    if not os.path.exists(screenshot_path):
        alt_path = os.path.join(r"C:\Users\Luca Drogo\Downloads", screenshot_filename)
        if os.path.exists(alt_path):
            screenshot_path = alt_path
        else:
            raise FileNotFoundError(f"Missing screenshot: {screenshot_filename}")

    with Image.open(screenshot_path) as src:
        screenshot = enhance_screenshot(src).convert("RGBA")

    bezel = 11
    radius = 32
    screen_radius = 24

    screen_w = outer_w - bezel * 2
    screen_h = int(round(screen_w * 2993 / 1440))
    outer_h = screen_h + bezel * 2

    pad = 40
    cw, ch = outer_w + pad * 2, outer_h + pad * 2
    device = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))
    draw = ImageDraw.Draw(device)

    body = (pad, pad, pad + outer_w - 1, pad + outer_h - 1)

    # 1. Titanium Dark Chassis Base
    draw.rounded_rectangle(body, radius=radius, fill=(26, 28, 34, 255))

    # 2. Outer Chamfer / Metallic Specular Rim
    draw.rounded_rectangle(body, radius=radius, outline=(180, 185, 198, 255), width=2)

    # 3. Inner frame depth
    inner_body = (body[0] + 2, body[1] + 2, body[2] - 2, body[3] - 2)
    draw.rounded_rectangle(inner_body, radius=radius - 2, outline=(42, 45, 54, 255), width=1)

    # 4. Screenshot content
    resized_screen = screenshot.resize((screen_w, screen_h), Image.Resampling.LANCZOS)
    screen_mask = Image.new("L", (screen_w, screen_h), 0)
    ImageDraw.Draw(screen_mask).rounded_rectangle(
        (0, 0, screen_w - 1, screen_h - 1), radius=screen_radius, fill=255
    )

    screen_x = pad + bezel
    screen_y = pad + bezel
    device.paste(resized_screen, (screen_x, screen_y), screen_mask)

    # 5. Screen inner hairline border
    draw.rounded_rectangle(
        (screen_x, screen_y, screen_x + screen_w - 1, screen_y + screen_h - 1),
        radius=screen_radius,
        outline=(12, 14, 18, 255),
        width=1,
    )

    # 6. Selfie camera punch-hole (Galaxy S25 Ultra Infinity-O)
    cam_x = pad + outer_w // 2
    cam_y = screen_y + 16
    cam_r = 7
    draw.ellipse((cam_x - cam_r, cam_y - cam_r, cam_x + cam_r, cam_y + cam_r), fill=(5, 6, 8, 255))
    draw.ellipse((cam_x - 2, cam_y - 2, cam_x + 1, cam_y + 1), fill=(35, 60, 100, 210))

    # 7. Earpiece micro slit at top rim
    draw.rounded_rectangle((cam_x - 28, pad + 2, cam_x + 28, pad + 5), radius=1, fill=(65, 68, 78, 255))

    # 8. Titanium side buttons (Volume and Power on right edge)
    btn_w = 3
    btn_x = pad + outer_w - 1
    v_top = pad + int(outer_h * 0.22)
    v_bot = pad + int(outer_h * 0.33)
    draw.rounded_rectangle((btn_x, v_top, btn_x + btn_w, v_bot), radius=1, fill=(130, 135, 148, 255))
    p_top = pad + int(outer_h * 0.38)
    p_bot = pad + int(outer_h * 0.45)
    draw.rounded_rectangle((btn_x, p_top, btn_x + btn_w, p_bot), radius=1, fill=(130, 135, 148, 255))

    return device, outer_w, outer_h, pad

def create_card_background(w, h, top_rgb, bot_rgb, bloom_rgb, is_light=False):
    card = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(card)

    for y in range(h):
        mix = y / (h - 1)
        r = int(top_rgb[0] * (1 - mix) + bot_rgb[0] * mix)
        g = int(top_rgb[1] * (1 - mix) + bot_rgb[1] * mix)
        b = int(top_rgb[2] * (1 - mix) + bot_rgb[2] * mix)
        draw.line([(0, y), (w, y)], fill=(r, g, b, 255))

    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    cx, cy = w // 2, int(h * 0.62)
    rx, ry = int(w * 0.50), int(h * 0.36)
    opacity = 20 if is_light else 32
    glow_draw.ellipse(
        (cx - rx, cy - ry, cx + rx, cy + ry),
        fill=bloom_rgb + (opacity,),
    )
    glow = glow.filter(ImageFilter.GaussianBlur(150))
    card.alpha_composite(glow)
    return card

def render_multi_layer_shadow(alpha_mask, cw, ch, is_light=False, offset_x=20, offset_y=36):
    shadow_canvas = Image.new("RGBA", (cw, ch), (0, 0, 0, 0))

    # Layer 1: Tight contact shadow
    s1_alpha = alpha_mask.filter(ImageFilter.GaussianBlur(14))
    s1_alpha = ImageEnhance.Brightness(s1_alpha).enhance(0.40 if is_light else 0.32)
    s1 = Image.new("RGBA", (cw, ch), (10, 12, 16, 255))
    s1.putalpha(s1_alpha)
    shadow_canvas.alpha_composite(s1, (int(offset_x * 0.3), int(offset_y * 0.3)))

    # Layer 2: Medium ambient depth shadow
    s2_alpha = alpha_mask.filter(ImageFilter.GaussianBlur(38))
    s2_alpha = ImageEnhance.Brightness(s2_alpha).enhance(0.28 if is_light else 0.22)
    s2 = Image.new("RGBA", (cw, ch), (12, 14, 20, 255))
    s2.putalpha(s2_alpha)
    shadow_canvas.alpha_composite(s2, (int(offset_x * 0.6), int(offset_y * 0.6)))

    # Layer 3: Soft wide diffusion shadow
    s3_alpha = alpha_mask.filter(ImageFilter.GaussianBlur(72))
    s3_alpha = ImageEnhance.Brightness(s3_alpha).enhance(0.18 if is_light else 0.12)
    s3 = Image.new("RGBA", (cw, ch), (15, 18, 25, 255))
    s3.putalpha(s3_alpha)
    shadow_canvas.alpha_composite(s3, (offset_x, offset_y))

    return shadow_canvas

def apply_card_frame_and_mask(card, is_light=False):
    w, h = card.size
    mask = Image.new("L", (w, h), 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, w - 1, h - 1), radius=40, fill=255)

    border_outline = (0, 0, 0, 18) if is_light else (255, 255, 255, 20)
    frame = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(frame).rounded_rectangle((0, 0, w - 1, h - 1), radius=40, outline=border_outline, width=2)
    card.alpha_composite(frame)
    card.putalpha(mask)
    return card

# ==============================================================================
# 1. HERO DIPTYCH (Cards 01 Pure Listening & 02 Stay with the Song)
# ==============================================================================
def render_hero_diptych(spec1, spec2):
    pair_w = CARD_W * 2 + GAP
    bg1 = create_card_background(CARD_W, CARD_H, spec1[5], spec1[6], spec1[7], is_light=spec1[8])
    bg2 = create_card_background(CARD_W, CARD_H, spec2[5], spec2[6], spec2[7], is_light=spec2[8])

    pair_canvas = Image.new("RGBA", (pair_w, CARD_H), (0, 0, 0, 0))
    pair_canvas.paste(bg1, (0, 0))
    pair_canvas.paste(bg2, (CARD_W + GAP, 0))

    # Left phone (Pure Listening): width=720, angle=13 deg, pos=(220, -150)
    dev1, ow1, oh1, pad1 = create_phone_mockup(spec1[4], outer_w=720)
    rot1 = dev1.rotate(13, resample=Image.Resampling.BICUBIC, expand=True)
    shadow1 = render_multi_layer_shadow(rot1.getchannel("A"), rot1.width, rot1.height, is_light=spec1[8], offset_x=22, offset_y=42)
    pair_canvas.alpha_composite(shadow1, (220, -150))
    pair_canvas.alpha_composite(rot1, (220, -150))

    # Right phone (Stay with the Song): width=720, angle=-8.5 deg, pos=(CARD_W + GAP + 200, 370)
    dev2, ow2, oh2, pad2 = create_phone_mockup(spec2[4], outer_w=720)
    rot2 = dev2.rotate(-8.5, resample=Image.Resampling.BICUBIC, expand=True)
    shadow2 = render_multi_layer_shadow(rot2.getchannel("A"), rot2.width, rot2.height, is_light=spec2[8], offset_x=22, offset_y=42)
    pair_canvas.alpha_composite(shadow2, (CARD_W + GAP + 200, 370))
    pair_canvas.alpha_composite(rot2, (CARD_W + GAP + 200, 370))

    # Crop into individual cards
    card1 = pair_canvas.crop((0, 0, CARD_W, CARD_H))
    card2 = pair_canvas.crop((CARD_W + GAP, 0, pair_w, CARD_H))

    # Card 1 Typography: Bottom-Left Anchored
    draw1 = ImageDraw.Draw(card1)
    if emblem_img:
        card1.paste(emblem_img, (72, 82), emblem_img)

    draw_spaced_text(draw1, (72, 1400), spec1[1], font_eyebrow, spec1[7] + (255,), letter_spacing=3)
    draw1.text((72, 1444), spec1[2], font=font_title, fill=(255, 255, 255, 255))
    draw1.text((72, 1516), spec1[3], font=font_sub, fill=(148, 163, 184, 255), spacing=8)

    # Card 2 Typography: Top-Right Anchored
    draw2 = ImageDraw.Draw(card2)
    right_margin = CARD_W - 72

    # Emblem on top right
    if emblem_img:
        card2.paste(emblem_img, (right_margin - emblem_img.width, 50), emblem_img)

    # Eyebrow right-aligned
    eyebrow2_w = get_spaced_text_width(draw2, spec2[1], font_eyebrow, letter_spacing=3)
    draw_spaced_text(draw2, (right_margin - eyebrow2_w, 104), spec2[1], font_eyebrow, spec2[7] + (255,), letter_spacing=3)

    # Title right-aligned
    t2_bbox = draw2.textbbox((0, 0), spec2[2], font=font_title)
    t2_w = t2_bbox[2] - t2_bbox[0]
    draw2.text((right_margin - t2_w, 146), spec2[2], font=font_title, fill=(255, 255, 255, 255))

    # Subtitle right-aligned
    sub_lines2 = spec2[3].split("\n")
    for i, line in enumerate(sub_lines2):
        l_bbox = draw2.textbbox((0, 0), line, font=font_sub)
        l_w = l_bbox[2] - l_bbox[0]
        draw2.text((right_margin - l_w, 216 + i * 36), line, font=font_sub, fill=(148, 163, 184, 255))

    card1 = apply_card_frame_and_mask(card1, is_light=spec1[8])
    card2 = apply_card_frame_and_mask(card2, is_light=spec2[8])
    return card1, card2

# ==============================================================================
# 2. THEME DIPTYCH (Cards 03 Daylight Expressive & 04 Midnight Obsidian)
# ==============================================================================
def render_theme_diptych(spec3, spec4):
    pair_w = CARD_W * 2 + GAP
    bg3 = create_card_background(CARD_W, CARD_H, spec3[5], spec3[6], spec3[7], is_light=spec3[8])
    bg4 = create_card_background(CARD_W, CARD_H, spec4[5], spec4[6], spec4[7], is_light=spec4[8])

    pair_canvas = Image.new("RGBA", (pair_w, CARD_H), (0, 0, 0, 0))
    pair_canvas.paste(bg3, (0, 0))
    pair_canvas.paste(bg4, (CARD_W + GAP, 0))

    # Left phone (Daylight Expressive): width=720, angle=-10.5 deg, pos=(120, 310)
    dev3, ow3, oh3, pad3 = create_phone_mockup(spec3[4], outer_w=720)
    rot3 = dev3.rotate(-10.5, resample=Image.Resampling.BICUBIC, expand=True)
    shadow3 = render_multi_layer_shadow(rot3.getchannel("A"), rot3.width, rot3.height, is_light=spec3[8], offset_x=20, offset_y=40)
    pair_canvas.alpha_composite(shadow3, (120, 310))
    pair_canvas.alpha_composite(rot3, (120, 310))

    # Right phone (Midnight Obsidian): width=720, angle=11.0 deg, pos=(CARD_W + GAP - 30, 390)
    dev4, ow4, oh4, pad4 = create_phone_mockup(spec4[4], outer_w=720)
    rot4 = dev4.rotate(11.0, resample=Image.Resampling.BICUBIC, expand=True)
    shadow4 = render_multi_layer_shadow(rot4.getchannel("A"), rot4.width, rot4.height, is_light=spec4[8], offset_x=22, offset_y=42)
    pair_canvas.alpha_composite(shadow4, (CARD_W + GAP - 30, 390))
    pair_canvas.alpha_composite(rot4, (CARD_W + GAP - 30, 390))

    card3 = pair_canvas.crop((0, 0, CARD_W, CARD_H))
    card4 = pair_canvas.crop((CARD_W + GAP, 0, pair_w, CARD_H))

    # Card 3 Typography: Top-Left Anchored (Light theme)
    draw3 = ImageDraw.Draw(card3)
    if emblem_img:
        card3.paste(emblem_img, (CARD_W - 72 - emblem_img.width, 82), emblem_img)

    draw_spaced_text(draw3, (72, 82), spec3[1], font_eyebrow, spec3[7] + (255,), letter_spacing=3)
    draw3.text((72, 126), spec3[2], font=font_title, fill=(15, 23, 42, 255))
    draw3.text((72, 196), spec3[3], font=font_sub, fill=(71, 85, 105, 255), spacing=8)

    # Card 4 Typography: Top-Right Anchored (Dark theme)
    draw4 = ImageDraw.Draw(card4)
    right_margin4 = CARD_W - 72

    if emblem_img:
        card4.paste(emblem_img, (72, 82), emblem_img)

    eyebrow4_w = get_spaced_text_width(draw4, spec4[1], font_eyebrow, letter_spacing=3)
    draw_spaced_text(draw4, (right_margin4 - eyebrow4_w, 82), spec4[1], font_eyebrow, spec4[7] + (255,), letter_spacing=3)

    t4_bbox = draw4.textbbox((0, 0), spec4[2], font=font_title)
    t4_w = t4_bbox[2] - t4_bbox[0]
    draw4.text((right_margin4 - t4_w, 126), spec4[2], font=font_title, fill=(255, 255, 255, 255))

    sub_lines4 = spec4[3].split("\n")
    for i, line in enumerate(sub_lines4):
        l_bbox = draw4.textbbox((0, 0), line, font=font_sub)
        l_w = l_bbox[2] - l_bbox[0]
        draw4.text((right_margin4 - l_w, 196 + i * 36), line, font=font_sub, fill=(148, 163, 184, 255))

    card3 = apply_card_frame_and_mask(card3, is_light=spec3[8])
    card4 = apply_card_frame_and_mask(card4, is_light=spec4[8])
    return card3, card4

# ==============================================================================
# 3. CENTERED CARDS (Cards 05 to 08)
# ==============================================================================
def render_centered_card(spec):
    card = create_card_background(CARD_W, CARD_H, spec[5], spec[6], spec[7], is_light=spec[8])
    draw = ImageDraw.Draw(card)

    header_x = 72
    eyebrow_y = 82
    title_y = 126
    sub_y = 196

    if spec[8]:  # is_light
        eyebrow_fill = spec[7] + (255,)
        title_fill = (15, 23, 42, 255)
        sub_fill = (71, 85, 105, 255)
    else:
        eyebrow_fill = spec[7] + (255,)
        title_fill = (255, 255, 255, 255)
        sub_fill = (148, 163, 184, 255)

    draw_spaced_text(draw, (header_x, eyebrow_y), spec[1], font_eyebrow, eyebrow_fill, letter_spacing=3)

    if emblem_img:
        emblem_x = CARD_W - header_x - emblem_img.width
        emblem_y = eyebrow_y - 2
        card.paste(emblem_img, (emblem_x, emblem_y), emblem_img)

    draw.text((header_x, title_y), spec[2], font=font_title, fill=title_fill)
    draw.text((header_x, sub_y), spec[3], font=font_sub, fill=sub_fill, spacing=8)

    dev_w = 708
    device, outer_w, outer_h, pad = create_phone_mockup(spec[4], dev_w)
    phone_x = (CARD_W - outer_w) // 2
    phone_y = 334

    dev_alpha = device.getchannel("A")
    shadow = render_multi_layer_shadow(dev_alpha, device.width, device.height, is_light=spec[8], offset_x=0, offset_y=38)

    card.alpha_composite(shadow, (phone_x - pad, phone_y - pad))
    card.alpha_composite(device, (phone_x - pad, phone_y - pad))

    card = apply_card_frame_and_mask(card, is_light=spec[8])
    return card

# Specification for all 8 cards
CARD_SPECS = [
    # 01. Pure Listening
    (
        "01_pure_listening",
        "SMART RADIO • ZERO ADS",
        "Pure Listening",
        "Smart radio crafted around your sound.\nZero ads, zero accounts, pure local audio.",
        "Screenshot_20261016_223653_LEVYRA.jpg",
        (18, 20, 26),   # Top: Dark Charcoal
        (11, 12, 16),   # Bot: Deep Graphite
        (79, 117, 255), # Accent: Levyra Acoustic Blue
        False,
    ),
    # 02. Stay with the Song
    (
        "02_stay_with_the_song",
        "IMMERSIVE PLAYBACK",
        "Stay with the Song",
        "Fullscreen artwork with synchronized lyrics,\ntap-to-seek, and vocal timing precision.",
        "Screenshot_20261016_135636_LEVYRA.jpg",
        (19, 14, 23),   # Top: Midnight Velvet Plum
        (10, 8, 14),    # Bot: Deep Obsidian
        (255, 46, 147), # Accent: Levyra Magenta
        False,
    ),
    # 03. Daylight Expressive
    (
        "03_daylight_expressive",
        "LIGHT THEME",
        "Daylight Expressive",
        "Natural warmth and high contrast tuned\nfor effortless daylight clarity and reading.",
        "Screenshot_20261024_204745_Levyra.jpg",
        (252, 250, 246), # Top: Warm Luxury Ivory
        (244, 239, 231), # Bot: Soft Studio Linen
        (180, 83, 9),    # Accent: Warm Honey Amber
        True,
    ),
    # 04. Midnight Obsidian
    (
        "04_midnight_obsidian",
        "AMOLED PRECISION",
        "Midnight Obsidian",
        "Deep obsidian surfaces with vibrant accents,\ncrafted for true-black OLED depth.",
        "Screenshot_20261024_205815_Levyra.jpg",
        (11, 13, 17),   # Top: Dark Pitch Obsidian
        (6, 7, 9),      # Bot: True Pitch Black
        (0, 210, 255),  # Accent: Electric Cyan
        False,
    ),
    # 05. Instant Search
    (
        "05_fresh_currents",
        "REAL-TIME DISCOVERY",
        "Instant Search",
        "Find any track, album, or artist with instant\nlive results and tailored suggestions.",
        "Screenshot_20261024_144737_Levyra.jpg",
        (248, 249, 252), # Top: Cool Studio Platinum
        (238, 241, 247), # Bot: Light Titanium
        (37, 99, 235),   # Accent: Electric Sapphire
        True,
    ),
    # 06. Player Styles
    (
        "06_built_for_your_taste",
        "CUSTOM ERGONOMICS",
        "Player Styles",
        "Choose between Classic, Modern, Minimal,\nand Expressive layouts in a single tap.",
        "Screenshot_20261024_203401_Levyra.jpg",
        (19, 21, 28),    # Top: Studio Titanium
        (12, 13, 18),    # Bot: Deep Charcoal
        (139, 92, 246),  # Accent: Amethyst Violet
        False,
    ),
    # 07. Every Mood and Genre
    (
        "07_every_genre",
        "CURATED CATALOG",
        "Every Mood and Genre",
        "Explore curated stations and categories\narranged by energy, place, and atmosphere.",
        "Screenshot_20261015_140910_LEVYRA.jpg",
        (18, 15, 22),    # Top: Cosmic Night
        (10, 9, 14),     # Bot: Deep Indigo Black
        (244, 63, 94),   # Accent: Rose Quartz Coral
        False,
    ),
    # 08. Acoustic Precision
    (
        "08_acoustic_precision",
        "AUDIOPHILE SOUND",
        "Acoustic Precision",
        "Verified 320 kbps streams, FLAC lossless\nsupport, and parametric 10-band equalizer.",
        "Screenshot_20261024_150113_Levyra.jpg",
        (15, 17, 23),    # Top: Audiophile Dark
        (8, 9, 13),      # Bot: Pitch Dark
        (20, 184, 166),  # Accent: Precision Teal
        False,
    ),
]

def main():
    print("Generating premium showcase cards with editorial diptych cuts and centered flagship styles...")
    cards = []

    # 1. Hero Diptych (Cards 01 & 02)
    print("Rendering Hero Diptych (Cards 01 & 02)...")
    c1, c2 = render_hero_diptych(CARD_SPECS[0], CARD_SPECS[1])
    cards.append(c1)
    cards.append(c2)

    # 2. Theme Diptych (Cards 03 & 04)
    print("Rendering Theme Diptych (Cards 03 & 04)...")
    c3, c4 = render_theme_diptych(CARD_SPECS[2], CARD_SPECS[3])
    cards.append(c3)
    cards.append(c4)

    # 3. Centered Cards (Cards 05 to 08)
    for idx in range(4, 8):
        spec = CARD_SPECS[idx]
        print(f"Rendering Centered Card {idx + 1}/8: {spec[2]} ({spec[0]})...")
        c = render_centered_card(spec)
        cards.append(c)

    # Save individual WebP cards
    for idx, card_img in enumerate(cards):
        spec = CARD_SPECS[idx]
        out_webp = os.path.join(OUT_DIR, spec[0] + ".webp")
        card_img.save(out_webp, "WEBP", quality=95, method=6)
        print(f"Saved: {out_webp}")

    # Build Row 1 (Cards 01..04)
    row1_spaced = Image.new("RGBA", (3696, 1800), (0, 0, 0, 0))
    row1_dense = Image.new("RGB", (3600, 1800), (0, 0, 0))
    for col in range(4):
        c = cards[col]
        row1_spaced.paste(c, (col * (CARD_W + GAP), 0), c)
        row1_dense.paste(c.convert("RGB"), (col * CARD_W, 0))

    r1_spaced_path = os.path.join(OUT_DIR, "editorial-grid-row-01-spaced.webp")
    r1_dense_path = os.path.join(OUT_DIR, "editorial-grid-row-01.webp")
    r1_ref_path = os.path.join(OUT_DIR, "editorial-grid-row-01-orbit-reference.webp")
    row1_spaced.save(r1_spaced_path, "WEBP", quality=95, method=6)
    row1_dense.save(r1_dense_path, "WEBP", quality=95, method=6)
    row1_dense.save(r1_ref_path, "WEBP", quality=95, method=6)
    print(f"Saved Row 1: {r1_spaced_path}")

    # Build Row 2 (Cards 05..08)
    row2_spaced = Image.new("RGBA", (3696, 1800), (0, 0, 0, 0))
    row2_dense = Image.new("RGB", (3600, 1800), (0, 0, 0))
    for col in range(4):
        c = cards[4 + col]
        row2_spaced.paste(c, (col * (CARD_W + GAP), 0), c)
        row2_dense.paste(c.convert("RGB"), (col * CARD_W, 0))

    r2_spaced_path = os.path.join(OUT_DIR, "editorial-grid-row-02-spaced.webp")
    r2_dense_path = os.path.join(OUT_DIR, "editorial-grid-row-02.webp")
    r2_uni_path = os.path.join(OUT_DIR, "editorial-grid-row-02-uniform-centered.webp")
    row2_spaced.save(r2_spaced_path, "WEBP", quality=95, method=6)
    row2_dense.save(r2_dense_path, "WEBP", quality=95, method=6)
    row2_dense.save(r2_uni_path, "WEBP", quality=95, method=6)
    print(f"Saved Row 2: {r2_spaced_path}")

    print("All 8 showcase cards and grid rows successfully created!")

if __name__ == "__main__":
    main()
