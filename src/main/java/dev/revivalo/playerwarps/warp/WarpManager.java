package dev.revivalo.playerwarps.warp;

import com.tchristofferson.configupdater.ConfigUpdater;
import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.category.Category;
import dev.revivalo.playerwarps.configuration.YamlFile;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.register.BlueMapHook;
import dev.revivalo.playerwarps.hook.register.DynmapHook;
import dev.revivalo.playerwarps.hook.register.SquaremapHook;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.menu.sort.*;
import dev.revivalo.playerwarps.playerconfig.PlayerConfig;
import dev.revivalo.playerwarps.util.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WarpManager {

    private final Set<Warp> warps;
    private final List<String> bannedWorlds;

    private final SortingManager sortingManager;

    public WarpManager() {
        ConfigurationSerialization.registerClass(Warp.class);

        // Read from the menu thread while the main thread creates and removes warps,
        // so iteration must not throw a ConcurrentModificationException.
        warps = ConcurrentHashMap.newKeySet();

        List<Sortable> sortableList = new ArrayList<>();
        for (String sortableFromConfig : Config.SORT_BY.asList()) {
            switch (sortableFromConfig.toUpperCase(Locale.ENGLISH)) {
                case "ALPHABETICAL":
                    sortableList.add(new AlphabeticalSort());
                    break;
                case "VISITS":
                    sortableList.add(new VisitsSort());
                    break;
                case "LATEST":
                    sortableList.add(new LatestSort());
                    break;
                case "RATING":
                    sortableList.add(new RatingSort());
                    break;
            }
        }
        sortingManager = new SortingManager(
                sortableList
        );

        bannedWorlds = new ArrayList<>();
        bannedWorlds.addAll(Config.DISABLED_WORLDS.asList());
    }

    public void reloadWarps(CommandSender sender) {
        PlayerWarpsPlugin.get().reloadConfig();
        File configFile = new File(PlayerWarpsPlugin.get().getDataFolder(), "config.yml");

        try {
            ConfigUpdater.update(PlayerWarpsPlugin.get(), "config.yml", configFile, Collections.emptyList());
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        sender.sendMessage(Lang.RELOAD_MESSAGE.asColoredString());
    }

    public List<Warp> getFeaturedWarps() {
        return warps.stream().filter(warp -> (warp.getFeaturedTimestamp() - System.currentTimeMillis()) > 0).collect(Collectors.toList());
    }

    public void loadWarps() {
        clearWarps();
        final ConfigurationSection warpSection = PlayerWarpsPlugin.getData().getConfiguration().getConfigurationSection("warps");
        if (warpSection == null) {
            return;
        }

        Map<String, Object> rawWarps = null;

        for (String warpID : warpSection.getKeys(false)) {
            Warp warp = warpSection.getSerializable(warpID, Warp.class);
            if (warp == null) {
                PlayerWarpsPlugin.get().getLogger().info("Error while importing warp " + warpID);
                continue;
            }

            if (warp.getLocation() == null) {
                // Bukkit drops a location whose world is not loaded while reading the file, so it
                // is taken from the raw file instead. Otherwise it would be gone after the next save.
                if (rawWarps == null) {
                    rawWarps = readRawWarps();
                }

                restoreUnresolvedLocation(warp, rawWarps.get(warpID));
            }

            addWarp(warp);
            HookRegister.ifEnabled(DynmapHook.class, dynmapHook -> dynmapHook.setMarker(warp));
            HookRegister.ifEnabled(BlueMapHook.class, blueMapHook -> blueMapHook.setMarker(warp));
            HookRegister.ifEnabled(SquaremapHook.class, squaremapHook -> squaremapHook.setMarker(warp));
        }
    }

    private Map<String, Object> readRawWarps() {
        final Object warps = PlayerWarpsPlugin.getData().getYamlFile().readRaw().get("warps");
        if (!(warps instanceof Map<?, ?> map)) {
            return Collections.emptyMap();
        }

        final Map<String, Object> result = new HashMap<>();
        map.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }

    @SuppressWarnings("unchecked")
    private void restoreUnresolvedLocation(Warp warp, @Nullable Object rawWarp) {
        if (!(rawWarp instanceof Map<?, ?> rawWarpMap) || !(rawWarpMap.get("loc") instanceof Map<?, ?> rawLocation)) {
            return;
        }

        warp.setUnresolvedLocation((Map<String, Object>) rawLocation);
        if (warp.hasUnresolvedLocation()) {
            PlayerWarpsPlugin.get().getLogger().warning("Warp " + warp.getName() + " is in world '" + warp.getWorldName()
                    + "', which is not loaded. The warp is kept and becomes usable once the world is loaded.");
        }
    }

    public void saveWarps() {
        saveWarps(true);
    }

    /**
     * Serializes all warps and writes them to data.yml. Serialization reads warp state and
     * Bukkit objects, so it always runs on the calling (main) thread; only the file write can
     * be offloaded. On shutdown the write has to stay synchronous, as the scheduler is gone.
     */
    public void saveWarps(boolean writeAsynchronously) {
        if (!PlayerWarpsPlugin.getData().getYamlFile().isLoaded()) {
            // The warps were never read, so writing the (empty) list would wipe data.yml.
            PlayerWarpsPlugin.get().getLogger().severe("Warps are not saved: data.yml could not be loaded on startup. "
                    + "Fix the file and restart the server.");
            return;
        }

        final ConfigurationSection warpsSection = PlayerWarpsPlugin.getData().getConfiguration().createSection("warps");

        warps.forEach(warp -> warpsSection.set(warp.getWarpID().toString(), warp));

        final YamlFile dataFile = PlayerWarpsPlugin.getData().getYamlFile();
        final String contents = PlayerWarpsPlugin.getData().getConfiguration().saveToString();
        final int savedWarps = warps.size();

        if (writeAsynchronously) {
            PlayerWarpsPlugin.get().runAsync(() -> writeWarps(dataFile, contents, savedWarps));
        } else {
            writeWarps(dataFile, contents, savedWarps);
        }
    }

    private void writeWarps(YamlFile dataFile, String contents, int savedWarps) {
        dataFile.write(contents);

        if (Config.AUTOSAVE_ANNOUNCE.asBoolean()) {
            Bukkit.getLogger().info("Saving " + savedWarps + " warps");
        }
    }

    public boolean canHaveWarp(final Player player) {
        UUID id = player.getUniqueId();
        if (!player.hasPermission("playerwarps.limit.unlimited")) {
            return PermissionUtil.getLimit(player, Config.DEFAULT_LIMIT_SIZE.asInteger()) > getOwnedWarps(id);
        }
        return true;
    }

    private int getOwnedWarps(UUID id) {
        int owned = 0;
        if (areWarps()) {
            for (Warp warp : warps) {
                if (Objects.equals(id, warp.getOwner())) {
                    ++owned;
                }
            }
        }
        return owned;
    }

    /**
     * Stores the current name on every warp the player owns. The server's user cache expires,
     * so this keeps "Unknown" owners (and searching by owner) working across restarts.
     */
    public void refreshOwnerNames(final Player owner) {
        final String name = owner.getName();

        for (Warp warp : warps) {
            if (Objects.equals(warp.getOwner(), owner.getUniqueId()) && !name.equals(warp.getOwnerName())) {
                warp.setOwnerName(name);
            }
        }
    }

    public int getCountOfWarps(Category category) {
        final String categoryName = category.getType();
        return (int) warps.stream()
                .filter(Warp::isAccessible)
                .filter(warp -> categoryName.equalsIgnoreCase("all") || warp.getCategory() != null && warp.getCategory().getType() != null && warp.getCategory().getType().equalsIgnoreCase(categoryName))
                .count();
    }

    public void clearWarps() {
        warps.clear();
    }

    public void addWarp(Warp warp) {
        //sortingManager.invalidateCache();
        warps.add(warp);
    }

    public void removeWarp(Warp warp) {
        //sortingManager.invalidateCache();
        warps.remove(warp);
    }

    public boolean existsWarp(String warpName) {
        return warps.stream().anyMatch(w -> w.getName().equalsIgnoreCase(warpName));
    }

    public boolean areWarps() {
        return !warps.isEmpty();
    }

    public Set<Warp> getWarps() {
        return warps;
    }

    public Optional<Warp> getWarpByID(final UUID warpID) {
        return warps.stream().filter(warp -> Objects.equals(warp.getWarpID(), warpID)).findAny();
    }

    public Optional<Warp> getWarpByID(final String warpID) {
        return getWarpByID(UUID.fromString(warpID));
    }

    public Optional<Warp> getWarpFromName(final String warpName) {
        return warps.stream().filter(a -> a.getName().equalsIgnoreCase(warpName)).findAny();
    }

    public List<Warp> getPlayerFavoriteWarps(Player player) {
        return PlayerConfig.getConfig(player).getStringList("favorites").stream()
                .map(PlayerWarpsPlugin.getWarpHandler()::getWarpByID)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public Set<Warp> getPlayerWarps() {
        return getPlayerWarps(null);
    }

    public Set<Warp> getPlayerWarps(@Nullable final Player owner) {
        return warps.stream()
                .filter(warp -> owner == null || warp.isOwner(owner))
                .collect(Collectors.toSet());
    }

    public boolean checkWarp(Warp warp) {
        return warp == null;
    }

    public List<String> getBannedWorlds() {
        return bannedWorlds;
    }

    public SortingManager getSortingManager() {
        return sortingManager;
    }
}