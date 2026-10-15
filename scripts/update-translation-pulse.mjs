import { mkdir, readFile, readdir, writeFile } from 'node:fs/promises'
import { join } from 'node:path'

const RESOURCE_ROOT = 'app/src/main/res'
const BASE_STRINGS = join(RESOURCE_ROOT, 'values', 'strings.xml')
const LOCALE_CONFIG = join(RESOURCE_ROOT, 'xml', 'locales_config.xml')
const ASSET_DIR = 'docs/assets'
const STRING_CATALOG_DIR = 'app/src/main/java/com/luc4n3x/levyra/ui/i18n'

const escapeXml = value => String(value)
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&apos;')

const canonicalTag = value => String(value)
  .trim()
  .replaceAll('_', '-')
  .toLowerCase()

const qualifierToTag = name => {
  const suffix = name.replace(/^values-/, '')
  return suffix.startsWith('b+')
    ? suffix.slice(2).replaceAll('+', '-')
    : suffix
}

const readStringKeys = xml => {
  const pattern = /<string\s+name="([^"]+)"([^>]*)>/g
  const keys = new Set()
  for (const match of xml.matchAll(pattern)) {
    if (!match[2].includes('translatable="false"')) keys.add(match[1])
  }
  return keys
}

const readLocalizedKeys = xml => {
  const pattern = /<string\s+name="([^"]+)"/g
  return new Set(Array.from(xml.matchAll(pattern), match => match[1]))
}

// Curated native display names & codes
const LOCALE_META = {
  'en': { name: 'English', code: 'EN' },
  'it': { name: 'Italiano', code: 'IT' },
  'es': { name: 'Español', code: 'ES' },
  'fr': { name: 'Français', code: 'FR' },
  'de': { name: 'Deutsch', code: 'DE' },
  'pt': { name: 'Português', code: 'PT' },
  'nl': { name: 'Nederlands', code: 'NL' },
  'pl': { name: 'Polski', code: 'PL' },
  'ro': { name: 'Română', code: 'RO' },
  'el': { name: 'Ελληνικά', code: 'EL' },
  'sv': { name: 'Svenska', code: 'SV' },
  'da': { name: 'Dansk', code: 'DA' },
  'cs': { name: 'Čeština', code: 'CS' },
  'sk': { name: 'Slovenčina', code: 'SK' },
  'hr': { name: 'Hrvatski', code: 'HR' },
  'sr': { name: 'Српски', code: 'SR' },
  'sq': { name: 'Shqip', code: 'SQ' },
  'bg': { name: 'Български', code: 'BG' },
  'hu': { name: 'Magyar', code: 'HU' },
  'fi': { name: 'Suomi', code: 'FI' },
  'et': { name: 'Eesti', code: 'ET' },
  'nb': { name: 'Norsk bokmål', code: 'NB' },
  'ca': { name: 'Català', code: 'CA' },
  'uk': { name: 'Українська', code: 'UK' },
  'ru': { name: 'Русский', code: 'RU' },
  'kk': { name: 'Қазақша', code: 'KK' },
  'tr': { name: 'Türkçe', code: 'TR' },
  'ar': { name: 'العربية', code: 'AR' },
  'fa': { name: 'فارسی', code: 'FA' },
  'zh-hans': { name: '简体中文', code: 'ZH' },
  'zh-hant': { name: '繁體中文', code: 'ZH-TW' },
  'ja': { name: '日本語', code: 'JA' },
  'ko': { name: '한국어', code: 'KO' },
  'hi': { name: 'हिन्दी', code: 'HI' },
  'bn': { name: 'বাংলা', code: 'BN' },
  'id': { name: 'Bahasa Indonesia', code: 'ID' },
  'ms': { name: 'Bahasa Melayu', code: 'MS' },
  'vi': { name: 'Tiếng Việt', code: 'VI' },
  'th': { name: 'ไทย', code: 'TH' },
  'fil': { name: 'Filipino', code: 'FIL' },
  'he': { name: 'עברית', code: 'HE' }
}

