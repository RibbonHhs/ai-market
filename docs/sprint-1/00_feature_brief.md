# Feature-1 Brief: Git 双向 Skill 上传 + Agent Skills 规范

> 作者：agile-rd-lead @ Feature-1 Kickoff (2026-06-06)
>
> 版本：v1.0 · 范围：Sprint 1 主线 · 状态：Approved · 引用：ADR-001

## 1. 一句话目标

让 admin 能用 **3 种方式** 上传 Skill（.md / .zip / Git URL），并按来源自动选择存储策略（ZIP → program git 写；Git URL → 只读拉取），目录结构符合 [Agent Skills 规范](https://docs.claude.com/en/docs/agents-and-tools/agent-skills/overview)。

## 2. 用户故事（顶层 3 条）

### US-G1: Git URL 注册

> **As an** admin，**I want to** paste a Git URL and credentials into the upload form，**so that** the system auto-clones the repo and registers every Skill folder matching the Agent Skills spec.

**验收**：
- POST `/api/admin/skills/git-source` with `{url, ref?, credentials?}` → 同步 clone + 解析 + 入库
- 返回每个解析出的 skill 列表（name / version / status）
- 拉取失败 → 错误码 `50300` + 详细 message（auth failed / 404 / network）

### US-G2: ZIP 上传自动 commit 到 program git

> **As an** admin，**I want to** upload a .zip / .skill / .md file，**so that** the system stores it locally AND commits + pushes it to the configured program Git repo for backup/audit.

**验收**：
- 上传成功后 `git_status.ready=true` 时 → 触发 `gitSyncService.commitAndPush(name, "upload")`
- 失败 → 优雅降级到本地，`failureCount++` + 日志
- 详情页显示 `Git Sync: success @ 2026-06-06 10:30` 或 `failed: <reason>`

### US-G3: Git 源 skill 周期/手动拉取

> **As a** user，**I want to** see a "重新拉取" button on git-source skill detail page，**so that** I can manually refresh from the remote repo when the upstream changes.

**验收**：
- 列表 / 详情页有 `Source: Git (https://...)` 徽章（区别于 ZIP）
- 详情页有 "立即拉取" 按钮（admin 角色才显示）→ POST `/api/admin/skills/{id}/git-pull`
- 后端 `@Scheduled` 每 6h 周期拉取，状态写入 `last_pulled_at` / `last_commit_sha`
- 拉取后 `body` / `assets` 字段更新，`form` 字段保留本地编辑

## 3. 验收（DoD）

- [ ] `SkillUploader.vue` 3 个 tab（md / zip / git-url）工作
- [ ] ZIP 上传后 30s 内 `git_status` 显示 success
- [ ] Git URL 注册后 ≥ 1 个标准 Agent Skills 目录被识别入库
- [ ] PAT 错误时返回明确错误码，不抛 500
- [ ] 凭据不在日志 / API 响应 / DB 明文（grep `ghp_` 应零命中）
- [ ] 周期拉取任务可关闭（`skillsmap.git-source.scheduler-enabled=false`）
- [ ] 10+ 测试用例（见 `05_test_matrix.md`）全绿
- [ ] 磁盘配额监控有告警阈值

## 4. 范围外（Out of Scope）

- ❌ Webhook 触发（Sprint 2）
- ❌ SSH key 支持（v1.1+）
- ❌ Git LFS（大文件）
- ❌ Subversion / Mercurial
- ❌ in-browser git 客户端
- ❌ GitHub Actions / GitLab CI 集成（这是程序内嵌 Git 同步，不是 CI）

## 5. 优先级（MoSCoW）

| 类别 | 内容 |
|---|---|
| **Must** | US-G1, US-G2, US-G3 + 数据模型 + Service 分流 + UI 3rd tab |
| **Should** | 周期拉取 + 凭据加密 + 状态徽章 + 磁盘配额 |
| **Could** | 拉取失败时 admin 邮件通知（用 `@Async`） |
| **Won't** | Webhook / SSH / LFS / 多 git provider 适配 |

## 6. 依赖

- 上游：`.claude/CLAUDE.md`（栈约束）/ `10_tech_architecture.md`（模块图）/ `SkillStorageService`（已有 zip 路径）/ `GitSyncService`（已有 commit/push 能力）
- 平行：Dev / QA / Ops 同时启动各自的子任务

## 7. 风险摘要

完整见 `docs/sprint-0/09_risk_register.md`（Sprint 1 增 5 条 Feature-1 风险）：
- FR-01：PAT 泄露风险
- FR-02：大仓库磁盘爆
- FR-03：周期拉取阻塞
- FR-04：私有仓库鉴权失败
- FR-05：Git LFS 支持缺失

## 8. 修订记录

| 版本 | 日期 | 作者 | 摘要 |
|---|---|---|---|
| v1.0 | 2026-06-06 | agile-rd-lead | 初版 Feature Brief：3 用户故事 + DoD + 范围外 + MoSCoW |
