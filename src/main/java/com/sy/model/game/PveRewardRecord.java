package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

import java.util.Date;
@Data
@TableName("pve_reward_record")
public class PveRewardRecord {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("detail_code")
    private String detailCode;
    @TableField("star_level")
    private Byte starLevel;
    @TableField("difficulty_level")
    private String difficultyLevel;
    @TableField("reward_type")
    private String rewardType;
    @TableField("reward_amount")
    private Integer rewardAmount;
    @TableField("reward_desc")
    private String rewardDesc;
    @TableField("status")
    private Byte status;
    @TableField("create_time")
    private Date createTime;
    @TableField("update_time")
    private Date updateTime;
    @TableField("item_id")
    private Integer itemId;
    @TableField("prent")
    private Integer prent;
    @TableField("user_id")
    private Integer userId;
}