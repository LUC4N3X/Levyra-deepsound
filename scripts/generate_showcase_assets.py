import os
import math
import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageEnhance

SCREENSHOT_DIR = r"C:\Users\Luca Drogo\Desktop\screenshot"
OUT_SHOWCASE_DIR = r"docs\assets\showcase"
OUT_SCREENSHOTS_DIR = r"docs\assets\screenshots"

os.makedirs(OUT_SHOWCASE_DIR, exist_ok=True)
os.makedirs(OUT_SCREENSHOTS_DIR, exist_ok=True)

SCREENS = {
    "home_orbit": "Screenshot_20260816_234335_LEVYRA.jpg",
    "home_top50": "Screenshot_20260816_234345_LEVYRA.jpg",
    "explore_samples": "Screenshot_20260816_234411_LEVYRA.jpg",
    "explore_genres": "Screenshot_20260816_234414_LEVYRA.jpg",
    "library_quickpicks": "Screenshot_20260816_234419_LEVYRA.jpg",
    "library_pulse": "Screenshot_20260816_234421_LEVYRA.jpg",
    "player_nowplaying": "Screenshot_20260816_234503_LEVYRA.jpg",
    "search_artist_avatars": "Screenshot_20260816_234530_LEVYRA.jpg",
    "lyrics_synced": "Screenshot_20260816_234701_LEVYRA.jpg",
    "artist_bio": "Screenshot_20260816_234724_LEVYRA.jpg",
    "artist_discography": "Screenshot_20260816_234730_LEVYRA.jpg",
    "video_energy": "Screenshot_20260817_145820_LEVYRA.jpg",
    "search_recent": "Screenshot_20260817_131545_LEVYRA.jpg",
    "playlist_recent": "Screenshot_20260817_131607_LEVYRA.jpg",
    "home_collections": "Screenshot_20260817_131624_LEVYRA.jpg",
}

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
        src_path = os.path.join(SCREENSHOT_DIR, filename)
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
    img1_src = Image.open(os.path.join(SCREENSHOT_DIR, SCREENS[screen1_key]))
    img2_src = Image.open(os.path.join(SCREENSHOT_DIR, SCREENS[screen2_key]))

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
    """
    Renders an expansive, ultra-wide 2400x1100 panoramic banner featuring 5 staggered
    devices with deep studio lighting and atmospheric glow.
    """
    canvas_w, canvas_h = 2400, 1100
    canvas = Image.new("RGBA", (canvas_w, canvas_h), (8, 10, 14, 255))

    # Studio lighting
    glow1 = create_ambient_glow(canvas_w, canvas_h, (1200, 500), 750, (130, 80, 255), max_alpha=85) # Purple center
    glow2 = create_ambient_glow(canvas_w, canvas_h, (1800, 480), 650, (230, 60, 90), max_alpha=70) # Crimson right
    glow3 = create_ambient_glow(canvas_w, canvas_h, (600, 520), 650, (40, 130, 255), max_alpha=75) # Blue left
    glow4 = create_ambient_glow(canvas_w, canvas_h, (1200, 1000), 800, (30, 190, 210), max_alpha=40) # Cyan bottom

    canvas = Image.alpha_composite(canvas, glow1)
    canvas = Image.alpha_composite(canvas, glow2)
    canvas = Image.alpha_composite(canvas, glow3)
    canvas = Image.alpha_composite(canvas, glow4)

    # 5 Key Screens: Library Pulse, Home Orbit, Now Playing, Synced Lyrics, Artist Bio
    keys = ["library_pulse", "home_orbit", "player_nowplaying", "lyrics_synced", "artist_bio"]
    imgs = [Image.open(os.path.join(SCREENSHOT_DIR, SCREENS[k])) for k in keys]

    p1 = create_phone_frame(imgs[0], target_height=740) # Pulse
    p2 = create_phone_frame(imgs[1], target_height=820) # Home
    p3 = create_phone_frame(imgs[2], target_height=920) # Player (Centerpiece)
    p4 = create_phone_frame(imgs[3], target_height=820) # Lyrics
    p5 = create_phone_frame(imgs[4], target_height=740) # Artist

    # Staggered 5-device layout
    # 1. Far Left (Pulse)
    x1, y1 = 120, 200
    sh1 = create_studio_shadow(p1, blur_radius=35, opacity=130)
    canvas.paste(sh1, (x1 - 35, y1 - 20), sh1)
    canvas.paste(p1, (x1, y1), p1)

    # 5. Far Right (Artist)
    x5, y5 = 1860, 200
    sh5 = create_studio_shadow(p5, blur_radius=35, opacity=130)
    canvas.paste(sh5, (x5 - 35, y5 - 20), sh5)
    canvas.paste(p5, (x5, y5), p5)

    # 2. Mid Left (Home)
    x2, y2 = 510, 130
    sh2 = create_studio_shadow(p2, blur_radius=45, opacity=160)
    canvas.paste(sh2, (x2 - 45, y2 - 20), sh2)
    canvas.paste(p2, (x2, y2), p2)

    # 4. Mid Right (Lyrics)
    x4, y4 = 1470, 130
    sh4 = create_studio_shadow(p4, blur_radius=45, opacity=160)
    canvas.paste(sh4, (x4 - 45, y4 - 20), sh4)
    canvas.paste(p4, (x4, y4), p4)

    # 3. Center Hero (Player)
    x3, y3 = 980, 65
    sh3 = create_studio_shadow(p3, blur_radius=60, opacity=210)
    canvas.paste(sh3, (x3 - 60, y3 - 20), sh3)
    canvas.paste(p3, (x3, y3), p3)

    # Top overlay header banner
    draw = ImageDraw.Draw(canvas)
    header_font = get_font(38, bold=True)
    sub_font = get_font(18, bold=False)

    # Center text with vector sparkle stars
    title_text = "LEVYRA EXPERIENCE"
    bbox = header_font.getbbox(title_text)
    t_w = bbox[2] - bbox[0]

    cx = canvas_w // 2
    draw.text((cx, 40), title_text, font=header_font, fill=(255, 255, 255, 245), anchor="mt")
    # Draw sparkles on left and right of title
    draw_vector_sparkle(draw, (cx - t_w // 2 - 26, 62), 10, (210, 225, 255, 255))
    draw_vector_sparkle(draw, (cx + t_w // 2 + 26, 62), 10, (210, 225, 255, 255))

    draw.text((cx, 90), "Native Media3 Audio Engine · Live Synced Lyrics · Private Listening Pulse · Real M4A Offline Vault", font=sub_font, fill=(170, 185, 210, 220), anchor="mt")

    out_path = os.path.join(OUT_SHOWCASE_DIR, "00_levyra_hero_showcase.webp")
    canvas.convert("RGB").save(out_path, "WEBP", quality=94, method=6)
    print(f"Generated Panoramic Hero Showcase: {out_path}")

def main():
    print("Generating refined Levyra showcase assets...")
    generate_individual_framed_screenshots()
    generate_hero_panoramic_showcase()

    # Showcase 1: Now Playing & Live Synced Lyrics
    generate_studio_dual_card(
        card_id="01_playback_and_lyrics",
        title_category="Acoustic Engine & Lyrics",
        title_main="Immersive Playback &\nLive Synced Lyrics",
        subtitle="Full-bleed visual canvas with high-res art,\nwaveform scrubber, and real-time LRCLIB karaoke sync.",
        screen1_key="player_nowplaying",
        screen2_key="lyrics_synced",
        primary_glow_color=(235, 75, 90),
        secondary_glow_color=(160, 70, 255),
        features_list=[
            "ExoPlayer & Media3 low-latency audio engine",
            "Real-time synchronized line-by-line lyrics",
            "Interactive tap-to-seek lyric scrubbing",
            "Song & Native Video seamless toggle",
            "Automatic SponsorBlock & silence skipping"
        ]
    )

    # Showcase 2: Home Discovery & Global Top 50
    generate_studio_dual_card(
        card_id="02_home_and_charts",
        title_category="Discovery & Exploration",
        title_main="Smart Orbit Feed &\nGlobal Top Charts",
        subtitle="Dynamic discovery tailored to your rhythm,\nwith live Top 50 charts across Italy, USA, UK, and Spain.",
        screen1_key="home_orbit",
        screen2_key="home_top50",
        primary_glow_color=(60, 120, 255),
        secondary_glow_color=(120, 80, 240),
        features_list=[
            "Personalized 'Your Orbit' dynamic rotation",
            "Live international Top 50 chart selectors",
            "Curated mood & activity quick chips",
            "Dual InnerTube & LevyraExtractor pipelines",
            "Instant prefetching for zero-latency skips"
        ]
    )

    # Showcase 3: Deep Search & Artist Immersion
    generate_studio_dual_card(
        card_id="03_search_and_artist",
        title_category="Artist Universe",
        title_main="Deep Search &\nArtist Discography",
        subtitle="Instant search suggestions, live artist matching,\nWikipedia biography cards, and full discographies.",
        screen1_key="search_artist_avatars",
        screen2_key="artist_bio",
        primary_glow_color=(240, 160, 50),
        secondary_glow_color=(210, 60, 120),
        features_list=[
            "Real-time search with instant artist bubbles",
            "Wikipedia biography & monthly audience stats",
            "Complete chronological albums, singles & EPs",
            "One-tap follow & artist radio queues",
            "Voice search & live waveform visualizer"
        ]
    )

    # Showcase 4: Offline Vault & Listening Pulse
    generate_studio_dual_card(
        card_id="04_library_and_pulse",
        title_category="Private Vault & Analytics",
        title_main="Offline M4A Vault &\nListening Pulse",
        subtitle="Real tagged audio files saved to storage, plus\na private on-device dashboard of your listening habits.",
        screen1_key="playlist_recent",
        screen2_key="library_pulse",
        primary_glow_color=(70, 180, 255),
        secondary_glow_color=(80, 100, 240),
        features_list=[
            "Standard M4A files with embedded tags & art",
            "Local-first playback: zero network data wasted",
            "Private 7-day listening activity & streaks",
            "Real minutes-played ranking (no accidental taps)",
            "100% on-device SQLite: zero tracking & telemetry"
        ]
    )

    # Showcase 5: Video Mode, Samples & Energy Pulse
    generate_studio_dual_card(
        card_id="05_video_and_samples",
        title_category="Visuals & Resonance",
        title_main="Samples Clips &\nComment Energy",
        subtitle="Explore short vertical video clips, music videos,\nand real-time audience resonance metrics.",
        screen1_key="explore_samples",
        screen2_key="video_energy",
        primary_glow_color=(40, 210, 150),
        secondary_glow_color=(255, 100, 80),
        features_list=[
            "Vertical 'Samples' teaser clips for fast discovery",
            "Live video feeds with inline preview",
            "Voci che risuonano: 73% energy metric",
            "One-tap full video playback mode",
            "High-resolution video stream extraction"
        ]
    )

    # Showcase 6: Editorial Collections & Genre Matrix
    generate_studio_dual_card(
        card_id="06_collections_and_genres",
        title_category="Curation & Moods",
        title_main="Editorial Playlists &\nGenre Matrix",
        subtitle="Handcrafted 4-tile artwork collections and\nan expansive spectrum of musical moods & subgenres.",
        screen1_key="home_collections",
        screen2_key="explore_genres",
        primary_glow_color=(180, 70, 240),
        secondary_glow_color=(240, 90, 160),
        features_list=[
            "4-tile dynamic editorial collection cards",
            "Mood matrix: Rap, Lo-Fi, Electronic, J-Pop & more",
            "Curated 'After hours' & 'Quick picks' mixes",
            "Artist ring avatars with neon glow accents",
            "One-tap playlist generation & shuffle"
        ]
    )

    print("Showcase generation completed successfully!")

if __name__ == "__main__":
    main()
