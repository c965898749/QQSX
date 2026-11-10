package com.sy.tool;

import com.sy.model.game.EqCard;

import java.math.BigDecimal;
import java.util.*;

/**
 * 装备生成工具类 - 山海经简化版（v2）
 * 核心规则：
 * 1. 固定4大类 × 3子类 × 30编号：
 *    兵刃(0)：剑J / 弓G / 法杖F
 *    防具(1)：衣服C / 头盔T / 鞋子X
 *    宝具(2)：戒指Z / 项链N / 披风P
 *    法器(3)：印章Y / 葫芦H / 魂幡B
 * 2. ID格式：{前缀}{001~027}_{属性字母}，编号即图片键（前端 emp/{id.split('_')[0]} 加载）；
 *    028~030 为预留位（活动/专属装备，打造不会随机出）。
 * 3. 编号由星级决定：每个星级占3个编号位（1星001-003、1.5星004-006 ... 5星025-027），段内随机。
 * 4. 星级-属性分级不变：1~3星1条、3.5~4星2条、4.5~5星3条，数值按星级基准±40%独立随机。
 * 5. 装备被动技能全部为空（passive_introduce_one/two/three = null），后期由技能书系统提供。
 */
public class EquipmentGenerateUtil {
    // ========== 一、全局常量配置区 ==========
    // 9大属性-固定字母映射（永久不变）
    public static final Map<String, String> ATTR_TO_CODE = new LinkedHashMap<String, String>() {{
        put("锋利", "F");
        put("坚韧", "J");
        put("火焰", "H");
        put("火抗", "K");
        put("毒素", "D");
        put("毒抗", "N");
        put("飞弹", "L");
        put("弹抗", "A");
        put("治愈", "Z");
    }};
    public static final List<String> BASE_ATTRS = new ArrayList<>(ATTR_TO_CODE.keySet());

