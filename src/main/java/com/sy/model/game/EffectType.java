package com.sy.model.game;

public enum EffectType {
    DISPEL("驱散"),
    DISP("闪避"),
    CRIT("暴击"),
    CRIT_UP("暴击提升"),
    CRIT_DOWN("暴击下降"),
    CRIT_UP_PRET("暴击提升"),
    CRIT_DOWN_PRET("暴击下降"),

    CRIT_DISP("暴击闪避"),
    SILENCE("沉默"),
    SILENCE_IMMUNE("沉默免疫"),
    STUN("眩晕"),
    STUN_IMMUNE("眩晕免疫"),
    BLOODTHIRST("嗜血"),
    CRAZY("疯狂"),
    // 蓄力：1~6层逐级展示，蓄满统一用XULIMAN
    XULI1("蓄力1"),
    XULI2("蓄力2"),
    XULI3("蓄力3"),
    XULI4("蓄力4"),
    XULI5("蓄力5"),
    XULI6("蓄力6"),
    XULIMAN("蓄力满"),
    DRAIN("吸血"),
    TRUE_DAMAGE("真实伤害"),
    FIXED_SOUL("固魂"),

    //治疗类
    HP_RECOVER("生命恢复"),
    XU_HEAL("续命治疗"),
    HEAL("治疗"),
    HEAL_BOOST("受到治疗提升"),
    HEAL_BOOST_PRET("受到治疗提升"),
    HEAL_DOWN("受到治疗下降"),
    HEAL_DOWNT_PRET("受到治疗下降"),
    XU_HEAL_BOOST("续命治疗提升"),
    XU_HEAL_BOOST_PRET("续命治疗提升"),
    XU_HEAL_DOWN("续命治疗下降"),
    XU_HEAL_DOWN_PRET("续命治疗下降"),
    
    //物理攻击
    DAMAGE("伤害"),
    ATTACK_UP("攻击提升"),
    ATTACK_DOWN("攻击下降"),
    ATTACK_UP_PRET("攻击提升"),
    ATTACK_DOWN_PRET("攻击下降"),
    ATTACK_RESIST_BOOST("物理抗性提升"),
    ATTACK_RESIST_BOOST_PRET("物理抗性提升"),
    ATTACK_RESIST_DOWN("物理抗性下降"),
    ATTACK_RESIST_DOWN_PRET("物理抗性下降"),
    
    
    
    //火焰伤害类
    BURN("灼烧"),
    FIRE_DAMAGE("火焰伤害"),
    FIRE_BOOST("火焰伤害提升"),
    FIRE_BOOST_PRET("火焰伤害提升"),
    FIRE_DOWN("火焰伤害下降"),
    FIRE_DOWN_PRET("火焰伤害下降"),
    FIRE_RESIST_BOOST("火焰抗性提升"),
    FIRE_RESIST_BOOST_PRET("火焰抗性提升"),
    FIRE_RESIST_DOWN("火焰抗性下降"),
    FIRE_RESIST_DOWN_PRET("火焰抗性下降"),


    //中毒类的
    POISON("中毒"),
    POISON_BOOST("中毒伤害提升"),
    POISON_BOOST_PRET("中毒伤害提升"),
    POISON_DOWN("中毒伤害下降"),
    POISON_DOWN_PRET("中毒伤害下降"),
    POISON_RESIST_BOOST("中毒抗性提升"),
    POISON_RESIST_BOOST_PRET("中毒抗性提升"),
    POISON_RESIST_DOWN("中毒抗性下降"),
    POISON_RESIST_DOWN_PRET("中毒抗性下降"),
    
    
    //飞弹类的
    MISSILE_DAMAGE("飞弹伤害"),
    MISSILE_BOOST("飞弹伤害提升"),
    MISSILE_BOOST_PRET("飞弹伤害提升"),
    MISSILE_DOWN("飞弹伤害下降"),
    MISSILE_DOWN_PRET("飞弹伤害下降"),
    MISSILE_RESIST_BOOST("飞弹抗性提升"),
    MISSILE_RESIST_BOOST_PRET("飞弹抗性提升"),
    MISSILE_RESIST_DOWN("飞弹抗性下降"),
    MISSILE_RESIST_DOWN_PRET("飞弹抗性下降"),

    //生命上限类
    HP_UP("生命上限提升"),
    HP_UP_PRET("生命上限提升"),
    MAX_HP_DOWN("生命上限下降"),
    MAX_HP_DOWN_PRET("生命上限下降"),
    MAX_HP_NO_DOWN("生命上限不下降"),
    
    //速度上限类
    SPEED_UP("速度提升"),
    SPEED_UP_PRET("速度提升"),
    SPEED_DOWN("速度下降"),
    SPEED_DOWN_PRET("速度下降"),
    PHYSICAL_BARRIER("物理结界");
    private String desc;
    EffectType(String desc) { this.desc = desc; }

    /**
     * 蓄力层数 → 蓄力EffectType
     * 1~6层依次对应 XULI1~XULI6；达到该角色蓄力上限（满层）或超过6层时统一用 XULIMAN
     * @param stacks    本次蓄力+1之后的当前层数
     * @param maxStacks 该角色的蓄力上限层数
     */
    public static EffectType fromChargeStacks(int stacks, int maxStacks) {
        // 蓄力已满
        if (maxStacks > 0 && stacks >= maxStacks) {
            return XULIMAN;
        }
        switch (stacks) {
            case 1:
                return XULI1;
            case 2:
                return XULI2;
            case 3:
                return XULI3;
            case 4:
                return XULI4;
            case 5:
                return XULI5;
            case 6:
                return XULI6;
            // 层数超过6层兜底为蓄力满
            default:
                return XULIMAN;
        }
    }
}