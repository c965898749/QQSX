package com.sy.tool;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 天匠锻造材料消耗工具
 * 品级入参：数字 0~9
 * 0=凡器，1=法器，2=灵器，3=法宝，4=古宝，5=造物，6=灵宝，7=通天，8=玄天，9=仙器
 * 材料ItemId：青铜矿13，玄铁矿14，紫金矿15，月华晶石16
 * 消耗数量统一使用 BigDecimal
 */
public class ForgeMaterialUtil {

    /**
     * 获取锻造材料消耗
     * @param rankNum 品级编号 0~9
     * @return key=物品ID, value=消耗数量(BigDecimal)
     */
    public static Map<Integer, BigDecimal> getForgeCost(int rankNum) {
        Map<Integer, BigDecimal> costMap = new HashMap<>(4);
        BigDecimal zero = BigDecimal.ZERO;

        switch (rankNum) {
            case 0:
                // 凡器
                costMap.put(13, zero);
                costMap.put(14, zero);
                costMap.put(15, zero);
                costMap.put(16, zero);
                break;
            case 1:
                // 法器
                costMap.put(13, new BigDecimal("1000000"));
                break;
            case 2:
                // 灵器
                costMap.put(13, new BigDecimal("1200000"));
                break;
            case 3:
                // 法宝
                costMap.put(13, new BigDecimal("1500000"));
                costMap.put(14, new BigDecimal("1000000"));
                break;
            case 4:
                // 古宝
                costMap.put(13, new BigDecimal("1800000"));
                costMap.put(14, new BigDecimal("1200000"));
                break;
            case 5:
                // 造物
                costMap.put(13, new BigDecimal("2200000"));
                costMap.put(14, new BigDecimal("1500000"));
                costMap.put(15, new BigDecimal("1000000"));
                break;
            case 6:
                // 灵宝
                costMap.put(13, new BigDecimal("2600000"));
                costMap.put(14, new BigDecimal("1800000"));
                costMap.put(15, new BigDecimal("1300000"));
                break;
            case 7:
                // 通天
                costMap.put(13, new BigDecimal("3100000"));
                costMap.put(14, new BigDecimal("2200000"));
                costMap.put(15, new BigDecimal("1600000"));
                costMap.put(16, new BigDecimal("1000000"));
                break;
            case 8:
                // 玄天
                costMap.put(13, new BigDecimal("3600000"));
                costMap.put(14, new BigDecimal("2600000"));
                costMap.put(15, new BigDecimal("1900000"));
                costMap.put(16, new BigDecimal("1300000"));
                break;
            case 9:
                // 仙器
                costMap.put(13, new BigDecimal("4200000"));
                costMap.put(14, new BigDecimal("3000000"));
                costMap.put(15, new BigDecimal("2300000"));
                costMap.put(16, new BigDecimal("1600000"));
                break;
            default:
                throw new IllegalArgumentException("品级编号只能是0-9");
        }
        return costMap;
    }

    // 测试调用
    public static void main(String[] args) {
        // 示例：获取仙器(9)消耗
        Map<Integer, BigDecimal> cost = getForgeCost(9);
        cost.forEach((itemId, num) -> System.out.println("物品ID:" + itemId + " 数量:" + num));
    }
}

