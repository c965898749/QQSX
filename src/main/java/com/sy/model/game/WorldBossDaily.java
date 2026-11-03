package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 每日世界BOSS排班表
 */
@Data
@TableName("world_boss_daily")
public class WorldBossDaily {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 战斗日期（YYYY-MM-DD） */
    @TableField("boss_date")
    private LocalDate bossDate;

    /** 关联world_boss_base的BOSS模板ID */
    @TableField("base_boss_id")
    private Integer baseBossId;

    /** BOSS开启时间 */
    @TableField("start_time")
    private LocalTime startTime;

    /** BOSS关闭时间 */
    @TableField("end_time")
    private LocalTime endTime;

    /** 血量倍率，当天总血量=base_hp * hp_rate */
    @TableField("hp_rate")
    private BigDecimal hpRate;

    /** 攻击倍率 */
    @TableField("atk_rate")
    private BigDecimal atkRate;

    /** 速度倍率（当天实际速度 = world_boss_base.base_speed * speed_rate） */
    @TableField("speed_rate")
    private BigDecimal speedRate;

    /** 奖励组ID，关联reward表 */
    @TableField("reward_group_id")
    private Integer rewardGroupId;

    /** 当天是否开启 0关闭 1开启 */
    @TableField("is_open")
    private Integer isOpen;

    /** 备注，例如：周一神兽、周二魔神 */
    @TableField("remark")
    private String remark;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
