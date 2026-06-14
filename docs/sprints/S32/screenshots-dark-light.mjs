// S32 dev self-verify — capture dark + light home hero screenshots
import { chromium } from '../S31/node_modules/playwright/index.mjs';
import { fileURLToPath } from 'node:url';
import { dirname, join } from 'node:path';

const HOME = 'http://localhost:7777/';
const OUT_DIR = dirname(fileURLToPath(import.meta.url)) + '\\screenshots\\';

async function shoot(ctx, file, label) {
  const page = await ctx.newPage();
  await page.goto(HOME, { waitUntil: 'networkidle' });
  await page.waitForTimeout(1500);
  // probe computed backdrop bg to verify the fix actually rendered
  const probe = await page.evaluate(() => {
    const el = document.querySelector('.home-hero__backdrop');
    const cs = el ? getComputedStyle(el) : null;
    return cs ? { bgImage: cs.backgroundImage, colorScheme: matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light' } : null;
  });
  await page.screenshot({ path: join(OUT_DIR, file), fullPage: false });
  await page.close();
  console.log(`[${label}] saved ${file} — backdrop bg =`, probe?.bgImage?.slice(0, 90), '| scheme =', probe?.colorScheme);
}

const browser = await chromium.launch();

// DARK
{
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, colorScheme: 'dark' });
  await shoot(ctx, 'home-hero-dark.png', 'DARK');
  await ctx.close();
}

// LIGHT
{
  const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, colorScheme: 'light' });
  await shoot(ctx, 'home-hero-light.png', 'LIGHT');
  await ctx.close();
}

await browser.close();
console.log('done.');
