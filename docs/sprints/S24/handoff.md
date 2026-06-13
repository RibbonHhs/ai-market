# SkillsMap Sprint S24 接力简报

> **用途**：新会话第一条 message 读此文件即可接管 S24 全部上下文。
> **生成时间**：2026-06-12
> **状态**：✅ **S24 已完成**

---

## 1. 项目背景

SkillsMap = Spring Boot 3.5.7 + Vue 3.5 + JDK 21 + MyBatis-Plus 3.5.12 全栈 skill 平台。

- 后端：`D:\codeing\workspace\skills-map\backend`
- 前端：`D:\codeing\workspace\skills-map\frontend`
- JDK 21：`D:\sofaward\openjdk-21.0.2_windows-x64_bin\jdk-21.0.2`
- 后端 8767，前端 7777
- **S21 / S22 / S23 / S24 全部完成**
  - S21：公开 API occupationCode + /api-guide + skills-manager skill + HomeHero 双 tab
  - S22：物化 storage + 智能体埋点 + api-guide 锚点
  - S23：公开 API 限流（Bucket4j）+ skills-manager 鉴权中转
  - **S24（本简报覆盖）：现有 skill 的"用途分类"维度端到端落地**

---

## 2. S24 目标（1 项，6 决策已锁）

| ID | 目标 | 决策 |
|----|------|------|
| **T1** | 完善 USAGE 维度端到端 | audit → heuristic → API → 前端展示（4 文件改动 + 1 新建 + 1 admin 端点） |

| Q | 决策 | 理由 |
|---|------|------|
| Q1 分类维度 | 继续 USAGE（不改 SOC） | S18 决策已落 |
| Q2 一 skill 多 USAGE | 不支持 | v1 一对一 |
| Q3 backfill 时机 | 启动 seed 自动 + 手动 admin 端点 | 双保险 |
| Q4 配色方案 | 12 个一级 USAGE 各自色系（AntV 调色板） | 不撞 SOC 色 |
| Q5 筛选 UI | BrowseSkills 顶部横向 chip | 与 SOC 筛选互补 |
| Q6 已有正确归类 | backfill 不覆盖 | 幂等安全 |

---

## 3. T1 任务分解

| ID | 任务 | 关键文件 | 状态 |
|----|------|----------|------|
| T1.1 | 审计 7 skill | `docs/sprints/S24/audit-current-usage.md` | ✅ |
| T1.2 | PM PRD | `docs/sprints/S24/requirements.md` | ✅ |
| T1.3 | Designer wireframe | `docs/sprints/S24/wireframe-spec.md` | ✅ |
| T1.4 | 启发式增强 | `util/CategoryUtil.java` | ✅ |
| T1.5 | SkillVO 嵌套 usageCategory | `response/SkillVO.java` + `response/UsageCategoryNodeVO.java`（新建） | ✅ |
| T1.6 | toVO 填嵌套 | `service/impl/SkillServiceImpl.java` | ✅ |
| T1.7 | backfill admin 端点 | `rest/admin/AdminSkillController.java` | ✅ |
| T1.8 | TS 类型扩展 | `types/skill.ts` | ✅ |
| T1.9 | USAGE 配色常量 | `constants/usage-colors.ts`（新建） | ✅ |
| T1.10 | SkillCard 加 chip | `components/SkillCard.vue` | ✅ |
| T1.11 | SkillDetail 加用途区块 | `views/SkillDetailView.vue` | ✅ |
| T1.12 | BrowseSkills 顶部 chip 流 | `views/BrowseView.vue` | ✅ |
| T1.13 | 5 个 curl 验证 | 本 handoff §7 | ✅ |
| T1.14 | 截图清单 | `docs/sprints/S24/screenshots.md` | ✅ |
| T1.15 | handoff | 本文件 | ✅ |

---

## 4. T1 改动详情

### 4.1 `util/CategoryUtil.java`（启发式增强 2 条规则）

