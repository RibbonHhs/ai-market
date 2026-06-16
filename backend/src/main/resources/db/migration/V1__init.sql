-- SkillsMap initial schema (MySQL 8.x)
-- 合并了原 H2 schema-h2.sql 全部内容：
--   * 主表 CREATE TABLE
--   * 索引
--   * S04 SOC 职业分类扩展
--   * S18 用途维度 FK
--   * 旧 slug -> 新 slug 重定向表
-- H2 差异点：BOOLEAN -> TINYINT(1)；CLOB -> LONGTEXT；删除 SET MODE MySQL

-- ========== category ==========
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128),
    slug VARCHAR(128),
    description VARCHAR(512),
    icon VARCHAR(512),
    sort_order INT,
    skill_count INT,
    type VARCHAR(16),
    code VARCHAR(32),
    parent_id BIGINT,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);
CREATE INDEX idx_category_type      ON category(type);
CREATE INDEX idx_category_parent_id ON category(parent_id);
CREATE INDEX idx_category_code      ON category(code);
CREATE UNIQUE INDEX uk_category_slug ON category(slug);

-- ========== skill ==========
CREATE TABLE IF NOT EXISTS skill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128),
    slug VARCHAR(128),
    display_name VARCHAR(256),
    description VARCHAR(2048),
    body LONGTEXT,
    category_id BIGINT,
    usage_category_id BIGINT,
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
    featured TINYINT(1) DEFAULT 0,
    -- S02: Git 源管理字段（原 DevSchemaSync 在 dev profile 加；local/prod 走 Flyway 由 V1 直接建）
    source_type      VARCHAR(16)   DEFAULT NULL,
    source_url       VARCHAR(500)  DEFAULT NULL,
    source_ref       VARCHAR(200)  DEFAULT NULL,
    source_token_enc VARCHAR(1000) DEFAULT NULL,
    token_hint       VARCHAR(32)   DEFAULT NULL,
    last_sync_at     TIMESTAMP     DEFAULT NULL,
    last_sync_status VARCHAR(16)   DEFAULT NULL,
    last_sync_error  VARCHAR(500)  DEFAULT NULL,
    last_commit_sha  VARCHAR(64)   DEFAULT NULL,
    -- S38: 用户上传端点
    created_by_user_id BIGINT,
    uploader_user_id   BIGINT COMMENT 'S38: 用户上传端点写入',
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    deleted INT DEFAULT 0
);
CREATE INDEX idx_skill_status          ON skill(status);
CREATE INDEX idx_skill_category        ON skill(category_id);
CREATE INDEX idx_skill_usage_category  ON skill(usage_category_id);
CREATE INDEX idx_uploader_user_id      ON skill(uploader_user_id);

-- ========== tag ==========
CREATE TABLE IF NOT EXISTS tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128),
    slug VARCHAR(128),
    skill_count INT,
    create_time TIMESTAMP,
    deleted INT DEFAULT 0
);

-- ========== skill_tag ==========
CREATE TABLE IF NOT EXISTS skill_tag (
    skill_id BIGINT,
    tag_id BIGINT
);

-- ========== user ==========
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
CREATE INDEX idx_user_username ON user(username);

-- ========== review ==========
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
CREATE INDEX idx_review_skill ON review(skill_id);

-- ========== favorite ==========
CREATE TABLE IF NOT EXISTS favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    skill_id BIGINT,
    create_time TIMESTAMP,
    deleted INT DEFAULT 0
);
CREATE INDEX idx_favorite_user ON favorite(user_id);

-- ========== skill_resource ==========
CREATE TABLE IF NOT EXISTS skill_resource (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    skill_id BIGINT,
    kind VARCHAR(32),
    path VARCHAR(512),
    size BIGINT,
    mime VARCHAR(128)
);

-- ========== category_slug_redirect ==========
CREATE TABLE IF NOT EXISTS category_slug_redirect (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    old_slug VARCHAR(128) NOT NULL,
    new_slug VARCHAR(128) NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE UNIQUE INDEX uk_redirect_old ON category_slug_redirect(old_slug);
CREATE INDEX idx_redirect_new ON category_slug_redirect(new_slug);
