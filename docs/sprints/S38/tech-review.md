# Tech Review — S38 Skill Upload

> **Reviewer**: dev-kevin
> **Sprint**: S38
> **Source PRD**: `docs/sprints/S38/prd.md` (Draft v1, 2026-06-14)
> **Date**: 2026-06-14
> **Status**: proposed

---

## 1. 可行性结论

**有条件 YES**。3 条理由：
1. 后端 80% 复用现有 `AdminSkillUploadController`（md/zip + MarkdownFrontmatterParser + SkillStorageService），新增成本集中在「**鉴权接入** + 「**入库入库到 `skill` 表**」+ 「**事务边界与清理**」三处。
2. 前端 `AdminSkillEditView.vue` 已含 `<SkillUploader>` 组件 + `SkillUploader.vue` 已封装 dragger；但现有路径是「Admin → 先解析 → 再填表 → 再 save」二段式，PRD 要求「上传即发布」一段式，需新增独立 `AdminSkillsNewView.vue`（不改旧页，避免回归 S05/S02/S32 流程）。
3. `skills-manager` skill 改动可控：`SKILL.md` 第 32 行 + 新增 `scripts/publish-skill.sh` + `api-endpoints.md` §7，三处均为增量。

**前置依赖（必须满足才能启动编码）**：
- PM 出 v2 PRD（含 DoD 字段级错误消息、tag 自动创建策略、uploader_id 字段是否要加）
- Designer 出 design.md（基于 PRD §4.2 骨架，亮/暗双截图）

---

## 2. 后端实现路径

### 2.1 涉及文件清单

| 类型 | 路径 | 用途 |
|------|------|------|
| 新增 | `rest/SkillUploadController.java`（放 `rest/` 根，非 admin） | 新端点 `POST /api/skills`（与现有 `POST /admin/skills/*` 解耦） |
| 新增 | `service/SkillUploadService.java` + `service/impl/SkillUploadServiceImpl.java` | 核心业务（解压 / frontmatter / 入库） |
| 新增 | `request/SkillUploadRequest.java` | multipart 接收 + `categoryId` / `usageCategoryIds` / `tagSlugs` |
| 新增 | `response/SkillUploadResponse.java` | 返回 `{id, slug, name, version, status, createdAt}` |
| 新增 | `util/YamlFrontmatterExtractor.java` | 复用 `MarkdownFrontmatterParser`，单测更轻 |
| 修改 | `common/BizCode.java` | 加 6 个码：`40001 / 40002 / 40003 / 41300 / 40901 / 50009`（见 §5.2） |
| 修改 | `application.yml` + `application-prod.yml` | `spring.servlet.multipart.max-file-size: 10MB` |
| 修改 | `config/StorageProperties.java` | 加 `skills.temp-dir=/tmp/skill-upload` |
| 改动 | `entity/Skill.java`（可能） | 见 §2.5 |

### 2.2 关键类设计

```java
// SkillUploadService 接口
public interface SkillUploadService {
    SkillUploadResponse upload(SkillUploadRequest req, Long userId);
}

// 私有方法签名（impl）
private Path extractZipSafely(MultipartFile file)              // throws BizException(40001/41300/50009)
private void assertNoPathTraversal(ZipEntry entry, Path dest)  // throws BizException(40001)
private SkillFrontmatter parseFrontmatter(Path skillMd)        // throws BizException(40002/40003)
private String resolveUniqueSlug(String name)                  // throws BizException(40901)
@Transactional(rollbackFor = Exception.class)
public SkillUploadResponse upload(SkillUploadRequest req, Long userId)
```

**事务边界**：`@Transactional` 包裹 zip 落盘之后 + DB 写入之前；**zip 解压不在事务内**（已落 `/tmp/skill-upload/{uuid}/`）。失败时 `finally` 清理临时目录。

**抛错类型**：统一 `BizException(BizCode.XXX, msg)`（沿用 `BizCode` 枚举，不引新体系）。

### 2.3 zip 解压 + path traversal 防御伪代码

