# Dev Environment Setup（本地开发环境 0 → 1）

> 作者：ops-max @ Sprint 0 Kickoff (2026-06-06)
>
> 版本：v1.0 · 适用范围：所有协作者（Win 11 / macOS 14+） · 引用：`.claude/CLAUDE.md` §前后端栈 / `11_dev_onboarding.md` / `docs/SEED_DATA.md`

## 0. 概述

本文档给出 **从 0 到跑通 SkillsMap** 的完整步骤。**dev 模式默认 H2 内存库 + 种子扫描，无需装 MySQL**；如需 local 模式（连本地 MySQL）见 §6。

预计总时长：**Windows 11 = 30–45 分钟** / **macOS = 20–30 分钟**（含下载）。

## 1. 前置依赖清单

| 工具 | 版本 | 必需 | 验证命令 |
|---|---|---|---|
| **Git** | 2.40+ | 是 | `git --version` |
| **JDK** | 21.0.x (LTS) | 是 | `java -version` → 应为 `21.x` |
| **Maven** | 3.9.x（推荐用 `./mvnw`，可不装） | 否 | `mvn -v` |
| **Node.js** | 20.x LTS 或 22.x LTS | 是 | `node -v` |
| **npm** | 10.x+（随 Node） | 是 | `npm -v` |
| **Docker Desktop** | 4.x（**仅** local / prod 模式用） | 否 | `docker -v` |
| **IDE** | IDEA 2024.x 或 VS Code 1.85+ | 是 | — |
| **MySQL 8.3** | 8.3.x（**仅** local / prod 模式用） | 否 | `mysql --version` |

### 1.1 Windows 11 安装路径（推荐）

| 工具 | 安装方式 | 备注 |
|---|---|---|
| Git | <https://git-scm.com/download/win> | 装时选 "Git from the command line" |
| JDK 21 | <https://adoptium.net/temurin/releases/?version=21> | 选 `.msi`；记下安装路径（如 `D:\sofaward\openjdk-21.0.2_windows-x64_bin\jdk-21.0.2`） |
| Node 20 | <https://nodejs.org/dist/v20.18.0/node-v20.18.0-x64.msi> | 装时勾 "Add to PATH" |
| IDEA | <https://www.jetbrains.com/idea/download/> | Community 即可；Ultimate 自带 Database 工具 |
| VS Code | <https://code.visualstudio.com/Download> | 推荐 + `Extension Pack for Java` |
| Docker Desktop | <https://www.docker.com/products/docker-desktop/> | 仅 local / prod 用 |
| MySQL 8.3 | <https://dev.mysql.com/downloads/mysql/8.3.html> | 仅 local / prod 用 |

### 1.2 macOS 安装（Homebrew）

```bash
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

brew install --cask temurin@21
brew install --cask node@20
brew install --cask git
brew install --cask intellij-idea-ce  # 或 visual-studio-code
# 可选
brew install --cask docker
brew install --cask mysql-shell
```

### 1.3 环境变量（Windows）

```powershell
# 设置 JAVA_HOME（PowerShell 管理员）
[System.Environment]::SetEnvironmentVariable(
  "JAVA_HOME",
  "D:\sofaward\openjdk-21.0.2_windows-x64_bin\jdk-21.0.2",
  "User"
)
[System.Environment]::SetEnvironmentVariable(
  "Path",
  $env:Path + ";D:\sofaward\openjdk-21.0.2_windows-x64_bin\jdk-21.0.2\bin",
  "User"
)
```

重启终端后 `java -version` 验证。

## 2. 仓库克隆与目录

```bash
git clone <repo-url> skills-map
cd skills-map
ls
# 预期：backend  frontend  docs  docker-compose.yml  README.md  .claude  CLAUDE.md
```

> Windows 长路径支持（避免 `Filename too long` 错）：

```powershell
git config --system core.longpaths true
```

## 3. 后端启动（dev profile）

### 3.1 首次启动

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

> Windows 用 `.\mvnw.cmd`；Git Bash 优先用 `./mvnw`。

### 3.2 启动期检查清单（看到这些日志 = OK）

