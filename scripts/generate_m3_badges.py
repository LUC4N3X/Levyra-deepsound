#!/usr/bin/env python3
"""
Generate Material 3 Expressive badges for Levyra.
Applies:
- White card container (fill="#FFFFFF", stroke="#E2E8F0", rx=16)
- Signature Material 3 Expressive scalloped rosette icon chip (12 lobes) in vibrant M3 expressive color
- Authentic white brand icons centered within the rosette
- M3 Expressive typography: colored Label Small overline (7.5px, bold, tracking 0.8) + high-contrast Title Medium (15px, bold 750)
- M3 download action chip on the right for primary badges
"""

import math
import os
import re

def make_scallop_path(cx, cy, r_outer, r_inner, lobes=12):
    d = []
    step = 2 * math.pi / lobes
    for i in range(lobes):
        a_start = i * step - step / 2
        a_mid = i * step
        a_end = i * step + step / 2
        x_start = cx + r_inner * math.sin(a_start)
        y_start = cy - r_inner * math.cos(a_start)
        x_mid = cx + r_outer * math.sin(a_mid)
        y_mid = cy - r_outer * math.cos(a_mid)
        x_end = cx + r_inner * math.sin(a_end)
        y_end = cy - r_inner * math.cos(a_end)
        if i == 0:
            d.append(f"M {x_start:.2f} {y_start:.2f}")
        d.append(f"Q {x_mid:.2f} {y_mid:.2f} {x_end:.2f} {y_end:.2f}")
    d.append("Z")
    return " ".join(d)

CARD_SCALLOP = make_scallop_path(26, 25, 17.5, 14.0, 12)

