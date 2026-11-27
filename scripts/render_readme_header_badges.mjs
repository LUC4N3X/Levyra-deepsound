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

const CHIP_ROSETTE = scallopPath(21, 18, 11.4, 14.2, 10);

// Official Material Symbols optically normalized to identical ~16.5px visual mass centered at (21, 18)
const infoIcons = {
  LATEST: '<g transform="translate(11.4 8.4) scale(0.80)"><path fill="currentColor" d="M21.41 11.58l-9-9C12.05 2.22 11.55 2 11 2H4c-1.1 0-2 .9-2 2v7c0 .55.22 1.05.59 1.42l9 9c.36.36.86.58 1.41.58s1.05-.22 1.41-.59l7-7c.37-.36.59-.86.59-1.41s-.23-1.06-.59-1.42zM5.5 7C4.67 7 4 6.33 4 5.5S4.67 4 5.5 4 7 4.67 7 5.5 6.33 7 5.5 7z"/></g>',
  LICENSE: '<g transform="translate(11.5 8.5) scale(0.79)"><path fill="currentColor" d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm-2 16l-4-4 1.41-1.41L10 14.17l6.59-6.59L18 9l-8 8z"/></g>',
  DOCS: '<g transform="translate(10.8 7.8) scale(0.85)"><path fill="currentColor" d="M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/></g>',
};

// Delicate Material 3 Expressive pastel palettes + sculptural triptych corners
const chipSpecs = {
  LATEST: { corners: [18, 7, 18, 7], dark: ['#F6D2E0', '#5A2238'], light: ['#FCE4EE', '#5A2238'] },
  LICENSE: { corners: [18, 18, 18, 18], dark: ['#CBEFE0', '#144230'], light: ['#DCF6EB', '#144230'] },
  DOCS: { corners: [7, 18, 7, 18], dark: ['#FAE3B4', '#4B3409'], light: ['#FDF0CF', '#4B3409'] },
};

