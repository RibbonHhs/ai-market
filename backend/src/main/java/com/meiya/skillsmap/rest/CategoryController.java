package com.meiya.skillsmap.rest;

import com.meiya.skillsmap.common.Result;
import com.meiya.skillsmap.response.CategoryVO;
import com.meiya.skillsmap.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Category - 用户端", description = "Skill 分类")
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "全部分类（含 skill_count）")
    @GetMapping
    public Result<List<CategoryVO>> list(@org.springframework.web.bind.annotation.RequestParam(required = false) String type) {
        return Result.ok(categoryService.listAllWithCount(type));
    }

    @Operation(summary = "S04: 分类树（两级折叠，按 type 过滤）")
    @GetMapping("/tree")
    public Result<List<CategoryVO>> tree(@org.springframework.web.bind.annotation.RequestParam(defaultValue = "SOC") String type) {
        return Result.ok(categoryService.listTree(type));
    }

    @Operation(summary = "S04: 旧 slug 301 重定向查询（命中返回 newSlug）")
    @GetMapping("/{slug}/redirect")
    public Result<java.util.Map<String, Object>> redirect(@org.springframework.web.bind.annotation.PathVariable String slug) {
        String newSlug = categoryService.findRedirect(slug);
        java.util.Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("oldSlug", slug);
        if (newSlug == null) {
            data.put("found", false);
            data.put("newSlug", null);
            return Result.ok(data);
        }
        data.put("found", true);
        data.put("newSlug", newSlug);
        data.put("redirectStatus", 301);
        return Result.ok(data);
    }
}
