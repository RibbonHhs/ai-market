package com.meiya.skillsmap.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meiya.skillsmap.entity.Category;
import com.meiya.skillsmap.entity.Skill;
import com.meiya.skillsmap.mapper.CategoryMapper;
import com.meiya.skillsmap.mapper.SkillMapper;
import com.meiya.skillsmap.response.CategoryVO;
import com.meiya.skillsmap.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    private final SkillMapper skillMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;

    public CategoryServiceImpl(SkillMapper skillMapper, org.springframework.jdbc.core.JdbcTemplate jdbc) {
        this.skillMapper = skillMapper;
        this.jdbc = jdbc;
    }

    @Override
    public List<CategoryVO> listAllWithCount(String type) {
        List<Category> all = list(new LambdaQueryWrapper<Category>()
                .eq(type != null, Category::getType, type)
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId));
        if (all.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Long> directCount = directSkillCountByCategory();
        return all.stream().map(c -> {
            CategoryVO vo = new CategoryVO();
            BeanUtil.copyProperties(c, vo);
            vo.setSkillCount(aggregateCount(c, all, directCount));
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 一次查所有 published skill 的 category_id 分布（避免 N+1）。
     */
    private Map<Long, Long> directSkillCountByCategory() {
        List<Skill> skills = skillMapper.selectList(new LambdaQueryWrapper<Skill>()
                .eq(Skill::getStatus, "published"));
        return skills.stream()
                .filter(s -> s.getCategoryId() != null)
                .collect(Collectors.groupingBy(Skill::getCategoryId, Collectors.counting()));
    }

    /**
     * S12: L1 聚合自身 + 所有 L2 children 的 direct skillCount。
     * L2 / 其他节点保持原 direct count 不变。
     */
    private int aggregateCount(Category c, List<Category> all, Map<Long, Long> directCount) {
        if (c.getParentId() == null) {
            long own = directCount.getOrDefault(c.getId(), 0L);
            long children = all.stream()
                    .filter(ch -> ch.getParentId() != null && ch.getParentId().equals(c.getId()))
                    .mapToLong(ch -> directCount.getOrDefault(ch.getId(), 0L))
                    .sum();
            return (int) (own + children);
        }
        return directCount.getOrDefault(c.getId(), 0L).intValue();
    }

    @Override
    public Category getBySlug(String slug) {
        return getOne(new LambdaQueryWrapper<Category>().eq(Category::getSlug, slug));
    }

    @Override
    public void refreshAllCategoryCount() {
        list().forEach(c -> {
            long count = skillMapper.selectCount(new LambdaQueryWrapper<Skill>()
                    .eq(Skill::getCategoryId, c.getId())
                    .eq(Skill::getStatus, "published"));
            c.setSkillCount((int) count);
            c.setUpdateTime(LocalDateTime.now());
            updateById(c);
        });
    }

    @Override
    public List<CategoryVO> listTree(String type) {
        List<Category> all = list(new LambdaQueryWrapper<Category>()
                .eq(type != null, Category::getType, type)
                .orderByAsc(Category::getSortOrder)
                .orderByAsc(Category::getId));
        if (all.isEmpty()) return new ArrayList<>();
        Map<Long, Long> directCount = directSkillCountByCategory();
        Map<Long, CategoryVO> voMap = new java.util.LinkedHashMap<>();
        for (Category c : all) {
            CategoryVO vo = new CategoryVO();
            BeanUtil.copyProperties(c, vo);
            vo.setSkillCount(aggregateCount(c, all, directCount));
            vo.setChildren(new ArrayList<>());
            voMap.put(c.getId(), vo);
        }
        List<CategoryVO> roots = new ArrayList<>();
        for (Category c : all) {
            CategoryVO vo = voMap.get(c.getId());
            if (c.getParentId() == null) {
                roots.add(vo);
            } else {
                CategoryVO parent = voMap.get(c.getParentId());
                if (parent != null) parent.getChildren().add(vo);
                else roots.add(vo);
            }
        }
        return roots;
    }

    @Override
    public String findRedirect(String oldSlug) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT new_slug FROM category_slug_redirect WHERE old_slug = ? LIMIT 1", oldSlug);
            if (rows != null && !rows.isEmpty()) {
                Object newSlug = rows.get(0).get("new_slug");
                if (newSlug == null || newSlug.toString().equals(oldSlug)) return null;
                return newSlug.toString();
            }
        } catch (Exception ignored) {}
        Category c = getBySlug(oldSlug);
        if (c == null) return null;
        if (c.getSlug() != null && c.getSlug().equals(oldSlug)) return null;
        return c.getSlug();
    }
}
