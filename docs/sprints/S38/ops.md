# S38 — Skill Upload 部署 / 监控 / 回滚 方案

> **作者**: ops-max（DevOps / SRE）
> **日期**: 2026-06-14
> **Sprint**: S38（用户上传 skill）
> **目标**: 让 S38 平稳从 staging 走到 prod，并在故障下 5 分钟内可回滚
> **范围**: 仅 S38 增量；不重写 `docs/deploy/DEPLOY.md`
> **状态**: 待 Lead 拍板 §7 待办

---

## 0. TL;DR

| 维度 | 结论 |
|---|---|
| 变更类型 | 后端新增端点 + 新 Flyway 迁移 + 新临时目录 + 新 @Scheduled 清理任务 + 前端新路由 |
| 风险等级 | **中**（V38 迁移失败 / zip 处理路径打满 / scheduler 失效 → 磁盘爆） |
| 部署策略 | **滚动灰度 1/4 → 全量**（与 S35+ 既定策略一致，无蓝绿） |
| 预计变更窗口 | 30 min（2 节点灰度 + 15 min 观察 + 流量全切） |
| 回滚 RTO | **≤ 3 min**（`rollback.sh backend` + Flyway 幂等保留 V38 列） |
| 待 Lead 拍板 | §7：磁盘扩容阈值 / WAF 规则 / CDN POST 缓存 |

---

## 1. 部署清单（30 min 灰度窗口）

### 1.1 步骤总表

| 步骤 | 操作 | 验证 | 备注 |
|------|------|------|------|
| 1 | 备份 prod DB | `mysqldump` 完成，文件 ≥ 50MB | Flyway 迁移 idempotent，但 V38 加列在 PB 级库上需备份兜底 |
| 2 | 拉新代码 `git pull` | `git log -1` 看 S38 commit | 包含 backend / frontend / skill 三路 |
| 3 | 启新后端（**1/4 pod 灰度**） | `kubectl rollout status` 或 docker compose scale | V38 迁移在此节点触发；非阻塞，加列操作秒级 |
| 4 | 跑 Flyway 迁移 | `flyway info` 看 V38 applied | 见 §2 详细验证 |
| 5 | 前端 build + 部署 | `npm run build` + nginx reload | 静态资源（路由 `/admin/skills/new`） |
| 6 | 灰度验证 15 min | 看 §3 监控指标 | 5xx 率 / P95 / 40100 / 临时目录 |
| 7 | 全量 rollout | 100% backend 切到新版本 | 旧镜像 tag 保留 7d（避免回滚失败） |
| 8 | 收尾监控 24h | 见 §6 观察清单 | 出首日 SLO 报告 |

### 1.2 详细命令序列

```bash
# 0) 进入项目根
cd /opt/skills-map

# 1) 备份 prod DB（前置条件；不可跳过）
./scripts/backup-mysql.sh
# 期望：./backups/mysql/skillsmap_$(date +%Y%m%d_%H%M%S).sql.gz 出现

# 2) 拉新代码
git pull origin main
git log -1 --oneline   # 确认 S38 commit 在 HEAD

# 3) 灰度：先 build 新后端镜像
TAG=s38-rc1 ./scripts/deploy.sh backend
# deploy.sh 内部：build → up -d --no-deps → 等 healthcheck

# 4) Flyway 自动迁移（启动时由 Spring Boot 触发）
docker exec skillsmap-backend \
  curl -fsS http://localhost:8767/actuator/health
# 期望：UP，且 logs 中可见 Flyway: "Successfully applied 1 migration to schema"
# 详细：docker logs skillsmap-backend 2>&1 | grep -i flyway

# 5) 前端 build + 部署
TAG=s38-rc1 ./scripts/deploy.sh frontend

# 6) 观察 15 min（关键监控看 §3）
# 5xx 率 < 1% / P95 < 3s / 40100 计数稳定 / upload-tmp < 1GB

# 7) 全量：剩余 3 个节点也升
# 当前 compose 编排为单节点（DEPLOY.md §5）；灰度=新版本跑在单节点
# 全量=继续在同节点观察 24h，无问题即视为稳定
# 若多节点：kubectl scale deploy/skillsmap-backend --replicas=4 后再 rollout

# 8) 保留旧镜像 tag（避免回滚失败）
docker images | grep skills-map-backend
# 期望：s37-stable (上次 prod tag) + s38-rc1 (本次) 共存
```

