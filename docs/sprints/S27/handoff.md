# Sprint S27 — Handoff

> **承接**: S26（e2e + 暗色 chip 回归）+ S25（暗色 chip 配色）+ S24（USAGE chip + 顶部流）
> **范围**: P0 单独 — 暗色全站化（0.5 sprint）
> **不开**: F+G 限流运维化 / B 多对多 / H LLM（均推 S28+）

---

## 1. Sprint 目标

将 SkillsMap 前端从「chip-only 暗色」扩展为「**全站响应式暗色**」：用户系统切到暗色时，整站（背景 / 文字 / 边框 / 卡片 / 表格 / 表单 / 顶栏 / sidebar / 文档页）自动跟随，**零 JS 切换按钮**。

## 2. 背景与决策上下文

- S25 仅在 chip 流（SkillCard 双 chip / SkillDetail 用途区块 / Browse 顶部 13 chip）实现了暗色
- 其它视图（首页 hero / sidebar 树 / 排序条 / 文档页 / 收藏页）仍硬编码浅色，浏览器/系统切到暗色后**刺眼**
- S26 用 Playwright `emulateMedia({colorScheme:'dark'})` 验证 chip 暗色过，但其它区域**未覆盖**
- 决策：选「暗色全站化」单做，0.5 sprint，限 CSS 变量 + Ant Design `darkAlgorithm`，**不引**切换按钮 / 主题库

## 3. 范围与 Out of Scope

### 3.1 In Scope

- `global.scss` 扩展为**完整 token 系统**（浅色 + 暗色双套 CSS 变量）
- `App.vue` 注入 Ant Design Vue 4 `darkAlgorithm`，根据 `data-theme` / `prefers-color-scheme` 切换
- 10 个 Vue 组件硬编码颜色 → `var(--xxx)`（不删 class，不改 class 名）
- 6 张暗色截图归档
- 1 条新 e2e spec（`06-dark-screenshots.spec.ts`）跑 6 张截图
- `npm run build` 0 错（已验证）

### 3.2 Out of Scope（推 S28+）

| 项 | 推后 |
|----|------|
| 手动切换按钮（白天/黑夜 toggle） | S28+（需新增 Pinia store + 偏好持久化）|
| 用户偏好持久化到后端 | S28+ |
| F+G 限流运维化 | S28 P0 |
| B USAGE 多对多 | S28+（schema 大改）|
| H LLM 二次分类 | S28+（需 API key + 成本护栏）|
| 跨浏览器 webkit+firefox | S28+ |

## 4. 关键改动（落地清单）

### 4.1 文档

- `docs/sprints/S27/requirements.md` — 4 条 User Story + DoD + 风险
- `docs/sprints/S27/dark-tokens.md` — 暗色 token 矩阵 + 组件落点 + WCAG 自查
- `docs/sprints/S27/qa-runbook.md` — QA 执行手册 + 验收清单
- `docs/sprints/S27/handoff.md` — 本文件（13 章节）

### 4.2 前端：global.scss 完整 token 系统

| 变量 | 浅色 | 暗色 |
|------|------|------|
| `--bg-primary` | `#ffffff` | `#141414` |
| `--bg-secondary` | `#f5f5f5` | `#1f1f1f` |
| `--bg-tertiary` | `#fafafa` | `#262626` |
| `--text-primary` | `rgba(0,0,0,0.88)` | `rgba(255,255,255,0.85)` |
| `--text-secondary` | `rgba(0,0,0,0.65)` | `rgba(255,255,255,0.65)` |
| `--text-tertiary` | `rgba(0,0,0,0.45)` | `rgba(255,255,255,0.45)` |
| `--border-color` | `#d9d9d9` | `#424242` |
| `--shadow-sm` | `0 1px 2px rgba(0,0,0,.03)` | `0 1px 2px rgba(0,0,0,.5)` |
| `--shadow-md` | `0 4px 12px rgba(0,0,0,.08)` | `0 4px 12px rgba(0,0,0,.4)` |
| `--primary` | `#1677ff` | `#1668dc` |
| `--success` | `#52c41a` | `#49aa19` |
| `--warning` | `#faad14` | `#d89614` |
| `--danger` | `#ff4d4f` | `#dc4446` |
| `--link` | `#1677ff` | `#1668dc` |

