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
  ['03_daylight_expressive', 'LIGHT THEME', ['A brighter', 'kind of play.'], ['Material 3 Expressive.']],
  ['04_midnight_obsidian', 'DARK THEME', ['Made for', 'after dark.'], ['Dark surfaces. Vivid artwork.']],
  ['05_fresh_currents', 'DISCOVERY', ['Discover more'], ['Search, revisit & explore.']],
  ['06_built_for_your_taste', 'PLAYBACK', ['Keep listening'], ['Gapless playback & crossfade.']],
  ['07_every_genre', 'EXPLORE', ['Moods & genres'], ['Find music for the moment.']],
  ['08_acoustic_precision', 'AUDIO', ['Your sound'], ['Output, quality & audio options.']],
  ['09_keep_it_personal', 'LIBRARY', ['One library'], ['Local files meet streamed music.']],
  ['10_curated_for_you', 'SEARCH', ['Find your next favorite'], ['Songs, artists & albums.']],
  ['11_your_rhythm', 'INSIGHTS', ['Listening insights'], ['Stats calculated on your device.']],
  ['12_in_your_orbit', 'COLLECTIONS', ['Made for you'], ['Mixes from your listening history.']],
  ['13_tailor_every_detail', 'PERSONALIZATION', ['Make it yours'], ['Skins, themes & studios.']],
  ['14_fresh_off_the_stage', 'LIVE RADIO', ['Live radio'], ['Tune in to stations worldwide.']],
  ['15_meet_the_artist', 'ARTISTS', ['Meet the artist'], ['Albums, top tracks & more.']],
  ['16_listen_together', 'JAM', ['Listen together'], ['One queue. The same Wi-Fi.']],
  ['17_sing_along', 'LYRICS', ['Follow every word'], ['Follow along with synced lyrics.']],
  ['18_open_the_album', 'OFFLINE', ['Music to go'], ['Save music for offline listening.']],
  ['19_discover_for_you', 'DISCOVERY', ['Hear it. Keep it.'], ['Preview tracks. Save your finds.']],
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
    phoneGeometry(725, 12.5, 215, -185),
    phoneGeometry(725, 12.5, 215 - 932, -185) + phoneGeometry(725, -8.5, 195, 415),
    phoneGeometry(720, -8, 105, 395),
    phoneGeometry(720, 8, 'right', 395),
  ];
  return paths[index];
}

async function extractPhone(index, spec) {
  const input = await readFile(resolve(source, `${spec[0]}.webp`));
  const metadata = await sharp(input).metadata();
  if (metadata.width !== width || metadata.height !== height) throw new Error(`Unexpected source dimensions: ${spec[0]}`);
  if (index < 4) {
    const shape = index === 1 ? phoneGeometry(725, -8.5, 195, 415) : heroShape(index);
    const cutout = await sharp(input).composite([{ input: svg(shape), blend: 'dest-in' }]).png().toBuffer();
    return sharp(cutout).trim({ threshold: 1 }).png().toBuffer();
  }
  const phoneHeight = Math.round(684 * 2993 / 1440) + 24;
  return sharp(input).extract({ left: 96, top: 264, width: 708, height: phoneHeight })
    .composite([{ input: rounded(708, phoneHeight, 34), blend: 'dest-in' }]).png().toBuffer();
}

const palettes = [
  { top: '#F8FCFF', bottom: '#CBE5F4', ink: '#173B49', muted: '#466672', motif: '#AAD2E8' },
  { top: '#FAF9FF', bottom: '#DDD7F2', ink: '#39344F', muted: '#635D78', motif: '#C5BBE5' },
  { top: '#FAFDFB', bottom: '#CFE7DF', ink: '#22453C', muted: '#4D6A62', motif: '#ADD4C5' },
  { top: '#FFFCF7', bottom: '#EDE0CF', ink: '#493E32', muted: '#756653', motif: '#DFCAAA' },
];

function flower(cx, cy, radius, lobes) {
  const points = Array.from({ length: 144 }, (_, i) => {
    const angle = i * Math.PI * 2 / 144;
    const r = radius * (0.9 + 0.1 * Math.cos(lobes * angle));
    return `${i ? 'L' : 'M'} ${(cx + r * Math.cos(angle)).toFixed(2)} ${(cy + r * Math.sin(angle)).toFixed(2)}`;
  });
  return `${points.join(' ')} Z`;
}

