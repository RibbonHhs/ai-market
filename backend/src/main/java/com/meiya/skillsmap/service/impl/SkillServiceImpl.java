package com.meiya.skillsmap.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meiya.skillsmap.common.BizCode;
import com.meiya.skillsmap.common.BizException;
import com.meiya.skillsmap.common.ListResult;
import com.meiya.skillsmap.entity.Category;
import com.meiya.skillsmap.entity.Skill;
import com.meiya.skillsmap.entity.Tag;
import com.meiya.skillsmap.entity.User;
import com.meiya.skillsmap.mapper.CategoryMapper;
import com.meiya.skillsmap.mapper.SkillMapper;
import com.meiya.skillsmap.mapper.TagMapper;
import com.meiya.skillsmap.mapper.UserMapper;
import com.meiya.skillsmap.request.SkillQueryRequest;
import com.meiya.skillsmap.response.SkillVO;
import com.meiya.skillsmap.service.SkillService;
import com.meiya.skillsmap.service.SkillStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import java.io.IOException;
import java.io.OutputStream;

@Service
public class SkillServiceImpl extends ServiceImpl<SkillMapper, Skill> implements SkillService {

    private static final Logger log = LoggerFactory.getLogger(SkillServiceImpl.class);

    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;
    private final com.meiya.skillsmap.mapper.FavoriteMapper favoriteMapper;
    private final com.meiya.skillsmap.service.SkillStorageService skillStorageService;

    public SkillServiceImpl(CategoryMapper categoryMapper, TagMapper tagMapper, UserMapper userMapper,
                            com.meiya.skillsmap.mapper.FavoriteMapper favoriteMapper,
                            com.meiya.skillsmap.service.SkillStorageService skillStorageService) {
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.userMapper = userMapper;
        this.favoriteMapper = favoriteMapper;
        this.skillStorageService = skillStorageService;
    }

    @Override
    public ListResult<SkillVO> listSkills(SkillQueryRequest q) {
        if (q == null) {
            q = new SkillQueryRequest();
        }
        long page = q.getPage() == null || q.getPage() < 1 ? 1 : q.getPage();
        long size = q.getSize() == null || q.getSize() < 1 ? 20 : Math.min(q.getSize(), 100);
        Page<Skill> mpPage = Page.of(page, size);

        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Skill::getStatus, "published");

