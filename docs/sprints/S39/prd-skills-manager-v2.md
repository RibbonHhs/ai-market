# S39 PRD — Skills Manager v2 优化

> **Sprint**: S39  
> **范围**: 后端密码泄露修复 + 客户端 skills-manager skill 全面升级  
> **上游输入**: 用户原话 3 条 + 上一轮 (`api-and-interface-design`) 上传过程中暴露的 7 项摩擦  
> **决策记录**: Q1→A（缓存 token + --relogin）/ Q2→A（全局防御）/ Q3→S39

---

## User Story 拆解

### US-1（P0 安全）— 修密码泄露 bug

**As a** SkillsMap 平台用户  
**I want** 调 `/api/auth/me` 和 `/api/auth/register` 时，响应里不要包含我的 password 字段  
**So that** 即使我贴响应给 AI 助手或日志，密码哈希不会泄露

**背景**:
- 上一轮我们上传 `api-and-interface-design` skill 时，用户调 `GET /api/auth/me` 拿 token，响应 `data.userInfo` 字段里**含 `password`（bcrypt 哈希 `$2a$10$...`）**，用户贴出响应时哈希已泄露
- 根因：`AuthController.me()` 和 `register()` 直接 `return Result.ok(user)`，而 `User` 实体无 `@JsonIgnore`/`@JsonProperty(WRITE_ONLY)` 字段注解

**AC（验收标准）**:
- [ ] `GET /api/auth/me` 响应 `data` 不含 `password` 字段（仅含 id/username/email/displayName/avatar/role/status/createTime）
- [ ] `POST /api/auth/register` 响应 `data` 不含 `password` 字段
- [ ] `User` 实体层 password 字段加 `@JsonProperty(access = WRITE_ONLY)` 兜底（即使将来有其他 controller 误返回 User 也不泄露）
- [ ] 现有 `/api/auth/login` 响应不变（`buildAuthResp` Map 路径无 password 字段）
- [ ] 现有 `JwtAuthFilter` + `AuthContext` 链路不变
- [ ] 现有前端反序列化（如有）不被破坏

**测试用例**:
1. 注册新用户 `test_pwd_leak_1`，响应 JSON grep `password` → 0 命中
2. 登录拿 token，调 `/api/auth/me`，响应 JSON grep `password` → 0 命中
3. 拿同样的 token 调 `/api/skills` POST（上传测试 skill），仍然 200

**改文件**:
- `backend/.../entity/User.java`（+1 import +1 字段注解）
- `backend/.../response/UserResponse.java`（新增，~25 行）
- `backend/.../rest/AuthController.java`（me() / register() 返回类型改 UserResponse，buildAuthResp 内 userInfo 同步替换）

**风险**: 低 — DTO 化是后端常规做法；login 端点不受影响；JWT 链路不变

---

### US-2（P0 UX）— skills-manager 支持用户名密码登录

**As a** Claude Code 用户（加载了 skills-manager skill）  
**I want** 直接输入 SkillsMap 平台用户名密码就能用 skill  
**So that** 不用再去浏览器 F12 → Application → Local Storage 复制 `auth_token`

**背景**:
- 当前 SKILL.md §鉴权使用 / §发布 Skill 段落明确写"用户在浏览器登录 SkillsMap 后，从开发者工具复制 `auth_token`"
- 实际体验：用户每次上传都得开浏览器登录、复制 token、贴回对话，摩擦极大
- 用户原话：「我希望是让用户输入平台用户名密码，而不是让用户去浏览器F12拿Token」

**AC**:
- [ ] 新增 `scripts/auth-login.sh`：用 `read -s` 静默读 username + password，调 `POST /api/auth/login` 拿 token
- [ ] Token 缓存到 `~/.skills-manager/.token` 文件（权限 `0600`），包含 username + token + expiresAt
- [ ] 其他脚本（publish-skill.sh / auth-example.sh）**自动复用**已缓存 token（通过 `scripts/auth-common.sh` 共享函数）
- [ ] 支持 `--relogin` 标志强制刷新 token
- [ ] 支持 `--logout` 标志清除缓存的 token
- [ ] 支持 `SKILLSMAP_USERNAME` / `SKILLSMAP_PASSWORD` env 变量传入（CI 场景）
- [ ] 密码在内存中调完 `curl` 立刻 `unset`，不写日志不 echo
- [ ] 客户端 SKILL.md 删除"F12 复制 token"的所有指引段落
- [ ] references/api-endpoints.md 更新 Token 获取流程为"调 login 端点"

