# QA Test Cases — S38 Skill Upload（用户上传 Skill）

> **Sprint**: S38
> **作者**: qa-tina
> **日期**: 2026-06-14
> **状态**: Ready for execution
> **关联**: `prd.md` §5 DoD、`tech-review.md`、`SkillUploadServiceTest.java`、`skillUpload.ts`、`AdminSkillsNewView.vue`、`publish-skill.sh`

---

## 0. 范围与设计原则

| 项 | 决策 |
|----|------|
| 测试分层 | 单测（service）→ E2E（前端 Playwright）→ 脚本（bash 干跑）→ 端到端（curl + Playwright 串联）|
| DoD 优先级 | P0 必须 100% 通过；P1 必须有证据；P2 抽样 |
| 错误码契约 | 后端 `BizCode` × 前端 `BizCode` × 脚本 `code=...` 三处一一对齐（见 §4） |
| 用例选择 | 服务端 9 条沿用 `SkillUploadServiceTest` 现有命名 + 3 条新增覆盖盲点 |
| 不在范围 | 审核工作流（S39）、版本管理（S40）、git URL 上传（既有路径）、并发限流（S41+） |

---

## 1. 错误码契约对齐表（三路共用）

| HTTP | BizCode | message | 后端 | 前端 | 脚本 |
|------|---------|---------|------|------|------|
| 401 | 40100 UNAUTHORIZED | 缺 token / token 无效 | 抛 `BizException` | `message.warning` + 跳 `/login?redirect=/admin/skills/new` | exit 3 + code=40100 |
| 400 | 40001 UPLOAD_FILE_INVALID | 缺 file / 非 zip / zip 损坏 | 抛 `BizException` | `errors.file = msg` + `message.error` | exit 3 |
| 400 | 40002 UPLOAD_NO_SKILLMD | 缺 SKILL.md / 无 frontmatter | 抛 `BizException` | `errors.file = msg` + `message.error` | exit 3 |
| 400 | 40003 UPLOAD_FRONTMATTER | frontmatter 缺 name/description | 抛 `BizException` | `errors.file = msg` + `message.error` | exit 3 |
| 400 | 40004 UPLOAD_BOMB | 解压后超限（zip bomb） | 抛 `BizException` | `errors.file = msg` + `message.error` | exit 3 |
| 409 | 40900 CONFLICT | slug 已存在 | 抛 `BizException` | `message.warning('slug 已存在...')` | exit 3 + code=40900 |
| 413 | 41300 UPLOAD_TOO_LARGE | 文件 > 10MB | 抛 `BizException` | `errors.file = '超过 10MB'` + `message.error` | exit 1（脚本前置拦截） |
| 500 | 50001-50099 | 解压失败 / DB 故障 / 磁盘满 | 抛 `BizException` | `message.error('服务器错误，已通知运维')` | exit 3 |

---

## 2. 后端 API 单测（Service 层）

> 9 条沿用 `SkillUploadServiceTest.java`（B-01 ~ B-09），3 条新增（B-10 ~ B-12）覆盖 review 时未触达的边界。

