package com.sy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 跨服内部调用配置。
 * <p>背景：世界BOSS伤害榜已改成多服合并，榜上会出现别服玩家。各实例的主库彼此独立
 * 且 userId 会跨服重号，本服库里那个 id 是另一个人，所以「查看对方战队」必须把请求
 * 转给玩家所在服的实例，用它自己库里的数据回答。
 * <p>只在 application.properties 的 crossserver.* 下配置，两套部署各自填对端地址。
 */
@Data
@Component
@ConfigurationProperties(prefix = "crossserver")
public class CrossServerProperties {

    /** 内部接口口令：两套部署必须填同一个值，留空则跨服查看整体关闭（收、发两端都不放行） */
    private String secret = "";

    /**
     * 对端节点表：服务器名=基础URL，多个用分号分隔。
     * <p>服务器名必须和那个实例的 worldboss.server-name 完全一致（榜单就是按它认服的）；
     * 只列「别人的服」，列了自己的那行也不会影响（本服判断优先走 worldboss.server-name）。
     */
    private String peers = "";

    /** 连接超时（毫秒）：对端不通时别把玩家的点击卡成转圈 */
    private int connectTimeout = 2000;

    /** 读超时（毫秒） */
    private int readTimeout = 3000;

    /** 解析 peers（节点数量个位数，每次点击解析一次即可，不必缓存） */
    public Map<String, String> peerMap() {
        Map<String, String> map = new LinkedHashMap<>();
        if (peers == null || peers.trim().isEmpty()) {
            return map;
        }
        for (String item : peers.split(";")) {
            if (item == null || item.trim().isEmpty()) {
                continue;
            }
            int idx = item.indexOf('=');
            if (idx <= 0 || idx == item.length() - 1) {
                continue;
            }
            String name = item.substring(0, idx).trim();
            String url = item.substring(idx + 1).trim();
            if (!name.isEmpty() && !url.isEmpty()) {
                map.put(name, url);
            }
        }
        return map;
    }

    /** 取某个服务器名的基础访问地址，未配置返回 null */
    public String peerUrl(String serverName) {
        if (serverName == null || serverName.trim().isEmpty()) {
            return null;
        }
        return peerMap().get(serverName.trim());
    }

    /** 跨服调用是否已启用（口令没配就等于关闭，避免裸奔的内部接口） */
    public boolean enabled() {
        return secret != null && !secret.trim().isEmpty();
    }

    /**
     * 校验内部接口口令：未启用时一律拒绝；两边都 trim，防止 properties 里手滑多打空格
     */
    public boolean matchSecret(String got) {
        if (!enabled()) {
            return false;
        }
        if (got == null) {
            return false;
        }
        // 长度不等先返回可以省掉逐字节比较，内容比较仍走常量时间，避免计时侧信道
        byte[] expect = secret.trim().getBytes();
        byte[] actual = got.getBytes();
        if (expect.length != actual.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < expect.length; i++) {
            diff |= expect[i] ^ actual[i];
        }
        return diff == 0;
    }
}
