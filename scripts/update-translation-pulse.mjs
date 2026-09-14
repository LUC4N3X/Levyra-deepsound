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

const makeGridLines = ({ startX, width, chartStartY, chartHeight, isDark }) => {
  const gridLineColor = isDark ? '#1C2536' : '#E2E8F0'
  const gridTextColor = isDark ? '#4B5565' : '#94A3B8'
  const baseline = chartStartY + chartHeight
  const endX = startX + width
  const levels = [
    { label: '0 dB', y: chartStartY },
    { label: '-3 dB', y: chartStartY + chartHeight * 0.25 },
    { label: '-6 dB', y: chartStartY + chartHeight * 0.5 },
    { label: '-12 dB', y: chartStartY + chartHeight * 0.75 }
  ]

  const lines = levels.map(lvl => [
    '<line x1="' + startX.toFixed(1) + '" y1="' + lvl.y.toFixed(1) + '" x2="' + endX.toFixed(1) + '" y2="' + lvl.y.toFixed(1) + '" stroke="' + gridLineColor + '" stroke-dasharray="2 4" stroke-width="1"/>',
    '<text x="' + (startX - 8).toFixed(1) + '" y="' + (lvl.y + 3).toFixed(1) + '" text-anchor="end" fill="' + gridTextColor + '" font-family="ui-monospace,SFMono-Regular,Consolas,monospace" font-size="7" font-weight="700">' + lvl.label + '</text>'
  ].join('\n')).join('\n')

  const baseLine = [
    '<line x1="' + startX.toFixed(1) + '" y1="' + baseline.toFixed(1) + '" x2="' + endX.toFixed(1) + '" y2="' + baseline.toFixed(1) + '" stroke="' + gridLineColor + '" stroke-width="1.2"/>',
    '<text x="' + (startX - 8).toFixed(1) + '" y="' + (baseline + 3).toFixed(1) + '" text-anchor="end" fill="' + gridTextColor + '" font-family="ui-monospace,SFMono-Regular,Consolas,monospace" font-size="7" font-weight="700">-∞</text>'
  ].join('\n')

  return lines + '\n' + baseLine
}

const makeBars = ({ languages, columns, startX, startY, width, chartHeight, rowGap, isDark }) => {
  const rows = Math.ceil(languages.length / columns)
  const step = width / columns
  const barWidth = Math.max(9, Math.min(14, step * 0.32))
  const trackBg = isDark ? '#121824' : '#F1F5F9'
  const trackStroke = isDark ? '#1E283A' : '#E2E8F0'

  const gridSections = []
  for (let r = 0; r < rows; r++) {
    const rowY = startY + r * rowGap
    gridSections.push(makeGridLines({ startX, width, chartStartY: rowY, chartHeight, isDark }))
  }

  const bars = languages.map((language, index) => {
    const row = Math.floor(index / columns)
    const col = index % columns
    const center = startX + step * col + step / 2
    const rowStartY = startY + row * rowGap
    const baseline = rowStartY + chartHeight
    const fillHeight = Math.max(5, (chartHeight * language.percent) / 100)
    const fillTop = baseline - fillHeight
    const isComplete = language.percent >= 99.5
    const hudY = rowStartY - 9
    const hudColor = isComplete
      ? (isDark ? '#2DD4BF' : '#0D9488')
      : (isDark ? '#8A95A5' : '#64748B')
    const hudWeight = isComplete ? '800' : '700'
    const peakColor = isComplete
      ? (isDark ? '#E0F2FE' : '#0284C7')
      : (isDark ? '#C084FC' : '#7C3AED')
    const labelColor = isComplete
      ? (isDark ? '#F0F6FC' : '#0F172A')
      : (isDark ? '#8A95A5' : '#64748B')
    const labelWeight = isComplete ? '800' : '700'

    const elements = [
      '<g>',
      '<title>' + escapeXml(language.name) + ' — ' + Math.round(language.percent) + '% (' + escapeXml(displayCode(language.code)) + ')</title>',
      '<text x="' + center.toFixed(1) + '" y="' + hudY.toFixed(1) + '" text-anchor="middle" fill="' + hudColor + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="8" font-weight="' + hudWeight + '">' + Math.round(language.percent) + '%</text>',
      '<rect x="' + (center - barWidth / 2).toFixed(1) + '" y="' + (baseline - chartHeight).toFixed(1) + '" width="' + barWidth.toFixed(1) + '" height="' + chartHeight.toFixed(1) + '" rx="' + (barWidth / 2).toFixed(1) + '" fill="' + trackBg + '" stroke="' + trackStroke + '" stroke-width="1"/>',
      '<rect x="' + (center - barWidth / 2).toFixed(1) + '" y="' + fillTop.toFixed(1) + '" width="' + barWidth.toFixed(1) + '" height="' + fillHeight.toFixed(1) + '" rx="' + (barWidth / 2).toFixed(1) + '" fill="url(#eqGrad)"/>',
      '<rect x="' + (center - barWidth / 2).toFixed(1) + '" y="' + (fillTop - 1).toFixed(1) + '" width="' + barWidth.toFixed(1) + '" height="2.2" rx="1.1" fill="' + peakColor + '"/>',
      '<text x="' + center.toFixed(1) + '" y="' + (baseline + 18).toFixed(1) + '" text-anchor="middle" fill="' + labelColor + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="9" font-weight="' + labelWeight + '" letter-spacing=".5">' + escapeXml(displayCode(language.code)) + '</text>'
    ]

    if (isComplete) {
      elements.push('<circle cx="' + center.toFixed(1) + '" cy="' + (baseline + 25).toFixed(1) + '" r="1.5" fill="' + (isDark ? '#2DD4BF' : '#0D9488') + '"/>')
    }

    elements.push('</g>')
    return elements.join('\n')
  }).join('\n')

  return gridSections.join('\n') + '\n' + bars
}

