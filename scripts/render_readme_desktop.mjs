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
const sourcePath = resolve(arg('--source', 'docs/assets/showcase/editorial-cards/desktop_home_clean.webp'));
const output = resolve(arg('--output', 'docs/assets/showcase/readme'));
const fontDir = resolve(arg('--font-dir', '.levyra-tools/readme-showcase/fonts'));
const outfitTitleFont = resolve(fontDir, 'Outfit-500.ttf');
const outfitBodyFont = resolve(fontDir, 'Outfit-400.ttf');
const hasOutfitFiles = existsSync(outfitTitleFont) && existsSync(outfitBodyFont);

const sharp = require(sharpModule.startsWith('.') || sharpModule.includes('/') || sharpModule.includes('\\')
  ? resolve(sharpModule)
  : sharpModule);

const W = 3630;
let H = 2060;
const brandColors = { ink: '#161F33', muted: '#4D5C7A' };

function svg(body, w = W, h = H) {
  return Buffer.from(`<svg xmlns="http://www.w3.org/2000/svg" width="${w}" height="${h}" viewBox="0 0 ${w} ${h}">${body}</svg>`);
}

function flower(cx, cy, r, petals, amp) {
  const points = [];
  for (let i = 0; i < 160; i++) {
    const a = (i / 160) * Math.PI * 2;
    const rad = r * (1 + amp * Math.cos(petals * a));
    points.push(`${(cx + rad * Math.cos(a)).toFixed(1)},${(cy + rad * Math.sin(a)).toFixed(1)}`);
  }
  return `M ${points[0]} L ${points.slice(1).join(' L ')} Z`;
}

function roundedMask(w, h, r) {
  return svg(`<rect width="${w}" height="${h}" rx="${r}" ry="${r}" fill="white"/>`, w, h);
}

function topRoundedMask(w, h, r) {
  return svg(
    `<path d="M ${r} 0 H ${w - r} A ${r} ${r} 0 0 1 ${w} ${r} V ${h} H 0 V ${r} A ${r} ${r} 0 0 1 ${r} 0 Z" fill="white"/>`,
    w,
    h,
  );
}

function backdropSvg() {
  const wave1 = 'M -150 1120 C 650 860, 1450 1380, 2250 1040 C 2900 780, 3350 1180, 3820 960';
  const wave2 = 'M -150 1420 C 750 1620, 1550 1180, 2400 1480 C 3000 1660, 3400 1320, 3820 1440';
  const flowerL = flower(220, 380, 240, 10, 0.10);
  const flowerR = flower(3430, 340, 260, 8, 0.11);
  return svg(`
    <defs>
      <linearGradient id="baseSky" x1="0" y1="0.1" x2="1" y2="0.9">
        <stop offset="0%" stop-color="#FFFFFF"/>
        <stop offset="22%" stop-color="#FFFFFF"/>
        <stop offset="56%" stop-color="#EBF2FF"/>
        <stop offset="82%" stop-color="#CFE0FF"/>
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
        <stop offset="0%" stop-color="#8EB6F6" stop-opacity=".42"/>
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
      <linearGradient id="topVeil" x1="0" y1="0" x2="0.35" y2="0.55">
        <stop offset="0%" stop-color="#FFFFFF" stop-opacity=".62"/>
        <stop offset="48%" stop-color="#FFFFFF" stop-opacity=".18"/>
        <stop offset="100%" stop-color="#FFFFFF" stop-opacity="0"/>
      </linearGradient>
      <filter id="glassBlur" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="68"/></filter>
      <filter id="softRim" x="-10%" y="-10%" width="120%" height="120%"><feGaussianBlur stdDeviation="9"/></filter>
    </defs>
    <rect width="${W}" height="${H}" fill="url(#baseSky)"/>
    <ellipse cx="820" cy="1460" rx="1080" ry="680" fill="url(#orbA)"/>
    <ellipse cx="2780" cy="1220" rx="1020" ry="700" fill="url(#orbB)"/>
    <ellipse cx="1820" cy="1680" rx="980" ry="560" fill="url(#orbC)"/>
    <path d="${wave1} L 3820 ${H + 200} L -150 ${H + 200} Z" fill="url(#silkBlue)" opacity=".32" filter="url(#glassBlur)"/>
    <path d="${wave2} L 3820 ${H + 200} L -150 ${H + 200} Z" fill="url(#silkDeep)" opacity=".26" filter="url(#glassBlur)"/>
    <path d="${wave1}" fill="none" stroke="#FFFFFF" stroke-width="5" stroke-opacity=".68" filter="url(#softRim)"/>
    <path d="${wave1}" fill="none" stroke="#FFFFFF" stroke-width="2" stroke-opacity=".55"/>
    <path d="${wave2}" fill="none" stroke="#FFFFFF" stroke-width="3" stroke-opacity=".42" filter="url(#softRim)"/>
    <rect width="${W}" height="${H}" fill="url(#topVeil)"/>
    <path d="${flowerL}" fill="#7693C9" opacity=".055"/>
    <path d="${flowerR}" fill="#6C84B5" opacity=".055"/>
  `);
}

