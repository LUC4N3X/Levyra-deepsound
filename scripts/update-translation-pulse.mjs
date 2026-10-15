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
  hero: isDark ? '#2B1D2A' : '#F0DCE7',
  heroStrong: isDark ? '#493347' : '#DFC1D2',
  ink: isDark ? '#F7F1F5' : '#241B21',
  muted: isDark ? '#A99DA6' : '#70636B',
  accent: isDark ? '#F09ACB' : '#8B416E',
  tiles: ['#E7B5D3', '#B9DDCF', '#C9CCF5', '#F2C5B2', '#E8D59C'],
  tileInk: '#241923',
  tileMuted: '#594751'
})

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
    const layouts = [
      { x: 26, y: 190, w: 668, h: 92, r: 38 },
      { x: 26, y: 298, w: 402, h: 88, r: 32 },
      { x: 444, y: 298, w: 250, h: 88, r: 32 },
      { x: 26, y: 402, w: 304, h: 88, r: 32 },
      { x: 346, y: 402, w: 348, h: 88, r: 32 }
    ]
    const tiles = groups.map((group, index) => {
      const box = layouts[index]
      const showExamples = box.w > 340
      return `<rect x="${box.x}" y="${box.y}" width="${box.w}" height="${box.h}" rx="${box.r}" fill="${theme.tiles[index]}"/>
  <text x="${box.x + 24}" y="${box.y + 38}" class="tile-label">${escapeXml(group.label)}</text>
  <text x="${box.x + box.w - 25}" y="${box.y + 42}" text-anchor="end" class="tile-count">${group.count}</text>
${showExamples ? `  <text x="${box.x + 24}" y="${box.y + 66}" class="tile-examples">${escapeXml(group.examples)}</text>` : ''}`
    }).join('\n  ')
    return `<svg xmlns="http://www.w3.org/2000/svg" width="720" height="530" viewBox="0 0 720 530" role="img" aria-label="${escapeXml(aria)}">
  <title>${escapeXml(aria)}</title>
  <desc>${escapeXml(description)}</desc>
  <style>
    text { font-family: ${FONT}; }
    .eyebrow { fill: ${theme.accent}; font-size: 11px; font-weight: 750; letter-spacing: 1.8px; }
    .hero { fill: ${theme.ink}; font-size: 58px; font-weight: 760; letter-spacing: -3px; }
    .meta { fill: ${theme.muted}; font-size: 12px; font-weight: 550; }
    .status { fill: ${theme.ink}; font-size: 12px; font-weight: 750; }
    .tile-label { fill: ${theme.tileInk}; font-size: 13px; font-weight: 800; letter-spacing: .4px; }
    .tile-count { fill: ${theme.tileInk}; font-size: 26px; font-weight: 800; }
    .tile-examples { fill: ${theme.tileMuted}; font-size: 10px; }
    .footer { fill: ${theme.muted}; font-size: 10px; font-weight: 600; letter-spacing: .3px; }
  </style>
  <rect width="720" height="530" rx="34" fill="${theme.background}"/>
  <rect x="26" y="20" width="668" height="150" rx="44" fill="${theme.hero}"/>
  <text x="58" y="62" class="eyebrow">LOCAL EVERYWHERE</text>
  <text x="54" y="126" class="hero">${languages.length} languages</text>
  <text x="58" y="151" class="meta">Five writing systems · ${catalogCount.toLocaleString('en-US')}+ app strings</text>
  <rect x="578" y="48" width="88" height="40" rx="20" fill="${theme.heroStrong}"/>
  <text x="622" y="73" text-anchor="middle" class="status">${roundedPercent}%</text>
  ${tiles}
  <text x="28" y="517" class="footer">TRANSLATIONS LIVE IN THE APP · NO CLOUD PROFILE</text>
</svg>`
  }

  const layouts = [
    { x: 360, y: 46, w: 712, h: 100, r: 42 },
    { x: 360, y: 162, w: 414, h: 88, r: 34 },
    { x: 790, y: 162, w: 282, h: 88, r: 34 },
    { x: 360, y: 266, w: 314, h: 88, r: 34 },
    { x: 690, y: 266, w: 382, h: 88, r: 34 }
  ]
  const tiles = groups.map((group, index) => {
    const box = layouts[index]
    const showExamples = box.w > 340
    return `<rect x="${box.x}" y="${box.y}" width="${box.w}" height="${box.h}" rx="${box.r}" fill="${theme.tiles[index]}"/>
  <text x="${box.x + 28}" y="${box.y + 40}" class="tile-label">${escapeXml(group.label)}</text>
  <text x="${box.x + box.w - 28}" y="${box.y + 43}" text-anchor="end" class="tile-count">${group.count}</text>
${showExamples ? `  <text x="${box.x + 28}" y="${box.y + 68}" class="tile-examples">${escapeXml(group.examples)}</text>` : ''}`
  }).join('\n  ')
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1100" height="390" viewBox="0 0 1100 390" role="img" aria-label="${escapeXml(aria)}">
  <title>${escapeXml(aria)}</title>
  <desc>${escapeXml(description)}</desc>
  <style>
    text { font-family: ${FONT}; }
    .eyebrow { fill: ${theme.accent}; font-size: 11px; font-weight: 750; letter-spacing: 1.8px; }
    .hero { fill: ${theme.ink}; font-size: 92px; font-weight: 780; letter-spacing: -6px; }
    .hero-label { fill: ${theme.ink}; font-size: 25px; font-weight: 720; letter-spacing: -.8px; }
    .meta { fill: ${theme.muted}; font-size: 12px; font-weight: 550; }
    .status { fill: ${theme.ink}; font-size: 12px; font-weight: 750; }
    .tile-label { fill: ${theme.tileInk}; font-size: 13px; font-weight: 800; letter-spacing: .4px; }
    .tile-count { fill: ${theme.tileInk}; font-size: 30px; font-weight: 820; }
    .tile-examples { fill: ${theme.tileMuted}; font-size: 10px; }
    .footer { fill: ${theme.muted}; font-size: 9px; font-weight: 650; letter-spacing: .35px; }
  </style>
  <rect width="1100" height="390" rx="34" fill="${theme.background}"/>
  <path d="M68 22H252Q338 22 338 108V284Q338 368 254 368H72Q22 368 22 318V72Q22 22 68 22Z" fill="${theme.hero}"/>
  <text x="58" y="72" class="eyebrow">LOCAL EVERYWHERE</text>
  <text x="51" y="183" class="hero">${languages.length}</text>
  <text x="58" y="218" class="hero-label">languages</text>
  <text x="58" y="252" class="meta">Five writing systems</text>
  <text x="58" y="272" class="meta">${catalogCount.toLocaleString('en-US')}+ app strings</text>
  <circle cx="280" cy="315" r="49" fill="${theme.heroStrong}"/>
  <text x="280" y="311" text-anchor="middle" class="status">${roundedPercent}%</text>
  <text x="280" y="329" text-anchor="middle" class="footer">COMPLETE</text>
  ${tiles}
  <text x="362" y="378" class="footer">TRANSLATIONS LIVE IN THE APP · NO CLOUD PROFILE</text>
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
