-- Sprint S38: 用户上传 Skill 端点 — 增加上传者字段
-- 说明：
--   * 沿用 created_by_user_id 是为了兼容旧 admin 代上传场景（uploader=admin，creator=admin）
--   * uploader_user_id 用于快速查"我上传的 skill"列表（与 admin 代上传解耦语义）
--   * FK ON DELETE SET NULL: 用户被删时 uploader 字段置空，不级联删 skill

ALTER TABLE skill
    ADD COLUMN uploader_user_id BIGINT NULL COMMENT '用户上传端点写入；FK → user(id)';

-- 索引：用于"我上传的 skill"快速查询
CREATE INDEX idx_uploader_user_id ON skill(uploader_user_id);

-- FK 约束（prod MySQL 8.x 支持；H2 跳过）
-- 注意：IF NOT EXISTS 不能加在 constraint 上，依赖 ops-max 上线脚本保证幂等
-- ALTER TABLE skill
--     ADD CONSTRAINT fk_skill_uploader_user
--         FOREIGN KEY (uploader_user_id) REFERENCES user(id) ON DELETE SET NULL;
