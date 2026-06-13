# S33 收尾 — 修复用途分类 skillCount 显示

> **Sprint**: S33
> **类型**: Bug Fix（hotfix）
> **上游 Bug**: `docs/sprints/S32/bug-usage-count.md`
> **结果**: ✅ 全部交付，build 双绿，运行时冒烟通过
> **日期**: 2026-06-13

---

## 1. 用户诉求

> 在浏览页（或相关页面）显示用途分类（usage category）时，应该在该分类的 chip / 标签旁边显示属于该分类的 skill 数量，但目前没有正确显示这个数字。

---

## 2. 根因

### 2.1 缺陷 A — `UsageCategoryNodeVO` 缺字段
S24 引入的 USAGE 嵌套节点 DTO 没有 `skillCount` 字段；`SkillServiceImpl.toVO()` 构造节点时也没写入。

### 2.2 缺陷 B — `CategoryServiceImpl` 只按 `category_id` 聚合
`directSkillCountByCategory()` 仅过滤 `Skill::getCategoryId`，对 USAGE 维度（`usage_category_id`）完全无视，导致 `/api/categories?type=USAGE` 返回所有节点 `skillCount=0`。

### 2.3 缺陷 C — 前端 `UsageChip` 无 count 渲染
S32 设计/实现都没要求 chip 渲染 count，所以前后端都没有这条数据通路。

---

## 3. 改动清单

### 3.1 后端 (3 文件)

| 文件 | 变更摘要 |
|------|---------|
| `backend/src/main/java/com/meiya/skillsmap/response/UsageCategoryNodeVO.java` | 新增 `Integer skillCount` 字段 + getter/setter；构造器加第 9 参 `skillCount` |
| `backend/src/main/java/com/meiya/skillsmap/service/impl/SkillServiceImpl.java` | `toVO()` 构造 `UsageCategoryNodeVO` 时调 `baseMapper.selectCount(...)` 查 `usage_category_id=ucat.id AND status='published'`，写入 `node.setSkillCount(...)` |
| `backend/src/main/java/com/meiya/skillsmap/service/impl/CategoryServiceImpl.java` | `directSkillCountByCategory()` 改为同时按 `category_id` 与 `usage_category_id` 聚合到同一 map（merge 同 key 求和；两维度 id 互不重叠，merge 安全）|

### 3.2 前端 (4 文件)

| 文件 | 变更摘要 |
|------|---------|
| `frontend/src/types/skill.ts` | `UsageCategoryNode` 新增 `skillCount?: number` |
| `frontend/src/components/UsageChip.vue` | 新增 `skillCount?: number \| null` prop + `resolvedSkillCount` 计算属性（null 不渲染，负数视作 0）；模板条件渲染 `<span class="usage-chip__count">· N</span>`；aria-label 拼"（N 个 skill）"后缀；新 SCSS `.usage-chip__count` 用 opacity 0.75 + tabular-nums |
| `frontend/src/components/SkillCard.vue` | 透传 `:skill-count="skill.usageCategory?.skillCount"` 到 UsageChip |
| `frontend/src/views/SkillDetailView.vue` | 同 SkillCard |

### 3.3 文档 (3 文件)

| 文件 | 用途 |
|------|------|
| `docs/sprints/S32/bug-usage-count.md` | S32 期间的 Bug 报告（根因 + 影响范围 + 修复方案 + 验收标准）|
| `docs/sprints/S33/plan.md` | Sprint 计划（任务拆解 + DoD + 风险）|
| `docs/sprints/S33/handoff.md` | 本文件 |

---

## 4. 运行时冒烟

### 4.1 端点验证
- `GET /api/categories?type=USAGE` → 返回 `后端开发=15`, `LLM与AI=4`, `测试=1`, `工具=1` 等真实挂载数（修复前全 0）
- `GET /api/categories?type=SOC` → `计算机与数学类职业=20`, `计算机职业=19`（修复前一致，无回归）
- `GET /api/skills?page=1&size=1` → 首条 skill 的 `usageCategory.skillCount=1`，与 sidebar 该 USAGE 节点 count 一致

### 4.2 Build 验证
- `cd backend && ./mvnw -q clean compile` → EXIT 0
- `cd frontend && npm run build` → `✓ built in 25.15s`（EXIT 0）

### 4.3 浏览器冒烟（建议交付前手工跑）
1. 启 backend dev + frontend dev
2. 打开 `http://localhost:7777/browse` → 切"按用途"维 → 检查 sidebar 每个 USAGE 节点右侧的 count
3. 打开任一详情 → 检查 header chip 末尾是否有 `· N`（如 `· 1`）
4. a11y：Tab 到 chip → 屏幕阅读器应读出"用途分类：工具 生产力工具（1 个 skill）"

---

## 5. 验收对照

| AC | 描述 | 状态 |
|----|------|------|
| AC-1 | `GET /api/categories?type=USAGE` 返回非零 count | ✅ 后端开发=15, LLM与AI=4 |
| AC-2 | `SkillVO.usageCategory.skillCount` 在 list/detail 都存在 | ✅ 首条返回 `skillCount=1` |
| AC-3 | 卡片 / 详情 chip 显示 ` · N`（null 不渲染）| ✅ 模板 `v-if="resolvedSkillCount != null"` |
| AC-4 | chip `aria-label` 含"（N 个 skill）"后缀 | ✅ `countSuffix` 拼接 |
| AC-5 | `mvnw clean compile` 通过 | ✅ EXIT 0 |
| AC-6 | `npm run build` 通过 | ✅ 25.15s |
| AC-7 | `type=SOC` 与修复前一致 | ✅ 计算机职业=19 不变 |
| AC-8 | H2 dev 库冒烟 | ✅ curl 三端点全绿 |

---

## 6. 反模式自检

- ✅ 后端走 Service 层（非 Controller 直查）
- ✅ `@TableField` 显式映射（无新增字段未触碰）
- ✅ TS 类型完整（`skillCount?: number`，非 `any`）
- ✅ 不引入 JPA / Tomcat / 新依赖（仅 MyBatis-Plus baseMapper）
- ✅ CSS 沿用 token（无 raw hex 写入组件，count 用 opacity + currentColor 继承）
- ✅ `UsageChip` 的 `kind` / `variant` prop 契约未破坏（S32 兼容层保留）
- ✅ `usageCategorySlug` 平铺字段仍读（S18 向后兼容）

---

## 7. 遗留 / Open

- `directSkillCountByCategory()` 当前仍是全表扫描 skill + stream group。dev 数据量下足够，prod 数据破千后建议改 SQL `GROUP BY`（S34+ 优化）
- 后端 `refreshAllCategoryCount()`（admin 用）仍是 `selectCount per category` 的 N+1；不在本 Sprint 范围
- `UsageChip.count` 在 sm 尺寸（卡片）会显示 `· N`；如需在某些上下文不显示（例如 hover tooltip only），可后续加 `showCount?: boolean` prop

---

## 8. 关联

- 上游 Bug 报告：`docs/sprints/S32/bug-usage-count.md`
- Sprint 计划：`docs/sprints/S33/plan.md`
- 前序 Sprint：S32（chip 同 row + icon 区分）→ `docs/sprints/S32/handoff.md`
- 前序 Sprint：S31 → commit `7b74319 fix(S31): B1 search btn white text + L4 warning amber-700`

---

**Lead 签收**：可合并 `master`。S34+ 候选优化项见 §7。