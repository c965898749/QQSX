package com.sy.controller.game;

import com.sy.model.game.WorldBossDaily;
import com.sy.model.resp.BaseResp;
import com.sy.service.WorldBossService;
import com.sy.tool.WorldBossSchemaHealer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 世界BOSS排班自检接口（与 CacheTestController 一样挂在 /test/** 下，不是给玩家用的，
 * 建议在 nginx/防火墙层只放行内网）。
 * <p>做这几个接口是为了「world_boss_daily 当天没新增行」这件事不再靠翻日志猜：
 * 一条 URL 就能看清有没有行、指向哪个 BOSS、模板表里有几条可用、以及上一次生成失败的真实异常。
 * 几个接口都是幂等的：只补建缺失的行，已有就原样读出来，绝不复位 hp_rate/atk_rate/speed_rate。
 */
@Slf4j
@RestController
@RequestMapping("/test/worldboss")
public class WorldBossOpsController {

    @Autowired
    private WorldBossService worldBossService;

    @Autowired
    private WorldBossSchemaHealer worldBossSchemaHealer;

    /**
     * 只看不动：GET http://&lt;内网IP&gt;:8889/test/worldboss/daily
     * <p>可选 date=2026-11-13 查指定日期，默认今天。
     */
    @GetMapping("daily")
    public BaseResp<String> daily(@RequestParam(required = false) String date) {
        BaseResp<String> baseResp = new BaseResp<>();
        try {
            baseResp.setData(worldBossService.dailyScheduleReport(parseDate(date)));
            baseResp.setSuccess(1);
        } catch (Exception e) {
            log.error("世界BOSS排班自检查询失败，date=" + date, e);
            baseResp.setSuccess(0);
            baseResp.setErrorMsg("查询失败：" + e.getMessage());
        }
        return baseResp;
    }

    /**
     * 一步到底：先把库结构/模板数据自愈到可用，再把 date 起 days 天的排班行全部补齐，最后把结论写在 data 里。
     * GET http://&lt;内网IP&gt;:8889/test/worldboss/daily/ensure?days=30
     * <p>不卡在任何一个时刻上：一次铺完后面一个多月，缺列它自己 ALTER，is_active 为 NULL 它自己补 1，
     * 同日重行它自己清，补完会看到「补齐 30 天新增 X 行」+ 当天那行的具体内容。
     */
    @GetMapping("daily/ensure")
    public BaseResp<String> ensureDaily(@RequestParam(required = false) String date,
                                         @RequestParam(defaultValue = "30") int days) {
        BaseResp<String> baseResp = new BaseResp<>();
        try {
            LocalDate day = parseDate(date);
            String healed = worldBossSchemaHealer.heal();
            int created = worldBossService.createFutureSchedules(day, days);
            String report = worldBossService.dailyScheduleReport(day);
            String gap = gapSummary(day, days);
            log.info("世界BOSS排班手动修复完成，补齐 {} 天新增 {} 行，{} | {}", days, created, healed, report);
            baseResp.setData("补齐 " + day + " 起 " + days + " 天，新增 " + created + " 行；" + gap
                    + " || " + healed + " || " + report);
            baseResp.setSuccess(1);
        } catch (Exception e) {
            log.error("世界BOSS排班手动修复异常，date=" + date, e);
            baseResp.setSuccess(0);
            baseResp.setErrorMsg("修复失败：" + e.getMessage());
        }
        return baseResp;
    }

    /**
     * 日期区间体检：GET http://&lt;内网IP&gt;:8889/test/worldboss/daily/range?days=30
     * <p>把从 date 起每天“有没有行 / 是哪个 BOSS / 三率”列成一行一行，补完以后拿这个确认铺齐了没有。
     */
    @GetMapping("daily/range")
    public BaseResp<String> dailyRange(@RequestParam(required = false) String date,
                                       @RequestParam(defaultValue = "30") int days) {
        BaseResp<String> baseResp = new BaseResp<>();
        try {
            LocalDate day = parseDate(date);
            baseResp.setData(gapSummary(day, days));
            baseResp.setSuccess(1);
        } catch (Exception e) {
            log.error("世界BOSS排班区间体检异常，date=" + date, e);
            baseResp.setSuccess(0);
            baseResp.setErrorMsg("查询失败：" + e.getMessage());
        }
        return baseResp;
    }

    /** 从 day 起 days 天的逐天现状；缺哪天直接写「无排班」，末尾给一个缺口总数 */
    private String gapSummary(LocalDate day, int days) {
        int span = days <= 0 ? 1 : Math.min(days, 120);
        StringBuilder sb = new StringBuilder();
        int missing = 0;
        for (int i = 0; i < span; i++) {
            LocalDate d = day.plusDays(i);
            WorldBossDaily row = worldBossService.getScheduleByDate(d);
            sb.append(d).append(row == null ? " 无排班；" : " id=" + row.getId() + " boss=" + row.getBaseBossId()
                    + " hp=" + row.getHpRate() + "；");
            if (row == null) {
                missing++;
            }
        }
        return span + " 天里缺 " + missing + " 天：" + sb;
    }

    /** 空则取今天；写错格式时给一句人话，而不是把 DateTimeParseException 堆栈抛给调用方 */
    private LocalDate parseDate(String date) {
        if (date == null || date.trim().isEmpty()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("date 格式应为 yyyy-MM-dd，收到的是：" + date);
        }
    }
}