| 日志关键词 | 含义 |
|---|---|
| `Started SkillsMapApplication in X.XXX seconds` | 启动成功 |
| `HikariPool-1 - Start completed.` | H2 连接池就绪 |
| `SkillSeedService: scanned N skills` | 种子扫到 N 条 |
| `Tomcat started on port(s): 8767` | 实际是 Undertow，应是 `Undertow started on port(s): 8767` |
| `Knife4j start success` | API 文档就绪 |

> 第一次启动会下载 Spring Boot 依赖，约 2–5 分钟（看网速）。

### 3.3 启动失败的 5 大常见原因

| 现象 | 原因 | 解决 |
|---|---|---|
| `JAVA_HOME is not set` | 没装 JDK / 环境变量没生效 | 装 JDK 21 + 重启终端 |
| `Unsupported class file major version 65` | 用了 JDK 22+ | 装 JDK 21（项目锁定 21） |
| `Lombok annotation processor not found` | IDEA 没装 Lombok 插件 | 装插件 + 启用 annotation processor |
| `Port 8767 already in use` | 别的进程占 | `netstat -ano \| findstr 8767` → 杀 PID；或改 `application.yml: server.port` |
| `SkillSeedService path not found` | skills 路径不存在 | 编辑 `application-dev.yml` 改 `local-skills-path` |

## 4. 前端启动

### 4.1 首次启动

```bash
cd frontend
npm install
# 或用国内镜像
# npm config set registry https://registry.npmmirror.com
# npm install
npm run dev
```

### 4.2 启动期检查清单

| 日志关键词 | 含义 |
|---|---|
| `Vite v7.x.x ready in XXX ms` | Vite 启动 OK |
| `➜  Local:   http://localhost:7777/` | 监听 7777 |
| `➜  Network: ...` | 网络访问 URL |
| `ready in` 后无 ERROR | 无致命错误 |

### 4.3 启动失败的常见原因

| 现象 | 原因 | 解决 |
|---|---|---|
| `npm install` 卡死 | 网络 | 切 npmmirror 镜像 |
| `ERESOLVE unable to resolve dependency tree` | Node 版本 < 18 | 升级 Node 20+ |
| `Port 7777 in use` | 别的服务占 | `npm run dev -- --port 7778` |
| `connect ECONNREFUSED 127.0.0.1:8767` | 后端没起 | 先起后端 |
| `Cannot find module 'ant-design-vue'` | auto-import 没生效 | 删 `node_modules` 重装；确认 `vite.config.ts` 有 `unplugin-vue-components` 配 AntDV resolver |

## 5. 验证步骤（Definition of Achieved）

> 全绿即"环境就绪"，可进入开发。

### 5.1 后端验证

```bash
# 1. 根路径（应 404 或 200，看是否有根 Controller）
curl -i http://localhost:8767/

# 2. Skill 列表（公开）
curl -s 'http://localhost:8767/api/skills?page=1&size=5' | jq .
# 预期：{"code":0,"message":"ok","data":{"records":[...],"total":N,"page":1,"size":5}}

# 3. 分类列表
curl -s http://localhost:8767/api/categories | jq '.data | length'
# 预期：10（10 个预置分类）

# 4. 登录（admin 默认）
curl -s -X POST http://localhost:8767/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq .data.token
# 预期：返回 JWT 字符串

# 5. 鉴权接口（拿上一步 token）
TOKEN="<上一步的 token>"
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8767/api/auth/me | jq .

# 6. Admin 接口（需 ADMIN 角色）
curl -s -H "Authorization: Bearer $TOKEN" \
  http://localhost:8767/api/admin/dashboard/source-stats | jq .
```

### 5.2 前端验证

浏览器打开：

| URL | 预期 |
|---|---|
| <http://localhost:7777/> | 首页：hero + 精选/最新/热门 + 分类 |
| <http://localhost:7777/browse> | 列表 + 过滤 + 分页 |
| <http://localhost:7777/login> | 登录表单 |
| <http://localhost:7777/admin> | 跳 `/login?redirect=/admin`（未登录） |

### 5.3 文档与 H2 控制台

| URL | 预期 |
|---|---|
| <http://localhost:8767/doc.html> | Knife4j API 文档 |
| <http://localhost:8767/h2-console> | H2 登录页（JDBC: `jdbc:h2:mem:skillsmap`，user: `sa`，password: 空） |

### 5.4 冒烟用例（PRD §6，3 条最低）