def extract_base64_image(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()
    match = re.search(r'href="(data:image/png;base64,[^"]+)"', content)
    if not match:
        raise ValueError(f"Could not find base64 image in {file_path}")
    return match.group(1)

def make_primary_card(label, brand, icon_svg, accent_color, brand_font_size="15"):
    title = f"{label} {brand}"
    chip_fill = f"{accent_color}18"
    chip_border = f"{accent_color}40"

    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="184" height="57.5" viewBox="0 0 160 50" role="img" aria-label="{title}">
  <title>{title}</title>
  <rect x=".75" y=".75" width="158.5" height="48.5" rx="16" fill="#FFFFFF" stroke="#E2E8F0" stroke-width="1.5"/>
  <path d="{CARD_SCALLOP}" fill="{accent_color}"/>
  {icon_svg}
  <text x="50" y="19" fill="{accent_color}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="7.5" font-weight="700" letter-spacing=".8">{label}</text>
  <text x="50" y="36.5" fill="#0F172A" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="{brand_font_size}" font-weight="750">{brand}</text>
  <circle cx="140" cy="25" r="13" fill="{chip_fill}" stroke="{chip_border}" stroke-width="1"/>
  <path d="M140 17v10.5m0 0l-3.5-3.5m3.5 3.5l3.5-3.5" fill="none" stroke="{accent_color}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M135 31.5h10" fill="none" stroke="{accent_color}" stroke-width="1.8" stroke-linecap="round"/>
</svg>"""

def make_secondary_card(label, brand, icon_svg, accent_color, brand_font_size="14.5"):
    title = f"{label} {brand}"

    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="184" height="57.5" viewBox="0 0 160 50" role="img" aria-label="{title}">
  <title>{title}</title>
  <rect x=".75" y=".75" width="158.5" height="48.5" rx="16" fill="#FFFFFF" stroke="#E2E8F0" stroke-width="1.5"/>
  <path d="{CARD_SCALLOP}" fill="{accent_color}"/>
  {icon_svg}
  <text x="50" y="19" fill="{accent_color}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="7.5" font-weight="700" letter-spacing=".8">{label}</text>
  <text x="50" y="36.5" fill="#0F172A" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="{brand_font_size}" font-weight="750">{brand}</text>
</svg>"""

def main():
    assets_dir = "docs/assets"

    # 1. GitHub (Forest Green #15803D like the screenshot)
    github_icon = '''<g transform="translate(17.5 16.5) scale(0.71)"><path fill="#FFFFFF" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"/></g>'''
    badge_github = make_primary_card("DOWNLOAD FROM", "GitHub", github_icon, "#15803D", "15")
    for f_name in [
        "levyra-github-download.svg",
        "levyra-github-download-desktop184.svg",
        "levyra-github-download-light.svg",
        "levyra-github-download-desktop184-light.svg"
    ]:
        with open(os.path.join(assets_dir, f_name), "w", encoding="utf-8", newline="\n") as f:
            f.write(badge_github)

    # 2. F-Droid (Sky Blue #0284C7 like screenshot)
    fdroid_img = extract_base64_image(os.path.join(assets_dir, "levyra-fdroid.svg"))
    fdroid_icon = f'<image x="14" y="13" width="24" height="24" preserveAspectRatio="xMidYMid meet" href="{fdroid_img}"/>'
    badge_fdroid = make_primary_card("GET IT ON", "F-Droid", fdroid_icon, "#0284C7", "15")
    for f_name in [
        "levyra-fdroid.svg",
        "levyra-fdroid-desktop184.svg",
        "levyra-fdroid-light.svg",
        "levyra-fdroid-desktop184-light.svg"
    ]:
        with open(os.path.join(assets_dir, f_name), "w", encoding="utf-8", newline="\n") as f:
            f.write(badge_fdroid)

    # 3. OpenAPK (Emerald #059669)
    openapk_icon = '''<g transform="translate(14.5 13.5) scale(0.033)"><path fill="#FFFFFF" d="M742.4,418.6c-0.2-4.1-0.5-8.1-0.8-12.2c-12.9-111.3-79-205.3-171.9-256.7l-1.7-0.9l0,0l9.2-15.9l47.9-82.9c1-1.7,1.6-3.7,1.6-5.9c0-6.6-5.3-11.9-11.9-11.9c-4.4,0-8.2,2.4-10.2,5.9l0,0l-47.9,82.9l-9.9,17.1c-43.1-19.8-93.6-31.4-146.8-31.4c-52.3,0-102,11.2-146.8,31.3l0,0.1l-2.2,0.9c0.7-0.3,1.5-0.7,2.2-1l-9.8-17.1l-47.9-83c-2.1-3.6-5.9-5.9-10.3-5.9c-6.6,0-11.9,5.3-11.9,11.9c0,2.2,0.6,4.2,1.6,6l0,0l47.9,83l9.2,15.9C137.6,201,71.5,294.8,58.5,404.8c-0.4,4.6-0.7,9.2-0.9,13.8c-0.3,5.8-0.4,11.7-0.4,17.6c0,151.8,92.5,281.2,222.5,331.6l79-217.6c-44.7-17.3-76.5-61.8-76.5-114c0-6,0.4-11.9,1.2-17.6c8.3-58.9,57.3-104.1,116.6-104.1c59.3,0,108.4,45.2,116.6,104.1c0.8,5.8,1.2,11.6,1.2,17.6c0,52.2-31.8,96.7-76.5,114l79,217.6c130-50.4,222.5-179.8,222.5-331.6C742.9,430.3,742.7,424.4,742.4,418.6z M242.1,263.8c15.8,0,28.6,12.8,28.6,28.6c0,15.8-12.8,28.5-28.6,28.6c-15.8,0-28.6-12.8-28.6-28.6C213.6,276.6,226.4,263.8,242.1,263.8z M557.8,263.8c15.8,0,28.6,12.8,28.6,28.6c0,15.8-12.8,28.5-28.6,28.6c-15.8,0-28.6-12.8-28.6-28.6C529.2,276.6,542,263.8,557.8,263.8z"/></g>'''
    badge_openapk = make_primary_card("GET IT ON", "OpenAPK", openapk_icon, "#059669", "15")
    for f_name in [
        "levyra-openapk.svg",
        "levyra-openapk-desktop184.svg",
        "levyra-openapk-light.svg",
        "levyra-openapk-desktop184-light.svg"
    ]:
        with open(os.path.join(assets_dir, f_name), "w", encoding="utf-8", newline="\n") as f:
            f.write(badge_openapk)

    # 4. Windows (Azure #0078D4)
    windows_icon = '''<g transform="translate(18 17) scale(0.32)" fill="#FFFFFF"><rect x="0" y="0" width="23" height="23" rx="2"/><rect x="27" y="0" width="23" height="23" rx="2"/><rect x="0" y="27" width="23" height="23" rx="2"/><rect x="27" y="27" width="23" height="23" rx="2"/></g>'''
    badge_windows = make_primary_card("DOWNLOAD FOR", "Windows", windows_icon, "#0078D4", "15")
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
    izzy_icon = f'<image x="14" y="13" width="24" height="24" preserveAspectRatio="xMidYMid meet" href="{izzy_img}"/>'
    badge_izzy = make_secondary_card("GET IT ON", "IzzyOnDroid", izzy_icon, "#2563EB", "13.5")
    for f_name in [
        "levyra-izzyondroid.svg",
        "levyra-izzyondroid-desktop184.svg",
        "levyra-izzyondroid-light.svg",
        "levyra-izzyondroid-desktop184-light.svg"
    ]:
        with open(os.path.join(assets_dir, f_name), "w", encoding="utf-8", newline="\n") as f:
            f.write(badge_izzy)

    # 6. SourceForge (Flame Orange #EA580C)
    sf_icon = '''<g transform="translate(17 16) scale(0.95)"><path fill="#FFFFFF" d="M9.998 1.4648c-.088 0-.1761.0395-.2402.1036L.1016 11.2148c-.1361.1281-.1361.3444 0 .4805l9.1523 9.1348c.064.072.1522.1054.2402.1054.0881 0 .1743-.0414.2383-.1054l1.3692-1.3848c.136-.128.136-.3444 0-.4805l-7.172-7.1738a.4682.4682 0 010-.664l9.0782-9.0782c.1201-.12.1123-.2728.0723-.377-.04-.096-.1444-.207-.3125-.207H9.998zm4.5313 1.5918c-.088 0-.1762.0415-.2402.1055l-1.3926 1.3926c-.1361.128-.1361.3444 0 .4805l7.1719 7.1816c.088.088.1367.208.1367.3281 0 .1201-.0487.24-.1367.3281l-9.0782 9.0782c-.12.12-.1123.2728-.0722.377.04.096.1443.207.3125.207v-.0079H14c.088 0 .1762-.0316.2402-.0957l9.6563-9.6543c.072-.064.1035-.1521.1035-.2402 0-.088-.0395-.1762-.1035-.2402l-9.127-9.1348c-.064-.072-.1521-.1055-.2402-.1055zM11.4336 6.213c-.1125.005-.2164.1007-.211.2383.1442 2.2176-2.6425 2.7635-2.6425 6.246v.0235c0 2.1216 1.6026 3.8418 3.58 3.8418 1.9775 0 3.5782-1.7202 3.5782-3.8418v-.0234c0-.9848-.3679-1.9225-.7442-2.627-.072-.136-.2896-.087-.2656.041.6725 2.9943-1.0156 4.8507-1.0156 2.545 0-3.931-1.3943-5.7162-2.1309-6.3887a.2072.2072 0 00-.1484-.0547Z"/></g>'''
    badge_sf = make_secondary_card("DOWNLOAD FROM", "SourceForge", sf_icon, "#EA580C", "14.5")
    for f_name in [
        "levyra-sourceforge.svg",
        "levyra-sourceforge-desktop184.svg",
        "levyra-sourceforge-light.svg",
        "levyra-sourceforge-desktop184-light.svg"
    ]:
        with open(os.path.join(assets_dir, f_name), "w", encoding="utf-8", newline="\n") as f:
            f.write(badge_sf)

    # 7. APKPure (Green #16A34A)
    apkpure_icon = '''<g transform="translate(14 13) scale(.022)" fill="#FFFFFF">
    <path d="M892.99993 611.72969 970.85741 611.77319 929.89641 688.37305Z"/>
    <path d="M794.99333 672 929.08802 671.54855 861.16069 803Z"/>
    <path d="M768.29222 932.79238 669 732.66606 867.96047 731.35091 770.04312 933.29238Z"/>
    <path d="M28.897776 715.5 346.90236 65.5 560.01789 64.99095 134.1887 928.20241Z"/>
    <path d="M458 289.69993 563.68924 73.231804 771.26447 497.72405 662.00331 710Z"/>
    <path d="M254 710.5329 358.22025 498H546.80163L650 710.63348Z"/>
  </g>'''
    with open(os.path.join(assets_dir, "levyra-apkpure.svg"), "w", encoding="utf-8", newline="\n") as f:
        f.write(make_secondary_card("GET IT ON", "APKPure", apkpure_icon, "#16A34A", "15"))

    # 8. APKFab (Purple #7C3AED)
    apkfab_icon = '''<path fill="#FFFFFF" transform="translate(4 3) scale(0.85)" d="M20.5 14.6c.8-2.5 2.4-3.6 5.5-3.6s4.7 1.1 5.5 3.6l6 18.6c.6 1.8.2 3.2-1 4.2-1.1.9-2.7 1.2-4.8 1.1-2.4-.1-3.4-1-3.6-3.5l-.3-3.2h-3.6l-.3 3.2c-.2 2.5-1.2 3.4-3.6 3.5-2.1.1-3.7-.2-4.8-1.1-1.2-1-1.6-2.4-1-4.2l6-18.6z"/>
  <path fill="#7C3AED" transform="translate(4 3) scale(0.85)" d="M26 20.2c-.2 2.3.5 3.6 1.9 5 1.2 1.2 1.8 2.3 1.8 3.8 0 2.2-1.7 4-3.8 4-2.2 0-3.9-1.8-3.9-4 0-1.7.8-3 2-4.1-.1 1.6.7 2.5 1.6 2.5.8 0 1.3-.7 1.3-1.7 0-1.2-.9-2.3-.9-3.8 0-.6 0-1.1 0-1.7z"/>'''
    with open(os.path.join(assets_dir, "levyra-apkfab.svg"), "w", encoding="utf-8", newline="\n") as f:
        f.write(make_secondary_card("GET IT ON", "APKFab", apkfab_icon, "#7C3AED", "15"))

    # 9. AndroidFreeware (Teal / Green #0D9488)
    af_img = extract_base64_image(os.path.join(assets_dir, "levyra-androidfreeware-download.svg"))
    af_icon = f'<image x="14" y="13" width="24" height="24" preserveAspectRatio="xMidYMid meet" href="{af_img}"/>'
    badge_af = make_secondary_card("GET IT ON", "AndroidFreeware", af_icon, "#0D9488", "11.2")
    for f_name in [
        "levyra-androidfreeware-download.svg",
        "levyra-androidfreeware-download-desktop184.svg",
        "levyra-androidfreeware-download-light.svg",
        "levyra-androidfreeware-download-desktop184-light.svg"
    ]:
        with open(os.path.join(assets_dir, f_name), "w", encoding="utf-8", newline="\n") as f:
            f.write(badge_af)

    # 10. Obtainium (Cyan / Teal #0284C7)
    obtainium_img = extract_base64_image(os.path.join(assets_dir, "levyra-obtainium-download.svg"))
    obtainium_icon = f'<image x="14" y="13" width="24" height="24" preserveAspectRatio="xMidYMid meet" href="{obtainium_img}"/>'
    badge_obtainium = make_secondary_card("INSTALL WITH", "Obtainium", obtainium_icon, "#0284C7", "14")
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
    appteka_icon = f'<image x="14" y="13" width="24" height="24" preserveAspectRatio="xMidYMid meet" href="{appteka_img}"/>'
    badge_appteka = make_secondary_card("GET IT ON", "Appteka", appteka_icon, "#E11D48", "15")
    for f_name in [
        "levyra-appteka.svg",
        "levyra-appteka-desktop184.svg",
        "levyra-appteka-light.svg",
        "levyra-appteka-desktop184-light.svg"
    ]:
        with open(os.path.join(assets_dir, f_name), "w", encoding="utf-8", newline="\n") as f:
            f.write(badge_appteka)

    print("Generated white M3 Expressive download badges.")

if __name__ == "__main__":
    main()
