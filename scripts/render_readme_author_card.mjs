import { existsSync } from 'node:fs';
import { mkdir, readFile } from 'node:fs/promises';
import { createRequire } from 'node:module';
import { resolve } from 'node:path';

function arg(name, fallback) {
  const index = process.argv.indexOf(name);
  return index >= 0 && process.argv[index + 1] ? process.argv[index + 1] : fallback;
}

const require = createRequire(import.meta.url);
const sharpModule = arg('--sharp-module', 'sharp');
const sourcePath = resolve(arg('--source', 'docs/assets/levyra-signature.webp'));
const output = resolve(arg('--output', 'docs/assets'));
const fontDir = resolve(arg('--font-dir', '.levyra-tools/readme-showcase/fonts'));
const outfitBold = resolve(fontDir, 'Outfit-600.ttf');
const outfitMedium = resolve(fontDir, 'Outfit-500.ttf');
const outfitRegular = resolve(fontDir, 'Outfit-400.ttf');
const hasFonts = existsSync(outfitBold) && existsSync(outfitMedium) && existsSync(outfitRegular);

const sharp = require(sharpModule.startsWith('.') || sharpModule.includes('/') || sharpModule.includes('\\')
  ? resolve(sharpModule)
  : sharpModule);

function svg(body, w, h) {
  return Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}">${body}</svg>`);
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

async function renderTextLine(content, ptSize, color, fontfile, family, letterSpacingPt = 0) {
  const ls = letterSpacingPt ? ` letter_spacing="${Math.round(letterSpacingPt * 1024)}"` : '';
  const safe = content.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
  return sharp({
    text: {
      text: `<span foreground="${color}"${ls}>${safe}</span>`,
      font: hasFonts ? `${family} ${ptSize}` : `Segoe UI ${ptSize}`,
      ...(hasFonts ? { fontfile } : {}),
      rgba: true,
      dpi: 96,
    },
  }).png().toBuffer({ resolveWithObject: true });
}

function palette(light) {
  return light
    ? {
        surface: '#F9EAF2',
        border: '#E8CEDD',
        glowA: '#E8CEF8',
        glowB: '#FCE0C8',
        ink: '#2B1624',
        muted: '#5B4052',
        divider: '#DEC0D2',
        chipBg: '#FAE3B4',
        chipInk: '#4B3409',
        tagBg: '#EFE0EA',
        tagBorder: '#DFC5D6',
        tagInk: '#4A2D41',
        tagDot: '#9B3E70',
        frameStroke: '#2B1624',
        frameOpacity: '0.11',
      }
    : {
        surface: '#2A1D29',
        border: '#422E40',
        glowA: '#A855F7',
        glowB: '#F59E0B',
        ink: '#FBF2F7',
        muted: '#DECED9',
        divider: '#463144',
        chipBg: '#F6D2E0',
        chipInk: '#5A2238',
        tagBg: '#392838',
        tagBorder: '#523A50',
        tagInk: '#F4E5EF',
        tagDot: '#F6B8D6',
        frameStroke: '#FFFFFF',
        frameOpacity: '0.18',
      };
}

async function preparePortrait(rawPhoto, size, radius, pal) {
  const resized = await sharp(rawPhoto)
    .resize({ width: size, height: size, fit: 'cover', kernel: sharp.kernel.lanczos3 })
    .sharpen({ sigma: 0.55 })
    .png()
    .toBuffer();
  const mask = svg(`<rect width="${size}" height="${size}" rx="${radius}" ry="${radius}" fill="white"/>`, size, size);
  const rim = svg(
    `<rect x="1.5" y="1.5" width="${size - 3}" height="${size - 3}" rx="${radius - 1.5}" fill="none" stroke="${pal.frameStroke}" stroke-opacity="${pal.frameOpacity}" stroke-width="3"/>`,
    size,
    size,
  );
  const clipped = await sharp(resized).composite([{ input: mask, blend: 'dest-in' }]).png().toBuffer();
  return sharp(clipped).composite([{ input: rim }]).png().toBuffer();
}

