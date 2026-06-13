package com.meiya.skillsmap.config;

import org.springframework.beans.factory.annotation.Autowired;
import com.meiya.skillsmap.security.JwtAuthFilter;
import com.meiya.skillsmap.security.RateLimitFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security + JWT 配置
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, RateLimitFilter rateLimitFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 公开
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/skills/**").permitAll()       // GET 全部
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/skills", "/api/skills/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/categories", "/api/categories/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/tags", "/api/tags/**").permitAll()
                        // S22: 公开事件埋点端点
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/events").permitAll()
                        .requestMatchers("/api/reviews/**").permitAll()       // GET 评分公开
                        .requestMatchers("/api/favorites/**").permitAll()     // 收藏需登录但走 controller
                        .requestMatchers("/doc.html", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/webjars/**", "/favicon.ico").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // 评分提交、收藏、个人中心需登录
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/reviews").authenticated()
                        .requestMatchers("/api/favorites/**").authenticated()
                        .requestMatchers("/api/auth/me").authenticated()
                        // 后台管理需 ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // 其他放行
                        .anyRequest().permitAll()
                )
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))  // H2 console iframe
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // S23: 公开 API 限流（放在鉴权之前，限流比 JWT 解析便宜）
                .addFilterBefore(rateLimitFilter, JwtAuthFilter.class);
        return http.build();
    }
}
