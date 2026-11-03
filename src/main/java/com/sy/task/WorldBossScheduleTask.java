package com.sy.task;

import com.sy.service.WorldBossService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 世界BOSS定时任务。
 * <p>依赖启动类 {@code @EnableScheduling}（YimemApplication 已开启）。
 */
@Slf4j
@Component
public class WorldBossScheduleTask {

    @Autowired
    private WorldBossService worldBossService;

    /**
     * 每天 00:00 重置世界BOSS的血量、攻击、速度：
     * 将当天 world_boss_daily 的 hp_rate/atk_rate/speed_rate 复位为满值(1.00)。
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetWorldBossDaily() {
        LocalDate today = LocalDate.now();
        log.info("世界BOSS每日重置任务开始，date={}", today);
        try {
            boolean done = worldBossService.resetDailyBossStats(today);
            log.info("世界BOSS每日重置任务结束，date={}，result={}", today, done);
        } catch (Exception e) {
            log.error("世界BOSS每日重置任务异常，date=" + today, e);
        }
    }
}
