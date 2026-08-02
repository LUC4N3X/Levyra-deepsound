import { mkdir, readFile, writeFile } from 'node:fs/promises'

const repo = process.env.GITHUB_REPOSITORY
if (!repo) throw new Error('GITHUB_REPOSITORY is required')
const headers = { Accept: 'application/vnd.github+json', 'User-Agent': 'Levyra-README-Badge-Updater', 'X-GitHub-Api-Version': '2022-11-28' }
if (process.env.GITHUB_TOKEN) headers.Authorization = `Bearer ${process.env.GITHUB_TOKEN}`

const get = async url => {
  const response = await fetch(url, { headers })
  if (!response.ok) throw new Error(`GitHub API request failed: ${response.status} ${await response.text()}`)
  return response.json()
}

const listReleases = async () => {
  const result = []
  for (let page = 1; page <= 100; page += 1) {
    const batch = await get(`https://api.github.com/repos/${repo}/releases?per_page=100&page=${page}`)
    result.push(...batch)
    if (batch.length < 100) return result
  }
  throw new Error('Release pagination exceeded 100 pages')
}

const xml = value => String(value).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&apos;')
const width = value => [...String(value)].reduce((sum, char) => sum + ('WM@%#'.includes(char) ? 10 : 'Il1|'.includes(char) ? 4.2 : ' .,:;'.includes(char) ? 4 : 7.1), 0)

const icons = {
  android: 'M3.7 5.2 2.5 3.1l.9-.5 1.3 2.2A7.2 7.2 0 0 1 8 4c1.2 0 2.3.3 3.3.8l1.3-2.2.9.5-1.2 2.1A6.8 6.8 0 0 1 15 10H1a6.8 6.8 0 0 1 2.7-4.8ZM5 8a.8.8 0 1 0 0-1.6A.8.8 0 0 0 5 8Zm6 0a.8.8 0 1 0 0-1.6A.8.8 0 0 0 11 8ZM2 11h12v2a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-2Z',
  release: 'M7.73 2.5a1.5 1.5 0 0 0-1.06.44L2.44 7.17a1.5 1.5 0 0 0 0 2.12l4.27 4.27a1.5 1.5 0 0 0 2.12 0l4.23-4.23a1.5 1.5 0 0 0 .44-1.06V4A1.5 1.5 0 0 0 12 2.5H7.73Zm0 1.5H12v4.27L7.77 12.5 3.5 8.23 7.73 4ZM9 7a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z',
  downloads: 'M8 2.25a.75.75 0 0 1 .75.75v5.69l1.97-1.97a.75.75 0 1 1 1.06 1.06l-3.25 3.25a.75.75 0 0 1-1.06 0L4.22 7.78a.75.75 0 0 1 1.06-1.06l1.97 1.97V3A.75.75 0 0 1 8 2.25Zm-4.75 10a.75.75 0 0 1 .75-.75h8a.75.75 0 0 1 0 1.5H4a.75.75 0 0 1-.75-.75Z',
  license: 'M8 1.25 2.5 3.2v4.16c0 3.42 2.29 6.55 5.5 7.39 3.21-.84 5.5-3.97 5.5-7.39V3.2L8 1.25Zm0 1.59 4 1.42v3.1c0 2.6-1.64 5.05-4 5.82-2.36-.77-4-3.22-4-5.82v-3.1l4-1.42Z',
  stars: 'M8 .75a.75.75 0 0 1 .673.418L10.52 4.9l4.12.599a.75.75 0 0 1 .416 1.279l-2.98 2.905.704 4.103a.75.75 0 0 1-1.088.79L8 12.61l-3.694 1.943a.75.75 0 0 1-1.088-.79l.704-4.103-2.98-2.905a.75.75 0 0 1 .416-1.279l4.12-.599L7.327 1.168A.75.75 0 0 1 8 .75Z'
}

