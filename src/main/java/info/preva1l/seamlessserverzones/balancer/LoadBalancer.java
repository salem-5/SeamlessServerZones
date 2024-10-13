package info.preva1l.seamlessserverzones.balancer;

import info.preva1l.seamlessserverzones.Config;
import info.preva1l.seamlessserverzones.SeamlessServerZones;

import java.util.List;

public enum LoadBalancer {
    MEMORIZE {
        @Override
        public String getServer(String group) {
            List<String> servers = Config.i().getGroups().get(group);

            return "";
        }
    },
//    LOWEST_PLAYER {
//        @Override
//        public Server getServer(String group) {
//            List<Server> servers = ServerManager.getInstance().getAllServers();
//            servers.removeIf(server -> server.getType() != serverType);
//            servers.sort(Comparator.comparingInt(Server::getPlayerCount));
//            return servers.getFirst();
//        }
//    },
//    LOWEST_USAGE {
//        @Override
//        public String getServer(String serverType) {
//            List<Server> servers = ServerManager.getInstance().getAllServers();
//            servers.removeIf(server -> server.getType() != serverType);
//            servers.sort(Comparator.comparingDouble(Server::getMspt));
//            return servers.getFirst();
//        }
//    },
    RANDOM {
        @Override
        public String getServer(String group) {
            List<String> servers = Config.i().getGroups().get(group);
            int index = SeamlessServerZones.getInstance().getRandom().nextInt(servers.size() - 1);
            return servers.get(index);
        }
    };

    public abstract String getServer(String group);
}
