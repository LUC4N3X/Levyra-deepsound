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
        muted: '#5E4455',
        chipBg: '#FAE3B4',
        chipInk: '#4B3409',
        frameStroke: '#2B1624',
        frameOpacity: '0.11',
      }
    : {
        surface: '#2A1D29',
        border: '#422E40',
        glowA: '#A855F7',
        glowB: '#F59E0B',
        ink: '#FBF2F7',
        muted: '#DCCBD7',
        chipBg: '#F6D2E0',
        chipInk: '#5A2238',
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

async function renderDesktopCard(rawPhoto, light) {
  const W = 1620;
  const H = 820;
  const pal = palette(light);
  const cardPath = roundedCornersPath(4, 4, W - 8, H - 8, [84, 54, 84, 54]);
  const textX = 844;
  const chipW = 368;
  const chipH = 58;
  const chipY = 232;
  const chipPath = roundedCornersPath(textX, chipY, chipW, chipH, [29, 14, 29, 14]);
  const chipRosette = scallopPath(textX + 32, chipY + chipH / 2, 15, 18.8, 10);

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
      <ellipse cx="410" cy="410" rx="520" ry="440" fill="url(#gA)"/>
      <ellipse cx="1360" cy="180" rx="420" ry="300" fill="url(#gB)"/>
    </g>
    <path d="${cardPath}" fill="none" stroke="${pal.border}" stroke-width="2.5"/>
    <path d="${chipPath}" fill="${pal.chipBg}"/>
    <path d="${chipRosette}" fill="${pal.chipInk}" fill-opacity=".12"/>
    <path d="M${textX + 32} ${chipY + chipH / 2 - 10} l2.5 5.6 5.8 0.6 -4.4 4 1.3 5.7 -5.2 -3 -5.2 3 1.3 -5.7 -4.4 -4 5.8 -0.6 Z" fill="${pal.chipInk}"/>
  `, W, H);

  const portrait = await preparePortrait(rawPhoto, 740, 44, pal);
  const chipLabel = await renderTextLine('CREATOR & MAINTAINER', 17.5, pal.chipInk, outfitBold, 'Outfit SemiBold', 1.15);
  const nameLine = await renderTextLine('LUC4N3X', 64, pal.ink, outfitBold, 'Outfit SemiBold', -0.8);
  const bio1 = await renderTextLine('I built Levyra and still maintain the Android,', 25, pal.muted, outfitRegular, 'Outfit', 0);
  const bio2 = await renderTextLine('Windows and Linux apps. I review contributions,', 25, pal.muted, outfitRegular, 'Outfit', 0);
  const bio3 = await renderTextLine('fix bugs and handle releases.', 25, pal.muted, outfitRegular, 'Outfit', 0);

  return sharp(bgSvg)
    .composite([
      { input: portrait, left: 40, top: 40 },
      { input: chipLabel.data, left: textX + 60, top: Math.round(chipY + chipH / 2 - chipLabel.info.height / 2) },
      { input: nameLine.data, left: textX, top: 320 },
      { input: bio1.data, left: textX, top: 442 },
      { input: bio2.data, left: textX, top: 492 },
      { input: bio3.data, left: textX, top: 542 },
    ])
    .webp({ quality: 95, smartSubsample: false, effort: 5 })
    .toBuffer();
}

async function renderMobileCard(rawPhoto, light) {
  const W = 960;
  const H = 1328;
  const pal = palette(light);
  const cardPath = roundedCornersPath(4, 4, W - 8, H - 8, [76, 76, 48, 48]);
  const chipW = 368;
  const chipH = 58;
  const chipX = Math.round((W - chipW) / 2);
  const chipY = 924;
  const chipPath = roundedCornersPath(chipX, chipY, chipW, chipH, [29, 14, 29, 14]);
  const chipRosette = scallopPath(chipX + 32, chipY + chipH / 2, 15, 18.8, 10);

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
      <ellipse cx="${W / 2}" cy="460" rx="500" ry="440" fill="url(#gA)"/>
    </g>
    <path d="${cardPath}" fill="none" stroke="${pal.border}" stroke-width="2.5"/>
    <path d="${chipPath}" fill="${pal.chipBg}"/>
    <path d="${chipRosette}" fill="${pal.chipInk}" fill-opacity=".12"/>
    <path d="M${chipX + 32} ${chipY + chipH / 2 - 10} l2.5 5.6 5.8 0.6 -4.4 4 1.3 5.7 -5.2 -3 -5.2 3 1.3 -5.7 -4.4 -4 5.8 -0.6 Z" fill="${pal.chipInk}"/>
  `, W, H);

  const portrait = await preparePortrait(rawPhoto, 832, 46, pal);
  const chipLabel = await renderTextLine('CREATOR & MAINTAINER', 17.5, pal.chipInk, outfitBold, 'Outfit SemiBold', 1.15);
  const nameLine = await renderTextLine('LUC4N3X', 58, pal.ink, outfitBold, 'Outfit SemiBold', -0.7);
  const bio1 = await renderTextLine('I built Levyra and still maintain the Android,', 24.5, pal.muted, outfitRegular, 'Outfit', 0);
  const bio2 = await renderTextLine('Windows and Linux apps. I review contributions,', 24.5, pal.muted, outfitRegular, 'Outfit', 0);
  const bio3 = await renderTextLine('fix bugs and handle releases.', 24.5, pal.muted, outfitRegular, 'Outfit', 0);

  return sharp(bgSvg)
    .composite([
      { input: portrait, left: 64, top: 52 },
      { input: chipLabel.data, left: chipX + 60, top: Math.round(chipY + chipH / 2 - chipLabel.info.height / 2) },
      { input: nameLine.data, left: Math.round((W - nameLine.info.width) / 2), top: 1004 },
      { input: bio1.data, left: Math.round((W - bio1.info.width) / 2), top: 1112 },
      { input: bio2.data, left: Math.round((W - bio2.info.width) / 2), top: 1158 },
      { input: bio3.data, left: Math.round((W - bio3.info.width) / 2), top: 1204 },
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
  sharp(darkDesk).toFile(resolve(output, 'levyra-author-card.webp')),
  sharp(lightDesk).toFile(resolve(output, 'levyra-author-card-light.webp')),
  sharp(darkMob).toFile(resolve(output, 'levyra-author-card-mobile.webp')),
  sharp(lightMob).toFile(resolve(output, 'levyra-author-card-mobile-light.webp')),
]);

console.log('Rendered levyra-author-card (desktop/mobile, dark/light).');
