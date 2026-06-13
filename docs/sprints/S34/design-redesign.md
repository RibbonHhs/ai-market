# S34 Design: 分类列表卡片视觉规范

> Sprint: S34
> Owner: designer-vicky（vibe）
> Status: Approved
> Date: 2026-06-13
> 上游: `plan.md` §2 方案 A
> 设计哲学: 工具型 SaaS / 矢量图标优先 / WCAG AA / 同 row 节奏 / 沿用 S32 视觉资产

---

## 1. 方案对比回顾

| 方案 | 视觉特征 | 适用场景 | 本次决策 |
|------|---------|---------|---------|
| A 大卡片 Grid | icon-tile + 标题 + 描述 + 计数 + 箭头 | 12–25 项分类、需要"目录"感 | **采用** |
| B 表格行 | 全宽 row + 列对齐 | 20+ 项 admin 场景 | 备选 |
| C 手风琴 | 父级 + 折叠子级 | 父子层级深 | 备选 |

**选 A 理由**：保留 4 列 grid 节奏；与 hero 渐变色块气质一致；改造面最小；改造前后视觉对比强烈。

## 2. 视觉 Token

### 2.1 Card 容器

| Token | 值 | 说明 |
|-------|----|----|
| `--cat-card-radius` | `16px` | 与 S32 chip-lg 一致（节奏统一） |
| `--cat-card-bg` | `var(--bg-secondary)` | light `#ffffff`，dark `#1c1c1f` |
| `--cat-card-border` | `1px solid var(--border-color)` | light `#e5e7eb`，dark `#2d2d33` |
| `--cat-card-padding` | `20px 20px 18px` | 8dp rhythm |
| `--cat-card-min-height` | `132px`（桌面）/ `120px`（<600px） | 防 CLS |
| `--cat-card-shadow` | `0 1px 2px rgba(0,0,0,0.04), 0 2px 6px rgba(0,0,0,0.03)` | base elevation（低） |
| `--cat-card-shadow-hover` | `0 8px 24px -6px rgba(0,0,0,0.10)` | 升一级 |

### 2.2 Icon-tile

| Token | 值 | 说明 |
|-------|----|----|
| `--cat-tile-size` | `48×48px`（桌面）/ `44×44px`（mobile，满足 44pt 触控） | |
| `--cat-tile-radius` | `12px` | 内圆角小于卡片 |
| `--cat-tile-bg` | USAGE: `USAGE_COLORS[code].bg` / SOC: `#E6F4FF` | 浅色态 |
| `--cat-tile-bg-dark` | USAGE: `USAGE_DARK[code].bg` / SOC: `rgba(96,165,250,0.16)` | 暗色态 |
| `--cat-tile-fg` | USAGE: `USAGE_COLORS[code].fg` / SOC: `#0958D9` | 浅色态 |
| `--cat-tile-fg-dark` | USAGE: `USAGE_DARK[code].fg` / SOC: `#93c5fd` | 暗色态 |
| `--cat-tile-icon-size` | `24px`（桌面）/ `22px`（mobile） | AimOutlined / ToolOutlined |

### 2.3 文字层级

| 层级 | 字号 | weight | color | 备注 |
|------|------|--------|-------|------|
| Title | `clamp(16px, 1.4vw, 18px)` | 700 | `--text-primary` | 一级名 |
| Code mono | `11px` | 400 | `--text-tertiary` | parentCode（USAGE 才有） |
| Description | `13px` | 400 | `--text-secondary` | line-height 1.5；1 行省略；缺数据时显示 "细分用途" 兜底 |
| Count num | `14px` | 600 | `--text-primary` | tabular-nums |
| Count unit | `12px` | 400 | `--text-tertiary` | "个细分" |

### 2.4 交互态

| 状态 | 样式 |
|------|------|
| `:hover` | `transform: translateY(-2px); box-shadow: var(--cat-card-shadow-hover); transition: 200ms cubic-bezier(0.16, 1, 0.3, 1);` |
| `:focus-visible` | `outline: 2px solid var(--primary); outline-offset: 3px;` |
| `:active`（mobile） | `transform: translateY(0) scale(0.98); transition: 120ms ease-out;` |
| `prefers-reduced-motion` | `transform: none; box-shadow: static; animation: none;` |

### 2.5 配色映射

**USAGE（12 色，沿用 usage-colors.ts）**：

| code | light bg | light fg | dark bg | dark fg |
|------|---------|---------|---------|---------|
| PURPOSE-TOOL | #F0F5FF | #1D39C4 | rgba(96,165,250,0.16) | #93c5fd |
| PURPOSE-BIZ | #FFF7E6 | #AD4E00 | rgba(251,191,36,0.16) | #fcd34d |
| PURPOSE-DEV | #E6FFFB | #006D75 | rgba(52,211,153,0.16) | #6ee7b7 |
| PURPOSE-QASEC | #F9F0FF | #391085 | rgba(167,139,250,0.16) | #c4b5fd |
| PURPOSE-AI | #FFF0F6 | #9E1068 | rgba(244,114,182,0.16) | #f9a8d4 |
| PURPOSE-DEVOPS | #FFF2E8 | #A8071A | rgba(248,113,113,0.16) | #fca5a5 |
| PURPOSE-DOC | #FCFFE6 | #435106 | rgba(250,204,21,0.16) | #fde047 |
| PURPOSE-MEDIA | #E6FAFF | #003A8C | rgba(34,211,238,0.16) | #67e8f9 |
| PURPOSE-RESEARCH | #F0FBE6 | #135200 | rgba(132,204,22,0.16) | #bef264 |
| PURPOSE-LIFE | #FFF1F0 | #820014 | rgba(251,146,60,0.16) | #fdba74 |
| PURPOSE-DB | #F4FFB8 | #874D00 | rgba(148,163,184,0.16) | #cbd5e1 |
| PURPOSE-BLOCKCHAIN | #FFE7BA | #874D00 | rgba(217,119,6,0.16) | #fbbf24 |

