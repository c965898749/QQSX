package com.sy.tool;

import java.util.Date;

public class StaminaUtil {
    // 体力、活力统一上限
    public static final int MAX_STAMINA = 720;
    // 每5分钟恢复1点（毫秒）
    public static final long RECOVER_MS = 5 * 60 * 1000L;

    /**
     * 计算体力活力自然恢复（登录/打开面板时调用）
     * @param tiliCount 当前体力
     * @param tiliCountTime 体力上次恢复时间Date
     * @param huoliCount 当前活力
     * @param huoliCountTime 活力上次恢复时间Date
     * @return 恢复后数据
     */
    public static StaminaResult calcStamina(int tiliCount, Date tiliCountTime,
                                            int huoliCount, Date huoliCountTime) {
        Date now = new Date();
        long nowMs = now.getTime();
        int newTili = calcSingle(tiliCount, tiliCountTime.getTime(), nowMs);
        int newHuoli = calcSingle(huoliCount, huoliCountTime.getTime(), nowMs);
        return new StaminaResult(newTili, now, newHuoli, now);
    }

    /**
     * 使用体力药水：增加/减少体力，上限720，不改动恢复计时时间
     * @param curTili 当前体力
     * @param tiliTime 体力恢复基准时间（原样带回，不修改）
     * @param changeNum 变化值，正数加药水，负数扣体力
     * @return 操作后体力+原时间
     */
    public static StaminaItem useTiliPotion(int curTili, Date tiliTime, int changeNum) {
        int after = curTili + changeNum;
        // 最低不能小于0，最高不能超上限
        after = Math.max(0, Math.min(after, MAX_STAMINA));
        return new StaminaItem(after, tiliTime);
    }

    /**
     * 使用活力药水：增加/减少活力，上限720，不改动恢复计时时间
     * @param curHuoli 当前活力
     * @param huoliTime 活力恢复基准时间（原样带回，不修改）
     * @param changeNum 变化值，正数加药水，负数扣活力
     * @return 操作后活力+原时间
     */
    public static StaminaItem useHuoliPotion(int curHuoli, Date huoliTime, int changeNum) {
        int after = curHuoli + changeNum;
        after = Math.max(0, Math.min(after, MAX_STAMINA));
        return new StaminaItem(after, huoliTime);
    }

    /**
     * 单个资源自然恢复计算内部方法
     */
    private static int calcSingle(int curVal, long lastTimeMs, long nowMs) {
        if (curVal >= MAX_STAMINA) {
            return MAX_STAMINA;
        }
        long diff = nowMs - lastTimeMs;
        if (diff <= 0) {
            return curVal;
        }
        int add = (int) (diff / RECOVER_MS);
        int total = curVal + add;
        return Math.min(total, MAX_STAMINA);
    }

    // 单种资源返回实体（药水操作专用：数值+对应Date时间）
    public static class StaminaItem {
        private int count;
        private Date countTime;

        public StaminaItem(int count, Date countTime) {
            this.count = count;
            this.countTime = countTime;
        }

        public int getCount() {
            return count;
        }

        public Date getCountTime() {
            return countTime;
        }
    }

    // 完整体力活力结果（自然恢复接口返回）
    public static class StaminaResult {
        private int tiliCount;
        private Date tiliCountTime;
        private int huoliCount;
        private Date huoliCountTime;

        public StaminaResult(int tiliCount, Date tiliCountTime, int huoliCount, Date huoliCountTime) {
            this.tiliCount = tiliCount;
            this.tiliCountTime = tiliCountTime;
            this.huoliCount = huoliCount;
            this.huoliCountTime = huoliCountTime;
        }

        public int getTiliCount() {
            return tiliCount;
        }

        public Date getTiliCountTime() {
            return tiliCountTime;
        }

        public int getHuoliCount() {
            return huoliCount;
        }

        public Date getHuoliCountTime() {
            return huoliCountTime;
        }
    }
}