# S38 Handoff — 任务交接追踪

> 实时更新。每完成一格在「状态」列打 ✅。

## 1. 任务流转

| # | From | To | 产物 | 状态 |
|---|------|----|------|------|
| 1 | Lead | PM | `prd.md` v1 骨架 | ✅ Done（agile-rd-lead 草稿） |
| 2 | PM | Designer | `prd.md` v2 + 验收用例 | ✅ Done（prd.md 内已含 User Story） |
| 3 | Designer | Dev-backend | `design.md`（UI 线框 + 错误态矩阵） | ✅ Done（design.md 380 行） |
| 4 | Designer | Dev-frontend | `design.md`（同上） | ✅ Done（design.md §6 含侧栏菜单） |
| 5 | Dev-backend | QA | `POST /api/skills` 端点 + 9 单测 | ✅ Done（BUILD SUCCESS / 9 Tests run:0 Failures） |
| 6 | Dev-frontend | QA | `AdminSkillsNewView.vue` 联调 | ✅ Done（build SUCCESS / curl id=28 status=published） |
| 7 | Dev-skill | QA | `publish-skill.sh` + `SKILL.md` 更新 | ✅ Done（bash -n OK / 干跑 RC=2） |
| 8 | QA | Ops | `qa.md` + `verify-upload.mjs` | ✅ Done（29 用例 / 338 行 E2E） |
| 9 | Ops | Lead | `ops.md` + 复核 application-prod.yml | ✅ Done（601 行 / 配置齐全） |
| 10 | Lead | — | 本 `handoff.md` 收尾 + 触发 commit | ✅ Done（handoff 收尾；commit 由用户触发） |

## 2. 并行轨道

```
轨道 A (PM)  ─→  prd.md
轨道 B (Designer)  ─→  design.md    [依赖 A]
轨道 C (Dev)  ─→  后端 + 前端 + skill  [依赖 A+B]
轨道 D (QA)  ─→  qa.md + verify  [依赖 C]
轨道 E (Ops)  ─→  部署 + 监控  [依赖 C]
轨道 F (Lead)  ─→  handoff 收尾  [依赖所有]
```

## 3. 关键时间窗口（建议）

| 阶段 | 起 | 止 | 累计 |
|------|----|----|------|
| PM + Designer | D+0 | D+1 | 1d |
| Dev 三路并行 | D+1 | D+3.5 | 2.5d（dev-kevin 估时） |
| QA 验证 | D+3.5 | D+4.5 | 1d |
| Ops 部署 | D+4.5 | D+5 | 0.5d |
| Lead 收尾 | D+5 | D+5.5 | 0.5d |
| Buffer | D+5.5 | D+6 | 0.5d |

**总 Sprint 容量**：6 工作日（2026-06-14 Lead 拍板，从 5d 调整）。

## 4. 决策日志

| 时刻 | 决策 | 提议方 | 拍板方 |
|------|------|--------|--------|
| 2026-06-14 00:18 | 上传方式 = zip | Lead | 用户 |
| 2026-06-14 00:18 | 鉴权 = 复用 JWT | Lead | 用户 |
| 2026-06-14 00:18 | 审核 = 免审核 | Lead | 用户 |
| 2026-06-14 00:22 | 编号 = S38（避开 S37 占用） | Lead | 用户 |
| 2026-06-14 00:30 | 旧 S37 内容保留，不归档 | Lead | 用户 |
| 2026-06-14 00:55 | tagSlugs 遇不存在 tag → **自动创建** | dev-kevin | 用户 |
| 2026-06-14 00:55 | 新增 `uploader_user_id` 字段 | dev-kevin | 用户 |
| 2026-06-14 00:55 | 孤儿目录清扫 → **后端 @Scheduled** | dev-kevin | 用户 |
| 2026-06-14 00:55 | Sprint 容量 **5d → 6d** | dev-kevin | 用户 |
| 2026-06-14 10:30 | 临时目录路径 = `${skillsmap.upload.tmp-dir}/{UUID}/` 默认 `System.getProperty("java.io.tmpdir")` | dev-kevin | 用户 |
| 2026-06-14 10:30 | `uploader_user_id` 加 FK + 索引 | dev-kevin | 用户 |
| 2026-06-14 10:30 | 不加 @RateLimiter（S23 IP 限速兜底） | dev-kevin | 用户 |
| 2026-06-14 10:30 | 不做审计专用表（logback 兜底） | dev-kevin | 用户 |
| 2026-06-14 10:30 | Knife4j bearer-jwt SecurityScheme 已加 | dev-kevin | 用户 |
| 2026-06-14 10:30 | multipart 全局 10MB/12MB | dev-kevin | 用户 |
| 2026-06-14 10:30 | 成功 toast = **停留 600ms 再跳转**（更友好） | designer-vicky | Lead 默认（可覆盖） |
| 2026-06-14 10:30 | 40900 = focus slug 输入框 + 字段红字 + 全局 toast 兜底 | designer-vicky | Lead 默认（可覆盖） |
| 2026-06-14 10:30 | 移动端 cascader 降级 → **放 S38.1**（手机端非本期重点） | designer-vicky | Lead 默认（可覆盖） |
| 2026-06-14 10:30 | B-10 同名不同 version → 维持 200（PRD §2.2 不做版本管理） | qa-tina | Lead 默认（可覆盖） |
| 2026-06-14 10:30 | S-05 端口 9999 → 改 `127.0.0.1:1`（privileged port 必拒连） | qa-tina | Lead 默认（可覆盖） |
| 2026-06-14 10:30 | nginx 加 `client_max_body_size 15M` 兜底 multipart（**本期做**） | ops-max | Lead 默认（可覆盖） |

