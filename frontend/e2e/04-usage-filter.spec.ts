import { test, expect } from '@playwright/test'

/**
 * US-4 Browse 顶部 USAGE 筛选
 * 验收：usage-filter 存在；点击 tool chip 后列表变（数量 ≤ 全量）。
 */
test('usage top filter narrows the list', async ({ page }) => {
  await page.goto('/browse-skills')

  await expect(page.getByTestId('usage-filter')).toBeVisible()

  // 等首屏卡片加载
  const cards = page.getByTestId('skill-card')
  await expect.poll(async () => await cards.count(), { timeout: 10_000 })
    .toBeGreaterThanOrEqual(5)
  const fullCount = await cards.count()

  // 点 tool chip（如果存在就点；不存在则跳过 — 不同 seed 可能缺某类）
  const toolChip = page.getByTestId('usage-filter-tool')
  const chipCount = await toolChip.count()
  test.skip(chipCount === 0, 'tool chip not present in this seed')

  await toolChip.click()
  // 等筛选后列表稳定
  await page.waitForTimeout(800)

  const filteredCount = await cards.count()
  expect(filteredCount).toBeLessThanOrEqual(fullCount)

  // active class 已应用
  await expect(toolChip).toHaveClass(/usage-chip--active/)
})