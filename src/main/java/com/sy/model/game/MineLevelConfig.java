package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("mine_level_config")
public class MineLevelConfig {
    @TableId(type = IdType.AUTO)
    private Integer id;

    @TableField("mine_level")
    private Integer mineLevel;

    @TableField("hour_output")
    private Integer hourOutput;

    @TableField("max_capacity")
    private Integer maxCapacity;

    @TableField("upgrade_cost")
    private Integer upgradeCost;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}