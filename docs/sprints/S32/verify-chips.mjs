// S32 verify: 截图 SkillDetail / Browse / CategoryView 三页
import { chromium } from 'playwright'
import { mkdir } from 'node:fs/promises'

const OUT = 'docs/sprints/S32/shots'
await mkdir(OUT, { recursive: true })

const browser = await chromium.launch()
const ctx = await browser.newContext({ viewport: { width: 1280, height: 900 } })
const page = await ctx.newPage()

async function shoot(url, file, opts = {}) {
  await page.goto(`http://127.0.0.1:7777${url}`, { waitUntil: 'networkidle' })
  await page.waitForTimeout(opts.wait ?? 800)
  await page.screenshot({ path: `${OUT}/${file}.png`, fullPage: opts.fullPage ?? false })
  console.log(`✓ ${url} -> ${file}.png`)
}

await shoot('/categories', '01-category-view', { fullPage: false })
await shoot('/browse', '02-browse-view', { fullPage: false })
await shoot('/browse?dim=usage', '03-browse-usage-dim', { fullPage: false })

// 找一个有用途分类的 skill 来截详情页
await page.goto('http://127.0.0.1:7777/browse', { waitUntil: 'networkidle' })
await page.waitForTimeout(800)
const firstCardLink = await page.locator('a[href^="/skills/"]').first().getAttribute('href').catch(() => null)
if (firstCardLink) {
  await shoot(firstCardLink, '04-skill-detail', { fullPage: false })
} else {
  console.log('⚠️ 未找到 skill 卡片链接，跳过详情页')
}

await browser.close()