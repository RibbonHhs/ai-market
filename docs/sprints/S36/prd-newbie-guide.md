# PRD — S36 新手指引（Newbie Guide）

> **Sprint**: S36
> **作者**: PM (agile-rd-team)
> **日期**: 2026-06-13
> **状态**: Draft v1

---

## 1. 背景与问题

SkillsMap 是 Agent Skills 集市，新用户（人类 + 智能体开发者）首次到访后常问三个问题：

1. Skill 到底是个啥东西？
2. 我装到 Claude Code 里该怎么用？
3. 想程序化访问有什么 API？

现在首页 `HomeStats` 里的「第一次使用 Skill?」卡片是一条很弱的 CTA，点击直接跳 browse，没有回答上述问题。本期要补齐这条新手指引链路。

## 2. 目标 / 非目标

### 2.1 目标
- 在首页显眼的「开始新手指引 →」入口
- 一条「3 分钟上手」引导页 `/newbie-guide`
- 引导页不重写 `ApiGuideView`，仅作 CTA 跳转

### 2.2 非目标
- 不重写 ApiGuideView
- 不动 HomeStats / HomeFeatured / HomeHot 任何一个
- 不引入新依赖

## 3. User Story

| ID | As a | I want to | So that |
|----|------|-----------|---------|
| US-1 | 首次访客（人类） | 在首页一眼看到「第一次使用 Skill?」入口 | 不再迷茫从哪开始 |
| US-2 | 首次访客 | 点击入口后进入 3 步引导页 | 3 分钟搞懂 Skill 是什么、怎么装 |
| US-3 | 智能体开发者 | 在引导页看到 4 步 skills-manager 安装教程 | 能直接跟着命令装到 Claude Code |
| US-4 | 接入方开发者 | 在引导页 §3 看到「前往完整 API 指南」CTA | 一键跳到完整 API 文档 |
| US-5 | 暗色态用户 | 引导页和卡片在系统暗色下可读 | 不被白底刺眼 |

## 4. 功能详述

### 4.1 首页「开始新手指引」卡片（新增组件 `HomeOnboarding.vue`）

- **位置**：`HomeView` 在 `HomeStats` 与 `HomeFeatured` 之间
- **视觉**：单行卡片，与 `HomeHero` 紫色调呼应；图标 + 一句话 + CTA 按钮
- **文案**：
  - 标题：「第一次使用 Skill?」
  - 副文：「不知道从哪开始？3 分钟带你了解 Skills 是什么、怎么安装、怎么用 API 接入。」
  - CTA：「开始新手指引 →」
- **行为**：点击 → `router.push({ name: 'newbie-guide' })`

### 4.2 `NewbieGuideView.vue` 引导页（新增）

- **路由**：`/newbie-guide`，name `newbie-guide`
- **布局**：沿用 `ApiGuideView` 的「Hero + 锚点导航 + 多 card」结构
- **顶部 Hero**：标题「新手指引 · 3 分钟上手 SkillsMap」+ 一段 lede
- **锚点导航**：`Skill 是什么` / `Skills Manager 使用说明` / `API 接入`

#### §1 Skill 是什么
- 用 Markdown 解释：Agent Skill 是一段带 `SKILL.md` 的可复用提示/工具包
- 含一段示例 SKILL.md frontmatter + body 的代码块
- 含一段示例 manifest.json

#### §2 Skills Manager 使用说明
- 把 `HomeHero` agent tab 那段 4 步教程搬过来
- 步骤 1：下载 skill 包（按钮调 `fetch('/api/skills/slug/skills-manager/download')`）
- 步骤 2：解压到 `~/.claude/skills/skills-manager/`（含 shell 代码块）
- 步骤 3：重启 Claude Code
- 步骤 4：试试这些指令（3 条 prompt 建议）

#### §3 API 接入（**不复制** ApiGuideView）
- 跳转卡片：标题「API 接入」+ 描述「想程序化访问 SkillsMap？查看完整 REST 文档：端点列表、参数、响应字段、示例。」
- CTA 按钮：「前往完整 API 接入指南 →」 → `router.push({ name: 'api-guide' })`
- 卡片内简要列 3-5 个常用端点（小列表 / chip）：
  - `GET /api/skills` — 列表 / 搜索
  - `GET /api/skills/slug/{slug}` — 详情
  - `GET /api/skills/slug/{slug}/download` — 下载
  - `GET /api/skills/hot` — 热门
  - `GET /api/categories` — 分类

## 5. 验收标准（AC）

| ID | 标准 | 优先级 |
|----|------|--------|
| AC-1 | 首页能看见新「开始新手指引 →」卡片（位置：HomeStats 与 HomeFeatured 之间） | P0 |
| AC-2 | 点击卡片跳到 `/newbie-guide`，title 显示「新手指引」 | P0 |
| AC-3 | 引导页顶部 Hero 标题 + lede 可见 | P0 |
| AC-4 | 锚点导航 3 个 pill 可见，点击对应区块平滑滚动 | P0 |
| AC-5 | §1「Skill 是什么」可见，含 SKILL.md 代码块 | P0 |
| AC-6 | §2「Skills Manager 使用说明」4 步教程可见，下载按钮可点击 | P0 |
| AC-7 | §3「API 接入」跳转卡片可见，CTA 点击跳到 `/api-guide` | P0 |
| AC-8 | 暗色态（系统 / data-theme="dark"）下页面可读，无白底 / 写死颜色 | P0 |
| AC-9 | 移动端（viewport 375px）布局正常，无横向滚动，卡片可堆叠 | P1 |
| AC-10 | `npm run build` 通过，无 TS error | P0 |

## 6. 风险与决策

| 风险 | 缓解 |
|------|------|
| 与 `HomeStats` 已有的「第一次使用 Skill?」卡片重复 | **决策**：保留旧的（弱化到只跳 browse），新增的才是本次主角；后续 Sprint 清理 |
| 引导页和 ApiGuideView 内容同源 | 引导页 §3 只做 CTA 跳转，不复制表格 |
| 移动端卡片内 CTA 按钮文字溢出 | 移动端断点下 `flex-direction: column`，按钮 `align-self: stretch` |

## 7. Out of Scope

- 不做引导页的多语言切换（保留中文 v1）
- 不做 Step 进度条（v2 可加）
- 不做用户角色分流（人类 vs 智能体统一一个引导页）
- 不做埋点（v1 跳过）

## 8. DoD（Definition of Done）

- 代码合并到 `master`，`npm run build` 通过
- 5 个 AC 全部勾选
- 截图归档到 `docs/sprints/S36/screenshots/`
- handoff.md 写完
