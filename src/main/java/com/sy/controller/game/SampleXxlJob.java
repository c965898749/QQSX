package com.sy.controller.game;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sy.mapper.UserMapper;
import com.sy.mapper.game.*;
import com.sy.model.game.CeremonialGiftRecord;
import com.sy.model.game.DailyViewFinsh;
import com.sy.model.game.GameNotice;
import com.sy.model.game.UserMine;
import com.sy.service.GameServiceService;
import com.sy.tool.MineUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import javax.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
    @Resource
    private UserMineMapper userMineMapper;



    /**
     * 每天定时清除游戏过多消息
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public  void pushsite() {
        userMapper.updateBronze1();
        userMapper.updateBronze2();
        userMapper.updateBronze3();
        userMapper.updateChongzhi();
        userMapper.updatechongzhiTower();
        userMapper.updatechongzhiQiangduo();
        userMapper.updatebaoCount();
        QueryWrapper<GameNotice> noticeWrapper = new QueryWrapper<>();
        gameNoticeMapper.delete(noticeWrapper);

        // 清理7天前的每日视图完成记录
        QueryWrapper<DailyViewFinsh> dailyViewWrapper = new QueryWrapper<>();
        dailyViewFinshMapper.delete(dailyViewWrapper);

        // 清理7天前的礼仪礼品记录
        QueryWrapper<CeremonialGiftRecord> giftRecordWrapper = new QueryWrapper<>();
        giftRecordMapper.delete(giftRecordWrapper);
        // 分批删除游戏战斗记录，避免超时
        int totalDeleted = 0;
        int deletedCount;
        do {
            deletedCount = gameFightMapper.deleteByTimeBatch();
            totalDeleted += deletedCount;
        } while (deletedCount > 0);
        System.out.println("删除完成，共删除: " + totalDeleted + " 条");
    }
    @Scheduled(cron = "0 0 0/4 * * ?")
    public void deleteAll(){
        gameServiceService.deleteAll();
    }

    @Scheduled(cron = "0 0 22 ? * 7")
    public void executeWeeklyTask() {
        // 任务逻辑
        System.out.println("奖励发放");
        gameServiceService.sendRawrd();

    }

    @Scheduled(cron = "0 0 1 1 * ?")
    public void executeMothlyTask() {
        gameServiceService.executeMothlyTask();
    }


    // 每5分钟批量结算所有生产矿场
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void mineSilverCalcTask() {
        LambdaQueryWrapper<UserMine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMine::getMineStatus, 1);
        List<UserMine> mineList = userMineMapper.selectList(wrapper);

        for (UserMine mine : mineList) {
            MineUtil.batchCalcMineSilver(mine);
            userMineMapper.updateById(mine);
        }
    }

}
