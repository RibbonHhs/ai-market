# Sprint S26 — E2E 截图清单 + 选择器约定（Designer Hand-off）

> **配套文档**: `requirements.md`
> **承接**: S25 暗色配色规范 + S24 USAGE chip 规范
> **目的**: 5 张关键截图的"鼠标落点 + 期望视觉"清单 + data-testid 命名约定

---

## 1. 截图总览（5 张）

| # | 文件名 | 视图 | 主题 | 鼠标落点 | 期望视觉重点 |
|---|--------|------|------|----------|--------------|
| 1 | `home.png` | `/` 首页 | 浅色 | 默认 Tab（人类） | Hero + 卡片入口 |
| 2 | `browse.png` | `/browse-skills` | 浅色 | 默认无筛选 | 顶部 chip 流 + 卡片网格 |
| 3 | `detail.png` | `/skills/{id}` | 浅色 | 第一张卡片 | 标题 + USAGE 区块 |
| 4 | `dark-home.png` | `/` 首页 | 暗色 | 同 #1 | 暗色背景 + chip 配色 |
| 5 | `dark-detail.png` | `/skills/{id}` | 暗色 | 同 #3 | 暗色 USAGE 区块 |

**严格 5 张**：浅 3 + 暗 2，不多做。

---

## 2. data-testid 命名约定

### 2.1 通用规则

- **全小写 + 短横线**（kebab-case）
- **作用域前缀**：`<视图>-<区块>-<元素>` 三段式
- **枚举值**：用短横线连接枚举（`usage-filter-tool` 而非 `usage-filter-Tool`）
- **DOM-only**：纯 `data-testid` 属性，不影响样式、不影响 ARIA
- **不删不改现有 class**：仅追加属性（任务书硬约束）

### 2.2 落点清单（4 个 Vue 文件）

#### `frontend/src/components/SkillCard.vue`

| data-testid | 落点 | 备注 |
|-------------|------|------|
| `skill-card` | 卡片根 `<a>` 或最外层包裹 | 整张卡片可点击 |
| `skill-usage-chip` | USAGE 文字徽标 | 单卡唯一 |
| `skill-soc-chip` | "人类/智能体"二选一 chip | 单卡唯一 |

#### `frontend/src/views/BrowseView.vue`

| data-testid | 落点 | 备注 |
|-------------|------|------|
| `usage-filter` | 顶部 USAGE 筛选条容器 | 整个筛选流 |
| `usage-filter-{parentCode小写}` | 每个 chip（例：`usage-filter-tool` / `usage-filter-creative`） | 枚举来自 S24 parent code |
| `skill-grid` | SkillCard 网格容器 | 用于断言列表渲染 |

#### `frontend/src/views/SkillDetailView.vue`

| data-testid | 落点 | 备注 |
|-------------|------|------|
| `skill-detail` | 详情页根容器 | 整页可识别 |
| `skill-usage-block` | 用途区块 | 包含若干 chip |

#### `frontend/src/views/HomeHero.vue`

| data-testid | 落点 | 备注 |
|-------------|------|------|
| `home-hero` | Hero 根容器 | 首屏 |
| `home-tab-human` | Tab "人类" 按钮 | 默认激活 |
| `home-tab-agent` | Tab "智能体" 按钮 | 切换目标 |

---

## 3. 视觉验收标准（每张截图）

### 3.1 浅色 3 张

| 维度 | 标准 |
|------|------|
| 背景 | 浅灰 / 白色（沿用 S24 §6 配色） |
| 文本对比 | 主体 ≥ 4.5:1 |
| Chip 颜色 | 浅色 variant（来自 `usage-colors.ts`） |
| 卡片间距 | 统一 grid 间距（不重叠、不溢出） |
| 首屏完整性 | 无水平滚动条（375px / 1280px 均测） |

### 3.2 暗色 2 张

| 维度 | 标准 |
|------|------|
| 背景 | 深色（不纯黑，沿用 S25 暗色规范） |
| 文本对比 | 主体 ≥ 4.5:1（暗色单独验） |
| Chip 颜色 | 暗色 variant（饱和度降、亮度提） |
| 截图方法 | `page.emulateMedia({ colorScheme: 'dark' })` 后整页截图 |

---

## 4. Playwright 截图规范

### 4.1 截图函数

```ts
await page.screenshot({ path: 'docs/sprints/S26/screenshots/home.png', fullPage: true })
```

- 全部 `fullPage: true`（保留完整滚动视图）
- 仅在断言通过后截图（避免污染归档）
- 截图路径统一相对 `frontend/` 项目根

### 4.2 失败处理

- 失败用例走 `use.screenshot: 'only-on-failure'`（config 已配）
- 失败截图存 `test-results/`（不进 git，.gitignore 覆盖）

---

## 5. 5 张截图 → 5 条 e2e 的映射

| 截图 | 触发用例 | 关键断言 |
|------|----------|----------|
| `home.png` | `01-home.spec.ts` | `[data-testid="home-hero"]` 可见 |
| `browse.png` | `02-browse.spec.ts` | `[data-testid="skill-card"]` ≥ 5 |
| `detail.png` | `03-detail.spec.ts` | `[data-testid="skill-detail"]` 可见 |
| （无） | `04-usage-filter.spec.ts` | 不出图（功能性断言） |
| `dark-home.png` + `dark-detail.png` | `05-dark-mode.spec.ts` | `emulateMedia` 切换有效 |

---

## 6. 反模式（Do NOT）

- ❌ 不要用 `.first-child` / `.nth-of-type` 等结构选择器（脆弱）
- ❌ 不要依赖文案（中英文切换可能破坏）
- ❌ 不要在选择器里嵌随机 ID / 时间戳
- ❌ 不要用 `data-testid` 同时承担样式（污染语义）
- ❌ 不要给一个组件加多个同义 `data-testid`（保持 1:1）
- ❌ 不要截图前手动滚动（让 `fullPage: true` 处理）

---

## 7. 完成标准

- [ ] 4 个 Vue 文件按 §2.2 落点加 data-testid
- [ ] 5 张截图全部归档至 `docs/sprints/S26/screenshots/`
- [ ] 浅色 3 张通过 `01-03` spec 触发
- [ ] 暗色 2 张由 `05-dark-mode.spec.ts` 触发
- [ ] 截图无水平滚动条 / 无溢出 / 无错位
- [ ] 暗色 chip 配色沿用 S25 规范（不退化）