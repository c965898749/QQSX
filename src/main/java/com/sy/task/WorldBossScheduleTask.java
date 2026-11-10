package com.sy.task;

import com.sy.mapper.game.GameNoticeMapper;
import com.sy.model.game.GameNotice;
import com.sy.model.game.WorldBossBase;
import com.sy.model.game.WorldBossDaily;
import com.sy.service.GameServiceService;
import com.sy.service.WorldBossService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    /** 世界BOSS每日开启时间 09:00 */
    private static final LocalTime OPEN_TIME = LocalTime.of(9, 0);
    /** 世界BOSS每日结束时间 23:00 */
    private static final LocalTime CLOSE_TIME = LocalTime.of(23, 0);

    /**
     * 每天 00:00 生成当天世界BOSS排班：
     * <p>已存在则把 hp_rate/atk_rate/speed_rate 复位为满值(1.00)并刷新时间窗；不存在则从启用模板按日期轮换新增。
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void createWorldBossDailySchedule() {
        LocalDate today = LocalDate.now();
        log.info("世界BOSS每日排班任务开始，date={}", today);
        try {
            WorldBossDaily daily = worldBossService.prepareDailySchedule(today, OPEN_TIME, CLOSE_TIME);
            log.info("世界BOSS每日排班任务结束，date={}，result={}", today, daily != null);
        } catch (Exception e) {
            log.error("世界BOSS每日排班任务异常，date=" + today, e);
        }
    }

    /**
     * 每天 09:00（世界BOSS开启时刻）发送全服通知。
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void announceWorldBossOpen() {
        LocalDate today = LocalDate.now();
        log.info("世界BOSS开启通知任务开始，date={}", today);
        try {
            WorldBossDaily daily = worldBossService.getScheduleByDate(today);
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
