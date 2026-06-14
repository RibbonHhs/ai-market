package com.meiya.skillsmap.response;

import java.time.LocalDateTime;

/**
 * S38: 用户上传 Skill 接口的响应体（与 PRD §4.1 字段对齐）
 */
public class SkillUploadResponse {

    private Long id;
    private String slug;
    private String name;
    private String version;
    private String status;
    private LocalDateTime createdAt;

    public SkillUploadResponse() {}

    public SkillUploadResponse(Long id, String slug, String name, String version,
                               String status, LocalDateTime createdAt) {
        this.id = id;
        this.slug = slug;
        this.name = name;
        this.version = version;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
