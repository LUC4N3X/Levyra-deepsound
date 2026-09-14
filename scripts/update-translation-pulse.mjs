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
  const barWidth = Math.max(5, Math.min(8, step * 0.28))
  const trackBg = isDark ? '#161B22' : '#EAEEF2'

  return languages.map((language, index) => {
    const row = Math.floor(index / columns)
    const col = index % columns
    const center = startX + step * col + step / 2
    const rowStartY = startY + row * rowGap
    const baseline = rowStartY + chartHeight
    const fillHeight = Math.max(3, (chartHeight * language.percent) / 100)
    const fillTop = baseline - fillHeight
    const isComplete = language.percent >= 99.5
    const barFill = isComplete
      ? (isDark ? '#2DD4BF' : '#0D9488')
      : (isDark ? '#818CF8' : '#6366F1')
    const labelColor = isDark ? '#8B949E' : '#57606A'
    const labelWeight = isComplete ? '800' : '600'
    const percentColor = isDark ? '#6E7681' : '#8C959F'

    return [
      '<g>',
      '<title>' + escapeXml(language.name) + ' — ' + Math.round(language.percent) + '% (' + escapeXml(displayCode(language.code)) + ')</title>',
      '<text x="' + center.toFixed(1) + '" y="' + (rowStartY - 6).toFixed(1) + '" text-anchor="middle" fill="' + (isComplete ? (isDark ? '#2DD4BF' : '#0D9488') : percentColor) + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="8" font-weight="' + (isComplete ? '800' : '600') + '">' + Math.round(language.percent) + '%</text>',
      '<rect x="' + (center - barWidth / 2).toFixed(1) + '" y="' + (baseline - chartHeight).toFixed(1) + '" width="' + barWidth.toFixed(1) + '" height="' + chartHeight.toFixed(1) + '" rx="' + (barWidth / 2).toFixed(1) + '" fill="' + trackBg + '"/>',
      '<rect x="' + (center - barWidth / 2).toFixed(1) + '" y="' + fillTop.toFixed(1) + '" width="' + barWidth.toFixed(1) + '" height="' + fillHeight.toFixed(1) + '" rx="' + (barWidth / 2).toFixed(1) + '" fill="' + barFill + '"/>',
      '<text x="' + center.toFixed(1) + '" y="' + (baseline + 16).toFixed(1) + '" text-anchor="middle" fill="' + (isComplete ? (isDark ? '#E6EDF3' : '#1F2328') : labelColor) + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="8.5" font-weight="' + labelWeight + '" letter-spacing=".4">' + escapeXml(displayCode(language.code)) + '</text>',
      '</g>'
    ].join('\n')
  }).join('\n')
}

