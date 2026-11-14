-- =====================================================================
-- 世界BOSS排班表 world_boss_daily：清重 + boss_date 唯一索引
--
-- 症状：定时任务每天没有给 world_boss_daily 新增当天行，玩家打世界BOSS 报“服务器异常”。
--
-- 根因（本脚本治的是第 2 条）：
--   1) 两套部署（梦回西游 / 再续前缘）共用同一个 worldboss 库，两个实例的排班守护（默认每小时，
--      worldboss.schedule-guard-cron 可调）会同一时刻各跑一次新增分支；两边都查不到当天行时就会各插一条，
--      于是同一个 boss_date 出现 2 行。
--   2) 之后所有按 boss_date 查询的地方如果用 selectOne，MyBatis-Plus 会抛 TooManyResultsException，
--      排班复位、倍率回写、挑战入口全部炸掉 —— 表现就是“既不新增，也进不去战斗”。
--   代码侧已把排班读取统一改成 selectList 取最新一条 + 异常兜底（WorldBossServiceImpl#selectDailyRow），
--      生成入口改成「守护定时任务（默认每小时）+ 启动自检」，一次铺未来 worldboss.schedule-ahead-days 天（默认 20）；
--      本脚本负责把数据治干净、并从根本上挡住并发重行。
--
-- 请在「世界BOSS库」上执行（10.126.126.1:3307/yimem，不是各服主库）。
-- =====================================================================

-- 1) 先看有没有重行（有结果就是要治的数据）
SELECT boss_date, COUNT(*) AS c, GROUP_CONCAT(id ORDER BY id) AS ids
FROM world_boss_daily
GROUP BY boss_date
HAVING c > 1;

-- 2) 同一天保留 id 最大的那条（代码取数口径就是 order by id desc 取第一条），其余删除
--    注意：只删排班行，不动 world_boss_record 的伤害记录，玩家的当日伤害与倍率结果不受影响。
DELETE d FROM world_boss_daily d
JOIN (
    SELECT boss_date, MAX(id) AS keep_id
    FROM world_boss_daily
    GROUP BY boss_date
    HAVING COUNT(*) > 1
) k ON k.boss_date = d.boss_date AND d.id <> k.keep_id;

-- 3) 建唯一索引，堵住两个实例并发各插一条（代码里已按“插入失败则改走复位”处理，加索引不会让任务报错）
ALTER TABLE `world_boss_daily`
  ADD UNIQUE INDEX `uk_boss_date` (`boss_date`);

-- ---------------------------------------------------------------------
-- 4) 顺带确认排班生成不出来是否还有另外两种原因（这两条不改代码，只能改数据）：
--    a. 没有启用的BOSS模板 → prepareDailySchedule 只能 warn 后返回 null，永远不会新增
--       SELECT COUNT(*) FROM world_boss_base WHERE is_active = 1;
--    b. 实体列比生产表多（speed_rate / reward_group_id / remark 是手工 ALTER 加的）
--       → 任何一次 SELECT/INSERT 都会 Unknown column，被兜底吞掉后排班依旧生成不了
--       SHOW COLUMNS FROM world_boss_daily;
--       缺列的话执行：sql/world_boss_daily_add_speed.sql、sql/world_boss_daily_rate_scale6.sql
-- ---------------------------------------------------------------------

-- 5) 治完之后补数据不用等任何时刻，直接走运维接口（会一次铺齐未来 N 天）：
--      GET http://<内网IP>:8889/test/worldboss/daily/ensure?days=30
--    一定要在库里手工灌时再用下面这段（只补今天，缺列/无启用模板时 base_boss_id 会插成 NULL）：
--    INSERT INTO world_boss_daily (boss_date, base_boss_id, start_time, end_time,
--                                  hp_rate, atk_rate, speed_rate, is_open, create_time, update_time)
--    SELECT CURDATE(),
--           (SELECT id FROM world_boss_base WHERE is_active = 1 ORDER BY sort, id LIMIT 1),
--           '09:00:00', '23:00:00', 1.000000, 1.000000, 1.000000, 1, NOW(), NOW()
--    FROM DUAL
--    WHERE NOT EXISTS (SELECT 1 FROM world_boss_daily WHERE boss_date = CURDATE());
