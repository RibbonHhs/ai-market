# S28 暗色手动切换按钮 + 偏好持久化 — 需求

> **Sprint**：S28（接续 S27 / S26 / S25 / S24）
> **范围**：P0，0.5 sprint
> **状态**：✅ 已完成

---

## 1. 背景

S27 落地的暗色全站化**只跟系统设置**（`prefers-color-scheme`）。用户希望：
- 临时想看浅色（系统是暗色）
- 临时想看暗色（系统是浅色）
- 跨会话记住偏好

## 2. 目标

Header 加一个三态循环切换按钮（**auto / light / dark**），选择写 localStorage 持久化 + 跨标签同步 + auto 态跟系统切换。

## 3. User Story

### US-1 手动切换
**作为**用户，**我希望**在 Header 看到切换按钮，**以便**在不修改系统设置的情况下立即切换主题。

验收：点击按钮 3 次循环 `auto → light → dark → auto`，对应图标 🌓 / ☀ / 🌙 切换。

### US-2 偏好持久化
**作为**用户，**我希望**刷新页面或下次访问时记住我选的主题，**以便**不用每次重新选。

验收：选择 `dark` 后 F5 刷新，仍是 `dark`；localStorage `skillsmap.theme` = `'dark'`。

### US-3 a11y
**作为**键盘用户 / 屏幕阅读器用户，**我希望**按钮能 Tab 聚焦 + Space/Enter 触发 + aria-label 描述当前态，**以便**无障碍使用。

验收：Tab 按钮聚焦 2px 焦点环；aria-label 包含"当前主题: X，点击切换下一态"；Space 触发 cycle。

### US-4 跨标签同步
**作为**多标签用户，**我希望**标签 A 切到 dark 后标签 B 立即同步，**以便**不重复操作。

验收：标签 A 切 dark，标签 B 立即变暗（无需刷新）。

## 4. 决策已锁

| Q | 决策 | 理由 |
|---|------|------|
| 状态机 | **3 态：auto / light / dark** | 兼容 S27 系统跟随 |
| 持久化 key | `localStorage.skillsmap.theme` | 与 `auth` cache key 同源（LocalPrivateCache 风格） |
| Pinia 风格 | **Options API** | 与现有 `app.ts` / `auth.ts` 一致 |
| 按钮位置 | **Header 右侧、搜索 Ctrl+K 按钮前** | 视觉连续性（搜索 → 主题 → 用户） |
| 按钮形态 | **图标按钮（无文字）** + tooltip + aria-label | 桌面紧凑，移动端可点击 32×32 |
| 焦点环 | 2px primary 色（Ant Design 默认） | a11y 标准 |
| 跨标签同步 | `window.addEventListener('storage', ...)` | W3C StorageEvent 标准 API |
| 跨系统同步（auto 态） | `matchMedia('change', ...)` | 跟 S27 逻辑复用 |
| 切到 light/dark 时 | `data-theme="light"` / `data-theme="dark"` | 强制覆盖；auto 时 `delete dataset.theme` 走 media query |
| 不引 | vue-use / pinia-plugin-persistedstate | 范围最小化 |

## 5. 验收标准

- [x] `docs/sprints/S28/requirements.md` 存在（本文件）
- [x] `docs/sprints/S28/toggle-spec.md` 存在
- [x] `docs/sprints/S28/handoff.md` 13 章节齐全
- [x] `npm run build` 0 错
- [x] `npm run test:e2e` 12/12 PASS（5 S26 + 6 S27 + 1 S28）
- [x] Pinia theme store 存在（≤ 60 行）
- [x] Header 切换按钮存在（data-testid="theme-toggle"）
- [x] 3 张截图归档（light / dark / auto）
- [x] 浅色基准线**未破**（S27 暗色全站化仍生效）
- [x] S26 / S27 全部 e2e 仍过

## 6. out of scope

- F+G 限流运维化 → S29
- B USAGE 多对多 → S30+
- H LLM 二次分类 → S31+
- 跨实例 Redis 共享 → S29+
- 跨浏览器 webkit+firefox → S32+
- 切换按钮放在设置页 / 个人中心（**只在 Header**）

## 7. 完成报告

Sprint S28 范围小（Pinia store + 按钮 + 1 e2e + 3 截图 + 4 文档），由 Lead 直接接管（subagent 卡在 PM/Designer 阻塞逻辑，自接管更可控）。交付后用户可手动切主题。
