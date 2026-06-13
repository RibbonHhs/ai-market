package com.meiya.skillsmap.service;

import com.alibaba.fastjson2.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * S22: 事件日志服务（v1：内存限流 + SLF4J 日志）
 * <p>限流策略：单 IP 60 秒内最多 60 次。计数器按 IP 维度，周期滚动清零。
 * <p>v1.1 计划：替换为 Bucket4j 令牌桶。
 */
@Service
public class EventLogService {

    private static final Logger log = LoggerFactory.getLogger("EVENT");

    /** 限流窗口（秒） */
    private static final long WINDOW_SECONDS = 60L;
    /** 单窗口内最大请求数 */
    private static final int MAX_PER_WINDOW = 60;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public boolean allowRequest(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            clientIp = "unknown";
        }
        long nowSec = Instant.now().getEpochSecond();
        Window w = windows.computeIfAbsent(clientIp, k -> new Window(nowSec));
        synchronized (w) {
            if (nowSec - w.windowStartSec >= WINDOW_SECONDS) {
                w.windowStartSec = nowSec;
                w.count.set(0);
            }
            return w.count.incrementAndGet() <= MAX_PER_WINDOW;
        }
    }

    public void log(String event, Map<String, Object> props, String clientIp, String ua) {
        try {
            String propsJson = props == null ? null : JSON.toJSONString(props);
            log.info("event={} ip={} ua=\"{}\" props={}",
                    event,
                    clientIp == null ? "-" : clientIp,
                    ua == null ? "" : ua.replace('\n', ' ').replace('\r', ' '),
                    propsJson == null ? "{}" : propsJson);
        } catch (Exception e) {
            log.warn("event log failed: {}", e.getMessage());
        }
    }

    private static class Window {
        long windowStartSec;
        final AtomicInteger count = new AtomicInteger(0);
        Window(long start) { this.windowStartSec = start; }
    }
}
