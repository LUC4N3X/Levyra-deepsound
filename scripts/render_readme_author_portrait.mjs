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

const sharp = require(sharpModule.startsWith('.') || sharpModule.includes('/') || sharpModule.includes('\\')
  ? resolve(sharpModule)
  : sharpModule);

function superellipsePath(cx, cy, r, exp = 4.2, steps = 256) {
  const pts = [];
  for (let i = 0; i < steps; i += 1) {
    const t = (2 * Math.PI * i) / steps;
    const ct = Math.cos(t);
    const st = Math.sin(t);
    const x = cx + Math.sign(ct) * r * (Math.abs(ct) ** (2 / exp));
    const y = cy + Math.sign(st) * r * (Math.abs(st) ** (2 / exp));
    pts.push(`${i === 0 ? 'M' : 'L'} ${x.toFixed(2)} ${y.toFixed(2)}`);
  }
  pts.push('Z');
  return pts.join(' ');
}

async function renderPortrait(rawPhoto, light) {
  const S = 640;
  const half = S / 2;
  const outerD = superellipsePath(half, half, half - 2, 4.2);
  const rimD = superellipsePath(half, half, half - 3.5, 4.2);
  const stroke = light ? '#2B1624' : '#FFFFFF';
  const strokeOp = light ? '0.12' : '0.18';

  const resized = await sharp(rawPhoto)
    .resize({ width: S, height: S, fit: 'cover', kernel: sharp.kernel.lanczos3 })
    .sharpen({ sigma: 0.55 })
    .ensureAlpha()
    .png()
    .toBuffer();

  const mask = Buffer.from(
    `<svg xmlns="http://www.w3.org/2000/svg" width="${S}" height="${S}" viewBox="0 0 ${S} ${S}"><path d="${outerD}" fill="white"/></svg>`,
  );
  const rim = Buffer.from(
    `<svg xmlns="http://www.w3.org/2000/svg" width="${S}" height="${S}" viewBox="0 0 ${S} ${S}"><path d="${rimD}" fill="none" stroke="${stroke}" stroke-opacity="${strokeOp}" stroke-width="3"/></svg>`,
  );

  const clipped = await sharp(resized)
    .composite([{ input: mask, blend: 'dest-in' }])
    .png()
    .toBuffer();

  return sharp(clipped)
    .composite([{ input: rim }])
    .webp({ quality: 94, alphaQuality: 100, effort: 5 })
    .toBuffer();
}

await mkdir(output, { recursive: true });
const rawPhoto = await readFile(sourcePath);

const [darkBuf, lightBuf] = await Promise.all([
  renderPortrait(rawPhoto, false),
  renderPortrait(rawPhoto, true),
]);

await Promise.all([
  sharp(darkBuf).toFile(resolve(output, 'levyra-author-portrait.webp')),
  sharp(lightBuf).toFile(resolve(output, 'levyra-author-portrait-light.webp')),
]);

console.log('Rendered levyra-author-portrait (dark/light M3 Expressive squircle).');
