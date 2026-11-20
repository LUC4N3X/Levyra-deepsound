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
const svg = (content, w = width, h = height) => Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}">${content}</svg>`);
const xml = value => value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
const rounded = (w, h, radius) => svg(`<rect width="${w}" height="${h}" rx="${radius}" fill="white"/>`, w, h);

const cards = [
  ['01_pure_listening', 'LISTEN', ['Just press', 'play.'], ['No Levyra account.', 'No ads or telemetry.']],
  ['02_stay_with_the_song', 'NOW PLAYING', ['Stay with', 'the song.'], ['Audio, video & motion artwork.']],
  ['06_built_for_your_taste', 'PLAYBACK', ['Keep listening'], ['Gapless playback & crossfade.']],
  ['06b_live_lyrics', 'LYRICS', ['Live lyrics'], ['Word-by-word sync & translation.']],
  ['05_fresh_currents', 'DISCOVERY', ['Discover more'], ['Search, revisit & explore.']],
  ['10_curated_for_you', 'SEARCH', ['Instant search'], ['Top results, tracks & discography.']],
  ['15_meet_the_artist', 'ARTISTS', ['Meet the artist'], ['Albums, top tracks & more.']],
  ['12_in_your_orbit', 'COLLECTIONS', ['Made for you'], ['Mixes from your listening history.']],
  ['18_open_the_album', 'OFFLINE', ['Music to go'], ['Save music for offline listening.']],
  ['19_discover_for_you', 'DISCOVERY', ['Hear it. Keep it.'], ['Preview tracks. Save your finds.']],
  ['14_fresh_off_the_stage', 'LIVE RADIO', ['Live radio'], ['Tune in to stations worldwide.']],
  ['08_acoustic_precision', 'AUDIO', ['Your sound'], ['Output, quality & audio options.']],
  ['09_keep_it_personal', 'LIBRARY', ['One library'], ['Local files meet streamed music.']],
  ['05b_explore_charts', 'EXPLORE', ['Explore charts'], ['Top 100, city charts & moods.']],
  ['07_every_genre', 'IMPORT', ['Import playlists'], ['Links, files & track review.']],
  ['13_tailor_every_detail', 'PERSONALIZATION', ['Make it yours'], ['Skins, themes & studios.']],
  ['17_sing_along', 'SETTINGS', ['Settings & studios'], ['Customize skins, themes & playback.']],
  ['11_your_rhythm', 'INSIGHTS', ['Listening insights'], ['Stats calculated on your device.']],
  ['16_listen_together', 'JAM', ['Listen together'], ['One queue. The same Wi-Fi.']],
  ['20_your_year_in_music', 'YOUR YEAR', ['Your year in music'], ['Your listening story, on device.']],
  ['21_spotify_style', 'LEVYRA SKINS', ['Spotify Style'], ['Browse genres & new releases.']],
  ['22_apple_music_style', 'LEVYRA SKINS', ['Apple Music Style'], ['Light surfaces & bold type.']],
  ['23_youtube_music_style', 'LEVYRA SKINS', ['YouTube Music Style'], ['Mood chips & music discovery.']],
  ['24_qobuz_style', 'LEVYRA SKINS', ['Qobuz Style'], ['An editorial take on your library.']],
];

function phoneGeometry(outerWidth, angle, x, y) {
  const outerHeight = Math.round((outerWidth - 24) * 2993 / 1440) + 24;
  const w = outerWidth + 72;
  const h = outerHeight + 72;
  const radians = angle * Math.PI / 180;
  const rotatedWidth = Math.ceil(w / 2 + (w * Math.abs(Math.cos(radians)) + h * Math.abs(Math.sin(radians))) / 2)
    - Math.floor(w / 2 - (w * Math.abs(Math.cos(radians)) + h * Math.abs(Math.sin(radians))) / 2);
  const rotatedHeight = Math.ceil(h / 2 + (h * Math.abs(Math.cos(radians)) + w * Math.abs(Math.sin(radians))) / 2)
    - Math.floor(h / 2 - (h * Math.abs(Math.cos(radians)) + w * Math.abs(Math.sin(radians))) / 2);
  const left = x === 'right' ? 840 - rotatedWidth : x;
  return `<g transform="translate(${left + rotatedWidth / 2} ${y + rotatedHeight / 2}) rotate(${-angle}) translate(${-w / 2} ${-h / 2})"><rect x="37" y="37" width="${outerWidth - 3}" height="${outerHeight - 3}" rx="33" fill="white"/></g>`;
}

function heroShape(index) {
  const paths = [
    phoneGeometry(710, 20.0, 175, -155),
    phoneGeometry(710, 20.0, 175 - 932, -155) + phoneGeometry(710, -11.0, 185, 420),
  ];
  return paths[index];
}

