-- =====================================================================
-- 世界BOSS倍率列排查：单次挑战伤害占 boss 基准血量比例若小于列小数位精度，
-- DECIMAL 存储会四舍五入回原值，表现为"打了但 hp_rate 不变"。
-- 建议把三个倍率列统一到 DECIMAL(10,6)。
-- 请在"世界BOSS库"(10.126.126.1:3307/yimem) 上执行。
-- =====================================================================

-- 1) 先看当前列定义（确认 decimal 标度是否为 2 等过小精度）
SELECT COLUMN_NAME, COLUMN_TYPE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = 'yimem' AND TABLE_NAME = 'world_boss_daily'
  AND COLUMN_NAME IN ('hp_rate', 'atk_rate', 'speed_rate');

-- 2) 扩到 6 位小数（与代码回写 scale=6 对齐；扩大精度不影响存量数据；若原列允许 NULL，可按需去掉 NOT NULL）
ALTER TABLE world_boss_daily
    MODIFY COLUMN hp_rate    DECIMAL(10,6) NOT NULL DEFAULT 1.000000 COMMENT '血量倍率(0~1)，当天实际血量=base_hp*hp_rate，0表示已被击杀',
    MODIFY COLUMN atk_rate   DECIMAL(10,6) NOT NULL DEFAULT 1.000000 COMMENT '攻击倍率(0~1)，当天实际攻击=base_atk*atk_rate',
    MODIFY COLUMN speed_rate DECIMAL(10,6) NOT NULL DEFAULT 1.000000 COMMENT '速度倍率(0~1)，当天实际速度=base_speed*speed_rate';
