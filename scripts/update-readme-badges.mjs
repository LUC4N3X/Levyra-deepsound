import { mkdir, writeFile } from 'node:fs/promises'

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

const makePill = ({ label, value, icon, isDark, accentColor }) => {
  const bg = isDark ? '#0D1117' : '#FFFFFF'
  const border = isDark ? '#30363D' : '#D0D7DE'
  const tileBg = isDark ? '#161B22' : '#F6F8FA'
  const tileBorder = isDark ? '#30363D' : '#D0D7DE'
  const textSub = '#8B949E'
  const textMain = isDark ? '#F0F6FC' : '#1F2328'
  const title = `${label} ${value}`

  return `<svg xmlns="http://www.w3.org/2000/svg" width="130" height="32" viewBox="0 0 130 32" role="img" aria-label="${escapeXml(title)}">
  <title>${escapeXml(title)}</title>
  <rect x=".75" y=".75" width="128.5" height="30.5" rx="8" fill="${bg}" stroke="${border}" stroke-width="1.5"/>
  <rect x="4" y="4" width="24" height="24" rx="6" fill="${tileBg}" stroke="${tileBorder}" stroke-width="1"/>
  <g transform="translate(16 16)" style="color:${accentColor};">
    ${icon}
  </g>
  <text x="35" y="12.5" fill="${textSub}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="7.2" font-weight="700" letter-spacing=".75">${escapeXml(label)}</text>
  <text x="35" y="24.5" fill="${textMain}" font-family="-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif" font-size="12" font-weight="700">${escapeXml(value)}</text>
</svg>`
}

const repositoryData = await requestJson(`https://api.github.com/repos/${repository}`)
const releases = (await listReleases()).filter(release => !release.draft)
const stableReleases = releases.filter(release => !release.prerelease)
const latestAndroid = stableReleases
  .filter(release => !String(release.tag_name ?? '').startsWith('desktop-v') && /^v?\d+\.\d+\.\d+(?:[-+].*)?$/.test(String(release.tag_name ?? '')))
  .sort((a, b) => new Date(b.published_at ?? b.created_at) - new Date(a.published_at ?? a.created_at))[0]
const totalDownloads = releases.reduce((total, release) => total + (release.assets ?? []).reduce((sum, asset) => sum + Number(asset.download_count ?? 0), 0), 0)

const releaseValue = latestAndroid?.tag_name ?? 'none'
const downloadsValue = new Intl.NumberFormat('en-US').format(totalDownloads)
const starsValue = new Intl.NumberFormat('en-US').format(repositoryData.stargazers_count ?? 0)

await mkdir('docs/assets', { recursive: true })
await Promise.all([
  // Dark
  writeFile('docs/assets/levyra-release.svg', makePill({ label: 'LATEST', value: releaseValue, icon: icons.latest, isDark: true, accentColor: '#A855F7' }), 'utf8'),
  writeFile('docs/assets/levyra-downloads.svg', makePill({ label: 'DOWNLOADS', value: downloadsValue, icon: icons.downloads, isDark: true, accentColor: '#10B981' }), 'utf8'),
  writeFile('docs/assets/levyra-license.svg', makePill({ label: 'LICENSE', value: 'GPL-3.0', icon: icons.license, isDark: true, accentColor: '#38BDF8' }), 'utf8'),
  writeFile('docs/assets/levyra-stars.svg', makePill({ label: 'STARS', value: starsValue, icon: icons.stars, isDark: true, accentColor: '#FBBF24' }), 'utf8'),
  // Light
  writeFile('docs/assets/levyra-release-light.svg', makePill({ label: 'LATEST', value: releaseValue, icon: icons.latest, isDark: false, accentColor: '#7C3AED' }), 'utf8'),
  writeFile('docs/assets/levyra-downloads-light.svg', makePill({ label: 'DOWNLOADS', value: downloadsValue, icon: icons.downloads, isDark: false, accentColor: '#059669' }), 'utf8'),
  writeFile('docs/assets/levyra-license-light.svg', makePill({ label: 'LICENSE', value: 'GPL-3.0', icon: icons.license, isDark: false, accentColor: '#0284C7' }), 'utf8'),
  writeFile('docs/assets/levyra-stars-light.svg', makePill({ label: 'STARS', value: starsValue, icon: icons.stars, isDark: false, accentColor: '#D97706' }), 'utf8')
])
