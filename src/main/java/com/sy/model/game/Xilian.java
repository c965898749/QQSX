package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("xilian")
public class Xilian {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("eq_id")
    private Integer eqId;
    @TableField("xilian")
    private Integer xilian;
    @TableField("quality")
    private Integer quality;
    @TableField("value")
    private String value;


}