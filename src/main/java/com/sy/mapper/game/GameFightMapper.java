package com.sy.mapper.game;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sy.model.game.GameFight;
import com.sy.model.game.LivelyGift;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
@Mapper
public interface GameFightMapper extends BaseMapper<GameFight> {
    List<GameFight> getGameFightList(String userId);
}