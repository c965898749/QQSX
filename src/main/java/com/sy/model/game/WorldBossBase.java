package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 世界BOSS基础模板
 */
@Data
@TableName("world_boss_base")
public class WorldBossBase {

    /** BOSS模板ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** BOSS名称 */
    @TableField("boss_name")
    private String bossName;

    /** 图标资源 */
    @TableField("boss_icon")
    private String bossIcon;

    /** BOSS简介（desc 是 MySQL 保留字，使用反引号包裹） */
    @TableField("`desc`")
    private String bossDesc;

    /** 基础血量 */
    @TableField("base_hp")
    private Long baseHp;

    /** 基础攻击 */
    @TableField("base_atk")
    private Long baseAtk;

    /** 基础防御 */
    @TableField("base_def")
    private Long baseDef;

    /** 基础速度（当天实际速度 = base_speed * world_boss_daily.speed_rate） */
    @TableField("base_speed")
    private Integer baseSpeed;

    /** 专属技能ID */
    @TableField("skill_id")
    private Integer skillId;

    /** 难度 1普通 2困难 3噩梦 */
    @TableField("difficulty_type")
    private Integer difficultyType;

    /** 最大参与人数 */
    @TableField("max_join_player")
    private Integer maxJoinPlayer;

    /** 是否启用模板 0禁用 1启用 */
    @TableField("is_active")
    private Integer isActive;

    /** 排序 */
    @TableField("sort")
    private Integer sort;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