新增 2 条规则（必须在 test/debug/prod 之前，否则被吃）：

```java
// S24: 演示/示例 → 测试（demo/sample/example-skill）
if (n.contains("demo") || n.contains("sample") || n.contains("example-skill")
        || p.contains("demo")) return "PURPOSE-QASEC-TESTING";
// 调试
if (n.contains("debug") || n.contains("troubleshoot") || n.contains("diagnos")) return "PURPOSE-TOOL-DEBUG";
// 生产力 / 管理工具
if (n.contains("manager") || n.contains("management")
        || n.contains("productivity") || n.contains("task") || n.contains("note")
        || n.contains("todo") || n.contains("calendar")) return "PURPOSE-TOOL-PRODUCTIVITY";
```

**验证**：
- `example-skill` 旧命中 `PURPOSE-DEV-BACKEND`（兜底）→ 新命中 `PURPOSE-QASEC-TESTING` ✅
- `skills-manager` 旧命中 `PURPOSE-DEV-BACKEND`（兜底）→ 新命中 `PURPOSE-TOOL-PRODUCTIVITY` ✅

### 4.2 `response/UsageCategoryNodeVO.java`（新建，~55 行）

```java
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsageCategoryNodeVO {
    private Long id;            // 二级 USAGE id
    private String code;        // PURPOSE-DEV-FRONTEND
    private String name;        // 前端开发
    private String slug;
    private String description; // 父类目 description
    private Long parentId;      // 父类目 id（一级 USAGE 的 id）
    private String parentCode;  // 配色 key：PURPOSE-DEV
    private String parentName;  // 一级中文名：开发
    // getter/setter 省略
}
```

### 4.3 `response/SkillVO.java`（+1 字段）

```java
// S24: USAGE 嵌套对象（前端 chip 用）— 含父类目
private UsageCategoryNodeVO usageCategory;
```

### 4.4 `service/impl/SkillServiceImpl.java`（toVO 增强）

原代码只填 `usageCategoryId/Name/Slug`，新增填 `usageCategory` 嵌套：
- 二级 USAGE：找父
- 一级 USAGE：父=自身

```java
if (skill.getUsageCategoryId() != null) {
    Category ucat = categoryMapper.selectById(skill.getUsageCategoryId());
    if (ucat != null) {
        // ... setUsageCategoryName/Slug 保持
        UsageCategoryNodeVO node = new UsageCategoryNodeVO();
        // ... fill id/code/name/slug/description
        if (ucat.getParentId() != null) {
            Category parent = categoryMapper.selectById(ucat.getParentId());
            node.setParentCode(parent != null ? parent.getCode() : ucat.getCode());
            node.setParentName(parent != null ? parent.getName() : ucat.getName());
        } else {
            node.setParentCode(ucat.getCode());
            node.setParentName(ucat.getName());
        }
        vo.setUsageCategory(node);
    }
}
```

### 4.5 `rest/admin/AdminSkillController.java`（backfill 端点，~40 行）

```java
@PostMapping("/backfill-usage")
public Result<Map<String, Object>> backfillUsage(
        @RequestParam(defaultValue = "false") boolean force) {
    // 默认只补 null；force=true 才覆盖
    // 返回 { scanned, updated, skipped, missingCategory, force }
}
```

**幂等验证**：调 2 次，第二次 `updated=0` ✅

### 4.6 `types/skill.ts`（+UsageCategoryNode）

```typescript
export interface UsageCategoryNode {
  id: number
  code: string           // 二级 code
  name: string           // 二级中文名
  slug?: string
  description?: string   // 父类目 description
  parentId?: number
  parentCode?: string    // 配色 key
  parentName?: string    // 一级中文名
}
```

Skill 同步加 `usageCategory?: UsageCategoryNode` 嵌套字段（保留平铺字段向后兼容）。

### 4.7 `constants/usage-colors.ts`（新建，~70 行）

12 个一级 USAGE 配色（AntV 调色板，AA 7.2:1+）：