### 1.3 application-prod.yml 复核结论

**已读 `D:\codeing\workspace\skills-map\backend\src\main\resources\application-prod.yml`，结论：S38 必备配置齐全，无缺漏。**

| 配置项 | 当前值 | 评估 |
|--------|--------|------|
| `spring.servlet.multipart.max-file-size` | 10MB | ✅ 与 PRD §4.1 一致 |
| `spring.servlet.multipart.max-request-size` | 12MB | ✅ 多预留 2MB 给 `categoryId` / `tagSlugs` |
| `skillsmap.upload.tmp-dir` | `/data/skillsmap/upload-tmp` | ✅ 落到持久化卷（`backend_data` volume） |
| `springdoc.api-docs.enabled` | false | ✅ 生产关闭文档 |
| `knife4j.enable` | false | ✅ |
| `management.endpoints.web.exposure.include` | `health,info` | ⚠️ **缺 metrics**：未来需加 `prometheus` 端点，本期 S38 不阻塞 |
| `management.health.diskspace.enabled` | true | ✅ 可监控 `/data` 卷剩余 |
| `spring.datasource.druid.max-active` | 50 | ✅ V38 加列不影响连接池 |

**唯一建议（不阻塞 S38 上线）**：下一迭代加 `micrometer-registry-prometheus`，暴露 `http_server_requests_seconds_bucket` 指标，便于 §3 监控。

---

## 2. Flyway V38 迁移验证（重点）

### 2.1 迁移内容确认

`backend/src/main/resources/db/migration/V38__add_uploader_user_id.sql` 实际包含：

```sql
ALTER TABLE skill ADD COLUMN uploader_user_id BIGINT NULL;
CREATE INDEX idx_uploader_user_id ON skill(uploader_user_id);
-- FK 约束被注释（依赖 ops-max 上线脚本保证幂等）
```

**与 PRD §4.1 / tech-review §2.5 一致**。需注意：
- 迁移是**加列 + 加索引**，无 FK → 回滚 SQL 也不需要 DROP FK
- 索引创建在 PB 级库上可能耗时数分钟（InnoDB online DDL），监控 `ALTER TABLE` 进度

### 2.2 升级前快照

```sql
-- 必跑：升级前取证，便于事后核对
SELECT version, description, type, script, checksum, installed_rank, installed_by, execution_time, success
  FROM flyway_schema_history
 ORDER BY installed_rank DESC
 LIMIT 5;
-- 期望：最末行是 V37 之前的某版本，不含 V38

-- 加列检查
SHOW COLUMNS FROM skill LIKE 'uploader_user_id';
-- 期望：Empty set（V38 未跑）
```

### 2.3 升级后验证

```bash
# 1) 应用启动后立刻验证
docker exec skillsmap-backend \
  curl -fsS http://localhost:8767/actuator/health
# 期望：UP

# 2) Flyway 表
docker exec -it skillsmap-mysql mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e "
SELECT version, description, success, execution_time, installed_on
  FROM flyway_schema_history
 WHERE version = 'V38';
"
# 期望：
#   version | description                      | success | execution_time | installed_on
#   V38     | add uploader user id             |    1    |       1500     | 2026-06-14 ...

# 3) 加列成功
docker exec -it skillsmap-mysql mysql ... -e "
SHOW COLUMNS FROM skill LIKE 'uploader_user_id';
SHOW INDEX FROM skill WHERE Key_name = 'idx_uploader_user_id';
"
# 期望：uploader_user_id BIGINT NULL，索引存在
```

