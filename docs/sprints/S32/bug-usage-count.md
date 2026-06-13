# S32 Bug Report: 用途分类 skillCount 不显示

> **Sprint**: S32 → 修复落 S33
> **Bug ID**: BUG-S32-01
> **Severity**: Medium（信息缺失，非崩溃）
> **影响**: 浏览页左 sidebar「按用途」维度的分类节点右侧 skill 数量；详情/卡片 chip 上展示的"该分类下 N 个 skill"信息
> **状态**: 已诊断根因，S33 修复中

---

## 1. 复现步骤

1. `cd backend && ./mvnw spring-boot:run`
2. `cd frontend && npm run dev` → 打开 `http://localhost:7777/browse`
3. 切换左 sidebar 顶部「分类维度」radio 为 **"按用途"**
4. 观察左侧分类树（a-tree）每个节点右侧的 count

**预期**：每个 usage 节点（如"开发"/"前端开发"等）右侧显示 `skillCount`（如 3 / 12）。
**实际**：所有节点 count 显示 `0`。

5. 打开任一详情页 `/skills/<slug>`，查看头部 chip 上的"用途分类：xxx → yyy"
6. **预期**（按需求）：chip 旁或 tooltip 显示"该分类下 N 个 skill"。
**实际**：完全没有任何计数信息（chip 字段缺失）。

---

## 2. 根因分析

### 2.1 缺陷 A — `UsageCategoryNodeVO` 字段缺失（S24 遗留）

**文件**：`backend/src/main/java/com/meiya/skillsmap/response/UsageCategoryNodeVO.java`

S24 引入的嵌套 USAGE 节点 DTO 仅承载分类元信息（id/code/name/slug/parent），**没有 `skillCount` 字段**。S32 的 PRD §3 In Scope 也仅提到 chip 同行布局 + icon 区分，未补 count。

**同时**：`SkillServiceImpl.toVO()` 构造 `UsageCategoryNodeVO` 时**也没有**写 skillCount——

```java
// SkillServiceImpl.java:272-296
UsageCategoryNodeVO node = new UsageCategoryNodeVO();
node.setId(ucat.getId());
node.setCode(ucat.getCode());
node.setName(ucat.getName());
node.setSlug(ucat.getSlug());
node.setDescription(ucat.getDescription());
node.setParentId(ucat.getParentId());
// ← 这里缺一行：node.setSkillCount(...);
```

**前端症状**：所有用到 `skill.usageCategory` 的组件（`SkillCard.vue`、`SkillDetailView.vue`）拿不到 skill 数；`UsageChip.vue` 自身也没渲染该字段（设计契约里也没要求它显示）。

### 2.2 缺陷 B — `CategoryServiceImpl` 只按 `category_id` 聚合

**文件**：`backend/src/main/java/com/meiya/skillsmap/service/impl/CategoryServiceImpl.java:53-59`

```java
private Map<Long, Long> directSkillCountByCategory() {
    List<Skill> skills = skillMapper.selectList(new LambdaQueryWrapper<Skill>()
            .eq(Skill::getStatus, "published"));
    return skills.stream()
            .filter(s -> s.getCategoryId() != null)
            .collect(Collectors.groupingBy(Skill::getCategoryId, Collectors.counting()));
}
```

注意过滤条件 `.filter(s -> s.getCategoryId() != null)`——只用 SOC 维度 FK。

**Skill 实体**有两个 FK：
- `category_id` → SOC 维度（一级职业）
- `usage_category_id` → USAGE 维度（一级或二级用途）

**调用链**：
- `GET /api/categories?type=USAGE` → `categoryService.listAllWithCount("USAGE")`
- → 走 `aggregateCount()` 读 `directCount`，但 `directCount` key 是 `category_id`，**USAGE 节点的 id 不会出现在 map 里**
- → 每个 USAGE 节点返回 `skillCount=0`

`type=SOC` 时正常（因为 SOC skill 的 `category_id` 直挂）。

### 2.3 缺陷 C — `aggregateCount` 不区分 type

即便把 B 修了，USAGE 也是两级树（一级 USAGE + 二级 USAGE）。当前 `aggregateCount` 对 L1 节点 = 自身 + 二级子节点的 direct count 求和，**这个语义对 USAGE 同样适用**，所以无需改 `aggregateCount` 本身——只要数据源 `directCount` 注入正确即可。

---

## 3. 影响范围

