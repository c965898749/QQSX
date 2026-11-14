package com.sy.tool;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 世界BOSS库（10.126.126.1:3307/yimem）结构与数据的启动自愈。
 * <p>「world_boss_daily 每天不新增」的真实原因从来不是定时任务没跑，而是那几步 SQL 走不通：
 * 生产表少列（speed_rate/reward_group_id/remark/is_active 这些是后来手工 ALTER 的，实体里却已经映射了），
 * world_boss_base 的 is_active 全是 NULL，同一天被两个实例各插一行。
 * <p>同一类问题也坑过挑战：world_boss_record 缺 role_lv（跨服合并榜单时才进实体的列）时，
 * 战斗和倍率回写都已做完，最后一步 accumulateDamage 才报 Unknown column，
 * 又被接口层的 catch 盖成“世界BOSS暂未开放”——所以三张表都要按实体校验，不只是排班表。
 * 让人去库里手工跑脚本，改一版就得跑一次，跑错库还能把主库折腾坏 —— 所以这里由进程自己补齐：
 * 每一步都先查 information_schema，缺才 ALTER，齐就跳过，只做 ADD/MODIFY/补默认值这类向前安全的动作，
 * 绝不 DROP 列、绝不改业务数据，也不动各服自己的主库。
 * <p>想关掉：application.properties 里 {@code worldboss.schema-auto-fix=false}（关掉后排班生成仍依赖手工建表）。
 */
@Slf4j
@Component
public class WorldBossSchemaHealer {

    /**
     * 复用 worldboss 那套 SqlSessionFactory 已经建好的数据源，不再另外注册 DataSource Bean：
     * WorldBossDataSourceConfig 里特别说明过，把第二个 DataSource 注册成 Bean 会让 Druid 自动配置回退，
     * 把主库的装配整个搞坏。
     */
    @Autowired
    @Qualifier("worldbossSqlSessionFactory")
    private SqlSessionFactory worldbossSqlSessionFactory;

    @Value("${worldboss.schema-auto-fix:true}")
    private boolean autoFix;

    private volatile JdbcTemplate jdbcTemplate;

    /** world_boss_daily 需要的列：列名 -> 缺失时补的 DDL 片段（与 WorldBossDaily 实体一一对应） */
    private static final Map<String, String> DAILY_COLUMNS = new java.util.LinkedHashMap<>();
    /** world_boss_base 需要的列，同上 */
    private static final Map<String, String> BASE_COLUMNS = new java.util.LinkedHashMap<>();
    /** world_boss_record 需要的列，同上（伤害记录少一列会让整次挑战在回写后抛异常，前端只看到“暂未开放”） */
    private static final Map<String, String> RECORD_COLUMNS = new java.util.LinkedHashMap<>();

