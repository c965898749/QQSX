package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;

@Data
@TableName("activity_boss")
public class ActivityBoss {
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    @TableField("go_into_num")
    private Integer goIntoNum;
    @TableField("boss_id")
    private Integer bossId;
    @TableField("boss_name")
    private String bossName;
    @TableField("detail_code")
    private String detailCode;
    @TableField("difficulty_level")
    private String difficultyLevel;

}