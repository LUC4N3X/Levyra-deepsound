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
const gap = 40;
const clear = { r: 0, g: 0, b: 0, alpha: 0 };
const svg = (content, w = width, h = height) => Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}">${content}</svg>`);
const xml = value => value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
const blank = (w, h) => sharp({ create: { width: w, height: h, channels: 4, background: clear } });
const rounded = (w, h, radius) => svg(`<rect width="${w}" height="${h}" rx="${radius}" fill="white"/>`, w, h);

const cards = [
  ['01_pure_listening', 'LISTEN', ['Just press', 'play.'], ['No Levyra account.', 'No ads or telemetry.']],
  ['02_stay_with_the_song', 'NOW PLAYING', ['Stay with', 'the song.'], ['Audio, video & motion artwork.']],
  ['03_daylight_expressive', 'LIGHT THEME', ['A brighter', 'kind of play.'], ['Material 3 Expressive.']],
  ['04_midnight_obsidian', 'DARK THEME', ['Made for', 'after dark.'], ['Dark surfaces. Vivid artwork.']],
  ['05_fresh_currents', 'DISCOVERY', ['Start with', 'curiosity.'], ['Search, revisit & explore.']],
  ['06_built_for_your_taste', 'PLAYBACK', ['Keep the', 'music going.'], ['Gapless playback & crossfade.']],
  ['07_every_genre', 'EXPLORE', ['Follow', 'your mood.'], ['Genres, charts & new finds.']],
  ['08_acoustic_precision', 'AUDIO', ['Find your', 'sound.'], ['Output, quality & audio options.']],
  ['09_keep_it_personal', 'LIBRARY', ['All yours.', 'All together.'], ['Local files meet streamed music.']],
  ['10_curated_for_you', 'SEARCH', ['Your next', 'great find.'], ['Songs, artists & albums.']],
  ['11_your_rhythm', 'INSIGHTS', ['See your', 'listening.'], ['Stats calculated on your device.']],
  ['12_in_your_orbit', 'COLLECTIONS', ['Back to', 'your favorites.'], ['Mixes from your listening history.']],
  ['13_tailor_every_detail', 'PERSONALIZATION', ['Make it', 'feel like you.'], ['Skins, themes & studios.']],
  ['14_fresh_off_the_stage', 'LIVE RADIO', ['Somewhere,', 'on air.'], ['Tune in to stations worldwide.']],
  ['15_meet_the_artist', 'ARTISTS', ['Go beyond', 'the single.'], ['Albums, top tracks & more.']],
  ['16_listen_together', 'JAM', ['Good music.', 'Good company.'], ['One queue. The same Wi-Fi.']],
  ['17_sing_along', 'LYRICS', ['Every line.', 'Right on time.'], ['Follow along with synced lyrics.']],
  ['18_open_the_album', 'OFFLINE', ['Take it', 'with you.'], ['Save music for offline listening.']],
  ['19_discover_for_you', 'DISCOVERY', ['Hear it.', 'Keep it.'], ['Preview tracks. Save your finds.']],
  ['20_your_year_in_music', 'YOUR YEAR', ['A year.', 'In music.'], ['Your listening story, on device.']],
  ['21_spotify_style', 'LEVYRA SKINS', ['Spotify', 'Style.'], ['Browse genres & new releases.']],
  ['22_apple_music_style', 'LEVYRA SKINS', ['Apple Music', 'Style.'], ['Light surfaces & bold type.']],
  ['23_youtube_music_style', 'LEVYRA SKINS', ['YouTube Music', 'Style.'], ['Mood chips & music discovery.']],
  ['24_qobuz_style', 'LEVYRA SKINS', ['Qobuz', 'Style.'], ['An editorial take on your library.']],
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

function shadow(shape) {
  return svg(`<defs><filter id="shadow" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="20"/></filter></defs><g transform="translate(0 18)" opacity=".16" filter="url(#shadow)">${shape.replaceAll('fill="white"', 'fill="#244b62"')}</g>`);
}

function type(index, spec) {
  const ink = '#285363';
  const accent = '#4a7786';
  const muted = '#4f707b';
  const y = index === 0 ? 1390 : 76;
  const right = index === 1 || index === 3;
  const x = index >= 4 ? width / 2 : right ? 836 : 64;
  const align = index >= 4 ? 'middle' : right ? 'end' : 'start';
  const titleSize = index < 4 ? 86 : 76;
  const label = `${String(index + 1).padStart(2, '0')} / ${spec[1]}`;
  const text = (value, top, size, fill, weight = 400, spacing = 0) => `<text x="${x}" y="${top}" text-anchor="${align}" font-family="Rubik, DejaVu Sans, sans-serif" font-size="${size}" font-weight="${weight}" letter-spacing="${spacing}" fill="${fill}">${xml(value)}</text>`;
  return svg([
    text(label, y + 24, 22, accent, 500, 1.8),
    ...spec[2].map((line, i) => text(line, y + 136 + i * 101, titleSize, ink, 600, -2)),
    ...spec[3].map((line, i) => text(line, y + 300 + i * 42, 32, muted)),
  ].join(''));
}

async function renderCard(index, spec, backdrop) {
  const input = await readFile(resolve(source, `${spec[0]}.webp`));
  const metadata = await sharp(input).metadata();
  if (metadata.width !== width || metadata.height !== height) throw new Error(`Unexpected source dimensions: ${spec[0]}`);
  const layers = [];
  if (index < 4) {
    const shape = heroShape(index);
    layers.push({ input: shadow(shape) });
    layers.push({ input: await sharp(input).composite([{ input: svg(shape), blend: 'dest-in' }]).png().toBuffer() });
  } else {
    const phoneHeight = Math.round(684 * 2993 / 1440) + 24;
    const phone = await sharp(input).extract({ left: 96, top: 264, width: 708, height: phoneHeight })
      .composite([{ input: rounded(708, phoneHeight, 34), blend: 'dest-in' }]).png().toBuffer();
    layers.push({ input: shadow(`<rect x="120" y="408" width="660" height="${Math.round(phoneHeight * 660 / 708)}" rx="32" fill="white"/>`) });
    layers.push({ input: await sharp(phone).resize({ width: 660 }).png().toBuffer(), left: 120, top: 408 });
  }
  layers.push({ input: type(index, spec) });
  layers.push({ input: svg('<rect x="1" y="1" width="898" height="1798" rx="38" fill="none" stroke="#b0cdda" stroke-opacity=".25" stroke-width="2"/>') });
  const background = await sharp(backdrop).composite(layers).png().toBuffer();
  return sharp(background).composite([{ input: rounded(width, height, 38), blend: 'dest-in' }]).png().toBuffer();
}

await mkdir(output, { recursive: true });
const backdrop = await sharp(resolve(root, 'docs/assets/showcase/readme/studio-backdrop.webp'))
  .resize(width, height, { fit: 'cover' }).png().toBuffer();
const rendered = [];
for (const [index, spec] of cards.entries()) rendered.push(await renderCard(index, spec, backdrop));
for (let row = 0; row < 6; row++) {
  const set = rendered.slice(row * 4, row * 4 + 4);
  const id = String(row + 1).padStart(2, '0');
  const desktop = await blank(width * 4 + gap * 3, height)
    .composite(set.map((input, col) => ({ input, left: col * (width + gap), top: 0 })))
    .png().toBuffer();
  const mobile = await blank(width * 2 + gap, height * 2 + gap)
    .composite(set.map((input, col) => ({ input, left: (col % 2) * (width + gap), top: Math.floor(col / 2) * (height + gap) })))
    .png().toBuffer();
  await sharp(desktop).resize({ width: 2400 }).webp({ quality: 90, effort: 6 }).toFile(resolve(output, `row-${id}.webp`));
  await sharp(mobile).resize({ width: 1200 }).webp({ quality: 90, effort: 6 }).toFile(resolve(output, `row-${id}-mobile.webp`));
  console.log(`Rendered row ${id}: desktop and mobile`);
}
