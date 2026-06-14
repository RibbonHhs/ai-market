# S39 Design — skills-manager v2 SKILL.md + scripts

> **承接**: S39 PRD `prd-skills-manager-v2.md`  
> **范围**: 客户端 skill 升级（US-2 / US-3 / US-4）  
> **设计原则**: 安全第一（密码不落盘不 echo）+ 跨平台（Windows/macOS/Linux）+ 单轮对话完成（自动 zip + 分类引导 + 错误码可读）

---

## 1. 整体架构

### 1.1 客户端 skill 文件结构（v2.0）

```
skills-manager/
├── SKILL.md                      # v2.0 重写
├── references/
│   └── api-endpoints.md          # 更新 § 6 Token 流程 + 新增 categories 端点
└── scripts/
    ├── auth-common.sh            # 新增：token 缓存 + 错误码映射共享函数
    ├── auth-login.sh             # 新增：username/password 登录拿 token
    ├── publish-skill.sh          # 重写：自动 zip + dry-run + 分类引导
    ├── auth-example.sh           # 重写：调 auth-common.sh 自动拿 token
    └── sync-skills.sh            # 不动
```

### 1.2 关键流程图

#### 登录流程（US-2）

```
用户: "帮我发布这个 skill"
  ↓
AI: 加载 skills-manager skill
  ↓
AI 调 bash auth-login.sh
  ↓
auth-login.sh 检测 ~/.skills-manager/.token
  ├─ 存在 + 未过期 → 直接用
  └─ 不存在/过期/--relogin → read -s 提示输入 username/password
                                ↓
                          curl POST /api/auth/login
                                ├─ 200 → 缓存到 .token (0600)
                                └─ 40100 → 提示"用户名或密码错误"，exit 4
  ↓
auth-login.sh 输出 TOKEN（stdout, 不进日志）
  ↓
AI 拿 TOKEN 调 publish-skill.sh
```

#### 发布流程（US-3）

```
AI: bash publish-skill.sh --skill-dir ./api-and-interface-design
  ↓
publish-skill.sh:
  1. 调 auth-common.sh 拿 token
  2. 检测 OS：
     - Windows → PowerShell ZipFile.CreateFromDirectory
     - macOS/Linux → zip 命令
  3. staging 到 /tmp/skill-pack-{uuid}/，cp SKILL.md 进去
  4. 打包 → {tmp}/xxx.zip
  5. dry-run：打印"将上传 X bytes, category 引导列表..."
  6. AI 与用户确认（除非 --no-dry-run）
  7. 调 POST /api/skills (multipart)
  8. 解析 code → message 映射 → 输出可读错误
  9. 清理 staging
```

---

## 2. 关键脚本设计

### 2.1 `auth-common.sh`（新增）

**职责**: token 缓存读写 + 错误码映射共享

```bash
# 关键函数
load_or_login_token() { ... }   # 检测 .token 有效就返回，否则自动调 auth-login
save_token() { ... }            # 写 .token (0600 权限)
clear_token() { ... }           # 删 .token
expired_token() { ... }         # 检查 expiresAt

# 错误码映射（与后端 BizCode 同步）
declare -A ERROR_MAP=(
  [40001]="文件缺失/非 zip/zip 损坏"
  [40002]="缺 SKILL.md / 无 frontmatter"
  [40003]="frontmatter 缺 name/description"
  [40004]="zip bomb（解压后 > 50MB 或条目过多）"
  [40100]="未认证（缺/无效 token）"
  [40101]="Token 无效"
  [40102]="Token 已过期（请重新登录）"
  [40300]="权限不足"
  [40900]="slug 已存在（建议改名后重试）"
  [41300]="文件 > 10MB"
  [42900]="请求过于频繁"
  [50001]="Skill 不存在 / 解压失败"
  [50008]="文件解析失败"
)
```

**Token 文件格式** (JSON, ~/.skills-manager/.token):
```json
{
  "username": "ribbon",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresAt": 1782000000,
  "host": "http://127.0.0.1:8767"
}
```

