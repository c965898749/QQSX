-- =====================================================================
-- 世界BOSS伤害记录表 world_boss_record：新增「等级快照」列 role_lv
--
-- 背景：世界BOSS 的伤害榜与每日结算已从「按服务器各自排名」改为「多服合并排名」
--   （榜单与名次的 rankList/assignRanks 不再带 server 过滤，榜上会出现别服玩家）。
--   各实例主库彼此独立且 roleId 会跨服重号，用 roleId 回本服主库查 lv 会把
--   别人的等级显示成他的，因此等级必须和挑战时的昵称/头像/服务器名一样冗余进记录。
--
-- server 列保持不变，但语义收窄：名次与榜单不再按它分组，它只用来判定
-- “这条记录属于哪个服”，因为 game_gift / game_gift_exchange_code 写的是各实例
-- 自己的主库 —— 兑换码只能由玩家所在服的实例发放，否则本服库里查不到码（领不到）。
--
-- 对应后端：
--   com.sy.model.game.WorldBossRecord#roleLv
--   WorldBossServiceImpl#accumulateDamage 写入 user.lv
--   GameServiceServiceImpl#mapRanking100 读快照，无快照时才回落本服主库
--
-- 请在「世界BOSS库」上执行（10.126.126.1:3307/yimem，不是各服主库）。
-- 若列已存在请忽略 ADD COLUMN 语句（MySQL ADD COLUMN 不支持 IF NOT EXISTS）。
-- =====================================================================

ALTER TABLE `world_boss_record`
  ADD COLUMN `role_lv` int NULL DEFAULT NULL COMMENT '玩家等级快照（=挑战时的 user.lv，合并榜单展示用，跨服不能回本服主库查）' AFTER `img`;

-- 名次改为全服合并后，榜单/结算的查询口径变成 (boss_date) + total_damage 排序，
-- 原 (boss_date, server, total_damage) 联合索引对不分服的查询帮助有限，补一条不带 server 的索引
ALTER TABLE `world_boss_record`
  ADD INDEX `idx_date_damage` (`boss_date`, `total_damage`);

-- ---------------------------------------------------------------------
-- 可选回填：历史记录的 role_lv 为 NULL，前端 lv 会显示 0（只影响看榜，不影响发奖）。
-- 若确要回填，只能填本服自己的记录（别服记录无通用可查来源）：
--   UPDATE world_boss_record r
--     JOIN `user` u ON u.userId = r.role_id
--   SET r.role_lv = u.lv
--   WHERE r.role_lv IS NULL AND r.server = '梦回西游' AND r.boss_date >= CURDATE();
--   （另一套部署把 server 换成 '再续前缘' 各执行一次；JOIN 走的仍是各实例自己的主库）
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
-- 切换当天必跑一次：名次口径从「本服前 100」改成「全服前 100」，
-- 旧的分服名次还留在 rank 列上。榜面按伤害顺序渲染不受影响，但落在
-- 全服 100 名之外的记录要等到当晚 23:05 结算才会被清空，白天可能显示
-- 一个过期的本服名次。上线时直接清一次即可（之后每天结算自会维护）：
--   UPDATE world_boss_record SET `rank` = NULL WHERE boss_date = CURDATE();
--   （rank 列若定义了 NOT NULL DEFAULT 0，把 NULL 改成 0）
-- ---------------------------------------------------------------------

-- 确认当前存量记录的服名与等级快照情况：
--   SELECT server, COUNT(*) total, SUM(role_lv IS NULL) lv_null
--   FROM world_boss_record WHERE boss_date >= CURDATE() - INTERVAL 1 DAY GROUP BY server;
