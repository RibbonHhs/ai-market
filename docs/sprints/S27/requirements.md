# Sprint S27 — 暗色全站化 PRD

> **承接**: S25（chip 暗色）+ S26（e2e + 暗色回归）
> **范围**: P0 单独 — 暗色全站化（0.5 sprint）
> **不开**: F+G 限流运维化 / B 多对多 / H LLM（均推 S28+）

---

## 1. Sprint 目标

将 SkillsMap 前端从「chip-only 暗色」扩展为「**全站响应式暗色**」：用户操作系统切换到暗色（或通过 `data-theme="dark"` 钩子）时，**整站**（背景 / 文字 / 边框 / 卡片 / 表格 / 表单 / 顶部导航 / 侧边栏 / 文档页）自动跟随，**零 JS 切换按钮**。

## 2. 背景与上下文

- S25 仅在 chip 流（SkillCard 双 chip / SkillDetail 用途区块 / Browse 顶部 13 chip）实现了暗色
- 其它视图（首页 hero / sidebar 树 / 排序条 / 文档页 / 收藏页）仍硬编码浅色，浏览器/系统切到暗色后**刺眼**
- S26 用 Playwright `emulateMedia({colorScheme:'dark'})` 验证 chip 暗色过，但其它区域**未覆盖**
- 决策：选「暗色全站化」单做，0.5 sprint，限 CSS 变量 + Ant Design darkAlgorithm，**不引**切换按钮 / 主题库

## 3. 范围与 Out of Scope

### 3.1 In Scope

- `global.scss` 扩展为**完整 token 系统**（浅色 + 暗色双套 CSS 变量）
- `App.vue` 注入 Ant Design Vue 4 `darkAlgorithm`，根据 `data-theme` / `prefers-color-scheme` 切换
- 7+ Vue 组件的硬编码颜色 → `var(--xxx)`（不删 class，不改 class 名）
- 6 张暗色截图归档：dark-home / dark-browse / dark-detail / dark-sidebar / dark-apiguide / dark-favorite
- `npm run build` + `npm run test:e2e` 全过

### 3.2 Out of Scope（推 S28+）

| 项 | 推后 |
|----|------|
| 手动切换按钮（白天/黑夜 toggle） | S28+（需新增 Pinia store + 偏好持久化）|
| 用户偏好持久化到后端 | S28+ |
| F+G 限流运维化 | S28 P0 |
| B USAGE 多对多 | S28+（schema 大改）|
| H LLM 二次分类 | S28+（需 API key + 成本护栏）|
| 跨浏览器 webkit+firefox | S28+ |

## 4. User Stories（4 条）

### US-1 暗色首页
**作为** 暗色模式用户
**我希望** 访问 `/` 时首页 hero、tab、热门 skill 区块全部跟随系统暗色
**以便** 夜间浏览不刺眼

**验收**:
- `prefers-color-scheme: dark` 触发后 `body` 背景为 `#141414`
- HomeHero 双 tab 文本、SkillsManager 下载按钮在暗色下文字对比度 ≥ 4.5:1
- HomeHot 卡片底色为 `--bg-secondary`（`#1f1f1f`）
- 浅色基准线**不破**（移除暗色后变回浅色无残留）

### US-2 暗色 Browse
**作为** 暗色模式用户
**我希望** `/browse-skills` 左侧筛选树、顶部 chip 流、搜索/排序条、skill grid 全部暗色化
**以便** 浏览体验与 chip 暗色一致

**验收**:
- 左侧 sidebar 背景 `--bg-secondary`，树节点 hover 态、active 态、暗色可读
- 顶部 chip 流保持 S25 配色（不动）
- `.browse__head` 搜索/排序条背景 `--bg-secondary`
- 卡片网格底色透明 / 跟随 `--bg-primary`

### US-3 暗色 Detail
**作为** 暗色模式用户
**我希望** `/skills/{slug}` 的 body、metadata、评分区全部暗色化
**以便** 详情阅读不刺眼

**验收**:
- S25 chip 流保持（不动）
- SkillDetail 主体背景 `--bg-primary`
- metadata 表格行 hover 态、文本、链接暗色可读
- ReviewForm 输入框、按钮、StarRating 暗色可读

