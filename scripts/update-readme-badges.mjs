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

const measure = value => {
  let width = 0
  for (const character of String(value)) {
    if ('WM@%#'.includes(character)) width += 10
    else if ('Il1|'.includes(character)) width += 4.2
    else if (' .,:;'.includes(character)) width += 4
    else width += 7.1
  }
  return width
}

const icons = {
  android: 'M3.7 5.2 2.5 3.1l.9-.5 1.3 2.2A7.2 7.2 0 0 1 8 4c1.2 0 2.3.3 3.3.8l1.3-2.2.9.5-1.2 2.1A6.8 6.8 0 0 1 15 10H1a6.8 6.8 0 0 1 2.7-4.8ZM5 8a.8.8 0 1 0 0-1.6A.8.8 0 0 0 5 8Zm6 0a.8.8 0 1 0 0-1.6A.8.8 0 0 0 11 8ZM2 11h12v2a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2v-2Z',
  release: 'M7.73 2.5a1.5 1.5 0 0 0-1.06.44L2.44 7.17a1.5 1.5 0 0 0 0 2.12l4.27 4.27a1.5 1.5 0 0 0 2.12 0l4.23-4.23a1.5 1.5 0 0 0 .44-1.06V4A1.5 1.5 0 0 0 12 2.5H7.73Zm0 1.5H12v4.27L7.77 12.5 3.5 8.23 7.73 4ZM9 7a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z',
  downloads: 'M8 2.25a.75.75 0 0 1 .75.75v5.69l1.97-1.97a.75.75 0 1 1 1.06 1.06l-3.25 3.25a.75.75 0 0 1-1.06 0L4.22 7.78a.75.75 0 0 1 1.06-1.06l1.97 1.97V3A.75.75 0 0 1 8 2.25Zm-4.75 10a.75.75 0 0 1 .75-.75h8a.75.75 0 0 1 0 1.5H4a.75.75 0 0 1-.75-.75Z',
  license: 'M8 1.25 2.5 3.2v4.16c0 3.42 2.29 6.55 5.5 7.39 3.21-.84 5.5-3.97 5.5-7.39V3.2L8 1.25Zm0 1.59 4 1.42v3.1c0 2.6-1.64 5.05-4 5.82-2.36-.77-4-3.22-4-5.82v-3.1l4-1.42Z',
  stars: 'M8 .75a.75.75 0 0 1 .673.418L10.52 4.9l4.12.599a.75.75 0 0 1 .416 1.279l-2.98 2.905.704 4.103a.75.75 0 0 1-1.088.79L8 12.61l-3.694 1.943a.75.75 0 0 1-1.088-.79l.704-4.103-2.98-2.905a.75.75 0 0 1 .416-1.279l4.12-.599L7.327 1.168A.75.75 0 0 1 8 .75Z'
}

const theme = {
  backgroundStart: '#0F172A',
  backgroundEnd: '#0A0F1D',
  border: '#263248'
}

