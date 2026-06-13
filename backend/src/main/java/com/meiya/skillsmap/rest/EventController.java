package com.meiya.skillsmap.rest;

import com.meiya.skillsmap.common.BizException;
import com.meiya.skillsmap.common.Result;
import com.meiya.skillsmap.service.EventLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * S22: 公开事件埋点端点
 * <ul>
 *   <li>POST /api/events — 接收前端事件上报</li>
 *   <li>无需鉴权，IP 计数限流（v1.1 升级 Bucket4j）</li>
 *   <li>v1：只打日志，不落库</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventLogService eventLogService;

    @Autowired
    public EventController(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @PostMapping
    public Result<Void> track(@RequestBody(required = false) Map<String, Object> body,
                              HttpServletRequest request) {
        if (body == null) {
            throw new BizException(40000, "请求体不能为空");
        }
        Object evt = body.get("event");
        if (!(evt instanceof String) || ((String) evt).isBlank()) {
            throw new BizException(40000, "event 字段必填且非空");
        }
        String event = ((String) evt).trim();
        if (event.length() > 64) {
            throw new BizException(40000, "event 名称过长（≤64）");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) body.get("props");
        String clientIp = resolveClientIp(request);
        String ua = request.getHeader("User-Agent");
        // 限流：单 IP 60 秒内最多 60 次（v1 简单计数）
        if (!eventLogService.allowRequest(clientIp)) {
            // 限流时仍打 warn 日志（便于排查），但返回 ok 不暴露后端细节
            eventLogService.log("rate_limited", props, clientIp, ua);
            return Result.ok();
        }
        eventLogService.log(event, props, clientIp, ua);
        return Result.ok();
    }

    private String resolveClientIp(HttpServletRequest req) {
        String h = req.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) {
            int comma = h.indexOf(',');
            return (comma > 0 ? h.substring(0, comma) : h).trim();
        }
        h = req.getHeader("X-Real-IP");
        if (h != null && !h.isBlank()) return h.trim();
        return req.getRemoteAddr();
    }
}