async function buildTagPills(pal, startX, y, centeredWidth = 0) {
  const items = [
    { label: 'Kotlin', corners: [26, 10, 26, 10] },
    { label: 'Compose', corners: [26, 26, 26, 26] },
    { label: 'GPL-3.0', corners: [10, 26, 10, 26] },
  ];
  const h = 52;
  const gap = 10;
  const rendered = await Promise.all(
    items.map(it => renderTextLine(it.label, 16.5, pal.tagInk, outfitMedium, 'Outfit Medium', 0.2)),
  );
  const widths = rendered.map(r => r.info.width + 54);
  const totalW = widths.reduce((a, b) => a + b, 0) + gap * (items.length - 1);
  let curX = centeredWidth > 0 ? Math.round((centeredWidth - totalW) / 2) : startX;

  const svgPaths = [];
  const composites = [];
  for (let i = 0; i < items.length; i += 1) {
    const w = widths[i];
    const d = roundedCornersPath(curX, y, w, h, items[i].corners);
    svgPaths.push(`<path d="${d}" fill="${pal.tagBg}" stroke="${pal.tagBorder}" stroke-width="1.5"/>`);
    svgPaths.push(`<circle cx="${curX + 21}" cy="${y + h / 2}" r="4.5" fill="${pal.tagDot}"/>`);
    composites.push({
      input: rendered[i].data,
      left: curX + 34,
      top: Math.round(y + h / 2 - rendered[i].info.height / 2),
    });
    curX += w + gap;
  }
  return { svgMarkup: svgPaths.join('\n'), composites };
}

async function renderDesktopCard(rawPhoto, light) {
  const W = 1360;
  const H = 664;
  const pal = palette(light);
  const cardPath = roundedCornersPath(4, 4, W - 8, H - 8, [72, 46, 72, 46]);
  const textX = 684;
  const chipW = 342;
  const chipH = 54;
  const chipY = 86;
  const chipPath = roundedCornersPath(textX, chipY, chipW, chipH, [27, 13, 27, 13]);
  const chipRosette = scallopPath(textX + 30, chipY + chipH / 2, 14, 17.5, 10);

  const tags = await buildTagPills(pal, textX, 514);

  const bgSvg = svg(`
    <defs>
      <clipPath id="cardClip"><path d="${cardPath}"/></clipPath>
      <radialGradient id="gA" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stop-color="${pal.glowA}" stop-opacity="${light ? '.32' : '.16'}"/>
        <stop offset="100%" stop-color="${pal.glowA}" stop-opacity="0"/>
      </radialGradient>
      <radialGradient id="gB" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stop-color="${pal.glowB}" stop-opacity="${light ? '.28' : '.10'}"/>
        <stop offset="100%" stop-color="${pal.glowB}" stop-opacity="0"/>
      </radialGradient>
    </defs>
    <path d="${cardPath}" fill="${pal.surface}"/>
    <g clip-path="url(#cardClip)">
      <ellipse cx="332" cy="332" rx="440" ry="380" fill="url(#gA)"/>
      <ellipse cx="1160" cy="140" rx="360" ry="260" fill="url(#gB)"/>
    </g>
    <path d="${cardPath}" fill="none" stroke="${pal.border}" stroke-width="2.5"/>
    <path d="${chipPath}" fill="${pal.chipBg}"/>
    <path d="${chipRosette}" fill="${pal.chipInk}" fill-opacity=".12"/>
    <path d="M${textX + 30} ${chipY + chipH / 2 - 9.5} l2.3 5.2 5.4 0.6 -4.1 3.7 1.2 5.3 -4.8 -2.8 -4.8 2.8 1.2 -5.3 -4.1 -3.7 5.4 -0.6 Z" fill="${pal.chipInk}"/>
    <line x1="${textX}" y1="276" x2="${W - 56}" y2="276" stroke="${pal.divider}" stroke-width="2"/>
    ${tags.svgMarkup}
  `, W, H);

  const portrait = await preparePortrait(rawPhoto, 600, 40, pal);
  const chipLabel = await renderTextLine('CREATOR & MAINTAINER', 16, pal.chipInk, outfitBold, 'Outfit SemiBold', 1.05);
  const nameLine = await renderTextLine('LUC4N3X', 58, pal.ink, outfitBold, 'Outfit SemiBold', -0.7);
  const bio1 = await renderTextLine('I built Levyra and still maintain the Android,', 21.5, pal.muted, outfitRegular, 'Outfit', 0);
  const bio2 = await renderTextLine('Windows and Linux apps. I review contributions,', 21.5, pal.muted, outfitRegular, 'Outfit', 0);
  const bio3 = await renderTextLine('fix bugs and handle releases.', 21.5, pal.muted, outfitRegular, 'Outfit', 0);

  return sharp(bgSvg)
    .composite([
      { input: portrait, left: 32, top: 32 },
      { input: chipLabel.data, left: textX + 56, top: Math.round(chipY + chipH / 2 - chipLabel.info.height / 2) },
      { input: nameLine.data, left: textX, top: 164 },
      { input: bio1.data, left: textX, top: 308 },
      { input: bio2.data, left: textX, top: 354 },
      { input: bio3.data, left: textX, top: 400 },
      ...tags.composites,
    ])
    .webp({ quality: 95, smartSubsample: false, effort: 5 })
    .toBuffer();
}

