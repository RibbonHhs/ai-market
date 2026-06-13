# Risk Register（风险登记册）

> 作者：agile-rd-lead @ Sprint 0 Kickoff (2026-06-06)
>
> 版本：v1.0 · 适用范围：所有 Sprint · 维护人：Lead · 评审频率：每次 Mid-sprint Check + Retrospective

## 0. 决策待办（Decision Pending）

> Sprint 0 期间如发现上下文文件之间矛盾，先记在这里，**继续往下推**而不是停下来等。

| # | 矛盾点 | 出处 A | 出处 B | 建议方案 | 决定人 | 状态 |
|---|---|---|---|---|---|---|
| D-01 | 后端包命名 `controller` 还是 `rest` | PRD.md 未明示 | `.claude/CLAUDE.md` §核心约束明确 `rest/` | **以 `.claude/CLAUDE.md` 为准** | Lead | 决议（2026-06-06） |
| D-02 | 是否使用 Tomcat | README.md 提到默认 Undertow | `.claude/CLAUDE.md` §反模式禁用 Tomcat | **README 描述正确，CLAUDE.md 是约束** | Lead | 决议（2026-06-06） |
| D-03 | 设计系统走 Ant Design Vue 还是自研 | PRD.md 未明示 | `.claude/CLAUDE.md` §前端栈锁定 Ant Design Vue 4 | **基于 Ant Design Vue 4 扩展，不自研底层** | designer-vicky | 决议（2026-06-06） |
| D-04 | Vitest 何时启用 | `.claude/CLAUDE.md` §测试：v1.1 再加 | PRD.md 未提及 | **Sprint 0 范围明确不实施 Vitest，留 Sprint 2 评估** | qa-tina | 决议（2026-06-06） |
| D-05 | 默认账号密码是否进 git | README.md 列出 admin/admin123 | `.claude/CLAUDE.md` §反模式禁止明文 | **dev 模式默认账号是文档约定，不算"数据库密码"反模式，但提醒首登必改** | Lead | 决议（2026-06-06） |

## 1. 风险登记矩阵

| 风险 ID | 类别 | 描述 | 概率 | 影响 | 风险值 | 触发条件 | 缓解措施 | Owner | 状态 |
|---|---|---|---|---|---|---|---|---|---|
| R-01 | 技术 | Spring Boot 3.5.7 + JDK 21 组合在团队机器上偶发 ByteBuddy / Lombok 编译失败 | 中 | 高 | **高** | 升级 JDK 或 Spring Boot 大版本 | 锁定 JDK 21.0.2 + Lombok 1.18.34；CI 跑 `./mvnw clean compile` 兜底 | dev-kevin | 监控中 |
| R-02 | 技术 | Knife4j / springdoc-openapi 在 Spring Boot 3.5.x 兼容性问题 | 中 | 中 | 中 | 升级 springdoc 时 | 升级前查 release notes；备选方案 = 退回 2.6.x | dev-kevin | 监控中 |
| R-03 | 技术 | H2 → MySQL 切换时字段类型 / 排序规则差异 | 中 | 中 | 中 | Sprint 2 切 prod profile | dev + ops 联合做 1 次"全表 SELECT"对比 | dev-kevin / ops-max | 待办 |
| R-04 | 技术 | 前端 auto-import 误把第三方组件也注册 | 低 | 中 | 低 | 安装新库 | 维护 `components.d.ts` 白名单；PR 评审卡 | dev-kevin | 缓解中 |
| R-05 | 技术 | Pinia 持久化到 LocalPrivateCache 时 token 加密性能 / 容量 | 低 | 低 | 低 | 用户量大时 | 监控 1k 用户；超 1 万切换方案 | dev-kevin | 监控中 |
| R-06 | 产品 | v1 Product Backlog 估值整体偏乐观（Story Points 基准未校准） | 高 | 中 | **高** | Sprint 1 实际速率 < 估点 60% | Sprint 1 设 2 周观察窗；Sprint 2 调整基准点 | pm-alice / Lead | 待 Sprint 1 数据 |
| R-07 | 产品 | 3 类用户角色卡基于 PRD 假设，未做用户访谈 | 中 | 中 | 中 | 团队按 persona 设计但用户不认 | Sprint 1 前补 3 场访谈（Sprint 2 内执行） | pm-alice | 待办 |
| R-08 | 产品 | "本地扫描 SKILL.md → 入库" 在大量 skill（30+）时性能未验证 | 中 | 中 | 中 | 用户数 > 10 或 skill > 100 | ops 加日志；首次扫描走异步任务 | dev-kevin / ops-max | 监控中 |
| R-09 | 产品 | Markdown 渲染 XSS 风险（SKILL.md 用户提交） | 中 | 高 | **高** | 上线后被注入 | 用 DOMPurify / markdown-it sanitization；QA 加 1 条恶意样例 | dev-kevin / qa-tina | 待解决 |
| R-10 | 团队 | AI 团队成员间响应 SLA 不一致（部分 Agent 拉长上下文） | 中 | 中 | 中 | 跨角色任务需等 > 1 小时 | Lead 30 分钟巡逻；超时切 Lead 协调 | Lead | 监控中 |
| R-11 | 团队 | 制品互相引用不一致（PM Story 名 ≠ Dev 模块名 ≠ Designer 页面名） | 高 | 中 | **高** | Sprint 0 中段 | Lead 强制做"名实校准"动作（见 §3） | Lead | 已规划 |
| R-12 | 外部 | Claude Skills 生态 / 官方 plugin 市场路径变化 | 低 | 中 | 低 | Anthropic 调整目录 | seed 路径配置化（`skillsmap.seed.local-skills-path`），不写死 | ops-max | 缓解中 |
| R-13 | 外部 | 依赖供应链风险（如 jjwt 0.12.x CVE） | 低 | 高 | 中 | 上游公告 CVE | CI 加 OWASP dependency-check（`15_cicd_plan.md` 草图已列） | ops-max | 监控中 |
| R-14 | 外部 | MySQL 8.3 在 Windows 11 兼容性偶发 | 低 | 中 | 低 | local 模式偶发连接失败 | dev 默认走 H2，local 才切 MySQL | dev-kevin / ops-max | 缓解中 |
| R-15 | 产品 | "可一键复制的 install_command" 在不同平台（win/mac/linux）路径差异未文档化 | 中 | 低 | 中 | 用户在 Windows 安装失败 | 详情页加"平台选择"提示 | designer-vicky | 待办 |
| BUG-S0-R01 | 技术 | `MarkdownFrontmatterParser.parse()` 在 SKILL.md frontmatter 含 `: ` 或 ` #`（如 `description: React: pages`）时 SnakeYAML 抛 ScannerException，上传/解析失败 | 高 | 中 | **高** | 用户上传 `web-design-engineer.zip` 等 description 含 `:` 的真实包 | 预清洗：`sanitizeYamlForPlainScalar` 把未引号 plain scalar 用单引号包起来（`'` → `''`），catch 日志带 yamlPart 前 200 字 | dev-kevin | **已修复**（2026-06-06）|
| BUG-S0-R02 | 技术 | `SkillStorageService.saveZipPackage` 在 zip 根目录无 SKILL.md（如 `zip/skill-name/SKILL.md`）时报"未找到 SKILL.md"，`frontend-patterns.zip` 真实包上传失败 | 高 | 中 | **高** | 用户上传 Anthropic 官方格式的 zip（带一个 wrapper 目录） | 新增 `flattenSingleTopLevelDir(dir, maxFileSize)`：预扫大小/路径 → 复制到 `.flatten.tmp/` → 验证 → 删原 sub → move → 兜底清理；4 阶段防护（zip bomb + 越界） | dev-kevin | **已修复**（2026-06-06）|

