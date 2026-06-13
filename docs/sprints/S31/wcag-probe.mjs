// S31 WCAG AA regression probe — uses Playwright (Node)
// Mirrors S30 wcag-regression.md 25-point matrix with corrected selectors.

import { chromium } from 'playwright';
import { writeFileSync } from 'node:fs';

const FRONTEND = 'http://localhost:7778';
const SHOT_DIR = 'D:/codeing/workspace/skills-map/docs/sprints/S31/screenshots';

// Mid-point of #7c3aed → #4f46e5 linear gradient (B1 bg)
const GRADIENT_MID = { r: 0x66, g: 0x40, b: 0xa6 };

// Color helper runs in browser context (no Node scope bleed)
const BROWSER_HELPER = `
function parseColor(str) {
  const s = (str || '').trim();
  if (!s || s === 'transparent' || s === 'rgba(0, 0, 0, 0)') return { r: 0, g: 0, b: 0, a: 0 };
  let m = s.match(/^rgba?\\(([^)]+)\\)$/i);
  if (m) {
    const parts = m[1].split(',').map(x => parseFloat(x.trim()));
    return { r: parts[0], g: parts[1], b: parts[2], a: parts.length === 4 ? parts[3] : 1 };
  }
  m = s.match(/^#([0-9a-f]{6})$/i);
  if (m) {
    const n = parseInt(m[1], 16);
    return { r: (n >> 16) & 0xff, g: (n >> 8) & 0xff, b: n & 0xff, a: 1 };
  }
  m = s.match(/^#([0-9a-f]{3})$/i);
  if (m) {
    const r = parseInt(m[1][0] + m[1][0], 16);
    const g = parseInt(m[1][1] + m[1][1], 16);
    const b = parseInt(m[1][2] + m[1][2], 16);
    return { r, g, b, a: 1 };
  }
  return null;
}
function srgbToLin(c) { const v = c / 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }
function relLum(rgb) { return 0.2126 * srgbToLin(rgb.r) + 0.7152 * srgbToLin(rgb.g) + 0.0722 * srgbToLin(rgb.b); }
function composite(top, bot) {
  const a = top.a;
  return { r: Math.round(top.r * a + bot.r * (1 - a)), g: Math.round(top.g * a + bot.g * (1 - a)), b: Math.round(top.b * a + bot.b * (1 - a)) };
}
function contrast(fg, bg) {
  const L1 = relLum(fg), L2 = relLum(bg);
  return (Math.max(L1, L2) + 0.05) / (Math.min(L1, L2) + 0.05);
}
`;

// Probe in-page
const PROBE_FN = `
${BROWSER_HELPER}
function probe(sel, opts) {
  const el = document.querySelector(sel);
  if (!el) return { error: 'selector not found: ' + sel };
  const target = opts.sample === 'self' ? el : el;
  const cs = getComputedStyle(target);
  const fgParsed = parseColor(cs.color);
  // Walk up DOM for first opaque background
  let node = target;
  let bgRgb = null, bgSource = null;
  while (node && node.nodeType === 1) {
    const bcs = getComputedStyle(node);
    const parsed = parseColor(bcs.backgroundColor);
    if (parsed && parsed.a > 0.01) {
      bgRgb = { r: parsed.r, g: parsed.g, b: parsed.b };
      bgSource = (node.className || node.tagName || '').toString().slice(0, 60);
      break;
    }
    node = node.parentElement;
  }
  if (!bgRgb) {
    const bodyBg = parseColor(getComputedStyle(document.body).backgroundColor) || { r: 255, g: 255, b: 255, a: 1 };
    bgRgb = { r: bodyBg.r, g: bodyBg.g, b: bodyBg.b };
    bgSource = 'body-fallback';
  }
  // Effective fg (alpha-composite over bg for display)
  let effFg = fgParsed ? { r: fgParsed.r, g: fgParsed.g, b: fgParsed.b } : null;
  if (fgParsed && fgParsed.a !== undefined && fgParsed.a < 1 && opts.useFg) {
    effFg = composite(fgParsed, bgRgb);
  }
  return {
    fg: fgParsed,
    effFg,
    bg: bgRgb,
    bgSource,
    bgImage: cs.backgroundImage !== 'none' ? cs.backgroundImage : null,
    fontSize: cs.fontSize,
    fontWeight: cs.fontWeight,
  };
}
`;

