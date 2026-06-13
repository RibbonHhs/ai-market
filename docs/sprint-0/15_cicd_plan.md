# CI/CD Plan（持续集成 / 持续交付 草图）

> 作者：ops-max @ Sprint 0 Kickoff (2026-06-06)
>
> 版本：v0.1 · 状态：**草图，不实施** · 实施时间窗：Sprint 2 ~ Sprint 3 · 引用：`14_dev_env_setup.md` / `12_dod.md` / `10_tech_architecture.md` / `.claude/CLAUDE.md`

## 0. 范围声明

本文档**仅出方案草图**，不实际配置 GitHub Actions / Jenkins / GitLab CI。Sprint 0 期间任何 CI 实施都视为范围蔓延。

Sprint 1 内 Ops 实施里程碑：

| Sprint | 任务 |
|---|---|
| Sprint 1 | 选型（GitHub Actions vs Jenkins）+ 跑通 §1 阶段 1（PR Check） |
| Sprint 2 | 阶段 2（Build & Push Image）+ 阶段 3（Deploy Staging） |
| Sprint 3 | 阶段 4（Deploy Prod with Approval） + 监控告警对接 |

## 1. 阶段划分（4 Stage Pipeline）

```
┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐
│ Stage 1  │──►│ Stage 2  │──►│ Stage 3  │──►│ Stage 4  │
│ PR Check │   │ Build &  │   │ Deploy   │   │ Deploy   │
│          │   │ Push     │   │ Staging  │   │ Prod     │
│          │   │ Image    │   │ (auto)   │   │ (manual) │
└──────────┘   └──────────┘   └──────────┘   └──────────┘
   必做          必做          必做          选做
   ≤ 5 min       ≤ 10 min      ≤ 5 min      ≤ 15 min
```

### 1.1 Stage 1 — PR Check（必做，Sprint 1）

| 触发 | `pull_request` 到 `master` / `main` |
|---|---|
| 节点 | GitHub Actions：`ubuntu-22.04` |
| 工具 | JDK 21 / Node 20 / Cache（maven / npm） |
| 步骤 | 1. Checkout  2. Setup JDK 21 + Node 20  3. Cache `~/.m2` `~/.npm`  4. `./mvnw -q clean verify`  5. `cd frontend && npm ci && npm run build`  6. `npx vue-tsc --noEmit` |
| 产物 | 无（仅 pass / fail） |
| 失败处理 | PR 阻塞合并；通知 Lead |

### 1.2 Stage 2 — Build & Push Image（必做，Sprint 2）

| 触发 | `push` 到 `master` / `main` |
|---|---|
| 工具 | Docker Buildx + 镜像仓库（推荐：阿里云容器镜像服务 / GitHub Container Registry） |
| 步骤 | 1. Checkout  2. Setup JDK 21 + Node 20  3. `npm ci && npm run build` → 产物到 `backend/src/main/resources/static/`  4. `./mvnw clean package -DskipTests`  5. `docker build -t <registry>/skillsmap-backend:$SHA backend/`  6. `docker push` |
| 产物 | 2 个镜像（backend / frontend nginx）；tag 含 git SHA + 时间戳 |
| 失败处理 | 镜像 push 失败 → Slack 告警 → 阻断 Stage 3 |

### 1.3 Stage 3 — Deploy Staging（必做，Sprint 2）

| 触发 | Stage 2 成功 |
|---|---|
| 目标 | Staging 环境（K8s / Docker Compose 均可） |
| 工具 | `kubectl apply` / `docker compose up -d` |
| 步骤 | 1. 拉新镜像  2. 更新 deployment  3. 等待 readiness probe  4. 跑 10 条冒烟用例（自动化脚本）  5. 失败自动回滚（`kubectl rollout undo`） |
| 产物 | Staging URL（<https://staging.skillsmap.example>） |
| 失败处理 | 自动回滚 + 告警 |

### 1.4 Stage 4 — Deploy Prod（选做，Sprint 3，需 Lead 手动 Approve）

| 触发 | Stage 3 通过 + 手动 Approve（GitHub Environment） |
|---|---|
| 目标 | Prod 环境（K8s / 多实例） |
| 步骤 | 1. 拉镜像  2. 蓝绿 / 灰度发布（10% → 50% → 100%）  3. 健康检查  4. 失败自动回滚 |
| 审批 | 至少 1 个 Lead Approve（GitHub Environment Protection Rules） |
| 失败处理 | 立即回滚 + Postmortem |

## 2. 触发条件总览

| 触发 | 跑哪些阶段 |
|---|---|
| PR 开 / 更新 | Stage 1 |
| Push master | Stage 2 + 3 |
| 手动（带 tag v*） | Stage 2 + 3 + 4 |
| Cron（每周日凌晨） | 仅 Stage 1（依赖 CVE 扫描） |

## 3. 产物（Artifacts）

| 阶段 | 产物 | 保留期 | 用途 |
|---|---|---|---|
| Stage 1 | 测试报告（Surefire / Vue TSC） | 30 天 | 失败回溯 |
| Stage 2 | 后端 jar + 镜像 `skillsmap-backend:<sha>` | 90 天 | 部署 / 回滚 |
| Stage 2 | 前端 dist + 镜像 `skillsmap-frontend:<sha>` | 90 天 | 部署 / 回滚 |
| Stage 3 | Staging URL + 冒烟报告 | 30 天 | 验收 |
| Stage 4 | Prod URL + 部署记录 | 永久 | 审计 |

## 4. 环境（Environments）

