import { mkdir, readFile, writeFile } from 'node:fs/promises'

const repository = process.env.GITHUB_REPOSITORY
const token = process.env.GITHUB_TOKEN

if (!repository) {
  throw new Error('GITHUB_REPOSITORY is required')
}

const headers = {
  Accept: 'application/vnd.github+json',
  'User-Agent': 'Levyra-README-Badge-Updater',
  'X-GitHub-Api-Version': '2022-11-28'
}

if (token) {
  headers.Authorization = `Bearer ${token}`
}

const requestJson = async url => {
  const response = await fetch(url, { headers })
  if (!response.ok) {
    const body = await response.text()
    throw new Error(`GitHub API request failed with ${response.status}: ${body}`)
  }
  return response.json()
}

const listReleases = async () => {
  const releases = []
  for (let page = 1; page <= 100; page += 1) {
    const batch = await requestJson(`https://api.github.com/repos/${repository}/releases?per_page=100&page=${page}`)
    releases.push(...batch)
    if (batch.length < 100) {
      return releases
    }
  }
  throw new Error('Release pagination exceeded 100 pages')
}

const escapeXml = value => String(value)
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&apos;')

const measure = value => {
  let width = 0
  for (const character of String(value)) {
    if ('WM@%#'.includes(character)) width += 10
    else if ('Il1|'.includes(character)) width += 4.2
    else if (' .,:;'.includes(character)) width += 4
    else width += 7.2
  }
  return width
}

const icons = {
  release: 'M7.73 2.5a1.5 1.5 0 0 0-1.06.44L2.44 7.17a1.5 1.5 0 0 0 0 2.12l4.27 4.27a1.5 1.5 0 0 0 2.12 0l4.23-4.23a1.5 1.5 0 0 0 .44-1.06V4A1.5 1.5 0 0 0 12 2.5H7.73Zm0 1.5H12v4.27L7.77 12.5 3.5 8.23 7.73 4ZM9 7a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z',
  downloads: 'M8 2.25a.75.75 0 0 1 .75.75v5.69l1.97-1.97a.75.75 0 1 1 1.06 1.06l-3.25 3.25a.75.75 0 0 1-1.06 0L4.22 7.78a.75.75 0 0 1 1.06-1.06l1.97 1.97V3A.75.75 0 0 1 8 2.25Zm-4.75 10a.75.75 0 0 1 .75-.75h8a.75.75 0 0 1 0 1.5H4a.75.75 0 0 1-.75-.75Z',
  license: 'M8 1.25 2.5 3.2v4.16c0 3.42 2.29 6.55 5.5 7.39 3.21-.84 5.5-3.97 5.5-7.39V3.2L8 1.25Zm0 1.59 4 1.42v3.1c0 2.6-1.64 5.05-4 5.82-2.36-.77-4-3.22-4-5.82v-3.1l4-1.42Z',
  stars: 'M8 .75a.75.75 0 0 1 .673.418L10.52 4.9l4.12.599a.75.75 0 0 1 .416 1.279l-2.98 2.905.704 4.103a.75.75 0 0 1-1.088.79L8 12.61l-3.694 1.943a.75.75 0 0 1-1.088-.79l.704-4.103-2.98-2.905a.75.75 0 0 1 .416-1.279l4.12-.599L7.327 1.168A.75.75 0 0 1 8 .75Z'
}

const makeBadge = ({ label, value, color, icon }) => {
  const labelWidth = Math.max(92, Math.ceil(measure(label) + 44))
  const valueWidth = Math.max(52, Math.ceil(measure(value) + 28))
  const totalWidth = labelWidth + valueWidth
  const valueCenter = labelWidth + valueWidth / 2
  const title = `${label}: ${value}`

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${totalWidth}" height="36" role="img" aria-label="${escapeXml(title)}"><title>${escapeXml(title)}</title><rect x="0.75" y="0.75" width="${totalWidth - 1.5}" height="34.5" rx="8" fill="#0d1117" stroke="#303b4d"/><path d="M${labelWidth} 1h${valueWidth - 8}a8 8 0 0 1 8 8v18a8 8 0 0 1-8 8h-${valueWidth - 8}Z" fill="${color}"/><path d="M${labelWidth} 6v24" stroke="#ffffff" stroke-opacity=".16"/><svg x="12" y="10" width="16" height="16" viewBox="0 0 16 16" aria-hidden="true"><path fill="#ffffff" d="${icon}"/></svg><text x="36" y="22.5" fill="#f8fafc" font-family="Segoe UI,Arial,sans-serif" font-size="11.5" font-weight="700" letter-spacing=".15">${escapeXml(label)}</text><text x="${valueCenter}" y="22.5" fill="#ffffff" text-anchor="middle" font-family="Segoe UI,Arial,sans-serif" font-size="12" font-weight="700">${escapeXml(value)}</text></svg>\n`
}

