import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageEnhance

SCREENSHOT_DIR = os.environ.get("LEVYRA_SCREEN_DIR", ".capture")
OUT_SHOWCASE_DIR = r"docs\assets\showcase"
OUT_CARDS_DIR = os.path.join(OUT_SHOWCASE_DIR, "cards")
LOGO_PATH = r"app\src\main\res\drawable\levyra_logo.png"

os.makedirs(OUT_SHOWCASE_DIR, exist_ok=True)
os.makedirs(OUT_CARDS_DIR, exist_ok=True)

SCREENS = {
    "home": "Screenshot_20260926_171253_LEVYRA.jpg",
    "charts": "charts.png",
    "genres": "genres.png",
    "listening_pulse": "pulse.png",
    "lyrics": "lyrics.png",
    "now_playing": "player.png",
    "search_artist": "search-artist.png",
    "artist_discography": "discography2.png",
    "artist_profile": "artist-profile.png",
    "album": "album.png",
    "search": "search.png",
    "player_settings": "player-settings.png",
    "explore": "explore.png",
    "new_releases": "new-releases.png",
    "collections": "library.png",
    "listening_rhythm": "rhythm.png",
}

def get_screen_path(filename):
    return filename if os.path.isabs(filename) else os.path.join(SCREENSHOT_DIR, filename)

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

def create_gallery_screen(screen_img, target_height=1600):
    screen_img = enhance_screenshot(screen_img)
    aspect = screen_img.width / screen_img.height
    target_width = int(target_height * aspect)
    screen = screen_img.resize((target_width, target_height), Image.Resampling.LANCZOS).convert("RGBA")
    radius = int(target_width * 0.055)
    mask = Image.new("L", screen.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, target_width - 1, target_height - 1],
        radius=radius,
        fill=255,
    )
    screen.putalpha(mask)
    border = Image.new("RGBA", screen.size, (0, 0, 0, 0))
    ImageDraw.Draw(border).rounded_rectangle(
        [1, 1, target_width - 2, target_height - 2],
        radius=radius,
        outline=(70, 78, 96, 180),
        width=3,
    )
    return Image.alpha_composite(screen, border)

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
    canvas_w, canvas_h = 2400, 1160
    canvas = Image.new("RGBA", (canvas_w, canvas_h), (7, 8, 13, 255))
    draw = ImageDraw.Draw(canvas)

    draw.polygon(
        [(720, 0), (2400, 0), (2400, 370), (980, 610)],
        fill=(21, 35, 72, 255),
    )
    draw.polygon(
        [(1080, 1160), (2400, 820), (2400, 1160)],
        fill=(126, 29, 143, 255),
    )
    draw.polygon(
        [(1730, 0), (1960, 0), (1420, 1160), (1190, 1160)],
        fill=(216, 31, 174, 80),
    )
    draw = ImageDraw.Draw(canvas)

    with Image.open(LOGO_PATH) as logo_source:
        logo = logo_source.convert("RGBA")
        logo.thumbnail((410, 410), Image.Resampling.LANCZOS)
    canvas.paste(logo, (118, 72), logo)

    draw.text((110, 470), "LEVYRA", font=get_font(112, bold=True), fill=(255, 255, 255, 255))
    draw.text((118, 605), "MUSIC, KEPT PERSONAL.", font=get_font(32, bold=True), fill=(98, 208, 255, 255))
    draw.multiline_text(
        (118, 675),
        "Native playback on Android and Windows.\nNo ads. No telemetry. Your library stays yours.",
        font=get_font(25),
        fill=(205, 209, 221, 255),
        spacing=12,
    )
    draw.line((118, 815, 560, 815), fill=(255, 255, 255, 85), width=3)
    draw.text((118, 845), "PLAY  /  EXPLORE  /  KEEP", font=get_font(20, bold=True), fill=(255, 255, 255, 185))
    draw.text((118, 1035), "ANDROID + WINDOWS", font=get_font(18, bold=True), fill=(255, 255, 255, 130))

    back_row = [
        ("search_artist", 500, 660, 120, -6),
        ("artist_discography", 520, 1020, 70, -3),
        ("charts", 520, 1740, 70, 4),
        ("genres", 500, 2010, 130, 6),
    ]
    front_row = [
        ("home", 700, 900, 330, -4),
        ("now_playing", 850, 1320, 180, 0),
        ("lyrics", 700, 1800, 330, 4),
    ]
    for key, height, x, y, angle in back_row + front_row:
        with Image.open(get_screen_path(SCREENS[key])) as source:
            phone = create_phone_frame(source, target_height=height)
        if angle:
            phone = phone.rotate(angle, resample=Image.Resampling.BICUBIC, expand=True)
        shadow = create_studio_shadow(phone, blur_radius=42, opacity=190, offset=(0, 26))
        canvas.paste(shadow, (x - 42, y - 16), shadow)
        canvas.paste(phone, (x, y), phone)

    out_path = os.path.join(OUT_SHOWCASE_DIR, "00_levyra_hero_wall_player.webp")
    canvas.convert("RGB").save(out_path, "WEBP", quality=94, method=6)
    print(f"Generated Panoramic Hero Showcase: {out_path}")

