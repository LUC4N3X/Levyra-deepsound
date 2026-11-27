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
  'ur': { name: 'اردو', code: 'UR' },
  'ta': { name: 'தமிழ்', code: 'TA' },
  'te': { name: 'తెలుగు', code: 'TE' },
  'mr': { name: 'मराठी', code: 'MR' },
  'gu': { name: 'ગુજરાતી', code: 'GU' },
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
  if (['ar', 'fa', 'he', 'ur'].includes(tag)) return 'rtl'
  if (['zh', 'zh-tw', 'ja', 'ko'].includes(tag)) return 'cjk'
  if (['hi', 'bn', 'th', 'ta', 'te', 'mr', 'gu'].includes(tag)) return 'brahmic'
  return 'latin'
}

const shortDisplayName = name => name.replace(/\s*\([^)]*\)/g, '')

const getTheme = isDark => ({
  hero: isDark ? '#ECF3FF' : '#F2F7FF',
  heroBorder: isDark ? 'none' : '#D0E0FA',
  ink: '#161F33',
  muted: '#4A5B7A',
  accent: '#295498',
  stampBg: '#C8DCFD',
  stampInk: '#122B52',
  footer: isDark ? '#8EA0BE' : '#536482',
  tiles: isDark
    ? [
        { bg: '#D4E4FF', ink: '#132C54', muted: '#284778' },
        { bg: '#C7EFE4', ink: '#103D31', muted: '#1F5748' },
        { bg: '#E3D6FC', ink: '#311D5A', muted: '#4A337A' },
        { bg: '#C5ECF8', ink: '#0F3B4B', muted: '#1E5568' },
        { bg: '#F1D6EC', ink: '#4A1C43', muted: '#682E5F' }
      ]
    : [
        { bg: '#E1ECFF', ink: '#132C54', muted: '#284778' },
        { bg: '#D8F5EC', ink: '#103D31', muted: '#1F5748' },
        { bg: '#EDE3FE', ink: '#311D5A', muted: '#4A337A' },
        { bg: '#D7F3FC', ink: '#0F3B4B', muted: '#1E5568' },
        { bg: '#F8E3F4', ink: '#4A1C43', muted: '#682E5F' }
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
    const heroPath = roundedCornersPath(16, 12, 688, 154, [54, 74, 46, 74])
    const stampPath = makeScallopPath(624, 89, 38, 45, 12)
    const layouts = [
      { x: 16, y: 178, w: 688, h: 94, c: [47, 47, 47, 32] },
      { x: 16, y: 284, w: 404, h: 92, c: [32, 46, 32, 46] },
      { x: 432, y: 284, w: 272, h: 92, c: [46, 46, 46, 46] },
      { x: 16, y: 388, w: 326, h: 92, c: [46, 46, 46, 46] },
      { x: 354, y: 388, w: 350, h: 92, c: [32, 46, 46, 32] }
    ]
    const tiles = groups.map((group, index) => {
      const box = layouts[index]
      const pal = theme.tiles[index]
      const d = roundedCornersPath(box.x, box.y, box.w, box.h, box.c)
      return `<path d="${d}" fill="${pal.bg}"/>
  <text x="${box.x + 30}" y="${box.y + 39}" fill="${pal.ink}" class="tile-label">${escapeXml(group.label)}</text>
  <text x="${box.x + box.w - 30}" y="${box.y + 43}" text-anchor="end" fill="${pal.ink}" class="tile-count">${group.count}</text>
  <text x="${box.x + 30}" y="${box.y + 68}" fill="${pal.muted}" class="tile-examples">${escapeXml(group.examples)}</text>`
    }).join('\n  ')
    return `<svg xmlns="http://www.w3.org/2000/svg" width="720" height="514" viewBox="0 0 720 514" role="img" aria-label="${escapeXml(aria)}">
  <title>${escapeXml(aria)}</title>
  <desc>${escapeXml(description)}</desc>
  <style>
    text { font-family: ${FONT}; }
    .eyebrow { fill: ${theme.accent}; font-size: 12px; font-weight: 700; letter-spacing: 1.6px; }
    .hero { fill: ${theme.ink}; font-size: 54px; font-weight: 760; letter-spacing: -2px; }
    .meta { fill: ${theme.muted}; font-size: 14px; font-weight: 550; }
    .status { fill: ${theme.stampInk}; font-size: 16px; font-weight: 780; }
    .status-sub { fill: ${theme.stampInk}; font-size: 9.5px; font-weight: 700; letter-spacing: .6px; opacity: .85; }
    .tile-label { font-size: 14.5px; font-weight: 760; letter-spacing: .4px; }
    .tile-count { font-size: 28px; font-weight: 800; }
    .tile-examples { font-size: 12px; font-weight: 550; }
    .footer { fill: ${theme.footer}; font-size: 11px; font-weight: 650; letter-spacing: .5px; }
  </style>
  <path d="${heroPath}" fill="${theme.hero}"${theme.heroBorder === 'none' ? '' : ` stroke="${theme.heroBorder}" stroke-width="1"`}/>
  <text x="52" y="52" class="eyebrow">LOCAL EVERYWHERE</text>
  <text x="50" y="114" class="hero">${languages.length} languages</text>
  <text x="52" y="144" class="meta">Five writing systems · ${catalogCount.toLocaleString('en-US')}+ app strings</text>
  <path d="${stampPath}" fill="${theme.stampBg}"/>
  <text x="624" y="86" text-anchor="middle" class="status">${roundedPercent}%</text>
  <text x="624" y="101" text-anchor="middle" class="status-sub">COMPLETE</text>
  ${tiles}
  <text x="360" y="503" text-anchor="middle" class="footer">COMMUNITY TRANSLATIONS · EVERY CONTRIBUTION COUNTS</text>
</svg>`
  }

  const heroPath = roundedCornersPath(16, 16, 326, 332, [64, 96, 64, 96])
  const stampPath = makeScallopPath(264, 272, 42, 49, 12)
  const layouts = [
    { x: 356, y: 16, w: 728, h: 104, c: [52, 52, 52, 34] },
    { x: 356, y: 132, w: 428, h: 102, c: [34, 51, 34, 51] },
    { x: 796, y: 132, w: 288, h: 102, c: [51, 51, 51, 51] },
    { x: 356, y: 246, w: 332, h: 102, c: [51, 51, 51, 51] },
    { x: 700, y: 246, w: 384, h: 102, c: [34, 51, 51, 34] }
  ]
  const tiles = groups.map((group, index) => {
    const box = layouts[index]
    const pal = theme.tiles[index]
    const d = roundedCornersPath(box.x, box.y, box.w, box.h, box.c)
    return `<path d="${d}" fill="${pal.bg}"/>
  <text x="${box.x + 34}" y="${box.y + 42}" fill="${pal.ink}" class="tile-label">${escapeXml(group.label)}</text>
  <text x="${box.x + box.w - 34}" y="${box.y + 46}" text-anchor="end" fill="${pal.ink}" class="tile-count">${group.count}</text>
  <text x="${box.x + 34}" y="${box.y + 73}" fill="${pal.muted}" class="tile-examples">${escapeXml(group.examples)}</text>`
  }).join('\n  ')
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1100" height="384" viewBox="0 0 1100 384" role="img" aria-label="${escapeXml(aria)}">
  <title>${escapeXml(aria)}</title>
  <desc>${escapeXml(description)}</desc>
  <style>
    text { font-family: ${FONT}; }
    .eyebrow { fill: ${theme.accent}; font-size: 12.5px; font-weight: 720; letter-spacing: 1.7px; }
    .hero { fill: ${theme.ink}; font-size: 88px; font-weight: 780; letter-spacing: -4.5px; }
    .hero-label { fill: ${theme.ink}; font-size: 25px; font-weight: 680; letter-spacing: -.5px; }
    .meta { fill: ${theme.muted}; font-size: 14.5px; font-weight: 540; }
    .status { fill: ${theme.stampInk}; font-size: 17px; font-weight: 800; }
    .status-sub { fill: ${theme.stampInk}; font-size: 9.5px; font-weight: 720; letter-spacing: .6px; opacity: .85; }
    .tile-label { font-size: 15px; font-weight: 760; letter-spacing: .45px; }
    .tile-count { font-size: 31px; font-weight: 800; }
    .tile-examples { font-size: 12.6px; font-weight: 550; }
    .footer { fill: ${theme.footer}; font-size: 11px; font-weight: 650; letter-spacing: .5px; }
  </style>
  <path d="${heroPath}" fill="${theme.hero}"${theme.heroBorder === 'none' ? '' : ` stroke="${theme.heroBorder}" stroke-width="1"`}/>
  <text x="54" y="68" class="eyebrow">LOCAL EVERYWHERE</text>
  <text x="50" y="170" class="hero">${languages.length}</text>
  <text x="54" y="206" class="hero-label">languages</text>
  <text x="54" y="242" class="meta">Five writing systems</text>
  <text x="54" y="264" class="meta">${catalogCount.toLocaleString('en-US')}+ app strings</text>
  <path d="${stampPath}" fill="${theme.stampBg}"/>
  <text x="264" y="269" text-anchor="middle" class="status">${roundedPercent}%</text>
  <text x="264" y="285" text-anchor="middle" class="status-sub">COMPLETE</text>
  ${tiles}
  <text x="720" y="373" text-anchor="middle" class="footer">COMMUNITY TRANSLATIONS · EVERY CONTRIBUTION COUNTS</text>
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
