import { defineConfig, devices } from '@playwright/test'

/**
 * Sprint S26 — Playwright e2e 配置
 * - 仅装 chromium（webkit/firefox 留 v1.1）
 * - @playwright/test pin 1.48（1.49 改了 browserType API，避免 breaking）
 * - 截图仅失败时保留（only-on-failure）
 * - webServer 复用已起的 Vite dev（reuseExistingServer: true）
 */
export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: [
    ['list'],
    ['html', { open: 'never' }]
  ],

  use: {
    baseURL: 'http://127.0.0.1:7777',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ],

  webServer: {
    command: 'npm run dev',
    url: 'http://127.0.0.1:7777',
    reuseExistingServer: true,
    timeout: 60_000
  }
})