function windowShadowSvg(x, y, w, h, r) {
  const path = `M ${x + r} ${y} H ${x + w - r} A ${r} ${r} 0 0 1 ${x + w} ${y + r} V ${y + h + 80} H ${x} V ${y + r} A ${r} ${r} 0 0 1 ${x + r} ${y} Z`;
  return svg(`
    <defs>
      <filter id="sh1" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="58"/></filter>
      <filter id="sh2" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="24"/></filter>
      <filter id="sh3" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="8"/></filter>
    </defs>
    <g transform="translate(0 28)" opacity=".26" filter="url(#sh1)"><path d="${path}" fill="#182A5E"/></g>
    <g transform="translate(0 14)" opacity=".24" filter="url(#sh2)"><path d="${path}" fill="#111E42"/></g>
    <g transform="translate(0 4)" opacity=".20" filter="url(#sh3)"><path d="${path}" fill="#0A122B"/></g>
  `);
}

function windowFrameSvg(x, y, w, h, r) {
  const outer = `M ${x + r} ${y} H ${x + w - r} A ${r} ${r} 0 0 1 ${x + w} ${y + r} V ${y + h + 20} H ${x} V ${y + r} A ${r} ${r} 0 0 1 ${x + r} ${y} Z`;
  const rim = `M ${x + 1.5} ${y + h + 20} V ${y + r} A ${r - 1.5} ${r - 1.5} 0 0 1 ${x + r} ${y + 1.5} H ${x + w - r} A ${r - 1.5} ${r - 1.5} 0 0 1 ${x + w - 1.5} ${y + r} V ${y + h + 20}`;
  return svg(`
    <defs>
      <linearGradient id="winRail" x1="0" y1="0" x2="1" y2="0.4">
        <stop offset="0%" stop-color="#8A92A2"/>
        <stop offset="22%" stop-color="#3E4452"/>
        <stop offset="55%" stop-color="#232833"/>
        <stop offset="85%" stop-color="#586070"/>
        <stop offset="100%" stop-color="#848C9C"/>
      </linearGradient>
    </defs>
    <path d="${outer}" fill="url(#winRail)"/>
    <path d="${rim}" fill="none" stroke="#E2E8F5" stroke-opacity=".55" stroke-width="2"/>
  `);
}

async function renderOutfitLine(content, ptSize, color, fontfile, family, letterSpacingPt = 0) {
  const ls = letterSpacingPt ? ` letter_spacing="${Math.round(letterSpacingPt * 1024)}"` : '';
  const safe = content.replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;');
  return sharp({
    text: {
      text: `<span foreground="${color}"${ls}>${safe}</span>`,
      font: `${family} ${ptSize}`,
      fontfile,
      rgba: true,
      dpi: 96,
    },
  }).png().toBuffer({ resolveWithObject: true });
}

async function renderTextLayers() {
  const titleText = 'Desktop app';
  const subText = 'Native player for Windows & Linux.';
  if (!hasOutfitFiles) {
    return [{
      input: svg(`
        <text x="${W / 2}" y="220" text-anchor="middle" font-family="Segoe UI, Arial, sans-serif" font-size="82" font-weight="500" fill="${brandColors.ink}">${titleText}</text>
        <text x="${W / 2}" y="294" text-anchor="middle" font-family="Segoe UI, Arial, sans-serif" font-size="38" font-weight="400" fill="${brandColors.muted}">${subText}</text>
      `),
    }];
  }
  const title = await renderOutfitLine(titleText, 68, brandColors.ink, outfitTitleFont, 'Outfit Medium', -1.0);
  const sub = await renderOutfitLine(subText, 32, brandColors.muted, outfitBodyFont, 'Outfit', 0);
  return [
    { input: title.data, left: Math.round((W - title.info.width) / 2), top: 152 },
    { input: sub.data, left: Math.round((W - sub.info.width) / 2), top: 274 },
  ];
}

await mkdir(output, { recursive: true });

const winW = 3080;
const bezel = 8;
const radius = 32;
const innerRadius = radius - bezel;

const rawWin = await readFile(sourcePath);
const resized = await sharp(rawWin).resize({ width: winW }).png().toBuffer({ resolveWithObject: true });
const winH = resized.info.height;
const clippedWin = await sharp(resized.data)
  .composite([{ input: topRoundedMask(winW, winH, innerRadius), blend: 'dest-in' }])
  .png().toBuffer();

const outerW = winW + bezel * 2;
const outerH = winH + bezel;
H = outerH + 466;
const winLeft = Math.round((W - outerW) / 2);
const winTop = H - outerH;

const textLayers = await renderTextLayers();
const banner = await sharp(backdropSvg())
  .composite([
    { input: windowShadowSvg(winLeft, winTop, outerW, outerH, radius) },
    { input: windowFrameSvg(winLeft, winTop, outerW, outerH, radius) },
    { input: clippedWin, left: winLeft + bezel, top: winTop + bezel },
    ...textLayers,
  ])
  .png().toBuffer();

const finalBanner = await sharp(banner)
  .composite([{ input: roundedMask(W, H, 8), blend: 'dest-in' }])
  .png().toBuffer();

await sharp(finalBanner).resize({ width: 2400 }).webp({ quality: 92, effort: 4 })
  .toFile(resolve(output, 'desktop-studio.webp'));
await sharp(finalBanner).resize({ width: 1200 }).webp({ quality: 92, effort: 4 })
  .toFile(resolve(output, 'desktop-studio-mobile.webp'));

console.log('Rendered desktop-studio.webp and desktop-studio-mobile.webp');
