package com.meiya.skillsmap.response;

import com.meiya.skillsmap.entity.User;

import java.time.LocalDateTime;

/**
 * S39 安全修复: 用户信息 DTO —— 显式排除 password 字段，杜绝序列化泄露。
 *
 * <p>使用 Java record（不可变值对象）而非传统 class：
 * <ul>
 *   <li>不可变 ⇒ 防止 controller 层意外修改 user 数据</li>
 *   <li>自动 getter / equals / toString ⇒ 减少模板代码</li>
 *   <li>Jackson 2.12+ 原生支持 record 序列化（项目使用 Spring Boot 3.5.7 / Jackson 2.18+）</li>
 * </ul>
 *
 * <p>字段对齐 AuthController.buildAuthResp 里的 userInfo（保持 login 响应兼容）：
 * id / username / displayName / avatar / email / role / status / createTime
 *
 * <p>双保险：即使 caller 错误地传 {@code User} entity（不走 DTO），
 * entity 层 {@code @JsonProperty(WRITE_ONLY)} 也会兜底排除 password。
 */
public record UserResponse(
        Long id,
        String username,
        String email,
        String displayName,
        String avatar,
        String role,
        Integer status,
        LocalDateTime createTime
) {
    /**
     * 从 entity 安全转换（password 字段不进入 DTO）。
     */
    public static UserResponse from(User u) {
        if (u == null) return null;
        return new UserResponse(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getDisplayName(),
                u.getAvatar(),
                u.getRole(),
                u.getStatus(),
                u.getCreateTime()
        );
    }
}
