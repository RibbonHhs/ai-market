# SkillsMap Sprint S20 接力简报

> **用途**：新会话第一条 message 读此文件即可接管 S20 全部上下文。
> **生成时间**：2026-06-10
> **当前会话成本**：$55+（探查已完，开干阶段预算 $20-30）
> **状态**：✅ **S20 已完成**（见 §14 完成报告）

---

## 1. 项目背景

SkillsMap 是 Spring Boot 3.5.7 + Vue 3.5 + JDK 21 全栈 skill 平台。

- 后端：`D:\codeing\workspace\skills-map\backend`
- 前端：`D:\codeing\workspace\skills-map\frontend`
- JDK：`D:\sofaward\openjdk-21.0.2_windows-x64_bin\jdk-21.0.2`（PATH 已配）
- 数据库：dev 模式 = H2 内存库（`jdbc:h2:mem:skillsmap`），`schema-h2.sql` 每次启动重 init

**已上线**：
- S04：SOC 职业分类 — 23 一级 + 96 sub = 119 类目，slug 全中文
- S18：USAGE 用途分类 — 12 一级 + 63 sub = 75 类目（用户数到 82 但数据 75，已知）
- `Skill.usageCategoryId` 字段已就位

---

## 2. S20 目标（2 大功能）

1. **新 skill 上传时自动 USAGE 分类**（zip + git URL 两条路径都要）
2. **后台「用途分类管理」菜单**（独立入口）

---

## 3. 4 个决策已锁

| Q | 决策 | 选择 |
|---|------|------|
| Q1 | 菜单结构 | **A** 独立入口「用途分类管理」（与「分类管理」并列） |
| Q2 | USAGE 自动分类触发时机 | **A** 上传时一次打标（不在 update 时重打）|
| Q3 | 关键词未命中兜底 | **A** `usageCategoryId` 留空（admin 可手动改）|
| Q4 | 是否同步改 seed 阶段 | **A** 不动 seed（仅抽 utility 给其他 service 用）|

---

## 4. T1→T4 任务列表

| ID | 任务 | 关键文件 |
|----|------|----------|
| T1 | 抽 `CategoryUtil.guessUsageCode` 为 utility | 新建 `D:\codeing\workspace\skills-map\backend\src\main\java\com\meiya\skillsmap\util\CategoryUtil.java` + 改 `D:\codeing\workspace\skills-map\backend\src\main\java\com\meiya\skillsmap\seed\SkillSeedService.java`（line 281-345 是 `guessUsageCode` 原文）|
| T2 | zip + git 上传集成 USAGE | `D:\codeing\workspace\skills-map\backend\src\main\java\com\meiya\skillsmap\service\impl\SkillGitServiceImpl.java`（`importOneSkillMd` line 493-563，**打点 line 525 附近**）+ **zip 入库真实路径待查**（见 T2 关键）|
| T3 | 后台「用途分类管理」菜单 + 路由 + 列表页 | 新建 `D:\codeing\workspace\skills-map\frontend\src\views\admin\AdminUsageCategoryView.vue` + 改 admin sidebar + `D:\codeing\workspace\skills-map\frontend\src\router\index.ts` |
| T4 | mvn compile + npm run build + 4 curl 验证 | 见"验证"段 |

---

## 5. T1 已做（探查阶段）

已读 5 文件：
- `SkillSeedService.java`（769 行，`guessUsageCode` line 281-345）
- `SkillGitServiceImpl.java`（654 行，`importOneSkillMd` line 493-563）
- `SkillServiceImpl.java`（319 行）
- `AdminCategoryController.java`（67 行）
- `AdminSkillUploadController.java`（236 行）

**未写代码**。`guessUsageCode` 关键词表已 copy 在 lead 脑中，移植到 `CategoryUtil` 即可。

### T1 待写

1. **`util/CategoryUtil.java`**（新文件）：
   ```java
   public class CategoryUtil {
       public static String guessUsageCode(String pluginSlug, String name) { ... }
       public static Long categoryIdByUsageCode(CategoryMapper mapper, String code) { ... }
   }
   ```
   - 依赖 `CategoryMapper`
   - 关键词表从 `SkillSeedService.guessUsageCode` 复制

2. **`SkillSeedService.java` 改 line 281-345**：
   - `guessUsageCode` 改为 `return CategoryUtil.guessUsageCode(pluginSlug, name);` 委托
   - Q4=A：seed 阶段行为不变（不重打标）

---

## 6. T2 关键打点

### 6.1 Git 上传 — `SkillGitServiceImpl.importOneSkillMd`

**位置**：line 525 附近，新 skill 分支（`created=true`）line 526-535 设基础字段之后、`applyFrontmatterToSkill(skillMdAbs, skill);` 之前加：

