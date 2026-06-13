package com.meiya.skillsmap.request;

public class SkillQueryRequest {

    private String keyword;
    private Long categoryId;
    // S18: USAGE 维度过滤（与 categoryId 并列，独立过滤，不展开子分类）
    private Long usageCategoryId;
    // S21: SOC 一级职业 code（如 "#01" / "01-01"）按职业维度筛
    // 支持：精确匹配一级 code、sub code；传 null/空 = 不按职业过滤
    private String occupationCode;
    private String tagSlug;
    private String source;
    private String sort = "latest";
    private Long page = 1L;
    private Long size = 20L;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Long getUsageCategoryId() { return usageCategoryId; }
    public void setUsageCategoryId(Long usageCategoryId) { this.usageCategoryId = usageCategoryId; }
    public String getOccupationCode() { return occupationCode; }
    public void setOccupationCode(String occupationCode) { this.occupationCode = occupationCode; }
    public String getTagSlug() { return tagSlug; }
    public void setTagSlug(String tagSlug) { this.tagSlug = tagSlug; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
    public Long getPage() { return page; }
    public void setPage(Long page) { this.page = page; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
}
