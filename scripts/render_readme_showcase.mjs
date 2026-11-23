import { existsSync } from 'node:fs';
import { mkdir, readFile } from 'node:fs/promises';
import { createRequire } from 'node:module';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { parseArgs } from 'node:util';

const { values } = parseArgs({ options: {
  'sharp-module': { type: 'string', default: 'sharp' },
  'font-dir': { type: 'string', default: '.levyra-tools/readme-showcase/fonts' },
  'hero-screen': { type: 'string' },
  output: { type: 'string' },
} });
const require = createRequire(import.meta.url);
const sharp = require(values['sharp-module'] === 'sharp' ? 'sharp' : resolve(values['sharp-module']));
const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const source = resolve(root, 'docs/assets/showcase/editorial-cards');
const heroScreenPath = values['hero-screen'] ? resolve(values['hero-screen']) : null;
const fontDir = resolve(root, values['font-dir']);
const outfitTitleFont = resolve(fontDir, 'Outfit-500.ttf');
const outfitBodyFont = resolve(fontDir, 'Outfit-400.ttf');
const hasOutfitFiles = existsSync(outfitTitleFont) && existsSync(outfitBodyFont);
const output = values.output ? resolve(values.output) : resolve(root, 'docs/assets/showcase/readme');
const width = 900;
const height = 1800;
const svg = (content, w = width, h = height) => Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}">${content}</svg>`);
const xml = value => value.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
const rounded = (w, h, radius) => svg(`<rect width="${w}" height="${h}" rx="${radius}" fill="white"/>`, w, h);

const cards = [
  ['01_pure_listening', 'LISTEN', ['Just the', 'music.'], ['No ads. No tracking.']],
  ['02_stay_with_the_song', 'HOME', ['Built from', 'your plays.'], ['Your home updates on device.']],
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
  ['22_apple_music_style', 'UPDATES', ['In-app updates'], ['Changelogs & one-tap updates.']],
  ['23_youtube_music_style', 'BROWSE', ['Moods & genres'], ['Browse by vibe, decade & style.']],
  ['24_qobuz_style', 'PLAYER DECK', ['Player Deck'], ['Player styles & live backgrounds.']],
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
  const screenHeight = Math.round(684 * 2993 / 1440);
  return sharp(input).extract({ left: 108, top: 276, width: 684, height: screenHeight }).png().toBuffer();
}

function smartphoneFrameSvg(left, top, outerW, outerH, cw = width, ch = height) {
  return svg(`
    <defs>
      <linearGradient id="pxRail" x1="0" y1="0" x2="1" y2="0.55">
        <stop offset="0%" stop-color="#8A92A2"/>
        <stop offset="18%" stop-color="#484E5C"/>
        <stop offset="52%" stop-color="#2B303B"/>
        <stop offset="84%" stop-color="#5D6474"/>
        <stop offset="100%" stop-color="#848C9C"/>
      </linearGradient>
    </defs>
    <rect x="${left}" y="${top}" width="${outerW}" height="${outerH}" rx="70" fill="url(#pxRail)"/>
    <rect x="${left + 1.5}" y="${top + 1.5}" width="${outerW - 3}" height="${outerH - 3}" rx="68.5" fill="none" stroke="#E2E8F5" stroke-opacity=".55" stroke-width="1.4"/>
    <rect x="${left + 5.5}" y="${top + 5.5}" width="${outerW - 11}" height="${outerH - 11}" rx="64.5" fill="#0A0C10"/>
  `, cw, ch);
}

