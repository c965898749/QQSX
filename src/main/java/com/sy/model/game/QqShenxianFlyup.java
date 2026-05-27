package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

@Data
@TableName("qq_shenxian_flyup")
public class QqShenxianFlyup {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("flyup_times")
    private Integer flyupTimes;
    @TableField("level_increase")
    private Integer levelIncrease;
    @TableField("current_consume")
    private Integer currentConsume;
    @TableField("total_consume")
    private Integer totalConsume;
    @TableField("gold")
    private Integer gold;
}