#!/usr/bin/env python3
"""
Generate Material 3 Expressive badges for Levyra.
Applies:
- Material 3 Expressive container: clean white surface with smooth rounded corners (rx=22)
- Distinctive signature M3 Expressive blooming rosettes (10-lobe & 12-lobe circular arc petals)
- BIG, bold, crisp brand logos (26px-28px) centered inside the expressive flower
- Authentic brand palettes: GitHub in official Dark Charcoal (#181717), F-Droid (#0284C7),
  OpenAPK (#059669), Windows (#0078D4), etc.
- Expressive typography: letter-spaced overline label + bold high-contrast title
- Subtle M3 tonal action chip on primary download cards
"""

import math
import os
import re

# Material 3 Expressive Blooming Rosette Generators (using smooth circular convex arcs)
def make_flower_10(cx, cy, r_valley=16.8, r_peak=21.2, lobes=10):
    step = 2 * math.pi / lobes
    half_step = step / 2
    d_mid = r_valley * math.cos(half_step)
    C = 2 * r_valley * math.sin(half_step)
    S = r_peak - d_mid
    R = S / 2.0 + (C**2) / (8.0 * S)
    d = []
    x0 = cx + r_valley * math.sin(-half_step)
    y0 = cy - r_valley * math.cos(-half_step)
    d.append(f"M {x0:.2f} {y0:.2f}")
    for i in range(lobes):
        a_end = (i + 0.5) * step
        x_end = cx + r_valley * math.sin(a_end)
        y_end = cy - r_valley * math.cos(a_end)
        d.append(f"A {R:.2f} {R:.2f} 0 0 1 {x_end:.2f} {y_end:.2f}")
    d.append("Z")
    return " ".join(d)

def make_flower_12(cx, cy, r_valley=17.2, r_peak=21.2, lobes=12):
    step = 2 * math.pi / lobes
    half_step = step / 2
    d_mid = r_valley * math.cos(half_step)
    C = 2 * r_valley * math.sin(half_step)
    S = r_peak - d_mid
    R = S / 2.0 + (C**2) / (8.0 * S)
    d = []
    x0 = cx + r_valley * math.sin(-half_step)
    y0 = cy - r_valley * math.cos(-half_step)
    d.append(f"M {x0:.2f} {y0:.2f}")
    for i in range(lobes):
        a_end = (i + 0.5) * step
        x_end = cx + r_valley * math.sin(a_end)
        y_end = cy - r_valley * math.cos(a_end)
        d.append(f"A {R:.2f} {R:.2f} 0 0 1 {x_end:.2f} {y_end:.2f}")
    d.append("Z")
    return " ".join(d)

SHAPE_10 = make_flower_10(30, 28)
SHAPE_12 = make_flower_12(30, 28)

