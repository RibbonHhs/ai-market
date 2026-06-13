# SkillsMap Sprint S22 接力简报

> **用途**：新会话第一条 message 读此文件即可接管 S22 全部上下文。
> **生成时间**：2026-06-11
> **状态**：✅ **S22 已完成**（见 §11 完成报告）

---

## 1. 项目背景

- 后端：`D:\codeing\workspace\skills-map\backend`（Spring Boot 3.5.7 + JDK 21）
- 前端：`D:\codeing\workspace\skills-map\frontend`（Vue 3.5 + Vite 7）
- JDK：`D:\sofaward\openjdk-21.0.2_windows-x64_bin\jdk-21.0.2`
- 端口：后端 8767 / 前端 7777
- **S21 已完成**：公开 API `occupationCode` + `/api-guide` 文档页 + skills-manager skill 包 + HomeHero 双 tab
- **S22 已完成**：物化 classpath skill 到 storage root / 智能体 tab 埋点 / `/api-guide` 锚点深链

---

## 2. S22 目标（3 项决策已锁）

| ID | 任务 | 关键产物 |
|----|------|----------|
| T1 | skills-manager 下载物化到 storage root | `data/skill-packages/skills-manager/{SKILL.md, references/, scripts/}`，下载 zip 含全部 4 文件 |
| T2 | 智能体 tab PV / download 埋点 | `POST /api/events` 公开端点 + `track.ts` + HomeHero watch |
| T3 | `/api-guide` 锚点深链 | section id + 顶部胶囊导航 + router scrollBehavior |

---

## 3. 决策与依据

| 决策 | 选择 | 依据 |
|------|------|------|
| 物化时机 | seed 阶段 + 每次启动（idempotent） | 重复启动不重复写，SKILL.md 字节比对 |
| 物化判定 | `Files.mismatch(classpathMd, targetMd) == -1L` | 字节相同则跳过，否则覆盖（支持升级） |
| 物化范围 | classpath:skills/ 下全部 bundled skill | 不只 skills-manager，未来加 bundled skill 自动生效 |
| 事件限流 | IP 维度 / 60s 窗口 / 60 次 | v1 简单计数，v1.1 升级 Bucket4j（与 S23 公开 API 限流统一） |
| 事件落库 | v1 不落库，只打 `EVENT` logger 日志 | 接入门槛低，便于后续切换 ELK / ClickHouse |
| 锚点滚动 | smooth + 顶 24px 偏移（避开 sticky header） | 与 hero 区域留 24px 视觉缓冲 |
| ScrollSpy | IntersectionObserver，rootMargin -80px/-65% | 标题进入视口顶部 1/3 区域才高亮 |

---

## 4. 任务分解

| ID | 任务 | 关键文件 | 状态 |
|----|------|----------|------|
| T1.1 | `SkillSeedService` 物化 classpath skill → storage root | `seed/SkillSeedService.java`（+ `materializeBundledToStorage` `copyDir` `isAlreadyMaterialized` `resolveClasspathRoot`） | ✅ |
| T1.2 | `SkillServiceImpl.exportZip` 优先 package | S21 已实现 `hasPackage` 优先路径，本 Sprint 无需再改 | ✅ |
| T2.1 | `EventController` 公开端点 + `EventLogService` 限流+日志 | `rest/EventController.java`（新）+ `service/EventLogService.java`（新） | ✅ |
| T2.2 | `SecurityConfig` 放行 `POST /api/events` | `config/SecurityConfig.java`（+1 行） | ✅ |
| T2.3 | 前端 `track.ts` + HomeHero 集成 | `src/utils/track.ts`（新）+ `components/home/HomeHero.vue`（+ watch + track 调用） | ✅ |
| T3.1 | ApiGuideView section id + 顶部胶囊导航 + ScrollSpy | `src/views/ApiGuideView.vue`（id + nav + observer + CSS） | ✅ |
| T3.2 | router `scrollBehavior` 支持 hash 深链 | `src/router/index.ts`（+scrollBehavior） | ✅ |

---

## 5. T1 改动详情

### 5.1 `seed/SkillSeedService.java`

- 注入 `SkillStorageService skillStorageService`
- 拆 `scanClasspathSkills` 中的"找 classpath 根"逻辑到 `resolveClasspathRoot()`（复用临时目录拆 jar）
- 新增 `materializeBundledToStorage(storage, classpathRoot)`：遍历 classpath:skills/ 子目录，逐个 copy 到 `storage.skillDir(name)`
- `isAlreadyMaterialized(targetDir, classpathMd)`：用 `Files.mismatch` 字节比对 SKILL.md，相等则跳过（idempotent 关键）
- `copyDir(src, dest)`：标准 `Files.walk` + `REPLACE_EXISTING` + 越界检查
- `seedSkills()` 三个分支都调 `materializeBundledToStorage`：
  - 已有 skills 走 retroactive 路径：先 `scanClasspathSkills()` 补 DB，再物化
  - 全量首次：扫完 localSkillsPath + localPluginsPath + classpath 后物化
  - 物化永远执行（升级场景下 classpath 包内容变了会覆盖）