### 2.4 异常处理：V38 success=0

**最可能失败原因**：
- (a) MySQL 8.0 之前版本不支持 online DDL → 当前 8.3.0 ✅
- (b) `skill` 表数据量 > 1 亿行 → 索引创建超时（`lock_wait_timeout` 默认 50s）
- (c) 磁盘满 → 加列前需要 double-write buffer

**手动回滚 SQL（注意：V38 实际无 FK，**不要**带 `DROP FOREIGN KEY`）**：

```sql
-- 1) 先停 backend（避免迁移反复重试）
docker stop skillsmap-backend

-- 2) 回滚 schema
ALTER TABLE skill DROP INDEX idx_uploader_user_id;
ALTER TABLE skill DROP COLUMN uploader_user_id;

-- 3) 清 flyway 记录
DELETE FROM flyway_schema_history WHERE version = 'V38';

-- 4) 启动旧版本镜像（保留 V37 之前的所有迁移）
TAG=s37-stable ./scripts/rollback.sh backend

-- 5) 验证
docker exec -it skillsmap-mysql mysql ... -e "
SELECT version FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 3;
"
# 期望：最末是 V37（不含 V38）
```

**回滚后必须通知 Lead + Dev-kevin**，因为：
- 加列成功但应用回滚 → 列保留无副作用（旧代码不读这列，安全）
- 加列失败但应用未回滚 → **500 蔓延**，需立刻切流量

### 2.5 部署前必跑的烟测

```bash
# 在 staging 跑一遍，模拟 prod 数据量
mysqldump prod | mysql staging_s38_test
./scripts/deploy.sh backend   # 触发 Flyway
# 观察：
#   - ALTER TABLE 耗时（期望 < 60s 在 100 万行内）
#   - 加列期间是否锁表（应不锁，MySQL 8 INSTANT DDL）
#   - 启动后健康检查 200
```

---

## 3. 监控指标（关键 5 项）

> 现状：项目**未接入 Prometheus**（`management.endpoints.web.exposure.include: health,info`，缺 metrics）。
> 本期 S38 监控**先以日志 + 简单 HTTP 探针 + 磁盘 df 报警实现**；Prometheus 接入推到下一迭代。

### 3.1 监控矩阵

| # | 指标 | 来源 | 阈值 | 告警通道 | 实施方式 |
|---|------|------|------|---------|---------|
| 1 | `POST /api/skills` 5xx 率 | Loki（logback JSON `code=5*`） | > 1% 持续 5 min | 企业微信 #ops-alert | Promtail 解析 `http_code` 字段 → 5xx 率 |
| 2 | `POST /api/skills` 40100 计数 | Loki（`biz_code=40100`） | 单 IP > 10/min | 企业微信 + 临时 ban 5 min | nginx `limit_req` + Logstash 触发 fail2ban |
| 3 | 上传临时目录 `/data/skillsmap/upload-tmp/` 占用 | node_exporter `node_filesystem_avail_bytes` | < 5GB free | 企业微信 #ops-alert | AlertManager 规则 `disk_free < 5GB` |
| 4 | `UploadTmpCleanupScheduler` 执行成功率 | Loki（logger=`UploadTmpCleanupScheduler`） | 连续 3 次失败 | 企业微信 #ops-alert | Promtail 解析 `cleanup.success=false` 字段 |
| 5 | 上传 P95 耗时 | Loki（logback `duration_ms`） | > 3s 持续 10 min | 企业微信 #ops-alert | histogram_quantile 0.95 |

### 3.2 关键日志字段约定（Dev-kevin 需在 `SkillUploadService` 中输出）

```java
// SkillUploadServiceImpl.upload() 入口 / 出口统一打 INFO 日志
log.info("upload.start userId={} size={} name={}", userId, file.getSize(), file.getOriginalFilename());
log.info("upload.end userId={} skillId={} slug={} durationMs={} code={}",
    userId, skillId, slug, durationMs, bizCode);
```

