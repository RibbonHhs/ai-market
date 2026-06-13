package com.meiya.skillsmap.rest.admin;

import com.meiya.skillsmap.common.Result;
import com.meiya.skillsmap.mapper.TagMapper;
import com.meiya.skillsmap.response.TagVO;
import com.meiya.skillsmap.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin - Tag")
@RestController
@RequestMapping("/api/admin/tags")
public class AdminTagController {

    private final TagService tagService;
    private final TagMapper tagMapper;

    public AdminTagController(TagService tagService, TagMapper tagMapper) {
        this.tagService = tagService;
        this.tagMapper = tagMapper;
    }

    @Operation(summary = "全部标签")
    @GetMapping
    public Result<List<TagVO>> list() {
        return Result.ok(tagService.listAllWithCount());
    }

    @Operation(summary = "删除标签")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tagMapper.deleteById(id);
        return Result.ok();
    }
}
