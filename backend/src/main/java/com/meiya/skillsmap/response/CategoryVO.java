package com.meiya.skillsmap.response;

public class CategoryVO {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String icon;
    private Integer sortOrder;
    private Integer skillCount;
    /** S04: 分类类型 — 'SOC' / 'DOMAIN' / null */
    private String type;
    /** S04: 稳定标识（如 '#01', '01-01'） */
    private String code;
    /** S04: 父类目 ID */
    private Long parentId;
    /** S04: 树形结构下的子节点（仅 tree 接口返回时填充） */
    private java.util.List<CategoryVO> children;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getSkillCount() { return skillCount; }
    public void setSkillCount(Integer skillCount) { this.skillCount = skillCount; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public java.util.List<CategoryVO> getChildren() { return children; }
    public void setChildren(java.util.List<CategoryVO> children) { this.children = children; }
}
