package info.preva1l.seamlessserverzones;

import de.exlll.configlib.*;
import info.preva1l.seamlessserverzones.balancer.LoadBalancer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Getter
@Configuration
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings("FieldMayBeFinal")
public class Config {
    private static Config instance;

    private static final String CONFIG_HEADER = """
            #########################################
            #          SeamlessServerZones          #
            #########################################
            """;

    private static final YamlConfigurationProperties PROPERTIES = YamlConfigurationProperties.newBuilder()
            .charset(StandardCharsets.UTF_8)
            .setNameFormatter(NameFormatters.LOWER_KEBAB_CASE)
            .header(CONFIG_HEADER).build();

    private List<String> seamlessServers = List.of("survival-spawn-01", "survival-zone-01", "survival-zone-02");

    private boolean loadBalancer = false;

    private Map<String, LoadBalancer> balanceMode = Map.of(
            "survival-spawns", LoadBalancer.RANDOM,
            "Survival", LoadBalancer.MEMORIZE
    );

    @Comment("Has nothing to do with making servers seamless, this is for the loadbalancer.")
    private Map<String, List<String>> groups = Map.of(
            "survival-spawns", List.of("survival-spawn-01"),
            "Survival", List.of("survival-spawns", "survival-zone-01", "survival-zone-02")
    );

    public static Config i() {
        if (instance != null) {
            return instance;
        }

        return instance = YamlConfigurations.update(new File(SeamlessServerZones.getInstance().getData().toFile(), "config.yml").toPath(), Config.class, PROPERTIES);
    }
}