function smartphoneDetailsSvg(left, top, outerW, bezel, screenH = 0, cw = width, ch = height) {
  const cx = left + outerW / 2;
  const camY = top + bezel + 21;
  const glint = screenH > 0
    ? `<defs><linearGradient id="pxGlint" x1="0" y1="0" x2="0.85" y2="0.65"><stop offset="0%" stop-color="#FFFFFF" stop-opacity=".08"/><stop offset="32%" stop-color="#FFFFFF" stop-opacity=".02"/><stop offset="55%" stop-color="#FFFFFF" stop-opacity="0"/></linearGradient></defs><rect x="${left + bezel}" y="${top + bezel}" width="${outerW - bezel * 2}" height="${screenH}" rx="56" fill="url(#pxGlint)"/>`
    : '';
  return svg(`
    ${glint}
    <rect x="${cx - 32}" y="${top + 6.5}" width="64" height="3" rx="1.5" fill="#2A2F3A" opacity=".85"/>
    <circle cx="${cx}" cy="${camY}" r="7.5" fill="#06080C" stroke="#222836" stroke-width="1"/>
    <circle cx="${cx}" cy="${camY}" r="3.6" fill="#121A2B"/>
    <circle cx="${cx - 1.5}" cy="${camY - 1.5}" r="1.2" fill="#9BB6EB" opacity=".68"/>
  `, cw, ch);
}

const brandColors = { ink: '#161F33', muted: '#4D5C7A' };
const shapeColors = ['#7693C9', '#6C84B5'];

const BACKDROP_DEFS = `
  <defs>
    <linearGradient id="baseSky" x1="0" y1="0.12" x2="1" y2="0.88">
      <stop offset="0%" stop-color="#FFFFFF"/>
      <stop offset="22%" stop-color="#FFFFFF"/>
      <stop offset="54%" stop-color="#EBF2FF"/>
      <stop offset="80%" stop-color="#CFE0FF"/>
      <stop offset="100%" stop-color="#A6C5F8"/>
    </linearGradient>
    <linearGradient id="silkBlue" x1="0" y1="0" x2="1" y2="0.25">
      <stop offset="0%" stop-color="#88B0F2"/>
      <stop offset="36%" stop-color="#6C89BE"/>
      <stop offset="68%" stop-color="#9AC0FA"/>
      <stop offset="100%" stop-color="#5E7BB2"/>
    </linearGradient>
    <linearGradient id="silkDeep" x1="0" y1="0.2" x2="1" y2="0.9">
      <stop offset="0%" stop-color="#546D9E"/>
      <stop offset="50%" stop-color="#6C84B5"/>
      <stop offset="100%" stop-color="#4A6494"/>
    </linearGradient>
    <radialGradient id="orbA" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="#8EB6F6" stop-opacity=".40"/>
      <stop offset="55%" stop-color="#B6D0FA" stop-opacity=".16"/>
      <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0"/>
    </radialGradient>
    <radialGradient id="orbB" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="#6C84B5" stop-opacity=".28"/>
      <stop offset="55%" stop-color="#9BB6E6" stop-opacity=".12"/>
      <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0"/>
    </radialGradient>
    <radialGradient id="orbC" cx="50%" cy="50%" r="50%">
      <stop offset="0%" stop-color="#B8D1FF" stop-opacity=".44"/>
      <stop offset="60%" stop-color="#D5E3FF" stop-opacity=".16"/>
      <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0"/>
    </radialGradient>
    <linearGradient id="topVeil" x1="0" y1="0" x2="0.4" y2="0.6">
      <stop offset="0%" stop-color="#FFFFFF" stop-opacity=".62"/>
      <stop offset="45%" stop-color="#FFFFFF" stop-opacity=".18"/>
      <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0"/>
    </linearGradient>
    <filter id="glassBlur" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="58"/></filter>
    <filter id="softRim" x="-10%" y="-10%" width="120%" height="120%"><feGaussianBlur stdDeviation="8"/></filter>
  </defs>
`;

function flower(cx, cy, radius, lobes, amp = 0.1) {
  const points = Array.from({ length: 144 }, (_, i) => {
    const angle = i * Math.PI * 2 / 144;
    const r = radius * (1 - amp + amp * Math.cos(lobes * angle));
    return `${i ? 'L' : 'M'} ${(cx + r * Math.cos(angle)).toFixed(2)} ${(cy + r * Math.sin(angle)).toFixed(2)}`;
  });
  return `${points.join(' ')} Z`;
}

function text(value, x, y, size, fill, weight = 400, anchor = 'middle') {
  const ls = size >= 50 ? -0.8 : 0;
  return `<text x="${x}" y="${y}" text-anchor="${anchor}" font-family="Outfit, Segoe UI, Inter, DejaVu Sans, sans-serif" font-size="${size}" font-weight="${weight}" letter-spacing="${ls}" fill="${fill}">${xml(value)}</text>`;
}

