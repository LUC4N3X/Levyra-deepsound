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

function scallopPath(cx, cy, rValley, rPeak, lobes = 12) {
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
  return {
    surface: light ? '#F8E7F1' : '#F5E1EC',
    border: light ? '#E6C8DA' : 'none',
    glowA: '#E2C6FA',
    glowB: '#FDE0CA',
    ink: '#2C1322',
    muted: '#533246',
    chipBg: '#E3CCFA',
    chipInk: '#3A1B59',
    seg1Bg: '#C6EEDE',
    seg1Ink: '#123D2B',
    seg2Bg: '#FAE0AE',
    seg2Ink: '#4A3308',
    frameStroke: '#2C1322',
    frameOpacity: '0.10',
  };
}

async function preparePortrait(rawPhoto, size, corners, pal) {
  const resized = await sharp(rawPhoto)
    .resize({ width: size, height: size, fit: 'cover', kernel: sharp.kernel.lanczos3 })
    .sharpen({ sigma: 0.55 })
    .png()
    .toBuffer();
  const outerD = roundedCornersPath(0, 0, size, size, corners);
  const innerD = roundedCornersPath(1.5, 1.5, size - 3, size - 3, corners.map(r => Math.max(4, r - 1.5)));
  const mask = svg(`<path d="${outerD}" fill="white"/>`, size, size);
  const rim = svg(
    `<path d="${innerD}" fill="none" stroke="${pal.frameStroke}" stroke-opacity="${pal.frameOpacity}" stroke-width="3"/>`,
    size,
    size,
  );
  const clipped = await sharp(resized).composite([{ input: mask, blend: 'dest-in' }]).png().toBuffer();
  return sharp(clipped).composite([{ input: rim }]).png().toBuffer();
}

async function buildConnectedBadges(pal, startX, y, centeredWidth = 0) {
  const items = [
    {
      label: 'Kotlin & Compose',
      bg: pal.seg1Bg,
      fg: pal.seg1Ink,
      corners: [28, 10, 10, 28],
      icon: '<path fill="currentColor" d="M9.4 16.6L4.8 12l4.6-4.6L8 6l-6 6 6 6 1.4-1.4zm5.2 0l4.6-4.6-4.6-4.6L16 6l6 6-6 6-1.4-1.4z"/>',
    },
    {
      label: 'Made in Italy',
      bg: pal.seg2Bg,
      fg: pal.seg2Ink,
      corners: [10, 28, 28, 10],
      icon: '<path fill="currentColor" d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z"/>',
    },
  ];
  const h = 56;
  const gap = 6;
  const rendered = await Promise.all(
    items.map(it => renderTextLine(it.label, 16.5, it.fg, outfitBold, 'Outfit SemiBold', 0.15)),
  );
  const widths = rendered.map(r => r.info.width + 72);
  const totalW = widths.reduce((a, b) => a + b, 0) + gap;
  let curX = centeredWidth > 0 ? Math.round((centeredWidth - totalW) / 2) : startX;

  const svgPaths = [];
  const composites = [];
  for (let i = 0; i < items.length; i += 1) {
    const w = widths[i];
    const d = roundedCornersPath(curX, y, w, h, items[i].corners);
    svgPaths.push(`<path d="${d}" fill="${items[i].bg}"/>`);
    svgPaths.push(
      `<g transform="translate(${curX + 20} ${y + h / 2 - 10.5}) scale(0.88)" style="color:${items[i].fg}">${items[i].icon}</g>`,
    );
    composites.push({
      input: rendered[i].data,
      left: curX + 48,
      top: Math.round(y + h / 2 - rendered[i].info.height / 2),
    });
    curX += w + gap;
  }
  return { svgMarkup: svgPaths.join('\n'), composites };
}

async function renderDesktopCard(rawPhoto, light) {
  const W = 1284;
  const H = 660;
  const pal = palette(light);
  const cardPath = roundedCornersPath(2, 2, W - 4, H - 4, [96, 48, 96, 48]);
  const textX = 680;
  const chipW = 352;
  const chipH = 58;
  const chipY = 82;
  const chipPath = roundedCornersPath(textX, chipY, chipW, chipH, [29, 14, 29, 14]);
  const chipRosette = scallopPath(textX + 32, chipY + chipH / 2, 15, 18.8, 12);

  const badges = await buildConnectedBadges(pal, textX, 514);
  const borderMarkup = pal.border === 'none'
    ? ''
    : `<path d="${cardPath}" fill="none" stroke="${pal.border}" stroke-width="2"/>`;

  const bgSvg = svg(`
    <defs>
      <clipPath id="cardClip"><path d="${cardPath}"/></clipPath>
      <radialGradient id="gA" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stop-color="${pal.glowA}" stop-opacity=".55"/>
        <stop offset="100%" stop-color="${pal.glowA}" stop-opacity="0"/>
      </radialGradient>
      <radialGradient id="gB" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stop-color="${pal.glowB}" stop-opacity=".50"/>
        <stop offset="100%" stop-color="${pal.glowB}" stop-opacity="0"/>
      </radialGradient>
    </defs>
    <path d="${cardPath}" fill="${pal.surface}"/>
    <g clip-path="url(#cardClip)">
      <ellipse cx="340" cy="330" rx="440" ry="380" fill="url(#gA)"/>
      <ellipse cx="1110" cy="130" rx="350" ry="270" fill="url(#gB)"/>
    </g>
    ${borderMarkup}
    <path d="${chipPath}" fill="${pal.chipBg}"/>
    <path d="${chipRosette}" fill="${pal.chipInk}" fill-opacity=".13"/>
    <path d="M${textX + 32} ${chipY + chipH / 2 - 9.5} l2.3 5.2 5.4 0.6 -4.1 3.7 1.2 5.3 -4.8 -2.8 -4.8 2.8 1.2 -5.3 -4.1 -3.7 5.4 -0.6 Z" fill="${pal.chipInk}"/>
    ${badges.svgMarkup}
  `, W, H);

  const portrait = await preparePortrait(rawPhoto, 596, [64, 28, 56, 20], pal);
  const chipLabel = await renderTextLine('CREATOR & MAINTAINER', 16, pal.chipInk, outfitBold, 'Outfit SemiBold', 1.05);
  const nameLine = await renderTextLine('LUC4N3X', 62, pal.ink, outfitBold, 'Outfit SemiBold', -0.8);
  const bio1 = await renderTextLine('I built Levyra and maintain the Android,', 22.5, pal.muted, outfitRegular, 'Outfit', 0);
  const bio2 = await renderTextLine('Windows, and Linux apps. I review pull', 22.5, pal.muted, outfitRegular, 'Outfit', 0);
  const bio3 = await renderTextLine('requests and ship every release.', 22.5, pal.muted, outfitRegular, 'Outfit', 0);

  return sharp(bgSvg)
    .composite([
      { input: portrait, left: 32, top: 32 },
      { input: chipLabel.data, left: textX + 60, top: Math.round(chipY + chipH / 2 - chipLabel.info.height / 2) },
      { input: nameLine.data, left: textX, top: 164 },
      { input: bio1.data, left: textX, top: 292 },
      { input: bio2.data, left: textX, top: 340 },
      { input: bio3.data, left: textX, top: 388 },
      ...badges.composites,
    ])
    .webp({ quality: 95, smartSubsample: false, effort: 5 })
    .toBuffer();
}