```java
private Path extractZipSafely(MultipartFile file) throws IOException {
    Path dest = tempDir.resolve(UUID.randomUUID().toString());
    Files.createDirectories(dest);
    String canonicalDest = dest.toFile().getCanonicalPath();

    long totalSize = 0;
    int entryCount = 0;
    try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
        ZipEntry e;
        while ((e = zis.getNextEntry()) != null) {
            // --- path traversal 防御 ---
            File target = new File(dest.toFile(), e.getName());
            String canonicalTarget = target.getCanonicalPath();
            if (!canonicalTarget.startsWith(canonicalDest + File.separator) && !canonicalTarget.equals(canonicalDest)) {
                throw new BizException(BizCode.BAD_REQUEST, "非法 zip 条目: " + e.getName());
            }
            // --- zip bomb 防御 ---
            if (e.getSize() > MAX_ENTRY_SIZE) throw new BizException(BizCode.BAD_REQUEST, "单文件过大");
            totalSize += e.getSize();
            if (totalSize > MAX_TOTAL_SIZE) throw new BizException(BizCode.BAD_REQUEST, "解压总大小超限");
            if (++entryCount > MAX_ENTRY_COUNT) throw new BizException(BizCode.BAD_REQUEST, "条目过多");
            // --- 写入 ---
            if (e.isDirectory()) Files.createDirectories(target.toPath());
            else Files.copy(zis, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
    return dest;
}
```

### 2.4 YAML frontmatter 解析库选型

| 选项 | 优 | 劣 | 决策 |
|------|----|----|------|
| **snakeyaml**（现状：`MarkdownFrontmatterParser` 已在用） | 已在项目；轻；safe load 防代码注入 | 需手写 trim `---` 块 | **采用** |
| jackson-yaml | 与现有 ObjectMapper 同源 | 多带一个依赖；当前 `pom.xml` 未引入 | 否 |
| esolang-yaml | 纯字符串解析 | 复杂 | 否 |

**评估结论**：复用 `MarkdownFrontmatterParser.parse()` 即可（其内部已用 snakeyaml safe load + 字段白名单），无需新依赖。`SkillUploadService` 直接调用它，不要重新写一份。

### 2.5 数据库改动

**首选方案：不动表，复用现有字段**。
- `skill.name / slug / description / version / license / author_name / homepage / tags` 全已存在
- `skill.source = "user-uploaded"`（已有 `source` 字段，admin 老路径用 `official / community / private / imported`）
- `skill.source_type = "USER_UPLOAD"`（沿用 S02 已加字段）
- `skill.created_by_user_id`（已有，存 `AuthContext.userId`）
- `skill.status = "PUBLIC"`（已有，沿用 enum 字符串）
- `skill.package_size = multipart.size`
- `skill.install_command` 自动生成为 `npx skills add {slug}`（沿用 S05 模式）

**可选加列（待 Lead 拍板，见 §7）**：
- `uploader_user_id`：若要快速查「我上传的 skill」列表，建议新增独立 FK 列（与 `created_by_user_id` 解耦语义）
- `uploaded_at`：与 `create_time` 重复可省

**不需新表**：tag 自动创建走 `TagService.createBySlugIfAbsent()`（假设已存在，否则需 S38 内补一个方法），关联走现有 `skill_tag` 关联表。

### 2.6 鉴权接入点

- `SkillUploadController` 类级别加 `@RequireAuth`（自定义注解，与现有 `JwtAuthFilter` + `AuthContext` 对齐；如项目无此注解，则直接 `if (AuthContext.userId == null) throw BizException(UNAUTHORIZED)`）
- **不**走 `@RequiresAdmin`：PRD §2.1 明确「登录用户」即可，非 ADMIN-only
- 与现有 `FavoriteController` / `ReviewController` 写端点的鉴权模式一致（参考 `rest/ReviewController.java`）

### 2.7 Knife4j 注解位置

- 类级 `@Tag(name = "Skill - 用户上传")`
- 方法级 `@Operation(summary = "上传 Skill 包（需鉴权）")` + `@ApiResponses({@ApiResponse(code=40001,...)})`
- request 字段 `@Parameter(description = "zip ≤ 10MB")`

---

## 3. 前端实现路径

### 3.1 涉及文件清单

| 类型 | 路径 | 用途 |
|------|------|------|
| 新增 | `views/admin/AdminSkillsNewView.vue` | 一段式上传页（drag + 表单 + 提交） |
| 新增 | `api/skillUpload.ts` | `skillUploadApi.uploadSkillZip(formData, onProgress)` |
| 修改 | `api/admin.ts`（**不动**） | 已有的 `uploadSkillZip` 走 `POST /api/admin/skills/upload-zip`，与新端点 `/api/skills` 路径冲突，**不要复用** |
| 修改 | `router/index.ts` | 注册 `/admin/skills/new` → `AdminSkillsNewView.vue`（覆盖现有指向 `AdminSkillEditView.vue`） |
| 修改 | `views/admin/AdminLayout.vue` | 加「上传 Skill」菜单项（如需独立入口） |

