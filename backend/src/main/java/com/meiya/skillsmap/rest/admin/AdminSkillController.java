package com.meiya.skillsmap.rest.admin;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meiya.skillsmap.common.ListResult;
import com.meiya.skillsmap.common.Result;
import com.meiya.skillsmap.entity.Skill;
import com.meiya.skillsmap.entity.SkillTag;
import com.meiya.skillsmap.mapper.CategoryMapper;
import com.meiya.skillsmap.mapper.SkillMapper;
import com.meiya.skillsmap.mapper.SkillTagMapper;
import com.meiya.skillsmap.mapper.TagMapper;
import com.meiya.skillsmap.request.GitImportRequest;
import com.meiya.skillsmap.response.GitImportResult;
import com.meiya.skillsmap.response.SkillVO;
import com.meiya.skillsmap.service.CategoryService;
import com.meiya.skillsmap.service.SkillGitService;
import com.meiya.skillsmap.service.SkillService;
import com.meiya.skillsmap.service.SkillStorageService;
import com.meiya.skillsmap.service.impl.TagServiceImpl;
import com.meiya.skillsmap.util.CategoryUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "Admin - Skill", description = "后台 Skill 管理")
@RestController
@RequestMapping("/api/admin/skills")
public class AdminSkillController {

    private static final Logger log = LoggerFactory.getLogger(AdminSkillController.class);

    private final SkillService skillService;
    private final SkillGitService skillGitService;
    private final CategoryService categoryService;
    private final SkillMapper skillMapper;
    private final TagMapper tagMapper;
    private final SkillTagMapper skillTagMapper;
    private final SkillStorageService storageService;
    private final CategoryMapper categoryMapper;

    public AdminSkillController(SkillService skillService,
                                SkillGitService skillGitService,
                                CategoryService categoryService,
                                SkillMapper skillMapper,
                                TagMapper tagMapper,
                                SkillTagMapper skillTagMapper,
                                SkillStorageService storageService,
                                CategoryMapper categoryMapper) {
        this.skillService = skillService;
        this.skillGitService = skillGitService;
        this.categoryService = categoryService;
        this.skillMapper = skillMapper;
        this.tagMapper = tagMapper;
        this.skillTagMapper = skillTagMapper;
        this.storageService = storageService;
        this.categoryMapper = categoryMapper;
    }