**安全要点**:
- 文件权限 `chmod 600`
- token 不 echo 到 stderr（仅 stdout）
- `trap 'unset PASSWORD; rm -f /tmp/.staging-pwd-*' EXIT`

### 2.2 `auth-login.sh`（新增）

**调用**:
```bash
# 交互式
bash auth-login.sh

# 显式重登
bash auth-login.sh --relogin

# 登出
bash auth-login.sh --logout

# 显式用户名（密码仍 prompt）
bash auth-login.sh --username ribbon

# CI 场景（env 传密码）
SKILLSMAP_USERNAME=ribbon SKILLSMAP_PASSWORD=xxx bash auth-login.sh
```

**流程**:
```bash
# 1. 解析参数
LOGIN_MODE="auto"   # auto | relogin | logout
while [[ $# -gt 0 ]]; do
  case $1 in
    --relogin) LOGIN_MODE="relogin"; shift ;;
    --logout)  LOGIN_MODE="logout"; shift ;;
    --username) USERNAME="$2"; shift 2 ;;
    *) shift ;;
  esac
done

# 2. --logout 分支
if [[ "$LOGIN_MODE" == "logout" ]]; then
  clear_token
  echo "✅ 已登出"
  exit 0
fi

# 3. 检测缓存
if [[ "$LOGIN_MODE" == "auto" ]] && ! expired_token; then
  TOKEN=$(cat ~/.skills-manager/.token | jq -r .token)
  echo "$TOKEN"   # stdout 供其他脚本用
  exit 0
fi

# 4. 提示输入
if [[ -z "$USERNAME" ]]; then
  read -rp "Username: " USERNAME
fi
if [[ -z "$SKILLSMAP_PASSWORD" ]]; then
  read -rsp "Password: " PASSWORD
  echo
fi
trap 'unset PASSWORD' EXIT

# 5. 调 login
RESP=$(curl -fsS -X POST "$HOST/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"$USERNAME\",\"password\":\"$PASSWORD\"}")
unset PASSWORD

# 6. 解析 + 缓存
CODE=$(echo "$RESP" | jq -r .code)
if [[ "$CODE" != "0" ]]; then
  MSG=$(echo "$RESP" | jq -r .message)
  echo "❌ 登录失败: $MSG" >&2
  exit 4   # 认证错
fi

TOKEN=$(echo "$RESP" | jq -r .data.token)
EXPIRES_IN=$(echo "$RESP" | jq -r .data.expiresIn)
EXPIRES_AT=$(($(date +%s) + EXPIRES_IN))

# 7. 写缓存（0600）
mkdir -p ~/.skills-manager
echo "{\"username\":\"$USERNAME\",\"token\":\"$TOKEN\",\"expiresAt\":$EXPIRES_AT,\"host\":\"$HOST\"}" \
  > ~/.skills-manager/.token
chmod 600 ~/.skills-manager/.token

echo "$TOKEN"
```

### 2.3 `publish-skill.sh`（重写）

**关键变化**:
- 接收 `SKILL_DIR`（目录）而非 `SKILL_ZIP`（文件）
- 自动 zip 打包（跨平台）
- Dry-run 默认开启
- 分类引导（自动调 GET /api/categories）
- 错误码可读化
- 退出码细分

**调用**:
```bash
# 推荐：传目录，自动打包
bash publish-skill.sh --skill-dir ./api-and-interface-design

# 兼容旧：传已打包的 zip
bash publish-skill.sh --zip ./my-skill.zip

# 跳过 dry-run（CI）
bash publish-skill.sh --skill-dir ./xxx --no-dry-run

# 显式分类
bash publish-skill.sh --skill-dir ./xxx --category-id 24

# 显式 USAGE / tag
bash publish-skill.sh --skill-dir ./xxx --usage-ids 1,2 --tags claude,agent
```