async function renderMobileCard(rawPhoto, light) {
  const W = 920;
  const H = 1356;
  const pal = palette(light);
  const cardPath = roundedCornersPath(2, 2, W - 4, H - 4, [84, 84, 52, 52]);
  const chipW = 352;
  const chipH = 58;
  const chipX = Math.round((W - chipW) / 2);
  const chipY = 894;
  const chipPath = roundedCornersPath(chipX, chipY, chipW, chipH, [29, 14, 29, 14]);
  const chipRosette = scallopPath(chipX + 32, chipY + chipH / 2, 15, 18.8, 12);

  const badges = await buildConnectedBadges(pal, 0, 1240, W);
  const borderMarkup = pal.border === 'none'
    ? ''
    : `<path d="${cardPath}" fill="none" stroke="${pal.border}" stroke-width="2"/>`;

  const bgSvg = svg(`
    <defs>
      <clipPath id="cardClip"><path d="${cardPath}"/></clipPath>
      <radialGradient id="gA" cx="50%" cy="50%" r="50%">
        <stop offset="0%" stop-color="${pal.glowA}" stop-opacity=".55"/>
        <stop offset="100%" stop-color="${pal.glowA}" stop-opacity="0"/>
      </radialGradient>
    </defs>
    <path d="${cardPath}" fill="${pal.surface}"/>
    <g clip-path="url(#cardClip)">
      <ellipse cx="${W / 2}" cy="440" rx="480" ry="420" fill="url(#gA)"/>
    </g>
    ${borderMarkup}
    <path d="${chipPath}" fill="${pal.chipBg}"/>
    <path d="${chipRosette}" fill="${pal.chipInk}" fill-opacity=".13"/>
    <path d="M${chipX + 32} ${chipY + chipH / 2 - 9.5} l2.3 5.2 5.4 0.6 -4.1 3.7 1.2 5.3 -4.8 -2.8 -4.8 2.8 1.2 -5.3 -4.1 -3.7 5.4 -0.6 Z" fill="${pal.chipInk}"/>
    ${badges.svgMarkup}
  `, W, H);

  const portrait = await preparePortrait(rawPhoto, 808, [56, 56, 36, 24], pal);
  const chipLabel = await renderTextLine('CREATOR & MAINTAINER', 16, pal.chipInk, outfitBold, 'Outfit SemiBold', 1.05);
  const nameLine = await renderTextLine('LUC4N3X', 58, pal.ink, outfitBold, 'Outfit SemiBold', -0.7);
  const bio1 = await renderTextLine('I built Levyra and maintain the Android,', 23.5, pal.muted, outfitRegular, 'Outfit', 0);
  const bio2 = await renderTextLine('Windows, and Linux apps. I review pull', 23.5, pal.muted, outfitRegular, 'Outfit', 0);
  const bio3 = await renderTextLine('requests and ship every release.', 23.5, pal.muted, outfitRegular, 'Outfit', 0);

  return sharp(bgSvg)
    .composite([
      { input: portrait, left: 56, top: 48 },
      { input: chipLabel.data, left: chipX + 60, top: Math.round(chipY + chipH / 2 - chipLabel.info.height / 2) },
      { input: nameLine.data, left: Math.round((W - nameLine.info.width) / 2), top: 974 },
      { input: bio1.data, left: Math.round((W - bio1.info.width) / 2), top: 1076 },
      { input: bio2.data, left: Math.round((W - bio2.info.width) / 2), top: 1122 },
      { input: bio3.data, left: Math.round((W - bio3.info.width) / 2), top: 1168 },
      ...badges.composites,
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
  sharp(darkDesk).toFile(resolve(output, 'levyra-m3-author.webp')),
  sharp(lightDesk).toFile(resolve(output, 'levyra-m3-author-light.webp')),
  sharp(darkMob).toFile(resolve(output, 'levyra-m3-author-mobile.webp')),
  sharp(lightMob).toFile(resolve(output, 'levyra-m3-author-mobile-light.webp')),
]);

console.log('Rendered levyra-m3-author (pastel M3 Expressive, desktop/mobile, dark/light).');
