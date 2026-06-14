# Sprint S38 — Skill Upload（用户上传本地 Skill）

> **Sprint**: S38
> **日期**: 2026-06-14
> **状态**: 启动中（Kickoff 完成，PRD 草稿 v1）
> **Lead**: agile-rd-lead
> **冲突说明**: 编号 S37 已被 admin-dark-theme 占用，本期顺延用 S38

---

## 🎯 一句话目标

让登录用户能把本地 `.skill` zip 直接上传到 SkillsMap 平台，**上传即发布**，无需审核。

## 📦 涉及产物

| 模块 | 产物 | 负责角色 |
|------|------|----------|
| 后端 | `POST /api/skills` (multipart, JWT) + `SkillUploadService` | Dev |
| 前端 | `AdminSkillsNewView.vue` (a-form + a-upload) | Dev |
| Skill | `skills-manager` 新增 `publish` 子命令 + `scripts/publish-skill.sh` | Dev |
| 文档 | PRD / Design / QA / Handoff 各 1 份 | PM / Designer / QA / Lead |
| 部署 | `application-prod.yml` 上传参数 + 监控告警 | Ops |

## ✅ 关键决策（已确认）

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 上传方式 | **仅 zip 包**（multipart/form-data） | 跨平台、易解析、易校验 |
| 鉴权 | **复用 JWT Bearer Token** | 零基础设施成本，与 /api/reviews 同源 |
| 审核 | **免审核，直接 PUBLIC** | MVP 优先效率，后续可加 status 状态机 |
| Sprint 编号 | **S38**（S37 已被占用） | 顺延避冲突 |

## 🚦 任务清单

| # | 任务 | 角色 | 状态 |
|---|------|------|------|
| 1 | 写 PRD + User Stories | PM | ✅ Done（prd.md 280 行 / 4 决策已固化） |
| 2 | 设计 Admin 上传页 UI | Designer | ✅ Done（design.md 380 行 / 2 ASCII 线框） |
| 3 | 后端 `POST /api/skills` (multipart) | Dev | ✅ Done（12 文件 / 9 单测全过 / BUILD SUCCESS） |
| 4 | 前端 `AdminSkillsNewView.vue` | Dev | ✅ Done（5 文件 / build SUCCESS / curl 联调 id=28） |
| 5 | skills-manager `publish` 子命令 | Dev | ✅ Done（3 文件 / bash -n OK / 干跑 RC=2） |
| 6 | QA 用例 + 端到端验证脚本 | QA | ✅ Done（qa.md 29 用例 / verify-upload.mjs 338 行） |
| 7 | 部署 / 监控 / 回滚预案 | Ops | ✅ Done（ops.md 601 行 / 5 项监控） |
| 8 | Sprint 收尾归档 | Lead | ✅ Done（handoff 收尾 / 9/9 任务 closed） |

## 📂 目录

```
docs/sprints/S38/
├── README.md          ← 本文件
├── prd.md             ← PRD v1（280 行，4 决策已固化）
├── tech-review.md     ← dev-kevin 技术评审（274 行）
├── design.md          ← designer-vicky 设计稿（380 行）
├── qa.md              ← qa-tina 测试矩阵（29 用例 / 147 行）
├── ops.md             ← ops-max 部署方案（601 行）
├── handoff.md         ← 任务交接 + 交付清单
├── screenshots/       ← 截图（待部署后补）
└── verify/            ← verify-upload.mjs（338 行 Playwright E2E）
```

## ⚠️ 风险与依赖

- **风险 1**：恶意 zip（zip slip、无限解压炸弹）— 必须做路径白名单 + 单文件大小限制
- **风险 2**：SKILL.md frontmatter 缺失/格式错 — 必须 fail-fast，错误信息可读
- **风险 3**：并发上传同 slug — 加 unique 约束 + 重命名策略
- **依赖**：AuthContext 已在 S23 完成（JWT jjwt 0.12.x），无新基础设施
- **复用**：项目已有 `AdminSkillUploadController` 提供 zip + frontmatter 解析能力（dev-kevin 2026-06-14 技术评审发现），新端点只需补「鉴权 + 入库」两段

## 🛠 技术决策（dev-kevin 技术评审，2026-06-14）

| 决策 | 选择 | 理由 |
|------|------|------|
| tagSlugs 自动创建 | ✅ 调 `TagService.createTag()` | 与 S05 搜索体验一致 |
| `uploader_user_id` 字段 | ✅ 新增（区别于 `created_by_user_id`） | 支持「作者 vs 上传者」统计与权益 |
| 孤儿目录清扫 | ✅ 后端 `@Scheduled(fixedRate=1h)` | 与 SkillSeedService 同包，零运维负担 |
| 总容量 | **6 工作日**（含 0.5d buffer） | dev-kevin 估时 19h（2.5d Dev + 1d QA + 1d PM/Design + 0.5d Ops + 0.5d Lead + 0.5d buffer） |

## 🔗 关联 Sprint

- S23 — 引入 JWT 鉴权基础设施（依赖）
- S32 — chip-row 公共组件（前端可复用）
- S36 — newbie-guide（用户首次上传时也会跳到此页，可考虑加 link）
