import os
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageEnhance

SCREENSHOT_DIR = r"C:\Users\Luca Drogo\Desktop\screenshots"
OUT_SHOWCASE_DIR = r"docs\assets\showcase"
OUT_SCREENSHOTS_DIR = r"docs\assets\screenshots"

os.makedirs(OUT_SHOWCASE_DIR, exist_ok=True)
os.makedirs(OUT_SCREENSHOTS_DIR, exist_ok=True)

SCREENS = {
    "home": r"C:\Users\Luca Drogo\Downloads\Screenshot_20260926_171253_LEVYRA.jpg",
    "charts": "screen-charts.jpg",
    "genres": "screen-genres.jpg",
    "listening_pulse": "screen-listening-pulse.jpg",
    "lyrics": "screen-lyrics.jpg",
    "now_playing": "screen-player-nowplaying.jpg",
    "search_artist": "screen-search-artist.jpg",
    "artist_discography": "screen-artist-discography.jpg",
}

def get_screen_path(filename):
    return filename if os.path.isabs(filename) else os.path.join(SCREENSHOT_DIR, filename)

def get_font(size, bold=False):
    font_path = r"C:\Windows\Fonts\segoeuib.ttf" if bold else r"C:\Windows\Fonts\segoeui.ttf"
    if not os.path.exists(font_path):
        font_path = r"C:\Windows\Fonts\arialbd.ttf" if bold else r"C:\Windows\Fonts\arial.ttf"
    return ImageFont.truetype(font_path, size)

def enhance_screenshot(img):
    """Subtle polish: OLED contrast, slight vibrance, crisp sharpness."""
    img = img.convert("RGB")
    enhancer = ImageEnhance.Contrast(img)
    img = enhancer.enhance(1.04)
    enhancer = ImageEnhance.Color(img)
    img = enhancer.enhance(1.05)
    enhancer = ImageEnhance.Sharpness(img)
    img = enhancer.enhance(1.10)
    return img

def create_phone_frame(screen_img, target_height=1400, bezel_color=(20, 22, 28)):
    """
    Renders a realistic, ultra-sleek modern bezel frame around the screenshot with antialiasing,
    rounded screen corners, edge reflection, and drop shadow.
    """
    screen_img = enhance_screenshot(screen_img)

    orig_w, orig_h = screen_img.size
    aspect = orig_w / orig_h

    screen_h = int(target_height)
    screen_w = int(screen_h * aspect)

    bezel_lr = int(screen_w * 0.032)
    bezel_tb = int(screen_h * 0.022)
    corner_radius = int(screen_w * 0.11)
    screen_corner_radius = int(screen_w * 0.08)

    frame_w = screen_w + bezel_lr * 2
    frame_h = screen_h + bezel_tb * 2

    SS = 2
    canvas_w = (frame_w + 60) * SS
    canvas_h = (frame_h + 60) * SS

    body_img = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(body_img)

    phone_x0 = 30 * SS
    phone_y0 = 30 * SS
    phone_x1 = phone_x0 + frame_w * SS
    phone_y1 = phone_y0 + frame_h * SS

    # Outer phone titanium bezel
    draw.rounded_rectangle(
        [phone_x0, phone_y0, phone_x1, phone_y1],
        radius=corner_radius * SS,
        fill=(bezel_color[0], bezel_color[1], bezel_color[2], 255),
        outline=(65, 72, 88, 255),
        width=int(2 * SS)
    )

    # Inner border
    inner_border_inset = int(1.5 * SS)
    draw.rounded_rectangle(
        [phone_x0 + inner_border_inset, phone_y0 + inner_border_inset,
         phone_x1 - inner_border_inset, phone_y1 - inner_border_inset],
        radius=(corner_radius - 2) * SS,
        outline=(12, 14, 18, 255),
        width=int(1.5 * SS)
    )

    # Resize screen to target
    resized_screen = screen_img.resize((screen_w * SS, screen_h * SS), Image.Resampling.LANCZOS).convert("RGBA")

    screen_mask = Image.new("L", (screen_w * SS, screen_h * SS), 0)
    mask_draw = ImageDraw.Draw(screen_mask)
    mask_draw.rounded_rectangle(
        [0, 0, screen_w * SS, screen_h * SS],
        radius=screen_corner_radius * SS,
        fill=255
    )

    screen_x = phone_x0 + bezel_lr * SS
    screen_y = phone_y0 + bezel_tb * SS

    body_img.paste(resized_screen, (screen_x, screen_y), screen_mask)

    # Subtle top speaker slit in the top bezel
    speaker_w = int(50 * SS)
    speaker_h = int(3 * SS)
    spk_x0 = (phone_x0 + phone_x1 - speaker_w) // 2
    spk_y0 = phone_y0 + int(7 * SS)
    draw.rounded_rectangle([spk_x0, spk_y0, spk_x0 + speaker_w, spk_y0 + speaker_h], radius=int(1.5 * SS), fill=(40, 44, 52, 255))

    framed = body_img.resize((frame_w + 60, frame_h + 60), Image.Resampling.LANCZOS)
    return framed

