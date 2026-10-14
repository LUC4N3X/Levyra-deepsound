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
const MONO = "ui-monospace,SFMono-Regular,Menlo,Consolas,monospace"

const SCRIPT_GROUPS = [
  { id: 'latin', label: 'LATIN', dark: ['#C4B5FD', '#7C3AED'], light: ['#8B5CF6', '#5B21B6'] },
  { id: 'cyrillic', label: 'CYRILLIC & GREEK', dark: ['#7DD3FC', '#0284C7'], light: ['#38BDF8', '#0369A1'] },
  { id: 'rtl', label: 'ARABIC & HEBREW', dark: ['#FDE68A', '#D97706'], light: ['#FBBF24', '#B45309'] },
  { id: 'cjk', label: 'CJK', dark: ['#F9A8D4', '#DB2777'], light: ['#F472B6', '#BE185D'] },
  { id: 'brahmic', label: 'INDIC & THAI', dark: ['#86EFAC', '#16A34A'], light: ['#4ADE80', '#15803D'] }
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
  bg: isDark ? '#0D1117' : '#FFFFFF',
  panel: isDark ? '#11161D' : '#F8FAFC',
  grid: isDark ? '#242B35' : '#E2E8F0',
  axis: isDark ? '#3A4350' : '#CBD5E1',
  barTop: isDark ? '#C4B5FD' : '#8B5CF6',
  barBottom: isDark ? '#7C3AED' : '#5B21B6',
  missing: isDark ? '#3B2F5C' : '#EDE9FE',
  accent: isDark ? '#A78BFA' : '#7C3AED',
  accentSoft: isDark ? '#2E2547' : '#EDE9FE',
  average: isDark ? '#F472B6' : '#DB2777',
  textTitle: isDark ? '#F0F6FC' : '#0F172A',
  textSub: isDark ? '#8B949E' : '#64748B',
  textLabel: isDark ? '#C9D1D9' : '#1E293B'
})

const renderRing = ({ cx, cy, r, percent, theme }) => {
  const circumference = 2 * Math.PI * r
  const dash = (circumference * Math.min(100, Math.max(0, percent))) / 100
  return `  <circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="${theme.accentSoft}" stroke-width="7"/>
  <circle cx="${cx}" cy="${cy}" r="${r}" fill="none" stroke="url(#ring)" stroke-width="7" stroke-linecap="round" stroke-dasharray="${dash.toFixed(2)} ${circumference.toFixed(2)}" transform="rotate(-90 ${cx} ${cy})"/>
  <text x="${cx}" y="${cy + 5}" text-anchor="middle" fill="${theme.textTitle}" font-family="${FONT}" font-size="15" font-weight="800">${Math.round(percent)}%</text>`
}

