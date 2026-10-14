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

// Curated native display names & codes
const LOCALE_META = {
  'en': { name: 'English', code: 'EN' },
  'it': { name: 'Italiano', code: 'IT' },
  'es': { name: 'Español', code: 'ES' },
  'fr': { name: 'Français', code: 'FR' },
  'de': { name: 'Deutsch', code: 'DE' },
  'pt': { name: 'Português', code: 'PT' },
  'nl': { name: 'Nederlands', code: 'NL' },
  'pl': { name: 'Polski', code: 'PL' },
  'ro': { name: 'Română', code: 'RO' },
  'el': { name: 'Ελληνικά', code: 'EL' },
  'sv': { name: 'Svenska', code: 'SV' },
  'da': { name: 'Dansk', code: 'DA' },
  'cs': { name: 'Čeština', code: 'CS' },
  'sk': { name: 'Slovenčina', code: 'SK' },
  'hr': { name: 'Hrvatski', code: 'HR' },
  'sr': { name: 'Српски', code: 'SR' },
  'sq': { name: 'Shqip', code: 'SQ' },
  'bg': { name: 'Български', code: 'BG' },
  'hu': { name: 'Magyar', code: 'HU' },
  'fi': { name: 'Suomi', code: 'FI' },
  'et': { name: 'Eesti', code: 'ET' },
  'nb': { name: 'Norsk bokmål', code: 'NB' },
  'ca': { name: 'Català', code: 'CA' },
  'uk': { name: 'Українська', code: 'UK' },
  'ru': { name: 'Русский', code: 'RU' },
  'kk': { name: 'Қазақша', code: 'KK' },
  'tr': { name: 'Türkçe', code: 'TR' },
  'ar': { name: 'العربية', code: 'AR' },
  'fa': { name: 'فارسی', code: 'FA' },
  'zh-hans': { name: '简体中文', code: 'ZH' },
  'zh-hant': { name: '繁體中文', code: 'ZH-TW' },
  'ja': { name: '日本語', code: 'JA' },
  'ko': { name: '한국어', code: 'KO' },
  'hi': { name: 'हिन्दी', code: 'HI' },
  'bn': { name: 'বাংলা', code: 'BN' },
  'id': { name: 'Bahasa Indonesia', code: 'ID' },
  'ms': { name: 'Bahasa Melayu', code: 'MS' },
  'vi': { name: 'Tiếng Việt', code: 'VI' },
  'th': { name: 'ไทย', code: 'TH' },
  'fil': { name: 'Filipino', code: 'FIL' },
  'he': { name: 'עברית', code: 'HE' }
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
    const meta = LOCALE_META[canon] || { name: code, code: code.toUpperCase() }
    if (canon === 'en') {
      return {
        name: meta.name,
        code: meta.code,
        percent: 100,
        translated: expectedKeys.size,
        total: expectedKeys.size
      }
    }

    const localized = localizedFiles.get(canon) || new Set()
    const translated = Array.from(expectedKeys).filter(key => localized.has(key)).length
    return {
      name: meta.name,
      code: meta.code,
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

const FONT = "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif"
const MONO = "ui-monospace,SFMono-Regular,Menlo,Consolas,monospace"

const getTheme = isDark => ({
  bg: isDark ? '#0D1117' : '#FFFFFF',
  border: isDark ? '#21262D' : '#E5E7EB',
  chipBg: isDark ? '#161B22' : '#F8FAFC',
  chipBorder: isDark ? '#262C36' : '#E2E8F0',
  track: isDark ? '#21262D' : '#E2E8F0',
  accent: isDark ? '#A78BFA' : '#7C3AED',
  accentSoft: isDark ? '#2E2547' : '#EDE9FE',
  textTitle: isDark ? '#F0F6FC' : '#0F172A',
  textSub: isDark ? '#8B949E' : '#64748B',
  textLabel: isDark ? '#C9D1D9' : '#1E293B'
})

const renderChip = ({ item, x, y, w, h, theme }) => {
  const progressW = Math.max(2, (w - 20) * item.percent / 100)
  const complete = item.percent >= 99.5
  return `    <g transform="translate(${x.toFixed(1)}, ${y.toFixed(1)})">
      <rect width="${w}" height="${h}" rx="9" fill="${theme.chipBg}" stroke="${theme.chipBorder}"/>
      <text x="10" y="13" fill="${theme.accent}" font-family="${MONO}" font-size="9.5" font-weight="700" letter-spacing=".4">${escapeXml(item.code)}</text>
      <text x="${w - 10}" y="13" text-anchor="end" fill="${complete ? theme.textSub : theme.textLabel}" font-family="${FONT}" font-size="9.5" font-weight="600">${Math.round(item.percent)}%</text>
      <text x="10" y="26" fill="${theme.textLabel}" font-family="${FONT}" font-size="11" font-weight="600">${escapeXml(item.name)}</text>
      <rect x="10" y="${h - 5}" width="${w - 20}" height="2" rx="1" fill="${theme.track}"/>
      <rect x="10" y="${h - 5}" width="${progressW.toFixed(1)}" height="2" rx="1" fill="${theme.accent}"/>
    </g>`
}

const renderRing = ({ cx, cy, r, percent, theme }) => {
  const circumference = 2 * Math.PI * r
  const dash = (circumference * Math.min(100, Math.max(0, percent))) / 100
  return `  <circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="${theme.accentSoft}" stroke-width="6"/>
  <circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="${theme.accent}" stroke-width="6" stroke-linecap="round" stroke-dasharray="${dash.toFixed(2)} ${circumference.toFixed(2)}" transform="rotate(-90 ${cx} ${cy})"/>
  <text x="${cx}" y="${cy + 4}" text-anchor="middle" fill="${theme.textTitle}" font-family="${FONT}" font-size="13" font-weight="700">${Math.round(percent)}%</text>`
}

const makePulse = ({ languages, globalPercent, isDark, mobile }) => {
  const theme = getTheme(isDark)
  const width = mobile ? 720 : 920
  const columns = mobile ? 4 : 7
  const pad = 20
  const gap = 6
  const chipH = 36
  const headerH = 88
  const rows = Math.ceil(languages.length / columns)
  const chipW = (width - pad * 2 - gap * (columns - 1)) / columns
  const height = headerH + rows * chipH + (rows - 1) * gap + pad
  const roundedPercent = Math.round(globalPercent)
  const aria = `Levyra localization coverage: ${roundedPercent}% across ${languages.length} languages`

  const chips = languages.map((item, index) => {
    const column = index % columns
    const row = Math.floor(index / columns)
    return renderChip({
      item,
      x: pad + column * (chipW + gap),
      y: headerH + row * (chipH + gap),
      w: chipW,
      h: chipH,
      theme
    })
  }).join('\n')

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${escapeXml(aria)}">
  <title>${escapeXml(aria)}</title>
  <rect width="${width}" height="${height}" rx="16" fill="${theme.bg}"/>
  <text x="${pad}" y="30" fill="${theme.textSub}" font-family="${FONT}" font-size="10" font-weight="700" letter-spacing="1.4">LANGUAGE COVERAGE</text>
  <text x="${pad}" y="56" fill="${theme.textTitle}" font-family="${FONT}" font-size="22" font-weight="800">${languages.length} languages</text>
  <text x="${pad}" y="74" fill="${theme.textSub}" font-family="${FONT}" font-size="11">Android resource coverage per locale · generated from the Levyra repository</text>
${renderRing({ cx: width - pad - 30, cy: 44, r: 24, percent: globalPercent, theme })}
${chips}
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
console.log('Generated translation coverage SVG.')