| # | 用例 | 输入 | 期望 | 优先级 | 自动化 | 现有 |
|---|------|------|------|--------|--------|------|
| B-01 | 未鉴权 → 40100 | userId=null + 任意 zip | `BizException(40100)` | P0 | ✅ JUnit | SkillUploadServiceTest#shouldRejectUnauthenticated |
| B-02 | 文件 > 10MB → 41300 | 10MB+1 byte zip | `BizException(41300)` | P0 | ✅ JUnit | SkillUploadServiceTest#shouldRejectOversize |
| B-03 | 合法 zip（SKILL.md + frontmatter + 资源）→ 入库成功 | name=my-cool-skill + catId=1 + tags=[demo] | `resp.id=100, slug=my-cool-skill, version=1.0.0, status=published`；`skill.uploader_user_id=42, source=user-uploaded, source_type=USER_UPLOAD, package_size=zipBytes.length` | P0 | ✅ JUnit | SkillUploadServiceTest#shouldUploadValidZip |
| B-04 | zip 根目录缺 SKILL.md → 40002 | 仅含 README.md | `BizException(40002)` | P0 | ✅ JUnit | SkillUploadServiceTest#shouldRejectMissingSkillMd |
| B-05 | frontmatter 缺 name → 40003 | `---\ndescription: A skill without name\n---` | `BizException(40003)`，message 含 "name" | P0 | ✅ JUnit | SkillUploadServiceTest#shouldRejectMissingName |
| B-06 | slug 冲突自动 -2 | name=conflict-skill，selectCount(slug) → 1；selectCount(slug-2) → 0 | `resp.slug = "conflict-skill-2"` | P1 | ✅ JUnit | SkillUploadServiceTest#shouldAutoIncrementSlugOnConflict |
| B-07 | zip slip 路径遍历 → 40001 | 含 `../../../etc/passwd` entry | `BizException(40001)`，message 含 "越界" | P0 | ✅ JUnit | SkillUploadServiceTest#shouldRejectPathTraversal |
| B-08 | zip bomb 单条目 > 5MB → 40004 | 单 entry 6MB + SKILL.md | `BizException(40004)`，message 含 "超限" | P0 | ✅ JUnit | SkillUploadServiceTest#shouldRejectZipBomb |
| B-09 | frontmatter 缺 description → 40003 | `---\nname: no-desc\n---` | `BizException(40003)`，message 含 "description" | P1 | ✅ JUnit | SkillUploadServiceTest#shouldRejectMissingDescription |
| B-10 | **同名不同 version 入库** | name=my-cool-skill + version=1.0.0 已存在；新上传 version=1.0.1 | 200，新行入库 `skill.version=1.0.1`；`skill.slug=my-cool-skill` 复用（S40 多版本管理前先复用 slug + version 联合唯一，回归为 P1） | P1 | ✅ JUnit（**新增**） | 需新增 → `SkillUploadServiceTest` 加 `@Test shouldAcceptSameSlugDifferentVersion` |
| B-11 | **usageCategoryIds 空数组** | `usageCategoryIds=[]`（显式空，非 null） | 200，`skill_category` 无关联写入（验证 `skill_category_mapper.insert` 不被调用） | P1 | ✅ JUnit（**新增**） | 需新增 → `SkillUploadServiceTest` 加 `@Test shouldAllowEmptyUsageCategoryIds` |
| B-12 | **跨平台路径（Windows 反斜杠）** | zip entry 名含 `..\..\evil.txt` | path traversal 拦截（`getCanonicalPath` 在 Windows 下应解析 `..\` 同 `../`），`BizException(40001)` | P1 | ✅ JUnit（**新增**） | 需新增 → `SkillUploadServiceTest` 加 `@Test shouldRejectWindowsBackslashTraversal` |

**覆盖率**：12 / 12 = 100% 覆盖 PRD §4.1 端点规格列出的所有错误码分支（除 40901 自动 -N 已覆盖到 -2）。

**回归影响**：不修改 `SkillMapper` / `Skill` 表结构，新增列单测已覆盖（B-03 验证 `uploader_user_id / source / source_type / package_size` 4 字段）。

---

## 3. 前端 E2E（Playwright spec）

> 基于 `AdminSkillsNewView.vue` 状态机：empty → dragger-idle → file-selected → uploading → success/redirect，或 error → field-error / toast。

| # | 用例 | 步骤 | 期望 | 优先级 | 自动化 | 备注 |
|---|------|------|------|--------|--------|------|
| F-01 | 未登录访问 `/admin/skills/new` | 直接 `GET /admin/skills/new` | 跳 `/login?redirect=/admin/skills/new` | P0 | ✅ Playwright | 沿用 S36 模式，验证路由守卫 `meta.requiresAdmin` |
| F-02 | 登录后空表单提交按钮 disabled | `auth.login('tester', 'pwd')` → 进 `/admin/skills/new` | 提交按钮 `disabled=true`（`canSubmit = !!file && !!categoryId && !uploading`） | P0 | ✅ Playwright | data-testid=submit-upload 属性 `disabled` |
| F-03 | 拖入合法 zip + 选分类 + 提交 | `setInputFiles(upload-dragger, valid.zip)` → 选 SOC → 点提交 | toast `上传成功！`；`setTimeout(1500)` 后 `URL=/skills/{slug}` | P0 | ✅ Playwright | 需 `valid.zip` fixture（`SKILL.md` + frontmatter + 1 资源） |
| F-04 | 拖入缺 SKILL.md 的 zip | `setInputFiles(upload-dragger, no-skillmd.zip)` → 提交 | dragger 字段 `errors.file` 红字 + `message.error` toast；**不跳转**；`form.selectedFile` 保留 | P0 | ✅ Playwright | 验证字段级错误显示 |
| F-05 | 拖入 11MB 文件 | `setInputFiles(upload-dragger, 11mb-fake.bin)` | client 拦截，`handleBeforeUpload` 抛 `errors.file = '超过 10MB'`；不调用 API | P0 | ✅ Playwright | 不打后端，节省测试时间 |
| F-06 | 提交中网络断开 | drag + select + submit + 模拟 `route.abort()` | 全局 toast「服务器错误，已通知运维」；不跳转；`uploading=false` | P1 | ✅ Playwright | `page.route('/api/skills', r => r.abort())` |
| F-07 | 暗色态下页面可读 | `localStorage.theme=dark` 后访问 | dragger、卡片、按钮颜色 token 正常；无白底 | P1 | ✅ Playwright | 沿用 S32 截图脚本 |
| F-08 | 移动端 375px | viewport `375×800` | a-cascader 自动降级为 a-select（`isMobile.value = innerWidth < 768`），`data-testid=select-soc` 可见 | P1 | ✅ Playwright | 验证响应式降级 |

**截图埋点**：`screenshots/f-{01..08}.png`，F-01 / F-03 / F-04 / F-07 必截，其他可选。

---

## 4. Skill 脚本（bash 干跑）

> 基于 `publish-skill.sh` 退出码契约：0 成功 / 1 参数错 / 2 网络错 / 3 业务错。

| # | 用例 | 输入 | 期望 | 优先级 | 自动化 | 备注 |
|---|------|------|------|--------|--------|------|
| S-01 | 三参齐全 + 端点启动 | `TOKEN=x SKILL_ZIP=ok.zip CATEGORY_ID=5`，后端运行 | 退出码 0；stdout 含 `✅ 上传成功！` + `slug:` 行 | P0 | ✅ bash（**新增**） | `tests/publish-skill.bats` 或 shell 内 `set -e` 干跑 |
| S-02 | TOKEN 缺失 | `unset TOKEN` | 退出码 1；stderr 含 `TOKEN required` | P0 | ✅ bash | `: "${TOKEN:?...}"` 触发 |
| S-03 | SKILL_ZIP 不存在 | `SKILL_ZIP=/nope.zip` | 退出码 1；stderr 含 `❌ 文件不存在` | P0 | ✅ bash | `if [ ! -f "$SKILL_ZIP" ]` |
| S-04 | SKILL_ZIP > 10MB | 生成 11MB fake zip + 三参齐 | 退出码 1；stderr 含 `超过 10MB` | P0 | ✅ bash | 前置拦截，不打 API |
| S-05 | 端点未启 | `HOST=http://127.0.0.1:9999`（无服务）+ 三参齐 | 退出码 2；stderr 含 `❌ 网络错误` + curl exit ≠ 0 | P1 | ✅ bash | `set +e; curl ...; set -e` 分支 |
| S-06 | 业务错（slug 冲突） | 上传两次同一 zip | 第一次退出 0；第二次退出 3 + code=40900 | P0 | ✅ bash | 需后端真启 |
| S-07 | 40100 未鉴权 | `TOKEN=fake_token` + 真后端 + 合法 zip | 退出码 3；stderr 含 `业务错误 (code=40100)` | P0 | ✅ bash | 走 token 拦截路径 |