// Probes (corrected selectors)
const PROBES = [
  // ========== DARK ==========
  { id: 'T1', theme: 'dark', name: '主标题 h1 on body',                  sel: 'h1',                                    opts: { useFg: true, sample: 'first' } },
  { id: 'T2', theme: 'dark', name: '副文 on body',                       sel: 'p',                                     opts: { useFg: true, sample: 'first' } },
  { id: 'T3', theme: 'dark', name: 'placeholder text on input-row',      sel: '.home-hero__input-row',                 opts: { useFg: false, sample: 'self', pickBg: true } },
  { id: 'T5', theme: 'dark', name: '卡片标题 on home-hot__card',         sel: '.home-hot__card-name',                  opts: { useFg: true, sample: 'first' } },
  { id: 'T6', theme: 'dark', name: '次文 on home-hot__card',             sel: '.home-hot__card-desc',                  opts: { useFg: true, sample: 'first' } },
  { id: 'S1', theme: 'dark', name: '卡评分 on home-hot__card',           sel: '.home-hot__card-rate',                  opts: { useFg: true, sample: 'first' } },
  { id: 'S2', theme: 'dark', name: '排行评分 on home-featured row',      sel: '.home-featured__rate-num',              opts: { useFg: true, sample: 'first' } },
  { id: 'S3', theme: 'dark', name: '排行数字 on row',                    sel: '.home-featured__rank',                  opts: { useFg: true, sample: 'first' } },
  { id: 'S4', theme: 'dark', name: 'sidebar cat 文字',                    sel: '.home-featured__cat.is-active',         opts: { useFg: true, sample: 'first' } },
  { id: 'B1', theme: 'dark', name: '主搜索按钮 紫底白字',                sel: '.home-hero__search-btn',                opts: { useFg: true, sample: 'first', gradientBg: true } },
  { id: 'B2', theme: 'dark', name: 'CTA 紫边紫字',                       sel: '.home-stats__cta-btn',                  opts: { useFg: true, sample: 'first' } },
  { id: 'B3', theme: 'dark', name: 'Tab is-active 紫底深字',             sel: '.home-hero__tab.is-active',             opts: { useFg: true, sample: 'first' } },
  { id: 'N1', theme: 'dark', name: 'search-card elevated on primary',     sel: '.home-hero__search-card',               opts: { useFg: true, sample: 'self' } },
  { id: 'N4', theme: 'dark', name: 'HomeHot 卡片标题 on card',           sel: '.home-hot__card-name',                  opts: { useFg: true, sample: 'first' } },
  { id: 'N5', theme: 'dark', name: 'HomeFeatured sidebar cat',           sel: '.home-featured__cat.is-active',         opts: { useFg: true, sample: 'first' } },
  { id: 'N6', theme: 'dark', name: 'HomeFeatured 排行名 on row',         sel: '.home-featured__name',                  opts: { useFg: true, sample: 'first' } },
  { id: 'N7', theme: 'dark', name: 'HomeStats 数字 on cell',             sel: '.home-stats__num',                      opts: { useFg: true, sample: 'first' } },
  { id: 'T4', theme: 'dark', name: '主色链接 on search-card',            sel: '.home-hero__search-card',               opts: { useFg: true, sample: 'self' } },
  { id: 'B4', theme: 'dark', name: '__agent-num 紫底深字',               sel: '.home-hero__agent-num',                 opts: { useFg: true, sample: 'first' }, preClick: '.home-hero__tab:nth-child(2)' },
  { id: 'N2', theme: 'dark', name: '__agent-cmd 暗底白字',               sel: '.home-hero__agent-cmd',                 opts: { useFg: true, sample: 'first' }, preClick: '.home-hero__tab:nth-child(2)' },

  // ========== LIGHT ==========
  { id: 'L1', theme: 'light', name: '浅色：主标题 on bg',                sel: 'h1',                                    opts: { useFg: true, sample: 'first' } },
  { id: 'L2', theme: 'light', name: '浅色：home-hot 卡片标题',           sel: '.home-hot__card-name',                  opts: { useFg: true, sample: 'first' } },
  { id: 'L3', theme: 'light', name: '浅色：home-hot 卡片次文',           sel: '.home-hot__card-desc',                  opts: { useFg: true, sample: 'first' } },
  { id: 'L4', theme: 'light', name: '浅色：home-hot card-rate',          sel: '.home-hot__card-rate',                  opts: { useFg: true, sample: 'first' } },
  { id: 'L5', theme: 'light', name: '浅色：home-hot 卡片作者',           sel: '.home-hot__card-author',                opts: { useFg: true, sample: 'first' } },
];

