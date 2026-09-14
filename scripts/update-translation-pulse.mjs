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
  const rawName = typeof row.language === 'string' ? row.language : row.language?.name
  const name = rawName || row.code || 'Unknown'
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
    'fil': 'FIL'
  }
  return aliases[normalized] || normalized.split('-')[0].slice(0, 3).toUpperCase()
}

const statusColor = (percent, isDark) => {
  if (percent >= 99.5) return isDark ? '#2DD4BF' : '#0F766E'
  if (percent >= 85) return isDark ? '#60A5FA' : '#2563EB'
  if (percent >= 60) return isDark ? '#A78BFA' : '#7C3AED'
  return isDark ? '#FB7185' : '#E11D48'
}

const makeBarGrid = ({ languages, columns, x, y, width, chartHeight, rowHeight, isDark, codeSize, percentSize }) => {
  const rows = Math.ceil(languages.length / columns)
  const step = width / columns
  const trackWidth = Math.min(26, step * 0.48)
  const track = isDark ? '#21262D' : '#EAEFF4'
  const codeColor = isDark ? '#C9D1D9' : '#39414D'
  const percentColor = isDark ? '#8B949E' : '#6E7781'
  const groups = languages.map((language, index) => {
    const row = Math.floor(index / columns)
    const column = index % columns
    const center = x + step * column + step / 2
    const baseline = y + row * rowHeight + chartHeight
    const height = Math.max(7, chartHeight * language.percent / 100)
    const top = baseline - height
    const code = displayCode(language.code)
    return [
      '<g>',
      '<title>' + escapeXml(language.name) + ' — ' + Math.round(language.percent) + '%</title>',
      '<rect x="' + (center - trackWidth / 2).toFixed(1) + '" y="' + (baseline - chartHeight).toFixed(1) + '" width="' + trackWidth.toFixed(1) + '" height="' + chartHeight.toFixed(1) + '" rx="' + (trackWidth / 2).toFixed(1) + '" fill="' + track + '"/>',
      '<rect x="' + (center - trackWidth / 2).toFixed(1) + '" y="' + top.toFixed(1) + '" width="' + trackWidth.toFixed(1) + '" height="' + height.toFixed(1) + '" rx="' + (trackWidth / 2).toFixed(1) + '" fill="' + statusColor(language.percent, isDark) + '"/>',
      '<text x="' + center.toFixed(1) + '" y="' + (top - 10).toFixed(1) + '" text-anchor="middle" fill="' + percentColor + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="' + percentSize + '" font-weight="700">' + Math.round(language.percent) + '</text>',
      '<text x="' + center.toFixed(1) + '" y="' + (baseline + 25).toFixed(1) + '" text-anchor="middle" fill="' + codeColor + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="' + codeSize + '" font-weight="800" letter-spacing=".6">' + escapeXml(code) + '</text>',
      '</g>'
    ].join('\n')
  }).join('\n')
  return { groups, rows }
}

