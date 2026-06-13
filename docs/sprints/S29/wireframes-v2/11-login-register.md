# 11 — LoginView / RegisterView（表单） — 暗色 wireframe

> **S28 状态**：基本暗色，需细化
> **v2 修复**：表单元素（input/label/error/help）全链路 v2 token

## 暗色配色

| 元素 | token | 实色 |
|------|-------|------|
| 页面 bg | `--bg-primary` | `#0d0d0f` |
| 卡片容器 bg | `--bg-elevated` | `#26262c` |
| 卡片边框 | `--border-strong` | `rgba(255,255,255,0.16)` |
| 卡片阴影 | `--shadow-lg` | `0 12px 32px rgba(0,0,0,.5)` |
| 标签 Label | `--text-secondary` | `rgba(255,255,255,0.68)` |
| 输入框 bg | `--bg-secondary` | `#161618` |
| 输入框边框 | `--border` | `rgba(255,255,255,0.08)` |
| 输入框文字 | `--text-primary` | `rgba(255,255,255,0.92)` |
| 输入框占位 | `--text-tertiary` | `rgba(255,255,255,0.42)` |
| 输入框 hover | `--border-strong` | |
| 输入框 focus | `--primary-border` 2px + 阴影 `0 0 0 4px rgba(167,139,250,0.16)` | |
| 输入框 error | `--danger` 边框 | `#f87171` |
| 帮助文字 | `--text-tertiary` | `rgba(255,255,255,0.42)` |
| 错误文字 | `--danger` | `#f87171` |
| 必填星号 | `--danger` | |
| 提交按钮 bg | `--primary` | `#a78bfa` |
| 提交按钮文字 | `--text-inverse` | `#0d0d0f` |
| 提交按钮 hover | `#c4b5fd` + scale(1.02) | |
| 提交按钮 loading | bg primary 0.7 + spinner | |
| 链接 | `--primary` | `#a78bfa` |
| 链接 hover | underline + `#c4b5fd` | |

## ASCII

```
┌──────────────────────────────────────────────────────────────────────┐
│  [SkillsMap]              Browse  API Guide  Docs       [🌓] [登录] │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│                                                                      │
│                  ┌──────────────────────────────────┐                │
│                  │  登录 SkillsMap                  │                │
│                  │                                  │                │
│                  │  邮箱  *                         │                │
│                  │  ┌────────────────────────────┐  │                │
│                  │  │ alice@example.com          │  │                │
│                  │  └────────────────────────────┘  │                │
│                  │                                  │                │
│                  │  密码  *                         │                │
│                  │  ┌────────────────────────────┐  │                │
│                  │  │ ••••••••             [👁]   │  │                │
│                  │  └────────────────────────────┘  │                │
│                  │                                  │                │
│                  │  ⚠ 邮箱或密码错误                │  ← error 红色  │
│                  │                                  │                │
│                  │  ┌────────────────────────────┐  │                │
│                  │  │         登 录              │  │  ← primary    │
│                  │  └────────────────────────────┘  │                │
│                  │                                  │                │
│                  │  忘记密码？  ·  立即注册          │  ← 链接       │
│                  │                                  │                │
│                  └──────────────────────────────────┘                │
│                                                                      │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

## 状态色

| 状态 | 表现 |
|------|------|
| 输入框 hover | 边框 border → border-strong |
| 输入框 focus | 边框 primary-border 2px + 4px 焦点环（0.16 alpha） |
| 输入框 error | 边框 danger 2px + 下方红字 |
| 输入框 disabled | bg bg-secondary 0.5 + text-tertiary |
| 提交按钮 hover | bg primary → #c4b5fd + scale(1.02) |
| 提交按钮 loading | bg primary 0.7 + spinner + 文字 "登录中..." |
| 提交按钮 disabled | bg bg-tertiary + text-tertiary |
| 密码 toggle | 眼睛图标 text-tertiary → text-secondary |
| 链接 hover | underline + 颜色 primary → #c4b5fd |

## 自检

- [ ] 输入框在 dark 下有明显焦点环（a11y 关键）
- [ ] 必填星号、错误提示不依赖颜色单独传达（同时有文字 + 图标）
- [ ] 提交按钮 loading 状态可识别
- [ ] 卡片阴影在 dark 下也清晰（shadow-lg 50% 黑）
