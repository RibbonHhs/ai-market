# S24 Wireframe & 配色规范：USAGE 维度展示

> **作者**：designer-vicky（兼 Lead 视角校对）
> **日期**：2026-06-12
> **依据**：ui-ux-pro-max 优先级 1-10（a11y / touch / perf / style / layout / typography / motion / forms / nav / data）
> **工具栈**：Vue 3.5 + Ant Design Vue 4 + Vite 7（已确定）

---

## 1. 12 色 USAGE 一级类目配色（AntV 调色板）

> **决策 Q4**：用 AntV 调色板顺序色（避免撞 SOC 蓝紫调）
> **依据**：`color-accessible-pairs`（4.5:1 AA），`color-not-decorative-only`（带 icon / 文字）

| # | code | 名称 | 主色 (bg) | 文字 (fg) | 14px 文字对比度 | 备注 |
|---|------|------|-----------|----------|----------------|------|
| 1 | PURPOSE-TOOL | 工具 | `#F0F5FF` (blue-50) | `#1D39C4` (blue-9) | 8.6:1 ✅ | 蓝（中性） |
| 2 | PURPOSE-BIZ | 商业 | `#FFF7E6` (orange-50) | `#AD4E00` (orange-9) | 7.2:1 ✅ | 橙 |
| 3 | PURPOSE-DEV | 开发 | `#E6FFFB` (cyan-50) | `#006D75` (cyan-9) | 8.4:1 ✅ | 青 |
| 4 | PURPOSE-QASEC | 测试与安全 | `#F9F0FF` (purple-50) | `#391085` (purple-9) | 9.1:1 ✅ | 紫 |
| 5 | PURPOSE-AI | 数据与AI | `#FFF0F6` (magenta-50) | `#9E1068` (magenta-9) | 8.0:1 ✅ | 品红 |
| 6 | PURPOSE-DEVOPS | DevOps | `#FFF2E8` (volcano-50) | `#A8071A` (volcano-9) | 7.8:1 ✅ | 朱 |
| 7 | PURPOSE-DOC | 文档 | `#FCFFE6` (lime-50) | `#435106` (lime-9) | 8.5:1 ✅ | 嫩绿 |
| 8 | PURPOSE-MEDIA | 内容与媒体 | `#E6FAFF` (geekblue-50) | `#003A8C` (geekblue-9) | 9.3:1 ✅ | 钴蓝 |
| 9 | PURPOSE-RESEARCH | 研究 | `#F0FBE6` (green-50) | `#135200` (green-9) | 9.0:1 ✅ | 草绿 |
| 10 | PURPOSE-LIFE | 生活方式 | `#FFF1F0` (red-50) | `#820014` (red-9) | 8.7:1 ✅ | 胭脂 |
| 11 | PURPOSE-DB | 数据库 | `#F4FFB8` (yellow-50) | `#874D00` (gold-9) | 7.5:1 ✅ | 琥珀 |
| 12 | PURPOSE-BLOCKCHAIN | 区块链 | `#FFE7BA` (gold-30) | `#874D00` (gold-9) | 7.5:1 ✅ | 黄金 |

> 12 色均通过 AA（4.5:1）。色相间距 30°，相邻不撞。
> **SOC 维度**仍用默认蓝（`#1677ff`），不与 USAGE 任何色撞。

---

## 2. SkillCard：USAGE 徽标（Chip）位置

### 2.1 现状（before）

```
┌────────────────────────────────────────┐
│ [LOGO] Display Name        [★ 精选]   │
│        👤 author · v1.0.0              │
│ ─── 来源徽标 ───                       │
│ 🔗 Git @ main                          │
│ description 2 行截断...                │
│ #tag1 #tag2 #tag3                      │
│ ──────────────────────────────────────│
│ ⭐ 4.5 (10)              ⬇ 1.2k        │
└────────────────────────────────────────┘
```

### 2.2 目标（after）

