# S35 Bug: `/categories/:slug` 跳转后浏览页未按 slug 过滤

**Sprint**: S35
**严重程度**: 中（核心导航流断裂，但数据本身正确）
**影响范围**: USAGE + SOC 两个维度（同样问题）

---

## 1. 复现步骤

1. 访问 `/categories`（USAGE 入口）
2. 点击任意分类卡片，例如「开发」(`slug=PURPOSE-DEV-开发`)
3. 跳转到 `/categories/PURPOSE-DEV-开发` (BrowseView)
4. **期望**：顶部 chip「开发」高亮，列表显示所有 `usage_category_id=开发对应 id` 的 skill
5. **实际**：浏览页没有按 slug 过滤，列表展示的是全部 skill（或上一个状态）

---

## 2. 根因

### 2.1 主要根因：路由参数变化未触发过滤更新

`BrowseView.vue` 的 slug 解析**只在 `onMounted` 里跑**：

```ts
onMounted(async () => {
  await loadCategories()
  ...
  if (route.params.slug) {
    const c = categories.value.find((cc) => cc.slug === route.params.slug)
    if (c) { ...query.usageCategoryId = c.id... }
  }
  reload()
})
```

但 `category-browse` 路由和 `browse` 路由都映射到**同一个** `BrowseView` 组件。当用户：

- 场景 A：从 `/browse` → `/categories/PURPOSE-DEV-开发`（**新挂载**，onMounted 触发，看似 OK）
- 场景 B：从 `/categories/A` → `/categories/B`（**组件复用**，onMounted **不**再触发，**B 的 slug 完全没被读**）

更关键的是 **Vue Router 默认行为下，**两个 `/categories/:slug` 之间切换**会复用组件**——这是问题的高发场景。

### 2.2 次要根因：slug → type 推断脆弱

即使 slug 解析触发，前端用 `find((cc) => cc.slug === route.params.slug)` 拿到 Category 后，再读 `c.type` 推断维度（USAGE / SOC）。这套机制：

- 当前种子数据 slug 命名空间确实分开（SOC `01-01-...`、USAGE `PURPOSE-...-...`），实际不撞
- 但 `OccupationCard.go()` **不传 type**，完全依赖 slug 反推，耦合且脆弱

### 2.3 BrowseView 顶部 chip 高亮逻辑未联动

`activeUsageTop`（line 213）只在用户**点 chip** 时更新。从路由进入时**不会**根据 `usageCategoryId` 反查一级 USAGE 并高亮——所以「顶部 chip 没亮」也是用户观感。

---

## 3. 影响范围

| 场景 | 表现 |
|------|------|
| `/categories` → 首次点 USAGE 卡片 | 看似 OK（onMounted 触发），但 chip 不高亮 + 没改写 dim 的初始值（其实是 OK 因为 onMounted 同步改） |
| `/categories/A` → `/categories/B` | **完全失效**（组件复用，slug 不重读） |
| `/occupations` → 点 SOC 卡片 | 同上，首次 OK；切换时失效 |
| `/browse` → `/categories/X` | OK（首次挂载） |
| `/` → `/categories/X` | OK（首次挂载） |

**结论**：USAGE + SOC 同样问题；首次跳转可用，**复用跳转**完全失效。

---

## 4. 修复方案

### 4.1 前端（核心修复）

**(1) BrowseView 增加 `watch(() => route.params.slug)`**

```ts
watch(
  () => [route.params.slug, route.query.type],
  async ([newSlug, newType]) => {
    if (newSlug) {
      await applySlugFilter(String(newSlug), newType as string | undefined)
    } else {
      // 清空过滤
      activeCategoryId.value = '0'
      query.categoryId = undefined
      query.usageCategoryId = undefined
      activeUsageTop.value = null
      reload()
    }
  }
)
```

并把 onMounted 里的 slug 处理逻辑抽出成 `applySlugFilter(slug, typeHint?)`：

- `typeHint === 'USAGE'` → 只匹配 type='USAGE' 的 Category
- `typeHint === 'SOC'` → 只匹配 type='SOC' 的 Category
- undefined → 按 slug 全局匹配（向后兼容直接访问 URL）

**(2) BrowseView 顶部 chip 联动**

当 `query.usageCategoryId` 设置后，反查一级 USAGE 节点并把 `activeUsageTop` 设为对应 id。

**(3) OccupationCard 跳转带 query**

```ts
function go(slug: string) {
  router.push({ name: 'category-browse', params: { slug }, query: { type: 'USAGE' /* 来自 props */ } })
}
```

`OccupationCard` 接收 `dim` prop（来自 CategoryView），决定 query.type = 'USAGE' 或 'SOC'。

### 4.2 后端

**不动**。`SkillServiceImpl.listSkills` 已正确支持 `usageCategoryId` 和 `categoryId` 过滤。

### 4.3 验收标准

- [ ] `cd backend && ./mvnw -q clean compile` 通过
- [ ] `cd frontend && npm run build` 通过
- [ ] 冒烟 1：点 USAGE 卡片 → URL `?type=USAGE` → 顶部 chip 高亮 + 列表只显示该 usage_category 的 skill
- [ ] 冒烟 2：USAGE 卡片 A → 卡片 B（同页面切换）→ 列表切换到 B 的 skill
- [ ] 冒烟 3：点 SOC 卡片 → URL `?type=SOC` → 侧栏 SOC 树高亮 + 列表只显示该 category 的 skill
- [ ] 冒烟 4：直接访问 `/categories/PURPOSE-DEV-开发`（无 type query）→ 仍能按 slug 推断 type

---

## 5. 不改的部分

- `UsageChip.vue` — 不动
- `SkillCard.vue` / `SkillDetailView.vue` — 不动
- `Skill` 实体结构 — 不动
- 后端 `SkillController` / `SkillServiceImpl` — 不动
