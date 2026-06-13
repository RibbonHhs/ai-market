# SkillsMap REST API 文档

> 完整 API 文档运行时访问：<http://localhost:8767/doc.html>

## 统一响应格式

```json
{ "code": 0, "message": "ok", "data": { /* 业务数据 */ } }
```

| 字段 | 说明 |
|---|---|
| `code` | 0 成功，其他见 BizCode |
| `message` | 文案 |
| `data` | 业务数据；分页接口为 `{ records, total, page, size }` |

## 业务码 BizCode

| code | 含义 |
|---|---|
| 0 | 成功 |
| 40000 | 请求参数错误 |
| 40100 | 未认证 |
| 40101 | Token 无效 |
| 40102 | Token 过期 |
| 40300 | 权限不足 |
| 40400 | 资源不存在 |
| 40900 | 资源冲突 |
| 500 | 系统异常 |
| 50001 | Skill 不存在 |
| 50003 | 用户不存在 |
| 50004 | 用户名已存在 |
| 50005 | 已评价过此 Skill |

## 1. 鉴权

| Method | Path | Auth | 说明 |
|---|---|---|---|
| POST | `/api/auth/register` | 公开 | 注册 |
| POST | `/api/auth/login` | 公开 | 登录，返回 `{token, userInfo, roles, expiresIn}` |
| GET | `/api/auth/me` | 需登录 | 当前用户信息 |
| POST | `/api/auth/logout` | 需登录 | 登出（前端清缓存即可） |

## 2. Skill 用户端

| Method | Path | Auth | 说明 |
|---|---|---|---|
| GET | `/api/skills` | 公开 | 列表（query: `keyword` `categoryId` `occupationCode` `usageCategoryId` `tagSlug` `source` `sort` `page` `size`） |
| GET | `/api/skills/{id}` | 公开 | 详情（自动 views+1） |
| GET | `/api/skills/slug/{slug}` | 公开 | 详情（按 slug） |
| GET | `/api/skills/hot` | 公开 | 热门（按 installs） |
| GET | `/api/skills/latest` | 公开 | 最新（按 create_time） |
| GET | `/api/skills/featured` | 公开 | 精选（featured=true） |
| GET | `/api/skills/slug/{slug}/download` | 公开 | 下载 .skill zip 流 |
| GET | `/api/skills/{id}/reviews` | 公开 | 评分列表 |

### 2.1 `GET /api/skills` 参数详解（S21 新增 occupationCode）

| 参数 | 必填 | 说明 | 示例 |
|---|---|---|---|
| `keyword` | 否 | 模糊匹配 name / displayName / description | `claude` |
| `categoryId` | 否 | SOC 分类 id；一级会自动展开到子分类 | `2` |
| `occupationCode` | **新增** | SOC 一级 code（如 `#01`）或子 code（如 `01-01`），按职业维度筛，会展开到匹配的所有 category | `#01` |
| `usageCategoryId` | 否 | USAGE 维度 id，精确匹配 | `5` |
| `tagSlug` | 否 | 标签 slug | `claude` |
| `source` | 否 | `official` / `official-bundled` / `community` | `official-bundled` |
| `sort` | 否 | `latest` / `hot` / `installs` / `rating` / `views` | `hot` |
| `page` | 否 | 页码，从 1 开始 | `1` |
| `size` | 否 | 每页条数（最大 100） | `20` |

**curl 示例**：

```bash
# 关键字搜索
curl "http://127.0.0.1:8767/api/skills?keyword=claude&size=5"

# 按 SOC 分类（一级展开）
curl "http://127.0.0.1:8767/api/skills?categoryId=2&size=10"

# 按职业维度（S21 新）
curl "http://127.0.0.1:8767/api/skills?occupationCode=%2301&size=10"

# 组合：关键字 + 职业 + 排序 + 分页
curl "http://127.0.0.1:8767/api/skills?keyword=code&occupationCode=01-01&sort=hot&page=1&size=20"
```

## 3. 分类 & 标签

| Method | Path | 说明 |
|---|---|---|
| GET | `/api/categories` | 全部分类（含 skill_count） |
| GET | `/api/tags` | 全部标签 |

## 4. 评分 & 收藏

| Method | Path | Auth | 说明 |
|---|---|---|---|
| POST | `/api/reviews` | 需登录 | 提交评分 `{skillId, rating, comment}` |
| GET | `/api/favorites/mine` | 需登录 | 我的收藏 |
| POST | `/api/favorites/{skillId}` | 需登录 | 添加/取消（toggle） |
| DELETE | `/api/favorites/{skillId}` | 需登录 | 取消 |
| GET | `/api/favorites/{skillId}/status` | 需登录 | 查询是否已收藏 |

## 5. 管理端（`/api/admin/**`，ROLE_ADMIN）

| Method | Path | 说明 |
|---|---|---|
| GET | `/api/admin/dashboard/source-stats` | 来源分布 |
| GET | `/api/admin/skills` | 后台列表（支持 `status` 过滤） |
| GET | `/api/admin/skills/{id}` | 后台查详情 |
| POST | `/api/admin/skills` | 新建 |
| PUT | `/api/admin/skills/{id}` | 更新 |
| DELETE | `/api/admin/skills/{id}` | 删除 |
| POST | `/api/admin/skills/{id}/publish` | 上架 |
| POST | `/api/admin/skills/{id}/unpublish` | 下架 |
| POST | `/api/admin/skills/import-from-local` | 从本地扫描导入 |
| POST | `/api/admin/skills/refresh-category-count` | 刷新分类计数 |
| GET / POST / PUT / DELETE | `/api/admin/categories` | 分类 CRUD |
| GET / DELETE | `/api/admin/tags` | 标签管理 |
| GET | `/api/admin/users` | 用户列表 |
| PUT | `/api/admin/users/{id}/role` | 修改角色 |
| PUT | `/api/admin/users/{id}/status` | 启/禁 |

## 6. 错误响应示例

```json
{
  "code": 40100,
  "message": "未认证",
  "data": null
}
```

## 7. 鉴权流程

1. 客户端 `POST /api/auth/login` → 拿到 `token`
2. 后续请求加 `Authorization: Bearer <token>` 头
3. 服务端 `JwtAuthFilter` 解析 → 注入 `SecurityContext` + `AuthContext` (ThreadLocal)
4. Controller 通过 `AuthContext.get()` 获取当前用户
