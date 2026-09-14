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
    fil: 'FIL'
  }
  return aliases[normalized] || normalized.split('-')[0].slice(0, 3).toUpperCase()
}

const statusColor = (percent, isDark) => {
  if (percent >= 99.5) return isDark ? '#2DD4BF' : '#0F766E'
  if (percent >= 80) return isDark ? '#60A5FA' : '#2563EB'
  if (percent >= 50) return isDark ? '#A78BFA' : '#7C3AED'
  return isDark ? '#FB7185' : '#E11D48'
}

const makeBars = ({ languages, columns, startX, startY, width, chartHeight, rowGap, isDark }) => {
  const rows = Math.ceil(languages.length / columns)
  const step = width / columns
  const barWidth = Math.max(8, Math.min(14, step * 0.34))
  const track = isDark ? '#242B35' : '#E7EBF0'
  const label = isDark ? '#A7B0BD' : '#57606A'
  const percent = isDark ? '#778191' : '#8C959F'
  return {
    rows,
    content: languages.map((language, index) => {
      const row = Math.floor(index / columns)
      const col = index % columns
      const center = startX + step * col + step / 2
      const baseline = startY + row * rowGap + chartHeight
      const fillHeight = Math.max(5, chartHeight * language.percent / 100)
      const fillTop = baseline - fillHeight
      return [
        '<g>',
        '<title>' + escapeXml(language.name) + ' — ' + Math.round(language.percent) + '%</title>',
        '<rect x="' + (center - barWidth / 2).toFixed(1) + '" y="' + (baseline - chartHeight).toFixed(1) + '" width="' + barWidth.toFixed(1) + '" height="' + chartHeight.toFixed(1) + '" rx="' + (barWidth / 2).toFixed(1) + '" fill="' + track + '"/>',
        '<rect x="' + (center - barWidth / 2).toFixed(1) + '" y="' + fillTop.toFixed(1) + '" width="' + barWidth.toFixed(1) + '" height="' + fillHeight.toFixed(1) + '" rx="' + (barWidth / 2).toFixed(1) + '" fill="' + statusColor(language.percent, isDark) + '"/>',
        '<text x="' + center.toFixed(1) + '" y="' + (fillTop - 8).toFixed(1) + '" text-anchor="middle" fill="' + percent + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="8.5" font-weight="700">' + Math.round(language.percent) + '</text>',
        '<text x="' + center.toFixed(1) + '" y="' + (baseline + 20).toFixed(1) + '" text-anchor="middle" fill="' + label + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="9.5" font-weight="800" letter-spacing=".5">' + escapeXml(displayCode(language.code)) + '</text>',
        '</g>'
      ].join('\n')
    }).join('\n')
  }
}

const makePulse = ({ languages, globalPercent, stringCount, generatedAt, isDark, mobile }) => {
  const width = mobile ? 720 : 1200
  const columns = mobile ? 13 : Math.max(1, languages.length)
  const chartHeight = mobile ? 64 : 112
  const rowGap = mobile ? 116 : 0
  const chartStartY = mobile ? 135 : 124
  const chart = makeBars({
    languages,
    columns,
    startX: mobile ? 34 : 42,
    startY: chartStartY,
    width: width - (mobile ? 68 : 84),
    chartHeight,
    rowGap,
    isDark
  })
  const height = mobile ? 410 : 320
  const bg = isDark ? '#0D1117' : '#FFFFFF'
  const border = isDark ? '#30363D' : '#D0D7DE'
  const text = isDark ? '#F0F6FC' : '#1F2328'
  const sub = isDark ? '#8B949E' : '#57606A'
  const accent = isDark ? '#A78BFA' : '#7C3AED'
  const line = isDark ? '#242B35' : '#E7EBF0'
  const languageCount = languages.length
  const aria = 'Levyra Translation Pulse: ' + Math.round(globalPercent) + '% translated across ' + languageCount + ' languages and ' + stringCount + ' strings'
  const footerY = height - 27

  return [
    '<svg xmlns="http://www.w3.org/2000/svg" width="' + width + '" height="' + height + '" viewBox="0 0 ' + width + ' ' + height + '" role="img" aria-label="' + escapeXml(aria) + '" data-generated-at="' + escapeXml(generatedAt) + '">',
    '<title>' + escapeXml(aria) + '</title>',
    '<rect x="1" y="1" width="' + (width - 2) + '" height="' + (height - 2) + '" rx="18" fill="' + bg + '" stroke="' + border + '" stroke-width="1.5"/>',
    '<rect x="32" y="28" width="3" height="30" rx="1.5" fill="' + accent + '"/>',
    '<text x="48" y="45" fill="' + text + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="14" font-weight="800" letter-spacing="1.7">TRANSLATION PULSE</text>',
    '<text x="48" y="69" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="11">Help Levyra speak your language.</text>',
    '<rect x="' + (width - (mobile ? 194 : 208)) + '" y="27" width="' + (mobile ? 162 : 176) + '" height="38" rx="9" fill="' + (isDark ? '#161B22' : '#F6F8FA') + '" stroke="' + border + '"/>',
    '<text x="' + (width - (mobile ? 180 : 194)) + '" y="51" fill="' + accent + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="19" font-weight="800">' + Math.round(globalPercent) + '%</text>',
    '<text x="' + (width - (mobile ? 128 : 142)) + '" y="50" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="9.5" font-weight="800" letter-spacing="1">TRANSLATED</text>',
    '<text x="48" y="100" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="9.5" font-weight="800" letter-spacing="1">LANGUAGES ' + languageCount + '   ·   STRINGS ' + stringCount + '   ·   LIVE WEBLATE</text>',
    '<line x1="32" y1="111" x2="' + (width - 32) + '" y2="111" stroke="' + line + '" stroke-width="1"/>',
    chart.content,
    '<line x1="32" y1="' + (footerY - 17) + '" x2="' + (width - 32) + '" y2="' + (footerY - 17) + '" stroke="' + line + '" stroke-width="1"/>',
    '<text x="32" y="' + footerY + '" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="9.5">Every translation brings Levyra closer to someone, somewhere.</text>',
    '<text x="' + (width - 32) + '" y="' + footerY + '" text-anchor="end" fill="' + accent + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="9.5" font-weight="800">HELP TRANSLATE →</text>',
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
