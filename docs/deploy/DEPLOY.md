# SkillsMap 生产部署

> 适用版本：v1.0（Sprint S35+ main）
> 编排：`docker-compose.yml` (dev) + `docker-compose.prod.yml` (prod overlay)
> 镜像：MySQL 8.3.0 / backend (temurin 21) / frontend (nginx 1.27-alpine)

---

## 1. 前置条件

- 1 台 Linux 主机（推荐 4C8G 起步），Docker Engine 24+、`docker compose` v2
- 域名（强烈推荐）+ 反代（Caddy / Traefik / 云 LB），用于 HTTPS 终结
- 端口规划：宿主机对外只开 22 / 80 / 443；3306 / 8767 仅容器内访问；7777 视情况
- 系统时钟同步（`timedatectl` / chrony），避免 token / 日志时间漂移

---

## 2. 一次性初始化

```bash
# 1) 拉代码
git clone <repo-url> /opt/skills-map && cd /opt/skills-map

# 2) 生成 .env.prod（强随机密钥，权限 600，gitignore）
cp .env.prod.example .env.prod
./scripts/gen-secrets.sh
# 检查生成结果：cat .env.prod  （应看到 ≥32/48/64 字节的随机串）

# 3) （可选）自定义 MySQL 配置：将 *.cnf 放到 deploy/mysql/conf/

# 4) 校对 docker-compose.prod.yml 的端口 / 资源 limits
```

**关键点**：
- `.env.prod` 永远不要 commit；权限 600
- MYSQL_USER / MYSQL_PASSWORD / JASYPT_PASSWORD / JWT_SECRET 必须**随机生成**，禁止沿用 dev 默认值

---

## 3. 启动

```bash
# 构建并启动整套栈（首次会拉基础镜像 + 编译 backend）
./scripts/deploy.sh backend     # 等价：先 build → up -d --no-deps → 等 health
./scripts/deploy.sh frontend
./scripts/healthcheck.sh        # 返回 0 = 健康
```

**验证清单**：
- `docker compose -f docker-compose.yml -f docker-compose.prod.yml ps` — 三个服务都是 `healthy`
- `curl http://localhost:8767/actuator/health` → `{"status":"UP"}`
- `curl http://localhost:7777/` → 200 + SPA HTML

---

## 4. 备份策略

### 4.1 MySQL 每日全量

`scripts/backup-mysql.sh` 走 `mysqldump --single-transaction`（一致快照）+ gzip，落盘到 `./backups/mysql/`。

**CRON 配置**（每天 03:00 跑，root 用户 crontab）：
```cron
0 3 * * * cd /opt/skills-map && ./scripts/backup-mysql.sh >> /var/log/skillsmap-backup.log 2>&1
```

**异地同步**（强烈建议）：把 `./backups/mysql/` 用 rsync / rclone 推到 OSS / S3 / 另一台：
```cron
30 4 * * * rclone sync /opt/skills-map/backups/mysql remote:bucket/skillsmap-backups --log-file=/var/log/skillsmap-rclone.log
```

### 4.2 备份保留

- 本地：14 天（脚本自动 find -mtime +14 -delete）
- 异地：建议 30 / 90 天分级

### 4.3 恢复演练

```bash
# 1) 停 backend 避免写入冲突
docker stop skillsmap-backend

# 2) 恢复
./scripts/restore-mysql.sh backups/mysql/skillsmap_20260613_030000.sql.gz
# （必须手动输入 YES）

# 3) 起 backend，跑 healthcheck
./scripts/deploy.sh backend
./scripts/healthcheck.sh
```

**演练周期**：每季度至少 1 次（写入运维日历）。

---

## 5. 升级 / 滚动更新

```bash
# 1) 升级 backend
TAG=1.0.1 ./scripts/deploy.sh backend
# 观察 10 分钟：错误率 / 延迟 / 关键业务流

# 2) 升级 frontend（前后端可独立发布）
TAG=1.0.1 ./scripts/deploy.sh frontend

# 3) 失败回滚
TAG=1.0.0 ./scripts/rollback.sh backend
# 或：PREV_TAG=1.0.0 ./scripts/rollback.sh backend
```

**原则**：
- backend / frontend 可独立发布，不强求同步
- 生产**先发 backend 到 1 节点观察**（流量切走）→ 全量；本项目未做蓝绿，建议至少先观察 10 分钟
- 任何变更必须有回滚方案（保留旧镜像 tag 不删：`docker image ls`）

---

## 6. 回滚

```bash
# 显式指定目标 tag
PREV_TAG=1.0.0-rc1 ./scripts/rollback.sh backend
# 或
PREV_TAG=1.0.0-rc1 ./scripts/rollback.sh frontend
```

回滚后立即跑 `./scripts/healthcheck.sh` 验证。

---

## 7. 排障速查

