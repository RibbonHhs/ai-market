package com.meiya.skillsmap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Sprint S23: 公开 API 限流配置（Bucket4j 按 IP）。
 *
 * 默认 60 req / 1 min / IP。修改后立即生效，无需重启外的额外操作。
 *
 * 例：
 * skillsmap:
 *   rate-limit:
 *     capacity: 60
 *     refill-tokens: 60
 *     refill-period: 1m
 */
@Configuration
@ConfigurationProperties(prefix = "skillsmap.rate-limit")
public class RateLimitProperties {

    /** 桶容量（最大突发请求数） */
    private long capacity = 60;

    /** 每个周期补充的 token 数 */
    private long refillTokens = 60;

    /** 补充周期，支持 1s / 1m / 1h（Duration 解析） */
    private String refillPeriod = "1m";

    /** 是否启用（默认 false：S23 第一次发布先关闭，等运维验证后再打开） */
    private boolean enabled = false;

    public long getCapacity() { return capacity; }
    public void setCapacity(long capacity) { this.capacity = capacity; }
    public long getRefillTokens() { return refillTokens; }
    public void setRefillTokens(long refillTokens) { this.refillTokens = refillTokens; }
    public String getRefillPeriod() { return refillPeriod; }
    public void setRefillPeriod(String refillPeriod) { this.refillPeriod = refillPeriod; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}