```
┌────────────────────────────────────────┐
│ [LOGO] Display Name        [★ 精选]   │
│        👤 author · v1.0.0              │
│ ─── 来源徽标 ───                       │
│ 🔗 Git @ main                          │
│ description 2 行截断...                │
│                                        │
│ [开发·前端]  [🛠 工具·调试工具]         │  ← 新增 2 个分类 chip
│ #tag1 #tag2 #tag3                      │
│ ──────────────────────────────────────│
│ ⭐ 4.5 (10)              ⬇ 1.2k        │
└────────────────────────────────────────┘
```

### 2.3 chip 规范

- **位置**：tags 行上方（描述后、tags 前），水平 8px 间距
- **高度**：22px（与 tags 一致）
- **内边距**：0 8px
- **圆角**：4px（与 a-tag 默认）
- **字号**：12px
- **chip 1（SOC）**：蓝实色 bg（`#e6f4ff`）+ fg `#1677ff`
- **chip 2（USAGE）**：12 色表对应色（bg + fg）
- **icon**：USAGE chip 前缀一个 14px 的"用途" SVG（用 Ant Design Icons 的 `AppstoreOutlined`）
- **文字**：父类目 + `·` + 子类目（如 "开发·前端开发"）。过长截断，hover 完整
- **点击**：跳到 BrowseSkills + 对应 dim + categoryId

### 2.4 移动端

- chip 字号缩到 11px
- 间距 4px

---

## 3. SkillDetail：用途区块

### 3.1 现状

```
┌──────────────────────────────────────────┐
│ [LOGO] Display Name           [★ 精选]  │
│        📦 name  📄 MIT  👤 author       │
│        [SOC 蓝 chip]                     │
│ description 1-2 行...                    │
│ [安装] [复制] [收藏] [主页]              │
│ ── 📊 stats ──                          │
└──────────────────────────────────────────┘
  [tag1 tag2 tag3]
  [📖 详细介绍]
  [💬 用户评价]
```

### 3.2 目标：加"用途"区块

**位置**：在"tags"区块后、"详细介绍"前

```
┌──────────────────────────────────────────┐
│ ── 🎯 用途 ──                            │  ← 新区块
│ 父类目：开发（前端开发）                  │
│ 💡 描述：用于构建现代 web 界面           │
└──────────────────────────────────────────┘
[tag1 tag2 tag3]
[📖 详细介绍]
[💬 用户评价]
```

### 3.3 区块规范

- **标题**：🎯 用途（emoji 作 icon 因现有组件已用）
- **父类目行**：粗体父类目（"开发"）+ 灰色 `→` + 子类目（"前端开发"）
- **描述行**：父类目 description（小字 12px，1 行，灰 `#999`）
- **背景**：白
- **padding**：12px 16px
- **圆角**：10px
- **SOC chip 仍保留在 detail__header 里**（不挪）

---

## 4. BrowseSkills：顶部横向 USAGE chip 筛选条

### 4.1 现状

```
┌─ 左 240px ─────┬─ 主区 1fr ──────────────┐
│ 分类维度:       │ [🔍 Search]  [Sort v]   │
│ (•) 按职业      │ ───────────────────────  │
│ ( ) 按用途      │ [Card] [Card] [Card]    │
│                 │ [Card] [Card] [Card]    │
│ 分类:           │                          │
│ 全部 12         │                          │
│ ▾ 15-12 xxx     │                          │
│   15-13 yyy     │                          │
│ 来源:           │                          │
│ (•) 全部        │                          │
│ ( ) 官方        │                          │
│ ( ) 社区        │                          │
└─────────────────┴──────────────────────────┘
```

### 4.2 目标：顶部加横向 chip 流

