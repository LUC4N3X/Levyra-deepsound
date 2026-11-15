import { mkdir, readFile } from 'node:fs/promises';
import { createRequire } from 'node:module';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseArgs } from 'node:util';

const { values } = parseArgs({ options: {
  'sharp-module': { type: 'string', default: 'sharp' },
  output: { type: 'string' },
} });
const require = createRequire(import.meta.url);
const sharp = require(values['sharp-module'] === 'sharp' ? 'sharp' : resolve(values['sharp-module']));
const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const source = resolve(root, 'docs/assets/showcase/editorial-cards');
const output = values.output ? resolve(values.output) : resolve(root, 'docs/assets/showcase/readme');

const width = 900;
const height = 1800;
const gap = 26;
const cardRadius = 48;
const clear = { r: 0, g: 0, b: 0, alpha: 0 };
const svg = (content, w = width, h = height) => Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}">${content}</svg>`);
const xml = value => value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
const blank = (w, h) => sharp({ create: { width: w, height: h, channels: 4, background: clear } });
const cardMask = svg(`<rect width="${width}" height="${height}" rx="${cardRadius}" fill="white"/>`);

// Delicate Material 3 Expressive pastel tonal palettes aligned with Levyra's UI & badges.
const palettes = {
  cobalt: {
    top: '#F6F9FF', mid: '#EFF4FE', bot: '#E5EEFD',
    motif: '#DCE7FC', motifAlt: '#E2ECFD',
    pillBg: '#E1EAFC', pillStroke: '#3558B8', pillText: '#2A4AA3',
    ink: '#182035', sub: '#4C5A78', shadow: '#1D2F5C', border: '#3558B8',
  },
  orchid: {
    top: '#FDF7FA', mid: '#F9EFF5', bot: '#F4E5EE',
    motif: '#EDD7E5', motifAlt: '#F1DFEB',
    pillBg: '#F1DCE9', pillStroke: '#8D3E70', pillText: '#7D3262',
    ink: '#261822', sub: '#614A59', shadow: '#4A1F3A', border: '#8D3E70',
  },
  ivory: {
    top: '#FDF9F3', mid: '#F9F2E7', bot: '#F3E9DA',
    motif: '#EBDCCB', motifAlt: '#EFE3D5',
    pillBg: '#EFE2D1', pillStroke: '#87582C', pillText: '#754920',
    ink: '#282017', sub: '#645646', shadow: '#4A3620', border: '#87582C',
  },
  sky: {
    top: '#F4FAFD', mid: '#ECF5FA', bot: '#E1EEF6',
    motif: '#D3E6F2', motifAlt: '#DCECF5',
    pillBg: '#D9ECF6', pillStroke: '#246384', pillText: '#1D5472',
    ink: '#15242E', sub: '#486070', shadow: '#193C52', border: '#246384',
  },
  sage: {
    top: '#F4FBF8', mid: '#ECF6F1', bot: '#E2F0E9',
    motif: '#D3E8DE', motifAlt: '#DCEEE5',
    pillBg: '#D9EDE3', pillStroke: '#276B55', pillText: '#1F5B48',
    ink: '#152620', sub: '#456157', shadow: '#184234', border: '#276B55',
  },
  iris: {
    top: '#F8F6FE', mid: '#F2EEFC', bot: '#EAE3FA',
    motif: '#DFD5F7', motifAlt: '#E6DDF8',
    pillBg: '#E6DFF8', pillStroke: '#5B45A8', pillText: '#4D3996',
    ink: '#1E1932', sub: '#534C70', shadow: '#322563', border: '#5B45A8',
  },
  coral: {
    top: '#FDF6F7', mid: '#F9ECEE', bot: '#F4E0E4',
    motif: '#EED3D8', motifAlt: '#F2DCE0',
    pillBg: '#F4DCE1', pillStroke: '#A03F54', pillText: '#8C3246',
    ink: '#29171B', sub: '#664A50', shadow: '#521E29', border: '#A03F54',
  },
  terracotta: {
    top: '#FDF7F3', mid: '#F8EEE7', bot: '#F2E3D8',
    motif: '#EAD5C6', motifAlt: '#EFE0D4',
    pillBg: '#F2DFD1', pillStroke: '#9A512E', pillText: '#854222',
    ink: '#2A1C15', sub: '#685043', shadow: '#4F2B19', border: '#9A512E',
  },
  amber: {
    top: '#FDF9F1', mid: '#F9F2E3', bot: '#F3E8D1',
    motif: '#ECDCB9', motifAlt: '#F0E3C7',
    pillBg: '#F1E3C3', pillStroke: '#8F621B', pillText: '#7A5212',
    ink: '#282012', sub: '#66583D', shadow: '#4D3812', border: '#8F621B',
  },
};