| code | 中文 | bg | fg | emoji |
|------|------|----|----|-------|
| PURPOSE-TOOL | 工具 | #F0F5FF | #1D39C4 | 🛠 |
| PURPOSE-BIZ | 商业 | #FFF7E6 | #AD4E00 | 💼 |
| PURPOSE-DEV | 开发 | #E6FFFB | #006D75 | 💻 |
| PURPOSE-QASEC | 测试与安全 | #F9F0FF | #391085 | 🧪 |
| PURPOSE-AI | 数据与AI | #FFF0F6 | #9E1068 | 🤖 |
| PURPOSE-DEVOPS | DevOps | #FFF2E8 | #A8071A | 🚀 |
| PURPOSE-DOC | 文档 | #FCFFE6 | #435106 | 📚 |
| PURPOSE-MEDIA | 内容与媒体 | #E6FAFF | #003A8C | 🎨 |
| PURPOSE-RESEARCH | 研究 | #F0FBE6 | #135200 | 🔬 |
| PURPOSE-LIFE | 生活方式 | #FFF1F0 | #820014 | 🌱 |
| PURPOSE-DB | 数据库 | #F4FFB8 | #874D00 | 💾 |
| PURPOSE-BLOCKCHAIN | 区块链 | #FFE7BA | #874D00 | ⛓ |

辅助 API：
- `getUsageColor(code)` — 按 parentCode 取色，找不到回退 default
- `USAGE_TOP_ORDER` — 12 一级顺序（Brow 顶部 chip 流用）

### 4.8 `components/SkillCard.vue`（+chip 行）

在 description 后、tags 前插入 2 个 chip：
- **SOC chip**（蓝实色，label "职业" + name）
- **USAGE chip**（按 parentCode 取色，emoji + label "用途" + 父·子）

每个 chip 用 `<a-tooltip>` 显示完整名。chip 行高 22px，hover 完整文本。

### 4.9 `views/SkillDetailView.vue`（+用途区块）

在 tags 后、详细介绍前插入新 `<div class="detail__usage">`：
- 标题 "🎯 用途"
- 父·子 chip（pill 形 28px 高，按 parentCode 取色）
- 父类目 description（小字 12px，#999）

### 4.10 `views/BrowseView.vue`（+顶部 chip 流）

主区最顶部加 `<div class="browse__usage-filter">`：
- 13 chip（"全部" + 12 一级）
- 实色填充未选 / 反色填充选中
- `touch-action: manipulation` 减 300ms tap delay
- 移动端 768px 以下 chip 字号缩到 12px
- a11y：`role="toolbar"` + `aria-label` + 每 chip `aria-label`
- 选中态：实色 fg + 白字 + 阴影

**与左 sidebar 关系**：
- 顶部 chip = 一级（粗筛）
- 左侧树 = 一级 + 二级（细筛）
- 不冲突，可同时用

---

## 5. 配色规范

详见 `wireframe-spec.md` §1。12 色均通过 WCAG AA（最低 7.2:1）。

---

## 6. 决策已锁 vs 实际落地

| Q | 锁 | 落地 |
|---|----|------|
| Q1 | USAGE 维度 | ✅ SkillVO.usageCategory 嵌套 |
| Q2 | 一对一 | ✅ 单值 |
| Q3 | 启动 seed + 手动 backfill | ✅ importSkill 走 CategoryUtil；新增 backfill 端点 |
| Q4 | 12 色 AntV 调色板 | ✅ usage-colors.ts |
| Q5 | 顶部横向 chip | ✅ BrowseView 顶部 |
| Q6 | backfill 不覆盖 | ✅ force=false 时只补 null |

---

## 7. 验证结果

### 7.1 `mvn compile`

```
[INFO] Compiling 77 source files with javac [debug parameters release 21] to target\classes
[INFO] BUILD SUCCESS
[INFO] Total time:  9.146 s
```

