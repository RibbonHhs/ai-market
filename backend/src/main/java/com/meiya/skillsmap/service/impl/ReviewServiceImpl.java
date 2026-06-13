package com.meiya.skillsmap.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.meiya.skillsmap.common.BizCode;
import com.meiya.skillsmap.common.BizException;
import com.meiya.skillsmap.common.ListResult;
import com.meiya.skillsmap.entity.Review;
import com.meiya.skillsmap.entity.Skill;
import com.meiya.skillsmap.entity.User;
import com.meiya.skillsmap.mapper.ReviewMapper;
import com.meiya.skillsmap.mapper.SkillMapper;
import com.meiya.skillsmap.mapper.UserMapper;
import com.meiya.skillsmap.response.ReviewVO;
import com.meiya.skillsmap.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReviewServiceImpl extends ServiceImpl<ReviewMapper, Review> implements ReviewService {

    private final SkillMapper skillMapper;
    private final UserMapper userMapper;

    public ReviewServiceImpl(SkillMapper skillMapper, UserMapper userMapper) {
        this.skillMapper = skillMapper;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public Review submit(Long skillId, Long userId, Integer rating, String comment) {
        if (skillId == null || userId == null) {
            throw new BizException(BizCode.BAD_REQUEST, "skillId/userId 不能为空");
        }
        if (rating == null || rating < 1 || rating > 5) {
            throw new BizException(BizCode.BAD_REQUEST, "rating 必须在 1-5 之间");
        }
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) {
            throw new BizException(BizCode.SKILL_NOT_FOUND);
        }
        Review existing = getOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getSkillId, skillId)
                .eq(Review::getUserId, userId));
        LocalDateTime now = LocalDateTime.now();
        if (existing == null) {
            Review r = new Review();
            r.setSkillId(skillId);
            r.setUserId(userId);
            r.setRating(rating);
            r.setComment(comment);
            r.setCreateTime(now);
            r.setUpdateTime(now);
            save(r);
        } else {
            existing.setRating(rating);
            existing.setComment(comment);
            existing.setUpdateTime(now);
            updateById(existing);
        }
        refreshSkillRating(skillId);
        return getOne(new LambdaQueryWrapper<Review>()
                .eq(Review::getSkillId, skillId)
                .eq(Review::getUserId, userId));
    }

    @Override
    public ListResult<ReviewVO> listBySkill(Long skillId, Long page, Long size) {
        long p = page == null || page < 1 ? 1 : page;
        long s = size == null || size < 1 ? 20 : Math.min(size, 100);
        Page<Review> mpPage = Page.of(p, s);
        Page<Review> result = page(mpPage, new LambdaQueryWrapper<Review>()
                .eq(Review::getSkillId, skillId)
                .orderByDesc(Review::getCreateTime));
        if (result.getRecords().isEmpty()) {
            return ListResult.empty(p, s);
        }
        List<Long> userIds = result.getRecords().stream().map(Review::getUserId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        List<ReviewVO> voList = result.getRecords().stream().map(r -> {
            ReviewVO vo = new ReviewVO();
            vo.setId(r.getId());
            vo.setSkillId(r.getSkillId());
            vo.setUserId(r.getUserId());
            vo.setRating(r.getRating());
            vo.setComment(r.getComment());
            vo.setCreateTime(r.getCreateTime());
            User u = userMap.get(r.getUserId());
            if (u != null) {
                vo.setUsername(u.getUsername());
                vo.setUserAvatar(u.getAvatar());
            }
            return vo;
        }).toList();
        return ListResult.of(voList, result.getTotal(), p, s);
    }

    @Override
    @Transactional
    public void refreshSkillRating(Long skillId) {
        List<Review> all = list(new LambdaQueryWrapper<Review>().eq(Review::getSkillId, skillId));
        if (all.isEmpty()) {
            Skill s = skillMapper.selectById(skillId);
            if (s != null) {
                s.setRatingAvg(0.0);
                s.setRatingCount(0);
                skillMapper.updateById(s);
            }
            return;
        }
        double avg = all.stream().mapToInt(Review::getRating).average().orElse(0.0);
        Skill s = skillMapper.selectById(skillId);
        if (s != null) {
            s.setRatingAvg(Math.round(avg * 100.0) / 100.0);
            s.setRatingCount(all.size());
            skillMapper.updateById(s);
        }
    }
}
