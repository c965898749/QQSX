package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 技能书配置：技能书道具 -> 抽取池映射 + 槽位容量。
 * draw_scope：0 从通用池按 tier 抽；1 从英雄池按穿戴英雄 hero_id 抽。
 * hero_id：一般留 NULL（运行时取穿戴英雄）。
 */
@Data
@TableName("eq_skill_book")
public class EqSkillBook {
    @TableId(value = "book_item_id", type = IdType.INPUT)
    private Integer bookItemId;
    @TableField("tier")
    private Integer tier;
    @TableField("draw_scope")
    private Integer drawScope;
    @TableField("hero_id")
    private String heroId;
    @TableField("max_slot")
    private Integer maxSlot;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;
}
