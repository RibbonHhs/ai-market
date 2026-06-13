# S02 Design · 01 — 上传 Tabs

> 作者：designer-vicky @ 2026-06-07
> 依据：`ui-ux-pro-max` 规则 + Ant Design Vue 4 既有约定
> 上游：[`../prd-git-source.md`](../prd-git-source.md) §4 US-01

## 1. 组件树

```
<SkillUploader>                                 — 扩展现有组件
├── <a-tabs v-model:active-key="mode">
│   ├── <a-tab-pane key="md"   tab="本地 .md">     ← 不动
│   ├── <a-tab-pane key="zip"  tab=".skill 包">    ← 不动
│   └── <a-tab-pane key="git"  tab="Git URL">      ★ 新增
│       ├── <a-form layout="vertical">
│       │   ├── a-form-item label="仓库 URL"  required
│       │   │   ├── a-input v-model="form.url"
│       │   │   │   placeholder="https://github.com/user/repo(.git)"
│       │   │   └── helper text: 支持 GitHub / GitLab / Gitea; 可含 .git
│       │   ├── a-row
│       │   │   ├── a-col span=12
│       │   │   │   └── a-form-item label="Branch / Tag"  optional
│       │   │   │       └── a-input v-model="form.ref"  placeholder="main"
│       │   │   └── a-col span=12
│       │   │       └── a-form-item label="子目录"   optional
│       │   │           └── a-input v-model="form.subdir"  placeholder="(根目录)"
│       │   ├── a-row
│       │   │   ├── a-col span=12
│       │   │   │   └── a-form-item label="Username"   optional
│       │   │   │       └── a-input v-model="form.username"
│       │   │   └── a-col span=12
│       │   │       └── a-form-item label="Access Token"  optional
│       │   │           ├── a-input-password
│       │   │           │   v-model="form.token"
│       │   │           │   visibilityToggle (eye icon)
│       │   │           └── helper text: ⚠️ 仅本次保存,后端 Jasypt 加密入库
│       │   ├── <a-checkbox v-model="form.insecure">
│       │   │       跳过 TLS 证书校验（自建 Gitea 用）
│       │   └── <a-button type="primary" @click="submitGit"
│       │           :loading="loading" size="large"
│       │           :disabled="!form.url">克隆并解析</a-button>
│       │
│       └── 解析结果预览（clone 完成后）
│           ├── <a-alert type="success" :message="`发现 ${n} 个 Skill`" />
│           ├── <a-list :data-source="discovered">
│           │   └── list-item: 名称 · 路径 · description · 状态 tag
│           └── <a-button>应用到表单</a-button> (自动跳转编辑页)
```

## 2. 线框

### 默认态（Git URL Tab）

```
┌──────────────────────────────────────────────────────────┐
│ 从文件 / Git 导入 Skill                                   │
├──────────────────────────────────────────────────────────┤
│  [ 本地 .md ]  [ .skill 包 ]  [ Git URL ]  ← 选中第 3    │
├──────────────────────────────────────────────────────────┤
│                                                          │
│  仓库 URL *                                               │
│  ┌────────────────────────────────────────────────────┐ │
│  │ https://github.com/anthropics/skills              │ │
│  └────────────────────────────────────────────────────┘ │
│  支持 GitHub / GitLab / Gitea · URL 末尾 .git 可选        │
│                                                          │
│  Branch / Tag            子目录（可选）                   │
│  ┌─────────────────┐    ┌──────────────────────────┐    │
│  │ main            │    │ （仓库根目录）            │    │
│  └─────────────────┘    └──────────────────────────┘    │
│                                                          │
│  Username               Access Token                     │
│  ┌─────────────────┐    ┌──────────────────────────┐    │
│  │                 │    │ ●●●●●●●●●●●●●●      👁  │    │
│  └─────────────────┘    └──────────────────────────┘    │
│  ⚠️ 仅本次保存,后端 Jasypt 加密入库                         │
│                                                          │
│  ☐ 跳过 TLS 证书校验（自建 Gitea / GitLab 用）             │
│                                                          │
│              [  克隆并解析  ]   (size=large)              │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

### 加载态

```
... 表单 ...
[ ⏳ 正在克隆 30% ]   ← size=large primary button 显示 loading
```

> 加载超 300ms 时按钮内嵌 spinner（Ant Design `loading` slot 默认行为）

### 错误态

```
[ 克隆并解析 失败 ]
─────────────────────────────────────────
  ❌ 鉴权失败: 请检查 Username / Access Token
─────────────────────────────────────────
  ↑ a-alert type="error" show-icon closable
  错误显示在表单下方,关联到 URL 字段（focus 自动回退到 url）
```

### 成功态 — Monorepo 拆分预览

```
✓ 已发现 3 个 Skill  ─────────────────────────
┌──────────────────────────────────────────┐
│  📦 web-research            (根目录/SKILL.md)  │  ✓ 已导入
│  📦 pdf-reader              (pdf-reader/)     │  ✓ 已导入
│  📦 image-ocr               (tools/ocr/)      │  ⚠ SKILL.md 缺 name 字段,已跳过
└──────────────────────────────────────────┘
                 [ 关闭并查看列表 ]
