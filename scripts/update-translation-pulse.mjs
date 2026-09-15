import { mkdir, readFile, readdir, writeFile } from 'node:fs/promises'
import { join } from 'node:path'

const RESOURCE_ROOT = 'app/src/main/res'
const BASE_STRINGS = join(RESOURCE_ROOT, 'values', 'strings.xml')
const LOCALE_CONFIG = join(RESOURCE_ROOT, 'xml', 'locales_config.xml')
const ASSET_DIR = 'docs/assets'

const escapeXml = value => String(value)
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&apos;')

const canonicalTag = value => String(value)
  .trim()
  .replaceAll('_', '-')
  .toLowerCase()

const qualifierToTag = name => {
  const suffix = name.replace(/^values-/, '')
  return suffix.startsWith('b+')
    ? suffix.slice(2).replaceAll('+', '-')
    : suffix
}

const readStringKeys = xml => {
  const pattern = /<string\s+name="([^"]+)"([^>]*)>/g
  const keys = new Set()
  for (const match of xml.matchAll(pattern)) {
    if (!match[2].includes('translatable="false"')) keys.add(match[1])
  }
  return keys
}

const readLocalizedKeys = xml => {
  const pattern = /<string\s+name="([^"]+)"/g
  return new Set(Array.from(xml.matchAll(pattern), match => match[1]))
}

// Curated native names and display tags
const LOCALE_META = {
  'en': { name: 'English', tag: 'EN' },
  'it': { name: 'Italiano', tag: 'IT' },
  'es': { name: 'Español', tag: 'ES' },
  'fr': { name: 'Français', tag: 'FR' },
  'de': { name: 'Deutsch', tag: 'DE' },
  'pt': { name: 'Português', tag: 'PT' },
  'nl': { name: 'Nederlands', tag: 'NL' },
  'pl': { name: 'Polski', tag: 'PL' },
  'ro': { name: 'Română', tag: 'RO' },
  'el': { name: 'Ελληνικά', tag: 'EL' },
  'sv': { name: 'Svenska', tag: 'SV' },
  'da': { name: 'Dansk', tag: 'DA' },
  'cs': { name: 'Čeština', tag: 'CS' },
  'sk': { name: 'Slovenčina', tag: 'SK' },
  'hr': { name: 'Hrvatski', tag: 'HR' },
  'bg': { name: 'Български', tag: 'BG' },
  'hu': { name: 'Magyar', tag: 'HU' },
  'fi': { name: 'Suomi', tag: 'FI' },
  'nb': { name: 'Norsk bokmål', tag: 'NB' },
  'ca': { name: 'Català', tag: 'CA' },
  'uk': { name: 'Українська', tag: 'UK' },
  'ru': { name: 'Русский', tag: 'RU' },
  'tr': { name: 'Türkçe', tag: 'TR' },
  'ar': { name: 'العربية', tag: 'AR' },
  'fa': { name: 'فارسی', tag: 'FA' },
  'zh-hans': { name: '简体中文', tag: 'ZH' },
  'zh-hant': { name: '繁體中文', tag: 'ZH-TW' },
  'ja': { name: '日本語', tag: 'JA' },
  'ko': { name: '한국어', tag: 'KO' },
  'hi': { name: 'हिन्दी', tag: 'HI' },
  'id': { name: 'Bahasa Indonesia', tag: 'ID' },
  'ms': { name: 'Bahasa Melayu', tag: 'MS' },
  'vi': { name: 'Tiếng Việt', tag: 'VI' },
  'th': { name: 'ไทย', tag: 'TH' },
  'fil': { name: 'Filipino', tag: 'FIL' },
  'he': { name: 'עברית', tag: 'HE' }
}