| 症状 | 排查 |
|---|---|
| 容器反复 restart | `docker inspect skillsmap-backend --format='{{.State.Health.Status}}'` + `./scripts/logs.sh backend 300` |
| 后端 health 200 但业务 500 | `./scripts/logs.sh backend 500` + `curl -v http://localhost:8767/actuator/health` 看 db 状态 |
| MySQL 连接失败 | `docker exec -it skillsmap-mysql mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"` |
| 前端 502 | backend 是否 healthy？`docker inspect skillsmap-backend --format='{{.State.Health.Status}}'` |
| 磁盘满 | `docker system df`；`./scripts/logs.sh` 看应用日志；`du -sh /var/lib/docker/volumes/skillsmap_*` |
| 端口冲突 | `ss -lntp | grep -E '7777\|8767\|3306'` 找宿主机占用 |

**常用命令**：
```bash
# 进后端容器
docker exec -it skillsmap-backend sh
# 进 mysql
docker exec -it skillsmap-mysql mysql -u$MYSQL_USER -p$MYSQL_PASSWORD $MYSQL_DATABASE
# 实时日志
./scripts/logs.sh backend 200
# 看 health
curl -s http://localhost:8767/actuator/health | jq .
```

---

## 8. 安全基线

### 8.1 密钥轮转

- `MYSQL_PASSWORD` / `JASYPT_PASSWORD`：建议每 90 天轮转；轮转后需 update `.env.prod` + restart backend
- `JWT_SECRET` 轮转 = **全员 token 失效**，需提前公告，建议半年到一年
- 任何密钥生成都走 `./scripts/gen-secrets.sh`，禁止人肉编

### 8.2 暴露面

- 防火墙：仅开 22 / 80 / 443；8767 / 3306 仅本机 / 容器网络
- Knife4j / Swagger 已在 prod 关闭（`application-prod.yml`）
- nginx 已加 5 个安全头：X-Frame-Options / X-Content-Type-Options / Referrer-Policy / X-XSS-Protection / server_tokens off
- 限流：API 30r/s burst=60；其他 10r/s burst=20
- 反代终结 TLS（推荐 Caddy 自动证书），容器内仍 80 即可

### 8.3 镜像与依赖

- 后端 / 前端镜像 tag 固定 patch 版本（MySQL 8.3.0）
- 依赖升级走 PR 流程；升级前 `./mvnw -q clean compile` + `npm run build` 必须通过
- 镜像扫描：上线前 Trivy / Snyk 扫一遍高危 CVE

### 8.4 账号与审计

- 默认账号 `admin/admin123` **首次登录必须改密**
- 任何生产操作（deploy / rollback / restore）记录到运维日志
- 失败操作留下完整日志（`./scripts/logs.sh xxx 1000` 保存到工单）

---

## 9. 监控 / 告警（接入建议）

- **Health 探针**：外部 uptime 监控（如 UptimeRobot / Healthchecks.io）调 `./scripts/healthcheck.sh` 或直接 `curl /actuator/health`
- **指标**：actuator 已暴露 health，prometheus 指标可后续加 `micrometer-registry-prometheus`
- **日志**：backend 落盘到 `backend_logs` 卷；可接 Promtail / Filebeat → Loki / ES
- **告警分级**：
  - P0（核心不可用，5 分钟响应）：health 失败 / 容器 restart 循环
  - P1（部分不可用，30 分钟）：MySQL 慢查询 / 单接口错误率 > 1%
  - P2（体验问题，4 小时）：前端 SPA 加载慢 / 静态资源 404

---

## 10. 变更窗口与通知

- 生产变更尽量选**业务低峰**（凌晨 02:00–05:00）
- 变更前 30 分钟在群内公告「XX 时间 YY 操作，预计停机/灰度 N 分钟，回滚方案 ZZ」
- 变更中：每 5 分钟同步一次状态
- 变更后：健康检查通过 + 关键指标 OK → 关闭变更，发布「done」

---

## 附录 A：完整 Go-Live Checklist

参见 `docs/deploy/audit-report.md` §5。

## 附录 B：脚本索引

| 脚本 | 用途 | 用法 |
|---|---|---|
| `scripts/gen-secrets.sh` | 生成 `.env.prod` | `./scripts/gen-secrets.sh` |
| `scripts/deploy.sh` | 滚动更新 | `./scripts/deploy.sh [service]` |
| `scripts/rollback.sh` | 回滚到 PREV_TAG | `PREV_TAG=x.y.z ./scripts/rollback.sh backend` |
| `scripts/start.sh` | 启动整套栈 | `./scripts/start.sh` |
| `scripts/stop.sh` | 停机（保留 volume） | `./scripts/stop.sh` |
| `scripts/healthcheck.sh` | 健康探针 | `./scripts/healthcheck.sh` |
| `scripts/logs.sh` | follow 日志 | `./scripts/logs.sh backend 200` |
| `scripts/backup-mysql.sh` | 每日备份 | CRON：`0 3 * * *` |
| `scripts/restore-mysql.sh` | 恢复（需 YES） | `./scripts/restore-mysql.sh <file.sql.gz>` |