    @Operation(summary = "后台列表（支持 status 过滤）")
    @GetMapping
    public Result<ListResult<SkillVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long size) {
        Page<Skill> mpPage = Page.of(page, size);
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(status)) {
            wrapper.eq(Skill::getStatus, status);
        }
        if (StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(Skill::getName, keyword)
                    .or().like(Skill::getDisplayName, keyword)
                    .or().like(Skill::getDescription, keyword));
        }
        wrapper.orderByDesc(Skill::getUpdateTime);
        Page<Skill> result = skillMapper.selectPage(mpPage, wrapper);
        List<SkillVO> voList = result.getRecords().stream().map(skillService::toVO).toList();
        return Result.ok(ListResult.of(voList, result.getTotal(), page, size));
    }

    @Operation(summary = "后台查详情")
    @GetMapping("/{id}")
    public Result<SkillVO> detail(@PathVariable Long id) {
        Skill skill = skillMapper.selectById(id);
        if (skill == null) {
            return Result.fail(50001, "Skill 不存在");
        }
        return Result.ok(skillService.toVO(skill));
    }

    @Operation(summary = "新建 Skill")
    @PostMapping
    public Result<SkillVO> create(@RequestBody Skill body) {
        if (StrUtil.isBlank(body.getName()) || !body.getName().matches("^[a-z0-9-]+$")) {
            return Result.fail(40000, "name 必须为 kebab-case");
        }
        if (skillMapper.selectCount(new LambdaQueryWrapper<Skill>().eq(Skill::getName, body.getName())) > 0) {
            return Result.fail(40900, "Skill 已存在");
        }
        if (body.getSlug() == null) body.setSlug(body.getName());
        if (body.getStatus() == null) body.setStatus("draft");
        if (body.getSource() == null) body.setSource("community");
        if (body.getIcon() == null) body.setIcon("📦");
        if (body.getRatingAvg() == null) body.setRatingAvg(0.0);
        if (body.getRatingCount() == null) body.setRatingCount(0);
        body.setCreateTime(LocalDateTime.now());
        body.setUpdateTime(LocalDateTime.now());
        body.setId(null);
        // S20: zip / 单文件上传时自动 USAGE 分类（Q2=A：仅 create 时打标；Q3=A：未命中留空让 admin 手动改）
        if (body.getUsageCategoryId() == null) {
            String usageCode = CategoryUtil.guessUsageCode(null, body.getName());
            body.setUsageCategoryId(CategoryUtil.categoryIdByUsageCode(categoryMapper, usageCode));
        }
        // 记录上传者
        com.meiya.skillsmap.security.AuthContext.UserPrincipal p = com.meiya.skillsmap.security.AuthContext.get();
        if (p != null) {
            body.setCreatedByUserId(p.getUserId());
        }
        skillMapper.insert(body);
        // 标签处理
        syncTags(body.getId(), body.getTags());
        return Result.ok(skillService.toVO(body));
    }

    @Operation(summary = "更新 Skill")
    @PutMapping("/{id}")
    public Result<SkillVO> update(@PathVariable Long id, @RequestBody Skill body) {
        Skill exist = skillMapper.selectById(id);
        if (exist == null) {
            return Result.fail(50001, "Skill 不存在");
        }
        body.setId(id);
        body.setName(exist.getName()); // name 不可改
        body.setUpdateTime(LocalDateTime.now());
        skillMapper.updateById(body);
        syncTags(id, body.getTags());
        // 刷新分类计数
        categoryService.refreshAllCategoryCount();
        return Result.ok(skillService.toVO(skillMapper.selectById(id)));
    }

    @Operation(summary = "删除 Skill")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 先查 name，用于清理本地包目录 + 触发 Git 同步
        Skill skill = skillMapper.selectById(id);
        skillMapper.deleteById(id);
        if (skill != null && skill.getName() != null) {
            try {
                storageService.deletePackage(skill.getName());
            } catch (Exception e) {
                log.warn("清理包目录失败: {}", e.getMessage());
            }
        }
        categoryService.refreshAllCategoryCount();
        return Result.ok();
    }

    @Operation(summary = "上架")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        Skill s = new Skill();
        s.setId(id);
        s.setStatus("published");
        s.setUpdateTime(LocalDateTime.now());
        skillMapper.updateById(s);
        return Result.ok();
    }

    @Operation(summary = "下架")
    @PostMapping("/{id}/unpublish")
    public Result<Void> unpublish(@PathVariable Long id) {
        Skill s = new Skill();
        s.setId(id);
        s.setStatus("flagged");
        s.setUpdateTime(LocalDateTime.now());
        skillMapper.updateById(s);
        return Result.ok();
    }

    @Operation(summary = "从本地扫描导入（占位，实际由 SeedService 处理）")
    @PostMapping("/import-from-local")
    public Result<Map<String, Object>> importFromLocal() {
        // 由 SkillSeedService 在启动时处理；这里返回计数
        Map<String, Object> res = new HashMap<>();
        res.put("imported", 0);
        res.put("skipped", 0);
        res.put("message", "启动时已自动扫描；如需重新导入，请重启后端或在管理界面手工新增");
        return Result.ok(res);
    }

    @Operation(summary = "刷新所有分类的 skill_count")
    @PostMapping("/refresh-category-count")
    public Result<Void> refreshCategoryCount() {
        categoryService.refreshAllCategoryCount();
        return Result.ok();
    }

    // ============================================================
    //  Sprint S02: Git 源 Skill 端点
    // ============================================================

    @Operation(summary = "从 Git URL 导入 Skill（Monorepo 自动拆分）")
    @PostMapping("/from-git")
    public Result<GitImportResult> fromGit(@Valid @RequestBody GitImportRequest req) {
        try {
            GitImportResult res = skillGitService.importFromGit(req);
            return Result.ok(res);
        } catch (com.meiya.skillsmap.common.BizException be) {
            return Result.fail(be.getCode(), be.getMessage());
        } catch (Exception e) {
            log.error("from-git failed", e);
            return Result.fail(500, "导入失败: " + e.getMessage());
        }
    }

    @Operation(summary = "同步单个 Git 源 Skill（覆盖策略）")
    @PostMapping("/{id}/sync")
    public Result<Map<String, Object>> syncSkill(@PathVariable Long id) {
        try {
            SkillGitService.GitSyncResult r = skillGitService.syncSkill(id);
            Map<String, Object> body = new HashMap<>();
            body.put("id", r.id());
            body.put("lastCommitSha", r.lastCommitSha());
            body.put("changed", r.changed());
            body.put("message", r.message());
            return Result.ok(body);
        } catch (com.meiya.skillsmap.common.BizException be) {
            return Result.fail(be.getCode(), be.getMessage());
        } catch (Exception e) {
            log.error("sync-skill id={} failed", id, e);
            return Result.fail(500, "同步失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询 Skill 同步状态（仅 GIT_URL）")
    @GetMapping("/{id}/sync-status")
    public Result<Map<String, Object>> syncStatus(@PathVariable Long id) {
        Skill s = skillMapper.selectById(id);
        if (s == null) {
            return Result.fail(50001, "Skill 不存在");
        }
        if (!"GIT_URL".equals(s.getSourceType())) {
            return Result.fail(40000, "仅 GIT_URL 类型支持 sync-status");
        }
        Map<String, Object> body = new HashMap<>();
        body.put("id", s.getId());
        body.put("name", s.getName());
        body.put("sourceUrl", s.getSourceUrl());
        body.put("sourceRef", s.getSourceRef());
        body.put("tokenHint", s.getTokenHint());
        body.put("lastSyncAt", s.getLastSyncAt());
        body.put("lastSyncStatus", s.getLastSyncStatus());
        body.put("lastSyncError", s.getLastSyncError());
        body.put("lastCommitSha", s.getLastCommitSha());
        return Result.ok(body);
    }

    private void syncTags(Long skillId, String tagsJson) {
        // 先删旧
        skillTagMapper.delete(new LambdaQueryWrapper<SkillTag>().eq(SkillTag::getSkillId, skillId));
        if (StrUtil.isBlank(tagsJson)) return;
        List<String> tagNames = new ArrayList<>();
        try {
            tagNames = cn.hutool.json.JSONUtil.toList(tagsJson, String.class);
        } catch (Exception ignored) {
        }
        for (String name : tagNames) {
            if (StrUtil.isBlank(name)) continue;
            String slug = TagServiceImpl.slugify(name);
            if (slug.isEmpty()) continue;
            com.meiya.skillsmap.entity.Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<com.meiya.skillsmap.entity.Tag>().eq(com.meiya.skillsmap.entity.Tag::getSlug, slug));
            if (tag == null) {
                tag = new com.meiya.skillsmap.entity.Tag();
                tag.setName(name);
                tag.setSlug(slug);
                tag.setSkillCount(0);
                tag.setCreateTime(LocalDateTime.now());
                tagMapper.insert(tag);
            }
            try {
                SkillTag st = new SkillTag();
                st.setSkillId(skillId);
                st.setTagId(tag.getId());
                skillTagMapper.insert(st);
            } catch (Exception ignored) { }
        }
    }

    /**
     * S24 + S25: 回填 USAGE 归类（admin 工具）
     * <p>默认行为：仅补 {@code usageCategoryId IS NULL} 的 skill（幂等安全）
     * <p>可选 {@code ?force=true}：覆盖已有归类（按当前启发式重算所有，危险操作）
     * <p>S25 新增 {@code ?override=true}：<b>仅</b>对 {@link CategoryUtil#MANUAL_OVERRIDES} 表中
     * 的 3 个 skill 强制覆盖，其余不动（用于"修历史误命中"场景）
     * <p>启发式与 seed 流程一致（CategoryUtil.guessUsageCode）
     *
     * @return {@code { scanned, updated, skipped, missingCategory, force, override }}
     */
    @Operation(summary = "S24+S25: 回填 USAGE 归类（force 全量 / override 仅修 3 误命中）")
    @PostMapping("/backfill-usage")
    public Result<Map<String, Object>> backfillUsage(
            @RequestParam(defaultValue = "false") boolean force,
            @RequestParam(defaultValue = "false") boolean override) {
        log.info("[admin][backfill-usage] start, force={}, override={}", force, override);
        List<Skill> all = skillMapper.selectList(
                new LambdaQueryWrapper<Skill>().isNotNull(Skill::getName));
        int scanned = 0;
        int updated = 0;
        int skipped = 0;
        int missingCategory = 0;
        for (Skill s : all) {
            scanned++;
            String nameKey = s.getName() == null ? "" : s.getName().toLowerCase();
            // S25: override 模式：仅对 MANUAL_OVERRIDES 表中 skill 强制覆盖
            if (override) {
                String targetCode = CategoryUtil.MANUAL_OVERRIDES.get(nameKey);
                if (targetCode == null) {
                    skipped++;
                    continue;
                }
                Long catId = CategoryUtil.categoryIdByUsageCode(categoryMapper, targetCode);
                if (catId == null) {
                    missingCategory++;
                    continue;
                }
                if (catId.equals(s.getUsageCategoryId())) {
                    // 幂等：已正确，不重复写
                    skipped++;
                    continue;
                }
                Skill upd = new Skill();
                upd.setId(s.getId());
                upd.setUsageCategoryId(catId);
                upd.setUpdateTime(LocalDateTime.now());
                skillMapper.updateById(upd);
                updated++;
                log.info("[admin][backfill-usage][override] skill={} -> {} (code={})",
                        s.getName(), catId, targetCode);
                continue;
            }
            // 原 force / 默认行为
            if (!force && s.getUsageCategoryId() != null) {
                skipped++;
                continue;
            }
            String usageCode = CategoryUtil.guessUsageCode(null, s.getName());
            Long catId = CategoryUtil.categoryIdByUsageCode(categoryMapper, usageCode);
            if (catId == null) {
                missingCategory++;
                log.warn("[admin][backfill-usage] skill={} 启发式命中 code={} 但 category 表缺失 id（rare）",
                        s.getName(), usageCode);
                continue;
            }
            Skill upd = new Skill();
            upd.setId(s.getId());
            upd.setUsageCategoryId(catId);
            upd.setUpdateTime(LocalDateTime.now());
            skillMapper.updateById(upd);
            updated++;
            log.info("[admin][backfill-usage] skill={} -> usageCategoryId={} (code={})",
                    s.getName(), catId, usageCode);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("scanned", scanned);
        resp.put("updated", updated);
        resp.put("skipped", skipped);
        resp.put("missingCategory", missingCategory);
        resp.put("force", force);
        resp.put("override", override);
        log.info("[admin][backfill-usage] done, {}", resp);
        return Result.ok(resp);
    }
}