**SOC（蓝色，统一）**：

| 主题 | bg | fg |
|------|----|----|
| light | `#E6F4FF` | `#0958D9` |
| dark | `rgba(96,165,250,0.16)` | `#93c5fd` |

> 与 S32 `.usage-chip--code-occupation` 完全一致（来源：global.scss:137 + 199–200）。

## 3. 布局

### 3.1 Grid

```css
.cat-grid {
  display: grid;
  gap: 20px;
  grid-template-columns: repeat(4, 1fr);
  /* ≥1024 */
}
@media (max-width: 1024px) {
  .cat-grid { grid-template-columns: repeat(2, 1fr); gap: 16px; }
}
@media (max-width: 600px) {
  .cat-grid { grid-template-columns: repeat(2, 1fr); gap: 12px; }
}
```

> 决策：手机仍 2 列，避免单列过长；breakpoint 一致性（与项目其他 grid 同步：≥1024 / ≥600 / <600）。

### 3.2 卡片内部布局

```
┌──────────────────────────────────────┐
│  ┌──────┐                            │
│  │ ICON │   工具  · PURPOSE-TOOL    │  ← icon-tile | title + code（mono）
│  │ 48px │   AI 编程、自动化、CLI     │  ← description（1 行省略）
│  └──────┘                            │
│  ──────────────────────────────────  │
│            16 个细分用途       →    │  ← 计数 + 箭头
└──────────────────────────────────────┘
```

- 头部：`[icon-tile 48] [title + code 行]`
- 描述：1 行省略（`text-overflow: ellipsis; white-space: nowrap;`）
- 分隔：1px `var(--border-color)` 横线
- 底栏：左对齐"X 个细分"，右对齐箭头 `→`

### 3.3 Stagger 进入

```css
.cat-card {
  --i: 0;
  animation: catFadeIn 320ms cubic-bezier(0.16, 1, 0.3, 1) backwards;
  animation-delay: calc(var(--i) * 40ms);
}
@keyframes catFadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to   { opacity: 1; transform: translateY(0); }
}
@media (prefers-reduced-motion: reduce) {
  .cat-card { animation: none; }
}
```

> stagger 40ms × 12 项 ≈ 480ms 总时长，落在 150–500ms 区间；最多卡在 8 项（避免最后一项 >500ms），超过 8 时封顶。

## 4. 可访问性

| 项 | 规范 |
|----|------|
| 容器 | `role="list"`（grid 容器），卡片 `role="listitem"` |
| 卡片交互 | 整卡 `<button>`（不是 `<a>`，因为目标路由动态决定；提供 `@click` + `@keydown.enter/space`） |
| a11y 标签 | `aria-label="用途分类：工具 · PURPOSE-TOOL，16 个细分"` |
| 图标 | `aria-hidden="true"`（装饰性，与文字组合传达） |
| 焦点 | `:focus-visible` 2px outline，与 S32 一致 |
| 键盘 | Tab 进入卡片 → Enter / Space 触发；与 router.push 兼容 |
| 颜色 vs icon | 同时用 icon 形状（AimOutlined / ToolOutlined）+ 颜色 + 文字前缀 — 不只靠颜色 |

## 5. 组件契约

### Props

```ts
defineProps<{
  categories: Category[]        // 后端返回的分类列表
  loading?: boolean             // 可选；true 时显示骨架
  emptyText?: string            // 可选；缺数据时显示
}>()
```

### Emits

```ts
const emit = defineEmits<{
  (e: 'select', category: Category): void
}>()
```

### 路由集成

组件本身**不**直接调用 `router.push`，而是 emit `select` 事件，由父组件（`CategoryView.vue`）决定跳转目标（保持组件解耦、可测试）。

## 6. 反模式（避免）

- ❌ 整卡变成 `<a>` 标签直接 push（破坏与侧栏树的一致性 — 侧栏树是 slug 跳转，卡片路由也是 slug，但 emit 模式利于后续扩展）
- ❌ 卡片内部再嵌 chip（避免视觉回到 chip-as-title）
- ❌ icon-tile 用 emoji（项目禁止 emoji 作结构图标）
- ❌ 12 色硬编码进组件（必须 import `usage-colors.ts` 的 token，保持单一来源）
- ❌ 卡片 hover 改 width/height（layout shift）— 仅 transform + shadow
- ❌ staggered 动画无 reduced-motion fallback

## 7. 与 S32 的关系

| 资产 | 是否沿用 |
|------|---------|
| 12 色 USAGE_COLORS | ✅ 直接 import |
| USAGE_DARK（暗色态） | ✅ 直接 import |
| SOC occupation 蓝 | ✅ 直接 import（与 S32 chip-occupation 一致） |
| AimOutlined / ToolOutlined | ✅ 继续用作 icon-tile |
| UsageChip.vue | ✅ **不改**（SkillCard / SkillDetailView 仍使用） |
| chip-as-title 模式 | ❌ **弃用**（仅本 Sprint 两个页面受影响） |

---

**设计稿结束。** Dev 可启动实施。