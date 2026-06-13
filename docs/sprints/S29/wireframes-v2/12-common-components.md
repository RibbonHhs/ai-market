# 12 — Footer / 通用组件（按钮/输入框/表格） — 暗色 wireframe

> **范围**：通用组件规范
> **v2 修复**：所有组件使用同一套 token，应用层无硬编码

---

## 12.1 Footer（页脚）

### 暗色配色

| 元素 | token | 实色 |
|------|-------|------|
| Footer bg | `--bg-secondary` | `#161618` |
| Footer 顶部边框 | `--border` | `rgba(255,255,255,0.08)` |
| 标题 | `--text-primary` | `rgba(255,255,255,0.92)` |
| 链接 | `--text-secondary` | `rgba(255,255,255,0.68)` |
| 链接 hover | `--primary` | `#a78bfa` |
| 版权 | `--text-tertiary` | `rgba(255,255,255,0.42)` |

### ASCII

```
┌──────────────────────────────────────────────────────────────────────┐
│ ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓ bg-secondary #161618 ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓│
│                                                                      │
│  SkillsMap          产品           资源         社区                  │
│  让工作流自动化      Browse        API Guide    GitHub                │
│  © 2026             API Guide     Docs         Discord               │
│                     Pricing       Blog         Twitter               │
│                                                                      │
│  ─────────────────────────────────────────────────────────────────   │
│  © 2026 SkillsMap · MIT · 备案号 xxx                                  │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

---

## 12.2 通用按钮（Button）

### 变体

| 变体 | bg | 文字 | 边框 | 用途 |
|------|-----|------|------|------|
| **Primary** | `--primary` | `--text-inverse` | 无 | 主要 CTA |
| **Secondary** | `--bg-tertiary` | `--text-primary` | `--border` | 次要操作 |
| **Ghost** | transparent | `--text-primary` | `--border` | 三级操作 |
| **Danger** | `--danger` | `#ffffff` | 无 | 危险操作 |
| **Link** | transparent | `--primary` | 无 | 行内链接 |

### 状态

| 状态 | 表现 |
|------|------|
| Hover | bg 提亮 8% + scale(1.02) 150ms ease-out |
| Active | bg 压暗 4% + scale(0.98) |
| Focus | 2px 焦点环 `--primary-border` |
| Disabled | bg `--bg-tertiary` + opacity 0.5 + cursor not-allowed |
| Loading | bg 0.7 + spinner + 文字保留 |

---

## 12.3 通用输入框（Input）

### 状态

| 状态 | bg | 边框 | 文字 |
|------|-----|------|------|
| 默认 | `--bg-secondary` | `--border` | `--text-primary` |
| Hover | `--bg-secondary` | `--border-strong` | `--text-primary` |
| Focus | `--bg-secondary` | `--primary-border` 2px + 4px 焦点环 | `--text-primary` |
| Error | `--bg-secondary` | `--danger` 2px | `--text-primary` |
| Disabled | `--bg-secondary` 0.5 | `--border` 0.5 | `--text-tertiary` |
| Placeholder | — | — | `--text-tertiary` |

---

## 12.4 通用表格（Table）

### 配色

| 元素 | token |
|------|-------|
| 表头 bg | `--bg-tertiary` |
| 表头文字 | `--text-secondary` 12px / 500 weight / uppercase |
| 表格行 bg（奇） | `--bg-secondary` |
| 表格行 bg（偶） | `--bg-tertiary`（斑马纹） |
| 表格行 hover | `--bg-elevated` |
| 表格边框 | `--border` |
| 表格文字 | `--text-primary` |
| 表格次要文字 | `--text-secondary` |

### ASCII

```
┌────────────────────────────────────────────────────────────────┐
│  NAME            USAGE         RATING   DOWNLOADS   ACTIONS   │  ← 表头 bg-tertiary
├────────────────────────────────────────────────────────────────┤
│  React Hooks    [🛠 工具]      ⭐ 4.8    1.2k        [查看]    │  ← 奇行 bg-secondary
├────────────────────────────────────────────────────────────────┤
│  LLM Prompts    [🤖 AI]        ⭐ 4.7    800         [查看]    │  ← 偶行 bg-tertiary
├────────────────────────────────────────────────────────────────┤
│  Vue 3 Setup    [💻 开发]      ⭐ 4.6    650         [查看]    │
└────────────────────────────────────────────────────────────────┘
   ↑ hover: bg-elevated
```

---

## 12.5 空状态（Empty）

### 配色

| 元素 | token |
|------|-------|
| 插画区 | `--text-tertiary` 0.6 opacity |
| 标题 | `--text-primary` |
| 描述 | `--text-secondary` |
| CTA 按钮 | primary |

### ASCII

```
┌────────────────────────────────────────────────────────────────┐
│                                                                │
│                          [📦 图标]                             │
│                                                                │
│                    还没有任何 skill                             │
│           试试调整筛选条件，或者成为第一个贡献者                 │
│                                                                │
│                       [ 清空筛选 ]                             │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## 12.6 加载状态（Loading）

### 变体

| 变体 | 表现 |
|------|------|
| Skeleton | bg `--bg-tertiary` + shimmer 1.5s loop |
| Spinner | 圆形旋转 1s linear infinite，颜色 `--primary` |
| Progress | 顶部 2px 进度条，颜色 `--primary` |

### Skeleton 示例

```
┌────────────────────────────────────────────────────────────────┐
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓  ← bg-tertiary + shimmer                  │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓                                              │
│  ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓                                │
└────────────────────────────────────────────────────────────────┘
```

---

## 12.7 错误状态（Error）

### 配色

| 元素 | token |
|------|-------|
| 卡片 bg | `--danger-bg` `rgba(248,113,113,0.08)` |
| 卡片边框 | `--danger-border` `rgba(248,113,113,0.32)` |
| 图标 | `--danger` |
| 标题 | `--text-primary` |
| 描述 | `--text-secondary` |
| 操作按钮 | primary + ghost |

### ASCII

```
┌────────────────────────────────────────────────────────────────┐
│  ⚠ 加载失败                                                    │
│                                                                │
│  网络连接异常，请检查后重试。                                  │
│                                                                │
│  [ 重试 ]   [ 反馈问题 ]                                       │
└────────────────────────────────────────────────────────────────┘
```

---

## 12.8 权限不足（Permission Denied）

### 配色

| 元素 | token |
|------|-------|
| 卡片 bg | `--bg-secondary` |
| 边框 | `--border` |
| 图标 | `--text-tertiary` |
| 标题 | `--text-primary` |
| 描述 | `--text-secondary` |
| 登录按钮 | primary |

### ASCII

```
┌────────────────────────────────────────────────────────────────┐
│                                                                │
│                          [🔒 图标]                              │
│                                                                │
│                    需要登录才能访问                              │
│           登录后即可收藏、评论、提交 skill                       │
│                                                                │
│                     [ 立即登录 ]                                │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

---

## 自检（全部 12 张）

- [ ] 所有 bg 用 token（无硬编码 `#0d1117` / `#141414` / `#fff`）
- [ ] 所有 fg 用 token（无硬编码 `#fff` / `#000`）
- [ ] 所有边框用 `--border` / `--border-strong`
- [ ] 所有阴影用 `--shadow-sm/md/lg`
- [ ] 所有 chip 用 v2 §3 12 色
- [ ] 所有交互元素有 hover / focus / active / disabled 4 态
- [ ] 所有空/加载/错误/权限 4 态有规范
