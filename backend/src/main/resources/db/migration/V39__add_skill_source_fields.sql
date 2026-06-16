-- S02: Git 源管理字段（dev profile 由 DevSchemaSync 在 @PostConstruct 时补；
--      prod / local 走 MySQL 时由本 migration 补齐，幂等）
-- 字段：
--   source_type / source_url / source_ref / source_token_enc / token_hint
--   last_sync_at / last_sync_status / last_sync_error / last_commit_sha
-- 索引：
--   idx_skill_source_type
--   idx_skill_last_sync_at
-- 背景：V1__init.sql 后续会包含这些字段（保持新建实例 schema 完整）；
--      本 migration 用 IF NOT EXISTS 兼容已部署但 V1 较老的实例。

-- 用 stored procedure 实现 MySQL 的 IF NOT EXISTS（MySQL 8 ALTER TABLE ADD COLUMN 不支持 IF NOT EXISTS）
DROP PROCEDURE IF EXISTS add_skill_source_columns;
CREATE PROCEDURE add_skill_source_columns()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'skill' AND COLUMN_NAME = 'source_type') THEN
        ALTER TABLE skill ADD COLUMN source_type      VARCHAR(16)   DEFAULT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'skill' AND COLUMN_NAME = 'source_url') THEN
        ALTER TABLE skill ADD COLUMN source_url       VARCHAR(500)  DEFAULT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'skill' AND COLUMN_NAME = 'source_ref') THEN
        ALTER TABLE skill ADD COLUMN source_ref       VARCHAR(200)  DEFAULT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'skill' AND COLUMN_NAME = 'source_token_enc') THEN
        ALTER TABLE skill ADD COLUMN source_token_enc VARCHAR(1000) DEFAULT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'skill' AND COLUMN_NAME = 'token_hint') THEN
        ALTER TABLE skill ADD COLUMN token_hint       VARCHAR(32)   DEFAULT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'skill' AND COLUMN_NAME = 'last_sync_at') THEN
        ALTER TABLE skill ADD COLUMN last_sync_at     TIMESTAMP     DEFAULT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'skill' AND COLUMN_NAME = 'last_sync_status') THEN
        ALTER TABLE skill ADD COLUMN last_sync_status VARCHAR(16)   DEFAULT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'skill' AND COLUMN_NAME = 'last_sync_error') THEN
        ALTER TABLE skill ADD COLUMN last_sync_error  VARCHAR(500)  DEFAULT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'skill' AND COLUMN_NAME = 'last_commit_sha') THEN
        ALTER TABLE skill ADD COLUMN last_commit_sha  VARCHAR(64)   DEFAULT NULL;
    END IF;
END;
CALL add_skill_source_columns();
DROP PROCEDURE add_skill_source_columns;

-- 索引（MySQL 8.0.16+ 支持 CREATE INDEX IF NOT EXISTS；为兼容老版本，存过程化）
DROP PROCEDURE IF EXISTS add_skill_source_indexes;
CREATE PROCEDURE add_skill_source_indexes()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'skill' AND INDEX_NAME = 'idx_skill_source_type') THEN
        CREATE INDEX idx_skill_source_type  ON skill(source_type);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'skill' AND INDEX_NAME = 'idx_skill_last_sync_at') THEN
        CREATE INDEX idx_skill_last_sync_at ON skill(last_sync_at);
    END IF;
END;
CALL add_skill_source_indexes();
DROP PROCEDURE add_skill_source_indexes;