function makeExpressiveChip({ key, description, display, vw, mw, mobile = false, light = false }) {
  const spec = chipSpecs[key];
  const [bg, fg] = spec[light ? 'light' : 'dark'];
  const w = mobile ? mw : vw;
  const h = mobile ? 32 : 36;
  const body = roundedCornersPath(0, 0, vw, 36, spec.corners);
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${vw} 36" role="img" aria-label="${escapeXml(description)}">
  <title>${escapeXml(description)}</title>
  <path d="${body}" fill="${bg}"/>
  <path d="${CHIP_ROSETTE}" fill="${fg}" fill-opacity="0.11"/>
  <g style="color:${fg}">
    ${infoIcons[key]}
  </g>
  <text x="40" y="23" fill="${fg}" font-family="Roboto,system-ui,-apple-system,'Segoe UI',Arial,sans-serif" font-size="14" font-weight="600" letter-spacing="0.05">${escapeXml(display)}</text>
</svg>
`;
}

// 100% official brand SVG vector icons mathematically normalized to identical 22.0 x 22.0px visual box centered at (0, 0)
const brandIcons = {
  github: `
    <g transform="translate(-11 -11) scale(0.9167)">
      <path fill="currentColor" d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61C4.422 18.07 3.633 17.7 3.633 17.7c-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.606-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"/>
    </g>
  `,
  fdroid: `
    <g transform="translate(-11 -11) scale(0.9167)">
      <path fill="currentColor" d="M20.472,10.081H3.528c-0.877,0-1.589,0.711-1.589,1.589v10.59c0,0.877,0.711,1.589,1.589,1.589h16.944 c0.877,0,1.589-0.711,1.589-1.589V11.67C22.061,10.792,21.349,10.081,20.472,10.081z M12,22.525c-3.066,0-5.56-2.494-5.56-5.56 s2.494-5.56,5.56-5.56c3.066,0,5.56,2.494,5.56,5.56S15.066,22.525,12,22.525z M12,12.411c-2.511,0-4.554,2.043-4.554,4.554 S9.489,21.519,12,21.519s4.554-2.043,4.554-4.554S14.511,12.411,12,12.411z M12,20.274c-1.563,0-2.881-1.103-3.221-2.568h1.67 c0.275,0.581,0.859,0.979,1.551,0.979c0.96,0,1.721-0.761,1.721-1.721c0-0.96-0.761-1.721-1.721-1.721 c-0.649,0-1.2,0.352-1.493,0.874H8.805c0.378-1.412,1.669-2.462,3.195-2.462c1.818,0,3.309,1.491,3.309,3.309 C15.309,18.783,13.818,20.274,12,20.274z M23.849,0.396c-0.001,0.001-0.002,0.002-0.002,0.003 c-0.002-0.002-0.004-0.003-0.006-0.005c0.001-0.001,0.002-0.003,0.004-0.004c-0.116-0.137-0.279-0.231-0.519-0.238 c-0.202,0.005-0.391,0.097-0.512,0.259l-1.818,2.353c-0.164-0.058-0.339-0.095-0.523-0.095H3.528c-0.184,0-0.358,0.038-0.523,0.095 L1.187,0.41c-0.121-0.162-0.31-0.253-0.512-0.259c-0.24,0.006-0.403,0.1-0.519,0.238c0.001,0.001,0.002,0.003,0.004,0.004 C0.157,0.395,0.155,0.397,0.153,0.399C0.153,0.398,0.152,0.397,0.151,0.396C0.085,0.474-0.146,0.822,0.139,1.22l1.909,2.471 C1.981,3.867,1.94,4.057,1.94,4.257v3.707c0,0.877,0.711,1.589,1.589,1.589h16.944c0.877,0,1.589-0.711,1.589-1.589V4.257 c0-0.2-0.041-0.39-0.109-0.566l1.909-2.471C24.146,0.822,23.915,0.474,23.849,0.396z M6.904,8.228c-0.987,0-1.787-0.8-1.787-1.787 s0.8-1.787,1.787-1.787s1.787,0.8,1.787,1.787S7.891,8.228,6.904,8.228z M17.229,8.228c-0.987,0-1.787-0.8-1.787-1.787 s0.8-1.787,1.787-1.787c0.987,0,1.787,0.8,1.787,1.787S18.216,8.228,17.229,8.228z"/>
    </g>
  `,
  openapk: `
    <g transform="translate(-12.24 -12.48) scale(0.0306)">
      <path fill="currentColor" d="M742.4,418.6c-0.2-4.1-0.5-8.1-0.8-12.2c-12.9-111.3-79-205.3-171.9-256.7l-1.7-0.9l0,0l9.2-15.9l47.9-82.9c1-1.7,1.6-3.7,1.6-5.9c0-6.6-5.3-11.9-11.9-11.9c-4.4,0-8.2,2.4-10.2,5.9l0,0l-47.9,82.9l-9.9,17.1c-43.1-19.8-93.6-31.4-146.8-31.4c-52.3,0-102,11.2-146.8,31.3l0,0.1l-2.2,0.9c0.7-0.3,1.5-0.7,2.2-1l-9.8-17.1l-47.9-83c-2.1-3.6-5.9-5.9-10.3-5.9c-6.6,0-11.9,5.3-11.9,11.9c0,2.2,0.6,4.2,1.6,6l0,0l47.9,83l9.2,15.9C137.6,201,71.5,294.8,58.5,404.8c-0.4,4.6-0.7,9.2-0.9,13.8c-0.3,5.8-0.4,11.7-0.4,17.6c0,151.8,92.5,281.2,222.5,331.6l79-217.6c-44.7-17.3-76.5-61.8-76.5-114c0-6,0.4-11.9,1.2-17.6c8.3-58.9,57.3-104.1,116.6-104.1c59.3,0,108.4,45.2,116.6,104.1c0.8,5.8,1.2,11.6,1.2,17.6c0,52.2-31.8,96.7-76.5,114l79,217.6c130-50.4,222.5-179.8,222.5-331.6C742.9,430.3,742.7,424.4,742.4,418.6z M242.1,263.8c15.8,0,28.6,12.8,28.6,28.6c0,15.8-12.8,28.5-28.6,28.6c-15.8,0-28.6-12.8-28.6-28.6C213.6,276.6,226.4,263.8,242.1,263.8z M557.8,263.8c15.8,0,28.6,12.8,28.6,28.6c0,15.8-12.8,28.5-28.6,28.5c-15.8,0-28.6-12.8-28.6-28.6C529.2,276.6,542,263.8,557.8,263.8z"/>
    </g>
  `,
  obtainium: `
    <g transform="translate(-12 -12) scale(1)">
      <path fill="currentColor" d="M16.85.97c-.269.004-.77.17-2.751.899-.668.245-1.291.48-1.758.658a40 40 0 0 0-.737.292 1 1 0 0 0-.083.04.526.526 0 0 0-.218.6c.026.092.143.429.308.89a262 262 0 0 0 1.383 3.781q.081.211.104.259a.6.6 0 0 0 .168.226c.07.05.162.07.288.06.252-.02.656-.154 1.395-.415.745-.263 1.112-.394 1.31-.44a.4.4 0 0 1 .188-.018c.037.01.062.03.103.063.058.048.082.073.09.11a.6.6 0 0 1-.031.206c-.03.121-.198.582-.43 1.197a192 192 0 0 1-1.663 4.227 41 41 0 0 1-.38.897l-.028.058-.014.023-.08.09a.14.14 0 0 1-.077.041c-.063.013-.175-.012-.4-.113-.45-.202-1.349-.702-3.246-1.75-.68-.373-1.31-.727-1.782-.996a41 41 0 0 1-.74-.433l-.073-.05c-.086-.079-.117-.163-.104-.243s.074-.16.185-.224c.044-.026.217-.1.451-.193.234-.092.53-.205.835-.315s.599-.222.829-.312l.289-.117q.113-.048.153-.07a.5.5 0 0 0 .191-.172.45.45 0 0 0 .044-.278c-.023-.238-.147-.604-.385-1.247-.207-.56-.583-1.588-.836-2.283-.126-.347-.253-.69-.357-.962a11 11 0 0 0-.216-.542.65.65 0 0 0-.266-.303.5.5 0 0 0-.369-.042q-.053.013-.278.092A280 280 0 0 0 3.371 5.8q-.108.041-.176.069-.065.025-.086.036c-.206.107-.41.354-.537.644-.03.067-.191.482-.433 1.104s-.562 1.45-.902 2.333a131 131 0 0 0-.943 2.501q-.15.418-.222.648a1.3 1.3 0 0 0-.072.297c0 .157.04.279.196.416s.423.296.886.55c.407.225 2.627 1.455 4.932 2.735 8.284 4.598 10.051 5.577 10.356 5.738l.043.023v-.008c.16.082.29.138.397.143.118.005.215-.04.315-.128a.2.2 0 0 0 .036-.048q.02-.035.048-.093.057-.116.154-.345c.13-.307.32-.777.589-1.456a903 903 0 0 0 2.687-6.944c.924-2.405 1.763-4.61 2.372-6.224a272 272 0 0 0 .919-2.474q.034-.097.05-.15.01-.027.014-.044c.003-.011.006-.015.006-.028 0-.123-.061-.248-.174-.37a2 2 0 0 0-.488-.362C20.29 2.678 18.755 1.83 17.94 1.401a8 8 0 0 0-.784-.376.7.7 0 0 0-.306-.055"/>
    </g>
  `,
  weblate: `
    <g transform="translate(-11 -11.4) scale(0.9167)">
      <path fill="currentColor" d="M9.662 3.809c-1.875 1.19-2.81 3.515-2.83 5.795-.014 2.628.666 5.258 1.988 7.305.936 1.46 2.238 2.715 3.836 3.412a6.942 6.942 0 005.647-.07c1.997-.927 3.523-2.73 4.463-4.785 1.606-3.518 1.643-7.724.12-11.295-1.146.458-2.166-.271-2.166-.271s.003 1.122-1.083 1.685c1.115 2.612 1.088 5.717-.03 8.263-.538 1.225-1.358 2.365-2.498 3.01-.917.52-2.04.625-3.052.184-1.342-.585-2.293-1.864-2.89-3.254-.466-1.067-.782-2.447-.802-3.878-.037-1.724.728-3.193 1.635-3.218.622-.024 1.427.918 1.598 2.435.158 1.543-.177 3.72-1.174 5.49.677 1.085 1.77 1.98 2.951 1.974 1.386-2.338 1.827-4.911 1.793-6.987-.02-2.28-.955-4.603-2.83-5.795-1.437-.907-3.173-.948-4.676 0zM3.278 3.9s-1.018.73-2.163.27c-1.524 3.573-1.488 7.778.12 11.296.94 2.056 2.465 3.858 4.462 4.785a6.95 6.95 0 005.523.124 9.12 9.12 0 01-1.75-1.455 11.18 11.18 0 01-1.267-1.628c-.768-.08-1.498-.482-2.003-.913-1.447-1.213-2.453-3.478-2.632-5.9-.12-1.635.14-3.354.795-4.894C3.276 5.022 3.278 3.9 3.278 3.9z"/>
    </g>
  `,
};

// Delicate, airy Material 3 Expressive pastel palettes (GitHub in warm soft Peach)
const storePalettes = {
  github: { dark: ['#FAD6C4', '#562614'], light: ['#FCE3D6', '#562614'] },
  fdroid: { dark: ['#C5ECF8', '#103B4B'], light: ['#D8F3FC', '#103B4B'] },
  openapk: { dark: ['#C9F0DC', '#133F2B'], light: ['#DCF7E9', '#133F2B'] },
  obtainium: { dark: ['#E6D5FA', '#3F225F'], light: ['#F0E4FD', '#3F225F'] },
  weblate: { dark: ['#CFE2FF', '#14345C'], light: ['#DEE9FF', '#14345C'] },
};

const WEBLATE_ROSETTE = scallopPath(28, 23, 14.5, 17.8, 10);

function makeWeblateButton({ mobile = false, light = false }) {
  const [bg, fg] = storePalettes.weblate[light ? 'light' : 'dark'];
  const vw = 238;
  const w = mobile ? 214 : vw;
  const h = mobile ? 41 : 46;
  const body = roundedCornersPath(0, 0, vw, 46, [23, 10, 23, 10]);
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${vw} 46" role="img" aria-label="Help translate Levyra on Weblate">
  <title>Help translate Levyra on Weblate</title>
  <path d="${body}" fill="${bg}"/>
  <path d="${WEBLATE_ROSETTE}" fill="${fg}" fill-opacity="0.11"/>
  <g transform="translate(28 23)" style="color:${fg}">
    ${brandIcons.weblate.trim()}
  </g>
  <text x="53" y="28.2" fill="${fg}" font-family="Roboto,system-ui,-apple-system,'Segoe UI',Arial,sans-serif" font-size="14.5" font-weight="600" letter-spacing="0.05">Translate on Weblate</text>
</svg>
`;
}

