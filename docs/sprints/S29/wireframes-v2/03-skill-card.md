# 03 — SkillCard — 暗色 wireframe

> **S27 错误**：卡片在暗色下仍是白底（漏改）
> **v2 修复**：卡片 bg 用 `--bg-secondary`，边框用 `--border`，hover 用 `--bg-tertiary`

## 暗色配色

| 元素 | token | 实色 |
|------|-------|------|
| 卡片 bg | `--bg-secondary` | `#161618` |
| 卡片边框 | `--border` | `rgba(255,255,255,0.08)` |
| 卡片 hover bg | `--bg-tertiary` | `#1f1f23` |
| 卡片 hover 边框 | `--border-strong` | `rgba(255,255,255,0.16)` |
| 卡片 hover 阴影 | `--shadow-md` | `0 4px 12px rgba(0,0,0,.3), inset 0 1px 0 rgba(255,255,255,0.04)` |
| Skill 标题 | `--text-primary` | `rgba(255,255,255,0.92)` |
| Skill 描述 | `--text-secondary` | `rgba(255,255,255,0.68)` |
| 12 用途 chip | 见 dark-tokens-v2 §3 | |
| 收藏图标（未收藏） | `--text-tertiary` | `rgba(255,255,255,0.42)` |
| 收藏图标（已收藏） | `--primary` | `#a78bfa` |
| 评分星 | `--warning` | `#fbbf24` |
| 作者 | `--text-tertiary` | `rgba(255,255,255,0.42)` |

## ASCII

```
┌────────────────────────────┐
│ [🛠 工具]      [♡]         │  ← chip (PURPOSE-TOOL) + 收藏 icon
│                            │
│ React Hooks 速查            │  ← h3 18px text-primary
│ 150+ 高频 hooks 示例...    │  ← p 14px text-secondary (2 行 truncate)
│                            │
│ ⭐ 4.8  · by @alice · 1.2k  │  ← meta text-tertiary
│                            │
│ #react  #hooks  #前端      │  ← 标签 text-tertiary
└────────────────────────────┘
  ↑ bg-secondary #161618
  ↑ border rgba(255,255,255,0.08)
  ↑ hover: bg-tertiary + shadow-md + translateY(-2px)
```

## 状态色

| 状态 | 表现 |
|------|------|
| 默认 | bg-secondary + border 0.08 |
| Hover | bg-tertiary + border 0.16 + shadow-md + translateY(-2px) 200ms ease-out |
| 收藏 icon hover | scale(1.1) + 颜色由 text-tertiary → primary |
| 收藏 icon 激活 | 填充色 `--primary`，心形 scale pulse 150ms |
| Focus (键盘) | 2px 焦点环 `--primary-border` |

## 自检

- [ ] 卡片在暗色下不是白底（修复 S27）
- [ ] chip 颜色与 bg-secondary 融合自然（16% alpha 软底）
- [ ] hover 状态有明显视觉反馈但不刺眼
- [ ] 卡片高度 4:3 比例（信息密度适中）
