# S36 — 新手指引（Newbie Guide）

> **Sprint**: S36
> **目标**: 为首次访问 SkillsMap 的用户打造一条「3 分钟上手」的引导路径，并在首页给一个明显入口。
> **分支**: `master`（直接推进）
> **日期**: 2026-06-13

---

## 1. 业务背景

当前首页「第一次使用 Skill?」卡片（位于 `HomeStats` 左半）只是一条 CTA，点击直接跳到 browse 页，没有真正介绍 Skill 是什么、怎么装、API 怎么接。S36 要做的是：

1. **新增一个独立的「开始新手指引」卡片**，紧跟在 `HomeStats` 与 `HomeFeatured` 之间，更醒目。
2. **新增一个完整的新手指引页** `/newbie-guide`，3 个区块：
   - §1 Skill 是什么
   - §2 Skills Manager 使用说明（迁移 HomeHero agent tab 的 4 步教程）
   - §3 API 接入（CTA 跳转 `/api-guide`，**不复制** ApiGuideView 内容）

## 2. 范围 / 反模式

### 2.1 范围内
- 新增 `HomeOnboarding.vue` 卡片组件
- 新增 `NewbieGuideView.vue` 引导页
- 新增路由 `/newbie-guide`（name `newbie-guide`）
- `HomeView` 在 `HomeStats` 与 `HomeFeatured` 之间挂载 `HomeOnboarding`

### 2.2 范围外（Do NOT）
- ❌ 替换 / 删除 `HomeStats` / `HomeFeatured` / `HomeHot` 任何一个
- ❌ 重写 `ApiGuideView`（只在新手指引里加 CTA 跳过去）
- ❌ 引入新依赖（Ant Design Vue Button / Card / Typography 已够用）
- ❌ 使用 Element Plus（项目规约禁用）

## 3. 角色分工

| 角色 | 任务 | 产物 |
|------|------|------|
| PM | 写 PRD（User Story + 验收标准） | `prd-newbie-guide.md` |
| Designer | 出设计稿（色板 / 间距 / 交互态） | `design-newbie-guide.md` |
| Dev | 实施 Vue 代码 + 路由 | `HomeOnboarding.vue` / `NewbieGuideView.vue` / 路由 |
| QA | Playwright 走查 + 截图 | `qa-newbie-guide.md` + `screenshots/` |
| Ops | handoff + `npm run build` | `handoff.md` |

## 4. Definition of Done

- [x] `docs/sprints/S36/README.md`（本文件）已建
- [x] `prd-newbie-guide.md` 写完，至少 5 条 AC
- [x] `design-newbie-guide.md` 写完，含色板 / 间距 / 暗色态
- [x] 首页能看到新卡片，点击跳 `/newbie-guide`
- [x] NewbieGuideView 3 个区块可见且锚点导航工作
- [x] §3 API 区块点击 CTA 跳 `/api-guide`
- [x] 暗色态自动适配（用 token，不写死颜色）
- [x] 移动端（≤640px）布局正常
- [x] `npm run build` 通过
- [x] Playwright 截图归档到 `screenshots/`
- [x] handoff.md 总结改动文件 + 自测结论

## 5. 关联文件

- 上游参考：`frontend/src/components/home/HomeStats.vue`（已有但弱化的卡片，本次独立）
- 上游参考：`frontend/src/components/home/HomeHero.vue`（agent tab 4 步教程 — 迁到引导页 §2）
- 下游目标页：`frontend/src/views/ApiGuideView.vue`（§3 CTA 跳过去，不重写）
- 路由：`frontend/src/router/index.ts`
- 全局 token：`frontend/src/style/global.scss`
