package com.meiya.skillsmap.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meiya.skillsmap.entity.SkillTag;
import com.meiya.skillsmap.entity.Tag;
import com.meiya.skillsmap.mapper.SkillTagMapper;
import com.meiya.skillsmap.mapper.TagMapper;
import com.meiya.skillsmap.response.TagVO;
import com.meiya.skillsmap.service.TagService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

    private final SkillTagMapper skillTagMapper;

    public TagServiceImpl(SkillTagMapper skillTagMapper) {
        this.skillTagMapper = skillTagMapper;
    }

    @Override
    public List<TagVO> listAllWithCount() {
        List<Tag> all = list(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getName));
        if (all.isEmpty()) {
            return List.of();
        }
        List<SkillTag> allSt = skillTagMapper.selectList(null);
        Map<Long, Long> countMap = allSt.stream()
                .collect(Collectors.groupingBy(SkillTag::getTagId, Collectors.counting()));

        return all.stream().map(t -> {
            TagVO vo = new TagVO();
            BeanUtil.copyProperties(t, vo);
            vo.setSkillCount(countMap.getOrDefault(t.getId(), 0L).intValue());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public Tag getBySlug(String slug) {
        return getOne(new LambdaQueryWrapper<Tag>().eq(Tag::getSlug, slug));
    }

    @Override
    public Tag findOrCreate(String name) {
        if (StrUtil.isBlank(name)) {
            return null;
        }
        String slug = slugify(name);
        Tag existing = getOne(new LambdaQueryWrapper<Tag>().eq(Tag::getSlug, slug));
        if (existing != null) {
            return existing;
        }
        Tag t = new Tag();
        t.setName(name);
        t.setSlug(slug);
        t.setSkillCount(0);
        t.setCreateTime(LocalDateTime.now());
        save(t);
        return t;
    }

    public static String slugify(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase()
                .replaceAll("[^a-z0-9\\u4e00-\\u9fa5-]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