const makeBadge = ({ label, value, icon, accentStart, accentEnd, valueTextColor = '#08101A', valueMinWidth = 52, key }) => {
  const labelWidth = Math.max(92, Math.ceil(measure(label) + 46))
  const valueWidth = Math.max(valueMinWidth, Math.ceil(measure(value) + 28))
  const totalWidth = labelWidth + valueWidth + 6
  const valueX = totalWidth - valueWidth - 7
  const valueCenter = valueX + valueWidth / 2
  const title = `${label}: ${value}`
  const safeKey = key ?? label.toLowerCase().replace(/[^a-z0-9]+/g, '-')

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${totalWidth}" height="34" viewBox="0 0 ${totalWidth} 34" role="img" aria-label="${escapeXml(title)}">\n<title>${escapeXml(title)}</title>\n<defs>\n  <linearGradient id="${safeKey}-bg" x1="0" y1="0" x2="1" y2="1">\n    <stop offset="0" stop-color="${theme.backgroundStart}"/>\n    <stop offset="1" stop-color="${theme.backgroundEnd}"/>\n  </linearGradient>\n  <linearGradient id="${safeKey}-accent" x1="0" y1="0" x2="1" y2="0">\n    <stop offset="0" stop-color="${accentStart}"/>\n    <stop offset="1" stop-color="${accentEnd}"/>\n  </linearGradient>\n</defs>\n<rect x=".5" y=".5" width="${totalWidth - 1}" height="33" rx="10" fill="url(#${safeKey}-bg)" stroke="${theme.border}"/>\n<rect x="1.5" y="8" width="3" height="18" rx="1.5" fill="url(#${safeKey}-accent)"/>\n<circle cx="20" cy="17" r="9.75" fill="none" stroke="${accentStart}" stroke-opacity=".22"/>\n<g transform="translate(12 9)" fill="url(#${safeKey}-accent)">\n  <path d="${icon}"/>\n</g>\n<text x="37" y="20.5" fill="#F8FAFC" font-family="Segoe UI,Inter,Arial,sans-serif" font-size="10.5" font-weight="800" letter-spacing=".12">${escapeXml(label)}</text>\n<rect x="${valueX}" y="5" width="${valueWidth}" height="24" rx="8" fill="url(#${safeKey}-accent)"/>\n<rect x="${valueX + 0.5}" y="5.5" width="${valueWidth - 1}" height="23" rx="7.5" fill="none" stroke="#FFFFFF" stroke-opacity=".16"/>\n<text x="${valueCenter}" y="20.5" text-anchor="middle" fill="${valueTextColor}" font-family="Segoe UI,Inter,Arial,sans-serif" font-size="10.4" font-weight="900">${escapeXml(value)}</text>\n</svg>\n`
}

const releaseTimestamp = release => new Date(release.published_at ?? release.created_at).getTime()
const repositoryData = await requestJson(`https://api.github.com/repos/${repository}`)
const releases = (await listReleases()).filter(release => !release.draft)
const stableReleases = releases.filter(release => !release.prerelease)
const androidRelease = [...stableReleases]
  .filter(release => {
    const tag = String(release.tag_name ?? '')
    return !tag.startsWith('desktop-v') && /^v?\d+\.\d+\.\d+(?:[-+].*)?$/.test(tag)
  })
  .sort((left, right) => releaseTimestamp(right) - releaseTimestamp(left))[0]
const totalDownloads = releases.reduce((releaseTotal, release) => {
  const assetTotal = Array.isArray(release.assets)
    ? release.assets.reduce((sum, asset) => sum + Number(asset.download_count ?? 0), 0)
    : 0
  return releaseTotal + assetTotal
}, 0)

const formatNumber = value => new Intl.NumberFormat('en-US').format(Number(value))
const releaseValue = androidRelease?.tag_name ?? 'none'
const downloadsValue = formatNumber(totalDownloads)
const starsValue = formatNumber(repositoryData.stargazers_count ?? 0)

await mkdir('docs/assets', { recursive: true })
await Promise.all([
  writeFile('docs/assets/levyra-android-platform.svg', makeBadge({ label: 'Android', value: releaseValue, icon: icons.android, accentStart: '#4ADE80', accentEnd: '#22D3EE', valueTextColor: '#07111A', valueMinWidth: 60, key: 'android' }), 'utf8'),
  writeFile('docs/assets/levyra-release.svg', makeBadge({ label: 'Release', value: releaseValue, icon: icons.release, accentStart: '#A78BFA', accentEnd: '#7C3AED', valueTextColor: '#F8FAFC', valueMinWidth: 60, key: 'release' }), 'utf8'),
  writeFile('docs/assets/levyra-downloads.svg', makeBadge({ label: 'Downloads', value: downloadsValue, icon: icons.downloads, accentStart: '#60A5FA', accentEnd: '#22D3EE', valueTextColor: '#07111A', valueMinWidth: 36, key: 'downloads' }), 'utf8'),
  writeFile('docs/assets/levyra-license.svg', makeBadge({ label: 'License', value: 'GPL-3.0', icon: icons.license, accentStart: '#34D399', accentEnd: '#22C55E', valueTextColor: '#07130B', valueMinWidth: 64, key: 'license' }), 'utf8'),
  writeFile('docs/assets/levyra-stars.svg', makeBadge({ label: 'Stars', value: starsValue, icon: icons.stars, accentStart: '#FBBF24', accentEnd: '#F59E0B', valueTextColor: '#15110A', valueMinWidth: 34, key: 'stars' }), 'utf8')
])

const badgeBlock = `<a href="https://github.com/LUC4N3X/Levyra-deepsound/releases/latest"><img src="docs/assets/levyra-android-platform.svg" alt="Download Levyra for Android"></a>\n<a href="https://github.com/LUC4N3X/Levyra-deepsound/releases?q=Levyra+Desktop&expanded=true"><img src="docs/assets/levyra-windows-platform.svg" alt="Download Levyra for Windows"></a>\n<a href="https://github.com/LUC4N3X/Levyra-deepsound/releases"><img src="docs/assets/levyra-downloads.svg" alt="Total Levyra downloads"></a>\n<a href="LICENSE"><img src="docs/assets/levyra-license.svg" alt="GPL-3.0 License"></a>\n<a href="https://github.com/LUC4N3X/Levyra-deepsound/stargazers"><img src="docs/assets/levyra-stars.svg" alt="Star Levyra"></a>`
const badgePattern = /<a href="https:\/\/github\.com\/LUC4N3X\/Levyra-deepsound\/releases\/latest"><img[^>]+><\/a>\n(?:<a href="https:\/\/github\.com\/LUC4N3X\/Levyra-deepsound\/releases\?q=[^\"]*&expanded=true"><img[^>]+><\/a>\n)?<a href="https:\/\/github\.com\/LUC4N3X\/Levyra-deepsound\/releases"><img[^>]+><\/a>\n<a href="LICENSE"><img[^>]+><\/a>\n<a href="https:\/\/github\.com\/LUC4N3X\/Levyra-deepsound(?:\/stargazers)?"><img[^>]+><\/a>/
const downloadBlock = `<sub><strong>CHOOSE YOUR PLATFORM</strong></sub>\n\n<p>\n<a href="https://github.com/LUC4N3X/Levyra-deepsound/releases/latest">\n  <img src="docs/assets/levyra-github-download.svg" alt="Download the latest signed Levyra APK from GitHub Releases" width="365" />\n</a>&nbsp;&nbsp;\n<a href="https://github.com/LUC4N3X/Levyra-deepsound/releases?q=Levyra+Desktop&expanded=true">\n  <img src="docs/assets/levyra-windows-download.svg" alt="Download Levyra Desktop for Windows from GitHub Releases" width="365" />\n</a>\n</p>`
const downloadPattern = /(?:<sub><strong>CHOOSE YOUR PLATFORM<\/strong><\/sub>\n\n<p>\n)?<a href="https:\/\/github\.com\/LUC4N3X\/Levyra-deepsound\/releases\/latest">\n\s*<img src="docs\/assets\/levyra-github-download\.svg"[^>]*>\n<\/a>(?:&nbsp;&nbsp;)?\n<a href="https:\/\/github\.com\/LUC4N3X\/Levyra-deepsound\/releases\?q=[^\"]*&expanded=true">\n\s*<img src="docs\/assets\/levyra-windows-download\.svg"[^>]*>\n<\/a>(?:\n<\/p>)?(?:\n\n<sub>\*\*Android and Windows\. No account\. No ads\.\*\* · Signed APK · MSI \/ EXE \/ Portable ZIP<\/sub>)?/

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
