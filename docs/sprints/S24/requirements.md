# S24 需求文档：用 USAGE 维度标注 skill 用途

> **Sprint 编号**：S24
> **主题**：完善现有 7 个 skill 的"用途分类"维度
> **范围**：audit → heuristic → API → 前端展示，端到端
> **接续**：S23（限流 + skills-manager 鉴权中转）
> **作者**：pm-alice（兼 Lead 视角）
> **日期**：2026-06-12

---

## 1. 背景与动机

### 1.1 已落地的 S18 USAGE 维度

- `Category` 表支持 `type='USAGE'`，已 seed 12 一级 + 70 sub-group = 82 条目
- `Skill` 实体已加 `usageCategoryId`（FK → category.id）
- `SkillSeedService.guessUsageCode`（现抽到 `CategoryUtil`）已被 `importSkill` 调用
- `GET /api/categories?type=USAGE` 可拉分类树

### 1.2 痛点

- **命中率低**：S24 启动审计发现 7 个现有 skill 中 3 个（42.9%）归类错误
- **未端到端展示**：前端 `SkillCard` / `SkillDetail` 未渲染 USAGE 字段
- **无筛选入口**：BrowseSkills 顶部只有 SOC 横向 chip，缺 USAGE 横向 chip
- **回填无路径**：手动改 7 个 skill 的 USAGE 只能 UPDATE SQL，无 admin 端点

### 1.3 一句话目标

> 让用户**一眼看出每个 skill 是干什么的**（不是"什么职业用"而是"用在什么场景"），并能按"用途"筛选。

---

## 2. 用户故事

### US-1：作为浏览者，我能在 SkillCard 上看到"用途"标签
**来源**：S21 HomeHero 双 tab 的浏览用户。

> 我在 BrowseSkills 浏览时，期望每张卡片有 2 个分类 chip：
> - **职业**（SOC）：告诉"谁会用到"
> - **用途**（USAGE）：告诉"用在什么场景"
>
> **验收**：
> - SkillCard 渲染 2 个 chip
> - USAGE chip 按一级类目配色（12 色循环）
> - chip 高度 22-24px，文字 12-13px
> - hover 显示完整 USAGE 名称（防止截断）

### US-2：作为浏览者，我能按"用途"筛选 skill
**来源**：S21 BrowseSkills 筛选扩展。

> 我在 BrowseSkills 顶部，期望有 USAGE 横向 chip 筛选条（与 SOC 并列）。
>
> **验收**：
> - 顶部 chip 列出 12 个一级 USAGE（"工具"/"开发"/"测试与安全"/...）
> - 选中 chip 时，下方列表只显示该 USAGE 下的 skill
> - "全部" chip 可清除筛选
> - 移动端可横向滚动 chip 流

### US-3：作为浏览者，我在 SkillDetail 看到"用途"区块
**来源**：S21 SkillDetailView。

> 我点开 skill 详情时，期望看到一个"用途"区块，告诉我：
> - 父类目（如"开发"）+ 子类目（如"前端开发"）
> - 父类目 description（中文 1 句）
>
> **验收**：
> - 详情页 Skill 头部下方 1 个区块，标题"用途"
> - 显示父类目（粗体）+ 箭头 → 子类目（细体）
> - 父类目 description 1 行小字
> - 区块位置：在"标签"和"描述"之间

### US-4：作为 admin，我能回填 USAGE 归类
**来源**：S20 admin 工具扩展。

> 当启发式增强后，存量 skill 的 USAGE 归类可能不准确。我期望一个 admin 端点能：
> - 一次性扫描所有 `usageCategoryId IS NULL` 或已分类但可能有误的 skill
> - 重新跑启发式，但**仅**对 `usageCategoryId IS NULL` 的做填充（幂等安全）
> - 返回 `{scanned, updated, skipped}` 计数
>
> **验收**：
> - `POST /api/admin/skills/backfill-usage` 仅 admin 可调
> - 默认行为：只补 null（不覆盖已有）
> - 可选 `?force=true` 参数：覆盖已有（危险，需 audit log）
> - 返回 JSON 计数

