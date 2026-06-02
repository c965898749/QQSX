package com.sy.controller.game;

import com.sy.mapper.UserMapper;
import com.sy.mapper.game.CeremonialGiftRecordMapper;
import com.sy.mapper.game.DailyViewFinshMapper;
import com.sy.mapper.game.GameFightMapper;
import com.sy.mapper.game.GameNoticeMapper;
import com.sy.service.GameServiceService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


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
        gameFightMapper.deleteByTime();
        gameNoticeMapper.deleteByMap(new HashMap<>());
        dailyViewFinshMapper.deleteByMap(new HashMap<>());
        giftRecordMapper.deleteByMap(new HashMap<>());
        userMapper.updateBronze1();
        userMapper.updateBronze2();
        userMapper.updateBronze3();
        userMapper.updateChongzhi();
        userMapper.updatechongzhiTower();
        userMapper.updatechongzhiQiangduo();
        userMapper.updatebaoCount();

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
