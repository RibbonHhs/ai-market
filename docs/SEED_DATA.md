# 种子数据来源

SkillsMap 在 dev 模式启动时会自动扫描本地 Skills 目录，将 frontmatter 解析后入库。

## 扫描路径

通过 `application.yml` 的 `skillsmap.seed.*` 配置：

```yaml
skillsmap:
  seed:
    enabled: true
    local-skills-path: C:/Users/86133/.claude/skills
    local-plugins-path: C:/Users/86133/.claude/plugins/marketplaces/claude-plugins-official/plugins
```

## 用户级 Skills

路径：`C:/Users/86133/.claude/skills/*/SKILL.md`

当前有 5 个：
- `find-skills` — 发现/安装 agent skills
- `mmx-cli` — MiniMax 多模态 CLI
- `skill-creator` — 创建 Skills
- `ui-ux-pro-max` — UI/UX 设计
- `web-video-presentation` — 视频化网页演示

## 官方插件市场 Skills

路径：`C:/Users/86133/.claude/plugins/marketplaces/claude-plugins-official/plugins/<plugin>/skills/<skill>/SKILL.md`

每个 plugin 下可能含多个 skill，遍历后大约可拿到 30+ 条。

## 解析字段映射

| 来源 | 字段 |
|---|---|
| frontmatter `name` | `skill.name` / `skill.slug` |
| frontmatter `description` | `skill.description` |
| SKILL.md 正文 | `skill.body` |
| frontmatter `license` | `skill.license` |
| frontmatter `allowed-tools` | `skill.allowed_tools` |
| frontmatter `compatibility` | `skill.compatibility` |
| manifest.json `version` | `skill.version` |
| manifest.json `homepage` | `skill.homepage` |
| plugin.json `author.name` | `skill.author_name` |
| plugin.json `author.email` | `skill.author_email` |
| 路径（plugin slug）| `skill.category_id`（按关键词映射） |

## 预置分类

启动时插入 10 个官方分类（沿用 find-skills 的分类表）：

| 名称 | Slug | Icon |
|---|---|---|
| Web Development | web | 🌐 |
| Testing | testing | 🧪 |
| DevOps | devops | 🚀 |
| Documentation | docs | 📚 |
| Code Quality | code-quality | ✨ |
| Design | design | 🎨 |
| Productivity | productivity | ⚡ |
| Database | database | 🗄️ |
| AI & Machine Learning | ai-ml | 🤖 |
| Data Processing | data | 📊 |

## 默认账号

| 用户名 | 密码 | 角色 |
|---|---|---|
| `admin` | `admin123` | ADMIN |
| `user` | `user123` | USER |

⚠️ **首次登录后请立即修改默认密码！**

## 关闭种子扫描

如果不想自动扫描（如生产环境），设置环境变量：

```bash
SKILLSMAP_SEED_ENABLED=false
```

或者在 `application-prod.yml` 中覆盖：

```yaml
skillsmap:
  seed:
    enabled: false
```
