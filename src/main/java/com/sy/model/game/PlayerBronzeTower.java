package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.util.Date;

@Data
@TableName("player_bronze_tower")
public class PlayerBronzeTower {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("player_id")
    private String playerId;
    @TableField("floor_num")
    private Integer floorNum;
    @TableField("is_get_reward")
    private Integer isGetReward;
    @TableField("pass_time")
    private Date passTime;
    @TableField("bronze_type")
    private String bronzeType;

}