### US-4 暗色 e2e 回归
**作为** QA
**我希望** 现有 5 条 e2e spec 在暗色态下仍 PASS，并新增 6 张暗色截图
**以便** 暗色上线有回归保障

**验收**:
- `npm run test:e2e` 5/5 PASS（浅 3 + 暗 2 已覆盖）
- 6 张暗色截图归档至 `docs/sprints/S27/screenshots/`：
  - `dark-home.png`（首页全屏）
  - `dark-browse.png`（browse 全屏）
  - `dark-detail.png`（detail 全屏）
  - `dark-sidebar.png`（browse 局部，聚焦左 sidebar）
  - `dark-apiguide.png`（api 文档页全屏）
  - `dark-favorite.png`（收藏页全屏）

## 5. 技术约束（硬性）

| 项 | 值 |
|----|----|
| 团队规约 | Vue 3.5 + Vite 7 + TS 5.8 + Ant Design Vue 4 |
| 新增依赖 | **无**（用 SCSS 原生 var() + Ant Design darkAlgorithm）|
| class 处理 | **不删不改**，只把硬编码颜色替换为 `var(--xxx)` |
| 主题切换 | 仅跟 `prefers-color-scheme` + 留 `data-theme="dark"` 钩子，**不引**切换按钮 |
| 暗色变量值 | 沿用 Ant Design 4 darkAlgorithm 默认 token（`#141414` / `#1668dc` 等成熟值）|
| chip 流 | **不动**（S25 已正确）|
| 后端 | **不跑 mvn**（无后端改动）|
| WCAG | 4.5:1（目测 + 截图为主，不写脚本）|

## 6. 关键决策

| 决策 | 理由 |
|------|------|
| 仅 CSS 变量 + Ant darkAlgorithm，**不引**颜色库 | 团队规约：最小依赖；var() 切换零运行时 |
| 不引切换按钮 | v1 范围；S28+ 再加用户偏好持久化 |
| chip 流不动 | S25 已通过 S26 e2e 验证，避免回归 |
| 变量值复用 Ant Design 4 darkAlgorithm | 视觉一致性 + 社区验证过的对比度 |
| 截图严格 6 张 | 任务书硬约束；多了浪费，少了缺证据 |

## 7. 风险与缓解

| 风险 | 缓解 |
|------|------|
| 组件硬编码颜色太多，逐个替换易漏 | 清单 7+ 文件已列；只替换，不重构 |
| 暗色文字对比度不达 AA | 沿用 Ant darkAlgorithm 默认值（已 ≥ 4.5:1）；截图目测 |
| 浅色基准线被破坏 | 浅色值仍写 `:root` 默认 + `data-theme="light"` 显式覆盖 |
| S26 e2e 在暗色下挂 | 暗色态下断言仅 `toBeVisible`，不锁像素；selector 不依赖颜色 |

## 8. DoD（Definition of Done）

- [ ] `docs/sprints/S27/requirements.md`（本文件）
- [ ] `docs/sprints/S27/dark-tokens.md`
- [ ] `docs/sprints/S27/handoff.md`（13 章节）
- [ ] `global.scss` 含浅色 + 暗色双套 CSS 变量
- [ ] `App.vue` 注入 `darkAlgorithm` 并响应 `prefers-color-scheme` / `data-theme`
- [ ] 7+ Vue 组件含 `var(--xxx)` 替换
- [ ] `npm run build` 0 错
- [ ] `npm run test:e2e` 5/5 PASS
- [ ] 6 张暗色截图归档
- [ ] 浅色基准线**未破**（S25 浅色 chip + S26 浅色 e2e 仍 PASS）
- [ ] commit co-author: `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`

## 9. 验收步骤（QA 执行）

1. `cd frontend && npm run build` — 期望 0 错
2. 启动后端 8767 + 前端 7777
3. `npm run test:e2e` — 期望 5/5 PASS
4. 截图 6 张 → `docs/sprints/S27/screenshots/`
5. 手测暗色态 5 视图：home / browse / detail / apiguide / favorite
6. 手测浅色态 5 视图：同上
7. Chrome DevTools → Rendering → Emulate `prefers-color-scheme: dark` 验证
