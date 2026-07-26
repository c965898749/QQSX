package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("game_equip_inlay")
public class GameEquipInlay {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("user_id")
    private Integer userId;
    @TableField("equip_unique_id")
    private Integer equipUniqueId;
    @TableField("slot_index")
    private Integer slotIndex;
    @TableField("item_id")
    private Integer itemId;
    @TableField("extra_attr")
    private String extraAttr;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;
    @TableField("icon")
    private String icon;
    @TableField(exist = false)
    private Boolean isBind;
    @TableField(exist = false)
    private BigDecimal itemCount;
    @TableField(exist = false)
    private Integer level;
}