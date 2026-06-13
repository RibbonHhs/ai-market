# S29 接力简报 — Designer → Dev

> **生成时间**：2026-06-12
> **状态**：✅ Designer 阶段完成，等待 Dev 落地
> **下一步**：dev-kevin 接手，按本文件 §3 顺序落地

---

## 1. 交付物清单

| # | 文件 | 路径 | 行数 | 状态 |
|---|------|------|------|------|
| 1 | 暗色 token 矩阵 v2 | `D:\codeing\workspace\skills-map\docs\sprints\S29\dark-tokens-v2.md` | 16 token + 12 chip | ✅ |
| 2 | 12 张 ASCII wireframe | `D:\codeing\workspace\skills-map\docs\sprints\S29\wireframes-v2\01-12.md` | 12 文件 | ✅ |
| 3 | WCAG 验证矩阵 | `D:\codeing\workspace\skills-map\docs\sprints\S29\wcag-matrix.md` | 30 行对比 | ✅ |
| 4 | 本 handoff | `D:\codeing\workspace\skills-map\docs\sprints\S29\handoff-design.md` | — | ✅ |

---

## 2. 设计方向一句话总结

> **温暖深灰**（不是纯黑）+ **低饱和紫**（#a78bfa）+ **三层 bg/text**（primary/secondary/tertiary）+ **12 chip 16% alpha 软底**。

参考系：Linear / Vercel / Raycast。

---

## 3. Dev 落地顺序（建议）

### 3.1 Phase A：token 替换（半天）

**改 `frontend/src/style/global.scss`**：
- 替换 `:root` 块（16 token 浅色值）
- 替换 `@media (prefers-color-scheme: dark)` 块（16 token 暗色值）
- 替换 `:root[data-theme="dark"]` 块（同上）
- 替换 `:root[data-theme="light"]` 块（浅色值）
- 替换 12 chip 浅色 + 暗色 24 个 token
- 删除 S25 的反相 `--usage-bg` / `--usage-fg` 全局变量
- 新增 `--chip-X-bg` / `--chip-X-fg` 两套
- 修复 `.markdown-body` pre bg 从 `#0d1117` → `var(--bg-elevated)`

**改 `frontend/src/constants/usage-colors.ts`**：
- 同步 `USAGE_COLORS` 浅色 bg/fg
- 同步 `USAGE_DARK` 暗色 bg/fg
- 删除反相 helper（如有）

**改 `frontend/src/App.vue`**：
- `<a-config-provider :theme="theme">` 同步 `colorPrimary: '#a78bfa'`、`colorBgBase: '#0d0d0f'`、`colorTextBase: 'rgba(255,255,255,0.92)'`

### 3.2 Phase B：组件适配（1 天）

按修复紧迫度排：

| 优先级 | 组件 | 文件 | 修复点 |
|--------|------|------|--------|
| P0 | `HomeHero.vue` | `frontend/src/views/HomeHero.vue` | 渐变 overlay + 文字 token |
| P0 | `BrowseView` 顶部 chip 流 | `frontend/src/views/BrowseView.vue` | chip 直接绑 `--chip-X-bg/fg` |
| P0 | `SkillCard` 卡片 | `frontend/src/components/SkillCard.vue` | bg-secondary + chip |
| P1 | `SkillDetailView` 代码块 | `frontend/src/views/SkillDetailView.vue` | pre bg → `--bg-elevated` |
| P1 | `ApiGuide` 表格 | `frontend/src/views/ApiGuideView.vue` | 斑马纹 + method tag |
| P1 | `AppHeader` | `frontend/src/components/AppHeader.vue` | bg-elevated + 导航 active |
| P2 | `Browse` sidebar | `frontend/src/views/BrowseView.vue` | 12 树节点色系 |
| P2 | `HomeHot` / `HomeStats` | `frontend/src/views/HomeView.vue` | 卡片 token |
| P2 | `ProfileView` | `frontend/src/views/ProfileView.vue` | 标签页 + 列表 |
| P3 | `LoginView` / `RegisterView` | `frontend/src/views/LoginView.vue` | 表单 focus 环 |
| P3 | `Footer` | `frontend/src/components/AppFooter.vue` | bg-secondary |
| P3 | 通用组件（Button/Input/Table） | `frontend/src/components/` | 状态 token |

### 3.3 Phase C：回归验证（半天）

- 6 张暗色截图重拍：`Browse chip` / `SkillCard` / `HomeHero` / `SkillDetail` / `Profile` / `ApiGuide`
- Playwright `07-dark-screenshots.spec.ts` 复用
- WCAG 自动化：`contrast-checks.spec.ts` 跑 30 行矩阵
- 手测焦点环 / hover / active / disabled 4 态

### 3.4 总计：2 个工作日

---

## 4. 不在本次范围（推 S30+）

