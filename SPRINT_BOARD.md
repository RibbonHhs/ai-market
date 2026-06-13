# SkillsMap — Sprint Board

> Sprint 0（2026-06-06 → 2026-06-19）— Team Initialization
>
> Lead: agile-rd-lead · 团队：pm-alice / designer-vicky / dev-kevin / qa-tina / ops-max

本入口汇总 **Sprint 0 的全部 15 份规划制品**。所有制品位于 `docs/sprint-0/`，由对应角色在 Sprint 0 Kickoff 时签字产出。

## 1. Sprint 0 一句话目标

> **让一个新人从克隆仓库到跑通冒烟用例 ≤ 30 分钟，且所有规划文档互相一致可被审计。**

## 2. 制品索引（15 份）

### 2.1 Lead 制品（4 份）

| # | 制品 | 摘要 |
|---|---|---|
| 01 | [Sprint Charter](docs/sprint-0/01_sprint_charter.md) | Sprint Goal / 范围 / 不做事项 / 团队承诺 |
| 02 | [Team Working Agreement](docs/sprint-0/02_team_working_agreement.md) | 工作时间 / 沟通渠道 / 会议节奏 / 决策机制 / 冲突升级 |
| 03 | [Sprint Calendar](docs/sprint-0/03_sprint_calendar.md) | 2 周 Sprint 节奏：计划会 / 站会 / 评审 / 回顾 / Refinement |
| 09 | [Risk Register](docs/sprint-0/09_risk_register.md) | 15 条风险 + 5 条决策待办 + 名实校准流程 |

### 2.2 PM 制品（3 份）

| # | 制品 | 摘要 |
|---|---|---|
| 04 | [Product Backlog v1](docs/sprint-0/04_product_backlog_v1.md) | 50 个 Story（Must=30 / Should=7 / Could=7 / Won't=6），MoSCoW 排序，143 SP |
| 05 | [Sprint 1 Backlog](docs/sprint-0/05_sprint1_backlog.md) | 13 个 Story 候选，31 SP（含 1 个备用），目标 25–31 SP 探针 |
| 06 | [User Personas](docs/sprint-0/06_user_personas.md) | 3 类角色卡：开发者 / 创作者 / 运营 |

### 2.3 Designer 制品（2 份）

| # | 制品 | 摘要 |
|---|---|---|
| 07 | [Design System v0](docs/sprint-0/07_design_system_v0.md) | 色板 / 字体 / 间距 / 圆角 / 阴影 / 组件清单（基于 AntDV 4 扩展） |
| 08 | [Information Architecture](docs/sprint-0/08_information_architecture.md) | 导航树 / 22 条路由表 / 4 个关键页面线框 / 组件层级 |

### 2.4 Dev 制品（2 份）

| # | 制品 | 摘要 |
|---|---|---|
| 10 | [Tech Architecture](docs/sprint-0/10_tech_architecture.md) | 模块图 / 启动时序 / 鉴权流程 / 8 条技术债 / 性能预算 |
| 11 | [Dev Onboarding](docs/sprint-0/11_dev_onboarding.md) | 30 分钟跑通 + 仓库结构 + 前后端约定 + 命令 + 调试 + 常见坑 |

### 2.5 QA 制品（2 份）

| # | 制品 | 摘要 |
|---|---|---|
| 12 | [Definition of Done](docs/sprint-0/12_dod.md) | 5 维度清单：代码 / 测试 / 文档 / 部署 / 评审 + 安全 / 性能 / A11y 加分维度 |
| 13 | [Test Strategy](docs/sprint-0/13_test_strategy.md) | 分层（单测 / 集成 / API / E2E / 安全）+ 工具 + 10 条冒烟用例索引 |

### 2.6 Ops 制品（2 份）

| # | 制品 | 摘要 |
|---|---|---|
| 14 | [Dev Environment Setup](docs/sprint-0/14_dev_env_setup.md) | 本地开发从 0 到 1：JDK 21 / Node 20 / 启动 / 验证 / local MySQL |
| 15 | [CI/CD Plan](docs/sprint-0/15_cicd_plan.md) | 4 阶段草图：PR Check → Build & Push → Staging → Prod（不实施） |

## 3. 核心团队契约（3 条）

> 完整版见 [02_team_working_agreement.md](docs/sprint-0/02_team_working_agreement.md)