const badge = ({ key, label, value, icon, start, end, text = '#07111A', min = 52 }) => {
  const labelWidth = Math.max(92, Math.ceil(width(label) + 46))
  const valueWidth = Math.max(min, Math.ceil(width(value) + 28))
  const total = labelWidth + valueWidth + 6
  const valueX = total - valueWidth - 7
  const title = `${label}: ${value}`
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${total}" height="34" viewBox="0 0 ${total} 34" role="img" aria-label="${xml(title)}">
<title>${xml(title)}</title>
<defs><linearGradient id="${key}-bg" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#0F172A"/><stop offset="1" stop-color="#0A0F1D"/></linearGradient><linearGradient id="${key}-a" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="${start}"/><stop offset="1" stop-color="${end}"/></linearGradient></defs>
<rect x=".5" y=".5" width="${total - 1}" height="33" rx="10" fill="url(#${key}-bg)" stroke="#263248"/>
<rect x="1.5" y="8" width="3" height="18" rx="1.5" fill="url(#${key}-a)"/>
<circle cx="20" cy="17" r="9.75" fill="none" stroke="${start}" stroke-opacity=".22"/>
<g transform="translate(12 9)" fill="url(#${key}-a)"><path d="${icon}"/></g>
<text x="37" y="20.5" fill="#F8FAFC" font-family="Segoe UI,Inter,Arial,sans-serif" font-size="10.5" font-weight="800">${xml(label)}</text>
<rect x="${valueX}" y="5" width="${valueWidth}" height="24" rx="8" fill="url(#${key}-a)"/>
<text x="${valueX + valueWidth / 2}" y="20.5" text-anchor="middle" fill="${text}" font-family="Segoe UI,Inter,Arial,sans-serif" font-size="10.4" font-weight="900">${xml(value)}</text>
</svg>
`
}

const repository = await get(`https://api.github.com/repos/${repo}`)
const releases = (await listReleases()).filter(release => !release.draft)
const stable = releases.filter(release => !release.prerelease)
const android = [...stable].filter(release => !String(release.tag_name ?? '').startsWith('desktop-v') && /^v?\d+\.\d+\.\d+(?:[-+].*)?$/.test(String(release.tag_name ?? ''))).sort((a, b) => new Date(b.published_at ?? b.created_at) - new Date(a.published_at ?? a.created_at))[0]
const downloads = releases.reduce((total, release) => total + (release.assets ?? []).reduce((sum, asset) => sum + Number(asset.download_count ?? 0), 0), 0)
const format = value => new Intl.NumberFormat('en-US').format(Number(value))
const version = android?.tag_name ?? 'none'

await mkdir('docs/assets', { recursive: true })
await Promise.all([
  writeFile('docs/assets/levyra-android-platform.svg', badge({ key: 'android', label: 'Android', value: version, icon: icons.android, start: '#4ADE80', end: '#22D3EE', min: 60 }), 'utf8'),
  writeFile('docs/assets/levyra-release.svg', badge({ key: 'release', label: 'Release', value: version, icon: icons.release, start: '#A78BFA', end: '#7C3AED', text: '#F8FAFC', min: 60 }), 'utf8'),
  writeFile('docs/assets/levyra-downloads.svg', badge({ key: 'downloads', label: 'Downloads', value: format(downloads), icon: icons.downloads, start: '#60A5FA', end: '#22D3EE', min: 36 }), 'utf8'),
  writeFile('docs/assets/levyra-license.svg', badge({ key: 'license', label: 'License', value: 'GPL-3.0', icon: icons.license, start: '#34D399', end: '#22C55E', text: '#07130B', min: 64 }), 'utf8'),
  writeFile('docs/assets/levyra-stars.svg', badge({ key: 'stars', label: 'Stars', value: format(repository.stargazers_count ?? 0), icon: icons.stars, start: '#FBBF24', end: '#F59E0B', text: '#15110A', min: 34 }), 'utf8')
])

const readme = await readFile('README.md', 'utf8')
const block = `<a href="https://github.com/LUC4N3X/Levyra-deepsound/releases/latest"><img src="docs/assets/levyra-android-platform.svg" alt="Download Levyra for Android"></a>
<a href="https://github.com/LUC4N3X/Levyra-deepsound/releases?q=Levyra+Desktop&expanded=true"><img src="docs/assets/levyra-windows-platform.svg" alt="Download Levyra for Windows"></a>
<a href="https://github.com/LUC4N3X/Levyra-deepsound/releases"><img src="docs/assets/levyra-downloads.svg" alt="Total Levyra downloads"></a>
<a href="LICENSE"><img src="docs/assets/levyra-license.svg" alt="GPL-3.0 License"></a>
<a href="https://github.com/LUC4N3X/Levyra-deepsound/stargazers"><img src="docs/assets/levyra-stars.svg" alt="Star Levyra"></a>`
const pattern = /<a href="https:\/\/github\.com\/LUC4N3X\/Levyra-deepsound\/releases\/latest"><img[^>]+><\/a>\n(?:<a href="https:\/\/github\.com\/LUC4N3X\/Levyra-deepsound\/releases\?q=[^\"]*&expanded=true"><img[^>]+><\/a>\n)?<a href="https:\/\/github\.com\/LUC4N3X\/Levyra-deepsound\/releases"><img[^>]+><\/a>\n<a href="LICENSE"><img[^>]+><\/a>\n<a href="https:\/\/github\.com\/LUC4N3X\/Levyra-deepsound(?:\/stargazers)?"><img[^>]+><\/a>/
const updated = readme.replace(pattern, block)
if (updated === readme && !readme.includes(block)) throw new Error('README badge block was not found')
if (updated !== readme) await writeFile('README.md', updated, 'utf8')
