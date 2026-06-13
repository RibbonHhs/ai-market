import { test, expect } from '@playwright/test'
import path from 'path'

/**
 * S27: 暗色全站化 — 6 张暗色截图归档
 * - home / browse / detail / sidebar / apiguide / favorite
 * - 用 emulateMedia dark 触发 prefers-color-scheme
 * - 截图存到 docs/sprints/S27/screenshots/
 */
const SHOT_DIR = path.resolve(__dirname, '../../docs/sprints/S27/screenshots')

test.describe('S27 dark mode screenshots', () => {
  test.use({ colorScheme: 'dark' })

  test('dark-home', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByTestId('home-hero')).toBeVisible({ timeout: 15_000 })
    await page.waitForTimeout(500)
    await page.screenshot({ path: path.join(SHOT_DIR, 'dark-home.png'), fullPage: true })
  })

  test('dark-browse', async ({ page }) => {
    await page.goto('/browse-skills')
    await expect(page.getByTestId('skill-grid')).toBeVisible({ timeout: 15_000 })
    await page.waitForTimeout(500)
    await page.screenshot({ path: path.join(SHOT_DIR, 'dark-browse.png'), fullPage: true })
  })

  test('dark-detail', async ({ page }) => {
    await page.goto('/browse-skills')
    const firstCard = page.getByTestId('skill-card').first()
    await expect(firstCard).toBeVisible({ timeout: 15_000 })
    await firstCard.click()
    await page.waitForURL(/\/skills\/.+/, { timeout: 15_000 })
    await expect(page.getByTestId('skill-detail')).toBeVisible({ timeout: 15_000 })
    await page.waitForTimeout(500)
    await page.screenshot({ path: path.join(SHOT_DIR, 'dark-detail.png'), fullPage: true })
  })

  test('dark-sidebar', async ({ page }) => {
    await page.goto('/browse-skills')
    await expect(page.getByTestId('skill-grid')).toBeVisible({ timeout: 15_000 })
    await page.waitForTimeout(500)
    // 局部截图：聚焦左 sidebar 区域
    const filter = page.locator('.browse__filter').first()
    await filter.screenshot({ path: path.join(SHOT_DIR, 'dark-sidebar.png') })
  })

  test('dark-apiguide', async ({ page }) => {
    await page.goto('/api-guide')
    await page.waitForLoadState('networkidle', { timeout: 15_000 })
    await page.waitForTimeout(500)
    await page.screenshot({ path: path.join(SHOT_DIR, 'dark-apiguide.png'), fullPage: true })
  })

  test('dark-favorite', async ({ page }) => {
    // 收藏页需登录；用未登录态先访问 /me 看引导页（仍能拍到「我的收藏」卡片标题与底色）
    await page.goto('/me')
    await page.waitForLoadState('networkidle', { timeout: 15_000 })
    await page.waitForTimeout(500)
    await page.screenshot({ path: path.join(SHOT_DIR, 'dark-favorite.png'), fullPage: true })
  })
})
