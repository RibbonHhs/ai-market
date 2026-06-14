# PRD — S38 Skill Upload（用户上传本地 Skill）

> **Sprint**: S38
> **作者**: agile-rd-lead（PM 草稿）
> **日期**: 2026-06-14
> **状态**: Draft v1（待 PM 完善）

---

## 1. 背景与问题

SkillsMap 是 Agent Skills 集市。当前 S36 上线后，**新手指引**已能引导用户浏览/搜索/下载 skill，但**最后一步「我能不能把自己写的 skill 也分享出去？」没有出口**：

- `skills-manager` skill 的 `publish` 操作仅「引导」用户去 Admin 后台（`SKILL.md:32`），不直接执行
- Admin 后台 `/admin/skills/new` 当前**只支持从 Git URL 拉取**，不支持 zip 上传
- 无鉴权保护的写端点（除 review/favorite）— 上传必须登录

本期要补齐这条「**用户上传 → 立即可见**」最短路径。

## 2. 目标 / 非目标

### 2.1 目标

- 登录用户能通过浏览器 **Admin 后台上传 zip**，平台 1 分钟内可被搜索/下载
- 登录用户能通过 **skills-manager skill `publish` 子命令** 在终端上传
- 上传端点受 **JWT 鉴权**保护，未登录 → 401
- 上传后 **免审核**，`status=PUBLIC`，立即对所有用户可见
- Admin 上传页支持 **drag & drop** + 实时进度 + 错误态 toast

### 2.2 非目标

- 不做审核工作流（status 状态机延后到 S39+）
- 不做 git URL 上传（已有，从 Admin 老路径走）
- 不做 skill 版本管理（v1/v2 多版本共存 — 留给 S40）
- 不做用户配额 / 限流（依赖 S23 既有 IP 限速）
- 不重写现有 `SkillSeedService` 本地目录扫描路径

## 3. User Story

| ID | As a | I want to | So that |
|----|------|-----------|---------|
| US-1 | 登录用户（人类） | 在 Admin 后台 drag & drop 一个 `.skill` zip | 我能 30 秒完成上传 |
| US-2 | 登录用户 | 上传后立刻看到详情页 + toast「上传成功」 | 确认我分享的 skill 已在平台上 |
| US-3 | 未登录用户 | 访问 `/admin/skills/new` 自动跳登录页 | 知道这功能需要登录 |
| US-4 | 终端用户 | 在 Claude Code 里说「publish my-skill.zip」 | skills-manager 引导我贴 token + 调 API 完成 |
| US-5 | 上传者 | zip 非法（缺 SKILL.md / frontmatter 错） | 收到 4xx + 字段级错误，**不会留下脏数据** |
| US-6 | 浏览者 | 上传者发布后立即搜索到 | 无延迟可见 |
| US-7 | 平台运营 | 后端有上传日志（userId + 文件名 + 时间） | 出问题可追溯 |

## 4. 功能详述

### 4.1 后端 — `POST /api/skills` (multipart/form-data)

**端点规格**

```
POST /api/skills
Authorization: Bearer <token>
Content-Type: multipart/form-data; boundary=...

- file: <binary zip, ≤ 10MB>
- categoryId: number (必填, SOC 二级)
- usageCategoryIds: number[] (可选, USAGE 维度)
- tagSlugs: string[] (可选, 自动创建未存在的 tag)
```