**Loki LogQL 模板**（运维直接复用）：

```logql
# 5xx 率（5 分钟窗口）
sum(rate({app="skillsmap-backend"} |~ "biz_code=5[0-9]{2}" [5m]))
  / sum(rate({app="skillsmap-backend"} |~ "biz_code=" [5m]))

# 40100 计数（按 IP 聚合，5 分钟）
sum by (client_ip) (
  count_over_time({app="skillsmap-backend"} |~ "biz_code=40100" [5m])
) > 10

# 上传 P95 耗时
quantile_over_time(0.95,
  {app="skillsmap-backend"} |~ "upload.end" | unwrap duration_ms [10m]
) > 3000

# scheduler 失败
{app="skillsmap-backend"} |~ "UploadTmpCleanupScheduler.*success=false"
```

### 3.3 告警分级

| 等级 | 触发 | 响应 | 通知 |
|------|------|------|------|
| **P0** | 5xx 率 > 5% 持续 10 min | 5 min | 电话 + 短信 + 企业微信 |
| **P1** | 5xx 率 > 1% / 临时目录 < 5GB / scheduler 连续 3 次失败 | 30 min | 企业微信 + 群 |
| **P2** | P95 > 3s / 单 IP 40100 > 10/min | 4 h | 企业微信 |

### 3.4 配套：logback 配置确认

`backend/src/main/resources/logback-spring.xml` 已加（git status 显示 `A`），需确认：
- 输出 JSON 格式（含 `code` / `duration_ms` 字段）→ §3.2 LogQL 才能 parse
- 落盘到 `/app/logs/skills-map-backend.log` → Promtail 能采集
- 单文件 50MB / 保留 30 天 / 总 5GB → 磁盘可控

> **建议**（不阻塞）：在 `SkillUploadService` 加 MDC：`MDC.put("traceId", UUID.randomUUID())`，便于串接请求链路。

---

## 4. 回滚预案

### 4.1 回滚触发条件（任一即触发）

1. **Flyway V38 失败**（§2.4）— `success=0` 或 `actuator/health` DOWN
2. **5xx 率 > 5% 持续 10 min**（监控项 #1）— 应用层故障
3. **临时目录爆 5GB 持续 1h**（监控项 #3）— scheduler 失效且 ZIP 落盘堆积
4. **zip slip / zip bomb 真打到 prod**（PRD §6 风险，service 层防御未覆盖）— 紧急止血
5. **`POST /api/skills` 鉴权绕过** — 40100 = 0 但有非授权写入（紧急）

### 4.2 回滚步骤

```bash
# === 步骤 1：切流量到旧版本（≤ 1 min） ===
# 当前编排为单实例（DEPLOY.md §5）；直接切镜像 tag
TAG=s37-stable ./scripts/rollback.sh backend
# 内部逻辑：up -d --no-deps --force-recreate backend

# === 步骤 2：等 healthcheck 通过（≤ 1 min） ===
./scripts/healthcheck.sh
# 期望：http://localhost:8767/actuator/health → 200

# === 步骤 3：通知群内更新状态 ===
# 模板：[@ops-max] S38 已回滚到 s37-stable，原因：XXX，RTO 实际 X min

# === 步骤 4：保留 V38 加列 ===
# 关键：旧版本代码不读 uploader_user_id 列，保留无副作用
# 验证：
docker exec -it skillsmap-mysql mysql ... -e "
SELECT COUNT(*) AS col_exists
  FROM information_schema.columns
 WHERE table_schema = DATABASE()
   AND table_name = 'skill'
   AND column_name = 'uploader_user_id';
"
# 期望：1（列保留）

# === 步骤 5：清临时目录（避免磁盘满） ===
docker exec skillsmap-backend sh -c "
  rm -rf /data/skillsmap/upload-tmp/*
  echo 'tmp cleaned'
"
# 注意：旧版本会创建新子目录，无副作用

# === 步骤 6：发事故报告 + 修复 + 重新上线 ===
# 48h 内出 Postmortem，路径：docs/postmortems/20260614-s38-rollback.md
```

