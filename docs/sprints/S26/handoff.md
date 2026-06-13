# Sprint S26 — Handoff

> **承接**: S25（暗色 chip + 配色对齐）+ S24（USAGE chip + Browse 顶部流）
> **范围**: E 单独 — Playwright 自动化 + e2e 冒烟（0.5 sprint）
> **不开**: F+G / B / H（均推 S27+）

---

## 1. Sprint 目标

为 v1 上线交付一套**轻量、可重复、CI-ready**（v1 留接口）的端到端冒烟用例，覆盖首页 / Browse / Detail / 筛选 / 暗色模式五大关键路径，并产出 5 张可归档的视觉证据。

## 2. 背景与决策上下文

- S25 完成后前端已有浅色 / 暗色双主题、USAGE chip 配色规范
- 此前所有验收依赖人工冒烟 + 截图，回归成本随视图增多线性上升
- 决策：选 E（Playwright e2e）单做，限 0.5 sprint，不扩散到 CI 配置 / 跨浏览器

## 3. 范围与 Out of Scope

### 3.1 In Scope

- Playwright `@playwright/test@1.48`（pin）
- 5 条 e2e spec（home / browse / detail / usage-filter / dark-mode）
- 5 张截图归档（浅 3 + 暗 2）
- 4 个 Vue 文件加 `data-testid`（不删不改现有 class）
- `package.json` scripts：`test:e2e` + `test:e2e:ui`
- `.github/workflows/e2e.yml` 空壳 + 启用注释

### 3.2 Out of Scope（推 S27+ 或 v1.1）

| 项 | 推后 |
|----|------|
| F+G 限流运维化 | S27 |
| B 多对多 Skill ↔ Tag | S27+（schema 大改）|
| H LLM description 二次分类 | S27+（需 API key + 成本护栏）|
| 暗色全站化 | v1.1（仅 chip 已暗）|
| 跨浏览器 webkit/firefox | v1.1 |
| 前端组件单测 Vitest | v1.1 |
| CI 真实接入 | v1.1（GH Actions 空壳）|
| Performance / Load 测试 | v2+ |

## 4. 关键改动（落地清单）

### 4.1 文档

- `docs/sprints/S26/requirements.md` — 5 条 User Story + DoD + 风险
- `docs/sprints/S26/shot-list.md` — 5 张截图清单 + data-testid 命名约定
- `docs/sprints/S26/qa-runbook.md` — QA 执行手册 + 验收清单
- `docs/sprints/S26/screenshots/README.md` — 截图归档说明

### 4.2 前端：Selectors（data-testid 落点）

| 文件 | 新增 data-testid |
|------|------------------|
| `frontend/src/components/SkillCard.vue` | `skill-card` / `skill-usage-chip` / `skill-soc-chip` |
| `frontend/src/views/BrowseView.vue` | `usage-filter` / `usage-filter-{code}` / `skill-grid` |
| `frontend/src/views/SkillDetailView.vue` | `skill-detail` / `skill-usage-block` |
| `frontend/src/components/home/HomeHero.vue` | `home-hero` / `home-tab-human` / `home-tab-agent` |

**约束遵守**：未删未改现有 class，纯追加属性。

### 4.3 前端：Playwright 配置

- `frontend/playwright.config.ts`
  - `testDir: ./e2e`
  - `baseURL: http://127.0.0.1:7777`
  - `use: { screenshot: 'only-on-failure', trace: 'retain-on-failure' }`
  - `webServer: { command: 'npm run dev', reuseExistingServer: true, timeout: 60000 }`
  - `reporter: [['list'], ['html', { open: 'never' }]]`
- `frontend/package.json` 新增 scripts：`test:e2e` + `test:e2e:ui`
- 依赖：`@playwright/test@1.48`（pin）

### 4.4 前端：5 条 e2e spec

| 文件 | 覆盖路径 | 关键断言 |
|------|----------|----------|
| `e2e/01-home.spec.ts` | `/` | home-hero 可见 + tab 切换 + 截图 |
| `e2e/02-browse.spec.ts` | `/browse-skills` | skill-grid ≥ 5 卡 + 每卡含 usage chip + 截图 |
| `e2e/03-detail.spec.ts` | 首页 → /skills/{slug} | 跳转 + skill-detail + usage-block + 截图 |
| `e2e/04-usage-filter.spec.ts` | /browse-skills | 点击 tool chip 后列表 ≤ 全量 + active class |
| `e2e/05-dark-mode.spec.ts` | `/` + `/skills/{slug}` | `colorScheme: 'dark'` + 2 张截图 |

### 4.5 CI 空壳

`.github/workflows/e2e.yml`：
- 触发器注释：`workflow_dispatch`（手动可选）
- 全 workflow 体已注释
- 注释说明启用条件 + v1.1 启用步骤

## 5. 测试用例覆盖矩阵

| 视图 | 浅色 | 暗色 | 筛选 | 跳转 |
|------|------|------|------|------|
| `/` | ✅ 01 | ✅ 05 | — | ✅ 03 |
| `/browse-skills` | ✅ 02 | — | ✅ 04 | — |
| `/skills/{slug}` | ✅ 03 | ✅ 05 | — | — |

## 6. 关键决策与理由

