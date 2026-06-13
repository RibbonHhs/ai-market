package com.meiya.skillsmap.rest.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiya.skillsmap.common.Result;
import com.meiya.skillsmap.entity.Skill;
import com.meiya.skillsmap.mapper.SkillMapper;
import com.meiya.skillsmap.mapper.UserMapper;
import com.meiya.skillsmap.service.GitSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Admin - Dashboard")
@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final SkillMapper skillMapper;
    private final UserMapper userMapper;
    private final GitSyncService gitSyncService;

    public AdminDashboardController(SkillMapper skillMapper, UserMapper userMapper,
                                    GitSyncService gitSyncService) {
        this.skillMapper = skillMapper;
        this.userMapper = userMapper;
        this.gitSyncService = gitSyncService;
    }

    @Operation(summary = "来源分布统计")
    @GetMapping("/source-stats")
    public Result<Map<String, Object>> sourceStats() {
        long total = skillMapper.selectCount(null);
        long official = skillMapper.selectCount(new LambdaQueryWrapper<Skill>().eq(Skill::getSource, "official"));
        long community = skillMapper.selectCount(new LambdaQueryWrapper<Skill>().eq(Skill::getSource, "community"));
        Map<String, Object> res = new HashMap<>();
        res.put("total", total);
        res.put("official", official);
        res.put("community", community);
        return Result.ok(res);
    }

    @Operation(summary = "Git 同步状态")
    @GetMapping("/git-status")
    public Result<Map<String, Object>> gitStatus() {
        Map<String, Object> res = new HashMap<>();
        res.put("enabled", gitSyncService.isEnabled());
        res.put("ready", gitSyncService.isReady());
        res.put("successCount", gitSyncService.getSuccessCount());
        res.put("failureCount", gitSyncService.getFailureCount());
        res.put("lastSyncAt", gitSyncService.getLastSyncAt());
        res.put("lastError", gitSyncService.getLastError());
        res.put("recentCommits", gitSyncService.listRecentCommits(5));
        return Result.ok(res);
    }
}