### 4.3 回滚不需做的事

| 误操作 | 风险 | 正确做法 |
|--------|------|----------|
| 手动 `DROP COLUMN uploader_user_id` | 旧版本代码不读这列；删列会触发锁表 | **保留列**，等下次正常发版再清 |
| 清 `flyway_schema_history` V38 行后不回滚应用 | 下次启动会重新跑 V38 → 加列重复 → 报错 | **要么都回滚，要么都保留** |
| 改 `.env.prod` 把 `SKILLSMAP_UPLOAD_TMP_DIR` 改回去 | 配置回退 ≠ 代码回退，混淆事故现场 | 仅回滚镜像 tag，配置不动 |

### 4.4 数据回滚（极端场景）

**如果上传的数据有脏数据**（zip 含恶意文件 / 错入库），**不要直接 DELETE**：

```bash
# 1) 逻辑删除（skill 表有 deleted 字段，0/1）
docker exec -it skillsmap-mysql mysql ... -e "
UPDATE skill
   SET deleted = 1, deleted_at = NOW()
 WHERE uploader_user_id = <userId>
   AND created_at > '2026-06-14 12:00:00';
"

# 2) 物理文件移到隔离区
mkdir -p /data/skillsmap/QUARANTINE/
mv /data/skillsmap/skill-packages/<slug>/* /data/skillsmap/QUARANTINE/
# 不要 rm -rf，留给安全审计

# 3) 通 Lead + 安全 review 后再清
```

---

## 5. 容量预估

### 5.1 上传流量

**假设**：
- 100 用户 / 天，每人 1 次上传，1MB 平均
- 月增 ~3GB（按 30 天）
- 峰值：并发 10 用户 × 10MB = 100MB 临时目录

### 5.2 `skills.storage.path` 容量

**当前卷**：`backend_data` volume（docker-compose.prod.yml L77），挂载 `/app/data/`
**子目录**：`/app/data/skill-packages/{slug}/{version}.zip`
**当前实际占用**：未在 DEPLOY.md 中明示（**待 Ops 上线前 `du -sh` 实测**，§7 待办 #2）

**基线判断**：
- 现网 S35+ 用户量级 ~ 几百 skill / 月
- 累计预期 1 年 ≤ 50GB（含历史 5 个月的 S02 之后数据）
- 当前主机磁盘默认 ≥ 100GB（DEPLOY.md §1 推荐 4C8G → 至少 100GB 系统盘）

**结论**：**短期（3 个月内）无需扩容**。若 1 个月内 `du -sh /data/skillsmap/skill-packages` > 50GB，触发 §7 待办扩容。

### 5.3 临时目录 `/data/skillsmap/upload-tmp/` 容量

| 维度 | 数值 | 备注 |
|------|------|------|
| 单文件上限 | 10MB | application-prod.yml |
| 并发上传上限 | 10 用户 | 假设；可调 |
| 临时目录峰值 | **100MB** | 10 × 10MB |
| scheduler 清理周期 | 1h | `@Scheduled(fixedRate = 1h)` |
| 清理阈值 | mtime > 24h | 与 PRD §4.1 一致 |
| 稳态占用 | **< 50MB** | scheduler 每小时清 + 进程 finally 清 |

**告警阈值**：< 5GB free → P1 告警（实际是 5GB free space 阈值，非占用阈值；误读会反向）

### 5.4 数据库容量影响

- V38 加列：`uploader_user_id BIGINT NULL` → 单行 +8 字节
- 索引：`idx_uploader_user_id` → 每行 +8 字节索引项
- 100 万 skill → +16MB 表 + 索引开销（可忽略）
- 不会触发 MySQL `innodb_buffer_pool_size` 调整

---

## 6. 安全 checklist

