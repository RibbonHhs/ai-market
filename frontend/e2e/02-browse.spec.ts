import { test, expect } from '@playwright/test'

/**
 * US-2 Browse 列表与卡片渲染
 * 验收：/browse-skills 返回 200；skill-grid 存在；skill-card ≥ 5；每张卡含 usage chip。
 */
test('browse page renders skill grid with ≥ 5 cards', async ({ page }) => {
  const response = await page.goto('/browse-skills')
  expect(response?.status()).toBe(200)

  const grid = page.getByTestId('skill-grid')
  await expect(grid).toBeVisible()

  // H2 seed 数据保证 ≥ 5
  const cards = page.getByTestId('skill-card')
  await expect.poll(async () => await cards.count(), { timeout: 10_000 })
    .toBeGreaterThanOrEqual(5)

  // 每张卡片至少含 1 个 usage chip
  const usageChips = page.getByTestId('skill-usage-chip')
  const cardCount = await cards.count()
  const chipCount = await usageChips.count()
  expect(chipCount).toBeGreaterThanOrEqual(cardCount)

  await page.screenshot({
    path: '../docs/sprints/S26/screenshots/browse.png',
    fullPage: true
  })
})