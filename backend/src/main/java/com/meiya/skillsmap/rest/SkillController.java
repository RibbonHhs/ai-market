package com.meiya.skillsmap.rest;

import com.meiya.skillsmap.common.BizException;
import com.meiya.skillsmap.common.BizCode;
import com.meiya.skillsmap.common.ListResult;
import com.meiya.skillsmap.common.Result;
import com.meiya.skillsmap.entity.Skill;
import com.meiya.skillsmap.request.SkillQueryRequest;
import com.meiya.skillsmap.response.SkillUploadResponse;
import com.meiya.skillsmap.response.SkillVO;
import com.meiya.skillsmap.security.AuthContext;
import com.meiya.skillsmap.service.SkillService;
import com.meiya.skillsmap.service.SkillUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

@Tag(name = "Skill - 用户端", description = "Skill 浏览、搜索、详情、下载、上传")
@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;
    private final SkillUploadService skillUploadService;

    public SkillController(SkillService skillService, SkillUploadService skillUploadService) {
        this.skillService = skillService;
        this.skillUploadService = skillUploadService;
    }

    @Operation(summary = "Skill 列表（搜索/筛选/分页）",
               description = "支持的 query 参数：keyword（关键字模糊匹配 name/displayName/description）"
                       + "、categoryId（SOC 分类 id，传入一级则自动展开到子分类）"
                       + "、occupationCode（SOC 一级 code 如 #01 / 子 code 如 01-01，展开所有匹配的 category）"
                       + "、usageCategoryId（USAGE 维度 id，精确匹配）"
                       + "、tagSlug、source、sort（latest/hot/installs/rating/views）、page、size")
    @GetMapping
    public Result<ListResult<SkillVO>> list(SkillQueryRequest query) {
        return Result.ok(skillService.listSkills(query));
    }

    @Operation(summary = "Skill 详情（按 ID）")
    @GetMapping("/{id}")
    public Result<SkillVO> detail(@PathVariable @Parameter(description = "Skill ID") Long id) {
        return Result.ok(skillService.getDetail(id, true));
    }

    @Operation(summary = "Skill 详情（按 slug）")
    @GetMapping("/slug/{slug}")
    public Result<SkillVO> detailBySlug(@PathVariable String slug) {
        return Result.ok(skillService.getDetailBySlug(slug, true));
    }

    @Operation(summary = "热门 Skills（sort=hot 默认按安装数；recent 按浏览数；featured 精选）")
    @GetMapping("/hot")
    public Result<List<SkillVO>> hot(
            @RequestParam(defaultValue = "12") int limit,
            @RequestParam(defaultValue = "hot") String sort) {
        return Result.ok(skillService.getHot(limit, sort));
    }

    @Operation(summary = "最新 Skills（按发布时间）")
    @GetMapping("/latest")
    public Result<List<SkillVO>> latest(@RequestParam(defaultValue = "12") int limit) {
        return Result.ok(skillService.getLatest(limit));
    }

    @Operation(summary = "精选 Skills（首页推荐）")
    @GetMapping("/featured")
    public Result<List<SkillVO>> featured(@RequestParam(defaultValue = "6") int limit) {
        return Result.ok(skillService.getFeatured(limit));
    }

    /**
     * 公开下载接口：按 slug 下载 .skill zip
     * 优先从 data/skill-packages/{name}/ 整目录打包；没本地包则从 DB 重建
     */
    @Operation(summary = "下载 Skill 包（.skill zip）")
    @GetMapping("/slug/{slug}/download")
    public ResponseEntity<byte[]> downloadBySlug(@PathVariable String slug) {
        SkillVO vo = skillService.getDetailBySlug(slug, false);
        if (vo == null || !"published".equals(vo.getStatus())) {
            throw new BizException(BizCode.SKILL_NOT_FOUND);
        }
        safeIncrementInstalls(vo.getId());
        return buildZip(vo.getId(), vo.getName());
    }

    @Operation(summary = "下载 Skill 包（按 ID）")
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadById(@PathVariable Long id) {
        SkillVO vo = skillService.getDetail(id, false);
        if (vo == null) {
            throw new BizException(BizCode.SKILL_NOT_FOUND);
        }
        safeIncrementInstalls(vo.getId());
        return buildZip(vo.getId(), vo.getName());
    }

    /**
     * S38: 用户上传 Skill 包（zip）→ 立即可见。
     * <p>受保护端点：必须登录（JWT Bearer Token）；与 review/favorite 写端点鉴权模式一致。
     */
    @Operation(summary = "上传 Skill 包（需鉴权）",
               description = "上传 .skill zip 包（≤ 10MB），含 SKILL.md + 资源文件。"
                       + "上传后 status=PUBLIC，立即对所有用户可见。"
                       + "错误码：40001 文件缺失/损坏/zip 越界；40002 缺 SKILL.md；"
                       + "40003 frontmatter 缺 name/description；40004 zip bomb（解压超限）；"
                       + "40900 slug 冲突且自动重试耗尽；41300 文件 > 10MB；40100 未登录；"
                       + "50001 解压失败。")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "上传成功"),
            @ApiResponse(responseCode = "400", description = "40001/40002/40003/40004 zip 非法"),
            @ApiResponse(responseCode = "401", description = "40100 未登录"),
            @ApiResponse(responseCode = "409", description = "40900 slug 冲突"),
            @ApiResponse(responseCode = "413", description = "41300 文件过大")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<SkillUploadResponse> upload(
            @RequestPart("file") @Parameter(description = "zip 包（≤ 10MB）") MultipartFile file,
            @RequestParam("categoryId") @Parameter(description = "SOC 二级分类 id（必填）") Long categoryId,
            @RequestParam(value = "usageCategoryIds", required = false)
            @Parameter(description = "USAGE 维度 id 列表（可选）") List<Long> usageCategoryIds,
            @RequestParam(value = "tagSlugs", required = false)
            @Parameter(description = "tag slug 列表（可选，缺失自动创建）") List<String> tagSlugs) {
        AuthContext.UserPrincipal p = AuthContext.get();
        if (p == null) {
            throw new BizException(BizCode.UNAUTHORIZED);
        }
        SkillUploadResponse resp = skillUploadService.upload(
                file, p.getUserId(), categoryId,
                usageCategoryIds == null ? Collections.emptyList() : usageCategoryIds,
                tagSlugs == null ? Collections.emptyList() : tagSlugs);
        return Result.ok(resp);
    }

    private ResponseEntity<byte[]> buildZip(Long id, String name) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            skillService.exportZip(id, baos);
            byte[] zip = baos.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", name + ".zip");
            headers.setContentLength(zip.length);
            return new ResponseEntity<>(zip, headers, org.springframework.http.HttpStatus.OK);
        } catch (Exception e) {
            log.error("下载失败 id={}", id, e);
            throw new BizException(BizCode.SYSTEM_ERROR, "打包失败: " + e.getMessage());
        }
    }

    /**
     * best-effort 自增 installs。计数失败不能阻塞下载（用户已付出网络代价拿到 zip），
     * 仅记 warn 日志供后续排查。
     */
    private void safeIncrementInstalls(Long id) {
        try {
            skillService.incrementInstalls(id);
        } catch (Exception e) {
            log.warn("下载计数自增失败 id={}, err={}", id, e.getMessage());
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SkillController.class);
}