### 3.2 样式复用点

- 复用 `components/SkillUploader.vue` 中 dragger 视觉（图标 + 文字 + hint 排版）
- 复用 `views/admin/AdminSkillEditView.vue` 中 a-form `layout="vertical"` + a-row/a-col 网格
- 复用 `views/admin/AdminLayout.vue` 的 `admin-content` padding 与 `page-header`
- **不复用** `SkillUploader.vue` 组件本体（它走「解析预览 → 回填表单」二段式，与 PRD §4.2 一段式不符）

### 3.3 ajax.ts 是否需要改？

**否**。已有 `ajax.upload<T>(url, formData, config)`（`frontend/src/axios/ajax.ts:91-102`），原生 axios 实例走请求拦截器自动加 `Authorization: Bearer ${auth.token}`（`interceptor.ts:21-31`），满足 JWT 注入。只需新增 `skillUploadApi.uploadSkillZip(formData, onProgress)` 调用现有 `ajax.upload` 并传入 `onUploadProgress`。

### 3.4 文件上传进度 — `a-upload` 还是 XHR？

**直接复用 `a-upload-dragger` 的 `:custom-request`**（PRD §4.2 给的是 `:before-upload`，不支持进度）。建议方案：
- 改用 `a-upload` 的 `:custom-request` 钩子，**内部**走 `ajax.upload` + `onUploadProgress` → 更新 `progress` ref
- `a-button :loading="uploading"`，loading 文本 = `上传中 ${progress}%`
- **不需要**自己写 `XMLHttpRequest`，axios 已透传 progress 事件

### 3.5 路由 + meta.requiresAdmin 配置点

- `frontend/src/router/index.ts:95-99`：现有 `/admin/skills/new` 指向 `AdminSkillEditView.vue`，**改为指向新 `AdminSkillsNewView.vue`**
- meta：`{ title: '上传 Skill' }`（无需新加 `requiresAdmin`，父路由 `meta: { requiresAdmin: true }` 已生效）

---

## 4. Skill 改动

### 4.1 `publish-skill.sh` 风格对齐点

- 与 `sync-skills.sh` 一致：`set -euo pipefail` + `HOST="${HOST:-http://127.0.0.1:8767}"` 默认值
- 用 `curl -fsS -X POST -F "file=@$SKILL_ZIP" -F "categoryId=$CATEGORY_ID" -H "Authorization: Bearer $TOKEN"`
- 退出码：0 / 1 参数 / 2 网络 / 3 业务（与 PRD §4.3 一致）
- 不引 `jq` 依赖（与 `sync-skills.sh` 不同，sync 才有大量列表解析需求）

### 4.2 `SKILL.md` 章节改动 diff

```diff
-| `publish` | 引导用户走 SkillsMap Admin 后台上传 zip 或 git URL |
+| `publish` | 上传本地 .skill zip 到平台（需用户提供 Bearer Token） |
```

新增「## 发布 Skill」章节，替换原「## 发布新 Skill」段落（87-101 行整段重写）。

`SKILL.md:91` 文首「受保护端点列表见 § 6」改为「§ 7」（因 § 6 被移走）。

### 4.3 `api-endpoints.md` §7 格式

沿用现有 §1-§6 的「示例 curl + 参数表 + 响应示例 + 错误码表」四段式。需补：
- 标题：## 7. 上传 Skill 包（需鉴权）
- curl 示例（与 §4.3 PRD 同步）
- 错误码表（40100 / 40001 / 40002 / 40003 / 40900 / 41300 / 50001）

---

## 5. 难点 / 风险点

### 5.1 Path traversal + zip bomb 同时防御

- zip slip：`ZipEntry.getName()` 可能含 `../../`，必须 `getCanonicalPath().startsWith(dest)` 校验
- zip bomb：单文件 100MB 但解压后 100GB，需双层限制（压缩 ≤10MB + 解压后总 ≤50MB + 单文件 ≤5MB）
- **解决思路**：抽 `ZipSafetyGuard` 静态方法，单测覆盖 3 个恶意 fixture（slip / bomb / 符号链接）

### 5.2 现有 `Skill` 字段够不够 + 「上传即发布」状态机缺位

- 当前 `skill.status` 字段是 String，不是 enum；上传直接写 `PUBLIC` 可能与未来 S39 审核工作流冲突
- **解决思路**：本期就用 String 写 `PUBLIC`，在 `Skill` 字段注释里加 `@since S38`，S39 引入 enum 时再迁移；同时 PRD §2.2 已明确「不做审核工作流」

