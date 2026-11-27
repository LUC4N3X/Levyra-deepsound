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
        surface: '#EBF2FE',
        border: '#C8DCFA',
        glowA: '#D5C6FC',
        glowB: '#C7DFFF',
        ink: '#13284A',
        muted: '#42597C',
        chipBg: '#E6D5FA',
        chipInk: '#3F225F',
        frameStroke: '#14345C',
        frameOpacity: '0.10',
      }
    : {
        surface: '#1B2436',
        border: '#2D3B56',
        glowA: '#9B6BFF',
        glowB: '#6FA4FF',
        ink: '#F0F5FF',
        muted: '#AFC0DC',
        chipBg: '#E6D5FA',
        chipInk: '#3F225F',
        frameStroke: '#FFFFFF',
        frameOpacity: '0.16',
      };
}

async function preparePortrait(rawPhoto, size, radius, pal) {
  const resized = await sharp(rawPhoto)
    .resize({ width: size, height: size, fit: 'cover', kernel: sharp.kernel.lanczos3 })
    .sharpen({ sigma: 0.6 })
    .png()
    .toBuffer();
  const mask = svg(`<rect width="${size}" height="${size}" rx="${radius}" ry="${radius}" fill="white"/>`, size, size);
  const rim = svg(
    `<rect x="1" y="1" width="${size - 2}" height="${size - 2}" rx="${radius - 1}" fill="none" stroke="${pal.frameStroke}" stroke-opacity="${pal.frameOpacity}" stroke-width="2"/>`,
    size,
    size,
  );
  const clipped = await sharp(resized).composite([{ input: mask, blend: 'dest-in' }]).png().toBuffer();
  return sharp(clipped).composite([{ input: rim }]).png().toBuffer();
}

async function renderDesktopCard(rawPhoto, light) {
  const W = 1450;
  const H = 468;
  const pal = palette(light);
  const cardPath = roundedCornersPath(4, 4, W - 8, H - 8, [64, 26, 64, 26]);
  const chipPath = roundedCornersPath(504, 92, 344, 54, [27, 12, 27, 12]);
  const chipRosette = scallopPath(534, 119, 14, 17.5, 10);

  const bgSvg = svg(`
    <defs>
      <clipPath id="cardClip"><path d="${cardPath}"/></clipPath>
      <radialGradient id="gA" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stop-color="${pal.glowA}" stop-opacity="${light ? '.28' : '.16'}"/>
        <stop offset="100%" stop-color="${pal.glowA}" stop-opacity="0"/>
      </radialGradient>
      <radialGradient id="gB" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stop-color="${pal.glowB}" stop-opacity="${light ? '.24' : '.12'}"/>
        <stop offset="100%" stop-color="${pal.glowB}" stop-opacity="0"/>
      </radialGradient>
    </defs>
    <path d="${cardPath}" fill="${pal.surface}"/>
    <g clip-path="url(#cardClip)">
      <ellipse cx="260" cy="234" rx="360" ry="300" fill="url(#gA)"/>
      <ellipse cx="1280" cy="110" rx="360" ry="260" fill="url(#gB)"/>
    </g>
    <path d="${cardPath}" fill="none" stroke="${pal.border}" stroke-width="2"/>
    <path d="${chipPath}" fill="${pal.chipBg}"/>
    <path d="${chipRosette}" fill="${pal.chipInk}" fill-opacity=".12"/>
    <path d="M534 109 l2.4 5.4 5.6 0.6 -4.2 3.8 1.2 5.5 -5 -2.9 -5 2.9 1.2 -5.5 -4.2 -3.8 5.6 -0.6 Z" fill="${pal.chipInk}"/>
  `, W, H);

  const portrait = await preparePortrait(rawPhoto, 388, 44, pal);
  const chipLabel = await renderTextLine('CREATOR & MAINTAINER', 16.5, pal.chipInk, outfitBold, 'Outfit SemiBold', 1.1);
  const nameLine = await renderTextLine('LUC4N3X', 50, pal.ink, outfitBold, 'Outfit SemiBold', -0.6);
  const bio1 = await renderTextLine('I built Levyra and maintain the Android, Windows and Linux apps.', 22.5, pal.muted, outfitRegular, 'Outfit', 0);
  const bio2 = await renderTextLine('I review contributions, fix bugs and handle releases.', 22.5, pal.muted, outfitRegular, 'Outfit', 0);

  return sharp(bgSvg)
    .composite([
      { input: portrait, left: 44, top: 40 },
      { input: chipLabel.data, left: 560, top: Math.round(119 - chipLabel.info.height / 2) },
      { input: nameLine.data, left: 504, top: 172 },
      { input: bio1.data, left: 504, top: 274 },
      { input: bio2.data, left: 504, top: 320 },
    ])
    .webp({ quality: 92, effort: 5 })
    .toBuffer();
}

