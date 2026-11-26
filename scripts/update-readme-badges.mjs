import { mkdir, readFile, writeFile } from 'node:fs/promises'

const repository = process.env.GITHUB_REPOSITORY
const token = process.env.GITHUB_TOKEN
if (!repository) throw new Error('GITHUB_REPOSITORY is required')

const headers = {
  Accept: 'application/vnd.github+json',
  'User-Agent': 'Levyra-README-Badge-Updater',
  'X-GitHub-Api-Version': '2022-11-28'
}
if (token) headers.Authorization = `Bearer ${token}`

const requestJson = async url => {
  const response = await fetch(url, { headers })
  if (!response.ok) throw new Error(`GitHub API request failed with ${response.status}: ${await response.text()}`)
  return response.json()
}

const apptekaPackage = 'com.luc4n3x.levyra'
const apptekaInfoUrl = 'https://appteka.store/api/1/app/info'

const fetchApptekaDownloads = async () => {
  const url = new URL(apptekaInfoUrl)
  url.searchParams.set('package', apptekaPackage)
  url.searchParams.set('locale', 'en')

  const response = await fetch(url, {
    headers: {
      Accept: 'application/json',
      'User-Agent': 'Levyra-README-Badge-Updater'
    },
    signal: AbortSignal.timeout(15000)
  })
  if (!response.ok) throw new Error(`Appteka API request failed with ${response.status}: ${await response.text()}`)

  const payload = await response.json()
  const details = payload?.result
  const info = details?.info
  if (!info || info.package !== apptekaPackage) throw new Error('Appteka API returned an invalid app payload')

  const entries = [info, ...(Array.isArray(details.versions) ? details.versions : [])]
  const downloadsByAppId = new Map()
  for (const entry of entries) {
    const appId = String(entry?.app_id ?? '')
    if (!appId) continue
    downloadsByAppId.set(appId, Number(entry.downloads ?? 0))
  }

  return [...downloadsByAppId.values()].reduce((total, downloads) => total + downloads, 0)
}

const readPreviousDownloads = async () => {
  try {
    const svg = await readFile('docs/assets/levyra-downloads.svg', 'utf8')
    const prefix = '<title>DOWNLOADS '
    const startIndex = svg.indexOf(prefix)
    if (startIndex < 0) return null
    const valueStart = startIndex + prefix.length
    const valueEnd = svg.indexOf('</title>', valueStart)
    if (valueEnd < 0) return null
    const value = svg.slice(valueStart, valueEnd).replaceAll(',', '')
    return /^\d+$/.test(value) ? Number(value) : null
  } catch {
    return null
  }
}

const listReleases = async () => {
  const releases = []
  for (let page = 1; page <= 100; page += 1) {
    const batch = await requestJson(`https://api.github.com/repos/${repository}/releases?per_page=100&page=${page}`)
    releases.push(...batch)
    if (batch.length < 100) return releases
  }
  throw new Error('Release pagination exceeded 100 pages')
}

const escapeXml = value => String(value)
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&apos;')

const makeArcScallopPath = (cx, cy, rValley, rPeak, lobes = 10) => {
  const step = (2 * Math.PI) / lobes
  const halfStep = step / 2
  const dMid = rValley * Math.cos(halfStep)
  const C = 2 * rValley * Math.sin(halfStep)
  const S = rPeak - dMid
  const R = S / 2.0 + (C ** 2) / (8.0 * S)
  const d = []
  const x0 = cx + rValley * Math.sin(-halfStep)
  const y0 = cy - rValley * Math.cos(-halfStep)
  d.push(`M ${x0.toFixed(2)} ${y0.toFixed(2)}`)
  for (let i = 0; i < lobes; i++) {
    const aEnd = (i + 0.5) * step
    const xEnd = cx + rValley * Math.sin(aEnd)
    const yEnd = cy - rValley * Math.cos(aEnd)
    d.push(`A ${R.toFixed(2)} ${R.toFixed(2)} 0 0 1 ${xEnd.toFixed(2)} ${yEnd.toFixed(2)}`)
  }
  d.push('Z')
  return d.join(' ')
}

const makeExpressivePillPath = (w, h, rLarge = 15.5, rSmall = 6.0) => {
  const x0 = 0.75
  const y0 = 0.75
  const rL = Math.min(rLarge, h / 2)
  const rS = Math.min(rSmall, h / 4)
  return `M ${(x0 + rL).toFixed(2)} ${y0.toFixed(2)} ` +
    `H ${(x0 + w - rS).toFixed(2)} ` +
    `A ${rS.toFixed(2)} ${rS.toFixed(2)} 0 0 1 ${(x0 + w).toFixed(2)} ${(y0 + rS).toFixed(2)} ` +
    `V ${(y0 + h - rL).toFixed(2)} ` +
    `A ${rL.toFixed(2)} ${rL.toFixed(2)} 0 0 1 ${(x0 + w - rL).toFixed(2)} ${(y0 + h).toFixed(2)} ` +
    `H ${(x0 + rS).toFixed(2)} ` +
    `A ${rS.toFixed(2)} ${rS.toFixed(2)} 0 0 1 ${x0.toFixed(2)} ${(y0 + h - rS).toFixed(2)} ` +
    `V ${(y0 + rL).toFixed(2)} ` +
    `A ${rL.toFixed(2)} ${rL.toFixed(2)} 0 0 1 ${(x0 + rL).toFixed(2)} ${y0.toFixed(2)} Z`
}

