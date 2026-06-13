# SkillsMap 项目规约

> ⚠️ **本文件为团队共享规约**，所有协作者强制遵守。
> 个人偏好 / 临时指令请放根目录的 `CLAUDE.md`（不进 git）。
> 上下文来自 `D:\codeing\workspace\` 下的既有项目（`dos-backend` + `dos-front-vue3`）。

## 🎯 核心约束

### 后端

- **Spring Boot 3.5.7** + **JDK 21** + **MyBatis-Plus 3.5.12**（不用 JPA、不用 Tomcat）
- 包命名空间 `com.meiya.skillsmap.*`
- 分层：`rest/ + service/ + service/impl/ + mapper/ + entity/ + request/ + response/ + common/ + config/ + security/ + seed/ + util/`
- 所有数据库字段用 `@TableField` 显式映射 snake_case
- 逻辑删除字段统一名 `deleted`（type=Integer，0/1）
- 统一响应：`Result<T>` / `ListResult<T>`（code=0 成功，code≠0 业务码）
- 异常：`BizException` + `GlobalExceptionHandler` 统一兜底
- 鉴权：JWT（jjwt 0.12.x），`JwtAuthFilter` 解析后塞 `AuthContext`
- 文档：Knife4j `/doc.html`（默认开启，prod 关闭）

### 前端

- **Vite 7** + **TypeScript 5.8** + **Vue 3.5** + **Pinia 3** + **Ant Design Vue 4**（不用 Vuex、不用 Element Plus）
- 包风格：ESM + TS
- 状态管理：Pinia Options API + LocalPrivateCache 持久化
- HTTP：`axios` 单例 + `ajax.ts` 用 `path-to-regexp` 编译 URL
- 路由：Vue Router 4 + meta.requiresAuth / meta.requiresAdmin
- 别名：`@` → `src/`
- 自动导入：`unplugin-auto-import` + `unplugin-vue-components`（已配 Ant Design Vue resolver）
- 端口：dev 7777（Vite proxy `/api` → `http://127.0.0.1:8767`）

### 数据库

- dev 模式：H2 内存库（MySQL 兼容模式 + LowerCase + 关闭敏感大小写）
- local / prod：MySQL 8.3
- 表由 MyBatis-Plus 首次启动时自动创建（基于 `@TableName` + `@TableField`）
- 种子数据由 `SkillSeedService` 启动时扫描本地 Skills 目录入库

## 🚫 反模式（Do NOT）

- ❌ 不要引入 JPA（Hibernate）/ Tomcat / Vuex / Element Plus
- ❌ 不要把 `controller` 当包名（用 `rest`）
- ❌ 不要在 Controller 直接写业务逻辑（必须经 Service）
- ❌ 不要用 `Optional` 当字段类型（用包装类型 + null 判断）
- ❌ 不要把数据库密码明文写在 `application*.yml`（用 `${ENV}` + jasypt）
- ❌ 不要在前端 `localStorage` 存敏感信息（用 LocalPrivateCache + 加密 token）
- ❌ 不要在 PR 中包含 `target/`、`node_modules/`、`dist/`

## 🧪 测试

- 后端：`spring-boot-starter-test` + JUnit 5（核心 service 写单测）
- 前端：v1.1 再加 Vitest（v1 优先手测冒烟）
- 冒烟用例见 `docs/PRD.md` §验收

## 📁 文件命名

- Java 类：大驼峰 `SkillService.java`
- 数据库表：小写蛇形 `skill` / `skill_tag`（关联表）
- 前端组件：大驼峰 `SkillCard.vue`
- 路由：kebab-case `/browse-skills` → 视图 `BrowseSkillsView.vue`
- 常量：`UPPER_SNAKE_CASE`

## 🔄 依赖升级

- 升级 Spring Boot 大版本需同时检查 springdoc / knife4j 兼容
- 升级 JDK 大版本需确认 Lombok / ByteBuddy 编译时增强版本
- 提交前 `./mvnw -q clean compile` + `npm run build` 必须通过

## 📂 项目记忆

项目专属的个人/机器事实（端口、JDK 路径等）存放在 **`.claude/memory/`** 目录，由 `MEMORY.md` 索引。本文件加载时，Claude 应主动查阅该目录；新事实也优先写到这里而非全局 memory（`~/.claude/projects/.../memory/`），以便与项目生命周期绑定。
