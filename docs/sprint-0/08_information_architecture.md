# Information Architecture（信息架构）

> 作者：designer-vicky @ Sprint 0 Kickoff (2026-06-06)
>
> 版本：v0.1 · 范围：v1 全站 · 引用：`07_design_system_v0.md` / `04_product_backlog_v1.md` / `.claude/CLAUDE.md` §前端栈

## 1. 站点总览

SkillsMap 站点分**用户端（公开）**和**管理端（ADMIN）**两大分区，**鉴权路由**统一通过 Vue Router 4 `meta.requiresAuth` / `meta.requiresAdmin` 控制。

| 分区 | 路径前缀 | 鉴权 | 角色 |
|---|---|---|---|
| 用户端 | `/` | 公开（部分写操作需登录） | 全部 |
| 个人中心 | `/me` | 需登录 | USER |
| 管理端 | `/admin` | 需 ADMIN | ADMIN |
| 鉴权 | `/login` / `/register` | 公开 | 全部 |

## 2. 导航树（Navigation Tree）

```
SkillsMap
│
├── /  (Home 首页)
│   ├── Hero 搜索
│   ├── 精选 Skills（精选区）
│   ├── 最新 Skills
│   ├── 热门 Skills
│   └── 分类入口（10 个分类）
│
├── /browse  (浏览页)
│   ├── 关键词搜索
│   ├── 分类筛选
│   ├── 标签筛选
│   ├── 排序（最新 / 安装 / 评分 / 浏览）
│   └── 分页
│
├── /skills/:id  (详情页) ── 别名 /skills/slug/:slug
│   ├── 基础信息
│   ├── SKILL.md 渲染
│   ├── 安装命令（复制）
│   ├── 评分列表
│   └── 相关推荐
│
├── /categories  (分类总览)
│   └── 分类网格
│
├── /login  (登录)
├── /register  (注册)
│
├── /me  (个人中心)
│   ├── /me/favorites  (我的收藏)
│   ├── /me/reviews  (我的评价)
│   └── /me/settings  (账号设置)
│
└── /admin  (后台 — ADMIN)
    ├── /admin/dashboard
    ├── /admin/skills
    │   ├── /admin/skills/new
    │   └── /admin/skills/:id/edit
    ├── /admin/categories
    ├── /admin/tags
    ├── /admin/users
    └── /admin/import  (本地扫描导入)
```

## 3. 路由表（Router Table）

> 路径命名遵循 `.claude/CLAUDE.md` §文件命名（kebab-case）→ 视图大驼峰 `View.vue`

| 路径 | 组件 | meta.requiresAuth | meta.requiresAdmin | 标题 | 关联 Story |
|---|---|---|---|---|---|
| `/` | `HomeView.vue` | false | false | SkillsMap — Claude Skills 集市 | US-001~005 |
| `/browse` | `BrowseView.vue` | false | false | 浏览 Skills | US-006~009, 031 |
| `/skills/:id` | `SkillDetailView.vue` | false | false | Skill 详情 | US-010~013, 032, 033, 040 |
| `/skills/slug/:slug` | `SkillDetailView.vue` | false | false | Skill 详情（按 slug） | 同上 |
| `/categories` | `CategoriesView.vue` | false | false | 全部分类 | US-041 |
| `/login` | `LoginView.vue` | false | false | 登录 | US-015 |
| `/register` | `RegisterView.vue` | false | false | 注册 | US-014 |
| `/me` | `MeView.vue` (重定向到 `/me/favorites`) | true | false | 个人中心 | US-016, 017 |
| `/me/favorites` | `MyFavoritesView.vue` | true | false | 我的收藏 | US-016 |
| `/me/reviews` | `MyReviewsView.vue` | true | false | 我的评价 | US-017 |
| `/me/settings` | `MeSettingsView.vue` | true | false | 账号设置 | — |
| `/admin` | `AdminLayout.vue` (重定向到 `/admin/dashboard`) | true | true | 后台 | US-020 |
| `/admin/dashboard` | `DashboardView.vue` | true | true | 仪表盘 | US-021, 037 |
| `/admin/skills` | `AdminSkillsView.vue` | true | true | Skill 管理 | US-022, 025, 026 |
| `/admin/skills/new` | `AdminSkillNewView.vue` | true | true | 新建 Skill | US-023 |
| `/admin/skills/:id/edit` | `AdminSkillEditView.vue` | true | true | 编辑 Skill | US-024 |
| `/admin/categories` | `AdminCategoriesView.vue` | true | true | 分类管理 | US-027 |
| `/admin/tags` | `AdminTagsView.vue` | true | true | 标签管理 | US-028 |
| `/admin/users` | `AdminUsersView.vue` | true | true | 用户管理 | US-029 |
| `/admin/import` | `AdminImportView.vue` | true | true | 导入管理 | US-035 |
| `/404` | `NotFoundView.vue` | false | false | 页面不存在 | — |
| `/403` | `ForbiddenView.vue` | true | false | 无权访问 | US-030 |

### 3.1 路由守卫

```typescript
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !auth.roles.includes('ADMIN')) {
    return { path: '/403' }
  }
  return true
})
```

