package com.meiya.skillsmap.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SkillsMap 自定义配置：文件存储 + 可选 Git 同步
 */
@Configuration
@ConfigurationProperties(prefix = "skillsmap.storage")
public class StorageProperties {

    /** Skill 包本地存储根目录（相对工作目录） */
    private String packagesPath = "./data/skill-packages";

    /** 上传文件最大字节数（默认 20MB） */
    private long maxFileSize = 20 * 1024 * 1024;

    /** Git 同步配置（可选） */
    private Git git = new Git();

    public String getPackagesPath() { return packagesPath; }
    public void setPackagesPath(String packagesPath) { this.packagesPath = packagesPath; }
    public long getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(long maxFileSize) { this.maxFileSize = maxFileSize; }
    public Git getGit() { return git; }
    public void setGit(Git git) { this.git = git; }

    public static class Git {
        /** 是否启用 Git 同步（默认 false，无 token 时优雅降级到仅本地） */
        private boolean enabled = false;
        /** Git 仓库 URL（支持 https://github.com/user/repo.git 或 git@github.com:user/repo.git） */
        private String repoUrl;
        /** 用户名（GitHub 用户名） */
        private String username;
        /** Personal Access Token（GitHub PAT，需 repo 写权限） */
        private String token;
        /** 分支名（默认 main） */
        private String branch = "main";
        /** 本地工作目录（clone 后的 working tree） */
        private String workDir = "./data/git-workdir";
        /** 提交作者显示名 */
        private String authorName = "SkillsMap";
        /** 提交作者邮箱 */
        private String authorEmail = "noreply@skillsmap.local";
        /** 推送前是否先 pull（默认 true，多实例部署防冲突） */
        private boolean pullBeforePush = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getRepoUrl() { return repoUrl; }
        public void setRepoUrl(String repoUrl) { this.repoUrl = repoUrl; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getBranch() { return branch; }
        public void setBranch(String branch) { this.branch = branch; }
        public String getWorkDir() { return workDir; }
        public void setWorkDir(String workDir) { this.workDir = workDir; }
        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
        public String getAuthorEmail() { return authorEmail; }
        public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }
        public boolean isPullBeforePush() { return pullBeforePush; }
        public void setPullBeforePush(boolean pullBeforePush) { this.pullBeforePush = pullBeforePush; }
    }
}

