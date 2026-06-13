# Sprint 0 — Sprint Charter

> 作者：agile-rd-lead @ Sprint 0 Kickoff (2026-06-06)
>
> 版本：v1.0 · 状态：Approved · 适用范围：SkillsMap v1.0 GA 启动

## 1. Sprint 基本信息

| 字段 | 值 |
|---|---|
| Sprint 编号 | **Sprint 0 — Team Initialization** |
| 起止日期 | 2026-06-06 → 2026-06-19 (2 周) |
| Sprint 长度 | 10 个工作日（不含周末） |
| Sprint Goal | **建立可运转的 Scrum 团队 + 产出可上线 v1 的全部规划制品** |
| Sprint 类型 | **奠基型（Foundation）** —— 不交付业务功能，只交付"做事的方式" |
| Lead | agile-rd-lead |
| 团队 | pm-alice / designer-vicky / dev-kevin / qa-tina / ops-max |

## 2. Sprint Goal（Sprint 目标）

> **让一个新人从克隆仓库到能跑通冒烟用例 ≤ 30 分钟，且所有规划文档互相一致可被审计。**

可衡量分解（Definition of Goal Achieved, DoGA）：

1. 仓库根目录出现 `SPRINT_BOARD.md`，链接到 `docs/sprint-0/` 下 15 份制品
2. `docs/sprint-0/04_product_backlog_v1.md` 中每个 Story 都能在 `10_tech_architecture.md` 找到对应模块、在 `08_information_architecture.md` 找到对应页面（如适用）
3. 新成员按 `11_dev_onboarding.md` 走完 → 在 `14_dev_env_setup.md` 的"验证步骤"中所有勾全部打勾
4. `12_dod.md` 中 5 个维度全部为可勾选项（无空文）
5. `09_risk_register.md` 至少登记 12 条风险，含 Owner、缓解措施和触发条件

## 3. Sprint 范围（In-Scope）

| # | 工作项 | 责任角色 | 产物 |
|---|---|---|---|
| 1 | 团队章程（Working Agreement） | Lead | `02_team_working_agreement.md` |
| 2 | Sprint 节奏（日历模板） | Lead | `03_sprint_calendar.md` |
| 3 | v1 Product Backlog 排序 | pm-alice | `04_product_backlog_v1.md` |
| 4 | Sprint 1 候选 Backlog | pm-alice | `05_sprint1_backlog.md` |
| 5 | 用户角色卡 | pm-alice | `06_user_personas.md` |
| 6 | 最小设计系统 v0 | designer-vicky | `07_design_system_v0.md` |
| 7 | 站点信息架构 | designer-vicky | `08_information_architecture.md` |
| 8 | 简版技术架构 | dev-kevin | `10_tech_architecture.md` |
| 9 | 新成员上手手册 | dev-kevin | `11_dev_onboarding.md` |
| 10 | Definition of Done | qa-tina | `12_dod.md` |
| 11 | 测试策略 | qa-tina | `13_test_strategy.md` |
| 12 | 本地开发环境从 0 到 1 | ops-max | `14_dev_env_setup.md` |
| 13 | CI/CD 草图（不实施） | ops-max | `15_cicd_plan.md` |
| 14 | 风险登记册 | Lead | `09_risk_register.md` |
| 15 | 入口与 README 串联 | Lead | `SPRINT_BOARD.md` + `README.md` 追加 |

## 4. Sprint 不做事项（Out-of-Scope）

> 显式声明"不做"以避免范围蔓延。

- ❌ 写任何业务功能代码（无 US-1 ~ US-4 的实现）
- ❌ 实际跑通 CI/CD（仅出 15_cicd_plan.md 草图，不接 GitHub Actions / Jenkins）
- ❌ 真实 Figma 视觉稿（07 仅为设计系统 v0 文档，线条稿占位）
- ❌ 真实用户访谈（06 personas 基于 PRD 假设，需在 Sprint 2 前补 3 场访谈验证）
- ❌ 修改后端 / 前端代码结构（10 架构图与 11 流程图基于现状总结，不重构）
- ❌ 切换数据库（dev 仍用 H2，prod 切换留到 Sprint 2）

## 5. 团队承诺（Team Commitment）

我们（5 角色 + Lead）共同承诺：

1. **可工作增量** —— 每份制品写完即视为"可被他人引用"，引用前 100% 自检
2. **一致性优先** —— PM 提 Story 名 → Designer 提页面名 → Dev 提模块名时，**先在 `04` / `08` / `10` 之间校准**，避免 Sprint 1 实现时发现名实不符
3. **可见性** —— 任何角色遇到阻塞（其他角色输入缺失、上下文矛盾、技术不可行）**30 分钟内**在团队频道 ping Lead，由 Lead 协调
4. **质量门** —— 所有制品必须 ≥ 100 实质性行 + 含"修订记录"表 + 顶部作者署名
5. **可追溯** —— Sprint 0 制品不修改即冻结；如需修改走 PR + 引用本 Charter 的相关章节

## 6. Sprint 容量与假设

| 维度 | 假设 |
|---|---|
| 团队速率（Velocity） | **N/A**（Sprint 0 不发业务 Story，速率为后续 Sprint 计算依据） |
| 工作日历 | 周一至周五，每天 09:00–18:00，午休 12:00–13:30 |
| 公共假日 | 2026-06 不在法定假日内（已查） |
| 角色带宽 | 每个角色 Sprint 0 投入 100%（无其他并行项目） |
| 工具就绪 | GitHub 仓库已建、IDE / Node / JDK 由 Ops 负责（见 14） |

## 7. 风险与依赖摘要

完整风险登记见 `09_risk_register.md`，Sprint 0 级别的关键依赖：

- 7 个上下文文件（PRD/API/ER/SEED_DATA/CLAUDE × 2/README）必须 100% 锁版，不接受 Sprint 0 期间漂移
- 五份角色制品互相引用，**不是完全可并行**——Designer 需 PM 出的页面名，Dev 需 PM 出的模块名，因此 Lead 需在 Sprint 0 中段做"名实校准"动作
- 任何超过 Sprint 0 范围（3. In-Scope）的新需求 → 进 Backlog，Sprint 0 拒绝插入

## 8. Sprint 结束检查（Definition of Sprint Closed）

Sprint 0 视为完成需满足：

- [ ] 15 份制品全部写入 `docs/sprint-0/` 且 ≥ 100 行
- [ ] `SPRINT_BOARD.md` 在仓库根建立，链接全部制品
- [ ] `README.md` 末尾追加"团队工作流"章节
- [ ] Lead 完成"名实一致性审计"（PM Story ↔ Dev 模块 ↔ Designer 页面 三方对齐）
- [ ] 5 角色 Agent 各自在制品底部"修订记录"留下 Sprint 0 签字
- [ ] Lead 在 `09_risk_register.md` 顶部记录"Sprint 0 收口纪要"段落

## 9. 修订记录

| 版本 | 日期 | 作者 | 摘要 |
|---|---|---|---|
| v1.0 | 2026-06-06 | agile-rd-lead | 初版 Sprint Charter，确立 Goal、范围、不做事项、团队承诺 |