const cards = [
  ['01_pure_listening', 'cobalt', 'rosette', 'NO ACCOUNT', ['Just press', 'play.'], ['No Levyra account. No ads or telemetry.']],
  ['02_stay_with_the_song', 'orchid', 'scallopWave', 'NOW PLAYING', ['Stay with', 'the song.'], ['Audio, video & motion artwork.']],
  ['03_daylight_expressive', 'ivory', 'squircleOrb', 'LIGHT THEME', ['A brighter', 'kind of play.'], ['Material 3 Expressive.']],
  ['04_midnight_obsidian', 'sky', 'clover', 'DARK THEME', ['Made for', 'after dark.'], ['Dark surfaces. Vivid artwork.']],

  ['05_fresh_currents', 'cobalt', 'rosette', 'DISCOVERY', ['Discover more'], ['Search, revisit & explore.']],
  ['06_built_for_your_taste', 'terracotta', 'scallopWave', 'PLAYBACK', ['Keep listening'], ['Gapless playback & crossfade.']],
  ['07_every_genre', 'sage', 'clover', 'EXPLORE', ['Moods & genres'], ['Find music for the moment.']],
  ['08_acoustic_precision', 'sky', 'squircleOrb', 'AUDIO', ['Your sound'], ['Output, quality & audio options.']],

  ['09_keep_it_personal', 'iris', 'squircleOrb', 'LIBRARY', ['One library'], ['Local files meet streamed music.']],
  ['10_curated_for_you', 'ivory', 'rosette', 'SEARCH', ['Find your next favorite'], ['Songs, artists & albums.']],
  ['11_your_rhythm', 'sky', 'clover', 'INSIGHTS', ['Listening insights'], ['Stats calculated on your device.']],
  ['12_in_your_orbit', 'orchid', 'scallopWave', 'COLLECTIONS', ['Made for you'], ['Mixes from your listening history.']],

  ['13_tailor_every_detail', 'iris', 'clover', 'PERSONALIZATION', ['Make it yours'], ['Skins, themes & studios.']],
  ['14_fresh_off_the_stage', 'coral', 'scallopWave', 'LIVE RADIO', ['Live radio'], ['Tune in to stations worldwide.']],
  ['15_meet_the_artist', 'ivory', 'rosette', 'ARTISTS', ['Meet the artist'], ['Albums, top tracks & more.']],
  ['16_listen_together', 'sage', 'squircleOrb', 'LEVYRA JAM', ['Listen together'], ['One queue. The same Wi-Fi.']],

  ['17_sing_along', 'amber', 'scallopWave', 'SYNCED LYRICS', ['Follow every word'], ['Follow along with synced lyrics.']],
  ['18_open_the_album', 'sky', 'rosette', 'OFFLINE', ['Music to go'], ['Save music for offline listening.']],
  ['19_discover_for_you', 'cobalt', 'clover', 'SWIPE DISCOVERY', ['Hear it. Keep it.'], ['Preview tracks. Save your finds.']],
  ['20_your_year_in_music', 'orchid', 'squircleOrb', 'YOUR YEAR', ['Your year in music'], ['Your listening story, on device.']],

  ['21_spotify_style', 'sage', 'rosette', 'LEVYRA SKINS', ['Spotify Style'], ['Browse genres & new releases.']],
  ['22_apple_music_style', 'coral', 'squircleOrb', 'LEVYRA SKINS', ['Apple Music Style'], ['Light surfaces & bold type.']],
  ['23_youtube_music_style', 'iris', 'scallopWave', 'LEVYRA SKINS', ['YouTube Music Style'], ['Mood chips & music discovery.']],
  ['24_qobuz_style', 'sky', 'clover', 'LEVYRA SKINS', ['Qobuz Style'], ['An editorial take on your library.']],
];

