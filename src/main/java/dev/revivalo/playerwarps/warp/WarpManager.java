package dev.revivalo.playerwarps.warp;

import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.category.Category;
import dev.revivalo.playerwarps.category.CategoryManager;
import dev.revivalo.playerwarps.commandmanager.command.PwarpMainCommand;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.hook.register.BlueMapHook;
import dev.revivalo.playerwarps.hook.register.DynmapHook;
import dev.revivalo.playerwarps.hook.register.SquaremapHook;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.menu.sort.*;
import dev.revivalo.playerwarps.playerconfig.PlayerConfig;
import dev.revivalo.playerwarps.storage.WarpStorage;
import dev.revivalo.playerwarps.util.PermissionUtil;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class WarpManager {

    private final Set<Warp> warps;
    private WarpStorage storage;

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

    /**
     * Reads the warps from the storage. With a database this finishes later, once the warps
     * have been fetched.
     */
    public void loadWarps() {
        storage.load(this, this::reportReservedNames);
    }

    public void saveWarps() {
        saveWarps(true);
    }

    /**
     * Writes the warps to the storage. Serialization reads warp state and Bukkit objects, so it
     * always runs on the calling (main) thread; only the write itself can be offloaded. On
     * shutdown the write has to stay synchronous, as the scheduler is gone.
     */
    public void saveWarps(boolean writeAsynchronously) {
        storage.save(this, writeAsynchronously);
    }

    public WarpStorage getStorage() {
        return storage;
    }

    public void setStorage(WarpStorage storage) {
        this.storage = storage;
    }

    public void showOnMaps(Warp warp) {
        HookRegister.ifEnabled(DynmapHook.class, dynmapHook -> dynmapHook.setMarker(warp));
        HookRegister.ifEnabled(BlueMapHook.class, blueMapHook -> blueMapHook.setMarker(warp));
        HookRegister.ifEnabled(SquaremapHook.class, squaremapHook -> squaremapHook.setMarker(warp));
    }

    public void hideFromMaps(Warp warp) {
        HookRegister.ifEnabled(DynmapHook.class, dynmapHook -> dynmapHook.removeMarker(warp));
        HookRegister.ifEnabled(BlueMapHook.class, blueMapHook -> blueMapHook.removeMarker(warp));
        HookRegister.ifEnabled(SquaremapHook.class, squaremapHook -> squaremapHook.removeMarker(warp));
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