import { test, expect } from '@playwright/test'

/**
 * US-5 暗色模式双视图
 * 验收：emulateMedia dark 后首页 + 详情可见，截图归档。
 */
test.describe('dark mode', () => {
  test.use({ colorScheme: 'dark' })

  test('home renders in dark mode', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByTestId('home-hero')).toBeVisible({ timeout: 15_000 })
    await page.screenshot({
      path: '../docs/sprints/S26/screenshots/dark-home.png',
      fullPage: true
    })
  })

  test('detail renders in dark mode', async ({ page }) => {
    await page.goto('/')
    const firstCard = page.getByTestId('skill-card').first()
    await expect(firstCard).toBeVisible({ timeout: 15_000 })
    await firstCard.click()
    await page.waitForURL(/\/skills\/.+/, { timeout: 15_000 })
    await expect(page.getByTestId('skill-detail')).toBeVisible({ timeout: 15_000 })
    await page.screenshot({
      path: '../docs/sprints/S26/screenshots/dark-detail.png',
      fullPage: true
    })
  })
})