    // 9个合法星级（顺序即编号段位顺序：第i个星级占用编号 3i+1 ~ 3i+3）
    public static final double[] STAR_TIERS = {1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0};
    public static final Set<Double> ALLOW_STARS = new HashSet(Arrays.asList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0));
    // 职业
    public static final String[] PROFESSION_LIST = {"武圣", "神将", "仙灵"};

    // 每个子类的编号数量（001~027 可打造随机出，028~030 预留）
    public static final int NUM_PER_STAR = 3;
    public static final int GENERABLE_NUMS = 27;

    // 装备子类全配置：{子类名, ID前缀, 大类eqType, 30个山海经风格名称(按星级从低到高排列)}
    // eqType: 0=兵刃 1=防具 2=宝具 3=法器（与前端 nameNode ["兵刃","防具","宝具","法器"] 槽位序一致）
    public static final Object[][] EQUIP_CONFIG = {
            {"剑", "J", 0, new String[]{
                    "楚金剑", "桃殳剑", "白茅剑",         // 1星
                    "棘狐剑", "赤螭剑", "玄铁剑",         // 1.5星
                    "夔牛剑", "毕方剑", "白泽剑",         // 2星
                    "英招剑", "陆吾剑", "长右剑",         // 2.5星
                    "神农剑", "帝江剑", "穷奇剑",         // 3星
                    "饕餮剑", "梼杌剑", "相柳剑",         // 3.5星
                    "九婴剑", "封狶剑", "朱厌剑",         // 4星
                    "天蛇剑", "猰貐剑", "凿齿剑",         // 4.5星
                    "烛龙剑", "应龙剑", "轩辕剑",         // 5星
                    "盘古剑", "女娲剑", "山海剑",         // 预留 028-030
            }},
            {"弓", "G", 0, new String[]{
                    "桃弧", "柳弰弓", "葛藤弓",           // 1星
                    "獠牙弓", "风羽弓", "石砦弓",         // 1.5星
                    "玄狐弓", "青雕弓", "辟邪弓",         // 2星
                    "风伯弓", "雨师弓", "计蒙弓",         // 2.5星
                    "金乌弓", "银兎弓", "禺号弓",         // 3星
                    "禺强弓", "蓐收弓", "芒神弓",         // 3.5星
                    "常羲弓", "西母弓", "夸父弓",         // 4星
                    "贯月弓", "裂云弓", "归墟弓",         // 4.5星
                    "后羿弓", "射日弓", "焚天弓",         // 5星
                    "落星弓", "扶桑弓", "若木弓",         // 预留
            }},
            {"法杖", "F", 0, new String[]{
                    "枯灵杖", "荆木杖", "青竹杖",         // 1星
                    "玄檀杖", "白檀杖", "灵柞杖",         // 1.5星
                    "招灵杖", "役鬼杖", "安魂杖",         // 2星
                    "巫咸杖", "巫彭杖", "巫抵杖",         // 2.5星
                    "青鸾杖", "丹穴杖", "英招杖",         // 3星
                    "羽民杖", "三苗杖", "玄冥杖",         // 3.5星
                    "句芒杖", "祝融杖", "不廷杖",         // 4星
                    "流沙杖", "弱水杖", "炎洲杖",         // 4.5星
                    "东皇杖", "羲和杖", "常阳杖",         // 5星
                    "建木杖", "扶桑杖", "混元杖",         // 预留
            }},
            {"衣服", "C", 1, new String[]{
                    "粗苧袍", "葛布衣", "鹿皮裘",         // 1星
                    "玄狐裘", "豹纹袍", "鹤氅衣",         // 1.5星
                    "火浣衣", "冰纨衫", "云锦袍",         // 2星
                    "茝兰袍", "杜若裙", "芙蓉裳",         // 2.5星
                    "青鸾袍", "彩凤衣", "云霓裳",         // 3星
                    "蛟龙袍", "云纹甲", "金缕衣",         // 3.5星
                    "火鳞甲", "冰蚕丝衣", "金蝉衣",       // 4星
                    "逆鳞铠", "麒麟袍", "白泽衣",         // 4.5星
                    "毕方衣", "应龙铠", "盘古衣",         // 5星
                    "九天玄衣", "山海玄甲", "混元仙袍",   // 预留
            }},
            {"头盔", "T", 1, new String[]{
                    "草编盔", "木鳞盔", "皮弁盔",         // 1星
                    "山鬼盔", "木客盔", "夔皮盔",         // 1.5星
                    "兽面盔", "狼牙盔", "鬼头盔",         // 2星
                    "神鹰盔", "灵犀冠", "文鳐冠",         // 2.5星
                    "紫金冠", "芙蓉冠", "进贤冠",         // 3星
                    "蛟额盔", "赤蛇冠", "玄螭冠",         // 3.5星
                    "风后冠", "力牧盔", "大挠冠",         // 4星
                    "麒麟冠", "獬豸冠", "白泽冠",         // 4.5星
                    "烛龙冠", "应龙头盔", "天帝冕",       // 5星
                    "九凤冠", "混沌冕", "混元兜鍪",       // 预留
            }},
            {"鞋子", "X", 1, new String[]{
                    "青葛履", "白麻鞋", "草编履",         // 1星
                    "鹿皮靴", "狼牙靴", "豹纹靴",         // 1.5星
                    "轻舟履", "踏雪履", "寻风履",         // 2星
                    "凌波靴", "青云靴", "扶摇靴",         // 2.5星
                    "步云履", "御风靴", "腾云靴",         // 3星
                    "火云靴", "炎尘靴", "赤焰靴",         // 3.5星
                    "大椿履", "丹穴靴", "夸父靴",         // 4星
                    "逐日靴", "追星靴", "凌霄靴",         // 4.5星
                    "缩地靴", "渡海靴", "登仙履",         // 5星
                    "天行靴", "造化履", "八荒靴",         // 预留
            }},
            {"戒指", "Z", 2, new String[]{
                    "青石戒", "素陶戒", "兽骨戒",         // 1星
                    "玄铁戒", "赤铜戒", "错金戒",         // 1.5星
                    "云母戒", "水晶戒", "玛瑙戒",         // 2星
                    "琥珀戒", "翡翠戒", "珊瑚戒",         // 2.5星
                    "羊脂玉戒", "蓝田玉戒", "灵犀戒",     // 3星
                    "蛟筋环", "逆鳞戒", "照骨环",         // 3.5星
                    "辟邪戒", "平安环", "太一环",         // 4星
                    "琅玕戒", "青要戒", "槐江戒",         // 4.5星
                    "瑶池戒", "昆仑戒", "招摇环",         // 5星
                    "东皇戒", "混沌戒", "山海戒",         // 预留
            }},
            {"项链", "N", 2, new String[]{
                    "贝珠串", "石珠串", "骨坠串",         // 1星
                    "青玉瓔珞", "白珠串", "玄珠串",       // 1.5星
                    "灵珠链", "珊瑚珞", "琥珀串",         // 2星
                    "星月珞", "云母珞", "夜光串",         // 2.5星
                    "明月珠链", "骊龙珠链", "兰英珞",     // 3星
                    "火珠链", "赤炎珞", "流金珞",         // 3.5星
                    "沧海珠链", "碧落珞", "云璈珞",       // 4星
                    "扶桑珠链", "若木珞", "三危珞",       // 4.5星
                    "轩辕珞", "瑶池玉坠", "盘古神坠",     // 5星
                    "星辰珠链", "山海璎珞", "混元珠链",   // 预留
            }},
            {"披风", "P", 2, new String[]{
                    "粗布披风", "青蓑衣", "麻布披风",     // 1星
                    "狼皮披风", "狐尾披", "玄豹披",       // 1.5星
                    "清风披", "疾风披", "回风披",         // 2星
                    "云霞帔", "虹霓披", "雾绡披",         // 2.5星
                    "烟波披", "沧海披", "碧云披",         // 3星
                    "云龙披", "风虎披", "霞光披",         // 3.5星
                    "霓裳披", "九霄披", "八荒披",         // 4星
                    "羽渊披", "丹岳披", "金乌披",         // 4.5星
                    "天孙云披", "鲲鹏披", "烛阴披",       // 5星
                    "山海披风", "混沌披", "混元披",       // 预留
            }},
            {"印章", "Y", 3, new String[]{
                    "青石印", "素陶印", "槐木印",         // 1星
                    "玄铁印", "赤铜印", "错金印",         // 1.5星
                    "灵玉印", "水晶印", "琥珀印",         // 2星
                    "山鬼印", "河伯印", "木神印",         // 2.5星
                    "青鸟印", "三青印", "灵山印",         // 3星
                    "瑶池印", "蓬山印", "泰山水印",       // 3.5星
                    "五岳印", "四渎印", "社稷印",         // 4星
                    "九州印", "八荒印", "幽都印",         // 4.5星
                    "东皇印", "轩辕印", "天帝印",         // 5星
                    "山海印", "盘古印", "混元国印",       // 预留
            }},
            {"葫芦", "H", 3, new String[]{
                    "素瓢", "粗陶壶", "木匏壶",           // 1星
                    "青铜壶", "错金壶", "黑陶壶",         // 1.5星
                    "灵兰壶", "百药壶", "雪水壶",         // 2星
                    "玉阳壶", "冰纹壶", "云水壶",         // 2.5星
                    "青莲壶", "紫烟壶", "悬壶",           // 3星
                    "日月壶", "星辰壶", "吞山壶",         // 3.5星
                    "玄景壶", "紫金壶", "七星壶",         // 4星
                    "沧溟壶", "焚天壶", "九幽壶",         // 4.5星
                    "乾坤壶", "造化壶", "壶天宝光",       // 5星
                    "东皇玉壶", "混沌金壶", "混元仙壶",   // 预留
            }},
            {"魂幡", "B", 3, new String[]{
                    "素幡", "白麻幡", "青布幡",           // 1星
                    "引魂幡", "招魂幡", "复魂幡",         // 1.5星
                    "聚灵幡", "安魂幡", "定魄幡",         // 2星
                    "风雷幡", "云雾幡", "山岚幡",         // 2.5星
                    "九幽幡", "碧落幡", "幽冥幡",         // 3星
                    "北斗幡", "南斗幡", "太乙幡",         // 3.5星
                    "紫微幡", "勾陈幡", "摇光幡",         // 4星
                    "酆都幡", "北阴幡", "岱宗幡",         // 4.5星
                    "开天幡", "盘古幡", "烛龙幡",         // 5星
                    "万灵幡", "山海幡", "太古幡",         // 预留
            }},
    };

    // 装备大类 eq_type 常量（0兵刃 1防具 2宝具 3法器），供外部业务代码引用
    public static final int EQ_TYPE_WEAPON = 0;   // 兵刃：剑/弓/法杖
    public static final int EQ_TYPE_DEFENSE = 1;  // 防具：衣服/头盔/鞋子
    public static final int EQ_TYPE_TREASURE = 2; // 宝具：戒指/项链/披风
    public static final int EQ_TYPE_MAGIC = 3;    // 法器：印章/葫芦/魂幡

    // 子类序号(eq_type2)：EQUIP_CONFIG 下标+1（1~12）

    private static final Random RANDOM = new Random();

    // ========== 二、核心工具方法 ==========

    /** 生成装备基础介绍（山海经风格），被动技能留空（后期技能书系统提供） */
    private static String generateIntroduce(String equipName, String typeName) {
        String[] intros = {
                "%1$s，山海灵韵所铸%2$s，纹饰古拙，隐有异兽虚影浮现。",
                "%1$s，取山川之精、铸百炼之形，%2$s中暗藏洪荒之力。",
                "%1$s，传说为山海界遗珍，%2$s之魄犹存，静待有缘之主。",
        };
        String tpl = intros[RANDOM.nextInt(intros.length)];
        return tpl.replace("%1$s", equipName).replace("%2$s", typeName);
    }

    /** 属性-数据库攻防字段精准映射 */
    private static void mappingAttrToDbField(String attr, int value, EqCard eqCard) {
        switch (attr) {
            case "锋利": eqCard.setWlAtk(value); break;
            case "坚韧": eqCard.setWlDef(value); break;
            case "火焰": eqCard.setHyAtk(value); break;
            case "火抗": eqCard.setHyDef(value); break;
            case "毒素": eqCard.setDsAtk(value); break;
            case "毒抗": eqCard.setDsDef(value); break;
            case "飞弹": eqCard.setFdAtk(value); break;
            case "弹抗": eqCard.setFdDef(value); break;
            case "治愈": eqCard.setZlDef(value); break;
            case "受愈": eqCard.setZlAtk(value); break;
        }
    }

    /** 星级-属性生成规则：1/2/3属性分级生成，保证属性不重复；每条属性数值独立随机 */
    private static List<String> generateAttrByStar(double star, List<Integer> valueList) {
        List<String> attrList = new ArrayList<>();
        // 1~3星 → 1个属性
        if (star >= 1.0 && star <= 3.0) {
            String attr = BASE_ATTRS.get(RANDOM.nextInt(BASE_ATTRS.size()));
            attrList.add(attr);
            valueList.add(rollAttrValue(star));
        }
        // 3.5~4星 → 2个不同属性
        else if (star >= 3.5 && star <= 4.0) {
            int idx1 = RANDOM.nextInt(BASE_ATTRS.size());
            int idx2;
            do { idx2 = RANDOM.nextInt(BASE_ATTRS.size()); } while (idx1 == idx2);
            attrList.add(BASE_ATTRS.get(idx1));
            attrList.add(BASE_ATTRS.get(idx2));
            valueList.add(rollAttrValue(star));
            valueList.add(rollAttrValue(star));
        }
        // 4.5~5星 → 3个不同属性
        else if (star >= 4.5 && star <= 5.0) {
            Set<Integer> idxSet = new HashSet<>();
            while (idxSet.size() < 3) {
                idxSet.add(RANDOM.nextInt(BASE_ATTRS.size()));
            }
            for (int idx : idxSet) {
                attrList.add(BASE_ATTRS.get(idx));
            }
            for (int i = 0; i < 3; i++) {
                valueList.add(rollAttrValue(star));
            }
        }
        return attrList;
    }

    /**
     * 按星级基准值随机出单条属性数值：
     * 基准 = 星级 × 系数（1~3星×4，3.5~4星×5，4.5~5星×6），在基准 ±40% 范围内浮动。
     */
    private static int rollAttrValue(double star) {
        int base;
        if (star <= 3.0) {
            base = (int) (star * 4);
        } else if (star <= 4.0) {
            base = (int) (star * 5);
        } else {
            base = (int) (star * 6);
        }
        int spread = Math.max(1, (int) Math.round(base * 0.4));
        int value = base - spread + RANDOM.nextInt(spread * 2 + 1);
        return Math.max(1, value);
    }

    /** 星级 → 编号段位起始（1星→1、1.5星→4 ... 5星→25），段内随机3选1；028-030预留不可打造 */
    private static int rollEquipNum(double starLevel) {
        int tierIdx = -1;
        for (int i = 0; i < STAR_TIERS.length; i++) {
            if (Math.abs(STAR_TIERS[i] - starLevel) < 0.001) { tierIdx = i; break; }
        }
        if (tierIdx < 0) {
            throw new IllegalArgumentException("非法星级！仅支持：1、1.5、2、2.5、3、3.5、4、4.5、5");
        }
        return tierIdx * NUM_PER_STAR + 1 + RANDOM.nextInt(NUM_PER_STAR);
    }

    /** ID生成：{前缀}{001~030}_{属性字母}，编号部分即前端图片键（emp/{前缀+编号}.png） */
    private static String generateFinalEquipId(String equipPrefix, List<String> attrList, int equipNum) {
        StringBuilder idSb = new StringBuilder();
        idSb.append(equipPrefix).append(String.format("%03d", equipNum));
        idSb.append("_");
        attrList.forEach(attr -> idSb.append(ATTR_TO_CODE.get(attr)));
        return idSb.toString();
    }

    // ========== 三、对外核心方法 - 生成单个装备 ==========
    public static EqCard generateEqCard(double starLevel) {
        // 星级合法性校验
        if (!ALLOW_STARS.contains(starLevel)) {
            throw new IllegalArgumentException("非法星级！仅支持：1、1.5、2、2.5、3、3.5、4、4.5、5");
        }
        EqCard eqCard = new EqCard();
        eqCard.setStar(new BigDecimal(starLevel));

        // 随机选择装备子类（12子类均匀分布）
        int typeIdx = RANDOM.nextInt(EQUIP_CONFIG.length);
        Object[] config = EQUIP_CONFIG[typeIdx];
        String typeName = (String) config[0];
        String equipPrefix = (String) config[1];
        int eqType = (Integer) config[2];
        String[] nameList = (String[]) config[3];

        // 编号由星级段位随机决定，名称与编号一一对应（30名称池按星级从低到高排列）
        int equipNum = rollEquipNum(starLevel);
        String equipName = nameList[equipNum - 1];

        // 设置基础字段
        eqCard.setWeight(0.1 + RANDOM.nextDouble() * 9.9); // 权重0.1~10.0
        eqCard.setProfession(PROFESSION_LIST[RANDOM.nextInt(PROFESSION_LIST.length)]);
        eqCard.setIntroduce(generateIntroduce(equipName, typeName));
        // 被动技能全部为空：后期由技能书系统提供
        eqCard.setPassiveIntroduceOne(null);
        eqCard.setPassiveIntroduceTwo(null);
        eqCard.setPassiveIntroduceThree(null);

        // 设置装备大类/子类（eq_type2 = 子类序号1~12）
        eqCard.setEqType(eqType);
        eqCard.setEqType2(typeIdx + 1);

        // 生成属性+数值，映射到数据库字段
        List<Integer> valueList = new ArrayList<>();
        List<String> attrList = generateAttrByStar(starLevel, valueList);
        for (int i = 0; i < attrList.size(); i++) {
            mappingAttrToDbField(attrList.get(i), valueList.get(i), eqCard);
        }

        // 生成装备显示名称：【属性1.属性2.属性3.装备名】
        StringBuilder nameSb = new StringBuilder();
        attrList.forEach(attr -> nameSb.append(attr).append("."));
        nameSb.append(equipName);
        eqCard.setName(nameSb.toString());

        // 生成最终ID
        eqCard.setId(generateFinalEquipId(equipPrefix, attrList, equipNum));

        return eqCard;
    }

    // ========== 四、批量生成装备方法（推荐入库使用） ==========
    public static List<EqCard> batchGenerateEqCard(int count, double... assignStars) {
        List<EqCard> eqCardList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            double star;
            if (assignStars != null && assignStars.length > 0) {
                star = assignStars[RANDOM.nextInt(assignStars.length)];
            } else {
                star = STAR_TIERS[RANDOM.nextInt(STAR_TIERS.length)];
            }
            eqCardList.add(generateEqCard(star));
        }
        return eqCardList;
    }

    // ========== 五、测试主方法 ==========
    public static void main(String[] args) {
        System.out.println("======= 1星 单属性 =======");
        System.out.println(generateEqCard(1.0));

        System.out.println("\n======= 4星 双属性 =======");
        System.out.println(generateEqCard(4.0));

        System.out.println("\n======= 5星 三属性 =======");
        System.out.println(generateEqCard(5.0));

        System.out.println("\n======= 批量生成8个装备 =======");
        List<EqCard> list = batchGenerateEqCard(8, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5);
        list.forEach(System.out::println);
    }
}
