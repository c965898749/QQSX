package com.sy.mapper.game;

import com.sy.model.game.PveDetail;

import java.util.List;

public interface PveDetailMapper {
    int insert(PveDetail record);

    int insertSelective(PveDetail record);

    PveDetail selectById(String id);

    List<PveDetail> selectAll();
}