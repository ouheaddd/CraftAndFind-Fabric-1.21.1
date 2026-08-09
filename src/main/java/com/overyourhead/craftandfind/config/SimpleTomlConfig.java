package com.overyourhead.craftandfind.config;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Tiny dependency-free TOML reader for Craft & Find's fixed scalar settings. */
final class SimpleTomlConfig {
    private SimpleTomlConfig() {
    }

    static Path configPath(String fileName) {
        return FabricLoader.getInstance().getConfigDir().resolve(fileName);
    }

    static Map<String, String> load(Path path, String defaults) {
        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.writeString(path, defaults, StandardCharsets.UTF_8);
            }
            return parse(Files.readAllLines(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            System.err.println("[Craft & Find] Failed to load config " + path + ": " + exception.getMessage());
            return Map.of();
        }
    }

    private static Map<String, String> parse(Iterable<String> lines) {
        Map<String, String> values = new HashMap<>();
        String section = "";
        for (String rawLine : lines) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }
            int equals = line.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            String key = line.substring(0, equals).trim();
            String value = line.substring(equals + 1).trim();
            values.put(section.isEmpty() ? key : section + "." + key, value);
        }
        return values;
    }

    private static String stripComment(String line) {
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (ch == '"') {
                quoted = !quoted;
            } else if (ch == '#' && !quoted) {
                return line.substring(0, index);
            }
        }
        return line;
    }

    static boolean bool(Map<String, String> values, String key, boolean fallback) {
        String value = values.get(key);
        if (value == null) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        return fallback;
    }

    static int integer(Map<String, String> values, String key, int fallback, int minimum, int maximum) {
        try {
            return clamp(Integer.parseInt(values.getOrDefault(key, Integer.toString(fallback))), minimum, maximum);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    static double decimal(Map<String, String> values, String key, double fallback, double minimum, double maximum) {
        try {
            return clamp(Double.parseDouble(values.getOrDefault(key, Double.toString(fallback))), minimum, maximum);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
