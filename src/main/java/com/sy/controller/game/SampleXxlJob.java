package com.sy.controller.game;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sy.mapper.UserMapper;
import com.sy.mapper.game.*;
import com.sy.model.game.*;
import com.sy.service.GameServiceService;
import com.sy.tool.MineUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
    @Autowired
    private FriendBlessingMapper friendBlessingMapper;
    @Autowired
    private CharactersMapper charactersMapper;
    @Autowired
    private EqCharactersMapper eqCharactersMapper;
    @Autowired
    private GamePlayerBagMapper gamePlayerBagMapper;
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

        // 清理7天前的好友祝福记录
        QueryWrapper<FriendBlessing> blessingWrapper = new QueryWrapper<>();
        friendBlessingMapper.delete(blessingWrapper);

        // 清理is_delete=1的角色记录
        charactersMapper.deleteByIsDelete();

        // 清理is_delete=1的装备角色记录
        QueryWrapper<EqCharacters> eqCharactersWrapper = new QueryWrapper<>();
        eqCharactersWrapper.eq("is_delete", "1");
        eqCharactersMapper.delete(eqCharactersWrapper);

        // 清理is_delete=1的玩家背包记录
        QueryWrapper<GamePlayerBag> playerBagWrapper = new QueryWrapper<>();
        playerBagWrapper.eq("is_delete", "1");
        gamePlayerBagMapper.delete(playerBagWrapper);
    }
    
    /**
     * 每天6点执行：修复异常等级数据
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void fixAbnormalLevel() {
        userMapper.fixAbnormalLevel();
        userMapper.characterMerge();
    }
    
    /**
     * 每天6点执行：修复异常Characters数据
     */
    @Scheduled(cron = "0 0 5 * * ?")
    public void fixAbnormalCharacters() {
        // 第一步：查询所有未删除的角色记录（is_delete='0'）
        List<Characters> allCharacters = charactersMapper.selectActiveCardList();
        
        if (allCharacters == null || allCharacters.isEmpty()) {
            return;
        }
        
        int lvFixedCount = 0;
        int duplicateFixedCount = 0;
        
        // 第二步：将lv=0的记录更新为1
        for (Characters character : allCharacters) {
            if ("0".equals(character.getIsDelete()) && character.getLv() != null && character.getLv() == 0) {
                character.setLv(1);
                character.setUpdateTime(new Date());
                charactersMapper.updateByPrimaryKeySelective(character);
                lvFixedCount++;
            }
        }
        
        // 第三步：处理重复数据 - 按userId和id分组（数据已经是is_delete='0'的，无需再过滤）
        Map<String, List<Characters>> groupMap = allCharacters.stream()
            .collect(Collectors.groupingBy(c -> c.getUserId() + "_" + c.getId()));
        
        // 第四步：对每组重复数据，保留最优的一条，其他的标记为删除
        for (Map.Entry<String, List<Characters>> entry : groupMap.entrySet()) {
            List<Characters> group = entry.getValue();
            
            if (group.size() <= 1) {
                continue; // 没有重复，跳过
            }
            
            // 找出最优的记录（stackCount、lv、flyup综合比较）
            Characters best = findBestCharacter(group);
            
            // 将其余较差的记录标记为删除
            for (Characters character : group) {
                if (!character.getUuid().equals(best.getUuid())) {
                    character.setIsDelete("1");
                    character.setUpdateTime(new Date());
                    charactersMapper.updateByPrimaryKeySelective(character);
                    duplicateFixedCount++;
                }
            }
        }
        
        System.out.println("Characters数据修复完成：修复lv=0的记录数=" + lvFixedCount + ", 标记删除的重复记录数=" + duplicateFixedCount);
    }
    
    /**
     * 从一组角色中找出最优的一条
     * 比较规则：stackCount、lv、flyup，综合评分最高的为最优
     */
    private Characters findBestCharacter(List<Characters> characters) {
        Characters best = characters.get(0);
        
        for (int i = 1; i < characters.size(); i++) {
            Characters current = characters.get(i);
            
            if (isBetter(current, best)) {
                best = current;
            }
        }
        
        return best;
    }
    
    /**
     * 判断current是否比other更优
     * 如果current的所有字段都>=other，且至少有一个字段>other，则current更优
     */
    private boolean isBetter(Characters current, Characters other) {
        int currentStackCount = current.getStackCount() != null ? current.getStackCount() : 0;
        int otherStackCount = other.getStackCount() != null ? other.getStackCount() : 0;
        
        int currentLv = current.getLv() != null ? current.getLv() : 0;
        int otherLv = other.getLv() != null ? other.getLv() : 0;
        
        int currentFlyup = current.getFlyup() != null ? current.getFlyup() : 0;
        int otherFlyup = other.getFlyup() != null ? other.getFlyup() : 0;
        
        // current的所有字段都 >= other
        boolean allGreaterOrEqual = 
            currentStackCount >= otherStackCount &&
            currentLv >= otherLv &&
            currentFlyup >= otherFlyup;
        
        // 至少有一个字段严格大于
        boolean atLeastOneGreater = 
            currentStackCount > otherStackCount ||
            currentLv > otherLv ||
            currentFlyup > otherFlyup;
        
        return allGreaterOrEqual && atLeastOneGreater;
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
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void mineSilverCalcTask() {
        LambdaQueryWrapper<UserMine> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMine::getMineStatus, 0);
        List<UserMine> mineList = userMineMapper.selectList(wrapper);

        for (UserMine mine : mineList) {
            MineUtil.batchCalcMineSilver(mine);
            userMineMapper.updateById(mine);
        }
    }

}
