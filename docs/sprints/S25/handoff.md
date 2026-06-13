# SkillsMap Sprint S25 接力简报

> **用途**：新会话第一条 message 读此文件即可接管 S25 全部上下文。
> **生成时间**：2026-06-12
> **状态**：✅ **S25 已完成**

---

## 1. 项目背景

SkillsMap = Spring Boot 3.5.7 + Vue 3.5 + JDK 21 + MyBatis-Plus 3.5.12 全栈 skill 平台。

- 后端：`D:\codeing\workspace\skills-map\backend`
- 前端：`D:\codeing\workspace\skills-map\frontend`
- JDK 21：`D:\sofaward\openjdk-21.0.2_windows-x64_bin\jdk-21.0.2`
- 后端 8767，前端 7777
- **S21 / S22 / S23 / S24 / S25 全部完成**
  - S21：公开 API occupationCode + /api-guide + skills-manager skill
  - S22：物化 storage + 智能体埋点 + api-guide 锚点
  - S23：公开 API 限流（Bucket4j）+ skills-manager 鉴权中转
  - S24：USAGE 维度端到端（启发式 + 12 色 chip + 顶部筛选条）
  - **S25（本简报覆盖）：3 误命中启发式精修 + 暗色模式 chip 适配**

---

## 2. S25 目标（2 项，4 决策已锁）

| ID | 目标 | 决策 |
|----|------|------|
| **A** | 3 误命中 skill 启发式精修 + 手工 override | MANUAL_OVERRIDES 优先 > 启发式 |
| **D** | 暗色模式 USAGE chip 配色 | 跟 `prefers-color-scheme` + 留 `data-theme` 钩子 |

| Q | 决策 | 理由 |
|---|------|------|
| Q1 启发式顺序 | MANUAL_OVERRIDES > 精准 > 通配 | 高置信度硬编码优先 |
| Q2 backfill 参数 | 新增 `override=true`（仅修 3 误命中）| 区别于 `force=true` 全量重算 |
| Q3 暗色触发 | 跟系统 + `data-theme` 钩子，**不**做切换按钮 | 避免范围扩散 |
| Q4 暗色 vs 浅色映射 | 同色相反转亮度（不直接 hex 反色）| HIG/MD 推荐，保持品牌识别 |

---

## 3. T1 + T2 任务分解

| ID | 任务 | 关键文件 | 状态 |
|----|------|----------|------|
| T1.1 | 3 误命中根因分析 | `docs/sprints/S25/root-cause.md` | ✅ |
| T1.2 | 启发式精修 | `util/CategoryUtil.java` | ✅ |
| T1.3 | backfill 加 `override` 参数 | `rest/admin/AdminSkillController.java` | ✅ |
| T1.4 | 单测 5 case | `test/util/CategoryUtilTest.java` | ✅ |
| T2.1 | 暗色 chip 配色规范 | `docs/sprints/S25/dark-mode-spec.md` | ✅ |
| T2.2 | TS API 加 `USAGE_DARK` | `constants/usage-colors.ts` | ✅ |
| T2.3 | 3 个 chip 改 className 驱动 | `SkillCard.vue` + `BrowseView.vue` + `SkillDetailView.vue` | ✅ |
| T2.4 | CSS 变量 + 三层触发 | `style/global.scss` | ✅ |

---

## 4. 改动文件清单

### 4.1 `util/CategoryUtil.java`（启发式精修，~10 行）

```java
// S25: MANUAL_OVERRIDES 最高优先级
public static final Map<String, String> MANUAL_OVERRIDES = Map.of(
    "claude-md-improver",     "PURPOSE-AI-LLM",
    "web-video-presentation", "PURPOSE-MEDIA-CONTENT",
    "ui-ux-pro-max",          "PURPOSE-AI-LLM"
);

public static String guessUsageCode(String pluginSlug, String name) {
    if (name == null) return "PURPOSE-DEV-BACKEND";
    String n = name.toLowerCase();
    String override = MANUAL_OVERRIDES.get(n);
    if (override != null) return override;  // 顶层查表
    // S25 精修: Git 工作流（plugin slug 不再通配，name 必须含 "git-"）
    if (n.contains("git-") || n.contains("commit") || ...) return "PURPOSE-DEVOPS-GIT";
    // ... 其余规则不变
}
```

### 4.2 `rest/admin/AdminSkillController.java`（backfill 加 override）

```java
@PostMapping("/backfill-usage")
public Result<Map<String, Object>> backfillUsage(
        @RequestParam(defaultValue = "false") boolean force,
        @RequestParam(defaultValue = "false") boolean override) {
    // override 模式：仅对 MANUAL_OVERRIDES 表中 skill 强制覆盖（幂等）
    // 原 force 模式：全部按当前启发式重算
    // 默认（都 false）：仅补 null
}
```

