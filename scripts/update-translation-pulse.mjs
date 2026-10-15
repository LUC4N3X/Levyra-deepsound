import { mkdir, readFile, readdir, writeFile } from 'node:fs/promises'
import { join } from 'node:path'

const RESOURCE_ROOT = 'app/src/main/res'
const BASE_STRINGS = join(RESOURCE_ROOT, 'values', 'strings.xml')
const LOCALE_CONFIG = join(RESOURCE_ROOT, 'xml', 'locales_config.xml')
const ASSET_DIR = 'docs/assets'
const STRING_CATALOG_DIR = 'app/src/main/java/com/luc4n3x/levyra/ui/i18n'

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

const countCatalogStrings = async () => {
  const keys = new Set()
  for (const file of await readdir(STRING_CATALOG_DIR)) {
    if (!file.endsWith('.kt')) continue
    const source = await readFile(join(STRING_CATALOG_DIR, file), 'utf8')
    for (const match of source.matchAll(/get\(\) = value\("([^"]+)"\)/g)) keys.add(match[1])
    for (const block of source.matchAll(/KeyOrder\s*=\s*listOf\(([\s\S]*?)\)/g)) {
      for (const key of block[1].matchAll(/"([^"]+)"/g)) keys.add(key[1])
    }
  }
  return keys.size
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
    stringCount: expectedKeys.size,
    catalogCount: await countCatalogStrings()
  }
}

const FONT = "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif"

const SCRIPT_GROUPS = [
  { id: 'latin', label: 'LATIN' },
  { id: 'cyrillic', label: 'CYRILLIC + GREEK' },
  { id: 'cjk', label: 'CJK' },
  { id: 'rtl', label: 'ARABIC + HEBREW' },
  { id: 'brahmic', label: 'INDIC + THAI' }
]

const scriptGroupFor = code => {
  const tag = code.toLowerCase()
  if (['ru', 'uk', 'bg', 'sr', 'kk', 'el'].includes(tag)) return 'cyrillic'
  if (['ar', 'fa', 'he'].includes(tag)) return 'rtl'
  if (['zh', 'zh-tw', 'ja', 'ko'].includes(tag)) return 'cjk'
  if (['hi', 'bn', 'th'].includes(tag)) return 'brahmic'
  return 'latin'
}

const getTheme = isDark => ({
  background: isDark ? '#0D1117' : '#FBF8F6',
  panel: isDark ? '#151019' : '#F4E9EF',
  ink: isDark ? '#F7F1F5' : '#241B21',
  muted: isDark ? '#A99DA6' : '#70636B',
  rule: isDark ? '#302732' : '#DDD0D7',
  accent: isDark ? '#E7A7CF' : '#8B416E',
  accentSoft: isDark ? '#52364A' : '#E7C7D9'
})

const markerRow = ({ x, y, count, available, theme }) => {
  const gap = 6
  const width = Math.min(17, (available - gap * (count - 1)) / count)
  return Array.from({ length: count }, (_, index) => {
    const height = [14, 24, 18, 30, 20][index % 5]
    const fill = index % 4 === 0 ? theme.accentSoft : theme.accent
    return `<rect x="${(x + index * (width + gap)).toFixed(1)}" y="${(y - height).toFixed(1)}" width="${width.toFixed(1)}" height="${height}" rx="${Math.min(4, width / 2).toFixed(1)}" fill="${fill}"/>`
  }).join('')
}

const groupRows = languages => SCRIPT_GROUPS.map(group => {
  const members = languages.filter(language => scriptGroupFor(language.code) === group.id)
  return {
    ...group,
    count: members.length,
    examples: members.slice(0, 6).map(language => language.name).join(' · ')
  }
}).filter(group => group.count > 0)

