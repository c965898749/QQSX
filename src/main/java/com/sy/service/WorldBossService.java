package com.sy.service;

import com.sy.model.game.WorldBossBase;
import com.sy.model.game.WorldBossDaily;
import com.sy.model.game.WorldBossRecord;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 世界BOSS服务
 */
public interface WorldBossService {

    // ================= 基础模板 world_boss_base =================

    /** 查询所有启用的BOSS模板（is_active=1，按 sort 升序） */
    List<WorldBossBase> listActiveBossTemplates();

    /** 按ID查询BOSS模板 */
    WorldBossBase getBossTemplate(Integer id);

    /** 新增BOSS模板 */
    boolean saveBossTemplate(WorldBossBase template);

    /** 更新BOSS模板 */
    boolean updateBossTemplate(WorldBossBase template);

    /** 删除BOSS模板 */
    boolean removeBossTemplate(Integer id);

    // ================= 每日排班 world_boss_daily =================

    /** 查询指定日期的排班（boss_date 唯一，可能为 null） */
    WorldBossDaily getScheduleByDate(LocalDate date);

    /**
     * 带自恢复的排班查询：先按日期查，若当天无排班（如 00:00 定时任务因调度线程池卡死/停机错过未补跑）
     * 则兜底调用一次 prepareDailySchedule 生成当天排班后再返回。默认时间窗 09:00-23:00。
     *
     * @return 当天排班；无启用模板时仍可能为 null
     */
    WorldBossDaily getScheduleByDateWithFallback(LocalDate date);

    /** 查询指定日期已开启的排班（is_open=1） */
    List<WorldBossDaily> listOpenSchedules(LocalDate date);

    /** 新增排班 */
    boolean saveSchedule(WorldBossDaily schedule);

    /** 更新排班 */
    boolean updateSchedule(WorldBossDaily schedule);

    /** 删除排班 */
    boolean removeSchedule(Integer id);

    // ================= 伤害记录 world_boss_record =================

    /**
     * 累计一次伤害：存在 (bossDate, roleId) 记录则累加伤害与攻击次数并刷新最后攻击时间，否则新增。
     *
     * @param bossDate 战斗日期
     * @param roleId   玩家角色ID
     * @param roleName 玩家角色名
     * @param damage   本次伤害
     * @param hitCount 本次挑战次数（语义为“挑战次数”，每次成功挑战固定传 1）
     * @return 累计后的记录
     */
    WorldBossRecord accumulateDamage(LocalDate bossDate, Long roleId, String roleName, long damage, int hitCount);

    /** 查询指定日期伤害排行（total_damage 降序，取前 limit 条） */
    List<WorldBossRecord> rankList(LocalDate bossDate, int limit);

    /** 查询某玩家在指定日期的记录 */
    WorldBossRecord findRoleRecord(LocalDate bossDate, Long roleId);

    /**
     * 结算排名：按 total_damage 降序为前 rankScope 名回写 rank 字段。
     *
     * @return 实际参与排名（被回写）的记录数
     */
    int assignRanks(LocalDate bossDate, int rankScope);

    /** 标记奖励已领取（按记录ID） */
    boolean markRewardGot(Long id);

    // ================= 每日重置 / 挑战更新 =================

    /**
     * 每日重置：把指定日期的 world_boss_daily 的 hp_rate/atk_rate/speed_rate 复位为满值(1.00)，
     * 即 boss 血量/攻击/速度回到基准满值。当天无排班行则不处理返回 false。
     */
    boolean resetDailyBossStats(LocalDate date);

    /**
     * 每次挑战更新当天排班：存在则刷新 update_time（记录最近一次挑战）。
     * 真正的按伤害扣减血量需接入伤害来源后再扩展（当前接口未计算伤害）。
     */
    boolean touchDailyScheduleOnChallenge(LocalDate date);

    /**
     * 战斗结算回写：把当天排班的 hp_rate/atk_rate/speed_rate 更新为本次战斗后的倍率
     * （倍率 = boss战后最终三维 / world_boss_base 基准三维，调用方需 clamp 到 0~1）。
     * hp_rate 回写为 0 表示当日 BOSS 已被击杀。
     *
     * @return 当天有排班且更新成功返回 true
     */
    boolean writeDailyRates(LocalDate date, BigDecimal hpRate, BigDecimal atkRate, BigDecimal speedRate);

    /**
     * 每日生成当天世界BOSS排班（供定时任务调用）：
     * <p>若当天已有排班行：将 hp_rate/atk_rate/speed_rate 复位满值(1.00)，刷新开启/结束时间并置 is_open=1；
     * 若当天无行：从启用模板列表按日期轮换选一个 base_boss_id 新增一行（三率=1.00，is_open=1）。
     *
     * @param startTime 当天开启时间（为 null 时默认 09:00）
     * @param endTime   当天结束时间（为 null 时默认 23:00）
     * @return 生成/复位后的当天排班；无启用模板时返回 null
     */
    WorldBossDaily prepareDailySchedule(LocalDate date, LocalTime startTime, LocalTime endTime);
}
