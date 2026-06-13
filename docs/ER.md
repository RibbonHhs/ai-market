# SkillsMap ER 图

```mermaid
erDiagram
    CATEGORY ||--o{ SKILL : contains
    SKILL ||--o{ SKILL_TAG : has
    TAG ||--o{ SKILL_TAG : has
    SKILL ||--o{ REVIEW : receives
    SKILL ||--o{ FAVORITE : has
    USER ||--o{ REVIEW : writes
    USER ||--o{ FAVORITE : owns
    SKILL ||--o{ SKILL_RESOURCE : bundles

    CATEGORY {
        bigint id PK
        varchar name
        varchar slug UK
        varchar description
        varchar icon
        int sort_order
        int skill_count
        datetime create_time
        datetime update_time
        int deleted
    }

    SKILL {
        bigint id PK
        varchar name UK "kebab-case, 64"
        varchar slug UK
        varchar display_name
        varchar description "1024"
        longtext body "SKILL.md 正文"
        bigint category_id FK
        varchar tags "JSON"
        varchar license
        varchar allowed_tools
        varchar compatibility
        varchar metadata "JSON"
        varchar version "SemVer"
        varchar homepage
        varchar author_name
        varchar author_email
        varchar author_github
        varchar icon
        varchar source "official/community/private/imported"
        varchar install_command
        varchar download_url
        bigint package_size
        int stars
        int installs
        int views
        decimal rating_avg
        int rating_count
        varchar status "draft/published/deprecated/flagged"
        boolean featured
        datetime create_time
        datetime update_time
        int deleted
    }

    TAG {
        bigint id PK
        varchar name
        varchar slug UK
        int skill_count
        datetime create_time
        int deleted
    }

    SKILL_TAG {
        bigint skill_id FK
        bigint tag_id FK
    }

    USER {
        bigint id PK
        varchar username UK
        varchar password "BCrypt"
        varchar email
        varchar display_name
        varchar avatar
        varchar role "ADMIN/USER"
        int status "0禁用/1启用"
        datetime create_time
        datetime update_time
        int deleted
    }

    REVIEW {
        bigint id PK
        bigint skill_id FK
        bigint user_id FK
        int rating "1-5"
        varchar comment
        datetime create_time
        datetime update_time
        int deleted
    }

    FAVORITE {
        bigint id PK
        bigint user_id FK
        bigint skill_id FK
        datetime create_time
        int deleted
    }

    SKILL_RESOURCE {
        bigint id PK
        bigint skill_id FK
        varchar kind "script/reference/asset/agent/template/theme"
        varchar path
        bigint size
        varchar mime
    }
```

## 关键设计

1. **逻辑删除**：所有主表（除 SKILL_RESOURCE 外）都有 `deleted` 字段（0/1），由 MyBatis-Plus `@TableLogic` 自动过滤
2. **审计字段**：`create_time` / `update_time` 由 Service 显式设置
3. **唯一约束**：
   - `skill.name` / `skill.slug` 唯一
   - `user.username` 唯一
   - `tag.slug` 唯一
   - `review(skill_id, user_id)` 业务唯一（一用户一评）
   - `favorite(user_id, skill_id)` 唯一
4. **JSON 字段**：`tags` / `metadata` 用 JSON 字符串存（避免多表 JOIN）
5. **冗余统计**：`category.skill_count` / `tag.skill_count` / `skill.rating_avg/count` 冗余以加速查询