const makePulse = ({ languages, globalPercent, stringCount, generatedAt, isDark, mobile }) => {
  const width = mobile ? 720 : 1200
  const height = mobile ? 418 : 340
  const columns = mobile ? 13 : Math.max(1, languages.length)
  const chartHeight = mobile ? 62 : 112
  const rowGap = mobile ? 124 : 0
  const chartStartY = mobile ? 126 : 138
  const chartStartX = mobile ? 44 : 50
  const chartWidth = width - chartStartX - (mobile ? 28 : 44)

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

  const bgGradId = isDark ? 'cardBgDark' : 'cardBgLight'
  const border = isDark ? '#1E293B' : '#CBD5E1'
  const text = isDark ? '#F8FAFC' : '#0F172A'
  const sub = isDark ? '#8A95A5' : '#64748B'
  const dividerLine = isDark ? '#1C2536' : '#E2E8F0'
  const panelBg = isDark ? '#111726' : '#F8FAFC'
  const panelBorder = isDark ? '#222E42' : '#CBD5E1'
  const meterTrack = isDark ? '#1C2536' : '#E2E8F0'
  const btnBg = isDark ? '#141B29' : '#F1F5F9'
  const btnBorder = isDark ? '#26334A' : '#CBD5E1'
  const btnText = isDark ? '#A78BFA' : '#7C3AED'

  const masterWidth = mobile ? 172 : 208
  const masterHeight = 44
  const masterX = width - (mobile ? 32 : 44) - masterWidth
  const masterY = 24
  const meterBarWidth = mobile ? 82 : 108
  const meterFillWidth = Math.max(3, (meterBarWidth * globalPercent) / 100)

  const footerY = height - 23
  const languageCount = languages.length
  const aria = 'Levyra Translation Pulse: ' + Math.round(globalPercent) + '% translated across ' + languageCount + ' languages and ' + stringCount + ' strings'

  return [
    '<svg xmlns="http://www.w3.org/2000/svg" width="' + width + '" height="' + height + '" viewBox="0 0 ' + width + ' ' + height + '" role="img" aria-label="' + escapeXml(aria) + '" data-generated-at="' + escapeXml(generatedAt) + '">',
    '<title>' + escapeXml(aria) + '</title>',
    '<defs>',
    '  <linearGradient id="cardBgDark" x1="0" y1="0" x2="0" y2="1">',
    '    <stop offset="0%" stop-color="#0B0F19"/>',
    '    <stop offset="100%" stop-color="#101626"/>',
    '  </linearGradient>',
    '  <linearGradient id="cardBgLight" x1="0" y1="0" x2="0" y2="1">',
    '    <stop offset="0%" stop-color="#FFFFFF"/>',
    '    <stop offset="100%" stop-color="#F8FAFC"/>',
    '  </linearGradient>',
    '  <linearGradient id="topHighlight" x1="0" y1="0" x2="1" y2="0">',
    '    <stop offset="0%" stop-color="#6366F1" stop-opacity="0"/>',
    '    <stop offset="25%" stop-color="#8B5CF6" stop-opacity="' + (isDark ? '0.7' : '0.35') + '"/>',
    '    <stop offset="75%" stop-color="#06B6D4" stop-opacity="' + (isDark ? '0.7' : '0.35') + '"/>',
    '    <stop offset="100%" stop-color="#2DD4BF" stop-opacity="0"/>',
    '  </linearGradient>',
    '  <linearGradient id="brandGrad" x1="0" y1="0" x2="1" y2="1">',
    '    <stop offset="0%" stop-color="' + (isDark ? '#A78BFA' : '#7C3AED') + '"/>',
    '    <stop offset="100%" stop-color="' + (isDark ? '#38BDF8' : '#0284C7') + '"/>',
    '  </linearGradient>',
    '  <linearGradient id="eqGrad" x1="0" y1="1" x2="0" y2="0">',
    '    <stop offset="0%" stop-color="' + (isDark ? '#4F46E5' : '#6366F1') + '"/>',
    '    <stop offset="40%" stop-color="' + (isDark ? '#7C3AED' : '#7C3AED') + '"/>',
    '    <stop offset="75%" stop-color="' + (isDark ? '#2563EB' : '#0284C7') + '"/>',
    '    <stop offset="92%" stop-color="' + (isDark ? '#06B6D4' : '#0EA5E9') + '"/>',
    '    <stop offset="100%" stop-color="' + (isDark ? '#2DD4BF' : '#0D9488') + '"/>',
    '  </linearGradient>',
    '  <linearGradient id="meterGrad" x1="0" y1="0" x2="1" y2="0">',
    '    <stop offset="0%" stop-color="' + (isDark ? '#7C3AED' : '#6366F1') + '"/>',
    '    <stop offset="100%" stop-color="' + (isDark ? '#06B6D4' : '#0284C7') + '"/>',
    '  </linearGradient>',
    '</defs>',
    '<rect x="1" y="1" width="' + (width - 2) + '" height="' + (height - 2) + '" rx="16" fill="url(#' + bgGradId + ')" stroke="' + border + '" stroke-width="1.2"/>',
    '<line x1="28" y1="1.5" x2="' + (width - 28) + '" y2="1.5" stroke="url(#topHighlight)" stroke-width="1.5"/>',
    '<g transform="translate(' + (mobile ? 32 : 44) + ', 27)">',
    '  <rect x="0" y="8" width="3" height="18" rx="1.5" fill="url(#brandGrad)"/>',
    '  <rect x="6" y="2" width="3" height="24" rx="1.5" fill="url(#brandGrad)"/>',
    '  <rect x="12" y="11" width="3" height="15" rx="1.5" fill="url(#brandGrad)"/>',
    '  <rect x="18" y="5" width="3" height="21" rx="1.5" fill="url(#brandGrad)"/>',
    '</g>',
    '<text x="' + (mobile ? 62 : 74) + '" y="42" fill="' + text + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="13" font-weight="900" letter-spacing="2">TRANSLATION PULSE</text>',
    '<text x="' + (mobile ? 62 : 74) + '" y="58" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="11">Help Levyra speak your language · Frequency of global voices</text>',
    '<g transform="translate(' + masterX + ', ' + masterY + ')">',
    '  <rect x="0" y="0" width="' + masterWidth + '" height="' + masterHeight + '" rx="10" fill="' + panelBg + '" stroke="' + panelBorder + '" stroke-width="1.2"/>',
    '  <text x="14" y="16" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="7.5" font-weight="800" letter-spacing="1.2">GLOBAL HARMONY</text>',
    '  <text x="14" y="36" fill="url(#brandGrad)" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="18" font-weight="900">' + Math.round(globalPercent) + '%</text>',
    '  <text x="' + (masterWidth - meterBarWidth - 19) + '" y="24" text-anchor="end" fill="' + sub + '" font-family="ui-monospace,monospace" font-size="6.5" font-weight="800">L</text>',
    '  <rect x="' + (masterWidth - meterBarWidth - 14) + '" y="20" width="' + meterBarWidth + '" height="3.5" rx="1.75" fill="' + meterTrack + '"/>',
    '  <rect x="' + (masterWidth - meterBarWidth - 14) + '" y="20" width="' + meterFillWidth.toFixed(1) + '" height="3.5" rx="1.75" fill="url(#meterGrad)"/>',
    '  <text x="' + (masterWidth - meterBarWidth - 19) + '" y="34" text-anchor="end" fill="' + sub + '" font-family="ui-monospace,monospace" font-size="6.5" font-weight="800">R</text>',
    '  <rect x="' + (masterWidth - meterBarWidth - 14) + '" y="30" width="' + meterBarWidth + '" height="3.5" rx="1.75" fill="' + meterTrack + '"/>',
    '  <rect x="' + (masterWidth - meterBarWidth - 14) + '" y="30" width="' + meterFillWidth.toFixed(1) + '" height="3.5" rx="1.75" fill="url(#meterGrad)"/>',
    '  <text x="' + (masterWidth - 14) + '" y="41" text-anchor="end" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="6.5" font-weight="800" letter-spacing="1">CALIBRATED</text>',
    '</g>',
    '<g transform="translate(' + (mobile ? 32 : 44) + ', 86)">',
    '  <circle cx="5" cy="5" r="5" fill="#10B981" opacity="0.25"/>',
    '  <circle cx="5" cy="5" r="2.5" fill="#10B981"/>',
    '  <text x="16" y="8" fill="#10B981" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="8.5" font-weight="800" letter-spacing="1">LIVE WEBLATE STREAM</text>',
    '  <text x="' + (mobile ? 142 : 148) + '" y="8" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="8.5" font-weight="700" letter-spacing="1"> ·  ' + languageCount + ' ACTIVE FREQUENCIES  ·  ' + stringCount + ' MASTER STRINGS</text>',
    '</g>',
    '<line x1="' + (mobile ? 32 : 44) + '" y1="102" x2="' + (width - (mobile ? 32 : 44)) + '" y2="102" stroke="' + dividerLine + '" stroke-width="1"/>',
    chart,
    '<line x1="' + (mobile ? 32 : 44) + '" y1="' + (footerY - 18) + '" x2="' + (width - (mobile ? 32 : 44)) + '" y2="' + (footerY - 18) + '" stroke="' + dividerLine + '" stroke-width="1"/>',
    '<text x="' + (mobile ? 32 : 44) + '" y="' + footerY + '" fill="' + sub + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="9.5">♪ Every translation brings Levyra closer to someone, somewhere.</text>',
    '<g>',
    '  <rect x="' + (width - (mobile ? 32 : 44) - 138) + '" y="' + (footerY - 15) + '" width="138" height="22" rx="6" fill="' + btnBg + '" stroke="' + btnBorder + '" stroke-width="1"/>',
    '  <text x="' + (width - (mobile ? 32 : 44) - 69) + '" y="' + (footerY - 1) + '" text-anchor="middle" fill="' + btnText + '" font-family="-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Helvetica,Arial,sans-serif" font-size="8.5" font-weight="800" letter-spacing=".6">HELP TRANSLATE →</text>',
    '</g>',
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
