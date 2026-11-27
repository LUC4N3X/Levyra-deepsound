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
  'pt-pt': { name: 'Português (Portugal)', code: 'PT' },
  'pt-br': { name: 'Português (Brasil)', code: 'BR' },
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

    const baseLocalized = localizedFiles.get(canon.split('-')[0]) || new Set()
    const localized = new Set([...baseLocalized, ...(localizedFiles.get(canon) || [])])
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

const FONT = "Roboto,-apple-system,BlinkMacSystemFont,'Segoe UI',Helvetica,Arial,sans-serif"

const makeScallopPath = (cx, cy, rValley, rPeak, lobes = 12) => {
  const step = (2 * Math.PI) / lobes
  const halfStep = step / 2
  const dMid = rValley * Math.cos(halfStep)
  const chord = 2 * rValley * Math.sin(halfStep)
  const sagitta = rPeak - dMid
  const radius = sagitta / 2 + (chord ** 2) / (8 * sagitta)
  const x0 = cx + rValley * Math.sin(-halfStep)
  const y0 = cy - rValley * Math.cos(-halfStep)
  const parts = [`M ${x0.toFixed(2)} ${y0.toFixed(2)}`]
  for (let i = 0; i < lobes; i += 1) {
    const aEnd = (i + 0.5) * step
    const xEnd = cx + rValley * Math.sin(aEnd)
    const yEnd = cy - rValley * Math.cos(aEnd)
    parts.push(`A ${radius.toFixed(2)} ${radius.toFixed(2)} 0 0 1 ${xEnd.toFixed(2)} ${yEnd.toFixed(2)}`)
  }
  parts.push('Z')
  return parts.join(' ')
}

const roundedCornersPath = (x, y, w, h, [tl, tr, br, bl]) => {
  const x1 = x + w
  const y1 = y + h
  return [
    `M ${(x + tl).toFixed(2)} ${y.toFixed(2)}`,
    `H ${(x1 - tr).toFixed(2)}`,
    `A ${tr} ${tr} 0 0 1 ${x1.toFixed(2)} ${(y + tr).toFixed(2)}`,
    `V ${(y1 - br).toFixed(2)}`,
    `A ${br} ${br} 0 0 1 ${(x1 - br).toFixed(2)} ${y1.toFixed(2)}`,
    `H ${(x + bl).toFixed(2)}`,
    `A ${bl} ${bl} 0 0 1 ${x.toFixed(2)} ${(y1 - bl).toFixed(2)}`,
    `V ${(y + tl).toFixed(2)}`,
    `A ${tl} ${tl} 0 0 1 ${(x + tl).toFixed(2)} ${y.toFixed(2)} Z`
  ].join(' ')
}

const SCRIPT_GROUPS = [
  { id: 'latin', label: 'LATIN', maxExamples: 6 },
  { id: 'cyrillic', label: 'CYRILLIC + GREEK', maxExamples: 5 },
  { id: 'cjk', label: 'CJK', maxExamples: 4 },
  { id: 'rtl', label: 'ARABIC + HEBREW', maxExamples: 3 },
  { id: 'brahmic', label: 'INDIC + THAI', maxExamples: 3 }
]

const scriptGroupFor = code => {
  const tag = code.toLowerCase()
  if (['ru', 'uk', 'bg', 'sr', 'kk', 'el'].includes(tag)) return 'cyrillic'
  if (['ar', 'fa', 'he'].includes(tag)) return 'rtl'
  if (['zh', 'zh-tw', 'ja', 'ko'].includes(tag)) return 'cjk'
  if (['hi', 'bn', 'th'].includes(tag)) return 'brahmic'
  return 'latin'
}

const shortDisplayName = name => name.replace(/\s*\([^)]*\)/g, '')

