# Sprint S26 — QA 执行手册（Runbook）

> **范围**：跑通 5 条 e2e + 归档 5 张截图 + 验证 `npm run build` 不破。
> **前置**：后端 8767（dev profile）+ 前端 7777 已就位（沿用 S25）。
> **执行人**：QA 角色（自动化）或任意协作者。

---

## 1. 前置条件

### 1.1 服务检查

```bash
# 后端 8767
curl -s http://127.0.0.1:8767/api/categories | head -c 200
# 期望：返回 JSON（200 OK）

# 前端 7777
curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:7777/
# 期望：200
```

### 1.2 若服务未起

```bash
# 后端（H2 in-memory）
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# 前端
cd frontend
npm install
npm run dev
```

---

## 2. 安装 Playwright 依赖（仅一次）

```bash
cd frontend
npm install -D @playwright/test@1.48
npx playwright install chromium
```

**注意**：
- pin 1.48（避免 1.49 breaking）
- 仅装 chromium（webkit/firefox 留 v1.1）

---

## 3. 跑测

### 3.1 e2e 全跑

```bash
cd frontend
npm run test:e2e
```

**期望**：5 passed (or 5 passed + 1 skipped for `04-usage-filter` if `usage-filter-tool` 不在 seed)

### 3.2 UI 模式（可选调试）

```bash
npm run test:e2e:ui
```

### 3.3 单 spec 跑（排错）

```bash
npx playwright test e2e/05-dark-mode.spec.ts
```

---

## 4. 截图归档验证

`npm run test:e2e` 通过后，检查：

```bash
ls -la docs/sprints/S26/screenshots/
```

**期望**：
- `home.png` （浅色首页）
- `browse.png` （浅色 Browse）
- `detail.png` （浅色 Detail）
- `dark-home.png` （暗色首页）
- `dark-detail.png` （暗色 Detail）

**核对**：
- [ ] 5 张齐全（浅 3 + 暗 2）
- [ ] 文件大小 > 10KB（避免空白截图）
- [ ] 浅色背景非纯黑 / 暗色背景非纯白（确认主题切换有效）
- [ ] 无水平滚动条 / 无明显错位

---

## 5. 构建验证

```bash
cd frontend
npm run build
```

**期望**：
- vue-tsc 类型检查 0 错
- vite build 产物生成 `dist/`
- 0 ERROR（WARNING 可接受）

---

## 6. 失败排错速查

| 现象 | 原因 | 修复 |
|------|------|------|
| `Executable doesn't exist` | 未跑 `npx playwright install chromium` | 执行步骤 2 |
| `connect ECONNREFUSED 127.0.0.1:7777` | 前端未起 | `npm run dev` |
| `connect ECONNREFUSED 127.0.0.1:8767` | 后端未起 | `./mvnw spring-boot:run` |
| `expect(locator).toBeVisible() timeout` | seed 数据缺失 | 检查 H2 启动日志 → seed 7 skill |
| `04-usage-filter` skipped | tool chip 不在 seed | 属正常（`test.skip`） |
| 截图全白 / 全黑 | 主题未应用 | 检查 prefers-color-scheme / data-theme |
| vue-tsc 报错 data-testid 类型 | props 不接受任意 attr | 已在 `<a-tag>` / `<button>` 等原生标签上加，无需 props |

---

## 7. 验收清单

- [ ] `npm run test:e2e` 5/5 PASS（最多 1 skipped for `04-usage-filter`）
- [ ] `docs/sprints/S26/screenshots/` 含 5 张 PNG
- [ ] `npm run build` 0 错
- [ ] 4 个 Vue 文件含 data-testid
- [ ] `.github/workflows/e2e.yml` 空壳存在
- [ ] `frontend/package.json` 含 `test:e2e` + `test:e2e:ui`

---

## 8. 完成签字

- 执行人：__________
- 执行日期：__________
- 结果：__________ PASS / FAIL
- 备注：__________

执行完成后，通知 **agile-rd-lead** 启动 #27（Lead 收尾 + handoff.md）。