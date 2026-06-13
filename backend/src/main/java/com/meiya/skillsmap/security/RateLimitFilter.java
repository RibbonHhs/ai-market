package com.meiya.skillsmap.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meiya.skillsmap.common.BizCode;
import com.meiya.skillsmap.common.Result;
import com.meiya.skillsmap.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Sprint S23: 公开 API 限流（Bucket4j 按 IP）。
 *
 * 行为：
 * - 从 X-Forwarded-For / X-Real-IP / remoteAddr 拿客户端 IP
 * - 每个 IP 一个 Bucket（ConcurrentHashMap）
 * - 超限 throw BizException(RATE_LIMITED, 429)
 * - 桶容量 / 补充速率走 RateLimitProperties
 *
 * 注意：本 filter 只挂在 SecurityConfig 中"明确受限的路径前缀"上，
 * 不会限制 /api/auth/*（防登录被锁）、/doc.html、/v3/api-docs/**。
 */
@Component
@Order(10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(RateLimitProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 开关关闭 → 整个 filter 跳过
        if (!props.isEnabled()) return true;
        String uri = request.getRequestURI();
        // 白名单：登录注册、API 文档
        if (uri.startsWith("/api/auth/")) return true;
        if (uri.startsWith("/doc.html")) return true;
        if (uri.startsWith("/v3/api-docs")) return true;
        if (uri.startsWith("/swagger-ui")) return true;
        if (uri.startsWith("/webjars/")) return true;
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String ip = resolveIp(request);
        Bucket bucket = buckets.computeIfAbsent(ip, k -> newBucket());
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }
        // 超限：直接写 HTTP 429 + 统一 Result JSON（绕过 GlobalExceptionHandler 的 500）
        log.warn("[rate-limit] blocked ip={} uri={}", ip, request.getRequestURI());
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(
                        Result.fail(BizCode.RATE_LIMITED.getCode(), BizCode.RATE_LIMITED.getMessage())));
    }

    private Bucket newBucket() {
        Duration period = Duration.parse("PT" + normalize(props.getRefillPeriod()));
        Refill refill = Refill.greedy(props.getRefillTokens(), period);
        Bandwidth limit = Bandwidth.classic(props.getCapacity(), refill);
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * 解析时长字符串：
     *   "1s" -> "1S"
     *   "1m" -> "1M"
     *   "1h" -> "1H"
     * Bucket4j Duration.parse(PT...) 接受 ISO-8601。
     */
    private static String normalize(String s) {
        if (!StringUtils.hasText(s)) return "1M";
        char unit = Character.toUpperCase(s.charAt(s.length() - 1));
        String num = s.substring(0, s.length() - 1);
        return num + unit;
    }

    private static String resolveIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xff)) {
            // 取第一个（原始客户端）
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        String xri = req.getHeader("X-Real-IP");
        if (StringUtils.hasText(xri)) return xri.trim();
        return req.getRemoteAddr() == null ? "unknown" : req.getRemoteAddr();
    }

    /** 测试用：清空桶缓存 */
    public void clearBuckets() {
        buckets.clear();
    }
}