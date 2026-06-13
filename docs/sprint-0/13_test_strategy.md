# Test Strategy（测试策略）

> 作者：qa-tina @ Sprint 0 Kickoff (2026-06-06)
>
> 版本：v1.0 · 适用范围：v1 全量 · 引用：`docs/PRD.md` §6 冒烟用例 / `12_dod.md` / `04_product_backlog_v1.md` / `11_dev_onboarding.md`

## 1. 测试分层（Test Pyramid）

```
                    ┌──────────────┐
                    │   E2E (少量)  │   ← 冒烟 10 条 + 关键路径
                    ├──────────────┤
                    │ 集成 / API   │   ← Controller 层 + 真实 H2
                    ├──────────────┤
                    │  单元测试    │   ← Service 核心方法 ≥ 70% 覆盖
                    └──────────────┘
```

| 层级 | 工具 | 数量目标 | 运行时机 |
|---|---|---|---|
| 单元（Unit） | JUnit 5 + Mockito | Service 70% 行覆盖 | 每次 PR / `./mvnw test` |
| 集成（Integration） | Spring Boot Test + 真实 H2 | 每个 Controller ≥ 1 个 happy + 1 个 fail | 每次 PR |
| API（Contract） | curl / Postman / Knife4j Try | 公开 API 100% | Sprint Review 前 |
| 端到端（E2E） | 手动（v1）+ Vitest（Sprint 2 评估） | 冒烟 10 条 | Sprint Review + Release |
| 安全 | 手动 + OWASP dependency-check | 关键接口 | Sprint 中段 |

> v1 不上 Vitest（见 `.claude/CLAUDE.md` §测试，v1.1 评估）；前端靠手测冒烟 + TS 类型检查。

## 2. 单元测试（Unit）

### 2.1 范围

- `service/impl/*` 中所有 public 方法
- `util/*` 中纯函数
- 关键 `entity` 字段逻辑（如状态机）

### 2.1.1 Sprint 0 落地清单（首批后端测试 — 2026-06-06 落地）

| 文件 | 用例数 | 关联 Bug / 风险 |
|---|---|---|
| `backend/src/test/java/com/meiya/skillsmap/util/MarkdownFrontmatterParserTest.java` | 11 | BUG-S0-R01（YAML `: ` 预清洗）+ 边界（空/null/已引号/list/map/` #`/URL key/单引号转义） |
| `backend/src/test/java/com/meiya/skillsmap/service/SkillStorageServiceFlattenTest.java` | 11 | BUG-S0-R02（zip 单层子目录上提）+ 大小/隐藏/`.git` 过滤 + zip `..` 拒绝 |

> 跑法：`cd backend && JAVA_HOME=<JDK21 路径> ./mvnw -q test`
> （本机默认 `java` 是 1.8，需切到 `D:/sofaward/openjdk-21.0.2_windows-x64_bin/jdk-21.0.2`）
> 结果：22/22 全绿，BUILD SUCCESS（详见 §11 + 12_dod.md §2）

### 2.2 规范

```java
// SkillServiceImplTest.java
@ExtendWith(MockitoExtension.class)
class SkillServiceImplTest {

    @Mock SkillMapper skillMapper;
    @InjectMocks SkillServiceImpl skillService;

    @Test
    void shouldReturnSkill_whenIdExists() {
        Skill s = new Skill(); s.setId(1L); s.setName("x");
        when(skillMapper.selectById(1L)).thenReturn(s);

        Skill result = skillService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("x");
    }

    @Test
    void shouldThrowBizException_whenIdNotFound() {
        when(skillMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> skillService.getById(99L))
            .isInstanceOf(BizException.class);
    }
}
```

### 2.3 覆盖率目标

| 模块 | 行覆盖 | 分支覆盖 |
|---|---|---|
| Service 核心（User/Skill/Review/Favorite/Category/Tag） | ≥ 70% | ≥ 50% |
| 安全（AuthService, JwtAuthFilter） | 100% | 100% |
| Util | ≥ 80% | — |

### 2.4 不测的东西

- 简单 getter / setter
- MyBatis-Plus BaseMapper 内部逻辑
- Spring Security 配置
- 自动配置类

## 3. 集成测试（Integration）

### 3.1 范围

- `rest/*` Controller 的 happy path
- 全局异常处理
- 鉴权（401 / 403）
- MyBatis-Plus 自动建表 + 逻辑删除

