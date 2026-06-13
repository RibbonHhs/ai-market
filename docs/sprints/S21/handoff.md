# SkillsMap Sprint S21 接力简报

> **用途**：新会话第一条 message 读此文件即可接管 S21 全部上下文。
> **生成时间**：2026-06-11
> **状态**：✅ **S21 已完成**（见 §14 完成报告）

---

## 1. 项目背景

SkillsMap 是 Spring Boot 3.5.7 + Vue 3.5 + JDK 21 全栈 skill 平台。

- 后端：`D:\codeing\workspace\skills-map\backend`
- 前端：`D:\codeing\workspace\skills-map\frontend`
- JDK：`D:\sofaward\openjdk-21.0.2_windows-x64_bin\jdk-21.0.2`（PATH 已配）
- 数据库：dev 模式 = H2 内存库（`jdbc:h2:mem:skillsmap`）
- 后端 8767，前端 7777

**S20 已完成**：USAGE 维度 + 「用途分类管理」菜单上线。
**S21 已完成**：公开 API 职业筛选 + API 接入文档页 + skills-manager 智能体 skill 包 + 首页「我是智能体」tab。

---

## 2. S21 目标（2 大需求）

1. **REQ-1 公开 REST API + 接入文档页**：`GET /api/skills` 新增 `occupationCode` 职业筛选；新增 `/api-guide` 前端文档页
2. **REQ-2 skills-manager Skill 包 + 首页「我是智能体」TAB**：打包官方 skill + 首页推广入口

---

## 3. 4 个决策已锁

| Q | 决策 | 选择 |
|---|------|------|
| Q1 | 职业字段 | **新增 occupationCode**（与 categoryId 并列，支持一级 `#01` / 二级 `01-01`） |
| Q2 | 文档页 | **仅本次需求涉及的端点介绍页**（非全量 API） |
| Q3 | 智能体 tab 位置 | **HomeHot 加 tab**（紧跟 USAGE tabs 末尾） |
| Q4 | skills-manager 路径 | **`backend/src/main/resources/skills/skills-manager/`** |
| Q5 (lead 决定) | seed 扫描策略 | **混合扫描**：`localSkillsPath` + `localPluginsPath` + 新增 `classpath:skills/` |

### Q4/Q5 路径扫描决策依据

`SkillSeedService` 原本扫两个路径：
- `localSkillsPath` (默认 `~/.claude/skills`) — 用户本地 skill
- `localPluginsPath` (默认 `~/.claude/plugins/marketplaces/...`) — Claude 官方 plugin

`skills-manager` 是应用自带的官方 skill（要随 jar 一起发布），但不应该放到用户家目录（污染用户本地 skills），也不属于 plugin。

**Lead 决策**：新增 classpath 扫描 `classpath:skills/`，不影响现有配置 schema；classpath 扫描是**幂等**的（重复启动不重复入库）；下载走 `exportZip` 的 DB-rebuild 路径，与现有下载流程一致。

实现细节：
- 拆 jar 提取 `skills/` 目录到临时目录后扫描
- 来源标记为 `official-bundled`（与 `official` 区分）
- 启动顺序：先扫 `localSkillsPath` → `localPluginsPath` → `classpath:skills/`
- 已存在 skills 时仍会跑 classpath 扫描（保证 bundled skill 补齐）

---

## 4. T1→T6 任务分解

| ID | 任务 | 关键文件 | 状态 |
|----|------|----------|------|
| T1 | `occupationCode` 字段 + 过滤 | `request/SkillQueryRequest.java` `service/impl/SkillServiceImpl.java` `rest/SkillController.java` | ✅ |
| T2 | ApiGuideView + 路由 + AppHeader 菜单 | `views/ApiGuideView.vue`（新）`router/index.ts` `components/AppHeader.vue` | ✅ |
| T3 | skills-manager Skill 包 | `resources/skills/skills-manager/SKILL.md` `references/api-endpoints.md` `scripts/sync-skills.sh` | ✅ |
| T4 | SkillSeedService 改 classpath 扫描 | `seed/SkillSeedService.java` | ✅ |
| T5 | HomeHot 加「我是智能体」tab | `components/home/HomeHot.vue` | ✅ |
| T6 | 启动时 skills-manager 自动入库 | 由 T4 触发（已验证：27 skills imported，skills-manager 在 id=27） | ✅ |

---

