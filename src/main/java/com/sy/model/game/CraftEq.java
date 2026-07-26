package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("craft_eq")
public class CraftEq {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("item_id")
    private Integer itemId;
    @TableField("type")
    private String type;
    @TableField("icon")
    private String icon;
    @TableField("level")
    private int level;
}