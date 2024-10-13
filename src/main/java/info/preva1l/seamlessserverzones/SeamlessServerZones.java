package info.preva1l.seamlessserverzones;

import com.google.inject.Inject;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.Connections;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;

@Plugin(
        id = "seamlessserverzones",
        name = "SeamlessServerZones",
        version = BuildConstants.VERSION,
        description = "Addon to AdvancedServerZones to make zone transfers seamless!",
        url = "https://preva1l.info/",
        authors = {"Preva1l"}
)
public class SeamlessServerZones {
    @Getter
    private static SeamlessServerZones instance;
    @Getter
    private final Path data;
    @Getter
    private final Random random;
    private final Logger logger;

    @Inject
    public SeamlessServerZones(ProxyServer server, @DataDirectory Path dataDirectory, Logger logger) {
        instance = this;
        this.data = dataDirectory;
        this.random = new Random(System.currentTimeMillis());
        this.logger = logger;
        Config.i();
        logger.info("Loaded SSZ");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {

    }

    @Subscribe
    public void onServerChange(ServerPreConnectEvent event) {
        List<String> seamlessServers = Config.i().getSeamlessServers();
        RegisteredServer oldServer = event.getPreviousServer();
        if (oldServer == null) return;
        RegisteredServer newServer = event.getResult().getServer().orElseThrow();

        PlayerChannelHandler handler = ((PlayerChannelHandler) ((ConnectedPlayer) event.getPlayer())
                .getConnection()
                .getChannel()
                .pipeline()
                .get("seamless-server-zones"));

        if (!seamlessServers.contains(oldServer.getServerInfo().getName())) {
            handler.flush();
            return;
        }

        if (!seamlessServers.contains(newServer.getServerInfo().getName())) {
            handler.flush();
            return;
        }

        handler.startSeamlessReconnect();
    }

    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        ((ConnectedPlayer) event.getPlayer())
                .getConnection()
                .getChannel()
                .pipeline()
                .addBefore(Connections.HANDLER, "seamless-server-zones", new PlayerChannelHandler(event.getPlayer()));
    }
}
