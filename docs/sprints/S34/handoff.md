# S34 收尾 — 分类列表重设计（弃 chip-as-title，升级大卡片 Grid）

> **Sprint**: S34
> **类型**: 视觉重设计（UI redesign）
> **上游反馈**: "用途分类（USAGE）和职业技能（SOC）两个页面的列表中，不应该使用标签/chip 的方式显式"
> **方案**: A 大卡片 Grid（推荐）
> **结果**: ✅ 实施完成，build 双绿
> **日期**: 2026-06-13

---

## 1. 改动文件清单

### 1.1 新增组件 (2 文件)

| 文件 | 用途 |
|------|------|
| `frontend/src/components/UsageCategoryGrid.vue` | USAGE 列表卡片，icon-tile 用 USAGE 12 色（按 parentCode 取自 `usage-colors.ts`），AimOutlined |
| `frontend/src/components/OccupationCategoryGrid.vue` | SOC 列表卡片，icon-tile 统一蓝色（与 S32 chip-occupation 一致：#E6F4FF / #0958D9），保留 S32 序号大字装饰，ToolOutlined |

### 1.2 修改 (1 文件)

| 文件 | 变更摘要 |
|------|---------|
| `frontend/src/views/CategoryView.vue` | 替换 `<OccupationCard>` 为 `<UsageCategoryGrid>` / `<OccupationCategoryGrid>`（按 `dim` 路由切换）；删除孤立的 `.grid` CSS 规则；新增 `onSelect(cat)` emit handler → `router.push('category-browse')` |

### 1.3 文档 (3 文件)

| 文件 | 用途 |
|------|------|
| `docs/sprints/S34/plan.md` | Sprint 计划（问题 + 方案对比 + DoD + 风险） |
| `docs/sprints/S34/design-redesign.md` | 视觉 token 归档（圆角 / 阴影 / 配色 / hover / dark / 响应式 / a11y / reduced-motion） |
| `docs/sprints/S34/handoff.md` | 本文件 |

---

## 2. 视觉对比（前后）

### 2.1 Before（旧 OccupationCard）
- 88px 灰色序号大字 + `<UsageChip size="lg">` **chip 当 title** + 数字
- 用户反馈：chip / tag / pill 这种"短标签"形态，缺少列表感

### 2.2 After（新 grid）
- 卡片圆角 16px、padding 20px、min-height 132-156px
- 48px icon-tile（USAGE 12 色 / SOC 蓝）+ AimOutlined/ToolOutlined 矢量图标
- 标题（17px / 700）+ parentCode（11px mono）双行
- 副标题（13px / 单行省略）— 有 description 用 description，否则兜底"X 个细分用途"
- 底栏：count + 箭头（hover 平移 3px + 主题色）
- Stagger 进入动画（40ms 间隔），reduced-motion 时禁用
- a11y：role="list" + role="listitem" + aria-label + Enter/Space 键盘可达 + focus ring
- 暗色态：背景 #1c1c1f + 序号大字 4% 透明度 + SOC icon-tile 切 16% alpha 软底

---

## 3. 反模式自检（CLAUDE.md 规约）

- ✅ 不引入新依赖（仅复用 Ant Design Vue 4 `a-empty` + `@ant-design/icons-vue` 已装包）
- ✅ 沿用 `usage-colors.ts` 的 12 色 USAGE + SOC 配色（零新增 token）
- ✅ 不修改 `UsageChip.vue`（grep 验证引用点仍是 3 处：SkillCard / SkillDetailView / 旧 OccupationCard）
- ✅ 不修改 `SkillCard.vue` / `SkillDetailView.vue`（chip 在那里仍合适，chip-as-title 是列表页的病，不是 chip 的病）
- ✅ 后端 API 不动；只换前端渲染
- ✅ TS 类型完整（props 强类型 + emit signature）
- ✅ a11y：role / aria-label / focus-visible / 键盘事件齐全
- ✅ reduced-motion：动画禁用 + hover translateY 禁用

---

## 4. DoD 对照

| DoD | 状态 |
|-----|------|
| CategoryView.vue 在 USAGE/SOC 两个模式渲染新卡片 | ✅ `<UsageCategoryGrid v-else-if="!isSoc" />` + `<OccupationCategoryGrid v-else />` |
| 视觉 token 按 design-redesign.md 实施 | ✅ 圆角 16px / 阴影静态+hover / icon-tile 48 / 配色 / hover / dark |
| 12 色 USAGE 配色仍生效 | ✅ 从 `usage-colors.ts` 按 parentCode 取色，inline 注入 `--tile-bg/fg` |
| SOC 蓝色与 S32 chip-occupation 一致 | ✅ #E6F4FF / #0958D9（light），rgba(96,165,250,0.16) / #93c5fd（dark） |
| a11y 完整 | ✅ role / aria-label / focus ring / Enter + Space 键盘可达 |
| 响应式 4 / 2 / 2 | ✅ `@media (max-width: 960px)` 2 列 + `<480px` 仍 2 列（避免单列过长） |
| reduced-motion | ✅ `@media (prefers-reduced-motion)` 禁用 stagger + hover translate |
| `npm run build` EXIT 0 | ✅ 18.44s |
| SkillCard / SkillDetailView chip 不动 | ✅ git diff 无这两个文件 |
| UsageChip.vue 不动 | ✅ git diff 无此文件 |

---

## 5. 验证

- `cd frontend && npm run build` → `✓ built in 18.44s`（EXIT 0）
- 类型检查：未单独跑 `vue-tsc --noEmit`，build 内含 `vue-tsc` 检查，无 type 错误
- 视觉：建议人工跑一遍
  - `/categories`（USAGE 列表）→ 12 张大卡片，icon-tile 各自配色（开发=青绿、商业=琥珀、工具=蓝灰…）
  - `/occupations`（SOC 列表）→ 卡片蓝色 icon-tile + 序号大字装饰
  - hover 卡片抬升 + 箭头位移 + 边框变 indigo；focus 显示 outline
  - 移动端 480px 以下仍 2 列

---

## 6. 风险 / 遗留

- `OccupationCard.vue` 现在没人调用（grep 仅 SkillDetailView 注释提及），可后续删除；本次保留以免破坏其他潜在引用
- 副标题暂未从后端 `Category.description` 拿到（API 返回的 Category 含 description 字段，部分有值）；无 description 时显示"X 个细分用途"兜底文案
- icon-tile 颜色注入用 inline style `['--tile-bg' as string]` — TS 严格模式下需要断言，build 已通过
- CategoryView 删了 `.grid` CSS（孤儿），scoped 块干净

---

## 7. 关联

- 设计规范：`docs/sprints/S34/design-redesign.md`
- Sprint 计划：`docs/sprints/S34/plan.md`
- 前序 Sprint：S33（skillCount 显示修复）→ `docs/sprints/S33/handoff.md`
- 前序 Sprint：S32（chip 同 row + icon 区分）→ `docs/sprints/S32/handoff.md`

---

**Lead 签收**：可合并 `master`。S35+ 候选：删除废弃 OccupationCard.vue / 后端补全 Category.description。