def create_ambient_glow(width, height, center, radius, color, max_alpha=120):
    glow = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    draw = ImageDraw.Draw(glow)
    cx, cy = center
    r = radius
    for step in range(25, 0, -1):
        curr_r = int(r * (step / 25.0))
        alpha = int(max_alpha * (1.0 - (step / 25.0)**0.7))
        draw.ellipse([cx - curr_r, cy - curr_r, cx + curr_r, cy + curr_r], fill=(color[0], color[1], color[2], alpha))
    glow = glow.filter(ImageFilter.GaussianBlur(int(radius * 0.45)))
    return glow

def create_studio_shadow(phone_img, blur_radius=35, opacity=150, offset=(0, 22)):
    w, h = phone_img.size
    shadow_canvas = Image.new("RGBA", (w + blur_radius * 2 + abs(offset[0]), h + blur_radius * 2 + abs(offset[1])), (0, 0, 0, 0))
    alpha = phone_img.split()[3]
    shadow_mask = Image.new("RGBA", phone_img.size, (0, 0, 0, opacity))
    shadow_mask.putalpha(alpha)
    paste_x = blur_radius + max(0, offset[0])
    paste_y = blur_radius + max(0, offset[1])
    shadow_canvas.paste(shadow_mask, (paste_x, paste_y), shadow_mask)
    shadow_canvas = shadow_canvas.filter(ImageFilter.GaussianBlur(blur_radius))
    return shadow_canvas

def draw_vector_sparkle(draw, center, radius, color):
    """Draws a 4-point diamond sparkle (like ✦)."""
    cx, cy = center
    r = radius
    r_inner = r * 0.28
    points = [
        (cx, cy - r),
        (cx + r_inner, cy - r_inner),
        (cx + r, cy),
        (cx + r_inner, cy + r_inner),
        (cx, cy + r),
        (cx - r_inner, cy + r_inner),
        (cx - r, cy),
        (cx - r_inner, cy - r_inner)
    ]
    draw.polygon(points, fill=color)

def generate_individual_framed_screenshots():
    print("Generating individual framed screenshots...")
    for key, filename in SCREENS.items():
        src_path = get_screen_path(filename)
        if not os.path.exists(src_path):
            print(f"Skipping missing: {filename}")
            continue

        img = Image.open(src_path)
        framed = create_phone_frame(img, target_height=1200)

        out_name = f"{key}.webp"
        out_path = os.path.join(OUT_SCREENSHOTS_DIR, out_name)
        framed.save(out_path, "WEBP", quality=92, method=6)
        print(f"Saved individual: {out_path}")