## 5. T1 改动详情

### 5.1 `request/SkillQueryRequest.java`

新增字段 + getter/setter：
```java
// S21: SOC 一级职业 code（如 "#01" / "01-01"）按职业维度筛
private String occupationCode;
```

### 5.2 `service/impl/SkillServiceImpl.java`

`listSkills` 中将 `categoryId` 与 `occupationCode` 共用一个 SOC category id 集合（**交集**，确保组合筛仍可用）：

```java
// S21: 合并 categoryId 和 occupationCode 的 SOC 过滤
java.util.List<Long> socIds = null;
if (q.getCategoryId() != null) { ... 原有展开逻辑 ... }
// S21: 按职业 code 过滤
if (StrUtil.isNotBlank(q.getOccupationCode())) {
    String occ = q.getOccupationCode().trim();
    List<Category> socCats = categoryMapper.selectList(
            new LambdaQueryWrapper<Category>().eq(Category::getType, "SOC"));
    java.util.List<Long> occIds = new ArrayList<>();
    Long rootId = null;
    for (Category oc : socCats) {
        if (oc.getCode() == null) continue;
        if (oc.getCode().equals(occ)) {
            occIds.add(oc.getId());
            if (oc.getParentId() == null) rootId = oc.getId();
        }
    }
    if (rootId != null) {
        // 一级职业 → 展开到所有 sub-category
        for (Category oc : socCats) {
            if (rootId.equals(oc.getParentId())) occIds.add(oc.getId());
        }
    }
    if (occIds.isEmpty()) return ListResult.empty(page, size);
    if (socIds != null) socIds.retainAll(occIds); else socIds = occIds;
}
if (socIds != null) {
    if (socIds.isEmpty()) return ListResult.empty(page, size);
    wrapper.in(Skill::getCategoryId, socIds);
}
```

**关键点**：
- 一级 `#01` → 展开到所有 `parentId=#01.id` 的 sub-category
- 二级 `01-01` → 精确匹配该 category
- `categoryId` + `occupationCode` 组合时取**交集**（都展开后再 retainAll）
- 不冲突 USAGE 维度（`usageCategoryId` 走 `eq` 独立过滤）

### 5.3 `rest/SkillController.java`

`@Operation` description 补充 occupationCode 说明。

### 5.4 `frontend/src/api/skill.ts`

`SkillQuery` 接口加 `occupationCode?: string`。

### 5.5 `docs/API.md`

§2 加 `occupationCode` 参数详解和 curl 示例。

---

## 6. T2 改动详情

### 6.1 `views/ApiGuideView.vue`（新建 320+ 行）

5 大块：
1. **端点总览** — 9 个公开端点表
2. **`GET /api/skills` 参数说明** — 9 个参数表
3. **响应字段（SkillVO 核心字段）** — 16 个字段表
4. **快速上手示例** — 6 个 curl 例子（关键字 / categoryId / occupationCode / 组合 / 分页 / 下载），每条带「拷贝」按钮
5. **统一响应格式** — 完整 JSON 示例

**特色**：
- 用 `URLSearchParams`/`fetch` 自带编码（`#` 自动转 `%23`）
- 「拷贝」按钮：`navigator.clipboard.writeText` + 降级到 `document.execCommand('copy')`
- 暗色 code block + 1f1f1f 高亮数字编号
- 路由 `/api-guide`，无 auth 限制（公开页）

### 6.2 `router/index.ts`

新增路由：
```ts
{
  path: '/api-guide',
  name: 'api-guide',
  component: () => import('@/views/ApiGuideView.vue'),
  meta: { title: 'API 接入' }
}
```

### 6.3 `components/AppHeader.vue`

桌面菜单 + 移动 drawer 都加 `<a-menu-item key="api-guide">API 接入</a-menu-item>`。

---

## 7. T3 + T4 改动详情

### 7.1 skills-manager skill 包结构

```
backend/src/main/resources/skills/skills-manager/
├── SKILL.md                       # 描述 + 典型用法
├── references/
│   └── api-endpoints.md           # 核心端点速查
└── scripts/
    └── sync-skills.sh             # 拉全量 CSV
```

### 7.2 `seed/SkillSeedService.java` 改动

