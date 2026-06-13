# S24 审计：现有 7 个 skill 的 USAGE 归类现状

> **目的**：在写代码之前，把"当前数据"摸清楚。
> **方法**：基于 `CategoryUtil.guessUsageCode(pluginSlug, name)` 静态追踪 + SKILL.md frontmatter 描述 + 人工判定"应该归到哪"。
> **生成时间**：2026-06-12
> **Lead**：agile-rd-lead（self-audit，本文档兼做 pm-alice 审计输出）
> **后端 8767 / 前端 7777 / 启动方式见 S23 handoff §13**

---

## 1. 7 个 skill 的 frontmatter 速览

| # | skill name（slug） | description（frontmatter 关键句） | tags | 内置/上传 |
|---|-------------------|----------------------------------|------|----------|
| 1 | `demo-uploaded-skill` | A demo skill uploaded via multipart endpoint to verify the upload flow works end to end. | demo, upload-test | 用户上传 |
| 2 | `frontend-patterns` | Frontend development patterns for React, Next.js, state management, performance optimization, and UI best practices. | （无） | 内置 |
| 3 | `git-on-test` | Testing git push end-to-end | （无） | 用户上传（git 源） |
| 4 | `git-zip-test` | Test zip push to git | （无） | 用户上传（git 源） |
| 5 | `skills-manager` | 管理与更新 SkillsMap 平台上的公开 Skills。提供列表、搜索、详情、下载、同步等操作；引导用户如何发布新 Skill。基于 SkillsMap 公开 REST API。 | skills, management, api, claude | 内置 |
| 6 | `web-design-engineer` | Build polished visual web artifacts with HTML/CSS/JavaScript/React: pages, dashboards, prototypes, slide decks, animations, UI mockups, and data visualizations. Use when the user wants a browser-rendered, interactive, or presentational front-end deliverable. Not for back-end, CLI, or non-visual coding tasks. | （无） | 内置 |
| 7 | `zip-demo-skill` | A demo skill uploaded as .skill zip package with scripts/ references/ and assets/ folders. | （无） | 用户上传 |

---

## 2. `CategoryUtil.guessUsageCode` 静态追踪（命中顺序从上到下）

调用参数约定：`pluginSlug = name`（seed 流程里 `name = directory 名`，`pluginSlug` 在本地 seed 时为 null；本审计同时考虑 n 与 p = name 自身小写）。

### 2.1 `demo-uploaded-skill` → 兜底
- n = "demo-uploaded-skill"
- 依次检查：git（no）/ ci/cd（no）/ docker（no）/ monitor（no）/ cloud（no）/ frontend/ui/vue/react/css/html/web（no）/ python/java/node/go/rust/spring/api/backend（no）/ bash/shell/script（no）/ test/qa/review/simplify/verify（no）/ lint/refactor/format/code-quality/clean（no）/ security/scan/vuln（no）/ claude/gpt/llm/prompt/agent（no）/ ml/train/model/neural（no）/ analysis/analytic/chart/visual（no）/ data/etl/pipeline-data/kafka/spark（no）/ pdf/docx/pptx/xlsx/readme/doc/markdown（no）/ blog/write/content/article/post（no）/ design/art/canvas/theme/brand/ux（no）/ video/image/gif/audio/media（no）/ debug/troubleshoot/diagnos（no）/ productivity/task/note/todo/calendar（no）
- **命中**：**默认 `PURPOSE-DEV-BACKEND`**（后端开发）
- **判定**：❌ 不合理。demo 用途类目的"工具"或"测试"更合适（meta 性质，无业务场景）

### 2.2 `frontend-patterns` → `PURPOSE-DEV-FRONTEND`
- n = "frontend-patterns"
- 第 1 条 git / commit / branch / pr / merge → no
- ……
- **第 6 条 frontend**：n.contains("frontend") → ✅
- **命中**：`PURPOSE-DEV-FRONTEND`（前端开发）
- **判定**：✅ **合理**。"Frontend development patterns" 描述完全对应

### 2.3 `git-on-test` → `PURPOSE-DEVOPS-GIT`
- n = "git-on-test"
- **第 1 条 git**：n.contains("git") → ✅
- **命中**：`PURPOSE-DEVOPS-GIT`（Git 工作流）
- **判定**：✅ **合理**。虽然叫 "git-on-test"（测 git push），但功能归属是 Git 工作流

### 2.4 `git-zip-test` → `PURPOSE-DEVOPS-GIT`
- n = "git-zip-test"
- **第 1 条 git**：n.contains("git") → ✅
- **命中**：`PURPOSE-DEVOPS-GIT`（Git 工作流）
- **判定**：✅ **合理**。同上

### 2.5 `skills-manager` → 兜底
- n = "skills-manager"
- 第 1 条 git（no）/ ci/cd（no）/ docker（no）/ monitor（no）/ cloud（no）/ frontend/ui/vue/react/css/html/web（no，p 也不含 web）/ python/java/node/go/rust/spring/api/backend（**注意**：n 不含 "api"，但 p = "skills-manager" 也不含 "api"）→ no / bash/shell/script（no）/ test/qa/review/simplify/verify（no）/ lint/refactor/format/code-quality/clean（no）/ security/scan/vuln（no）/ claude/gpt/llm/prompt/agent（no，p 也不含 "claude"）→ no / ……
- **命中**：**默认 `PURPOSE-DEV-BACKEND`**
- **判定**：❌ **不合理**。skills-manager 是"管理 / 工具"类（平台客户端），但 USAGE taxonomy 中没有 "Management" 概念，**最近合理项**：
  - `PURPOSE-TOOL-PRODUCTIVITY`（生产力工具） — 描述说 "管理 Skills"
  - `PURPOSE-BIZ-PM`（项目管理） — 部分沾边
  - `PURPOSE-AI-LLM`（LLM 与 AI） — tags 有 "claude"，但 n/p 都不含关键词

  **但**：当前 USAGE taxonomy **没有** `PURPOSE-TOOL-MANAGEMENT` 或 `PURPOSE-BIZ-MANAGEMENT`。本审计**决策**：
  - 方案 A（推荐）：扩展启发式，让 "manager" / "management" 命中 `PURPOSE-TOOL-PRODUCTIVITY`
  - 方案 B：扩展 USAGE taxonomy（v1.1+，本 sprint 不动）