```

## 3. 交互规范

| # | 触发 | 反馈 | 规则来源 |
|---|------|------|----------|
| I-1 | URL 输入框 onBlur | 若格式不合法（无 scheme / 无 host）→ 输入框红框 + 下方红色 error：「URL 必须以 http:// 或 https:// 开头」 | `inline-validation` (on blur) + `error-placement` |
| I-2 | 提交按钮 click | 立即 disable + 显示 spinner（loading=true），loading 期间其它表单字段 disabled | `loading-buttons` + `disabled-states` |
| I-3 | 后端 50302 鉴权失败 | `a-alert type="error"` + focus 回 url 字段 + **不**显示后端原始 message 全文（避免泄露用户名格式） | `error-clarity` + R-01 |
| I-4 | Clone 成功（>0 skill） | `a-alert type="success"` + 列表 + 「应用到表单」按钮 | `success-feedback` |
| I-5 | Clone 成功（0 skill） | `a-alert type="warning"` + 提示「仓库内未发现符合 Agent Skills 规范的子目录,请确认根目录含 SKILL.md」 | `empty-states` |
| I-6 | 用户关闭含未提交表单的 Tab | Ant Design `Tabs` 已有 confirm 行为（如 dirty）— 我们额外用 `beforeLeave` 拦截 → 弹 `a-modal` 二次确认 | `sheet-dismiss-confirm` |
| I-7 | 鼠标悬停 token 字段 | helper text 显示：`⚠️ 仅本次保存,后端 Jasypt 加密入库,不会回显到前端` | `input-helper-text` |
| I-8 | 「跳过 TLS」勾选 | 右侧立刻出现黄色 `a-alert type="warning"`：「⚠️ 仅用于自建 Gitea / GitLab 自签证书,公网仓库请勿勾选」 | `progressive-disclosure` + `destructive-emphasis` |
| I-9 | Token 字段失焦 | 若为空但 URL 是 github.com → 显示 `a-alert type="info"`：「公开仓库无需 token」 | `input-helper-text` |

## 4. 校验规则

| 字段 | 规则 | 错误文案 |
|------|------|----------|
| url | `^https?://[^\\s]+$` | 「URL 必须以 http:// 或 https:// 开头」 |
| url | 长度 ≤ 500 | 「URL 过长（> 500 字符）」 |
| ref | 若填则 ≤ 200 字符 | 「Branch / Tag 太长」 |
| subdir | 若填则不能以 `/` 开头、不能含 `..` | 「子目录路径非法」 |
| username | 若填则 ≤ 100 字符 | — |
| token | 若填则 ≤ 500 字符 | — |

## 5. 状态（State）覆盖

| 状态 | 行为 | Ant Design 组件态 |
|------|------|-------------------|
| Default | 全部字段 enabled、按钮 enabled、spinner 隐藏 | `:disabled="false"` `:loading="false"` |
| Loading | URL 字段 disabled、按钮 loading=true 不可重复点击 | `:disabled="true"` `:loading="true"` |
| Success | 切换为结果预览视图，原始表单 hide（v-if） | `v-if="!result"` |
| Error | 保留原表单（不清空）、错误 alert 显示在按钮上方 | `<a-alert type="error">` |
| Empty (0 skill) | 警告 alert + 按钮文案变「重新克隆」 | `type="warning"` |

## 6. 无障碍（A11y）走查

| 项 | 状态 | 说明 |
|----|------|------|
| 所有可点击元素 ≥ 44×44 px | ✓ | `<a-button size="large">` 默认 40px；URL 输入框 height 32px；加 `style="height:44px"` 补足 |
| Focus ring 可见 | ✓ | Ant Design 默认 `outline: 5px auto -webkit-focus-ring-color` |
| 颜色 + 文字双指示 | ✓ | 徽章同时有 emoji + 文字（GIT_URL: 🔗 Git @ main） |
| 表单 label 永久可见 | ✓ | `a-form-item label` 始终显示（不用 placeholder 替代） |
| Error 用 `aria-live="polite"` | ✓ | `a-alert` 默认带 `role="alert"` |
| 键盘 Tab 顺序 = 视觉顺序 | ✓ | url → branch → subdir → username → token → insecure → 提交 |
| Enter 键提交 | ✓ | `a-form @finish` 处理；loading 时 Enter 无效 |
| `prefers-reduced-motion` | ✓ | Ant Design Modal/Tabs 已自动尊重 |

## 7. 反模式（已规避）

| 反模式 | 规避方式 |
|--------|----------|
| 错误只在顶部 | 错误 alert 紧贴提交按钮上方 |
| 二次确认用浏览器 confirm() | 用 `a-modal` 自定义，匹配项目风格 |
| 密码字段无切换可见 | `a-input-password` 带 visibility toggle |
| Loading 期间无反馈 | 按钮立刻 disable + spinner（< 100ms） |
| Color-only 状态指示 | GIT_URL 徽章用「🔗 + 文字」组合 |

## 8. 交付物清单

- [ ] `frontend/src/components/SkillUploader.vue`（改造，加第 3 个 Tab）
- [ ] `frontend/src/api/admin.ts` 加 2 个方法（fromGit / sync）
- [ ] `frontend/src/types/skill.ts` 加 `SourceType` 枚举
- [ ] 新建 `frontend/src/components/GitUrlForm.vue`（可选拆组件；若不拆则 SkillUploader 内含）

## 9. 待 dev-kevin 同步的 Props / Emits

```ts
// SkillUploader.vue 扩展 props
defineProps<{
  // 不变
  parsedData?: any
  // 新增
  gitImportResult?: { url: string; ref?: string; discovered: Skill[] }
}>()

defineEmits<{
  // 不变
  (e: 'apply', data: any): void
  (e: 'update:parsedData', data: any): void
  // 新增
  (e: 'git-imported', data: GitImportResult): void
}>()
```