async function renderMobileCard(rawPhoto, light) {
  const W = 960;
  const H = 880;
  const pal = palette(light);
  const cardPath = roundedCornersPath(4, 4, W - 8, H - 8, [58, 58, 28, 28]);
  const chipW = 348;
  const chipX = Math.round((W - chipW) / 2);
  const chipPath = roundedCornersPath(chipX, 498, chipW, 54, [27, 12, 27, 12]);
  const chipRosette = scallopPath(chipX + 30, 525, 14, 17.5, 10);

  const bgSvg = svg(`
    <defs>
      <clipPath id="cardClip"><path d="${cardPath}"/></clipPath>
      <radialGradient id="gA" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stop-color="${pal.glowA}" stop-opacity="${light ? '.28' : '.16'}"/>
        <stop offset="100%" stop-color="${pal.glowA}" stop-opacity="0"/>
      </radialGradient>
    </defs>
    <path d="${cardPath}" fill="${pal.surface}"/>
    <g clip-path="url(#cardClip)">
      <ellipse cx="${W / 2}" cy="240" rx="380" ry="300" fill="url(#gA)"/>
    </g>
    <path d="${cardPath}" fill="none" stroke="${pal.border}" stroke-width="2"/>
    <path d="${chipPath}" fill="${pal.chipBg}"/>
    <path d="${chipRosette}" fill="${pal.chipInk}" fill-opacity=".12"/>
    <path d="M${chipX + 30} 515 l2.4 5.4 5.6 0.6 -4.2 3.8 1.2 5.5 -5 -2.9 -5 2.9 1.2 -5.5 -4.2 -3.8 5.6 -0.6 Z" fill="${pal.chipInk}"/>
  `, W, H);

  const portrait = await preparePortrait(rawPhoto, 400, 46, pal);
  const chipLabel = await renderTextLine('CREATOR & MAINTAINER', 16.5, pal.chipInk, outfitBold, 'Outfit SemiBold', 1.1);
  const nameLine = await renderTextLine('LUC4N3X', 52, pal.ink, outfitBold, 'Outfit SemiBold', -0.6);
  const bio1 = await renderTextLine('I built Levyra and maintain the Android,', 23, pal.muted, outfitRegular, 'Outfit', 0);
  const bio2 = await renderTextLine('Windows and Linux apps. I review contributions,', 23, pal.muted, outfitRegular, 'Outfit', 0);
  const bio3 = await renderTextLine('fix bugs and handle releases.', 23, pal.muted, outfitRegular, 'Outfit', 0);

  return sharp(bgSvg)
    .composite([
      { input: portrait, left: Math.round((W - 400) / 2), top: 56 },
      { input: chipLabel.data, left: chipX + 56, top: Math.round(525 - chipLabel.info.height / 2) },
      { input: nameLine.data, left: Math.round((W - nameLine.info.width) / 2), top: 576 },
      { input: bio1.data, left: Math.round((W - bio1.info.width) / 2), top: 680 },
      { input: bio2.data, left: Math.round((W - bio2.info.width) / 2), top: 724 },
      { input: bio3.data, left: Math.round((W - bio3.info.width) / 2), top: 768 },
    ])
    .webp({ quality: 92, effort: 5 })
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
  sharp(darkDesk).toFile(resolve(output, 'levyra-creator-card.webp')),
  sharp(lightDesk).toFile(resolve(output, 'levyra-creator-card-light.webp')),
  sharp(darkMob).toFile(resolve(output, 'levyra-creator-card-mobile.webp')),
  sharp(lightMob).toFile(resolve(output, 'levyra-creator-card-mobile-light.webp')),
]);

console.log('Rendered levyra-creator-card (desktop/mobile, dark/light).');