async function extractPhone(index, spec) {
  const input = await readFile(resolve(source, `${spec[0]}.webp`));
  const metadata = await sharp(input).metadata();
  if (metadata.width !== width || metadata.height !== height) throw new Error(`Unexpected source dimensions: ${spec[0]}`);
  if (index < 2) {
    const shape = index === 1 ? phoneGeometry(710, -11.0, 185, 420) : heroShape(index);
    const cutout = await sharp(input).composite([{ input: svg(shape), blend: 'dest-in' }]).png().toBuffer();
    return sharp(cutout).trim({ threshold: 1 }).png().toBuffer();
  }
  const phoneHeight = Math.round(684 * 2993 / 1440) + 24;
  return sharp(input).extract({ left: 96, top: 264, width: 708, height: phoneHeight })
    .composite([{ input: rounded(708, phoneHeight, 34), blend: 'dest-in' }]).png().toBuffer();
}

const palettes = [
  { top: '#EFF3FD', mid: '#D4DFF8', bottom: '#9EB6EC', shape: '#7F9EE4', ink: '#12182B', muted: '#3B486B' },
  { top: '#F1F2FD', mid: '#D8DCF8', bottom: '#A7B2EC', shape: '#8B99E4', ink: '#14172E', muted: '#40476B' },
  { top: '#EDF4FC', mid: '#CFE2F6', bottom: '#95BCE8', shape: '#74A5DF', ink: '#101C2C', muted: '#374D68' },
  { top: '#EFF2FD', mid: '#D5DDF8', bottom: '#A2B4EC', shape: '#849AE4', ink: '#13182C', muted: '#3D486B' },
];

function flower(cx, cy, radius, lobes, amp = 0.1) {
  const points = Array.from({ length: 144 }, (_, i) => {
    const angle = i * Math.PI * 2 / 144;
    const r = radius * (1 - amp + amp * Math.cos(lobes * angle));
    return `${i ? 'L' : 'M'} ${(cx + r * Math.cos(angle)).toFixed(2)} ${(cy + r * Math.sin(angle)).toFixed(2)}`;
  });
  return `${points.join(' ')} Z`;
}

function text(value, x, y, size, fill, weight = 400, anchor = 'middle', letterSpacing = null) {
  const ls = letterSpacing !== null ? letterSpacing : (weight >= 600 ? -1.8 : 0);
  return `<text x="${x}" y="${y}" text-anchor="${anchor}" font-family="Rubik, Segoe UI, DejaVu Sans, sans-serif" font-size="${size}" font-weight="${weight}" letter-spacing="${ls}" fill="${fill}">${xml(value)}</text>`;
}

function shadow(shape) {
  return svg(`
    <defs>
      <filter id="s1" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="44"/></filter>
      <filter id="s2" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="20"/></filter>
      <filter id="s3" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="8"/></filter>
    </defs>
    <g transform="translate(0 36)" opacity=".34" filter="url(#s1)">${shape.replaceAll('fill="white"', 'fill="#0E1733"')}</g>
    <g transform="translate(0 20)" opacity=".32" filter="url(#s2)">${shape.replaceAll('fill="white"', 'fill="#0B1228"')}</g>
    <g transform="translate(0 8)" opacity=".28" filter="url(#s3)">${shape.replaceAll('fill="white"', 'fill="#060A18"')}</g>
  `);
}

function cardBackdrop(index, p) {
  const isHeroLeft = index === 0;
  const isHeroRight = index === 1;
  const gradAttrs = isHeroLeft
    ? 'x1="0" y1="1" x2="1" y2="0"'
    : isHeroRight
      ? 'x1="0" y1="0" x2="1" y2="1"'
      : (index % 2 === 0 ? 'x1="0" y1="0" x2="1" y2="1"' : 'x1="1" y1="0" x2="0" y2="1"');
  const mainShape = isHeroLeft
    ? `<path d="${flower(105, 560, 240, 10, 0.11)}" fill="${p.shape}" opacity=".26"/>
       <path d="${flower(135, 1170, 150, 4, 0.16)}" fill="#FFFFFF" opacity=".42"/>`
    : isHeroRight
      ? `<path d="${flower(785, 430, 235, 8, 0.12)}" fill="${p.shape}" opacity=".24"/>
         <path d="${flower(125, 1520, 165, 4, 0.16)}" fill="#FFFFFF" opacity=".38"/>`
      : index % 2 === 0
        ? `<path d="${flower(805, 340, 225, 12, 0.09)}" fill="${p.shape}" opacity=".24"/>
           <path d="${flower(85, 1180, 175, 4, 0.16)}" fill="#FFFFFF" opacity=".36"/>`
        : `<path d="${flower(95, 350, 220, 8, 0.12)}" fill="${p.shape}" opacity=".24"/>
           <path d="${flower(820, 1200, 180, 4, 0.16)}" fill="#FFFFFF" opacity=".36"/>`;
  return svg(`
    <defs>
      <linearGradient id="paper" ${gradAttrs}>
        <stop offset="0%" stop-color="${p.top}"/>
        <stop offset="50%" stop-color="${p.mid}"/>
        <stop offset="100%" stop-color="${p.bottom}"/>
      </linearGradient>
    </defs>
    <rect width="900" height="1800" fill="url(#paper)"/>
    ${mainShape}
  `);
}