**响应（成功）**

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 42,
    "slug": "my-cool-skill",
    "name": "my-cool-skill",
    "version": "1.0.0",
    "status": "PUBLIC",
    "createdAt": "2026-06-14T..."
  }
}
```

**错误码**

| HTTP | code | 场景 |
|------|------|------|
| 400 | 40001 | 缺 file / 文件非 zip / zip 损坏 |
| 400 | 40002 | 缺 SKILL.md / SKILL.md 无 frontmatter |
| 400 | 40003 | frontmatter 缺 name / description |
| 401 | 40100 | 缺 token / token 无效 |
| 409 | 40900 | slug 已存在（建议改名后重试） |
| 413 | 41300 | 文件 > 10MB |
| 500 | 50001 | 解压失败（磁盘满等） |

**核心处理流（`SkillUploadService.upload()`）**

1. 鉴权：`JwtAuthFilter` 解析 → `AuthContext.userId`
2. 大小校验：`MultipartFile.getSize() > 10MB` → 413
3. zip 解压到 `/tmp/skill-upload/{uuid}/`（**path traversal 防御**：`new File(dest, entry.getName()).getCanonicalPath().startsWith(dest.getCanonicalPath())`）
4. 校验根目录存在 `SKILL.md`
5. 解析 YAML frontmatter → 必填：`name`, `description`
6. 提取可选字段：`version`, `author`, `tags`, `homepage`, `license`
7. 生成 `slug = name`（冲突则加后缀 `-2`, `-3`）
8. 落盘 zip 到 `${skills.storage.path}/{slug}/{version}.zip`
9. 入库 `skill` 表（status=PUBLIC, source=user-uploaded, **uploader_user_id=ctx.userId**）
10. 关联 `skill_category` / `skill_tag`（tag 不存在 → **自动调用 `TagService.createTag()` 创建**）
11. 清理 `/tmp/skill-upload/{uuid}/`
12. 返回 skill DTO

**入库字段映射**

| SKILL.md frontmatter | DB column | 备注 |
|----------------------|-----------|------|
| `name` | `name`, `slug`（去非 URL 字符） | 必填 |
| `description` | `description` | 必填 |
| `version` | `version`（默认 1.0.0） | 可选 |
| `author` | `author` | 可选 |
| `tags[]` | → `skill_tag` 关联 | **缺失 tag 自动创建**（2026-06-14 Lead 拍板） |
| `homepage` | `homepage` | 可选 |
| — | `uploader_user_id` | **新增列**，从 `AuthContext.userId` 写入（2026-06-14 Lead 拍板） |
| — | `created_by_user_id` | 沿用现有，等同 uploader（MVP 暂存同值） |

**孤儿目录清扫（2026-06-14 Lead 拍板）**

- 落地：`skill.upload.cleanup` 包下加 `UploadTmpCleanupScheduler`，注解 `@Scheduled(fixedRate = 1h, initialDelay = 5min)`
- 逻辑：扫 `/tmp/skill-upload/`，删除 mtime > 24h 的子目录
- 与 `SkillSeedService` 同包，便于复用 `PathUtils` 等工具
- log：INFO 级别记录「清理 N 个孤儿目录，释放 M MB」

### 4.2 前端 — `AdminSkillsNewView.vue`

**路由**：`/admin/skills/new`，在 `meta.requiresAdmin = true` 守卫下

**页面骨架（a-form + a-upload）**

```vue
<a-form layout="vertical" @finish="onSubmit">
  <a-form-item label="Skill 包" required>
    <a-upload-dragger
      :before-upload="handleBeforeUpload"
      :max-size="10"
      accept=".zip,.skill"
      :file-list="fileList"
    >
      <p class="ant-upload-drag-icon"><inbox-outlined /></p>
      <p class="ant-upload-text">点击或拖拽 .skill / .zip 到这里</p>
      <p class="ant-upload-hint">最大 10MB，必须含 SKILL.md</p>
    </a-upload-dragger>
  </a-form-item>

  <a-form-item label="SOC 分类" required>
    <a-cascader :options="socTree" v-model:value="categoryId" />
  </a-form-item>

  <a-form-item label="USAGE 维度">
    <a-select mode="multiple" :options="usageOptions" v-model:value="usageCategoryIds" />
  </a-form-item>

  <a-form-item label="标签">
    <a-select mode="tags" v-model:value="tagSlugs" placeholder="回车确认" />
  </a-form-item>

  <a-button type="primary" html-type="submit" :loading="uploading">
    {{ uploading ? `上传中 ${progress}%` : '上传并发布' }}
  </a-button>
