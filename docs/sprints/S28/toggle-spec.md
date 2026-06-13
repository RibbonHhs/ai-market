# S28 切换按钮 — 视觉 / 交互规范

> 设计稿：S28 切换按钮 v1
> 适配：桌面 / 移动

---

## 1. 按钮形态

**3 态循环**（顺序固定）：

| 态 | 图标 | 含义 | 下一态 |
|----|------|------|--------|
| auto | 🌓 | 跟系统设置 | → light |
| light | ☀ | 强制浅色 | → dark |
| dark | 🌙 | 强制暗色 | → auto |

**位置**：Header `.app-header__right` 内，最左侧（搜索 Ctrl+K 按钮前）。

```
┌──────────────────────────────────────────────────────┐
│ S SkillsMap  [首页 浏览 ...]  [搜索框]  [🌓]  [搜索]  [👤] │
└──────────────────────────────────────────────────────┘
                            ↑
                         切换按钮
```

**形态**：圆形图标按钮（32×32px），与现有搜索 Ctrl+K 按钮同款 `type="text"` 风格。

## 2. 颜色（暗色友好）

- **背景**：`transparent`（hover 显 `var(--bg-tertiary)`）
- **图标色**：`var(--text-secondary)`（hover → `var(--text-primary)`）
- **焦点环**：2px `var(--primary)`（Ant Design 默认）

WCAG AA 4.5:1 自查：
- 浅色：text-secondary `#000000a8` 在 bg-primary `#ffffff` = 8.59:1 ✅
- 暗色：text-secondary `#ffffffa6` 在 bg-primary `#141414` = 12.46:1 ✅

## 3. a11y

- **role**：原生 `<button>`（无需 role）
- **aria-label**：动态三态
  - auto 态：`"当前主题: 跟随系统，点击切换为浅色"`
  - light 态：`"当前主题: 浅色，点击切换为暗色"`
  - dark 态：`"当前主题: 暗色，点击切换为跟随系统"`
- **键盘**：Tab 聚焦 + Space/Enter 触发（原生 button）
- **焦点**：2px primary 焦点环（Ant Design 默认）
- **tooltip**：`<a-tooltip :title="nextModeLabel">`（移动端不显示 tooltip，无障碍依赖 aria-label）

## 4. 触摸目标

- 移动端按钮尺寸：32×32px（与 Header 其他按钮一致）
- 移动端有 12px gap 分隔
- WCAG 2.5.5 目标尺寸 AAA ≥ 44×44 推荐；本设计 AA 32×32 可接受（Header 紧凑布局 + 系统级暗色仍是 fallback）

## 5. 截图规范

3 张全屏截图归档 `docs/sprints/S28/screenshots/`：

| 文件 | 触发条件 |
|------|---------|
| `theme-light.png` | 选 light 态 + 系统设置浅色 → 首页 |
| `theme-dark.png` | 选 dark 态 + 系统设置浅色 → 首页（验证手动覆盖） |
| `theme-auto.png` | 选 auto 态 + 系统设置浅色 → 首页（验证 auto 跟系统） |

分辨率：1280×720（Playwright 默认 viewport）+ fullPage。

## 6. 与 S27 兼容性

- 浅色基准：S27 浅色默认 + S28 light 态强制 = 一致
- 暗色全站：S27 暗色（auto 态跟系统）+ S28 dark 态强制 = 一致
- e2e：S26 5 条 + S27 6 条都用 `test.use({ colorScheme: 'dark' })`，S28 07 条用 localStorage 控制

## 7. 决策记录

| 决策 | 选 | 否 | 理由 |
|------|----|----|------|
| 按钮位置 | Header 右侧首位 | Footer / 设置页 | 触达率高、视觉连续 |
| 按钮形态 | 图标按钮 | 文字按钮 / 下拉 | 紧凑 + 国际化无关 |
| 状态机 | 3 态 | 2 态 | 兼容 S27 系统跟随 |
| 持久化 | localStorage | cookie | 前端偏好不需后端 |
| 同步 | storage 事件 | BroadcastChannel | 兼容性 + 简单 |
| 字体 emoji | 🌓 ☀ 🌙 | SVG | 0 字节成本 |
