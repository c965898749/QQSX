package com.sy.model.game;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 世界BOSS各服身份（只存服务名与 IP，其他什么都不放）。
 * <p>进程启动时拿自己网卡的内网 IPv4 去 {@code server_ip} 上对号入座，命中哪行就代表“我是哪个服”，
 * 从此以后配置文件里不用再写 worldboss.server-name（中文名还得写 Unicode 转义）。
 */
@Data
@TableName("world_boss_server")
public class WorldBossServer {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /** 服务器名：要和前端 serverList 里的名字完全一致，榜单与公告都直接展示它 */
    @TableField("server_name")
    private String serverName;

    /** 这台机器自己的内网 IP，进程用它认身份 */
    @TableField("server_ip")
    private String serverIp;
}
