# S25 根因分析 — 3 误命中 + 启发式精修方案

> **Sprint**: S25（A + D）
> **作者**: agile-rd-lead (PM 视角)
> **生成时间**: 2026-06-12
> **状态**: 已锁目标 code，待 Dev 落地

---

## 1. 背景

S24 完成 USAGE 维度端到端，启发式 `CategoryUtil.guessUsageCode(pluginSlug, name)` 跑通全量 seed。
S24 §7.4 实测发现 **3 个误命中**（pluginSlug 是 git 源场景，含 `git` 路径段）：

| # | skill slug | 当前 code | 期望 code | 根因 |
|---|------------|-----------|-----------|------|
| 1 | `claude-md-improver` | PURPOSE-DEVOPS-GIT | **PURPOSE-AI-LLM** | plugin slug 含 `git` 路径段 → 第 1 条规则撞中 |
| 2 | `web-video-presentation` | PURPOSE-DEVOPS-GIT | **PURPOSE-MEDIA-CONTENT** | plugin slug 含 `git` 路径段 → 第 1 条规则撞中 |
| 3 | `ui-ux-pro-max` | PURPOSE-DEVOPS-GIT | **PURPOSE-AI-LLM** | plugin slug 含 `git` 路径段 → 第 1 条规则撞中 |

**对照源码**（`backend/src/main/java/com/meiya/skillsmap/util/CategoryUtil.java` L28-L31）：

```java
String p = pluginSlug == null ? "" : pluginSlug.toLowerCase();
// Git 工作流
if (n.contains("git") || n.contains("commit") || n.contains("branch")
        || p.contains("git") || n.contains("pr") || n.contains("merge")) return "PURPOSE-DEVOPS-GIT";
```

`p.contains("git")` 太宽 —— 任何 git 源 plugin 都会被这一条拦下。

---

## 2. 误命中关键词拆解

| skill | name 关键词 | plugin slug 关键词 | 误中关键词 | 真正强信号 |
|-------|-------------|--------------------|------------|------------|
| `claude-md-improver` | `claude`, `md`, `improver` | 含 `git` | `p.contains("git")` | `claude` → AI-LLM；`-md` 后缀太弱 |
| `web-video-presentation` | `web`, `video`, `presentation` | 含 `git` | `p.contains("git")` | `video`/`presentation` → MEDIA-CONTENT |
| `ui-ux-pro-max` | `ui`, `ux`, `pro`, `max` | 含 `git` | `p.contains("git")` | `ui`/`ux` → AI-LLM（LLM 设计类，按 S24 wireframe） |

---

## 3. 精修方案

### 3.1 顺序调整（最关键）

**MANUAL_OVERRIDES 优先 > 精准关键词 > 通配兜底**

新 `guessUsageCode` 顺序：

1. **MANUAL_OVERRIDES 查表**（最高优先级，3 个硬编码）
2. **精准关键词**（按 name 强信号）
3. **plugin slug 强信号**（仅当 name 没撞中）
4. **通配兜底**（原顺序，仅在无信号时使用，**不依赖 plugin slug**）

### 3.2 MANUAL_OVERRIDES 草案

```java
private static final Map<String, String> MANUAL_OVERRIDES = Map.of(
    "claude-md-improver", "PURPOSE-AI-LLM",
    "web-video-presentation", "PURPOSE-MEDIA-CONTENT",
    "ui-ux-pro-max", "PURPOSE-AI-LLM"
);
```

**判定时机**：用 name slug 精确匹配（**不**用 plugin slug 完整路径，避免命中路径段）。

### 3.3 启发式精修

| 改动 | 原 | 新 |
|------|----|----|
| 1 | `p.contains("git")` 通配 | 删除 —— 改为：`n.contains("git-")`（带连字符才算 git 工具，如 `git-workflow`） |
| 2 | `n.contains("markdown")` 撞 DEVOPS/MEDIA-DOC 一起 | 拆为：name 含 `markdown` 且 plugin slug 含 `git` 才进 MEDIA-DOC；否则不进任何（避免单凭 `markdown` 误中） |
| 3 | `n.contains("refactor")` 撞 QASEC-QUALITY | 保留，但优先级降到"代码质量"分组末尾（避免吞 "ui-ux refactor" 这类） |

**核心原则**：启发式只承担"高置信度"推断，模糊 case 走 MANUAL_OVERRIDES + admin 手工。

### 3.4 backfill 端点增强

`AdminSkillController.backfillUsage` 新增 `override` 参数：

| 参数 | 行为 |
|------|------|
| `force=false`（默认）| 仅补 null，不动已有（保持 S24 行为）|
| `force=true`（S24 已有） | 强制按当前启发式重算（**会**覆盖已有）|
| `override=true`（S25 新增）| **仅**对 MANUAL_OVERRIDES 表中的 3 个 slug 强制覆盖，其余不动 |

幂等：`override=true` 调 2 次，第二次 0 更新。

---

## 4. 单测覆盖（3 case）

| 用例 | name | pluginSlug | 期望 code |
|------|------|------------|-----------|
| TC1 | `claude-md-improver` | `git-on-test` | `PURPOSE-AI-LLM` |
| TC2 | `web-video-presentation` | `git-on-test` | `PURPOSE-MEDIA-CONTENT` |
| TC3 | `ui-ux-pro-max` | `git-on-test` | `PURPOSE-AI-LLM` |

用 JUnit 5 + Spring Boot Test，断言 `CategoryUtil.guessUsageCode(pluginSlug, name)` 返回值。

---

## 5. 不在本 sprint

- **不**做通用 fuzzy match / ML 兜底（Q3 决策：启发式不解决 admin 手工边界）
- **不**改 `category` 表（3 个目标 code 已存在）
- **不**支持多 USAGE（Q2 决策：一对一）

---

## 6. 风险

| 风险 | 缓解 |
|------|------|
| MANUAL_OVERRIDES 写死，3 个名字换名就失效 | 留 TODO 注释：未来走 `skill.usage_override` 字段（参考 S26 候选） |
| 启发式顺序改动可能误中其他 skill | 单测只锁 3 case；其余用 S24 验证数据回归 |

---

## 7. 验收

- [ ] 3 个 skill 的 `usageCategory.code` 与本表 §1 期望列一致
- [ ] 调 `POST /api/admin/skills/backfill-usage?override=true` 第二次 0 更新
- [ ] `GET /api/categories?type=USAGE` 仍返 12 个一级（无 schema 变更）
- [ ] 3 个 JUnit case 全过
