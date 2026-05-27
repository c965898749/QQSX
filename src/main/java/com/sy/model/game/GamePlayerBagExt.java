package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.util.Date;
@Data
@TableName("game_player_bag_ext")
public class GamePlayerBagExt {
    @TableId(value = "user_id", type = IdType.AUTO)
    private Integer userId;
    @TableField("max_grid")
    private Integer maxGrid;
    @TableField("unlocked_grid")
    private Integer unlockedGrid;
    @TableField("expand_count")
    private Integer expandCount;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;

}