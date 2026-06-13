# Sprint 1 Backlog（Sprint 1 待办）

> 作者：pm-alice @ Sprint 0 Kickoff (2026-06-06)
>
> 版本：v1.0 · Sprint 区间：2026-06-22 → 2026-07-03（2 周）· 假设团队速率：N/A（首个 Sprint 以"探针"为目标准入）

## 1. Sprint 1 目标（Sprint Goal）

> **完成"浏览 + 详情"端到端跑通，公开访问无须登录，冒烟 1-3 全部通过。**

可衡量分解：

- US-001 ~ US-013（13 个浏览/详情 Story）100% 完成 + 冒烟用例 1-3 绿
- 后端 `SkillController` + `SkillService` 模块就位（dev-kevin 完成）
- 前端 `/` `/browse` `/skills/:id` 三个核心路由可用（designer-vicky + dev-kevin 协作）
- 种子数据扫描 30+ skill 入库可被 `/browse` 列出（dev-kevin + ops-max 协作）
- 0 个 P0/P1 缺陷进入下 Sprint

## 2. Sprint 1 候选 Backlog

> 选 Story 原则：①覆盖"端到端最小可发布"；②Must 优先；③估点 ≤ 13；④无高风险依赖

| ID | 标题 | 角色 | SP | 依赖 | 状态 |
|---|---|---|---|---|---|
| US-001 | 首页 Hero 搜索 | dev-kevin + designer-vicky | 2 | IA 已锁 | Ready |
| US-002 | 精选 Skills 板块 | dev-kevin + designer-vicky | 2 | US-011 数据源 | Ready |
| US-003 | 最新 Skills 板块 | dev-kevin | 2 | 同上 | Ready |
| US-004 | 热门 Skills 板块 | dev-kevin | 2 | 同上 | Ready |
| US-005 | 分类入口 | dev-kevin + designer-vicky | 2 | 分类数据 OK | Ready |
| US-006 | 浏览页 - 列表 | dev-kevin + designer-vicky | 3 | US-001~005 完 | Ready |
| US-007 | 浏览页 - 搜索 | dev-kevin | 3 | US-006 | Ready |
| US-008 | 浏览页 - 分类筛选 | dev-kevin | 2 | US-006 | Ready |
| US-009 | 浏览页 - 排序 | dev-kevin | 2 | US-006 | Ready |
| US-010 | 详情页 - 基础信息 | dev-kevin + designer-vicky | 3 | 详情路由 | Ready |
| US-011 | 详情页 - SKILL.md 渲染 | dev-kevin | 5 | markdown-it | Ready |
| US-012 | 详情页 - 安装命令复制 | designer-vicky + dev-kevin | 2 | US-010 | Ready |
| US-013 | 详情页 - 评分列表 | dev-kevin | 2 | review API 已就绪 | Ready |
| US-033 | 详情页 - 浏览数自增 | dev-kevin | 1 | US-010 | Ready |
| US-037 | 后台 - 仪表盘加载骨架屏 | designer-vicky | 2 | — | Could 进（备用） |

**总候选 SP**：31（13 个 Story + 1 个备用）

## 3. 容量与假设

| 维度 | 假设 / 目标 |
|---|---|
| 团队速率（首 Sprint，无历史） | 假设 **25–35 SP**（基于"5 角色 × 2 周 × 70% 产能"） |
| Sprint 1 实际目标 | 25–31 SP（探针，不强求满载） |
| 工作日 | 10 个（2026-06-22 ~ 2026-07-03） |
| 团队投入 | 5 角色 100% + Lead 协调 |
| 已知阻塞 | R-09 XSS（仅详情页有，已纳入 US-011） |
| 设备 / 工具 | dev-kevin / designer-vicky / qa-tina / ops-max 各自有 Node + JDK |

**容量公式**：
> 实际可承受 SP = Σ(角色带宽%) × 历史速率（Sprint 0 无 → 用 5 SP/人/周保守值）

5 角色 × 2 周 × 5 SP = 50 SP 理论上限；考虑会议 / 协作开销打 60% = 30 SP 实际目标。

## 4. DoR 检查（Definition of Ready）

> 入 Sprint 1 的 13 个 Story 已 100% 满足 DoR：

- [x] User Story 含 As a / I want / So that
- [x] 验收标准 ≥ 3 条（见 `04_product_backlog_v1.md`）
- [x] Story Points 已被估出（Planning Poker 假设已跑）
- [x] 依赖已识别（见上表）
- [x] 拆分粒度 ≤ 1 人天（最大 US-011 = 5 SP / 2 天完成）

## 5. Sprint 1 风险

> 来自 `09_risk_register.md` 与本 Sprint 特别风险。

| ID | 风险 | 影响 | 缓解 |
|---|---|---|---|
| SP1-R1 | 首次速率估点无基准，可能整 Sprint 高估 | 中 | 探针 Sprint，每日 stand-up 跟踪，不强求 100% 完成 |
| SP1-R2 | US-011 markdown 渲染未做 XSS 防护 → 上线风险 | **高** | qa-tina 加 1 条恶意样例验收，dev-kevin 用 DOMPurify |
| SP1-R3 | 种子数据扫描 30+ skill 入库耗时长 | 低 | ops-max 改为异步任务，超 30s 走后台 |
| SP1-R4 | 三方命名不一致（PM Story / Dev 模块 / Designer 页面） | 中 | D5 Mid-sprint Check 前 Lead 做一次校准（见 R-11） |
| SP1-R5 | Vite dev 端口 7777 被占用 | 低 | fallback 7778，文档写明 |
| SP1-R6 | 后端 H2 内存库重启后种子重扫 | 低 | dev profile 默认 enabled；prod profile disabled（已约定） |

## 6. 计划 Daily 流转（D1 ~ D10 概要）

| Day | 主要工作 |
|---|---|
| D1 | Sprint Planning → 各角色领取 Story |
| D2 | US-001 / 002 / 003 / 004 实现 |
| D3 | US-005 / 006 / 007 实现 |
| D4 | US-008 / 009 实现 |
| D5 | Mid-sprint Check + US-010 / 011 实现 |
| D6 | US-011 / 012 / 013 实现 |
| D7 | Backlog Refinement（为 Sprint 2 备 Story）+ 修复 |
| D8 | 缺陷修复 + 文档补完 |
| D9 | Sprint Review + 回归测试 |
| D10 | Retro + 部署归档 |

## 7. 退出条件（Definition of Sprint Goal Achieved）

- [ ] 13 个 Story 全部完成 DoD（见 `12_dod.md`）
- [ ] 冒烟用例 1-3（PRD.md §6）全部绿
- [ ] 0 个 P0/P1 缺陷未解决
- [ ] 文档（API / ER / README）随代码同步更新
- [ ] 部署到 staging（ops-max）+ 可对外演示

## 8. 与其他制品的引用

- Story 全文 → `04_product_backlog_v1.md` §2.1
- 路由 / 页面名 → `08_information_architecture.md` §3 路由表
- 后端模块名 → `10_tech_architecture.md` §3 模块图
- 视觉规范 → `07_design_system_v0.md`
- DoD → `12_dod.md`
- 测试策略 → `13_test_strategy.md`
- 风险 → `09_risk_register.md`

## 9. 修订记录

| 版本 | 日期 | 作者 | 摘要 |
|---|---|---|---|
| v1.0 | 2026-06-06 | pm-alice | 初版 Sprint 1 Backlog，13 个 Story 共 31 SP（含 1 个备用） |
