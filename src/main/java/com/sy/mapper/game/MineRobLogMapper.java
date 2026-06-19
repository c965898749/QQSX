package com.sy.mapper.game;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sy.model.game.MineRobLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface MineRobLogMapper extends BaseMapper<MineRobLog> {
    List<MineRobLog> selectAll(@Param("userId") String userId);
}