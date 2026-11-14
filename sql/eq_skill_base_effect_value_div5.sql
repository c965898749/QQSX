-- =====================================================================================
-- 装备技能数值折算：eq_skill_base.effect_value 全部 ÷5（存量库迁移）
-- 执行库：master（游戏主库 yimem）—— 梦回西游、再续前缘【两套主库都要执行】
--        注意：eq_skill_base 走 com.sy.mapper.game（各服独立主库），不在世界BOSS共用库(10.126.126.1:3307)里。
--
-- 背景（为什么要改）：
--   技能等级公式已简化为「等级 = Σ宝石等级 ÷ 宝石槽数」= 宝石平均等级，而每类宝石是 Lv1~Lv50，
--   所以等级最高能到 50。效果数值 = effect_value × 等级，若不折算，满级宝石下会出现
--   「破甲无视 100% 防御」「万象归一 500% 几率净化」这类溢出数值。
--   把每级值 ÷5 后：Lv50 × 0.40 = 20%，正好等于最初的设计上限（原每级值 ×10），低段也线性收敛。
--
-- 幂等性：全部按 skill_id 写【绝对值】，重复执行结果一致，不会二次折半。
-- ⚠ 只改 effect_value，不动 description（{v} 占位符由后端按等级回填）。
-- =====================================================================================

-- ---------- 执行前先看一眼现状（应为 2.00 / 3.00 / 5.00 ... 这类未折算值） ----------
SELECT skill_id, name, tier, scope, effect_value, description FROM eq_skill_base ORDER BY scope, tier, skill_id;

-- ---------- 通用池（scope=0） ----------
UPDATE eq_skill_base SET effect_value = 0.40 WHERE skill_id = 1;   -- 破甲     无视防御%   （原 2.00）
UPDATE eq_skill_base SET effect_value = 0.60 WHERE skill_id = 2;   -- 嗜血     吸血%       （原 3.00）
UPDATE eq_skill_base SET effect_value = 0.40 WHERE skill_id = 3;   -- 铁骨     物理减伤%   （原 2.00）
UPDATE eq_skill_base SET effect_value = 1.00 WHERE skill_id = 4;   -- 迅捷     速度点      （原 5.00）
UPDATE eq_skill_base SET effect_value = 0.80 WHERE skill_id = 5;   -- 连击     追击几率%   （原 4.00）
UPDATE eq_skill_base SET effect_value = 0.60 WHERE skill_id = 6;   -- 格挡     抵挡几率%   （原 3.00）
UPDATE eq_skill_base SET effect_value = 0.80 WHERE skill_id = 7;   -- 狂暴     攻击上限%   （原 4.00）
UPDATE eq_skill_base SET effect_value = 1.20 WHERE skill_id = 8;   -- 法术精通 技能系数%   （原 6.00）
UPDATE eq_skill_base SET effect_value = 1.00 WHERE skill_id = 9;   -- 不灭     防御%       （原 5.00）
UPDATE eq_skill_base SET effect_value = 0.60 WHERE skill_id = 10;  -- 天罡护体 反弹%       （原 3.00）
UPDATE eq_skill_base SET effect_value = 1.60 WHERE skill_id = 11;  -- 致命一击 暴击几率%   （原 8.00）
UPDATE eq_skill_base SET effect_value = 2.00 WHERE skill_id = 12;  -- 万象归一 净化几率%   （原 10.00）Lv50 恰好 100%

-- ---------- 英雄池（scope=1） ----------
UPDATE eq_skill_base SET effect_value = 0.20 WHERE skill_id = 13;  -- 烈焰焚天   灼烧%      （原 1.00）
UPDATE eq_skill_base SET effect_value = 1.00 WHERE skill_id = 14;  -- 寒冰护盾   护盾%      （原 5.00）
UPDATE eq_skill_base SET effect_value = 1.00 WHERE skill_id = 15;  -- 疾风连斩   攻速%      （原 5.00）
UPDATE eq_skill_base SET effect_value = 2.00 WHERE skill_id = 16;  -- 大地的怒吼 眩晕几率%   （原 10.00）

-- ---------- 运营后续自行新增的技能（skill_id > 16）本脚本不覆盖 ----------
-- 这些行必须按「设计值 ÷5」填 effect_value，否则依旧是 Lv50 溢出的老口径。
-- 想批量折算可以先 SELECT 复核、再手工放开下面这条（注意：它不幂等，只能跑一次）：
-- SELECT skill_id, name, effect_value, effect_value / 5 AS after_div5
-- FROM eq_skill_base WHERE skill_id > 16 AND effect_value IS NOT NULL;
-- UPDATE eq_skill_base SET effect_value = ROUND(effect_value / 5, 2) WHERE skill_id > 16 AND effect_value IS NOT NULL;

-- ---------- 验证：每级值 ×50 就是玩家满级宝石能看到的数值，不应超过描述语义的合理区间 ----------
SELECT skill_id, name, effect_value AS 每级值,
       ROUND(effect_value * 50, 1)  AS 满级Lv50数值,
       ROUND(effect_value * 10, 1)  AS Lv10数值,
       description
FROM eq_skill_base ORDER BY scope, tier, skill_id;

-- 说明：加列/改值后无需清缓存——eq_skill_base 每次查询直接走库（eqSkillBaseMapper.selectById），
--       玩家重新打开炼化面板即可看到新数值；正在展示中的旧描述会随下次请求刷新。
