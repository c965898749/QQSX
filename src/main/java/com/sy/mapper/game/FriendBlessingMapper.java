package com.sy.mapper.game;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sy.model.game.FriendBlessing;
import com.sy.model.game.FriendRelation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface FriendBlessingMapper extends BaseMapper<FriendBlessing> {
    // 统计今日收到祝福总数（普通查询）
    long countTodayReceive(@Param("receiverId") Integer receiverId, @Param("today") LocalDate today);

    // 统计今日送出祝福总数 悲观锁FOR UPDATE，锁住当天该用户所有祝福记录，串行执行
    List<FriendBlessing> listTodaySendLock(@Param("senderId") Integer senderId, @Param("today") LocalDate today);
}