    static {
        DAILY_COLUMNS.put("boss_date", "DATE NOT NULL");
        DAILY_COLUMNS.put("base_boss_id", "INT NULL");
        DAILY_COLUMNS.put("start_time", "TIME NULL DEFAULT '09:00:00'");
        DAILY_COLUMNS.put("end_time", "TIME NULL DEFAULT '23:00:00'");
        DAILY_COLUMNS.put("hp_rate", "DECIMAL(10,6) NOT NULL DEFAULT 1.000000");
        DAILY_COLUMNS.put("atk_rate", "DECIMAL(10,6) NOT NULL DEFAULT 1.000000");
        DAILY_COLUMNS.put("speed_rate", "DECIMAL(10,6) NOT NULL DEFAULT 1.000000");
        DAILY_COLUMNS.put("reward_group_id", "INT NULL");
        DAILY_COLUMNS.put("is_open", "INT NOT NULL DEFAULT 1");
        DAILY_COLUMNS.put("remark", "VARCHAR(255) NULL");
        DAILY_COLUMNS.put("create_time", "DATETIME NULL");
        DAILY_COLUMNS.put("update_time", "DATETIME NULL");

        BASE_COLUMNS.put("boss_name", "VARCHAR(64) NULL");
        BASE_COLUMNS.put("boss_icon", "VARCHAR(255) NULL");
        BASE_COLUMNS.put("`desc`", "VARCHAR(500) NULL");
        BASE_COLUMNS.put("base_hp", "BIGINT NULL DEFAULT 0");
        BASE_COLUMNS.put("base_atk", "BIGINT NULL DEFAULT 0");
        BASE_COLUMNS.put("base_def", "BIGINT NULL DEFAULT 0");
        BASE_COLUMNS.put("base_speed", "INT NULL DEFAULT 0");
        BASE_COLUMNS.put("skill_id", "INT NULL");
        BASE_COLUMNS.put("difficulty_type", "INT NULL DEFAULT 1");
        BASE_COLUMNS.put("max_join_player", "INT NULL DEFAULT 0");
        BASE_COLUMNS.put("is_active", "INT NULL DEFAULT 1");
        BASE_COLUMNS.put("sort", "INT NULL DEFAULT 0");
        BASE_COLUMNS.put("create_time", "DATETIME NULL");
        BASE_COLUMNS.put("update_time", "DATETIME NULL");

        RECORD_COLUMNS.put("boss_date", "DATE NULL");
        RECORD_COLUMNS.put("role_id", "BIGINT NULL");
        RECORD_COLUMNS.put("role_name", "VARCHAR(64) NULL");
        RECORD_COLUMNS.put("img", "VARCHAR(255) NULL");
        // role_lv/server/img 是跨服合并榜单时才加进实体的列：漏补一列，accumulateDamage 的 SELECT 就整句 Unknown column
        RECORD_COLUMNS.put("role_lv", "INT NULL");
        RECORD_COLUMNS.put("server", "VARCHAR(64) NULL");
        RECORD_COLUMNS.put("total_damage", "BIGINT NULL DEFAULT 0");
        RECORD_COLUMNS.put("hit_count", "INT NULL DEFAULT 0");
        RECORD_COLUMNS.put("`rank`", "INT NULL DEFAULT 0");
        RECORD_COLUMNS.put("is_reward_get", "INT NULL DEFAULT 0");
        RECORD_COLUMNS.put("last_atk_time", "DATETIME NULL");
        RECORD_COLUMNS.put("create_time", "DATETIME NULL");
        RECORD_COLUMNS.put("update_time", "DATETIME NULL");
    }

