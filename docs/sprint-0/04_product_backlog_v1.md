# Product Backlog v1（产品待办列表 v1）

> 作者：pm-alice @ Sprint 0 Kickoff (2026-06-06) · 增量作者：pm-alice @ Feature-1 Kickoff (2026-06-06)
>
> 版本：v1.1（v1.0 + Feature-1 Epic 增量）· 优先级方法：MoSCoW · 估点：Fibonacci (1, 2, 3, 5, 8, 13) · 评审节奏：每 Sprint Refinement
>
> 依赖：必须对齐 `docs/PRD.md`（产品定位、5 类 User Story 概要） / `docs/API.md`（接口能力） / `docs/ER.md`（数据边界）
>
> 增量引用：`docs/sprint-1/00_adr/ADR-001-git-bidirectional-skills.md` / `00_feature_brief.md`

## 1. Epic 划分

| Epic ID | 名称 | 价值主张 | 优先级 |
|---|---|---|---|
| E-1 | 浏览与发现 | 用户能"找到想要的 Skill" | Must |
| E-2 | 详情与决策 | 用户能"判断是否安装" | Must |
| E-3 | 社区互动 | 用户能"评分 / 收藏" | Must |
| E-4 | 账号体系 | 用户能"注册 / 登录 / 个人中心" | Must |
| E-5 | 后台管理 | 运营能"维护 Skill / 分类 / 用户" | Must |
| E-6 | 导入与同步 | 运营能"从本地 / 官方市场扫描入库" | Should |
| **E-10** | **Git 双向 Skill 来源**（**Feature-1**） | **运营能"用 Git URL 或 ZIP 上传" + 用户能"自动跟踪上游"** | **Must (Sprint 1)** |
| E-7 | 国际化与可访问性 | 全平台可访问 + 中英双语 | Could |
| E-8 | 数据洞察 | 平台能"看 Dashboard" | Should |
| E-9 | 高级管理 | 角色 / 权限细化 + 审计 | Won't（v1.1+） |

## 2. v1 Backlog 全量

> 按 MoSCoW 排序：Must → Should → Could → Won't
> 每行：ID / 标题 / As a / I want / So that / 验收标准（Given-When-Then）/ SP / 优先级 / 关联

### 2.1 Must（必须 v1 上线）

