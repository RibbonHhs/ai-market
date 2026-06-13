# Definition of Done（完成定义）

> 作者：qa-tina @ Sprint 0 Kickoff (2026-06-06)
>
> 版本：v1.0 · 适用范围：所有 User Story · 检查时机：Sprint Review + PR 合并前 · 引用：`04_product_backlog_v1.md` / `13_test_strategy.md` / `.claude/CLAUDE.md`

## 0. 使用方式

每条 Story 进入 Review 之前，作者必须按本清单**逐项打勾**。任意一项未勾 → 视为 Not Done，回流到 In Progress。

> Lead / QA 在 Sprint Review 上对每个 Story 抽样核验；Sprint Retrospective 中统计 DoD 通过率，作为下一 Sprint 流程改进输入。

## 1. 代码维度（Code）

> 范围：所有 PR 涉及 `.java` / `.vue` / `.ts` / `.scss` 文件

- [ ] 通过本地 `./mvnw -q clean compile`（后端改动时）
- [ ] 通过本地 `npm run build`（前端改动时）
- [ ] 通过 `npx vue-tsc --noEmit`（前端 TS 检查）
- [ ] 无新增 ESLint 错误（前端）
- [ ] 无新增 Checkstyle / SpotBugs 错误（后端）
- [ ] 无新增 `// FIXME` / `// XXX` / `console.log` 调试残留
- [ ] 无新增 `TODO`（含设计意图的 TODO 需在 PR 描述说明）
- [ ] 无 hardcode 敏感信息（密码 / token / API key）
- [ ] 无 hardcode 业务常量（用 `private static final` / `const`）
- [ ] 无复制粘贴代码（≥ 5 行重复即抽公共方法 / 组件）
- [ ] 新增依赖已写入 `pom.xml` / `package.json` 并说明用途（PR 描述）
- [ ] 依赖升级前查过 release notes（升 major 必须有 Lead 备案）
- [ ] 业务逻辑放在 `service/impl/`，不在 `rest/`（后端铁律）
- [ ] Controller 不直接操作 entity（用 request/response DTO）
- [ ] 异常用 `BizException` + `BizCode`，不抛 `RuntimeException`
- [ ] 数据库字段用 `@TableField` 显式映射 snake_case
- [ ] 逻辑删除字段统一名 `deleted` (Integer, 0/1)
- [ ] 前端 `localStorage` 不存敏感信息（用 LocalPrivateCache + 加密 token）
- [ ] 前端无 `package-lock.json` 之外手改 `node_modules/`
- [ ] 前端组件命名遵循 `SkillCard.vue` / `BrowseView.vue`

## 2. 测试维度（Test）

> 范围：所有 PR 含业务逻辑

- [ ] 核心 service 有 JUnit 5 单测（覆盖正常 + 至少 1 个异常分支）
- [ ] 后端 `./mvnw test` 全部通过
- [ ] 前端冒烟用例索引（见 `13_test_strategy.md`）对应到 PR 的 Story
- [ ] **冒烟用例 ≥ 3 条** 对当前 Story 验证通过
- [ ] 至少 1 条边界用例（空 / 极值 / 错权限）
- [ ] 至少 1 条失败用例（错误码 / 异常路径）
- [ ] 修复 bug 的 PR 含回归测试
- [ ] 测试数据准备 / 清理完整（不留脏数据）
- [ ] 自动化测试不依赖外部服务（mock 掉 HTTP / DB / 时间）
- [ ] 关键接口 Postman / curl 脚本已 commit 到 `docs/api-samples/`（新接口时）

### 2.1 覆盖率目标（Sprint 1 起统计）

| 模块 | 目标 |
|---|---|
| Service 层核心方法 | 行覆盖 ≥ 70% |
| Rest 层 happy path | 100% |
| 安全相关（auth / admin） | 100% |
| 通用工具类 | ≥ 80% |

## 3. 文档维度（Documentation）

> 范围：所有 PR

- [ ] PR 描述含：目的 / 主要改动 / 关联 Story ID / 截图（如有 UI 改动）
- [ ] 涉及 API 变更 → `docs/API.md` 已同步
- [ ] 涉及数据模型变更 → `docs/ER.md` 已同步
- [ ] 涉及范围 / 验收标准变更 → `04_product_backlog_v1.md` 已同步
- [ ] 涉及环境变量 / 配置变更 → `.env.example` + `docs/sprint-0/14_dev_env_setup.md` 已同步
- [ ] 涉及架构变更 → `10_tech_architecture.md` 已同步
- [ ] 涉及路由 / 页面变更 → `08_information_architecture.md` 已同步
- [ ] 涉及视觉规范变更 → `07_design_system_v0.md` 已同步
- [ ] 新增复杂逻辑有行内注释（**为什么**而非**做什么**）
- [ ] public 方法 / 导出的函数有 JSDoc / JavaDoc

## 4. 部署维度（Deployment）

> 范围：所有 PR 涉及可运行代码

- [ ] 后端可通过 `./mvnw clean package` 打包成功
- [ ] 前端可通过 `npm run build` 构建成功
- [ ] 启动后无 ERROR 级别日志
- [ ] H2 dev profile 启动后自动建表 + 扫种子 OK
- [ ] 启动后 API `/actuator/health` 返回 `UP`（如配）
- [ ] Knife4j `/doc.html` 能正常打开且包含新接口
- [ ] 前端 dev server `npm run dev` 启动无控制台 error
- [ ] 关键页面冒烟 1 次手动验证（截图附 PR 描述）
- [ ] 至少 1 个角色 reviewer 通过（PR Approve）
- [ ] 涉及数据库 schema 变更 → 写了回滚 SQL 或说明（v1 dev 模式允许下次启动重扫）