async function renderOutfitLine(value, sizePt, fill, fontfile, fontName, letterSpacing = 0) {
  const lsAttr = letterSpacing ? ` letter_spacing="${Math.round(letterSpacing * 1024)}"` : '';
  return sharp({
    text: {
      text: `<span foreground="${fill}"${lsAttr}>${xml(value)}</span>`,
      font: `${fontName} ${sizePt}`,
      fontfile,
      rgba: true,
      dpi: 96,
    },
  }).png().toBuffer({ resolveWithObject: true });
}

function shadow(shape) {
  return svg(`
    <defs>
      <filter id="s1" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="46"/></filter>
      <filter id="s2" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="18"/></filter>
      <filter id="s3" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="6"/></filter>
    </defs>
    <g transform="translate(0 28)" opacity=".20" filter="url(#s1)">${shape.replaceAll('fill="white"', 'fill="#182A5E"')}</g>
    <g transform="translate(0 14)" opacity=".18" filter="url(#s2)">${shape.replaceAll('fill="white"', 'fill="#111E42"')}</g>
    <g transform="translate(0 4)" opacity=".14" filter="url(#s3)">${shape.replaceAll('fill="white"', 'fill="#0A122B"')}</g>
  `);
}

function cardBackdrop(index, shapeColor) {
  const row = Math.floor(index / 4);
  const col = index % 4;
  const xOff = -(col * 910);
  const flip = row % 2 === 1;
  const waveY = flip ? 1140 : 1260;
  const amp = flip ? -220 : 220;
  const glassEdge1 = `M -200 ${waveY} C 550 ${waveY - amp}, 1150 ${waveY + amp * 1.15}, 1850 ${waveY - amp * 0.45} C 2550 ${waveY - amp * 1.4}, 3150 ${waveY + amp * 0.85}, 3900 ${waveY - 120}`;
  const glassEdge2 = `M -200 ${waveY + 260} C 650 ${waveY + 260 + amp * 0.8}, 1350 ${waveY + 120 - amp * 0.9}, 2150 ${waveY + 280 + amp * 0.6} C 2850 ${waveY + 380 - amp * 0.7}, 3350 ${waveY + 140}, 3900 ${waveY + 220}`;
  const [orbX1, orbX2, orbX3] = flip ? [2850, 1250, 2050] : [780, 2650, 1780];
  const shapePath = col % 2 === 0 ? flower(800, 380, 195, 10, 0.10) : flower(105, 420, 190, 8, 0.11);
  return svg(`
    ${BACKDROP_DEFS}
    <rect width="900" height="1800" fill="url(#baseSky)"/>
    <g transform="translate(${xOff} 0)">
      <ellipse cx="${orbX1}" cy="1360" rx="920" ry="620" fill="url(#orbA)"/>
      <ellipse cx="${orbX2}" cy="1180" rx="860" ry="640" fill="url(#orbB)"/>
      <ellipse cx="${orbX3}" cy="1540" rx="820" ry="520" fill="url(#orbC)"/>
      <path d="${glassEdge1} L 3900 2050 L -200 2050 Z" fill="url(#silkBlue)" opacity=".32" filter="url(#glassBlur)"/>
      <path d="${glassEdge2} L 3900 2050 L -200 2050 Z" fill="url(#silkDeep)" opacity=".26" filter="url(#glassBlur)"/>
      <path d="${glassEdge1}" fill="none" stroke="#FFFFFF" stroke-width="5" stroke-opacity=".68" filter="url(#softRim)"/>
      <path d="${glassEdge1}" fill="none" stroke="#FFFFFF" stroke-width="2" stroke-opacity=".55"/>
      <path d="${glassEdge2}" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-opacity=".42" filter="url(#softRim)"/>
    </g>
    <rect width="900" height="1800" fill="url(#topVeil)"/>
    <path d="${shapePath}" fill="${shapeColor}" opacity=".055"/>
  `);
}

