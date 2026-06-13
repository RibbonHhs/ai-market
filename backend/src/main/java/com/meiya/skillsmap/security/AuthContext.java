package com.meiya.skillsmap.security;

/**
 * 当前登录用户上下文（ThreadLocal）
 */
public class AuthContext {

    private static final ThreadLocal<UserPrincipal> HOLDER = new ThreadLocal<>();

    public static void set(UserPrincipal principal) {
        HOLDER.set(principal);
    }

    public static UserPrincipal get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static class UserPrincipal {
        private Long userId;
        private String username;
        private String role;

        public UserPrincipal() {}

        public UserPrincipal(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