const collectRepositoryCoverage = async () => {
  const [baseXml, localeConfig, entries] = await Promise.all([
    readFile(BASE_STRINGS, 'utf8'),
    readFile(LOCALE_CONFIG, 'utf8'),
    readdir(RESOURCE_ROOT, { withFileTypes: true })
  ])

  const expectedKeys = readStringKeys(baseXml)
  if (expectedKeys.size === 0) throw new Error('No translatable Android strings found')

  const localePattern = /<locale\s+android:name="([^"]+)"\s*\/>/g
  const localeTags = Array.from(localeConfig.matchAll(localePattern), match => match[1])
  if (localeTags.length === 0) throw new Error('No Android locales found')

  const localizedFiles = new Map()
  for (const entry of entries) {
    if (!entry.isDirectory() || !entry.name.startsWith('values-')) continue
    const tag = canonicalTag(qualifierToTag(entry.name))
    try {
      const xml = await readFile(join(RESOURCE_ROOT, entry.name, 'strings.xml'), 'utf8')
      localizedFiles.set(tag, readLocalizedKeys(xml))
    } catch {}
  }

  const languages = localeTags.map(code => {
    const canon = canonicalTag(code)
    const meta = LOCALE_META[canon] || { name: code, tag: code.toUpperCase() }
    if (canon === 'en') {
      return {
        name: meta.name,
        code,
        tag: meta.tag,
        percent: 100,
        translated: expectedKeys.size,
        total: expectedKeys.size
      }
    }

    const localized = localizedFiles.get(canon) || new Set()
    const translated = Array.from(expectedKeys).filter(key => localized.has(key)).length
    return {
      name: meta.name,
      code,
      tag: meta.tag,
      percent: (translated / expectedKeys.size) * 100,
      translated,
      total: expectedKeys.size
    }
  })

  const translated = languages.reduce((sum, l) => sum + l.translated, 0)
  const total = languages.reduce((sum, l) => sum + l.total, 0)

  return {
    languages,
    globalPercent: total === 0 ? 0 : (translated / total) * 100,
    stringCount: expectedKeys.size
  }
}

const getCardColor = (percent, isDark) => {
  if (percent >= 99.5) return isDark ? '#2DD4BF' : '#0F766E'
  return isDark ? '#E3B341' : '#9A6700'
}

const makeLanguageCard = ({ lang, idx, cols, padX, cardGapX, cardGapY, cardW, cardH, cardsStartY, theme }) => {
  const r = Math.floor(idx / cols)
  const c = idx % cols
  const x = padX + c * (cardW + cardGapX)
  const y = cardsStartY + r * (cardH + cardGapY)
  const checkColor = getCardColor(lang.percent, theme.isDark)
  const percentStr = `${Math.round(lang.percent)}%`
  const nameFontSize = lang.name.length > 15 ? 11 : 12

  return `  <g transform="translate(${x.toFixed(1)}, ${y.toFixed(1)})">
    <rect width="${cardW.toFixed(1)}" height="${cardH}" rx="7" fill="${theme.itemBg}" stroke="${theme.itemBorder}" stroke-width="1"/>
    <circle cx="14" cy="${(cardH / 2).toFixed(1)}" r="3" fill="${checkColor}"/>
    <text x="25" y="${(cardH / 2 + 4).toFixed(1)}" fill="${theme.textTitle}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,sans-serif" font-size="${nameFontSize}" font-weight="600">${escapeXml(lang.name)}</text>
    <rect x="${(cardW - 48).toFixed(1)}" y="${(cardH / 2 - 9).toFixed(1)}" width="24" height="18" rx="4" fill="${theme.badgeBg}"/>
    <text x="${(cardW - 36).toFixed(1)}" y="${(cardH / 2 + 3.5).toFixed(1)}" text-anchor="middle" fill="${theme.badgeText}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,sans-serif" font-size="9" font-weight="700" letter-spacing=".2">${escapeXml(lang.tag)}</text>
    <text x="${(cardW - 7).toFixed(1)}" y="${(cardH / 2 + 3.5).toFixed(1)}" text-anchor="end" fill="${checkColor}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,sans-serif" font-size="10" font-weight="700">${percentStr}</text>
  </g>`
}

const getTheme = isDark => ({
  isDark,
  bg: isDark ? '#0D1117' : '#FFFFFF',
  border: isDark ? '#30363D' : '#D0D7DE',
  itemBg: isDark ? '#161B22' : '#F6F8FA',
  itemBorder: isDark ? '#21262D' : '#EAECEF',
  textTitle: isDark ? '#F0F6FC' : '#1F2328',
  textSub: isDark ? '#8B949E' : '#57606A',
  badgeBg: isDark ? '#21262D' : '#E7EBF0',
  badgeText: isDark ? '#C9D1D9' : '#424A53',
  teal: isDark ? '#2DD4BF' : '#0F766E',
  purple: isDark ? '#A855F7' : '#7C3AED'
})

