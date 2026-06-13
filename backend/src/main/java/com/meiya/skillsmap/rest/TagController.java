package com.meiya.skillsmap.rest;

import com.meiya.skillsmap.common.Result;
import com.meiya.skillsmap.response.TagVO;
import com.meiya.skillsmap.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Tag - 用户端", description = "Skill 标签")
@RestController
@RequestMapping("/api/tags")
public class TagController {

    private final TagService tagService;

    public TagController(TagService tagService) {
        this.tagService = tagService;
    }

    @Operation(summary = "全部标签（含 skill_count）")
    @GetMapping
    public Result<List<TagVO>> list() {
        return Result.ok(tagService.listAllWithCount());
    }
}
