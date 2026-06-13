package com.meiya.skillsmap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Git 源 Skill 配置（Sprint S02）。
 * <p>与 {@link StorageProperties.Git}（用于本地 zip 流程推送主仓）不同，
 * 本配置用于「通过 Git URL 上传」流程：从任意远端拉取、解析、入库。
 *
 * <p>设计目标：
 * <ul>
 *   <li>浅克隆（depth=1），减小仓库体积</li>
 *   <li>独立工作目录 {@code data/skill-clones/}，<b>不与主仓同目录</b></li>
 *   <li>后台定时同步（@Scheduled）开关可控</li>
 *   <li>并发上限避免网络/磁盘打满</li>
 * </ul>
 */
@Configuration
@ConfigurationProperties(prefix = "skillsmap.git-source")
public class GitSourceProperties {

    /** 是否启用 Git 源 Skill 功能 */
    private boolean enabled = true;

    /** 浅克隆 depth */
    private int depth = 1;

    /** 独立缓存目录（相对工作目录） */
    private String workdir = "./data/skill-clones";

    /** 远程 transport 超时（毫秒） */
    private int timeoutMs = 30_000;

    /** clone 后允许的最大单文件大小（字节），超过则跳过该文件 */
    private long maxFileSize = 50L * 1024L * 1024L;

    /** clone 完成后允许的总大小上限（字节），超过则拒绝 */
    private long maxTotalSize = 500L * 1024L * 1024L;

    /** 定时同步开关 */
    private boolean schedulerEnabled = true;

    /** 定时任务 cron 表达式（默认每 60 分钟） */
    private String syncCron = "0 0 */1 * * *";

    /** 多久未同步视为「过期」（分钟） */
    private int staleThresholdMinutes = 60;

    /** 同时进行的 clone / pull 最大并发数 */
    private int maxConcurrent = 3;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }
    public String getWorkdir() { return workdir; }
    public void setWorkdir(String workdir) { this.workdir = workdir; }
    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(int timeoutMs) { this.timeoutMs = timeoutMs; }
    public long getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }
    public long getMaxTotalSize() { return maxTotalSize; }
    public void setMaxTotalSize(long maxTotalSize) { this.maxTotalSize = maxTotalSize; }
    public boolean isSchedulerEnabled() { return schedulerEnabled; }
    public void setSchedulerEnabled(boolean schedulerEnabled) { this.schedulerEnabled = schedulerEnabled; }
    public String getSyncCron() { return syncCron; }
    public void setSyncCron(String syncCron) { this.syncCron = syncCron; }
    public int getStaleThresholdMinutes() { return staleThresholdMinutes; }
    public void setStaleThresholdMinutes(int staleThresholdMinutes) { this.staleThresholdMinutes = staleThresholdMinutes; }
    public int getMaxConcurrent() { return maxConcurrent; }
    public void setMaxConcurrent(int maxConcurrent) { this.maxConcurrent = maxConcurrent; }
}