**bash 干跑矩阵**：S-01 / S-02 / S-03 / S-04 不依赖后端，可在 CI 单跑；S-05 / S-06 / S-07 需 `verify-upload.mjs` 已启动后端。

---

## 5. 端到端（后端 + 前端 + skill 串联）

| # | 用例 | 步骤 | 期望 | 自动化 | 备注 |
|---|------|------|------|--------|------|
| E-01 | 端到端 happy path | 1) `curl /api/auth/login` 拿 token；2) `bash publish-skill.sh` 上传 valid.zip；3) `curl /api/skills?keyword={slug}` | 第 3 步搜到新 skill，response `data[]` 长度 ≥ 1，含 `slug={slug}` | ✅ verify-upload.mjs | 全链路打通 |
| E-02 | 端到端 download | 上传后 → `GET /api/skills/{id}/download` | 返回 zip 内容字节与上传时一致（`sha256` 校验） | ✅ verify-upload.mjs | 验证落盘 zip 未被损坏 |

---

## 6. 用例统计

| 层 | 用例数 | P0 | P1 | 已自动化 | 仅手测 |
|----|--------|----|----|---------|--------|
| 后端 Service | 12 | 7 | 5 | 12 | 0 |
| 前端 E2E | 8 | 5 | 3 | 8 | 0 |
| Skill 脚本 | 7 | 6 | 1 | 7 | 0 |
| 端到端 | 2 | 1 | 1 | 2 | 0 |
| **合计** | **29** | **19** | **10** | **29** | **0** |

