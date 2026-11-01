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
  latest: '<path d="M-4 -8 h6 l5 5 -9 9 -6 -6 v-4 Z M1 -5 a1.5 1.5 0 1 0 0 3 A1.5 1.5 0 0 0 1 -5 Z" fill="currentColor"/>',
  downloads: '<path d="M0 -6 v11 m0 0 l4 -4 m-4 4 l-4 -4 M-6 8 h12" fill="none" stroke="currentColor" stroke-width="1.9" stroke-linecap="round" stroke-linejoin="round"/>',
  license: '<path d="M0 -8 l7 3 v4 c0 5 -3 7 -7 9 -4 -2 -7 -4 -7 -9 v-4 Z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><path d="M-3 0 l2 2 4 -5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>',
  stars: '<path d="M0 -8 l2.5 5 5.5 .8 -4 3.8 1 5.4 -5 -2.6 -5 2.6 1 -5.4 -4 -3.8 5.5 -.8 Z" fill="currentColor"/>',
  docs: '<path d="M-5 -7 h7 l4 4 v10 h-11 Z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><path d="M2 -7 v4 h4 M-2 1 h5 M-2 5 h5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>'
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


function makeInfoBadge({label,value,icon,mobile=false,light=false}){
 const styles={
  LATEST:{corners:[19,8,19,8],description:`Latest Levyra release ${value}`,dark:['#474151','#DECDF4','#DDCDEF','#FAF8FF'],light:['#EEE8F3','#72548D','#715387','#33283C']},
  LICENSE:{corners:[9,20,9,19],description:'Levyra license GPL-3.0',dark:['#364B49','#B8E7DA','#C1EBDD','#F4FFFA'],light:['#E4F0EC','#3D7469','#416D64','#294D43']},
  DOCS:{corners:[18,9,9,19],description:'Read Levyra documentation',dark:['#4A3D49','#F0CBDD','#F1CFDE','#FFF8FD'],light:['#F2E7EE','#96607B','#83526D','#503647']}
 };
 const style=styles[label];
 if(!style)throw new Error(`Unknown information badge ${label}`);
 const [bg,accent,kicker,text]=style[light?'light':'dark'];
 const [tl,tr,br,bl]=style.corners;
 const number=x=>Number(x.toFixed(1));
 const path=`M ${tl} 0.8 H ${number(159.2-tr)} Q 159.2 0.8 159.2 ${tr} V ${number(41.2-br)} Q 159.2 41.2 ${number(159.2-br)} 41.2 H ${number(bl+.8)} Q .8 41.2 .8 ${number(41.2-bl)} V ${tl} Q .8 .8 ${tl} .8 Z`;
 return `<svg xmlns="http://www.w3.org/2000/svg" width="${mobile?126:160}" height="${mobile?33.075:42}" viewBox="0 0 160 42" role="img" aria-label="${escapeXml(style.description)}">
<title>${escapeXml(style.description)}</title>
<path d="${path}" fill="${bg}"/>
<path d="M 1 21 C 1 10 10 1 24 1 H 33 C 45 10 46 31 34 41 H 17 C 7 41 1 32 1 21 Z" fill="${accent}" opacity="${light?'0.13':'0.12'}"/>
<g transform="translate(23 21) scale(1.33)" style="color:${accent}">
    ${icon}
  </g>
<text x="51" y="16.2" fill="${kicker}" font-family="system-ui,-apple-system,'Segoe UI',Roboto,Arial,sans-serif" font-size="9.4" font-weight="670" letter-spacing=".65">${escapeXml(label)}</text>
<text x="51" y="31.7" fill="${text}" font-family="system-ui,-apple-system,'Segoe UI',Roboto,Arial,sans-serif" font-size="15.3" font-weight="710" letter-spacing="-.35">${escapeXml(value)}</text>
</svg>
`;
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
