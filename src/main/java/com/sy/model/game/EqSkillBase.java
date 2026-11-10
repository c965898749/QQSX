package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 装备技能表（技能池静态定义）。
 * tier：0低 1中 2高（通用池档位）；scope：0通用池 1英雄池。
 * hero_id：英雄专属技能对应 characters.id；通用技能为 NULL。
 * 本期 effect_type/effect_value 仅预留，不接入战斗。
 */
@Data
@TableName("eq_skill_base")
public class EqSkillBase {
    @TableId(value = "skill_id", type = IdType.AUTO)
    private Integer skillId;
    @TableField("name")
    private String name;
    @TableField("tier")
    private Integer tier;
    @TableField("hero_id")
    private String heroId;
    @TableField("scope")
    private Integer scope;
    @TableField("description")
    private String description;
    @TableField("weight")
    private Integer weight;
    @TableField("effect_type")
    private Integer effectType;
    @TableField("effect_value")
    private BigDecimal effectValue;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;
}
