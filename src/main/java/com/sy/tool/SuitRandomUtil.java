package com.sy.tool;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 装备套装随机工具类
 * 规则：套装序号数字越大权重越高，随机更容易出高阶套装
 * 概率计算：宝石最低等级 * 系数，常规最高25%，闪避最高12%
 */
public class SuitRandomUtil {

    // ===================== 套装ID编码映射 =====================
    public enum SuitType {
        // 输出数字编码 | 套装名称
        ZHU_XIAN(0, "诛仙"),
        SHI_REN(1, "食人"),
        QU_MO(2, "驱魔"),
        DOU_SHOU(3, "斗兽"),

        XIAN_SHI(4, "仙师"),
        REN_JIE(5, "人杰"),
        MO_WANG(6, "魔王"),
        SHOU_LING(7, "兽灵"),

        FAN_HUO(8, "返火"),
        PI_TU(9, "辟土"),
        NI_LEI(10, "逆雷"),
        FEN_SHUI(11, "分水"),

        YU_BING(12, "驭兵"),
        PO_WANG(13, "破妄"),

        TA_LANG(14, "踏浪"),
        PO_YAN(15, "破岩"),
        QU_LEI(16, "驱雷"),
        DAO_HUO(17, "蹈火"),

        BU_QIN(18, "不侵"),
        MIE_FA(19, "灭法"),

        TU_XI(20, "突袭"),
        SHAN_BI(21, "闪避");

        private final int code;
        private final String name;

        SuitType(int code, String name) {
            this.code = code;
            this.name = name;
        }

        public int getCode() {
            return code;
        }

        public String getName() {
            return name;
        }

        // 根据code反查枚举
        public static SuitType getByCode(int code) {
            for (SuitType type : values()) {
                if (type.getCode() == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("不存在该套装编码：" + code);
        }
    }

    // ===================== 权重配置：序号越大权重越高 =====================
    // 基础权重基数，每高1号+1权重，数值越大抽中概率越高
    private static final int BASE_WEIGHT = 1;
    private static final int WEIGHT_STEP = 1;

    /**
     * 根据套装编码获取对应权重
     * @param suitCode 套装数字编码
     * @return 权重值
     */
    public static int getSuitWeight(int suitCode) {
        // 编码越大权重越高
        return BASE_WEIGHT + suitCode * WEIGHT_STEP;
    }

    /**
     * 随机单条套装编码（权重递增）
     * @return 随机套装数字code
     */
    public static int randomOneSuitCode() {
        List<Integer> allCodeList = Arrays.stream(SuitType.values())
                .map(SuitType::getCode)
                .collect(Collectors.toList());

        // 构建权重池
        List<Integer> weightPool = new ArrayList<>();
        for (Integer code : allCodeList) {
            int weight = getSuitWeight(code);
            for (int i = 0; i < weight; i++) {
                weightPool.add(code);
            }
        }
        // 随机抽取
        Random random = new Random();
        return weightPool.get(random.nextInt(weightPool.size()));
    }

    /**
     * 批量随机N个套装编码（可重复）
     * @param count 随机数量
     * @return 套装编码列表
     */
    public static List<Integer> randomBatchSuitCode(int count) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(randomOneSuitCode());
        }
        return result;
    }

    // ===================== 套装概率计算逻辑 =====================
    /**
     * 计算套装触发概率
     * @param suitCode 套装编码
     * @param gemMinLv 宝石最低等级 0~50
     * @return 最终概率百分比（已封顶）
     */
    public static double calcTriggerRate(int suitCode, int gemMinLv) {
        if (gemMinLv <= 0) {
            return 0;
        }
        // 最高宝石等级50级封顶
        int realLv = Math.min(gemMinLv, 50);
        double rate;
        // 闪避单独系数 0.24%，上限12%
        if (SuitType.SHAN_BI.getCode() == suitCode) {
            rate = realLv * 0.24;
            return Math.min(rate, 12);
        }
        // 其余套装系数0.5%，上限25%
        rate = realLv * 0.5;
        return Math.min(rate, 25);
    }

    // ===================== 测试入口 =====================
    public static void main(String[] args) {
        // 1. 随机10条套装
        List<Integer> randomCodes = randomBatchSuitCode(10);
        System.out.println("随机套装编码列表：" + randomCodes);

        // 2. 概率测试：宝石最低50级
        int testGemLv = 50;
        for (SuitType type : SuitType.values()) {
            double rate = calcTriggerRate(type.getCode(), testGemLv);
            System.out.printf("编码%d 【%s】 满级50级概率：%.2f%%%n",
                    type.getCode(), type.getName(), rate);
        }
    }
}