async function renderMobileCard(rawPhoto, light) {
  const W = 920;
  const H = 1364;
  const pal = palette(light);
  const cardPath = roundedCornersPath(4, 4, W - 8, H - 8, [72, 72, 46, 46]);
  const chipW = 348;
  const chipH = 54;
  const chipX = Math.round((W - chipW) / 2);
  const chipY = 898;
  const chipPath = roundedCornersPath(chipX, chipY, chipW, chipH, [27, 13, 27, 13]);
  const chipRosette = scallopPath(chipX + 30, chipY + chipH / 2, 14, 17.5, 10);

  const tags = await buildTagPills(pal, 0, 1252, W);

  const bgSvg = svg(`
    <defs>
      <clipPath id="cardClip"><path d="${cardPath}"/></clipPath>
      <radialGradient id="gA" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stop-color="${pal.glowA}" stop-opacity="${light ? '.32' : '.16'}"/>
        <stop offset="100%" stop-color="${pal.glowA}" stop-opacity="0"/>
      </radialGradient>
    </defs>
    <path d="${cardPath}" fill="${pal.surface}"/>
    <g clip-path="url(#cardClip)">
      <ellipse cx="${W / 2}" cy="440" rx="480" ry="420" fill="url(#gA)"/>
    </g>
    <path d="${cardPath}" fill="none" stroke="${pal.border}" stroke-width="2.5"/>
    <path d="${chipPath}" fill="${pal.chipBg}"/>
    <path d="${chipRosette}" fill="${pal.chipInk}" fill-opacity=".12"/>
    <path d="M${chipX + 30} ${chipY + chipH / 2 - 9.5} l2.3 5.2 5.4 0.6 -4.1 3.7 1.2 5.3 -4.8 -2.8 -4.8 2.8 1.2 -5.3 -4.1 -3.7 5.4 -0.6 Z" fill="${pal.chipInk}"/>
    ${tags.svgMarkup}
  `, W, H);

  const portrait = await preparePortrait(rawPhoto, 808, 44, pal);
  const chipLabel = await renderTextLine('CREATOR & MAINTAINER', 16, pal.chipInk, outfitBold, 'Outfit SemiBold', 1.05);
  const nameLine = await renderTextLine('LUC4N3X', 56, pal.ink, outfitBold, 'Outfit SemiBold', -0.7);
  const bio1 = await renderTextLine('I built Levyra and still maintain the Android,', 23.5, pal.muted, outfitRegular, 'Outfit', 0);
  const bio2 = await renderTextLine('Windows and Linux apps. I review contributions,', 23.5, pal.muted, outfitRegular, 'Outfit', 0);
  const bio3 = await renderTextLine('fix bugs and handle releases.', 23.5, pal.muted, outfitRegular, 'Outfit', 0);

  return sharp(bgSvg)
    .composite([
      { input: portrait, left: 56, top: 48 },
      { input: chipLabel.data, left: chipX + 56, top: Math.round(chipY + chipH / 2 - chipLabel.info.height / 2) },
      { input: nameLine.data, left: Math.round((W - nameLine.info.width) / 2), top: 974 },
      { input: bio1.data, left: Math.round((W - bio1.info.width) / 2), top: 1080 },
      { input: bio2.data, left: Math.round((W - bio2.info.width) / 2), top: 1126 },
      { input: bio3.data, left: Math.round((W - bio3.info.width) / 2), top: 1172 },
      ...tags.composites,
    ])
    .webp({ quality: 95, smartSubsample: false, effort: 5 })
    .toBuffer();
}

await mkdir(output, { recursive: true });
const rawPhoto = await readFile(sourcePath);

const [darkDesk, lightDesk, darkMob, lightMob] = await Promise.all([
  renderDesktopCard(rawPhoto, false),
  renderDesktopCard(rawPhoto, true),
  renderMobileCard(rawPhoto, false),
  renderMobileCard(rawPhoto, true),
]);

await Promise.all([
  sharp(darkDesk).toFile(resolve(output, 'levyra-creator-bento.webp')),
  sharp(lightDesk).toFile(resolve(output, 'levyra-creator-bento-light.webp')),
  sharp(darkMob).toFile(resolve(output, 'levyra-creator-bento-mobile.webp')),
  sharp(lightMob).toFile(resolve(output, 'levyra-creator-bento-mobile-light.webp')),
]);

console.log('Rendered levyra-creator-bento (compact 680px, desktop/mobile, dark/light).');
