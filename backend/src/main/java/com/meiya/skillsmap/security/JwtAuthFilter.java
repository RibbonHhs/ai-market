package com.meiya.skillsmap.security;

import org.springframework.beans.factory.annotation.Autowired;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 过滤器：解析 Authorization 头，塞入 SecurityContext + AuthContext
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        try {
            String header = req.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);
                Claims claims = jwtUtil.parse(token);
                Long userId = claims.get("userId", Number.class).longValue();
                String username = claims.get("username", String.class);
                String role = claims.get("role", String.class);
                AuthContext.UserPrincipal principal = new AuthContext.UserPrincipal(userId, username, role);
                AuthContext.set(principal);

                var auth = new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } catch (JwtException e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            AuthContext.clear();
        } catch (Exception e) {
            log.warn("JWT 处理异常: {}", e.getMessage());
            AuthContext.clear();
        }
        try {
            chain.doFilter(req, res);
        } finally {
            AuthContext.clear();
        }
    }
}
