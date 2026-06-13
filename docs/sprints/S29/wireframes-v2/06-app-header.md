# 06 — AppHeader（导航） — 暗色 wireframe

> **S28 状态**：已加手动切换按钮
> **v2 修复**：背景从 `var(--skillsmap-bg)` 改为 `--bg-elevated`，与主背景区分；logo 区域可加品牌色微高光

## 暗色配色

| 元素 | token | 实色 |
|------|-------|------|
| Header bg | `--bg-elevated` | `#26262c` |
| Header 底部边框 | `--border` | `rgba(255,255,255,0.08)` |
| Logo 文字 | `--text-primary` | `rgba(255,255,255,0.95)` |
| Logo 副色（图标） | `--primary` | `#a78bfa` |
| 导航链接（未选） | `--text-secondary` | `rgba(255,255,255,0.68)` |
| 导航链接 hover | `--text-primary` | `rgba(255,255,255,0.92)` |
| 导航链接 active | `--primary` | `#a78bfa` + 底部 2px |
| 搜索框 bg | `--bg-secondary` | `#161618` |
| 搜索框边框 | `--border` | `rgba(255,255,255,0.08)` |
| 搜索框 focus | `--primary-border` | `rgba(167,139,250,0.32)` + 2px 焦点环 |
| 主题切换按钮 | `--text-secondary` | `rgba(255,255,255,0.68)` |
| 主题切换按钮 hover | `--text-primary` | `rgba(255,255,255,0.92)` + bg `--bg-tertiary` |
| 头像边框 | `--border-strong` | `rgba(255,255,255,0.16)` |
| 下拉菜单 bg | `--bg-elevated` | `#26262c` |
| 下拉菜单 hover | `--bg-tertiary` | `#1f1f23` |

## ASCII

```
┌──────────────────────────────────────────────────────────────────────┐
│ ▓▓▓▓▓ bg-elevated #26262c + 底部 border rgba(255,255,255,0.08) ▓▓▓▓│
│                                                                      │
│  [◆ SkillsMap]    Browse   API Guide   Docs   Skills     🔍 [⌘K] 🌓 │  │
│                                            ▓▓▓▓▓▓▓                ▓ │  │
│                                            (active 导航 active=primary)│
│                                                              👤 alice▼│
└──────────────────────────────────────────────────────────────────────┘
   ↑                                    ↑                            ↑
   Logo (primary 图标 + text-primary)   搜索框 bg-secondary      头像 + 下拉
```

## 状态色

| 状态 | 表现 |
|------|------|
| 导航 hover | 文字 text-secondary → text-primary + bg `--bg-tertiary` |
| 导航 active | 文字 `--primary` + 底部 2px primary |
| 搜索 focus | 边框 `--primary-border` 2px + 阴影 `0 0 0 4px rgba(167,139,250,0.16)` |
| 主题按钮 hover | bg `--bg-tertiary` + 圆形 ripple |
| 头像 hover | 边框 `--primary-border` |
| 下拉菜单项 hover | bg `--bg-tertiary` |
| 下拉菜单项 destructive（注销） | 文字 `--danger` |

## 自检

- [ ] Header bg 与主背景有明显分层（elevated 比 primary 浅 11%）
- [ ] 当前路由（active）有清晰视觉指示
- [ ] 主题切换按钮三态图标清晰：auto / light / dark
- [ ] 移动端折叠为汉堡菜单，bg 同 `--bg-elevated`
