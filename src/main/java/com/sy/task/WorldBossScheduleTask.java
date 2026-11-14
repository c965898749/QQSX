package com.sy.task;

import com.sy.mapper.game.GameNoticeMapper;
import com.sy.model.game.GameNotice;
import com.sy.model.game.WorldBossBase;
import com.sy.model.game.WorldBossDaily;
import com.sy.service.GameServiceService;
import com.sy.service.WorldBossService;
import com.sy.tool.WorldBossNodeRegistry;
import com.sy.tool.WorldBossSchemaHealer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 世界BOSS定时任务。
 * <p>依赖启动类 {@code @EnableScheduling}（YimemApplication 已开启）。
 */
@Slf4j
@Component
public class WorldBossScheduleTask {

    @Autowired
    private WorldBossService worldBossService;

    @Autowired
    private GameNoticeMapper gameNoticeMapper;

    @Autowired
    private GameServiceService gameServiceService;

    @Autowired
    private WorldBossSchemaHealer worldBossSchemaHealer;

    /** 节点表预加载：heal 刚把 world_boss_server 建出来时靠它立刻重读，不靠快照过期 */
    @Autowired
    private WorldBossNodeRegistry worldBossNodeRegistry;

    /** 世界BOSS每日开启时间 09:00 */
    private static final LocalTime OPEN_TIME = LocalTime.of(9, 0);
    /** 世界BOSS每日结束时间 23:00 */
    private static final LocalTime CLOSE_TIME = LocalTime.of(23, 0);

    /**
     * 排班提前量（天）：守护任务一次把今天往后这么多天都灌好。
     * <p>只靠“到某个时刻跑一次”太脆：重启、调度池卡死、线上跑旧包都会让当天缺行；
     * 把后面的行提前铺好，“今天有没有数据”就不再是个时间点了。
     */
    @Value("${worldboss.schedule-ahead-days:20}")
    private int scheduleAheadDays;

    /**
     * 排班守护（也是唯一的生成入口）：每小时先做一次库结构自愈，再把「今天起 scheduleAheadDays 天」里缺的排班行全部补齐（只补建、不复位）。
     * <p>以前还有 01:00 / 08:30 两条 cron 专门生成当天，但它们只处理“今天”这一个日期，错过就是错过，
     * 而且会把手工调过的当天倍率冲掉；现在提前量由本任务统一维护，不再依赖任何具体时刻。
     * 挑战入口里虽然有自恢复，但那要有人真的去点世界BOSS 才会触发。这条守护把「有没有行」
     * 变成与定时任务、与玩家点击、与具体时刻都无关的兜底，并且一次铺出后面二十来天。
     * <p>结构自愈放在最前面且无条件执行，不只为了排班：world_boss_record 缺列时排班行照样是好的，
     * 走“补不出行才自愈”的分支永远碰不到它，伤害记录就会一直悄悄写不进去（本次线上故障就是这条）。
     * <p>执行频率做成配置 worldboss.schedule-guard-cron（默认每小时整点跑一次），线上想改频不用重编包。
     */
    @Scheduled(cron = "${worldboss.schedule-guard-cron:0 0 * * * ?}")
    public void guardWorldBossDailyRow() {
        LocalDate today = LocalDate.now();
        try {
            // 三张表按实体校验一遍：只有真做了变更才打一行，不把日志刷满屏
            String healed = worldBossSchemaHealer.heal();
            if (healed != null && !healed.contains("无需变更")) {
                log.warn("世界BOSS库结构自愈：{}", healed);
                // 节点两张表有可能就是这次刚建好的：不重读的话，本服身份最长还要等一个快照周期
                log.info("世界BOSS节点表重读：{}", worldBossNodeRegistry.reload());
            }
            int created = worldBossService.createFutureSchedules(today, scheduleAheadDays);
            if (created > 0) {
                log.info("世界BOSS排班守护：补齐今天起 {} 天，新增 {} 行", scheduleAheadDays, created);
            }
            if (worldBossService.getScheduleByDate(today) != null) {
                // 今天有行就到此为止（最多再加一次范围补建），不把日志刷满屏
                return;
            }
            // 自愈过了还是补不出行，把具体原因留在这一行里
            log.error("世界BOSS排班守护结果（仍无行就是真的补不上，看这一行里的具体原因）：{}",
                    worldBossService.dailyScheduleReport(today));
        } catch (Exception e) {
            log.error("世界BOSS排班守护任务异常，date=" + today, e);
        }
    }