let cachedHeroPair = null;

async function buildHeroPairLayer() {
  if (cachedHeroPair) return cachedHeroPair;
  const bezel = 15;
  const screenW = 780;
  const { data, info } = await sharp(heroScreenPath).resize({ width: screenW }).png().toBuffer({ resolveWithObject: true });
  const screen = await sharp(data).composite([{ input: rounded(screenW, info.height, 57), blend: 'dest-in' }]).png().toBuffer();
  const outerW = screenW + bezel * 2;
  const outerH = info.height + bezel * 2;
  const pad = 20;
  const cw = outerW + pad * 2;
  const ch = outerH + pad * 2;
  const phoneBuf = await sharp({ create: { width: cw, height: ch, channels: 4, background: { r: 0, g: 0, b: 0, alpha: 0 } } })
    .composite([
      { input: smartphoneFrameSvg(pad, pad, outerW, outerH, cw, ch) },
      { input: screen, left: pad + bezel, top: pad + bezel },
      { input: smartphoneDetailsSvg(pad, pad, outerW, bezel, info.height, cw, ch) },
    ]).png().toBuffer();
  const angle = -35;
  const rotated = await sharp(phoneBuf).rotate(angle, { background: { r: 0, g: 0, b: 0, alpha: 0 } }).png().toBuffer({ resolveWithObject: true });
  const pairW = width * 2 + 10;
  const cx = Math.round(pairW / 2 - 42);
  const cy = Math.round(height / 2 - 58);
  const big = await sharp({ create: { width: pairW + 1000, height: height + 1000, channels: 4, background: { r: 0, g: 0, b: 0, alpha: 0 } } })
    .composite([{ input: rotated.data, left: Math.round(cx - rotated.info.width / 2) + 500, top: Math.round(cy - rotated.info.height / 2) + 500 }])
    .png().toBuffer();
  const clipped = await sharp(big).extract({ left: 500, top: 500, width: pairW, height }).png().toBuffer();
  const sh = `<g transform="translate(${cx} ${cy}) rotate(${angle}) translate(${-outerW / 2} ${-outerH / 2})"><rect width="${outerW}" height="${outerH}" rx="72" fill="white"/></g>`;
  const pairSh = svg(`
    <defs>
      <filter id="s1" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="52"/></filter>
      <filter id="s2" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="20"/></filter>
      <filter id="s3" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="7"/></filter>
    </defs>
    <g transform="translate(0 36)" opacity=".28" filter="url(#s1)">${sh.replaceAll('fill="white"', 'fill="#182A5E"')}</g>
    <g transform="translate(0 18)" opacity=".26" filter="url(#s2)">${sh.replaceAll('fill="white"', 'fill="#111E42"')}</g>
    <g transform="translate(0 6)" opacity=".22" filter="url(#s3)">${sh.replaceAll('fill="white"', 'fill="#0A122B"')}</g>
  `, pairW, height);
  cachedHeroPair = await sharp({ create: { width: pairW, height, channels: 4, background: { r: 0, g: 0, b: 0, alpha: 0 } } })
    .composite([{ input: pairSh }, { input: clipped }]).png().toBuffer();
  return cachedHeroPair;
}