### 2.6 `web-design-engineer` → `PURPOSE-DEV-FRONTEND`
- n = "web-design-engineer"
- 第 1 条 git（no）/ ci/cd（no）/ docker（no）/ monitor（no）/ cloud（no）/ …
- **第 6 条 frontend / web**：n.contains("web") → ✅
- **命中**：`PURPOSE-DEV-FRONTEND`（前端开发）
- **判定**：⚠️ **不完全合理**。skill 描述说 "Build polished visual web artifacts with HTML/CSS/JS/React ... UI mockups, design systems"。明显是 **设计 + 前端** 双标签。
  - 可选项：
    - `PURPOSE-DEV-FRONTEND`（当前命中） — 代码角度
    - `PURPOSE-MEDIA-DESIGN`（设计） — 设计角度
    - **一者二选一**（v1 一对一）
  - **决策**：保留 `PURPOSE-DEV-FRONTEND`（其产出物是代码 HTML/CSS/JS，前端语义更准）。但前端展示可同时显示 SOC=设计 + USAGE=前端。

### 2.7 `zip-demo-skill` → 兜底
- n = "zip-demo-skill"
- 全部关键词不命中
- **命中**：**默认 `PURPOSE-DEV-BACKEND`**
- **判定**：❌ **不合理**。demo 性质，应为"工具"或"测试"

---

## 3. 归类前后对比表

| # | skill | **当前 USAGE（命中）** | 当前是否合理 | **建议 USAGE** | 调整原因 | 启发式需补的关键词 |
|---|-------|----------------------|------------|---------------|---------|------------------|
| 1 | demo-uploaded-skill | `PURPOSE-DEV-BACKEND`（兜底） | ❌ | `PURPOSE-QASEC-TESTING` | demo / upload-test → 测试 | `demo` → `PURPOSE-QASEC-TESTING` |
| 2 | frontend-patterns | `PURPOSE-DEV-FRONTEND` | ✅ | 同左 | 描述完全对应 | — |
| 3 | git-on-test | `PURPOSE-DEVOPS-GIT` | ✅ | 同左 | 名字含 git | — |
| 4 | git-zip-test | `PURPOSE-DEVOPS-GIT` | ✅ | 同左 | 名字含 git | — |
| 5 | skills-manager | `PURPOSE-DEV-BACKEND`（兜底） | ❌ | `PURPOSE-TOOL-PRODUCTIVITY` | tags: management/api/claude，是"管理"工具 | `manager` / `management` → `PURPOSE-TOOL-PRODUCTIVITY` |
| 6 | web-design-engineer | `PURPOSE-DEV-FRONTEND` | ⚠️ 可接受 | 同左 | 产出物是 HTML/CSS/JS 代码 | — |
| 7 | zip-demo-skill | `PURPOSE-DEV-BACKEND`（兜底） | ❌ | `PURPOSE-QASEC-TESTING` | demo → 测试 | （与 #1 共享） |

---

## 4. 总结

### 4.1 命中率

- **命中正确**：3 / 7 = **42.9%**（frontend-patterns / git-on-test / git-zip-test）
- **可接受**：1 / 7 = 14.3%（web-design-engineer）
- **需要调整**：3 / 7 = 42.9%（demo-uploaded-skill / skills-manager / zip-demo-skill）

### 4.2 启发式需补的 2 个规则

| 优先级 | 关键词 | 目标 code | 影响 skill |
|--------|--------|----------|-----------|
| P0 | `demo` | `PURPOSE-QASEC-TESTING` | demo-uploaded-skill, zip-demo-skill |
| P0 | `manager` / `management` | `PURPOSE-TOOL-PRODUCTIVITY` | skills-manager |

> **位置**：插入到 `CategoryUtil.guessUsageCode` 的**测试**分支之前（priority 5-QASEC-TESTING 之前），避免被 `test` 关键词先吃。

### 4.3 验证方式

1. 后端 `mvn spring-boot:run` 启动后，curl `GET /api/skills` 拉所有 skill
2. 对照本表"当前 USAGE"列
3. 应用启发式补丁后，调用 `POST /api/admin/skills/backfill-usage`（幂等，仅补 null）
4. 再次 curl 对比

### 4.4 不在本 sprint 范围

- "Management" USAGE 一级类目新增（v1.1+）
- USAGE 多对多（一 skill 多 USAGE）（v1.1+）
- 启发式基于 description 全文（v1 仍只用 name + pluginSlug）

---

## 5. 已知限制

- **slug 为 directory 名**，所以 `name` 与 `pluginSlug`（若传入）会重复。本审计只考虑 n。
- **description 不参与启发式**，是 v1 的限制；v1.1 可加 LLM 二次分类。
- **同名 slug 不同源**（git vs zip vs 本地）的差异，本审计统一用 directory 名分析。
