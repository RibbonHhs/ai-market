package com.meiya.skillsmap.service;

import com.meiya.skillsmap.response.SkillUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * S38: 用户上传 Skill 包（zip）→ 立即可见。
 * <p>鉴权：调用方保证 ctx.userId 非空（本接口不对外暴露 AuthContext 依赖）。
 * <p>事务：DB 写入在事务边界内，zip 落盘与事务解耦（finally 清理临时目录）。
 */
public interface SkillUploadService {

    /**
     * 上传 .skill zip → 入库 → 返回 SkillUploadResponse
     * @param file   multipart zip 文件（≤ 10MB，必须含 SKILL.md）
     * @param userId 当前登录用户 ID（由 Controller 从 AuthContext 取）
     * @param categoryId SOC 二级分类 id（必填）
     * @param usageCategoryIds USAGE 维度 id 列表（可选）
     * @param tagSlugs 自动创建缺失 tag（可选）
     */
    SkillUploadResponse upload(MultipartFile file,
                               Long userId,
                               Long categoryId,
                               java.util.List<Long> usageCategoryIds,
                               java.util.List<String> tagSlugs);
}