| ID | 标题 | As a | I want | So that | 验收标准（节选） | SP | 优先级 | 关联 |
|---|---|---|---|---|---|---|---|---|
| US-001 | 首页 Hero 搜索 | AI 开发者 | 在首页看到醒目搜索框 | 能快速定位关键词 | ①首页加载 ≤ 1s 出现 hero ②输入框聚焦有视觉反馈 ③回车跳 `/browse?keyword=` | 2 | Must | E-1 |
| US-002 | 精选 Skills 板块 | AI 开发者 | 在首页看到 6 个精选 Skill | 能浏览推荐内容 | ①后台 `featured=true` 的 skill 拉取 ②卡片 6 个 / 一行 3 个 ③空数据时显示空态 | 2 | Must | E-1 |
| US-003 | 最新 Skills 板块 | AI 开发者 | 看到最近 30 天新发布 | 跟进新内容 | ①按 `create_time desc` ②≤ 6 个 ③点击进详情 | 2 | Must | E-1 |
| US-004 | 热门 Skills 板块 | AI 开发者 | 看到安装量 Top 6 | 跟随社区选择 | ①按 `installs desc` ②显示安装数 ③前 6 个 | 2 | Must | E-1 |
| US-005 | 分类入口 | AI 开发者 | 在首页看到分类网格 | 按分类筛选 | ①展示 10 个分类 ②显示 icon + skill_count ③点击进 `/browse?categoryId=` | 2 | Must | E-1 |
| US-006 | 浏览页 - 列表 | AI 开发者 | 在 `/browse` 看到分页列表 | 翻页浏览 | ①每页 12 个 ②分页器可跳页 ③空结果显示空态 | 3 | Must | E-1 |
| US-007 | 浏览页 - 搜索 | AI 开发者 | 在 `/browse` 输入关键词 | 找到相关 skill | ①关键词高亮（标题/描述） ②≥ 300ms 防抖 ③无结果有建议词 | 3 | Must | E-1 |
| US-008 | 浏览页 - 分类筛选 | AI 开发者 | 在 `/browse` 按分类过滤 | 只看一类 | ①单选分类 ②`?categoryId=` 路由可分享 ③URL 状态可后退 | 2 | Must | E-1 |
| US-009 | 浏览页 - 排序 | AI 开发者 | 在 `/browse` 切换排序 | 找到目标 | ①支持最新 / 安装 / 评分 / 浏览 ②默认最新 ③切换不刷新 | 2 | Must | E-1 |
| US-010 | 详情页 - 基础信息 | AI 开发者 | 看到 name / version / license / author | 了解 skill 概况 | ①字段全 ②缺失时显示"—" ③markdown 链接可点 | 3 | Must | E-2 |
| US-011 | 详情页 - SKILL.md 渲染 | AI 开发者 | 看 SKILL.md 正文章节 | 学习使用 | ①markdown-it 解析 ②代码高亮 ③支持锚点跳转 | 5 | Must | E-2 |
| US-012 | 详情页 - 安装命令复制 | AI 开发者 | 一键复制 install_command | 立即安装 | ①显示命令 ②点复制按钮 ③成功 toast 3s 消失 | 2 | Must | E-2 |
| US-013 | 详情页 - 评分列表 | AI 开发者 | 看现有评分 | 判断质量 | ①显示 5⭐ + 评论 + 时间 ②分页 10 条 ③空时引导"写评价" | 2 | Must | E-2 |
| US-014 | 用户注册 | 访客 | 在 `/register` 创建账号 | 登录享受互动 | ①用户名 ≥ 4 字符唯一 ②密码 ≥ 6 字符 ③成功后自动登录 | 3 | Must | E-4 |
| US-015 | 用户登录 | 访客 | 在 `/login` 输入凭证 | 拿到 token | ①失败提示明确 ②成功跳来源页或 `/` ③token 存 LocalPrivateCache | 2 | Must | E-4 |
| US-016 | 个人中心 - 我的收藏 | 注册用户 | 在 `/me/favorites` 看我收藏的 | 找回它们 | ①分页列表 ②取消收藏即时同步 ③空态引导"去发现" | 2 | Must | E-4 |
| US-017 | 个人中心 - 我的评价 | 注册用户 | 在 `/me/reviews` 看我评论的 | 跟踪历史 | ①分页 ②可跳详情 ③显示评分 + 时间 | 2 | Must | E-4 |
| US-018 | 评分提交 | 注册用户 | 在详情页提交 1-5 星 + 评论 | 表达喜好 | ①未登录跳 `/login?redirect=` ②重复提交覆盖 ③成功后详情刷新 | 5 | Must | E-3 |
| US-019 | 收藏 toggle | 注册用户 | 在详情页点收藏 | 跟踪 skill | ①未登录跳登录 ②图标 + 文本变化 ③`/me/favorites` 同步 | 3 | Must | E-3 |
| US-020 | 后台登录 | admin | 在 `/admin` 用 admin/admin123 登录 | 维护平台 | ①admin 角色才能进 ②失败 5 次锁 1min ③session 用 JWT | 2 | Must | E-5 |
| US-021 | 后台 - 仪表盘 | admin | 看到总览数字 | 了解平台 | ①总 skill / 总用户 / 总下载 ②近 7 天趋势 ③Git 同步状态 | 3 | Must | E-5 / E-8 |
| US-022 | 后台 - 分类管理 | admin | 增删改分类 | 维护结构 | ①CRUD 全 ②slug 唯一 ③删除前确认 | 3 | Must | E-5 |
| US-023 | 后台 - 新建 Skill | admin | 填表 + 选 zip | 入库发布 | ①表单 12 字段 ②zip 上传走 SKILL.md 解析 ③发布可立刻 / 草稿 | 5 | Must | E-5 / E-6 |
| US-024 | 后台 - 编辑 Skill | admin | 修改已存在 | 修正内容 | ①同新建表单 ②表单回填 ③修改后 sync Git | 5 | Must | E-5 / E-6 |
| US-025 | 后台 - 上下架 | admin | 切换 skill `status` | 控制可见 | ①一键切 published/draft ②列表过滤 ③详情页 404 draft | 2 | Must | E-5 |
| US-026 | 后台 - 用户管理 | admin | 看 / 改用户角色 / 状态 | 维护账号 | ①列表分页 ②禁启用 ③角色调整 | 3 | Must | E-5 |

### 2.2 Should（v1 后期 / Sprint 1）

> Feature-1 在此分类下设 E-10 Epic

