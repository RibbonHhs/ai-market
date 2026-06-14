import { test, expect, Page } from '@playwright/test'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
const SHOT_DIR = path.resolve(__dirname, '../../docs/sprints/S37/screenshots')

/**
 * S37: 暗色主题下后台管理可视性修复 — QA 视觉回归
 * - 6 个 admin 页面 × 浅/深 双主题 = 12 张 after 截图
 * - admin 用户登录态：admin / admin123 (seed)
 * - 截图与 docs/sprints/admin-dark-before/* 做对比矩阵
 */

const ADMIN_PAGES: Array<{ key: string; path: string; title: string }> = [
  { key: 'dashboard',       path: '/admin/dashboard',       title: 'Dashboard' },
  { key: 'skills-list',     path: '/admin/skills',          title: 'Skill 管理' },
  { key: 'categories',      path: '/admin/categories',      title: '职业技能管理' },
  { key: 'categories-usage', path: '/admin/categories/usage', title: '用途分类管理' },
  { key: 'tags',            path: '/admin/tags',            title: '标签管理' },
  { key: 'users',           path: '/admin/users',           title: '用户管理' }
]

async function loginAdmin(page: Page) {
  await page.goto('/login')
  await page.waitForLoadState('networkidle', { timeout: 20_000 })
  await page.waitForTimeout(500)
  // 表单 label 是 "用户名" / "密码"
  await page.getByLabel('用户名').fill('admin').catch(async () => {
    // fallback: by placeholder
    await page.locator('input[placeholder*="用户名"], input[placeholder*="账号"]').first().fill('admin')
  })
  await page.getByLabel('密码').fill('admin123').catch(async () => {
    await page.locator('input[type="password"]').first().fill('admin123')
  })
  await page.locator('button[type="submit"]').first().click()
  // 等待跳转到 admin 或 home（带 token 后）
  await page.waitForURL(/\/admin|\//, { timeout: 15_000 })
  await page.waitForTimeout(500)
}

async function visitAdmin(page: Page, route: string) {
  await page.goto(route)
  await page.waitForLoadState('networkidle', { timeout: 20_000 })
  await page.waitForTimeout(800)
}

// AdminTagView 只有 a-page-header title，没有 h2；用更宽松的 locator 兼容
async function isPageReady(page: Page): Promise<boolean> {
  return await page.locator('.ant-page-header, .ant-card').first().isVisible({ timeout: 5_000 }).catch(() => false)
}

test.describe('S37 admin dark — visual regression', () => {
  test.use({ viewport: { width: 1440, height: 900 } })

  test.describe('dark mode', () => {
    test.use({ colorScheme: 'dark' })

    for (const p of ADMIN_PAGES) {
      test(`dark-${p.key}`, async ({ page }) => {
        await loginAdmin(page)
        await visitAdmin(page, p.path)
        await expect.poll(async () => isPageReady(page)).toBe(true)
        await page.screenshot({
          path: path.join(SHOT_DIR, `${p.key}-dark-after.png`),
          fullPage: true
        })
      })
    }
  })

  test.describe('light mode', () => {
    test.use({ colorScheme: 'light' })

    for (const p of ADMIN_PAGES) {
      test(`light-${p.key}`, async ({ page }) => {
        await loginAdmin(page)
        await visitAdmin(page, p.path)
        await expect.poll(async () => isPageReady(page)).toBe(true)
        await page.screenshot({
          path: path.join(SHOT_DIR, `${p.key}-light-after.png`),
          fullPage: true
        })
      })
    }
  })
})
