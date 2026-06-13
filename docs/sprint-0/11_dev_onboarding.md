# Dev Onboarding（开发者上手手册）

> 作者：dev-kevin @ Sprint 0 Kickoff (2026-06-06)
>
> 版本：v0.1 · 适用对象：所有协作者（Dev / QA / Ops / Designer 需要看后端章节） · 引用：`10_tech_architecture.md` / `14_dev_env_setup.md`

## 1. 30 分钟跑通（DoA: Definition of Achieved）

> 新成员从 clone 到看到冒烟 1-3 全绿 ≤ 30 分钟。

| 步骤 | 时长 | 命令 / 动作 |
|---|---|---|
| 0. 装好前置 | 10 min | 见 `14_dev_env_setup.md` §1 |
| 1. Clone 仓库 | 1 min | `git clone <repo> && cd skills-map` |
| 2. 后端启动 | 5 min | `cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` |
| 3. 前端启动 | 5 min | `cd frontend && npm install && npm run dev` |
| 4. 验证后端 | 1 min | `curl http://localhost:8767/api/skills?page=1&size=5` |
| 5. 验证前端 | 1 min | 浏览器打开 <http://localhost:7777> |
| 6. 验证文档 | 1 min | 打开 <http://localhost:8767/doc.html> |
| 7. 验证 H2 | 1 min | 打开 <http://localhost:8767/h2-console> |
| 8. 跑冒烟 1-3 | 5 min | 见 `docs/PRD.md` §6 步骤 |

> 任何一步卡住 → 看 §8 常见坑。

## 2. 仓库结构速览

```
skills-map/
├── backend/                 Spring Boot 后端
│   ├── data/                运行时数据（gitignore）
│   │   └── skill-packages/  上传的 skill 包
│   ├── src/main/java/com/meiya/skillsmap/
│   │   ├── SkillsMapApplication.java
│   │   ├── common/          Result / BizException / GlobalExceptionHandler
│   │   ├── config/          Security / OpenApi / MyBatisPlus / Web / Seed / Storage
│   │   ├── entity/          Skill / User / Category / Tag / Review / Favorite / SkillResource
│   │   ├── mapper/          MyBatis-Plus BaseMapper 接口
│   │   ├── rest/            公开 Controller
│   │   │   └── admin/       后台 Controller
│   │   ├── security/        JwtAuthFilter / AuthContext
│   │   ├── seed/            SkillSeedService
│   │   ├── service/         业务接口
│   │   │   └── impl/        业务实现
│   │   ├── request/         入参 DTO
│   │   ├── response/        出参 DTO
│   │   └── util/
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   ├── application-dev.yml
│   │   ├── application-local.yml
│   │   └── application-prod.yml
│   ├── src/test/java/       JUnit 5 测试
│   ├── pom.xml
│   ├── mvnw / mvnw.cmd
│   └── Dockerfile
│
├── frontend/                Vue 3 前端
│   ├── public/
│   ├── src/
│   │   ├── api/             axios 请求模块（skill/auth/review/admin）
│   │   ├── assets/
│   │   ├── axios/           单例 + ajax.ts
│   │   ├── components/      通用组件（SkillCard, MarkdownView, ...）
│   │   ├── router/          Vue Router 4
│   │   ├── stores/          Pinia 3 (auth, app)
│   │   ├── style/           全局样式 / tokens.scss
│   │   ├── types/
│   │   ├── utils/
│   │   ├── views/           视图（Home/Browse/SkillDetail/...）
│   │   │   └── admin/
│   │   ├── App.vue
│   │   ├── main.ts
│   │   ├── auto-imports.d.ts
│   │   ├── components.d.ts
│   │   └── env.d.ts
│   ├── package.json
│   ├── tsconfig.json
│   ├── vite.config.ts
│   └── index.html
│
├── docs/                    PRD / API / ER / SEED_DATA / sprint-0
├── docker-compose.yml       MySQL + Backend + Frontend 一键起
├── .claude/CLAUDE.md        团队规约（提交 git）
├── CLAUDE.md                个人覆盖（gitignore）
├── README.md
├── .env.example
├── .dockerignore
└── .gitignore
```

## 3. 后端开发约定

### 3.1 包结构

- **新功能**：先在 `entity/` 建 POJO → `mapper/` 建 BaseMapper → `service/` 建接口 → `service/impl/` 建实现 → `rest/` 建 Controller
- **DTO 分离**：`request/` 入参 + `response/` 出参，禁止直接返回 entity
- **异常**：`throw new BizException(BizCode.XXX)`，由 `GlobalExceptionHandler` 兜底

