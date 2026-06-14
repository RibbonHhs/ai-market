// Quick probe — confirm agent / agent-num / backdrop all swap in dark mode
import { chromium } from '../S31/node_modules/playwright/index.mjs';

const browser = await chromium.launch();
const ctx = await browser.newContext({ viewport: { width: 1440, height: 900 }, colorScheme: 'dark' });
const page = await ctx.newPage();
await page.goto('http://localhost:7777/', { waitUntil: 'networkidle' });
await page.waitForTimeout(1200);
// click the "agent" tab so __agent / __agent-num render
const agentTab = await page.$('text=我是智能体');
if (agentTab) await agentTab.click();
await page.waitForTimeout(800);

const probe = await page.evaluate(() => {
  const pick = (sel) => {
    const el = document.querySelector(sel);
    if (!el) return null;
    const cs = getComputedStyle(el);
    return { bg: cs.backgroundColor, bgImage: cs.backgroundImage, color: cs.color, border: cs.borderColor };
  };
  return {
    backdrop: pick('.home-hero__backdrop'),
    agent: pick('.home-hero__agent'),
    agentNum: pick('.home-hero__agent-num'),
    title: pick('.home-hero__title'),
    lede: pick('.home-hero__lede'),
  };
});
console.log(JSON.stringify(probe, null, 2));
await browser.close();
