package com.meiya.skillsmap.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Dev 环境 schema 同步器（@Profile("dev")，仅 H2 内存库生效）
 *
 * <p>背景：MyBatis-Plus 的"自动建表"只在表不存在时触发，对已有表不会
 * 自动 {@code ALTER}。Sprint S02 给 {@code skill} 表加了 9 个字段（Git 源管理），
 * dev H2 内存库在 JVM 间持久（{@code DB_CLOSE_DELAY=-1}），所以
 * 老 schema 表的列不会自动更新。本组件在每次启动时用
 * {@code ALTER TABLE ... ADD COLUMN IF NOT EXISTS}（H2 1.4.197+ 支持）补齐，
 * prod / local 走 MySQL 时由 {@code db/migration/} 飞 Flyway 脚本维护。
 */
@Component
@Profile("dev")
public class DevSchemaSync {

    private static final Logger log = LoggerFactory.getLogger(DevSchemaSync.class);

    /** Sprint S02 新增字段。顺序无关（H2 不依赖列序），缺哪个补哪个。 */
    private static final List<String> ALTER_STATEMENTS = List.of(
        "ALTER TABLE skill ADD COLUMN IF NOT EXISTS source_type      VARCHAR(16)   DEFAULT NULL",
        "ALTER TABLE skill ADD COLUMN IF NOT EXISTS source_url       VARCHAR(500)  DEFAULT NULL",
        "ALTER TABLE skill ADD COLUMN IF NOT EXISTS source_ref       VARCHAR(200)  DEFAULT NULL",
        "ALTER TABLE skill ADD COLUMN IF NOT EXISTS source_token_enc VARCHAR(1000) DEFAULT NULL",
        "ALTER TABLE skill ADD COLUMN IF NOT EXISTS token_hint       VARCHAR(32)   DEFAULT NULL",
        "ALTER TABLE skill ADD COLUMN IF NOT EXISTS last_sync_at     TIMESTAMP     DEFAULT NULL",
        "ALTER TABLE skill ADD COLUMN IF NOT EXISTS last_sync_status VARCHAR(16)   DEFAULT NULL",
        "ALTER TABLE skill ADD COLUMN IF NOT EXISTS last_sync_error  VARCHAR(500)  DEFAULT NULL",
        "ALTER TABLE skill ADD COLUMN IF NOT EXISTS last_commit_sha  VARCHAR(64)   DEFAULT NULL"
    );

    private final JdbcTemplate jdbc;

    public DevSchemaSync(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void syncSchema() {
        log.info("[dev-schema-sync] start — 校验 skill 表 {} 个新增字段", ALTER_STATEMENTS.size());
        int applied = 0, skipped = 0;
        for (String ddl : ALTER_STATEMENTS) {
            try {
                jdbc.execute(ddl);
                applied++;
            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().toUpperCase().contains("ALREADY EXISTS")) {
                    skipped++;
                } else {
                    log.warn("[dev-schema-sync] DDL 失败（{}）— {}（如果表都还没建，会在 seed 阶段创建）",
                            e.getClass().getSimpleName(), e.getMessage());
                }
            }
        }
        log.info("[dev-schema-sync] done — applied={}, skipped={}", applied, skipped);

        try {
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_skill_source_type  ON skill(source_type)");
            jdbc.execute("CREATE INDEX IF NOT EXISTS idx_skill_last_sync_at ON skill(last_sync_at)");
            log.info("[dev-schema-sync] 索引已就绪");
        } catch (Exception e) {
            log.warn("[dev-schema-sync] 索引创建失败: {}", e.getMessage());
        }
    }
}