const PILL_SCALLOP = makeArcScallopPath(16, 16, 9.2, 12.2, 10)

const icons = {
  latest: '<path fill="currentColor" d="M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58s1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41s-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z"/>',
  downloads: '<path d="M0 -6 v11 m0 0 l4 -4 m-4 4 l-4 -4 M-6 8 h12" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"/>',
  license: '<path fill="currentColor" d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z"/>',
  stars: '<path d="M0 -8 l2.5 5 5.5 .8 -4 3.8 1 5.4 -5 -2.6 -5 2.6 1 -5.4 -4 -3.8 5.5 -.8 Z" fill="currentColor"/>',
  docs: '<path fill="currentColor" d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>'
}

const makePill = ({ label, value, icon, accentColor, mobile = false }) => {
  const title = `${label} ${value}`
  const width = mobile ? 126 : 130
  const height = mobile ? 31.0154 : 32
  const pillPath = makeExpressivePillPath(128.5, 30.5, 15.5, 6.0)

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 130 32" role="img" aria-label="${escapeXml(title)}">
  <title>${escapeXml(title)}</title>
  <path d="${pillPath}" fill="#FFFFFF" stroke="#CBD5E1" stroke-width="1.5"/>
  <path d="${PILL_SCALLOP}" fill="${accentColor}"/>
  <g transform="translate(16 16)" style="color:#FFFFFF;">
    ${icon}
  </g>
  <text x="35" y="12.2" fill="${accentColor}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="7.5" font-weight="700" letter-spacing=".8">${escapeXml(label)}</text>
  <text x="35" y="24.5" fill="#0F172A" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="12" font-weight="750">${escapeXml(value)}</text>
</svg>`
}


const CHIP_ROSETTE = makeArcScallopPath(21, 18, 11.4, 14.2, 10)

function makeRoundedCornersPath(x, y, w, h, [tl, tr, br, bl]) {
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

function makeInfoBadge({ label, value, icon, mobile = false, light = false }) {
  const meta = {
    LATEST: { description: `Latest Levyra release ${value}`, display: value, w: 102, mw: 92, corners: [18, 7, 18, 7], xform: 'translate(11.4 8.4) scale(0.80)', dark: ['#F6D2E0', '#5A2238'], light: ['#FCE4EE', '#5A2238'] },
    LICENSE: { description: 'Levyra license GPL-3.0', display: 'GPL-3.0', w: 112, mw: 101, corners: [18, 18, 18, 18], xform: 'translate(11.5 8.5) scale(0.79)', dark: ['#CBEFE0', '#144230'], light: ['#DCF6EB', '#144230'] },
    DOCS: { description: 'Read Levyra documentation', display: 'Docs', w: 92, mw: 83, corners: [7, 18, 7, 18], xform: 'translate(10.8 7.8) scale(0.85)', dark: ['#FAE3B4', '#4B3409'], light: ['#FDF0CF', '#4B3409'] }
  }
  const entry = meta[label]
  if (!entry) throw new Error(`Unknown information badge ${label}`)
  const [bg, fg] = entry[light ? 'light' : 'dark']
  const vw = entry.w
  const w = mobile ? entry.mw : vw
  const h = mobile ? 32 : 36
  const body = makeRoundedCornersPath(0, 0, vw, 36, entry.corners)
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${vw} 36" role="img" aria-label="${escapeXml(entry.description)}">
  <title>${escapeXml(entry.description)}</title>
  <path d="${body}" fill="${bg}"/>
  <path d="${CHIP_ROSETTE}" fill="${fg}" fill-opacity="0.11"/>
  <g transform="${entry.xform}" style="color:${fg}">
    ${icon}
  </g>
  <text x="40" y="23" fill="${fg}" font-family="Roboto,system-ui,-apple-system,'Segoe UI',Arial,sans-serif" font-size="14" font-weight="600" letter-spacing="0.05">${escapeXml(entry.display)}</text>
</svg>
`
}

const repositoryData = await requestJson(`https://api.github.com/repos/${repository}`)
const releases = (await listReleases()).filter(release => !release.draft)
const stableReleases = releases.filter(release => !release.prerelease)
const latestAndroid = stableReleases
  .filter(release => !String(release.tag_name ?? '').startsWith('desktop-v') && /^v?\d+\.\d+\.\d+(?:[-+].*)?$/.test(String(release.tag_name ?? '')))
  .sort((a, b) => new Date(b.published_at ?? b.created_at) - new Date(a.published_at ?? a.created_at))[0]
