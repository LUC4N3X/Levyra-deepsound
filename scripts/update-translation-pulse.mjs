import { mkdir, readFile, writeFile } from 'node:fs/promises'

const WEBLATE_BASE = 'https://hosted.weblate.org'
const PROJECT = 'levyra'
const REFRESH_MS = 6 * 60 * 60 * 1000
const ASSET_DIR = 'docs/assets'
const DARK_ASSET = ASSET_DIR + '/levyra-translation-pulse.svg'

const escapeXml = value => String(value)
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&apos;')

const requestJson = async path => {
  const response = await fetch(WEBLATE_BASE + path, {
    headers: {
      Accept: 'application/json',
      'User-Agent': 'Levyra-Translation-Pulse-Updater'
    },
    signal: AbortSignal.timeout(15000)
  })
  if (!response.ok) throw new Error('Weblate API request failed with ' + response.status + ': ' + await response.text())
  return response.json()
}

const readGeneratedAt = async () => {
  try {
    const content = await readFile(DARK_ASSET, 'utf8')
    const match = content.match(/data-generated-at="([^"]+)"/)
    if (!match) return 0
    const timestamp = Date.parse(match[1])
    return Number.isFinite(timestamp) ? timestamp : 0
  } catch {
    return 0
  }
}

const shouldRefresh = async () => {
  if (process.env.FORCE_TRANSLATION_PULSE === '1') return true
  const generatedAt = await readGeneratedAt()
  return generatedAt === 0 || Date.now() - generatedAt >= REFRESH_MS
}

const normalizeLanguage = row => {
  const rawName = (typeof row.name === 'string' && row.name.trim())
    || (typeof row.language === 'string' && row.language.trim())
    || (row.language?.name)
    || row.code
    || 'Unknown'
  const name = rawName
  const code = row.code || row.language?.code || name
  const percent = Number(row.translated_percent ?? 0)
  const total = Number(row.total ?? 0)
  return {
    name: String(name),
    code: String(code),
    percent: Number.isFinite(percent) ? Math.max(0, Math.min(100, percent)) : 0,
    total: Number.isFinite(total) ? Math.max(0, total) : 0
  }
}

const displayCode = code => {
  const normalized = String(code).replaceAll('_', '-').toLowerCase()
  const aliases = {
    'zh-hans': 'ZH',
    'zh-hant': 'ZH-TW',
    'pt-br': 'PT-BR',
    fil: 'FIL'
  }
  return aliases[normalized] || normalized.split('-')[0].slice(0, 3).toUpperCase()
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

const makePulse = ({ languages, globalPercent, stringCount, generatedAt, isDark, mobile }) => {
  const width = mobile ? 720 : 1040
  const height = mobile ? 330 : 250
  const text = isDark ? '#F0F6FC' : '#1F2328'
  const sub = isDark ? '#8B949E' : '#57606A'
  const border = isDark ? '#30363D' : '#D0D7DE'
  const accent = isDark ? '#818CF8' : '#6366F1'
  const languageCount = languages.length
  const roundedPercent = Math.round(globalPercent)
  const aria = 'Levyra translations: ' + roundedPercent + '% translated across ' + languageCount + ' languages and ' + stringCount + ' strings'

  const columns = mobile ? Math.ceil(languageCount / 2) : Math.max(1, languageCount)
  const chartHeight = mobile ? 58 : 82
  const rowGap = mobile ? 98 : 0
  const chartStartY = mobile ? 135 : 122
  const chartStartX = mobile ? 34 : 44
  const chartWidth = width - chartStartX * 2

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
    '<svg xmlns="http://www.w3.org/2000/svg" width="' + width + '" height="' + height + '" viewBox="0 0 ' + width + ' ' + height + '" role="img" aria-label="' + escapeXml(aria) + '" data-generated-at="' + escapeXml(generatedAt) + '">',
    '<title>' + escapeXml(aria) + '</title>',
    '<line x1="24" y1="16" x2="' + (width - 24) + '" y2="16" stroke="' + border + '" stroke-width="1"/>',
    '<rect x="' + (width / 2 - 58) + '" y="30" width="116" height="28" rx="7" fill="' + accent + '" opacity=".14"/>',
    '<text x="' + (width / 2) + '" y="49" text-anchor="middle" fill="' + accent + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="10.5" font-weight="800" letter-spacing="1.4">TRANSLATED ' + roundedPercent + '%</text>',
    '<text x="' + (width / 2) + '" y="83" text-anchor="middle" fill="' + text + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="' + (mobile ? 18 : 20) + '" font-weight="800">Help Levyra speak your language.</text>',
    '<text x="' + (width / 2) + '" y="103" text-anchor="middle" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="10.5">' + languageCount + ' languages · live on Weblate</text>',
    chart,
    '<text x="' + (width / 2) + '" y="' + (height - 14) + '" text-anchor="middle" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="10.5">Every translation brings Levyra closer to more listeners.</text>',
    '</svg>'
  ].join('\n')
}

const updateTranslationPulse = async () => {
  if (!await shouldRefresh()) return
  try {
    const [stats, languageResponse] = await Promise.all([
      requestJson('/api/projects/' + PROJECT + '/statistics/'),
      requestJson('/api/projects/' + PROJECT + '/languages/?page_size=100')
    ])
    const rows = Array.isArray(languageResponse) ? languageResponse : languageResponse.results ?? []
    const languages = rows
      .map(normalizeLanguage)
      .filter(language => language.code)
      .sort((a, b) => b.percent - a.percent || a.name.localeCompare(b.name))
    if (languages.length === 0) throw new Error('Weblate returned no languages')
    const globalPercent = Number(stats.translated_percent)
    if (!Number.isFinite(globalPercent)) throw new Error('Weblate returned an invalid translated percentage')
    const stringCount = Math.max(...languages.map(language => language.total), 0)
    const generatedAt = new Date().toISOString()

    await mkdir(ASSET_DIR, { recursive: true })
    await Promise.all([
      writeFile(ASSET_DIR + '/levyra-translation-pulse.svg', makePulse({ languages, globalPercent, stringCount, generatedAt, isDark: true, mobile: false }), 'utf8'),
      writeFile(ASSET_DIR + '/levyra-translation-pulse-light.svg', makePulse({ languages, globalPercent, stringCount, generatedAt, isDark: false, mobile: false }), 'utf8'),
      writeFile(ASSET_DIR + '/levyra-translation-pulse-mobile.svg', makePulse({ languages, globalPercent, stringCount, generatedAt, isDark: true, mobile: true }), 'utf8'),
      writeFile(ASSET_DIR + '/levyra-translation-pulse-mobile-light.svg', makePulse({ languages, globalPercent, stringCount, generatedAt, isDark: false, mobile: true }), 'utf8')
    ])
  } catch (error) {
    console.warn('Translation Pulse update skipped: ' + (error instanceof Error ? error.message : String(error)))
  }
}

await updateTranslationPulse()
