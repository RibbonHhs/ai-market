# S33 Plan: 修复用途分类 skillCount 显示

> **Sprint**: S33
> **Goal**: 浏览页左 sidebar「按用途」维度的分类树 count + 卡片/详情 chip 旁的 skill 数都正确显示
> **Upstream**: `docs/sprints/S32/bug-usage-count.md`（Bug 报告 + 根因 + 修复方案）
> **Predecessor**: S32 已签收（`docs/sprints/S32/handoff.md`），本 Sprint 为 S32 收尾后的 hotfix
> **日期**: 2026-06-13
> **Owner**: agile-rd-lead 调度

---

## 1. Sprint Goal

> 让「用途分类（USAGE）」在两个入口都正确显示挂载的 published skill 数：
> 1. 浏览页 sidebar a-tree「按用途」维度的右侧 count（`cat-count`）
> 2. 卡片 / 详情页 chip 上的 skill 数（`UsageChip`）

---

## 2. 范围

### In Scope
- 后端 3 个 Java 文件改动（VO + Service + SkillServiceImpl）
- 前端 4 个 TS/Vue 文件改动（UsageChip + 类型 + SkillCard + SkillDetailView）
- `mvnw clean compile` + `npm run build` 双绿

### Out of Scope
- 浏览页顶部 chip 流（粗筛按钮）—— 无需 count
- 后台 admin 分类页 —— 自动复用 B3 修复
- Tag / Favorite count —— 与本 bug 无关
- SQL `GROUP BY` 优化（dev 数据量小，stream 已足够）

---

## 3. 任务拆解

| Task ID | 描述 | Owner | 依赖 | 估时 |
|---------|------|-------|------|------|
| T-S33-01 | 在 `UsageCategoryNodeVO` 加 `skillCount` 字段 + getter/setter | dev-kevin | - | 5 min |
| T-S33-02 | `SkillServiceImpl.toVO()` 构造节点时查并写入 skillCount（一次 SQL：`selectCount by usage_category_id`） | dev-kevin | T-S33-01 | 15 min |
| T-S33-03 | `CategoryServiceImpl.directSkillCountByCategory()` 同时按 `category_id` 与 `usage_category_id` 聚合（合并 map） | dev-kevin | - | 15 min |
| T-S33-04 | `mvnw clean compile` 通过 | dev-kevin | T-S33-01..03 | 2 min |
| T-S33-05 | `types/skill.ts` `UsageCategoryNode` 加 `skillCount?` | dev-kevin | - | 2 min |
| T-S33-06 | `UsageChip.vue` 加 `skillCount` prop + 模板渲染 ` · N` + aria-label 后缀 | dev-kevin | T-S33-05 | 10 min |
| T-S33-07 | `SkillCard.vue` 透传 `skill.usageCategory?.skillCount` | dev-kevin | T-S33-06 | 3 min |
| T-S33-08 | `SkillDetailView.vue` 同 T-S33-07 | dev-kevin | T-S33-06 | 3 min |
| T-S33-09 | `npm run build` 通过 | dev-kevin | T-S33-05..08 | 2 min |
| T-S33-10 | 冒烟：启 backend → curl `/api/categories?type=USAGE` 确认非零 count；启 frontend → 浏览 `/browse` 切「按用途」看右侧 count；开详情看 chip | qa-tina | T-S33-04, T-S33-09 | 10 min |
| T-S33-11 | 写 `handoff.md` + Conventional Commits `fix(S33): display skillCount per usage category` | agile-rd-lead | T-S33-10 | 5 min |

总估时 ≈ 70 min（含 build 等待）

---

## 4. 验收标准（DoD）

- [ ] AC-1：`GET /api/categories?type=USAGE` 每个节点 `skillCount ≥ 0` 且等真实挂载数
- [ ] AC-2：`SkillVO.usageCategory.skillCount` 在 list/detail 接口都返回
- [ ] AC-3：卡片 / 详情 chip 旁显示 ` · N`（null 时不显示）
- [ ] AC-4：chip `aria-label` 含"（N 个 skill）"后缀（不破坏 S32 a11y 契约）
- [ ] AC-5：`mvnw clean compile` 通过
- [ ] AC-6：`npm run build` 通过
- [ ] AC-7：`type=SOC` 返回与修复前**完全一致**（无回归）
- [ ] AC-8：h2 dev 库冒烟跑过

---

## 5. 反模式检查

- ❌ 不改 `controller` 包名 → 仍用 `rest/`
- ❌ 不在 Controller 直接写逻辑 → 走 Service 层
- ❌ 不用 Optional 字段类型 → 用 `Integer` + null
- ❌ 不引入 JPA / Tomcat / 新依赖
- ❌ 不写死 hex 色 → 沿用 `UsageColor` token
- ❌ 不破坏 `UsageChip` 的 `kind` / `variant` prop 契约（S32 兼容层不删）

---

## 6. 风险

| 风险 | 缓解 |
|------|------|
| B3 改 `directSkillCountByCategory` 可能让 SOC 聚合出现重复计数（同一 skill 同时挂两个相同 id）| 同 id 在两 map 中值相加会重复，但实际 schema 保证 `category_id` 与 `usage_category_id` **指向不同维度**，id 不会冲突（USAGE 用 `PURPOSE-*` code，SOC 用 `#01` code）|
| chip 末尾追加 `· N` 让长 chip 在 360px 视口溢出 | sm chip 已有 `padding: 1px 8px` 与 nowrap；人工 spot check 360×640 |
| 后端全表扫描 skill 在 prod 数据量增长后变慢 | 已知问题，留作 S34+ 优化（写一条 SQL `SELECT category_id, COUNT(*) FROM skill WHERE status='published' GROUP BY category_id` 同理 USAGE） |

---

## 7. 关联

- 上游 Bug：`docs/sprints/S32/bug-usage-count.md`
- 设计契约：`docs/sprints/S32/design-chip-row.md`（chip 视觉规范沿用）
- 组件契约：`docs/sprints/S32/prd-chip-row.md` §4.1 UsageChip 组件契约（本次新增 prop）

---

**Plan 结束。** dev-kevin 启动 T-S33-01。