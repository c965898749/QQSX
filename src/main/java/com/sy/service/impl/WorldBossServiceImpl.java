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
        // boss_date 唯一，最多一条
        return worldBossDailyMapper.selectOne(new LambdaQueryWrapper<WorldBossDaily>()
                .eq(WorldBossDaily::getBossDate, date));
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
    public WorldBossRecord accumulateDamage(LocalDate bossDate, Long roleId, String roleName, long damage, int hitCount) {
        LocalDateTime now = LocalDateTime.now();
        List<WorldBossRecord> exists = worldBossRecordMapper.selectList(new LambdaQueryWrapper<WorldBossRecord>()
                .eq(WorldBossRecord::getBossDate, bossDate)
                .eq(WorldBossRecord::getRoleId, roleId));
        WorldBossRecord record = exists.isEmpty() ? null : exists.get(0);
        if (record == null) {
            record = new WorldBossRecord();
            record.setBossDate(bossDate);
            record.setRoleId(roleId);
            record.setRoleName(roleName);
            record.setTotalDamage(Math.max(damage, 0L));
            record.setHitCount(Math.max(hitCount, 0));
            record.setBossRank(0);
            record.setIsRewardGet(0);
            record.setLastAtkTime(now);
            worldBossRecordMapper.insert(record);
        } else {
            record.setTotalDamage((record.getTotalDamage() == null ? 0L : record.getTotalDamage()) + Math.max(damage, 0L));
            record.setHitCount((record.getHitCount() == null ? 0 : record.getHitCount()) + Math.max(hitCount, 0));
            // 同步最新角色名与攻击时间
            if (roleName != null) {
                record.setRoleName(roleName);
            }
            record.setLastAtkTime(now);
            worldBossRecordMapper.updateById(record);
        }
        return record;
    }

    @Override
    public List<WorldBossRecord> rankList(LocalDate bossDate, int limit) {
        if (bossDate == null || limit <= 0) {
            return Collections.emptyList();
        }
        return worldBossRecordMapper.selectList(new LambdaQueryWrapper<WorldBossRecord>()
                .eq(WorldBossRecord::getBossDate, bossDate)
                .orderByDesc(WorldBossRecord::getTotalDamage)
                .orderByAsc(WorldBossRecord::getLastAtkTime)
                .last("limit " + limit));
    }

    @Override
    public WorldBossRecord findRoleRecord(LocalDate bossDate, Long roleId) {
        if (bossDate == null || roleId == null) {
            return null;
        }
        List<WorldBossRecord> list = worldBossRecordMapper.selectList(new LambdaQueryWrapper<WorldBossRecord>()
                .eq(WorldBossRecord::getBossDate, bossDate)
                .eq(WorldBossRecord::getRoleId, roleId)
                .orderByDesc(WorldBossRecord::getTotalDamage)
                .last("limit 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public int assignRanks(LocalDate bossDate, int rankScope) {
        List<WorldBossRecord> list = rankList(bossDate, rankScope);
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
        WorldBossDaily daily = worldBossDailyMapper.selectOne(new LambdaQueryWrapper<WorldBossDaily>()
                .eq(WorldBossDaily::getBossDate, date));
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
        WorldBossDaily daily = worldBossDailyMapper.selectOne(new LambdaQueryWrapper<WorldBossDaily>()
                .eq(WorldBossDaily::getBossDate, date));
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
        WorldBossDaily daily = worldBossDailyMapper.selectOne(new LambdaQueryWrapper<WorldBossDaily>()
                .eq(WorldBossDaily::getBossDate, date));
        if (daily == null) {
            log.warn("世界BOSS倍率回写：{} 无排班记录，跳过", date);
            return false;
        }
        daily.setHpRate(hpRate);
        daily.setAtkRate(atkRate);
        daily.setSpeedRate(speedRate);
        daily.setUpdateTime(LocalDateTime.now());
        worldBossDailyMapper.updateById(daily);
        return true;
    }
}
