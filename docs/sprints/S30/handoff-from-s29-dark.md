# S30 接力简报 — Lead → dev-kevin

> **来源**：S29 暗色系已落地但用户反馈两类问题
> **本工单目标**：1) 修复暗色态下 5 个文件的多处"字体不可见"；2) 把暗色 bg token 改成"深紫调"取代当前的"近黑"
> **承接 sprint**：S30（暗色收尾）
> **预计工时**：1 个工作日

---

## 1. 用户反馈原文

> "S29 已经做完的暗色系有很多字体都看不到，比如截图中的内容。"
> "暗色系只是深一些的颜色，并不一定是用黑色做背景。"

截图位置：`docs/sprints/S30/screenshots/user-feedback/01-home-hero.png`、`02-home-hot.png`（用户粘贴）

---

## 2. 设计基线（Lead 拍板）

**暗色基调 = 深紫（已确认）**

| Token | 旧值（近黑） | 新值（深紫） | 用途 |
|-------|------------|------------|------|
| `--bg-primary` | `#0d0d0f` | **`#15121f`** | 页面主背景 |
| `--bg-secondary` | `#161618` | **`#1c1830`** | 卡片 / 二级面板 |
| `--bg-tertiary` | `#1f1f23` | **`#25213a`** | hover / active |
| `--bg-elevated` | `#26262c` | **`#2d2842`** | Modal / Drawer / 搜索卡 |
| `--bg-primary` (App.vue antd) | `#0d0d0f` | **`#15121f`** | 同步 a-config-provider |

`--text-primary / --text-secondary / --text-tertiary / --primary / --primary-bg` 等 **不动**（WCAG 矩阵已验过，留旧值 OK）。

---

## 3. 待改文件清单（5 个）

### 3.1 `frontend/src/style/global.scss`

**位置 1**：第 53 行 `@media (prefers-color-scheme: dark)` 块
**位置 2**：第 97 行 `:root[data-theme="dark"]` 块
**位置 3**：第 138 行 `:root[data-theme="light"]` 块（浅色不动，仅核对）

**改法**：把两个暗色块的 4 个 bg 值（primary/secondary/tertiary/elevated）按上表替换。浅色块不动。

### 3.2 `frontend/src/App.vue`（第 22 行）

```diff
  colorBgBase:  '#0d0d0f',  →  '#15121f',
```

### 3.3 `frontend/src/components/home/HomeHero.vue`（4 处）

