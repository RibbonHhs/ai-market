# Sprint S02 PRD: Git URL Skill Source（增量）

> 作者：pm-alice @ Sprint S02 Kickoff (2026-06-07)
>
> 上游：[`docs/sprint-1/00_feature_brief.md`](../sprint-1/00_feature_brief.md) Feature-1 US-G1/G3
> 下游：`docs/sprints/S02/design/*.md` · `docs/sprints/S02/test-cases.md`
> 决策基线：Q1=B / Q2=B / Q3=B / Q4=B / Q5=A（2026-06-07 用户确认）

## 1. 一句话目标

新增 **「通过 Git URL 上传」** 入口，与本地 .zip / .md 上传并列；后端用 JGit 浅克隆到独立缓存目录，扫描仓库内所有符合 Agent Skills 规范的子目录，每个 SKILL.md 拆成一条独立 Skill 记录。**Git URL 源 skill 不入程序自配的 Git 仓库**（区别于 zip 上传流程）；后台定时同步 + 手动同步走覆盖策略。

## 2. 与已有流程的边界

| 维度 | 本地 .md / .zip 上传（不变） | **Git URL 上传（本次新增）** |
|------|------|------|
| 落盘 | `data/skill-packages/{name}/` | `data/skill-clones/{name}/`（**独立目录**） |
| 同步主仓 | `GitSyncService.commitAndPush()` → 主仓 | **不入主仓**（仅本地 + 可选后台 git pull） |
| 凭据 | 无（无需） | URL 形式（无凭据 / Basic Auth / 自签证书跳过） |
| 触发方式 | 一次上传 | 一次性 clone + 后续可 pull |
| 入库元数据 | `source='community' \| 'imported' \| ...` | 新增 `source_type='GIT_URL'` |

## 3. 数据模型增量

### 3.1 `Skill` 实体新增 6 字段

| 字段 | 类型 | 长度 | 必填 | 说明 |
|------|------|------|------|------|
| `source_type` | VARCHAR | 16 | 否 | 枚举：`LOCAL_ZIP` / `LOCAL_FILE` / `GIT_URL`；默认 NULL 视作 `LOCAL_ZIP` 兼容历史数据 |
| `source_url` | VARCHAR | 500 | 否 | Git 仓库 URL（仅 GIT_URL 用）；私有仓库 URL 嵌 token 时会被解析时剥离 |
| `source_ref` | VARCHAR | 200 | 否 | branch / tag / commit SHA（仅 GIT_URL 用） |
| `source_token_enc` | VARCHAR | 1000 | 否 | **Jasypt 加密后**的 `username:token`；前端永不返回（`SkillVO` `@JsonIgnore`） |
| `token_hint` | VARCHAR | 32 | 否 | 脱敏提示，如 `ghp_xx****abcd`（仅前 4 + 末 4 字符） |
| `last_sync_at` | DATETIME | — | 否 | 最近一次成功 sync 时间（手动 / 定时） |

### 3.2 字段填充规则

- LOCAL_ZIP / LOCAL_FILE：`source_type` 显式置值，其余 5 字段 NULL
- GIT_URL：6 字段全部必填（`source_token_enc` 允许 NULL 表示公开仓库）

### 3.3 索引

```sql
CREATE INDEX idx_skill_source_type ON skill(source_type);
CREATE INDEX idx_skill_last_sync_at ON skill(last_sync_at);
```

> 用途：后台定时任务扫描 `source_type='GIT_URL' AND (last_sync_at IS NULL OR last_sync_at < NOW() - INTERVAL N MINUTE)`

## 4. REST API 增量

所有路径前缀 `/api/admin/skills`（ADMIN 角色）。

| Method | Path | 入参 | 出参 | 说明 |
|--------|------|------|------|------|
| POST | `/from-git` | `{url, ref?, branch?, token?, insecureSkipTls?}` | `{repoUrl, ref, discovered: SkillVO[], totalDiscovered, totalImported, totalSkipped}` | 一次性 clone + 解析 + 入库 |
| POST | `/{id}/sync` | — | `{id, lastSyncAt, lastCommitSha, changed: boolean}` | 单个 skill 强制重新拉取（覆盖策略） |
| GET | `/{id}/sync-status` | — | `{id, lastSyncAt, lastSyncStatus, lastSyncError, lastCommitSha, repoUrl, ref, tokenHint}` | 详情页拉取状态展示 |

