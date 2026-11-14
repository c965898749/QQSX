package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 世界BOSS跨服节点子表（其他服清单，只存服务名与 IP）。
 * <p>以后加新服就 INSERT 一行，两台老机器不用改任何东西也立刻能跨服查看新服玩家的战队；
 * 表里把自己那行也留着没关系，代码会按本机 IP 把自己跳过。
 */
@Data
@TableName("world_boss_server_peer")
public class WorldBossServerPeer {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 对端服务器名，必须与对端在 world_boss_server 里的那行同名（榜单就是按名字认服的） */
    @TableField("server_name")
    private String serverName;

    /** 对端内网 IP：跨服调用必须后端直连对端 8889，不能走 czx.yimem.com 那种公网入口 */
    @TableField("server_ip")
    private String serverIp;
}