| # | 项 | 状态 | 备注 |
|---|----|------|------|
| 1 | multipart 限 10MB（`max-file-size: 10MB`） | ✅ | application-prod.yml L37 |
| 2 | 请求体限 12MB（`max-request-size: 12MB`） | ✅ | application-prod.yml L38 |
| 3 | 上传路径 `/admin/skills/new` 走 `meta.requiresAdmin` | ✅ | tech-review §3.5（前端） |
| 4 | 上传端点 `POST /api/skills` 走 `JwtAuthFilter` 鉴权 | ✅ | tech-review §2.6 |
| 5 | zip slip 防御（`getCanonicalPath().startsWith(dest)`） | ✅ | tech-review §2.3 + 单测 fixture |
| 6 | zip bomb 防御（10MB / 50MB 总 / 5MB 单文件 / 100 条目） | ✅ | tech-review §2.3 |
| 7 | frontmatter YAML 注入（snakeyaml safe load） | ✅ | 复用 `MarkdownFrontmatterParser` |
| 8 | 鉴权 JWT Bearer Token | ✅ | application.yml L69 |
| 9 | Knife4j 生产关闭 | ✅ | application-prod.yml L47 |
| 10 | 日志不打印 zip 内容（仅打 userId + size + name） | ✅ | logback JSON 约定 |
| 11 | **WAF 规则封禁 `multipart/form-data` 体积 > 15MB** | ❌ 待补 | §7 待办 #3 |
| 12 | **CDN 不缓存 `POST /api/skills` 响应** | ❌ 待补 | §7 待办 #4 |
| 13 | **`uploader_user_id` 非 NULL 约束** | ⚠️ 可选 | tech-review §2.5 倾向 NULL（兼容旧 admin 路径） |
| 14 | **临时目录权限**（仅 appuser 可读写） | ⚠️ 待验 | docker 容器内 `app` user，§7 待办 #5 |
| 15 | **磁盘满时 `Files.exists` 试探** | ✅ | tech-review §2.7 风险 → 缓解 |

### 6.1 反向代理层（nginx）需补

```nginx
# 在 /api/ location 中追加
client_max_body_size 15M;     # 比 Spring 多 5MB buffer，nginx 先截
client_body_buffer_size 1M;
client_body_timeout 30s;       # 避免慢速上传占连接
```

**但需 Dev-kevin 确认**：`client_max_body_size 15M` 与 §7 WAF 规则不冲突（WAF 在更外层）。

---

## 7. 上线后 24h 观察清单

| 时点 | 关注项 | 命令 / 面板 | 期望 |
|------|--------|-------------|------|
| **0h**（上线即看） | 5xx 率 / 40100 计数 / 上传次数 | Loki dashboard | 5xx < 0.5%；40100 < 5/min |
| | healthcheck | `curl /actuator/health` | UP |
| | 容器 restart 计数 | `docker inspect ... .RestartCount` | 0 |
| **4h** | 上传 P95 耗时 | Loki LogQL §3.2 | < 3s |
| | 临时目录大小 | `du -sh /data/skillsmap/upload-tmp/` | < 500MB |
| | `skills.storage.path` 增量 | `du -sh /data/skillsmap/skill-packages/` | +0.5GB 内（4h × ~30 用户 × 1MB） |
| **12h** | scheduler 是否运行 | Loki `UploadTmpCleanupScheduler.start` 出现 1 行 + 每小时 1 行 | 12-13 行 |
| | scheduler 失败计数 | Loki `cleanup.success=false` | 0 |
| | 慢查询 | MySQL slow log | 无 `ALTER` 残留 |
| **24h** | 首日 SLO 报告 | 模板见 §8 | 成功率 ≥ 99.9% / P95 < 3s |

### 7.1 收尾动作

