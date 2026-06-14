import { test, expect } from '@playwright/test'

/**
 * Sprint S36 — 新手指引（Newbie Guide）
 * 验收：
 *  AC-1 首页能看到「开始新手指引 →」入口（HomeStats 左侧 CTA）
 *  AC-2 点击入口跳到 /newbie-guide
 *  AC-3 引导页顶部 Hero 标题 + lede 可见
 *  AC-4 锚点导航 3 个 pill 可见
 *  AC-5 §1「Skill 是什么」可见，含 SKILL.md 代码块
 *  AC-6 §2「Skills Manager 使用说明」4 步教程可见，下载按钮可点击
 *  AC-7 §3「API 接入」跳转卡片可见，CTA 点击跳到 /api-guide
 *  AC-8 暗色态可读
 *  AC-9 移动端布局正常
 *  AC-10 npm run build 通过（由 handoff 验证，不在此处测）
 */

test.describe('S36 newbie guide (light)', () => {
  test('home shows stats CTA and jumps to /newbie-guide', async ({ page }) => {
    await page.goto('/')
    // 等首页骨架
    await expect(page.getByTestId('home-hero')).toBeVisible({ timeout: 15_000 })
    await expect(page.getByTestId('home-stats')).toBeVisible({ timeout: 10_000 })
    // HomeStats 左侧 CTA（唯一的新手指引入口）
    const cta = page.getByTestId('home-stats-cta')
    await expect(cta).toBeVisible()
    await expect(cta).toContainText('开始新手指引')
    // 验证首页没有第二张新手卡片
    await expect(page.getByTestId('home-onboarding')).toHaveCount(0)

    // 截图：首页
    await page.screenshot({
      path: '../docs/sprints/S36/screenshots/01-home-with-onboarding.png',
      fullPage: true
    })

    // 点击 → 跳到 /newbie-guide
    await cta.click()
    await page.waitForURL(/\/newbie-guide$/, { timeout: 10_000 })
    await expect(page.getByTestId('newbie-guide')).toBeVisible({ timeout: 10_000 })
    await expect(page).toHaveTitle(/新手指引/)
  })

  test('newbie-guide page has all 3 sections and anchor nav', async ({ page }) => {
    await page.goto('/newbie-guide')
    await expect(page.getByTestId('newbie-guide')).toBeVisible({ timeout: 15_000 })

    // Hero
    await expect(page.locator('.newbie-hero__title')).toContainText('3 分钟上手 SkillsMap')

    // 三个 section
    await expect(page.getByTestId('newbie-section-what')).toBeVisible()
    await expect(page.getByTestId('newbie-section-manager')).toBeVisible()
    await expect(page.getByTestId('newbie-section-api')).toBeVisible()

    // 三个 anchor pill
    await expect(page.getByTestId('newbie-anchor-what-is-skill')).toBeVisible()
    await expect(page.getByTestId('newbie-anchor-skills-manager')).toBeVisible()
    await expect(page.getByTestId('newbie-anchor-api-access')).toBeVisible()

    // 截图：引导页全屏
    await page.screenshot({
      path: '../docs/sprints/S36/screenshots/02-newbie-guide-full.png',
      fullPage: true
    })
  })

  test('§1 contains SKILL.md code block', async ({ page }) => {
    await page.goto('/newbie-guide')
    const section = page.getByTestId('newbie-section-what')
    await expect(section).toContainText('SKILL.md')
    await expect(section.locator('.code-block').first()).toBeVisible()
  })

  test('§2 has 4-step tutorial and download button is enabled', async ({ page }) => {
    await page.goto('/newbie-guide')
    const section = page.getByTestId('newbie-section-manager')
    await expect(section).toBeVisible()
    // 4 步
    const steps = section.locator('.steps__item')
    await expect(steps).toHaveCount(4)
    // 下载按钮（mock：仅断言可点，不真发请求）
    const downloadBtn = page.getByTestId('newbie-download-btn')
    await expect(downloadBtn).toBeVisible()
    await expect(downloadBtn).toBeEnabled()

    // 截图：§2
    await section.scrollIntoViewIfNeeded()
    await page.screenshot({
      path: '../docs/sprints/S36/screenshots/03-section-manager.png'
    })
  })

  test('§3 API CTA jumps to /api-guide', async ({ page }) => {
    await page.goto('/newbie-guide')
    const section = page.getByTestId('newbie-section-api')
    await expect(section).toBeVisible()
    // 5 个端点 chip
    const chips = section.locator('.endpoint-chip')
    await expect(chips).toHaveCount(5)
    // CTA 跳转
    const cta = page.getByTestId('newbie-api-cta')
    await expect(cta).toBeVisible()
    await cta.click()
    await page.waitForURL(/\/api-guide$/, { timeout: 10_000 })
    await expect(page).toHaveTitle(/API 接入/)
  })

  test('anchor click scrolls to corresponding section', async ({ page }) => {
    await page.goto('/newbie-guide')
    await expect(page.getByTestId('newbie-guide')).toBeVisible({ timeout: 15_000 })
    // 点击「Skills Manager」锚点
    await page.getByTestId('newbie-anchor-skills-manager').click()
    // 等待滚动稳定
    await page.waitForTimeout(500)
    // §2 应该在视口里
    const section = page.getByTestId('newbie-section-manager')
    const box = await section.boundingBox()
    expect(box).not.toBeNull()
    // 顶边距在 0~200 之间（考虑 scroll-margin-top: 24px）
    expect(box!.y).toBeLessThan(200)
    expect(box!.y).toBeGreaterThanOrEqual(0)
  })
})

