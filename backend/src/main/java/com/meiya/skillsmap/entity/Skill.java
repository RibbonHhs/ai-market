package com.meiya.skillsmap.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("skill")
public class Skill implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("slug")
    private String slug;

    @TableField("display_name")
    private String displayName;

    @TableField("description")
    private String description;

    @TableField("body")
    private String body;

    @TableField("category_id")
    private Long categoryId;

    // S18: USAGE 维度 FK（nullable，可只挂 SOC 或只挂 USAGE 或都不挂）
    @TableField("usage_category_id")
    private Long usageCategoryId;

    @TableField("tags")
    private String tags;

    @TableField("license")
    private String license;

    @TableField("allowed_tools")
    private String allowedTools;

    @TableField("compatibility")
    private String compatibility;

    @TableField("metadata")
    private String metadata;

    @TableField("version")
    private String version;

    @TableField("homepage")
    private String homepage;

    @TableField("author_name")
    private String authorName;

    @TableField("author_email")
    private String authorEmail;

    @TableField("author_github")
    private String authorGithub;

    @TableField("icon")
    private String icon;

    @TableField("source")
    private String source;

    @TableField("install_command")
    private String installCommand;

    @TableField("download_url")
    private String downloadUrl;

    @TableField("package_size")
    private Long packageSize;

    @TableField("stars")
    private Integer stars;

    @TableField("installs")
    private Integer installs;

    @TableField("views")
    private Integer views;

    @TableField("rating_avg")
    private Double ratingAvg;

    @TableField("rating_count")
    private Integer ratingCount;

    @TableField("status")
    private String status;

    @TableField("featured")
    private Boolean featured;

    // ===== Sprint S02: Git 源字段 =====
    /** 来源类型: LOCAL_ZIP / LOCAL_FILE / GIT_URL / null (历史数据视作 LOCAL_ZIP) */
    @TableField("source_type")
    private String sourceType;

    /** Git 仓库 URL（仅 GIT_URL 用） */
    @TableField("source_url")
    private String sourceUrl;

    /** Branch / Tag / Commit SHA（仅 GIT_URL 用） */
    @TableField("source_ref")
    private String sourceRef;

    /** Jasypt 加密后的 username:token（仅 GIT_URL 用，私有仓库）；前端 @JsonIgnore */
    @TableField("source_token_enc")
    private String sourceTokenEnc;

    /** Token 脱敏提示，如 ghp_xx****abcd（仅展示用） */
    @TableField("token_hint")
    private String tokenHint;

    /** 最近一次成功同步时间（手动 / 定时） */
    @TableField("last_sync_at")
    private LocalDateTime lastSyncAt;

    /** 最近一次同步状态: success / failed / syncing */
    @TableField("last_sync_status")
    private String lastSyncStatus;

    /** 最近一次同步错误信息（失败时） */
    @TableField("last_sync_error")
    private String lastSyncError;

    /** 最近一次同步后的 HEAD commit SHA */
    @TableField("last_commit_sha")
    private String lastCommitSha;

    @TableField("created_by_user_id")
    private Long createdByUserId;

    // ===== Sprint S38: 用户上传端点新增字段 =====
    /** 上传者 user id（FK → user.id ON DELETE SET NULL）。
     *  与 created_by_user_id 解耦：MVP 暂存同值；ADMIN 代上传时二者不同。 */
    @TableField("uploader_user_id")
    private Long uploaderUserId;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getUsageCategoryId() { return usageCategoryId; }
    public void setUsageCategoryId(Long usageCategoryId) { this.usageCategoryId = usageCategoryId; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getLicense() { return license; }
    public void setLicense(String license) { this.license = license; }
    public String getAllowedTools() { return allowedTools; }
    public void setAllowedTools(String allowedTools) { this.allowedTools = allowedTools; }
    public String getCompatibility() { return compatibility; }
    public void setCompatibility(String compatibility) { this.compatibility = compatibility; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getHomepage() { return homepage; }
    public void setHomepage(String homepage) { this.homepage = homepage; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }
    public String getAuthorGithub() { return authorGithub; }
    public void setAuthorGithub(String authorGithub) { this.authorGithub = authorGithub; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getInstallCommand() { return installCommand; }
    public void setInstallCommand(String installCommand) { this.installCommand = installCommand; }
    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
    public Long getPackageSize() { return packageSize; }
    public void setPackageSize(Long packageSize) { this.packageSize = packageSize; }
    public Integer getStars() { return stars; }
    public void setStars(Integer stars) { this.stars = stars; }
    public Integer getInstalls() { return installs; }
    public void setInstalls(Integer installs) { this.installs = installs; }
    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views; }
    public Double getRatingAvg() { return ratingAvg; }
    public void setRatingAvg(Double ratingAvg) { this.ratingAvg = ratingAvg; }
    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean featured) { this.featured = featured; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
    public Long getUploaderUserId() { return uploaderUserId; }
    public void setUploaderUserId(Long uploaderUserId) { this.uploaderUserId = uploaderUserId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }
    public String getSourceTokenEnc() { return sourceTokenEnc; }
    public void setSourceTokenEnc(String sourceTokenEnc) { this.sourceTokenEnc = sourceTokenEnc; }
    public String getTokenHint() { return tokenHint; }
    public void setTokenHint(String tokenHint) { this.tokenHint = tokenHint; }
    public LocalDateTime getLastSyncAt() { return lastSyncAt; }
    public void setLastSyncAt(LocalDateTime lastSyncAt) { this.lastSyncAt = lastSyncAt; }
    public String getLastSyncStatus() { return lastSyncStatus; }
    public void setLastSyncStatus(String lastSyncStatus) { this.lastSyncStatus = lastSyncStatus; }
    public String getLastSyncError() { return lastSyncError; }
    public void setLastSyncError(String lastSyncError) { this.lastSyncError = lastSyncError; }
    public String getLastCommitSha() { return lastCommitSha; }
    public void setLastCommitSha(String lastCommitSha) { this.lastCommitSha = lastCommitSha; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