### US-5：作为开发者，我能用更精确的关键词推断 USAGE
**来源**：启发式增强。

> 7 个现有 skill 中 3 个归错。新启发式应处理：
> - `demo` → 测试（demo-uploaded-skill, zip-demo-skill）
> - `manager` / `management` → 生产力工具（skills-manager）
>
> **验收**：
> - 单元测试覆盖 2 个新规则
> - 集成测试：7 个 skill 重启 seed 后，USAGE 全部正确

---

## 3. 验收标准（Definition of Done）

### 3.1 数据层
- [ ] `audit-current-usage.md` 7 行表格存在并附调整理由
- [ ] `CategoryUtil.guessUsageCode` 增加 2 个新规则
- [ ] 重启后端后，7 个 skill 的 USAGE 全部命中正确（按审计表"建议 USAGE"列）

### 3.2 后端 API
- [ ] `SkillVO.usageCategory` 嵌套对象已存在（`{id, code, name, slug, parentId}`）
- [ ] `GET /api/skills` 返回 `usageCategory` 字段（非空）
- [ ] `GET /api/skills/slug/{slug}` 返回 `usageCategory` 字段
- [ ] `POST /api/admin/skills/backfill-usage` 端点存在（admin only）
- [ ] 调 2 次 backfill，第二次 `updated=0`（幂等）

### 3.3 前端展示
- [ ] SkillCard 显示 USAGE chip（彩色）
- [ ] SkillDetail 显示"用途"区块（父+子+描述）
- [ ] BrowseSkills 顶部加 USAGE 横向 chip 筛选条
- [ ] USAGE chip 12 色与一级类目一一对应

### 3.4 验证
- [ ] `mvn -q clean compile` BUILD SUCCESS
- [ ] `npm run build` 0 错
- [ ] 5 个 curl 验证全过
- [ ] 3 张 Playwright 截图（SkillCard 列表 / SkillDetail / Browse 筛选）
- [ ] 7 个 skill 的 USAGE 与审计报告 100% 一致

---

## 4. 非目标（Out of Scope）

| 不做 | 理由 |
|------|------|
| USAGE 多对多 | DTO 复杂，v1 一对一够用 |
| USAGE 类目自定义 | 沿用 seed 的 82 条 |
| 国际化多语言 | 沿用现有中英双语 |
| 暗色模式 USAGE chip 配色 | 与全局主题一起做 |
| "Management" 新一级类目 | 启发式兜到 PRODUCTIVITY 即可 |

---

## 5. 决策汇总

| Q | 决策 | 理由 |
|---|------|------|
| Q1 分类维度 | **继续 USAGE** | S18 已落 |
| Q2 一 skill 多 USAGE | **不支持** | v1 一对一 |
| Q3 backfill 时机 | **启动 seed 自动 + 手动 admin 端点** | 双保险 |
| Q4 配色方案 | **12 个一级 USAGE 各自色系**（AntV 调色板） | 不撞 SOC 色 |
| Q5 筛选 UI | **BrowseSkills 顶部横向 chip** | 与 SOC 筛选对齐 |
| Q6 已有正确归类 | **backfill 不覆盖** | 幂等安全 |
| Q7 skills-manager 归类 | `PURPOSE-TOOL-PRODUCTIVITY` | 无 MANAGEMENT 类目，最接近 |
| Q8 web-design-engineer 归类 | `PURPOSE-DEV-FRONTEND` | 产出物是代码 HTML/CSS/JS |

---

## 6. 风险与缓解

| 风险 | 缓解 |
|------|------|
| 启发式 2 个新规则误伤 | 单元测试覆盖 7 个 skill + 边界 case |
| 12 色循环撞 SOC 色 | 用 AntV 调色板而非 Tailwind 默认；QC 阶段比对 |
| backfill 误覆盖 | 默认只补 null；force=true 才覆盖 |
| 移动端 chip 横向滚动 | iOS Safari 测试；touch-action: manipulation |

---

## 7. 时间盒

- PM / Audit / Design：半天
- Dev 后端 + 前端：1 天
- QA + Handoff：半天
- 总计：2 个工作日