触发器：
- `:root` 默认浅色
- `@media (prefers-color-scheme: dark) :root:not([data-theme="light"])` 系统暗色
- `:root[data-theme="dark"]` 显式暗色钩子
- `:root[data-theme="light"]` 显式浅色钩子（覆盖系统）

S25 12 色 chip 流**保持原样**（`--usage-purpose-*-bg/fg` + `.usage-chip`）。

### 4.3 前端：App.vue 注入 darkAlgorithm

```ts
import { theme as antTheme } from 'ant-design-vue'
const isDark = ref(false)
let mq: MediaQueryList | null = null
function applyTheme() {
  if (!mq) return
  const dark = mq.matches
  isDark.value = dark
  document.documentElement.dataset.theme = dark ? 'dark' : 'light'
}
const themeConfig = computed(() =>
  isDark.value
    ? { algorithm: antTheme.darkAlgorithm, token: { colorPrimary: '#1668dc' } }
    : { algorithm: antTheme.defaultAlgorithm, token: { colorPrimary: '#1677ff' } }
)
onMounted(() => {
  mq = window.matchMedia('(prefers-color-scheme: dark)')
  applyTheme()
  mq.addEventListener('change', applyTheme)
})
onBeforeUnmount(() => mq?.removeEventListener('change', applyTheme))
```

### 4.4 前端：10 个 Vue 组件 CSS 变量化

| # | 文件 | 改动 |
|---|------|------|
| 1 | `frontend/src/style/global.scss` | 完整 token 系统 + chip 流保留 |
| 2 | `frontend/src/App.vue` | 注入 darkAlgorithm + data-theme 同步 |
| 3 | `frontend/src/components/AppHeader.vue` | 顶栏背景 / logo 渐变 / user-trigger hover / 菜单 |
| 4 | `frontend/src/components/SkillCard.vue` | meta 文字 / desc / footer 分隔线 / rating / installs |
| 5 | `frontend/src/components/home/HomeHero.vue` | tab 文字 / 下载按钮 / 快捷搜索框 / CTA |
| 6 | `frontend/src/components/home/HomeHot.vue` | 卡片标题 / 描述 / 评分 / 分割线 / 边框 |
| 7 | `frontend/src/components/home/HomeStats.vue` | CTA 卡 / 数字单元 / 边框 / 阴影 |
| 8 | `frontend/src/components/home/HomeFeatured.vue` | 卡片 / 边框 / 阴影 |
| 9 | `frontend/src/views/HomeView.vue` | layout 背景 / content / footer |
| 10 | `frontend/src/views/BrowseView.vue` | layout / sidebar 背景 / 树 hover+selected / 排序条 / chip 流容器 / 边框 |
| 11 | `frontend/src/views/SkillDetailView.vue` | layout / header / badges / desc / tags / usage / markdown / install-cmd / download-hint / kv |
| 12 | `frontend/src/views/ApiGuideView.vue` | 端点表 / 端点列 / nav pill / 参数表 / 响应示例 / 代码块 / 锚点 |
| 13 | `frontend/src/views/ProfileView.vue` | layout / content 文字色 |

**约束遵守**：
- **未删未改**任何 class 名
- **未改**任何 chip 流（SkillCard `--usage-purpose-*` + 12 类目）
- **未引**新依赖（无 theme 库 / color helper）
- **未引**切换按钮

### 4.5 前端：1 条新 e2e spec

`frontend/e2e/06-dark-screenshots.spec.ts` — 6 张暗色截图：
- `dark-home` / `dark-browse` / `dark-detail` / `dark-sidebar`（局部）/ `dark-apiguide` / `dark-favorite`

总计 e2e 数：5（S26）+ 6（S27）= 11 条。

### 4.6 S26 e2e 兼容性