const countCatalogStrings = async () => {
  const keys = new Set()
  for (const file of await readdir(STRING_CATALOG_DIR)) {
    if (!file.endsWith('.kt')) continue
    const source = await readFile(join(STRING_CATALOG_DIR, file), 'utf8')
    for (const match of source.matchAll(/get\(\) = value\("([^"]+)"\)/g)) keys.add(match[1])
    for (const block of source.matchAll(/KeyOrder\s*=\s*listOf\(([\s\S]*?)\)/g)) {
      for (const key of block[1].matchAll(/"([^"]+)"/g)) keys.add(key[1])
    }
  }
  return keys.size
}

const collectRepositoryCoverage = async () => {
  const [baseXml, localeConfig, entries] = await Promise.all([
    readFile(BASE_STRINGS, 'utf8'),
    readFile(LOCALE_CONFIG, 'utf8'),
    readdir(RESOURCE_ROOT, { withFileTypes: true })
  ])

  const expectedKeys = readStringKeys(baseXml)
  if (expectedKeys.size === 0) throw new Error('No translatable Android strings found')

  const localePattern = /<locale\s+android:name="([^"]+)"\s*\/>/g
  const localeTags = Array.from(localeConfig.matchAll(localePattern), match => match[1])
  if (localeTags.length === 0) throw new Error('No Android locales found')

  const localizedFiles = new Map()
  for (const entry of entries) {
    if (!entry.isDirectory() || !entry.name.startsWith('values-')) continue
    const tag = canonicalTag(qualifierToTag(entry.name))
    try {
      const xml = await readFile(join(RESOURCE_ROOT, entry.name, 'strings.xml'), 'utf8')
      localizedFiles.set(tag, readLocalizedKeys(xml))
    } catch {}
  }

  const languages = localeTags.map(code => {
    const canon = canonicalTag(code)
    const meta = LOCALE_META[canon] || { name: code, code: code.toUpperCase() }
    if (canon === 'en') {
      return {
        name: meta.name,
        code: meta.code,
        percent: 100,
        translated: expectedKeys.size,
        total: expectedKeys.size
      }
    }

    const localized = localizedFiles.get(canon) || new Set()
    const translated = Array.from(expectedKeys).filter(key => localized.has(key)).length
    return {
      name: meta.name,
      code: meta.code,
      percent: (translated / expectedKeys.size) * 100,
      translated,
      total: expectedKeys.size
    }
  })

  const translated = languages.reduce((sum, l) => sum + l.translated, 0)
  const total = languages.reduce((sum, l) => sum + l.total, 0)

  return {
    languages,
    globalPercent: total === 0 ? 0 : (translated / total) * 100,
    stringCount: expectedKeys.size,
    catalogCount: await countCatalogStrings()
  }
}

const FONT = "-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif"

const SCRIPT_GROUPS = [
  { id: 'latin', label: 'LATIN' },
  { id: 'cyrillic', label: 'CYRILLIC + GREEK' },
  { id: 'cjk', label: 'CJK' },
  { id: 'rtl', label: 'ARABIC + HEBREW' },
  { id: 'brahmic', label: 'INDIC + THAI' }
]

const scriptGroupFor = code => {
  const tag = code.toLowerCase()
  if (['ru', 'uk', 'bg', 'sr', 'kk', 'el'].includes(tag)) return 'cyrillic'
  if (['ar', 'fa', 'he'].includes(tag)) return 'rtl'
  if (['zh', 'zh-tw', 'ja', 'ko'].includes(tag)) return 'cjk'
  if (['hi', 'bn', 'th'].includes(tag)) return 'brahmic'
  return 'latin'
}

const getTheme = isDark => ({
  background: isDark ? '#0D1117' : '#FBF8F6',
  surface: isDark ? '#211923' : '#F4E8EF',
  surfaceStrong: isDark ? '#352735' : '#EBCFDF',
  ink: isDark ? '#F7F1F5' : '#241B21',
  muted: isDark ? '#A99DA6' : '#70636B',
  rule: isDark ? '#30363D' : '#D8D0D4',
  accent: isDark ? '#D98AB8' : '#8B416E'
})

const groupRows = languages => SCRIPT_GROUPS.map(group => {
  const members = languages.filter(language => scriptGroupFor(language.code) === group.id)
  return {
    ...group,
    count: members.length,
    examples: members.slice(0, 6).map(language => language.name).join(' · ')
  }
}).filter(group => group.count > 0)

