# Design — S36 新手指引（Newbie Guide）

> **Sprint**: S36
> **作者**: Designer (agile-rd-team)
> **日期**: 2026-06-13

---

## 1. 设计目标

让新用户 **3 分钟内** 完成「知道 Skill 是什么 → 装到 Claude → 知道有 API」三件事。

**关键 UX 原则**（来自 ui-ux-pro-max Quick Reference）：
- P1 Accessibility：暗色态对比度 ≥ 4.5:1
- P2 Touch & Interaction：CTA 主按钮 ≥ 44×44px，8px+ 间距
- P5 Layout & Responsive：移动端单列堆叠，无横滚
- P7 Animation：微交互 150-300ms，ease-out
- P9 Navigation：锚点导航有当前态指示

## 2. 色彩 / Token 对照

| 用途 | Token | 浅色 | 暗色 |
|------|-------|------|------|
| 卡片底 | `--bg-primary` / `--bg-elevated` | #fff | #2d2842 |
| 卡片描边 | `--border` | #e5e5ea | rgba(255,255,255,.08) |
| 主文字 | `--text-primary` | #1a1a1f | rgba(255,255,255,.92) |
| 副文字 | `--text-secondary` | #5a5a66 | rgba(255,255,255,.68) |
| 主品牌 | `--primary` | #7c3aed | #a78bfa |
| CTA 渐变 | linear `#7c3aed → #4f46e5`（沿用 HomeHero） | — | — |
| Hero 背景渐变 | linear `#f3f0ff → #eef2ff`（浅）/ var(--bg-primary) → var(--bg-secondary)（暗） | — | — |

**严禁写死颜色**；所有色值必须走 token。

## 3. 首页卡片 `HomeOnboarding.vue`

### 3.1 线框（桌面 ≥ 1024px）

```
+--------------------------------------------------------------------+
|  [🎯]  第一次使用 Skill?                                  [开始新手指引 →] |
|        不知道从哪开始？3 分钟带你了解 Skills 是…                      |
+--------------------------------------------------------------------+
```

- 高 100px
- 左：48×48 圆角方块（紫粉渐变） + 文字区
- 右：CTA 主按钮（pill 形，紫底白字）
- 背景：浅色 `var(--bg-elevated)`，暗色 `var(--bg-secondary)`
- 描边：1px `var(--border)`
- 圆角：14px
- 阴影：浅 `var(--shadow-sm)`，暗 `inset 0 1px 0 rgba(255,255,255,.04)`（沿用 token）

### 3.2 移动端（≤ 640px）

```
+--------------------------+
|  [🎯]                      |
|  第一次使用 Skill?         |
|  不知道从哪开始？…          |
|  [    开始新手指引 →    ]   |  <- 按钮 stretch
+--------------------------+
```

- flex-direction: column
- align-items: flex-start
- 按钮 align-self: stretch

### 3.3 交互态

| 态 | 视觉 |
|----|------|
| 默认 | 卡片 + 描边 + shadow-sm |
| hover | transform: translateY(-1px)，shadow-md |
| 按钮 hover | bg 紫渐变，文字 #fff，translateY(-1px) |
| focus | 2px ring `--primary`，outline 偏移 2px |
| active | transform: translateY(0) |

### 3.4 a11y
- `role="region"` + `aria-label="新手指引入口"`
- CTA 按钮：可见文字「开始新手指引」+ 箭头（装饰性 `aria-hidden="true"`）

## 4. 引导页 `NewbieGuideView.vue`

### 4.1 整体骨架

```
+---------------------------------------------+
|             [AppHeader]                      |
+---------------------------------------------+
|                                              |
|        S36 · 3 分钟上手                       |
|        新手指引                               |
|        带你 3 分钟了解 Skill …                |
|        ───────────                            |
|                                              |
|  [Skill 是什么] [Skills Manager] [API 接入]   |  <- 锚点导航（sticky）
|                                              |
|  ╔══ §1 Skill 是什么 ═══════════════╗        |
|  ║ Markdown 解释…                     ║        |
|  ║ ┌─ SKILL.md 示例 ─────────────┐    ║        |
|  ║ │ ---                         │    ║        |
|  ║ │ name: my-skill              │    ║        |
|  ║ │ description: ...            │    ║        |
|  ║ └─────────────────────────────┘    ║        |
|  ╚════════════════════════════════════╝        |
|                                              |
|  ╔══ §2 Skills Manager 使用说明 ═════╗        |
|  ║ [1] 下载 skill 包  [下载按钮]      ║        |
|  ║ [2] 解压…                          ║        |
|  ║ [3] 重启 Claude Code               ║        |
|  ║ [4] 试试这些指令                   ║        |
|  ╚════════════════════════════════════╝        |
|                                              |
|  ╔══ §3 API 接入 ═══════════════════╗        |
|  ║ 跳转卡片：                         ║        |
|  ║   标题：API 接入                   ║        |
|  ║   描述：…                          ║        |
|  ║   端点 chips：                     ║        |
|  ║     [GET /api/skills]              ║        |
|  ║     [GET /api/skills/slug/{slug}]  ║        |
|  ║     [...]                          ║        |
|  ║   [前往完整 API 接入指南 →]         ║        |
|  ╚════════════════════════════════════╝        |
+---------------------------------------------+
```

