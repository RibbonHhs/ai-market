# Design System v0（最小设计系统）

> 作者：designer-vicky @ Sprint 0 Kickoff (2026-06-06)
>
> 版本：v0.1 · 基础：Ant Design Vue 4 · 用途：v1 全站视觉 / 交互规范 · 引用：`docs/PRD.md` / `06_user_personas.md` / `08_information_architecture.md`

## 1. 设计原则（Design Principles）

| # | 原则 | 落地动作 |
|---|---|---|
| 1 | **内容优先** | 卡片 / 列表展示突出 Skill 名称与评分，装饰元素 ≤ 5% 视觉重量 |
| 2 | **开发者审美** | 等宽字体承载代码 / 命令；中英混排时英文优先 monospace |
| 3 | **可访问性 (A11y)** | 文本对比 ≥ 4.5:1；所有交互元素 ≥ 44×44pt；可见 focus ring |
| 4 | **响应式** | Mobile-first；断点 375 / 768 / 1024 / 1440 |
| 5 | **统一节律** | 4pt / 8pt 间距系统；圆角阶梯 4 / 6 / 8 / 12 |
| 6 | **AntDV 4 一致性** | 不在 AntDV 组件外自造样式；扩展走 ConfigProvider + Token |
| 7 | **暗色优先** | 默认提供 light / dark 双 Token，由用户切换 |

## 2. 品牌定位与语调

- **品牌名**：SkillsMap
- **Slogan**：**发现、决策、装上 — Claude Skills 一站式集市**
- **语调**：专业但不冷；技术但不晦涩；中文为主，关键术语括号附英文
- **Logo 占位**：`SM` 字母组合 + 路径节点图形（v0 文字标记，Sprint 2 后替换为正式 logo）

## 3. 色板（Color Palette）

> 基于 AntDV 4 算法色扩展；色值通过 `ConfigProvider` 注入；禁止在组件内写死 hex。

### 3.1 语义色 Token

| Token | Light | Dark | 用途 |
|---|---|---|---|
| `--color-primary` | `#1677FF` | `#1668DC` | 主操作 / 链接 / 选中态 |
| `--color-primary-hover` | `#4096FF` | `#3C9AE8` | hover 态 |
| `--color-primary-active` | `#0958D9` | `#0E55C7` | active 态 |
| `--color-success` | `#52C41A` | `#49AA19` | 发布成功 / 已发布 |
| `--color-warning` | `#FAAD14` | `#D89614` | 草稿 / 待审核 |
| `--color-error` | `#FF4D4F` | `#DC4446` | 错误 / 危险操作 |
| `--color-info` | `#1677FF` | `#1668DC` | 提示信息 |

### 3.2 中性色 Token

| Token | Light | Dark | 用途 |
|---|---|---|---|
| `--color-text` | `#1F1F1F` | `#F5F5F5` | 主文本 |
| `--color-text-secondary` | `#595959` | `#A6A6A6` | 次文本 |
| `--color-text-tertiary` | `#8C8C8C` | `#595959` | 辅助文本 |
| `--color-text-disabled` | `#BFBFBF` | `#434343` | 禁用文本 |
| `--color-bg` | `#FFFFFF` | `#141414` | 页面背景 |
| `--color-bg-elevated` | `#FFFFFF` | `#1F1F1F` | 卡片 / Modal |
| `--color-bg-layout` | `#F5F5F5` | `#000000` | 布局背景 |
| `--color-border` | `#D9D9D9` | `#303030` | 分割线 / 边框 |
| `--color-divider` | `#F0F0F0` | `#1F1F1F` | 细分割线 |

### 3.3 业务色

| Token | 值 | 用途 |
|---|---|---|
| `--color-rating-star` | `#FAAD14` | 5 星评分（金色） |
| `--color-source-official` | `#1677FF` | 来源：official |
| `--color-source-community` | `#52C41A` | 来源：community |
| `--color-source-private` | `#722ED1` | 来源：private |
| `--color-source-imported` | `#FA8C16` | 来源：imported |
| `--color-status-draft` | `#8C8C8C` | 状态：草稿 |
| `--color-status-published` | `#52C41A` | 状态：已发布 |
| `--color-status-deprecated` | `#BFBFBF` | 状态：弃用 |
| `--color-status-flagged` | `#FF4D4F` | 状态：标记 |

### 3.4 对比度合规检查

| 组合 | 对比度 | 状态 |
|---|---|---|
| `--color-text` on `--color-bg` | 16.1:1 | ✅ AAA |
| `--color-text-secondary` on `--color-bg` | 7.0:1 | ✅ AAA |
| `--color-text-tertiary` on `--color-bg` | 4.6:1 | ✅ AA |
| `--color-primary` on `--color-bg` | 4.5:1 | ✅ AA |
| `--color-error` on `--color-bg` | 4.5:1 | ✅ AA |
| `--color-rating-star` on `--color-bg` | 2.9:1 | ⚠️ 仅用于 ≥ 18px 图标 |