const githubDownloads = releases.reduce((total, release) => total + (release.assets ?? []).reduce((sum, asset) => sum + Number(asset.download_count ?? 0), 0), 0)

let totalDownloads
try {
  totalDownloads = githubDownloads + await fetchApptekaDownloads()
} catch (error) {
  console.warn(`Appteka downloads unavailable: ${error instanceof Error ? error.message : String(error)}`)
  totalDownloads = await readPreviousDownloads() ?? githubDownloads
}

const releaseValue = latestAndroid?.tag_name ?? 'none'
const downloadsValue = new Intl.NumberFormat('en-US').format(totalDownloads)
const starsValue = new Intl.NumberFormat('en-US').format(repositoryData.stargazers_count ?? 0)

await mkdir('docs/assets', { recursive: true })
await Promise.all([
  writeFile('docs/assets/levyra-release.svg', makeInfoBadge({ label: 'LATEST', value: releaseValue, icon: icons.latest, mobile: false, light: false }), 'utf8'),
  writeFile('docs/assets/levyra-downloads.svg', makePill({ label: 'DOWNLOADS', value: downloadsValue, icon: icons.downloads, accentColor: '#059669' }), 'utf8'),
  writeFile('docs/assets/levyra-license.svg', makeInfoBadge({ label: 'LICENSE', value: 'GPL-3.0', icon: icons.license, mobile: false, light: false }), 'utf8'),
  writeFile('docs/assets/levyra-stars.svg', makePill({ label: 'STARS', value: starsValue, icon: icons.stars, accentColor: '#D97706' }), 'utf8'),
  writeFile('docs/assets/levyra-wiki.svg', makeInfoBadge({ label: 'DOCS', value: 'Open docs', icon: icons.docs, mobile: false, light: false }), 'utf8'),
  writeFile('docs/assets/levyra-release-light.svg', makeInfoBadge({ label: 'LATEST', value: releaseValue, icon: icons.latest, mobile: false, light: true }), 'utf8'),
  writeFile('docs/assets/levyra-downloads-light.svg', makePill({ label: 'DOWNLOADS', value: downloadsValue, icon: icons.downloads, accentColor: '#059669' }), 'utf8'),
  writeFile('docs/assets/levyra-license-light.svg', makeInfoBadge({ label: 'LICENSE', value: 'GPL-3.0', icon: icons.license, mobile: false, light: true }), 'utf8'),
  writeFile('docs/assets/levyra-stars-light.svg', makePill({ label: 'STARS', value: starsValue, icon: icons.stars, accentColor: '#D97706' }), 'utf8'),
  writeFile('docs/assets/levyra-wiki-light.svg', makeInfoBadge({ label: 'DOCS', value: 'Open docs', icon: icons.docs, mobile: false, light: true }), 'utf8'),
  writeFile('docs/assets/levyra-release-mobile.svg', makeInfoBadge({ label: 'LATEST', value: releaseValue, icon: icons.latest, mobile: true, light: false }), 'utf8'),
  writeFile('docs/assets/levyra-downloads-mobile.svg', makePill({ label: 'DOWNLOADS', value: downloadsValue, icon: icons.downloads, accentColor: '#059669', mobile: true }), 'utf8'),
  writeFile('docs/assets/levyra-license-mobile.svg', makeInfoBadge({ label: 'LICENSE', value: 'GPL-3.0', icon: icons.license, mobile: true, light: false }), 'utf8'),
  writeFile('docs/assets/levyra-stars-mobile.svg', makePill({ label: 'STARS', value: starsValue, icon: icons.stars, accentColor: '#D97706', mobile: true }), 'utf8'),
  writeFile('docs/assets/levyra-wiki-mobile.svg', makeInfoBadge({ label: 'DOCS', value: 'Open docs', icon: icons.docs, mobile: true, light: false }), 'utf8'),
  writeFile('docs/assets/levyra-release-mobile-light.svg', makeInfoBadge({ label: 'LATEST', value: releaseValue, icon: icons.latest, mobile: true, light: true }), 'utf8'),
  writeFile('docs/assets/levyra-downloads-mobile-light.svg', makePill({ label: 'DOWNLOADS', value: downloadsValue, icon: icons.downloads, accentColor: '#059669', mobile: true }), 'utf8'),
  writeFile('docs/assets/levyra-license-mobile-light.svg', makeInfoBadge({ label: 'LICENSE', value: 'GPL-3.0', icon: icons.license, mobile: true, light: true }), 'utf8'),
  writeFile('docs/assets/levyra-stars-mobile-light.svg', makePill({ label: 'STARS', value: starsValue, icon: icons.stars, accentColor: '#D97706', mobile: true }), 'utf8'),
  writeFile('docs/assets/levyra-wiki-mobile-light.svg', makeInfoBadge({ label: 'DOCS', value: 'Open docs', icon: icons.docs, mobile: true, light: true }), 'utf8')
])
