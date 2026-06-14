# S32 接力简报 — Lead → dev-kevin

> **来源**：用户截图反馈暗色系首页字体不可见（S30 / S31 修复后新发现）
> **目标**：拆分 `:root:not([data-theme="light"])` 复合选择器为简单媒体查询块
> **预计工时**：1 h

---

## 1. 用户反馈原文 + 截图分析

> "暗色系的首页字体都看不见了，详细看截图"

截图（`docs/sprints/S32/screenshots/user-feedback/01-home-hero-dark.png`）显示：

| 元素 | 期望（暗色态） | 实际（截图） | 状态 |
|------|--------------|------------|------|
| 页面 backdrop | `--bg-primary → --bg-secondary` 暗紫渐变 | 仍是 `#f3f0ff → #eef2ff` 浅紫 | ❌ 未切换 |
| `Skills Marketplace` 标题 | `var(--text-primary)` 浅白 | 白字 on 浅紫底 | ❌ 不可见 |
| 副标题 "发现与分享..." | `var(--text-secondary)` | 白字 on 浅紫底 | ❌ 不可见 |
| `__search-card` 容器 | `--bg-elevated` 暗紫 | `#2d2842` 暗紫 | ✅ 已切换 |
| `__agent` 容器 | 媒体查询覆盖 dark | 部分可见 | ⚠️ 可能未切 |
| `__agent-num` 1/2/3/4 | 紫底深字 | 部分可见 | ⚠️ 可能未切 |
| `__agent-cmd` pre | `--bg-elevated` 暗底 | 暗底白字（应该 OK）| ✅ |

**根因**：S30 用了 `:root:not([data-theme="light"])` 的"否定 + 媒体查询"复合选择器。Vue scoped CSS 处理后：
- 默认规则 specificity = (0, 3, 0) — `.home-hero[data-v-xxx] .home-hero__backdrop[data-v-xxx]`
- 暗色规则 specificity = (0, 3, 0) — `:root:not([data-theme="light"]) .home-hero__backdrop[data-v-xxx]`

**同 specificity** → 源序后者胜出（暗色规则写在后面，理论上应该胜）→ 但**部分浏览器/实际渲染中，scoped CSS 重写 + 复合否定选择器组合不稳定**，导致 backdrop / agent / agent-num 暗色覆盖不生效。

---

## 2. 待改：3 处 `:root:not([data-theme="light"])` 拆开

**唯一改动文件**：`frontend/src/components/home/HomeHero.vue`

### 2.1 L282-290 — `__backdrop`

**改前**：
```scss
&__backdrop {
  position: absolute;
  inset: 0;
  z-index: -1;
  background: linear-gradient(180deg, #f3f0ff 0%, #eef2ff 60%, transparent 100%);
  pointer-events: none;
}
@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) .home-hero__backdrop {
    background: linear-gradient(180deg, var(--bg-primary) 0%, var(--bg-secondary) 60%, transparent 100%);
  }
}
:root[data-theme="dark"] .home-hero__backdrop {
  background: linear-gradient(180deg, var(--bg-primary) 0%, var(--bg-secondary) 60%, transparent 100%);
}
```

**改后**：
```scss
&__backdrop {
  position: absolute;
  inset: 0;
  z-index: -1;
  background: linear-gradient(180deg, #f3f0ff 0%, #eef2ff 60%, transparent 100%);
  pointer-events: none;
}
@media (prefers-color-scheme: dark) {
  .home-hero__backdrop {
    background: linear-gradient(180deg, var(--bg-primary) 0%, var(--bg-secondary) 60%, transparent 100%);
  }
}
:root[data-theme="dark"] .home-hero__backdrop {
  background: linear-gradient(180deg, var(--bg-primary) 0%, var(--bg-secondary) 60%, transparent 100%);
}
```

> 关键：去掉 `:root:not([data-theme="light"])` 否定选择器，让 media query 独立工作。`data-theme="dark"` 钩子保持不变。

### 2.2 L492-501 — `__agent`

**改前**：
```scss
@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) .home-hero__agent {
    background: var(--bg-secondary);
    border-color: var(--border);
  }
}
```

**改后**：
```scss
@media (prefers-color-scheme: dark) {
  .home-hero__agent {
    background: var(--bg-secondary);
    border-color: var(--border);
  }
}
```

### 2.3 L547-552 — `__agent-num`

**改前**：
```scss
@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) .home-hero__agent-num {
    background: var(--primary);
    color: var(--text-inverse);
  }
}
```

**改后**：
```scss
@media (prefers-color-scheme: dark) {
  .home-hero__agent-num {
    background: var(--primary);
    color: var(--text-inverse);
  }
}
```

---

## 3. 验收清单

### 3.1 浅色态（默认 / data-theme=light）— 不破坏 v1 视觉
- [ ] `__backdrop` 仍是 `#f3f0ff → #eef2ff` 浅紫渐变
- [ ] `__agent` 仍是 `#faf5ff → #fff` 紫粉渐变
- [ ] `__agent-num` 仍是浅灰底（`--bg-tertiary` + `--text-primary`）
- [ ] 标题/副标题深色可读