## 4. 关键页面清单（Key Pages）

| 页面 | 路径 | 目的 | 关键组件 | 关联 Story |
|---|---|---|---|---|
| 首页 | `/` | 让访客快速进入浏览 / 搜索 | `HeroSearch`, `SkillGrid`, `CategoryGrid` | US-001~005 |
| 浏览页 | `/browse` | 列表 + 过滤 + 排序 | `SkillGrid`, `FilterBar`, `Pagination` | US-006~009 |
| 详情页 | `/skills/:id` | 展示 SKILL.md + 评分 + 安装 | `MarkdownView`, `InstallCommandBox`, `RatingStars` | US-010~013 |
| 分类总览 | `/categories` | 展示 10 个分类 | `CategoryGrid` | US-041 |
| 登录 | `/login` | 鉴权入口 | `LoginForm` | US-015 |
| 注册 | `/register` | 拉新 | `RegisterForm` | US-014 |
| 我的收藏 | `/me/favorites` | 收藏列表 | `SkillGrid` (迷你版) | US-016 |
| 我的评价 | `/me/reviews` | 评价历史 | `ReviewList` | US-017 |
| 仪表盘 | `/admin/dashboard` | 总览 + 来源分布 | `StatCards`, `SourcePieChart` | US-021 |
| Skill 管理 | `/admin/skills` | 列表 + 过滤 | `DataTable`, `PublishToggle` | US-022 |
| 新建 / 编辑 Skill | `/admin/skills/new` `/admin/skills/:id/edit` | 表单 CRUD | `SkillForm`, `MarkdownEditor` | US-023, 024 |
| 分类管理 | `/admin/categories` | CRUD | `DataTable`, `CategoryForm` | US-027 |
| 标签管理 | `/admin/tags` | 列表 + 删除 | `DataTable` | US-028 |
| 用户管理 | `/admin/users` | 列表 + 改角色 + 启禁 | `DataTable`, `RoleSelect`, `StatusSwitch` | US-029 |
| 导入管理 | `/admin/import` | 本地扫描 | `ImportButton`, `ProgressBar` | US-035 |

## 5. 关键页面线框（Wireframe — 文字版）

### 5.1 首页 `/`

```
┌────────────────────────────────────────────────────────────┐
│ Logo  SkillsMap           [搜索框]      [登录] [注册] [≡]  │  ← Header 64px
├────────────────────────────────────────────────────────────┤
│                                                            │
│         发现、决策、装上 — Claude Skills 一站式集市            │  ← Hero h1 32px
│         30+ Skills · 10 分类 · 持续更新                       │
│                                                            │
│              [ 搜索框 · 关键词 · 回车 ]                       │  ← Hero search
│                                                            │
├────────────────────────────────────────────────────────────┤
│ 精选 (6)                                    [查看全部 →]   │
│ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐    │
│ │Card 1│ │Card 2│ │Card 3│ │Card 4│ │Card 5│ │Card 6│    │
│ └──────┘ └──────┘ └──────┘ └──────┘ └──────┘ └──────┘    │
├────────────────────────────────────────────────────────────┤
│ 最新 (6)                                       [查看全部 →]│
│ ┌──────┐ ┌──────┐ ...                                     │
├────────────────────────────────────────────────────────────┤
│ 热门 (6)                                       [查看全部 →]│
│ ┌──────┐ ┌──────┐ ...                                     │
├────────────────────────────────────────────────────────────┤
│ 分类                                                    │
│ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐                        │
│ │🌐 Web│ │🧪 QA│ │🚀 Ops│ │📚 Docs│ │✨ Code│  ...           │
│ └────┘ └────┘ └────┘ └────┘ └────┘                        │
├────────────────────────────────────────────────────────────┤
│ Footer                                                    │
└────────────────────────────────────────────────────────────┘
```

### 5.2 浏览页 `/browse`

```
┌────────────────────────────────────────────────────────────┐
│ Header                                                     │
├──────────┬─────────────────────────────────────────────────┤
│ 过滤      │ 关键词: claude  [清空]  排序: [最新▾]            │
│           │ ─────────────────────────────────────────────  │
│ 分类      │ 找到 12 个 skill                                │
│ ☐ Web (8)│ ┌──────┐ ┌──────┐ ┌──────┐                      │
│ ☐ QA (3) │ │Card 1│ │Card 2│ │Card 3│                      │
│ ...      │ └──────┘ └──────┘ └──────┘                      │
│           │ ┌──────┐ ┌──────┐ ┌──────┐                      │
│ 标签      │ │Card 4│ │Card 5│ │Card 6│                      │
│ ☐ api    │ └──────┘ └──────┘ └──────┘                      │
│ ...      │        [上一页] 1 2 3 [下一页]                    │
└──────────┴─────────────────────────────────────────────────┘
```

> 移动端：过滤栏折叠为 Drawer，触发按钮在排序右侧。

### 5.3 详情页 `/skills/:id`

