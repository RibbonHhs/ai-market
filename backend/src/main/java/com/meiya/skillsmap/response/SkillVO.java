package com.meiya.skillsmap.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SkillVO {
    private Long id;
    private String name;
    private String slug;
    private String displayName;
    private String description;
    private String body;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    // S18: USAGE 维度
    private Long usageCategoryId;
    private String usageCategoryName;
    private String usageCategorySlug;
    // S24: USAGE 嵌套对象（前端 chip 用）— 含父类目
    private UsageCategoryNodeVO usageCategory;
    private List<String> tags;
    private String license;
    private String allowedTools;
    private String compatibility;
    private String version;
    private String homepage;
    private String authorName;
    private String authorEmail;
    private String authorGithub;
    private String icon;
    private String source;
    private String installCommand;
    private String downloadUrl;
    private Long packageSize;
    private Integer stars;
    private Integer installs;
    private Integer views;
    private Double ratingAvg;
    private Integer ratingCount;
    private String status;
    private Boolean featured;
    private Boolean favorited;
    private Long createdByUserId;
    private String createdByUsername;
    private String createdByDisplayName;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    // ===== Sprint S02: Git 源字段 =====
    /** 来源类型: LOCAL_ZIP / LOCAL_FILE / GIT_URL / null */
    private String sourceType;
    /** Git 仓库 URL（仅 GIT_URL 用） */
    private String sourceUrl;
    /** Branch / Tag / Commit SHA */
    private String sourceRef;
    /** Token 脱敏提示，前端展示用 */
    private String tokenHint;
    /** 最近一次成功同步时间 */
    private LocalDateTime lastSyncAt;
    /** 最近一次同步状态: success / failed / syncing */
    private String lastSyncStatus;
    /** 最近一次同步错误信息 */
    private String lastSyncError;
    /** 最近一次同步后的 HEAD commit SHA */
    private String lastCommitSha;

    /**
     * 加密后的 token 字段，前端永不返回。
     * 用 {@link JsonIgnore} 防止 Jackson 序列化。
     */
    @JsonIgnore
    private String sourceTokenEnc;

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
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getCategorySlug() { return categorySlug; }
    public void setCategorySlug(String categorySlug) { this.categorySlug = categorySlug; }
    public Long getUsageCategoryId() { return usageCategoryId; }
    public void setUsageCategoryId(Long usageCategoryId) { this.usageCategoryId = usageCategoryId; }
    public String getUsageCategoryName() { return usageCategoryName; }
    public void setUsageCategoryName(String usageCategoryName) { this.usageCategoryName = usageCategoryName; }
    public String getUsageCategorySlug() { return usageCategorySlug; }
    public void setUsageCategorySlug(String usageCategorySlug) { this.usageCategorySlug = usageCategorySlug; }
    public UsageCategoryNodeVO getUsageCategory() { return usageCategory; }
    public void setUsageCategory(UsageCategoryNodeVO usageCategory) { this.usageCategory = usageCategory; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getLicense() { return license; }
    public void setLicense(String license) { this.license = license; }
    public String getAllowedTools() { return allowedTools; }
    public void setAllowedTools(String allowedTools) { this.allowedTools = allowedTools; }
    public String getCompatibility() { return compatibility; }
    public void setCompatibility(String compatibility) { this.compatibility = compatibility; }
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
    public Boolean getFavorited() { return favorited; }
    public void setFavorited(Boolean favorited) { this.favorited = favorited; }
    public Long getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(Long createdByUserId) { this.createdByUserId = createdByUserId; }
    public String getCreatedByUsername() { return createdByUsername; }
    public void setCreatedByUsername(String createdByUsername) { this.createdByUsername = createdByUsername; }
    public String getCreatedByDisplayName() { return createdByDisplayName; }
    public void setCreatedByDisplayName(String createdByDisplayName) { this.createdByDisplayName = createdByDisplayName; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceUrl() { return sourceUrl; }
    public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }
    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }
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
    public String getSourceTokenEnc() { return sourceTokenEnc; }
    public void setSourceTokenEnc(String sourceTokenEnc) { this.sourceTokenEnc = sourceTokenEnc; }
}
