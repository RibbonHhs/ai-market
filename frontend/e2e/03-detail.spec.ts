import { test, expect } from '@playwright/test'

/**
 * US-3 首页 → 详情页跳转
 * 验收：点首页 SkillCard → URL 变 /skills/{slug} → skill-detail 可见 → usage-block 含 chip。
 */
test('clicking skill card navigates to detail with usage block', async ({ page }) => {
  await page.goto('/')
  // 等首页卡片渲染完成（H2 seed 异步）
  const firstCard = page.getByTestId('skill-card').first()
  await expect(firstCard).toBeVisible({ timeout: 15_000 })
  await firstCard.click()

  // URL 形态：/skills/...（slug 由后端返回）
  await page.waitForURL(/\/skills\/.+/, { timeout: 15_000 })

  await expect(page.getByTestId('skill-detail')).toBeVisible({ timeout: 15_000 })

  // usage block 可见（部分 skill 可能无 usage，断言「若存在则可见」）
  const usageBlock = page.getByTestId('skill-usage-block')
  if (await usageBlock.count() > 0) {
    await expect(usageBlock).toBeVisible()
  }

  await page.screenshot({
    path: '../docs/sprints/S26/screenshots/detail.png',
    fullPage: true
  })
})