function makeFlowerPath(cx, cy, rValley, rPeak, lobes) {
  const step = (2 * Math.PI) / lobes;
  const half = step / 2;
  const dMid = rValley * Math.cos(half);
  const chord = 2 * rValley * Math.sin(half);
  const sagitta = rPeak - dMid;
  const radius = sagitta / 2 + (chord * chord) / (8 * sagitta);
  const parts = [`M ${(cx + rValley * Math.sin(-half)).toFixed(2)} ${(cy - rValley * Math.cos(-half)).toFixed(2)}`];
  for (let i = 0; i < lobes; i++) {
    const a = (i + 0.5) * step;
    parts.push(`A ${radius.toFixed(2)} ${radius.toFixed(2)} 0 0 1 ${(cx + rValley * Math.sin(a)).toFixed(2)} ${(cy - rValley * Math.cos(a)).toFixed(2)}`);
  }
  parts.push('Z');
  return parts.join(' ');
}

function makeAsymmetricSquircle(x, y, w, h, rLarge, rSmall) {
  return [
    `M ${(x + rLarge).toFixed(2)} ${y.toFixed(2)}`,
    `H ${(x + w - rSmall).toFixed(2)}`,
    `A ${rSmall} ${rSmall} 0 0 1 ${(x + w).toFixed(2)} ${(y + rSmall).toFixed(2)}`,
    `V ${(y + h - rLarge).toFixed(2)}`,
    `A ${rLarge} ${rLarge} 0 0 1 ${(x + w - rLarge).toFixed(2)} ${(y + h).toFixed(2)}`,
    `H ${(x + rSmall).toFixed(2)}`,
    `A ${rSmall} ${rSmall} 0 0 1 ${x.toFixed(2)} ${(y + h - rSmall).toFixed(2)}`,
    `V ${(y + rLarge).toFixed(2)}`,
    `A ${rLarge} ${rLarge} 0 0 1 ${(x + rLarge).toFixed(2)} ${y.toFixed(2)} Z`,
  ].join(' ');
}

function makeWavyPath(xStart, yCenter, totalW, amplitude, halfCycles) {
  const step = totalW / halfCycles;
  const parts = [`M ${xStart} ${yCenter}`];
  for (let i = 0; i < halfCycles; i++) {
    const x1 = xStart + i * step + step * 0.5;
    const x2 = xStart + (i + 1) * step;
    const cy = yCenter + (i % 2 === 0 ? -amplitude : amplitude);
    parts.push(`Q ${x1.toFixed(1)} ${cy.toFixed(1)} ${x2.toFixed(1)} ${yCenter.toFixed(1)}`);
  }
  return parts.join(' ');
}

function motifMarkup(index, kind, p) {
  if (index === 0) {
    return [
      `<path d="${makeFlowerPath(760, 1460, 210, 255, 12)}" fill="${p.motif}" opacity="0.62"/>`,
      `<circle cx="130" cy="480" r="145" fill="${p.motifAlt}" opacity="0.52"/>`,
    ].join('');
  }
  if (index === 1) {
    return [
      `<path d="${makeFlowerPath(150, 560, 180, 224, 8)}" fill="${p.motif}" opacity="0.62"/>`,
      `<path d="${makeWavyPath(35, 1520, 420, 26, 6)}" fill="none" stroke="${p.motif}" stroke-width="18" stroke-linecap="round" opacity="0.72"/>`,
    ].join('');
  }
  if (index === 2) {
    return [
      `<path d="${makeAsymmetricSquircle(510, 320, 340, 380, 140, 44)}" fill="${p.motif}" opacity="0.58"/>`,
      `<circle cx="780" cy="1440" r="170" fill="${p.motifAlt}" opacity="0.52"/>`,
    ].join('');
  }
  if (index === 3) {
    return [
      `<path d="${makeFlowerPath(155, 620, 175, 232, 4)}" fill="${p.motif}" opacity="0.64"/>`,
      `<circle cx="150" cy="1510" r="155" fill="${p.motifAlt}" opacity="0.50"/>`,
    ].join('');
  }
  const presets = {
    rosette: [
      `<path d="${makeFlowerPath(765, 495, 200, 248, 12)}" fill="${p.motif}" opacity="0.60"/>`,
      `<circle cx="115" cy="1475" r="185" fill="${p.motifAlt}" opacity="0.52"/>`,
    ].join(''),
    scallopWave: [
      `<path d="${makeFlowerPath(135, 515, 175, 218, 8)}" fill="${p.motif}" opacity="0.60"/>`,
      `<circle cx="775" cy="1475" r="155" fill="${p.motifAlt}" opacity="0.50"/>`,
      `<path d="${makeWavyPath(640, 1475, 250, 22, 4)}" fill="none" stroke="${p.motif}" stroke-width="18" stroke-linecap="round" opacity="0.72"/>`,
    ].join(''),
    clover: [
      `<path d="${makeFlowerPath(760, 520, 170, 226, 4)}" fill="${p.motif}" opacity="0.62"/>`,
      `<path d="${makeAsymmetricSquircle(48, 1260, 250, 340, 110, 36)}" fill="${p.motifAlt}" opacity="0.52"/>`,
    ].join(''),
    squircleOrb: [
      `<path d="${makeAsymmetricSquircle(565, 365, 285, 345, 124, 42)}" fill="${p.motif}" opacity="0.58"/>`,
      `<path d="${makeFlowerPath(120, 1490, 150, 186, 10)}" fill="${p.motifAlt}" opacity="0.54"/>`,
    ].join(''),
  };
  return presets[kind] || presets.rosette;
}