### 3.2 MyBatis-Plus 约定

- **所有字段** 必须用 `@TableField("snake_case")` 显式映射
- **逻辑删除** 用 `@TableLogic` 注解，统一字段名 `deleted` (Integer, 0/1)
- **分页**：用 `MybatisPlusConfig` 中的 `PaginationInnerInterceptor`
- **不要** 写手写 SQL（除非极复杂聚合），用 `LambdaQueryWrapper` / `UpdateWrapper`
- **不要** 把数据库密码明文写 yml，用 `${ENV}` + jasypt

### 3.3 鉴权

- 受保护接口在 `SecurityConfig` 中放行规则
- Controller 通过 `AuthContext.get()` 拿当前用户

```java
@GetMapping("/me")
public Result<UserInfo> me() {
    Long uid = AuthContext.getUserId();
    return Result.ok(userService.getById(uid));
}
```

### 3.4 API 文档

- Controller 方法加 `@Operation(summary = "...")` + `@Parameter`
- DTO 加 `@Schema(description = "...")`
- 启动后访问 `/doc.html` 验证

### 3.5 测试

- 核心 service 写单测：`src/test/java/.../service/impl/*Test.java`
- 跑全部：`./mvnw test`
- 跑单个：`./mvnw test -Dtest=SkillServiceImplTest`

## 4. 前端开发约定

### 4.1 别名

- `@/` → `src/`
- 例：`import SkillCard from '@/components/SkillCard.vue'`

### 4.2 自动导入

- Vue / Vue Router / Pinia API **不用 import**（自动）
- Ant Design Vue 组件 **不用 import**（自动）
- 工具函数（如 `message`, `Modal`) 自动

> 看 `auto-imports.d.ts` / `components.d.ts` 知注册了哪些

### 4.3 HTTP 请求

- 用 `src/api/<domain>.ts` 中的方法，不在组件内直接 axios
- 编译 URL 用 `path-to-regexp`（在 `ajax.ts` 中）

```ts
// 正确
import { listSkills } from '@/api/skill'
const res = await listSkills({ keyword: 'claude', page: 1, size: 12 })

// 错误
import axios from 'axios'
await axios.get('/api/skills', { params: { keyword: 'claude' } })
```

### 4.4 Pinia Store

- Options API 写法（不用 setup syntax）
- 持久化走 `LocalPrivateCache`（不进 localStorage）
- 鉴权 store：`useAuthStore()` → `token` / `userInfo` / `roles` / `login()` / `logout()`

### 4.5 路由

- 新页面 → `src/views/<Name>View.vue` + 在 `router/index.ts` 加路由
- 受保护路由加 `meta: { requiresAuth: true }` / `meta: { requiresAdmin: true }`

### 4.6 组件命名

- 视图：大驼峰 + View 后缀
- 组件：大驼峰（无 View 后缀）
- 路由：kebab-case

### 4.7 样式

- 全局 token 在 `src/style/tokens.scss`（与 `07_design_system_v0.md` 同步）
- 组件 scoped style，**禁用** 行内 style
- 间距 / 颜色走 CSS Variable（`var(--space-4)` / `var(--color-primary)`）

## 5. 常用命令

### 5.1 后端

```bash
cd backend

# 开发启动（dev profile：H2 + 种子扫描）
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 编译检查
./mvnw -q clean compile

# 打包（jar）
./mvnw clean package -DskipTests

# 跑测试
./mvnw test

# 跑单个测试
./mvnw test -Dtest=SkillServiceImplTest

# 看依赖树
./mvnw dependency:tree | head -50
```

### 5.2 前端

```bash
cd frontend

# 装依赖
npm install

# 开发
npm run dev                    # 默认 :7777
npm run dev -- --port 7778     # 自定义端口

# 构建
npm run build

# 预览构建
npm run preview

# 类型检查
npx vue-tsc --noEmit
```

### 5.3 组合

```bash
# 终端 1
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 终端 2
cd frontend && npm run dev
```

## 6. 调试技巧

### 6.1 后端

| 场景 | 方法 |
|---|---|
| 看 SQL | Druid 控制台：<http://localhost:8767/druid>（dev 开启） |
| 看 JWT 解码 | <https://jwt.io> 粘 token |
| 看 H2 数据 | <http://localhost:8767/h2-console>（JDBC `jdbc:h2:mem:skillsmap`，user `sa`，无密码） |
| 打断点 | IDEA：行号左侧单击；VSCode：装 `Extension Pack for Java` |
| 热重启 | 装 `spring-boot-devtools`（已含），保存自动重启 |
| 看启动日志 | `tail -f backend/logs/spring.log`（如配置） |