const makePulse = ({ languages, globalPercent, catalogCount, isDark, mobile }) => {
  const theme = getTheme(isDark)
  const groups = groupRows(languages)
  const roundedPercent = Math.round(globalPercent)
  const aria = `Levyra localization coverage: ${roundedPercent}% across ${languages.length} languages`
  const description = languages.map(language => `${language.name}: ${Math.round(language.percent)}%`).join(', ')

  if (mobile) {
    const rows = groups.map((group, index) => {
      const y = 218 + index * 63
      return `<text x="34" y="${y}" class="group">${escapeXml(group.label)}</text>
  <rect x="650" y="${y - 20}" width="36" height="28" rx="14" fill="${theme.surface}"/>
  <text x="668" y="${y}" text-anchor="middle" class="count">${group.count}</text>
  <text x="34" y="${y + 22}" class="examples">${escapeXml(group.examples)}</text>
  <line x1="34" y1="${y + 39}" x2="686" y2="${y + 39}" stroke="${theme.rule}"/>`
    }).join('\n  ')
    return `<svg xmlns="http://www.w3.org/2000/svg" width="720" height="560" viewBox="0 0 720 560" role="img" aria-label="${escapeXml(aria)}">
  <title>${escapeXml(aria)}</title>
  <desc>${escapeXml(description)}</desc>
  <style>
    text { font-family: ${FONT}; }
    .eyebrow { fill: ${theme.accent}; font-size: 11px; font-weight: 750; letter-spacing: 1.8px; }
    .hero { fill: ${theme.ink}; font-size: 62px; font-weight: 720; letter-spacing: -3px; }
    .meta { fill: ${theme.muted}; font-size: 12px; }
    .group { fill: ${theme.ink}; font-size: 13px; font-weight: 700; letter-spacing: .5px; }
    .count { fill: ${theme.muted}; font-size: 15px; font-weight: 650; }
    .examples { fill: ${theme.muted}; font-size: 10px; }
    .footer { fill: ${theme.muted}; font-size: 10px; font-weight: 600; letter-spacing: .3px; }
  </style>
  <rect width="720" height="560" rx="28" fill="${theme.background}"/>
  <rect x="18" y="18" width="684" height="160" rx="36" fill="${theme.surface}"/>
  <text x="48" y="61" class="eyebrow">LANGUAGE COVERAGE</text>
  <text x="45" y="132" class="hero">${languages.length} languages</text>
  <text x="48" y="157" class="meta">${catalogCount.toLocaleString('en-US')}+ app strings</text>
  <rect x="584" y="43" width="86" height="38" rx="19" fill="${theme.surfaceStrong}"/>
  <text x="627" y="67" text-anchor="middle" class="group">${roundedPercent}%</text>
  <line x1="34" y1="190" x2="686" y2="190" stroke="${theme.rule}"/>
  ${rows}
  <text x="34" y="535" class="footer">LOCALIZED IN THE APP · NO CLOUD PROFILE</text>
</svg>`
  }

  const rows = groups.map((group, index) => {
    const y = 116 + index * 49
    return `<text x="348" y="${y}" class="group">${escapeXml(group.label)}</text>
  <rect x="1002" y="${y - 20}" width="46" height="28" rx="14" fill="${theme.surface}"/>
  <text x="1025" y="${y}" text-anchor="middle" class="count">${group.count}</text>
  <text x="540" y="${y}" class="examples">${escapeXml(group.examples)}</text>
  <line x1="348" y1="${y + 18}" x2="1048" y2="${y + 18}" stroke="${theme.rule}"/>`
  }).join('\n  ')
  return `<svg xmlns="http://www.w3.org/2000/svg" width="1100" height="390" viewBox="0 0 1100 390" role="img" aria-label="${escapeXml(aria)}">
  <title>${escapeXml(aria)}</title>
  <desc>${escapeXml(description)}</desc>
  <style>
    text { font-family: ${FONT}; }
    .eyebrow { fill: ${theme.accent}; font-size: 11px; font-weight: 750; letter-spacing: 1.8px; }
    .hero { fill: ${theme.ink}; font-size: 70px; font-weight: 720; letter-spacing: -3.5px; }
    .meta { fill: ${theme.muted}; font-size: 13px; }
    .group { fill: ${theme.ink}; font-size: 12px; font-weight: 700; letter-spacing: .5px; }
    .count { fill: ${theme.muted}; font-size: 14px; font-weight: 650; }
    .examples { fill: ${theme.muted}; font-size: 11px; }
    .footer { fill: ${theme.muted}; font-size: 10px; font-weight: 600; letter-spacing: .35px; }
  </style>
  <rect width="1100" height="390" rx="28" fill="${theme.background}"/>
  <rect x="22" y="22" width="286" height="346" rx="42" fill="${theme.surface}"/>
  <text x="54" y="72" class="eyebrow">LANGUAGE COVERAGE</text>
  <text x="49" y="166" class="hero">${languages.length}</text>
  <text x="54" y="198" class="meta">languages</text>
  <text x="54" y="224" class="meta">${catalogCount.toLocaleString('en-US')}+ app strings</text>
  <rect x="54" y="286" width="116" height="42" rx="21" fill="${theme.surfaceStrong}"/>
  <text x="112" y="312" text-anchor="middle" class="group">${roundedPercent}% complete</text>
  <text x="348" y="76" class="footer">WRITING SYSTEM</text>
  <text x="540" y="76" class="footer">SELECTED LANGUAGES</text>
  <text x="1048" y="76" text-anchor="end" class="footer">TOTAL</text>
  ${rows}
  <text x="348" y="356" class="footer">LOCALIZED IN THE APP · NO CLOUD PROFILE</text>
</svg>`
}

const updateTranslationPulse = async () => {
  const coverage = await collectRepositoryCoverage()
  await mkdir(ASSET_DIR, { recursive: true })
  await Promise.all([
    writeFile(ASSET_DIR + '/levyra-translation-pulse.svg', makePulse({ ...coverage, isDark: true, mobile: false }), 'utf8'),
    writeFile(ASSET_DIR + '/levyra-translation-pulse-light.svg', makePulse({ ...coverage, isDark: false, mobile: false }), 'utf8'),
    writeFile(ASSET_DIR + '/levyra-translation-pulse-mobile.svg', makePulse({ ...coverage, isDark: true, mobile: true }), 'utf8'),
    writeFile(ASSET_DIR + '/levyra-translation-pulse-mobile-light.svg', makePulse({ ...coverage, isDark: false, mobile: true }), 'utf8')
  ])
}

await updateTranslationPulse()
console.log('Generated translation coverage SVG.')
