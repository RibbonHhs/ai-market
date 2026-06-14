# SkillsMap 生产部署审计报告

> **审计时间**：2026-06-13
> **审计视角**：ops-max（DevOps / SRE）— 健壮性 / 可观测性 / 安全性 / 可回滚
> **审计范围**：Dockerfile / docker-compose / 运维脚本 / 生产配置 / 反向代理 / 部署文档
> **适用版本**：v1.0（基于 Sprint S35+ 当前 main）

---

## 0. 摘要（TL;DR）

| 维度 | 评级 | 说明 |
|---|---|---|
| 容器化骨架 | 中 | 后端 / 前端 Dockerfile 已存在且为多阶段；缺镜像版本固定 / 非 root 用户 / 镜像体积优化 |
| Compose 编排 | 中 | dev 友好，但 **生产模式混用**（profile 标注与实际不符、密码明文、缺资源限制） |
| 运维脚本 | 差 | 仅有 1 个本地 kill 脚本（PowerShell），缺 start / stop / health / backup / logrotate / migrate / rollback |
| 生产配置 | 中-差 | 有 `application-dev.yml` + `application-local.yml`，**缺 `application-prod.yml`**；日志仅 console，**无文件落盘 + 轮转** |
| 反向代理 | 中 | `nginx.conf` 已含 SPA fallback + API 反代；缺 HTTPS、限流、安全头、上游健康 |
| 部署文档 | 中 | README 有快速开始，**无独立 DEPLOY.md / 无 .env.prod 示例 / 无 runbook** |

**总体结论**：骨架已具备，**不能直接 production 上线**。需补 12 项缺口（5 高 / 4 中 / 3 低），预计工作量 1 人天。

---

## 1. 现状盘点（已存在的部署资产）

### 1.1 容器化（Dockerfile）

| 路径 | 类型 | 评估 |
|---|---|---|
| `D:\codeing\workspace\skills-map\backend\Dockerfile` | 多阶段（maven 3.9-temurin-21 → eclipse-temurin:21-jre） | OK，但 `eclipse-temurin:21-jre` 未固定 patch 版本；JRE 镜像内 apt 装 git 增大约 200MB；未建非 root user；未显式设置 `LANG=C.UTF-8` |
| `D:\codeing\workspace\skills-map\frontend\Dockerfile` | 多阶段（node:22-alpine → nginx:1.27-alpine） | OK，alpine 镜像较瘦；`COPY nginx.conf` 路径正确；`healthcheck` 用 `wget` 可用 |
| `D:\codeing\workspace\skills-map\.dockerignore` | 存在 | OK，覆盖 node_modules / target / dist / .git / .idea / docs / seed-data |

### 1.2 编排（docker-compose）

| 路径 | 评估 |
|---|---|
| `D:\codeing\workspace\skills-map\docker-compose.yml` | 唯一 compose 文件，**无 prod/dev 区分**；`docker compose --profile full up` 引用了不存在的 `profiles: [full]` 字段；MYSQL 密码、JWT secret、Jasypt key 全部明文；backend healthcheck 引用 `/actuator/health` 但 pom 中未引入 actuator starter → **健康检查会持续失败** |

### 1.3 后端应用配置

| 路径 | profile | 用途 | 评估 |
|---|---|---|---|
| `backend/src/main/resources/application.yml` | 公共 | 端口 / Jackson / Jasypt / JWT / Seed / Git source | OK，使用 `${ENV:default}` 注入；Jasypt 加密占位 + JWT secret 占位均就绪 |
| `backend/src/main/resources/application-dev.yml` | dev | H2 内存库 + 控制台 | OK |
| `backend/src/main/resources/application-local.yml` | local | MySQL 8 + Druid | OK，但 `useSSL=false`、未设 `&serverTimezone=Asia/Shanghai` 之外的 `&rewriteBatchedStatements=true` 等性能开关 |
| `backend/src/main/resources/application-prod.yml` | **缺失** | — | **必须新增** |

