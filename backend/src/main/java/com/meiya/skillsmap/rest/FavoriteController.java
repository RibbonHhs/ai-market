package com.meiya.skillsmap.rest;

import com.meiya.skillsmap.common.BizCode;
import com.meiya.skillsmap.common.BizException;
import com.meiya.skillsmap.common.ListResult;
import com.meiya.skillsmap.common.Result;
import com.meiya.skillsmap.response.SkillVO;
import com.meiya.skillsmap.security.AuthContext;
import com.meiya.skillsmap.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "Favorite", description = "Skill 收藏")
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @Operation(summary = "我的收藏列表")
    @GetMapping("/mine")
    public Result<ListResult<SkillVO>> listMine(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size) {
        AuthContext.UserPrincipal p = AuthContext.get();
        if (p == null) throw new BizException(BizCode.UNAUTHORIZED);
        return Result.ok(favoriteService.listMine(p.getUserId(), page, size));
    }

    @Operation(summary = "添加/切换收藏")
    @PostMapping("/{skillId}")
    public Result<Map<String, Object>> add(@PathVariable Long skillId) {
        AuthContext.UserPrincipal p = AuthContext.get();
        if (p == null) throw new BizException(BizCode.UNAUTHORIZED);
        boolean added = favoriteService.toggle(p.getUserId(), skillId);
        if (!added) {
            return Result.ok(Map.of("favorited", false, "action", "removed"));
        }
        return Result.ok(Map.of("favorited", true, "action", "added"));
    }

    @Operation(summary = "取消收藏")
    @DeleteMapping("/{skillId}")
    public Result<Map<String, Object>> remove(@PathVariable Long skillId) {
        AuthContext.UserPrincipal p = AuthContext.get();
        if (p == null) throw new BizException(BizCode.UNAUTHORIZED);
        favoriteService.toggle(p.getUserId(), skillId);
        return Result.ok(Map.of("favorited", false, "action", "removed"));
    }

    @Operation(summary = "查询是否已收藏")
    @GetMapping("/{skillId}/status")
    public Result<Map<String, Object>> status(@PathVariable Long skillId) {
        AuthContext.UserPrincipal p = AuthContext.get();
        boolean fav = p != null && favoriteService.isFavorited(p.getUserId(), skillId);
        return Result.ok(Map.of("favorited", fav));
    }
}