### 6.2 前端

| 场景 | 方法 |
|---|---|
| Vue Devtools | 浏览器装扩展，看组件树 / Pinia state |
| 看网络 | 浏览器 F12 → Network，filter XHR |
| 看 Pinia state | 浏览器 console：`useAuthStore()` 后 `$state` |
| 看路由 | console：`router.currentRoute.value` |
| 打断点 | 浏览器 Sources → 找 .vue / .ts |
| 模拟慢网络 | F12 → Network → Throttling: Slow 3G |

### 6.3 跨域

- Vite 已配 `proxy: '/api' → http://127.0.0.1:8767`
- 不需后端 CORS（除非生产跨域）

## 7. 提 PR / 合并流程

1. 从 `master` 拉新分支：`git checkout -b feat/xxx`
2. 改完先本地 `./mvnw -q clean compile && npm run build` 通过
3. 写 commit message：`feat(skill): 详情页加 markdown 渲染`
4. push + 开 PR，标题同上
5. **至少 1 个角色 Reviewer 通过**：
   - 后端改 → dev-kevin
   - 前端改 → designer-vicky
   - 涉及 PRD/范围 → pm-alice
6. 合并策略：Squash merge（默认），commit message 保留原 PR 标题

## 8. 常见坑（Troubleshooting）

| 现象 | 原因 | 解决 |
|---|---|---|
| 后端启动报 `ByteBuddy` 错 | JDK 不是 21 | 装 JDK 21.0.x（见 `14_dev_env_setup.md`） |
| Lombok 不生效 | IDEA 未装 Lombok 插件 / 未开 annotation processor | 装插件 + 勾 Settings → Build → Compiler → Annotation Processors |
| 前端 `npm install` 卡 | 网络 | 配 `npm config set registry https://registry.npmmirror.com` |
| `vite proxy` 跨域 | 后端没启动 | 先起后端 |
| Knife4j `/doc.html` 404 | 忘了加 `OpenApiConfig` | 确认 `OpenApiConfig` bean 存在 |
| 鉴权 401 | token 过期 / 错 | 重新登录 |
| H2 控制台连不上 | JDBC URL 错 | 用 `jdbc:h2:mem:skillsmap` |
| 种子没扫到 | 路径不存在 / frontmatter 缺 `name` | 看启动日志 `[SkillSeedService] skipped: ...` |
| 端口 8767 / 7777 被占 | 别的服务占 | 杀进程 / 改端口（前端 `vite.config.ts` / 后端 `application.yml: server.port`） |
| Lombok 编译报 `getter not found` | 缓存 | IDEA → Build → Rebuild Project |
| 前端 antdv 组件 `resolveComponent` 警告 | 没用自动导入 + 漏了注册 | 确认 `unplugin-vue-components` 配置 AntDV resolver |

## 9. 常用资源

| 资源 | 链接 |
|---|---|
| Spring Boot 3.5 文档 | <https://docs.spring.io/spring-boot/docs/3.5.x/reference/htmlsingle/> |
| MyBatis-Plus 3.5 | <https://baomidou.com/> |
| Knife4j 4.x | <https://doc.xiaominfo.com/> |
| Ant Design Vue 4 | <https://antdv.com/components/overview> |
| Pinia 3 | <https://pinia.vuejs.org/> |
| Vue Router 4 | <https://router.vuejs.org/> |
| Vite 7 | <https://vitejs.dev/> |
| jjwt 0.12.x | <https://github.com/jwtk/jjwt> |
| markdown-it | <https://github.com/markdown-it/markdown-it> |

## 10. 紧急联系 / 升级

| 场景 | 联系人 |
|---|---|
| 后端问题 | dev-kevin（dev 团队） |
| 前端问题 | designer-vicky + dev-kevin |
| 部署 / 环境 | ops-max |
| 需求 / 范围 | pm-alice |
| 跨以上 | agile-rd-lead |

## 11. 修订记录

| 版本 | 日期 | 作者 | 摘要 |
|---|---|---|---|
| v0.1 | 2026-06-06 | dev-kevin | 初版开发者上手：30 分钟跑通 + 仓库结构 + 前后端约定 + 命令 + 调试 + 常见坑 |