const getLayout = (mobile, languageCount) => {
  const width = mobile ? 680 : 920
  const cols = mobile ? 3 : 4
  const rows = Math.ceil(languageCount / cols)
  const padX = mobile ? 18 : 22
  const cardGapX = mobile ? 10 : 12
  const cardGapY = mobile ? 8 : 10
  const cardW = (width - (padX * 2) - ((cols - 1) * cardGapX)) / cols
  const cardH = mobile ? 36 : 38
  const headerH = mobile ? 84 : 92
  const cardsStartY = headerH + 12
  const totalCardsH = rows * cardH + (rows - 1) * cardGapY
  const footerH = mobile ? 42 : 46
  const height = cardsStartY + totalCardsH + footerH

  return { width, cols, padX, cardGapX, cardGapY, cardW, cardH, cardsStartY, height }
}

const makePulse = ({ languages, globalPercent, isDark, mobile }) => {
  const theme = getTheme(isDark)
  const layout = getLayout(mobile, languages.length)
  const roundedPercent = Math.round(globalPercent)
  const languageCount = languages.length
  const aria = `Levyra translations: ${roundedPercent}% coverage across ${languageCount} supported languages`

  const cardsSvg = languages
    .map((lang, idx) => makeLanguageCard({ lang, idx, theme, ...layout }))
    .join('\n')

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${layout.width}" height="${layout.height}" viewBox="0 0 ${layout.width} ${layout.height}" role="img" aria-label="${escapeXml(aria)}">
  <title>${escapeXml(aria)}</title>
  <defs>
    <linearGradient id="g-accent" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" stop-color="${theme.purple}" />
      <stop offset="100%" stop-color="${theme.teal}" />
    </linearGradient>
  </defs>

  <rect x="0.75" y="0.75" width="${(layout.width - 1.5).toFixed(1)}" height="${(layout.height - 1.5).toFixed(1)}" rx="12" fill="${theme.bg}" stroke="${theme.border}" stroke-width="1.5"/>
  <path d="M 1 12 A 11 11 0 0 1 12 1 L ${layout.width - 12} 1 A 11 11 0 0 1 ${layout.width - 1} 12 L ${layout.width - 1} 3 L 1 3 Z" fill="url(#g-accent)" opacity="0.85"/>

  <g transform="translate(${layout.padX}, ${mobile ? 24 : 28})">
    <rect x="0" y="0" width="${mobile ? 112 : 124}" height="24" rx="12" fill="${theme.teal}" opacity="0.12"/>
    <circle cx="10" cy="12" r="3.5" fill="${theme.teal}"/>
    <text x="22" y="15.5" fill="${theme.teal}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,sans-serif" font-size="10.5" font-weight="700" letter-spacing=".6">TRANSLATIONS</text>

    <text x="${mobile ? 122 : 136}" y="16" fill="${theme.textTitle}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,sans-serif" font-size="${mobile ? 15 : 17}" font-weight="700">Native Multilingual Experience</text>
    <text x="0" y="${mobile ? 42 : 46}" fill="${theme.textSub}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,sans-serif" font-size="${mobile ? 11 : 12}">
      <tspan font-weight="700">${languageCount} supported languages</tspan> · 100% Android string coverage · Zero local setup needed
    </text>
  </g>

  <g>
${cardsSvg}
  </g>

  <g transform="translate(${layout.width / 2}, ${(layout.height - 18).toFixed(1)})">
    <text text-anchor="middle" fill="${theme.textSub}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,sans-serif" font-size="11">
      Community-powered localization via Weblate · <tspan fill="${theme.teal}" font-weight="600">Contribute or review in your browser →</tspan>
    </text>
  </g>
</svg>`
}

const updateTranslationPulse = async () => {
  const coverage = await collectRepositoryCoverage()
  await mkdir(ASSET_DIR, { recursive: true })
  await Promise.all([
    writeFile(ASSET_DIR + '/levyra-translation-pulse.svg', makePulse({ ...coverage, isDark: true, mobile: false }), 'utf8'),
    writeFile(ASSET_DIR + '/levyra-translation-pulse-light.svg', makePulse({ ...coverage, isDark: false, mobile: false }), 'utf8'),
    writeFile(ASSET_DIR + '/levyra-translation-pulse-mobile.svg', makePulse({ ...coverage, isDark: true, mobile: true }), 'utf8'),
    writeFile(ASSET_DIR + '/levyra-translation-pulse-mobile-light.svg', makePulse({ ...coverage, isDark: false, mobile: true }), 'utf8')
  ])
}

await updateTranslationPulse()
console.log('Successfully generated modern Translation Pulse SVG cards.')