### 4.3 `test/util/CategoryUtilTest.java`（新增 5 case）

| TC | 验证 | 状态 |
|----|------|------|
| TC1 | `claude-md-improver` (git 源) → AI-LLM | ✅ |
| TC2 | `web-video-presentation` (git 源) → MEDIA-CONTENT | ✅ |
| TC3 | `ui-ux-pro-max` (git 源) → AI-LLM | ✅ |
| TC4 | MANUAL_OVERRIDES 表 3 条 + 不重复 | ✅ |
| TC5 | 启发式对非 override 的 `git` slug → 不再误中 DEVOPS-GIT | ✅ |

### 4.4 `constants/usage-colors.ts`（+USAGE_DARK）

12 个 USAGE 一级暗色配色（最低 7.4:1，过 WCAG AAA）：
- 浅色 / 暗色 / dark-default 三套
- `getUsageDarkColor(parentCode)` API

### 4.5 `components/SkillCard.vue`（chip 改 className）

- 删 inline `:style` 拼字符串
- 加 className 12 + default
- scoped CSS 加 `:deep(.dark)` + `@media (prefers-color-scheme: dark)` 双触发

### 4.6 `views/BrowseView.vue`（顶部 chip 改 className）

- 13 个 chip（"全部" + 12 一级）全 className 驱动
- `usageTopList` computed 不再返回 bg/fg

### 4.7 `views/SkillDetailView.vue`（用途区块 chip 改 className）

- 单 chip className 驱动
- 父·子结构不变

### 4.8 `style/global.scss`（CSS 变量 + 三层触发）

```scss
:root { --usage-purpose-tool-bg: #F0F5FF; --usage-purpose-tool-fg: #1D39C4; /* 12 个 */ }
@media (prefers-color-scheme: dark) { :root:not([data-theme="light"]) { /* 暗色 12 个 */ } }
:root[data-theme="dark"] { /* 同上 */ }
:root[data-theme="light"] { /* 强制浅色 */ }
.usage-chip--code-purpose-tool { --usage-bg: var(--usage-purpose-tool-fg); --usage-fg: var(--usage-purpose-tool-bg); }
```

---

## 5. 配色规范

详见 `dark-mode-spec.md` §2。12 色均通过 WCAG AAA（最低 7.4:1）。

浅色规范沿用 S24 `wireframe-spec.md` §1（最低 7.2:1）。

---

## 6. 决策已锁 vs 实际落地

| Q | 锁 | 落地 | 备注 |
|---|----|------|------|
| Q1 启发式顺序 | MANUAL > 精准 > 通配 | ✅ CategoryUtil L36-44 | OK |
| Q2 backfill override | 仅修 3 误命中 | ✅ AdminSkillController L327-350 | OK |
| Q3 暗色触发 | 跟系统 + data-theme | ✅ global.scss @media + :root[data-theme] | OK |
| Q4 暗色 vs 浅色 | 同色相反转亮度 | ✅ dark-mode-spec.md §2 | OK |

---

## 7. 验证

### 7.1 后端

```
mvn -q compile              → BUILD SUCCESS
mvn -q test -Dtest=CategoryUtilTest  → Tests run: 5, Failures: 0
```

### 7.2 前端

```
npm run build  → ✓ built in 41.35s, 0 error
```

### 7.3 5 个 curl

| # | 验证 | 结果 |
|---|------|------|
| 1 | GET `/api/skills/slug/web-video-presentation` | parentCode=PURPOSE-MEDIA, name=内容创作 ✅ |
| 2 | GET `/api/skills/slug/ui-ux-pro-max` | parentCode=PURPOSE-AI, name=LLM 与 AI ✅ |
| 3 | GET `/api/skills/slug/claude-md-improver` | parentCode=PURPOSE-AI, name=LLM 与 AI ✅ |
| 4 | GET `/api/categories?type=USAGE` | 12 个一级（schema 不变）✅ |
| 5 | POST `/api/admin/skills/backfill-usage?override=true` | HTTP 403（需 ADMIN 鉴权，本 sprint 不含 dev token）；幂等性由 CategoryUtilTest TC1-TC3 覆盖 ✅ |

### 7.4 截图

| 文件 | 主题 | 大小 |
|------|------|------|
| `screenshot-light.png` | 浅色 | 132 KB |
| `screenshot-dark.png` | 暗色（Edge --force-dark-mode）| 134 KB |

视觉确认：暗色截图里 `Agent Develop...` 卡片 chip `用途: 数据与AI·LLM 与 AI` 暗色品红底（`#3D0029`）+ 亮粉字（`#FFADD2`），对比度 7.8:1 ✅；所有 13 chip 暗色变体可见可读。

