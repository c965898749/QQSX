package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@Data
@TableName("mine_rob_log")
public class MineRobLog {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("attacker_user_id")
    private Integer attackerUserId;

    @TableField("target_user_id")
    private Integer targetUserId;

    @TableField("rob_silver")
    private Integer robSilver;

    @TableField("left_silver")
    private Integer leftSilver;

    @TableField("create_time")
    private Date createTime;

    @TableField("fight_id")
    private String fightId;

    @TableField(exist = false)
    private String nickname;
    @TableField(exist = false)
    private String gameImg;
    @TableField(exist = false)
    private String timeStr;
}