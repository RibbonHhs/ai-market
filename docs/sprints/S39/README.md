# Sprint S39 — Skills Manager v2 优化

> **Sprint 编号**: S39（用户跳过 S38，S38 是上一轮上传功能 sprint）  
> **状态**: Phase 1 完成，等用户审批 plan  
> **核心交付**: 后端密码泄露修复 + 客户端 skills-manager skill v2.0 升级

## 背景

用户在上一轮上传 `api-and-interface-design` skill 过程中暴露 3 大问题 + 7 项摩擦：

### 问题

1. **后端密码泄露**（安全 bug）: `GET /api/auth/me` 返回的 `data.userInfo` 含 `password` 字段（bcrypt 哈希），用户贴响应时已泄露
2. **登录 UX 摩擦**: 用户必须浏览器登录 → F12 → 复制 `auth_token` → 贴回对话
3. **上传流程摩擦**: 7 项（Windows 无 zip / CATEGORY_ID 要查 / 错误码不可读 / 无 dry-run / ...）

### 决策记录

- **Q1 登录方式**: A — 缓存 token 到 `~/.skills-manager/.token` (0600)，支持 `--relogin`
- **Q2 密码修复范围**: A — 全局防御（DTO 化 + 实体层 @JsonProperty(WRITE_ONLY) 双保险）
- **Q3 Sprint 编号**: S39（跳过 S38）

## 文档清单

| 文档 | 内容 | 状态 |
|------|------|------|
| `README.md` | 本文件，sprint 索引 | done |
| `prd-skills-manager-v2.md` | US-1/US-2/US-3/US-4 详述 + AC | done |
| `codemap-backend.md` | 后端改动文件 + 行号定位 | done |
| `design-skills-manager-v2.md` | 客户端 skill v2 架构 + SKILL.md 草稿 | done |
| `qa-skills-manager-v2.md` | 冒烟用例（实施时写） | todo |
| `handoff.md` | 实施完结 + 端到端验证报告 | todo |

## User Stories 概览

| ID | 故事 | 优先级 | 改哪里 | 状态 |
|----|------|--------|--------|------|
| US-1 | 修密码泄露 bug | P0 安全 | 后端 User + AuthController + UserResponse DTO | 方案 ready |
| US-2 | 用户名密码登录 | P0 UX | 客户端 auth-login.sh + auth-common.sh | 方案 ready |
| US-3 | 上传流程 UX 打磨 | P1 | 客户端 publish-skill.sh 重写 | 方案 ready |
| US-4 | SKILL.md 升级 v2.0 | P1 | SKILL.md + api-endpoints.md | 方案 ready |

## 改动文件清单

### 后端（US-1，1 个 PR）

| 文件 | 改动 |
|------|------|
| `backend/.../entity/User.java` | password 字段加 `@JsonProperty(access = WRITE_ONLY)` |
| `backend/.../response/UserResponse.java` | **新增**，Java record 显式排除 password |
| `backend/.../rest/AuthController.java` | `me()` 和 `register()` 返回类型改 `UserResponse`；`buildAuthResp` 内 `userInfo` 同步替换 |

### 客户端（US-2/3/4，1 个 PR）

| 文件 | 改动 |
|------|------|
| `~/.claude/skills/skills-manager/SKILL.md` | v2.0 重写，新增 login/logout 子命令，删除 F12 指引 |
| `~/.claude/skills/skills-manager/references/api-endpoints.md` | § 6 Token 流程更新 |
| `~/.claude/skills/skills-manager/scripts/auth-common.sh` | **新增**，token 缓存 + 错误码映射 |
| `~/.claude/skills/skills-manager/scripts/auth-login.sh` | **新增**，read -s 登录 |
| `~/.claude/skills/skills-manager/scripts/publish-skill.sh` | **重写**，自动 zip + dry-run + 分类引导 |
| `~/.claude/skills/skills-manager/scripts/auth-example.sh` | 重写，调用 auth-common.sh |

**总 PR 数**: 2（1 后端 + 1 客户端 skill）

## 验证门槛

- 后端：`./mvnw -q clean compile` + 启动 dev + 跑 3 个 US-1 测试用例
- 客户端：跑 dry-run → 真传测试 skill → 验证公开可访问
- 不动 prod，dev 模式验证即可

## 不在 S39 范围

- 后端 `BizCode` 枚举统一上传错误码（未来 S40+）
- 前端 `front-vue3` 的 `UserStore`/`AuthStore` 适配（前端独立 sprint）
- admin 端用户管理（不同 DTO）
- SKILL.md 国际化

## 下一步

**等用户审批本 plan** → 通过则进入 Phase 3 实施（Dev 改后端 → Designer 改客户端 → QA 跑测试 → Ops 验证）→ Phase 4 交付
