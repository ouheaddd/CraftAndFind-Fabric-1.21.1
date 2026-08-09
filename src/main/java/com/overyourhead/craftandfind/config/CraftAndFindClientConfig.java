package com.overyourhead.craftandfind.config;

import java.util.Map;

/** Client-only presentation settings for storage highlight feedback. */
public final class CraftAndFindClientConfig {
    private static boolean enabled = true;
    private static boolean showItemMarker = true;
    private static boolean enableParticles = true;
    private static double particleAmount = 1.0D;
    private static double durationSeconds = 5.0D;
    private static double extraParticleDurationSeconds = 1.5D;
    private static boolean enableSounds = true;
    private static double soundVolume = 1.0D;

    private static final String DEFAULTS = """
            # Storage highlight appearance and sound settings.
            # These options only affect this client. They do not change storage searching,
            # crafting, item counts, or the settings of other players.
            [highlight]
            # Enables the complete storage-location feedback effect.
            # true  = show enabled marker/particles and play enabled sounds
            # false = disable the location effect without disabling storage search or crafting
            enabled = true
            # Shows the framed item icon above the primary matching container.
            # Disable this to keep only particles and sounds.
            showItemMarker = true
            # Shows particles around containers that contain the selected item.
            # Disable this on weak computers or when particles interfere with shaders.
            enableParticles = true
            # Multiplier for the number of storage-highlight particles.
            # 0.0 = no particles, 0.5 = half, 1.0 = default, 1.5 = 50% more, 2.0 = maximum.
            # Recommended: weak computer 0.5, normal computer 1.0, strong computer 1.25-1.5.
            # Default: 1.0
            # Range: 0.0 ~ 2.0
            particleAmount = 1.0
            # How long the framed item marker remains visible, in seconds.
            # Recommended: small storage room 3-4, normal usage 5, large warehouse 7-10.
            # Default: 5.0
            # Range: 1.0 ~ 15.0
            durationSeconds = 5.0
            # How long particles remain after the item marker disappears, in seconds.
            # 0.0 makes particles disappear with the marker; 1.5 is the default smooth ending.
            # Default: 1.5
            # Range: 0.0 ~ 5.0
            extraParticleDurationSeconds = 1.5
            # Enables the click and directional chime sounds for storage highlighting.
            enableSounds = true
            # Volume multiplier for Craft & Find highlight sounds.
            # 0.0 = silent, 0.5 = half volume, 1.0 = default full volume.
            # Default: 1.0
            # Range: 0.0 ~ 1.0
            soundVolume = 1.0
            """;

    private CraftAndFindClientConfig() {
    }

    public static void load() {
        Map<String, String> values = SimpleTomlConfig.load(
                SimpleTomlConfig.configPath("craftandfind-client.toml"),
                DEFAULTS
        );
        enabled = SimpleTomlConfig.bool(values, "highlight.enabled", true);
        showItemMarker = SimpleTomlConfig.bool(values, "highlight.showItemMarker", true);
        enableParticles = SimpleTomlConfig.bool(values, "highlight.enableParticles", true);
        particleAmount = SimpleTomlConfig.decimal(values, "highlight.particleAmount", 1.0D, 0.0D, 2.0D);
        durationSeconds = SimpleTomlConfig.decimal(values, "highlight.durationSeconds", 5.0D, 1.0D, 15.0D);
        extraParticleDurationSeconds = SimpleTomlConfig.decimal(values, "highlight.extraParticleDurationSeconds", 1.5D, 0.0D, 5.0D);
        enableSounds = SimpleTomlConfig.bool(values, "highlight.enableSounds", true);
        soundVolume = SimpleTomlConfig.decimal(values, "highlight.soundVolume", 1.0D, 0.0D, 1.0D);
    }

    public static boolean enabled() { return enabled; }
    public static boolean showItemMarker() { return showItemMarker; }
    public static boolean enableParticles() { return enableParticles; }
    public static double particleAmount() { return particleAmount; }
    public static long markerDurationMillis() { return Math.round(durationSeconds * 1_000.0D); }
    public static long extraParticleDurationMillis() { return Math.round(extraParticleDurationSeconds * 1_000.0D); }
    public static boolean enableSounds() { return enableSounds; }
    public static float soundVolume() { return (float) soundVolume; }
}