- `seedSkills()` 不再只判断 `selectCount > 0` 就全跳；会继续跑 classpath 扫描
- 新增 `scanClasspathSkills()`：拆 jar 提取 `skills/` → 扫子目录 → 复用 `importSkill`
- 新增 `extractFromJar()`：处理 `jar:file:...!` 协议，把 jar 内资源写到临时目录

入口调整后启动日志：
```
[seed] classpath:skills/ scanned, 1 bundled skills imported
[seed] total 27 skills imported
```

### 7.3 download 兼容性

`SkillServiceImpl.exportZip` 检测到 `skillStorageService.hasPackage(name) == false` 时走 DB-rebuild 路径。classpath 资源不在 storage root 下，所以 skills-manager 下载返回的是 DB-rebuild 的 SKILL.md（内容一致）。生产环境若要确保下载到完整 resources 包（包含 references/ 和 scripts/），需要把 resources 拷贝到 storage root 或调整 exportZip 兜底。**v1 接受 DB-rebuild**，待后续优化。

---

## 8. T5 改动详情

### 8.1 `components/home/HomeHot.vue`

- `tabs` 末尾追加 `{ key: 'agent', label: '我是智能体', icon: '🤖' }`
- `buildUsageTabs` 末尾自动 push agent tab
- `load()` 检测 `activeTab === 'agent'` 时不请求 API，直接渲染 promotion panel
- 新增 `downloadSkill()`：`fetch('/api/skills/slug/skills-manager/download')` → blob → a.click()

### 8.2 agent panel 结构

- 简介（skills-manager 一句话）
- 4 步流程（下载 → 解压 → 重启 → 试指令）
- 3 个 prompt 例子
- 跳转「API 接入指南」

CSS 用 SCSS scoped，类前缀 `agent-panel__*`。

---

## 9. 验证结果

### 9.1 mvn compile

```
[INFO] Compiling 72 source files with javac [debug parameters release 21] to target\classes
[INFO] BUILD SUCCESS
[INFO] Total time:  12.442 s
```

### 9.2 npm run build

```
✓ built in 19.60s
dist/assets/ApiGuideView-Cbn5w0iO.js               8.63 kB │ gzip:  3.75 kB
```

零 TypeScript 错误，零 ESLint 错误（项目无 eslint 配置）。

### 9.3 5 个 curl 验证全过

| # | 命令 | 结果 |
|---|------|------|
| 1 | `GET /api/skills?keyword=claude&size=3` | total=4，命中 4 条 claude 相关 skill |
| 2 | `GET /api/skills?categoryId=1&size=5` | total=20，SOC 一级「计算机与数学」下所有 skill |
| 3 | `GET /api/skills?occupationCode=%2301&size=10` | total=20，职业维度 #01 下所有 skill（与 #2 等价） |
| 4 | `GET /api/skills?keyword=skills&occupationCode=%2301&sort=hot&size=5` | total=7，组合筛结果 |
| 5 | `GET /api/skills?page=2&size=3&sort=latest` | total=27，page=2 of 9 页 |
| 6 | `GET /api/skills/slug/skills-manager/download` | 1642 字节 zip（包含 SKILL.md 2562 字节） |

r2 用 `categoryId=2` 测试时 total=0 是因为 SOC 一级 #02「商业与金融运营」下确实没 skill 入库（数据本身就是 0），不是 bug。

### 9.4 数据库状态

启动后 `selectCount(skill)` = 27，含 1 个 `source=official-bundled` 的 `skills-manager`（id=27，slug=skills-manager，status=published，featured=true）。

---

## 10. 改动文件清单

### 新建（4 个）

| 路径 | 行数 | 说明 |
|------|------|------|
| `frontend/src/views/ApiGuideView.vue` | 350+ | API 接入文档页 |
| `backend/src/main/resources/skills/skills-manager/SKILL.md` | 100 | skills-manager skill 描述 |
| `backend/src/main/resources/skills/skills-manager/references/api-endpoints.md` | 80 | API 端点速查 |
| `backend/src/main/resources/skills/skills-manager/scripts/sync-skills.sh` | 50 | CSV 同步脚本 |

### 修改（7 个）

