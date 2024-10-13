package info.preva1l.seamlessserverzones.balancer;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import info.preva1l.seamlessserverzones.Config;
import lombok.experimental.UtilityClass;

import java.net.InetSocketAddress;

@UtilityClass
public class Groups {
    public void registerGroups(ProxyServer proxy) {
        for (String group : Config.i().getGroups().keySet()) {
            for (String server : Config.i().getGroups().get(group)) {
                ServerInfo info = new ServerInfo(server, InetSocketAddress.createUnresolved("127.0.0.1", 25565));
                proxy.registerServer(info);
            }
        }
    }
}