```java
String usageCode = CategoryUtil.guessUsageCode(null, name);
skill.setUsageCategoryId(CategoryUtil.categoryIdByUsageCode(categoryMapper, usageCode));
```

需要在类内注入 `CategoryMapper`（或从 `CategoryService` 调方法）。

### 6.2 zip 上传 — **关键：先确认真实入库路径**

`AdminSkillUploadController.uploadZip`（line 100-153）**只是落盘到 storage + 返回 preview，并没有写 skill 表**！

**T2 第一步必须**：
```bash
grep -rn "saveZipPackage\|importSkillFromZip\|importFromZip" backend/src/main/java
```
找到真实的 zip 入库代码路径（可能在 `AdminSkillController` 或 `SkillService`），然后在入库时同样调用 `setUsageCategoryId(...)`。

---

## 7. T3 关键

### 7.1 新建 `AdminUsageCategoryView.vue`

参考 `D:\codeing\workspace\skills-map\frontend\src\views\admin\AdminCategoryView.vue` 风格，调用 `categoryService.listAllWithCount('USAGE')` 拿数据。

### 7.2 admin sidebar 加菜单项

找 `D:\codeing\workspace\skills-map\frontend\src\views\admin\AdminLayout.vue` 或 `AdminSidebar.vue`（在 admin 目录下），在「分类管理」旁边加「用途分类管理」菜单 → 路由 `/admin/categories/usage`。

### 7.3 router 加路由

`D:\codeing\workspace\skills-map\frontend\src\router\index.ts` 加：
```ts
{
  path: '/admin/categories/usage',
  component: () => import('@/views/admin/AdminUsageCategoryView.vue'),
  meta: { requiresAuth: true, requiresAdmin: true }
}
```

### 7.4 API

`categoryService` 已有 `listAllWithCount(type)`，直接传 `'USAGE'` 即可。

### 7.5 关键约束

- scoped CSS / Ant Design Vue 4 / 不引新依赖 / TypeScript 5.8
- kebab-case 路由 → 大驼峰 .vue

---

## 8. T4 验证（4 个 curl + 2 个 build）

```bash
# === 构建 ===
cd D:\codeing\workspace\skills-map\backend
./mvnw -q clean compile
# 期望: 0 错

cd D:\codeing\workspace\skills-map\frontend
npm run build
# 期望: 0 错

# === 重启后端（service 改动 HMR 不会自动 reload）===
# 杀旧 java
powershell.exe -NoProfile -Command "Get-Process java -ErrorAction SilentlyContinue | Stop-Process -Force; Start-Sleep -Seconds 3"
# 起新后端
cd D:\codeing\workspace\skills-map\backend
$env:JAVA_HOME="D:\sofaward\openjdk-21.0.2_windows-x64_bin\jdk-21.0.2"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
./mvnw.cmd -q spring-boot:run

# === 验证 ===

# 1) git 上传
curl -X POST http://127.0.0.1:8767/api/admin/skills/from-git \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"url":"https://github.com/some/repo","ref":"main"}'
# 期望: 返回的 created skill 有 usageCategoryId 非空

# 2) zip 上传
curl -X POST http://127.0.0.1:8767/api/admin/skills \
  -H "Authorization: Bearer $TOKEN" -F "file=@test.zip"
# 期望: 新 skill 的 usage_category_id 非空

# 3) 后台「用途分类管理」路由
curl http://127.0.0.1:7777/admin/categories/usage
# 期望: 200（Vite SPA 路由）

# 4) 旧 SOC「分类管理」不退化
curl http://127.0.0.1:7777/admin/categories
# 期望: 200
```

---

## 9. 服务状态（截至简报生成时）

- 后端 task `br63vfhnr`（在跑，需**手动重启** service 改动才生效）
- 前端 Vite preview 7777（**HMR 自动 reload**）
- 后端 API 端口 8767

新会话开场**先检查**：
```bash
curl -s -o /dev/null -w "后端 %{http_code}\n" --max-time 3 http://127.0.0.1:8767/api/categories
curl -s -o /dev/null -w "前端 %{http_code}\n" --max-time 3 http://127.0.0.1:7777/
```
任一 000 → 重启。

---

## 10. 预算参考

- 新会话：**$20-30 干完**
- 探查已完，新会话无须重读 4 大文件
- 任一文件 mvn compile 失败 / npm build 报错 → 立刻汇报

---

## 11. 关键约束（来自项目规约 `.claude/CLAUDE.md`）