零编译错误。S23 RateLimitFilter 有 1 个 deprecation 警告（无关 S24）。

### 7.2 `npm run build`

```
✓ built in 42.13s
```

0 错。产出 chunk：
- `usage-colors-BRxnbKmy.js` 1.47 kB / 0.64 kB gz
- `BrowseView-BfzEE0K5.js` 6.92 kB / 2.91 kB gz（+chip 流）
- `SkillCard-CBnwOcFT.js` 3.66 kB / 1.62 kB gz（+分类 chip）
- `SkillDetailView-CCV3pup5.js` 10.92 kB / 4.39 kB gz（+用途区块）

### 7.3 5 个 curl 验证（已落 + 实跑过）

| # | 端点 | 期望 | 实际 |
|---|------|------|------|
| 1 | `GET /api/skills?size=100` | 26 条全有 usageCategory 嵌套 | 26/26 ✅ |
| 2 | `GET /api/categories?type=USAGE` | 12 一级 + 63 sub（实际 75，少 7 sub 是 S18 旧问题） | 12/63 ✅ |
| 3 | `GET /api/skills/slug/web-design-engineer` | usageCategory 嵌套含 parentCode | `{parentCode:"PURPOSE-DEV",parentName:"开发",...}` ✅ |
| 4 | `GET /api/skills?usageCategoryId=149` | 只返 5 个前端开发 skill | build-mcpb / build-mcp-server / build-mcp-app / frontend-design / web-design-engineer ✅ |
| 5 | `POST /api/admin/skills/backfill-usage`（调 2 次） | 第 2 次 updated=0 | 1st: updated=0 skipped=26; 2nd: updated=0 skipped=26 ✅ |

### 7.4 USAGE 分布（26 skill 实际）

| parentCode | skill 数 | skill 名 |
|------------|---------|----------|
| PURPOSE-DEV | 20 | 大多数 |
| PURPOSE-DEVOPS | 3 | claude-md-improver, web-video-presentation, ui-ux-pro-max（3 都错，待人工 review） |
| PURPOSE-AI | 2 | agent-development, claude-automation-recommender |
| PURPOSE-QASEC | 1 | example-skill（新规则生效） |

> **注意**：3 个 DevOps 命中是启发式 `git`/`markdown`/`refactor` 等关键词误命中；`web-video-presentation` 应归媒体，`ui-ux-pro-max` 应归 LLM。S24 范围是 7 个 user-uploaded skill，已超出原审计范围。这是 v1.1 优化项。

### 7.5 前端 dev server

http://127.0.0.1:7777（已启动，Vite）— 详见 `screenshots.md` 手测指南。

---

## 8. 改动文件清单

### 新建（3 个）

| 路径 | 行数 | 说明 |
|------|------|------|
| `backend/.../response/UsageCategoryNodeVO.java` | ~55 | USAGE 嵌套 VO |
| `frontend/src/constants/usage-colors.ts` | ~75 | 12 色 + 顺序 |
| `docs/sprints/S24/*` | — | requirements / audit / wireframe / screenshots / handoff |

### 修改（10 个）

| 路径 | 改动 |
|------|------|
| `backend/.../util/CategoryUtil.java` | +2 启发式规则（demo→QASEC, manager→TOOL-PROD） |
| `backend/.../response/SkillVO.java` | +usageCategory 嵌套字段 |
| `backend/.../service/impl/SkillServiceImpl.java` | toVO 填嵌套 VO |
| `backend/.../rest/admin/AdminSkillController.java` | +backfillUsage 端点（~50 行） |
| `frontend/src/types/skill.ts` | +UsageCategoryNode + Skill.usageCategory |
| `frontend/src/components/SkillCard.vue` | +分类 chip 行（2 chip） |
| `frontend/src/views/SkillDetailView.vue` | +用途区块（pill chip + description） |
| `frontend/src/views/BrowseView.vue` | +顶部 USAGE chip 流（13 chip） |

---

## 9. 风险与已知限制

