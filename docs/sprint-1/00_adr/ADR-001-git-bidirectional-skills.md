# ADR-001: Git 双向 Skill 上传 + Agent Skills 规范

> 作者：agile-rd-lead @ Feature-1 Kickoff (2026-06-06)
>
> 版本：v1.0 · 状态：Proposed → Pending Review · 适用范围：SkillsMap Feature-1 全部实现
> 引用：`.claude/CLAUDE.md` / `docs/sprint-0/10_tech_architecture.md` / Anthropic Agent Skills 规范 / `SkillStorageService` + `GitSyncService`（已存在）

## 1. Context（背景）

### 1.1 用户原始诉求

> 我希望上传的skill可以通过GIT来管理，并且符合Agent Skills的目录结构，并且还支持上传Skills的方式是配置一个 git 地址，然后自动解析引用。如果是通过git地址上传的skill则不需要保存到程序自身配置的git地址，反之如果是通过上传 .skill 或者 .zip 的方式，则需要用程序配置的git来管理。

### 1.2 现状

- `StorageProperties.Git` 已有，但 `enabled=false`，且**只支持单一 program-level repo**
- `GitSyncService` 实现了 `commitAndPush(name, operation)`，复用 JGit 做 commit + push
- `SkillStorageService` 是唯一写入路径：所有上传（md / zip）都落到 `git-workdir` 或 `packages-path`
- `Skill` entity 有 `source`（String）字段但只存 `"official"` / `"community"` 等枚举值，不区分 zip / git
- 上传 UI 2 个 tab：单 .md / .zip，**没有 git URL 入口**

### 1.3 约束