| 路径 | 改动 |
|------|------|
| `backend/.../request/SkillQueryRequest.java` | +1 字段 + getter/setter |
| `backend/.../service/impl/SkillServiceImpl.java` | `categoryId` + `occupationCode` 合并 SOC 过滤逻辑（+30 行） |
| `backend/.../rest/SkillController.java` | `@Operation` description 补充 |
| `backend/.../seed/SkillSeedService.java` | +`scanClasspathSkills` + `extractFromJar`（+80 行） |
| `frontend/src/router/index.ts` | +`/api-guide` 路由 |
| `frontend/src/components/AppHeader.vue` | +桌面 + 移动菜单项 |
| `frontend/src/components/home/HomeHot.vue` | +agent tab + panel + downloadSkill()（+180 行） |
| `frontend/src/api/skill.ts` | +`occupationCode` 类型 |
| `docs/API.md` | §2 新增 occupationCode 参数表 + curl 示例 |

---

## 11. 风险与已知限制

| 风险 | 说明 | 缓解 |
|------|------|------|
| skills-manager 下载只含 SKILL.md | classpath 资源不在 storage root，exportZip 走 DB-rebuild 不含 references/scripts | v1 接受；v1.1 计划：seed 时把 bundled skill 物化到 storage root |
| occupationCode 与 categoryId 组合是交集 | 不是 union（按 SOC 维度，叠加筛应更严而非更松） | 行为符合预期，与 SQL IN 语义一致 |
| `code=0` 不展开 `01-01` 的 sub-sub | 当前 sub-group 没有子层 | 留作未来扩展 |
| AppHeader 移动菜单未做折叠 | 现有 5 个菜单项（首页/浏览/职业技能/用途分类/API 接入）在移动端可能溢出 | 留作 v1.1 polish |
| API 接入页没有锚点深链 | 用户无法分享到具体端点 | 留作 v1.1 |
| 智能体 tab 没有「已加载 / 未加载」反馈 | 不知道用户是否真装了 skill | 留作后续，可加 localStorage 标记 |

---

## 12. 后续 Sprint 建议

- **S22 候选**：
  - skills-manager 物化到 storage root（让下载包含 references/scripts）
  - `/api-guide` 加锚点深链 + 暗色模式适配
  - 智能体 tab 加埋点（PV / download-click）
  - `keyword` 增强（高亮命中片段）
  - `occupationCode` 支持数组（多职业 OR 筛）
- **S23 候选**：
  - skills-manager 升级：加 CRUD / 评分提交等写操作（需鉴权代理）
  - 公开 API 限流（按 IP）

---

## 13. 验收清单

- [x] `mvn clean compile` BUILD SUCCESS（72 源文件）
- [x] `npm run build` 0 错
- [x] 5 个 curl 验证全过（关键字/分类/职业/组合/分页）
- [x] skills-manager slug 在 /api/skills 可查（id=27，source=official-bundled）
- [x] skills-manager zip 可下载
- [x] /api-guide 路由可访问
- [x] AppHeader 「API 接入」菜单可见
- [x] HomeHot 「我是智能体」tab 可点
- [x] docs/API.md 同步更新

---

## 14. 完成报告

Sprint S21 完成。
- REQ-1（API 职业筛选 + 文档页）✅
- REQ-2（skills-manager + 智能体 tab）✅
- 4 + 1 决策全部落地
- 6 个任务全部交付
- 验证全过

Lead 决策（Q5：路径扫描策略 = classpath 混合扫描）已在 §3 写明依据，可追溯。

---

## 15. Patch 2026-06-11：智能体 tab 位置调整

### 15.1 原因
- 用户反馈：「我是智能体」不应在「热门 skills」里，应和搜索框同一个区域分 TAB
- 「我是人类」= 搜索框；「我是智能体」= skills-manager 说明

### 15.2 改动
- `frontend/src/components/home/HomeHero.vue`：
  - 模板：`activeTab === 'assist'` 显示搜索框 + chips；`activeTab === 'agent'` 显示 skills-manager 介绍 + 4 步流程 + 下载按钮
  - script：新增 `downloadSkill()` + `message` import + `downloading` ref
  - style：新增 `&__agent*` CSS（约 110 行，agent panel 视觉）
- `frontend/src/components/home/HomeHot.vue`：
  - 移除 `agent` tab、`agent-panel` 模板分支、`downloadSkill` 函数、`message` import、`downloading` ref
  - `buildUsageTabs` 不再追加 agent tab
  - 删除大段 `.agent-panel` CSS

### 15.3 验证
- `npm run build` ✅ 0 错（built in 26.86s）
- HomeHero 双 tab 切换：assist → 搜索框；agent → skills-manager 介绍 + 下载
