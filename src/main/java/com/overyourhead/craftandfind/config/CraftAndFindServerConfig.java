package com.overyourhead.craftandfind.config;

import java.util.Map;

/** World/server settings for storage discovery and update frequency. */
public final class CraftAndFindServerConfig {
    private static int searchRadius = 16;
    private static int maxHighlightedContainers = 64;
    private static int containerScanIntervalTicks = 100;
    private static int contentRefreshIntervalTicks = 20;

    private static final String DEFAULTS = """
            # Gameplay settings for nearby storage discovery.
            # On multiplayer servers, the server values are used.
            [storage]
            # Maximum distance from the Storage Workbench in which containers are discovered.
            # The scan uses a three-dimensional radius, so larger values can increase server work.
            # Recommended: small storage room 8-12, normal storage room 16, large warehouse 24.
            # Use 32 only for very large storage builds.
            # Default: 16
            # Range: 4 ~ 32
            searchRadius = 16
            # Maximum number of matching containers that receive highlight effects after a click.
            # This does not limit storage searching, crafting, or the displayed total item count.
            # Recommended: weak clients/large servers 32, default 64, showcase builds up to 128.
            # Default: 64
            # Range: 1 ~ 128
            maxHighlightedContainers = 64

            # Advanced performance settings.
            # Lower intervals react faster but make the server perform the related work more often.
            # 20 game ticks are approximately one second. Most users should keep the defaults.
            [advanced]
            # How often the workbench performs a full radius scan for placed or removed containers.
            # Recommended: fast updates 40-60, default 100, large servers 200-400.
            # Default: 100
            # Range: 20 ~ 600
            containerScanIntervalTicks = 100
            # How often item contents of already discovered containers are refreshed and checked
            # for a changed storage snapshot.
            # Recommended: very responsive 10, default 20, large servers 40-60.
            # Default: 20
            # Range: 5 ~ 100
            contentRefreshIntervalTicks = 20
            """;

    private CraftAndFindServerConfig() {
    }

    public static void load() {
        Map<String, String> values = SimpleTomlConfig.load(
                SimpleTomlConfig.configPath("craftandfind-server.toml"),
                DEFAULTS
        );
        searchRadius = SimpleTomlConfig.integer(values, "storage.searchRadius", 16, 4, 32);
        maxHighlightedContainers = SimpleTomlConfig.integer(values, "storage.maxHighlightedContainers", 64, 1, 128);
        containerScanIntervalTicks = SimpleTomlConfig.integer(values, "advanced.containerScanIntervalTicks", 100, 20, 600);
        contentRefreshIntervalTicks = SimpleTomlConfig.integer(values, "advanced.contentRefreshIntervalTicks", 20, 5, 100);
    }

    public static int searchRadius() { return searchRadius; }
    public static int maxHighlightedContainers() { return maxHighlightedContainers; }
    public static int containerScanIntervalTicks() { return containerScanIntervalTicks; }
    public static int contentRefreshIntervalTicks() { return contentRefreshIntervalTicks; }
}