const makePulse = ({ languages, globalPercent, catalogCount, isDark, mobile }) => {
  const theme = getTheme(isDark)
  const groupOrder = SCRIPT_GROUPS.map(group => group.id)
  const ranked = [...languages].sort((a, b) =>
    b.percent - a.percent ||
    groupOrder.indexOf(scriptGroupFor(a.code)) - groupOrder.indexOf(scriptGroupFor(b.code)) ||
    a.name.localeCompare(b.name)
  )
  const presentGroups = SCRIPT_GROUPS.filter(group => ranked.some(item => scriptGroupFor(item.code) === group.id))
  const hasMissing = ranked.some(item => item.percent < 99.5)
  const width = mobile ? 720 : 1100
  const pad = 28
  const headerH = 112
  const plotTop = headerH + 26
  const plotH = mobile ? 230 : 300
  const labelH = mobile ? 54 : 118
  const footerH = 46
  const axisW = mobile ? 34 : 96
  const plotLeft = pad + axisW
  const plotRight = width - pad
  const plotW = plotRight - plotLeft
  const plotBottom = plotTop + plotH
  const height = plotBottom + labelH + footerH
  const slot = plotW / ranked.length
  const barW = Math.max(6, Math.min(18, slot * 0.62))
  const y = percent => plotBottom - (plotH * Math.min(100, Math.max(0, percent))) / 100
  const roundedPercent = Math.round(globalPercent)
  const aria = `Levyra localization coverage: ${roundedPercent}% across ${languages.length} languages`

  const grid = [0, 25, 50, 75, 100].map(value => {
    const gy = y(value).toFixed(1)
    return `  <line x1="${plotLeft}" y1="${gy}" x2="${plotRight}" y2="${gy}" stroke="${value === 0 ? theme.axis : theme.grid}" stroke-width="1" ${value === 0 ? '' : 'stroke-dasharray="3 5"'}/>
  <text x="${plotLeft - 10}" y="${(+gy + 4).toFixed(1)}" text-anchor="end" fill="${theme.textSub}" font-family="${FONT}" font-size="11">${value}</text>`
  }).join('\n')

  const bars = ranked.map((item, index) => {
    const cx = plotLeft + slot * index + slot / 2
    const x = (cx - barW / 2).toFixed(1)
    const top = y(item.percent)
    const missingTop = y(100)
    const radius = Math.min(barW / 2, 6)
    const label = mobile ? item.code : `${item.name} · ${item.code}`
    const missing = item.percent < 99.5
      ? `    <rect x="${x}" y="${missingTop.toFixed(1)}" width="${barW.toFixed(1)}" height="${(top - missingTop).toFixed(1)}" rx="${radius}" fill="${theme.missing}"/>\n`
      : ''
    return `  <g>
    <title>${escapeXml(`${item.name}: ${Math.round(item.percent)}% (${item.translated}/${item.total})`)}</title>
${missing}    <rect x="${x}" y="${top.toFixed(1)}" width="${barW.toFixed(1)}" height="${(plotBottom - top).toFixed(1)}" rx="${radius}" fill="url(#bar-${scriptGroupFor(item.code)})"/>
    <text transform="translate(${(cx + 4).toFixed(1)} ${plotBottom + 14}) rotate(-50)" text-anchor="end" fill="${theme.textLabel}" font-family="${mobile ? MONO : FONT}" font-size="${mobile ? 10 : 11}" font-weight="${mobile ? 700 : 500}">${escapeXml(label)}</text>
  </g>`
  }).join('\n')

  const avgY = y(globalPercent).toFixed(1)
  const legendY = 92
  const legendItems = [
    ...presentGroups.map(group => ({ swatch: `url(#bar-${group.id})`, label: group.label })),
    ...(hasMissing ? [{ swatch: theme.missing, label: 'MISSING' }] : [])
  ]
  let legendX = pad
  const legend = legendItems.map(item => {
    const block = `  <rect x="${legendX}" y="${legendY + 4}" width="12" height="12" rx="3" fill="${item.swatch}"/>
  <text x="${legendX + 18}" y="${legendY + 14}" fill="${theme.textLabel}" font-family="${FONT}" font-size="${mobile ? 9.5 : 11}" font-weight="600" letter-spacing=".3">${escapeXml(item.label)}</text>`
    legendX += 18 + item.label.length * (mobile ? 6.4 : 7.4) + 18
    return block
  }).join('\n')

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${escapeXml(aria)}">
  <title>${escapeXml(aria)}</title>
  <defs>
${SCRIPT_GROUPS.map(group => {
    const [top, bottom] = isDark ? group.dark : group.light
    return `    <linearGradient id="bar-${group.id}" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="${top}"/>
      <stop offset="1" stop-color="${bottom}"/>
    </linearGradient>`
  }).join('\n')}
    <linearGradient id="ring" x1="0" y1="0" x2="1" y2="1">
      <stop offset="0" stop-color="${theme.barTop}"/>
      <stop offset="1" stop-color="${theme.average}"/>
    </linearGradient>
  </defs>
  <rect width="${width}" height="${height}" rx="20" fill="${theme.bg}"/>
  <rect x="${pad - 12}" y="${plotTop - 18}" width="${width - (pad - 12) * 2}" height="${plotH + labelH + 18}" rx="14" fill="${theme.panel}"/>
  <text x="${pad}" y="38" fill="${theme.accent}" font-family="${FONT}" font-size="11" font-weight="800" letter-spacing="1.6">LANGUAGE COVERAGE</text>
  <text x="${pad}" y="66" fill="${theme.textTitle}" font-family="${FONT}" font-size="${mobile ? 22 : 26}" font-weight="800">Levyra in ${languages.length} languages</text>
  <text x="${pad}" y="${legendY - 8}" fill="${theme.textSub}" font-family="${FONT}" font-size="12">${catalogCount.toLocaleString('en-US')}+ app strings · coverage per language, grouped by writing system</text>
${legend}
${renderRing({ cx: width - pad - 38, cy: 60, r: 30, percent: globalPercent, theme })}
${grid}
${bars}
${hasMissing ? `  <line x1="${plotLeft}" y1="${avgY}" x2="${plotRight}" y2="${avgY}" stroke="${theme.average}" stroke-width="2" stroke-dasharray="6 5" opacity=".9"/>` : ''}
  <text x="${pad}" y="${height - 20}" fill="${theme.textSub}" font-family="${FONT}" font-size="11" font-style="italic">Ranked by coverage, then by writing system · generated from the Levyra repository</text>
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
