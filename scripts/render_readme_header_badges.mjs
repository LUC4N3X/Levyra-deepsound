import { readFile, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const assetsDir = resolve('docs/assets');

const escapeXml = value => String(value)
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&apos;');

// Exact Material 3 5-key-color scheme (Primary, Secondary, Tertiary, Neutral, Neutral-Variant)
// derived from Levyra's HCT brand seed (cyan/blue primary, slate-indigo secondary, violet-rose tertiary)
// following MaterialTheme.colorScheme pairing rules (on-X strictly on X, Tone 80/20 & 30/90 dark, 40/100 & 90/10 light):
function tokens(light) {
  return light
    ? {
        primary: '#1B5EBE',
        onPrimary: '#FFFFFF',
        secondaryContainer: '#DAE2F9',
        onSecondaryContainer: '#131C2B',
        tertiaryContainer: '#FAD8FD',
        onTertiaryContainer: '#28132E',
        onSurfaceVariant: '#44474E',
        outlineVariant: '#C4C6D0',
      }
    : {
        primary: '#A8C7FA',
        onPrimary: '#062E6F',
        secondaryContainer: '#3E4759',
        onSecondaryContainer: '#DAE2F9',
        tertiaryContainer: '#573E5C',
        onTertiaryContainer: '#FAD8FD',
        onSurfaceVariant: '#C4C6D0',
        outlineVariant: '#44474E',
      };
}

function roundedCornersPath(x, y, w, h, [tl, tr, br, bl]) {
  const x1 = x + w;
  const y1 = y + h;
  return [
    `M ${(x + tl).toFixed(2)} ${y.toFixed(2)}`,
    `H ${(x1 - tr).toFixed(2)}`,
    `A ${tr} ${tr} 0 0 1 ${x1.toFixed(2)} ${(y + tr).toFixed(2)}`,
    `V ${(y1 - br).toFixed(2)}`,
    `A ${br} ${br} 0 0 1 ${(x1 - br).toFixed(2)} ${y1.toFixed(2)}`,
    `H ${(x + bl).toFixed(2)}`,
    `A ${bl} ${bl} 0 0 1 ${x.toFixed(2)} ${(y1 - bl).toFixed(2)}`,
    `V ${(y + tl).toFixed(2)}`,
    `A ${tl} ${tl} 0 0 1 ${(x + tl).toFixed(2)} ${y.toFixed(2)} Z`,
  ].join(' ');
}

const infoIcons = {
  LATEST: '<path d="M-4 -8 h6 l5 5 -9 9 -6 -6 v-4 Z M1 -5 a1.5 1.5 0 1 0 0 3 A1.5 1.5 0 0 0 1 -5 Z" fill="currentColor"/>',
  LICENSE: '<path d="M0 -8 l7 3 v4 c0 5 -3 7 -7 9 -4 -2 -7 -4 -7 -9 v-4 Z" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/><path d="M-3 0 l2 2 4 -5" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>',
  DOCS: '<path d="M-5 -7 h7 l4 4 v10 h-11 Z" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linejoin="round"/><path d="M2 -7 v4 h4 M-2 1 h5 M-2 5 h5" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>',
};

// M3 Chip spec (m3.material.io/components/chips/specs + MaterialTheme.shapes.small = 8dp):
// - Highlighted chip (LATEST release): borderless tertiaryContainer + onTertiaryContainer (M3 Tertiary accent role)
// - Standard chips (LICENSE, DOCS): flat transparent fill, 1dp outlineVariant stroke + onSurfaceVariant content
// Typography: labelLarge = Roboto Medium (500) 14/20, letter-spacing 0.1.
function makeAssistChip({ description, display, icon, vw, mw, highlighted = false, mobile = false, light = false }) {
  const t = tokens(light);
  const w = mobile ? mw : vw;
  const h = mobile ? 29 : 32;
  const rect = highlighted
    ? `<rect x="0" y="0" width="${vw}" height="32" rx="8" fill="${t.tertiaryContainer}"/>`
    : `<rect x="0.5" y="0.5" width="${vw - 1}" height="31" rx="8" fill="none" stroke="${t.outlineVariant}" stroke-width="1"/>`;
  const fg = highlighted ? t.onTertiaryContainer : t.onSurfaceVariant;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${vw} 32" role="img" aria-label="${escapeXml(description)}">
  <title>${escapeXml(description)}</title>
  ${rect}
  <g transform="translate(17 16) scale(0.95)" style="color:${fg}">
    ${icon}
  </g>
  <text x="34" y="20.8" fill="${fg}" font-family="Roboto,system-ui,-apple-system,'Segoe UI',Arial,sans-serif" font-size="14" font-weight="500" letter-spacing="0.1">${escapeXml(display)}</text>
</svg>
`;
}

// M3 Expressive Filled Button (m3.material.io/components/buttons/specs):
// Borderless Primary container + On-primary icon & single-line 500 Medium label, Full circular corners,
// inline 20dp icon with 8dp gap, no nested icon boxes.
function makeFilledGithubButton({ mobile = false, light = false }) {
  const t = tokens(light);
  const vw = 116;
  const w = mobile ? 105 : vw;
  const h = mobile ? 40 : 44;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${vw} 44" role="img" aria-label="Download Levyra from GitHub Releases">
  <title>Download Levyra from GitHub Releases</title>
  <rect x="0" y="0" width="${vw}" height="44" rx="22" fill="${t.primary}"/>
  <g transform="translate(18 12) scale(0.8333)">
    <path fill="${t.onPrimary}" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"/>
  </g>
  <text x="46" y="27.2" fill="${t.onPrimary}" font-family="Roboto,system-ui,-apple-system,'Segoe UI',Arial,sans-serif" font-size="14.5" font-weight="500" letter-spacing="0.1">GitHub</text>
</svg>
`;
}

// M3 Expressive Connected Tonal Button Group (m3.material.io/components/button-groups/specs):
// Borderless Secondary container + On-secondary-container icon & single-line 500 Medium label,
// Outer corners Full (22), inner corners 8dp, 2dp inner spacing (1px pad per adjacent edge), no nested boxes.
function makeConnectedTonalButton({ title, label, vw, mw, padLeft, padRight, corners, icon, mobile = false, light = false }) {
  const t = tokens(light);
  const w = mobile ? mw : vw;
  const h = mobile ? 40 : 44;
  const shapeW = vw - padLeft - padRight;
  const body = roundedCornersPath(padLeft, 0, shapeW, 44, corners);
  const iconCx = padLeft + 26;
  const textX = padLeft + 44;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${vw} 44" role="img" aria-label="${escapeXml(title)}">
  <title>${escapeXml(title)}</title>
  <path d="${body}" fill="${t.secondaryContainer}"/>
  <g transform="translate(${iconCx} 22)" style="color:${t.onSecondaryContainer}">
    ${icon.trim()}
  </g>
  <text x="${textX}" y="27.2" fill="${t.onSecondaryContainer}" font-family="Roboto,system-ui,-apple-system,'Segoe UI',Arial,sans-serif" font-size="14.5" font-weight="500" letter-spacing="0.1">${escapeXml(label)}</text>
</svg>
`;
}

// Inline 20dp monochrome icons centered at (0, 0)
const fdroidIcon = `
  <g transform="scale(0.86)">
    <path d="M-6.2 -7.6 L-8.6 -10.8 M6.2 -7.6 L8.6 -10.8" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
    <rect x="-9" y="-7.8" width="18" height="6.6" rx="2.3" fill="none" stroke="currentColor" stroke-width="1.8"/>
    <circle cx="-4" cy="-4.5" r="1.15" fill="currentColor"/>
    <circle cx="4" cy="-4.5" r="1.15" fill="currentColor"/>
    <rect x="-9" y="1" width="18" height="9" rx="2.5" fill="none" stroke="currentColor" stroke-width="1.8"/>
    <circle cx="0" cy="5.5" r="2.4" fill="none" stroke="currentColor" stroke-width="1.7"/>
  </g>
`;

const openapkIcon = `
  <g transform="translate(-9.6 -9.6) scale(0.024)">
    <path fill="currentColor" d="M742.4,418.6c-0.2-4.1-0.5-8.1-0.8-12.2c-12.9-111.3-79-205.3-171.9-256.7l-1.7-0.9l0,0l9.2-15.9l47.9-82.9c1-1.7,1.6-3.7,1.6-5.9c0-6.6-5.3-11.9-11.9-11.9c-4.4,0-8.2,2.4-10.2,5.9l0,0l-47.9,82.9l-9.9,17.1c-43.1-19.8-93.6-31.4-146.8-31.4c-52.3,0-102,11.2-146.8,31.3l0,0.1l-2.2,0.9c0.7-0.3,1.5-0.7,2.2-1l-9.8-17.1l-47.9-83c-2.1-3.6-5.9-5.9-10.3-5.9c-6.6,0-11.9,5.3-11.9,11.9c0,2.2,0.6,4.2,1.6,6l0,0l47.9,83l9.2,15.9C137.6,201,71.5,294.8,58.5,404.8c-0.4,4.6-0.7,9.2-0.9,13.8c-0.3,5.8-0.4,11.7-0.4,17.6c0,151.8,92.5,281.2,222.5,331.6l79-217.6c-44.7-17.3-76.5-61.8-76.5-114c0-6,0.4-11.9,1.2-17.6c8.3-58.9,57.3-104.1,116.6-104.1c59.3,0,108.4,45.2,116.6,104.1c0.8,5.8,1.2,11.6,1.2,17.6c0,52.2-31.8,96.7-76.5,114l79,217.6c130-50.4,222.5-179.8,222.5-331.6C742.9,430.3,742.7,424.4,742.4,418.6z M242.1,263.8c15.8,0,28.6,12.8,28.6,28.6c0,15.8-12.8,28.5-28.6,28.6c-15.8,0-28.6-12.8-28.6-28.6C213.6,276.6,226.4,263.8,242.1,263.8z M557.8,263.8c15.8,0,28.6,12.8,28.6,28.6c0,15.8-12.8,28.5-28.6,28.5c-15.8,0-28.6-12.8-28.6-28.6C529.2,276.6,542,263.8,557.8,263.8z"/>
  </g>
`;

const obtainiumIcon = `
  <g transform="scale(0.92)">
    <path d="M-7.2 -8.2 H7.2 L10 -1.8 L0 9.4 L-10 -1.8 Z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/>
    <path d="M0 -4.2 V3.4 M-3.2 0.4 L0 3.6 L3.2 0.4" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
  </g>
`;

async function readCurrentReleaseVersion() {
  try {
    const raw = await readFile(resolve(assetsDir, 'levyra-release.svg'), 'utf8');
    const match = raw.match(/v\d+\.\d+\.\d+(?:[-+][\w.-]+)?/);
    return match ? match[0] : 'v3.2.5';
  } catch {
    return 'v3.2.5';
  }
}

const releaseVersion = await readCurrentReleaseVersion();

const writes = [];
for (const light of [false, true]) {
  for (const mobile of [false, true]) {
    const suffix = `${mobile ? '-mobile' : ''}${light ? '-light' : ''}.svg`;

    writes.push(
      writeFile(
        resolve(assetsDir, `levyra-release${suffix}`),
        makeAssistChip({ description: `Latest Levyra release ${releaseVersion}`, display: releaseVersion, icon: infoIcons.LATEST, vw: 94, mw: 86, highlighted: true, mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-license${suffix}`),
        makeAssistChip({ description: 'Levyra license GPL-3.0', display: 'GPL-3.0', icon: infoIcons.LICENSE, vw: 104, mw: 94, mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-wiki${suffix}`),
        makeAssistChip({ description: 'Read Levyra documentation', display: 'Docs', icon: infoIcons.DOCS, vw: 86, mw: 78, mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-github-download${suffix}`),
        makeFilledGithubButton({ mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-fdroid${suffix}`),
        makeConnectedTonalButton({ title: 'Get Levyra on F-Droid', label: 'F-Droid', vw: 114, mw: 104, padLeft: 0, padRight: 1, corners: [22, 8, 8, 22], icon: fdroidIcon, mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-openapk${suffix}`),
        makeConnectedTonalButton({ title: 'Get Levyra on OpenAPK', label: 'OpenAPK', vw: 128, mw: 116, padLeft: 1, padRight: 1, corners: [8, 8, 8, 8], icon: openapkIcon, mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-obtainium-download${suffix}`),
        makeConnectedTonalButton({ title: 'Install Levyra with Obtainium', label: 'Obtainium', vw: 134, mw: 122, padLeft: 1, padRight: 0, corners: [8, 22, 22, 8], icon: obtainiumIcon, mobile, light }),
        'utf8',
      ),
    );
  }
}

await Promise.all(writes);
console.log(`Generated ${writes.length} spec-compliant Material 3 Expressive SVGs (release ${releaseVersion}).`);