| ID | 标题 | As a | I want | So that | 验收标准（节选） | SP | 优先级 | 关联 |
|---|---|---|---|---|---|---|---|---|
| US-027 | 后台 - 从本地扫描 | admin | 点按钮触发扫描 | 导入新 skill | ①扫描 `local-skills-path` + `local-plugins-path` ②返回 imported/skipped ③不重复 | 3 | Should | E-6 |
| US-028 | Dashboard Git 状态 | admin | 看到 program git 同步统计 | 知道同步健康 | ①success/failure count ②lastSyncAt ③lastError | 2 | Should | E-5 / E-10 |
| US-029 | 后台 - 标签管理 | admin | CRUD tag | 维护标签 | ①列表 ②合并重复 ③删除前确认 | 3 | Should | E-5 |
| **US-030** | **Git URL 注册 Skill** | **admin** | **粘贴 git URL + ref + PAT 表单** | **批量从 GitHub 等仓库导入** | **①POST `/api/admin/skills/git-source` 同步 clone + 解析 + 入库 ②返回解析结果列表 ③PAT 错返回 50300** | **8** | **Must (S1)** | **E-10** |
| **US-031** | **Git 源 Skill 详情徽章** | **访客** | **在详情页看到 `Source: Git (https://...)` 徽章** | **区分本地 zip 上传** | **①列表卡片有徽章 ②详情页有大徽章 ③点击 URL 跳源仓库** | **2** | **Must (S1)** | **E-10** |
| **US-032** | **Git 源 Skill 手动拉取** | **admin** | **在详情页点"立即拉取"按钮** | **手动同步上游** | **①按钮仅 admin 可见 ②POST `/api/admin/skills/{id}/git-pull` ③返回新 SHA + 变更字段** | **3** | **Must (S1)** | **E-10** |
| **US-033** | **Git 源 Skill 周期拉取** | **admin** | **配置 cron 表达式** | **自动跟踪上游** | **①`@Scheduled` 每 6h 跑 ②写 `last_pulled_at` ③失败不阻塞** | **5** | **Should (S1)** | **E-10** |
| **US-034** | **Git 源 Skill form 字段保护** | **admin** | **编辑 git 源 skill 的 description 等 form 字段** | **不被拉取冲掉** | **①拉取 diff form vs git ②form 优先 ③UI 标记"本地编辑"** | **3** | **Must (S1)** | **E-10** |
| **US-035** | **Git 凭据加密** | **admin** | **在 UI 输入 PAT** | **不在 DB 明文** | **①jasypt 加密 ②DB grep `ghp_` 零命中 ③UI 输入有 password toggle** | **3** | **Must (S1)** | **E-10** |
| **US-036** | **Git 源 Skill 拉取失败透明展示** | **访客** | **看到 skill 状态 `error` 但仍可访问** | **知道是临时问题** | **①status=error 徽章 ②详情页 banner ③admin 可重试** | **2** | **Should (S1)** | **E-10** |
| **US-037** | **Skill 上传 3rd tab（Git URL）** | **admin** | **在 SkillUploader 看到 3 个 tab** | **选择 Git URL 上传方式** | **①md / zip / git-url 三 tab ②git-url 表单有 url/ref/cred ③提交后跳管理页** | **3** | **Must (S1)** | **E-10** |
| **US-038** | **Git 源列表筛选** | **admin** | **在后台 skill 列表加 `source` 筛选** | **只看 git 源** | **①筛选 zip / git ②计数显示 ③组合 status** | **2** | **Should (S1)** | **E-10** |
| **US-039** | **磁盘配额监控** | **ops** | **看到 `data/` 目录大小 + 告警** | **防止大仓库撑爆** | **①Actuator metric ②阈值告警 ③按 skill 排序 top10** | **2** | **Should (S1)** | **E-10** |

### 2.3 Could（v1.1+）

| ID | 标题 | SP | 优先级 | 关联 |
|---|---|---|---|---|
| US-040 | 平台选择提示（win/mac/linux） | 2 | Could | E-7 |
| US-041 | 详情页 TOC 自动生成 | 3 | Could | E-2 |
| US-042 | Webhook 触发拉取 | 5 | Could (S2) | E-10 |
| US-043 | SSH key 支持私有仓库 | 5 | Could (v1.1) | E-10 |
| US-044 | Git LFS 大文件支持 | 8 | Could (v1.1) | E-10 |

### 2.4 Won't（v1 不做）

| ID | 标题 | 备注 |
|---|---|---|
| US-W01 | 高级 RBAC（资源级权限） | v1.2 |
| US-W02 | 审计日志 | v1.2 |
| US-W03 | 多租户 | 永久 Won't（v1 单租户） |
| US-W04 | API 限流 | v1.1 |
| US-W05 | CDN 静态资源 | 触发后切 |

## 3. Feature-1 Epic 详情（E-10）

> 详细 12 个 Story 见 `docs/sprint-1/01_sprint1_backlog.md`。
> 总 SP：**38**（Sprint 1 容量见 Sprint Backlog §3 评估）

| 故事群 | SP | 备注 |
|---|---|---|
| 数据模型 + Service 分流（US-030/-035） | 11 | dev-kevin 主 |
| UI 3rd tab + 状态徽章（US-031/-037） | 5 | designer-vicky + dev-kevin |
| 手动 + 周期拉取（US-032/-033） | 8 | dev-kevin |
| form 字段保护 + 失败透明（US-034/-036） | 5 | dev-kevin + qa-tina |
| 列表筛选 + 磁盘监控（US-038/-039） | 4 | dev-kevin + ops-max |
| 测试 + Runbook | 5 | qa-tina + ops-max |
| **合计** | **38** | |

## 4. 修订记录

| 版本 | 日期 | 作者 | 摘要 |
|---|---|---|---|
| v1.0 | 2026-06-06 | pm-alice | 初版 Backlog：9 Epic + 26 US + MoSCoW 排序 |
| v1.1 | 2026-06-06 | pm-alice | Feature-1 增量：新增 E-10 Epic（Git 双向 Skill 来源）+ US-030~US-039 共 10 个 Story，总 SP 38 |
