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

// Curated clean display names & codes
const LOCALE_META = {
  'en': { name: 'English', code: 'EN' },
  'it': { name: 'Italian', code: 'IT' },
  'es': { name: 'Spanish', code: 'ES' },
  'fr': { name: 'French', code: 'FR' },
  'de': { name: 'German', code: 'DE' },
  'pt': { name: 'Portuguese', code: 'PT' },
  'nl': { name: 'Dutch', code: 'NL' },
  'pl': { name: 'Polish', code: 'PL' },
  'ro': { name: 'Romanian', code: 'RO' },
  'el': { name: 'Greek', code: 'EL' },
  'sv': { name: 'Swedish', code: 'SV' },
  'da': { name: 'Danish', code: 'DA' },
  'cs': { name: 'Czech', code: 'CS' },
  'sk': { name: 'Slovak', code: 'SK' },
  'hr': { name: 'Croatian', code: 'HR' },
  'bg': { name: 'Bulgarian', code: 'BG' },
  'hu': { name: 'Hungarian', code: 'HU' },
  'fi': { name: 'Finnish', code: 'FI' },
  'nb': { name: 'Norwegian', code: 'NB' },
  'ca': { name: 'Catalan', code: 'CA' },
  'uk': { name: 'Ukrainian', code: 'UK' },
  'ru': { name: 'Russian', code: 'RU' },
  'tr': { name: 'Turkish', code: 'TR' },
  'ar': { name: 'Arabic', code: 'AR' },
  'fa': { name: 'Persian', code: 'FA' },
  'zh-hans': { name: 'Chinese (Simp.)', code: 'ZH' },
  'zh-hant': { name: 'Chinese (Trad.)', code: 'ZH-TW' },
  'ja': { name: 'Japanese', code: 'JA' },
  'ko': { name: 'Korean', code: 'KO' },
  'hi': { name: 'Hindi', code: 'HI' },
  'id': { name: 'Indonesian', code: 'ID' },
  'ms': { name: 'Malay', code: 'MS' },
  'vi': { name: 'Vietnamese', code: 'VI' },
  'th': { name: 'Thai', code: 'TH' },
  'fil': { name: 'Filipino', code: 'FIL' },
  'he': { name: 'Hebrew', code: 'HE' }
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

const getTheme = isDark => ({
  isDark,
  bg: isDark ? '#0D1117' : '#FFFFFF',
  border: isDark ? '#30363D' : '#D0D7DE',
  barFill: isDark ? '#1F6FEB' : '#0B5796',
  barTrack: isDark ? '#161B22' : '#F0F3F6',
  gridLine: isDark ? '#21262D' : '#EAECEF',
  textTitle: isDark ? '#F0F6FC' : '#1F2328',
  textSub: isDark ? '#8B949E' : '#57606A',
  textLabel: isDark ? '#C9D1D9' : '#24292F',
  tagBg: isDark ? '#1E293B' : '#E0F2FE',
  tagText: isDark ? '#38BDF8' : '#0369A1',
  tagBorder: isDark ? '#334155' : '#BAE6FD',
  accentRed: '#EF4444'
})

const renderBarRow = ({ item, y, labelX, barX, maxBarW, barH, theme }) => {
  const barW = Math.max(3, (maxBarW * item.percent) / 100)
  const valX = barX + maxBarW + 8
  const pctStr = `${Math.round(item.percent)}%`

  return `    <g transform="translate(0, ${y.toFixed(1)})">
      <!-- Language Label -->
      <text x="${labelX}" y="${(barH - 2).toFixed(1)}" text-anchor="end" fill="${theme.textLabel}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,sans-serif" font-size="11" font-weight="500">${escapeXml(item.name)}</text>
      <!-- Track background -->
      <rect x="${barX}" y="0" width="${maxBarW}" height="${barH}" rx="2" fill="${theme.barTrack}"/>
      <!-- Progress Bar -->
      <rect x="${barX}" y="0" width="${barW.toFixed(1)}" height="${barH}" rx="2" fill="${theme.barFill}"/>
      <!-- Code tag badge -->
      <rect x="${valX}" y="0" width="24" height="${barH}" rx="3" fill="${theme.tagBg}" stroke="${theme.tagBorder}" stroke-width="0.5"/>
      <text x="${valX + 12}" y="${(barH - 2.5).toFixed(1)}" text-anchor="middle" fill="${theme.tagText}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,sans-serif" font-size="8.5" font-weight="700" letter-spacing=".2">${escapeXml(item.code)}</text>
      <!-- Percent -->
      <text x="${valX + 32}" y="${(barH - 2.5).toFixed(1)}" fill="${theme.textSub}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,sans-serif" font-size="9" font-weight="600">${pctStr}</text>
    </g>`
}

const renderColumn = ({ items, startX, startY, labelW, maxBarW, barH, rowGap, theme }) => {
  const labelX = startX + labelW
  const barX = labelX + 10
  const ticks = [0, 0.25, 0.5, 0.75, 1.0]
  const colHeight = items.length * barH + (items.length - 1) * rowGap

  // Vertical grid lines like in The Economist charts
  const gridLines = ticks.map(t => {
    const gx = barX + maxBarW * t
    return `    <line x1="${gx.toFixed(1)}" y1="${startY - 8}" x2="${gx.toFixed(1)}" y2="${startY + colHeight}" stroke="${theme.gridLine}" stroke-width="1" stroke-dasharray="${t === 1.0 ? 'none' : '2,2'}"/>
    <text x="${gx.toFixed(1)}" y="${startY - 12}" text-anchor="middle" fill="${theme.textSub}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,sans-serif" font-size="8.5">${Math.round(t * 100)}%</text>`
  }).join('\n')

  const rows = items.map((item, idx) => {
    const y = startY + idx * (barH + rowGap)
    return renderBarRow({ item, y, labelX, barX, maxBarW, barH, theme })
  }).join('\n')

  return `${gridLines}\n${rows}`
}

const makePulse = ({ languages, globalPercent, isDark, mobile }) => {
  const theme = getTheme(isDark)
  const width = mobile ? 720 : 880
  const isSingleCol = mobile

  // 2 columns of 18 rows each on desktop (ultra compact ~390px tall)
  const leftCol = isSingleCol ? languages : languages.slice(0, 18)
  const rightCol = isSingleCol ? [] : languages.slice(18)

  const labelW = 95
  const barH = 13
  const rowGap = 7
  const startY = 82
  const maxBarW = isSingleCol ? 140 : 185

  const leftColSvg = renderColumn({
    items: leftCol,
    startX: 16,
    startY,
    labelW,
    maxBarW,
    barH,
    rowGap,
    theme
  })

  const rightColSvg = isSingleCol ? '' : renderColumn({
    items: rightCol,
    startX: 455,
    startY,
    labelW,
    maxBarW,
    barH,
    rowGap,
    theme
  })

  const rowsCount = leftCol.length
  const chartH = rowsCount * barH + (rowsCount - 1) * rowGap
  const height = startY + chartH + 46
  const roundedPercent = Math.round(globalPercent)
  const aria = `Levyra localization coverage: ${roundedPercent}% across ${languages.length} languages`

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="${escapeXml(aria)}">
  <title>${escapeXml(aria)}</title>

  <!-- Clean Canvas Background -->
  <rect width="${width}" height="${height}" fill="${theme.bg}"/>

  <!-- The Economist style top Red Accent Block -->
  <rect x="20" y="16" width="12" height="4" fill="${theme.accentRed}"/>

  <!-- Chart Title & Subtitle -->
  <text x="20" y="36" fill="${theme.textTitle}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,sans-serif" font-size="16" font-weight="700">Language coverage</text>
  <text x="20" y="52" fill="${theme.textSub}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,sans-serif" font-size="11">
    Translation completion index across ${languages.length} supported Android locales (100% = fully translated)
  </text>

  <!-- Horizontal Divider -->
  <line x1="20" y1="62" x2="${width - 20}" y2="62" stroke="${theme.border}" stroke-width="1"/>

  <!-- Left Column Data -->
${leftColSvg}

  <!-- Right Column Data -->
${rightColSvg}

  <!-- Footer Info -->
  <line x1="20" y1="${height - 24}" x2="${width - 20}" y2="${height - 24}" stroke="${theme.border}" stroke-width="0.5"/>
  <text x="20" y="${height - 10}" fill="${theme.textSub}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,sans-serif" font-size="9.5">
    Source: Levyra open-source strings catalog · Translations hosted on Weblate (contributions open)
  </text>
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
console.log('Successfully generated Economist-style data chart SVG.')