**核心逻辑**:
```bash
# 1. 参数解析
SKILL_DIR=""
ZIP_PATH=""
CATEGORY_ID=""
USAGE_IDS=""
TAG_SLUGS=""
DRY_RUN=1   # 默认开启
HOST="http://127.0.0.1:8767"

while [[ $# -gt 0 ]]; do
  case $1 in
    --skill-dir) SKILL_DIR="$2"; shift 2 ;;
    --zip)       ZIP_PATH="$2"; shift 2 ;;
    --category-id) CATEGORY_ID="$2"; shift 2 ;;
    --usage-ids) USAGE_IDS="$2"; shift 2 ;;
    --tags) TAG_SLUGS="$2"; shift 2 ;;
    --no-dry-run) DRY_RUN=0; shift ;;
    --host) HOST="$2"; shift 2 ;;
    *) shift ;;
  esac
done

# 2. 拿 token
source auth-common.sh
TOKEN=$(load_or_login_token) || exit $?

# 3. 若传 SKILL_DIR → 自动打包
if [[ -n "$SKILL_DIR" ]]; then
  STAGING=$(mktemp -d)
  cp "$SKILL_DIR/SKILL.md" "$STAGING/"   # 关键：根目录直接含 SKILL.md
  # 复制其他资源
  find "$SKILL_DIR" -mindepth 1 -not -name SKILL.md -exec cp -r {} "$STAGING/" \;

  ZIP_PATH=$(mktemp --suffix=.zip)
  if is_windows; then
    powershell -NoProfile -Command "Compress-Archive -Path '$STAGING\*' -DestinationPath '$ZIP_PATH' -Force"
  else
    (cd "$STAGING" && zip -r "$ZIP_PATH" .)
  fi
fi

# 4. 校验文件
validate_zip "$ZIP_PATH" || exit 1

# 5. 分类引导
if [[ -z "$CATEGORY_ID" ]]; then
  CATEGORY_ID=$(prompt_category) || exit 1
fi

# 6. Dry-run
if [[ $DRY_RUN -eq 1 ]]; then
  echo "📋 Dry-run 模式："
  echo "   zip:    $ZIP_PATH ($(stat_size $ZIP_PATH) bytes)"
  echo "   host:   $HOST"
  echo "   cat:    $CATEGORY_ID ($(cat_name $HOST $CATEGORY_ID))"
  echo "   usage:  ${USAGE_IDS:-<none>}"
  echo "   tags:   ${TAG_SLUGS:-<none>}"
  read -rp "确认上传？[y/N] " ans
  [[ "$ans" =~ ^[Yy]$ ]] || { echo "已取消"; exit 0; }
fi

# 7. 调 API
RESP=$(curl -fsS -X POST "$HOST/api/skills" \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@${ZIP_PATH}" \
  -F "categoryId=${CATEGORY_ID}" \
  ${USAGE_IDS:+-F "usageCategoryIds=$(echo $USAGE_IDS | cut -d, -f1)"} \
  ...)

# 8. 错误码映射
CODE=$(echo "$RESP" | jq -r .code)
if [[ "$CODE" != "0" ]]; then
  MSG=${ERROR_MAP[$CODE]:-$(echo "$RESP" | jq -r .message)}
  echo "❌ [${CODE}] ${MSG}" >&2
  case $CODE in
    40100|40101|40102) exit 3 ;;  # 认证错
    40001|40002|40003|40004|41300) exit 4 ;;  # 客户端错
    40900) exit 6 ;;  # 冲突
    5*) exit 5 ;;  # 服务端错
    *) exit 1 ;;
  esac
fi

# 9. 成功
SLUG=$(echo "$RESP" | jq -r .data.slug)
ID=$(echo "$RESP" | jq -r .data.id)
echo "✅ 上传成功！"
echo "   skill id: $ID"
echo "   slug:     $SLUG"
echo "   url:      $HOST/skills/$SLUG"

# 10. 清理
rm -rf "$STAGING" "$ZIP_PATH"
```

### 2.4 分类引导实现细节

```bash
prompt_category() {
  # 1. 调 GET /api/categories
  TREE=$(curl -fsS "$HOST/api/categories")

  # 2. 渲染为带编号的扁平列表（保留层级缩进）
  echo "请选择 SOC 分类（输入编号或关键字搜索）："
  echo "$TREE" | jq -r '.data[] | .code as $code | .children[]? | "\(.id) [\($code)] \(.name)"' \
    | nl -ba | head -50

  # 3. 等待输入
  read -rp "编号: " ans
  echo "$ans"
}
```

