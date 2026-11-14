package com.sy.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sy.config.CrossServerProperties;
import com.sy.model.resp.BaseResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 跨服内部接口客户端：本服实例去对端实例要数据（目前只有「查看对方战队」）。
 * <p>为什么在服务端转发而不是让前端直连对端：前端的 token 是各自服的登录态，
 * 拿本服的 token 去请求别服接口在鉴权上不成立；由后端用内部口令转发，
 * 客户端只面对自己那一个服务器地址，也不用考虑跨域和对端地址暴露给玩家。
 * <p>对端地址与本服名都取自 {@link WorldBossNodeRegistry}（即 world_boss_server / world_boss_server_peer 两张表），
 * 只有口令还留在 crossserver.secret：表里只存服务名与 IP。
 */
@Slf4j
@Component
public class CrossServerClient {

    @Autowired
    private CrossServerProperties crossServerProperties;

    @Autowired
    private ObjectMapper objectMapper;

    /** 节点表预加载结果：本服名与对端地址都从这里拿（表读不到时它会自己回退到 properties） */
    @Autowired
    private WorldBossNodeRegistry worldBossNodeRegistry;

    private volatile RestTemplate restTemplate;

    /**
     * 启动完成后把跨服配置的真实生效情况打一行日志：配错（光填了口令没填地址、或者两者反了）
     * 在玩家点开后没有任何外在症状，只能到榜上点别人才发现，事后查成本很高。
     * <p>放在启动完成而不是 Bean 初始化：那一步要读世界BOSS库，不能让节点表把进程启动卡住。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void logCrossServerConfig() {
        if (!crossServerProperties.enabled()) {
            log.info("跨服查看战队未启用（crossserver.secret 为空）：世界BOSS榜上别服玩家点开头像会提示暂未开放");
            return;
        }
        Map<String, String> peers = worldBossNodeRegistry.peerMap();
        if (peers.isEmpty()) {
            log.warn("跨服查看战队已设口令但没有任何对端节点（world_boss_server_peer 与 crossserver.peers 都是空的）：别服玩家的查看请求会全部被拒");
            return;
        }
        log.info("跨服查看战队已启用，{}，超时={}/{}ms",
                worldBossNodeRegistry.describe(),
                crossServerProperties.getConnectTimeout(), crossServerProperties.getReadTimeout());
    }

    /**
     * 去对端实例查某个玩家的战队信息。
     * <p>任何失败都只返回 success=0 + 一句能显示给玩家的话，不抛异常：
     * 榜上跨服玩家看不到阵容是可接受的降级，报「服务器异常」不是。
     *
     * @param server       目标玩家所在服务器名（世界BOSS伤害记录里的 server 快照）
     * @param targetUserId 目标玩家 userId（对端库里的 id）
     * @param arenaLevel   擂台级别，0/null 表示常规战队
     * @param playerUserId 发起查询的玩家 userId
     */
    public BaseResp otherTeamInfo(String server, Integer targetUserId, Integer arenaLevel, String playerUserId) {
        BaseResp baseResp = new BaseResp();
        baseResp.setSuccess(0);
        if (targetUserId == null) {
            baseResp.setErrorMsg("参数错误");
            return baseResp;
        }
        if (!crossServerProperties.enabled()) {
            baseResp.setErrorMsg("跨服查看暂未开放，请稍后再试");
            log.warn("跨服查看战队被请求但未配置 crossserver.secret，server={} targetUserId={}", server, targetUserId);
            return baseResp;
        }
        String peerUrl = worldBossNodeRegistry.peerUrl(server);
        if (peerUrl == null) {
            baseResp.setErrorMsg("该玩家所在服务器暂不支持跨服查看");
            log.warn("跨服查看战队：节点表里没有服务器「{}」，请往 world_boss_server_peer 加一行（服务名+内网IP）", server);
            return baseResp;
        }
        String url = apiUrl(peerUrl);
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("secret", crossServerProperties.getSecret().trim());
            payload.put("targetUserId", targetUserId);
            payload.put("arenaLevel", arenaLevel == null ? 0 : arenaLevel);
            payload.put("fromServer", worldBossNodeRegistry.serverName());
            payload.put("requesterUserId", playerUserId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(payload), headers);
            ResponseEntity<String> response = restTemplate().postForEntity(url, entity, String.class);
            String body = response.getBody();
            if (!response.getStatusCode().is2xxSuccessful() || body == null || body.trim().isEmpty()) {
                baseResp.setErrorMsg("对方服务器暂时连不上，请稍后再试");
                log.warn("跨服查看战队返回异常，url={} status={} body={}", url, response.getStatusCode(), body);
                return baseResp;
            }
            Map<?, ?> parsed = objectMapper.readValue(body, Map.class);
            Object success = parsed.get("success");
            if (success == null || !"1".equals(String.valueOf(success))) {
                Object errorMsg = parsed.get("errorMsg");
                baseResp.setErrorMsg(errorMsg == null || String.valueOf(errorMsg).trim().isEmpty()
                        ? "对方服务器暂时连不上，请稍后再试" : String.valueOf(errorMsg));
                log.warn("跨服查看战队被对端拒绝，url={} body={}", url, body);
                return baseResp;
            }
            // data 原样透传：字段结构与本服 otherTeamInfo 完全一致，前端渲染逻辑不需要区分跨服
            baseResp.setSuccess(1);
            baseResp.setData(parsed.get("data"));
            return baseResp;
        } catch (Exception e) {
            baseResp.setErrorMsg("对方服务器暂时连不上，请稍后再试");
            log.warn("跨服查看战队调用失败，url=" + url + " targetUserId=" + targetUserId + "：" + e.getMessage());
            return baseResp;
        }
    }

    /** 基础地址 + 内部接口路径，兼容配置里带不带结尾斜杠 */
    private String apiUrl(String peerUrl) {
        String base = peerUrl.trim();
        if (!base.endsWith("/")) {
            base = base + "/";
        }
        return base + "cross/otherTeamInfo";
    }

    /** 带超时的 RestTemplate，只在本类内部用，不注册成全局 Bean 免得影响别处 */
    private RestTemplate restTemplate() {
        RestTemplate template = restTemplate;
        if (template == null) {
            synchronized (this) {
                template = restTemplate;
                if (template == null) {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(crossServerProperties.getConnectTimeout());
                    factory.setReadTimeout(crossServerProperties.getReadTimeout());
                    template = new RestTemplate(factory);
                    restTemplate = template;
                }
            }
        }
        return template;
    }
}
