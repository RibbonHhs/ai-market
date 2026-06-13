package com.meiya.skillsmap.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meiya.skillsmap.common.ListResult;
import com.meiya.skillsmap.entity.Favorite;
import com.meiya.skillsmap.response.SkillVO;

public interface FavoriteService extends IService<Favorite> {

    /**
     * 切换收藏（已收藏则取消，未收藏则添加）
     */
    boolean toggle(Long userId, Long skillId);

    /**
     * 当前用户是否已收藏
     */
    boolean isFavorited(Long userId, Long skillId);

    /**
     * 我的收藏列表
     */
    ListResult<SkillVO> listMine(Long userId, Long page, Long size);
}