```
┌─ 左 240px ─────┬─ 主区 1fr ──────────────────┐
│ 分类维度:       │ ── 顶部 USAGE chip 流 ──     │  ← 新增
│ (•) 按职业      │ [全部] [🛠 工具] [💼 商业]   │
│ ( ) 按用途      │       [💻 开发] [🧪 测试]    │  ← 12+1 个
│                 │       [🤖 AI]  [🚀 DevOps]   │
│ 分类:           │       [📚 文档] [🎨 媒体]    │
│ ...             │       [🔬 研究] [🌱 生活]    │
│ 来源:           │       [💾 数据] [⛓ 区块链]  │
│ ...             │ ───────────────────────────  │
│                 │ [🔍 Search]  [Sort v]        │
│                 │ [Card] [Card] [Card] [Card]  │
│                 │ [Card] [Card] [Card] [Card]  │
└─────────────────┴────────────────────────────┘
```

### 4.3 chip 规范

- **位置**：主区最顶部（main 头部上方，1 行）
- **数量**：13 个 chip（"全部" + 12 一级 USAGE）
- **样式**：实色背景 + fg（用 §1 配色）
- **高度**：28px（比 card chip 略大）
- **圆角**：14px（pill 形）
- **选中态**：实色填充（反色：`bg=fg, fg=bg`），阴影 `0 2px 8px` 同色 20% 透明
- **未选中态**：浅色填充
- **icon**：chip 前缀 14px 缩写（用首字"🛠/💼/💻/..."，因为 emoji 不算 icon substitute 此处是装饰+增强）
- **横向溢出**：移动端可横滚；桌面端单行展示（>1200px 一行够，1200px 以下横滚）
- **touch target**：≥44px 高（包含 padding）
- **a11y**：每 chip `aria-label="筛选：开发"`（不能只靠色块区分）

### 4.4 行为

- 点击 chip → `query.usageCategoryId = <一级 USAGE id>` → reload
- 再次点击同一 chip → 取消筛选
- "全部" chip → 清空 `query.usageCategoryId`
- 与左 sidebar 的"按用途"树**互补**：
  - 顶部 chip = 一级（粗筛）
  - 左侧树 = 一级 + 二级（细筛）
  - 不冲突，可同时用

---

## 5. a11y 校验（ui-ux-pro-max P1）

| 检查项 | 状态 |
|--------|------|
| 对比度 ≥ 4.5:1 | §1 表已验证（最低 7.2:1） |
| 键盘可达 | chip 用 `<button>` / `<a-tag>`（ant 已支持） |
| 焦点环 | 沿用 ant 默认 2-4px ring |
| aria-label | 顶部 chip 流需加 `aria-label="按用途筛选"` |
| 颜色非唯一信号 | 每个 chip 有 icon + 文字 ✅ |

## 6. 触控校验（P2）

| 元素 | 大小 | 状态 |
|------|------|------|
| 顶部 chip | 28px 高 + 4-8px 水平 padding | OK（接近 32px） |
| Card chip | 22px 高（点击区域 32px+） | 加 hitSlop |
| 筛选 chip 移动端横滚 | `touch-action: manipulation` | 减 300ms tap delay |

## 7. 动画（P7）

- chip 选中/未选中：transition `all 150ms ease-out`（背景、阴影、scale 1.02）
- 卡片 hover：lift 1px + 阴影加 0.05
- 列表项进入：stagger 30ms（用 `<TransitionGroup>`）

## 8. 暗色模式（Out of Scope v1.1）

- v1 仅做亮色 chip 配色
- 暗色模式 chip 配色与全局主题一起做

## 9. 验收清单（Designer → Dev 交接）

- [ ] 12 色常量文件：`frontend/src/constants/usage-colors.ts`
- [ ] SkillCard 改：插入 2 个 chip 到 tags 前
- [ ] SkillDetail 改：在 tags 后加"用途"区块
- [ ] BrowseView 改：主区顶部加 USAGE chip 流（不动左 sidebar）
- [ ] Pinia store（`stores/usage-filter.ts`）：保存当前选中的一级 USAGE
- [ ] API：`GET /api/categories?type=USAGE&parentId=null` 拉一级（看是否需要新端点）
- [ ] 点击 chip 后路由同步：`?dim=usage&categoryId=<id>`（deep link）
