package com.meiya.skillsmap.skill.upload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * S38: 上传临时目录孤儿清扫（PRD §4.1 §12）。
 *
 * <p>每 1 小时扫描一次 {@code ${skillsmap.upload.tmp-dir}}，删除修改时间 > 1 小时的子目录。
 * 上传请求自身通过 {@code finally} 清理，但进程崩溃/容器 kill -9 等异常路径可能留下孤儿。
 *
 * <p>日志：INFO 记录「清理 N 个孤儿目录，释放 M MB」（PRD §5.3 部署可观测性）。
 */
@Component
public class UploadTmpCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(UploadTmpCleanupScheduler.class);

    /** 孤儿阈值：mtime 超过此值的目录视为孤儿 */
    private static final Duration ORPHAN_THRESHOLD = Duration.ofHours(1);

    @Value("${skillsmap.upload.tmp-dir:#{T(java.lang.System).getProperty('java.io.tmpdir') + '/skill-upload'}}")
    private String tmpDir;

    /**
     * 每小时跑一次；启动后 5 分钟首跑。
     * <p>fixedRate = 3600000ms；initialDelay = 300000ms。
     */
    @Scheduled(fixedRate = 3600_000L, initialDelay = 300_000L)
    public void cleanupOrphanTmpDirs() {
        Path base;
        try {
            base = Paths.get(tmpDir).toAbsolutePath().normalize();
            if (!Files.isDirectory(base)) {
                log.debug("[upload-cleanup] tmp dir not exist, skip: {}", base);
                return;
            }
        } catch (Exception e) {
            log.warn("[upload-cleanup] resolve tmp dir failed: {}", e.getMessage());
            return;
        }

        int removed = 0;
        long freedBytes = 0;
        long now = System.currentTimeMillis();
        long thresholdMs = ORPHAN_THRESHOLD.toMillis();

        try (Stream<Path> children = Files.list(base)) {
            for (Path child : (Iterable<Path>) children::iterator) {
                if (!Files.isDirectory(child)) continue;
                try {
                    BasicFileAttributes attrs = Files.readAttributes(child, BasicFileAttributes.class);
                    FileTime mtime = attrs.lastModifiedTime();
                    long ageMs = now - mtime.toMillis();
                    if (ageMs < thresholdMs) continue;

                    long sizeBefore = dirSizeBytes(child);
                    deleteRecursively(child);
                    removed++;
                    freedBytes += sizeBefore;
                    log.debug("[upload-cleanup] removed orphan: {} (age={}m, size={}KB)",
                            child.getFileName(), ageMs / 60_000L, sizeBefore / 1024L);
                } catch (Exception e) {
                    log.warn("[upload-cleanup] failed to remove {}: {}", child, e.getMessage());
                }
            }
        } catch (IOException e) {
            log.warn("[upload-cleanup] list tmp dir failed: {}", e.getMessage());
            return;
        }

        if (removed > 0) {
            log.info("[upload-cleanup] 清理 {} 个孤儿目录，释放 {} MB",
                    removed, freedBytes / (1024L * 1024L));
        } else {
            log.debug("[upload-cleanup] 无孤儿目录（扫描 {}）", base);
        }
    }

    /** 递归计算目录总字节数 */
    private long dirSizeBytes(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try { return Files.size(p); } catch (IOException e) { return 0L; }
                    }).sum();
        } catch (IOException e) {
            return 0L;
        }
    }

    /** 递归删除目录（best-effort） */
    private void deleteRecursively(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {
            // best-effort
        }
    }
}
