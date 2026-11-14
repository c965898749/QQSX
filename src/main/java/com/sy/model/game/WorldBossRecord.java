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

    /**
     * 头像路径快照（与挑战时的 user.game_img 一致）。
     * <p>世界BOSS库为多服共用，跨服 roleId 回本服主库查会串成人，故头像/昵称/服务器名按记录冗余存储。
     */
    @TableField("img")
    private String img;

    /**
     * 等级快照（=挑战时的 user.lv）。
     * <p>榜单改为多服合并后，别服玩家的 roleId 回本服主库会查成另一个人，等级必须跟着记录存。
     */
    @TableField("role_lv")
    private Integer roleLv;

    /** 所属服务器名（取配置 worldboss.server-name，如“梦回西游”“再续前缘”）；名次与榜单已改为全服合并，该字段只用于判定记录归属哪个服（发奖只能由本服实例发） */
    @TableField("server")
    private String server;

    /** 累计伤害 */
    @TableField("total_damage")
    private Long totalDamage;

    /** 挑战次数（前端以“攻击次数”展示；每次成功世界BOSS挑战 +1） */
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