test.describe('S36 newbie guide (dark)', () => {
  test.use({ colorScheme: 'dark' })

  test('home stats CTA renders in dark mode', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByTestId('home-hero')).toBeVisible({ timeout: 15_000 })
    await expect(page.getByTestId('home-stats-cta')).toBeVisible({ timeout: 10_000 })
    await page.screenshot({
      path: '../docs/sprints/S36/screenshots/04-dark-home-onboarding.png',
      fullPage: true
    })
  })

  test('newbie guide renders in dark mode', async ({ page }) => {
    await page.goto('/newbie-guide')
    await expect(page.getByTestId('newbie-guide')).toBeVisible({ timeout: 15_000 })
    await expect(page.getByTestId('newbie-section-what')).toBeVisible()
    await expect(page.getByTestId('newbie-section-manager')).toBeVisible()
    await expect(page.getByTestId('newbie-section-api')).toBeVisible()
    await page.screenshot({
      path: '../docs/sprints/S36/screenshots/05-dark-newbie-guide.png',
      fullPage: true
    })
  })
})

test.describe('S36 newbie guide (mobile)', () => {
  test.use({ viewport: { width: 375, height: 812 } })

  test('home stats CTA stacks on mobile', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByTestId('home-hero')).toBeVisible({ timeout: 15_000 })
    await expect(page.getByTestId('home-stats-cta')).toBeVisible({ timeout: 10_000 })
    // 仅断言 HomeStats CTA 自身不溢出（app-header 既有 overflow 不在 S36 范围）
    const ctaWidth = await page.evaluate(() => {
      const el = document.querySelector('[data-testid="home-stats-cta"]') as HTMLElement | null
      return el ? el.getBoundingClientRect().width : 0
    })
    expect(ctaWidth).toBeLessThanOrEqual(375)
    await page.screenshot({
      path: '../docs/sprints/S36/screenshots/06-mobile-home-onboarding.png',
      fullPage: true
    })
  })

  test('newbie guide stacks on mobile', async ({ page }) => {
    await page.goto('/newbie-guide')
    await expect(page.getByTestId('newbie-guide')).toBeVisible({ timeout: 15_000 })
    // 新手指引 view 主体不溢出（app-header 既有 overflow 不在 S36 范围）
    const guideWidth = await page.evaluate(() => {
      const el = document.querySelector('[data-testid="newbie-guide"]') as HTMLElement | null
      return el ? el.getBoundingClientRect().width : 0
    })
    expect(guideWidth).toBeLessThanOrEqual(375)
    await page.screenshot({
      path: '../docs/sprints/S36/screenshots/07-mobile-newbie-guide.png',
      fullPage: true
    })
  })
})
