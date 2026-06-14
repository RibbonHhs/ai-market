# S39 QA — Skills Manager v2 冒烟用例

> **Sprint**: S39  
> **范围**: 9 个冒烟用例覆盖：密码泄露修复（AT-1） + 用户名密码登录（AT-2/6/7） + 跨平台 zip（AT-3） + 真传（AT-4） + 错误码可读化（AT-5） + 端到端（AT-8） + 前端兼容（AT-9）  
> **测试方式**: curl + bash 脚本 + git 验证

---

## AT-1: `GET /api/auth/me` 响应无 `password` 字段（P0 安全）

**前置条件**:
- 后端 dev 模式运行（127.0.0.1:8767）
- 有 admin token（`admin / admin123`）
- commit `16112ef` 已部署

**操作步骤**:
1. `curl -X POST http://127.0.0.1:8767/api/auth/login -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}'` 拿 token
2. `curl -fsS http://127.0.0.1:8767/api/auth/me -H "Authorization: Bearer $TOKEN"`
3. `curl ... | grep -i password` 应 0 命中

**期望结果**:
- 响应 `data` 字段：`id/username/email/displayName/avatar/role/status/createTime`
- 响应 `data` **不含** `password` 字段

**实际结果**: ✅ **PASS**（Phase 3A 跑过，详见 16112ef commit log）

---

## AT-2: `bash auth-login.sh` 用户名密码登录能拿到 token 缓存到 `~/.skills-manager/.token`

**前置条件**:
- 后端 dev 模式运行
- 客户端 4 个脚本已就位：`auth-common.sh / auth-login.sh / publish-skill.sh / auth-example.sh`
- 平台用户存在（admin）

**操作步骤**:
1. `rm -f ~/.skills-manager/.token` 清缓存
2. `SKILLSMAP_USERNAME=admin SKILLSMAP_PASSWORD=admin123 bash ~/.claude/skills/skills-manager/scripts/auth-login.sh`
3. `cat ~/.skills-manager/.token` 应为 JSON 含 token/username/expiresAt/host
4. `ls -la ~/.skills-manager/.token` 看权限（Unix 期望 0600；Windows NTFS 期望 icacls 收紧）

**期望结果**:
- stdout 打印 JWT
- `.token` 文件 JSON 结构 `{username, token, expiresAt, host}`
- 文件权限：Unix=`-rw-------`；Windows `icacls .token` 仅当前用户

**实际结果**: ✅ **PASS**（Phase 3B 跑过，token 缓存成功）

---

## AT-3: `bash publish-skill.sh` 跨平台 zip — Windows PowerShell / Unix zip

**前置条件**:
- 准备 `SKILL_DIR=/tmp/s39-e2e-skill/` 含 SKILL.md
- 平台支持：本机是 Windows（PowerShell）；CI 期望 Unix

**操作步骤**:
1. Windows 验证: `bash publish-skill.sh --skill-dir /tmp/s39-e2e-skill --category-id 24`（不真传，dry-run 即可）
   - 观察 `[2/5] 打包` 阶段：应走 PowerShell `Compress-Archive`（不报"zip command not found"）
2. Unix 验证（如有 macOS/Linux 节点）: `bash publish-skill.sh --skill-dir /tmp/s39-e2e-skill --category-id 24`
   - 观察 `[2/5] 打包` 阶段：应走系统 `zip` 命令
3. 验证 zip 根目录结构：unzip -l 看到 `SKILL.md` 在根（不带父目录前缀）

**期望结果**:
- Windows 走 PowerShell 成功打包（上一轮 Phase 3B 验证过 cygpath 路径转换修过）
- Unix 走 `zip` 命令成功打包
- zip 根目录**直接含** `SKILL.md`（不带父目录前缀，如 `api-and-interface-design/SKILL.md` 是错的）

**实际结果**: ✅ **PASS**（Windows PowerShell 路径已跑过，345 bytes 成功；Unix 待 CI 节点跑）

---

## AT-4: `bash publish-skill.sh --no-dry-run` 真传

**前置条件**:
- AT-2 token 已缓存
- AT-3 zip 打包成功
- 后端 dev 运行

**操作步骤**:
1. `bash publish-skill.sh --skill-dir /tmp/s39-e2e-skill --category-id 24 --no-dry-run`
2. 观察 stdout 最后 5 行应包含 `upload ok / skill id / slug / url`
3. exit code 应为 0

**期望结果**:
- `skill id: <数字>`
- `slug: s39-...`
- `url: http://127.0.0.1:8767/skills/<slug>`
- exit code = 0

**实际结果**: ✅ **PASS**（Phase 3B 跑过，id=29, slug=s39-test-skill, url=`http://127.0.0.1:8767/skills/s39-test-skill`）

---

## AT-5: 错误码可读化 — 故意传不含 SKILL.md 的 zip → `[40002]`

**前置条件**:
- token 已缓存
- 构造一个不含 SKILL.md 的 zip

