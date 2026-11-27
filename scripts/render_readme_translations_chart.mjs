import { mkdir, readFile, readdir, writeFile } from 'node:fs/promises';
import { join, resolve } from 'node:path';

const output = resolve('docs/assets');
const KT_DIR = 'app/src/main/java/com/luc4n3x/levyra/ui/i18n';
const BASE_XML = 'app/src/main/res/values/strings.xml';
const LOCALE_CONFIG = 'app/src/main/res/xml/locales_config.xml';
const DESKTOP_KT = 'desktop/app/src/main/kotlin/com/luc4n3x/levyra/desktop/app/ui/i18n/Strings.kt';

const FONT = "Roboto,system-ui,-apple-system,'Segoe UI',Helvetica,Arial,sans-serif";

const SERIES = [
  { id: 'Core Labels', dark: '#89B4FA', light: '#4E82EE' },
  { id: 'Actions & Controls', dark: '#7AE2C3', light: '#32B28D' },
  { id: 'Headers & Tabs', dark: '#BDA2FA', light: '#886AEF' },
  { id: 'Guides & Subtitles', dark: '#7AD4F6', light: '#36B0DA' },
  { id: 'States & Badges', dark: '#F49EC4', light: '#E16B9D' },
  { id: 'Presets & Options', dark: '#FAD07A', light: '#E9AA34' },
  { id: 'Desktop App', dark: '#FBA686', light: '#EF7D53' },
];

const escapeXml = value => String(value)
  .replaceAll('&', '&amp;')
  .replaceAll('<', '&lt;')
  .replaceAll('>', '&gt;')
  .replaceAll('"', '&quot;')
  .replaceAll("'", '&apos;');

function domainFor(file, key) {
  const s = `${file} ${key}`.toLowerCase();
  if (/skin|prism|atelier|qobuz|spotify.*skin|youtubemusic.*skin|theme|glass|colorpicker|miniplayerstudio|navigationstudio|widgetstudio|visualcraft|dockmorph|floatingnav/.test(s)) return 'Skins & Studios';
  if (/lyric|storypost|sharemoment|trackcredit/.test(s)) return 'Lyrics & Share';
  if (/eq\b|autoeq|parametric|crossfade|replaygain|audio|smartlistening|smartaudio|normalization|mixlab|sectionloop|pitch|bitrate|flac/.test(s)) return 'Audio & DSP';
  if (/home|explore|discover|chart|genre|mood|fresh|curated|foryou|newrelease|trending|dailytop|ambient/.test(s)) return 'Home & Explore';
  if (/player|nowplaying|queue|sleeptimer|playback|video|keepscreen|gesture|deck/.test(s)) return 'Player & Queue';
  if (/download|offline|vault|backup|storage/.test(s)) return 'Offline & Vault';
  if (/playlist|import|bulklink|likeall/.test(s)) return 'Playlists';
  if (/library|local|album|artist|sort|tag|collection|personal/.test(s)) return 'Library & Files';
  if (/insight|rewind|recap|wrapped|pulse|receipt|taste|stat|lastfm|listenbrainz/.test(s)) return 'Stats & Rewind';
  if (/jam|unison|livechat|radio/.test(s)) return 'Unison & Radio';
  if (/network|byedpi|proxy|update|release|diagnostic|account|youtube|login|auth/.test(s)) return 'Network & Sync';
  if (/podcast|recognition|news/.test(s)) return 'Podcasts & ID';
  if (/onboard|languagepicker|musicregion/.test(s)) return 'Onboarding';
  return 'Settings & Core';
}

function layerFor(key) {
  const k = key.toLowerCase();
  if (/title|header|headline|heading|section|tab\b|screen|page|hub/.test(k)) return 'Headers & Tabs';
  if (/desc|subtitle|summary|body|hint|helper|message|guide|intro|about|note|explain|tip|detail|reason|warning|notice/.test(k)) return 'Guides & Subtitles';
  if (/action|button|btn|cta|apply|save|cancel|delete|remove|add|open|close|start|stop|toggle|enable|disable|select|choose|share|export|import|retry|reset|play|pause|skip|next|prev|copy|edit|create|clear|refresh|search|connect|disconnect/.test(k)) return 'Actions & Controls';
  if (/dialog|sheet|picker|menu|confirm|prompt|preset|option|mode|style|filter|sort|quality|format|theme|skin|profile|source|folder|region/.test(k)) return 'Presets & Options';
  if (/error|empty|loading|status|state|failed|success|complete|ready|active|offline|online|unavailable|count|badge|tag|none|unknown|default/.test(k)) return 'States & Badges';
  return 'Core Labels';
}

