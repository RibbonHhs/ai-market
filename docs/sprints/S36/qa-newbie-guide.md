# QA — S36 新手指引（Newbie Guide）

> **Sprint**: S36
> **测试工具**: Playwright 1.48 (Chromium)
> **dev server**: http://127.0.0.1:7777 (Vite dev)
> **结果**: ✅ 10/10 通过
> **日期**: 2026-06-13

---

## 1. 用例与结果

| ID | AC | 用例 | 结果 | 截图 |
|----|----|------|------|------|
| AC-1 | 首页能看到新卡片（位置：HomeStats 与 HomeFeatured 之间） | `home shows onboarding card and CTA jumps to /newbie-guide` | PASS | `01-home-with-onboarding.png` |
| AC-2 | 点击卡片跳到 `/newbie-guide` | 同上 | PASS | — |
| AC-2 | 引导页 title 含「新手指引」 | 同上（`toHaveTitle(/新手指引/)`） | PASS | — |
| AC-3 | Hero 标题 + lede 可见 | `newbie-guide page has all 3 sections and anchor nav` | PASS | `02-newbie-guide-full.png` |
| AC-4 | 3 个 anchor pill 可见 | 同上 | PASS | 同上 |
| AC-4 | 点击 anchor 滚动到对应区块 | `anchor click scrolls to corresponding section` | PASS | — |
| AC-5 | §1 可见，含 SKILL.md 代码块 | `§1 contains SKILL.md code block` | PASS | 同上 |
| AC-6 | §2 4 步教程可见，下载按钮可点 | `§2 has 4-step tutorial and download button is enabled` | PASS | `03-section-manager.png` |
| AC-7 | §3 5 个端点 chip 可见，CTA 跳到 /api-guide | `§3 API CTA jumps to /api-guide` | PASS | 同上 |
| AC-8 | 暗色态 — 首页 + 引导页可读 | `home onboarding renders in dark mode` / `newbie guide renders in dark mode` | PASS | `04-dark-home-onboarding.png` / `05-dark-newbie-guide.png` |
| AC-9 | 移动端（375px）布局正常 | `home onboarding stacks on mobile` / `newbie guide stacks on mobile` | PASS* | `06-mobile-home-onboarding.png` / `07-mobile-newbie-guide.png` |

\* 详见 §3 关于 app-header 既有溢出的说明。

## 2. 自动化

文件：`frontend/e2e/36-newbie-guide.spec.ts`（10 cases）

```
✓ S36 newbie guide (light) › home shows onboarding card and CTA jumps to /newbie-guide (8.5s)
✓ S36 newbie guide (light) › newbie-guide page has all 3 sections and anchor nav (6.8s)
✓ S36 newbie guide (light) › §1 contains SKILL.md code block (2.9s)
✓ S36 newbie guide (light) › §2 has 4-step tutorial and download button is enabled (7.2s)
✓ S36 newbie guide (light) › §3 API CTA jumps to /api-guide (5.2s)
✓ S36 newbie guide (light) › anchor click scrolls to corresponding section (6.5s)
✓ S36 newbie guide (dark)  › home onboarding renders in dark mode (7.3s)
✓ S36 newbie guide (dark)  › newbie guide renders in dark mode (7.7s)
✓ S36 newbie guide (mobile) › home onboarding stacks on mobile (5.9s)
✓ S36 newbie guide (mobile) › newbie guide stacks on mobile (2.8s)

10 passed (14.6s)
```

## 3. 移动端已知问题（**Pre-existing**, 不在 S36 范围）

- AppHeader 的 `.app-header__right` 在 viewport 375px 下右溢出（r=428，> 375）
- 该溢出在 S36 之前就存在（S32 截图同样有此问题），与本次新增的 `HomeOnboarding` / `NewbieGuideView` 无关
- 修复建议（**S37+**）：在 AppHeader 内部加 `max-width: 100%` + `overflow: hidden`，或在小屏隐藏主题切换按钮
- S36 范围：仅验证本次新增的两个组件 / 视图在 375px 下不溢出
  - `[data-testid="home-onboarding"]` 在 375px 下宽度 ≤ 375 ✓
  - `[data-testid="newbie-guide"]` 在 375px 下宽度 ≤ 375 ✓

## 4. a11y 抽查

- 锚点 pill：可见文字「Skill 是什么」「Skills Manager 使用说明」「API 接入」+ `<nav aria-label="快速跳转">`
- 装饰性 emoji 全部 `aria-hidden="true"`
- CTA 按钮：可见文字「开始新手指引」「下载 Skill 包」「前往完整 API 接入指南」+ 箭头 `aria-hidden`
- Section heading：h1 + h2，层级正确
- 暗色态对比度：所有 `--text-primary` / `--text-secondary` / `--primary` 组合在暗色下经 token 校验 ≥ 4.5:1

## 5. 视觉自检

- ✅ 首页「第一次使用 Skill?」卡片位置：HomeStats（上方）→ HomeOnboarding（中间）→ 精选 Skills 榜单（下方）
- ✅ 卡片 hover：上浮 + 阴影增强（`translateY(-1px)` + `shadow-md`）
- ✅ 引导页 Hero：紫色 eyebrow + 大标题 + 灰副文
- ✅ 锚点导航：3 个 pill，sticky-ish（不强制 sticky，由 scrollIntoView 滚动）
- ✅ §1 解释 + SKILL.md 示例 + manifest.json 示例
- ✅ §2 4 步数字徽章 + 下载按钮（沿用 HomeHero 的 fetch 逻辑）+ shell 代码块 + 3 条 prompt
- ✅ §3 跳转卡片：紫渐变背景 + 5 个端点 chip + 主 CTA 按钮
- ✅ 暗色态：白底 → 暗紫底，代码块 `var(--bg-elevated)` 区分
- ✅ 移动端：单列堆叠，CTA 按钮 stretch，代码块缩字号

## 6. 反模式自检

- [x] 没有引入新依赖
- [x] 没有替换 HomeStats / HomeFeatured / HomeHot
- [x] 没有重写 ApiGuideView
- [x] 没有用 Element Plus
- [x] 颜色全部走 token（`--bg-*` / `--text-*` / `--primary` / `--border`）
- [x] 路由 name 用 kebab-case `newbie-guide`
- [x] Vue 文件用 `<script setup lang="ts">`
- [x] 没有用 `transition: all`（仅指定属性）
- [x] 没有 animating width/height

## 7. 归档

- Playwright spec: `frontend/e2e/36-newbie-guide.spec.ts`
- 截图: `docs/sprints/S36/screenshots/` (7 张)
- 测试报告: `playwright-report/` (HTML 报告)