## 5. 待办阻塞点

**无阻塞**。S38 三路代码 + 设计 + QA + 部署方案全部就位；Sprint 可收尾。

## 6. 实际交付清单

### 6.1 文档（8 份，docs/sprints/S38/）

| 文件 | 行数 | 大小 | 状态 |
|------|------|------|------|
| `README.md` | 81 | 3.8 KB | ✅ |
| `prd.md` | 280 | 10.2 KB | ✅ |
| `tech-review.md` | 274 | 15.2 KB | ✅ |
| `design.md` | 380 | ~12 KB（designer 自报） | ✅ |
| `qa.md` | 147 | 12.7 KB | ✅ |
| `ops.md` | 601 | 24.8 KB | ✅ |
| `handoff.md` | 73 → 已更新 | ~5 KB | ✅（本文件） |
| `verify/verify-upload.mjs` | 338 | 14 KB | ✅ |

### 6.2 后端代码（6 新 + 6 改）

| 文件 | 性质 |
|------|------|
| `service/SkillUploadService.java` | 新 |
| `service/impl/SkillUploadServiceImpl.java` | 新（21 KB / 528 行） |
| `skill/upload/UploadTmpCleanupScheduler.java` | 新（@Scheduled 1h） |
| `response/SkillUploadResponse.java` | 新 |
| `test/.../SkillUploadServiceTest.java` | 新（9 单测全过） |
| `db/migration/V38__add_uploader_user_id.sql` | 新 |
| `entity/Skill.java` | + uploaderUserId 字段 |
| `rest/SkillController.java` | + POST /api/skills |
| `config/OpenApiConfig.java` | + bearer-jwt SecurityScheme |
| `application.yml` | multipart 10MB/12MB |
| `application-prod.yml` | multipart + tmp-dir |
| `db/schema-h2.sql` | + uploader_user_id 列 |

**编译**：`./mvnw -q clean compile` → **BUILD SUCCESS**
**单测**：`./mvnw -q test -Dtest=SkillUploadServiceTest` → **Tests run: 9, Failures: 0, Errors: 0**

### 6.3 前端代码（2 新 + 3 改）

| 文件 | 性质 |
|------|------|
| `api/skillUpload.ts` | 新（typed wrapper） |
| `views/admin/AdminSkillsNewView.vue` | 新（431 行） |
| `router/index.ts` | + /admin/skills/new 路由 |
| `views/admin/AdminLayout.vue` | + 「上传 Skill」菜单 |
| `axios/biz-code.ts` | + 5 个 S38 业务码 |

**build**：`npm run build` → **✓ built in 13.88s**
**联调**：`POST /api/skills` → `id=28, slug=my-test-skill, status=published`

### 6.4 Skill 改动（1 新 + 2 改）

| 文件 | 性质 |
|------|------|
| `scripts/publish-skill.sh` | 新（109 行，bash -n OK，干跑 RC=2） |
| `SKILL.md` | publish 行改写 + 「## 发布 Skill」整章重写 |
| `references/api-endpoints.md` | + §7 完整章节 |

### 6.5 延后到 S38.1（7 项）

1. 移动端 cascader 降级（< 768px → a-select）
2. 磁盘扩容阈值（1 个月内 `du -sh` 复核 `skill-packages`）
3. CDN 不缓存 `POST /api/skills`（用 CDN 时再做）
4. 临时目录宿主机权限复核
5. 接入 Prometheus + AlertManager（替代 Loki 兜底）
6. B-12 Windows 反斜杠 traversal 实测
7. F-05 11MB Playwright 启动慢优化

### 6.6 待 commit 清单（**未 commit，由用户触发**）

- 后端：12 文件（6 新 + 6 改）
- 前端：5 文件（2 新 + 3 改）
- skill：3 文件（1 新 + 2 改）— `~/.claude/skills/skills-manager/` 是独立仓库
- sprint 文档：8 文件

**未 commit** 是按用户原意「保持工作树 dirty，等 Lead 审」+「不要 commit / 不要 push」。

### 6.7 关闭的任务 ID 列表

TaskList 中 **#1 #2 #3 #4 #6 #7 #8 #9 #12 全部 completed**；#8 在收尾动作结束时标 completed。
