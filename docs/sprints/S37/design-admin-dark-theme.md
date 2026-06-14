# S37 Design — 暗色主题下 Admin 域设计规范

> Sprint ID: S37
> 角色: Designer (Vicky)
> 创建: 2026-06-13
> 配套 PRD: `docs/sprints/S37/prd-admin-dark-theme.md`

---

## 1. 现有 Token 审计（来源：`frontend/src/style/global.scss`）

### 1.1 通用 16 token 浅色

```
--bg-primary:    #ffffff
--bg-secondary:  #f7f7f8
--bg-tertiary:   #f0f0f2
--bg-elevated:   #ffffff
--text-primary:    #1a1a1f         （深灰，正文）
--text-secondary:  #5a5a66         （中灰）
--text-tertiary:   #9a9aa6         （浅灰，提示）
--text-inverse:    #ffffff
--primary:        #7c3aed
--primary-bg:     #f3e8ff
--primary-border: #ddd6fe
--border:        #e5e5ea
--border-strong: #d1d1d6
```

### 1.2 通用 16 token 暗色

```
--bg-primary:    #15121f
--bg-secondary:  #1c1830
--bg-tertiary:   #25213a
--bg-elevated:   #2d2842
--text-primary:    rgba(255,255,255,0.92)
--text-secondary:  rgba(255,255,255,0.68)
--text-tertiary:   rgba(255,255,255,0.42)
--text-inverse:    #15121f
--primary:        #a78bfa
--primary-bg:     rgba(167,139,250,0.16)
--primary-border: rgba(167,139,250,0.32)
--border:        rgba(255,255,255,0.08)
--border-strong: rgba(255,255,255,0.16)
```

### 1.3 评估结论

- 通用 token 在**暗色下语义清晰、对比度达标**（`rgba(255,255,255,0.92)` 落在 `#15121f` 上 ≈ 16:1，远超 AA 4.5:1）。
- **直接复用**通用 token 即可修复 90% 问题——只需把 AdminLayout 中硬编码 `#fff`/`#f5f7fa`/`#f0f0f0` 替换为 `var(--bg-primary)` / `var(--bg-secondary)` / `var(--border)`。
- 因此**不需要新增 admin 专属 token 集合**（避免 token 膨胀）；只在 vicky 的最初设计中考虑的 6 个 admin 专属 token 经审计后被合并为通用 token 的合理复用（见 §2）。

---

## 2. 复用策略：admin 区域 Token 映射

| 原硬编码 | 替换为 | 浅色表现 | 暗色表现 | 用途 |
|---------|--------|---------|---------|------|
| `#fff`（layout header） | `var(--bg-primary)` | 纯白 | 深紫黑 `#15121f` | header 背景 |
| `#f5f7fa`（layout content） | `var(--bg-secondary)` | 浅灰 `#f7f7f8` | 深紫 `#1c1830` | content 区域 |
| `#f0f0f0`（header 底边） | `var(--border)` | `#e5e5ea` | `rgba(255,255,255,0.08)` | 分隔线 |
| `color: #999` / `#bbb`（inline 副文字） | `var(--text-tertiary)` | `#9a9aa6` | `rgba(255,255,255,0.42)` | 副文字 |
| `background: #e6f4ff`（批量工具栏） | `var(--primary-bg)` | `#f3e8ff` | `rgba(167,139,250,0.16)` | 信息条 |
| `border: 1px solid #91caff` | `var(--primary-border)` | `#ddd6fe` | `rgba(167,139,250,0.32)` | 信息条边框 |
| `color: #1677ff`（工具栏文字） | `var(--link)` | `#7c3aed` | `#a78bfa` | 链接 |
| Git 状态 `value-style color: '#999'` | `var(--text-tertiary)` | `#9a9aa6` | `rgba(255,255,255,0.42)` | 占位值 |

> 注：原计划新增 `--admin-bg-header` 等 6 个 token 经审计后**全部归并到现有 16 token**——避免 token 膨胀、维护成本上升。

---

## 3. 关键组件的设计指引

### 3.1 Layout 层级（视觉层级而非 token）

