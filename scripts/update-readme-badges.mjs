import { mkdir, writeFile } from 'node:fs/promises'

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
  for (const character of String(value).toUpperCase()) {
    if ('MW@#%&'.includes(character)) width += 9
    else if ('I1L'.includes(character)) width += 4.5
    else if (' .,:;|!'.includes(character)) width += 3.5
    else width += 7
  }
  return width
}

const githubPath = 'M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82A7.65 7.65 0 0 1 8 5.8c.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8Z'

const makeBadge = ({ label, message, color }) => {
  const normalizedLabel = String(label).toUpperCase()
  const normalizedMessage = String(message).toUpperCase()
  const labelWidth = Math.max(92, Math.ceil(measure(normalizedLabel) + 48))
  const messageWidth = Math.max(46, Math.ceil(measure(normalizedMessage) + 24))
  const totalWidth = labelWidth + messageWidth
  const labelTextX = 28 + (labelWidth - 28) / 2
  const messageTextX = labelWidth + messageWidth / 2
  const title = `${normalizedLabel}: ${normalizedMessage}`

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${totalWidth}" height="28" role="img" aria-label="${escapeXml(title)}"><title>${escapeXml(title)}</title><defs><linearGradient id="badge-sheen" x2="0" y2="100%"><stop offset="0" stop-color="#fff" stop-opacity=".08"/><stop offset="1" stop-opacity=".08"/></linearGradient><clipPath id="badge-round"><rect width="${totalWidth}" height="28" rx="4"/></clipPath></defs><g clip-path="url(#badge-round)"><rect width="${labelWidth}" height="28" fill="#0d1117"/><rect x="${labelWidth}" width="${messageWidth}" height="28" fill="${color}"/><rect width="${totalWidth}" height="28" fill="url(#badge-sheen)"/></g><g fill="#fff"><svg x="9" y="6" width="16" height="16" viewBox="0 0 16 16" aria-hidden="true"><path fill="#fff" d="${githubPath}"/></svg><g font-family="Verdana,Geneva,DejaVu Sans,sans-serif" font-size="10" font-weight="700" text-anchor="middle"><text x="${labelTextX}" y="18" textLength="${Math.max(1, Math.round(measure(normalizedLabel)))}" lengthAdjust="spacing">${escapeXml(normalizedLabel)}</text><text x="${messageTextX}" y="18" textLength="${Math.max(1, Math.round(measure(normalizedMessage)))}" lengthAdjust="spacing">${escapeXml(normalizedMessage)}</text></g></g></svg>\n`
}

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

const releaseMessage = latestRelease?.tag_name ?? 'none'
const downloadsMessage = new Intl.NumberFormat('en-US').format(totalDownloads)

await mkdir('docs/assets', { recursive: true })
await Promise.all([
  writeFile('docs/assets/levyra-release.svg', makeBadge({ label: 'release', message: releaseMessage, color: '#7F52FF' }), 'utf8'),
  writeFile('docs/assets/levyra-downloads.svg', makeBadge({ label: 'downloads', message: downloadsMessage, color: '#0A84FF' }), 'utf8')
])
