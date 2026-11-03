-- =====================================================================
-- 世界BOSS"速度"属性相关列（与血量/攻击保持一致的"基准 * 倍率"模型）
--   当天实际血量 = world_boss_base.base_hp    * world_boss_daily.hp_rate
--   当天实际攻击 = world_boss_base.base_atk   * world_boss_daily.atk_rate
--   当天实际速度 = world_boss_base.base_speed * world_boss_daily.speed_rate
-- 即：速度基准(base_speed)落在基础模板表 world_boss_base，速度倍率(speed_rate)落在每日排班表 world_boss_daily。
-- 请在"世界BOSS库"(当前路由为 10.126.126.1:3307/yimem) 上执行；
--   若该表实际在别的主机，请在对应库执行。
-- 若列已存在请忽略对应语句（MySQL ADD COLUMN 不支持 IF NOT EXISTS）。
-- =====================================================================

-- 速度基准：加到基础模板表 world_boss_base
ALTER TABLE `world_boss_base`
  ADD COLUMN `base_speed` int(11) NOT NULL DEFAULT 0 COMMENT '基础速度（当天实际速度=base_speed*world_boss_daily.speed_rate）' AFTER `base_def`;

-- 速度倍率：加到每日排班表 world_boss_daily
ALTER TABLE `world_boss_daily`
  ADD COLUMN `speed_rate` decimal(3, 2) NOT NULL DEFAULT 1.00 COMMENT '速度倍率（当天实际速度=world_boss_base.base_speed*speed_rate）' AFTER `atk_rate`;
