# S29 暗色 Token v2 — 调色板规范

> **承接**：S27 暗色全站化失败反思 + S28 手动切换 store
> **目标**：重设暗色调色板，让 Designer 主导、Dev 落地零猜测
> **不写代码**：本文件为规范文档，Dev 照此实现

---

## 1. 设计原则（S27 失败后的纠正）

| 原则 | S27 错误 | v2 纠正 |
|------|----------|---------|
| 主背景 | `#141414` 纯黑 | `#0d0d0f` 温暖深灰（带极轻紫调，避免 OLED 灼烧） |
| 主色 | `#1668dc` antd 默认蓝 | `#a78bfa` 低饱和紫（Linear / Vercel 调） |
| 文字 | 0.85 白 | 0.92 白（更"白"→更易读，但非纯白） |
| 边框 | `#424242` 硬灰 | `rgba(255,255,255,0.08)` 软白叠加 |
| 阴影 | `0 4px 12px rgba(0,0,0,0.4)` | 同左 + 加 `inset 0 1px 0 rgba(255,255,255,0.04)` 顶部微高光 |
| Chip 应用 | Dev 自接 className 链断了 | **统一走 `--chip-bg` / `--chip-fg` 两个变量**，className 只换 chip 主题，bg/fg 不反相 |

---

## 2. 调色板 Token 矩阵（16 个）

| Token | 浅色值 | 暗色值 | 用途 | 备注 |
|-------|--------|--------|------|------|
| `--bg-primary` | `#ffffff` | `#0d0d0f` | 主背景（页面 / body） | 暗色带 5% 紫调 `#0d0d0f` |
| `--bg-secondary` | `#f7f7f8` | `#161618` | 卡片 / 二级面板 | 比 primary 浅 4% |
| `--bg-tertiary` | `#f0f0f2` | `#1f1f23` | hover / active / selected | 比 secondary 浅 4% |
| `--bg-elevated` | `#ffffff` | `#26262c` | 弹层 / Modal / Drawer | 比 tertiary 浅 4%，带阴影 |
| `--text-primary` | `#1a1a1f` | `rgba(255,255,255,0.92)` | 主文（标题 / 段落） | 暗色不用 `#fff`，避免纯白刺眼 |
| `--text-secondary` | `#5a5a66` | `rgba(255,255,255,0.68)` | 次文（描述 / 标签） | 对比度 ≥ 7:1（AAA） |
| `--text-tertiary` | `#9a9aa6` | `rgba(255,255,255,0.42)` | placeholder / hint | 对比度 4.6:1（AA 边缘） |
| `--text-inverse` | `#ffffff` | `#0d0d0f` | 主色按钮上的文字 | 与主色 bg 反相 |
| `--primary` | `#7c3aed` | `#a78bfa` | 主色（紫） | 暗色提亮一档，hover 用 `#c4b5fd` |
| `--primary-bg` | `#f3e8ff` | `rgba(167,139,250,0.16)` | 主色背景（hover/selected） | 暗色 16% alpha |
| `--primary-border` | `#ddd6fe` | `rgba(167,139,250,0.32)` | 主色边框 | 暗色 32% alpha |
| `--success` | `#10b981` | `#34d399` | 成功 | 暗色提亮 |
| `--warning` | `#f59e0b` | `#fbbf24` | 警告 | 暗色提亮 |
| `--danger` | `#ef4444` | `#f87171` | 错误 | 暗色提亮 |
| `--link` | `#7c3aed` | `#a78bfa` | 链接 | 同主色 |
| `--border` | `#e5e5ea` | `rgba(255,255,255,0.08)` | 边框 | 暗色用软白叠加 |
| `--border-strong` | `#d1d1d6` | `rgba(255,255,255,0.16)` | 强边框（卡片 / 输入框） | |
| `--shadow-sm` | `0 1px 2px rgba(0,0,0,.04)` | `0 1px 2px rgba(0,0,0,.4)` | 小阴影 | 暗色加深 |
| `--shadow-md` | `0 4px 12px rgba(0,0,0,.06)` | `0 4px 12px rgba(0,0,0,.3), inset 0 1px 0 rgba(255,255,255,0.04)` | 中阴影 | 暗色加顶部微高光 |
| `--shadow-lg` | `0 12px 32px rgba(0,0,0,.08)` | `0 12px 32px rgba(0,0,0,.5), inset 0 1px 0 rgba(255,255,255,0.06)` | 大阴影 | Modal 用 |
| `--scrim` | `rgba(0,0,0,.4)` | `rgba(0,0,0,.6)` | 弹层遮罩 | 暗色加深 |

---

## 3. 12 USAGE Chip 暗色（v2 修正版）

**S27 错误**：chip bg/fg 变量在 S25 时被定义为「fg 当 bg 用」，S27 暗色映射沿用，导致 12 chip 暗色态"深底 + 浅字"实际生效，但前端 className 路径用浅色 token 链 → 暗色态不触发。

**v2 方案**：每个 chip 显式两套 token，className 应用时直接 `var(--chip-X-bg)` / `var(--chip-X-fg)`，**不再反相**。

