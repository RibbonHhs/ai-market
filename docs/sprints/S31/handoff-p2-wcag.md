# S31 接力简报 — Lead → dev-kevin

> **来源**：S30 wcag-regression.md 失败 2 项 + Lead 拍板走"修而不是 defer"
> **目标**：B1 + L4 一次性修干净，25/25 全过 AA
> **承接 sprint**：S31（WCAG P2 收尾）
> **预计工时**：2-3 h

---

## 1. 待修 2 项（P2-A + P2-B）

### 1.1 B1 — HomeHero `__search-btn` 深字 3.24:1

**位置**：`frontend/src/components/home/HomeHero.vue:396`
**现状**：`color: var(--bg-primary);`（暗色 = #15121f，浅色 = #1a1a1f，都和紫渐变中点 ~#6640a6 对比 ≈ 3.2:1）
**目标**：改 `#fff`（和 v1 wireframe 一致，浅色/暗色态都用白字）
**改法**（1 行 CSS）：

```diff
  &__search-btn {
    ...
    background: linear-gradient(135deg, #7c3aed 0%, #4f46e5 100%);
-   color: var(--bg-primary);
+   color: #fff;
    ...
  }
```

**预期对比度**：`#fff` on 紫渐变中点 `#6640a6` ≈ **4.8:1** ✅ 普通 AA

⚠️ **不要用 `var(--text-primary)`** — 浅色态下 `--text-primary = #1a1a1f` 深色，破坏 v1 视觉。

### 1.2 L4 — 浅色 `--warning` 2.01:1

**位置**：`frontend/src/style/global.scss:17`（浅色默认块）
**现状**：`--warning: #f59e0b`（amber-500，金）
**目标**：`#b45309`（amber-700，焦糖）
**改法**（1 行）：

```diff
  :root {
    ...
-   --warning: #f59e0b;
+   --warning: #b45309;
    ...
  }
```

**暗色态不动**（`global.scss:66` 和 `:110` 都是 `--warning: #fbbf24`，暗色背景对比度仍 ≥ 8:1）。
**预期对比度**：`#b45309` on 浅色卡片底 `#f7f7f8` ≈ **4.6:1** ✅ AA

---

## 2. 全站 var(--warning) 扫描结果

> 已 grep `frontend/src/**/*.{vue,ts,scss}`，结果如下：

| 文件 | 行 | 用法 | 受影响 | 验证 |
|------|---|------|--------|------|
| `frontend/src/style/global.scss` | 17 | 浅色 `--warning` 定义 | ✅ 改 | 看 L4 |
| `frontend/src/style/global.scss` | 66, 110 | 暗色 `--warning` 定义 | ❌ 不动（仍 #fbbf24）| 已 AA |
| `frontend/src/components/home/HomeHot.vue` | 257 | `__card-rate` 评分数字 | ✅ 自动跟随 token | 跑 Playwright 验证 |
| `frontend/src/components/home/HomeFeatured.vue` | 233 | `__rate-num` 评分数字 | ✅ 自动跟随 token | 跑 Playwright 验证 |
| `frontend/src/components/SkillLogo.vue` | 35, 38 | **logo 渐变色**（非文字） | ❌ **禁止动** | 这是 SVG 渐变硬编码，与 `--warning` 无关 |
| `frontend/src/constants/usage-colors.ts` | 66 | chip fg #fbbf24（暗色） | ❌ 不动 | 是 chip token，不在本次范围 |

**结论**：只动 `global.scss:17` + `HomeHero.vue:396` 两处，其他 0 影响。

---

## 3. 验收清单

### 3.1 B1 验收
- [ ] `HomeHero.vue:396` 改为 `color: #fff;`
- [ ] 浅色态按钮仍白字（v1 视觉不变）
- [ ] 暗色态按钮白字紫底 ≈ 4.8:1（≥ 4.5:1 ✅）
- [ ] 按钮 hover 视觉不变（box-shadow / transform 照旧）

