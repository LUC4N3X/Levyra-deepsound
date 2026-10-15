from html import escape
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "assets"

GROUPS = [
    ("LATIN", 25, "English · Español · Italiano · Français · Deutsch · Português"),
    ("CYRILLIC + GREEK", 6, "Ελληνικά · Български · Қазақша · Русский · Српски · Українська"),
    ("CJK", 4, "한국어 · 日本語 · 简体中文 · 繁體中文"),
    ("ARABIC + HEBREW", 3, "עברית · العربية · فارسی"),
    ("INDIC + THAI", 3, "हिन्दी · বাংলা · ไทย"),
]

ALL_LANGUAGES = (
    "Bahasa Indonesia, Bahasa Melayu, Català, Čeština, Dansk, Deutsch, Eesti, English, "
    "Español, Filipino, Français, Hrvatski, Italiano, Magyar, Nederlands, Norsk bokmål, "
    "Polski, Português, Română, Shqip, Slovenčina, Suomi, Svenska, Tiếng Việt, Türkçe, "
    "Ελληνικά, Български, Қазақша, Русский, Српски, Українська, עברית, العربية, فارسی, "
    "한국어, 日本語, 简体中文, 繁體中文, हिन्दी, বাংলা, ไทย"
)

THEMES = {
    "dark": {
        "background": "#0D1117",
        "panel": "#151019",
        "ink": "#F7F1F5",
        "muted": "#A99DA6",
        "rule": "#302732",
        "accent": "#E7A7CF",
        "accent_soft": "#52364A",
        "quiet": "#251E27",
    },
    "light": {
        "background": "#FBF8F6",
        "panel": "#F4E9EF",
        "ink": "#241B21",
        "muted": "#70636B",
        "rule": "#DDD0D7",
        "accent": "#8B416E",
        "accent_soft": "#E7C7D9",
        "quiet": "#EDE3E8",
    },
}


def marker_row(x, y, count, available, color, quiet):
    gap = 6
    width = min(17, (available - gap * (count - 1)) / count)
    blocks = []
    for index in range(count):
        height = (14, 24, 18, 30, 20)[index % 5]
        blocks.append(
            f'<rect x="{x + index * (width + gap):.1f}" y="{y - height:.1f}" '
            f'width="{width:.1f}" height="{height}" rx="{min(4, width / 2):.1f}" '
            f'fill="{color if index % 4 else quiet}"/>'
        )
    return "".join(blocks)


def render_desktop(theme_name):
    t = THEMES[theme_name]
    rows = []
    for index, (label, count, examples) in enumerate(GROUPS):
        y = 104 + index * 68
        rows.append(
            f'<line x1="390" y1="{y + 31}" x2="1044" y2="{y + 31}" stroke="{t["rule"]}"/>'
            f'<text x="390" y="{y}" class="group">{escape(label)}</text>'
            f'<text x="1038" y="{y}" text-anchor="end" class="count">{count:02d}</text>'
            f'<text x="390" y="{y + 22}" class="examples">{escape(examples)}</text>'
            f'<g>{marker_row(790, y + 26, count, 248, t["accent"], t["accent_soft"])}</g>'
        )
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="1100" height="480" viewBox="0 0 1100 480" role="img" aria-label="Levyra is fully translated in 41 languages">
  <title>Levyra speaks 41 languages with 100% translation coverage</title>
  <desc>{escape(ALL_LANGUAGES)}</desc>
  <style>
    text {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }}
    .kicker {{ fill: {t["accent"]}; font-size: 12px; font-weight: 800; letter-spacing: 2.2px; }}
    .hero {{ fill: {t["ink"]}; font-size: 132px; font-weight: 850; letter-spacing: -8px; }}
    .hero-label {{ fill: {t["ink"]}; font-size: 38px; font-weight: 760; letter-spacing: -1.5px; }}
    .meta {{ fill: {t["muted"]}; font-size: 14px; }}
    .statement {{ fill: {t["ink"]}; font-size: 30px; font-weight: 800; letter-spacing: -1px; }}
    .statement-small {{ fill: {t["muted"]}; font-size: 12px; font-weight: 700; letter-spacing: 1.4px; }}
    .group {{ fill: {t["ink"]}; font-size: 13px; font-weight: 800; letter-spacing: .8px; }}
    .count {{ fill: {t["accent"]}; font-size: 24px; font-weight: 850; }}
    .examples {{ fill: {t["muted"]}; font-size: 11px; }}
    .footer {{ fill: {t["muted"]}; font-size: 11px; font-weight: 650; letter-spacing: .5px; }}
  </style>
  <rect width="1100" height="480" rx="22" fill="{t["background"]}"/>
  <path d="M0 0H328C360 78 352 166 310 236C268 306 270 390 334 480H0Z" fill="{t["panel"]}"/>
  <text x="48" y="58" class="kicker">LEVYRA / LOCAL EVERYWHERE</text>
  <text x="42" y="190" class="hero">41</text>
  <text x="49" y="232" class="hero-label">languages.</text>
  <text x="50" y="269" class="meta">One interface. Five writing systems.</text>
  <text x="50" y="290" class="meta">Every app string, complete.</text>
  <line x1="50" y1="339" x2="302" y2="339" stroke="{t["rule"]}"/>
  <text x="50" y="375" class="statement">100%</text>
  <text x="147" y="368" class="statement-small">TRANSLATION</text>
  <text x="147" y="384" class="statement-small">COVERAGE</text>
  <text x="390" y="49" class="kicker">THE LANGUAGE SCORE</text>
  <text x="1038" y="49" text-anchor="end" class="footer">1,105+ APP STRINGS</text>
  {''.join(rows)}
  <text x="390" y="446" class="footer">41 / 41 LOCALE PACKS COMPLETE</text>
  <text x="1038" y="446" text-anchor="end" class="footer">BUILT INTO LEVYRA · NO CLOUD PROFILE</text>