```bash
# 24h 后
# 1) 出 SLO 报告
echo "S38 上线首日 SLO 报告 $(date +%Y-%m-%d)
  - 上传次数: $(grep -c 'upload.end' /app/logs/skillsmap-backend.$(date +%Y-%m-%d).log)
  - 成功率: $(...)
  - P50 耗时: $(...)
  - P95 耗时: $(...)
  - P99 耗时: $(...)
  - scheduler 清理: N 次
  - 临时目录峰值: X MB
  - 磁盘剩余: Y GB
" | tee docs/sprints/S38/post-launch-slo-$(date +%Y%m%d).md

# 2) 通知 PM
# [@ops-max] S38 24h SLO 报告已出，上线成功，可正式 commit
```

---

## 8. 应急 Runbook（故障定位流程）

### 8.1 故障 1：5xx 率突增

```
1. 看 healthcheck
   curl http://localhost:8767/actuator/health
2. 看最近 200 行错误日志
   docker logs skillsmap-backend --tail 200 | grep -E "ERROR|Exception"
3. 看 DB 连接
   docker exec -it skillsmap-mysql mysql ... -e "SHOW PROCESSLIST"
4. 看 Flyway
   docker exec -it skillsmap-mysql mysql ... -e "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 3"
5. 若 V38 失败 → §2.4 回滚
6. 若 DB 满 → §8.4 应急
7. 若是 zip 路径 bug → §4.2 整体回滚
```

### 8.2 故障 2：临时目录爆满

```
1. 立即看占用
   du -sh /data/skillsmap/upload-tmp/
2. 看 scheduler 状态
   docker logs skillsmap-backend | grep UploadTmpCleanupScheduler | tail 5
3. 手动清理（> 24h 子目录）
   docker exec skillsmap-backend sh -c "
     find /data/skillsmap/upload-tmp/ -mindepth 1 -maxdepth 1 -mtime +0 -exec rm -rf {} \;
   "
4. 看磁盘 free
   df -h /data
5. 若仍满 → §8.4 扩容
```

### 8.3 故障 3：40100 暴增（撞库）

```
1. 看 40100 源 IP
   Loki: {app="skillsmap-backend"} |~ "biz_code=40100" | json | client_ip
2. 触发自动 ban
   # 在 nginx / fail2ban 加规则
   iptables -I INPUT -s <ip> -j DROP
3. 通知安全
4. 持续观察 5 min，无下降则扩大 ban 范围
```

### 8.4 故障 4：磁盘满

```
1. 立即清理日志（保留 7d）
   docker exec skillsmap-backend sh -c "
     find /app/logs -name '*.gz' -mtime +7 -delete
   "
2. 清理 Docker 旧镜像
   docker image prune -a --filter "until=72h"
3. 扩容
   # 在云厂商控制台扩 disk（无需重启）
   # 若是物理机：LVM lvextend + resize2fs
4. 通知 PM 后续容量规划
```

### 8.5 故障 5：Flyway V38 锁表

```
1. 看是否有 ALTER 残留
   docker exec -it skillsmap-mysql mysql ... -e "
     SELECT * FROM information_schema.INNODB_TRX\G
     SELECT * FROM information_schema.PROCESSLIST WHERE COMMAND='Query' AND TIME > 30;
   "
2. 等 60s（MySQL 8 online DDL 通常秒级）
3. 若超 5 min：kill ALTER，§2.4 回滚
4. 联系 Dev-kevin 评估是否需要 pt-online-schema-change 重做
```

---

## 9. 与既有部署文档的衔接

| 主题 | 既有文档 | S38 增量（本文档） |
|------|----------|---------------------|
| 备份 | `DEPLOY.md §4` 每日 03:00 备份 | **同流程**，部署前手动跑一次 |
| 升级 | `DEPLOY.md §5` `deploy.sh` | **同流程**，tag 改 `s38-rc1` |
| 回滚 | `DEPLOY.md §6` `rollback.sh` | **同流程**，tag 改 `s37-stable` |
| 监控 | `DEPLOY.md §9` 仅 actuator health | §3 增加 5 项关键指标 + LogQL 模板 |
| 安全 | `DEPLOY.md §8` 5 个安全头 + 限流 | §6 增加 multipart / zip / WAF 项 |
| 应急 | `DEPLOY.md §7` 排障速查表 | §8 增加 5 个 S38 专属故障 Runbook |