## 2. 风险评估方法

- **概率**：低（< 20%）/ 中（20–60%）/ 高（> 60%）
- **影响**：低（功能降级）/ 中（功能受阻但有 workaround）/ 高（阻塞 Sprint Goal）
- **风险值**：高 + 高 = **极高** / 高 + 中 / 中 + 高 = **高** / 其他组合按矩阵

## 3. "名实校准"动作流程（针对 R-11）

> 必做：Sprint 0 中段（拟 Sprint 1 D5 同步）由 Lead 主持。

1. 从 `04_product_backlog_v1.md` 抽取**所有页面类 Story**（含 view / list / detail）
2. 对照 `08_information_architecture.md` 的路由表 → 路径名是否一致
3. 对照 `10_tech_architecture.md` 的模块图 → 后端模块名是否对齐 Story 中的名词
4. 不一致项填入下表，**24h 内**出统一命名表

| 原始 Story 用词 | 设计师页面名 | Dev 模块名 | 统一命名 | 决定人 |
|---|---|---|---|---|
| （例）"我的收藏" | `MyFavoritesView` | `favorite` | `MyFavoritesView` + `FavoriteController` | Lead |

## 4. 风险升级触发条件

| 触发 | 动作 |
|---|---|
| 单条风险影响值升到"极高" | 立即召集团队会议（≤ 1h） |
| 高风险 ≥ 3 条同 Sprint 累积 | Mid-sprint Check 单独加 30 分钟专题 |
| 风险连续 2 个 Sprint 状态无变化 | Retrospective 强制讨论"为何未推进" |
| 触发 R-09（XSS）等安全问题 | 立即在制品顶部加"🚨 安全"标识，跳过优先级直接处理 |

## 5. Sprint 0 收口纪要（留空，Sprint 0 末尾填）

> 由 Lead 在 Sprint 0 最后一天填写。

- Sprint 0 实际发生的高风险（≥ 中）：____
- 触发升级的：____
- 缓解成功的：____
- 仍残留的（带入 Sprint 1）：____
- 是否同意 Sprint 0 关闭：____（签名：agile-rd-lead）

## 6. 风险分类与对应缓解责任人速查

> 按"如果发生，找谁"索引。本节便于 Sprint 中段快速定位 Owner。

