-- SkillsMap H2 schema (dev mode)
SET MODE MySQL;

CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128),
    slug VARCHAR(128),
    description VARCHAR(512),
    icon VARCHAR(512),
    sort_order INT,
    skill_count INT,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS skill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128),
    slug VARCHAR(128),
    display_name VARCHAR(256),
    description VARCHAR(2048),
    body CLOB,
    category_id BIGINT,
    tags VARCHAR(1024),
    license VARCHAR(64),
    allowed_tools VARCHAR(512),
    compatibility VARCHAR(512),
    metadata VARCHAR(2048),
    version VARCHAR(64),
    homepage VARCHAR(512),
    author_name VARCHAR(128),
    author_email VARCHAR(128),
    author_github VARCHAR(128),
    icon VARCHAR(512),
    source VARCHAR(32),
    install_command VARCHAR(512),
    download_url VARCHAR(512),
    package_size BIGINT,
    stars INT DEFAULT 0,
    installs INT DEFAULT 0,
    views INT DEFAULT 0,
    rating_avg DOUBLE,
    rating_count INT DEFAULT 0,
    status VARCHAR(32) DEFAULT 'published',
    featured BOOLEAN DEFAULT FALSE,
    created_by_user_id BIGINT,
    uploader_user_id BIGINT,  -- S38: 用户上传端点写入
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128),
    slug VARCHAR(128),
    skill_count INT,
    create_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS skill_tag (
    skill_id BIGINT,
    tag_id BIGINT
);

CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(128),
    password VARCHAR(256),
    email VARCHAR(128),
    display_name VARCHAR(128),
    avatar VARCHAR(512),
    role VARCHAR(32),
    status INT,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS review (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id BIGINT,
    user_id BIGINT,
    rating INT,
    comment VARCHAR(2048),
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    skill_id BIGINT,
    create_time TIMESTAMP,
    deleted INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS skill_resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id BIGINT,
    kind VARCHAR(32),
    path VARCHAR(512),
    size BIGINT,
    mime VARCHAR(128)
);

-- 索引
CREATE INDEX IF NOT EXISTS idx_skill_status ON skill(status);
CREATE INDEX IF NOT EXISTS idx_skill_category ON skill(category_id);
-- S18: USAGE 维度 FK
CREATE INDEX IF NOT EXISTS idx_skill_usage_category ON skill(usage_category_id);
CREATE INDEX IF NOT EXISTS idx_user_username ON user(username);
CREATE INDEX IF NOT EXISTS idx_review_skill ON review(skill_id);
CREATE INDEX IF NOT EXISTS idx_favorite_user ON favorite(user_id);
-- S38: 用户上传端点 — uploader_user_id 索引
CREATE INDEX IF NOT EXISTS idx_uploader_user_id ON skill(uploader_user_id);

-- ========== S04: SOC 职业分类扩展 ==========
ALTER TABLE category ADD COLUMN IF NOT EXISTS type      VARCHAR(16);
ALTER TABLE category ADD COLUMN IF NOT EXISTS code      VARCHAR(32);
ALTER TABLE category ADD COLUMN IF NOT EXISTS parent_id BIGINT;
-- S18: 用途维度 FK（nullable，可只挂 SOC 或只挂 USAGE）
ALTER TABLE skill  ADD COLUMN IF NOT EXISTS usage_category_id BIGINT;
CREATE INDEX IF NOT EXISTS idx_category_type      ON category(type);
CREATE INDEX IF NOT EXISTS idx_category_parent_id ON category(parent_id);
CREATE INDEX IF NOT EXISTS idx_category_code      ON category(code);
-- slug 唯一约束（防 301 跳转歧义，H2 用 CREATE UNIQUE INDEX 实现）
CREATE UNIQUE INDEX IF NOT EXISTS uk_category_slug ON category(slug);

-- 旧 slug → 新 slug 重定向（用于 301 跳转）
CREATE TABLE IF NOT EXISTS category_slug_redirect (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    old_slug    VARCHAR(128) NOT NULL,
    new_slug    VARCHAR(128) NOT NULL,
    create_time TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_redirect_old ON category_slug_redirect(old_slug);
CREATE INDEX IF NOT EXISTS idx_redirect_new ON category_slug_redirect(new_slug);
