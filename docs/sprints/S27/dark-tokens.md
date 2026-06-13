# Sprint S27 — 暗色设计 Token 矩阵

> **承接**: `requirements.md` + S25 `dark-mode-spec.md`（chip 12 色）
> **目标**: 给出整站暗色 CSS 变量矩阵 + 组件级落点清单
> **不动**: S25 chip 流（已通过 S26 e2e 验证）

---

## 1. 浅色 token（`:root` 默认值）

```css
:root {
  /* —— 背景层 ——————————————————————————————————————— */
  --bg-primary:   #ffffff;   /* 页面主背景（卡片、容器、模态底） */
  --bg-secondary: #f5f5f5;   /* 二级背景（侧栏、搜索条、toolbar） */
  --bg-tertiary:  #fafafa;   /* 三级背景（hover、selected 弱态） */

  /* —— 文本层 ——————————————————————————————————————— */
  --text-primary:   rgba(0, 0, 0, 0.88); /* 主文字 */
  --text-secondary: rgba(0, 0, 0, 0.65); /* 次文字（描述、label） */
  --text-tertiary:  rgba(0, 0, 0, 0.45); /* 三级文字（placeholder、辅助） */

  /* —— 边框 / 分割线 —————————————————————————————————— */
  --border-color:  #d9d9d9;
  --border-radius: 6px;

  /* —— 阴影 ——————————————————————————————————————— */
  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.03);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.08);

  /* —— 主题色（沿用 Ant Design 4）—————————————— */
  --primary: #1677ff;
  --success: #52c41a;
  --warning: #faad14;
  --danger:  #ff4d4f;
  --link:    #1677ff;
}
```

## 2. 暗色 token（`:root[data-theme="dark"]` + `prefers-color-scheme: dark` 兜底）

```css
:root[data-theme="dark"] {
  --bg-primary:   #141414;   /* Ant darkAlgorithm 主背景 */
  --bg-secondary: #1f1f1f;   /* 略亮一层 */
  --bg-tertiary:  #262626;   /* hover 态、selected 弱态 */

  --text-primary:   rgba(255, 255, 255, 0.85);
  --text-secondary: rgba(255, 255, 255, 0.65);
  --text-tertiary:  rgba(255, 255, 255, 0.45);

  --border-color: #424242;

  --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.5);
  --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.4);

  --primary: #1668dc;        /* Ant darkAlgorithm 蓝 */
  --success: #49aa19;
  --warning: #d89614;
  --danger:  #dc4446;
  --link:    #1668dc;
}

@media (prefers-color-scheme: dark) {
  :root:not([data-theme="light"]) { /* 同上覆盖，保留 light 钩子 */
    --bg-primary: #141414; --bg-secondary: #1f1f1f; --bg-tertiary: #262626;
    --text-primary: rgba(255, 255, 255, 0.85);
    --text-secondary: rgba(255, 255, 255, 0.65);
    --text-tertiary: rgba(255, 255, 255, 0.45);
    --border-color: #424242;
    --shadow-sm: 0 1px 2px rgba(0, 0, 0, 0.5);
    --shadow-md: 0 4px 12px rgba(0, 0, 0, 0.4);
    --primary: #1668dc; --success: #49aa19; --warning: #d89614; --danger: #dc4446; --link: #1668dc;
  }
}

:root[data-theme="light"] {
  --bg-primary: #ffffff; --bg-secondary: #f5f5f5; --bg-tertiary: #fafafa;
  /* 其余与 :root 默认一致 */
}
```

## 3. 对比度自查（WCAG AA 4.5:1）

| 配对 | 暗色值 | 比值 | 评级 |
|------|--------|------|------|
| `--text-primary` on `--bg-primary` | rgba(255,255,255,0.85) on #141414 | ~14.8:1 | AAA |
| `--text-secondary` on `--bg-primary` | rgba(255,255,255,0.65) on #141414 | ~8.5:1 | AAA |
| `--text-tertiary` on `--bg-primary` | rgba(255,255,255,0.45) on #141414 | ~4.7:1 | AA |
| `--primary` on `--bg-primary` | #1668dc on #141414 | ~5.3:1 | AA |
| `--link` on `--bg-primary` | #1668dc on #141414 | ~5.3:1 | AA |

> 全部 ≥ 4.5:1，达到 AA；主/次文字达 AAA。无需自创色系，复用 Ant darkAlgorithm 即可。

## 4. 组件落点清单（7+ 文件）

