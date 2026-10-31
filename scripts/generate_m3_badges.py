#!/usr/bin/env python3
"""
Generate Material 3 Expressive download badges for Levyra.
Applies:
- Squircle container rx=16, height=50
- Tonal elevation gradient (#161924 -> #0E1119) with subtle 1.2px outline and inner specular highlight rim
- Rounded squircle brand icon tiles (rx=11, 36x36)
- Material 3 download action FAB chip on the right for primary download options
- M3 typography: Label Small (font-size 7.5, letter-spacing 0.8) + Title Medium (font-size 15, font-weight 750)
"""

import os
import re

def extract_base64_image(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()
    match = re.search(r'href="(data:image/png;base64,[^"]+)"', content)
    if not match:
        raise ValueError(f"Could not find base64 image in {file_path}")
    return match.group(1)

def make_primary_badge(label, brand, icon_svg, brand_font_size="15", is_light=False):
    title = f"{label} {brand}"
    grad_id = f"dl_grad_{brand.lower().replace(' ', '_').replace('-', '_')}_{'light' if is_light else 'dark'}"
    bg_top = "#FFFFFF" if is_light else "#161924"
    bg_bot = "#F1F4F9" if is_light else "#0E1119"
    border = "rgba(0, 0, 0, 0.10)" if is_light else "rgba(255, 255, 255, 0.12)"
    inner_stroke = "rgba(255, 255, 255, 0.70)" if is_light else "rgba(255, 255, 255, 0.05)"
    text_sub = "#64748B" if is_light else "#94A3B8"
    text_main = "#0F172A" if is_light else "#FFFFFF"
    chip_bg = "rgba(0, 0, 0, 0.04)" if is_light else "rgba(255, 255, 255, 0.06)"
    chip_border = "rgba(0, 0, 0, 0.08)" if is_light else "rgba(255, 255, 255, 0.12)"
    arrow_stroke = "#0F172A" if is_light else "#F1F5F9"
    bar_stroke = "#64748B" if is_light else "#94A3B8"

    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="184" height="57.5" viewBox="0 0 160 50" role="img" aria-label="{title}">
  <title>{title}</title>
  <defs>
    <linearGradient id="{grad_id}" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="{bg_top}"/>
      <stop offset="100%" stop-color="{bg_bot}"/>
    </linearGradient>
  </defs>
  <rect x=".75" y=".75" width="158.5" height="48.5" rx="16" fill="url(#{grad_id})" stroke="{border}" stroke-width="1.2"/>
  <rect x="1.75" y="1.75" width="156.5" height="46.5" rx="15" fill="none" stroke="{inner_stroke}" stroke-width="1"/>
  {icon_svg}
  <text x="49" y="19" fill="{text_sub}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="7.5" font-weight="700" letter-spacing=".8">{label}</text>
  <text x="49" y="36.5" fill="{text_main}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="{brand_font_size}" font-weight="750">{brand}</text>
  <circle cx="140" cy="25" r="13.5" fill="{chip_bg}" stroke="{chip_border}" stroke-width="1"/>
  <path d="M140 17v10.5m0 0l-3.5-3.5m3.5 3.5l3.5-3.5" fill="none" stroke="{arrow_stroke}" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
  <path d="M135 31.5h10" fill="none" stroke="{bar_stroke}" stroke-width="1.8" stroke-linecap="round"/>
</svg>"""

def make_secondary_badge(label, brand, icon_svg, brand_font_size="15", is_light=False):
    title = f"{label} {brand}"
    grad_id = f"dl_grad_sec_{brand.lower().replace(' ', '_').replace('-', '_')}_{'light' if is_light else 'dark'}"
    bg_top = "#FFFFFF" if is_light else "#161924"
    bg_bot = "#F1F4F9" if is_light else "#0E1119"
    border = "rgba(0, 0, 0, 0.10)" if is_light else "rgba(255, 255, 255, 0.12)"
    inner_stroke = "rgba(255, 255, 255, 0.70)" if is_light else "rgba(255, 255, 255, 0.05)"
    text_sub = "#64748B" if is_light else "#94A3B8"
    text_main = "#0F172A" if is_light else "#FFFFFF"

    return f"""<svg xmlns="http://www.w3.org/2000/svg" width="184" height="57.5" viewBox="0 0 160 50" role="img" aria-label="{title}">
  <title>{title}</title>
  <defs>
    <linearGradient id="{grad_id}" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" stop-color="{bg_top}"/>
      <stop offset="100%" stop-color="{bg_bot}"/>
    </linearGradient>
  </defs>
  <rect x=".75" y=".75" width="158.5" height="48.5" rx="16" fill="url(#{grad_id})" stroke="{border}" stroke-width="1.2"/>
  <rect x="1.75" y="1.75" width="156.5" height="46.5" rx="15" fill="none" stroke="{inner_stroke}" stroke-width="1"/>
  {icon_svg}
  <text x="49" y="19" fill="{text_sub}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="7.5" font-weight="700" letter-spacing=".8">{label}</text>
  <text x="49" y="36.5" fill="{text_main}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="{brand_font_size}" font-weight="750">{brand}</text>
</svg>"""

def main():
    assets_dir = "docs/assets"

    # 1. GitHub
    github_icon = '''<rect x="8" y="7" width="36" height="36" rx="11" fill="#F0F3F6" stroke="rgba(255, 255, 255, 0.2)" stroke-width="1"/>
  <g transform="translate(11 10) scale(1.68)"><path fill="#0D1117" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"/></g>'''
    for path, is_light in [
        ("levyra-github-download.svg", False),
        ("levyra-github-download-desktop184.svg", False),
        ("levyra-github-download-light.svg", True),
        ("levyra-github-download-desktop184-light.svg", True)
    ]:
        with open(os.path.join(assets_dir, path), "w", encoding="utf-8", newline="\n") as f:
            f.write(make_primary_badge("DOWNLOAD FROM", "GitHub", github_icon, "15", is_light))

    # 2. F-Droid
    fdroid_img = extract_base64_image(os.path.join(assets_dir, "levyra-fdroid.svg"))
    fdroid_icon = f'''<rect x="8" y="7" width="36" height="36" rx="11" fill="#151E2E" stroke="rgba(56, 189, 248, 0.25)" stroke-width="1"/>
  <image x="10" y="9" width="32" height="32" preserveAspectRatio="xMidYMid meet" href="{fdroid_img}"/>'''
    for path, is_light in [
        ("levyra-fdroid.svg", False),
        ("levyra-fdroid-desktop184.svg", False),
        ("levyra-fdroid-light.svg", True),
        ("levyra-fdroid-desktop184-light.svg", True)
    ]:
        with open(os.path.join(assets_dir, path), "w", encoding="utf-8", newline="\n") as f:
            f.write(make_primary_badge("GET IT ON", "F-Droid", fdroid_icon, "15", is_light))

    # 3. OpenAPK
    openapk_icon = '''<rect x="8" y="7" width="36" height="36" rx="11" fill="#161B24" stroke="rgba(255, 255, 255, 0.15)" stroke-width="1"/>
  <g transform="translate(10.3 8.8) scale(0.041)"><path fill="#FFFFFF" d="M742.4,418.6c-0.2-4.1-0.5-8.1-0.8-12.2c-12.9-111.3-79-205.3-171.9-256.7l-1.7-0.9l0,0l9.2-15.9l47.9-82.9c1-1.7,1.6-3.7,1.6-5.9c0-6.6-5.3-11.9-11.9-11.9c-4.4,0-8.2,2.4-10.2,5.9l0,0l-47.9,82.9l-9.9,17.1c-43.1-19.8-93.6-31.4-146.8-31.4c-52.3,0-102,11.2-146.8,31.3l0,0.1l-2.2,0.9c0.7-0.3,1.5-0.7,2.2-1l-9.8-17.1l-47.9-83c-2.1-3.6-5.9-5.9-10.3-5.9c-6.6,0-11.9,5.3-11.9,11.9c0,2.2,0.6,4.2,1.6,6l0,0l47.9,83l9.2,15.9C137.6,201,71.5,294.8,58.5,404.8c-0.4,4.6-0.7,9.2-0.9,13.8c-0.3,5.8-0.4,11.7-0.4,17.6c0,151.8,92.5,281.2,222.5,331.6l79-217.6c-44.7-17.3-76.5-61.8-76.5-114c0-6,0.4-11.9,1.2-17.6c8.3-58.9,57.3-104.1,116.6-104.1c59.3,0,108.4,45.2,116.6,104.1c0.8,5.8,1.2,11.6,1.2,17.6c0,52.2-31.8,96.7-76.5,114l79,217.6c130-50.4,222.5-179.8,222.5-331.6C742.9,430.3,742.7,424.4,742.4,418.6z M242.1,263.8c15.8,0,28.6,12.8,28.6,28.6c0,15.8-12.8,28.5-28.6,28.6c-15.8,0-28.6-12.8-28.6-28.6C213.6,276.6,226.4,263.8,242.1,263.8z M557.8,263.8c15.8,0,28.6,12.8,28.6,28.6c0,15.8-12.8,28.5-28.6,28.6c-15.8,0-28.6-12.8-28.6-28.6C529.2,276.6,542,263.8,557.8,263.8z"/></g>'''
    for path, is_light in [
        ("levyra-openapk.svg", False),
        ("levyra-openapk-desktop184.svg", False),
        ("levyra-openapk-light.svg", True),
        ("levyra-openapk-desktop184-light.svg", True)
    ]:
        with open(os.path.join(assets_dir, path), "w", encoding="utf-8", newline="\n") as f:
            f.write(make_primary_badge("GET IT ON", "OpenAPK", openapk_icon, "15", is_light))

    # 4. Windows
    windows_icon = '''<rect x="8" y="7" width="36" height="36" rx="11" fill="#141E2C" stroke="rgba(0, 120, 212, 0.35)" stroke-width="1"/>
  <g transform="translate(13.5 12.5) scale(0.48)" fill="#0078D4"><rect x="0" y="0" width="24" height="24" rx="2.5"/><rect x="28" y="0" width="24" height="24" rx="2.5"/><rect x="0" y="28" width="24" height="24" rx="2.5"/><rect x="28" y="28" width="24" height="24" rx="2.5"/></g>'''
    for path, is_light in [
        ("levyra-windows-download.svg", False),
        ("levyra-windows-download-desktop184.svg", False),
        ("levyra-windows-download-light.svg", True),
        ("levyra-windows-download-desktop184-light.svg", True)
    ]:
        with open(os.path.join(assets_dir, path), "w", encoding="utf-8", newline="\n") as f:
            f.write(make_primary_badge("DOWNLOAD FOR", "Windows", windows_icon, "15", is_light))

    # 5. IzzyOnDroid
    izzy_img = extract_base64_image(os.path.join(assets_dir, "levyra-izzyondroid.svg"))
    izzy_icon = f'''<rect x="8" y="7" width="36" height="36" rx="11" fill="#151E2E" stroke="rgba(255, 255, 255, 0.15)" stroke-width="1"/>
  <image x="10" y="9" width="32" height="32" preserveAspectRatio="xMidYMid meet" href="{izzy_img}"/>'''
    for path, is_light in [
        ("levyra-izzyondroid.svg", False),
        ("levyra-izzyondroid-desktop184.svg", False),
        ("levyra-izzyondroid-light.svg", True),
        ("levyra-izzyondroid-desktop184-light.svg", True)
    ]:
        with open(os.path.join(assets_dir, path), "w", encoding="utf-8", newline="\n") as f:
            f.write(make_secondary_badge("GET IT ON", "IzzyOnDroid", izzy_icon, "13.5", is_light))

    # 6. SourceForge
    sf_icon = '''<rect x="8" y="7" width="36" height="36" rx="11" fill="#1C181E" stroke="rgba(255, 102, 0, 0.3)" stroke-width="1"/>
  <g transform="translate(14 12) scale(1.3)"><path fill="#FF6600" d="M9.998 1.4648c-.088 0-.1761.0395-.2402.1036L.1016 11.2148c-.1361.1281-.1361.3444 0 .4805l9.1523 9.1348c.064.072.1522.1054.2402.1054.0881 0 .1743-.0414.2383-.1054l1.3692-1.3848c.136-.128.136-.3444 0-.4805l-7.172-7.1738a.4682.4682 0 010-.664l9.0782-9.0782c.1201-.12.1123-.2728.0723-.377-.04-.096-.1444-.207-.3125-.207H9.998zm4.5313 1.5918c-.088 0-.1762.0415-.2402.1055l-1.3926 1.3926c-.1361.128-.1361.3444 0 .4805l7.1719 7.1816c.088.088.1367.208.1367.3281 0 .1201-.0487.24-.1367.3281l-9.0782 9.0782c-.12.12-.1123.2728-.0722.377.04.096.1443.207.3125.207v-.0079H14c.088 0 .1762-.0316.2402-.0957l9.6563-9.6543c.072-.064.1035-.1521.1035-.2402 0-.088-.0395-.1762-.1035-.2402l-9.127-9.1348c-.064-.072-.1521-.1055-.2402-.1055zM11.4336 6.213c-.1125.005-.2164.1007-.211.2383.1442 2.2176-2.6425 2.7635-2.6425 6.246v.0235c0 2.1216 1.6026 3.8418 3.58 3.8418 1.9775 0 3.5782-1.7202 3.5782-3.8418v-.0234c0-.9848-.3679-1.9225-.7442-2.627-.072-.136-.2896-.087-.2656.041.6725 2.9943-1.0156 4.8507-1.0156 2.545 0-3.931-1.3943-5.7162-2.1309-6.3887a.2072.2072 0 00-.1484-.0547Z"/></g>'''
    for path, is_light in [
        ("levyra-sourceforge.svg", False),
        ("levyra-sourceforge-desktop184.svg", False),
        ("levyra-sourceforge-light.svg", True),
        ("levyra-sourceforge-desktop184-light.svg", True)
    ]:
        with open(os.path.join(assets_dir, path), "w", encoding="utf-8", newline="\n") as f:
            f.write(make_secondary_badge("DOWNLOAD FROM", "SourceForge", sf_icon, "14.5", is_light))

    # 7. APKPure
    apkpure_icon = '''<rect x="8" y="7" width="36" height="36" rx="11" fill="#24CD77" stroke="rgba(36, 205, 119, 0.4)" stroke-width="1"/>
  <g transform="translate(10.5 9.5) scale(.028)" fill="#FFFFFF">
    <path d="M892.99993 611.72969 970.85741 611.77319 929.89641 688.37305Z"/>
    <path d="M794.99333 672 929.08802 671.54855 861.16069 803Z"/>
    <path d="M768.29222 932.79238 669 732.66606 867.96047 731.35091 770.04312 933.29238Z"/>
    <path d="M28.897776 715.5 346.90236 65.5 560.01789 64.99095 134.1887 928.20241Z"/>
    <path d="M458 289.69993 563.68924 73.231804 771.26447 497.72405 662.00331 710Z"/>
    <path d="M254 710.5329 358.22025 498H546.80163L650 710.63348Z"/>
  </g>'''
    with open(os.path.join(assets_dir, "levyra-apkpure.svg"), "w", encoding="utf-8", newline="\n") as f:
        f.write(make_secondary_badge("GET IT ON", "APKPure", apkpure_icon, "15", False))

    # 8. APKFab
    apkfab_icon = '''<defs>
    <linearGradient id="apkfab_tile_grad" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="#8B2CF5"/>
      <stop offset="100%" stop-color="#4B3CFF"/>
    </linearGradient>
  </defs>
  <rect x="8" y="7" width="36" height="36" rx="11" fill="url(#apkfab_tile_grad)" stroke="rgba(139, 44, 245, 0.35)" stroke-width="1"/>
  <path fill="#FFFFFF" transform="translate(0 -0.5)" d="M20.5 14.6c.8-2.5 2.4-3.6 5.5-3.6s4.7 1.1 5.5 3.6l6 18.6c.6 1.8.2 3.2-1 4.2-1.1.9-2.7 1.2-4.8 1.1-2.4-.1-3.4-1-3.6-3.5l-.3-3.2h-3.6l-.3 3.2c-.2 2.5-1.2 3.4-3.6 3.5-2.1.1-3.7-.2-4.8-1.1-1.2-1-1.6-2.4-1-4.2l6-18.6z"/>
  <path fill="url(#apkfab_tile_grad)" transform="translate(0 -0.5)" d="M26 20.2c-.2 2.3.5 3.6 1.9 5 1.2 1.2 1.8 2.3 1.8 3.8 0 2.2-1.7 4-3.8 4-2.2 0-3.9-1.8-3.9-4 0-1.7.8-3 2-4.1-.1 1.6.7 2.5 1.6 2.5.8 0 1.3-.7 1.3-1.7 0-1.2-.9-2.3-.9-3.8 0-.6 0-1.1 0-1.7z"/>'''
    with open(os.path.join(assets_dir, "levyra-apkfab.svg"), "w", encoding="utf-8", newline="\n") as f:
        f.write(make_secondary_badge("GET IT ON", "APKFab", apkfab_icon, "15", False))

    # 9. AndroidFreeware
    af_img = extract_base64_image(os.path.join(assets_dir, "levyra-androidfreeware-download.svg"))
    af_icon = f'''<rect x="8" y="7" width="36" height="36" rx="11" fill="#151E2E" stroke="rgba(255, 255, 255, 0.15)" stroke-width="1"/>
  <image x="10" y="9" width="32" height="32" preserveAspectRatio="xMidYMid meet" href="{af_img}"/>'''
    for path, is_light in [
        ("levyra-androidfreeware-download.svg", False),
        ("levyra-androidfreeware-download-desktop184.svg", False),
        ("levyra-androidfreeware-download-light.svg", True),
        ("levyra-androidfreeware-download-desktop184-light.svg", True)
    ]:
        with open(os.path.join(assets_dir, path), "w", encoding="utf-8", newline="\n") as f:
            f.write(make_secondary_badge("GET IT ON", "AndroidFreeware", af_icon, "11.2", is_light))

    # 10. Obtainium
    obtainium_img = extract_base64_image(os.path.join(assets_dir, "levyra-obtainium-download.svg"))
    obtainium_icon = f'''<rect x="8" y="7" width="36" height="36" rx="11" fill="#151E2E" stroke="rgba(255, 255, 255, 0.15)" stroke-width="1"/>
  <image x="10" y="9" width="32" height="32" preserveAspectRatio="xMidYMid meet" href="{obtainium_img}"/>'''
    for path, is_light in [
        ("levyra-obtainium-download.svg", False),
        ("levyra-obtainium-download-desktop184.svg", False),
        ("levyra-obtainium-download-light.svg", True),
        ("levyra-obtainium-download-desktop184-light.svg", True)
    ]:
        with open(os.path.join(assets_dir, path), "w", encoding="utf-8", newline="\n") as f:
            f.write(make_secondary_badge("INSTALL WITH", "Obtainium", obtainium_icon, "14", is_light))

    # 11. Appteka
    appteka_img = extract_base64_image(os.path.join(assets_dir, "levyra-appteka.svg"))
    appteka_icon = f'''<rect x="8" y="7" width="36" height="36" rx="11" fill="#151E2E" stroke="rgba(255, 255, 255, 0.15)" stroke-width="1"/>
  <image x="10" y="9" width="32" height="32" preserveAspectRatio="xMidYMid meet" href="{appteka_img}"/>'''
    for path, is_light in [
        ("levyra-appteka.svg", False),
        ("levyra-appteka-desktop184.svg", False),
        ("levyra-appteka-light.svg", True),
        ("levyra-appteka-desktop184-light.svg", True)
    ]:
        with open(os.path.join(assets_dir, path), "w", encoding="utf-8", newline="\n") as f:
            f.write(make_secondary_badge("GET IT ON", "Appteka", appteka_icon, "15", is_light))

    print("All Material 3 Expressive download badges generated successfully.")

if __name__ == "__main__":
    main()
