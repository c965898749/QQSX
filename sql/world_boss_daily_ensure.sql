-- =====================================================================
-- 世界BOSS每日排班：一份脚本把「world_boss_daily 每天不新增」的三种成因一次治干净
--
-- 必须在「世界BOSS库」上执行：10.126.126.1:3307 的 yimem（不是各服自己的主库）。
--   执行前先 USE 到那个库，脚本里全部用 DATABASE()，不写死库名。
--
-- 为什么以前会不新增（三条都在这脚本里治，代码侧已同步加固）：
--   1) 表结构与实体不一致。speed_rate / reward_group_id / remark / is_active 这些列是后来手工 ALTER 加的，
--      只要生产表少任何一个，MyBatis-Plus 的 SELECT/INSERT 就整句报 Unknown column；
--      而旧代码把这个异常归成一句“多为另一实例已抢先写入”的 warn，看起来像并发问题，实际是缺列。
--   2) world_boss_base 没有 is_active=1 的行（列新加时老数据是 NULL，或全是 0）。
--      旧代码只认 is_active=1，查不到就直接 warn 返回 null，于是永远生不成排班行。
--      代码现在会退回「全表模板」，但数据仍建议按本脚本放开。
--   3) 两个实例共用本库，守护任务同刻各插一次 → 同一 boss_date 两行，后续 selectOne 抛 TooManyResults。
--      代码已改成按 id 取最新一条，本脚本负责清重 + 建唯一索引从根上堵住。
--
-- 全程幂等：已存在的东西一律跳过，可以反复执行。
--
-- ⚠ 现在这份脚本已经是可选的：同样的事情进程自己会做（com.sy.tool.WorldBossSchemaHealer，
--   启动时与守护任务（默认每小时，worldboss.schedule-guard-cron 可调）补建失败时跑一轮，
--   开关是 worldboss.schema-auto-fix，默认开）。
--   留着它的用处只有一个：关掉自愈、或想让 DBA 先看过再改结构时，照着这份手工执行。
-- 跑完之后不用重启、不用等定时任务，直接看这一行就知道有没有生效：
--   GET http://<内网IP>:8889/test/worldboss/daily          只看现状
--   GET http://<内网IP>:8889/test/worldboss/daily/ensure    自愈 + 补建 + 报现状（一条 URL 到底）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 0) 自检三段：先单独跑这三条，把结果贴出来，就能确定到底是上面哪一种
-- ---------------------------------------------------------------------
-- 0.1 两张表在不在
SELECT TABLE_NAME FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('world_boss_daily', 'world_boss_base');
-- 0.2 排班表实际有哪些列（和下面 1) 里要求的列名对比）
SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'world_boss_daily' ORDER BY ORDINAL_POSITION;
-- 0.3 模板表有没有可用的（is_active=1 为 0 而全表不为 0，就是成因 2）
SELECT COUNT(*) AS total, SUM(CASE WHEN is_active = 1 THEN 1 ELSE 0 END) AS active_cnt
FROM world_boss_base;