async function cardMediaLayers(index, spec) {
  if (index >= 2) {
    const phone = await extractPhone(index, spec);
    const { data, info } = await sharp(phone).resize({ width: 744 }).png().toBuffer({ resolveWithObject: true });
    return [
      { input: shadow(`<rect x="78" y="274" width="744" height="${info.height}" rx="36" fill="white"/>`) },
      { input: data, left: 78, top: 274 }
    ];
  }
  const input = await readFile(resolve(source, `${spec[0]}.webp`));
  const metadata = await sharp(input).metadata();
  if (metadata.width !== width || metadata.height !== height) throw new Error(`Unexpected source dimensions: ${spec[0]}`);
  const shape = heroShape(index);
  return [
    { input: shadow(shape) },
    { input: await sharp(input).composite([{ input: svg(shape), blend: 'dest-in' }]).png().toBuffer() }
  ];
}

async function renderCard(index, spec) {
  const p = palettes[index < 2 ? 0 : index % 4];
  const backdrop = cardBackdrop(index, p);
  const layers = await cardMediaLayers(index, spec);
  layers.push(await cardTextLayer(index, spec, p));
  layers.push({ input: svg('<rect x="1.5" y="1.5" width="897" height="1797" rx="34" fill="none" stroke="white" stroke-opacity=".70" stroke-width="3"/>') });
  const card = await sharp(backdrop).composite(layers).png().toBuffer();
  return sharp(card).composite([{ input: rounded(width, height, 36), blend: 'dest-in' }]).png().toBuffer();
}

async function cardTextLayer(index, spec, p) {
  const right = index === 1;
  const x = index >= 2 ? 450 : right ? 832 : 68;
  const anchor = index >= 2 ? 'middle' : right ? 'end' : 'start';
  const y = index === 0 ? 1458 : index < 2 ? 156 : 124;
  let titleSize = index < 2 ? 82 : 64;
  if (index >= 2) {
    const probe = await sharp(svg(text(spec[2][0], 450, 100, titleSize, p.ink, 700))).trim().toBuffer({ resolveWithObject: true });
    if (probe.info.width > 780) titleSize = Math.floor(titleSize * 780 / probe.info.width);
  }
  const titles = spec[2].map((line, i) => text(line, x, y + i * 96, titleSize, p.ink, 700, anchor));
  const subtitleY = index === 0 ? 1646 : index < 2 ? 328 : 186;
  const subtitles = spec[3].map((line, i) => text(line, x, subtitleY + i * 42, 30, p.muted, 400, anchor));
  return { input: svg(titles.join('') + subtitles.join('')) };
}

await mkdir(output, { recursive: true });
const rendered = [];
for (const [index, spec] of cards.entries()) {
  const card = await renderCard(index, spec);
  rendered.push(card);
  await sharp(card).webp({ quality: 92, effort: 4 }).toFile(resolve(output, `card-${String(index + 1).padStart(2, '0')}-studio.webp`));
}
const gap = 24;
const rowSlices = [
  [0, 4],
  [4, 8],
  [8, 12],
  [12, 16],
  [16, 20],
  [20, 24],
];
for (const [row, [start, end]] of rowSlices.entries()) {
  const set = rendered.slice(start, end);
  const id = String(row + 1).padStart(2, '0');
  for (const mobile of [false, true]) {
    const columns = mobile ? Math.min(2, set.length) : set.length;
    const rowsCount = Math.ceil(set.length / columns);
    const panelWidth = width * columns + gap * (columns - 1);
    const panelHeight = height * rowsCount + gap * (rowsCount - 1);
    const grid = await sharp({ create: { width: panelWidth, height: panelHeight, channels: 4, background: { r: 0, g: 0, b: 0, alpha: 0 } } })
      .composite(set.map((input, i) => ({ input, left: (i % columns) * (width + gap), top: Math.floor(i / columns) * (height + gap) })))
      .png().toBuffer();
    const outWidth = mobile ? 1200 : Math.round(2400 * columns / 4);
    await sharp(grid).resize({ width: outWidth }).webp({ quality: 92, effort: 4 })
      .toFile(resolve(output, `row-${id}-studio${mobile ? '-mobile' : ''}.webp`));
  }
  console.log(`Rendered cards ${start + 1}-${end}: desktop and mobile`);
}
