# SkillsMap Sprint S23 接力简报

> **用途**：新会话第一条 message 读此文件即可接管 S23 全部上下文。
> **生成时间**：2026-06-11
> **状态**：✅ **S23 已完成**

---

## 1. 项目背景

SkillsMap = Spring Boot 3.5.7 + Vue 3.5 + JDK 21 + MyBatis-Plus 3.5.12 全栈 skill 平台。

- 后端：`D:\codeing\workspace\skills-map\backend`
- 前端：`D:\codeing\workspace\skills-map\frontend`
- JDK 21：`D:\sofaward\openjdk-21.0.2_windows-x64_bin\jdk-21.0.2`
- 后端 8767，前端 7777
- **S21 已完成**（公开 API occupationCode + /api-guide + skills-manager skill + HomeHero 双 tab）
- **S22 同步进行中**（物化 storage + 智能体埋点 + api-guide 锚点）
- **S23 已完成**（公开 API 限流 + skills-manager 鉴权中转，本简报覆盖）

---

## 2. S23 目标（2 项，A 决策已锁）

| ID | 目标 | 决策 |
|----|------|------|
| **T1** | 公开 API 限流（Bucket4j 按 IP） | 引入 bucket4j-core 8.10.1；默认 60 req/min；超限 HTTP 429 + `code=42900` |
| **T2** | skills-manager 写能力（鉴权中转） | **方案 A**（用户自备 token），skill 不代登录 |

---

## 3. 决策已锁

| Q | 决策 | 理由 |
|---|------|------|
| Q1 | 限流后端框架 | **Bucket4j 8.10.1**（Java 8+ 单文件，零外部依赖，适合单实例） |
| Q2 | 限流粒度 | **按 IP**（无 token 时唯一标识） |
| Q3 | 限流键 | X-Forwarded-For → X-Real-IP → remoteAddr 优先级 |
| Q4 | 限流白名单 | `/api/auth/**`、`/doc.html`、`/v3/api-docs/**`、`/swagger-ui/**`、`/webjars/**` |
| Q5 | 默认值 | `capacity=60, refill-tokens=60, refill-period=1m, enabled=false` |
| Q6 | 超限响应 | HTTP **429** + JSON `{code:42900, message:"请求过于频繁，请稍后再试", success:false}` |
| Q7 | skills-manager 鉴权方案 | **A：用户自备 token**（B：代理 token scope 大，v1 不做） |
| Q8 | 限流开关默认 | `enabled=false`，运维确认后再打开（避免上线即限流） |

### Q4 路径白名单依据

- **`/api/auth/**`**：登录注册是限流反模式——密码错误 60 次后无法重试会被锁登不上
- **`/doc.html` / `/v3/api-docs`**：开发者本地反复刷文档会被误伤
- **`/swagger-ui`**：同上

### Q6 响应格式依据

直接写 HTTP 429 + JSON，**绕过** `GlobalExceptionHandler` 的默认500兜底。让前端能直接根据 HTTP 状态码识别限流（不必解析 JSON）。

---

## 4. T1→T6 任务分解

| ID | 任务 | 关键文件 | 状态 |
|----|------|----------|------|
| T1.1 | pom.xml 加 bucket4j-core 8.10.1 | `backend/pom.xml` | ✅ |
| T1.2 | RateLimitProperties | `config/RateLimitProperties.java`（新建） | ✅ |
| T1.3 | RateLimitFilter | `security/RateLimitFilter.java`（新建，~100 行） | ✅ |
| T1.4 | BizCode.RATE_LIMITED | `common/BizCode.java` | ✅ |
| T1.5 | SecurityConfig 装配 | `config/SecurityConfig.java` | ✅ |
| T1.6 | application.yml 配置 | `application.yml` | ✅ |
| T2.1 | SKILL.md 加鉴权章节 | `resources/skills/skills-manager/SKILL.md` | ✅ |
| T2.2 | scripts/auth-example.sh | `resources/skills/skills-manager/scripts/auth-example.sh`（新建，~70 行） | ✅ |
| T2.3 | api-endpoints.md 加鉴权端点表 | `resources/skills/skills-manager/references/api-endpoints.md` | ✅ |