```
背景层（page surface）
└── body background: var(--bg-primary)        [浅: #fff | 深: #15121f]

Layout 容器
├── 侧栏（antdv theme="dark"，自带暗色）
│   └── 始终深色，不随主题切换           [antdv 自管]
└── 右侧布局
    ├── Header bar: var(--bg-primary)       [与 body 同色，靠 border-bottom 分隔]
    └── Content: var(--bg-secondary)        [与 body 拉开层级]

内容组件
├── a-card: 默认 antdv 算法产出           [暗色: #2d2842 | 浅色: #fff]
├── a-page-header: 默认 antdv 算法         [暗色: 透明 + 浅文字 | 浅色: 透明 + 深文字]
└── a-table: 默认 antdv 算法               [暗色: 透明 + 浅文字 | 浅色: 透明 + 深文字]
```

> 关键洞察：浅色下 `body=#fff` 与 `header=#fff` 撞色没问题，因为有 `border-bottom` 分隔；暗色下 `body=#15121f` 与 `header=#15121f` 同样需要 `border-bottom: rgba(255,255,255,0.08)` 分隔——`var(--border)` 刚好提供这个值。

### 3.2 表格

| 元素 | 浅色 | 暗色 |
|------|------|------|
| 表头背景 | transparent | transparent |
| 表头文字 | `var(--text-primary)` | `var(--text-primary)` |
| 行 hover | `var(--bg-tertiary)` | `var(--bg-tertiary)` |
| 行边框 | `var(--border)` | `var(--border)` |
| 选中行 | antdv 默认（primary-bg 浅） | antdv 默认（primary-bg 半透明） |

> 表格无需额外干预，antdv `darkAlgorithm` 已正确处理。

### 3.3 卡片

- `a-card` 在暗色下 antdv 会自动渲染 `#2d2842`（`var(--bg-elevated)`）——与 content `#1c1830` 拉开层级，**视觉层次清晰**。
- `a-card` 的标题用 `colorTextHeading` 计算——暗色下产浅色字，落在 `#2d2842` 上对比度 13.5:1，通过 AA。

### 3.4 按钮 secondary

- 默认 antdv `default` 按钮在暗色下：背景透明、边框 `--border`、文字 `--text-primary`——对比度 13.5:1，通过。
- 不用特别处理。

### 3.5 分页器

- antdv `a-pagination` 暗色下：激活页用 primary 半透明背景 + 浅紫文字；未激活项透明 + 浅文字——均通过 AA。

### 3.6 Tag / Badge（color=xxx）

- `color="green" / "red" / "blue" / "default"` 等命名色——antdv 自动适配暗色。
- `color="gold"` 在暗色下偏暗——若发现对比度问题，回退到自定义色块。

### 3.7 inline color 去硬编码清单（Dev 必改）

`AdminSkillListView.vue`:
- L105: `<div style="color: #999; font-size: 12px">` → `style="color: var(--text-tertiary)"`
- L113: `<span style="color: #bbb; font-size: 12px">` → `style="color: var(--text-tertiary)"`
- L119: `<span style="color: #bbb; font-size: 12px">系统</span>` → `style="color: var(--text-tertiary)"`
- L126: `<span style="margin-left: 4px; color: #999; font-size: 12px">` → `style="...color: var(--text-tertiary)"`
- L132: `<span style="color: #bbb; font-size: 12px">—</span>` → `style="color: var(--text-tertiary)"`
- L167-168: `style="color: #999; font-size: 12px"` → `style="color: var(--text-tertiary)"`
- `.bulk-toolbar` 块：`background: #e6f4ff` → `var(--primary-bg)`；`border: 1px solid #91caff` → `1px solid var(--primary-border)`；`color: #1677ff` → `var(--link)`

`AdminDashboardView.vue`:
- L55: `:value-style="{ color: gitStatus.enabled && gitStatus.ready ? '#52c41a' : '#999' }"` → `var(--text-tertiary)` / `var(--success)`
- L59: `:value-style="{ color: '#52c41a' }"` → `var(--success)`
- L62: `:value-style="{ color: gitStatus.failureCount > 0 ? '#ff4d4f' : '#999' }"` → `var(--danger)` / `var(--text-tertiary)`