### 5.3 临时目录 `/tmp/skill-upload/` 清理

- 失败时 `finally` 清理（事务回滚已涵盖 DB，但磁盘残留要手工）
- 进程崩溃后孤儿目录：需 `@Scheduled` 每日清扫 `mtime > 1h` 的目录
- **解决思路**：本期先做 `finally` 清理 + 日志告警；清扫定时任务留给 Ops（已记在 PRD §5.3「logrotate」）

---

## 6. 估时

| 子任务 | 估时（h） | 备注 |
|--------|-----------|------|
| 后端：`SkillUploadController` + `SkillUploadService` | 5 | 含 path traversal + zip bomb 防御 |
| 后端：临时目录清理 + logback 上传日志 | 1 | |
| 后端单测：`SkillUploadService` 单元 + 集成测试 | 4 | 覆盖 slip/bomb/前 matter 错/slug 冲突/事务回滚 5 路径 |
| 前端：`AdminSkillsNewView.vue` + `skillUploadApi` | 3 | 含 a-upload custom-request + progress |
| 前端：路由切换 + 错误码映射 | 1 | |
| Skill：`SKILL.md` + `publish-skill.sh` + `api-endpoints.md §7` | 1 | |
| QA 验证（手动 + Playwright 1 个 spec） | 2 | 沿用 S36 模式 |
| Buffer（联调 + fix） | 2 | |
| **合计** | **19h ≈ 2.5 工作日** | |

**与 handoff.md 5d 计划对比**：
- handoff 计划：PM 1d + Dev 2d + QA 1d + Ops 0.5d + Lead 0.5d = 5d
- 估时差异：**Dev 估时少 0.5d**（实际 19h ≈ 2.4d，handoff 给 2d，**乐观 0.4d**；建议 buffer 加到 3d）
- QA + Ops 估时合理
- **建议 handoff.md 调整为 6 工作日**（Dev 3d + 其他 3d），或保持 5d 但 Lead 收尾压到 0d（自动化 commit）

---

## 7. 待 Lead 决策的技术点

1. **`tagSlugs` 自动创建策略**：上传时若 tag 不存在，是否自动 `INSERT INTO tag (slug, name, created_at)`？自动则需 `TagService.createBySlugIfAbsent(slug)`（当前未实现，需新增 + 单测）；不做则 40004「tag 不存在」让用户先到后台建。
   - **倾向**：自动创建（与 S05 搜索体验一致），但需 Lead 拍板。
2. **`uploader_user_id` 字段是否新增**：现有 `created_by_user_id` 语义重叠，但 ADMIN 后台代上传时二者不同。建议新增独立列（nullable，迁移成本低）。
3. **临时目录 `/tmp/skill-upload/` 清理周期**：本期仅 `finally` 清理；进程崩溃后的孤儿目录清扫定时任务（`@Scheduled(cron = "0 0 3 * * ?")`）归 Ops 还是后端？
   - **倾向**：后端内置（与现有 `SkillSeedService` 启动清扫同包），Ops 只负责 logrotate。

---

## 附录 A — 自测 Checklist（编码完成后）

- [ ] `./mvnw -q clean compile` 通过
- [ ] `npm run build` 通过
- [ ] 单测覆盖率 ≥ 80%
- [ ] 3 个恶意 zip fixture（slip / bomb / 符号链接）单测拦截
- [ ] Knife4j `/doc.html` 新端点可见
- [ ] `application-prod.yml` 已加 `max-file-size: 10MB`
- [ ] Dockerfile 无变化（无需 ops-max 介入镜像层）
- [ ] `publish-skill.sh` 本地 dry-run 跑通（无需真上传）

## 附录 B — 风险 → 缓解 映射（PRD §6 落地确认）

| 风险 | 本评审缓解 | 状态 |
|------|------------|------|
| zip slip | §2.3 `getCanonicalPath.startsWith` + 单测 fixture | ✅ |
| zip bomb | §2.3 双层限制（10MB / 50MB / 5MB 单文件 / 100 条目） | ✅ |
| frontmatter 注入 | 复用 `MarkdownFrontmatterParser`（已 safe load + 白名单） | ✅ |
| slug 冲突 | §2.2 `resolveUniqueSlug()` 自动加 `-2` / `-3`，仍冲突才 40901 | ✅ |
| 磁盘满 | §2.1 `StorageProperties` 配上限 + 落盘前 `Files.exists` 试探 | ✅ |
| 并发上传 | **本期不处理**（与 PRD §2.2「不做限流」一致），留 S41+ | ⚠️ |