### 4.1 `/from-git` 错误码

| 场景 | BizCode | HTTP | message 示例 |
|------|---------|------|--------------|
| URL 格式非法 | 40000 | 400 | `URL 格式非法: ...` |
| 404 / 仓库不存在 | 50301 | 502 | `仓库不存在: <url>` |
| 鉴权失败 | 50302 | 401 | `鉴权失败: 检查 username/token` |
| SSL 证书无效（且未开 insecure） | 50303 | 502 | `SSL 证书校验失败: ...` |
| 仓库内未找到任何 SKILL.md | 50304 | 422 | `未发现符合 Agent Skills 规范的子目录` |
| 浅克隆超时 | 50305 | 504 | `Clone 超时 (>30s)` |
| 磁盘配额不足 | 50306 | 507 | `磁盘空间不足, 需 >= 100MB` |

### 4.2 `/{id}/sync` 行为

1. 校验 `source_type = 'GIT_URL'`，否则 40000
2. 拉取 token，构造 JGit 命令
3. 浅克隆到 `data/skill-clones/{name}/`（如不存在；存在则 `fetch + reset --hard`）
4. 重新解析 SKILL.md → 更新 Skill 实体（**保留** `form` 字段中 `createdByUserId` / `featured` / `categoryId` / `tags` 等本地元数据）
5. 写 `last_sync_at = NOW()`
6. 返回 `{changed, lastCommitSha}`

> 「覆盖策略」Q5=A 落地：本地修改过的 SKILL.md / assets 会被远端覆盖（用户已被 UI 二次确认告知此风险）

## 5. 用户故事

### US-01（US-G1 增量）Git URL 上传

> **As an** admin，**I want to** paste a Git URL + optional branch/token into the upload form，**so that** the system clones + parses + auto-imports every subdirectory containing a valid SKILL.md.

**验收**：

- 公开 GitHub 仓库（如 `https://github.com/anthropics/skills`）无需 token，返回 200 + discovered 列表
- 私有 GitLab/Gitea 仓库 + token → 200
- 自签证书自建仓库 + 勾选"跳过 TLS 校验" → 200
- 仓库内同时含 `SKILL.md` 和 `sub/SKILL.md` → 入库 2 条 Skill（monorepo 自动拆分）
- URL 末尾带/不带 `.git` 都能识别
- 仓库 URL 中已嵌入 token（`https://user:token@host/...`）→ 自动剥离，仅存 ref + 脱敏 hint

### US-02（US-G3 增量）手动同步

> **As an** admin，**I want to** click "立即同步" on a Git-source skill detail page，**so that** I can refresh content from upstream after a release.

**验收**：

- 按钮触发前弹出**二次确认 Modal**（告知"将覆盖本地 skill 内容"）
- 同步中按钮显示 spinner，禁用
- 失败时 toast 显示后端 message（不暴露 token）
- 成功后刷新详情页，看到 `last_sync_at` 更新

### US-03 后台定时同步

> **As a** operator，**I want to** enable the scheduler so stale Git-source skills auto-refresh，**so that** the marketplace stays in sync without manual clicks.

**验收**：

- `@Scheduled` 每 60 min 扫描（`skillsmap.git-source.sync-interval-minutes`）
- 阈值 60 min（`skillsmap.git-source.stale-threshold-minutes`）
- 单次失败不影响其他 skill；连续 3 次失败的 skill 记录 `syncStatus='failed'` 并在控制台 WARN
- 关闭开关（`skillsmap.git-source.scheduler-enabled=false`）后定时任务不注册

### US-04 列表展示源类型

> **As a** user，**I want to** see a "来源" badge on each skill card，**so that** I can tell if a skill is local-zip or git-synced.

**验收**：

- LOCAL_ZIP → 灰色徽章"📦 本地"
- GIT_URL → 蓝色徽章"🔗 Git @ {ref}"（hover 显示完整 URL）
- LOCAL_FILE → 灰色徽章"📄 .md"
- 列表行可按 `source_type` 过滤

