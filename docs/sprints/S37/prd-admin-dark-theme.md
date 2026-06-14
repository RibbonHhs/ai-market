# S37 PRD — 暗色主题下后台管理可视性修复

> Sprint ID: S37
> 状态: 进行中
> 负责人: agile-rd-lead
> 创建: 2026-06-13

---

## 1. 背景与问题陈述

用户反馈：在系统处于 **暗色主题**（包括 `prefers-color-scheme: dark` 自动切换或显式选择 "dark"）时，`/admin/*` 域几乎所有管理页面的标题、表头、正文链接、说明文字"消失"——具体表现为浅灰/浅紫色文字落在浅色背景上，肉眼基本不可辨认，严重影响管理员执行增删改查。

### 1.1 影响面

| 范围 | 是否受影响 |
|------|-----------|
| `/admin/dashboard` | 是（标题"📊 Dashboard" + 统计卡片标题 + Git 状态说明文字） |
| `/admin/skills` | 是（"Skill 管理"页头 + 表头 + 批量操作栏 + 操作列） |
| `/admin/categories`（职业技能） | 是（"职业技能管理"页头 + 表头 + 操作链接） |
| `/admin/categories/usage`（用途分类） | 是（同上） |
| `/admin/tags` | 是（同上） |
| `/admin/users` | 是（同上） |
| `/admin/skills/:id/edit` | 是（继承 layout） |
| 前台 `/` 与 `/browse/*` | **否**（已正确接入主题） |

### 1.2 已采集证据

**Before 截图**（用户提供的 5 张全屏截图，均处于暗色主题下）：
- `docs/sprints/admin-dark-before/dashboard.png`
- `docs/sprints/admin-dark-before/skills-list.png`
- `docs/sprints/admin-dark-before/categories.png`
- `docs/sprints/admin-dark-before/users.png`
- `docs/sprints/admin-dark-before/tags.png`

**对比基线**（之前换完 LOGO 后在浅色背景下 admin 反而清晰，因为浅色背景下 admin 内的硬编码背景与暗色主题冲突不暴露）：
- `docs/sprints/logo-swap-admin.png`

---

## 2. 根因分析（RCA）

### 2.1 初判（已确认）

`frontend/src/views/admin/AdminLayout.vue` 的 scoped `<style lang="scss">` 中 **硬编码了浅色主题专用的背景色**：

```scss
.admin-header  { background: #fff;       border-bottom: 1px solid #f0f0f0; }
.admin-content { background: #f5f7fa; }
```

但 AdminLayout 内部的 Admin*View（`AdminDashboardView` / `AdminSkillListView` / `AdminCategoryView` / `AdminTagView` / `AdminUserView` / `AdminUsageCategoryView`）均使用 Ant Design Vue 组件（`a-page-header` / `a-card` / `a-table` / `a-tag` / `a-statistic` 等），这些组件的标题、表格头、说明文字本身通过 antdv 的主题算法产出，**在暗色主题下应使用浅色文字**。

### 2.2 冲突路径

1. `App.vue` 已经用 `a-config-provider` + `theme.darkAlgorithm` 给 antdv 组件应用暗色主题。
2. `:root[data-theme="dark"]` 已经把 `--text-primary` 切到 `rgba(255,255,255,0.92)`。
3. 但是 AdminLayout 内的 `<a-page-header>` 渲染时，**它的容器背景** 来自 `AdminLayout` scoped 的 `.admin-content { background: #f5f7fa }`——这个背景是浅色的。
4. antdv 的 `a-page-header` 标题使用 `colorTextHeading` 算法产出深色（**因为背景被强制成浅色，darkAlgorithm 在子组件上下文里可能仍然给标题计算成深色**）。
5. 同时，AdminSkillListView 内多个 inline `style="color: #999"`、`color: #bbb` 是固定浅灰——在暗色背景下几乎隐形。

### 2.3 复核（root cause 升级）

经实地读源码确认：
- **真正根因**：AdminLayout 的 `.admin-header` / `.admin-content` 硬编码浅色背景，与暗色主题不兼容。
- **次要根因**：AdminSkillListView 中存在多处硬编码颜色（`color: #999` / `color: #bbb` / `background: #e6f4ff` 等），即便修复 layout 也会残留"暗色下看不见"的子问题。
- **不涉及**：antdv darkAlgorithm 接入本身（`App.vue` 已正确接入），不是配置问题。

> 因此本 Sprint 修复策略 = **layout 去硬编码 + view 内 inline color 同步去硬编码**。

---

## 3. 用户故事

### US-1（必须）
> 作为 **管理员**，我希望在 **暗色主题下** 打开 `/admin/dashboard`，能清晰看到页面标题、4 个统计卡片、Skill 来源分布列表、快速操作按钮与 Git 同步状态，**不需要眯眼就能读**。

### US-2（必须）
> 作为 **管理员**，我希望在 **暗色主题下** 进入 `/admin/skills`，能清楚看到 Skill 列表的表头、批量操作栏、所有列内容、操作列链接；筛选表单 label 与 placeholder 可见。