    /**
     * 执行自愈，返回一句给日志/运维接口看的结论（做了什么，或者为什么没做）。
     * <p>任何一步失败都只记进结论里，不抛异常：结构补不上时排班照样生不成，
     * 但那不能让进程起不来 —— 主库和其他玩法比世界BOSS 重要得多。
     */
    public String heal() {
        if (!autoFix) {
            return "worldboss.schema-auto-fix=false，跳过结构自愈（排班依赖库里已有表结构）";
        }
        List<String> done = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        JdbcTemplate jdbc = jdbc();
        try {
            // 1) 三张表：不存在就按实体的完整形状建（存在则这句什么都不做）
            if (!tableExists(jdbc, "world_boss_daily")) {
                jdbc.execute("CREATE TABLE `world_boss_daily` ("
                        + "`id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                        + "`boss_date` DATE NOT NULL COMMENT '战斗日期（一天一行）',"
                        + "`base_boss_id` INT NULL COMMENT '关联 world_boss_base.id',"
                        + "`start_time` TIME NULL DEFAULT '09:00:00',"
                        + "`end_time` TIME NULL DEFAULT '23:00:00',"
                        + "`hp_rate` DECIMAL(10,6) NOT NULL DEFAULT 1.000000,"
                        + "`atk_rate` DECIMAL(10,6) NOT NULL DEFAULT 1.000000,"
                        + "`speed_rate` DECIMAL(10,6) NOT NULL DEFAULT 1.000000,"
                        + "`reward_group_id` INT NULL,"
                        + "`is_open` INT NOT NULL DEFAULT 1,"
                        + "`remark` VARCHAR(255) NULL,"
                        + "`create_time` DATETIME NULL,"
                        + "`update_time` DATETIME NULL,"
                        + "PRIMARY KEY (`id`), UNIQUE KEY `uk_boss_date` (`boss_date`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='每日世界BOSS排班表'");
                done.add("建表 world_boss_daily");
            }
            if (!tableExists(jdbc, "world_boss_base")) {
                jdbc.execute("CREATE TABLE `world_boss_base` ("
                        + "`id` INT NOT NULL AUTO_INCREMENT COMMENT 'BOSS模板ID',"
                        + "`boss_name` VARCHAR(64) NULL,"
                        + "`boss_icon` VARCHAR(255) NULL,"
                        + "`desc` VARCHAR(500) NULL,"
                        + "`base_hp` BIGINT NULL DEFAULT 0,"
                        + "`base_atk` BIGINT NULL DEFAULT 0,"
                        + "`base_def` BIGINT NULL DEFAULT 0,"
                        + "`base_speed` INT NULL DEFAULT 0,"
                        + "`skill_id` INT NULL,"
                        + "`difficulty_type` INT NULL DEFAULT 1,"
                        + "`max_join_player` INT NULL DEFAULT 0,"
                        + "`is_active` INT NULL DEFAULT 1,"
                        + "`sort` INT NULL DEFAULT 0,"
                        + "`create_time` DATETIME NULL,"
                        + "`update_time` DATETIME NULL,"
                        + "PRIMARY KEY (`id`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='世界BOSS基础模板'");
                done.add("建表 world_boss_base");
            }

            if (!tableExists(jdbc, "world_boss_record")) {
                jdbc.execute("CREATE TABLE `world_boss_record` ("
                        + "`id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                        + "`boss_date` DATE NULL COMMENT '对应哪一天的世界BOSS',"
                        + "`role_id` BIGINT NULL COMMENT '玩家角色ID',"
                        + "`role_name` VARCHAR(64) NULL,"
                        + "`img` VARCHAR(255) NULL COMMENT '头像快照',"
                        + "`role_lv` INT NULL COMMENT '等级快照',"
                        + "`server` VARCHAR(64) NULL COMMENT '所属服务器名',"
                        + "`total_damage` BIGINT NULL DEFAULT 0,"
                        + "`hit_count` INT NULL DEFAULT 0 COMMENT '挑战次数',"
                        + "`rank` INT NULL DEFAULT 0,"
                        + "`is_reward_get` INT NULL DEFAULT 0,"
                        + "`last_atk_time` DATETIME NULL,"
                        + "`create_time` DATETIME NULL,"
                        + "`update_time` DATETIME NULL,"
                        + "PRIMARY KEY (`id`), KEY `idx_boss_date_role` (`boss_date`, `role_id`)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='世界BOSS玩家伤害记录'");
                done.add("建表 world_boss_record");
            }

            // 2) 缺列就补：实体映射了而表里没有的列，会让 MyBatis-Plus 整句 Unknown column
            done.addAll(ensureColumns(jdbc, "world_boss_daily", DAILY_COLUMNS, failed));
            done.addAll(ensureColumns(jdbc, "world_boss_base", BASE_COLUMNS, failed));
            done.addAll(ensureColumns(jdbc, "world_boss_record", RECORD_COLUMNS, failed));
            ensureRecordIndex(jdbc, done, failed);

            // 2.1) 节点表（只有服务名与 IP）：本服身份与对端地址都靠它，启动预加载在它们上面跑
            ensureNodeTables(jdbc, done);

            // 3) 三个倍率列统一到 6 位小数：标度太小（DECIMAL(3,2)）时单次伤害占比会被四舍五入回原值，
            //    表现为「打了但 hp_rate 纹丝不动」
            ensureRateScale(jdbc, done, failed);

            // 4) is_active 的 NULL 补成 1：列是后来加的，老行留 NULL 就永远查不出「启用模板」
            ensureActiveTemplates(jdbc, done, failed);

            // 5) 同一天多行：留 id 最大的一条（与代码取数口径一致），否则先清重再加索引加不上
            dedupeDailyRows(jdbc, done, failed);

            // 6) boss_date 唯一索引：堵住两个实例同刻各插一条
            ensureUniqueBossDate(jdbc, done, failed);
        } catch (Exception e) {
            failed.add("整体异常：" + brief(e));
        }
        String report = "自愈结果：库=" + currentDb(jdbc)
                + (done.isEmpty() ? " 结构已符合实体，无需变更" : " 已执行[" + String.join("、", done) + "]")
                + (failed.isEmpty() ? "" : " 未成功[" + String.join("、", failed) + "]");
        log.info("世界BOSS库结构自愈，{}", report);
        return report;
    }

    /** 从 worldboss 那套工厂里取它自己的数据源，包成 JdbcTemplate（懒加载，双检锁） */
    private JdbcTemplate jdbc() {
        JdbcTemplate template = jdbcTemplate;
        if (template == null) {
            synchronized (this) {
                template = jdbcTemplate;
                if (template == null) {
                    template = new JdbcTemplate(
                            worldbossSqlSessionFactory.getConfiguration().getEnvironment().getDataSource());
                    jdbcTemplate = template;
                }
            }
        }
        return template;
    }

