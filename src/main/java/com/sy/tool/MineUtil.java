package com.sy.tool;

import com.sy.model.game.MineLevelConfig;
import com.sy.model.game.MineRobLog;
import com.sy.model.game.UserMine;

import java.util.Date;

public class MineUtil {
    // 抢夺全局配置
    public static final long ROB_OFFLINE_MS = 30L * 60 * 1000;   // 离线30分钟可抢夺
    public static final long ROB_PROTECT_MS = 60L * 60 * 1000;  // 被抢后1小时保护
    public static final double ROB_RATE = 0.3;                  // 抢夺比例30%
    public static final int ROB_MIN_SILVER = 100;               // 最低抢夺银两

    /**
     * 定时任务单条矿场产出结算
     */
    public static void batchCalcMineSilver(UserMine mine) {
        if (mine.getMineStatus() == null || mine.getMineStatus() != 1) {
            return;
        }
        Date lastCollect = mine.getLastCollectTime();
        Date now = new Date();
        long nowMs = now.getTime();

        // 新矿场初始化结算时间
        if (lastCollect == null) {
            mine.setLastCollectTime(now);
            mine.setUpdateTime(now);
            return;
        }

        long diffMs = nowMs - lastCollect.getTime();
        if (diffMs <= 0) {
            return;
        }

        long hourDiff = diffMs / (1000 * 60 * 60);
        int hourOutput = mine.getHourOutput() == null ? 0 : mine.getHourOutput();
        int addSilver = (int) hourDiff * hourOutput;
        if (addSilver <= 0) {
            return;
        }

        int curSilver = mine.getCurrentSilver() == null ? 0 : mine.getCurrentSilver();
        int maxCap = mine.getMaxCapacity() == null ? 0 : mine.getMaxCapacity();
        int newSilver = Math.min(curSilver + addSilver, maxCap);

        mine.setCurrentSilver(newSilver);
        mine.setLastCollectTime(now);
        mine.setUpdateTime(now);
    }

    /**
     * 升级矿场，赋值等级属性
     */
    public static UserMine upgradeMine(UserMine mine, MineLevelConfig nextConfig) {
        if (nextConfig == null) {
            throw new IllegalArgumentException("已达到最高等级");
        }
        mine.setMineLevel(nextConfig.getMineLevel());
        mine.setHourOutput(nextConfig.getHourOutput());
        mine.setMaxCapacity(nextConfig.getMaxCapacity());
        mine.setUpdateTime(new Date());
        return mine;
    }

    /**
     * 收取全部银两
     */
    public static UserMine collectSilver(UserMine mine) {
        mine.setCurrentSilver(0);
        mine.setLastCollectTime(new Date());
        mine.setUpdateTime(new Date());
        return mine;
    }

    /**
     * 抢夺校验结果
     */
    @lombok.Data
    public static class MineRobResult {
        private boolean canRob;
        private String robMsg;
        private int robSilver;
        private int leftSilver;
    }

    /**
     * 校验是否可抢夺 + 计算抢夺数量
     */
    public static MineRobResult checkAndCalcRob(UserMine mine) {
        MineRobResult res = new MineRobResult();
        Date now = new Date();
        long nowMs = now.getTime();
        int curSilver = mine.getCurrentSilver() == null ? 0 : mine.getCurrentSilver();

        // 矿场未开启
        if (mine.getMineStatus() == null || mine.getMineStatus() != 1) {
            res.setCanRob(false);
            res.setRobMsg("对方矿场未开启");
            return res;
        }
        // 银两不足门槛
        if (curSilver < ROB_MIN_SILVER) {
            res.setCanRob(false);
            res.setRobMsg("对方银两过少，无法抢夺");
            return res;
        }
        // 离线不足
        Date lastLogin = mine.getLastLoginTime();
        if (lastLogin == null) {
            res.setCanRob(false);
            res.setRobMsg("对方新矿场保护中");
            return res;
        }
        long offlineDiff = nowMs - lastLogin.getTime();
        if (offlineDiff < ROB_OFFLINE_MS) {
            res.setCanRob(false);
            res.setRobMsg("对方在线/离线时间较短，不可抢夺");
            return res;
        }
        // 被抢冷却
        Date lastRob = mine.getLastRobTime();
        if (lastRob != null) {
            long robDiff = nowMs - lastRob.getTime();
            if (robDiff < ROB_PROTECT_MS) {
                res.setCanRob(false);
                res.setRobMsg("对方刚被抢夺，处于保护期");
                return res;
            }
        }
        // 可抢夺
        int robNum = (int) (curSilver * ROB_RATE);
        int left = curSilver - robNum;
        res.setCanRob(true);
        res.setRobSilver(robNum);
        res.setLeftSilver(left);
        return res;
    }

    /**
     * 执行抢夺更新矿场数据
     */
    public static UserMine doRobMine(UserMine mine, MineRobResult robResult) {
        if (!robResult.isCanRob()) {
            throw new RuntimeException("不满足抢夺条件");
        }
        mine.setCurrentSilver(robResult.getLeftSilver());
        mine.setLastRobTime(new Date());
        mine.setUpdateTime(new Date());
        return mine;
    }

    /**
     * 构建抢夺日志
     */
    public static MineRobLog buildRobLog(Integer attackerUserId, Integer targetUserId, MineRobResult robResult) {
        MineRobLog log = new MineRobLog();
        log.setAttackerUserId(attackerUserId);
        log.setTargetUserId(targetUserId);
        log.setRobSilver(robResult.getRobSilver());
        log.setLeftSilver(robResult.getLeftSilver());
        log.setCreateTime(new Date());
        return log;
    }

    /**
     * 登录刷新在线时间，重置离线保护
     */
    public static void refreshLoginTime(UserMine mine) {
        mine.setLastLoginTime(new Date());
        mine.setUpdateTime(new Date());
    }
}