-- ---------------------------------------------------------------------
-- 1) 表不存在就按实体的完整形状建表（已存在则这句什么都不做）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `world_boss_daily` (
  `id`              INT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `boss_date`       DATE NOT NULL COMMENT '战斗日期（一天一行）',
  `base_boss_id`    INT NULL COMMENT '关联 world_boss_base.id',
  `start_time`      TIME NULL DEFAULT '09:00:00' COMMENT '当天开启时间',
  `end_time`        TIME NULL DEFAULT '23:00:00' COMMENT '当天结束时间',
  `hp_rate`         DECIMAL(10,6) NOT NULL DEFAULT 1.000000 COMMENT '血量倍率(0~1)，0=已击杀',
  `atk_rate`        DECIMAL(10,6) NOT NULL DEFAULT 1.000000 COMMENT '攻击倍率(0~1)',
  `speed_rate`      DECIMAL(10,6) NOT NULL DEFAULT 1.000000 COMMENT '速度倍率(0~1)',
  `reward_group_id` INT NULL COMMENT '奖励组ID，代码不写可为空',
  `is_open`         INT NOT NULL DEFAULT 1 COMMENT '当天是否开启 0关闭 1开启',
  `remark`          VARCHAR(255) NULL COMMENT '备注',
  `create_time`     DATETIME NULL COMMENT '创建时间',
  `update_time`     DATETIME NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_boss_date` (`boss_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='每日世界BOSS排班表';

CREATE TABLE IF NOT EXISTS `world_boss_base` (
  `id`              INT NOT NULL AUTO_INCREMENT COMMENT 'BOSS模板ID',
  `boss_name`       VARCHAR(64) NULL COMMENT 'BOSS名称',
  `boss_icon`       VARCHAR(255) NULL COMMENT '图标资源',
  `desc`            VARCHAR(500) NULL COMMENT 'BOSS简介（保留字，代码用反引号）',
  `base_hp`         BIGINT NULL DEFAULT 0 COMMENT '基础血量',
  `base_atk`        BIGINT NULL DEFAULT 0 COMMENT '基础攻击',
  `base_def`        BIGINT NULL DEFAULT 0 COMMENT '基础防御',
  `base_speed`      INT NULL DEFAULT 0 COMMENT '基础速度（实际=base_speed*daily.speed_rate）',
  `skill_id`        INT NULL COMMENT '专属技能ID',
  `difficulty_type` INT NULL DEFAULT 1 COMMENT '难度 1普通 2困难 3噩梦',
  `max_join_player` INT NULL DEFAULT 0 COMMENT '最大参与人数',
  `is_active`       INT NULL DEFAULT 1 COMMENT '是否启用 0禁用 1启用',
  `sort`            INT NULL DEFAULT 0 COMMENT '排序',
  `create_time`     DATETIME NULL COMMENT '创建时间',
  `update_time`     DATETIME NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='世界BOSS基础模板';


-- ---------------------------------------------------------------------
-- 2) 排班表补列：只拼当前缺的那几列，一句 ALTER 搞定；列齐全时什么都不做
--    （把实体里映射到的列都列进来，缺一个就会 Unknown column，见开头成因 1）
-- ---------------------------------------------------------------------
SET @missing := (SELECT GROUP_CONCAT(CONCAT('ADD COLUMN `', c.col, '` ', c.def) ORDER BY c.ord SEPARATOR ', ')
  FROM (
        SELECT 'boss_date'       col, 'DATE NOT NULL'                                   def, 1 ord
  UNION ALL SELECT 'base_boss_id',   'INT NULL'                                                , 2
  UNION ALL SELECT 'start_time',     'TIME NULL DEFAULT ''09:00:00'''                           , 3
  UNION ALL SELECT 'end_time',       'TIME NULL DEFAULT ''23:00:00'''                           , 4
  UNION ALL SELECT 'hp_rate',        'DECIMAL(10,6) NOT NULL DEFAULT 1.000000'                  , 5
  UNION ALL SELECT 'atk_rate',       'DECIMAL(10,6) NOT NULL DEFAULT 1.000000'                  , 6
  UNION ALL SELECT 'speed_rate',     'DECIMAL(10,6) NOT NULL DEFAULT 1.000000'                  , 7
  UNION ALL SELECT 'reward_group_id','INT NULL'                                                , 8
  UNION ALL SELECT 'is_open',        'INT NOT NULL DEFAULT 1'                                  , 9
  UNION ALL SELECT 'remark',         'VARCHAR(255) NULL'                                       , 10
  UNION ALL SELECT 'create_time',    'DATETIME NULL'                                           , 11
  UNION ALL SELECT 'update_time',    'DATETIME NULL'                                           , 12
  ) c
  WHERE NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS x
                    WHERE x.TABLE_SCHEMA = DATABASE() AND x.TABLE_NAME = 'world_boss_daily'
                      AND x.COLUMN_NAME = c.col));

