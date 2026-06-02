package com.sy.controller.game;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sy.mapper.UserMapper;
import com.sy.mapper.game.CeremonialGiftRecordMapper;
import com.sy.mapper.game.DailyViewFinshMapper;
import com.sy.mapper.game.GameFightMapper;
import com.sy.mapper.game.GameNoticeMapper;
import com.sy.model.game.CeremonialGiftRecord;
import com.sy.model.game.DailyViewFinsh;
import com.sy.model.game.GameNotice;
import com.sy.service.GameServiceService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

/**
 * XxlJob开发示例（Bean模式）
 *
 * 开发步骤：
 *      1、任务开发：在Spring Bean实例中，开发Job方法；
 *      2、注解配置：为Job方法添加注解 "@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 *      3、执行日志：需要通过 "XxlJobHelper.log" 打印执行日志；
 *      4、任务结果：默认任务结果为 "成功" 状态，不需要主动设置；如有诉求，比如设置任务结果为失败，可以通过 "XxlJobHelper.handleFail/handleSuccess" 自主设置任务结果；
 *
 * @author xuxueli 2019-12-11 21:52:51
 */
@Component
public class SampleXxlJob {
    //定时器
    @Autowired
    private GameFightMapper gameFightMapper;
    @Autowired
    private GameServiceService gameServiceService;
    @Autowired
    private GameNoticeMapper gameNoticeMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DailyViewFinshMapper dailyViewFinshMapper;
    @Autowired
    private CeremonialGiftRecordMapper giftRecordMapper;

    /**
     * 1、简单任务示例（Bean模式）
     */
    @XxlJob("demoJobHandler")
    public void demoJobHandler() throws Exception {
        System.out.println("XXL-JOB, Hello World.");
        for (int i = 0; i < 5; i++) {
            XxlJobHelper.log("beat at:" + i);
            TimeUnit.SECONDS.sleep(2);
        }
        // default success
    }

    /**
     * 每天定时清除游戏过多消息
     */
    @XxlJob("ClearExcessiveGameMessagesDaily")
    public  void pushsite() {
        // 分批删除游戏战斗记录，避免超时
        int totalDeleted = 0;
        int deletedCount;
        do {
            deletedCount = gameFightMapper.deleteByTimeBatch();
            totalDeleted += deletedCount;
            XxlJobHelper.log("本次删除游戏战斗记录: " + deletedCount + " 条，累计删除: " + totalDeleted + " 条");
        } while (deletedCount > 0);
        
        // 清理7天前的游戏公告
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        Date sevenDaysAgo = calendar.getTime();
        QueryWrapper<GameNotice> noticeWrapper = new QueryWrapper<>();
        noticeWrapper.lt("create_time", sevenDaysAgo);
        int noticeDeleted = gameNoticeMapper.delete(noticeWrapper);
        XxlJobHelper.log("删除7天前的游戏公告: " + noticeDeleted + " 条");
        
        // 清理7天前的每日视图完成记录
        QueryWrapper<DailyViewFinsh> dailyViewWrapper = new QueryWrapper<>();
        dailyViewWrapper.lt("get_time", sevenDaysAgo);
        int dailyViewDeleted = dailyViewFinshMapper.delete(dailyViewWrapper);
        XxlJobHelper.log("删除7天前的每日视图完成记录: " + dailyViewDeleted + " 条");
        
        // 清理7天前的礼仪礼品记录
        QueryWrapper<CeremonialGiftRecord> giftRecordWrapper = new QueryWrapper<>();
        giftRecordWrapper.lt("get_time", sevenDaysAgo);
        int giftRecordDeleted = giftRecordMapper.delete(giftRecordWrapper);
        XxlJobHelper.log("删除7天前的礼仪礼品记录: " + giftRecordDeleted + " 条");
        
        userMapper.updateBronze1();
        userMapper.updateBronze2();
        userMapper.updateBronze3();
        userMapper.updateChongzhi();
        userMapper.updatechongzhiTower();
        userMapper.updatechongzhiQiangduo();
        userMapper.updatebaoCount();
        XxlJobHelper.log("清理任务完成，共删除游戏战斗记录: " + totalDeleted + " 条");
    }
    @XxlJob("DeleteAllCraftingRecords")
    public void deleteAll(){
        gameServiceService.deleteAll();
    }

    @XxlJob("Reward Distribution")
    public void executeWeeklyTask() {
        // 任务逻辑
        System.out.println("奖励发放");
        gameServiceService.sendRawrd();

    }

    @XxlJob("SyncLastWeekRankings")
    public void executeMothlyTask() {
        gameServiceService.executeMothlyTask();
    }


    @XxlJob("AutoReleaseAgentRewards")
    public void executeTaskAtMondayMidnight() {
        // 任务逻辑
        System.out.println("游戏托自动释放奖励");
        gameServiceService.sendTuoRawrd();

    }

}
