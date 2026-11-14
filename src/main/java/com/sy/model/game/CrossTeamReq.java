package com.sy.model.game;

import lombok.Data;

/**
 * 跨服「查看对方战队」内部接口的请求体（实例之间调用，不经过客户端）。
 * <p>对端收到后按自己库里的数据组装并原样返回，字段口径与 otherTeamInfo 完全一致。
 */
@Data
public class CrossTeamReq {

    /** 内部接口口令，两边配置必须一致（crossserver.secret） */
    private String secret;

    /** 要查看的玩家 userId（在发起方那边记录的 role_id，对对端而言就是它自己库里的 userId） */
    private Integer targetUserId;

    /** 擂台级别：>0 时按该级擂台本周报名阵容返回，世界BOSS榜用不到，传 0/不传即常规战队 */
    private Integer arenaLevel;

    /** 发起方服务器名，只用于日志排障 */
    private String fromServer;

    /** 发起方玩家 userId，擂台口径下用来取「我方」阵容做战力对比 */
    private String requesterUserId;
}
