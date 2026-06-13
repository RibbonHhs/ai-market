# Sprint S02 Test Cases — Git URL Skill Source

> 作者：qa-tina @ 2026-06-07
> 依据：[`prd-git-source.md`](./prd-git-source.md) §6 边界场景 + DoD
> 工具：JUnit 5（后端） + 手测（前端） + Postman（API 集成）

## 1. 测试矩阵（API 端 14 条）

| ID | 类型 | 场景 | 步骤 | 期望 | BizCode | 优先级 |
|----|------|------|------|------|---------|--------|
| TC-01 | 冒烟 | 公开 GitHub 仓库 | POST `/api/admin/skills/from-git` body=`{url:"https://github.com/anthropics/skills"}` | 200 + `totalImported >= 1` + discovered 列表 | — | P0 |
| TC-02 | 冒烟 | 公开 GitHub 仓库 + 分支 | URL + `ref="dev"` | 200 + `ref="dev"` | — | P0 |
| TC-03 | 鉴权 | 私有 Gitea + 正确 token | URL + `username + token` | 200 + imported >= 1 | — | P0 |
| TC-04 | 鉴权 | 私有仓库 + 错误 token | URL + 错误 token | 50302 + 中文 message | 50302 | P0 |
| TC-05 | URL | URL 格式非法（无 scheme） | `url="github.com/foo/bar"` | 50309 | 50309 | P1 |
| TC-06 | URL | URL 格式非法（无 host） | `url="https://"` | 50309 | 50309 | P1 |
| TC-07 | 404 | 仓库不存在 | `url="https://github.com/no-such-org-9999/no-such-repo"` | 50301 | 50301 | P0 |
| TC-08 | TLS | 自签证书自建 Gitea（未开 insecure） | URL + 不勾选 insecure | 50303 | 50303 | P1 |
| TC-09 | TLS | 自签证书自建 Gitea + 勾选 insecure | URL + `insecureSkipTls=true` | 200 | — | P1 |
| TC-10 | 性能 | 浅克隆 depth=1 | 观察 clone 耗时 + 磁盘占用 | 30s 内 + 总大小 < 100MB | — | P1 |
| TC-11 | 安全 | token 不入日志 | clone 失败后 `grep ghp_ backend.log` | 0 命中 | — | P0 |
| TC-12 | 安全 | token 不入 DB 明文 | `SELECT source_token_enc FROM skill` | 以 `ENC(` 开头 | — | P0 |
| TC-13 | 安全 | token 不出 API | GET `/api/admin/skills/{id}` 响应 | 不含 `source_token_enc` 字段 | — | P0 |
| TC-14 | 并发 | 同一 skill 并发 sync | 同一 id 触发 5 次 POST `/sync` | 串行化执行，无脏写 | — | P2 |

## 2. 业务场景（13 条）

| ID | 场景 | 步骤 | 期望 |
|----|------|------|------|
| TC-15 | Monorepo 自动拆分 | 仓库内同时含根 SKILL.md + sub1/SKILL.md + sub2/SKILL.md | 返回 3 条 discovered，2 个 sub 都有独立 skillId |
| TC-16 | SKILL.md 缺 name 字段 | 仓库内 `bad-skill/SKILL.md` 无 name | 该子目录 skipped，skipReason 写明 |
| TC-17 | name 非 kebab-case | frontmatter `name: MySkill` | 跳过 + skipReason 写明 |
| TC-18 | 同一仓库重复 import | 第二次 import 同一 URL | 走 `updated` 分支，更新 lastSyncAt，不重复创建 |
| TC-19 | 手动 sync 二次确认 | 前端点击「同步」 | 弹出 Modal，列出 URL + ref + token hint + 上次同步状态 |
| TC-20 | sync 失败错误展示 | 后端 lastSyncError 写入 | 详情页/列表显示「✗ 失败 + 时间」，tooltip 显示错误 |
| TC-21 | 列表按 sourceType 过滤 | 选「Git URL」 | 列表只显示 sourceType='GIT_URL' 的 skill |
| TC-22 | 卡片徽章 hover | 鼠标悬停 🔗 徽章 | tooltip 显示完整 URL（截断 200 字符） |
| TC-23 | 详情页「立即同步」按钮 | 详情页存在 GIT_URL skill | 显示「立即同步」按钮（admin 角色），非 admin 隐藏 |
| TC-24 | 定时任务关闭 | 设 `skillsmap.git-source.scheduler-enabled=false` 启动 | 启动日志「scheduler disabled」，60min 后无定时日志 |
| TC-25 | 定时任务开启 | 默认配置启动 | 启动日志「scheduler enabled」，60min 后出现「scanning N stale」 |
| TC-26 | 磁盘配额 | 构造 > 500MB 仓库（depth=1） | 50306 + 提示「仓库总大小」 |
| TC-27 | 单文件超限 | assets/ 下放一个 100MB 文件 | 50307 或警告后跳过（取决于实现，本期跳过 + WARN 日志） |

## 3. 回归用例（zip 流程不退化，5 条）