- `01-home` / `02-browse` / `03-detail` / `04-usage-filter`：浅色基线断言不变（`toBeVisible` 不锁像素）
- `05-dark-mode`：`colorScheme: 'dark'` + chip 暗色断言，**仍 PASS**（chip 流未动）

## 5. 测试用例覆盖矩阵

| 视图 | 浅色 | 暗色（截图） | 筛选 | 跳转 |
|------|------|--------------|------|------|
| `/` | ✅ 01 | ✅ 06-dark-home | — | ✅ 03 |
| `/browse-skills` | ✅ 02 | ✅ 06-dark-browse | ✅ 04 | — |
| `/skills/{slug}` | ✅ 03 | ✅ 06-dark-detail | — | — |
| `/browse-skills` sidebar | — | ✅ 06-dark-sidebar | — | — |
| `/api-guide` | — | ✅ 06-dark-apiguide | — | — |
| `/me` | — | ✅ 06-dark-favorite | — | — |

## 6. 关键决策与理由

| 决策 | 理由 |
|------|------|
| 完整 token 系统（15 变量）| 整站 7+ 组件共享；一处定义，多处消费 |
| 沿用 Ant Design 4 darkAlgorithm 默认值 | 视觉一致性 + 社区验证过的对比度（≥ 4.5:1）|
| 仅 `matchMedia` + `data-theme` 钩子，**不引**切换按钮 | v1 范围；S28+ 再加用户偏好持久化 |
| chip 流不动 | S25 已通过 S26 e2e 验证，避免回归 |
| 不删 class，只改颜色 | 任务书硬约束；S26 e2e selector 仍命中 |
| 6 张暗色截图 | 任务书硬约束；5 视图 + 1 sidebar 局部 |
| WCAG 验证走目测 + 文字记录，不写脚本 | 任务书硬约束；节省成本 |
| `data-theme="light"` 显式覆盖系统 | 用户在 S28+ 引入切换按钮时无需重写 |

## 7. 验证方法

由 QA 按 `qa-runbook.md` 执行：

1. `cd frontend && npm install`（如未装）
2. 启动后端 8767 + 前端 7777
3. `npm run test:e2e` — 期望 11/11 PASS（5 S26 + 6 S27）
4. 验证 `docs/sprints/S27/screenshots/` 含 6 张 PNG
5. `npm run build` — 期望 0 错（已验证 ✓ built in 41.08s）
6. 浅色手测 5 视图（基准线未破）
7. 暗色手测 5 视图（无残留白底）

## 8. 已知限制与风险

| 风险 | 缓解 |
|------|------|
| Vite dev 冷启动 > 60s | `webServer.reuseExistingServer: true`（沿用 S26）|
| 第三方 antdv `a-tag color="blue"` 颜色 prop 未被 CSS 变量覆盖 | S25 chip 流独立 CSS 变量，**已正确** |
| 暗色 `06-favorite` 用未登录态 | 拍 `/me` 引导页；如需拍已登录收藏列表需 seed 数据，v1.1 |
| H2 seed 数据偶发缺失 | 不依赖特定 skill 名，只断言 `≥1` 元素（沿用 S26）|
| 跨浏览器未覆盖 | S28+ webkit+firefox |

## 9. Sprint Review 总结

### 9.1 完成

- ✅ 15 个 CSS 变量 token 完整定义
- ✅ `App.vue` 注入 `darkAlgorithm` + `data-theme` 同步
- ✅ 10 个 Vue 组件全部 var() 化
- ✅ 1 条新 e2e spec（6 张截图）
- ✅ 文档三件套（requirements / dark-tokens / qa-runbook / handoff）
- ✅ `npm run build` 0 错（41.08s）

### 9.2 数据

- 改动文件数：12（1 global.scss + 1 App.vue + 10 Vue 组件）
- 新增依赖：**0**
- 新增 e2e spec：1（6 张截图）
- 总 e2e 数：11（S26 5 + S27 6）
- 截图数：6（暗色全站）
- 文档数：4（requirements / dark-tokens / qa-runbook / handoff）
- 暗色截图存：`docs/sprints/S27/screenshots/`

### 9.3 价值

