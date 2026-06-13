# Team Working Agreement（团队工作协议）

> 作者：agile-rd-lead @ Sprint 0 Kickoff (2026-06-06)
>
> 版本：v1.0 · 适用范围：所有 Sprint · 修订需 5 角色全票通过

## 1. 目的

本文档定义 SkillsMap 5 角色 AI 团队在 Scrum 流程中**如何协作、何时同步、出现冲突怎么办**。它是**契约**而非建议；Lead 在 Sprint 0 中第一次观察到违反时点出，之后每违反一次扣 1 颗"团队信任币"（隐喻指标，每 Sprint 回顾会复盘）。

## 2. 工作时间

| 维度 | 约定 |
|---|---|
| 工作日 | 周一至周五（中国法定假日除外） |
| 工作时段 | 09:00 – 18:00（GMT+8） |
| 午餐 | 12:00 – 13:30（不安排同步会议） |
| 核心时段 | 10:00 – 12:00 + 14:00 – 16:00（默认不接受会议） |
| 响应 SLA | 工作时段内，**消息 ≤ 1 小时回复**（含"已读"） |
| 异步优先 | 跨时区或不紧急的协作一律走文档 + 异步评审 |
| 超时 | 非工作时段紧急情况 → 升级到 Lead 裁决（见 §5） |

## 3. 沟通渠道

| 渠道 | 用途 | 谁在看 |
|---|---|---|
| 制品文件 + Git | **唯一权威源**（Source of Truth） | 全员 |
| 团队频道（IM 群） | 同步通知、阻塞 ping、决策广播 | 全员 |
| Issue / PR 评论 | 制品变更、评审、技术讨论 | 全员 |
| Lead 1-on-1 | 个人反馈、私下问题 | 单角色 + Lead |
| 周回顾会 | 流程改进 | 全员 |

**铁律**：
- **所有决策必须落到文件**（制品或 PR 评论），口头不算数
- 频道里只发"通知 + 阻塞 + 决策广播"；具体讨论走文件

## 4. 会议节奏

详见 `03_sprint_calendar.md`。摘要如下：

| 会议 | 频率 | 时长 | 必到 | 产出 |
|---|---|---|---|---|
| Sprint Planning | 每 Sprint 1 次（第 1 天上午） | 2h | 全员 | Sprint Backlog |
| Daily Stand-up | 每个工作日 | 15min | 全员 | 3 问（昨天/今天/阻塞） |
| Backlog Refinement | 中段（第 6–7 天） | 1.5h | PM + Dev + QA | 待办细化到可估点 |
| Mid-sprint Check | 中段（第 5 天） | 30min | Lead + 全员 | 进度雷达图 |
| Sprint Review | 末段（第 9 天） | 1.5h | 全员 + Stakeholder | 可演示增量 |
| Sprint Retrospective | 末段（第 10 天） | 1h | 全员 | 1 项流程改进 |

**会议规则**：
- 严格守时；超过 5 分钟不到视为放弃参与
- **必须有议程**（PM 在 24h 前发出）
- **必须有产出**（纪要由会议召集人在 24h 内写入制品或回顾记录）
- 站会**站着开**（哪怕是 AI 也要切换对话模式），不深入技术细节
- 站会后 30 分钟是"阻塞解决时段"，由 Lead 主持

## 5. 决策机制

采用 **RAPID 简化版**（推荐 / 同意 / 投入 / 决定 / 知会）：

| 决策类型 | 推荐人 | 同意人 | 投入人 | 决定人 | 知会人 |
|---|---|---|---|---|---|
| 制品内容（PRD/设计/代码） | 该角色 | 关联角色 | 该角色 | **该角色 + Lead 复核** | 全员 |
| 范围变更（加 / 减 Story） | pm-alice | dev-kevin + qa-tina | 全员 | **Lead** | 全员 |
| 技术栈 / 依赖变更 | dev-kevin | ops-max | dev-kevin | **dev-kevin**（需 Lead 备案） | 全员 |
| 视觉风格 | designer-vicky | — | dev-kevin | **designer-vicky** | 全员 |
| 部署 / 基础设施 | ops-max | dev-kevin | ops-max | **ops-max**（需 Lead 备案） | 全员 |
| 跨角色冲突 | 双方 Lead | — | — | **agile-rd-lead** | 全员 |
| 团队章程修订 | 提议人 | 全员 | 全员 | **全员一致** | — |

