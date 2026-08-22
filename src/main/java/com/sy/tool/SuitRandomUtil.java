package com.sy.tool;

import com.sy.model.game.Xilian;

import java.util.*;

/**
 * 装备洗练属性随机工具类
 * genEquipRefine(int count) 仅一个入参：生成多少条词条
 * 权重规则：code数字越小权重越低，权重=code+1
 * 数值规则：
 * 0突击、1灵能、2防护、3御灵：整数
 * 4暴击、5暴抗、6闪避、7命中：保留1位小数
 * 8速度：100~1000 整数
 * 9生命：500~2000 整数
 * 返回 List<Map<String,Object>> key:attr,quality,value
 */
public class SuitRandomUtil {

    public enum RefineAttrType {
        TU_JI(0, "突击"),
        LING_NENG(1, "灵能"),
        FANG_HU(2, "防护"),
        YU_LING(3, "御灵"),
        BAO_JI(4, "暴击"),
        BAO_KANG(5, "暴抗"),
        SHAN_BI(6, "闪避"),
        MING_ZHONG(7, "命中"),
        SU_DU(8, "速度"),
        SHENG_MING(9, "生命");

        private final int code;
        private final String name;

        RefineAttrType(int code, String name) {
            this.code = code;
            this.name = name;
        }

        public int getCode() {
            return code;
        }
    }

    //品质枚举 0普通 1优秀 2极品
    public enum RefineQuality {
        NORMAL(0, "普通"),
        GOOD(1, "优秀"),
        PERFECT(2, "极品");

        private final int code;

        RefineQuality(int code, String name) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }
    }

    /**
     * 基础区间 [普通min,普通max,优秀min,优秀max,极品min,极品max]
     * 0,1,2,3：整数
     * 4,5,6,7：小数(1位)
     * 8速度：100‑1000
     * 9生命：500‑2000
     */
    private static final double[][] ATTR_VALUE_RANGE = {
            {3,69, 70,119, 120,150},     //0 突击
            {3,69, 70,119, 120,150},     //1 灵能
            {3,69, 70,119, 120,150},     //2 防护
            {3,69, 70,119, 120,150},     //3 御灵
            {2.0,4.0, 4.1,7.0, 7.1,10.0}, //4 暴击 小数
            {2.0,4.0, 4.1,7.0, 7.1,10.0}, //5 暴抗 小数
            {2.0,4.0, 4.1,7.0, 7.1,10.0}, //6 闪避 小数
            {2.0,4.0, 4.1,7.0, 7.1,10.0}, //7 命中 小数
            {100,350, 360,650, 660,1000}, //8 速度
            {500,900, 910,2450, 2460,4000}//9 生命
    };

    //权重 = code +1，code越小权重越低
    private static int getAttrWeight(int code){
        return code + 1;
    }

    //品质权重 普通60 优秀30 极品10
    private static final int[] QUALITY_WEIGHT = {60,30,10};
    private static final Random RANDOM = new Random();

    public static RefineQuality randomQuality(){
        int total = QUALITY_WEIGHT[0]+QUALITY_WEIGHT[1]+QUALITY_WEIGHT[2];
        int r = RANDOM.nextInt(total);
        if(r < QUALITY_WEIGHT[0]){
            return RefineQuality.NORMAL;
        }else if(r < QUALITY_WEIGHT[0]+QUALITY_WEIGHT[1]){
            return RefineQuality.GOOD;
        }else{
            return RefineQuality.PERFECT;
        }
    }

    /**
     * 根据attrCode+quality生成value
     * 4/5/6/7 返回1位小数；其余返回整数
     */
    public static Object randomAttrValue(int attrCode, RefineQuality quality){
        double[] range = ATTR_VALUE_RANGE[attrCode];
        int q = quality.getCode();
        double min = range[q*2];
        double max = range[q*2+1];

        if(attrCode >=4 && attrCode <=7){
            //暴击、暴抗、闪避、命中：保留1位小数
            double val = min + (max - min) * RANDOM.nextDouble();
            return Math.round(val * 10.0) / 10.0;
        }else{
            //其余取整数
            int minI = (int)min;
            int maxI = (int)max;
            return minI + RANDOM.nextInt(maxI - minI + 1);
        }
    }

    /**
     * 权重随机属性，排除已使用code，保证不重复
     */
    private static int randomWeightAttr(Set<Integer> excludeSet){
        int totalWeight = 0;
        List<Integer> codeList = new ArrayList<>();
        List<Integer> weightList = new ArrayList<>();

        for(int code=0;code<10;code++){
            if(excludeSet.contains(code)) continue;
            int w = getAttrWeight(code);
            codeList.add(code);
            weightList.add(w);
            totalWeight += w;
        }
        if(totalWeight <=0) throw new RuntimeException("没有可用属性");

        int r = RANDOM.nextInt(totalWeight);
        int cur = 0;
        for(int i=0;i<codeList.size();i++){
            cur += weightList.get(i);
            if(r < cur){
                return codeList.get(i);
            }
        }
        return codeList.get(codeList.size()-1);
    }

    /**
     * 生成N条洗练词条【仅一个入参count】
     * @param count 1~10
     * @return List<Map<String,Object>> attr(int),quality(int),value(Integer/Double)
     */
    public static List<Xilian> genEquipRefine(int count){
        return genEquipRefine(count, new HashSet<>());
    }

    /**
     * 生成N条洗练词条，排除指定属性code（锁定属性不重复）
     * @param count 1~10
     * @param excludeAttrs 需要排除的属性code集合（如锁定的属性）
     * @return List<Xilian>
     */
    public static List<Xilian> genEquipRefine(int count, Set<Integer> excludeAttrs){
        if(count <=0 || count >10){
            throw new IllegalArgumentException("count范围1~10");
        }
        List<Xilian> res = new ArrayList<>();
        Set<Integer> used = new HashSet<>(excludeAttrs);
        for(int i=0;i<count;i++){
            int code = randomWeightAttr(used);
            used.add(code);
            RefineQuality q = randomQuality();
            Object val = randomAttrValue(code, q);

            Xilian item = new Xilian();
            item.setXilian(code);
            item.setQuality(q.getCode());
            item.setValue(val.toString());
            res.add(item);
        }
        return res;
    }


}
