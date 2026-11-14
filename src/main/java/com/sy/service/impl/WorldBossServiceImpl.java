package com.sy.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sy.worldboss.mapper.WorldBossBaseMapper;
import com.sy.worldboss.mapper.WorldBossDailyMapper;
import com.sy.worldboss.mapper.WorldBossRecordMapper;
import com.sy.model.game.WorldBossBase;
import com.sy.model.game.WorldBossDaily;
import com.sy.model.game.WorldBossRecord;
import com.sy.service.WorldBossService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class WorldBossServiceImpl implements WorldBossService {

    @Autowired
    private WorldBossBaseMapper worldBossBaseMapper;

    @Autowired
    private WorldBossDailyMapper worldBossDailyMapper;

    @Autowired
    private WorldBossRecordMapper worldBossRecordMapper;

    /** 满值基准：每日重置时血量/攻击/速度倍率复位为此值 */
    private static final BigDecimal FULL_RATE = new BigDecimal("1.00");

    /** 默认时间窗：09:00 开 / 23:00 关，与 WorldBossScheduleTask 的 OPEN_TIME/CLOSE_TIME 一致 */
    private static final LocalTime DEFAULT_OPEN = LocalTime.of(9, 0);
    private static final LocalTime DEFAULT_CLOSE = LocalTime.of(23, 0);

    /**
     * 上一次「生成当天排班」失败的真实原因。
     * <p>排班生成不出来在日志里原本只有一句含糊的 warn，而 MySQL 的真实原因（Unknown column、Table doesn't exist、
     * Duplicate entry）全部在被 catch 掉的异常里；这里把它留住，让守护任务和运维接口能一句话讲清卡在何处。
     */
    private volatile String lastScheduleFailure;

    /** 服务器名是否为有效过滤条件（未配置时退化为不分服，兼容单服/本地环境） */
    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    /**
     * 取异常链最里层的 message：Spring 会把 MySQL 报错包成 DataIntegrityViolationException 等外层壳，
     * 只看外层就只能是“数据库操作异常”，而真正能定位问题的“Unknown column 'speed_rate'”在最里面。
     */
    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String msg = t.getMessage();
        if (msg == null || msg.trim().isEmpty()) {
            msg = "(无异常信息)";
        }
        return t.getClass().getSimpleName() + ": " + msg.replaceAll("\\s+", " ").trim();
    }

    /**
     * 取当天排班行，所有读 world_boss_daily 的地方统一走这里。
     * <p>不用 selectOne：世界BOSS库由多个实例共用，两个实例的守护任务同刻都会跑到新增分支，
     * boss_date 上没有唯一索引时就会写出两条同日记录，selectOne 直接抛 TooManyResultsException，
     * 异常透到接口层就是前端一句“服务器异常”。改成取最新一条 + 异常兼底，脏数据也能让玩家正常打。
     */
    private WorldBossDaily selectDailyRow(LocalDate date) {
        try {
            List<WorldBossDaily> list = worldBossDailyMapper.selectList(new LambdaQueryWrapper<WorldBossDaily>()
                    .eq(WorldBossDaily::getBossDate, date)
                    .orderByDesc(WorldBossDaily::getId));
            if (list == null || list.isEmpty()) {
                return null;
            }
            if (list.size() > 1) {
                log.warn("世界BOSS排班：{} 存在 {} 条记录（boss_date 缺唯一索引），取 id={} 这条，请执行 sql/world_boss_daily_unique_boss_date.sql 清重",
                        date, list.size(), list.get(0).getId());
            }
            return list.get(0);
        } catch (Exception e) {
            lastScheduleFailure = "查询 world_boss_daily 失败：" + rootMessage(e)
                    + "（多为表缺列，启动自愈会补；若关了 worldboss.schema-auto-fix 则需手工执行 sql/world_boss_daily_ensure.sql）";
            log.error("世界BOSS排班查询异常，date=" + date + "，" + lastScheduleFailure, e);
            return null;
        }
    }

    // ================= 基础模板 =================

    @Override
    public List<WorldBossBase> listActiveBossTemplates() {
        return worldBossBaseMapper.selectList(new LambdaQueryWrapper<WorldBossBase>()
                .eq(WorldBossBase::getIsActive, 1)
                .orderByAsc(WorldBossBase::getSort)
                .orderByAsc(WorldBossBase::getId));
    }

    @Override
    public WorldBossBase getBossTemplate(Integer id) {
        if (id == null) {
            return null;
        }
        return worldBossBaseMapper.selectById(id);
    }

    @Override
    public boolean saveBossTemplate(WorldBossBase template) {
        return template != null && worldBossBaseMapper.insert(template) > 0;
    }

    @Override
    public boolean updateBossTemplate(WorldBossBase template) {
        return template != null && template.getId() != null && worldBossBaseMapper.updateById(template) > 0;
    }

    @Override
    public boolean removeBossTemplate(Integer id) {
        return id != null && worldBossBaseMapper.deleteById(id) > 0;
    }

    // ================= 每日排班 =================

    @Override
    public WorldBossDaily getScheduleByDate(LocalDate date) {
        if (date == null) {
            return null;
        }
        return selectDailyRow(date);
    }

    @Override
    public WorldBossDaily getScheduleByDateWithFallback(LocalDate date) {
        if (date == null) {
            return null;
        }
        WorldBossDaily daily = getScheduleByDate(date);
        if (daily != null) {
            return daily;
        }
        // 自恢复：当天无排班（多为进程没在线跨过触发点，或调度线程池被其他任务卡死），
        // 兜底生成一次再查；时间窗传 null 走默认 09:00-23:00；补生成失败也不能影响玩家入口，静默返回 null 由上层给友好文案
        try {
            log.warn("世界BOSS排班自恢复：{} 无排班，兜底调用 prepareDailySchedule 生成", date);
            prepareDailySchedule(date, null, null);
        } catch (Exception e) {
            log.error("世界BOSS排班自恢复失败，date=" + date, e);
        }
        return getScheduleByDate(date);
    }

    /**
     * 往后连着补 days 天：不依赖任何单次 cron 触发，提前把后面的行灌好，
     * 每天满血靠“每天一行新行”天然成立，所以这里只补建、不复位。
     */
    @Override
    public int createFutureSchedules(LocalDate from, int days) {
        if (from == null || days <= 0) {
            return 0;
        }
        int created = 0;
        for (int i = 0; i < days; i++) {
            LocalDate d = from.plusDays(i);
            if (selectDailyRow(d) != null) {
                continue;
            }
            if (insertNewDailyRow(d, DEFAULT_OPEN, DEFAULT_CLOSE) != null) {
                created++;
            }
        }
        return created;
    }

    @Override
    public String dailyScheduleReport(LocalDate date) {
        LocalDate day = date == null ? LocalDate.now() : date;
        StringBuilder sb = new StringBuilder();
        sb.append("date=").append(day);
        try {
            List<WorldBossDaily> list = worldBossDailyMapper.selectList(new LambdaQueryWrapper<WorldBossDaily>()
                    .eq(WorldBossDaily::getBossDate, day)
                    .orderByDesc(WorldBossDaily::getId));
            if (list == null || list.isEmpty()) {
                sb.append(" 当天无排班行");
            } else {
                WorldBossDaily d = list.get(0);
                WorldBossBase boss = d.getBaseBossId() == null ? null : worldBossBaseMapper.selectById(d.getBaseBossId());
                sb.append(" 已有 ").append(list.size()).append(" 行，取最新 id=").append(d.getId())
                        .append(" base_boss_id=").append(d.getBaseBossId())
                        .append("(").append(boss == null ? "模板不存在" : boss.getBossName()).append(")")
                        .append(" is_open=").append(d.getIsOpen())
                        .append(" 时间窗=").append(d.getStartTime()).append("-").append(d.getEndTime())
                        .append(" 三率=").append(d.getHpRate()).append("/").append(d.getAtkRate())
                        .append("/").append(d.getSpeedRate());
            }
        } catch (Exception e) {
            return sb.append(" 查询 world_boss_daily 失败：").append(rootMessage(e))
                    .append("（表结构与实体不一致，启动自愈会补列；已关闭时手工执行 sql/world_boss_daily_ensure.sql）").toString();
        }
        // 模板侧情况：没有可用模板时代码再怎么改也生不成行，这一行就是给运维看的
        try {
            Long activeCount = worldBossBaseMapper.selectCount(new LambdaQueryWrapper<WorldBossBase>()
                    .eq(WorldBossBase::getIsActive, 1));
            Long allCount = worldBossBaseMapper.selectCount(null);
            sb.append("；模板表 world_boss_base：is_active=1 共 ").append(activeCount)
                    .append(" 条，全表共 ").append(allCount).append(" 条");
        } catch (Exception e) {
            sb.append("；模板表 world_boss_base 查询失败：").append(rootMessage(e));
        }
        if (lastScheduleFailure != null) {
            sb.append("；上一次生成失败原因：").append(lastScheduleFailure);
        }
        return sb.toString();
    }

    @Override
    public List<WorldBossDaily> listOpenSchedules(LocalDate date) {
        if (date == null) {
            return Collections.emptyList();
        }
        return worldBossDailyMapper.selectList(new LambdaQueryWrapper<WorldBossDaily>()
                .eq(WorldBossDaily::getBossDate, date)
                .eq(WorldBossDaily::getIsOpen, 1));
    }

    @Override
    public boolean saveSchedule(WorldBossDaily schedule) {
        return schedule != null && worldBossDailyMapper.insert(schedule) > 0;
    }

    @Override
    public boolean updateSchedule(WorldBossDaily schedule) {
        return schedule != null && schedule.getId() != null && worldBossDailyMapper.updateById(schedule) > 0;
    }

    @Override
    public boolean removeSchedule(Integer id) {
        return id != null && worldBossDailyMapper.deleteById(id) > 0;
    }

    // ================= 伤害记录 =================

    @Override
    public WorldBossRecord accumulateDamage(LocalDate bossDate, Long roleId, String roleName, String roleImg, Integer roleLv, String server, long damage, int hitCount) {
        LocalDateTime now = LocalDateTime.now();
        List<WorldBossRecord> exists = worldBossRecordMapper.selectList(new LambdaQueryWrapper<WorldBossRecord>()
                .eq(WorldBossRecord::getBossDate, bossDate)
                .eq(WorldBossRecord::getRoleId, roleId)
                // 多个服共用本库且 roleId 会重号，必须带上服务器名，否则会累加到别服的记录上
                .eq(hasText(server), WorldBossRecord::getServer, server));
        WorldBossRecord record = exists.isEmpty() ? null : exists.get(0);
        if (record == null) {
            record = new WorldBossRecord();
            record.setBossDate(bossDate);
            record.setRoleId(roleId);
            record.setRoleName(roleName);
            record.setImg(roleImg);
            // 等级快照：合并榜单上别服玩家不能回本服主库查 lv（roleId 会串号）
            record.setRoleLv(roleLv);
            record.setServer(server);
            record.setTotalDamage(Math.max(damage, 0L));
            record.setHitCount(Math.max(hitCount, 0));
            record.setBossRank(0);
            record.setIsRewardGet(0);
            record.setLastAtkTime(now);
            worldBossRecordMapper.insert(record);
        } else {
            record.setTotalDamage((record.getTotalDamage() == null ? 0L : record.getTotalDamage()) + Math.max(damage, 0L));
            record.setHitCount((record.getHitCount() == null ? 0 : record.getHitCount()) + Math.max(hitCount, 0));
            // 同步最新角色名/头像/服务器名与攻击时间（头像旧行为空时在此补全）
            if (roleName != null) {
                record.setRoleName(roleName);
            }
            if (hasText(roleImg)) {
                record.setImg(roleImg);
            }
            if (roleLv != null) {
                record.setRoleLv(roleLv);
            }
            if (hasText(server)) {
                record.setServer(server);
            }
            record.setLastAtkTime(now);
            worldBossRecordMapper.updateById(record);
        }
        return record;
    }

    @Override
    public List<WorldBossRecord> rankList(LocalDate bossDate, int limit, String server) {
        if (bossDate == null || limit <= 0) {
            return Collections.emptyList();
        }
        return worldBossRecordMapper.selectList(new LambdaQueryWrapper<WorldBossRecord>()
                .eq(WorldBossRecord::getBossDate, bossDate)
                .eq(hasText(server), WorldBossRecord::getServer, server)
                .orderByDesc(WorldBossRecord::getTotalDamage)
                .orderByAsc(WorldBossRecord::getLastAtkTime)
                .last("limit " + limit));
    }

    @Override
    public List<WorldBossRecord> listByMinDamage(LocalDate bossDate, String server, long minDamage) {
        if (bossDate == null) {
            return Collections.emptyList();
        }
        return worldBossRecordMapper.selectList(new LambdaQueryWrapper<WorldBossRecord>()
                .eq(WorldBossRecord::getBossDate, bossDate)
                .eq(hasText(server), WorldBossRecord::getServer, server)
                .ge(WorldBossRecord::getTotalDamage, minDamage)
                .orderByDesc(WorldBossRecord::getTotalDamage));
    }

    @Override
    public WorldBossRecord findRoleRecord(LocalDate bossDate, Long roleId, String server) {
        if (bossDate == null || roleId == null) {
            return null;
        }
        List<WorldBossRecord> list = worldBossRecordMapper.selectList(new LambdaQueryWrapper<WorldBossRecord>()
                .eq(WorldBossRecord::getBossDate, bossDate)
                .eq(WorldBossRecord::getRoleId, roleId)
                // 多服共库，而 userId 在各服会重复：不带 server 就可能把别服同 id 玩家的记录当成“我的”，
                // 取本服口径请传 worldboss.server-name（传 null 则不过滤，仅运维排查时用）
                .eq(hasText(server), WorldBossRecord::getServer, server)
                .orderByDesc(WorldBossRecord::getTotalDamage)
                .last("limit 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public int assignRanks(LocalDate bossDate, int rankScope, String server) {
        List<WorldBossRecord> list = rankList(bossDate, rankScope, server);
        int rank = 1;
        for (WorldBossRecord r : list) {
            r.setBossRank(rank++);
            worldBossRecordMapper.updateById(r);
        }
        return list.size();
    }

    @Override
    public boolean markRewardGot(Long id) {
        if (id == null) {
            return false;
        }
        WorldBossRecord record = worldBossRecordMapper.selectById(id);
        if (record == null) {
            return false;
        }
        record.setIsRewardGet(1);
        return worldBossRecordMapper.updateById(record) > 0;
    }

    @Override
    public boolean resetDailyBossStats(LocalDate date) {
        if (date == null) {
            return false;
        }
        WorldBossDaily daily = selectDailyRow(date);
        if (daily == null) {
            log.warn("世界BOSS每日重置：{} 无排班记录，跳过", date);
            return false;
        }
        // 血量/攻击/速度倍率复位为满值(1.00)，即回到 base 基准满值
        daily.setHpRate(FULL_RATE);
        daily.setAtkRate(FULL_RATE);
        daily.setSpeedRate(FULL_RATE);
        daily.setUpdateTime(LocalDateTime.now());
        worldBossDailyMapper.updateById(daily);
        log.info("世界BOSS每日重置完成：date={} hp_rate/atk_rate/speed_rate 复位为 1.00", date);
        return true;
    }

    @Override
    public boolean touchDailyScheduleOnChallenge(LocalDate date) {
        if (date == null) {
            return false;
        }
        WorldBossDaily daily = selectDailyRow(date);
        if (daily == null) {
            return false;
        }
        // 每次挑战刷新当天排班的 update_time（记录最近一次挑战）；若需按伤害扣减血量待接入伤害后扩展
        daily.setUpdateTime(LocalDateTime.now());
        worldBossDailyMapper.updateById(daily);
        return true;
    }

    @Override
    public boolean writeDailyRates(LocalDate date, BigDecimal hpRate, BigDecimal atkRate, BigDecimal speedRate) {
        if (date == null || hpRate == null || atkRate == null || speedRate == null) {
            return false;
        }
        WorldBossDaily daily = selectDailyRow(date);
        if (daily == null) {
            log.warn("世界BOSS倍率回写：{} 无排班记录，跳过", date);
            return false;
        }
        daily.setHpRate(hpRate);
        daily.setAtkRate(atkRate);
        daily.setSpeedRate(speedRate);
        daily.setUpdateTime(LocalDateTime.now());
        worldBossDailyMapper.updateById(daily);
        log.info("世界BOSS每日倍率已更新：date={}, id={}, hpRate={}, atkRate={}, speedRate={}", date, daily.getId(), hpRate, atkRate, speedRate);
        return true;
    }

    @Override
    public WorldBossDaily prepareDailySchedule(LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (date == null) {
            return null;
        }
        LocalTime open = startTime != null ? startTime : DEFAULT_OPEN;
        LocalTime close = endTime != null ? endTime : DEFAULT_CLOSE;
        WorldBossDaily daily = selectDailyRow(date);
        if (daily != null) {
            return resetDailyRow(daily, open, close);
        }
        return insertNewDailyRow(date, open, close);
    }

    /**
     * 新增当天排班行。成功返回新行（并发下别的实例刚插的那行也算成功），失败返回 null 并把真实原因写进 lastScheduleFailure。
     * <p>以前这一段把插入异常笼统归为“多为另一实例已抢先写入”，而真正的常见原因（表缺 speed_rate 列、
     * world_boss_base 没一行 is_active=1）全被这句误导话掩盖，导致“排班不新增”查了好几轮都定不了位。
     */
    private WorldBossDaily insertNewDailyRow(LocalDate date, LocalTime open, LocalTime close) {
        lastScheduleFailure = null;
        List<WorldBossBase> actives;
        try {
            actives = pickScheduleTemplates();
        } catch (Exception e) {
            lastScheduleFailure = "查询 world_boss_base 模板失败：" + rootMessage(e);
            log.error("世界BOSS每日排班：查模板异常，date=" + date + "，" + lastScheduleFailure, e);
            return null;
        }
        if (actives == null || actives.isEmpty()) {
            lastScheduleFailure = "world_boss_base 一条模板都没有（含 is_active 不为 1 的也没有），排班无法生成；请先灌 BOSS 模板数据";
            log.error("世界BOSS每日排班：{}，date={}", lastScheduleFailure, date);
            return null;
        }
        int idx = (int) Math.floorMod(date.toEpochDay(), actives.size());
        WorldBossBase boss = actives.get(idx);
        WorldBossDaily nd = new WorldBossDaily();
        nd.setBossDate(date);
        nd.setBaseBossId(boss.getId());
        nd.setStartTime(open);
        nd.setEndTime(close);
        nd.setHpRate(FULL_RATE);
        nd.setAtkRate(FULL_RATE);
        nd.setSpeedRate(FULL_RATE);
        nd.setIsOpen(1);
        nd.setCreateTime(LocalDateTime.now());
        nd.setUpdateTime(LocalDateTime.now());
        try {
            worldBossDailyMapper.insert(nd);
        } catch (Exception e) {
            String insertMsg = rootMessage(e);
            WorldBossDaily exist = selectDailyRow(date);
            if (exist != null) {
                // 两个实例同刻跑守护任务时，唯一索引会挡下后到的那一次：这不是错，直接用已有行
                log.info("世界BOSS每日排班：本次插入被挡（{}），回查到另一实例已写入的 id={}，改用该行", insertMsg, exist.getId());
                return exist;
            }
            lastScheduleFailure = "插入 world_boss_daily 失败：" + insertMsg
                    + (lastScheduleFailure == null ? "" : "；且" + lastScheduleFailure)
                    + "；若是 Unknown column 就是实体比生产表多列，启动自愈会补列（或手工执行 sql/world_boss_daily_ensure.sql）";
            log.error("世界BOSS每日排班新增失败，date=" + date + "，" + lastScheduleFailure, e);
            return null;
        }
        log.info("世界BOSS每日排班新增：date={} base_boss_id={}({}) 时间窗={}-{}", date, boss.getId(), boss.getBossName(), open, close);
        return nd;
    }

    /**
     * 排班用哪些模板：先取 is_active=1 的，取不到再退回全表。
     * <p>world_boss_base 的 is_active 列是后来 ALTER 加的，线上老数据要么整列 NULL 要么全 0；
     * 以前只认 is_active=1，一旦如此就“永远生不成排班行”，而玩家那边只看到世界BOSS 点不开。
     * 这里退化成“全部模板也能用”，同时把该补的数据在日志里说清楚。
     */
    private List<WorldBossBase> pickScheduleTemplates() {
        List<WorldBossBase> actives = null;
        try {
            actives = listActiveBossTemplates();
        } catch (Exception e) {
            log.warn("世界BOSS排班：按 is_active=1 查模板失败（多为该列不存在），改查全部模板。原因={}", rootMessage(e));
        }
        if (actives != null && !actives.isEmpty()) {
            return actives;
        }
        List<WorldBossBase> all = worldBossBaseMapper.selectList(new LambdaQueryWrapper<WorldBossBase>()
                .orderByAsc(WorldBossBase::getSort)
                .orderByAsc(WorldBossBase::getId));
        if (all != null && !all.isEmpty()) {
            log.warn("世界BOSS排班：world_boss_base 没有 is_active=1 的模板，本次按全表 {} 条模板轮换；"
                    + "启动自愈会把 NULL 的 is_active 补成 1，整表都是 0 时需人工放开", all.size());
        }
        return all;
    }

    /** 已有当天排班行时复位：三率回满值、刷新时间窗并重新开启 */
    private WorldBossDaily resetDailyRow(WorldBossDaily daily, LocalTime open, LocalTime close) {
        daily.setHpRate(FULL_RATE);
        daily.setAtkRate(FULL_RATE);
        daily.setSpeedRate(FULL_RATE);
        daily.setStartTime(open);
        daily.setEndTime(close);
        daily.setIsOpen(1);
        daily.setUpdateTime(LocalDateTime.now());
        worldBossDailyMapper.updateById(daily);
        log.info("世界BOSS每日排班复位：date={} 三率=1.00 时间窗={}-{}", daily.getBossDate(), open, close);
        return daily;
    }
}
