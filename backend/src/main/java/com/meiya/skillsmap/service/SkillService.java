package com.meiya.skillsmap.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meiya.skillsmap.common.ListResult;
import com.meiya.skillsmap.entity.Skill;
import com.meiya.skillsmap.request.SkillQueryRequest;
import com.meiya.skillsmap.response.SkillVO;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public interface SkillService extends IService<Skill> {

    /**
     * 通用列表查询
     */
    ListResult<SkillVO> listSkills(SkillQueryRequest query);

    /**
     * 通过 ID 查详情（自动 views+1）
     */
    SkillVO getDetail(Long id, boolean increaseView);

    /**
     * 通过 slug 查详情
     */
    SkillVO getDetailBySlug(String slug, boolean increaseView);

    /**
     * 热门（按 installs）
     */
    List<SkillVO> getHot(int limit);

    /**
     * 热门（带 sort：hot=installs desc / recent=views desc / featured=featured by rating desc）
     */
    List<SkillVO> getHot(int limit, String sort);

    /**
     * 最新（按 create_time）
     */
    List<SkillVO> getLatest(int limit);

    /**
     * 精选（管理员标记或 top rated）
     */
    List<SkillVO> getFeatured(int limit);

    /**
     * Entity -> VO
     */
    SkillVO toVO(Skill skill);

    /**
     * 统计指定分类的 skill 数量
     */
    int countByCategory(Long categoryId);

    /**
     * 把 Skill 打成 .skill zip 写入流
     * <p>优先打包本地 data/skill-packages/{name}/ 目录；否则从 DB 字段动态重建 SKILL.md
     */
    void exportZip(Long skillId, OutputStream out) throws IOException;
}

