package com.meiya.skillsmap.rest.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meiya.skillsmap.common.ListResult;
import com.meiya.skillsmap.common.Result;
import com.meiya.skillsmap.entity.User;
import com.meiya.skillsmap.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Admin - User")
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserMapper userMapper;

    public AdminUserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Operation(summary = "用户列表")
    @GetMapping
    public Result<ListResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size) {
        Page<User> mpPage = Page.of(page, size);
        Page<User> result = userMapper.selectPage(mpPage, new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreateTime));
        List<Map<String, Object>> records = result.getRecords().stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("displayName", u.getDisplayName());
            m.put("email", u.getEmail());
            m.put("avatar", u.getAvatar());
            m.put("role", u.getRole());
            m.put("status", u.getStatus());
            m.put("createTime", u.getCreateTime());
            return m;
        }).toList();
        return Result.ok(ListResult.of(records, result.getTotal(), page, size));
    }

    @Operation(summary = "修改角色")
    @PutMapping("/{id}/role")
    public Result<Void> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String role = body.get("role");
        if (!"ADMIN".equals(role) && !"USER".equals(role)) {
            return Result.fail(40000, "role 必须为 ADMIN 或 USER");
        }
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(u);
        return Result.ok();
    }

    @Operation(summary = "启/禁用户")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Integer status = body.get("status");
        User u = new User();
        u.setId(id);
        u.setStatus(status == null ? 1 : status);
        u.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(u);
        return Result.ok();
    }
}
