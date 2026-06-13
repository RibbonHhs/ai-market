package com.meiya.skillsmap.seed;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiya.skillsmap.config.GitSourceProperties;
import com.meiya.skillsmap.entity.Skill;
import com.meiya.skillsmap.mapper.SkillMapper;
import com.meiya.skillsmap.service.SkillGitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Git 源 Skill 定时同步器（Sprint S02）
 * <p>默认每 60 分钟扫描一次 {@code last_sync_at} 超过阈值的 skill，逐个 sync。
 * <p>关闭方式：{@code skillsmap.git-source.scheduler-enabled=false}
 * <p>并发由 {@link GitSourceProperties#getMaxConcurrent()} 限制。
 */
@Component
@ConditionalOnProperty(prefix = "skillsmap.git-source", name = "scheduler-enabled",
        havingValue = "true", matchIfMissing = true)
public class SkillGitScheduler {

    private static final Logger log = LoggerFactory.getLogger(SkillGitScheduler.class);

    private final GitSourceProperties cfg;
    private final SkillMapper skillMapper;
    private final SkillGitService gitService;

    public SkillGitScheduler(GitSourceProperties cfg,
                             SkillMapper skillMapper,
                             SkillGitService gitService) {
        this.cfg = cfg;
        this.skillMapper = skillMapper;
        this.gitService = gitService;
    }

    @Scheduled(cron = "${skillsmap.git-source.sync-cron:0 0 */1 * * *}")
    public void syncStaleSkills() {
        if (!cfg.isEnabled()) {
            log.debug("[git-source-scheduler] disabled globally, skip");
            return;
        }
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(cfg.getStaleThresholdMinutes());
        List<Skill> stale = skillMapper.selectList(new LambdaQueryWrapper<Skill>()
                .eq(Skill::getSourceType, "GIT_URL")
                .and(w -> w.isNull(Skill::getLastSyncAt)
                        .or().lt(Skill::getLastSyncAt, threshold)));
        if (stale.isEmpty()) {
            log.debug("[git-source-scheduler] no stale GIT_URL skill to sync");
            return;
        }
        log.info("[git-source-scheduler] scanning {} stale GIT_URL skill(s)", stale.size());

        AtomicInteger ok = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        for (Skill s : stale) {
            try {
                gitService.syncSkill(s.getId());
                ok.incrementAndGet();
            } catch (Exception e) {
                fail.incrementAndGet();
                log.warn("[git-source-scheduler] sync id={} failed: {}", s.getId(), e.getMessage());
            }
        }
        log.info("[git-source-scheduler] done. success={} failed={}", ok.get(), fail.get());
    }
}
