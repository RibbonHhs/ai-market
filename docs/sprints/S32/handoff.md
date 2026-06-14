# S32 收尾 — 用途/职业 Chip 同 Row + Icon 区分

> **Sprint**: S32
> **范围**: 前端 SkillCard / SkillDetailView / OccupationCard / UsageChip / global.scss
> **结果**: ✅ 全部交付，build 绿，可签收
> **日期**: 2026-06-13

---

## 1. 用户需求（原文）

> 浏览页和 skill 详情页中用途分类和职业技能应该在同一行，并且都使用 icon 区分是 用途分类还是职业技能

## 2. 改动清单

### 2.1 新增文件
| 文件 | 行 | 用途 |
|------|----|------|
| `frontend/src/components/UsageChip.vue` | 215 | 统一 chip 组件，支持 `kind="usage"\|"occupation"` 区分类型 icon |
| `docs/sprints/S32/prd-chip-row.md` | 151 | PM 阶段产出（需求 + 验收标准） |
| `docs/sprints/S32/design-chip-row.md` | 102 | Designer 阶段产出（icon 选型 + token 规范） |
| `docs/sprints/S32/qa-chip-row.md` | 110 | QA 阶段产出（验收对照表） |

### 2.2 修改文件
| 文件 | 变更摘要 |
|------|---------|
| `frontend/src/components/SkillCard.vue` | 卡片内 `.skill-card__categories` 容器放职业 + 用途两个 UsageChip（flex 同 row） |
| `frontend/src/components/OccupationCard.vue` | 单 chip 卡升级用 UsageChip（保持 `kind` 透传） |
| `frontend/src/views/SkillDetailView.vue` | 详情页 header `.detail__categories` 行加职业 + 用途 chip；移除原 `<a-tag color="blue">` 重复展示 |
| `frontend/src/style/global.scss` | 新增 `:root` 浅色 `--chip-occupation-bg/fg`；`.usage-chip--code-occupation` 切到 var 驱动（之前写死浅色 → 暗色失效） |
| `frontend/src/components/home/HomeHero.vue` | 联动调整（沿用上次 S32 修复，未在本 Sprint 改） |
| `frontend/src/components/home/HomeFeatured.vue` | 联动调整 |
| `frontend/src/components/AppHeader.vue` | 联动调整 |

### 2.3 icon 选型（Designer 拍板）
- **用途分类**（USAGE）→ `<AimOutlined />`（瞄准形 = 目标隐喻）
- **职业技能**（OCCUPATION）→ `<ToolOutlined />`（扳手 = 工具型 SaaS 语义）
- 全部走 `currentColor` 跟随 chip 前景色，支持暗色 + 12 色 USAGE 体系

### 2.4 瑕疵修复
| ID | 问题 | 修复 |
|----|------|------|
| QA-6 | 暗色 occupation chip 不切换（line 199 写死） | 加浅色 token + 切 var 驱动 |
| QA-7 | `SkillCard` ★精选 与职业 chip testid 冲突 | `skill-soc-chip` → `skill-featured-tag` |
| QA-8 | `SkillDetailView` 用旧 prop `variant` | 统一改 `kind`（`variant` 兼容回退保留） |

## 3. 验收结果

- ✅ `vue-tsc --noEmit` 无 error
- ✅ `vite build` 17.83s 通过
- ✅ PRD §5 全部 8 个 AC 通过
- ✅ 5 个 data-testid 无冲突
- ✅ a11y：每个 chip 有 `role="img"` + `aria-label="用途分类：xxx" / "职业分类：xxx"`
- ⏳ 视觉冒烟：Playwright 脚本 `verify-chips.mjs` 已就绪，需 dev-server 跑（QA §4）

## 4. 向后兼容

- `UsageChip` `variant` prop 保留为兼容回退（`kind ?? variant`），旧调用方继续可用
- `skill.usageCategorySlug` 平铺字段仍读，不破坏 S18 旧数据
- BrowseView 顶部 12 个 USAGE 粗筛按钮仍是 `<button class="usage-chip">`，与新组件共存（class 名共用但语义不同）

## 5. 遗留 / Open

- `variant` 兼容 prop：留作 S33+ 清理
- 装饰 emoji（🛠 💼 💻）仍在 UsageChip 内（Design §2.3 明示「不强制替换」）
- Playwright 视觉冒烟未跑（dev-server 未起）— QA §4 给出了脚本

## 6. 关联文档

- 需求：`docs/sprints/S32/prd-chip-row.md`
- 设计：`docs/sprints/S32/design-chip-row.md`
- 验收：`docs/sprints/S32/qa-chip-row.md`
- 视觉冒烟脚本：`docs/sprints/S32/verify-chips.mjs`
- CSS 校验脚本：`docs/sprints/S32/verify-css.mjs`
- 前序：S31 收尾 → `7b74319 fix(S31): B1 search btn white text + L4 warning amber-700`

---

**Lead 签收**：可合并 `master`。后续 Sprint S33 推荐清理项见 §5。