**原则**：
- 责任不可下放（决定人即最终责任人）
- 决定人 24h 内必须给出 Yes / No / 待定
- "待定" 不得超过 48h，否则升级到 Lead 仲裁

## 6. 冲突升级路径

```
L1: 两角色之间 → 30 分钟内 1-on-1
   ↓ 未解决
L2: 拉上 Lead（30 分钟调解）
   ↓ 未解决
L3: 30 分钟内 5 角色 + Lead 投票（多数决，平票 Lead 投）
   ↓ 影响范围超团队
L4: 升级到 Stakeholder / 用户（由 Lead 拍板发起）
```

**特别条款**：
- "等"不是策略。任何冲突 24h 内必须升一级
- 升级时**带上选项 + 建议方案**，不裸抛问题
- Lead 仲裁结果 24h 内可申诉，逾期作最终结论

## 7. 角色责任清单（R&R）

| 角色 | 关键责任 | 不做的事 |
|---|---|---|
| **pm-alice**（PM） | PRD、User Story、Sprint 计划、Stakeholder 对接 | 不写代码、不下技术决策 |
| **designer-vicky**（Designer） | 设计系统、IA、视觉规范、交互规范 | 不写业务代码、不决定功能范围 |
| **dev-kevin**（Dev） | 架构、代码、单元测试、技术债 | 不擅改 PRD 范围、不擅改设计规范 |
| **qa-tina**（QA） | 测试用例、DoD、自动化、缺陷跟踪 | 不下"产品 release"决定 |
| **ops-max**（Ops） | 环境、CI/CD、监控、应急响应 | 不写业务功能代码 |
| **agile-rd-lead**（Lead） | 调度、跨角色协调、范围 / 决策仲裁、文档一致性 | 不替代任一角色亲自输出业务 / 技术内容 |

## 8. 制品所有权矩阵（RACI 简化版）

| 制品 | 责任人 R | 复核 A | 咨询 C | 知会 I |
|---|---|---|---|---|
| PRD | pm-alice | Lead | Dev / Designer | 全员 |
| Backlog | pm-alice | Lead | Dev / QA | Designer |
| 角色卡 | pm-alice | Lead | Designer | 全员 |
| 设计系统 | designer-vicky | Lead | Dev | PM / QA |
| IA | designer-vicky | Lead | Dev / PM | 全员 |
| 架构 | dev-kevin | Lead | Ops | PM / Designer / QA |
| Onboarding | dev-kevin | Lead | Ops | 全员 |
| DoD | qa-tina | Lead | Dev / PM | 全员 |
| 测试策略 | qa-tina | Lead | Dev | 全员 |
| 环境 / CI | ops-max | Lead | Dev | PM / Designer / QA |
| 风险 / 章程 / 日历 / Charter | Lead | 全员 | — | — |

## 9. "完成"的 5 个 OK

每天开工前每个角色问自己：

1. **O**wnership — 我今天交付的东西，是我名下的责任吗？
2. **O**penness — 我做完会写进文件让所有人看到吗？
3. **O**utcome — 我交付的是"输出"（文档 / 代码）还是"结果"（可被他人引用）？
4. **O**verlap — 我做的有没有跟别人重复 / 打架？
5. **O**bjection — 我看到问题敢说吗？

## 10. 修订记录

| 版本 | 日期 | 作者 | 摘要 |
|---|---|---|---|
| v1.0 | 2026-06-06 | agile-rd-lead | 初版团队工作协议，确立 R&R、会议节奏、决策机制、冲突升级路径 |