SET @sql := IF(@missing IS NULL OR @missing = '',
               'SELECT ''world_boss_daily 列已齐全，无需补列'' AS action',
               CONCAT('ALTER TABLE `world_boss_daily` ', @missing));
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- id 列一般跟着建表就有；真缺的话上面循环故意没带它（AUTO_INCREMENT 要配主键，混在一起容易出错），
-- 这条会直接告诉你需不需要手工处理
SELECT IF(COUNT(*) = 0,
      '需要手工处理：world_boss_daily 没有 id 列，请执行 ALTER TABLE `world_boss_daily` ADD COLUMN `id` INT NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY(`id`);',
      'world_boss_daily.id 正常') AS action
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'world_boss_daily' AND COLUMN_NAME = 'id';

-- 三个倍率列统一到 6 位小数：标度太小（如 DECIMAL(3,2)）时单次挑战的伤害占比会被四舍五入回原值，
-- 表现为“打了但 hp_rate 纹丝不动”（见 sql/world_boss_daily_rate_scale6.sql）
ALTER TABLE `world_boss_daily`
  MODIFY COLUMN `hp_rate`    DECIMAL(10,6) NOT NULL DEFAULT 1.000000 COMMENT '血量倍率(0~1)，当天实际血量=base_hp*hp_rate，0表示已被击杀',
  MODIFY COLUMN `atk_rate`   DECIMAL(10,6) NOT NULL DEFAULT 1.000000 COMMENT '攻击倍率(0~1)，当天实际攻击=base_atk*atk_rate',
  MODIFY COLUMN `speed_rate` DECIMAL(10,6) NOT NULL DEFAULT 1.000000 COMMENT '速度倍率(0~1)，当天实际速度=base_speed*speed_rate';


-- ---------------------------------------------------------------------
-- 3) 模板表补列 + 放开 is_active（成因 2）
-- ---------------------------------------------------------------------
SET @missing2 := (SELECT GROUP_CONCAT(CONCAT('ADD COLUMN `', c.col, '` ', c.def) ORDER BY c.ord SEPARATOR ', ')
  FROM (
        SELECT 'boss_name'       col, 'VARCHAR(64) NULL'      def, 1 ord
  UNION ALL SELECT 'boss_icon',     'VARCHAR(255) NULL'            , 2
  UNION ALL SELECT 'desc',          'VARCHAR(500) NULL'            , 3
  UNION ALL SELECT 'base_hp',       'BIGINT NULL DEFAULT 0'        , 4
  UNION ALL SELECT 'base_atk',      'BIGINT NULL DEFAULT 0'        , 5
  UNION ALL SELECT 'base_def',      'BIGINT NULL DEFAULT 0'        , 6
  UNION ALL SELECT 'base_speed',    'INT NULL DEFAULT 0'           , 7
  UNION ALL SELECT 'skill_id',      'INT NULL'                     , 8
  UNION ALL SELECT 'difficulty_type','INT NULL DEFAULT 1'          , 9
  UNION ALL SELECT 'max_join_player','INT NULL DEFAULT 0'          , 10
  UNION ALL SELECT 'is_active',     'INT NULL DEFAULT 1'           , 11
  UNION ALL SELECT 'sort',          'INT NULL DEFAULT 0'           , 12
  UNION ALL SELECT 'create_time',   'DATETIME NULL'                , 13
  UNION ALL SELECT 'update_time',   'DATETIME NULL'                , 14
  ) c
  WHERE NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS x
                    WHERE x.TABLE_SCHEMA = DATABASE() AND x.TABLE_NAME = 'world_boss_base'
                      AND x.COLUMN_NAME = c.col));

SET @sql := IF(@missing2 IS NULL OR @missing2 = '',
               'SELECT ''world_boss_base 列已齐全，无需补列'' AS action',
               CONCAT('ALTER TABLE `world_boss_base` ', @missing2));
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;

