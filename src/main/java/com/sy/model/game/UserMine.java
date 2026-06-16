package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_mine")
public class UserMine {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Integer userId;
    @TableField("mine_level")
    private Integer mineLevel;
    @TableField("hour_output")
    private Integer hourOutput;
    @TableField("max_capacity")
    private Integer maxCapacity;
    @TableField("current_silver")
    private Integer currentSilver;
    @TableField("last_collect_time")
    private Date lastCollectTime;
    @TableField("last_login_time")
    private Date lastLoginTime;
    @TableField("last_rob_time")
    private Date lastRobTime;
    @TableField("mine_status")
    private Integer mineStatus;
    @TableField("create_time")
    private Date createTime;
    @TableField("create_time")
    private Date updateTime;



}