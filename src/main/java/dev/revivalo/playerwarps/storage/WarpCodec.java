package dev.revivalo.playerwarps.storage;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.warp.Warp;
import org.bukkit.Location;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Turns a single warp into text and back, in the same YAML form it has in data.yml. Both
 * directions create Bukkit objects, so they run on the main thread.
 */
final class WarpCodec {
    private static final String KEY = "warp";

    private WarpCodec() {
    }

    static String encode(Warp warp) {
        final YamlConfiguration yaml = new YamlConfiguration();
        yaml.set(KEY, warp);
        return yaml.saveToString();
    }

    /**
     * @return the warp, or null if it cannot be read or misses its ID, owner or name
     */
    @Nullable
    static Warp decode(String data) {
        final YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(data);
        } catch (InvalidConfigurationException | RuntimeException ex) {
            PlayerWarpsPlugin.get().getLogger().warning("Cannot read a warp from the database: " + ex.getMessage());
            return null;
        }

        final Warp warp = yaml.getSerializable(KEY, Warp.class);
        if (warp == null || !warp.isComplete()) {
            return null;
        }

        if (warp.getLocation() == null) {
            restoreUnresolvedLocation(warp, readRaw(data));
        }

        return warp;
    }

    /**
     * The warp's stored values, compared to find out whether it changed since it was saved.
     * Mutable values are copied, so later changes to the warp do not change the snapshot.
     */
    static Map<String, Object> snapshot(Warp warp) {
        final Map<String, Object> snapshot = new HashMap<>(warp.serialize());
        snapshot.computeIfPresent("loc", (key, value) -> {
            if (value instanceof Location location) return location.clone();
            if (value instanceof Map<?, ?> map) return new LinkedHashMap<>(map);
            return value;
        });
        snapshot.computeIfPresent("item", (key, value) -> value instanceof ItemStack item ? item.clone() : value);
        return snapshot;
    }

    /**
     * Bukkit drops a location whose world is not loaded while reading, so it is taken from the
     * raw values instead. Otherwise the warp would lose it on the next save.
     */
    @SuppressWarnings("unchecked")
    static void restoreUnresolvedLocation(Warp warp, @Nullable Object rawWarp) {
        if (!(rawWarp instanceof Map<?, ?> rawWarpMap) || !(rawWarpMap.get("loc") instanceof Map<?, ?> rawLocation)) {
            return;
        }

        warp.setUnresolvedLocation((Map<String, Object>) rawLocation);
        if (warp.hasUnresolvedLocation()) {
            PlayerWarpsPlugin.get().getLogger().warning("Warp " + warp.getName() + " is in world '" + warp.getWorldName()
                    + "', which is not loaded. The warp is kept and becomes usable once the world is loaded.");
        }
    }

    @Nullable
    private static Object readRaw(String data) {
        final LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(Integer.MAX_VALUE);

        try {
            final Object root = new Yaml(new SafeConstructor(options)).load(data);
            return root instanceof Map<?, ?> map ? map.get(KEY) : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