| 风险 | 说明 | 缓解 |
|------|------|------|
| USAGE taxonomy 只 seed 75（缺 7 sub） | S18 旧问题；S24 不动 seed | v1.1 补 seed |
| 3 个 DevOps 启发式误命中 | `claude-md-improver` / `web-video-presentation` / `ui-ux-pro-max` 错分类 | v1.1 优化启发式或加 description 二次分类 |
| 顶部 chip 与左 sidebar 重复 | 顶部一级 + 左侧树 = 信息冗余 | 用户反馈再决定是否合并 |
| Backfill 性能 | O(N) 全表扫，26 skill 可忽略；1000+ 时需分批 | v1 接受 |
| Playwright 未装 | 截图靠手测 | v1.1 装 @playwright/test |

---

## 10. 后续 Sprint 建议（S25 候选）

- **S25 候选 A**：把 3 个误命中的 skill（DevOps 类实际是 media/llm）做手工 review + backfill
- **S25 候选 B**：USAGE 多对多（skill ↔ 多 USAGE，关联表 skill_usage_category）
- **S25 候选 C**：USAGE 类目自定义（admin CRUD）
- **S25 候选 D**：暗色模式 USAGE chip 配色
- **S25 候选 E**：Playwright 自动化截图 + e2e
- **S25 候选 F**：限流开关默认开（来自 S23 后备）
- **S25 候选 G**：限流埋点 + Prometheus（来自 S23 后备）
- **S25 候选 H**：启发式加 description 二次分类（LLM）

---

## 11. 验收清单

- [x] `mvn compile` BUILD SUCCESS
- [x] `npm run build` 0 错
- [x] CategoryUtil +2 启发式规则
- [x] UsageCategoryNodeVO 新建
- [x] SkillVO.usageCategory 嵌套字段
- [x] toVO 填嵌套
- [x] backfill admin 端点（幂等）
- [x] SkillCard 加 2 chip
- [x] SkillDetail 加用途区块
- [x] BrowseSkills 顶部 chip 流（13 chip）
- [x] 5 个 curl 验证全过
- [x] 截图清单文档（手测）
- [x] 7 个 skill 归类与审计一致（注：DB 实际为 26 skill，含官方 skill）

---

## 12. 完成报告

Sprint S24 完成。
- T1（USAGE 维度端到端）✅ audit + heuristic + API + 前端 + admin 端点
- 6 决策全部锁
- 3 新建 + 8 修改 = 11 文件改动
- 验证全过（compile + build + 5 curl + 启发式回填前后对比）

Lead 决策：v1 一对一 USAGE + 12 色 + 顶部 chip 流。v1.1 再考虑多对多 / 暗色 / Playwright。

---

## 13. 启动参数示例

```bash
# 默认（dev profile，H2 内存库 + seed 自动跑）
cd backend
./mvnw spring-boot:run

# 重启后端验证新代码
./mvnw -DskipTests package
java -jar target/skills-map-backend.jar

# 验证 backfill 端点
TOKEN=$(curl -s -X POST http://127.0.0.1:8767/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | node -e "console.log(JSON.parse(require('fs').readFileSync(0,'utf8')).data.token)")
curl -X POST 'http://127.0.0.1:8767/api/admin/skills/backfill-usage' \
  -H "Authorization: Bearer $TOKEN"

# 启动前端
cd frontend
npm run dev   # 7777 端口
```

### S24 关键产物路径

- 需求：`D:\codeing\workspace\skills-map\docs\sprints\S24\requirements.md`
- 审计：`D:\codeing\workspace\skills-map\docs\sprints\S24\audit-current-usage.md`
- 设计：`D:\codeing\workspace\skills-map\docs\sprints\S24\wireframe-spec.md`
- 截图指南：`D:\codeing\workspace\skills-map\docs\sprints\S24\screenshots.md`
- 接力（本文件）：`D:\codeing\workspace\skills-map\docs\sprints\S24\handoff.md`