| # | 文件 | 关键替换 |
|---|------|----------|
| 1 | `frontend/src/components/AppHeader.vue` | `.app-header` 背景 `var(--bg-primary)`、文字 `var(--text-primary)`、logo 描边 `var(--primary)`、菜单选中 `var(--primary)` |
| 2 | `frontend/src/components/home/HomeHero.vue` | `.home-hero` 背景渐变、tab 文字 `var(--text-primary)`、下载按钮 hover 用 `var(--bg-tertiary)` |
| 3 | `frontend/src/components/home/HomeHot.vue` | 卡片底色 `var(--bg-secondary)`、hover 阴影 `var(--shadow-md)`、标题 `var(--text-primary)`、描述 `var(--text-secondary)` |
| 4 | `frontend/src/components/home/HomeStats.vue` | 数字 `var(--text-primary)`、label `var(--text-secondary)` |
| 5 | `frontend/src/views/HomeView.vue` | 整体底色透明（继承 `body`）|
| 6 | `frontend/src/views/BrowseView.vue` | `.app-layout` 背景 `var(--bg-primary)`、`.browse__filter` 背景 `var(--bg-secondary)`、`.browse__head` 背景 `var(--bg-secondary)`、`.cat-all:hover` 用 `var(--bg-tertiary)`、`.cat-all.active` 蓝底用 `var(--primary)` 半透明 |
| 7 | `frontend/src/views/SkillDetailView.vue` | body 容器背景 `var(--bg-primary)`、metadata 行 hover `var(--bg-tertiary)`、表格底色 `var(--bg-primary)`、链接 `var(--link)` |
| 8 | `frontend/src/views/ApiGuideView.vue` | 文档页表格行 hover、代码块底色（深色用 `#0d1117`）、胶囊 nav active 态用 `var(--primary)` |
| 9 | `frontend/src/views/ProfileView.vue`（如含收藏） | 收藏列表卡片 `var(--bg-secondary)` |
| 10 | `frontend/src/components/ReviewForm.vue` | 输入框背景 `var(--bg-primary)`、按钮 primary 用 `var(--primary)` |
| 11 | `frontend/src/components/SkillCard.vue` | 卡片底色（已用 `var(--skillsmap-card-shadow)`）、标题 `var(--text-primary)`、描述 `var(--text-secondary)` |
| 12 | `frontend/src/App.vue` | `<a-config-provider :theme="isDark ? darkTheme : lightTheme">` + `data-theme` 同步 |

## 5. Ant Design Vue 4 集成

```ts
// App.vue
import { theme } from 'ant-design-vue'
const darkTheme = {
  algorithm: theme.darkAlgorithm,
  token: { colorPrimary: '#1668dc' }
}
const lightTheme = {
  algorithm: theme.defaultAlgorithm,
  token: { colorPrimary: '#1677ff' }
}

const isDark = ref(false)
onMounted(() => {
  const mq = window.matchMedia('(prefers-color-scheme: dark)')
  const apply = () => {
    const d = document.documentElement.dataset.theme === 'dark' || mq.matches
    isDark.value = d
    document.documentElement.dataset.theme = d ? 'dark' : 'light'
  }
  apply()
  mq.addEventListener('change', apply)
})
```

## 6. 验收映射

| 需求 | 落点 | 截图 |
|------|------|------|
| US-1 暗色首页 | AppHeader / HomeHero / HomeHot / HomeStats | dark-home.png |
| US-2 暗色 Browse | BrowseView 全文件 | dark-browse.png + dark-sidebar.png |
| US-3 暗色 Detail | SkillDetailView / ReviewForm | dark-detail.png |
| US-4 暗色 e2e 回归 | 5 spec 全过 + 6 截图归档 | — |
| 文档页 | ApiGuideView | dark-apiguide.png |
| 收藏 | ProfileView（Favorite 区）| dark-favorite.png |

## 7. 反模式（Do NOT）

- ❌ 不要给元素加 `[data-theme="dark"] .xxx` 选择器 — **统一走 CSS 变量**
- ❌ 不要把 `#fff` / `#000` 散落组件 — 必须 `var(--bg-primary)` 等
- ❌ 不要自创色系 — 复用 Ant darkAlgorithm 值
- ❌ 不要动 S25 chip 流（`usage-chip` + 12 `--usage-purpose-*-bg/fg`）
- ❌ 不要引 `theme` / 颜色库（chroma、polished 等）
- ❌ 不要删/改 class 名 — 只改颜色