    /**
     * 进程一起来就把世界BOSS库的结构与数据补到可用，并一次铺出今天往后 scheduleAheadDays 天的排班行。
     * <p>先 heal 再补建：列齐了、is_active 不再是 NULL，插入那一句才走得过去。
     * 补完玩家立刻就能打，不用等任何 cron，也不用管以前那天定时任务有没有跑成。
     * <p>末尾主动重读一次节点表：两个启动监听器（本任务的 heal 与 WorldBossNodeRegistry 的预加载）顺序不定，
     * 预加载跑在前面的话表还不存在，这一步把刚建好的 world_boss_server / _peer 读进来，本服名不用等 60 秒。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void ensureWorldBossDailyRowOnStartup() {
        LocalDate today = LocalDate.now();
        try {
            log.info("世界BOSS库启动自愈：{}", worldBossSchemaHealer.heal());
            int created = worldBossService.createFutureSchedules(today, scheduleAheadDays);
            log.info("世界BOSS排班启动自检：新增 {} 行（覆盖 {} ~ {}），{}",
                    created, today, today.plusDays(scheduleAheadDays - 1L), worldBossService.dailyScheduleReport(today));
            log.info("世界BOSS节点启动预加载：{}", worldBossNodeRegistry.reload());
        } catch (Exception e) {
            log.error("世界BOSS排班启动自检异常，date=" + today, e);
        }
    }

    /**
     * 每天 09:00（世界BOSS开启时刻）发送全服通知。
     * <p>用 WithFallback 版本：即使守护那一轮没跑成，开榜公告也不会因“无排班”静默消失。
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void announceWorldBossOpen() {
        LocalDate today = LocalDate.now();
        log.info("世界BOSS开启通知任务开始，date={}", today);
        try {
            WorldBossDaily daily = worldBossService.getScheduleByDateWithFallback(today);
            if (daily == null || daily.getBaseBossId() == null) {
                log.warn("世界BOSS开启通知：{} 无当天排班，跳过", today);
                return;
            }
            WorldBossBase boss = worldBossService.getBossTemplate(daily.getBaseBossId());
            if (boss == null) {
                log.warn("世界BOSS开启通知：{} 排班对应模板不存在(baseBossId={})，跳过", today, daily.getBaseBossId());
                return;
            }
            GameNotice gameNotice = new GameNotice();
            gameNotice.setDescription("世界BOSS【" + boss.getBossName() + "】已开启！挑战时间 "
                    + OPEN_TIME + "-" + CLOSE_TIME + "，全服勇士速来挑战！");
            gameNoticeMapper.insert(gameNotice);
            log.info("世界BOSS开启通知已广播：{}", boss.getBossName());
        } catch (Exception e) {
            log.error("世界BOSS开启通知任务异常，date=" + today, e);
        }
    }

    /**
     * 每天 23:05（世界BOSS结束 23:00 后留 5 分钟缓冲，避免与进行中的战斗冲突）进行伤害排行结算发奖。
     */
    @Scheduled(cron = "0 5 23 * * ?")
    public void settleWorldBossRewardSchedule() {
        LocalDate today = LocalDate.now();
        log.info("世界BOSS每日结算任务开始，date={}", today);
        try {
            gameServiceService.worldBossSendReward();
            log.info("世界BOSS每日结算任务结束，date={}", today);
        } catch (Exception e) {
            log.error("世界BOSS每日结算任务异常，date=" + today, e);
        }
    }
}
