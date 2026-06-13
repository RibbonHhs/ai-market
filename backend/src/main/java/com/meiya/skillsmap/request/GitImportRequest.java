package com.meiya.skillsmap.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Git URL 导入请求（Sprint S02）
 * <p>对应 {@code POST /api/admin/skills/from-git}
 */
public class GitImportRequest {

    /** Git 仓库 URL（必填） */
    @NotBlank
    @Size(max = 500)
    private String url;

    /** Branch / Tag / Commit SHA（可选，默认 main） */
    @Size(max = 200)
    private String ref;

    /** Username（私有仓库，可选） */
    @Size(max = 100)
    private String username;

    /** Personal Access Token（私有仓库，可选；后端 Jasypt 加密） */
    @Size(max = 500)
    private String token;

    /** 跳过 TLS 证书校验（自建 Gitea / GitLab 用） */
    private Boolean insecureSkipTls = false;

    /** 单次请求的并发 clone 上限（可选，覆盖全局配置） */
    private Integer maxConcurrent;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Boolean getInsecureSkipTls() { return insecureSkipTls; }
    public void setInsecureSkipTls(Boolean insecureSkipTls) { this.insecureSkipTls = insecureSkipTls; }
    public Integer getMaxConcurrent() { return maxConcurrent; }
    public void setMaxConcurrent(Integer maxConcurrent) { this.maxConcurrent = maxConcurrent; }
}
