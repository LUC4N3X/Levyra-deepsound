import { readFile, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';

const assetsDir = resolve('docs/assets');

const escapeXml = value => String(value)
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&apos;');

function scallopPath(cx, cy, rValley, rPeak, lobes = 10) {
  const step = (2 * Math.PI) / lobes;
  const halfStep = step / 2;
  const dMid = rValley * Math.cos(halfStep);
  const chord = 2 * rValley * Math.sin(halfStep);
  const sagitta = rPeak - dMid;
  const radius = sagitta / 2 + (chord ** 2) / (8 * sagitta);
  const x0 = cx + rValley * Math.sin(-halfStep);
  const y0 = cy - rValley * Math.cos(-halfStep);
  const parts = [`M ${x0.toFixed(2)} ${y0.toFixed(2)}`];
  for (let i = 0; i < lobes; i += 1) {
    const aEnd = (i + 0.5) * step;
    const xEnd = cx + rValley * Math.sin(aEnd);
    const yEnd = cy - rValley * Math.cos(aEnd);
    parts.push(`A ${radius.toFixed(2)} ${radius.toFixed(2)} 0 0 1 ${xEnd.toFixed(2)} ${yEnd.toFixed(2)}`);
  }
  parts.push('Z');
  return parts.join(' ');
}

function asymmetricRectPath(w, h, [tl, tr, br, bl], inset = 0.75) {
  const x0 = inset;
  const y0 = inset;
  const x1 = w - inset;
  const y1 = h - inset;
  return [
    `M ${(x0 + tl).toFixed(2)} ${y0.toFixed(2)}`,
    `H ${(x1 - tr).toFixed(2)}`,
    `A ${tr} ${tr} 0 0 1 ${x1.toFixed(2)} ${(y0 + tr).toFixed(2)}`,
    `V ${(y1 - br).toFixed(2)}`,
    `A ${br} ${br} 0 0 1 ${(x1 - br).toFixed(2)} ${y1.toFixed(2)}`,
    `H ${(x0 + bl).toFixed(2)}`,
    `A ${bl} ${bl} 0 0 1 ${x0.toFixed(2)} ${(y1 - bl).toFixed(2)}`,
    `V ${(y0 + tl).toFixed(2)}`,
    `A ${tl} ${tl} 0 0 1 ${(x0 + tl).toFixed(2)} ${y0.toFixed(2)} Z`,
  ].join(' ');
}

const infoIcons = {
  LATEST: '<path d="M-4 -8 h6 l5 5 -9 9 -6 -6 v-4 Z M1 -5 a1.5 1.5 0 1 0 0 3 A1.5 1.5 0 0 0 1 -5 Z" fill="currentColor"/>',
  LICENSE: '<path d="M0 -8 l7 3 v4 c0 5 -3 7 -7 9 -4 -2 -7 -4 -7 -9 v-4 Z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><path d="M-3 0 l2 2 4 -5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>',
  DOCS: '<path d="M-5 -7 h7 l4 4 v10 h-11 Z" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round"/><path d="M2 -7 v4 h4 M-2 1 h5 M-2 5 h5" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>',
};

function makeAssistChip({ description, display, icon, vw, mw, mobile = false, light = false }) {
  const bg = light ? '#F2F6FD' : '#171E2B';
  const stroke = light ? '#CBD9F2' : '#2B374D';
  const accent = light ? '#1B5EBE' : '#A8C7FA';
  const wellOpacity = light ? '0.10' : '0.13';
  const text = light ? '#142645' : '#E8F0FE';
  const w = mobile ? mw : vw;
  const h = mobile ? 30 : 34;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${vw} 34" role="img" aria-label="${escapeXml(description)}">
  <title>${escapeXml(description)}</title>
  <rect x="0.75" y="0.75" width="${vw - 1.5}" height="32.5" rx="16.25" fill="${bg}" stroke="${stroke}" stroke-width="1.2"/>
  <circle cx="19" cy="17" r="11" fill="${accent}" fill-opacity="${wellOpacity}"/>
  <g transform="translate(19 17) scale(0.86)" style="color:${accent}">
    ${icon}
  </g>
  <text x="38" y="21.5" fill="${text}" font-family="system-ui,-apple-system,'Segoe UI',Roboto,Arial,sans-serif" font-size="13" font-weight="640" letter-spacing="-.15">${escapeXml(display)}</text>
</svg>
`;
}

const GITHUB_SCALLOP = scallopPath(30, 27, 14.4, 17.6, 10);

function makePrimaryGithubButton({ mobile = false, light = false }) {
  const bg = light ? '#1B5EBE' : '#A8C7FA';
  const fg = light ? '#FFFFFF' : '#062E6F';
  const cookieOpacity = light ? '0.16' : '0.12';
  const subOpacity = light ? '0.84' : '0.78';
  const w = mobile ? 168 : 192;
  const h = mobile ? 47.25 : 54;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 192 54" role="img" aria-label="Download Levyra from GitHub Releases">
  <title>Download Levyra from GitHub Releases</title>
  <rect x="0.5" y="0.5" width="191" height="53" rx="26.5" fill="${bg}"/>
  <path d="${GITHUB_SCALLOP}" fill="${fg}" fill-opacity="${cookieOpacity}"/>
  <g transform="translate(19.5 16.5) scale(0.88)">
    <path fill="${fg}" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z"/>
  </g>
  <text x="58" y="25.5" fill="${fg}" font-family="system-ui,-apple-system,'Segoe UI',Roboto,Arial,sans-serif" font-size="15.5" font-weight="720" letter-spacing="-.25">GitHub</text>
  <text x="58" y="40.2" fill="${fg}" fill-opacity="${subOpacity}" font-family="system-ui,-apple-system,'Segoe UI',Roboto,Arial,sans-serif" font-size="10.5" font-weight="580" letter-spacing=".1">APK · Win · Linux</text>
</svg>
`;
}

function makeConnectedButton({ title, label, corners, icon, mobile = false, light = false }) {
  const bg = light ? '#EAF1FE' : '#1A2232';
  const stroke = light ? '#C5D7F5' : '#2E3B54';
  const accent = light ? '#1B5EBE' : '#A8C7FA';
  const wellOpacity = light ? '0.10' : '0.13';
  const text = light ? '#142645' : '#E8F0FE';
  const w = mobile ? 136 : 154;
  const h = mobile ? 47.69 : 54;
  const body = asymmetricRectPath(154, 54, corners, 0.75);
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 154 54" role="img" aria-label="${escapeXml(title)}">
  <title>${escapeXml(title)}</title>
  <path d="${body}" fill="${bg}" stroke="${stroke}" stroke-width="1.2"/>
  <rect x="13" y="10" width="34" height="34" rx="11" fill="${accent}" fill-opacity="${wellOpacity}"/>
  <g style="color:${accent}">
    ${icon.trim()}
  </g>
  <text x="57" y="32.4" fill="${text}" font-family="system-ui,-apple-system,'Segoe UI',Roboto,Arial,sans-serif" font-size="15" font-weight="660" letter-spacing="-.2">${escapeXml(label)}</text>
</svg>
`;
}

// Pure-vector monochrome store icons centered in (30, 27)
const fdroidIcon = `
  <g transform="translate(30 27)">
    <path d="M-6.2 -7.6 L-8.6 -10.8 M6.2 -7.6 L8.6 -10.8" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
    <rect x="-9" y="-7.8" width="18" height="6.8" rx="2.4" fill="currentColor"/>
    <circle cx="-4.2" cy="-4.4" r="1.2" fill="#0D1117" fill-opacity="0.55"/>
    <circle cx="4.2" cy="-4.4" r="1.2" fill="#0D1117" fill-opacity="0.55"/>
    <rect x="-9" y="0.6" width="18" height="9.4" rx="2.6" fill="none" stroke="currentColor" stroke-width="1.8"/>
    <circle cx="0" cy="5.3" r="2.7" fill="none" stroke="currentColor" stroke-width="1.7"/>
  </g>
`;

const openapkIcon = `
  <g transform="translate(18.6 15.6) scale(0.0285)">
    <path fill="currentColor" d="M742.4,418.6c-0.2-4.1-0.5-8.1-0.8-12.2c-12.9-111.3-79-205.3-171.9-256.7l-1.7-0.9l0,0l9.2-15.9l47.9-82.9c1-1.7,1.6-3.7,1.6-5.9c0-6.6-5.3-11.9-11.9-11.9c-4.4,0-8.2,2.4-10.2,5.9l0,0l-47.9,82.9l-9.9,17.1c-43.1-19.8-93.6-31.4-146.8-31.4c-52.3,0-102,11.2-146.8,31.3l0,0.1l-2.2,0.9c0.7-0.3,1.5-0.7,2.2-1l-9.8-17.1l-47.9-83c-2.1-3.6-5.9-5.9-10.3-5.9c-6.6,0-11.9,5.3-11.9,11.9c0,2.2,0.6,4.2,1.6,6l0,0l47.9,83l9.2,15.9C137.6,201,71.5,294.8,58.5,404.8c-0.4,4.6-0.7,9.2-0.9,13.8c-0.3,5.8-0.4,11.7-0.4,17.6c0,151.8,92.5,281.2,222.5,331.6l79-217.6c-44.7-17.3-76.5-61.8-76.5-114c0-6,0.4-11.9,1.2-17.6c8.3-58.9,57.3-104.1,116.6-104.1c59.3,0,108.4,45.2,116.6,104.1c0.8,5.8,1.2,11.6,1.2,17.6c0,52.2-31.8,96.7-76.5,114l79,217.6c130-50.4,222.5-179.8,222.5-331.6C742.9,430.3,742.7,424.4,742.4,418.6z M242.1,263.8c15.8,0,28.6,12.8,28.6,28.6c0,15.8-12.8,28.5-28.6,28.6c-15.8,0-28.6-12.8-28.6-28.6C213.6,276.6,226.4,263.8,242.1,263.8z M557.8,263.8c15.8,0,28.6,12.8,28.6,28.6c0,15.8-12.8,28.5-28.6,28.5c-15.8,0-28.6-12.8-28.6-28.6C529.2,276.6,542,263.8,557.8,263.8z"/>
  </g>
`;

const obtainiumIcon = `
  <g transform="translate(30 27)">
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
        makeAssistChip({ description: `Latest Levyra release ${releaseVersion}`, display: releaseVersion, icon: infoIcons.LATEST, vw: 102, mw: 92, mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-license${suffix}`),
        makeAssistChip({ description: 'Levyra license GPL-3.0', display: 'GPL-3.0', icon: infoIcons.LICENSE, vw: 112, mw: 100, mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-wiki${suffix}`),
        makeAssistChip({ description: 'Read Levyra documentation', display: 'Docs', icon: infoIcons.DOCS, vw: 92, mw: 84, mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-github-download${suffix}`),
        makePrimaryGithubButton({ mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-fdroid${suffix}`),
        makeConnectedButton({ title: 'Get Levyra on F-Droid', label: 'F-Droid', corners: [26, 9, 9, 26], icon: fdroidIcon, mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-openapk${suffix}`),
        makeConnectedButton({ title: 'Get Levyra on OpenAPK', label: 'OpenAPK', corners: [9, 9, 9, 9], icon: openapkIcon, mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-obtainium-download${suffix}`),
        makeConnectedButton({ title: 'Install Levyra with Obtainium', label: 'Obtainium', corners: [9, 26, 26, 9], icon: obtainiumIcon, mobile, light }),
        'utf8',
      ),
    );
  }
}

await Promise.all(writes);
console.log(`Generated ${writes.length} Material 3 Expressive header SVGs (release ${releaseVersion}).`);
