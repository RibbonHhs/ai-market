# S37 交接清单 — 暗色主题下后台管理可视性修复

> Sprint ID: S37
> 角色: agile-rd-lead
> 创建: 2026-06-13

---

## 1. 变更文件清单

### 1.1 修改 (frontend only)

| 文件 | 类型 | 变更要点 |
|------|------|----------|
| `frontend/src/views/admin/AdminLayout.vue` | 修改 | 移除硬编码 `#fff`/`#f5f7fa`/`#f0f0f0` → `var(--bg-primary)`/`var(--bg-secondary)`/`var(--border)`；保留 sidebar logo 白色；新增 `:deep(.ant-btn-text)` 与 `:deep(.ant-page-header-heading-title)` 暗色保底 |
| `frontend/src/views/admin/AdminSkillListView.vue` | 修改 | 7 处 inline `color: #999/#bbb` → `var(--text-tertiary)`；`.bulk-toolbar` 三色 → `var(--primary-bg/border/link)`；4 处 action 链接 → `var(--link/danger/warning)` |
| `frontend/src/views/admin/AdminDashboardView.vue` | 修改 | 4 处 `:value-style` color → `var(--success/danger/text-tertiary)`；4 处 stats 数组 color → `var(--link/primary/success/warning)` |
| `frontend/src/views/admin/AdminCategoryView.vue` | 修改 | 删除链接 `color: #ff4d4f` → `var(--danger)` |
| `frontend/src/views/admin/AdminUsageCategoryView.vue` | 修改 | 删除链接 `color: #ff4d4f` → `var(--danger)` |
| `frontend/src/views/admin/AdminTagView.vue` | 修改 | 删除链接 `color: #ff4d4f` → `var(--danger)` |
| `frontend/e2e/s37-admin-dark.spec.ts` | 新增 | 12 个 Playwright 截图测试 |

### 1.2 新增文档

| 文件 | 内容 |
|------|------|
| `docs/sprints/S37/prd-admin-dark-theme.md` | PRD：根因 + 用户故事 + 验收标准 + 非目标 |
| `docs/sprints/S37/design-admin-dark-theme.md` | 设计稿：CSS 变量审计 + Token 映射矩阵 + Dev/QA 移交清单 |
| `docs/sprints/S37/qa-admin-dark-theme.md` | QA 报告：12 截图清单 + Before/After 对比矩阵 + WCAG 核验 |
| `docs/sprints/S37/handoff.md` | 本文档 |
| `docs/sprints/S37/screenshots/*.png` | 12 张 after 截图（6 页面 × 浅/深） |

### 1.3 未变更

- 后端 / 数据库 / API：未触及
- 前台 `HomeView` / `BrowseSkillsView` / `SkillDetailView`：未触及
- `frontend/src/components/SkillLogo.vue`：未触及
- `frontend/public/logo.png`：未触及
- `frontend/src/stores/theme.ts`：未触及
- `frontend/src/App.vue`（ConfigProvider + darkAlgorithm）：未触及（已正确）
- `frontend/src/style/global.scss`（16 + 12 chip token）：未触及（已完备）

---

## 2. Before / After 截图对照

### 2.1 暗色态（核心修复）

| 页面 | Before (admin-dark-before/) | After (S37/screenshots/) |
|------|---------------------------|-------------------------|
| dashboard | `dashboard.png` — "Dashboard" 标题几乎不可见 | `dashboard-dark-after.png` — 全部清晰 |
| skills | `skills-list.png` — 表头+工具栏+标题全部褪色 | `skills-list-dark-after.png` — 全部清晰 |
| categories | `categories.png` — 标题不可见 | `categories-dark-after.png` — 清晰 |
| users | `users.png` — 同上 | `users-dark-after.png` — 清晰 |
| tags | `tags.png` — 同上 | `tags-dark-after.png` — 清晰 |
| categories-usage | (未截图) | `categories-usage-dark-after.png` — 清晰 |

### 2.2 浅色态（防破坏）

| 页面 | After 浅色 (S37/screenshots/) |
|------|------------------------------|
| dashboard | `dashboard-light-after.png` |
| skills | `skills-list-light-after.png` |
| categories | `categories-light-after.png` |
| users | `users-light-after.png` |
| tags | `tags-light-after.png` |
| categories-usage | `categories-usage-light-after.png` |

> 视觉与 `docs/sprints/logo-swap-admin.png` 浅色基线一致。

---

## 3. Build 结果

```
> vue-tsc --noEmit && vite build
✓ 3612 modules transformed.
✓ built in 22.67s
```

零 TS 错误，零 Vite 警告（除 chunk-size advisory）。

---

## 4. 测试结果

```
S37 admin dark — visual regression
  dark-dashboard, dark-skills-list, dark-categories, dark-categories-usage, dark-tags, dark-users
  light-dashboard, light-skills-list, light-categories, light-categories-usage, light-tags, light-users
12 passed (1.7m)
```

---

## 5. 风险与遗留

| 项 | 状态 |
|----|------|
| 浅色主题破坏 | 无 |
| 后端兼容 | 不涉及 |
| 主线 / 浏览页兼容 | 不涉及 |
| SkillLogo / LOGO 兼容 | 不涉及 |

> 零遗留风险。

---

## 6. 后续 Sprint 候选

1. S38: 把 admin 的 WCAG 实测引入 s30-style 自动报告（基于真实合成对比度）
2. S38+: 给 admin 域增加 prefers-reduced-motion 适配（批量操作 feedback 动画）
3. S39: 把"硬编码颜色 lint"加到 CI，禁止新增 `color: #xxx` 出现

---

## 7. 签发

- PM: agile-rd-lead (PRD §7)
- Designer: Vicky (design §7)
- Dev: Kevin (handoff §1.1)
- QA: Tina (qa §6 PASS)
- Ops: 不涉及
