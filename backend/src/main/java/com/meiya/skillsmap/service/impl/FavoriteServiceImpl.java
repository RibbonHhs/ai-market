package com.meiya.skillsmap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meiya.skillsmap.common.ListResult;
import com.meiya.skillsmap.entity.Favorite;
import com.meiya.skillsmap.entity.Skill;
import com.meiya.skillsmap.mapper.FavoriteMapper;
import com.meiya.skillsmap.mapper.SkillMapper;
import com.meiya.skillsmap.response.SkillVO;
import com.meiya.skillsmap.service.FavoriteService;
import com.meiya.skillsmap.service.SkillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FavoriteServiceImpl extends ServiceImpl<FavoriteMapper, Favorite> implements FavoriteService {

    private final SkillMapper skillMapper;
    private final SkillService skillService;

    public FavoriteServiceImpl(SkillMapper skillMapper, SkillService skillService) {
        this.skillMapper = skillMapper;
        this.skillService = skillService;
    }

    @Override
    public boolean toggle(Long userId, Long skillId) {
        Favorite existing = getOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getSkillId, skillId));
        if (existing != null) {
            removeById(existing.getId());
            return false;
        }
        Favorite f = new Favorite();
        f.setUserId(userId);
        f.setSkillId(skillId);
        f.setCreateTime(LocalDateTime.now());
        save(f);
        return true;
    }

    @Override
    public boolean isFavorited(Long userId, Long skillId) {
        if (userId == null || skillId == null) {
            return false;
        }
        return count(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getSkillId, skillId)) > 0;
    }

    @Override
    public ListResult<SkillVO> listMine(Long userId, Long page, Long size) {
        long p = page == null || page < 1 ? 1 : page;
        long s = size == null || size < 1 ? 20 : Math.min(size, 100);
        Page<Favorite> mpPage = Page.of(p, s);
        Page<Favorite> result = page(mpPage, new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .orderByDesc(Favorite::getCreateTime));
        if (result.getRecords().isEmpty()) {
            return ListResult.empty(p, s);
        }
        List<Long> skillIds = result.getRecords().stream().map(Favorite::getSkillId).toList();
        List<Skill> skills = skillMapper.selectBatchIds(skillIds).stream()
                .filter(sk -> "published".equals(sk.getStatus()))
                .collect(Collectors.toList());
        List<SkillVO> voList = skillIds.stream()
                .map(id -> skills.stream().filter(sk -> sk.getId().equals(id)).findFirst().orElse(null))
                .filter(Objects::nonNull)
                .map(skillService::toVO)
                .collect(Collectors.toList());
        return ListResult.of(voList, result.getTotal(), p, s);
    }
}