## 5. 评审维度（Review）

> 范围：所有 PR

- [ ] PR 标题遵循 `<type>(<scope>): <subject>` 格式（conventional commits）
  - 例：`feat(skill): 详情页加 markdown 渲染`
  - 例：`fix(auth): token 过期 401 不跳登录`
  - 例：`docs(prd): 更新验收用例`
- [ ] PR 单文件改动 ≤ 400 行（超大改动拆 PR）
- [ ] PR 描述关联 `Sprint-{N}` 与 Story ID
- [ ] 无 merge conflict
- [ ] CI 全绿（如已配 CI；见 `15_cicd_plan.md`）
- [ ] 至少 1 个 reviewer Approve（非作者本人）
- [ ] 跨角色改动拉对应角色评审（如后端 PR 改 PRD → 拉 pm-alice）
- [ ] 涉及风险登记册（R-XX）的风险已更新或备注

## 6. 安全 / 合规维度（Security & Compliance）— 加分但必选其一标记

> 范围：涉及用户输入 / 鉴权 / 公开内容

- [ ] 输入校验（前后端双层）
- [ ] SQL 注入检查（用参数化，不拼接）
- [ ] XSS 防护（用户提交 markdown 走 DOMPurify 等 sanitizer）
- [ ] CSRF（前后端分离 + JWT 不需要 CSRF token，但写操作验证来源 origin）
- [ ] 鉴权检查（受保护资源未授权返回 401/403）
- [ ] 密码用 BCrypt 存储（不存明文）
- [ ] 敏感信息不在日志 / URL 中
- [ ] 默认密码强制首登修改（admin/admin123 等）
- [ ] 上传文件类型 / 大小限制

## 7. 性能维度（Performance）— 非强制，Sprint 1 末统计

> 范围：列表 / 详情 / 搜索

- [ ] 列表页 P95 ≤ 300ms
- [ ] 详情页 P95 ≤ 500ms
- [ ] 搜索（关键词）P95 ≤ 300ms
- [ ] 索引：核心查询字段（`name` / `slug` / `category_id` / `status`）有索引
- [ ] 列表分页有 max 上限（≤ 100）
- [ ] 大字段（`body`）不在列表接口返回

## 8. 可访问性维度（A11y）— 适用于所有 UI 改动

- [ ] 文本对比 ≥ 4.5:1（手测或用工具）
- [ ] 所有交互元素 ≥ 44×44 pt
- [ ] 可见 focus ring
- [ ] 表单有 label
- [ ] 按钮有可读文字（无 icon-only 不带 aria-label）
- [ ] 图片有 alt
- [ ] 错误信息在字段附近（不在顶部统一报）
- [ ] 键盘可导航（Tab 顺序 = 视觉顺序）

## 9. Story-level DoD 模板（PR 描述末尾粘贴）

```markdown
## DoD Checklist

### Code
- [ ] 后端 ./mvnw clean compile 通过
- [ ] 前端 npm run build 通过
- [ ] 无新增 ESLint / Checkstyle 错误
- [ ] 无 console.log / FIXME 残留
- [ ] 业务逻辑在 service 层

### Test
- [ ] Service 单测覆盖正常 + 异常
- [ ] ./mvnw test 全绿
- [ ] 冒烟用例 1 / 2 / 3 通过（截图）
- [ ] 边界用例：____
- [ ] 失败用例：____

### Doc
- [ ] PR 描述含 Story ID 与关联
- [ ] docs/API.md 同步（如涉及）
- [ ] 04_product_backlog_v1.md 已勾选 Story

### Deploy
- [ ] ./mvnw clean package 成功
- [ ] 本地 dev profile 启动 OK
- [ ] /doc.html 含新接口（如涉及）
- [ ] 截图附 PR 描述

### Review
- [ ] 标题遵循 conventional commit
- [ ] 单文件 ≤ 400 行
- [ ] 至少 1 reviewer Approve
- [ ] 无 merge conflict
```

## 10. 例外与豁免

- **纯文档 PR**（仅改 .md）→ §1 §2 §4 自动勾选，只需 §3 §5
- **纯配置 PR**（仅改 yml / vite.config 等）→ §2 跳过测试，§1 跳过代码规范
- **Hotfix**（生产事故）→ §2 §3 §5 走快速通道（1 reviewer 即可），但 §4 部署后必须补回 §2 测试
- 任何例外需在 PR 描述写"豁免项"并由 Lead 批准

## 11. 与 Sprint Review 联动

Sprint Review 上：

1. 演示者按 §9 模板自述 DoD 通过项
2. QA 抽样 ≥ 1 个 Story 重跑冒烟
3. 任一 §必须项未通过 → Story 不算 Done
4. Sprint Done 率 = DoD 全过 Story 数 / 计划 Story 数

## 12. 修订记录

| 版本 | 日期 | 作者 | 摘要 |
|---|---|---|---|
| v1.0 | 2026-06-06 | qa-tina | 初版 DoD：5 大维度（代码/测试/文档/部署/评审）+ 安全 / 性能 / A11y 加分维度 + 例外条款 |