| # | 步骤 | 预期 |
|---|---|---|
| 1 | 打开 <http://localhost:7777/> | 看到 hero + 精选/最新/热门三组 |
| 2 | `/browse` 搜 "claude" | 返回相关 skill 列表 |
| 3 | 点任意 skill 进详情 | Markdown 渲染 + 评分列表 |

## 6. local 模式（连本地 MySQL，可选）

> dev 模式 H2 即可；local 模式用于"接近生产"测试。

### 6.1 启动 MySQL（Docker）

```bash
docker run -d \
  --name skillsmap-mysql \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -e MYSQL_DATABASE=skillsmap \
  -e MYSQL_USER=skillsmap \
  -e MYSQL_PASSWORD=skillsmap_dev_pwd \
  -p 3306:3306 \
  mysql:8.3
```

### 6.2 改 application-local.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/skillsmap?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4
    username: skillsmap
    password: ENC(<jasypt 加密后的密码>)
  jpa:
    hibernate:
      ddl-auto: none  # MyBatis-Plus 自己建表
```

### 6.3 启动

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 6.4 关闭种子

```yaml
skillsmap:
  seed:
    enabled: false
```

## 7. 端口与资源

| 服务 | 端口 | 内存 | 备注 |
|---|---|---|---|
| Spring Boot (Undertow) | 8767 | 建议 512M+ | dev 默认 |
| Vite dev | 7777 | — | 转发 `/api` → 8767 |
| MySQL (local) | 3306 | 256M+ | 仅 local / prod |
| H2 Console | 8767/h2-console | — | dev only |
| Druid Console | 8767/druid | — | dev only |

## 8. 防火墙 / 杀软

Windows 第一次启动可能弹防火墙提示（Undertow 监听 8767），**点"允许"**。

## 9. 验证脚本（一键跑通）

`scripts/dev-verify.sh`（Git Bash）：

```bash
#!/usr/bin/env bash
set -e

echo "== 验证后端 =="
curl -sf http://localhost:8767/api/categories > /dev/null && echo "  ✓ /api/categories OK"
curl -sf 'http://localhost:8767/api/skills?page=1&size=1' > /dev/null && echo "  ✓ /api/skills OK"

echo "== 验证前端 =="
curl -sf http://localhost:7777/ > /dev/null && echo "  ✓ / OK"
curl -sf http://localhost:7777/browse > /dev/null && echo "  ✓ /browse OK"

echo "== 验证登录 =="
TOKEN=$(curl -s -X POST http://localhost:8767/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .data.token)
[ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] && echo "  ✓ /api/auth/login OK (token len=${#TOKEN})"

echo "ALL GREEN"
```

## 10. 常见坑速查

| 症状 | 解决 |
|---|---|
| 中文乱码 | 确认 MySQL 字符集 `utf8mb4`；前端请求头 `Content-Type: application/json;charset=UTF-8` |
| 端口被占 | 杀进程 / 改端口 |
| 启动后 30s 内退出 | 看 log（OOM / DB 连不上 / 端口占） |
| Knife4j 404 | 缺 `OpenApiConfig` bean；或 prod profile 自动关 |
| 前端 auto-import 失效 | 清 `.nuxt` / `node_modules/.vite` 缓存 |
| Windows 路径反斜杠 | 用 `/` 或 `\\`；`path-to-regexp` 编译时已处理 |
| PowerShell 不识别 `./mvnw` | 用 `.\mvnw.cmd` |
| `npm install` 报 `gyp ERR! find Python` | 装 Python 3.x（仅当依赖含 native 模块） |

## 11. 工具推荐

| 类别 | 推荐 |
|---|---|
| API 调试 | Knife4j（内置） / Postman / Insomnia |
| JSON 查看 | `jq`（命令行） / 浏览器 F12 |
| 数据库 | IDEA Database / DBeaver / MySQL Workbench |
| Markdown | Typora / VS Code + 预览 |
| Git GUI | Sourcetree / GitHub Desktop |

## 12. 修订记录

| 版本 | 日期 | 作者 | 摘要 |
|---|---|---|---|
| v1.0 | 2026-06-06 | ops-max | 初版开发环境 0→1：依赖 + 安装 + 启动 + 验证 + local 模式 + 常见坑 |
