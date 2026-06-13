package com.meiya.skillsmap.response;

public class TagVO {
    private Long id;
    private String name;
    private String slug;
    private Integer skillCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public Integer getSkillCount() { return skillCount; }
    public void setSkillCount(Integer skillCount) { this.skillCount = skillCount; }
}
