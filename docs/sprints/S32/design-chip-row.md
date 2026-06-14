# S32 Design Spec: 用途/职业 Chip 类型 Icon 选型

> Sprint: S32
> Owner: designer-vicky
> Status: Approved (by PM)
> Date: 2026-06-13
> 上游: `prd-chip-row.md`
> 设计哲学: 工具型 SaaS / 矢量图标优先 / WCAG AA / 同 row 节奏

---

## 1. Icon 选型（Ant Design Vue）

| 语义 | 图标 | 组件名 | 备选 | 决定 |
|------|------|--------|------|------|
| 用途分类（USAGE） | 准星 / 瞄准 | `AimOutlined` | `AppstoreOutlined` / `BulbOutlined` | **`AimOutlined`**（核心"目标"隐喻更强） |
| 职业技能（OCCUPATION） | 工具 | `ToolOutlined` | `BriefcaseOutlined` / `HammerOutlined` | **`ToolOutlined`**（与"工具型 SaaS"产品语义最贴合） |

**理由：**
- 两者形状差异显著：Aim 是"十字 + 圆环"，Tool 是"扳手 / 螺丝刀"几何 — 即使单色也能快速区分
- 都是 outline 风格（与项目其余 AntV 图标家族统一：`<DownloadOutlined/>` `<CopyOutlined/>` `<LinkOutlined/>` `<StarOutlined/>` 等）
- 避免使用 emoji 字符（系统字体不一致 + a11y 弱）
- 一律用 `Outlined` 变体 — 与"卡片/列表信息密度高"场景的视觉权重匹配；`Filled` 变体过重，喧宾夺主

## 2. 视觉 Token

### 2.1 尺寸
| size | icon px | font-size | padding (H × V) | min-height | gap | border-radius |
|------|---------|-----------|-----------------|------------|-----|---------------|
| `sm`（卡片） | **12** | 11 | 8 × 1 | 20 | 3 | 10 |
| `md`（详情） | **14** | 13 | 12 × 2 | 24 | 4 | 14 |
| `lg`（独立卡） | **16** | 14 | 16 × 4 | 30 | 6 | 16 |

> icon 与文字之间的 gap 由 chip 容器 `.usage-chip--pill` 的 `gap: N` 统一控制（`sm→3, md→4, lg→6`）

### 2.2 颜色
- icon 颜色 = `currentColor`（继承 chip fg），**禁止**写死 `#xxx`
- 原因：12 个 usage + 1 个 occupation 共 13 套配色；用 `currentColor` 让 icon 自动跟随主题，避免每次换色都要改 2 处
- 焦点态：`.usage-chip--pill.is-clickable:focus-visible { outline: 2px solid var(--primary); outline-offset: 2px; }`（已存在）

### 2.3 类别 emoji（仅 USAGE 内部装饰）
- USAGE chip 仍可有 `emoji` 装饰（来自 `usage-colors.ts` 的 `🛠 💼 💻 🧪 🤖` 等）— 这是"一级 USAGE 类目"的视觉锚点，**非类型区分**
- OCCUPATION chip **不显示** emoji（`showCategoryEmoji` 判定 `kind === 'usage'`，已正确）
- ⚠️ 装饰性 emoji 在 AntV icon 引入后**可保留也可换 icon**，本 Sprint 不强制替换（防 scope creep）

## 3. 布局规范

### 3.1 同行布局
| 容器 | 位置 | CSS | 顺序 |
|------|------|-----|------|
| `.skill-card__categories` | SkillCard 内 | `display: flex; gap: 6px; flex-wrap: wrap; align-items: center;` | 先 occupation，后 usage（与"职业"更基础放前） |
| `.detail__chips`（**新增**） | SkillDetailView header | `display: flex; gap: 8px; flex-wrap: wrap; align-items: center; margin: 8px 0 0;` | 同上：occupation → usage |

### 3.2 响应式断点
| 断点 | 行为 |
|------|------|
| ≥ 768px | 强制同 row（容器足够） |
| 360–767px | 同 row 优先；容器不足自然 wrap |
| < 360px | 允许 wrap 到两行；icon 仍显示；不丢 a11y |

### 3.3 暗色模式
- 12 个 USAGE 暗色配色已存在（`USAGE_DARK`），fg 在 `#161618` 背景下对比度 AAA ≥ 7:1
- `.usage-chip--code-occupation` 在 light 模式：`#E6F4FF` bg + `#0958D9` fg → 6.0:1 ✅
- 暗色模式需补：在 `.dark .usage-chip--code-occupation` 下用 `rgba(96,165,250,0.16)` bg + `#93c5fd` fg（与其他 USAGE 暗色 token 风格一致）— **由 Dev 实施**

## 4. 可访问性

| 项 | 规范 |
|----|------|
| 图标语义 | `<AimOutlined />` / `<ToolOutlined />` 加 `aria-hidden="true"`（装饰性，与文字组合传达） |
| 容器 a11y | 整体 chip 设 `role="img"` + `aria-label="用途分类：{parentName}"` / `aria-label="职业分类：{parentName}"` |
| 颜色 vs icon | 区分信息**同时**用 icon 形状 + 颜色，**不**仅靠颜色（满足 `color-not-only`） |
| 焦点 | 已经是 `router-link`（`clickable + to`）时，按 Tab 进入；`:focus-visible` 有 2px outline |
| 字号 | chip 文字 ≥ 12px（满足 mobile 16px 基线下不缩放） |

## 5. 组件契约（与 PM 对齐）

新增 `kind` prop：
- `kind: 'usage' | 'occupation'`（默认 `'usage'`）
- 同时保留 `variant` 做过渡期兼容（旧调用方仍可传 `variant="occupation"`，内部统一映射到 `kind`）
- 当 `kind === 'occupation'` 时：
  - 不显示 category emoji
  - 不应用 12 色 USAGE className（`codeClass` 用 `--code-occupation`）
  - icon 改为 `<ToolOutlined />`

## 6. 反模式（避免）

- ❌ 用 emoji 字符作类型区分（混字体、不可控对比度）
- ❌ 用 `Filled` 图标变体（视觉过重，喧宾夺主）
- ❌ 给 icon 写死颜色（破坏暗色主题与 12 色 USAGE 体系）
- ❌ 把两个 chip 拆到两行（在桌面端丢失"对照"语义）
- ❌ 在 chip 内只靠颜色区分 USAGE / OCCUPATION（违反 `color-not-only`）

## 7. 参考截图（人工对照清单）

需 QA 在以下视口与主题下冒烟：
- 1440×900 light / dark（桌面）
- 768×1024 light（平板）
- 375×812 light / dark（手机）
- 320×568 light（极窄屏）

---

**设计稿结束。** Dev 可启动。