**测试用例**:
1. 首次运行：脚本检测 `~/.skills-manager/.token` 不存在 → 提示输入 username/password → 调 login → 缓存 token
2. 再次运行：脚本检测缓存 → 直接用 token → 上传成功
3. Token 过期（模拟 40101/40102 响应）：脚本自动 `unset` + 重新提示输入
4. `--relogin`：强制忽略缓存，提示重新输入
5. `--logout`：删除 `~/.skills-manager/.token`
6. 缓存文件权限检查：Linux/macOS 上 `stat -c %a` 返回 `600`（Windows 上跳过此检查）
7. 错误密码：返回 40100，脚本 exit 4，提示"用户名或密码错误"
8. 网络错：exit 2

**改文件**:
- `~/.claude/skills/skills-manager/scripts/auth-login.sh`（新增，~80 行）
- `~/.claude/skills/skills-manager/scripts/auth-common.sh`（新增，~50 行，token 缓存读写函数）
- `~/.claude/skills/skills-manager/scripts/publish-skill.sh`（重写，不再依赖 env TOKEN）
- `~/.claude/skills/skills-manager/scripts/auth-example.sh`（重写，调用 auth-common.sh 拿 token）
- `~/.claude/skills/skills-manager/SKILL.md`（删除 F12 段，新增 auth-login 说明）
- `~/.claude/skills/skills-manager/references/api-endpoints.md`（更新 §6 Token 获取流程）

**风险**: 中 — 涉及凭据处理，需严格 `read -s` + `unset` + 0600 权限；Windows 上 `read -s` 在 Git Bash 下可用但 PowerShell 行为不同，需说明

---

### US-3（P1）— skills-manager 上传流程 UX 打磨

**As a** Claude Code 用户  
**I want** 在对话里说"帮我上传这个 skill"就能完成，**不需要我去查 CATEGORY_ID / 自己 zip / 自己查错误码含义**  
**So that** 整个发布流程在 1 轮对话里完成

**背景（上一轮暴露的 7 项摩擦）**:
1. Windows 没有 `zip` 命令 + PowerShell `Compress-Archive` 限制（必须 `.zip` 扩展名，不接受 `.skill`）
2. zip 根目录必须含 SKILL.md，但首次打包打成 `api-and-interface-design\SKILL.md`（带父目录前缀）被用户自行发现
3. 用户必须知道 SOC 二级分类 id（如 24=计算机职业），且二级下没有"API 设计"细分类
4. 后端只校验 `name`+`description`，但 `version` 已支持默认 1.0.0（无问题）
5. USAGE / tag 全靠用户记忆，无引导
6. 退出码只区分 0/1/2/3，但后端错误码有 9 种
7. 无 dry-run；用户上传前不知道 zip 会被打成什么样

**AC**:
- [ ] **A. 自动 zip 打包**（Windows / macOS / Linux 跨平台）
  - 脚本接受 `SKILL_DIR=./api-and-interface-design`（目录路径），而非 `SKILL_ZIP=./xxx.zip`
  - Windows 走 PowerShell `System.IO.Compression.ZipFile.CreateFromDirectory`（已验证）
  - macOS/Linux 走系统 `zip` 命令
  - 自动 staging 到临时目录，确保 zip 根目录直接含 `SKILL.md`（不带父目录前缀）
  - 打包完成自动清理 staging
- [ ] **B. Dry-run 模式**
  - 默认开启 `dry-run`：显示"将打包 X 个文件，共 Y bytes，将调 POST /api/skills" → 用户确认后才真正上传
  - `--no-dry-run` 跳过确认（CI 场景）
  - `--dry-run` 显式触发（默认已是）
- [ ] **C. 分类引导**
  - 脚本自动调 `GET /api/categories` 拿分类树
  - 渲染为带编号的列表（如 `[1] 计算机与数学 > 软件开发 > 后端开发`）
  - 用户输入编号或关键字筛选
  - 选中后**不要求用户记 CATEGORY_ID**，脚本内部换算
  - `--category-id 24` 支持显式传值绕过引导
- [ ] **D. USAGE / Tag 引导（可选）**
  - Dry-run 时列出 USAGE 维度（`GET /api/categories?usage=true`）和已存在 tags
  - 用户可输入 `USAGE_IDS="1,2"` 或 `--tag claude,agent` 一次传完
  - 全部不传也 OK，service 端允许空
- [ ] **E. 错误码可读化**
  - 脚本内置 `code → message` 映射（覆盖 40001-40004/40900/41300/50001/50008 + 40100/40101/40102/40300/42900）
  - 业务错时输出：`❌ [40001] 文件缺失/非 zip/zip 损坏（请检查 SKILL_ZIP 是否为有效 zip）` 而非裸 `code=40001`
  - 退出码细分：1=参数错 / 2=网络错 / 3=认证错 / 4=客户端错（40001-40004/41300） / 5=服务端错（5xxx） / 6=冲突（40900，提示改名重试）
