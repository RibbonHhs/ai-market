# Sprint S26 — 截图归档目录

> 此目录存放 5 张 e2e 截图（浅 3 + 暗 2）。
> 由 `frontend/e2e/0X-*.spec.ts` 在断言通过后自动写入。
> Playwright `test-results/` 不入库（已在根 `.gitignore` 覆盖）。

## 期望文件清单（5 张）

| 文件名 | 触发用例 | 视图 | 主题 |
|--------|----------|------|------|
| `home.png` | `01-home.spec.ts` | `/` | 浅色 |
| `browse.png` | `02-browse.spec.ts` | `/browse-skills` | 浅色 |
| `detail.png` | `03-detail.spec.ts` | `/skills/{slug}` | 浅色 |
| `dark-home.png` | `05-dark-mode.spec.ts` | `/` | 暗色 |
| `dark-detail.png` | `05-dark-mode.spec.ts` | `/skills/{slug}` | 暗色 |

## 生成方式

```bash
cd frontend
npm run test:e2e
```

截图路径在 `frontend/e2e/0X-*.spec.ts` 中以**相对路径** `../docs/sprints/S26/screenshots/` 写入。