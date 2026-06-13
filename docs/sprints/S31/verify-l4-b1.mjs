// Verify L4 and B1 specifically — print exact fg/bg/ratio
import { chromium } from 'playwright';
(async () => {
  const browser = await chromium.launch();

  // LIGHT
  {
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, colorScheme: 'light' });
    const page = await ctx.newPage();
    await page.addInitScript(() => {
      document.addEventListener('DOMContentLoaded', () => document.documentElement.setAttribute('data-theme', 'light'));
    });
    await page.goto('http://localhost:7778/', { waitUntil: 'networkidle' });
    await page.waitForTimeout(1500);
    const r = await page.evaluate(() => {
      const el = document.querySelector('.home-hot__card-rate');
      const cs = getComputedStyle(el);
      let node = el;
      let bg = null;
      while (node) {
        const bcs = getComputedStyle(node);
        const m = bcs.backgroundColor.match(/^rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)$/);
        if (m && parseFloat(m[4] || '1') > 0.01) {
          bg = { r: +m[1], g: +m[2], b: +m[3] };
          break;
        }
        node = node.parentElement;
      }
      const m = cs.color.match(/^rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([\d.]+))?\)$/);
      const fg = { r: +m[1], g: +m[2], b: +m[3], a: m[4] !== undefined ? +m[4] : 1 };
      return { cs: { color: cs.color, bg: cs.backgroundColor, fs: cs.fontSize, fw: cs.fontWeight }, fg, bg, text: el.textContent.trim() };
    });
    console.log('L4 — light card-rate:');
    console.log('  computed color =', r.cs.color);
    console.log('  effective bg =', r.bg);
    console.log('  text =', r.text);
    await ctx.close();
  }

  // DARK
  {
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, colorScheme: 'dark' });
    const page = await ctx.newPage();
    await page.goto('http://localhost:7778/', { waitUntil: 'networkidle' });
    await page.waitForTimeout(1500);
    const r = await page.evaluate(() => {
      const el = document.querySelector('.home-hero__search-btn');
      const cs = getComputedStyle(el);
      return { color: cs.color, bgImage: cs.backgroundImage, fs: cs.fontSize, fw: cs.fontWeight, text: el.textContent.trim() };
    });
    console.log('\nB1 — dark search-btn:');
    console.log('  color =', r.color);
    console.log('  bgImage =', r.bgImage);
    console.log('  font =', r.fs, r.fw);
    console.log('  text =', r.text);
    await ctx.close();
  }

  // Check --warning CSS variable value in both themes
  {
    const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, colorScheme: 'dark' });
    const page = await ctx.newPage();
    await page.goto('http://localhost:7778/', { waitUntil: 'networkidle' });
    await page.waitForTimeout(800);
    const dark = await page.evaluate(() => getComputedStyle(document.documentElement).getPropertyValue('--warning').trim());
    await ctx.close();
    const ctx2 = await browser.newContext({ viewport: { width: 1440, height: 900 }, colorScheme: 'light' });
    const page2 = await ctx2.newPage();
    await page2.addInitScript(() => {
      document.addEventListener('DOMContentLoaded', () => document.documentElement.setAttribute('data-theme', 'light'));
    });
    await page2.goto('http://localhost:7778/', { waitUntil: 'networkidle' });
    await page2.waitForTimeout(800);
    const light = await page2.evaluate(() => getComputedStyle(document.documentElement).getPropertyValue('--warning').trim());
    await ctx2.close();
    console.log('\n--warning token:');
    console.log('  dark =', dark);
    console.log('  light =', light);
  }

  await browser.close();
})();