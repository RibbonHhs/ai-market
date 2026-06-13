package com.meiya.skillsmap.rest;

import com.meiya.skillsmap.common.BizCode;
import com.meiya.skillsmap.common.BizException;
import com.meiya.skillsmap.common.ListResult;
import com.meiya.skillsmap.common.Result;
import com.meiya.skillsmap.entity.Review;
import com.meiya.skillsmap.response.ReviewVO;
import com.meiya.skillsmap.security.AuthContext;
import com.meiya.skillsmap.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 评分
 */
@Tag(name = "Review", description = "Skill 评分（提交需登录）")
@RestController
@Validated
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "某 Skill 的评分列表")
    @GetMapping("/api/skills/{skillId}/reviews")
    public Result<ListResult<ReviewVO>> list(
            @PathVariable Long skillId,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size) {
        return Result.ok(reviewService.listBySkill(skillId, page, size));
    }

    @Operation(summary = "提交评分（需登录）")
    @PostMapping("/api/reviews")
    public Result<Review> submit(@RequestBody SubmitReviewRequest req) {
        AuthContext.UserPrincipal p = AuthContext.get();
        if (p == null) {
            throw new BizException(BizCode.UNAUTHORIZED);
        }
        Review r = reviewService.submit(req.getSkillId(), p.getUserId(), req.getRating(), req.getComment());
        return Result.ok(r);
    }

    public static class SubmitReviewRequest {
        private Long skillId;
        @Min(1) @Max(5)
        private Integer rating;
        private String comment;

        public Long getSkillId() { return skillId; }
        public void setSkillId(Long skillId) { this.skillId = skillId; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
    }
}