        if (StrUtil.isNotBlank(q.getKeyword())) {
            String kw = q.getKeyword().trim();
            wrapper.and(w -> w.like(Skill::getName, kw)
                    .or().like(Skill::getDisplayName, kw)
                    .or().like(Skill::getDescription, kw));
        }
        // S21: categoryId 与 occupationCode 共用一个 SOC category 集合
        // 两者都展开到 sub-category 列表；任一指定则取并集；都不指定则不按 SOC 过滤
        java.util.List<Long> socIds = null;
        if (q.getCategoryId() != null) {
            // S11 修复：一级分类（parentId == null）需要展开到所有 sub-id 再 IN 查询，
            // 因为 skill.category_id 实际指向二级分类。
            // 二级分类时仅含自身，行为与原 eq() 等价。
            socIds = new java.util.ArrayList<>();
            socIds.add(q.getCategoryId());
            Category c = categoryMapper.selectById(q.getCategoryId());
            if (c != null && c.getParentId() == null) {
                List<Category> children = categoryMapper.selectList(
                        new LambdaQueryWrapper<Category>().eq(Category::getParentId, q.getCategoryId()));
                for (Category ch : children) {
                    socIds.add(ch.getId());
                }
            }
        }
        // S21: 按职业 code 过滤（支持一级 #01 / 二级 01-01）
        // 行为：传一级 (#01) → 展开到该一级下的所有 sub-category
        //      传二级 (01-01) → 仅精确匹配该 category
        // 兼容：旧一级（#01） 形式 AND 新二级（01-01） 形式
        if (StrUtil.isNotBlank(q.getOccupationCode())) {
            String occ = q.getOccupationCode().trim();
            List<Category> socCats = categoryMapper.selectList(
                    new LambdaQueryWrapper<Category>().eq(Category::getType, "SOC"));
            java.util.List<Long> occIds = new java.util.ArrayList<>();
            Long rootId = null;
            for (Category oc : socCats) {
                if (oc.getCode() == null) continue;
                if (oc.getCode().equals(occ)) {
                    // 直接匹配：可能是 #01 一级 或 01-01 二级
                    occIds.add(oc.getId());
                    if (oc.getParentId() == null) {
                        rootId = oc.getId();
                    }
                }
            }
            if (rootId != null) {
                // 一级职业 → 展开到所有 sub-category
                for (Category oc : socCats) {
                    if (rootId.equals(oc.getParentId())) {
                        occIds.add(oc.getId());
                    }
                }
            }
            if (occIds.isEmpty()) {
                return ListResult.empty(page, size);
            }
            // 与 categoryId 组合：取交集（都指定时），否则仅用 occIds
            if (socIds != null) {
                socIds.retainAll(occIds);
            } else {
                socIds = occIds;
            }
        }
        if (socIds != null) {
            if (socIds.isEmpty()) {
                return ListResult.empty(page, size);
            }
            wrapper.in(Skill::getCategoryId, socIds);
        }
        // S18: USAGE 维度过滤（与 SOC 独立，1:1 关系不展开子分类聚合）
        if (q.getUsageCategoryId() != null) {
            wrapper.eq(Skill::getUsageCategoryId, q.getUsageCategoryId());
        }
        if (StrUtil.isNotBlank(q.getSource())) {
            wrapper.eq(Skill::getSource, q.getSource());
        }
        String sort = q.getSort() == null ? "latest" : q.getSort();
        switch (sort) {
            case "hot":
            case "installs":
                wrapper.orderByDesc(Skill::getInstalls);
                break;
            case "rating":
                wrapper.orderByDesc(Skill::getRatingAvg);
                break;
            case "views":
                wrapper.orderByDesc(Skill::getViews);
                break;
            default:
                wrapper.orderByDesc(Skill::getCreateTime);
        }