async function collectDataset() {
  const [ktFiles, baseXml, localeXml, deskKt] = await Promise.all([
    readdir(KT_DIR),
    readFile(BASE_XML, 'utf8'),
    readFile(LOCALE_CONFIG, 'utf8'),
    readFile(DESKTOP_KT, 'utf8'),
  ]);

  const localeCount = Array.from(localeXml.matchAll(/<locale\s+android:name="([^"]+)"\s*\/>/g)).length;
  const domains = {};
  const seriesTotals = Object.fromEntries(SERIES.map(s => [s.id, 0]));
  const seen = new Set();

  const record = (domain, layer) => {
    domains[domain] ??= { label: domain, total: 0, layers: {} };
    domains[domain].total += 1;
    domains[domain].layers[layer] = (domains[domain].layers[layer] || 0) + 1;
    seriesTotals[layer] = (seriesTotals[layer] || 0) + 1;
  };

  for (const f of ktFiles.filter(name => name.endsWith('.kt'))) {
    const src = await readFile(join(KT_DIR, f), 'utf8');
    const keys = [
      ...Array.from(src.matchAll(/get\(\)\s*=\s*value\("([^"]+)"\)/g), m => m[1]),
      ...Array.from(src.matchAll(/val\s+([A-Za-z0-9_]+)\s*:\s*String\b/g), m => m[1]),
    ];
    for (const k of keys) {
      const id = `${f}:${k}`;
      if (seen.has(id)) continue;
      seen.add(id);
      record(domainFor(f, k), layerFor(k));
    }
  }

  for (const m of baseXml.matchAll(/<string\s+name="([^"]+)"([^>]*)>/g)) {
    if (m[2].includes('translatable="false"')) continue;
    record(domainFor('strings.xml', m[1]), layerFor(m[1]));
  }

  for (const m of deskKt.matchAll(/val\s+([A-Za-z0-9_]+)\s*:\s*String\b/g)) {
    record(domainFor('Desktop', m[1]), 'Desktop App');
  }

  const columns = Object.values(domains).sort((a, b) => b.total - a.total);
  const grandTotal = columns.reduce((sum, c) => sum + c.total, 0);
  return { columns, seriesTotals, grandTotal, localeCount };
}

