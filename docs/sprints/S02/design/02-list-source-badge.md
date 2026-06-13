# S02 Design · 02 — 列表「来源」徽章 + 同步按钮

> 作者：designer-vicky @ 2026-06-07
> 依据：`ui-ux-pro-max` §1 A11y / §9 Navigation / §8 Forms
> 上游：[`../prd-git-source.md`](../prd-git-source.md) §4 US-04

## 1. 改动点

| 组件 | 改动 |
|------|------|
| `SkillCard.vue` | 新增 `<a-tag>` 来源徽章（紧贴标题下方） |
| `AdminSkillListView.vue` | 表格新增「来源」列（含徽章 + ref 文本） + 「同步」操作按钮 |
| `AdminSkillEditView.vue` | 编辑页底部新增「Git 同步」区域（仅 `source_type='GIT_URL'` 时显示） |

## 2. SkillCard 徽章规格

### 视觉

```
┌─────────────────────────────────────────┐
│ 📦  My Skill  [★ 精选]                   │
│         👤 Author · v1.0.0               │
│         [🔗 Git @ main]   ← 仅 GIT_URL   │
│                                          │
│ description...                           │
│ [tag] [tag] [tag]                        │
│ ★★★★☆ 4.5 (12)                  ⬇ 1.2k  │
└─────────────────────────────────────────┘
```

### 徽章矩阵

| source_type | 徽章 | 颜色 token | Icon + 文字 |
|-------------|------|------------|-------------|
| `LOCAL_ZIP` | `<a-tag color="default">` | gray-7 | `📦 本地` |
| `LOCAL_FILE` | `<a-tag color="default">` | gray-7 | `📄 .md` |
| `GIT_URL` | `<a-tag color="geekblue">` | geekblue-6 | `🔗 Git @ {ref}` |
| `null` / 旧数据 | 不渲染徽章 | — | — |

### 鼠标 hover 行为

- GIT_URL 徽章 → `<a-tooltip>` 显示完整 URL（最长截断 200 字符 + `…`）

## 3. AdminSkillListView 表格

### 表格列

| 列 | 宽 | 内容 |
|----|-----|------|
| 图标 | 60 | emoji |
| 名称 | — | displayName + slug |
| 分类 | 120 | categoryName |
| **来源**（新增列） | 140 | `<a-tag>` + 文字 + hover 提示 |
| 上传者 | 100 | username |
| 安装数 | 100 | 数字 |
| 浏览 | 100 | 数字 |
| 评分 | 180 | rate + count |
| 状态 | 100 | tag |
| 操作 | 240（fixed right） | 编辑 · 上下架 · 删除 · **同步** |

### 「同步」按钮规则

| 条件 | 是否显示 | 行为 |
|------|----------|------|
| `source_type === 'GIT_URL'` | ✓ | 点击 → 二次确认 Modal → 调 `syncSkill(id)` |
| 其它 | ✗ | 不渲染 |

### 二次确认 Modal

```
┌────────────────────────────────────────┐
│  ⚠️ 确认从 Git 同步                   │
├────────────────────────────────────────┤
│  将从远端重新拉取 Skill:                │
│                                        │
│  📦 My Skill  [🔗 Git @ main]          │
│  https://gitlab.example.com/team/sk.git│
│  token: ghp_xx****abcd                 │
│                                        │
│  ⚠️ 警告                                │
│  • 本地 SKILL.md 改动将被覆盖           │
│  • 本地 assets/ 改动将被覆盖             │
│  • 分类 / 标签 / 精选 标记将保留         │
│                                        │
│              [取消]  [确认同步]        │
└────────────────────────────────────────┘
```

- 「确认同步」按钮 = `<a-button type="primary" danger>`（semantic danger color，区别于普通 primary）
- Cancel 按钮 keyboard focus 默认
- Modal 标题用 `aria-label="确认从 Git 同步"`

## 4. 详情页底部「Git 同步」区域（仅 GIT_URL 显示）

```
┌─── Git 同步 ───────────────────────────────────┐
│  仓库 URL    https://gitlab.example.com/...     │
│  Branch      main                               │
│  Token       ghp_xx****abcd        [更换]       │
│  最后同步    2026-06-07 10:30 (12 分钟前)       │
│  状态        ✓ 上次同步成功                      │
│                                                │
│  [  立即同步  ]   size=large                    │
└────────────────────────────────────────────────┘
```

- 状态徽章：成功=绿色 ✓ · 失败=红色 ✗（带 lastSyncError 详情）· 同步中=loading
- 连续 3 次失败 → 状态徽章变橙色 + 提示「⚠️ 已连续失败 3 次,请检查仓库是否仍可访问」

## 5. A11y / 动效 / 状态走查

| 项 | 状态 |
|----|------|
| 徽章文字对比度 ≥ 4.5:1 | ✓ geekblue-6 (#2f54eb) on white = 7.2:1 |
| 徽章不用颜色作唯一区分 | ✓ 同时有 emoji + 文字 |
| 同步按钮 focus ring 可见 | ✓ Ant Design 默认 |
| Modal 出现动画 ≤ 300ms | ✓ Ant Design Modal 默认 200ms |
| 同步中按钮 disable + spinner | ✓ `:loading="true"` |
| 同步成功 toast 自动消失 | ✓ `message.success` 默认 3s |
| 同步失败保留表单 | ✓ Modal 关闭但 skill 详情不重置 |
| 键盘 Esc 关闭 Modal | ✓ Ant Design Modal 默认 |

## 6. 反模式（已规避）

| 反模式 | 规避 |
|--------|------|
| 表格行内操作挤在一起 | 「同步」用 `<a-space size="small">` + vertical divider |
| 同步按钮无 confirm | 强制二次确认 |
| 失败 silent | toast + 详情页 status badge + 控制台 error log |
| 成功/失败仅靠颜色 | 配合 icon（✓ / ✗） |

## 7. 交付物清单

- [ ] `frontend/src/components/SkillCard.vue` 加徽章
- [ ] `frontend/src/views/admin/AdminSkillListView.vue` 加列 + 同步按钮 + Modal
- [ ] `frontend/src/views/admin/AdminSkillEditView.vue` 加「Git 同步」面板
- [ ] `frontend/src/api/admin.ts` 加 `syncSkill` / `getSyncStatus` 方法
- [ ] `frontend/src/types/skill.ts` 加 `SourceType` + `sourceTokenEnc?`（前端不应收到）
