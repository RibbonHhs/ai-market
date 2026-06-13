# S34 Plan: 分类列表重设计（弃 chip-as-title，升级为大卡片 Grid）

> Sprint: S34
> Owner: agile-rd-lead
> Status: Approved
> Date: 2026-06-13
> 上游反馈: 用户 — "用途分类（USAGE）和职业技能（SOC）两个页面的列表中，不应该使用标签/chip 的方式显式，请 UI 重新设计一下展示风格"

---

## 1. 背景与问题

### 1.1 现状

- `/categories`（用途分类 USAGE）与 `/occupations`（职业技能 SOC）共用 `frontend/src/views/CategoryView.vue`，按 `route.meta.dim` 切换数据。
- grid 内每张 `<OccupationCard>` 当前视觉是：「88px 灰色序号大字」+ `<UsageChip size="lg">`（chip 当 title）+ 数字。
- 用户反馈：列表项使用 chip / tag / pill 这种"短标签"形态展示，缺乏列表感、信息层级扁平、不像"分类目录页"应有的样子。

### 1.2 设计目标

- 把 `/categories` 与 `/occupations` 两个页面的列表项从「chip 当标题」升级为完整大卡片
- 沿用 S32 已通过的 12 色 USAGE + SOC 蓝色 + AimOutlined/ToolOutlined 矢量图标体系（视觉资产零损失）
- 列表项可点击进入 `/category/:slug`（=BrowseView），不破坏现有路由
- WCAG AA、暗色态一致
- 父子层级、副标题、细分数量在卡片内可见

## 2. 方案选择

候选 A / B / C 三套方案（详见 design-redesign.md §1）：

| 方案 | 视觉 | 优点 | 缺点 |
|------|------|------|------|
| **A 大卡片 Grid（采用）** | icon-tile + title + parentCode + 描述 + 计数 + 箭头 | 改造面小、与 hero 渐变色块一致、信息密度高 | 信息密度低于表格 |
| B 表格行 | 单列全宽行 + 列对齐 | 扫描速度最快、admin 感 | 与产品气质反差 |
| C 手风琴 | 父级 + 展开子级 grid | 父子层级最清晰 | 与侧栏树冗余 |

**采用 A**：保留 4 列 grid 节奏，视觉升级最明显、风险最低。

## 3. 实施步骤

### Step 1 — 文档
- [ ] `docs/sprints/S34/plan.md`（本文件）
- [ ] `docs/sprints/S34/design-redesign.md`（视觉 token 归档）

### Step 2 — 新组件
- [ ] `frontend/src/components/UsageCategoryGrid.vue`：USAGE 列表卡片
- [ ] `frontend/src/components/OccupationCategoryGrid.vue`：SOC 列表卡片
- [ ] 两个组件 props：`categories: Category[]`、`loading?: boolean`、`emptyText?: string`
- [ ] 两个组件 emit：`select(category: Category)`
- [ ] 共同规范：响应式 4/2/2 列 grid、a11y、reduced-motion、stagger 进入

### Step 3 — 接入 CategoryView
- [ ] `frontend/src/views/CategoryView.vue` 替换 `<OccupationCard>` 为 `<UsageCategoryGrid>` / `<OccupationCategoryGrid>`
- [ ] 路由 / categories → UsageCategoryGrid；/occupations → OccupationCategoryGrid
- [ ] 保留 hero、计数、empty 状态
- [ ] 删除 `<OccupationCard>` 引用（`OccupationCard.vue` 文件保留作废，已无人调用）

### Step 4 — 验证
- [ ] `cd frontend && npm run build` EXIT 0
- [ ] `vue-tsc --noEmit` EXIT 0
- [ ] SkillCard / SkillDetailView / BrowseView 顶部 chip 流未被改动（grep 验证 UsageChip 使用点仍是 3 处）
- [ ] UsageChip.vue 文件本身未被改动
- [ ] 写 docs/sprints/S34/handoff.md

## 4. DoD（Definition of Done）

- [ ] `CategoryView.vue` 在 USAGE/SOC 两个模式下都渲染新卡片
- [ ] 视觉 token（圆角 / shadow / icon-tile / 配色 / hover / dark）按 design-redesign.md 实施
- [ ] 12 色 USAGE 配色仍生效（来自 usage-colors.ts，不新增 token）
- [ ] SOC 蓝色与 S32 chip-occupation 一致（#E6F4FF / #0958D9 light，rgba(96,165,250,0.16) / #93c5fd dark）
- [ ] a11y：role="list" + role="listitem" + aria-label + focus ring + 键盘可达
- [ ] 响应式：≥1024px 4 列 / ≥600px 2 列 / <600px 2 列（手机保持 2 列避免单列过长）
- [ ] reduced-motion：禁用 stagger + hover translateY
- [ ] build EXIT 0
- [ ] SkillCard / SkillDetailView chip 不动
- [ ] UsageChip.vue 不动
- [ ] handoff.md 写完

## 5. 风险与处理

| 风险 | 处理 |
|------|------|
| hero 渐变色块与卡片背景色块撞色 | 卡片用 `var(--bg-secondary)` 中性背景，仅 icon-tile 用 USAGE 12 色 |
| 副标题缺数据 | 后端 Category 无 description 字段，使用一级名 + 「X 个细分用途」兜底 |
| 移动端单列过高 | mobile 仍 2 列；卡片 min-height 120px；底部留 8px gap |
| color-only 区分信息 | icon-tile 形状（AimOutlined / ToolOutlined）+ aria-label "用途分类：xxx" / "职业分类：xxx" 双轨 |
| 暗色态 12 色 + SOC 与 S32 chip 不同步 | 复用 S32 已有的 USAGE_DARK（USAGE 暗色）+ occupation 蓝；不另写新 token |

## 6. 不在范围内（明确排除）

- ❌ 不修改 `UsageChip.vue`
- ❌ 不修改 `SkillCard.vue` / `SkillDetailView.vue`
- ❌ 不修改 `BrowseView.vue`（顶部 chip 流 + 侧栏树都不动）
- ❌ 不引入新依赖 / 新框架
- ❌ 不重写 Admin 后台分类管理 UI
- ❌ 不改后端 API

---

**Plan 结束。** Dev 可启动实施。