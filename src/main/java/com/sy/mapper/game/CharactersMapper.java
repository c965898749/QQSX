package com.sy.mapper.game;

import com.sy.model.game.Characters;
import com.sy.model.game.TokenDto;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CharactersMapper {
    int deleteByPrimaryKey(Integer uuid);

    int insert(Characters record);

    int insert2(Characters record);

    int insertSelective(Characters record);

    Characters selectByPrimaryKey(Integer uuid);

    int updateByPrimaryKeySelective(Characters record);

    int updateByPrimaryKey(Characters record);

    List<Characters> selectAllCardList();

    /**
     * 查询所有未删除的角色记录
     * @return is_delete='0' 的角色列表
     */
    List<Characters> selectActiveCardList();

    List<Characters> selectByUserId(Integer userId);

    int updateGoNuM(String userId);

    int updateDelte(Integer uuid);

    int updateGoNuM2(@Param("num") Integer num,@Param("id") String id,@Param("userId") String userId);

    Characters listById(@Param("userId") String userId,@Param("id") String id);

    List<Characters> goIntoListById(@Param("userId") String userId);

    List<Characters> goIntoListByIds(@Param("userIds") String userIds);

    // 删除is_delete=1的角色记录
    int deleteByIsDelete();

    /**
     * 修复异常数据：处理同一userId和id下的重复数据，保留最优的一条
     * 比较规则：stackCount、lv、flyup，如果一个记录的这些字段都大于等于另一条，则另一条标记为删除
     * 同时将lv=0的记录更新为1
     * @return 更新的行数
     */
    int fixAbnormalCharacters();
}