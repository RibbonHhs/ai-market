# 05 — ApiGuideView（文档页表格/代码块） — 暗色 wireframe

> **S28 状态**：已基本暗色（S27 写过 .markdown-body 暗色覆盖）
> **v2 修复**：把 S27 的 `#0d1117` 硬编码替换为 `--bg-elevated` token；表格新增斑马纹

## 暗色配色

| 元素 | token | 实色 |
|------|-------|------|
| 主内容 bg | `--bg-primary` | `#0d0d0f` |
| 标题区 bg | `--bg-secondary` | `#161618` |
| API endpoint 卡片 | `--bg-elevated` | `#26262c` |
| Method tag GET | `--success-bg` + `--success` | `rgba(52,211,153,0.16)` + `#34d399` |
| Method tag POST | `--primary-bg` + `--primary` | |
| Method tag PUT | `--warning-bg` + `--warning` | |
| Method tag DELETE | `--danger-bg` + `--danger` | |
| 表格表头 bg | `--bg-tertiary` | `#1f1f23` |
| 表格行 bg（奇） | `--bg-secondary` | `#161618` |
| 表格行 bg（偶） | `--bg-tertiary` | `#1f1f23`（斑马纹） |
| 表格行 hover | `--bg-elevated` | `#26262c` |
| 表格边框 | `--border` | `rgba(255,255,255,0.08)` |
| 代码块 bg | `--bg-elevated` | `#26262c`（v2 替代 S27 的 `#0d1117`） |
| 锚点 hover | `--primary` underline | |

## ASCII

```
┌──────────────────────────────────────────────────────────────────────┐
│  [SkillsMap]              Browse  API Guide  Docs       [🌓] [登录] │
├──────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌─ 侧边栏 ─────────────┐  ┌─ 内容 ───────────────────────────────┐  │
│  │                       │  │  # API 指南                          │  │
│  │  简介                 │  │                                      │  │
│  │  · 鉴权               │  │  ## 鉴权                              │  │
│  │  · 限流               │  │  所有 API 需要 Bearer token...         │  │
│  │                       │  │                                      │  │
│  │  Skills                │  │  ## 限流                              │  │
│  │  · GET /skills        │  │  100 req/min/IP                      │  │
│  │  · POST /skills       │  │                                      │  │
│  │  · GET /skills/:id    │  │  ────────────────────────────         │  │
│  │  · POST /skills/:id/  │  │                                      │  │
│  │     favorite          │  │  ## GET /skills                       │  │
│  │                       │  │  列出所有 skills。                    │  │
│  │  Reviews               │  │                                      │  │
│  │  · GET /reviews       │  │  [GET] /api/v1/skills                 │  │
│  │  · POST /reviews      │  │  ┌──────────────────────────────────┐ │  │
│  │                       │  │  │ Query Params:                    │ │  │
│  │                       │  │  │ ─────────────────                │ │  │
│  │                       │  │  │ page    int   页码  默认 1        │ │  │
│  │                       │  │  │ size    int   每页  默认 20       │ │  │
│  │                       │  │  │ usage   str  用途过滤            │ │  │
│  │                       │  │  └──────────────────────────────────┘ │  │
│  │                       │  │                                      │  │
│  │                       │  │  响应示例:                            │  │
│  │                       │  │  ┌──────────────────────────────────┐ │  │
│  │                       │  │  │ {                                │ │  │
│  │                       │  │  │   "code": 0,                     │ │  │
│  │                       │  │  │   "data": [...],                 │ │  │
│  │                       │  │  │   "total": 123                   │ │  │
│  │                       │  │  │ }                                │ │  │
│  │                       │  │  └──────────────────────────────────┘ │  │
│  │                       │  │                                      │  │
│  │                       │  │  ## 状态码                            │  │
│  │                       │  │  ┌────────┬──────────┬──────────┐    │  │
│  │                       │  │  │  code  │   msg    │  说明     │    │  │  ← 斑马纹
│  │                       │  │  ├────────┼──────────┼──────────┤    │  │
│  │                       │  │  │  0     │  成功    │  OK      │    │  │
│  │                       │  │  │  40001 │  限流    │  Too many│    │  │
│  │                       │  │  │  40100 │  未登录  │  No auth │    │  │
│  │                       │  │  │  50000 │  系统错  │  Server  │    │  │
│  │                       │  │  └────────┴──────────┴──────────┘    │  │
│  │                       │  │                                      │  │
│  └───────────────────────┘  └──────────────────────────────────────┘  │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

## 状态色

| 状态 | 表现 |
|------|------|
| 侧边栏链接 hover | 文字 `--text-secondary` → `--text-primary` + 左侧 2px `--primary` |
| 侧边栏链接 active | 文字 `--primary` + 左侧 2px `--primary` + bg `--primary-bg` |
| Method tag | 不同 method 不同色（GET 绿 / POST 紫 / PUT 琥珀 / DELETE 红） |
| 代码块顶部 | 1px 边框 `--border` 区分内容 |
| 表格行 hover | bg `--bg-elevated` |
| 锚点 hover | underline 出现 + 颜色 `--primary` |

## 自检

- [ ] 代码块用 `--bg-elevated` 而非硬编码 `#0d1117`（修复 S27）
- [ ] 表格斑马纹让横向阅读不疲劳
- [ ] Method tag 一眼区分 HTTP method
- [ ] 侧边栏在桌面端固定，移动端 Drawer