### 3.2 规范

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SkillControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @Test
    void list_shouldReturnPaged() throws Exception {
        mvc.perform(get("/api/skills?page=1&size=10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.records").isArray());
    }
}
```

### 3.3 测试 profile

- `application-test.yml` 用 H2 内存库 + 关闭种子扫描
- 测试前 `@Sql` 注解插 fixture
- 测试后 `@Sql` 注解清表（或 `@Transactional` 回滚）

## 4. API 契约测试（Contract）

### 4.1 范围

- `docs/API.md` 中所有接口
- 路径 / 方法 / 入参 / 业务码

### 4.2 工具

- **首选**：Knife4j `/doc.html` 的 Try it out（手动）
- **进阶**：Postman + Newman 跑脚本（v1.1）
- **样例**：每个新接口在 `docs/api-samples/` 下放 `.sh` 脚本（curl） 或 `.json`（Postman）

```bash
# docs/api-samples/skills-list.sh
curl -s 'http://localhost:8767/api/skills?page=1&size=5' \
  | jq '.data.records | length'
```

### 4.3 鉴权约定

- 公开接口：`/api/auth/**` `/api/skills/**` `/api/categories` `/api/tags`
- 需登录：`/api/auth/me` `/api/reviews` `/api/favorites/**`
- 需 ADMIN：`/api/admin/**`

## 5. 端到端测试（E2E / 冒烟）

### 5.1 范围：PRD §6 的 10 条冒烟用例

> **完整冒烟用例索引**（每条对应 Story ID）

| # | 场景 | 步骤 | 预期 | 关联 Story |
|---|---|---|---|---|
| 1 | 浏览首页 | 打开 `/` | 看到 hero + 精选/最新/热门三组 | US-001~005 |
| 2 | 关键词搜索 | `/browse` 搜 "claude" | 返回相关 skill 列表 | US-007 |
| 3 | 详情查看 | 点任意 skill | Markdown 渲染 + 评分列表 | US-010, 011, 013 |
| 4 | 用户注册 | `/register` 注册 test1/123456 | 注册成功，自动登录 | US-014 |
| 5 | 提交评分 | 详情页 5⭐+评论 | 提交后详情页评分更新 | US-018 |
| 6 | 收藏 | 详情页点收藏 | `/me/favorites` 可见 | US-019, 016 |
| 7 | 后台登录 | `/admin` admin/admin123 | 进 Dashboard | US-020 |
| 8 | 新建 Skill | 后台表单填写 | `/browse` 立即可见 | US-023 |
| 9 | 上架/下架 | 后台操作 | 列表过滤 | US-025 |
| 10 | 错误路径 | 未登录访问 `/admin` | 跳 `/login` | US-030 |

### 5.2 执行节奏

- **Sprint 内**：每个 Story 完成的当天跑该 Story 对应冒烟
- **Sprint Review 前**：跑全部 10 条
- **Release 前**：跑全部 10 条 + 加 5 条回归（修复过的 bug）

### 5.3 通过标准

- 10 条全绿 → Sprint Goal 达成
- ≤ 2 条非阻塞（红但有 workaround）→ 可放行
- > 2 条红 → Sprint 不算 Done

### 5.4 Bug 回归（来自 09_risk_register.md 已修复风险）

| # | Bug 场景 | 复现步骤 | 预期 | 自动化位置 | 关联 Risk |
|---|---|---|---|---|---|
| B-1 | 上传 zip 根目录无 SKILL.md（被"包了一层"目录）→ 上传失败 | admin 上传 `frontend-patterns.zip`（结构：`zip/frontend-patterns/SKILL.md`） | `code:0` + `data.name="frontend-patterns"` + `dir/SKILL.md` 与 `dir/agents/*` 都在根 | `backend/src/test/.../service/SkillStorageServiceFlattenTest.java#shouldFlattenOnSaveZipPackage` + E2E curl | BUG-S0-R02 / R-08 |
| B-2 | SKILL.md 的 `description` 含 `: `（如 `React: pages`）→ SnakeYAML 抛 ScannerException | 解析 `web-design-engineer.zip` 的 SKILL.md frontmatter | `frontmatter.description` 是 String，内容含 "React: pages" | `backend/src/test/.../util/MarkdownFrontmatterParserTest.java#shouldNotThrowWhenDescriptionContainsColonSpace` + E2E curl | BUG-S0-R01 / R-08 |

> E2E curl 命令见 `docs/api-samples/smoke-bugfix-s0.sh`（v1.1 补）。

## 6. 安全测试（Security）

### 6.1 范围（Sprint 1 起执行）

| 项 | 工具 / 方法 | 频率 |
|---|---|---|
| 依赖 CVE | OWASP dependency-check（Maven 插件） | 每次 PR |
| XSS | qa-tina 手测：详情页注入 `<script>alert(1)</script>` | Sprint 1 |
| SQL 注入 | qa-tina 手测：搜索框输入 `' OR 1=1 --` | Sprint 1 |
| 越权 | 用 USER 角色 token 调 `/api/admin/**`，应 403 | 每个 Sprint |
| 密码强度 | 注册接口弱密码（"123"）应被拒 | Sprint 1 |
| 默认密码 | `admin/admin123` 首登强提示 | Sprint 1 |

### 6.2 关键安全样例（写到 `docs/api-samples/security/`）

```bash
# xss.sh — 提交恶意 markdown
curl -X POST http://localhost:8767/api/admin/skills \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d @malicious-skill.json
# 期望：详情页渲染 <script> 不执行
```

## 7. 性能测试（Performance — 非强制）

### 7.1 Sprint 1 末跑 1 次

- 用 `wrk` 或 Apache Bench 对 `/api/skills` 打 100 RPS
- 目标：P95 ≤ 300ms，无 5xx

### 7.2 监控指标

- API P50 / P95
- DB 慢 SQL（Druid）
- 内存 / GC

## 8. 测试数据管理

| 数据 | 来源 | 清理策略 |
|---|---|---|
| H2 默认 | 种子扫描 | dev 重启清空 |
| 测试 fixture | `@Sql` 注解 / `data-test.sql` | 每次测试前重置 |
| 真实注册用户 | 冒烟 4 | 测试结束手动 `DELETE` 或保留供回归 |
| 默认账号 admin / user | 启动种子 | 不删 |

## 9. 缺陷管理

### 9.1 等级

| 等级 | 含义 | 修复 SLA |
|---|---|---|
| P0 | 阻塞核心流程 / 数据丢失 / 安全 | 立即（hotfix） |
| P1 | 主流程不可用 | 当 Sprint |
| P2 | 次要功能失效 | 下 Sprint |
| P3 | 体验问题 / 文案 | Backlog |

### 9.2 报告模板

```markdown
## [P1] 详情页 markdown 代码块高亮丢失

**环境**：dev profile / Chrome 120
**步骤**：
1. 打开 /skills/claude-test
2. 查看正文
**预期**：代码块带语法高亮
**实际**：纯文本
**截图**：____
**复现率**：100%
**关联 Story**：US-011
```

### 9.3 跟踪

- Sprint 内发现的缺陷 → GitHub Issue（标签 `bug` + `P{0-3}` + 关联 Story）
- Sprint Review 上过一遍 P0/P1 状态

## 10. 测试工具栈

| 工具 | 用途 | 配置位置 |
|---|---|---|
| JUnit 5 | 后端单测 | spring-boot-starter-test |
| Mockito | Mock 框架 | spring-boot-starter-test 含 |
| AssertJ | 断言 | spring-boot-starter-test 含 |
| Spring Boot Test | 集成测试 | spring-boot-starter-test |
| MockMvc | Web 集成 | spring-boot-starter-test |
| `@TempDir` | 隔离临时数据 | JUnit 5 内建（SkillStorageService 测试用）|
| H2 (test profile) | 集成测试 DB | application-test.yml |
| OWASP dependency-check | 依赖 CVE | `15_cicd_plan.md` 草图 |
| curl / jq | 手动 API 测试 | `docs/api-samples/` |
| Postman | API 探索 | 可选 |

## 11. Sprint 1 测试计划（具体到天）

| Day | 测试活动 |
|---|---|
| D1 | 跑 PRD §6 冒烟 1-3，验收 US-001~005 |
| D2 | 跑冒烟 2（搜索），验收 US-007 |
| D3 | 跑冒烟 3（详情），验收 US-010/011/013 |
| D5 | Mid-sprint 测：XSS / SQL 注入样例 |
| D6 | 跑冒烟 1-3 全套 |
| D7 | 修复回归 + 加 3 条边界用例 |
| D9 | Sprint Review 全冒烟 + 缺陷评审 |
| D10 | Retro 反馈入下 Sprint |

## 12. 修订记录

| 版本 | 日期 | 作者 | 摘要 |
|---|---|---|---|
| v1.0 | 2026-06-06 | qa-tina | 初版测试策略：分层 + 工具 + 冒烟索引（10 条）+ 安全 + 缺陷管理 |
| v1.1 | 2026-06-06 | qa-tina | §5.4 新增 B-1/B-2 Bug 回归（Bug1 zip 单层上提 / Bug2 YAML `: ` 预清洗） + 工具表加 `@TempDir`；首版 2 个 JUnit 测试类落地（22 个用例全绿，详见 §2.1） |