**简化版**（第一版不做搜索，先满足基本可用）:
- 直接列出 50 条一级 + 热门二级
- 关键字搜索 v2.1 再做

---

## 3. SKILL.md v2.0 草稿

```markdown
---
name: skills-manager
description: 管理与更新 SkillsMap 平台上的公开 Skills。提供列表、搜索、详情、下载、同步、登录、发布等操作。基于 SkillsMap 公开 REST API。
license: MIT
allowed-tools: Bash, Read, Write
compatibility: claude-code, cursor, codex
metadata:
  version: 2.0.0
  author: SkillsMap Team
  homepage: https://github.com/RibbonHhs/skills
  tags:
    - skills
    - management
    - api
    - claude
---

# Skills Manager v2

SkillsMap 平台官方客户端 skill。让你直接在 Claude Code 对话中**用平台账号登录**、
**浏览/搜索/下载/上传** Skills。

## 可执行操作

| 操作 | 何时用 | 一句话 |
|------|--------|--------|
| `list` | "列出 skills" | `GET /api/skills` 支持 keyword/categoryId/usageCategoryId/tagSlug/sort 筛选 |
| `search` | "搜包含 XXX 的 skill" | 等同 list + keyword |
| `detail` | "看 skill 详情" | `GET /api/skills/slug/{slug}` |
| `download` | "下载 .skill zip" | `GET /api/skills/slug/{slug}/download` |
| `sync` | "拉全量 skill 清单" | `bash scripts/sync-skills.sh` |
| `login` | "用账号登录拿 token" | `bash scripts/auth-login.sh`（token 缓存到 `~/.skills-manager/.token`）|
| `logout` | "清缓存 token" | `bash scripts/auth-login.sh --logout` |
| `publish` | "上传 .skill zip" | `bash scripts/publish-skill.sh --skill-dir ./xxx`（自动打包 + 分类引导 + dry-run） |

## 基础 URL

- 本地开发：`http://127.0.0.1:8767`（默认）
- 远程实例：见部署方提供的主机地址

公开端点无需鉴权，受保护端点需 `Authorization: Bearer <token>`。
端点清单见 `references/api-endpoints.md`。

## 典型用法

### 浏览 skills

```
用户: 列出 SkillsMap 上的热门 skills
→ 调 GET /api/skills/hot?limit=12
→ 渲染 displayName/description/installCommand
```

### 登录拿 token（v2 新增）

```
用户: 用我的账号登录 SkillsMap
→ bash scripts/auth-login.sh
→ 提示输入 username + password（read -s 静默）
→ 调 POST /api/auth/login → 缓存 token 到 ~/.skills-manager/.token (0600)
→ 后续命令自动复用
```

### 发布 skill（v2 重写）

```
用户: 帮我把这个目录发布到 SkillsMap
→ bash scripts/publish-skill.sh --skill-dir ./my-skill
→ 自动 zip 打包（跨平台）
→ dry-run 显示计划
→ 分类引导（编号选择）
→ 调 POST /api/skills
→ 输出 id/slug/url
```

## 安全须知

- **密码用 `read -s` 静默输入**，绝不 echo 到屏幕/日志/commit
- Token 缓存到 `~/.skills-manager/.token` 权限 `0600`，**不写入 skill 目录**
- 缓存有效期由后端 JWT expiration 决定（默认 7 天）；过期自动触发 re-login
- 撤销 token：`bash scripts/auth-login.sh --logout`

## 端点速查

完整文档见 `references/api-endpoints.md` § 1-7。

## 退出码（publish-skill.sh）

| code | 含义 |
|------|------|
| 0 | 成功 |
| 1 | 参数错 |
| 2 | 网络错 |
| 3 | 认证错（40100/40101/40102） |
| 4 | 客户端错（40001-40004/41300/zip 非法） |
| 5 | 服务端错（5xxx） |
| 6 | 冲突（40900，slug 已存在） |