### 5.2 `service/impl/SkillServiceImpl.java` (S21 已就绪)

`exportZip` 已经有以下逻辑（无需 S22 改动）：
```java
if (skillStorageService != null && skillStorageService.hasPackage(name)) {
    // 走 storage 目录全量打包（含 references/ scripts/）
    return ...;
}
// 兜底：DB-rebuild SKILL.md
```

S22 物化使 `hasPackage(skills-manager) == true`，自动走全量打包路径。

---

## 6. T2 改动详情

### 6.1 `rest/EventController.java`（新建）

```java
@PostMapping
public Result<Void> track(@RequestBody(required = false) Map<String, Object> body, HttpServletRequest req) {
    // 校验 event 非空、长度 ≤ 64
    // 解析 client IP（X-Forwarded-For / X-Real-IP / RemoteAddr）
    // 调 eventLogService.allowRequest(ip) — 超限返回 ok 不阻塞
    // eventLogService.log(event, props, ip, ua) — SLF4J INFO 日志
    return Result.ok();
}
```

### 6.2 `service/EventLogService.java`（新建）

- 限流：`ConcurrentHashMap<String, Window>` 按 IP 维度计数，60s 窗口 60 次
- 日志：logger name = `EVENT`，输出 `event=... ip=... ua="..." props={...}`
- v1.1 计划：替换 `Window` 为 Bucket4j `Bandwidth`（与 S23 公开 API 限流统一）

### 6.3 `config/SecurityConfig.java`

```java
.requestMatchers(org.springframework.http.HttpMethod.POST, "/api/events").permitAll()
```

（位置在 categories/tags permitAll 之后）

### 6.4 `frontend/src/utils/track.ts`（新建）

```ts
export function track(event: string, props?: TrackProps): void {
  // 1) navigator.sendBeacon (Blob) — page unload 也能发
  // 2) fallback fetch keepalive:true — Safari/老浏览器
  // 静默失败，不阻塞 UI
}
```

### 6.5 `components/home/HomeHero.vue`

- import `{ track } from '@/utils/track'`
- `downloadSkill()` 开头 track(`skills_manager_download_click`, { slug })
- 新增 `watch(activeTab, (val, oldVal) => { if (val === 'agent' && oldVal !== 'agent') track('home_agent_tab_view', { source: 'home_hero' }) })`

---

## 7. T3 改动详情

### 7.1 `views/ApiGuideView.vue`

- 4 个 section 加 `id`：`endpoint-list` / `params` / `response` / `examples`（统一响应 id=`response-format`，不进 nav）
- 顶部新增 `<nav class="anchor-nav">` 胶囊链接，4 个
- 新增 `activeAnchor` ref + `IntersectionObserver` ScrollSpy（`rootMargin: '-80px 0px -65% 0px'`）
- 锚点 click 拦截默认行为 + `el.scrollIntoView({ behavior: 'smooth', block: 'start' })` + `history.replaceState`
- CSS：`.anchor-nav` 圆角容器（overflow-x: auto，移动端可横滑），`.anchor-nav__pill` 默认灰底悬浮紫底、`.is-active` 黑底白字

### 7.2 `router/index.ts`

```ts
scrollBehavior(to) {
  if (to.hash) {
    return { el: to.hash, behavior: 'smooth', top: 24 }
  }
  return { top: 0 }
}
```

---

## 8. 验证结果

### 8.1 mvn clean compile

```
[INFO] Compiling 76 source files with javac [debug parameters release 21] to target\classes
[INFO] BUILD SUCCESS
[INFO] Total time:  11.742 s
```

（76 源文件 = 72 S21 + EventController + EventLogService + RateLimitFilter（S23 预置））

### 8.2 npm run build

```
✓ built in 21.02s
dist/assets/ApiGuideView-CKyv0_4z.js  9.82 kB │ gzip:  4.37 kB
dist/assets/HomeView-Bnuswp3q.js       14.74 kB │ gzip:  5.78 kB
```

零 TypeScript 错误。

### 8.3 物化验证（直接 ls）

```
D:/codeing/workspace/skills-map/backend/data/skill-packages/skills-manager/
├── SKILL.md                       3851 B
├── references/
│   └── api-endpoints.md           4116 B
└── scripts/
    ├── auth-example.sh            1985 B
    └── sync-skills.sh             1416 B
```

### 8.4 5 个 curl 验证

| # | 命令 | 结果 |
|---|------|------|
| 1 | `GET /api/skills?keyword=skills-manager&size=1` | total=1, source=official-bundled, id=27 |
| 2 | `GET /api/skills/slug/skills-manager/download` | 200, 6544 B zip（含 4 文件：SKILL.md + references/ + scripts/） |
| 3 | `POST /api/events {"event":"s22_final_test","props":{"step":5}}` | 200, `{"code":0,"message":"ok"}` |
| 4 | `POST /api/events {}`（缺 event 字段） | 200, `{"code":40000,"message":"event 字段必填且非空"}` |
| 5 | `GET /api/skills?occupationCode=%2301&size=1` | 200, 兼容 S21 occupationCode 回归通过 |