function roundedCornersPath(x, y, w, h, [tl, tr, br, bl]) {
  const x1 = x + w;
  const y1 = y + h;
  const rtl = Math.min(tl, w / 2, h / 2);
  const rtr = Math.min(tr, w / 2, h / 2);
  const rbr = Math.min(br, w / 2, h / 2);
  const rbl = Math.min(bl, w / 2, h / 2);
  return [
    `M ${(x + rtl).toFixed(2)} ${y.toFixed(2)}`,
    `H ${(x1 - rtr).toFixed(2)}`,
    `A ${rtr.toFixed(2)} ${rtr.toFixed(2)} 0 0 1 ${x1.toFixed(2)} ${(y + rtr).toFixed(2)}`,
    `V ${(y1 - rbr).toFixed(2)}`,
    `A ${rbr.toFixed(2)} ${rbr.toFixed(2)} 0 0 1 ${(x1 - rbr).toFixed(2)} ${y1.toFixed(2)}`,
    `H ${(x + rbl).toFixed(2)}`,
    `A ${rbl.toFixed(2)} ${rbl.toFixed(2)} 0 0 1 ${x.toFixed(2)} ${(y1 - rbl).toFixed(2)}`,
    `V ${(y + rtl).toFixed(2)}`,
    `A ${rtl.toFixed(2)} ${rtl.toFixed(2)} 0 0 1 ${(x + rtl).toFixed(2)} ${y.toFixed(2)} Z`,
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

function buildColumnsMarkup({ columns, plotX, baselineY, plotH, maxVal, barW, barGap, light, countSize, labelSize }) {
  const parts = [];
  columns.forEach((col, colIdx) => {
    const x = plotX + colIdx * (barW + barGap);
    const totalH = Math.max(7, (col.total / maxVal) * plotH);
    let active = SERIES
      .map(s => ({ id: s.id, color: light ? s.light : s.dark, val: col.layers[s.id] || 0 }))
      .filter(s => s.val > 0);

    const minPx = 3.0;
    while (active.length > 1) {
      const segGapTry = Math.min(2.2, totalH / (active.length * 4));
      const avail = totalH - (active.length - 1) * segGapTry;
      const smallest = Math.min(...active.map(s => (s.val / col.total) * avail));
      if (smallest >= minPx || active.length <= 2) break;
      let minIdx = 0;
      for (let i = 1; i < active.length; i += 1) {
        if (active[i].val < active[minIdx].val) minIdx = i;
      }
      const targetIdx = minIdx > 0 ? minIdx - 1 : 1;
      active[targetIdx] = { ...active[targetIdx], val: active[targetIdx].val + active[minIdx].val };
      active.splice(minIdx, 1);
    }

    const segGap = active.length > 1 ? Math.min(2.2, Math.max(1.2, totalH * 0.04)) : 0;
    const totalGap = Math.max(0, (active.length - 1) * segGap);
    const drawableH = Math.max(active.length * 2.2, totalH - totalGap);
    const sumActive = active.reduce((s, a) => s + a.val, 0);
    const heights = active.map(s => (s.val / sumActive) * drawableH);

    let curBottom = baselineY;
    active.forEach((seg, segIdx) => {
      const h = heights[segIdx];
      const y = curBottom - h;
      const isBottom = segIdx === 0;
      const isTop = segIdx === active.length - 1;
      const rCap = Math.min(11, barW * 0.34, h * 0.85);
      const rBase = Math.min(7, barW * 0.22, h * 0.75);
      const rMid = Math.min(3.5, h * 0.45);
      let corners = [rMid, rMid, rMid, rMid];
      if (isBottom && isTop) corners = [rCap, rCap, rBase, rBase];
      else if (isTop) corners = [rCap, rCap, rMid, rMid];
      else if (isBottom) corners = [rMid, rMid, rBase, rBase];

      parts.push(`  <path d="${roundedCornersPath(x, y, barW, h, corners)}" fill="${seg.color}"/>`);
      curBottom = y - segGap;
    });

    const cx = (x + barW / 2).toFixed(1);
    const topY = (baselineY - totalH - 8).toFixed(1);
    parts.push(`  <text x="${cx}" y="${topY}" text-anchor="middle" class="bar-val" font-size="${countSize}">${col.total}</text>`);
    parts.push(`  <text transform="translate(${(x + barW / 2 + 4).toFixed(1)} ${baselineY + 16}) rotate(-38)" text-anchor="end" class="x-label" font-size="${labelSize}">${escapeXml(col.label)}</text>`);
  });
  return parts.join('\n');
}

function makeChartSvg(data, { light = false, mobile = false }) {
  const ink = light ? '#161F33' : '#EEF3FC';
  const muted = light ? '#546584' : '#96A7C4';
  const axisInk = light ? '#283856' : '#CBD6EC';
  const gridColor = light ? '#DCE6F7' : '#222D42';
  const baselineColor = light ? '#94A8CC' : '#3B4C6B';
  const chipBg = light ? '#DCE8FF' : '#D2E3FF';
  const chipInk = '#132C54';
  const totalLocalizedEntries = Math.round((data.grandTotal * data.localeCount) / 1000);
  const aria = `Levyra localization chart: ${data.grandTotal} UI strings per language across ${data.localeCount} languages`;

  const legendReverse = [...SERIES].reverse();

  if (mobile) {
    const W = 732;
    const H = 740;
    const pillPath = roundedCornersPath(24, 14, 276, 32, [16, 7, 16, 7]);
    const pillRosette = scallopPath(43, 30, 10, 12.6, 10);

    const plotX = 74;
    const baselineY = 468;
    const plotH = 290;
    const maxVal = 600;
    const barW = 28;
    const barGap = 17.5;
    const plotRight = plotX + data.columns.length * (barW + barGap) - barGap + 10;

    const gridTicks = [0, 200, 400, 600];
    const gridMarkup = gridTicks.map(v => {
      const y = (baselineY - (v / maxVal) * plotH).toFixed(1);
      const stroke = v === 0 ? baselineColor : gridColor;
      const sw = v === 0 ? '1.6' : '1';
      return `  <line x1="${plotX - 8}" y1="${y}" x2="${plotRight.toFixed(1)}" y2="${y}" stroke="${stroke}" stroke-width="${sw}"/>
  <text x="${plotX - 14}" y="${(Number(y) + 4).toFixed(1)}" text-anchor="end" class="y-tick">${v}</text>`;
    }).join('\n');

    const barsMarkup = buildColumnsMarkup({
      columns: data.columns,
      plotX,
      baselineY,
      plotH,
      maxVal,
      barW,
      barGap,
      light,
      countSize: 10.5,
      labelSize: 11,
    });

    const legStartY = 588;
    const legendMarkup = legendReverse.map((s, idx) => {
      const col = idx % 2;
      const row = Math.floor(idx / 2);
      const sx = 36 + col * 336;
      const sy = legStartY + row * 34;
      const swColor = light ? s.light : s.dark;
      return `  <path d="${roundedCornersPath(sx, sy, 18, 18, [8, 3.5, 8, 3.5])}" fill="${swColor}"/>
  <text x="${sx + 28}" y="${sy + 13.5}" class="leg-label">${escapeXml(s.id)}</text>
  <text x="${sx + 292}" y="${sy + 13.5}" text-anchor="end" class="leg-val">${data.seriesTotals[s.id] || 0}</text>`;
    }).join('\n');

    return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="${escapeXml(aria)}">
  <title>${escapeXml(aria)}</title>
  <style>
    text { font-family: ${FONT}; }
    .pill-txt { fill: ${chipInk}; font-size: 10.5px; font-weight: 700; letter-spacing: .8px; }
    .title { fill: ${ink}; font-size: 26px; font-weight: 700; letter-spacing: -.4px; }
    .sub { fill: ${muted}; font-size: 13px; font-weight: 500; }
    .y-tick { fill: ${muted}; font-size: 11px; font-weight: 600; }
    .bar-val { fill: ${axisInk}; font-weight: 700; }
    .x-label { fill: ${axisInk}; font-weight: 600; }
    .leg-label { fill: ${ink}; font-size: 12.5px; font-weight: 550; }
    .leg-val { fill: ${muted}; font-size: 12px; font-weight: 700; }
  </style>
  <path d="${pillPath}" fill="${chipBg}"/>
  <path d="${pillRosette}" fill="${chipInk}" fill-opacity=".12"/>
  <g transform="translate(35.8 22.8) scale(0.60)" style="color:${chipInk}">
    <path fill="currentColor" d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95a15.65 15.65 0 00-1.38-3.56A8.03 8.03 0 0118.92 8zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2 0 .68.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56A7.987 7.987 0 015.08 16zm2.95-8H5.08a7.987 7.987 0 014.33-3.56A15.65 15.65 0 008.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2 0-.68.07-1.35.16-2h4.68c.09.65.16 1.32.16 2 0 .68-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95a8.03 8.03 0 01-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2 0-.68-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z"/>
  </g>
  <text x="61" y="34" class="pill-txt">${data.localeCount} LANGUAGES · 100% COMPLETE</text>
  <text x="24" y="80" class="title">Localized strings by feature</text>
  <text x="24" y="106" class="sub">${data.grandTotal.toLocaleString('en-US')} UI strings per language · ${totalLocalizedEntries}k+ translated entries</text>
${gridMarkup}
${barsMarkup}
${legendMarkup}
</svg>
`;
  }

  const W = 1090;
  const H = 510;
  const pillPath = roundedCornersPath(20, 12, 278, 32, [16, 7, 16, 7]);
  const pillRosette = scallopPath(39, 28, 10, 12.6, 10);

  const plotX = 74;
  const baselineY = 396;
  const plotH = 264;
  const maxVal = 600;
  const barW = 34;
  const barGap = 19.5;
  const plotRight = plotX + data.columns.length * (barW + barGap) - barGap + 12;

  const gridTicks = [0, 200, 400, 600];
  const gridMarkup = gridTicks.map(v => {
    const y = (baselineY - (v / maxVal) * plotH).toFixed(1);
    const stroke = v === 0 ? baselineColor : gridColor;
    const sw = v === 0 ? '1.6' : '1';
    return `  <line x1="${plotX - 8}" y1="${y}" x2="${plotRight.toFixed(1)}" y2="${y}" stroke="${stroke}" stroke-width="${sw}"/>
  <text x="${plotX - 14}" y="${(Number(y) + 4).toFixed(1)}" text-anchor="end" class="y-tick">${v}</text>`;
  }).join('\n');

  const barsMarkup = buildColumnsMarkup({
    columns: data.columns,
    plotX,
    baselineY,
    plotH,
    maxVal,
    barW,
    barGap,
    light,
    countSize: 11.5,
    labelSize: 11.8,
  });

  const legX = 842;
  const legStartY = 142;
  const legendMarkup = legendReverse.map((s, idx) => {
    const sy = legStartY + idx * 32;
    const swColor = light ? s.light : s.dark;
    return `  <path d="${roundedCornersPath(legX, sy, 18, 18, [8, 3.5, 8, 3.5])}" fill="${swColor}"/>
  <text x="${legX + 28}" y="${sy + 13.5}" class="leg-label">${escapeXml(s.id)}</text>
  <text x="${W - 18}" y="${sy + 13.5}" text-anchor="end" class="leg-val">${data.seriesTotals[s.id] || 0}</text>`;
  }).join('\n');

  return `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="${escapeXml(aria)}">
  <title>${escapeXml(aria)}</title>
  <style>
    text { font-family: ${FONT}; }
    .pill-txt { fill: ${chipInk}; font-size: 10.5px; font-weight: 700; letter-spacing: .8px; }
    .title { fill: ${ink}; font-size: 26px; font-weight: 700; letter-spacing: -.4px; }
    .sub { fill: ${muted}; font-size: 13.5px; font-weight: 500; }
    .y-tick { fill: ${muted}; font-size: 11.5px; font-weight: 600; }
    .bar-val { fill: ${axisInk}; font-weight: 700; }
    .x-label { fill: ${axisInk}; font-weight: 600; }
    .leg-head { fill: ${ink}; font-size: 13px; font-weight: 700; letter-spacing: .2px; }
    .leg-label { fill: ${ink}; font-size: 12.5px; font-weight: 550; }
    .leg-val { fill: ${muted}; font-size: 12px; font-weight: 700; }
    .script-note { fill: ${muted}; font-size: 11.5px; font-weight: 550; }
  </style>
  <path d="${pillPath}" fill="${chipBg}"/>
  <path d="${pillRosette}" fill="${chipInk}" fill-opacity=".12"/>
  <g transform="translate(31.8 20.8) scale(0.60)" style="color:${chipInk}">
    <path fill="currentColor" d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zm6.93 6h-2.95a15.65 15.65 0 00-1.38-3.56A8.03 8.03 0 0118.92 8zM12 4.04c.83 1.2 1.48 2.53 1.91 3.96h-3.82c.43-1.43 1.08-2.76 1.91-3.96zM4.26 14C4.1 13.36 4 12.69 4 12s.1-1.36.26-2h3.38c-.08.66-.14 1.32-.14 2 0 .68.06 1.34.14 2H4.26zm.82 2h2.95c.32 1.25.78 2.45 1.38 3.56A7.987 7.987 0 015.08 16zm2.95-8H5.08a7.987 7.987 0 014.33-3.56A15.65 15.65 0 008.03 8zM12 19.96c-.83-1.2-1.48-2.53-1.91-3.96h3.82c-.43 1.43-1.08 2.76-1.91 3.96zM14.34 14H9.66c-.09-.66-.16-1.32-.16-2 0-.68.07-1.35.16-2h4.68c.09.65.16 1.32.16 2 0 .68-.07 1.34-.16 2zm.25 5.56c.6-1.11 1.06-2.31 1.38-3.56h2.95a8.03 8.03 0 01-4.33 3.56zM16.36 14c.08-.66.14-1.32.14-2 0-.68-.06-1.34-.14-2h3.38c.16.64.26 1.31.26 2s-.1 1.36-.26 2h-3.38z"/>
  </g>
  <text x="57" y="32" class="pill-txt">${data.localeCount} LANGUAGES · 100% COMPLETE</text>
  <text x="20" y="76" class="title">Localized strings by feature</text>
  <text x="366" y="75" class="sub">${data.grandTotal.toLocaleString('en-US')} UI strings per language (${totalLocalizedEntries}k+ translated entries)</text>
${gridMarkup}
${barsMarkup}
  <text x="${legX}" y="124" class="leg-head">UI layers</text>
${legendMarkup}
  <text x="${legX}" y="394" class="script-note">Latin 26 · Cyrillic &amp; Greek 6</text>
  <text x="${legX}" y="414" class="script-note">CJK 4 · RTL 3 · Indic &amp; Thai 3</text>
</svg>
`;
}

await mkdir(output, { recursive: true });
const dataset = await collectDataset();

await Promise.all([
  writeFile(resolve(output, 'levyra-translations-chart.svg'), makeChartSvg(dataset, { light: false, mobile: false }), 'utf8'),
  writeFile(resolve(output, 'levyra-translations-chart-light.svg'), makeChartSvg(dataset, { light: true, mobile: false }), 'utf8'),
  writeFile(resolve(output, 'levyra-translations-chart-mobile.svg'), makeChartSvg(dataset, { light: false, mobile: true }), 'utf8'),
  writeFile(resolve(output, 'levyra-translations-chart-mobile-light.svg'), makeChartSvg(dataset, { light: true, mobile: true }), 'utf8'),
]);

console.log(`Generated frameless M3 Expressive translations chart SVGs (${dataset.grandTotal} strings x ${dataset.localeCount} langs).`);