| 入口 | 影响 | 现有 UI |
|------|------|---------|
| `BrowseView` sidebar a-tree（按用途维度） | 全部 USAGE 节点 count=0 | `<span class="cat-count">{{ skillCount \|\| 0 }}</span>`（line 41）|
| `BrowseView` sidebar 一级 SOC 节点 | 聚合正常（SOC L1 = L1 直挂 + L2 直挂） | 同上 |
| `SkillCard.vue` chip 旁 | 字段未透出 | `UsageChip` 未展示 |
| `SkillDetailView.vue` chip 旁 | 字段未透出 | 同上 |
| `OccupationCard.vue` | 仅 SOC 维度，已正常 | 跳过 |

---

## 4. 修复方案

### 4.1 后端 — 三处

| # | 文件 | 改动 |
|---|------|------|
| B1 | `response/UsageCategoryNodeVO.java` | 新增 `Integer skillCount` 字段 + getter/setter |
| B2 | `service/impl/SkillServiceImpl.java` `toVO()` | 构造 node 时查一次 `usage_category_id` 的 published skill 数，写入 `node.setSkillCount(...)`；二级 USAGE 直接用自身 id 查 |
| B3 | `service/impl/CategoryServiceImpl.java` `directSkillCountByCategory()` | 同时按 `category_id` 与 `usage_category_id` 聚合 → 拆成两个 map，**合并**后作为 `directCount`（同 id 在两 map 出现也只算一次——一个 skill 不会同时挂两个相同 id）|

> **注**：B3 实现上更稳健的做法是改 SQL 直接 `GROUP BY` 两列。当前 Java stream 方案已可工作（N 不大），故仅微调 stream；如未来 skill 数破千，再迁移到 SQL `GROUP BY`。

### 4.2 前端 — 两处

| # | 文件 | 改动 |
|---|------|------|
| F1 | `components/UsageChip.vue` | 新增 `skillCount?: number \| null` prop；模板中在 chip 末尾条件渲染 ` · N`（仅当 `skillCount != null`）；同步 aria-label 拼接"（N 个 skill）" |
| F2 | `types/skill.ts` `UsageCategoryNode` | 加 `skillCount?: number` |
| F3 | `components/SkillCard.vue` | 透传 `:skill-count="skill.usageCategory?.skillCount"` 到 UsageChip |
| F4 | `views/SkillDetailView.vue` | 同 F3 |

### 4.3 不在范围

- 浏览页顶部 USAGE chip 流（粗筛）—— 本就是按钮，不需要数（hover tooltip 可后续再加）
- 后台 admin 分类页 — 已用 `CategoryVO.skillCount`，走 B3 修复后自动正确
- Tag / Review / Favorite 的计数 — 与本 bug 无关

---

## 5. 验收

| AC | 描述 |
|----|------|
| AC-1 | `GET /api/categories?type=USAGE` 返回每个节点的 `skillCount` ≥ 0，等于"挂载到该 usage_category_id（包含二级 L2）的 published skill 数" |
| AC-2 | `SkillVO.usageCategory.skillCount` 字段在每个 skill 详情/列表中存在，且值 = 该 usage_category_id 直挂的 published skill 数 |
| AC-3 | 卡片 / 详情 chip 旁显示 ` · N`（N≥0 时显示，null/undefined 时不显示）|
| AC-4 | a11y：chip `aria-label` 包含"（N 个 skill）"后缀 |
| AC-5 | `mvnw clean compile` + `npm run build` 双绿 |
| AC-6 | 不影响 SOC 维度（`type=SOC` 返回值与修复前一致，差值 ≤ 1 噪声范围内）|

---

## 6. 风险

| 风险 | 缓解 |
|------|------|
| `directSkillCountByCategory` 当前是全表扫描 skill，扩到双 FK 后仍是单次扫描，不会变 N+1 | 监控 dev 日志 |
| USAGE L1 聚合：若二级挂的 skill 不止 `usage_category_id=L2`，`aggregateCount` 已正确加和，无需改 | 单测覆盖 |
| chip 末尾追加 `· N` 触发视觉 reflow | 用 flex gap，已自然撑开；sm chip 测试 360px 不溢出 |

---

## 7. Sprint 安排

- **S33**：`fix(S33): display skillCount per usage category (chip + sidebar)`
- 范围：后端 3 文件 + 前端 4 文件
- 文档：本文作为 bug 报告，`docs/sprints/S33/plan.md` 作为 Sprint 计划，`docs/sprints/S33/handoff.md` 作为交付总结