### 3.2 L4 验收
- [ ] `global.scss:17` 改为 `--warning: #b45309;`
- [ ] 浅色态 HomeHot 卡片评分数字可读（4.6:1 ✅）
- [ ] 浅色态 HomeFeatured 排行评分数字可读
- [ ] 暗色态 `--warning` 不动（仍是 `#fbbf24`，AA 边缘已过）
- [ ] 全站扫 `var(--warning)` / `#f59e0b` / `#fbbf24` 无破坏

### 3.3 DoD
- [ ] `cd frontend && npm run build` 通过（37s 量级）
- [ ] dev 改动 2 个文件（`global.scss` + `HomeHero.vue`）
- [ ] Playwright 跑 wcag-regression.md 25 行 → **25/25 全过 AA**

---

## 4. 风险与缓解

| 风险 | 缓解 |
|------|------|
| `--warning` 暗化影响 `SkillLogo.vue` | 已确认是 logo 渐变色硬编码，与 `--warning` 无关，**不动** |
| `#b45309` 视觉过深，破坏"金色评分"语义 | amber-700 仍是焦糖金，vs 原 amber-500 仅降 2 档亮度 |
| 暗色态 `--warning` 错误被改 | L17 浅色块在文件顶部，L66/L110 暗色块在中间和 data-theme="dark" 块；Dev 改前**只动 L17** |
| `__search-btn` 白字在浅色态反而显突兀 | v1 wireframe 1-home-hero.md §暗色配色 已规定"主按钮文字 `--text-inverse` = #0d0d0f / 暗色 #fff"；浅色用 `#fff` 比 `--text-inverse` 更直接，且是 v1 原状 |

---

## 5. 工时拆解

| 步骤 | 文件 | 预计 |
|------|------|------|
| Step 1: 改 B1 按钮白字 | HomeHero.vue | 10 min |
| Step 2: 改 L4 浅色 warning | global.scss | 10 min |
| Step 3: 跑 build 验证 | — | 5 min |
| Step 4: 派 qa-tina 跑 25 行 assertion | — | 1-2 h |
| **总计 dev** | **2 文件 / 2 行** | **30 min** |

---

## 6. 完成定义

- [ ] 2 行 CSS 改动落地
- [ ] `npm run build` 通过
- [ ] qa-tina 重跑 wcag-regression.md → **25/25 全过 AA**
- [ ] PR 标题：`fix(S31): B1 search btn white text + L4 warning amber-700`

---

**@dev-kevin 收到后请在本文件底部加 "✅ 接到 2026-06-13" 标记，并按 Step 1→3 顺序开工。**
**改完派 qa-tina 跑 Step 4 回归。**

---

## 7. 实施记录（dev-kevin，2026-06-13）

✅ dev-kevin 接到 2026-06-13

### 改动小结（2 文件 / 2 行）

| ID | 文件 | 行 | before | after | 验证 |
|----|------|----|--------|-------|------|
| B1 | `frontend/src/components/home/HomeHero.vue` | 396 | `color: var(--bg-primary);` | `color: #fff;` | 紫渐变中点 ≈ 4.8:1 ✅ AA |
| L4 | `frontend/src/style/global.scss` | 17 | `--warning: #f59e0b;` | `--warning: #b45309;` | 浅色卡片底 ≈ 4.6:1 ✅ AA |

### 约束遵守

- [x] B1 未用 `var(--text-primary)`（保留 v1 视觉）
- [x] L4 未动 `SkillLogo.vue:35/38`（logo 渐变硬编码）
- [x] L4 未动 `usage-colors.ts:66`（chip token，不在本次范围）
- [x] 暗色态 `--warning` L66 / L110 保持 `#fbbf24`（已 AA，未碰）
- [x] 不引入新依赖

### 构建验证

```
$ cd frontend && npm run build
✓ built in 23.37s
```

仅 chunk-size 警告（antd 1.4MB / editor 1.07MB），与本次改动无关，属历史告警。

### git status

```
 M frontend/src/components/home/HomeHero.vue
 M frontend/src/style/global.scss
?? docs/sprints/S31/
```

### 下一步

派 qa-tina 跑 wcag-regression.md 25 行 assertion，预期 25/25 全过 AA。

PR 标题：`fix(S31): B1 search btn white text + L4 warning amber-700`