**操作步骤**:
1. 准备一个不合法 zip（如普通 txt 文件，绕开客户端 SKILL.md 校验）
2. `bash publish-skill.sh --zip /tmp/bad-skill.txt --category-id 24 --no-dry-run`
3. 观察 stderr 应打印 `[40002] 缺 SKILL.md 或无 YAML frontmatter（zip 根目录必须含 SKILL.md）`
4. exit code 应为 4

**期望结果**:
- 业务错（40002）→ 中文 message + 修复建议
- exit code = 4（客户端错）

**实际结果**: ✅ **PASS**（Phase 3B 跑过，错误码 + 退出码都对）

---

## AT-6: `bash auth-login.sh --status` — 显示 cached / username / host / token TTL

**前置条件**: 无（任意状态）

**操作步骤**:
1. 清缓存: `rm -f ~/.skills-manager/.token`
2. `bash auth-login.sh --status` → 应报 `no cached token`
3. 登录: `SKILLSMAP_USERNAME=admin SKILLSMAP_PASSWORD=admin123 bash auth-login.sh`
4. `bash auth-login.sh --status` → 应报 `cached: yes / username: admin / host: ... / expires: ... (Ns remaining)`
5. **不打印 token 内容**（只显示元信息）

**期望结果**:
- 未登录: `no cached token at <path>`
- 已登录: `cached: yes / username: admin / host: http://127.0.0.1:8767 / expires: <unix_ts> (<N>s remaining)`
- N ≈ 604800 (7 天)

**实际结果**: ✅ **PASS**（Phase 3B 跑过，604786s remaining ≈ 7 天）

---

## AT-7: `bash auth-login.sh --relogin` 强制刷新

**前置条件**: AT-6 已登录，缓存 token

**操作步骤**:
1. `bash auth-login.sh --relogin` → 提示输入 username/password（或读 env）
2. 缓存应被覆盖（不是叠加；不是 no-op）
3. 新 token 与旧 token 不同

**期望结果**:
- 强制走 login 流程（不读缓存）
- 缓存文件 mtime 更新
- 新 token 与旧 token JWT body 不同（iat/exp 都前进）

**实际结果**: TODO（Phase 4 跑）

---

## AT-8: 端到端完整路径

**前置条件**: 后端 dev 运行；客户端脚本就位；token 缓存可用

**操作步骤**（7 步）:
1. `bash auth-login.sh --status` → no cached
2. `bash auth-login.sh` 输入 admin/admin123（read -s 静默）
3. `bash auth-login.sh --status` → cached: yes
4. 准备 `/tmp/s39-e2e-skill/SKILL.md` 含 name + description
5. `bash publish-skill.sh --skill-dir /tmp/s39-e2e-skill`（自动引导分类、dry-run 显示计划、确认后真传）
6. `curl http://127.0.0.1:8767/api/skills/slug/{slug}` 验证公开可访问
7. 故意传 bad zip 看错误码可读化

**期望结果**: 7 步全过；最终 skill 在平台公开可访问；错误码中文 message 正确

**实际结果**: 见 Phase 4 报告

---

## AT-9: 前端兼容 — 已有 front-vue3 的 `/api/auth/me` 调用仍正常

**前置条件**:
- 前端 `front-vue3` 跑 dev
- S39 后端 DTO 改了响应字段（少 password，其他字段保留）
- 找前端代码里所有调 `/api/auth/me` 的位置

**操作步骤**:
1. `grep -rn '/api/auth/me' /d/codeing/workspace/skills-map/frontend/src/ 2>&1` 找消费者
2. 看每个消费者期望的字段（`data.id / data.username / data.role` 等）
3. 在 DTO `UserResponse` 里确认这些字段都保留
4. （如果前端有 store）跑一次 dev mode，登录，验证 store 字段填充正常

**期望结果**:
- DTO 字段：`id / username / email / displayName / avatar / role / status / createTime`（无 password）
- 前端原有期望的字段（`id / username / displayName / role` 等）全部保留
- 前端 store 反序列化无错

**实际结果**: TODO（需 grep 前端代码 + 跑前端 dev）

---

## 总览

| AT | 主题 | 状态 |
|----|------|------|
| AT-1 | me 端点无 password | ✅ PASS（Phase 3A） |
| AT-2 | 登录 + token 缓存 | ✅ PASS（Phase 3B） |
| AT-3 | 跨平台 zip | ✅ Windows PASS / Unix TODO（CI 节点） |
| AT-4 | 真传 publish | ✅ PASS（Phase 3B） |
| AT-5 | 错误码可读化 | ✅ PASS（Phase 3B） |
| AT-6 | --status 元信息 | ✅ PASS（Phase 3B） |
| AT-7 | --relogin 强制刷新 | TODO（Phase 4） |
| AT-8 | 端到端 7 步 | TODO（Phase 4） |
| AT-9 | 前端兼容 | TODO（grep + 验证） |

**当前通过率**: 6/9 ✅，3/9 TODO（Phase 4 跑完 → 8/9，Unix 节点 AT-3 → 9/9）
