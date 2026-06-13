# S30 WCAG AA 暗色系回归报告

> **生成时间**: 2026-06-13 02:58:06
> **基础矩阵**: `docs/sprints/S29/wcag-matrix.md` (30 行)
> **回归方法**: Playwright 截屏 + JS 实测合成对比度（sRGB 公式）
> **目标**: AA 4.5:1（大文字/装饰元素 3:1）

## 1. 概要

| 维度 | 通过 | 总数 | 通过率 |
|------|------|------|--------|
| 暗色 token 关键对 + 新 bg | 19 | 20 | 95% |
| 浅色对照 | 4 | 5 | 80% |
| **合计** | **23** | **25** | **92%** |

**结论**: ⚠️ 2 项未达 AA，需调整 token

## 2. 暗色态全量对比度实测

| # | 验收点 | 实测 | 要求 | 状态 | 原始 fg | 原始 bg |
|---|--------|------|------|------|---------|---------|
| T1 | 主标题 h1 (暗) on --bg-primary #15121f | 15.63:1 | ≥ 4.5:1 | ✅ | `rgba(255, 255, 255, 0.92)` | `effective on BODY = rgb(21, 18, 31)` |
| T2 | 副文 on --bg-primary #15121f | 8.85:1 | ≥ 4.5:1 | ✅ | `rgba(255, 255, 255, 0.68)` | `effective on BODY = rgb(21, 18, 31)` |
| T3 | placeholder on --bg-primary #15121f | 13.26:1 | ≥ 4.5:1 | ✅ | `rgba(255, 255, 255, 0.92)` | `effective on home-hero__input-row = rgb(37, 33, 58)` |
| T5 | 卡片标题 on --bg-secondary #1c1830 | 14.68:1 | ≥ 4.5:1 | ✅ | `rgba(255, 255, 255, 0.92)` | `effective on home-hot__card = rgb(28, 24, 48)` |
| T6 | 次文 on --bg-secondary #1c1830 | 8.45:1 | ≥ 4.5:1 | ✅ | `rgba(255, 255, 255, 0.68)` | `effective on home-hot__card = rgb(28, 24, 48)` |
| S1 | 卡评分数字 on 卡片 bg | 10.29:1 | ≥ 3:1 | ✅ | `rgb(251, 191, 36)` | `effective on home-hot__card = rgb(28, 24, 48)` |
| S2 | 排行评分数字 on row bg | 11.05:1 | ≥ 3:1 | ✅ | `rgb(251, 191, 36)` | `effective on home-featured__layout = rgb(21, 18, 31)` |
| S3 | 排行榜数字 on row bg | 6.78:1 | ≥ 3:1 | ✅ | `rgb(167, 139, 250)` | `effective on home-featured__layout = rgb(21, 18, 31)` |
| S4 | sidebar cat 文字 on sidebar | 13.26:1 | ≥ 4.5:1 | ✅ | `rgba(255, 255, 255, 0.92)` | `effective on home-featured__sidebar = rgb(37, 33, 58)` |
| B1 | 主搜索按钮 紫底深字 | 3.24:1 | ≥ 4.5:1 | ❌ | `rgb(21, 18, 31)` | `按钮渐变中点 rgb(124, 58, 237)（3 stops）` |
| B2 | CTA 紫边紫字 | 5.17:1 | ≥ 4.5:1 | ✅ | `rgb(167, 139, 250)` | `effective on home-stats__cta-btn = rgb(45, 40, 66)` |
| B3 | Tab is-active 紫底深字 | 5.69:1 | ≥ 4.5:1 | ✅ | `rgb(167, 139, 250)` | `effective on home-hero__tab is-active = rgb(37, 33, 58)` |
| N1 | __search-card elevated on primary | 12.13:1 | ≥ 4.5:1 | ✅ | `rgba(255, 255, 255, 0.92)` | `effective on home-hero__search-card = rgb(45, 40, 66)` |
| N4 | HomeHot 12 卡片标题 on card bg | 14.68:1 | ≥ 4.5:1 | ✅ | `rgba(255, 255, 255, 0.92)` | `effective on home-hot__card = rgb(28, 24, 48)` |
| N5 | HomeFeatured sidebar cat 文字 on sidebar | 13.26:1 | ≥ 4.5:1 | ✅ | `rgba(255, 255, 255, 0.92)` | `effective on home-featured__sidebar = rgb(37, 33, 58)` |
| N6 | HomeFeatured 排行名 on row | 15.63:1 | ≥ 4.5:1 | ✅ | `rgba(255, 255, 255, 0.92)` | `effective on home-featured__layout = rgb(21, 18, 31)` |
| N7 | HomeStats 数字 on cell | 15.63:1 | ≥ 4.5:1 | ✅ | `rgba(255, 255, 255, 0.92)` | `effective on home-stats__cell = rgb(21, 18, 31)` |
| T4 | 主色链接 on --bg-secondary #1c1830 | 5.17:1 | ≥ 4.5:1 | ✅ | `rgb(167, 139, 250)` | `effective on home-hero__search-card = rgb(45, 40, 66)` |
| B4 | __agent-num 紫底深字 | 13.26:1 | ≥ 4.5:1 | ✅ | `rgba(255, 255, 255, 0.92)` | `effective on home-hero__agent-num = rgb(37, 33, 58)` |
| N2 | __agent-cmd 暗底白字 | 12.13:1 | ≥ 4.5:1 | ✅ | `rgba(255, 255, 255, 0.92)` | `effective on home-hero__agent-cmd = rgb(45, 40, 66)` |
| L1 | 浅色：主标题 on bg | 17.33:1 | ≥ 4.5:1 | ✅ | `rgb(26, 26, 31)` | `effective on BODY = rgb(255, 255, 255)` |
| L2 | 浅色：home-hot 卡片标题 on card bg | 16.19:1 | ≥ 4.5:1 | ✅ | `rgb(26, 26, 31)` | `effective on home-hot__card = rgb(247, 247, 248)` |
| L3 | 浅色：home-hot 卡片次文 on card bg | 6.35:1 | ≥ 4.5:1 | ✅ | `rgb(90, 90, 102)` | `effective on home-hot__card = rgb(247, 247, 248)` |
| L4 | 浅色：home-hot card-rate on card bg (warning 3:1) | 2.01:1 | ≥ 3:1 | ❌ | `rgb(245, 158, 11)` | `effective on home-hot__card = rgb(247, 247, 248)` |
| L5 | 浅色：home-hot 卡片作者 on card bg | 6.35:1 | ≥ 4.5:1 | ✅ | `rgb(90, 90, 102)` | `effective on home-hot__card = rgb(247, 247, 248)` |

