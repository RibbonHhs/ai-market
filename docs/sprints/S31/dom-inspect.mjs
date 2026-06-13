// DOM inspector — run once to learn real selectors
import { chromium } from 'playwright';

(async () => {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, colorScheme: 'dark' });
  const page = await ctx.newPage();
  await page.goto('http://localhost:7778/', { waitUntil: 'networkidle', timeout: 30000 });
  await page.waitForTimeout(1500);

  const info = await page.evaluate(() => {
    const out = {};
    // Find all elements with class starting with home- or __
    const all = document.querySelectorAll('*');
    const seen = new Set();
    for (const el of all) {
      const cls = el.className;
      if (typeof cls !== 'string') continue;
      for (const c of cls.split(/\s+/)) {
        if (c.startsWith('home-') || c.includes('__')) {
          if (!seen.has(c)) {
            seen.add(c);
            out[c] = (out[c] || 0) + 1;
          }
        }
      }
    }
    return out;
  });
  console.log('=== ALL HOME/__ CLASSES ===');
  console.log(Object.keys(info).sort().join('\n'));
  console.log('=== SAMPLE TARGET ELEMENTS ===');
  // Sample computed style for key targets
  const samples = await page.evaluate(() => {
    const sel = (s) => {
      const el = document.querySelector(s);
      if (!el) return `[NOT FOUND] ${s}`;
      const cs = getComputedStyle(el);
      return `${s}\n  color: ${cs.color}\n  bg: ${cs.backgroundColor}\n  bgImage: ${cs.backgroundImage.slice(0, 80)}\n  fs: ${cs.fontSize} fw: ${cs.fontWeight}\n  text: "${el.textContent?.trim().slice(0, 30)}"`;
    };
    return [
      sel('h1'),
      sel('h2'),
      sel('p'),
      sel('button'),
      sel('input'),
      sel('[class*="search-btn"]'),
      sel('[class*="card-rate"]'),
      sel('[class*="card-title"]'),
      sel('[class*="rate-num"]'),
      sel('[class*="rank"]'),
      sel('[class*="sidebar"]'),
      sel('[class*="cta-btn"]'),
      sel('[class*="stats__num"]'),
      sel('[class*="hero__"]'),
    ].join('\n\n');
  });
  console.log(samples);

  await browser.close();
})();