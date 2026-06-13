# SkillsMap — Claude Skills 集市与管理平台

> 集浏览、搜索、评分、收藏、后台管理于一体的 Claude Skills 平台，对标 skillsmp.com

## ✨ 特性

- 🛒 **市场浏览**：首页推荐、列表筛选、关键词搜索、详情查看
- ⭐ **社区互动**：评分评论、收藏
- 🛠️ **管理后台**：Skill CRUD、上下架、分类标签管理、用户管理、本地 SKILL.md 一键导入
- 🔐 **鉴权**：JWT + BCrypt，ADMIN / USER 双角色
- 🚀 **零依赖启动**：H2 内存库，无需安装 MySQL 即可本地开发
- 📚 **API 文档**：集成 Knife4j，访问 `/doc.html`
- 🐳 **容器化**：提供 Docker Compose 一键启动 MySQL + 后端 + 前端

## 🏗️ 技术栈

| 层 | 选型 |
|---|---|
| 后端 | Spring Boot 3.5.7 + JDK 21 + MyBatis-Plus 3.5.12 + Druid + Undertow + Spring Security + JJWT + Knife4j |
| 前端 | Vite 7 + TypeScript 5.8 + Vue 3.5 + Pinia 3 + Ant Design Vue 4 + Vue Router 4 + Axios + markdown-it |
| 数据库 | H2 (dev) / MySQL 8.3 (local / prod) |
| 工具 | Lombok + Hutool + Guava + Fastjson2 + Jasypt + SnakeYAML |

## 📁 目录结构

```
skills-map/
├── backend/                  # Spring Boot 后端
├── frontend/                 # Vue 3 前端
├── docker-compose.yml        # MySQL + Backend + Frontend
├── docs/                     # PRD / API / ER / SEED_DATA
├── .claude/CLAUDE.md         # AI 助手团队规约（提交 git）
├── CLAUDE.md                 # 个人覆盖（gitignore，仅本地）
└── README.md
```

## 🤖 AI 助手规约（给协作者）

本项目使用 Claude Code 时，规约**已纳入版本控制**：

| 文件 | 用途 | 是否进 git |
|---|---|---|
| **`.claude/CLAUDE.md`** | 团队共用规约（Spring Boot 3.5.7 / Vite 7 / Ant Design Vue 4 等） | ✅ 提交，PR 评审 |
| `CLAUDE.md`（根） | 个人覆盖 / 临时指令 | ❌ gitignore，仅本地 |

> 🚫 **请勿在根目录 `CLAUDE.md` 写共用规则**——那是个人本地文件，**不会**推送给其他协作者。
> 要改团队规约请改 `.claude/CLAUDE.md` 并提 PR。

## 🚀 快速开始

### 方式一：本地开发（零依赖，推荐）

#### 1. 启动后端

```bash
cd backend
export JAVA_HOME="D:/sofaward/openjdk-21.0.2_windows-x64_bin/jdk-21.0.2"   # 你的 JDK 21 路径
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

启动后会自动：
- 创建 H2 内存库
- 扫描 `C:\Users\86133\.claude\skills\*\SKILL.md` 解析并入库（首次）
- 预置 10 个分类 + 默认账号 `admin/admin123` / `user/user123`
- 监听 `http://localhost:8767`

API 文档：<http://localhost:8767/doc.html>
H2 控制台：<http://localhost:8767/h2-console>（JDBC URL: `jdbc:h2:mem:skillsmap`，用户 `sa`，无密码）

#### 2. 启动前端

```bash
cd frontend
npm install
npm run dev
```

打开 <http://localhost:7777> 即可访问。

### 方式二：Docker Compose（生产模式）

```bash
docker compose --profile full up -d
```

会启动 MySQL 8.3 + 后端 + 前端（nginx），访问 <http://localhost:7777>。

## 📤 上传自构建 Skill

进入 `/admin/skills/new` 后：

1. **单个 SKILL.md** — 拖拽或点击"单个 .md"标签页上传，系统自动解析 frontmatter 预填表单
2. **.skill 包（zip）** — 切到".skill 包"标签页上传 zip，内含 `SKILL.md` + `scripts/` + `references/` + `assets/`，系统自动识别 4 类资源
3. 点击"应用到表单"→ 补充剩余字段 → 保存

上传的包文件存储在 `backend/data/skill-packages/{name}/`，可通过 `GET /api/admin/skills/{name}/download` 重新打包下载。

## 🔑 默认账号

| 用户名 | 密码 | 角色 |
|---|---|---|
| `admin` | `admin123` | ADMIN（可访问 /admin 后台） |
| `user` | `user123` | USER（普通用户） |

⚠️ **首次部署后请立即修改默认密码**

## 📖 文档

- [PRD 产品需求](docs/PRD.md)
- [API 接口文档](docs/API.md)（也可见运行时 `/doc.html`）
- [数据库 ER 图](docs/ER.md)
- [种子数据来源](docs/SEED_DATA.md)
- [AI 助手团队规约](.claude/CLAUDE.md)
- [Sprint Board（Sprint 0 全部制品入口）](SPRINT_BOARD.md)

## 🤖 团队工作流（Sprint 0 已建立）

> 5 角色 AI 团队 + Lead，标准 Scrum 节奏，所有 Sprint 0 规划制品在 [SPRINT_BOARD.md](SPRINT_BOARD.md)。

**团队组成**：

| 角色 | 代号 | 职责 |
|---|---|---|
| Lead | `agile-rd-lead` | 调度、跨角色协调、决策仲裁、文档一致性 |
| PM | `pm-alice` | PRD、User Story、Sprint 计划、Stakeholder 对接 |
| Designer | `designer-vicky` | 设计系统、IA、视觉 / 交互规范 |
| Dev | `dev-kevin` | 架构、代码、单元测试、技术债 |
| QA | `qa-tina` | 测试策略、DoD、自动化、缺陷跟踪 |
| Ops | `ops-max` | 环境、CI/CD、监控、应急响应 |

**Sprint 节奏**：2 周 / Sprint；详见 [Sprint 节奏日历](docs/sprint-0/03_sprint_calendar.md)。

**核心团队契约（3 条）**：

1. **可工作增量**：每份制品写完即视为"可被他人引用"，引用前 100% 自检
2. **一致性优先**：PM 提 Story 名 → Designer 提页面名 → Dev 提模块名时，先互相校准
3. **可见性**：任何角色遇到阻塞 30 分钟内 ping Lead

详细章程见 [团队工作协议](docs/sprint-0/02_team_working_agreement.md)。

## 🛠️ 常用命令

```bash
# 后端
cd backend
./mvnw clean package -DskipTests          # 打包
./mvnw spring-boot:run -Pdev              # 开发模式
java -jar target/skills-map-backend.jar   # 运行 jar

# 前端
cd frontend
npm run dev        # 开发
npm run build      # 生产构建（输出到 dist/）
npm run preview    # 预览构建结果
```

## 🌐 端口约定

| 服务 | 端口 |
|---|---|
| 前端 (Vite dev / nginx prod) | 7777 |
| 后端 (Spring Boot) | 8767 |
| MySQL | 3306 |
| H2 Console (dev) | 8767/h2-console |

## 📜 许可证

MIT
