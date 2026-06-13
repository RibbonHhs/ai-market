# Sprint S26 — E2E 自动化冒烟需求 (Playwright)

> **Sprint**: S26
> **范围**: E 单独 —— Playwright 自动化 + e2e 冒烟（0.5 sprint）
> **不开**: F+G 限流运维 / B 多对多 / H LLM 二次分类（均推 S27+）
> **承接**: S25（暗色 chip + 配色对齐）+ S24（USAGE chip + Browse 顶部流）

---

## 1. 背景与目标

### 1.1 背景

S25 完成暗色模式适配后，前端已有：

- 浅色 / 暗色双主题（`prefers-color-scheme`）
- USAGE chip 配色规范（`usage-colors.ts`）
- Browse 顶部 USAGE 筛选流
- SkillCard / SkillDetail / HomeHero 三大核心视图稳定

但**所有验收仍依赖人工冒烟 + 截图**，未自动化。回归成本随视图增多线性上升。

### 1.2 目标

为 v1 上线前最后一次冲刺交付一套**轻量、可重复、CI-ready（v1 留接口）**的端到端冒烟用例，覆盖首页 / Browse / Detail / 筛选 / 暗色模式五大关键路径，并产出 5 张可归档的视觉证据。

### 1.3 非目标

- 不做组件级单测（前端 Vitest 留 v1.1）
- 不做性能 / 负载测试
- 不接真实 CI（`.github/workflows/e2e.yml` 仅留接口空壳，注释说明）
- 不覆盖跨浏览器（webkit/firefox 留 v1.1）
- 不改后端任何代码

---

## 2. User Stories（5 条）

### US-1 首页加载与 Tab 切换

> **作为** 访客
> **我想** 打开首页看到 SkillsMap 介绍并切换 Tab
> **以便** 在人类/智能体两种视角间快速定位入口

**验收**：
- 访问 `/` 返回 200
- `[data-testid="home-hero"]` 存在且可见
- 点击 `[data-testid="home-tab-agent"]` 后，对应面板渲染（DOM 含 tab 内容关键字）
- 默认 Tab 为"人类"（`home-tab-human`）

### US-2 Browse 列表与卡片渲染

> **作为** 浏览者
> **我想** 看到 Skill 卡片列表
> **以便** 快速浏览全部可用 Skill

**验收**：
- 访问 `/browse-skills` 返回 200
- `[data-testid="skill-grid"]` 存在
- `[data-testid="skill-card"]` 数量 ≥ 5（H2 seed 数据保证）
- 每张卡片含 `[data-testid="skill-usage-chip"]`

### US-3 首页 → 详情页跳转

> **作为** 浏览者
> **我想** 点击首页卡片进入详情
> **以便** 阅读 Skill 的完整描述与用途

**验收**：
- 从首页点击第一张 SkillCard 后 URL 变为 `/skills/{id}`
- 详情页 `[data-testid="skill-detail"]` 存在
- `[data-testid="skill-usage-block"]` 可见且包含至少一个 chip

### US-4 Browse 顶部 USAGE 筛选

> **作为** 浏览者
> **我想** 按用途筛选 Skill
> **以便** 缩小范围找到目标

**验收**：
- `[data-testid="usage-filter"]` 存在
- 点击 `[data-testid="usage-filter-tool"]` 后列表重新渲染
- 重新渲染后 `[data-testid="skill-card"]` 数量 ≤ 全量（断言变少或相等，不变多）

### US-5 暗色模式双视图

> **作为** 暗色偏好用户
> **我想** 自动获得暗色主题
> **以便** 长时间浏览不刺眼

**验收**：
- `page.emulateMedia({ colorScheme: 'dark' })` 后：
  - 首页背景对比文本满足可读性（无纯黑底 + 纯灰字）
  - 详情页 chip 在暗色下仍清晰
- 截图归档 `docs/sprints/S26/screenshots/dark-home.png` / `dark-detail.png`

---

## 3. 验收标准（Definition of Done）

| 维度 | 标准 |
|------|------|
| 用例数 | **严格 5 条**（不多不少） |
| 通过率 | `npm run test:e2e` 5/5 PASS |
| 构建 | `npm run build` 0 错 0 警告 |
| 选择器 | 4 个 Vue 文件均含 `data-testid`，**未删未改** 现有 class |
| 截图归档 | `docs/sprints/S26/screenshots/` 含 5 张 PNG（浅 3 + 暗 2） |
| 依赖 | `@playwright/test` **pin 1.48**，仅装 `chromium` |
| 包脚本 | `package.json` 含 `test:e2e` + `test:e2e:ui` |
| CI 钩子 | `.github/workflows/e2e.yml` 空壳存在（注释说明 v1 不接） |

---

## 4. Out of Scope（S26 不做）

| 任务 | 推后到 |
|------|--------|
| F+G 限流运维化 | S27 |
| B 多对多 Skill ↔ Tag | S27+（schema 大改） |
| H LLM description 二次分类 | S27+（需 API key + 成本护栏） |
| 暗色全站化（仅 chip 已暗） | v1.1 整站主题 |
| 跨浏览器（webkit/firefox） | v1.1 |
| 前端组件单测（Vitest） | v1.1 |
| CI 真实接入 | v1.1（GH Actions 留空壳） |
| Performance / Load 测试 | v2+ |

---

## 5. 风险与缓解

| 风险 | 缓解 |
|------|------|
| Playwright 1.49 改 `browserType` API | **pin 1.48** |
| Vite dev server 冷启动慢（>60s） | `webServer.timeout: 60000` + `reuseExistingServer: true` |
| 截图差异导致 flaky | 浅色截首页 / browse / detail 三张已覆盖关键视图，断言不依赖像素 |
| H2 seed 数据偶发缺失 | 不依赖特定 skill 名，只断言 `≥5` 张卡片 |
| 后端 8767 未启动 | `webServer` 仅托管 Vite dev，后端假设已起（沿用 S25 现状） |

---

## 6. 交付物清单

- [ ] `frontend/playwright.config.ts`
- [ ] `frontend/e2e/01-home.spec.ts`
- [ ] `frontend/e2e/02-browse.spec.ts`
- [ ] `frontend/e2e/03-detail.spec.ts`
- [ ] `frontend/e2e/04-usage-filter.spec.ts`
- [ ] `frontend/e2e/05-dark-mode.spec.ts`
- [ ] `frontend/package.json` 新增 scripts
- [ ] `frontend/src/components/SkillCard.vue` 加 data-testid
- [ ] `frontend/src/views/BrowseView.vue` 加 data-testid
- [ ] `frontend/src/views/SkillDetailView.vue` 加 data-testid
- [ ] `frontend/src/views/HomeHero.vue` 加 data-testid
- [ ] `docs/sprints/S26/screenshots/` 5 张 PNG
- [ ] `.github/workflows/e2e.yml` 空壳 + 注释
- [ ] `docs/sprints/S26/shot-list.md`（Designer Hand-off）
- [ ] `docs/sprints/S26/handoff.md`（Lead 收尾）

---

## 7. 时间盒

- Phase 1（PM + Designer Hand-off）：**已并行** —— 本文档 + shot-list
- Phase 2（Dev：Selectors → Playwright）：2 步串行
- Phase 3（QA 跑测 + 回归）：1 步
- Phase 4（Lead handoff）：1 步

总计 0.5 sprint。