import base64
import io
import math
import os
import shutil
from PIL import Image, ImageDraw, ImageEnhance, ImageFilter, ImageFont
import numpy as np

WIDTH = 2880
HEIGHT = 1080

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS_DIR = os.path.join(REPO_ROOT, "docs", "assets")

def resolve_input(preferred_path, fallback_rel_path):
    """Resolves preferred local input path with graceful fallback to tracked repository asset."""
    if preferred_path and os.path.exists(preferred_path):
        return preferred_path
    repo_fallback = os.path.join(REPO_ROOT, fallback_rel_path)
    if os.path.exists(repo_fallback):
        return repo_fallback
    return preferred_path

LOGO_SRC_PATH = resolve_input(
    os.environ.get("LEVYRA_LOGO_PATH", r"C:/Users/Luca Drogo/.gemini/antigravity/brain/56548c8c-b7bc-4dce-809e-6e9d455ced20/.user_uploaded/media_1790506287135.png"),
    os.path.join("app", "src", "main", "res", "drawable", "levyra_logo.png")
)
SCREEN_HOME_PATH = resolve_input(
    os.environ.get("LEVYRA_SCREEN_HOME", r"C:\Users\Luca Drogo\Desktop\screenshots\Screenshot_20260926_171253_LEVYRA.jpg"),
    os.path.join("docs", "assets", "showcase", "cards", "01_home.webp")
)
SCREEN_PLAYER_PATH = resolve_input(
    os.environ.get("LEVYRA_SCREEN_PLAYER", r"C:\Users\Luca Drogo\Desktop\screenshots\Screenshot_20260926_193948_LEVYRA.jpg"),
    os.path.join("docs", "assets", "showcase", "cards", "03_lyrics.webp")
)

def find_preferred_dark_bg():
    paths = [
        os.environ.get("LEVYRA_BG_DARK", ""),
        r"C:\Users\Luca Drogo\.gemini\antigravity\brain\56548c8c-b7bc-4dce-809e-6e9d455ced20\musical_concert_stage_bg_1790508925612.jpg",
        r"C:\Users\Luca Drogo\.gemini\antigravity\brain\56548c8c-b7bc-4dce-809e-6e9d455ced20\minimal_concert_stage_1790508945319.jpg",
    ]
    for p in paths:
        if p and os.path.exists(p):
            return p
    return ""

BG_DARK_CONCERT = find_preferred_dark_bg()
BG_LIGHT_CONCERT = os.environ.get("LEVYRA_BG_LIGHT", r"C:\Users\Luca Drogo\.gemini\antigravity\brain\56548c8c-b7bc-4dce-809e-6e9d455ced20\light_concert_stage_1790508968511.jpg")

OUTPUT_DARK_PATH = os.path.join(ASSETS_DIR, "levyra-github-banner.webp")
OUTPUT_LIGHT_PATH = os.path.join(ASSETS_DIR, "levyra-github-banner-light.webp")

DESKTOP_DIR = os.environ.get("LEVYRA_DESKTOP_DIR", os.path.join(os.path.expanduser("~"), "Desktop"))

# Official GitHub Octocat vector assets (256x256 rendered directly from GitHub official SVG path)
GITHUB_WHITE_B64 = (
    "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"
)

GITHUB_DARK_B64 = (
    "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"
)

def get_official_github_mark(light_mode=False, target_size=60):
    """Returns the official GitHub Octocat brand mark as an RGBA image."""
    b64_str = GITHUB_DARK_B64 if light_mode else GITHUB_WHITE_B64
    raw = base64.b64decode(b64_str)
    img = Image.open(io.BytesIO(raw)).convert('RGBA')
    return img.resize((target_size, target_size), Image.Resampling.LANCZOS)