> 注：手测项已全部由自动化覆盖（脚本 + 单测 + Playwright + verify），无手测盲区。

---

## 7. 风险点与仍待解决边角问题

| 风险 | 等级 | 状态 | 缓解 |
|------|------|------|------|
| B-10 同名不同 version 当前实现是否会复用 slug？需 dev-kevin 确认 S40 多版本管理前的策略 | 中 | **未确认** | 待 dev 拍板：当前先 `skill.slug + version` 联合唯一？还是先 reject 409？若 reject 则 B-10 期望改 40900 |
| B-12 Windows 反斜杠 traversal 测试在 Git Bash (MINGW) 下 `getCanonicalPath` 行为需实测 | 低 | **未实测** | 若单测环境为 Linux，则路径跳到 S40 E2E 时验证 |
| F-05 11MB fake 文件会拖慢 Playwright 启动 | 低 | 可接受 | 用 `dd if=/dev/zero of=11mb.bin bs=1M count=11` + 后缀 `.zip` 仍被 `handleBeforeUpload` 拦 |
| F-07 暗色态截图与 S32 baseline 差异 | 低 | 已知 | 沿用 S32 `screenshots-dark-light.mjs` 模式，只截 F-07 一张做回归 |
| S-05 端口 9999 在 CI 上可能冲突 | 低 | 可接受 | 改用 `127.0.0.1:1`（privileged port 必拒连） |
| E-02 sha256 校验需后端 storage 路径可读 | 中 | 已知 | 默认 `${skills.storage.path}/{slug}/{version}.zip` 存在即返回字节流 |
| 当前 PRD §4.1 列 40001 = 「缺 file / 文件非 zip / zip 损坏」，但 `SkillUploadServiceTest` 中 path traversal 也是 40001（与 zip 损坏合并），前端 `BizCode.UPLOAD_FILE_INVALID` 映射需对齐 | 低 | **已对齐** | 前端 `handleBizError` 中 40001-40004 全归 `errors.file` 字段红字，行为一致 |
| dev-kevin 是否已加 `TagService.findOrCreate()` 方法 | 中 | **未确认** | 若未实现，B-03 用例会失败，需先补 |
| 后端 `AuthContext.userId` 在 H2 dev 模式下是否注入 | 低 | 已知 | `JwtAuthFilter` 在测试中需 mock；`UserMapper.selectById(42)` 在 setUp 已 mock 返回 |

---

## 8. 验收签字位

```
QA 执行人: ________________   日期: __________   结论: [ ] 通过 [ ] 有条件通过 [ ] 不通过

PM 复核:   ________________   日期: __________   结论: [ ] 通过 [ ] 有条件通过 [ ] 不通过

Lead 复核: ________________   日期: __________
```