### 3.2 暗色态（prefers-color-scheme: dark / data-theme=dark）
- [ ] `__backdrop` 切到 `--bg-primary → --bg-secondary` 暗紫渐变（**关键修复**）
- [ ] `__search-card` 暗紫（`--bg-elevated`）
- [ ] `__agent` 暗紫（`--bg-secondary`）
- [ ] `__agent-num` 1/2/3/4 紫底深字（`--primary` + `--text-inverse`）
- [ ] `__agent-cmd` pre 暗底白字
- [ ] 标题/副标题浅白字可读
- [ ] **页面整体视觉一致**（不再有"暗色页面 + 浅色 backdrop"的违和）

### 3.3 DoD
- [ ] 1 文件改动（仅 `HomeHero.vue`）
- [ ] 3 处 `:root:not()` 全部移除
- [ ] `cd frontend && npm run build` 通过
- [ ] Playwright 跑深色截图（暗 + 浅各 1 张）→ 暗色 backdrop 必须切到暗紫
- [ ] 8 张回归（home-hero × 2 + 之前 6 张）重跑确保 25/25 AA 仍过

---

## 4. 风险与缓解

| 风险 | 缓解 |
|------|------|
| 去掉 `:root:not()` 后若 data-theme=light，media query 仍会触发导致浅色态变暗 | 用户当前**没有 data-theme 切换器**（全局 grep 仅 SCSS 用，无 JS 设置 data-theme）。所以去掉否定是安全的。如果未来加切换器，那时再处理 |
| 浅色态 `__agent-num` 变紫（破坏 v1 视觉） | S30 决策：浅色态 num 不动（仍是 `--bg-tertiary`），只在暗色态覆盖 |
| Vue scoped CSS 改写 selector 出新坑 | 测试时跑 Playwright 三场景：浅色态 / 暗色态 / data-theme=dark 强制暗色态 |

---

## 5. 工时拆解

| 步骤 | 文件 | 预计 |
|------|------|------|
| Step 1: 改 3 处 `:root:not()` | HomeHero.vue | 15 min |
| Step 2: 跑 build 验证 | — | 5 min |
| Step 3: Playwright 暗色截图（自验） | — | 15 min |
| **总计 dev** | **1 文件** | **35 min** |
| Step 4: 派 qa-tina 跑回归 | — | 1-2 h |

---

## 6. 完成定义

- [ ] 3 处 `:root:not()` 全部移除
- [ ] `npm run build` 通过
- [ ] dev 自测 Playwright 暗色截图：backdrop 切到暗紫 ✅
- [ ] qa-tina 跑 wcag-regression.md → 25/25 仍过 AA
- [ ] PR 标题：`fix(S32): home-hero dark mode backdrop + agent selectors`

---

**@dev-kevin 收到后请在本文件底部加 "✅ 接到 2026-06-13" 标记，并按 Step 1→3 顺序开工。**
**改完派 qa-tina 跑 Step 4 回归。**

---

## 7. 实施记录（dev-kevin 2026-06-13）

✅ 接到 2026-06-13

**改动**：
- 文件：`frontend/src/components/home/HomeHero.vue`（仅此 1 文件）
- 3 处删除 `:root:not([data-theme="light"])` 包装器（`__backdrop` / `__agent` / `__agent-num`）
- `data-theme="dark"` 钩子（3 处显式 `[data-theme="dark"]` 规则）保持不变
- `git diff` 输出确认：仅 3 行变化，无其他文件波及

**验证**（`npm run build` + Playwright 探针）：
- `npm run build` → `✓ built in 16.91s`（PASS）
- 暗色态 computed style 探针（`colorScheme: dark`）：
  - `__backdrop` → `linear-gradient(rgb(21,18,31) 0%, rgb(28,24,48) 60%, transparent)` ✅ 暗紫渐变
  - `__agent` → bg `rgb(28,24,48)` ✅ `--bg-secondary` 暗紫
  - `__agent-num` → bg `rgb(167,139,250)`、text `rgb(21,18,31)` ✅ `--primary` + `--text-inverse` 反向
  - `__title` → `rgba(255,255,255,0.92)` 浅白 ✅ 可读
  - `__lede` → `rgba(255,255,255,0.68)` 浅灰 ✅ 可读
- 浅色态对照：backdrop 仍为 `linear-gradient(rgb(243,240,255) → rgb(238,242,255))` 浅紫渐变，v1 视觉未破坏

**截图归档**：
- `docs/sprints/S32/screenshots/home-hero-dark.png`（暗色态整页）
- `docs/sprints/S32/screenshots/home-hero-light.png`（浅色态整页）

**自验脚本**：
- `docs/sprints/S32/screenshots-dark-light.mjs`（截图）
- `docs/sprints/S32/verify-css.mjs`（computed style 探针）

**已知偏差 / 给 qa-tina 的提示**：
- 浅色态 backdrop / agent / agent-num 与 v1 像素级一致，未做改动
- 暗色态下窗口宽度 ≥ 1024px 验证；移动端断点未单独测（handoff 未要求）
- WCAG 25/25 回归由 qa-tina 在 Step 4 跑 `wcag-regression.md`

**遗留（不在本 PR）**：
- 未来加 `data-theme` 切换器时需重新审视：当前 media query 在 OS 暗色 + 用户切 light 时会冲突。建议切换器落地时改用 `:root[data-theme="dark"]` 单一钩子

