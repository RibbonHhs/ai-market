package com.meiya.skillsmap.rest.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.meiya.skillsmap.common.BizException;
import com.meiya.skillsmap.common.Result;
import com.meiya.skillsmap.entity.Category;
import com.meiya.skillsmap.mapper.CategoryMapper;
import com.meiya.skillsmap.response.CategoryVO;
import com.meiya.skillsmap.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "Admin - Category")
@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CategoryMapper categoryMapper;
    private final CategoryService categoryService;

    public AdminCategoryController(CategoryMapper categoryMapper, CategoryService categoryService) {
        this.categoryMapper = categoryMapper;
        this.categoryService = categoryService;
    }

    @Operation(summary = "分类列表（type 缺省=SOC+USAGE 全部；USAGE=仅用途；SOC=仅职业）")
    @GetMapping
    public Result<List<CategoryVO>> list(@RequestParam(required = false) String type) {
        return Result.ok(categoryService.listAllWithCount(type));
    }

    @Operation(summary = "新建分类")
    @PostMapping
    public Result<Category> create(@RequestBody Category body) {
        if (body.getName() == null || body.getSlug() == null) {
            throw new BizException(40000, "name/slug 必填");
        }
        body.setId(null);
        body.setCreateTime(LocalDateTime.now());
        body.setUpdateTime(LocalDateTime.now());
        if (body.getSortOrder() == null) body.setSortOrder(99);
        if (body.getSkillCount() == null) body.setSkillCount(0);
        categoryMapper.insert(body);
        return Result.ok(body);
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public Result<Category> update(@PathVariable Long id, @RequestBody Category body) {
        body.setId(id);
        body.setUpdateTime(LocalDateTime.now());
        categoryMapper.updateById(body);
        return Result.ok(body);
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        return Result.ok();
    }
}