function backdropSvg(index, kind, p) {
  return svg(`
    <defs>
      <linearGradient id="bg" x1="0.08" y1="0" x2="0.85" y2="1">
        <stop offset="0%" stop-color="${p.top}"/>
        <stop offset="52%" stop-color="${p.mid}"/>
        <stop offset="100%" stop-color="${p.bot}"/>
      </linearGradient>
      <radialGradient id="glow" cx="50%" cy="60%" r="54%">
        <stop offset="0%" stop-color="#FFFFFF" stop-opacity="0.72"/>
        <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0"/>
      </radialGradient>
    </defs>
    <rect width="${width}" height="${height}" fill="url(#bg)"/>
    <rect width="${width}" height="${height}" fill="url(#glow)"/>
    ${motifMarkup(index, kind, p)}
  `);
}

function frameOverlaySvg(p) {
  return svg(`
    <rect x="1.5" y="1.5" width="${width - 3}" height="${height - 3}" rx="${cardRadius - 1.5}" fill="none" stroke="#FFFFFF" stroke-opacity="0.86" stroke-width="2.5"/>
    <rect x="0.75" y="0.75" width="${width - 1.5}" height="${height - 1.5}" rx="${cardRadius - 0.75}" fill="none" stroke="${p.border}" stroke-opacity="0.16" stroke-width="1.5"/>
  `);
}

function phoneGeometry(outerWidth, angle, x, y) {
  const outerHeight = Math.round((outerWidth - 24) * 2993 / 1440) + 24;
  const w = outerWidth + 72;
  const h = outerHeight + 72;
  const radians = (angle * Math.PI) / 180;
  const rotatedWidth = Math.ceil(w / 2 + (w * Math.abs(Math.cos(radians)) + h * Math.abs(Math.sin(radians))) / 2)
    - Math.floor(w / 2 - (w * Math.abs(Math.cos(radians)) + h * Math.abs(Math.sin(radians))) / 2);
  const rotatedHeight = Math.ceil(h / 2 + (h * Math.abs(Math.cos(radians)) + w * Math.abs(Math.sin(radians))) / 2)
    - Math.floor(h / 2 - (h * Math.abs(Math.cos(radians)) + w * Math.abs(Math.sin(radians))) / 2);
  const left = x === 'right' ? 840 - rotatedWidth : x;
  return `<g transform="translate(${left + rotatedWidth / 2} ${y + rotatedHeight / 2}) rotate(${-angle}) translate(${-w / 2} ${-h / 2})"><rect x="36.5" y="36.5" width="${outerWidth - 2}" height="${outerHeight - 2}" rx="33.5" fill="white"/></g>`;
}

function heroShape(index) {
  const paths = [
    phoneGeometry(725, 12.5, 215, -185),
    phoneGeometry(725, -8.5, 195, 415),
    phoneGeometry(720, -8, 105, 395),
    phoneGeometry(720, 8, 'right', 395),
  ];
  return paths[index];
}

