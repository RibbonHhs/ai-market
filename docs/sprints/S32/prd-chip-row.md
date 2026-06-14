# S32 PRD: 用途/职业 Chip 同 Row 展示 + Icon 区分

> Sprint: S32
> Owner: pm-alice
> Status: Draft v1
> Date: 2026-06-13
> 上游: S31 收尾（UsageChip.vue 已落地，emoji 占位 + 双 chip 同行布局已就位）

---

## 1. 背景与目标

S31 已交付 `UsageChip.vue` 组件，并在 `SkillCard.vue` 中将"职业 chip + 用途 chip"放入同一行 `.skill-card__categories` 容器。但当前 chip 内的类型区分仍使用 emoji（🎯 / 💼）：

- emoji 字体依赖（macOS / Windows / Linux / Android 渲染不一致）
- 与项目"工具型 SaaS 风格"不匹配
- WCAG 语义弱（emoji 不是可控矢量图标，无法做 a11y label 与对比度）
- 与项目"禁用 emoji 作结构化 icon"的规约冲突

**目标：**

1. 把 chip 内的类型 emoji 替换为 **Ant Design Vue icon**（结构化矢量图标）。
2. 三处入口（`SkillCard.vue` 卡片、`SkillDetailView.vue` 详情头、`OccupationCard.vue` 已仅含用途 chip 不变）展示行为保持一致：用途/职业**严格同 row**，窄屏下允许 wrap。
3. chip 内可访问性增强（icon 有 a11y 描述；不仅靠颜色区分）。

## 2. 用户故事

| ID | 角色 | 故事 | 验收点 |
|----|------|------|--------|
| US-1 | 浏览者 | 在浏览页看到每张 Skill 卡片，"用途分类"和"职业技能"两类信息**横向并排**显示 | 桌面端两者在同一行；窄屏（<360px）允许换行 |
| US-2 | 浏览者 | 通过不同的 icon 形状**快速识别**两类信息（不需要读字） | 用途 chip 左侧的 icon 与职业 chip 的 icon 视觉上明显不同 |
| US-3 | 详情访客 | 在 Skill 详情页头部，"职业技能"和"用途分类"两个 chip 也是**同一行** | 详情页 header 区域，职业 + 用途两个 chip 同行展示 |
| US-4 | 屏幕阅读器用户 | 听到 chip 时，icon + 文字组合传达完整语义 | chip 含 `aria-label` 包含"用途分类"或"职业分类"前缀 |
| US-5 | 设计/审稿人 | 在 dark 模式 / light 模式下对比度仍满足 WCAG AA（≥4.5:1） | chip 文字 vs 背景在两个主题下均 ≥4.5:1 |

## 3. 范围

### In Scope
- 重写 `frontend/src/components/UsageChip.vue`：
  - 新增 `kind: 'usage' | 'occupation'` prop（保留 `variant` 兼容别名，Dev 阶段统一迁移）
  - 类型区分用 `<AimOutlined />`（用途） vs `<ToolOutlined />`（职业）（具体听 Designer）
  - 移除所有 emoji 字符
  - 保留 `parentName / childName / parentCode / emoji / size / clickable / to / testid` 现有 prop
- 改 `frontend/src/components/SkillCard.vue`：保持 `.skill-card__categories` 的同 row flex 布局（已存在）
- 改 `frontend/src/views/SkillDetailView.vue`：在 `.badges` 行后增加"职业 chip + 用途 chip"**同一 row** 容器（目前仅有用途 chip，职业 chip 仍是普通 a-tag，需并入 chip 行）
- 改 `frontend/src/style/global.scss`：补 `.usage-chip--code-occupation` 在 dark 主题的对比度（如缺）
- `frontend/src/components/OccupationCard.vue`：本卡片语义上是"职业分类卡"，chip 仍为 usage 视角（因为显示的是 category.name → 对应"职业/分类"）；保持不变（如有歧义由 Designer 确认）

### Out of Scope
- 改后端 `SkillController` 字段
- 改 `usage-colors.ts` 12 色配色（仅做暗色对比度校验，不动 token）
- 改路由/筛选/搜索
- 引入新依赖（icon 来自已装 `@ant-design/icons-vue`）

## 4. 详细规范

### 4.1 组件契约（UsageChip.vue）

| Prop | 类型 | 默认 | 说明 |
|------|------|------|------|
| `kind` | `'usage' \| 'occupation'` | `'usage'` | 新增 prop，决定类型 icon 和是否显示 category emoji |
| `variant` | `'usage' \| 'occupation'` | `'usage'` | 兼容旧 prop，Dev 阶段统一迁移到 `kind`（保留期内双支持） |
| `parentCode` | `string \| null` | `null` | usage 必填；occupation 可空 |
| `parentName` | `string \| null` | `null` | 一级分类名 |
| `childName` | `string \| null` | `null` | 二级分类名（可空） |
| `emoji` | `string \| null` | `null` | usage 专属：分类前的小图标符号（不是类型 icon） |
| `size` | `'sm' \| 'md' \| 'lg'` | `'md'` | 卡片用 sm，详情用 md，独立卡用 lg |
| `clickable` | `boolean` | `false` | 是否可点击跳转 |
| `to` | `string \| RouteLocationRaw` | `''` | 跳转目标 |
| `testid` | `string` | `undefined` | e2e 钩子 |

