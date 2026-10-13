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

    /**
     * Scoring every warp on each menu open would be wasteful, and the ranking only moves as
     * players travel, so the result is reused for a short while.
     */
    private static final long RECOMMENDED_CACHE_DURATION = 30_000L;

    private final Set<Warp> warps;
    private final List<String> bannedWorlds;

    private final SortingManager sortingManager;

    // Read from the menu thread, written by whichever thread refreshes the list first.
    private volatile List<Warp> recommendedWarps = Collections.emptyList();
    private volatile long recommendedWarpsRefreshedAt;

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

    /**
     * The warps players are visiting the most right now, best first. Unlike the featured list
     * this is not bought - it is derived from the visits of the last few days.
     */
    public List<Warp> getRecommendedWarps() {
        final long now = System.currentTimeMillis();
        if (now - recommendedWarpsRefreshedAt < RECOMMENDED_CACHE_DURATION) {
            return recommendedWarps;
        }

        final int period = Math.min(Config.RECOMMENDED_WARPS_PERIOD.asInteger(), Warp.RECENT_VISITS_RETENTION_DAYS);
        final int minimumVisits = Config.RECOMMENDED_WARPS_MIN_VISITS.asInteger();
        final int limit = Config.MAX_RECOMMENDED_WARPS.asInteger();

        final List<Warp> ranked = warps.stream()
                .filter(Warp::isAccessible)
                .filter(warp -> warp.getRecentVisits(period) >= minimumVisits)
                .sorted(Comparator
                        .comparingDouble((Warp warp) -> getTrendingScore(warp, period))
                        .thenComparingInt(Warp::getVisits)
                        .reversed()
                        .thenComparing(Warp::getName))
                .limit(Math.max(limit, 0))
                .collect(Collectors.toList());

        recommendedWarps = ranked;
        recommendedWarpsRefreshedAt = now;

        return ranked;
    }

    /**
     * Weighs a warp's visits by how recent they are, so that a warp busy today outranks one
     * that was busy at the start of the period with the same total.
     */
    private static double getTrendingScore(final Warp warp, final int period) {
        if (period <= 0) {
            return 0;
        }

        final long today = Warp.currentDay();

        double score = 0;
        for (Map.Entry<Long, Integer> entry : warp.getRecentVisits().entrySet()) {
            final long age = today - entry.getKey();
            if (age < 0 || age >= period) {
                continue;
            }

            score += entry.getValue() * (double) (period - age) / period;
        }

        return score;
    }

    public void loadWarps() {
        clearWarps();
        Optional<ConfigurationSection> warpDataSection = Optional.ofNullable(PlayerWarpsPlugin.getData().getConfiguration().getConfigurationSection("warps"));
        warpDataSection.flatMap(warpsSection -> warpDataSection).ifPresent(warpSection ->
                warpSection
                        .getKeys(false)
                        .forEach(warpID -> {
                                    Warp warp = warpSection.getSerializable(warpID, Warp.class);
                                    if (warp == null) {
                                        PlayerWarpsPlugin.get().getLogger().info("Error while importing warp " + warpID);
                                        return;
                                    }

                                    addWarp(warp);
                                    HookRegister.ifEnabled(DynmapHook.class, dynmapHook -> dynmapHook.setMarker(warp));
                                    HookRegister.ifEnabled(BlueMapHook.class, blueMapHook -> blueMapHook.setMarker(warp));
                                    HookRegister.ifEnabled(SquaremapHook.class, squaremapHook -> squaremapHook.setMarker(warp));
                                }
                        ));
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