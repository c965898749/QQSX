package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.util.Date;
@Data
@TableName("qq_shenxian_player_flyup")
public class QqShenxianPlayerFlyup {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("player_id")
    private Integer playerId;
    @TableField("flyup_times")
    private Integer flyupTimes;
    @TableField("total_level")
    private Integer totalLevel;
    @TableField("total_dan_consume")
    private Integer totalDanConsume;
    @TableField("item_id")
    private String itemId;
    @TableField("flyup_time")
    private Date flyupTime;

}