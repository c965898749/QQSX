package com.sy.tool;

import com.sy.config.CrossServerProperties;
import com.sy.model.game.WorldBossServer;
import com.sy.model.game.WorldBossServerPeer;
import com.sy.worldboss.mapper.WorldBossServerMapper;
import com.sy.worldboss.mapper.WorldBossServerPeerMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 世界BOSS各服节点信息的预加载与查询（数据在共用库的 world_boss_server / world_boss_server_peer 两张表里，都只有服务名与 IP）。
 * <p>为什么搬到表里：以前「我是哪个服」「对端在哪」写在 application.properties，两套部署要各自注释/反注释对应行，
 * 中文服务名还必须写成 Unicode 转义，改一个服经常要动两个文件还要重新打包。现在表里改一行就行，
 * 加新服也只是往子表 INSERT 一行。
 * <p>本进程怎么知道自己是哪个服：枚举本机网卡的内网 IPv4，去 world_boss_server.server_ip 上对号入座，命中哪行就是哪个服；
 * 多网卡想固定，配 {@code worldboss.self-ip} 直接指定。
 * <p>读不到表/认不出自己时一律回退到原来的 properties（{@code worldboss.server-name} 与 {@code crossserver.peers}），
 * 保证老部署换了包也不会瞎；表读得通就以表为准。
 */
@Slf4j
@Component
public class WorldBossNodeRegistry {

    @Autowired
    private WorldBossServerMapper worldBossServerMapper;

    @Autowired
    private WorldBossServerPeerMapper worldBossServerPeerMapper;

    /** 兜底对端表与超时：表里没配到时才用 */
    @Autowired
    private CrossServerProperties crossServerProperties;

    /** 兜底本服名：表里认不出自己时用（老配置项，可为空） */
    @Value("${worldboss.server-name:}")
    private String fallbackServerName;

    /** 固定本机 IP（多网卡又不想被自动匹配时用），空则自动枚举 */
    @Value("${worldboss.self-ip:}")
    private String selfIpOverride;

    /** 对端 Java 进程端口：表里只存 IP，端口统一取这个（两台机器后端都监听 8889） */
    @Value("${worldboss.peer-port:8889}")
    private int peerPort;

    /** 快照存活秒数：改完表最多等这么久就生效，不用重启 */
    @Value("${worldboss.node-reload-seconds:60}")
    private int reloadSeconds;

    /** 当前快照：整体替换，读侧不加锁 */
    private volatile Snapshot snapshot = new Snapshot();

    /** 快照内容：本服身份 + 对端节点表 + 一点排障用的上下文 */
    private static class Snapshot {
        private String selfName = "";
        private String selfIp = "";
        /** 本服名是怎么来的：表命中 / 指定 IP / 回退 properties / 没配 */
        private String selfFrom = "未加载";
        /** 服务器名 -> 形如 http://10.126.126.4:8889/ 的基础地址（已排除本服自己） */
        private Map<String, String> peers = Collections.emptyMap();
        /** 对端表来源：子表 / 主表 / 回退 properties */
        private String peerFrom = "未加载";
        private List<String> localIps = Collections.emptyList();
        /** world_boss_server 的行数：用来区分“单服没配名字（合法）”与“多服但认不出自己（不能发奖）” */
        private int serverRowCount;
        private LocalDateTime loadedAt;
        private String error = "";
    }

    /**
     * 启动就预加载一次，并把结论打进日志。
     * <p>与排班启动自检的先后顺序不保证：万一这次表还不存在（healer 还没建），这里只是回退到 properties，
     * healer 建完表后 {@code WorldBossScheduleTask} 会再调一次 {@link #reload()}，之后每 {@code reloadSeconds} 秒也会自己重读。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void preloadOnStartup() {
        try {
            log.info("世界BOSS节点预加载：{}", reload());
        } catch (Exception e) {
            // 节点表读不通不该影响进程启动，其它玩法照跑
            log.warn("世界BOSS节点预加载异常：{}", e.getMessage());
        }
    }

    /** 本服服务器名（世界BOSS记录、榜单展示、结算归属都用它）；表认不出自己时回退 properties，可能为空串 */
    public String serverName() {
        return current().selfName;
    }

    /** 取某服跨服调用的基础地址，形如 http://10.126.126.4:8889/；不知道这个服返回 null */
    public String peerUrl(String serverName) {
        if (serverName == null || serverName.trim().isEmpty()) {
            return null;
        }
        String url = current().peers.get(serverName.trim());
        if (url != null) {
            return url;
        }
        // 表刚改过、快照还没过期时不至于查不到：直接重读一次再试
        synchronized (this) {
            reloadLocked();
        }
        return current().peers.get(serverName.trim());
    }

    /** 对端节点表（不含本服），给日志与运维接口看 */
    public Map<String, String> peerMap() {
        return current().peers;
    }

