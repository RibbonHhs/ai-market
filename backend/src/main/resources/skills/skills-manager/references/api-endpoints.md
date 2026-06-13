# SkillsMap 公开 API — 核心端点速查

> 完整 Knife4j 文档：`<host>/doc.html`  
> OpenAPI JSON：`<host>/v3/api-docs`

所有端点无需鉴权（除标注的）。  
`host` 默认 `http://127.0.0.1:8767`，远程部署时替换为实际地址。

---

## 1. 列表（搜索 / 筛选 / 分页）

`GET /api/skills`

| 参数 | 说明 | 示例 |
|------|------|------|
| `keyword` | 关键字模糊匹配 name/displayName/description | `claude` |
| `categoryId` | SOC 分类 id（一级会展开到子分类） | `2` |
| `occupationCode` | SOC 一级 code `#01` / 子 code `01-01` | `#01` |
| `usageCategoryId` | USAGE 维度 id | `5` |
| `tagSlug` | 标签 slug | `claude` |
| `source` | 来源过滤 | `official` / `official-bundled` |
| `sort` | `latest` / `hot` / `installs` / `rating` / `views` | `hot` |
| `page` | 页码（从 1 开始） | `1` |
| `size` | 每页条数（最大 100） | `20` |

### 示例

```bash
# 关键字搜索
curl "http://127.0.0.1:8767/api/skills?keyword=claude&size=5"

# 按 SOC 分类筛
curl "http://127.0.0.1:8767/api/skills?categoryId=2&size=10"

# 按职业维度筛（SOC 一级 = #01 计算机与数学）
curl "http://127.0.0.1:8767/api/skills?occupationCode=%2301&size=10"

# 组合筛：关键字 + 职业 + 排序
curl "http://127.0.0.1:8767/api/skills?keyword=code&occupationCode=01-01&sort=hot&size=20"
```

---

## 2. 详情

| 端点 | 用途 |
|------|------|
| `GET /api/skills/{id}` | 按 ID 查详情（自动 views+1） |
| `GET /api/skills/slug/{slug}` | 按 slug 查详情（推荐） |

```bash
curl http://127.0.0.1:8767/api/skills/slug/skills-manager
```

---

## 3. 热门 / 最新 / 精选

```bash
curl http://127.0.0.1:8767/api/skills/hot?limit=12
curl http://127.0.0.1:8767/api/skills/latest?limit=12
curl http://127.0.0.1:8767/api/skills/featured?limit=6
```

`/hot` 还支持 `sort=hot|recent|featured`。

---

## 4. 下载 Skill 包

```bash
# 按 slug 下载
curl -OJ http://127.0.0.1:8767/api/skills/slug/skills-manager/download

# 按 ID 下载
curl -OJ http://127.0.0.1:8767/api/skills/42/download
```

返回 `.zip` 流，content-type `application/octet-stream`。

---

## 5. 统一响应

```json
{ "code": 0, "message": "ok", "data": { /* 业务数据 */ } }
```

分页接口 `data` 形如：

```json
{ "records": [ ... ], "total": 123, "page": 1, "size": 20 }
```

---

## 错误码

| code | 含义 |
|------|------|
| 0 | 成功 |
| 40000 | 请求参数错误 |
| 40100 | 未认证（缺失 / 无效 Token） |
| 40300 | 权限不足 |
| 40400 | 资源不存在 |
| 42900 | 限流（公开 API 按 IP 限速，S23） |
| 50001 | Skill 不存在 |

---

## 6. 需鉴权端点（S23 新增）

以下端点需 `Authorization: Bearer <token>`，未带或无效 → 401。

| 端点 | 方法 | 用途 | 请求体 / 参数 |
|------|------|------|--------------|
| `/api/auth/me` | GET | 取当前用户信息 | — |
| `/api/reviews` | POST | 给 skill 评分 | `{skillId, rating, comment}` |
| `/api/favorites/{skillId}` | POST | 收藏 skill | — |
| `/api/favorites/{skillId}/status` | GET | 是否已收藏 | — |
| `/api/favorites/mine` | GET | 我的收藏列表 | `?page&size` |

### Token 获取流程

1. 用户在 SkillsMap 网页登录
2. 浏览器开发者工具 → Application → Local Storage → `auth_token`
3. 或登录后调 `GET /api/auth/me`，响应 `data.token`

### 示例

```bash
export TOKEN="<从浏览器 LocalStorage 复制>"

# 评分
curl -fsS -X POST "http://127.0.0.1:8767/api/reviews" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"skillId":1,"rating":5,"comment":"好"}'

# 收藏
curl -fsS -X POST "http://127.0.0.1:8767/api/favorites/1" \
  -H "Authorization: Bearer $TOKEN"

# 查我的收藏
curl -fsS "http://127.0.0.1:8767/api/favorites/mine" \
  -H "Authorization: Bearer $TOKEN"
```

### 智能体使用约定

- skill **不会代用户登录**；需用户主动提供 token
- token 视为用户凭据；不要 echo、不要写入 commit、不要持久化到 skill 目录
- 若用户拒绝提供 token，告知"只读浏览"模式可用