### 1.4 前端运行时

| 路径 | 评估 |
|---|---|
| `frontend/nginx.conf` | 存在；含 gzip / SPA fallback / API 反代 / Knife4j 反代 / 静态资源缓存；**缺 HTTPS / HSTS / X-Frame-Options / 限流 / proxy 超时** |
| `frontend/.env*` | **缺失**（未看到 `.env.production` 等；目前 API 走 nginx 反代，不需前端 env 单独配，但建议保留 `VITE_API_BASE` 兜底以支持直连场景） |

### 1.5 运维脚本

| 路径 | 类型 | 评估 |
|---|---|---|
| `backend/kill-8767.ps1` | Windows 杀进程脚本 | 仅 dev 期本地用，**无对应 .sh**；仅做 kill，**无优雅停机 / SIGTERM** |
| `backend/delombok.py` | 工具脚本 | 与部署无关 |
| `scripts/` 目录 | **不存在** | 缺失全套运维脚本 |

### 1.6 CI / 制品

| 路径 | 评估 |
|---|---|
| `.github/workflows/*` | **不存在**；无 CI / CD 流水线 |
| `backend/s22-backend.log` `backend-dev.log` `frontend-dev.log` | dev 期日志文件，**未纳入日志轮转 / 未挂卷** |

### 1.7 文档

| 路径 | 评估 |
|---|---|
| `README.md` | 含"方式一 / 方式二"启动 + 端口表 + 默认账号 + 工具命令；缺故障排查 / 备份恢复 / 升级流程 |
| `docs/PRD.md` `API.md` `ER.md` `SEED_DATA.md` | 与部署无关 |
| `docs/deploy/` | **当前为空，本次审计创建** |
| `SPRINT_BOARD.md` | 与部署无关 |

---

## 2. 缺失清单（按优先级）

### P0 / 高风险（生产事故级）

| # | 缺口 | 后果 |
|---|---|---|
| H1 | **缺 `application-prod.yml`**（仅 dev / local） | 直接用 `local` 跑生产将保留 H2 控制台、Knife4j 暴露、`/actuator` 行为不一致；信息泄露 + 误用 |
| H2 | **`docker-compose.yml` 缺 prod 变体** | 同一 compose 含 dev seed path、弱密码、缺资源限制；运维无法分环境拉起 |
| H3 | **`/actuator/health` 引用但未引入 actuator starter**（pom 缺 `spring-boot-starter-actuator`） | compose healthcheck 永远 fail → 容器反复重启 / 服务不可用 |
| H4 | **缺镜像 tag 固定 + 非 root 用户** | `eclipse-temurin:21-jre` 浮动到 broken 版本会拖死 prod；JRE 镜像以 root 跑违反 CIS-Docker 4.1 |
| H5 | **日志仅 stdout、无文件 + 无轮转** | 容器重启日志全失；Docker logs 容量无上限 → 磁盘撑爆；ELK/Promtail 无法稳定采集 |

### P1 / 中风险（运维 / 可用性）

| # | 缺口 | 后果 |
|---|---|---|
| M1 | **缺运维脚本**（start / stop / status / health / backup / restore / migrate / rollback / logrotate） | 故障时人工拼命令，RTO 显著拉长 |
| M2 | **缺 MySQL 备份策略**（无 mysqldump / xtrabackup 脚本，无 cron / 定时任务） | 丢数据风险 |
| M3 | **缺 `.env.prod` 模板 + 强密钥生成指引** | 部署者直接 copy `.env.example`，沿用明文密钥 |
| M4 | **nginx 缺 HTTPS / 安全头 / 限流** | 中间人攻击、点击劫持、CC 攻击无防护 |
| M5 | **缺独立部署文档 `DEPLOY.md`** | 运维交接 / 排障无手册 |

### P2 / 低风险（健壮性优化）

