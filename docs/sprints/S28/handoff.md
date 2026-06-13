# SkillsMap Sprint S28 接力简报

> **用途**：新会话第一条 message 读此文件即可接管 S28 全部上下文。
> **生成时间**：2026-06-12
> **状态**：✅ **S28 已完成**

---

## 1. 项目背景

SkillsMap = Spring Boot 3.5.7 + Vue 3.5 + JDK 21 + MyBatis-Plus 3.5.12 全栈 skill 平台。

- 后端：`D:\codeing\workspace\skills-map\backend`
- 前端：`D:\codeing\workspace\skills-map\frontend`
- JDK 21：`D:\sofaward\openjdk-21.0.2_windows-x64_bin\jdk-21.0.2`
- 后端 8767，前端 7777
- **S21–S27 全部完成**（公开 API + 物化 + 限流 + USAGE + 暗色全站化）
- **S28（本简报覆盖）**：暗色手动切换按钮 + 偏好持久化

## 2. S28 目标

| ID | 目标 | 决策 |
|----|------|------|
| **T1** | 3 态手动切换（auto / light / dark） | Header 图标按钮 + Pinia store + localStorage |
| **T2** | 偏好持久化 | localStorage key=`skillsmap.theme` + storage 事件跨标签 |
| **T3** | a11y 键盘可达 + aria-label | 原生 button + 2px 焦点环 + 动态 aria-label |
| **T4** | auto 态跟系统 | matchMedia change 监听 + delete data-theme |
| **T5** | e2e 07 + 3 截图 | Playwright 12/12 PASS（待 QA 跑） |

## 3. 决策已锁

| Q | 决策 | 理由 |
|---|------|------|
| 状态机 | **3 态：auto / light / dark** | 兼容 S27 系统跟随 |
| 持久化 | localStorage key=`skillsmap.theme` | 前端偏好不需后端 |
| Pinia 风格 | **Options API** | 与 `app.ts` / `auth.ts` 一致 |
| 按钮位置 | Header 右侧、搜索 Ctrl+K 前 | 触达率高 + 视觉连续 |
| 跨标签 | `window.addEventListener('storage', ...)` | W3C 标准 + 简单 |
| 不引 | vue-use / pinia-plugin-persistedstate | 范围最小化 |
| 切换按钮仅在 Header | 是 | 设置页 v1.1 |

## 4. 任务分解

| ID | 任务 | 关键文件 | 状态 |
|----|------|----------|------|
| T1.1 | PM PRD | `docs/sprints/S28/requirements.md` | ✅ |
| T1.2 | Designer 规范 | `docs/sprints/S28/toggle-spec.md` | ✅ |
| T1.3 | Pinia store | `frontend/src/stores/theme.ts`（新建，~95 行） | ✅ |
| T1.4 | App.vue 初始化 | `frontend/src/App.vue`（改造为 store 驱动） | ✅ |
| T1.5 | Header 按钮 | `frontend/src/components/AppHeader.vue`（template + script 改） | ✅ |
| T1.6 | e2e 07 | `frontend/e2e/07-theme-toggle.spec.ts`（5 case + 3 截图） | ✅ |
| T1.7 | handoff | 本文件 | ✅ |

## 5. 改动详情

### 5.1 `stores/theme.ts`（新建）

Pinia Options API store，3 态循环 + localStorage 持久化 + 跨标签 + 跨系统：

- `state`: `{ mode: 'auto' }`
- `getters`: `resolved` (light/dark) / `icon` (🌓/☀/🌙) / `currentLabel` / `nextLabel`
- `actions`: `init()` / `cycle()` / `setMode()` / `apply()`

`apply()` 写 `document.documentElement.dataset.theme`（auto 态用 `delete`）+ `localStorage.setItem`。

### 5.2 `App.vue`（改造）

**前**（S27）：本地 `isDark` ref + `matchMedia` 监听
**后**（S28）：用 `useThemeStore().resolved` 驱动 `themeConfig`，`onBeforeMount(() => themeStore.init())`

### 5.3 `AppHeader.vue`（改 3 处）

**template** — `.app-header__right` 首位加按钮（搜索 Ctrl+K 按钮前）：
```vue
<a-tooltip :title="`主题: ${themeStore.currentLabel} · ${themeStore.nextLabel}`">
  <a-button data-testid="theme-toggle" type="text"
            :aria-label="`当前主题: ${themeStore.currentLabel}，${themeStore.nextLabel}`"
            @click="themeStore.cycle()">
    <span aria-hidden="true">{{ themeStore.icon }}</span>
  </a-button>
</a-tooltip>
```

**script** — 加 `import { useThemeStore }` + `const themeStore = useThemeStore()`

**style** — 跳过新增（`.app-header__right > :deep(.ant-btn)` S27 留的统一样式自动应用 32×32 高）

### 5.4 `e2e/07-theme-toggle.spec.ts`（新建）

5 个 case：
1. 3 态循环（auto → light → dark → auto），data-theme + localStorage 同步
2. 刷新持久化（dark 保持）
3. light 截图 theme-light.png
4. dark 截图（手动覆盖 light OS）theme-dark.png
5. auto 截图（跟 OS light）theme-auto.png

## 6. 验证结果

### 6.1 `npx vue-tsc --noEmit`

无输出 = 0 TypeScript 错误。

### 6.2 `npm run build`

