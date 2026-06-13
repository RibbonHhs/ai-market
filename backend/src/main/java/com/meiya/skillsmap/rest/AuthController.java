package com.meiya.skillsmap.rest;

import org.springframework.beans.factory.annotation.Autowired;
import cn.hutool.core.util.StrUtil;
import com.meiya.skillsmap.common.BizCode;
import com.meiya.skillsmap.common.BizException;
import com.meiya.skillsmap.common.Result;
import com.meiya.skillsmap.entity.User;
import com.meiya.skillsmap.security.AuthContext;
import com.meiya.skillsmap.security.JwtUtil;
import com.meiya.skillsmap.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 鉴权：登录 / 注册 / 当前用户
 */
@Tag(name = "Auth", description = "登录、注册、当前用户")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest req) {
        if (StrUtil.isBlank(req.getUsername()) || StrUtil.isBlank(req.getPassword())) {
            throw new BizException(BizCode.BAD_REQUEST, "用户名/密码必填");
        }
        User user = userService.getByUsername(req.getUsername());
        if (user == null) {
            throw new BizException(BizCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BizException(BizCode.FORBIDDEN, "账号已禁用");
        }
        if (!userService.checkPassword(user, req.getPassword())) {
            throw new BizException(BizCode.UNAUTHORIZED, "用户名或密码错误");
        }
        String token = jwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        return Result.ok(buildAuthResp(token, user));
    }

    @Operation(summary = "注册")
    @PostMapping("/register")
    public Result<User> register(@RequestBody RegisterRequest req) {
        if (StrUtil.isBlank(req.getUsername()) || StrUtil.isBlank(req.getPassword())) {
            throw new BizException(BizCode.BAD_REQUEST, "用户名/密码必填");
        }
        if (req.getUsername().length() < 3 || req.getUsername().length() > 20) {
            throw new BizException(BizCode.BAD_REQUEST, "用户名长度 3-20");
        }
        if (!req.getUsername().matches("^[a-zA-Z0-9_-]+$")) {
            throw new BizException(BizCode.BAD_REQUEST, "用户名只能含字母数字下划线连字符");
        }
        if (req.getPassword().length() < 6) {
            throw new BizException(BizCode.BAD_REQUEST, "密码至少 6 位");
        }
        if (userService.getByUsername(req.getUsername()) != null) {
            throw new BizException(BizCode.USER_ALREADY_EXISTS);
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setEmail(req.getEmail());
        user.setDisplayName(StrUtil.isBlank(req.getDisplayName()) ? req.getUsername() : req.getDisplayName());
        user.setAvatar("🙂");
        user.setRole("USER");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userService.save(user);
        return Result.ok(user);
    }

    @Operation(summary = "当前用户")
    @GetMapping("/me")
    public Result<User> me() {
        AuthContext.UserPrincipal p = AuthContext.get();
        if (p == null) {
            throw new BizException(BizCode.UNAUTHORIZED);
        }
        User user = userService.getById(p.getUserId());
        if (user == null) {
            throw new BizException(BizCode.USER_NOT_FOUND);
        }
        return Result.ok(user);
    }

    @Operation(summary = "登出（前端清缓存即可）")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.ok();
    }

    private Map<String, Object> buildAuthResp(String token, User user) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("token", token);
        Map<String, Object> info = new HashMap<>();
        info.put("id", user.getId());
        info.put("username", user.getUsername());
        info.put("displayName", user.getDisplayName());
        info.put("avatar", user.getAvatar());
        info.put("email", user.getEmail());
        info.put("role", user.getRole());
        resp.put("userInfo", info);
        resp.put("roles", user.getRole() == null ? List.of() : List.of(user.getRole()));
        resp.put("permissions", List.of());
        resp.put("expiresIn", jwtUtil.getExpirationSeconds());
        return resp;
    }

    public static class LoginRequest {
        private String username;
        private String password;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        private String username;
        private String password;
        private String email;
        private String displayName;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }
}