| ID | 场景 | 期望 |
|----|------|------|
| REG-01 | 上传 zip → 自动应用表单 | 解析后表单字段自动填充（与 Sprint-1 行为一致） |
| REG-02 | 上传 zip → 触发主仓 push | `git_status` 30s 内 success count +1（启用 Git 同步时） |
| REG-03 | zip 内含单层子目录 | flatten 成功，skill 内容上提到 name/ 根目录 |
| REG-04 | 上传后 save 200 | POST `/api/admin/skills` 正常（修复后 status=201, code=0） |
| REG-05 | 列表 / 详情下载 zip 正常 | 公开接口 `/api/skills/{id}/download` 返回 zip 字节流 |

## 4. 前端手测用例（8 条）

| ID | 场景 | 步骤 | 期望 |
|----|------|------|------|
| FE-01 | Tab 切换 | 点击「Git URL」 | 显示 GitUrlForm 组件 |
| FE-02 | URL 必填 | 留空点「克隆并解析」 | 按钮 disabled（不发起请求） |
| FE-03 | URL 格式校验 | 填 `github.com/foo` | 失焦后红框 + 错误文案 |
| FE-04 | insecure 警告 | 勾选「跳过 TLS」 | 下方出现黄色 alert「公网仓库请勿勾选」 |
| FE-05 | 成功结果显示 | clone 成功 | 绿色 alert + 列表 + 「新建 / 更新 / 跳过」tag |
| FE-06 | 鉴权失败错误展示 | 私有仓库错误 token | 红色 alert 显示「鉴权失败,请检查 Username / Access Token」 |
| FE-07 | 列表同步按钮 | 在 GIT_URL skill 行点「同步」 | 二次确认 Modal，列出 ref + token hint |
| FE-08 | 列表来源徽章 | 浏览列表 | GIT_URL 显示「🔗 Git @ main」蓝色徽章；LOCAL_ZIP 显示「📦 本地」灰色 |

## 5. 单元测试（SkillGitServiceImpl 至少 70% 行覆盖）

| 类 | 方法 | 用例 |
|----|------|------|
| SkillGitServiceImpl | `importFromGit` | 4 个 case：成功 / 鉴权失败 / 404 / 无 SKILL.md |
| SkillGitServiceImpl | `syncSkill` | 3 个 case：成功 / 失败 / 非 GIT_URL 抛 40000 |
| SkillGitServiceImpl | `parseUrlAndExtractCredentials` | 3 个 case：URL 嵌 token / 表单 token 优先 / 无 token |
| SkillGitServiceImpl | `makeTokenHint` | 3 个 case：长 token（>8）/ 短 token（<8）/ null |
| SkillGitServiceImpl | `scanAllSkillMd` | 2 个 case：根 + 嵌套 / 全部在子目录 |
| SkillGitServiceImpl | `urlToSlug` | 2 个 case：含特殊字符 / 超长截断 |
| SkillGitServiceImpl | `validateUrl` | 4 个 case：合法 / 无 scheme / 无 host / 非 http(s) |
| SkillGitServiceImpl | `applyFrontmatterToSkill` | 2 个 case：完整 frontmatter / 缺 metadata.version |

```java
// 骨架示例（src/test/java/.../SkillGitServiceImplTest.java）
@SpringBootTest
@ActiveProfiles("test")
class SkillGitServiceImplTest {
    @Autowired SkillGitServiceImpl service;
    @MockBean SkillMapper skillMapper;
    @MockBean StringEncryptor encryptor;

    @Test
    void importFromGit_publicRepo_success() {
        // 用 GitHub test fixture repo
        when(encryptor.encrypt(anyString())).thenReturn("mock-cipher");
        GitImportRequest req = new GitImportRequest();
        req.setUrl("https://github.com/anthropics/skills");
        GitImportResult res = service.importFromGit(req);
        assertThat(res.getTotalImported()).isGreaterThan(0);
    }
    // ...
}
```

## 6. 自动化脚本（Postman / curl 速查）

```bash
# 公开仓库
curl -X POST http://127.0.0.1:8767/api/admin/skills/from-git \
  -H "Authorization: Bearer $ADMIN_JWT" \
  -H "Content-Type: application/json" \
  -d '{"url":"https://github.com/anthropics/skills"}'

# 私有仓库
curl -X POST http://127.0.0.1:8767/api/admin/skills/from-git \
  -H "Authorization: Bearer $ADMIN_JWT" \
  -H "Content-Type: application/json" \
  -d '{
    "url":"https://gitlab.example.com/team/skills.git",
    "ref":"main",
    "username":"ci-bot",
    "token":"glpat-xxxxxxxxxxxx"
  }'

# 同步单个 skill
curl -X POST http://127.0.0.1:8767/api/admin/skills/42/sync \
  -H "Authorization: Bearer $ADMIN_JWT"

# 查询同步状态
curl http://127.0.0.1:8767/api/admin/skills/42/sync-status \
  -H "Authorization: Bearer $ADMIN_JWT"
```

## 7. 验收门槛

- TC-01 ~ TC-13 全部通过（P0 用例）
- TC-15、TC-18、TC-20、TC-21 业务场景通过
- REG-01 ~ REG-05 全部通过
- SkillGitServiceImpl 行覆盖 ≥ 70%
- 前后端构建通过（`./mvnw -q clean compile` + `npm run build`）