### US-3（必须）
> 作为 **管理员**，我希望在 **暗色主题下** 完成增删改查操作：批量上架/下架/同步/删除的二次确认 modal 能看清警告文字、操作结果 toast 能看清成功/失败提示。

### US-4（必须）
> 作为 **管理员**，我希望 **浅色主题不被破坏**：同一份代码在浅色主题下视觉效果保持与"换 LOGO 后"基线一致，不能出现新的对比度问题。

### US-5（推荐，非阻塞）
> 作为 **管理员**，我希望侧栏（暗色 `theme="dark"`）与右侧内容区在视觉上"协调"——不会出现"侧栏纯黑 + 内容卡白"的撕裂感。

---

## 4. 验收标准（AC）

### 4.1 对比度（WCAG 2.1 AA）

| 元素 | 浅色 | 暗色 |
|------|------|------|
| 页面主标题（H2 / page-header title） | ≥ 4.5:1 | ≥ 4.5:1 |
| 表格表头文本 | ≥ 4.5:1 | ≥ 4.5:1 |
| 表格正文 | ≥ 4.5:1 | ≥ 4.5:1 |
| 操作列链接 | ≥ 4.5:1 | ≥ 4.5:1 |
| 副标题 / 描述 / 占位说明（tertiary） | ≥ 3:1 | ≥ 3:1 |
| 分隔线 / 边框 | ≥ 3:1（与背景对比） | ≥ 3:1 |

### 4.2 视觉

- after 截图与 before 截图肉眼对比：标题、表头、正文链接**全部清晰可见**
- 浅深双主题下都通过（同一份代码切换主题不出问题）
- 侧栏（暗）+ 内容区（暗）协调；浅色下侧栏（暗 antdv menu）+ 内容区（浅）也保持正常

### 4.3 工程

- `npm run build` 通过，零类型错误
- 不修改后端
- 不修改主页 / 浏览页 / `SkillLogo.vue` / 新换的 `public/logo.png`
- 全部产物归档到 `docs/sprints/S37/`

---

## 5. 非目标（Out of Scope）

- ❌ 不重新设计 admin 的整体视觉风格（不动排版、间距、字号阶梯）
- ❌ 不引入新的主题切换交互（保持现有的 cycle 行为）
- ❌ 不修复前台（主页 / 浏览页）的暗色问题（已知无问题）
- ❌ 不优化 antd darkAlgorithm 内部 token（仅做应用层适配）
- ❌ 不做 a11y 全量审计（仅覆盖 admin 域的可视性）

---

## 6. 涉及面 / 风险

### 6.1 涉及文件

| 类型 | 文件 |
|------|------|
| 修改 | `frontend/src/views/admin/AdminLayout.vue`（移除硬编码） |
| 修改 | `frontend/src/views/admin/AdminSkillListView.vue`（批量操作栏 + inline color 去硬编码） |
| 修改 | `frontend/src/views/admin/AdminDashboardView.vue`（Git 状态 inline color 暗色变体） |
| 新增 | `frontend/src/style/admin.scss`（admin 专属 token 集合） |
| 新增 | `frontend/src/style/global.scss` 中 admin token 段（与现有 16+12 token 同级） |
| 新增 | `docs/sprints/S37/`（PRD + 设计稿 + QA 截图） |

### 6.2 风险

| 风险 | 等级 | 缓解 |
|------|------|------|
| 浅色主题被破坏 | 中 | 必须双主题截图回归 |
| antd vtable 在暗色下边框对比度 | 中 | 用 `--border-strong` 而非 `--border` |
| 批量操作栏（蓝色 #e6f4ff）在暗色下 | 低 | 改用 `--primary-bg` + `--primary-border` |
| 侧栏 antdv `theme="dark"` 与内容区割裂 | 中 | 内容区背景用 `--bg-secondary`（与 body 拉开层级） |

### 6.3 不动项

- 后端 / 数据库 / API
- 前台 `HomeView` / `BrowseSkillsView` / `SkillDetailView`
- `SkillLogo.vue`
- `frontend/public/logo.png`
- 主题 store 行为
- `App.vue` 的 ConfigProvider（已正确，无需改）

---

## 7. 派单与依赖

```
PM (本文档) ─→ Designer (design-admin-dark-theme.md)
                       │
                       └→ Dev (实施 AdminLayout 去硬编码 + view inline color 修复 + admin.scss)
                                                  │
                                                  └→ QA (Playwright 巡检 6 页面 × 2 主题 = 12 截图)
```

---

## 8. 完成度门槛

1. 6 个 admin 页面在浅色与暗色下均产出 after 截图，**清晰可见**（肉眼判断）。
2. before/after 截图对比矩阵文档化（QA 产物）。
3. `npm run build` 通过。
4. 没有引入新的 lint / type 错误。
5. 不破坏前台任何页面。

---

## 9. 修订记录

| 日期 | 版本 | 变更 |
|------|------|------|
| 2026-06-13 | v1.0 | 初稿 |