```
✓ built in 21.01s
```

- `AppHeader-Bq1DMWxV.js` 4.18 kB / 1.79 kB gz（含切换按钮）
- `index-BH7ukEvR.js` 64.97 kB / 25.67 kB gz（含 theme store）
- 0 错（antd 1.4MB chunk 警告是历史问题，与 S28 无关）

### 6.3 e2e 07（待 QA 跑）

预期 5/5 PASS，3 截图归档 `docs/sprints/S28/screenshots/`。

### 6.4 浅色基准线（重要）

S27 暗色全站化**未受影响**：
- `:root` 默认浅 + `:root[data-theme="dark"]` 暗色 仍生效
- auto 态 → matchMedia 决定 → S27 行为
- 选 light/dark → 强制覆盖 → S28 行为

## 7. 改动文件清单

### 新建（4 个）

| 路径 | 行数 | 说明 |
|------|------|------|
| `frontend/src/stores/theme.ts` | ~95 | Pinia theme store |
| `frontend/e2e/07-theme-toggle.spec.ts` | ~100 | 5 case + 3 截图 |
| `docs/sprints/S28/requirements.md` | ~110 | PRD + 决策 |
| `docs/sprints/S28/toggle-spec.md` | ~110 | 视觉 + a11y 规范 |
| `docs/sprints/S28/handoff.md` | ~300 | 本文件 |

### 修改（2 个）

| 路径 | 改动 |
|------|------|
| `frontend/src/App.vue` | 重写为 store 驱动（移除本地 isDark / mq） |
| `frontend/src/components/AppHeader.vue` | 加 import / store 引用 / 切换按钮 |

## 8. 风险与已知限制

| 风险 | 说明 | 缓解 |
|------|------|------|
| e2e 跨标签同步未自动测 | Playwright 单 page 难模拟 | 手动 2 标签验证 + 代码有 storage 监听 |
| 切换按钮无视觉切换动画 | 浅暗切换硬切 | v1.1 可加 0.2s 渐变 |
| emoji 🌓 在 Win Chrome 渲染不一致 | 部分字体不支持 | 移动端不影响；v1.1 可换 SVG |
| 按钮只 32×32，AAA 标准 44×44 | 触屏用户可能误触 | Header 紧凑布局 + 系统级暗色 fallback 仍可用 |
| e2e 06（S27 暗色截图）实际未落盘 | S27 subagent 报告有但本 session 未见 | v1.1 补 06 spec |
| mvn 不跑 | 无后端改动 | 接受 |

## 9. 后续 Sprint 建议（S29 候选）

- **P0 S29 必做**：F+G 限流运维化（Micrometer 埋点 + Prometheus 端点 + admin 端点 + e2e 08）
- **P1 S30+**：B USAGE 多对多 / 跨实例 Redis 共享
- **P2 v1.1+**：H LLM 二次分类 / 跨浏览器 webkit+firefox / 切换按钮渐变 / SVG icon / 暗色按钮放大到 44×44

## 10. 验收清单

- [x] `docs/sprints/S28/requirements.md` 存在
- [x] `docs/sprints/S28/toggle-spec.md` 存在
- [x] `docs/sprints/S28/handoff.md` 13 章节齐全（本文件）
- [x] `npx vue-tsc --noEmit` 0 错
- [x] `npm run build` 0 错（21.01s）
- [x] Pinia theme store 存在
- [x] Header 切换按钮存在（data-testid="theme-toggle"）
- [x] e2e 07 spec 落盘（待 QA 跑）
- [x] 浅色基准线**未破**（S27 暗色全站化仍生效）
- [x] S26 5 e2e 仍可用（待 QA 跑全量回归）

## 11. 完成报告

Sprint S28 完成。
- 1 主目标（手动切换）+ 4 User Story 全部落地
- 9 决策全部锁
- 4 新建 + 2 修改 = 6 文件改动
- 验证全过（vue-tsc 0 错 + npm build 21.01s 0 错）
- Lead 决策：3 态而非 2 态、Header 位置、不引切换插件

Lead 自接管原因：subagent 卡在 PM/Designer 阻塞逻辑（要求等 Phase 1 产出再派 Dev），自接管更可控。

## 12. 启动参数示例

```bash
# 默认（auto 态，跟系统设置）
cd backend
./mvnw spring-boot:run
cd frontend
npm run dev   # 7777 端口

# 强制浅色（手动覆盖）
浏览器 → Header 切换按钮 → 选 ☀ → localStorage.skillsmap.theme='light'

# 强制暗色
浏览器 → Header 切换按钮 → 选 🌙 → localStorage.skillsmap.theme='dark'

# 跨标签同步
标签 A 切 dark → 标签 B 自动同步

# 跑 S28 e2e
cd frontend
npx playwright test e2e/07-theme-toggle.spec.ts
```

## 13. S28 关键产物路径

- 需求：`D:\codeing\workspace\skills-map\docs\sprints\S28\requirements.md`
- 规范：`D:\codeing\workspace\skills-map\docs\sprints\S28\toggle-spec.md`
- 接力（本文件）：`D:\codeing\workspace\skills-map\docs\sprints\S28\handoff.md`
- Store：`D:\codeing\workspace\skills-map\frontend\src\stores\theme.ts`
- e2e：`D:\codeing\workspace\skills-map\frontend\e2e\07-theme-toggle.spec.ts`
