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

const icons = {
  latest: '<path d="M-4 -8 h6 l5 5 -9 9 -6 -6 v-4 Z M1 -5 a1.5 1.5 0 1 0 0 3 A1.5 1.5 0 0 0 1 -5 Z" fill="currentColor"/>',
  downloads: '<path d="M0 -6 v11 m0 0 l4 -4 m-4 4 l-4 -4 M-6 8 h12" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"/>',
  license: '<path d="M0 -8 l7 3 v4 c0 5 -3 7 -7 9 -4 -2 -7 -4 -7 -9 v-4 Z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><path d="M-3 0 l2 2 4 -5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>',
  stars: '<path d="M0 -8 l2.5 5 5.5 .8 -4 3.8 1 5.4 -5 -2.6 -5 2.6 1 -5.4 -4 -3.8 5.5 -.8 Z" fill="currentColor"/>'
}

const makePill = ({ label, value, icon, isDark, accentColor, mobile = false }) => {
  const bgTop = isDark ? '#111820' : '#FFFFFF'
  const bgBottom = isDark ? '#0B1118' : '#F7F9FC'
  const border = isDark ? '#3A4655' : '#C9D2DE'
  const textSub = isDark ? '#98A6B7' : '#667085'
  const textMain = isDark ? '#F8FAFC' : '#172033'
  const innerOpacity = isDark ? '.045' : '.7'
  const title = `${label} ${value}`
  const width = mobile ? 126 : 130
  const height = mobile ? 31.0154 : 32

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 130 32" role="img" aria-label="${escapeXml(title)}">
  <title>${escapeXml(title)}</title>
  <defs>
    <linearGradient id="surface" x1="0" y1="0" x2="0" y2="1">
      <stop offset="0" stop-color="${bgTop}"/>
      <stop offset="1" stop-color="${bgBottom}"/>
    </linearGradient>
  </defs>
  <rect x=".7" y=".7" width="128.6" height="30.6" rx="9.2" fill="url(#surface)" stroke="${border}" stroke-width="1.4"/>
  <rect x="4" y="4" width="24" height="24" rx="7.2" fill="${accentColor}" fill-opacity="${isDark ? '.12' : '.085'}" stroke="${accentColor}" stroke-opacity="${isDark ? '.42' : '.30'}" stroke-width=".9"/>
  <rect x="5" y="5" width="22" height="22" rx="6.3" fill="none" stroke="#FFFFFF" stroke-opacity="${innerOpacity}" stroke-width=".55"/>
  <g transform="translate(16 16)" style="color:${accentColor};">
    ${icon}
  </g>
  <text x="35" y="11.9" fill="${textSub}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="6.7" font-weight="700" letter-spacing=".9">${escapeXml(label)}</text>
  <text x="35" y="24.2" fill="${textMain}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="11.7" font-weight="700">${escapeXml(value)}</text>
</svg>`
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
  writeFile('docs/assets/levyra-release.svg', makePill({ label: 'LATEST', value: releaseValue, icon: icons.latest, isDark: true, accentColor: '#B865FF' }), 'utf8'),
  writeFile('docs/assets/levyra-downloads.svg', makePill({ label: 'DOWNLOADS', value: downloadsValue, icon: icons.downloads, isDark: true, accentColor: '#2DD4A3' }), 'utf8'),
  writeFile('docs/assets/levyra-license.svg', makePill({ label: 'LICENSE', value: 'GPL-3.0', icon: icons.license, isDark: true, accentColor: '#4CC9F0' }), 'utf8'),
  writeFile('docs/assets/levyra-stars.svg', makePill({ label: 'STARS', value: starsValue, icon: icons.stars, isDark: true, accentColor: '#F7C948' }), 'utf8'),
  writeFile('docs/assets/levyra-release-light.svg', makePill({ label: 'LATEST', value: releaseValue, icon: icons.latest, isDark: false, accentColor: '#7C3AED' }), 'utf8'),
  writeFile('docs/assets/levyra-downloads-light.svg', makePill({ label: 'DOWNLOADS', value: downloadsValue, icon: icons.downloads, isDark: false, accentColor: '#059669' }), 'utf8'),
  writeFile('docs/assets/levyra-license-light.svg', makePill({ label: 'LICENSE', value: 'GPL-3.0', icon: icons.license, isDark: false, accentColor: '#0284C7' }), 'utf8'),
  writeFile('docs/assets/levyra-stars-light.svg', makePill({ label: 'STARS', value: starsValue, icon: icons.stars, isDark: false, accentColor: '#D97706' }), 'utf8'),
  writeFile('docs/assets/levyra-release-mobile.svg', makePill({ label: 'LATEST', value: releaseValue, icon: icons.latest, isDark: true, accentColor: '#B865FF', mobile: true }), 'utf8'),
  writeFile('docs/assets/levyra-downloads-mobile.svg', makePill({ label: 'DOWNLOADS', value: downloadsValue, icon: icons.downloads, isDark: true, accentColor: '#2DD4A3', mobile: true }), 'utf8'),
  writeFile('docs/assets/levyra-license-mobile.svg', makePill({ label: 'LICENSE', value: 'GPL-3.0', icon: icons.license, isDark: true, accentColor: '#4CC9F0', mobile: true }), 'utf8'),
  writeFile('docs/assets/levyra-stars-mobile.svg', makePill({ label: 'STARS', value: starsValue, icon: icons.stars, isDark: true, accentColor: '#F7C948', mobile: true }), 'utf8'),
  writeFile('docs/assets/levyra-release-mobile-light.svg', makePill({ label: 'LATEST', value: releaseValue, icon: icons.latest, isDark: false, accentColor: '#7C3AED', mobile: true }), 'utf8'),
  writeFile('docs/assets/levyra-downloads-mobile-light.svg', makePill({ label: 'DOWNLOADS', value: downloadsValue, icon: icons.downloads, isDark: false, accentColor: '#059669', mobile: true }), 'utf8'),
  writeFile('docs/assets/levyra-license-mobile-light.svg', makePill({ label: 'LICENSE', value: 'GPL-3.0', icon: icons.license, isDark: false, accentColor: '#0284C7', mobile: true }), 'utf8'),
  writeFile('docs/assets/levyra-stars-mobile-light.svg', makePill({ label: 'STARS', value: starsValue, icon: icons.stars, isDark: false, accentColor: '#D97706', mobile: true }), 'utf8')
])