```
┌────────────────────────────────────────────────────────────┐
│ Header (含 breadcrumb: 首页 / browse / 当前)                │
├────────────────────────────────────────────────────────────┤
│ ←返回  Skill Name                            ⭐ 4.5 (12)   │
│         Display Name v1.0.0 · MIT · @author                │
│         [来源: official] [分类: Web] [标签: api,test]      │
│                                                            │
│  ┌────────────────────────┐  ┌─────────────────────────┐  │
│  │ 安装命令                │  │ 操作                    │  │
│  │ $ npm install skill-..  │  │ [⭐ 收藏] [📤 分享]      │  │
│  │ [📋 复制]               │  │                         │  │
│  └────────────────────────┘  └─────────────────────────┘  │
│                                                            │
│ ## 介绍                                                    │
│ description 文本 ...                                       │
│                                                            │
│ ## SKILL.md                                                 │
│ ┌──────────────────────────────────────────────────────┐  │
│ │ # Markdown 渲染（含目录 + 代码高亮）                   │  │
│ │ ```ts                                              │  │
│ │ // 代码块                                            │  │
│ │ ```                                                  │  │
│ └──────────────────────────────────────────────────────┘  │
│                                                            │
│ ## 评分 (12)  [写评价]                                       │
│ ┌──────────────────────────┐                                │
│ │ user-avatar  user-name   │ ⭐⭐⭐⭐⭐  2026-05-01  │  │
│ │ 评论内容 ...                │                                │
│ └──────────────────────────┘                                │
│ ...                                                         │
│ [加载更多]                                                  │
│                                                            │
│ ## 相关 Skill                                                │
│ ┌──────┐ ┌──────┐ ┌──────┐ ┌──────┐                        │
│ │Card 1│ │Card 2│ │Card 3│ │Card 4│                        │
│ └──────┘ └──────┘ └──────┘ └──────┘                        │
└────────────────────────────────────────────────────────────┘
```

### 5.4 后台 Layout

```
┌────────────────────────────────────────────────────────────┐
│ Admin Header  [Logo]  SkillsMap 后台        [admin ▾]      │
├──────────┬─────────────────────────────────────────────────┤
│ 200px    │                                                  │
│ 导航      │  <router-view>                                  │
│ ──────── │                                                  │
│ 📊 仪表盘 │                                                  │
│ 📦 Skill │                                                  │
│ 🏷 分类  │                                                  │
│ 🏷 标签  │                                                  │
│ 👥 用户  │                                                  │
│ 📥 导入  │                                                  │
│          │                                                  │
│          │                                                  │
└──────────┴─────────────────────────────────────────────────┘
```

## 6. 组件层级（Component Hierarchy）

```
App.vue
├── AppHeader.vue
│   ├── Logo
│   ├── NavSearch (快捷搜索)
│   ├── ThemeToggle
│   └── UserMenu (登录 / 头像下拉)
│
├── <router-view>
│   ├── HomeView
│   │   ├── HeroSearch
│   │   ├── SkillSection (featured)
│   │   ├── SkillSection (latest)
│   │   ├── SkillSection (hot)
│   │   └── CategoryGrid
│   │
│   ├── BrowseView
│   │   ├── FilterBar (mobile: drawer)
│   │   │   ├── CategoryFilter
│   │   │   ├── TagFilter
│   │   │   └── SortSelect
│   │   ├── SkillGrid
│   │   │   └── SkillCard × 12
│   │   └── Pagination
│   │
│   ├── SkillDetailView
│   │   ├── SkillHeader
│   │   ├── SkillMeta (来源/分类/标签)
│   │   ├── InstallCommandBox
│   │   ├── MarkdownView
│   │   ├── ReviewSection
│   │   │   ├── ReviewForm (登录后)
│   │   │   └── ReviewList
│   │   └── RelatedSkills
│   │
│   └── AdminLayout
│       ├── AdminSidebar
│       └── <router-view> (admin/* 子路由)
│
└── AppFooter
```

## 7. 错误与边界页面

| 场景 | 路由 | 行为 |
|---|---|---|
| 404 | `/404` | 文案 "页面不存在" + 跳首页按钮 |
| 403 | `/403` | 文案 "无权访问" + 联系 admin |
| 网络错误 | 任意 | Toast 提示 + 自动重试按钮 |
| Token 过期 | 任意 API | 跳 `/login?redirect=` + 提示 |
| 服务降级 | 详情页 | 显示骨架 + 重试按钮（≤ 3 次） |

## 8. URL 规范

- 全部小写 + kebab-case
- 详情用 `:id` (Long) 或 `:slug` (String)
- 过滤条件走 `?key=value`（可分享 / 收藏）
- 不暴露内部 ID 之外的私有信息

## 9. SEO 与可分享性

- 每页 `<title>` 含站点名 + 页名（如 "Skill 详情 - SkillsMap"）
- `<meta description>` 来自页面主内容
- 详情页支持 Open Graph（`og:title` `og:description` `og:image`）
- 404 状态码正确返回（前端路由用 history 模式需后端 fallback）

## 10. 修订记录

| 版本 | 日期 | 作者 | 摘要 |
|---|---|---|---|
| v0.1 | 2026-06-06 | designer-vicky | 初版 IA：导航树 + 22 条路由 + 4 个关键页面线框 + 组件层级 |