    private boolean tableExists(JdbcTemplate jdbc, String table) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.TABLES"
                + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?", Long.class, table);
        return count != null && count > 0;
    }

    private Set<String> columns(JdbcTemplate jdbc, String table) {
        List<String> list = jdbc.queryForList("SELECT COLUMN_NAME FROM information_schema.COLUMNS"
                + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?", String.class, table);
        Set<String> set = new HashSet<>();
        for (String name : list) {
            set.add(name.toLowerCase());
        }
        return set;
    }

    /** 逐个补少的列；名字带反引号的（如 `desc`）在比对时先剥掉 */
    private List<String> ensureColumns(JdbcTemplate jdbc, String table, Map<String, String> required, List<String> failed) {
        List<String> done = new ArrayList<>();
        Set<String> exists;
        try {
            exists = columns(jdbc, table);
        } catch (Exception e) {
            failed.add(table + " 列信息读取失败：" + brief(e));
            return done;
        }
        for (Map.Entry<String, String> entry : required.entrySet()) {
            String column = entry.getKey();
            String pure = column.replace("`", "").toLowerCase();
            if (exists.contains(pure)) {
                continue;
            }
            try {
                jdbc.execute("ALTER TABLE `" + table + "` ADD COLUMN " + column + " " + entry.getValue());
                done.add(table + "." + pure + " 补列");
                exists.add(pure);
            } catch (Exception e) {
                // 两个实例同时启动时可能一方已抢先把列加上，Duplicate column 就当成功
                if (brief(e).contains("Duplicate column")) {
                    exists.add(pure);
                } else {
                    failed.add(table + "." + pure + " 补列失败：" + brief(e));
                }
            }
        }
        return done;
    }

    private void ensureRateScale(JdbcTemplate jdbc, List<String> done, List<String> failed) {
        try {
            List<String> wrong = jdbc.queryForList("SELECT COLUMN_NAME FROM information_schema.COLUMNS"
                    + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'world_boss_daily'"
                    + " AND COLUMN_NAME IN ('hp_rate','atk_rate','speed_rate') AND COLUMN_TYPE <> 'decimal(10,6)'",
                    String.class);
            if (wrong.isEmpty()) {
                return;
            }
            jdbc.execute("ALTER TABLE `world_boss_daily`"
                    + " MODIFY COLUMN `hp_rate` DECIMAL(10,6) NOT NULL DEFAULT 1.000000,"
                    + " MODIFY COLUMN `atk_rate` DECIMAL(10,6) NOT NULL DEFAULT 1.000000,"
                    + " MODIFY COLUMN `speed_rate` DECIMAL(10,6) NOT NULL DEFAULT 1.000000");
            done.add("倍率精度统一到(10,6)");
        } catch (Exception e) {
            failed.add("倍率精度调整失败：" + brief(e));
        }
    }

    private void ensureActiveTemplates(JdbcTemplate jdbc, List<String> done, List<String> failed) {
        try {
            if (!columns(jdbc, "world_boss_base").contains("is_active")) {
                return;
            }
            int fixed = jdbc.update("UPDATE `world_boss_base` SET `is_active` = 1 WHERE `is_active` IS NULL");
            if (fixed > 0) {
                done.add("is_active 的 NULL 补成 1（" + fixed + " 行）");
            }
            // 整表都是 0（当初手工建表填的默认值）时不能替运营把所有 BOSS 打开，只提示一句
            Long active = jdbc.queryForObject("SELECT COUNT(*) FROM `world_boss_base` WHERE `is_active` = 1", Long.class);
            Long total = jdbc.queryForObject("SELECT COUNT(*) FROM `world_boss_base`", Long.class);
            if ((active == null || active == 0) && total != null && total > 0) {
                done.add("注意：world_boss_base 有 " + total + " 条模板但没有一条 is_active=1，"
                        + "排班已按全表模板兜底生成，要固定只轮某几个请把这列置 1");
            }
        } catch (Exception e) {
            failed.add("is_active 修补失败：" + brief(e));
        }
    }

    /** (boss_date, role_id) 查询索引：累加伤害与榜单都按这两列查，表建好时没带索引就在此补上 */
    private void ensureRecordIndex(JdbcTemplate jdbc, List<String> done, List<String> failed) {
        try {
            Long has = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.STATISTICS"
                    + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'world_boss_record'"
                    + " AND INDEX_NAME = 'idx_boss_date_role'", Long.class);
            if (has != null && has > 0) {
                return;
            }
            jdbc.execute("ALTER TABLE `world_boss_record` ADD INDEX `idx_boss_date_role` (`boss_date`, `role_id`)");
            done.add("建索引 world_boss_record.idx_boss_date_role");
        } catch (Exception e) {
            // 没索引只是慢一点，不影响能不能打，记进结论即可
            failed.add("idx_boss_date_role 未建成：" + brief(e));
        }
    }

    /**
     * 两张节点表：只存服务名与 IP，已经存在就什么都不做。
     * <p>不替运营插数据：本服叫什么、内网 IP 是哪个只有人知道，插错了会让记录写到错的服上；
     * 表为空时 {@code WorldBossNodeRegistry} 会回退到原来的 properties，服务不会因为空表而进不去。
     */
    private void ensureNodeTables(JdbcTemplate jdbc, List<String> done) {
        try {
            jdbc.execute("CREATE TABLE IF NOT EXISTS `world_boss_server` ("
                    + "`id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                    + "`server_name` VARCHAR(64) NOT NULL COMMENT '服务器名（与前端 serverList 一致）',"
                    + "`server_ip` VARCHAR(64) NOT NULL COMMENT '这台机器自己的内网 IP，进程用它对号入座',"
                    + "PRIMARY KEY (`id`), UNIQUE KEY `uk_server_name` (`server_name`),"
                    + " UNIQUE KEY `uk_server_ip` (`server_ip`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='世界BOSS各服身份表（只存服务名与IP）'");
            jdbc.execute("CREATE TABLE IF NOT EXISTS `world_boss_server_peer` ("
                    + "`id` INT NOT NULL AUTO_INCREMENT COMMENT '主键',"
                    + "`server_name` VARCHAR(64) NOT NULL COMMENT '对端服务器名',"
                    + "`server_ip` VARCHAR(64) NOT NULL COMMENT '对端内网 IP，端口用配置 worldboss.peer-port',"
                    + "PRIMARY KEY (`id`), UNIQUE KEY `uk_peer_name` (`server_name`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='世界BOSS跨服节点子表（其他服，可无限扩充）'");
            if (!tableExists(jdbc, "world_boss_server")) {
                done.add("world_boss_server 建表未生效（多为权限不够）");
            }
        } catch (Exception e) {
            log.warn("世界BOSS节点表自愈失败（节点信息会回退到配置文件）：{}", brief(e));
        }
    }

    private void dedupeDailyRows(JdbcTemplate jdbc, List<String> done, List<String> failed) {
        try {
            Long dupDates = jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT boss_date FROM `world_boss_daily`"
                    + " GROUP BY boss_date HAVING COUNT(*) > 1) t", Long.class);
            if (dupDates == null || dupDates == 0) {
                return;
            }
            int removed = jdbc.update("DELETE d FROM `world_boss_daily` d"
                    + " JOIN (SELECT boss_date, MAX(id) AS keep_id FROM `world_boss_daily`"
                    + " GROUP BY boss_date HAVING COUNT(*) > 1) k"
                    + " ON k.boss_date = d.boss_date AND d.id <> k.keep_id");
            done.add("同日重行清掉 " + removed + " 行（每天只留 id 最大的一条，不动伤害记录）");
        } catch (Exception e) {
            failed.add("同日重行清理失败：" + brief(e));
        }
    }

    private void ensureUniqueBossDate(JdbcTemplate jdbc, List<String> done, List<String> failed) {
        try {
            Long has = jdbc.queryForObject("SELECT COUNT(*) FROM information_schema.STATISTICS"
                    + " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'world_boss_daily'"
                    + " AND INDEX_NAME = 'uk_boss_date'", Long.class);
            if (has != null && has > 0) {
                return;
            }
            jdbc.execute("ALTER TABLE `world_boss_daily` ADD UNIQUE INDEX `uk_boss_date` (`boss_date`)");
            done.add("建唯一索引 uk_boss_date");
        } catch (Exception e) {
            // 索引加不上只意味着并发时可能再出现同日两行，代码侧已按 id 取最新一条，不算致命
            failed.add("uk_boss_date 未建成（多为还剩同日重行）：" + brief(e));
        }
    }

    private String currentDb(JdbcTemplate jdbc) {
        try {
            return jdbc.queryForObject("SELECT DATABASE()", String.class);
        } catch (Exception e) {
            return "(未知)";
        }
    }

    /** 异常压成一行：外层壳 + 最里层消息，多实例并发的 Duplicate 之类要靠这句判 */
    private static String brief(Exception e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String msg = t.getMessage();
        return t.getClass().getSimpleName() + ": " + (msg == null ? "(无异常信息)" : msg.replaceAll("\\s+", " ").trim());
    }
}