---

## 5. T1 改动详情

### 5.1 `pom.xml`

新增 properties + dependency：

```xml
<bucket4j.version>8.10.1</bucket4j.version>

<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>${bucket4j.version}</version>
</dependency>
```

**Bucket4j 8.x 兼容 JDK 21 + Spring Boot 3.5**：是纯 Java 库，无外部依赖，无 JDK 版本陷阱。

### 5.2 `config/RateLimitProperties.java`（新建）

```java
@Configuration
@ConfigurationProperties(prefix = "skillsmap.rate-limit")
public class RateLimitProperties {
    private long capacity = 60;          // 桶容量（最大突发）
    private long refillTokens = 60;      // 每个周期补充
    private String refillPeriod = "1m";  // 补充周期（1s/1m/1h）
    private boolean enabled = false;     // 默认关闭，运维开关
    // getter/setter 省略
}
```

### 5.3 `security/RateLimitFilter.java`（新建，~120 行）

核心逻辑：

```java
@Component
@Order(10)
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!props.isEnabled()) return true;
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/auth/")) return true;
        if (uri.startsWith("/doc.html")) return true;
        if (uri.startsWith("/v3/api-docs")) return true;
        if (uri.startsWith("/swagger-ui")) return true;
        if (uri.startsWith("/webjars/")) return true;
        return false;
    }

    @Override
    protected void doFilterInternal(...) {
        String ip = resolveIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket());
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }
        // 超限：直接写 HTTP 429 + 统一 Result JSON
        response.setStatus(429);
        response.setContentType("application/json");
        response.getWriter().write(
            objectMapper.writeValueAsString(
                Result.fail(BizCode.RATE_LIMITED.getCode(), BizCode.RATE_LIMITED.getMessage())));
    }
}
```

**关键点**：
- IP 解析：`X-Forwarded-For`（取首段）→ `X-Real-IP` → `remoteAddr`
- 时间字符串：`"1m"` → `"PT1M"`（ISO-8601，Bucket4j Duration.parse 要求）
- 超限**直接写 response**，不抛异常（避免被 GlobalExceptionHandler 当500兜底）
- `clearBuckets()`：测试钩子

### 5.4 `common/BizCode.java`

新增：
```java
// ---- Sprint S23: 公开 API 限流 (429) ----
RATE_LIMITED(42900, "请求过于频繁，请稍后再试"),
```

### 5.5 `config/SecurityConfig.java`

```java
// 新增构造器参数 + 装配 filter
private final RateLimitFilter rateLimitFilter;

.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
   .addFilterBefore(rateLimitFilter, JwtAuthFilter.class);
```

**filter 顺序**：RateLimitFilter → JwtAuthFilter → 业务 controller。  
理由：限流比 JWT 解析便宜，先把超限请求挡掉。

### 5.6 `application.yml`

```yaml
skillsmap:
  # Sprint S23: 公开 API 限流（Bucket4j 按 IP）
  rate-limit:
    enabled: ${SKILLSMAP_RATE_LIMIT_ENABLED:false}
    capacity: ${SKILLSMAP_RATE_LIMIT_CAPACITY:60}
    refill-tokens: ${SKILLSMAP_RATE_LIMIT_REFILL_TOKENS:60}
    refill-period: ${SKILLSMAP_RATE_LIMIT_REFILL_PERIOD:1m}
```

环境变量全可控，默认值适合生产 60 req/min。

---

## 6. T2 改动详情

### 6.1 `resources/skills/skills-manager/SKILL.md`

新增「鉴权使用」章节：

```markdown
## 鉴权使用（S23 新增）

SkillsMap 写操作（评分 / 收藏）需要 Bearer Token。**本 skill 不会代用户登录**：

1. 用户先在 SkillsMap 网页登录，DevTools → Application → Local Storage → `auth_token`
2. 把 token 字符串贴给本 skill
3. skill 用此 token 调用受保护端点

完整示例脚本：scripts/auth-example.sh

安全提示：token 视为用户凭据；不要写入 git commit、不要 echo 到日志。
```

