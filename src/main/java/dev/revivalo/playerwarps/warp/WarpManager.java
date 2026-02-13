package dev.revivalo.playerwarps.warp;

import com.tchristofferson.configupdater.ConfigUpdater;
import dev.revivalo.playerwarps.PlayerWarpsPlugin;
import dev.revivalo.playerwarps.category.Category;
import dev.revivalo.playerwarps.configuration.file.Config;
import dev.revivalo.playerwarps.configuration.file.Lang;
import dev.revivalo.playerwarps.hook.register.BlueMapHook;
import dev.revivalo.playerwarps.hook.register.DynmapHook;
import dev.revivalo.playerwarps.menu.page.ManageMenu;
import dev.revivalo.playerwarps.hook.HookRegister;
import dev.revivalo.playerwarps.menu.sort.*;
import dev.revivalo.playerwarps.playerconfig.PlayerConfig;
import dev.revivalo.playerwarps.util.PermissionUtil;
import dev.revivalo.playerwarps.warp.action.WarpAction;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class WarpManager {

    private final Set<Warp> warps;
    private final List<String> bannedWorlds;

    private final SortingManager sortingManager;

    public WarpManager() {
        ConfigurationSerialization.registerClass(Warp.class);

        warps = new HashSet<>();

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
                                }
                        ));
    }

    public void saveWarps() {
        final ConfigurationSection warpsSection = PlayerWarpsPlugin.getData().getConfiguration().createSection("warps");

        warps.forEach(warp -> warpsSection.set(warp.getWarpID().toString(), warp));

        PlayerWarpsPlugin.getData().getYamlFile().save();
        if (Config.AUTOSAVE_ANNOUNCE.asBoolean()) {
            Bukkit.getLogger().info("Saving " + warps.size() + " warps");
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

    public CompletableFuture<String> waitForPlayerInput(Player player, Warp warp, WarpAction<?> warpAction) {
        CompletableFuture<String> future = new CompletableFuture<>();

        player.closeInventory();
        player.sendMessage(warpAction.getMessage().asColoredString().replace("%warp%", warp.getName()));

        BaseComponent[] msg = TextComponent.fromLegacyText(Lang.CANCEL_INPUT.asColoredString());
        for (BaseComponent bc : msg) {
            bc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(Lang.CLICK_TO_CANCEL_INPUT.asColoredString())));
            bc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/pwcancel"));
        }

        player.spigot().sendMessage(msg);

        Listener listener = new Listener() {
            @EventHandler
            public void onPlayerChat(AsyncPlayerChatEvent event) {
                if (event.getPlayer().equals(player)) {
                    event.setCancelled(true);
                    future.complete(event.getMessage());
                    PlayerWarpsPlugin.get().runSync(() -> {
                        if (!warpAction.hasFee()) new ManageMenu(warp).openFor(player);
                    });
                    HandlerList.unregisterAll(this);
                }
            }

            @EventHandler
            public void onChat(final PlayerCommandPreprocessEvent event) {
                if (!event.getPlayer().equals(player))
                    return;

                if (event.getMessage().equalsIgnoreCase("/pwcancel")) {
                    event.setCancelled(true);
                    HandlerList.unregisterAll(this);
                    player.sendMessage(Lang.INPUT_CANCELLED.asColoredString());
                }
            }
        };

        Bukkit.getPluginManager().registerEvents(listener, PlayerWarpsPlugin.get());

        Bukkit.getScheduler().runTaskLater(PlayerWarpsPlugin.get(), () -> {
            if (!future.isDone()) {
                future.completeExceptionally(new TimeoutException("Player did not respond in time"));
                PlayerWarpsPlugin.get().getLogger().info("Not responded");
                HandlerList.unregisterAll(listener);
            }
        }, 15 * 20);


        return future;
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