function shadowLayer(shape, shadowColor) {
  return svg(`
    <defs>
      <filter id="sWide" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="26"/></filter>
      <filter id="sNear" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="8"/></filter>
    </defs>
    <g transform="translate(0 18)" opacity="0.11" filter="url(#sWide)">${shape.replaceAll('fill="white"', `fill="${shadowColor}"`)}</g>
    <g transform="translate(0 6)" opacity="0.07" filter="url(#sNear)">${shape.replaceAll('fill="white"', `fill="${shadowColor}"`)}</g>
  `);
}

const pillWidthCache = new Map();
async function measureTextWidth(label, size, weight, spacing) {
  const key = `${label}|${size}|${weight}|${spacing}`;
  if (pillWidthCache.has(key)) return pillWidthCache.get(key);
  const probe = svg(
    `<text x="40" y="70" font-family="Rubik, Segoe UI, DejaVu Sans, sans-serif" font-size="${size}" font-weight="${weight}" letter-spacing="${spacing}" fill="#000000">${xml(label)}</text>`,
    900,
    120,
  );
  const { info } = await sharp(probe).trim().toBuffer({ resolveWithObject: true });
  pillWidthCache.set(key, info.width);
  return info.width;
}

const miniFlowerPath = makeFlowerPath(0, 0, 7.2, 9.5, 8);

function pillSvg(x, y, w, h, p, label) {
  const path = makeAsymmetricSquircle(x, y, w, h, 20, 10);
  const iconCx = x + 26;
  const iconCy = y + h / 2;
  const textX = x + 44;
  const textY = y + h / 2 + 6.2;
  return `
    <path d="${path}" fill="${p.pillBg}" stroke="${p.pillStroke}" stroke-opacity="0.22" stroke-width="1.3"/>
    <g transform="translate(${iconCx} ${iconCy})"><path d="${miniFlowerPath}" fill="${p.pillText}"/></g>
    <text x="${textX}" y="${textY}" font-family="Rubik, Segoe UI, DejaVu Sans, sans-serif" font-size="17.5" font-weight="700" letter-spacing="1.6" fill="${p.pillText}">${xml(label)}</text>
  `;
}

async function headerLayer(index, spec, p) {
  const [, , , eyebrow, titleLines, subLines] = spec;
  const labelW = await measureTextWidth(eyebrow, 17.5, 700, 1.6);
  const pillH = 40;
  const pillW = Math.ceil(44 + labelW + 19);

  if (index >= 4) {
    const title = titleLines[0];
    const titleW = await measureTextWidth(title, 60, 700, -1.4);
    let titleSize = 60;
    if (titleW > width - 116) {
      const w56 = await measureTextWidth(title, 56, 700, -1.4);
      titleSize = w56 > width - 104 ? 52 : 56;
    }
    const pillX = Math.round((width - pillW) / 2);
    return svg(`
      ${pillSvg(pillX, 64, pillW, pillH, p, eyebrow)}
      <text x="${width / 2}" y="176" text-anchor="middle" font-family="Rubik, Segoe UI, DejaVu Sans, sans-serif" font-size="${titleSize}" font-weight="700" letter-spacing="-1.4" fill="${p.ink}">${xml(title)}</text>
      ${subLines.map((line, i) => `<text x="${width / 2}" y="${226 + i * 38}" text-anchor="middle" font-family="Rubik, Segoe UI, DejaVu Sans, sans-serif" font-size="29.5" font-weight="400" fill="${p.sub}">${xml(line)}</text>`).join('')}
    `);
  }

  const right = index === 1 || index === 3;
  const margin = 64;
  const top = index === 0 ? 1365 : 64;
  const align = right ? 'end' : 'start';
  const anchorX = right ? width - margin : margin;
  const pillX = right ? width - margin - pillW : margin;
  const titleSize = 74;
  const titleStart = top + 122;
  const subStart = top + 122 + titleLines.length * 88 + 6;
  const subText = index === 0 ? ['No Levyra account.', 'No ads or telemetry.'] : subLines;

  return svg(`
    ${pillSvg(pillX, top, pillW, pillH, p, eyebrow)}
    ${titleLines.map((line, i) => `<text x="${anchorX}" y="${titleStart + i * 88}" text-anchor="${align}" font-family="Rubik, Segoe UI, DejaVu Sans, sans-serif" font-size="${titleSize}" font-weight="700" letter-spacing="-1.8" fill="${p.ink}">${xml(line)}</text>`).join('')}
    ${subText.map((line, i) => `<text x="${anchorX}" y="${subStart + i * 40}" text-anchor="${align}" font-family="Rubik, Segoe UI, DejaVu Sans, sans-serif" font-size="30" font-weight="400" fill="${p.sub}">${xml(line)}</text>`).join('')}
  `);
}