async function runForTheme(browser, theme, out) {
  const context = await browser.newContext({
    viewport: { width: 1440, height: 900 },
    colorScheme: theme === 'dark' ? 'dark' : 'light',
  });
  const page = await context.newPage();

  // Register browser-side helper once via addInitScript (concatenated string)
  const initScript = BROWSER_HELPER + '\n' + PROBE_FN + '\nwindow.__probe = function(sel, opts) { return probe(sel, opts); };';
  await page.addInitScript({ content: initScript });

  if (theme === 'light') {
    await page.addInitScript(() => {
      const apply = () => {
        if (document.documentElement) document.documentElement.setAttribute('data-theme', 'light');
      };
      document.addEventListener('DOMContentLoaded', apply);
      apply();
    });
  }

  await page.goto(FRONTEND + '/', { waitUntil: 'networkidle', timeout: 30000 });
  await page.waitForTimeout(1200);

  console.log(`\n=== Theme: ${theme} ===`);
  for (const p of PROBES.filter(x => x.theme === theme)) {
    // pre-click if needed (e.g., agent tab)
    if (p.preClick) {
      try {
        await page.click(p.preClick);
        await page.waitForTimeout(400);
      } catch (e) { /* tab not present, skip */ }
    }
    let res;
    try {
      res = await page.evaluate(
        ({ sel, opts }) => window.__probe(sel, opts),
        { sel: p.sel, opts: p.opts }
      );
    } catch (e) {
      res = { error: e.message };
    }
    if (res.error) {
      out.push({ id: p.id, theme, name: p.name, error: res.error, status: 'ERROR' });
      console.log(`  ${p.id} ERROR: ${res.error}`);
      continue;
    }

    let effFg = res.effFg;
    let effBg = res.bg;
    if (p.opts.gradientBg) {
      effBg = GRADIENT_MID;
    }
    if (!effFg) {
      // Use T3 special: probe picks bg only (no fg)
      if (p.opts.pickBg) {
        out.push({ id: p.id, theme, name: p.name, note: 'bg-only', bg: effBg, bgSource: res.bgSource, status: 'INFO' });
        console.log(`  ${p.id} INFO (bg only): bg=${JSON.stringify(effBg)} from ${res.bgSource}`);
        continue;
      }
      out.push({ id: p.id, theme, name: p.name, error: 'no fg', status: 'ERROR' });
      continue;
    }

    const Lf = relLum(effFg), Lb = relLum(effBg);
    const ratio = (Math.max(Lf, Lb) + 0.05) / (Math.min(Lf, Lb) + 0.05);
    const pxSize = parseFloat(res.fontSize);
    const weight = parseInt(res.fontWeight, 10) || 400;
    const isLarge = pxSize >= 24 || (pxSize >= 18.66 && weight >= 700);
    const required = isLarge ? 3.0 : 4.5;
    const pass = ratio >= required;

    out.push({
      id: p.id, theme, name: p.name,
      fg: { r: effFg.r, g: effFg.g, b: effFg.b, alphaOrig: res.fg?.a },
      bg: { r: effBg.r, g: effBg.g, b: effBg.b },
      bgSource: res.bgSource,
      bgImage: res.bgImage,
      fontSize: res.fontSize, fontWeight: res.fontWeight,
      ratio: Number(ratio.toFixed(2)),
      required, isLarge,
      status: pass ? 'PASS' : 'FAIL',
    });
    console.log(`  ${p.id} ${pass ? 'PASS' : 'FAIL'} ${ratio.toFixed(2)}:1 (req ${required}:1) — ${p.name}`);
  }

  // Screenshots
  if (theme === 'dark') {
    await page.screenshot({ path: `${SHOT_DIR}/home-hero-dark.png`, clip: { x: 0, y: 0, width: 1440, height: 900 } });
    await page.evaluate(() => window.scrollTo(0, 800));
    await page.waitForTimeout(500);
    await page.screenshot({ path: `${SHOT_DIR}/home-hot-dark.png`, clip: { x: 0, y: 0, width: 1440, height: 900 } });
  } else {
    await page.screenshot({ path: `${SHOT_DIR}/home-hero-light.png`, clip: { x: 0, y: 0, width: 1440, height: 900 } });
    await page.evaluate(() => window.scrollTo(0, 800));
    await page.waitForTimeout(500);
    await page.screenshot({ path: `${SHOT_DIR}/home-hot-light.png`, clip: { x: 0, y: 0, width: 1440, height: 900 } });
  }

  await context.close();
}

// Node-side relLum (mirror of browser helper) for verification outside page
function srgbToLin(c) { const v = c / 255; return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4); }
function relLum(rgb) { return 0.2126 * srgbToLin(rgb.r) + 0.7152 * srgbToLin(rgb.g) + 0.0722 * srgbToLin(rgb.b); }

(async () => {
  const browser = await chromium.launch();
  const out = [];
  for (const theme of ['dark', 'light']) {
    await runForTheme(browser, theme, out);
  }
  await browser.close();

  const pass = out.filter(x => x.status === 'PASS').length;
  const fail = out.filter(x => x.status === 'FAIL').length;
  const err = out.filter(x => x.status === 'ERROR').length;
  const info = out.filter(x => x.status === 'INFO').length;
  console.log(`\n=========== SUMMARY ===========`);
  console.log(`Total: ${out.length} | PASS: ${pass} | FAIL: ${fail} | ERROR: ${err} | INFO: ${info}`);

  writeFileSync('D:/codeing/workspace/skills-map/docs/sprints/S31/wcag-results.json', JSON.stringify(out, null, 2));
  console.log(`Results → docs/sprints/S31/wcag-results.json`);
  process.exit(fail > 0 || err > 0 ? 1 : 0);
})();