def get_font(size, bold=False, light=False):
    """Loads a TrueType font across Windows/Linux/macOS or falls back safely to default font."""
    candidates = []
    if light:
        candidates = [
            r"C:\Windows\Fonts\segoeuil.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/System/Library/Fonts/SFCompactText.ttf"
        ]
    elif bold:
        candidates = [
            r"C:\Windows\Fonts\segoeuib.ttf",
            r"C:\Windows\Fonts\arialbd.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
            "/System/Library/Fonts/SFCompactText-Bold.ttf"
        ]
    else:
        candidates = [
            r"C:\Windows\Fonts\segoeui.ttf",
            r"C:\Windows\Fonts\arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/System/Library/Fonts/SFCompactText.ttf"
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

def draw_vector_diamond(draw, center, radius, color):
    """Draws an 8-point vector diamond sparkle icon."""
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

def create_ultra_flagship_phone(screenshot_path, target_height=840, light_mode=False):
    """Renders a flagship phone mockup with speaker, camera notch, specular reflection and rounded glass."""
    screen_src = Image.open(screenshot_path).convert('RGBA')
    screen_src = ImageEnhance.Contrast(screen_src).enhance(1.04)
    screen_src = ImageEnhance.Color(screen_src).enhance(1.05)
    screen_src = ImageEnhance.Sharpness(screen_src).enhance(1.08)

    SS = 2
    orig_w, orig_h = screen_src.size
    aspect = orig_w / float(orig_h)

    screen_h = int(target_height * SS)
    screen_w = int(screen_h * aspect)

    bezel = int(11 * SS)
    corner_r = int(42 * SS)
    screen_r = int(33 * SS)

    phone_w = screen_w + bezel * 2
    phone_h = screen_h + bezel * 2

    phone = Image.new('RGBA', (phone_w, phone_h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(phone)

    if light_mode:
        frame_fill = (222, 228, 238, 255)
        frame_outline = (175, 185, 202, 255)
        specular_col = (255, 255, 255, 180)
        inner_border = (120, 130, 145, 255)
        cam_fill = (15, 18, 24, 240)
        spk_fill = (140, 150, 165, 255)
    else:
        frame_fill = (18, 21, 28, 255)
        frame_outline = (52, 60, 78, 255)
        specular_col = (190, 215, 245, 85)
        inner_border = (10, 12, 16, 255)
        cam_fill = (8, 10, 14, 240)
        spk_fill = (35, 40, 50, 255)

    draw.rounded_rectangle([0, 0, phone_w - 1, phone_h - 1], radius=corner_r, fill=frame_fill, outline=frame_outline, width=int(2 * SS))

    specular = Image.new('RGBA', (phone_w, phone_h), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(specular)
    sdraw.rounded_rectangle([1, 1, phone_w - 2, phone_h - 2], radius=corner_r - 1, outline=specular_col, width=int(1.5 * SS))
    phone = Image.alpha_composite(phone, specular)

    draw.rounded_rectangle([bezel - 1, bezel - 1, phone_w - bezel, phone_h - bezel], radius=screen_r + 1, outline=inner_border, width=int(1 * SS))

    screen_resized = screen_src.resize((screen_w, screen_h), Image.Resampling.LANCZOS)

    screen_mask = Image.new('L', (screen_w, screen_h), 0)
    ImageDraw.Draw(screen_mask).rounded_rectangle([0, 0, screen_w - 1, screen_h - 1], radius=screen_r, fill=255)

    inner_screen = Image.new('RGBA', (screen_w, screen_h), (0, 0, 0, 0))
    inner_screen.paste(screen_resized, (0, 0), screen_mask)

    cam_r = int(6.5 * SS)
    cam_x = screen_w // 2
    cam_y = int(12 * SS)
    ImageDraw.Draw(inner_screen).ellipse([cam_x - cam_r, cam_y - cam_r, cam_x + cam_r, cam_y + cam_r], fill=cam_fill, outline=(30, 35, 45, 180), width=int(1 * SS))
    ImageDraw.Draw(inner_screen).ellipse([cam_x - int(2*SS), cam_y - int(2*SS), cam_x, cam_y], fill=(70, 100, 150, 180))

    glass = Image.new('RGBA', (screen_w, screen_h), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glass)
    gdraw.polygon([(0, 0), (int(screen_w * 0.65), 0), (0, int(screen_h * 0.42))], fill=(255, 255, 255, 14 if not light_mode else 20))
    inner_screen = Image.alpha_composite(inner_screen, glass)

    phone.paste(inner_screen, (bezel, bezel), screen_mask)

    spk_w = int(42 * SS)
    spk_h = int(2.5 * SS)
    spk_x = (phone_w - spk_w) // 2
    spk_y = int(4.5 * SS)
    draw.rounded_rectangle([spk_x, spk_y, spk_x + spk_w, spk_y + spk_h], radius=int(1.5 * SS), fill=spk_fill)

    return phone.resize((phone_w // SS, phone_h // SS), Image.Resampling.LANCZOS)

def add_floor_reflection(canvas, element_img, x, base_y, max_alpha=75, fade_height=210, blur_val=6):
    """Adds a studio reflection of an element on the floor plane."""
    w, h = element_img.size
    flipped = element_img.transpose(Image.Transpose.FLIP_TOP_BOTTOM)

    refl_h = min(h, fade_height)
    flipped_crop = flipped.crop((0, 0, w, refl_h))

    mask_arr = np.zeros((refl_h, w), dtype=np.uint8)
    for y in range(refl_h):
        fade = (1.0 - (y / float(refl_h))) ** 1.7
        alpha = int(max_alpha * fade)
        mask_arr[y, :] = alpha

    mask = Image.fromarray(mask_arr, "L")

    r, g, b, orig_a = flipped_crop.split()
    combined_a_arr = (np.array(orig_a) * (np.array(mask) / 255.0)).astype(np.uint8)
    combined_a = Image.fromarray(combined_a_arr, "L")

    flipped_crop.putalpha(combined_a)
    blurred_refl = flipped_crop.filter(ImageFilter.GaussianBlur(blur_val))
    canvas.paste(blurred_refl, (x, base_y), blurred_refl)

def add_studio_shadow(canvas, element_img, x, y, blur_radius=36, opacity=170, offset=(0, 25), shadow_color=(0, 0, 0)):
    """Adds a soft blurred studio shadow behind an element."""
    w, h = element_img.size
    shadow = Image.new("RGBA", (w + blur_radius * 2, h + blur_radius * 2), (0, 0, 0, 0))
    alpha = element_img.split()[3]
    shadow_mask = Image.new("RGBA", (w, h), (*shadow_color, opacity))
    shadow_mask.putalpha(alpha)
    shadow.paste(shadow_mask, (blur_radius, blur_radius), shadow_mask)
    shadow = shadow.filter(ImageFilter.GaussianBlur(blur_radius))
    canvas.paste(shadow, (x - blur_radius + offset[0], y - blur_radius + offset[1]), shadow)

def create_glass_platform_pill(light_mode=False):
    """Renders a sleek frosted glass pill container with Android and Windows platform icons."""
    ss = 3
    target_w, target_h = 164, 58
    pw, ph = target_w * ss, target_h * ss
    pill = Image.new('RGBA', (pw, ph), (0, 0, 0, 0))
    pdraw = ImageDraw.Draw(pill)

    fill_col = (255, 255, 255, 22) if not light_mode else (255, 255, 255, 215)
    border_col = (255, 255, 255, 80) if not light_mode else (175, 190, 215, 220)
    pdraw.rounded_rectangle([0, 0, pw - 1, ph - 1], radius=ph // 2, fill=fill_col, outline=border_col, width=int(1.8 * ss))

    ico_col = (245, 248, 255, 255) if not light_mode else (20, 28, 45, 255)
    div_col = (255, 255, 255, 70) if not light_mode else (160, 175, 200, 190)
    eye_col = (8, 12, 18, 255) if not light_mode else (255, 255, 255, 255)

    div_x = (target_w // 2) * ss
    pdraw.line([(div_x, 14 * ss), (div_x, (target_h - 14) * ss)], fill=div_col, width=int(1.5 * ss))

    # Left: Android robot icon
    cx_a = (target_w // 4) * ss
    cy_a = (target_h // 2) * ss
    head_w = 26 * ss
    ax = cx_a - head_w // 2
    ay = cy_a - 4 * ss

    pdraw.pieslice([ax, ay, ax + head_w, ay + head_w], 180, 360, fill=ico_col)
    eye_r = int(1.8 * ss)
    pdraw.ellipse([cx_a - 6 * ss - eye_r, ay + 6 * ss - eye_r, cx_a - 6 * ss + eye_r, ay + 6 * ss + eye_r], fill=eye_col)
    pdraw.ellipse([cx_a + 6 * ss - eye_r, ay + 6 * ss - eye_r, cx_a + 6 * ss + eye_r, ay + 6 * ss + eye_r], fill=eye_col)
    ant_w = int(2.2 * ss)
    pdraw.line([(cx_a - 8 * ss, ay - 6 * ss), (cx_a - 5 * ss, ay)], fill=ico_col, width=ant_w)
    pdraw.line([(cx_a + 8 * ss, ay - 6 * ss), (cx_a + 5 * ss, ay)], fill=ico_col, width=ant_w)

    # Right: Windows icon
    cx_w = (target_w * 3 // 4) * ss
    cy_w = (target_h // 2) * ss
    ww = 24 * ss
    gap = int(2.5 * ss)
    pw_box = (ww - gap) // 2
    wx = cx_w - ww // 2
    wy = cy_w - ww // 2

    pdraw.rounded_rectangle([wx, wy, wx + pw_box, wy + pw_box], radius=int(1 * ss), fill=ico_col)
    pdraw.rounded_rectangle([wx + pw_box + gap, wy, wx + ww, wy + pw_box], radius=int(1 * ss), fill=ico_col)
    pdraw.rounded_rectangle([wx, wy + pw_box + gap, wx + pw_box, wy + ww], radius=int(1 * ss), fill=ico_col)
    pdraw.rounded_rectangle([wx + pw_box + gap, wy + pw_box + gap, wx + ww, wy + ww], radius=int(1 * ss), fill=ico_col)

    return pill.resize((target_w, target_h), Image.Resampling.LANCZOS)

def generate_dark_unified_banner():
    """Generates the dark mode unified banner with Tidal-style concert stage backdrop."""
    canvas = Image.new("RGBA", (WIDTH, HEIGHT), (7, 9, 14, 255))

    # 1. Tidal-Graded Concert Stage & Crowd
    if os.path.exists(BG_DARK_CONCERT):
        bg_src = Image.open(BG_DARK_CONCERT).convert("RGB")
        arr = np.array(bg_src, dtype=np.float32)

        r = arr[:, :, 0]
        g = arr[:, :, 1]
        b = arr[:, :, 2]
        lum = 0.299 * r + 0.587 * g + 0.114 * b

        # Tidal grading: deep blacks, electric cobalt/cyan midtones, moody indigo
        tidal_arr = np.zeros_like(arr)
        tidal_arr[:, :, 0] = lum * 0.16 + (r * 0.04)
        tidal_arr[:, :, 1] = lum * 0.52
        tidal_arr[:, :, 2] = lum * 0.92 + (b * 0.28)
        tidal_arr = np.clip(tidal_arr, 0, 255).astype(np.uint8)

        stage_img = Image.fromarray(tidal_arr).convert("RGBA")
        new_w = WIDTH
        new_h = int(stage_img.height * (WIDTH / float(stage_img.width)))
        stage_resized = stage_img.resize((new_w, new_h), Image.Resampling.LANCZOS)

        crop_y = 290 if new_h > 1200 else 60
        stage_crop = stage_resized.crop((0, crop_y, WIDTH, crop_y + HEIGHT))

        # Precision Opacity Mask:
        # Left side (x < 950): 100% black so the 3D logo has pure contrast and focus
        # Center & right: smooth emergence of concert crowd silhouettes
        # Top (y < 480): atmospheric darkening for typography clarity
        # Far right (x > 2650): clean fade to black
        mask = np.ones((HEIGHT, WIDTH), dtype=np.float32)
        for x in range(WIDTH):
            if x < 950:
                mask[:, x] = 0.0
            elif x < 1350:
                fade = (x - 950) / 400.0
                mask[:, x] *= fade * fade * (3.0 - 2.0 * fade)
            elif x > 2650:
                fade = 1.0 - (x - 2650) / 230.0
                mask[:, x] *= fade * fade * (3.0 - 2.0 * fade)

        for y in range(HEIGHT):
            if y < 480:
                fy = y / 480.0
                mask[y, :] *= (0.15 + 0.85 * fy * fy)

        stage_arr = np.array(stage_crop)
        stage_arr[:, :, 3] = np.clip(mask * 255 * 0.90, 0, 255).astype(np.uint8)
        canvas = Image.alpha_composite(canvas, Image.fromarray(stage_arr, "RGBA"))
    else:
        base_arr = np.zeros((HEIGHT, WIDTH, 4), dtype=np.uint8)
        for y in range(HEIGHT):
            ratio = y / float(HEIGHT)
            base_arr[y, :, 0] = int(5 * (1.0 - ratio * 0.3))
            base_arr[y, :, 1] = int(7 + 4 * (1.0 - ratio))
            base_arr[y, :, 2] = int(14 + 14 * (1.0 - ratio * 0.5))
            base_arr[y, :, 3] = 255
        canvas = Image.fromarray(base_arr, "RGBA")

    # 2. Acoustic Soundwave Ribbons (Music energy)
    ribbon_layer = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    rdraw = ImageDraw.Draw(ribbon_layer)
    pts_cyan, pts_mag, pts_indigo = [], [], []
    for x in range(0, WIDTH + 40, 12):
        yc = int(480 + 95 * math.sin(x * 0.0022 - 0.2) + 40 * math.sin(x * 0.0044))
        ym = int(520 + 105 * math.sin(x * 0.0019 + 1.1) + 45 * math.cos(x * 0.0038))
        yi = int(560 + 90 * math.cos(x * 0.0021 + 0.5) + 35 * math.sin(x * 0.0050))
        pts_cyan.append((x, yc))
        pts_mag.append((x, ym))
        pts_indigo.append((x, yi))
    for i in range(len(pts_cyan) - 1):
        rdraw.line([pts_indigo[i], pts_indigo[i+1]], fill=(30, 80, 210, 35), width=7)
        rdraw.line([pts_mag[i], pts_mag[i+1]], fill=(180, 35, 205, 38), width=5)
        rdraw.line([pts_cyan[i], pts_cyan[i+1]], fill=(0, 220, 255, 45), width=3)
    canvas = Image.alpha_composite(canvas, ribbon_layer.filter(ImageFilter.GaussianBlur(26)))
    canvas = Image.alpha_composite(canvas, ribbon_layer.filter(ImageFilter.GaussianBlur(4)))

    # 3. Stage Floor Plane
    floor_y = 865
    floor_layer = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    fdraw = ImageDraw.Draw(floor_layer)
    fdraw.line([(0, floor_y), (WIDTH, floor_y)], fill=(50, 80, 130, 50), width=1)
    for r in range(400, 0, -25):
        fdraw.ellipse([480 - r, floor_y + 45 - int(r*0.20), 480 + r, floor_y + 45 + int(r*0.20)], fill=(0, 180, 255, int(22 * (1.0 - (r / 400.0)))))
        fdraw.ellipse([580 - r, floor_y + 55 - int(r*0.18), 580 + r, floor_y + 55 + int(r*0.18)], fill=(180, 40, 210, int(22 * (1.0 - (r / 400.0)))))
    for r in range(450, 0, -25):
        fdraw.ellipse([2120 - r, floor_y + 45 - int(r*0.20), 2120 + r, floor_y + 45 + int(r*0.20)], fill=(70, 100, 230, int(25 * (1.0 - (r / 450.0)))))
        fdraw.ellipse([1850 - r, floor_y + 50 - int(r*0.18), 1850 + r, floor_y + 50 + int(r*0.18)], fill=(0, 195, 255, int(25 * (1.0 - (r / 450.0)))))
        fdraw.ellipse([2350 - r, floor_y + 50 - int(r*0.18), 2350 + r, floor_y + 50 + int(r*0.18)], fill=(190, 40, 190, int(25 * (1.0 - (r / 450.0)))))
    floor_layer = floor_layer.filter(ImageFilter.GaussianBlur(32))
    canvas = Image.alpha_composite(canvas, floor_layer)

    # 5. Unified 3D Logo Lockup
    logo_img = Image.open(LOGO_SRC_PATH).convert('RGBA')
    if logo_img.width > 900 and logo_img.height > 900:
        full_logo = logo_img.crop((14, 32, 1006, 946))
    else:
        full_logo = logo_img
    target_h = 780
    target_w = int(full_logo.width * (target_h / float(full_logo.height)))
    logo_resized = full_logo.resize((target_w, target_h), Image.Resampling.LANCZOS)
    logo_x = 130
    logo_y = 115
    logo_base = logo_y + target_h

    add_studio_shadow(canvas, logo_resized, logo_x, logo_y, blur_radius=46, opacity=195, offset=(0, 26))
    add_floor_reflection(canvas, logo_resized, logo_x, logo_base, max_alpha=75, fade_height=200, blur_val=8)
    canvas.paste(logo_resized, (logo_x, logo_y), logo_resized)

    # 6. Middle Typography & Platform Glass Pill (Centered editorial lockup)
    col_center = 1325
    draw = ImageDraw.Draw(canvas)
    tagline_font = get_font(88, bold=True)
    tag_y = 280
    title_text = "Hear every layer."
    t_bbox = tagline_font.getbbox(title_text)
    tw = t_bbox[2] - t_bbox[0]
    draw.text((col_center - tw // 2, tag_y), title_text, font=tagline_font, fill=(255, 255, 255, 255))

    sub_font = get_font(34, light=False)
    sub_y = tag_y + 112
    sub_text = "Lossless sound. Zero tracking. Completely yours."
    s_bbox = sub_font.getbbox(sub_text)
    sw = s_bbox[2] - s_bbox[0]
    draw.text((col_center - sw // 2, sub_y), sub_text, font=sub_font, fill=(215, 232, 255, 245))

    pill = create_glass_platform_pill(light_mode=False)
    pill_x = col_center - pill.width // 2
    pill_y = sub_y + 64
    canvas.paste(pill, (pill_x, pill_y), pill)

    # 7. Phones
    phone_hero = create_ultra_flagship_phone(SCREEN_HOME_PATH, target_height=855, light_mode=False)
    phone_player = create_ultra_flagship_phone(SCREEN_PLAYER_PATH, target_height=815, light_mode=False)

    p1_x, p1_y = 1735, 80
    p1_base = p1_y + phone_hero.height

    p2_x, p2_y = 2210, 120
    p2_base = p2_y + phone_player.height

    add_studio_shadow(canvas, phone_player, p2_x, p2_y, blur_radius=42, opacity=195, offset=(0, 25))
    add_floor_reflection(canvas, phone_player, p2_x, p2_base, max_alpha=72, fade_height=190, blur_val=6)
    canvas.paste(phone_player, (p2_x, p2_y), phone_player)

    add_studio_shadow(canvas, phone_hero, p1_x, p1_y, blur_radius=46, opacity=205, offset=(0, 28))
    add_floor_reflection(canvas, phone_hero, p1_x, p1_base, max_alpha=78, fade_height=200, blur_val=6)
    canvas.paste(phone_hero, (p1_x, p1_y), phone_hero)

    # 8. Real Official GitHub Octocat Mark
    gh = get_official_github_mark(light_mode=False, target_size=60)
    canvas.paste(gh, (WIDTH - 110, 45), gh)

    canvas.convert("RGB").save(OUTPUT_DARK_PATH, "WEBP", quality=95, method=6)
    print(f"Generated official dark banner: {OUTPUT_DARK_PATH}")

def generate_light_unified_banner():
    """Generates the light mode unified banner with concert stage backdrop."""
    # 1. Concert Stage Base Background (Light Mode)
    base_arr = np.zeros((HEIGHT, WIDTH, 4), dtype=np.uint8)
    for y in range(HEIGHT):
        ratio = y / float(HEIGHT)
        base_arr[y, :, 0] = int(252 - ratio * 14)
        base_arr[y, :, 1] = int(253 - ratio * 12)
        base_arr[y, :, 2] = int(255 - ratio * 8)
        base_arr[y, :, 3] = 255
    canvas = Image.fromarray(base_arr, "RGBA")

    if os.path.exists(BG_LIGHT_CONCERT):
        bg_src = Image.open(BG_LIGHT_CONCERT).convert("RGBA")
        new_w = WIDTH
        new_h = int(bg_src.height * (WIDTH / float(bg_src.width)))
        bg_resized = bg_src.resize((new_w, new_h), Image.Resampling.LANCZOS)
        stage_crop = bg_resized.crop((0, 60, WIDTH, 60 + HEIGHT))

        # Precision Opacity Mask for Light Mode:
        # Left side (x < 950): clean isolated background for logo focus
        # Center & right: graceful emergence of concert crowd silhouettes
        mask = np.ones((HEIGHT, WIDTH), dtype=np.float32)
        for x in range(WIDTH):
            if x < 950:
                mask[:, x] = 0.0
            elif x < 1350:
                fade = (x - 950) / 400.0
                mask[:, x] *= fade * fade * (3.0 - 2.0 * fade)
            elif x > 2650:
                fade = 1.0 - (x - 2650) / 230.0
                mask[:, x] *= fade * fade * (3.0 - 2.0 * fade)

        for y in range(HEIGHT):
            if y < 450:
                fy = y / 450.0
                mask[y, :] *= (0.3 + 0.7 * fy * fy)

        stage_arr = np.array(stage_crop)
        stage_arr[:, :, 3] = np.clip(mask * stage_arr[:, :, 3].astype(np.float32), 0, 255).astype(np.uint8)
        canvas = Image.alpha_composite(canvas, Image.fromarray(stage_arr, "RGBA"))

    # 2. Soft Ambient Pastel Glows
    glow_layer = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow_layer)
    for r in range(450, 0, -25):
        gdraw.ellipse([480 - r, 420 - r, 480 + r, 420 + r], fill=(0, 190, 255, int(22 * (1.0 - (r / 450.0)**0.8))))
        gdraw.ellipse([540 - r, 460 - r, 540 + r, 460 + r], fill=(210, 80, 240, int(18 * (1.0 - (r / 420.0)**0.8))))
    for r in range(500, 0, -25):
        gdraw.ellipse([2100 - r, 420 - r, 2100 + r, 420 + r], fill=(130, 110, 245, int(24 * (1.0 - (r / 500.0)**0.8))))
        gdraw.ellipse([1850 - r, 360 - r, 1850 + r, 360 + r], fill=(0, 185, 255, int(20 * (1.0 - (r / 460.0)**0.8))))
        gdraw.ellipse([2350 - r, 440 - r, 2350 + r, 440 + r], fill=(230, 75, 210, int(22 * (1.0 - (r / 440.0)**0.8))))
    canvas = Image.alpha_composite(canvas, glow_layer.filter(ImageFilter.GaussianBlur(70)))

    # 3. Soft Pastel Acoustic Ribbons
    ribbon_layer = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    rdraw = ImageDraw.Draw(ribbon_layer)
    pts_cyan, pts_mag, pts_indigo = [], [], []
    for x in range(0, WIDTH + 40, 12):
        yc = int(480 + 95 * math.sin(x * 0.0022 - 0.2) + 40 * math.sin(x * 0.0044))
        ym = int(520 + 105 * math.sin(x * 0.0019 + 1.1) + 45 * math.cos(x * 0.0038))
        yi = int(560 + 90 * math.cos(x * 0.0021 + 0.5) + 35 * math.sin(x * 0.0050))
        pts_cyan.append((x, yc))
        pts_mag.append((x, ym))
        pts_indigo.append((x, yi))
    for i in range(len(pts_cyan) - 1):
        rdraw.line([pts_indigo[i], pts_indigo[i+1]], fill=(100, 140, 240, 38), width=8)
        rdraw.line([pts_mag[i], pts_mag[i+1]], fill=(220, 80, 230, 40), width=6)
        rdraw.line([pts_cyan[i], pts_cyan[i+1]], fill=(0, 190, 245, 45), width=4)
    canvas = Image.alpha_composite(canvas, ribbon_layer.filter(ImageFilter.GaussianBlur(25)))
    canvas = Image.alpha_composite(canvas, ribbon_layer.filter(ImageFilter.GaussianBlur(4)))

    # 4. Stage Floor Plane
    floor_y = 865
    floor_layer = Image.new("RGBA", (WIDTH, HEIGHT), (0, 0, 0, 0))
    fdraw = ImageDraw.Draw(floor_layer)
    fdraw.line([(0, floor_y), (WIDTH, floor_y)], fill=(195, 208, 225, 90), width=1)
    for r in range(400, 0, -25):
        fdraw.ellipse([480 - r, floor_y + 45 - int(r*0.20), 480 + r, floor_y + 45 + int(r*0.20)], fill=(0, 180, 245, int(18 * (1.0 - (r / 400.0)))))
        fdraw.ellipse([580 - r, floor_y + 55 - int(r*0.18), 580 + r, floor_y + 55 + int(r*0.18)], fill=(210, 85, 220, int(18 * (1.0 - (r / 400.0)))))
    for r in range(450, 0, -25):
        fdraw.ellipse([2120 - r, floor_y + 45 - int(r*0.20), 2120 + r, floor_y + 45 + int(r*0.20)], fill=(120, 140, 245, int(20 * (1.0 - (r / 450.0)))))
        fdraw.ellipse([1850 - r, floor_y + 50 - int(r*0.18), 1850 + r, floor_y + 50 + int(r*0.18)], fill=(0, 195, 250, int(20 * (1.0 - (r / 450.0)))))
        fdraw.ellipse([2350 - r, floor_y + 50 - int(r*0.18), 2350 + r, floor_y + 50 + int(r*0.18)], fill=(225, 80, 210, int(20 * (1.0 - (r / 450.0)))))
    floor_layer = floor_layer.filter(ImageFilter.GaussianBlur(32))
    canvas = Image.alpha_composite(canvas, floor_layer)

    # 5. Unified 3D Logo Lockup
    logo_img = Image.open(LOGO_SRC_PATH).convert('RGBA')
    if logo_img.width > 900 and logo_img.height > 900:
        full_logo = logo_img.crop((14, 32, 1006, 946))
    else:
        full_logo = logo_img
    target_h = 780
    target_w = int(full_logo.width * (target_h / float(full_logo.height)))
    logo_resized = full_logo.resize((target_w, target_h), Image.Resampling.LANCZOS)
    logo_x = 130
    logo_y = 115
    logo_base = logo_y + target_h

    add_studio_shadow(canvas, logo_resized, logo_x, logo_y, blur_radius=40, opacity=85, offset=(0, 22), shadow_color=(80, 100, 130))
    add_floor_reflection(canvas, logo_resized, logo_x, logo_base, max_alpha=40, fade_height=190, blur_val=7)
    canvas.paste(logo_resized, (logo_x, logo_y), logo_resized)

    # 6. Middle Typography & Platform Glass Pill (Centered editorial lockup)
    col_center = 1325
    draw = ImageDraw.Draw(canvas)
    tagline_font = get_font(88, bold=True)
    tag_y = 280
    title_text = "Hear every layer."
    t_bbox = tagline_font.getbbox(title_text)
    tw = t_bbox[2] - t_bbox[0]
    draw.text((col_center - tw // 2, tag_y), title_text, font=tagline_font, fill=(15, 23, 42, 255))

    sub_font = get_font(34, light=False)
    sub_y = tag_y + 112
    sub_text = "Lossless sound. Zero tracking. Completely yours."
    s_bbox = sub_font.getbbox(sub_text)
    sw = s_bbox[2] - s_bbox[0]
    draw.text((col_center - sw // 2, sub_y), sub_text, font=sub_font, fill=(55, 68, 88, 245))

    pill = create_glass_platform_pill(light_mode=True)
    pill_x = col_center - pill.width // 2
    pill_y = sub_y + 64
    canvas.paste(pill, (pill_x, pill_y), pill)

    # 7. Phones
    phone_hero = create_ultra_flagship_phone(SCREEN_HOME_PATH, target_height=855, light_mode=True)
    phone_player = create_ultra_flagship_phone(SCREEN_PLAYER_PATH, target_height=815, light_mode=True)

    p1_x, p1_y = 1735, 80
    p1_base = p1_y + phone_hero.height

    p2_x, p2_y = 2210, 120
    p2_base = p2_y + phone_player.height

    add_studio_shadow(canvas, phone_player, p2_x, p2_y, blur_radius=38, opacity=95, offset=(0, 24), shadow_color=(70, 85, 115))
    add_floor_reflection(canvas, phone_player, p2_x, p2_base, max_alpha=40, fade_height=180, blur_val=6)
    canvas.paste(phone_player, (p2_x, p2_y), phone_player)

    add_studio_shadow(canvas, phone_hero, p1_x, p1_y, blur_radius=42, opacity=105, offset=(0, 26), shadow_color=(70, 85, 115))
    add_floor_reflection(canvas, phone_hero, p1_x, p1_base, max_alpha=45, fade_height=190, blur_val=6)
    canvas.paste(phone_hero, (p1_x, p1_y), phone_hero)

    # 8. Real Official GitHub Octocat Mark
    gh = get_official_github_mark(light_mode=True, target_size=60)
    canvas.paste(gh, (WIDTH - 110, 45), gh)

    canvas.convert("RGB").save(OUTPUT_LIGHT_PATH, "WEBP", quality=95, method=6)
    print(f"Generated official light banner: {OUTPUT_LIGHT_PATH}")

def main():
    """Main banner generator entry point."""
    print("Generating official Levyra banners with musical concert stage aesthetic...")
    generate_dark_unified_banner()
    generate_light_unified_banner()

    # Copy to Desktop
    if os.path.exists(DESKTOP_DIR):
        shutil.copyfile(OUTPUT_DARK_PATH, os.path.join(DESKTOP_DIR, "levyra-github-banner.webp"))
        shutil.copyfile(OUTPUT_LIGHT_PATH, os.path.join(DESKTOP_DIR, "levyra-github-banner-light.webp"))
        print(f"Copied both banners to Desktop: {DESKTOP_DIR}")

    print("All tasks completed successfully!")

if __name__ == "__main__":
    main()