- 不引入新框架（继续 JGit 6.x）
- 不破坏 Sprint 0 已修的 BUG-S0-R01/R02 行为
- 后端 Spring Boot 3.5.7 + JDK 21 + MyBatis-Plus 3.5.12 + Jasypt 加密
- 目录结构须符合 [Anthropic Agent Skills 规范](https://docs.claude.com/en/docs/agents-and-tools/agent-skills/overview)：`SKILL.md` 必选 + `scripts/` + `references/` + `assets/` 推荐

## 2. Decisions（4 个决策点）

### 2.1 决策点 #1 — Agent Skills 规范合规策略

**Decision**：**保留扩展**（agent/template/theme/script/reference/asset/other 7 类），但 `kind` 推断时按 Agent Skills 规范优先匹配（**scripts → reference → asset → 其他**），并在 `Skill.metadata` JSON 字段写入 `agent-skills-version: "1.0"` 标记。

**Context**：当前 `kindColor()` 已支持 7 类（`SkillUploader.vue`）；Anthropic 官方规范只 4 类（SKILL.md + scripts/ + references/ + assets/）。完全收敛会破坏向后兼容，已上传的 7+ 个真实 zip 包会丢数据；完全放任则与官方生态脱节。

**Consequences**：
- (+) 向后兼容：现有 zip 包（`frontend-patterns.zip` 等）不需重打包
- (+) 官方消费者（Claude Code）能识别"标准"目录
- (-) `kindColor` 仍要支持 7 类
- (-) frontmatter 解析需多一层规范化（Agent Skills 规范要求 frontmatter `name` + `description` 必填）

**Alternatives considered**：
- A：完全收敛到 4 类 → 拒绝：破坏 7+ 真实包
- B：双轨（保留 7 类 + 新增 `standard: true` 标记）→ 接受：即本决策

### 2.2 决策点 #2 — Git 双向策略

**Decision**：**双存储分层**：
- **ZIP 上传**（含 .skill / 单 .md）→ 走 `StorageProperties.Git`（program-level repo），commit + push
- **Git URL 上传** → 走独立 `git_source` 表（per-skill URL + ref + PAT），**只读**克隆到 `backend/data/git-sources/<sanitized-url>/`，**不**写入 program repo

**Context**：用户原话明确区分两种路径。若共用存储目录，program git 会把用户私有仓库内容推到 program 公共 repo，泄露风险。`skill` 表新增 `source_type ENUM('zip','git')` + `source_url` + `source_ref` 字段；`git_source` 表存凭据（PAT）和最后拉取元数据。

**Consequences**：
- (+) 物理隔离：program repo 不会污染用户 repo
- (+) 凭据粒度：每 skill 独立 PAT，便于按需吊销
- (-) 存储目录从 1 个变 2 个（`data/skill-packages/` vs `data/git-sources/`）
- (-) 需要"路径路由"在 Service 层根据 `source_type` 分流

**Alternatives considered**：
- A：单一存储 + `.gitignore` 排除 → 拒绝：本地还是会被 commit 候选扫描
- B：用 Git submodule → 拒绝：JGit 对 submodule 支持差，UX 复杂

### 2.3 决策点 #3 — Git 拉取触发 + 冲突策略

**Decision**：**三触发 + form 字段保护**：
- 触发：① 手动（admin 点"重新拉取"按钮）② 周期（Spring `@Scheduled`，默认 6h，可在 `application.yml` 配）③ Webhook（Sprint 2 评估）
- 冲突：git 源 skill 的 `body` 和 `assets/` 字段会被覆盖；**`form` 字段**（`description` / `displayName` / `categoryId` / `featured` / `status` / `tags` / `installCommand`）由 admin 在管理后台编辑过的版本**优先**，拉取时**不**覆盖

**Context**：用户对 git 源 skill 的核心诉求是"自动同步最新内容"，但**不希望**自己的元数据编辑被冲掉。需要 Service 层在 `applyPulledContent()` 时做"field-level diff"：用 `last_pulled_sha` 标记的 commit + 当前 DB 的 form 字段对比，git 内容只填 `body` / `version` / `metadata.git-sha` / 资源文件。

**Consequences**：
- (+) 元数据可独立于源码演进
- (-) 需要 form 字段编辑时间戳（`form_modified_at`）做判定
- (-) git 源 skill 在 admin 编辑 UI 上 form 字段需明显标记"本地编辑优先"

**Alternatives considered**：
- A：整条覆盖 → 拒绝：破坏用户编辑
- B：整条不动 → 拒绝：失去"自动同步"价值

### 2.4 决策点 #4 — 凭据存储

**Decision**：**Jasypt 加密**（`ENC(...)` 包裹），与 `application.yml` 一致；PAT 字段长度限制 256 字符；SSH key 不在 Sprint 1 范围。

**Context**：团队规约定：明文密码 / token 不进 git。Jasypt 3.x 已配，密钥走环境变量 `JASYPT_ENCRYPTOR_PASSWORD`。SSH key 需要挂载卷和 ssh-agent 集成，对 dev 体验是负担，先 public repo + HTTPS PAT 跑通。

**Consequences**：
- (+) 加密强度 = Jasypt 默认（PBEWithMD5AndDES，可换 AES）
- (-) DB 备份泄露时密钥被一并备份的风险 → 缓解：密钥不入库
- (-) Sprint 1 不支持 SSH → 私有 GitHub 仓库必须 PAT

**Alternatives considered**：
- A：明文存 DB → 拒绝：违反 `.claude/CLAUDE.md` §反模式
- B：Vault / KMS → 拒绝：v1 不引入额外基础设施

## 3. 实施总览（4 阶段 → Sprint 1 全量交付）

| 阶段 | 内容 | 角色 |
|---|---|---|
| 1. 数据模型 | `skill` 表加 `source_type` / `source_url` / `source_ref` / `form_modified_at` / `metadata` 字段 | dev-kevin |
| 2. Service 层 | 新增 `GitSourceService`（clone / pull / 冲突合并），`SkillStorageService` 分流 | dev-kevin |
| 3. Rest 层 | 新增 `AdminSkillGitSourceController`（POST 注册 / POST 拉取 / GET 状态） | dev-kevin |
| 4. UI + 调度 | `SkillUploader.vue` 加 3rd tab（Git URL），`@Scheduled` 周期任务 | designer-vicky / dev-kevin |
| 5. QA + Ops | 测试矩阵（10+ 用例）+ Git runbook（PAT 注入 / 磁盘配额） | qa-tina / ops-max |

## 4. Open Questions（执行中暴露的矛盾）

| # | 问题 | 状态 | 临时方案 |
|---|---|---|---|
| Q-01 | H2 内存库下 git 源 skill 重启后丢失（`data/git-sources/` 是本地） | 待 Ops 评估 | Sprint 1 期间 dev profile 重启后用"重新拉取"恢复 |
| Q-02 | `Skill.metadata` 当前是 String 存 JSON，`agent-skills-version` 写入是否要拆字段 | 待 PM 定 | Sprint 1 保持 String，写入格式 `{...,"agent-skills-version":"1.0"}` |
| Q-03 | 周期拉取的 `cron` 表达式：默认 `0 0 */6 * * ?`（每 6h）是否合理 | 待 Ops 评估 | 走配置项 `skillsmap.git-source.pull-cron` |
| Q-04 | git 源 skill 拉取失败（网络 / 鉴权）时是否阻塞 skill 在列表展示 | 待 PM 定 | 当前决策：标记 `status=error` 但仍展示（透明） |

## 5. 决策回退成本

若 Sprint 1 末用户验收发现：
- "我希望 git 源 skill 也能被 push 到 program repo" → 撤销决策 #2，恢复共用存储
- "我希望 Agent Skills 规范是 4 类硬约束" → 撤销决策 #1，写迁移脚本
- "我希望 form 字段也被覆盖" → 撤销决策 #3，去掉 form-modified 保护

每次回退预估工作量 ≤ 2 SP（Sprint 1 末评估）。

## 6. 引用

- Agent Skills 官方规范：<https://docs.claude.com/en/docs/agents-and-tools/agent-skills/overview>
- Anthropic Skills 标准目录示例：`SKILL.md` + `scripts/` + `references/` + `assets/`
- 现状代码：`StorageProperties.java:29-67`（Git inner class）/ `GitSyncService.java:55-329` / `SkillStorageService.java:46-...` / `Skill.java:13-181`

## 7. 修订记录

| 版本 | 日期 | 作者 | 摘要 |
|---|---|---|---|
| v1.0 | 2026-06-06 | agile-rd-lead | 初版 ADR：4 决策点（Agent Skills 合规 / Git 双向 / 拉取冲突 / 凭据）+ 实施总览 + Open Questions |