const repositoryData = await requestJson(`https://api.github.com/repos/${repository}`)
const releases = (await listReleases()).filter(release => !release.draft)
const latestRelease = [...releases].sort((left, right) => {
  const leftDate = new Date(left.published_at ?? left.created_at).getTime()
  const rightDate = new Date(right.published_at ?? right.created_at).getTime()
  return rightDate - leftDate
})[0]
const totalDownloads = releases.reduce((releaseTotal, release) => {
  const assetTotal = Array.isArray(release.assets)
    ? release.assets.reduce((sum, asset) => sum + Number(asset.download_count ?? 0), 0)
    : 0
  return releaseTotal + assetTotal
}, 0)

const formatNumber = value => new Intl.NumberFormat('en-US').format(Number(value))
const releaseValue = latestRelease?.tag_name ?? 'none'
const downloadsValue = formatNumber(totalDownloads)
const starsValue = formatNumber(repositoryData.stargazers_count ?? 0)

await mkdir('docs/assets', { recursive: true })
await Promise.all([
  writeFile('docs/assets/levyra-release.svg', makeBadge({ label: 'Release', value: releaseValue, color: '#7C3AED', icon: icons.release }), 'utf8'),
  writeFile('docs/assets/levyra-downloads.svg', makeBadge({ label: 'Downloads', value: downloadsValue, color: '#1689E8', icon: icons.downloads }), 'utf8'),
  writeFile('docs/assets/levyra-license.svg', makeBadge({ label: 'License', value: 'GPL-3.0', color: '#22C776', icon: icons.license }), 'utf8'),
  writeFile('docs/assets/levyra-stars.svg', makeBadge({ label: 'Stars', value: starsValue, color: '#F2A900', icon: icons.stars }), 'utf8')
])

const badgeBlock = `<a href="https://github.com/LUC4N3X/Levyra-deepsound/releases/latest"><img src="docs/assets/levyra-release.svg" alt="Latest Levyra release"></a>
<a href="https://github.com/LUC4N3X/Levyra-deepsound/releases"><img src="docs/assets/levyra-downloads.svg" alt="Total Levyra downloads"></a>
<a href="LICENSE"><img src="docs/assets/levyra-license.svg" alt="GPL-3.0 License"></a>
<a href="https://github.com/LUC4N3X/Levyra-deepsound/stargazers"><img src="docs/assets/levyra-stars.svg" alt="Star Levyra"></a>`
const badgePattern = /<a href="https:\/\/github\.com\/LUC4N3X\/Levyra-deepsound\/releases\/latest"><img[^>]+><\/a>\n<a href="https:\/\/github\.com\/LUC4N3X\/Levyra-deepsound\/releases"><img[^>]+><\/a>\n<a href="LICENSE"><img[^>]+><\/a>\n<a href="https:\/\/github\.com\/LUC4N3X\/Levyra-deepsound(?:\/stargazers)?"><img[^>]+><\/a>/
const downloadBlock = `<a href="https://github.com/LUC4N3X/Levyra-deepsound/releases/latest">
  <img src="docs/assets/levyra-github-download.svg" alt="Download the latest signed Levyra APK from GitHub Releases" width="520" />
</a>`
const downloadPattern = /<a href="https:\/\/github\.com\/LUC4N3X\/Levyra-deepsound\/releases\/latest">\n\s*<img src="docs\/assets\/levyra-github-download\.svg"[^>]*>\n<\/a>/
const readme = await readFile('README.md', 'utf8')
const badgesUpdated = readme.replace(badgePattern, badgeBlock)
const updatedReadme = badgesUpdated.replace(downloadPattern, downloadBlock)

if (badgesUpdated === readme && !readme.includes(badgeBlock)) {
  throw new Error('README badge block was not found')
}

if (updatedReadme === badgesUpdated && !badgesUpdated.includes(downloadBlock)) {
  throw new Error('README download block was not found')
}

if (updatedReadme !== readme) {
  await writeFile('README.md', updatedReadme, 'utf8')
}