function makeExpressiveStoreButton({ key, title, label, vw, mw, padLeft, padRight, corners, mobile = false, light = false }) {
  const [bg, fg] = storePalettes[key][light ? 'light' : 'dark'];
  const w = mobile ? mw : vw;
  const h = mobile ? 43 : 48;
  const shapeW = vw - padLeft - padRight;
  const body = roundedCornersPath(padLeft, 0, shapeW, 48, corners);
  const iconCx = padLeft + 28;
  const textX = padLeft + 48;
  return `<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${vw} 48" role="img" aria-label="${escapeXml(title)}">
  <title>${escapeXml(title)}</title>
  <path d="${body}" fill="${bg}"/>
  <g transform="translate(${iconCx} 24)" style="color:${fg}">
    ${brandIcons[key].trim()}
  </g>
  <text x="${textX}" y="29.3" fill="${fg}" font-family="Roboto,system-ui,-apple-system,'Segoe UI',Arial,sans-serif" font-size="15" font-weight="600" letter-spacing="0.05">${escapeXml(label)}</text>
</svg>
`;
}

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
        makeExpressiveChip({ key: 'LATEST', description: `Latest Levyra release ${releaseVersion}`, display: releaseVersion, vw: 102, mw: 92, mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-license${suffix}`),
        makeExpressiveChip({ key: 'LICENSE', description: 'Levyra license GPL-3.0', display: 'GPL-3.0', vw: 112, mw: 101, mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-wiki${suffix}`),
        makeExpressiveChip({ key: 'DOCS', description: 'Read Levyra documentation', display: 'Docs', vw: 92, mw: 83, mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-github-download${suffix}`),
        makeExpressiveStoreButton({ key: 'github', title: 'Download Levyra from GitHub Releases', label: 'GitHub', vw: 122, mw: 110, padLeft: 0, padRight: 1.5, corners: [24, 9, 9, 24], mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-fdroid${suffix}`),
        makeExpressiveStoreButton({ key: 'fdroid', title: 'Get Levyra on F-Droid', label: 'F-Droid', vw: 126, mw: 114, padLeft: 1.5, padRight: 1.5, corners: [9, 9, 9, 9], mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-openapk${suffix}`),
        makeExpressiveStoreButton({ key: 'openapk', title: 'Get Levyra on OpenAPK', label: 'OpenAPK', vw: 140, mw: 127, padLeft: 1.5, padRight: 1.5, corners: [9, 9, 9, 9], mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-obtainium-download${suffix}`),
        makeExpressiveStoreButton({ key: 'obtainium', title: 'Install Levyra with Obtainium', label: 'Obtainium', vw: 146, mw: 132, padLeft: 1.5, padRight: 0, corners: [9, 24, 24, 9], mobile, light }),
        'utf8',
      ),
      writeFile(
        resolve(assetsDir, `levyra-weblate${suffix}`),
        makeWeblateButton({ mobile, light }),
        'utf8',
      ),
    );
  }
}

await Promise.all(writes);
console.log(`Generated ${writes.length} delicate Material 3 Expressive SVGs with equalized official icons (release ${releaseVersion}).`);