</svg>
'''


def render_mobile(theme_name):
    t = THEMES[theme_name]
    rows = []
    for index, (label, count, examples) in enumerate(GROUPS):
        y = 272 + index * 63
        rows.append(
            f'<text x="42" y="{y}" class="group">{escape(label)}</text>'
            f'<text x="678" y="{y}" text-anchor="end" class="count">{count:02d}</text>'
            f'<text x="42" y="{y + 19}" class="examples">{escape(examples)}</text>'
            f'<g>{marker_row(458, y + 24, count, 220, t["accent"], t["accent_soft"])}</g>'
            f'<line x1="42" y1="{y + 36}" x2="678" y2="{y + 36}" stroke="{t["rule"]}"/>'
        )
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="720" height="620" viewBox="0 0 720 620" role="img" aria-label="Levyra is fully translated in 41 languages">
  <title>Levyra speaks 41 languages with 100% translation coverage</title>
  <desc>{escape(ALL_LANGUAGES)}</desc>
  <style>
    text {{ font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; }}
    .kicker {{ fill: {t["accent"]}; font-size: 11px; font-weight: 800; letter-spacing: 2px; }}
    .hero {{ fill: {t["ink"]}; font-size: 112px; font-weight: 850; letter-spacing: -7px; }}
    .hero-label {{ fill: {t["ink"]}; font-size: 34px; font-weight: 760; letter-spacing: -1.2px; }}
    .meta {{ fill: {t["muted"]}; font-size: 13px; }}
    .statement {{ fill: {t["ink"]}; font-size: 28px; font-weight: 850; }}
    .statement-small {{ fill: {t["muted"]}; font-size: 10px; font-weight: 750; letter-spacing: 1.2px; }}
    .group {{ fill: {t["ink"]}; font-size: 12px; font-weight: 800; letter-spacing: .7px; }}
    .count {{ fill: {t["accent"]}; font-size: 20px; font-weight: 850; }}
    .examples {{ fill: {t["muted"]}; font-size: 10px; }}
    .footer {{ fill: {t["muted"]}; font-size: 10px; font-weight: 650; letter-spacing: .4px; }}
  </style>
  <rect width="720" height="620" rx="20" fill="{t["background"]}"/>
  <path d="M0 0H308C332 64 322 133 285 190C265 220 256 238 258 250H0Z" fill="{t["panel"]}"/>
  <text x="42" y="43" class="kicker">LEVYRA / LOCAL EVERYWHERE</text>
  <text x="37" y="151" class="hero">41</text>
  <text x="43" y="188" class="hero-label">languages.</text>
  <text x="43" y="218" class="meta">Five writing systems. Every string complete.</text>
  <text x="676" y="72" text-anchor="end" class="statement">100%</text>
  <text x="676" y="91" text-anchor="end" class="statement-small">TRANSLATION COVERAGE</text>
  {''.join(rows)}
  <text x="42" y="590" class="footer">41 / 41 LOCALE PACKS COMPLETE</text>
  <text x="678" y="590" text-anchor="end" class="footer">1,105+ APP STRINGS</text>
</svg>
'''


def main():
    outputs = {
        "levyra-translation-pulse.svg": render_desktop("dark"),
        "levyra-translation-pulse-light.svg": render_desktop("light"),
        "levyra-translation-pulse-mobile.svg": render_mobile("dark"),
        "levyra-translation-pulse-mobile-light.svg": render_mobile("light"),
    }
    for filename, content in outputs.items():
        path = OUT / filename
        path.write_text(content, encoding="utf-8")
        print(f"Generated {path.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