-- 新加列时老行是 NULL，NULL 不等于 1，按 is_active=1 查就一条也查不到 —— 这就是“没有启用模板”的真身
UPDATE `world_boss_base` SET `is_active` = 1 WHERE `is_active` IS NULL;
-- 整表都是 0 的情况（多半是当初手工建表填的默认值）需要人工确认后再放开，所以留成注释：
--   先看： SELECT COUNT(*) AS zero_cnt FROM world_boss_base WHERE is_active <> 1 OR is_active IS NULL;
--   再放： UPDATE world_boss_base SET is_active = 1;
-- 代码侧已经改成「is_active=1 查不到就退回全表模板」，所以就算不放开也能生成排班，只是日志会一直 warn


-- ---------------------------------------------------------------------
-- 4) 清掉同一天的重复排班行 + 建 boss_date 唯一索引（成因 3）
--    只删排班行，绝不动 world_boss_record 的伤害记录。
-- ---------------------------------------------------------------------
DELETE d FROM `world_boss_daily` d
JOIN (
    SELECT boss_date, MAX(id) AS keep_id
    FROM world_boss_daily
    GROUP BY boss_date
    HAVING COUNT(*) > 1
) k ON k.boss_date = d.boss_date AND d.id <> k.keep_id;

SET @sql := IF(EXISTS(SELECT 1 FROM information_schema.STATISTICS
                      WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'world_boss_daily'
                        AND INDEX_NAME = 'uk_boss_date'),
               'SELECT ''uk_boss_date 已存在，跳过'' AS action',
               'ALTER TABLE `world_boss_daily` ADD UNIQUE INDEX `uk_boss_date` (`boss_date`)');
PREPARE st FROM @sql; EXECUTE st; DEALLOCATE PREPARE st;


-- ---------------------------------------------------------------------
-- 5) 立刻补上今天的排班行（不等任何 cron，也不用等人去点世界BOSS）
--    取模板的口径与代码一致：is_active=1 优先、按 sort/id 排，没有启用的就退全表。
-- ---------------------------------------------------------------------
INSERT INTO `world_boss_daily`
  (boss_date, base_boss_id, start_time, end_time, hp_rate, atk_rate, speed_rate, is_open, create_time, update_time)
SELECT CURDATE(), t.id, '09:00:00', '23:00:00', 1.000000, 1.000000, 1.000000, 1, NOW(), NOW()
FROM (
  SELECT id FROM world_boss_base
  ORDER BY (CASE WHEN is_active = 1 THEN 0 ELSE 1 END), sort, id
  LIMIT 1
) t
WHERE NOT EXISTS (SELECT 1 FROM world_boss_daily WHERE boss_date = CURDATE());

-- 昨天的行如果也没生成，按同样口径补：把 CURDATE() 换成 CURDATE() - INTERVAL 1 DAY 再执行一次。
-- 注意历史日期补出来的行三率是满值，只影响那一天的显示，不影响今天。


-- ---------------------------------------------------------------------
-- 6) 验证：这两条的结果就应该是「今天一行、指向一个真存在的 BOSS」
-- ---------------------------------------------------------------------
SELECT d.id, d.boss_date, d.base_boss_id, b.boss_name, d.start_time, d.end_time,
       d.hp_rate, d.atk_rate, d.speed_rate, d.is_open, d.create_time, d.update_time
FROM world_boss_daily d LEFT JOIN world_boss_base b ON b.id = d.base_boss_id
WHERE d.boss_date = CURDATE();

-- 顺手看一眼最近 7 天有没有空着的日子（缺哪天就照 5) 补哪天）
SELECT DATE_SUB(CURDATE(), INTERVAL n.day DAY) AS expect_date,
       (SELECT COUNT(*) FROM world_boss_daily w
         WHERE w.boss_date = DATE_SUB(CURDATE(), INTERVAL n.day DAY)) AS rows_cnt
FROM (SELECT 0 day UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3
      UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) n
ORDER BY n.day;
