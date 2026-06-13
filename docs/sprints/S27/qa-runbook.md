# S27 QA Runbook — 暗色全站化

> **承接**: S26 `qa-runbook.md`（5 条 e2e） + S27 `requirements.md`（4 条 User Story）
> **范围**: 验证 7+ Vue 组件暗色化 + 6 张暗色截图 + 浅色基准线未破
> **不动**: 后端 / 路由 / Pinia store

---

## 1. 前置条件

1. 后端 8767 已起：`cd backend && ./mvnw spring-boot:run` 或 Docker
2. 前端依赖就绪：`cd frontend && npm install`（S26 已装）
3. H2 seed 7 个 skill 已入库（S25 已 seed）

## 2. 执行步骤

### 2.1 浅色基准回归（S26 5 条 e2e + S25 chip 浅色）

```bash
cd frontend
npm run test:e2e
# 期望 5/5 PASS（含 05-dark-mode 用 dark colorScheme）
# 注意：S27 新增 06-dark-screenshots.spec.ts 6 条 spec 也跑，期望 6/6 PASS
# 总计 11/11
```

### 2.2 构建检查

```bash
cd frontend
npm run build
# 期望 0 错（warning 可接受）
# 期望 dist/index.html 生成
```

### 2.3 6 张暗色截图归档

`npm run test:e2e` 自动跑 `e2e/06-dark-screenshots.spec.ts` 6 条 spec，存到：
- `docs/sprints/S27/screenshots/dark-home.png`
- `docs/sprints/S27/screenshots/dark-browse.png`
- `docs/sprints/S27/screenshots/dark-detail.png`
- `docs/sprints/S27/screenshots/dark-sidebar.png`（局部：仅左 sidebar）
- `docs/sprints/S27/screenshots/dark-apiguide.png`
- `docs/sprints/S27/screenshots/dark-favorite.png`

### 2.4 浅色手测（基准线未破）

Chrome DevTools → Rendering → 关闭 `prefers-color-scheme: dark`（设成 no-preference 或 light），访问：
- `/` — 浅色首页，背景 #ffffff
- `/browse-skills` — 浅色 browse，sidebar 白底
- `/skills/{slug}` — 浅色 detail
- `/api-guide` — 浅色文档页
- `/me` — 浅色收藏页

**关键检查**：
- chip 流颜色 = S25 浅色（`#F0F5FF` 蓝底等 12 色）
- 卡片底色 = 白
- 文字 = `--text-primary` 浅色值
- 没有任何元素残留暗色

### 2.5 暗色手测（5 视图）

Chrome DevTools → Rendering → `prefers-color-scheme: dark`，访问：
- `/` — 暗色首页，背景 #141414，文字 #fff85 透明度
- `/browse-skills` — 暗色 browse，sidebar 深灰 #1f1f1f
- `/skills/{slug}` — 暗色 detail，body 深色
- `/api-guide` — 暗色文档页，代码块 #0d1117
- `/me` — 暗色收藏页

**关键检查**：
- 没有任何白色 / 浅色硬编码背景残留
- 文字对比度 ≥ 4.5:1（主文 ≥ 14.8:1 AAA）
- 边框 / 分割线在暗色下可见
- chip 流保持 12 色暗色变体（S25 已正确）

## 3. WCAG AA 自查（目测 + 文字记录）

不需要写 axe-core 脚本。直接看 6 张截图 + 关键文字：

| 配对 | 预期比值 | 截图证据 |
|------|----------|----------|
| 主文字 on 主背景 | ≥ 14:1 | dark-home / dark-browse 顶部 |
| 次文字 on 主背景 | ≥ 8:1 | dark-detail 描述、metadata |
| 主按钮文字 on 主按钮 | ≥ 4.5:1 | dark-browse chip 流 |
| 链接 on 主背景 | ≥ 4.5:1 | dark-detail body 链接 |
| 边框 on 背景 | ≥ 3:1 | dark-browse sidebar 边 |

## 4. 验收清单（DoD）

- [ ] `npm run build` 0 错
- [ ] `npm run test:e2e` 11/11 PASS（S26 5 + S27 6）
- [ ] 6 张暗色截图归档
- [ ] 浅色基准未破（手测 5 视图）
- [ ] 暗色无残留白底（手测 5 视图）
- [ ] 文字对比度 ≥ 4.5:1（目测）
- [ ] 提交：commit co-author `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`

## 5. 已知限制

- 暗色截图 06-favorite 用未登录态（拍 `/me` 引导页），如需拍已登录收藏列表，需先注册 + 加收藏再跑（v1.1 优化）
- chrome devtools emulateMedia 触发的是浏览器级，系统级 `prefers-color-scheme` 需在 OS 层面切
- 第三方 antdv `a-tag color="blue"` 等颜色 prop 未被 CSS 变量覆盖（部分 chip / 徽章），**不动** S25 chip 流

## 6. 回滚

如回归：直接 `git revert` 即可。S27 改动仅 12 个文件（1 global.scss + 1 App.vue + 10 Vue 组件），无 schema 改动。