| # | 缺口 | 后果 |
|---|---|---|
| L1 | **缺 CI 工作流**（`.github/workflows/ci.yml`） | 合并前无自动化校验 |
| L2 | **`docker-compose.yml` 未配 `deploy.resources` / ulimits** | 单容器可吃满宿主机 CPU/内存；OOM 风险 |
| L3 | **缺 README "生产部署"小节**（仅 dev 模式） | 新人不知道 prod 走哪条路 |

---

## 3. 补全方案（直接可用代码 / 配置）

> 所有路径相对项目根 `D:\codeing\workspace\skills-map\`。
> 本次仅出草案，**未写入任何文件**（审计任务要求"不要修改任何代码"）。

### 3.1 H1 + H3：`application-prod.yml` + 引入 actuator

**`backend/pom.xml`** 在 `<dependencies>` 内新增：

```xml
<!-- Actuator (健康检查 + 指标) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**`backend/src/main/resources/application-prod.yml`**（新建）：

```yaml
# Prod profile: MySQL + Druid + 文件日志 + Actuator 暴露 health
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${DB_HOST:127.0.0.1}:${DB_PORT:3306}/${DB_NAME:skillsmap}?useUnicode=true&characterEncoding=UTF-8&useSSL=true&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=false&rewriteBatchedStatements=true&useServerPrepStmts=true&cachePrepStmts=true
    username: ${DB_USER}
    password: ${DB_PASSWORD}     # 生产严禁给默认值，缺失即失败
    druid:
      initial-size: 10
      min-idle: 10
      max-active: 50
      max-wait: 30000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
      test-on-return: false
      filters: stat,wall
      # StatViewServlet 在生产关闭（用监控侧抓 /druid 不可达）
      stat-view-servlet:
        enabled: false

  # 生产关闭 H2 控制台 / SQL init
  h2:
    console:
      enabled: false
  sql:
    init:
      mode: never

# Knife4j 在生产关闭
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
knife4j:
  enable: false

# Actuator: 仅暴露健康 + 基础 info
management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: never
      probes:
        enabled: true
  info:
    env:
      enabled: false
  health:
    db:
      enabled: true
    diskspace:
      enabled: true

# 日志落盘 + 按日切割
logging:
  file:
    name: ${LOG_PATH:/app/logs}/skills-map-backend.log
  logback:
    rollingpolicy:
      max-file-size: 50MB
      max-history: 30
      total-size-cap: 5GB
      file-name-pattern: ${LOG_FILE_NAME_PATTERN:/app/logs/skills-map-backend.%d{yyyy-MM-dd}.%i.log.gz}
  level:
    root: INFO
    com.meiya.skillsmap: INFO

# 限流在生产默认开启
skillsmap:
  rate-limit:
    enabled: ${SKILLSMAP_RATE_LIMIT_ENABLED:true}
    capacity: ${SKILLSMAP_RATE_LIMIT_CAPACITY:120}
    refill-tokens: ${SKILLSMAP_RATE_LIMIT_REFILL_TOKENS:120}
    refill-period: ${SKILLSMAP_RATE_LIMIT_REFILL_PERIOD:1m}
  # 生产默认关闭 seed（避免重启时反复扫描）
  seed:
    enabled: ${SKILLSMAP_SEED_ENABLED:false}
```

### 3.2 H2：`docker-compose.prod.yml`（新建）