    /** world_boss_server 里有几行（就是当前配了几个服） */
    public int serverRowCount() {
        return current().serverRowCount;
    }

    /**
     * 是不是“多服部署但认不出自己”：表里有两个以上的服，本机 IP 却没匹配到任何一行，且连兜底配置名也没有。
     * <p>这种情况本服名是空串，而空串在结算口径里等于“不分服”，会把共库里别的服的记录也一起发码（错发、且不可补），
     * 所以发奖前必须拿它挡一下：宁可今天不发，也不能发给错的人。
     */
    public boolean selfUnknown() {
        Snapshot s = current();
        return s.selfName.isEmpty() && s.serverRowCount > 1;
    }

    /** 跨服内部调用是否可用：口令仍来自 crossserver.secret（表里只存服务名与 IP），但节点地址以表为准 */
    public boolean crossEnabled() {
        return crossServerProperties.enabled() && !current().peers.isEmpty();
    }

    /** 一行说清当前状态，日志和运维接口都返回它，省得再猜“到底读的是表还是配置文件” */
    public String describe() {
        return describe0(current());
    }

    /** 强制重读两张表，返回一行结论（运维接口用） */
    public String reload() {
        synchronized (this) {
            reloadLocked();
        }
        return describe0(snapshot);
    }

    /** 过期就重读，未过期直接给快照 */
    private Snapshot current() {
        Snapshot s = snapshot;
        if (s.loadedAt == null || expired(s)) {
            synchronized (this) {
                if (snapshot.loadedAt == null || expired(snapshot)) {
                    reloadLocked();
                }
                s = snapshot;
            }
        }
        return s;
    }

    private boolean expired(Snapshot s) {
        return s.loadedAt.plusSeconds(Math.max(5, reloadSeconds)).isBefore(LocalDateTime.now());
    }

    /** 真正读表：任何异常都只记进快照的「注意」里，绝不往外抛 */
    private void reloadLocked() {
        Snapshot s = new Snapshot();
        s.loadedAt = LocalDateTime.now();
        try {
            s.localIps = localSiteIpv4();
            List<WorldBossServer> servers = worldBossServerMapper.selectList(null);
            s.serverRowCount = servers == null ? 0 : servers.size();
            String selfName = "";
            String selfIp = "";
            String selfFrom = "表里没有可用行";
            List<String> hitNames = new ArrayList<>();
            for (WorldBossServer row : servers == null ? Collections.<WorldBossServer>emptyList() : servers) {
                String ip = trim(row.getServerIp());
                if (ip.isEmpty() || trim(row.getServerName()).isEmpty()) {
                    continue;
                }
                if (matchesSelf(ip, s.localIps)) {
                    hitNames.add(row.getServerName() + "=" + ip);
                    if (selfName.isEmpty()) {
                        selfName = trim(row.getServerName());
                        selfIp = ip;
                    }
                }
            }
            if (!selfName.isEmpty()) {
                selfFrom = selfIpOverride != null && !selfIpOverride.trim().isEmpty()
                        ? "worldboss.self-ip 指定 " + selfIp.trim() : "本机IP命中 world_boss_server";
                if (hitNames.size() > 1) {
                    s.error = "本机IP命中了多行（" + String.join("、", hitNames) + "），取 id 最小的一行，请核对表内容";
                }
            } else if (!trim(fallbackServerName).isEmpty()) {
                // 认不出自己时用老配置兜底：宁可顶着原来的名字继续跑，也别把记录与榜单的 server 字段变成空
                selfName = trim(fallbackServerName);
                selfFrom = "回退 worldboss.server-name（表里没匹配到本机IP）";
                s.error = "world_boss_server 里没有本机IP的行，本服身份来自配置文件；本机IP=" + s.localIps;
            } else {
                s.error = "world_boss_server 没有匹配本机IP的行且 worldboss.server-name 为空：世界BOSS记录不会带服务器名";
            }
            s.selfName = selfName;
            s.selfIp = selfIp;
            s.selfFrom = selfFrom;

            // 对端：子表优先（可以无限扩充）；子表空则退回主表里除自己以外的行；再空才用 properties
            Map<String, String> peers = new LinkedHashMap<>();
            String peerFrom = "world_boss_server_peer";
            List<WorldBossServerPeer> rows = worldBossServerPeerMapper.selectList(null);
            if (rows != null) {
                for (WorldBossServerPeer row : rows) {
                    putPeer(peers, row.getServerName(), row.getServerIp(), selfName, s.localIps);
                }
            }
            if (peers.isEmpty()) {
                for (WorldBossServer row : servers == null ? Collections.<WorldBossServer>emptyList() : servers) {
                    putPeer(peers, row.getServerName(), row.getServerIp(), selfName, s.localIps);
                }
                peerFrom = "world_boss_server(子表为空)";
            }
            if (peers.isEmpty() && !crossServerProperties.peerMap().isEmpty()) {
                peers.putAll(crossServerProperties.peerMap());
                peerFrom = "回退 crossserver.peers";
            }
            s.peers = peers;
            s.peerFrom = peerFrom;
        } catch (Exception e) {
            // 多为表还没建或还没授权：保留上一次的快照内容，只把这次失败记下来，别让读表异常影响业务
            Snapshot old = snapshot;
            s.selfName = old.selfName;
            s.selfIp = old.selfIp;
            s.selfFrom = old.selfFrom.isEmpty() ? "沿用上次快照" : old.selfFrom + "(沿用上次)";
            s.peers = old.peers;
            s.peerFrom = old.peerFrom.isEmpty() ? "沿用上次快照" : old.peerFrom + "(沿用上次)";
            s.serverRowCount = old.serverRowCount;
            s.localIps = old.localIps.isEmpty() ? localSiteIpv4() : old.localIps;
            s.error = "读节点表失败（表未建好或无权限？自愈会建表）：" + brief(e);
            log.warn("世界BOSS节点表读取失败，本次沿用上次快照。原因={}", s.error);
        }
        snapshot = s;
    }