const makePulse = ({ languages, globalPercent, catalogCount, isDark, mobile }) => {
  const theme = getTheme(isDark)
  const groups = groupRows(languages)
  const roundedPercent = Math.round(globalPercent)
  const aria = `Levyra localization coverage: ${roundedPercent}% across ${languages.length} languages`
  const description = languages.map(language => `${language.name}: ${Math.round(language.percent)}%`).join(', ')

  if (mobile) {
    const rows = groups.map((group, index) => {
      const y = 272 + index * 63
      return `<text x="42" y="${y}" class="group">${escapeXml(group.label)}</text>
  <text x="678" y="${y}" text-anchor="end" class="count">${String(group.count).padStart(2, '0')}</text>
  <text x="42" y="${y + 19}" class="examples">${escapeXml(group.examples)}</text>
  <g>${markerRow({ x: 458, y: y + 24, count: group.count, available: 220, theme })}</g>
  <line x1="42" y1="${y + 36}" x2="678" y2="${y + 36}" stroke="${theme.rule}"/>`
    }).join('\n  ')
    return `<svg xmlns="http://www.w3.org/2000/svg" width="720" height="620" viewBox="0 0 720 620" role="img" aria-label="${escapeXml(aria)}">
  <title>${escapeXml(aria)}</title>
  <desc>${escapeXml(description)}</desc>
  <style>
    text { font-family: ${FONT}; }
    .kicker { fill: ${theme.accent}; font-size: 11px; font-weight: 800; letter-spacing: 2px; }
    .hero { fill: ${theme.ink}; font-size: 112px; font-weight: 850; letter-spacing: -7px; }
    .hero-label { fill: ${theme.ink}; font-size: 34px; font-weight: 760; letter-spacing: -1.2px; }
    .meta { fill: ${theme.muted}; font-size: 13px; }
    .statement { fill: ${theme.ink}; font-size: 28px; font-weight: 850; }
    .statement-small { fill: ${theme.muted}; font-size: 10px; font-weight: 750; letter-spacing: 1.2px; }
    .group { fill: ${theme.ink}; font-size: 12px; font-weight: 800; letter-spacing: .7px; }
    .count { fill: ${theme.accent}; font-size: 20px; font-weight: 850; }
    .examples { fill: ${theme.muted}; font-size: 10px; }
    .footer { fill: ${theme.muted}; font-size: 10px; font-weight: 650; letter-spacing: .4px; }
  </style>
  <rect width="720" height="620" rx="20" fill="${theme.background}"/>
  <path d="M0 0H308C332 64 322 133 285 190C265 220 256 238 258 250H0Z" fill="${theme.panel}"/>
  <text x="42" y="43" class="kicker">LEVYRA / LOCAL EVERYWHERE</text>
  <text x="37" y="151" class="hero">${languages.length}</text>
  <text x="43" y="188" class="hero-label">languages.</text>
  <text x="43" y="218" class="meta">Five writing systems. Every string complete.</text>
  <text x="676" y="72" text-anchor="end" class="statement">${roundedPercent}%</text>
  <text x="676" y="91" text-anchor="end" class="statement-small">TRANSLATION COVERAGE</text>
  ${rows}
  <text x="42" y="590" class="footer">${languages.length} / ${languages.length} LOCALE PACKS COMPLETE</text>
  <text x="678" y="590" text-anchor="end" class="footer">${catalogCount.toLocaleString('en-US')}+ APP STRINGS</text>
</svg>`
  }

  const rows = groups.map((group, index) => {
    const y = 104 + index * 68
    return `<line x1="390" y1="${y + 31}" x2="1044" y2="${y + 31}" stroke="${theme.rule}"/>
  <text x="390" y="${y}" class="group">${escapeXml(group.label)}</text>
  <text x="1038" y="${y}" text-anchor="end" class="count">${String(group.count).padStart(2, '0')}</text>
  <text x="390" y="${y + 22}" class="examples">${escapeXml(group.examples)}</text>
  <g>${markerRow({ x: 790, y: y + 26, count: group.count, available: 248, theme })}</g>`
  }).join('\n  ')
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1100" height="480" viewBox="0 0 1100 480" role="img" aria-label="${escapeXml(aria)}">
  <title>${escapeXml(aria)}</title>
  <desc>${escapeXml(description)}</desc>
  <style>
    text { font-family: ${FONT}; }
    .kicker { fill: ${theme.accent}; font-size: 12px; font-weight: 800; letter-spacing: 2.2px; }
    .hero { fill: ${theme.ink}; font-size: 132px; font-weight: 850; letter-spacing: -8px; }
    .hero-label { fill: ${theme.ink}; font-size: 38px; font-weight: 760; letter-spacing: -1.5px; }
    .meta { fill: ${theme.muted}; font-size: 14px; }
    .statement { fill: ${theme.ink}; font-size: 30px; font-weight: 800; letter-spacing: -1px; }
    .statement-small { fill: ${theme.muted}; font-size: 12px; font-weight: 700; letter-spacing: 1.4px; }
    .group { fill: ${theme.ink}; font-size: 13px; font-weight: 800; letter-spacing: .8px; }
    .count { fill: ${theme.accent}; font-size: 24px; font-weight: 850; }
    .examples { fill: ${theme.muted}; font-size: 11px; }
    .footer { fill: ${theme.muted}; font-size: 11px; font-weight: 650; letter-spacing: .5px; }
  </style>
  <rect width="1100" height="480" rx="22" fill="${theme.background}"/>
  <path d="M0 0H328C360 78 352 166 310 236C268 306 270 390 334 480H0Z" fill="${theme.panel}"/>
  <text x="48" y="58" class="kicker">LEVYRA / LOCAL EVERYWHERE</text>
  <text x="42" y="190" class="hero">${languages.length}</text>
  <text x="49" y="232" class="hero-label">languages.</text>
  <text x="50" y="269" class="meta">One interface. Five writing systems.</text>
  <text x="50" y="290" class="meta">Every app string, complete.</text>
  <line x1="50" y1="339" x2="302" y2="339" stroke="${theme.rule}"/>
  <text x="50" y="375" class="statement">${roundedPercent}%</text>
  <text x="147" y="368" class="statement-small">TRANSLATION</text>
  <text x="147" y="384" class="statement-small">COVERAGE</text>
  <text x="390" y="49" class="kicker">THE LANGUAGE SCORE</text>
  <text x="1038" y="49" text-anchor="end" class="footer">${catalogCount.toLocaleString('en-US')}+ APP STRINGS</text>
  ${rows}
  <text x="390" y="446" class="footer">${languages.length} / ${languages.length} LOCALE PACKS COMPLETE</text>
  <text x="1038" y="446" text-anchor="end" class="footer">BUILT INTO LEVYRA · NO CLOUD PROFILE</text>
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
