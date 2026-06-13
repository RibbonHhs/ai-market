# S24 截图清单（手测 / Playwright 后置）

> **状态**：本机未装 Playwright / Puppeteer，截图需手动浏览器验证
> **前端 dev server**：http://127.0.0.1:7777（已启动）
> **后端**：http://127.0.0.1:8767（H2 dev 模式，26 skills 已 seed）

---

## 截图 1：BrowseSkills — 顶部 USAGE 横向 chip 筛选条

**操作**：
1. 打开 http://127.0.0.1:7777/browse
2. 默认 dim=SOC 时，顶部 chip 仍可见（独立于左侧树）
3. 点击"开发"chip → 列表筛到 20 个 DEV 类 skill
4. 再次点击"开发"chip → 取消筛选，列表恢复 26 个

**预期视觉**：

```
┌────────────────────────────────────────────────────────────────┐
│ AppHeader                                                       │
├────────────────────────────────────────────────────────────────┤
│ ┌──────────┬─────────────────────────────────────────────────────┐│
│ │ 左侧     │ 用途：[📦 全部] [🛠 工具] [💼 商业] [💻 开发*]...   ││  ← 新顶部
│ │ 240px    │       (选中态: 实色 fg 反色)                       ││
│ │ 维度     │ ───────────────────────────────────────────────────││
│ │ (•)SOC   │ [🔍 搜索框              ]  [Sort v]                ││
│ │ ( )USAGE │ ───────────────────────────────────────────────────││
│ │ 分类树   │ [Card] [Card] [Card] [Card] [Card] [Card]         ││
│ │ ...      │ [Card] [Card] [Card] [Card] [Card] [Card]         ││
│ │ 来源     │ [Card] [Card] [Card] [Card] [Card] [Card]         ││
│ └──────────┴─────────────────────────────────────────────────────┘│
└────────────────────────────────────────────────────────────────┘
```

**12 个 chip 顺序**（来自 `USAGE_TOP_ORDER`）：
全部 → 🛠 工具 → 💼 商业 → 💻 开发 → 🧪 测试与安全 → 🤖 数据与AI → 🚀 DevOps → 📚 文档 → 🎨 内容与媒体 → 🔬 研究 → 🌱 生活方式 → 💾 数据库 → ⛓ 区块链

**截图保存路径**：`docs/sprints/S24/screenshot-1-browse-usage-filter.png`

---

## 截图 2：SkillCard — 分类 chip 展示

**操作**：
1. http://127.0.0.1:7777/browse
2. 滚到列表中部
3. 选中任一 skill 卡片

**预期视觉**（2 个分类 chip 在 tags 上方）：

```
┌─────────────────────────────────────────────────┐
│ [LOGO] Web Design Engineer              [★ 精选]│
│        👤 SkillsMap Team · v1.2.1                │
│ ── 来源 ──                                       │
│ description...                                   │
│                                                 │
│ [职业: 艺术与设计工作者]  [💻 用途: 开发·前端开发]│  ← 2 chip
│ #design #engineer #web                          │
│ ──────────────────────────────────────────────  │
│ ⭐ 5.0  ⬇ 1.2k                                  │
└─────────────────────────────────────────────────┘
```

**配色**（USAGE 12 色，对应 parentCode）：
- 工具 → 蓝
- 商业 → 橙
- 开发 → 青  ← `web-design-engineer` 用此色
- 测试与安全 → 紫
- 数据与AI → 品红
- DevOps → 朱
- 文档 → 嫩绿
- 内容与媒体 → 钴蓝
- 研究 → 草绿
- 生活方式 → 胭脂
- 数据库 → 琥珀
- 区块链 → 黄金

**截图保存路径**：`docs/sprints/S24/screenshot-2-skillcard-chips.png`

---

## 截图 3：SkillDetail — 用途区块

**操作**：
1. http://127.0.0.1:7777/browse
2. 点击 "Web Design Engineer" 卡片
3. 滚动到 tags 后、详细介绍前

**预期视觉**：

```
┌──────────────────────────────────────────────────────┐
│ 顶部 header（logo + 名称 + 描述 + 按钮）               │
└──────────────────────────────────────────────────────┘

[tag1 tag2 tag3]   ← 旧区块

┌──────────────────────────────────────────────────────┐
│ 🎯 用途                                              │  ← 新区块
│                                                      │
│  ┌──────────────────────────────────────┐            │
│  │ 💻  开发  →  前端开发               │            │  ← 父+子 chip
│  └──────────────────────────────────────┘            │
│  💡 描述：...                                         │  ← 父类目 description
└──────────────────────────────────────────────────────┘

[📖 详细介绍]
[💬 用户评价]
```

**截图保存路径**：`docs/sprints/S24/screenshot-3-skilldetail-usage.png`

---

## Playwright 自动截图（v1.1）

若需 CI 自动化，建议装 `@playwright/test`：

```bash
cd frontend
npm install -D @playwright/test
npx playwright install chromium
```

```typescript
// e2e/s24-usage.spec.ts
import { test, expect } from '@playwright/test'

test('S24 截图：browse + card + detail', async ({ page }) => {
  await page.goto('http://127.0.0.1:7777/browse')
  await page.screenshot({ path: 'docs/sprints/S24/screenshot-1-browse.png', fullPage: true })
  await page.locator('.skill-card').first().screenshot({ path: 'docs/sprints/S24/screenshot-2-skillcard.png' })
  await page.locator('.skill-card').first().click()
  await page.waitForURL(/\/skills\//)
  await page.locator('.detail__usage').screenshot({ path: 'docs/sprints/S24/screenshot-3-detail-usage.png' })
})
```

> 本 sprint 不强制（v1.1 + Playwright 安装较大）。
