package com.meiya.skillsmap.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * S24: USAGE 嵌套节点（前端 chip 用）
 * <p>含自身 id/code/name/slug/parentId + 父类目摘要 + 一级 USAGE code（用于配色 key）。
 * <p>父类目与自身在大多数场景下相同；只有当 skill 挂的是"二级 USAGE"时，parent 与自身不同。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UsageCategoryNodeVO {
    /** 子类目 id */
    private Long id;
    /** 子类目 code（如 PURPOSE-DEV-FRONTEND） */
    private String code;
    /** 子类目中文名（如 前端开发） */
    private String name;
    /** 子类目 slug */
    private String slug;
    /** 子类目 description（中文 1 句） */
    private String description;
    /** 父类目 id（如 PURPOSE-DEV） */
    private Long parentId;
    /** 父类目 code（一级 USAGE，用于前端配色 key） */
    private String parentCode;
    /** 父类目中文名（一级 USAGE，如 开发） */
    private String parentName;
    /** S33: 该 usage_category_id 直挂的 published skill 数（前端 chip "· N" + sidebar count）*/
    private Integer skillCount;

    public UsageCategoryNodeVO() {}

    public UsageCategoryNodeVO(Long id, String code, String name, String slug, String description,
                                Long parentId, String parentCode, String parentName, Integer skillCount) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.parentId = parentId;
        this.parentCode = parentCode;
        this.parentName = parentName;
        this.skillCount = skillCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getParentCode() { return parentCode; }
    public void setParentCode(String parentCode) { this.parentCode = parentCode; }
    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
    public Integer getSkillCount() { return skillCount; }
    public void setSkillCount(Integer skillCount) { this.skillCount = skillCount; }
}