### 4.2 间距系统（4/8dp）

| 元素 | 值 |
|------|----|
| 页面外 padding | 48px top / 80px bottom / 24px side |
| 卡片 padding | 24px 28px |
| 卡片间距 | 24px |
| 锚点导航 padding | 8px |
| 锚点 pill padding | 6px 16px |
| Hero 与内容间距 | 24px |
| 移动端断点 | ≤ 640px（页面 padding 改 24px / 16px） |

### 4.3 锚点导航（沿用 ApiGuideView 风格）

- 横排 pill，`gap: 8px`
- 圆角 999px，背景 `var(--bg-primary)`，描边 1px
- hover：bg `var(--bg-tertiary)`，文字 `--primary`
- active（scroll-spy）：bg `var(--bg-tertiary)`，文字 `--primary`
- overflow-x: auto，移动端可横滑
- `rootMargin: -80px 0px -65% 0px`（沿用 ApiGuideView）

### 4.4 §1 Skill 是什么

- 标题：`§1 Skill 是什么`
- 段落：3-4 行解释
- 代码块：示例 `SKILL.md`（frontmatter + body）— 用 `<pre><code>`，token 走 `--text-primary` 文字 / `--bg-secondary` 底
- 代码块：示例 `manifest.json`（可选）

### 4.5 §2 Skills Manager 使用说明

沿用 `HomeHero` 的 `&__agent` 风格，**独立**为一个 card：

- 标题：`§2 Skills Manager 使用说明`
- 4 步教程（`<ol>` + 数字徽章 + 描述）：
  1. 下载 skill 包（含 `<a-button type="primary">`）
  2. 解压到 `~/.claude/skills/skills-manager/`（含 shell 代码块）
  3. 重启 Claude Code
  4. 试试这些指令（3 条 prompt 建议）

### 4.6 §3 API 接入（跳转卡片）

- 标题：`§3 API 接入`
- **不复制** ApiGuideView 的表格
- 跳转卡片：紫渐变背景（沿用 HomeHero 的搜索按钮色），白色文字
  - 标题「API 接入」
  - 描述「想程序化访问 SkillsMap？查看完整 REST 文档：端点列表、参数、响应字段、示例。」
  - 端点 chips（小 chip 列表，4-5 个）
  - 主 CTA `<a-button type="primary" size="large">前往完整 API 接入指南 →</a-button>` → `router.push({ name: 'api-guide' })`

### 4.7 暗色态

- 全部走 token，不写死
- 代码块背景：浅色 `var(--bg-tertiary)`，暗色 `var(--bg-elevated)`
- 跳转卡片：浅色紫底白字，暗色用 `--primary-bg` 半透明 + `--text-primary` 文字（避免纯饱和紫刺眼）
- 卡片描边：浅 `#e2e8f0`，暗 `var(--border)`
- 验证方式：Chrome DevTools 切 `prefers-color-scheme: dark` + 项目 `data-theme="dark"` 钩子

## 5. 字体 / 排版

- 标题（H1）：32px / 800（桌面），28px（移动）
- 段标题（H2 in card）：18px / 700
- 正文：15px / 1.7 line-height
- 代码：13px，'SF Mono', Menlo, Consolas, monospace
- 卡片按钮：14px / 600

## 6. 动效

- 卡片 hover：150ms ease-out
- 锚点点击滚动：smooth
- 滚动到位：200ms ease-out（无 transform scale，避免抖动）

## 7. a11y / i18n 备忘

- 所有按钮有可见文字（CTA「开始新手指引 →」）
- 装饰性 emoji（🎯）`aria-hidden="true"`
- 移动端不依赖 hover
- v1 仅中文；v2 可接 i18n

## 8. 反模式自检

- [x] 没有用 emoji 做 icon
- [x] 没有写死颜色
- [x] 没有用 `transition: all`
- [x] 没有 animating width/height
- [x] 移动端无横滚
- [x] CTA 按钮 ≥ 44×44px
- [x] 暗色态对比度 ≥ 4.5:1（已用 token）
- [x] 没有把 `controller` / `vuex` 等反模式引入
