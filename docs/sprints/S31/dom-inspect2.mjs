import { chromium } from 'playwright';
(async () => {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, colorScheme: 'dark' });
  const page = await ctx.newPage();
  await page.goto('http://localhost:7778/', { waitUntil: 'networkidle', timeout: 30000 });
  await page.waitForTimeout(1500);

  const out = await page.evaluate(() => {
    const sel = (s) => {
      const el = document.querySelector(s);
      if (!el) return `[NOT FOUND] ${s}`;
      const cs = getComputedStyle(el);
      return `${s}\n  color: ${cs.color}\n  bg: ${cs.backgroundColor}\n  fs: ${cs.fontSize} fw: ${cs.fontWeight}\n  text: "${el.textContent?.trim().slice(0, 30)}"\n  cls: "${el.className}"`;
    };
    return [
      sel('.home-featured__rank'),
      sel('.home-featured__cat'),
      sel('.home-featured__name'),
      sel('.home-featured__row'),
      sel('.home-hero__tab.is-active'),
      sel('.home-hero__tab'),
      sel('.home-stats__num'),
      sel('.home-hot__card-name'),
      sel('.home-hot__card'),
      sel('.home-stats__cta-btn'),
      sel('.home-hero__search-card'),
      sel('.home-hero__input-row'),
      sel('a'),
      sel('[class*="link"]'),
    ].join('\n\n');
  });
  console.log(out);
  await browser.close();
})();