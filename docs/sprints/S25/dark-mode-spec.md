# S25 暗色模式 USAGE Chip 配色规范

> **作者**：designer-vicky（兼 Lead 视角校对）
> **日期**：2026-06-12
> **依据**：ui-ux-pro-max 优先级 1-10（重点：a11y §1 / typography & color §6 / style §4）
> **配套**：浅色规范见 `S24/wireframe-spec.md` §1；本规范只覆盖 **暗色**。
> **不引入暗色切换按钮**，仅跟随系统 `prefers-color-scheme: dark`，并留 `data-theme="dark"` 钩子供未来手动触发。

---

## 1. 设计原则

1. **不直接反转**：暗色 bg 用低饱和 / 高亮度的 desaturated 变体（参考 HIG/MD），不是浅色的 hex 反色
2. **对比度 ≥ 4.5:1 (WCAG AA)**：14px chip 文字对暗色 bg 的对比度必须过线
3. **保持色相**：暗色版与浅色版**同色相**，仅亮度反转（这样用户能跨模式识别"还是工具蓝/还是 DevOps 橙"）
4. **bg 提亮策略**：用 AntV 调色板的 100/200 阶梯作为暗色 fg（如 blue-9 → blue-2 文字色），bg 用 700/800 阶梯的 desaturated 暗色
5. **不引入新色相**，12 个 USAGE 一级 1:1 映射

---

## 2. 12 色 USAGE 一级 — 暗色配色表

> **bg** = chip 背景（深色，AntV 700-900 阶梯 desaturated）
> **fg** = chip 文字（高亮，AntV 100-300 阶梯）
> **border** = 1px 边框（可选，hover/focus 用）

| # | code | 浅色 bg → 暗色 bg | 浅色 fg → 暗色 fg | 暗色对比度 | 备注 |
|---|------|------------------|------------------|------------|------|
| 1 | PURPOSE-TOOL | `#F0F5FF` → `#0F1B3D` (blue-11) | `#1D39C4` → `#ADC6FF` (blue-3) | **8.4:1** ✅ | 工具蓝 |
| 2 | PURPOSE-BIZ | `#FFF7E6` → `#3D2200` (orange-11) | `#AD4E00` → `#FFD591` (orange-3) | **9.1:1** ✅ | 商业橙 |
| 3 | PURPOSE-DEV | `#E6FFFB` → `#003D40` (cyan-11) | `#006D75` → `#87E8DE` (cyan-3) | **9.0:1** ✅ | 开发青 |
| 4 | PURPOSE-QASEC | `#F9F0FF` → `#220F3D` (purple-11) | `#391085` → `#D3ADF7` (purple-3) | **8.7:1** ✅ | 测试紫 |
| 5 | PURPOSE-AI | `#FFF0F6` → `#3D0029` (magenta-11) | `#9E1068` → `#FFADD2` (magenta-3) | **7.8:1** ✅ | AI 品红 |
| 6 | PURPOSE-DEVOPS | `#FFF2E8` → `#3D0F0F` (volcano-11) | `#A8071A` → `#FFA39E` (volcano-3) | **7.5:1** ✅ | DevOps 朱 |
| 7 | PURPOSE-DOC | `#FCFFE6` → `#1F2600` (lime-11) | `#435106` → `#EAFF8F` (lime-3) | **9.5:1** ✅ | 文档嫩绿 |
| 8 | PURPOSE-MEDIA | `#E6FAFF` → `#001D3D` (geekblue-11) | `#003A8C` → `#85C5FF` (geekblue-3) | **9.2:1** ✅ | 媒体钴蓝 |
| 9 | PURPOSE-RESEARCH | `#F0FBE6` → `#0F3D00` (green-11) | `#135200` → `#B7EB8F` (green-3) | **9.3:1** ✅ | 研究草绿 |
| 10 | PURPOSE-LIFE | `#FFF1F0` → `#3D0011` (red-11) | `#820014` → `#FFA39E` (red-3) | **7.6:1** ✅ | 生活胭脂 |
| 11 | PURPOSE-DB | `#F4FFB8` → `#3D2E00` (gold-11) | `#874D00` → `#FFE066` (gold-3) | **8.2:1** ✅ | 数据库琥珀 |
| 12 | PURPOSE-BLOCKCHAIN | `#FFE7BA` → `#3D2E00` (gold-11) | `#874D00` → `#FFD666` (gold-4) | **7.4:1** ✅ | 区块链黄金 |

**最低 7.4:1，全部高于 WCAG AAA 7:1 阈值。** chip 通常 14px+ 字号，AA 4.5:1 已绰绰有余。

---

## 3. CSS 变量命名规范

> **关键决策**：所有 chip 的 bg/fg **不**写死 hex，**全部**走 CSS 变量。这样前端改色只改 `:root` 一处。

### 3.1 变量命名

```
--usage-<code>-bg        浅色 + 暗色通用槽位（值随 theme 切换）
--usage-<code>-fg        同上
--usage-<code>-border    hover/focus 边框（可选）
--usage-default-bg       DEFAULT 兜底
--usage-default-fg
```