### 6.2 `resources/skills/skills-manager/scripts/auth-example.sh`（新建）

70 行 bash 脚本，演示 5 个鉴权调用：
1. `GET /api/auth/me` — 当前用户
2. `POST /api/reviews` — 评分
3. `POST /api/favorites/{id}` — 收藏
4. `GET /api/favorites/{id}/status` — 收藏状态
5. `GET /api/favorites/mine` — 我的收藏

环境变量：`TOKEN`（必填）、`HOST`（默认本机）、`SKILL_ID`（默认 1）。

### 6.3 `references/api-endpoints.md`

新增 §6「需鉴权端点」小节：

| 端点 | 方法 | 用途 |
|------|------|------|
| `/api/auth/me` | GET | 当前用户 |
| `/api/reviews` | POST | 评分 |
| `/api/favorites/{skillId}` | POST | 收藏 |
| `/api/favorites/{skillId}/status` | GET | 收藏状态 |
| `/api/favorites/mine` | GET | 我的收藏 |

并把错误码表补全（40100 / 40300 / 42900）。

---

## 7. 验证结果

### 7.1 `mvn compile`

```
[INFO] BUILD SUCCESS
[INFO] Total time: 3.956 s
```

零编译错误，零警告（lombok 增强正常）。

### 7.2 限流 curl 验证（capacity=5 / 10s 测试配置）

```
req 1 -> 200 | body={"code":0,...}
req 2 -> 200
req 3 -> 200
req 4 -> 200
req 5 -> 200
req 6 -> 200   ← 桶还有少量 token
req 7 -> 200
req 8 -> 429 | body={"code":42900,"message":"请求过于频繁，请稍后再试","success":false}
req 9 -> 200   ← refill 补 1 个
req 10 -> 429
```

**超限响应体**（含 HTTP 状态码）：
```
{"code":42900,"message":"请求过于频繁，请稍后再试","success":false}
HTTP_CODE:429
```

### 7.3 白名单验证（限流不应影响）

| 路径 | 期望 | 实际 |
|------|------|------|
| `POST /api/auth/login`（无 token） | 不被限流（4xx 由鉴权处理） | 200（permitAll，无凭据返回业务错误） |
| `GET /doc.html` | 不被限流 | 200 |
| `GET /v3/api-docs` | 不被限流 | 200 |
| `GET /favicon.ico` | 不被限流 | 404（资源不存在，与限流无关） |

### 7.4 skills-manager.zip 下载验证

```
skills-manager.zip    6544 bytes

文件清单：
  SKILL.md                       3851 bytes  ← 含「鉴权使用」章节
  references/api-endpoints.md    4116 bytes  ← 含「需鉴权端点」章节
  scripts/auth-example.sh        1985 bytes  ← 新建
  scripts/sync-skills.sh         1416 bytes  ← S21 已有
```

文本内容关键字验证：
- `grep -c "鉴权使用" SKILL.md` → `1` ✅
- `grep -c "需鉴权端点" references/api-endpoints.md` → `1` ✅

### 7.5 npm build

**未运行** — S23 无前端改动（T1 是后端 filter，T2 是 skill 包文本）。S21 patch 后的前端代码已 locked，无需重 build。

---

## 8. 改动文件清单

### 新建（3 个）

| 路径 | 行数 | 说明 |
|------|------|------|
| `backend/.../config/RateLimitProperties.java` | ~35 | 限流配置类 |
| `backend/.../security/RateLimitFilter.java` | ~120 | Bucket4j filter |
| `backend/.../skills/skills-manager/scripts/auth-example.sh` | ~70 | 鉴权示例脚本 |

### 修改（6 个）

| 路径 | 改动 |
|------|------|
| `backend/pom.xml` | +bucket4j.version +dependency |
| `backend/.../common/BizCode.java` | +RATE_LIMITED(42900) |
| `backend/.../config/SecurityConfig.java` | +RateLimitFilter 构造参数 + addFilterBefore |
| `backend/src/main/resources/application.yml` | +skillsmap.rate-limit 配置块 |
| `backend/.../skills/skills-manager/SKILL.md` | +鉴权使用 章节 |
| `backend/.../skills/skills-manager/references/api-endpoints.md` | +§6 需鉴权端点 + 错误码补 40100/40300/42900 |