</a-form>
```

**错误处理**

| 后端 code | 前端行为 |
|----------|---------|
| 40100 | 跳 `/login?redirect=/admin/skills/new` |
| 40001/40002/40003 | 在对应字段下显示红色错误，文件保留在 form |
| 40900 | 全局 toast「slug 已存在，建议改名后重试」 |
| 41300 | 全局 toast「文件超过 10MB」 |
| 5xx | 全局 toast「服务器错误，已通知运维」 |

**成功行为**

- toast「上传成功！」
- `setTimeout(1500)` 后 `router.push('/skills/{slug}')`

### 4.3 skills-manager skill — `publish` 子命令

**SKILL.md 改动**（`C:\Users\86133\.claude\skills\skills-manager\SKILL.md`）

把第 32 行：

```
| `publish` | 引导用户走 SkillsMap Admin 后台上传 zip 或 git URL |
```

改为：

```
| `publish` | 上传本地 .skill zip 到平台（需用户提供 Bearer Token） |
```

并新增「## 发布 Skill」章节（替代原「## 发布新 Skill」），内容：

1. 用户先在 SkillsMap 登录拿 token（沿用 §鉴权使用 流程）
2. 调脚本：
   ```bash
   TOKEN="<粘贴>" SKILL_ZIP="./my-skill.zip" CATEGORY_ID=5 \
     bash scripts/publish-skill.sh
   ```
3. 脚本输出：成功 → skill URL；失败 → 退出码 + 错误信息
4. 也可直接 curl：
   ```bash
   curl -fsS -X POST http://127.0.0.1:8767/api/skills \
     -H "Authorization: Bearer $TOKEN" \
     -F "file=@./my-skill.zip" \
     -F "categoryId=5"
   ```

**`scripts/publish-skill.sh` 要点**

- `set -euo pipefail`
- 校验 `$TOKEN` / `$SKILL_ZIP` / `$CATEGORY_ID` 三参
- 校验文件存在 + 大小 ≤ 10MB
- curl 调端点，输出响应体
- 退出码：0 成功 / 1 参数错 / 2 网络错 / 3 业务错

**`api-endpoints.md` 改动**

- 新增 §7 「POST /api/skills（需鉴权）」一节
- 含 curl 示例 + 响应示例 + 错误码表
- 更新文首「受保护端点列表见 § 7」

## 5. 验收标准（DoD）

### 5.1 功能

- [ ] 未登录访问 `/admin/skills/new` → 跳登录
- [ ] 登录后上传合法 zip → 跳详情页，列表可搜到
- [ ] 上传非法 zip（无 SKILL.md）→ 4xx + 字段级错误，无脏数据
- [ ] 同一 zip 上传两次 → 第二次返回 409 + slug 建议
- [ ] 上传 11MB 文件 → 413
- [ ] skill 端 `publish` 子命令可走通

### 5.2 质量

- [ ] 后端：核心 service 单测覆盖率 ≥ 80%
- [ ] 前端：上传页 E2E 用例（Playwright）通过
- [ ] 性能：单 zip（1MB）解压→入库 P95 < 2s
- [ ] 安全：恶意 zip（`../../etc/passwd`）被 path traversal 防御拦截
- [ ] Knife4j 文档同步

### 5.3 部署

- [ ] `application-prod.yml` 含 `spring.servlet.multipart.max-file-size: 10MB`
- [ ] 上传临时目录 `/tmp/skill-upload/` 加进 logrotate
- [ ] 监控告警：5xx 率 > 1% 触发企业微信

## 6. 风险与缓解

| 风险 | 等级 | 缓解 |
|------|------|------|
| zip slip 攻击 | 高 | path traversal 校验 |
| zip bomb（解压后 100GB） | 中 | 单文件 + 解压后总大小双重限制（10MB → 50MB） |
| frontmatter 注入 | 中 | 用 snakeyaml safe load + 字段白名单 |
| slug 冲突 | 低 | 自动加后缀 + 409 引导改名 |
| 磁盘满 | 中 | 落盘前先 `df` 检查 + 失败清理 |
| 并发上传同用户 | 低 | 同一 userId 单文件并发限 3 |

## 7. 依赖

- `AuthContext` (S23)
- `a-upload-dragger`（Ant Design Vue 4 自带）
- `LocalPrivateCache` 取 token（既有）
- 后端 `spring.servlet.multipart` 配置

## 8. 不在范围

- 多版本管理（S40）
- 审核工作流（S39）
- 增量更新（diff 上传）（S41）
- 用户配额（按 S23 既有 IP 限速，不做用户级）
