package com.sy.model.game;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Map;

@Data
@AllArgsConstructor
public class BattleLog {
    private String battleId;                // 战斗ID（核心标识）
    private int round;                      // 回合数（核心时序信息）
    private String eventType;               // 事件类型/技能名称（核心事件标识）
    private Integer flyup;               // 事件类型/技能名称（核心事件标识）

    // 来源单位核心信息
    private String sourceUnitId;            // 来源单位唯一ID
    private int sourceHpBefore;             // 来源单位作用前血量
    private int sourceHpAfter;              // 来源单位作用后血量
    private int sourceSelfValue;        // 来源单位自身数值（自疗/自损等，null表示无）
    private boolean sourceFieldStatus; // 单目标的场上/场下状态

    // 单目标场景专属字段（与多目标字段二选一使用）
    private String targetUnitId;            // 单个目标单位唯一ID
    private int targetHpBefore;             // 单个目标作用前血量
    private int targetHpAfter;              // 单个目标作用后血量
    private int singleTargetValue;      // 单个目标对应的数值（伤害/治疗等）
    private boolean targetFieldStatus; // 单目标的场上/场下状态

    // 多目标场景专属字段（合并后的唯一Map）
    private Map<String, TargetBattleData> multiTargetDataMap;

    private EffectType effectType;          // 效果类型（核心分类）
    private DamageType damageType;          // 伤害类型（如果适用）
    private String extraDesc;               // 额外描述信息（可选）
    private int aoe;
    private int shieldRemaining;        // 目标当前盾（物理结界）剩余值
    private Integer yinjiCount;         // 应龙当前印记层数（仅应龙相关日志回写，供前端刷新场下 yinji 动画的 num；null 表示该日志不涉及印记变化）

    // 便捷构造：与旧版 20 参全字段构造签名一致（不含 yinjiCount），委托给 Lombok @AllArgsConstructor 生成的 21 参构造，yinjiCount 默认 null。
    // 这样既有 new BattleLog(20 参) 调用点无需修改即可编译；需要回写印记的应龙日志通过 setYinjiCount 追加。
    public BattleLog(String battleId, int round, String eventType, Integer flyup,
                     String sourceUnitId, int sourceHpBefore, int sourceHpAfter, int sourceSelfValue, boolean sourceFieldStatus,
                     String targetUnitId, int targetHpBefore, int targetHpAfter, int singleTargetValue, boolean targetFieldStatus,
                     Map<String, TargetBattleData> multiTargetDataMap, EffectType effectType, DamageType damageType, String extraDesc,
                     int aoe, int shieldRemaining) {
        this(battleId, round, eventType, flyup, sourceUnitId, sourceHpBefore, sourceHpAfter, sourceSelfValue, sourceFieldStatus,
                targetUnitId, targetHpBefore, targetHpAfter, singleTargetValue, targetFieldStatus, multiTargetDataMap,
                effectType, damageType, extraDesc, aoe, shieldRemaining, null);
    }
}