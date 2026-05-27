package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

@Data
@TableName("ceremonial_gift")
public class CeremonialGift {
    @TableId(value = "item_id", type = IdType.AUTO)
    private Integer itemId;
    @TableField("item_type")
    private Integer itemType;
    @TableField("weight")
    private Integer weight;
    @TableField("award")
    private Integer award;
    @TableField("txt")
    private String txt;
    @TableField("icon")
    private String icon;
    @TableField(exist = false)
    private String isSign;
    @TableField(exist = false)
    private Integer index;
}