def generate_studio_dual_card(
    card_id,
    title_category,
    title_main,
    subtitle,
    screen1_key,
    screen2_key,
    primary_glow_color,
    secondary_glow_color,
    features_list=None
):
    """
    Renders a high-end 1600x960 studio showcase banner with two floating phones,
    ambient studio lighting, subtle grid/gradient background, and crisp typography.
    """
    card_w, card_h = 1600, 960
    canvas = Image.new("RGBA", (card_w, card_h), (10, 12, 16, 255))

    # 1. Background studio lighting & ambient gradients
    bg_glow1 = create_ambient_glow(card_w, card_h, (1080, 420), 550, primary_glow_color, max_alpha=95)
    bg_glow2 = create_ambient_glow(card_w, card_h, (1320, 680), 450, secondary_glow_color, max_alpha=75)
    bg_glow3 = create_ambient_glow(card_w, card_h, (250, 180), 350, (30, 40, 65), max_alpha=45)

    canvas = Image.alpha_composite(canvas, bg_glow1)
    canvas = Image.alpha_composite(canvas, bg_glow2)
    canvas = Image.alpha_composite(canvas, bg_glow3)

    # Top subtle border
    draw = ImageDraw.Draw(canvas)
    draw.line([(0, 0), (card_w, 0)], fill=(50, 56, 70, 200), width=1)

    # 2. Left Column: Studio Typography & Feature badges
    tag_font = get_font(16, bold=True)
    title_font = get_font(42, bold=True)
    sub_font = get_font(19, bold=False)
    bullet_font = get_font(17, bold=False)

    # Category badge pill
    cat_text = title_category.upper()
    cat_bbox = tag_font.getbbox(cat_text)
    cat_w = cat_bbox[2] - cat_bbox[0]
    cat_h = cat_bbox[3] - cat_bbox[1]

    pill_x, pill_y = 75, 80
    sparkle_pad = 28
    pill_pad_x, pill_pad_y = 16, 8
    pill_rect = [pill_x, pill_y, pill_x + cat_w + pill_pad_x * 2 + sparkle_pad, pill_y + cat_h + pill_pad_y * 2]

    pill_overlay = Image.new("RGBA", (card_w, card_h), (0, 0, 0, 0))
    pill_draw = ImageDraw.Draw(pill_overlay)
    pill_draw.rounded_rectangle(pill_rect, radius=8, fill=(primary_glow_color[0], primary_glow_color[1], primary_glow_color[2], 40), outline=(primary_glow_color[0], primary_glow_color[1], primary_glow_color[2], 130), width=1)

    # Draw vector diamond inside pill
    draw_vector_sparkle(pill_draw, (pill_x + pill_pad_x + 8, pill_y + pill_pad_y + cat_h // 2), 7, (230, 240, 255, 240))
    pill_draw.text((pill_x + pill_pad_x + sparkle_pad, pill_y + pill_pad_y - cat_bbox[1]), cat_text, font=tag_font, fill=(235, 240, 255, 255))
    canvas = Image.alpha_composite(canvas, pill_overlay)

    # Main Title
    draw = ImageDraw.Draw(canvas)
    y_cursor = pill_y + cat_h + pill_pad_y * 2 + 28

    for line in title_main.split("\n"):
        draw.text((75, y_cursor), line, font=title_font, fill=(255, 255, 255, 255))
        y_cursor += 52

    y_cursor += 10
    # Subtitle
    for line in subtitle.split("\n"):
        draw.text((75, y_cursor), line, font=sub_font, fill=(160, 172, 195, 255))
        y_cursor += 28

    y_cursor += 24

    # Feature bullets
    if features_list:
        for feat in features_list:
            draw_vector_sparkle(draw, (84, y_cursor + 12), 5, (primary_glow_color[0], primary_glow_color[1], primary_glow_color[2], 255))
            draw.text((102, y_cursor), feat, font=bullet_font, fill=(220, 230, 245, 255))
            y_cursor += 36

    # Bottom brand watermark
    brand_font = get_font(15, bold=True)
    draw.text((75, card_h - 65), "LEVYRA · NATIVE MUSIC EXPERIENCE", font=brand_font, fill=(80, 92, 115, 200))

    # 3. Right Side: Dual Floating Phone Mockups
    img1_src = Image.open(get_screen_path(SCREENS[screen1_key]))
    img2_src = Image.open(get_screen_path(SCREENS[screen2_key]))

    phone1 = create_phone_frame(img1_src, target_height=780)
    phone2 = create_phone_frame(img2_src, target_height=840)

    # Phone 1 (Back / Left phone)
    p1_x = 600
    p1_y = 85
    shadow1 = create_studio_shadow(phone1, blur_radius=40, opacity=150, offset=(0, 20))
    canvas.paste(shadow1, (p1_x - 40, p1_y - 20), shadow1)
    canvas.paste(phone1, (p1_x, p1_y), phone1)

    # Phone 2 (Front / Right phone)
    p2_x = 950
    p2_y = 55
    shadow2 = create_studio_shadow(phone2, blur_radius=50, opacity=190, offset=(0, 30))
    canvas.paste(shadow2, (p2_x - 50, p2_y - 20), shadow2)
    canvas.paste(phone2, (p2_x, p2_y), phone2)

    out_path = os.path.join(OUT_SHOWCASE_DIR, f"{card_id}.webp")
    canvas.convert("RGB").save(out_path, "WEBP", quality=94, method=6)
    print(f"Generated Showcase Card: {out_path}")

def generate_hero_panoramic_showcase():
    canvas_w, canvas_h = 2400, 1240
    canvas = Image.new("RGBA", (canvas_w, canvas_h), (5, 7, 11, 255))

    canvas = Image.alpha_composite(
        canvas,
        create_ambient_glow(canvas_w, canvas_h, (520, 500), 780, (196, 54, 118), max_alpha=72),
    )
    canvas = Image.alpha_composite(
        canvas,
        create_ambient_glow(canvas_w, canvas_h, (1200, 550), 900, (37, 116, 255), max_alpha=90),
    )
    canvas = Image.alpha_composite(
        canvas,
        create_ambient_glow(canvas_w, canvas_h, (1980, 520), 760, (105, 52, 210), max_alpha=75),
    )

    keys = ["home", "charts", "now_playing", "lyrics", "artist_discography"]
    heights = [900, 1010, 1160, 1010, 900]
    positions = [(-40, 245), (390, 135), (925, 25), (1480, 135), (2020, 245)]
    opacities = [135, 165, 220, 165, 135]

    for key, height, (x, y), opacity in zip(keys, heights, positions, opacities):
        with Image.open(get_screen_path(SCREENS[key])) as source:
            phone = create_phone_frame(source, target_height=height)
        shadow = create_studio_shadow(phone, blur_radius=55, opacity=opacity, offset=(0, 30))
        canvas.paste(shadow, (x - 55, y - 25), shadow)
        canvas.paste(phone, (x, y), phone)

    fade = Image.new("RGBA", (canvas_w, 260), (0, 0, 0, 0))
    fade_alpha = Image.new("L", (1, 260))
    fade_alpha.putdata([int(190 * (y / 259) ** 1.8) for y in range(260)])
    fade.putalpha(fade_alpha.resize((canvas_w, 260)))
    canvas.alpha_composite(fade, (0, canvas_h - 260))

    out_path = os.path.join(OUT_SHOWCASE_DIR, "00_levyra_hero_showcase.webp")
    canvas.convert("RGB").save(out_path, "WEBP", quality=94, method=6)
    print(f"Generated Panoramic Hero Showcase: {out_path}")

def main():
    print("Generating refined Levyra showcase assets...")
    generate_individual_framed_screenshots()
    generate_hero_panoramic_showcase()
    print("Showcase generation completed successfully!")

if __name__ == "__main__":
    main()