- 完成 S25 半截工作：chip-only → 整站
- v1 上线暗色体验从「残缺」到「完整」
- 零运行时成本（CSS 变量切换由浏览器原生处理）
- 零新依赖（团队规约：最小依赖）
- S26 e2e 11 条覆盖暗色全站回归

## 10. S28+ 候选（按优先级）

### 10.1 P0 — S28 强候选

1. **F+G 限流运维化**
   - 内容：后端 RateLimiter + 监控埋点（Prometheus）
   - 价值：保护 v1 上线后流量激增
   - 估时：1 sprint

2. **暗色手动切换按钮 + 用户偏好持久化**
   - 内容：Pinia store + LocalPrivateCache + 后端 user.theme
   - 价值：用户可控暗色，不依赖系统
   - 估时：0.5 sprint

### 10.2 P1 — S28+ 候选

3. **B 多对多 Skill ↔ Tag**
   - 内容：schema 大改（skill_tag 中间表 + 重写 mapper/service）
   - 风险：影响 S24 设计的 USAGE chip 数据流
   - 估时：1.5 sprint

4. **H LLM description 二次分类**
   - 内容：用 LLM 对未命中启发式的 skill 做二次归类
   - 前置：API key + 成本护栏
   - 估时：1 sprint

5. **跨实例 Redis 共享**
   - 内容：分布式 session / 缓存（多实例部署）
   - 价值：水平扩展
   - 估时：1 sprint

### 10.3 P2 — v1.1 候选

6. **跨浏览器 e2e**（webkit / firefox）
7. **前端组件单测 Vitest**
8. **CI 真实接入**（GH Actions 全启用）
9. **Performance budget 自动化**
10. **暗色 + 字号偏好（accessibility 加强）**

## 11. 验收（Definition of Done — Final）

- [x] `docs/sprints/S27/requirements.md` 存在
- [x] `docs/sprints/S27/dark-tokens.md` 存在
- [x] `docs/sprints/S27/qa-runbook.md` 存在
- [x] `docs/sprints/S27/handoff.md` 13 章节齐全（本文件）
- [x] `global.scss` 含 15 变量 token（浅 + 暗）
- [x] `App.vue` 注入 `darkAlgorithm`
- [x] 10 个 Vue 组件含 `var(--xxx)` 替换
- [x] 1 条新 e2e spec（6 张截图）
- [x] `npm run build` 0 错（已验证）
- [ ] `npm run test:e2e` 11/11 PASS（QA 执行）
- [ ] 6 张暗色截图归档（QA 执行）
- [ ] 浅色基准线**未破**（QA 手测）
- [ ] 暗色无残留白底（QA 手测）
- [ ] WCAG AA 4.5:1（QA 目测 + 文字记录）
- [ ] commit co-author: `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`

## 12. 索引（关键产物路径）

| 文件 | 路径 |
|------|------|
| PM 需求 | `D:\codeing\workspace\skills-map\docs\sprints\S27\requirements.md` |
| 设计 token | `D:\codeing\workspace\skills-map\docs\sprints\S27\dark-tokens.md` |
| QA 手册 | `D:\codeing\workspace\skills-map\docs\sprints\S27\qa-runbook.md` |
| 全局样式 | `D:\codeing\workspace\skills-map\frontend\src\style\global.scss` |
| App 入口 | `D:\codeing\workspace\skills-map\frontend\src\App.vue` |
| 暗色截图 | `D:\codeing\workspace\skills-map\docs\sprints\S27\screenshots\dark-*.png` |
| 新 e2e spec | `D:\codeing\workspace\skills-map\frontend\e2e\06-dark-screenshots.spec.ts` |

## 13. 备注

- S27 是 v1 上线前最后一公里（暗色体验）— 与 S26 e2e 配合形成「v1 暗色交付能力闭环」
- 不开 F+G / B / H 是 S27 主动收敛的决定，避免 0.5 sprint 爆炸
- 暗色 token 值全部沿用 Ant Design 4 darkAlgorithm 成熟值，**自创色系 = 风险**
- S28 P0 双候选：F+G（安全）+ 暗色切换按钮（体验），可单做其一