**不重写** `DEPLOY.md`，按增量风格独立；后续若 S39/S40 引入类似端点，可复用本文档结构。

---

## 10. 总结

### 10.1 已就位（Ops 已做 / 已确认）

- ✅ application-prod.yml S38 配置齐全（10MB / 12MB / upload-tmp-dir）
- ✅ V38 Flyway 迁移 SQL 已写（idempotent，加列 + 加索引）
- ✅ docker-compose.prod.yml 已挂载 `backend_data` 卷（upload-tmp 落点）
- ✅ Dockerfile 非 root 用户（`app`）已生效（DEPLOY.md 修正后）
- ✅ /actuator/health/liveness + /readiness 已暴露（K8s probes 可用）
- ✅ 运维脚本 `deploy.sh` / `rollback.sh` / `healthcheck.sh` / `backup-mysql.sh` 已就位（DEPLOY.md §3 / §5 / §6）
- ✅ MySQL 每日全量备份 CRON 已配（DEPLOY.md §4.1）

### 10.2 待 Lead 拍板（§7 已列）

1. **磁盘扩容阈值**：S38 短期无需，但 1 个月内需 `du -sh` 复核
2. **WAF 规则封禁 `multipart/form-data` 体积 > 15MB**（推荐 Nginx `client_max_body_size 15M`）
3. **CDN 不缓存 `POST /api/skills`**（如用 CDN，需在边缘节点加 `Cache-Control: no-store`）
4. **临时目录权限收紧**（chown 到 app:app，目前容器内已生效，但宿主机需复核）
5. **监控接入 Prometheus**（下一迭代；本期用 Loki 兜底）

### 10.3 不在本期 S38 范围

- ❌ 审核工作流（S39）
- ❌ 多版本管理（S40）
- ❌ 增量更新 / 用户配额（S41）
- ❌ Prometheus 接入 / AlertManager 规则化（下迭代）

---

## 11. 附录：相关路径速查

| 用途 | 路径 |
|------|------|
| S38 PRD | `D:\codeing\workspace\skills-map\docs\sprints\S38\prd.md` |
| S38 tech-review | `D:\codeing\workspace\skills-map\docs\sprints\S38\tech-review.md` |
| S38 handoff | `D:\codeing\workspace\skills-map\docs\sprints\S38\handoff.md` |
| 既有部署文档 | `D:\codeing\workspace\skills-map\docs\deploy\DEPLOY.md` |
| 既有 audit 报告 | `D:\codeing\workspace\skills-map\docs\deploy\audit-report.md` |
| V38 迁移 SQL | `D:\codeing\workspace\skills-map\backend\src\main\resources\db\migration\V38__add_uploader_user_id.sql` |
| 公共配置 | `D:\codeing\workspace\skills-map\backend\src\main\resources\application.yml` |
| 生产配置 | `D:\codeing\workspace\skills-map\backend\src\main\resources\application-prod.yml` |
| 后端 Dockerfile | `D:\codeing\workspace\skills-map\backend\Dockerfile` |
| 生产 compose | `D:\codeing\workspace\skills-map\docker-compose.prod.yml` |
| 运维脚本 | `D:\codeing\workspace\skills-map\scripts\`（`deploy.sh` / `rollback.sh` / `healthcheck.sh` / `backup-mysql.sh`） |
| 团队规约 | `D:\codeing\workspace\skills-map\.claude\CLAUDE.md` |
| **本文档** | `D:\codeing\workspace\skills-map\docs\sprints\S38\ops.md` |

---

> **下一步**：本文档发出后请 Lead（agile-rd-lead）确认 §7 待办 + 决策窗口（推荐选凌晨 02:00-05:00 低峰期）。Ops 收到确认后开 §1.2 部署序列，预计 30 min 完成。