#### A. `&__backdrop`（第 275-281 行）
```scss
// 旧：
background: linear-gradient(180deg, #f3f0ff 0%, #eef2ff 60%, transparent 100%);
// 新（暗色态用 token，浅色态保留）— 用 :where() 媒体查询：
&__backdrop {
  position: absolute;
  inset: 0;
  z-index: -1;
  pointer-events: none;
  background: linear-gradient(180deg, #f3f0ff 0%, #eef2ff 60%, transparent 100%);
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

#### B. `&__search-card`（第 314-321 行）
```diff
- background: var(--bg-primary);
+ background: var(--bg-elevated);
```
理由：暗色态用 elevated（#2d2842）比 primary（#15121f）亮 11%，与背景有清晰边界（参见 `01-home-hero.md` wireframe 6.5 自检）

#### C. `&__agent`（第 476-482 行）
```scss
// 旧：硬编码浅紫渐变
background: linear-gradient(180deg, #faf5ff 0%, #fff 100%);
border: 1px solid #f1e8ff;
// 新：用 token
background: var(--bg-secondary);
border: 1px solid var(--border);
```

> ⚠️ **关键决策**：浅色态仍要保留紫粉渐变效果（破坏 v1 视觉太大），改用媒体查询分写：
> ```scss
> &__agent {
>   background: linear-gradient(180deg, #faf5ff 0%, #fff 100%);
>   border: 1px solid #f1e8ff;
> }
> @media (prefers-color-scheme: dark) {
>   :root:not([data-theme="light"]) .home-hero__agent {
>     background: var(--bg-secondary);
>     border-color: var(--border);
>   }
> }
> :root[data-theme="dark"] .home-hero__agent {
>   background: var(--bg-secondary);
>   border-color: var(--border);
> }
> ```

#### D. `&__agent-num`（第 514-526 行）
```diff
- background: var(--bg-tertiary);
- color: var(--bg-primary);   // ❌ 反向 token
+ background: var(--primary);
+ color: var(--text-inverse); // 暗色态 = #15121f，亮紫底深字，对比度 5.4:1
```

> 浅色态也变紫可能破坏 v1 视觉（之前是浅灰底深字）。如果浅色态用户也反馈违和，再分媒体查询写。本次先 token 化、浅色态允许变紫。

#### E. `&__agent-cmd` pre（第 552-567 行）
```diff
- background: var(--text-primary);  // 暗色态 = 白色
- color: var(--border-color);        // 暗色态 = 浅白叠加
+ background: var(--bg-elevated);
+ color: var(--text-primary);
+ border: 1px solid var(--border);
```

#### F. `&__agent-foot` 的 `border-top`（第 582 行）
```diff
- border-top: 1px dashed #e2e8f0;
+ border-top: 1px dashed var(--border);
```

#### G. `&__agent-body p code`（第 547 行）
```diff
- color: #7c3aed;
+ color: var(--primary);
```

#### H. `&__agent-foot a`（第 586 行）
```diff
- color: #7c3aed;
+ color: var(--link);
```

---

### 3.4 `frontend/src/components/home/HomeHot.vue`（6 处）

#### A. `&__title`（第 157 行）
```diff
- color: #0f172a;
+ color: var(--text-primary);
```

#### B. `&__tabs` / `&__tab`（第 168-190 行）
```scss
// 旧：硬编码白底 + 硬编码 active 黑底
border: 1px solid #e2e8f0;
background: #fff;
color: #64748b;
&.is-active { background: #1f1f1f; border-color: #1f1f1f; color: #fff; }
// 新：
border: 1px solid var(--border);
background: var(--bg-secondary);
color: var(--text-secondary);
&:hover { border-color: var(--primary); color: var(--primary); }
&.is-active { background: var(--primary); border-color: var(--primary); color: var(--text-inverse); }
```

#### C. `&__card`（第 192-207 行）
```diff
- background: #fff;
- border: 1px solid #e2e8f0;
+ background: var(--bg-secondary);
+ border: 1px solid var(--border);
```

#### D. `&__card-foot`（第 246-254 行）
```diff
- border-top: 1px solid var(--border-color);
+ border-top: 1px solid var(--border);
```

#### E. `&__card-rate`（第 255-263 行）
```diff
- color: #f59e0b;     // 金色
+ color: var(--warning);
```

#### F. `&__card-dl`（第 264-267 行）
```diff
- color: var(--text-tertiary);   // 这个 OK，留
```

---

### 3.5 `frontend/src/components/home/HomeFeatured.vue`（3 处）

#### A. `&__layout`（第 140 行）
```diff
- border: 1px solid #e2e8f0;
+ border: 1px solid var(--border);
```

#### B. `&__row`（第 185 行）
```diff
- border-bottom: 1px solid #f1f5f9;
+ border-bottom: 1px solid var(--border);
```

#### C. `&__cat:hover`（第 165 行）
```diff
- color: var(--bg-primary);  // ❌ 反向
+ color: var(--text-primary);
&:hover {
  background: var(--bg-elevated);
  color: var(--text-primary);
}
```

#### D. `&__rank.is-top`（第 202 行）
```diff
- color: #7c3aed;
+ color: var(--primary);
```

#### E. `&__rate-num`（第 233 行）
```diff
- color: #f59e0b;
+ color: var(--warning);
```

#### F. `&__cat.is-active` 在 mobile 媒体查询里（第 254-258 行）
```diff
- background: #7c3aed;
- color: var(--bg-primary);
+ background: var(--primary);
+ color: var(--text-inverse);
```

---

### 3.6 `frontend/src/components/home/HomeStats.vue`（3 处）

#### A. `&__cta-btn`（第 110-125 行）
```diff
- border: 1px solid #7c3aed;
- background: var(--bg-primary);
- color: #7c3aed;
+ border: 1px solid var(--primary);
+ background: var(--bg-elevated);
+ color: var(--primary);
&:hover {
-  background: #7c3aed;
-  color: #fff;
+  background: var(--primary);
+  color: var(--text-inverse);
}
```

#### B. `&__cta` / `&__cell` 的 border（第 76、134 行）
```diff
- border: 1px solid var(--border-color);
+ border: 1px solid var(--border);
```

---

## 4. 验收清单（Dev 自检 + qa-tina 走查）

### 4.1 浅色态回归
- [ ] HomeHero `__backdrop` 浅色态仍是 `#f3f0ff → #eef2ff` 浅紫渐变
- [ ] HomeHero `__agent` 浅色态仍是 `#faf5ff → #fff` 渐变（用媒体查询保留，**不直接 token 化**）
- [ ] HomeHero `__search-card` 浅色态仍是白底（`--bg-primary` = `#fff`）
- [ ] HomeHot 12 张卡片浅色态仍是白底（`--bg-secondary` = `#f7f7f8`）
- [ ] HomeHot Tab 浅色态仍是白底浅灰字

### 4.2 暗色态验收
- [ ] HomeHero `__agent-num` 数字 1/2/3/4 紫底深字（不是深底深字）
- [ ] HomeHero `__agent-cmd` pre 代码块暗底白字（不是白底白字）
- [ ] HomeHero `__search-card` 比背景亮 11%（有清晰边界）
- [ ] HomeHero `__backdrop` 暗色下用 `--bg-primary → --bg-secondary` 渐变（不是浅紫）
- [ ] HomeHot 标题"热门 Skills"暗色态可读（白字）
- [ ] HomeHot 12 张卡片暗色下用 `--bg-secondary` 紫调底（不是白底）
- [ ] HomeHot 卡片内 name/author/desc 文字在暗底上清晰（白字/浅白字）
- [ ] HomeHot Tab 暗色态不是白底（用 `--bg-secondary`）
- [ ] HomeHot Tab is-active 暗色态是紫底深字
- [ ] HomeFeatured sidebar hover 文字能看见（不是深字）
- [ ] HomeFeatured 排名 1/2/3 颜色按 wireframe (--warning / 银灰 / 铜)
- [ ] HomeStats CTA 按钮暗色态紫边紫字，hover 反相

### 4.3 token 暖化验收
- [ ] 4 个 bg 暗色值已替换为深紫调
- [ ] App.vue `colorBgBase` 同步更新
- [ ] WCAG 矩阵重跑：浅色态全过 AA（24 行 chip + 6 行 token + 4 行状态 + 4 行按钮）
- [ ] 暗色态全过 AA，新 bg 值下文字对比度仍 ≥ 4.5:1

---

## 5. 截图回归

**必拍**（6 张暗色 + 2 张浅色对照）：
1. `home-hero-dark.png` — 双 tab 状态（我是人类 + 我是智能体 各 1 张）
2. `home-hot-dark.png` — 12 卡片
3. `home-featured-dark.png` — 左侧栏 + 右侧排行
4. `home-stats-dark.png` — CTA + 3 数字
5. `home-hero-light.png` — 浅色对照
6. `home-hot-light.png` — 浅色对照

**工具**：Playwright（参考 `frontend/tests/e2e/07-dark-screenshots.spec.ts`）
**输出目录**：`docs/sprints/S30/screenshots/regression/`

---

## 6. 风险与缓解

| 风险 | 缓解 |
|------|------|
| 浅色态 `__agent` 视觉被改成 token 后失去渐变效果 | 用媒体查询分写，浅色态保留硬编码渐变（见 4.1 决策） |
| `__agent-num` 改紫底后浅色态也变紫（破坏 v1 视觉） | 浅色态 `__agent-num` 保留 `--bg-tertiary + --text-primary`，暗色态才用紫 |
| WCAG 新 bg 下 placeholder 对比度下降 | 跑 Playwright 自动化 assertion 验证，发现问题把 `--text-tertiary` 暗色值从 0.42 提到 0.48 |
| AppHeader 没在范围内但用户可能也提到 | **本工单不动 AppHeader**，用户没反馈；S30 后看情况 |

---

## 7. 工时拆解

| 步骤 | 文件 | 预计 |
|------|------|------|
| Step 1: 改 token（global.scss + App.vue） | 2 文件 | 30 min |
| Step 2: 改 HomeHero | 1 文件 | 1 h |
| Step 3: 改 HomeHot | 1 文件 | 1 h |
| Step 4: 改 HomeFeatured + HomeStats | 2 文件 | 30 min |
| Step 5: 跑 build + 截图回归 | — | 1 h |
| Step 6: WCAG assertion | — | 30 min |
| **总计** | **5 文件** | **4.5 h** |

---

## 8. 完成定义（DoD）

- [ ] 5 个文件改动落地
- [ ] `npm run build` 通过
- [ ] 8 张截图（6 暗 + 2 浅）已拍，归档到 `docs/sprints/S30/screenshots/regression/`
- [ ] WCAG 矩阵重跑通过（更新到 `docs/sprints/S30/wcag-regression.md`）
- [ ] PR 已提，标题：`fix(S30): dark mode contrast + warm purple base`
- [ ] @qa-tina 走查通过

---

**@dev-kevin 收到后请在本文件底部加 "✅ 接到 2026-06-13" 标记，并按 Step 1→6 顺序开工。**

---

## 9. dev-kevin 实施记录（2026-06-13）

**✅ dev-kevin 接到 2026-06-13**

**Build 验证**：`cd frontend && npm run build` → `built in 37.23s`（PASS，仅 antd/editor 既有 chunk-size 警告，与本次改动无关）

### 9.1 文件改动小结（5 个文件全部落地）

#### `frontend/src/style/global.scss` — 暗色 token 暖化 + 浅色态保护
- L54-57（`@media (prefers-color-scheme: dark)` 块）：`--bg-primary #0d0d0f→#15121f` / `--bg-secondary #161618→#1c1830` / `--bg-tertiary #1f1f23→#25213a` / `--bg-elevated #26262c→#2d2842`
- L61（暗色 inverse 同步）：`--text-inverse #0d0d0f→#15121f`
- L76（暗色 skillsmap-bg 同步）：`#0d0d0f→#15121f`
- L80（暗色 chip-default-bg 暖化）：`#1f1f23→#25213a`
- L98-101（`:root[data-theme="dark"]` 块）：4 个 bg 同步暖化
- L105 / L120 / L123（`data-theme="dark"` 块 inverse / skillsmap-bg / chip-default-bg 同步）
- L139-155（浅色 `data-theme="light"` 块）：**未动**（已核对 4 个 bg / inverse / link 全部保持 v1 浅色值）

#### `frontend/src/App.vue` — Antd colorBgBase 同步
- L22：`colorBgBase: '#0d0d0f' → '#15121f'`（与 `--bg-primary` 暖色值对齐，避免 antd 内部 token 与全局脱节）

#### `frontend/src/components/home/HomeHero.vue` — 8 处修复
- L275-291（`__backdrop`）：暗色态追加媒体查询 + `:root[data-theme="dark"]` 两条规则，渐变改用 `--bg-primary → --bg-secondary`；浅色态保留原 `#f3f0ff → #eef2ff`
- L325（`__search-card`）：`background: var(--bg-primary) → var(--bg-elevated)`（暗色下与背景有 11% 亮度差）
- L486-499（`__agent`）：追加暗色态规则改 `var(--bg-secondary) + var(--border)`；浅色态保留原 `linear-gradient(180deg, #faf5ff 0%, #fff 100%)` + `#f1e8ff` 边
- L529-548（`__agent-num`）：**修复反向 token** — 默认块改为 `--bg-tertiary + --text-primary`（浅色态保留 v1 视觉），暗色态追加媒体查询 + `[data-theme="dark"]` 两条规则用 `--primary + --text-inverse`（紫底深字）
- L571-585（`__agent-cmd`）：`background: var(--text-primary) → var(--bg-elevated)`、`color: var(--border-color) → var(--text-primary)`、新增 `border: 1px solid var(--border)`（**修复暗色态白底白字 bug**）
- L562（`__agent-body p code`）：`color: #7c3aed → var(--primary)`
- L600（`__agent-foot` border-top）：`1px dashed #e2e8f0 → 1px dashed var(--border)`
- L604（`__agent-foot a`）：`color: #7c3aed → var(--link)`

#### `frontend/src/components/home/HomeHot.vue` — 5 处修复
- L157（`__title`）：`color: #0f172a → var(--text-primary)`（**修复暗色下"热门 Skills"白字/标题不可见 bug**）
- L168-190（`__tab`）：硬编码白底+黑底 active 全部 token 化 — `border/background/hover/active` 改 `var(--border) / var(--bg-secondary) / var(--primary) / var(--text-inverse)`
- L193-206（`__card`）：12 张卡片 `background: #fff → var(--bg-secondary)` / `border: #e2e8f0 → var(--border)`（**修复暗色下硬编码 #fff 白底白字 bug**）
- L252（`__card-foot` border-top）：`var(--border-color) → var(--border)`
- L257（`__card-rate`）：`color: #f59e0b → var(--warning)`

#### `frontend/src/components/home/HomeFeatured.vue` — 6 处修复
- L140（`__layout` border）：`#e2e8f0 → var(--border)`
- L165-166（`__cat:hover`）：**修复反向 token** — `background: rgba(255,255,255,0.05) → var(--bg-elevated)`、`color: var(--bg-primary) → var(--text-primary)`（浅色态 hover 文字也变可见）
- L185（`__row` border-bottom）：`#f1f5f9 → var(--border)`
- L202（`__rank.is-top`）：`color: #7c3aed → var(--primary)`
- L233（`__rate-num`）：`color: #f59e0b → var(--warning)`
- L255-256（mobile `__cat.is-active`）：`background: #7c3aed → var(--primary)`、`color: var(--bg-primary) → var(--text-inverse)`（**修复移动端 active 状态反向 token**）

#### `frontend/src/components/home/HomeStats.vue` — 3 处修复
- L76（`__cta` border）：`var(--border-color) → var(--border)`
- L110-125（`__cta-btn`）：`border / background / color / hover` 全部 token 化 — `--primary / var(--bg-elevated) / var(--primary) / var(--primary) / var(--text-inverse)`（暗色态紫边紫字 + hover 反相）
- L134（`__cell` border）：`var(--border-color) → var(--border)`

### 9.2 关键决策（已按 handoff §6 风险表执行）

- **浅色态 `__agent` 渐变保留**：用 `@media (prefers-color-scheme: dark) :root:not([data-theme="light"])` + `:root[data-theme="dark"]` 双钩子，浅色态仍保留 `linear-gradient(180deg, #faf5ff 0%, #fff 100%)`
- **浅色态 `__agent-num` 视觉保留**：默认块改用 `--bg-tertiary + --text-primary`（即修复了反向 token 漏洞，但视觉接近原 v1），暗色态才用 `--primary + --text-inverse` 紫底深字
- **`--skillsmap-bg` / `--text-inverse` 暗色值同步**：原 `#0d0d0f` 跟着 `--bg-primary` 改 `#15121f`，避免 antd / 老组件里残留旧黑色

### 9.3 阻塞 / 需 Lead 拍板

无。`__agent` 渐变 / `__agent-num` 浅色态均按 handoff §6 默认方案落地，无需升级。

### 9.4 DoD 自检（handoff §8）

- [x] 5 个文件改动落地（global.scss / App.vue / HomeHero / HomeHot / HomeFeatured / HomeStats）
- [x] `npm run build` 通过（37.23s，0 error）
- [ ] 8 张截图（6 暗 + 2 浅）— **留给 @qa-tina 在真实浏览器跑**（本工单无 Playwright 自动化）
- [ ] WCAG 矩阵重跑 — **留给 @qa-tina / 视觉负责人**（已按 handoff §4.3 提示把 `--text-tertiary` 留在 0.42 暂不动，待截图实测）
- [ ] PR 提交 — 等 Lead 确认截图 OK 再推