| code | 浅色 bg | 浅色 fg | 暗色 bg | 暗色 fg | 色相 |
|------|---------|---------|---------|---------|------|
| `PURPOSE-TOOL` | `#F0F5FF` | `#1D39C4` | `rgba(96,165,250,0.16)` | `#93c5fd` | 蓝 |
| `PURPOSE-BIZ` | `#FFF7E6` | `#AD4E00` | `rgba(251,191,36,0.16)` | `#fcd34d` | 琥珀 |
| `PURPOSE-DEV` | `#E6FFFB` | `#006D75` | `rgba(52,211,153,0.16)` | `#6ee7b7` | 翠绿 |
| `PURPOSE-QASEC` | `#F9F0FF` | `#391085` | `rgba(167,139,250,0.16)` | `#c4b5fd` | 紫 |
| `PURPOSE-AI` | `#FFF0F6` | `#9E1068` | `rgba(244,114,182,0.16)` | `#f9a8d4` | 粉 |
| `PURPOSE-DEVOPS` | `#FFF2E8` | `#A8071A` | `rgba(248,113,113,0.16)` | `#fca5a5` | 红 |
| `PURPOSE-DOC` | `#FCFFE6` | `#435106` | `rgba(250,204,21,0.16)` | `#fde047` | 黄 |
| `PURPOSE-MEDIA` | `#E6FAFF` | `#003A8C` | `rgba(34,211,238,0.16)` | `#67e8f9` | 青 |
| `PURPOSE-RESEARCH` | `#F0FBE6` | `#135200` | `rgba(132,204,22,0.16)` | `#bef264` | 柠绿 |
| `PURPOSE-LIFE` | `#FFF1F0` | `#820014` | `rgba(251,146,60,0.16)` | `#fdba74` | 橙 |
| `PURPOSE-DB` | `#F4FFB8` | `#874D00` | `rgba(148,163,184,0.16)` | `#cbd5e1` | 灰蓝 |
| `PURPOSE-BLOCKCHAIN` | `#FFE7BA` | `#874D00` | `rgba(217,119,6,0.16)` | `#fbbf24` | 金 |
| `DEFAULT`（未分类） | `#F5F5F5` | `#595959` | `#1f1f23` | `rgba(255,255,255,0.68)` | 中性 |

**应用规范**：
- chip 高度 22px / 24px（Browse 顶部 / 卡片内）
- chip 圆角 4px（不圆不方）
- chip 文字 12px / 500 weight
- chip padding 0 8px
- 暗色态 bg 用 16% alpha 而非纯色 → 与 `--bg-secondary` 自然融合
- **不要**再用 S25 的 fg/bg 反相 trick（导致映射混乱）

---

## 4. Dev 落地清单（v2 落地步骤）

### 4.1 替换 `global.scss` 的 `:root` 与暗色块
- 直接覆盖 S27 写的 16 个 token
- 暗色块同时支持 `@media (prefers-color-scheme: dark)` 与 `:root[data-theme="dark"]`
- S28 store 已有 `setTheme` 逻辑，无需改

### 4.2 替换 12 chip 变量
- 删除 S25 的反相 `--usage-bg` / `--usage-fg` 全局变量
- 12 chip 各自暴露 `--chip-X-bg` / `--chip-X-fg` 两套（浅 + 暗）
- `usage-colors.ts` 中的 `USAGE_DARK` 与 SCSS 同步更新
- `usage-colors.ts` 中的 `USAGE_COLORS` 与 SCSS 同步更新

### 4.3 修复 chip className 应用链（S27 漏改根因）
- `BrowseView` 顶部 chip 流：用 `<a-tag :style="{ background: c.bg, color: c.fg }">` 或 className 直接绑 `--chip-X-bg`
- `SkillCard` 用途 chip：同上
- `SkillDetailView` 用途区块：同上
- **不要**依赖 antd `<a-tag color="..." />` 自动选 token（v1 实测 antd darkAlgorithm 不会改 chip 颜色）

### 4.4 HomeHero 文字修（S27 漏改）
- `<HomeHero>` 渐变背景上文字必须用 `var(--text-inverse)` 或 `var(--text-primary)`，**不再**用 `var(--text-primary)` on 紫渐变
- 渐变 overlay 加 `rgba(13,13,15,0.6)` 半透黑底，确保文字对比度 ≥ 7:1

### 4.5 验证
- 6 张暗色截图重拍（Browse chip / SkillCard / HomeHero / SkillDetail / Profile / ApiGuide）
- WCAG 矩阵见 `wcag-matrix.md`
- Playwright `07-dark-screenshots.spec.ts` 复用

---

## 5. 风险 & 缓解

| 风险 | 缓解 |
|------|------|
| Ant Design Vue 4 `darkAlgorithm` 与新 token 冲突 | 在 `<a-config-provider :theme="theme">` 中只覆盖必要 token（colorPrimary / colorBgBase / colorTextBase），不引全局 darkAlgorithm |
| 12 chip 在浏览器缓存中显示旧色 | 加 `:where()` 提升 v2 变量优先级 |
| 暗色态渐变背景被新 bg-secondary 吃掉 | 渐变 overlay 用 `--text-primary` 反相（白）做底，不依赖 bg |
| `--primary` 从蓝变紫可能影响 antd 默认组件色 | 同步更新 `a-config-provider` 的 `colorPrimary` 为 `#a78bfa` |

---

**交付确认 → 写 handoff-design.md**
