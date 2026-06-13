package com.meiya.skillsmap.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meiya.skillsmap.entity.Tag;
import com.meiya.skillsmap.response.TagVO;

import java.util.List;

public interface TagService extends IService<Tag> {

    /**
     * 全标签（带 skill_count）
     */
    List<TagVO> listAllWithCount();

    /**
     * 通过 slug 查标签
     */
    Tag getBySlug(String slug);

    /**
     * 根据名字查或创建
     */
    Tag findOrCreate(String name);
}