function text(value, x, y, size, fill, weight = 400, anchor = 'middle') {
  return `<text x="${x}" y="${y}" text-anchor="${anchor}" font-family="Rubik, Segoe UI, DejaVu Sans, sans-serif" font-size="${size}" font-weight="${weight}" letter-spacing="${weight >= 600 ? -1.8 : 0}" fill="${fill}">${xml(value)}</text>`;
}

function shadow(shape) {
  return svg(`<defs><filter id="s" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="24"/></filter></defs><g transform="translate(0 18)" opacity=".16" filter="url(#s)">${shape.replaceAll('fill="white"', 'fill="#284457"')}</g>`);
}

function cardBackdrop(index, p) {
  return svg(`
    <defs><linearGradient id="paper" x1="0" y1="0" x2="1" y2="1"><stop stop-color="${p.top}"/><stop offset="1" stop-color="${p.bottom}"/></linearGradient></defs>
    <rect width="900" height="1800" fill="url(#paper)"/>
    <path d="${flower(index === 0 ? 90 : 790, index === 0 ? 520 : 540, 205, index % 2 ? 8 : 12)}" fill="${p.motif}" opacity=".30"/>
    <path d="${flower(85, 1540, 165, 4)}" fill="${p.top}" opacity=".36"/>
  `);
}

async function cardMediaLayers(index, spec) {
  if (index >= 4) {
    const phone = await extractPhone(index, spec);
    const { data, info } = await sharp(phone).resize({ width: 672 }).png().toBuffer({ resolveWithObject: true });
    return [
      { input: shadow(`<rect x="114" y="340" width="672" height="${info.height}" rx="32" fill="white"/>`) },
      { input: data, left: 114, top: 340 }
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
  const p = palettes[[0, 1, 2, 0][index % 4]];
  const backdrop = cardBackdrop(index, p);
  const layers = await cardMediaLayers(index, spec);
  layers.push(await cardTextLayer(index, spec, p));
  layers.push({ input: svg('<rect x="1.5" y="1.5" width="897" height="1797" rx="30" fill="none" stroke="white" stroke-opacity=".65" stroke-width="3"/>') });
  const card = await sharp(backdrop).composite(layers).png().toBuffer();
  return sharp(card).composite([{ input: rounded(width, height, 32), blend: 'dest-in' }]).png().toBuffer();
}

async function cardTextLayer(index, spec, p) {
  const right = index === 1 || index === 3;
  const x = index >= 4 ? 450 : right ? 832 : 68;
  const anchor = index >= 4 ? 'middle' : right ? 'end' : 'start';
  const y = index === 0 ? 1450 : index < 4 ? 151 : 154;
  let titleSize = index < 4 ? 82 : 66;
  if (index >= 4) {
    const probe = await sharp(svg(text(spec[2][0], 450, 100, titleSize, p.ink, 600))).trim().toBuffer({ resolveWithObject: true });
    if (probe.info.width > 780) titleSize = Math.floor(titleSize * 780 / probe.info.width);
  }
  const titles = spec[2].map((line, i) => text(line, x, y + i * 100, titleSize, p.ink, 600, anchor));
  const subtitleY = index === 0 ? 1644 : index < 4 ? 328 : 223;
  const subtitles = spec[3].map((line, i) => text(line, x, subtitleY + i * 42, 32, p.muted, 400, anchor));
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
for (let row = 0; row < 6; row++) {
  const set = rendered.slice(row * 4, row * 4 + 4);
  const id = String(row + 1).padStart(2, '0');
  for (const mobile of [false, true]) {
    const columns = mobile ? 2 : 4;
    const panelWidth = width * columns + gap * (columns - 1);
    const panelHeight = mobile ? height * 2 + gap : height;
    const grid = await sharp({ create: { width: panelWidth, height: panelHeight, channels: 4, background: { r: 0, g: 0, b: 0, alpha: 0 } } })
      .composite(set.map((input, i) => ({ input, left: (i % columns) * (width + gap), top: Math.floor(i / columns) * (height + gap) })))
      .png().toBuffer();
    await sharp(grid).resize({ width: mobile ? 1200 : 2400 }).webp({ quality: 92, effort: 4 })
      .toFile(resolve(output, `row-${id}-studio${mobile ? '-mobile' : ''}.webp`));
  }
  console.log(`Rendered cards ${(row * 4) + 1}-${row * 4 + 4}: desktop and mobile`);
}