const getTheme = isDark => ({
  hero: isDark ? '#1B2436' : '#E8F0FE',
  heroBorder: isDark ? '#2B3852' : '#C9DCFA',
  ink: isDark ? '#EAF1FF' : '#13294B',
  muted: isDark ? '#9EB0CE' : '#4B6387',
  accent: isDark ? '#A5C8FF' : '#24569C',
  stampBg: isDark ? '#CFE2FF' : '#C4DCFF',
  stampInk: '#123158',
  footer: isDark ? '#8B9AB5' : '#586A87',
  tiles: isDark
    ? [
        { bg: '#F6D2E0', ink: '#5A2238', muted: '#75334D' },
        { bg: '#CBEFE0', ink: '#144230', muted: '#235B44' },
        { bg: '#E6D5FA', ink: '#3F225F', muted: '#57367C' },
        { bg: '#FAD6C4', ink: '#562614', muted: '#753B24' },
        { bg: '#FAE3B4', ink: '#4B3409', muted: '#694B14' }
      ]
    : [
        { bg: '#FCE4EE', ink: '#5A2238', muted: '#75334D' },
        { bg: '#DCF6EB', ink: '#144230', muted: '#235B44' },
        { bg: '#F0E4FD', ink: '#3F225F', muted: '#57367C' },
        { bg: '#FCE3D6', ink: '#562614', muted: '#753B24' },
        { bg: '#FDF0CF', ink: '#4B3409', muted: '#694B14' }
      ]
})

const groupRows = languages => SCRIPT_GROUPS.map(group => {
  const members = languages.filter(language => scriptGroupFor(language.code) === group.id)
  return {
    ...group,
    count: members.length,
    examples: members.slice(0, group.maxExamples).map(language => shortDisplayName(language.name)).join(' · ')
  }
}).filter(group => group.count > 0)