1. **可工作增量** —— 每份制品写完即视为"可被他人引用"，引用前 100% 自检
2. **一致性优先** —— PM Story / Designer 页面 / Dev 模块三方命名先校准再实现
3. **可见性** —— 任何角色遇阻塞 30 分钟内 ping Lead，24h 内冲突必须升级

## 4. Sprint 1 候选 Backlog 总览

> 详细见 [05_sprint1_backlog.md](docs/sprint-0/05_sprint1_backlog.md)

| 维度 | 值 |
|---|---|
| Sprint Goal | 完成"浏览 + 详情"端到端跑通，公开访问无须登录，冒烟 1-3 全部通过 |
| 候选 Story 数 | 13 + 1 备用 |
| 候选总 SP | 31 |
| 目标 SP（探针） | 25–31 |
| 周期 | 2026-06-22 → 2026-07-03 |

13 个候选 Story：US-001/002/003/004/005/006/007/008/009/010/011/012/013/033

## 5. Top 3 已识别风险

> 完整 15 条 + 5 条决策待办见 [09_risk_register.md](docs/sprint-0/09_risk_register.md)

| ID | 风险 | 等级 | Owner |
|---|---|---|---|
| **R-09** | markdown 渲染 XSS 风险（SKILL.md 用户提交） | **高** | dev-kevin + qa-tina |
| **R-11** | 制品互相引用不一致（PM Story / Dev 模块 / Designer 页面命名） | **高** | agile-rd-lead |
| **R-01** | Spring Boot 3.5.7 + JDK 21 偶发 ByteBuddy / Lombok 编译失败 | **高** | dev-kevin |

## 6. 制品互相引用关系图

```
┌─────────────────────────────────────────────────────────────┐
│  01 Charter ─┐                                               │
│  02 Working  │                                               │
│  03 Calendar │── Lead 总章 ──┐                               │
│  09 Risk     ┘               │                               │
├──────────────────────────────┼──────────────────────────────┤
│  04 Backlog ─┐               │                               │
│  05 Sprint1  │── PM 待办 ────┤                               │
│  06 Personas ┘               │                               │
├──────────────────────────────┼──────────────────────────────┤
│  07 Design  ─┐               │                               │
│  08 IA       │── Designer 规范┤                              │
├──────────────┼──────────────┼──────────────────────────────┤
│  10 Arch    ─┤               │                               │
│  11 Onboard  │── Dev 实现 ───┤                               │
├──────────────┼──────────────┼──────────────────────────────┤
│  12 DoD     ─┤               │                               │
│  13 Test    ─┤── QA 验收 ────┤                               │
├──────────────┼──────────────┼──────────────────────────────┤
│  14 Env     ─┤               │                               │
│  15 CI/CD   ─┤── Ops 部署 ───┘                               │
└──────────────┴──────────────────────────────────────────────┘
```

**关键引用**：

- 04 Story → 08 路由（前端页面名） + 10 模块（后端模块名）
- 08 路由 → 07 视觉规范
- 10 模块 → 11 启动 / 调试命令
- 12 DoD → 13 测试策略 + 14 验证步骤
- 15 CI/CD → 12 DoD 的"评审维度"
- 09 风险 ← 所有制品（更新时同步检查）

## 7. Sprint 0 收口清单（DoSC）

- [x] 15 份制品全部写入 `docs/sprint-0/` 且 ≥ 100 行
- [x] `SPRINT_BOARD.md` 在仓库根建立
- [x] `README.md` 末尾追加"团队工作流"章节
- [x] 所有制品顶部有作者署名 + 修订记录表
- [x] 风险登记册含 15 条风险 + 5 条决策待办
- [x] Sprint 1 Backlog 含容量、假设、DoR、退出条件

## 8. 引用：项目核心文档

| 文档 | 链接 |
|---|---|
| 产品需求 | [docs/PRD.md](docs/PRD.md) |
| REST API | [docs/API.md](docs/API.md) |
| 数据模型 | [docs/ER.md](docs/ER.md) |
| 种子数据 | [docs/SEED_DATA.md](docs/SEED_DATA.md) |
| 团队规约 | [.claude/CLAUDE.md](.claude/CLAUDE.md) |

## 9. 修订记录

| 版本 | 日期 | 作者 | 摘要 |
|---|---|---|---|
| v1.0 | 2026-06-06 | agile-rd-lead | 初版 Sprint Board：15 份制品索引 + 3 条契约 + Sprint 1 总览 + Top 3 风险 + 引用图 |
