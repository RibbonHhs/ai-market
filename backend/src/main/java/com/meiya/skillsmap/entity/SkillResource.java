package com.meiya.skillsmap.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

@TableName("skill_resource")
public class SkillResource implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("skill_id")
    private Long skillId;

    @TableField("kind")
    private String kind;

    @TableField("path")
    private String path;

    @TableField("size")
    private Long size;

    @TableField("mime")
    private String mime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
    public String getMime() { return mime; }
    public void setMime(String mime) { this.mime = mime; }
}