| 类别 | Owner | 关联风险 ID |
|---|---|---|
| 后端代码 / 编译 / 升级 | dev-kevin | R-01, R-02, R-09 |
| 前端构建 / 自动导入 / Pinia | dev-kevin | R-04, R-05 |
| 数据库 / 切换 | dev-kevin + ops-max | R-03, R-14 |
| 产品范围 / Story 估值 | pm-alice + Lead | R-06, R-07 |
| 用户研究 / 角色卡 | pm-alice | R-07 |
| 视觉 / 跨平台 / 命令 | designer-vicky | R-15 |
| 测试 / 验收 / DoD | qa-tina | R-09（XSS 用例） |
| 部署 / CI / 监控 | ops-max | R-08, R-12, R-13, R-14 |
| 调度 / 协调 / 一致性 | agile-rd-lead | R-10, R-11 |

## 7. 风险与制品的关联

每条风险应在至少 1 份其他制品里有"对应策略"被引用。本节做交叉验证。

| 风险 ID | 引用它的制品 | 引用章节 |
|---|---|---|
| R-01 | `11_dev_onboarding.md` | §8 常见坑（ByteBuddy） |
| R-02 | `15_cicd_plan.md` | §1.1 Stage 1（含依赖检查） |
| R-03 | `14_dev_env_setup.md` | §6 local 模式（MySQL） |
| R-04 | `11_dev_onboarding.md` | §4.2 自动导入 |
| R-05 | `10_tech_architecture.md` | §8 技术债 TD-08 |
| R-06 | `05_sprint1_backlog.md` | §3 容量与假设 |
| R-07 | `06_user_personas.md` | §6 验证计划 |
| R-08 | `10_tech_architecture.md` | §2 启动时序 |
| R-09 | `13_test_strategy.md` | §6 安全测试 + 冒烟索引 |
| R-09 | `12_dod.md` | §6 安全 / 合规维度 |
| R-10 | `02_team_working_agreement.md` | §2 响应 SLA |
| R-11 | `02_team_working_agreement.md` | §3 名实校准 |
| R-12 | `docs/SEED_DATA.md` | §扫描路径（可配置） |
| R-13 | `15_cicd_plan.md` | §7 OWASP dependency-check |
| R-14 | `14_dev_env_setup.md` | §10 常见坑（端口） |
| R-15 | `08_information_architecture.md` | §3 路由表（US-040 平台选择） |
| BUG-S0-R01 | `13_test_strategy.md` | §5.4 Bug 回归 B-2 + §2.1 用例 `MarkdownFrontmatterParserTest` |
| BUG-S0-R02 | `13_test_strategy.md` | §5.4 Bug 回归 B-1 + §2.1 用例 `SkillStorageServiceFlattenTest` |

## 8. 历史变更记录

> 每次 Sprint Retrospective 增补一行，记录"新增 / 关闭 / 升级"风险。

| Sprint | 变更 | 详情 |
|---|---|---|
| Sprint 0 | 初始化 | 15 条风险全部进入监控 |
| Sprint 0 (mid) | 关闭 2 条 | BUG-S0-R01 (YAML `: ` ScannerException) + BUG-S0-R02 (zip 单层上提) 由 dev-kevin 修复，qa-tina 落地 22 个 JUnit 5 用例 + 2 个 curl E2E 验证通过 |

## 9. 应急响应剧本（Playbook）

> 高 / 极高风险条目对应"如果发生，怎么做"的快速指引。

### 9.1 R-09 XSS 触发

1. dev-kevin 立即在 `SkillServiceImpl` 找到 markdown 渲染点，加 DOMPurify
2. qa-tina 加 1 条冒烟（`docs/api-samples/security/xss.sh`）
3. 提交 hotfix，标记 P0，跨过常规 Review 流
4. Lead 在团队频道广播"已修复 + 验证方法"
5. 1 个工作日内出 Postmortem，更新 R-09 状态为"已缓解 + 监控中"

### 9.2 R-11 命名不一致触发

1. Lead 召集 5 角色开 30 分钟专题会
2. 按 `04` / `08` / `10` 三方对齐，输出一致性表（§3 模板）
3. 由 Lead 拍板命名，PM / Designer / Dev 在 24h 内同步制品
4. 不允许"先做后改"，Sprint 1 后续 Story 必须用统一命名

### 9.3 R-01 编译失败触发

1. dev-kevin 切到 Node + JDK 21 锁定版本（参考 `11_dev_onboarding.md` §1.1）
2. 加 `maven-compiler-plugin` 的 `-parameters` 参数
3. 若仍失败 → 切 JDK 21.0.2（用户已知稳定版）
4. 在制品底部留经验记录，附最小可复现 snippet

## 10. 修订记录

| 版本 | 日期 | 作者 | 摘要 |
|---|---|---|---|
| v1.0 | 2026-06-06 | agile-rd-lead | 初版风险登记册：15 条风险 + 5 条决策待办 + 名实校准 + 应急剧本 + 制品关联表 |