const uprightRawH = Math.round((684 * 2993) / 1440) + 24;
const uprightRawW = 711; // Includes 708px chassis + 3px right hardware keys
const uprightMask = svg(`
  <rect x="0" y="0" width="708" height="${uprightRawH}" rx="34" fill="white"/>
  <rect x="707" y="${Math.round(uprightRawH * 0.21)}" width="4" height="${Math.round(uprightRawH * 0.11)}" rx="1.5" fill="white"/>
  <rect x="707" y="${Math.round(uprightRawH * 0.37)}" width="4" height="${Math.round(uprightRawH * 0.07)}" rx="1.5" fill="white"/>
`, uprightRawW, uprightRawH);

async function renderCard(index, spec) {
  const [fileId, paletteName, motifKind] = spec;
  const p = palettes[paletteName];
  const input = await readFile(resolve(source, `${fileId}.webp`));
  const metadata = await sharp(input).metadata();
  if (metadata.width !== width || metadata.height !== height) throw new Error(`Unexpected source dimensions: ${fileId}`);

  const layers = [];
  if (index < 4) {
    const shape = heroShape(index);
    layers.push({ input: shadowLayer(shape, p.shadow) });
    layers.push({ input: await sharp(input).composite([{ input: svg(shape), blend: 'dest-in' }]).png().toBuffer() });
  } else {
    const phoneW = 650;
    const phoneH = Math.round((uprightRawH * phoneW) / uprightRawW);
    const phoneX = Math.round((width - (708 * phoneW) / uprightRawW) / 2);
    const phoneY = 330;
    const extracted = await sharp(input)
      .extract({ left: 96, top: 264, width: uprightRawW, height: uprightRawH })
      .composite([{ input: uprightMask, blend: 'dest-in' }])
      .png()
      .toBuffer();
    const resizedPhone = await sharp(extracted).resize({ width: phoneW }).png().toBuffer();
    const bodyW = Math.round((708 * phoneW) / uprightRawW);
    layers.push({ input: shadowLayer(`<rect x="${phoneX}" y="${phoneY}" width="${bodyW}" height="${phoneH}" rx="31" fill="white"/>`, p.shadow) });
    layers.push({ input: resizedPhone, left: phoneX, top: phoneY });
  }

  layers.push({ input: await headerLayer(index, spec, p) });
  layers.push({ input: frameOverlaySvg(p) });

  const composed = await sharp(backdropSvg(index, motifKind, p)).composite(layers).png().toBuffer();
  return sharp(composed).composite([{ input: cardMask, blend: 'dest-in' }]).png().toBuffer();
}

await mkdir(output, { recursive: true });
const rendered = [];
for (const [index, spec] of cards.entries()) {
  rendered.push(await renderCard(index, spec));
}

for (let row = 0; row < 6; row++) {
  const set = rendered.slice(row * 4, row * 4 + 4);
  const id = String(row + 1).padStart(2, '0');
  const desktop = await blank(width * 4 + gap * 3, height)
    .composite(set.map((input, col) => ({ input, left: col * (width + gap), top: 0 })))
    .png()
    .toBuffer();
  const mobile = await blank(width * 2 + gap, height * 2 + gap)
    .composite(set.map((input, col) => ({ input, left: (col % 2) * (width + gap), top: Math.floor(col / 2) * (height + gap) })))
    .png()
    .toBuffer();
  await sharp(desktop).resize({ width: 2400 }).webp({ quality: 92, effort: 6 }).toFile(resolve(output, `row-${id}-expressive.webp`));
  await sharp(mobile).resize({ width: 1200 }).webp({ quality: 92, effort: 6 }).toFile(resolve(output, `row-${id}-expressive-mobile.webp`));
  console.log(`Rendered row ${id}: desktop and mobile`);
}