```yaml
# 生产编排：MySQL + Backend + Frontend，资源限制 + 真实密钥 + 健康检查
# 启动：docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d
version: '3.8'

services:
  mysql:
    image: mysql:8.3.0          # 固定 patch 版本
    container_name: skillsmap-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: ${MYSQL_DATABASE:-skillsmap}
      MYSQL_USER: ${MYSQL_USER}
      MYSQL_PASSWORD: ${MYSQL_PASSWORD}
      TZ: Asia/Shanghai
    command:
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
      - --default-time-zone=+08:00
      - --max-connections=512
      - --innodb-buffer-pool-size=1G
    volumes:
      - mysql_data:/var/lib/mysql
      - ./deploy/mysql/conf:/etc/mysql/conf.d:ro
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "127.0.0.1", "-u", "${MYSQL_USER}", "-p${MYSQL_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 10
      start_period: 30s
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2G
    networks:
      - skillsmap-net
    logging:
      driver: json-file
      options:
        max-size: "20m"
        max-file: "10"

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    image: skills-map-backend:1.0.0
    container_name: skillsmap-backend
    restart: unless-stopped
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: prod          # 切到 prod
      SERVER_PORT: 8767
      DB_HOST: mysql
      DB_PORT: 3306
      DB_NAME: ${MYSQL_DATABASE}
      DB_USER: ${MYSQL_USER}
      DB_PASSWORD: ${MYSQL_PASSWORD}
      JASYPT_PASSWORD: ${JASYPT_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      LOG_PATH: /app/logs
      JAVA_OPTS: "-Xms512m -Xmx1024m -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"
      TZ: Asia/Shanghai
    volumes:
      - backend_logs:/app/logs              # 持久化日志
      - backend_data:/app/data              # skill-clones / skill-packages 持久化
    expose:
      - "8767"
    healthcheck:
      test: ["CMD-SHELL", "curl -fsS http://localhost:8767/actuator/health/liveness || exit 1"]
      interval: 15s
      timeout: 5s
      retries: 5
      start_period: 60s
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 1.5G
    networks:
      - skillsmap-net
    logging:
      driver: json-file
      options:
        max-size: "20m"
        max-file: "10"

  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    image: skills-map-frontend:1.0.0
    container_name: skillsmap-frontend
    restart: unless-stopped
    depends_on:
      backend:
        condition: service_healthy
    ports:
      - "${FRONTEND_PORT:-7777}:80"
    healthcheck:
      test: ["CMD-SHELL", "wget -qO- http://localhost/ >/dev/null 2>&1 || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 3
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 256M
    networks:
      - skillsmap-net
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "5"

volumes:
  mysql_data:
    name: skillsmap_mysql_data
  backend_logs:
    name: skillsmap_backend_logs
  backend_data:
    name: skillsmap_backend_data

networks:
  skillsmap-net:
    name: skillsmap_net
    driver: bridge
```

**`backend/Dockerfile`** 修正建议（diff 形式）：

```dockerfile
# 第 14 行 改为固定版本
- FROM eclipse-temurin:21-jre
+ FROM eclipse-temurin:21.0.2_13-jre-jammy

# 第 28 行后追加：建非 root 用户 + 切主
+ RUN groupadd -r app && useradd -r -g app -d /app -s /sbin/nologin app \
+     && mkdir -p /app/logs /app/data \
+     && chown -R app:app /app
+ USER app
```

### 3.3 H4 / H5 延伸：调整 backend Dockerfile 支持 logback 滚动

在 `backend/src/main/resources/` 增加 `logback-spring.xml`（新建）：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
  <property name="LOG_PATH" value="${LOG_PATH:-./logs}"/>
  <property name="APP_NAME" value="skills-map-backend"/>

  <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>

  <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_PATH}/${APP_NAME}.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
      <fileNamePattern>${LOG_PATH}/${APP_NAME}.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
      <maxFileSize>50MB</maxFileSize>
      <maxHistory>30</maxHistory>
      <totalSizeCap>5GB</totalSizeCap>
    </rollingPolicy>
    <encoder>
      <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
  </appender>

  <root level="INFO">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="FILE"/>
  </root>
  <logger name="com.meiya.skillsmap" level="INFO"/>
</configuration>
```

### 3.4 M1：运维脚本骨架（`scripts/` 目录）

#### `scripts/deploy.sh`（部署 / 滚动更新）

```bash
#!/usr/bin/env bash
# 滚动更新后端：build 新镜像 → up -d --no-deps backend → 等 healthcheck
set -euo pipefail

