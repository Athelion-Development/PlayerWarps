package dev.revivalo.playerwarps.storage;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.configuration.Data;
import dev.revivalo.playerwarps.configuration.YamlFile;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.warp.Warp;
import dev.revivalo.playerwarps.warp.WarpManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.logging.Level;

/**
 * Keeps all warps in data.yml.
 */
public final class YamlWarpStorage implements WarpStorage {

    private final Data data;

    /**
     * Warps from data.yml that could not be loaded, by ID, exactly as they are stored in the
     * file. They are written back unchanged on every save instead of being dropped.
     */
    private final Map<String, Object> unloadableWarps = new LinkedHashMap<>();

    public YamlWarpStorage() {
        // Parses data.yml, which deserializes every warp - categories and hooks have to be ready.
        this.data = new Data();
    }

    @Override
    public StorageType getType() {
        return StorageType.YAML;
    }

    @Override
    public void load(WarpManager manager, Runnable onLoaded) {
        manager.clearWarps();
        unloadableWarps.clear();

        for (Warp warp : readWarps(data, unloadableWarps)) {
            manager.addWarp(warp);
            manager.showOnMaps(warp);
        }

        onLoaded.run();
    }

    /**
     * Reads every warp from the file. Warps that cannot be loaded are put into
     * {@code unloadableWarps} as they are stored, keyed by their ID.
     */
    static List<Warp> readWarps(Data data, Map<String, Object> unloadableWarps) {
        final List<Warp> result = new ArrayList<>();
        final ConfigurationSection warpSection = data.getConfiguration().getConfigurationSection("warps");

        // Bukkit silently leaves out every warp it fails to deserialize, so the IDs come from the
        // raw file as well. Otherwise such a warp would simply be gone after the next save.
        final Map<String, Object> rawWarps = readRawWarps(data);
        final Set<String> warpIDs = new LinkedHashSet<>(rawWarps.keySet());
        if (warpSection != null) {
            warpIDs.addAll(warpSection.getKeys(false));
        }

        for (String warpID : warpIDs) {
            final Warp warp = warpSection == null ? null : warpSection.getSerializable(warpID, Warp.class);
            if (warp == null || !warp.isComplete()) {
                final Object rawWarp = rawWarps.get(warpID);
                if (rawWarp != null) {
                    unloadableWarps.put(warpID, rawWarp);
                }

                PlayerWarpsPlugin.get().getLogger().warning("Warp " + warpID + " could not be loaded (it is missing its ID, owner or name, or see the errors above). "
                        + "It is kept unchanged in data.yml.");
                continue;
            }

            if (warp.getLocation() == null) {
                WarpCodec.restoreUnresolvedLocation(warp, rawWarps.get(warpID));
            }

            result.add(warp);
        }

        return result;
    }

    private static Map<String, Object> readRawWarps(Data data) {
        final Object warps = data.getYamlFile().readRaw().get("warps");
        if (!(warps instanceof Map<?, ?> map)) {
            return Collections.emptyMap();
        }

        final Map<String, Object> result = new HashMap<>();
        map.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    @Override
    public void startTasks(WarpManager manager) {
        if (!Config.AUTOSAVE_ENABLED.asBoolean()) {
            return;
        }

        long intervalInTicks = (Config.AUTOSAVE_INTERVAL.asLong() * 60) * 20;
        // Runs synchronously on purpose - save() serializes on the calling thread and
        // offloads only the file write.
        new BukkitRunnable() {
            @Override
            public void run() {
                manager.saveWarps();
            }
        }.runTaskTimer(PlayerWarpsPlugin.get(), intervalInTicks, intervalInTicks);
    }

    /**
     * Serializes all warps and writes them to data.yml. Serialization reads warp state and
     * Bukkit objects, so it always runs on the calling (main) thread; only the file write can
     * be offloaded. On shutdown the write has to stay synchronous, as the scheduler is gone.
     */
    @Override
    public void save(WarpManager manager, boolean asynchronously) {
        write(data, manager.getWarps(), unloadableWarps, asynchronously);
    }

    @Override
    public void shutdown(WarpManager manager) {
        save(manager, false);
    }

    /**
     * Replaces the warps in data.yml with the given ones. The previous file is kept as
     * data.yml.bak.
     *
     * @return false if data.yml exists but cannot be read, nothing is written then
     */
    public static boolean export(Collection<Warp> warps) {
        final Data data = new Data();
        if (!data.getYamlFile().isLoaded()) {
            return false;
        }

        write(data, warps, Collections.emptyMap(), true);
        return true;
    }

    private static void write(Data data, Collection<Warp> warps, Map<String, Object> unloadableWarps, boolean asynchronously) {
        if (!data.getYamlFile().isLoaded()) {
            // The warps were never read, so writing the (empty) list would wipe data.yml.
            PlayerWarpsPlugin.get().getLogger().severe("Warps are not saved: data.yml could not be loaded on startup. "
                    + "Fix the file and restart the server.");
            return;
        }

        final ConfigurationSection warpsSection = data.getConfiguration().createSection("warps");

        // Serialized one by one, so a warp that fails cannot stop the others from being saved.
        // It keeps the version that is already in the file instead.
        Map<String, Object> rawWarps = null;
        for (Warp warp : warps) {
            final String warpID = warp.getWarpID().toString();
            try {
                final Map<String, Object> serialized = new LinkedHashMap<>();
                serialized.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(Warp.class));
                serialized.putAll(warp.serialize());
                warpsSection.set(warpID, serialized);
            } catch (RuntimeException ex) {
                PlayerWarpsPlugin.get().getLogger().log(Level.SEVERE, "Cannot save warp " + warp.getName()
                        + ", keeping its previously saved version.", ex);

                if (rawWarps == null) {
                    rawWarps = readRawWarps(data);
                }

                final Object previous = rawWarps.get(warpID);
                if (previous != null) {
                    warpsSection.set(warpID, previous);
                }
            }
        }

        unloadableWarps.forEach(warpsSection::set);

        final YamlFile dataFile = data.getYamlFile();
        final String contents = data.getConfiguration().saveToString();
        final int savedWarps = warps.size();

        if (asynchronously) {
            PlayerWarpsPlugin.get().runAsync(() -> writeWarps(dataFile, contents, savedWarps));
        } else {
            writeWarps(dataFile, contents, savedWarps);
        }
    }

    private static void writeWarps(YamlFile dataFile, String contents, int savedWarps) {
        dataFile.write(contents);

        if (Config.AUTOSAVE_ANNOUNCE.asBoolean()) {
            Bukkit.getLogger().info("Saving " + savedWarps + " warps");
        }
    }
}
