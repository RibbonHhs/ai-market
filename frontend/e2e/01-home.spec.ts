import { test, expect } from '@playwright/test'

/**
 * US-1 首页加载与 Tab 切换
 * 验收：访问 / 返回 200；home-hero 可见；tab 切换有效；默认 human。
 */
test('home page loads with hero and tabs', async ({ page }) => {
  const response = await page.goto('/')
  expect(response?.status()).toBe(200)

  await expect(page.getByTestId('home-hero')).toBeVisible()

  // 默认 Tab = 人类
  const humanTab = page.getByTestId('home-tab-human')
  await expect(humanTab).toBeVisible()
  await expect(humanTab).toHaveClass(/is-active/)

  // 切换到智能体
  const agentTab = page.getByTestId('home-tab-agent')
  await expect(agentTab).toBeVisible()
  await agentTab.click()
  await expect(agentTab).toHaveClass(/is-active/)
  await expect(humanTab).not.toHaveClass(/is-active/)

  // 归档浅色截图
  await page.screenshot({
    path: '../docs/sprints/S26/screenshots/home.png',
    fullPage: true
  })
})