### 4.2 视觉规范（具体听 Designer）
- **类型 icon**：
  - `kind="usage"` → `<AimOutlined />`（瞄准/目标 → 表示"用途"）
  - `kind="occupation"` → `<ToolOutlined />`（工具 → 表示"职业技能"）
- icon 尺寸：与字号成比例
  - `size="sm"` → icon 12px
  - `size="md"` → icon 14px
  - `size="lg"` → icon 16px
- icon 颜色：默认 `currentColor`（继承 chip 文字色），保持对比度
- a11y：chip 整体 `role="img"` 或保持默认 + `aria-label="用途分类：xxx"` / `aria-label="职业分类：xxx"`

### 4.3 布局规范（关键）

**SkillCard.vue** — `.skill-card__categories`（**已存在**，无需新增）：
```html
<div class="skill-card__categories">
  <UsageChip kind="occupation" ... />
  <UsageChip kind="usage" ... />
</div>
```
- CSS：`display: flex; gap: 6px; flex-wrap: wrap; align-items: center;`（**已正确**）
- 不再改 — 只把内部 emoji 换为 icon

**SkillDetailView.vue** — 需新增行：
- 位置：在 `.badges` 行下方、`.desc` 上方
- 容器：`<div class="detail__chips">`
- 内容：职业 chip + 用途 chip（顺序：先职业，再用途 → 与 SkillCard 一致）
- CSS：与 SkillCard 同结构（`display: flex; gap: 8px; flex-wrap: wrap;`）
- 现状：详情页 `.badges` 内"职业"用 `<a-tag color="blue">` 显示，且仅 categoryName — **需要迁出**到 chip 行
- 现状：详情页 header 内仅含 1 个用途 chip — **需新增职业 chip**到同 row

**OccupationCard.vue** — 不变（该卡只展示单个职业 chip）

### 4.4 响应式

| 断点 | 行为 |
|------|------|
| ≥ 768px（md+） | 两个 chip **必须同 row**（用 `flex-wrap: wrap` 但当容器足够时不换行） |
| 360–767px（sm） | 同 row 优先；容器不足时自然 wrap |
| < 360px（xs） | 允许换行；icon 仍区分（不丢 a11y） |

### 4.5 暗色模式
- chip 背景/前景在 `.dark` 类下对比度 ≥ 4.5:1
- icon 颜色 = `currentColor` 跟随 chip 前景
- 若现有 `--code-occupation` 变量在暗色下不达标，由 Designer 给出 fallback 颜色

## 5. 验收标准（E2E 视角）

| # | 场景 | 预期 |
|---|------|------|
| AC-1 | 打开 `/browse`，每张 SkillCard 同时存在职业 chip + 用途 chip | 两个 chip 在桌面端同 row；窄屏允许 wrap |
| AC-2 | 两个 chip 的类型 icon 形状不同 | 用途为瞄准形，职业为工具形（具体听 Designer） |
| AC-3 | 打开 `/skills/<slug>` 详情页 | 头部"职业 + 用途"两个 chip 在同 row；不再用 a-tag 展示职业名 |
| AC-4 | 切换暗色模式 | chip 文字对比度仍 ≥ 4.5:1 |
| AC-5 | 屏幕宽度压到 360px | 两个 chip 仍可见，icon 区分不丢失 |
| AC-6 | 视障辅助 | 按 Tab 到 chip 时，屏幕阅读器读出"用途分类：xxx"或"职业分类：xxx" |
| AC-7 | `npm run build` 通过 | 无 TS / 编译错误 |
| AC-8 | Playwright/DOM 探针（来自 S31 `verify-chips.mjs`） | chip 元素包含 `[data-testid="skill-soc-chip"]` / `[data-testid="skill-usage-chip"]` 仍存在 |

## 6. 风险与依赖

| 风险 | 缓解 |
|------|------|
| 现有 `SkillDetailView` 职业信息用 a-tag 显示，迁移到 chip 期间可能造成视觉差异 | 在 `.badges` 内保留职业 a-tag **作为 fallback** 时可考虑直接删除以避免重复（Dev 决策） |
| `variant` 与 `kind` prop 双轨期可能引起误用 | 文档明确推荐 `kind`；保留 `variant` 仅做兼容 |
| 暗色主题下 `usage-chip--code-occupation` 用 `#E6F4FF / #0958D9` 可能在 dark 反转后不达标 | 由 Designer 给出 dark 变体或校验；Dev 实施 |

## 7. Sprint 计划

| 阶段 | Owner | 产出 |
|------|-------|------|
| PRD | pm-alice | `prd-chip-row.md`（本文） |
| Design | designer-vicky | `design-chip-row.md`（icon 选型 + token） |
| Dev | dev-kevin | `UsageChip.vue` 重写 + 三处接入 |
| QA | qa-tina | `qa-chip-row.md` + `npm run build` + 冒烟 |
| 收尾 | agile-rd-lead | `handoff.md` 变更摘要 |

---

**PRD 结束。** Designer 阶段可启动。