const makePulse = ({ languages, globalPercent, stringCount, generatedAt, isDark, mobile }) => {
  const width = mobile ? 720 : 1200
  const columns = mobile ? 7 : 13
  const chartHeight = mobile ? 118 : 132
  const rowHeight = mobile ? 176 : 192
  const gridX = mobile ? 38 : 58
  const gridWidth = width - gridX * 2
  const gridY = mobile ? 292 : 265
  const grid = makeBarGrid({
    languages,
    columns,
    x: gridX,
    y: gridY,
    width: gridWidth,
    chartHeight,
    rowHeight,
    isDark,
    codeSize: mobile ? 11 : 11,
    percentSize: mobile ? 10 : 10
  })
  const height = gridY + grid.rows * rowHeight + (mobile ? 64 : 58)
  const bg = isDark ? '#0D1117' : '#FFFFFF'
  const border = isDark ? '#30363D' : '#D0D7DE'
  const text = isDark ? '#F0F6FC' : '#1F2328'
  const sub = isDark ? '#8B949E' : '#57606A'
  const track = isDark ? '#21262D' : '#EAEFF4'
  const accent = isDark ? '#A78BFA' : '#7C3AED'
  const complete = isDark ? '#2DD4BF' : '#0F766E'
  const translatedWidth = Math.max(0, Math.min(width - 116, (width - 116) * globalPercent / 100))
  const titleSize = mobile ? 17 : 18
  const percentSize = mobile ? 56 : 64
  const languageCount = languages.length
  const aria = 'Levyra Translation Pulse: ' + Math.round(globalPercent) + '% translated across ' + languageCount + ' languages and ' + stringCount + ' strings'
  const metricY = mobile ? 190 : 134
  const progressY = mobile ? 235 : 178
  const gridLabelY = mobile ? 270 : 239

  return [
    '<svg xmlns="http://www.w3.org/2000/svg" width="' + width + '" height="' + height + '" viewBox="0 0 ' + width + ' ' + height + '" role="img" aria-label="' + escapeXml(aria) + '" data-generated-at="' + escapeXml(generatedAt) + '">',
    '<title>' + escapeXml(aria) + '</title>',
    '<rect x="1" y="1" width="' + (width - 2) + '" height="' + (height - 2) + '" rx="26" fill="' + bg + '" stroke="' + border + '" stroke-width="2"/>',
    '<rect x="28" y="30" width="4" height="46" rx="2" fill="' + accent + '"/>',
    '<text x="48" y="51" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="' + titleSize + '" font-weight="800" letter-spacing="2.2">TRANSLATION PULSE</text>',
    '<text x="48" y="' + (mobile ? 126 : 126) + '" fill="' + text + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="' + percentSize + '" font-weight="800">' + Math.round(globalPercent) + '%</text>',
    '<text x="50" y="' + (mobile ? 151 : 153) + '" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="12" font-weight="700" letter-spacing="1.7">GLOBAL TRANSLATED</text>',
    '<text x="' + (mobile ? 48 : 760) + '" y="' + metricY + '" fill="' + text + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="' + (mobile ? 20 : 22) + '" font-weight="800">' + languageCount + '</text>',
    '<text x="' + (mobile ? 78 : 797) + '" y="' + metricY + '" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="11" font-weight="800" letter-spacing="1.2">LANGUAGES</text>',
    '<text x="' + (mobile ? 250 : 925) + '" y="' + metricY + '" fill="' + text + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="' + (mobile ? 20 : 22) + '" font-weight="800">' + stringCount + '</text>',
    '<text x="' + (mobile ? 286 : 968) + '" y="' + metricY + '" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="11" font-weight="800" letter-spacing="1.2">STRINGS</text>',
    '<circle cx="' + (mobile ? 492 : 1080) + '" cy="' + (metricY - 7) + '" r="5" fill="' + complete + '"/>',
    '<text x="' + (mobile ? 507 : 1094) + '" y="' + metricY + '" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="11" font-weight="800" letter-spacing="1.2">LIVE WEBLATE</text>',
    '<rect x="58" y="' + progressY + '" width="' + (width - 116) + '" height="8" rx="4" fill="' + track + '"/>',
    '<rect x="58" y="' + progressY + '" width="' + translatedWidth.toFixed(1) + '" height="8" rx="4" fill="' + accent + '"/>',
    '<text x="58" y="' + gridLabelY + '" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="11" font-weight="800" letter-spacing="1.3">COMPLETION BY LANGUAGE</text>',
    '<text x="' + (width - 58) + '" y="' + gridLabelY + '" text-anchor="end" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="10">TEAL 100 · BLUE 85+ · VIOLET 60+ · CORAL &lt;60</text>',
    grid.groups,
    '<text x="' + (width / 2) + '" y="' + (height - 28) + '" text-anchor="middle" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="11">Live community translation status · hosted.weblate.org/engage/levyra</text>',
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
    const languages = (languageResponse.results ?? [])
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