| 项 | 推后 | 备注 |
|----|------|------|
| 用户偏好持久化到后端 | S30+ | S28 已做 localStorage 持久化，够用 |
| 自定义主题色 | S30+ | 暂只锁 1 套暗色（v2 紫调） |
| 跨浏览器 webkit + firefox | S30+ | v1 优先 Chrome |
| 移动端深度适配 | S30+ | v1 桌面优先 |

---

## 5. S27 失败教训（Dev 必读）

| S27 错误 | v2 防错 |
|----------|--------|
| Dev 自接 CSS 变量 | **设计师先出 token 矩阵 + wireframe**，Dev 照搬 |
| 沿用 antd darkAlgorithm 默认 token | 改用设计师指定的 v2 紫调（#a78bfa），不依赖 antd 默认 |
| chip 在暗色下仍是白底（漏改） | **className 应用层直接绑 `--chip-X-bg/fg`**，不再走 antd `<a-tag color>` 自动选色 |
| HomeHero 文字被渐变吃 | 渐变 overlay 用半透黑底（`rgba(13,13,15,0.6)`），文字用 `text-primary` on overlay |
| `&__tab.is-active` 用 `var(--bg-primary)` 导致白字白底 | v2 已修复：active 用 `--primary` + 2px 下划线 |

---

## 6. 走查 Checklist（Dev 自检 + qa-tina 走查用）

### 6.1 布局与设计稿一致
- [ ] bg 用 `--bg-primary` / `--bg-secondary` / `--bg-tertiary` / `--bg-elevated`（无硬编码）
- [ ] text 用 `--text-primary` / `--text-secondary` / `--text-tertiary`（无硬编码）
- [ ] border 用 `--border` / `--border-strong`（无硬编码）
- [ ] 阴影用 `--shadow-sm/md/lg`（无硬编码）

### 6.2 颜色 / 字体 / 间距正确
- [ ] 浅色 / 暗色双套 token 全部到位
- [ ] 12 chip 浅色 + 暗色 = 24 个 token 全部到位
- [ ] 字体：`-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC'` 不变
- [ ] 间距：4 / 8 / 12 / 16 / 24 / 32 倍数

### 6.3 所有状态已实现
- [ ] empty / loading / error / permission / success 5 态
- [ ] hover / focus / active / disabled 4 态

### 6.4 交互正确
- [ ] hover 有视觉反馈（不瞬变）
- [ ] focus 2px 焦点环可见（a11y 关键）
- [ ] active 状态不破坏布局

### 6.5 暗色态专项
- [ ] HomeHero 渐变上文字可读（≥ 7:1）
- [ ] Browse chip 流暗色下不是白底
- [ ] SkillCard 暗色下不是白底
- [ ] 代码块暗色 bg 是 `--bg-elevated` 而非 `#0d1117`
- [ ] 输入框焦点环 2px primary 可见

### 6.6 无障碍
- [ ] WCAG AA ≥ 4.5:1（见 wcag-matrix.md）
- [ ] 键盘 Tab 顺序合理
- [ ] aria-label 在 icon-only 按钮上
- [ ] 主题切换按钮 aria-label 反映当前态

---

## 7. 风险与缓解

| 风险 | 缓解 |
|------|------|
| Ant Design Vue 4 darkAlgorithm 与新 token 冲突 | `a-config-provider` 显式覆盖 colorPrimary / colorBgBase / colorTextBase，**不引**全局 darkAlgorithm |
| 12 chip 在浏览器缓存中显示旧色 | 加 `:where()` 提升 v2 变量优先级 |
| 暗色态渐变背景被新 bg 吃掉 | 渐变 overlay 用半透黑底，不依赖 bg |
| WCAG 计算有偏差 | Dev 落地后用 Playwright 跑自动化 assertion 再核一遍 |
| Danger 按钮普通文字不达 AA | 改 outline 风格（见 wcag-matrix §7.1）|

---

## 8. 下一步协作

| 角色 | 任务 | 时机 |
|------|------|------|
| **pm-alice** | 审 token 矩阵 + 主色 #a78bfa | 用户拍板后 |
| **dev-kevin** | 按 §3 Phase A→B→C 落地 | 用户拍板后 |
| **qa-tina** | 走查 6 截图 + 5 态 + WCAG | Phase C 后 |

---

## 9. 一句话总结

> **v2 暗色 = 温暖深灰 (#0d0d0f) + 低饱和紫 (#a78bfa) + 16 token + 12 chip 软底 16% alpha + 全 token 化、零硬编码、零反相 trick。**
> 
> **Dev 照搬 dark-tokens-v2.md 的 16 token + 12 chip 即可，无需自创颜色。**

---

**Designer 阶段交付完成。等待用户 / PM 拍板后转 dev-kevin。**