    /** 把自己那行排除掉，剩下的才是「别的服」；IP 列写成完整地址也照原样收下，免得手工填表时来回猜格式 */
    private void putPeer(Map<String, String> peers, String nameValue, String ipValue, String selfName, List<String> localIps) {
        String name = trim(nameValue);
        String ip = trim(ipValue);
        if (name.isEmpty() || ip.isEmpty()) {
            return;
        }
        if (!selfName.isEmpty() && selfName.equals(name)) {
            return;
        }
        if (localIps.contains(ipOf(ip))) {
            return;
        }
        // 列里已经是完整地址就原样用（有人想把端口写在表里）；否则用纯 IP + 统一端口拼出来
        peers.put(name, ip.startsWith("http") ? ip : "http://" + ipOf(ip) + ":" + peerPort + "/");
    }

    /** 指定了 worldboss.self-ip 就只认它，否则看本机内网 IPv4 里有没有这一行 */
    private boolean matchesSelf(String ip, List<String> localIps) {
        String fixed = trim(selfIpOverride);
        if (!fixed.isEmpty()) {
            return fixed.equals(ipOf(ip));
        }
        return localIps.contains(ipOf(ip));
    }

    /**
     * 从 server_ip 列里取纯 IP：手填时常写成 10.126.126.4:8889 或 http://10.126.126.4/ ，
     * 直接拿整串去比本机网卡 IP 永远不相等，就会出现“表里明明有这台机器却认不出自己”。
     */
    private static String ipOf(String value) {
        String ip = trim(value);
        int scheme = ip.indexOf("://");
        if (scheme >= 0) {
            ip = ip.substring(scheme + 3);
        }
        int slash = ip.indexOf('/');
        if (slash >= 0) {
            ip = ip.substring(0, slash);
        }
        int colon = ip.indexOf(':');
        if (colon >= 0) {
            ip = ip.substring(0, colon);
        }
        return ip;
    }

    /** 枚举本机网卡的内网 IPv4（回环、虚拟网卡、链路本地地址都不要） */
    private List<String> localSiteIpv4() {
        String fixed = trim(selfIpOverride);
        if (!fixed.isEmpty()) {
            return Collections.singletonList(fixed);
        }
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            while (nifs != null && nifs.hasMoreElements()) {
                NetworkInterface nif = nifs.nextElement();
                if (!nif.isUp() || nif.isLoopback() || nif.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addrs = nif.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress() && addr.isSiteLocalAddress()) {
                        String ip = addr.getHostAddress();
                        if (!ips.contains(ip)) {
                            ips.add(ip);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("世界BOSS节点：枚举本机网卡IP失败，只能用配置文件兜底。原因={}", brief(e));
        }
        return ips;
    }

    private String describe0(Snapshot s) {
        return "本服=" + (s.selfName.isEmpty() ? "(未识别)" : s.selfName) + "[" + s.selfFrom + "]"
                + " 本机IP=" + s.localIps + " 节点表=" + s.serverRowCount + "行 对端=" + s.peers + "[" + s.peerFrom + "]"
                + " 载入于" + s.loadedAt + (s.error.isEmpty() ? "" : "；注意：" + s.error);
    }

    private static String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private static String brief(Exception e) {
        Throwable t = e;
        while (t.getCause() != null && t.getCause() != t) {
            t = t.getCause();
        }
        String msg = t.getMessage();
        return t.getClass().getSimpleName() + ": " + (msg == null ? "(无异常信息)" : msg.replaceAll("\\s+", " ").trim());
    }
}