        if (StrUtil.isNotBlank(q.getTagSlug())) {
            Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getSlug, q.getTagSlug()));
            if (tag == null) {
                return ListResult.empty(page, size);
            }
            wrapper.inSql(Skill::getId,
                    "SELECT skill_id FROM skill_tag WHERE tag_id = " + tag.getId());
        }

        Page<Skill> result = page(mpPage, wrapper);
        List<SkillVO> voList = result.getRecords().stream().map(this::toVO).toList();
        return ListResult.of(voList, result.getTotal(), page, size);
    }

    @Override
    public SkillVO getDetail(Long id, boolean increaseView) {
        Skill skill = getById(id);
        if (skill == null || !"published".equals(skill.getStatus())) {
            throw new BizException(BizCode.SKILL_NOT_FOUND);
        }
        if (increaseView) {
            skill.setViews((skill.getViews() == null ? 0 : skill.getViews()) + 1);
            updateById(skill);
        }
        return toVO(skill);
    }

    @Override
    public SkillVO getDetailBySlug(String slug, boolean increaseView) {
        Skill skill = getOne(new LambdaQueryWrapper<Skill>().eq(Skill::getSlug, slug));
        if (skill == null) {
            throw new BizException(BizCode.SKILL_NOT_FOUND);
        }
        return getDetail(skill.getId(), increaseView);
    }

    @Override
    public List<SkillVO> getHot(int limit) {
        return getHot(limit, "hot");
    }

    @Override
    public List<SkillVO> getHot(int limit, String sort) {
        int cap = Math.min(limit, 50);
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<Skill>().eq(Skill::getStatus, "published");
        String s = sort == null ? "hot" : sort;
        switch (s) {
            case "recent":
                wrapper.orderByDesc(Skill::getViews);
                break;
            case "featured":
                // 精选：featured=true 按评分降序
                wrapper.eq(Skill::getFeatured, true).orderByDesc(Skill::getRatingAvg);
                break;
            case "hot":
            case "installs":
            default:
                wrapper.orderByDesc(Skill::getInstalls);
                break;
        }
        return list(wrapper.last("LIMIT " + cap)).stream().map(this::toVO).toList();
    }

    @Override
    public List<SkillVO> getLatest(int limit) {
        return list(new LambdaQueryWrapper<Skill>()
                        .eq(Skill::getStatus, "published")
                        .orderByDesc(Skill::getCreateTime)
                        .last("LIMIT " + Math.min(limit, 50)))
                .stream().map(this::toVO).toList();
    }

    @Override
    public List<SkillVO> getFeatured(int limit) {
        return list(new LambdaQueryWrapper<Skill>()
                        .eq(Skill::getStatus, "published")
                        .eq(Skill::getFeatured, true)
                        .orderByDesc(Skill::getRatingAvg)
                        .last("LIMIT " + Math.min(limit, 50)))
                .stream().map(this::toVO).toList();
    }

    @Override
    public SkillVO toVO(Skill skill) {
        if (skill == null) {
            return null;
        }
        SkillVO vo = new SkillVO();
        BeanUtil.copyProperties(skill, vo, "tags");
        if (StrUtil.isNotBlank(skill.getTags())) {
            try {
                vo.setTags(JSONUtil.toList(skill.getTags(), String.class));
            } catch (Exception e) {
                log.warn("tags JSON parse failed: {}", skill.getTags());
                vo.setTags(Collections.emptyList());
            }
        } else {
            vo.setTags(Collections.emptyList());
        }
        if (skill.getCategoryId() != null) {
            Category cat = categoryMapper.selectById(skill.getCategoryId());
            if (cat != null) {
                vo.setCategoryName(cat.getName());
                vo.setCategorySlug(cat.getSlug());
            }
        }
        // S18: USAGE 维度
        if (skill.getUsageCategoryId() != null) {
            Category ucat = categoryMapper.selectById(skill.getUsageCategoryId());
            if (ucat != null) {
                vo.setUsageCategoryName(ucat.getName());
                vo.setUsageCategorySlug(ucat.getSlug());
                // S24: 嵌套 USAGE 节点（含父类目信息，前端 chip 配色按 parentCode 取色）
                com.meiya.skillsmap.response.UsageCategoryNodeVO node =
                        new com.meiya.skillsmap.response.UsageCategoryNodeVO();
                node.setId(ucat.getId());
                node.setCode(ucat.getCode());
                node.setName(ucat.getName());
                node.setSlug(ucat.getSlug());
                node.setDescription(ucat.getDescription());
                node.setParentId(ucat.getParentId());
                if (ucat.getParentId() != null) {
                    // 二级 USAGE：找父
                    Category parent = categoryMapper.selectById(ucat.getParentId());
                    if (parent != null) {
                        node.setParentCode(parent.getCode());
                        node.setParentName(parent.getName());
                    } else {
                        // 父缺失兜底
                        node.setParentCode(ucat.getCode());
                        node.setParentName(ucat.getName());
                    }
                } else {
                    // 一级 USAGE：父 = 自身
                    node.setParentCode(ucat.getCode());
                    node.setParentName(ucat.getName());
                }
                // S33: 直挂到该 usage_category_id 的 published skill 数
                Long count = baseMapper.selectCount(new LambdaQueryWrapper<Skill>()
                        .eq(Skill::getUsageCategoryId, ucat.getId())
                        .eq(Skill::getStatus, "published"));
                node.setSkillCount(count == null ? 0 : count.intValue());
                vo.setUsageCategory(node);
            }
        }
        // 上传者信息
        if (skill.getCreatedByUserId() != null) {
            User u = userMapper.selectById(skill.getCreatedByUserId());
            if (u != null) {
                vo.setCreatedByUserId(u.getId());
                vo.setCreatedByUsername(u.getUsername());
                vo.setCreatedByDisplayName(u.getDisplayName());
            }
        }
        // 当前用户是否已收藏
        try {
            com.meiya.skillsmap.security.AuthContext.UserPrincipal p = com.meiya.skillsmap.security.AuthContext.get();
            if (p != null) {
                Long count = favoriteMapper.selectCount(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.meiya.skillsmap.entity.Favorite>()
                                .eq(com.meiya.skillsmap.entity.Favorite::getUserId, p.getUserId())
                                .eq(com.meiya.skillsmap.entity.Favorite::getSkillId, skill.getId()));
                vo.setFavorited(count != null && count > 0);
            }
        } catch (Exception e) {
            log.debug("查询 favorited 失败: {}", e.getMessage());
        }
        return vo;
    }

    @Override
    public int countByCategory(Long categoryId) {
        return Math.toIntExact(count(new LambdaQueryWrapper<Skill>()
                .eq(Skill::getCategoryId, categoryId)
                .eq(Skill::getStatus, "published")));
    }

    @Override
    public void exportZip(Long skillId, OutputStream out) throws IOException {
        Skill skill = getById(skillId);
        if (skill == null) {
            throw new com.meiya.skillsmap.common.BizException(com.meiya.skillsmap.common.BizCode.SKILL_NOT_FOUND);
        }
        String name = skill.getName();
        java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(out);

        // 优先打包本地目录
        if (skillStorageService != null && skillStorageService.hasPackage(name)) {
            java.nio.file.Path skillDir = skillStorageService.rootPath().resolve(name);
            try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(skillDir)) {
                paths.filter(java.nio.file.Files::isRegularFile).forEach(p -> {
                    try {
                        String entryName = skillDir.relativize(p).toString().replace('\\', '/');
                        zos.putNextEntry(new java.util.zip.ZipEntry(entryName));
                        java.nio.file.Files.copy(p, zos);
                        zos.closeEntry();
                    } catch (java.io.IOException e) {
                        throw new RuntimeException("打包失败: " + p, e);
                    }
                });
            }
            zos.finish();
            return;
        }
        // 兜底：从 DB 字段动态重建 SKILL.md
        byte[] skillMdBytes = buildSkillMd(skill).getBytes(java.nio.charset.StandardCharsets.UTF_8);
        zos.putNextEntry(new java.util.zip.ZipEntry("SKILL.md"));
        zos.write(skillMdBytes);
        zos.closeEntry();
        zos.finish();
    }

    /**
     * 从 Skill DB 字段重新构造 SKILL.md 文本
     * 格式：--- frontmatter --- 正文
     */
    private String buildSkillMd(Skill skill) {
        StringBuilder fm = new StringBuilder();
        fm.append("---\n");
        fm.append("name: ").append(skill.getName() != null ? skill.getName() : "").append('\n');
        if (skill.getDescription() != null) {
            fm.append("description: ").append(skill.getDescription().replace("\n", " ")).append('\n');
        }
        if (skill.getLicense() != null) {
            fm.append("license: ").append(skill.getLicense()).append('\n');
        }
        if (skill.getAllowedTools() != null) {
            fm.append("allowed-tools: ").append(skill.getAllowedTools()).append('\n');
        }
        if (skill.getCompatibility() != null) {
            fm.append("compatibility: ").append(skill.getCompatibility()).append('\n');
        }
        // metadata 字段（含 version 等）
        java.util.Map<String, Object> metaMap = new java.util.HashMap<>();
        if (skill.getVersion() != null) metaMap.put("version", skill.getVersion());
        if (skill.getAuthorName() != null) {
            java.util.Map<String, String> author = new java.util.HashMap<>();
            author.put("name", skill.getAuthorName());
            if (skill.getAuthorEmail() != null) author.put("email", skill.getAuthorEmail());
            metaMap.put("author", author);
        }
        if (skill.getTags() != null && !skill.getTags().equals("[]")) {
            try {
                metaMap.put("tags", cn.hutool.json.JSONUtil.toList(skill.getTags(), String.class));
            } catch (Exception ignored) {}
        }
        if (!metaMap.isEmpty()) {
            fm.append("metadata:\n");
            metaMap.forEach((k, v) -> fm.append("  ").append(k).append(": ").append(v).append('\n'));
        }
        fm.append("---\n\n");
        fm.append(skill.getBody() != null ? skill.getBody() : "");
        return fm.toString();
    }
}
