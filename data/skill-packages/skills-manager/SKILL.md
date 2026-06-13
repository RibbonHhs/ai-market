---
name: skills-manager
description: 管理与更新 SkillsMap 平台上的公开 Skills。提供列表、搜索、详情、下载、同步等操作；引导用户如何发布新 Skill。基于 SkillsMap 公开 REST API。
license: MIT
allowed-tools: Bash, Read, Write
compatibility: claude-code, cursor, codex
metadata:
  version: 1.0.0
  author: SkillsMap Team
  homepage: https://github.com/RibbonHhs/skills
  tags:
    - skills
    - management
    - api
    - claude
---

# Skills Manager

一个面向 Claude Code 用户的 SkillsMap 平台客户端 skill。  
通过 SkillsMap 的公开 REST API，让你直接在对话中浏览、搜索、下载、同步 Skills。

## 可执行操作

| 操作 | 说明 |
|------|------|
| `list` | 列出已发布的 skills，支持关键字 / 分类 / 职业筛选 |
| `search` | 关键字模糊搜索（name / displayName / description） |
| `detail` | 按 slug 查 skill 详情（描述、安装命令、统计） |
| `download` | 下载 .skill zip 包（含 SKILL.md + 可选脚本与参考文档） |
| `sync` | 调用 `scripts/sync-skills.sh` 拉取全量已发布 skill 清单到本地 |
| `publish` | 引导用户走 SkillsMap Admin 后台上传 zip 或 git URL |

## 基础 URL

- 本地开发：`http://127.0.0.1:8767`
- 远程实例：见部署方提供的主机地址

所有公开端点以 `/api` 开头，详见 `references/api-endpoints.md`。

## 典型用法（当用户说「列出 SkillsMap 上的热门 skills」时）

1. 调用 `GET /api/skills/hot?limit=12` 获取热门列表
2. 把结果按 `displayName` / `description` 渲染给用户
3. 若用户要点开某个 skill，调用 `GET /api/skills/slug/{slug}` 拿详情
4. 若用户要安装，告诉用户可以：
   - 用 `npx skills add` 命令（见 `installCommand` 字段）
   - 或直接调 `GET /api/skills/slug/{slug}/download` 拿 zip

## 同步本地

```bash
# 默认 host = http://127.0.0.1:8767，可改
HOST=http://127.0.0.1:8767 bash scripts/sync-skills.sh
```

脚本会把全量已发布 skill（最多 200 条）按 `name,installs,version` 写入 stdout，保存为 CSV 风格。

## 注意事项

- **不要**把 token / API key 写在 skill 目录；SkillsMap 公开 API 当前无需鉴权
- 搜索时 `keyword` 区分大小写不敏感（后端已处理）
- 一级职业 code 形如 `#01`（SOC 23 一级），子 code 形如 `01-01`
- USAGE 维度 code 形如 `PURPOSE-DEV-BACKEND`

## 鉴权使用（S23 新增）

SkillsMap 写操作（评分 / 收藏）需要 Bearer Token。**本 skill 不会代用户登录**，请按以下流程让用户提供 token：

1. 用户先在 SkillsMap 网页登录（`/login`），从浏览器开发者工具 `Application → Local Storage` 找到 `auth_token`（或登录后调 `GET /api/auth/me` 返回值里的 `token`）。
2. 把 token 字符串贴给本 skill。
3. skill 用此 token 调用受保护端点。

完整示例脚本：`scripts/auth-example.sh`

```bash
# 1) 用户提供 token
export TOKEN="eyJhbGciOiJIUzI1NiJ9..."

# 2) 调用受保护端点（评分）
curl -fsS -X POST "http://127.0.0.1:8767/api/reviews" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"skillId":1,"rating":5,"comment":"很好用"}'

# 3) 收藏
curl -fsS -X POST "http://127.0.0.1:8767/api/favorites/1" \
  -H "Authorization: Bearer $TOKEN"
```

受保护端点列表见 `references/api-endpoints.md` § 6。

**安全提示**：token 视为用户凭据；不要写入 git commit、不要 echo 到日志、不要硬编码到 skill 脚本里。

## 发布新 Skill

本 skill 不直接提供 publish 接口。请用户前往 SkillsMap Admin 后台：
- 上传 zip：`/admin/skills/new` → 上传 .skill zip
- 从 Git URL：`/admin/skills/new` → Git URL 表单
- 本地目录：把 skill 放到 `~/.claude/skills/<name>/` 后重启后端