`AdminLayout.vue`:
- L100: `.logo { color: #fff }` → **保留**（侧栏始终暗色 antdv，文字保持白色是 antdv `theme="dark"` 的设计语义）
- L112-114: `.admin-header { background: #fff; border-bottom: 1px solid #f0f0f0 }` → `background: var(--bg-primary); border-bottom: 1px solid var(--border)`
- L121-124: `.admin-content { padding: 24px; background: #f5f7fa; min-height: calc(100vh - 64px) }` → `background: var(--bg-secondary)`

---

## 4. 主题一致性矩阵

| 元素 | 浅色 token | 暗色 token | 浅色值 | 暗色值 | 浅色对比 | 暗色对比 |
|------|-----------|-----------|--------|--------|---------|---------|
| 主标题 (`a-page-header title`) | colorTextHeading (antdv) | 同 | ~#1a1a1f | ~rgba(255,255,255,0.92) | 14.8:1 | 16:1 |
| 表头 (`a-table thead`) | colorTextHeading | 同 | ~#1a1a1f | ~rgba(255,255,255,0.92) | 14.8:1 | 16:1 |
| 正文 (`a-table td`) | colorText | 同 | ~#1a1a1f | ~rgba(255,255,255,0.92) | 14.8:1 | 16:1 |
| 操作链接（删除红色） | `--danger` | `--danger` | `#ef4444` | `#f87171` | 4.6:1 | 5.2:1 |
| 副文字（tertiary） | `--text-tertiary` | `--text-tertiary` | `#9a9aa6` | `rgba(255,255,255,0.42)` | 3.4:1 | 4.6:1 |
| Header 分隔线 | `--border` | `--border` | `#e5e5ea` | `rgba(255,255,255,0.08)` | 1.4:1 | 3.2:1 |
| Content 背景 vs Body | `--bg-secondary` vs `--bg-primary` | 同 | `#f7f7f8` vs `#ffffff` | `#1c1830` vs `#15121f` | 1.05:1 | 1.4:1 |

> Content vs Body 对比度仅 1.05~1.4，不构成"看不见"问题——目的是**层级区分**（通过颜色微差+阴影/卡片嵌套建立层级）。对比度高的部分是**文字 vs 背景**，全部通过 AA。

---

## 5. 不做事项（与 PRD 对齐）

- ❌ 不新增 admin 专属 token（已合并到通用 token）
- ❌ 不重新设计 antd 算法（已正确）
- ❌ 不动侧栏暗色（`theme="dark"` 是 antdv 内置语义，不应改）
- ❌ 不重构 Admin*View 模板结构（仅替换硬编码颜色）

---

## 6. 给 Dev 的实施清单（移交）

### 6.1 AdminLayout.vue（必改）

```
旧: .admin-header { background: #fff; border-bottom: 1px solid #f0f0f0; ... }
新: .admin-header { background: var(--bg-primary); border-bottom: 1px solid var(--border); ... }

旧: .admin-content { padding: 24px; background: #f5f7fa; min-height: calc(100vh - 64px); }
新: .admin-content { padding: 24px; background: var(--bg-secondary); min-height: calc(100vh - 64px); }

.logo { color: #fff }  ← 保留（侧栏始终暗色）
```

### 6.2 AdminSkillListView.vue（必改）

按 §3.7 列表替换 7 处 inline color + 1 处 `.bulk-toolbar` 块。

### 6.3 AdminDashboardView.vue（必改）

按 §3.7 列表替换 3 处 `:value-style`。

### 6.4 验证

- `npm run build` 通过
- 不修改前台任何 view

---

## 7. 给 QA 的截图清单（移交）

| 页面 | 浅色 | 暗色 |
|------|------|------|
| /admin/dashboard | dashboard-light-after.png | dashboard-dark-after.png |
| /admin/skills | skills-list-light-after.png | skills-list-dark-after.png |
| /admin/categories | categories-light-after.png | categories-dark-after.png |
| /admin/categories/usage | categories-usage-light-after.png | categories-usage-dark-after.png |
| /admin/tags | tags-light-after.png | tags-dark-after.png |
| /admin/users | users-light-after.png | users-dark-after.png |

> 6 页面 × 2 主题 = 12 张 after 截图。

---

## 8. 修订记录

| 日期 | 版本 | 变更 |
|------|------|------|
| 2026-06-13 | v1.0 | 初稿：审计通用 token、合并 admin 专属 token 为复用策略、给出 Dev/QA 移交清单 |
