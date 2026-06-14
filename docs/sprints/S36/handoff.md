# S36 收尾 — 新手指引（Newbie Guide）

> **Sprint**: S36
> **范围**: 首页「第一次使用 Skill?」入口（HomeStats CTA） + `/newbie-guide` 引导页
> **结果**: ✅ 全部交付，build 绿，10/10 Playwright 通过
> **日期**: 2026-06-13

---

## 1. 用户需求（原文）

> 开发首页「第一次使用 Skill?」卡片中「开始新手指引」的页面，这个页面应该详细介绍 skill，并将 API 接入菜单放在这个新手指引，然后在添加 skills manager 使用说明。

## 2. 改动清单

### 2.1 新增文件
| 文件 | 用途 |
|------|------|
| `frontend/src/views/NewbieGuideView.vue` | `/newbie-guide` 引导页（Hero + 3 区块 + 锚点导航） |
| `frontend/e2e/36-newbie-guide.spec.ts` | Playwright 走查脚本（10 cases：light / dark / mobile） |
| `docs/sprints/S36/README.md` | Sprint 计划 |
| `docs/sprints/S36/prd-newbie-guide.md` | PRD（10 AC） |
| `docs/sprints/S36/design-newbie-guide.md` | 设计稿（色板 / 间距 / 暗色态 / 反模式自检） |
| `docs/sprints/S36/qa-newbie-guide.md` | QA 报告（用例 / 截图 / a11y） |
| `docs/sprints/S36/handoff.md` | 本文件 |
| `docs/sprints/S36/screenshots/*.png` | 7 张截图（light / dark / mobile） |

### 2.2 修改文件
| 文件 | 变更摘要 |
|------|---------|
| `frontend/src/router/index.ts` | 新增 `/newbie-guide` 路由，name `newbie-guide` |
| `frontend/src/components/home/HomeStats.vue` | 旧 CTA 改文案「第一次使用 Skill? 不知道从哪开始？3 分钟带你了解 Skills 是什么、怎么安装、怎么用 API 接入。」+ 跳 `/newbie-guide` + 加 `data-testid="home-stats-cta"`（S36-007 收口） |
| `frontend/src/views/HomeView.vue` | （S36-007 收口：移除原本挂在 HomeStats 与 HomeFeatured 之间的 `<HomeOnboarding />`） |

### 2.3 删除文件
| 文件 | 说明 |
|------|------|
| `frontend/src/components/home/HomeOnboarding.vue` | S36-007 收口：与 HomeStats 旧 CTA 文案重复，删除避免双入口 |

### 2.4 关键设计决策
- **单一入口（S36-007 收口后）**：HomeStats 左侧 CTA 改文案 + 跳 `/newbie-guide`；原新增的 `HomeOnboarding` 卡片因与 HomeStats 旧 CTA 重复已被删除
- **§3 不重写 ApiGuideView**：跳转卡片 + 5 个常用端点 chip + 主 CTA，详情跳 `/api-guide`
- **§2 迁自 HomeHero agent tab**：4 步教程 + 下载逻辑（`fetch('/api/skills/slug/skills-manager/download')`）原样保留
- **暗色态全部走 token**：浅色 `#7c3aed` / 暗色 `#a78bfa` 由 `--primary` 提供，不写死
- **响应式**：≤640px CTA 与下方数字 grid 一起 `flex-direction: column`，按钮 `align-self: stretch`；code-block 字号缩为 12px

## 3. 验收结果

- ✅ `npm run build` 18.84s 通过（S36-007 收口后无 TS error，HomeView chunk 未增长）
- ✅ Playwright 10/10 通过（light 6 + dark 2 + mobile 2；`home-onboarding` 已 `.toHaveCount(0)` 显式断言旧卡片不存在）
- ✅ 7 张截图归档到 `docs/sprints/S36/screenshots/`
- ✅ PRD §5 的 10 个 AC 全部勾选
- ✅ a11y：装饰 emoji `aria-hidden`，CTA 可见文字，nav 标签


## 4. 已知遗留 / 下一步

- **AppHeader 在 375px 既有溢出**（`app-header__right` r=428）：S36 之前就存在，与本次新增组件无关。修复建议放到 S37+（在 AppHeader 内加 `max-width: 100%` 或隐藏小屏主题按钮）
- **HomeStats 老 CTA 仍指向 browse**：本 Sprint 保留，清理放到 S37+ 统一做「合并 / 删除」决定
- **§2 下载按钮未跑端到端真下载**：仅断言 enabled + 可见；真实下载链路（ZIP 流）由 HomeHero 上次 Sprint 已验证，本期共用同一 fetch 函数，行为一致

## 5. 关联文档

- 需求：`docs/sprints/S36/prd-newbie-guide.md`
- 设计：`docs/sprints/S36/design-newbie-guide.md`
- 验收：`docs/sprints/S36/qa-newbie-guide.md`
- 截图：`docs/sprints/S36/screenshots/`
- 自动化：`frontend/e2e/36-newbie-guide.spec.ts`
- 计划：`docs/sprints/S36/README.md`

## 6. 关键路径

- 新组件：`D:\codeing\workspace\skills-map\frontend\src\components\home\HomeOnboarding.vue`
- 新页面：`D:\codeing\workspace\skills-map\frontend\src\views\NewbieGuideView.vue`
- 路由：`D:\codeing\workspace\skills-map\frontend\src\router\index.ts`（在 `/api-guide` 之后插入）
- 首页挂载：`D:\codeing\workspace\skills-map\frontend\src\views\HomeView.vue`

---

**Lead 签收**：可合并 `master`。S37 候选：清理 HomeStats 旧 CTA、修复 AppHeader 移动端溢出、给新手指引加埋点（CTA 点击 / 区块 PV）。
