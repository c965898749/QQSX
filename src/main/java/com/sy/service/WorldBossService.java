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

    /** 查询指定日期的排班（同日多条时取 id 最大的一条，可能为 null） */
    WorldBossDaily getScheduleByDate(LocalDate date);

    /**
     * 带自恢复的排班查询：先按日期查，若当天无排班（多为守护任务那一轮没跑成：停机跨过触发点、
     * 或调度线程池被其他任务卡死）则兜底调用一次 prepareDailySchedule 生成当天排班后再返回。默认时间窗 09:00-23:00。
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
     * 累计一次伤害：存在 (bossDate, roleId, server) 记录则累加伤害与攻击次数并刷新最后攻击时间，否则新增。
     *
     * @param bossDate 战斗日期
     * @param roleId   玩家角色ID
     * @param roleName 玩家角色名
     * @param roleImg  玩家头像路径（冗余存入 img列，供跨服展示）
     * @param roleLv   玩家等级（冗余存入 role_lv列；合并榜单下跨服 roleId 会重号，不能回本服主库查）
     * @param server   服务器名（写入 server列，榜单全服合并，但发奖靠它判定“哪条记录属于本服”）
     * @param damage   本次伤害
     * @param hitCount 本次挑战次数（语义为“挑战次数”，每次成功挑战固定传 1）
     * @return 累计后的记录
     */
    WorldBossRecord accumulateDamage(LocalDate bossDate, Long roleId, String roleName, String roleImg, Integer roleLv, String server, long damage, int hitCount);

    /**
     * 查询指定日期伤害排行（total_damage 降序，取前 limit 条）。
     *
     * @param server 服务器名；为空则全服合并（现行榜单口径），传名字只看本服
     */
    List<WorldBossRecord> rankList(LocalDate bossDate, int limit, String server);

    /**
     * 查询指定日期、指定服务器中伤害达到 minDamage 的全部记录（不限前100名，按伤害降序），
     * 用于伤害达标类额外奖励结算。
     */
    List<WorldBossRecord> listByMinDamage(LocalDate bossDate, String server, long minDamage);

    /**
     * 查询某玩家在指定日期的记录。
     * <p>世界BOSS库由多个服务器实例共用，而 userId 在各服会重复，因此取本服口径时
     * server 必须传自己的 worldboss.server-name（传 null 则不按服过滤，仅运维排查时用）。
     */
    WorldBossRecord findRoleRecord(LocalDate bossDate, Long roleId, String server);

    /**
     * 结算排名：按 total_damage 降序为前 rankScope 名回写 rank 字段。
     * <p>server 传 null = 全服合并名次（现行口径），各实例算出的名次完全一致，重复回写无副作用。
     *
     * @return 实际参与排名（被回写）的记录数
     */
    int assignRanks(LocalDate bossDate, int rankScope, String server);

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
     * 生成当天世界BOSS排班（现只给挑战入口的自恢复兜底用，定时任务已不再调它）：
     * <p>若当天已有排班行：将 hp_rate/atk_rate/speed_rate 复位满值(1.00)，刷新开启/结束时间并置 is_open=1；
     * 若当天无行：从启用模板列表按日期轮换选一个 base_boss_id 新增一行（三率=1.00，is_open=1）。
     * <p>注意“复位”会冲掉人工给某天调过的倍率/关掉的 is_open，所以不能拿来高频调；
     * 只要不在战斗时段调它（自恢复只在当天无行时才走到这里）就没这个风险。提前铺行请用 createFutureSchedules。
     *
     * @param startTime 当天开启时间（为 null 时默认 09:00）
     * @param endTime   当天结束时间（为 null 时默认 23:00）
     * @return 生成/复位后的当天排班；无可用模板时返回 null（具体原因看 dailyScheduleReport）
     */
    WorldBossDaily prepareDailySchedule(LocalDate date, LocalTime startTime, LocalTime endTime);

    /**
     * 一次往后补出 days 天的排班行（从 from 开始，含 from），已有的日期跳过不动。
     * <p>不依赖任何单次 cron 触发：把后面十几天的行提前灌好，进程哪怕连着几天没起来、或者线上跑的是旧包，
     * 玩家当天照样有 BOSS 可打。所有查询都是按 boss_date 过滤的，未来的行放着不产生任何影响。
     *
     * @return 本次真正新增了多少行（0 表示这些天都已经有了）
     */
    int createFutureSchedules(LocalDate from, int days);

    /**
     * 一行结论说清当天排班到底卡在哪儿：有没有行、行里指向哪个 BOSS、模板表里有多少可用模板，
     * 以及上一次生成失败时抓住的真实异常（表缺列的 Unknown column 会被包装成“另一实例已写入”之类误导话）。
     * <p>供日志与 /test/worldboss 运维接口直接读，不需要去翻日志。
     */
    String dailyScheduleReport(LocalDate date);
}