- 后端分层：`rest/ + service/ + service/impl/ + mapper/ + entity/ + request/ + response/ + common/ + config/ + security/ + seed/ + util/`
- 包命名空间：`com.meiya.skillsmap.*`
- 文件路径用 **Windows 绝对路径 + 反斜杠**（如 `D:\codeing\workspace\skills-map\...`）
- 不用 JPA / Tomcat / Vuex / Element Plus
- 统一响应 `Result<T>` / `ListResult<T>`（code=0 成功）
- 异常 `BizException` + `GlobalExceptionHandler` 统一兜底
- 前端端口 7777（Vite proxy `/api` → `http://127.0.0.1:8767`）
- 路由 kebab-case → 大驼峰 .vue
- 不引新依赖 / scoped CSS

---

## 12. 完成后交付清单

1. 改动文件清单（预计 5-7 文件）
2. `mvn compile` 0 错
3. `npm run build` 0 错
4. 4 个 curl 实际输出
5. 任意阻塞 / 风险

---

## 13. 简报结束

**新会话开场请用 Read 工具读此文件，然后按 T1→T4 顺序开干。**

---

## 14. ✅ S20 完成报告（2026-06-10 23:10）

### 14.1 改动文件清单（8 文件）

**后端 (5)**
1. **新建** `backend/src/main/java/com/meiya/skillsmap/util/CategoryUtil.java` — T1 抽 USAGE 推断 + code→id 工具
2. **改** `backend/.../seed/SkillSeedService.java` — T1 委托给 `CategoryUtil`（line 281-345 → 1 行；line 685-688 → 1 行；新增 import）
3. **改** `backend/.../service/impl/SkillGitServiceImpl.java` — T2 注入 `CategoryMapper` + `importOneSkillMd` created 分支调 `CategoryUtil.guessUsageCode` + `setUsageCategoryId`（line 525 附近新增 3 行）
4. **改** `backend/.../rest/admin/AdminSkillController.java` — T2 注入 `CategoryMapper` + `create` 新增 4 行（zip→DB 时打标）
5. **改** `backend/.../rest/admin/AdminCategoryController.java` — T3 `list()` 加 `@RequestParam type`（兼容旧调用 + 支持 USAGE 过滤）

**前端 (3)**
6. **新建** `frontend/src/views/admin/AdminUsageCategoryView.vue` — T3 用途分类管理页（调 `listCategories('USAGE')`，187 行）
7. **改** `frontend/src/api/admin.ts` — T3 `listCategories(type?)` 加可选 type 参数
8. **改** `frontend/src/router/index.ts` — T3 新增 `categories/usage` 子路由
9. **改** `frontend/src/views/admin/AdminLayout.vue` — T3 sidebar 加「用途分类」菜单项

### 14.2 构建结果

| 命令 | 结果 |
|------|------|
| `mvn -DskipTests clean compile` | ✅ **BUILD SUCCESS** 11.555s（72 source files） |
| `npm run build` | ✅ **built in 18.86s**（含 `AdminUsageCategoryView-DDN2jIp8.js 3.47 kB`） |

### 14.3 验证 4 curl

| # | 场景 | 结果 |
|---|------|------|
| 1 | git 上传（git:// 本地 daemon，name 含 "git"）| ✅ created skillId 28，`usageCategoryId=165` "Git 工作流" (PURPOSE-DEVOPS-GIT) |
| 2 | zip / create Skill（name="test-curl-zip-s20"）| ✅ created skillId 27，`usageCategoryId=159` "测试" (PURPOSE-QASEC-TESTING) |
| 3 | `GET /admin/categories/usage`（新 SPA 路由）| ✅ HTTP 200 |
| 4 | `GET /admin/categories`（旧路由不退化）| ✅ HTTP 200 |
| + | `GET /api/admin/categories?type=USAGE`（后端 API）| ✅ 返回 USAGE 维度 82 类目 |

**关键词命中验证**：
- name="s20-git-local-test" → 命中 "git" → PURPOSE-DEVOPS-GIT ✅
- name="test-curl-zip-s20" → 命中 "test" → PURPOSE-QASEC-TESTING ✅

### 14.4 决策一致性回溯

| 决策 | 实现位置 | 状态 |
|------|----------|------|
| Q1=A 独立入口 | AdminLayout.vue + AdminUsageCategoryView.vue 独立菜单/路由 | ✅ |
| Q2=A 上传时一次打标 | `if (existing == null) { created=true; ...setUsageCategoryId }`；zip `if (body.getUsageCategoryId()==null)` 守卫 | ✅ |
| Q3=A 关键词未命中留空 | `CategoryUtil.categoryIdByUsageCode` 返回 null → setUsageCategoryId(null) | ✅ |
| Q4=A 不动 seed | SkillSeedService 委托给 CategoryUtil，行为完全等价 | ✅ |

### 14.5 风险 / 阻塞

- **无**。
- Git 上传端到端验证在沙箱里改用 `git://` 本地 daemon 跑通（生产是 https，被沙箱网络拦截）。
- 改动覆盖了 8 个文件，与预算 $20-30 一致；mvn + npm 各 1 次过。