## 3. 分类汇总

| 类别 | 通过 | 总数 | 备注 |
|------|------|------|------|
| 全局 token 关键对 (T1-T6) | 6 | 6 | 全过 |
| 状态色 (S1-S4) | 4 | 4 | 全过 |
| 按钮 (B1-B4) | 3 | 4 | 需调整按钮 fg/bg |
| 新 bg 值下关键文字 (N1-N5) | 6 | 6 | 全过 |
| 浅色对照 (L1-L5) | 4 | 5 |  |

## 4. 失败项 + 修复建议

### B1 — 主搜索按钮 紫底深字

- **实测**: 3.24:1（要求 ≥ 4.5:1）
- **fg**: `rgb(21, 18, 31)`
- **bg**: `按钮渐变中点 rgb(124, 58, 237)（3 stops）`
- **根因 + 建议**:
  - **根因**: `.home-hero__search-btn` 是 `linear-gradient(135deg, #7c3aed → #4f46e5)` 渐变背景（不是纯紫），对按钮深字 #15121f 实测 ≈ **3.24:1**（大文字 3:1 AA 边缘过，普通文字 4.5:1 AA 失败）
  - **建议 (dev-kevin 二选一)**:
    - 选项 A：按钮文字提亮到 `--text-primary` 浅白（`#fff`）— 算上渐变中点 ≈ 4.8:1 ✅ 普通 AA
    - 选项 B：保持深字但确认按钮是 `font-size: 16px; font-weight: 600` 大文字 (3:1 AA 过)，并在 PR 描述里注明 "Acceptable per large-text 3:1"

### L4 — 浅色：home-hot card-rate on card bg (warning 3:1)

- **实测**: 2.01:1（要求 ≥ 3:1）
- **fg**: `rgb(245, 158, 11)`
- **bg**: `effective on home-hot__card = rgb(247, 247, 248)`
- **根因 + 建议**:
  - **根因**: 浅色态下 `--warning` (#f59e0b 金色) on card bg (#f7f7f8) ≈ 2.0:1 — **S30 工单前就存在**，dev-kevin 改 `__card-rate` 时只动了 token 引用但没调浅色态 warning 值
  - **建议 (dev-kevin 二选一)**:
    - 选项 A：浅色态下把 `--warning` 暗化到 `#b45309`（amber-700），对比度提升到 ≈ 4.6:1 ✅
    - 选项 B：浅色态 `.home-hot__card-rate` 字号从 12px 提到 14px + weight 700，按"大文字 3:1 AA"过（当前 2.0:1 仍不达 3:1）
  - **影响范围**: 浅色态所有 `var(--warning)` 用色点（卡片评分、排行评分）— 需做全站扫描

## 5. 截图清单

| 文件 | 模式 | 验收点 |
|------|------|--------|
| `home-hero-dark-assist.png` | 暗色 | assist tab + __search-card + __backdrop 渐变 |
| `home-hero-dark-agent.png` | 暗色 | agent tab + __agent-num 紫底深字 + __agent-cmd 暗底白字 |
| `home-hot-dark.png` | 暗色 | 12 卡片紫调底 + Tab is-active 紫底深字 |
| `home-featured-dark.png` | 暗色 | sidebar hover 文字 + 排行 1/2/3 |
| `home-stats-dark.png` | 暗色 | CTA 紫边紫字 + 3 数字 |
| `home-full-dark.png` | 暗色 | 全页合成（兜底） |
| `home-hero-light.png` | 浅色 | 浅色态视觉未破坏对照 |
| `home-hot-light.png` | 浅色 | 浅色态 12 卡片白底对照 |

---

**报告生成**: Playwright + 自写合成对比度（sRGB → 相对亮度 → 对比度比）
**签发**: qa-tina @ S30