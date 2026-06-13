package com.meiya.skillsmap.security;

import org.springframework.beans.factory.annotation.Autowired;
import com.meiya.skillsmap.config.SeedProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具
 */
@Component
public class JwtUtil {

    private final SeedProperties seedProperties;

    public JwtUtil(SeedProperties seedProperties) {
        this.seedProperties = seedProperties;
    }
    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(seedProperties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generate(Long userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);
        long now = System.currentTimeMillis();
        long exp = now + seedProperties.getJwt().getExpirationSeconds() * 1000L;
        return Jwts.builder()
                .claims(claims)
                .subject(username)
                .issuedAt(new Date(now))
                .expiration(new Date(exp))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getExpirationSeconds() {
        return seedProperties.getJwt().getExpirationSeconds();
    }
}
