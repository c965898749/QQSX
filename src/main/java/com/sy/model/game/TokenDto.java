package com.sy.model.game;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TokenDto {
    private String token;
    private String userId;
    private String id;
    private String str;
    private int finalLevel;       // 最终等级
    private int remainingExp;     // 当前等级的剩余经验
    private int totalSilverSpent; // 升级消耗的总银两
    private String difficultyLevel;
    /** 目标玩家所在服务器名：世界BOSS合并榜单下发，不是本服时 otherTeamInfo 会转发到对端实例；其他入口不传 */
    private String server;
    private List<List<Object>> myMap; // 接收前端的str（JSON对象 → Java Map）
}
