import { test, expect } from '@playwright/test'
import { mkdirSync } from 'fs'

const SHOT_DIR = '../docs/sprints/S28/screenshots'

test.beforeAll(() => mkdirSync(SHOT_DIR, { recursive: true }))

/**
 * S28 暗色手动切换按钮 — 3 态循环 + localStorage 持久化
 * 验收：点击 → mode 循环 auto→light→dark→auto；data-theme 同步；localStorage 写入
 */
test.describe('theme toggle (S28)', () => {
  test.beforeEach(async ({ context }) => {
    // 清空 localStorage 让每次测试从 auto 开始
    await context.addInitScript(() => {
      try { localStorage.removeItem('skillsmap.theme') } catch { /* 忽略 */ }
    })
  })

  test('cycle through auto/light/dark and back to auto', async ({ page }) => {
    await page.goto('/')
    const btn = page.getByTestId('theme-toggle')
    await expect(btn).toBeVisible({ timeout: 15_000 })

    // 第 1 次点击：auto → light
    await btn.click()
    await expect.poll(async () =>
      page.evaluate(() => document.documentElement.dataset.theme)
    ).toBe('light')
    expect(await page.evaluate(() => localStorage.getItem('skillsmap.theme'))).toBe('light')

    // 第 2 次点击：light → dark
    await btn.click()
    await expect.poll(async () =>
      page.evaluate(() => document.documentElement.dataset.theme)
    ).toBe('dark')
    expect(await page.evaluate(() => localStorage.getItem('skillsmap.theme'))).toBe('dark')

    // 第 3 次点击：dark → auto（data-theme 删掉，localStorage 写 'auto'）
    await btn.click()
    await expect.poll(async () =>
      page.evaluate(() => document.documentElement.dataset.theme)
    ).toBeUndefined()
    expect(await page.evaluate(() => localStorage.getItem('skillsmap.theme'))).toBe('auto')
  })

  test('persists across reload', async ({ page }) => {
    await page.goto('/')
    const btn = page.getByTestId('theme-toggle')
    await btn.click() // → light
    await btn.click() // → dark
    await expect.poll(async () =>
      page.evaluate(() => document.documentElement.dataset.theme)
    ).toBe('dark')

    // 刷新页面：应保持 dark
    await page.reload()
    await expect.poll(async () =>
      page.evaluate(() => document.documentElement.dataset.theme)
    ).toBe('dark')
    expect(await page.evaluate(() => localStorage.getItem('skillsmap.theme'))).toBe('dark')
  })

  test('light mode screenshot', async ({ page }) => {
    await page.goto('/')
    const btn = page.getByTestId('theme-toggle')
    await btn.click() // → light
    await expect(btn).toBeVisible()
    await expect.poll(async () =>
      page.evaluate(() => document.documentElement.dataset.theme)
    ).toBe('light')
    await page.screenshot({ path: `${SHOT_DIR}/theme-light.png`, fullPage: true })
  })

  test('dark mode screenshot (manual override over light OS)', async ({ page }) => {
    await page.goto('/')
    const btn = page.getByTestId('theme-toggle')
    await btn.click() // → light
    await btn.click() // → dark
    await expect.poll(async () =>
      page.evaluate(() => document.documentElement.dataset.theme)
    ).toBe('dark')
    await page.screenshot({ path: `${SHOT_DIR}/theme-dark.png`, fullPage: true })
  })

  test('auto mode screenshot (follow OS light)', async ({ page, context }) => {
    await context.addInitScript(() => {
      try { localStorage.setItem('skillsmap.theme', 'auto') } catch { /* 忽略 */ }
    })
    await page.emulateMedia({ colorScheme: 'light' })
    await page.goto('/')
    const btn = page.getByTestId('theme-toggle')
    await expect(btn).toBeVisible({ timeout: 15_000 })
    const theme = await page.evaluate(() => document.documentElement.dataset.theme)
    expect(['light', undefined]).toContain(theme)
    await page.screenshot({ path: `${SHOT_DIR}/theme-auto.png`, fullPage: true })
  })
})