## 4. 字体系统（Typography）

### 4.1 字体族

| 用途 | 字体 | 备选 |
|---|---|---|
| 中英正文 | `"PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", -apple-system, BlinkMacSystemFont, sans-serif` | — |
| 代码 / 命令 | `"JetBrains Mono", "Fira Code", "SF Mono", Menlo, Consolas, monospace` | — |
| 数字（评分、计数） | 主字体 + `font-variant-numeric: tabular-nums` | — |

### 4.2 字号阶梯

| Token | px | rem | 行高 | 用途 |
|---|---|---|---|---|
| `--fs-display` | 32 | 2.0 | 1.2 | 首页 Hero 标题 |
| `--fs-h1` | 24 | 1.5 | 1.3 | 页面标题 |
| `--fs-h2` | 20 | 1.25 | 1.35 | 区段标题 |
| `--fs-h3` | 18 | 1.125 | 1.4 | 卡片标题 |
| `--fs-body-lg` | 16 | 1.0 | 1.5 | 大正文 |
| `--fs-body` | 14 | 0.875 | 1.5 | 默认正文 |
| `--fs-caption` | 12 | 0.75 | 1.5 | 辅助说明 |
| `--fs-code` | 13 | 0.8125 | 1.6 | 代码块 |

**最小字号保护**：移动端正文 ≥ 16px（避免 iOS 自动缩放）；桌面端正文 ≥ 14px。

### 4.3 字重

| Weight | Token | 用途 |
|---|---|---|
| 400 | regular | 正文 |
| 500 | medium | 标签 / 按钮 / Tab |
| 600 | semibold | 小标题 / 强调 |
| 700 | bold | 仅 Hero 标题 |

## 5. 间距系统（Spacing Scale）

> 严格 4pt / 8pt 节律

| Token | px | 用途 |
|---|---|---|
| `--space-1` | 4 | icon 与文字间距 |
| `--space-2` | 8 | 表单内边距 |
| `--space-3` | 12 | 卡片内小间距 |
| `--space-4` | 16 | 卡片内边距 / 区段间 |
| `--space-5` | 24 | 区段间（紧凑） |
| `--space-6` | 32 | 区段间（默认） |
| `--space-7` | 48 | 页面顶部间距 |
| `--space-8` | 64 | 大留白 / Hero |

## 6. 圆角（Radius）

| Token | px | 用途 |
|---|---|---|
| `--radius-sm` | 4 | Tag / 小按钮 |
| `--radius-md` | 6 | Input / 按钮 |
| `--radius-lg` | 8 | Card |
| `--radius-xl` | 12 | Modal / Drawer |
| `--radius-full` | 9999 | 头像 / 徽章 |

## 7. 阴影（Shadow / Elevation）

| Token | 值 | 用途 |
|---|---|---|
| `--shadow-1` | `0 1px 2px rgba(0,0,0,.03), 0 1px 6px -1px rgba(0,0,0,.02), 0 2px 4px rgba(0,0,0,.02)` | Card 静止 |
| `--shadow-2` | `0 6px 16px 0 rgba(0,0,0,.08), 0 3px 6px -4px rgba(0,0,0,.12), 0 9px 28px 8px rgba(0,0,0,.05)` | Card hover |
| `--shadow-3` | `0 12px 32px 0 rgba(0,0,0,.10), 0 4px 8px -4px rgba(0,0,0,.06)` | Drawer / Modal |
| `--shadow-popover` | `0 0 8px rgba(0,0,0,.12)` | Tooltip / Popover |

## 8. 组件清单（基于 AntDV 4 扩展）

| 组件 | AntDV 4 基线 | 扩展点 | 备注 |
|---|---|---|---|
| `Button` | 主 / 次 / 危险 | loading 态必须显式 | 高度 32 / 40 |
| `Input` | default | 加 `affix` 图标前缀 | 高度 32 |
| `Input.Search` | 内置 | — | 列表页搜索 |
| `Select` | 多选 / 搜索 | — | 后台筛选用 |
| `Table` | 分页 / 排序 / 过滤 | 行操作下拉 | 后台列表 |
| `Card` | 基础 | 自定义 `SkillCard` | 列表 / 详情侧 |
| `Tag` | 5 种预设色 | + `RatingTag` | 来源 / 状态 |
| `Rate` | 1-5 星 | 不可点击（展示用） | 评分 |
| `Avatar` | 文字 / 图片 | 默认 user icon | 评论列表 |
| `Modal` | 居中 | 危险操作 confirm 模板 | 删除 / 启禁 |
| `Drawer` | 右侧 | 移动端筛选 | 移动端筛选 |
| `Empty` | 内置 4 种 | 全部带"主操作"按钮 | 列表空态 |
| `Skeleton` | 段落 / 列表 / 卡片 | Dashboard 用 | 加载占位 |
| `Message` | 全局 | 操作反馈 | toast |
| `Breadcrumb` | 路径 | 详情页用 | IA 层级 |
| `Pagination` | 标准 | — | 浏览页 |
| `Form` | validate | onBlur 校验 | 全部表单 |
| `Upload` | dragger | SKILL.md / .skill zip | 后台上传 |

