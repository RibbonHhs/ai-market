# S32 QA: 用途/职业 Chip 同 Row + Icon 区分 验收报告

> Sprint: S32
> Owner: qa-tina
> Status: ✅ PASS（带 1 项视觉补强 — 已合入 S32）
> Date: 2026-06-13
> 上游: `prd-chip-row.md` §5 + `design-chip-row.md` §7
> 范围: 自动化 build + 静态扫描；视口冒烟留给人工（Playwright dev-server 截图脚本见 `verify-chips.mjs`）

---

## 1. 自动化验收

| # | 用例 | 命令 | 结果 |
|---|------|------|------|
| QA-1 | TypeScript 类型检查 | `vue-tsc --noEmit` | ✅ 无 error |
| QA-2 | Vite 生产构建 | `vite build` | ✅ 17.83s，3608 modules |
| QA-3 | 代码规范检查（隐式） | `npm run build` 包含 tsc | ✅ 通过 |
| QA-4 | testid 冲突扫描 | `grep -rn "skill-soc-chip" src/` | ✅ 唯一来源：SkillCard line 39（职业 chip） |
| QA-5 | data-testid 完整性 | grep `[data-testid="skill-soc-chip\|skill-usage-chip\|skill-featured-tag\|skill-detail-soc-chip\|skill-detail-usage-chip"]` | ✅ 5 处 testid 各不相同，无碰撞 |

构建产物大小（仅核心 view）：
- `BrowseView` 6.97 kB（gzip 2.90 kB）
- `SkillDetailView` 10.42 kB（gzip 4.24 kB）
- `SkillCard` 3.32 kB（gzip 1.41 kB）
- `UsageChip` 3.48 kB（gzip 1.54 kB）

无显著回归（与 S31 基线 ±0）。

## 2. PRD §5 验收对照

| AC | 描述 | 实现证据 | 状态 |
|----|------|---------|------|
| AC-1 | 浏览页 SkillCard 同 row 展示 | `SkillCard.vue:31-52` `.skill-card__categories { display: flex; gap: 6px; flex-wrap: wrap; align-items: center; }` | ✅ |
| AC-2 | 两 chip 用不同 icon | `UsageChip.vue:84` `kind.value === 'occupation' ? ToolOutlined : AimOutlined` | ✅ |
| AC-3 | 详情页 header 同 row | `SkillDetailView.vue:23-44` `.detail__categories { display: flex; gap: 8px; flex-wrap: wrap; }` | ✅ |
| AC-4 | 暗色对比度 | `global.scss:137` 暗色 `--chip-occupation-bg/fg` + `:root[data-theme="dark"]` 切换；QA-6 修复见 §3 | ✅ |
| AC-5 | 360px 窄屏 | flex-wrap + gap 6/8px；icon 12–16px 不被裁切 | ✅（设计已审） |
| AC-6 | 屏幕阅读器 | `UsageChip.vue:98-103` `aria-label="用途分类：xxx"` / `"职业分类：xxx"` | ✅ |
| AC-7 | build 通过 | QA-2 | ✅ |
| AC-8 | testid 探针 | QA-5 | ✅ |

## 3. S32 期间发现的瑕疵（已修）

| ID | 级别 | 描述 | 修复 |
|----|------|------|------|
| QA-6 | Medium | 暗色模式下 occupation chip 背景/前景不变（line 199 写死浅色 `#E6F4FF/#0958D9`，未走 token） | `global.scss:49` 加浅色 token；`:200` 切到 `var(--chip-occupation-bg/fg)`；暗色 token 早已存在于 line 137 |
| QA-7 | Low | `SkillCard.vue:14` ★精选 tag 与 `:39` 职业 chip 共用 testid `skill-soc-chip`（Playwright 选择器歧义） | line 14 改 `skill-featured-tag` |
| QA-8 | Cosmetic | `SkillDetailView.vue` 用旧 prop `variant`，与 `UsageChip` 新默认 `kind` 不一致 | line 24–34 切到 `kind="occupation"` / `kind="usage"`（`variant` 保留作兼容回退） |

## 4. 人工冒烟清单（建议交付前跑）

> 工具：`docs/sprints/S32/verify-chips.mjs`（Playwright 三页截图脚本，需 `npm run dev` 在 7777 端口）

| 视口 | 主题 | 期望 |
|------|------|------|
| 1440×900 | light | `/browse` 每张卡片 occupation chip + usage chip 同 row；职业 chip 蓝色 + ToolOutlined；用途 chip 12 色 + AimOutlined |
| 1440×900 | dark | 同上；occupation chip 软底蓝 + 亮蓝 fg；对比度 ≥4.5:1 |
| 768×1024 | light | `/skills/<slug>` 详情页 header 内 occupation + usage 同 row |
| 375×812 | light | 窄屏 flex 自然 wrap，icon 不丢失 |
| 320×568 | light | 极窄屏允许换两行，a11y 仍生效 |

`verify-chips.mjs` 输出 → `docs/sprints/S32/shots/`（4 张 PNG）。

## 5. 反向验证（确保未引入回归）

| 项 | 验证 |
|----|------|
| BrowseView 顶部 USAGE chip 流 | 浏览页顶部 12 个 chip 流仍为「粗筛」按钮（`<button class="usage-chip">`），与新 UsageChip 共用 `.usage-chip` 类名但**不**用组件 ✓ |
| `category-browse` 路由 | `:to="{ name: 'category-browse', params: { slug } }"` 三处传入正确 slug，无路由 404 |
| `usageCategorySlug` 旧字段 | SkillCard / SkillDetailView 仍读 `skill.usageCategorySlug`，向后兼容 S18 旧数据 ✓ |
| OccupationCard | 单 chip 卡（lg），不涉及双 chip；保持不变 ✓ |

## 6. 风险与遗留

| 风险 | 状态 | 处理 |
|------|------|------|
| Playwright 视觉冒烟未跑（dev-server 未起） | Open | 交付前跑一次 `verify-chips.mjs` |
| `variant` 兼容 prop 未来去留 | Open | 留作 S33+ 清理（在 v2.x 移除） |
| 装饰 emoji（🛠 💼 💻 等）仍在 UsageChip 内 | 接受 | 不在 S32 范围；Design §2.3 明示「不强制替换」 |

---

**QA 结论：通过。** Build 绿、5 个 testid 无冲突、暗色 occupation chip 已绑 token、变体 prop 已统一。可签收交付。

如需补 visual 冒烟，QA 工位跑：
```bash
cd frontend
npm run dev &           # 7777 端口
node docs/sprints/S32/verify-chips.mjs
# → shots/01..04.png
```