const makePulse = ({ languages, globalPercent, catalogCount, isDark, mobile }) => {
  const theme = getTheme(isDark)
  const groups = groupRows(languages)
  const roundedPercent = Math.round(globalPercent)
  const aria = `Levyra localization coverage: ${roundedPercent}% across ${languages.length} languages`
  const description = languages.map(language => `${language.name}: ${Math.round(language.percent)}%`).join(', ')

  if (mobile) {
    const heroPath = roundedCornersPath(16, 12, 688, 152, [34, 34, 14, 14])
    const stampPath = makeScallopPath(634, 88, 38, 45, 12)
    const layouts = [
      { x: 16, y: 176, w: 688, h: 94, c: [14, 14, 12, 12] },
      { x: 16, y: 280, w: 404, h: 92, c: [12, 12, 12, 12] },
      { x: 430, y: 280, w: 274, h: 92, c: [12, 12, 12, 12] },
      { x: 16, y: 382, w: 326, h: 92, c: [12, 12, 12, 32] },
      { x: 352, y: 382, w: 352, h: 92, c: [12, 12, 32, 12] }
    ]
    const tiles = groups.map((group, index) => {
      const box = layouts[index]
      const pal = theme.tiles[index]
      const d = roundedCornersPath(box.x, box.y, box.w, box.h, box.c)
      return `<path d="${d}" fill="${pal.bg}"/>
  <text x="${box.x + 24}" y="${box.y + 38}" fill="${pal.ink}" class="tile-label">${escapeXml(group.label)}</text>
  <text x="${box.x + box.w - 24}" y="${box.y + 42}" text-anchor="end" fill="${pal.ink}" class="tile-count">${group.count}</text>
  <text x="${box.x + 24}" y="${box.y + 68}" fill="${pal.muted}" class="tile-examples">${escapeXml(group.examples)}</text>`
    }).join('\n  ')
    return `<svg xmlns="http://www.w3.org/2000/svg" width="720" height="508" viewBox="0 0 720 508" role="img" aria-label="${escapeXml(aria)}">
  <title>${escapeXml(aria)}</title>
  <desc>${escapeXml(description)}</desc>
  <style>
    text { font-family: ${FONT}; }
    .eyebrow { fill: ${theme.accent}; font-size: 12px; font-weight: 700; letter-spacing: 1.6px; }
    .hero { fill: ${theme.ink}; font-size: 54px; font-weight: 760; letter-spacing: -2px; }
    .meta { fill: ${theme.muted}; font-size: 14px; font-weight: 550; }
    .status { fill: ${theme.stampInk}; font-size: 16px; font-weight: 780; }
    .status-sub { fill: ${theme.stampInk}; font-size: 9.5px; font-weight: 700; letter-spacing: .6px; opacity: .82; }
    .tile-label { font-size: 14.5px; font-weight: 760; letter-spacing: .4px; }
    .tile-count { font-size: 28px; font-weight: 800; }
    .tile-examples { font-size: 12.2px; font-weight: 550; }
    .footer { fill: ${theme.footer}; font-size: 11px; font-weight: 650; letter-spacing: .5px; }
  </style>
  <path d="${heroPath}" fill="${theme.hero}" stroke="${theme.heroBorder}" stroke-width="1"/>
  <text x="46" y="50" class="eyebrow">LOCAL EVERYWHERE</text>
  <text x="44" y="112" class="hero">${languages.length} languages</text>
  <text x="46" y="142" class="meta">Five writing systems · ${catalogCount.toLocaleString('en-US')}+ app strings</text>
  <path d="${stampPath}" fill="${theme.stampBg}"/>
  <text x="634" y="85" text-anchor="middle" class="status">${roundedPercent}%</text>
  <text x="634" y="100" text-anchor="middle" class="status-sub">COMPLETE</text>
  ${tiles}
  <text x="360" y="497" text-anchor="middle" class="footer">COMMUNITY TRANSLATIONS · EVERY CONTRIBUTION COUNTS</text>
</svg>`
  }

  const heroPath = roundedCornersPath(16, 16, 326, 330, [36, 14, 14, 36])
  const stampPath = makeScallopPath(276, 282, 42, 49, 12)
  const layouts = [
    { x: 354, y: 16, w: 730, h: 104, c: [14, 36, 12, 12] },
    { x: 354, y: 130, w: 432, h: 103, c: [12, 12, 12, 12] },
    { x: 796, y: 130, w: 288, h: 103, c: [12, 12, 12, 12] },
    { x: 354, y: 243, w: 334, h: 103, c: [12, 12, 12, 14] },
    { x: 698, y: 243, w: 386, h: 103, c: [12, 12, 36, 12] }
  ]
  const tiles = groups.map((group, index) => {
    const box = layouts[index]
    const pal = theme.tiles[index]
    const d = roundedCornersPath(box.x, box.y, box.w, box.h, box.c)
    return `<path d="${d}" fill="${pal.bg}"/>
  <text x="${box.x + 26}" y="${box.y + 41}" fill="${pal.ink}" class="tile-label">${escapeXml(group.label)}</text>
  <text x="${box.x + box.w - 26}" y="${box.y + 45}" text-anchor="end" fill="${pal.ink}" class="tile-count">${group.count}</text>
  <text x="${box.x + 26}" y="${box.y + 74}" fill="${pal.muted}" class="tile-examples">${escapeXml(group.examples)}</text>`
  }).join('\n  ')
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1100" height="380" viewBox="0 0 1100 380" role="img" aria-label="${escapeXml(aria)}">
  <title>${escapeXml(aria)}</title>
  <desc>${escapeXml(description)}</desc>
  <style>
    text { font-family: ${FONT}; }
    .eyebrow { fill: ${theme.accent}; font-size: 12.5px; font-weight: 720; letter-spacing: 1.7px; }
    .hero { fill: ${theme.ink}; font-size: 88px; font-weight: 780; letter-spacing: -4.5px; }
    .hero-label { fill: ${theme.ink}; font-size: 25px; font-weight: 680; letter-spacing: -.5px; }
    .meta { fill: ${theme.muted}; font-size: 14.5px; font-weight: 540; }
    .status { fill: ${theme.stampInk}; font-size: 17px; font-weight: 800; }
    .status-sub { fill: ${theme.stampInk}; font-size: 9.5px; font-weight: 720; letter-spacing: .6px; opacity: .82; }
    .tile-label { font-size: 15px; font-weight: 760; letter-spacing: .45px; }
    .tile-count { font-size: 31px; font-weight: 800; }
    .tile-examples { font-size: 12.8px; font-weight: 550; }
    .footer { fill: ${theme.footer}; font-size: 11px; font-weight: 650; letter-spacing: .5px; }
  </style>
  <path d="${heroPath}" fill="${theme.hero}" stroke="${theme.heroBorder}" stroke-width="1"/>
  <text x="50" y="64" class="eyebrow">LOCAL EVERYWHERE</text>
  <text x="46" y="168" class="hero">${languages.length}</text>
  <text x="50" y="204" class="hero-label">languages</text>
  <text x="50" y="242" class="meta">Five writing systems</text>
  <text x="50" y="264" class="meta">${catalogCount.toLocaleString('en-US')}+ app strings</text>
  <path d="${stampPath}" fill="${theme.stampBg}"/>
  <text x="276" y="279" text-anchor="middle" class="status">${roundedPercent}%</text>
  <text x="276" y="295" text-anchor="middle" class="status-sub">COMPLETE</text>
  ${tiles}
  <text x="719" y="369" text-anchor="middle" class="footer">COMMUNITY TRANSLATIONS · EVERY CONTRIBUTION COUNTS</text>
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