- [ ] **F. 不再依赖 `TOKEN` env**
  - publish-skill.sh 改用 `auth-common.sh` 自动拿 token
  - 旧 `TOKEN=...` 写法保留兼容（带 deprecation warning）

**测试用例**:
1. Windows 走 PowerShell 路径：传 `SKILL_DIR=./test-skill`（含 SKILL.md），脚本应能打包 + 上传
2. macOS/Linux 走 zip 命令：同上
3. Dry-run：脚本显示计划，不实际调 POST
4. 分类引导：脚本列分类列表，输入 `2` → 选中并继续
5. 错误 40900：脚本输出 `❌ [40900] slug 冲突（建议改名后重试）`，exit 6
6. 错误 40002：脚本输出 `❌ [40002] 缺 SKILL.md / 无 frontmatter（zip 根目录必须含 SKILL.md）`，exit 4
7. 错误 40101/40102：脚本自动触发 --relogin

**改文件**:
- `~/.claude/skills/skills-manager/scripts/publish-skill.sh`（重写，~200 行）
- `~/.claude/skills/skills-manager/scripts/auth-common.sh`（新增，含 `code → message` 映射表）
- `~/.claude/skills/skills-manager/SKILL.md`（更新 publish 章节 + 新增 zip 跨平台说明）
- `~/.claude/skills/skills-manager/references/api-endpoints.md`（新增 categories 端点说明）

**风险**: 中 — 跨平台 zip 复杂度；分类树可能有几百条 → 需要搜索/分页

---

### US-4（P1）— SKILL.md 升级为含子命令的完整文档

**As a** Claude Code / Cursor / Codex 用户  
**I want** SKILL.md 顶部有清晰的子命令清单，每个子命令有"何时用 / 怎么调 / 返回什么"的快速说明  
**So that** 加载 skill 后我（AI）能立刻知道有哪些操作可用

**背景**: 当前 SKILL.md 顶部"可执行操作"表已包含 list/search/detail/download/sync/publish 6 项，但缺 `auth` / `login` / `logout` 3 项子命令的入口；且 publish 章节冗长嵌在文档中部。

**AC**:
- [ ] SKILL.md 顶部 `## 可执行操作` 表新增 `auth`（登录拿 token）/ `login`（同 auth 别名）/ `logout`（清缓存）3 行
- [ ] 每个操作给出 1 行说明 + 1-2 行典型示例
- [ ] publish 章节抽出来作为子命令详解（含 dry-run / 分类引导）
- [ ] 移除"用户在浏览器登录复制 auth_token"的所有指引
- [ ] 新增"安全须知"章节：明确密码用 `read -s`、不写入 commit、不 echo

**改文件**:
- `~/.claude/skills/skills-manager/SKILL.md`（重写 v2.0.0）

**风险**: 低 — 纯文档

---

## Sprint 拆分建议

### Sprint 1（本轮 S39）— 必修

- US-1（密码泄露修复）— 1 个后端 PR
- US-2（用户名密码登录）— 1 个客户端 skill PR（含 auth-login.sh + auth-common.sh）
- US-3（上传流程打磨）— 1 个客户端 skill PR（重写 publish-skill.sh）
- US-4（SKILL.md v2）— 跟随上面 PR，合并在 1 个 skill 升级 PR 里

**总 PR 数**: 2（1 后端 + 1 客户端 skill）

### 验证门槛

- 后端：`./mvnw -q clean compile` + 启动 dev + 跑 3 个测试用例
- 客户端：跑 dry-run → 真传 `test-skill.zip` → 验证公开可访问
- 不动 prod，dev 模式验证即可

---

## 关键风险（汇总）

1. **客户端密码处理**：必须 `read -s` + 调完 `unset` + 缓存文件 0600
2. **DTO 化不能破坏现有 login 链路**：`buildAuthResp` Map 路径不变
3. **Windows `read -s` 兼容性**：Git Bash 可用，PowerShell 行为不同，SKILL.md 里要写清楚
4. **跨平台 zip**：Windows 走 .NET ZipFile，macOS/Linux 走系统 zip；脚本需检测平台
5. **错误码映射表与后端 BizCode 同步**：本轮在后端 BizCode 枚举不变情况下，客户端做独立映射（文档里加 note 提示）

---

## 不在 S39 范围（明确划清）

- 后端 `BizCode` 枚举统一上传错误码（建议未来 S40+ 优化）
- 前端 `front-vue3` 的 `UserStore`/`AuthStore` 适配（需前端团队独立 sprint）
- admin 端的用户管理（不同 DTO `AdminUserResponse` 后续再说）
- SKILL.md 国际化（v1.0 中文，v2.0 仍中文，i18n 推后）
