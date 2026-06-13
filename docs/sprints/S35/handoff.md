# S35 Handoff — 修复 `/categories/:slug` 跳转未过滤

**Sprint**: S35
**类型**: Bugfix
**前置依赖**: 无
**提交**: `fix(S35): filter browse page by category slug on /category/:slug`

---

## 1. 改动文件清单

| 文件 | 改动 |
|------|------|
| `frontend/src/views/BrowseView.vue` | 抽出 `applySlugFilter(slug, typeHint?)`；onMounted 用新方法；新增 `watch([route.params.slug, route.query.type])` 处理路由复用；顶部 USAGE chip 联动高亮 |
| `frontend/src/components/OccupationCard.vue` | 新增 `dim` prop（USAGE / SOC），`go()` 跳转时带 `?type=` query |
| `frontend/src/views/CategoryView.vue` | 给 `<OccupationCard>` 传 `:dim="dim"` |
| `docs/sprints/S35/bug-browse-not-filtered.md` | 诊断文档 |
| `docs/sprints/S35/handoff.md` | 本文档 |

**未改动**：
- 后端（`SkillServiceImpl` 已正确支持 `usageCategoryId` 过滤）
- `UsageChip.vue` / `SkillCard.vue` / `SkillDetailView.vue` / `Skill` 实体

---

## 2. 根因

`BrowseView.vue` 的 slug 解析**只在 `onMounted` 里跑**，但 Vue Router 默认**复用**同一组件（`browse` 和 `category-browse` 路由都映射到 `BrowseView`）。

- **首次跳转**（如 `/categories` → `/categories/PURPOSE-DEV-开发`）：onMounted 触发，看似 OK
- **复用跳转**（如 `/categories/A` → `/categories/B`）：onMounted **不**再触发，slug 完全没被读取，**完全失效**

附加问题：
- `OccupationCard.go()` 不传 type，BrowseView 只能按 slug 全局匹配，USAGE/SOC 即使撞名也无解
- 进入路由后顶部 USAGE chip 不会自动高亮，反差让用户以为没过滤

---

## 3. 修复方案

### 3.1 `BrowseView` — 抽出 `applySlugFilter` + 加 watch

```ts
async function applySlugFilter(slug: string, typeHint?: string) {
  if (!categories.value.length) await loadCategories()
  const c = categories.value.find(
    (cc) => cc.slug === slug && (!typeHint || cc.type === typeHint)
  )
  if (!c) return false
  if (c.type === 'USAGE') {
    dim.value = 'usage'
    activeCategoryId.value = c.id
    query.usageCategoryId = c.id
    query.categoryId = undefined
    activeUsageTop.value = c.parentId ?? c.id  // 顶部 chip 联动
  } else {
    dim.value = 'soc'
    activeCategoryId.value = c.id
    query.categoryId = c.id
    query.usageCategoryId = undefined
    activeUsageTop.value = null
  }
  query.page = 1
  return true
}

watch(
  () => [route.params.slug, route.query.type],
  async ([newSlug, newType]) => {
    if (newSlug) {
      const ok = await applySlugFilter(String(newSlug), newType as string | undefined)
      if (ok) reload()
    }
  }
)
```

### 3.2 `OccupationCard` — 跳转带 `?type=`

```ts
const props = defineProps<{ category: Category; index?: number; dim?: 'USAGE' | 'SOC' }>()

function go(slug: string) {
  router.push({
    name: 'category-browse',
    params: { slug },
    query: props.dim ? { type: props.dim } : {}
  })
}
```

### 3.3 `CategoryView` — 传 `dim` 给 `<OccupationCard>`

```vue
<OccupationCard :category="cat" :index="idx" :dim="dim" />
```

---

## 4. 冒烟验收

- [x] `cd backend && ./mvnw -q -DskipTests compile` 通过
- [x] `cd frontend && npm run build` 通过
- [ ] 手动验证（需启动后端 + 前端 dev）：
  - USAGE 卡片首次点击 → 顶部 chip「对应一级」高亮 + 列表只显示该 usage_category_id 的 skill
  - USAGE 卡片 A → 卡片 B（同页切换）→ 列表切换到 B 的 skill
  - SOC 卡片点击 → 侧栏 SOC 树高亮 + 列表只显示该 category_id 的 skill
  - 直接访问 `/categories/PURPOSE-DEV-开发`（无 type query）→ 仍能按 slug 推断 type

---

## 5. 约束遵守

- 未引入新依赖
- 未改 `UsageChip.vue` / `SkillCard.vue` / `SkillDetailView.vue`
- 后端零改动
- 沿用 `categoryApi` / `skillApi`