const makePulse = ({ languages, globalPercent, stringCount, generatedAt, isDark, mobile }) => {
  const width = mobile ? 720 : 1200
  const height = mobile ? 460 : 310
  const bg = isDark ? '#0B0F17' : '#FFFFFF'
  const border = isDark ? '#262C36' : '#D0D7DE'
  const text = isDark ? '#E6EDF3' : '#1F2328'
  const sub = isDark ? '#7D8590' : '#656D76'
  const accent = isDark ? '#818CF8' : '#6366F1'
  const line = isDark ? '#1C2128' : '#EAEEF2'
  const vinylBg = isDark ? '#12161F' : '#F1F4F8'
  const vinylGroove = isDark ? '#1B222E' : '#E2E7ED'
  const languageCount = languages.length
  const completedCount = languages.filter(l => l.percent >= 99.5).length
  const inProgressCount = languageCount - completedCount
  const roundedPercent = Math.round(globalPercent)
  const aria = 'Levyra Translation Pulse: ' + roundedPercent + '% translated across ' + languageCount + ' languages and ' + stringCount + ' strings'

  const circum = 2 * Math.PI * 62
  const strokeDash = (circum * roundedPercent) / 100

  const turntable = [
    '<g transform="translate(' + (mobile ? 40 : 48) + ', ' + (mobile ? 95 : 92) + ')">',
    '  <circle cx="68" cy="68" r="66" fill="' + vinylBg + '" stroke="' + border + '" stroke-width="1.2"/>',
    '  <circle cx="68" cy="68" r="54" fill="none" stroke="' + vinylGroove + '" stroke-width="1"/>',
    '  <circle cx="68" cy="68" r="42" fill="none" stroke="' + vinylGroove + '" stroke-width="1"/>',
    '  <circle cx="68" cy="68" r="62" fill="none" stroke="' + (isDark ? '#1C2330' : '#E2E8F0') + '" stroke-width="3"/>',
    '  <circle cx="68" cy="68" r="62" fill="none" stroke="' + accent + '" stroke-width="3" stroke-dasharray="' + strokeDash.toFixed(1) + ' ' + (circum - strokeDash).toFixed(1) + '" stroke-linecap="round" transform="rotate(-90 68 68)"/>',
    '  <circle cx="68" cy="68" r="26" fill="' + (isDark ? '#161B24' : '#EAEFF4') + '" stroke="' + border + '" stroke-width="1"/>',
    '  <circle cx="68" cy="68" r="4" fill="' + accent + '"/>',
    '  <text x="68" y="65" text-anchor="middle" fill="' + text + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="14" font-weight="800">' + roundedPercent + '%</text>',
    '  <text x="68" y="78" text-anchor="middle" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="7.5" font-weight="700" letter-spacing=".5">SYNC</text>',
    '</g>',
    '<g transform="translate(' + (mobile ? 195 : 205) + ', ' + (mobile ? 110 : 108) + ')">',
    '  <text x="0" y="14" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="8.5" font-weight="700" letter-spacing=".8">LISTENING PULSE</text>',
    '  <text x="0" y="36" fill="' + text + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="16" font-weight="800">' + completedCount + ' Complete · ' + inProgressCount + ' In Progress</text>',
    '  <text x="0" y="55" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="10">Global community translations synced directly from Weblate.</text>',
    '</g>'
  ].join('\n')

  const columns = mobile ? 13 : Math.max(1, languages.length)
  const chartHeight = mobile ? 50 : 64
  const rowGap = mobile ? 88 : 0
  const chartStartY = mobile ? 260 : 190
  const chartStartX = mobile ? 32 : 48
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

  const footerY = height - 20

  return [
    '<svg xmlns="http://www.w3.org/2000/svg" width="' + width + '" height="' + height + '" viewBox="0 0 ' + width + ' ' + height + '" role="img" aria-label="' + escapeXml(aria) + '" data-generated-at="' + escapeXml(generatedAt) + '">',
    '<title>' + escapeXml(aria) + '</title>',
    '<rect x="1" y="1" width="' + (width - 2) + '" height="' + (height - 2) + '" rx="14" fill="' + bg + '" stroke="' + border + '" stroke-width="1"/>',
    '<g transform="translate(' + (mobile ? 28 : 48) + ', 26)">',
    '  <rect x="0" y="6" width="2.5" height="16" rx="1.25" fill="' + accent + '"/>',
    '  <rect x="5.5" y="1" width="2.5" height="21" rx="1.25" fill="' + accent + '"/>',
    '  <rect x="11" y="9" width="2.5" height="13" rx="1.25" fill="' + accent + '"/>',
    '  <text x="22" y="13" fill="' + text + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="13" font-weight="800" letter-spacing="1">TRANSLATION PULSE</text>',
    '  <text x="22" y="27" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="10.5">Community Localization · ' + languageCount + ' Languages · ' + stringCount + ' Strings</text>',
    '</g>',
    '<g transform="translate(' + (width - (mobile ? 28 : 48) - 150) + ', 24)">',
    '  <rect x="0" y="0" width="150" height="34" rx="8" fill="' + (isDark ? '#12161F' : '#F6F8FA') + '" stroke="' + border + '" stroke-width="1"/>',
    '  <text x="14" y="22" fill="' + accent + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="15" font-weight="800">' + roundedPercent + '%</text>',
    '  <text x="56" y="21" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="8.5" font-weight="700" letter-spacing=".8">TRANSLATED</text>',
    '</g>',
    '<line x1="' + (mobile ? 28 : 48) + '" y1="74" x2="' + (width - (mobile ? 28 : 48)) + '" y2="74" stroke="' + line + '" stroke-width="1"/>',
    turntable,
    '<line x1="' + (mobile ? 28 : 48) + '" y1="' + (chartStartY - (mobile ? 24 : 20)) + '" x2="' + (width - (mobile ? 28 : 48)) + '" y2="' + (chartStartY - (mobile ? 24 : 20)) + '" stroke="' + line + '" stroke-width="1"/>',
    chart,
    '<line x1="' + (mobile ? 28 : 48) + '" y1="' + (footerY - 16) + '" x2="' + (width - (mobile ? 28 : 48)) + '" y2="' + (footerY - 16) + '" stroke="' + line + '" stroke-width="1"/>',
    '<text x="' + (mobile ? 28 : 48) + '" y="' + footerY + '" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="9.5">Every translation brings Levyra closer to someone, somewhere.</text>',
    '<text x="' + (width - (mobile ? 28 : 48)) + '" y="' + footerY + '" text-anchor="end" fill="' + accent + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="9" font-weight="800" letter-spacing=".4">HELP TRANSLATE →</text>',
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
