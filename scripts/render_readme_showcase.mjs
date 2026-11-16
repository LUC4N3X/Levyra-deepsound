import { mkdir, readFile, writeFile } from 'node:fs/promises';
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

const chapters = [
  ['THE LEVYRA EXPERIENCE', ['A home for', 'your music.'], 'No Levyra account. No ads. No telemetry.', ['Your home', 'Now playing', 'In the light', 'After dark']],
  ['DISCOVER & LISTEN', ['Find your', 'next repeat.'], 'Search, explore and settle into the sound.', ['Discover', 'Keep listening', 'Moods & genres', 'Your sound']],
  ['YOUR COLLECTION', ['Everything,', 'in your orbit.'], 'Local files, streamed tracks and listening insights.', ['One library', 'Find a favorite', 'Your listening', 'Made for you']],
  ['MAKE IT PERSONAL', ['Music, on', 'your terms.'], 'Choose your look. Find your people. Press play.', ['Skins & themes', 'Live radio', 'Artist pages', 'Listen together']],
  ['KEEP IT CLOSE', ['Take the music', 'with you.'], 'Synced lyrics, offline albums and your listening story.', ['Every word', 'Offline albums', 'Swipe & discover', 'Your year']],
  ['LEVYRA SKINS', ['Same music.', 'Your style.'], 'Four more ways to make Levyra feel like home.', ['Spotify Style', 'Apple Music Style', 'YouTube Music Style', 'Qobuz Style']],
];

function text(value, x, y, size, fill, weight = 400, spacing = 0) {
  return `<text x="${x}" y="${y}" font-family="Rubik, Segoe UI, DejaVu Sans, sans-serif" font-size="${size}" font-weight="${weight}" letter-spacing="${spacing}" fill="${fill}">${xml(value)}</text>`;
}

async function renderPanel(row, phones, mobile) {
  const w = mobile ? 1000 : 2000;
  const h = mobile ? 2590 : 1580;
  const margin = mobile ? 64 : 88;
  const [eyebrow, headline, subtitle, labels] = chapters[row];
  const light = row === 2 || row === 5;
  const palette = light
    ? { surface: '#F1F0EB', ink: '#182225', muted: '#586264', accent: '#366C65', line: '#D4D8D2', well: '#E4E6E0' }
    : { surface: '#141B1E', ink: '#F5F6F1', muted: '#B0BDBE', accent: '#AADDD0', line: '#354346', well: '#1D282C' };
  const header = [
    `<rect width="${w}" height="${h}" fill="${palette.surface}"/>`,
    text(eyebrow, margin, 100, mobile ? 28 : 27, palette.accent, 500, 4),
    ...headline.map((line, i) => text(line, margin, 225 + i * 112, mobile ? 100 : 112, palette.ink, 700, -4)),
    `<path d="M ${margin} 440 H ${w - margin}" stroke="${palette.line}" stroke-width="2"/>`,
    text('LEVYRA / ANDROID', margin, h - 62, 24, palette.muted, 500, 3),
    text(`${String(row + 1).padStart(2, '0')} / 06`, w - margin - 112, h - 62, 24, palette.muted, 500, 2),
  ];
  {
    const words = subtitle.split(' ');
    const lines = [''];
    for (const word of words) {
      if ((lines.at(-1) + word).length > 43) lines.push('');
      lines[lines.length - 1] += `${word} `;
    }
    lines.forEach((line, i) => header.push(text(line.trim(), mobile ? margin : 1130, (mobile ? 387 : 281) + i * 36, 29, palette.muted)));
  }
  const layers = [];
  const columns = mobile ? 2 : 4;
  const slotWidth = (w - margin * 2) / columns;
  for (let i = 0; i < 4; i++) {
    const x = Math.round(margin + (i % columns) * slotWidth);
    const y = 505 + Math.floor(i / columns) * 975;
    const phoneW = Math.floor(slotWidth - 58);
    const phoneH = mobile ? 810 : 830;
    header.push(`<rect x="${x + 8}" y="${y - 24}" width="${slotWidth - 28}" height="${phoneH + 150}" rx="28" fill="${palette.well}" stroke="${palette.line}" stroke-width="1.5"/>`);
    header.push(text(labels[i], x + 30, y + 28, mobile ? 29 : 30, palette.ink, 500));
    const { data, info } = await sharp(phones[i]).resize(phoneW, phoneH, { fit: 'inside' }).png().toBuffer({ resolveWithObject: true });
    layers.push({ input: data, left: x + Math.round((slotWidth - 12 - info.width) / 2), top: y + 78 + Math.round((phoneH - info.height) / 2) });
  }
  const panel = await sharp(svg(header.join(''), w, h)).composite(layers).png().toBuffer();
  return sharp(panel).composite([{ input: rounded(w, h, 36), blend: 'dest-in' }]).webp({ quality: 90, effort: 6 }).toBuffer();
}

await mkdir(output, { recursive: true });
for (let row = 0; row < chapters.length; row++) {
  const phones = [];
  for (let i = 0; i < 4; i++) phones.push(await extractPhone(row * 4 + i, cards[row * 4 + i]));
  const id = String(row + 1).padStart(2, '0');
  for (const mobile of [false, true]) {
    const panel = await renderPanel(row, phones, mobile);
    await writeFile(resolve(output, `row-${id}-studio${mobile ? '-mobile' : ''}.webp`), panel);
  }
  console.log(`Rendered chapter ${id}: desktop and mobile`);
}