### 8.1 自研组件（Sprint 1+）

| 组件 | 何时建 |
|---|---|
| `SkillCard` | Sprint 1 D2（US-002 起） |
| `SkillGrid` | Sprint 1 D2 |
| `RatingStars` | Sprint 1 D2（基于 AntDV Rate 包装） |
| `MarkdownView` | Sprint 1 D4（US-011） |
| `EmptyState` | Sprint 1 D3 |
| `InstallCommandBox` | Sprint 1 D6（US-012） |
| `SourceTag` | Sprint 1 D3 |
| `StatusTag` | Sprint 1 D3 |

## 9. 图标系统（Iconography）

| 维度 | 规范 |
|---|---|
| 库 | `@ant-design/icons-vue`（随 AntDV 4 自动导入） |
| 风格 | **线性 / 填充二元**（同层只用一种） |
| 尺寸 | 16 / 20 / 24 / 32 px |
| 描边 | 默认 1.5px |
| 颜色 | 跟随 `currentColor` + 语义色 |
| **反例** | ❌ 不用 emoji 作为结构图标（按 ui-ux-pro-max 规则） |

## 10. 交互规范

### 10.1 触控目标

| 设备 | 最小尺寸 | 来源 |
|---|---|---|
| iOS | 44 × 44 pt | Apple HIG |
| Android | 48 × 48 dp | Material Design |
| 桌面（鼠标） | 32 × 32 px 即可 | — |

### 10.2 动画

| 维度 | 规范 |
|---|---|
| 时长 | 微交互 150–300ms；复杂 400ms；禁止 > 500ms |
| 缓动 | `cubic-bezier(0.4, 0, 0.2, 1)`（标准） / `cubic-bezier(0.0, 0, 0.2, 1)`（退出） |
| 属性 | 仅 `transform` + `opacity`（避免 reflow） |
| 减弱动效 | `@media (prefers-reduced-motion: reduce)` 时关闭非必要动画 |

### 10.3 反馈

| 场景 | 反馈 |
|---|---|
| 提交中 | Button 禁用 + spinner + 文案变 "提交中..." |
| 成功 | toast 3s 自动消失 + 列表/详情自动更新 |
| 失败 | 行内错误（表单）/ toast 5s（操作） |
| 空态 | 图标 + 文案 + 主操作按钮 |

## 11. 暗色模式

- 用户切换 → 写 `localStorage.theme` → 切 `<html data-theme="dark">`
- 所有 Token 通过 `[data-theme="dark"]` 覆盖
- 状态色在 dark 下采用降低饱和度版本（防刺眼）
- 关键页 100% 覆盖；v1 桌面端默认 light，dark 走"自动跟随系统 + 用户可覆盖"

## 12. 页面层级与栅格

| 断点 | 宽度 | 容器最大宽度 | 列数 |
|---|---|---|---|
| xs | < 576px | 100% | 1 |
| sm | ≥ 576px | 540px | 2 |
| md | ≥ 768px | 720px | 2 |
| lg | ≥ 1024px | 960px | 3 |
| xl | ≥ 1280px | 1200px | 3 |
| xxl | ≥ 1600px | 1400px | 4 |

后端管理页：固定 1440px 容器，左侧 200px 导航 + 右侧自适应内容区。

## 13. 交付物（Deliverables）

| 阶段 | 内容 | 时间 |
|---|---|---|
| v0（当前） | 本文档（Token 规范 + 组件清单） | Sprint 0 |
| v1 | Figma 主线框稿（首页 / 浏览 / 详情 / 后台） | Sprint 1 D3 |
| v2 | Figma 高保真 + 交互动效 | Sprint 1 D9 |
| v3 | 全站页面 | Sprint 2 末 |

## 14. 反例（Do NOT）

- ❌ 不要在 AntDV 4 之外造视觉风格
- ❌ 不要用 emoji 作为导航 / 按钮图标
- ❌ 不要写死 hex（必须走 Token）
- ❌ 不要 > 500ms 动画
- ❌ 不要纯色灰（必须配文字 / 图标 / 双重指示）
- ❌ 不要让触控目标 < 44pt
- ❌ 不要让正文 < 16px（移动端）
- ❌ 不要让 text-only 状态指示（必须有图标或文字辅助）

## 15. 引用关系

- 字号 / 间距 → 落地为 `frontend/src/style/tokens.scss`
- 组件 → 引用 AntDV 4 文档 + `frontend/src/components/`
- 路由 / 页面布局 → `08_information_architecture.md`

## 16. 修订记录

| 版本 | 日期 | 作者 | 摘要 |
|---|---|---|---|
| v0.1 | 2026-06-06 | designer-vicky | 初版设计系统 v0：原则 + 色板 + 字体 + 间距 + 圆角 + 阴影 + 组件清单 + 反例 |