def extract_base64_image(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()
    match = re.search(r'href="(data:image/png;base64,[^"]+)"', content)
    if not match:
        raise ValueError(f"Could not find base64 image in {file_path}")
    return match.group(1)

def make_primary_card(label, brand, icon_svg, brand_color, shape_path, label_color=None, brand_size="16.5"):
    if not label_color:
        label_color = brand_color
    title = f"{label} {brand}"
    chip_fill = f"{brand_color}14"
    chip_border = f"{brand_color}2E"

    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="185" height="56" viewBox="0 0 185 56" role="img" aria-label="{title}">
  <title>{title}</title>
  <rect x=".75" y=".75" width="183.5" height="54.5" rx="22" fill="#FFFFFF" stroke="#CBD5E1" stroke-width="1.5"/>
  <path d="{shape_path}" fill="{brand_color}"/>
  {icon_svg}
  <text x="59" y="21" fill="{label_color}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="8.5" font-weight="700" letter-spacing=".9">{label}</text>
  <text x="59" y="41" fill="#0F172A" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="{brand_size}" font-weight="800">{brand}</text>
  <circle cx="163" cy="28" r="11" fill="{chip_fill}" stroke="{chip_border}" stroke-width="1"/>
  <path d="M163 21v8.5m0 0l-2.8-2.8m2.8 2.8l2.8-2.8" fill="none" stroke="{brand_color}" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M159 33.5h8" fill="none" stroke="{brand_color}" stroke-width="1.7" stroke-linecap="round"/>
</svg>"""

def make_secondary_card(label, brand, icon_svg, brand_color, shape_path, label_color=None, brand_size="16"):
    if not label_color:
        label_color = brand_color
    title = f"{label} {brand}"

    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="185" height="56" viewBox="0 0 185 56" role="img" aria-label="{title}">
  <title>{title}</title>
  <rect x=".75" y=".75" width="183.5" height="54.5" rx="22" fill="#FFFFFF" stroke="#CBD5E1" stroke-width="1.5"/>
  <path d="{shape_path}" fill="{brand_color}"/>
  {icon_svg}
  <text x="59" y="21" fill="{label_color}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="8.5" font-weight="700" letter-spacing=".9">{label}</text>
  <text x="59" y="41" fill="#0F172A" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="{brand_size}" font-weight="800">{brand}</text>
</svg>"""

def main():
    assets_dir = "docs/assets"

    # 1. GitHub (Authentic Dark Charcoal #181717, BIG Octocat logo 26px!)
    github_icon = '''<g transform="translate(17 15) scale(1.08)"><path fill="#FFFFFF" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"/></g>'''
    badge_github = make_primary_card("DOWNLOAD FROM", "GitHub", github_icon, "#181717", SHAPE_10, label_color="#57606A", brand_size="16.5")
    for f_name in [
        "levyra-github-download.svg",
        "levyra-github-download-desktop184.svg",
        "levyra-github-download-light.svg",
        "levyra-github-download-desktop184-light.svg"
    ]:
        with open(os.path.join(assets_dir, f_name), "w", encoding="utf-8", newline="\n") as f:
            f.write(badge_github)

    # 2. F-Droid (Android Blue #0284C7, BIG Robot 26px!)
    fdroid_img = extract_base64_image(os.path.join(assets_dir, "levyra-fdroid.svg"))
    fdroid_icon = f'<image x="17" y="15" width="26" height="26" preserveAspectRatio="xMidYMid meet" href="{fdroid_img}"/>'
    badge_fdroid = make_primary_card("GET IT ON", "F-Droid", fdroid_icon, "#0284C7", SHAPE_12, label_color="#0284C7", brand_size="16.5")
    for f_name in [
        "levyra-fdroid.svg",
        "levyra-fdroid-desktop184.svg",
        "levyra-fdroid-light.svg",
        "levyra-fdroid-desktop184-light.svg"
    ]:
        with open(os.path.join(assets_dir, f_name), "w", encoding="utf-8", newline="\n") as f:
            f.write(badge_fdroid)

    # 3. OpenAPK (Emerald #059669, BIG Package Icon 26px!)
    openapk_icon = '''<g transform="translate(17 15) scale(0.038)"><path fill="#FFFFFF" d="M742.4,418.6c-0.2-4.1-0.5-8.1-0.8-12.2c-12.9-111.3-79-205.3-171.9-256.7l-1.7-0.9l0,0l9.2-15.9l47.9-82.9c1-1.7,1.6-3.7,1.6-5.9c0-6.6-5.3-11.9-11.9-11.9c-4.4,0-8.2,2.4-10.2,5.9l0,0l-47.9,82.9l-9.9,17.1c-43.1-19.8-93.6-31.4-146.8-31.4c-52.3,0-102,11.2-146.8,31.3l0,0.1l-2.2,0.9c0.7-0.3,1.5-0.7,2.2-1l-9.8-17.1l-47.9-83c-2.1-3.6-5.9-5.9-10.3-5.9c-6.6,0-11.9,5.3-11.9,11.9c0,2.2,0.6,4.2,1.6,6l0,0l47.9,83l9.2,15.9C137.6,201,71.5,294.8,58.5,404.8c-0.4,4.6-0.7,9.2-0.9,13.8c-0.3,5.8-0.4,11.7-0.4,17.6c0,151.8,92.5,281.2,222.5,331.6l79-217.6c-44.7-17.3-76.5-61.8-76.5-114c0-6,0.4-11.9,1.2-17.6c8.3-58.9,57.3-104.1,116.6-104.1c59.3,0,108.4,45.2,116.6,104.1c0.8,5.8,1.2,11.6,1.2,17.6c0,52.2-31.8,96.7-76.5,114l79,217.6c130-50.4,222.5-179.8,222.5-331.6C742.9,430.3,742.7,424.4,742.4,418.6z M242.1,263.8c15.8,0,28.6,12.8,28.6,28.6c0,15.8-12.8,28.5-28.6,28.6c-15.8,0-28.6-12.8-28.6-28.6C213.6,276.6,226.4,263.8,242.1,263.8z M557.8,263.8c15.8,0,28.6,12.8,28.6,28.6c0,15.8-12.8,28.5-28.6,28.5c-15.8,0-28.6-12.8-28.6-28.6C529.2,276.6,542,263.8,557.8,263.8z"/></g>'''
    badge_openapk = make_primary_card("GET IT ON", "OpenAPK", openapk_icon, "#059669", SHAPE_10, label_color="#059669", brand_size="16.5")
    for f_name in [
        "levyra-openapk.svg",
        "levyra-openapk-desktop184.svg",
        "levyra-openapk-light.svg",
        "levyra-openapk-desktop184-light.svg"
    ]:
        with open(os.path.join(assets_dir, f_name), "w", encoding="utf-8", newline="\n") as f:
            f.write(badge_openapk)

    # 4. Windows (Azure Blue #0078D4, BIG Windows Tiles 26px!)
    windows_icon = '''<g transform="translate(17 15) scale(0.52)" fill="#FFFFFF"><rect x="0" y="0" width="23" height="23" rx="3"/><rect x="27" y="0" width="23" height="23" rx="3"/><rect x="0" y="27" width="23" height="23" rx="3"/><rect x="27" y="27" width="23" height="23" rx="3"/></g>'''
    badge_windows = make_primary_card("DOWNLOAD FOR", "Windows", windows_icon, "#0078D4", SHAPE_12, label_color="#0078D4", brand_size="16.5")
    for f_name in [
        "levyra-windows-download.svg",
        "levyra-windows-download-desktop184.svg",
        "levyra-windows-download-light.svg",
        "levyra-windows-download-desktop184-light.svg"
    ]:
        with open(os.path.join(assets_dir, f_name), "w", encoding="utf-8", newline="\n") as f:
            f.write(badge_windows)

    # 5. IzzyOnDroid (Royal Blue #2563EB)
    izzy_img = extract_base64_image(os.path.join(assets_dir, "levyra-izzyondroid.svg"))
    izzy_icon = f'<image x="17" y="15" width="26" height="26" preserveAspectRatio="xMidYMid meet" href="{izzy_img}"/>'
    badge_izzy = make_secondary_card("GET IT ON", "IzzyOnDroid", izzy_icon, "#2563EB", SHAPE_12, brand_size="15")
    for f_name in [
        "levyra-izzyondroid.svg",
        "levyra-izzyondroid-desktop184.svg",
        "levyra-izzyondroid-light.svg",
        "levyra-izzyondroid-desktop184-light.svg"
    ]:
        with open(os.path.join(assets_dir, f_name), "w", encoding="utf-8", newline="\n") as f:
            f.write(badge_izzy)

    # 6. SourceForge (Flame Orange #EA580C)
    sf_icon = '''<g transform="translate(17.5 16.5) scale(1.05)"><path fill="#FFFFFF" d="M9.998 1.4648c-.088 0-.1761.0395-.2402.1036L.1016 11.2148c-.1361.1281-.1361.3444 0 .4805l9.1523 9.1348c.064.072.1522.1054.2402.1054.0881 0 .1743-.0414.2383-.1054l1.3692-1.3848c.136-.128.136-.3444 0-.4805l-7.172-7.1738a.4682.4682 0 010-.664l9.0782-9.0782c.1201-.12.1123-.2728.0723-.377-.04-.096-.1444-.207-.3125-.207H9.998zm4.5313 1.5918c-.088 0-.1762.0415-.2402.1055l-1.3926 1.3926c-.1361.128-.1361.3444 0 .4805l7.1719 7.1816c.088.088.1367.208.1367.3281 0 .1201-.0487.24-.1367.3281l-9.0782 9.0782c-.12.12-.1123.2728-.0722.377.04.096.1443.207.3125.207v-.0079H14c.088 0 .1762-.0316.2402-.0957l9.6563-9.6543c.072-.064.1035-.1521.1035-.2402 0-.088-.0395-.1762-.1035-.2402l-9.127-9.1348c-.064-.072-.1521-.1055-.2402-.1055zM11.4336 6.213c-.1125.005-.2164.1007-.211.2383.1442 2.2176-2.6425 2.7635-2.6425 6.246v.0235c0 2.1216 1.6026 3.8418 3.58 3.8418 1.9775 0 3.5782-1.7202 3.5782-3.8418v-.0234c0-.9848-.3679-1.9225-.7442-2.627-.072-.136-.2896-.087-.2656.041.6725 2.9943-1.0156 4.8507-1.0156 2.545 0-3.931-1.3943-5.7162-2.1309-6.3887a.2072.2072 0 00-.1484-.0547Z"/></g>'''
    badge_sf = make_secondary_card("DOWNLOAD FROM", "SourceForge", sf_icon, "#EA580C", SHAPE_10, brand_size="15")
    for f_name in [
        "levyra-sourceforge.svg",
        "levyra-sourceforge-desktop184.svg",
        "levyra-sourceforge-light.svg",
        "levyra-sourceforge-desktop184-light.svg"
    ]:
        with open(os.path.join(assets_dir, f_name), "w", encoding="utf-8", newline="\n") as f:
            f.write(badge_sf)

    # 7. APKPure (Emerald Green #16A34A)
    apkpure_icon = '''<g transform="translate(17 15) scale(.027)" fill="#FFFFFF">
    <path d="M892.99993 611.72969 970.85741 611.77319 929.89641 688.37305Z"/>
    <path d="M794.99333 672 929.08802 671.54855 861.16069 803Z"/>
    <path d="M768.29222 932.79238 669 732.66606 867.96047 731.35091 770.04312 933.29238Z"/>
    <path d="M28.897776 715.5 346.90236 65.5 560.01789 64.99095 134.1887 928.20241Z"/>
    <path d="M458 289.69993 563.68924 73.231804 771.26447 497.72405 662.00331 710Z"/>
    <path d="M254 710.5329 358.22025 498H546.80163L650 710.63348Z"/>
  </g>'''
    with open(os.path.join(assets_dir, "levyra-apkpure.svg"), "w", encoding="utf-8", newline="\n") as f:
        f.write(make_secondary_card("GET IT ON", "APKPure", apkpure_icon, "#16A34A", SHAPE_12, brand_size="16.5"))

    # 8. APKFab (Royal Purple #7C3AED)
    apkfab_icon = '''<path fill="#FFFFFF" transform="translate(6 5) scale(0.95)" d="M20.5 14.6c.8-2.5 2.4-3.6 5.5-3.6s4.7 1.1 5.5 3.6l6 18.6c.6 1.8.2 3.2-1 4.2-1.1.9-2.7 1.2-4.8 1.1-2.4-.1-3.4-1-3.6-3.5l-.3-3.2h-3.6l-.3 3.2c-.2 2.5-1.2 3.4-3.6 3.5-2.1.1-3.7-.2-4.8-1.1-1.2-1-1.6-2.4-1-4.2l6-18.6z"/>
  <path fill="#7C3AED" transform="translate(6 5) scale(0.95)" d="M26 20.2c-.2 2.3.5 3.6 1.9 5 1.2 1.2 1.8 2.3 1.8 3.8 0 2.2-1.7 4-3.8 4-2.2 0-3.9-1.8-3.9-4 0-1.7.8-3 2-4.1-.1 1.6.7 2.5 1.6 2.5.8 0 1.3-.7 1.3-1.7 0-1.2-.9-2.3-.9-3.8 0-.6 0-1.1 0-1.7z"/>'''
    with open(os.path.join(assets_dir, "levyra-apkfab.svg"), "w", encoding="utf-8", newline="\n") as f:
        f.write(make_secondary_card("GET IT ON", "APKFab", apkfab_icon, "#7C3AED", SHAPE_10, brand_size="16.5"))

    # 9. AndroidFreeware (Teal #0D9488)
    af_img = extract_base64_image(os.path.join(assets_dir, "levyra-androidfreeware-download.svg"))
    af_icon = f'<image x="17" y="15" width="26" height="26" preserveAspectRatio="xMidYMid meet" href="{af_img}"/>'
    badge_af = make_secondary_card("GET IT ON", "AndroidFreeware", af_icon, "#0D9488", SHAPE_12, brand_size="12.5")
    for f_name in [
        "levyra-androidfreeware-download.svg",
        "levyra-androidfreeware-download-desktop184.svg",
        "levyra-androidfreeware-download-light.svg",
        "levyra-androidfreeware-download-desktop184-light.svg"
    ]:
        with open(os.path.join(assets_dir, f_name), "w", encoding="utf-8", newline="\n") as f:
            f.write(badge_af)

    # 10. Obtainium (Cyan #0284C7)
    obtainium_img = extract_base64_image(os.path.join(assets_dir, "levyra-obtainium-download.svg"))
    obtainium_icon = f'<image x="17" y="15" width="26" height="26" preserveAspectRatio="xMidYMid meet" href="{obtainium_img}"/>'
    badge_obtainium = make_secondary_card("INSTALL WITH", "Obtainium", obtainium_icon, "#0284C7", SHAPE_10, brand_size="16")
    for f_name in [
        "levyra-obtainium-download.svg",
        "levyra-obtainium-download-desktop184.svg",
        "levyra-obtainium-download-light.svg",
        "levyra-obtainium-download-desktop184-light.svg"
    ]:
        with open(os.path.join(assets_dir, f_name), "w", encoding="utf-8", newline="\n") as f:
            f.write(badge_obtainium)

    # 11. Appteka (Crimson #E11D48)
    appteka_img = extract_base64_image(os.path.join(assets_dir, "levyra-appteka.svg"))
    appteka_icon = f'<image x="17" y="15" width="26" height="26" preserveAspectRatio="xMidYMid meet" href="{appteka_img}"/>'
    badge_appteka = make_secondary_card("GET IT ON", "Appteka", appteka_icon, "#E11D48", SHAPE_12, brand_size="16.5")
    for f_name in [
        "levyra-appteka.svg",
        "levyra-appteka-desktop184.svg",
        "levyra-appteka-light.svg",
        "levyra-appteka-desktop184-light.svg"
    ]:
        with open(os.path.join(assets_dir, f_name), "w", encoding="utf-8", newline="\n") as f:
            f.write(badge_appteka)

    print("Generated all M3 Expressive download badges with big logos & authentic colors.")

if __name__ == "__main__":
    main()