def generate_feature_cards():
    card_w, card_h = 900, 1600
    specs = [
        ("home", "01", "Your music, up front", "Radio, mood shortcuts,\nand Your Orbit.", (235, 215, 220), (199, 143, 158)),
        ("now_playing", "02", "Stay with the song", "Song and video share\none focused player.", (204, 222, 236), (139, 181, 211)),
        ("lyrics", "03", "Follow every line", "Synced lyrics move\nwith the music.", (235, 220, 193), (195, 158, 104)),
        ("charts", "04", "See what is playing", "Browse Top 50 charts\nacross countries.", (205, 220, 241), (121, 166, 217)),
        ("search_artist", "05", "Find the artist", "Search songs, albums,\nplaylists, and artists.", (222, 213, 235), (164, 138, 199)),
        ("artist_discography", "06", "Go deeper", "Popular tracks, albums,\nsingles, and EPs.", (235, 215, 208), (198, 139, 124)),
        ("genres", "07", "Pick a direction", "Move through moods\nand genres quickly.", (207, 227, 220), (128, 182, 168)),
        ("listening_pulse", "08", "Keep it personal", "Private listening stats,\ncomputed on this device.", (212, 216, 237), (139, 149, 200)),
        ("artist_profile", "09", "Meet the artist", "Biography, audience,\nand popular tracks.", (228, 225, 204), (174, 163, 96)),
        ("album", "10", "Open the whole album", "Artwork, context,\nand a complete tracklist.", (238, 221, 203), (199, 151, 106)),
        ("search", "11", "Find it fast", "Recent music and artists,\nready when you return.", (205, 228, 231), (105, 177, 181)),
        ("player_settings", "12", "Choose your stage", "Immersive Canvas, framed\nartwork, or a calm view.", (232, 211, 226), (190, 127, 169)),
        ("explore", "13", "Explore and mix", "Live stations, custom mixes,\nand fresh currents.", (205, 225, 245), (68, 138, 245)),
        ("new_releases", "14", "Fresh off the stage", "New singles and albums\nupdated every week.", (245, 215, 210), (225, 115, 95)),
        ("collections", "15", "Everything together", "Favorites, offline music,\nhistory, and playlists.", (226, 212, 244), (152, 95, 215)),
        ("listening_rhythm", "16", "Your listening rhythm", "Activity, peak hours,\nand listening habits.", (210, 238, 225), (72, 180, 135)),
    ]

    label_font = get_font(19, bold=True)
    title_font = get_font(52, bold=True)
    subtitle_font = get_font(27, bold=False)

    for key, number, title, subtitle, background, accent in specs:
        card = Image.new("RGBA", (card_w, card_h), (*background, 255))
        draw = ImageDraw.Draw(card)
        draw.polygon(
            [(0, 950), (card_w, 650), (card_w, card_h), (0, card_h)],
            fill=(*accent, 255),
        )
        draw.rounded_rectangle((64, 58, 254, 104), radius=23, fill=(20, 23, 29, 235))
        draw.text((159, 81), f"LEVYRA / {number}", font=label_font, fill=(255, 255, 255, 255), anchor="mm")
        draw.text((64, 132), title, font=title_font, fill=(20, 23, 29, 255))
        draw.multiline_text(
            (64, 205),
            subtitle,
            font=subtitle_font,
            fill=(66, 73, 83, 255),
            spacing=8,
        )

        with Image.open(get_screen_path(SCREENS[key])) as source:
            phone = create_phone_frame(source, target_height=1050)
        phone_x = (card_w - phone.width) // 2
        phone_y = 390
        shadow = create_studio_shadow(phone, blur_radius=42, opacity=175, offset=(0, 28))
        card.paste(shadow, (phone_x - 42, phone_y - 16), shadow)
        card.paste(phone, (phone_x, phone_y), phone)

        output_name = {
            "now_playing": "02_stay_with_the_song.webp",
            "listening_pulse": "08_keep_it_personal.webp",
        }.get(key, f"{number}_{key}.webp")
        out_path = os.path.join(OUT_CARDS_DIR, output_name)
        card.convert("RGB").save(out_path, "WEBP", quality=91, method=6)
        print(f"Generated Feature Card: {out_path}")

def main():
    print("Generating refined Levyra showcase assets...")
    generate_hero_panoramic_showcase()
    generate_feature_cards()
    print("Showcase generation completed successfully!")

if __name__ == "__main__":
    main()