COMPOSE="docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod"
SERVICE="${1:-backend}"
TAG="${TAG:-1.0.0}"

echo "[deploy] building $SERVICE:$TAG"
$COMPOSE build $SERVICE

echo "[deploy] rolling $SERVICE"
$COMPOSE up -d --no-deps --build $SERVICE

echo "[deploy] waiting for healthcheck..."
for i in {1..30}; do
  status=$(docker inspect --format='{{.State.Health.Status}}' "skillsmap-$SERVICE" 2>/dev/null || echo "starting")
  echo "  attempt $i: $status"
  [ "$status" = "healthy" ] && exit 0
  sleep 5
done
echo "[deploy] FAILED to become healthy in 150s" >&2
exit 1
```

#### `scripts/rollback.sh`（回滚）

```bash
#!/usr/bin/env bash
set -euo pipefail
COMPOSE="docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod"
SERVICE="${1:-backend}"
PREV_TAG="${PREV_TAG:-1.0.0-rc1}"

echo "[rollback] $SERVICE → $PREV_TAG"
IMAGE="${SERVICE}:${PREV_TAG}" $COMPOSE up -d --no-deps --force-recreate $SERVICE
$COMPOSE logs -f --tail=200 $SERVICE
```

#### `scripts/backup-mysql.sh`（每日 mysqldump）

```bash
#!/usr/bin/env bash
set -euo pipefail
# 用法：CRON 每天 03:00 跑 → ./scripts/backup-mysql.sh
ENV_FILE="${ENV_FILE:-.env.prod}"
[ -f "$ENV_FILE" ] || { echo "missing $ENV_FILE" >&2; exit 1; }
set -a; . "$ENV_FILE"; set +a

TS=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="${BACKUP_DIR:-./backups/mysql}"
mkdir -p "$BACKUP_DIR"
FILE="$BACKUP_DIR/skillsmap_${TS}.sql.gz"

# 容器内执行 mysqldump（已在 mysql 镜像里）
docker exec skillsmap-mysql sh -c \
  "exec mysqldump -u'$MYSQL_USER' -p'$MYSQL_PASSWORD' \
   --single-transaction --quick --routines --triggers --events \
   $MYSQL_DATABASE" | gzip -9 > "$FILE"

echo "[backup] $FILE ($(du -h "$FILE" | cut -f1))"

# 保留 14 天
find "$BACKUP_DIR" -name 'skillsmap_*.sql.gz' -mtime +14 -delete
```

#### `scripts/restore-mysql.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail
ENV_FILE="${ENV_FILE:-.env.prod}"; . "$ENV_FILE"
FILE="${1:?usage: $0 <backup.sql.gz>}"
[ -f "$FILE" ] || { echo "no such file: $FILE" >&2; exit 1; }

echo "[restore] this will OVERWRITE $MYSQL_DATABASE on $DB_HOST"
read -rp "type 'YES' to continue: " c
[ "$c" = "YES" ] || exit 1

gunzip -c "$FILE" | docker exec -i skillsmap-mysql \
  mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"
echo "[restore] done"
```

#### `scripts/healthcheck.sh`（运维 / 监控探针）

```bash
#!/usr/bin/env bash
# 用法：./scripts/healthcheck.sh → 退出 0=健康 / 1=异常
set -e
ENDPOINTS=(
  "http://localhost:8767/actuator/health"
  "http://localhost:7777/"
)
for url in "${ENDPOINTS[@]}"; do
  code=$(curl -fsS -o /dev/null -w "%{http_code}" --max-time 5 "$url" || echo 000)
  echo "[health] $url → $code"
  [ "$code" = "200" ] || exit 1
done
```

#### `scripts/start.sh` / `scripts/stop.sh` / `scripts/logs.sh`

```bash
# start.sh
#!/usr/bin/env bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml --env-file .env.prod up -d
docker compose ps