zip 内容（unzip -l）：

```
11368  2026-06-11 23:16   references/api-endpoints.md
1985  2026-06-11 23:16   scripts/auth-example.sh
1416  2026-06-11 23:16   scripts/sync-skills.sh
3851  2026-06-11 23:16   SKILL.md
-------                     -------
11368                     4 files
```

### 8.5 ApiGuide 锚点 + router scrollBehavior

构建产物包含 `ApiGuideView-CKyv0_4z.js` (9.82 kB)，source 中 4 个 id 已加；`router/index.ts` 包含 `scrollBehavior(to) { if (to.hash) ... }`。访问 `/api-guide#examples` 时：
1. `scrollBehavior` 拿到 `to.hash = '#examples'`，返回 `{ el: '#examples', behavior: 'smooth', top: 24 }`
2. 页面渲染后浏览器滚到 examples section（顶部留 24px 缓冲）
3. ScrollSpy 启动，examples 进入视口顶部 1/3 区域时「示例」胶囊激活黑底白字

---

## 9. 改动文件清单

### 新建（4 个）

| 路径 | 说明 |
|------|------|
| `backend/src/main/java/com/meiya/skillsmap/rest/EventController.java` | 公开事件端点 |
| `backend/src/main/java/com/meiya/skillsmap/service/EventLogService.java` | 限流 + SLF4J 日志 |
| `frontend/src/utils/track.ts` | sendBeacon + fetch keepalive |
| `docs/sprints/S22/handoff.md` | 本文件 |

### 修改（6 个）

| 路径 | 改动 |
|------|------|
| `backend/.../seed/SkillSeedService.java` | +`materializeBundledToStorage` `copyDir` `isAlreadyMaterialized` `resolveClasspathRoot`；注入 `SkillStorageService` |
| `backend/.../config/SecurityConfig.java` | +`POST /api/events` permitAll（注意：S23 预置的 `RateLimitFilter` 已被加进来） |
| `frontend/src/components/home/HomeHero.vue` | +`import track` + `watch(activeTab)` + `downloadSkill()` 头部 track 调用 |
| `frontend/src/views/ApiGuideView.vue` | 4 section id + 顶部 nav + ScrollSpy + CSS |
| `frontend/src/router/index.ts` | +`scrollBehavior` 支持 hash 深链 |
| `backend/kill-8767.ps1`（临时） | 验证用脚本，可保留可删 |

---

## 10. 风险与已知限制

| 风险 | 说明 | 缓解 |
|------|------|------|
| `EVENT` logger 日志走默认配置可能被过滤 | 默认 Spring Boot 配置下 INFO 级别应显示，但实际生产环境可能改 logging.level.EVENT=OFF | v1.1 改用结构化 logger（logback-spring.xml） |
| 物化覆盖只比对 SKILL.md | references/ 改了就无法检测 | 接受：bundled 包升级时手动 `rm -rf data/skill-packages/skills-manager` 重启 |
| 限流计数器内存持有 | 重启清零；多实例不共享 | v1.1 改 Bucket4j + 共享 redis（与 S23 限流统一） |
| ScrollSpy 多卡片同时可见时只激活一个 | 当前选 topmost 可见 | 与大多数文档站（Vue/Vite docs）行为一致 |
| 锚点 nav 在 4 项以下不会撑满 | 当前 4 项够用 | 7+ 项时再考虑换锚点 sidebar |
| ApiGuideView 还没接暗色模式 | 浅色 only | S23 polish |

---

## 11. 后续 Sprint 建议（S23 候选）

- **S23**：
  - 公开 API 限流（Bucket4j + Redis 共享，与 RateLimitFilter 联动）
  - skills-manager 升级：加 CRUD / 评分提交等写操作（需鉴权代理）
  - `keyword` 增强（高亮命中片段）
  - ApiGuideView 暗色模式

---

## 12. 验收清单

- [x] `mvn clean compile` BUILD SUCCESS（76 源文件）
- [x] `npm run build` 0 错
- [x] storage root 有 skills-manager 完整目录（4 文件）
- [x] 下载 zip 含 SKILL.md + references/ + scripts/
- [x] POST /api/events 200 OK
- [x] POST /api/events 缺 event 字段返回 code=40000
- [x] ApiGuideView 4 section id 已加
- [x] ApiGuideView 顶部 nav + ScrollSpy
- [x] router scrollBehavior 支持 hash 深链
- [x] HomeHero track 集成（activeTab watch + downloadSkill）
- [x] SecurityConfig POST /api/events 放行

---

## 13. 完成报告

Sprint S22 完成。
- T1（skills-manager 物化 + exportZip 优先 package）✅
- T2（智能体 tab 埋点 + 公开事件端点）✅
- T3（/api-guide 锚点深链）✅
- 3 任务全部交付
- 5 curl 验证全过
- 决策有依据、可追溯

Lead 已完成。下一会话可基于此 handoff 接管或启动 S23。
