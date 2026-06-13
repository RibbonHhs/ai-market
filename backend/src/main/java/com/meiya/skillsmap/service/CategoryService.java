package com.meiya.skillsmap.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.meiya.skillsmap.entity.Category;
import com.meiya.skillsmap.response.CategoryVO;

import java.util.List;

public interface CategoryService extends IService<Category> {

    /**
     * 全部分类（带 skill_count 冗余）。S18: type=null=全部, type='SOC'/'USAGE' 按维度过滤
     */
    List<CategoryVO> listAllWithCount(String type);

    /**
     * 通过 slug 查分类
     */
    Category getBySlug(String slug);

    /**
     * 重置所有分类的 skill_count
     */
    void refreshAllCategoryCount();

    /**
     * S04: 获取分类树形结构（type=SOC 时返回两级，一级 + sub-group）
     */
    List<CategoryVO> listTree(String type);

    /**
     * S04: 旧 slug → 新 slug 重定向（命中返回 newSlug，未命中返回 null）
     */
    String findRedirect(String oldSlug);
}