# stop.sh
#!/usr/bin/env bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml down

# logs.sh  用法：./scripts/logs.sh backend 200
#!/usr/bin/env bash
SERVICE="${1:-backend}"
LINES="${2:-100}"
docker compose -f docker-compose.yml -f docker-compose.prod.yml logs -f --tail="$LINES" "$SERVICE"
```

#### `scripts/gen-secrets.sh`（首次部署生成 .env.prod）

```bash
#!/usr/bin/env bash
set -euo pipefail
ENV_FILE="${ENV_FILE:-.env.prod}"
if [ -f "$ENV_FILE" ]; then
  echo "$ENV_FILE already exists, abort." >&2; exit 1
fi
umask 077
cat > "$ENV_FILE" <<EOF
# SkillsMap 生产环境变量（gitignore！权限 600）
MYSQL_ROOT_PASSWORD=$(openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | head -c 32)
MYSQL_DATABASE=skillsmap
MYSQL_USER=skillsmap
MYSQL_PASSWORD=$(openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | head -c 32)
JASYPT_PASSWORD=$(openssl rand -base64 32 | tr -dc 'A-Za-z0-9' | head -c 48)
JWT_SECRET=$(openssl rand -base64 48 | tr -dc 'A-Za-z0-9' | head -c 64)
FRONTEND_PORT=7777
TAG=1.0.0
EOF
chmod 600 "$ENV_FILE"
echo "generated $ENV_FILE"
```

> 同时把 `chmod +x scripts/*.sh` 加上，`git add scripts/`。

### 3.5 M3：`.env.prod.example`（提交到 git，部署者复制改）

```dotenv
# SkillsMap 生产环境变量模板
# 部署前：cp .env.prod.example .env.prod && ./scripts/gen-secrets.sh
# 重要：.env.prod 必须 gitignore，权限 600
MYSQL_ROOT_PASSWORD=
MYSQL_DATABASE=skillsmap
MYSQL_USER=skillsmap
MYSQL_PASSWORD=

# Jasypt 加密密钥（≥ 32 字节随机）
JASYPT_PASSWORD=

# JWT 签名密钥（≥ 64 字节随机；HS512 推荐 64 字节）
JWT_SECRET=

# 对外暴露的前端端口
FRONTEND_PORT=7777

# 镜像 tag
TAG=1.0.0
```

并在 `.gitignore` 追加：

```
.env.prod
backups/
```

### 3.6 M4：`frontend/nginx.conf` 安全增强版

```nginx
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    # gzip
    gzip on;
    gzip_min_length 1024;
    gzip_proxied any;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml application/xml+rss text/javascript image/svg+xml;

    # ===== 安全头（如果上游有 TLS，可加 HSTS） =====
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header Referrer-Policy "no-referrer-when-downgrade" always;
    add_header X-XSS-Protection "0" always;
    # add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # ===== 隐藏 nginx 版本 =====
    server_tokens off;

    # ===== 限流（防 CC） =====
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=30r/s;
    limit_req_zone $binary_remote_addr zone=general_limit:10m rate=10r/s;
    limit_req_status 429;

    # ===== SPA fallback =====
    location / {
        limit_req zone=general_limit burst=20 nodelay;
        try_files $uri $uri/ /index.html;
    }

    # ===== 后端 API =====
    location /api/ {
        limit_req zone=api_limit burst=60 nodelay;
        proxy_pass http://backend:8767/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Connection "";
        proxy_connect_timeout 5s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        proxy_buffering on;
    }

    # ===== 文档（生产建议直接 404 掉） =====
    location = /doc.html       { return 404; }
    location = /swagger-ui.html { return 404; }
    location /swagger-ui/      { return 404; }
    location = /v3/api-docs    { return 404; }
    location /v3/api-docs/     { return 404; }
    location /webjars/         { return 404; }

    # ===== 静态缓存 =====
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff2?)$ {
        expires 7d;
        add_header Cache-Control "public, max-age=604800, immutable";
        access_log off;
    }

    # ===== 禁止访问隐藏文件 =====
    location ~ /\.(?!well-known) { deny all; }
}
```

如需 HTTPS，在宿主机前置 Caddy / Traefik / 云 LB 终结 TLS（推荐），容器内仍走 80。

### 3.7 M5：`docs/deploy/DEPLOY.md` 大纲

```markdown
# SkillsMap 生产部署

## 前置
- 1 台 Linux 主机（≥ 4C8G），Docker 24+、docker compose v2
- 域名（可选，但生产强烈推荐）+ 反代（Caddy / Nginx / 云 LB）

## 一次性
1. `git clone … && cd skills-map`
2. `cp .env.prod.example .env.prod && ./scripts/gen-secrets.sh`
3. 校对 `docker-compose.prod.yml` 的端口 / 资源
4. 拷贝 MySQL 自定义配置到 `deploy/mysql/conf/`（可选）

## 启动
./scripts/deploy.sh backend
./scripts/deploy.sh frontend
./scripts/healthcheck.sh

## 备份
CRON: 0 3 * * * /opt/skills-map/scripts/backup-mysql.sh
恢复：./scripts/restore-mysql.sh backups/mysql/skillsmap_YYYYMMDD_HHMMSS.sql.gz

## 回滚
TAG=1.0.0-rc1 ./scripts/rollback.sh backend

## 排障
- 看日志：./scripts/logs.sh backend
- 进容器：docker exec -it skillsmap-backend sh
- MySQL 连接：docker exec -it skillsmap-mysql mysql -u$MYSQL_USER -p$MYSQL_PASSWORD skillsmap
- 健康检查失败：curl -v http://localhost:8767/actuator/health

## 升级流程
1. TAG=1.0.1 ./scripts/deploy.sh backend → 观察 10 分钟
2. 同样升级 frontend
3. 若失败：TAG=1.0.0 ./scripts/rollback.sh backend

## 安全基线
- 每月轮转 MYSQL_PASSWORD（rotate-and-restart）
- JWT_SECRET 轮转需要让所有 token 失效（提前通知）
- 关闭 Knife4j（已默认在 prod 关闭）
- 防火墙：仅开 22 / 80 / 443；8767 / 3306 仅容器内访问
```

### 3.8 L1：`.github/workflows/ci.yml` 骨架

```yaml
name: ci
on: [push, pull_request]
jobs:
  backend-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '21' }
      - run: cd backend && ./mvnw -B -q clean verify

  frontend-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with: { node-version: '22', cache: 'npm', cache-dependency-path: frontend/package-lock.json }
      - run: cd frontend && npm ci && npm run build
```

### 3.9 L2：`docker-compose.prod.yml` 已含 `deploy.resources`

（见 3.2）。

---

## 4. 风险提示（生产稳定性 7 维）

| # | 风险 | 当前状态 | 触发条件 | 缓解 |
|---|---|---|---|---|
| 1 | **密钥泄露 / 弱密钥** | `.env.example` 与 `docker-compose.yml` 直接用 `skillsmap-jwt-secret-key-please-change-in-production-environment-2024` | 任何人 git clone 即可获取生产密钥样本；初次部署者大概率不改 | 强制走 `gen-secrets.sh` 生成；`.env.prod` gitignore；`application-prod.yml` 移除默认值，缺 KEY 启动失败 |
| 2 | **健康检查不工作** | 引用 `/actuator/health` 但未引入 actuator | 容器反复进入 `unhealthy` → restart loop；前端永远 `depends_on: backend` 不放行 | 引入 `spring-boot-starter-actuator`（H3） |
| 3 | **日志丢失 / 磁盘撑爆** | 仅 stdout，`docker compose logs` 无轮转 | 容器重启日志全失；3 个月后 /var/lib/docker 满 → kubelet / docker 拒服务 | 落盘 + logback 滚动 + `logging.driver` json-file 配 `max-size` |
| 4 | **无数据库备份** | 无任何脚本 / cron | 误删 / 勒索 / 硬件故障 → 全量数据丢失 | `backup-mysql.sh` + CRON + 异地同步（rsync 到 OSS） |
| 5 | **无滚动更新 / 无回滚** | 仅 `docker compose up -d` | 部署失败 / 镜像错误 → 5xx 蔓延；RTO 取决于人工 | `deploy.sh` 等 health 才返；`rollback.sh` 1 行回滚 |
| 6 | **前端缺 HTTPS / 安全头** | nginx 80 明文 | 中间人 / 运营商劫持 / 点击劫持 / MIME sniff | 宿主机前置 Caddy 自动 TLS；nginx 加 5 个安全头 |
| 7 | **无资源限制** | 无 `deploy.resources` / ulimits | 单容器吃满宿主机 → 全栈雪崩 | compose 配 `cpus/memory limits`；启动加 `default-ulimits` |

---

## 5. 一次性 Go-Live Checklist（建议贴在工单）

```
[ ] H1 application-prod.yml 落地 + pom 引入 actuator
[ ] H2 docker-compose.prod.yml 落地 + 删除 dev seed 路径
[ ] H3 /actuator/health 在容器内返回 200
[ ] H4 后端镜像 tag 固定 + 非 root 用户
[ ] H5 backend logback-spring.xml 落盘 + 日志卷挂载
[ ] M1 scripts/*.sh 全部 chmod +x 并入 git
[ ] M2 backup-mysql.sh 加入 CRON (3:00 AM)，跑通一次还原演练
[ ] M3 .env.prod 已生成 (gen-secrets.sh) 且权限 600 / gitignore
[ ] M4 nginx 安全头已加 + Knife4j 路径 404
[ ] M5 docs/deploy/DEPLOY.md 发布，运维过一遍
[ ] L1 CI 工作流接入，PR 必须 green
[ ] L2 资源 limits 上线
[ ] 防火墙：8767 / 3306 仅本机；7777 / 80 / 443 对外
[ ] 域名 / 反代 / TLS 证书就绪
[ ] 默认账号 admin/admin123 已改密
[ ] 备份还原演练通过（杀 mysql 容器 → restore 10 分钟内可恢复）
[ ] 监控 / 告警接入（actuator health → uptime 监控）
[ ] 变更窗口 / 回滚预案 / 通知模板就绪
```

---

## 6. 关键路径速查

| 用途 | 路径 |
|---|---|
| 后端 Dockerfile | `D:\codeing\workspace\skills-map\backend\Dockerfile` |
| 前端 Dockerfile | `D:\codeing\workspace\skills-map\frontend\Dockerfile` |
| 当前 compose | `D:\codeing\workspace\skills-map\docker-compose.yml` |
| 前端 nginx | `D:\codeing\workspace\skills-map\frontend\nginx.conf` |
| 后端公共配置 | `D:\codeing\workspace\skills-map\backend\src\main\resources\application.yml` |
| 后端 dev 配置 | `D:\codeing\workspace\skills-map\backend\src\main\resources\application-dev.yml` |
| 后端 local 配置 | `D:\codeing\workspace\skills-map\backend\src\main\resources\application-local.yml` |
| 后端 pom | `D:\codeing\workspace\skills-map\backend\pom.xml` |
| 环境变量样例 | `D:\codeing\workspace\skills-map\.env.example` |
| .dockerignore | `D:\codeing\workspace\skills-map\.dockerignore` |
| 本次审计报告 | `D:\codeing\workspace\skills-map\docs\deploy\audit-report.md` |

---

> 本报告仅做审计，未改动任何文件。下一步可由 Lead 派发：pm-alice 出"生产部署 Sprint"PRD；dev-kevin 落地 H1–H5；ops-max 落地 M1–M5 / L1–L2。
