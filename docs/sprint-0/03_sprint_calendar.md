# Sprint Calendar（Sprint 节奏日历）

> 作者：agile-rd-lead @ Sprint 0 Kickoff (2026-06-06)
>
> 版本：v1.0 · 适用范围：所有 2 周 Sprint · 时区：GMT+8 (Asia/Shanghai)

## 1. 时间盒（Time-box）总览

| Sprint 阶段 | 工作日 | 关键会议 | 角色密集度 |
|---|---|---|---|
| 启动（Start） | D1（周一） | Sprint Planning | 全员 |
| 构建（Build） | D2 – D6 | Daily Stand-up × 5 | Dev / QA / Designer |
| 中段（Mid-sprint） | D5（周四） | Mid-sprint Check + Refinement | PM + Dev + QA |
| 收尾（Close） | D9（周二） | Sprint Review | 全员 + Stakeholder |
| 复盘（Retro） | D10（周三） | Sprint Retrospective | 全员 |
| 缓冲 | D6 / D10 下午 | Code Freeze / 文档冻结 | Dev / PM |

## 2. 两周 Sprint 详细日程

> 假设 Sprint 1 = 2026-06-22（周一）→ 2026-07-03（周五）
> Sprint 0 节奏对齐 Sprint 1，但制品为文档而非代码

| 日期 | 工作日 | 时段 | 活动 | 必到 | 主持人 | 产出 |
|---|---|---|---|---|---|---|
| D1 (06-22) Mon | 1 | 09:30–11:30 | **Sprint Planning** | 全员 | pm-alice | Sprint Backlog 锁版 |
| D1 | 1 | 14:00–17:00 | 各角色领取 Story | 各角色 | — | 个人 WIP 列表 |
| D2 (06-23) Tue | 2 | 09:00–09:15 | Daily Stand-up #1 | 全员 | 轮值 | 站会纪要 |
| D2 | 2 | 09:15–18:00 | 构建 / 编码 / 设计 | 各自 | — | 增量 |
| D3 (06-24) Wed | 3 | 09:00–09:15 | Daily Stand-up #2 | 全员 | 轮值 | 站会纪要 |
| D4 (06-25) Thu | 4 | 09:00–09:15 | Daily Stand-up #3 | 全员 | 轮值 | 站会纪要 |
| D5 (06-26) Fri | 5 | 09:00–09:15 | Daily Stand-up #4 | 全员 | 轮值 | 站会纪要 |
| D5 | 5 | 10:00–10:30 | **Mid-sprint Check** | 全员 | agile-rd-lead | 进度雷达图 |
| D5 | 5 | 14:00–15:30 | **Backlog Refinement #1** | pm-alice + dev-kevin + qa-tina | pm-alice | 后续 Sprint 候选 Story |
| D6 (06-29) Mon | 6 | 09:00–09:15 | Daily Stand-up #5 | 全员 | 轮值 | 站会纪要 |
| D6 | 6 | 16:00 起 | **Code / 文档冻结** | — | — | — |
| D7 (06-30) Tue | 7 | 09:00–09:15 | Daily Stand-up #6 | 全员 | 轮值 | 站会纪要 |
| D7 | 7 | 14:00–15:30 | **Backlog Refinement #2** | pm-alice + dev-kevin + qa-tina | pm-alice | Sprint 2 候选 Story 锁版 |
| D8 (07-01) Wed | 8 | 09:00–09:15 | Daily Stand-up #7 | 全员 | 轮值 | 站会纪要 |
| D8 | 8 | 14:00–17:00 | 缺陷修复 / 文档补完 | dev-kevin + qa-tina | — | 修复记录 |
| D9 (07-02) Thu | 9 | 09:00–09:15 | Daily Stand-up #8 | 全员 | 轮值 | 站会纪要 |
| D9 | 9 | 10:00–11:30 | **Sprint Review** | 全员 + Stakeholder | pm-alice | 演示 + 验收记录 |
| D9 | 9 | 14:00–17:00 | 回归测试 / 验收 | qa-tina | — | 测试报告 |
| D10 (07-03) Fri | 10 | 09:00–09:15 | Daily Stand-up #9 | 全员 | 轮值 | 站会纪要 |
| D10 | 10 | 10:00–11:00 | **Sprint Retrospective** | 全员 | agile-rd-lead | 1 项流程改进入 Backlog |
| D10 | 10 | 14:00–17:00 | 部署 / 文档归档 / 准备下 Sprint | ops-max + pm-alice | — | 部署报告 |

## 3. Daily Stand-up 模板

每个工作日 09:00 – 09:15，时长 15 分钟硬上限。

每角色 1 分钟，按顺序发言：

```
1. 昨天交付了什么（粘 Git 链接 / 文件路径）
2. 今天计划做什么
3. 阻塞是什么（具体到缺谁、缺什么输入）
```