async function cardMediaLayers(index, spec) {
  if (index >= 2) {
    const screenRaw = await extractPhone(index, spec);
    const bezel = 15;
    const screenW = 626;
    const { data, info } = await sharp(screenRaw).resize({ width: screenW }).png().toBuffer({ resolveWithObject: true });
    const screen = await sharp(data).composite([{ input: rounded(screenW, info.height, 55), blend: 'dest-in' }]).png().toBuffer();
    const outerW = screenW + bezel * 2;
    const outerH = info.height + bezel * 2;
    const left = Math.round((width - outerW) / 2);
    const top = height - outerH + 22;
    return [
      { input: shadow(`<rect x="${left}" y="${top}" width="${outerW}" height="${outerH}" rx="70" fill="white"/>`) },
      { input: smartphoneFrameSvg(left, top, outerW, outerH) },
      { input: screen, left: left + bezel, top: top + bezel },
      { input: smartphoneDetailsSvg(left, top, outerW, bezel) },
    ];
  }
  if (heroScreenPath && existsSync(heroScreenPath)) {
    const pair = await buildHeroPairLayer();
    return [{ input: await sharp(pair).extract({ left: index * (width + 10), top: 0, width, height }).png().toBuffer() }];
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

function textLayoutFor(index) {
  if (index === 0) {
    return { x: 74, anchor: 'start', titleTop: 1428, titleStep: 74, subtitleTop: 1602, svgY: 1492, svgSubY: 1660, titlePt: 52, subPt: 25, svgSize: 64, fitWidth: false };
  }
  if (index === 1) {
    return { x: 585, anchor: 'middle', titleTop: 144, titleStep: 74, subtitleTop: 316, svgY: 196, svgSubY: 326, titlePt: 52, subPt: 25, svgSize: 64, fitWidth: false };
  }
  return { x: 450, anchor: 'middle', titleTop: 178, titleStep: 68, subtitleTop: 262, svgY: 236, svgSubY: 296, titlePt: 50, subPt: 26, svgSize: 60, fitWidth: true };
}

function alignLeft(x, anchor, boxWidth) {
  if (anchor === 'middle') return Math.round(x - boxWidth / 2);
  if (anchor === 'end') return Math.round(x - boxWidth);
  return x;
}

function renderSvgTextLayers(spec, layout) {
  const titles = spec[2].map((line, i) => text(line, layout.x, layout.svgY + i * 78, layout.svgSize, brandColors.ink, 500, layout.anchor));
  const subtitles = spec[3].map((line, i) => text(line, layout.x, layout.svgSubY + i * 40, 29, brandColors.muted, 400, layout.anchor));
  return [{ input: svg(titles.join('') + subtitles.join('')) }];
}

async function renderOutfitTextLayers(spec, layout) {
  let { titlePt, subPt } = layout;
  if (layout.fitWidth) {
    const probe = await renderOutfitLine(spec[2][0], titlePt, brandColors.ink, outfitTitleFont, 'Outfit Medium', -0.8);
    if (probe.info.width > 780) titlePt = Math.floor(titlePt * 780 / probe.info.width);
    const subProbe = await renderOutfitLine(spec[3][0], subPt, brandColors.muted, outfitBodyFont, 'Outfit', 0);
    if (subProbe.info.width > 800) subPt = Math.floor(subPt * 800 / subProbe.info.width);
  }
  const layers = [];
  for (const [i, line] of spec[2].entries()) {
    const { data, info } = await renderOutfitLine(line, titlePt, brandColors.ink, outfitTitleFont, 'Outfit Medium', -0.8);
    layers.push({ input: data, left: alignLeft(layout.x, layout.anchor, info.width), top: layout.titleTop + i * layout.titleStep });
  }
  for (const [i, line] of spec[3].entries()) {
    const { data, info } = await renderOutfitLine(line, subPt, brandColors.muted, outfitBodyFont, 'Outfit', 0);
    layers.push({ input: data, left: alignLeft(layout.x, layout.anchor, info.width), top: layout.subtitleTop + i * 42 });
  }
  return layers;
}

async function renderCard(index, spec) {
  const shapeColor = shapeColors[index % shapeColors.length];
  const backdrop = cardBackdrop(index, shapeColor);
  const layers = await cardMediaLayers(index, spec);
  const layout = textLayoutFor(index);
  const textLayers = hasOutfitFiles ? await renderOutfitTextLayers(spec, layout) : renderSvgTextLayers(spec, layout);
  layers.push(...textLayers);
  const card = await sharp(backdrop).composite(layers).png().toBuffer();
  return sharp(card).composite([{ input: rounded(width, height, 8), blend: 'dest-in' }]).png().toBuffer();
}

await mkdir(output, { recursive: true });
const rendered = [];
for (const [index, spec] of cards.entries()) {
  const card = await renderCard(index, spec);
  rendered.push(card);
  await sharp(card).webp({ quality: 92, effort: 4 }).toFile(resolve(output, `card-${String(index + 1).padStart(2, '0')}-studio.webp`));
}
const gap = 10;
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
