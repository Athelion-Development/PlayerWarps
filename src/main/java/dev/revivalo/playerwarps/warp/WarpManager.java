package dev.revivalo.playerwarps.warp;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.category.Category;
import dev.revivalo.playerwarps.category.CategoryManager;
import dev.revivalo.playerwarps.commandmanager.command.PwarpMainCommand;
import dev.revivalo.playerwarps.configuration.YamlFile;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.register.BlueMapHook;
import dev.revivalo.playerwarps.hook.register.DynmapHook;
import dev.revivalo.playerwarps.hook.register.SquaremapHook;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.menu.sort.*;
import dev.revivalo.playerwarps.playerconfig.PlayerConfig;
import dev.revivalo.playerwarps.util.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class WarpManager {

    private final Set<Warp> warps;

    /**
     * Warps from data.yml that could not be loaded, by ID, exactly as they are stored in the
     * file. They are written back unchanged on every save instead of being dropped.
     */
    private final Map<String, Object> unloadableWarps = new LinkedHashMap<>();

    // Replaced as a whole on reload, so readers on the menu thread never see a half-filled list.
    private volatile List<String> bannedWorlds = Collections.emptyList();
    private volatile SortingManager sortingManager;

    public WarpManager() {
        ConfigurationSerialization.registerClass(Warp.class);

        // Read from the menu thread while the main thread creates and removes warps,
        // so iteration must not throw a ConcurrentModificationException.
        warps = ConcurrentHashMap.newKeySet();

        reloadSettings();
    }

    /**
     * Re-reads the settings taken from config.yml: the sort types and the disabled worlds.
     */
    public void reloadSettings() {
        List<Sortable> sortableList = new ArrayList<>();
        for (String sortableFromConfig : Optional.ofNullable(Config.SORT_BY.asList()).orElse(Collections.emptyList())) {
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
                default:
                    PlayerWarpsPlugin.get().getLogger().warning("Unknown sort type '" + sortableFromConfig + "' in sort-by.");
            }
        }

        if (sortableList.isEmpty()) {
            PlayerWarpsPlugin.get().getLogger().warning("No valid sort type in sort-by, sorting alphabetically.");
            sortableList.add(new AlphabeticalSort());
        }

        sortingManager = new SortingManager(sortableList);
        bannedWorlds = List.copyOf(Optional.ofNullable(Config.DISABLED_WORLDS.asList()).orElse(Collections.emptyList()));
    }

    /**
     * Points every warp to the category of the same name after categories.yml was reloaded.
     * Warps would otherwise keep the old objects with their old names and permissions.
     */
    public void refreshCategories() {
        for (Warp warp : warps) {
            final Category category = warp.getCategory();
            warp.setCategory(CategoryManager.getCategoryFromName(category == null ? null : category.getType()));
        }
    }

    public List<Warp> getFeaturedWarps() {
        return warps.stream().filter(warp -> (warp.getFeaturedTimestamp() - System.currentTimeMillis()) > 0).collect(Collectors.toList());
    }

    public void loadWarps() {
        clearWarps();
        unloadableWarps.clear();

        final ConfigurationSection warpSection = PlayerWarpsPlugin.getData().getConfiguration().getConfigurationSection("warps");

        // Bukkit silently leaves out every warp it fails to deserialize, so the IDs come from the
        // raw file as well. Otherwise such a warp would simply be gone after the next save.
        final Map<String, Object> rawWarps = readRawWarps();
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
                // Bukkit drops a location whose world is not loaded while reading the file, so it
                // is taken from the raw file instead. Otherwise it would be gone after the next save.
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
                    rawWarps = readRawWarps();
                }

                final Object previous = rawWarps.get(warpID);
                if (previous != null) {
                    warpsSection.set(warpID, previous);
                }
            }
        }

        unloadableWarps.forEach(warpsSection::set);

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

    /**
     * Whether a warp may not be called like this, because "/pwarp <name>" would run the
     * subcommand of that name instead of teleporting.
     */
    public boolean isReservedName(String warpName) {
        final PwarpMainCommand mainCommand = PlayerWarpsPlugin.get().getMainCommand();
        return mainCommand != null && mainCommand.isSubCommand(warpName);
    }

    /**
     * Warps created before names of subcommands were refused cannot be reached by name.
     */
    public void reportReservedNames() {
        for (Warp warp : warps) {
            if (isReservedName(warp.getName())) {
                PlayerWarpsPlugin.get().getLogger().warning("Warp '" + warp.getName() + "' has the name of a /pwarp subcommand, "
                        + "so it cannot be visited with /pwarp " + warp.getName() + ". It is still listed in the menu - rename it.");
            }
        }
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