## 6. 边界 / 异常场景（必须测）

| 场景 | 期望 |
|------|------|
| URL 格式非法（无 scheme / 无 host） | 40000 + 中文错误信息 |
| 仓库不存在（404） | 50301 + message |
| 私有仓库无 token | 50302 |
| token 错误 | 50302，**不暴露** "Bad credentials" 全文（避免泄露用户名格式） |
| 自签证书且未勾选跳过 | 50303 |
| 仓库 > 500MB（depth=1 后仍超限） | 50306 + 提示用户用 sparse-checkout |
| 单文件 > 50MB（assets 下） | 50307 + 跳过该文件但继续 |
| submodule | 50308 + 提示"暂不支持 submodule" |
| 仓库内无任何 SKILL.md | 50304 + 列出已扫描的根目录 |
| 仓库内 SKILL.md 缺 `name` 字段 | 跳过该子目录 + 返回 skipped 列表 |
| 并发 sync 同一 skill | 互斥锁串行化（`SkillGitService` 内部 synchronized per id） |
| 定时任务运行中应用关闭 | Spring lifecycle 优雅停机（不杀正在进行的 clone） |
| Token 包含特殊字符 | URLEncoder 注入（不写入命令行） |

## 7. 验收标准（DoD）

- [ ] `SkillUploader.vue` 第 3 个 Tab「Git URL」可切换并提交
- [ ] 公开 GitHub 仓库 clone + 入库 30s 内完成（depth=1）
- [ ] 私有仓库 + 正确 token 入库成功
- [ ] 私有仓库 + 错误 token 返回 50302
- [ ] token 数据库中以 `ENC(` 开头，前端接口 grep `ghp_` / `glpat-` 零命中
- [ ] 列表卡片显示来源徽章 + hover 完整 URL
- [ ] 详情页"立即同步"按钮二次确认后触发
- [ ] `@Scheduled` 启动日志可见，定时日志每 60min 出现
- [ ] 关闭 `scheduler-enabled=false` 后定时日志消失
- [ ] 已有 zip 上传流程不回归（5 条回归用例全过）
- [ ] `/api/admin/skills/from-git` 在 Knife4j 文档可见
- [ ] 前端 `npm run build` 通过
- [ ] 后端 `./mvnw -q clean compile` 通过
- [ ] 单元测试覆盖率：SkillGitService ≥ 70%

## 8. 范围外（Out of Scope, Sprint S03+）

- ❌ Webhook 接收 push 事件
- ❌ SSH key 鉴权
- ❌ Git LFS（大文件）
- ❌ 多 git provider 自动识别（统一走 URL + token）
- ❌ Sparse checkout / 部分克隆
- ❌ Sync 失败邮件 / IM 通知
- ❌ Git-URL skill 的"评论 / 评分"是否影响主仓

## 9. 风险

| ID | 等级 | 描述 | 缓解 |
|----|------|------|------|
| R-01 | 高 | 私有仓库 token 误写日志 | JGit CredentialsProvider 不写日志；`@JsonIgnore` 字段；CI 加 `git secrets` 扫描 |
| R-02 | 中 | 浅克隆无法拉取历史 commit | 接受 trade-off；详情页只显示 `lastCommitSha`（来自 `HEAD`） |
| R-03 | 中 | 定时任务高并发打满网络 | `@Scheduled` 单线程池 + 全局限速（最多同时 3 个 clone） |
| R-04 | 中 | 磁盘被恶意大仓库撑爆 | clone 前检查 HTTP `Content-Length`（无则按 500MB 拒绝） + 50MB 单文件限制 |
| R-05 | 低 | Monorepo 拆分 N 条 skill 后 N 个 name 冲突 | 同 URL 同一 `path` 视为同一 skill，按 `{slug}:{path}` 复合主键 |
| R-06 | 低 | JGit SSL 跳过开关被滥用 | 仅 admin 角色可用；操作日志记入 `last_sync_error` |

## 10. 修订记录

| 版本 | 日期 | 作者 | 摘要 |
|---|---|---|---|
| v1.0 | 2026-06-07 | pm-alice | 初版：基于决策 Q1-Q5 落地 |
