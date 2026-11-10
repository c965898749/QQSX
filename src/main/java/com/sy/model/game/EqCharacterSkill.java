package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 装备已学技能实例（1 使用中 + 其余待使用）。
 * is_active：1使用中 / 0待使用，同一 eq_uuid 至多 1 条 =1。
 * 技能等级不落库，由后端按装备宝石实时计算后回填到 skillLevel（非数据库字段）。
 */
@Data
@TableName("eq_character_skill")
public class EqCharacterSkill {
    @TableId(value = "uuid", type = IdType.AUTO)
    private Integer uuid;
    @TableField("eq_uuid")
    private Integer eqUuid;
    @TableField("user_id")
    private Integer userId;
    @TableField("skill_id")
    private Integer skillId;
    @TableField("slot_index")
    private Integer slotIndex;
    @TableField("is_active")
    private Integer isActive;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;

    // ===== 非数据库字段：下发列表时按 skill_id 关联 eq_skill_base 回填，供前端直接展示 =====
    @TableField(exist = false)
    private String name;
    @TableField(exist = false)
    private Integer tier;
    @TableField(exist = false)
    private String description;
    @TableField(exist = false)
    private BigDecimal effectValue;
    @TableField(exist = false)
    private Integer skillLevel;
}