**轮值主持人**：D2 = dev-kevin / D3 = qa-tina / D4 = designer-vicky / D5 = ops-max / D6 = pm-alice；D7 重新开始。

**纪要模板**：写到 `docs/sprint-{N}/standup/YYYY-MM-DD.md`，含日期 / 在场 / 三问。

## 4. Sprint Planning 模板

时长 2 小时，分两段：

### 第一段：Sprint Goal（30 min）

- pm-alice 提议 Sprint Goal
- 5 角色 + Lead 讨论 → 锁定

### 第二段：Sprint Backlog（90 min）

- pm-alice 展示 Top 候选 Story（按 `04_product_backlog_v1.md`）
- dev-kevin / qa-tina 估点（Planning Poker）
- 容量 vs 速度历史值对比 → 砍 Story
- 各角色领取 → 写入 `docs/sprint-{N}/sprint{N}_backlog.md`

**DoR（Definition of Ready）**检查：
- [ ] User Story 含 As a / I want / So that
- [ ] 验收标准 ≥ 3 条 Given-When-Then 或勾选项
- [ ] Story Points 已被团队估出
- [ ] 依赖已识别
- [ ] 拆分粒度 ≤ 1 人天（> 1 天强制拆）

## 5. Backlog Refinement 模板

每 Sprint 中段 2 次，每次 1.5 小时。

**输入**：
- 当前 Sprint 进行中的 Story（拆解 / 复审）
- 下 Sprint 候选 Story（细化 + 估点）

**输出**：
- 当前 Sprint 中需要重新估点 / 拆分的 Story 列表
- 下 Sprint 候选 Story 细化版（满足 DoR）

**出席**：
- 必到：pm-alice / dev-kevin / qa-tina
- 列席（可选）：designer-vicky / ops-max

## 6. Mid-sprint Check 模板

D5 第 5 个工作日，30 分钟，Lead 主持。

**进度雷达图**（5 维）：

| 维度 | 红 / 黄 / 绿 | 备注 |
|---|---|---|
| 范围（Scope） | — | 计划 vs 实际 |
| 速率（Velocity） | — | SP 完成率 |
| 质量（Quality） | — | 缺陷数 / DoD 通过率 |
| 风险（Risk） | — | 来自 `09_risk_register.md` |
| 士气（Morale） | — | 1-5 分自评 |

任一维度红 → Lead 立即召集"范围 / 人员"调整讨论。

## 7. Sprint Review 模板

D9 第 9 个工作日，1.5 小时。

**议程**：
- 09:00–09:10 Sprint Goal 回顾（pm-alice）
- 09:10–10:30 **Demo**（按 Story 顺序，每角色 5–10 分钟）
- 10:30–10:50 Stakeholder 反馈 / Q&A
- 10:50–11:10 验收 / 接受决定
- 11:10–11:30 文档归档（ops-max）

**Demo 硬性要求**：
- 必须展示**运行中**的功能（在 dev 环境或 staging）
- 不接受截图 / PPT
- 每个 Story 必须对照 `05_sprint1_backlog.md` 中的验收标准

## 8. Sprint Retrospective 模板

D10 第 10 个工作日，1 小时，Lead 主持。

**4 Lenses**（每项 10 分钟）：
1. **What went well?** 做得好的（保留）
2. **What didn't go well?** 不好的（改进）
3. **What puzzled us?** 困惑的（实验）
4. **What will we commit to change?** 1 项下 Sprint 必改的

**铁律**：每次 Retro 产出 **不超过 2 项行动项**（多了等于没有）。

**行动项** 写入下个 Sprint 的 `01_sprint_charter.md` 顶部"持续改进"区。

## 9. 例外与变更

- **法定假日** → Sprint 长度自动延长对应天数（在 `01_sprint_charter.md` 注明）
- **团队请假 ≥ 2 人** → Lead 决定是否缩减 Sprint Goal（30 分钟内出决议）
- **外部紧急需求** → 走范围变更流程（见 `02_team_working_agreement.md` §5）

## 10. 工具与地点

| 会议类型 | 工具 | 录制 |
|---|---|---|
| 同步会议 | 视频会议 + 共享屏幕 | 必须录制（异步回看） |
| 异步评审 | GitHub PR / 文件评论 | 评论即记录 |
| 文档协作 | Markdown + Git | 历史即记录 |
| 站会 | 视频 + 共享文本 | 不录制（轻量） |

## 11. 修订记录

| 版本 | 日期 | 作者 | 摘要 |
|---|---|---|---|
| v1.0 | 2026-06-06 | agile-rd-lead | 初版 Sprint 节奏日历，2 周 / 5 会议 / 4 仪式模板 |