| 环境 | 用途 | 数据 | 域名 |
|---|---|---|---|
| **Dev** | 本地（无 CI 部署） | H2 内存库 | `localhost:8767` |
| **Staging** | 类生产 + 真实数据脱敏 | MySQL + 脱敏种子 | `staging.skillsmap.example` |
| **Prod** | 生产 | MySQL + 真实用户 | `skillsmap.example` |

## 5. 回滚策略（Rollback）

### 5.1 镜像回滚（首选）

```bash
# K8s
kubectl set image deployment/skillsmap-backend backend=<registry>/skillsmap-backend:<previous-sha> -n prod
kubectl rollout status deployment/skillsmap-backend -n prod
```

### 5.2 DB 迁移回滚

- v1 无 Flyway / Liquibase（直接由 MyBatis-Plus 建表）
- 任何 schema 变更走"先扩列、后清理"原则，避免一次性 drop column
- 应急：H2 不持久化可丢；MySQL 用 `mysqldump` 每 6h 一次自动备份（待 Ops 配）

### 5.3 配置回滚

- yml 走 Git，revert PR 即回滚
- jasypt 加密密钥走环境变量 / KMS，密钥轮换时所有配置需重加密

### 5.4 回滚 SLA

| 严重度 | SLA |
|---|---|
| P0（服务全挂） | ≤ 5 分钟回滚到上一版本 |
| P1（核心功能挂） | ≤ 30 分钟 |
| P2（次要功能挂） | ≤ 4 小时（不紧急回滚） |

## 6. 监控 & 告警（Sprint 3 配）

| 指标 | 工具 | 告警阈值 |
|---|---|---|
| API P95 | Prometheus + Grafana | > 500ms 持续 5min |
| 5xx 错误率 | Prometheus | > 1% 持续 1min |
| JVM Heap | Actuator + Prometheus | > 80% 持续 5min |
| DB 连接池 | Druid | 活跃 > 80% |
| 磁盘 | node_exporter | > 80% |

## 7. 依赖安全扫描（Sprint 1 末接入）

```xml
<!-- pom.xml 加 OWASP dependency-check 插件 -->
<plugin>
  <groupId>org.owasp</groupId>
  <artifactId>dependency-check-maven</artifactId>
  <version>9.0.9</version>
  <configuration>
    <failBuildOnCVSS>8</failBuildOnCVSS>
    <suppressionFiles>
      <suppressionFile>owasp-suppress.xml</suppressionFile>
    </suppressionFiles>
  </configuration>
</plugin>
```

```bash
./mvnw dependency-check:check
```

## 8. GitHub Actions 示例（Sprint 1 实施时用）

`.github/workflows/pr-check.yml`：

```yaml
name: PR Check

on:
  pull_request:
    branches: [master, main]

jobs:
  backend:
    runs-on: ubuntu-22.04
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
          cache: maven
      - name: Build & Test
        working-directory: backend
        run: ./mvnw -B clean verify

  frontend:
    runs-on: ubuntu-22.04
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - name: Install
        working-directory: frontend
        run: npm ci
      - name: Type Check
        working-directory: frontend
        run: npx vue-tsc --noEmit
      - name: Build
        working-directory: frontend
        run: npm run build
```

## 9. 镜像构建示例（Sprint 2）

`backend/Dockerfile`（已存在，需完善多阶段）：

```dockerfile
# 多阶段：先用 maven 构建，再用最小 JRE 运行
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/skills-map-backend.jar app.jar
EXPOSE 8767
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

`frontend/Dockerfile`：

```dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

## 10. 镜像仓库策略

| 项 | 决策 |
|---|---|
| Registry | 阿里云容器镜像服务（个人/小团队免费额度够用） / GHCR（开源友好） |
| 命名空间 | `skillsmap` |
| Tag 规范 | `<service>:git-sha-<short>` + `<service>:latest`（latest 仅 dev） |
| 保留 | 90 天（自动清理策略） |

## 11. 部署模式（Deploy Strategy）

| 环境 | 模式 | 备注 |
|---|---|---|
| Staging | 滚动更新（Rolling Update） | 默认 K8s |
| Prod | 蓝绿 / 灰度 | Sprint 3 评估 |

## 12. 密钥管理（Secrets）

| 类型 | 存储 | 注入方式 |
|---|---|---|
| DB 密码 | GitHub Secrets / K8s Secret | 环境变量 → jasypt 解密 |
| JWT 密钥 | GitHub Secrets / K8s Secret | 环境变量 |
| 镜像仓库凭证 | GitHub Secrets | `docker login` |
| 通知 Webhook（Slack） | GitHub Secrets | API 调用 |

> **永远不**把密钥 commit 到 Git（包括 jasypt 加密后的密文如果被猜到原文）。

## 13. 容量与扩展（Sprint 3+ 评估）

| 项 | v1 规模 | 扩展触发 |
|---|---|---|
| 后端实例 | 1 | CPU > 70% 持续 10min → +1 |
| DB | 1（dev / staging 同实例） | 连接数 > 80% |
| 文件存储 | 本地 `backend/data/` | > 10GB → 切 OSS / S3 |
| CDN | 无 | 国内访问慢 → 切阿里云 CDN / 腾讯云 CDN |

## 14. DoD 与 CI 的联动

每 PR 必须满足 `12_dod.md` 才能进 Stage 1；Stage 1 通过才允许合并。

## 15. 修订记录

| 版本 | 日期 | 作者 | 摘要 |
|---|---|---|---|
| v0.1 | 2026-06-06 | ops-max | 初版 CI/CD 草图：4 阶段 + 触发 + 产物 + 回滚 + 监控 + 实施计划 |