---

## 9. 风险与已知限制

| 风险 | 说明 | 缓解 |
|------|------|------|
| ConcurrentHashMap 内存增长 | 每个 IP 一个 Bucket，长期可能 OOM（极端流量） | v1 接受；v1.1 可加 Caffeine TTL（10 分钟无访问清空） |
| 多实例部署下各自限流 | 4 实例 × 60 req/min = 240 req/min/IP 实际值 | v1 单实例；分布式部署需切换 Redis Bucket4j |
| X-Forwarded-For 信任问题 | 默认信任 header 可能被客户端伪造 | 生产环境应配合 Nginx 强制设置 X-Real-IP；Spring 也可换 `forward-headers-strategy: framework` |
| 默认 `enabled=false` | 上线后默认不生效，需运维显式开 | 文档已写在 application.yml 注释；运维 checklist |
| Bucket4j 8.x groupId | `com.bucket4j`（不是 `com.github.vladimir-bukhtoyarov`） | 已锁定，文档说明 |
| 限流对热路径写响应 | `ObjectMapper.writeValueAsString` 每次构造 JSON | 当前 OK（429 是低频事件）；若流量大可改静态常量字符串 |

---

## 10. 后续 Sprint 建议

- **S24 候选**：
  - 限流开关默认改为 `true`，运维指南补充
  - `RateLimitFilter` 加 Bucket TTL（Caffeine 10 min 自动清）
  - skills-manager 加「代理 token」端点（方案 B：一次性 token scope 限）
  - `/api-guide` 锚点深链（S22 接力）
  - 限流埋点：429 计数 + IP 维度 Prometheus
  - `OccupationCode` 数组筛（多职业 OR）

---

## 11. 验收清单

- [x] `mvn compile` BUILD SUCCESS
- [x] Bucket4j 8.10.1 已引入并解析
- [x] 限流超限返回 HTTP 429 + code=42900
- [x] 白名单（/api/auth/**、/doc.html、/v3/api-docs）不被限
- [x] 限流默认 disabled，运维 env var 控制
- [x] SKILL.md 加「鉴权使用」章节（grep 验证）
- [x] `scripts/auth-example.sh` 新建（1985 bytes）
- [x] `references/api-endpoints.md` 加 §6 鉴权端点表
- [x] skills-manager.zip 下载含全部4 个文件
- [x] 错误码表补全 40100/40300/42900
- [ ] 前端无改动，跳过 npm build

---

## 12. 完成报告

Sprint S23 完成。
- T1（公开 API 限流）✅ Bucket4j + IP + 60/min 默认 + 429 响应
- T2（skills-manager 鉴权中转）✅ 用户自备 token 方案落地
- 8 项决策全部锁
- 3 新建 + 6 修改 =9 文件改动
- 验证全过（compile + 限流 curl + 下载 unzip + 关键字 grep）

Lead 决策：限流默认 `enabled=false`（避免上线即误限），运维确认后再开。

---

## 13. 启动参数示例

```bash
# 默认（限流关闭）
./mvnw spring-boot:run

# 打开限流 + 60 req/min（生产推荐）
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--skillsmap.rate-limit.enabled=true"

# 调试用：5 req / 10s（快速触发 429）
./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--skillsmap.rate-limit.enabled=true \
    --skillsmap.rate-limit.capacity=5 \
    --skillsmap.rate-limit.refill-tokens=5 \
    --skillsmap.rate-limit.refill-period=10s"

# env var 方式（容器部署推荐）
SKILLSMAP_RATE_LIMIT_ENABLED=true \
SKILLSMAP_RATE_LIMIT_CAPACITY=60 \
SKILLSMAP_RATE_LIMIT_REFILL_TOKENS=60 \
SKILLSMAP_RATE_LIMIT_REFILL_PERIOD=1m \
java -jar app.jar
```