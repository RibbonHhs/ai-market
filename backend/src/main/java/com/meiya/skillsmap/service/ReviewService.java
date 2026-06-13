package com.meiya.skillsmap.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meiya.skillsmap.common.ListResult;
import com.meiya.skillsmap.entity.Review;
import com.meiya.skillsmap.response.ReviewVO;

public interface ReviewService extends IService<Review> {

    /**
     * 提交评分（一用户一评，更新覆盖）
     */
    Review submit(Long skillId, Long userId, Integer rating, String comment);

    /**
     * 列出某 skill 的评价
     */
    ListResult<ReviewVO> listBySkill(Long skillId, Long page, Long size);

    /**
     * 重新计算 skill 的平均分与总数
     */
    void refreshSkillRating(Long skillId);
}
