package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("craft")
public class Craft {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("item_id_id")
    private Integer itemIdId;
    @TableField("material_count")
    private Integer materialCount;
    @TableField("target_id")
    private Integer targetId;
    @TableField("type")
    private String type;
    @TableField("target_type")
    private String targetType;

}