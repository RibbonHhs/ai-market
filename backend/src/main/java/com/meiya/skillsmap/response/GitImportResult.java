package com.meiya.skillsmap.response;

import java.util.List;

/**
 * Git URL 导入结果（Sprint S02）
 * <p>对应 {@code POST /api/admin/skills/from-git} 的成功响应。
 */
public class GitImportResult {

    /** 仓库 URL（剥离 token 后） */
    private String repoUrl;

    /** 实际使用的 ref（branch/tag/sha） */
    private String ref;

    /** 浅克隆目标目录 */
    private String workdir;

    /** 发现的 skill 列表（含已导入 + 跳过） */
    private List<DiscoveredSkill> discovered;

    /** 发现的总数（含根 + 子目录） */
    private int totalDiscovered;

    /** 实际成功导入/更新的 skill 数 */
    private int totalImported;

    /** 跳过的子目录数（缺 name 字段 / 已存在的合法 skill 视为更新） */
    private int totalSkipped;

    /** 跳过的原因说明（每条一个 reason） */
    private List<String> skipReasons;

    public static class DiscoveredSkill {
        private String name;
        private String path;          // 仓库内相对路径
        private String description;
        private String version;
        private String action;        // "created" | "updated" | "skipped"
        private String skipReason;    // action=skipped 时填充
        private Long skillId;         // action=created/updated 时填充

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getSkipReason() { return skipReason; }
        public void setSkipReason(String skipReason) { this.skipReason = skipReason; }
        public Long getSkillId() { return skillId; }
        public void setSkillId(Long skillId) { this.skillId = skillId; }
    }

    public String getRepoUrl() { return repoUrl; }
    public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }
    public String getWorkdir() { return workdir; }
    public void setWorkdir(String workdir) { this.workdir = workdir; }
    public List<DiscoveredSkill> getDiscovered() { return discovered; }
    public void setDiscovered(List<DiscoveredSkill> discovered) { this.discovered = discovered; }
    public int getTotalDiscovered() { return totalDiscovered; }
    public void setTotalDiscovered(int totalDiscovered) { this.totalDiscovered = totalDiscovered; }
    public int getTotalImported() { return totalImported; }
    public void setTotalImported(int totalImported) { this.totalImported = totalImported; }
    public int getTotalSkipped() { return totalSkipped; }
    public void setTotalSkipped(int totalSkipped) { this.totalSkipped = totalSkipped; }
    public List<String> getSkipReasons() { return skipReasons; }
    public void setSkipReasons(List<String> skipReasons) { this.skipReasons = skipReasons; }
}