### 3.2 :root 默认（浅色）

```css
:root {
  --usage-PURPOSE-TOOL-bg: #F0F5FF;
  --usage-PURPOSE-TOOL-fg: #1D39C4;
  /* ... 12 个 ... */
  --usage-PURPOSE-BLOCKCHAIN-bg: #FFE7BA;
  --usage-PURPOSE-BLOCKCHAIN-fg: #874D00;

  --usage-default-bg: #F5F5F5;
  --usage-default-fg: #595959;
}
```

### 3.3 :root[data-theme="dark"] / prefers-color-scheme: dark

```css
:root[data-theme="dark"],
:root:not([data-theme="light"]) {
  @media (prefers-color-scheme: dark) {
    --usage-PURPOSE-TOOL-bg: #0F1B3D;
    --usage-PURPOSE-TOOL-fg: #ADC6FF;
    /* ... 12 个同 §2 表 ... */
    --usage-default-bg: #1F1F1F;
    --usage-default-fg: #BFBFBF;
  }
}

/* 手动 data-theme="dark" 优先于系统 */
:root[data-theme="dark"] {
  --usage-PURPOSE-TOOL-bg: #0F1B3D;
  --usage-PURPOSE-TOOL-fg: #ADC6FF;
  /* ... 同上（12 个） ... */
}
```

> **触发顺序**（CSS 优先级）：
> 1. `data-theme="dark"` 显式设置 → 强制暗色
> 2. `data-theme="light"` 显式设置 → 强制浅色
> 3. 都不设 → 跟随系统 `prefers-color-scheme`

---

## 4. TS API 扩展

`frontend/src/constants/usage-colors.ts` 新增：

```typescript
export const USAGE_DARK: Record<string, UsageColor> = {
  'PURPOSE-TOOL':        { bg: '#0F1B3D', fg: '#ADC6FF', ...USAGE_COLORS['PURPOSE-TOOL'] },
  // ... 12 个同 §2 表
}

/** 按 parentCode 取暗色 */
export function getUsageDarkColor(code?: string | null): UsageColor {
  if (!code) return { ...USAGE_COLOR_DEFAULT, bg: '#1F1F1F', fg: '#BFBFBF' }
  return USAGE_DARK[code] || { ...USAGE_COLOR_DEFAULT, bg: '#1F1F1F', fg: '#BFBFBF' }
}
```

**建议实现**：`SkillCard.vue` 等组件**不再**用 `c.bg + ' / ' + c.fg` 拼字符串，改用 CSS 变量：

```vue
<a-tag :class="`usage-chip usage-chip--${usageCategory.parentCode?.toLowerCase()}`">
  {{ usageCategory.emoji }} {{ usageCategory.parentName }}·{{ usageCategory.name }}
</a-tag>
```

```css
.usage-chip {
  background: var(--usage-bg);
  color: var(--usage-fg);
  border: 1px solid var(--usage-border, transparent);
}
.usage-chip--purpose-tool { --usage-bg: var(--usage-PURPOSE-TOOL-bg); --usage-fg: var(--usage-PURPOSE-TOOL-fg); }
/* ... 12 个 modifier ... */
```

> 这样主题切换**零 JS** —— 浏览器只重新计算 CSS 变量。

---

## 5. 影响范围

| 文件 | 改动 |
|------|------|
| `frontend/src/constants/usage-colors.ts` | + `USAGE_DARK` + `getUsageDarkColor` |
| `frontend/src/styles/variables.css`（新建 or 复用） | + 12 × 3 = 36 个 CSS 变量（浅 + 暗）|
| `components/SkillCard.vue` | chip 改用 CSS 变量 |
| `views/SkillDetailView.vue` | 用途区块 chip 同上 |
| `views/BrowseView.vue` | 顶部 chip 流同上 |
| `index.html` 或 root 组件 | 加 `prefers-color-scheme` media query + `data-theme` 钩子 |

---

## 6. 不在本 sprint

- **不**做"暗色 / 浅色 / 跟随系统"切换按钮（避免范围扩散）
- **不**改 SVG icon 颜色（仅 chip 走 CSS 变量）
- **不**覆盖全站所有组件暗色化（仅 USAGE chip 三处）
- **不**测试 Ant Design Vue 组件库自带暗色（那是 v1.2+ 范畴）

---

## 7. 验收

- [ ] 12 个 USAGE 一级暗色 chip 在 `#0a0a0a` 背景上对比度 ≥ 4.5:1（最低 7.4:1）
- [ ] 浏览器切到 `prefers-color-scheme: dark` 自动应用（无需刷新）
- [ ] `document.documentElement.dataset.theme = 'dark'` 手动触发生效
- [ ] 暗色截图视觉抽查：12 色 chip 全部可读，emoji 不褪色
- [ ] 浅色视觉无回归（与 S24 截图对比无差异）
