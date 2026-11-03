package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 世界BOSS玩家伤害记录
 */
@Data
@TableName("world_boss_record")
public class WorldBossRecord {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 对应哪一天的世界BOSS */
    @TableField("boss_date")
    private LocalDate bossDate;

    /** 玩家角色ID */
    @TableField("role_id")
    private Long roleId;

    /** 玩家角色名称 */
    @TableField("role_name")
    private String roleName;

    /** 累计伤害 */
    @TableField("total_damage")
    private Long totalDamage;

    /** 攻击次数 */
    @TableField("hit_count")
    private Integer hitCount;

    /** 本次排名（rank 是 MySQL 保留字，使用反引号包裹） */
    @TableField("`rank`")
    private Integer bossRank;

    /** 是否已领取奖励 0未领 1已领 */
    @TableField("is_reward_get")
    private Integer isRewardGet;

    /** 最后攻击时间 */
    @TableField("last_atk_time")
    private LocalDateTime lastAtkTime;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
