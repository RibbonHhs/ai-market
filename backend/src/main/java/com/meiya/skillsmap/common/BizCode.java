package com.meiya.skillsmap.common;

public enum BizCode {

    SUCCESS(0, "ok"),

    BAD_REQUEST(40000, "请求参数错误"),
    UNAUTHORIZED(40100, "未认证"),
    TOKEN_INVALID(40101, "Token 无效"),
    TOKEN_EXPIRED(40102, "Token 已过期"),
    FORBIDDEN(40300, "权限不足"),
    NOT_FOUND(40400, "资源不存在"),
    CONFLICT(40900, "资源冲突"),

    SYSTEM_ERROR(500, "系统异常"),
    SKILL_NOT_FOUND(50001, "Skill 不存在"),
    CATEGORY_NOT_FOUND(50002, "分类不存在"),
    USER_NOT_FOUND(50003, "用户不存在"),
    USER_ALREADY_EXISTS(50004, "用户名已存在"),
    REVIEW_DUPLICATE(50005, "已评价过此 Skill"),
    REVIEW_FORBIDDEN(50006, "未登录或权限不足"),
    FAVORITE_DUPLICATE(50007, "已收藏"),
    FILE_PARSE_ERROR(50008, "文件解析失败"),

    // ---- Sprint S23: 公开 API 限流 (429) ----
    RATE_LIMITED(42900, "请求过于频繁，请稍后再试"),

    // ---- Sprint S02: Git 源 Skill 错误码 (50300~50309) ----
    GIT_SOURCE_DISABLED(50300, "Git 源功能未启用"),
    GIT_REPO_NOT_FOUND(50301, "仓库不存在或不可达"),
    GIT_AUTH_FAILED(50302, "Git 鉴权失败"),
    GIT_TLS_FAILED(50303, "SSL/TLS 证书校验失败"),
    GIT_NO_SKILL_FOUND(50304, "仓库内未发现符合 Agent Skills 规范的子目录"),
    GIT_CLONE_TIMEOUT(50305, "Clone 超时"),
    GIT_DISK_QUOTA(50306, "磁盘空间不足"),
    GIT_FILE_TOO_LARGE(50307, "单文件超过大小限制"),
    GIT_SUBMODULE_UNSUPPORTED(50308, "暂不支持 Git submodule"),
    GIT_INVALID_URL(50309, "Git URL 格式非法");

    private final int code;
    private final String message;

    BizCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
}
