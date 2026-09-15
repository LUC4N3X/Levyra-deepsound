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

const displayCode = code => {
  const normalized = canonicalTag(code)
  const aliases = {
    'zh-hans': 'ZH',
    'zh-hant': 'ZH-TW',
    fil: 'FIL'
  }
  return aliases[normalized] || normalized.split('-')[0].slice(0, 3).toUpperCase()
}

const displayName = code => {
  try {
    return new Intl.DisplayNames(['en'], { type: 'language' }).of(code) || code
  } catch {
    return code
  }
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
    } catch {
    }
  }

  const languages = localeTags.map(code => {
    if (canonicalTag(code) === 'en') {
      return {
        name: 'English',
        code,
        percent: 100,
        translated: expectedKeys.size,
        total: expectedKeys.size
      }
    }

    const localized = localizedFiles.get(canonicalTag(code)) || new Set()
    const translated = Array.from(expectedKeys).filter(key => localized.has(key)).length
    return {
      name: displayName(code),
      code,
      percent: (translated / expectedKeys.size) * 100,
      translated,
      total: expectedKeys.size
    }
  })

  const translated = languages.reduce((sum, language) => sum + language.translated, 0)
  const total = languages.reduce((sum, language) => sum + language.total, 0)

  return {
    languages,
    globalPercent: total === 0 ? 0 : (translated / total) * 100,
    stringCount: expectedKeys.size
  }
}

const makeBars = ({ languages, columns, startX, startY, width, chartHeight, rowGap, isDark }) => {
  const step = width / columns
  const barWidth = Math.max(5, Math.min(7, step * 0.22))
  const trackBg = isDark ? '#20262E' : '#E7EBF0'

  return languages.map((language, index) => {
    const row = Math.floor(index / columns)
    const col = index % columns
    const center = startX + step * col + step / 2
    const baseline = startY + row * rowGap + chartHeight
    const fillHeight = Math.max(4, (chartHeight * language.percent) / 100)
    const fillTop = baseline - fillHeight
    const fill = language.percent >= 99.5
      ? (isDark ? '#2DD4BF' : '#0F766E')
      : language.percent >= 80
        ? (isDark ? '#60A5FA' : '#2563EB')
        : language.percent >= 50
          ? (isDark ? '#818CF8' : '#6366F1')
          : (isDark ? '#FB7185' : '#E11D48')
    const label = isDark ? '#8B949E' : '#57606A'

    return [
      '<g>',
      '<title>' + escapeXml(language.name) + ' — ' + Math.round(language.percent) + '%</title>',
      '<rect x="' + (center - barWidth / 2).toFixed(1) + '" y="' + (baseline - chartHeight).toFixed(1) + '" width="' + barWidth.toFixed(1) + '" height="' + chartHeight.toFixed(1) + '" rx="' + (barWidth / 2).toFixed(1) + '" fill="' + trackBg + '"/>',
      '<rect x="' + (center - barWidth / 2).toFixed(1) + '" y="' + fillTop.toFixed(1) + '" width="' + barWidth.toFixed(1) + '" height="' + fillHeight.toFixed(1) + '" rx="' + (barWidth / 2).toFixed(1) + '" fill="' + fill + '"/>',
      '<text x="' + center.toFixed(1) + '" y="' + (baseline + 17).toFixed(1) + '" text-anchor="middle" fill="' + label + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="8" font-weight="700" letter-spacing=".35">' + escapeXml(displayCode(language.code)) + '</text>',
      '</g>'
    ].join('\n')
  }).join('\n')
}

const makePulse = ({ languages, globalPercent, stringCount, isDark, mobile }) => {
  const width = mobile ? 720 : 1040
  const columns = mobile ? 12 : 18
  const rows = Math.ceil(languages.length / columns)
  const chartHeight = mobile ? 44 : 54
  const rowGap = mobile ? 72 : 85
  const chartStartY = 126
  const chartStartX = mobile ? 34 : 44
  const chartWidth = width - chartStartX * 2
  const lastBaseline = chartStartY + (rows - 1) * rowGap + chartHeight
  const height = lastBaseline + 66
  const text = isDark ? '#F0F6FC' : '#1F2328'
  const sub = isDark ? '#8B949E' : '#57606A'
  const border = isDark ? '#30363D' : '#D0D7DE'
  const accent = isDark ? '#2DD4BF' : '#0F766E'
  const languageCount = languages.length
  const roundedPercent = Math.round(globalPercent)
  const aria = 'Levyra Android translations: ' + roundedPercent + '% coverage across ' + languageCount + ' supported languages and ' + stringCount + ' translatable strings'

  const chart = makeBars({
    languages,
    columns,
    startX: chartStartX,
    startY: chartStartY,
    width: chartWidth,
    chartHeight,
    rowGap,
    isDark
  })

  return [
    '<svg xmlns="http://www.w3.org/2000/svg" width="' + width + '" height="' + height + '" viewBox="0 0 ' + width + ' ' + height + '" role="img" aria-label="' + escapeXml(aria) + '">',
    '<title>' + escapeXml(aria) + '</title>',
    '<line x1="24" y1="16" x2="' + (width - 24) + '" y2="16" stroke="' + border + '" stroke-width="1"/>',
    '<rect x="' + (width / 2 - 62) + '" y="30" width="124" height="28" rx="7" fill="' + accent + '" opacity=".14"/>',
    '<text x="' + (width / 2) + '" y="49" text-anchor="middle" fill="' + accent + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="10.5" font-weight="800" letter-spacing="1.4">TRANSLATED ' + roundedPercent + '%</text>',
    '<text x="' + (width / 2) + '" y="83" text-anchor="middle" fill="' + text + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="' + (mobile ? 18 : 20) + '" font-weight="800">Help Levyra speak your language.</text>',
    '<text x="' + (width / 2) + '" y="103" text-anchor="middle" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="10.5">' + languageCount + ' languages · Android coverage complete</text>',
    chart,
    '<text x="' + (width / 2) + '" y="' + (height - 16) + '" text-anchor="middle" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="10.5">Weblate stays open for reviews, improvements and future strings.</text>',
    '</svg>'
  ].join('\n')
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
