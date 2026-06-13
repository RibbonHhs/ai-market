package com.meiya.skillsmap.service;

import com.meiya.skillsmap.entity.Skill;
import com.meiya.skillsmap.request.GitImportRequest;
import com.meiya.skillsmap.response.GitImportResult;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;

/**
 * Git 源 Skill 服务接口（Sprint S02）
 * <p>负责：
 * <ul>
 *   <li>从任意 Git URL 浅克隆到 {@code data/skill-clones/}</li>
 *   <li>扫描仓库内所有含 SKILL.md 的子目录（Monorepo 自动拆分）</li>
 *   <li>解析 frontmatter → 入库 Skill 实体</li>
 *   <li>对已存在的 Git 源 skill 重新拉取（覆盖策略）</li>
 * </ul>
 *
 * <p>与 {@link GitSyncService}（推送主仓）职责完全独立，互不干扰。
 */
public interface SkillGitService {

    /**
     * 从 Git URL 导入 skill
     * @param req 导入请求
     * @return 导入结果（含 discovered / imported / skipped 计数）
     */
    GitImportResult importFromGit(GitImportRequest req) throws IOException, GitAPIException;

    /**
     * 同步单个 skill（已存在 source_type='GIT_URL' 的记录）
     * @param skillId Skill 主键
     * @return 同步结果
     */
    GitSyncResult syncSkill(Long skillId) throws IOException;

    /**
     * 同步结果（与 GitImportResult 不同，复用单体更新场景）
     */
    record GitSyncResult(Long id, String lastCommitSha, boolean changed, String message) {}
}
