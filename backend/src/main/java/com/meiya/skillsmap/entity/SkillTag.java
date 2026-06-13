package com.meiya.skillsmap.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

@TableName("skill_tag")
public class SkillTag implements Serializable {

    @TableField("skill_id")
    private Long skillId;

    @TableField("tag_id")
    private Long tagId;

    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }
    public Long getTagId() { return tagId; }
    public void setTagId(Long tagId) { this.tagId = tagId; }
}