| 决策 | 理由 |
|------|------|
| pin `@playwright/test@1.48` | 1.49 改 `browserType` API，避免 breaking |
| 仅装 chromium | 节约 CI 时间，webkit/firefox 留 v1.1 |
| `webServer.reuseExistingServer: true` | 避免 dev server 冷启动 60s 超时 |
| `data-testid` 不删不改 class | 任务书硬约束，最小侵入 |
| `04-usage-filter` 用 `test.skip` 兜底 | 不同 seed 可能缺 tool chip，避免 flaky |
| 截图路径 `../docs/sprints/S26/...` 相对 e2e/ | 让 spec 不依赖 cwd |
| GH Actions 仅留空壳 | 任务书 §4 Out of Scope 明示 v1 不接 CI |

## 7. 验证方法

由 QA 按 `qa-runbook.md` 执行：

1. `cd frontend && npm install -D @playwright/test@1.48`
2. `npx playwright install chromium`
3. 启动后端 8767 + 前端 7777
4. `npm run test:e2e`（期望 5/5 PASS，可能 1 skipped for 04）
5. 验证 `docs/sprints/S26/screenshots/` 含 5 张 PNG
6. `npm run build` 期望 0 错

## 8. 已知限制与风险

| 风险 | 缓解 |
|------|------|
| Vite dev 冷启动 > 60s | `reuseExistingServer: true` + `timeout: 60000` |
| H2 seed 数据偶发缺失 | 不依赖特定 skill 名，只断言 `≥5` 卡 |
| 截图差异 flaky | 浅色截首页/browse/detail 三张已覆盖关键视图 |
| 后端 8767 未起 | `webServer` 仅托管 Vite dev，后端假设已起（沿用 S25）|
| 1.49 升级 breaking | pin 1.48，升级需先验 browserType API |

## 9. Sprint Review 总结

### 9.1 完成

- ✅ 4 个 Vue 文件含 data-testid（无侵入）
- ✅ 5 条 e2e spec 编写完毕
- ✅ 截图归档目录就绪 + README 说明
- ✅ CI 空壳 + 启用注释
- ✅ QA runbook 可一键执行
- ✅ 文档三件套（requirements / shot-list / qa-runbook / handoff）

### 9.2 数据

- 改动文件数：~10（4 Vue + 1 config + 5 spec + 1 GH Actions + package.json）
- 新增依赖：`@playwright/test@1.48`（devDep）
- 截图数：5（浅 3 + 暗 2）
- e2e 数：5（严格 5，不多不少）
- 文档数：5（requirements / shot-list / qa-runbook / screenshots README / handoff）

### 9.3 价值

- v1 上线前最后一公里：回归从「人工手测」降到「`npm run test:e2e`」
- 暗色模式首次被自动化覆盖（S25 仅人工截图）
- USAGE chip 交互首次被覆盖（S24 仅有功能，无回归）

## 10. S27+ 候选（按优先级）

### 10.1 P0 — S27 强候选

1. **F+G 限流运维化**
   - 内容：后端 RateLimiter + 监控埋点（Prometheus）
   - 价值：保护 v1 上线后流量激增
   - 估时：1 sprint

2. **暗色全站化**
   - 内容：从 chip-only 扩展到整站（背景 / 文本 / 卡片 / 表格）
   - 价值：完成 S25 半截工作
   - 估时：0.5 sprint

### 10.2 P1 — S28+ 候选

3. **B 多对多 Skill ↔ Tag**
   - 内容：schema 大改（skill_tag 中间表 + 重写 mapper/service）
   - 风险：影响 S24 设计的 USAGE chip 数据流
   - 估时：1.5 sprint

4. **H LLM description 二次分类**
   - 内容：用 LLM 对未命中启发式的 skill 做二次归类
   - 前置：API key + 成本护栏（每 skill $0.001 上限？）
   - 估时：1 sprint

### 10.3 P2 — v1.1 候选

5. **跨浏览器 e2e**（webkit / firefox）
6. **前端组件单测 Vitest**
7. **CI 真实接入**（GH Actions 全启用）
8. **Performance budget 自动化**

## 11. 验收（Definition of Done — Final）

- [x] `docs/sprints/S26/requirements.md` 存在
- [x] `docs/sprints/S26/shot-list.md` 存在
- [x] `docs/sprints/S26/handoff.md` 13 章节齐全（本文件）
- [x] `docs/sprints/S26/qa-runbook.md` 存在
- [x] `docs/sprints/S26/screenshots/README.md` 存在
- [x] 4 个 Vue 文件含 data-testid
- [x] `playwright.config.ts` 配置完毕
- [x] 5 个 e2e spec 编写完毕
- [x] `package.json` 含 `test:e2e` + `test:e2e:ui`
- [x] `.github/workflows/e2e.yml` 空壳存在 + 注释
- [ ] `npm run build` 0 错 ← **待 QA 验证**
- [ ] `npm run test:e2e` 5/5 PASS ← **待 QA 验证**
- [ ] 5 张截图归档 ← **待 QA 跑测生成**

---

## 12. 给 S27 Lead 的建议

1. **必读**：本 handoff §10 的候选清单
2. **限流运维（F+G）** 是 S27 首选——v1 上线流量风险已存在
3. **暗色全站化** 可与限流并行（无代码冲突）
4. **不要在 S27 做 B（多对多）**：schema 大改风险高，留 S28+
5. **H（LLM 分类）** 需先确定 API key 预算与成本护栏，再开 sprint

## 13. 致谢与下一步

S26 是 v1 上线前的**收尾冲刺**，交付物清晰、范围严格、文档齐全。

**下一步**：由 QA 按 `qa-runbook.md` 执行验收 → 通过后提交 PR → S27 启动。