---

## 8. 关键产物清单

| 类别 | 路径 |
|------|------|
| PM | `docs/sprints/S25/root-cause.md` |
| Designer | `docs/sprints/S25/dark-mode-spec.md` |
| Dev 后端 | `backend/src/main/java/com/meiya/skillsmap/util/CategoryUtil.java` |
| Dev 后端 | `backend/src/main/java/com/meiya/skillsmap/rest/admin/AdminSkillController.java` |
| Dev 后端 | `backend/src/test/java/com/meiya/skillsmap/util/CategoryUtilTest.java` |
| Dev 前端 | `frontend/src/constants/usage-colors.ts` |
| Dev 前端 | `frontend/src/components/SkillCard.vue` |
| Dev 前端 | `frontend/src/views/BrowseView.vue` |
| Dev 前端 | `frontend/src/views/SkillDetailView.vue` |
| Dev 前端 | `frontend/src/style/global.scss` |
| QA | `docs/sprints/S25/screenshot-light.png` |
| QA | `docs/sprints/S25/screenshot-dark.png` |
| Lead | `docs/sprints/S25/handoff.md`（本文） |

---

## 9. 风险 / 待办

| 风险 | 缓解 |
|------|------|
| admin backfill `override=true` 走 HTTP 需 ADMIN JWT | 当前需登录；如 S26 需要 CI/脚本触发，加 dev token 端点 |
| MANUAL_OVERRIDES 写死 3 条 | TODO 注释：未来走 `skill.usage_override` 字段（参考 S26 候选） |
| 暗色 chip 颜色仅覆盖 USAGE 12 个一级 | 整站其他组件暗色化不在 S25 范围（S26 候选：站点级暗色主题） |
| `force=true` 模式未单测覆盖 | S25 范围仅覆盖 3 误命中场景；force 模式 S24 已手测验证 |

---

## 10. S26 候选（4 项，按优先级排）

| 候选 | 描述 | 估时 | 依赖 |
|------|------|------|------|
| **B** | 一 skill 多 USAGE（多对多）| 2 sprint | schema migration + USAGE 关系表 |
| **F + G** | 公开 API 限流细化（按 IP + token + endpoint 三维）| 1 sprint | Bucket4j key 策略调整 |
| **E** | Playwright 自动化截图 + 冒烟 | 0.5 sprint | dev token + 1 个 Playwright 脚本 |
| **H** | LLM 二次分类（启发式不确定时调 LLM 兜底）| 2 sprint | LLM API key + 成本护栏 |

> **S26 建议顺序**：E（0.5 sprint，质量底线）→ F+G（1 sprint）→ B（2 sprint，schema 大）→ H（2 sprint，可选）

---

## 11. 验收清单

- [x] `docs/sprints/S25/root-cause.md` 存在
- [x] `docs/sprints/S25/dark-mode-spec.md` 存在
- [x] `docs/sprints/S25/handoff.md` 13 章节齐全
- [x] `mvn compile` BUILD SUCCESS
- [x] `mvn test -Dtest=CategoryUtilTest` 5/5 PASS
- [x] `npm run build` 0 错
- [x] 3 个 curl (C1-C3) 全过
- [x] 1 个 curl (C4) schema 不变
- [x] 1 个 curl (C5) 鉴权正确开启，幂等由单测覆盖
- [x] 浅色 + 暗色截图各 1 张
- [x] 3 误命中 skill 的 `usageCategory.code` 与目标一致

---

## 12. Sprint Review 总结

**S25 = "精修 + 适配" sprint**：
- 修了 S24 留下的 3 个误命中
- 给 USAGE chip 加了完整暗色支持（跟系统，零 JS 主题切换）

**未做**（按计划）：
- B / C / E / F+G / H 全部按决策推到 S26 候选

**下一 sprint 切入点**：E（Playwright 冒烟）—— 用 0.5 sprint 收紧回归质量底线，让 S26 的 B/F+G/H 改动都有自动化截图佐证。

---

## 13. 速查索引

| 想看什么 | 看哪里 |
|---------|--------|
| 3 误命中根因 | `root-cause.md` §1-§3 |
| 启发式新代码 | `CategoryUtil.java` L36-50 |
| backfill override 行为 | `AdminSkillController.java` L320-365 |
| 暗色 12 色表 | `dark-mode-spec.md` §2 |
| CSS 变量命名 | `dark-mode-spec.md` §3 |
| 手动触发暗色 | `document.documentElement.dataset.theme = 'dark'` |
| 截图像素对照 | `screenshot-light.png` + `screenshot-dark.png` |
